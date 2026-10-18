/* =========================================================
   SMARTAGRI CUSTOMER ORDER TRACKING
   Phase 5 Delivery Tracking & Fulfillment Management
   ========================================================= */

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Enforce Customer Authentication
    if (typeof Utils !== 'undefined' && Utils.requireAuth) {
        if (!Utils.requireAuth('CUSTOMER')) return;
    } else {
        const token = localStorage.getItem('smartagri_auth_token');
        if (!token) {
            window.location.href = 'customer-login.html';
            return;
        }
    }

    // Update account nav name
    const storedUser = JSON.parse(localStorage.getItem('smartagri_user') || '{}');
    const nameEl = document.querySelector('[data-user-name]');
    if (nameEl && storedUser.name) {
        nameEl.textContent = storedUser.name.split(' ')[0];
    }

    // 2. Parse Order ID from URL
    const params = new URLSearchParams(window.location.search);
    const orderId = params.get('orderId');

    if (!orderId) {
        document.getElementById('orderSearchCard').style.display = 'block';
        document.getElementById('trackingContent').innerHTML = `
            <div class="alert alert-info border-0 shadow-sm rounded-3 py-3">
                <i class="bi bi-info-circle-fill me-2"></i>
                Please select an order from <a href="customer-orders.html" class="fw-bold text-success">My Orders</a> or enter an Order ID above to track delivery.
            </div>
        `;
        setupSearchForm();
        return;
    }

    await loadTrackingData(orderId);
});

function setupSearchForm() {
    const form = document.getElementById('trackingSearchForm');
    if (!form) return;
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        const inputVal = document.getElementById('trackingSearchInput').value.trim();
        if (inputVal) {
            window.location.href = `customer-order-tracking.html?orderId=${encodeURIComponent(inputVal)}`;
        }
    });
}

async function loadTrackingData(orderId) {
    const container = document.getElementById('trackingContent');
    container.innerHTML = `
        <div class="text-center py-5">
            <div class="spinner-border text-success" role="status"></div>
            <p class="mt-2 text-muted">Loading live tracking details for Order #${orderId}...</p>
        </div>
    `;

    try {
        // Fetch Order, Delivery, and Fulfillment tracking details in parallel
        const [orderRes, delivRes, fulfillRes] = await Promise.all([
            ApiClient.get(`/orders/${orderId}`).catch(() => null),
            ApiClient.get(`/orders/${orderId}/delivery`).catch(() => null),
            ApiClient.get(`/orders/${orderId}/tracking`).catch(() => null)
        ]);

        if (!orderRes || !orderRes.success || !orderRes.data) {
            container.innerHTML = `
                <div class="alert alert-danger rounded-3 py-4 text-center">
                    <h5><i class="bi bi-exclamation-triangle-fill me-2"></i> Order Not Found</h5>
                    <p class="mb-3 text-muted">${orderRes ? orderRes.message : 'Unable to find this order or you do not have permission to view it.'}</p>
                    <a href="customer-orders.html" class="btn btn-outline-secondary btn-sm">Return to My Orders</a>
                </div>
            `;
            return;
        }

        const order = orderRes.data;
        const delivery = (delivRes && delivRes.success && delivRes.data) ? delivRes.data : null;
        const fulfillment = (fulfillRes && fulfillRes.success && fulfillRes.data) ? fulfillRes.data : null;

        renderTrackingView(order, delivery, fulfillment);

    } catch (err) {
        container.innerHTML = `
            <div class="alert alert-danger rounded-3 py-4 text-center">
                <h5><i class="bi bi-wifi-off me-2"></i> Communication Error</h5>
                <p class="mb-3">${err.message || 'Failed to retrieve tracking details.'}</p>
                <a href="customer-orders.html" class="btn btn-outline-secondary btn-sm">Back to Orders</a>
            </div>
        `;
    }
}

function renderTrackingView(order, delivery, fulfillment) {
    const container = document.getElementById('trackingContent');

    const total = Number(order.grandTotal) || 0;
    const dateStr = order.createdAt ? new Date(order.createdAt).toLocaleDateString('en-IN', {
        day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    }) : 'Recent';

    const fStatus = fulfillment ? fulfillment.fulfillmentStatus : null;
    const dStatus = delivery ? delivery.deliveryStatus : (fStatus || 'NOT_ASSIGNED');
    const oStatus = order.orderStatus || 'PENDING';

    const estDateStr = (fulfillment && fulfillment.expectedDeliveryDate)
        ? new Date(fulfillment.expectedDeliveryDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
        : (delivery && delivery.estimatedDeliveryDate 
            ? new Date(delivery.estimatedDeliveryDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' })
            : 'To be estimated');

    const trackingNum = (fulfillment && fulfillment.trackingNumber)
        ? fulfillment.trackingNumber
        : ((delivery && delivery.trackingNumber) ? delivery.trackingNumber : 'Pending dispatch');

    const partner = (fulfillment && fulfillment.deliveryPartnerName)
        ? fulfillment.deliveryPartnerName
        : ((delivery && delivery.deliveryPartner) ? delivery.deliveryPartner : 'SmartAgri Express Logistics');

    const partnerPhone = (delivery && delivery.deliveryPartnerPhone) ? `+91 ${delivery.deliveryPartnerPhone}` : 'Support Available';
    const partnerTrackingUrl = fulfillment ? fulfillment.trackingUrl : null;

    // Compute Timeline Step State
    const timelineData = getTimelineState(oStatus, dStatus);

    container.innerHTML = `
        <!-- HEADER OVERVIEW CARD -->
        <div class="tracking-card">
            <div class="d-flex flex-wrap justify-content-between align-items-center gap-3 pb-3 border-bottom">
                <div>
                    <span class="badge bg-light text-secondary border small mb-1">CONSIGNMENT TRACKING</span>
                    <h3 class="fw-bold text-success mb-0">${order.orderNumber}</h3>
                    <div class="text-muted small mt-1">Placed on ${dateStr}</div>
                </div>
                <div class="text-md-end">
                    <div class="fw-bold fs-5 text-dark">₹${total.toFixed(2)}</div>
                    <span class="badge ${getStatusBadgeClass(oStatus)} px-2 py-1 rounded-pill small fw-semibold me-1">${oStatus}</span>
                    <span class="badge ${order.paymentStatus === 'PAID' ? 'bg-success' : 'bg-warning text-dark'} px-2 py-1 rounded-pill small">${order.paymentMethod} &bull; ${order.paymentStatus}</span>
                </div>
            </div>

            <!-- LIVE STATUS TIMELINE -->
            <div class="my-4">
                <div class="timeline-steps">
                    <div class="timeline-progress-bar" style="width: ${timelineData.progressPercent}%;"></div>

                    ${timelineData.steps.map(s => `
                        <div class="timeline-step ${s.state}">
                            <div class="timeline-node">
                                <i class="bi ${s.icon}"></i>
                            </div>
                            <div class="timeline-label">${s.label}</div>
                            ${s.time ? `<div class="text-muted" style="font-size: 0.72rem;">${s.time}</div>` : ''}
                        </div>
                    `).join('')}
                </div>
            </div>

            ${dStatus === 'FAILED' || dStatus === 'DELIVERY_FAILED' ? `
                <div class="alert alert-danger py-2 small mb-0 rounded-3">
                    <i class="bi bi-exclamation-octagon-fill me-1"></i>
                    Delivery Attempt Failed: ${delivery && delivery.deliveryNotes ? delivery.deliveryNotes : 'Consignment could not be delivered. Courier will re-attempt shortly.'}
                </div>
            ` : ''}
        </div>

        <!-- LOGISTICS & COURIER CARD -->
        <div class="row g-3">
            <div class="col-md-7">
                <div class="tracking-card h-100">
                    <h6 class="fw-bold text-dark border-bottom pb-2 mb-3">
                        <i class="bi bi-truck text-success me-2"></i> Courier & Shipment Details
                    </h6>

                    <div class="row g-3 mb-3">
                        <div class="col-sm-6">
                            <label class="text-muted small d-block">Tracking Number</label>
                            <div class="fw-bold text-dark d-flex align-items-center gap-2">
                                <span>${trackingNum}</span>
                                ${(trackingNum && trackingNum !== 'Pending dispatch') ? `
                                    <button class="btn btn-link btn-sm p-0 text-success text-decoration-none" onclick="copyTracking('${trackingNum}')" title="Copy tracking number">
                                        <i class="bi bi-clipboard"></i>
                                    </button>
                                ` : ''}
                            </div>
                        </div>
                        <div class="col-sm-6">
                            <label class="text-muted small d-block">Estimated Delivery Date</label>
                            <div class="fw-bold text-success">${estDateStr}</div>
                        </div>
                        <div class="col-sm-6">
                            <label class="text-muted small d-block">Logistics Partner</label>
                            <div class="fw-semibold text-dark">${partner}</div>
                        </div>
                        <div class="col-sm-6">
                            <label class="text-muted small d-block">Delivery Agent Contact</label>
                            <div class="fw-semibold text-dark">${partnerPhone}</div>
                        </div>
                    </div>

                    ${partnerTrackingUrl ? `
                        <div class="mt-2 mb-2">
                            <a href="${partnerTrackingUrl}" target="_blank" class="btn btn-outline-success btn-sm">
                                <i class="bi bi-box-arrow-up-right me-1"></i> Track on Carrier Website
                            </a>
                        </div>
                    ` : ''}

                    ${delivery && delivery.deliveryNotes ? `
                        <div class="p-2 bg-light rounded small text-secondary">
                            <strong>Status Log:</strong> ${delivery.deliveryNotes}
                        </div>
                    ` : ''}
                </div>
            </div>

            <div class="col-md-5">
                <div class="tracking-card h-100">
                    <h6 class="fw-bold text-dark border-bottom pb-2 mb-3">
                        <i class="bi bi-pin-map text-success me-2"></i> Delivery Address
                    </h6>

                    <div class="fw-bold text-dark mb-1">${order.recipientName}</div>
                    <div class="text-muted small mb-2"><i class="bi bi-telephone me-1"></i> +91 ${order.recipientPhone}</div>
                    <div class="text-secondary small">
                        ${order.addressLine1}${order.addressLine2 ? ', ' + order.addressLine2 : ''}<br>
                        ${order.city}, ${order.state} - <strong>${order.pincode}</strong>
                        ${order.landmark ? `<br><span class="text-muted">Near: ${order.landmark}</span>` : ''}
                    </div>
                </div>
            </div>
        </div>

        <!-- REAL-TIME TRACKING TIMELINE / MILESTONES -->
        ${fulfillment && fulfillment.trackingEvents && fulfillment.trackingEvents.length > 0 ? `
            <div class="tracking-card mt-3">
                <h6 class="fw-bold text-dark border-bottom pb-2 mb-3">
                    <i class="bi bi-clock-history text-success me-2"></i> Real-time Consignment Milestones (${fulfillment.trackingEvents.length})
                </h6>
                <div class="list-group list-group-flush">
                    ${fulfillment.trackingEvents.map(ev => `
                        <div class="list-group-item px-0 py-2 d-flex justify-content-between align-items-start">
                            <div>
                                <span class="badge bg-success-subtle text-success me-2">${ev.status}</span>
                                <span class="fw-semibold text-dark">${ev.description || ''}</span>
                                <div class="text-muted small">${ev.location || 'Facility'}</div>
                            </div>
                            <div class="text-secondary small text-nowrap">
                                ${ev.timestamp ? new Date(ev.timestamp).toLocaleString('en-IN') : ''}
                            </div>
                        </div>
                    `).join('')}
                </div>
            </div>
        ` : ''}

        <!-- ITEMS IN CONSIGNMENT -->
        <div class="tracking-card mt-3">
            <h6 class="fw-bold text-dark border-bottom pb-2 mb-3">
                <i class="bi bi-box-seam text-success me-2"></i> Items in this Consignment (${order.items ? order.items.length : 0})
            </h6>

            <div class="list-group list-group-flush">
                ${(order.items || []).map(item => `
                    <div class="list-group-item px-0 py-2 d-flex justify-content-between align-items-center">
                        <div class="d-flex align-items-center gap-3">
                            <img src="${item.imageUrl || '../assets/fertilizers/default.png'}" 
                                 alt="${item.productName}" 
                                 class="rounded border" 
                                 style="width: 48px; height: 48px; object-fit: cover;">
                            <div>
                                <div class="fw-bold small text-dark">${item.productName}</div>
                                <div class="text-muted" style="font-size: 0.78rem;">
                                    SKU: ${item.sku} &bull; Qty: ${item.quantity} ${item.unit || 'Bag'}
                                </div>
                            </div>
                        </div>
                        <div class="fw-bold text-dark small">₹${Number(item.lineTotal || 0).toFixed(2)}</div>
                    </div>
                `).join('')}
            </div>
        </div>

        <!-- ACTION FOOTER -->
        <div class="d-flex flex-wrap justify-content-between align-items-center gap-2 mt-4">
            <a href="customer-orders.html" class="btn btn-outline-secondary">
                <i class="bi bi-arrow-left me-1"></i> Back to Orders
            </a>

            <div class="d-flex gap-2">
                <a href="customer-invoice.html?orderId=${order.id}" class="btn btn-outline-success">
                    <i class="bi bi-receipt me-1"></i> View GST Invoice
                </a>
                <a href="${(window.CONFIG && window.CONFIG.API_BASE_URL) ? window.CONFIG.API_BASE_URL : 'http://localhost:8080/api'}/orders/${order.id}/invoice/pdf" target="_blank" class="btn btn-success">
                    <i class="bi bi-file-earmark-pdf me-1"></i> Download Official PDF
                </a>
            </div>
        </div>
    `;
}

function getTimelineState(orderStatus, deliveryStatus) {
    const steps = [
        { key: 'CONFIRMED', label: 'Order Confirmed', icon: 'bi-check2-circle' },
        { key: 'PROCESSING', label: 'Processing', icon: 'bi-gear' },
        { key: 'PACKED', label: 'Packed', icon: 'bi-box-seam' },
        { key: 'SHIPPED', label: 'Dispatched', icon: 'bi-truck' },
        { key: 'OUT_FOR_DELIVERY', label: 'Out for Delivery', icon: 'bi-bicycle' },
        { key: 'DELIVERED', label: 'Delivered', icon: 'bi-house-check' }
    ];

    if (orderStatus === 'CANCELLED') {
        return {
            progressPercent: 0,
            steps: steps.map(s => ({ ...s, state: 'cancelled' }))
        };
    }

    // Determine current step index
    let currentIdx = 0;
    if (deliveryStatus === 'DELIVERED' || orderStatus === 'DELIVERED') currentIdx = 5;
    else if (deliveryStatus === 'OUT_FOR_DELIVERY') currentIdx = 4;
    else if (deliveryStatus === 'SHIPPED' || orderStatus === 'SHIPPED') currentIdx = 3;
    else if (deliveryStatus === 'PACKED' || orderStatus === 'PACKED') currentIdx = 2;
    else if (orderStatus === 'PROCESSING' || deliveryStatus === 'ASSIGNED') currentIdx = 1;
    else if (orderStatus === 'CONFIRMED' || orderStatus === 'PENDING') currentIdx = 0;

    const progressPercent = Math.min(100, Math.round((currentIdx / (steps.length - 1)) * 100));

    const enrichedSteps = steps.map((s, idx) => {
        let state = '';
        if (idx < currentIdx) state = 'completed';
        else if (idx === currentIdx) state = 'active';
        return { ...s, state };
    });

    return { progressPercent, steps: enrichedSteps };
}

function getStatusBadgeClass(status) {
    switch (status) {
        case 'CONFIRMED': return 'status-badge-confirmed';
        case 'PROCESSING': return 'status-badge-processing';
        case 'PACKED': return 'status-badge-packed';
        case 'SHIPPED': return 'status-badge-shipped';
        case 'DELIVERED': return 'status-badge-delivered';
        case 'CANCELLED': return 'status-badge-cancelled';
        default: return 'status-badge-pending';
    }
}

function copyTracking(text) {
    navigator.clipboard.writeText(text).then(() => {
        if (typeof showToast !== 'undefined') {
            showToast('Tracking number copied!', 'success');
        } else {
            alert('Tracking number copied: ' + text);
        }
    });
}

function logout() {
    localStorage.removeItem('smartagri_auth_token');
    localStorage.removeItem('smartagri_user');
    window.location.href = 'customer-login.html';
}
