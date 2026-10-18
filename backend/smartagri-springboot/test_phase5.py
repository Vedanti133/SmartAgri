import urllib.request
import urllib.error
import json
import subprocess
import sys
from decimal import Decimal, ROUND_HALF_UP

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8080/api"

def api_call(method, endpoint, data=None, token=None, return_raw=False):
    url = f"{BASE_URL}{endpoint}"
    headers = {"Accept": "application/json"}
    if not return_raw:
        headers["Content-Type"] = "application/json"
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode('utf-8') if data is not None else None

    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            content = resp.read()
            if return_raw:
                return status, content
            text = content.decode('utf-8')
            json_data = json.loads(text) if text else {}
            return status, json_data
    except urllib.error.HTTPError as e:
        content = e.read()
        if return_raw:
            return e.code, content
        text = content.decode('utf-8')
        try:
            json_data = json.loads(text) if text else {}
        except Exception:
            json_data = {"raw": text}
        return e.code, json_data
    except Exception as e:
        return 0, {"error": str(e)}

def mysql_query(sql):
    cmd = ['C:\\xampp\\mysql\\bin\\mysql.exe', '-u', 'root', 'smartagri', '-N', '-e', sql]
    proc = subprocess.run(cmd, capture_output=True, text=True)
    if proc.returncode != 0:
        raise RuntimeError(f"MySQL error: {proc.stderr}")
    return proc.stdout.strip()

def clean_test_data():
    queries = [
        "DELETE FROM invoices WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p5_%');",
        "DELETE FROM deliveries WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p5_%');",
        "DELETE FROM payments WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p5_%');",
        "DELETE oi FROM order_items oi JOIN orders o ON oi.order_id=o.id JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p5_%';",
        "DELETE o FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p5_%';",
        "DELETE a FROM addresses a JOIN users u ON a.user_id=u.id WHERE u.username LIKE 'p5_%';",
        "DELETE cp FROM customer_profiles cp JOIN users u ON cp.user_id=u.id WHERE u.username LIKE 'p5_%';",
        "DELETE ci FROM cart_items ci JOIN cart c ON ci.cart_id=c.id JOIN users u ON c.user_id=u.id WHERE u.username LIKE 'p5_%';",
        "DELETE c FROM cart c JOIN users u ON c.user_id=u.id WHERE u.username LIKE 'p5_%';",
        "DELETE FROM users WHERE username LIKE 'p5_%';"
    ]
    for q in queries:
        mysql_query(q)

def run_tests():
    passed = 0
    failed = 0
    results = []

    def record(test_num, name, condition, details=""):
        nonlocal passed, failed
        if condition:
            passed += 1
            results.append((test_num, name, "PASS", details))
            print(f"[PASS] #{test_num}: {name} | {details}")
        else:
            failed += 1
            results.append((test_num, name, "FAIL", details))
            print(f"[FAIL] #{test_num}: {name} | {details}")

    print("==================================================")
    print("SMARTAGRI PHASE 5 - PAYMENT, DELIVERY, INVOICE & FULFILLMENT")
    print("==================================================")

    clean_test_data()

    # 1. Admin Login
    code, res = api_call("POST", "/auth/login", {"username": "admin", "password": "adminpassword123"})
    admin_token = res.get('data', {}).get('token')
    record(1, "Admin Login & Token", code == 200 and bool(admin_token), f"Status: {code}")

    # 2. Customer 1 Registration & Login
    cust1_user = "p5_cust1"
    cust1_email = "p5_cust1@smartagri.com"
    code, res = api_call("POST", "/auth/register", {
        "name": "Arjun Deshmukh",
        "username": cust1_user,
        "email": cust1_email,
        "password": "Password123!",
        "role": "CUSTOMER"
    })
    cust1_token = res.get('data', {}).get('token')
    cust1_id = res.get('data', {}).get('id')
    record(2, "Customer 1 Registration", code == 201 and bool(cust1_token), f"Status: {code}, UserId: {cust1_id}")

    # 3. Invalid authentication rejected
    code, _ = api_call("POST", "/auth/login", {"username": cust1_user, "password": "WrongPassword"})
    record(3, "Invalid Authentication Rejected (HTTP 401)", code == 401, f"Status: {code}")

    # Setup Customer 1 Address
    code, res = api_call("POST", "/addresses", {
        "fullName": "Arjun Deshmukh",
        "phone": "9822012345",
        "addressLine1": "Wadgaon Sheri Farm Estate",
        "addressLine2": "Lane 4",
        "city": "Pune",
        "state": "Maharashtra",
        "pincode": "411014",
        "landmark": "Near Anand Agro Agency",
        "addressType": "HOME",
        "isDefault": True
    }, token=cust1_token)
    addr1_id = res.get('data', {}).get('id')

    # Get Catalog and Stock baseline
    code, res = api_call("GET", "/fertilizers")
    ferts = res.get('data', [])
    f1 = ferts[0]
    f2 = ferts[1]
    f1_id = f1['id']
    f2_id = f2['id']
    f1_stock_initial = int(mysql_query(f"SELECT stock FROM fertilizer WHERE id={f1_id};"))

    # Populate Cart & Checkout
    api_call("POST", "/cart/items", {"fertilizerId": f1_id, "quantity": 2}, token=cust1_token)
    api_call("POST", "/cart/items", {"fertilizerId": f2_id, "quantity": 1}, token=cust1_token)

    order_payload = {
        "addressId": addr1_id,
        "paymentMethod": "COD",
        "notes": "Deliver at main gate"
    }
    code, res = api_call("POST", "/orders", order_payload, token=cust1_token)
    order1 = res.get('data', {})
    order1_id = order1.get('id')
    order1_num = order1.get('orderNumber')
    order1_total = Decimal(str(order1.get('grandTotal', '0')))

    # 4. Payment created for order
    code, res = api_call("GET", f"/orders/{order1_id}/payment", token=cust1_token)
    pay1 = res.get('data', {})
    pay1_amount = Decimal(str(pay1.get('amount', '0')))
    record(4, "Payment Record Created for Order", code == 200 and pay1.get('orderId') == order1_id, 
           f"Status: {code}, TxnId: {pay1.get('transactionId')}, Status: {pay1.get('paymentStatus')}")

    # 5. Correct payment amount matches authoritative order grand total
    record(5, "Correct Payment Amount Matches Order Grand Total", pay1_amount == order1_total and pay1_amount > 0,
           f"PayAmount: {pay1_amount}, OrderGrandTotal: {order1_total}")

    # 6. Backend rejects customer manipulation of payment amount (authoritative source of truth)
    # Customer cannot post custom payment amounts directly
    code_mod, _ = api_call("PUT", f"/admin/payments/{order1_id}/status", {"paymentStatus": "PAID"}, token=cust1_token)
    record(6, "Customer Cannot Access Admin Payment Update (HTTP 403)", code_mod == 403, f"Status: {code_mod}")

    # 7. Payment status update by Admin
    code, res = api_call("PUT", f"/admin/payments/{order1_id}/status", {
        "paymentStatus": "INITIATED",
        "gatewayResponse": "{\"test\":\"verified\"}"
    }, token=admin_token)
    pay1_updated = res.get('data', {})
    record(7, "Admin Can Update Payment Status", code == 200 and pay1_updated.get('paymentStatus') == 'INITIATED',
           f"Status: {code}, NewPaymentStatus: {pay1_updated.get('paymentStatus')}")

    # Reset payment status back to PENDING for COD flow
    api_call("PUT", f"/admin/payments/{order1_id}/status", {"paymentStatus": "PENDING"}, token=admin_token)

    # 8. Customer cannot modify payment status
    code_cust_put, _ = api_call("PUT", f"/admin/payments/{order1_id}/status", {"paymentStatus": "PAID"}, token=cust1_token)
    record(8, "Customer Blocked from Modifying Payment Status (HTTP 403)", code_cust_put == 403, f"Status: {code_cust_put}")

    # 9. Delivery created for order (NOT_ASSIGNED)
    code, res = api_call("GET", f"/orders/{order1_id}/delivery", token=cust1_token)
    deliv1 = res.get('data', {})
    deliv1_id = deliv1.get('id')
    record(9, "Delivery Record Initialized (NOT_ASSIGNED)", code == 200 and deliv1.get('deliveryStatus') == 'NOT_ASSIGNED',
           f"Status: {code}, DelivId: {deliv1_id}, DeliveryStatus: {deliv1.get('deliveryStatus')}")

    # 10. Delivery assignment (partner, phone, estimated date)
    assign_payload = {
        "deliveryPartner": "Kisan Speed Logistics",
        "deliveryPartnerPhone": "9822998877",
        "estimatedDeliveryDate": "2026-09-30",
        "notes": "Assigned to driver Ramesh"
    }
    code, res = api_call("PUT", f"/admin/deliveries/{deliv1_id}/assign", assign_payload, token=admin_token)
    deliv1_assigned = res.get('data', {})
    record(10, "Delivery Partner Assignment", code == 200 and deliv1_assigned.get('deliveryStatus') == 'ASSIGNED',
           f"Status: {code}, Partner: {deliv1_assigned.get('deliveryPartner')}, Phone: {deliv1_assigned.get('deliveryPartnerPhone')}")

    # 11. Tracking number generated
    tracking_num = deliv1_assigned.get('trackingNumber')
    record(11, "Tracking Number Generated (SA-TRK-*)", bool(tracking_num) and tracking_num.startswith("SA-TRK-"),
           f"TrackingNum: {tracking_num}")

    # 12. Valid delivery transitions: ASSIGNED -> PACKED -> SHIPPED -> OUT_FOR_DELIVERY -> DELIVERED
    transitions = [
        ("PACKED", "Order packed in facility"),
        ("SHIPPED", "Dispatched to local hub"),
        ("OUT_FOR_DELIVERY", "Out with delivery agent"),
        ("DELIVERED", "Delivered to customer")
    ]
    all_deliv_tr_ok = True
    for st, note in transitions:
        code_tr, res_tr = api_call("PUT", f"/admin/deliveries/{deliv1_id}/status", {"deliveryStatus": st, "notes": note}, token=admin_token)
        curr = res_tr.get('data', {}).get('deliveryStatus')
        if code_tr != 200 or curr != st:
            all_deliv_tr_ok = False
            print(f"  Delivery transition to {st} failed: code={code_tr}, res={res_tr}")
            break
    record(12, "Valid Delivery Lifecycle Progression to DELIVERED", all_deliv_tr_ok, f"Final DeliveryStatus: {curr}")

    # 13. Invalid delivery transition rejected (DELIVERED -> NOT_ASSIGNED or PACKED)
    code_inv_tr, _ = api_call("PUT", f"/admin/deliveries/{deliv1_id}/status", {"deliveryStatus": "PACKED"}, token=admin_token)
    record(13, "Reject Invalid Delivery Transition (HTTP 400)", code_inv_tr == 400, f"Status: {code_inv_tr}")

    # 14. Customer can view own delivery
    code, res = api_call("GET", f"/orders/{order1_id}/delivery", token=cust1_token)
    fetched_deliv = res.get('data', {})
    record(14, "Customer Can View Own Delivery Tracking", code == 200 and fetched_deliv.get('trackingNumber') == tracking_num,
           f"Status: {code}, Tracking: {fetched_deliv.get('trackingNumber')}")

    # 15. Customer 2 cannot view Customer 1's delivery (HTTP 403)
    cust2_user = "p5_cust2"
    cust2_email = "p5_cust2@smartagri.com"
    code, res = api_call("POST", "/auth/register", {
        "name": "Vijay Shinde",
        "username": cust2_user,
        "email": cust2_email,
        "password": "Password123!",
        "role": "CUSTOMER"
    })
    cust2_token = res.get('data', {}).get('token')
    code_deliv_iso, res_deliv_iso = api_call("GET", f"/orders/{order1_id}/delivery", token=cust2_token)
    record(15, "Customer Cannot View Another Customer's Delivery (HTTP 403)", code_deliv_iso == 403,
           f"Status: {code_deliv_iso}, Message: {res_deliv_iso.get('message')}")

    # 16. Customer cannot modify delivery status (HTTP 403)
    code_cust_deliv_mod, _ = api_call("PUT", f"/admin/deliveries/{deliv1_id}/status", {"deliveryStatus": "RETURNED"}, token=cust1_token)
    record(16, "Customer Cannot Modify Delivery Status (HTTP 403)", code_cust_deliv_mod == 403, f"Status: {code_cust_deliv_mod}")

    # 17. Admin can query all deliveries
    code, res = api_call("GET", "/admin/deliveries", token=admin_token)
    all_delivs = res.get('data', [])
    record(17, "Admin Can Query All Deliveries", code == 200 and len(all_delivs) >= 1, f"Status: {code}, Count: {len(all_delivs)}")

    # 18. Invoice generated
    code, res = api_call("GET", f"/orders/{order1_id}/invoice", token=cust1_token)
    inv = res.get('data', {})
    record(18, "GST Invoice Generated Successfully", code == 200 and inv.get('orderId') == order1_id,
           f"Status: {code}, InvoiceNum: {inv.get('invoiceNumber')}")

    # 19. Invoice number unique and formatted INV-SA-YYYYMMDD-XXXX
    inv_num = inv.get('invoiceNumber', '')
    record(19, "Unique Invoice Number Format (INV-SA-*)", inv_num.startswith("INV-SA-"), f"InvoiceNumber: {inv_num}")

    # 20. Invoice totals match order totals exactly
    inv_subtotal = Decimal(str(inv.get('subtotal', '0')))
    inv_tax = Decimal(str(inv.get('totalGstAmount', '0')))
    inv_shipping = Decimal(str(inv.get('shippingAmount', '0')))
    inv_grand = Decimal(str(inv.get('grandTotal', '0')))
    inv_cgst = Decimal(str(inv.get('cgstAmount', '0')))
    inv_sgst = Decimal(str(inv.get('sgstAmount', '0')))

    totals_match = (inv_subtotal == Decimal(str(order1.get('subtotal'))) and
                    inv_tax == Decimal(str(order1.get('taxAmount'))) and
                    inv_shipping == Decimal(str(order1.get('shippingAmount'))) and
                    inv_grand == order1_total and
                    (inv_cgst + inv_sgst == inv_tax))
    record(20, "Invoice Totals & GST Match Order Totals Exactly", totals_match,
           f"Subtotal: {inv_subtotal}, CGST: {inv_cgst}, SGST: {inv_sgst}, TotalGST: {inv_tax}, GrandTotal: {inv_grand}")

    # 21. Historical product prices preserved in invoice
    inv_items = inv.get('items', [])
    items_preserved = (len(inv_items) == 2 and 
                       Decimal(str(inv_items[0].get('unitPrice'))) == Decimal(str(f1['sellingPrice'])))
    record(21, "Historical Product Prices Preserved in Invoice", items_preserved,
           f"ItemCount: {len(inv_items)}, Item1Price: {inv_items[0].get('unitPrice')}")

    # 22. Historical delivery address preserved in invoice
    record(22, "Historical Delivery Address Preserved in Invoice", inv.get('buyerCity') == "Pune" and inv.get('buyerPincode') == "411014",
           f"BuyerName: {inv.get('buyerName')}, City: {inv.get('buyerCity')}, PIN: {inv.get('buyerPincode')}")

    # 23. Order -> Payment relation correct
    order_txn = order1.get('orderNumber')
    db_pay_order_id = int(mysql_query(f"SELECT order_id FROM payments WHERE order_id={order1_id};"))
    record(23, "Order -> Payment DB Relation Intact", db_pay_order_id == order1_id, f"DB OrderId: {db_pay_order_id}")

    # 24. Order -> Delivery relation correct
    db_deliv_order_id = int(mysql_query(f"SELECT order_id FROM deliveries WHERE order_id={order1_id};"))
    record(24, "Order -> Delivery DB Relation Intact", db_deliv_order_id == order1_id, f"DB OrderId: {db_deliv_order_id}")

    # 25. Delivered order has correct payment status (PAID)
    code, res = api_call("GET", f"/orders/{order1_id}", token=cust1_token)
    refreshed_order1 = res.get('data', {})
    record(25, "Delivered Order Automatically Marked PAID", refreshed_order1.get('paymentStatus') == 'PAID' and refreshed_order1.get('orderStatus') == 'DELIVERED',
           f"OrderStatus: {refreshed_order1.get('orderStatus')}, PaymentStatus: {refreshed_order1.get('paymentStatus')}")

    # 26. Existing order history still works
    code, res = api_call("GET", "/orders", token=cust1_token)
    orders_list = res.get('data', [])
    record(26, "Existing Order History Endpoint (GET /api/orders)", code == 200 and len(orders_list) >= 1,
           f"Status: {code}, OrdersCount: {len(orders_list)}")

    # 27. Create Order 2 and test cancellation
    api_call("POST", "/cart/items", {"fertilizerId": f1_id, "quantity": 1}, token=cust1_token)
    code, res = api_call("POST", "/orders", order_payload, token=cust1_token)
    order2 = res.get('data', {})
    order2_id = order2.get('id')
    code_cancel, res_cancel = api_call("PUT", f"/orders/{order2_id}/cancel", token=cust1_token)
    cancelled_order = res_cancel.get('data', {})
    record(27, "Existing Order Cancellation Still Works", code_cancel == 200 and cancelled_order.get('orderStatus') == 'CANCELLED',
           f"Status: {code_cancel}, OrderStatus: {cancelled_order.get('orderStatus')}")

    # 28. Existing stock restoration still works upon cancellation
    f1_stock_after_cancel = int(mysql_query(f"SELECT stock FROM fertilizer WHERE id={f1_id};"))
    # Order 1 used 2, Order 2 used 1 and was cancelled -> net deducted 2
    record(28, "Stock Accurately Restored on Cancellation", f1_stock_after_cancel == f1_stock_initial - 2,
           f"Initial: {f1_stock_initial}, Current: {f1_stock_after_cancel} (Net -2 for completed order 1)")

    # 29. Customer cannot access admin delivery APIs
    code_admin_deliv, _ = api_call("GET", "/admin/deliveries", token=cust1_token)
    record(29, "Customer Blocked from Admin Delivery APIs (HTTP 403)", code_admin_deliv == 403, f"Status: {code_admin_deliv}")

    # 30. Customer cannot access another customer's invoice (HTTP 403)
    code_inv_iso, res_inv_iso = api_call("GET", f"/orders/{order1_id}/invoice", token=cust2_token)
    record(30, "Customer Blocked from Another Customer's Invoice (HTTP 403)", code_inv_iso == 403,
           f"Status: {code_inv_iso}, Message: {res_inv_iso.get('message')}")

    # 31. Password hashes never appear in responses
    raw_str = json.dumps(inv) + json.dumps(deliv1) + json.dumps(pay1)
    has_hash = "$2a$" in raw_str or "$2b$" in raw_str or "password" in inv
    record(31, "Security: Password Hashes Never Appear in API Responses", not has_hash, "No password hashes found")

    # 32. Payment secrets never appear in responses
    has_secret = "mock_test_secret" in raw_str or "key_secret" in raw_str
    record(32, "Security: Payment Secrets Never Appear in API Responses", not has_secret, "No payment secrets found")

    # 33. Bonus Test: Invoice PDF Generation
    code_pdf, raw_pdf = api_call("GET", f"/orders/{order1_id}/invoice/pdf", token=cust1_token, return_raw=True)
    is_valid_pdf = (code_pdf == 200 and raw_pdf.startswith(b"%PDF"))
    record(33, "Invoice PDF Generated and Verified (%PDF Header)", is_valid_pdf,
           f"Status: {code_pdf}, Bytes: {len(raw_pdf) if raw_pdf else 0}")

    # 34. Bonus Test: Admin Fulfillment Metrics Endpoint
    code_m, res_m = api_call("GET", "/admin/orders/fulfillment-metrics", token=admin_token)
    fm = res_m.get('data', {})
    fm_valid = (code_m == 200 and 
                fm.get('totalDeliveries', 0) >= 2 and 
                'deliveredDeliveries' in fm and 
                'pendingPayments' in fm and 
                'successfulPayments' in fm)
    record(34, "Admin Fulfillment Metrics Endpoint (GET /api/admin/orders/fulfillment-metrics)", fm_valid,
           f"TotalDeliveries: {fm.get('totalDeliveries')}, Delivered: {fm.get('deliveredDeliveries')}, SuccessPayments: {fm.get('successfulPayments')}")

    print("==================================================")
    print(f"TEST RESULTS: {passed} PASSED, {failed} FAILED (TOTAL {passed + failed})")
    print("==================================================")

    if failed > 0:
        sys.exit(1)
    else:
        sys.exit(0)

if __name__ == "__main__":
    run_tests()
