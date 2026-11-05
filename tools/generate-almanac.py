#!/usr/bin/env python3
"""Build-time only: pip install lunar_python==1.4.8. No Python dependency in APK.
Month starts and solar term dates follow the library's China calendar (UTC+8).
"""
from pathlib import Path
from datetime import date, timedelta
from lunar_python import LunarYear, Solar
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'app/src/main/assets/calendar'
OUT.mkdir(parents=True,exist_ok=True)
EPOCH=date(1900,1,1)
months={}
terms={}
NAMES='小寒 大寒 立春 雨水 惊蛰 春分 清明 谷雨 立夏 小满 芒种 夏至 小暑 大暑 立秋 处暑 白露 秋分 寒露 霜降 立冬 小雪 大雪 冬至'.split()
for year in range(1899,2102):
    for m in LunarYear.fromYear(year).getMonths():
        start=Solar.fromJulianDay(m.getFirstJulianDay())
        day=date(start.getYear(),start.getMonth(),start.getDay())
        months[day]=(m.getYear(),m.getMonth(),m.getDayCount())
    for name, solar in Solar.fromYmd(year,7,1).getLunar().getJieQiTable().items():
        if name not in NAMES: continue
        day=date(solar.getYear(),solar.getMonth(),solar.getDay())
        if 1900<=day.year<=2100: terms[day]=NAMES.index(name)
(OUT/'months.csv').write_text(''.join(f'{(d-EPOCH).days},{y},{m},{n}\n' for d,(y,m,n) in sorted(months.items())),encoding='utf-8')
(OUT/'terms.csv').write_text(''.join(f'{d.isoformat()},{idx}\n' for d,idx in sorted(terms.items())),encoding='utf-8')
assert len(terms)==201*24, len(terms)
print(f'{len(months)} lunar months; {len(terms)} solar terms')
