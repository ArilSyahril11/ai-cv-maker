const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('AndroidBridge', {
  triggerAiPolish(type, text, callbackId, apiKey) {
    ipcRenderer.send('ai-polish', { type, text, callbackId, apiKey });
  },
  sharePdf(base64Data, fileName) {
    ipcRenderer.send('save-pdf', { base64Data, fileName });
  },

  exportPdfContent(htmlContent, fileName, scaleFactor) {
    ipcRenderer.send('export-pdf-content', { htmlContent, fileName, scaleFactor: scaleFactor || 1.0 });
  },
  analyzeCV(cvData, apiKey) {
    ipcRenderer.send('analyze-cv', { cvData, apiKey });
  },
  scanCvReference(base64Data, mimeType, apiKey) {
    ipcRenderer.send('scan-cv-reference', { base64Data, mimeType, apiKey });
  },
  // Register a callback when PDF export is done (works across contextIsolation)
  onPdfResult(callback) {
    ipcRenderer.removeAllListeners('pdf-export-result');
    ipcRenderer.on('pdf-export-result', (_event, data) => callback(data));
  },
  // Register a callback when CV analysis is done
  onAnalyzeResult(callback) {
    ipcRenderer.removeAllListeners('analyze-cv-result');
    ipcRenderer.on('analyze-cv-result', (_event, data) => callback(data));
  },
  onScanCvReferenceResult(callback) {
    ipcRenderer.removeAllListeners('scan-cv-reference-result');
    ipcRenderer.on('scan-cv-reference-result', (_event, data) => callback(data));
  }
});

contextBridge.exposeInMainWorld('DesktopBridge', {
  minimizeWindow() { ipcRenderer.send('window-minimize'); },
  maximizeWindow() { ipcRenderer.send('window-maximize'); },
  closeWindow() { ipcRenderer.send('window-close'); },
  
  getStorageItem(key) { return ipcRenderer.sendSync('storage-get', key); },
  setStorageItem(key, value) { ipcRenderer.sendSync('storage-set', {key, value}); },
  
  openProject() { ipcRenderer.send('file-open-dialog'); },
  saveProject(data, currentFilePath) { ipcRenderer.send('file-save-dialog', { data, currentFilePath }); },
  saveProjectAs(data) { ipcRenderer.send('file-save-dialog', { data, currentFilePath: null }); },
  
  printContent(htmlContent) { ipcRenderer.send('print-content', { htmlContent }); },
  
  onMenuAction(callback) {
    ipcRenderer.removeAllListeners('menu-action');
    ipcRenderer.on('menu-action', (_event, action) => callback(action));
  },
  onFileOpened(callback) {
    ipcRenderer.removeAllListeners('file-opened');
    ipcRenderer.on('file-opened', (_event, data) => callback(data));
  },
  onFileSaved(callback) {
    ipcRenderer.removeAllListeners('file-saved');
    ipcRenderer.on('file-saved', (_event, data) => callback(data));
  },
  
  // Auto Updater
  onUpdateStatus(callback) {
    ipcRenderer.removeAllListeners('update-status');
    ipcRenderer.on('update-status', (_event, status, message) => callback(status, message));
  },
  checkForUpdates() { ipcRenderer.send('check-for-updates'); },
  downloadUpdate() { ipcRenderer.send('download-update'); },
  quitAndInstall() { ipcRenderer.send('quit-and-install'); }
});
