'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const { MicrosoftAccount, minecraftSession } = require('../auth.cjs');
const CLIENT = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee';
const secure = { isEncryptionAvailable: () => true, encryptString: text => Buffer.from('encrypted:' + Buffer.from(text).toString('base64')), decryptString: data => Buffer.from(data.toString().replace('encrypted:', ''), 'base64').toString() };
test('login requires the publisher client ID and protected storage', async () => {
  const absent = new MicrosoftAccount({ root: '/unused', getClientId: () => '', secureStore: secure });
  await assert.rejects(absent.publicClient(), /Client ID/);
  const insecure = new MicrosoftAccount({ root: '/unused', getClientId: () => CLIENT, secureStore: { isEncryptionAvailable: () => false } });
  await assert.rejects(insecure.publicClient(), /texto simples/);
});
test('MSAL cache writes only encrypted bytes and logout removes the saved account', async t => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'zyon-auth-')); t.after(() => fs.rm(root, { recursive: true, force: true }));
  let config;
  const account = new MicrosoftAccount({ root, getClientId: () => CLIENT, secureStore: secure, publicClientFactory: value => { config = value; return {}; } });
  await account.publicClient();
  await config.cache.cachePlugin.afterCacheAccess({ cacheHasChanged: true, tokenCache: { serialize: () => 'REFRESH_TOKEN_SECRET' } });
  const saved = await fs.readFile(path.join(root, 'accounts', CLIENT + '.bin'));
  assert.equal(saved.includes(Buffer.from('REFRESH_TOKEN_SECRET')), false);
  let deserialized;
  await config.cache.cachePlugin.beforeCacheAccess({ tokenCache: { deserialize: value => { deserialized = value; } } });
  assert.equal(deserialized, 'REFRESH_TOKEN_SECRET'); await account.signOut();
  await assert.rejects(fs.readFile(path.join(root, 'accounts', CLIENT + '.bin')));
});
test('Minecraft session performs Xbox, XSTS, ownership, and profile exchange without reusing other launchers tokens', async () => {
  const urls = [], bodies = [];
  const data = [{ Token: 'XBOX' }, { Token: 'XSTS', DisplayClaims: { xui: [{ uhs: 'HASH' }] } }, { access_token: 'GAME_TOKEN', expires_in: 3600 }, { items: [{ name: 'game_minecraft' }] }, { id: 'b'.repeat(32), name: 'Player' }];
  const session = await minecraftSession('MS_TOKEN', null, async (url, options) => { urls.push(url); bodies.push(options.body); return new Response(JSON.stringify(data[urls.length - 1])); });
  assert.equal(session.accessToken, 'GAME_TOKEN'); assert.equal(session.name, 'Player'); assert.equal(urls.length, 5);
  assert.ok(bodies[0].includes('d=MS_TOKEN')); assert.ok(bodies[2].includes('XBL3.0 x=HASH;XSTS'));
});
test('missing ownership blocks a direct game session', async () => {
  const data = [{ Token: 'XBOX' }, { Token: 'XSTS', DisplayClaims: { xui: [{ uhs: 'HASH' }] } }, { access_token: 'GAME_TOKEN' }, { items: [] }]; let index = 0;
  await assert.rejects(minecraftSession('MS_TOKEN', null, async () => new Response(JSON.stringify(data[index++]))), /não possui acesso/);
});
test('Minecraft app registration rejection is reported without leaking credentials', async () => {
  let index = 0;
  const responses = [{ Token: 'XBOX' }, { Token: 'XSTS', DisplayClaims: { xui: [{ uhs: 'HASH' }] } }];
  await assert.rejects(minecraftSession('MS_TOKEN', null, async () => index < 2 ? new Response(JSON.stringify(responses[index++])) : new Response('{"errorMessage":"Invalid app registration"}', { status: 403 })), error => error.message.includes('aprovado') && !error.message.includes('MS_TOKEN'));
});
test('cancelling device login stops before exchanging or persisting account tokens', async t => {
  const root = await fs.mkdtemp(path.join(os.tmpdir(), 'zyon-auth-')); t.after(() => fs.rm(root, { recursive: true, force: true }));
  const controller = new AbortController(); let deviceRequest;
  const account = new MicrosoftAccount({ root, getClientId: () => CLIENT, secureStore: secure,
    publicClientFactory: () => ({ acquireTokenByDeviceCode: async request => { deviceRequest = request; controller.abort(); return { accessToken: 'SHOULD_NOT_BE_USED' }; } }),
    request: async () => { throw new Error('No account request expected'); }
  });
  await assert.rejects(account.signIn(controller.signal), /cancelado/); assert.equal(deviceRequest.cancel, true);
  await assert.rejects(fs.readFile(path.join(root, 'account.json')));
});
