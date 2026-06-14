#!/usr/bin/env python3
"""
Eterna Browser — Branding Patch Script
Replaces all Kiwi Browser references with Eterna branding.
Run after gclient sync, before gn gen.
"""
import os, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent

REPLACEMENTS = [
    (r'com\.kiwibrowser\.browser', 'com.aivos.eterna'),
    (r'com/kiwibrowser/browser',     'com/aivos/eterna'),
    (r'"Kiwi"',                      '"Eterna"'),
    (r'>Kiwi Browser<',              '>Eterna<'),
    (r'>Kiwi<',                      '>Eterna<'),
    (r'Kiwi Browser',                'Eterna Browser'),
    (r'KiwiBrowser',                 'EternaBrowser'),
    (r'kiwibrowser',                 'eterna'),
    (r'arnaud@geeksville\.com',     'contact@aivos.io'),
    (r'Arnaud Granal',               'AIVOS'),
]

EXTENSIONS = {'.xml','.java','.kt','.gni','.gn','.gradle','.json','.properties','.md','.txt','.cfg'}
SKIP_DIRS  = {'.git','out','build','node_modules','.cipd'}

def patch_file(fpath: Path) -> bool:
    try: text = fpath.read_text(encoding='utf-8', errors='ignore')
    except Exception: return False
    patched = text
    for pattern, replacement in REPLACEMENTS:
        patched = re.sub(pattern, replacement, patched)
    if patched != text:
        fpath.write_text(patched, encoding='utf-8')
        return True
    return False

def main():
    changed = 0
    for dirpath, dirnames, filenames in os.walk(ROOT):
        dirnames[:] = [d for d in dirnames if d not in SKIP_DIRS]
        for fname in filenames:
            fpath = Path(dirpath) / fname
            if fpath.suffix in EXTENSIONS:
                if patch_file(fpath): changed += 1
    print(f"Eterna branding applied: {changed} files patched.")
    return 0

if __name__ == '__main__':
    sys.exit(main())
