'use strict';
const fs = require('node:fs');
const path = require('node:path');
const { normalizeRepository } = require('../updates.cjs');
const { validClientId } = require('../auth.cjs');
function publisherConfiguration(repository, clientId = '') {
  const repo = normalizeRepository(repository); const id = String(clientId).trim();
  if (!repo) throw new Error('Informe um repositório público no formato usuario/repositorio.');
  if (id && !validClientId(id)) throw new Error('O Client ID precisa ser um identificador público Microsoft válido.');
  return { githubRepository: repo, microsoftClientId: id };
}
if (require.main === module) {
  const args = process.argv.slice(2);
  const value = name => { const index = args.indexOf(name); return index < 0 ? '' : args[index + 1] || ''; };
  try {
    const config = publisherConfiguration(value('--repository'), value('--client-id'));
    const existing = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'release-config.json'), 'utf8'));
    if (existing.discordInvite) config.discordInvite = existing.discordInvite;
    fs.writeFileSync(path.join(__dirname, '..', 'release-config.json'), JSON.stringify(config, null, 2) + '\n');
    console.log('Canal público configurado: ' + config.githubRepository);
    console.log(config.microsoftClientId ? 'Client ID incluído. O login depende da aprovação Microsoft/Minecraft.' : 'Client ID não informado: atualizações habilitadas; login online próprio continua pendente.');
  } catch (error) { console.error(error.message); process.exitCode = 1; }
}
module.exports = { publisherConfiguration };
