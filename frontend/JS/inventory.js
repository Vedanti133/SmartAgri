/**
 * SmartAgri - Inventory Management Console (Phase 10)
 * Real-time stock levels, transactions, adjustments, low-stock alerts & health checks
 */

// State
let inventoryItems = [];
let allTransactions = [];
let currentPage = 0;
let pageSize = 10;
let totalPages = 1;
let totalElements = 0;

let searchQuery = '';
let selectedCategory = 'all';
let selectedStatus = 'all';
let searchDebounceTimer = null;

let stockInModalInstance = null;
let stockAdjustModalInstance = null;
let transactionsModalInstance = null;
let healthModalInstance = null;

document.addEventListener('DOMContentLoaded', async () => {
    // Session & Role check
    if (typeof requireAdmin === 'function') {
        requireAdmin();
    } else if (typeof requireLogin === 'function') {
        requireLogin();
    }

    initModals();
    initFiltersAndSearch();
    await loadCategories();
    await loadInventorySummary();
    await loadInventoryData();
});

function initModals() {
    if (typeof bootstrap !== 'undefined') {
        const inEl = document.getElementById('stockInModal');
        if (inEl) stockInModalInstance = new bootstrap.Modal(inEl);

        const adjEl = document.getElementById('stockAdjustModal');
        if (adjEl) stockAdjustModalInstance = new bootstrap.Modal(adjEl);

        const txEl = document.getElementById('transactionHistoryModal');
        if (txEl) transactionsModalInstance = new bootstrap.Modal(txEl);

        const hEl = document.getElementById('inventoryHealthModal');
        if (hEl) healthModalInstance = new bootstrap.Modal(hEl);
    }
}

function initFiltersAndSearch() {
    const sInput = document.getElementById('inventorySearch');
    const sClear = document.getElementById('inventoryClearSearch');
    const topSearch = document.getElementById('inventoryTopSearchInput');
    const topClear = document.getElementById('inventoryTopSearchClear');

    if (sInput) {
        sInput.addEventListener('input', (e) => {
            searchQuery = e.target.value.trim();
            if (sClear) sClear.style.display = searchQuery ? 'inline-flex' : 'none';
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(() => {
                currentPage = 0;
                loadInventoryData();
            }, 350);
        });
    }

    if (topSearch) {
        topSearch.addEventListener('input', (e) => {
            searchQuery = e.target.value.trim();
            if (sInput) sInput.value = searchQuery;
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(() => {
                currentPage = 0;
                loadInventoryData();
            }, 350);
        });
    }

    if (sClear) {
        sClear.addEventListener('click', () => {
            if (sInput) sInput.value = '';
            searchQuery = '';
            sClear.style.display = 'none';
            currentPage = 0;
            loadInventoryData();
        });
    }

    if (topClear) {
        topClear.addEventListener('click', () => {
            if (topSearch) topSearch.value = '';
            if (sInput) sInput.value = '';
            searchQuery = '';
            currentPage = 0;
            loadInventoryData();
        });
    }

    const catFilter = document.getElementById('inventoryCategoryFilter');
    if (catFilter) {
        catFilter.addEventListener('change', (e) => {
            selectedCategory = e.target.value;
            currentPage = 0;
            loadInventoryData();
        });
    }

    const statusFilter = document.getElementById('inventoryStatusFilter');
    if (statusFilter) {
        statusFilter.addEventListener('change', (e) => {
            selectedStatus = e.target.value;
            currentPage = 0;
            loadInventoryData();
        });
    }

    const resetBtn = document.getElementById('inventoryReset');
    if (resetBtn) {
        resetBtn.addEventListener('click', () => {
            resetInventoryFilters();
        });
    }
}

function resetInventoryFilters() {
    searchQuery = '';
    selectedCategory = 'all';
    selectedStatus = 'all';

    const sInput = document.getElementById('inventorySearch');
    if (sInput) sInput.value = '';
    const topSearch = document.getElementById('inventoryTopSearchInput');
    if (topSearch) topSearch.value = '';
    const sClear = document.getElementById('inventoryClearSearch');
    if (sClear) sClear.style.display = 'none';

    const catFilter = document.getElementById('inventoryCategoryFilter');
    if (catFilter) catFilter.value = 'all';

    const statusFilter = document.getElementById('inventoryStatusFilter');
    if (statusFilter) statusFilter.value = 'all';

    currentPage = 0;
    loadInventoryData();
}

async function loadCategories() {
    try {
        const res = await ApiClient.get('/categories');
        const sel = document.getElementById('inventoryCategoryFilter');
        if (sel && res && res.success && Array.isArray(res.data)) {
            sel.innerHTML = '<option value="all">All Categories</option>';
            res.data.forEach(c => {
                sel.innerHTML += `<option value="${c.id}">${c.name}</option>`;
            });
        }
    } catch (e) {
        console.warn('Could not load categories:', e);
    }
}

/* =========================================================
   SUMMARY & METRICS
   ========================================================= */

async function loadInventorySummary() {
    try {
        const res = await ApiClient.get('/admin/inventory/summary');
        if (res && res.success && res.data) {
            const d = res.data;
            const uEl = document.getElementById('totalUnits');
            if (uEl) uEl.textContent = Number(d.totalStockUnits || 0).toLocaleString();

            const lEl = document.getElementById('lowStockCount');
            if (lEl) lEl.textContent = Number(d.lowStockCount || 0).toLocaleString();

            const oEl = document.getElementById('outOfStockCount');
            if (oEl) oEl.textContent = Number(d.outOfStockCount || 0).toLocaleString();

            const pEl = document.getElementById('inventoryProductCount');
            if (pEl) pEl.textContent = Number(d.totalProducts || 0).toLocaleString();

            const inEl = document.getElementById('stockAddedToday');
            if (inEl) inEl.textContent = `+${Number(d.stockAddedToday || 0).toLocaleString()}`;

            const outEl = document.getElementById('stockRemovedToday');
            if (outEl) outEl.textContent = `-${Number(d.stockRemovedToday || 0).toLocaleString()}`;
        }
    } catch (e) {
        console.error('Failed to load inventory summary:', e);
    }
}

/* =========================================================
   INVENTORY TABLE & PAGINATION
   ========================================================= */

async function loadInventoryData() {
    const tbody = document.getElementById('inventoryTableBody');
    const emptyState = document.getElementById('inventoryEmptyState');
    const resultText = document.getElementById('inventoryResultText');

    if (tbody) {
        tbody.innerHTML = `
            <tr>
                <td colspan="12" class="text-center py-4">
                    <div class="spinner-border spinner-border-sm text-success me-2"></div>
                    <span class="text-muted">Loading live inventory...</span>
                </td>
            </tr>
        `;
    }

    try {
        const params = new URLSearchParams();
        params.set('page', currentPage);
        params.set('size', pageSize);
        if (searchQuery) params.set('search', searchQuery);
        if (selectedCategory && selectedCategory !== 'all') params.set('categoryId', selectedCategory);
        if (selectedStatus && selectedStatus !== 'all') params.set('status', selectedStatus);

        const res = await ApiClient.get(`/admin/inventory?${params.toString()}`);

        if (res && res.success && res.data) {
            const pageData = res.data;
            inventoryItems = pageData.content || [];
            totalPages = pageData.totalPages || 1;
            totalElements = pageData.totalElements || 0;

            if (resultText) {
                resultText.textContent = `Showing ${inventoryItems.length} of ${totalElements} products`;
            }

            renderInventoryTable(inventoryItems);
            renderPaginationUI();

            if (inventoryItems.length === 0) {
                if (emptyState) emptyState.classList.remove('d-none');
            } else {
                if (emptyState) emptyState.classList.add('d-none');
            }
        } else {
            throw new Error(res.message || 'Failed to fetch inventory records');
        }
    } catch (e) {
        console.error('Inventory fetch error:', e);
        if (tbody) {
            tbody.innerHTML = `
                <tr>
                    <td colspan="12" class="text-center py-4 text-danger">
                        <i class="bi bi-exclamation-triangle me-2"></i> ${e.message || 'Error loading inventory'}
                    </td>
                </tr>
            `;
        }
        if (emptyState) emptyState.classList.add('d-none');
    }
}

function renderInventoryTable(items) {
    const tbody = document.getElementById('inventoryTableBody');
    if (!tbody) return;

    if (!items || items.length === 0) {
        tbody.innerHTML = '';
        return;
    }

    tbody.innerHTML = items.map(item => {
        const stock = Number(item.stock || 0);
        const reserved = Number(item.reservedStock || 0);
        const available = Number(item.availableStock !== undefined ? item.availableStock : (stock - reserved));
        const threshold = Number(item.lowStockThreshold || item.minimumStock || 10);
        const status = item.stockStatus || (available <= 0 ? 'OUT_OF_STOCK' : (available <= threshold ? 'LOW_STOCK' : 'IN_STOCK'));

        let badgeHtml = '';
        if (status === 'OUT_OF_STOCK') {
            badgeHtml = '<span class="badge bg-danger">Out of Stock</span>';
        } else if (status === 'LOW_STOCK') {
            badgeHtml = '<span class="badge bg-warning text-dark">Low Stock</span>';
        } else {
            badgeHtml = '<span class="badge bg-success">In Stock</span>';
        }

        const safeName = (item.name || '').replace(/'/g, "\\'");
        const safeSku = (item.sku || '').replace(/'/g, "\\'");

        return `
            <tr>
                <td>${item.id}</td>
                <td><span class="badge bg-light text-dark border font-monospace">${item.sku || '-'}</span></td>
                <td>
                    <div class="fw-semibold">${item.name}</div>
                    <small class="text-muted">${item.composition || ''}</small>
                </td>
                <td>${item.brand || '-'}</td>
                <td>${item.categoryName || '-'}</td>
                <td><strong>${stock}</strong></td>
                <td><span class="text-secondary">${reserved}</span></td>
                <td>
                    <span class="fw-bold ${available <= 0 ? 'text-danger' : (available <= threshold ? 'text-warning' : 'text-success')}">
                        ${available}
                    </span>
                </td>
                <td><small class="text-muted">&le; ${threshold}</small></td>
                <td>${item.unit || 'Bag'}</td>
                <td>${badgeHtml}</td>
                <td class="text-end text-nowrap">
                    <button type="button" class="btn btn-sm btn-outline-success me-1" title="Stock In"
                        onclick="openStockInModal(${item.id}, '${safeName}', '${safeSku}', ${stock})">
                        <i class="bi bi-plus-lg"></i> In
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-warning me-1" title="Adjust Stock"
                        onclick="openStockAdjustModal(${item.id}, '${safeName}', '${safeSku}', ${stock})">
                        <i class="bi bi-sliders"></i>
                    </button>
                    <button type="button" class="btn btn-sm btn-outline-secondary" title="Audit Logs"
                        onclick="openTransactionsModal(${item.id}, '${safeName}')">
                        <i class="bi bi-clock-history"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join('');
}

function renderPaginationUI() {
    const info = document.getElementById('inventoryPageInfo');
    const container = document.getElementById('inventoryPaginationButtons');

    if (info) {
        info.textContent = `Page ${currentPage + 1} of ${totalPages || 1} (${totalElements} items)`;
    }

    if (!container) return;

    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    let html = `
        <button type="button" class="btn btn-sm btn-outline-secondary me-1" ${currentPage === 0 ? 'disabled' : ''} onclick="goToPage(${currentPage - 1})">
            &laquo; Prev
        </button>
    `;

    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(totalPages - 1, currentPage + 2);

    for (let p = startPage; p <= endPage; p++) {
        html += `
            <button type="button" class="btn btn-sm ${p === currentPage ? 'btn-success' : 'btn-outline-secondary'} me-1" onclick="goToPage(${p})">
                ${p + 1}
            </button>
        `;
    }

    html += `
        <button type="button" class="btn btn-sm btn-outline-secondary" ${currentPage >= totalPages - 1 ? 'disabled' : ''} onclick="goToPage(${currentPage + 1})">
            Next &raquo;
        </button>
    `;

    container.innerHTML = html;
}

function goToPage(page) {
    if (page < 0 || page >= totalPages) return;
    currentPage = page;
    loadInventoryData();
}

/* =========================================================
   STOCK IN MODAL & ACTION
   ========================================================= */

function openStockInModal(id, name, sku, stock) {
    const idEl = document.getElementById('stockInFertilizerId');
    const nameEl = document.getElementById('stockInProductName');
    const curStockEl = document.getElementById('stockInCurrentStock');
    const qtyEl = document.getElementById('stockInQuantity');
    const form = document.getElementById('stockInForm');

    if (form) form.reset();

    if (idEl) idEl.value = id || '';
    if (nameEl) nameEl.value = `${name} (${sku || ''})`;
    if (curStockEl) curStockEl.value = `${stock || 0} units`;
    if (qtyEl) qtyEl.value = '';

    if (stockInModalInstance) {
        stockInModalInstance.show();
    }
}

async function handleStockInSubmit(e) {
    e.preventDefault();
    const btn = document.getElementById('stockInSubmitBtn');
    const fertilizerId = document.getElementById('stockInFertilizerId')?.value;
    const quantity = parseInt(document.getElementById('stockInQuantity')?.value || '0', 10);
    const unitCost = document.getElementById('stockInUnitCost')?.value ? parseFloat(document.getElementById('stockInUnitCost').value) : null;
    const supplier = document.getElementById('stockInSupplier')?.value.trim();
    const referenceId = document.getElementById('stockInReferenceId')?.value.trim();
    const notes = document.getElementById('stockInNotes')?.value.trim();

    if (!fertilizerId || quantity <= 0) {
        if (typeof toast === 'function') toast('Please enter a valid positive quantity', 'danger');
        return;
    }

    try {
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Adding...';
        }

        const payload = {
            fertilizerId: Number(fertilizerId),
            quantity: quantity,
            unitCost: unitCost,
            supplier: supplier || null,
            referenceId: referenceId || null,
            notes: notes || null
        };

        const res = await ApiClient.post('/admin/inventory/stock-in', payload);

        if (res && res.success) {
            if (stockInModalInstance) stockInModalInstance.hide();
            if (typeof toast === 'function') toast(res.message || 'Stock added successfully', 'success');
            await loadInventorySummary();
            await loadInventoryData();
        } else {
            throw new Error(res.message || 'Failed to add stock');
        }
    } catch (err) {
        console.error('Stock-in error:', err);
        if (typeof toast === 'function') toast(err.message || 'Error executing stock-in', 'danger');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-check-circle me-1"></i> Add Stock';
        }
    }
}

/* =========================================================
   STOCK ADJUSTMENT MODAL & ACTION
   ========================================================= */

function openStockAdjustModal(id, name, sku, stock) {
    const idEl = document.getElementById('adjustFertilizerId');
    const nameEl = document.getElementById('adjustProductName');
    const curStockEl = document.getElementById('adjustCurrentStock');
    const form = document.getElementById('stockAdjustForm');

    if (form) form.reset();

    if (idEl) idEl.value = id || '';
    if (nameEl) nameEl.value = `${name} (${sku || ''})`;
    if (curStockEl) curStockEl.value = `${stock || 0} units`;

    onAdjustTypeChange();

    if (stockAdjustModalInstance) {
        stockAdjustModalInstance.show();
    }
}

function onAdjustTypeChange() {
    const type = document.getElementById('adjustType')?.value;
    const label = document.getElementById('adjustQuantityLabel');
    const qtyInput = document.getElementById('adjustQuantity');

    if (type === 'SET') {
        if (label) label.textContent = 'New Total Physical Stock Level *';
        if (qtyInput) {
            qtyInput.min = '0';
            qtyInput.placeholder = 'Exact target stock count';
        }
    } else if (type === 'REMOVE') {
        if (label) label.textContent = 'Units to Deduct / Remove *';
        if (qtyInput) {
            qtyInput.min = '1';
            qtyInput.placeholder = 'e.g. 5';
        }
    } else {
        if (label) label.textContent = 'Units to Add *';
        if (qtyInput) {
            qtyInput.min = '1';
            qtyInput.placeholder = 'e.g. 10';
        }
    }
}

function onAdjustReasonSelectChange(sel) {
    const reasonInput = document.getElementById('adjustReason');
    if (!reasonInput) return;
    if (sel.value && sel.value !== 'Custom') {
        reasonInput.value = sel.value;
    } else if (sel.value === 'Custom') {
        reasonInput.value = '';
        reasonInput.focus();
    }
}

async function handleStockAdjustSubmit(e) {
    e.preventDefault();
    const btn = document.getElementById('stockAdjustSubmitBtn');
    const fertilizerId = document.getElementById('adjustFertilizerId')?.value;
    const type = document.getElementById('adjustType')?.value;
    const quantity = parseInt(document.getElementById('adjustQuantity')?.value || '0', 10);
    const reason = document.getElementById('adjustReason')?.value.trim();
    const notes = document.getElementById('adjustNotes')?.value.trim();

    if (!fertilizerId || quantity < 0) {
        if (typeof toast === 'function') toast('Please enter a valid quantity', 'danger');
        return;
    }
    if (!reason) {
        if (typeof toast === 'function') toast('Mandatory audit reason is required', 'danger');
        return;
    }

    try {
        if (btn) {
            btn.disabled = true;
            btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Adjusting...';
        }

        const payload = {
            fertilizerId: Number(fertilizerId),
            adjustmentType: type,
            quantity: quantity,
            reason: reason,
            notes: notes || null
        };

        const res = await ApiClient.post('/admin/inventory/adjust', payload);

        if (res && res.success) {
            if (stockAdjustModalInstance) stockAdjustModalInstance.hide();
            if (typeof toast === 'function') toast(res.message || 'Stock adjusted successfully', 'success');
            await loadInventorySummary();
            await loadInventoryData();
        } else {
            throw new Error(res.message || 'Failed to adjust stock');
        }
    } catch (err) {
        console.error('Stock adjustment error:', err);
        if (typeof toast === 'function') toast(err.message || 'Error executing stock adjustment', 'danger');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="bi bi-check-circle me-1"></i> Confirm Adjustment';
        }
    }
}

/* =========================================================
   TRANSACTION AUDIT LOGS MODAL
   ========================================================= */

async function openTransactionsModal(productId, productName) {
    const subtitle = document.getElementById('transactionsModalSubtitle');
    const tbody = document.getElementById('transactionsTableBody');

    if (subtitle) {
        subtitle.textContent = productId ? `Audit trail for ${productName || 'Product #' + productId}` : 'All recent stock movements';
    }

    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div>Loading audit transactions...</td></tr>';
    }

    if (transactionsModalInstance) {
        transactionsModalInstance.show();
    }

    try {
        const endpoint = productId ? `/admin/inventory/transactions?productId=${productId}` : '/admin/inventory/transactions';
        const res = await ApiClient.get(endpoint);

        if (res && res.success && res.data) {
            allTransactions = res.data.content || res.data || [];
            renderTransactionsTable(allTransactions);
        } else {
            throw new Error(res.message || 'Failed to retrieve audit transactions');
        }
    } catch (e) {
        console.error('Transactions fetch error:', e);
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="9" class="text-center py-4 text-danger"><i class="bi bi-exclamation-circle me-2"></i>${e.message}</td></tr>`;
        }
    }
}

function filterTransactionsTable() {
    const text = document.getElementById('transactionsFilterInput')?.value.toLowerCase().trim() || '';
    const type = document.getElementById('transactionsTypeFilter')?.value || 'ALL';

    const filtered = allTransactions.filter(t => {
        const matchType = (type === 'ALL' || t.transactionType === type);
        const matchText = !text ||
            (t.productName && t.productName.toLowerCase().includes(text)) ||
            (t.referenceId && t.referenceId.toLowerCase().includes(text)) ||
            (t.performedBy && t.performedBy.toLowerCase().includes(text)) ||
            (t.reason && t.reason.toLowerCase().includes(text));
        return matchType && matchText;
    });

    renderTransactionsTable(filtered);
}

function renderTransactionsTable(transactions) {
    const tbody = document.getElementById('transactionsTableBody');
    if (!tbody) return;

    if (!transactions || transactions.length === 0) {
        tbody.innerHTML = '<tr><td colspan="9" class="text-center py-4 text-muted">No stock movement transactions recorded yet.</td></tr>';
        return;
    }

    tbody.innerHTML = transactions.map((t, idx) => {
        const isAddition = ['PURCHASE', 'RESTOCK', 'ADJUSTMENT_IN', 'RETURN', 'CANCELLATION', 'REFUND'].includes(t.transactionType);
        const qtyColor = isAddition ? 'text-success' : 'text-danger';
        const qtySign = isAddition ? '+' : '-';

        let badgeClass = 'bg-secondary';
        if (t.transactionType === 'SALE') badgeClass = 'bg-primary';
        else if (t.transactionType === 'RESTOCK' || t.transactionType === 'PURCHASE') badgeClass = 'bg-success';
        else if (t.transactionType === 'ADJUSTMENT_IN') badgeClass = 'bg-info text-dark';
        else if (t.transactionType === 'ADJUSTMENT_OUT') badgeClass = 'bg-warning text-dark';
        else if (t.transactionType === 'DAMAGE') badgeClass = 'bg-danger';
        else if (t.transactionType === 'CANCELLATION') badgeClass = 'bg-secondary';

        const dtStr = t.createdAt ? new Date(t.createdAt).toLocaleString('en-IN') : '-';

        return `
            <tr>
                <td>${t.id || (idx + 1)}</td>
                <td class="text-nowrap text-muted">${dtStr}</td>
                <td>
                    <span class="fw-semibold">${t.productName || 'Product #' + t.fertilizerId}</span>
                    <span class="badge bg-light text-dark border ms-1 font-monospace">${t.sku || ''}</span>
                </td>
                <td><span class="badge ${badgeClass}">${t.transactionType}</span></td>
                <td><strong class="${qtyColor}">${qtySign}${t.quantity}</strong></td>
                <td><span class="text-muted">${t.stockBefore}</span> &rarr; <strong>${t.stockAfter}</strong></td>
                <td><span class="font-monospace text-secondary">${t.referenceId || '-'}</span></td>
                <td><span class="badge bg-light text-dark border"><i class="bi bi-person me-1"></i>${t.performedBy || 'System'}</span></td>
                <td>
                    <div>${t.reason || '-'}</div>
                    ${t.notes ? `<small class="text-muted fst-italic">${t.notes}</small>` : ''}
                </td>
            </tr>
        `;
    }).join('');
}

/* =========================================================
   SYSTEM HEALTH MODAL & ACTION
   ========================================================= */

function openHealthModal() {
    if (healthModalInstance) {
        healthModalInstance.show();
    }
    loadInventoryHealth();
}

async function loadInventoryHealth() {
    const loading = document.getElementById('healthLoading');
    const content = document.getElementById('healthContent');

    if (loading) loading.classList.remove('d-none');
    if (content) content.classList.add('d-none');

    try {
        const res = await ApiClient.get('/admin/inventory/health');

        if (res && res.success && res.data) {
            const h = res.data;

            const card = document.getElementById('healthStatusCard');
            const title = document.getElementById('healthStatusTitle');
            const desc = document.getElementById('healthStatusDesc');
            const badge = document.getElementById('healthStatusBadge');

            if (title) title.textContent = h.status;
            if (badge) badge.textContent = h.status;

            if (h.status === 'HEALTHY') {
                if (card) card.className = 'd-flex align-items-center justify-content-between p-3 rounded mb-3 bg-success-subtle border border-success';
                if (badge) badge.className = 'badge bg-success fs-6';
                if (desc) desc.textContent = 'All inventory invariants verified. No negative stock or discrepancies.';
            } else if (h.status === 'WARNING') {
                if (card) card.className = 'd-flex align-items-center justify-content-between p-3 rounded mb-3 bg-warning-subtle border border-warning';
                if (badge) badge.className = 'badge bg-warning text-dark fs-6';
                if (desc) desc.textContent = 'Low or out of stock items require review.';
            } else {
                if (card) card.className = 'd-flex align-items-center justify-content-between p-3 rounded mb-3 bg-danger-subtle border border-danger';
                if (badge) badge.className = 'badge bg-danger fs-6';
                if (desc) desc.textContent = 'Critical invariants violated! Potential negative stock detected.';
            }

            const pEl = document.getElementById('healthProductsChecked');
            if (pEl) pEl.textContent = h.productsChecked;

            const uEl = document.getElementById('healthTotalUnits');
            if (uEl) uEl.textContent = Number(h.totalStockUnits || 0).toLocaleString();

            const nEl = document.getElementById('healthNegativeCount');
            if (nEl) {
                nEl.textContent = h.negativeStockCount;
                nEl.className = h.negativeStockCount > 0 ? 'text-danger fw-bold' : 'text-success';
            }

            const lEl = document.getElementById('healthLowStockCount');
            if (lEl) lEl.textContent = h.lowStockCount;

            const oEl = document.getElementById('healthOutOfStockCount');
            if (oEl) oEl.textContent = h.outOfStockCount;

            const tEl = document.getElementById('healthTotalTransactions');
            if (tEl) tEl.textContent = h.totalTransactions;

            const issuesContainer = document.getElementById('healthIssuesContainer');
            const issuesList = document.getElementById('healthIssuesList');

            if (h.issuesFound > 0 && Array.isArray(h.issuesList) && h.issuesList.length > 0) {
                if (issuesContainer) issuesContainer.classList.remove('d-none');
                if (issuesList) {
                    issuesList.innerHTML = h.issuesList.map(issue => `<li>${issue}</li>`).join('');
                }
            } else {
                if (issuesContainer) issuesContainer.classList.add('d-none');
            }

            if (loading) loading.classList.add('d-none');
            if (content) content.classList.remove('d-none');
        } else {
            throw new Error(res.message || 'Failed to audit health');
        }
    } catch (e) {
        console.error('Health check error:', e);
        if (loading) loading.classList.add('d-none');
        if (typeof toast === 'function') toast(e.message || 'Health audit check failed', 'danger');
    }
}