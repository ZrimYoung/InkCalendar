export function browserEffects(report=()=>{}) {
  let wake=null,options={awake:false,full:false};
  const abort=new AbortController();
  const capabilities={full:Boolean(document.fullscreenEnabled&&document.documentElement.requestFullscreen),awake:Boolean(navigator.wakeLock&&window.isSecureContext)};
  const message=()=>options.language==='en'?'The browser could not enable this display option.':'浏览器未能开启此显示选项。';
  async function wakeScreen(silent=false){
    if(!options.awake||!capabilities.awake||document.visibilityState!=='visible'||wake)return;
    try{const lock=await navigator.wakeLock.request('screen');if(!options.awake||document.visibilityState!=='visible'){await lock.release();return;}wake=lock;wake.addEventListener('release',()=>{wake=null;},{once:true});}
    catch{if(!silent)report(message());}
  }
  document.addEventListener('visibilitychange',()=>{if(document.visibilityState==='visible')void wakeScreen(true);},{signal:abort.signal});
  return {
    capabilities,
    prepare(next){
      if(next.full&&capabilities.full&&!document.fullscreenElement)void document.documentElement.requestFullscreen().catch(()=>report(message()));
      else if(!next.full&&document.fullscreenElement)void document.exitFullscreen().catch(()=>report(message()));
    },
    apply(next,{silent=false}={}){
      options={...next};if(!next.awake&&wake){void wake.release().catch(()=>{});wake=null;}else void wakeScreen(silent);
    },
    dispose(){abort.abort();if(wake)void wake.release().catch(()=>{});},
  };
}
