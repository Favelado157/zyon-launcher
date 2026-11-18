'use strict';
const $ = selector => document.querySelector(selector);
const $$ = selector => [...document.querySelectorAll(selector)];
const menu = $('#mainNav'), menuToggle = $('#menuToggle');
function closeMenu(){menu.classList.remove('open');menuToggle.setAttribute('aria-expanded','false');menuToggle.setAttribute('aria-label','Abrir navegação');}
menuToggle.addEventListener('click',()=>{const open=menu.classList.toggle('open');menuToggle.setAttribute('aria-expanded',String(open));menuToggle.setAttribute('aria-label',open?'Fechar navegação':'Abrir navegação');});
menu.querySelectorAll('a').forEach(a=>a.addEventListener('click',closeMenu));
document.addEventListener('keydown',e=>{if(e.key==='Escape'&&menu.classList.contains('open')){closeMenu();menuToggle.focus();}});

const search=$('#moduleSearch'), modules=$$('.module-card');let category='all';
const normalize=value=>value.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
function filterModules(){const query=normalize(search.value.trim());let visible=0;for(const card of modules){const show=(category==='all'||card.dataset.category===category)&&normalize(card.textContent).includes(query);card.hidden=!show;if(show)visible++;}$('#moduleCount').textContent=visible===modules.length?`${modules.length} módulos disponíveis`:`${visible} de ${modules.length} módulos`;
 $('#noModules').hidden=visible>0;
}
$$('[data-filter]').forEach(button=>button.addEventListener('click',()=>{category=button.dataset.filter;$$('[data-filter]').forEach(item=>{const selected=item===button;item.classList.toggle('active',selected);item.setAttribute('aria-pressed',String(selected));});filterModules();}));
search.addEventListener('input',filterModules);
$('#resetFilters').addEventListener('click',()=>{search.value='';$('[data-filter="all"]').click();search.focus();});

const stage=$('#hudStage'),widget=$('#hudWidget'),scaleControl=$('#hudScale');let pos={x:32,y:30},scale=1,drag=null;
function clampHud(){pos.x=Math.max(0,Math.min(pos.x,Math.max(0,stage.clientWidth-widget.offsetWidth*scale)));pos.y=Math.max(0,Math.min(pos.y,Math.max(0,stage.clientHeight-widget.offsetHeight*scale)));widget.style.left=pos.x+'px';widget.style.top=pos.y+'px';widget.style.transform=`scale(${scale})`;}
widget.addEventListener('pointerdown',event=>{if(event.button!==0||event.target.closest('button'))return;const rect=stage.getBoundingClientRect();drag={id:event.pointerId,dx:event.clientX-rect.left-pos.x,dy:event.clientY-rect.top-pos.y};widget.setPointerCapture(event.pointerId);widget.classList.add('dragging');widget.focus({preventScroll:true});event.preventDefault();});
widget.addEventListener('pointermove',event=>{if(!drag||event.pointerId!==drag.id)return;const rect=stage.getBoundingClientRect();pos.x=event.clientX-rect.left-drag.dx;pos.y=event.clientY-rect.top-drag.dy;clampHud();});
function stopDrag(){drag=null;widget.classList.remove('dragging');}
widget.addEventListener('pointerup',stopDrag);widget.addEventListener('pointercancel',stopDrag);widget.addEventListener('lostpointercapture',stopDrag);
widget.addEventListener('keydown',event=>{if(event.target!==widget)return;const d={ArrowLeft:[-5,0],ArrowRight:[5,0],ArrowUp:[0,-5],ArrowDown:[0,5]}[event.key];if(d){event.preventDefault();pos.x+=d[0];pos.y+=d[1];clampHud();}const key=event.key.toLowerCase();if(['w','a','s','d',' '].includes(key)){event.preventDefault();$$('[data-key]').filter(el=>el.dataset.key===key).forEach(el=>el.classList.add('pressed'));}});
widget.addEventListener('keyup',event=>{$$('[data-key]').filter(el=>el.dataset.key===event.key.toLowerCase()).forEach(el=>el.classList.remove('pressed'));});
widget.addEventListener('blur',()=>$$('[data-key]').forEach(el=>el.classList.remove('pressed')));
const clicks=[[],[]];function updateCps(){const now=Date.now();clicks.forEach((list,i)=>{while(list.length&&list[0]<=now-1000)list.shift();$(i?'#rightCps':'#leftCps').textContent=list.length;});}
$$('[data-mouse]').forEach(button=>button.addEventListener('click',()=>{const i=Number(button.dataset.mouse);clicks[i].push(Date.now());updateCps();button.classList.add('pressed');setTimeout(()=>button.classList.remove('pressed'),120);}));
setInterval(updateCps,200);
const colors={violet:'#bba1ff',cyan:'#58d9e8',mint:'#72d9a0'};
$$('[data-color]').forEach(button=>button.addEventListener('click',()=>{$('.demo-shell').style.setProperty('--hud-color',colors[button.dataset.color]);$$('[data-color]').forEach(item=>{const selected=item===button;item.classList.toggle('selected',selected);item.setAttribute('aria-pressed',String(selected));});}));
scaleControl.addEventListener('input',()=>{scale=Number(scaleControl.value)/100;$('#hudScaleValue').textContent=scaleControl.value+'%';clampHud();});
$('#resetHud').addEventListener('click',()=>{pos={x:32,y:30};scale=1;scaleControl.value='100';$('#hudScaleValue').textContent='100%';$('[data-color="violet"]').click();clampHud();});
if('ResizeObserver' in window)new ResizeObserver(clampHud).observe(stage);else window.addEventListener('resize',clampHud);clampHud();

function validRelease(release){
 if(release?.draft||release?.prerelease||!/^v\d+\.\d+\.\d+$/.test(release?.tag_name||''))return null;
 const version=release.tag_name.slice(1),name=`Zyon-Launcher-Setup-${version}.exe`;
 const file=release.assets?.find(asset=>asset.name===name);
 if(!file)return null;
 try{const url=new URL(file.browser_download_url);if(url.origin!=='https://github.com'||!url.pathname.startsWith('/Favelado157/zyon-launcher/releases/download/'))return null;}catch{return null;}
 return {version,url:file.browser_download_url};
}
async function loadRelease(){
 try{
  const response=await fetch('https://api.github.com/repos/Favelado157/zyon-launcher/releases/latest',{headers:{Accept:'application/vnd.github+json'},signal:AbortSignal.timeout(7000)});
  if(!response.ok)return;
  const release=validRelease(await response.json());if(!release)return;
  $$('[data-version]').forEach(element=>element.textContent='v'+release.version);
  $$('[data-download]').forEach(element=>element.href=release.url);
  $('#releaseStatus').textContent='Versão '+release.version+' disponível. O download abre o instalador publicado no GitHub.';
 }catch{/* The stable releases/latest links remain usable when the API is unavailable. */}
}
loadRelease();
