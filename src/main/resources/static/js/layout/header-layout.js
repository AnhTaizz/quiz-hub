function doLogout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('user');
    document.cookie = 'jwt=; path=/; max-age=0;';
    window.location.href = '/login';
}

// Robust implementation for initializing the sidebar toggle
function initSidebarToggle() {
    // 1. Restore state immediately on initialization
    if (localStorage.getItem('sidebar-collapsed') === 'true') {
        document.body.classList.add('sidebar-collapsed');
    }

    // 2. Find and bind to the toggle button
    const toggleBtn = document.getElementById('sidebarToggle') || document.querySelector('.toggle-btn');
    if (toggleBtn) {
        // Remove listener if already exists (safety precaution)
        toggleBtn.removeEventListener('click', handleSidebarToggle);
        toggleBtn.addEventListener('click', handleSidebarToggle);
    }
}

function handleSidebarToggle(e) {
    e.preventDefault();
    const isCollapsed = document.body.classList.toggle('sidebar-collapsed');
    localStorage.setItem('sidebar-collapsed', isCollapsed);
}

// Execute immediately if DOM is already ready, otherwise wait for event
if (document.readyState !== 'loading') {
    initSidebarToggle();
} else {
    document.addEventListener('DOMContentLoaded', initSidebarToggle);
}
