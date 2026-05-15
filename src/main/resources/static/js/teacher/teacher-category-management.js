/* ══════════════════════════════════════════════
   STATE
══════════════════════════════════════════════ */
let MODE = 'mine';   // 'mine' | 'public'
let TREE_DATA = [];
let navStack = [];       // [{id, name}]
let OWN_MODE = true;     // can edit in mine mode
let expandedNodes = new Set(); // Lưu trữ ID các thư mục đang được xổ ra
let searchTerm = '';      // Từ khóa tìm kiếm cây danh mục

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

    const catType = document.getElementById('categoryTypeInput').value || 'mine';
    initMode(catType);

    const searchInput = document.getElementById('treeSearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', e => {
            searchTerm = e.target.value.trim().toLowerCase();
            const treeActions = document.getElementById('treeGlobalActions');
            if (treeActions) {
                treeActions.style.display = (searchTerm === '' && OWN_MODE) ? 'flex' : 'none';
            }
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
function initMode(m) {
    MODE = m; OWN_MODE = (m === 'mine');

    document.getElementById('treeLabel').textContent = m === 'mine' ? 'Danh mục của tôi' : 'Danh mục công khai';
    document.getElementById('rootHint').textContent = m === 'mine'
        ? 'Chọn thư mục bên trái hoặc tạo danh mục mới để bắt đầu.'
        : 'Chọn thư mục bên trái để xem các đề thi.';

    // Show/hide create buttons per mode
    const tf = document.getElementById('toolbarActions');
    tf.style.display = 'flex';
    document.getElementById('btnNewFolder').style.display = OWN_MODE ? 'flex' : 'none';
    const dropdownWrap = document.getElementById('dropdownQuizWrap');
    if (dropdownWrap) dropdownWrap.style.display = OWN_MODE ? 'block' : 'none';
    const treeGlobal = document.getElementById('treeGlobalActions');
    if (treeGlobal) treeGlobal.style.display = OWN_MODE ? 'flex' : 'none';

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
    const api = MODE === 'mine' ? '/api/teacher/categories/mine' : '/api/teacher/categories/public';
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
    wrap.innerHTML = `<div style="padding:6px;">${nodes.map(n => treeNodeHtml(n, false)).join('')}</div>`;
}

function treeNodeHtml(node, forceShow = false) {
    const isDirectMatch = searchTerm === '' || node.name.toLowerCase().includes(searchTerm);
    const match = forceShow || isDirectMatch;
    
    let childrenHtml = '';
    let hasMatchingChild = false;

    if (node.children && node.children.length > 0) {
        // Nếu nút hiện tại khớp, toàn bộ con của nó được forceShow = true
        const childHtmls = node.children.map(c => treeNodeHtml(c, match)).filter(h => h !== '');
        if (childHtmls.length > 0) {
            hasMatchingChild = true;
            childrenHtml = childHtmls.join('');
        }
    }

    if (searchTerm !== '' && !match && !hasMatchingChild) {
        return '';
    }

    // Tự động xổ ra nếu node con có chứa kết quả tìm kiếm, để người dùng thấy được node khớp.
    // Nhưng nếu node này khớp trực tiếp, không ép buộc mở toàn bộ con sâu dưới (để tự click mở sau).
    const isForceOpen = searchTerm !== '' && hasMatchingChild && !isDirectMatch;
    const isNodeOpen = isForceOpen || expandedNodes.has(node.id);

    const hasKids = node.children && node.children.length > 0;
    // Chỉ hiện mũi tên nếu thực tế node đó có con được kết xuất
    const arrow = (hasKids && childrenHtml !== '')
        ? `<i class="bi bi-chevron-right tree-arrow ${isNodeOpen ? 'open' : ''}" id="arr-${node.id}"></i>`
        : `<span style="width:12px;display:inline-block;"></span>`;

    const kids = childrenHtml !== ''
        ? `<div class="tree-children ${isNodeOpen ? 'open' : ''}" id="kids-${node.id}">${childrenHtml}</div>`
        : '';

    // Inline action buttons — chỉ hiện trong chế độ Own
    const actions = OWN_MODE ? `
<span class="tree-row-actions" onclick="event.stopPropagation()">
    <button class="tree-act-btn" title="Thêm thư mục con"
        onclick="openCatModal(${node.id})">
        <i class="bi bi-folder-plus"></i>
    </button>
    <button class="tree-act-btn" title="Sửa thư mục"
        onclick="openEditCatModal(${node.id},${JSON.stringify(node.name).replace(/"/g, '&quot;')},${JSON.stringify(node.description || '').replace(/"/g, '&quot;')},${node.parentId || null})">
        <i class="bi bi-pencil"></i>
    </button>
    <button class="tree-act-btn" title="Xóa thư mục"
        onclick="openDelCatModal(${node.id})">
        <i class="bi bi-trash"></i>
    </button>
</span>` : '';

    let displayName = esc(node.name);
    if (searchTerm !== '' && isDirectMatch) {
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
    onclick="treeClick(${node.id},${JSON.stringify(node.name).replace(/"/g, '&quot;')},event)">
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
    const arr = document.getElementById('arr-' + id);
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
        const arr = document.getElementById('arr-' + id);
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
    document.getElementById('rootLanding').style.display = 'flex';
    document.querySelectorAll('.tree-row').forEach(r => r.classList.remove('active'));

    // Reset toolbar buttons to no category context
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

    document.getElementById('rootLanding').style.display = 'none';
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
        const parentId = i > 0 ? navStack[i - 1].id : null;
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
async function loadFolderContent(catId) {
    const dc = document.getElementById('dynamicContent');
    dc.innerHTML = `<div class="quiz-grid">${[1, 2, 3].map(() => `<div class="skeleton" style="height:180px;border-radius:16px;"></div>`).join('')}</div>`;

    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const node = findNode(TREE_DATA, catId);
    const subs = node ? (node.children || []) : [];

    let quizzes = [];
    try {
        const suffix = MODE === 'mine' ? '/quizzes/mine' : '/quizzes/public';
        const res = await fetch(`/api/teacher/categories/${catId}${suffix}`,
            { headers: { Authorization: 'Bearer ' + token } });
        if (res.ok) quizzes = await res.json();
    } catch (_) { }

    renderFolderContent(dc, subs, quizzes, catId);
}

function renderFolderContent(container, subs, quizzes, parentId) {
    let html = '';

    // Quizzes
    if (quizzes.length > 0) {
        html += `<p class="section-title"><i class="bi bi-file-earmark-text me-1"></i> Đề thi / Quiz (${quizzes.length})</p>`;
        html += `<div class="quiz-grid">`;
        quizzes.forEach(q => { html += quizCardHtml(q); });
        html += `</div>`;
    }

    // Empty
    if (quizzes.length === 0 && subs.length === 0) {
        html = `<div class="empty-root">
    <div class="empty-root-icon"><i class="bi bi-file-earmark-x"></i></div>
    <h3>Thư mục trống</h3>
    <p>${OWN_MODE ? 'Hãy thêm thư mục con hoặc tạo đề thi mới vào đây.' : 'Thư mục này hiện chưa có đề thi nào.'}</p>
    ${OWN_MODE ? `<div style="display:flex;gap:12px;margin-top:8px;">
        <button class="btn-tool btn-new-folder" onclick="openCatModal(${parentId})"><i class="bi bi-folder-plus"></i> Thêm thư mục con</button>
        <div class="dropdown">
            <button class="btn-tool btn-new-quiz dropdown-toggle" type="button" data-bs-toggle="dropdown" aria-expanded="false">
                <i class="bi bi-file-earmark-plus-fill"></i> Tạo đề thi
            </button>
            <ul class="dropdown-menu shadow border-0 rounded-4 mt-2 p-2" style="width: 260px; animation: fadeIn 0.2s ease;">
                <li>
                    <a class="dropdown-item p-3 rounded-3 d-flex align-items-center gap-3 mb-1" href="javascript:void(0)" onclick="goCreate('manual')">
                        <div style="background:#eff6ff; color:#3b82f6; width:36px; height:36px; border-radius:10px; display:flex; align-items:center; justify-content:center; font-size:1rem;"><i class="bi bi-pencil-square"></i></div>
                        <div>
                            <div class="fw-bold text-dark" style="font-size:0.8rem;">Soạn thủ công</div>
                            <div class="text-muted" style="font-size:0.65rem;">Chọn câu hỏi từng bước</div>
                        </div>
                    </a>
                </li>
                <li>
                    <a class="dropdown-item p-3 rounded-3 d-flex align-items-center gap-3" href="javascript:void(0)" onclick="goCreate('quick')">
                        <div style="background:#f0fdf4; color:#16a34a; width:36px; height:36px; border-radius:10px; display:flex; align-items:center; justify-content:center; font-size:1rem;"><i class="bi bi-magic"></i></div>
                        <div>
                            <div class="fw-bold text-dark" style="font-size:0.8rem;">Tạo nhanh (Magic)</div>
                            <div class="text-muted" style="font-size:0.65rem;">Visual Designer: Excel/JSON</div>
                        </div>
                    </a>
                </li>
            </ul>
        </div>
    </div>` : ''}
</div>`;
    }

    container.innerHTML = html;
}

function quizCardHtml(q) {
    const typeBadge = q.isExam
        ? `<span class="qbadge qbadge-exam"><i class="bi bi-mortarboard-fill"></i> Kiểm tra</span>`
        : `<span class="qbadge qbadge-quiz"><i class="bi bi-journal-check"></i> Luyện tập</span>`;

    const actions = OWN_MODE
        ? `<div class="quiz-actions">
     <a class="qact qact-edit" href="/teacher/quizzes/${q.id}/edit"><i class="bi bi-pencil-square"></i> Sửa</a>
     <a class="qact qact-view" href="javascript:void(0)" onclick="openQuizPreviewModal('${q.id}', event)"><i class="bi bi-eye"></i> Xem</a>
     <button class="qact qact-del" onclick="deleteQuiz('${q.id}')"><i class="bi bi-trash3"></i></button>
   </div>`
        : `<div class="quiz-actions">
     <a class="qact qact-view" style="flex:1;" href="javascript:void(0)" onclick="openQuizPreviewModal('${q.id}', event)"><i class="bi bi-eye"></i> Xem thử</a>
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
    document.getElementById('catEditId').value = '';
    document.getElementById('catParentId').value = parentId || '';
    document.getElementById('catModalTitle').textContent = parentId ? 'Tạo thư mục con' : 'Tạo danh mục mới';
    document.getElementById('catNameInput').value = '';
    document.getElementById('catDescInput').value = '';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('catModal')).show();
}

function openEditCatModal(id, name, desc, parentId) {
    document.getElementById('catEditId').value = id;
    document.getElementById('catParentId').value = parentId || '';
    document.getElementById('catModalTitle').textContent = 'Sửa danh mục';
    document.getElementById('catNameInput').value = name;
    document.getElementById('catDescInput').value = desc || '';
    bootstrap.Modal.getOrCreateInstance(document.getElementById('catModal')).show();
}

async function submitCategory() {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const editId = document.getElementById('catEditId').value;
    const parentId = document.getElementById('catParentId').value;
    const name = document.getElementById('catNameInput').value.trim();
    const desc = document.getElementById('catDescInput').value.trim();
    const isPublic = false; // Teacher cannot set isPublic

    if (!name) { showToast('Vui lòng nhập tên danh mục!', 'err'); return; }

    const isEdit = !!editId;
    const url = isEdit ? `/api/teacher/categories/${editId}` : '/api/teacher/categories';
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const res = await fetch(url, {
            method, headers: { Authorization: 'Bearer ' + token, 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, description: desc, parentId: parentId ? parseInt(parentId) : null, isPublic })
        });
        if (!res.ok) { const e = await res.json(); throw new Error(e.message || 'Lỗi'); }

        const modalInstance = bootstrap.Modal.getInstance(document.getElementById('catModal'));
        if (modalInstance) modalInstance.hide();
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
            loadFolderContent(navStack[navStack.length - 1].id);
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
    const id = document.getElementById('delCatId').value;
    try {
        const res = await fetch(`/api/teacher/categories/${id}`, {
            method: 'DELETE', headers: { Authorization: 'Bearer ' + token }
        });
        if (!res.ok) { const e = await res.json(); throw new Error(e.message || 'Lỗi'); }
        const modalInstance = bootstrap.Modal.getInstance(document.getElementById('delCatModal'));
        if (modalInstance) modalInstance.hide();
        showToast('Đã xóa danh mục!', 'ok');
        const curId = navStack.length > 0 ? navStack[navStack.length - 1].id : null;
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
    if (el) el.classList.remove('dragging');

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
            isPublic: false
        };
        const res = await fetch(`/api/teacher/categories/${draggedNodeId}`, {
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
function goCreate(mode) {
    const catId = (navStack.length > 0) ? navStack[navStack.length - 1].id : '';
    const suffix = catId ? `?categoryId=${catId}` : '';
    if (mode === 'manual') {
        window.location.href = `/teacher/quizzes/create${suffix}`;
    } else {
        window.location.href = `/teacher/quizzes/quick-create${suffix}`;
    }
}

async function deleteQuiz(id) {
    if (!confirm('Xóa đề thi này? (Soft delete, không thể khôi phục)')) return;
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    try {
        const res = await fetch(`/api/teacher/quizzes/${id}`, {
            method: 'DELETE', headers: { Authorization: 'Bearer ' + token }
        });
        if (!res.ok) throw new Error('Lỗi xóa');
        showToast('Đã xóa đề thi!', 'ok');
        if (navStack.length > 0) loadFolderContent(navStack[navStack.length - 1].id);
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
function esc(s) { const d = document.createElement('div'); d.textContent = s || ''; return d.innerHTML; }
function escJs(s) { return s ? s.replace(/'/g, "\\'").replace(/"/g, '\\"') : ''; }

async function openQuizPreviewModal(id, event) {
    if (event) event.preventDefault();
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    try {
        const res = await fetch(`/api/teacher/quizzes/${id}`, {
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
                    let ansHtml = '';
                    if (item.answers && item.answers.length > 0) {
                        ansHtml = item.answers.map((a, i) => `
                            <div class="p-2 border rounded mb-2 d-flex align-items-center justify-content-between ${a.isCorrect ? 'bg-success bg-opacity-10 text-success fw-bold' : 'bg-light text-muted'}" style="border-radius:10px; font-size:0.95rem; border:1px solid #e2e8f0 !important;">
                                <span>${String.fromCharCode(65 + i)}. ${esc(a.text || '')}</span>
                                ${a.isCorrect ? '<i class="bi bi-check-circle-fill ms-2 text-success"></i>' : ''}
                            </div>`).join('');
                    } else {
                        ansHtml = `<p class="text-muted small">Không có đáp án.</p>`;
                    }

                    return `<div class="p-3 border rounded-3 bg-white mb-3" style="border-color:#e2e8f0;">
                        <div class="fw-bold mb-2" style="color: #1e1b4b;">Câu ${idx + 1}: ${esc(item.text || '')}</div>
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

function showToast(msg, type = 'ok') {
    const w = document.getElementById('toastWrap');
    const t = document.createElement('div');
    t.className = `toast-msg ${type}`;
    t.innerHTML = `<i class="bi ${type === 'ok' ? 'bi-check-circle-fill' : 'bi-exclamation-circle-fill'}"></i> ${msg}`;
    w.appendChild(t);
    setTimeout(() => t.classList.add('show'), 10);
    setTimeout(() => { t.classList.remove('show'); setTimeout(() => t.remove(), 300); }, 3500);
}
function initResize() {
    const handle = document.getElementById('resizeHandle');
    const panel = document.getElementById('treePanel');
    if (!handle || !panel) return;
    let dragging = false, startX, startW;
    handle.addEventListener('mousedown', e => {
        dragging = true; startX = e.clientX; startW = panel.offsetWidth;
        document.body.style.cursor = 'col-resize'; document.body.style.userSelect = 'none';
    });
    document.addEventListener('mousemove', e => {
        if (!dragging) return;
        panel.style.width = Math.max(160, Math.min(420, startW + e.clientX - startX)) + 'px';
    });
    document.addEventListener('mouseup', () => {
        dragging = false; document.body.style.cursor = ''; document.body.style.userSelect = '';
    });
}
