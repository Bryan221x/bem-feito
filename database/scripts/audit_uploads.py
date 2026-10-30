"""Read-only reconciliation against a fresh JSON-lines DB export. Never deletes files."""
import argparse
import hashlib
import json
from pathlib import Path
import re

KEY = re.compile(r"[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\.png")


def audit(root, references):
    root = Path(root)
    if root.is_symlink() or not root.is_dir():
        raise ValueError("Upload root must be a real directory")
    expected = set()
    for row in references:
        owner, url = row["institutionId"], row["url"]
        if not isinstance(owner, int) or isinstance(owner, bool) or owner <= 0 or not isinstance(url, str):
            raise ValueError("Invalid reference export")
        if url.startswith(("https://", "http://")):
            continue
        prefix = f"/api/history-media/{owner}/"
        if not url.startswith(prefix) or not KEY.fullmatch(url[len(prefix):]):
            raise ValueError("Invalid managed image reference; stop reconciliation")
        expected.add(f"{owner}/{url[len(prefix):]}")
    present, unknown, orphans = set(), [], []
    for directory in sorted(root.iterdir()):
        if directory.is_symlink() or not directory.is_dir() or not re.fullmatch(r"[1-9][0-9]*", directory.name):
            unknown.append(directory.name)
            continue
        for file in sorted(directory.iterdir()):
            name = f"{directory.name}/{file.name}"
            if file.is_symlink() or not file.is_file() or not KEY.fullmatch(file.name):
                unknown.append(name)
                continue
            present.add(name)
            if name not in expected:
                with file.open("rb") as stream:
                    digest = hashlib.file_digest(stream, "sha256").hexdigest()
                orphans.append({"path": name, "sha256": digest, "bytes": file.stat().st_size})
    return {"missing": sorted(expected - present), "orphanCandidates": orphans,
            "unknown": unknown, "referencedFiles": len(expected), "action": "REPORT_ONLY"}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", required=True)
    parser.add_argument("--references", required=True, help="JSON lines from deploy/upload-references.sql")
    args = parser.parse_args()
    rows = [json.loads(line) for line in Path(args.references).read_text(encoding="utf-8-sig").splitlines() if line.strip()]
    print(json.dumps(audit(args.root, rows), indent=2))
