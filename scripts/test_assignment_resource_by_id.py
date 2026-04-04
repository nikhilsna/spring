#!/usr/bin/env python3
import argparse
import json
from http.cookies import SimpleCookie
import random
import string
import sys
import time
from pathlib import Path

import requests

DEFAULT_BASE_URL = "http://localhost:8585"
DEFAULT_UID = "toby"
DEFAULT_PASSWORD = "not putting in the actual password"


def random_suffix(length: int = 8) -> str:
    return "".join(random.choices(string.ascii_lowercase + string.digits, k=length))


def fetch_assignments(session: requests.Session, base_url: str, jwt_token: str) -> list:
    resp = session.get(
        f"{base_url}/api/assignments/debug",
        cookies={"jwt_java_spring": jwt_token},
        timeout=20,
    )
    resp.raise_for_status()
    data = resp.json()
    if not isinstance(data, list):
        raise RuntimeError("Unexpected assignments payload")
    return data


def assignment_map(assignments: list) -> dict:
    return {a.get("id"): a for a in assignments if isinstance(a, dict) and a.get("id") is not None}


def extract_jwt_token(session: requests.Session, auth_resp: requests.Response) -> str | None:
    if "jwt_java_spring" in session.cookies:
        return session.cookies.get("jwt_java_spring")

    set_cookie = auth_resp.headers.get("Set-Cookie", "")
    if not set_cookie:
        return None

    cookie = SimpleCookie()
    cookie.load(set_cookie)
    morsel = cookie.get("jwt_java_spring")
    if morsel is None:
        return None

    token = morsel.value
    return token or None


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Upload URL and file resources to a specific assignment ID and verify scope"
    )
    parser.add_argument("--assignment-id", type=int, help="Target assignment ID")
    parser.add_argument("--base-url", default=DEFAULT_BASE_URL)
    parser.add_argument("--uid", default=DEFAULT_UID)
    parser.add_argument("--password", default=DEFAULT_PASSWORD)
    parser.add_argument(
        "--file-path",
        default="scripts/random_assignment_resource_upload.txt",
        help="Local file path to upload",
    )
    parser.add_argument(
        "--url-only",
        action="store_true",
        help="Only test URL upload and skip file upload",
    )
    args = parser.parse_args()

    uid = args.uid or DEFAULT_UID
    password = args.password or DEFAULT_PASSWORD

    if not password:
        print("ERROR: Missing password. Set --password", file=sys.stderr)
        return 1

    target_assignment_id = args.assignment_id
    file_path = Path(args.file_path)
    file_path.parent.mkdir(parents=True, exist_ok=True)

    # Generate random local file content for this test run.
    payload_text = (
        "Assignment resource upload test\n"
        f"timestamp={int(time.time())}\n"
        f"nonce={random_suffix(16)}\n"
    )
    file_path.write_text(payload_text, encoding="utf-8")

    session = requests.Session()

    auth_resp = session.post(
        f"{args.base_url}/authenticate",
        json={"uid": uid, "password": password},
        timeout=20,
    )
    if auth_resp.status_code != 200:
        print(f"ERROR: Authentication failed ({auth_resp.status_code}): {auth_resp.text}", file=sys.stderr)
        return 1

    jwt_token = extract_jwt_token(session, auth_resp)
    if not jwt_token:
        print("ERROR: Authentication succeeded but JWT cookie was not set", file=sys.stderr)
        print(f"Auth headers: {dict(auth_resp.headers)}", file=sys.stderr)
        print(f"Auth body: {auth_resp.text}", file=sys.stderr)
        return 1

    before_assignments = fetch_assignments(session, args.base_url, jwt_token)
    before_by_id = assignment_map(before_assignments)

    if target_assignment_id is None:
        if not before_by_id:
            print(
                "ERROR: No assignments found. Create one manually first, then rerun this test.",
                file=sys.stderr,
            )
            return 1
        target_assignment_id = sorted(before_by_id.keys())[0]
        print(f"INFO: --assignment-id not provided, using assignment ID {target_assignment_id}")

    if target_assignment_id not in before_by_id:
        print(f"ERROR: Assignment ID {target_assignment_id} not found", file=sys.stderr)
        return 1

    control_id = next((aid for aid in before_by_id.keys() if aid != target_assignment_id), None)
    control_before = json.dumps(before_by_id.get(control_id), sort_keys=True) if control_id is not None else None

    test_url = f"https://example.com/assignment-resource/{target_assignment_id}/{random_suffix(10)}"
    url_resp = session.post(
        f"{args.base_url}/api/assignments/{target_assignment_id}/resource/url",
        json={"url": test_url},
        cookies={"jwt_java_spring": jwt_token},
        timeout=20,
    )
    if url_resp.status_code != 200:
        print(f"ERROR: URL upload failed ({url_resp.status_code}): {url_resp.text}", file=sys.stderr)
        return 1

    after_url_assignments = fetch_assignments(session, args.base_url, jwt_token)
    after_url_by_id = assignment_map(after_url_assignments)
    target_after_url = after_url_by_id.get(target_assignment_id, {})
    if target_after_url.get("resourceType") != "url":
        print(
            "ERROR: URL upload did not set resourceType=url. "
            f"Current value: {target_after_url.get('resourceType')}",
            file=sys.stderr,
        )
        return 1
    if target_after_url.get("resourceUrl") != test_url:
        print(
            "ERROR: URL upload mismatch. "
            f"Expected {test_url}, got {target_after_url.get('resourceUrl')}",
            file=sys.stderr,
        )
        return 1
    print("PASS: URL resource upload verified.")

    if not args.url_only:
        with file_path.open("rb") as f:
            file_resp = session.post(
                f"{args.base_url}/api/assignments/{target_assignment_id}/resource/file",
                files={"file": (file_path.name, f, "text/plain")},
                cookies={"jwt_java_spring": jwt_token},
                timeout=30,
            )
        if file_resp.status_code != 200:
            print(f"ERROR: File upload failed ({file_resp.status_code}): {file_resp.text}", file=sys.stderr)
            return 1

        after_assignments = fetch_assignments(session, args.base_url, jwt_token)
        after_by_id = assignment_map(after_assignments)
        target_after = after_by_id.get(target_assignment_id, {})

        if target_after.get("resourceType") != "file":
            print(
                "ERROR: Target assignment was not updated to file resource type. "
                f"Current value: {target_after.get('resourceType')}",
                file=sys.stderr,
            )
            return 1

        if target_after.get("resourceFilename") != file_path.name:
            print(
                "ERROR: Uploaded filename mismatch. "
                f"Expected {file_path.name}, got {target_after.get('resourceFilename')}",
                file=sys.stderr,
            )
            return 1

        if control_id is not None:
            control_after = json.dumps(after_by_id.get(control_id), sort_keys=True)
            if control_before != control_after:
                print(
                    "ERROR: A non-target assignment changed unexpectedly "
                    f"(control ID {control_id}).",
                    file=sys.stderr,
                )
                return 1

        print("PASS: File resource upload verified.")
        print("PASS: Resource URL+file uploads succeeded for the target assignment ID only.")
    else:
        print("PASS: URL-only mode complete. File upload skipped by request.")

    print(f"Target assignment ID: {target_assignment_id}")
    print(f"Uploaded file: {file_path}")
    print(f"URL used: {test_url}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())