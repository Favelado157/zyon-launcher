'use strict';
const $ = id => document.getElementById(id);
let current,operation='',page='home',currentStep=-1,updateAvailable=null;
const steps=['minecraft','forge','java','client'],activity=[];
function toast(message,error=false){$('toastText').textContent=String(message);$('toast').classList.remove('hidden');$('toast').classList.toggle('error',error);}
async function call(name,...args){if(!window.zyon)throw Error('Abra o Zyon Visual instalado.');const result=await window.zyon[name](...args);if(!result.ok)throw Error(result.error||'Não foi possível concluir.');return result.value;}
function showPage(next){page=next;for(const element of document.querySelectorAll('.page'))element.classList.toggle('hidden',element.id!=='page-'+next);for(const element of document.querySelectorAll('.nav'))element.classList.toggle('active',element.dataset.page===next);}
function applyBusy(){
 const busy=!!operation||!!current?.busy,running=!!current?.running,blocked=busy||running;
 for(const element of document.querySelectorAll('[data-pick],#memory,#saveAction,#prepareAction,#loginAction,#logoutAction'))element.disabled=blocked;
 $('mainAction').disabled=blocked;$('launchAction').disabled=blocked||!current?.installed;
 $('installUpdateAction').disabled=blocked||!updateAvailable?.available;
 $('cancelAction').classList.toggle('hidden',!busy);$('updateCancel').classList.toggle('hidden',operation!=='update');
 $('mainAction').textContent=busy?'Aguarde…':running?current.gameReady?'Minecraft aberto':'Iniciando Minecraft…':!current?.account?'Entrar com Microsoft':!current.installed?'Verificar e preparar':'Verificar e jogar';
 if(busy)$('statusBadge').textContent={login:'Conectando sua conta',prepare:'Verificando e preparando',launch:'Verificando e iniciando',update:'Baixando atualização'}[operation||current?.busyType]||'Aguarde…';
}
function render(state){
 current=state; $('discordAction').disabled=!state.discordConfigured;$('discordAction').title=state.discordConfigured?'Abrir o servidor Discord do Zyon':'Convite do Discord em configuração';$('discordCaption').textContent=state.discordConfigured?'Entre para a comunidade':'Convite em configuração';
 $('statusBadge').textContent=state.running?state.gameReady?'Minecraft em execução':'Iniciando Minecraft…':!state.account?'Conecte sua conta':state.installed?'Arquivos preparados':'Prepare os arquivos';
 $('statusBadge').classList.toggle('ready',state.installed||state.running);$('installDot').classList.toggle('hidden',state.installed);
 $('appVersion').textContent='v'+state.version;$('currentVersion').textContent='v'+state.version;
 $('instancePath').value=state.instanceDir;$('minecraftPath').value=state.minecraftRoot;$('previousAction').classList.toggle('hidden',!state.previousInstanceDir);
 $('memory').max=state.maxMemoryGB;if(document.activeElement!==$('memory')){$('memory').value=state.memoryGB;$('memoryValue').textContent=state.memoryGB;}$('maxMemory').textContent=state.maxMemoryGB+' GB';
 $('heroStatus').textContent=state.running?state.gameReady?'Seu Minecraft está aberto':'Carregando Minecraft':!state.account?'Conecte sua conta Microsoft':state.installed?'Pronto para verificar e jogar':'Prepare a edição visual';
 $('heroHint').textContent=state.gameError|| (state.running?'Instância visual separada do Zyon.':state.account?'Perfil '+state.account.name+' · propriedade verificada novamente ao jogar.':'É necessário acesso ao Minecraft Java na conta conectada.');
 $('accountName').textContent=state.account?state.account.name:'Conta Microsoft';$('accountDescription').textContent=state.account?'Perfil conectado. A propriedade é consultada antes de preparar ou jogar.':'A senha é digitada somente na página oficial da Microsoft.';
 $('loginAction').textContent=state.account?'Conectar novamente':'Entrar com Microsoft';$('logoutAction').classList.toggle('hidden',!state.account);
 $('prepareAction').textContent=state.installed?'Verificar e preparar novamente':'Verificar conta e preparar';
 if(state.installed&&!operation)progress({step:'done',percent:100,message:'Arquivos preparados. A propriedade será verificada ao jogar.'},false);
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
async function login(){showPage('account');await run('login',async()=>{await call('login');toast('Conta conectada e acesso ao Minecraft Java verificado.');});}
async function prepare(){if(!current.account){await login();return;}showPage('install');currentStep=-1;await run('prepare',async()=>{await call('prepare');toast('Propriedade verificada e arquivos preparados.');});}
async function play(){if(!current.account){await login();return;}await run('launch',async()=>{const started=await call('launch');if(started.gameError)throw Error(started.gameError);if(!started.gameReady)toast('Acesso verificado. Iniciando Minecraft.');});}
document.addEventListener('click',async event=>{
 const button=event.target.closest('button');if(!button||button.disabled)return;
 try{
  if(button.dataset.page){showPage(button.dataset.page);return;}
  if(button.dataset.pick){render(await call('pick',button.dataset.pick));return;}
  switch(button.dataset.action){
   case 'primary':if(!current.account)await login();else if(!current.installed)await prepare();else await play();break;
   case 'prepare':await prepare();break;
   case 'launch':await play();break;
   case 'login':await login();break;
   case 'logout':await call('logout');await refresh();break;
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
$('memory').addEventListener('input',()=>{$('memoryValue').textContent=$('memory').value;});$('dismissToast').addEventListener('click',()=>$('toast').classList.add('hidden'));
window.zyon?.onProgress(progress);window.zyon?.onEvent('auth-code',value=>{$('deviceCodePanel').classList.toggle('hidden',!!value.finished);if(!value.finished){showPage('account');$('deviceCode').textContent=value.code;$('deviceUrl').textContent=value.url;}});
window.zyon?.onEvent('game-state',value=>{if(current)current.running=value.running;refresh().catch(()=>{});if(value.error)toast(value.error,true);else if(value.ready)toast('Minecraft carregado.');});
window.zyon?.onEvent('update-state',value=>{renderUpdate(value,value.error);applyBusy();});window.zyon?.onEvent('update-download',value=>{$('updateProgress').textContent='Baixando atualização: '+(value.total?Math.floor(100*value.received/value.total):0)+'%';});
refresh().catch(error=>toast(error.message,true));
