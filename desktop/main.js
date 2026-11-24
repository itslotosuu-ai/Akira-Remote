const { app, BrowserWindow, Menu, ipcMain, desktopCapturer } = require("electron");
const path = require("path");

ipcMain.handle("synkron:get-screen-sources", async () => {
  const sources = await desktopCapturer.getSources({ types: ["screen"], thumbnailSize: { width: 320, height: 180 } });
  return sources.map(source => ({ id: source.id, name: source.name }));
});

function createWindow() {
  const win = new BrowserWindow({
    width: 1440,
    height: 920,
    minWidth: 900,
    minHeight: 650,
    backgroundColor: "#050907",
    title: "SYNKRON Desktop // CONTROL NODE",
    autoHideMenuBar: true,
    webPreferences: {
      preload: path.join(__dirname, "preload.js"),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  });
  win.loadFile(path.join(__dirname, "index.html"));
}
app.whenReady().then(() => {
  Menu.setApplicationMenu(null);
  createWindow();
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});
app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});
