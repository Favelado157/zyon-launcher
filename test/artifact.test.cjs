'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),AdmZip=require('adm-zip');
test('distributed client is visual 1.5.1 and contains no retired gameplay engine',()=>{
 const payload=path.join(__dirname,'..','payload'),manifest=JSON.parse(fs.readFileSync(path.join(payload,'manifest.json'))),bytes=fs.readFileSync(path.join(payload,manifest.file)),zip=new AdmZip(bytes);
 assert.equal(manifest.modules,20);assert.equal(manifest.file,'zyon-1.8.9-1.5.1.jar');assert.equal(crypto.createHash('sha256').update(bytes).digest('hex'),manifest.sha256);
 assert.equal(zip.getEntry('dev/zyon/Engine.class'),null);assert.ok(zip.getEntry('dev/zyon/Visuals.class'));assert.equal(JSON.parse(zip.readAsText('mcmod.info'))[0].version,'1.5.1');
 for(const entry of zip.getEntries().filter(e=>e.entryName.startsWith('dev/zyon/')&&e.entryName.endsWith('.class'))){assert.doesNotMatch(entry.getData().toString('latin1'),/killaura|triggerbot|aimassist|autoclicker|autoeat|autotool|fastplace|safewalk|tracers/i,entry.entryName);}
});
