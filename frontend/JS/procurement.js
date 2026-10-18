const API_BASE =
    (typeof CONFIG !== 'undefined' && CONFIG.API_BASE_URL) ||
    (typeof API_URL !== 'undefined' && API_URL) ||
    "http://localhost:8080/api";

let currentSuppliers = [];
let currentFertilizers = [];
let activeGrnInspectData = null;


/* =========================================================
   INITIALIZATION
========================================================= */

document.addEventListener("DOMContentLoaded", () => {
    setupProcurementSidebar();
    checkAdminAuth();
    loadDashboard();
    loadSuppliers();
    loadFertilizersList();
    loadSuppliersForDropdown();
});


/* =========================================================
   RESPONSIVE SIDEBAR
========================================================= */

function setupProcurementSidebar() {
    const sidebar = document.getElementById("side");
    const toggle = document.getElementById("sidebarToggle");

    if (!sidebar || !toggle) return;

    toggle.addEventListener("click", () => {
        sidebar.classList.toggle("show");
    });

    document.addEventListener("click", (event) => {

        if (
            window.innerWidth > 991 ||
            !sidebar.classList.contains("show")
        ) {
            return;
        }

        if (
            sidebar.contains(event.target) ||
            toggle.contains(event.target)
        ) {
            return;
        }

        sidebar.classList.remove("show");
    });

    window.addEventListener("resize", () => {
        if (window.innerWidth > 991) {
            sidebar.classList.remove("show");
        }
    });
}


/* =========================================================
   AUTHENTICATION
========================================================= */

function checkAdminAuth() {

    const token =
        localStorage.getItem("token") ||
        localStorage.getItem("smartagriAuthToken") ||
        localStorage.getItem("smartagri_auth_token");

    let role = localStorage.getItem("role");
    let username = localStorage.getItem("username");

    if (!role || !username) {

        try {

            const u = JSON.parse(
                localStorage.getItem("user") ||
                localStorage.getItem("smartagriUser") ||
                localStorage.getItem("smartagri_user") ||
                "{}"
            );

            if (u.role) {
                role = u.role;
            }

            if (u.username) {
                username = u.username;
            }

        } catch (e) {
            console.error("Unable to read user information", e);
        }
    }

    if (!token) {

        window.location.href =
            window.location.pathname.includes("/admin/")
                ? "../HTML/login.html"
                : "login.html";

        return;
    }

    if (role && role !== "ADMIN") {

        alert(
            "Access denied: Administrator privileges required."
        );

        window.location.href =
            window.location.pathname.includes("/admin/")
                ? "../HTML/index.html"
                : "customer-catalog.html";

        return;
    }

    const nameEl =
        document.getElementById("currentUserName");

    if (nameEl && username) {
        nameEl.textContent = username;
    }
}


function logout() {

    [
        "smartagriUser",
        "smartagri_user",
        "user",
        "smartagriAuthToken",
        "smartagri_auth_token",
        "token",
        "role",
        "username"
    ].forEach(key => {
        localStorage.removeItem(key);
    });

    window.location.href =
        window.location.pathname.includes("/admin/")
            ? "../HTML/login.html"
            : "login.html";
}


function getAuthHeaders() {

    const token =
        localStorage.getItem("token") ||
        localStorage.getItem("smartagriAuthToken") ||
        localStorage.getItem("smartagri_auth_token");

    return {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${token}`
    };
}


/* =========================================================
   ALERTS
========================================================= */

function showAlert(message, type = "success") {

    const container =
        document.getElementById("alertContainer");

    if (!container) return;

    const icon =
        type === "success"
            ? "check-circle"
            : "exclamation-circle";

    container.innerHTML = `
        <div
            class="alert alert-${type} alert-dismissible fade show"
            role="alert">

            <i class="bi bi-${icon} me-2"></i>

            ${message}

            <button
                type="button"
                class="btn-close"
                data-bs-dismiss="alert">
            </button>

        </div>
    `;

    setTimeout(() => {

        const alertElement =
            container.querySelector(".alert");

        if (!alertElement) return;

        const alertInstance =
            bootstrap.Alert.getOrCreateInstance(
                alertElement
            );

        if (alertInstance) {
            alertInstance.close();
        }

    }, 5000);
}


/* =========================================================
   DEBOUNCE
========================================================= */

function debounce(func, wait) {

    let timeout;

    return function executedFunction(...args) {

        const later = () => {

            clearTimeout(timeout);

            func(...args);
        };

        clearTimeout(timeout);

        timeout = setTimeout(
            later,
            wait
        );
    };
}


/* =========================================================
   ACTIVE TAB RELOAD
========================================================= */

function reloadActiveTab() {

    const activeBtn =
        document.querySelector(
            "#procurementTabs .nav-link.active"
        );

    if (!activeBtn) return;

    if (
        activeBtn.id ===
        "tab-suppliers-btn"
    ) {
        loadSuppliers();
    }

    else if (
        activeBtn.id ===
        "tab-pos-btn"
    ) {
        loadPurchaseOrders();
    }

    else if (
        activeBtn.id ===
        "tab-grns-btn"
    ) {
        loadGoodsReceipts();
    }
}


/* =========================================================
   DASHBOARD
========================================================= */

async function loadDashboard() {

    try {

        const res = await fetch(
            `${API_BASE}/admin/procurement/dashboard`,
            {
                credentials: "include",
                headers: getAuthHeaders()
            }
        );

        if (!res.ok) return;

        const json = await res.json();

        if (json.success && json.data) {

            const d = json.data;

            document.getElementById(
                "kpiActiveSuppliers"
            ).textContent =
                d.activeSuppliers || 0;

            document.getElementById(
                "kpiTotalSuppliers"
            ).textContent =
                `Total: ${d.totalSuppliers || 0}`;

            document.getElementById(
                "kpiOpenPOs"
            ).textContent =
                d.openPurchaseOrders || 0;

            document.getElementById(
                "kpiDraftPendingPOs"
            ).textContent =
                `Draft: ${d.draftPOs || 0} | Pending: ${d.pendingApprovalPOs || 0}`;

            document.getElementById(
                "kpiPendingGRNs"
            ).textContent =
                d.pendingGRNs || 0;

            document.getElementById(
                "kpiPartiallyReceivedPOs"
            ).textContent =
                `Partial POs: ${d.partiallyReceivedPOs || 0}`;

            const spend =
                d.thisMonthPurchaseValue
                    ? Number(
                        d.thisMonthPurchaseValue
                    ).toLocaleString(
                        "en-IN",
                        {
                            minimumFractionDigits: 2,
                            maximumFractionDigits: 2
                        }
                    )
                    : "0.00";

            document.getElementById(
                "kpiMonthSpend"
            ).textContent =
                `₹${spend}`;
        }

    } catch (err) {

        console.error(
            "Dashboard metrics failed:",
            err
        );
    }
}


/* =========================================================
   SUPPLIERS
========================================================= */

async function loadSuppliers() {

    const search =
        document.getElementById(
            "supplierSearchInput"
        )?.value || "";

    const status =
        document.getElementById(
            "supplierStatusFilter"
        )?.value || "";

    const tbody =
        document.getElementById(
            "suppliersTableBody"
        );

    if (!tbody) return;

    tbody.innerHTML = `
        <tr>
            <td
                colspan="8"
                class="text-center py-4 text-muted">

                <div
                    class="spinner-border spinner-border-sm me-2">
                </div>

                Loading suppliers...

            </td>
        </tr>
    `;

    let url =
        `${API_BASE}/admin/suppliers?`;

    if (status) {
        url +=
            `status=${encodeURIComponent(status)}&`;
    }

    if (search) {
        url +=
            `search=${encodeURIComponent(search)}&`;
    }

    try {

        const res = await fetch(
            url,
            {
                credentials: "include",
                headers: getAuthHeaders()
            }
        );

        const json = await res.json();

        if (
            !res.ok ||
            !json.success
        ) {
            throw new Error(
                json.message ||
                "Failed to load suppliers"
            );
        }

        currentSuppliers =
            json.data || [];

        if (
            currentSuppliers.length === 0
        ) {

            tbody.innerHTML = `
                <tr>
                    <td
                        colspan="8"
                        class="text-center py-4 text-muted">

                        No suppliers found.

                    </td>
                </tr>
            `;

            return;
        }

        tbody.innerHTML =
            currentSuppliers.map(
                supplier => {

                    let statusBadge =
                        "bg-success";

                    if (
                        supplier.status ===
                        "INACTIVE"
                    ) {
                        statusBadge =
                            "bg-secondary";
                    }

                    if (
                        supplier.status ===
                        "BLOCKED"
                    ) {
                        statusBadge =
                            "bg-danger";
                    }

                    return `
                        <tr>

                            <td
                                class="fw-bold font-monospace text-primary">

                                ${supplier.supplierCode}

                            </td>

                            <td>

                                <div class="fw-bold">
                                    ${supplier.companyName}
                                </div>

                                ${
                                    supplier.paymentTerms
                                        ? `
                                            <small class="text-muted">

                                                <i
                                                    class="bi bi-clock-history me-1">
                                                </i>

                                                ${supplier.paymentTerms}

                                            </small>
                                        `
                                        : ""
                                }

                            </td>

                            <td>
                                ${supplier.contactPerson || "-"}
                            </td>

                            <td>

                                <div>
                                    ${supplier.phone || "-"}
                                </div>

                                <small class="text-muted">
                                    ${supplier.email || ""}
                                </small>

                            </td>

                            <td>

                                ${
                                    supplier.city
                                        ? `${supplier.city}, ${supplier.state || ""}`
                                        : "-"
                                }

                            </td>

                            <td>

                                <small
                                    class="d-block text-muted">

                                    GST:
                                    ${supplier.gstNumber || "-"}

                                </small>

                                <small
                                    class="d-block text-muted">

                                    PAN:
                                    ${supplier.panNumber || "-"}

                                </small>

                            </td>

                            <td>

                                <span
                                    class="badge ${statusBadge} status-badge">

                                    ${supplier.status}

                                </span>

                            </td>

                            <td
                                class="text-end pe-3">

                                <div
                                    class="btn-group btn-group-sm">

                                    <button
                                        class="btn btn-outline-secondary"
                                        title="Edit Supplier"
                                        onclick="openEditSupplierModal(${supplier.id})">

                                        <i
                                            class="bi bi-pencil">
                                        </i>

                                    </button>

                                    ${
                                        supplier.status === "ACTIVE"

                                            ? `
                                                <button
                                                    class="btn btn-outline-warning"
                                                    title="Deactivate"
                                                    onclick="toggleSupplierStatus(${supplier.id}, 'deactivate')">

                                                    <i
                                                        class="bi bi-pause">
                                                    </i>

                                                </button>
                                            `

                                            : `
                                                <button
                                                    class="btn btn-outline-success"
                                                    title="Activate"
                                                    onclick="toggleSupplierStatus(${supplier.id}, 'activate')">

                                                    <i
                                                        class="bi bi-play">
                                                    </i>

                                                </button>
                                            `
                                    }

                                    ${
                                        supplier.status !== "BLOCKED"

                                            ? `
                                                <button
                                                    class="btn btn-outline-danger"
                                                    title="Block"
                                                    onclick="toggleSupplierStatus(${supplier.id}, 'block')">

                                                    <i
                                                        class="bi bi-slash-circle">
                                                    </i>

                                                </button>
                                            `
                                            : ""
                                    }

                                </div>

                            </td>

                        </tr>
                    `;
                }
            ).join("");

    } catch (err) {

        tbody.innerHTML = `
            <tr>

                <td
                    colspan="8"
                    class="text-center py-4 text-danger">

                    ${err.message}

                </td>

            </tr>
        `;
    }
}


function openCreateSupplierModal() {

    document.getElementById(
        "supplierEditId"
    ).value = "";

    document.getElementById(
        "supplierForm"
    ).reset();

    document.getElementById(
        "supplierModalTitle"
    ).innerHTML =
        `<i class="bi bi-building me-2"></i>Add Supplier`;

    document.getElementById(
        "supCode"
    ).disabled = false;

    new bootstrap.Modal(
        document.getElementById(
            "supplierModal"
        )
    ).show();
}


function openEditSupplierModal(id) {

    const supplier =
        currentSuppliers.find(
            x => x.id === id
        );

    if (!supplier) return;

    document.getElementById(
        "supplierEditId"
    ).value =
        supplier.id;

    document.getElementById(
        "supCode"
    ).value =
        supplier.supplierCode;

    document.getElementById(
        "supCode"
    ).disabled = true;

    document.getElementById(
        "supCompanyName"
    ).value =
        supplier.companyName;

    document.getElementById(
        "supContactPerson"
    ).value =
        supplier.contactPerson || "";

    document.getElementById(
        "supPhone"
    ).value =
        supplier.phone || "";

    document.getElementById(
        "supEmail"
    ).value =
        supplier.email || "";

    document.getElementById(
        "supGstNumber"
    ).value =
        supplier.gstNumber || "";

    document.getElementById(
        "supPanNumber"
    ).value =
        supplier.panNumber || "";

    document.getElementById(
        "supCity"
    ).value =
        supplier.city || "";

    document.getElementById(
        "supState"
    ).value =
        supplier.state || "";

    document.getElementById(
        "supPostalCode"
    ).value =
        supplier.postalCode || "";

    document.getElementById(
        "supAddress"
    ).value =
        supplier.address || "";

    document.getElementById(
        "supPaymentTerms"
    ).value =
        supplier.paymentTerms || "";

    document.getElementById(
        "supNotes"
    ).value =
        supplier.notes || "";

    document.getElementById(
        "supplierModalTitle"
    ).innerHTML =
        `<i class="bi bi-pencil me-2"></i>Edit Supplier: ${supplier.companyName}`;

    new bootstrap.Modal(
        document.getElementById(
            "supplierModal"
        )
    ).show();
}


async function saveSupplier() {

    const editId =
        document.getElementById(
            "supplierEditId"
        ).value;

    const body = {

        supplierCode:
            document.getElementById(
                "supCode"
            ).value.trim(),

        companyName:
            document.getElementById(
                "supCompanyName"
            ).value.trim(),

        contactPerson:
            document.getElementById(
                "supContactPerson"
            ).value.trim(),

        phone:
            document.getElementById(
                "supPhone"
            ).value.trim(),

        email:
            document.getElementById(
                "supEmail"
            ).value.trim(),

        gstNumber:
            document.getElementById(
                "supGstNumber"
            ).value.trim(),

        panNumber:
            document.getElementById(
                "supPanNumber"
            ).value.trim(),

        city:
            document.getElementById(
                "supCity"
            ).value.trim(),

        state:
            document.getElementById(
                "supState"
            ).value.trim(),

        postalCode:
            document.getElementById(
                "supPostalCode"
            ).value.trim(),

        address:
            document.getElementById(
                "supAddress"
            ).value.trim(),

        paymentTerms:
            document.getElementById(
                "supPaymentTerms"
            ).value.trim(),

        notes:
            document.getElementById(
                "supNotes"
            ).value.trim()
    };

    if (!body.companyName) {

        alert(
            "Company name is required"
        );

        return;
    }

    if (
        !editId &&
        !body.supplierCode
    ) {

        alert(
            "Supplier code is required"
        );

        return;
    }

    const url =
        editId
            ? `${API_BASE}/admin/suppliers/${editId}`
            : `${API_BASE}/admin/suppliers`;

    const method =
        editId
            ? "PUT"
            : "POST";

    try {

        const res = await fetch(
            url,
            {
                method: method,
                credentials: "include",
                headers: getAuthHeaders(),
                body: JSON.stringify(body)
            }
        );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {
            throw new Error(
                json.message ||
                "Failed to save supplier"
            );
        }

        bootstrap.Modal
            .getInstance(
                document.getElementById(
                    "supplierModal"
                )
            )
            .hide();

        showAlert(
            json.message ||
            "Supplier saved successfully"
        );

        loadSuppliers();
        loadSuppliersForDropdown();
        loadDashboard();

    } catch (err) {

        alert(err.message);
    }
}


async function toggleSupplierStatus(
    id,
    action
) {

    if (
        !confirm(
            `Are you sure you want to ${action} this supplier?`
        )
    ) {
        return;
    }

    try {

        const res = await fetch(
            `${API_BASE}/admin/suppliers/${id}/${action}`,
            {
                method: "POST",
                credentials: "include",
                headers: getAuthHeaders()
            }
        );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {
            throw new Error(
                json.message ||
                "Status update failed"
            );
        }

        showAlert(
            json.message ||
            `Supplier ${action}d successfully`
        );

        loadSuppliers();
        loadDashboard();

    } catch (err) {

        alert(err.message);
    }
}


/* =========================================================
   FERTILIZERS
========================================================= */

async function loadFertilizersList() {

    try {

        const res = await fetch(
            `${API_BASE}/fertilizers`,
            {
                credentials: "include"
            }
        );

        const json =
            await res.json();

        if (
            json.success &&
            json.data
        ) {
            currentFertilizers =
                json.data;
        }

    } catch (e) {

        console.error(
            "Failed to load fertilizers list",
            e
        );
    }
}


/* =========================================================
   SUPPLIER DROPDOWN
========================================================= */

async function loadSuppliersForDropdown() {

    try {

        const res = await fetch(
            `${API_BASE}/admin/suppliers`,
            {
                credentials: "include",
                headers: getAuthHeaders()
            }
        );

        const json =
            await res.json();

        if (
            json.success &&
            json.data
        ) {

            const list =
                json.data;

            const filterEl =
                document.getElementById(
                    "poSupplierFilter"
                );

            if (filterEl) {

                filterEl.innerHTML =
                    `<option value="">All Suppliers</option>` +
                    list.map(
                        supplier =>
                            `<option value="${supplier.id}">
                                ${supplier.companyName}
                                (${supplier.supplierCode})
                            </option>`
                    ).join("");
            }

            const modalSel =
                document.getElementById(
                    "poSupplierSelect"
                );

            if (modalSel) {

                modalSel.innerHTML =
                    `<option value="">
                        Select Supplier...
                    </option>` +

                    list
                        .filter(
                            supplier =>
                                supplier.status !== "BLOCKED"
                        )
                        .map(
                            supplier =>
                                `<option value="${supplier.id}">
                                    ${supplier.companyName}
                                    (${supplier.supplierCode})
                                </option>`
                        )
                        .join("");
            }
        }

    } catch (e) {

        console.error(
            "Failed to load suppliers dropdown",
            e
        );
    }
}


/* =========================================================
   PURCHASE ORDERS
========================================================= */

async function loadPurchaseOrders() {

    const search =
        document.getElementById(
            "poSearchInput"
        )?.value || "";

    const status =
        document.getElementById(
            "poStatusFilter"
        )?.value || "";

    const supplierId =
        document.getElementById(
            "poSupplierFilter"
        )?.value || "";

    const tbody =
        document.getElementById(
            "posTableBody"
        );

    if (!tbody) return;

    tbody.innerHTML = `
        <tr>

            <td
                colspan="8"
                class="text-center py-4 text-muted">

                <div
                    class="spinner-border spinner-border-sm me-2">
                </div>

                Loading purchase orders...

            </td>

        </tr>
    `;

    let url =
        `${API_BASE}/admin/purchase-orders?`;

    if (status) {

        url +=
            `status=${encodeURIComponent(status)}&`;
    }

    if (supplierId) {

        url +=
            `supplierId=${encodeURIComponent(supplierId)}&`;
    }

    if (search) {

        url +=
            `search=${encodeURIComponent(search)}&`;
    }

    try {

        const res =
            await fetch(
                url,
                {
                    credentials: "include",
                    headers: getAuthHeaders()
                }
            );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {

            throw new Error(
                json.message ||
                "Failed to load purchase orders"
            );
        }

        const pos =
            json.data || [];

        if (pos.length === 0) {

            tbody.innerHTML = `
                <tr>

                    <td
                        colspan="8"
                        class="text-center py-4 text-muted">

                        No purchase orders found.

                    </td>

                </tr>
            `;

            return;
        }

        tbody.innerHTML =
            pos.map(po => {

                let statusBadge =
                    "bg-secondary";

                if (
                    po.status ===
                    "APPROVED"
                ) {
                    statusBadge =
                        "bg-info text-dark";
                }

                if (
                    po.status ===
                    "SENT_TO_SUPPLIER"
                ) {
                    statusBadge =
                        "bg-primary";
                }

                if (
                    po.status ===
                    "PARTIALLY_RECEIVED"
                ) {
                    statusBadge =
                        "bg-warning text-dark";
                }

                if (
                    po.status ===
                    "FULLY_RECEIVED"
                ) {
                    statusBadge =
                        "bg-success";
                }

                if (
                    po.status ===
                    "CANCELLED"
                ) {
                    statusBadge =
                        "bg-danger";
                }

                if (
                    po.status ===
                    "CLOSED"
                ) {
                    statusBadge =
                        "bg-dark";
                }

                const total =
                    Number(
                        po.totalAmount || 0
                    ).toLocaleString(
                        "en-IN",
                        {
                            minimumFractionDigits: 2
                        }
                    );

                const progress =
                    po.totalOrderedQuantity > 0
                        ? (
                            po.totalReceivedQuantity /
                            po.totalOrderedQuantity *
                            100
                        )
                        : 0;

                return `
                    <tr>

                        <td
                            class="fw-bold font-monospace text-primary">

                            ${po.poNumber}

                        </td>

                        <td>

                            <div class="fw-bold">
                                ${po.supplierName || "Unknown"}
                            </div>

                            <small
                                class="text-muted font-monospace">

                                ${po.supplierCode || ""}

                            </small>

                        </td>

                        <td>
                            ${po.orderDate || "-"}
                        </td>

                        <td>

                            <span
                                class="badge bg-light text-dark border">

                                ${po.totalItemsCount || 0}
                                line items

                            </span>

                        </td>

                        <td>

                            <div
                                class="small fw-semibold">

                                ${po.totalReceivedQuantity || 0}
                                /
                                ${po.totalOrderedQuantity || 0}
                                units

                            </div>

                            <div
                                class="progress"
                                style="height: 5px;">

                                <div
                                    class="progress-bar bg-success"
                                    style="width: ${progress}%;">
                                </div>

                            </div>

                        </td>

                        <td
                            class="fw-bold text-success">

                            ₹${total}

                        </td>

                        <td>

                            <span
                                class="badge ${statusBadge} status-badge">

                                ${po.status}

                            </span>

                        </td>

                        <td
                            class="text-end pe-3">

                            <div
                                class="btn-group btn-group-sm">

                                <button
                                    class="btn btn-outline-primary"
                                    title="View Details"
                                    onclick="viewPoDetails(${po.id})">

                                    <i
                                        class="bi bi-eye">
                                    </i>

                                </button>

                                ${
                                    po.status === "DRAFT"

                                        ? `
                                            <button
                                                class="btn btn-outline-success"
                                                title="Submit for Approval"
                                                onclick="submitPo(${po.id})">

                                                <i
                                                    class="bi bi-send-check">
                                                </i>

                                            </button>
                                        `
                                        : ""
                                }

                                ${
                                    po.status === "PENDING_APPROVAL" ||
                                    po.status === "DRAFT"

                                        ? `
                                            <button
                                                class="btn btn-outline-success"
                                                title="Approve PO"
                                                onclick="approvePo(${po.id})">

                                                <i
                                                    class="bi bi-check-circle">
                                                </i>

                                            </button>
                                        `
                                        : ""
                                }

                                ${
                                    po.status === "APPROVED"

                                        ? `
                                            <button
                                                class="btn btn-outline-primary"
                                                title="Mark Sent to Supplier"
                                                onclick="sendPoToSupplier(${po.id})">

                                                <i
                                                    class="bi bi-truck">
                                                </i>

                                            </button>
                                        `
                                        : ""
                                }

                                ${
                                    po.status !== "FULLY_RECEIVED" &&
                                    po.status !== "CLOSED" &&
                                    po.status !== "CANCELLED"

                                        ? `
                                            <button
                                                class="btn btn-outline-danger"
                                                title="Cancel PO"
                                                onclick="cancelPo(${po.id})">

                                                <i
                                                    class="bi bi-x-circle">
                                                </i>

                                            </button>
                                        `
                                        : ""
                                }

                            </div>

                        </td>

                    </tr>
                `;
            }).join("");

    } catch (err) {

        tbody.innerHTML = `
            <tr>

                <td
                    colspan="8"
                    class="text-center py-4 text-danger">

                    ${err.message}

                </td>

            </tr>
        `;
    }
}


/* =========================================================
   CREATE PURCHASE ORDER
========================================================= */

function openCreatePoModal() {

    document.getElementById(
        "poForm"
    ).reset();

    document.getElementById(
        "poOrderDate"
    ).value =
        new Date()
            .toISOString()
            .split("T")[0];

    document.getElementById(
        "poItemsBody"
    ).innerHTML = "";

    addPoItemRow();

    new bootstrap.Modal(
        document.getElementById(
            "poModal"
        )
    ).show();
}


function addPoItemRow() {

    const tbody =
        document.getElementById(
            "poItemsBody"
        );

    const tr =
        document.createElement("tr");

    tr.className =
        "po-item-row";

    const options =
        currentFertilizers
            .map(
                fertilizer =>
                    `<option
                        value="${fertilizer.id}"
                        data-price="${
                            fertilizer.purchasePrice ||
                            fertilizer.sellingPrice ||
                            100
                        }">

                        ${fertilizer.name}
                        (${fertilizer.brand || ""})

                    </option>`
            )
            .join("");

    tr.innerHTML = `

        <td>

            <select
                class="form-select form-select-sm po-fert-select"
                onchange="onPoFertilizerChange(this)"
                required>

                <option value="">
                    Select fertilizer...
                </option>

                ${options}

            </select>

        </td>

        <td>

            <input
                type="number"
                class="form-control form-control-sm po-qty-input"
                min="1"
                value="10"
                oninput="calculatePoRowTotal(this)"
                required>

        </td>

        <td>

            <input
                type="number"
                class="form-control form-control-sm po-cost-input"
                min="0"
                step="0.5"
                value="100.00"
                oninput="calculatePoRowTotal(this)"
                required>

        </td>

        <td>

            <input
                type="number"
                class="form-control form-control-sm po-tax-input"
                min="0"
                max="28"
                step="1"
                value="5"
                oninput="calculatePoRowTotal(this)"
                required>

        </td>

        <td>

            <span
                class="fw-semibold po-row-total">

                ₹1050.00

            </span>

        </td>

        <td class="text-center">

            <button
                type="button"
                class="btn btn-outline-danger btn-sm p-1"
                onclick="removePoRow(this)"
                title="Remove item">

                <i class="bi bi-trash"></i>

            </button>

        </td>
    `;

    tbody.appendChild(tr);

    recalculateAllPoTotals();
}


function onPoFertilizerChange(selectEl) {

    const selected =
        selectEl.options[
            selectEl.selectedIndex
        ];

    const price =
        selected.getAttribute(
            "data-price"
        ) || "100.00";

    const row =
        selectEl.closest("tr");

    row.querySelector(
        ".po-cost-input"
    ).value =
        Number(price).toFixed(2);

    calculatePoRowTotal(
        selectEl
    );
}


function calculatePoRowTotal(inputEl) {

    const row =
        inputEl.closest("tr");

    const qty =
        parseFloat(
            row.querySelector(
                ".po-qty-input"
            ).value
        ) || 0;

    const cost =
        parseFloat(
            row.querySelector(
                ".po-cost-input"
            ).value
        ) || 0;

    const taxRate =
        parseFloat(
            row.querySelector(
                ".po-tax-input"
            ).value
        ) || 0;

    const subtotal =
        qty * cost;

    const tax =
        subtotal *
        (taxRate / 100);

    const total =
        subtotal + tax;

    row.querySelector(
        ".po-row-total"
    ).textContent =
        `₹${total.toFixed(2)}`;

    recalculateAllPoTotals();
}


function removePoRow(btn) {

    const tbody =
        document.getElementById(
            "poItemsBody"
        );

    if (
        tbody.children.length <= 1
    ) {

        alert(
            "A purchase order must have at least one line item."
        );

        return;
    }

    btn.closest("tr").remove();

    recalculateAllPoTotals();
}


function recalculateAllPoTotals() {

    let subtotal = 0;
    let tax = 0;

    const rows =
        document.querySelectorAll(
            ".po-item-row"
        );

    rows.forEach(row => {

        const qty =
            parseFloat(
                row.querySelector(
                    ".po-qty-input"
                ).value
            ) || 0;

        const cost =
            parseFloat(
                row.querySelector(
                    ".po-cost-input"
                ).value
            ) || 0;

        const taxRate =
            parseFloat(
                row.querySelector(
                    ".po-tax-input"
                ).value
            ) || 0;

        const sub =
            qty * cost;

        const rowTax =
            sub *
            (taxRate / 100);

        subtotal += sub;
        tax += rowTax;
    });

    document.getElementById(
        "poSubtotalDisplay"
    ).textContent =
        `₹${subtotal.toFixed(2)}`;

    document.getElementById(
        "poTaxDisplay"
    ).textContent =
        `₹${tax.toFixed(2)}`;

    document.getElementById(
        "poTotalDisplay"
    ).textContent =
        `₹${(
            subtotal + tax
        ).toFixed(2)}`;
}


/* =========================================================
   SAVE PURCHASE ORDER
========================================================= */

async function savePurchaseOrder() {

    const supplierId =
        document.getElementById(
            "poSupplierSelect"
        ).value;

    if (!supplierId) {

        alert(
            "Please select a supplier"
        );

        return;
    }

    const items = [];

    const rows =
        document.querySelectorAll(
            ".po-item-row"
        );

    for (const row of rows) {

        const fertId =
            row.querySelector(
                ".po-fert-select"
            ).value;

        const qty =
            parseInt(
                row.querySelector(
                    ".po-qty-input"
                ).value,
                10
            );

        const cost =
            parseFloat(
                row.querySelector(
                    ".po-cost-input"
                ).value
            );

        const taxRate =
            parseFloat(
                row.querySelector(
                    ".po-tax-input"
                ).value
            );

        if (!fertId) {

            alert(
                "Please select a fertilizer for each row"
            );

            return;
        }

        if (!qty || qty <= 0) {

            alert(
                "Ordered quantity must be greater than 0"
            );

            return;
        }

        items.push({

            fertilizerId:
                parseInt(
                    fertId,
                    10
                ),

            orderedQuantity:
                qty,

            unitCost:
                cost,

            taxRate:
                taxRate
        });
    }

    const body = {

        supplierId:
            parseInt(
                supplierId,
                10
            ),

        orderDate:
            document.getElementById(
                "poOrderDate"
            ).value || undefined,

        expectedDeliveryDate:
            document.getElementById(
                "poExpectedDate"
            ).value || undefined,

        notes:
            document.getElementById(
                "poNotes"
            ).value.trim() ||
            undefined,

        items:
            items
    };

    try {

        const res =
            await fetch(
                `${API_BASE}/admin/purchase-orders`,
                {
                    method: "POST",
                    credentials: "include",
                    headers: getAuthHeaders(),
                    body: JSON.stringify(body)
                }
            );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {

            throw new Error(
                json.message ||
                "Failed to create purchase order"
            );
        }

        bootstrap.Modal
            .getInstance(
                document.getElementById(
                    "poModal"
                )
            )
            .hide();

        showAlert(
            json.message ||
            "Purchase order created successfully"
        );

        loadPurchaseOrders();
        loadDashboard();

    } catch (err) {

        alert(err.message);
    }
}


/* =========================================================
   VIEW PURCHASE ORDER
========================================================= */

async function viewPoDetails(id) {

    const modalEl =
        document.getElementById(
            "poViewModal"
        );

    const modal =
        new bootstrap.Modal(
            modalEl
        );

    modal.show();

    const bodyEl =
        document.getElementById(
            "poViewModalBody"
        );

    bodyEl.innerHTML = `
        <div class="text-center py-4">

            <div
                class="spinner-border spinner-border-sm me-2">
            </div>

            Loading details...

        </div>
    `;

    try {

        const res =
            await fetch(
                `${API_BASE}/admin/purchase-orders/${id}`,
                {
                    credentials: "include",
                    headers: getAuthHeaders()
                }
            );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {

            throw new Error(
                json.message ||
                "Failed to load PO details"
            );
        }

        const po =
            json.data;

        document.getElementById(
            "poViewModalTitle"
        ).innerHTML =
            `<i class="bi bi-file-earmark-text me-2"></i>
             Purchase Order #${po.poNumber}`;

        const subtotal =
            Number(
                po.subtotal || 0
            ).toLocaleString(
                "en-IN",
                {
                    minimumFractionDigits: 2
                }
            );

        const tax =
            Number(
                po.taxAmount || 0
            ).toLocaleString(
                "en-IN",
                {
                    minimumFractionDigits: 2
                }
            );

        const total =
            Number(
                po.totalAmount || 0
            ).toLocaleString(
                "en-IN",
                {
                    minimumFractionDigits: 2
                }
            );

        bodyEl.innerHTML = `

            <div
                class="row g-3 mb-3">

                <div class="col-md-4">

                    <small
                        class="text-muted text-uppercase fw-bold">

                        Supplier

                    </small>

                    <div class="fw-bold">

                        ${po.supplierName}
                        (${po.supplierCode})

                    </div>

                </div>

                <div class="col-md-4">

                    <small
                        class="text-muted text-uppercase fw-bold">

                        Order Date

                    </small>

                    <div>
                        ${po.orderDate}
                    </div>

                </div>

                <div class="col-md-4">

                    <small
                        class="text-muted text-uppercase fw-bold">

                        Status

                    </small>

                    <div>

                        <span
                            class="badge bg-primary">

                            ${po.status}

                        </span>

                    </div>

                </div>

                <div class="col-md-4">

                    <small
                        class="text-muted text-uppercase fw-bold">

                        Created By

                    </small>

                    <div>
                        ${po.createdBy || "admin"}
                    </div>

                </div>

                <div class="col-md-4">

                    <small
                        class="text-muted text-uppercase fw-bold">

                        Approved By

                    </small>

                    <div>

                        ${
                            po.approvedBy
                                ? `${po.approvedBy} (${po.approvedAt || ""})`
                                : "Pending"
                        }

                    </div>

                </div>

                <div class="col-md-4">

                    <small
                        class="text-muted text-uppercase fw-bold">

                        Expected Date

                    </small>

                    <div>

                        ${
                            po.expectedDeliveryDate ||
                            "Not specified"
                        }

                    </div>

                </div>

            </div>


            <h6 class="fw-bold mb-2">
                Line Items
            </h6>


            <div
                class="table-responsive mb-3">

                <table
                    class="table table-bordered table-sm align-middle">

                    <thead class="table-light">

                        <tr>

                            <th>
                                Fertilizer
                            </th>

                            <th>
                                Ordered
                            </th>

                            <th>
                                Received
                            </th>

                            <th>
                                Remaining
                            </th>

                            <th>
                                Unit Cost
                            </th>

                            <th>
                                Total
                            </th>

                        </tr>

                    </thead>

                    <tbody>

                        ${po.items.map(
                            item => `

                                <tr>

                                    <td
                                        class="fw-semibold">

                                        ${item.fertilizerName}

                                    </td>

                                    <td>
                                        ${item.orderedQuantity}
                                    </td>

                                    <td
                                        class="fw-bold text-success">

                                        ${item.receivedQuantity}

                                    </td>

                                    <td
                                        class="fw-bold text-warning">

                                        ${item.remainingQuantity}

                                    </td>

                                    <td>

                                        ₹${Number(
                                            item.unitCost
                                        ).toFixed(2)}

                                    </td>

                                    <td>

                                        ₹${Number(
                                            item.lineTotal
                                        ).toFixed(2)}

                                    </td>

                                </tr>

                            `
                        ).join("")}

                    </tbody>

                </table>

            </div>


            <div
                class="row justify-content-end">

                <div
                    class="col-md-5">

                    <div
                        class="bg-light p-3 rounded">

                        <div
                            class="d-flex justify-content-between mb-1">

                            <span>
                                Subtotal:
                            </span>

                            <span>
                                ₹${subtotal}
                            </span>

                        </div>

                        <div
                            class="d-flex justify-content-between mb-1">

                            <span>
                                Tax:
                            </span>

                            <span>
                                ₹${tax}
                            </span>

                        </div>

                        <hr class="my-1">

                        <div
                            class="d-flex justify-content-between fw-bold">

                            <span>
                                Total Amount:
                            </span>

                            <span
                                class="text-success">

                                ₹${total}

                            </span>

                        </div>

                    </div>

                </div>

            </div>
        `;

    } catch (err) {

        bodyEl.innerHTML = `
            <div
                class="text-danger py-4 text-center">

                ${err.message}

            </div>
        `;
    }
}


/* =========================================================
   PURCHASE ORDER ACTIONS
========================================================= */

async function submitPo(id) {

    if (
        !confirm(
            "Submit this draft purchase order for approval?"
        )
    ) {
        return;
    }

    try {

        const res =
            await fetch(
                `${API_BASE}/admin/purchase-orders/${id}/submit`,
                {
                    method: "POST",
                    credentials: "include",
                    headers: getAuthHeaders()
                }
            );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {
            throw new Error(
                json.message ||
                "Failed to submit PO"
            );
        }

        showAlert(
            json.message ||
            "PO submitted for approval"
        );

        loadPurchaseOrders();
        loadDashboard();

    } catch (e) {

        alert(e.message);
    }
}


async function approvePo(id) {

    if (
        !confirm(
            "Approve this purchase order?"
        )
    ) {
        return;
    }

    try {

        const res =
            await fetch(
                `${API_BASE}/admin/purchase-orders/${id}/approve`,
                {
                    method: "POST",
                    credentials: "include",
                    headers: getAuthHeaders()
                }
            );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {
            throw new Error(
                json.message ||
                "Failed to approve PO"
            );
        }

        showAlert(
            json.message ||
            "PO approved successfully"
        );

        loadPurchaseOrders();
        loadDashboard();

    } catch (e) {

        alert(e.message);
    }
}


async function sendPoToSupplier(id) {

    if (
        !confirm(
            "Mark this purchase order as SENT to supplier?"
        )
    ) {
        return;
    }

    try {

        const res =
            await fetch(
                `${API_BASE}/admin/purchase-orders/${id}/send`,
                {
                    method: "POST",
                    credentials: "include",
                    headers: getAuthHeaders()
                }
            );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {
            throw new Error(
                json.message ||
                "Failed to mark PO as sent"
            );
        }

        showAlert(
            json.message ||
            "PO marked as sent to supplier"
        );

        loadPurchaseOrders();
        loadDashboard();

    } catch (e) {

        alert(e.message);
    }
}


async function cancelPo(id) {

    const reason =
        prompt(
            "Please enter cancellation reason:"
        );

    if (reason === null) {
        return;
    }

    try {

        const res =
            await fetch(
                `${API_BASE}/admin/purchase-orders/${id}/cancel`,
                {
                    method: "POST",
                    credentials: "include",
                    headers: getAuthHeaders(),
                    body: JSON.stringify({
                        reason: reason
                    })
                }
            );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {
            throw new Error(
                json.message ||
                "Failed to cancel PO"
            );
        }

        showAlert(
            json.message ||
            "PO cancelled"
        );

        loadPurchaseOrders();
        loadDashboard();

    } catch (e) {

        alert(e.message);
    }
}


/* =========================================================
   GOODS RECEIPTS / GRN
========================================================= */

async function loadGoodsReceipts() {

    const search =
        document.getElementById(
            "grnSearchInput"
        )?.value || "";

    const status =
        document.getElementById(
            "grnStatusFilter"
        )?.value || "";

    const tbody =
        document.getElementById(
            "grnsTableBody"
        );

    if (!tbody) return;

    tbody.innerHTML = `
        <tr>

            <td
                colspan="8"
                class="text-center py-4 text-muted">

                <div
                    class="spinner-border spinner-border-sm me-2">
                </div>

                Loading goods receipts...

            </td>

        </tr>
    `;

    let url =
        `${API_BASE}/admin/goods-receipts?`;

    if (status) {

        url +=
            `status=${encodeURIComponent(status)}&`;
    }

    if (search) {

        url +=
            `search=${encodeURIComponent(search)}&`;
    }

    try {

        const res =
            await fetch(
                url,
                {
                    credentials: "include",
                    headers: getAuthHeaders()
                }
            );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {

            throw new Error(
                json.message ||
                "Failed to load goods receipts"
            );
        }

        const grns =
            json.data || [];

        if (grns.length === 0) {

            tbody.innerHTML = `
                <tr>

                    <td
                        colspan="8"
                        class="text-center py-4 text-muted">

                        No goods receipts found.

                    </td>

                </tr>
            `;

            return;
        }

        tbody.innerHTML =
            grns.map(grn => {

                let statusBadge =
                    "bg-secondary";

                if (
                    grn.status ===
                    "PENDING_INSPECTION"
                ) {
                    statusBadge =
                        "bg-warning text-dark";
                }

                if (
                    grn.status ===
                    "INSPECTION_PASSED"
                ) {
                    statusBadge =
                        "bg-info text-dark";
                }

                if (
                    grn.status ===
                    "PARTIALLY_ACCEPTED"
                ) {
                    statusBadge =
                        "bg-warning text-dark";
                }

                if (
                    grn.status ===
                    "ACCEPTED"
                ) {
                    statusBadge =
                        "bg-success";
                }

                if (
                    grn.status ===
                    "REJECTED"
                ) {
                    statusBadge =
                        "bg-danger";
                }

                let inspBadge =
                    "bg-secondary";

                if (
                    grn.inspectionStatus ===
                    "PASSED"
                ) {
                    inspBadge =
                        "bg-success";
                }

                if (
                    grn.inspectionStatus ===
                    "PARTIAL"
                ) {
                    inspBadge =
                        "bg-warning text-dark";
                }

                if (
                    grn.inspectionStatus ===
                    "FAILED"
                ) {
                    inspBadge =
                        "bg-danger";
                }

                return `
                    <tr>

                        <td
                            class="fw-bold font-monospace text-primary">

                            ${grn.grnNumber}

                        </td>

                        <td>

                            <span
                                class="font-monospace text-secondary">

                                ${grn.poNumber || "-"}

                            </span>

                        </td>

                        <td>
                            ${grn.supplierName || "Unknown"}
                        </td>

                        <td>
                            ${grn.receivedDate || "-"}
                        </td>

                        <td>

                            <span
                                class="fw-bold text-success">

                                ${grn.totalAcceptedQuantity}

                            </span>

                            /
                            ${grn.totalReceivedQuantity}
                            units

                            ${
                                grn.totalRejectedQuantity > 0

                                    ? `
                                        <span
                                            class="badge bg-danger ms-1">

                                            ${grn.totalRejectedQuantity}
                                            rejected

                                        </span>
                                    `
                                    : ""
                            }

                        </td>

                        <td>

                            <span
                                class="badge ${inspBadge} status-badge">

                                ${grn.inspectionStatus}

                            </span>

                        </td>

                        <td>

                            <span
                                class="badge ${statusBadge} status-badge">

                                ${grn.status}

                            </span>

                        </td>

                        <td
                            class="text-end pe-3">

                            <div
                                class="btn-group btn-group-sm">

                                ${
                                    grn.status ===
                                    "PENDING_INSPECTION"

                                        ? `
                                            <button
                                                class="btn btn-outline-warning"
                                                title="Perform Inspection"
                                                onclick="openInspectModal(${grn.id})">

                                                <i
                                                    class="bi bi-shield-check">
                                                </i>

                                                Inspect

                                            </button>
                                        `
                                        : ""
                                }

                                ${
                                    grn.status !== "ACCEPTED" &&
                                    grn.status !== "REJECTED" &&
                                    grn.status !== "CANCELLED"

                                        ? `
                                            <button
                                                class="btn btn-success"
                                                title="Accept & Add to Inventory Batches"
                                                onclick="acceptGrn(${grn.id})">

                                                <i
                                                    class="bi bi-check-lg">
                                                </i>

                                                Accept

                                            </button>
                                        `
                                        : ""
                                }

                            </div>

                        </td>

                    </tr>
                `;
            }).join("");

    } catch (err) {

        tbody.innerHTML = `
            <tr>

                <td
                    colspan="8"
                    class="text-center py-4 text-danger">

                    ${err.message}

                </td>

            </tr>
        `;
    }
}


/* =========================================================
   CREATE GRN
========================================================= */

async function openCreateGrnModal() {

    const select =
        document.getElementById(
            "grnPoSelect"
        );

    select.innerHTML =
        `<option value="">
            Loading POs...
        </option>`;

    document.getElementById(
        "grnReceivedDate"
    ).value =
        new Date()
            .toISOString()
            .split("T")[0];

    document.getElementById(
        "grnItemsBody"
    ).innerHTML = `
        <tr>

            <td
                colspan="7"
                class="text-center py-3 text-muted">

                Please select a purchase order.

            </td>

        </tr>
    `;

    try {

        const res =
            await fetch(
                `${API_BASE}/admin/purchase-orders`,
                {
                    credentials: "include",
                    headers: getAuthHeaders()
                }
            );

        const json =
            await res.json();

        const pos =
            (json.data || [])
                .filter(
                    purchaseOrder =>
                        purchaseOrder.status === "APPROVED" ||
                        purchaseOrder.status === "SENT_TO_SUPPLIER" ||
                        purchaseOrder.status === "PARTIALLY_RECEIVED"
                );

        if (pos.length === 0) {

            select.innerHTML = `
                <option value="">
                    No open approved purchase orders available
                </option>
            `;

        } else {

            select.innerHTML =
                `<option value="">
                    Select Purchase Order...
                </option>` +

                pos.map(
                    purchaseOrder =>
                        `<option
                            value="${purchaseOrder.id}">

                            ${purchaseOrder.poNumber}
                            —
                            ${purchaseOrder.supplierName}
                            (${purchaseOrder.status})

                        </option>`
                ).join("");
        }

        new bootstrap.Modal(
            document.getElementById(
                "grnModal"
            )
        ).show();

    } catch (e) {

        alert(
            "Failed to load POs: " +
            e.message
        );
    }
}


/* =========================================================
   SELECT PO FOR GRN
========================================================= */

async function onPoSelectedForGrn() {

    const poId =
        document.getElementById(
            "grnPoSelect"
        ).value;

    const tbody =
        document.getElementById(
            "grnItemsBody"
        );

    if (!poId) {

        tbody.innerHTML = `
            <tr>

                <td
                    colspan="7"
                    class="text-center py-3 text-muted">

                    Please select a purchase order.

                </td>

            </tr>
        `;

        return;
    }

    tbody.innerHTML = `
        <tr>

            <td
                colspan="7"
                class="text-center py-3 text-muted">

                <div
                    class="spinner-border spinner-border-sm me-2">
                </div>

                Loading items...

            </td>

        </tr>
    `;

    try {

        const res =
            await fetch(
                `${API_BASE}/admin/purchase-orders/${poId}`,
                {
                    credentials: "include",
                    headers: getAuthHeaders()
                }
            );

        const json =
            await res.json();

        const po =
            json.data;

        const openItems =
            po.items.filter(
                item =>
                    item.remainingQuantity > 0
            );

        if (
            openItems.length === 0
        ) {

            tbody.innerHTML = `
                <tr>

                    <td
                        colspan="7"
                        class="text-center py-3 text-warning">

                        All items on this PO
                        are already fully received.

                    </td>

                </tr>
            `;

            return;
        }

        const dateNextYear =
            new Date();

        dateNextYear.setFullYear(
            dateNextYear.getFullYear() + 2
        );

        const defaultExp =
            dateNextYear
                .toISOString()
                .split("T")[0];

        const defaultMfg =
            new Date()
                .toISOString()
                .split("T")[0];

        tbody.innerHTML =
            openItems.map(
                item => `

                    <tr
                        class="grn-row"
                        data-poi-id="${item.id}">

                        <td>

                            <div class="fw-bold">

                                ${item.fertilizerName}

                            </div>

                            <small
                                class="text-muted font-monospace">

                                ${item.fertilizerSku || ""}

                            </small>

                        </td>

                        <td>
                            ${item.orderedQuantity}
                        </td>

                        <td>

                            <span
                                class="badge bg-primary">

                                ${item.remainingQuantity}

                            </span>

                        </td>

                        <td>

                            <input
                                type="number"
                                class="form-control form-control-sm grn-rec-qty"
                                min="1"
                                max="${item.remainingQuantity}"
                                value="${item.remainingQuantity}"
                                required>

                        </td>

                        <td>

                            <input
                                type="text"
                                class="form-control form-control-sm grn-batch-no"
                                value="BATCH-${new Date().getFullYear()}-${item.fertilizerId}-${Math.floor(1000 + Math.random() * 9000)}"
                                required>

                        </td>

                        <td>

                            <input
                                type="date"
                                class="form-control form-control-sm grn-mfg-date"
                                value="${defaultMfg}">

                        </td>

                        <td>

                            <input
                                type="date"
                                class="form-control form-control-sm grn-exp-date"
                                value="${defaultExp}"
                                required>

                        </td>

                    </tr>
                `
            ).join("");

    } catch (e) {

        tbody.innerHTML = `
            <tr>

                <td
                    colspan="7"
                    class="text-center py-3 text-danger">

                    ${e.message}

                </td>

            </tr>
        `;
    }
}


/* =========================================================
   SAVE GOODS RECEIPT
========================================================= */

async function saveGoodsReceipt() {

    const poId =
        document.getElementById(
            "grnPoSelect"
        ).value;

    if (!poId) {

        alert(
            "Please select a purchase order"
        );

        return;
    }

    const rows =
        document.querySelectorAll(
            ".grn-row"
        );

    if (rows.length === 0) {

        alert(
            "No items to receive"
        );

        return;
    }

    const items = [];

    for (const row of rows) {

        const poiId =
            row.getAttribute(
                "data-poi-id"
            );

        const qty =
            parseInt(
                row.querySelector(
                    ".grn-rec-qty"
                ).value,
                10
            );

        const batchNo =
            row.querySelector(
                ".grn-batch-no"
            ).value.trim();

        const mfg =
            row.querySelector(
                ".grn-mfg-date"
            ).value ||
            undefined;

        const exp =
            row.querySelector(
                ".grn-exp-date"
            ).value;

        if (!qty || qty <= 0) {

            alert(
                "Received quantity must be greater than 0"
            );

            return;
        }

        if (!batchNo) {

            alert(
                "Batch number is required for all receiving lots"
            );

            return;
        }

        if (!exp) {

            alert(
                "Expiry date is required for all receiving lots"
            );

            return;
        }

        items.push({

            purchaseOrderItemId:
                parseInt(
                    poiId,
                    10
                ),

            receivedQuantity:
                qty,

            batchNumber:
                batchNo,

            manufacturingDate:
                mfg,

            expiryDate:
                exp
        });
    }

    const body = {

        purchaseOrderId:
            parseInt(
                poId,
                10
            ),

        receivedDate:
            document.getElementById(
                "grnReceivedDate"
            ).value ||
            undefined,

        supplierInvoiceNumber:
            document.getElementById(
                "grnInvoiceNo"
            ).value.trim() ||
            undefined,

        supplierDeliveryNumber:
            document.getElementById(
                "grnDeliveryNo"
            ).value.trim() ||
            undefined,

        packagingCondition:
            document.getElementById(
                "grnPackaging"
            ).value,

        sealCondition:
            document.getElementById(
                "grnSeal"
            ).value,

        items:
            items
    };

    try {

        const res =
            await fetch(
                `${API_BASE}/admin/goods-receipts`,
                {
                    method: "POST",
                    credentials: "include",
                    headers: getAuthHeaders(),
                    body: JSON.stringify(body)
                }
            );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {

            throw new Error(
                json.message ||
                "Failed to create goods receipt"
            );
        }

        bootstrap.Modal
            .getInstance(
                document.getElementById(
                    "grnModal"
                )
            )
            .hide();

        showAlert(
            json.message ||
            "Goods receipt created successfully"
        );

        loadGoodsReceipts();
        loadDashboard();

    } catch (err) {

        alert(err.message);
    }
}


/* =========================================================
   GRN INSPECTION
========================================================= */

async function openInspectModal(id) {

    document.getElementById(
        "inspectGrnId"
    ).value = id;

    const tbody =
        document.getElementById(
            "inspectItemsBody"
        );

    tbody.innerHTML = `
        <tr>

            <td
                colspan="5"
                class="text-center py-3 text-muted">

                <div
                    class="spinner-border spinner-border-sm me-2">
                </div>

                Loading items...

            </td>

        </tr>
    `;

    new bootstrap.Modal(
        document.getElementById(
            "grnInspectModal"
        )
    ).show();

    try {

        const res =
            await fetch(
                `${API_BASE}/admin/goods-receipts/${id}`,
                {
                    credentials: "include",
                    headers: getAuthHeaders()
                }
            );

        const json =
            await res.json();

        const grn =
            json.data;

        activeGrnInspectData =
            grn;

        tbody.innerHTML =
            grn.items.map(
                item => `

                    <tr
                        class="inspect-item-row"
                        data-item-id="${item.id}">

                        <td>

                            <div class="fw-bold">

                                ${item.fertilizerName}

                            </div>

                            <small
                                class="text-muted font-monospace">

                                Lot:
                                ${item.batchNumber}

                            </small>

                        </td>

                        <td>

                            <span
                                class="badge bg-secondary fs-6">

                                ${item.receivedQuantity}

                            </span>

                        </td>

                        <td>

                            <input
                                type="number"
                                class="form-control form-control-sm insp-acc-qty"
                                min="0"
                                max="${item.receivedQuantity}"
                                value="${item.receivedQuantity}"
                                oninput="onInspQtyChange(this)">

                        </td>

                        <td>

                            <input
                                type="number"
                                class="form-control form-control-sm insp-rej-qty"
                                min="0"
                                max="${item.receivedQuantity}"
                                value="0"
                                oninput="onInspQtyChange(this)">

                        </td>

                        <td>

                            <input
                                type="text"
                                class="form-control form-control-sm insp-rej-reason"
                                placeholder="Reason if rejected">

                        </td>

                    </tr>
                `
            ).join("");

    } catch (e) {

        tbody.innerHTML = `
            <tr>

                <td
                    colspan="5"
                    class="text-center py-3 text-danger">

                    ${e.message}

                </td>

            </tr>
        `;
    }
}


/* =========================================================
   QUALITY STATUS
========================================================= */

function onQualityStatusChange() {

    const status =
        document.getElementById(
            "inspectQualityStatus"
        ).value;

    const rows =
        document.querySelectorAll(
            ".inspect-item-row"
        );

    rows.forEach(row => {

        const accInput =
            row.querySelector(
                ".insp-acc-qty"
            );

        const rejInput =
            row.querySelector(
                ".insp-rej-qty"
            );

        const max =
            parseInt(
                accInput.max,
                10
            );

        if (
            status === "PASSED"
        ) {

            accInput.value =
                max;

            rejInput.value =
                0;
        }

        else if (
            status === "FAILED"
        ) {

            accInput.value =
                0;

            rejInput.value =
                max;
        }
    });
}


/* =========================================================
   INSPECTION QUANTITY CHANGE
========================================================= */

function onInspQtyChange(
    inputEl
) {

    const row =
        inputEl.closest("tr");

    const accInput =
        row.querySelector(
            ".insp-acc-qty"
        );

    const rejInput =
        row.querySelector(
            ".insp-rej-qty"
        );

    const max =
        parseInt(
            accInput.max,
            10
        );

    if (
        inputEl === accInput
    ) {

        let acc =
            parseInt(
                accInput.value,
                10
            ) || 0;

        if (acc > max) {
            acc = max;
        }

        if (acc < 0) {
            acc = 0;
        }

        accInput.value =
            acc;

        rejInput.value =
            max - acc;

    } else {

        let rej =
            parseInt(
                rejInput.value,
                10
            ) || 0;

        if (rej > max) {
            rej = max;
        }

        if (rej < 0) {
            rej = 0;
        }

        rejInput.value =
            rej;

        accInput.value =
            max - rej;
    }
}


/* =========================================================
   SUBMIT GRN INSPECTION
========================================================= */

async function submitInspection() {

    const id =
        document.getElementById(
            "inspectGrnId"
        ).value;

    const itemInspections = [];

    const rows =
        document.querySelectorAll(
            ".inspect-item-row"
        );

    for (const row of rows) {

        const itemId =
            row.getAttribute(
                "data-item-id"
            );

        const accepted =
            parseInt(
                row.querySelector(
                    ".insp-acc-qty"
                ).value,
                10
            ) || 0;

        const rejected =
            parseInt(
                row.querySelector(
                    ".insp-rej-qty"
                ).value,
                10
            ) || 0;

        const reason =
            row.querySelector(
                ".insp-rej-reason"
            ).value.trim();

        itemInspections.push({

            itemId:
                parseInt(
                    itemId,
                    10
                ),

            acceptedQuantity:
                accepted,

            rejectedQuantity:
                rejected,

            rejectionReason:
                reason ||
                undefined
        });
    }

    const body = {

        inspectionStatus:
            document.getElementById(
                "inspectQualityStatus"
            ).value,

        packagingCondition:
            document.getElementById(
                "inspectPackaging"
            ).value,

        inspectionNotes:
            document.getElementById(
                "inspectNotes"
            ).value.trim() ||
            undefined,

        itemInspections:
            itemInspections
    };

    try {

        const res =
            await fetch(
                `${API_BASE}/admin/goods-receipts/${id}/inspect`,
                {
                    method: "POST",
                    credentials: "include",
                    headers: getAuthHeaders(),
                    body: JSON.stringify(body)
                }
            );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {

            throw new Error(
                json.message ||
                "Failed to submit inspection"
            );
        }

        bootstrap.Modal
            .getInstance(
                document.getElementById(
                    "grnInspectModal"
                )
            )
            .hide();

        showAlert(
            json.message ||
            "Inspection recorded successfully"
        );

        loadGoodsReceipts();
        loadDashboard();

    } catch (err) {

        alert(err.message);
    }
}


/* =========================================================
   ACCEPT GRN
========================================================= */

async function acceptGrn(id) {

    if (
        !confirm(
            "Accept this Goods Receipt and integrate accepted quantities into warehouse batch inventory? This action is permanent and creates audited RESTOCK movements."
        )
    ) {
        return;
    }

    try {

        const res =
            await fetch(
                `${API_BASE}/admin/goods-receipts/${id}/accept`,
                {
                    method: "POST",
                    credentials: "include",
                    headers: getAuthHeaders()
                }
            );

        const json =
            await res.json();

        if (
            !res.ok ||
            !json.success
        ) {

            throw new Error(
                json.message ||
                "Failed to accept GRN"
            );
        }

        showAlert(
            json.message ||
            "GRN accepted! Inventory batches created/replenished and parent stock updated."
        );

        loadGoodsReceipts();
        loadDashboard();

    } catch (err) {

        alert(err.message);
    }
}