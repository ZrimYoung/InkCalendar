import { createServer } from 'node:http';
import { readFile,stat } from 'node:fs/promises';
import { resolve,extname,relative } from 'node:path';
import { webRoot } from './build.mjs';

const base=resolve(webRoot,'dist'),port=Number(process.env.PORT||5173);
const types={'.html':'text/html; charset=utf-8','.js':'text/javascript; charset=utf-8','.css':'text/css; charset=utf-8','.svg':'image/svg+xml','.webmanifest':'application/manifest+json','.txt':'text/plain; charset=utf-8','.ttf':'font/ttf','.otf':'font/otf'};
await stat(resolve(base,'index.html'));
const server=createServer(async(req,res)=>{
  try{
    if(!['GET','HEAD'].includes(req.method)){res.writeHead(405);res.end();return;}
    const url=new URL(req.url,'http://127.0.0.1'),pathName=url.pathname==='/'?'/index.html':url.pathname,file=resolve(base,'.'+decodeURIComponent(pathName));
    const path=relative(base,file);if(path.startsWith('..')){res.writeHead(403);res.end();return;}
    const data=await readFile(file);res.writeHead(200,{'Content-Type':types[extname(file)]||'application/octet-stream','Cache-Control':'no-cache','X-Content-Type-Options':'nosniff'});res.end(req.method==='HEAD'?undefined:data);
  }catch{res.writeHead(404);res.end('Not found');}
});
server.listen(port,'127.0.0.1',()=>console.log(`Local: http://127.0.0.1:${port}/`));
process.on('SIGINT',()=>server.close());process.on('SIGTERM',()=>server.close());
