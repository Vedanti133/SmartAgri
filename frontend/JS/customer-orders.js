/**
 * SmartAgri - Customer Orders & Cancellation Management
 */

let myOrders = [];

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Verify authentication
    if (typeof requireLogin === 'function') {
        requireLogin();
    }

    // 2. Load orders
    await loadOrders();
    if (typeof updateCartBadge === 'function') {
        await updateCartBadge();
    }
});

async function loadOrders() {
    const container = document.getElementById('customerOrdersContainer');
    try {
        const res = await ApiClient.get('/orders');
        if (res && res.success && Array.isArray(res.data)) {
            myOrders = res.data;
            renderOrders(myOrders);
        } else {
            renderEmptyOrders();
        }
    } catch (e) {
        container.innerHTML = `
            <div class="p-5 text-center text-danger border rounded-3 bg-white">
                <i class="bi bi-exclamation-triangle fs-1"></i>
                <h5 class="mt-3">Failed to load order history</h5>
                <p class="small text-muted">${e.message}</p>
                <button class="btn btn-sm btn-outline-success" onclick="loadOrders()">Try Again</button>
            </div>
        `;
    }
}

function renderOrders(orders) {
    const container = document.getElementById('customerOrdersContainer');
    if (!orders || orders.length === 0) {
        renderEmptyOrders();
        return;
    }

    container.innerHTML = orders.map(order => {
        const total = Number(order.grandTotal) || 0;
        const statusClass = `status-badge-${(order.orderStatus || 'pending').toLowerCase()}`;
        const deliveryStatusClass = `badge bg-${order.deliveryStatus === 'DELIVERED' ? 'success' : order.deliveryStatus === 'OUT_FOR_DELIVERY' ? 'warning text-dark' : order.deliveryStatus === 'SHIPPED' ? 'info text-dark' : 'secondary'}`;
        const dateStr = order.createdAt ? new Date(order.createdAt).toLocaleDateString('en-IN', {
            day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
        }) : 'Recent';

        return `
            <div class="order-card">
                <div class="order-card-header d-flex flex-column flex-md-row justify-content-between align-items-md-center gap-2">
                    <div class="d-flex flex-wrap align-items-center gap-2">
                        <span class="fw-bold fs-6 text-dark">${order.orderNumber}</span>
                        <span class="badge ${statusClass} px-3 py-1 rounded-pill fw-bold">${order.orderStatus}</span>
                        <span class="badge bg-light text-secondary border small">${order.paymentMethod} &bull; ${order.paymentStatus}</span>
                        ${order.deliveryStatus ? `<span class="${deliveryStatusClass} px-2 py-1 rounded-pill small"><i class="bi bi-truck me-1"></i>${order.deliveryStatus.replace(/_/g, ' ')}</span>` : ''}
                    </div>
                    <div class="text-muted small">
                        <i class="bi bi-clock me-1"></i>${dateStr}
                    </div>
                </div>

                <div class="order-card-body">
                    <div class="row align-items-center g-3">
                        <div class="col-md-7">
                            <div class="text-secondary small mb-1">
                                <i class="bi bi-geo-alt me-1 text-success"></i>Dispatched to: <strong>${order.recipientName}</strong>, ${order.city} (${order.pincode})
                            </div>
                            <div class="small text-muted mb-1">
                                Items: <strong>${order.totalItemsCount || (order.items ? order.items.length : 1)} product(s)</strong>
                            </div>
                            ${order.trackingNumber ? `
                                <div class="small text-primary">
                                    <i class="bi bi-upc-scan me-1"></i>Tracking: <strong>${order.trackingNumber}</strong>
                                    ${order.deliveryPartner ? `(${order.deliveryPartner})` : ''}
                                </div>
                            ` : ''}
                        </div>

                        <div class="col-md-5 text-md-end">
                            <div class="fs-5 fw-bold text-success mb-2">₹${total.toFixed(2)}</div>
                            <div class="d-flex flex-wrap justify-content-md-end gap-2">
                                <button class="btn btn-outline-success btn-sm" onclick="viewOrderDetails(${order.id})">
                                    <i class="bi bi-eye me-1"></i> Details
                                </button>
                                <a href="customer-order-tracking.html?orderId=${order.id}" class="btn btn-outline-primary btn-sm">
                                    <i class="bi bi-truck me-1"></i> Track
                                </a>
                                <a href="customer-invoice.html?orderId=${order.id}" class="btn btn-outline-secondary btn-sm" target="_blank">
                                    <i class="bi bi-receipt me-1"></i> Invoice
                                </a>
                                ${(order.orderStatus === 'DELIVERED' || order.deliveryStatus === 'DELIVERED') ? `
                                    <a href="returns.html?orderId=${order.id}" class="btn btn-outline-info btn-sm">
                                        <i class="bi bi-arrow-return-left me-1"></i> Return / Replace
                                    </a>
                                ` : ''}
                                ${(order.canRetryPayment || (order.paymentMethod === 'ONLINE' && (order.paymentStatus === 'FAILED' || order.paymentStatus === 'PENDING') && order.orderStatus !== 'CANCELLED')) ? `
                                    <button class="btn btn-warning btn-sm fw-bold" onclick="openRetryPaymentModal(${order.id}, '${order.orderNumber}', ${total})">
                                        <i class="bi bi-arrow-repeat me-1"></i> Retry Payment
                                    </button>
                                ` : ''}
                                ${order.canCancel ? `
                                    <button class="btn btn-outline-danger btn-sm" onclick="cancelOrder(${order.id}, '${order.orderNumber}')">
                                        <i class="bi bi-x-circle me-1"></i> Cancel
                                    </button>
                                ` : ''}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function renderEmptyOrders() {
    const container = document.getElementById('customerOrdersContainer');
    container.innerHTML = `
        <div class="p-5 text-center border rounded-3 bg-white shadow-sm">
            <i class="bi bi-receipt text-muted" style="font-size: 3.5rem;"></i>
            <h5 class="fw-bold mt-3 text-secondary">No Orders Placed Yet</h5>
            <p class="text-muted small mb-4">You have not placed any orders yet. Discover our premium crop nutrients and soil products.</p>
            <a href="customer-catalog.html" class="btn btn-success fw-bold px-4 py-2 rounded-pill shadow-sm">
                <i class="bi bi-shop me-1"></i> Explore Catalog
            </a>
        </div>
    `;
}

async function viewOrderDetails(orderId) {
    const modalNumber = document.getElementById('modalOrderNumber');
    const modalDate = document.getElementById('modalOrderDate');
    const modalBody = document.getElementById('modalOrderBody');
    const modalFooter = document.getElementById('modalOrderFooter');

    modalBody.innerHTML = '<div class="text-center py-4"><div class="spinner-border spinner-border-sm text-success"></div></div>';
    const modal = new bootstrap.Modal(document.getElementById('orderDetailsModal'));
    modal.show();

    try {
        const res = await ApiClient.get(`/orders/${orderId}`);
        if (!res || !res.success || !res.data) {
            modalBody.innerHTML = `<div class="alert alert-danger">${res.message || 'Order details unavailable.'}</div>`;
            return;
        }

        const order = res.data;
        modalNumber.textContent = `Order #${order.orderNumber}`;
        const dateStr = order.createdAt ? new Date(order.createdAt).toLocaleString('en-IN') : 'Recent';
        modalDate.textContent = `Placed on: ${dateStr}`;

        const statusClass = `status-badge-${(order.orderStatus || 'pending').toLowerCase()}`;
        const itemsHtml = (order.items || []).map(item => {
            const imgSrc = item.imageUrl || '../img/img1.jpeg';
            const unitPrice = Number(item.unitPrice) || 0;
            const lineTotal = Number(item.lineTotal) || 0;
            const discountAmt = Number(item.discountAmount) || 0;

            return `
                <tr>
                    <td style="width: 60px;">
                        <img src="${imgSrc}" alt="${item.productName}" style="width: 48px; height: 48px; object-fit: cover; border-radius: 6px;" onerror="this.src='../img/img1.jpeg'">
                    </td>
                    <td>
                        <div class="fw-bold small">${item.productName}</div>
                        <div class="text-muted small">Brand: ${item.brand || 'SmartAgri'} &bull; SKU: ${item.sku}</div>
                    </td>
                    <td class="text-center small">₹${unitPrice.toFixed(2)}</td>
                    <td class="text-center small">${item.quantity}</td>
                    <td class="text-center small text-success">${discountAmt > 0 ? '-₹' + discountAmt.toFixed(2) : '-'}</td>
                    <td class="text-end fw-bold small text-success">₹${lineTotal.toFixed(2)}</td>
                </tr>
            `;
        }).join('');

        modalBody.innerHTML = `
            <!-- Status & Destination & Delivery Cards -->
            <div class="row g-3 mb-4">
                <div class="col-md-4">
                    <div class="p-3 bg-light rounded-3 h-100">
                        <h6 class="fw-bold mb-2"><i class="bi bi-info-circle text-primary me-1"></i>Order Status</h6>
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <span class="badge ${statusClass} px-3 py-1 rounded-pill fw-bold">${order.orderStatus}</span>
                        </div>
                        <div class="small text-muted">Order ID: <strong>#${order.id}</strong></div>
                        <div class="small text-muted mt-1">Payment: <strong>${order.paymentMethod}</strong></div>
                        <div class="small text-${order.paymentStatus === 'PAID' || order.paymentStatus === 'SUCCESS' ? 'success' : order.paymentStatus === 'FAILED' ? 'danger' : 'warning'} fw-semibold">
                            Status: ${order.paymentStatus}
                        </div>
                        ${order.transactionId ? `<div class="small text-muted mt-1">Txn ID: <strong class="font-monospace text-dark">${order.transactionId}</strong></div>` : ''}
                        ${(order.canRetryPayment || (order.paymentMethod === 'ONLINE' && (order.paymentStatus === 'FAILED' || order.paymentStatus === 'PENDING') && order.orderStatus !== 'CANCELLED')) ? `
                            <div class="mt-2">
                                <button class="btn btn-sm btn-warning fw-bold py-1 px-2" onclick="bootstrap.Modal.getInstance(document.getElementById('orderDetailsModal')).hide(); openRetryPaymentModal(${order.id}, '${order.orderNumber}', ${order.grandTotal});">
                                    <i class="bi bi-arrow-repeat me-1"></i> Retry Payment
                                </button>
                            </div>
                        ` : ''}
                        ${order.notes ? `<div class="small text-secondary mt-1"><i class="bi bi-chat-left-text me-1"></i>${order.notes}</div>` : ''}
                    </div>
                </div>

                <div class="col-md-4">
                    <div class="p-3 bg-light rounded-3 h-100">
                        <h6 class="fw-bold mb-2"><i class="bi bi-truck text-success me-1"></i>Delivery Details</h6>
                        <div class="small mb-1">Status: <span class="badge bg-secondary">${order.deliveryStatus || 'PENDING'}</span></div>
                        ${order.trackingNumber ? `
                            <div class="small text-muted">Tracking #: <strong class="text-dark">${order.trackingNumber}</strong></div>
                            <div class="small text-muted">Partner: <strong>${order.deliveryPartner || 'Standard Logistics'}</strong></div>
                            ${order.estimatedDeliveryDate ? `<div class="small text-muted">Est. Date: <strong>${order.estimatedDeliveryDate}</strong></div>` : ''}
                            <div class="mt-2">
                                <a href="customer-order-tracking.html?orderId=${order.id}" class="btn btn-sm btn-outline-primary py-0 px-2" style="font-size: 0.75rem;">
                                    <i class="bi bi-geo-alt me-1"></i>Live Tracking
                                </a>
                            </div>
                        ` : `<div class="small text-muted">Consignment preparation underway.</div>`}
                    </div>
                </div>

                <div class="col-md-4">
                    <div class="p-3 bg-light rounded-3 h-100">
                        <h6 class="fw-bold mb-2"><i class="bi bi-geo-alt text-danger me-1"></i>Shipping Address</h6>
                        <div class="small fw-semibold">${order.recipientName}</div>
                        <div class="small text-muted">+91 ${order.recipientPhone}</div>
                        <div class="small text-muted">${order.addressLine1}${order.addressLine2 ? ', ' + order.addressLine2 : ''}</div>
                        <div class="small text-muted">${order.city}, ${order.state} - <strong>${order.pincode}</strong></div>
                    </div>
                </div>
            </div>

            <!-- Items Table -->
            <h6 class="fw-bold mb-2">Ordered Fertilizer Items</h6>
            <div class="table-responsive mb-4 border rounded-3">
                <table class="table table-sm align-middle mb-0">
                    <thead class="table-light small">
                        <tr>
                            <th>Item</th>
                            <th>Description</th>
                            <th class="text-center">Unit Price</th>
                            <th class="text-center">Qty</th>
                            <th class="text-center">Discount</th>
                            <th class="text-end">Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${itemsHtml}
                    </tbody>
                </table>
            </div>

            <!-- Financial Summary -->
            <div class="row justify-content-end">
                <div class="col-md-6">
                    <div class="p-3 border rounded-3 bg-light small">
                        <div class="d-flex justify-content-between mb-1">
                            <span class="text-muted">Subtotal (Gross):</span>
                            <span class="fw-semibold">₹${(Number(order.subtotal) || 0).toFixed(2)}</span>
                        </div>
                        <div class="d-flex justify-content-between mb-1 text-success">
                            <span>Discounts:</span>
                            <span class="fw-semibold">- ₹${(Number(order.discountAmount) || 0).toFixed(2)}</span>
                        </div>
                        <div class="d-flex justify-content-between mb-1">
                            <span class="text-muted">GST (5%):</span>
                            <span class="fw-semibold">₹${(Number(order.taxAmount) || 0).toFixed(2)}</span>
                        </div>
                        <div class="d-flex justify-content-between mb-1">
                            <span class="text-muted">Shipping Fee:</span>
                            <span class="fw-semibold">${Number(order.shippingAmount) === 0 ? '<span class="text-success fw-bold">FREE</span>' : '₹' + Number(order.shippingAmount).toFixed(2)}</span>
                        </div>
                        <div class="d-flex justify-content-between border-top pt-2 mt-2 fw-bold text-dark fs-6">
                            <span>Grand Total:</span>
                            <span class="text-success">₹${(Number(order.grandTotal) || 0).toFixed(2)}</span>
                        </div>
                    </div>
                </div>
            </div>
        `;

        modalFooter.innerHTML = `
            <a href="customer-order-tracking.html?orderId=${order.id}" class="btn btn-outline-primary btn-sm">
                <i class="bi bi-truck me-1"></i> Track Order
            </a>
            <a href="customer-invoice.html?orderId=${order.id}" class="btn btn-outline-secondary btn-sm" target="_blank">
                <i class="bi bi-receipt me-1"></i> GST Invoice
            </a>
            ${(order.orderStatus === 'DELIVERED' || order.deliveryStatus === 'DELIVERED') ? `
                <a href="returns.html?orderId=${order.id}" class="btn btn-outline-info btn-sm">
                    <i class="bi bi-arrow-return-left me-1"></i> Request Return / Replace
                </a>
            ` : ''}
            ${order.canCancel ? `
                <button type="button" class="btn btn-danger btn-sm fw-semibold" onclick="cancelOrder(${order.id}, '${order.orderNumber}')">
                    <i class="bi bi-x-circle me-1"></i> Cancel Order
                </button>
            ` : ''}
            <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Close</button>
        `;
    } catch (e) {
        modalBody.innerHTML = `<div class="alert alert-danger small">Error: ${e.message}</div>`;
    }
}

async function cancelOrder(orderId, orderNumber) {
    if (!confirm(`Are you sure you want to cancel Order #${orderNumber}?\nInventory stock will be restored automatically.`)) {
        return;
    }

    try {
        const res = await ApiClient.put(`/orders/${orderId}/cancel`);
        if (res && res.success) {
            showToast(`Order #${orderNumber} has been cancelled`, 'success');
            // Hide modal if open
            const modalEl = document.getElementById('orderDetailsModal');
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) modalInstance.hide();

            await loadOrders();
        } else {
            showToast(res.message || 'Unable to cancel order', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Error cancelling order', 'danger');
    }
}

let currentRetryOrderId = null;

function openRetryPaymentModal(orderId, orderNumber, amount) {
    currentRetryOrderId = orderId;
    const numEl = document.getElementById('retryOrderNumber');
    const amtEl = document.getElementById('retryOrderAmount');
    const msgEl = document.getElementById('retryMsg');
    const btn = document.getElementById('btnExecuteRetry');

    if (numEl) numEl.textContent = `Order #${orderNumber}`;
    if (amtEl) amtEl.textContent = `₹${(Number(amount) || 0).toFixed(2)}`;
    if (msgEl) msgEl.innerHTML = '';
    if (btn) {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-lock-fill me-1"></i> Confirm Test Payment';
    }

    const modalEl = document.getElementById('paymentRetryModal');
    if (modalEl) {
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
    }
}

async function executePaymentRetry() {
    if (!currentRetryOrderId) return;

    const btn = document.getElementById('btnExecuteRetry');
    const msgEl = document.getElementById('retryMsg');
    const simSuccess = document.getElementById('retrySuccess') ? document.getElementById('retrySuccess').checked : true;

    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status"></span> Contacting Mock Gateway...';
    msgEl.innerHTML = '';

    try {
        const ref = simSuccess ? 'MOCK_RETRY_USER' : 'FAIL_SIMULATION';
        // 1. Call retry endpoint to create/refresh transaction
        const retryRes = await ApiClient.post(`/payments/order/${currentRetryOrderId}/retry`, {
            paymentReference: ref
        });

        // 2. Complete payment attempt via pay-online mock gateway
        const payRes = await ApiClient.post(`/orders/${currentRetryOrderId}/payment/pay-online`, {
            paymentReference: ref
        });

        if (payRes && payRes.success) {
            msgEl.innerHTML = '<div class="alert alert-success py-2 small"><i class="bi bi-check-circle me-1"></i> Payment verified successfully! Order is now paid.</div>';
            showToast('Payment retry successful!', 'success');
            setTimeout(async () => {
                const modalEl = document.getElementById('paymentRetryModal');
                const modalInstance = bootstrap.Modal.getInstance(modalEl);
                if (modalInstance) modalInstance.hide();
                await loadOrders();
            }, 1200);
        } else {
            msgEl.innerHTML = `<div class="alert alert-danger py-2 small"><i class="bi bi-x-circle me-1"></i> ${payRes.message || 'Payment simulation failed.'}</div>`;
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-arrow-repeat me-1"></i> Retry Again';
            await loadOrders();
        }
    } catch (err) {
        msgEl.innerHTML = `<div class="alert alert-danger py-2 small"><i class="bi bi-x-circle me-1"></i> ${err.message || 'Payment simulation failed.'}</div>`;
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-arrow-repeat me-1"></i> Retry Again';
        await loadOrders();
    }
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
