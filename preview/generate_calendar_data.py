"""Reuse the Android app's offline almanac and holiday tables in the prototype."""
from pathlib import Path
import json
import re

base = Path(__file__).resolve().parent
repo = base.parent
assets = repo / 'app/src/main/assets/calendar'
months = [list(map(int, line.split(','))) for line in (assets / 'months.csv').read_text().splitlines()]
terms = dict((d, int(n)) for d, n in (line.split(',') for line in (assets / 'terms.csv').read_text().splitlines()))
holidays = (repo / 'app/src/main/java/com/inkcalendar/app/Holidays.java').read_text(encoding='utf-8')
rest = re.search(r'REST=\{(.*?)\};', holidays, re.S).group(1)
work = re.search(r'WORK=\{(.*?)\};', holidays, re.S).group(1)
data = {'months': months, 'terms': terms,
        'rest': re.findall(r'\{"([\d-]+)","([\d-]+)"\}', rest),
        'work': re.findall(r'"([\d-]+)"', work)}
source = '// Generated from the packaged Android data. See LICENSE-calendar-data.txt.\n'
source += 'globalThis.INK_CALENDAR_DATA=' + json.dumps(data, separators=(',', ':')) + ';\n'
(base / 'calendar-data.js').write_text(source, encoding='utf-8')
(base / 'LICENSE-calendar-data.txt').write_text((assets / 'LICENSE-lunar-python.txt').read_text(encoding='utf-8'), encoding='utf-8')
print('Updated prototype calendar data')
