/* =========================================================
   SMARTAGRI CUSTOMER INVOICE
   Phase 5 GST Invoice Generation & Presentation
   ========================================================= */

document.addEventListener('DOMContentLoaded', async () => {
    const params = new URLSearchParams(window.location.search);
    const orderId = params.get('orderId');

    const sheet = document.getElementById('invoiceSheet');
    const pdfBtn = document.getElementById('downloadPdfBtn');

    if (!orderId) {
        sheet.innerHTML = `
            <div class="alert alert-danger text-center py-4">
                <h5>Invalid Request</h5>
                <p class="mb-0 text-muted">No order specified for invoice generation.</p>
            </div>
        `;
        return;
    }

    if (pdfBtn) {
        const baseUrl = (window.CONFIG && window.CONFIG.API_BASE_URL) ? window.CONFIG.API_BASE_URL : 'http://localhost:8080/api';
        pdfBtn.href = `${baseUrl}/orders/${orderId}/invoice/pdf`;
    }

    try {
        const res = await ApiClient.get(`/orders/${orderId}/invoice`);
        if (!res || !res.success || !res.data) {
            sheet.innerHTML = `
                <div class="alert alert-danger text-center py-4">
                    <h5><i class="bi bi-exclamation-triangle-fill me-2"></i> Invoice Unavailable</h5>
                    <p class="mb-0 text-muted">${res ? res.message : 'Unable to retrieve tax invoice.'}</p>
                </div>
            `;
            return;
        }

        renderInvoice(res.data);

    } catch (e) {
        sheet.innerHTML = `
            <div class="alert alert-danger text-center py-4">
                <h5>Communication Error</h5>
                <p class="mb-0 text-muted">${e.message || 'Failed to communicate with invoice service.'}</p>
            </div>
        `;
    }
});

function renderInvoice(inv) {
    const sheet = document.getElementById('invoiceSheet');

    const invDateStr = inv.invoiceDate ? new Date(inv.invoiceDate).toLocaleDateString('en-IN', {
        day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
    }) : 'Recent';

    const subtotal = Number(inv.subtotal) || 0;
    const discount = Number(inv.discountAmount) || 0;
    const taxable = Number(inv.taxableAmount) || 0;
    const cgst = Number(inv.cgstAmount) || 0;
    const sgst = Number(inv.sgstAmount) || 0;
    const igst = Number(inv.igstAmount) || 0;
    const shipping = Number(inv.shippingAmount) || 0;
    const grandTotal = Number(inv.grandTotal) || 0;

    sheet.innerHTML = `
        <!-- HEADER -->
        <div class="d-flex justify-content-between align-items-start pb-3 border-bottom mb-4">
            <div>
                <div class="invoice-brand"><i class="bi bi-flower1 me-1"></i> Smart<strong>Agri</strong></div>
                <div class="text-muted small">Fertilizer Sales, Inventory & E-Commerce Management</div>
            </div>
            <div class="text-end">
                <span class="badge bg-success px-3 py-1 rounded-pill fw-bold mb-1">TAX INVOICE</span>
                <div class="fw-bold text-dark mt-1">${inv.invoiceNumber}</div>
                <div class="text-muted small">Date: ${invDateStr}</div>
                <div class="text-muted small">Order Ref: <strong>${inv.orderNumber}</strong></div>
            </div>
        </div>

        <!-- SELLER & BUYER GRID -->
        <div class="row g-3 mb-4">
            <div class="col-sm-6">
                <div class="info-box h-100">
                    <div class="text-uppercase fw-bold text-success small mb-2">Sold By (Seller)</div>
                    <div class="fw-bold text-dark">${inv.sellerName}</div>
                    <div class="text-dark small"><strong>GSTIN:</strong> ${inv.sellerGstin}</div>
                    <div class="text-secondary small mt-1">${inv.sellerAddress}</div>
                    <div class="text-secondary small">State: ${inv.sellerState}</div>
                </div>
            </div>

            <div class="col-sm-6">
                <div class="info-box h-100">
                    <div class="text-uppercase fw-bold text-success small mb-2">Billed & Delivered To (Buyer)</div>
                    <div class="fw-bold text-dark">${inv.buyerName}</div>
                    <div class="text-dark small"><strong>Phone:</strong> +91 ${inv.buyerPhone}</div>
                    <div class="text-secondary small mt-1">${inv.buyerAddress}</div>
                    <div class="text-secondary small">${inv.buyerCity}, ${inv.buyerState} - <strong>${inv.buyerPincode}</strong></div>
                </div>
            </div>
        </div>

        <!-- LINE ITEMS TABLE -->
        <div class="table-responsive mb-4">
            <table class="table table-bordered table-invoice align-middle mb-0">
                <thead>
                    <tr>
                        <th style="width: 5%;">#</th>
                        <th>Product & SKU</th>
                        <th class="text-center" style="width: 10%;">Qty</th>
                        <th class="text-end" style="width: 14%;">Unit Price</th>
                        <th class="text-end" style="width: 12%;">Discount</th>
                        <th class="text-end" style="width: 14%;">GST (5%)</th>
                        <th class="text-end" style="width: 15%;">Line Total</th>
                    </tr>
                </thead>
                <tbody>
                    ${(inv.items || []).map((item, idx) => {
                        const unitPrice = Number(item.unitPrice) || 0;
                        const itemDisc = Number(item.discountAmount) || 0;
                        const itemTax = Number(item.taxAmount) || 0;
                        const lineTot = Number(item.lineTotal) || 0;

                        return `
                            <tr>
                                <td class="text-muted">${idx + 1}</td>
                                <td>
                                    <div class="fw-bold text-dark">${item.productName}</div>
                                    <div class="text-muted" style="font-size: 0.76rem;">SKU: ${item.sku} &bull; ${item.brand || 'SmartAgri'}</div>
                                </td>
                                <td class="text-center">${item.quantity} ${item.unit || 'Bag'}</td>
                                <td class="text-end">₹${unitPrice.toFixed(2)}</td>
                                <td class="text-end text-muted">${itemDisc > 0 ? '-₹' + itemDisc.toFixed(2) : '—'}</td>
                                <td class="text-end">₹${itemTax.toFixed(2)}</td>
                                <td class="text-end fw-bold text-dark">₹${lineTot.toFixed(2)}</td>
                            </tr>
                        `;
                    }).join('')}
                </tbody>
            </table>
        </div>

        <!-- TOTALS & TAX BREAKDOWN -->
        <div class="row g-3">
            <div class="col-sm-6">
                <div class="p-3 bg-light rounded border h-100">
                    <h6 class="fw-bold small text-muted text-uppercase mb-2">Payment Information</h6>
                    <div class="d-flex justify-content-between small py-1">
                        <span class="text-muted">Payment Method:</span>
                        <span class="fw-bold">${inv.paymentMethod}</span>
                    </div>
                    <div class="d-flex justify-content-between small py-1">
                        <span class="text-muted">Payment Status:</span>
                        <span class="badge ${inv.paymentStatus === 'PAID' ? 'bg-success' : 'bg-warning text-dark'}">${inv.paymentStatus}</span>
                    </div>
                    <div class="mt-3 text-muted" style="font-size: 0.78rem;">
                        <i class="bi bi-shield-check text-success me-1"></i>
                        GST compliant computerized invoice generated electronically.
                    </div>
                </div>
            </div>

            <div class="col-sm-6">
                <div class="table-responsive">
                    <table class="table table-sm table-borderless align-middle mb-0">
                        <tr>
                            <td class="text-muted">Subtotal:</td>
                            <td class="text-end fw-semibold">₹${subtotal.toFixed(2)}</td>
                        </tr>
                        ${discount > 0 ? `
                            <tr>
                                <td class="text-muted">Item Discount:</td>
                                <td class="text-end text-success">-₹${discount.toFixed(2)}</td>
                            </tr>
                        ` : ''}
                        <tr>
                            <td class="text-muted">Taxable Value:</td>
                            <td class="text-end fw-semibold">₹${taxable.toFixed(2)}</td>
                        </tr>
                        ${cgst > 0 ? `
                            <tr>
                                <td class="text-muted">CGST (2.5%):</td>
                                <td class="text-end">₹${cgst.toFixed(2)}</td>
                            </tr>
                            <tr>
                                <td class="text-muted">SGST (2.5%):</td>
                                <td class="text-end">₹${sgst.toFixed(2)}</td>
                            </tr>
                        ` : `
                            <tr>
                                <td class="text-muted">IGST (5.0%):</td>
                                <td class="text-end">₹${igst.toFixed(2)}</td>
                            </tr>
                        `}
                        <tr>
                            <td class="text-muted">Shipping Charges:</td>
                            <td class="text-end">${shipping === 0 ? '<span class="text-success fw-bold">FREE</span>' : '₹' + shipping.toFixed(2)}</td>
                        </tr>
                        <tr class="border-top">
                            <td class="fw-bold fs-5 text-dark pt-2">Grand Total:</td>
                            <td class="text-end fw-bold fs-5 text-success pt-2">₹${grandTotal.toFixed(2)}</td>
                        </tr>
                    </table>
                </div>
            </div>
        </div>

        <div class="text-center text-muted small mt-5 pt-3 border-top">
            Thank you for farming with SmartAgri! For support, contact support@smartagri.com or call 1800-SMARTAGRI.
        </div>
    `;
}
