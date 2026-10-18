/**
 * SmartAgri - Phase 9 Admin Payment Management & Gateway Ledger
 */

let allPayments = [];
let filteredPayments = [];
let currentStatus = 'ALL';
let currentMethod = 'ALL';
let currentSearch = '';

document.addEventListener('DOMContentLoaded', async () => {
    // Ensure admin authentication
    if (typeof requireAdmin === 'function') {
        requireAdmin();
    } else if (typeof requireLogin === 'function') {
        requireLogin();
    }

    setupUpdatePaymentForm();
    await loadPayments();
});

/**
 * Loads all payments from admin endpoint
 */
async function loadPayments() {
    const tbody = document.getElementById('paymentsTableBody');
    const noState = document.getElementById('noPaymentsState');

    tbody.innerHTML = `
        <tr>
            <td colspan="9" class="text-center py-5">
                <div class="spinner-border spinner-border-sm text-success" role="status"></div>
                <span class="ms-2 text-muted">Loading payments ledger...</span>
            </td>
        </tr>
    `;
    if (noState) noState.style.display = 'none';

    try {
        const queryParams = new URLSearchParams();
        if (currentStatus && currentStatus !== 'ALL') {
            queryParams.append('status', currentStatus);
        }
        if (currentMethod && currentMethod !== 'ALL') {
            queryParams.append('method', currentMethod);
        }
        if (currentSearch) {
            queryParams.append('search', currentSearch);
        }

        const queryString = queryParams.toString() ? `?${queryParams.toString()}` : '';
        const res = await ApiClient.get(`/admin/payments${queryString}`);

        if (res && res.success && Array.isArray(res.data)) {
            allPayments = res.data;
            applyFiltersAndRender();
            computeMetrics(allPayments);
        } else {
            tbody.innerHTML = `<tr><td colspan="9" class="text-center text-danger py-4">${res.message || 'Failed to load payments.'}</td></tr>`;
        }
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="9" class="text-center text-danger py-4">${err.message || 'Error connecting to server.'}</td></tr>`;
    }
}

/**
 * Computes dashboard metric cards based on ledger data
 */
function computeMetrics(payments) {
    let totalRevenue = 0;
    let successCount = 0;
    let pendingCount = 0;
    let failedCount = 0;

    payments.forEach(p => {
        const status = (p.paymentStatus || '').toUpperCase();
        const amt = Number(p.amount) || 0;

        if (status === 'PAID' || status === 'SUCCESS') {
            totalRevenue += amt;
            successCount++;
        } else if (status === 'PENDING' || status === 'INITIATED') {
            pendingCount++;
        } else if (status === 'FAILED' || status === 'CANCELLED') {
            failedCount++;
        }
    });

    const revEl = document.getElementById('metricTotalRevenue');
    const succEl = document.getElementById('metricSuccessCount');
    const pendEl = document.getElementById('metricPendingCount');
    const failEl = document.getElementById('metricFailedCount');

    if (revEl) revEl.textContent = `₹${totalRevenue.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    if (succEl) succEl.textContent = successCount;
    if (pendEl) pendEl.textContent = pendingCount;
    if (failEl) failEl.textContent = failedCount;
}

/**
 * Filters the payments list and renders into table
 */
function applyFiltersAndRender() {
    filteredPayments = allPayments.filter(p => {
        const status = (p.paymentStatus || '').toUpperCase();
        const method = (p.paymentMethod || '').toUpperCase();

        if (currentStatus !== 'ALL') {
            if (currentStatus === 'PAID' && status !== 'PAID' && status !== 'SUCCESS') return false;
            if (currentStatus !== 'PAID' && status !== currentStatus) return false;
        }

        if (currentMethod !== 'ALL' && method !== currentMethod) {
            return false;
        }

        if (currentSearch) {
            const q = currentSearch.toLowerCase();
            const orderNum = (p.orderNumber || '').toLowerCase();
            const txnId = (p.transactionId || '').toLowerCase();
            const gwOrderId = (p.gatewayOrderId || '').toLowerCase();
            const custName = (p.customerName || '').toLowerCase();
            const custUser = (p.customerUsername || '').toLowerCase();

            if (!orderNum.includes(q) && !txnId.includes(q) && !gwOrderId.includes(q) && !custName.includes(q) && !custUser.includes(q)) {
                return false;
            }
        }

        return true;
    });

    renderPaymentsTable(filteredPayments);
}

/**
 * Renders filtered payments into table
 */
function renderPaymentsTable(payments) {
    const tbody = document.getElementById('paymentsTableBody');
    const noState = document.getElementById('noPaymentsState');

    if (!payments || payments.length === 0) {
        tbody.innerHTML = '';
        if (noState) noState.style.display = 'block';
        return;
    }

    if (noState) noState.style.display = 'none';

    tbody.innerHTML = payments.map(p => {
        const statusClass = getStatusBadgeClass(p.paymentStatus);
        const formattedAmount = `₹${(Number(p.amount) || 0).toFixed(2)}`;
        const dateStr = p.createdAt ? new Date(p.createdAt).toLocaleString('en-IN', { dateStyle: 'medium', timeStyle: 'short' }) : '-';
        const isOnline = (p.paymentMethod || '').toUpperCase() === 'ONLINE';

        return `
            <tr>
                <td class="ps-3">
                    <span class="font-monospace fw-bold small text-dark">${p.transactionId || 'None'}</span>
                    ${p.gatewayOrderId ? `<br><small class="text-muted font-monospace">${p.gatewayOrderId}</small>` : ''}
                </td>
                <td>
                    <span class="fw-bold text-success font-monospace">#${p.orderNumber || p.orderId}</span>
                </td>
                <td>
                    <div class="fw-semibold text-dark">${p.customerName || p.customerUsername || 'Customer'}</div>
                    <small class="text-muted">@${p.customerUsername || 'user'}</small>
                </td>
                <td>
                    <strong class="text-dark">${formattedAmount}</strong>
                </td>
                <td>
                    <span class="badge ${isOnline ? 'bg-primary-subtle text-primary' : 'bg-secondary-subtle text-secondary'} fw-bold">
                        <i class="bi ${isOnline ? 'bi-credit-card-2-front' : 'bi-cash-coin'} me-1"></i>
                        ${p.paymentMethod || 'COD'}
                    </span>
                </td>
                <td>
                    <span class="badge ${statusClass} fw-bold">
                        ${p.paymentStatus || 'PENDING'}
                    </span>
                    ${p.failureReason ? `<br><small class="text-danger small" title="${p.failureReason}"><i class="bi bi-info-circle me-1"></i>Declined</small>` : ''}
                </td>
                <td>
                    <span class="small text-muted">${p.gatewayName || 'SYSTEM'}</span>
                </td>
                <td>
                    <span class="small text-secondary">${dateStr}</span>
                </td>
                <td class="text-end pe-3">
                    <div class="d-flex justify-content-end gap-1">
                        <button class="btn btn-sm btn-outline-info" onclick="openPaymentDetails(${p.id})" title="View Audit Details">
                            <i class="bi bi-eye"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-success" onclick="openUpdatePaymentStatus(${p.orderId}, '${p.orderNumber}', '${p.amount}', '${p.paymentStatus}')" title="Update Status">
                            <i class="bi bi-pencil-square"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function getStatusBadgeClass(status) {
    if (!status) return 'bg-secondary';
    switch (status.toUpperCase()) {
        case 'PAID':
        case 'SUCCESS':
            return 'status-badge-paid';
        case 'PENDING':
        case 'INITIATED':
            return 'status-badge-pending';
        case 'FAILED':
        case 'CANCELLED':
            return 'status-badge-failed';
        case 'REFUNDED':
        case 'PARTIALLY_REFUNDED':
            return 'status-badge-refunded';
        default:
            return 'bg-secondary';
    }
}

/**
 * Filter handlers
 */
function filterByStatus(status) {
    currentStatus = status;
    document.querySelectorAll('.filter-chip').forEach(btn => {
        btn.classList.toggle('active', btn.getAttribute('data-status') === status);
    });
    applyFiltersAndRender();
}

function filterByMethod(method) {
    currentMethod = method;
    applyFiltersAndRender();
}

let searchTimeout = null;
function onSearchInput(val) {
    clearTimeout(searchTimeout);
    searchTimeout = setTimeout(() => {
        currentSearch = val.trim();
        applyFiltersAndRender();
    }, 250);
}

/**
 * View technical payment audit details modal
 */
function openPaymentDetails(paymentId) {
    const payment = allPayments.find(p => p.id === paymentId);
    if (!payment) return;

    const modalBody = document.getElementById('paymentDetailsBody');
    const formattedAmount = `₹${(Number(payment.amount) || 0).toFixed(2)}`;
    const createdStr = payment.createdAt ? new Date(payment.createdAt).toLocaleString('en-IN') : '-';
    const updatedStr = payment.updatedAt ? new Date(payment.updatedAt).toLocaleString('en-IN') : '-';
    const paidStr = payment.paidAt ? new Date(payment.paidAt).toLocaleString('en-IN') : 'Not settled yet';

    modalBody.innerHTML = `
        <div class="row g-3">
            <div class="col-md-6">
                <div class="p-3 bg-light rounded-3">
                    <h6 class="fw-bold text-success mb-3"><i class="bi bi-file-earmark-text me-1"></i> Transaction Core</h6>
                    <div class="mb-2"><span class="text-muted small">Payment ID:</span> <strong>#${payment.id}</strong></div>
                    <div class="mb-2"><span class="text-muted small">Order Number:</span> <strong class="text-success font-monospace">#${payment.orderNumber}</strong></div>
                    <div class="mb-2"><span class="text-muted small">Payable Amount:</span> <strong class="fs-5 text-dark">${formattedAmount} ${payment.currency || 'INR'}</strong></div>
                    <div class="mb-2"><span class="text-muted small">Payment Method:</span> <span class="badge bg-secondary">${payment.paymentMethod}</span></div>
                    <div class="mb-0"><span class="text-muted small">Payment Status:</span> <span class="badge ${getStatusBadgeClass(payment.paymentStatus)}">${payment.paymentStatus}</span></div>
                </div>
            </div>

            <div class="col-md-6">
                <div class="p-3 bg-light rounded-3">
                    <h6 class="fw-bold text-primary mb-3"><i class="bi bi-shield-lock me-1"></i> Gateway Audit Data</h6>
                    <div class="mb-2"><span class="text-muted small">Gateway Name:</span> <strong>${payment.gatewayName || 'SYSTEM'}</strong></div>
                    <div class="mb-2"><span class="text-muted small">Internal Txn ID:</span> <div class="font-monospace small text-dark">${payment.transactionId || 'None'}</div></div>
                    <div class="mb-2"><span class="text-muted small">Gateway Order ID:</span> <div class="font-monospace small text-dark">${payment.gatewayOrderId || 'N/A'}</div></div>
                    <div class="mb-0"><span class="text-muted small">Gateway Payment ID:</span> <div class="font-monospace small text-dark">${payment.gatewayPaymentId || 'N/A'}</div></div>
                </div>
            </div>

            <div class="col-12">
                <div class="p-3 bg-light rounded-3">
                    <h6 class="fw-bold text-dark mb-2"><i class="bi bi-person me-1"></i> Customer & Timestamps</h6>
                    <div class="row g-2 small">
                        <div class="col-sm-6"><span class="text-muted">Customer Name:</span> <strong>${payment.customerName || 'N/A'}</strong></div>
                        <div class="col-sm-6"><span class="text-muted">Username:</span> <strong>@${payment.customerUsername || 'N/A'}</strong></div>
                        <div class="col-sm-4"><span class="text-muted">Initiated At:</span> <div>${createdStr}</div></div>
                        <div class="col-sm-4"><span class="text-muted">Last Updated:</span> <div>${updatedStr}</div></div>
                        <div class="col-sm-4"><span class="text-muted">Settled (Paid At):</span> <div class="text-success fw-bold">${paidStr}</div></div>
                    </div>
                </div>
            </div>

            ${payment.failureReason ? `
            <div class="col-12">
                <div class="alert alert-danger mb-0 small">
                    <i class="bi bi-exclamation-triangle-fill me-1"></i>
                    <strong>Failure / Decline Diagnostic:</strong> ${payment.failureReason}
                </div>
            </div>` : ''}

            ${payment.gatewayResponse ? `
            <div class="col-12">
                <label class="form-label small fw-bold text-muted">Raw Gateway Response JSON:</label>
                <pre class="bg-dark text-white p-2 rounded small mb-0" style="max-height: 140px; overflow-y: auto;"><code>${payment.gatewayResponse}</code></pre>
            </div>` : ''}
        </div>
    `;

    const modal = new bootstrap.Modal(document.getElementById('paymentDetailsModal'));
    modal.show();
}

/**
 * Open status update modal
 */
function openUpdatePaymentStatus(orderId, orderNumber, amount, currentStatus) {
    document.getElementById('editPaymentOrderId').value = orderId;
    document.getElementById('editPaymentOrderNum').value = `#${orderNumber}`;
    document.getElementById('editPaymentAmount').value = `₹${(Number(amount) || 0).toFixed(2)}`;
    document.getElementById('editPaymentStatusSelect').value = currentStatus || 'PAID';
    document.getElementById('editPaymentNotes').value = '';
    document.getElementById('editPaymentMsg').innerHTML = '';

    const modal = new bootstrap.Modal(document.getElementById('updatePaymentModal'));
    modal.show();
}

/**
 * Setup submit handler on status update modal form
 */
function setupUpdatePaymentForm() {
    const form = document.getElementById('updatePaymentForm');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const orderId = document.getElementById('editPaymentOrderId').value;
        const newStatus = document.getElementById('editPaymentStatusSelect').value;
        const notes = document.getElementById('editPaymentNotes').value.trim();
        const btn = document.getElementById('btnSaveStatus');
        const msg = document.getElementById('editPaymentMsg');

        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Saving...';
        msg.innerHTML = '';

        try {
            const res = await ApiClient.put(`/admin/payments/${orderId}/status`, {
                paymentStatus: newStatus,
                failureReason: notes || null
            });

            if (res && res.success) {
                showToast('Payment status updated successfully', 'success');
                const modalInstance = bootstrap.Modal.getInstance(document.getElementById('updatePaymentModal'));
                if (modalInstance) modalInstance.hide();
                await loadPayments();
            } else {
                msg.innerHTML = `<div class="alert alert-danger py-2 small mb-0">${res.message || 'Failed to update payment status.'}</div>`;
            }
        } catch (err) {
            msg.innerHTML = `<div class="alert alert-danger py-2 small mb-0">${err.message || 'Error occurred.'}</div>`;
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-check2-circle me-1"></i> Save Changes';
        }
    });
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
