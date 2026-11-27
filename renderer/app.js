'use strict';
const $ = id => document.getElementById(id);
let current,operation='',page='home',currentStep=-1,updateAvailable=null;
const steps=['minecraft','engine','java','client'],activity=[];
function toast(message,error=false){$('toastText').textContent=String(message);$('toast').classList.remove('hidden');$('toast').classList.toggle('error',error);}
async function call(name,...args){if(!window.zyon)throw Error('Abra o Zyon Client instalado.');const result=await window.zyon[name](...args);if(!result.ok)throw Error(result.error||'Não foi possível concluir.');return result.value;}
function showPage(next){page=next;for(const element of document.querySelectorAll('.page'))element.classList.toggle('hidden',element.id!=='page-'+next);for(const element of document.querySelectorAll('.nav'))element.classList.toggle('active',element.dataset.page===next);}
function applyBusy(){
 const busy=!!operation||!!current?.busy,running=!!current?.running,blocked=busy||running;
 for(const element of document.querySelectorAll('[data-pick],#memory,#saveAction,#prepareAction,#nicknameAction,#nicknameInput'))element.disabled=blocked;
 $('mainAction').disabled=blocked;$('launchAction').disabled=blocked||!current?.installed;
 $('installUpdateAction').disabled=blocked||!updateAvailable?.available;
 $('cancelAction').classList.toggle('hidden',!busy);$('updateCancel').classList.toggle('hidden',operation!=='update');
 $('mainAction').textContent=busy?'Aguarde…':running?current.gameReady?'Minecraft aberto':'Iniciando Minecraft…':!current?.account?'Escolher nick':'Jogar offline';
 if(busy)$('statusBadge').textContent={login:'Conectando sua conta',prepare:'Verificando e preparando',launch:'Verificando e iniciando',update:'Baixando atualização'}[operation||current?.busyType]||'Aguarde…';
}
function render(state){
 current=state; $('discordAction').disabled=!state.discordConfigured;$('discordAction').title=state.discordConfigured?'Abrir o servidor Discord do Zyon':'Convite do Discord em configuração';$('discordCaption').textContent=state.discordConfigured?'Entre para a comunidade':'Convite em configuração';
 $('statusBadge').textContent=state.running?state.gameReady?'Minecraft em execução':'Iniciando Minecraft…':!state.account?'Escolha seu nick':state.installed?'Arquivos preparados':'Prepare os arquivos';
 $('statusBadge').classList.toggle('ready',state.installed||state.running);$('installDot').classList.toggle('hidden',state.installed);
 $('appVersion').textContent='v'+state.version;$('currentVersion').textContent='v'+state.version;
 $('instancePath').value=state.instanceDir;$('minecraftPath').value=state.minecraftRoot;$('previousAction').classList.toggle('hidden',!state.previousInstanceDir);
 $('memory').max=state.maxMemoryGB;if(document.activeElement!==$('memory')){$('memory').value=state.memoryGB;$('memoryValue').textContent=state.memoryGB;}$('maxMemory').textContent=state.maxMemoryGB+' GB';
 $('heroStatus').textContent=state.running?state.gameReady?'Seu Minecraft está aberto':'Carregando Minecraft':!state.account?'Escolha seu nick':state.installed?'Pronto para jogar offline':'Prepare seu Zyon';
 $('heroHint').textContent=state.gameError|| (state.running?'Zyon Client em execução.':state.account?'Perfil '+state.account.name+' · modo offline, sem login neste aplicativo.':state.profileError||'Digite seu nick para jogar offline.');
 $('accountName').textContent=state.account?state.account.name:'Seu nick offline';$('accountDescription').textContent=state.account?'Nick salvo neste PC. Servidores que exigem autenticação online precisam de uma sessão online válida.':state.profileError;
 if(document.activeElement!==$('nicknameInput'))$('nicknameInput').value=state.offlineNickname||'';
 $('prepareAction').textContent=state.installed?'Conferir arquivos locais':'Preparar arquivos';
 if(state.installed&&!operation)progress({step:'done',percent:100,message:'Arquivos preparados. Pronto para jogar offline.'},false);
 renderUpdate(state.update||updateAvailable,state.updateMessage);applyBusy();
}
function progress(value, record = true) {
  if (value.finished) return;
  if (typeof value.percent === 'number') { const percent = Math.max(0, Math.min(100, value.percent)); $('progressPercent').textContent = percent + '%'; $('progressFill').style.width = percent + '%'; }
  if (value.message) {
    $('progressMessage').textContent = value.message;
    if (record) { activity.push(value.message); if (activity.length > 8) activity.shift(); $('activity').textContent = activity.join('\n'); }
  }
  if (value.step) currentStep = value.step === 'done' ? 4 : steps.indexOf(value.step);
  for (const [index, name] of steps.entries()) {
    const element = document.querySelector('[data-step="' + name + '"]');
    element.classList.toggle('current', index === currentStep); element.classList.toggle('complete', index < currentStep);
    element.querySelector('.step-status').textContent = index < currentStep ? 'Concluído' : index === currentStep ? 'Preparando…' : 'Aguardando';
  }
}
function renderUpdate(value, message) {
  if (value) updateAvailable = value;
  $('updateDot').classList.toggle('hidden', !updateAvailable?.available);
  $('installUpdateAction').classList.toggle('hidden', !updateAvailable?.available);
  $('updateTitle').textContent = updateAvailable?.available ? 'Zyon ' + updateAvailable.version + ' está disponível' : current?.updatesConfigured ? 'Seu canal de atualizações' : 'Canal aguardando configuração';
  $('updateDescription').textContent = message || updateAvailable?.error || updateAvailable?.message || 'O publicador precisa definir o repositório para ativar os avisos.';
  $('releaseNotes').textContent = updateAvailable?.available ? updateAvailable.notes || 'Melhorias e correções nesta versão.' : '';
  if (value?.openPage) showPage('updates');
}
async function refresh() { render(await call('state')); }
async function run(kind, task) {
  if (operation) return;
  operation = kind; applyBusy();
  try { return await task(); }
  catch (error) { toast(error.message, true); if (['prepare', 'launch'].includes(kind)) $('progressMessage').textContent = error.message; }
  finally {
    operation = ''; try { await refresh(); } catch (error) { toast(error.message, true); }
    if (currentStep >= 0 && currentStep < 4) { const active = document.querySelector('.step.current .step-status'); if (active) active.textContent = 'Não concluído'; }
    applyBusy();
  }
}
function needProfile(){showPage('account');$('nicknameInput').focus();toast(current.profileError||'Escolha um nick antes de jogar.',true);}
async function prepare(){if(!current.account){needProfile();return;}showPage('install');currentStep=-1;await run('prepare',async()=>{await call('prepare');toast('Zyon preparado para jogar offline.');});}
async function play(){if(!current.account){needProfile();return;}await run('launch',async()=>{const started=await call('launch');if(started.gameError)throw Error(started.gameError);if(!started.gameReady)toast('Iniciando Zyon offline.');});}
document.addEventListener('click',async event=>{
 const button=event.target.closest('button');if(!button||button.disabled)return;
 try{
  if(button.dataset.page){showPage(button.dataset.page);return;}
  if(button.dataset.pick){render(await call('pick',button.dataset.pick));return;}
  switch(button.dataset.action){
   case 'primary':await play();break;
   case 'prepare':await prepare();break;
   case 'launch':await play();break;
   case 'nickname':render(await call('nickname',$('nicknameInput').value));toast('Nick salvo. Pronto para jogar offline.');break;
   case 'cancel':await call('cancel');toast('Cancelando…');break;
   case 'folder':await call('folder','instance');break;
   case 'previous':await call('folder','previous');break;
   case 'logs':await call('folder','logs');break;
   case 'discord':await call('external','discord');break;
   case 'official':await call('external','launcher');break;
   case 'save':render(await call('settings',{memoryGB:Number($('memory').value)}));toast('Memória salva.');break;
   case 'check-updates':$('checkUpdatesAction').disabled=true;try{const r=await call('checkUpdates');renderUpdate(r.update,r.message);}finally{$('checkUpdatesAction').disabled=false;}applyBusy();break;
   case 'install-update':showPage('updates');$('updateProgress').classList.remove('hidden');await run('update',()=>call('installUpdate'));break;
  }
 }catch(error){toast(error.message,true);}
});
$('nicknameInput').addEventListener('keydown',async event=>{if(event.key==='Enter'&&!event.isComposing&&!$('nicknameAction').disabled){event.preventDefault();$('nicknameAction').click();}});
$('memory').addEventListener('input',()=>{$('memoryValue').textContent=$('memory').value;});$('dismissToast').addEventListener('click',()=>$('toast').classList.add('hidden'));
window.zyon?.onProgress(progress);
window.zyon?.onEvent('game-state',value=>{if(current)current.running=value.running;refresh().catch(()=>{});if(value.error)toast(value.error,true);else if(value.ready)toast('Minecraft carregado.');});
window.zyon?.onEvent('update-state',value=>{renderUpdate(value,value.error);applyBusy();});window.zyon?.onEvent('update-download',value=>{$('updateProgress').textContent='Baixando atualização: '+(value.total?Math.floor(100*value.received/value.total):0)+'%';});
refresh().catch(error=>toast(error.message,true));
