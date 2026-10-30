import tempfile
import unittest
from pathlib import Path
from audit_uploads import audit


class UploadAuditTests(unittest.TestCase):
    def test_reports_missing_orphan_and_unknown_without_mutating(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            folder = root / "1"
            folder.mkdir()
            key = "11111111-1111-1111-1111-111111111111.png"
            (folder / key).write_bytes(b"test bytes")
            (folder / "unknown.txt").write_text("keep", encoding="utf-8")
            result = audit(root, [{"institutionId": 2, "url": f"/api/history-media/2/{key}"}])
            self.assertEqual(result["missing"], [f"2/{key}"])
            self.assertEqual(result["orphanCandidates"][0]["path"], f"1/{key}")
            self.assertEqual(result["unknown"], ["1/unknown.txt"])
            self.assertEqual((folder / key).read_bytes(), b"test bytes")

    def test_foreign_owner_and_traversal_abort(self):
        with tempfile.TemporaryDirectory() as root:
            for url in ["/api/history-media/2/../x", "/api/history-media/1/../../x"]:
                with self.assertRaises(ValueError):
                    audit(root, [{"institutionId": 1, "url": url}])

    def test_referenced_file_is_never_a_candidate(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / "1").mkdir()
            key = "11111111-1111-1111-1111-111111111111.png"
            (root / "1" / key).write_bytes(b"preserved")
            result = audit(root, [{"institutionId": 1, "url": f"/api/history-media/1/{key}"},
                                  {"institutionId": 1, "url": "https://example.invalid/photo.png"}])
            self.assertEqual(result["orphanCandidates"], [])
            self.assertEqual(result["missing"], [])
