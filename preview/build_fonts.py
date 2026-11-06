"""Build offline WOFF2 subsets from the same fonts packaged in the Android app.

Optional development dependencies: fonttools and brotli.
"""
from pathlib import Path
from io import BytesIO
import base64
from fontTools import subset
from fontTools.ttLib import TTFont

base = Path(__file__).resolve().parent
assets = base.parent / 'app/src/main/assets/fonts'
source = ''.join((base / name).read_text(encoding='utf-8') for name in
                 ['ink-calendar-preview.html', 'ink-calendar-preview.css', 'ink-calendar-preview.js'])
characters = ''.join(sorted(set(source + ''.join(chr(i) for i in range(32, 127)))))
specs = [('InkReading', 'NewsprintReading-Regular.ttf', 400),
         ('InkOriginal', 'NewsprintDisplay-Regular.ttf', 400),
         ('InkOriginal', 'NewsprintDisplay-Black.ttf', 900),
         ('InkCJKSerif', 'NotoSerifSC-Regular.otf', 400),
         ('InkCJKSans', 'NotoSansSC-Regular.otf', 400)]
for family in ['InkGeist', 'InkBaskerville', 'InkFraunces', 'InkSlab', 'InkSpace', 'InkAtkinson']:
    specs.extend([(family, family+'-Regular.ttf', 400), (family, family+'-Bold.ttf', 700)])
css = []
for family, filename, weight in specs:
    font = TTFont(assets / filename)
    options = subset.Options()
    options.flavor = 'woff2'
    options.name_IDs = ['*']
    options.name_legacy = True
    options.name_languages = ['*']
    sub = subset.Subsetter(options=options)
    sub.populate(text=characters)
    sub.subset(font)
    for record in font['name'].names:
        if record.nameID in [1, 3, 4, 6, 16, 25]:
            record.string = ('InkDemo'+family+str(weight)).encode(record.getEncoding(), errors='replace')
    font.flavor = 'woff2'
    output = BytesIO()
    font.save(output)
    data = base64.b64encode(output.getvalue()).decode('ascii')
    css.append(f"@font-face{{font-family:{family};font-weight:{weight};font-display:swap;src:url(data:font/woff2;base64,{data}) format('woff2')}}")
for filename in ['OFL.txt', 'sourceserif4-OFL.txt', 'ebgaramond-OFL.txt', 'notosanssc-OFL.txt',
                 'geist-OFL.txt', 'librebaskerville-OFL.txt', 'fraunces-OFL.txt',
                 'robotoslab-LICENSE.txt', 'spacegrotesk-OFL.txt', 'atkinsonhyperlegiblenext-OFL.txt']:
    license_text='\n'.join(line.rstrip() for line in (assets/filename).read_text(encoding='utf-8').splitlines())
    css.append('/* Font license: '+filename+'\n'+license_text.replace('*/','* /')+'\n*/')
(base / 'fonts.css').write_text('\n'.join(css)+'\n', encoding='utf-8')
print('Updated', len(specs), 'offline font subsets')
