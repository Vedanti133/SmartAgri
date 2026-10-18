/**
 * SmartAgri - Customer Shopping Cart Management
 */

let activeCart = null;

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Verify authentication
    if (typeof requireLogin === 'function') {
        requireLogin();
    }

    // 2. Fetch and render cart
    await loadCart();
});

async function loadCart() {
    const wrapper = document.getElementById('cartContentWrapper');
    try {
        const res = await ApiClient.get('/cart');
        if (res && res.success && res.data) {
            activeCart = res.data;
            renderCart(activeCart);
            updateSummary(activeCart);
            if (typeof updateCartBadge === 'function') {
                updateCartBadge();
            }
        } else {
            renderEmptyCart();
        }
    } catch (e) {
        wrapper.innerHTML = `
            <div class="p-5 text-center text-danger">
                <i class="bi bi-exclamation-triangle fs-1"></i>
                <h5 class="mt-3">Failed to load cart</h5>
                <p class="small text-muted">${e.message}</p>
                <button class="btn btn-outline-success btn-sm mt-2" onclick="loadCart()">Try Again</button>
            </div>
        `;
    }
}

function renderCart(cart) {
    const wrapper = document.getElementById('cartContentWrapper');
    if (!cart || !cart.items || cart.items.length === 0) {
        renderEmptyCart();
        return;
    }

    let hasUnavailable = false;
    const itemsHtml = cart.items.map(item => {
        const unitPrice = Number(item.unitPrice) || 0;
        const effectivePrice = Number(item.effectivePrice) || unitPrice;
        const discount = Number(item.discountPercent) || 0;
        const lineTotal = Number(item.lineTotal) || 0;
        const stock = Number(item.availableStock) || 0;
        const qty = Number(item.quantity) || 1;
        const imgSrc = item.imageUrl || '../img/img1.jpeg';
        const isAvail = item.available !== false && stock > 0 && qty <= stock;
        if (!isAvail) hasUnavailable = true;

        const statusAlert = item.statusMessage ? `
            <div class="cart-stock-warning">
                <i class="bi bi-exclamation-triangle-fill"></i>
                <span>${item.statusMessage}</span>
            </div>
        ` : '';

        const npkHtml = item.npkRatio ? ` &bull; NPK: <strong>${item.npkRatio}</strong>` : '';

        return `
            <div class="cart-item-row d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-3 ${!isAvail ? 'bg-light' : ''}">
                <div class="d-flex align-items-center gap-3">
                    <img src="${imgSrc}" alt="${item.name}" class="cart-img-thumb" onerror="this.src='../img/img1.jpeg'">
                    <div>
                        <span class="badge bg-success-subtle text-success small mb-1">${item.categoryName || 'General'}</span>
                        <h6 class="fw-bold mb-1">${item.name}</h6>
                        <div class="small text-muted">
                            Brand: <strong>${item.brand || 'SmartAgri'}</strong> &bull; SKU: ${item.sku}${npkHtml}
                        </div>
                        <div class="small text-muted mt-1">
                            Unit Price: <strong>₹${effectivePrice.toFixed(2)}</strong>
                            ${discount > 0 ? `<span class="text-decoration-line-through text-secondary ms-1">₹${unitPrice.toFixed(2)}</span> <span class="badge bg-danger-subtle text-danger small">${discount}% off</span>` : ''}
                        </div>
                        ${statusAlert}
                    </div>
                </div>

                <div class="d-flex align-items-center justify-content-between justify-content-md-end gap-3 gap-md-4">
                    <div class="quantity-control">
                        <button type="button" class="qty-btn" onclick="changeQuantity(${item.id}, ${qty - 1})" ${qty <= 1 ? 'disabled' : ''}>-</button>
                        <input type="number" class="qty-input" value="${qty}" min="1" max="${stock}" readonly>
                        <button type="button" class="qty-btn" onclick="changeQuantity(${item.id}, ${qty + 1})" ${qty >= stock ? 'disabled' : ''}>+</button>
                    </div>

                    <div class="text-end" style="min-width: 90px;">
                        <div class="fw-bold text-success fs-5">₹${lineTotal.toFixed(2)}</div>
                        <div class="small text-muted">Subtotal</div>
                    </div>

                    <div class="d-flex gap-1">
                        <button class="btn btn-outline-secondary btn-sm p-2" title="Save for later in Wishlist" onclick="saveItemToWishlist(${item.fertilizerId}, ${item.id})">
                            <i class="bi bi-heart"></i>
                        </button>
                        <button class="btn btn-outline-danger btn-sm p-2" title="Remove item" onclick="removeItem(${item.id})">
                            <i class="bi bi-trash3"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');

    wrapper.innerHTML = `
        <div class="p-3 bg-light border-bottom d-flex justify-content-between align-items-center">
            <span class="fw-bold text-secondary">Product & Specifications</span>
            <span class="fw-bold text-secondary d-none d-md-inline">Quantity & Total</span>
        </div>
        ${itemsHtml}
    `;

    const checkoutBtn = document.getElementById('checkoutBtn');
    if (hasUnavailable) {
        checkoutBtn.disabled = true;
        checkoutBtn.title = 'Please adjust or remove out-of-stock / unavailable items';
    } else {
        checkoutBtn.disabled = false;
        checkoutBtn.title = '';
    }
    document.getElementById('clearCartBtn').disabled = false;
}

function renderEmptyCart() {
    const wrapper = document.getElementById('cartContentWrapper');
    wrapper.innerHTML = `
        <div class="p-5 text-center">
            <i class="bi bi-cart-x text-muted" style="font-size: 4rem;"></i>
            <h4 class="fw-bold mt-3 text-secondary">Your shopping cart is empty</h4>
            <p class="text-muted small mb-4">You haven't added any fertilizers or agricultural products to your cart yet.</p>
            <a href="customer-catalog.html" class="btn btn-success fw-bold px-4 py-2 rounded-pill shadow-sm">
                <i class="bi bi-shop me-1"></i> Browse Products
            </a>
        </div>
    `;

    document.getElementById('summaryTotalItems').textContent = '0 items';
    document.getElementById('summarySubtotal').textContent = '₹0.00';
    document.getElementById('summaryDiscounts').textContent = '- ₹0.00';
    document.getElementById('summaryNetAmount').textContent = '₹0.00';
    document.getElementById('summaryTax').textContent = '₹0.00';
    document.getElementById('summaryGrandTotal').textContent = '₹0.00';
    document.getElementById('checkoutBtn').disabled = true;
    document.getElementById('clearCartBtn').disabled = true;
}

function updateSummary(cart) {
    if (!cart) return;

    document.getElementById('summaryTotalItems').textContent = `${cart.totalItemsCount || 0} item(s)`;
    document.getElementById('summarySubtotal').textContent = `₹${(Number(cart.subtotal) || 0).toFixed(2)}`;
    document.getElementById('summaryDiscounts').textContent = `- ₹${(Number(cart.discountAmount) || 0).toFixed(2)}`;
    document.getElementById('summaryNetAmount').textContent = `₹${(Number(cart.netAmount) || 0).toFixed(2)}`;
    document.getElementById('summaryTaxPercent').textContent = `${cart.taxPercent || 5.0}%`;
    document.getElementById('summaryTax').textContent = `₹${(Number(cart.taxAmount) || 0).toFixed(2)}`;
    document.getElementById('summaryGrandTotal').textContent = `₹${(Number(cart.grandTotal) || 0).toFixed(2)}`;
}

async function changeQuantity(itemId, newQty) {
    if (newQty < 1) return;

    try {
        const res = await ApiClient.put(`/cart/items/${itemId}`, { quantity: newQty });
        if (res && res.success && res.data) {
            activeCart = res.data;
            renderCart(activeCart);
            updateSummary(activeCart);
            if (typeof updateCartBadge === 'function') {
                updateCartBadge();
            }
            showToast('Cart quantity updated', 'success');
        } else {
            showToast(res.message || 'Unable to update item quantity', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Error updating item quantity', 'danger');
    }
}

async function removeItem(itemId) {
    if (!confirm('Are you sure you want to remove this fertilizer from your cart?')) return;

    try {
        const res = await ApiClient.delete(`/cart/items/${itemId}`);
        if (res && res.success && res.data) {
            activeCart = res.data;
            renderCart(activeCart);
            updateSummary(activeCart);
            if (typeof updateCartBadge === 'function') {
                updateCartBadge();
            }
            showToast('Item removed from cart', 'success');
        } else {
            showToast(res.message || 'Unable to remove item', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Error removing item', 'danger');
    }
}

async function onClearCart() {
    if (!confirm('Clear all items from your cart?')) return;

    try {
        const res = await ApiClient.delete('/cart');
        if (res && res.success) {
            activeCart = res.data;
            renderCart(activeCart);
            updateSummary(activeCart);
            if (typeof updateCartBadge === 'function') {
                updateCartBadge();
            }
            showToast('Cart cleared', 'success');
        }
    } catch (e) {
        showToast(e.message || 'Error clearing cart', 'danger');
    }
}

async function saveItemToWishlist(fertilizerId, cartItemId) {
    try {
        const wRes = await ApiClient.post('/wishlist/items', { fertilizerId });
        if (wRes && wRes.success) {
            const cRes = await ApiClient.delete(`/cart/items/${cartItemId}`);
            if (cRes && cRes.success && cRes.data) {
                activeCart = cRes.data;
                renderCart(activeCart);
                updateSummary(activeCart);
            } else {
                await loadCart();
            }
            if (typeof updateCartBadge === 'function') updateCartBadge();
            if (typeof updateWishlistBadge === 'function') updateWishlistBadge();
            showToast('Item saved to your Wishlist!', 'success');
        } else {
            showToast(wRes.message || 'Unable to move to Wishlist', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Failed to move to Wishlist', 'danger');
    }
}

function onProceedToCheckout() {
    window.location.href = 'checkout.html';
}

function showToast(message, type = 'success') {
    const toastEl = document.getElementById('liveToast');
    const toastMsg = document.getElementById('toastMessage');
    if (toastEl && toastMsg) {
        toastEl.className = `toast align-items-center text-white bg-${type === 'danger' ? 'danger' : 'success'} border-0 shadow`;
        toastMsg.textContent = message;
        const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
        toast.show();
    }
}
