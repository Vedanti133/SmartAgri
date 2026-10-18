/**
 * SmartAgri - Customer Catalog & E-Commerce Storefront
 * Phase 6: Advanced Product Search, Filtering, Sorting & Voice Search
 */

// Voice Recognition Configuration (Phase 6 requirement: multilingual prep for Phase 14)
const VOICE_LANGUAGE = "en-IN";

// Catalog State
let currentPage = 0;
const pageSize = 12;
let totalPages = 1;
let totalElements = 0;
let currentProducts = [];
let filterOptions = null;
let wishlistProductIds = new Set();

let searchQuery = '';
let selectedCategory = 'ALL';
let selectedBrand = 'ALL';
let selectedType = 'ALL';
let selectedStock = 'all';
let minPrice = null;
let maxPrice = null;
let selectedNpk = '';
let currentSort = 'relevance';

let searchDebounceTimer = null;
let speechRecognition = null;
let isListening = false;

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Verify user authentication
    if (typeof requireLogin === 'function') {
        requireLogin();
    }

    // 2. Setup event listeners
    initSearchAndControls();
    initVoiceSearch();

    // 3. Load wishlist product IDs, dynamic filter options & catalog products
    await loadWishlistProductIds();
    await loadFilterOptions();
    await fetchAndRenderCatalog();

    // 4. Update cart and wishlist badge counters
    if (typeof updateCartBadge === 'function') {
        await updateCartBadge();
    }
    if (typeof updateWishlistBadge === 'function') {
        await updateWishlistBadge();
    }
});

async function loadWishlistProductIds() {
    try {
        const res = await ApiClient.get('/wishlist/product-ids');
        if (res && res.success && Array.isArray(res.data)) {
            wishlistProductIds = new Set(res.data.map(id => Number(id)));
        }
    } catch (e) {
        console.warn('Could not load wishlist product IDs:', e);
    }
}

/* ==========================================================================
   INITIALIZATION & EVENT LISTENERS
   ========================================================================== */

function initSearchAndControls() {
    const searchInput = document.getElementById('searchInput');
    const clearBtn = document.getElementById('clearSearchBtn');

    if (searchInput) {
        searchInput.addEventListener('input', (e) => {
            searchQuery = e.target.value.trim();
            if (clearBtn) {
                clearBtn.style.display = searchQuery ? 'inline-flex' : 'none';
            }
            // Debounce server search (400ms)
            clearTimeout(searchDebounceTimer);
            searchDebounceTimer = setTimeout(() => {
                currentPage = 0;
                fetchAndRenderCatalog();
            }, 400);
        });

        // Allow pressing Enter for immediate search
        searchInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') {
                e.preventDefault();
                clearTimeout(searchDebounceTimer);
                currentPage = 0;
                fetchAndRenderCatalog();
            }
        });
    }

    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            if (searchInput) {
                searchInput.value = '';
                searchInput.focus();
            }
            searchQuery = '';
            clearBtn.style.display = 'none';
            currentPage = 0;
            fetchAndRenderCatalog();
        });
    }

    const sortSelect = document.getElementById('sortSelect');
    if (sortSelect) {
        sortSelect.addEventListener('change', (e) => {
            currentSort = e.target.value;
            currentPage = 0;
            fetchAndRenderCatalog();
        });
    }
}

/* ==========================================================================
   WEB SPEECH API VOICE SEARCH
   ========================================================================== */

function initVoiceSearch() {
    const voiceBtn = document.getElementById('voiceSearchBtn');
    const stopVoiceBtn = document.getElementById('stopVoiceBtn');
    const voiceIndicator = document.getElementById('voiceIndicator');
    const voiceAlert = document.getElementById('voiceUnsupportedAlert');
    const searchInput = document.getElementById('searchInput');
    const clearBtn = document.getElementById('clearSearchBtn');

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!SpeechRecognition) {
        if (voiceBtn) {
            voiceBtn.addEventListener('click', () => {
                if (voiceAlert) {
                    voiceAlert.style.display = 'block';
                    setTimeout(() => { voiceAlert.style.display = 'none'; }, 6000);
                } else {
                    showToast('Voice search is not supported by your browser. Please use Chrome or Edge.', 'danger');
                }
            });
        }
        return;
    }

    try {
        speechRecognition = new SpeechRecognition();
        speechRecognition.lang = VOICE_LANGUAGE;
        speechRecognition.interimResults = false;
        speechRecognition.maxAlternatives = 1;

        speechRecognition.onstart = () => {
            isListening = true;
            if (voiceBtn) voiceBtn.classList.add('listening');
            if (voiceIndicator) voiceIndicator.classList.add('active');
        };

        speechRecognition.onresult = (event) => {
            const transcript = event.results[0][0].transcript;
            if (transcript) {
                // Strip trailing punctuation like full stops sometimes added by speech engines
                const cleaned = transcript.replace(/[.]+$/, '').trim();
                if (searchInput) {
                    searchInput.value = cleaned;
                }
                searchQuery = cleaned;
                if (clearBtn) {
                    clearBtn.style.display = 'inline-flex';
                }
                currentPage = 0;
                fetchAndRenderCatalog();
                showToast(`Voice Search: "${cleaned}"`, 'success');
            }
        };

        speechRecognition.onerror = (event) => {
            console.warn('Voice recognition error:', event.error);
            stopListening();
            if (event.error !== 'no-speech') {
                showToast(`Voice input: ${event.error}`, 'danger');
            }
        };

        speechRecognition.onend = () => {
            stopListening();
        };

        if (voiceBtn) {
            voiceBtn.addEventListener('click', () => {
                if (isListening) {
                    speechRecognition.stop();
                } else {
                    try {
                        speechRecognition.start();
                    } catch (err) {
                        console.warn('Recognition start exception:', err);
                    }
                }
            });
        }

        if (stopVoiceBtn) {
            stopVoiceBtn.addEventListener('click', () => {
                if (speechRecognition) {
                    speechRecognition.stop();
                }
            });
        }
    } catch (e) {
        console.error('Speech recognition setup failed:', e);
    }
}

function stopListening() {
    isListening = false;
    const voiceBtn = document.getElementById('voiceSearchBtn');
    const voiceIndicator = document.getElementById('voiceIndicator');
    if (voiceBtn) voiceBtn.classList.remove('listening');
    if (voiceIndicator) voiceIndicator.classList.remove('active');
}

/* ==========================================================================
   DYNAMIC FILTER OPTIONS
   ========================================================================== */

async function loadFilterOptions() {
    try {
        const res = await ApiClient.get('/fertilizers/filter-options');
        if (res && res.success && res.data) {
            filterOptions = res.data;
            populateFilterSidebar();
        }
    } catch (e) {
        console.error('Failed to load filter options:', e);
    }
}

function populateFilterSidebar() {
    if (!filterOptions) return;

    // 1. Categories
    const categories = filterOptions.categories || [];
    renderRadioList('desktopCategoryFilterList', 'desktopCategory', categories, 'id', 'name', selectedCategory, onCategoryFilterChange);
    renderRadioList('mobileCategoryFilterList', 'mobileCategory', categories, 'id', 'name', selectedCategory, onCategoryFilterChange);

    // 2. Brands
    const brands = filterOptions.brands || [];
    renderStringRadioList('desktopBrandFilterList', 'desktopBrand', brands, selectedBrand, onBrandFilterChange);
    renderStringRadioList('mobileBrandFilterList', 'mobileBrand', brands, selectedBrand, onBrandFilterChange);

    // 3. Fertilizer Types
    const types = filterOptions.fertilizerTypes || [];
    renderStringRadioList('desktopTypeFilterList', 'desktopType', types, selectedType, onTypeFilterChange);
    renderStringRadioList('mobileTypeFilterList', 'mobileType', types, selectedType, onTypeFilterChange);

    // 4. Min/Max price placeholders
    const minP = filterOptions.minPrice != null ? Math.floor(filterOptions.minPrice) : '';
    const maxP = filterOptions.maxPrice != null ? Math.ceil(filterOptions.maxPrice) : '';

    const dMin = document.getElementById('desktopMinPriceInput');
    const dMax = document.getElementById('desktopMaxPriceInput');
    const mMin = document.getElementById('mobileMinPriceInput');
    const mMax = document.getElementById('mobileMaxPriceInput');

    if (dMin) dMin.placeholder = minP !== '' ? `₹${minP}` : 'Min';
    if (dMax) dMax.placeholder = maxP !== '' ? `₹${maxP}` : 'Max';
    if (mMin) mMin.placeholder = minP !== '' ? `₹${minP}` : 'Min';
    if (mMax) mMax.placeholder = maxP !== '' ? `₹${maxP}` : 'Max';

    // 5. NPK Ratios
    const npkRatios = filterOptions.npkRatios || [];
    renderNpkPills('desktopNpkPills', npkRatios);
    renderNpkPills('mobileNpkPills', npkRatios);
}

function renderRadioList(containerId, groupName, items, valKey, labelKey, selectedVal, onChangeCallback) {
    const el = document.getElementById(containerId);
    if (!el) return;

    let html = `
        <div class="filter-item-check">
            <label>
                <input type="radio" name="${groupName}" value="ALL" ${selectedVal === 'ALL' ? 'checked' : ''} onchange="${onChangeCallback.name}('ALL')">
                All (${filterOptions.totalCount || 'All'})
            </label>
        </div>
    `;

    items.forEach(item => {
        const val = item[valKey];
        const label = item[labelKey];
        const isChecked = String(val) === String(selectedVal);
        html += `
            <div class="filter-item-check">
                <label>
                    <input type="radio" name="${groupName}" value="${val}" ${isChecked ? 'checked' : ''} onchange="${onChangeCallback.name}('${val}')">
                    ${label}
                </label>
            </div>
        `;
    });

    el.innerHTML = html;
}

function renderStringRadioList(containerId, groupName, list, selectedVal, onChangeCallback) {
    const el = document.getElementById(containerId);
    if (!el) return;

    let html = `
        <div class="filter-item-check">
            <label>
                <input type="radio" name="${groupName}" value="ALL" ${selectedVal === 'ALL' ? 'checked' : ''} onchange="${onChangeCallback.name}('ALL')">
                All
            </label>
        </div>
    `;

    list.forEach(item => {
        const isChecked = item === selectedVal;
        html += `
            <div class="filter-item-check">
                <label>
                    <input type="radio" name="${groupName}" value="${item}" ${isChecked ? 'checked' : ''} onchange="${onChangeCallback.name}('${item}')">
                    ${item}
                </label>
            </div>
        `;
    });

    el.innerHTML = html;
}

function renderNpkPills(containerId, npkList) {
    const el = document.getElementById(containerId);
    if (!el) return;

    let html = `
        <button type="button" class="npk-quick-pill ${selectedNpk === '' ? 'active' : ''}" onclick="onNpkFilterChange('')">
            All
        </button>
    `;

    npkList.forEach(ratio => {
        const isActive = selectedNpk === ratio;
        html += `
            <button type="button" class="npk-quick-pill ${isActive ? 'active' : ''}" onclick="onNpkFilterChange('${ratio}')">
                ${ratio}
            </button>
        `;
    });

    el.innerHTML = html;
}

/* ==========================================================================
   FILTER HANDLERS
   ========================================================================== */

function onCategoryFilterChange(val) {
    selectedCategory = val;
    syncRadios('desktopCategory', val);
    syncRadios('mobileCategory', val);
    currentPage = 0;
    fetchAndRenderCatalog();
}

function onBrandFilterChange(val) {
    selectedBrand = val;
    syncRadios('desktopBrand', val);
    syncRadios('mobileBrand', val);
    currentPage = 0;
    fetchAndRenderCatalog();
}

function onTypeFilterChange(val) {
    selectedType = val;
    syncRadios('desktopType', val);
    syncRadios('mobileType', val);
    currentPage = 0;
    fetchAndRenderCatalog();
}

function onStockFilterChange(val) {
    selectedStock = val;
    syncRadios('desktopStock', val);
    syncRadios('mobileStock', val);
    currentPage = 0;
    fetchAndRenderCatalog();
}

function onNpkFilterChange(val) {
    selectedNpk = val;
    if (filterOptions) {
        renderNpkPills('desktopNpkPills', filterOptions.npkRatios || []);
        renderNpkPills('mobileNpkPills', filterOptions.npkRatios || []);
    }
    currentPage = 0;
    fetchAndRenderCatalog();
}

function applyPriceFilter() {
    const dMin = document.getElementById('desktopMinPriceInput');
    const dMax = document.getElementById('desktopMaxPriceInput');
    const mMin = document.getElementById('mobileMinPriceInput');
    const mMax = document.getElementById('mobileMaxPriceInput');

    const minVal = (dMin && dMin.value !== '') ? dMin.value : (mMin ? mMin.value : '');
    const maxVal = (dMax && dMax.value !== '') ? dMax.value : (mMax ? mMax.value : '');

    minPrice = minVal !== '' ? Number(minVal) : null;
    maxPrice = maxVal !== '' ? Number(maxVal) : null;

    // Sync input values
    if (dMin) dMin.value = minVal;
    if (mMin) mMin.value = minVal;
    if (dMax) dMax.value = maxVal;
    if (mMax) mMax.value = maxVal;

    currentPage = 0;
    fetchAndRenderCatalog();
}

function syncRadios(groupName, value) {
    document.querySelectorAll(`input[name="${groupName}"]`).forEach(r => {
        r.checked = (r.value === String(value));
    });
}

function clearAllFilters() {
    searchQuery = '';
    selectedCategory = 'ALL';
    selectedBrand = 'ALL';
    selectedType = 'ALL';
    selectedStock = 'all';
    minPrice = null;
    maxPrice = null;
    selectedNpk = '';
    currentSort = 'relevance';

    // Clear search input & buttons
    const searchInput = document.getElementById('searchInput');
    if (searchInput) searchInput.value = '';
    const clearBtn = document.getElementById('clearSearchBtn');
    if (clearBtn) clearBtn.style.display = 'none';

    // Reset sort dropdown
    const sortSelect = document.getElementById('sortSelect');
    if (sortSelect) sortSelect.value = 'relevance';

    // Reset inputs
    ['desktopMinPriceInput', 'desktopMaxPriceInput', 'mobileMinPriceInput', 'mobileMaxPriceInput'].forEach(id => {
        const el = document.getElementById(id);
        if (el) el.value = '';
    });

    // Reset radios
    syncRadios('desktopCategory', 'ALL');
    syncRadios('mobileCategory', 'ALL');
    syncRadios('desktopBrand', 'ALL');
    syncRadios('mobileBrand', 'ALL');
    syncRadios('desktopType', 'ALL');
    syncRadios('mobileType', 'ALL');
    syncRadios('desktopStock', 'all');
    syncRadios('mobileStock', 'all');

    // Reset NPK
    if (filterOptions) {
        renderNpkPills('desktopNpkPills', filterOptions.npkRatios || []);
        renderNpkPills('mobileNpkPills', filterOptions.npkRatios || []);
    }

    currentPage = 0;
    fetchAndRenderCatalog();
}

function removeFilter(filterKey) {
    switch (filterKey) {
        case 'search':
            searchQuery = '';
            const searchInput = document.getElementById('searchInput');
            if (searchInput) searchInput.value = '';
            const clearBtn = document.getElementById('clearSearchBtn');
            if (clearBtn) clearBtn.style.display = 'none';
            break;
        case 'category':
            selectedCategory = 'ALL';
            syncRadios('desktopCategory', 'ALL');
            syncRadios('mobileCategory', 'ALL');
            break;
        case 'brand':
            selectedBrand = 'ALL';
            syncRadios('desktopBrand', 'ALL');
            syncRadios('mobileBrand', 'ALL');
            break;
        case 'type':
            selectedType = 'ALL';
            syncRadios('desktopType', 'ALL');
            syncRadios('mobileType', 'ALL');
            break;
        case 'stock':
            selectedStock = 'all';
            syncRadios('desktopStock', 'all');
            syncRadios('mobileStock', 'all');
            break;
        case 'price':
            minPrice = null;
            maxPrice = null;
            ['desktopMinPriceInput', 'desktopMaxPriceInput', 'mobileMinPriceInput', 'mobileMaxPriceInput'].forEach(id => {
                const el = document.getElementById(id);
                if (el) el.value = '';
            });
            break;
        case 'npk':
            selectedNpk = '';
            if (filterOptions) {
                renderNpkPills('desktopNpkPills', filterOptions.npkRatios || []);
                renderNpkPills('mobileNpkPills', filterOptions.npkRatios || []);
            }
            break;
    }
    currentPage = 0;
    fetchAndRenderCatalog();
}

/* ==========================================================================
   CATALOG FETCH & RENDER
   ========================================================================== */

async function fetchAndRenderCatalog() {
    const grid = document.getElementById('productsGrid');
    const countText = document.getElementById('resultsCountText');
    const paginationContainer = document.getElementById('paginationContainer');

    if (grid) {
        grid.innerHTML = `
            <div class="col-12 text-center py-5">
                <div class="spinner-border text-success" role="status"></div>
                <p class="mt-2 text-muted">Searching agricultural catalog...</p>
            </div>
        `;
    }

    // Build URL query params
    const params = new URLSearchParams();

    if (searchQuery) params.set('q', searchQuery);
    if (selectedCategory && selectedCategory !== 'ALL') {
        if (/^\d+$/.test(selectedCategory)) {
            params.set('categoryId', selectedCategory);
        } else {
            params.set('category', selectedCategory);
        }
    }
    if (selectedBrand && selectedBrand !== 'ALL') params.set('brand', selectedBrand);
    if (selectedType && selectedType !== 'ALL') params.set('type', selectedType);
    if (selectedStock && selectedStock !== 'all') params.set('stock', selectedStock);
    if (minPrice != null) params.set('minPrice', minPrice);
    if (maxPrice != null) params.set('maxPrice', maxPrice);
    if (selectedNpk) params.set('npk', selectedNpk);
    if (currentSort && currentSort !== 'relevance') params.set('sort', currentSort);

    params.set('page', currentPage);
    params.set('size', pageSize);

    updateActiveFiltersUI();

    try {
        const res = await ApiClient.get(`/fertilizers/search?${params.toString()}`);

        if (res && res.success && res.data) {
            let data = res.data;
            if (data.content && Array.isArray(data.content)) {
                // PageResponse
                currentProducts = data.content;
                totalPages = data.totalPages;
                totalElements = data.totalElements;
            } else if (Array.isArray(data)) {
                // Direct list fallback
                currentProducts = data;
                totalPages = Math.ceil(data.length / pageSize) || 1;
                totalElements = data.length;
            } else {
                currentProducts = [];
                totalPages = 1;
                totalElements = 0;
            }

            renderProductsGrid();
            renderPaginationUI();
            updateResultsCountText();
        } else {
            renderEmptyCatalog(res.message || 'No fertilizers matched your criteria.');
        }
    } catch (e) {
        console.error('Catalog fetch error:', e);
        if (grid) {
            grid.innerHTML = `
                <div class="col-12 text-center text-danger py-5">
                    <i class="bi bi-exclamation-triangle fs-1"></i>
                    <h5 class="mt-3">Unable to search fertilizers</h5>
                    <p class="small text-secondary">${e.message || 'Network or server error'}</p>
                    <button class="btn btn-sm btn-outline-success mt-2" onclick="fetchAndRenderCatalog()">Try Again</button>
                </div>
            `;
        }
    }
}

function updateActiveFiltersUI() {
    const container = document.getElementById('activeFiltersContainer');
    const topClearBtn = document.getElementById('clearAllFiltersTopBtn');
    const badgeCount = document.getElementById('mobileFilterCountBadge');

    if (!container) return;

    const badges = [];

    if (searchQuery) {
        badges.push({ key: 'search', label: `Search: "${searchQuery}"` });
    }
    if (selectedCategory && selectedCategory !== 'ALL') {
        let catLabel = selectedCategory;
        if (filterOptions && filterOptions.categories) {
            const match = filterOptions.categories.find(c => String(c.id) === String(selectedCategory));
            if (match) catLabel = match.name;
        }
        badges.push({ key: 'category', label: `Category: ${catLabel}` });
    }
    if (selectedBrand && selectedBrand !== 'ALL') {
        badges.push({ key: 'brand', label: `Brand: ${selectedBrand}` });
    }
    if (selectedType && selectedType !== 'ALL') {
        badges.push({ key: 'type', label: `Type: ${selectedType}` });
    }
    if (selectedStock && selectedStock !== 'all') {
        const stockLabels = { in_stock: 'In Stock Only', low_stock: 'Low Stock (≤10)', out_of_stock: 'Out of Stock' };
        badges.push({ key: 'stock', label: stockLabels[selectedStock] || selectedStock });
    }
    if (minPrice != null || maxPrice != null) {
        let pLabel = 'Price: ';
        if (minPrice != null && maxPrice != null) pLabel += `₹${minPrice} - ₹${maxPrice}`;
        else if (minPrice != null) pLabel += `≥ ₹${minPrice}`;
        else pLabel += `≤ ₹${maxPrice}`;
        badges.push({ key: 'price', label: pLabel });
    }
    if (selectedNpk) {
        badges.push({ key: 'npk', label: `NPK: ${selectedNpk}` });
    }

    if (badges.length > 0) {
        container.style.display = 'flex';
        if (topClearBtn) topClearBtn.style.display = 'inline-block';
        if (badgeCount) {
            badgeCount.style.display = 'inline-block';
            badgeCount.textContent = badges.length;
        }

        container.innerHTML = badges.map(b => `
            <span class="active-filter-badge">
                ${b.label}
                <button type="button" class="btn-remove-filter" title="Remove filter" onclick="removeFilter('${b.key}')">
                    <i class="bi bi-x"></i>
                </button>
            </span>
        `).join('');
    } else {
        container.style.display = 'none';
        if (topClearBtn) topClearBtn.style.display = 'none';
        if (badgeCount) badgeCount.style.display = 'none';
    }
}

function updateResultsCountText() {
    const countText = document.getElementById('resultsCountText');
    if (!countText) return;

    if (totalElements === 0) {
        countText.textContent = '0 products found';
        return;
    }

    const start = currentPage * pageSize + 1;
    const end = Math.min((currentPage + 1) * pageSize, totalElements);
    countText.innerHTML = `Showing <span class="fw-bold text-success">${start}–${end}</span> of <span class="fw-bold">${totalElements}</span> products`;
}

function renderProductsGrid() {
    const grid = document.getElementById('productsGrid');
    if (!grid) return;

    if (currentProducts.length === 0) {
        renderEmptyCatalog();
        return;
    }

    grid.innerHTML = currentProducts.map(item => {
        const price = Number(item.sellingPrice) || 0;
        const discount = Number(item.discountPercent) || 0;
        let originalPrice = price;
        let discountHtml = '';

        if (discount > 0) {
            originalPrice = price / (1 - (discount / 100));
            discountHtml = `
                <span class="text-decoration-line-through text-muted small me-2">₹${originalPrice.toFixed(2)}</span>
                <span class="badge bg-danger-subtle text-danger small">${discount}% OFF</span>
            `;
        }

        const stock = (item.availableStock !== undefined && item.availableStock !== null) ? Number(item.availableStock) : (Number(item.stock) || 0);
        const threshold = item.lowStockThreshold || item.minimumStock || 10;
        let stockBadge = '';
        let isOutOfStock = stock <= 0;

        if (stock <= 0) {
            stockBadge = '<span class="stock-badge stock-out">Out of Stock</span>';
        } else if (stock <= threshold) {
            stockBadge = `<span class="stock-badge stock-low">Only ${stock} left</span>`;
        } else {
            stockBadge = `<span class="stock-badge stock-in">In Stock (${stock})</span>`;
        }

    const imgSrc = item.imageUrl || `../img/fertilizers/${item.sku}.jpg`;
        const npkBadge = item.npkRatio 
            ? `<span class="npk-badge me-1" title="Nutrient Ratio N-P-K">NPK ${item.npkRatio}</span>` 
            : '';
        const inWish = wishlistProductIds.has(Number(item.id));

        return `
            <div class="col-12 col-sm-6 col-xl-4">
                <div class="product-card shadow-sm">
                    <div class="product-card-img-wrap">
                        <img src="${imgSrc}" alt="${item.name}" onerror="this.src='../img/img1.jpeg'">
                        <div class="position-absolute top-0 start-0 m-2">
                            ${stockBadge}
                        </div>
                        <button type="button" class="btn-wishlist ${inWish ? 'in-wishlist' : ''}" 
                            id="wishlist-card-btn-${item.id}"
                            onclick="toggleWishlist(${item.id}, event)" 
                            title="${inWish ? 'Remove from Wishlist' : 'Add to Wishlist'}">
                            <i class="bi ${inWish ? 'bi-heart-fill' : 'bi-heart'}"></i>
                        </button>
                    </div>
                    <div class="product-card-body">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <span class="badge bg-success-subtle text-success small">${item.categoryName || 'Fertilizer'}</span>
                            <span class="small text-muted fw-semibold">${item.brand || 'SmartAgri'}</span>
                        </div>
                        <h6 class="product-card-title text-truncate" title="${item.name}">${item.name}</h6>
                        
                        <div class="text-muted small mb-2 d-flex align-items-center flex-wrap gap-1">
                            ${npkBadge}
                            <span><i class="bi bi-box me-1"></i>${item.unit || 'Bag'} ${item.weightValue ? '(' + item.weightValue + ')' : ''}</span>
                        </div>
                        
                        <div class="mt-auto pt-2 border-top">
                            <div class="d-flex align-items-baseline mb-2">
                                <span class="fs-5 fw-bold text-success me-2">₹${price.toFixed(2)}</span>
                                ${discountHtml}
                            </div>

                            <div class="d-flex gap-2 align-items-center mt-2">
                                <div class="quantity-control">
                                    <button type="button" class="qty-btn" onclick="decrementCatalogQty(${item.id})" ${isOutOfStock ? 'disabled' : ''}>-</button>
                                    <input type="number" id="qty-input-${item.id}" class="qty-input" value="1" min="1" max="${stock}" readonly>
                                    <button type="button" class="qty-btn" onclick="incrementCatalogQty(${item.id}, ${stock})" ${isOutOfStock ? 'disabled' : ''}>+</button>
                                </div>
                                
                                <button type="button" class="btn btn-success btn-sm flex-grow-1" 
                                    id="add-btn-${item.id}"
                                    onclick="addToCart(${item.id})" ${isOutOfStock ? 'disabled' : ''}>
                                    <i class="bi bi-cart-plus me-1"></i> Add
                                </button>
                            </div>

                            <button type="button" class="btn btn-link btn-sm text-decoration-none text-muted w-100 mt-2 p-0" onclick="showProductDetails(${item.id})">
                                <i class="bi bi-info-circle me-1"></i> Specifications & Details
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function renderEmptyCatalog(customMsg) {
    const grid = document.getElementById('productsGrid');
    const paginationContainer = document.getElementById('paginationContainer');
    if (paginationContainer) paginationContainer.style.display = 'none';

    if (grid) {
        grid.innerHTML = `
            <div class="col-12 text-center py-5">
                <i class="bi bi-search fs-1 text-muted"></i>
                <h5 class="mt-3 text-muted">${customMsg || 'No fertilizers matched your criteria'}</h5>
                <p class="text-secondary small">Try clearing some filters or searching for alternative names or nutrient terms.</p>
                <button type="button" class="btn btn-outline-success btn-sm mt-2" onclick="clearAllFilters()">
                    <i class="bi bi-arrow-counterclockwise me-1"></i> Reset All Filters
                </button>
            </div>
        `;
    }
}

/* ==========================================================================
   PAGINATION
   ========================================================================== */

function renderPaginationUI() {
    const container = document.getElementById('paginationContainer');
    if (!container) return;

    if (totalPages <= 1) {
        container.style.display = 'none';
        return;
    }

    container.style.display = 'flex';

    let html = `
        <button type="button" class="page-btn" ${currentPage === 0 ? 'disabled' : ''} onclick="goToPage(${currentPage - 1})">
            <i class="bi bi-chevron-left"></i>
        </button>
    `;

    // Window of pages around current page
    const startPage = Math.max(0, currentPage - 2);
    const endPage = Math.min(totalPages - 1, currentPage + 2);

    if (startPage > 0) {
        html += `<button type="button" class="page-btn" onclick="goToPage(0)">1</button>`;
        if (startPage > 1) {
            html += `<span class="px-1 text-muted">...</span>`;
        }
    }

    for (let p = startPage; p <= endPage; p++) {
        html += `
            <button type="button" class="page-btn ${p === currentPage ? 'active' : ''}" onclick="goToPage(${p})">
                ${p + 1}
            </button>
        `;
    }

    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            html += `<span class="px-1 text-muted">...</span>`;
        }
        html += `<button type="button" class="page-btn" onclick="goToPage(${totalPages - 1})">${totalPages}</button>`;
    }

    html += `
        <button type="button" class="page-btn" ${currentPage >= totalPages - 1 ? 'disabled' : ''} onclick="goToPage(${currentPage + 1})">
            <i class="bi bi-chevron-right"></i>
        </button>
    `;

    container.innerHTML = html;
}

function goToPage(page) {
    if (page < 0 || page >= totalPages) return;
    currentPage = page;
    fetchAndRenderCatalog();
    window.scrollTo({ top: 250, behavior: 'smooth' });
}

/* ==========================================================================
   QUANTITY & CART INTEGRATION
   ========================================================================== */

function incrementCatalogQty(id, max) {
    const input = document.getElementById(`qty-input-${id}`);
    if (input) {
        let val = parseInt(input.value) || 1;
        if (val < max) {
            input.value = val + 1;
        }
    }
}

function decrementCatalogQty(id) {
    const input = document.getElementById(`qty-input-${id}`);
    if (input) {
        let val = parseInt(input.value) || 1;
        if (val > 1) {
            input.value = val - 1;
        }
    }
}

async function addToCart(fertilizerId, explicitQty = null) {
    let qty = explicitQty;
    if (qty === null) {
        const input = document.getElementById(`qty-input-${fertilizerId}`);
        qty = input ? parseInt(input.value) || 1 : 1;
    }

    const btn = document.getElementById(`add-btn-${fertilizerId}`);
    const origHtml = btn ? btn.innerHTML : '';
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm" role="status"></span>';
    }

    try {
        const res = await ApiClient.post('/cart/items', {
            fertilizerId: fertilizerId,
            quantity: qty
        });

        if (res && res.success) {
            showToast(`Added ${qty} item(s) to your cart!`, 'success');
            if (typeof updateCartBadge === 'function') {
                updateCartBadge();
            }
        } else {
            showToast(res.message || 'Unable to add item to cart', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Error adding to cart', 'danger');
    } finally {
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = origHtml;
        }
    }
}

/* ==========================================================================
   PRODUCT DETAILS MODAL
   ========================================================================== */

function showProductDetails(id) {
    const item = currentProducts.find(f => f.id === id);
    if (!item) return;

    const modalName = document.getElementById('modalProductName');
    const modalBody = document.getElementById('modalProductBody');

    modalName.textContent = item.name;

    const price = Number(item.sellingPrice) || 0;
    const discount = Number(item.discountPercent) || 0;
    const stock = (item.availableStock !== undefined && item.availableStock !== null) ? Number(item.availableStock) : (Number(item.stock) || 0);
    const threshold = item.lowStockThreshold || item.minimumStock || 10;
    const isOutOfStock = stock <= 0;

    const npkHtml = item.npkRatio 
        ? `<div class="product-detail-section-title">NPK Nutrient Ratio</div>
           <div class="product-detail-info-box fw-bold text-primary">NPK ${item.npkRatio}</div>` 
        : '';

    modalBody.innerHTML = `
        <div class="row g-4">
            <div class="col-md-5 text-center">
                <div class="product-detail-img-wrap mb-3">
                    <img src="${item.imageUrl || `../img/fertilizers/${item.sku}.jpg`}" 
     alt="${item.name}" 
     class="img-fluid" 
     onerror="this.src='../img/img1.jpeg'">
                </div>
                <div class="fs-3 fw-bold text-success">₹${price.toFixed(2)}</div>
                ${discount > 0 ? `<div class="badge bg-danger small mb-2">${discount}% Promotional Discount</div>` : ''}
                <div class="mt-2">
                    ${isOutOfStock 
                        ? '<span class="badge bg-danger">Out of Stock</span>' 
                        : (stock <= threshold
                            ? `<span class="badge bg-warning text-dark">Only ${stock} left</span>`
                            : `<span class="badge bg-success">In Stock (${stock} available)</span>`)}
                </div>
            </div>
            <div class="col-md-7">
                <h5 class="fw-bold mb-1">${item.name}</h5>
                <p class="text-muted small mb-3">
                    Brand: <strong>${item.brand || 'SmartAgri'}</strong> | 
                    Type: <strong>${item.type || 'Standard'}</strong> | 
                    Category: <strong>${item.categoryName || 'General'}</strong> | 
                    SKU: <strong>${item.sku}</strong>
                </p>

                ${npkHtml}

                <div class="product-detail-section-title">Chemical / Nutrient Composition</div>
                <div class="product-detail-info-box">${item.composition || 'Standard agricultural formulation.'}</div>

                <div class="product-detail-section-title">Recommended Crops</div>
                <div class="product-detail-info-box">${item.suitableCrops || 'Suitable for all cereal, legume, fruit and vegetable crops.'}</div>

                <div class="product-detail-section-title">Soil Compatibility</div>
                <div class="product-detail-info-box">${item.suitableSoil || 'Compatible with Black, Alluvial, Red, and Sandy Loam soils.'}</div>

                <div class="product-detail-section-title">Usage Guidelines</div>
                <p class="small text-secondary mb-4">${item.usageInstructions || 'Apply according to recommended soil testing report and crop growth stage.'}</p>

                <div class="d-flex flex-wrap gap-2 align-items-center pt-3 border-top">
                    <div class="quantity-control">
                        <button type="button" class="qty-btn" onclick="decrementModalQty()" ${isOutOfStock ? 'disabled' : ''}>-</button>
                        <input type="number" id="modalQtyInput" class="qty-input" value="1" min="1" max="${stock}" readonly>
                        <button type="button" class="qty-btn" onclick="incrementModalQty(${stock})" ${isOutOfStock ? 'disabled' : ''}>+</button>
                    </div>
                    <button type="button" class="btn btn-success fw-semibold px-4" id="modalAddCartBtn" 
                        onclick="addFromModal(${item.id})" ${isOutOfStock ? 'disabled' : ''}>
                        <i class="bi bi-cart-plus me-1"></i> Add to Cart
                    </button>
                    <button type="button" class="btn btn-outline-danger fw-semibold px-3 ${wishlistProductIds.has(Number(item.id)) ? 'active' : ''}" id="modalWishlistBtn"
                        onclick="toggleWishlist(${item.id}, event, true)">
                        <i class="bi ${wishlistProductIds.has(Number(item.id)) ? 'bi-heart-fill' : 'bi-heart'} me-1"></i>
                        <span id="modalWishlistText">${wishlistProductIds.has(Number(item.id)) ? 'In Wishlist' : 'Add to Wishlist'}</span>
                    </button>
                </div>
            </div>
        </div>
    `;

    const modal = new bootstrap.Modal(document.getElementById('productModal'));
    modal.show();
}

/* ==========================================================================
   WISHLIST INTEGRATION
   ========================================================================== */

async function toggleWishlist(productId, event, isModal = false) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }

    const pId = Number(productId);
    const inWish = wishlistProductIds.has(pId);
    const cardBtn = document.getElementById(`wishlist-card-btn-${pId}`);
    const modalBtn = document.getElementById('modalWishlistBtn');

    try {
        if (inWish) {
            const res = await ApiClient.delete(`/wishlist/items/product/${pId}`);
            if (res && res.success) {
                wishlistProductIds.delete(pId);
                showToast('Removed from Wishlist', 'success');
            } else {
                showToast(res.message || 'Unable to remove from Wishlist', 'danger');
                return;
            }
        } else {
            const res = await ApiClient.post('/wishlist/items', { fertilizerId: pId });
            if (res && res.success) {
                wishlistProductIds.add(pId);
                showToast('Added to Wishlist!', 'success');
            } else {
                showToast(res.message || 'Unable to add to Wishlist', 'danger');
                return;
            }
        }

        const nowInWish = wishlistProductIds.has(pId);
        if (cardBtn) {
            cardBtn.className = `btn-wishlist ${nowInWish ? 'in-wishlist' : ''}`;
            cardBtn.title = nowInWish ? 'Remove from Wishlist' : 'Add to Wishlist';
            cardBtn.innerHTML = `<i class="bi ${nowInWish ? 'bi-heart-fill' : 'bi-heart'}"></i>`;
        }
        if (modalBtn) {
            if (nowInWish) {
                modalBtn.classList.add('active');
                modalBtn.innerHTML = `<i class="bi bi-heart-fill me-1"></i><span id="modalWishlistText">In Wishlist</span>`;
            } else {
                modalBtn.classList.remove('active');
                modalBtn.innerHTML = `<i class="bi bi-heart me-1"></i><span id="modalWishlistText">Add to Wishlist</span>`;
            }
        }

        if (typeof updateWishlistBadge === 'function') {
            await updateWishlistBadge();
        }
    } catch (e) {
        showToast(e.message || 'Wishlist operation failed', 'danger');
    }
}

function incrementModalQty(max) {
    const input = document.getElementById('modalQtyInput');
    if (input) {
        let val = parseInt(input.value) || 1;
        if (val < max) input.value = val + 1;
    }
}

function decrementModalQty() {
    const input = document.getElementById('modalQtyInput');
    if (input) {
        let val = parseInt(input.value) || 1;
        if (val > 1) input.value = val - 1;
    }
}

async function addFromModal(fertilizerId) {
    const input = document.getElementById('modalQtyInput');
    const qty = input ? parseInt(input.value) || 1 : 1;
    await addToCart(fertilizerId, qty);
    const modalEl = document.getElementById('productModal');
    const modalInstance = bootstrap.Modal.getInstance(modalEl);
    if (modalInstance) {
        modalInstance.hide();
    }
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
