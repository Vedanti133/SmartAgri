/* =========================================================
   SMARTAGRI STOCK IN
   CONNECTED TO 50 FERTILIZERS
========================================================= */

const PRIMARY_FERTILIZER_KEY =
    "smartagri_fertilizers_v2";

const STOCK_IN_STORAGE_KEY =
    "smartagri_stock_in_records_v1";

const STOCK_IN_SEEDED_KEY =
    "smartagri_stock_in_seeded_v1";


let fertilizers = [];

let fertilizerStorageKey =
    PRIMARY_FERTILIZER_KEY;

let stockInRecords = [];


/* =========================================================
   DEFAULT 50 FERTILIZER DATA
========================================================= */

const defaultFertilizers = [

    {
        id: 1,
        sku: "FRT-001",
        name: "Urea",
        brand: "IFFCO",
        category: "Nitrogen",
        unit: "Bag",
        buyPrice: 260,
        sellPrice: 290,
        stock: 150,
        reorderLevel: 50
    },

    {
        id: 2,
        sku: "FRT-002",
        name: "DAP",
        brand: "Coromandel",
        category: "Phosphate",
        unit: "Bag",
        buyPrice: 1150,
        sellPrice: 1280,
        stock: 20,
        reorderLevel: 50
    },

    {
        id: 3,
        sku: "FRT-003",
        name: "NPK 10:26:26",
        brand: "IFFCO",
        category: "NPK",
        unit: "Bag",
        buyPrice: 1350,
        sellPrice: 1490,
        stock: 90,
        reorderLevel: 35
    },

    {
        id: 4,
        sku: "FRT-004",
        name: "NPK 20:20:20",
        brand: "GSFC",
        category: "NPK",
        unit: "Bag",
        buyPrice: 1420,
        sellPrice: 1580,
        stock: 0,
        reorderLevel: 30
    },

    {
        id: 5,
        sku: "FRT-005",
        name: "MOP",
        brand: "IPL",
        category: "Potassic",
        unit: "Bag",
        buyPrice: 980,
        sellPrice: 1090,
        stock: 75,
        reorderLevel: 25
    },

    {
        id: 6,
        sku: "FRT-006",
        name: "SOP",
        brand: "Deepak Fertilizers",
        category: "Potassic",
        unit: "Bag",
        buyPrice: 1650,
        sellPrice: 1800,
        stock: 18,
        reorderLevel: 25
    },

    {
        id: 7,
        sku: "FRT-007",
        name: "SSP",
        brand: "Paradeep",
        category: "Phosphate",
        unit: "Bag",
        buyPrice: 410,
        sellPrice: 475,
        stock: 105,
        reorderLevel: 40
    },

    {
        id: 8,
        sku: "FRT-008",
        name: "Ammonium Sulphate",
        brand: "FACT",
        category: "Nitrogen",
        unit: "Bag",
        buyPrice: 580,
        sellPrice: 650,
        stock: 62,
        reorderLevel: 25
    },

    {
        id: 9,
        sku: "FRT-009",
        name: "Ammonium Nitrate",
        brand: "RCF",
        category: "Nitrogen",
        unit: "Bag",
        buyPrice: 720,
        sellPrice: 805,
        stock: 42,
        reorderLevel: 25
    },

    {
        id: 10,
        sku: "FRT-010",
        name: "CAN",
        brand: "GSFC",
        category: "Nitrogen",
        unit: "Bag",
        buyPrice: 630,
        sellPrice: 710,
        stock: 34,
        reorderLevel: 20
    },

    {
        id: 11,
        sku: "FRT-011",
        name: "NPK 12:32:16",
        brand: "IFFCO",
        category: "NPK",
        unit: "Bag",
        buyPrice: 1280,
        sellPrice: 1430,
        stock: 88,
        reorderLevel: 30
    },

    {
        id: 12,
        sku: "FRT-012",
        name: "NPK 15:15:15",
        brand: "Zuari",
        category: "NPK",
        unit: "Bag",
        buyPrice: 1120,
        sellPrice: 1250,
        stock: 58,
        reorderLevel: 25
    },

    {
        id: 13,
        sku: "FRT-013",
        name: "NPK 19:19:19",
        brand: "Aries",
        category: "NPK",
        unit: "Kg",
        buyPrice: 92,
        sellPrice: 110,
        stock: 140,
        reorderLevel: 50
    },

    {
        id: 14,
        sku: "FRT-014",
        name: "NPK 13:40:13",
        brand: "Coromandel",
        category: "Water Soluble",
        unit: "Kg",
        buyPrice: 115,
        sellPrice: 135,
        stock: 72,
        reorderLevel: 25
    },

    {
        id: 15,
        sku: "FRT-015",
        name: "NPK 00:52:34",
        brand: "Haifa",
        category: "Water Soluble",
        unit: "Kg",
        buyPrice: 180,
        sellPrice: 215,
        stock: 36,
        reorderLevel: 20
    },

    {
        id: 16,
        sku: "FRT-016",
        name: "Calcium Nitrate",
        brand: "Yara",
        category: "Water Soluble",
        unit: "Kg",
        buyPrice: 145,
        sellPrice: 175,
        stock: 64,
        reorderLevel: 20
    },

    {
        id: 17,
        sku: "FRT-017",
        name: "Magnesium Sulphate",
        brand: "GreenGrow",
        category: "Micronutrient",
        unit: "Kg",
        buyPrice: 52,
        sellPrice: 68,
        stock: 95,
        reorderLevel: 30
    },

    {
        id: 18,
        sku: "FRT-018",
        name: "Zinc Sulphate",
        brand: "Coromandel",
        category: "Micronutrient",
        unit: "Kg",
        buyPrice: 68,
        sellPrice: 84,
        stock: 120,
        reorderLevel: 40
    },

    {
        id: 19,
        sku: "FRT-019",
        name: "Ferrous Sulphate",
        brand: "AgroStar",
        category: "Micronutrient",
        unit: "Kg",
        buyPrice: 49,
        sellPrice: 65,
        stock: 44,
        reorderLevel: 20
    },

    {
        id: 20,
        sku: "FRT-020",
        name: "Boron 20%",
        brand: "Multiplex",
        category: "Micronutrient",
        unit: "Kg",
        buyPrice: 125,
        sellPrice: 148,
        stock: 27,
        reorderLevel: 25
    },

    {
        id: 21,
        sku: "FRT-021",
        name: "Boron 10.5%",
        brand: "Aries",
        category: "Micronutrient",
        unit: "Kg",
        buyPrice: 95,
        sellPrice: 115,
        stock: 51,
        reorderLevel: 20
    },

    {
        id: 22,
        sku: "FRT-022",
        name: "Manganese Sulphate",
        brand: "IPL",
        category: "Micronutrient",
        unit: "Kg",
        buyPrice: 72,
        sellPrice: 89,
        stock: 38,
        reorderLevel: 20
    },

    {
        id: 23,
        sku: "FRT-023",
        name: "Sulphur Bentonite",
        brand: "Deepak",
        category: "Sulphur",
        unit: "Bag",
        buyPrice: 780,
        sellPrice: 890,
        stock: 56,
        reorderLevel: 20
    },

    {
        id: 24,
        sku: "FRT-024",
        name: "Elemental Sulphur",
        brand: "Paradeep",
        category: "Sulphur",
        unit: "Bag",
        buyPrice: 620,
        sellPrice: 720,
        stock: 41,
        reorderLevel: 20
    },

    {
        id: 25,
        sku: "FRT-025",
        name: "Neem Cake",
        brand: "Organic India",
        category: "Organic",
        unit: "Bag",
        buyPrice: 540,
        sellPrice: 650,
        stock: 48,
        reorderLevel: 15
    },

    {
        id: 26,
        sku: "FRT-026",
        name: "Vermicompost",
        brand: "Green Earth",
        category: "Organic",
        unit: "Bag",
        buyPrice: 280,
        sellPrice: 350,
        stock: 85,
        reorderLevel: 25
    },

    {
        id: 27,
        sku: "FRT-027",
        name: "Farmyard Manure",
        brand: "Kisan Organics",
        category: "Organic",
        unit: "Bag",
        buyPrice: 180,
        sellPrice: 230,
        stock: 110,
        reorderLevel: 30
    },

    {
        id: 28,
        sku: "FRT-028",
        name: "Seaweed Extract",
        brand: "Multiplex",
        category: "Bio-Fertilizer",
        unit: "Litre",
        buyPrice: 210,
        sellPrice: 260,
        stock: 33,
        reorderLevel: 15
    },

    {
        id: 29,
        sku: "FRT-029",
        name: "Humic Acid",
        brand: "Aries",
        category: "Bio-Fertilizer",
        unit: "Litre",
        buyPrice: 195,
        sellPrice: 240,
        stock: 60,
        reorderLevel: 20
    },

    {
        id: 30,
        sku: "FRT-030",
        name: "Fulvic Acid",
        brand: "AgriLife",
        category: "Bio-Fertilizer",
        unit: "Litre",
        buyPrice: 220,
        sellPrice: 275,
        stock: 16,
        reorderLevel: 20
    },

    {
        id: 31,
        sku: "FRT-031",
        name: "Azospirillum",
        brand: "BioGrow",
        category: "Bio-Fertilizer",
        unit: "Bottle",
        buyPrice: 75,
        sellPrice: 95,
        stock: 45,
        reorderLevel: 20
    },

    {
        id: 32,
        sku: "FRT-032",
        name: "Azotobacter",
        brand: "BioGrow",
        category: "Bio-Fertilizer",
        unit: "Bottle",
        buyPrice: 72,
        sellPrice: 92,
        stock: 31,
        reorderLevel: 20
    },

    {
        id: 33,
        sku: "FRT-033",
        name: "Phosphate Solubilizing Bacteria",
        brand: "IFFCO Bio",
        category: "Bio-Fertilizer",
        unit: "Bottle",
        buyPrice: 80,
        sellPrice: 105,
        stock: 29,
        reorderLevel: 20
    },

    {
        id: 34,
        sku: "FRT-034",
        name: "Potassium Mobilizing Bacteria",
        brand: "IFFCO Bio",
        category: "Bio-Fertilizer",
        unit: "Bottle",
        buyPrice: 85,
        sellPrice: 110,
        stock: 18,
        reorderLevel: 20
    },

    {
        id: 35,
        sku: "FRT-035",
        name: "Mono Ammonium Phosphate",
        brand: "Coromandel",
        category: "Phosphate",
        unit: "Bag",
        buyPrice: 1320,
        sellPrice: 1470,
        stock: 39,
        reorderLevel: 20
    },

    {
        id: 36,
        sku: "FRT-036",
        name: "Mono Potassium Phosphate",
        brand: "Haifa",
        category: "Potassic",
        unit: "Kg",
        buyPrice: 185,
        sellPrice: 220,
        stock: 24,
        reorderLevel: 20
    },

    {
        id: 37,
        sku: "FRT-037",
        name: "Potassium Nitrate",
        brand: "Yara",
        category: "Potassic",
        unit: "Kg",
        buyPrice: 145,
        sellPrice: 178,
        stock: 68,
        reorderLevel: 25
    },

    {
        id: 38,
        sku: "FRT-038",
        name: "Calcium Ammonium Nitrate",
        brand: "Yara",
        category: "Nitrogen",
        unit: "Bag",
        buyPrice: 710,
        sellPrice: 815,
        stock: 47,
        reorderLevel: 20
    },

    {
        id: 39,
        sku: "FRT-039",
        name: "UAN 28",
        brand: "AgriChem",
        category: "Nitrogen",
        unit: "Litre",
        buyPrice: 48,
        sellPrice: 60,
        stock: 75,
        reorderLevel: 25
    },

    {
        id: 40,
        sku: "FRT-040",
        name: "UAN 32",
        brand: "AgriChem",
        category: "Nitrogen",
        unit: "Litre",
        buyPrice: 52,
        sellPrice: 65,
        stock: 12,
        reorderLevel: 20
    },

    {
        id: 41,
        sku: "FRT-041",
        name: "NPK 08:21:21",
        brand: "Zuari",
        category: "NPK",
        unit: "Bag",
        buyPrice: 980,
        sellPrice: 1100,
        stock: 61,
        reorderLevel: 25
    },

    {
        id: 42,
        sku: "FRT-042",
        name: "NPK 14:28:14",
        brand: "FACT",
        category: "NPK",
        unit: "Bag",
        buyPrice: 1210,
        sellPrice: 1350,
        stock: 26,
        reorderLevel: 25
    },

    {
        id: 43,
        sku: "FRT-043",
        name: "NPK 17:17:17",
        brand: "Mangalore Chemicals",
        category: "NPK",
        unit: "Bag",
        buyPrice: 1180,
        sellPrice: 1320,
        stock: 73,
        reorderLevel: 30
    },

    {
        id: 44,
        sku: "FRT-044",
        name: "NPK 19:19:19 Premium",
        brand: "Kristalon",
        category: "Water Soluble",
        unit: "Kg",
        buyPrice: 108,
        sellPrice: 128,
        stock: 91,
        reorderLevel: 30
    },

    {
        id: 45,
        sku: "FRT-045",
        name: "MAP 12:61:00",
        brand: "Haifa",
        category: "Water Soluble",
        unit: "Kg",
        buyPrice: 175,
        sellPrice: 210,
        stock: 43,
        reorderLevel: 20
    },

    {
        id: 46,
        sku: "FRT-046",
        name: "Potassium Schoenite",
        brand: "IPL",
        category: "Potassic",
        unit: "Bag",
        buyPrice: 760,
        sellPrice: 850,
        stock: 52,
        reorderLevel: 20
    },

    {
        id: 47,
        sku: "FRT-047",
        name: "Gypsum",
        brand: "Agrimax",
        category: "Sulphur",
        unit: "Bag",
        buyPrice: 330,
        sellPrice: 400,
        stock: 67,
        reorderLevel: 25
    },

    {
        id: 48,
        sku: "FRT-048",
        name: "Dolomite",
        brand: "Kisan Minerals",
        category: "Organic",
        unit: "Bag",
        buyPrice: 290,
        sellPrice: 365,
        stock: 14,
        reorderLevel: 20
    },

    {
        id: 49,
        sku: "FRT-049",
        name: "Bio NPK",
        brand: "AgroStar",
        category: "Bio-Fertilizer",
        unit: "Bottle",
        buyPrice: 105,
        sellPrice: 135,
        stock: 57,
        reorderLevel: 20
    },

    {
        id: 50,
        sku: "FRT-050",
        name: "Micro Nutrient Mix",
        brand: "Multiplex",
        category: "Micronutrient",
        unit: "Kg",
        buyPrice: 135,
        sellPrice: 165,
        stock: 32,
        reorderLevel: 25
    }

];


/* =========================================================
   INIT
========================================================= */

document.addEventListener(
    "DOMContentLoaded",
    function () {

        loadFertilizers();

        loadStockInRecords();

        /*
         * Create opening-stock history only once.
         */

        seedOpeningStockHistory();

        populateFertilizerSelect();

        bindEvents();

        updateSummary();

        renderHistory();

    }
);


/* =========================================================
   FIND EXISTING FERTILIZER DATA
========================================================= */

function findFertilizerStorage() {

    /*
     * First check the expected key.
     */

    const primary =
        localStorage.getItem(
            PRIMARY_FERTILIZER_KEY
        );


    if (primary) {

        try {

            const parsed =
                JSON.parse(primary);


            if (
                Array.isArray(parsed) &&
                parsed.length > 0
            ) {

                fertilizerStorageKey =
                    PRIMARY_FERTILIZER_KEY;

                return parsed;

            }

        } catch (error) {

            console.warn(
                "Invalid fertilizer data."
            );

        }

    }


    /*
     * Look through localStorage to find the
     * existing fertilizer list if your current
     * fertilizers.js used another key.
     */

    for (
        let i = 0;
        i < localStorage.length;
        i++
    ) {

        const key =
            localStorage.key(i);


        if (!key) {
            continue;
        }


        if (
            key === STOCK_IN_STORAGE_KEY
        ) {
            continue;
        }


        try {

            const value =
                localStorage.getItem(key);


            const parsed =
                JSON.parse(value);


            if (
                Array.isArray(parsed) &&
                parsed.length > 0 &&
                looksLikeFertilizerArray(
                    parsed
                )
            ) {

                fertilizerStorageKey =
                    key;

                return parsed;

            }

        } catch (error) {

            /* Ignore non-JSON localStorage entries. */

        }

    }


    return null;

}


/* =========================================================
   IDENTIFY FERTILIZER DATA
========================================================= */

function looksLikeFertilizerArray(
    data
) {

    return data.some(
        function (item) {

            return (
                item &&
                (
                    item.name !== undefined ||
                    item.fertilizer !== undefined
                ) &&
                (
                    item.stock !== undefined ||
                    item.quantity !== undefined
                )
            );

        }
    );

}


/* =========================================================
   NORMALIZE
========================================================= */

function normalizeFertilizers(
    data
) {

    return data.map(
        function (item, index) {

            return {

                id:
                    Number(item.id) ||
                    index + 1,

                sku:
                    item.sku ||
                    `FRT-${String(
                        index + 1
                    ).padStart(
                        3,
                        "0"
                    )}`,

                name:
                    item.name ||
                    item.fertilizer ||
                    `Fertilizer ${index + 1}`,

                brand:
                    item.brand ||
                    "SmartAgri",

                category:
                    item.category ||
                    "Uncategorized",

                unit:
                    item.unit ||
                    "Bag",

                buyPrice:
                    Number(
                        item.buyPrice
                    ) || 0,

                sellPrice:
                    Number(
                        item.sellPrice
                    ) || 0,

                stock:
                    Number(
                        item.stock
                    ) || 0,

                reorderLevel:
                    Number(
                        item.reorderLevel
                    ) || 0

            };

        }
    );

}


/* =========================================================
   LOAD FERTILIZERS
========================================================= */

function loadFertilizers() {

    const existing =
        findFertilizerStorage();


    if (existing) {

        fertilizers =
            normalizeFertilizers(
                existing
            );


        /*
         * If fewer than 50 are currently saved,
         * merge in the missing base records.
         */

        if (
            fertilizers.length < 50
        ) {

            const merged =
                defaultFertilizers.map(
                    item => ({
                        ...item
                    })
                );


            fertilizers.forEach(
                function (savedItem) {

                    const index =
                        merged.findIndex(
                            item =>
                                Number(
                                    item.id
                                ) ===
                                Number(
                                    savedItem.id
                                )
                        );


                    if (
                        index !== -1
                    ) {

                        merged[index] = {
                            ...merged[index],
                            ...savedItem
                        };

                    } else {

                        merged.push(
                            savedItem
                        );

                    }

                }
            );


            fertilizers =
                merged;

        }


        saveFertilizers();

        return;

    }


    /*
     * No data found.
     * Use the complete 50.
     */

    fertilizers =
        defaultFertilizers.map(
            item => ({
                ...item
            })
        );


    saveFertilizers();

}


/* =========================================================
   SAVE FERTILIZERS
========================================================= */

function saveFertilizers() {

    localStorage.setItem(
        fertilizerStorageKey,
        JSON.stringify(
            fertilizers
        )
    );

}


/* =========================================================
   LOAD HISTORY
========================================================= */

function loadStockInRecords() {

    try {

        const saved =
            localStorage.getItem(
                STOCK_IN_STORAGE_KEY
            );


        if (saved) {

            const parsed =
                JSON.parse(saved);


            if (
                Array.isArray(parsed)
            ) {

                stockInRecords =
                    parsed;

                return;

            }

        }

    } catch (error) {

        console.error(
            "Unable to load stock-in records:",
            error
        );

    }


    stockInRecords = [];

}


/* =========================================================
   SAVE HISTORY
========================================================= */

function saveStockInRecords() {

    localStorage.setItem(
        STOCK_IN_STORAGE_KEY,
        JSON.stringify(
            stockInRecords
        )
    );

}


/* =========================================================
   OPENING STOCK HISTORY
========================================================= */

function seedOpeningStockHistory() {

    /*
     * Already created?
     */

    const alreadySeeded =
        localStorage.getItem(
            STOCK_IN_SEEDED_KEY
        );


    if (
        alreadySeeded === "true"
    ) {

        return;

    }


    /*
     * If existing real Stock In transactions
     * already exist, don't overwrite them.
     */

    if (
        stockInRecords.length > 0
    ) {

        localStorage.setItem(
            STOCK_IN_SEEDED_KEY,
            "true"
        );

        return;

    }


    const today =
        getLocalDateString(
            new Date()
        );


    /*
     * Create one history record for
     * each existing fertilizer.
     *
     * IMPORTANT:
     * These records do NOT increase stock.
     * They only document the initial stock
     * that is already present.
     */

    stockInRecords =
        fertilizers.map(
            function (fertilizer, index) {

                const stock =
                    Number(
                        fertilizer.stock
                    ) || 0;


                return {

                    id:
                        index + 1,

                    fertilizerId:
                        fertilizer.id,

                    fertilizer:
                        fertilizer.name,

                    category:
                        fertilizer.category,

                    quantity:
                        stock,

                    supplier:
                        "Initial Inventory",

                    reference:
                        `OPENING-${String(
                            index + 1
                        ).padStart(
                            3,
                            "0"
                        )}`,

                    batchNumber:
                        "OPENING-STOCK",

                    purchasePrice:
                        Number(
                            fertilizer.buyPrice
                        ) || 0,

                    date:
                        today,

                    oldStock:
                        0,

                    newStock:
                        stock,

                    unit:
                        fertilizer.unit ||
                        "Units",

                    remarks:
                        "Initial stock loaded from existing fertilizer master data.",

                    openingStock:
                        true,

                    createdAt:
                        new Date().toISOString()

                };

            }
        );


    saveStockInRecords();


    localStorage.setItem(
        STOCK_IN_SEEDED_KEY,
        "true"
    );

}


/* =========================================================
   SET TODAY
========================================================= */

function setTodayDate() {

    const input =
        document.getElementById(
            "stockInDate"
        );


    if (!input) {
        return;
    }


    input.value =
        getLocalDateString(
            new Date()
        );

}


/* =========================================================
   DROPDOWN
========================================================= */

function populateFertilizerSelect() {

    const select =
        document.getElementById(
            "fertilizerSelect"
        );


    if (!select) {
        return;
    }


    if (
        !fertilizers.length
    ) {

        select.innerHTML = `

            <option value="">
                No fertilizers found
            </option>

        `;

        return;

    }


    select.innerHTML = `

        <option value="">
            Select fertilizer
        </option>

        ${fertilizers
            .slice()
            .sort(
                function (a, b) {

                    return String(
                        a.name
                    ).localeCompare(
                        String(b.name)
                    );

                }
            )
            .map(
                function (item) {

                    return `

                        <option
                            value="${item.id}">

                            ${escapeHtml(
                                item.name
                            )}

                            -
                            ${escapeHtml(
                                item.brand
                            )}

                            |
                            Stock:
                            ${Number(
                                item.stock
                            ).toLocaleString(
                                "en-IN"
                            )}

                            ${escapeHtml(
                                item.unit
                            )}

                        </option>

                    `;

                }
            )
            .join("")}

    `;

}


/* =========================================================
   EVENTS
========================================================= */

function bindEvents() {

    const fertilizerSelect =
        document.getElementById(
            "fertilizerSelect"
        );

    const quantity =
        document.getElementById(
            "quantity"
        );

    const form =
        document.getElementById(
            "stockInForm"
        );

    const clearButton =
        document.getElementById(
            "clearFormBtn"
        );

    const historySearch =
        document.getElementById(
            "recordSearch"
        );

    const topSearch =
        document.getElementById(
            "stockTopSearchInput"
        );

    const topClear =
        document.getElementById(
            "stockTopSearchClear"
        );

    const menuButton =
        document.getElementById(
            "menuBtn"
        );


    if (fertilizerSelect) {

        fertilizerSelect.addEventListener(
            "change",
            updateSelectedProduct
        );

    }


    if (quantity) {

        quantity.addEventListener(
            "input",
            updateStockPreview
        );

    }


    if (form) {

        form.addEventListener(
            "submit",
            saveStockIn
        );

    }


    if (clearButton) {

        clearButton.addEventListener(
            "click",
            clearStockForm
        );

    }


    if (historySearch) {

        historySearch.addEventListener(
            "input",
            renderHistory
        );

    }


    if (topSearch) {

        topSearch.addEventListener(
            "input",
            function () {

                if (historySearch) {

                    historySearch.value =
                        this.value;

                    renderHistory();

                }

            }
        );

    }


    if (topClear) {

        topClear.addEventListener(
            "click",
            function () {

                if (topSearch) {
                    topSearch.value = "";
                }

                if (historySearch) {
                    historySearch.value = "";
                }

                renderHistory();

            }
        );

    }


    if (menuButton) {

        menuButton.addEventListener(
            "click",
            toggleStockInSidebar
        );

    }


    /*
     * Listen for Fertilizer page changes.
     */

    window.addEventListener(
        "storage",
        function (event) {

            if (
                event.key ===
                fertilizerStorageKey ||
                event.key ===
                PRIMARY_FERTILIZER_KEY
            ) {

                loadFertilizers();

                populateFertilizerSelect();

                updateSelectedProduct();

                updateSummary();

            }


            if (
                event.key ===
                STOCK_IN_STORAGE_KEY
            ) {

                loadStockInRecords();

                updateSummary();

                renderHistory();

            }

        }
    );


    /*
     * Refresh when page becomes visible.
     */

    document.addEventListener(
        "visibilitychange",
        function () {

            if (
                document.visibilityState ===
                "visible"
            ) {

                loadFertilizers();

                loadStockInRecords();

                populateFertilizerSelect();

                updateSelectedProduct();

                updateSummary();

                renderHistory();

            }

        }
    );


    /*
     * Mobile search.
     */

    const searchBox =
        document.getElementById(
            "stockTopSearch"
        );


    if (searchBox) {

        searchBox.addEventListener(
            "click",
            function () {

                if (
                    window.innerWidth <= 767
                ) {

                    searchBox.classList.add(
                        "active"
                    );

                    if (topSearch) {
                        topSearch.focus();
                    }

                }

            }
        );

    }

}


/* =========================================================
   SELECTED PRODUCT
========================================================= */

function updateSelectedProduct() {

    loadFertilizers();


    const select =
        document.getElementById(
            "fertilizerSelect"
        );


    const item =
        fertilizers.find(
            function (fertilizer) {

                return (
                    String(
                        fertilizer.id
                    ) ===
                    String(
                        select.value
                    )
                );

            }
        );


    const name =
        document.getElementById(
            "selectedProductName"
        );

    const category =
        document.getElementById(
            "selectedCategory"
        );

    const stock =
        document.getElementById(
            "selectedStock"
        );

    const reorder =
        document.getElementById(
            "selectedReorder"
        );

    const unit =
        document.getElementById(
            "selectedUnit"
        );

    const info =
        document.getElementById(
            "currentStockInfo"
        );

    const unitLabel =
        document.getElementById(
            "unitLabel"
        );


    if (!item) {

        if (name) {
            name.textContent = "None";
        }

        if (category) {
            category.textContent = "-";
        }

        if (stock) {
            stock.textContent = "-";
        }

        if (reorder) {
            reorder.textContent = "-";
        }

        if (unit) {
            unit.textContent = "-";
        }

        if (unitLabel) {
            unitLabel.textContent = "Units";
        }

        if (info) {

            info.textContent =
                "Select a fertilizer to view current stock.";

        }

        hidePreview();

        return;

    }


    const currentStock =
        Number(
            item.stock
        ) || 0;


    const reorderLevel =
        Number(
            item.reorderLevel
        ) || 0;


    const itemUnit =
        item.unit ||
        "Units";


    name.textContent =
        item.name;


    category.textContent =
        item.category ||
        "-";


    stock.textContent =
        currentStock.toLocaleString(
            "en-IN"
        );


    reorder.textContent =
        reorderLevel.toLocaleString(
            "en-IN"
        );


    unit.textContent =
        itemUnit;


    unitLabel.textContent =
        itemUnit;


    info.innerHTML = `

        Current stock:
        <strong>
            ${currentStock.toLocaleString(
                "en-IN"
            )}
            ${escapeHtml(
                itemUnit
            )}
        </strong>

        &nbsp; | &nbsp;

        Reorder level:
        <strong>
            ${reorderLevel.toLocaleString(
                "en-IN"
            )}
        </strong>

    `;


    updateStockPreview();

}


/* =========================================================
   PREVIEW
========================================================= */

function updateStockPreview() {

    const select =
        document.getElementById(
            "fertilizerSelect"
        );

    const quantityInput =
        document.getElementById(
            "quantity"
        );


    const preview =
        document.getElementById(
            "stockPreview"
        );


    if (
        !select ||
        !select.value
    ) {

        hidePreview();

        return;

    }


    const item =
        fertilizers.find(
            function (fertilizer) {

                return (
                    String(
                        fertilizer.id
                    ) ===
                    String(
                        select.value
                    )
                );

            }
        );


    if (!item) {

        hidePreview();

        return;

    }


    const current =
        Number(
            item.stock
        ) || 0;


    const quantity =
        Number(
            quantityInput.value
        ) || 0;


    document.getElementById(
        "previewCurrent"
    ).textContent =
        current.toLocaleString(
            "en-IN"
        );


    document.getElementById(
        "previewAfter"
    ).textContent =
        (
            current +
            quantity
        ).toLocaleString(
            "en-IN"
        );


    preview.classList.remove(
        "d-none"
    );

}


function hidePreview() {

    const preview =
        document.getElementById(
            "stockPreview"
        );


    if (preview) {

        preview.classList.add(
            "d-none"
        );

    }

}


/* =========================================================
   SAVE STOCK IN
========================================================= */

function saveStockIn(event) {

    event.preventDefault();


    loadFertilizers();


    const fertilizerId =
        document.getElementById(
            "fertilizerSelect"
        ).value;


    const quantity =
        Number(
            document.getElementById(
                "quantity"
            ).value
        );


    const supplier =
        document.getElementById(
            "supplier"
        ).value.trim();


    const reference =
        document.getElementById(
            "reference"
        ).value.trim();


    const batchNumber =
        document.getElementById(
            "batchNumber"
        ).value.trim();


    const purchasePrice =
        Number(
            document.getElementById(
                "purchasePrice"
            ).value
        ) || 0;


    const date =
        document.getElementById(
            "stockInDate"
        ).value;


    const remarks =
        document.getElementById(
            "remarks"
        ).value.trim();


    if (!fertilizerId) {

        showFormMessage(
            "Please select a fertilizer.",
            true
        );

        return;

    }


    if (
        !Number.isFinite(quantity) ||
        quantity <= 0
    ) {

        showFormMessage(
            "Quantity must be greater than 0.",
            true
        );

        return;

    }


    if (!supplier) {

        showFormMessage(
            "Please enter the supplier name.",
            true
        );

        return;

    }


    if (!date) {

        showFormMessage(
            "Please select the stock-in date.",
            true
        );

        return;

    }


    const index =
        fertilizers.findIndex(
            function (item) {

                return (
                    String(
                        item.id
                    ) ===
                    String(
                        fertilizerId
                    )
                );

            }
        );


    if (index === -1) {

        showFormMessage(
            "Fertilizer not found.",
            true
        );

        return;

    }


    const fertilizer =
        fertilizers[index];


    const oldStock =
        Number(
            fertilizer.stock
        ) || 0;


    const newStock =
        oldStock +
        quantity;


    /*
     * UPDATE FERTILIZER STOCK
     */

    fertilizers[index] = {

        ...fertilizer,

        stock:
            newStock

    };


    /*
     * SAVE TO SAME STORAGE
     */

    saveFertilizers();


    /*
     * ADD TRANSACTION
     */

    const record = {

        id:
            getNextRecordId(),

        fertilizerId:
            fertilizer.id,

        fertilizer:
            fertilizer.name,

        category:
            fertilizer.category,

        quantity:
            quantity,

        supplier:
            supplier,

        reference:
            reference || "-",

        batchNumber:
            batchNumber || "-",

        purchasePrice:
            purchasePrice,

        date:
            date,

        oldStock:
            oldStock,

        newStock:
            newStock,

        unit:
            fertilizer.unit ||
            "Units",

        remarks:
            remarks || "-",

        openingStock:
            false,

        createdAt:
            new Date().toISOString()

    };


    stockInRecords.unshift(
        record
    );


    saveStockInRecords();


    /*
     * UPDATE UI
     */

    populateFertilizerSelect();

    updateSummary();

    renderHistory();


    showToast(

        `${fertilizer.name} stock updated from ${oldStock} to ${newStock}.`

    );


    clearStockForm();

}


/* =========================================================
   SUMMARY
========================================================= */

function updateSummary() {

    loadFertilizers();

    loadStockInRecords();


    /*
     * CURRENT INVENTORY
     *
     * This comes directly from the 50
     * fertilizer stock values.
     */

    const currentInventory =
        fertilizers.reduce(
            function (
                total,
                item
            ) {

                return (
                    total +
                    (
                        Number(
                            item.stock
                        ) || 0
                    )
                );

            },
            0
        );


    /*
     * TODAY
     */

    const today =
        getLocalDateString(
            new Date()
        );


    const now =
        new Date();


    let todayStockIn = 0;

    let monthStockIn = 0;


    stockInRecords.forEach(
        function (record) {

            const quantity =
                Number(
                    record.quantity
                ) || 0;


            const recordDate =
                new Date(
                    record.date +
                    "T00:00:00"
                );


            if (
                record.date ===
                today
            ) {

                todayStockIn +=
                    quantity;

            }


            if (
                recordDate.getFullYear() ===
                now.getFullYear() &&

                recordDate.getMonth() ===
                now.getMonth()
            ) {

                monthStockIn +=
                    quantity;

            }

        }
    );


    /*
     * UPDATE CARDS
     */

    updateElement(
        "todayStockIn",
        todayStockIn.toLocaleString(
            "en-IN"
        )
    );


    updateElement(
        "monthStockIn",
        monthStockIn.toLocaleString(
            "en-IN"
        )
    );


    updateElement(
        "transactionCount",
        stockInRecords.length
    );


    updateElement(
        "currentInventory",
        currentInventory.toLocaleString(
            "en-IN"
        )
    );

}


/* =========================================================
   HISTORY
========================================================= */

function renderHistory() {

    loadStockInRecords();


    const tbody =
        document.getElementById(
            "stockHistoryBody"
        );


    const empty =
        document.getElementById(
            "stockHistoryEmpty"
        );


    if (!tbody) {
        return;
    }


    const searchElement =
        document.getElementById(
            "recordSearch"
        );


    const search =
        searchElement
            ? searchElement.value
                .trim()
                .toLowerCase()
            : "";


    const filtered =
        stockInRecords
            .filter(
                function (record) {

                    const text = [

                        record.fertilizer,

                        record.category,

                        record.supplier,

                        record.reference,

                        record.batchNumber,

                        record.date,

                        record.remarks

                    ]
                        .join(" ")
                        .toLowerCase();


                    return (
                        !search ||
                        text.includes(
                            search
                        )
                    );

                }
            )
            .slice(
                0,
                50
            );


    if (!filtered.length) {

        tbody.innerHTML = "";


        if (empty) {

            empty.classList.remove(
                "d-none"
            );

        }

        return;

    }


    if (empty) {

        empty.classList.add(
            "d-none"
        );

    }


    tbody.innerHTML =
        filtered.map(
            function (record) {

                const isOpening =
                    record.openingStock === true;


                return `

                    <tr>

                        <td>

                            ${formatDate(
                                record.date
                            )}

                        </td>


                        <td>

                            <strong>
                                ${escapeHtml(
                                    record.fertilizer
                                )}
                            </strong>

                        </td>


                        <td>

                            ${escapeHtml(
                                record.category
                            )}

                        </td>


                        <td>

                            <span
                                class="quantity-badge">

                                +

                                ${Number(
                                    record.quantity
                                ).toLocaleString(
                                    "en-IN"
                                )}

                                ${escapeHtml(
                                    record.unit
                                )}

                            </span>

                        </td>


                        <td>

                            ${escapeHtml(
                                record.supplier
                            )}

                        </td>


                        <td>

                            ${escapeHtml(
                                record.reference
                            )}

                        </td>


                        <td>

                            <strong>
                                ${Number(
                                    record.newStock
                                ).toLocaleString(
                                    "en-IN"
                                )}
                            </strong>


                            ${
                                isOpening
                                    ? `
                                        <div class="small text-secondary">
                                            Opening Stock
                                        </div>
                                      `
                                    : ""
                            }

                        </td>

                    </tr>

                `;

            }
        ).join("");

}


/* =========================================================
   CLEAR FORM
========================================================= */

function clearStockForm() {

    const form =
        document.getElementById(
            "stockInForm"
        );


    if (!form) {
        return;
    }


    form.reset();


    setTodayDate();


    document.getElementById(
        "stockFormMessage"
    ).textContent =
        "";


    document.getElementById(
        "currentStockInfo"
    ).textContent =
        "Select a fertilizer to view current stock.";


    document.getElementById(
        "selectedProductName"
    ).textContent =
        "None";


    document.getElementById(
        "selectedCategory"
    ).textContent =
        "-";


    document.getElementById(
        "selectedStock"
    ).textContent =
        "-";


    document.getElementById(
        "selectedReorder"
    ).textContent =
        "-";


    document.getElementById(
        "selectedUnit"
    ).textContent =
        "-";


    document.getElementById(
        "unitLabel"
    ).textContent =
        "Units";


    hidePreview();

}


/* =========================================================
   GET NEXT TRANSACTION ID
========================================================= */

function getNextRecordId() {

    if (!stockInRecords.length) {
        return 1;
    }


    return (
        Math.max(
            ...stockInRecords.map(
                record =>
                    Number(
                        record.id
                    ) || 0
            )
        ) + 1
    );

}


/* =========================================================
   FORM MESSAGE
========================================================= */

function showFormMessage(
    message,
    isError
) {

    const element =
        document.getElementById(
            "stockFormMessage"
        );


    if (!element) {
        return;
    }


    element.textContent =
        message;


    element.style.color =
        isError
            ? "#c14444"
            : "#23834c";

}


/* =========================================================
   TOAST
========================================================= */

function showToast(
    message
) {

    const element =
        document.getElementById(
            "stockToastMessage"
        );


    const toastElement =
        document.getElementById(
            "stockToast"
        );


    if (
        !element ||
        !toastElement
    ) {

        alert(message);

        return;

    }


    element.textContent =
        message;


    const toast =
        bootstrap.Toast.getOrCreateInstance(
            toastElement,
            {
                delay: 3500
            }
        );


    toast.show();

}


/* =========================================================
   DATE
========================================================= */

function getLocalDateString(
    date
) {

    const year =
        date.getFullYear();


    const month =
        String(
            date.getMonth() + 1
        ).padStart(
            2,
            "0"
        );


    const day =
        String(
            date.getDate()
        ).padStart(
            2,
            "0"
        );


    return (
        `${year}-${month}-${day}`
    );

}


function formatDate(
    value
) {

    if (!value) {
        return "-";
    }


    const parts =
        value.split("-");


    if (
        parts.length !== 3
    ) {

        return value;

    }


    const months = [

        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",

        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec"

    ];


    return `

        ${parts[2]}
        ${months[
            Number(parts[1]) - 1
        ]}
        ${parts[0]}

    `;

}


/* =========================================================
   UPDATE ELEMENT
========================================================= */

function updateElement(
    id,
    value
) {

    const element =
        document.getElementById(
            id
        );


    if (element) {

        element.textContent =
            value;

    }

}


/* =========================================================
   SIDEBAR
========================================================= */

function toggleStockInSidebar() {

    const side =
        document.getElementById(
            "side"
        );

    const overlay =
        document.getElementById(
            "sidebarOverlay"
        );


    if (!side || !overlay) {
        return;
    }


    side.classList.toggle(
        "show"
    );

    overlay.classList.toggle(
        "show"
    );

}


function closeStockInSidebar() {

    const side =
        document.getElementById(
            "side"
        );

    const overlay =
        document.getElementById(
            "sidebarOverlay"
        );


    if (!side || !overlay) {
        return;
    }


    side.classList.remove(
        "show"
    );

    overlay.classList.remove(
        "show"
    );

}


/* =========================================================
   ESCAPE HTML
========================================================= */

function escapeHtml(
    value
) {

    return String(value)
        .replace(
            /&/g,
            "&amp;"
        )
        .replace(
            /</g,
            "&lt;"
        )
        .replace(
            />/g,
            "&gt;"
        )
        .replace(
            /"/g,
            "&quot;"
        )
        .replace(
            /'/g,
            "&#039;"
        );

}


/* =========================================================
   RESIZE
========================================================= */

window.addEventListener(
    "resize",
    function () {

        if (
            window.innerWidth > 991
        ) {

            closeStockInSidebar();

        }

    }
);