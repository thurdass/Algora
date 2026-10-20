const { contextBridge, ipcRenderer } = require("electron");
contextBridge.exposeInMainWorld("algora", {
  openFile: () => ipcRenderer.invoke("file:open"),
  saveFile: (data) => ipcRenderer.invoke("file:save", data),
});
