'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const os = require('node:os');
const { EventEmitter } = require('node:events');
const { PassThrough } = require('node:stream');
const AdmZip = require('adm-zip');
const game = require('../game.cjs');
const core = require('../core.cjs');
async function folder(t) { const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'zyon-game-')); t.after(() => fs.rm(dir, { recursive: true, force: true })); return dir; }
test('Windows rules are evaluated before resolving duplicate library versions', () => {
  const windows = { name: 'org.lwjgl.lwjgl:lwjgl:2.9.4', rules: [{ action: 'allow' }, { action: 'disallow', os: { name: 'osx' } }] };
  const mac = { name: 'org.lwjgl.lwjgl:lwjgl:2.9.2', rules: [{ action: 'allow', os: { name: 'osx' } }] };
  assert.deepEqual(game.mergeLibraries([windows, mac], []), [windows]);
  assert.equal(game.allowed({ rules: [{ action: 'allow', os: { arch: 'x86' } }] }), false);
});
test('native-only packages are not added as missing normal jars', () => {
  const lib = { name: 'net.java.jinput:jinput-platform:2.0.5', natives: { windows: 'natives-windows' }, downloads: { classifiers: { 'natives-windows': { path: 'jinput/windows.jar', sha1: 'a'.repeat(40), url: 'https://libraries.minecraft.net/native.jar' } } } };
  const result = game.libraryPlan({ libraries: [lib] }, { version: { libraries: [] }, relativeJar: 'forge.jar', jar: Buffer.from('forge') }, '/game');
  assert.equal(result.length, 1); assert.equal(result[0].native, true);
});
test('the actual vanilla and Forge metadata resolve Windows LWJGL and all classpath entries', { skip: !process.env.MINECRAFT_FIXTURE || !process.env.FORGE_FIXTURE }, async () => {
  const vanilla = JSON.parse(await fs.readFile(process.env.MINECRAFT_FIXTURE)); const forge = core.parseForge(await fs.readFile(process.env.FORGE_FIXTURE));
  const plan = game.libraryPlan(vanilla, forge, '/minecraft');
  assert.ok(plan.some(item => item.relative.includes('lwjgl/2.9.4') && !item.native));
  assert.ok(plan.some(item => item.relative.includes('lwjgl-platform/2.9.4') && item.native));
  assert.ok(!plan.some(item => item.relative.includes('2.9.2')));
  assert.ok(!plan.some(item => item.relative.endsWith('jinput-platform-2.0.5.jar')));
  assert.ok(plan.every(item => item.url.startsWith('https://')));
});
test('resource paths cannot leave the managed folders', () => {
  for (const relative of ['../outside', '/root', 'a/../../b', 'C:/windows/file', 'a\\b', 'a//b']) assert.throws(() => game.safePath('/cache', relative));
  assert.equal(game.safePath('/cache', 'ab/cd'), path.join('/cache', 'ab', 'cd'));
});
test('native extraction omits META-INF and rejects traversal', async t => {
  const root = await folder(t); const zip = new AdmZip(); zip.addFile('META-INF/manifest.mf', Buffer.from('ignored')); zip.addFile('lwjgl64.dll', Buffer.from('native'));
  const file = path.join(root, 'natives.jar'); await fs.writeFile(file, zip.toBuffer()); await game.extractNatives(file, path.join(root, 'out'), ['META-INF/']);
  assert.equal(await core.exists(path.join(root, 'out', 'META-INF')), false); assert.equal(await fs.readFile(path.join(root, 'out', 'lwjgl64.dll'), 'utf8'), 'native');
});
const session = { uuid: 'a'.repeat(32), name: 'Player_Test', accessToken: 'TOP_SECRET_TOKEN', expiresAt: Date.now() + 100000, mode: 'microsoft', userType: 'mojang', ownershipVerified: true, verifiedAt: Date.now() };
const ready = { classpath: ['C:\\cache\\one.jar', 'C:\\cache\\two.jar'], natives: 'C:\\natives', instanceDir: 'C:\\My Games\\Zyon', assetsRoot: 'C:\\assets', assetsIndex: '1.8', mainClass: 'net.minecraft.launchwrapper.Launch', arguments: '--username ${auth_player_name} --gameDir ${game_directory} --accessToken ${auth_access_token} --uuid ${auth_uuid} --userProperties ${user_properties}', logging: { file: 'C:\\config\\client-1.7.xml', argument: '-Dlog4j.configurationFile=${path}' } };
test('direct launch uses a Windows classpath and keeps paths with spaces as separate arguments', () => {
  const args = game.launchArguments(ready, session, 3);
  assert.equal(args[0], '-Xmx3G'); assert.equal(args[args.indexOf('-cp') + 1], ready.classpath.join(';'));
  assert.equal(args[args.indexOf('--gameDir') + 1], ready.instanceDir); assert.ok(args.includes('-Dlog4j.configurationFile=C:\\config\\client-1.7.xml'));
  assert.throws(() => game.launchArguments(ready, { ...session, uuid: '' }, 2), /Sessão/);
});
test('launch starts Java directly without a shell or the official launcher and redacts streamed tokens', async () => {
  let command, argumentsUsed, options; const output = [];
  const child = new EventEmitter(); child.stdout = new PassThrough(); child.stderr = new PassThrough();
  const started = game.launch({ javaExe: 'C:\\runtime\\javaw.exe', ready, session, memoryGB: 2, onOutput: line => output.push(line), spawnProcess(file, args, opts) { command = file; argumentsUsed = args; options = opts; queueMicrotask(() => child.emit('spawn')); return child; } });
  assert.equal(await started, child); assert.equal(command, 'C:\\runtime\\javaw.exe'); assert.equal(options.shell, false); assert.equal(options.cwd, ready.instanceDir);
  assert.ok(argumentsUsed.includes('net.minecraft.launchwrapper.Launch'));
  child.stdout.write('Session: TOP_SE'); child.stdout.write('CRET_TOKEN\n'); child.stdout.end();
  await new Promise(resolve => setImmediate(resolve));
  assert.ok(output.join('').includes('[sessão omitida]')); assert.ok(!output.join('').includes(session.accessToken));
});

test('menu readiness is confirmed by the client and early failures retain the final stderr without tokens', async () => {
  for (const code of [0, 1, null]) {
    const child = new EventEmitter(); child.exitCode = null;
    child.stdout = new PassThrough(); child.stderr = new PassThrough();
    let result, confirmed = 0;
    await game.launch({ javaExe: 'javaw.exe', ready, session, memoryGB: 2,
      onReady: () => confirmed++, onExit: value => { result = value; },
      spawnProcess(_file, _args, options) {
        assert.equal(options.windowsHide, false);
        queueMicrotask(() => child.emit('spawn')); return child;
      }
    });
    assert.equal(confirmed, 0);
    child.emit('exit', code); assert.equal(result, undefined);
    child.stderr.end('Caused by: java.lang.UnsatisfiedLinkError: TOP_SECRET_TOKEN');
    await new Promise(resolve => setImmediate(resolve));
    child.emit('close', code, code === null ? 'SIGTERM' : null);
    assert.equal(result.startupFailed, true); assert.match(result.error, /antes de carregar/);
    assert.match(result.error, /UnsatisfiedLinkError/); assert.ok(!result.error.includes(session.accessToken));
  }
});

test('a ready menu is reported once and a normal exit is not a startup failure', async () => {
  const child = new EventEmitter(); child.stdout = new PassThrough(); child.stderr = new PassThrough();
  let count = 0, result;
  await game.launch({ javaExe: 'javaw.exe', ready, session, memoryGB: 2,
    onReady: () => count++, onExit: value => { result = value; },
    spawnProcess() { queueMicrotask(() => child.emit('spawn')); return child; }
  });
  child.stdout.write('ZYON_MENU_'); assert.equal(count, 0);
  child.stdout.end('READY\nZYON_MENU_READY\n');
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(count, 1);
  child.emit('close', 0, null); assert.equal(result.error, ''); assert.equal(result.startupFailed, false);
});
