/**
 * SmartAgri - Batches, Expiry & FEFO Management Console (Phase 11)
 */

let allBatches = [];
let allProducts = [];
let createBatchModalInstance = null;
let adjustBatchModalInstance = null;
let damageBatchModalInstance = null;
let blockBatchModalInstance = null;
let historyBatchModalInstance = null;

document.addEventListener('DOMContentLoaded', async () => {
    initModals();
    initFilterListeners();
    await loadProducts();
    await loadBatchesSummary();
    await loadBatchesData();
});

function initModals() {
    if (typeof bootstrap !== 'undefined') {
        const cbEl = document.getElementById('createBatchModal');
        if (cbEl) createBatchModalInstance = new bootstrap.Modal(cbEl);

        const adjEl = document.getElementById('adjustBatchModal');
        if (adjEl) adjustBatchModalInstance = new bootstrap.Modal(adjEl);

        const dmgEl = document.getElementById('damageBatchModal');
        if (dmgEl) damageBatchModalInstance = new bootstrap.Modal(dmgEl);

        const blkEl = document.getElementById('blockBatchModal');
        if (blkEl) blockBatchModalInstance = new bootstrap.Modal(blkEl);

        const histEl = document.getElementById('batchHistoryModal');
        if (histEl) historyBatchModalInstance = new bootstrap.Modal(histEl);
    }
}

function initFilterListeners() {
    const searchInput = document.getElementById('batchSearchInput');
    if (searchInput) {
        let timer = null;
        searchInput.addEventListener('input', () => {
            clearTimeout(timer);
            timer = setTimeout(() => {
                loadBatchesData();
            }, 300);
        });
    }

    const prodSelect = document.getElementById('filterProductSelect');
    if (prodSelect) {
        prodSelect.addEventListener('change', () => loadBatchesData());
    }

    const statusSelect = document.getElementById('filterStatusSelect');
    if (statusSelect) {
        statusSelect.addEventListener('change', () => loadBatchesData());
    }

    const expSelect = document.getElementById('filterExpirySelect');
    if (expSelect) {
        expSelect.addEventListener('change', () => loadBatchesData());
    }

    const clearBtn = document.getElementById('clearFiltersBtn');
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            if (searchInput) searchInput.value = '';
            if (prodSelect) prodSelect.value = '';
            if (statusSelect) statusSelect.value = 'ALL';
            if (expSelect) expSelect.value = 'ALL';
            loadBatchesData();
        });
    }
}

async function loadProducts() {
    try {
        const res = await ApiClient.get('/fertilizers');
        if (res && res.success && Array.isArray(res.data)) {
            allProducts = res.data;
            populateProductDropdowns();
        }
    } catch (e) {
        console.error('Failed to load products for dropdowns:', e);
    }
}

function populateProductDropdowns() {
    const filterSelect = document.getElementById('filterProductSelect');
    const modalSelect = document.getElementById('cbFertilizerId');

    if (filterSelect) {
        filterSelect.innerHTML = '<option value="">All Products</option>';
        allProducts.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p.id;
            opt.textContent = `${p.name} (${p.sku || 'No SKU'})`;
            filterSelect.appendChild(opt);
        });
    }

    if (modalSelect) {
        modalSelect.innerHTML = '<option value="">Select Product...</option>';
        allProducts.forEach(p => {
            const opt = document.createElement('option');
            opt.value = p.id;
            opt.textContent = `${p.name} [SKU: ${p.sku || '-'}]`;
            modalSelect.appendChild(opt);
        });
    }
}

async function loadBatchesSummary() {
    try {
        const res = await ApiClient.get('/admin/inventory/batches/summary');
        if (res && res.success && res.data) {
            const d = res.data;
            document.getElementById('metricTotalBatches').textContent = d.totalBatches || 0;
            document.getElementById('metricActiveBatches').textContent = d.activeBatches || 0;
            document.getElementById('metricNearExpiry').textContent = d.nearExpiryBatches || 0;
            document.getElementById('metricNearExpiryUnits').textContent = `${d.nearExpiryUnits || 0} units`;
            document.getElementById('metricExpired').textContent = d.expiredBatches || 0;
            document.getElementById('metricExpiredUnits').textContent = `${d.expiredUnits || 0} units`;
            document.getElementById('metricDepleted').textContent = d.depletedBatches || 0;
            document.getElementById('metricBlocked').textContent = d.blockedBatches || 0;
        }
    } catch (e) {
        console.error('Failed to load batch summary:', e);
    }
}

async function loadBatchesData() {
    const tbody = document.getElementById('batchesTableBody');
    if (!tbody) return;

    tbody.innerHTML = `
        <tr>
            <td colspan="13" class="text-center py-4 text-muted">
                <div class="spinner-border spinner-border-sm me-2 text-success" role="status"></div>
                Loading batches...
            </td>
        </tr>
    `;

    try {
        const search = document.getElementById('batchSearchInput')?.value.trim() || '';
        const fertilizerId = document.getElementById('filterProductSelect')?.value || '';
        const status = document.getElementById('filterStatusSelect')?.value || 'ALL';
        const expiryFilter = document.getElementById('filterExpirySelect')?.value || 'ALL';

        let params = [];
        if (search) params.push(`search=${encodeURIComponent(search)}`);
        if (fertilizerId) params.push(`fertilizerId=${encodeURIComponent(fertilizerId)}`);
        if (status && status !== 'ALL') params.push(`status=${encodeURIComponent(status)}`);
        if (expiryFilter && expiryFilter !== 'ALL') params.push(`expiryFilter=${encodeURIComponent(expiryFilter)}`);

        const queryString = params.length > 0 ? `?${params.join('&')}` : '';
        const res = await ApiClient.get(`/admin/inventory/batches${queryString}`);

        if (res && res.success && Array.isArray(res.data)) {
            allBatches = res.data;
            renderBatchesTable(allBatches);
        } else {
            tbody.innerHTML = `<tr><td colspan="13" class="text-center py-4 text-muted">No batches found matching criteria.</td></tr>`;
        }
    } catch (e) {
        console.error('Failed to load batches:', e);
        tbody.innerHTML = `<tr><td colspan="13" class="text-center py-4 text-danger">Error loading batches: ${e.message || 'Server error'}</td></tr>`;
    }
}

function renderBatchesTable(batches) {
    const tbody = document.getElementById('batchesTableBody');
    if (!tbody) return;

    if (!batches || batches.length === 0) {
        tbody.innerHTML = `<tr><td colspan="13" class="text-center py-4 text-muted">No batch records available.</td></tr>`;
        return;
    }

    tbody.innerHTML = batches.map(b => {
        let statusBadge = '';
        const st = (b.status || '').toUpperCase();
        if (st === 'ACTIVE') {
            statusBadge = '<span class="badge bg-success">ACTIVE</span>';
        } else if (st === 'NEAR_EXPIRY') {
            statusBadge = '<span class="badge bg-warning text-dark"><i class="bi bi-clock me-1"></i>NEAR EXPIRY</span>';
        } else if (st === 'EXPIRED') {
            statusBadge = '<span class="badge bg-danger"><i class="bi bi-exclamation-octagon me-1"></i>EXPIRED</span>';
        } else if (st === 'DEPLETED') {
            statusBadge = '<span class="badge bg-secondary">DEPLETED</span>';
        } else if (st === 'BLOCKED') {
            statusBadge = '<span class="badge bg-dark"><i class="bi bi-shield-x me-1"></i>BLOCKED</span>';
        } else {
            statusBadge = `<span class="badge bg-light text-dark">${st}</span>`;
        }

        let expiryDisplay = b.expiryDate || '-';
        if (b.daysUntilExpiry !== null && b.daysUntilExpiry !== undefined) {
            if (b.daysUntilExpiry < 0) {
                expiryDisplay += `<br><span class="badge bg-danger-subtle text-danger small">Expired ${Math.abs(b.daysUntilExpiry)}d ago</span>`;
            } else if (b.daysUntilExpiry <= 30) {
                expiryDisplay += `<br><span class="badge bg-warning-subtle text-warning small">${b.daysUntilExpiry}d left</span>`;
            } else {
                expiryDisplay += `<br><small class="text-muted">${b.daysUntilExpiry}d left</small>`;
            }
        }

        const isBlocked = (st === 'BLOCKED');
        const isExpired = b.expired || (b.daysUntilExpiry !== null && b.daysUntilExpiry < 0);

        return `
            <tr>
                <td class="ps-3 fw-bold text-muted">#${b.id}</td>
                <td>
                    <div class="fw-semibold">${escapeHtml(b.fertilizerName || 'Unknown')}</div>
                    <small class="text-muted">${escapeHtml(b.brand || '')}</small>
                </td>
                <td><code>${escapeHtml(b.sku || '-')}</code></td>
                <td><strong class="text-dark">${escapeHtml(b.batchNumber)}</strong></td>
                <td><small class="text-muted">${b.manufacturingDate || '-'}</small></td>
                <td>${expiryDisplay}</td>
                <td>${b.initialQuantity}</td>
                <td><strong>${b.currentQuantity}</strong></td>
                <td>${b.reservedQuantity || 0}</td>
                <td><span class="badge ${b.availableQuantity > 0 ? 'bg-success-subtle text-success' : 'bg-danger-subtle text-danger'} fw-bold">${b.availableQuantity}</span></td>
                <td>${statusBadge}</td>
                <td><small>${escapeHtml(b.supplierName || '-')}</small></td>
                <td class="text-end pe-3">
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-secondary" title="View Audit Trail" onclick="openBatchHistory(${b.id}, '${escapeHtml(b.batchNumber)}', '${escapeHtml(b.fertilizerName || '')}', ${b.currentQuantity})">
                            <i class="bi bi-journal-text"></i>
                        </button>
                        <button class="btn btn-outline-primary" title="Adjust Stock" onclick="openAdjustModal(${b.id}, '${escapeHtml(b.batchNumber)}', '${escapeHtml(b.fertilizerName || '')}', ${b.currentQuantity})">
                            <i class="bi bi-sliders"></i>
                        </button>
                        <button class="btn btn-outline-danger" title="Mark Damaged" onclick="openDamageModal(${b.id}, '${escapeHtml(b.batchNumber)}', '${escapeHtml(b.fertilizerName || '')}', ${b.availableQuantity})">
                            <i class="bi bi-slash-circle"></i>
                        </button>
                        ${isBlocked ? `
                            <button class="btn btn-outline-success" title="Unblock Batch" onclick="submitUnblockBatch(${b.id}, '${escapeHtml(b.batchNumber)}')">
                                <i class="bi bi-shield-check"></i>
                            </button>
                        ` : `
                            <button class="btn btn-outline-warning" title="Block Batch" onclick="openBlockModal(${b.id}, '${escapeHtml(b.batchNumber)}', '${escapeHtml(b.fertilizerName || '')}')" ${isExpired ? 'disabled' : ''}>
                                <i class="bi bi-shield-x"></i>
                            </button>
                        `}
                    </div>
                </td>
            </tr>
        `;
    }).join('');
}

// ----------------------------------------------------------------------------
// Create Batch Intake
// ----------------------------------------------------------------------------

function openCreateBatchModal() {
    const form = document.getElementById('createBatchForm');
    if (form) form.reset();
    populateProductDropdowns();

    // Default dates
    const todayStr = new Date().toISOString().split('T')[0];
    const nextYear = new Date();
    nextYear.setFullYear(nextYear.getFullYear() + 1);
    const expStr = nextYear.toISOString().split('T')[0];

    const mfgEl = document.getElementById('cbManufacturingDate');
    const expEl = document.getElementById('cbExpiryDate');
    if (mfgEl) mfgEl.value = todayStr;
    if (expEl) expEl.value = expStr;

    if (createBatchModalInstance) {
        createBatchModalInstance.show();
    }
}

async function submitCreateBatch() {
    const fertId = document.getElementById('cbFertilizerId')?.value;
    const batchNum = document.getElementById('cbBatchNumber')?.value.trim();
    const qty = parseInt(document.getElementById('cbQuantity')?.value, 10);
    const expDate = document.getElementById('cbExpiryDate')?.value;
    const mfgDate = document.getElementById('cbManufacturingDate')?.value || null;
    const unitCost = document.getElementById('cbUnitCost')?.value || null;
    const supplier = document.getElementById('cbSupplierName')?.value.trim() || null;
    const poNum = document.getElementById('cbPurchaseOrderNumber')?.value.trim() || null;
    const suppRef = document.getElementById('cbSupplierReference')?.value.trim() || null;
    const notes = document.getElementById('cbNotes')?.value.trim() || null;

    if (!fertId) {
        alert('Please select a fertilizer product');
        return;
    }
    if (!batchNum) {
        alert('Batch number is required');
        return;
    }
    if (!qty || qty <= 0) {
        alert('Initial quantity must be greater than zero');
        return;
    }
    if (!expDate) {
        alert('Expiry date is required');
        return;
    }

    const payload = {
        fertilizerId: parseInt(fertId, 10),
        batchNumber: batchNum,
        quantity: qty,
        expiryDate: expDate,
        manufacturingDate: mfgDate,
        unitCost: unitCost ? parseFloat(unitCost) : null,
        supplierName: supplier,
        purchaseOrderNumber: poNum,
        supplierReference: suppRef,
        notes: notes
    };

    try {
        const res = await ApiClient.post('/admin/inventory/batches', payload);
        if (res && res.success) {
            alert('Batch intake created successfully!');
            if (createBatchModalInstance) createBatchModalInstance.hide();
            await loadBatchesSummary();
            await loadBatchesData();
        } else {
            alert('Failed: ' + (res?.message || 'Error creating batch'));
        }
    } catch (e) {
        alert('Error creating batch: ' + (e.message || 'Server error'));
    }
}

// ----------------------------------------------------------------------------
// Adjust Batch Modal
// ----------------------------------------------------------------------------

function openAdjustModal(id, batchNum, prodName, currentStock) {
    document.getElementById('adjBatchId').value = id;
    document.getElementById('adjBatchNumber').textContent = batchNum;
    document.getElementById('adjProductName').textContent = prodName;
    document.getElementById('adjCurrentStock').textContent = currentStock;
    document.getElementById('adjQuantity').value = '';
    document.getElementById('adjReason').value = '';

    if (adjustBatchModalInstance) adjustBatchModalInstance.show();
}

async function submitAdjustBatch() {
    const id = document.getElementById('adjBatchId').value;
    const type = document.getElementById('adjTypeSelect').value;
    const qty = parseInt(document.getElementById('adjQuantity').value, 10);
    const reason = document.getElementById('adjReason').value.trim();

    if (!reason) {
        alert('Audit reason is strictly mandatory for batch adjustments');
        return;
    }
    if (isNaN(qty) || qty < 0) {
        alert('Valid quantity is required');
        return;
    }

    try {
        const res = await ApiClient.post(`/admin/inventory/batches/${id}/adjust`, {
            adjustmentType: type,
            quantity: qty,
            reason: reason
        });

        if (res && res.success) {
            alert('Batch stock adjusted successfully!');
            if (adjustBatchModalInstance) adjustBatchModalInstance.hide();
            await loadBatchesSummary();
            await loadBatchesData();
        } else {
            alert('Failed: ' + (res?.message || 'Error adjusting batch'));
        }
    } catch (e) {
        alert('Error: ' + (e.message || 'Adjustment failed'));
    }
}

// ----------------------------------------------------------------------------
// Mark Damaged Modal
// ----------------------------------------------------------------------------

function openDamageModal(id, batchNum, prodName, availableStock) {
    document.getElementById('dmgBatchId').value = id;
    document.getElementById('dmgBatchNumber').textContent = batchNum;
    document.getElementById('dmgProductName').textContent = prodName;
    document.getElementById('dmgAvailableStock').textContent = availableStock;
    document.getElementById('dmgQuantity').value = '';
    document.getElementById('dmgReason').value = '';

    if (damageBatchModalInstance) damageBatchModalInstance.show();
}

async function submitMarkDamaged() {
    const id = document.getElementById('dmgBatchId').value;
    const qty = parseInt(document.getElementById('dmgQuantity').value, 10);
    const reason = document.getElementById('dmgReason').value.trim();

    if (!reason) {
        alert('Audit reason is strictly required for reporting damaged stock');
        return;
    }
    if (!qty || qty <= 0) {
        alert('Damaged quantity must be greater than zero');
        return;
    }

    try {
        const res = await ApiClient.post(`/admin/inventory/batches/${id}/damaged`, {
            quantity: qty,
            reason: reason
        });

        if (res && res.success) {
            alert('Damaged stock written off successfully!');
            if (damageBatchModalInstance) damageBatchModalInstance.hide();
            await loadBatchesSummary();
            await loadBatchesData();
        } else {
            alert('Failed: ' + (res?.message || 'Error writing off damaged stock'));
        }
    } catch (e) {
        alert('Error: ' + (e.message || 'Damage report failed'));
    }
}

// ----------------------------------------------------------------------------
// Block & Unblock Batch
// ----------------------------------------------------------------------------

function openBlockModal(id, batchNum, prodName) {
    document.getElementById('blockBatchId').value = id;
    document.getElementById('blockBatchNumber').textContent = batchNum;
    document.getElementById('blockProductName').textContent = prodName;
    document.getElementById('blockReason').value = '';

    if (blockBatchModalInstance) blockBatchModalInstance.show();
}

async function submitBlockBatch() {
    const id = document.getElementById('blockBatchId').value;
    const reason = document.getElementById('blockReason').value.trim();

    if (!reason) {
        alert('Audit reason is strictly required for blocking a batch');
        return;
    }

    try {
        const res = await ApiClient.post(`/admin/inventory/batches/${id}/block`, {
            reason: reason
        });

        if (res && res.success) {
            alert('Batch blocked from sale successfully!');
            if (blockBatchModalInstance) blockBatchModalInstance.hide();
            await loadBatchesSummary();
            await loadBatchesData();
        } else {
            alert('Failed: ' + (res?.message || 'Error blocking batch'));
        }
    } catch (e) {
        alert('Error: ' + (e.message || 'Block failed'));
    }
}

async function submitUnblockBatch(id, batchNum) {
    if (!confirm(`Are you sure you want to unblock Batch #${batchNum} and return it to active sale?`)) {
        return;
    }

    try {
        const res = await ApiClient.post(`/admin/inventory/batches/${id}/unblock`, {});
        if (res && res.success) {
            alert('Batch unblocked successfully!');
            await loadBatchesSummary();
            await loadBatchesData();
        } else {
            alert('Failed: ' + (res?.message || 'Error unblocking batch'));
        }
    } catch (e) {
        alert('Error: ' + (e.message || 'Unblock failed'));
    }
}

// ----------------------------------------------------------------------------
// Batch History / Audit Trail
// ----------------------------------------------------------------------------

async function openBatchHistory(id, batchNum, prodName, currentStock) {
    document.getElementById('histBatchNumber').textContent = batchNum;
    document.getElementById('histProductName').textContent = prodName;
    document.getElementById('histCurrentStock').textContent = currentStock;

    const tbody = document.getElementById('batchHistoryTableBody');
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted py-3">Loading history...</td></tr>`;

    if (historyBatchModalInstance) historyBatchModalInstance.show();

    try {
        const res = await ApiClient.get(`/admin/inventory/batches/${id}/history`);
        if (res && res.success && Array.isArray(res.data) && res.data.length > 0) {
            tbody.innerHTML = res.data.map(tx => {
                let badgeClass = 'bg-secondary';
                if (tx.transactionType === 'RESTOCK' || tx.transactionType === 'ADJUSTMENT_IN' || tx.transactionType === 'CANCELLATION') {
                    badgeClass = 'bg-success';
                } else if (tx.transactionType === 'SALE' || tx.transactionType === 'ADJUSTMENT_OUT' || tx.transactionType === 'DAMAGED') {
                    badgeClass = 'bg-danger';
                }

                return `
                    <tr>
                        <td><small>${tx.createdAt ? tx.createdAt.replace('T', ' ').substring(0, 19) : '-'}</small></td>
                        <td><span class="badge ${badgeClass}">${tx.transactionType || ''}</span></td>
                        <td><strong>${tx.quantity}</strong></td>
                        <td>${tx.stockBefore} &rarr; ${tx.stockAfter}</td>
                        <td><code>${escapeHtml(tx.referenceId || '-')}</code></td>
                        <td><small>${escapeHtml(tx.reason || '-')}</small></td>
                        <td><small class="text-muted">${escapeHtml(tx.performedBy || 'system')}</small></td>
                    </tr>
                `;
            }).join('');
        } else {
            tbody.innerHTML = `<tr><td colspan="7" class="text-center text-muted py-3">No movements recorded for this batch.</td></tr>`;
        }
    } catch (e) {
        console.error('Failed to load batch history:', e);
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-3">Error: ${e.message}</td></tr>`;
    }
}

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
