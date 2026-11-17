'use strict';
const fs = require('node:fs/promises');
const path = require('node:path');
const { PublicClientApplication } = require('@azure/msal-node');
const { atomicWrite, readJson } = require('./core.cjs');
const SCOPES = ['XboxLive.signin', 'offline_access'];
const CLIENT_PATTERN = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function validClientId(value) { return typeof value === 'string' && CLIENT_PATTERN.test(value); }
async function jsonRequest(url, body, signal, request = fetch, bearer) {
  const response = await request(url, {
    method: body ? 'POST' : 'GET', signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(30000)]) : AbortSignal.timeout(30000),
    headers: { Accept: 'application/json', ...(body ? { 'Content-Type': 'application/json' } : {}), ...(bearer ? { Authorization: 'Bearer ' + bearer } : {}) },
    ...(body ? { body: JSON.stringify(body) } : {})
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    if (url.includes('minecraftservices.com') && response.status === 403) throw new Error('O aplicativo Microsoft deste launcher ainda não foi aprovado para os serviços do Minecraft. O publicador precisa concluir a configuração.');
    if (data.XErr === 2148916233) throw new Error('Sua conta precisa de um perfil Xbox. Configure-o no site oficial do Xbox.');
    if (data.XErr === 2148916238) throw new Error('O Xbox exige configuração da conta familiar antes de continuar.');
    throw new Error(`O login foi recusado por ${new URL(url).hostname} (HTTP ${response.status}).`);
  }
  return data;
}
async function minecraftSession(microsoftToken, signal, request = fetch) {
  const xbox = await jsonRequest('https://user.auth.xboxlive.com/user/authenticate', {
    Properties: { AuthMethod: 'RPS', SiteName: 'user.auth.xboxlive.com', RpsTicket: 'd=' + microsoftToken },
    RelyingParty: 'http://auth.xboxlive.com', TokenType: 'JWT'
  }, signal, request);
  const xsts = await jsonRequest('https://xsts.auth.xboxlive.com/xsts/authorize', {
    Properties: { SandboxId: 'RETAIL', UserTokens: [xbox.Token] }, RelyingParty: 'rp://api.minecraftservices.com/', TokenType: 'JWT'
  }, signal, request);
  const hash = xsts.DisplayClaims?.xui?.[0]?.uhs;
  if (!hash || !xsts.Token) throw new Error('O Xbox retornou uma sessão incompleta.');
  const game = await jsonRequest('https://api.minecraftservices.com/authentication/login_with_xbox', { identityToken: 'XBL3.0 x=' + hash + ';' + xsts.Token }, signal, request);
  if (typeof game.access_token !== 'string' || !game.access_token) throw new Error('O Minecraft não retornou uma sessão válida.');
  const ownership = await jsonRequest('https://api.minecraftservices.com/entitlements/mcstore', null, signal, request, game.access_token);
  if (!Array.isArray(ownership.items) || ownership.items.length === 0) throw new Error('Esta conta não possui acesso ao Minecraft Java. Confira sua compra ou assinatura no site oficial.');
  const profile = await jsonRequest('https://api.minecraftservices.com/minecraft/profile', null, signal, request, game.access_token);
  if (!/^[a-f0-9]{32}$/i.test(profile.id || '') || !/^[A-Za-z0-9_]{1,16}$/.test(profile.name || '')) throw new Error('O perfil Minecraft não foi encontrado. Configure o nome Java na sua conta.');
  return { uuid: profile.id, name: profile.name, accessToken: game.access_token, expiresAt: Date.now() + Number(game.expires_in || 3600) * 1000 };
}
class MicrosoftAccount {
  constructor({ root, getClientId, secureStore, publicClientFactory = config => new PublicClientApplication(config), request = fetch }) {
    this.root = root; this.getClientId = getClientId; this.secureStore = secureStore; this.factory = publicClientFactory; this.request = request;
    this.client = null; this.clientId = null; this.session = null;
  }
  configured() { return validClientId(this.getClientId()); }
  async publicClient() {
    const clientId = this.getClientId();
    if (!validClientId(clientId)) throw new Error('O login próprio precisa ser ativado pelo publicador com um Client ID Microsoft aprovado para Minecraft. Veja o guia de configuração.');
    if (!this.secureStore.isEncryptionAvailable()) throw new Error('O armazenamento protegido do Windows está indisponível. A sessão não será salva em texto simples.');
    if (this.client && this.clientId === clientId) return this.client;
    this.clientId = clientId; this.session = null;
    const file = path.join(this.root, 'accounts', clientId + '.bin');
    this.client = this.factory({
      auth: { clientId, authority: 'https://login.microsoftonline.com/consumers' },
      cache: { cachePlugin: {
        beforeCacheAccess: async context => {
          try { context.tokenCache.deserialize(this.secureStore.decryptString(await fs.readFile(file))); }
          catch (error) { if (error.code !== 'ENOENT') throw new Error('Não foi possível abrir a sessão protegida. Saia da conta e entre novamente.'); }
        },
        afterCacheAccess: async context => {
          if (context.cacheHasChanged) await atomicWrite(file, this.secureStore.encryptString(context.tokenCache.serialize()));
        }
      } },
      system: { loggerOptions: { piiLoggingEnabled: false, loggerCallback: () => {} } }
    });
    return this.client;
  }
  async profile() {
    const value = await readJson(path.join(this.root, 'account.json'), null).catch(() => null);
    return value && value.clientId === this.getClientId() ? { name: value.name, uuid: value.uuid } : null;
  }
  async record(session) {
    this.session = session;
    await atomicWrite(path.join(this.root, 'account.json'), JSON.stringify({ clientId: this.getClientId(), name: session.name, uuid: session.uuid }));
    return { name: session.name, uuid: session.uuid };
  }
  async signIn(signal, onCode = () => {}) {
    const client = await this.publicClient();
    const request = { scopes: SCOPES, timeout: 600, deviceCodeCallback: response => {
      const url = new URL(response.verificationUri);
      if (url.protocol !== 'https:' || !['microsoft.com', 'www.microsoft.com', 'login.microsoftonline.com'].includes(url.hostname)) throw new Error('Endereço de autenticação Microsoft inesperado.');
      onCode({ code: response.userCode, url: url.toString(), expiresIn: response.expiresIn });
    } };
    const cancel = () => { request.cancel = true; };
    signal?.addEventListener('abort', cancel, { once: true });
    try {
      if (signal?.aborted) throw new Error('Login cancelado.');
      const result = await client.acquireTokenByDeviceCode(request);
      if (signal?.aborted || !result?.accessToken) throw new Error('Login cancelado ou expirado.');
      return this.record(await minecraftSession(result.accessToken, signal, this.request));
    } catch (error) {
      if (signal?.aborted || /cancel|expired/i.test(error.errorCode || '')) throw new Error('Login cancelado ou expirado.');
      if (error.errorCode) throw new Error(`A Microsoft não concluiu o login (${error.errorCode}). Confira a configuração do aplicativo.`);
      throw error;
    } finally { signal?.removeEventListener('abort', cancel); }
  }
  async getSession(signal) {
    if (this.session && this.session.expiresAt > Date.now() + 60000 && this.clientId === this.getClientId()) return this.session;
    const client = await this.publicClient();
    const accounts = await client.getTokenCache().getAllAccounts();
    if (!accounts.length) throw new Error('Entre na sua conta Microsoft antes de clicar em Play.');
    let result;
    try { result = await client.acquireTokenSilent({ account: accounts[0], scopes: SCOPES }); }
    catch { throw new Error('Sua sessão expirou. Entre na conta Microsoft novamente.'); }
    const session = await minecraftSession(result.accessToken, signal, this.request); await this.record(session); return session;
  }
  async signOut() {
    const id = this.getClientId();
    if (validClientId(id)) await fs.rm(path.join(this.root, 'accounts', id + '.bin'), { force: true });
    await fs.rm(path.join(this.root, 'account.json'), { force: true }); this.client = null; this.session = null; this.clientId = null;
  }
}
module.exports = { MicrosoftAccount, minecraftSession, jsonRequest, validClientId };
