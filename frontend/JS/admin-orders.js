/**
 * SmartAgri - Admin Order Management & Lifecycle Controls
 */

let allAdminOrders = [];
let currentStatusFilter = 'ALL';
let currentSearchTerm = '';

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Verify admin authentication
    if (typeof requireLogin === 'function') {
        requireLogin();
    }

    const user = getUser();
    if (!user || user.role !== 'ADMIN') {
        alert('Access denied: Admin privileges required.');
        window.location.href = 'customer-catalog.html';
        return;
    }

    // 2. Setup sidebar toggle
    setupSidebar();

    // 3. Setup filters & tabs
    setupOrderFilters();

    // 4. Load initial orders
    await loadAdminOrders();
});

function setupSidebar() {
    const toggleBtn = document.getElementById('toggleBtn');
    const sidebar = document.getElementById('side');
    const main = document.getElementById('main');
    if (toggleBtn && sidebar && main) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('active');
            main.classList.toggle('active');
        });
    }
}

function setupOrderFilters() {
    const searchInput = document.getElementById('adminOrderSearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            currentSearchTerm = e.target.value.toLowerCase().trim();
            renderAdminOrders();
        });
    }

    const tabs = document.querySelectorAll('#statusFilterTabs .nav-link');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            currentStatusFilter = tab.dataset.status;
            renderAdminOrders();
        });
    });
}

async function loadAdminOrders() {
    const tbody = document.getElementById('adminOrdersTableBody');
    try {
        const res = await ApiClient.get('/admin/orders');
        if (res && res.success && Array.isArray(res.data)) {
            allAdminOrders = res.data;
            renderAdminOrders();
        } else {
            tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5 text-muted">No orders found.</td></tr>';
        }
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center py-4 text-danger">Error: ${e.message}</td></tr>`;
    }
}

function renderAdminOrders() {
    const tbody = document.getElementById('adminOrdersTableBody');
    if (!tbody) return;

    let filtered = allAdminOrders.filter(order => {
        if (currentStatusFilter !== 'ALL' && order.orderStatus !== currentStatusFilter) {
            return false;
        }

        if (currentSearchTerm) {
            const num = (order.orderNumber || '').toLowerCase();
            const cust = (order.customerName || order.customerUsername || '').toLowerCase();
            const recip = (order.recipientName || '').toLowerCase();
            const city = (order.city || '').toLowerCase();
            const trk = (order.trackingNumber || '').toLowerCase();

            if (!num.includes(currentSearchTerm) &&
                !cust.includes(currentSearchTerm) &&
                !recip.includes(currentSearchTerm) &&
                !city.includes(currentSearchTerm) &&
                !trk.includes(currentSearchTerm)) {
                return false;
            }
        }

        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center py-5 text-muted">No matching orders found.</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.map(order => {
        const total = Number(order.grandTotal) || 0;
        const statusClass = `status-badge-${(order.orderStatus || 'pending').toLowerCase()}`;
        const delStatus = order.deliveryStatus || 'NOT_ASSIGNED';
        const delBadgeClass = delStatus === 'DELIVERED' ? 'bg-success' :
                              delStatus === 'OUT_FOR_DELIVERY' ? 'bg-warning text-dark' :
                              delStatus === 'SHIPPED' ? 'bg-info text-dark' :
                              delStatus === 'PACKED' ? 'bg-purple text-white' :
                              delStatus === 'ASSIGNED' ? 'bg-primary' : 'bg-secondary';

        const dateStr = order.createdAt ? new Date(order.createdAt).toLocaleDateString('en-IN', {
            day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
        }) : 'Recent';

        // Compute valid next transitions
        const nextOptions = getNextStatusOptions(order.orderStatus);

        return `
            <tr>
                <td class="ps-3 fw-bold text-success">${order.orderNumber}</td>
                <td>
                    <div class="fw-semibold small">${order.customerName || order.customerUsername}</div>
                    <div class="text-muted small">${order.recipientName} (+91 ${order.recipientPhone})</div>
                </td>
                <td class="small text-muted">${dateStr}</td>
                <td class="small">${order.totalItemsCount || (order.items ? order.items.length : 1)} item(s)</td>
                <td class="fw-bold text-dark small">₹${total.toFixed(2)}</td>
                <td>
                    <span class="badge bg-light text-secondary border small">${order.paymentMethod} &bull; ${order.paymentStatus}</span>
                </td>
                <td>
                    <span class="badge ${delBadgeClass} px-2 py-1 rounded-pill small fw-bold">${delStatus.replace(/_/g, ' ')}</span>
                    ${order.trackingNumber ? `
                        <div class="font-monospace text-muted mt-1" style="font-size: 0.72rem;">${order.trackingNumber}</div>
                    ` : ''}
                </td>
                <td>
                    <span class="badge ${statusClass} px-2 py-1 rounded-pill small fw-bold">${order.orderStatus}</span>
                </td>
                <td class="text-end pe-3">
                    <div class="d-inline-flex gap-1 align-items-center">
                        ${nextOptions.length > 0 ? `
                            <select id="select-status-${order.id}" class="form-select form-select-sm" style="width: auto; font-size: 0.8rem;">
                                ${nextOptions.map(opt => `<option value="${opt}">${opt}</option>`).join('')}
                            </select>
                            <button class="btn btn-sm btn-outline-success" onclick="updateOrderStatusFromTable(${order.id})" title="Update Status">
                                <i class="bi bi-check-lg"></i>
                            </button>
                        ` : '<span class="text-muted small me-2">Terminal</span>'}

                        <button class="btn btn-sm btn-light border" onclick="viewAdminOrderModal(${order.id})" title="View Details">
                            <i class="bi bi-eye"></i>
                        </button>
                        <a href="customer-invoice.html?orderId=${order.id}" target="_blank" class="btn btn-sm btn-light border" title="GST Invoice">
                            <i class="bi bi-receipt"></i>
                        </a>
                        <a href="customer-order-tracking.html?orderId=${order.id}" target="_blank" class="btn btn-sm btn-light border text-primary" title="Live Tracking">
                            <i class="bi bi-truck"></i>
                        </a>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function getNextStatusOptions(currentStatus) {
    switch (currentStatus) {
        case 'PENDING':
            return ['CONFIRMED', 'CANCELLED'];
        case 'CONFIRMED':
            return ['PROCESSING', 'CANCELLED'];
        case 'PROCESSING':
            return ['PACKED'];
        case 'PACKED':
            return ['SHIPPED'];
        case 'SHIPPED':
            return ['DELIVERED'];
        default:
            return [];
    }
}

async function updateOrderStatusFromTable(orderId) {
    const select = document.getElementById(`select-status-${orderId}`);
    if (!select) return;

    const newStatus = select.value;
    if (!confirm(`Update Order #${orderId} status to ${newStatus}?`)) return;

    try {
        const res = await ApiClient.put(`/admin/orders/${orderId}/status`, { status: newStatus });
        if (res && res.success) {
            showToast(`Order status updated to ${newStatus}`, 'success');
            await loadAdminOrders();
        } else {
            showToast(res.message || 'Status update failed', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Error updating order status', 'danger');
    }
}

async function viewAdminOrderModal(orderId) {
    const modalNumber = document.getElementById('adminModalOrderNumber');
    const modalDate = document.getElementById('adminModalOrderDate');
    const modalBody = document.getElementById('adminModalOrderBody');

    modalBody.innerHTML = '<div class="text-center py-4"><div class="spinner-border spinner-border-sm text-success"></div></div>';
    const modal = new bootstrap.Modal(document.getElementById('adminOrderModal'));
    modal.show();

    try {
        const res = await ApiClient.get(`/admin/orders/${orderId}`);
        if (!res || !res.success || !res.data) {
            modalBody.innerHTML = `<div class="alert alert-danger">${res.message || 'Failed to load order'}</div>`;
            return;
        }

        const order = res.data;
        modalNumber.textContent = `Order #${order.orderNumber}`;
        const dateStr = order.createdAt ? new Date(order.createdAt).toLocaleString('en-IN') : 'Recent';
        modalDate.textContent = `Placed on: ${dateStr}`;

        const itemsHtml = (order.items || []).map(item => {
            const imgSrc = item.imageUrl || '../img/img1.jpeg';
            const unitPrice = Number(item.unitPrice) || 0;
            const lineTotal = Number(item.lineTotal) || 0;
            const discountAmt = Number(item.discountAmount) || 0;

            return `
                <tr>
                    <td style="width: 50px;">
                        <img src="${imgSrc}" alt="${item.productName}" style="width: 40px; height: 40px; object-fit: cover; border-radius: 4px;" onerror="this.src='../img/img1.jpeg'">
                    </td>
                    <td>
                        <div class="fw-bold small">${item.productName}</div>
                        <div class="text-muted small">SKU: ${item.sku} &bull; Brand: ${item.brand || 'SmartAgri'}</div>
                    </td>
                    <td class="text-center small">₹${unitPrice.toFixed(2)}</td>
                    <td class="text-center small">${item.quantity}</td>
                    <td class="text-center small text-success">${discountAmt > 0 ? '-₹' + discountAmt.toFixed(2) : '-'}</td>
                    <td class="text-end fw-bold small text-success">₹${lineTotal.toFixed(2)}</td>
                </tr>
            `;
        }).join('');

        const statusClass = `status-badge-${(order.orderStatus || 'pending').toLowerCase()}`;
        const delStatus = order.deliveryStatus || 'NOT_ASSIGNED';
        const delBadgeClass = delStatus === 'DELIVERED' ? 'bg-success' :
                              delStatus === 'OUT_FOR_DELIVERY' ? 'bg-warning text-dark' :
                              delStatus === 'SHIPPED' ? 'bg-info text-dark' :
                              delStatus === 'PACKED' ? 'bg-purple text-white' :
                              delStatus === 'ASSIGNED' ? 'bg-primary' : 'bg-secondary';

        modalBody.innerHTML = `
            <!-- Info Cards (3 Columns) -->
            <div class="row g-3 mb-4">
                <div class="col-md-4">
                    <div class="p-3 bg-light rounded-3 h-100">
                        <h6 class="fw-bold mb-2"><i class="bi bi-info-circle text-primary me-1"></i>Order & Payment</h6>
                        <div class="d-flex align-items-center gap-2 mb-2">
                            <span class="badge ${statusClass} px-3 py-1 rounded-pill fw-bold">${order.orderStatus}</span>
                            <span class="badge bg-white border text-secondary">${order.paymentMethod}</span>
                        </div>
                        <div class="small">Payment Status: <strong class="text-${order.paymentStatus === 'PAID' || order.paymentStatus === 'SUCCESS' ? 'success' : 'warning'}">${order.paymentStatus}</strong></div>
                        ${order.transactionId ? `<div class="small text-muted font-monospace mt-1">Txn: ${order.transactionId}</div>` : ''}
                        <div class="small mt-2">Customer: <strong>${order.customerName}</strong> (@${order.customerUsername})</div>
                        <div class="small text-muted">Email: ${order.customerEmail || 'N/A'}</div>
                        ${order.notes ? `<div class="small text-secondary mt-1"><i class="bi bi-chat-left-text me-1"></i>${order.notes}</div>` : ''}
                    </div>
                </div>

                <div class="col-md-4">
                    <div class="p-3 bg-light rounded-3 h-100">
                        <h6 class="fw-bold mb-2"><i class="bi bi-truck text-success me-1"></i>Delivery & Logistics</h6>
                        <div class="mb-2">
                            <span class="badge ${delBadgeClass} px-2 py-1 rounded-pill small fw-bold">${delStatus.replace(/_/g, ' ')}</span>
                        </div>
                        ${order.trackingNumber ? `
                            <div class="small text-muted">Tracking #: <strong class="text-dark font-monospace">${order.trackingNumber}</strong></div>
                            <div class="small text-muted">Carrier: <strong>${order.deliveryPartner || 'Standard Logistics'}</strong></div>
                            ${order.estimatedDeliveryDate ? `<div class="small text-muted">Est. Date: <strong>${order.estimatedDeliveryDate}</strong></div>` : ''}
                        ` : `<div class="small text-muted">Carrier not yet assigned.</div>`}
                        <div class="d-flex gap-2 mt-3">
                            <a href="admin-delivery.html" class="btn btn-outline-success btn-sm py-0 px-2" style="font-size: 0.75rem;">
                                <i class="bi bi-boxes me-1"></i>Fulfillment
                            </a>
                            <a href="customer-order-tracking.html?orderId=${order.id}" target="_blank" class="btn btn-outline-primary btn-sm py-0 px-2" style="font-size: 0.75rem;">
                                <i class="bi bi-geo-alt me-1"></i>Track
                            </a>
                        </div>
                    </div>
                </div>

                <div class="col-md-4">
                    <div class="p-3 bg-light rounded-3 h-100">
                        <h6 class="fw-bold mb-1"><i class="bi bi-geo-alt text-danger me-1"></i>Shipping Address</h6>
                        <div class="small fw-semibold">${order.recipientName}</div>
                        <div class="small text-muted"><i class="bi bi-telephone me-1"></i>+91 ${order.recipientPhone}</div>
                        <div class="small text-muted mt-1">${order.addressLine1}${order.addressLine2 ? ', ' + order.addressLine2 : ''}</div>
                        <div class="small text-muted">${order.city}, ${order.state} - <strong>${order.pincode}</strong></div>
                        ${order.landmark ? `<div class="small text-muted">Landmark: ${order.landmark}</div>` : ''}
                    </div>
                </div>
            </div>

            <!-- Items -->
            <h6 class="fw-bold mb-2">Order Items</h6>
            <div class="table-responsive mb-4 border rounded-3">
                <table class="table table-sm align-middle mb-0">
                    <thead class="table-light small">
                        <tr>
                            <th>Image</th>
                            <th>Product</th>
                            <th class="text-center">Unit Price</th>
                            <th class="text-center">Qty</th>
                            <th class="text-center">Discount</th>
                            <th class="text-end">Line Total</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${itemsHtml}
                    </tbody>
                </table>
            </div>

            <!-- Summary -->
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

        const modalFooter = document.getElementById('adminModalOrderFooter');
        if (modalFooter) {
            modalFooter.innerHTML = `
                <a href="customer-invoice.html?orderId=${order.id}" target="_blank" class="btn btn-outline-secondary btn-sm">
                    <i class="bi bi-receipt me-1"></i> GST Invoice
                </a>
                <a href="customer-order-tracking.html?orderId=${order.id}" target="_blank" class="btn btn-outline-primary btn-sm">
                    <i class="bi bi-truck me-1"></i> Tracking Page
                </a>
                <a href="admin-delivery.html" class="btn btn-outline-success btn-sm">
                    <i class="bi bi-boxes me-1"></i> Logistics Console
                </a>
                <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Close</button>
            `;
        }
    } catch (e) {
        modalBody.innerHTML = `<div class="alert alert-danger small">${e.message}</div>`;
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
