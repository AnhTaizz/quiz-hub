/* ══════════════════════════════════════════════
   STATE
 ══════════════════════════════════════════════ */
let MODE      = 'admin';
let TREE_DATA = [];
let navStack  = [];       // [{id, name}]
let OWN_MODE  = true;     // always true for admin
let expandedNodes = new Set();
let searchTerm = '';

// Hàm tìm đường dẫn chuẩn từ Gốc -> Thư mục hiện tại (giúp Breadcrumb chuẩn như File Explorer)
function getPathToNode(nodes, targetId, currentPath = []) {
    for (const n of nodes) {
        const path = [...currentPath, { id: n.id, name: n.name }];
        if (n.id === targetId) return path;
        if (n.children) {
            const found = getPathToNode(n.children, targetId, path);
            if (found) return found;
        }
    }
    return null;
}

/* ══════════════════════════════════════════════
   INIT
 ══════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
    initResize();

    initMode();

    const searchInput = document.getElementById('treeSearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', e => {
            searchTerm = e.target.value.trim().toLowerCase();
            renderTree(TREE_DATA);
            restoreTreeState();
        });
    }

    document.addEventListener('click', () => {
        document.querySelectorAll('.bc-dropdown').forEach(d => d.classList.remove('show'));
    });
});

/* ══════════════════════════════════════════════
   MODE INIT
 ══════════════════════════════════════════════ */
function initMode() {
    MODE = 'admin'; OWN_MODE = true;

    document.getElementById('treeLabel').textContent = 'Danh mục công khai';
    document.getElementById('rootHint').textContent  = 'Chọn thư mục bên trái hoặc tạo danh mục mới để bắt đầu.';

    // Show/hide create buttons per mode
    const tf = document.getElementById('toolbarActions');
    tf.style.display = 'flex';
    document.getElementById('btnNewFolder').style.display = 'flex';
    const treeGlobal = document.getElementById('treeGlobalActions');
    if(treeGlobal) treeGlobal.style.display = 'flex';

    navStack = [];
    TREE_DATA = [];
    searchTerm = '';
    const searchInput = document.getElementById('treeSearchInput');
    if (searchInput) searchInput.value = '';

    navigateToRoot();
    loadTree();
}

/* ══════════════════════════════════════════════
   TREE
 ══════════════════════════════════════════════ */
async function loadTree() {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const api   = '/api/admin/categories';
    try {
        const res = await fetch(api, { headers: { Authorization: 'Bearer ' + token } });
        if (!res.ok) throw new Error('Lỗi tải danh mục');
        TREE_DATA = await res.json();

        renderTree(TREE_DATA);
        restoreTreeState();
    } catch (e) {
        document.getElementById('treeScroll').innerHTML =
            `<div style="padding:16px;text-align:center;color:#94a3b8;font-size:.84rem;">
                <i class="bi bi-wifi-off d-block mb-2" style="font-size:1.6rem;"></i>${e.message}
             </div>`;
    }
}

function renderTree(nodes) {
    const wrap = document.getElementById('treeScroll');
    if (!nodes || nodes.length === 0) {
        wrap.innerHTML = `<div style="padding:24px 10px;text-align:center;color:#94a3b8;">
            <i class="bi bi-folder-plus d-block mb-3" style="font-size:2.5rem;color:#c4b5fd;"></i>
            <h6 style="color:#475569;font-weight:700;margin-bottom:8px;">Chưa có danh mục</h6>
            <p style="font-size:0.8rem;margin-bottom:16px;">Tạo thư mục đầu tiên để lưu trữ đề thi.</p>
            ${OWN_MODE ? `<button class="btn-tool btn-new-folder" style="margin:0 auto;" onclick="openCatModal(null)"><i class="bi bi-plus-lg"></i> Thêm mới</button>` : ''}
        </div>`;
        return;
    }
    wrap.innerHTML = `<div style="padding:6px;">${nodes.map(n => treeNodeHtml(n)).join('')}</div>`;
}

function treeNodeHtml(node) {
    const match = searchTerm === '' || node.name.toLowerCase().includes(searchTerm);
    let childrenHtml = '';
    let hasMatchingChild = false;

    if (node.children && node.children.length > 0) {
        const childHtmls = node.children.map(c => treeNodeHtml(c)).filter(h => h !== '');
        if (childHtmls.length > 0) {
            hasMatchingChild = true;
            childrenHtml = childHtmls.join('');
        }
    }

    if (searchTerm !== '' && !match && !hasMatchingChild) {
        return '';
    }

    const isForceOpen = searchTerm !== '' && hasMatchingChild;
    const isNodeOpen = isForceOpen || expandedNodes.has(node.id);

    const hasKids = node.children && node.children.length > 0;
    const arrow = hasKids
        ? `<i class="bi bi-chevron-right tree-arrow ${isNodeOpen ? 'open' : ''}" id="arr-${node.id}"></i>`
        : `<span style="width:12px;display:inline-block;"></span>`;

    const finalKidsHtml = (searchTerm !== '') ? childrenHtml : (node.children ? node.children.map(c => treeNodeHtml(c)).join('') : '');
    const kids = finalKidsHtml !== ''
        ? `<div class="tree-children ${isNodeOpen ? 'open' : ''}" id="kids-${node.id}">${finalKidsHtml}</div>`
        : '';

    // Inline action buttons — chỉ hiện trong chế độ Own
    const actions = OWN_MODE ? `
        <span class="tree-row-actions" onclick="event.stopPropagation()">
            <button class="tree-act-btn" title="Thêm thư mục con"
                onclick="openCatModal(${node.id})">
                <i class="bi bi-folder-plus"></i>
            </button>
            <button class="tree-act-btn" title="Sửa thư mục"
                onclick="openEditCatModal(${node.id},${JSON.stringify(node.name).replace(/"/g,'&quot;')},${JSON.stringify(node.description||'').replace(/"/g,'&quot;')},${node.parentId || null})">
                <i class="bi bi-pencil"></i>
            </button>
            <button class="tree-act-btn" title="Xóa thư mục"
                onclick="openDelCatModal(${node.id})">
                <i class="bi bi-trash"></i>
            </button>
        </span>` : '';

    let displayName = esc(node.name);
    if (searchTerm !== '' && match) {
        const regex = new RegExp(`(${searchTerm.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})`, 'gi');
        displayName = esc(node.name).replace(regex, '<mark style="background:#fef08a;padding:0 2px;border-radius:3px;color:#1e1b4b;">$1</mark>');
    }

    return `<div class="tree-node">
        <div class="tree-row" id="tr-${node.id}"
            draggable="${OWN_MODE ? 'true' : 'false'}"
            ondragstart="handleDragStart(event, ${node.id})"
            ondragover="handleDragOver(event)"
            ondragleave="handleDragLeave(event)"
            ondrop="handleDrop(event, ${node.id})"
            onclick="treeClick(${node.id},${JSON.stringify(node.name).replace(/"/g,'&quot;')},event)">
            ${arrow}
            <i class="bi bi-folder-fill tree-icon"></i>
            <span class="tree-label">${displayName}</span>
            <span class="tree-count">${calcTotal(node)}</span>
            ${actions}
        </div>
        ${kids}
    </div>`;
}

function calcTotal(node) {
    return node.quizCount || 0;
}

function treeClick(id, name, e) {
    // 1. Xem nội dung thư mục (hiển thị sang bảng bên phải)
    navigateTo(id, name);

    // 2. Tự động đóng/mở thư mục tại chỗ
    const kids = document.getElementById('kids-' + id);
    const arr  = document.getElementById('arr-'  + id);
    if (kids) {
        const isOpen = kids.classList.contains('open');
        if (isOpen) {
            kids.classList.remove('open');
            if (arr) arr.classList.remove('open');
            expandedNodes.delete(id);
        } else {
            kids.classList.add('open');
            if (arr) arr.classList.add('open');
            expandedNodes.add(id);
        }
    }
    e.stopPropagation();
}

function setTreeActive(id) {
    document.querySelectorAll('.tree-row').forEach(r => r.classList.remove('active'));
    const el = document.getElementById('tr-' + id);
    if (el) { el.classList.add('active'); el.scrollIntoView({ block: 'nearest' }); }
}

function restoreTreeState() {
    // Tự động xổ lại những thư mục người dùng đã thao tác mở trước đó
    expandedNodes.forEach(id => {
        const kids = document.getElementById('kids-' + id);
        const arr  = document.getElementById('arr-'  + id);
        if (kids) kids.classList.add('open');
        if (arr) arr.classList.add('open');
    });
    // Active lại node cuối cùng
    if (navStack.length > 0) {
        setTreeActive(navStack[navStack.length - 1].id);
    }
}

/* ══════════════════════════════════════════════
   NAVIGATION
 ══════════════════════════════════════════════ */
function navigateToRoot() {
    navStack = [];
    document.getElementById('breadcrumb').innerHTML =
        `<span class="bc-item active"><i class="bi bi-house-fill"></i> Gốc</span>`;
    document.getElementById('dynamicContent').style.display = 'none';
    document.getElementById('rootLanding').style.display    = 'flex';
    document.querySelectorAll('.tree-row').forEach(r => r.classList.remove('active'));

    // Reset toolbar buttons
    document.getElementById('btnNewFolder').onclick = () => openCatModal(null);
}

function navigateTo(id, name) {
    // Tự động tính toán đường dẫn thực tế (Ví dụ: Gốc > Toán > Giải Tích)
    const path = getPathToNode(TREE_DATA, id);
    if (path) {
        navStack = path;
        // Đảm bảo tất cả các thư mục cha trên đường dẫn này đều được xổ ra
        path.forEach(p => expandedNodes.add(p.id));
    } else {
        navStack = [{ id, name }];
    }

    setTreeActive(id);
    renderBreadcrumb();

    document.getElementById('rootLanding').style.display    = 'none';
    document.getElementById('dynamicContent').style.display = 'block';

    // Cập nhật toolbars tạo mới
    document.getElementById('btnNewFolder').onclick = () => openCatModal(id);

    loadFolderContent(id);
}

function navigateFromBreadcrumb(idx) {
    if (idx < 0) { navigateToRoot(); return; }
    const item = navStack[idx];
    navigateTo(item.id, item.name); // Click trên thanh đường dẫn sẽ điều hướng như bình thường
}

function renderBreadcrumb() {
    let html = `<span class="bc-item" onclick="navigateToRoot()"><i class="bi bi-house-fill"></i></span>`;
    navStack.forEach((item, i) => {
        const parentId = i > 0 ? navStack[i-1].id : null;
        const siblings = getChildrenOf(parentId);

        let siblingHtml = '';
        let hasSiblings = siblings && siblings.length > 1;
        if (hasSiblings) {
            siblingHtml = `<div class="bc-dropdown" onclick="event.stopPropagation()">
                ${siblings.map(s => `<div class="bc-dropdown-item ${s.id === item.id ? 'active' : ''}" onclick="navigateTo(${s.id}, '${escJs(s.name)}')">${esc(s.name)}</div>`).join('')}
            </div>`;
        }

        html += `<span class="bc-sep ${hasSiblings ? 'bc-sep-hoverable' : ''}" ${hasSiblings ? 'style="cursor:pointer;" onclick="toggleBcDropdown(event, this)"' : ''}>
            <i class="bi bi-chevron-right"></i>
            ${siblingHtml}
        </span>`;

        if (i < navStack.length - 1)
            html += `<span class="bc-item" onclick="navigateFromBreadcrumb(${i})">${esc(item.name)}</span>`;
        else
            html += `<span class="bc-item active">${esc(item.name)}</span>`;
    });
    document.getElementById('breadcrumb').innerHTML = html;
}

function getChildrenOf(parentId) {
    if (!parentId) return TREE_DATA;
    const parent = findNode(TREE_DATA, parentId);
    return parent ? parent.children : [];
}

function toggleBcDropdown(e, el) {
    e.stopPropagation();
    document.querySelectorAll('.bc-dropdown').forEach(d => {
        if (d !== el.querySelector('.bc-dropdown')) d.classList.remove('show');
    });
    const dropdown = el.querySelector('.bc-dropdown');
    if (dropdown) dropdown.classList.toggle('show');
}

/* ══════════════════════════════════════════════
   FOLDER CONTENT
 ══════════════════════════════════════════════ */
let currentQuestionPage = 0;
let currentQuestionSize = 10;
let activeFolderId = null;

async function loadFolderContent(catId, page = 0) {
    if (activeFolderId !== catId) {
        currentQuestionPage = 0;
        activeFolderId = catId;
    } else {
        currentQuestionPage = page;
    }

    const dc = document.getElementById('dynamicContent');
    dc.style.display = 'block';
    const rootLanding = document.getElementById('rootLanding');
    if (rootLanding) rootLanding.style.display = 'none';

    // Filter values
    const keyword = document.getElementById('q-filter-keyword')?.value || '';
    const type = document.getElementById('q-filter-type')?.value || '';
    const level = document.getElementById('q-filter-level')?.value || '';

    // Render skeleton and filters if not already there
    if (!document.getElementById('q-filters-container')) {
        dc.innerHTML = `
            <div id="q-filters-container" class="dashboard-card mb-4" style="padding: 15px; border-radius: 14px; background: #f8fafc; border: 1px dashed #cbd5e1;">
                <div class="row g-2">
                    <div class="col-md-5">
                        <div class="input-group input-group-sm">
                            <span class="input-group-text bg-white border-end-0"><i class="bi bi-search text-muted"></i></span>
                            <input type="text" id="q-filter-keyword" class="form-control border-start-0" placeholder="Tìm câu hỏi..." value="${keyword}" oninput="debounceQFetch()">
                        </div>
                    </div>
                    <div class="col-md-3">
                        <select id="q-filter-type" class="form-select form-select-sm" onchange="loadFolderContent(activeFolderId, 0)">
                            <option value="">-- Loại --</option>
                            <option value="SINGLE_CHOICE" ${type==='SINGLE_CHOICE'?'selected':''}>Một đáp án</option>
                            <option value="MULTIPLE_CHOICE" ${type==='MULTIPLE_CHOICE'?'selected':''}>Nhiều đáp án</option>
                            <option value="FILL_IN_BLANK" ${type==='FILL_IN_BLANK'?'selected':''}>Điền khuyết</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <select id="q-filter-level" class="form-select form-select-sm" onchange="loadFolderContent(activeFolderId, 0)">
                            <option value="">-- Mức độ --</option>
                            <option value="EASY" ${level==='EASY'?'selected':''}>Dễ</option>
                            <option value="MEDIUM" ${level==='MEDIUM'?'selected':''}>Trung bình</option>
                            <option value="HARD" ${level==='HARD'?'selected':''}>Khó</option>
                        </select>
                    </div>
                    <div class="col-md-1">
                        <button class="btn btn-sm btn-outline-secondary w-100" onclick="resetQFilters()" title="Xóa bộ lọc"><i class="bi bi-x-lg"></i></button>
                    </div>
                </div>
            </div>
            <div id="q-content-area">
                <div class="quiz-grid">${[1,2,3].map(()=>`<div class="skeleton" style="height:180px;border-radius:16px;"></div>`).join('')}</div>
            </div>`;
    } else {
        document.getElementById('q-content-area').innerHTML = `<div class="quiz-grid">${[1,2,3].map(()=>`<div class="skeleton" style="height:180px;border-radius:16px;"></div>`).join('')}</div>`;
    }

    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const node  = findNode(TREE_DATA, catId);
    const subs  = node ? (node.children || []) : [];

    let quizzes = [];
    let questionsData = null;

    try {
        const res = await fetch(`/api/admin/categories/${catId}/quizzes`,
            { headers: { Authorization: 'Bearer ' + token } });
        if (res.ok) quizzes = await res.json();
    } catch (_) {}

    try {
        let qUrl = `/api/admin/categories/${catId}/questions?page=${currentQuestionPage}&size=${currentQuestionSize}`;
        if (keyword) qUrl += `&keyword=${encodeURIComponent(keyword)}`;
        if (type) qUrl += `&type=${type}`;
        if (level) qUrl += `&level=${level}`;

        const qRes = await fetch(qUrl, { headers: { Authorization: 'Bearer ' + token } });
        if (qRes.ok) questionsData = await qRes.json();
    } catch (_) {}

    renderFolderContent(document.getElementById('q-content-area'), subs, quizzes, questionsData, catId);
}

let qDebounceTimer;
function debounceQFetch() {
    clearTimeout(qDebounceTimer);
    qDebounceTimer = setTimeout(() => {
        loadFolderContent(activeFolderId, 0);
    }, 500);
}

function resetQFilters() {
    document.getElementById('q-filter-keyword').value = '';
    document.getElementById('q-filter-type').value = '';
    document.getElementById('q-filter-level').value = '';
    loadFolderContent(activeFolderId, 0);
}

function renderFolderContent(container, subs, quizzes, questionsData, parentId) {
    let html = '';

    // Quizzes
    if (quizzes.length > 0) {
        html += `<p class="section-title fw-bold" style="font-size:1.1rem;margin-top:10px;"><i class="bi bi-file-earmark-text me-1"></i> Đề thi / Quiz (${quizzes.length})</p>`;
        html += `<div class="quiz-grid" style="display:grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 16px; margin-bottom:24px;">`;
        quizzes.forEach(q => { html += quizCardHtml(q); });
        html += `</div>`;
    }

    // Questions (if present)
    if (questionsData && questionsData.totalElements > 0) {
        html += `<p class="section-title fw-bold" style="font-size:1.1rem;margin-top:20px;border-top:1px solid #e2e8f0;padding-top:20px;"><i class="bi bi-patch-check me-1"></i> Danh sách câu hỏi (${questionsData.totalElements})</p>`;
        html += `<div class="question-list" style="display:flex; flex-direction:column; gap:16px; margin-bottom:24px;">`;
        questionsData.content.forEach(q => {
            let typeLabel = q.type === 'SINGLE_CHOICE' ? 'Một đáp án' : (q.type === 'MULTIPLE_CHOICE' ? 'Nhiều đáp án' : 'Điền khuyết');
            let levelLabel = q.level === 'EASY' ? 'Dễ' : (q.level === 'MEDIUM' ? 'Trung bình' : 'Khó');

            let answersPreviewHtml = '';
            if (q.answers && q.answers.length > 0) {
                answersPreviewHtml = `
                    <div class="ans-preview" style="display:grid;grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 10px; margin-top:12px;">
                        ${q.answers.map((a, idx) => `
                            <div class="ans-item ${a.isCorrect ? 'correct' : ''}" style="background:${a.isCorrect ? '#f0fdf4' : '#f8fafc'}; border:1px solid ${a.isCorrect ? '#bbf7d0' : '#e2e8f0'}; border-radius:12px; padding:10px 14px; font-size:0.9rem; color:${a.isCorrect ? '#15803d' : '#334155'}; font-weight:${a.isCorrect ? '600' : '400'}; display:flex; align-items:center; gap:8px;">
                                <i class="bi bi-${a.isCorrect ? 'check-circle-fill text-success' : 'circle'}"></i>
                                <span>${String.fromCharCode(65 + idx)}. ${esc(a.text)}</span>
                            </div>
                        `).join('')}
                    </div>`;
            }

            html += `
                <div class="question-card" style="background:#fff; border:1px solid #e2e8f0; border-radius:16px; padding:20px; box-shadow:0 4px 12px rgba(0,0,0,0.03); transition:all 0.2s;">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <div class="badge-row" style="display:flex; gap:8px; flex-wrap:wrap;">
                            <span class="q-badge" style="background:#f1f5f9; color:#475569; border-radius:8px; padding:4px 10px; font-size:0.78rem; font-weight:600;"><i class="bi bi-patch-check"></i> ${typeLabel}</span>
                            <span class="q-badge" style="background:#f1f5f9; color:#475569; border-radius:8px; padding:4px 10px; font-size:0.78rem; font-weight:600;"><i class="bi bi-graph-up"></i> ${levelLabel}</span>
                        </div>
                        <div class="text-muted small">Người gửi: <strong>${esc(q.creatorName || 'N/A')}</strong></div>
                    </div>
                    <div class="q-text" style="font-size:1rem; font-weight:600; color:#1e293b; margin:10px 0;">${esc(q.text)}</div>
                    ${answersPreviewHtml}
                    <div class="d-flex justify-content-end gap-2 mt-3 pt-2" style="border-top:1px dashed #e2e8f0;">
                        <button class="btn btn-sm btn-outline-primary fw-semibold" style="border-radius:10px; padding:6px 14px; font-size:0.85rem;" onclick="openMoveQuestionModal(${q.id})">
                            <i class="bi bi-folder-symlink-fill"></i> Chuyển danh mục
                        </button>
                        <button class="btn btn-sm btn-outline-danger fw-semibold" style="border-radius:10px; padding:6px 14px; font-size:0.85rem;" onclick="confirmDeleteQuestion(${q.id})">
                            <i class="bi bi-trash3-fill"></i> Xóa câu hỏi
                        </button>
                    </div>
                </div>`;
        });
        html += `</div>`;

        // Pagination for questions
        html += renderQuestionsPagination(questionsData);
    }

    // Empty
    if (quizzes.length === 0 && subs.length === 0 && (!questionsData || questionsData.totalElements === 0)) {
        html = `<div class="empty-root">
            <div class="empty-root-icon"><i class="bi bi-file-earmark-x"></i></div>
            <h3>Thư mục trống</h3>
            <p>${OWN_MODE ? 'Hãy thêm thư mục con hoặc tạo đề thi mới vào đây.' : 'Thư mục này hiện chưa có đề thi nào.'}</p>
            ${OWN_MODE ? `<div style="display:flex;gap:12px;margin-top:8px;">
                <button class="btn-tool btn-new-folder" onclick="openCatModal(${parentId})"><i class="bi bi-folder-plus"></i> Thêm thư mục con</button>
            </div>` : ''}
        </div>`;
    }

    container.innerHTML = html;
}

function renderQuestionsPagination(data) {
    if (!data || data.totalPages <= 1) return '';

    let pagesHtml = '';
    const maxVisible = 6;
    const startPage = Math.max(0, data.number - Math.floor(maxVisible / 2));
    const endPage = Math.min(data.totalPages - 1, startPage + maxVisible - 1);

    pagesHtml += `<div class="d-flex flex-column align-items-center justify-content-center gap-2 mt-4 pt-3 border-top" style="border-top: 1px solid #e2e8f0 !important;">`;
    pagesHtml += `<div class="text-muted small mb-1">Hiển thị ${data.number * data.size + 1} đến ${Math.min((data.number + 1) * data.size, data.totalElements)} trong tổng số ${data.totalElements} câu hỏi</div>`;
    
    pagesHtml += `<nav><ul class="pagination pagination-sm mb-1 gap-1">`;

    // First page
    if (startPage > 0) {
        pagesHtml += `<li class="page-item"><button class="btn btn-sm btn-outline-secondary px-3" onclick="goToQuestionsPage(0)">1</button></li>`;
        if (startPage > 1) {
            pagesHtml += `<li class="page-item disabled"><span class="btn btn-sm btn-light px-3" style="cursor:default">...</span></li>`;
        }
    }

    for (let i = startPage; i <= endPage; i++) {
        pagesHtml += `<li class="page-item"><button class="btn btn-sm ${i === data.number ? 'btn-primary active' : 'btn-outline-secondary'} px-3" onclick="goToQuestionsPage(${i})">${i + 1}</button></li>`;
    }

    // Last page
    if (endPage < data.totalPages - 1) {
        if (endPage < data.totalPages - 2) {
            pagesHtml += `<li class="page-item disabled"><span class="btn btn-sm btn-light px-3" style="cursor:default">...</span></li>`;
        }
        pagesHtml += `<li class="page-item"><button class="btn btn-sm btn-outline-secondary px-3" onclick="goToQuestionsPage(${data.totalPages - 1})">${data.totalPages}</button></li>`;
    }

    pagesHtml += `</ul></nav>`;

    // Page jump input
    pagesHtml += `
        <div class="d-flex align-items-center gap-2" style="font-size: 0.85rem;">
            <span class="text-muted fw-bold">Đi đến trang:</span>
            <input type="number" id="jumpToPageInput" class="form-control form-control-sm text-center" style="width:55px; height:32px; border-radius:8px;" min="1" max="${data.totalPages}" value="${data.number + 1}">
            <button class="btn btn-sm btn-secondary fw-bold" style="height:32px; border-radius:8px; padding:0 12px" onclick="jumpToQuestionsPage(${data.totalPages})">Đến</button>
        </div>`;

    pagesHtml += `</div>`;
    return pagesHtml;
}

function goToQuestionsPage(page) {
    loadFolderContent(activeFolderId, page);
}

function jumpToQuestionsPage(totalPages) {
    const input = document.getElementById('jumpToPageInput');
    let val = parseInt(input.value);
    if (isNaN(val) || val < 1 || val > totalPages) {
        showToast('Số trang nhập vào không hợp lệ!', 'err');
        return;
    }
    loadFolderContent(activeFolderId, val - 1);
}

function flattenTreeForSelect(nodes, level = 0, output = []) {
    nodes.forEach(n => {
        output.push({ id: n.id, name: ' '.repeat(level * 4).replace(/ /g, '&nbsp;') + n.name });
        if (n.children && n.children.length > 0) {
            flattenTreeForSelect(n.children, level + 1, output);
        }
    });
    return output;
}

function openMoveQuestionModal(questionId) {
    document.getElementById('moveQuestionId').value = questionId;
    document.getElementById('moveQuestionCategoryId').value = '';
    document.getElementById('moveQuestionCategoryName').textContent = '-- Chưa chọn danh mục --';
    document.getElementById('moveQuestionCategoryName').classList.add('text-muted');

    const modal = new bootstrap.Modal(document.getElementById('moveQuestionModal'));
    modal.show();
}

// 🌟 Callback cho Category Explorer
window.handleCategorySelection = function(id, name, target) {
    if (target === 'moveQuestion') {
        document.getElementById('moveQuestionCategoryId').value = id;
        document.getElementById('moveQuestionCategoryName').textContent = name;
        document.getElementById('moveQuestionCategoryName').classList.remove('text-muted');
        document.getElementById('moveQuestionCategoryName').classList.add('text-dark');
    }
}

async function doMoveQuestion() {
    const questionId = document.getElementById('moveQuestionId').value;
    const catId = document.getElementById('moveQuestionCategoryId').value;
    if (!catId) {
        showToast('Vui lòng chọn danh mục đích!', 'err');
        return;
    }

    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    try {
        const res = await fetch(`/api/admin/questions/${questionId}/move?categoryId=${catId}`, {
            method: 'PUT',
            headers: { Authorization: 'Bearer ' + token }
        });
        if (res.ok) {
            const modalEl = document.getElementById('moveQuestionModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
            showToast('Thành công', 'Đã chuyển danh mục cho câu hỏi.', 'success');
            loadFolderContent(activeFolderId, currentQuestionPage);
        } else {
            const err = await res.json().catch(() => ({}));
            showToast(err.message || 'Có lỗi xảy ra khi chuyển danh mục!', 'err');
        }
    } catch (_) {
        showToast('Có lỗi kết nối!', 'err');
    }
}

let questionToDelete = null;
function confirmDeleteQuestion(questionId) {
    questionToDelete = questionId;
    const modal = new bootstrap.Modal(document.getElementById('deleteQuestionModal'));
    modal.show();
}

async function doDeleteQuestion() {
    if (!questionToDelete) return;
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    try {
        const res = await fetch(`/api/admin/questions/${questionToDelete}`, {
            method: 'DELETE',
            headers: { Authorization: 'Bearer ' + token }
        });
        if (res.ok) {
            const modalEl = document.getElementById('deleteQuestionModal');
            const modal = bootstrap.Modal.getInstance(modalEl);
            if (modal) modal.hide();
            showToast('Thành công', 'Đã xóa câu hỏi khỏi hệ thống.', 'success');
            loadFolderContent(activeFolderId, currentQuestionPage);
        } else {
            showToast('Có lỗi xảy ra khi xóa câu hỏi!', 'err');
        }
    } catch (_) {
        showToast('Có lỗi kết nối!', 'err');
    }
}

function quizCardHtml(q) {
    const typeBadge   = q.isExam
        ? `<span class="qbadge qbadge-exam"><i class="bi bi-mortarboard-fill"></i> Kiểm tra</span>`
        : `<span class="qbadge qbadge-quiz"><i class="bi bi-journal-check"></i> Luyện tập</span>`;

    const actions = OWN_MODE
        ? `<div class="quiz-actions">
             <a class="qact qact-edit" href="/teacher/quizzes/${q.id}/edit"><i class="bi bi-pencil-square"></i> Sửa</a>
             <a class="qact qact-view" href="/teacher/quizzes/${q.id}/preview"><i class="bi bi-eye"></i> Xem</a>
             <button class="qact qact-del" onclick="deleteQuiz('${q.id}')"><i class="bi bi-trash3"></i></button>
           </div>`
        : `<div class="quiz-actions">
             <a class="qact qact-view" style="flex:1;" href="/teacher/quizzes/${q.id}/preview"><i class="bi bi-eye"></i> Xem thử</a>
           </div>`;

    return `<div class="quiz-card">
        <div class="quiz-badge-row">${typeBadge}</div>
        <h3 class="quiz-title">${esc(q.title) || '(Chưa có tiêu đề)'}</h3>
        <p class="quiz-desc">${esc(q.description) || '<span style="color:#cbd5e1;font-style:italic;">Chưa có mô tả.</span>'}</p>
        <div class="quiz-chips">
            <span class="chip"><i class="bi bi-list-check text-success"></i> ${q.questionCount} câu</span>
            ${q.creatorName ? `<span class="chip"><i class="bi bi-person text-primary"></i> ${esc(q.creatorName)}</span>` : ''}
        </div>
        ${actions}
    </div>`;
}

/* ══════════════════════════════════════════════
   CATEGORY CRUD
 ══════════════════════════════════════════════ */
function openCatModal(parentId) {
    document.getElementById('catEditId').value    = '';
    document.getElementById('catParentId').value  = parentId || '';
    document.getElementById('catModalTitle').textContent = parentId ? 'Tạo thư mục con' : 'Tạo danh mục mới';
    document.getElementById('catNameInput').value = '';
    document.getElementById('catDescInput').value = '';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('catModal')).show();
}

function openEditCatModal(id, name, desc, parentId) {
    document.getElementById('catEditId').value    = id;
    document.getElementById('catParentId').value  = parentId || '';
    document.getElementById('catModalTitle').textContent = 'Sửa danh mục';
    document.getElementById('catNameInput').value = name;
    document.getElementById('catDescInput').value = desc || '';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('catModal')).show();
}

async function submitCategory() {
    const token   = localStorage.getItem('token') || sessionStorage.getItem('token');
    const editId  = document.getElementById('catEditId').value;
    const parentId = document.getElementById('catParentId').value;
    const name    = document.getElementById('catNameInput').value.trim();
    const desc    = document.getElementById('catDescInput').value.trim();
    const isPublic = true;

    if (!name) { showToast('Vui lòng nhập tên danh mục!', 'err'); return; }

    const isEdit = !!editId;
    const url    = isEdit ? `/api/admin/categories/${editId}` : '/api/admin/categories';
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method, headers: { Authorization: 'Bearer ' + token, 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, description: desc, parentId: parentId ? parseInt(parentId) : null, isPublic })
        });
        if (!res.ok) { const e = await res.json(); throw new Error(e.message || 'Lỗi'); }

        bootstrap.Modal.getInstance(document.getElementById('catModal')).hide();
        showToast(isEdit ? 'Đã cập nhật danh mục!' : 'Tạo danh mục thành công!', 'ok');

        // 🌟 Tự động ghi nhớ phải xổ thư mục cha ra
        if (parentId) {
            expandedNodes.add(parseInt(parentId));
        }

        await loadTree();
        restoreTreeState();

        // 🌟 Điều hướng ngay lập tức vào thư mục cha để nội dung bên phải được Refresh,
        // cho phép bạn thấy ngay thư mục con vừa tạo y hệt Windows Explorer.
        if (parentId) {
            const parentNode = findNode(TREE_DATA, parseInt(parentId));
            if (parentNode) navigateTo(parentNode.id, parentNode.name);
        } else if (navStack.length > 0) {
            loadFolderContent(navStack[navStack.length-1].id);
        } else {
             navigateToRoot();
        }
    } catch (e) { showToast(e.message, 'err'); }
}

function openDelCatModal(id) {
    document.getElementById('delCatId').value = id;
    bootstrap.Modal.getOrCreateInstance(document.getElementById('delCatModal')).show();
}

async function confirmDelCat() {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const id    = document.getElementById('delCatId').value;
    try {
        const res = await fetch(`/api/admin/categories/${id}`, {
            method: 'DELETE', headers: { Authorization: 'Bearer ' + token }
        });
        if (!res.ok) { const e = await res.json(); throw new Error(e.message || 'Lỗi'); }
        bootstrap.Modal.getInstance(document.getElementById('delCatModal')).hide();
        showToast('Đã xóa danh mục!', 'ok');
        const curId = navStack.length > 0 ? navStack[navStack.length-1].id : null;
        if (String(curId) === String(id)) navigateFromBreadcrumb(navStack.length - 2);
        await loadTree();
        restoreTreeState();
    } catch (e) { showToast(e.message, 'err'); }
}

/* ══════════════════════════════════════════════
   DRAG & DROP
 ══════════════════════════════════════════════ */
let draggedNodeId = null;

function handleDragStart(e, id) {
    if (!OWN_MODE) return;
    draggedNodeId = id;
    e.dataTransfer.effectAllowed = 'move';
    setTimeout(() => e.target.classList.add('dragging'), 0);
}

function handleDragOver(e) {
    if (!OWN_MODE) return;
    e.preventDefault(); // Cho phép drop
    e.currentTarget.classList.add('drag-over');
    e.dataTransfer.dropEffect = 'move';
}

function handleDragLeave(e) {
    if (!OWN_MODE) return;
    e.currentTarget.classList.remove('drag-over');
}

async function handleDrop(e, targetParentId) {
    if (!OWN_MODE) return;
    e.stopPropagation();
    e.preventDefault();
    e.currentTarget.classList.remove('drag-over');

    const el = document.querySelector('.tree-row.dragging');
    if(el) el.classList.remove('dragging');

    if (!draggedNodeId || draggedNodeId === targetParentId) return;

    // Validate: Không được thả vào chính con/cháu của nó
    const draggedNode = findNode(TREE_DATA, draggedNodeId);
    if (!draggedNode) return;

    if (targetParentId && isDescendant(draggedNode, targetParentId)) {
        showToast('Không thể di chuyển danh mục vào bên trong thư mục con của nó!', 'err');
        return;
    }

    // Gọi API update parentId
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    try {
        const payload = {
            name: draggedNode.name,
            description: draggedNode.description || '',
            parentId: targetParentId,
            isPublic: true
        };
        const res = await fetch(`/api/admin/categories/${draggedNodeId}`, {
            method: 'PUT',
            headers: { Authorization: 'Bearer ' + token, 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        if (!res.ok) throw new Error('Lỗi di chuyển danh mục');

        showToast('Đã di chuyển danh mục!', 'ok');
        if (targetParentId) expandedNodes.add(targetParentId);
        await loadTree();
        restoreTreeState();

        // Điều hướng lại
        if (targetParentId) {
            const parentNode = findNode(TREE_DATA, targetParentId);
            if (parentNode) navigateTo(parentNode.id, parentNode.name);
        } else {
            navigateToRoot();
        }
    } catch (err) {
        showToast(err.message, 'err');
    } finally {
        draggedNodeId = null;
    }
}

function isDescendant(parent, childId) {
    if (parent.id === childId) return true;
    if (!parent.children) return false;
    for (const child of parent.children) {
        if (isDescendant(child, childId)) return true;
    }
    return false;
}

/* ══════════════════════════════════════════════
   QUIZ ACTIONS
 ══════════════════════════════════════════════ */
function openQuizCreate(catId) {
    window.location.href = `/teacher/quizzes/create${catId ? '?categoryId='+catId : ''}`;
}

function deleteQuiz(id) {
    document.getElementById('delQuizId').value = id;
    const modal = new bootstrap.Modal(document.getElementById('delQuizModal'));
    modal.show();
}

async function doDeleteQuiz() {
    const id = document.getElementById('delQuizId').value;
    if (!id) return;
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    try {
        const res = await fetch(`/api/teacher/quizzes/${id}`, {
            method: 'DELETE', headers: { Authorization: 'Bearer ' + token }
        });
        if (!res.ok) throw new Error('Lỗi xóa');
        
        const modalEl = document.getElementById('delQuizModal');
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();
        
        showToast('Đã xóa đề thi!', 'ok');
        if (navStack.length > 0) loadFolderContent(navStack[navStack.length-1].id);
    } catch (e) { showToast(e.message, 'err'); }
}

/* ══════════════════════════════════════════════
   UTILS
 ══════════════════════════════════════════════ */
function findNode(nodes, id) {
    for (const n of nodes) {
        if (n.id === id) return n;
        if (n.children) { const f = findNode(n.children, id); if (f) return f; }
    }
    return null;
}
function esc(s) { const d = document.createElement('div'); d.textContent = s||''; return d.innerHTML; }
function escJs(s) { return s ? s.replace(/'/g, "\\'").replace(/"/g, '\\"') : ''; }
function showToast(msg, type='ok') {
    const w = document.getElementById('toastWrap');
    const t = document.createElement('div');
    t.className = `toast-msg ${type}`;
    t.innerHTML = `<i class="bi ${type==='ok'?'bi-check-circle-fill':'bi-exclamation-circle-fill'}"></i> ${msg}`;
    w.appendChild(t);
    setTimeout(() => t.classList.add('show'), 10);
    setTimeout(() => { t.classList.remove('show'); setTimeout(() => t.remove(), 300); }, 3500);
}
function initResize() {
    const handle = document.getElementById('resizeHandle');
    const panel  = document.getElementById('treePanel');
    let dragging=false, startX, startW;
    handle.addEventListener('mousedown', e => {
        dragging=true; startX=e.clientX; startW=panel.offsetWidth;
        document.body.style.cursor='col-resize'; document.body.style.userSelect='none';
    });
    document.addEventListener('mousemove', e => {
        if (!dragging) return;
        panel.style.width = Math.max(160, Math.min(420, startW + e.clientX - startX)) + 'px';
    });
    document.addEventListener('mouseup', () => {
        dragging=false; document.body.style.cursor=''; document.body.style.userSelect='';
    });
}
