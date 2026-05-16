(function() {
    // --- Configurations ---
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const getClassroomId = () => window.CLASSROOM_ID || 0;

    // --- Common Utilities ---
    function escapeJS(str) {
        if (!str) return "";
        return str.replace(/'/g, "\\'");
    }

    function escapeHTML(str) {
        if (!str) return '';
        return str.replace(/[&<>'"]/g, 
            tag => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', "'": '&#39;', '"': '&quot;' }[tag] || tag)
        );
    }
    const esc = escapeHTML;

    function showToast(msg, type = 'ok') {
        const w = document.getElementById('toastWrap') || document.body;
        const t = document.createElement('div');
        t.className = `toast-msg ${type}`;
        t.style = `position:fixed; top:20px; right:20px; z-index:9999; padding:12px 24px; border-radius:12px; background:#10b981; color:#fff; font-weight:700; opacity:0; transition:all 0.3s ease; box-shadow:0 10px 25px rgba(0,0,0,0.15);`;
        if (type === 'error') t.style.background = '#ef4444';
        t.innerHTML = `<i class="bi ${type === 'ok' ? 'bi-check-circle-fill' : 'bi-exclamation-circle-fill'} me-2"></i> ${msg}`;
        w.appendChild(t);
        setTimeout(() => t.style.opacity = '1', 10);
        setTimeout(() => { 
            t.style.opacity = '0'; 
            setTimeout(() => t.remove(), 300); 
        }, 2500);
    }

    function getAllChildIds(catId, categories) {
        let ids = [parseInt(catId)];
        const findAndAdd = (list) => {
            for (let cat of list) {
                if (cat.id == catId) {
                    const addChildren = (children) => {
                        for (let child of children) {
                            ids.push(child.id);
                            if (child.children) addChildren(child.children);
                        }
                    };
                    if (cat.children) addChildren(cat.children);
                    return true;
                }
                if (cat.children && findAndAdd(cat.children)) return true;
            }
            return false;
        };
        findAndAdd(categories);
        return ids;
    }

    // --- Category & Assignment Management ---
    window.handleCategorySelection = function(id, name, target) {
        if (id === null) {
            document.getElementById('categoryFilterValue').value = '';
            document.getElementById('categoryFilterDisplay').value = 'Tất cả danh mục';
            const clearBtn = document.getElementById('clearCatBtn');
            if (clearBtn) clearBtn.style.display = 'none';
            window.filterQuizzesByCategory('');
            return;
        }
        
        if (id === -1) {
            document.getElementById('categoryFilterValue').value = -1;
            document.getElementById('categoryFilterDisplay').value = 'Chưa phân mục';
            const clearBtn = document.getElementById('clearCatBtn');
            if (clearBtn) clearBtn.style.display = 'inline-block';
            window.filterQuizzesByCategory(-1);
            return;
        }
        
        document.getElementById('categoryFilterValue').value = id;
        document.getElementById('categoryFilterDisplay').value = name;
        const clearBtn = document.getElementById('clearCatBtn');
        if (clearBtn) clearBtn.style.display = 'inline-block';

        window.filterQuizzesByCategory(id);
    };

    window.clearCategoryFilter = function() {
        document.getElementById('categoryFilterValue').value = '';
        document.getElementById('categoryFilterDisplay').value = '';
        const clearBtn = document.getElementById('clearCatBtn');
        if (clearBtn) clearBtn.style.display = 'none';
        window.filterQuizzesByCategory('');
    };

    window.filterQuizzesByCategory = function(catId) {
        const select = document.getElementById('quizSelect');
        if (!select) return;
        const options = select.querySelectorAll('option');

        select.value = "";

        if (catId === null || catId === undefined || catId === '') {
            options.forEach(opt => opt.style.display = 'block');
            return;
        }

        // Specialized handling for 'No category' (-1)
        if (catId === -1) {
            options.forEach(opt => {
                if (opt.value === "") {
                    opt.style.display = 'block';
                    return;
                }
                const optCatId = opt.getAttribute('data-category-id');
                if (!optCatId || optCatId.trim() === '') {
                    opt.style.display = 'block';
                } else {
                    opt.style.display = 'none';
                }
            });
            return;
        }

        // Get all child IDs recursively
        const listToCheck = [...(window.myCategories || []), ...(window.publicCategories || [])];
        const childIds = getAllChildIds(catId, listToCheck);

        options.forEach(opt => {
            if (opt.value === "") {
                opt.style.display = 'block';
                return;
            }
            const optCatIdStr = opt.getAttribute('data-category-id');
            if (!optCatIdStr || optCatIdStr.trim() === '') {
                opt.style.display = 'none';
                return;
            }
            
            const optCatId = parseInt(optCatIdStr);
            if (!isNaN(optCatId) && childIds.includes(optCatId)) {
                opt.style.display = 'block';
            } else {
                opt.style.display = 'none';
            }
        });
    };

    window.submitAssignment = function() {
        const form = document.getElementById('assignQuizForm');
        if (!form) return;
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

        fetch('/api/teacher/quiz-assigning', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: 'Bearer ' + token
            },
            body: JSON.stringify(data)
        })
        .then(res => {
            if (res.ok) {
                alert('Giao đề thi thành công!');
                location.reload();
            } else {
                alert('Đã có lỗi xảy ra. Hãy thử lại!');
            }
        })
        .catch(err => {
            console.error(err);
            alert('Lỗi kết nối. Vui lòng kiểm tra lại!');
        });
    };

    // --- Topic CRUD Operations ---
    window.createTopic = async function() {
        const newTopicInput = document.getElementById('newTopicName');
        if (!newTopicInput) return;
        const name = newTopicInput.value.trim();
        const classroomId = getClassroomId();
        if (!name) return;

        try {
            const res = await fetch('/api/teacher/class-topics', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: 'Bearer ' + token
                },
                body: JSON.stringify({ name, classroomId })
            });
            if (res.ok) {
                showToast('Thêm chủ đề thành công!');
                setTimeout(() => { location.reload(); }, 1200);
            } else {
                showToast('Có lỗi xảy ra khi tạo chủ đề.', 'error');
            }
        } catch (e) {
            showToast('Lỗi kết nối mạng.', 'error');
        }
    };

    window.deleteTopic = async function(topicId) {
        if (!confirm('Bạn có chắc chắn muốn xóa chủ đề này?')) return;

        try {
            const res = await fetch('/api/teacher/class-topics/' + topicId, {
                method: 'DELETE',
                headers: { Authorization: 'Bearer ' + token }
            });
            if (res.ok) {
                showToast('Đã xóa chủ đề thành công!');
                setTimeout(() => { location.reload(); }, 1200);
            } else {
                showToast('Có lỗi xảy ra khi xóa chủ đề.', 'error');
            }
        } catch (e) {
            showToast('Lỗi kết nối mạng.', 'error');
        }
    };

    window.editTopic = async function(topicId, currentName) {
        const newName = prompt('Nhập tên chủ đề mới:', currentName);
        if (!newName || newName.trim() === '' || newName.trim() === currentName) return;

        const classroomId = getClassroomId();

        try {
            const res = await fetch('/api/teacher/class-topics/' + topicId, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: 'Bearer ' + token
                },
                body: JSON.stringify({ name: newName.trim(), classroomId })
            });
            if (res.ok) {
                showToast('Đã sửa chủ đề thành công!');
                setTimeout(() => { location.reload(); }, 1200);
            } else {
                showToast('Có lỗi xảy ra khi sửa chủ đề.', 'error');
            }
        } catch (e) {
            showToast('Lỗi kết nối mạng.', 'error');
        }
    };

    // --- Classroom Management ---
    window.updateClassroom = async function() {
        const nameEl = document.getElementById('editClassName');
        const descEl = document.getElementById('editClassDesc');
        const approveEl = document.getElementById('editRequireApproval');
        if (!nameEl) return;

        const name = nameEl.value.trim();
        const description = descEl ? descEl.value.trim() : '';
        const requireApproval = approveEl ? approveEl.checked : false;
        const classroomId = getClassroomId();

        if (!name) {
            alert('Tên lớp học không được để trống.');
            return;
        }

        try {
            const res = await fetch('/api/teacher/classrooms/' + classroomId, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    Authorization: 'Bearer ' + token
                },
                body: JSON.stringify({ name, description, requireApproval })
            });

            if (res.ok) {
                alert('Cập nhật lớp học thành công!');
                location.reload();
            } else {
                alert('Đã xảy ra lỗi khi cập nhật thông tin lớp học.');
            }
        } catch (e) {
            alert('Lỗi kết nối mạng.');
        }
    };

    window.deleteClassroom = async function() {
        if (!confirm('Bạn có chắc chắn muốn xóa lớp học này không? Hành động này không thể hoàn tác.')) return;

        const classroomId = getClassroomId();

        try {
            const res = await fetch('/api/teacher/classrooms/' + classroomId, {
                method: 'DELETE',
                headers: { Authorization: 'Bearer ' + token }
            });

            if (res.ok) {
                alert('Đã xóa lớp học thành công!');
                window.location.href = '/teacher/classrooms';
            } else {
                alert('Đã xảy ra lỗi khi xóa lớp học.');
            }
        } catch (e) {
            alert('Lỗi kết nối mạng.');
        }
    };

    // --- Visual Actions / Modals ---
    window.openQuizPreviewModal = async function(id, event) {
        if (event) event.preventDefault();
        try {
            const res = await fetch(`/api/teacher/quizzes/${id}`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            if (res.ok) {
                const q = await res.json();
                const titleEl = document.getElementById('preview-quiz-title');
                const descEl = document.getElementById('preview-quiz-desc');
                const countEl = document.getElementById('preview-quiz-count');
                const container = document.getElementById('preview-quiz-questions');
                
                if (titleEl) titleEl.textContent = q.title || 'Chưa có tiêu đề';
                if (descEl) descEl.textContent = q.description || 'Chưa có mô tả.';

                if (container) {
                    container.innerHTML = '';
                    const list = q.questions || [];
                    if (countEl) countEl.textContent = list.length;

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
                }

                const modalEl = document.getElementById('previewQuizModal');
                if (modalEl) {
                    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
                    modal.show();
                }
            }
        } catch (err) {
            console.error('Lỗi tải đề thi', err);
        }
    };

    window.openTopicDetailsModal = function(topicId, topicName) {
        const titleEl = document.getElementById('topicModalTitle');
        const container = document.getElementById('topicModalBody');
        if (titleEl) titleEl.innerText = 'Chủ đề: ' + topicName;
        if (!container) return;

        container.innerHTML = '';

        const matches = document.querySelectorAll(`.assigned-card[data-topic-id="${topicId}"]`);
        if (matches.length === 0) {
            container.innerHTML = '<div class="text-center text-muted py-4"><i class="bi bi-folder-x fs-2 text-muted d-block mb-2"></i> Chưa có đề thi nào trong chủ đề này.</div>';
        } else {
            let html = '<div class="d-flex flex-column gap-3">';
            matches.forEach((el, idx) => {
                const titleEl = el.querySelector('.assigned-title');
                const titleText = titleEl ? titleEl.textContent : 'Đề thi';
                const id = `quizCollapse_${idx}`;

                const originalActionButtons = el.querySelector('.d-flex.gap-2')?.innerHTML || '';
                const metaEl = el.querySelector('.assigned-meta')?.innerHTML || '';
                const badgeEl = el.querySelector('.badge-status')?.innerHTML || '';
                const topicBadgeEl = el.querySelector('.badge.bg-light')?.outerHTML || '';

                html += `
                <div class="card border rounded-3 bg-white p-3 shadow-sm mb-2" style="border-color:#e2e8f0 !important;">
                    <div class="d-flex justify-content-between align-items-center" style="cursor:pointer;" data-bs-toggle="collapse" data-bs-target="#${id}">
                        <div>
                            <span class="badge bg-primary bg-opacity-10 text-primary px-2 py-1 me-2 rounded" style="font-size:11px; font-weight:700;">${badgeEl || 'Đề thi'}</span>
                            ${topicBadgeEl}
                            <span class="fw-bold" style="font-size: 1.1rem; color:#1e1b4b;">${titleText}</span>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <i class="bi bi-chevron-down fs-5 text-muted transition-all" style="transition: transform 0.3s;" id="chevron_${id}"></i>
                        </div>
                    </div>
                    <div id="${id}" class="collapse mt-3 pt-3 border-top" style="border-top:1px solid #f1f5f9 !important;">
                        <div class="mb-3" style="font-size:13px; color:#475569;">
                            ${metaEl}
                        </div>
                        <div class="d-flex gap-2">
                            ${originalActionButtons}
                        </div>
                    </div>
                </div>`;
            });
            html += '</div>';
            container.innerHTML = html;

            matches.forEach((el, idx) => {
                const id = `quizCollapse_${idx}`;
                const collapseEl = document.getElementById(id);
                if (collapseEl) {
                    collapseEl.addEventListener('show.bs.collapse', () => {
                        const chevron = document.getElementById(`chevron_${id}`);
                        if (chevron) chevron.style.transform = 'rotate(180deg)';
                    });
                    collapseEl.addEventListener('hide.bs.collapse', () => {
                        const chevron = document.getElementById(`chevron_${id}`);
                        if (chevron) chevron.style.transform = 'rotate(0deg)';
                    });
                }
            });
        }

        const modalEl = document.getElementById('viewTopicQuizzesModal');
        if (modalEl) {
            const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
            modal.show();
        }
    };

    // --- Initialization ---
    document.addEventListener('DOMContentLoaded', () => {
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
})();
