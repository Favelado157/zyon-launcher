'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const crypto = require('node:crypto');
const core = require('../core.cjs');
const AdmZip = require('adm-zip');

async function folder(t) { const dir = await fs.mkdtemp(path.join(os.tmpdir(), 'zyon-test-')); t.after(() => fs.rm(dir, { recursive: true, force: true })); return dir; }
const response = data => new Response(Buffer.from(data), { status: 200 });
test('correctly installs bundled mod and preserves unrelated mods', async t => {
  const root = await folder(t), instance = path.join(root, 'instance');
  await fs.mkdir(path.join(instance, 'mods'), { recursive: true });
  await fs.writeFile(path.join(instance, 'mods', 'other.jar'), 'unchanged');
  await core.installMod(path.join(__dirname, '../payload'), instance);
  const expected = require('../payload/manifest.json');
  assert.equal(core.digest(await fs.readFile(path.join(instance, 'mods', core.MOD))), expected.sha256);
  assert.equal(await fs.readFile(path.join(instance, 'mods/other.jar'), 'utf8'), 'unchanged');
  await core.installMod(path.join(__dirname, '../payload'), instance);
  assert.equal((await fs.readdir(path.join(instance, 'mods'))).length, 2);
});
test('changed bundled destination is backed up before replacement', async t => {
  const root = await folder(t); const mods = path.join(root, 'mods'); await fs.mkdir(mods);
  await fs.writeFile(path.join(mods, core.MOD), 'custom-old-client');
  await core.installMod(path.join(__dirname, '../payload'), root);
  const backup = (await fs.readdir(mods)).find(name => name.includes('.zyon-backup-'));
  assert.ok(backup); assert.equal(await fs.readFile(path.join(mods, backup), 'utf8'), 'custom-old-client');
});
test('corrupted payload fails without replacing existing file', async t => {
  const root = await folder(t), payload = path.join(root, 'payload'), instance = path.join(root, 'instance');
  await fs.mkdir(payload); await fs.mkdir(path.join(instance, 'mods'), { recursive: true });
  await fs.writeFile(path.join(payload, 'manifest.json'), JSON.stringify({ file: core.MOD, sha256: '0'.repeat(64) }));
  await fs.writeFile(path.join(payload, core.MOD), Buffer.from('PK\x03\x04not-the-client'));
  await fs.writeFile(path.join(instance, 'mods', core.MOD), 'existing');
  await assert.rejects(core.installMod(payload, instance), /verificação/);
  assert.equal(await fs.readFile(path.join(instance, 'mods', core.MOD), 'utf8'), 'existing');
});
test('upgrading moves the old Zyon jar to backup even when the new jar was already copied', async t => {
  const root = await folder(t), mods = path.join(root, 'mods'); await fs.mkdir(mods);
  const old = new AdmZip(); old.addFile('mcmod.info', Buffer.from('[{"modid":"zyon","version":"1.0.0"}]')); old.addFile('custom.dat', Buffer.from('preserved'));
  const oldData = old.toBuffer(); await fs.writeFile(path.join(mods, 'zyon-1.8.9-1.0.0.jar'), oldData);
  await fs.writeFile(path.join(mods, 'zyon-1.8.9-1.1.0.jar'), oldData);
  await fs.writeFile(path.join(mods, 'zyon-1.8.9-1.2.0.jar'), oldData);
  await fs.writeFile(path.join(mods, 'zyon-1.8.9-1.2.1.jar'), oldData);
  await fs.mkdir(path.join(root, 'config', 'zyon'), { recursive: true });
  await fs.writeFile(path.join(root, 'config', 'zyon', 'auto.json'), 'existing-profile');
  await fs.copyFile(path.join(__dirname, '../payload', core.MOD), path.join(mods, core.MOD));
  await core.installMod(path.join(__dirname, '../payload'), root);
  assert.equal(await core.exists(path.join(mods, 'zyon-1.8.9-1.0.0.jar')), false);
  assert.equal(await core.exists(path.join(mods, 'zyon-1.8.9-1.1.0.jar')), false);
  assert.equal(await core.exists(path.join(mods, 'zyon-1.8.9-1.2.0.jar')), false);
  assert.equal(await core.exists(path.join(mods, 'zyon-1.8.9-1.2.1.jar')), false);
  assert.equal(await fs.readFile(path.join(root, 'config', 'zyon', 'auto.json'), 'utf8'), 'existing-profile');
  const names = await fs.readdir(path.join(root, 'zyon-backups')); assert.equal(names.length, 4);
  for (const name of names) assert.deepEqual(await fs.readFile(path.join(root, 'zyon-backups', name)), oldData);
  await core.installMod(path.join(__dirname, '../payload'), root);
  assert.equal((await fs.readdir(path.join(root, 'zyon-backups'))).length, 4);
});
test('profile update preserves existing profiles, settings, and account fields', async t => {
  const root = await folder(t), original = { profiles: { vanilla: { name: 'Vanilla', gameDir: 'original' } }, settings: { locale: 'pt-br' }, authenticationDatabase: { fixture: 'preserved' }, version: 3 };
  const file = path.join(root, 'launcher_profiles.json'); await fs.writeFile(file, JSON.stringify(original));
  const instance = path.join(root, 'separate');
  const id = await core.updateProfiles(root, instance, 'C:\\Java 8\\bin\\javaw.exe', 3);
  const result = JSON.parse(await fs.readFile(file));
  assert.deepEqual(result.profiles.vanilla, original.profiles.vanilla);
  assert.deepEqual(result.settings, original.settings); assert.deepEqual(result.authenticationDatabase, original.authenticationDatabase);
  assert.equal(result.profiles[id].gameDir, instance); assert.equal(result.profiles[id].lastVersionId, core.VERSION);
  assert.ok(result.profiles[id].javaArgs.startsWith('-Xmx3G'));
  const copy = (await fs.readdir(root)).find(name => name.includes('.zyon-backup-'));
  assert.deepEqual(JSON.parse(await fs.readFile(path.join(root, copy))), original);
});
test('invalid second launcher profile file cannot modify first one', async t => {
  const root = await folder(t), file = path.join(root, 'launcher_profiles.json'), content = '{"profiles":{"a":{"name":"Original"}}}';
  await fs.writeFile(file, content); await fs.writeFile(path.join(root, 'launcher_profiles_microsoft_store.json'), '{broken');
  await assert.rejects(core.updateProfiles(root, path.join(root, 'new'), 'javaw.exe', 2));
  assert.equal(await fs.readFile(file, 'utf8'), content);
});
test('download verifies hash before committing a file', async t => {
  const root = await folder(t), file = path.join(root, 'download.bin');
  await assert.rejects(core.downloadVerified('https://example.test/file', file, '0'.repeat(64), 'sha256', { request: async () => response('corrupted') }), /verificação/);
  assert.equal(await core.exists(file), false);
  await core.downloadVerified('https://example.test/file', file, core.digest('expected'), 'sha256', { request: async () => response('expected') });
  assert.equal(await fs.readFile(file, 'utf8'), 'expected');
});
test('preexisting different library stays intact', async t => {
  const root = await folder(t), file = path.join(root, 'library.jar'); await fs.writeFile(file, 'local-custom');
  await assert.rejects(core.downloadVerified('https://example.test/file', file, core.digest('remote'), 'sha256'), /preservado/);
  assert.equal(await fs.readFile(file, 'utf8'), 'local-custom');
});
test('network limits and cancellation are enforced', async () => {
  await assert.rejects(core.fetchBuffer('https://example.test/file', { maxBytes: 2, request: async () => response('123456') }), /limite/);
  const controller = new AbortController(); controller.abort();
  await assert.rejects(core.fetchBuffer('https://example.test/file', { signal: controller.signal }), /cancelada/);
  await assert.rejects(core.fetchBuffer('http://example.test/file'), /HTTPS/);
});
test('library coordinates cannot escape the library directory', () => {
  assert.equal(core.coordinatePath('net.minecraft:launchwrapper:1.12'), 'net/minecraft/launchwrapper/1.12/launchwrapper-1.12.jar');
  for (const bad of ['../../a:b:1', 'a:b:../escape', 'a:b:c:d:e', 'a:/evil:1', '..:x:1']) assert.throws(() => core.coordinatePath(bad));
});
test('instance cannot overlap shared Minecraft root', async () => {
  await assert.rejects(core.prepare({ root: '/tmp/z', instanceDir: '/tmp/minecraft/instance', minecraftRoot: '/tmp/minecraft' }), /separadas/);
});
test('real pinned Forge installer has usable version metadata and embedded library', { skip: !process.env.FORGE_FIXTURE }, async () => {
  const bytes = await fs.readFile(process.env.FORGE_FIXTURE); const forge = core.parseForge(bytes);
  assert.equal(forge.version.id, core.VERSION); assert.equal(forge.version.inheritsFrom, '1.8.9');
  assert.equal(forge.jar.readUInt32LE(0), 0x04034b50); assert.ok(forge.relativeJar.startsWith('net/minecraftforge/forge/'));
  assert.ok(forge.version.libraries.every(lib => !lib.url || lib.url.startsWith('https:')));
  await assert.rejects(async () => core.parseForge(Buffer.from('wrong installer')), /inválido/);
});
