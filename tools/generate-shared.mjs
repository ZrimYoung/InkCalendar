import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { dirname, resolve } from 'node:path';
import { createHash } from 'node:crypto';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const check = process.argv.includes('--check');
const read = path => readFile(resolve(root, path), 'utf8');
const raw = await read('shared/calendar-contract.json');
const contract = JSON.parse(raw);
const digest = createHash('sha256').update(raw.replaceAll('\r\n', '\n')).digest('hex').slice(0, 12);
const quoted = value => JSON.stringify(value);
const array = values => `{${values.map(quoted).join(',')}}`;
const matrix = rows => `{${rows.map(array).join(',')}}`;
const keys = contract.components.map(c => c.key);
if (new Set(keys).size !== keys.length) throw new Error('Duplicate component keys');
for (const theme of contract.themes) {
  if (theme.recipe.length !== keys.length) throw new Error(`Invalid recipe: ${theme.id}`);
  theme.recipe.forEach((v, i) => {
    if (!contract.components[i].options.some(o => o.id === v)) throw new Error(`Unknown ${keys[i]} value: ${v}`);
  });
}
const constants = Object.entries(contract.limits).map(([key, value]) => {
  const name = key.replace(/[A-Z]/g, c => '_'+c).toUpperCase();
  return `    static final ${typeof value === 'number' ? 'int' : 'String'} ${name}=${quoted(value)};`;
}).join('\n');
const defaults = Object.entries(contract.defaults).filter(([key]) => key !== 'theme').map(([key,value]) => {
  const name=key.replace(/[A-Z]/g,c=>'_'+c).toUpperCase();
  return `    static final ${typeof value==='number'?'int':typeof value==='boolean'?'boolean':'String'} DEFAULT_${name}=${quoted(value)};`;
}).join('\n');
const java = `// Generated from shared/calendar-contract.json (${digest}). Do not edit.\n`+
`package com.inkcalendar.app;\n\nfinal class SharedThemeCatalog {\n`+
`    private SharedThemeCatalog() {}\n`+
`    static final String DEFAULT_STYLE=${quoted(contract.defaults.theme.style)};\n`+
`    static final String DEFAULT_FONT=${quoted(contract.defaults.theme.font)};\n`+
`    static final String[] FONTS=${array(contract.fonts.map(f => f.id))};\n`+
`    static final String[] FONT_NAMES=${array(contract.fonts.map(f => f.name))};\n`+
`    static final String[] FONT_REGULAR=${array(contract.fonts.map(f => f.regular))};\n`+
`    static final String[] FONT_BOLD=${array(contract.fonts.map(f => f.bold))};\n`+
`    static final boolean[] FONT_SANS={${contract.fonts.map(f => f.sans).join(',')}};\n`+
`    static final String[] STYLES=${array(contract.themes.map(t => t.id))};\n`+
`    static final int[] STYLE_LABELS={${contract.themes.map(t => 'R.string.'+t.resource).join(',')}};\n`+
`    static final int[] RULE_WIDTHS={${contract.themes.map(t => t.ruleWidth).join(',')}};\n`+
`    static final String[] KEYS=${array(keys)};\n`+
`    static final int[] KEY_LABELS={${contract.components.map(c => 'R.string.'+c.resource).join(',')}};\n`+
`    static final String[][] OPTIONS=${matrix(contract.components.map(c => c.options.map(o => o.id)))};\n`+
`    static final int[][] OPTION_LABELS={${contract.components.map(c => '{'+c.options.map(o => 'R.string.'+o.resource).join(',')+'}').join(',')}};\n`+
`    static final String[][] RECIPES=${matrix(contract.themes.map(t => t.recipe))};\n`+
constants+'\n'+defaults+'\n}\n';

function decodeXml(text) {
  return text.replace(/&(?:amp|lt|gt|quot|apos);|&#(?:x[\da-f]+|\d+);/gi, entity => {
    const named = { '&amp;': '&', '&lt;': '<', '&gt;': '>', '&quot;': '"', '&apos;': "'" };
    if (named[entity]) return named[entity];
    return String.fromCodePoint(entity.startsWith('&#x') ? parseInt(entity.slice(3, -1), 16) : parseInt(entity.slice(2, -1), 10));
  }).replaceAll('\\n', '\n').replaceAll("\\'", "'").replaceAll('\\"', '"');
}
function parseStrings(xml) {
  const strings = {};
  for (const match of xml.matchAll(/<string\b[^>]*\bname="([\w]+)"[^>]*>([\s\S]*?)<\/string>/g)) strings[match[1]] = decodeXml(match[2]);
  const plurals = {};
  for (const match of xml.matchAll(/<plurals\b[^>]*\bname="([\w]+)"[^>]*>([\s\S]*?)<\/plurals>/g)) {
    plurals[match[1]] = {};
    for (const item of match[2].matchAll(/<item\b[^>]*\bquantity="([\w]+)"[^>]*>([\s\S]*?)<\/item>/g)) plurals[match[1]][item[1]] = decodeXml(item[2]);
  }
  return { strings, plurals };
}
const strings = {
  en: parseStrings(await read('app/src/main/res/values/strings.xml')),
  zh: parseStrings(await read('app/src/main/res/values-zh/strings.xml')),
};
const months = (await read('app/src/main/assets/calendar/months.csv')).trim().split(/\r?\n/).map(line => line.split(',').map(Number));
const terms = Object.fromEntries((await read('app/src/main/assets/calendar/terms.csv')).trim().split(/\r?\n/).map(line => { const [date, n] = line.split(','); return [date, Number(n)]; }));
const holidays = await read('app/src/main/java/com/inkcalendar/app/Holidays.java');
const rest = [...holidays.match(/REST\s*=\s*\{([\s\S]*?)\};/)[1].matchAll(/\{"([\d-]+)","([\d-]+)"\}/g)].map(m => [m[1], m[2]]);
const work = [...holidays.match(/WORK\s*=\s*\{([\s\S]*?)\};/)[1].matchAll(/"([\d-]+)"/g)].map(m => m[1]);
const outputs = [
  ['app/src/main/java/com/inkcalendar/app/SharedThemeCatalog.java', java],
  ['web/src/generated/catalog.mjs', '// Generated by tools/generate-shared.mjs. Do not edit.\nexport default '+JSON.stringify(contract, null, 2)+';\n'],
  ['web/src/generated/native-strings.mjs', '// Generated from Android string resources. Do not edit.\nexport default '+JSON.stringify(strings, null, 2)+';\n'],
  ['web/src/generated/almanac.mjs', '// Generated from packaged calendar data. See app/src/main/assets/calendar/LICENSE-lunar-python.txt.\nexport default '+JSON.stringify({ months, terms, rest, work })+';\n'],
];
const stale = [];
for (const [path, text] of outputs) {
  const full = resolve(root, path);
  let current;
  try { current = await readFile(full, 'utf8'); } catch (error) { if (error.code !== 'ENOENT') throw error; }
  if (current?.replaceAll('\r\n', '\n') === text) continue;
  if (check) stale.push(path);
  else { await mkdir(dirname(full), { recursive: true }); await writeFile(full, text); }
}
if (stale.length) throw new Error('Generated files are stale; run node tools/generate-shared.mjs:\n'+stale.join('\n'));
console.log(check ? 'Shared definitions are current.' : 'Shared definitions generated.');
