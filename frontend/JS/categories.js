/* =========================================================
   SMARTAGRI CATEGORY MANAGEMENT - PHASE 2
   Connected directly to Spring Boot REST APIs (ApiClient)
   MySQL is the definitive source of truth.
========================================================= */

let categories = [];
let currentPage = 1;
let pageSize = 10;
let editingId = null;
let categoryModal = null;

/* =========================================================
   INITIALIZE
========================================================= */

document.addEventListener("DOMContentLoaded", async function () {
    const modalEl = document.getElementById("categoryModal");
    if (modalEl && typeof bootstrap !== "undefined") {
        categoryModal = new bootstrap.Modal(modalEl);
    }

    bindEvents();
    await loadCategories();
});

/* =========================================================
   LOAD CATEGORIES VIA REST API
========================================================= */

async function loadCategories() {
    try {
        const response = await ApiClient.get("/categories");
        if (response && response.success && Array.isArray(response.data)) {
            categories = response.data;
        } else {
            console.warn("Categories API returned empty or invalid response:", response);
            categories = [];
        }
    } catch (error) {
        console.error("Unable to load categories from backend API:", error);
        showToast("Error loading categories from database", "error");
        categories = [];
    }

    render();
}

/* =========================================================
   EVENTS
========================================================= */

function bindEvents() {
    const addButton = document.getElementById("addCategoryBtn");
    const tableSearch = document.getElementById("tableSearch");
    const topSearch = document.getElementById("searchInput");
    const statusFilter = document.getElementById("statusFilter");
    const resetButton = document.getElementById("resetBtn");
    const clearTableSearch = document.getElementById("clearTableSearch");
    const clearTopSearch = document.getElementById("clearTopSearch");
    const form = document.getElementById("categoryForm");
    const menuButton = document.getElementById("menuBtn");
    const searchBox = document.getElementById("searchBox");

    if (addButton) {
        addButton.addEventListener("click", openAddCategoryModal);
    }

    if (tableSearch) {
        tableSearch.addEventListener("input", function () {
            if (topSearch) topSearch.value = tableSearch.value;
            currentPage = 1;
            render();
        });
    }

    if (topSearch) {
        topSearch.addEventListener("input", function () {
            if (tableSearch) tableSearch.value = topSearch.value;
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

    if (resetButton) {
        resetButton.addEventListener("click", function () {
            if (tableSearch) tableSearch.value = "";
            if (topSearch) topSearch.value = "";
            if (statusFilter) statusFilter.value = "all";
            currentPage = 1;
            render();
        });
    }

    if (clearTableSearch) {
        clearTableSearch.addEventListener("click", function () {
            if (tableSearch) tableSearch.value = "";
            if (topSearch) topSearch.value = "";
            currentPage = 1;
            render();
        });
    }

    if (clearTopSearch) {
        clearTopSearch.addEventListener("click", function () {
            if (topSearch) topSearch.value = "";
            if (tableSearch) tableSearch.value = "";
            currentPage = 1;
            render();
        });
    }

    if (form) {
        form.addEventListener("submit", saveCategory);
    }

    if (menuButton) {
        menuButton.addEventListener("click", toggleSidebar);
    }

    if (searchBox) {
        searchBox.addEventListener("click", function () {
            if (window.innerWidth <= 767) {
                searchBox.classList.add("active");
                if (topSearch) topSearch.focus();
            }
        });
    }

    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape") {
            closeSidebar();
            if (searchBox) searchBox.classList.remove("active");
        }
    });
}

/* =========================================================
   SIDEBAR
========================================================= */

function toggleSidebar() {
    const side = document.getElementById("side");
    const overlay = document.getElementById("sidebarOverlay");
    if (!side || !overlay) return;
    side.classList.toggle("show");
    overlay.classList.toggle("show");
}

function closeSidebar() {
    const side = document.getElementById("side");
    const overlay = document.getElementById("sidebarOverlay");
    if (!side || !overlay) return;
    side.classList.remove("show");
    overlay.classList.remove("show");
}

/* =========================================================
   FILTERING
========================================================= */

function getFilteredCategories() {
    const searchInput = document.getElementById("tableSearch") || document.getElementById("searchInput");
    const statusInput = document.getElementById("statusFilter");
    const search = searchInput ? searchInput.value.trim().toLowerCase() : "";
    const status = statusInput ? statusInput.value : "all";

    return categories.filter(function (category) {
        const name = category.name || "";
        const desc = category.description || "";
        const catStatus = category.status || "ACTIVE";
        const productCount = category.productCount || 0;

        const searchableText = `${name} ${desc} ${catStatus} ${productCount}`.toLowerCase();
        const matchesSearch = !search || searchableText.includes(search);
        const matchesStatus = status === "all" || catStatus.toLowerCase() === status.toLowerCase();

        return matchesSearch && matchesStatus;
    });
}

/* =========================================================
   RENDER
========================================================= */

function render() {
    const filtered = getFilteredCategories();
    const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));

    if (currentPage > totalPages) {
        currentPage = totalPages;
    }

    const start = (currentPage - 1) * pageSize;
    const pageItems = filtered.slice(start, start + pageSize);

    renderTable(pageItems);
    renderPagination(totalPages);
    updateSummary(filtered.length);
}

function renderTable(items) {
    const tbody = document.getElementById("categoryTableBody");
    const noResults = document.getElementById("noResults");
    if (!tbody) return;

    if (!items.length) {
        tbody.innerHTML = "";
        if (noResults) noResults.classList.remove("d-none");
        return;
    }

    if (noResults) {
        noResults.classList.add("d-none");
    }

    tbody.innerHTML = items.map(function (category) {
        const productCount = category.productCount || 0;
        const statusClass = (category.status && category.status.toUpperCase() === "ACTIVE")
            ? "status-active"
            : "status-inactive";

        const createdDate = category.createdAt
            ? new Date(category.createdAt).toLocaleDateString("en-GB", { day: "2-digit", month: "short", year: "numeric" })
            : "22 Sep 2026";

        return `
            <tr>
                <td>${category.id}</td>
                <td>
                    <span class="category-name fw-semibold">${escapeHtml(category.name)}</span>
                </td>
                <td>
                    <span class="category-description" title="${escapeHtml(category.description || "")}">
                        ${escapeHtml(category.description || "-")}
                    </span>
                </td>
                <td>
                    <span class="badge bg-light text-dark border px-2 py-1">
                        ${productCount} product${productCount === 1 ? "" : "s"}
                    </span>
                </td>
                <td>
                    <span class="status-badge ${statusClass}">
                        ${escapeHtml(category.status || "ACTIVE")}
                    </span>
                </td>
                <td>${escapeHtml(createdDate)}</td>
                <td>
                    <div class="action-buttons d-flex gap-1">
                        <button type="button" class="action-btn edit-btn" onclick="openEditCategoryModal(${category.id})" title="Edit category">
                            <i class="bi bi-pencil"></i> Edit
                        </button>
                        <button type="button" class="action-btn delete-btn" onclick="deleteCategory(${category.id})" title="Delete category">
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
    const filtered = getFilteredCategories();
    const totalPages = Math.max(1, Math.ceil(filtered.length / pageSize));
    if (page < 1 || page > totalPages) return;
    currentPage = page;
    render();
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function updateSummary(filteredCount) {
    const summaryTotal = document.getElementById("summaryTotal");
    const summaryActive = document.getElementById("summaryActive");
    const summaryInactive = document.getElementById("summaryInactive");
    const showingCount = document.getElementById("showingCount");
    const totalEntries = document.getElementById("totalEntries");

    const activeCount = categories.filter(c => (c.status || "ACTIVE").toUpperCase() === "ACTIVE").length;
    const inactiveCount = categories.length - activeCount;

    if (summaryTotal) summaryTotal.textContent = categories.length;
    if (summaryActive) summaryActive.textContent = activeCount;
    if (summaryInactive) summaryInactive.textContent = inactiveCount;
    if (showingCount) showingCount.textContent = filteredCount;
    if (totalEntries) totalEntries.textContent = categories.length;
}

/* =========================================================
   ADD / EDIT MODALS
========================================================= */

function openAddCategoryModal() {
    editingId = null;
    document.getElementById("modalTitle").textContent = "Add Category";
    document.getElementById("categoryForm").reset();
    document.getElementById("categoryId").value = "";
    document.getElementById("categoryStatus").value = "ACTIVE";
    const prodInput = document.getElementById("categoryProducts");
    if (prodInput) prodInput.value = "0";

    if (categoryModal) categoryModal.show();
}

function openEditCategoryModal(id) {
    const category = categories.find(c => Number(c.id) === Number(id));
    if (!category) return;

    editingId = id;
    document.getElementById("modalTitle").textContent = "Edit Category";
    document.getElementById("categoryId").value = category.id;
    document.getElementById("categoryName").value = category.name;
    document.getElementById("categoryDescription").value = category.description || "";
    document.getElementById("categoryStatus").value = (category.status || "ACTIVE").toUpperCase() === "ACTIVE" ? "ACTIVE" : "INACTIVE";

    const prodInput = document.getElementById("categoryProducts");
    if (prodInput) prodInput.value = category.productCount || 0;

    if (categoryModal) categoryModal.show();
}

/* =========================================================
   SAVE CATEGORY (CREATE / UPDATE VIA REST API)
========================================================= */

async function saveCategory(event) {
    event.preventDefault();

    const name = document.getElementById("categoryName").value.trim();
    const description = document.getElementById("categoryDescription").value.trim();
    const statusVal = document.getElementById("categoryStatus").value;

    if (!name) {
        showToast("Category name is required.", "error");
        return;
    }

    const payload = {
        name,
        description,
        status: statusVal.toUpperCase()
    };

    try {
        let response;
        if (editingId) {
            response = await ApiClient.put(`/categories/${editingId}`, payload);
        } else {
            response = await ApiClient.post("/categories", payload);
        }

        if (response && response.success) {
            showToast(editingId ? "Category updated successfully." : "Category created successfully.");
            if (categoryModal) categoryModal.hide();
            await loadCategories();
        } else {
            showToast(response.message || "Failed to save category.", "error");
        }
    } catch (error) {
        console.error("Save category error:", error);
        showToast(error.message || "Error communicating with server.", "error");
    }
}

/* =========================================================
   DELETE CATEGORY (VIA REST API)
========================================================= */

async function deleteCategory(id) {
    const category = categories.find(c => Number(c.id) === Number(id));
    const catName = category ? category.name : `ID #${id}`;

    if (!confirm(`Are you sure you want to delete category "${catName}"?`)) {
        return;
    }

    try {
        const response = await ApiClient.delete(`/categories/${id}`);
        if (response && response.success) {
            showToast("Category deleted successfully.");
            await loadCategories();
        } else {
            showToast(response.message || "Failed to delete category.", "error");
        }
    } catch (error) {
        console.error("Delete category error:", error);
        showToast(error.message || "Error deleting category from server.", "error");
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

function showToast(message, type = "success") {
    const toastEl = document.getElementById("categoryToast");
    const toastMsgEl = document.getElementById("toastMessage");
    if (!toastEl || !toastMsgEl) return;

    toastMsgEl.textContent = message;
    if (typeof bootstrap !== "undefined") {
        const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
        toast.show();
    }
}