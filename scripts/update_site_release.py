#!/usr/bin/env python3
"""Fetch the latest DevAI Studio release and update docs/data/latest.json."""

from __future__ import annotations

import json
import os
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

REPO = os.environ.get("GITHUB_REPOSITORY", "Karishka-sr/devai-studio")
TOKEN = os.environ.get("GITHUB_TOKEN", "")
OUT = Path("docs/data/latest.json")
API = "https://api.github.com"


def api_get(path: str) -> dict:
    request = urllib.request.Request(
        f"{API}{path}",
        headers={
            "Accept": "application/vnd.github+json",
            "Authorization": f"Bearer {TOKEN}",
            "User-Agent": "DevAI-Studio-Site",
            "X-GitHub-Api-Version": "2022-11-28",
        },
    )
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def has_apk(release: dict) -> bool:
    return any(
        str(asset.get("name", "")).lower().endswith(".apk")
        for asset in release.get("assets", [])
    )


def pick_release() -> dict:
    try:
        latest = api_get(f"/repos/{REPO}/releases/latest")
        if has_apk(latest):
            return latest
    except urllib.error.HTTPError as error:
        if error.code not in (403, 404):
            raise

    for release in api_get(f"/repos/{REPO}/releases?per_page=10"):
        if not release.get("draft") and not release.get("prerelease") and has_apk(release):
            return release

    raise SystemExit("No published release with an APK asset found")


def main() -> int:
    release = pick_release()
    assets = release.get("assets", [])
    apk = next(
        (asset for asset in assets if str(asset.get("name", "")).lower().endswith(".apk")),
        None,
    )
    languages = next(
        (asset for asset in assets if str(asset.get("name", "")).lower().endswith(".zip")),
        None,
    )
    if apk is None:
        raise SystemExit("Latest release has no APK asset")

    tag = str(release.get("tag_name", "")).strip()
    version = tag.removeprefix("v").removeprefix("V")
    digest = str(apk.get("digest") or "").removeprefix("sha256:")

    payload = {
        "tag": tag,
        "version": version,
        "name": release.get("name") or f"DevAI Studio {version}",
        "published_at": release.get("published_at"),
        "page_url": release.get("html_url"),
        "apk_url": apk.get("browser_download_url"),
        "apk_name": apk.get("name"),
        "apk_size": apk.get("size"),
        "apk_sha256": digest or None,
        "languages_url": languages.get("browser_download_url") if languages else "",
        "languages_name": languages.get("name") if languages else "",
        "body": release.get("body") or "",
        "generated_at": datetime.now(timezone.utc).isoformat(timespec="seconds").replace("+00:00", "Z"),
    }

    if OUT.exists():
        try:
            current = json.loads(OUT.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            current = None
        if current is not None:
            comparable = {key: value for key, value in payload.items() if key != "generated_at"}
            previous = {key: value for key, value in current.items() if key != "generated_at"}
            if comparable == previous:
                print(f"{OUT} is already current ({tag})")
                return 0

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Updated {OUT} to {tag}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
