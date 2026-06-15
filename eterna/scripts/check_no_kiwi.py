#!/usr/bin/env python3
"""Ensure no Kiwi-branded strings remain in Eterna overlay files."""
import sys, re
from pathlib import Path

ETERNA_DIR = Path(__file__).resolve().parent.parent
KIWI_PATTERNS = [
    r'com\.kiwibrowser\.browser',
    r'Kiwi Browser',
    r'KiwiBrowser',
]
errors = []

for fpath in ETERNA_DIR.rglob('*'):
    if not fpath.is_file(): continue
    if fpath.suffix not in {'.xml', '.java', '.kt', '.gni', '.gn', '.gradle'}: continue
    text = fpath.read_text(encoding='utf-8', errors='ignore')
    for p in KIWI_PATTERNS:
        if re.search(p, text):
            errors.append(f"{fpath.relative_to(ETERNA_DIR)}: found pattern '{p}'")

if errors:
    for e in errors: print(f"KIWI_REMNANT: {e}", file=sys.stderr)
    sys.exit(1)
print("No Kiwi remnants found in Eterna overlays.")
