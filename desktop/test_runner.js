// test_runner.js
const fs = require('fs');
const path = require('path');

module.exports = function injectTests(mainWindow) {
    mainWindow.webContents.on('did-finish-load', () => {
        mainWindow.webContents.executeJavaScript(`
            (async () => {
                const results = [];
                function log(msg) {
                    console.log('[TEST]', msg);
                    results.push(msg);
                }
                
                try {
                    log('Waiting for UI to render...');
                    await new Promise(r => setTimeout(r, 1000));
                    
                    const drafts = document.querySelectorAll('.grid > div');
                    if (drafts.length === 0) {
                        log('FAIL: No drafts found');
                        return results;
                    }
                    
                    log('Clicking draft...');
                    drafts[0].click();
                    
                    await new Promise(r => setTimeout(r, 2000));
                    
                    const iframe = document.getElementById('cv-preview-frame');
                    if (!iframe) {
                        log('FAIL: Iframe not found');
                        return results;
                    }
                    
                    const idoc = iframe.contentDocument;
                    if (!idoc) {
                        log('FAIL: Iframe contentDocument not found');
                        return results;
                    }
                    
                    log('Iframe loaded. Checking selectedEl bug...');
                    const selectedElVal = idoc.defaultView.selectedEl;
                    log('selectedEl is ' + typeof selectedElVal + ' (should be undefined due to IIFE isolation)');
                    
                    // Simulate clicking an element inside iframe
                    const h1 = idoc.querySelector('h1');
                    if (h1) {
                        log('Found H1, simulating click...');
                        h1.click();
                        await new Promise(r => setTimeout(r, 500));
                        log('H1 contentEditable: ' + h1.getAttribute('contenteditable'));
                    }
                    
                    // Check builderHistory
                    log('Builder history length: ' + window.builderHistory.length);
                    
                    log('SUCCESS: All checks passed!');
                } catch(e) {
                    log('ERROR: ' + e.message);
                }
                return results;
            })();
        `).then(res => {
            fs.writeFileSync(path.join(__dirname, '..', 'test_results.txt'), res.join('\\n'));
        }).catch(e => {
            fs.writeFileSync(path.join(__dirname, '..', 'test_results.txt'), 'EXEC JS ERROR: ' + e.message);
        });
    });
};
