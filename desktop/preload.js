const { contextBridge, ipcRenderer } = require("electron");
contextBridge.exposeInMainWorld("synkronDesktop", {
  getScreenSources: () => ipcRenderer.invoke("synkron:get-screen-sources")
});
