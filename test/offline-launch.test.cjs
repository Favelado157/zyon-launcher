'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const vm = require('node:vm');
const core = require('../core.cjs');
const game = require('../game.cjs');

test('desktop IPC saves a nick and reaches Java without a Microsoft account, while Microsoft mode still requires authentication', async t => {
  const temporary = await fs.mkdtemp(path.join(os.tmpdir(), 'zyon-offline-desktop-'));
  t.after(() => fs.rm(temporary, { recursive: true, force: true }));
  const root = path.join(temporary, 'ZyonLauncher'), instance = path.join(root, 'instances', '1.8.9');
  const executable = path.join(root, 'runtime', 'javaw.exe');
  await fs.mkdir(path.dirname(executable), { recursive: true });await fs.writeFile(executable, 'fixture');
  await core.atomicWrite(path.join(root, 'java.json'), JSON.stringify({ executable }));
  const handlers = new Map(), calls = { microsoft: 0, external: 0, java: 0, prepare: 0 };
  let window, exit, menuReady, prepared = false, loaded;
  const onLoaded = new Promise(resolve => { loaded = resolve; });
  const ready = { classpath: ['minecraft.jar'], natives: 'natives', instanceDir: instance, assetsRoot: 'assets', assetsIndex: '1.8', mainClass: 'net.minecraft.launchwrapper.Launch', arguments: '--username ${auth_player_name} --uuid ${auth_uuid} --accessToken ${auth_access_token} --userType ${user_type}' };
  class Window {
    constructor() { window = this;this.webContents = { mainFrame: {}, send() {}, on() {}, setWindowOpenHandler() {} }; }
    on() {} once() {} removeMenu() {} isDestroyed() { return false; } isVisible() { return true; }
    async loadFile() { loaded(); }
  }
  const app = { isPackaged: false, setName() {}, setPath() {}, getPath: name => name === 'appData' ? temporary : root, getVersion: () => '1.2.6', requestSingleInstanceLock: () => true, on() {}, quit() {}, whenReady: () => Promise.resolve() };
  class Account {
    configured() { return false; }
    async profile() { return null; }
    async getSession() { calls.microsoft++;throw new Error('Entre na sua conta Microsoft'); }
  }
  const modules = {
    electron: { app, BrowserWindow: Window, ipcMain: { handle: (name, fn) => handlers.set(name, fn) }, safeStorage: {}, dialog: {}, shell: { openExternal() { calls.external++; } } },
    './auth.cjs': { MicrosoftAccount: Account, validClientId: require('../auth.cjs').validClientId },
    './core.cjs': { ...core, async atomicWrite(file, data) { if (calls.failSave && path.basename(file) === 'config.json') throw new Error('Falha ao salvar configuração');return core.atomicWrite(file, data); }, async prepare(options) { calls.prepare++;await core.installMod(options.payload, options.instanceDir); } },
    './game.cjs': { ...game, readReady: async () => prepared ? ready : null, async ensureArtifacts() { prepared = true;return ready; }, async launch(options) { calls.java++;calls.session = options.session;calls.args = game.launchArguments(options.ready, options.session, options.memoryGB);exit = options.onExit;menuReady = options.onReady;return { exitCode: null }; } },
    './updates.cjs': { ...require('../updates.cjs'), check: async () => ({ available: false }) }
  };
  const repository = path.resolve(__dirname, '..');
  const context = {
    require: name => name in modules ? modules[name] : require(name.startsWith('.') ? path.join(repository, name) : name),
    __dirname: repository, process: { platform: 'win32' }, console, AbortController,
    setTimeout: () => ({ unref() {} }), setInterval: () => ({ unref() {} }), clearInterval() {}
  };
  vm.runInNewContext(await fs.readFile(path.join(repository, 'main.cjs'), 'utf8'), context, { filename: 'main.cjs' });
  await onLoaded;
  const invoke = (name, ...args) => handlers.get('zyon:' + name)({ sender: window.webContents, senderFrame: window.webContents.mainFrame }, ...args);
  assert.equal((await invoke('state')).value.loginMode, 'offline');
  assert.equal((await invoke('offline-login', '../bad')).ok, false);
  calls.failSave = true;assert.equal((await invoke('offline-login', 'UnsavedPlayer')).ok, false);
  assert.equal((await invoke('state')).value.offlineProfile, null);
  assert.equal((await core.readJson(path.join(root, 'config.json'))).offlineNickname, '');calls.failSave = false;
  const saved = await invoke('offline-login', '  ZyonPlayer  ');
  assert.equal(saved.ok, true);assert.equal(saved.value.offlineProfile.name, 'ZyonPlayer');assert.equal(saved.value.authConfigured, false);
  const config = await core.readJson(path.join(root, 'config.json'));
  assert.equal(config.loginMode, 'offline');assert.equal(config.offlineNickname, 'ZyonPlayer');assert.equal(config.accessToken, undefined);
  const launched = await invoke('launch');
  assert.equal(launched.ok, true);assert.equal(launched.value.running, true);assert.equal(launched.value.gameReady, false);
  menuReady();assert.equal((await invoke('state')).value.gameReady, true);
  assert.equal(calls.microsoft, 0);assert.equal(calls.external, 0);assert.equal(calls.java, 1);assert.equal(calls.prepare, 1);
  assert.equal(calls.session.mode, 'offline');assert.equal(calls.args[calls.args.indexOf('--accessToken') + 1], '0');
  assert.equal((await invoke('offline-login', 'OtherPlayer')).ok, false);
  exit({ code: 0 });await new Promise(resolve => setImmediate(resolve));
  assert.equal((await invoke('state')).value.gameReady, false);
  const retry = await invoke('launch');assert.equal(retry.value.gameReady, false);
  exit({ code: 0, error: 'O Minecraft fechou antes de carregar o menu.' });
  const failed = (await invoke('state')).value;
  assert.equal(failed.running, false);assert.match(failed.gameError, /antes de carregar/);
  assert.equal((await invoke('session-mode', 'microsoft')).ok, true);
  const online = await invoke('launch');assert.equal(online.ok, false);assert.match(online.error, /Microsoft/);
  assert.equal(calls.microsoft, 1);assert.equal(calls.java, 2);
  assert.equal((await invoke('state')).value.loginMode, 'microsoft');
});
