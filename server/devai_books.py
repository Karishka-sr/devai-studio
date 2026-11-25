"""Text extraction for books and documents without executing them."""

from __future__ import annotations

import html
import io
import re
import subprocess
import zipfile
from pathlib import Path
from xml.etree import ElementTree


def _clean(text: str) -> str:
    text = html.unescape(text)
    text = re.sub(r"\r\n?", "\n", text)
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def _html_text(raw: bytes) -> str:
    text = raw.decode("utf-8", "replace")
    text = re.sub(r"(?is)<(script|style|nav|header|footer).*?</\1>", " ", text)
    text = re.sub(r"(?s)<[^>]+>", " ", text)
    return _clean(text)


def _epub_text(raw: bytes) -> str:
    parts: list[str] = []
    with zipfile.ZipFile(io.BytesIO(raw)) as archive:
        names = [name for name in archive.namelist() if name.lower().endswith((".xhtml", ".html", ".htm"))]
        for name in names[:200]:
            parts.append(_html_text(archive.read(name)))
    return _clean("\n\n".join(parts))


def _docx_text(raw: bytes) -> str:
    with zipfile.ZipFile(io.BytesIO(raw)) as archive:
        xml = archive.read("word/document.xml")
    root = ElementTree.fromstring(xml)
    chunks = [node.text or "" for node in root.iter() if node.tag.endswith("}t")]
    return _clean("\n".join(chunks))


def _fb2_text(raw: bytes) -> str:
    root = ElementTree.fromstring(raw)
    chunks = [node.text or "" for node in root.iter() if node.tag.endswith("}p") or node.tag.endswith("}title")]
    return _clean("\n\n".join(chunks))


def _pdf_text(raw: bytes) -> str:
    try:
        from pypdf import PdfReader
    except Exception as exc:  # noqa: BLE001
        raise ValueError("для PDF установите pypdf") from exc
    reader = PdfReader(io.BytesIO(raw))
    return _clean("\n\n".join((page.extract_text() or "") for page in reader.pages[:2000]))


def _pdftotext(raw: bytes) -> str:
    try:
        result = subprocess.run(
            ["pdftotext", "-layout", "-", "-"],
            input=raw,
            capture_output=True,
            timeout=120,
            check=True,
        )
        return _clean(result.stdout.decode("utf-8", "replace"))
    except Exception:
        return ""


def extract_text(data: bytes, filename: str = "") -> str:
    suffix = Path(filename or "").suffix.lower()
    if suffix in {".txt", ".md", ".markdown", ".csv", ".tsv", ".log"}:
        return _clean(data.decode("utf-8", "replace"))
    if suffix in {".html", ".htm", ".xhtml"}:
        return _html_text(data)
    if suffix == ".epub":
        return _epub_text(data)
    if suffix == ".docx":
        return _docx_text(data)
    if suffix == ".fb2":
        return _fb2_text(data)
    if suffix == ".pdf":
        text = _pdftotext(data)
        return text or _pdf_text(data)
    if suffix in {".rtf"}:
        text = data.decode("utf-8", "replace")
        text = re.sub(r"\\'[0-9a-fA-F]{2}", " ", text)
        text = re.sub(r"\\[a-zA-Z]+-?\d* ?", " ", text)
        return _clean(text)
    raise ValueError("поддерживаются txt, md, html, epub, fb2, docx, pdf, rtf")


def chunk_text(text: str, max_chars: int = 1800) -> list[str]:
    text = _clean(text)
    if len(text) < 80:
        return []
    paragraphs = [part.strip() for part in re.split(r"\n{2,}", text) if part.strip()]
    chunks: list[str] = []
    current: list[str] = []
    size = 0
    for paragraph in paragraphs:
        if len(paragraph) > max_chars:
            paragraphs.extend(
                paragraph[index:index + max_chars]
                for index in range(0, len(paragraph), max_chars)
            )
            continue
        if current and size + len(paragraph) + 2 > max_chars:
            chunks.append("\n\n".join(current))
            current, size = [], 0
        current.append(paragraph)
        size += len(paragraph) + 2
    if current:
        chunks.append("\n\n".join(current))
    return [chunk for chunk in chunks if len(chunk) >= 80]
