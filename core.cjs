'use strict';
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawn } = require('node:child_process');
const AdmZip = require('adm-zip');
const extractZip = require('extract-zip');

const FORGE_URL = 'https://maven.minecraftforge.net/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/forge-1.8.9-11.15.1.2318-1.8.9-installer.jar';
const FORGE_SHA256 = 'f9fdf4945ca02d73ec6cc46300942f4e199e4add068877d517157b3677563656';
const VERSION = 'zyon-1.8.9-forge';
const MOD = 'zyon-1.8.9-1.2.1.jar';
const digest = (data, algorithm = 'sha256') => crypto.createHash(algorithm).update(data).digest('hex');
const exists = async file => { try { await fs.access(file); return true; } catch { return false; } };
function checkAbort(signal) { if (signal?.aborted) throw new Error('Operação cancelada. Você pode retomar a instalação.'); }
function inside(root, file) {
  const relative = path.relative(path.resolve(root), path.resolve(file));
  return relative === '' || (!relative.startsWith('..' + path.sep) && relative !== '..' && !path.isAbsolute(relative));
}
async function atomicWrite(file, data) {
  await fs.mkdir(path.dirname(file), { recursive: true });
  const tmp = `${file}.${crypto.randomUUID()}.tmp`;
  try { await fs.writeFile(tmp, data, { flag: 'wx', mode: 0o600 }); await fs.rename(tmp, file); }
  finally { await fs.rm(tmp, { force: true }).catch(() => {}); }
}
async function readJson(file, fallback) {
  try {
    if ((await fs.stat(file)).size > 10 * 1024 * 1024) throw new Error('Arquivo JSON excede 10 MB.');
    return JSON.parse(await fs.readFile(file, 'utf8'));
  } catch (error) { if (error.code === 'ENOENT' && fallback !== undefined) return fallback; throw error; }
}
async function backup(file) {
  if (!await exists(file)) return;
  await fs.copyFile(file, `${file}.zyon-backup-${Date.now()}-${crypto.randomBytes(3).toString('hex')}`, 1);
}
function coordinatePath(name) {
  const parts = name.split(':');
  if (parts.length < 3 || parts.length > 4 || parts.some(part => !/^[a-zA-Z0-9_.-]+$/.test(part) || part === '.' || part === '..')) throw new Error('Coordenada de biblioteca inválida.');
  const [group, artifact, version, classifier] = parts;
  return `${group.replaceAll('.', '/')}/${artifact}/${version}/${artifact}-${version}${classifier ? '-' + classifier : ''}.jar`;
}
async function fetchBuffer(url, { hash, algorithm = 'sha256', maxBytes = 256 * 1024 * 1024, signal, report = () => {}, request = fetch } = {}) {
  if (new URL(url).protocol !== 'https:') throw new Error('O download exige HTTPS.');
  checkAbort(signal);
  const timed = AbortSignal.timeout(600000);
  const response = await request(url, { signal: signal ? AbortSignal.any([signal, timed]) : timed, redirect: 'follow' });
  if (!response.ok) throw new Error(`Falha no download (${response.status}): ${new URL(url).hostname}`);
  const expected = Number(response.headers.get('content-length') || 0);
  if (expected > maxBytes) throw new Error('Download maior que o limite permitido.');
  const chunks = []; let count = 0;
  for await (const part of response.body) {
    checkAbort(signal); const chunk = Buffer.from(part); count += chunk.length;
    if (count > maxBytes) throw new Error('Download maior que o limite permitido.');
    chunks.push(chunk); report(count, expected);
  }
  const result = Buffer.concat(chunks);
  if (hash && digest(result, algorithm) !== hash.toLowerCase()) throw new Error('A verificação do arquivo falhou. Tente novamente.');
  return result;
}
async function downloadVerified(url, file, hash, algorithm, options = {}) {
  if (await exists(file)) {
    if (digest(await fs.readFile(file), algorithm) === hash.toLowerCase()) return;
    throw new Error(`Já existe um arquivo diferente: ${path.basename(file)}. Ele foi preservado.`);
  }
  const data = await fetchBuffer(url, { ...options, hash, algorithm });
  checkAbort(options.signal); await atomicWrite(file, data);
}
async function installMod(payload, instanceDir) {
  const manifest = await readJson(path.join(payload, 'manifest.json'));
  if (manifest.file !== MOD || !/^[a-f0-9]{64}$/.test(manifest.sha256)) throw new Error('Manifesto do cliente inválido.');
  const data = await fs.readFile(path.join(payload, MOD));
  if (digest(data) !== manifest.sha256 || data.readUInt32LE(0) !== 0x04034b50) throw new Error('O cliente incluído não passou na verificação.');
  const mods = path.join(instanceDir, 'mods');
  if (await exists(mods) && (await fs.lstat(mods)).isSymbolicLink()) throw new Error('A pasta mods não pode ser um link simbólico.');
  await fs.mkdir(mods, { recursive: true });
  const target = path.join(mods, MOD);
  let needsWrite = true;
  if (await exists(target)) {
    if ((await fs.lstat(target)).isSymbolicLink()) throw new Error('O arquivo do mod não pode ser um link simbólico.');
    if (digest(await fs.readFile(target)) === manifest.sha256) needsWrite = false;
    else await backup(target);
  }
  if (needsWrite) await atomicWrite(target, data);
  // Move only previous Zyon binaries out of Forge's scan, keeping configurations and a reversible backup.
  for (const previous of manifest.previousFiles || []) {
    if (!/^zyon-1\.8\.9-[0-9.]+\.jar$/.test(previous) || previous === MOD) continue;
    const old = path.join(mods, previous);
    if (!await exists(old)) continue;
    if ((await fs.lstat(old)).isSymbolicLink()) throw new Error('Uma versão anterior do Zyon é um link simbólico. Ela foi preservada.');
    const zip = new AdmZip(await fs.readFile(old));
    const info = JSON.parse(zip.readAsText('mcmod.info'));
    if (!Array.isArray(info) || !info.some(mod => mod.modid === 'zyon')) throw new Error('Existe um JAR anterior inesperado. Ele foi preservado.');
    const backups = path.join(instanceDir, 'zyon-backups'); await fs.mkdir(backups, { recursive: true });
    await fs.rename(old, path.join(backups, previous + '.' + Date.now() + '.bak'));
  }
}
function parseForge(data) {
  if (digest(data) !== FORGE_SHA256) throw new Error('Instalador Forge inválido.');
  const zip = new AdmZip(data);
  const profile = JSON.parse(zip.readAsText('install_profile.json'));
  if (profile.install.minecraft !== '1.8.9' || profile.versionInfo.inheritsFrom !== '1.8.9') throw new Error('Versão Forge inesperada.');
  const jar = zip.readFile(profile.install.filePath);
  if (!jar || jar.length < 1000) throw new Error('Biblioteca Forge ausente.');
  const version = { ...profile.versionInfo, id: VERSION };
  version.libraries = version.libraries.map(lib => ({ ...lib, ...(lib.url ? { url: lib.url.replace(/^http:/, 'https:') } : {}) }));
  return { version, jar, relativeJar: coordinatePath(profile.install.path) };
}
async function updateProfiles(root, instanceDir, javaExe, memoryGB) {
  const files = [];
  for (const name of ['launcher_profiles.json', 'launcher_profiles_microsoft_store.json']) {
    const file = path.join(root, name); if (await exists(file)) files.push(file);
  }
  if (!files.length) files.push(path.join(root, 'launcher_profiles.json'));
  const id = 'zyon-' + digest(path.resolve(instanceDir)).slice(0, 12);
  const gb = Number(memoryGB);
  if (!Number.isInteger(gb) || gb < 1 || gb > 16) throw new Error('Memória inválida.');
  // Parse every profile file before writing any of them. Other profiles and account fields are preserved.
  const changes = [];
  for (const file of files) {
    const json = await readJson(file, { profiles: {}, version: 3 });
    if (!json || typeof json !== 'object' || Array.isArray(json) || (json.profiles && (typeof json.profiles !== 'object' || Array.isArray(json.profiles)))) throw new Error('O arquivo de perfis do Minecraft é inválido. Ele foi preservado.');
    json.profiles ||= {};
    const previous = json.profiles[id] || {};
    json.profiles[id] = {
      ...previous, name: 'Zyon 1.8.9', type: 'custom', created: previous.created || new Date().toISOString(),
      lastVersionId: VERSION, gameDir: instanceDir, javaDir: javaExe,
      javaArgs: `-Xmx${gb}G -XX:+UseG1GC -XX:+UnlockExperimentalVMOptions -XX:G1NewSizePercent=20 -XX:G1ReservePercent=20 -XX:MaxGCPauseMillis=50 -XX:G1HeapRegionSize=32M`
    };
    changes.push([file, JSON.stringify(json, null, 2)]);
  }
  for (const [file, json] of changes) { await backup(file); await atomicWrite(file, json); }
  return id;
}
async function ensureJava(root, options = {}) {
  const stateFile = path.join(root, 'java.json');
  const current = await readJson(stateFile, {});
  if (typeof current.executable === 'string' && inside(path.join(root, 'runtime'), current.executable) && path.basename(current.executable).toLowerCase() === 'javaw.exe' && await exists(current.executable)) return current.executable;
  const metadataUrl = 'https://api.adoptium.net/v3/assets/latest/8/hotspot?architecture=x64&image_type=jre&os=windows';
  const metadata = JSON.parse((await fetchBuffer(metadataUrl, { ...options, maxBytes: 2 * 1024 * 1024 })).toString('utf8'));
  const pkg = metadata[0]?.binary?.package;
  if (!pkg || !/^[a-f0-9]{64}$/i.test(pkg.checksum) || !pkg.name.endsWith('.zip')) throw new Error('Metadados do Java inválidos.');
  const source = new URL(pkg.link);
  if (source.protocol !== 'https:' || source.hostname !== 'github.com' || !source.pathname.startsWith('/adoptium/temurin8-binaries/releases/download/')) throw new Error('Origem Java inesperada.');
  const zipFile = path.join(root, 'downloads', `java-${pkg.checksum}.zip`);
  await downloadVerified(pkg.link, zipFile, pkg.checksum, 'sha256', options);
  const folder = path.join(root, 'runtime', 'java8-' + crypto.randomUUID());
  await fs.mkdir(folder, { recursive: true }); let expanded = 0;
  try {
    await extractZip(zipFile, {
      dir: folder,
      onEntry(entry) {
        checkAbort(options.signal); expanded += entry.uncompressedSize;
        if (expanded > 600 * 1024 * 1024 || ((entry.externalFileAttributes >>> 16) & 0xf000) === 0xa000) throw new Error('Arquivo Java contém uma entrada inválida.');
      }
    });
    const children = await fs.readdir(folder, { withFileTypes: true });
    const candidates = [path.join(folder, 'bin', 'javaw.exe'), ...children.filter(e => e.isDirectory()).flatMap(e => [path.join(folder, e.name, 'bin', 'javaw.exe'), path.join(folder, e.name, 'jre', 'bin', 'javaw.exe')])];
    let executable;
    for (const file of candidates) if (await exists(file)) { executable = file; break; }
    if (!executable) throw new Error('javaw.exe não encontrado no Java baixado.');
    checkAbort(options.signal); await atomicWrite(stateFile, JSON.stringify({ executable, checksum: pkg.checksum }));
    return executable;
  } catch (error) { await fs.rm(folder, { recursive: true, force: true }); throw error; }
}
async function prepare({ root, instanceDir, minecraftRoot, payload, memoryGB = 2, signal, emit = () => {}, beforeProfiles = async () => {}, registerProfiles = true }) {
  if (inside(minecraftRoot, instanceDir) || inside(instanceDir, minecraftRoot)) throw new Error('A instância e a pasta base devem ficar separadas.');
  const stage = (step, percent, message) => { checkAbort(signal); emit({ step, percent, message }); };
  const options = { signal };
  await fs.mkdir(instanceDir, { recursive: true }); await fs.mkdir(minecraftRoot, { recursive: true });
  stage('minecraft', 5, 'Buscando Minecraft 1.8.9 nos servidores da Mojang…');
  const manifest = JSON.parse((await fetchBuffer('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json', { ...options, maxBytes: 5 * 1024 * 1024 })).toString());
  const entry = manifest.versions.find(v => v.id === '1.8.9');
  if (!entry?.url || !/^[a-f0-9]{40}$/i.test(entry.sha1)) throw new Error('Minecraft 1.8.9 não encontrado.');
  const metadata = await fetchBuffer(entry.url, { ...options, hash: entry.sha1, algorithm: 'sha1', maxBytes: 5 * 1024 * 1024 });
  const vanilla = JSON.parse(metadata.toString());
  if (vanilla.id !== '1.8.9' || !vanilla.downloads?.client?.sha1) throw new Error('Metadados Minecraft inválidos.');
  const vanillaDir = path.join(minecraftRoot, 'versions', '1.8.9');
  const metadataFile = path.join(vanillaDir, '1.8.9.json');
  let keepMetadata = false;
  if (await exists(metadataFile)) {
    const previous = await readJson(metadataFile);
    if (previous.id !== '1.8.9' || previous.downloads?.client?.sha1 !== vanilla.downloads.client.sha1) throw new Error('Existe um manifesto 1.8.9 diferente. Ele foi preservado; confira essa versão no launcher oficial.');
    keepMetadata = true;
  }
  await downloadVerified(vanilla.downloads.client.url, path.join(vanillaDir, '1.8.9.jar'), vanilla.downloads.client.sha1, 'sha1', options);
  if (!keepMetadata) await atomicWrite(metadataFile, metadata);
  stage('forge', 30, 'Preparando Forge 11.15.1.2318…');
  const forgeFile = path.join(root, 'downloads', 'forge-installer.jar');
  await downloadVerified(FORGE_URL, forgeFile, FORGE_SHA256, 'sha256', options);
  const forge = parseForge(await fs.readFile(forgeFile));
  const forgeLibrary = path.join(minecraftRoot, 'libraries', ...forge.relativeJar.split('/'));
  if (await exists(forgeLibrary) && digest(await fs.readFile(forgeLibrary)) !== digest(forge.jar)) throw new Error('Já existe uma biblioteca Forge diferente. Ela foi preservada.');
  await atomicWrite(forgeLibrary, forge.jar);
  // The official launcher resolves inherited libraries, assets, and native components when Play is pressed.
  await atomicWrite(path.join(minecraftRoot, 'versions', VERSION, VERSION + '.json'), JSON.stringify(forge.version, null, 2));
  stage('java', 50, 'Baixando e verificando o Java 8 para esta instalação…');
  let lastProgress = 0;
  const javaExe = await ensureJava(root, { ...options, report(received, total) {
    if (total < 1024 * 1024 || Date.now() - lastProgress < 700) return;
    lastProgress = Date.now();
    emit({ step: 'java', percent: 50 + Math.min(27, Math.floor(27 * received / total)), message: `Baixando Java 8: ${(received / 1024 / 1024).toFixed(1)} de ${(total / 1024 / 1024).toFixed(1)} MB…` });
  } });
  stage('client', 82, 'Instalando os 19 módulos e criando o perfil separado…');
  await installMod(payload, instanceDir);
  checkAbort(signal);
  if (registerProfiles) { await beforeProfiles(); checkAbort(signal); await updateProfiles(minecraftRoot, instanceDir, javaExe, memoryGB); }
  await atomicWrite(path.join(root, 'installation.json'), JSON.stringify({ version: VERSION, instanceDir, minecraftRoot, javaExe, memoryGB, prepared: new Date().toISOString() }, null, 2));
  stage('done', 100, 'Instalação pronta. No Minecraft Launcher, selecione Zyon 1.8.9 e clique em Jogar.');
}
function launchExecutable(file) {
  return new Promise((resolve, reject) => {
    const child = spawn(file, [], { detached: true, stdio: 'ignore', cwd: path.dirname(file), shell: false, windowsHide: false });
    child.once('error', reject); child.once('spawn', () => { child.unref(); resolve(); });
  });
}
module.exports = { MOD, VERSION, FORGE_SHA256, FORGE_URL, digest, exists, inside, atomicWrite, readJson, coordinatePath, fetchBuffer, downloadVerified, installMod, parseForge, updateProfiles, ensureJava, prepare, launchExecutable };
