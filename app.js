'use strict';
const toggle=document.getElementById('menuToggle'),navigation=document.getElementById('mainNav');
function closeNavigation(){navigation.classList.remove('open');toggle.setAttribute('aria-expanded','false');}
toggle.addEventListener('click',()=>{const open=navigation.classList.toggle('open');toggle.setAttribute('aria-expanded',String(open));});
navigation.addEventListener('click',event=>{if(event.target.closest('a'))closeNavigation();});
document.addEventListener('keydown',event=>{if(event.key==='Escape')closeNavigation();});

function openGuideTopic(hash){
  if(!hash.startsWith('#guia-'))return;
  const topic=document.getElementById(hash.slice(1));
  if(topic?.matches('.guide-content details'))topic.open=true;
}
document.addEventListener('click',event=>{const link=event.target.closest('a[href^="#guia-"]');if(link)openGuideTopic(link.getAttribute('href'));});
window.addEventListener('hashchange',()=>openGuideTopic(window.location.hash));
openGuideTopic(window.location.hash);
