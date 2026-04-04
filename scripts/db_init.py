#!/usr/bin/env python3
"""
db_init.py - Spring Boot Database Initialization

Resets the database to original state with default data.
- Backs up the current database
- Drops all existing tables
- Creates fresh schema via Spring Boot
- Loads default data via ModelInit

Usage:
    cd scripts && ./db_init.py
    or
    scripts/db_init.py
    or with force flag:
    FORCE_YES=true scripts/db_init.py
"""

import os
import sys
from pathlib import Path

from migration_utils import (
    MigrationError,
    SpringBootSchemaRunner,
    SqliteDatabaseFiles,
    ensure_port_not_in_use,
    print_header,
)

# Configuration
PROJECT_ROOT = Path(__file__).parent.parent
DB_FILE = PROJECT_ROOT / "volumes" / "sqlite.db"
BACKUP_DIR = PROJECT_ROOT / "volumes" / "backups"
SPRING_PORT = 8585
LOG_FILE = "/tmp/db_init.log"

def check_spring_boot_running():
    """Check if Spring Boot is already running"""
    ensure_port_not_in_use(SPRING_PORT, "Please stop it first: pkill -f 'spring-boot:run'")


def get_user_confirmation():
    """Get user confirmation before proceeding"""
    if not DB_FILE.exists():
        return True
    
    # Check for FORCE_YES environment variable
    if os.getenv('FORCE_YES') == 'true':
        print("FORCE_YES detected, proceeding automatically...")
        return True
    
    print("WARNING: You are about to lose all data in the database!")
    print("This will reset the database to original state with default data")
    print("All current data will be lost (after backup)\n")
    
    response = input("Continue? (y/n): ").strip().lower()
    return response in ('y', 'yes')


def main():
    """Main initialization process"""
    print_header("Database Reset to Original State")
    
    # Step 1: Check if Spring Boot is running
    check_spring_boot_running()
    
    # Step 2: Get user confirmation
    if not get_user_confirmation():
        print("Cancelled.")
        sys.exit(0)
    
    # Step 3: Backup database
    db_files = SqliteDatabaseFiles(DB_FILE, BACKUP_DIR)
    db_files.backup()
    
    # Step 4: Remove old database
    db_files.remove()
    
    # Step 5: Start Spring Boot to recreate schema and load data
    print("\nStarting Spring Boot to recreate schema and load default data...")
    print("(This will take a few seconds...)")
    runner = SpringBootSchemaRunner(PROJECT_ROOT, LOG_FILE, SPRING_PORT)
    runner.run(timeout_seconds=180, settle_seconds=5)
    
    # Success message
    print_header("DATABASE RESET COMPLETE")
    print("The database has been reset to original state with:")
    print("  Fresh schema created")
    print("  Default data loaded (Person.init(), QuizScore.init(), etc.)")
    print("\nYou can now start your application normally:")
    print("  ./mvnw spring-boot:run\n")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\nInterrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\nAn error occurred: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)