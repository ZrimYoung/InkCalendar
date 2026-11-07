import catalog from '../generated/catalog.mjs';
import {resolveTheme} from '../domain/model.mjs';

export function drawDateSample(c,r,font,x,y,w,h,column=false){
  c.save();c.fillStyle=c.strokeStyle='#000';c.lineWidth=1;
  const heading=value=>r.caps==='on'?value.toUpperCase():value;
  function text(value,px,py,size,bold=false,align='left',maxWidth=w){
    c.font=`${bold?font.boldWeight:400} ${size}px ${font.family},serif`;
    const ratio=Math.min(1,maxWidth/Math.max(1,c.measureText(value).width));
    c.font=`${bold?font.boldWeight:400} ${size*ratio}px ${font.family},serif`;c.textAlign=align;c.fillText(value,px,py);
  }
  if(r.date==='masthead'){
    text('November',x,y+h*.14,h*.14);c.beginPath();c.moveTo(x,y+h*.24);c.lineTo(x+w,y+h*.24);c.stroke();
    text('07',x,y+h*.77,h*.72,true);c.fillRect(x,y+h*.84,w,h*.16);c.fillStyle='#fff';text(heading('Saturday'),x+w/2,y+h*.96,h*.11,false,'center',w-6);
  }else if(r.date==='tiles'){
    text('0',x+w*.26,y+h*.64,h*.74,true,'center',w*.44);text('7',x+w*.73,y+h*.84,h*.74,true,'center',w*.44);
    text('November',x+w/2,y+h*.99,h*.12,false,'center');
  }else if(r.date==='center'){
    text('07',x+w/2,y+h*.6,h*.72,true,'center');text('November',x+w/2,y+h*.8,h*.12,false,'center');text(heading('Saturday'),x+w/2,y+h*.98,h*.13,false,'center');
  }else if(r.date==='reverse'){
    text('November',x,y+h*.3,h*.14,false,'left',w*.5);text(heading('Saturday'),x,y+h*.58,h*.15,false,'left',w*.5);text('07',x+w,y+h*.75,h*.68,true,'right',w*.46);
  }else if(column){
    text('November',x,y+h*.14,h*.14);text('07',x,y+h*.7,h*.64,true);text(heading('Saturday'),x,y+h*.98,h*.13);
  }else{
    text('07',x,y+h*.74,h*.64,true,'left',w*.48);text('November',x+w*.54,y+h*.3,h*.14,false,'left',w*.46);text(heading('Saturday'),x+w*.54,y+h*.6,h*.15,false,'left',w*.46);
  }
  c.restore();
}

/** Focused vector samples for the same component recipes used by the calendar. */
export function drawComponentPreview(canvas,theme,key,{landscape=false,wide=false,weekly=false,balanced=false}={}) {
  const r=resolveTheme(theme),font=catalog.fonts.find(f=>f.id===theme.font)||catalog.fonts[0],c=canvas.getContext('2d');
  canvas.width=296;canvas.height=192;
  c.setTransform(2,0,0,2,0,0);c.fillStyle='#fff';c.fillRect(0,0,148,96);c.fillStyle=c.strokeStyle='#000';c.lineWidth=1;
  function text(value,x,y,size=8,bold=false,align='left'){
    c.font=`${bold?font.boldWeight:400} ${size}px ${font.family},serif`;c.textAlign=align;c.fillText(value,x,y);
  }
  function line(x,y,end,bottom=y){c.beginPath();c.moveTo(x,y);c.lineTo(end,bottom);c.stroke();}
  function fitted(value,x,y,size,width,align='left'){
    c.font=`400 ${size}px ${font.family},serif`;const factor=Math.min(1,width/Math.max(1,c.measureText(value).width));text(value,x,y,size*factor,false,align);
  }
  function box(x,y,w,h,fill=false){if(fill)c.fillRect(x,y,w,h);else c.strokeRect(x,y,w,h);}
  const heading=value=>r.caps==='on'?value.toUpperCase():value;
  const hero=(x,y,w,column=false)=>drawDateSample(c,r,font,x,13,w,70,column);
  function calendar(x,y,w,h,rows=3){
    const count=weekly?7:rows,step=h/count,col=w/7;
    for(let row=0;row<count;row++){
      if(weekly){
        const selected=row===5,center=x+8,top=y+row*step;
        if(selected&&r.calendar==='ruled'){box(x,top,16,step,true);c.fillStyle='#fff';}
        if(selected&&r.calendar==='open'){c.beginPath();c.arc(center,top+step/2,Math.min(6,step*.42),0,Math.PI*2);c.fill();c.fillStyle='#fff';}
        text(String(row+2),center,top+step*.7,Math.min(8,step*.7),selected,'center');c.fillStyle='#000';
        line(x+25,top+step/2,x+w-3);
        if(selected&&r.calendar==='plain')line(x+2,top+step-1,x+14);
      }else for(let column=0;column<7;column++){
        const selected=row===1&&column===3,center=x+(column+.5)*col,top=y+row*step;
        if(selected&&r.calendar==='ruled'){box(x+column*col,top,col,step,true);c.fillStyle='#fff';}
        if(selected&&r.calendar==='open'){c.beginPath();c.arc(center,top+step/2,Math.min(7,step*.4),0,Math.PI*2);c.fill();c.fillStyle='#fff';}
        text(String(row*7+column+1),center,top+step*.68,Math.min(9,step*.7),selected,'center');c.fillStyle='#000';
        if(selected&&r.calendar==='plain')line(center-5,top+step-2,center+5);
      }
      if(r.calendar==='ruled')line(x,y+(row+1)*step,x+w);
    }
  }
  function agenda(x,y,w,h){
    const step=h/2;
    for(let i=0;i<2;i++){
      const top=y+i*step,time=i?'14:00':'09:30',title=i?'Weekly plan':'Read a chapter';
      if(r.agenda==='cards'){c.setLineDash([3,3]);line(x,top+step-3,x+w);c.setLineDash([]);}
      if(r.agenda==='timeline'){fitted(time,x+3,top+step*.55,7,w*.28-3);fitted(title,x+w*.34,top+step*.55,7.5,w*.66-3);}
      else if(r.agenda==='ledger'){fitted(title,x+3,top+step*.55,7.5,w*.67-3);fitted(time,x+w-3,top+step*.55,7,w*.28-3,'right');}
      else{fitted(time,x+4,top+step*.28,Math.min(7,step*.28),w-8);fitted(title,x+4,top+step*.66,Math.min(8.5,step*.34),w-8);}
    }
  }
  if(key==='date')hero(wide?36:10,wide?14:24,wide?76:128,wide);
  else if(key==='calendar')calendar(10,10,128,76);
  else if(key==='agenda')agenda(10,15,128,68);
  else if(key==='buttons'){
    for(let i=0;i<3;i++){
      const x=23+i*37,solid=r.buttons==='solid';
      if(r.buttons!=='bare')box(x,34,27,27,solid);
      c.strokeStyle=solid?'#fff':'#000';
      if(i===2){box(x+7,41,13,12);line(x+7,45,x+20);}
      else{line(x+7,47,x+20);if(i===1)line(x+13.5,41,x+13.5,54);}
      c.strokeStyle='#000';
    }
  }else if(key==='flow'){
    if(wide&&balanced){
      const first=r.flow==='agenda-first',side=first?10:82,cal=first?82:10;
      text('07',side,32,28,true);agenda(side,43,56,42);calendar(cal,12,56,73);
    }else if(wide){
      if(r.flow==='agenda-first'){text('07',10,34,28,true);agenda(64,10,74,35);calendar(10,55,128,31);}
      else{text('07',10,34,28,true);agenda(10,46,47,40);calendar(66,12,72,74);}
    }else{
      text('07',10,27,25,true);text(heading('Saturday'),65,22,8);
      if(r.flow==='agenda-first'){agenda(10,36,128,24);calendar(10,67,128,23);}
      else{calendar(10,37,128,24);agenda(10,68,128,24);}
    }
  }else{
    const samples=key==='tracking'?[heading('Calendar')]:r.caps==='on'?['NOVEMBER','SATURDAY']:['November','Saturday'];
    const spacing=r.tracking==='spaced'?2:r.tracking==='tight'?-.5:0;
    samples.forEach((value,index)=>{
      c.font=`400 ${key==='tracking'?17:14}px ${font.family},serif`;
      const width=[...value].reduce((sum,ch)=>sum+c.measureText(ch).width,0)+spacing*(value.length-1);
      let x=(148-width)/2;c.textAlign='left';
      for(const ch of value){c.fillText(ch,x,samples.length===1?55:42+index*22);x+=c.measureText(ch).width+spacing;}
    });
  }
}
