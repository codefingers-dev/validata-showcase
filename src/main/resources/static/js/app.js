/**
 * FraudLens - Minimal Frontend
 */

// Elements
const uploadArea = document.getElementById('uploadArea');
const fileInput = document.getElementById('fileInput');
const fileSelected = document.getElementById('fileSelected');
const fileName = document.getElementById('fileName');
const fileSize = document.getElementById('fileSize');
const btnRemove = document.getElementById('btnRemove');
const btnAnalyze = document.getElementById('btnAnalyze');
const uploadSection = document.getElementById('uploadSection');
const resultsSection = document.getElementById('resultsSection');
const status = document.getElementById('status');

// State
let selectedFile = null;

// ===== Upload Handlers =====

uploadArea.addEventListener('click', () => fileInput.click());

fileInput.addEventListener('change', e => {
    if (e.target.files[0]) selectFile(e.target.files[0]);
});

uploadArea.addEventListener('dragover', e => {
    e.preventDefault();
    uploadArea.classList.add('dragover');
});

uploadArea.addEventListener('dragleave', () => {
    uploadArea.classList.remove('dragover');
});

uploadArea.addEventListener('drop', e => {
    e.preventDefault();
    uploadArea.classList.remove('dragover');
    if (e.dataTransfer.files[0]) selectFile(e.dataTransfer.files[0]);
});

btnRemove.addEventListener('click', clearFile);
btnAnalyze.addEventListener('click', analyze);

// ===== Functions =====

function selectFile(file) {
    const valid = ['application/pdf', 'image/png', 'image/jpeg'];
    if (!valid.includes(file.type)) {
        alert('Bitte PDF oder Bild wählen');
        return;
    }
    
    selectedFile = file;
    fileName.textContent = file.name;
    fileSize.textContent = formatSize(file.size);
    
    uploadArea.style.display = 'none';
    fileSelected.style.display = 'flex';
    btnAnalyze.disabled = false;
}

function clearFile() {
    selectedFile = null;
    fileInput.value = '';
    uploadArea.style.display = 'block';
    fileSelected.style.display = 'none';
    btnAnalyze.disabled = true;
}

async function analyze() {
    if (!selectedFile) return;
    
    btnAnalyze.classList.add('loading');
    btnAnalyze.disabled = true;
    
    try {
        const form = new FormData();
        form.append('file', selectedFile);
        
        const res = await fetch('/api/v1/invoices/analyze?includeExtractedData=true', {
            method: 'POST',
            body: form
        });
        
        if (!res.ok) throw new Error('Analyse fehlgeschlagen');
        
        const data = await res.json();
        showResults(data);
        
    } catch (err) {
        alert(err.message);
    } finally {
        btnAnalyze.classList.remove('loading');
        btnAnalyze.disabled = false;
    }
}

function showResults(data) {
    uploadSection.style.display = 'none';
    resultsSection.style.display = 'block';
    
    // Score
    const score = data.riskScore || 0;
    animateScore(score);
    
    // Level
    const level = document.getElementById('scoreLevel');
    level.textContent = data.riskLevel || 'LOW';
    level.className = 'score-level ' + (data.riskLevel || 'LOW');
    
    // Recommendation
    const recMap = {
        'APPROVE': 'Freigabe empfohlen',
        'REVIEW': 'Prüfung empfohlen',
        'REJECT': 'Ablehnung empfohlen'
    };
    document.getElementById('scoreRecommendation').textContent = 
        recMap[data.recommendation] || data.recommendation;
    
    // Summary
    document.getElementById('summaryText').textContent = 
        data.summary || 'Keine Zusammenfassung verfügbar.';
    
    // Flags
    renderFlags(data.redFlags || []);
    
    // Validation
    renderValidation(data.validation);
    
    // Data
    renderData(data.extractedData);
    
    // Meta
    renderMeta(data.metadata);
}

function animateScore(score) {
    const el = document.getElementById('scoreValue');
    const ring = document.getElementById('scoreProgress');
    
    // Animate number
    let current = 0;
    const step = Math.max(1, Math.floor(score / 30));
    const timer = setInterval(() => {
        current += step;
        if (current >= score) {
            current = score;
            clearInterval(timer);
        }
        el.textContent = current;
    }, 30);
    
    // Animate ring
    const offset = 283 - (score / 100 * 283);
    ring.style.strokeDashoffset = offset;
    
    // Color
    if (score <= 25) ring.style.stroke = '#34c759';
    else if (score <= 50) ring.style.stroke = '#ff9500';
    else ring.style.stroke = '#ff3b30';
}

function renderFlags(flags) {
    const list = document.getElementById('flagsList');
    const count = document.getElementById('flagsCount');
    count.textContent = flags.length;
    
    if (!flags.length) {
        list.innerHTML = '<div class="empty-flags">Keine Auffälligkeiten</div>';
        return;
    }
    
    const icons = { AMOUNT: '€', FORMAL: '§', CONTENT: '¶', DOCUMENT: '◊' };
    
    list.innerHTML = flags.map(f => `
        <div class="flag-item ${f.severity}">
            <span class="flag-icon">${icons[f.category] || '•'}</span>
            <div class="flag-body">
                <div class="flag-title">${f.code}</div>
                <div class="flag-desc">${f.description}</div>
                ${f.evidence ? `<div class="flag-evidence">${f.evidence}</div>` : ''}
            </div>
            <span class="flag-score">+${f.scoreImpact}</span>
        </div>
    `).join('');
}

function renderValidation(v) {
    const list = document.getElementById('validationList');
    if (!v) { list.innerHTML = ''; return; }
    
    const items = [
        ['taxNumberValid', 'Steuernummer'],
        ['vatIdValid', 'USt-IdNr'],
        ['vatCalculationCorrect', 'MwSt-Berechnung'],
        ['sumCalculationCorrect', 'Summen'],
        ['licensePlateValid', 'Kennzeichen']
    ];
    
    list.innerHTML = items.map(([key, label]) => {
        const ok = v[key];
        return `
            <div class="check-item">
                <span class="check-icon ${ok ? 'valid' : 'invalid'}">${ok ? '✓' : '×'}</span>
                <span>${label}</span>
            </div>
        `;
    }).join('');
}

function renderData(d) {
    const list = document.getElementById('dataList');
    if (!d) { list.innerHTML = ''; return; }
    
    const items = [
        ['workshopName', 'Werkstatt'],
        ['invoiceNumber', 'Rechnungsnr.'],
        ['invoiceDate', 'Datum'],
        ['licensePlate', 'Kennzeichen'],
        ['grossAmount', 'Brutto', ' €']
    ];
    
    list.innerHTML = items
        .filter(([k]) => d[k])
        .map(([k, l, s]) => `
            <div class="data-item">
                <span class="data-label">${l}</span>
                <span class="data-value">${d[k]}${s || ''}</span>
            </div>
        `).join('');
}

function renderMeta(m) {
    const bar = document.getElementById('metaBar');
    if (!m) { bar.innerHTML = ''; return; }
    
    const items = [];
    if (m.processingTimeMs) items.push(`${m.processingTimeMs}ms`);
    if (m.modelUsed) items.push(m.modelUsed.includes('mock') ? 'Mock' : 'Claude');
    if (m.analysisMode) items.push(m.analysisMode);
    
    bar.innerHTML = items.map(i => `<span class="meta-item">${i}</span>`).join('');
    
    // Update status
    if (m.modelUsed?.includes('mock')) {
        status.innerHTML = '<span class="status-dot"></span>Local';
    } else {
        status.innerHTML = '<span class="status-dot" style="background:#0071e3"></span>AWS';
    }
}

function formatSize(bytes) {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
}

console.log('FraudLens ready');
