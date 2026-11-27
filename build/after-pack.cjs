'use strict';
const fs = require('node:fs/promises');
const path = require('node:path');
module.exports = async context => {
  if (context.electronPlatformName === 'win32') {
    const ResEdit = require('resedit');
    const PE = require('pe-library');
    const executable = path.join(context.appOutDir, 'Zyon Launcher.exe');
    const exe = PE.NtExecutable.from(await fs.readFile(executable), { ignoreCert: true });
    const resource = PE.NtExecutableResource.from(exe);
    const version = context.packager.appInfo.version.split('.').map(Number);
    const icon = ResEdit.Data.IconFile.from(await fs.readFile(path.join(context.packager.projectDir, 'assets/icon.ico')));
    const groups = ResEdit.Resource.IconGroupEntry.fromEntries(resource.entries);
    for (const group of groups.length ? groups : [{ id: 1, lang: 1033 }]) {
      ResEdit.Resource.IconGroupEntry.replaceIconsForResource(resource.entries, group.id, group.lang, icon.icons.map(item => item.data));
    }
    for (const info of ResEdit.Resource.VersionInfo.fromEntries(resource.entries)) {
      info.setFileVersion(...version, 0, 1033); info.setProductVersion(...version, 0, 1033);
      info.setStringValues({ lang: 1033, codepage: 1200 }, { ProductName: 'Zyon Launcher', FileDescription: 'Zyon Launcher', CompanyName: 'Zyon', OriginalFilename: 'Zyon Launcher.exe' });
      info.outputToResourceEntries(resource.entries);
    }
    resource.outputResource(exe); await fs.writeFile(executable, Buffer.from(exe.generate()));
    await fs.copyFile(path.join(context.packager.projectDir, 'assets/icon.ico'), path.join(context.appOutDir, 'resources', 'zyon-icon-' + context.packager.appInfo.version + '.ico'));
  }
  // Validate transitive production dependencies from the actual packaged app.
  const check = await fs.mkdtemp(path.join(context.packager.projectDir, 'dist', '.verify-deps-'));
  try {
    require('@electron/asar').extractAll(path.join(context.appOutDir, 'resources', 'app.asar'), check);
    const packaged = require('node:module').createRequire(path.join(check, 'package.json'));
    for (const module of ['./core.cjs','./game.cjs','./offline.cjs','./updates.cjs']) packaged(module);
    console.log('Zyon: packaged backend dependencies verified.');
  } finally { await fs.rm(check, {recursive:true,force:true}); }
  const files = [], dirs = [];
  async function scan(folder) {
    for (const entry of await fs.readdir(folder, { withFileTypes: true })) {
      const file = path.join(folder, entry.name);
      if (entry.isDirectory()) { dirs.push(path.relative(context.appOutDir, file)); await scan(file); }
      else files.push(path.relative(context.appOutDir, file));
    }
  }
  await scan(context.appOutDir);
  const escape = value => value.replaceAll(path.sep, '\\').replaceAll('$', '$$').replaceAll('"', '$\\"');
  const lines = ['; Generated from the actual packaged file list. Never remove unrelated files.', '!macro ZyonUninstallFiles'];
  for (const file of files) lines.push(`  Delete "$INSTDIR\\${escape(file)}"`);
  for (const dir of dirs.sort((a, b) => b.length - a.length)) lines.push(`  RMDir "$INSTDIR\\${escape(dir)}"`);
  lines.push('!macroend');
  const manifest = path.join(context.packager.projectDir, 'build', 'installer-files.nsh');
  await fs.writeFile(manifest, lines.join('\n'));
  console.log(`Zyon: generated uninstall manifest at ${manifest} (${files.length} files).`);
};
