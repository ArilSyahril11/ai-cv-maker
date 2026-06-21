const { app, BrowserWindow, ipcMain, dialog, shell, Menu } = require('electron');
const path = require('path');
const fs = require('fs');
const dotenv = require('dotenv');
const { autoUpdater } = require('electron-updater');
const { polishText, analyzeCv, scanCvReference } = require('./gemini');

// Configure autoUpdater to not automatically download updates without user consent
autoUpdater.autoDownload = false;

let mainWindow = null;

function loadEnv() {
  const envPaths = [
    path.join(__dirname, '.env'),
    path.join(__dirname, '..', '.env'),
    path.join(app.getPath('userData'), '.env'),
  ];

  for (const envPath of envPaths) {
    if (fs.existsSync(envPath)) {
      dotenv.config({ path: envPath, override: true });
    }
  }
}

function getIndexHtmlPath() {
  const packagedPath = path.join(process.resourcesPath, 'app', 'index.html');
  if (app.isPackaged && fs.existsSync(packagedPath)) {
    return packagedPath;
  }
  return path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'index.html');
}

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 860,
    minWidth: 960,
    minHeight: 640,
    title: 'AI CV Maker',
    frame: false,
    titleBarStyle: 'hidden',
    webPreferences: {
      preload: path.join(__dirname, 'preload.js'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
    },
  });

  const indexPath = getIndexHtmlPath();
  
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('http://') || url.startsWith('https://')) {
      shell.openExternal(url);
    }
    return { action: 'deny' };
  });

  mainWindow.loadFile(indexPath);
  
  // Make the window maximized by default upon launch
  mainWindow.maximize();

  // Set application menu to enable native shortcuts and broadcast menu actions
  const template = [
    {
      label: 'File',
      submenu: [
        { label: 'New Project', accelerator: 'CmdOrCtrl+N', click: () => { mainWindow.webContents.send('menu-action', 'new-project'); } },
        { label: 'Open Project...', accelerator: 'CmdOrCtrl+O', click: () => { mainWindow.webContents.send('menu-action', 'open-project'); } },
        { type: 'separator' },
        { label: 'Save', accelerator: 'CmdOrCtrl+S', click: () => { mainWindow.webContents.send('menu-action', 'save-project'); } },
        { label: 'Save As...', accelerator: 'CmdOrCtrl+Shift+S', click: () => { mainWindow.webContents.send('menu-action', 'save-as-project'); } },
        { type: 'separator' },
        { label: 'Export to PDF...', accelerator: 'CmdOrCtrl+E', click: () => { mainWindow.webContents.send('menu-action', 'export-pdf'); } },
        { label: 'Print...', accelerator: 'CmdOrCtrl+P', click: () => { mainWindow.webContents.send('menu-action', 'print-project'); } },
        { type: 'separator' },
        { role: 'quit' }
      ]
    },
    {
      label: 'Edit',
      submenu: [
        { role: 'undo' },
        { role: 'redo' },
        { type: 'separator' },
        { role: 'cut' },
        { role: 'copy' },
        { role: 'paste' },
        { role: 'selectAll' }
      ]
    },
    {
      label: 'View',
      submenu: [
        { role: 'reload' },
        { role: 'forceReload' },
        { role: 'toggleDevTools' },
        { type: 'separator' },
        { role: 'resetZoom' },
        { role: 'zoomIn' },
        { role: 'zoomOut' },
        { type: 'separator' },
        { role: 'togglefullscreen' }
      ]
    }
  ];
  const menu = Menu.buildFromTemplate(template);
  Menu.setApplicationMenu(menu);
  mainWindow.setMenuBarVisibility(false);
  mainWindow.autoHideMenuBar = false;

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

ipcMain.on('ai-polish', async (event, { type, text, callbackId, apiKey }) => {
  try {
    const result = await polishText(type, text, apiKey);
    const jsonResult = JSON.stringify(result);
    event.sender.executeJavaScript(
      `window.onAiPolishResult(${JSON.stringify(callbackId)}, true, ${jsonResult})`
    );
  } catch (err) {
    const jsonResult = JSON.stringify(`Error: ${err.message}`);
    event.sender.executeJavaScript(
      `window.onAiPolishResult(${JSON.stringify(callbackId)}, false, ${jsonResult})`
    );
  }
});

ipcMain.on('analyze-cv', async (event, { cvData, apiKey }) => {
  try {
    const result = await analyzeCv(cvData, apiKey);
    event.sender.send('analyze-cv-result', { success: true, data: result });
  } catch (err) {
    event.sender.send('analyze-cv-result', { success: false, error: err.message });
  }
});

ipcMain.on('scan-cv-reference', async (event, { base64Data, mimeType, apiKey }) => {
  try {
    const result = await scanCvReference(base64Data, mimeType, apiKey);
    event.sender.send('scan-cv-reference-result', { success: true, data: result });
  } catch (err) {
    event.sender.send('scan-cv-reference-result', { success: false, error: err.message });
  }
});

const storagePath = path.join(app.getPath('userData'), 'local_storage.json');
function loadStorage() {
  try { return JSON.parse(fs.readFileSync(storagePath, 'utf8')); } catch (e) { return {}; }
}
function saveStorage(data) {
  fs.writeFileSync(storagePath, JSON.stringify(data));
}
ipcMain.on('storage-get', (event, key) => {
  const data = loadStorage();
  event.returnValue = data[key] || null;
});
ipcMain.on('storage-set', (event, {key, value}) => {
  const data = loadStorage();
  data[key] = value;
  saveStorage(data);
  event.returnValue = true;
});

ipcMain.on('save-pdf', async (event, { base64Data, fileName }) => {
  const win = BrowserWindow.fromWebContents(event.sender);

  const { canceled, filePath } = await dialog.showSaveDialog(win, {
    title: 'Simpan CV sebagai PDF',
    defaultPath: fileName,
    filters: [{ name: 'PDF Document', extensions: ['pdf'] }],
  });

  if (canceled || !filePath) return;

  try {
    const pdfBuffer = Buffer.from(base64Data, 'base64');
    fs.writeFileSync(filePath, pdfBuffer);
    const { response } = await dialog.showMessageBox(win, {
      type: 'info',
      title: 'PDF Tersimpan',
      message: 'CV berhasil disimpan!',
      detail: filePath,
      buttons: ['Buka Folder', 'Buka File', 'Tutup'],
      defaultId: 1,
    });
    if (response === 0) {
      shell.showItemInFolder(filePath);
    } else if (response === 1) {
      shell.openPath(filePath);
    }
  } catch (err) {
    dialog.showErrorBox('Gagal Menyimpan PDF', err.message);
  }
});

// Export PDF: render only the CV in a hidden window, then printToPDF
ipcMain.on('export-pdf-content', async (event, { htmlContent, fileName, scaleFactor }) => {
  const win = BrowserWindow.fromWebContents(event.sender);

  const { canceled, filePath } = await dialog.showSaveDialog(win, {
    title: 'Simpan CV sebagai PDF',
    defaultPath: fileName,
    filters: [{ name: 'PDF Document', extensions: ['pdf'] }],
  });

  if (canceled || !filePath) {
    event.sender.send('pdf-export-result', { success: false, error: 'Dibatalkan' });
    return;
  }

  // Write standalone CV HTML to a temp file
  const tempPath = path.join(app.getPath('temp'), 'cv_export_temp.html');
  try {
    fs.writeFileSync(tempPath, htmlContent, 'utf-8');
  } catch (err) {
    event.sender.send('pdf-export-result', { success: false, error: err.message });
    return;
  }

  // Create an invisible window just for printing the CV
  const printWin = new BrowserWindow({
    width: 794,
    height: 1123,
    show: false,
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
    },
  });

  printWin.loadFile(tempPath);

  printWin.webContents.once('did-finish-load', () => {
    // Give fonts, FontAwesome icons and images 4s to fully render
    setTimeout(async () => {
      try {
        // scaleFactor from renderer is 0.4–1.0; Electron printToPDF expects 10–200 (percent)
        const scale = Math.min(1.0, Math.max(0.4, scaleFactor || 1.0));
        const electronScale = Math.round(scale * 100); // e.g. 0.85 → 85

        const pdfData = await printWin.webContents.printToPDF({
          pageSize: 'A4',
          printBackground: true,
          landscape: false,
          margins: { marginType: 'none' },
          scaleFactor: electronScale,
        });

        printWin.close();
        try { fs.unlinkSync(tempPath); } catch (_) {}

        fs.writeFileSync(filePath, pdfData);
        event.sender.send('pdf-export-result', { success: true });

        const { response } = await dialog.showMessageBox(win, {
          type: 'info',
          title: 'PDF Tersimpan',
          message: 'CV berhasil disimpan!',
          detail: filePath,
          buttons: ['Buka Folder', 'Buka File', 'Tutup'],
          defaultId: 1,
        });
        if (response === 0) shell.showItemInFolder(filePath);
        else if (response === 1) shell.openPath(filePath);

      } catch (err) {
        printWin.close();
        try { fs.unlinkSync(tempPath); } catch (_) {}
        event.sender.send('pdf-export-result', { success: false, error: err.message });
        dialog.showErrorBox('Gagal Ekspor PDF', err.message);
      }
    }, 2500);
  });
});

// Window Control Handlers
ipcMain.on('window-minimize', () => { if (mainWindow) mainWindow.minimize(); });
ipcMain.on('window-maximize', () => { 
  if (mainWindow) {
    if (mainWindow.isMaximized()) mainWindow.unmaximize();
    else mainWindow.maximize();
  }
});
ipcMain.on('window-close', () => { if (mainWindow) mainWindow.close(); });

// File System (Native) Handlers for .cvm
ipcMain.on('file-open-dialog', async (event) => {
  const win = BrowserWindow.fromWebContents(event.sender);
  const { canceled, filePaths } = await dialog.showOpenDialog(win, {
    title: 'Buka Proyek CV',
    filters: [{ name: 'CV Maker Project', extensions: ['cvm'] }],
    properties: ['openFile']
  });
  if (canceled || filePaths.length === 0) return;
  try {
    const content = fs.readFileSync(filePaths[0], 'utf-8');
    event.sender.send('file-opened', { success: true, filePath: filePaths[0], data: JSON.parse(content) });
  } catch(err) {
    dialog.showErrorBox('Gagal Membuka File', err.message);
  }
});

ipcMain.on('file-save-dialog', async (event, { data, currentFilePath }) => {
  const win = BrowserWindow.fromWebContents(event.sender);
  let targetPath = currentFilePath;

  if (!targetPath) {
    const { canceled, filePath } = await dialog.showSaveDialog(win, {
      title: 'Simpan Proyek CV',
      defaultPath: 'Proyek CV Baru.cvm',
      filters: [{ name: 'CV Maker Project', extensions: ['cvm'] }],
    });
    if (canceled || !filePath) return;
    targetPath = filePath;
  }

  try {
    fs.writeFileSync(targetPath, JSON.stringify(data, null, 2), 'utf-8');
    event.sender.send('file-saved', { success: true, filePath: targetPath });
  } catch(err) {
    dialog.showErrorBox('Gagal Menyimpan File', err.message);
  }
});

// Native Print Handler
ipcMain.on('print-content', async (event, { htmlContent }) => {
  const tempPath = path.join(app.getPath('temp'), 'cv_print_temp.html');
  try {
    fs.writeFileSync(tempPath, htmlContent, 'utf-8');
  } catch (err) {
    dialog.showErrorBox('Gagal Memproses Print', err.message);
    return;
  }
  const printWin = new BrowserWindow({
    width: 794, height: 1123, show: false,
    webPreferences: { nodeIntegration: false, contextIsolation: true }
  });
  printWin.loadFile(tempPath);
  printWin.webContents.once('did-finish-load', () => {
    setTimeout(() => {
      printWin.webContents.print({ printBackground: true, margins: { marginType: 'none' } }, (success, failureReason) => {
        printWin.close();
        try { fs.unlinkSync(tempPath); } catch (_) {}
      });
    }, 2000);
  });
});

app.whenReady().then(() => {
  loadEnv();
  createWindow();

  // Auto-updater event listeners
  autoUpdater.on('checking-for-update', () => {
    if (mainWindow) mainWindow.webContents.send('update-status', 'checking', 'Mengecek pembaruan via GitHub...');
  });
  autoUpdater.on('update-available', (info) => {
    if (mainWindow) mainWindow.webContents.send('update-status', 'available', `Pembaruan v${info.version} tersedia!`);
  });
  autoUpdater.on('update-not-available', (info) => {
    if (mainWindow) mainWindow.webContents.send('update-status', 'not-available', `Aplikasi ini sudah versi terbaru (v${app.getVersion()}).`);
  });
  autoUpdater.on('error', (err) => {
    if (mainWindow) mainWindow.webContents.send('update-status', 'error', 'Gagal mengecek pembaruan.');
  });
  autoUpdater.on('download-progress', (progressObj) => {
    let log_message = `Mengunduh... ${Math.round(progressObj.percent)}%`;
    if (mainWindow) mainWindow.webContents.send('update-status', 'downloading', log_message);
  });
  autoUpdater.on('update-downloaded', (info) => {
    if (mainWindow) mainWindow.webContents.send('update-status', 'ready', `Pembaruan v${info.version} siap diinstal.`);
  });

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) createWindow();
  });
});

// IPC handlers for updater
ipcMain.on('check-for-updates', () => {
  try {
    autoUpdater.checkForUpdatesAndNotify();
  } catch (err) {
    if (mainWindow) mainWindow.webContents.send('update-status', 'error', 'Auto-update hanya tersedia pada versi build (compiled).');
  }
});
ipcMain.on('download-update', () => {
  autoUpdater.downloadUpdate();
});
ipcMain.on('quit-and-install', () => {
  autoUpdater.quitAndInstall(false, true);
});

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

