#!/usr/bin/env python3
"""
db_local2mysql.py

Schema-aware migration from local SQLite -> MySQL.

This script pushes data explicitly to MySQL (DB_URL/DB_USERNAME/DB_PASSWORD in .env)
by merging matching local tables/columns into the existing MySQL schema.
It does not call db_local2prod.py.

Usage:
    python3 scripts/db_local2mysql.py
    FORCE_YES=true python3 scripts/db_local2mysql.py
"""

import os
import sqlite3
from pathlib import Path

import mysql.connector
from mysql.connector import Error

from migration_utils import print_header
from mysqlrestore import get_mysql_config

PROJECT_ROOT = Path(__file__).parent.parent
DB_FILE = PROJECT_ROOT / "volumes" / "sqlite.db"


def get_user_confirmation() -> bool:
    if os.getenv("FORCE_YES") == "true":
        print("FORCE_YES detected, proceeding automatically...")
        return True

    print("WARNING: This will import local SQLite data into MySQL.")
    print("MySQL schema is preserved; matching tables will be replaced with local data.")
    print(f"Source file: {DB_FILE}")
    response = input("Continue? (yes/no): ").strip().lower()
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


def get_mysql_tables(conn) -> set[str]:
    cursor = conn.cursor()
    cursor.execute("SHOW TABLES")
    tables = {row[0] for row in cursor.fetchall()}
    cursor.close()
    return tables


def get_mysql_columns(conn, table_name: str) -> set[str]:
    cursor = conn.cursor()
    cursor.execute(f"SHOW COLUMNS FROM `{table_name}`")
    cols = {row[0] for row in cursor.fetchall()}
    cursor.close()
    return cols


def merge_local_to_mysql(sqlite_db: Path, mysql_conn) -> tuple[int, int, int]:
    src = sqlite3.connect(str(sqlite_db))

    imported = 0
    skipped = 0
    errors = 0

    try:
        src_tables = get_sqlite_tables(src)
        dst_tables = get_mysql_tables(mysql_conn)

        mysql_cursor = mysql_conn.cursor()
        mysql_cursor.execute("SET FOREIGN_KEY_CHECKS = 0")
        mysql_conn.commit()
        mysql_cursor.close()

        for table_name in sorted(src_tables):
            if table_name not in dst_tables:
                skipped += 1
                print(f"  Skipping {table_name}: table not present in MySQL schema")
                continue

            src_cols = get_sqlite_columns(src, table_name)
            dst_cols = get_mysql_columns(mysql_conn, table_name)
            dst_lookup = {c.lower(): c for c in dst_cols}

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
            dst_cur = mysql_conn.cursor()
            src_col_sql = ", ".join([f'"{src_col}"' for src_col, _ in matched_pairs])
            dst_col_sql = ", ".join([f"`{dst_col}`" for _, dst_col in matched_pairs])
            placeholders = ", ".join(["%s" for _ in matched_pairs])

            try:
                src_cur.execute(f'SELECT {src_col_sql} FROM "{table_name}"')
                rows = src_cur.fetchall()

                dst_cur.execute(f"DELETE FROM `{table_name}`")
                if rows:
                    dst_cur.executemany(
                        f"INSERT INTO `{table_name}` ({dst_col_sql}) VALUES ({placeholders})",
                        rows,
                    )
                mysql_conn.commit()
                imported += 1
                print(f"  {table_name}: {len(rows)} rows imported")
            except Exception as exc:
                mysql_conn.rollback()
                errors += 1
                print(f"  {table_name}: ERROR - {str(exc)[:140]}")
            finally:
                src_cur.close()
                dst_cur.close()

        mysql_cursor = mysql_conn.cursor()
        mysql_cursor.execute("SET FOREIGN_KEY_CHECKS = 1")
        mysql_conn.commit()
        mysql_cursor.close()
    finally:
        src.close()

    return imported, skipped, errors


def main() -> int:
    print_header("LOCAL SQLITE TO MYSQL")

    if not DB_FILE.exists():
        print(f"Error: SQLite file not found at {DB_FILE}")
        return 1

    if not get_user_confirmation():
        print("Cancelled.")
        return 0

    host, port, username, password, database = get_mysql_config()

    try:
        mysql_conn = mysql.connector.connect(
            host=host,
            port=port,
            user=username,
            password=password,
            database=database,
        )
    except Error as exc:
        print(f"Error connecting to MySQL: {exc}")
        return 1

    try:
        imported, skipped, errors = merge_local_to_mysql(DB_FILE, mysql_conn)

        print_header("Migration Summary")
        print(f"  Imported tables: {imported}")
        print(f"  Skipped tables:  {skipped}")
        print(f"  Error tables:    {errors}")

        if errors:
            print("\nMigration incomplete due to table errors.")
            return 1

        print("\nDone. MySQL now contains local SQLite data merged onto existing schema.")
        print(f"MySQL target: {username}@{host}:{port}/{database}")
        return 0
    finally:
        mysql_conn.close()


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except KeyboardInterrupt:
        print("\nInterrupted by user")
        raise SystemExit(1)
