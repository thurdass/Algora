const { app, BrowserWindow, ipcMain, dialog, Menu } = require('electron');
const fs = require('node:fs/promises');
const path = require('node:path');
let win;
// Only paths selected through a native dialog are writable by the renderer.
const allowedPaths = new Set();
function decode(bytes) {
  try { return new TextDecoder('utf-8', { fatal: true }).decode(bytes); }
  catch { return new TextDecoder('windows-1252').decode(bytes); }
}
ipcMain.handle('file:open', async (event) => {
  if (event.sender !== win.webContents) throw new Error('Origem inválida');
  const result = await dialog.showOpenDialog(win, { filters: [{ name: 'Algoritmos', extensions: ['alg'] }], properties: ['openFile'] });
  if (result.canceled) return null;
  const file = result.filePaths[0];
  allowedPaths.add(file);
  return { path: file, name: path.basename(file), content: decode(await fs.readFile(file)) };
});
ipcMain.handle('file:save', async (event, data) => {
  if (event.sender !== win.webContents || typeof data?.content !== 'string') throw new Error('Dados inválidos');
  let file = allowedPaths.has(data.path) ? data.path : null;
  if (!file) {
    const result = await dialog.showSaveDialog(win, { defaultPath: 'semnome.alg', filters: [{ name: 'Algoritmos', extensions: ['alg'] }] });
    if (result.canceled) return null;
    file = result.filePath;
    allowedPaths.add(file);
  }
  await fs.writeFile(file, data.content, 'utf8');
  return { path: file };
});
app.whenReady().then(() => {
  Menu.setApplicationMenu(null);
  win = new BrowserWindow({ width: 1440, height: 940, minWidth: 820, minHeight: 600, backgroundColor: '#0d1017', title: 'Algora', webPreferences: { preload: path.join(__dirname, 'preload.cjs'), contextIsolation: true, nodeIntegration: false, sandbox: true } });
  win.webContents.setWindowOpenHandler(() => ({ action: 'deny' }));
  win.webContents.on('will-navigate', (event) => event.preventDefault());
  if (process.argv.includes('--dev')) win.loadURL('http://127.0.0.1:5173');
  else win.loadFile(path.join(__dirname, '../dist/index.html'));
});
app.on('window-all-closed', () => app.quit());
