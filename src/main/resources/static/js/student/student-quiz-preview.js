const quizId = document.getElementById('quizIdInput').value;

document.addEventListener('DOMContentLoaded', async () => {
    if (quizId) {
        await loadQuizDetails();
    }
});

async function loadQuizDetails() {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    try {
        const res = await fetch(`/api/student/quiz/${quizId}`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        if (res.ok) {
            const q = await res.json();
            renderQuizMeta(q);
            renderQuizQuestions(q.questions || []);
        }
    } catch (err) {
        console.error('Lỗi tải đề thi', err);
    }
}

function renderQuizMeta(q) {
    const wrap = document.getElementById('quiz-meta-wrapper');
    const typeBadge = q.isExam
        ? `<span class="qbadge qbadge-exam"><i class="bi bi-mortarboard-fill"></i> Kiểm tra</span>`
        : `<span class="qbadge qbadge-quiz"><i class="bi bi-journal-check"></i> Luyện tập</span>`;

    wrap.innerHTML = `
        ${q.imageUrl ? `<img src="${q.imageUrl}" alt="Quiz Image" class="img-fluid rounded mb-3" style="max-height:180px; object-fit:cover; width:100%;">` : ''}
        <div class="d-flex gap-2 mb-3">${typeBadge}</div>
        <h3 class="fw-bold text-dark" style="font-family:'Nunito Sans'; font-size:1.35rem;">${esc(q.title)}</h3>
        <p class="text-muted small mb-3">${esc(q.description) || '<i>Không có mô tả.</i>'}</p>
        <hr>
        <div class="d-flex flex-column gap-2">
            <div class="d-flex justify-content-between"><span class="text-muted small fw-bold">Tổng số câu hỏi:</span> <span class="fw-bold">${q.questions ? q.questions.length : (q.questionCount || 0)} câu</span></div>
            <div class="d-flex justify-content-between"><span class="text-muted small fw-bold">Danh mục:</span> <span class="fw-bold">${q.category ? esc(q.category.name) : 'Hệ thống'}</span></div>
            <div class="d-flex justify-content-between"><span class="text-muted small fw-bold">Tạo bởi:</span> <span class="fw-bold">${esc(q.creatorName) || 'Bạn'}</span></div>
        </div>
    `;
}

function renderQuizQuestions(questions) {
    const wrap = document.getElementById('quiz-questions-wrapper');
    if (!questions || questions.length === 0) {
        wrap.innerHTML = `<p class="text-muted text-center py-4">Đề thi này chưa có câu hỏi nào.</p>`;
        return;
    }

    wrap.innerHTML = questions.map((q, i) => {
        let typeLabel = q.type === 'SINGLE_CHOICE' ? 'Trắc nghiệm 1 đáp án' : (q.type === 'MULTIPLE_CHOICE' ? 'Trắc nghiệm nhiều đáp án' : 'Điền khuyết');
        let levelLabel = q.level === 'EASY' ? 'Dễ' : (q.level === 'MEDIUM' ? 'Trung bình' : 'Khó');

        return `
            <div class="mb-4 bg-white p-3 border rounded shadow-sm">
                <div class="d-flex justify-content-between align-items-center mb-2">
                    <span class="text-indigo fw-bold">Câu hỏi ${i + 1}</span>
                    <div class="d-flex gap-2">
                        <span class="badge bg-light text-dark border px-2 py-1">${typeLabel}</span>
                        <span class="badge bg-light text-dark border px-2 py-1">${levelLabel}</span>
                    </div>
                </div>
                <div class="fw-bold mb-3" style="font-size:1rem; color:#1e293b;">${esc(q.text)}</div>
                <div class="ans-list">
                    ${q.answers && q.answers.length > 0 ? q.answers.map((a, idx) => `
                        <div class="ans-row ${a.isCorrect ? 'correct' : ''}">
                            <div class="d-flex align-items-center gap-2">
                                <span class="fw-bold">${String.fromCharCode(65 + idx)}.</span>
                                <span>${esc(a.text)}</span>
                                ${a.isCorrect ? `<i class="bi bi-check-circle-fill ms-auto"></i>` : ''}
                            </div>
                        </div>
                    `).join('') : '<p class="text-muted small">Không có đáp án.</p>'}
                </div>
            </div>
        `;
    }).join('');
}

function esc(s) { const d = document.createElement('div'); d.textContent = s || ''; return d.innerHTML; }
