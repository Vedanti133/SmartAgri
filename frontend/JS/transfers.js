const API_BASE = (typeof CONFIG !== 'undefined' && CONFIG.API_BASE_URL) || (typeof API_URL !== 'undefined' && API_URL) || "http://localhost:8080/api";
let currentTransfers = [];
let filteredTransfers = [];
let allWarehouses = [];
let sourceWarehouseLocations = [];
let sourceLocationStocks = [];
let activeTransferForReceive = null;

let createTransferModalInstance = null;
let receiveTransferModalInstance = null;
let transferDetailsModalInstance = null;

document.addEventListener("DOMContentLoaded", () => {
    checkAdminAuth();
    createTransferModalInstance = new bootstrap.Modal(document.getElementById("createTransferModal"));
    receiveTransferModalInstance = new bootstrap.Modal(document.getElementById("receiveTransferModal"));
    transferDetailsModalInstance = new bootstrap.Modal(document.getElementById("transferDetailsModal"));

    loadTransfers();
    loadTransfersKPIs();
    loadWarehousesDropdown();
});

function checkAdminAuth() {
    const token = localStorage.getItem("token") || localStorage.getItem("smartagriAuthToken") || localStorage.getItem("smartagri_auth_token");
    let role = localStorage.getItem("role");
    let username = localStorage.getItem("username");
    
    if (!role || !username) {
        try {
            const u = JSON.parse(localStorage.getItem("user") || localStorage.getItem("smartagriUser") || localStorage.getItem("smartagri_user") || "{}");
            if (u.role) role = u.role;
            if (u.username) username = u.username;
        } catch(e) {}
    }

    if (!token) {
        window.location.href = window.location.pathname.includes('/admin/') ? "../HTML/login.html" : "login.html";
        return;
    }
    if (role && role !== "ADMIN") {
        alert("Access denied: Administrator privileges required.");
        window.location.href = window.location.pathname.includes('/admin/') ? "../HTML/index.html" : "customer-catalog.html";
        return;
    }
    const nameEl = document.getElementById("currentUserName");
    if (nameEl && username) {
        nameEl.textContent = username;
    }
}

function logout() {
    ['smartagriUser', 'smartagri_user', 'user', 'smartagriAuthToken', 'smartagri_auth_token', 'token', 'role', 'username'].forEach(k => localStorage.removeItem(k));
    window.location.href = window.location.pathname.includes('/admin/') ? "../HTML/login.html" : "login.html";
}

function getAuthHeaders() {
    const token = localStorage.getItem("token") || localStorage.getItem("smartagriAuthToken") || localStorage.getItem("smartagri_auth_token");
    return {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
    };
}

function showAlert(message, type = "success") {
    const container = document.getElementById("alertContainer");
    if (!container) return;
    container.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show" role="alert">
            <i class="bi bi-${type === 'success' ? 'check-circle' : 'exclamation-circle'} me-2"></i>
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    setTimeout(() => {
        const al = container.querySelector(".alert");
        if (al) {
            const b = bootstrap.Alert.getOrCreateInstance(al);
            if (b) b.close();
        }
    }, 5000);
}

// ----------------------------------------------------
// LOAD TRANSFERS & METRICS
// ----------------------------------------------------
async function loadTransfers() {
    const tbody = document.getElementById("transfersTableBody");
    tbody.innerHTML = `
        <tr>
            <td colspan="7" class="text-center py-4 text-muted">
                <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
                Loading stock transfers...
            </td>
        </tr>
    `;

    try {
        const res = await fetch(`${API_BASE}/admin/transfers`, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        currentTransfers = data.data || [];
        filterTransfers();
    } catch (err) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-4 text-danger">
                    <i class="bi bi-exclamation-triangle me-2"></i>Failed to load transfers: ${err.message}
                </td>
            </tr>
        `;
    }
}

function loadTransfersKPIs() {
    const total = currentTransfers.length;
    let pending = 0;
    let dispatched = 0;
    let completed = 0;

    currentTransfers.forEach(t => {
        if (t.status === "PENDING_APPROVAL") pending++;
        else if (t.status === "DISPATCHED" || t.status === "PARTIALLY_RECEIVED") dispatched++;
        else if (t.status === "FULLY_RECEIVED") completed++;
    });

    document.getElementById("kpiTotalTransfers").textContent = total;
    document.getElementById("kpiPendingApproval").textContent = pending;
    document.getElementById("kpiDispatched").textContent = dispatched;
    document.getElementById("kpiCompleted").textContent = completed;
}

async function loadWarehousesDropdown() {
    try {
        const res = await fetch(`${API_BASE}/admin/warehouses`, { headers: getAuthHeaders() });
        if (!res.ok) return;
        const data = await res.json();
        allWarehouses = data.data || [];

        const srcFilter = document.getElementById("filterSourceWhSelect");
        const destFilter = document.getElementById("filterDestWhSelect");
        const newSrc = document.getElementById("newSourceWh");
        const newDest = document.getElementById("newDestWh");

        srcFilter.innerHTML = `<option value="">All Warehouses</option>`;
        destFilter.innerHTML = `<option value="">All Warehouses</option>`;
        newSrc.innerHTML = `<option value="">-- Select Source Facility --</option>`;
        newDest.innerHTML = `<option value="">-- Select Destination Facility --</option>`;

        allWarehouses.forEach(w => {
            const opt = `<option value="${w.id}">${escapeHtml(w.warehouseCode)} - ${escapeHtml(w.warehouseName)}</option>`;
            srcFilter.innerHTML += opt;
            destFilter.innerHTML += opt;
            if (w.status === "ACTIVE") {
                newSrc.innerHTML += opt;
                newDest.innerHTML += opt;
            }
        });
    } catch (e) {
        console.error("Failed to load warehouses for dropdowns:", e);
    }
}

// ----------------------------------------------------
// FILTERING & RENDERING
// ----------------------------------------------------
function filterTransfers() {
    const search = (document.getElementById("searchTransferInput").value || "").toLowerCase().trim();
    const status = document.getElementById("filterStatusSelect").value;
    const srcWhId = document.getElementById("filterSourceWhSelect").value;
    const destWhId = document.getElementById("filterDestWhSelect").value;

    filteredTransfers = currentTransfers.filter(t => {
        const matchSearch = !search ||
            (t.transferNumber && t.transferNumber.toLowerCase().includes(search));
        const matchStatus = !status || t.status === status;
        const matchSrc = !srcWhId || (t.sourceWarehouseId && String(t.sourceWarehouseId) === srcWhId);
        const matchDest = !destWhId || (t.destinationWarehouseId && String(t.destinationWarehouseId) === destWhId);
        return matchSearch && matchStatus && matchSrc && matchDest;
    });

    renderTransfersTable();
    loadTransfersKPIs();
}

function resetTransferFilters() {
    document.getElementById("searchTransferInput").value = "";
    document.getElementById("filterStatusSelect").value = "";
    document.getElementById("filterSourceWhSelect").value = "";
    document.getElementById("filterDestWhSelect").value = "";
    filterTransfers();
}

function renderTransfersTable() {
    const tbody = document.getElementById("transfersTableBody");
    const countEl = document.getElementById("transfersTableCount");
    countEl.textContent = `${filteredTransfers.length} Transfer${filteredTransfers.length === 1 ? '' : 's'}`;

    if (!filteredTransfers.length) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-4 text-muted">
                    No stock transfers found matching filters.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filteredTransfers.map(t => {
        let badgeClass = "bg-secondary";
        if (t.status === "PENDING_APPROVAL") badgeClass = "bg-warning text-dark";
        else if (t.status === "APPROVED") badgeClass = "bg-info text-dark";
        else if (t.status === "DISPATCHED") badgeClass = "bg-primary";
        else if (t.status === "PARTIALLY_RECEIVED") badgeClass = "bg-warning text-dark";
        else if (t.status === "FULLY_RECEIVED") badgeClass = "bg-success";
        else if (t.status === "CANCELLED" || t.status === "REJECTED") badgeClass = "bg-danger";

        // Action Buttons based on status
        let actionButtons = `
            <button class="btn btn-outline-info btn-sm" title="View Details" onclick="viewTransferDetails(${t.id})">
                <i class="bi bi-eye"></i>
            </button>
        `;

        if (t.status === "DRAFT") {
            actionButtons += `
                <button class="btn btn-outline-primary btn-sm ms-1" title="Submit for Approval" onclick="submitForApproval(${t.id})">
                    <i class="bi bi-send-check"></i> Submit
                </button>
                <button class="btn btn-outline-danger btn-sm ms-1" title="Cancel" onclick="cancelTransfer(${t.id})">
                    <i class="bi bi-x-circle"></i>
                </button>
            `;
        } else if (t.status === "PENDING_APPROVAL") {
            actionButtons += `
                <button class="btn btn-outline-success btn-sm ms-1" title="Approve" onclick="approveTransfer(${t.id})">
                    <i class="bi bi-check-lg"></i> Approve
                </button>
                <button class="btn btn-outline-danger btn-sm ms-1" title="Reject" onclick="rejectTransfer(${t.id})">
                    <i class="bi bi-slash-circle"></i>
                </button>
            `;
        } else if (t.status === "APPROVED") {
            actionButtons += `
                <button class="btn btn-primary btn-sm ms-1" title="Dispatch from Source" onclick="dispatchTransfer(${t.id})">
                    <i class="bi bi-truck"></i> Dispatch
                </button>
            `;
        } else if (t.status === "DISPATCHED" || t.status === "PARTIALLY_RECEIVED") {
            actionButtons += `
                <button class="btn btn-success btn-sm ms-1" title="Receive at Destination" onclick="openReceiveTransferModal(${t.id})">
                    <i class="bi bi-box-arrow-in-down"></i> Receive
                </button>
            `;
        }

        const dateStr = t.createdAt ? t.createdAt.replace("T", " ").substring(0, 16) : "N/A";

        return `
            <tr>
                <td>
                    <div class="fw-bold text-dark">${escapeHtml(t.transferNumber)}</div>
                    <small class="text-muted">${dateStr}</small>
                </td>
                <td>
                    <div class="fw-semibold">${escapeHtml(t.sourceWarehouseName || '')}</div>
                    <code class="small text-muted">${escapeHtml(t.sourceWarehouseCode || '')}</code>
                </td>
                <td>
                    <div class="fw-semibold">${escapeHtml(t.destinationWarehouseName || '')}</div>
                    <code class="small text-muted">${escapeHtml(t.destinationWarehouseCode || '')}</code>
                </td>
                <td>
                    <span class="fw-bold">${t.totalRequestedUnits || 0}</span> req / 
                    <span class="text-primary">${t.totalDispatchedUnits || 0}</span> disp / 
                    <span class="text-success">${t.totalReceivedUnits || 0}</span> rcvd
                    <div class="small text-muted">${t.itemCount || (t.items ? t.items.length : 0)} items</div>
                </td>
                <td><span class="badge ${badgeClass} status-badge">${t.status}</span></td>
                <td>
                    <div class="small">By: <strong>${escapeHtml(t.requestedBy || 'admin')}</strong></div>
                    ${t.dispatchedBy ? `<div class="small text-muted">Disp: ${escapeHtml(t.dispatchedBy)}</div>` : ''}
                </td>
                <td class="text-end">
                    <div class="d-flex justify-content-end align-items-center">
                        ${actionButtons}
                    </div>
                </td>
            </tr>
        `;
    }).join("");
}

// ----------------------------------------------------
// CREATE TRANSFER WORKFLOW
// ----------------------------------------------------
function openCreateTransferModal() {
    document.getElementById("createTransferForm").reset();
    document.getElementById("transferItemsContainer").innerHTML = "";
    sourceWarehouseLocations = [];
    sourceLocationStocks = [];
    addTransferItemRow();
    createTransferModalInstance.show();
}

async function onSourceWarehouseChange() {
    const srcId = document.getElementById("newSourceWh").value;
    const container = document.getElementById("transferItemsContainer");
    container.innerHTML = "";

    if (!srcId) {
        sourceWarehouseLocations = [];
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/admin/warehouses/${srcId}/locations`, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error("Failed to load locations");
        const data = await res.json();
        sourceWarehouseLocations = (data.data || []).filter(l => l.status === "ACTIVE");
        addTransferItemRow();
    } catch (e) {
        alert("Error loading source locations: " + e.message);
    }
}

let itemRowIndex = 0;
function addTransferItemRow() {
    const container = document.getElementById("transferItemsContainer");
    const idx = itemRowIndex++;

    const row = document.createElement("div");
    row.className = "card bg-light border p-3 mb-2 transfer-item-row";
    row.id = `itemRow_${idx}`;
    row.innerHTML = `
        <div class="row g-2 align-items-end">
            <div class="col-md-4">
                <label class="form-label small fw-semibold">Source Bin / Location <span class="text-danger">*</span></label>
                <select class="form-select form-select-sm" id="itemSrcLoc_${idx}" onchange="onSourceLocationChange(${idx})" required>
                    <option value="">-- Select Source Location --</option>
                    ${sourceWarehouseLocations.map(l => `<option value="${l.id}">${escapeHtml(l.locationCode)} (${l.locationType})</option>`).join("")}
                </select>
            </div>
            <div class="col-md-5">
                <label class="form-label small fw-semibold">Batch / Product <span class="text-danger">*</span></label>
                <select class="form-select form-select-sm" id="itemBatch_${idx}" onchange="onBatchSelectChange(${idx})" required>
                    <option value="">-- Select Source Location First --</option>
                </select>
            </div>
            <div class="col-md-2">
                <label class="form-label small fw-semibold">Qty <span class="text-danger">*</span></label>
                <input type="number" class="form-control form-control-sm" id="itemQty_${idx}" min="1" placeholder="Max: 0" required>
            </div>
            <div class="col-md-1 text-end">
                <button type="button" class="btn btn-outline-danger btn-sm" onclick="removeItemRow(${idx})">
                    <i class="bi bi-trash"></i>
                </button>
            </div>
        </div>
    `;
    container.appendChild(row);
}

function removeItemRow(idx) {
    const row = document.getElementById(`itemRow_${idx}`);
    if (row) row.remove();
}

async function onSourceLocationChange(idx) {
    const locId = document.getElementById(`itemSrcLoc_${idx}`).value;
    const batchSelect = document.getElementById(`itemBatch_${idx}`);
    const qtyInput = document.getElementById(`itemQty_${idx}`);
    batchSelect.innerHTML = `<option value="">Loading stocks...</option>`;
    qtyInput.value = "";
    qtyInput.placeholder = "Max: 0";

    if (!locId) {
        batchSelect.innerHTML = `<option value="">-- Select Source Location First --</option>`;
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/admin/locations/${locId}/stock`, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error("Failed to load location stocks");
        const data = await res.json();
        const stocks = (data.data || []).filter(s => s.availableQuantity > 0);

        if (!stocks.length) {
            batchSelect.innerHTML = `<option value="">-- No available stock in this bin --</option>`;
            return;
        }

        batchSelect.innerHTML = `<option value="">-- Select Batch / Lot --</option>`;
        stocks.forEach(s => {
            batchSelect.innerHTML += `
                <option value="${s.batchId}" data-avail="${s.availableQuantity}" data-name="${escapeHtml(s.fertilizerName)}">
                    ${escapeHtml(s.batchNumber)} - ${escapeHtml(s.fertilizerName)} (Avail: ${s.availableQuantity})
                </option>
            `;
        });
    } catch (e) {
        batchSelect.innerHTML = `<option value="">Error loading stock</option>`;
    }
}

function onBatchSelectChange(idx) {
    const batchSelect = document.getElementById(`itemBatch_${idx}`);
    const selected = batchSelect.options[batchSelect.selectedIndex];
    const avail = selected ? parseInt(selected.getAttribute("data-avail") || "0") : 0;
    const qtyInput = document.getElementById(`itemQty_${idx}`);
    qtyInput.max = avail;
    qtyInput.placeholder = `Max: ${avail}`;
}

async function submitTransfer(targetStatus) {
    const srcId = document.getElementById("newSourceWh").value;
    const destId = document.getElementById("newDestWh").value;
    const reason = document.getElementById("newTransferReason").value.trim();

    if (!srcId || !destId) {
        alert("Source and Destination facilities are required.");
        return;
    }
    if (srcId === destId) {
        alert("Source and Destination facilities must be different.");
        return;
    }

    const rows = document.querySelectorAll(".transfer-item-row");
    const items = [];

    for (let r of rows) {
        const idx = r.id.replace("itemRow_", "");
        const locSelect = document.getElementById(`itemSrcLoc_${idx}`);
        const batchSelect = document.getElementById(`itemBatch_${idx}`);
        const qtyInput = document.getElementById(`itemQty_${idx}`);

        if (!locSelect || !batchSelect || !qtyInput) continue;

        const locId = locSelect.value;
        const batchId = batchSelect.value;
        const qty = parseInt(qtyInput.value || "0");

        if (!locId || !batchId || qty <= 0) {
            alert("All item fields are required and quantity must be greater than zero.");
            return;
        }

        items.push({
            batchId: parseInt(batchId),
            sourceLocationId: parseInt(locId),
            requestedQuantity: qty
        });
    }

    if (!items.length) {
        alert("Please add at least one transfer item.");
        return;
    }

    const body = {
        sourceWarehouseId: parseInt(srcId),
        destinationWarehouseId: parseInt(destId),
        reason: reason || null,
        status: targetStatus,
        items: items
    };

    try {
        const res = await fetch(`${API_BASE}/admin/transfers`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify(body)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to create transfer");

        createTransferModalInstance.hide();
        showAlert(`Transfer Order ${data.data.transferNumber} created successfully!`, "success");
        loadTransfers();
        loadTransfersKPIs();
    } catch (err) {
        alert(`Error: ${err.message}`);
    }
}

// ----------------------------------------------------
// STATE TRANSITIONS & ACTIONS
// ----------------------------------------------------
async function submitForApproval(id) {
    if (!confirm("Submit this transfer order for management approval?")) return;
    try {
        const res = await fetch(`${API_BASE}/admin/transfers/${id}/submit`, {
            method: "POST",
            headers: getAuthHeaders()
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to submit transfer");

        showAlert("Transfer submitted for approval", "success");
        loadTransfers();
    } catch (e) {
        alert(`Error: ${e.message}`);
    }
}

async function approveTransfer(id) {
    if (!confirm("Approve this stock transfer? Once approved, it can be dispatched from the source warehouse.")) return;
    try {
        const res = await fetch(`${API_BASE}/admin/transfers/${id}/approve`, {
            method: "POST",
            headers: getAuthHeaders()
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to approve transfer");

        showAlert("Transfer approved successfully", "success");
        loadTransfers();
    } catch (e) {
        alert(`Error: ${e.message}`);
    }
}

async function rejectTransfer(id) {
    const reason = prompt("Enter reason for rejecting this transfer:");
    if (reason === null) return;

    try {
        const res = await fetch(`${API_BASE}/admin/transfers/${id}/reject?reason=${encodeURIComponent(reason)}`, {
            method: "POST",
            headers: getAuthHeaders()
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to reject transfer");

        showAlert("Transfer rejected", "warning");
        loadTransfers();
    } catch (e) {
        alert(`Error: ${e.message}`);
    }
}

async function dispatchTransfer(id) {
    if (!confirm("Dispatch transfer? This will deduct the inventory from the source location and mark items IN TRANSIT.")) return;
    try {
        const res = await fetch(`${API_BASE}/admin/transfers/${id}/dispatch`, {
            method: "POST",
            headers: getAuthHeaders()
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to dispatch transfer");

        showAlert("Transfer dispatched! Inventory deducted from source location.", "success");
        loadTransfers();
    } catch (e) {
        alert(`Error: ${e.message}`);
    }
}

async function cancelTransfer(id) {
    const reason = prompt("Enter reason for cancellation:");
    if (reason === null) return;

    try {
        const res = await fetch(`${API_BASE}/admin/transfers/${id}/cancel?reason=${encodeURIComponent(reason)}`, {
            method: "POST",
            headers: getAuthHeaders()
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to cancel transfer");

        showAlert("Transfer cancelled", "warning");
        loadTransfers();
    } catch (e) {
        alert(`Error: ${e.message}`);
    }
}

// ----------------------------------------------------
// RECEIVE TRANSFER WORKFLOW
// ----------------------------------------------------
async function openReceiveTransferModal(id) {
    const transfer = currentTransfers.find(t => t.id === id);
    if (!transfer) return;

    activeTransferForReceive = transfer;
    document.getElementById("receiveTransferId").value = transfer.id;
    document.getElementById("receiveModalTrNumber").textContent = transfer.transferNumber;
    document.getElementById("receiveNotes").value = "";

    const tbody = document.getElementById("receiveItemsTableBody");
    tbody.innerHTML = `<tr><td colspan="5" class="text-center py-3 text-muted">Loading destination bins...</td></tr>`;
    receiveTransferModalInstance.show();

    // Fetch active locations for the destination warehouse
    let destLocations = [];
    try {
        const res = await fetch(`${API_BASE}/admin/warehouses/${transfer.destinationWarehouseId}/locations`, {
            headers: getAuthHeaders()
        });
        if (res.ok) {
            const data = await res.json();
            destLocations = (data.data || []).filter(l => l.status === "ACTIVE");
        }
    } catch (e) {
        console.error("Failed to load dest locations", e);
    }

    if (!destLocations.length) {
        tbody.innerHTML = `<tr><td colspan="5" class="text-center py-3 text-danger">No active locations found in destination warehouse. Please add an active bin first.</td></tr>`;
        return;
    }

    tbody.innerHTML = (transfer.items || []).map(item => {
        const remaining = (item.dispatchedQuantity || 0) - (item.receivedQuantity || 0);
        if (remaining <= 0) return "";

        const locOptions = destLocations.map(l =>
            `<option value="${l.id}">${escapeHtml(l.locationCode)} (${l.locationType})</option>`
        ).join("");

        return `
            <tr data-item-id="${item.id}">
                <td>
                    <div class="fw-bold">${escapeHtml(item.fertilizerName)}</div>
                    <code class="small text-muted">${escapeHtml(item.batchNumber)}</code>
                </td>
                <td><span class="badge bg-primary">${item.dispatchedQuantity}</span></td>
                <td><span class="badge bg-secondary">${item.receivedQuantity || 0}</span></td>
                <td>
                    <input type="number" class="form-control form-control-sm rcv-qty" value="${remaining}" min="1" max="${remaining}" required>
                </td>
                <td>
                    <select class="form-select form-select-sm rcv-loc" required>
                        ${locOptions}
                    </select>
                </td>
            </tr>
        `;
    }).join("");
}

async function confirmReceiveTransfer() {
    if (!activeTransferForReceive) return;
    const transferId = activeTransferForReceive.id;
    const rows = document.querySelectorAll("#receiveItemsTableBody tr[data-item-id]");
    const receiveItems = [];

    for (let r of rows) {
        const itemId = parseInt(r.getAttribute("data-item-id"));
        const qtyInput = r.querySelector(".rcv-qty");
        const locSelect = r.querySelector(".rcv-loc");

        const qty = parseInt(qtyInput.value || "0");
        const locId = parseInt(locSelect.value || "0");

        if (qty > 0 && locId > 0) {
            receiveItems.push({
                itemId: itemId,
                receivedQuantity: qty,
                destinationLocationId: locId
            });
        }
    }

    if (!receiveItems.length) {
        alert("Please enter receiving quantity and location for at least one item.");
        return;
    }

    const body = {
        items: receiveItems,
        notes: document.getElementById("receiveNotes").value.trim() || null
    };

    try {
        const res = await fetch(`${API_BASE}/admin/transfers/${transferId}/receive`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify(body)
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to receive transfer items");

        receiveTransferModalInstance.hide();
        showAlert("Stock successfully received and added to destination bin!", "success");
        loadTransfers();
    } catch (e) {
        alert(`Error: ${e.message}`);
    }
}

// ----------------------------------------------------
// VIEW DETAILS MODAL
// ----------------------------------------------------
function viewTransferDetails(id) {
    const t = currentTransfers.find(item => item.id === id);
    if (!t) return;

    document.getElementById("viewTrNumber").textContent = t.transferNumber;
    document.getElementById("viewSourceWh").textContent = `${t.sourceWarehouseName} (${t.sourceWarehouseCode})`;
    document.getElementById("viewDestWh").textContent = `${t.destinationWarehouseName} (${t.destinationWarehouseCode})`;

    let badgeClass = "bg-secondary";
    if (t.status === "PENDING_APPROVAL") badgeClass = "bg-warning text-dark";
    else if (t.status === "APPROVED") badgeClass = "bg-info text-dark";
    else if (t.status === "DISPATCHED") badgeClass = "bg-primary";
    else if (t.status === "PARTIALLY_RECEIVED") badgeClass = "bg-warning text-dark";
    else if (t.status === "FULLY_RECEIVED") badgeClass = "bg-success";
    else if (t.status === "CANCELLED" || t.status === "REJECTED") badgeClass = "bg-danger";

    document.getElementById("viewStatusBadge").innerHTML = `<span class="badge ${badgeClass}">${t.status}</span>`;
    document.getElementById("viewCreatedDate").textContent = t.createdAt ? t.createdAt.replace("T", " ").substring(0, 16) : "N/A";
    document.getElementById("viewRequestedBy").textContent = t.requestedBy || "N/A";
    document.getElementById("viewApprovedBy").textContent = t.approvedBy || "None";

    const tbody = document.getElementById("viewItemsTableBody");
    tbody.innerHTML = (t.items || []).map(i => `
        <tr>
            <td><strong>${escapeHtml(i.fertilizerName)}</strong></td>
            <td><code>${escapeHtml(i.batchNumber)}</code></td>
            <td>${escapeHtml(i.sourceLocationCode || 'N/A')}</td>
            <td><span class="badge bg-light text-dark border">${i.requestedQuantity}</span></td>
            <td><span class="badge bg-primary">${i.dispatchedQuantity || 0}</span></td>
            <td><span class="badge bg-success">${i.receivedQuantity || 0}</span></td>
        </tr>
    `).join("");

    transferDetailsModalInstance.show();
}

function escapeHtml(str) {
    if (!str) return "";
    return String(str).replace(/[&<>"']/g, m => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    }[m]));
}
