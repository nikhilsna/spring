#!/usr/bin/env python3
"""
db_mysql2local.py

Schema-aware migration from MySQL -> local SQLite.

This script pulls data explicitly from MySQL (DB_URL/DB_USERNAME/DB_PASSWORD in .env),
exports it to a temporary SQLite file, and merges matching tables/columns into your
existing local SQLite schema. It does not call db_prod2local.py.

Usage:
    python3 scripts/db_mysql2local.py
    FORCE_YES=true python3 scripts/db_mysql2local.py
"""

import os
import sqlite3
import tempfile
from pathlib import Path

from migration_utils import SqliteDatabaseFiles, print_header
from mysqlbackup import backup_mysql_to_sqlite, get_mysql_config

PROJECT_ROOT = Path(__file__).parent.parent
DB_FILE = PROJECT_ROOT / "volumes" / "sqlite.db"
BACKUP_DIR = PROJECT_ROOT / "volumes" / "backups"


def get_user_confirmation() -> bool:
    if os.getenv("FORCE_YES") == "true":
        print("FORCE_YES detected, proceeding automatically...")
        return True

    print("WARNING: This will import data from MySQL into your local SQLite database.")
    print("Local schema is preserved; matching tables will be replaced with MySQL data.")
    print(f"Target file: {DB_FILE}")
    response = input("Continue? (y/n): ").strip().lower()
    return response in ("y", "yes")


def get_sqlite_tables(conn: sqlite3.Connection) -> set[str]:
    cursor = conn.cursor()
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")
    tables = {row[0] for row in cursor.fetchall()}
    cursor.close()
    return tables


def get_sqlite_columns(conn: sqlite3.Connection, table_name: str) -> list[str]:
    cursor = conn.cursor()
    cursor.execute(f'PRAGMA table_info("{table_name}")')
    cols = [row[1] for row in cursor.fetchall()]
    cursor.close()
    return cols


def merge_sqlite_data(source_db: Path, target_db: Path) -> tuple[int, int, int]:
    """Merge source SQLite into target SQLite by matching table/column names."""
    src = sqlite3.connect(str(source_db))
    dst = sqlite3.connect(str(target_db))

    imported = 0
    skipped = 0
    errors = 0

    try:
        src_tables = get_sqlite_tables(src)
        dst_tables = get_sqlite_tables(dst)

        dst.execute("PRAGMA foreign_keys = OFF")

        for table_name in sorted(src_tables):
            if table_name not in dst_tables:
                skipped += 1
                print(f"  Skipping {table_name}: table not present in local schema")
                continue

            src_cols = get_sqlite_columns(src, table_name)
            dst_cols = get_sqlite_columns(dst, table_name)
            dst_lookup = {c.lower(): c for c in dst_cols}

            # Match columns case-insensitively so MySQL-origin names like TEXT
            # map onto local sqlite columns like text.
            matched_pairs = []
            for src_col in src_cols:
                dst_col = dst_lookup.get(src_col.lower())
                if dst_col:
                    matched_pairs.append((src_col, dst_col))

            if not matched_pairs:
                skipped += 1
                print(f"  Skipping {table_name}: no matching columns")
                continue

            src_cur = src.cursor()
            dst_cur = dst.cursor()
            src_col_sql = ", ".join([f'"{src_col}"' for src_col, _ in matched_pairs])
            dst_col_sql = ", ".join([f'"{dst_col}"' for _, dst_col in matched_pairs])
            placeholders = ", ".join(["?" for _ in matched_pairs])

            try:
                src_cur.execute(f'SELECT {src_col_sql} FROM "{table_name}"')
                rows = src_cur.fetchall()

                dst_cur.execute(f'DELETE FROM "{table_name}"')
                if rows:
                    dst_cur.executemany(
                        f'INSERT INTO "{table_name}" ({dst_col_sql}) VALUES ({placeholders})',
                        rows,
                    )
                dst.commit()
                imported += 1
                print(f"  {table_name}: {len(rows)} rows imported")
            except Exception as exc:
                dst.rollback()
                errors += 1
                print(f"  {table_name}: ERROR - {str(exc)[:140]}")
            finally:
                src_cur.close()
                dst_cur.close()

        dst.execute("PRAGMA foreign_keys = ON")
    finally:
        src.close()
        dst.close()

    return imported, skipped, errors


def main() -> int:
    print_header("MYSQL TO LOCAL SQLITE")

    if not DB_FILE.exists():
        print(f"Error: Local SQLite file not found at {DB_FILE}")
        print("Run python scripts/db_init.py first to build local schema.")
        return 1

    if not get_user_confirmation():
        print("Cancelled.")
        return 0

    host, port, username, password, database = get_mysql_config()

    db_files = SqliteDatabaseFiles(DB_FILE, BACKUP_DIR)
    db_files.backup()

    with tempfile.NamedTemporaryFile(suffix=".db", delete=False) as tmp:
        temp_sqlite = Path(tmp.name)

    try:
        print_header("Export MySQL To Temp SQLite")
        backup_mysql_to_sqlite(
            host=host,
            port=port,
            user=username,
            password=password,
            database=database,
            backup_file=temp_sqlite,
        )

        print_header("Merge Into Local SQLite")
        imported, skipped, errors = merge_sqlite_data(temp_sqlite, DB_FILE)

        print_header("Migration Summary")
        print(f"  Imported tables: {imported}")
        print(f"  Skipped tables:  {skipped}")
        print(f"  Error tables:    {errors}")

        if errors:
            print("\nMigration incomplete due to table errors.")
            return 1

        print("\nDone. Local SQLite now contains MySQL data merged onto local schema.")
        return 0
    finally:
        try:
            temp_sqlite.unlink(missing_ok=True)
        except Exception:
            pass


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print("\nInterrupted by user")
        raise SystemExit(1)
