'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),edition=require('../edition.cjs');
test('legacy configuration cannot select offline auth, borrowed client IDs or an old mod folder',()=>{const cfg=edition.migrate({loginMode:'offline',instanceDir:path.resolve('/legacy'),microsoftClientId:'borrowed'},path.resolve('/data'),{microsoftClientId:'own',githubRepository:'owner/repo'});assert.equal(cfg.loginMode,'microsoft');assert.equal(cfg.microsoftClientId,'own');assert.equal(cfg.instanceDir,path.join(path.resolve('/data'),'instances','zyon-visual-1.8.9'));assert.equal(cfg.previousInstanceDir,path.resolve('/legacy'));});
test('profile migration copies only visual fields and never overwrites a new profile',async t=>{const root=await fs.mkdtemp(path.join(os.tmpdir(),'visual-profile-'));t.after(()=>fs.rm(root,{recursive:true,force:true}));const old=path.join(root,'old'),next=path.join(root,'next');await fs.mkdir(path.join(old,'config','zyon'),{recursive:true});await fs.writeFile(path.join(old,'config','zyon','auto.json'),JSON.stringify({schema:1,modules:{fly:{enabled:true},hud:{enabled:true,settings:{target:1,x:15}},theme:{settings:{accent:2}}}}));await edition.migrateProfiles(old,next);const file=path.join(next,'config','zyon','auto.json'),m=JSON.parse(await fs.readFile(file));assert.equal(m.modules.fly,undefined);assert.equal(m.modules.hud.settings.target,undefined);assert.equal(m.modules.hud.settings.x,15);assert.equal(m.modules.theme.settings.accent,2);await fs.writeFile(file,'user-preferences');await edition.migrateProfiles(old,next);assert.equal(await fs.readFile(file,'utf8'),'user-preferences');});

for (const version of ['1.3.0','1.3.1']) test(`known official client ${version} upgrades preserve worlds and back up the previous jar`,async t=>{
 const core=require('../core.cjs'),root=await fs.mkdtemp(path.join(os.tmpdir(),'zyon-nameplates-upgrade-'));
 t.after(()=>fs.rm(root,{recursive:true,force:true}));const mods=path.join(root,'mods'),payload=path.resolve(__dirname,'../payload');
 const old=await fs.readFile(path.join(payload,`zyon-1.8.9-${version}.jar`));
 await fs.mkdir(mods);await fs.writeFile(path.join(mods,`zyon-1.8.9-${version}.jar`),old);
 await fs.mkdir(path.join(root,'saves'));await fs.writeFile(path.join(root,'saves','world-marker'),'keep-world');
 await edition.validateInstance(root,payload);await core.installMod(payload,root);await edition.validateInstance(root,payload);
 assert.deepEqual(await fs.readdir(mods),[core.MOD]);assert.equal(await fs.readFile(path.join(root,'saves','world-marker'),'utf8'),'keep-world');
 const backups=await fs.readdir(path.join(root,'zyon-backups'));assert.equal(backups.length,1);assert.deepEqual(await fs.readFile(path.join(root,'zyon-backups',backups[0])),old);
});
test('unknown or altered previous clients are preserved and rejected before upgrading',async t=>{
 const root=await fs.mkdtemp(path.join(os.tmpdir(),'zyon-nameplates-altered-'));t.after(()=>fs.rm(root,{recursive:true,force:true}));
 await fs.mkdir(path.join(root,'mods'));const file=path.join(root,'mods','zyon-1.8.9-1.3.0.jar');await fs.writeFile(file,'unreviewed');
 await assert.rejects(edition.validateInstance(root,path.resolve(__dirname,'../payload')),/conteúdo diferente/);assert.equal(await fs.readFile(file,'utf8'),'unreviewed');
});
