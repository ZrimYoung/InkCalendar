"""Create static, renamed instances for Android 4.4 (fonttools 4.60.1)."""
from pathlib import Path
import argparse
import shutil
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

parser = argparse.ArgumentParser()
parser.add_argument('sources', type=Path)
args = parser.parse_args()
destination = Path(__file__).resolve().parents[1] / 'app/src/main/assets/fonts'
instances = [
    ('sourceserif4-SourceSerif4[opsz,wght].ttf', 'NewsprintDisplay', 'Regular', {'wght': 400, 'opsz': 20}),
    ('sourceserif4-SourceSerif4[opsz,wght].ttf', 'NewsprintDisplay', 'Semibold', {'wght': 650, 'opsz': 20}),
    ('sourceserif4-SourceSerif4[opsz,wght].ttf', 'NewsprintDisplay', 'Black', {'wght': 900, 'opsz': 60}),
    ('ebgaramond-EBGaramond[wght].ttf', 'NewsprintReading', 'Regular', {'wght': 400}),
]
for source, family, style, axes in instances:
    font = instantiateVariableFont(TTFont(args.sources / source), axes, inplace=True)
    name = family + '-' + style
    # Retain OFL/copyright metadata; derived binaries use distinct family names.
    names = {1: family, 2: style, 3: name + '-InkCalendar-1', 4: family + ' ' + style,
             6: name, 16: family, 17: style}
    for record in list(font['name'].names):
        if record.nameID in names:
            font['name'].setName(names[record.nameID], record.nameID,
                                 record.platformID, record.platEncID, record.langID)
    assert 'fvar' not in font
    font.save(destination / (name + '.ttf'))
    print(name)
for family in ('sourceserif4', 'ebgaramond'):
    shutil.copyfile(args.sources / (family + '-OFL.txt'), destination / (family + '-OFL.txt'))
