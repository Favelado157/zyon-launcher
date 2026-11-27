'use strict';
const path=require('node:path'),{spawnSync}=require('node:child_process');
const root=path.resolve(__dirname,'..'),cp=require('../client-src/build/classpath.json');
const result=spawnSync(process.execPath,['--test',...require('node:fs').readdirSync(path.join(root,'test')).filter(f=>f.endsWith('.test.cjs')).map(f=>path.join(root,'test',f))],{cwd:root,stdio:'inherit',env:{...process.env,MINECRAFT_FIXTURE:cp.metadata,VANILLA_JAR:cp.vanilla,JAVA8_EXE:cp.java}});
if(result.error)throw result.error;process.exitCode=result.status===null?1:result.status;
