/* =========================================================
   SMARTAGRI FERTILIZER MANAGEMENT - PHASE 2
   Connected directly to Spring Boot REST APIs (ApiClient)
   MySQL is the definitive source of truth.
========================================================= */

let fertilizers = [];
let categoriesList = [];
let currentPage = 1;
let pageSize = 10;
let editingId = null;

let fertilizerModal = null;
let productDetailsModal = null;

/* =========================================================
   INITIALIZATION
========================================================= */

document.addEventListener("DOMContentLoaded", async function () {
    const fertModalEl = document.getElementById("fertilizerModal");
    if (fertModalEl && typeof bootstrap !== "undefined") {
        fertilizerModal = new bootstrap.Modal(fertModalEl);
    }

    const detailModalEl = document.getElementById("productDetailsModal");
    if (detailModalEl && typeof bootstrap !== "undefined") {
        productDetailsModal = new bootstrap.Modal(detailModalEl);
    }

    bindEvents();
    await loadCategories();
    await loadFertilizers();
});

/* =========================================================
   LOAD DATA FROM BACKEND API
========================================================= */

async function loadCategories() {
    try {
        const response = await ApiClient.get("/categories");
        if (response && response.success && Array.isArray(response.data)) {
            categoriesList = response.data;
        } else {
            console.warn("Categories API returned empty or invalid structure:", response);
            categoriesList = [];
        }
    } catch (error) {
        console.error("Could not fetch categories from server:", error);
        categoriesList = [];
    }
    populateCategories();
}

async function loadFertilizers() {
    try {
        const response = await ApiClient.get("/fertilizers");
        if (response && response.success && Array.isArray(response.data)) {
            fertilizers = response.data;
        } else {
            console.warn("Fertilizers API returned empty or invalid structure:", response);
            fertilizers = [];
        }
    } catch (error) {
        console.error("Could not fetch fertilizers from backend API:", error);
        showToast("Error loading catalog from backend database", "error");
        fertilizers = [];
    }

    populateBrands();
    render();
}

/* =========================================================
   DROPDOWN POPULATION
========================================================= */

function populateCategories() {
    const filter = document.getElementById("categoryFilter");
    const modalCategory = document.getElementById("fertilizerCategory");

    const categoryNames = categoriesList.length > 0
        ? categoriesList.map(c => c.name)
        : [...new Set(fertilizers.map(item => item.categoryName || item.category))].filter(Boolean).sort();

    if (filter) {
        const currentVal = filter.value;
        filter.innerHTML = `
            <option value="all">All Categories</option>
            ${categoryNames.map(cat => `
                <option value="${escapeHtml(cat)}">${escapeHtml(cat)}</option>
            `).join("")}
        `;
        if (currentVal && categoryNames.includes(currentVal)) {
            filter.value = currentVal;
        }
    }

    if (modalCategory) {
        modalCategory.innerHTML = `
            <option value="">Select category</option>
            ${categoriesList.map(cat => `
                <option value="${cat.id}">${escapeHtml(cat.name)}</option>
            `).join("")}
        `;
    }
}

function populateBrands() {
    const brandFilter = document.getElementById("brandFilter");
    if (!brandFilter) return;

    const currentVal = brandFilter.value;
    const brands = [...new Set(fertilizers.map(item => item.brand).filter(Boolean))].sort();

    brandFilter.innerHTML = `
        <option value="all">All Brands</option>
        ${brands.map(brand => `
            <option value="${escapeHtml(brand)}">${escapeHtml(brand)}</option>
        `).join("")}
    `;

    if (currentVal && brands.includes(currentVal)) {
        brandFilter.value = currentVal;
    }
}

/* =========================================================
   EVENT BINDINGS
========================================================= */

function bindEvents() {
    const search = document.getElementById("fertilizerSearch");
    const globalSearch = document.getElementById("globalSearch");
    const categoryFilter = document.getElementById("categoryFilter");
    const brandFilter = document.getElementById("brandFilter");
    const statusFilter = document.getElementById("statusFilter");
    const resetFilters = document.getElementById("resetFilters");
    const form = document.getElementById("fertilizerForm");
    const searchClose = document.getElementById("searchClose");
    const topbarSearch = document.getElementById("topbarSearch");

    if (search) {
        search.addEventListener("input", function () {
            currentPage = 1;
            render();
        });
    }

    if (globalSearch) {
        globalSearch.addEventListener("input", function () {
            if (search) search.value = globalSearch.value;
            currentPage = 1;
            render();
        });
    }

    if (categoryFilter) {
        categoryFilter.addEventListener("change", function () {
            currentPage = 1;
            render();
        });
    }

    if (brandFilter) {
        brandFilter.addEventListener("change", function () {
            currentPage = 1;
            render();
        });
    }

    if (statusFilter) {
        statusFilter.addEventListener("change", function () {
            currentPage = 1;
            render();
        });
    }

    if (resetFilters) {
        resetFilters.addEventListener("click", function () {
            if (search) search.value = "";
            if (globalSearch) globalSearch.value = "";
            if (categoryFilter) categoryFilter.value = "all";
            if (brandFilter) brandFilter.value = "all";
            if (statusFilter) statusFilter.value = "all";
            currentPage = 1;
            render();
        });
    }

    if (form) {
        form.addEventListener("submit", saveFertilizer);
    }

    if (searchClose && topbarSearch) {
        searchClose.addEventListener("click", function () {
            topbarSearch.classList.remove("active");
            if (globalSearch) globalSearch.value = "";
            if (search) search.value = "";
            currentPage = 1;
            render();
        });
    }
}

/* =========================================================
   SIDEBAR CONTROLS
========================================================= */

function saToggleSidebar() {
    const side = document.getElementById("side");
    const overlay = document.getElementById("sidebarOverlay");
    if (side) side.classList.toggle("show");
    if (overlay) overlay.classList.toggle("show");
}

function saCloseSidebar() {
    const side = document.getElementById("side");
    const overlay = document.getElementById("sidebarOverlay");
    if (side) side.classList.remove("show");
    if (overlay) overlay.classList.remove("show");
}

/* =========================================================
   STATUS HELPERS
========================================================= */

function getStatus(item) {
    if (Number(item.stock) <= 0) {
        return "Out of Stock";
    }
    const minStock = Number(item.minimumStock || item.reorderLevel || 10);
    if (Number(item.stock) <= minStock) {
        return "Low Stock";
    }
    return "Available";
}

function getStatusClass(status) {
    if (status === "Available") return "status-active";
    if (status === "Low Stock") return "status-warning";
    return "status-danger";
}

/* =========================================================
   FILTERING
========================================================= */

function getFilteredFertilizers() {
    const searchEl = document.getElementById("fertilizerSearch");
    const globalSearchEl = document.getElementById("globalSearch");
    const term = (searchEl?.value || globalSearchEl?.value || "").trim().toLowerCase();

    const categoryFilter = document.getElementById("categoryFilter")?.value || "all";
    const brandFilter = document.getElementById("brandFilter")?.value || "all";
    const statusFilter = document.getElementById("statusFilter")?.value || "all";

    return fertilizers.filter(function (item) {
        const catName = item.categoryName || item.category || "";
        const brand = item.brand || "";
        const sku = item.sku || "";
        const name = item.name || "";
        const status = getStatus(item);

        const matchesSearch = !term ||
            name.toLowerCase().includes(term) ||
            brand.toLowerCase().includes(term) ||
            sku.toLowerCase().includes(term) ||
            catName.toLowerCase().includes(term);

        const matchesCategory = categoryFilter === "all" || catName.toLowerCase() === categoryFilter.toLowerCase();
        const matchesBrand = brandFilter === "all" || brand.toLowerCase() === brandFilter.toLowerCase();
        const matchesStatus = statusFilter === "all" || status.toLowerCase() === statusFilter.toLowerCase();

        return matchesSearch && matchesCategory && matchesBrand && matchesStatus;
    });
}

/* =========================================================
   RENDERING
========================================================= */

function render() {
    const filtered = getFilteredFertilizers();
    const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));

    if (currentPage > totalPages) {
        currentPage = totalPages;
    }

    const start = (currentPage - 1) * pageSize;
    const pageItems = filtered.slice(start, start + pageSize);

    renderTable(pageItems);
    renderPagination(totalPages);
    updateSummary();
    updateResultCount(filtered.length);

    const emptyState = document.getElementById("emptyState");
    if (emptyState) {
        emptyState.classList.toggle("d-none", filtered.length !== 0);
    }
}

function renderTable(items) {
    const body = document.getElementById("fertilizerTableBody");
    if (!body) return;

    if (!items.length) {
        body.innerHTML = "";
        return;
    }

    body.innerHTML = items.map(function (item) {
        const status = getStatus(item);
        const statusClass = getStatusClass(status);
        const buyPrice = Number(item.purchasePrice || item.buyPrice || 0);
        const sellPrice = Number(item.sellingPrice || item.sellPrice || 0);
        const stock = Number(item.stock || 0);
        const reorder = Number(item.minimumStock || item.reorderLevel || 10);
        const catName = item.categoryName || item.category || "-";

        return `
            <tr>
                <td>${item.id}</td>
                <td><span class="product-sku">${escapeHtml(item.sku)}</span></td>
                <td>
                    <span class="product-name fw-bold" style="cursor: pointer;" onclick="viewFertilizerDetails(${item.id})">
                        ${escapeHtml(item.name)}
                    </span>
                </td>
                <td><span class="brand-name">${escapeHtml(item.brand || "-")}</span></td>
                <td><span class="badge bg-light text-dark border">${escapeHtml(catName)}</span></td>
                <td>${escapeHtml(item.unit || "Bag")}</td>
                <td class="price-cell">${formatCurrency(buyPrice)}</td>
                <td class="price-cell font-weight-bold text-success">${formatCurrency(sellPrice)}</td>
                <td class="stock-cell">${stock.toLocaleString("en-IN")}</td>
                <td>${reorder.toLocaleString("en-IN")}</td>
                <td><span class="status-badge ${statusClass}">${status}</span></td>
                <td>
                    <div class="action-buttons d-flex gap-1">
                        <button type="button" class="action-btn view-btn" onclick="viewFertilizerDetails(${item.id})" title="View Product Details">
                            <i class="bi bi-eye"></i> View
                        </button>
                        <button type="button" class="action-btn edit-btn" onclick="editFertilizer(${item.id})" title="Edit Fertilizer">
                            <i class="bi bi-pencil"></i> Edit
                        </button>
                        <button type="button" class="action-btn delete-btn" onclick="deleteFertilizer(${item.id})" title="Delete Fertilizer">
                            <i class="bi bi-trash3"></i> Delete
                        </button>
                    </div>
                </td>
            </tr>
        `;
    }).join("");
}

function renderPagination(totalPages) {
    const pagination = document.getElementById("pagination");
    if (!pagination) return;

    let html = `
        <button type="button" class="page-btn" ${currentPage === 1 ? "disabled" : ""} onclick="changePage(${currentPage - 1})">
            <i class="bi bi-chevron-left"></i>
        </button>
    `;

    for (let page = 1; page <= totalPages; page++) {
        if (totalPages > 7) {
            if (page !== 1 && page !== totalPages && Math.abs(page - currentPage) > 2) {
                if (page === 2 || page === totalPages - 1) {
                    html += `<span class="page-btn disabled">...</span>`;
                }
                continue;
            }
        }
        html += `
            <button type="button" class="page-btn ${page === currentPage ? "active" : ""}" onclick="changePage(${page})">
                ${page}
            </button>
        `;
    }

    html += `
        <button type="button" class="page-btn" ${currentPage === totalPages ? "disabled" : ""} onclick="changePage(${currentPage + 1})">
            <i class="bi bi-chevron-right"></i>
        </button>
    `;

    pagination.innerHTML = html;
}

function changePage(page) {
    const filtered = getFilteredFertilizers();
    const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
    if (page < 1 || page > totalPages) return;
    currentPage = page;
    render();
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function updateSummary() {
    let available = 0;
    let lowStock = 0;
    let outOfStock = 0;

    fertilizers.forEach(function (item) {
        const status = getStatus(item);
        if (status === "Available") available++;
        else if (status === "Low Stock") lowStock++;
        else if (status === "Out of Stock") outOfStock++;
    });

    const sumTotalEl = document.getElementById("summaryTotal");
    const sumAvailEl = document.getElementById("summaryAvailable");
    const sumLowEl = document.getElementById("summaryLow");
    const sumOutEl = document.getElementById("summaryOut");

    if (sumTotalEl) sumTotalEl.textContent = fertilizers.length;
    if (sumAvailEl) sumAvailEl.textContent = available;
    if (sumLowEl) sumLowEl.textContent = lowStock;
    if (sumOutEl) sumOutEl.textContent = outOfStock;
}

function updateResultCount(count) {
    const resCountEl = document.getElementById("resultCount");
    if (resCountEl) {
        resCountEl.textContent = `Showing ${count} of ${fertilizers.length} products`;
    }
}

/* =========================================================
   PRODUCT DETAILS MODAL (STEP 14)
========================================================= */

function viewFertilizerDetails(id) {
    const item = fertilizers.find(f => Number(f.id) === Number(id));
    if (!item) return;

    const sellPrice = Number(item.sellingPrice || item.sellPrice || 0);
    const stock = Number(item.stock || 0);
    const minStock = Number(item.minimumStock || item.reorderLevel || 10);
    const catName = item.categoryName || item.category || "-";
    const status = getStatus(item);
    const statusClass = getStatusClass(status);

    document.getElementById("detailProductName").textContent = item.name;
    document.getElementById("detailProductBrand").textContent = item.brand || "SmartAgri Brand";
    document.getElementById("detailProductSku").textContent = `SKU: ${item.sku}`;
    document.getElementById("detailProductCategory").textContent = catName;

    document.getElementById("detailProductPrice").textContent = formatCurrency(sellPrice);
    const origPriceEl = document.getElementById("detailProductOriginalPrice");
    if (Number(item.discountPercent) > 0) {
        const origPrice = sellPrice / (1 - (Number(item.discountPercent) / 100));
        origPriceEl.textContent = formatCurrency(origPrice);
    } else {
        origPriceEl.textContent = "";
    }

    const statusBadgeContainer = document.getElementById("detailProductStatusBadge");
    statusBadgeContainer.innerHTML = `<span class="status-badge ${statusClass}">${status}</span>`;

    document.getElementById("detailProductUnit").textContent = `${item.unit || "Bag"} (${item.weightValue || "Standard"})`;
    document.getElementById("detailProductStock").textContent = `${stock.toLocaleString("en-IN")} available`;
    document.getElementById("detailProductReorder").textContent = `${minStock.toLocaleString("en-IN")} units`;
    document.getElementById("detailProductTax").textContent = `${item.taxPercent || "5.00"}% GST`;

    document.getElementById("detailProductComposition").textContent = item.composition || "Standard agricultural grade formulation.";
    document.getElementById("detailProductCrops").textContent = item.suitableCrops || "Wheat, Rice, Maize, Sugarcane, Vegetables, Pulses";
    document.getElementById("detailProductSoil").textContent = item.suitableSoil || "Suitable for all soil types (Alluvial, Black, Red, Sandy Loam).";

    const imgEl = document.getElementById("detailProductImage");
    if (imgEl) {
        imgEl.src = item.imageUrl || "../img/img1.jpeg";
    }

    if (productDetailsModal) {
        productDetailsModal.show();
    }
}

function handleAddToCartClick() {
    showToast("Product added to cart! (Order flow will be activated in upcoming Phase)", "success");
}

/* =========================================================
   ADD / EDIT FERTILIZER MODAL
========================================================= */

function openAddFertilizerModal() {
    editingId = null;
    document.getElementById("fertilizerModalTitle").textContent = "Add Fertilizer";
    document.getElementById("saveButtonText").textContent = "Save Fertilizer";
    document.getElementById("fertilizerForm").reset();
    document.getElementById("editId").value = "";
    document.getElementById("formMessage").textContent = "";

    populateCategories();
    fertilizerModal.show();
}

function editFertilizer(id) {
    const item = fertilizers.find(f => Number(f.id) === Number(id));
    if (!item) return;

    editingId = id;
    document.getElementById("fertilizerModalTitle").textContent = "Edit Fertilizer";
    document.getElementById("saveButtonText").textContent = "Update Fertilizer";

    document.getElementById("editId").value = item.id;
    document.getElementById("fertilizerName").value = item.name;
    document.getElementById("fertilizerSku").value = item.sku;
    document.getElementById("fertilizerBrand").value = item.brand || "";

    // Set category select by categoryId or matching name
    const categorySelect = document.getElementById("fertilizerCategory");
    if (item.categoryId) {
        categorySelect.value = item.categoryId;
    } else {
        const match = categoriesList.find(c => c.name.toLowerCase() === (item.categoryName || item.category || "").toLowerCase());
        if (match) categorySelect.value = match.id;
    }

    document.getElementById("fertilizerUnit").value = item.unit || "Bag";
    document.getElementById("fertilizerBuyPrice").value = item.purchasePrice || item.buyPrice || 0;
    document.getElementById("fertilizerSellPrice").value = item.sellingPrice || item.sellPrice || 0;
    document.getElementById("fertilizerStock").value = item.stock || 0;
    document.getElementById("fertilizerReorder").value = item.minimumStock || item.reorderLevel || 10;
    document.getElementById("formMessage").textContent = "";

    fertilizerModal.show();
}

/* =========================================================
   SAVE FERTILIZER (CREATE OR UPDATE VIA REST API)
========================================================= */

async function saveFertilizer(event) {
    event.preventDefault();

    const name = document.getElementById("fertilizerName").value.trim();
    const sku = document.getElementById("fertilizerSku").value.trim();
    const brand = document.getElementById("fertilizerBrand").value.trim();
    const categoryIdVal = document.getElementById("fertilizerCategory").value;
    const unit = document.getElementById("fertilizerUnit").value;
    const buyPrice = Number(document.getElementById("fertilizerBuyPrice").value);
    const sellPrice = Number(document.getElementById("fertilizerSellPrice").value);
    const stock = Number(document.getElementById("fertilizerStock").value);
    const reorderLevel = Number(document.getElementById("fertilizerReorder").value);
    const message = document.getElementById("formMessage");

    if (!name || !sku || !brand || !categoryIdVal || !unit) {
        message.textContent = "Please fill all required details.";
        message.style.color = "#c14444";
        return;
    }

    if (buyPrice < 0 || sellPrice <= 0 || stock < 0 || reorderLevel < 0) {
        message.textContent = "Prices and stock must be valid non-negative numbers (Sell price > 0).";
        message.style.color = "#c14444";
        return;
    }

    const payload = {
        name,
        sku,
        brand,
        categoryId: Number(categoryIdVal),
        unit,
        purchasePrice: buyPrice,
        sellingPrice: sellPrice,
        stock,
        minimumStock: reorderLevel,
        status: stock > 0 ? "ACTIVE" : "OUT_OF_STOCK"
    };

    try {
        message.textContent = "Saving to database...";
        message.style.color = "#2e7d32";

        let response;
        if (editingId) {
            response = await ApiClient.put(`/fertilizers/${editingId}`, payload);
        } else {
            response = await ApiClient.post("/fertilizers", payload);
        }

        if (response && response.success) {
            showToast(editingId ? "Fertilizer updated successfully." : "Fertilizer added successfully.");
            fertilizerModal.hide();
            await loadFertilizers();
        } else {
            message.textContent = response.message || "Failed to save fertilizer.";
            message.style.color = "#c14444";
        }
    } catch (error) {
        console.error("Save fertilizer error:", error);
        message.textContent = error.message || "Error communicating with server.";
        message.style.color = "#c14444";
    }
}

/* =========================================================
   DELETE FERTILIZER (VIA REST API)
========================================================= */

async function deleteFertilizer(id) {
    const item = fertilizers.find(f => Number(f.id) === Number(id));
    const itemName = item ? item.name : `ID #${id}`;

    if (!confirm(`Are you sure you want to delete ${itemName}?`)) {
        return;
    }

    try {
        const response = await ApiClient.delete(`/fertilizers/${id}`);
        if (response && response.success) {
            showToast("Fertilizer deleted successfully.");
            await loadFertilizers();
        } else {
            showToast(response.message || "Failed to delete fertilizer.", "error");
        }
    } catch (error) {
        console.error("Delete fertilizer error:", error);
        showToast(error.message || "Error deleting fertilizer from server.", "error");
    }
}

/* =========================================================
   UTILITIES
========================================================= */

function escapeHtml(text) {
    if (text === null || text === undefined) return "";
    return String(text)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function formatCurrency(value) {
    return new Intl.NumberFormat("en-IN", {
        style: "currency",
        currency: "INR",
        maximumFractionDigits: 2
    }).format(Number(value) || 0);
}

function showToast(message, type = "success") {
    const toastEl = document.getElementById("actionToast");
    const toastMsgEl = document.getElementById("toastMessage");
    if (!toastEl || !toastMsgEl) return;

    toastMsgEl.textContent = message;
    if (typeof bootstrap !== "undefined") {
        const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
        toast.show();
    }
}