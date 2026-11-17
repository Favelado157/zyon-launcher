'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const root = path.join(__dirname, '..');
const pkg = require('../package.json');
const manifestFile = path.join(root, 'payload', 'manifest.json');
const jar = path.join(root, 'payload', 'zyon-1.8.9-1.2.1.jar');
if (!fs.existsSync(jar)) throw new Error('Compile e copie o JAR Java 1.2.1 antes de publicar.');
const hash = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
fs.writeFileSync(manifestFile, JSON.stringify({ file: path.basename(jar), sha256: hash(jar), modules: 19, minecraft: '1.8.9', previousFiles: ['zyon-1.8.9-1.0.0.jar', 'zyon-1.8.9-1.1.0.jar', 'zyon-1.8.9-1.2.0.jar'] }, null, 2) + '\n');
if (process.argv.includes('--checksums')) {
  const installer = path.join(root, 'dist', `Zyon-Launcher-Setup-${pkg.version}.exe`);
  if (!fs.existsSync(installer)) throw new Error('Instalador não encontrado.');
  fs.writeFileSync(path.join(root, 'dist', 'SHA256SUMS.txt'), hash(installer) + '  ' + path.basename(installer) + '\n');
}
