/**
 * SmartAgri - Common Utility Functions
 * Reusable helper functions for UI formatting, toasts, session checks, and notifications.
 */

const Utils = (function () {

    /**
     * Display a Bootstrap floating toast notification.
     * @param {string} message - Message text to display.
     * @param {string} type - 'success', 'danger', 'warning', 'info' (default: 'success').
     */
    function showToast(message, type = 'success') {
        // Ensure container exists
        let toastContainer = document.getElementById('smartagriToastContainer');
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'smartagriToastContainer';
            toastContainer.className = 'toast-container position-fixed bottom-0 end-0 p-3';
            toastContainer.style.zIndex = '9999';
            document.body.appendChild(toastContainer);
        }

        const iconMap = {
            success: 'bi-check-circle-fill',
            danger: 'bi-exclamation-triangle-fill',
            warning: 'bi-exclamation-circle-fill',
            info: 'bi-info-circle-fill'
        };

        const icon = iconMap[type] || 'bi-bell-fill';
        const toastEl = document.createElement('div');
        toastEl.className = `toast align-items-center text-bg-${type} border-0 shadow-sm mb-2`;
        toastEl.setAttribute('role', 'alert');
        toastEl.setAttribute('aria-live', 'assertive');
        toastEl.setAttribute('aria-atomic', 'true');

        toastEl.innerHTML = `
            <div class="d-flex">
                <div class="toast-body d-flex align-items-center gap-2">
                    <i class="bi ${icon} fs-5"></i>
                    <span>${message}</span>
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        `;

        toastContainer.appendChild(toastEl);
        if (typeof bootstrap !== 'undefined' && bootstrap.Toast) {
            const bsToast = new bootstrap.Toast(toastEl, { delay: 3500 });
            bsToast.show();
            toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
        } else {
            // Fallback if Bootstrap JS is not yet loaded
            setTimeout(() => toastEl.remove(), 3500);
        }
    }

    /**
     * Format a numeric amount to Indian Rupee (INR) currency format.
     * Example: 12500 -> "₹12,500.00"
     */
    function formatCurrency(amount, includeDecimals = true) {
        const num = Number(amount);
        if (isNaN(num)) return '₹0.00';
        return '₹' + num.toLocaleString('en-IN', {
            minimumFractionDigits: includeDecimals ? 2 : 0,
            maximumFractionDigits: 2
        });
    }

    /**
     * Format a date string (YYYY-MM-DD or ISO) into readable localized format.
     * Example: "2026-09-26" -> "26 Sep 2026"
     */
    function formatDate(dateInput) {
        if (!dateInput) return '—';
        const date = new Date(dateInput);
        if (isNaN(date.getTime())) return dateInput;
        return date.toLocaleDateString('en-IN', {
            day: '2-digit',
            month: 'short',
            year: 'numeric'
        });
    }

    /**
     * Set a button into a loading state (disabled with spinner).
     */
    function setButtonLoading(button, isLoading, loadingText = 'Processing...') {
        if (!button) return;
        if (isLoading) {
            button.dataset.originalHtml = button.innerHTML;
            button.disabled = true;
            button.innerHTML = `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>${loadingText}`;
        } else {
            if (button.dataset.originalHtml) {
                button.innerHTML = button.dataset.originalHtml;
            }
            button.disabled = false;
        }
    }

    /**
     * Retrieve the currently logged in user session.
     */
    function getSessionUser() {
        try {
            const key = (typeof CONFIG !== 'undefined' && CONFIG.STORAGE_KEYS?.USER_SESSION)
                ? CONFIG.STORAGE_KEYS.USER_SESSION
                : 'smartagriUser';
            const raw = localStorage.getItem(key);
            return raw ? JSON.parse(raw) : null;
        } catch (e) {
            console.warn('[Utils] Error reading session user', e);
            return null;
        }
    }

    /**
     * Check if a user is currently authenticated.
     */
    function isAuthenticated() {
        return getSessionUser() !== null;
    }

    /**
     * Check if the authenticated user has a specific role.
     */
    function hasRole(roleName) {
        const user = getSessionUser();
        return user && user.role === roleName;
    }

    /**
     * Safe HTML escaper to prevent XSS in dynamic rendering.
     */
    function escapeHtml(str) {
        if (!str) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    return {
        showToast,
        formatCurrency,
        formatDate,
        setButtonLoading,
        getSessionUser,
        isAuthenticated,
        hasRole,
        escapeHtml
    };
})();

// Attach globally for convenience and backward compatibility
window.Utils = Utils;
window.toast = Utils.showToast;
