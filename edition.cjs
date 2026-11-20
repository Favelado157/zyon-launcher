'use strict';
const path = require('node:path');
const fs = require('node:fs/promises');
const core = require('./core.cjs');
const EDITION = 'visual-1.3';
function migrate(previous, root, publisher) {
  const defaultDir = path.join(root, 'instances', 'zyon-visual-1.8.9');
  const sameEdition = previous.edition === EDITION;
  return {
    edition: EDITION, loginMode: 'microsoft',
    instanceDir: sameEdition && typeof previous.instanceDir === 'string' && path.isAbsolute(previous.instanceDir) ? previous.instanceDir : defaultDir,
    previousInstanceDir: typeof (sameEdition ? previous.previousInstanceDir : previous.instanceDir) === 'string' && path.isAbsolute(sameEdition ? previous.previousInstanceDir : previous.instanceDir) ? (sameEdition ? previous.previousInstanceDir : previous.instanceDir) : '',
    microsoftClientId: publisher.microsoftClientId || '', githubRepository: publisher.githubRepository || ''
  };
}
async function validateInstance(instanceDir, payload) {
  const mods = path.join(instanceDir, 'mods');
  if (!await core.exists(mods)) return;
  if ((await fs.lstat(mods)).isSymbolicLink()) throw new Error('Escolha uma pasta nova para a edição visual. A pasta mods não pode ser um link.');
  const manifest = await core.readJson(path.join(payload, 'manifest.json'));
  for (const entry of await fs.readdir(mods, { withFileTypes: true })) {
    if (!entry.isFile() || entry.isSymbolicLink()) throw new Error('Esta pasta contém mods externos à edição visual. Escolha uma pasta nova; os arquivos anteriores serão preservados.');
    const expected = entry.name === core.MOD ? [manifest.sha256] : manifest.previousVisuals?.[entry.name];
    if (!Array.isArray(expected) || !expected.length) throw new Error('Esta pasta contém mods externos à edição visual. Escolha uma pasta nova; os arquivos anteriores serão preservados.');
    const bytes = await fs.readFile(path.join(mods, entry.name));
    if (manifest.file !== core.MOD || !expected.includes(core.digest(bytes))) throw new Error('O cliente visual existente tem conteúdo diferente. Escolha uma pasta nova para preservar esse arquivo.');
  }
}
module.exports = { EDITION, migrate, validateInstance };

async function migrateProfiles(previousDir, instanceDir) {
  if (!previousDir || previousDir === instanceDir) return;
  const source = path.join(previousDir, 'config', 'zyon'), destination = path.join(instanceDir, 'config', 'zyon');
  const allowed = {
    hud: ['logo','fps','coords','modules','x','y'], keystrokes: ['mouse','cps','jump','scale','color','opacity','x','y'],
    theme: ['menu','accent','opacity','rainbow'], badge: [], selfname: ['scale'], crosshair: ['size','gap','dot']
  };
  if (!await core.exists(source) || (await fs.lstat(source)).isSymbolicLink()) return;
  for (const entry of await fs.readdir(source, { withFileTypes: true })) {
    if (!entry.isFile() || !/^[a-zA-Z0-9_-]{1,32}\.json$/.test(entry.name)) continue;
    const file = path.join(source, entry.name), target = path.join(destination, entry.name);
    if (await core.exists(target) || (await fs.stat(file)).size > 1048576) continue;
    let data; try { data = JSON.parse(await fs.readFile(file, 'utf8')); } catch { continue; }
    if (data.schema !== 1 || typeof data.modules !== 'object' || !data.modules) continue;
    const modules = {};
    for (const [id, keys] of Object.entries(allowed)) {
      const old = data.modules[id]; if (!old || typeof old !== 'object') continue;
      const settings = {};
      for (const key of keys) if (typeof old.settings?.[key] === 'number' && Number.isFinite(old.settings[key])) settings[key] = old.settings[key];
      modules[id] = { enabled: old.enabled === true, key: Number.isInteger(old.key) && old.key >= 0 && old.key < 256 ? old.key : 0, settings };
    }
    await core.atomicWrite(target, JSON.stringify({ schema: 1, modules }, null, 2));
  }
}
module.exports.migrateProfiles = migrateProfiles;
