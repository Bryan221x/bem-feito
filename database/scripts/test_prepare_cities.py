"""Testes offline sobre o snapshot oficial completo, sem dados inventados."""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
import prepare_cities as importer


class PrepareCitiesTests(unittest.TestCase):
    def test_offline_generation_is_reproducible(self):
        expected = importer.OUTPUT.read_bytes()
        original_output = importer.OUTPUT
        try:
            with tempfile.TemporaryDirectory() as directory:
                importer.OUTPUT = Path(directory) / "cities.sql"
                importer.generate()
                self.assertEqual(expected, importer.OUTPUT.read_bytes())
        finally:
            importer.OUTPUT = original_output

    def test_corrupt_snapshot_is_rejected_before_sql_is_written(self):
        original_snapshot, original_output = importer.SNAPSHOT, importer.OUTPUT
        try:
            with tempfile.TemporaryDirectory() as directory:
                importer.SNAPSHOT = Path(directory) / "municipalities.csv"
                importer.SNAPSHOT.write_bytes(original_snapshot.read_bytes() + b"\n")
                importer.OUTPUT = Path(directory) / "cities.sql"
                with self.assertRaisesRegex(ValueError, "Checksum"):
                    importer.generate()
                self.assertFalse(importer.OUTPUT.exists())
        finally:
            importer.SNAPSHOT, importer.OUTPUT = original_snapshot, original_output

    def test_reference_test_ddl_matches_versioned_mysql_schema(self):
        import re
        schema = (importer.BASE / "schema.sql").read_text(encoding="utf-8")
        test_schema = (importer.BASE.parent / "backend/src/test/resources/reference-schema.sql").read_text(encoding="utf-8")
        for table in ["states", "cities", "institution_categories", "service_areas", "need_categories",
                      "institutions", "units", "institution_contacts", "unit_contacts", "unit_opening_hours",
                      "unit_service_areas", "needs", "institution_history_images", "users", "institution_memberships", "registration_requests", "registration_request_contacts",
                      "registration_request_units", "registration_request_unit_contacts", "access_requests", "account_invitation_tokens"]:
            statement = re.search(r"CREATE TABLE " + table + r" \(.*?\) ENGINE = InnoDB;", schema, re.S).group(0)
            self.assertIn(statement, test_schema)


if __name__ == "__main__":
    unittest.main()
