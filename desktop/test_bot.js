const { app, BrowserWindow, ipcMain, Menu } = require('electron');
const path = require('path');

app.whenReady().then(async () => {
    const mainWindow = new BrowserWindow({
        width: 1200,
        height: 800,
        webPreferences: {
            preload: path.join(__dirname, 'preload.js'),
            contextIsolation: true,
            nodeIntegration: false
        }
    });

    mainWindow.loadFile(path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'index.html'));

    mainWindow.webContents.on('did-finish-load', async () => {
        try {
            console.log("--- TEST AUTOMATION START ---");
            
            // 1. Wait for UI
            await new Promise(r => setTimeout(r, 2000));
            
            // 2. Click draft
            await mainWindow.webContents.executeJavaScript(`
                document.querySelectorAll('.grid > div')[0].click();
            `);
            console.log("Clicked draft...");
            
            await new Promise(r => setTimeout(r, 2000));
            
            // 3. Inspect the iframe HTML and test
            const result = await mainWindow.webContents.executeJavaScript(`
                (async () => {
                    const logs = [];
                    const log = m => { console.log(m); logs.push(m); };
                    try {
                        const iframe = document.getElementById('cv-preview-frame');
                        const doc = iframe.contentDocument;
                        
                        const h1 = doc.querySelector('h1, h2, p, span');
                        log("Found element: " + h1.tagName + " with text: " + h1.innerText);
                        
                        // Simulate click to select
                        h1.click();
                        log("Element clicked.");
                        
                        await new Promise(r => setTimeout(r, 500));
                        
                        const selected = doc.defaultView.selectedEl;
                        if (!selected) log("selectedEl is null!");
                        else log("selectedEl is: " + selected.tagName);
                        
                        // Change color via input
                        const inputColor = doc.getElementById('input-color');
                        inputColor.value = '#ff0000';
                        inputColor.dispatchEvent(new Event('input'));
                        
                        await new Promise(r => setTimeout(r, 500));
                        log("Color changed. Element text is now: " + h1.innerText);
                        log("Element color style is: " + h1.style.color);
                        
                        // Verify builder history
                        log("History pointer: " + window.historyPointer);
                        log("History length: " + window.builderHistory.length);
                        
                        // Try Undo
                        window.undoBuilder();
                        await new Promise(r => setTimeout(r, 500));
                        
                        const iframe2 = document.getElementById('cv-preview-frame');
                        const doc2 = iframe2.contentDocument;
                        const h1_after = doc2.querySelector('h1, h2, p, span');
                        log("After undo, element text: " + h1_after.innerText);
                        
                        return logs.join('\\n');
                    } catch(e) {
                        return "TEST ERROR: " + e.stack;
                    }
                })();
            `);
            
            console.log(result);
            require('fs').writeFileSync(path.join(__dirname, 'test_output.txt'), result);
            app.quit();
        } catch(e) {
            console.error(e);
            app.quit();
        }
    });
});
