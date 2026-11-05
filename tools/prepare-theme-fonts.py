"""Download official fonts and build renamed static instances for Android 4.4.

Usage: PYTHONPATH=<fonttools directory> python3 tools/prepare-theme-fonts.py <source-cache>
The first run resolves upstream revisions; subsequent runs reuse font-sources.json.
"""
from pathlib import Path
import argparse
import hashlib
import json
import re
import subprocess
import urllib.parse
import urllib.request
from fontTools.ttLib import TTFont
from fontTools.varLib.instancer import instantiateVariableFont

parser=argparse.ArgumentParser()
parser.add_argument('cache',type=Path)
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
destination=root/'app/src/main/assets/fonts'
destination.mkdir(parents=True,exist_ok=True)
args.cache.mkdir(parents=True,exist_ok=True)
manifest=root/'tools/theme-font-sources.json'

def get(url):
    return subprocess.check_output(['curl','--fail','--location','--retry','2','--connect-timeout','15','--max-time','180','--silent','--show-error',url])

if manifest.exists():
    lock=json.loads(manifest.read_text())
else:
    lock={repo:subprocess.check_output(['git','ls-remote','https://github.com/'+repo+'.git','HEAD'],text=True).split()[0] for repo in ['google/fonts','notofonts/noto-cjk']}
    lock['files']={}
    manifest.write_text(json.dumps(lock,indent=2)+'\n')

def cached(repo,path):
    key=repo+'/'+path
    target=args.cache/(hashlib.sha256(key.encode()).hexdigest()[:12]+'-'+Path(path).name)
    if not target.exists():target.write_bytes(get('https://raw.githubusercontent.com/'+repo+'/'+lock[repo]+'/'+urllib.parse.quote(path)))
    data=target.read_bytes();digest=hashlib.sha256(data).hexdigest()
    if key in lock['files'] and lock['files'][key]!=digest:raise ValueError('Source checksum changed: '+key)
    lock['files'][key]=digest
    manifest.write_text(json.dumps(lock,indent=2)+'\n')
    return target

families=[('ofl/geist','InkGeist',700),('ofl/librebaskerville','InkBaskerville',700),('ofl/fraunces','InkFraunces',600),('apache/robotoslab','InkSlab',600),('ofl/spacegrotesk','InkSpace',500),('ofl/atkinsonhyperlegiblenext','InkAtkinson',700)]
for folder,family,weight in families:
    metadata=cached('google/fonts',folder+'/METADATA.pb').read_text()
    metadata_family=re.search(r'^name: "([^"]+)"',metadata).group(1)
    assert re.sub(r'[^a-z]','',metadata_family.lower())==folder.split('/')[-1],(folder,metadata_family)
    source=next(name for name in re.findall(r'filename: "([^"]+)"',metadata) if name.endswith('.ttf') and 'Italic' not in name)
    path=cached('google/fonts',folder+'/'+source)
    license='LICENSE.txt' if folder.startswith('apache/') else 'OFL.txt'
    (destination/(folder.split('/')[-1]+'-'+license)).write_bytes(cached('google/fonts',folder+'/'+license).read_bytes())
    for style,w in [('Regular',400),('Bold',weight)]:
        font=TTFont(path)
        assert re.sub(r'[^a-z]','',font['name'].getBestFamilyName().lower())==folder.split('/')[-1],(folder,font['name'].getBestFamilyName())
        axes={}
        if 'fvar' in font:
            axes={axis.axisTag:(w if axis.axisTag=='wght' else axis.defaultValue) for axis in font['fvar'].axes}
            font=instantiateVariableFont(font,axes,inplace=True)
        name=family+'-'+style
        names={1:family,2:style,3:name+'-InkCalendar-1',4:family+' '+style,6:name,16:family,17:style}
        for record in list(font['name'].names):
            if record.nameID in names:font['name'].setName(names[record.nameID],record.nameID,record.platformID,record.platEncID,record.langID)
        font['name'].setName('Modified for InkCalendar: renamed static instance; axes '+str(axes)+'. Original copyright and license retained.',10,3,1,0x409)
        assert 'fvar' not in font
        font.save(destination/(name+'.ttf'))
    print('Prepared',family,flush=True)
(destination/'NotoSansSC-Regular.otf').write_bytes(cached('notofonts/noto-cjk','Sans/SubsetOTF/SC/NotoSansSC-Regular.otf').read_bytes())
(destination/'notosanssc-OFL.txt').write_bytes(cached('notofonts/noto-cjk','Sans/LICENSE').read_bytes())
print('Prepared Noto Sans SC; pinned source revisions and checksums:',manifest,flush=True)
assert len({hashlib.sha256((destination/(family+'-Regular.ttf')).read_bytes()).hexdigest() for _,family,_ in families})==len(families)
