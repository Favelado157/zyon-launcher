'use strict';
function validateSession(session){if(!require('./offline.cjs').isOfflineSession(session)||session.mode!=='nickname-offline'||session.userType!=='legacy'||session.accessToken!=='0'||!/^[A-Za-z0-9_]{3,16}$/.test(session.name||'')||!/^[a-f0-9]{32}$/.test(session.uuid||''))throw Error('Sessão local inválida. Salve seu nick novamente.');return 'legacy';}
module.exports={validateSession};
