'use strict';
const crypto = require('node:crypto');
const issued = new WeakSet();
function nickname(value) {
  const name = typeof value === 'string' ? value.trim() : '';
  if (!/^[A-Za-z0-9_]{3,16}$/.test(name)) throw new Error('Use um nick de 3 a 16 caracteres, com letras, números ou _.');
  return name;
}
function loadProfile(value) {
  const name = nickname(value);
  // Matches Java UUID.nameUUIDFromBytes("OfflinePlayer:" + name), without an online identity.
  const bytes = crypto.createHash('md5').update('OfflinePlayer:' + name, 'utf8').digest();
  bytes[6] = (bytes[6] & 0x0f) | 0x30; bytes[8] = (bytes[8] & 0x3f) | 0x80;
  return { name, uuid: bytes.toString('hex'), source: 'local-nickname' };
}
function profileStatus(value) {
  try { return { profile: loadProfile(value), error: '' }; }
  catch (error) { return { profile: null, error: error.message }; }
}
function resolveSession(value) {
  const profile = loadProfile(value);
  const session = Object.freeze({ name: profile.name, uuid: profile.uuid, mode: 'nickname-offline', userType: 'legacy', accessToken: '0' });
  issued.add(session); return session;
}
function isOfflineSession(session) { return !!session && issued.has(session); }
module.exports = { nickname, loadProfile, profileStatus, resolveSession, isOfflineSession };
