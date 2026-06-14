#!/usr/bin/env python3
"""
Verify Eterna overlay files have correct branding.
"""
import sys
from pathlib import Path

ETERNA_DIR = Path(__file__).resolve().parent.parent

REQUIRED = {
    'eterna/res/values/strings.xml': ['com.aivos.eterna', 'Eterna'],
    'eterna/res/values/channel_constants.xml': ['Eterna'],
    'eterna/config/eterna_build_config.gni': ['com.aivos.eterna'],
}

errors = []
for path, keywords in REQUIRED.items():
    p = ETERNA_DIR.parent / path
    if not p.exists():
        errors.append(f"MISSING: {path}")
        continue
    text = p.read_text(encoding='utf-8', errors='ignore')
    for kw in keywords:
        if kw not in text:
            errors.append(f"MISSING keyword '{kw}' in {path}")

if errors:
    for e in errors:
        print(f"ERROR: {e}", file=sys.stderr)
    sys.exit(1)
print("Branding check passed.")
