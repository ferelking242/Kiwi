#!/usr/bin/env python3
"""Validate all XML resource files in eterna/res/ are well-formed."""
import sys, xml.etree.ElementTree as ET
from pathlib import Path

RES_DIR = Path(__file__).resolve().parent.parent / 'res'
errors = []

for xml_file in RES_DIR.rglob('*.xml'):
    try:
        ET.parse(xml_file)
    except ET.ParseError as e:
        errors.append(f"{xml_file}: {e}")

if errors:
    for e in errors: print(f"INVALID XML: {e}", file=sys.stderr)
    sys.exit(1)
print(f"All {sum(1 for _ in RES_DIR.rglob('*.xml'))} XML files are valid.")
