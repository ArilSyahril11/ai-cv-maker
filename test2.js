const scriptContent = `
                    let selectedEl = null;
                    const toolbar = document.getElementById('wysiwyg-toolbar');
                    const resizeBox = document.getElementById('cv-resize-box');
                    
                    const btnEditText = document.getElementById('btn-edit-text');
                    const inputColor = document.getElementById('input-color');
                    const inputBg = document.getElementById('input-bg');
                    const selectFontSize = document.getElementById('select-font-size');
                    const btnBold = document.getElementById('btn-bold');
                    const btnItalic = document.getElementById('btn-italic');
                    const btnAlignL = document.getElementById('btn-align-l');
                    const btnAlignC = document.getElementById('btn-align-c');
                    const btnAlignR = document.getElementById('btn-align-r');
                    const btnDelete = document.getElementById('btn-delete');
                    const dragHandle = document.getElementById('drag-handle');
                    
                    let autoSaveTimeout = null;
                    function triggerAutoSave() {
                        clearTimeout(autoSaveTimeout);
                        autoSaveTimeout = setTimeout(() => {
                            parent.saveHistoryState(document);
                        }, 500);
                    }
                    
                    function updateOverlays() {
                        if (!selectedEl) return;
                        const rect = selectedEl.getBoundingClientRect();
                        
                        // Toolbar
                        toolbar.style.display = 'flex';
                        toolbar.style.top = Math.max(0, rect.top + window.scrollY - toolbar.offsetHeight - 5) + 'px';
                        toolbar.style.left = (rect.left + window.scrollX) + 'px';
                        
                        // Resize Box
                        resizeBox.style.display = 'block';
                        resizeBox.style.top = (rect.top + window.scrollY) + 'px';
                        resizeBox.style.left = (rect.left + window.scrollX) + 'px';
                        resizeBox.style.width = rect.width + 'px';
                        resizeBox.style.height = rect.height + 'px';
                    }

                    function cleanupEl(el) {
                        if(!el) return;
                        el.classList.remove('cv-builder-hover', 'cv-builder-selected');
                        el.removeAttribute('contenteditable');
                    }

                    document.addEventListener('mouseover', e => {
                        if (e.target === toolbar || toolbar.contains(e.target) || resizeBox.contains(e.target)) return;
                        if (selectedEl !== e.target && e.target.classList && e.target.tagName !== 'HTML' && e.target.tagName !== 'BODY') {
                            e.target.classList.add('cv-builder-hover');
                        }
                    });

                    document.addEventListener('mouseout', e => {
                        if (e.target.classList) {
                            e.target.classList.remove('cv-builder-hover');
                        }
                    });

                    document.addEventListener('click', e => {
                        if (e.target === toolbar || toolbar.contains(e.target) || resizeBox.contains(e.target)) return;
                        e.preventDefault();
                        e.stopPropagation();
                        
                        if (selectedEl) {
                            cleanupEl(selectedEl);
                        }
                        
                        if (e.target.tagName === 'HTML' || e.target.tagName === 'BODY' || e.target.classList.contains('cv-a4-page-wrap')) {
                            toolbar.style.display = 'none';
                            resizeBox.style.display = 'none';
                            selectedEl = null;
                            return;
                        }

                        selectedEl = e.target;
                        selectedEl.classList.remove('cv-builder-hover');
                        selectedEl.classList.add('cv-builder-selected');

                        updateOverlays();
                        
                        // Sync controls with current styles
                        const style = window.getComputedStyle(selectedEl);
                        function rgb2hex(rgb) {
                            if (!rgb || rgb === 'rgba(0, 0, 0, 0)' || rgb === 'transparent') return '#ffffff';
                            const match = rgb.match(/^rgba?\\((\\d+),\\s*(\\d+),\\s*(\\d+)/);
                            if (!match) return '#000000';
                            function hex(x) { return ("0" + parseInt(x).toString(16)).slice(-2); }
                            return "#" + hex(match[1]) + hex(match[2]) + hex(match[3]);
                        }
                        
                        inputColor.value = rgb2hex(style.color);
                        inputBg.value = rgb2hex(style.backgroundColor);
                        selectFontSize.value = style.fontSize;
                    });
                    
                    // Toolbar actions
                    btnEditText.onclick = () => {
                        if (!selectedEl) return;
                        selectedEl.contentEditable = "true";
                        selectedEl.focus();
                        toolbar.style.display = 'none';
                        resizeBox.style.display = 'none';
                    };
                    
                    document.addEventListener('focusout', e => {
                        if (e.target.getAttribute('contenteditable') === 'true') {
                            e.target.removeAttribute('contenteditable');
                            triggerAutoSave();
                            updateOverlays();
                        }
                    });

                    inputColor.oninput = e => { if(selectedEl) { selectedEl.style.color = e.target.value; triggerAutoSave(); } };
                    inputBg.oninput = e => { if(selectedEl) { selectedEl.style.backgroundColor = e.target.value; triggerAutoSave(); } };
                    selectFontSize.onchange = e => { if(selectedEl) { selectedEl.style.fontSize = e.target.value; updateOverlays(); triggerAutoSave(); } };
                    
                    btnBold.onclick = () => { if(selectedEl) { selectedEl.style.fontWeight = selectedEl.style.fontWeight === 'bold' || parseInt(window.getComputedStyle(selectedEl).fontWeight) >= 700 ? 'normal' : 'bold'; updateOverlays(); triggerAutoSave(); } };
                    btnItalic.onclick = () => { if(selectedEl) { selectedEl.style.fontStyle = selectedEl.style.fontStyle === 'italic' ? 'normal' : 'italic'; updateOverlays(); triggerAutoSave(); } };
                    btnAlignL.onclick = () => { if(selectedEl) { selectedEl.style.textAlign = 'left'; updateOverlays(); triggerAutoSave(); } };
                    btnAlignC.onclick = () => { if(selectedEl) { selectedEl.style.textAlign = 'center'; updateOverlays(); triggerAutoSave(); } };
                    btnAlignR.onclick = () => { if(selectedEl) { selectedEl.style.textAlign = 'right'; updateOverlays(); triggerAutoSave(); } };
                    btnDelete.onclick = () => { 
                        if(selectedEl) { 
                            selectedEl.remove(); 
                            toolbar.style.display = 'none'; 
                            resizeBox.style.display = 'none';
                            selectedEl = null; 
                            triggerAutoSave(); 
                        } 
                    };
                    
                    // --- DRAG LOGIC ---
                    let isDragging = false;
                    let dragStartX, dragStartY, initialTx = 0, initialTy = 0;
                    
                    dragHandle.onmousedown = e => {
                        if (!selectedEl) return;
                        isDragging = true;
                        dragStartX = e.clientX;
                        dragStartY = e.clientY;
                        
                        const transform = window.getComputedStyle(selectedEl).transform;
                        if (transform && transform !== 'none') {
                            const matrix = new DOMMatrix(transform);
                            initialTx = matrix.m41;
                            initialTy = matrix.m42;
                        } else {
                            initialTx = 0; initialTy = 0;
                        }
                        e.preventDefault();
                    };
                    
                    // --- RESIZE LOGIC ---
                    let isResizing = false;
                    let resizeDir = '';
                    let startW, startH, startX, startY;
                    
                    resizeBox.addEventListener('mousedown', e => {
                        if (e.target.classList.contains('cv-resize-handle')) {
                            isResizing = true;
                            resizeDir = e.target.dataset.dir;
                            startX = e.clientX;
                            startY = e.clientY;
                            const rect = selectedEl.getBoundingClientRect();
                            startW = rect.width;
                            startH = rect.height;
                            e.preventDefault();
                        }
                    });

                    document.addEventListener('mousemove', e => {
                        if (isDragging && selectedEl) {
                            const dx = e.clientX - dragStartX;
                            const dy = e.clientY - dragStartY;
                            selectedEl.style.transform = \`translate(\${initialTx + dx}px, \${initialTy + dy}px)\`;
                            updateOverlays();
                        }
                        if (isResizing && selectedEl) {
                            const dx = e.clientX - startX;
                            const dy = e.clientY - startY;
                            
                            // Prevent inline elements from ignoring width/height
                            if (window.getComputedStyle(selectedEl).display === 'inline') {
                                selectedEl.style.display = 'inline-block';
                            }
                            
                            let newW = startW;
                            let newH = startH;
                            
                            if (resizeDir.includes('e')) newW = startW + dx;
                            if (resizeDir.includes('w')) newW = startW - dx;
                            if (resizeDir.includes('s')) newH = startH + dy;
                            if (resizeDir.includes('n')) newH = startH - dy;
                            
                            if (newW > 10) selectedEl.style.width = newW + 'px';
                            if (newH > 10) selectedEl.style.height = newH + 'px';
                            
                            updateOverlays();
                        }
                    });
                    
                    document.addEventListener('mouseup', () => {
                        if (isDragging || isResizing) {
                            isDragging = false;
                            isResizing = false;
                            triggerAutoSave();
                        }
                    });

                    document.addEventListener('keydown', e => {
                        if (e.ctrlKey && (e.key === 'z' || e.key === 'Z')) {
                            if (e.shiftKey) parent.redoBuilder();
                            else parent.undoBuilder();
                            e.preventDefault();
                        }
                        if (e.ctrlKey && (e.key === 'y' || e.key === 'Y')) {
                            parent.redoBuilder();
                            e.preventDefault();
                        }
                    });
`;
try {
  new Function(scriptContent);
  console.log("Syntax OK");
} catch (e) {
  console.error("Syntax Error:", e);
}
