/**
 * SmartAgri - Phase 8 Customer Checkout, Address Management & Order Review
 */

let activeCart = null;
let savedAddresses = [];
let selectedAddressId = null;
let selectedPaymentMethod = 'COD';

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Verify customer authentication
    if (typeof requireLogin === 'function') {
        requireLogin();
    }

    // 2. Setup address modal submit handler
    setupAddressModal();

    // 3. Load cart items and saved addresses
    await loadCheckoutCart();
    await loadAddresses();

    // 4. Update cart badge in navbar
    if (typeof updateCartBadge === 'function') {
        await updateCartBadge();
    }
});

/**
 * Loads customer cart from backend and validates that it contains items
 */
async function loadCheckoutCart() {
    const container = document.getElementById('checkoutFullCartItemsList');
    try {
        const res = await ApiClient.get('/cart');
        if (!res || !res.success || !res.data || !res.data.items || res.data.items.length === 0) {
            alert('Your shopping cart is empty! Please add fertilizers before proceeding to checkout.');
            window.location.href = 'customer-catalog.html';
            return;
        }

        activeCart = res.data;
        renderCartReview(activeCart);
    } catch (e) {
        if (container) {
            container.innerHTML = `<div class="alert alert-danger py-2 small">Failed to load cart items: ${e.message}</div>`;
        }
    }
}

/**
 * Renders the cart review items and summary breakdown
 */
function renderCartReview(cart) {
    const container = document.getElementById('checkoutFullCartItemsList');
    if (!container) return;

    let totalItemsCount = 0;
    container.innerHTML = cart.items.map(item => {
        const imgSrc = item.imageUrl || '../img/img1.jpeg';
        const price = Number(item.effectivePrice) || Number(item.unitPrice) || 0;
        const lineTotal = Number(item.lineTotal) || 0;
        const qty = Number(item.quantity) || 1;
        totalItemsCount += qty;

        return `
            <div class="d-flex align-items-center justify-content-between p-3 border rounded-3 bg-light">
                <div class="d-flex align-items-center gap-3">
                    <img src="${imgSrc}" alt="${item.name}" class="cart-mini-thumb" onerror="this.src='../img/img1.jpeg'">
                    <div>
                        <div class="fw-bold text-dark mb-0">${item.name}</div>
                        <div class="text-muted small">
                            ${item.brand ? '<span class="badge bg-light text-secondary border me-1">' + item.brand + '</span>' : ''}
                            ${item.categoryName ? item.categoryName + ' | ' : ''}Qty: <strong>${qty}</strong>
                        </div>
                    </div>
                </div>
                <div class="text-end">
                    <div class="fw-bold text-success">₹${lineTotal.toFixed(2)}</div>
                    <div class="text-muted small">₹${price.toFixed(2)} each</div>
                </div>
            </div>
        `;
    }).join('');

    const subtotal = Number(cart.subtotal) || 0;
    const discount = Number(cart.discountAmount) || 0;
    const net = Number(cart.netAmount) || 0;
    const tax = Number(cart.taxAmount) || 0;
    // Shipping policy: Net >= 1000 ? 0.00 : 50.00
    const shipping = net >= 1000 ? 0.00 : 50.00;
    const grandTotal = net + tax + shipping;

    const subtotalEl = document.getElementById('checkoutSubtotal');
    const discountEl = document.getElementById('checkoutDiscounts');
    const netEl = document.getElementById('checkoutNet');
    const taxEl = document.getElementById('checkoutTax');
    const shippingEl = document.getElementById('checkoutShipping');
    const grandTotalEl = document.getElementById('checkoutGrandTotal');
    const countEl = document.getElementById('checkoutItemCount');

    if (subtotalEl) subtotalEl.textContent = `₹${subtotal.toFixed(2)}`;
    if (discountEl) discountEl.textContent = `- ₹${discount.toFixed(2)}`;
    if (netEl) netEl.textContent = `₹${net.toFixed(2)}`;
    if (taxEl) taxEl.textContent = `₹${tax.toFixed(2)}`;
    if (shippingEl) {
        shippingEl.innerHTML = shipping === 0 
            ? '<span class="text-success fw-bold">FREE Delivery</span>' 
            : `₹${shipping.toFixed(2)}`;
    }
    if (grandTotalEl) grandTotalEl.textContent = `₹${grandTotal.toFixed(2)}`;
    if (countEl) countEl.textContent = `${totalItemsCount} ${totalItemsCount === 1 ? 'item' : 'items'}`;
}

/**
 * Loads delivery addresses for the logged-in customer
 */
async function loadAddresses() {
    const container = document.getElementById('addressSelectionContainer');
    try {
        const res = await ApiClient.get('/addresses');
        if (res && res.success && Array.isArray(res.data)) {
            savedAddresses = res.data;
            renderAddresses(savedAddresses);
        } else {
            savedAddresses = [];
            renderAddresses([]);
        }
    } catch (e) {
        if (container) {
            container.innerHTML = `<div class="alert alert-danger py-2 small">Error loading addresses: ${e.message}</div>`;
        }
    }
}

/**
 * Renders address selection cards
 */
function renderAddresses(addresses) {
    const container = document.getElementById('addressSelectionContainer');
    if (!container) return;

    if (!addresses || addresses.length === 0) {
        container.innerHTML = `
            <div class="p-4 border rounded-3 bg-light text-center">
                <i class="bi bi-geo text-muted fs-2"></i>
                <h6 class="fw-bold mt-2">No Delivery Addresses Found</h6>
                <p class="small text-muted mb-3">Please add your farm or home address to proceed with order dispatch.</p>
                <button class="btn btn-sm btn-success fw-bold" onclick="openAddressModal()">
                    <i class="bi bi-plus-lg me-1"></i> Add Delivery Address
                </button>
            </div>
        `;
        selectedAddressId = null;
        updateAddressPreview();
        return;
    }

    // Default to the address marked default, or first address
    if (!selectedAddressId || !addresses.some(a => a.id === selectedAddressId)) {
        const defaultAddr = addresses.find(a => a.default) || addresses[0];
        selectedAddressId = defaultAddr.id;
    }

    container.innerHTML = addresses.map(addr => {
        const isSelected = String(addr.id) === String(selectedAddressId);
        const districtText = addr.district ? `, ${addr.district}` : '';
        const landmarkText = addr.landmark ? ` (Near ${addr.landmark})` : '';

        return `
            <div class="address-select-card ${isSelected ? 'selected' : ''}" onclick="selectAddress(${addr.id})">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <div class="d-flex align-items-center gap-2">
                        <input type="radio" name="selectedAddressRadio" value="${addr.id}" ${isSelected ? 'checked' : ''} 
                               onclick="event.stopPropagation(); selectAddress(${addr.id});">
                        <span class="badge bg-secondary-subtle text-secondary small fw-semibold">${addr.addressType || 'HOME'}</span>
                        ${addr.default ? '<span class="badge bg-success-subtle text-success small fw-semibold"><i class="bi bi-star-fill me-1"></i>Default</span>' : ''}
                        <h6 class="fw-bold mb-0 text-dark">${addr.fullName}</h6>
                    </div>
                    <div class="small text-muted">
                        <i class="bi bi-telephone me-1"></i>+91 ${addr.phone}
                    </div>
                </div>

                <div class="small text-secondary ps-4 mb-3">
                    ${addr.addressLine1}${addr.addressLine2 ? ', ' + addr.addressLine2 : ''}, 
                    ${addr.city}${districtText}, ${addr.state} - <strong>${addr.pincode}</strong>
                    ${landmarkText}
                </div>

                <div class="d-flex justify-content-between align-items-center pt-2 border-top ps-4" onclick="event.stopPropagation();">
                    <div class="d-flex gap-2">
                        <button class="btn btn-sm ${isSelected ? 'btn-success' : 'btn-outline-success'}" onclick="selectAddress(${addr.id})">
                            <i class="bi bi-check-circle me-1"></i> ${isSelected ? 'Deliver Here' : 'Select'}
                        </button>
                        ${!addr.default ? `
                            <button class="btn btn-sm btn-outline-secondary" onclick="setDefaultAddress(${addr.id})">
                                Set Default
                            </button>
                        ` : ''}
                    </div>

                    <div class="d-flex gap-1">
                        <button class="btn btn-sm btn-outline-primary" onclick="openEditAddressModal(${addr.id})" title="Edit Address">
                            <i class="bi bi-pencil"></i> Edit
                        </button>
                        <button class="btn btn-sm btn-outline-danger" onclick="deleteAddress(${addr.id})" title="Delete Address">
                            <i class="bi bi-trash3"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');

    updateAddressPreview();
}

/**
 * Selects an address for delivery
 */
function selectAddress(id) {
    selectedAddressId = id;
    renderAddresses(savedAddresses);
}

/**
 * Updates the selected address preview in Step 4 Order Summary
 */
function updateAddressPreview() {
    const previewEl = document.getElementById('selectedAddressPreview');
    if (!previewEl) return;

    if (!selectedAddressId || !savedAddresses.length) {
        previewEl.innerHTML = `
            <div class="text-danger small">
                <i class="bi bi-exclamation-triangle me-1"></i> No delivery address selected yet.
            </div>
        `;
        return;
    }

    const addr = savedAddresses.find(a => a.id === selectedAddressId);
    if (!addr) {
        previewEl.innerHTML = `<div class="text-danger small">Selected address not found.</div>`;
        return;
    }

    const districtText = addr.district ? `, ${addr.district}` : '';
    const landmarkText = addr.landmark ? ` (Near ${addr.landmark})` : '';

    previewEl.innerHTML = `
        <div class="d-flex justify-content-between align-items-center mb-1">
            <span class="badge bg-success-subtle text-success small fw-bold">Delivering To</span>
            <span class="badge bg-secondary-subtle text-secondary small">${addr.addressType || 'HOME'}</span>
        </div>
        <div class="fw-bold text-dark">${addr.fullName}</div>
        <div class="small text-muted mb-1"><i class="bi bi-telephone me-1"></i>+91 ${addr.phone}</div>
        <div class="small text-secondary">
            ${addr.addressLine1}${addr.addressLine2 ? ', ' + addr.addressLine2 : ''}, 
            ${addr.city}${districtText}, ${addr.state} - <strong>${addr.pincode}</strong>
            ${landmarkText}
        </div>
    `;
}

/**
 * Sets an address as the default
 */
async function setDefaultAddress(id) {
    try {
        const res = await ApiClient.put(`/addresses/${id}/default`);
        if (res && res.success) {
            showToast('Default delivery address updated', 'success');
            await loadAddresses();
        } else {
            showToast(res.message || 'Failed to set default address', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Error updating default address', 'danger');
    }
}

/**
 * Deletes an address with confirmation
 */
async function deleteAddress(id) {
    if (!confirm('Are you sure you want to delete this delivery address?')) return;

    try {
        const res = await ApiClient.delete(`/addresses/${id}`);
        if (res && res.success) {
            showToast('Address deleted successfully', 'success');
            if (selectedAddressId === id) {
                selectedAddressId = null;
            }
            await loadAddresses();
        } else {
            showToast(res.message || 'Failed to delete address', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Error deleting address', 'danger');
    }
}

/**
 * Opens address modal for adding new address
 */
function openAddressModal() {
    const form = document.getElementById('addressForm');
    if (form) form.reset();

    document.getElementById('modalAddressId').value = '';
    document.getElementById('addressModalTitle').textContent = 'Add Delivery Address';
    document.getElementById('modalAddrMsg').innerHTML = '';

    const modalEl = document.getElementById('addressModal');
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
}

/**
 * Opens address modal pre-populated for editing
 */
function openEditAddressModal(id) {
    const addr = savedAddresses.find(a => a.id === id);
    if (!addr) return;

    document.getElementById('modalAddressId').value = addr.id;
    document.getElementById('addressModalTitle').textContent = 'Edit Delivery Address';
    document.getElementById('modalAddrMsg').innerHTML = '';

    document.getElementById('modalFullName').value = addr.fullName || '';
    document.getElementById('modalPhone').value = addr.phone || '';
    document.getElementById('modalLine1').value = addr.addressLine1 || '';
    document.getElementById('modalLine2').value = addr.addressLine2 || '';
    document.getElementById('modalCity').value = addr.city || '';
    document.getElementById('modalDistrict').value = addr.district || '';
    document.getElementById('modalState').value = addr.state || '';
    document.getElementById('modalPincode').value = addr.pincode || '';
    document.getElementById('modalLandmark').value = addr.landmark || '';
    document.getElementById('modalType').value = addr.addressType || 'FARM';
    document.getElementById('modalIsDefault').checked = !!addr.default;

    const modalEl = document.getElementById('addressModal');
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
}

/**
 * Sets up submit event listener on address form (handles both Add and Edit)
 */
function setupAddressModal() {
    const form = document.getElementById('addressForm');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const btn = document.getElementById('modalSaveAddrBtn');
        const msg = document.getElementById('modalAddrMsg');
        const editId = document.getElementById('modalAddressId').value.trim();

        const fullName = document.getElementById('modalFullName').value.trim();
        const phone = document.getElementById('modalPhone').value.trim();
        const addressLine1 = document.getElementById('modalLine1').value.trim();
        const addressLine2 = document.getElementById('modalLine2').value.trim();
        const city = document.getElementById('modalCity').value.trim();
        const district = document.getElementById('modalDistrict').value.trim();
        const state = document.getElementById('modalState').value.trim();
        const pincode = document.getElementById('modalPincode').value.trim();
        const landmark = document.getElementById('modalLandmark').value.trim();
        const addressType = document.getElementById('modalType').value;
        const isDefault = document.getElementById('modalIsDefault').checked;

        if (!/^[1-9][0-9]{5}$/.test(pincode)) {
            msg.innerHTML = '<div class="alert alert-danger py-2 small">Enter a valid 6-digit Indian PIN code.</div>';
            return;
        }

        if (!/^[0-9]{10}$/.test(phone)) {
            msg.innerHTML = '<div class="alert alert-danger py-2 small">Enter a valid 10-digit mobile number.</div>';
            return;
        }

        btn.disabled = true;
        btn.textContent = 'Saving Address...';
        msg.innerHTML = '';

        try {
            const payload = {
                fullName,
                phone,
                addressLine1,
                addressLine2: addressLine2 || null,
                city,
                district: district || null,
                state,
                pincode,
                landmark: landmark || null,
                addressType,
                default: isDefault
            };

            let res;
            if (editId) {
                res = await ApiClient.put(`/addresses/${editId}`, payload);
            } else {
                res = await ApiClient.post('/addresses', payload);
            }

            if (res && res.success && res.data) {
                selectedAddressId = res.data.id;
                form.reset();

                const modalEl = document.getElementById('addressModal');
                const modalInstance = bootstrap.Modal.getInstance(modalEl);
                if (modalInstance) modalInstance.hide();

                await loadAddresses();
                showToast(editId ? 'Address updated successfully!' : 'Address added & selected!', 'success');
            } else {
                msg.innerHTML = `<div class="alert alert-danger py-2 small">${res.message || 'Failed to save address'}</div>`;
            }
        } catch (err) {
            msg.innerHTML = `<div class="alert alert-danger py-2 small">${err.message}</div>`;
        } finally {
            btn.disabled = false;
            btn.textContent = 'Save Address';
        }
    });
}

/**
 * Handles switching payment methods
 */
function selectPaymentMethod(method) {
    selectedPaymentMethod = method;

    const codCard = document.getElementById('codOptionCard');
    const onlineCard = document.getElementById('onlineOptionCard');
    const codRadio = document.getElementById('payCOD');
    const onlineRadio = document.getElementById('payONLINE');
    const simPanel = document.getElementById('onlineSimPanel');

    if (method === 'COD') {
        if (codCard) codCard.classList.add('selected');
        if (onlineCard) onlineCard.classList.remove('selected');
        if (codRadio) codRadio.checked = true;
        if (simPanel) simPanel.style.display = 'none';
    } else {
        if (onlineCard) onlineCard.classList.add('selected');
        if (codCard) codCard.classList.remove('selected');
        if (onlineRadio) onlineRadio.checked = true;
        if (simPanel) simPanel.style.display = 'block';
    }
}

/**
 * Step 5: Places the order with authoritative server-side price & stock revalidation
 */
async function onPlaceOrder() {
    const btn = document.getElementById('placeOrderBtn');
    const msg = document.getElementById('checkoutMsg');

    if (!selectedAddressId) {
        msg.innerHTML = '<div class="alert alert-danger py-2 small">Please select a delivery address before placing your order.</div>';
        return;
    }

    if (!activeCart || !activeCart.items || activeCart.items.length === 0) {
        msg.innerHTML = '<div class="alert alert-danger py-2 small">Your shopping cart is empty. Please add items to checkout.</div>';
        return;
    }

    const notes = document.getElementById('orderNotesInput') ? document.getElementById('orderNotesInput').value.trim() : '';

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span> Processing Order...';
    msg.innerHTML = '';

    try {
        // Collect current expected prices for price change detection
        const expectedItemPrices = {};
        if (activeCart.items) {
            activeCart.items.forEach(it => {
                const fertId = it.fertilizerId || it.id;
                expectedItemPrices[fertId] = it.effectivePrice || it.unitPrice;
            });
        }

        const net = Number(activeCart.netAmount) || 0;
        const tax = Number(activeCart.taxAmount) || 0;
        const shipping = net >= 1000 ? 0.00 : 50.00;
        const grandTotal = net + tax + shipping;

        const payload = {
            addressId: selectedAddressId,
            paymentMethod: selectedPaymentMethod,
            notes: notes || null,
            expectedSubtotal: activeCart.subtotal,
            expectedGrandTotal: grandTotal,
            expectedItemPrices: expectedItemPrices
        };

        const res = await ApiClient.post('/orders', payload);
        if (res && res.success && res.data) {
            const orderData = res.data;
            sessionStorage.setItem('lastPlacedOrder', JSON.stringify(orderData));

            if (selectedPaymentMethod === 'ONLINE') {
                const simChoice = document.querySelector('input[name="onlineSimChoice"]:checked')?.value || 'SUCCESS';
                const isFail = simChoice === 'FAIL';

                try {
                    // Fetch created payment for this order and confirm with gateway
                    const payRes = await ApiClient.get(`/payments/order/${orderData.id}`);
                    if (payRes && payRes.success && payRes.data) {
                        const payment = payRes.data;
                        await ApiClient.post(`/payments/${payment.id}/confirm`, {
                            gatewayOrderId: payment.gatewayOrderId,
                            gatewayPaymentId: 'pay_mock_' + Date.now(),
                            gatewaySignature: isFail ? 'INVALID_TEST_SIGNATURE' : 'valid_test_signature',
                            amount: payment.amount
                        });
                    }
                } catch (payErr) {
                    console.warn('Online gateway simulation error/declined:', payErr.message);
                }

                window.location.href = `order-success.html?orderId=${orderData.id}&orderNumber=${orderData.orderNumber}${isFail ? '&paymentFailed=true' : ''}`;
            } else {
                window.location.href = `order-success.html?orderId=${orderData.id}&orderNumber=${orderData.orderNumber}`;
            }
        } else {
            const errorMsg = res.message || 'Unable to place order. Please review your cart.';
            msg.innerHTML = `<div class="alert alert-danger py-2 small">${errorMsg}</div>`;
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-bag-check-fill me-1"></i> Step 5: Place Order';

            // If price change detected, refresh cart
            if (errorMsg.toLowerCase().includes('price')) {
                await loadCheckoutCart();
            }
        }
    } catch (e) {
        const errorMsg = e.message || 'Order placement failed. Please verify stock availability.';
        msg.innerHTML = `<div class="alert alert-danger py-2 small">${errorMsg}</div>`;
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-bag-check-fill me-1"></i> Step 5: Place Order';

        if (errorMsg.toLowerCase().includes('price')) {
            await loadCheckoutCart();
        }
    }
}

/**
 * Toast notifications helper
 */
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
