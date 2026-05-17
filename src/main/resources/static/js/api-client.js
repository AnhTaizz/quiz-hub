/**
 * QuizHub Unified API Client
 * Automatic Bearer Token injection, JSON handling, and global error intercepting.
 */
const apiClient = (() => {
    const showGlobalError = (message) => {
        if (window.toast && typeof window.toast.error === 'function') {
            window.toast.error(message);
        } else {
            console.error('QuizHub API Error:', message);
            // alert will fall back to toast if toast.js is loaded
            alert(message);
        }
    };

    const request = async (url, options = {}) => {
        const token = localStorage.getItem('token') || sessionStorage.getItem('token');
        
        // Setup default headers
        options.headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        if (token) {
            options.headers['Authorization'] = `Bearer ${token}`;
        }

        try {
            const response = await fetch(url, options);

            // Handle unauthorized / expired tokens globally
            if (response.status === 401 || response.status === 403) {
                localStorage.removeItem('token');
                sessionStorage.removeItem('token');
                showGlobalError('Phiên làm việc đã hết hạn hoặc không có quyền truy cập. Đang chuyển hướng về trang đăng nhập...');
                setTimeout(() => {
                    window.location.href = '/login';
                }, 1500);
                return Promise.reject(new Error('Unauthorized or Forbidden access.'));
            }

            // Handle empty responses (like HTTP 204 No Content)
            if (response.status === 204) {
                return null;
            }

            const contentType = response.headers.get('content-type');
            let data = null;

            if (contentType && contentType.includes('application/json')) {
                data = await response.json();
            } else {
                data = await response.text();
            }

            if (!response.ok) {
                // If it's a backend ErrorResponse with a message
                const errMsg = (data && data.message) || `Đã xảy ra lỗi hệ thống (Mã lỗi: ${response.status})`;
                showGlobalError(errMsg);
                return Promise.reject(data || new Error(errMsg));
            }

            return data;
        } catch (error) {
            console.error('Fetch error:', error);
            showGlobalError('Không thể kết nối đến máy chủ. Vui lòng kiểm tra lại đường truyền mạng.');
            throw error;
        }
    };

    return {
        get: (url, headers = {}) => request(url, { method: 'GET', headers }),
        post: (url, data, headers = {}) => request(url, { method: 'POST', body: JSON.stringify(data), headers }),
        put: (url, data, headers = {}) => request(url, { method: 'PUT', body: JSON.stringify(data), headers }),
        delete: (url, headers = {}) => request(url, { method: 'DELETE', headers }),
        request: request
    };
})();

// Export to global scope
window.apiClient = apiClient;
