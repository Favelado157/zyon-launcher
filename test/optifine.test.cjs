'use strict';
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const os = require('node:os');
const path = require('node:path');
const AdmZip = require('adm-zip');
const optifine = require('../optifine.cjs');
// Small synthetic archives exercise validation; no proprietary OptiFine code is distributed.
function fixture(version = '1.8.9', release = 'M5') {
  const strings = ['OptiFine', version, 'HD_U', release];
  const header = Buffer.alloc(10);header.writeUInt32BE(0xcafebabe);header.writeUInt16BE(52,6);header.writeUInt16BE(strings.length+1,8);
  const constants = strings.map(value => {const data=Buffer.from(value);const prefix=Buffer.alloc(3);prefix[0]=1;prefix.writeUInt16BE(data.length,1);return Buffer.concat([prefix,data]);});
  const zip = new AdmZip();
  zip.addFile('Config.class',Buffer.concat([header,...constants]));
  zip.addFile('optifine/OptiFineForgeTweaker.class',Buffer.from('fixture'));
  zip.addFile('optifine/OptiFineClassTransformer.class',Buffer.from('fixture'));
  return zip.toBuffer();
}
async function setup(t){const root=await fs.mkdtemp(path.join(os.tmpdir(),'zyon-of-'));t.after(()=>fs.rm(root,{recursive:true,force:true}));return root;}
test('OptiFine import checks the embedded Minecraft version instead of trusting a filename',()=>{
  assert.equal(optifine.inspect(fixture()).version,'1.8.9 HD U M5');
  assert.throws(()=>optifine.inspect(fixture('1.12.2')),/1.8.9/);
  assert.throws(()=>optifine.inspect(fixture('1.8.9','L5')),/M5/);
  assert.throws(()=>optifine.inspect(Buffer.from('not a jar')),/JAR/);
  const zip=new AdmZip(fixture());zip.deleteFile('optifine/OptiFineForgeTweaker.class');assert.throws(()=>optifine.inspect(zip.toBuffer()),/Forge/);
});
test('truncated or oversized version class is rejected before extraction',()=>{
  const zip=new AdmZip(fixture());zip.addFile('Config.class',Buffer.from([0xca,0xfe,0xba,0xbe]));assert.throws(()=>optifine.inspect(zip.toBuffer()),/Classe/);
  zip.addFile('Config.class',Buffer.alloc(1024*1024+1));assert.throws(()=>optifine.inspect(zip.toBuffer()),/versão/);
});
test('import installs into the selected instance and removal keeps a reversible backup',async t=>{
  const root=await setup(t), instance=path.join(root,'instance'), source=path.join(root,'download.jar'), data=fixture();await fs.writeFile(source,data);
  assert.equal((await optifine.status(instance)).installed,false);
  assert.equal((await optifine.install(source,instance)).managed,true);
  assert.deepEqual(await fs.readFile(source),data);
  assert.equal((await optifine.install(source,instance)).installed,true);
  await optifine.remove(instance);
  assert.equal((await optifine.status(instance)).installed,false);
  const backups=await fs.readdir(path.join(instance,'zyon-backups'));assert.equal(backups.length,1);assert.deepEqual(await fs.readFile(path.join(instance,'zyon-backups',backups[0])),data);
});
test('existing mod and conflicting OptiFine are preserved without creating duplicates',async t=>{
  const root=await setup(t), mods=path.join(root,'instance','mods'),source=path.join(root,'download.jar');await fs.mkdir(path.join(mods,'1.8.9'),{recursive:true});await fs.writeFile(source,fixture());
  const other=new AdmZip(fixture());other.addFile('other.txt',Buffer.from('different'));const previous=other.toBuffer();await fs.writeFile(path.join(mods,'1.8.9','renamed.jar'),previous);await fs.writeFile(path.join(mods,'unrelated.jar'),Buffer.from('untouched'));
  await assert.rejects(optifine.install(source,path.join(root,'instance')),/outro OptiFine/);
  assert.deepEqual(await fs.readFile(path.join(mods,'1.8.9','renamed.jar')),previous);assert.equal(await fs.readFile(path.join(mods,'unrelated.jar'),'utf8'),'untouched');
});
test('a manually installed or modified JAR cannot be removed by the launcher',async t=>{
  const root=await setup(t),instance=path.join(root,'instance'),mods=path.join(instance,'mods');await fs.mkdir(mods,{recursive:true});await fs.writeFile(path.join(mods,optifine.FILE),fixture());
  assert.equal((await optifine.status(instance)).managed,false);await assert.rejects(optifine.remove(instance),/só remove/);
  const source=path.join(root,'source.jar');await fs.writeFile(source,fixture());await fs.rm(path.join(mods,optifine.FILE));await optifine.install(source,instance);
  const modified=new AdmZip(fixture());modified.addFile('changed.txt',Buffer.from('edit'));await fs.writeFile(path.join(mods,optifine.FILE),modified.toBuffer());
  await assert.rejects(optifine.remove(instance),/só remove/);assert.equal((await fs.stat(path.join(mods,optifine.FILE))).isFile(),true);
});
test('symbolic mod folders cannot redirect installation outside the instance',async t=>{
  const root=await setup(t),instance=path.join(root,'instance'),outside=path.join(root,'outside'),source=path.join(root,'source.jar');await fs.mkdir(instance);await fs.mkdir(outside);await fs.symlink(outside,path.join(instance,'mods'),process.platform==='win32'?'junction':'dir');await fs.writeFile(source,fixture());
  await assert.rejects(optifine.install(source,instance),/simbólico/);assert.deepEqual(await fs.readdir(outside),[]);
});
