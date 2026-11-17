'use strict';
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const AdmZip = require('adm-zip');
const core = require('./core.cjs');
const FILE = 'OptiFine_1.8.9_HD_U_M5.jar';
const MAX_BYTES = 32 * 1024 * 1024;
const receiptPath = instance => path.join(instance, 'zyon-optifine.json');

function classStrings(data) {
  if (!data || data.length < 10 || data.length > 1024 * 1024 || data.readUInt32BE(0) !== 0xcafebabe) throw new Error('Classe de versão OptiFine inválida.');
  const strings = []; let offset = 10;
  const count = data.readUInt16BE(8);
  for (let i = 1; i < count; i++) {
    if (offset >= data.length) throw new Error('Classe OptiFine incompleta.');
    const tag = data[offset++];
    if (tag === 1) {
      if (offset + 2 > data.length) throw new Error('Classe OptiFine incompleta.');
      const size = data.readUInt16BE(offset); offset += 2;
      if (offset + size > data.length) throw new Error('Classe OptiFine incompleta.');
      strings.push(data.toString('utf8', offset, offset + size)); offset += size;
    } else if ([3, 4, 9, 10, 11, 12, 17, 18].includes(tag)) offset += 4;
    else if ([5, 6].includes(tag)) { offset += 8; i++; }
    else if ([7, 8, 16, 19, 20].includes(tag)) offset += 2;
    else if (tag === 15) offset += 3;
    else throw new Error('Classe OptiFine inválida.');
    if (offset > data.length) throw new Error('Classe OptiFine incompleta.');
  }
  return strings;
}
function inspect(data) {
  if (!Buffer.isBuffer(data) || data.length < 4 || data.length > MAX_BYTES || data.readUInt32LE(0) !== 0x04034b50) throw new Error('Selecione o JAR oficial OptiFine 1.8.9 HD U M5.');
  let zip;
  try { zip = new AdmZip(data); } catch { throw new Error('O arquivo selecionado não é um JAR válido.'); }
  for (const name of ['optifine/OptiFineForgeTweaker.class', 'optifine/OptiFineClassTransformer.class']) {
    if (!zip.getEntry(name)) throw new Error('Este JAR não contém a integração OptiFine para Forge.');
  }
  const entry = ['Config.class', 'net/minecraft/src/Config.class', 'net/optifine/Config.class'].map(name => zip.getEntry(name)).find(Boolean);
  if (!entry || entry.header.size > 1024 * 1024) throw new Error('Não foi possível identificar a versão do OptiFine. Use o JAR original, sem extrair.');
  const strings = classStrings(entry.getData());
  const full = strings.includes('OptiFine_1.8.9_HD_U_M5');
  if (!full && !(strings.includes('1.8.9') && strings.includes('HD_U') && strings.includes('M5') && strings.includes('OptiFine'))) throw new Error('Esta instância exige OptiFine 1.8.9 HD U M5, compatível com Forge 2318.');
  return { version: '1.8.9 HD U M5', sha256: core.digest(data) };
}
async function regularFile(file) {
  const stat = await fs.lstat(file);
  if (!stat.isFile() || stat.isSymbolicLink() || stat.size > MAX_BYTES) throw new Error('Selecione um arquivo JAR local de até 32 MB, sem links simbólicos.');
}
async function directory(folder) {
  await fs.mkdir(folder, { recursive: true });
  if ((await fs.lstat(folder)).isSymbolicLink()) throw new Error('A pasta de mods ou de backup não pode ser um link simbólico.');
}
async function candidates(instance) {
  const result = [];
  for (const folder of [path.join(instance, 'mods'), path.join(instance, 'mods', '1.8.9')]) {
    if (!await core.exists(folder)) continue;
    if ((await fs.lstat(folder)).isSymbolicLink()) throw new Error('A pasta de mods não pode ser um link simbólico.');
    for (const name of await fs.readdir(folder)) {
      if (!name.toLowerCase().endsWith('.jar')) continue;
      const file = path.join(folder, name);
      const stat = await fs.lstat(file);
      if (!stat.isFile() || stat.isSymbolicLink() || stat.size > MAX_BYTES) {
        if (/optifine/i.test(name)) throw new Error('O JAR OptiFine existente é grande demais ou é um link simbólico. Confira a pasta da instância.');
        continue;
      }
      try {
        const data = await fs.readFile(file), zip = new AdmZip(data);
        if (zip.getEntry('optifine/OptiFineForgeTweaker.class') || zip.getEntry('optifine/OptiFineClassTransformer.class')) result.push({ file, data });
      } catch (error) { if (/optifine/i.test(name)) throw new Error('Já existe um JAR OptiFine inválido na pasta mods. Confira esse arquivo antes de importar.'); }
    }
  }
  return result;
}
async function status(instance) {
  try {
    const found = await candidates(instance);
    if (!found.length) return { installed: false, managed: false };
    if (found.length > 1) return { installed: false, managed: false, problem: 'Há mais de um OptiFine nesta instância. Mantenha apenas um JAR.' };
    const info = inspect(found[0].data), receipt = await core.readJson(receiptPath(instance), {});
    return { installed: true, version: info.version, fileName: path.basename(found[0].file), managed: found[0].file === path.join(instance, 'mods', FILE) && receipt.sha256 === info.sha256 };
  } catch (error) { return { installed: false, managed: false, problem: error.message }; }
}
async function install(source, instance) {
  if (path.extname(source).toLowerCase() !== '.jar') throw new Error('Selecione o arquivo .jar baixado do site oficial OptiFine.');
  await regularFile(source);const data = await fs.readFile(source), info = inspect(data);
  const found = await candidates(instance);
  if (found.length) {
    if (found.length === 1 && core.digest(found[0].data) === info.sha256) return status(instance);
    throw new Error('Já existe outro OptiFine nesta instância. Remova-o antes de importar para evitar dois JARs em conflito.');
  }
  const folder = path.join(instance, 'mods'); await directory(folder);
  const target = path.join(folder, FILE);
  if (await core.exists(target)) throw new Error('Já existe um arquivo com esse nome. Ele foi preservado.');
  // Copy, never execute the installer, and never extract files from the selected JAR.
  await fs.copyFile(source, target, fs.constants.COPYFILE_EXCL);
  try {
    if (core.digest(await fs.readFile(target)) !== info.sha256) throw new Error('O arquivo mudou durante a importação. Tente novamente.');
    await core.atomicWrite(receiptPath(instance), JSON.stringify(info, null, 2));
  } catch (error) { await fs.rm(target, { force: true }); throw error; }
  return status(instance);
}
async function remove(instance) {
  const current = await status(instance);
  if (!current.managed) throw new Error('O Zyon só remove o OptiFine importado por ele e que não foi alterado. Confira os demais arquivos pela pasta da instância.');
  const backups = path.join(instance, 'zyon-backups'); await directory(backups);
  await fs.rename(path.join(instance, 'mods', FILE), path.join(backups, FILE + '.' + crypto.randomUUID() + '.bak'));
  await fs.rm(receiptPath(instance), { force: true });
  return status(instance);
}
module.exports = { FILE, inspect, status, install, remove };
