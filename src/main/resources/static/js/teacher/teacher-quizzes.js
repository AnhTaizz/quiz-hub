let allQuizzes = [];

document.addEventListener('DOMContentLoaded', () => {
    loadQuizzes();
    fetchCategories();

    flatpickr('.flatpickr-datetime', {
        enableTime: true,
        altInput: true,
        altFormat: "d/m/Y H:i",
        dateFormat: "Y-m-dTH:i",
        time_24hr: true,
        locale: "vn",
        allowInput: true,
        minDate: "today"
    });
});

async function fetchCategories() {
    try {
        window.myCategories = await apiClient.get('/api/teacher/categories/mine');
    } catch (e) {
        console.error('Failed to load categories', e);
    }
}

async function loadQuizzes() {
    const wrapper = document.getElementById('quizzesWrapper');
    try {
        allQuizzes = await apiClient.get('/api/teacher/quizzes/mine');
        filterQuizzes();
    } catch (e) {
        wrapper.innerHTML = `
            <div class="text-center py-5" style="grid-column:1/-1;">
                <i class="bi bi-wifi-off text-muted d-block mb-3" style="font-size: 2.8rem;"></i>
                <h5 class="text-muted fw-bold">Không thể tải đề thi</h5>
                <p class="text-muted small">${e.message || 'Lỗi tải danh sách đề thi'}</p>
            </div>`;
    }
}

function filterQuizzes() {
    const search = document.getElementById('searchInput').value.trim().toLowerCase();
    const categoryId = document.getElementById('filter-category').value;

    // Get all descendant category IDs for recursive filtering
    let targetCategoryIds = [];
    if (categoryId) {
        targetCategoryIds = [parseInt(categoryId)];
        const descendants = getDescendantIds(window.myCategories || [], parseInt(categoryId));
        targetCategoryIds = targetCategoryIds.concat(descendants);
    }

    const filtered = allQuizzes.filter(q => {
        const matchSearch = !search || (q.title && q.title.toLowerCase().includes(search));
        const matchCategory = !categoryId || targetCategoryIds.includes(q.categoryId);
        return matchSearch && matchCategory;
    });

    renderQuizzes(filtered);
}

// Helper to find all children IDs recursively
function getDescendantIds(nodes, parentId) {
    let ids = [];
    for (const node of nodes) {
        if (node.id === parentId) {
            collectChildIds(node.children || [], ids);
            return ids;
        }
        if (node.children) {
            const found = getDescendantIds(node.children, parentId);
            if (found.length > 0) return found;
        }
    }
    return ids;
}

function collectChildIds(children, ids) {
    for (const child of children) {
        ids.push(child.id);
        if (child.children) collectChildIds(child.children, ids);
    }
}

function clearCategoryFilter() {
    document.getElementById('filter-category').value = '';
    document.getElementById('filter-category-display').value = 'Tất cả danh mục';
    document.getElementById('clear-cat-btn').style.display = 'none';
    filterQuizzes();
}

// Global callback for category selection
window.triggerSearch = function () {
    const catId = document.getElementById('filter-category').value;
    document.getElementById('clear-cat-btn').style.display = catId ? 'inline-block' : 'none';
    filterQuizzes();
};

function renderQuizzes(list) {
    const wrapper = document.getElementById('quizzesWrapper');
    if (!list || list.length === 0) {
        wrapper.innerHTML = `
            <div class="text-center py-5" style="grid-column: 1/-1;">
                <i class="bi bi-file-earmark-x text-muted d-block mb-3" style="font-size: 3rem; color: #cbd5e1 !important;"></i>
                <h5 style="color:#475569; font-weight:700;">Không tìm thấy đề thi nào</h5>
                <p style="font-size:.85rem; color:#94a3b8; margin-bottom:0;">Vui lòng đổi bộ lọc hoặc soạn đề thi mới.</p>
            </div>`;
        return;
    }

    wrapper.innerHTML = list.map(q => {
        const typeBadge = q.isExam
            ? `<span class="qbadge qbadge-exam"><i class="bi bi-mortarboard-fill"></i> Kiểm tra</span>`
            : `<span class="qbadge qbadge-quiz"><i class="bi bi-journal-check"></i> Luyện tập</span>`;

        return `
        <div class="quiz-card">
            <div class="quiz-badge-row">${typeBadge}</div>
            <h3 class="quiz-title">${esc(q.title) || '(Chưa có tiêu đề)'}</h3>
            <p class="quiz-desc">${esc(q.description) || '<span style="color:#cbd5e1; font-style:italic;">Chưa có mô tả.</span>'}</p>
            <div class="quiz-chips">
                <span class="chip"><i class="bi bi-list-check text-success"></i> ${q.questionCount} câu</span>
                <span class="chip"><i class="bi bi-folder text-primary"></i> ${esc(q.categoryName) || 'Kho chung'}</span>
            </div>
            <div class="quiz-actions">
                <a class="qact qact-edit" href="/teacher/quizzes/${q.id}/edit"><i class="bi bi-pencil-square"></i> Sửa</a>
                <button class="qact qact-view" onclick="openPreviewModal('${q.id}', '${esc(q.title)}')"><i class="bi bi-eye"></i> Xem</button>
                <button class="qact qact-assign" onclick="openAssignModal('${q.id}', '${esc(q.title)}')"><i class="bi bi-send-fill"></i> Giao bài</button>
                <button class="qact qact-del" onclick="openDeleteModal('${q.id}')"><i class="bi bi-trash3"></i></button>
            </div>
        </div>`;
    }).join('');
}

function openDeleteModal(id) {
    document.getElementById('delQuizId').value = id;
    bootstrap.Modal.getOrCreateInstance(document.getElementById('deleteModal')).show();
}

async function confirmDelete() {
    const id = document.getElementById('delQuizId').value;
    try {
        await apiClient.delete(`/api/teacher/quizzes/${id}`);
        bootstrap.Modal.getInstance(document.getElementById('deleteModal')).hide();
        showToast('Đã xóa đề thi thành công!', 'ok');
        loadQuizzes();
    } catch (e) {
        showToast(e.message || 'Lỗi khi xóa đề thi', 'err');
    }
}

function esc(s) {
    if (!s) return '';
    const d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
}

function showToast(msg, type = 'ok') {
    const wrap = document.getElementById('toastWrap');
    if (!wrap) return;

    const el = document.createElement('div');
    el.className = `toast-msg ${type}`;
    el.innerHTML = `<i class="bi ${type === 'ok' ? 'bi-check-circle-fill' : 'bi-exclamation-circle-fill'}"></i> <span>${msg}</span>`;

    wrap.appendChild(el);
    setTimeout(() => el.classList.add('show'), 10);
    setTimeout(() => {
        el.classList.remove('show');
        setTimeout(() => el.remove(), 300);
    }, 3000);
}

document.addEventListener('DOMContentLoaded', () => {
    const select = document.getElementById('classroomIdSelect');
    if (select) {
        select.addEventListener('change', async function () {
            const classId = this.value;
            const topicContainer = document.getElementById('topicDropdownContainer');
            const topicSelect = document.getElementById('assignTopicId');

            topicSelect.innerHTML = '<option value="" selected>-- Không chọn chủ đề --</option>';
            topicContainer.style.display = 'none';

            if (!classId) return;

            try {
                const topics = await apiClient.get('/api/teacher/class-topics/classroom/' + classId);
                if (topics && topics.length > 0) {
                    topics.forEach(t => {
                        const opt = document.createElement('option');
                        opt.value = t.id;
                        opt.textContent = t.name;
                        topicSelect.appendChild(opt);
                    });
                    topicContainer.style.display = 'block';
                }
            } catch (e) {
                console.error('Lỗi khi tải danh sách chủ đề:', e);
            }
        });
    }
});

async function openAssignModal(id, title) {
    document.getElementById('assignQuizId').value = id;
    document.getElementById('assignQuizTitle').value = title;

    document.getElementById('assignTopicId').innerHTML = '<option value="" selected>-- Không chọn chủ đề --</option>';
    document.getElementById('topicDropdownContainer').style.display = 'none';

    // Set default dates
    const now = new Date();
    const due = new Date();
    due.setDate(now.getDate() + 7);
    due.setHours(23, 59, 0, 0);

    // Update Flatpickr values
    const startPicker = document.querySelector("#assignStartDate")._flatpickr;
    const duePicker = document.querySelector("#assignDueDate")._flatpickr;
    if (startPicker) startPicker.setDate(now);
    if (duePicker) duePicker.setDate(due);

    // Fetch classrooms
    const select = document.getElementById('classroomIdSelect');
    select.innerHTML = '<option value="" disabled selected>-- Chọn lớp học --</option>';

    try {
        const classrooms = await apiClient.get('/api/teacher/classrooms');
        classrooms.forEach(c => {
            const opt = document.createElement('option');
            opt.value = c.id;
            opt.textContent = `${c.name} (${c.code})`;
            select.appendChild(opt);
        });
    } catch (err) {
        console.error(err);
    }

    bootstrap.Modal.getOrCreateInstance(document.getElementById('assignModal')).show();
}

async function confirmAssign() {
    const form = document.getElementById('assignForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const formData = new FormData(form);
    const data = {};
    formData.forEach((value, key) => {
        if (key === 'questionShuffled' || key === 'answerShuffled' || key === 'showAnswer') {
            data[key] = true;
        } else {
            data[key] = value;
        }
    });

    // Validation
    const start = new Date(data.startDate);
    const due = new Date(data.dueDate);
    const duration = parseInt(data.durationInMins);

    if (due <= start) {
        alert('Thời gian kết thúc phải sau thời gian bắt đầu!');
        return;
    }

    const windowMins = (due - start) / (1000 * 60);
    if (duration > windowMins) {
        alert(`Thời gian làm bài (${duration} phút) không được vượt quá khoảng thời gian mở đề (${Math.floor(windowMins)} phút)!`);
        return;
    }

    if (!data.questionShuffled) data.questionShuffled = false;
    if (!data.answerShuffled) data.answerShuffled = false;
    if (!data.showAnswer) data.showAnswer = false;

    try {
        await apiClient.post('/api/teacher/quiz-assigning', data);
        bootstrap.Modal.getInstance(document.getElementById('assignModal')).hide();
        showToast('Giao đề thi thành công!', 'ok');
    } catch (err) {
        showToast(err.message || 'Lỗi khi giao đề thi. Hãy thử lại!', 'err');
    }
}

async function openPreviewModal(id, title) {
    document.getElementById('preview-quiz-title').textContent = title;
    document.getElementById('preview-quiz-desc').textContent = 'Đang tải thông tin đề thi...';
    document.getElementById('preview-quiz-count').textContent = '0';
    document.getElementById('preview-quiz-questions').innerHTML = `
        <div class="text-center py-4">
            <div class="spinner-border text-primary" role="status">
                <span class="visually-hidden">Loading...</span>
            </div>
        </div>`;

    bootstrap.Modal.getOrCreateInstance(document.getElementById('previewQuizModal')).show();

    try {
        const quiz = await apiClient.get(`/api/teacher/quizzes/${id}`);
        document.getElementById('preview-quiz-title').textContent = quiz.title || '(Chưa có tiêu đề)';
        document.getElementById('preview-quiz-desc').textContent = quiz.description || 'Chưa có mô tả.';
        document.getElementById('preview-quiz-count').textContent = quiz.questions ? quiz.questions.length : 0;

        if (!quiz.questions || quiz.questions.length === 0) {
            document.getElementById('preview-quiz-questions').innerHTML = `
                <div class="text-center py-4 text-muted">
                    <i class="bi bi-file-earmark-x d-block mb-2" style="font-size: 2rem;"></i>
                    Đề thi này chưa có câu hỏi nào.
                </div>`;
            return;
        }

        document.getElementById('preview-quiz-questions').innerHTML = quiz.questions.map((q, idx) => {
            const levelBadge = q.level === 'EASY' 
                ? '<span class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1">Dễ</span>'
                : q.level === 'MEDIUM'
                ? '<span class="badge bg-warning-subtle text-warning border border-warning-subtle px-2 py-1">Trung bình</span>'
                : '<span class="badge bg-danger-subtle text-danger border border-danger-subtle px-2 py-1">Khó</span>';

            const typeLabel = q.type === 'SINGLE_CHOICE'
                ? 'Trắc nghiệm 1 đáp án'
                : q.type === 'MULTIPLE_CHOICE'
                ? 'Trắc nghiệm nhiều đáp án'
                : 'Điền khuyết';

            const answersHtml = q.answers && q.answers.length > 0
                ? `<div class="mt-2 d-flex flex-column gap-2">
                    ${q.answers.map(ans => {
                        const icon = ans.isCorrect
                            ? '<i class="bi bi-check-circle-fill text-success"></i>'
                            : '<i class="bi bi-circle text-muted"></i>';
                        const textStyle = ans.isCorrect
                            ? 'fw-bold text-success'
                            : '';
                        return `<div class="d-flex align-items-center gap-2 p-2 rounded bg-light border border-light-subtle" style="font-size:0.88rem;">
                            ${icon} <span class="${textStyle}">${esc(ans.text)}</span>
                        </div>`;
                    }).join('')}
                   </div>`
                : '';

            return `
            <div class="card border border-light-subtle rounded-3 p-3 shadow-sm bg-white">
                <div class="d-flex align-items-center justify-content-between mb-2">
                    <span class="text-muted fw-bold" style="font-size: 0.8rem;">Câu hỏi ${idx + 1} (${typeLabel})</span>
                    ${levelBadge}
                </div>
                <div class="fw-bold text-dark mb-2" style="font-size: 0.95rem; line-height: 1.5;">${esc(q.text)}</div>
                ${answersHtml}
            </div>`;
        }).join('');

    } catch (e) {
        document.getElementById('preview-quiz-desc').textContent = 'Có lỗi xảy ra khi tải dữ liệu.';
        document.getElementById('preview-quiz-questions').innerHTML = `
            <div class="text-center py-4 text-danger">
                <i class="bi bi-exclamation-circle-fill d-block mb-2" style="font-size: 2rem;"></i>
                Không thể tải câu hỏi của đề thi. Vui lòng thử lại!
            </div>`;
    }
}
