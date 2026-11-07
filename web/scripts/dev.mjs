import { resolve } from 'node:path';
import { readFile } from 'node:fs/promises';
import * as esbuild from 'esbuild';
import { webRoot,repoRoot,generate,prepareAssets,buildOptions } from './build.mjs';

const outDir=resolve(webRoot,'.cache/dev'),port=Number(process.env.PORT||5173);
await generate();await prepareAssets(outDir,{dev:true});
const watched=['shared/calendar-contract.json','app/src/main/res/values/strings.xml','app/src/main/res/values-zh/strings.xml','app/src/main/assets/calendar/months.csv','app/src/main/assets/calendar/terms.csv','app/src/main/java/com/inkcalendar/app/Holidays.java','web/index.html','web/public/icon.svg','web/public/manifest.webmanifest'].map(path=>resolve(repoRoot,path));
const ctx=await esbuild.context({...buildOptions(outDir,{dev:true}),plugins:[{name:'shared-sources',setup(build){build.onStart(async()=>{await generate();await prepareAssets(outDir,{dev:true});});build.onLoad({filter:/generated[\\/].*\.mjs$/},async args=>({contents:await readFile(args.path,'utf8'),loader:'js',watchFiles:watched}));}}]});
await ctx.watch();await ctx.serve({host:'127.0.0.1',port,servedir:outDir});
console.log(`Local: http://127.0.0.1:${port}/`);
console.log(`Prototype: http://127.0.0.1:${port}/?demo=1`);
async function stop(){await ctx.dispose();process.exit(0);}
process.on('SIGINT',stop);process.on('SIGTERM',stop);
