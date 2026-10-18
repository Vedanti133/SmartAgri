/**
 * SmartAgri - Common Application Initializer
 * Coordinates page initialization, session verification, and shared UI helpers.
 */

document.addEventListener('DOMContentLoaded', () => {
    // Enforce session check on protected pages
    if (typeof requireLogin === 'function') {
        requireLogin();
    }

    // Apply logged-in user profile to UI
    if (typeof applyUser === 'function') {
        applyUser();
    }

    // Initialize all Bootstrap tooltips if bootstrap is loaded
    if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
        document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(el => {
            new bootstrap.Tooltip(el);
        });
    }
});

/**
 * Backward-compatible api() helper that delegates to ApiClient
 */
async function api(path, options = {}) {
    if (typeof ApiClient !== 'undefined') {
        return ApiClient.request(path, options);
    }
    // Fallback implementation if api.js is not loaded
    const base = (typeof CONFIG !== 'undefined' && CONFIG.API_BASE_URL) ? CONFIG.API_BASE_URL : (typeof API_URL !== 'undefined' ? API_URL : 'http://localhost:8080/api');
    const url = path.startsWith('http') ? path : `${base}${path.startsWith('/') ? path : '/' + path}`;
    const r = await fetch(url, {
        headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
        ...options
    });
    const d = await r.json().catch(() => ({}));
    if (!r.ok) throw new Error(d.message || 'Request failed');
    return d;
}

/**
 * Backward-compatible toast() helper that delegates to Utils.showToast
 */
function toast(msg, type = 'success') {
    if (typeof Utils !== 'undefined' && typeof Utils.showToast === 'function') {
        Utils.showToast(msg, type);
        return;
    }
    const el = document.createElement('div');
    el.className = `toast align-items-center text-bg-${type} border-0 position-fixed bottom-0 end-0 m-3`;
    el.innerHTML = `<div class="d-flex"><div class="toast-body">${msg}</div><button class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>`;
    document.body.appendChild(el);
    if (typeof bootstrap !== 'undefined' && bootstrap.Toast) {
        new bootstrap.Toast(el).show();
    }
}
