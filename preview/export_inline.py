"""Combine the editable browser prototype into one conversation preview fragment."""
from pathlib import Path
import sys

base = Path(__file__).resolve().parent
page = (base / 'ink-calendar-preview.html').read_text(encoding='utf-8')
markup = page.split('<!-- BEGIN PROTOTYPE -->', 1)[1].split('<!-- END PROTOTYPE -->', 1)[0].strip()
css = '\n'.join((base / name).read_text(encoding='utf-8') for name in ['fonts.css', 'ink-calendar-preview.css'])
js = '\n'.join((base / name).read_text(encoding='utf-8') for name in ['calendar-data.js', 'ink-calendar-preview.js'])
fragment = '<style>\n' + css + '\n</style>\n' + markup + '\n<script>\n' + js + '\n</script>\n'
assert len(fragment.encode('utf-8')) < 1_000_000
if len(sys.argv) != 2:
    raise SystemExit('Usage: python preview/export_inline.py OUTPUT.html')
Path(sys.argv[1]).write_text(fragment, encoding='utf-8')
print('Exported inline preview')
