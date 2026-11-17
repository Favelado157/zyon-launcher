'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const sessions = require('../sessions.cjs');
const game = require('../game.cjs');

test('offline nick validation excludes paths, control characters and invalid Minecraft names', () => {
  assert.equal(sessions.nickname('  Player_123  '), 'Player_123');
  for (const name of ['', 'ab', 'a'.repeat(17), '../Player', 'My Nick', '<script>', 'José', 'Player\nName', null, {}, 123]) assert.throws(() => sessions.offlineSession(name), /nick/);
});
test('offline identity uses the standard name-based UUID and preserves case', () => {
  const profile = sessions.offlineSession('Notch');
  assert.equal(profile.uuid, 'b50ad385829d3141a2167e7d7539ba7f');
  assert.deepEqual(sessions.offlineSession('Notch'), profile);
  assert.notEqual(sessions.offlineSession('notch').uuid, profile.uuid);
  assert.equal(profile.accessToken, '0');assert.equal(profile.userType, 'legacy');
});
test('offline launch resolves without Microsoft, Xbox, ownership or device-code requests', async () => {
  let requests = 0;
  const microsoftAccount = { getSession: async () => { requests++;throw new Error('Microsoft must not be called'); } };
  const profile = await sessions.resolveSession({ mode: 'offline', name: 'ZyonPlayer', microsoftAccount });
  assert.equal(profile.mode, 'offline');assert.equal(requests, 0);
});
test('Microsoft session failures cannot silently fall back to a nick', async () => {
  const microsoftAccount = { getSession: async () => { throw new Error('Conta sem acesso ao Java'); } };
  await assert.rejects(sessions.resolveSession({ mode: 'microsoft', name: 'ZyonPlayer', microsoftAccount }), /sem acesso/);
  await assert.rejects(sessions.resolveSession({ mode: 'unexpected', name: 'ZyonPlayer', microsoftAccount }), /Escolha/);
});
test('saved login preferences survive reload and invalid preferences reset safely', () => {
  const saved = JSON.parse(JSON.stringify({ loginMode: 'offline', offlineNickname: 'ZyonPlayer' }));
  assert.deepEqual(sessions.loginSettings(saved), saved);
  assert.deepEqual(sessions.loginSettings({ loginMode: 'microsoft', offlineNickname: 'ZyonPlayer' }), { loginMode: 'microsoft', offlineNickname: 'ZyonPlayer' });
  assert.deepEqual(sessions.loginSettings({ loginMode: 'invalid', offlineNickname: '../evil' }), { loginMode: 'offline', offlineNickname: '' });
});
test('offline Java arguments carry only the derived offline identity and a placeholder token', () => {
  const ready = { classpath: ['C:\\game\\minecraft.jar'], natives: 'C:\\game\\natives', instanceDir: 'C:\\My Game', assetsRoot: 'C:\\game\\assets', assetsIndex: '1.8', mainClass: 'net.minecraft.launchwrapper.Launch', arguments: '--username ${auth_player_name} --uuid ${auth_uuid} --accessToken ${auth_access_token} --userType ${user_type}' };
  const profile = sessions.offlineSession('Notch'), args = game.launchArguments(ready, profile, 2);
  const value = key => args[args.indexOf(key) + 1];
  assert.equal(value('--username'), 'Notch');assert.equal(value('--uuid'), profile.uuid);assert.equal(value('--accessToken'), '0');assert.equal(value('--userType'), 'legacy');
  assert.throws(() => game.launchArguments(ready, { ...profile, uuid: 'a'.repeat(32) }, 2), /offline/);
  assert.throws(() => game.launchArguments(ready, { ...profile, accessToken: 'pretend-token' }, 2), /offline/);
  assert.throws(() => game.launchArguments(ready, { ...profile, mode: 'microsoft' }, 2), /Microsoft/);
});
test('offline logs retain ordinary zeroes while Microsoft tokens remain redacted', () => {
  assert.equal(game.redactGameLog('100 FPS at 10:00', sessions.offlineSession('ZyonPlayer')), '100 FPS at 10:00');
  assert.equal(game.redactGameLog('token=PRIVATE', { mode: 'microsoft', accessToken: 'PRIVATE' }), 'token=[sessão omitida]');
});
