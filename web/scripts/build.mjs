import { readFile,writeFile,mkdir,cp,rm,readdir,stat } from 'node:fs/promises';
import { dirname,resolve,relative,basename,join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { createHash } from 'node:crypto';
import * as esbuild from 'esbuild';

export const webRoot=resolve(dirname(fileURLToPath(import.meta.url)),'..');
export const repoRoot=resolve(webRoot,'..');
export async function generate(){await import(new URL('../../tools/generate-shared.mjs?run='+Date.now(),import.meta.url));}
export async function prepareAssets(outDir,{dev=false}={}){
  const target=resolve(outDir),within=relative(webRoot,target);
  if(!within||within.startsWith('..')||!['dist','.cache'].includes(within.split(/[\\/]/)[0]))throw new Error('Build output must stay under web/dist or web/.cache.');
  await mkdir(target,{recursive:true});
  await cp(resolve(webRoot,'public'),target,{recursive:true});
  await cp(resolve(webRoot,'index.html'),resolve(target,'index.html'));
  const catalog=JSON.parse(await readFile(resolve(repoRoot,'shared/calendar-contract.json'),'utf8'));
  const source=resolve(repoRoot,'app/src/main/assets/fonts'),fontDir=resolve(target,'fonts');await mkdir(fontDir,{recursive:true});
  const faces=catalog.fonts.flatMap(f=>[{family:f.family,file:f.regular,weight:400},{family:f.family,file:f.bold,weight:f.boldWeight}]);
  faces.push({family:'InkOriginal',file:'NewsprintDisplay-Semibold.ttf',weight:700},{family:'InkReading',file:'NewsprintReading-Regular.ttf',weight:400},{family:'InkCJKSerif',file:'NotoSerifSC-Regular.otf',weight:400},{family:'InkCJKSans',file:'NotoSansSC-Regular.otf',weight:400});
  for(const {file} of faces){if(basename(file)!==file)throw new Error('Invalid font asset');await cp(resolve(source,file),resolve(fontDir,file));}
  for(const name of await readdir(source))if(name.endsWith('.txt'))await cp(resolve(source,name),resolve(fontDir,name));
  const css=faces.map(f=>`@font-face{font-family:${f.family};font-weight:${f.weight};font-display:swap;src:url('./fonts/${f.file}') format('${f.file.endsWith('.otf')?'opentype':'truetype'}')}`).join('\n');
  await writeFile(resolve(target,'fonts.css'),css+'\n');
  await cp(resolve(repoRoot,'app/src/main/assets/calendar/LICENSE-lunar-python.txt'),resolve(target,'LICENSE-calendar-data.txt'));
  await cp(resolve(webRoot,'node_modules/lucide/LICENSE'),resolve(target,'LICENSE-lucide.txt'));
  if(dev)await writeFile(resolve(target,'sw.js'),`self.addEventListener('install',()=>self.skipWaiting());self.addEventListener('activate',e=>e.waitUntil(self.registration.unregister().then(()=>self.clients.matchAll({type:'window'})).then(clients=>Promise.all(clients.map(c=>c.navigate(c.url))))));\n`);
}
export function buildOptions(outDir,{dev=false}={}){
  return {absWorkingDir:webRoot,entryPoints:{app:'src/main.mjs'},outdir:outDir,bundle:true,format:'esm',target:'es2022',charset:'utf8',minify:!dev,sourcemap:dev?'inline':false,legalComments:'eof',define:{__DEV__:String(dev)}};
}
async function listFiles(base,path=''){
  const files=[];for(const name of await readdir(resolve(base,path))){const item=join(path,name);if((await stat(resolve(base,item))).isDirectory())files.push(...await listFiles(base,item));else files.push(item.replaceAll('\\','/'));}return files.sort();
}
export async function writeWorker(outDir){
  const files=(await listFiles(outDir)).filter(f=>f!=='sw.js');const hash=createHash('sha256');
  for(const file of files)hash.update(file).update(await readFile(resolve(outDir,file)));
  const cache='ink-calendar-web-'+hash.digest('hex').slice(0,16);
  const source=`const CACHE=${JSON.stringify(cache)},BASE=new URL('./',self.location.href),FILES=${JSON.stringify(files)};\n`+
  `self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(FILES.map(file=>new Request(new URL(file,BASE),{cache:'reload'}))))));\n`+
  `self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(key=>key.startsWith('ink-calendar-web-')&&key!==CACHE).map(key=>caches.delete(key)))).then(()=>self.clients.claim())));\n`+
  `self.addEventListener('fetch',event=>{const request=event.request,url=new URL(request.url);if(request.method!=='GET'||url.origin!==BASE.origin)return;if(request.mode==='navigate'){event.respondWith(caches.open(CACHE).then(cache=>cache.match(new URL('index.html',BASE))).then(cached=>cached||fetch(request)));return;}if(FILES.some(file=>new URL(file,BASE).pathname===url.pathname))event.respondWith(caches.open(CACHE).then(cache=>cache.match(request,{ignoreSearch:true})).then(cached=>cached||fetch(request)));});\n`;
  await writeFile(resolve(outDir,'sw.js'),source);
}
export async function buildPrototype(){
  const target=resolve(repoRoot,'preview');
  await esbuild.build({absWorkingDir:webRoot,entryPoints:['src/prototype-entry.mjs'],outfile:resolve(target,'ink-calendar-preview.js'),bundle:true,format:'iife',target:'es2022',charset:'utf8',minify:true,legalComments:'eof',banner:{js:'// Generated from web/src. Run npm --prefix web run build; do not edit this bundle.'}});
  await cp(resolve(webRoot,'src/ui/calendar.css'),resolve(target,'ink-calendar-preview.css'));
}
export async function buildWeb({outDir=resolve(webRoot,'dist')}={}){
  const target=resolve(outDir),path=relative(webRoot,target);
  if(!path||path.startsWith('..')||!['dist','.cache'].includes(path.split(/[\\/]/)[0]))throw new Error('Invalid build output');
  await rm(target,{recursive:true,force:true});await generate();await prepareAssets(target);await esbuild.build(buildOptions(target));await writeWorker(target);await buildPrototype();
  console.log('Built Web client and shared browser prototype.');
}
if(process.argv[1]&&resolve(process.argv[1])===fileURLToPath(import.meta.url))await buildWeb();
