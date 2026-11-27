'use strict';
const { app, BrowserWindow, ipcMain, dialog, shell } = require('electron');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const core = require('./core.cjs');
const game = require('./game.cjs');
const updates = require('./updates.cjs');
const edition = require('./edition.cjs');
const offline = require('./offline.cjs');
const publisher = require('./release-config.json');
function discordInvite(value){
 if(!value)return '';try{const u=new URL(String(value).trim());if(u.protocol!=='https:'||u.username||u.password||u.port||u.search||u.hash)return '';
 const valid=u.hostname==='discord.gg'&&/^\/[A-Za-z0-9_-]{1,64}\/?$/.test(u.pathname)||u.hostname==='discord.com'&&/^\/invite\/[A-Za-z0-9_-]{1,64}\/?$/.test(u.pathname);return valid?u.toString():'';}catch{return '';}
}
const discordUrl=discordInvite(publisher.discordInvite);
app.setName('Zyon Launcher');
app.setPath('userData', path.join(app.getPath('appData'), 'ZyonLauncher'));
const root = app.getPath('userData');
const payload = app.isPackaged ? path.join(process.resourcesPath, 'payload') : path.join(__dirname, 'payload');
let window, config, controller, busy = false, busyType = '', gameChild = null, gameReady = false, gameError = '', lastUpdate = null, updateMessage = '', promptedVersion = '', updateTimer;
let logChain = Promise.resolve(), gameLogChain = Promise.resolve();
const lock = app.requestSingleInstanceLock();
if (!lock) app.quit();
app.on('second-instance', () => { if (window) { if (window.isMinimized()) window.restore(); window.focus(); } });
const defaults = () => ({
  instanceDir: path.join(root, 'instances', 'zyon-client-1.8.9'), minecraftRoot: path.join(root, 'minecraft'),
  memoryGB: 2, githubRepository: publisher.githubRepository || '',
  edition: edition.EDITION, loginMode: 'nickname-offline', offlineNickname: '', previousInstanceDir: ''
});
async function log(message) {
  const line = `${new Date().toISOString()}  ${String(message).replaceAll(/[\r\n]/g, ' ')}\n`;
  logChain = logChain.then(async () => {
    await fs.mkdir(root, { recursive: true }); const file = path.join(root, 'launcher.log');
    if (await core.exists(file) && (await fs.stat(file)).size > 2 * 1024 * 1024) await fs.rename(file, file + '.previous').catch(() => {});
    await fs.appendFile(file, line);
  }).catch(() => {});
}
function notify(channel, value) { if (window && !window.isDestroyed()) window.webContents.send('zyon:' + channel, value); }
async function saveConfig(next = config) { await core.atomicWrite(path.join(root, 'config.json'), JSON.stringify(next, null, 2)); }
function noOperation(requireGameClosed = false) {
  if (busy) throw new Error('Aguarde a operação atual terminar ou cancele.');
  if (requireGameClosed && gameChild) throw new Error('Feche o Minecraft antes desta operação.');
}
async function state() {
  let installed = false;
  try {
    await edition.validateInstance(config.instanceDir, payload);
    const manifest = await core.readJson(path.join(payload, 'manifest.json'));
    const modFile = core.clientFile(config.instanceDir);
    const ready = await game.readReady(root, config.instanceDir, config.minecraftRoot);
    const java = await core.readJson(path.join(root, 'java.json'), {});
    installed = !!ready && typeof java.executable === 'string' && await core.exists(java.executable)
      && await core.exists(modFile) && core.digest(await fs.readFile(modFile)) === manifest.sha256;
  } catch { installed = false; }
  const profile = await offline.profileStatus(config.offlineNickname);
  return { ...config, installed, busy, busyType, running: !!gameChild, gameReady, gameError, version: app.getVersion(),
    maxMemoryGB: Math.max(1, Math.min(16, Math.floor(os.totalmem() / 1024 ** 3) - 2)), platform: process.platform,
    account: profile.profile, profileError: profile.error, offlinePreview: false,
    authConfigured: false, update: lastUpdate, updateMessage,
    updatesConfigured: !!config.githubRepository, discordConfigured: !!discordUrl, profile: 'Zyon Client 1.8.9 — offline', root };
}
function register(channel, handler) {
  ipcMain.handle('zyon:' + channel, async (event, ...args) => {
    if (!window || event.sender !== window.webContents || event.senderFrame !== window.webContents.mainFrame) return { ok: false, error: 'Origem inválida.' };
    try { return { ok: true, value: await handler(...args) }; }
    catch (error) { const message = String(error.message || 'Não foi possível concluir a operação.'); await log(message); return { ok: false, error: message }; }
  });
}
async function prepareInstallation() {
  await edition.validateInstance(config.instanceDir, payload);
  await edition.migrateProfiles(config.previousInstanceDir, config.instanceDir);
  const localReady = await game.readReady(root, config.instanceDir, config.minecraftRoot);
  const localJava = await core.readJson(path.join(root, 'java.json'), null).catch(() => null);
  const expectedClient = await core.readJson(path.join(payload, 'manifest.json'));
  const localClient = core.clientFile(config.instanceDir);
  const matchesClient = await core.exists(localClient) && core.digest(await fs.readFile(localClient)) === expectedClient.sha256;
  if (localReady && matchesClient && typeof localJava?.executable === 'string' && await core.exists(localJava.executable)) {
    await core.installClient(payload, config.instanceDir);
    notify('progress', { step: 'done', percent: 100, message: 'Zyon Client atualizado com os arquivos locais.' });
    return;
  }
  await core.prepare({ root, payload, ...config, signal: controller.signal,
    emit: value => { if (value.step === 'done') return; notify('progress', { ...value, percent: Math.floor(value.percent * .47) }); } });
  await game.ensureArtifacts({ root, ...config, signal: controller.signal, emit: value => { notify('progress', value); } });
  await log('Instância própria preparada.');
}
register('state', state);
register('pick', async kind => {
  noOperation(true);
  if (kind !== 'instance') throw new Error('Destino inválido.');
  const result = await dialog.showOpenDialog(window, { title: 'Pasta do Zyon Client', properties: ['openDirectory', 'createDirectory'] });
  if (result.canceled) return state();
  const selected = path.resolve(result.filePaths[0]);
  if (core.inside(config.minecraftRoot, selected) || core.inside(selected, config.minecraftRoot)) throw new Error('Escolha uma pasta fora do cache do Minecraft.');
  await edition.validateInstance(selected, payload);
  config.instanceDir = selected;
  await saveConfig(); return state();
});
register('settings', async value => {
  noOperation(true);
  const gb = Number(value?.memoryGB), max = (await state()).maxMemoryGB;
  if (!Number.isInteger(gb) || gb < 1 || gb > max) throw new Error('Quantidade de memória inválida.');
  config = { ...config, memoryGB: gb };
  await saveConfig(); return state();
});
register('prepare', async () => {
  noOperation(true); if (process.platform !== 'win32') throw new Error('Esta distribuição prepara Minecraft para Windows x64.');
  busy = true; busyType = 'prepare'; controller = new AbortController();
  try { await offline.resolveSession(config.offlineNickname); await prepareInstallation(); }
  finally { busy = false; busyType = ''; controller = null; notify('progress', { finished: true }); }
  return state();
});
register('cancel', async () => { controller?.abort(); return true; });
register('nickname', async value => { noOperation(true); const name = offline.nickname(value); await saveConfig({ ...config, offlineNickname: name }); config.offlineNickname = name; return state(); });
register('launch', async () => {
  noOperation(true); if (process.platform !== 'win32') throw new Error('A inicialização desta versão é para Windows x64.');
  busy = true; busyType = 'launch'; controller = new AbortController();
  try {
    let session = await offline.resolveSession(config.offlineNickname);
    if (!(await state()).installed) {
      await prepareInstallation();
      // Large first-time downloads can outlast the session freshness window.
      session = await offline.resolveSession(config.offlineNickname);
    }
    await edition.validateInstance(config.instanceDir, payload);
    const ready = await game.readReady(root, config.instanceDir, config.minecraftRoot);
    if (!ready) throw new Error('A instalação está incompleta. Prepare-a novamente.');
    const java = await core.readJson(path.join(root, 'java.json'));
    const gameLog = path.join(root, 'game.log');
    if (await core.exists(gameLog) && (await fs.stat(gameLog)).size > 5 * 1024 * 1024) await fs.rename(gameLog, gameLog + '.previous').catch(() => {});
    if (controller.signal.aborted) throw new Error('Inicialização cancelada.');
    gameReady = false; gameError = '';
    let ended = false;
    const child = await game.launch({ javaExe: java.executable, ready, session, memoryGB: config.memoryGB,
      onOutput: text => { gameLogChain = gameLogChain.then(() => fs.appendFile(gameLog, text)).catch(() => {}); },
      onReady: () => { gameReady = true; notify('game-state', { running: true, ready: true }); },
      onExit: result => { ended = true; gameChild = null; gameReady = false; gameError = result.error || ''; notify('game-state', { running: false, code: result.code, error: gameError }); log('Minecraft encerrado com código ' + result.code + (gameError ? ': ' + gameError : '')); if (window && !window.isDestroyed() && !window.isVisible()) app.quit(); }
    });
    gameChild = !ended && child.exitCode === null ? child : null;
    await log('Processo Java iniciado no modo ' + config.loginMode + '; aguardando o menu do Minecraft.'); notify('game-state', { running: !!gameChild, ready: gameReady });
  } finally { busy = false; busyType = ''; controller = null; notify('progress', { finished: true }); }
  return state();
});
async function checkUpdates(prompt = false) {
  if (!config) return;
  try {
    lastUpdate = await updates.check({ repository: config.githubRepository, currentVersion: app.getVersion() }); updateMessage = lastUpdate.message;
    notify('update-state', lastUpdate);
    if (prompt && lastUpdate.available && !busy && !gameChild && promptedVersion !== lastUpdate.version) {
      promptedVersion = lastUpdate.version;
      const result = await dialog.showMessageBox(window, { type: 'info', title: 'Atualização do Zyon', message: `Zyon ${lastUpdate.version} está disponível.`, detail: (lastUpdate.notes || 'Uma nova versão foi publicada.') + '\n\nSeus mundos e configurações são mantidos.', buttons: ['Ver atualização', 'Mais tarde'], defaultId: 0, cancelId: 1 });
      if (result.response === 0) notify('update-state', { ...lastUpdate, openPage: true });
    }
  } catch (error) { updateMessage = error.message; notify('update-state', { configured: true, available: false, error: error.message }); }
  return { update: lastUpdate, message: updateMessage };
}
register('check-updates', () => checkUpdates(false));
register('install-update', async () => {
  noOperation(true);
  if (!lastUpdate?.available || lastUpdate.repository !== config.githubRepository) throw new Error('Verifique as atualizações antes de instalar.');
  busy = true; busyType = 'update'; controller = new AbortController();
  try {
    let lastReport = 0;
    const file = await updates.download(lastUpdate, { root, signal: controller.signal, report(received, total) {
      if (Date.now() - lastReport < 500 && received !== total) return; lastReport = Date.now();
      notify('update-download', { received, total: total || lastUpdate.size });
    } });
    await core.launchExecutable(file); await log('Instalador de atualização iniciado.'); app.quit();
    return true;
  } finally { busy = false; busyType = ''; controller = null; }
});
register('folder', async kind => {
  if (!['logs', 'instance', 'previous'].includes(kind)) throw new Error('Pasta inválida.');
  if (kind === 'previous' && !config.previousInstanceDir) throw new Error('Não há pasta anterior registrada.');
  const folder = kind === 'logs' ? root : kind === 'previous' ? config.previousInstanceDir : config.instanceDir; await fs.mkdir(folder, { recursive: true });
  const error = await shell.openPath(folder); if (error) throw new Error(error); return true;
});
register('external', async kind => {
  const urls = { discord: discordUrl, launcher: 'https://www.minecraft.net/download', help: 'https://help.minecraft.net/', azure: 'https://entra.microsoft.com/', github: 'https://github.com/new', review: 'https://aka.ms/mce-reviewappid' };
  if (!urls[kind]) throw new Error('Link inválido.'); await shell.openExternal(urls[kind]); return true;
});
if (lock) app.whenReady().then(async () => {
  await fs.mkdir(root, { recursive: true }); config = defaults();
  if (!await core.exists(path.join(root, 'config.json'))) {
    for (const name of ['ZyonClientPreview', 'ZyonTesteLocal']) {
      const sourceRoot = path.join(app.getPath('appData'), name);
      const old = await core.readJson(path.join(sourceRoot, 'config.json'), null).catch(() => null);
      if (!old) continue;
      if (typeof old.instanceDir === 'string' && path.isAbsolute(old.instanceDir) && await core.exists(old.instanceDir)) config.instanceDir = old.instanceDir;
      if (typeof old.minecraftRoot === 'string' && path.isAbsolute(old.minecraftRoot) && await core.exists(path.join(old.minecraftRoot, 'versions', '1.8.9', '1.8.9.json'))) config.minecraftRoot = old.minecraftRoot;
      try { config.offlineNickname = offline.nickname(old.offlineNickname); } catch {}
      if (Number.isInteger(old.memoryGB) && old.memoryGB >= 1 && old.memoryGB <= 16) config.memoryGB = old.memoryGB;
      const java = await core.readJson(path.join(sourceRoot, 'java.json'), null).catch(() => null);
      if (java && typeof java.executable === 'string' && core.inside(path.join(sourceRoot, 'runtime'), java.executable) && await core.exists(java.executable)) await core.atomicWrite(path.join(root, 'java.json'), JSON.stringify(java));
      await saveConfig(); break;
    }
  }
  try {
    const previous = await core.readJson(path.join(root, 'config.json'), {});
    Object.assign(config, edition.migrate(previous, root, publisher));
    config.loginMode = 'nickname-offline';
    try { config.offlineNickname = offline.nickname(previous.offlineNickname); } catch { config.offlineNickname = ''; }
    if (typeof previous.minecraftRoot === 'string' && path.isAbsolute(previous.minecraftRoot)) config.minecraftRoot = previous.minecraftRoot;
    if (Number.isInteger(previous.memoryGB) && previous.memoryGB >= 1 && previous.memoryGB <= 16) config.memoryGB = previous.memoryGB;
  } catch { await log('Configuração inválida; valores padrão aplicados.'); }
  config.memoryGB = Math.min(config.memoryGB, Math.max(1, Math.min(16, Math.floor(os.totalmem() / 1024 ** 3) - 2)));
  await saveConfig();
  window = new BrowserWindow({ width: 1240, height: 900, minWidth: 1020, minHeight: 740, show: false,
    title: 'Zyon Client', backgroundColor: '#060f1b', icon: path.join(__dirname, 'assets/icon.png'),
    webPreferences: { preload: path.join(__dirname, 'preload.cjs'), nodeIntegration: false, contextIsolation: true, sandbox: true, webSecurity: true } });
  window.removeMenu(); window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  window.webContents.on('will-navigate', event => event.preventDefault()); window.once('ready-to-show', () => window.show());
  window.on('close', event => { if (gameChild) { event.preventDefault(); window.hide(); } });
  await window.loadFile(path.join(__dirname, 'renderer', 'index.html'));
  setTimeout(() => checkUpdates(true), 5000);
  updateTimer = setInterval(() => checkUpdates(true), 30 * 60 * 1000);

}).catch(error => { dialog.showErrorBox('Zyon Launcher', String(error.message)); app.quit(); });
app.on('window-all-closed', () => { if (!gameChild) app.quit(); });
app.on('before-quit', () => { controller?.abort(); if (updateTimer) clearInterval(updateTimer); });
