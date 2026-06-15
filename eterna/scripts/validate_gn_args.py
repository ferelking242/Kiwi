#!/usr/bin/env python3
"""Validate that Eterna build config GN args are correctly set."""
import sys
from pathlib import Path

CONFIG = Path(__file__).resolve().parent.parent / 'config' / 'eterna_build_config.gni'
if not CONFIG.exists():
    print(f"ERROR: {CONFIG} not found", file=sys.stderr)
    sys.exit(1)

text = CONFIG.read_text(encoding='utf-8')
required = {
    'target_cpu = "arm64"',
    'com.aivos.eterna',
    'is_official_build = true',
}
missing = [r for r in required if r not in text]
if missing:
    for m in missing:
        print(f"MISSING in GN config: {m}", file=sys.stderr)
    sys.exit(1)
print("GN args validation passed.")
