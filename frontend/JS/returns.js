/**
 * SmartAgri - Customer Returns, Refunds & Replacements Portal
 */

let myDeliveredOrders = [];
let selectedOrder = null;

document.addEventListener('DOMContentLoaded', async () => {
    checkCustomerAuth();
    await loadAllData();

    // Check if redirected from order history with ?orderId=
    const urlParams = new URLSearchParams(window.location.search);
    const orderIdParam = urlParams.get('orderId');
    if (orderIdParam) {
        await prepareReturnModal(orderIdParam);
        const modalEl = document.getElementById('newReturnModal');
        if (modalEl && typeof bootstrap !== 'undefined') {
            const modal = new bootstrap.Modal(modalEl);
            modal.show();
        }
    }
});

function checkCustomerAuth() {
    const token = localStorage.getItem('token') || localStorage.getItem('smartagriAuthToken') || localStorage.getItem('smartagri_auth_token');
    const userStr = localStorage.getItem('user') || localStorage.getItem('smartagriUser') || localStorage.getItem('smartagri_user');
    if (!token) {
        window.location.href = 'login.html';
        return;
    }
    if (userStr) {
        try {
            const user = JSON.parse(userStr);
            const greeting = document.getElementById('userGreeting');
            if (greeting) greeting.textContent = `Hello, ${user.name || user.username || 'Customer'}`;
        } catch (e) {}
    }
}

function logout() {
    ['smartagriUser', 'smartagri_user', 'user', 'smartagriAuthToken', 'smartagri_auth_token', 'token', 'role', 'username'].forEach(k => localStorage.removeItem(k));
    window.location.href = 'login.html';
}

async function loadAllData() {
    await Promise.all([
        loadMyReturns(),
        loadMyRefunds(),
        loadMyReplacements()
    ]);
}

async function loadMyReturns() {
    const container = document.getElementById('returnsListContainer');
    try {
        const res = await ApiClient.get('/customer/returns');
        if (res && res.success && Array.isArray(res.data)) {
            const list = res.data;
            document.getElementById('returnsCount').textContent = list.length;
            if (list.length === 0) {
                container.innerHTML = `
                    <div class="text-center py-5 bg-white border rounded-3 p-4">
                        <i class="bi bi-box2 fs-1 text-muted"></i>
                        <h5 class="mt-3">No return requests found</h5>
                        <p class="text-muted small">You haven't requested any returns or replacements yet.</p>
                        <button class="btn btn-outline-success btn-sm mt-2" data-bs-toggle="modal" data-bs-target="#newReturnModal" onclick="prepareReturnModal()">
                            Request a Return
                        </button>
                    </div>
                `;
                return;
            }

            container.innerHTML = list.map(ret => renderReturnCard(ret)).join('');
        }
    } catch (e) {
        container.innerHTML = `<div class="alert alert-danger">Failed to load returns: ${e.message}</div>`;
    }
}

function renderReturnCard(ret) {
    const statusBadges = {
        'REQUESTED': 'bg-secondary',
        'UNDER_REVIEW': 'bg-info text-dark',
        'APPROVED': 'bg-primary',
        'PICKUP_SCHEDULED': 'bg-warning text-dark',
        'PICKED_UP': 'bg-info text-dark',
        'IN_TRANSIT': 'bg-primary',
        'RECEIVED': 'bg-info text-dark',
        'INSPECTING': 'bg-warning text-dark',
        'INSPECTION_COMPLETED': 'bg-primary',
        'REFUND_PENDING': 'bg-warning text-dark',
        'REFUNDED': 'bg-success',
        'REPLACEMENT_PENDING': 'bg-warning text-dark',
        'REPLACED': 'bg-success',
        'REJECTED': 'bg-danger',
        'CANCELLED': 'bg-dark',
        'RETURNED_TO_CUSTOMER': 'bg-secondary'
    };
    const badgeClass = statusBadges[ret.status] || 'bg-secondary';
    const dateStr = ret.requestedAt ? new Date(ret.requestedAt).toLocaleDateString('en-IN', {
        day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    }) : 'Recent';

    const canCancel = ret.status === 'REQUESTED' || ret.status === 'UNDER_REVIEW';

    const itemsHtml = (ret.items || []).map(i => `
        <div class="d-flex align-items-center justify-content-between py-2 border-bottom">
            <div>
                <strong>${i.fertilizerName || 'Product #' + i.fertilizerId}</strong>
                <div class="small text-muted">
                    Qty: <span class="badge bg-light text-dark border">${i.requestedQuantity} requested</span>
                    ${i.approvedQuantity > 0 ? `<span class="badge bg-success-subtle text-success border ms-1">${i.approvedQuantity} approved</span>` : ''}
                    <span class="badge bg-secondary-subtle text-secondary border ms-1">${i.resolution}</span>
                </div>
            </div>
            <div class="text-end small">
                <span class="badge bg-light text-muted">${i.itemCondition}</span>
            </div>
        </div>
    `).join('');

    return `
        <div class="return-card">
            <div class="return-card-header d-flex flex-wrap justify-content-between align-items-center gap-2">
                <div>
                    <span class="fw-bold fs-6 me-2">${ret.returnNumber}</span>
                    <span class="badge ${badgeClass}">${ret.status.replace(/_/g, ' ')}</span>
                    <span class="text-muted small ms-2">Order: <strong>${ret.orderTrackingNumber || '#' + ret.orderId}</strong></span>
                </div>
                <div class="text-muted small">
                    <i class="bi bi-calendar3 me-1"></i> ${dateStr}
                </div>
            </div>
            <div class="return-card-body">
                <div class="mb-3">
                    ${itemsHtml}
                </div>
                <div class="d-flex flex-wrap justify-content-between align-items-center pt-2">
                    <div class="small text-muted">
                        <strong>Reason:</strong> ${ret.reason ? ret.reason.replace(/_/g, ' ') : 'N/A'}
                        ${ret.customerComments ? `<span class="ms-2 fst-italic">"${ret.customerComments}"</span>` : ''}
                        ${ret.rejectionReason ? `<div class="text-danger mt-1"><strong>Rejection Reason:</strong> ${ret.rejectionReason}</div>` : ''}
                    </div>
                    <div class="d-flex gap-2 mt-2 mt-md-0">
                        ${ret.shipment ? `
                            <button class="btn btn-outline-success btn-sm" onclick="showTracking(${ret.id})">
                                <i class="bi bi-geo-alt me-1"></i> Track Shipment
                            </button>
                        ` : ''}
                        ${canCancel ? `
                            <button class="btn btn-outline-danger btn-sm" onclick="cancelReturn(${ret.id})">
                                <i class="bi bi-x-circle me-1"></i> Cancel Return
                            </button>
                        ` : ''}
                    </div>
                </div>
            </div>
        </div>
    `;
}

async function loadMyRefunds() {
    const container = document.getElementById('refundsListContainer');
    try {
        const res = await ApiClient.get('/customer/refunds');
        if (res && res.success && Array.isArray(res.data)) {
            const list = res.data;
            document.getElementById('refundsCount').textContent = list.length;
            if (list.length === 0) {
                container.innerHTML = `
                    <div class="text-center py-5 bg-white border rounded-3 p-4">
                        <i class="bi bi-cash-stack fs-1 text-muted"></i>
                        <h5 class="mt-3">No refunds yet</h5>
                        <p class="text-muted small">Processed refunds will appear here.</p>
                    </div>
                `;
                return;
            }

            container.innerHTML = list.map(ref => {
                const isPendingBank = ref.status === 'PENDING_CUSTOMER_REFUND_DETAILS';
                const statusBadge = ref.status === 'PROCESSED' ? 'bg-success' : isPendingBank ? 'bg-warning text-dark' : 'bg-primary';
                return `
                    <div class="card mb-3 border-0 shadow-sm">
                        <div class="card-body d-flex flex-wrap justify-content-between align-items-center gap-3">
                            <div>
                                <h6 class="fw-bold mb-1">${ref.refundNumber} <span class="badge ${statusBadge} ms-2">${ref.status.replace(/_/g, ' ')}</span></h6>
                                <div class="text-muted small">
                                    <span>Amount: <strong class="text-success fs-6">₹${Number(ref.amount).toFixed(2)}</strong></span>
                                    <span class="mx-2">•</span>
                                    <span>Method: ${ref.paymentMethod || 'Online'}</span>
                                    ${ref.paymentReference ? `<span class="mx-2">•</span><span>Ref: ${ref.paymentReference}</span>` : ''}
                                </div>
                                ${isPendingBank ? `<div class="text-danger small mt-1"><i class="bi bi-exclamation-circle me-1"></i>Bank details required to deposit refund.</div>` : ''}
                            </div>
                            <div>
                                ${isPendingBank ? `
                                    <button class="btn btn-warning btn-sm fw-bold" onclick="openBankDetailsModal(${ref.id})">
                                        <i class="bi bi-bank me-1"></i> Provide Bank Details
                                    </button>
                                ` : ''}
                            </div>
                        </div>
                    </div>
                `;
            }).join('');
        }
    } catch (e) {
        container.innerHTML = `<div class="alert alert-danger">${e.message}</div>`;
    }
}

async function loadMyReplacements() {
    const container = document.getElementById('replacementsListContainer');
    try {
        const res = await ApiClient.get('/customer/replacements');
        if (res && res.success && Array.isArray(res.data)) {
            const list = res.data;
            document.getElementById('replacementsCount').textContent = list.length;
            if (list.length === 0) {
                container.innerHTML = `
                    <div class="text-center py-5 bg-white border rounded-3 p-4">
                        <i class="bi bi-arrow-left-right fs-1 text-muted"></i>
                        <h5 class="mt-3">No replacements yet</h5>
                        <p class="text-muted small">Replacement orders generated for approved returns will appear here.</p>
                    </div>
                `;
                return;
            }

            container.innerHTML = list.map(rep => `
                <div class="card mb-3 border-0 shadow-sm">
                    <div class="card-body">
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <h6 class="fw-bold mb-0">Replacement Order: ${rep.replacementOrderTrackingNumber || '#' + rep.replacementOrderId}</h6>
                            <span class="badge bg-success">FREE REPLACEMENT (₹0.00)</span>
                        </div>
                        <div class="small text-muted">
                            <span>Origin Return: <strong>${rep.returnNumber}</strong></span>
                            <span class="mx-2">•</span>
                            <span>Original Order: <strong>${rep.originalOrderTrackingNumber || '#' + rep.originalOrderId}</strong></span>
                        </div>
                        <p class="small text-muted mt-2 mb-0">${rep.notes || 'Replacement order being fulfilled by nearest warehouse.'}</p>
                    </div>
                </div>
            `).join('');
        }
    } catch (e) {
        container.innerHTML = `<div class="alert alert-danger">${e.message}</div>`;
    }
}

async function prepareReturnModal(preselectedOrderId = null) {
    const select = document.getElementById('returnOrderSelect');
    select.innerHTML = '<option value="">-- Choose an eligible delivered order --</option>';
    document.getElementById('orderItemsSection').classList.add('d-none');
    document.getElementById('orderItemsList').innerHTML = '';

    try {
        const res = await ApiClient.get('/orders');
        if (res && res.success && Array.isArray(res.data)) {
            // Filter delivered orders within 14 days
            myDeliveredOrders = res.data.filter(o => o.orderStatus === 'DELIVERED');
            if (myDeliveredOrders.length === 0) {
                select.innerHTML = '<option value="">No delivered orders available for return</option>';
                return;
            }
            myDeliveredOrders.forEach(o => {
                const opt = document.createElement('option');
                opt.value = o.id;
                opt.textContent = `Order ${o.orderNumber} (Placed on ${new Date(o.createdAt).toLocaleDateString()}, Total: ₹${o.grandTotal})`;
                select.appendChild(opt);
            });

            if (preselectedOrderId) {
                select.value = preselectedOrderId;
                onOrderSelected();
            }
        }
    } catch (e) {
        alert('Failed to load delivered orders: ' + e.message);
    }
}

function onOrderSelected() {
    const orderId = document.getElementById('returnOrderSelect').value;
    const itemsSection = document.getElementById('orderItemsSection');
    const itemsList = document.getElementById('orderItemsList');

    if (!orderId) {
        itemsSection.classList.add('d-none');
        return;
    }

    selectedOrder = myDeliveredOrders.find(o => o.id == orderId);
    if (!selectedOrder || !selectedOrder.items || selectedOrder.items.length === 0) {
        itemsList.innerHTML = '<div class="text-muted small">No items found for this order.</div>';
        itemsSection.classList.remove('d-none');
        return;
    }

    // Prefill pickup address
    const pickupAddr = document.getElementById('pickupAddress');
    if (pickupAddr) {
        pickupAddr.value = `${selectedOrder.addressLine1 || ''}, ${selectedOrder.city || ''}, ${selectedOrder.state || ''} - ${selectedOrder.pincode || ''}`;
    }

    itemsList.innerHTML = selectedOrder.items.map(item => `
        <div class="card p-2 mb-2 bg-white border">
            <div class="form-check d-flex align-items-center justify-content-between">
                <div>
                    <input class="form-check-input item-select-check me-2" type="checkbox" id="check_item_${item.id}" value="${item.id}" onchange="toggleItemInputs(${item.id})">
                    <label class="form-check-label fw-bold" for="check_item_${item.id}">
                        ${item.productName || 'Fertilizer #' + item.id}
                    </label>
                    <div class="small text-muted">Purchased Qty: ${item.quantity} | Unit Price: ₹${item.unitPrice}</div>
                </div>
                <div class="d-flex align-items-center gap-2">
                    <label class="small text-muted mb-0">Return Qty:</label>
                    <input type="number" class="form-control form-control-sm text-center" id="qty_item_${item.id}" value="1" min="1" max="${item.quantity}" style="width: 70px;" disabled>
                </div>
            </div>
        </div>
    `).join('');

    itemsSection.classList.remove('d-none');
}

function toggleItemInputs(itemId) {
    const check = document.getElementById(`check_item_${itemId}`);
    const qtyInput = document.getElementById(`qty_item_${itemId}`);
    if (qtyInput) {
        qtyInput.disabled = !check.checked;
    }
}

async function submitReturnRequest(event) {
    event.preventDefault();
    if (!selectedOrder) {
        alert('Please select an order first.');
        return;
    }

    const checks = document.querySelectorAll('.item-select-check:checked');
    if (checks.length === 0) {
        alert('Please select at least one item to return.');
        return;
    }

    const resolution = document.getElementById('returnResolution').value;
    const items = [];
    checks.forEach(c => {
        const itemId = Number(c.value);
        const qty = Number(document.getElementById(`qty_item_${itemId}`).value) || 1;
        items.push({
            orderItemId: itemId,
            quantity: qty,
            condition: 'OPENED',
            resolution: resolution,
            reason: document.getElementById('returnReason').value
        });
    });

    const payload = {
        orderId: selectedOrder.id,
        reason: document.getElementById('returnReason').value,
        customerComments: document.getElementById('customerComments').value,
        pickupAddress: document.getElementById('pickupAddress').value,
        bankAccountDetails: document.getElementById('bankAccountDetails').value,
        items: items
    };

    const btn = document.getElementById('btnSubmitReturn');
    btn.disabled = true;
    btn.textContent = 'Submitting...';

    try {
        const res = await ApiClient.post('/customer/returns', payload);
        if (res && res.success) {
            bootstrap.Modal.getInstance(document.getElementById('newReturnModal')).hide();
            alert('Return request submitted successfully! Return #' + res.data.returnNumber);
            await loadAllData();
        } else {
            alert('Error: ' + (res.message || 'Failed to submit return request'));
        }
    } catch (e) {
        alert('Submission failed: ' + e.message);
    } finally {
        btn.disabled = false;
        btn.textContent = 'Submit Return Request';
    }
}

async function cancelReturn(returnId) {
    if (!confirm('Are you sure you want to cancel this return request?')) return;
    try {
        const res = await ApiClient.post(`/customer/returns/${returnId}/cancel`, {});
        if (res && res.success) {
            alert('Return request cancelled.');
            await loadAllData();
        } else {
            alert('Cancellation failed: ' + (res.message || 'Unknown error'));
        }
    } catch (e) {
        alert('Cancellation error: ' + e.message);
    }
}

async function showTracking(returnId) {
    const modalBody = document.getElementById('trackingModalBody');
    modalBody.innerHTML = '<div class="text-center py-4"><div class="spinner-border text-success"></div></div>';
    const modal = new bootstrap.Modal(document.getElementById('trackingModal'));
    modal.show();

    try {
        const res = await ApiClient.get(`/customer/returns/${returnId}/tracking`);
        if (res && res.success && res.data) {
            const shp = res.data;
            const events = shp.trackingEvents || [];

            const eventsHtml = events.length > 0 ? events.map(e => `
                <div class="timeline-event-item">
                    <div class="timeline-event-dot"></div>
                    <div class="fw-bold">${e.status.replace(/_/g, ' ')}</div>
                    <div class="small text-muted">${e.description || ''}</div>
                    <div class="small text-secondary">${e.location ? '<i class="bi bi-geo-alt me-1"></i>' + e.location : ''} • ${new Date(e.eventTime).toLocaleString()}</div>
                </div>
            `).join('') : '<div class="text-muted small">No tracking events recorded yet.</div>';

            modalBody.innerHTML = `
                <div class="mb-3 p-3 bg-light rounded">
                    <div><strong>Shipment Number:</strong> ${shp.shipmentNumber}</div>
                    <div><strong>Tracking Number:</strong> <span class="badge bg-dark">${shp.trackingNumber}</span></div>
                    <div><strong>Carrier:</strong> ${shp.carrier || 'Pending Assignment'}</div>
                    <div><strong>Current Status:</strong> <span class="badge bg-success">${shp.status.replace(/_/g, ' ')}</span></div>
                    <div><strong>Pickup Address:</strong> ${shp.pickupAddress || 'Customer registered address'}</div>
                </div>
                <h6 class="fw-bold mb-3">Logistics Activity Timeline</h6>
                <div>
                    ${eventsHtml}
                </div>
            `;
        } else {
            modalBody.innerHTML = '<div class="alert alert-warning">No tracking data available.</div>';
        }
    } catch (e) {
        modalBody.innerHTML = `<div class="alert alert-danger">${e.message}</div>`;
    }
}

function openBankDetailsModal(refundId) {
    document.getElementById('bankRefundId').value = refundId;
    document.getElementById('modalBankDetails').value = '';
    new bootstrap.Modal(document.getElementById('bankDetailsModal')).show();
}

async function submitBankDetails(event) {
    event.preventDefault();
    const refundId = document.getElementById('bankRefundId').value;
    const details = document.getElementById('modalBankDetails').value;

    try {
        const res = await ApiClient.post(`/customer/refunds/${refundId}/bank-details`, { bankAccountDetails: details });
        if (res && res.success) {
            bootstrap.Modal.getInstance(document.getElementById('bankDetailsModal')).hide();
            alert('Bank details updated successfully! Your refund will be processed shortly.');
            await loadMyRefunds();
        } else {
            alert('Failed: ' + (res.message || 'Unknown error'));
        }
    } catch (e) {
        alert('Update error: ' + e.message);
    }
}
