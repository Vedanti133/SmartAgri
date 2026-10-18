/* =========================================================
   SMARTAGRI DASHBOARD.JS
   ========================================================= */


/* =========================================================
   ELEMENTS
   ========================================================= */

const side = document.getElementById("side");
const sidebarOverlay = document.getElementById("sidebarOverlay");

const mobileSearchButton =
    document.getElementById("mobileSearchButton");

const topbarSearch =
    document.getElementById("topbarSearch");

const searchClose =
    document.getElementById("searchClose");

const dashboardSearch =
    document.getElementById("dashboardSearch");


/* =========================================================
   SIDEBAR
   ========================================================= */

function toggleSidebar() {
    if (!side || !sidebarOverlay) return;

    side.classList.toggle("show");
    sidebarOverlay.classList.toggle("show");
}

function closeSidebar() {
    if (!side || !sidebarOverlay) return;

    side.classList.remove("show");
    sidebarOverlay.classList.remove("show");
}


/* Close sidebar when a menu item is clicked on mobile */

document.querySelectorAll(".sidebar a").forEach(function (link) {

    link.addEventListener("click", function () {

        if (window.innerWidth <= 991) {
            closeSidebar();
        }

    });

});


/* =========================================================
   RESPONSIVE SEARCH
   ========================================================= */

if (mobileSearchButton && topbarSearch && dashboardSearch) {

    mobileSearchButton.addEventListener(
        "click",
        function () {

            topbarSearch.classList.add("active");

            dashboardSearch.focus();

        }
    );
}


if (searchClose && topbarSearch && dashboardSearch) {

    searchClose.addEventListener(
        "click",
        function () {

            topbarSearch.classList.remove("active");

            dashboardSearch.value = "";

            resetDashboardSearch();

        }
    );
}


/* Close search with Escape */

document.addEventListener(
    "keydown",
    function (event) {

        if (event.key === "Escape") {

            if (topbarSearch) {
                topbarSearch.classList.remove("active");
            }

            if (dashboardSearch) {
                dashboardSearch.value = "";

                resetDashboardSearch();
            }

            closeSidebar();
        }

    }
);


/* =========================================================
   DASHBOARD SEARCH
   ========================================================= */

function resetDashboardSearch() {

    const cards = document.querySelectorAll(
        ".dashboard-search-item"
    );

    cards.forEach(function (card) {
        card.style.display = "";
    });
}


if (dashboardSearch) {

    dashboardSearch.addEventListener(
        "input",
        function () {

            const searchValue =
                this.value.trim().toLowerCase();

            /*
             * Search dashboard cards.
             * This class is added dynamically below.
             */

            const cards =
                document.querySelectorAll(
                    ".dashboard-search-item"
                );

            if (!searchValue) {

                cards.forEach(function (card) {
                    card.style.display = "";
                });

                return;
            }


            cards.forEach(function (card) {

                const cardText =
                    card.innerText.toLowerCase();

                card.style.display =
                    cardText.includes(searchValue)
                        ? ""
                        : "none";

            });

        }
    );
}


/* Add searchable class */

document.querySelectorAll(
    ".stat, .chart-card, .content-card"
).forEach(function (element) {

    element.classList.add(
        "dashboard-search-item"
    );

});


/* =========================================================
   SALES CHART
   ========================================================= */

function createSalesChart() {

    const canvas =
        document.getElementById("salesChart");

    if (!canvas || typeof Chart === "undefined") {
        return;
    }


    /* Remove any existing Chart.js instance */

    const existingChart =
        Chart.getChart(canvas);

    if (existingChart) {
        existingChart.destroy();
    }


    new Chart(canvas, {

        type: "line",

        data: {

            labels: [
                "Jan",
                "Feb",
                "Mar",
                "Apr",
                "May",
                "Jun",
                "Jul",
                "Aug",
                "Sep"
            ],

            datasets: [

                {
                    label: "Sales",

                    data: [
                        42000,
                        50000,
                        47000,
                        61000,
                        70000,
                        67000,
                        79000,
                        84000,
                        86000
                    ],

                    borderColor: "#3fa3df",

                    backgroundColor:
                        "rgba(63, 163, 223, 0.12)",

                    borderWidth: 3,

                    pointRadius: 3,

                    pointHoverRadius: 6,

                    pointBackgroundColor: "#3fa3df",

                    pointBorderColor: "#ffffff",

                    pointBorderWidth: 2,

                    tension: 0.4,

                    fill: false
                }

            ]

        },


        options: {

            responsive: true,

            maintainAspectRatio: false,

            interaction: {
                intersect: false,
                mode: "index"
            },


            plugins: {

                legend: {
                    display: true,

                    position: "top",

                    labels: {
                        usePointStyle: true,

                        pointStyle: "line",

                        padding: 18,

                        font: {
                            size: 12
                        }
                    }
                },


                tooltip: {

                    backgroundColor: "#173f2f",

                    titleColor: "#ffffff",

                    bodyColor: "#ffffff",

                    padding: 11,

                    cornerRadius: 9,

                    callbacks: {

                        label: function (context) {

                            return (
                                " Sales: ₹" +
                                Number(
                                    context.raw
                                ).toLocaleString("en-IN")
                            );

                        }

                    }

                }

            },


            scales: {

                x: {

                    grid: {
                        display: false
                    },

                    ticks: {
                        color: "#78837f",

                        font: {
                            size: 11
                        }
                    }

                },


                y: {

                    beginAtZero: false,

                    grid: {

                        color:
                            "rgba(80, 100, 90, 0.12)",

                        drawBorder: false
                    },

                    ticks: {

                        color: "#78837f",

                        font: {
                            size: 10
                        },

                        callback: function (value) {

                            if (value >= 1000) {

                                return (
                                    "₹" +
                                    (value / 1000)
                                        .toFixed(0) +
                                    "K"
                                );

                            }

                            return "₹" + value;

                        }

                    }

                }

            },


            animation: {

                duration: 900,

                easing: "easeOutQuart"

            }

        }

    });

}


/* =========================================================
   STOCK BY CATEGORY CHART
   ========================================================= */

function createStockChart() {

    const canvas =
        document.getElementById("stockChart");

    if (!canvas || typeof Chart === "undefined") {
        return;
    }


    /* Remove existing chart */

    const existingChart =
        Chart.getChart(canvas);

    if (existingChart) {
        existingChart.destroy();
    }


    const values = [
        1830,
        1300,
        965,
        725
    ];


    new Chart(canvas, {

        type: "doughnut",

        data: {

            labels: [
                "Nitrogen",
                "NPK",
                "Phosphate",
                "Organic"
            ],

            datasets: [

                {

                    data: values,

                    backgroundColor: [
                        "#35a1df",
                        "#eb6b86",
                        "#f59b3e",
                        "#f4c34f"
                    ],

                    borderColor: "#ffffff",

                    borderWidth: 4,

                    spacing: 3,

                    hoverOffset: 10
                }

            ]

        },


        options: {

            responsive: true,

            maintainAspectRatio: false,

            cutout: "70%",


            animation: {

                animateRotate: true,

                animateScale: true,

                duration: 900
            },


            interaction: {

                intersect: true,

                mode: "nearest"

            },


            plugins: {

                legend: {
                    display: false
                },


                tooltip: {

                    backgroundColor: "#173f2f",

                    titleColor: "#ffffff",

                    bodyColor: "#ffffff",

                    padding: 12,

                    cornerRadius: 10,

                    displayColors: true,


                    callbacks: {

                        label: function (context) {

                            const total =
                                context.dataset.data
                                    .reduce(
                                        function (
                                            sum,
                                            value
                                        ) {
                                            return (
                                                sum +
                                                value
                                            );
                                        },
                                        0
                                    );

                            const value =
                                context.raw;

                            const percentage =
                                (
                                    value /
                                    total *
                                    100
                                ).toFixed(1);


                            return (
                                " " +
                                Number(
                                    value
                                ).toLocaleString(
                                    "en-IN"
                                ) +
                                " units (" +
                                percentage +
                                "%)"
                            );

                        }

                    }

                }

            }

        }

    });

}


/* =========================================================
   UPDATE STOCK TOTAL
   ========================================================= */

async function updateStockTotal() {
    const totalElement = document.querySelector(".stock-chart-center strong");
    const statCards = document.querySelectorAll(".card.stat .stat-value");

    try {
        if (typeof ApiClient !== "undefined") {
            const res = await ApiClient.get("/fertilizers");
            if (res && res.success && Array.isArray(res.data)) {
                const ferts = res.data;
                const totalStock = ferts.reduce((sum, f) => sum + (Number(f.stock) || 0), 0);
                const lowStockCount = ferts.filter(f => {
                    const st = Number(f.stock) || 0;
                    const min = Number(f.minimumStock) || 10;
                    return st > 0 && st <= min;
                }).length;
                const outOfStockCount = ferts.filter(f => (Number(f.stock) || 0) <= 0).length;

                if (totalElement) {
                    totalElement.textContent = totalStock.toLocaleString("en-IN");
                }
                if (statCards && statCards.length >= 4) {
                    statCards[2].textContent = totalStock.toLocaleString("en-IN");
                    statCards[3].textContent = (lowStockCount + outOfStockCount).toLocaleString("en-IN");
                }
                return;
            }
        }
    } catch (e) {
        console.warn("Could not fetch live fertilizers for dashboard metrics:", e);
    }

    if (totalElement) {
        totalElement.textContent = (4820).toLocaleString("en-IN");
    }
}


/* =========================================================
   WINDOW RESIZE
   ========================================================= */

let resizeTimer;

window.addEventListener(
    "resize",
    function () {

        clearTimeout(resizeTimer);

        resizeTimer = setTimeout(
            function () {

                /*
                 * Close mobile search when moving
                 * back to desktop.
                 */

                if (
                    window.innerWidth > 767 &&
                    topbarSearch
                ) {

                    topbarSearch.classList.remove(
                        "active"
                    );

                }


                /*
                 * Close sidebar when switching
                 * from mobile/tablet to desktop.
                 */

                if (
                    window.innerWidth > 991
                ) {

                    closeSidebar();

                }

            },
            150
        );

    }
);


/* =========================================================
   INITIALIZE & ORDER METRICS (PHASE 4)
   ========================================================= */

async function loadOrderMetrics() {
    try {
        if (typeof ApiClient === 'undefined') return;
        const res = await ApiClient.get('/admin/orders/metrics');
        if (res && res.success && res.data) {
            const m = res.data;
            const elTotal = document.getElementById('statTotalOrders');
            const elPending = document.getElementById('statPendingOrders');
            const elProcessing = document.getElementById('statProcessingOrders');
            const elShipped = document.getElementById('statShippedOrders');
            const elDelivered = document.getElementById('statDeliveredOrders');
            const elCancelled = document.getElementById('statCancelledOrders');

            if (elTotal) elTotal.textContent = m.totalOrders;
            if (elPending) elPending.textContent = m.pendingOrders;
            if (elProcessing) elProcessing.textContent = m.processingOrders + m.confirmedOrders + m.packedOrders;
            if (elShipped) elShipped.textContent = m.shippedOrders;
            if (elDelivered) elDelivered.textContent = m.deliveredOrders;
            if (elCancelled) elCancelled.textContent = m.cancelledOrders;
        }
    } catch (e) {
        console.warn('Unable to load order metrics', e);
    }
}

async function loadFulfillmentMetrics() {
    try {
        if (typeof ApiClient === 'undefined') return;
        const res = await ApiClient.get('/admin/orders/fulfillment-metrics');
        if (res && res.success && res.data) {
            const m = res.data;
            const elUnassigned = document.getElementById('statUnassignedDeliveries');
            const elActive = document.getElementById('statActiveDeliveries');
            const elDeliveredToday = document.getElementById('statDeliveredToday');
            const elPayments = document.getElementById('statSuccessfulPayments');

            if (elUnassigned) elUnassigned.textContent = m.notAssignedDeliveries || 0;
            if (elActive) {
                const active = (m.assignedDeliveries || 0) + (m.packedDeliveries || 0) + (m.shippedDeliveries || 0) + (m.outForDeliveryDeliveries || 0);
                elActive.textContent = active;
            }
            if (elDeliveredToday) elDeliveredToday.textContent = m.deliveredToday || 0;
            if (elPayments) elPayments.textContent = m.successfulPayments || 0;
        }
    } catch (e) {
        console.warn('Unable to load fulfillment metrics', e);
    }
}

document.addEventListener(
    "DOMContentLoaded",
    function () {

        createSalesChart();

        createStockChart();

        updateStockTotal();

        loadOrderMetrics();

        loadFulfillmentMetrics();

    }
);