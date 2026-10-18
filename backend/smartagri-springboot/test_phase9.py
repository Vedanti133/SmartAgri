import urllib.request
import urllib.parse
import urllib.error
import json
import subprocess
import sys
import time
from decimal import Decimal

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8')

BASE_URL = "http://localhost:8080/api"

def api_call(method, endpoint, data=None, token=None):
    url = f"{BASE_URL}{endpoint}"
    headers = {"Accept": "application/json", "Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = json.dumps(data).encode('utf-8') if data is not None else None

    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            text = resp.read().decode('utf-8')
            json_data = json.loads(text) if text else {}
            return resp.status, json_data
    except urllib.error.HTTPError as e:
        text = e.read().decode('utf-8')
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

passed_count = 0
failed_count = 0

def record_pass(test_id, name):
    global passed_count
    passed_count += 1
    print(f"  [PASS] Test {test_id}: {name}")

def record_fail(test_id, name, reason):
    global failed_count
    failed_count += 1
    print(f"  [FAIL] Test {test_id}: {name} - Reason: {reason}")

def clean_test_data():
    queries = [
        "DELETE FROM invoices WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p9_%');",
        "DELETE FROM deliveries WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p9_%');",
        "DELETE FROM payments WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p9_%');",
        "DELETE oi FROM order_items oi JOIN orders o ON oi.order_id=o.id JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p9_%';",
        "DELETE o FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p9_%';",
        "DELETE a FROM addresses a JOIN users u ON a.user_id=u.id WHERE u.username LIKE 'p9_%';",
        "DELETE cp FROM customer_profiles cp JOIN users u ON cp.user_id=u.id WHERE u.username LIKE 'p9_%';",
        "DELETE ci FROM cart_items ci JOIN cart c ON ci.cart_id=c.id JOIN users u ON c.user_id=u.id WHERE u.username LIKE 'p9_%';",
        "DELETE c FROM cart c JOIN users u ON c.user_id=u.id WHERE u.username LIKE 'p9_%';",
        "DELETE FROM users WHERE username LIKE 'p9_%';"
    ]
    for q in queries:
        try:
            mysql_query(q)
        except Exception:
            pass

def run_tests():
    print("======================================================================")
    print("SMARTAGRI PHASE 9 AUTOMATED TEST SUITE")
    print("Payment Gateway Integration, Payment Lifecycle & Security Verification")
    print("======================================================================")

    clean_test_data()
    ts = int(time.time())
    user_a_name = f"p9_user_a_{ts}"
    user_b_name = f"p9_user_b_{ts}"

    # -------------------------------------------------------------------------
    # PART 1: AUTHENTICATION & RBAC SECURITY
    # -------------------------------------------------------------------------

    # P9-01: Unauthenticated GET /api/payments/my rejected (HTTP 401)
    try:
        st, res = api_call("GET", "/payments/my")
        if st == 401:
            record_pass("P9-01", "Unauthenticated GET /api/payments/my rejected (HTTP 401)")
        else:
            record_fail("P9-01", "Unauthenticated GET /api/payments/my", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P9-01", "Unauthenticated GET /api/payments/my", str(e))

    # P9-02: Unauthenticated GET /api/payments/{id} rejected (HTTP 401)
    try:
        st, res = api_call("GET", "/payments/1")
        if st == 401:
            record_pass("P9-02", "Unauthenticated GET /api/payments/1 rejected (HTTP 401)")
        else:
            record_fail("P9-02", "Unauthenticated GET /api/payments/1", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P9-02", "Unauthenticated GET /api/payments/1", str(e))

    # P9-03: Unauthenticated GET /api/payments/order/{orderId} rejected (HTTP 401)
    try:
        st, res = api_call("GET", "/payments/order/1")
        if st == 401:
            record_pass("P9-03", "Unauthenticated GET /api/payments/order/1 rejected (HTTP 401)")
        else:
            record_fail("P9-03", "Unauthenticated GET /api/payments/order/1", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P9-03", "Unauthenticated GET /api/payments/order/1", str(e))

    # P9-04: Unauthenticated POST /api/payments/{id}/confirm rejected (HTTP 401)
    try:
        st, res = api_call("POST", "/payments/1/confirm", {"gatewayPaymentId": "pay_test"})
        if st == 401:
            record_pass("P9-04", "Unauthenticated POST /api/payments/1/confirm rejected (HTTP 401)")
        else:
            record_fail("P9-04", "Unauthenticated POST /api/payments/1/confirm", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P9-04", "Unauthenticated POST /api/payments/1/confirm", str(e))

    # P9-05: Unauthenticated POST /api/payments/{id}/retry rejected (HTTP 401)
    try:
        st, res = api_call("POST", "/payments/1/retry")
        if st == 401:
            record_pass("P9-05", "Unauthenticated POST /api/payments/1/retry rejected (HTTP 401)")
        else:
            record_fail("P9-05", "Unauthenticated POST /api/payments/1/retry", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P9-05", "Unauthenticated POST /api/payments/1/retry", str(e))

    # P9-06: Unauthenticated GET /api/admin/payments rejected (HTTP 401)
    try:
        st, res = api_call("GET", "/admin/payments")
        if st == 401:
            record_pass("P9-06", "Unauthenticated GET /api/admin/payments rejected (HTTP 401)")
        else:
            record_fail("P9-06", "Unauthenticated GET /api/admin/payments", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P9-06", "Unauthenticated GET /api/admin/payments", str(e))

    # Register & Login Admin, User A, and User B
    admin_token = None
    token_a = None
    user_a_id = None
    token_b = None
    user_b_id = None

    try:
        # Admin login
        st, res = api_call("POST", "/auth/login", {"username": "admin", "password": "adminpassword123"})
        if st == 200 and res.get("success"):
            admin_token = res["data"]["token"]

        # Register User A
        st_a, res_a = api_call("POST", "/auth/register", {
            "name": "Kisan Ramesh",
            "username": user_a_name,
            "email": f"{user_a_name}@smartagri.com",
            "password": "Password123!",
            "role": "CUSTOMER"
        })
        if st_a in (200, 201) and res_a.get("success"):
            user_a_id = res_a["data"]["id"]
            _, log_a = api_call("POST", "/auth/login", {"username": user_a_name, "password": "Password123!"})
            token_a = log_a["data"]["token"]

        # Register User B
        st_b, res_b = api_call("POST", "/auth/register", {
            "name": "Kisan Suresh",
            "username": user_b_name,
            "email": f"{user_b_name}@smartagri.com",
            "password": "Password123!",
            "role": "CUSTOMER"
        })
        if st_b in (200, 201) and res_b.get("success"):
            user_b_id = res_b["data"]["id"]
            _, log_b = api_call("POST", "/auth/login", {"username": user_b_name, "password": "Password123!"})
            token_b = log_b["data"]["token"]

        if admin_token and token_a and token_b:
            record_pass("P9-07", "Admin, Customer User A, and Customer User B authenticated")
        else:
            record_fail("P9-07", "User Authentication", f"Admin: {bool(admin_token)}, UserA: {bool(token_a)}, UserB: {bool(token_b)}")
    except Exception as e:
        record_fail("P9-07", "User Authentication", str(e))

    # P9-08: Customer role accessing /api/admin/payments rejected (HTTP 403)
    try:
        st, res = api_call("GET", "/admin/payments", token=token_a)
        if st == 403:
            record_pass("P9-08", "Customer role accessing /api/admin/payments rejected with 403 Forbidden")
        else:
            record_fail("P9-08", "Admin payments RBAC check", f"Expected 403, got {st}: {res}")
    except Exception as e:
        record_fail("P9-08", "Admin payments RBAC check", str(e))

    # P9-09: Customer role accessing /api/admin/payments/{id}/status rejected (HTTP 403)
    try:
        st, res = api_call("PUT", "/admin/payments/1/status", {"paymentStatus": "PAID"}, token=token_a)
        if st == 403:
            record_pass("P9-09", "Customer role updating admin payment status rejected with 403 Forbidden")
        else:
            record_fail("P9-09", "Admin status update RBAC check", f"Expected 403, got {st}: {res}")
    except Exception as e:
        record_fail("P9-09", "Admin status update RBAC check", str(e))

    # Setup address and place Order 1 for User A (ONLINE)
    addr_a_id = None
    order_a1_id = None
    order_a1_num = None
    payment_a1_id = None
    payment_a1_amount = None
    gw_order_a1_id = None

    try:
        st, res = api_call("POST", "/addresses", {
            "fullName": "Kisan Ramesh",
            "phone": "9876543210",
            "addressLine1": "Plot 101, Farm Colony",
            "city": "Nashik",
            "state": "Maharashtra",
            "pincode": "422003",
            "addressType": "FARM",
            "default": True
        }, token=token_a)
        if st in (200, 201) and res.get("success"):
            addr_a_id = res["data"]["id"]

        # Add items to User A cart and checkout ONLINE
        api_call("POST", "/cart/items", {"fertilizerId": 1, "quantity": 2}, token=token_a)
        st, ord_res = api_call("POST", "/orders", {
            "addressId": addr_a_id,
            "paymentMethod": "ONLINE"
        }, token=token_a)

        if st in (200, 201) and ord_res.get("success"):
            order_a1_id = ord_res["data"]["id"]
            order_a1_num = ord_res["data"]["orderNumber"]

            # Fetch payment for Order 1
            st_p, pay_res = api_call("GET", f"/payments/order/{order_a1_id}", token=token_a)
            if st_p == 200 and pay_res.get("success"):
                payment_a1_id = pay_res["data"]["id"]
                payment_a1_amount = Decimal(str(pay_res["data"]["amount"]))
                gw_order_a1_id = pay_res["data"]["gatewayOrderId"]

        if order_a1_id and payment_a1_id and gw_order_a1_id:
            record_pass("P9-10", f"Order #{order_a1_num} created for User A with initial ONLINE payment #{payment_a1_id}")
        else:
            record_fail("P9-10", "Order creation & payment init", f"Ord: {order_a1_id}, Pay: {payment_a1_id}, GW: {gw_order_a1_id}")
    except Exception as e:
        record_fail("P9-10", "Order creation & payment init", str(e))

    # -------------------------------------------------------------------------
    # PART 2: CROSS-USER PAYMENT SECURITY & ACCESS CONTROL
    # -------------------------------------------------------------------------

    # P9-11: User B cannot view User A's payment by paymentId (GET /api/payments/{id} -> 403)
    try:
        st, res = api_call("GET", f"/payments/{payment_a1_id}", token=token_b)
        if st == 403:
            record_pass("P9-11", "Cross-user payment access rejected (User B cannot view User A's payment ID)")
        else:
            record_fail("P9-11", "Cross-user payment ID access", f"Expected 403, got {st}: {res}")
    except Exception as e:
        record_fail("P9-11", "Cross-user payment ID access", str(e))

    # P9-12: User B cannot view User A's payment by orderId (GET /api/payments/order/{id} -> 403)
    try:
        st, res = api_call("GET", f"/payments/order/{order_a1_id}", token=token_b)
        if st == 403:
            record_pass("P9-12", "Cross-user order payment access rejected (User B cannot view User A's order payment)")
        else:
            record_fail("P9-12", "Cross-user order payment access", f"Expected 403, got {st}: {res}")
    except Exception as e:
        record_fail("P9-12", "Cross-user order payment access", str(e))

    # P9-13: User B cannot confirm User A's payment (POST /api/payments/{id}/confirm -> 403)
    try:
        st, res = api_call("POST", f"/payments/{payment_a1_id}/confirm", {
            "gatewayOrderId": gw_order_a1_id,
            "gatewayPaymentId": "pay_hacker_123",
            "amount": float(payment_a1_amount)
        }, token=token_b)
        if st == 403:
            record_pass("P9-13", "Cross-user payment confirmation rejected (User B cannot confirm User A's payment)")
        else:
            record_fail("P9-13", "Cross-user payment confirmation", f"Expected 403, got {st}: {res}")
    except Exception as e:
        record_fail("P9-13", "Cross-user payment confirmation", str(e))

    # P9-14: User B cannot retry User A's payment (POST /api/payments/{id}/retry -> 403)
    try:
        st, res = api_call("POST", f"/payments/{payment_a1_id}/retry", token=token_b)
        if st == 403:
            record_pass("P9-14", "Cross-user payment retry rejected (User B cannot retry User A's payment)")
        else:
            record_fail("P9-14", "Cross-user payment retry", f"Expected 403, got {st}: {res}")
    except Exception as e:
        record_fail("P9-14", "Cross-user payment retry", str(e))

    # -------------------------------------------------------------------------
    # PART 3: AUTHORITATIVE SERVER-SIDE AMOUNT VERIFICATION
    # -------------------------------------------------------------------------

    # P9-15: Client tampered amount (underpaid by ₹100) rejected with HTTP 400
    try:
        tampered_low = float(payment_a1_amount - Decimal('100.00'))
        st, res = api_call("POST", f"/payments/{payment_a1_id}/confirm", {
            "gatewayOrderId": gw_order_a1_id,
            "gatewayPaymentId": "pay_test_tamper_low",
            "amount": tampered_low
        }, token=token_a)
        if st == 400 and "mismatch" in res.get("message", "").lower():
            record_pass("P9-15", f"Server rejected underpaid amount tampering (₹{tampered_low} vs ₹{payment_a1_amount})")
        else:
            record_fail("P9-15", "Tampered underpaid amount rejection", f"Expected 400 with mismatch message, got {st}: {res}")
    except Exception as e:
        record_fail("P9-15", "Tampered underpaid amount rejection", str(e))

    # P9-16: Client tampered amount (overpaid by ₹50) rejected with HTTP 400
    try:
        tampered_high = float(payment_a1_amount + Decimal('50.00'))
        st, res = api_call("POST", f"/payments/{payment_a1_id}/confirm", {
            "gatewayOrderId": gw_order_a1_id,
            "gatewayPaymentId": "pay_test_tamper_high",
            "amount": tampered_high
        }, token=token_a)
        if st == 400 and "mismatch" in res.get("message", "").lower():
            record_pass("P9-16", f"Server rejected overpaid amount tampering (₹{tampered_high} vs ₹{payment_a1_amount})")
        else:
            record_fail("P9-16", "Tampered overpaid amount rejection", f"Expected 400 with mismatch message, got {st}: {res}")
    except Exception as e:
        record_fail("P9-16", "Tampered overpaid amount rejection", str(e))

    # -------------------------------------------------------------------------
    # PART 4: PAYMENT GATEWAY SIGNATURES, VERIFICATION & LIFECYCLE
    # -------------------------------------------------------------------------

    # P9-17: Initial payment has INITIATED status in DB and DTO
    try:
        st, res = api_call("GET", f"/payments/{payment_a1_id}", token=token_a)
        dto = res.get("data", {})
        if st == 200 and dto.get("paymentStatus") == "INITIATED" and dto.get("gatewayOrderId"):
            record_pass("P9-17", f"Payment initial state is INITIATED with gatewayOrderId '{dto.get('gatewayOrderId')}'")
        else:
            record_fail("P9-17", "Payment initial state check", f"Data: {dto}")
    except Exception as e:
        record_fail("P9-17", "Payment initial state check", str(e))

    # P9-18: Payment confirmation with INVALID signature fails and transitions to FAILED
    try:
        st, res = api_call("POST", f"/payments/{payment_a1_id}/confirm", {
            "gatewayOrderId": gw_order_a1_id,
            "gatewayPaymentId": "pay_test_fail_1",
            "gatewaySignature": "INVALID_TEST_SIGNATURE",
            "amount": float(payment_a1_amount)
        }, token=token_a)
        # Server throws IllegalArgumentException which results in 400 Bad Request
        if st == 400:
            record_pass("P9-18", "Payment verification with INVALID signature rejected by gateway (HTTP 400)")
        else:
            record_fail("P9-18", "Invalid signature verification", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P9-18", "Invalid signature verification", str(e))

    # P9-19: Payment status is now FAILED and failureReason is stored in MySQL
    try:
        db_status = mysql_query(f"SELECT payment_status FROM payments WHERE id = {payment_a1_id};")
        db_reason = mysql_query(f"SELECT failure_reason FROM payments WHERE id = {payment_a1_id};")
        if db_status == "FAILED" and db_reason and len(db_reason) > 0:
            record_pass("P9-19", f"Payment #{payment_a1_id} status updated to FAILED with reason: '{db_reason}'")
        else:
            record_fail("P9-19", "Payment FAILED state in DB", f"Status: {db_status}, Reason: {db_reason}")
    except Exception as e:
        record_fail("P9-19", "Payment FAILED state in DB", str(e))

    # P9-20: Associated Order status reflected as FAILED
    try:
        ord_status = mysql_query(f"SELECT payment_status FROM orders WHERE id = {order_a1_id};")
        if ord_status == "FAILED":
            record_pass("P9-20", f"Order #{order_a1_num} payment_status synchronized to FAILED")
        else:
            record_fail("P9-20", "Order payment_status sync", f"Status: {ord_status}")
    except Exception as e:
        record_fail("P9-20", "Order payment_status sync", str(e))

    # P9-21: canRetry flag is TRUE for FAILED payment
    try:
        st, res = api_call("GET", f"/payments/{payment_a1_id}", token=token_a)
        if st == 200 and res.get("data", {}).get("canRetry") is True:
            record_pass("P9-21", f"Payment DTO reports canRetry=true for FAILED status")
        else:
            record_fail("P9-21", "canRetry flag on FAILED payment", f"Data: {res.get('data')}")
    except Exception as e:
        record_fail("P9-21", "canRetry flag on FAILED payment", str(e))

    # -------------------------------------------------------------------------
    # PART 5: PAYMENT RETRY FLOW & DUPLICATE PREVENTION
    # -------------------------------------------------------------------------

    # Count orders, deliveries, invoices before retry
    orders_count_before = int(mysql_query("SELECT count(*) FROM orders;"))
    deliveries_count_before = int(mysql_query("SELECT count(*) FROM deliveries;"))
    invoices_count_before = int(mysql_query("SELECT count(*) FROM invoices;"))

    retried_payment_dto = None

    # P9-22: Retry payment for failed order (POST /api/payments/order/{orderId}/retry)
    try:
        st, res = api_call("POST", f"/payments/order/{order_a1_id}/retry", {
            "paymentReference": "RETRY_ATTEMPT_1"
        }, token=token_a)
        if st == 200 and res.get("success"):
            retried_payment_dto = res["data"]
            if retried_payment_dto.get("paymentStatus") == "INITIATED" and "TXN-RETRY-" in retried_payment_dto.get("transactionId", ""):
                record_pass("P9-22", f"Payment retry succeeded: new transactionId '{retried_payment_dto['transactionId']}' (Status: INITIATED)")
            else:
                record_fail("P9-22", "Payment retry status/txn", f"DTO: {retried_payment_dto}")
        else:
            record_fail("P9-22", "Payment retry API", f"Expected 200, got {st}: {res}")
    except Exception as e:
        record_fail("P9-22", "Payment retry API", str(e))

    # P9-23: Failure reason cleared upon retry
    try:
        db_reason_after = mysql_query(f"SELECT failure_reason FROM payments WHERE id = {payment_a1_id};")
        if not db_reason_after or db_reason_after == "NULL":
            record_pass("P9-23", "Previous failure_reason successfully cleared upon retry")
        else:
            record_fail("P9-23", "Failure reason clearance", f"Reason: {db_reason_after}")
    except Exception as e:
        record_fail("P9-23", "Failure reason clearance", str(e))

    # P9-24: Verified NO duplicate order created in MySQL during retry
    try:
        orders_count_after = int(mysql_query("SELECT count(*) FROM orders;"))
        if orders_count_after == orders_count_before:
            record_pass("P9-24", f"Zero duplicate orders created on payment retry (Count: {orders_count_after})")
        else:
            record_fail("P9-24", "Duplicate order check", f"Before: {orders_count_before}, After: {orders_count_after}")
    except Exception as e:
        record_fail("P9-24", "Duplicate order check", str(e))

    # P9-25: Verified NO duplicate delivery created in MySQL during retry
    try:
        deliveries_count_after = int(mysql_query("SELECT count(*) FROM deliveries;"))
        if deliveries_count_after == deliveries_count_before:
            record_pass("P9-25", f"Zero duplicate deliveries created on payment retry (Count: {deliveries_count_after})")
        else:
            record_fail("P9-25", "Duplicate delivery check", f"Before: {deliveries_count_before}, After: {deliveries_count_after}")
    except Exception as e:
        record_fail("P9-25", "Duplicate delivery check", str(e))

    # P9-26: Confirm retried payment with valid signature -> SUCCESS
    confirmed_pay_dto = None
    try:
        st, res = api_call("POST", f"/payments/{payment_a1_id}/confirm", {
            "gatewayOrderId": retried_payment_dto.get("gatewayOrderId"),
            "gatewayPaymentId": "pay_test_success_1",
            "gatewaySignature": "valid_test_signature",
            "amount": float(payment_a1_amount)
        }, token=token_a)
        if st == 200 and res.get("success"):
            confirmed_pay_dto = res["data"]
            if confirmed_pay_dto.get("paymentStatus") == "SUCCESS":
                record_pass("P9-26", f"Retried payment confirmed: Status=SUCCESS, TxnId={confirmed_pay_dto.get('transactionId')}")
            else:
                record_fail("P9-26", "Retried payment confirmation", f"Status: {confirmed_pay_dto.get('paymentStatus')}")
        else:
            record_fail("P9-26", "Retried payment confirmation", f"Expected 200, got {st}: {res}")
    except Exception as e:
        record_fail("P9-26", "Retried payment confirmation", str(e))

    # P9-27: paid_at timestamp is populated in MySQL
    try:
        paid_at = mysql_query(f"SELECT paid_at FROM payments WHERE id = {payment_a1_id};")
        if paid_at and paid_at != "NULL":
            record_pass("P9-27", f"Settlement timestamp paid_at populated: {paid_at}")
        else:
            record_fail("P9-27", "paid_at verification", f"paid_at: {paid_at}")
    except Exception as e:
        record_fail("P9-27", "paid_at verification", str(e))

    # P9-28: Order payment_status updated to PAID
    try:
        ord_status = mysql_query(f"SELECT payment_status FROM orders WHERE id = {order_a1_id};")
        if ord_status == "PAID":
            record_pass("P9-28", f"Order #{order_a1_num} payment_status synchronized to PAID")
        else:
            record_fail("P9-28", "Order payment_status sync", f"Status: {ord_status}")
    except Exception as e:
        record_fail("P9-28", "Order payment_status sync", str(e))

    # -------------------------------------------------------------------------
    # PART 6: IDEMPOTENCY & DUPLICATE PAYMENT REJECTION
    # -------------------------------------------------------------------------

    # P9-29: Re-confirming already SUCCESS payment is idempotent (returns 200 without duplicate processing)
    try:
        st, res = api_call("POST", f"/payments/{payment_a1_id}/confirm", {
            "gatewayOrderId": retried_payment_dto.get("gatewayOrderId"),
            "gatewayPaymentId": "pay_test_duplicate_try",
            "amount": float(payment_a1_amount)
        }, token=token_a)
        if st == 200 and res.get("data", {}).get("paymentStatus") == "SUCCESS":
            record_pass("P9-29", "Payment confirmation is idempotent for already successful payment")
        else:
            record_fail("P9-29", "Idempotency re-confirm", f"Expected 200 SUCCESS, got {st}: {res}")
    except Exception as e:
        record_fail("P9-29", "Idempotency re-confirm", str(e))

    # P9-30: Cannot retry already successful payment (HTTP 400)
    try:
        st, res = api_call("POST", f"/payments/{payment_a1_id}/retry", token=token_a)
        if st == 400:
            record_pass("P9-30", "Payment retry on already successful payment rejected (HTTP 400)")
        else:
            record_fail("P9-30", "Retry on paid payment check", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P9-30", "Retry on paid payment check", str(e))

    # P9-31: Cannot initiate payment on already paid order (HTTP 400)
    try:
        st, res = api_call("POST", f"/payments/order/{order_a1_id}/initiate", token=token_a)
        if st == 400:
            record_pass("P9-31", "Payment initiation on already paid order rejected (HTTP 400)")
        else:
            record_fail("P9-31", "Initiate on paid order check", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P9-31", "Initiate on paid order check", str(e))

    # P9-32: canRetry flag is FALSE for SUCCESS payment
    try:
        st, res = api_call("GET", f"/payments/{payment_a1_id}", token=token_a)
        if st == 200 and res.get("data", {}).get("canRetry") is False:
            record_pass("P9-32", "canRetry flag is false for SUCCESS payment")
        else:
            record_fail("P9-32", "canRetry flag check", f"Data: {res.get('data')}")
    except Exception as e:
        record_fail("P9-32", "canRetry flag check", str(e))

    # -------------------------------------------------------------------------
    # PART 7: COD PAYMENT LIFECYCLE & GST INVOICE INTEGRATION
    # -------------------------------------------------------------------------

    # Setup address and place Order 2 for User B (COD)
    addr_b_id = None
    order_b_cod_id = None
    order_b_cod_num = None
    payment_b_cod_id = None

    try:
        st, res = api_call("POST", "/addresses", {
            "fullName": "Kisan Suresh",
            "phone": "9811223344",
            "addressLine1": "Village Khadakwasla, Gat 45",
            "city": "Pune",
            "state": "Maharashtra",
            "pincode": "411024",
            "addressType": "HOME",
            "default": True
        }, token=token_b)
        if st in (200, 201) and res.get("success"):
            addr_b_id = res["data"]["id"]

        api_call("POST", "/cart/items", {"fertilizerId": 2, "quantity": 1}, token=token_b)
        st, ord_res = api_call("POST", "/orders", {
            "addressId": addr_b_id,
            "paymentMethod": "COD"
        }, token=token_b)

        if st in (200, 201) and ord_res.get("success"):
            order_b_cod_id = ord_res["data"]["id"]
            order_b_cod_num = ord_res["data"]["orderNumber"]
            pay_dto = ord_res["data"]
            payment_b_cod_id = pay_dto.get("paymentId")
            if not payment_b_cod_id:
                st_p, pay_res = api_call("GET", f"/payments/order/{order_b_cod_id}", token=token_b)
                if st_p == 200:
                    payment_b_cod_id = pay_res["data"]["id"]

        if order_b_cod_id and payment_b_cod_id:
            record_pass("P9-33", f"COD Order #{order_b_cod_num} created with payment #{payment_b_cod_id}")
        else:
            record_fail("P9-33", "COD order creation", f"Order: {order_b_cod_id}, Pay: {payment_b_cod_id}")
    except Exception as e:
        record_fail("P9-33", "COD order creation", str(e))

    # P9-34: COD payment initialized with PENDING status and SYSTEM_COD gateway
    try:
        st, res = api_call("GET", f"/payments/{payment_b_cod_id}", token=token_b)
        dto = res.get("data", {})
        if st == 200 and dto.get("paymentStatus") == "PENDING" and dto.get("gatewayName") == "SYSTEM_COD":
            record_pass("P9-34", f"COD payment initialized with PENDING status and SYSTEM_COD gateway")
        else:
            record_fail("P9-34", "COD initial state check", f"Data: {dto}")
    except Exception as e:
        record_fail("P9-34", "COD initial state check", str(e))

    # P9-35: Customer can retrieve their own payments list via GET /api/payments/my
    try:
        st, res = api_call("GET", "/payments/my", token=token_a)
        if st == 200 and res.get("success") and len(res.get("data", [])) >= 1:
            record_pass("P9-35", f"Customer User A retrieved {len(res['data'])} payment records via /api/payments/my")
        else:
            record_fail("P9-35", "Customer my payments", f"Status: {st}, Data: {res.get('data')}")
    except Exception as e:
        record_fail("P9-35", "Customer my payments", str(e))

    # P9-36: Admin updates COD payment status to PAID via PUT /api/admin/payments/{orderId}/status
    try:
        st, res = api_call("PUT", f"/admin/payments/{order_b_cod_id}/status", {
            "paymentStatus": "PAID",
            "failureReason": "Cash collected on delivery by courier agent"
        }, token=admin_token)
        if st == 200 and res.get("success"):
            dto = res["data"]
            if dto.get("paymentStatus") == "PAID":
                record_pass("P9-36", f"Admin successfully updated COD payment to PAID for Order #{order_b_cod_num}")
            else:
                record_fail("P9-36", "Admin update COD status", f"Status: {dto.get('paymentStatus')}")
        else:
            record_fail("P9-36", "Admin update COD status", f"Expected 200, got {st}: {res}")
    except Exception as e:
        record_fail("P9-36", "Admin update COD status", str(e))

    # P9-37: Invoice generation is accessible once payment is settled
    try:
        st, res = api_call("GET", f"/orders/{order_a1_id}/invoice", token=token_a)
        if st == 200 and res.get("success") and res.get("data", {}).get("invoiceNumber"):
            record_pass("P9-37", f"GST Invoice verified for paid order: #{res['data']['invoiceNumber']}")
        else:
            record_fail("P9-37", "GST Invoice verification", f"Status: {st}, Data: {res.get('data')}")
    except Exception as e:
        record_fail("P9-37", "GST Invoice verification", str(e))

    # -------------------------------------------------------------------------
    # PART 8: ADMIN PAYMENT MANAGEMENT & FILTERING CONSOLE
    # -------------------------------------------------------------------------

    # P9-38: Admin GET /api/admin/payments returns all records with customer details
    try:
        st, res = api_call("GET", "/admin/payments", token=admin_token)
        if st == 200 and res.get("success") and len(res.get("data", [])) >= 2:
            first_p = res["data"][0]
            if "customerName" in first_p and "orderNumber" in first_p and "amount" in first_p:
                record_pass("P9-38", f"Admin payments ledger returned {len(res['data'])} records with customer metadata")
            else:
                record_fail("P9-38", "Admin payments metadata check", f"Data: {first_p}")
        else:
            record_fail("P9-38", "Admin payments list", f"Status: {st}, Data: {res.get('data')}")
    except Exception as e:
        record_fail("P9-38", "Admin payments list", str(e))

    # P9-39: Admin filter by status (status=SUCCESS or status=PAID)
    try:
        st, res = api_call("GET", "/admin/payments?status=SUCCESS", token=admin_token)
        if st == 200 and res.get("success"):
            items = res.get("data", [])
            all_success = all(p.get("paymentStatus") == "SUCCESS" for p in items)
            if all_success and len(items) >= 1:
                record_pass("P9-39", f"Admin status filter (?status=SUCCESS) returned {len(items)} matching records")
            else:
                record_fail("P9-39", "Admin status filter SUCCESS", f"Count: {len(items)}, all_success: {all_success}")
        else:
            record_fail("P9-39", "Admin status filter SUCCESS", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P9-39", "Admin status filter SUCCESS", str(e))

    # P9-40: Admin filter by method (method=ONLINE)
    try:
        st, res = api_call("GET", "/admin/payments?method=ONLINE", token=admin_token)
        if st == 200 and res.get("success"):
            items = res.get("data", [])
            all_online = all(p.get("paymentMethod") == "ONLINE" for p in items)
            if all_online and len(items) >= 1:
                record_pass("P9-40", f"Admin method filter (?method=ONLINE) returned {len(items)} matching records")
            else:
                record_fail("P9-40", "Admin method filter ONLINE", f"Count: {len(items)}, all_online: {all_online}")
        else:
            record_fail("P9-40", "Admin method filter ONLINE", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P9-40", "Admin method filter ONLINE", str(e))

    # P9-41: Admin filter by method (method=COD)
    try:
        st, res = api_call("GET", "/admin/payments?method=COD", token=admin_token)
        if st == 200 and res.get("success"):
            items = res.get("data", [])
            all_cod = all(p.get("paymentMethod") == "COD" for p in items)
            if all_cod and len(items) >= 1:
                record_pass("P9-41", f"Admin method filter (?method=COD) returned {len(items)} matching records")
            else:
                record_fail("P9-41", "Admin method filter COD", f"Count: {len(items)}, all_cod: {all_cod}")
        else:
            record_fail("P9-41", "Admin method filter COD", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P9-41", "Admin method filter COD", str(e))

    # P9-42: Admin search by orderNumber returns exact payment record
    try:
        st, res = api_call("GET", f"/admin/payments?search={order_a1_num}", token=admin_token)
        if st == 200 and res.get("success"):
            items = res.get("data", [])
            matched = any(p.get("orderNumber") == order_a1_num for p in items)
            if matched and len(items) == 1:
                record_pass("P9-42", f"Admin search (?search={order_a1_num}) accurately returned payment for order")
            else:
                record_fail("P9-42", "Admin search orderNumber", f"Count: {len(items)}, matched: {matched}")
        else:
            record_fail("P9-42", "Admin search orderNumber", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P9-42", "Admin search orderNumber", str(e))

    # P9-43: Admin get payment by paymentId (GET /api/admin/payments/payment/{paymentId})
    try:
        st, res = api_call("GET", f"/admin/payments/payment/{payment_a1_id}", token=admin_token)
        if st == 200 and res.get("success") and res.get("data", {}).get("id") == payment_a1_id:
            record_pass("P9-43", f"Admin audit endpoint GET /api/admin/payments/payment/{payment_a1_id} returned full details")
        else:
            record_fail("P9-43", "Admin payment audit endpoint", f"Status: {st}, Data: {res.get('data')}")
    except Exception as e:
        record_fail("P9-43", "Admin payment audit endpoint", str(e))

    # P9-44: Order cancellation auto-refunds payment if paid
    try:
        st, res = api_call("PUT", f"/orders/{order_a1_id}/cancel", token=token_a)
        if st == 200 and res.get("success"):
            # Verify payment status in DB is REFUNDED
            p_status = mysql_query(f"SELECT payment_status FROM payments WHERE id = {payment_a1_id};")
            if p_status == "REFUNDED":
                record_pass("P9-44", f"Order cancellation auto-refunded payment to REFUNDED in MySQL")
            else:
                record_fail("P9-44", "Order cancel auto-refund", f"Status: {p_status}")
        else:
            record_fail("P9-44", "Order cancel API", f"Expected 200, got {st}: {res}")
    except Exception as e:
        record_fail("P9-44", "Order cancel API", str(e))

    print("======================================================================")
    print(f"PHASE 9 TESTS COMPLETED: {passed_count} PASSED, {failed_count} FAILED (TOTAL: {passed_count + failed_count})")
    print("======================================================================")
    return failed_count == 0

if __name__ == '__main__':
    success = run_tests()
    sys.exit(0 if success else 1)
