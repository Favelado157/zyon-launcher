'use strict';
const fs=require('node:fs'),path=require('node:path'),{normalizeRepository}=require('../updates.cjs');
function publisherConfiguration(repository){const repo=normalizeRepository(repository);if(!repo)throw Error('Informe usuario/repositorio.');return{githubRepository:repo};}
if(require.main===module){const a=process.argv.slice(2),i=a.indexOf('--repository');const config=publisherConfiguration(i<0?'':a[i+1]),file=path.join(__dirname,'../release-config.json'),old=JSON.parse(fs.readFileSync(file));if(old.discordInvite)config.discordInvite=old.discordInvite;fs.writeFileSync(file,JSON.stringify(config,null,2)+'\n');console.log('Canal de atualizacoes: '+config.githubRepository);}
module.exports={publisherConfiguration};
