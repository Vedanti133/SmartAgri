/**
 * SmartAgri - Authentication & Session State Management
 * Provides user retrieval, role checks, role-based sidebar navigation filtering,
 * route guards, and session state.
 */

// -------------------------------------------------------------
// Role to Permitted Pages Mapping
// -------------------------------------------------------------
const ROLE_PAGE_PERMISSIONS = {
    ADMIN: ['*'], // Full access to all administrative & business pages
    SALES_STAFF: [
        'admin-orders.html',
        'sales.html',
        'new-sale.html',
        'admin-delivery.html',
        'fulfillment.html',
        'customers.html',
        'fertilizers.html',
        'categories.html',
        'inventory.html',
        'stock-alerts.html',
        'reports.html',
        'customer-catalog.html',
        'profile.html',
        'about.html'
    ],
    SUPPLIER: [
        'procurement.html',
        'purchases.html',
        'new-purchase.html',
        'stock-in.html',
        'suppliers.html',
        'fertilizers.html',
        'profile.html',
        'about.html'
    ],
    DISTRIBUTOR: [
        'customer-catalog.html',
        'customer-cart.html',
        'checkout.html',
        'order-success.html',
        'customer-orders.html',
        'customer-order-tracking.html',
        'invoice.html',
        'customer-invoice.html',
        'returns.html',
        'support.html',
        'transfers.html',
        'warehouses.html',
        'inventory.html',
        'stock-alerts.html',
        'fertilizers.html',
        'profile.html',
        'customer-profile.html',
        'about.html'
    ],
    DEALER: [
        'customer-catalog.html',
        'customer-cart.html',
        'checkout.html',
        'order-success.html',
        'customer-orders.html',
        'customer-order-tracking.html',
        'invoice.html',
        'customer-invoice.html',
        'returns.html',
        'support.html',
        'transfers.html',
        'inventory.html',
        'fertilizers.html',
        'profile.html',
        'customer-profile.html',
        'about.html'
    ],
    RETAILER: [
        'customer-catalog.html',
        'customer-wishlist.html',
        'customer-cart.html',
        'checkout.html',
        'order-success.html',
        'customer-orders.html',
        'customer-order-tracking.html',
        'invoice.html',
        'customer-invoice.html',
        'returns.html',
        'support.html',
        'profile.html',
        'customer-profile.html',
        'about.html'
    ],
    CUSTOMER: [
        'customer-catalog.html',
        'customer-wishlist.html',
        'customer-cart.html',
        'checkout.html',
        'order-success.html',
        'customer-orders.html',
        'customer-order-tracking.html',
        'invoice.html',
        'customer-invoice.html',
        'returns.html',
        'support.html',
        'customer-profile.html',
        'about.html'
    ]
};

function getUser() {
    try {
        const key = (typeof CONFIG !== 'undefined' && CONFIG.STORAGE_KEYS?.USER_SESSION)
            ? CONFIG.STORAGE_KEYS.USER_SESSION
            : 'smartagriUser';
        const raw = localStorage.getItem(key) ||
                    localStorage.getItem('smartagri_user') ||
                    localStorage.getItem('user');
        return raw ? JSON.parse(raw) : null;
    } catch (e) {
        return null;
    }
}

function getToken() {
    try {
        const key = (typeof CONFIG !== 'undefined' && CONFIG.STORAGE_KEYS?.AUTH_TOKEN)
            ? CONFIG.STORAGE_KEYS.AUTH_TOKEN
            : 'smartagriAuthToken';
        return localStorage.getItem(key) ||
               localStorage.getItem('smartagri_auth_token') ||
               localStorage.getItem('token') || '';
    } catch (e) {
        return '';
    }
}

function isAuthenticated() {
    return getUser() !== null;
}

function hasRole(allowedRoles) {
    const user = getUser();
    if (!user || !user.role) return false;
    const userRole = user.role.toUpperCase();
    if (Array.isArray(allowedRoles)) {
        return allowedRoles.some(r => r.toUpperCase() === userRole || (r.toUpperCase() === 'SUPPLIER' && userRole === 'SUPPLIERS'));
    }
    return userRole === allowedRoles.toUpperCase() || (allowedRoles.toUpperCase() === 'SUPPLIER' && userRole === 'SUPPLIERS');
}

function getDefaultLandingPage(role) {
    const r = (role || 'CUSTOMER').toUpperCase();
    if (r === 'ADMIN') return 'admin-dashboard.html';
    if (r === 'SALES_STAFF') return 'admin-orders.html';
    if (r === 'SUPPLIER' || r === 'SUPPLIERS') return 'procurement.html';
    return 'customer-catalog.html';
}

function isPageAllowedForRole(role, pageName) {
    let r = (role || 'CUSTOMER').toUpperCase();
    if (r === 'SUPPLIERS') r = 'SUPPLIER';
    if (r === 'ADMIN') return true;

    const allowed = ROLE_PAGE_PERMISSIONS[r] || [];
    const clean = pageName.toLowerCase().replace(/^.*[\\\/]/, '').split('?')[0].split('#')[0];
    if (clean === '' || clean === 'index.html' || clean === 'about.html') return true;
    return allowed.includes('*') || allowed.includes(clean);
}

function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function requireLogin() {
    const user = getUser();
    const pathname = window.location.pathname.toLowerCase();
    const currentPage = pathname.replace(/^.*[\\\/]/, '') || 'index.html';

    // Whitelist public pages
    const isPublicPage = currentPage === 'login.html' ||
                         currentPage === 'registration.html' ||
                         currentPage === 'about.html' ||
                         currentPage === '404.html' ||
                         currentPage === 'index.html';

    if (!user) {
        if (!isPublicPage) {
            window.location.href = 'login.html';
        }
        return false;
    }

    const role = (user.role || 'CUSTOMER').toUpperCase();

    // If on a public page while logged in, permit viewing
    if (isPublicPage) {
        return true;
    }

    // Role-based route guard
    if (!isPageAllowedForRole(role, currentPage)) {
        console.warn(`[SmartAgri Route Guard] Access denied for role '${role}' on page '${currentPage}'`);
        const landingPage = getDefaultLandingPage(role);

        // Replace content immediately to protect sensitive UI and data
        document.body.innerHTML = `
            <div style="min-height: 100vh; display: flex; align-items: center; justify-content: center; background-color: #f8fafc; font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;">
                <div style="background: white; border-radius: 12px; padding: 2.5rem; max-width: 480px; width: 90%; text-align: center; box-shadow: 0 10px 25px rgba(0,0,0,0.08); border-top: 5px solid #dc2626;">
                    <div style="font-size: 3rem; color: #dc2626; margin-bottom: 1rem;">&#128737;</div>
                    <h2 style="color: #1e293b; font-size: 1.5rem; font-weight: 700; margin-bottom: 0.5rem;">Access Denied</h2>
                    <p style="color: #64748b; font-size: 0.95rem; margin-bottom: 1.25rem;">
                        Your account role (<strong>${escapeHtml(role)}</strong>) is not authorized to access this page.
                    </p>
                    <p style="color: #94a3b8; font-size: 0.85rem; margin-bottom: 1.5rem;">
                        Redirecting to your authorized dashboard...
                    </p>
                    <a href="${landingPage}" style="display: inline-block; background-color: #16a34a; color: white; padding: 0.65rem 1.5rem; border-radius: 6px; text-decoration: none; font-weight: 600;">
                        Go to Dashboard
                    </a>
                </div>
            </div>
        `;
        setTimeout(() => {
            window.location.href = landingPage;
        }, 1200);
        return false;
    }

    return true;
}

function applyRoleSidebar() {
    const user = getUser();
    if (!user) return;
    const role = (user.role || 'CUSTOMER').toUpperCase();
    const effectiveRole = (role === 'SUPPLIERS') ? 'SUPPLIER' : role;
    const allowed = ROLE_PAGE_PERMISSIONS[effectiveRole] || [];

    const sidebars = document.querySelectorAll('.sidebar, #side, aside.sidebar');
    sidebars.forEach(sidebar => {
        const links = sidebar.querySelectorAll('a');
        links.forEach(link => {
            const href = link.getAttribute('href') || '';
            const cleanHref = href.split('?')[0].split('#')[0].replace(/^.*[\\\/]/, '').toLowerCase();

            // Always preserve logout link
            const isLogout = cleanHref === '' || cleanHref === '#' ||
                             link.getAttribute('onclick')?.includes('logout') ||
                             link.textContent.toLowerCase().includes('logout');
            if (isLogout) {
                link.style.display = '';
                return;
            }

            // Always allow Help / About
            if (cleanHref === 'about.html') {
                link.style.display = '';
                return;
            }

            if (allowed.includes('*') || allowed.includes(cleanHref)) {
                link.style.display = '';
            } else {
                link.style.display = 'none';
            }
        });
    });
}

function logout() {
    const keysToRemove = [
        'smartagriUser', 'smartagri_user', 'user',
        'smartagriAuthToken', 'smartagri_auth_token', 'token',
        'role', 'username'
    ];
    keysToRemove.forEach(k => localStorage.removeItem(k));
    window.location.href = 'login.html';
}

function applyUser() {
    const user = getUser();
    if (!user) return;

    // Populate user display names in topbar & profile
    document.querySelectorAll('[data-user-name]').forEach(el => {
        el.textContent = user.name || user.username || 'User';
    });

    // Populate user roles in badges
    document.querySelectorAll('[data-user-role]').forEach(el => {
        el.textContent = user.role || 'USER';
    });

    // Apply role-based dynamic sidebar visibility
    applyRoleSidebar();

    // Update cart and wishlist badges if user is authenticated
    updateCartBadge();
    updateWishlistBadge();
}

async function updateCartBadge() {
    const user = getUser();
    const token = getToken();
    const badges = document.querySelectorAll('.cart-count-badge, #cartCountBadge, [data-cart-badge]');
    if (badges.length === 0) return;

    if (!user || !token) {
        badges.forEach(b => {
            b.textContent = '0';
            b.style.display = 'none';
        });
        return;
    }

    try {
        if (typeof ApiClient !== 'undefined') {
            const res = await ApiClient.get('/cart');
            if (res && res.success && res.data) {
                const count = res.data.totalItemsCount || 0;
                badges.forEach(b => {
                    b.textContent = count;
                    b.style.display = count > 0 ? 'inline-flex' : 'none';
                });
            }
        }
    } catch (e) {
        // Silently tolerate network/initialization issues for cart badge
    }
}

async function updateWishlistBadge() {
    const user = getUser();
    const token = getToken();
    const badges = document.querySelectorAll('.wishlist-count-badge, #wishlistCountBadge, [data-wishlist-badge]');
    if (badges.length === 0) return;

    if (!user || !token) {
        badges.forEach(b => {
            b.textContent = '0';
            b.style.display = 'none';
        });
        return;
    }

    try {
        if (typeof ApiClient !== 'undefined') {
            const res = await ApiClient.get('/wishlist');
            if (res && res.success && res.data) {
                const count = res.data.totalItemsCount || 0;
                badges.forEach(b => {
                    b.textContent = count;
                    b.style.display = count > 0 ? 'inline-flex' : 'none';
                });
            }
        }
    } catch (e) {
        // Silently tolerate network/initialization issues for wishlist badge
    }
}

// Auto-run on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
    requireLogin();
    applyUser();
});
