'use strict';
// Build the independent client against an ephemeral, verified vanilla archive.
// Never package the generated Minecraft derivative in the distributed SDK.
const fs=require('node:fs/promises'),path=require('node:path'),crypto=require('node:crypto'),{spawnSync}=require('node:child_process'),Zip=require('adm-zip');
const root=path.resolve(__dirname,'..'),build=path.join(root,'client-src/build'),cache=path.join(build,'dependencies'),core=require('../core.cjs');
const sha=b=>crypto.createHash('sha1').update(b).digest('hex');
const dependencies=[
 ['org/ow2/asm/asm/9.3/asm-9.3.jar','8e6300ef51c1d801a7ed62d07cd221aca3a90640'],
 ['org/ow2/asm/asm-commons/9.3/asm-commons-9.3.jar','1f2a432d1212f5c352ae607d7b61dcae20c20af5'],
 ['org/ow2/asm/asm-tree/9.3/asm-tree-9.3.jar','78d2ecd61318b5a58cd04fb237636c0e86b77d97'],
 ['junit/junit/4.13.2/junit-4.13.2.jar','8ac9e16d933b6fb43bc7f576336b8f4d7eb5ba12'],
 ['org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar','42a25dc3219429f0e5d060061f71acb49bf010a0']
];
async function get(url,file,hash,algorithm='sha1'){
 if(await core.exists(file)&&core.digest(await fs.readFile(file),algorithm)===hash)return file;
 const data=await core.fetchBuffer(url,{hash,algorithm,maxBytes:32*1024*1024});
 // fetchBuffer uses hashAlgorithm; verify explicitly as an independent build boundary.
 if(core.digest(data,algorithm)!==hash)throw Error('Dependency digest mismatch: '+url);
 await core.atomicWrite(file,data);return file;
}
async function files(dir){let result=[];for(const e of await fs.readdir(dir,{withFileTypes:true})){const p=path.join(dir,e.name);if(e.isDirectory())result.push(...await files(p));else result.push(p);}return result.sort();}
const javaHome=process.env.ZYON_JAVA8||process.env.JAVA_HOME;
const exe=n=>javaHome?path.join(javaHome,'bin',n+(process.platform==='win32'?'.exe':'')):n;
function run(program,args){const p=spawnSync(exe(program),args,{stdio:'inherit',cwd:root});if(p.error)throw p.error;if(p.status!==0)throw Error(program+' failed: '+p.status);}
async function main(){
 await fs.mkdir(cache,{recursive:true});
 const metadata=path.join(cache,'minecraft-1.8.9.json');
 // Immutable metadata URL and digest from the Mojang version manifest.
 if(!await core.exists(metadata)||core.digest(await fs.readFile(metadata))!==core.METADATA_SHA256){const manifest=JSON.parse((await core.fetchBuffer('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json')).toString());const entry=manifest.versions.find(v=>v.id==='1.8.9');if(!entry)throw Error('Missing vanilla metadata');await get(entry.url,metadata,core.METADATA_SHA256,'sha256');}
 const m=JSON.parse(await fs.readFile(metadata,'utf8'));
 const vanilla=await get(m.downloads.client.url,path.join(cache,'minecraft-original.jar'),core.CLIENT_SHA1);
 const libraries=[];for(const l of m.libraries){const a=l.downloads?.artifact;if(a&&!a.path.includes('java-objc-bridge'))libraries.push(await get(a.url,path.join(cache,a.path),a.sha1));}
 const deps=[];for(const [p,h]of dependencies)deps.push(await get('https://repo.maven.apache.org/maven2/'+p,path.join(cache,p),h));
 const bootstrap=path.join(build,'bootstrap'),classes=path.join(build,'classes'),tests=path.join(build,'test-classes'),mapped=path.join(build,'vanilla-client.jar');
 for(const d of [bootstrap,classes,tests]){await fs.rm(d,{recursive:true,force:true});await fs.mkdir(d,{recursive:true});}
 const asm=deps.slice(0,3).join(path.delimiter),resources=path.join(root,'client-src/src/main/resources');
 run('javac',['-encoding','UTF-8','-source','8','-target','8','-cp',asm,'-d',bootstrap,path.join(root,'client-src/bootstrap/dev/zyon/bootstrap/PatchMain.java')]);
 run('java',['-cp',[bootstrap,resources,asm].join(path.delimiter),'dev.zyon.bootstrap.PatchMain',vanilla,mapped]);
 const cp=[mapped,...libraries].join(path.delimiter),sources=(await files(path.join(root,'client-src/src/main/java'))).filter(f=>f.endsWith('.java'));
 run('javac',['-encoding','UTF-8','-source','8','-target','8','-cp',cp,'-d',classes,...sources]);
 const zip=new Zip();function add(name,bytes){if(!zip.getEntry(name)){zip.addFile(name,bytes);zip.getEntry(name).header.time=new Date(1980,0,1);}}
 add('META-INF/MANIFEST.MF',Buffer.from('Manifest-Version: 1.0\nMain-Class: dev.zyon.platform.Main\n\n'));
 for(const dir of [classes,bootstrap,resources])for(const file of await files(dir)){const name=path.relative(dir,file).split(path.sep).join('/');if(name!=='mcmod.info')add(name,await fs.readFile(file));}
 for(const file of deps.slice(0,3))for(const entry of new Zip(await fs.readFile(file)).getEntries())if(entry.entryName.startsWith('org/objectweb/asm/')&&entry.entryName.endsWith('.class'))add(entry.entryName,entry.getData());
 add('THIRD-PARTY.txt',Buffer.from('ASM 9.3: BSD 3-Clause (licenses/ASM.txt). MCP stable 22 symbolic mappings are used for local integration. Minecraft code and original assets are downloaded from Mojang; they are not included in this archive. OptiFine is not included. Public capes, when available, are retrieved from s.optifine.net.\n'));
 await core.atomicWrite(path.join(root,'payload',core.CLIENT),zip.toBuffer());
 await fs.writeFile(path.join(build,'classpath.json'),JSON.stringify({cp,libraries,asm:deps.slice(0,3),junit:deps.slice(3),metadata,vanilla,java:exe('java')},null,2));
 require('./prepare-release.cjs');
 if(process.argv.includes('--test')){
  const testCp=[classes,cp,...deps.slice(3)].join(path.delimiter),sources=(await files(path.join(root,'client-src/src/test/java'))).filter(f=>f.endsWith('.java'));
  run('javac',['-encoding','UTF-8','-cp',testCp,'-d',tests,...sources]);const names=[];for(const source of sources)if((await fs.readFile(source,'utf8')).includes('@Test'))names.push('dev.zyon.'+path.basename(source,'.java'));
  run('java',['-Djava.awt.headless=true','-cp',[tests,resources,testCp].join(path.delimiter),'org.junit.runner.JUnitCore',...names]);
 }
 console.log('Independent Zyon SDK built; original and patched Minecraft archives remain in ignored client-src/build.');
}
main().catch(e=>{console.error(e);process.exitCode=1;});
