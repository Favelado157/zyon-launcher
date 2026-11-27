'use strict';
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const { spawn } = require('node:child_process');
const AdmZip = require('adm-zip');
const extractZip = require('extract-zip');

const VERSION = 'zyon-client-1.8.9';
const CLIENT = 'zyon-client-1.8.9-2.0.0.jar';
const METADATA_SHA256 = '341af5f47a6b5bc137bfe681f4f3533e081041290aabbf9d7ebeae9881de768f';
const CLIENT_SHA1 = '3870888a6c3d349d3771a3e9d16c9bf5e076b908';
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
function clientFile(instanceDir) { return path.join(instanceDir, 'client', CLIENT); }
async function installClient(payload, instanceDir) {
  const manifest = await readJson(path.join(payload, 'manifest.json'));
  if (manifest.file !== CLIENT || manifest.engine !== 'zyon-vanilla-1' || !/^[a-f0-9]{64}$/.test(manifest.sha256)) throw new Error('Manifesto do Zyon Client inválido.');
  const bytes = await fs.readFile(path.join(payload, CLIENT));
  if (digest(bytes) !== manifest.sha256 || bytes.readUInt32LE(0) !== 0x04034b50) throw new Error('O cliente incluído falhou na verificação.');
  const folder = path.join(instanceDir, 'client');
  if (await exists(folder) && (await fs.lstat(folder)).isSymbolicLink()) throw new Error('A pasta do cliente não pode ser um link.');
  const file = clientFile(instanceDir);
  if (await exists(file)) {
    if ((await fs.lstat(file)).isSymbolicLink()) throw new Error('O arquivo do cliente não pode ser um link.');
    if (digest(await fs.readFile(file)) === manifest.sha256) return;
    await backup(file);
  }
  await atomicWrite(file, bytes);
}
async function ensureJava(root, options = {}) {
  const stateFile = path.join(root, 'java.json');
  const current = await readJson(stateFile, {});
  if (typeof current.executable === 'string' && [path.join(root, 'runtime'), path.join(path.dirname(root), 'ZyonTesteLocal', 'runtime'), path.join(path.dirname(root), 'ZyonLauncher', 'runtime')].some(folder => inside(folder, current.executable)) && path.basename(current.executable).toLowerCase() === 'javaw.exe' && await exists(current.executable)) return current.executable;
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
async function prepare({ root, instanceDir, minecraftRoot, payload, memoryGB = 2, signal, emit = () => {} }) {
  if (inside(minecraftRoot, instanceDir) || inside(instanceDir, minecraftRoot)) throw new Error('A instância e o cache devem ficar separados.');
  await fs.mkdir(instanceDir, { recursive: true }); await fs.mkdir(minecraftRoot, { recursive: true });
  const options = { signal }, metadataFile = path.join(minecraftRoot, 'versions', '1.8.9', '1.8.9.json');
  emit({ step: 'minecraft', percent: 5, message: 'Conferindo os arquivos oficiais do Minecraft 1.8.9…' });
  let bytes = await fs.readFile(metadataFile).catch(() => null);
  if (!bytes || digest(bytes) !== METADATA_SHA256) {
    const manifest = JSON.parse((await fetchBuffer('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json', { ...options, maxBytes: 5 * 1024 * 1024 })).toString());
    const entry = manifest.versions.find(v => v.id === '1.8.9');
    if (!entry || !/^[a-f0-9]{40}$/i.test(entry.sha1)) throw new Error('Metadados Minecraft indisponíveis.');
    bytes = await fetchBuffer(entry.url, { ...options, hash: entry.sha1, algorithm: 'sha1', maxBytes: 5 * 1024 * 1024 });
    await atomicWrite(metadataFile, bytes);
  }
  const vanilla = JSON.parse(bytes.toString());
  if (vanilla.id !== '1.8.9' || vanilla.downloads?.client?.sha1 !== CLIENT_SHA1) throw new Error('Versão Minecraft inesperada.');
  await downloadVerified(vanilla.downloads.client.url, path.join(minecraftRoot, 'versions', '1.8.9', '1.8.9.jar'), CLIENT_SHA1, 'sha1', options);
  emit({ step: 'engine', percent: 30, message: 'Preparando a base própria do Zyon Client…' });
  await installClient(payload, instanceDir);
  emit({ step: 'java', percent: 40, message: 'Conferindo o Java 8…' });
  const javaExe = await ensureJava(root, options);
  checkAbort(signal);
  await atomicWrite(path.join(root, 'installation.json'), JSON.stringify({ engine: 'zyon-vanilla-1', instanceDir, minecraftRoot, javaExe, memoryGB }));
}
function launchExecutable(file) {
  return new Promise((resolve, reject) => {
    const child = spawn(file, [], { detached: true, stdio: 'ignore', cwd: path.dirname(file), shell: false, windowsHide: false });
    child.once('error', reject); child.once('spawn', () => { child.unref(); resolve(); });
  });
}
module.exports = { CLIENT, VERSION, METADATA_SHA256, CLIENT_SHA1, clientFile, digest, exists, inside, atomicWrite, readJson, coordinatePath, fetchBuffer, downloadVerified, installClient, ensureJava, prepare, launchExecutable };
