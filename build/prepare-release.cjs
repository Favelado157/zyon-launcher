'use strict';
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const root = path.join(__dirname, '..');
const pkg = require('../package.json');
const manifestFile = path.join(root, 'payload', 'manifest.json');
const jar = path.join(root, 'payload', 'zyon-1.8.9-1.3.1.jar');
if (!fs.existsSync(jar)) throw new Error('Compile e copie o JAR Java 1.3.1 antes de publicar.');
const hash = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
fs.writeFileSync(manifestFile, JSON.stringify({ file: path.basename(jar), sha256: hash(jar), modules: 7, minecraft: '1.8.9', previousVisuals: {'zyon-1.8.9-1.3.0.jar': ["85f3fabe87fc717b2f6106b6f36955f791d70035754089a4ec177f05aa293a97", "da30b170857bcac11837830cc7a6c03be5e365cdfa68ca7da1f2448f69ebadc8", "fcc1343c1b9933c260c70fca937d00f6af4f0718c487d8aef361f91c7ed18738"]}, previousFiles: ['zyon-1.8.9-1.3.0.jar', 'zyon-1.8.9-1.0.0.jar', 'zyon-1.8.9-1.1.0.jar', 'zyon-1.8.9-1.2.0.jar', 'zyon-1.8.9-1.2.1.jar', 'zyon-1.8.9-1.2.2.jar'] }, null, 2) + '\n');
if (process.argv.includes('--checksums')) {
  const installer = path.join(root, 'dist', `Zyon-Launcher-Setup-${pkg.version}.exe`);
  if (!fs.existsSync(installer)) throw new Error('Instalador não encontrado.');
  fs.writeFileSync(path.join(root, 'dist', 'SHA256SUMS.txt'), hash(installer) + '  ' + path.basename(installer) + '\n' + hash(jar) + '  ' + path.basename(jar) + '\n');
}
