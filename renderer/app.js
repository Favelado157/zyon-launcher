'use strict';
const $ = id => document.getElementById(id);
let current, operation = '', page = 'home', currentStep = -1, updateAvailable = null;
const steps = ['minecraft', 'forge', 'java', 'client'];
const activity = [];
function toast(message, error = false) { $('toastText').textContent = String(message); $('toast').classList.remove('hidden'); $('toast').classList.toggle('error', error); }
async function call(name, ...args) {
  if (!window.zyon) throw new Error('Abra o aplicativo pelo Zyon Launcher instalado.');
  const result = await window.zyon[name](...args);
  if (!result.ok) throw new Error(result.error || 'Não foi possível concluir.'); return result.value;
}
function showPage(next) {
  page = next;
  for (const element of document.querySelectorAll('.page')) element.classList.toggle('hidden', element.id !== 'page-' + next);
  for (const element of document.querySelectorAll('.nav')) element.classList.toggle('active', element.dataset.page === next);
}
function setup() { showPage('settings'); $('publisherSetup').open = true; $('publisherSetup').scrollIntoView({ block: 'center', behavior: 'smooth' }); }
function applyBusy() {
  const busy = !!operation || !!current?.busy, running = !!current?.running;
  for (const element of document.querySelectorAll('[data-pick], [data-action="save"], [data-action="legacy"], #memory, #clientId, #repository, #prepareAction')) element.disabled = busy || running;
  $('mainAction').disabled = busy || running; $('launchAction').disabled = busy || running || !current?.installed;
  $('loginAction').disabled = busy || running; $('logoutAction').disabled = busy || running;
  $('installUpdateAction').disabled = busy || running || !updateAvailable?.available;
  $('cancelAction').classList.toggle('hidden', operation !== 'prepare' && operation !== 'launch');
  $('updateCancel').classList.toggle('hidden', operation !== 'update');
  if (busy) {
    $('statusBadge').textContent = { login: 'Conectando sua conta', launch: 'Iniciando Minecraft', update: 'Baixando atualização', prepare: 'Preparando Minecraft' }[operation || current.busyType] || 'Aguarde…';
    $('mainAction').textContent = 'Aguarde…';
  } else if (current) {
    $('mainAction').textContent = running ? 'Minecraft aberto' : !current.installed ? 'Preparar Minecraft →' : !current.authConfigured ? 'Configurar login →' : !current.account ? 'Entrar com Microsoft →' : 'Play →';
  }
}
function render(state) {
  current = state;
  $('statusBadge').textContent = state.running ? 'Minecraft em execução' : state.installed ? 'Pronto para jogar' : 'Preparação pendente';
  $('statusBadge').classList.toggle('ready', state.installed || state.running); $('installDot').classList.toggle('hidden', state.installed);
  $('appVersion').textContent = 'v' + state.version; $('currentVersion').textContent = 'v' + state.version;
  $('instancePath').value = state.instanceDir; $('minecraftPath').value = state.minecraftRoot; $('launcherPath').value = state.legacyLauncherPath || '';
  if (document.activeElement !== $('clientId')) $('clientId').value = state.microsoftClientId || '';
  if (document.activeElement !== $('repository')) $('repository').value = state.githubRepository || '';
  $('memory').max = state.maxMemoryGB;
  if (document.activeElement !== $('memory')) { $('memory').value = state.memoryGB; $('memoryValue').textContent = state.memoryGB; }
  $('maxMemory').textContent = state.maxMemoryGB + ' GB';
  $('heroStatus').textContent = state.running ? 'Seu mundo está aberto' : state.installed ? 'Sua instância está preparada' : 'Uma nova experiência está chegando';
  $('heroHint').textContent = state.running ? 'O Minecraft foi iniciado diretamente pelo Zyon.' : state.account ? 'Conectado como ' + state.account.name + '. Pronto para Play.' : 'Faça o login em Minha conta para usar o Play direto.';
  $('prepareAction').textContent = state.installed ? 'Verificar arquivos ↻' : 'Preparar Minecraft ↓';
  $('accountName').textContent = state.account ? state.account.name : 'Entre com a Microsoft';
  $('accountDescription').textContent = state.account ? 'Conta conectada. A senha e a sessão permanecem protegidas.' : 'Use sua conta com acesso ao Minecraft Java. A senha fica na página da Microsoft.';
  $('loginAction').textContent = !state.authConfigured ? 'Configurar login' : state.account ? 'Entrar novamente' : 'Entrar com Microsoft';
  $('logoutAction').classList.toggle('hidden', !state.account); $('authSetupNotice').classList.toggle('hidden', state.authConfigured);
  if (state.installed && !operation) progress({ step: 'done', percent: 100, message: 'Minecraft preparado. O Play inicia o jogo diretamente.' }, false);
  if (!state.installed && !operation && currentStep === 4) { currentStep = -1; progress({ percent: 0, message: 'Prepare os arquivos desta versão.' }, false); }
  renderUpdate(state.update || updateAvailable, state.updateMessage);
  applyBusy();
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
async function prepare() {
  showPage('install'); currentStep = -1; progress({ step: 'minecraft', percent: 0, message: 'Preparando sua instância…' });
  await run('prepare', async () => { await call('prepare'); toast('Arquivos preparados. Entre com Microsoft e use Play.'); });
}
async function login() {
  if (!current.authConfigured) { setup(); toast('Veja o guia do publicador para registrar e aprovar o aplicativo Microsoft.'); return; }
  showPage('account');
  await run('login', async () => { await call('login'); toast('Conta Microsoft conectada. Seu Play está pronto.'); });
}
async function play() {
  if (!current.authConfigured) { setup(); toast('O login próprio ainda precisa da configuração do publicador.'); return; }
  if (!current.account) { await login(); return; }
  await run('launch', async () => { await call('launch'); toast('Minecraft iniciado diretamente pelo Zyon.'); });
}
document.addEventListener('click', async event => {
  const button = event.target.closest('button'); if (!button || button.disabled) return;
  try {
    if (button.dataset.page) { showPage(button.dataset.page); return; }
    if (button.dataset.pick) { render(await call('pick', button.dataset.pick)); return; }
    switch (button.dataset.action) {
      case 'primary': if (current?.installed) await play(); else await prepare(); break;
      case 'prepare': await prepare(); break;
      case 'launch': await play(); break;
      case 'login': await login(); break;
      case 'logout': await call('logout'); await refresh(); break;
      case 'cancel': await call('cancel'); toast('Cancelando a operação…'); break;
      case 'folder': await call('folder', 'instance'); break;
      case 'logs': await call('folder', 'logs'); break;
      case 'legacy': { const result = await call('legacy'); toast(result.message); break; }
      case 'setup': setup(); break;
      case 'azure': await call('external', 'azure'); break;
      case 'github': await call('external', 'github'); break;
      case 'official': await call('external', 'launcher'); break;
      case 'check-updates': {
        $('checkUpdatesAction').disabled = true; $('updateDescription').textContent = 'Verificando publicações…';
        try { const result = await call('checkUpdates'); renderUpdate(result.update, result.message); }
        finally { $('checkUpdatesAction').disabled = false; }
        applyBusy(); break;
      }
      case 'install-update':
        showPage('updates'); $('updateProgress').classList.remove('hidden'); $('updateProgress').textContent = 'Baixando e verificando o instalador…';
        await run('update', async () => { await call('installUpdate'); }); break;
      case 'save':
        render(await call('settings', { memoryGB: Number($('memory').value), microsoftClientId: $('clientId').value, githubRepository: $('repository').value }));
        toast('Configurações salvas neste PC. Para distribuir a todos, publique uma versão com esses IDs no canal do Zyon.'); break;
    }
  } catch (error) { toast(error.message, true); }
});
$('memory').addEventListener('input', () => { $('memoryValue').textContent = $('memory').value; });
$('dismissToast').addEventListener('click', () => $('toast').classList.add('hidden'));
window.zyon?.onProgress(progress);
window.zyon?.onEvent('auth-code', value => {
  $('deviceCodePanel').classList.toggle('hidden', !!value.finished);
  if (!value.finished) { showPage('account'); $('deviceCode').textContent = value.code; $('deviceUrl').textContent = value.url; }
});
window.zyon?.onEvent('game-state', value => {
  if (current) current.running = value.running;
  refresh().catch(() => {});
  if (!value.running && value.code) toast('O Minecraft foi encerrado com erro. Use Abrir registros para conferir o motivo.', true);
});
window.zyon?.onEvent('update-state', value => { renderUpdate(value, value.error); applyBusy(); });
window.zyon?.onEvent('update-download', value => {
  const percent = value.total ? Math.floor(100 * value.received / value.total) : 0;
  $('updateProgress').textContent = `Baixando atualização: ${percent}% · ${(value.received / 1024 / 1024).toFixed(1)} MB`;
});
refresh().catch(error => toast(error.message, true));
