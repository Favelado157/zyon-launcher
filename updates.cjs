'use strict';
const fs = require('node:fs/promises');
const path = require('node:path');
const crypto = require('node:crypto');
const core = require('./core.cjs');
const VERSION_RE = /^(0|[1-9]\d*)\.(0|[1-9]\d*)\.(0|[1-9]\d*)$/;
function normalizeRepository(value) {
  let text = String(value || '').trim().replace(/\/$/, '').replace(/^https:\/\/github\.com\//i, '').replace(/\.git$/, '');
  if (!text) return '';
  if (!/^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})\/[A-Za-z0-9_.-]{1,100}$/.test(text) || text.split('/').some(p => p === '.' || p === '..')) throw new Error('Informe o repositório no formato usuario/repositorio.');
  return text;
}
function newer(candidate, current) {
  if (!VERSION_RE.test(candidate) || !VERSION_RE.test(current)) throw new Error('Número de versão inválido.');
  const left = candidate.split('.').map(Number), right = current.split('.').map(Number);
  for (let i = 0; i < 3; i++) { if (left[i] !== right[i]) return left[i] > right[i]; } return false;
}
function trustedAsset(url, repository) {
  const parsed = new URL(url);
  return parsed.protocol === 'https:' && parsed.hostname === 'github.com' && parsed.pathname.toLowerCase().startsWith(('/' + repository + '/releases/download/').toLowerCase()) && !parsed.username && !parsed.password;
}
async function check({ repository, currentVersion, request = fetch, signal }) {
  const repo = normalizeRepository(repository);
  if (!repo) return { configured: false, available: false, message: 'O publicador ainda não configurou o canal de atualizações.' };
  const response = await request(`https://api.github.com/repos/${repo}/releases/latest`, {
    headers: { Accept: 'application/vnd.github+json', 'User-Agent': 'Zyon-Launcher/' + currentVersion },
    signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(20000)]) : AbortSignal.timeout(20000)
  });
  if (response.status === 404) return { configured: true, available: false, message: 'Ainda não há uma versão pública disponível neste canal.' };
  if (!response.ok) throw new Error('Não foi possível verificar as atualizações agora. Tente novamente mais tarde.');
  const text = await response.text(); if (text.length > 2 * 1024 * 1024) throw new Error('Resposta de atualização inválida.');
  const release = JSON.parse(text); const version = String(release.tag_name || '').replace(/^v/, '');
  if (!VERSION_RE.test(version) || release.draft || release.prerelease || !newer(version, currentVersion)) return { configured: true, available: false, message: 'Você já está na versão mais recente.' };
  const name = `Zyon-Launcher-Setup-${version}.exe`;
  const asset = release.assets?.find(file => file.name === name);
  if (!asset || !trustedAsset(asset.browser_download_url, repo) || !Number.isSafeInteger(asset.size) || asset.size < 10000 || asset.size > 250 * 1024 * 1024) throw new Error('A publicação não contém um instalador Windows válido.');
  let sha256;
  if (/^sha256:[a-f0-9]{64}$/i.test(asset.digest || '')) sha256 = asset.digest.slice(7).toLowerCase();
  else {
    const checksumAsset = release.assets.find(file => file.name === 'SHA256SUMS.txt');
    if (!checksumAsset || !trustedAsset(checksumAsset.browser_download_url, repo)) throw new Error('A publicação não contém a verificação do instalador.');
    const data = await core.fetchBuffer(checksumAsset.browser_download_url, { request, signal, maxBytes: 64 * 1024 });
    const match = data.toString('utf8').split(/\r?\n/).map(line => line.match(/^([a-f0-9]{64})\s+\*?(.+)$/i)).find(parts => parts && parts[2] === name);
    if (!match) throw new Error('A verificação do instalador não foi encontrada na publicação.'); sha256 = match[1].toLowerCase();
  }
  return { configured: true, available: true, version, name, size: asset.size, sha256, url: asset.browser_download_url, repository: repo,
    notes: String(release.body || '').slice(0, 5000), publishedAt: release.published_at || null, message: 'Zyon ' + version + ' está disponível.' };
}
async function download(update, { root, signal, report = () => {}, request = fetch }) {
  if (!update?.available || !VERSION_RE.test(update.version) || !/^[a-f0-9]{64}$/.test(update.sha256) || !trustedAsset(update.url, normalizeRepository(update.repository)) || update.name !== `Zyon-Launcher-Setup-${update.version}.exe`) throw new Error('Atualização inválida.');
  const data = await core.fetchBuffer(update.url, { hash: update.sha256, maxBytes: 250 * 1024 * 1024, signal, request, report });
  if (data.length !== update.size || data.readUInt16LE(0) !== 0x5a4d) throw new Error('O arquivo recebido não corresponde ao instalador publicado.');
  const file = path.join(root, 'updates', update.version + '-' + crypto.randomUUID() + '.exe');
  await core.atomicWrite(file, data); return file;
}
module.exports = { normalizeRepository, newer, trustedAsset, check, download };
