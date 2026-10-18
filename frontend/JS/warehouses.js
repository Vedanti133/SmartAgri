const API_BASE = (typeof CONFIG !== 'undefined' && CONFIG.API_BASE_URL) || (typeof API_URL !== 'undefined' && API_URL) || "http://localhost:8080/api";
let currentWarehouses = [];
let filteredWarehouses = [];
let currentWarehouseLocations = [];
let warehouseModalInstance = null;
let locationsModalInstance = null;
let createLocationModalInstance = null;
let locStockModalInstance = null;

document.addEventListener("DOMContentLoaded", () => {
    checkAdminAuth();
    warehouseModalInstance = new bootstrap.Modal(document.getElementById("warehouseModal"));
    locationsModalInstance = new bootstrap.Modal(document.getElementById("locationsModal"));
    createLocationModalInstance = new bootstrap.Modal(document.getElementById("createLocationModal"));
    locStockModalInstance = new bootstrap.Modal(document.getElementById("locStockModal"));

    loadWarehouses();
    loadDashboardKPIs();
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
// DASHBOARD & WAREHOUSE LISTING
// ----------------------------------------------------
async function loadDashboardKPIs() {
    try {
        const res = await fetch(`${API_BASE}/admin/warehouses/dashboard`, { headers: getAuthHeaders() });
        if (!res.ok) return;
        const data = await res.json();
        const d = data.data || {};
        document.getElementById("kpiTotalWarehouses").textContent = d.totalWarehouses || 0;
        document.getElementById("kpiActiveWarehouses").textContent = d.activeWarehouses || 0;
        document.getElementById("kpiTotalLocations").textContent = d.totalLocations || 0;
        const util = d.overallUtilizationPercentage != null ? d.overallUtilizationPercentage.toFixed(1) + "%" : "0%";
        document.getElementById("kpiStockUtilization").textContent = `${util} (${d.totalStockUnits || 0} u)`;
    } catch (e) {
        console.error("Failed to load dashboard KPIs:", e);
    }
}

async function loadWarehouses() {
    const tbody = document.getElementById("warehousesTableBody");
    tbody.innerHTML = `
        <tr>
            <td colspan="7" class="text-center py-4 text-muted">
                <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
                Loading warehouses...
            </td>
        </tr>
    `;

    try {
        const res = await fetch(`${API_BASE}/admin/warehouses`, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        currentWarehouses = data.data || [];
        filterWarehouses();
    } catch (err) {
        console.error("Error loading warehouses:", err);
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-4 text-danger">
                    <i class="bi bi-exclamation-triangle me-2"></i>Failed to load warehouses: ${err.message}
                </td>
            </tr>
        `;
    }
}

function filterWarehouses() {
    const search = (document.getElementById("searchWarehouseInput").value || "").toLowerCase().trim();
    const type = document.getElementById("filterTypeSelect").value;
    const status = document.getElementById("filterStatusSelect").value;

    filteredWarehouses = currentWarehouses.filter(w => {
        const matchSearch = !search ||
            (w.warehouseCode && w.warehouseCode.toLowerCase().includes(search)) ||
            (w.warehouseName && w.warehouseName.toLowerCase().includes(search)) ||
            (w.city && w.city.toLowerCase().includes(search));
        const matchType = !type || w.warehouseType === type;
        const matchStatus = !status || w.status === status;
        return matchSearch && matchType && matchStatus;
    });

    renderWarehousesTable();
}

function resetWarehouseFilters() {
    document.getElementById("searchWarehouseInput").value = "";
    document.getElementById("filterTypeSelect").value = "";
    document.getElementById("filterStatusSelect").value = "";
    filterWarehouses();
}

function renderWarehousesTable() {
    const tbody = document.getElementById("warehousesTableBody");
    const countEl = document.getElementById("warehouseTableCount");
    countEl.textContent = `${filteredWarehouses.length} Warehouse${filteredWarehouses.length === 1 ? '' : 's'}`;

    if (!filteredWarehouses.length) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" class="text-center py-4 text-muted">
                    No warehouses found matching current criteria.
                </td>
            </tr>
        `;
        return;
    }

    tbody.innerHTML = filteredWarehouses.map(w => {
        let statusBadge = "bg-secondary";
        if (w.status === "ACTIVE") statusBadge = "bg-success";
        else if (w.status === "INACTIVE") statusBadge = "bg-warning text-dark";
        else if (w.status === "BLOCKED") statusBadge = "bg-danger";

        const cap = w.capacity || 0;
        const util = w.currentUtilization || 0;
        const pct = cap > 0 ? Math.min(100, Math.round((util / cap) * 100)) : 0;
        let barColor = "bg-success";
        if (pct > 75) barColor = "bg-warning";
        if (pct >= 95) barColor = "bg-danger";

        const defaultBadge = w.isDefaultReceiving
            ? `<span class="badge bg-primary"><i class="bi bi-star-fill me-1"></i>Default</span>`
            : `<button class="btn btn-sm btn-link text-decoration-none p-0 text-muted" onclick="setDefaultWarehouse(${w.id})">Set as Default</button>`;

        return `
            <tr>
                <td>
                    <div class="fw-bold text-dark">${escapeHtml(w.warehouseName)}</div>
                    <code class="small text-primary">${escapeHtml(w.warehouseCode)}</code>
                </td>
                <td><span class="badge-type">${escapeHtml(w.warehouseType || 'STANDARD')}</span></td>
                <td><span class="badge ${statusBadge} status-badge">${w.status}</span></td>
                <td>
                    <div>${escapeHtml(w.city || 'N/A')}, ${escapeHtml(w.state || '')}</div>
                    <small class="text-muted">${escapeHtml(w.address || '')}</small>
                </td>
                <td style="min-width: 140px;">
                    <div class="d-flex justify-content-between small mb-1">
                        <span>${util.toLocaleString()} / ${cap > 0 ? cap.toLocaleString() : '∞'} u</span>
                        <span class="fw-semibold">${pct}%</span>
                    </div>
                    <div class="progress" style="height: 6px;">
                        <div class="progress-bar ${barColor}" style="width: ${pct}%"></div>
                    </div>
                </td>
                <td>${defaultBadge}</td>
                <td class="text-end">
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-info" title="Manage Locations / Bins" onclick="openLocationsModal(${w.id}, '${escapeHtml(w.warehouseName)}')">
                            <i class="bi bi-grid-3x3"></i> Bins
                        </button>
                        <button class="btn btn-outline-primary" title="Edit Warehouse" onclick="openEditWarehouseModal(${w.id})">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-outline-${w.status === 'ACTIVE' ? 'warning' : 'success'}" title="${w.status === 'ACTIVE' ? 'Deactivate' : 'Activate'}" onclick="toggleWarehouseStatus(${w.id}, '${w.status}')">
                            <i class="bi bi-power"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");
}

// ----------------------------------------------------
// CREATE / EDIT WAREHOUSE
// ----------------------------------------------------
function openCreateWarehouseModal() {
    document.getElementById("warehouseForm").reset();
    document.getElementById("editWarehouseId").value = "";
    document.getElementById("warehouseModalLabel").innerHTML = `<i class="bi bi-building me-2"></i>Create Warehouse`;
    warehouseModalInstance.show();
}

function openEditWarehouseModal(id) {
    const w = currentWarehouses.find(item => item.id === id);
    if (!w) return;

    document.getElementById("editWarehouseId").value = w.id;
    document.getElementById("whCode").value = w.warehouseCode || "";
    document.getElementById("whName").value = w.warehouseName || "";
    document.getElementById("whType").value = w.warehouseType || "REGIONAL";
    document.getElementById("whAddress").value = w.address || "";
    document.getElementById("whCity").value = w.city || "";
    document.getElementById("whState").value = w.state || "";
    document.getElementById("whPostalCode").value = w.postalCode || "";
    document.getElementById("whCapacity").value = w.capacity || "";
    document.getElementById("whContactPerson").value = w.contactPerson || "";
    document.getElementById("whContactPhone").value = w.contactPhone || "";
    document.getElementById("whEmail").value = w.email || "";
    document.getElementById("whDefaultReceiving").checked = !!w.isDefaultReceiving;
    document.getElementById("whNotes").value = w.notes || "";

    document.getElementById("warehouseModalLabel").innerHTML = `<i class="bi bi-pencil me-2"></i>Edit Warehouse`;
    warehouseModalInstance.show();
}

async function saveWarehouse() {
    const editId = document.getElementById("editWarehouseId").value;
    const body = {
        warehouseCode: document.getElementById("whCode").value.trim(),
        warehouseName: document.getElementById("whName").value.trim(),
        warehouseType: document.getElementById("whType").value,
        address: document.getElementById("whAddress").value.trim() || null,
        city: document.getElementById("whCity").value.trim() || null,
        state: document.getElementById("whState").value.trim() || null,
        postalCode: document.getElementById("whPostalCode").value.trim() || null,
        capacity: document.getElementById("whCapacity").value ? parseInt(document.getElementById("whCapacity").value) : null,
        contactPerson: document.getElementById("whContactPerson").value.trim() || null,
        contactPhone: document.getElementById("whContactPhone").value.trim() || null,
        email: document.getElementById("whEmail").value.trim() || null,
        isDefaultReceiving: document.getElementById("whDefaultReceiving").checked,
        notes: document.getElementById("whNotes").value.trim() || null
    };

    if (!body.warehouseCode || !body.warehouseName || !body.warehouseType) {
        alert("Warehouse Code, Name, and Type are required.");
        return;
    }

    try {
        const url = editId ? `${API_BASE}/admin/warehouses/${editId}` : `${API_BASE}/admin/warehouses`;
        const method = editId ? "PUT" : "POST";

        const res = await fetch(url, {
            method,
            headers: getAuthHeaders(),
            body: JSON.stringify(body)
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to save warehouse");

        warehouseModalInstance.hide();
        showAlert(`Warehouse ${body.warehouseCode} saved successfully!`, "success");
        loadWarehouses();
        loadDashboardKPIs();
    } catch (err) {
        alert(`Error: ${err.message}`);
    }
}

async function toggleWarehouseStatus(id, currentStatus) {
    const newStatus = currentStatus === "ACTIVE" ? "INACTIVE" : "ACTIVE";
    if (!confirm(`Are you sure you want to change status to ${newStatus}?`)) return;

    try {
        const res = await fetch(`${API_BASE}/admin/warehouses/${id}/status?status=${newStatus}`, {
            method: "PATCH",
            headers: getAuthHeaders()
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to change status");

        showAlert(`Warehouse status updated to ${newStatus}`, "success");
        loadWarehouses();
        loadDashboardKPIs();
    } catch (err) {
        alert(`Error: ${err.message}`);
    }
}

async function setDefaultWarehouse(id) {
    try {
        const res = await fetch(`${API_BASE}/admin/warehouses/${id}/default`, {
            method: "POST",
            headers: getAuthHeaders()
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to set default receiving warehouse");

        showAlert("Default receiving warehouse set successfully", "success");
        loadWarehouses();
    } catch (err) {
        alert(`Error: ${err.message}`);
    }
}

// ----------------------------------------------------
// LOCATIONS & BINS MANAGEMENT
// ----------------------------------------------------
async function openLocationsModal(warehouseId, warehouseName) {
    document.getElementById("locModalWhId").value = warehouseId;
    document.getElementById("locModalWhName").textContent = warehouseName;
    locationsModalInstance.show();
    loadLocations(warehouseId);
}

async function loadLocations(warehouseId) {
    const tbody = document.getElementById("locationsTableBody");
    tbody.innerHTML = `<tr><td colspan="7" class="text-center py-3 text-muted">Loading locations...</td></tr>`;

    try {
        const res = await fetch(`${API_BASE}/admin/warehouses/${warehouseId}/locations`, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        currentWarehouseLocations = data.data || [];
        renderLocationsTable();
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-3 text-danger">Error: ${err.message}</td></tr>`;
    }
}

function renderLocationsTable() {
    const tbody = document.getElementById("locationsTableBody");
    document.getElementById("locModalTotalCount").textContent = `${currentWarehouseLocations.length} Locations`;

    if (!currentWarehouseLocations.length) {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center py-3 text-muted">No locations or bins defined for this warehouse yet. Click "Add Location / Bin" to create one.</td></tr>`;
        return;
    }

    tbody.innerHTML = currentWarehouseLocations.map(loc => {
        let statusBadge = "bg-secondary";
        if (loc.status === "ACTIVE") statusBadge = "bg-success";
        else if (loc.status === "FULL") statusBadge = "bg-warning text-dark";
        else if (loc.status === "MAINTENANCE") statusBadge = "bg-info text-dark";
        else if (loc.status === "BLOCKED") statusBadge = "bg-danger";

        const cap = loc.capacity || 0;
        const util = loc.currentUtilization || 0;
        const pct = cap > 0 ? Math.min(100, Math.round((util / cap) * 100)) : 0;

        let flags = [];
        if (loc.isTemperatureControlled) flags.push(`<span class="badge bg-info text-dark" title="Temperature Controlled"><i class="bi bi-thermometer-half"></i> Temp</span>`);
        if (loc.isHazardousAllowed) flags.push(`<span class="badge bg-danger" title="Hazardous Permitted"><i class="bi bi-exclamation-triangle"></i> Haz</span>`);

        return `
            <tr>
                <td>
                    <div class="fw-bold">${escapeHtml(loc.locationCode)}</div>
                    <small class="text-muted">${escapeHtml(loc.locationName || '')}</small>
                </td>
                <td><span class="badge bg-light text-dark border">${loc.locationType}</span></td>
                <td>${loc.parentLocationCode ? `<code>${escapeHtml(loc.parentLocationCode)}</code>` : '<span class="text-muted small">None (Top)</span>'}</td>
                <td><span class="badge ${statusBadge}">${loc.status}</span></td>
                <td style="min-width: 120px;">
                    <div class="small">${util} / ${cap > 0 ? cap : '∞'} u (${pct}%)</div>
                    <div class="progress" style="height: 4px;">
                        <div class="progress-bar ${pct > 80 ? 'bg-danger' : 'bg-primary'}" style="width: ${pct}%"></div>
                    </div>
                </td>
                <td>${flags.join(" ") || '<span class="text-muted small">Standard</span>'}</td>
                <td class="text-end">
                    <div class="btn-group btn-group-sm">
                        <button class="btn btn-outline-primary" title="View Stock in Location" onclick="viewLocationStock(${loc.id}, '${escapeHtml(loc.locationCode)}')">
                            <i class="bi bi-boxes"></i> Stock
                        </button>
                        <button class="btn btn-outline-secondary" title="Edit Location" onclick="openEditLocationModal(${loc.id})">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-outline-${loc.status === 'ACTIVE' ? 'danger' : 'success'}" title="Toggle Blocked/Active" onclick="toggleLocationStatus(${loc.id}, '${loc.status}')">
                            <i class="bi bi-slash-circle"></i>
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");
}

function openCreateLocationModal() {
    document.getElementById("createLocationForm").reset();
    document.getElementById("editLocationId").value = "";
    document.getElementById("createLocationModalLabel").innerHTML = `<i class="bi bi-plus-circle me-2"></i>Add Location / Bin`;

    // Populate Parent Location dropdown
    const select = document.getElementById("locParentSelect");
    select.innerHTML = `<option value="">-- None (Top Level) --</option>`;
    currentWarehouseLocations.forEach(l => {
        select.innerHTML += `<option value="${l.id}">${escapeHtml(l.locationCode)} (${l.locationType})</option>`;
    });

    createLocationModalInstance.show();
}

function openEditLocationModal(id) {
    const loc = currentWarehouseLocations.find(l => l.id === id);
    if (!loc) return;

    document.getElementById("editLocationId").value = loc.id;
    document.getElementById("locCode").value = loc.locationCode || "";
    document.getElementById("locName").value = loc.locationName || "";
    document.getElementById("locType").value = loc.locationType || "BIN";
    document.getElementById("locCapacity").value = loc.capacity || "";
    document.getElementById("locMaxWeight").value = loc.maxWeightKg || "";
    document.getElementById("locTempControlled").checked = !!loc.isTemperatureControlled;
    document.getElementById("locHazardousAllowed").checked = !!loc.isHazardousAllowed;

    const select = document.getElementById("locParentSelect");
    select.innerHTML = `<option value="">-- None (Top Level) --</option>`;
    currentWarehouseLocations.filter(l => l.id !== id).forEach(l => {
        select.innerHTML += `<option value="${l.id}" ${loc.parentLocationId === l.id ? 'selected' : ''}>${escapeHtml(l.locationCode)} (${l.locationType})</option>`;
    });

    document.getElementById("createLocationModalLabel").innerHTML = `<i class="bi bi-pencil me-2"></i>Edit Location`;
    createLocationModalInstance.show();
}

async function saveLocation() {
    const whId = document.getElementById("locModalWhId").value;
    const editId = document.getElementById("editLocationId").value;

    const body = {
        locationCode: document.getElementById("locCode").value.trim(),
        locationName: document.getElementById("locName").value.trim() || null,
        locationType: document.getElementById("locType").value,
        parentLocationId: document.getElementById("locParentSelect").value ? parseInt(document.getElementById("locParentSelect").value) : null,
        capacity: document.getElementById("locCapacity").value ? parseInt(document.getElementById("locCapacity").value) : null,
        maxWeightKg: document.getElementById("locMaxWeight").value ? parseFloat(document.getElementById("locMaxWeight").value) : null,
        isTemperatureControlled: document.getElementById("locTempControlled").checked,
        isHazardousAllowed: document.getElementById("locHazardousAllowed").checked
    };

    if (!body.locationCode || !body.locationType) {
        alert("Location Code and Type are required.");
        return;
    }

    try {
        const url = editId ? `${API_BASE}/admin/locations/${editId}` : `${API_BASE}/admin/warehouses/${whId}/locations`;
        const method = editId ? "PUT" : "POST";

        const res = await fetch(url, {
            method,
            headers: getAuthHeaders(),
            body: JSON.stringify(body)
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to save location");

        createLocationModalInstance.hide();
        showAlert(`Location ${body.locationCode} saved successfully!`, "success");
        loadLocations(whId);
        loadDashboardKPIs();
    } catch (err) {
        alert(`Error: ${err.message}`);
    }
}

async function toggleLocationStatus(locationId, currentStatus) {
    const newStatus = currentStatus === "BLOCKED" ? "ACTIVE" : "BLOCKED";
    if (!confirm(`Are you sure you want to change location status to ${newStatus}?`)) return;

    try {
        const res = await fetch(`${API_BASE}/admin/locations/${locationId}/status?status=${newStatus}`, {
            method: "PATCH",
            headers: getAuthHeaders()
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.message || "Failed to update location status");

        const whId = document.getElementById("locModalWhId").value;
        loadLocations(whId);
        showAlert(`Location status updated to ${newStatus}`, "success");
    } catch (err) {
        alert(`Error: ${err.message}`);
    }
}

// ----------------------------------------------------
// LOCATION INVENTORY VIEW
// ----------------------------------------------------
async function viewLocationStock(locationId, locationCode) {
    document.getElementById("locStockModalTitle").textContent = locationCode;
    const tbody = document.getElementById("locStockTableBody");
    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-3 text-muted">Loading stock items...</td></tr>`;
    locStockModalInstance.show();

    try {
        const res = await fetch(`${API_BASE}/admin/locations/${locationId}/stock`, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        const items = data.data || [];

        if (!items.length) {
            tbody.innerHTML = `<tr><td colspan="6" class="text-center py-3 text-muted">No stock currently stored in this location.</td></tr>`;
            return;
        }

        tbody.innerHTML = items.map(item => `
            <tr>
                <td><code>${escapeHtml(item.batchNumber)}</code></td>
                <td><div class="fw-semibold">${escapeHtml(item.fertilizerName)}</div></td>
                <td><span class="fw-bold">${item.quantity}</span></td>
                <td><span class="text-warning">${item.reservedQuantity || 0}</span></td>
                <td><span class="text-success fw-bold">${item.availableQuantity}</span></td>
                <td><span class="small ${item.expired ? 'text-danger fw-bold' : 'text-muted'}">${item.expiryDate || 'N/A'}</span></td>
            </tr>
        `).join("");
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-3 text-danger">Error: ${err.message}</td></tr>`;
    }
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
