/**
 * SmartAgri - Customer Wishlist Management
 * Phase 7: Shopping Cart, Wishlist & Customer Shopping Experience
 */

let activeWishlist = null;

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Verify user authentication
    if (typeof requireLogin === 'function') {
        requireLogin();
    }

    // 2. Fetch and render wishlist
    await loadWishlist();
});

async function loadWishlist() {
    const grid = document.getElementById('wishlistGrid');
    try {
        const res = await ApiClient.get('/wishlist');
        if (res && res.success && res.data) {
            activeWishlist = res.data;
            renderWishlist(activeWishlist);
            if (typeof updateWishlistBadge === 'function') {
                updateWishlistBadge();
            }
            if (typeof updateCartBadge === 'function') {
                updateCartBadge();
            }
        } else {
            renderEmptyWishlist();
        }
    } catch (e) {
        console.error('Failed to load wishlist:', e);
        if (grid) {
            grid.innerHTML = `
                <div class="col-12 text-center text-danger py-5">
                    <i class="bi bi-exclamation-triangle fs-1"></i>
                    <h5 class="mt-3">Failed to load wishlist</h5>
                    <p class="small text-muted">${e.message || 'Network or server error'}</p>
                    <button class="btn btn-outline-success btn-sm mt-2" onclick="loadWishlist()">Try Again</button>
                </div>
            `;
        }
    }
}

function renderWishlist(wishlist) {
    const grid = document.getElementById('wishlistGrid');
    const badge = document.getElementById('wishlistItemsBadge');
    const moveAllBtn = document.getElementById('moveAllBtn');

    if (!wishlist || !wishlist.items || wishlist.items.length === 0) {
        renderEmptyWishlist();
        return;
    }

    const items = wishlist.items;
    if (badge) badge.textContent = `${items.length} item(s)`;
    if (moveAllBtn) moveAllBtn.style.display = 'inline-block';

    grid.innerHTML = items.map(item => {
        const price = Number(item.sellingPrice) || 0;
        const discount = Number(item.discountPercent) || 0;
        let originalPrice = price;
        let discountHtml = '';

        if (discount > 0) {
            originalPrice = price / (1 - (discount / 100));
            discountHtml = `
                <span class="text-decoration-line-through text-muted small me-2">₹${originalPrice.toFixed(2)}</span>
                <span class="badge bg-danger-subtle text-danger small">${discount}% OFF</span>
            `;
        }

        const stock = Number(item.availableStock) || 0;
        const isOutOfStock = stock <= 0 || !item.available;
        let stockBadge = '';

        if (!item.available) {
            stockBadge = '<span class="stock-badge stock-out">Unavailable</span>';
        } else if (stock <= 0) {
            stockBadge = '<span class="stock-badge stock-out">Out of Stock</span>';
        } else if (stock <= 10) {
            stockBadge = `<span class="stock-badge stock-low">Only ${stock} left</span>`;
        } else {
            stockBadge = `<span class="stock-badge stock-in">In Stock (${stock})</span>`;
        }

        const imgSrc = item.imageUrl || '../img/img1.jpeg';
        const npkBadge = item.npkRatio 
            ? `<span class="npk-badge me-1" title="Nutrient Ratio N-P-K">NPK ${item.npkRatio}</span>` 
            : '';

        return `
            <div class="col-12 col-sm-6 col-lg-4 col-xl-3" id="wishlist-item-card-${item.id}">
                <div class="wishlist-card shadow-sm">
                    <div class="wishlist-card-img">
                        <img src="${imgSrc}" alt="${item.fertilizerName}" onerror="this.src='../img/img1.jpeg'">
                        <div class="position-absolute top-0 start-0 m-2">
                            ${stockBadge}
                        </div>
                        <button type="button" class="btn btn-sm btn-light position-absolute top-0 end-0 m-2 rounded-circle shadow-sm"
                            title="Remove from wishlist" onclick="removeFromWishlist(${item.id})">
                            <i class="bi bi-x-lg text-danger"></i>
                        </button>
                    </div>

                    <div class="wishlist-card-body">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <span class="badge bg-success-subtle text-success small">${item.categoryName || 'Fertilizer'}</span>
                            <span class="small text-muted fw-semibold">${item.brand || 'SmartAgri'}</span>
                        </div>
                        <h6 class="product-card-title text-truncate" title="${item.fertilizerName}">${item.fertilizerName}</h6>

                        <div class="text-muted small mb-2 d-flex align-items-center flex-wrap gap-1">
                            ${npkBadge}
                            <span><i class="bi bi-box me-1"></i>${item.unit || 'Bag'}</span>
                        </div>

                        <div class="mt-auto pt-2 border-top">
                            <div class="d-flex align-items-baseline mb-3">
                                <span class="fs-5 fw-bold text-success me-2">₹${price.toFixed(2)}</span>
                                ${discountHtml}
                            </div>

                            <div class="d-grid gap-2">
                                <button type="button" class="btn btn-success btn-sm fw-semibold" 
                                    id="move-btn-${item.id}"
                                    onclick="moveToCart(${item.id})" ${isOutOfStock ? 'disabled' : ''}>
                                    <i class="bi bi-cart-plus me-1"></i> ${isOutOfStock ? 'Unavailable' : 'Move to Cart'}
                                </button>
                                <button type="button" class="btn btn-outline-danger btn-sm" onclick="removeFromWishlist(${item.id})">
                                    <i class="bi bi-trash3 me-1"></i> Remove
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function renderEmptyWishlist() {
    const grid = document.getElementById('wishlistGrid');
    const badge = document.getElementById('wishlistItemsBadge');
    const moveAllBtn = document.getElementById('moveAllBtn');

    if (badge) badge.textContent = '0 items';
    if (moveAllBtn) moveAllBtn.style.display = 'none';

    if (grid) {
        grid.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="bi bi-heart text-muted" style="font-size: 4rem;"></i>
                <h4 class="fw-bold mt-3 text-secondary">Your wishlist is currently empty</h4>
                <p class="text-muted small mb-4">Explore our wide selection of certified organic, chemical, and bio-fertilizers and save your choices for later.</p>
                <a href="customer-catalog.html" class="btn btn-success fw-bold px-4 py-2 rounded-pill shadow-sm">
                    <i class="bi bi-shop me-1"></i> Explore Fertilizer Catalog
                </a>
            </div>
        `;
    }
}

async function moveToCart(itemId) {
    const btn = document.getElementById(`move-btn-${itemId}`);
    const origHtml = btn ? btn.innerHTML : '';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span> Moving...';
    }

    try {
        const res = await ApiClient.post(`/wishlist/items/${itemId}/move-to-cart`);
        if (res && res.success) {
            showToast('Moved to shopping cart!', 'success');
            await loadWishlist();
            if (typeof updateCartBadge === 'function') updateCartBadge();
            if (typeof updateWishlistBadge === 'function') updateWishlistBadge();
        } else {
            showToast(res.message || 'Unable to move item to cart', 'danger');
            if (btn) {
                btn.disabled = false;
                btn.innerHTML = origHtml;
            }
        }
    } catch (e) {
        showToast(e.message || 'Error moving item to cart', 'danger');
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = origHtml;
        }
    }
}

async function removeFromWishlist(itemId) {
    if (!confirm('Remove this product from your wishlist?')) return;

    try {
        const res = await ApiClient.delete(`/wishlist/items/${itemId}`);
        if (res && res.success) {
            showToast('Item removed from wishlist', 'success');
            await loadWishlist();
            if (typeof updateWishlistBadge === 'function') updateWishlistBadge();
        } else {
            showToast(res.message || 'Unable to remove item', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Error removing item', 'danger');
    }
}

async function moveAllToCart() {
    if (!activeWishlist || !activeWishlist.items || activeWishlist.items.length === 0) return;

    const availableItems = activeWishlist.items.filter(i => i.available && i.availableStock > 0);
    if (availableItems.length === 0) {
        showToast('None of the saved items are currently in stock.', 'warning');
        return;
    }

    let movedCount = 0;
    for (const item of availableItems) {
        try {
            const res = await ApiClient.post(`/wishlist/items/${item.id}/move-to-cart`);
            if (res && res.success) {
                movedCount++;
            }
        } catch (e) {
            console.warn(`Could not move item ${item.id}:`, e);
        }
    }

    showToast(`Successfully moved ${movedCount} item(s) to your cart!`, 'success');
    await loadWishlist();
    if (typeof updateCartBadge === 'function') updateCartBadge();
    if (typeof updateWishlistBadge === 'function') updateWishlistBadge();
}

function showToast(message, type = 'success') {
    const toastEl = document.getElementById('liveToast');
    const toastMsg = document.getElementById('toastMessage');
    if (toastEl && toastMsg) {
        toastEl.className = `toast align-items-center text-white bg-${type === 'danger' ? 'danger' : (type === 'warning' ? 'warning text-dark' : 'success')} border-0 shadow`;
        toastMsg.textContent = message;
        const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
        toast.show();
    }
}
