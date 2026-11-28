'use strict';
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),Zip=require('adm-zip');
const root=path.join(__dirname,'..'),pkg=require('../package.json'),{CLIENT}=require('../core.cjs'),jar=path.join(root,'payload',CLIENT);
if(!fs.existsSync(jar))throw Error('Execute npm run build:client antes de publicar.');
const hash=f=>crypto.createHash('sha256').update(fs.readFileSync(f)).digest('hex'),zip=new Zip(jar);
if(zip.getEntries().some(e=>/^(net\/minecraft(?:forge)?\/|cpw\/mods\/)/.test(e.entryName)))throw Error('O SDK nao pode distribuir Minecraft ou Forge.');
for(const c of ['platform/Main','platform/Hooks','bootstrap/PatchMain','Engine','CapeSupport','ArmorCit','ArmorCitRule'])if(!zip.getEntry('dev/zyon/'+c+'.class'))throw Error('Classe ausente: '+c);
fs.writeFileSync(path.join(root,'payload/manifest.json'),JSON.stringify({file:CLIENT,sha256:hash(jar),modules:36,minecraft:'1.8.9',engine:'zyon-vanilla-1',entrypoint:'dev.zyon.platform.Main',patcher:'dev.zyon.bootstrap.PatchMain'},null,2)+'\n');
if(process.argv.includes('--checksums')){const installer=path.join(root,'dist',`Zyon-Launcher-Setup-${pkg.version}.exe`);if(!fs.existsSync(installer))throw Error('Instalador ausente.');fs.writeFileSync(path.join(root,'dist/SHA256SUMS.txt'),hash(installer)+'  '+path.basename(installer)+'\n'+hash(jar)+'  '+CLIENT+'\n');}
