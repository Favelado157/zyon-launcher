'use strict';
const { contextBridge, ipcRenderer } = require('electron');
const call = (name, ...args) => ipcRenderer.invoke('zyon:' + name, ...args);
contextBridge.exposeInMainWorld('zyon', Object.freeze({
  state: () => call('state'), pick: kind => call('pick', kind), settings: value => call('settings', value),
  prepare: () => call('prepare'), cancel: () => call('cancel'), launch: () => call('launch'),
  nickname: value => call('nickname', value),
  checkUpdates: () => call('check-updates'), installUpdate: () => call('install-update'),
  folder: kind => call('folder', kind), external: kind => call('external', kind),
  onProgress(callback) {
    const listener = (_event, value) => callback(value);
    ipcRenderer.on('zyon:progress', listener);
    return () => ipcRenderer.removeListener('zyon:progress', listener);
  },
  onEvent(name, callback) {
    if (!['auth-code', 'game-state', 'update-state', 'update-download'].includes(name)) throw new Error('Evento inválido.');
    const listener = (_event, value) => callback(value);
    ipcRenderer.on('zyon:' + name, listener);
    return () => ipcRenderer.removeListener('zyon:' + name, listener);
  }
}));
