'use strict';
const fs = require('node:fs/promises');
const path = require('node:path');
const { spawn } = require('node:child_process');
const AdmZip = require('adm-zip');
const core = require('./core.cjs');
const GAME_VERSION = '1.8.9';
function canceled(signal) { if (signal?.aborted) throw new Error('Preparação cancelada.'); }
function safePath(root, relative) {
  if (typeof relative !== 'string' || !relative || relative.includes('\\') || relative.split('/').some(p => !p || p === '.' || p === '..') || /^[A-Za-z]:/.test(relative) || relative.startsWith('/')) throw new Error('Caminho de recurso inválido.');
  const file = path.join(root, ...relative.split('/'));
  if (!core.inside(root, file)) throw new Error('Recurso fora da pasta de instalação.'); return file;
}
function allowed(library, platform = { name: 'windows', arch: 'x86_64', version: '10.0' }) {
  if (library.clientreq === false) return false;
  if (!library.rules?.length) return true;
  let result = false;
  for (const rule of library.rules) {
    const os = rule.os;
    if (os?.name && os.name !== platform.name) continue;
    if (os?.arch && !new RegExp('^(?:' + os.arch + ')$').test(platform.arch)) continue;
    if (os?.version && !new RegExp(os.version).test(platform.version)) continue;
    if (rule.features && Object.values(rule.features).some(value => value === true)) continue;
    result = rule.action === 'allow';
  }
  return result;
}
function mergeLibraries(vanilla, forge) {
  const entries = new Map();
  for (const library of [...vanilla, ...forge]) {
    if (!allowed(library)) continue;
    const [group, artifact, , classifier = ''] = library.name.split(':');
    entries.set(group + ':' + artifact + ':' + classifier, library);
  }
  return [...entries.values()];
}
function libraryPlan(vanilla, forge, minecraftRoot) {
  const result = [], seen = new Set();
  for (const lib of mergeLibraries(vanilla.libraries || [], forge.version.libraries || [])) {
    if (!allowed(lib)) continue;
    const add = (artifact, classifier) => {
      const relative = artifact?.path || core.coordinatePath(lib.name + (classifier ? ':' + classifier : ''));
      const file = safePath(path.join(minecraftRoot, 'libraries'), relative);
      if (seen.has(file)) return; seen.add(file);
      let url = artifact?.url || (lib.url || 'https://libraries.minecraft.net/') + relative;
      url = url.replace(/^http:/, 'https:');
      let hashes = artifact?.sha1 ? [artifact.sha1.toLowerCase()] : (lib.checksums || []).filter(hash => /^[a-f0-9]{40}$/i.test(hash)).map(hash => hash.toLowerCase());
      if (relative === forge.relativeJar) hashes = [core.digest(forge.jar, 'sha1')];
      result.push({ file, relative, url, hashes, native: !!classifier, exclude: lib.extract?.exclude || ['META-INF/'] });
    };
    if (!lib.downloads || lib.downloads.artifact) add(lib.downloads?.artifact, null);
    if (lib.natives?.windows) {
      const classifier = lib.natives.windows.replace('${arch}', '64');
      const artifact = lib.downloads?.classifiers?.[classifier];
      if (!artifact && lib.downloads?.classifiers) throw new Error('Componente nativo Windows ausente: ' + lib.name);
      add(artifact, classifier);
    }
  }
  return result;
}
async function ensureLibrary(item, options) {
  let hashes = item.hashes;
  if (!hashes.length) {
    const text = (await core.fetchBuffer(item.url + '.sha1', { ...options, maxBytes: 1024 })).toString();
    const match = text.match(/\b[a-f0-9]{40}\b/i);
    if (!match) throw new Error('A biblioteca não possui uma verificação válida: ' + path.basename(item.file));
    hashes = [match[0].toLowerCase()];
  }
  if (await core.exists(item.file)) {
    if (hashes.includes(core.digest(await fs.readFile(item.file), 'sha1'))) return;
    throw new Error('Uma biblioteca existente tem conteúdo diferente e foi preservada: ' + path.basename(item.file));
  }
  const data = await core.fetchBuffer(item.url, { ...options, maxBytes: 64 * 1024 * 1024 });
  if (!hashes.includes(core.digest(data, 'sha1'))) throw new Error('A verificação da biblioteca falhou: ' + path.basename(item.file));
  canceled(options.signal); await core.atomicWrite(item.file, data);
}
async function pool(items, parallel, work, signal) {
  let index = 0, firstError;
  const workers = Array.from({ length: Math.min(parallel, items.length) }, async () => {
    while (index < items.length && !firstError) {
      canceled(signal); const item = items[index++];
      try { await work(item); } catch (error) { firstError = error; }
    }
  });
  await Promise.all(workers); if (firstError) throw firstError;
}
async function extractNatives(file, destination, exclude) {
  const zip = new AdmZip(await fs.readFile(file)); let total = 0;
  for (const entry of zip.getEntries()) {
    if (entry.isDirectory || exclude.some(prefix => entry.entryName.startsWith(prefix))) continue;
    if (((entry.attr >>> 16) & 0xf000) === 0xa000) throw new Error('Componente nativo contém um link simbólico.');
    const target = safePath(destination, entry.entryName); total += entry.header.size;
    if (total > 128 * 1024 * 1024) throw new Error('Componente nativo excede o limite.');
    await core.atomicWrite(target, entry.getData());
  }
}
async function ensureArtifacts({ root, instanceDir, minecraftRoot, signal, emit = () => {}, request = fetch }) {
  const options = { signal, request };
  const vanilla = await core.readJson(path.join(minecraftRoot, 'versions', GAME_VERSION, GAME_VERSION + '.json'));
  const forgeData = await fs.readFile(path.join(root, 'downloads', 'forge-installer.jar'));
  const forge = core.parseForge(forgeData);
  const embeddedForge = safePath(path.join(minecraftRoot, 'libraries'), forge.relativeJar);
  if (!await core.exists(embeddedForge)) await core.atomicWrite(embeddedForge, forge.jar);
  else if (core.digest(await fs.readFile(embeddedForge)) !== core.digest(forge.jar)) throw new Error('A biblioteca Forge existente é diferente e foi preservada.');
  const libraries = libraryPlan(vanilla, forge, minecraftRoot);
  let completed = 0;
  emit({ step: 'client', percent: 48, message: 'Verificando bibliotecas e componentes do jogo…' });
  await pool(libraries, 4, async library => {
    await ensureLibrary(library, options); completed++;
    emit({ step: 'client', percent: 48 + Math.floor(17 * completed / libraries.length), message: `Bibliotecas verificadas: ${completed}/${libraries.length}` });
  }, signal);
  const assetsRoot = path.join(minecraftRoot, 'assets');
  const index = vanilla.assetIndex;
  if (!index || !/^[a-f0-9]{40}$/i.test(index.sha1) || !/^[A-Za-z0-9_.-]+$/.test(index.id)) throw new Error('Índice de recursos Minecraft inválido.');
  const indexFile = path.join(assetsRoot, 'indexes', index.id + '.json');
  await core.downloadVerified(index.url, indexFile, index.sha1, 'sha1', options);
  const assetIndex = await core.readJson(indexFile);
  const objects = [...new Map(Object.values(assetIndex.objects).map(value => [value.hash, value])).values()];
  let assetsDone = 0, reported = 0;
  await pool(objects, 6, async object => {
    if (!/^[a-f0-9]{40}$/i.test(object.hash)) throw new Error('Recurso Minecraft inválido.');
    const relative = object.hash.slice(0, 2) + '/' + object.hash;
    await core.downloadVerified('https://resources.download.minecraft.net/' + relative, safePath(path.join(assetsRoot, 'objects'), relative), object.hash, 'sha1', { ...options, maxBytes: 64 * 1024 * 1024 });
    assetsDone++;
    if (Date.now() - reported > 500 || assetsDone === objects.length) {
      reported = Date.now(); emit({ step: 'client', percent: 65 + Math.floor(28 * assetsDone / objects.length), message: `Recursos do jogo: ${assetsDone}/${objects.length}` });
    }
  }, signal);
  for (const [name, object] of Object.entries(assetIndex.objects)) {
    if (!assetIndex.virtual && !assetIndex.map_to_resources) break;
    const data = await fs.readFile(path.join(assetsRoot, 'objects', object.hash.slice(0, 2), object.hash));
    if (assetIndex.virtual) await core.atomicWrite(safePath(path.join(assetsRoot, 'virtual', index.id), name), data);
    if (assetIndex.map_to_resources) await core.atomicWrite(safePath(path.join(instanceDir, 'resources'), name), data);
  }
  const natives = path.join(root, 'natives', '1.8.9-win-x64'); await fs.mkdir(natives, { recursive: true });
  for (const lib of libraries.filter(value => value.native)) { canceled(signal); await extractNatives(lib.file, natives, lib.exclude); }
  let logging = null;
  if (vanilla.logging?.client?.file) {
    const config = vanilla.logging.client.file;
    const file = safePath(path.join(minecraftRoot, 'assets', 'log_configs'), config.id);
    await core.downloadVerified(config.url, file, config.sha1, 'sha1', options);
    logging = { argument: vanilla.logging.client.argument, file };
  }
  const clientJar = path.join(minecraftRoot, 'versions', GAME_VERSION, GAME_VERSION + '.jar');
  const ready = {
    schema: 1, version: GAME_VERSION, clientVersion: '1.2.1', minecraftRoot, instanceDir, mainClass: forge.version.mainClass,
    arguments: forge.version.minecraftArguments || vanilla.minecraftArguments, classpath: libraries.filter(value => !value.native).map(value => value.file).concat(clientJar),
    natives, assetsRoot, assetsIndex: index.id, logging,
    resources: objects.map(value => path.join(assetsRoot, 'objects', value.hash.slice(0, 2), value.hash)), prepared: new Date().toISOString()
  };
  canceled(signal); await core.atomicWrite(path.join(root, 'game-ready.json'), JSON.stringify(ready, null, 2));
  emit({ step: 'done', percent: 100, message: 'Minecraft preparado para iniciar diretamente pelo Zyon.' });
  return ready;
}
async function readReady(root, instanceDir, minecraftRoot) {
  const ready = await core.readJson(path.join(root, 'game-ready.json'), null).catch(() => null);
  if (!ready || ready.schema !== 1 || ready.clientVersion !== '1.2.1' || ready.instanceDir !== instanceDir || ready.minecraftRoot !== minecraftRoot) return null;
  if (!core.inside(root, ready.natives) || !core.inside(minecraftRoot, ready.assetsRoot)) return null;
  const files = [...ready.classpath, ...(ready.resources || []), ...(ready.logging ? [ready.logging.file] : [])];
  if (files.some(file => !core.inside(minecraftRoot, file))) return null;
  let missing = false;
  await pool(files, 32, async file => { if (!await core.exists(file)) missing = true; });
  if (missing || !await core.exists(path.join(ready.natives, 'lwjgl64.dll'))) return null;
  return ready;
}
function splitArguments(text) {
  const result = []; const matches = text.matchAll(/"([^"\\]*(?:\\.[^"\\]*)*)"|'([^']*)'|(\S+)/g);
  for (const match of matches) result.push(match[1] ?? match[2] ?? match[3]); return result;
}
function launchArguments(ready, session, memoryGB) {
  if (!session?.accessToken || !/^[a-f0-9]{32}$/i.test(session.uuid || '') || !/^[A-Za-z0-9_]{1,16}$/.test(session.name || '')) throw new Error('Sessão Minecraft inválida. Entre na conta novamente.');
  if (!Number.isInteger(memoryGB) || memoryGB < 1 || memoryGB > 16) throw new Error('Memória inválida.');
  const variables = {
    auth_player_name: session.name, version_name: core.VERSION, game_directory: ready.instanceDir, assets_root: ready.assetsRoot,
    game_assets: path.join(ready.assetsRoot, 'virtual', ready.assetsIndex), assets_index_name: ready.assetsIndex,
    auth_uuid: session.uuid, auth_access_token: session.accessToken, auth_session: 'token:' + session.accessToken + ':' + session.uuid,
    user_properties: '{}', user_type: 'mojang', version_type: 'release'
  };
  const args = [`-Xmx${memoryGB}G`, '-XX:+UseG1GC', '-Djava.library.path=' + ready.natives, '-Dminecraft.launcher.brand=Zyon', '-Dminecraft.launcher.version=1.2.1'];
  if (ready.logging) args.push(ready.logging.argument.replace('${path}', ready.logging.file));
  args.push('-cp', ready.classpath.join(';'), ready.mainClass);
  for (const token of splitArguments(ready.arguments)) {
    args.push(token.replace(/\$\{([^}]+)\}/g, (_all, key) => {
      if (!(key in variables)) throw new Error('Argumento Minecraft não suportado: ' + key); return variables[key];
    }));
  }
  return args;
}
function redactGameLog(value, session) {
  let text = String(value);
  if (session?.accessToken) text = text.split(session.accessToken).join('[sessão omitida]');
  return text.replace(/(access[_-]?token[=: ]+)[^\s,;}]+/gi, '$1[omitido]');
}
function launch({ javaExe, ready, session, memoryGB, onOutput = () => {}, onExit = () => {}, spawnProcess = spawn }) {
  const args = launchArguments(ready, session, memoryGB);
  return new Promise((resolve, reject) => {
    const child = spawnProcess(javaExe, args, { cwd: ready.instanceDir, shell: false, windowsHide: true, stdio: ['ignore', 'pipe', 'pipe'] });
    for (const stream of [child.stdout, child.stderr]) {
      if (!stream) continue;
      // Keep an overlap so a session token split across stream chunks is never persisted.
      let pending = '';
      stream.setEncoding('utf8');
      stream.on('data', data => { pending += data; const lines = pending.split('\n'); pending = lines.pop(); for (const line of lines) onOutput(redactGameLog(line + '\n', session)); });
      stream.on('end', () => { if (pending) onOutput(redactGameLog(pending, session)); });
    }
    child.once('error', () => reject(new Error('O Java não pôde ser iniciado. Verifique a instalação e os registros.')));
    child.once('spawn', () => resolve(child));
    child.once('exit', (code, signal) => onExit({ code, signal }));
  });
}
module.exports = { safePath, allowed, mergeLibraries, libraryPlan, ensureLibrary, pool, extractNatives, ensureArtifacts, readReady, splitArguments, launchArguments, redactGameLog, launch };
