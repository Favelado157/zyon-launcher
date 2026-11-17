'use strict';
const crypto = require('node:crypto');

const NAME = /^[A-Za-z0-9_]{3,16}$/;
function validNickname(value) { return typeof value === 'string' && NAME.test(value); }
function nickname(value) {
  if (typeof value !== 'string' || !validNickname(value.trim())) throw new Error('Use um nick de 3 a 16 caracteres, com letras, números ou _.');
  return value.trim();
}
function offlineSession(value) {
  const name = nickname(value);
  // Matches Java UUID.nameUUIDFromBytes("OfflinePlayer:" + name), including case.
  const bytes = crypto.createHash('md5').update('OfflinePlayer:' + name, 'utf8').digest();
  bytes[6] = (bytes[6] & 0x0f) | 0x30;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  return { mode: 'offline', name, uuid: bytes.toString('hex'), accessToken: '0', userType: 'legacy' };
}
function loginSettings(value = {}) {
  return { loginMode: value.loginMode === 'microsoft' ? 'microsoft' : 'offline', offlineNickname: validNickname(value.offlineNickname) ? value.offlineNickname : '' };
}
async function resolveSession({ mode, name, microsoftAccount, signal }) {
  if (mode === 'offline') return offlineSession(name);
  if (mode !== 'microsoft') throw new Error('Escolha nick offline ou conta Microsoft em Minha conta.');
  // A Microsoft failure must not silently switch the player to an offline identity.
  return { ...await microsoftAccount.getSession(signal), mode: 'microsoft', userType: 'mojang' };
}
function validateSession(session) {
  if (!session || !/^[A-Za-z0-9_]{1,16}$/.test(session.name || '') || !/^[a-f0-9]{32}$/i.test(session.uuid || '') || typeof session.accessToken !== 'string' || !session.accessToken) throw new Error('Sessão Minecraft inválida. Escolha seu nick ou conecte sua conta.');
  if (session.mode === 'offline') {
    const expected = offlineSession(session.name);
    if (session.uuid !== expected.uuid || session.accessToken !== '0' || session.userType !== 'legacy') throw new Error('Perfil offline inválido. Salve o nick novamente.');
    return 'legacy';
  }
  if ((session.mode !== undefined && session.mode !== 'microsoft') || session.accessToken === '0') throw new Error('Sessão Microsoft inválida. Entre na conta novamente.');
  return 'mojang';
}
module.exports = { validNickname, nickname, offlineSession, loginSettings, resolveSession, validateSession };
