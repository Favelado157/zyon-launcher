'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const updates = require('../updates.cjs');
const core = require('../core.cjs');
test('version comparison is numeric and never proposes a downgrade', () => {
  assert.ok(updates.newer('1.10.0', '1.9.9')); assert.equal(updates.newer('1.1.0', '1.1.0'), false); assert.equal(updates.newer('1.0.0', '1.1.0'), false);
  assert.throws(() => updates.newer('1.2.0-beta', '1.1.0'));
});
test('repository and installer URLs remain restricted to the chosen GitHub repository', () => {
  assert.equal(updates.normalizeRepository('https://github.com/Owner/Project.git'), 'Owner/Project');
  assert.equal(updates.trustedAsset('https://github.com/owner/project/releases/download/v1.2.0/app.exe', 'Owner/Project'), true);
  for (const url of ['http://github.com/owner/project/releases/download/a.exe', 'https://evil.test/owner/project/releases/download/a.exe', 'https://github.com/other/project/releases/download/a.exe']) assert.equal(updates.trustedAsset(url, 'owner/project'), false);
  assert.throws(() => updates.normalizeRepository('https://evil.test/owner/project'));
});
test('an unconfigured channel does not pretend to offer working global updates', async () => {
  const result = await updates.check({ repository: '', currentVersion: '1.1.0', request: () => { throw new Error('No network expected'); } });
  assert.equal(result.configured, false); assert.equal(result.available, false);
});
test('the release checker accepts a newer installer with a verified digest', async () => {
  const release = { tag_name: 'v1.2.0', draft: false, prerelease: false, body: '<script>untrusted notes</script>', assets: [{ name: 'Zyon-Launcher-Setup-1.2.0.exe', browser_download_url: 'https://github.com/owner/project/releases/download/v1.2.0/Zyon-Launcher-Setup-1.2.0.exe', size: 40000, digest: 'sha256:' + 'a'.repeat(64) }] };
  const result = await updates.check({ repository: 'owner/project', currentVersion: '1.1.0', request: async () => new Response(JSON.stringify(release)) });
  assert.equal(result.available, true); assert.equal(result.sha256, 'a'.repeat(64)); assert.equal(result.version, '1.2.0'); assert.equal(result.notes, release.body);
});
test('a corrupted update cannot be written or executed', async t => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'zyon-update-')); t.after(() => fs.rm(root, { recursive: true, force: true }));
  const update = { available: true, version: '1.2.0', repository: 'owner/project', name: 'Zyon-Launcher-Setup-1.2.0.exe', url: 'https://github.com/owner/project/releases/download/v1.2.0/Zyon-Launcher-Setup-1.2.0.exe', sha256: 'a'.repeat(64), size: 10000 };
  await assert.rejects(updates.download(update, { root, request: async () => new Response('corrupted') }), /verificação/);
  assert.equal(await core.exists(path.join(root, 'updates')), false);
});
test('release checksum files are supported when GitHub does not provide an asset digest', async () => {
  const base = 'https://github.com/owner/project/releases/download/v1.2.0/';
  const release = { tag_name: 'v1.2.0', assets: [{ name: 'Zyon-Launcher-Setup-1.2.0.exe', size: 20000, browser_download_url: base + 'Zyon-Launcher-Setup-1.2.0.exe' }, { name: 'SHA256SUMS.txt', browser_download_url: base + 'SHA256SUMS.txt' }] };
  const request = async url => new Response(url.includes('api.github.com') ? JSON.stringify(release) : 'b'.repeat(64) + '  Zyon-Launcher-Setup-1.2.0.exe\n');
  const result = await updates.check({ repository: 'owner/project', currentVersion: '1.1.0', request });
  assert.equal(result.sha256, 'b'.repeat(64)); assert.equal(result.available, true);
});
test('a correct executable download is committed only after all checks', async t => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'zyon-update-')); t.after(() => fs.rm(root, { recursive: true, force: true }));
  const data = Buffer.alloc(20000); data.write('MZ');
  const update = { available: true, version: '1.2.0', repository: 'owner/project', name: 'Zyon-Launcher-Setup-1.2.0.exe', url: 'https://github.com/owner/project/releases/download/v1.2.0/Zyon-Launcher-Setup-1.2.0.exe', sha256: core.digest(data), size: data.length };
  const file = await updates.download(update, { root, request: async () => new Response(data) }); assert.deepEqual(await fs.readFile(file), data);
});
