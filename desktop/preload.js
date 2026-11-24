const { contextBridge, ipcRenderer } = require("electron");
contextBridge.exposeInMainWorld("akiraDesktop", {
  getScreenSources: () => ipcRenderer.invoke("akira:get-screen-sources")
});
