"use strict";
const path=require('node:path'),fs=require('node:fs/promises'),core=require('./core.cjs');
const EDITION='standalone-2';
function migrate(previous,root,publisher){return {edition:EDITION,loginMode:'nickname-offline',instanceDir:typeof previous.instanceDir==='string'&&path.isAbsolute(previous.instanceDir)?previous.instanceDir:path.join(root,'instances','zyon-client-1.8.9'),previousInstanceDir:typeof previous.previousInstanceDir==='string'?previous.previousInstanceDir:'',githubRepository:publisher.githubRepository||''};}
async function validateInstance(instanceDir,payload){if(typeof instanceDir!=='string'||!path.isAbsolute(instanceDir))throw Error('Pasta de jogo inválida.');const folder=path.join(instanceDir,'client');if(await core.exists(folder)&&(await fs.lstat(folder)).isSymbolicLink())throw Error('A pasta do cliente não pode ser um link.');}
module.exports={EDITION,migrate,validateInstance};
async function migrateProfiles(previousDir, instanceDir) {
  if (!previousDir || previousDir === instanceDir) return;
  const source = path.join(previousDir, 'config', 'zyon'), destination = path.join(instanceDir, 'config', 'zyon');
  const allowed = {
  "hud": [
    "logo",
    "fps",
    "coords",
    "modules",
    "x",
    "y",
    "_favorite"
  ],
  "crosshair": [
    "size",
    "gap",
    "dot",
    "_favorite"
  ],
  "zoom": [
    "factor",
    "_favorite"
  ],
  "theme": [
    "menu",
    "style",
    "pixel",
    "accent",
    "customAccent",
    "highlight",
    "icons",
    "shadow",
    "transitions",
    "sounds",
    "hideBackground",
    "opacity",
    "rainbow",
    "frame",
    "background",
    "button",
    "blur",
    "blurStrength",
    "contrast",
    "_favorite"
  ],
  "badge": [
    "_favorite"
  ],
  "selfname": [
    "scale",
    "_favorite"
  ],
  "fullbright": [
    "_favorite"
  ],
  "itemphysics": [
    "speed",
    "_favorite"
  ],
  "nametag": [
    "hide",
    "own",
    "_favorite"
  ],
  "classicpvp": [
    "inventory",
    "block",
    "position",
    "food",
    "rod",
    "equip",
    "_favorite"
  ],
  "blockoverlay": [
    "customOutline",
    "outlineColor",
    "width",
    "customFill",
    "fillColor",
    "fillAlpha",
    "_favorite"
  ],
  "fps": [
    "labelColor",
    "bracketColor",
    "valueColor",
    "lineHeight",
    "format",
    "_favorite",
    "x",
    "y",
    "scale",
    "alignX",
    "alignY"
  ],
  "health": [
    "mode",
    "icon",
    "hardcore",
    "text",
    "scale",
    "_favorite"
  ],
  "mainhand": [
    "durability",
    "durabilityColor",
    "_favorite",
    "x",
    "y",
    "scale",
    "alignX",
    "alignY"
  ],
  "chestplate": [
    "durability",
    "durabilityColor",
    "_favorite",
    "x",
    "y",
    "scale",
    "alignX",
    "alignY"
  ],
  "boots": [
    "durability",
    "durabilityColor",
    "_favorite",
    "x",
    "y",
    "scale",
    "alignX",
    "alignY"
  ],
  "keystrokes": [
    "mouse",
    "cps",
    "jump",
    "color",
    "opacity",
    "text",
    "background",
    "pressed",
    "customPressed",
    "outline",
    "textOutline",
    "outlineColor",
    "rounded",
    "radius",
    "_favorite",
    "x",
    "y",
    "scale",
    "alignX",
    "alignY",
    "_key0",
    "_code0",
    "_kx0",
    "_ky0",
    "_key1",
    "_code1",
    "_kx1",
    "_ky1",
    "_key2",
    "_code2",
    "_kx2",
    "_ky2",
    "_key3",
    "_code3",
    "_kx3",
    "_ky3",
    "_key4",
    "_code4",
    "_kx4",
    "_ky4",
    "_key5",
    "_code5",
    "_kx5",
    "_ky5",
    "_key6",
    "_code6",
    "_kx6",
    "_ky6",
    "_key7",
    "_code7",
    "_kx7",
    "_ky7"
  ],
  "helmet": [
    "durability",
    "durabilityColor",
    "_favorite",
    "x",
    "y",
    "scale",
    "alignX",
    "alignY"
  ],
  "leggings": [
    "durability",
    "durabilityColor",
    "_favorite",
    "x",
    "y",
    "scale",
    "alignX",
    "alignY"
  ],
  "autotext": [
    "_favorite"
  ]
};
  if (!await core.exists(source) || (await fs.lstat(source)).isSymbolicLink()) return;
  for (const entry of await fs.readdir(source, { withFileTypes: true })) {
    if (!entry.isFile() || !/^[a-zA-Z0-9_-]{1,32}\.json$/.test(entry.name)) continue;
    const file = path.join(source, entry.name), target = path.join(destination, entry.name);
    if (await core.exists(target) || (await fs.stat(file)).size > 1048576) continue;
    let data; try { data = JSON.parse(await fs.readFile(file, 'utf8')); } catch { continue; }
    if (data.schema !== 1 || typeof data.modules !== 'object' || !data.modules) continue;
    const modules = {};
    for (const [id, keys] of Object.entries(allowed)) {
      const old = data.modules[id]; if (!old || typeof old !== 'object') continue;
      const settings = {};
      for (const key of keys) if (typeof old.settings?.[key] === 'number' && Number.isFinite(old.settings[key])) settings[key] = old.settings[key];
      modules[id] = { enabled: old.enabled === true, key: Number.isInteger(old.key) && old.key >= 0 && old.key < 256 ? old.key : 0, settings };
    }
    await core.atomicWrite(target, JSON.stringify({ schema: 1, modules }, null, 2));
  }
}
module.exports.migrateProfiles = migrateProfiles;
