"""Heuristic file antivirus for direct file uploads and chat attachments.

This is not a replacement for ClamAV/VirusTotal. It is a fast, dependency-free
first line of defence that recognises the most common dangerous constructs:
executables, scripts, macro documents, PDFs with JavaScript, APK abuse and
archives that hide executables. It never executes uploaded content.
"""
from __future__ import annotations

import base64
import binascii
import hashlib
import io
import math
import os
import re
import time
import zipfile

EICAR = b"X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*"
MAX_SCAN_BYTES = 8 * 1024 * 1024
MAX_ARCHIVE_ENTRIES = 40
MAX_ARCHIVE_MEMBER = 2 * 1024 * 1024

DANGEROUS_EXT = {
    ".exe", ".scr", ".bat", ".cmd", ".com", ".pif", ".vbs", ".vbe", ".js",
    ".jse", ".wsf", ".wsh", ".hta", ".msi", ".msp", ".jar", ".apk", ".dll",
    ".ps1", ".psm1", ".lnk", ".reg", ".cpl", ".docm", ".xlsm", ".pptm",
}
DOC_EXT = {".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".rtf", ".odt"}
TEXT_EXT = {
    ".txt", ".md", ".json", ".xml", ".yaml", ".yml", ".csv", ".py", ".kt",
    ".java", ".c", ".cpp", ".h", ".sh", ".bash", ".zsh", ".php", ".rb",
    ".go", ".rs", ".html", ".htm", ".svg", ".css", ".js", ".ts", ".lua",
}

TEXT_RULES = [
    ("eicar", "malicious", "Тестовый вирус EICAR", 100, re.compile(re.escape(EICAR.decode("ascii")))),
    ("powershell-encoded", "high", "PowerShell с закодированной командой", 50,
     re.compile(r"(?i)(powershell|pwsh)[^\n]{0,80}(-enc|-encodedcommand|frombase64string)")),
    ("powershell-iex", "high", "PowerShell Invoke-Expression", 30,
     re.compile(r"(?i)(invoke-expression|\biex\s*\()")),
    ("download-exec", "high", "Скачивание и запуск кода", 30,
     re.compile(r"(?i)(downloadstring|downloadfile|start-bitstransfer|net\.webclient|urlDownloadToFile)")),
    ("shell-pipe", "high", "Запуск скрипта из интернета", 40,
     re.compile(r"(?i)(curl|wget)[^\n]{0,120}\|\s*(sh|bash|zsh|python|perl|powershell)")),
    ("reverse-shell", "malicious", "Обратная оболочка", 65,
     re.compile(r"(?i)(/dev/tcp/|nc\s+-[a-z]*e\s+|bash\s+-i\s+>&|socket\.socket\([^)]*\).{0,120}(connect|dup2))")),
    ("wscript-shell", "high", "Запуск через WScript.Shell", 25,
     re.compile(r"(?i)(wscript\.shell|shell\.application|createobject\s*\(\s*[\"']wscript)")),
    ("mshta", "high", "Запуск через mshta/rundll32/regsvr32", 25,
     re.compile(r"(?i)(mshta|rundll32|regsvr32)[^\n]{0,120}(http|javascript:|scrobj)")),
    ("certutil-decode", "high", "Certutil/Bitsadmin для декодирования", 25,
     re.compile(r"(?i)(certutil[^\n]{0,80}-decode|bitsadmin[^\n]{0,80}/transfer)")),
    ("shadow-copy", "malicious", "Удаление теневых копий", 70,
     re.compile(r"(?i)(vssadmin[^\n]{0,80}delete[^\n]{0,40}shadows|wbadmin[^\n]{0,80}delete|bcdedit[^\n]{0,80}recoveryenabled\s+no)")),
    ("ransom-note", "high", "Признаки шифровальщика", 45,
     re.compile(r"(?i)(your files (are|have been) encrypted|все ваши файлы зашифрованы|расшифровк[аи][^\n]{0,40}биткоин)")),
    ("startup-persist", "high", "Автозапуск", 25,
     re.compile(r"(?i)(currentversion\\run|schtasks[^\n]{0,80}/create|reg\s+add[^\n]{0,80}run)")),
    ("credential-dump", "malicious", "Кража паролей", 55,
     re.compile(r"(?i)(mimikatz|lsass\.dmp|sekurlsa|procdump[^\n]{0,40}lsass|hashdump)")),
    ("keylogger", "high", "Кейлоггер", 40,
     re.compile(r"(?i)(getasynckeystate|setwindowshookex[^\n]{0,60}keyboard|keylog)")),
    ("macro-autoopen", "high", "Автозапуск макроса", 30,
     re.compile(r"(?i)(autoopen|auto_open|document_open|workbook_open|shell\s*\()")),
    ("pdf-js", "high", "JavaScript в PDF", 30,
     re.compile(r"(?i)(/javascript|/js\s|\bopenaction\b|\blaunch\b|/embeddedfile|/richmedia)")),
    ("html-credential", "high", "Форма ввода пароля", 55,
     re.compile(r"(?i)<input[^>]+type=[\"']?password")),
    ("hidden-iframe", "medium", "Скрытый iframe", 15,
     re.compile(r"(?i)<iframe[^>]+(display\s*:\s*none|width=[\"']?0|height=[\"']?0)")),
    ("obfuscated-js", "medium", "Обфусцированный JavaScript", 15,
     re.compile(r"(?i)(eval\s*\(|atob\s*\(|unescape\s*\(|document\.write\s*\(|\\x[0-9a-f]{2}\\x[0-9a-f]{2})")),
    ("base64-blob", "low", "Большой Base64-блок", 8,
     re.compile(r"[A-Za-z0-9+/]{300,}={0,2}")),
    ("double-extension", "medium", "Двойное расширение", 20,
     re.compile(r"(?i)\.(pdf|docx?|xlsx?|jpg|jpeg|png|txt)\.(exe|scr|bat|cmd|js|vbs|jar|msi|apk)$")),
    ("rlo", "high", "Скрытие расширения RLO", 40,
     re.compile("\u202e")),
    ("wallet-theft", "high", "Кража криптокошелька", 35,
     re.compile(r"(?i)(seed phrase|private key|metamask|wallet connect|подтвердите кошел)")),
]


def _sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _entropy(data: bytes) -> float:
    if not data:
        return 0.0
    counts = [0] * 256
    for byte in data[:200_000]:
        counts[byte] += 1
    total = sum(counts)
    value = 0.0
    for count in counts:
        if count:
            probability = count / total
            value -= probability * math.log2(probability)
    return value


def _detect_type(data: bytes, filename: str) -> str:
    head = data[:16]
    ext = os.path.splitext(filename.lower())[1]
    if data.startswith(b"MZ"):
        return "pe"
    if data.startswith(b"\x7fELF"):
        return "elf"
    if data.startswith(b"PK\x03\x04"):
        if ext == ".apk":
            return "apk"
        if ext in {".docx", ".xlsx", ".pptx", ".odt"}:
            return "office"
        return "zip"
    if data.startswith(b"%PDF"):
        return "pdf"
    if data.startswith(b"\xd0\xcf\x11\xe0"):
        return "ole"
    if data.startswith(b"<svg") or (ext == ".svg" and b"<svg" in head.lower()):
        return "svg"
    if head.lstrip().startswith((b"<!doctype", b"<html", b"<head", b"<body")):
        return "html"
    if ext in {".ps1", ".psm1"}:
        return "powershell"
    if ext in {".sh", ".bash", ".zsh"}:
        return "shell"
    if ext in {".js", ".jse", ".mjs"}:
        return "javascript"
    if ext in {".bat", ".cmd"}:
        return "batch"
    if ext in {".py", ".rb", ".php", ".pl", ".lua"}:
        return "script"
    if ext == ".pdf":
        return "pdf-ish"
    if ext in {".jpg", ".jpeg", ".png", ".gif", ".webp"}:
        return "image"
    return "unknown"


def _add(findings: list[dict], score: int, fid: str, severity: str, title: str,
         detail: str, points: int) -> int:
    findings.append({"id": fid, "severity": severity, "title": title, "detail": detail})
    return min(100, score + points)


def scan_text(text: str, findings: list[dict], score: int, *, label: str = "текст") -> tuple[list[dict], int]:
    for fid, severity, title, points, pattern in TEXT_RULES:
        match = pattern.search(text)
        if not match:
            continue
        excerpt = re.sub(r"\s+", " ", match.group(0))[:160]
        findings.append({
            "id": fid,
            "severity": severity,
            "title": title,
            "detail": label + ": «" + excerpt + "»",
        })
        score = min(100, score + points)
    return findings, score


def _scan_pe(data: bytes, findings: list[dict], score: int) -> tuple[list[dict], int]:
    text = data[:2_000_000].decode("latin-1", "ignore")
    findings, score = scan_text(text, findings, score, label="PE-файл")
    if b"UPX0" in data[:200_000] or b"UPX1" in data[:200_000]:
        findings.append({"id": "packed", "severity": "medium",
                         "title": "Упакованный executable", "detail": "Найдена упаковка UPX."})
        score = min(100, score + 25)
    entropy = _entropy(data)
    if entropy > 7.45 and len(data) > 20_000:
        findings.append({"id": "high-entropy", "severity": "low",
                         "title": "Высокая энтропия", "detail": "Файл может быть зашифрован или упакован."})
        score = min(100, score + 8)
    return findings, score


def _scan_zip(data: bytes, filename: str, findings: list[dict], score: int,
              depth: int = 0) -> tuple[list[dict], int]:
    try:
        archive = zipfile.ZipFile(io.BytesIO(data))
    except Exception:
        return findings, score
    names = archive.namelist()
    for name in names[:MAX_ARCHIVE_ENTRIES]:
        lower = name.lower()
        if ".." in name or name.startswith("/"):
            findings.append({"id": "zip-slip", "severity": "high",
                             "title": "Небезопасный путь в архиве", "detail": name[:200]})
            score = min(100, score + 45)
        ext = os.path.splitext(lower)[1]
        if ext in DANGEROUS_EXT or lower.endswith((".docm", ".xlsm", ".pptm")):
            findings.append({"id": "archive-exec", "severity": "high",
                             "title": "Исполняемый файл внутри архива", "detail": name[:200]})
            score = min(100, score + 35)
        try:
            info = archive.getinfo(name)
        except KeyError:
            continue
        if info.file_size > MAX_ARCHIVE_MEMBER:
            continue
        if info.file_size and info.compress_size:
            if info.file_size / max(1, info.compress_size) > 120 and info.file_size > 200_000:
                findings.append({"id": "zip-bomb", "severity": "medium",
                                 "title": "Подозрительная компрессия", "detail": name[:200]})
                score = min(100, score + 15)
        if depth < 1 and ext in {".zip", ".jar", ".apk"}:
            try:
                nested = archive.read(name)[:MAX_ARCHIVE_MEMBER]
                findings, score = _scan_zip(nested, name, findings, score, depth + 1)
            except Exception:
                pass
        if depth < 1 and ext in TEXT_EXT | {".dex", ".xml", ".json"}:
            try:
                member = archive.read(name)[:MAX_ARCHIVE_MEMBER]
                text = member.decode("utf-8", "ignore")
                findings, score = scan_text(text, findings, score, label=name[:80])
            except Exception:
                pass
    return findings, score


def _scan_office(data: bytes, findings: list[dict], score: int) -> tuple[list[dict], int]:
    text = data[:2_000_000].decode("latin-1", "ignore")
    findings, score = scan_text(text, findings, score, label="документ")
    if b"vbaProject.bin" in data:
        findings.append({"id": "vba-macro", "severity": "high",
                         "title": "VBA-макрос", "detail": "Документ содержит макросы."})
        score = min(100, score + 35)
    if b"AutoOpen" in data or b"Auto_Open" in data or b"Document_Open" in data:
        findings.append({"id": "macro-authorun", "severity": "high",
                         "title": "Макрос автозапуска", "detail": "Макрос запускается при открытии."})
        score = min(100, score + 35)
    return findings, score


def scan_bytes(data: bytes, filename: str = "file") -> dict:
    if len(data) > MAX_SCAN_BYTES:
        data = data[:MAX_SCAN_BYTES]
    findings: list[dict] = []
    score = 0
    kind = _detect_type(data, filename)
    lower_name = filename.lower()
    ext = os.path.splitext(lower_name)[1]

    if EICAR in data:
        findings.append({"id": "eicar", "severity": "malicious",
                         "title": "Тестовый вирус EICAR",
                         "detail": "Файл совпадает с антивирусной тестовой сигнатурой."})
        score = 100
    else:
        if kind == "pe":
            findings, score = _scan_pe(data, findings, score)
        elif kind in {"zip", "apk", "office"}:
            findings, score = _scan_zip(data, filename, findings, score)
            if kind == "office":
                findings, score = _scan_office(data, findings, score)
        elif kind in {"pdf", "pdf-ish"}:
            findings, score = scan_text(data.decode("latin-1", "ignore"), findings, score, label="PDF")
        elif kind == "ole":
            findings, score = _scan_office(data, findings, score)
        elif kind in {"html", "svg"}:
            text = data.decode("utf-8", "ignore")
            findings, score = scan_text(text, findings, score, label=kind.upper())
            if re.search(r"(?i)<input[^>]+type=[\"']?password", text) and re.search(
                r"(?i)<form[^>]+action=[\"']?https?://", text
            ):
                findings.append({"id": "external-credential-form", "severity": "high",
                                 "title": "Пароль отправляется на внешний адрес",
                                 "detail": "Форма похожа на фишинговую."})
                score = min(100, score + 25)
        else:
            text = data.decode("utf-8", "ignore")
            findings, score = scan_text(text, findings, score)

    if ext in DANGEROUS_EXT:
        findings.append({"id": "dangerous-ext", "severity": "medium",
                         "title": "Опасное расширение", "detail": "Файл «" + filename[:120] + "» может запускать код."})
        score = min(100, score + 20)
    if ext in DOC_EXT and kind in {"pe", "elf", "shell", "javascript", "powershell", "batch"}:
        findings.append({"id": "mismatch", "severity": "high",
                         "title": "Подмена типа файла", "detail": "Расширение документа, но содержимое исполняемое."})
        score = min(100, score + 50)
    if lower_name.endswith((".pdf.exe", ".doc.exe", ".docx.exe", ".jpg.exe", ".png.exe", ".txt.exe")):
        findings.append({"id": "double-ext", "severity": "high",
                         "title": "Двойное расширение", "detail": "Файл маскируется под документ."})
        score = min(100, score + 40)

    verdict = "malicious" if score >= 60 else "suspicious" if score >= 25 else "clean"
    return {
        "kind": "file",
        "target": filename,
        "verdict": verdict,
        "score": score,
        "sha256": _sha256(data),
        "md5": hashlib.md5(data).hexdigest(),
        "size": len(data),
        "type": kind,
        "findings": findings,
        "checked_at": int(time.time()),
    }


def scan_base64(content: str, filename: str = "file") -> dict:
    try:
        data = base64.b64decode(re.sub(r"\s+", "", str(content or "")), validate=True)
    except (binascii.Error, ValueError):
        raise ValueError("content_base64 is invalid")
    return scan_bytes(data, filename)
