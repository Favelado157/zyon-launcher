'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs/promises'),path=require('node:path'),os=require('node:os'),vm=require('node:vm');
const core=require('../core.cjs'),game=require('../game.cjs');
test('desktop gates preparation and launch on ownership, migrates old profiles and rejects external mods',async t=>{
 const temporary=await fs.mkdtemp(path.join(os.tmpdir(),'zyon-visual-desktop-'));t.after(()=>fs.rm(temporary,{recursive:true,force:true}));
 const root=path.join(temporary,'ZyonLauncher'),old=path.join(root,'instances','1.8.9'),instance=path.join(root,'instances','zyon-visual-1.8.9');
 await core.atomicWrite(path.join(old,'saves','MyWorld','level.dat'),'world-preserved');
 await core.atomicWrite(path.join(old,'config','zyon','auto.json'),JSON.stringify({schema:1,modules:{killaura:{enabled:true},hud:{enabled:true,key:19,settings:{x:24,target:1}},keystrokes:{enabled:true,settings:{x:55}}}}));
 await core.atomicWrite(path.join(root,'config.json'),JSON.stringify({instanceDir:old,loginMode:'offline',offlineNickname:'OldNick',microsoftClientId:'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'}));
 const executable=path.join(root,'runtime','javaw.exe');await core.atomicWrite(executable,'fixture');await core.atomicWrite(path.join(root,'java.json'),JSON.stringify({executable}));
 const handlers=new Map(),calls={session:0,prepare:0,java:0};let window,loaded,prepared=false,exit;const onLoaded=new Promise(r=>loaded=r);
 const ready={classpath:['minecraft.jar'],natives:'natives',instanceDir:instance,assetsRoot:'assets',assetsIndex:'1.8',mainClass:'net.minecraft.launchwrapper.Launch',arguments:'--username ${auth_player_name} --uuid ${auth_uuid} --accessToken ${auth_access_token} --userType ${user_type}'};
 class Window{constructor(){window=this;this.webContents={mainFrame:{},send(){},on(){},setWindowOpenHandler(){}};}on(){}once(){}removeMenu(){}isDestroyed(){return false;}isVisible(){return true;}async loadFile(){loaded();}}
 const app={isPackaged:false,setName(){},setPath(){},getPath:n=>n==='appData'?temporary:root,getVersion:()=> '1.3.9',requestSingleInstanceLock:()=>true,on(){},quit(){},whenReady:()=>Promise.resolve()};
 class Account{configured(){return true;}async profile(){return{name:'Player',uuid:'a'.repeat(32)};}async getSession(){calls.session++;if(calls.fail!==false)throw Error('Conta sem propriedade do Minecraft Java');return{name:'Player',uuid:'a'.repeat(32),accessToken:'SERVICE_TOKEN',expiresAt:Date.now()+3600000,verifiedAt:Date.now(),ownershipVerified:true};}}
 const modules={electron:{app,BrowserWindow:Window,ipcMain:{handle:(n,f)=>handlers.set(n,f)},safeStorage:{},dialog:{},shell:{}},'./auth.cjs':{MicrosoftAccount:Account,validClientId:require('../auth.cjs').validClientId},'./core.cjs':{...core,async prepare(options){calls.prepare++;await core.installMod(options.payload,options.instanceDir);}},'./game.cjs':{...game,readReady:async()=>prepared?ready:null,async ensureArtifacts(){prepared=true;return ready;},async launch(options){calls.java++;calls.args=game.launchArguments(options.ready,options.session,options.memoryGB);exit=options.onExit;return{exitCode:null};}},'./updates.cjs':{...require('../updates.cjs'),check:async()=>({available:false})}};
 const repo=path.resolve(__dirname,'..');vm.runInNewContext(await fs.readFile(path.join(repo,'main.cjs'),'utf8'),{require:n=>n in modules?modules[n]:require(n.startsWith('.')?path.join(repo,n):n),__dirname:repo,process:{platform:'win32'},console,AbortController,setTimeout:()=>({unref(){}}),setInterval:()=>({unref(){}}),clearInterval(){}},{filename:'main.cjs'});
 await onLoaded;const invoke=(n,...a)=>handlers.get('zyon:'+n)({sender:window.webContents,senderFrame:window.webContents.mainFrame},...a);
 const state=(await invoke('state')).value;assert.equal(state.loginMode,'microsoft');assert.equal(state.instanceDir,instance);assert.equal(state.previousInstanceDir,old);assert.equal(state.offlineProfile,undefined);assert.equal(state.offlineNickname,undefined);assert.equal(state.microsoftClientId,require('../release-config.json').microsoftClientId);
 for(const n of ['offline-login','session-mode','legacy','import-optifine'])assert.equal(handlers.has('zyon:'+n),false,n);
 assert.equal((await invoke('prepare')).ok,false);assert.equal((await invoke('launch')).ok,false);assert.equal(calls.prepare,0);assert.equal(calls.java,0);
 calls.fail=false;assert.equal((await invoke('prepare')).ok,true);assert.equal(calls.prepare,1);
 const profile=await core.readJson(path.join(instance,'config','zyon','auto.json'));assert.equal(profile.modules.killaura,undefined);assert.equal(profile.modules.hud.settings.target,undefined);assert.equal(profile.modules.hud.settings.x,24);assert.equal(profile.modules.keystrokes.settings.x,55);
 calls.fail=true;assert.equal((await invoke('launch')).ok,false);assert.equal(calls.java,0);
 calls.fail=false;assert.equal((await invoke('launch')).ok,true);assert.equal(calls.java,1);assert.equal(calls.args[calls.args.indexOf('--userType')+1],'mojang');exit({code:0});
 await core.atomicWrite(path.join(instance,'mods','foreign.jar'),'external-mod');assert.equal((await invoke('launch')).ok,false);assert.equal(calls.java,1);assert.equal(calls.prepare,1);
 assert.equal(await fs.readFile(path.join(old,'saves','MyWorld','level.dat'),'utf8'),'world-preserved');
 assert.equal(await fs.readFile(path.join(instance,'mods','foreign.jar'),'utf8'),'external-mod');
});
