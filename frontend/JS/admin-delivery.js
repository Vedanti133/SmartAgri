/**
 * SmartAgri - Admin Delivery & Logistics Management
 */

let allDeliveries = [];
let currentFilter = 'ALL';
let currentSearch = '';
let assignModalInstance = null;
let updateStatusModalInstance = null;

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Check admin authorization
    if (typeof requireLogin === 'function') {
        requireLogin();
    }
    const user = typeof getUser === 'function' ? getUser() : null;
    if (!user || user.role !== 'ADMIN') {
        alert('Access denied: Admin credentials required.');
        window.location.href = 'customer-catalog.html';
        return;
    }

    // 2. Initialize bootstrap modals
    const assignEl = document.getElementById('assignPartnerModal');
    if (assignEl) assignModalInstance = new bootstrap.Modal(assignEl);

    const statusEl = document.getElementById('updateStatusModal');
    if (statusEl) updateStatusModalInstance = new bootstrap.Modal(statusEl);

    // 3. Setup sidebar & listeners
    setupSidebar();
    setupFilters();
    setupForms();

    // 4. Load data
    await loadMetrics();
    await loadDeliveries();
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

function setupFilters() {
    const searchInput = document.getElementById('deliverySearchInput');
    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            currentSearch = e.target.value.toLowerCase().trim();
            renderDeliveries();
        });
    }

    const tabs = document.querySelectorAll('#deliveryFilterTabs .nav-link');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            currentFilter = tab.dataset.status;
            renderDeliveries();
        });
    });
}

function setupForms() {
    const assignForm = document.getElementById('assignPartnerForm');
    if (assignForm) {
        assignForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await submitAssignment();
        });
    }

    const statusForm = document.getElementById('updateStatusForm');
    if (statusForm) {
        statusForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await submitStatusUpdate();
        });
    }
}

async function loadMetrics() {
    try {
        const res = await ApiClient.get('/admin/orders/fulfillment-metrics');
        if (res && res.success && res.data) {
            const m = res.data;
            document.getElementById('kpiTotalShipments').textContent = m.totalDeliveries || 0;
            document.getElementById('kpiUnassigned').textContent = m.notAssignedDeliveries || 0;
            const inTransit = (m.assignedDeliveries || 0) + (m.packedDeliveries || 0) + (m.shippedDeliveries || 0) + (m.outForDeliveryDeliveries || 0);
            document.getElementById('kpiInTransit').textContent = inTransit;
            document.getElementById('kpiDeliveredToday').textContent = m.deliveredToday || 0;
        }
    } catch (e) {
        console.warn('Failed to load fulfillment metrics:', e);
    }
}

async function loadDeliveries() {
    const tbody = document.getElementById('deliveriesTableBody');
    try {
        const res = await ApiClient.get('/admin/deliveries');
        if (res && res.success && Array.isArray(res.data)) {
            allDeliveries = res.data;
            renderDeliveries();
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-muted">No consignments found.</td></tr>';
        }
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-4 text-danger">Failed to load deliveries: ${e.message}</td></tr>`;
    }
}

function renderDeliveries() {
    const tbody = document.getElementById('deliveriesTableBody');
    if (!tbody) return;

    let filtered = allDeliveries.filter(d => {
        // Filter by tab status
        if (currentFilter === 'EXCEPTIONS') {
            if (d.deliveryStatus !== 'FAILED' && d.deliveryStatus !== 'RETURNED') return false;
        } else if (currentFilter !== 'ALL') {
            if (d.deliveryStatus !== currentFilter) return false;
        }

        // Filter by search query
        if (currentSearch) {
            const trk = (d.trackingNumber || '').toLowerCase();
            const ord = (d.orderNumber || '').toLowerCase();
            const partner = (d.deliveryPartner || '').toLowerCase();
            const recipient = (d.recipientName || '').toLowerCase();
            const city = (d.city || '').toLowerCase();

            if (!trk.includes(currentSearch) &&
                !ord.includes(currentSearch) &&
                !partner.includes(currentSearch) &&
                !recipient.includes(currentSearch) &&
                !city.includes(currentSearch)) {
                return false;
            }
        }
        return true;
    });

    if (filtered.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center py-5 text-muted">No matching deliveries found.</td></tr>';
        return;
    }

    tbody.innerHTML = filtered.map(d => {
        const statusKey = (d.deliveryStatus || 'not_assigned').toLowerCase();
        const statusClass = `status-badge-${statusKey}`;
        const estDate = d.estimatedDeliveryDate ? new Date(d.estimatedDeliveryDate).toLocaleDateString('en-IN') : 'Pending';

        return `
            <tr>
                <td class="ps-3">
                    ${d.trackingNumber ? `
                        <div class="fw-bold text-dark font-monospace small">${d.trackingNumber}</div>
                    ` : `
                        <span class="badge bg-light text-muted border">Not Generated</span>
                    `}
                </td>
                <td>
                    <a href="admin-orders.html" class="fw-semibold text-success small text-decoration-none" title="View Order">
                        ${d.orderNumber}
                    </a>
                </td>
                <td>
                    <div class="small fw-semibold text-dark">${d.recipientName || 'Customer'}</div>
                    <div class="small text-muted">${d.city || '-'}, ${d.state || ''} (${d.pincode || ''})</div>
                </td>
                <td>
                    ${d.deliveryPartner ? `
                        <div class="small fw-semibold">${d.deliveryPartner}</div>
                        <div class="small text-muted"><i class="bi bi-telephone me-1"></i>+91 ${d.deliveryPartnerPhone || 'N/A'}</div>
                    ` : `
                        <span class="badge bg-warning-subtle text-warning">Unassigned</span>
                    `}
                </td>
                <td class="small text-muted">${estDate}</td>
                <td>
                    <span class="badge ${statusClass} px-2 py-1 rounded-pill small fw-bold">
                        ${(d.deliveryStatus || 'NOT_ASSIGNED').replace(/_/g, ' ')}
                    </span>
                </td>
                <td class="text-end pe-3">
                    <div class="d-inline-flex gap-1 align-items-center">
                        ${d.deliveryStatus === 'NOT_ASSIGNED' ? `
                            <button class="btn btn-sm btn-primary py-1 px-2 fw-semibold" onclick="openAssignModal(${d.id})" title="Assign Logistics Partner">
                                <i class="bi bi-person-plus me-1"></i> Assign
                            </button>
                        ` : ''}

                        ${canUpdateDelivery(d.deliveryStatus) ? `
                            <button class="btn btn-sm btn-outline-success py-1 px-2" onclick="openStatusModal(${d.id}, '${d.deliveryStatus}')" title="Update Delivery Status">
                                <i class="bi bi-arrow-repeat me-1"></i> Status
                            </button>
                        ` : ''}

                        <a href="customer-order-tracking.html?orderId=${d.orderId}" target="_blank" class="btn btn-sm btn-light border py-1 px-2" title="View Tracking Timeline">
                            <i class="bi bi-geo-alt"></i>
                        </a>
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

function canUpdateDelivery(status) {
    return status !== 'RETURNED';
}

function openAssignModal(deliveryId) {
    const delivery = allDeliveries.find(d => d.id === deliveryId);
    if (!delivery) return;

    document.getElementById('assignDeliveryId').value = deliveryId;
    document.getElementById('partnerNameInput').value = delivery.deliveryPartner || '';
    document.getElementById('partnerPhoneInput').value = delivery.deliveryPartnerPhone || '';
    document.getElementById('partnerEstDateInput').value = delivery.estimatedDeliveryDate || '';
    document.getElementById('partnerNotesInput').value = delivery.deliveryNotes || '';

    // Set min date to today
    const today = new Date().toISOString().split('T')[0];
    document.getElementById('partnerEstDateInput').min = today;

    if (assignModalInstance) assignModalInstance.show();
}

async function submitAssignment() {
    const deliveryId = document.getElementById('assignDeliveryId').value;
    const partner = document.getElementById('partnerNameInput').value.trim();
    const phone = document.getElementById('partnerPhoneInput').value.trim();
    const estDate = document.getElementById('partnerEstDateInput').value;
    const notes = document.getElementById('partnerNotesInput').value.trim();

    if (!partner) {
        showToast('Please specify delivery partner name.', 'danger');
        return;
    }
    if (!/^[0-9]{10}$/.test(phone)) {
        showToast('Phone must be a valid 10-digit number.', 'danger');
        return;
    }

    try {
        const payload = {
            deliveryPartner: partner,
            deliveryPartnerPhone: phone,
            estimatedDeliveryDate: estDate || null,
            notes: notes || null
        };

        const res = await ApiClient.put(`/admin/deliveries/${deliveryId}/assign`, payload);
        if (res && res.success) {
            showToast('Delivery partner assigned successfully!', 'success');
            if (assignModalInstance) assignModalInstance.hide();
            await loadDeliveries();
            await loadMetrics();
        } else {
            showToast(res.message || 'Assignment failed.', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Error assigning delivery partner.', 'danger');
    }
}

function getAllowedTransitions(current) {
    switch (current) {
        case 'NOT_ASSIGNED':
            return ['ASSIGNED'];
        case 'ASSIGNED':
            return ['PACKED', 'SHIPPED', 'FAILED'];
        case 'PACKED':
            return ['SHIPPED', 'FAILED'];
        case 'SHIPPED':
            return ['OUT_FOR_DELIVERY', 'DELIVERED', 'FAILED'];
        case 'OUT_FOR_DELIVERY':
            return ['DELIVERED', 'FAILED'];
        case 'FAILED':
            return ['OUT_FOR_DELIVERY', 'RETURNED', 'ASSIGNED'];
        case 'DELIVERED':
            return ['RETURNED'];
        default:
            return [];
    }
}

function openStatusModal(deliveryId, currentStatus) {
    document.getElementById('updateDeliveryId').value = deliveryId;
    document.getElementById('currentStatusBadge').textContent = currentStatus.replace(/_/g, ' ');
    document.getElementById('statusNotesInput').value = '';

    const allowed = getAllowedTransitions(currentStatus);
    const select = document.getElementById('newDeliveryStatusSelect');
    select.innerHTML = allowed.map(st => `<option value="${st}">${st.replace(/_/g, ' ')}</option>`).join('');

    if (allowed.length === 0) {
        showToast(`No further status updates allowed for status ${currentStatus}.`, 'warning');
        return;
    }

    if (updateStatusModalInstance) updateStatusModalInstance.show();
}

async function submitStatusUpdate() {
    const deliveryId = document.getElementById('updateDeliveryId').value;
    const newStatus = document.getElementById('newDeliveryStatusSelect').value;
    const notes = document.getElementById('statusNotesInput').value.trim();

    try {
        const payload = {
            deliveryStatus: newStatus,
            notes: notes || null
        };

        const res = await ApiClient.put(`/admin/deliveries/${deliveryId}/status`, payload);
        if (res && res.success) {
            showToast(`Delivery status updated to ${newStatus.replace(/_/g, ' ')}!`, 'success');
            if (updateStatusModalInstance) updateStatusModalInstance.hide();
            await loadDeliveries();
            await loadMetrics();
        } else {
            showToast(res.message || 'Failed to update delivery status.', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Error updating delivery status.', 'danger');
    }
}

function showToast(message, type = 'success') {
    const toastEl = document.getElementById('liveToast');
    const toastMsg = document.getElementById('toastMessage');
    if (toastEl && toastMsg) {
        toastEl.className = `toast align-items-center text-white bg-${type === 'danger' ? 'danger' : type === 'warning' ? 'warning text-dark' : 'success'} border-0 shadow`;
        toastMsg.textContent = message;
        const toast = new bootstrap.Toast(toastEl, { delay: 3500 });
        toast.show();
    }
}
