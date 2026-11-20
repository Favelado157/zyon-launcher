'use strict';
// A game session is accepted only after the official Minecraft services verify ownership.
function loginSettings() { return { loginMode: 'microsoft' }; }
async function resolveSession({ mode = 'microsoft', microsoftAccount, signal }) {
  if (mode !== 'microsoft') throw new Error('Esta edição exige conta Microsoft com acesso verificado ao Minecraft Java.');
  const session = { ...await microsoftAccount.getSession(signal), mode: 'microsoft', userType: 'mojang' };
  validateSession(session);
  return session;
}
function validateSession(session) {
  if (!session || session.mode !== 'microsoft' || session.userType !== 'mojang' || !/^[A-Za-z0-9_]{1,16}$/.test(session.name || '') || !/^[a-f0-9]{32}$/i.test(session.uuid || '') || typeof session.accessToken !== 'string' || !session.accessToken || session.accessToken === '0' || session.ownershipVerified !== true || !Number.isFinite(session.verifiedAt) || session.verifiedAt > Date.now() + 5000 || session.verifiedAt < Date.now() - 300000 || !Number.isFinite(session.expiresAt) || session.expiresAt <= Date.now() + 10000) throw new Error('Sessão sem verificação recente de propriedade do Minecraft Java. Entre com Microsoft novamente.');
  return 'mojang';
}
module.exports = { loginSettings, resolveSession, validateSession };
