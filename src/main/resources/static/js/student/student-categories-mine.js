const token = localStorage.getItem('token') || sessionStorage.getItem('token');
let TREE_DATA = [];
let ALL_QUIZZES = [];
let ALL_QUESTIONS = [];
let currentFolderId = null;
let currentContext = 'quiz';
let expandedFolders = new Set();
// Question bank state
let qbPage = 0, qbSize = 10, qbTotal = 0;
let qbKeyword = '', qbType = '', qbLevel = '';

const COVERS = [
    '/images/covers/cover1.png',
    '/images/covers/cover2.png',
    '/images/covers/cover3.png'
];

document.addEventListener('DOMContentLoaded', async () => {
    await loadData();
    renderSidebarTree();
    loadFolder(null);
});

async function loadData() {
    try {
        const [resCat, resQuiz, resQues] = await Promise.all([
            fetch('/api/student/categories/mine', { headers: { Authorization: 'Bearer ' + token } }),
            fetch('/api/student/quiz/mine', { headers: { Authorization: 'Bearer ' + token } }),
            fetch('/api/student/questions?size=500', { headers: { Authorization: 'Bearer ' + token } })
        ]);
        TREE_DATA = await resCat.json();
        window.myCategories = TREE_DATA;
        ALL_QUIZZES = await resQuiz.json();
        const qData = await resQues.json();
        ALL_QUESTIONS = qData.content || [];
    } catch (e) { console.error("Lỗi khi tải dữ liệu:", e); }
}

function renderSidebarTree(nodes = TREE_DATA, container = document.getElementById('treeRoot'), level = 0) {
    if (level === 0) {
        container.innerHTML = '';
        // Handle active state for Unassigned node
        const unassignedNode = document.getElementById('folder--1');
        if (unassignedNode) {
            if (currentFolderId === -1) {
                unassignedNode.style.background = '#eef2ff';
                unassignedNode.style.color = '#4f46e5';
                unassignedNode.style.fontWeight = '700';
            } else {
                unassignedNode.style.background = 'transparent';
                unassignedNode.style.color = '#475569';
                unassignedNode.style.fontWeight = '500';
            }
        }
        // Add "All"
        const allRow = document.createElement('div');
        allRow.className = `tree-node ${currentFolderId === null ? 'active' : ''}`;
        allRow.innerHTML = `<i class="bi bi-collection-fill folder-icon" style="color: #4f46e5;"></i><span class="fw-bold">Tất cả thư mục</span>`;
        allRow.onclick = () => loadFolder(null);
        container.appendChild(allRow);
        
        if (!nodes || nodes.length === 0) return;
    }

    nodes.forEach(n => {
        const isExpanded = expandedFolders.has(n.id);
        const hasChildren = n.children && n.children.length > 0;

        const row = document.createElement('div');
        row.className = `tree-node ${currentFolderId === n.id ? 'active' : ''} ${isExpanded ? 'expanded' : ''}`;
        row.style.paddingLeft = (level * 20 + 12) + 'px';
        
        row.innerHTML = `
            <i class="bi bi-chevron-right chevron-icon ${hasChildren ? '' : 'opacity-0'}"></i>
            <i class="bi ${isExpanded ? 'bi-folder2-open' : 'bi-folder-fill'} folder-icon"></i>
            <span class="text-truncate">${esc(n.name)}</span>
            <div class="btn-add-sub" onclick="event.stopPropagation(); openCreateModal(${n.id})">
                <i class="bi bi-plus"></i>
            </div>
        `;

        row.onclick = (e) => {
            if (e.target.closest('.chevron-icon') || e.target.closest('.folder-icon')) {
                if (hasChildren) {
                    if (isExpanded) expandedFolders.delete(n.id);
                    else expandedFolders.add(n.id);
                    renderSidebarTree();
                }
            }
            loadFolder(n.id);
        };

        container.appendChild(row);

        if (hasChildren && isExpanded) {
            renderSidebarTree(n.children, container, level + 1);
        }
    });
}

window.loadFolder = function(id) {
    currentFolderId = id;
    const folder = id ? findCategory(TREE_DATA, id) : null;

    
    // Update active state in sidebar
    renderSidebarTree();

    document.getElementById('currentName').textContent = folder ? folder.name : (id === -1 ? 'Chưa phân loại' : 'Thư viện của tôi');
    
    const pathArr = folder ? getCategoryPath(TREE_DATA, id) : [];
    const segments = [{ name: 'LIBRARY', id: null }];
    pathArr.forEach(p => {
        segments.push({ name: p.name.toUpperCase(), id: p.id });
    });

    const pathHtml = segments.map((seg, i) => {
        if (i === segments.length - 1) {
            return `<span>${esc(seg.name)}</span>`;
        } else {
            return `<span class="bc-link" onclick="loadFolder(${seg.id === null ? 'null' : seg.id})">${esc(seg.name)}</span>`;
        }
    }).join('<i class="bi bi-chevron-right" style="font-size: 0.6rem;"></i>');

    document.getElementById('pathText').innerHTML = pathHtml;
    
    document.getElementById('btnEditCat').style.display = id && id !== -1 ? 'block' : 'none';
    document.getElementById('btnDeleteCat').style.display = id && id !== -1 ? 'block' : 'none';
    
    // Sync action button
    const actionBtn = document.getElementById('mainActionBtn');
    if (currentContext === 'quiz') {
        actionBtn.innerHTML = `
            <div class="dropdown d-inline-block">
                <button class="btn-premium dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false" style="border: none;">
                    <i class="bi bi-plus-lg me-1"></i> Tạo đề thi
                </button>
                <ul class="dropdown-menu dropdown-menu-end shadow border-0 rounded-4 mt-2 p-2" style="width: 240px; z-index: 1050;">
                    <li>
                        <a class="dropdown-item p-2 rounded-3 d-flex align-items-center gap-2 mb-1" href="/student/quiz/create${id && id !== -1 ? '?categoryId=' + id : ''}">
                            <i class="bi bi-pencil-square text-primary" style="font-size: 1.1rem;"></i>
                            <span class="fw-bold text-dark small">Soạn thủ công</span>
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item p-2 rounded-3 d-flex align-items-center gap-2" href="/student/quiz/quick-create">
                            <i class="bi bi-magic text-success" style="font-size: 1.1rem;"></i>
                            <span class="fw-bold text-dark small">Tạo nhanh (Excel/JSON)</span>
                        </a>
                    </li>
                </ul>
            </div>
        `;
    } else {
        actionBtn.innerHTML = `
            <button class="btn-premium btn-accent" onclick="openQuestionEditor()" style="border: none;">
                <i class="bi bi-plus-lg me-1"></i> Tạo câu hỏi
            </button>
        `;
    }

    initActionBtnPopovers();
    refreshContent();
}

window.switchContext = function(ctx) {
    currentContext = ctx;
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.getElementById(`tab-${ctx}`).classList.add('active');
    
    // Dynamic Action Button
    const actionBtn = document.getElementById('mainActionBtn');
    if (ctx === 'quiz') {
        actionBtn.innerHTML = `
            <div class="dropdown d-inline-block">
                <button class="btn-premium dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false" style="border: none;">
                    <i class="bi bi-plus-lg me-1"></i> Tạo đề thi
                </button>
                <ul class="dropdown-menu dropdown-menu-end shadow border-0 rounded-4 mt-2 p-2" style="width: 240px; z-index: 1050;">
                    <li>
                        <a class="dropdown-item p-2 rounded-3 d-flex align-items-center gap-2 mb-1" href="/student/quiz/create${currentFolderId && currentFolderId !== -1 ? '?categoryId=' + currentFolderId : ''}">
                            <i class="bi bi-pencil-square text-primary" style="font-size: 1.1rem;"></i>
                            <span class="fw-bold text-dark small">Soạn thủ công</span>
                        </a>
                    </li>
                    <li>
                        <a class="dropdown-item p-2 rounded-3 d-flex align-items-center gap-2" href="/student/quiz/quick-create">
                            <i class="bi bi-magic text-success" style="font-size: 1.1rem;"></i>
                            <span class="fw-bold text-dark small">Tạo nhanh (Excel/JSON)</span>
                        </a>
                    </li>
                </ul>
            </div>
        `;
    } else {
        actionBtn.innerHTML = `
            <button class="btn-premium btn-accent" onclick="openQuestionEditor()" style="border: none;">
                <i class="bi bi-plus-lg me-1"></i> Tạo câu hỏi
            </button>
        `;
    }
    
    initActionBtnPopovers();
    refreshContent();
}

function getDescendantIds(folderId) {
    const folder = findCategory(TREE_DATA, folderId);
    if (!folder) return [folderId];
    const ids = [folderId];
    const collect = (nodes) => {
        nodes.forEach(n => {
            ids.push(n.id);
            if (n.children) collect(n.children);
        });
    }
    if (folder.children) collect(folder.children);
    return ids;
}

function refreshContent() {
    let quizList = ALL_QUIZZES.filter(q => {
        if (currentFolderId === null) return true;
        if (currentFolderId === -1) return !q.categoryId;
        const descendants = getDescendantIds(currentFolderId);
        return descendants.includes(q.categoryId);
    });
    document.getElementById('statQuizCount').textContent = quizList.length;

    if (currentContext === 'quiz') {
        renderQuizGrid(quizList);
    } else {
        qbPage = 0;
        renderQBankShell();
        fetchQBank();
    }
}

// ── Render vỏ Question Bank (filter bar + container) ──
function renderQBankShell() {
    const container = document.getElementById('contentList');
    if (document.getElementById('qbFilterShell')) return; // Already rendered

    container.innerHTML = `
        <div class="fade-in">
            <div class="qb-filter-shell" id="qbFilterShell">
                <div class="fg">
                    <label><i class="bi bi-search me-1"></i> Tìm kiếm</label>
                    <input type="text" id="qbKeyword" class="form-control" placeholder="Tìm câu hỏi..." oninput="qbSearch()" autocomplete="off" value="${esc(qbKeyword)}">
                </div>
                <div class="fg">
                    <label><i class="bi bi-tags-fill me-1"></i> Loại</label>
                    <select id="qbType" class="qb-ctrl" onchange="qbSearch()">
                        <option value="">Tất cả</option>
                        <option value="SINGLE_CHOICE" ${qbType==='SINGLE_CHOICE'?'selected':''}>Một đáp án</option>
                        <option value="MULTIPLE_CHOICE" ${qbType==='MULTIPLE_CHOICE'?'selected':''}>Nhiều đáp án</option>
                        <option value="FILL_IN_BLANK" ${qbType==='FILL_IN_BLANK'?'selected':''}>Điền khuyết</option>
                    </select>
                </div>
                <div class="fg">
                    <label><i class="bi bi-bar-chart-fill me-1"></i> Độ khó</label>
                    <select id="qbLevel" class="qb-ctrl" onchange="qbSearch()">
                        <option value="">Tất cả</option>
                        <option value="EASY" ${qbLevel==='EASY'?'selected':''}>Dễ</option>
                        <option value="MEDIUM" ${qbLevel==='MEDIUM'?'selected':''}>Trung bình</option>
                        <option value="HARD" ${qbLevel==='HARD'?'selected':''}>Khó</option>
                    </select>
                </div>
            </div>
            <div id="qbList"></div>
            <div id="qbPagination" class="qb-pagination"></div>
        </div>
    `;
}

let qbDebounce = null;
window.qbSearch = function() {
    clearTimeout(qbDebounce);
    qbDebounce = setTimeout(() => {
        qbKeyword = document.getElementById('qbKeyword')?.value.trim() || '';
        qbType    = document.getElementById('qbType')?.value || '';
        qbLevel   = document.getElementById('qbLevel')?.value || '';
        qbPage = 0;
        fetchQBank();
    }, 350);
}

async function fetchQBank() {
    const listContainer = document.getElementById('qbList');
    if (!listContainer) return;

    listContainer.innerHTML = `
        <div class="text-center py-5 fade-in">
            <div class="spinner-border text-primary" role="status"></div>
            <p class="mt-2 text-muted small">Đang tải câu hỏi...</p>
        </div>
    `;

    let url = `/api/student/questions?page=${qbPage}&size=${qbSize}&sortBy=id&sortDir=desc`;
    if (currentFolderId === -1) url += `&categoryId=-1`;
    else if (currentFolderId) url += `&categoryId=${currentFolderId}`;
    
    if (qbKeyword) url += `&keyword=${encodeURIComponent(qbKeyword)}`;
    if (qbType)    url += `&type=${qbType}`;
    if (qbLevel)   url += `&level=${qbLevel}`;

    try {
        const res = await fetch(url, { headers: { Authorization: 'Bearer ' + token } });
        const data = await res.json();
        qbTotal = data.totalElements;
        document.getElementById('statQuestionCount').textContent = qbTotal;
        renderQuestionList(data.content);
        renderQBankPagination(data);
    } catch (e) {
        listContainer.innerHTML = `<div class="empty-state text-danger"><i class="bi bi-exclamation-circle"></i><h3>Lỗi tải dữ liệu</h3></div>`;
    }
}

function renderQuestionList(items) {
    const list = document.getElementById('qbList');
    if (!list) return;

    if (!items || items.length === 0) {
        list.innerHTML = `<div class="empty-state fade-in"><i class="bi bi-patch-question"></i><h3>Chưa có câu hỏi nào</h3></div>`;
        return;
    }

    const typeLbl = { SINGLE_CHOICE: 'Một đáp án', MULTIPLE_CHOICE: 'Nhiều đáp án', FILL_IN_BLANK: 'Điền khuyết' };
    const levelCls = { EASY: 'qb-level-easy', MEDIUM: 'qb-level-medium', HARD: 'qb-level-hard' };
    const levelLbl = { EASY: 'Dễ', MEDIUM: 'Trung bình', HARD: 'Khó' };

    list.innerHTML = items.map(q => {
        const ansHtml = (q.answers && q.answers.length > 0)
            ? q.answers.map((a, i) => `<div class="q-ans-item ${a.isCorrect ? 'correct' : ''}">
                    <i class="bi bi-${a.isCorrect ? 'check-circle-fill' : 'circle'}"></i>
                    <span>${String.fromCharCode(65+i)}. ${esc(a.text)}</span>
                </div>`).join('')
            : '<div class="q-ans-item text-muted"><i class="bi bi-dash"></i><span>Chưa có đáp án</span></div>';

        return `<div class="q-card fade-in">
            <div class="q-card-badges">
                <div class="q-badge-row">
                    <span class="qb-badge qb-type">${typeLbl[q.type] || q.type}</span>
                    <span class="qb-badge ${levelCls[q.level] || 'qb-level-medium'}">${levelLbl[q.level] || q.level}</span>
                    ${currentFolderId === null ? `<span class="qb-badge qb-cat"><i class="bi bi-folder me-1"></i>${esc(q.categoryName || 'Chưa phân mục')}</span>` : ''}
                </div>
                <span class="q-card-id">#${q.id}</span>
            </div>
            <div class="q-card-text">${esc(q.text)}</div>
            <div class="q-answers">${ansHtml}</div>
            <div class="q-card-actions">
                <button class="qb-btn qb-btn-edit" onclick="editQuestion(${q.id})"><i class="bi bi-pencil-fill"></i> Sửa</button>
                <button class="qb-btn qb-btn-del" onclick="deleteQuestion('${q.id}')"><i class="bi bi-trash-fill"></i> Xóa</button>
            </div>
        </div>`;
    }).join('');
}

function renderQBankPagination(data) {
    const container = document.getElementById('qbPagination');
    if (!container) return;
    if (data.totalPages <= 1) { container.style.display = 'none'; return; }

    container.style.display = 'flex';
    
    let pagesHtml = '';
    const current = data.number;
    const total = data.totalPages;
    
    // Previous Button
    pagesHtml += `<div class="page-link-up ${data.first ? 'disabled' : ''}" onclick="${data.first ? '' : `changeQBPage(${current - 1})`}"><i class="bi bi-chevron-left"></i></div>`;
    
    // Page Numbers (Show max 5 pages with ellipsis logic)
    let start = Math.max(0, current - 2);
    let end = Math.min(total - 1, start + 4);
    if (end - start < 4) start = Math.max(0, end - 4);
    
    for (let i = start; i <= end; i++) {
        pagesHtml += `<div class="page-link-up ${i === current ? 'active' : ''}" onclick="changeQBPage(${i})">${i + 1}</div>`;
    }
    
    // Next Button
    pagesHtml += `<div class="page-link-up ${data.last ? 'disabled' : ''}" onclick="${data.last ? '' : `changeQBPage(${current + 1})`}"><i class="bi bi-chevron-right"></i></div>`;

    container.innerHTML = `
        <div class="text-muted small fw-bold">
            <span class="text-dark">Trang ${current + 1}</span> / ${total} 
            <span class="mx-2 text-slate-300">•</span> 
            ${data.totalElements} câu hỏi
        </div>
        <div class="d-flex gap-2">
            ${pagesHtml}
        </div>
    `;
}

window.changeQBPage = function(p) { qbPage = p; fetchQBank(); }

window.loadQuestions = async function() {
    renderQBankShell();
    await fetchQBank();
}

window.qbGoPage = function(p) { qbPage = p; fetchQBank(); }

function getQuizCover(quizId) {
    const index = (quizId.toString().charCodeAt(0) + quizId.toString().length) % COVERS.length;
    return COVERS[index];
}

function renderQuizGrid(list) {
    const container = document.getElementById('contentList');
    if (list.length === 0) { 
        container.innerHTML = `
            <div class="empty-state fade-in">
                <i class="bi bi-journal-x"></i>
                <h3>Thư mục này chưa có đề thi</h3>
                <p>Hãy bắt đầu bằng cách tạo một đề thi mới hoặc di chuyển đề thi vào đây.</p>
                <a href="/student/quiz/create${currentFolderId ? '?categoryId=' + currentFolderId : ''}" class="btn btn-primary rounded-pill px-4 mt-3 fw-bold">Tạo đề ngay</a>
            </div>`; 
        return; 
    }

    container.innerHTML = `<div class="quiz-grid fade-in">` + list.map(q => `
        <div class="up-card">
            <div class="up-card-banner">
                <img src="${q.imageUrl || getQuizCover(q.id)}" 
                     onerror="this.src='${getQuizCover(q.id)}'"
                     style="width: 100%; height: 100%; object-fit: cover; position: absolute; inset: 0;">
                <div class="up-card-overlay"></div>
                <span class="up-badge ${q.isExam ? 'badge-exam' : 'badge-quiz'}">${q.isExam ? 'Đề thi' : 'Luyện tập'}</span>
            </div>
            <div class="up-card-body">
                <h3 class="up-q-title text-truncate" title="${esc(q.title)}">${esc(q.title)}</h3>
                <div class="up-q-meta">
                    <span><i class="bi bi-list-check"></i> ${q.questionCount} câu</span>
                    <span><i class="bi bi-clock"></i> ${q.durationInMins || 0} phút</span>
                    <span><i class="bi bi-tag"></i> ${esc(q.categoryName || 'Mặc định')}</span>
                </div>
            </div>
            <div class="up-card-actions">
                <button class="btn-play-up" onclick="startQuiz('${q.id}')">Bắt đầu</button>
                <a href="javascript:void(0)" onclick="openQuizPreviewModal('${q.id}', event)" class="btn-opt-up btn-opt-preview" title="Xem trước"><i class="bi bi-eye"></i></a>
                <a href="/student/quiz/${q.id}/edit" class="btn-opt-up btn-opt-edit" title="Sửa đề thi"><i class="bi bi-pencil"></i></a>
                <button class="btn-opt-up btn-opt-delete text-danger" title="Xóa đề thi" onclick="deleteQuiz('${q.id}')"><i class="bi bi-trash3"></i></button>
            </div>
        </div>
    `).join('') + `</div>`;
}

const TYPE_MAP = { 'SINGLE_CHOICE': 'Trắc nghiệm', 'MULTIPLE_CHOICE': 'Chọn nhiều', 'FILL_IN_BLANK': 'Điền khuyết', 'ESSAY': 'Tự luận' };
const LEVEL_MAP = { 'EASY': 'Dễ', 'MEDIUM': 'Trung bình', 'HARD': 'Khó' };


// Use functions from fragment but keep them compatible with this page's naming
window.openCreateModal = function(pId) {
    let pName = null;
    if (pId) {
        const folder = findCategory(TREE_DATA, pId);
        if (folder) pName = folder.name;
    }
    openCreateCategoryModal(pId, pName);
};

window.openEditModal = function() {
    const folder = findCategory(TREE_DATA, currentFolderId);
    if (!folder) return;
    
    // Pre-fill the modal fields directly
    document.getElementById('categoryCreateId').value = folder.id;
    document.getElementById('categoryCreateName').value = folder.name;
    document.getElementById('categoryCreateDesc').value = folder.description || '';
    
    const pId = folder.parentId;
    let pName = 'Mặc định: Danh mục gốc';
    if (pId) {
        const parent = findCategory(TREE_DATA, pId);
        if (parent) pName = parent.name;
    }
    
    const idInput = document.getElementById('categoryCreateParentId');
    const nameInput = document.getElementById('categoryCreateParentName');
    const disp = document.getElementById('categoryCreateParentNameDisplay');
    
    if (idInput) idInput.value = pId || '';
    if (nameInput) nameInput.value = pName;
    if (disp) {
        disp.textContent = pName;
        disp.classList.toggle('text-muted', !pId);
        disp.classList.toggle('text-dark', !!pId);
    }
    
    const titleEl = document.getElementById('categoryCreateTitle');
    if (titleEl) titleEl.innerHTML = '<i class="bi bi-pencil-square me-2"></i>Sửa danh mục';
    
    showModalStacked('categoryCreateModal');
};

// Note: the fragment's submitCategoryCreateForm handles both POST and PUT if ID is present
// and it calls fetchCategories() on success. We need to make sure fetchCategories is defined here.
window.fetchCategories = async function() {
    await loadData();
    renderSidebarTree();
    loadFolder(currentFolderId);
};

window.deleteFolder = function() {
    if (!currentFolderId || currentFolderId === -1) return;
    bootstrap.Modal.getOrCreateInstance(document.getElementById('delCatModal')).show();
}

window.confirmDelCat = async function() {
    if (!currentFolderId) return;
    try {
        const res = await fetch(`/api/student/categories/${currentFolderId}`, { 
            method: 'DELETE', 
            headers: { Authorization: 'Bearer ' + token } 
        });
        if (res.ok) {
            const modal = bootstrap.Modal.getInstance(document.getElementById('delCatModal'));
            if (modal) modal.hide();
            await loadData();
            loadFolder(null);
        } else {
            const e = await res.json();
            alert(e.message || 'Lỗi khi xóa');
        }
    } catch (e) { alert('Lỗi kết nối'); }
}

function findCategory(nodes, id) {
    for (const n of nodes) {
        if (n.id === id) return n;
        if (n.children) {
            const found = findCategory(n.children, id);
            if (found) return found;
        }
    }
    return null;
}

function getCategoryPath(nodes, id, path = []) {
    for (const n of nodes) {
        if (n.id === id) return [...path, n];
        if (n.children) {
            const found = getCategoryPath(n.children, id, [...path, n]);
            if (found.length > path.length + 1) return found;
        }
    }
    return path;
}

window.startQuiz = function(id) {
    fetch(`/api/student/quiz/start-personal?quizId=${id}`, { headers: { Authorization: 'Bearer ' + token } })
        .then(res => res.json())
        .then(data => window.location.href = `/student/quiz/play?attemptId=${data.attemptId}`);
}

window.deleteQuiz = async function(id) {
    if (!confirm('Xác nhận xóa đề thi này khỏi kho?')) return;
    try {
        const res = await fetch(`/api/student/quiz/${id}`, { method: 'DELETE', headers: { Authorization: 'Bearer ' + token } });
        if (res.ok) { await loadData(); refreshContent(); }
    } catch (e) { }
}

window.editQuestion = async function(id) {
    try {
        const res = await fetch(`/api/student/questions/${id}`, { headers: { Authorization: 'Bearer ' + token } });
        const q = await res.json();
        openQuestionEditor(q);
    } catch (e) { alert('Lỗi tải câu hỏi'); }
}

window.deleteQuestion = async function(id) {
    if (!confirm('Xác nhận xóa câu hỏi này?')) return;
    try {
        const res = await fetch(`/api/student/questions/${id}`, { method: 'DELETE', headers: { Authorization: 'Bearer ' + token } });
        if (res.ok) { await loadData(); refreshContent(); }
    } catch (e) { }
}

function esc(s) { const d = document.createElement('div'); d.textContent = s || ''; return d.innerHTML; }



window.initActionBtnPopovers = function() {
    if (typeof bootstrap === 'undefined') return;
    
    // First, dispose any existing popovers inside #mainActionBtn to prevent memory leaks
    const existingPopovers = document.querySelectorAll('#mainActionBtn [data-bs-toggle="popover"]');
    existingPopovers.forEach(el => {
        const popover = bootstrap.Popover.getInstance(el);
        if (popover) {
            popover.dispose();
        }
    });

    // Initialize new popovers
    const popoverTriggerList = [].slice.call(document.querySelectorAll('#mainActionBtn [data-bs-toggle="popover"]'));
    popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });
}

window.openQuizPreviewModal = async function(id, event) {
    if (event) event.preventDefault();
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    try {
        const res = await fetch(`/api/student/quiz/${id}`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        if (res.ok) {
            const q = await res.json();
            document.getElementById('preview-quiz-title').textContent = q.title || 'Chưa có tiêu đề';
            document.getElementById('preview-quiz-desc').textContent = q.description || 'Chưa có mô tả.';

            const container = document.getElementById('preview-quiz-questions');
            container.innerHTML = '';

            const list = q.questions || [];
            document.getElementById('preview-quiz-count').textContent = list.length;

            if (list.length === 0) {
                container.innerHTML = `<div class="text-center text-muted py-4">Chưa có câu hỏi nào trong đề.</div>`;
            } else {
                container.innerHTML = list.map((item, idx) => {
                    const levelClass = item.level === 'EASY' 
                        ? 'modal-q-badge-easy'
                        : item.level === 'MEDIUM'
                        ? 'modal-q-badge-medium'
                        : 'modal-q-badge-hard';
                    
                    const levelLabel = item.level === 'EASY' ? 'Dễ' : item.level === 'MEDIUM' ? 'Trung bình' : 'Khó';
                    const levelBadge = `<span class="modal-q-badge ${levelClass}">${levelLabel}</span>`;

                    const typeLabel = item.type === 'SINGLE_CHOICE'
                        ? 'Trắc nghiệm 1 đáp án'
                        : item.type === 'MULTIPLE_CHOICE'
                        ? 'Trắc nghiệm nhiều đáp án'
                        : 'Điền khuyết';

                    let ansHtml = '';
                    if (item.answers && item.answers.length > 0) {
                        ansHtml = item.answers.map((a, i) => {
                            const correctClass = a.isCorrect ? 'correct' : 'incorrect';
                            const checkIcon = a.isCorrect ? '<i class="bi bi-check-circle-fill modal-ans-icon"></i>' : '';
                            return `
                                <div class="modal-ans-row ${correctClass}">
                                    <span class="ans-letter">${String.fromCharCode(65 + i)}.</span>
                                    <span>${esc(a.text || '')}</span>
                                    ${checkIcon}
                                </div>`;
                        }).join('');
                    } else {
                        ansHtml = `<p class="text-muted small">Không có đáp án.</p>`;
                    }

                    return `
                    <div class="modal-q-card mb-3">
                        <div class="d-flex align-items-center justify-content-between mb-3">
                            <span class="fw-bold" style="font-size: 0.82rem; color: #4338ca;">Câu hỏi ${idx + 1} (${typeLabel})</span>
                            ${levelBadge}
                        </div>
                        <div class="fw-bold text-dark mb-2" style="font-size: 0.98rem; line-height: 1.5;">${esc(item.text || '')}</div>
                        <div>${ansHtml}</div>
                    </div>`;
                }).join('');
            }

            const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('previewQuizModal'));
            modal.show();
        }
    } catch (err) {
        console.error('Lỗi tải đề thi', err);
    }
}
