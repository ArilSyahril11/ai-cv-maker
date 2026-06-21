const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const fs = require('fs');

app.whenReady().then(async () => {
  const win = new BrowserWindow({
    show: false,
    webPreferences: {
      nodeIntegration: true,
      contextIsolation: false
    }
  });

  const oldPath = path.join(__dirname, 'index.html');
  // Load the old URL to access its localStorage
  await win.loadURL(`file://${oldPath}`);
  
  const drafts = await win.webContents.executeJavaScript(`localStorage.getItem('ai_cv_maker_drafts')`);
  console.log("Old drafts found:", drafts ? drafts.length : "None");
  
  if (drafts && drafts.length > 5) {
      // Save it to a temp file
      fs.writeFileSync(path.join(__dirname, 'old_drafts.json'), drafts);
      console.log("Saved to old_drafts.json");
      
      // Load the new URL and set it!
      const newPath = path.join(__dirname, '../app/src/main/assets/index.html');
      await win.loadURL(`file://${newPath}`);
      
      await win.webContents.executeJavaScript(`
          const existing = localStorage.getItem('ai_cv_maker_drafts');
          let merged = [];
          if (existing) {
              try { merged = JSON.parse(existing); } catch(e) {}
          }
          const oldDrafts = ${JSON.stringify(drafts)};
          let parsedOld = [];
          try { parsedOld = JSON.parse(oldDrafts); } catch(e) {}
          
          parsedOld.forEach(oldD => {
              if (!merged.find(m => m.id === oldD.id)) {
                  merged.push(oldD);
              }
          });
          localStorage.setItem('ai_cv_maker_drafts', JSON.stringify(merged));
          console.log("Merged drafts successfully.");
      `);
      console.log("Migration complete!");
  }
  
  app.quit();
});
