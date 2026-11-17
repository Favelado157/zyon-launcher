'use strict';
const { app, BrowserWindow, ipcMain, dialog, shell, safeStorage } = require('electron');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const core = require('./core.cjs');
const game = require('./game.cjs');
const updates = require('./updates.cjs');
const { MicrosoftAccount, validClientId } = require('./auth.cjs');
const publisher = require('./release-config.json');
app.setName('Zyon Launcher');
app.setPath('userData', path.join(app.getPath('appData'), 'ZyonLauncher'));
const root = app.getPath('userData');
const payload = app.isPackaged ? path.join(process.resourcesPath, 'payload') : path.join(__dirname, 'payload');
let window, config, account, controller, busy = false, busyType = '', gameChild = null, lastUpdate = null, updateMessage = '', promptedVersion = '', updateTimer;
let logChain = Promise.resolve(), gameLogChain = Promise.resolve();
const lock = app.requestSingleInstanceLock();
if (!lock) app.quit();
app.on('second-instance', () => { if (window) { if (window.isMinimized()) window.restore(); window.focus(); } });
const defaults = () => ({
  instanceDir: path.join(root, 'instances', '1.8.9'), minecraftRoot: path.join(root, 'minecraft'),
  memoryGB: 2, microsoftClientId: publisher.microsoftClientId || '', githubRepository: publisher.githubRepository || '', legacyLauncherPath: ''
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
async function saveConfig() { await core.atomicWrite(path.join(root, 'config.json'), JSON.stringify(config, null, 2)); }
function noOperation(requireGameClosed = false) {
  if (busy) throw new Error('Aguarde a operação atual terminar ou cancele.');
  if (requireGameClosed && gameChild) throw new Error('Feche o Minecraft antes desta operação.');
}
async function state() {
  let installed = false;
  try {
    const manifest = await core.readJson(path.join(payload, 'manifest.json'));
    const modFile = path.join(config.instanceDir, 'mods', core.MOD);
    const ready = await game.readReady(root, config.instanceDir, config.minecraftRoot);
    const java = await core.readJson(path.join(root, 'java.json'), {});
    installed = !!ready && typeof java.executable === 'string' && await core.exists(java.executable)
      && await core.exists(modFile) && core.digest(await fs.readFile(modFile)) === manifest.sha256;
  } catch { installed = false; }
  return { ...config, installed, busy, busyType, running: !!gameChild, version: app.getVersion(),
    maxMemoryGB: Math.max(1, Math.min(16, Math.floor(os.totalmem() / 1024 ** 3) - 2)), platform: process.platform,
    account: await account.profile(), authConfigured: account.configured(), update: lastUpdate, updateMessage,
    updatesConfigured: !!config.githubRepository, profile: 'Zyon 1.8.9', root };
}
function register(channel, handler) {
  ipcMain.handle('zyon:' + channel, async (event, ...args) => {
    if (!window || event.sender !== window.webContents || event.senderFrame !== window.webContents.mainFrame) return { ok: false, error: 'Origem inválida.' };
    try { return { ok: true, value: await handler(...args) }; }
    catch (error) { const message = String(error.message || 'Não foi possível concluir a operação.'); await log(message); return { ok: false, error: message }; }
  });
}
async function prepareInstallation() {
  await core.prepare({ root, payload, ...config, registerProfiles: false, signal: controller.signal,
    emit: value => { if (value.step === 'done') return; notify('progress', { ...value, percent: Math.floor(value.percent * .47) }); } });
  await game.ensureArtifacts({ root, ...config, signal: controller.signal, emit: value => { notify('progress', value); } });
  await log('Instância própria preparada.');
}
register('state', state);
register('pick', async kind => {
  noOperation(true);
  if (!['instance', 'launcher'].includes(kind)) throw new Error('Destino inválido.');
  const launcher = kind === 'launcher';
  const result = await dialog.showOpenDialog(window, { title: launcher ? 'Selecione o launcher oficial para compatibilidade' : 'Pasta da instância Zyon',
    properties: launcher ? ['openFile'] : ['openDirectory', 'createDirectory'], ...(launcher ? { filters: [{ name: 'Aplicativo Windows', extensions: ['exe'] }] } : {}) });
  if (result.canceled) return state();
  const selected = path.resolve(result.filePaths[0]);
  if (launcher) { if (path.extname(selected).toLowerCase() !== '.exe') throw new Error('Selecione um aplicativo .exe.'); config.legacyLauncherPath = selected; }
  else {
    if (core.inside(config.minecraftRoot, selected) || core.inside(selected, config.minecraftRoot)) throw new Error('Escolha uma pasta fora do cache do Minecraft.');
    config.instanceDir = selected;
  }
  await saveConfig(); return state();
});
register('settings', async value => {
  noOperation(true);
  const gb = Number(value?.memoryGB), max = (await state()).maxMemoryGB;
  if (!Number.isInteger(gb) || gb < 1 || gb > max) throw new Error('Quantidade de memória inválida.');
  const id = String(value?.microsoftClientId ?? config.microsoftClientId).trim();
  if (id && !validClientId(id)) throw new Error('O Application (client) ID precisa ser um identificador Microsoft válido. Não informe senhas ou client secrets.');
  const repo = updates.normalizeRepository(value?.githubRepository ?? config.githubRepository);
  const oldRepo = config.githubRepository;
  config = { ...config, memoryGB: gb, microsoftClientId: id, githubRepository: repo };
  if (repo !== oldRepo) { lastUpdate = null; updateMessage = ''; promptedVersion = ''; }
  await saveConfig(); return state();
});
register('prepare', async () => {
  noOperation(true); if (process.platform !== 'win32') throw new Error('Esta distribuição prepara Minecraft para Windows x64.');
  busy = true; busyType = 'prepare'; controller = new AbortController();
  try { await prepareInstallation(); }
  finally { busy = false; busyType = ''; controller = null; notify('progress', { finished: true }); }
  return state();
});
register('cancel', async () => { controller?.abort(); return true; });
register('login', async () => {
  noOperation(true); busy = true; busyType = 'login'; controller = new AbortController();
  try {
    await account.signIn(controller.signal, value => {
      notify('auth-code', value);
      shell.openExternal(value.url).catch(() => notify('auth-code', { ...value, openManually: true }));
    });
    await log('Login Microsoft concluído.');
  } finally { busy = false; busyType = ''; controller = null; notify('auth-code', { finished: true }); }
  return state();
});
register('logout', async () => { noOperation(true); await account.signOut(); return state(); });
register('launch', async () => {
  noOperation(true); if (process.platform !== 'win32') throw new Error('A inicialização desta versão é para Windows x64.');
  busy = true; busyType = 'launch'; controller = new AbortController();
  try {
    const session = await account.getSession(controller.signal);
    if (!(await state()).installed) await prepareInstallation();
    const ready = await game.readReady(root, config.instanceDir, config.minecraftRoot);
    if (!ready) throw new Error('A instalação está incompleta. Prepare-a novamente.');
    const java = await core.readJson(path.join(root, 'java.json'));
    const gameLog = path.join(root, 'game.log');
    if (await core.exists(gameLog) && (await fs.stat(gameLog)).size > 5 * 1024 * 1024) await fs.rename(gameLog, gameLog + '.previous').catch(() => {});
    const child = await game.launch({ javaExe: java.executable, ready, session, memoryGB: config.memoryGB,
      onOutput: text => { gameLogChain = gameLogChain.then(() => fs.appendFile(gameLog, text)).catch(() => {}); },
      onExit: result => { gameChild = null; notify('game-state', { running: false, code: result.code }); log('Minecraft encerrado com código ' + result.code); if (window && !window.isDestroyed() && !window.isVisible()) app.quit(); }
    });
    gameChild = child.exitCode === null ? child : null;
    await log('Minecraft iniciado diretamente pelo Java.'); notify('game-state', { running: !!gameChild });
  } finally { busy = false; busyType = ''; controller = null; notify('progress', { finished: true }); }
  return state();
});
register('legacy', async () => {
  noOperation(true);
  if (!config.legacyLauncherPath || !await core.exists(config.legacyLauncherPath)) throw new Error('Escolha o launcher oficial nas opções de compatibilidade.');
  if (!await core.exists(path.join(app.getPath('appData'), '.minecraft', 'versions', core.VERSION, core.VERSION + '.json'))) throw new Error('A compatibilidade precisa da instalação anterior preparada na versão 1.0. Use o Play direto após ativar o login próprio.');
  const java = await core.readJson(path.join(root, 'java.json'), {});
  if (!java.executable) throw new Error('Prepare a instalação primeiro.');
  await core.updateProfiles(path.join(app.getPath('appData'), '.minecraft'), config.instanceDir, java.executable, config.memoryGB);
  await core.launchExecutable(config.legacyLauncherPath);
  return { message: 'Modo de compatibilidade: selecione Zyon 1.8.9 no launcher oficial. O Play principal usa a inicialização direta.' };
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
  const folder = kind === 'logs' ? root : config.instanceDir; await fs.mkdir(folder, { recursive: true });
  const error = await shell.openPath(folder); if (error) throw new Error(error); return true;
});
register('external', async kind => {
  const urls = { launcher: 'https://www.minecraft.net/download', help: 'https://help.minecraft.net/', azure: 'https://entra.microsoft.com/', github: 'https://github.com/new', review: 'https://aka.ms/mce-reviewappid' };
  if (!urls[kind]) throw new Error('Link inválido.'); await shell.openExternal(urls[kind]); return true;
});
if (lock) app.whenReady().then(async () => {
  await fs.mkdir(root, { recursive: true }); config = defaults();
  try {
    const previous = await core.readJson(path.join(root, 'config.json'), {});
    if (typeof previous.instanceDir === 'string' && path.isAbsolute(previous.instanceDir)) config.instanceDir = previous.instanceDir;
    // 1.0 shared the official folder; 1.1 has its own cache and preserves the existing instance directory.
    if (typeof previous.minecraftRoot === 'string' && core.inside(root, previous.minecraftRoot)) config.minecraftRoot = previous.minecraftRoot;
    const legacyPath = previous.legacyLauncherPath || previous.launcherPath;
    if (typeof legacyPath === 'string' && path.isAbsolute(legacyPath)) config.legacyLauncherPath = legacyPath;
    if (Number.isInteger(previous.memoryGB) && previous.memoryGB >= 1 && previous.memoryGB <= 16) config.memoryGB = previous.memoryGB;
    if (!publisher.microsoftClientId && validClientId(previous.microsoftClientId)) config.microsoftClientId = previous.microsoftClientId;
    if (!publisher.githubRepository) config.githubRepository = updates.normalizeRepository(previous.githubRepository);
  } catch { await log('Configuração inválida; valores padrão aplicados.'); }
  config.memoryGB = Math.min(config.memoryGB, Math.max(1, Math.min(16, Math.floor(os.totalmem() / 1024 ** 3) - 2)));
  await saveConfig();
  account = new MicrosoftAccount({ root, getClientId: () => config.microsoftClientId, secureStore: safeStorage });
  window = new BrowserWindow({ width: 1240, height: 900, minWidth: 1020, minHeight: 740, show: false,
    title: 'Zyon Launcher', backgroundColor: '#101116', icon: path.join(__dirname, 'assets/icon.png'),
    webPreferences: { preload: path.join(__dirname, 'preload.cjs'), nodeIntegration: false, contextIsolation: true, sandbox: true, webSecurity: true } });
  window.removeMenu(); window.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  window.webContents.on('will-navigate', event => event.preventDefault()); window.once('ready-to-show', () => window.show());
  window.on('close', event => { if (gameChild) { event.preventDefault(); window.hide(); } });
  await window.loadFile(path.join(__dirname, 'renderer', 'index.html'));
  setTimeout(() => checkUpdates(true), 2500).unref();
  updateTimer = setInterval(() => checkUpdates(true), (6 * 60 + Math.floor(Math.random() * 30)) * 60000); updateTimer.unref();
}).catch(error => { dialog.showErrorBox('Zyon Launcher', String(error.message)); app.quit(); });
app.on('window-all-closed', () => { if (!gameChild) app.quit(); });
app.on('before-quit', () => { controller?.abort(); if (updateTimer) clearInterval(updateTimer); });
