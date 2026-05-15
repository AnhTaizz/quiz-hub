function togglePwdVisibility(inputId, btn) {
    const input = document.getElementById(inputId);
    const icon = btn.querySelector('i');
    if (input.type === 'password') {
        input.type = 'text';
        icon.className = 'bi bi-eye-slash';
    } else {
        input.type = 'password';
        icon.className = 'bi bi-eye';
    }
}

function showCpwAlert(msg, isSuccess) {
    if (isSuccess) {
        if (window.toast && typeof window.toast.success === 'function') window.toast.success(msg);
        else alert(msg);
    } else {
        if (window.toast && typeof window.toast.error === 'function') window.toast.error(msg);
        else alert(msg);
    }
}

async function submitChangePassword() {
    const current = document.getElementById('cpw-current').value.trim();
    const newPwd = document.getElementById('cpw-new').value.trim();
    const confirm = document.getElementById('cpw-confirm').value.trim();

    if (!current || !newPwd || !confirm) { showCpwAlert('Vui lòng điền đầy đủ các trường!', false); return; }
    if (newPwd.length < 6) { showCpwAlert('Mật khẩu mới phải ít nhất 6 ký tự!', false); return; }
    if (newPwd !== confirm) { showCpwAlert('Mật khẩu xác nhận không khớp!', false); return; }

    const btn = document.getElementById('cpw-submit-btn');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Đang xử lý...';

    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    try {
        const res = await fetch('/api/users/change-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + token },
            body: JSON.stringify({ oldPassword: current, newPassword: newPwd, confirmNewPassword: confirm })
        });
        const text = await res.text();
        if (res.ok) {
            showCpwAlert('Đổi mật khẩu thành công!', true);
            setTimeout(() => {
                document.getElementById('cpw-current').value = '';
                document.getElementById('cpw-new').value = '';
                document.getElementById('cpw-confirm').value = '';
                document.getElementById('cpw-alert').style.display = 'none';
                
                const modalEl = document.getElementById('changePasswordModal');
                if (modalEl && window.bootstrap && window.bootstrap.Modal) {
                    const modalInstance = window.bootstrap.Modal.getInstance(modalEl);
                    if (modalInstance) modalInstance.hide();
                }
            }, 1500);
        } else {
            let errorMsg = text;
            try {
                const json = JSON.parse(text);
                errorMsg = json.message || text;
            } catch (e) { }

            let msg = errorMsg || 'Có lỗi xảy ra!';
            if (errorMsg.includes('hiện tại') || errorMsg.includes('incorrect')) {
                msg = 'Mật khẩu hiện tại không đúng!';
            } else if (errorMsg.includes('same as the old password') || errorMsg.includes('PASSWORD_SAME')) {
                msg = 'Mật khẩu mới không được trùng với mật khẩu hiện tại!';
            }
            showCpwAlert(msg, false);
        }
    } catch (e) {
        showCpwAlert('Lỗi kết nối mạng!', false);
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check-lg me-1"></i> Cập nhật';
    }
}

// Reset form khi đóng modal
document.addEventListener('DOMContentLoaded', () => {
    const modal = document.getElementById('changePasswordModal');
    if (modal) {
        modal.addEventListener('hidden.bs.modal', () => {
            document.getElementById('cpw-current').value = '';
            document.getElementById('cpw-new').value = '';
            document.getElementById('cpw-confirm').value = '';
            const alertEl = document.getElementById('cpw-alert');
            if (alertEl) alertEl.style.display = 'none';
        });
    }
});
