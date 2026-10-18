/**
 * SmartAgri - Admin Returns, Reverse Logistics, Restocking & Refunds Console
 */

let allReturns = [];
let allRefunds = [];
let allWarehouses = [];
let activeReturnForInspection = null;

document.addEventListener('DOMContentLoaded', async () => {
    checkAdminAuth();
    await loadWarehouses();
    await loadDashboardData();
    await loadReturnsTable();
    await loadRefundsTable();
});

function checkAdminAuth() {
    const token = localStorage.getItem('token') || localStorage.getItem('smartagriAuthToken') || localStorage.getItem('smartagri_auth_token');
    const userStr = localStorage.getItem('user') || localStorage.getItem('smartagriUser') || localStorage.getItem('smartagri_user');
    const role = localStorage.getItem('role');
    const loginPath = window.location.pathname.includes('/admin/') ? '../HTML/login.html' : 'login.html';

    if (!token) {
        window.location.href = loginPath;
        return;
    }
    if (userStr || role) {
        try {
            const user = userStr ? JSON.parse(userStr) : { role: role };
            const effectiveRole = user.role || role;
            if (effectiveRole !== 'ADMIN' && effectiveRole !== 'ROLE_ADMIN') {
                alert('Access restricted to administrators.');
                window.location.href = loginPath;
                return;
            }
            const greeting = document.getElementById('adminUserGreeting');
            if (greeting) greeting.textContent = `${user.name || user.username || 'Admin'} (${effectiveRole})`;
        } catch (e) {}
    }
}

function logoutAdmin() {
    ['smartagriUser', 'smartagri_user', 'user', 'smartagriAuthToken', 'smartagri_auth_token', 'token', 'role', 'username'].forEach(k => localStorage.removeItem(k));
    window.location.href = window.location.pathname.includes('/admin/') ? '../HTML/login.html' : 'login.html';
}

async function loadWarehouses() {
    try {
        const res = await ApiClient.get('/warehouses');
        if (res && res.success && Array.isArray(res.data)) {
            allWarehouses = res.data;
            populateWarehouseDropdowns();
        }
    } catch (e) {
        console.error('Failed to load warehouses', e);
    }
}

function populateWarehouseDropdowns() {
    const selects = ['approveWarehouseSelect', 'pickupWarehouseSelect'];
    selects.forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        el.innerHTML = '<option value="">-- Choose Warehouse --</option>';
        allWarehouses.forEach(wh => {
            const opt = document.createElement('option');
            opt.value = wh.id;
            opt.textContent = `${wh.warehouseName} (${wh.city || ''})`;
            el.appendChild(opt);
        });
    });
}

async function loadDashboardData() {
    try {
        const res = await ApiClient.get('/admin/returns/dashboard');
        if (res && res.success && res.data) {
            const d = res.data;
            document.getElementById('kpiTotalReturns').textContent = d.totalReturns || 0;
            document.getElementById('kpiPendingReturns').textContent = d.pendingReturns || 0;
            document.getElementById('kpiApprovedReturns').textContent = d.approvedReturns || 0;
            document.getElementById('kpiInInspection').textContent = d.inInspection || 0;
            document.getElementById('kpiCompletedReturns').textContent = d.completedReturns || 0;
            document.getElementById('kpiTotalRefunded').textContent = `₹${Number(d.totalRefundedAmount || 0).toFixed(2)}`;
        }
    } catch (e) {
        console.error('Failed to load dashboard KPIs', e);
    }
}

async function loadReturnsTable() {
    const status = document.getElementById('requestStatusFilter').value;
    const url = status ? `/admin/returns?status=${status}` : '/admin/returns';
    const tbody = document.getElementById('returnsTableBody');

    try {
        const res = await ApiClient.get(url);
        if (res && res.success && Array.isArray(res.data)) {
            allReturns = res.data;
            renderReturnsTable(allReturns);
            renderLogisticsTable(allReturns);
            renderInspectionsTable(allReturns);
        }
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-danger py-4 text-center">Error loading returns: ${e.message}</td></tr>`;
    }
}

function renderReturnsTable(list) {
    const tbody = document.getElementById('returnsTableBody');
    if (!list || list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4 text-muted">No return requests found.</td></tr>';
        return;
    }

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
        'CANCELLED': 'bg-dark'
    };

    tbody.innerHTML = list.map(r => {
        const badge = statusBadges[r.status] || 'bg-secondary';
        const dateStr = r.requestedAt ? new Date(r.requestedAt).toLocaleDateString() : '';
        const itemsCount = (r.items || []).reduce((acc, i) => acc + (i.requestedQuantity || 0), 0);
        const canReview = r.status === 'REQUESTED' || r.status === 'UNDER_REVIEW';

        return `
            <tr>
                <td><span class="fw-bold text-success">${r.returnNumber}</span></td>
                <td><span class="badge bg-light text-dark border">${r.orderTrackingNumber || '#' + r.orderId}</span></td>
                <td>${r.customerName || 'Customer #' + r.customerId}</td>
                <td><span class="small">${r.reason ? r.reason.replace(/_/g, ' ') : 'N/A'}</span></td>
                <td><span class="badge bg-light text-dark border">${itemsCount} units</span></td>
                <td><span class="badge ${badge}">${r.status.replace(/_/g, ' ')}</span></td>
                <td><small class="text-muted">${dateStr}</small></td>
                <td class="text-end">
                    <button class="btn btn-outline-secondary btn-sm" onclick="viewReturnDetail(${r.id})"><i class="bi bi-eye"></i></button>
                    ${canReview ? `
                        <button class="btn btn-success btn-sm ms-1" onclick="openApproveModal(${r.id})"><i class="bi bi-check-lg"></i></button>
                        <button class="btn btn-danger btn-sm ms-1" onclick="openRejectModal(${r.id})"><i class="bi bi-x-lg"></i></button>
                    ` : ''}
                </td>
            </tr>
        `;
    }).join('');
}

function renderLogisticsTable(list) {
    const tbody = document.getElementById('logisticsTableBody');
    const logisticsList = list.filter(r => ['APPROVED', 'PICKUP_SCHEDULED', 'PICKED_UP', 'IN_TRANSIT'].includes(r.status));

    if (logisticsList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-4 text-muted">No active reverse logistics shipments.</td></tr>';
        return;
    }

    tbody.innerHTML = logisticsList.map(r => {
        const shp = r.shipment || {};
        return `
            <tr>
                <td><strong>${r.returnNumber}</strong></td>
                <td><span class="badge bg-dark">${shp.trackingNumber || 'Pending'}</span></td>
                <td>${shp.carrier || 'Not Assigned'}</td>
                <td><small class="text-muted">${shp.pickupAddress || 'Customer Address'}</small></td>
                <td>${shp.destinationWarehouseName || 'Main Hub'}</td>
                <td><span class="badge bg-primary">${(shp.status || r.status).replace(/_/g, ' ')}</span></td>
                <td class="text-end">
                    ${r.status === 'APPROVED' ? `
                        <button class="btn btn-primary btn-sm" onclick="openPickupModal(${r.id})"><i class="bi bi-calendar-event me-1"></i> Schedule Pickup</button>
                    ` : ''}
                    ${r.status === 'PICKUP_SCHEDULED' ? `
                        <button class="btn btn-info btn-sm text-dark" onclick="updateLogisticsStatus(${r.id}, 'PICKED_UP')"><i class="bi bi-box-arrow-up me-1"></i> Mark Picked Up</button>
                    ` : ''}
                    ${r.status === 'PICKED_UP' ? `
                        <button class="btn btn-primary btn-sm" onclick="updateLogisticsStatus(${r.id}, 'IN_TRANSIT')"><i class="bi bi-truck me-1"></i> Mark In-Transit</button>
                    ` : ''}
                    ${r.status === 'IN_TRANSIT' ? `
                        <button class="btn btn-success btn-sm" onclick="receiveReturn(${r.id})"><i class="bi bi-building-down me-1"></i> Receive at Warehouse</button>
                    ` : ''}
                </td>
            </tr>
        `;
    }).join('');
}

function renderInspectionsTable(list) {
    const tbody = document.getElementById('inspectionsTableBody');
    const inspectionList = list.filter(r => ['RECEIVED', 'INSPECTING'].includes(r.status));

    if (inspectionList.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center py-4 text-muted">No returns currently awaiting quality inspection.</td></tr>';
        return;
    }

    tbody.innerHTML = inspectionList.map(r => {
        const shp = r.shipment || {};
        const dateStr = r.receivedAt ? new Date(r.receivedAt).toLocaleString() : 'Just now';
        const items = r.items || [];
        const units = items.reduce((acc, i) => acc + (i.receivedQuantity > 0 ? i.receivedQuantity : i.approvedQuantity), 0);

        return `
            <tr>
                <td><strong>${r.returnNumber}</strong></td>
                <td>${r.customerName || '#' + r.customerId}</td>
                <td>${shp.destinationWarehouseName || 'Central Warehouse'}</td>
                <td><small class="text-muted">${dateStr}</small></td>
                <td><span class="badge bg-light text-dark border">${units} units</span></td>
                <td class="text-end">
                    <button class="btn btn-warning btn-sm fw-bold" onclick="openInspectModal(${r.id})">
                        <i class="bi bi-shield-check me-1"></i> Start Inspection
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

async function loadRefundsTable() {
    const status = document.getElementById('refundStatusFilter').value;
    const url = status ? `/admin/refunds?status=${status}` : '/admin/refunds';
    const tbody = document.getElementById('refundsTableBody');

    try {
        const res = await ApiClient.get(url);
        if (res && res.success && Array.isArray(res.data)) {
            allRefunds = res.data;
            if (allRefunds.length === 0) {
                tbody.innerHTML = '<tr><td colspan="8" class="text-center py-4 text-muted">No refunds found.</td></tr>';
                return;
            }

            tbody.innerHTML = allRefunds.map(ref => {
                const isProcessed = ref.status === 'PROCESSED';
                const isPendingBank = ref.status === 'PENDING_CUSTOMER_REFUND_DETAILS';
                const statusBadge = isProcessed ? 'bg-success' : isPendingBank ? 'bg-warning text-dark' : 'bg-primary';

                return `
                    <tr>
                        <td><strong>${ref.refundNumber}</strong></td>
                        <td>${ref.returnRequestId ? '#' + ref.returnRequestId : 'Manual'}</td>
                        <td>#${ref.orderId}</td>
                        <td><strong class="text-success">₹${Number(ref.amount).toFixed(2)}</strong></td>
                        <td>${ref.paymentMethod || 'Online'}</td>
                        <td><small class="text-muted">${ref.bankAccountDetails || 'N/A'}</small></td>
                        <td><span class="badge ${statusBadge}">${ref.status.replace(/_/g, ' ')}</span></td>
                        <td class="text-end">
                            ${!isProcessed ? `
                                <button class="btn btn-success btn-sm" onclick="openProcessRefundModal(${ref.id})">
                                    <i class="bi bi-cash-stack me-1"></i> Process Refund
                                </button>
                            ` : `<span class="badge bg-light text-muted">Completed</span>`}
                        </td>
                    </tr>
                `;
            }).join('');
        }
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-danger py-4 text-center">${e.message}</td></tr>`;
    }
}

function filterReturnsTable() {
    const q = document.getElementById('searchReturnsInput').value.toLowerCase();
    const filtered = allReturns.filter(r =>
        (r.returnNumber && r.returnNumber.toLowerCase().includes(q)) ||
        (r.orderTrackingNumber && r.orderTrackingNumber.toLowerCase().includes(q)) ||
        (r.customerName && r.customerName.toLowerCase().includes(q))
    );
    renderReturnsTable(filtered);
}

async function viewReturnDetail(id) {
    const modalBody = document.getElementById('returnDetailBody');
    modalBody.innerHTML = '<div class="text-center py-4"><div class="spinner-border text-success"></div></div>';
    new bootstrap.Modal(document.getElementById('returnDetailModal')).show();

    try {
        const res = await ApiClient.get(`/admin/returns/${id}`);
        if (res && res.success && res.data) {
            const r = res.data;
            document.getElementById('returnDetailTitle').textContent = `Return #${r.returnNumber}`;

            const itemsHtml = (r.items || []).map(i => `
                <div class="border rounded p-2 mb-2 bg-light d-flex justify-content-between">
                    <div>
                        <strong>${i.fertilizerName || 'Item #' + i.fertilizerId}</strong>
                        <div class="small text-muted">
                            Requested: ${i.requestedQuantity} | Approved: ${i.approvedQuantity} | Accepted: ${i.acceptedQuantity}
                            <span class="badge bg-secondary ms-2">${i.resolution}</span>
                        </div>
                    </div>
                    <div class="text-end small">
                        <div>Batch: <strong>#${i.batchId || 'N/A'}</strong></div>
                        <div>Bin: <strong>#${i.locationId || 'N/A'}</strong></div>
                    </div>
                </div>
            `).join('');

            modalBody.innerHTML = `
                <div class="row g-3 mb-3">
                    <div class="col-md-6">
                        <h6>Order & Customer</h6>
                        <div>Order: <strong>${r.orderTrackingNumber || '#' + r.orderId}</strong></div>
                        <div>Customer: <strong>${r.customerName}</strong> (${r.customerEmail})</div>
                        <div>Status: <span class="badge bg-success">${r.status}</span></div>
                    </div>
                    <div class="col-md-6">
                        <h6>Timestamps</h6>
                        <small class="text-muted d-block">Requested: ${r.requestedAt ? new Date(r.requestedAt).toLocaleString() : 'N/A'}</small>
                        <small class="text-muted d-block">Approved: ${r.approvedAt ? new Date(r.approvedAt).toLocaleString() : 'N/A'}</small>
                        <small class="text-muted d-block">Completed: ${r.completedAt ? new Date(r.completedAt).toLocaleString() : 'N/A'}</small>
                    </div>
                </div>
                <h6>Return Items</h6>
                ${itemsHtml}
                ${r.customerComments ? `<div class="mt-3"><strong>Customer Comments:</strong> <span class="fst-italic text-muted">"${r.customerComments}"</span></div>` : ''}
                ${r.adminNotes ? `<div class="mt-2"><strong>Admin Notes:</strong> <span class="text-muted">${r.adminNotes}</span></div>` : ''}
            `;
        }
    } catch (e) {
        modalBody.innerHTML = `<div class="alert alert-danger">${e.message}</div>`;
    }
}

function openApproveModal(id) {
    document.getElementById('approveReturnId').value = id;
    document.getElementById('approveAdminNotes').value = '';
    new bootstrap.Modal(document.getElementById('approveReturnModal')).show();
}

async function submitApproveReturn(event) {
    event.preventDefault();
    const id = document.getElementById('approveReturnId').value;
    const notes = document.getElementById('approveAdminNotes').value;
    const whId = document.getElementById('approveWarehouseSelect').value;

    const payload = {
        adminNotes: notes,
        destinationWarehouseId: whId ? Number(whId) : null
    };

    try {
        const res = await ApiClient.post(`/admin/returns/${id}/approve`, payload);
        if (res && res.success) {
            bootstrap.Modal.getInstance(document.getElementById('approveReturnModal')).hide();
            alert('Return approved successfully.');
            await loadDashboardData();
            await loadReturnsTable();
        } else {
            alert('Approval failed: ' + (res.message || 'Unknown error'));
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

function openRejectModal(id) {
    document.getElementById('rejectReturnId').value = id;
    document.getElementById('rejectReasonInput').value = '';
    document.getElementById('rejectAdminNotes').value = '';
    new bootstrap.Modal(document.getElementById('rejectReturnModal')).show();
}

async function submitRejectReturn(event) {
    event.preventDefault();
    const id = document.getElementById('rejectReturnId').value;
    const reason = document.getElementById('rejectReasonInput').value;
    const notes = document.getElementById('rejectAdminNotes').value;

    try {
        const res = await ApiClient.post(`/admin/returns/${id}/reject`, { rejectionReason: reason, adminNotes: notes });
        if (res && res.success) {
            bootstrap.Modal.getInstance(document.getElementById('rejectReturnModal')).hide();
            alert('Return rejected.');
            await loadDashboardData();
            await loadReturnsTable();
        } else {
            alert('Rejection failed: ' + (res.message || 'Unknown error'));
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

function openPickupModal(id) {
    const ret = allReturns.find(r => r.id == id);
    document.getElementById('pickupReturnId').value = id;
    document.getElementById('pickupAddressField').value = ret && ret.shipment ? ret.shipment.pickupAddress : '';
    new bootstrap.Modal(document.getElementById('pickupModal')).show();
}

async function submitSchedulePickup(event) {
    event.preventDefault();
    const id = document.getElementById('pickupReturnId').value;
    const carrier = document.getElementById('pickupCarrier').value;
    const timeVal = document.getElementById('pickupTime').value;
    const whId = document.getElementById('pickupWarehouseSelect').value;
    const addr = document.getElementById('pickupAddressField').value;

    const payload = {
        carrier: carrier,
        pickupScheduledAt: timeVal ? timeVal : null,
        destinationWarehouseId: whId ? Number(whId) : null,
        pickupAddress: addr
    };

    try {
        const res = await ApiClient.post(`/admin/returns/${id}/pickup`, payload);
        if (res && res.success) {
            bootstrap.Modal.getInstance(document.getElementById('pickupModal')).hide();
            alert('Pickup scheduled successfully.');
            await loadDashboardData();
            await loadReturnsTable();
        } else {
            alert('Scheduling failed: ' + (res.message || 'Unknown error'));
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

async function updateLogisticsStatus(id, newStatus) {
    try {
        const res = await ApiClient.post(`/admin/returns/${id}/shipment-status`, {
            status: newStatus,
            location: 'Central Transit Hub',
            notes: `Package status updated to ${newStatus}`
        });
        if (res && res.success) {
            await loadDashboardData();
            await loadReturnsTable();
        } else {
            alert('Status update failed: ' + (res.message || 'Unknown error'));
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

async function receiveReturn(id) {
    if (!confirm('Confirm receiving this return package at the destination warehouse?')) return;
    try {
        const res = await ApiClient.post(`/admin/returns/${id}/receive`, { notes: 'Package received intact at warehouse inbound dock' });
        if (res && res.success) {
            alert('Package marked as RECEIVED at warehouse.');
            await loadDashboardData();
            await loadReturnsTable();
        } else {
            alert('Receive failed: ' + (res.message || 'Unknown error'));
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

function openInspectModal(id) {
    activeReturnForInspection = allReturns.find(r => r.id == id);
    if (!activeReturnForInspection) return;

    document.getElementById('inspectReturnId').value = id;
    document.getElementById('inspectNotes').value = '';
    document.getElementById('inspectResult').value = 'ACCEPTED';
    document.getElementById('inspectCondition').value = 'UNOPENED';

    const tbody = document.getElementById('inspectItemsTableBody');
    tbody.innerHTML = (activeReturnForInspection.items || []).map(item => {
        const baseQty = item.receivedQuantity > 0 ? item.receivedQuantity : (item.approvedQuantity > 0 ? item.approvedQuantity : item.requestedQuantity);
        return `
            <tr>
                <td>
                    <strong>${item.fertilizerName || 'Item #' + item.id}</strong>
                    <div class="small text-muted">Original Batch: #${item.batchId || 'N/A'} | Bin: #${item.locationId || 'N/A'}</div>
                </td>
                <td class="text-center align-middle">
                    <span class="badge bg-secondary">${baseQty}</span>
                </td>
                <td class="text-center align-middle">
                    <input type="number" class="form-control form-control-sm text-center mx-auto inspect-acc-input" id="insp_acc_${item.id}" data-id="${item.id}" value="${baseQty}" min="0" max="${baseQty}" style="width: 80px;" onchange="onQtyChange(${item.id}, ${baseQty})">
                </td>
                <td class="text-center align-middle">
                    <input type="number" class="form-control form-control-sm text-center mx-auto inspect-rej-input" id="insp_rej_${item.id}" data-id="${item.id}" value="0" min="0" max="${baseQty}" style="width: 80px;">
                </td>
            </tr>
        `;
    }).join('');

    new bootstrap.Modal(document.getElementById('inspectModal')).show();
}

function onQtyChange(itemId, baseQty) {
    const acc = Number(document.getElementById(`insp_acc_${itemId}`).value) || 0;
    const rej = Math.max(0, baseQty - acc);
    document.getElementById(`insp_rej_${itemId}`).value = rej;
}

function onInspectionResultChange() {
    const res = document.getElementById('inspectResult').value;
    if (!activeReturnForInspection) return;

    (activeReturnForInspection.items || []).forEach(item => {
        const baseQty = item.receivedQuantity > 0 ? item.receivedQuantity : item.approvedQuantity;
        const accInput = document.getElementById(`insp_acc_${item.id}`);
        const rejInput = document.getElementById(`insp_rej_${item.id}`);
        if (!accInput || !rejInput) return;

        if (res === 'ACCEPTED') {
            accInput.value = baseQty;
            rejInput.value = 0;
        } else if (res === 'REJECTED' || res === 'QUARANTINED') {
            accInput.value = 0;
            rejInput.value = baseQty;
        }
    });
}

async function submitInspection(event) {
    event.preventDefault();
    const id = document.getElementById('inspectReturnId').value;
    const result = document.getElementById('inspectResult').value;
    const condition = document.getElementById('inspectCondition').value;
    const notes = document.getElementById('inspectNotes').value;

    const acceptedQuantities = {};
    const rejectedQuantities = {};

    document.querySelectorAll('.inspect-acc-input').forEach(input => {
        const itemId = Number(input.getAttribute('data-id'));
        acceptedQuantities[itemId] = Number(input.value) || 0;
    });

    document.querySelectorAll('.inspect-rej-input').forEach(input => {
        const itemId = Number(input.getAttribute('data-id'));
        rejectedQuantities[itemId] = Number(input.value) || 0;
    });

    const payload = {
        result: result,
        itemCondition: condition,
        inspectionNotes: notes,
        acceptedQuantities: acceptedQuantities,
        rejectedQuantities: rejectedQuantities
    };

    try {
        const res = await ApiClient.post(`/admin/returns/${id}/inspect`, payload);
        if (res && res.success) {
            bootstrap.Modal.getInstance(document.getElementById('inspectModal')).hide();
            alert('Inspection completed! Eligible stock has been automatically restored into its original fulfillment batch and bin.');
            await loadDashboardData();
            await loadReturnsTable();
            await loadRefundsTable();
        } else {
            alert('Inspection failed: ' + (res.message || 'Unknown error'));
        }
    } catch (e) {
        alert('Inspection error: ' + e.message);
    }
}

function openProcessRefundModal(refundId) {
    const refund = allRefunds.find(r => r.id == refundId);
    if (!refund) return;

    document.getElementById('processRefundId').value = refundId;
    document.getElementById('refundAmountInput').value = refund.amount;
    document.getElementById('refundPaymentMethod').value = refund.paymentMethod || 'Online';
    document.getElementById('refundBankDetailsInput').value = refund.bankAccountDetails || '';
    document.getElementById('refundPaymentRef').value = '';

    new bootstrap.Modal(document.getElementById('processRefundModal')).show();
}

async function submitProcessRefund(event) {
    event.preventDefault();
    const id = document.getElementById('processRefundId').value;
    const amt = Number(document.getElementById('refundAmountInput').value);
    const bankDetails = document.getElementById('refundBankDetailsInput').value;
    const refNum = document.getElementById('refundPaymentRef').value;

    const payload = {
        amount: amt,
        bankAccountDetails: bankDetails,
        paymentReference: refNum
    };

    try {
        const res = await ApiClient.post(`/admin/refunds/${id}/process`, payload);
        if (res && res.success) {
            bootstrap.Modal.getInstance(document.getElementById('processRefundModal')).hide();
            alert('Refund disbursed successfully!');
            await loadDashboardData();
            await loadReturnsTable();
            await loadRefundsTable();
        } else {
            alert('Refund execution failed: ' + (res.message || 'Unknown error'));
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}
