const API_BASE = (typeof CONFIG !== 'undefined' && CONFIG.API_BASE_URL) || (typeof API_URL !== 'undefined' && API_URL) || "http://localhost:8080/api";

let currentOrders = [];
let currentPickLists = [];
let currentPackages = [];
let currentShipments = [];
let currentPartners = [];

let activeTab = "orders";

document.addEventListener("DOMContentLoaded", () => {
    checkAdminAuth();
    loadDashboardMetrics();
    loadFulfillmentOrders();
    loadDeliveryPartnersDropdown();

    // Tab switch listeners
    const tabEls = document.querySelectorAll('button[data-bs-toggle="tab"]');
    tabEls.forEach(tab => {
        tab.addEventListener('shown.bs.tab', (e) => {
            const targetId = e.target.getAttribute('data-bs-target');
            if (targetId === '#orders-pane') {
                activeTab = "orders";
                loadFulfillmentOrders();
            } else if (targetId === '#picklists-pane') {
                activeTab = "picklists";
                loadPickLists();
            } else if (targetId === '#packages-pane') {
                activeTab = "packages";
                loadPackages();
            } else if (targetId === '#shipments-pane') {
                activeTab = "shipments";
                loadShipments();
            } else if (targetId === '#partners-pane') {
                activeTab = "partners";
                loadDeliveryPartners();
            }
        });
    });
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
        window.location.href = window.location.pathname.includes('/admin/') ? "../HTML/index.html" : "index.html";
        return;
    }
    const userBadge = document.getElementById("adminUserBadge");
    if (userBadge && username) {
        userBadge.textContent = username;
    }
}

function getAuthHeaders() {
    const token = localStorage.getItem("token") || localStorage.getItem("smartagriAuthToken") || localStorage.getItem("smartagri_auth_token");
    return {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
    };
}

function refreshCurrentTab() {
    loadDashboardMetrics();
    if (activeTab === "orders") loadFulfillmentOrders();
    else if (activeTab === "picklists") loadPickLists();
    else if (activeTab === "packages") loadPackages();
    else if (activeTab === "shipments") loadShipments();
    else if (activeTab === "partners") loadDeliveryPartners();
}

// ----------------------------------------------------
// KPI & DASHBOARD METRICS
// ----------------------------------------------------
async function loadDashboardMetrics() {
    try {
        const res = await fetch(`${API_BASE}/admin/fulfillment/dashboard`, { headers: getAuthHeaders() });
        if (!res.ok) return;
        const resData = await res.json();
        const data = resData.data || {};
        
        document.getElementById("kpiAwaiting").textContent = data.awaitingAllocationCount || 0;
        document.getElementById("kpiPickingPacking").textContent = data.pickingPackingCount || 0;
        document.getElementById("kpiInTransit").textContent = data.inTransitOutForDeliveryCount || 0;
        document.getElementById("kpiDelivered").textContent = data.deliveredCount || 0;
    } catch (e) {
        console.error("Failed to load dashboard metrics", e);
    }
}

// ----------------------------------------------------
// 1. FULFILLMENT ORDERS
// ----------------------------------------------------
async function loadFulfillmentOrders() {
    const tbody = document.getElementById("ordersTableBody");
    tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div>Loading orders...</td></tr>`;

    const statusFilter = document.getElementById("orderStatusFilter").value;
    let url = `${API_BASE}/admin/fulfillment/orders`;
    if (statusFilter && statusFilter !== "ALL") {
        url += `?status=${encodeURIComponent(statusFilter)}`;
    }

    try {
        const res = await fetch(url, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const json = await res.json();
        currentOrders = json.data || [];
        renderOrders(currentOrders);
    } catch (err) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-danger"><i class="bi bi-exclamation-triangle me-2"></i>Failed to load orders: ${err.message}</td></tr>`;
    }
}

function renderOrders(orders) {
    const tbody = document.getElementById("ordersTableBody");
    if (!orders || orders.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted">No fulfillment orders found.</td></tr>`;
        return;
    }

    tbody.innerHTML = orders.map(ord => {
        const fStatus = ord.fulfillmentStatus || 'UNALLOCATED';
        const fBadge = getFulfillmentStatusBadge(fStatus);
        const orderDate = ord.orderDate ? new Date(ord.orderDate).toLocaleDateString() : 'N/A';
        const total = ord.totalAmount ? `₹${Number(ord.totalAmount).toFixed(2)}` : '₹0.00';
        const address = ord.shippingAddress ? `${ord.shippingAddress.city || ''}, ${ord.shippingAddress.state || ''}` : 'N/A';

        let actionBtns = `
            <button class="btn btn-sm btn-outline-info me-1" onclick="viewOrderDetails(${ord.id})" title="View Details">
                <i class="bi bi-eye"></i>
            </button>
        `;

        if (fStatus === 'UNALLOCATED') {
            actionBtns += `
                <button class="btn btn-sm btn-success me-1" onclick="allocateOrder(${ord.id})" title="Allocate Inventory">
                    <i class="bi bi-check2-square me-1"></i>Allocate
                </button>
            `;
        } else if (fStatus === 'ALLOCATED') {
            actionBtns += `
                <button class="btn btn-sm btn-warning text-dark me-1" onclick="createPickListForOrder(${ord.id})" title="Create Pick List">
                    <i class="bi bi-card-checklist me-1"></i>Pick List
                </button>
            `;
        } else if (fStatus === 'PICKED') {
            actionBtns += `
                <button class="btn btn-sm btn-primary me-1" onclick="createPackageForOrder(${ord.id})" title="Pack Order">
                    <i class="bi bi-box me-1"></i>Pack
                </button>
            `;
        } else if (fStatus === 'PACKED' || fStatus === 'READY_TO_SHIP') {
            actionBtns += `
                <button class="btn btn-sm btn-dark me-1" onclick="openCreateShipmentModalForOrder(${ord.id})" title="Create Shipment">
                    <i class="bi bi-truck me-1"></i>Ship
                </button>
            `;
        }

        return `
            <tr>
                <td class="fw-bold">#${ord.id}</td>
                <td>${orderDate}</td>
                <td>${ord.customerName || (ord.user ? ord.user.name : 'Customer')}</td>
                <td>${address}</td>
                <td><span class="badge bg-secondary-subtle text-secondary">${ord.status || 'PLACED'}</span></td>
                <td>${fBadge}</td>
                <td class="fw-semibold text-success">${total}</td>
                <td class="text-end text-nowrap">${actionBtns}</td>
            </tr>
        `;
    }).join("");
}

function searchOrders() {
    const q = document.getElementById("orderSearchInput").value.toLowerCase();
    if (!q) {
        renderOrders(currentOrders);
        return;
    }
    const filtered = currentOrders.filter(o => 
        String(o.id).includes(q) ||
        (o.customerName && o.customerName.toLowerCase().includes(q)) ||
        (o.shippingAddress && o.shippingAddress.city && o.shippingAddress.city.toLowerCase().includes(q))
    );
    renderOrders(filtered);
}

function getFulfillmentStatusBadge(status) {
    switch (status) {
        case 'UNALLOCATED': return `<span class="badge bg-secondary">Unallocated</span>`;
        case 'ALLOCATED': return `<span class="badge bg-info text-dark">Allocated</span>`;
        case 'PICKING': return `<span class="badge bg-warning text-dark">Picking</span>`;
        case 'PICKED': return `<span class="badge bg-warning text-dark">Picked</span>`;
        case 'PACKING': return `<span class="badge bg-primary">Packing</span>`;
        case 'PACKED': return `<span class="badge bg-primary">Packed</span>`;
        case 'READY_TO_SHIP': return `<span class="badge bg-dark">Ready to Ship</span>`;
        case 'SHIPPED': return `<span class="badge bg-indigo text-light" style="background:#6366f1;">Shipped</span>`;
        case 'IN_TRANSIT': return `<span class="badge bg-info text-dark">In Transit</span>`;
        case 'OUT_FOR_DELIVERY': return `<span class="badge bg-warning text-dark">Out for Delivery</span>`;
        case 'DELIVERED': return `<span class="badge bg-success">Delivered</span>`;
        case 'DELIVERY_FAILED': return `<span class="badge bg-danger">Delivery Failed</span>`;
        case 'RETURN_TO_ORIGIN': return `<span class="badge bg-danger text-light">RTO</span>`;
        case 'RETURNED': return `<span class="badge bg-secondary">Returned</span>`;
        case 'CANCELLED': return `<span class="badge bg-dark">Cancelled</span>`;
        default: return `<span class="badge bg-secondary">${status}</span>`;
    }
}

// ----------------------------------------------------
// ORDER ACTIONS (Allocate, Details, PickList, etc.)
// ----------------------------------------------------
async function allocateOrder(orderId) {
    if (!confirm(`Allocate warehouse inventory for Order #${orderId}?`)) return;
    try {
        const res = await fetch(`${API_BASE}/admin/fulfillment/orders/${orderId}/allocate`, {
            method: "POST",
            headers: getAuthHeaders()
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to allocate order");
        alert(json.message || `Order #${orderId} allocated successfully!`);
        loadDashboardMetrics();
        loadFulfillmentOrders();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function viewOrderDetails(orderId) {
    const modalEl = document.getElementById("detailModal");
    const modal = bootstrap.Modal.getOrCreateInstance(modalEl);
    const body = document.getElementById("detailModalBody");
    document.getElementById("detailModalTitle").textContent = `Order #${orderId} Fulfillment Details`;
    body.innerHTML = `<div class="text-center py-4"><div class="spinner-border text-primary"></div><p class="mt-2 text-muted">Loading fulfillment details...</p></div>`;
    modal.show();

    try {
        const res = await fetch(`${API_BASE}/admin/fulfillment/orders/${orderId}`, { headers: getAuthHeaders() });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to load order details");
        const d = json.data;

        let allocationsHtml = `<p class="text-muted small">No warehouse allocations yet.</p>`;
        if (d.allocations && d.allocations.length > 0) {
            allocationsHtml = `
                <div class="table-responsive mb-3">
                    <table class="table table-sm table-bordered align-middle">
                        <thead class="table-light">
                            <tr>
                                <th>Product</th>
                                <th>Warehouse</th>
                                <th>Bin/Location</th>
                                <th>Batch #</th>
                                <th>Allocated</th>
                                <th>Picked</th>
                                <th>Packed</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${d.allocations.map(a => `
                                <tr>
                                    <td class="fw-semibold">${a.productName || 'Fertilizer'}</td>
                                    <td>${a.warehouseName || a.warehouseCode || 'Main'}</td>
                                    <td>${a.locationCode || 'General'}</td>
                                    <td><span class="badge bg-light text-dark border">${a.batchNumber || 'N/A'}</span></td>
                                    <td>${a.allocatedQuantity}</td>
                                    <td>${a.pickedQuantity}</td>
                                    <td>${a.packedQuantity}</td>
                                </tr>
                            `).join("")}
                        </tbody>
                    </table>
                </div>
            `;
        }

        let pickListsHtml = `<p class="text-muted small">No pick lists generated.</p>`;
        if (d.pickLists && d.pickLists.length > 0) {
            pickListsHtml = `
                <div class="list-group mb-3">
                    ${d.pickLists.map(pl => `
                        <div class="list-group-item d-flex justify-content-between align-items-center">
                            <div>
                                <h6 class="mb-0 fw-bold">Pick List #${pl.pickListNumber || pl.id}</h6>
                                <small class="text-muted">Warehouse: ${pl.warehouseName || pl.warehouseCode} | Picker: ${pl.assignedPicker || 'Unassigned'}</small>
                            </div>
                            <div>
                                <span class="badge bg-secondary me-2">${pl.status}</span>
                                ${pl.status === 'CREATED' ? `<button class="btn btn-sm btn-primary" onclick="startPicking(${pl.id})">Start Pick</button>` : ''}
                                ${pl.status === 'PICKING' ? `<button class="btn btn-sm btn-success" onclick="confirmPick(${pl.id})">Confirm Pick</button>` : ''}
                            </div>
                        </div>
                    `).join("")}
                </div>
            `;
        }

        let packagesHtml = `<p class="text-muted small">No packaging records.</p>`;
        if (d.packages && d.packages.length > 0) {
            packagesHtml = `
                <div class="table-responsive mb-3">
                    <table class="table table-sm table-bordered align-middle">
                        <thead class="table-light">
                            <tr>
                                <th>Package #</th>
                                <th>Dims (L×W×H cm)</th>
                                <th>Actual Wt (kg)</th>
                                <th>Volumetric (kg)</th>
                                <th>Chargeable (kg)</th>
                                <th>Status</th>
                                <th>Action</th>
                            </tr>
                        </thead>
                        <tbody>
                            ${d.packages.map(p => `
                                <tr>
                                    <td class="fw-bold">${p.packageNumber || p.id}</td>
                                    <td>${p.lengthCm || 0} × ${p.widthCm || 0} × ${p.heightCm || 0}</td>
                                    <td>${p.actualWeightKg || 0}</td>
                                    <td>${p.volumetricWeightKg || 0}</td>
                                    <td class="fw-bold text-success">${p.chargeableWeightKg || 0}</td>
                                    <td><span class="badge bg-primary-subtle text-primary">${p.status}</span></td>
                                    <td>
                                        ${p.status === 'PACKING' ? `<button class="btn btn-xs btn-outline-success" onclick="finalizePackage(${p.id})">Finalize Pack</button>` : ''}
                                    </td>
                                </tr>
                            `).join("")}
                        </tbody>
                    </table>
                </div>
            `;
        }

        let shipmentsHtml = `<p class="text-muted small">No shipments created.</p>`;
        if (d.shipments && d.shipments.length > 0) {
            shipmentsHtml = `
                <div class="list-group mb-3">
                    ${d.shipments.map(s => `
                        <div class="list-group-item">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <div>
                                    <h6 class="mb-0 fw-bold">Shipment #${s.shipmentNumber || s.id} <span class="badge bg-secondary ms-2">${s.status}</span></h6>
                                    <small class="text-muted">Partner: <strong>${s.deliveryPartnerName || s.deliveryPartnerCode || 'N/A'}</strong> | Tracking: <span class="badge bg-dark">${s.trackingNumber || 'N/A'}</span></small>
                                </div>
                                <div class="btn-group btn-group-sm">
                                    ${s.status === 'READY_FOR_DISPATCH' ? `<button class="btn btn-sm btn-dark" onclick="dispatchShipment(${s.id})">Dispatch</button>` : ''}
                                    ${s.status === 'DISPATCHED' || s.status === 'IN_TRANSIT' ? `<button class="btn btn-sm btn-warning text-dark" onclick="markOutForDelivery(${s.id})">Out For Delivery</button>` : ''}
                                    ${s.status === 'OUT_FOR_DELIVERY' ? `<button class="btn btn-sm btn-success" onclick="openDeliverModal(${s.id})">Mark Delivered</button>` : ''}
                                    ${s.status === 'OUT_FOR_DELIVERY' ? `<button class="btn btn-sm btn-outline-danger" onclick="openFailModal(${s.id})">Fail</button>` : ''}
                                    ${s.status === 'DELIVERY_FAILED' ? `<button class="btn btn-sm btn-danger" onclick="initiateRto(${s.id})">Return to Origin</button>` : ''}
                                    ${s.status === 'RETURN_TO_ORIGIN' ? `<button class="btn btn-sm btn-warning text-dark" onclick="receiveReturnInventory(${s.id})">Receive Return In Stock</button>` : ''}
                                </div>
                            </div>
                            ${renderTrackingEventsTimeline(s.events)}
                        </div>
                    `).join("")}
                </div>
            `;
        }

        body.innerHTML = `
            <div class="row g-3">
                <div class="col-md-6">
                    <div class="card p-3 border-0 bg-light">
                        <h6 class="fw-bold mb-2">Customer & Destination</h6>
                        <div><strong>Customer:</strong> ${d.order.customerName || 'N/A'}</div>
                        <div><strong>Email:</strong> ${d.order.customerEmail || 'N/A'}</div>
                        <div><strong>Phone:</strong> ${d.order.customerPhone || 'N/A'}</div>
                        <div><strong>Address:</strong> ${d.order.shippingAddress ? `${d.order.shippingAddress.street || ''}, ${d.order.shippingAddress.city || ''}, ${d.order.shippingAddress.state || ''} - ${d.order.shippingAddress.pincode || ''}` : 'N/A'}</div>
                    </div>
                </div>
                <div class="col-md-6">
                    <div class="card p-3 border-0 bg-light">
                        <h6 class="fw-bold mb-2">Fulfillment Summary</h6>
                        <div><strong>Order Status:</strong> <span class="badge bg-secondary">${d.order.status}</span></div>
                        <div><strong>Fulfillment Status:</strong> ${getFulfillmentStatusBadge(d.order.fulfillmentStatus)}</div>
                        <div><strong>Order Total:</strong> ₹${Number(d.order.totalAmount || 0).toFixed(2)}</div>
                        <div><strong>Payment Method:</strong> ${d.order.paymentMethod || 'COD'}</div>
                    </div>
                </div>
            </div>

            <hr>
            <h6 class="fw-bold"><i class="bi bi-layers me-2 text-primary"></i>1. Warehouse Allocations</h6>
            ${allocationsHtml}

            <hr>
            <h6 class="fw-bold"><i class="bi bi-card-checklist me-2 text-warning"></i>2. Pick Lists</h6>
            ${pickListsHtml}

            <hr>
            <h6 class="fw-bold"><i class="bi bi-box me-2 text-info"></i>3. Packaging & Dimensions</h6>
            ${packagesHtml}

            <hr>
            <h6 class="fw-bold"><i class="bi bi-truck me-2 text-success"></i>4. Shipments & Timeline</h6>
            ${shipmentsHtml}
        `;

    } catch (e) {
        body.innerHTML = `<div class="alert alert-danger"><i class="bi bi-exclamation-triangle me-2"></i>${e.message}</div>`;
    }
}

function renderTrackingEventsTimeline(events) {
    if (!events || events.length === 0) {
        return `<small class="text-muted d-block mt-2">No tracking events recorded yet.</small>`;
    }

    return `
        <div class="mt-3 p-3 bg-white rounded border">
            <h6 class="small fw-bold text-muted text-uppercase mb-2">Shipment Tracking History</h6>
            <div class="timeline-container ps-2">
                ${events.map(ev => `
                    <div class="timeline-event-item">
                        <div class="timeline-event-dot"></div>
                        <div class="fw-semibold small">${ev.status} - <span class="text-muted">${ev.location || 'Facility'}</span></div>
                        <div class="text-muted small">${ev.description || ''}</div>
                        <div class="text-secondary" style="font-size:0.75rem">${ev.timestamp ? new Date(ev.timestamp).toLocaleString() : ''}</div>
                    </div>
                `).join("")}
            </div>
        </div>
    `;
}

// ----------------------------------------------------
// 2. PICK LISTS
// ----------------------------------------------------
async function loadPickLists() {
    const tbody = document.getElementById("pickListsTableBody");
    tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div>Loading pick lists...</td></tr>`;

    try {
        const res = await fetch(`${API_BASE}/admin/picklists`, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const json = await res.json();
        currentPickLists = json.data || [];
        renderPickLists(currentPickLists);
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-danger"><i class="bi bi-exclamation-triangle me-2"></i>Failed to load pick lists: ${e.message}</td></tr>`;
    }
}

function renderPickLists(lists) {
    const tbody = document.getElementById("pickListsTableBody");
    if (!lists || lists.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted">No pick lists created.</td></tr>`;
        return;
    }

    tbody.innerHTML = lists.map(pl => {
        let actionBtns = ``;
        if (pl.status === 'CREATED') {
            actionBtns += `<button class="btn btn-sm btn-primary me-1" onclick="startPicking(${pl.id})"><i class="bi bi-play me-1"></i>Start Pick</button>`;
        } else if (pl.status === 'PICKING') {
            actionBtns += `<button class="btn btn-sm btn-success me-1" onclick="confirmPick(${pl.id})"><i class="bi bi-check2-circle me-1"></i>Confirm Pick</button>`;
        }

        const dateStr = pl.createdAt ? new Date(pl.createdAt).toLocaleDateString() : 'N/A';
        const itemCount = pl.items ? pl.items.length : 0;

        return `
            <tr>
                <td class="fw-bold">${pl.pickListNumber || '#' + pl.id}</td>
                <td><a href="javascript:void(0)" onclick="viewOrderDetails(${pl.orderId})">#${pl.orderId}</a></td>
                <td>${pl.warehouseName || pl.warehouseCode || 'Main'}</td>
                <td><span class="badge bg-light text-dark border">${pl.assignedPicker || 'Unassigned'}</span></td>
                <td><span class="badge bg-secondary-subtle text-secondary">${pl.status}</span></td>
                <td>${itemCount} line(s)</td>
                <td>${dateStr}</td>
                <td class="text-end text-nowrap">${actionBtns}</td>
            </tr>
        `;
    }).join("");
}

async function createPickListForOrder(orderId) {
    const picker = prompt(`Enter picker name or warehouse staff for Order #${orderId}:`, "Staff Warehouse Picker");
    if (!picker) return;

    try {
        const res = await fetch(`${API_BASE}/admin/picklists`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({ orderId: orderId, assignedPicker: picker })
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to create pick list");
        alert(json.message || "Pick list created successfully!");
        loadDashboardMetrics();
        loadFulfillmentOrders();
        loadPickLists();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function startPicking(pickListId) {
    try {
        const res = await fetch(`${API_BASE}/admin/picklists/${pickListId}/start`, {
            method: "POST",
            headers: getAuthHeaders()
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to start picking");
        alert("Picking started!");
        refreshCurrentTab();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function confirmPick(pickListId) {
    if (!confirm("Confirm all items in pick list have been verified and picked from bins?")) return;
    try {
        const res = await fetch(`${API_BASE}/admin/picklists/${pickListId}/confirm`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({})
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to confirm pick");
        alert("Pick confirmed successfully!");
        refreshCurrentTab();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

// ----------------------------------------------------
// 3. PACKAGES
// ----------------------------------------------------
async function loadPackages() {
    const tbody = document.getElementById("packagesTableBody");
    tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div>Loading packages...</td></tr>`;

    try {
        const res = await fetch(`${API_BASE}/admin/packages`, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const json = await res.json();
        currentPackages = json.data || [];
        renderPackages(currentPackages);
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-danger"><i class="bi bi-exclamation-triangle me-2"></i>Failed to load packages: ${e.message}</td></tr>`;
    }
}

function renderPackages(pkgs) {
    const tbody = document.getElementById("packagesTableBody");
    if (!pkgs || pkgs.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted">No packages found.</td></tr>`;
        return;
    }

    tbody.innerHTML = pkgs.map(p => {
        let actionBtns = ``;
        if (p.status === 'PACKING') {
            actionBtns += `<button class="btn btn-sm btn-success me-1" onclick="finalizePackage(${p.id})"><i class="bi bi-check2 me-1"></i>Finalize (Pack)</button>`;
        }

        return `
            <tr>
                <td class="fw-bold">${p.packageNumber || '#' + p.id}</td>
                <td><a href="javascript:void(0)" onclick="viewOrderDetails(${p.orderId})">#${p.orderId}</a></td>
                <td>${p.warehouseName || p.warehouseCode || 'Main'}</td>
                <td>${p.lengthCm || 0} × ${p.widthCm || 0} × ${p.heightCm || 0}</td>
                <td>${p.actualWeightKg || 0}</td>
                <td class="fw-bold text-success">${p.chargeableWeightKg || 0}</td>
                <td><span class="badge bg-info-subtle text-info">${p.status}</span></td>
                <td class="text-end text-nowrap">${actionBtns}</td>
            </tr>
        `;
    }).join("");
}

async function createPackageForOrder(orderId) {
    const lengthCm = prompt("Enter package Length in cm:", "30");
    if (!lengthCm) return;
    const widthCm = prompt("Enter package Width in cm:", "20");
    if (!widthCm) return;
    const heightCm = prompt("Enter package Height in cm:", "15");
    if (!heightCm) return;
    const weightKg = prompt("Enter Actual Weight in kg:", "5.0");
    if (!weightKg) return;

    try {
        const res = await fetch(`${API_BASE}/admin/packages`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({
                orderId: orderId,
                lengthCm: parseFloat(lengthCm),
                widthCm: parseFloat(widthCm),
                heightCm: parseFloat(heightCm),
                actualWeightKg: parseFloat(weightKg),
                packageType: "BOX"
            })
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to create package");
        alert("Package created! Volumetric and chargeable weight calculated.");
        refreshCurrentTab();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function finalizePackage(pkgId) {
    try {
        const res = await fetch(`${API_BASE}/admin/packages/${pkgId}/finalize`, {
            method: "POST",
            headers: getAuthHeaders()
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to finalize package");
        alert("Package finalized! Order is ready to ship.");
        refreshCurrentTab();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

// ----------------------------------------------------
// 4. SHIPMENTS
// ----------------------------------------------------
async function loadShipments() {
    const tbody = document.getElementById("shipmentsTableBody");
    tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div>Loading shipments...</td></tr>`;

    const statusFilter = document.getElementById("shipmentStatusFilter").value;
    let url = `${API_BASE}/admin/shipments`;
    if (statusFilter && statusFilter !== "ALL") {
        url += `?status=${encodeURIComponent(statusFilter)}`;
    }

    try {
        const res = await fetch(url, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const json = await res.json();
        currentShipments = json.data || [];
        renderShipments(currentShipments);
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-danger"><i class="bi bi-exclamation-triangle me-2"></i>Failed to load shipments: ${e.message}</td></tr>`;
    }
}

function renderShipments(shipments) {
    const tbody = document.getElementById("shipmentsTableBody");
    if (!shipments || shipments.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" class="text-center py-4 text-muted">No shipments found.</td></tr>`;
        return;
    }

    tbody.innerHTML = shipments.map(s => {
        const expectedDate = s.expectedDeliveryDate || 'N/A';
        const dispatchedDate = s.dispatchedAt ? new Date(s.dispatchedAt).toLocaleString() : 'Not yet';

        let actionBtns = ``;
        if (s.status === 'READY_FOR_DISPATCH') {
            actionBtns += `<button class="btn btn-sm btn-dark me-1" onclick="dispatchShipment(${s.id})"><i class="bi bi-send me-1"></i>Dispatch</button>`;
        } else if (s.status === 'DISPATCHED' || s.status === 'IN_TRANSIT') {
            actionBtns += `<button class="btn btn-sm btn-warning text-dark me-1" onclick="markOutForDelivery(${s.id})"><i class="bi bi-bicycle me-1"></i>Out For Delivery</button>`;
        } else if (s.status === 'OUT_FOR_DELIVERY') {
            actionBtns += `<button class="btn btn-sm btn-success me-1" onclick="openDeliverModal(${s.id})"><i class="bi bi-check2-all me-1"></i>Delivered</button>`;
            actionBtns += `<button class="btn btn-sm btn-outline-danger me-1" onclick="openFailModal(${s.id})"><i class="bi bi-x-circle me-1"></i>Failed</button>`;
        } else if (s.status === 'DELIVERY_FAILED') {
            actionBtns += `<button class="btn btn-sm btn-danger me-1" onclick="initiateRto(${s.id})"><i class="bi bi-arrow-return-left me-1"></i>RTO</button>`;
        } else if (s.status === 'RETURN_TO_ORIGIN') {
            actionBtns += `<button class="btn btn-sm btn-warning text-dark me-1" onclick="receiveReturnInventory(${s.id})"><i class="bi bi-box-arrow-in-down me-1"></i>Restock Bin</button>`;
        }

        return `
            <tr>
                <td class="fw-bold">${s.shipmentNumber || '#' + s.id}</td>
                <td><a href="javascript:void(0)" onclick="viewOrderDetails(${s.orderId})">#${s.orderId}</a></td>
                <td><span class="badge bg-light text-dark border">${s.deliveryPartnerName || s.deliveryPartnerCode || 'Partner'}</span></td>
                <td><span class="font-monospace fw-semibold">${s.trackingNumber || 'N/A'}</span></td>
                <td>${expectedDate}</td>
                <td><span class="badge bg-secondary-subtle text-secondary">${s.status}</span></td>
                <td>${dispatchedDate}</td>
                <td class="text-end text-nowrap">${actionBtns}</td>
            </tr>
        `;
    }).join("");
}

function openCreateShipmentModalForOrder(orderId) {
    document.getElementById("shpOrderId").value = orderId;
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById("createShipmentModal"));
    modal.show();
}

function openCreateShipmentModal() {
    document.getElementById("shpOrderId").value = "";
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById("createShipmentModal"));
    modal.show();
}

async function handleCreateShipment(e) {
    e.preventDefault();
    const orderId = document.getElementById("shpOrderId").value;
    const partnerId = document.getElementById("shpPartnerSelect").value;
    const trackingNumber = document.getElementById("shpTrackingNumber").value.trim();
    const expectedDate = document.getElementById("shpExpectedDate").value;
    const cost = document.getElementById("shpCost").value;
    const notes = document.getElementById("shpNotes").value;

    try {
        const res = await fetch(`${API_BASE}/admin/shipments`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({
                orderId: parseInt(orderId),
                deliveryPartnerId: parseInt(partnerId),
                trackingNumber: trackingNumber || null,
                expectedDeliveryDate: expectedDate || null,
                shippingCost: parseFloat(cost) || 0,
                notes: notes || null
            })
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to create shipment");
        alert("Shipment created successfully!");
        bootstrap.Modal.getInstance(document.getElementById("createShipmentModal")).hide();
        refreshCurrentTab();
    } catch (err) {
        alert("Error: " + err.message);
    }
}

async function dispatchShipment(shipmentId) {
    if (!confirm(`Dispatch shipment #${shipmentId}? This advances order to SHIPPED.`)) return;
    try {
        const res = await fetch(`${API_BASE}/admin/shipments/${shipmentId}/dispatch`, {
            method: "POST",
            headers: getAuthHeaders()
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to dispatch shipment");
        alert("Shipment dispatched!");
        refreshCurrentTab();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function markOutForDelivery(shipmentId) {
    try {
        const res = await fetch(`${API_BASE}/admin/shipments/${shipmentId}/out-for-delivery`, {
            method: "POST",
            headers: getAuthHeaders()
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to mark out for delivery");
        alert("Shipment is out for delivery!");
        refreshCurrentTab();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function openDeliverModal(shipmentId) {
    const receivedBy = prompt("Received by (Customer / Representative Name):", "Customer");
    if (!receivedBy) return;
    const otp = prompt("Delivery OTP / Verification Code (Optional):", "1234");

    try {
        const res = await fetch(`${API_BASE}/admin/shipments/${shipmentId}/deliver`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({
                receivedBy: receivedBy,
                otpEntered: otp || null,
                proofType: "OTP"
            })
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to confirm delivery");
        alert("Shipment marked DELIVERED! If COD, payment is auto-completed.");
        refreshCurrentTab();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function openFailModal(shipmentId) {
    const reason = prompt("Select Failure Reason:\nCUSTOMER_UNAVAILABLE\nINCORRECT_ADDRESS\nCUSTOMER_REJECTED\nPAYMENT_FAILED\nWEATHER_DELAY\nOTHER", "CUSTOMER_UNAVAILABLE");
    if (!reason) return;
    const notes = prompt("Enter failure details / remarks:", "Customer was not reachable at contact number");

    try {
        const res = await fetch(`${API_BASE}/admin/shipments/${shipmentId}/fail`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({
                reason: reason,
                remarks: notes
            })
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to record delivery failure");
        alert("Delivery failure recorded.");
        refreshCurrentTab();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function initiateRto(shipmentId) {
    const reason = prompt("Reason for Return to Origin (RTO):", "Multiple failed delivery attempts");
    if (!reason) return;

    try {
        const res = await fetch(`${API_BASE}/admin/shipments/${shipmentId}/rto`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({ reason: reason })
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to initiate RTO");
        alert("RTO initiated!");
        refreshCurrentTab();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

async function receiveReturnInventory(shipmentId) {
    if (!confirm(`Physically receive returned package for shipment #${shipmentId}? This will restock the inventory back to the exact warehouse, bin, and batch with an audited RETURN_IN transaction.`)) return;

    try {
        const res = await fetch(`${API_BASE}/admin/shipments/${shipmentId}/receive-return`, {
            method: "POST",
            headers: getAuthHeaders()
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to receive returned inventory");
        alert("Returned inventory successfully restocked back into warehouse location and batch!");
        refreshCurrentTab();
    } catch (e) {
        alert("Error: " + e.message);
    }
}

// ----------------------------------------------------
// 5. DELIVERY PARTNERS
// ----------------------------------------------------
async function loadDeliveryPartnersDropdown() {
    try {
        const res = await fetch(`${API_BASE}/admin/delivery-partners`, { headers: getAuthHeaders() });
        if (!res.ok) return;
        const json = await res.json();
        const partners = json.data || [];
        const select = document.getElementById("shpPartnerSelect");
        if (!select) return;
        select.innerHTML = `<option value="">Select Delivery Partner</option>` + partners.map(p => 
            `<option value="${p.id}">${p.partnerName} (${p.partnerCode})</option>`
        ).join("");
    } catch (e) {
        console.error("Failed to load delivery partner dropdown", e);
    }
}

async function loadDeliveryPartners() {
    const tbody = document.getElementById("partnersTableBody");
    tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted"><div class="spinner-border spinner-border-sm text-primary me-2"></div>Loading partners...</td></tr>`;

    try {
        const res = await fetch(`${API_BASE}/admin/delivery-partners`, { headers: getAuthHeaders() });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const json = await res.json();
        currentPartners = json.data || [];
        renderDeliveryPartners(currentPartners);
    } catch (e) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-danger"><i class="bi bi-exclamation-triangle me-2"></i>Failed to load partners: ${e.message}</td></tr>`;
    }
}

function renderDeliveryPartners(partners) {
    const tbody = document.getElementById("partnersTableBody");
    if (!partners || partners.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-4 text-muted">No delivery partners found.</td></tr>`;
        return;
    }

    tbody.innerHTML = partners.map(p => {
        return `
            <tr>
                <td class="fw-bold">${p.partnerCode}</td>
                <td>${p.partnerName}</td>
                <td>${p.contactNumber || 'N/A'}</td>
                <td><code>${p.trackingUrlTemplate || 'N/A'}</code></td>
                <td><span class="badge bg-success-subtle text-success">${p.status || 'ACTIVE'}</span></td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-secondary" onclick="alert('Partner details: ' + JSON.stringify(p))">
                        <i class="bi bi-info-circle"></i>
                    </button>
                </td>
            </tr>
        `;
    }).join("");
}

function openCreatePartnerModal() {
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById("createPartnerModal"));
    modal.show();
}

async function handleCreatePartner(e) {
    e.preventDefault();
    const code = document.getElementById("ptCode").value.trim();
    const name = document.getElementById("ptName").value.trim();
    const phone = document.getElementById("ptPhone").value.trim();
    const url = document.getElementById("ptUrl").value.trim();

    try {
        const res = await fetch(`${API_BASE}/admin/delivery-partners`, {
            method: "POST",
            headers: getAuthHeaders(),
            body: JSON.stringify({
                partnerCode: code,
                partnerName: name,
                contactNumber: phone || null,
                trackingUrlTemplate: url || null
            })
        });
        const json = await res.json();
        if (!res.ok) throw new Error(json.message || "Failed to create partner");
        alert("Partner added successfully!");
        bootstrap.Modal.getInstance(document.getElementById("createPartnerModal")).hide();
        loadDeliveryPartnersDropdown();
        loadDeliveryPartners();
    } catch (err) {
        alert("Error: " + err.message);
    }
}
