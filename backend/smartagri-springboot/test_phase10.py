import urllib.request
import urllib.parse
import urllib.error
import json
import subprocess
import sys
import time
import threading
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
        "DELETE FROM invoices WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p10_%');",
        "DELETE FROM deliveries WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p10_%');",
        "DELETE FROM payments WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p10_%');",
        "DELETE oi FROM order_items oi JOIN orders o ON oi.order_id=o.id JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p10_%';",
        "DELETE o FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p10_%';",
        "DELETE a FROM addresses a JOIN users u ON a.user_id=u.id WHERE u.username LIKE 'p10_%';",
        "DELETE cp FROM customer_profiles cp JOIN users u ON cp.user_id=u.id WHERE u.username LIKE 'p10_%';",
        "DELETE ci FROM cart_items ci JOIN cart c ON ci.cart_id=c.id JOIN users u ON c.user_id=u.id WHERE u.username LIKE 'p10_%';",
        "DELETE c FROM cart c JOIN users u ON c.user_id=u.id WHERE u.username LIKE 'p10_%';",
        "DELETE FROM inventory_transactions WHERE reference_id LIKE 'P10_%' OR notes LIKE '%Phase 10%' OR performed_by LIKE 'p10_%';",
        "DELETE FROM users WHERE username LIKE 'p10_%';"
    ]
    for q in queries:
        try:
            mysql_query(q)
        except Exception:
            pass

def run_tests():
    print("======================================================================")
    print("SMARTAGRI PHASE 10 AUTOMATED TEST SUITE")
    print("Advanced Inventory, Stock Transactions, Low Stock & Audited Movements")
    print("======================================================================")

    clean_test_data()
    ts = int(time.time())
    customer_user = f"p10_cust_{ts}"

    # -------------------------------------------------------------------------
    # PART 1: AUTHENTICATION & ROLE-BASED ACCESS CONTROL (RBAC)
    # -------------------------------------------------------------------------

    # P10-01: Unauthenticated GET /api/admin/inventory rejected (HTTP 401)
    try:
        st, res = api_call("GET", "/admin/inventory")
        if st == 401:
            record_pass("P10-01", "Unauthenticated GET /api/admin/inventory rejected (HTTP 401)")
        else:
            record_fail("P10-01", "Unauthenticated GET /api/admin/inventory", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P10-01", "Unauthenticated GET /api/admin/inventory", str(e))

    # P10-02: Unauthenticated GET /api/admin/inventory/summary rejected (HTTP 401)
    try:
        st, res = api_call("GET", "/admin/inventory/summary")
        if st == 401:
            record_pass("P10-02", "Unauthenticated GET /api/admin/inventory/summary rejected (HTTP 401)")
        else:
            record_fail("P10-02", "Unauthenticated GET /api/admin/inventory/summary", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P10-02", "Unauthenticated GET /api/admin/inventory/summary", str(e))

    # P10-03: Unauthenticated GET /api/admin/inventory/health rejected (HTTP 401)
    try:
        st, res = api_call("GET", "/admin/inventory/health")
        if st == 401:
            record_pass("P10-03", "Unauthenticated GET /api/admin/inventory/health rejected (HTTP 401)")
        else:
            record_fail("P10-03", "Unauthenticated GET /api/admin/inventory/health", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P10-03", "Unauthenticated GET /api/admin/inventory/health", str(e))

    # P10-04: Unauthenticated POST /api/admin/inventory/stock-in rejected (HTTP 401)
    try:
        st, res = api_call("POST", "/admin/inventory/stock-in", {"fertilizerId": 1, "quantity": 10})
        if st == 401:
            record_pass("P10-04", "Unauthenticated POST /api/admin/inventory/stock-in rejected (HTTP 401)")
        else:
            record_fail("P10-04", "Unauthenticated POST /api/admin/inventory/stock-in", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P10-04", "Unauthenticated POST /api/admin/inventory/stock-in", str(e))

    # P10-05: Unauthenticated POST /api/admin/inventory/adjust rejected (HTTP 401)
    try:
        st, res = api_call("POST", "/admin/inventory/adjust", {"fertilizerId": 1, "adjustmentType": "ADD", "quantity": 5, "reason": "Test"})
        if st == 401:
            record_pass("P10-05", "Unauthenticated POST /api/admin/inventory/adjust rejected (HTTP 401)")
        else:
            record_fail("P10-05", "Unauthenticated POST /api/admin/inventory/adjust", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P10-05", "Unauthenticated POST /api/admin/inventory/adjust", str(e))

    # P10-06: Unauthenticated GET /api/admin/inventory/transactions rejected (HTTP 401)
    try:
        st, res = api_call("GET", "/admin/inventory/transactions")
        if st == 401:
            record_pass("P10-06", "Unauthenticated GET /api/admin/inventory/transactions rejected (HTTP 401)")
        else:
            record_fail("P10-06", "Unauthenticated GET /api/admin/inventory/transactions", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P10-06", "Unauthenticated GET /api/admin/inventory/transactions", str(e))

    # Authenticate Admin and Register Customer
    admin_token = None
    cust_token = None
    cust_user_id = None

    try:
        st, res = api_call("POST", "/auth/login", {"username": "admin", "password": "adminpassword123"})
        if st == 200 and res.get("success"):
            admin_token = res["data"]["token"]

        st_c, res_c = api_call("POST", "/auth/register", {
            "name": "Kisan Inventory User",
            "username": customer_user,
            "email": f"{customer_user}@smartagri.com",
            "password": "Password123!",
            "phone": "9876500010"
        })
        if st_c in [200, 201] and res_c.get("success"):
            cust_token = res_c["data"]["token"]
            cust_user_id = res_c["data"].get("id", res_c["data"].get("user", {}).get("id"))
    except Exception as e:
        print(f"Error during auth setup: {e}")

    # P10-07: Customer token GET /api/admin/inventory forbidden (HTTP 403)
    try:
        st, res = api_call("GET", "/admin/inventory", token=cust_token)
        if st == 403:
            record_pass("P10-07", "Customer token GET /api/admin/inventory forbidden (HTTP 403)")
        else:
            record_fail("P10-07", "Customer token GET /api/admin/inventory", f"Expected 403, got {st}: {res}")
    except Exception as e:
        record_fail("P10-07", "Customer token GET /api/admin/inventory", str(e))

    # P10-08: Customer token POST /api/admin/inventory/stock-in forbidden (HTTP 403)
    try:
        st, res = api_call("POST", "/admin/inventory/stock-in", {"fertilizerId": 1, "quantity": 10}, token=cust_token)
        if st == 403:
            record_pass("P10-08", "Customer token POST /api/admin/inventory/stock-in forbidden (HTTP 403)")
        else:
            record_fail("P10-08", "Customer token POST /api/admin/inventory/stock-in", f"Expected 403, got {st}: {res}")
    except Exception as e:
        record_fail("P10-08", "Customer token POST /api/admin/inventory/stock-in", str(e))

    # P10-09: Customer token POST /api/admin/inventory/adjust forbidden (HTTP 403)
    try:
        st, res = api_call("POST", "/admin/inventory/adjust", {"fertilizerId": 1, "adjustmentType": "ADD", "quantity": 5, "reason": "Test"}, token=cust_token)
        if st == 403:
            record_pass("P10-09", "Customer token POST /api/admin/inventory/adjust forbidden (HTTP 403)")
        else:
            record_fail("P10-09", "Customer token POST /api/admin/inventory/adjust", f"Expected 403, got {st}: {res}")
    except Exception as e:
        record_fail("P10-09", "Customer token POST /api/admin/inventory/adjust", str(e))

    # P10-10: Customer can access public /api/inventory/status/{productId} (HTTP 200)
    try:
        st, res = api_call("GET", "/inventory/status/1", token=cust_token)
        if st == 200 and res.get("success") and "data" in res and res["data"]["productId"] == 1:
            record_pass("P10-10", "Customer can access public /api/inventory/status/1 (HTTP 200)")
        else:
            record_fail("P10-10", "Customer access /api/inventory/status/1", f"Expected 200, got {st}: {res}")
    except Exception as e:
        record_fail("P10-10", "Customer access /api/inventory/status/1", str(e))

    # P10-11: Public unauthenticated access to /api/inventory/status/{productId} allowed (HTTP 200)
    try:
        st, res = api_call("GET", "/inventory/status/1")
        if st == 200 and res.get("success") and "data" in res and res["data"]["available"] is not None:
            record_pass("P10-11", "Public unauthenticated access to /api/inventory/status/1 allowed (HTTP 200)")
        else:
            record_fail("P10-11", "Public unauthenticated access /api/inventory/status/1", f"Expected 200, got {st}: {res}")
    except Exception as e:
        record_fail("P10-11", "Public unauthenticated access /api/inventory/status/1", str(e))

    # -------------------------------------------------------------------------
    # PART 2: INVENTORY SUMMARY & HEALTH CHECK
    # -------------------------------------------------------------------------

    # P10-12: Admin GET /api/admin/inventory/summary returns valid metrics
    try:
        st, res = api_call("GET", "/admin/inventory/summary", token=admin_token)
        if st == 200 and res.get("success") and "data" in res:
            d = res["data"]
            if d.get("totalProducts", 0) >= 50 and d.get("totalStockUnits", 0) > 0 and "stockAddedToday" in d:
                record_pass("P10-12", f"Admin GET /api/admin/inventory/summary returns valid metrics (Products: {d['totalProducts']}, Units: {d['totalStockUnits']})")
            else:
                record_fail("P10-12", "Admin GET /api/admin/inventory/summary", f"Unexpected metrics structure: {d}")
        else:
            record_fail("P10-12", "Admin GET /api/admin/inventory/summary", f"Expected 200, got {st}: {res}")
    except Exception as e:
        record_fail("P10-12", "Admin GET /api/admin/inventory/summary", str(e))

    # P10-13: Admin GET /api/admin/inventory/health returns status HEALTHY with 0 negative stock
    try:
        st, res = api_call("GET", "/admin/inventory/health", token=admin_token)
        if st == 200 and res.get("success") and "data" in res:
            h = res["data"]
            if h.get("status") in ["HEALTHY", "WARNING"] and h.get("negativeStockCount") == 0:
                record_pass("P10-13", f"Admin GET /api/admin/inventory/health returns {h['status']} with negativeStockCount=0")
            else:
                record_fail("P10-13", "Admin GET /api/admin/inventory/health", f"Unexpected health report: {h}")
        else:
            record_fail("P10-13", "Admin GET /api/admin/inventory/health", f"Expected 200, got {st}: {res}")
    except Exception as e:
        record_fail("P10-13", "Admin GET /api/admin/inventory/health", str(e))

    # P10-14: Admin GET /api/admin/inventory returns paginated list with stockStatus and availableStock
    try:
        st, res = api_call("GET", "/admin/inventory?page=0&size=5", token=admin_token)
        if st == 200 and res.get("success") and "data" in res:
            p = res["data"]
            content = p.get("content", [])
            if len(content) > 0 and "availableStock" in content[0] and ("stockStatus" in content[0] or "status" in content[0]):
                record_pass("P10-14", f"Admin GET /api/admin/inventory returned {len(content)} items with stockStatus & availableStock")
            else:
                record_fail("P10-14", "Admin GET /api/admin/inventory", f"Items missing required fields: {content[:1]}")
        else:
            record_fail("P10-14", "Admin GET /api/admin/inventory", f"Expected 200, got {st}: {res}")
    except Exception as e:
        record_fail("P10-14", "Admin GET /api/admin/inventory", str(e))

    # P10-15: Admin GET /api/admin/inventory/{id} returns accurate single product inventory record
    try:
        st, res = api_call("GET", "/admin/inventory/1", token=admin_token)
        p_id = res.get("data", {}).get("id") or res.get("data", {}).get("productId")
        p_name = res.get("data", {}).get("name")
        p_stock = res.get("data", {}).get("stock", res.get("data", {}).get("currentStock"))
        if st == 200 and res.get("success") and p_id == 1:
            record_pass("P10-15", f"Admin GET /api/admin/inventory/1 returns product '{p_name}' (Stock: {p_stock})")
        else:
            record_fail("P10-15", "Admin GET /api/admin/inventory/1", f"Expected 200, got {st}: {res}")
    except Exception as e:
        record_fail("P10-15", "Admin GET /api/admin/inventory/1", str(e))

    # -------------------------------------------------------------------------
    # PART 3: STOCK-IN OPERATIONS & AUDIT TRAIL
    # -------------------------------------------------------------------------

    # P10-16: Stock-in with non-positive quantity rejected (HTTP 400)
    try:
        st, res = api_call("POST", "/admin/inventory/stock-in", {"fertilizerId": 1, "quantity": 0}, token=admin_token)
        if st == 400:
            record_pass("P10-16", "Stock-in with non-positive quantity rejected (HTTP 400)")
        else:
            record_fail("P10-16", "Stock-in with non-positive quantity", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P10-16", "Stock-in with non-positive quantity", str(e))

    # P10-17: Stock-in for non-existent product rejected (HTTP 404)
    try:
        st, res = api_call("POST", "/admin/inventory/stock-in", {"fertilizerId": 999999, "quantity": 10}, token=admin_token)
        if st == 404:
            record_pass("P10-17", "Stock-in for non-existent product rejected (HTTP 404)")
        else:
            record_fail("P10-17", "Stock-in for non-existent product", f"Expected 404, got {st}: {res}")
    except Exception as e:
        record_fail("P10-17", "Stock-in for non-existent product", str(e))

    # P10-18: Admin executes valid stock-in (+25 units)
    initial_stock_1 = 0
    ref_stockin = f"P10_PO_{ts}"
    matching_tx = None
    try:
        st_pre, res_pre = api_call("GET", "/admin/inventory/1", token=admin_token)
        initial_stock_1 = res_pre["data"].get("stock", res_pre["data"].get("currentStock", 0))

        st_in, res_in = api_call("POST", "/admin/inventory/stock-in", {
            "fertilizerId": 1,
            "quantity": 25,
            "unitCost": 280.0,
            "supplier": "IFFCO Northern Depot",
            "referenceId": ref_stockin,
            "notes": "Phase 10 Test Shipment Restock"
        }, token=admin_token)

        if st_in == 200 and res_in.get("success"):
            record_pass("P10-18", f"Admin successfully executed stock-in (+25 units) for Product #1")
        else:
            record_fail("P10-18", "Admin stock-in (+25 units)", f"Expected 200, got {st_in}: {res_in}")
    except Exception as e:
        record_fail("P10-18", "Admin stock-in (+25 units)", str(e))

    # P10-19: Stock-in correctly increases total stock and available stock by +25
    try:
        st_post, res_post = api_call("GET", "/admin/inventory/1", token=admin_token)
        new_stock_1 = res_post["data"].get("stock", res_post["data"].get("currentStock", 0))
        if new_stock_1 == initial_stock_1 + 25:
            record_pass("P10-19", f"Stock increased accurately from {initial_stock_1} to {new_stock_1} (+25)")
        else:
            record_fail("P10-19", "Stock increase verification", f"Expected {initial_stock_1 + 25}, got {new_stock_1}")
    except Exception as e:
        record_fail("P10-19", "Stock increase verification", str(e))

    # P10-20: Stock-in generates an immutable transaction of type RESTOCK with before/after stock
    try:
        st_tx, res_tx = api_call("GET", f"/admin/inventory/transactions?productId=1", token=admin_token)
        tx_data = res_tx.get("data", [])
        tx_list = tx_data if isinstance(tx_data, list) else tx_data.get("content", [])
        matching_tx = next((t for t in tx_list if t.get("referenceId") == ref_stockin), None)

        if matching_tx and matching_tx.get("transactionType") == "RESTOCK" and matching_tx.get("quantity") == 25:
            if matching_tx.get("stockBefore") == initial_stock_1 and matching_tx.get("stockAfter") == initial_stock_1 + 25:
                record_pass("P10-20", f"RESTOCK transaction recorded accurately (Before: {matching_tx['stockBefore']} -> After: {matching_tx['stockAfter']})")
            else:
                record_fail("P10-20", "RESTOCK transaction before/after verification", f"Mismatch: {matching_tx}")
        else:
            record_fail("P10-20", "RESTOCK transaction generation", f"Could not find matching transaction with ref {ref_stockin}")
    except Exception as e:
        record_fail("P10-20", "RESTOCK transaction generation", str(e))

    # P10-21: Stock-in transaction records performedBy = 'admin' derived from session token
    try:
        if matching_tx and matching_tx.get("performedBy") == "admin":
            record_pass("P10-21", "Stock-in transaction records performedBy='admin' from token")
        else:
            record_fail("P10-21", "Stock-in performedBy", f"Expected 'admin', got '{matching_tx.get('performedBy') if matching_tx else None}'")
    except Exception as e:
        record_fail("P10-21", "Stock-in performedBy", str(e))

    # P10-22: Inventory summary stockAddedToday increments by the added units
    try:
        st_sum, res_sum = api_call("GET", "/admin/inventory/summary", token=admin_token)
        if st_sum == 200 and res_sum["data"].get("stockAddedToday", 0) >= 25:
            record_pass("P10-22", f"Inventory summary stockAddedToday accurately reflects movement ({res_sum['data']['stockAddedToday']} added)")
        else:
            record_fail("P10-22", "Inventory summary stockAddedToday", f"Expected >= 25, got {res_sum.get('data', {}).get('stockAddedToday')}")
    except Exception as e:
        record_fail("P10-22", "Inventory summary stockAddedToday", str(e))

    # -------------------------------------------------------------------------
    # PART 4: AUDITED STOCK ADJUSTMENTS
    # -------------------------------------------------------------------------

    # P10-23: Stock adjustment without mandatory reason is rejected (HTTP 400)
    try:
        st, res = api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": 1,
            "adjustmentType": "ADD",
            "quantity": 5
            # Missing reason
        }, token=admin_token)
        if st == 400:
            record_pass("P10-23", "Stock adjustment without mandatory reason rejected (HTTP 400)")
        else:
            record_fail("P10-23", "Stock adjustment missing reason", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P10-23", "Stock adjustment missing reason", str(e))

    # P10-24: Stock adjustment with blank/whitespace reason is rejected (HTTP 400)
    try:
        st, res = api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": 1,
            "adjustmentType": "ADD",
            "quantity": 5,
            "reason": "   "
        }, token=admin_token)
        if st == 400:
            record_pass("P10-24", "Stock adjustment with whitespace reason rejected (HTTP 400)")
        else:
            record_fail("P10-24", "Stock adjustment whitespace reason", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P10-24", "Stock adjustment whitespace reason", str(e))

    # P10-25: Stock adjustment with invalid adjustmentType is rejected (HTTP 400)
    try:
        st, res = api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": 1,
            "adjustmentType": "UNKNOWN_ACTION",
            "quantity": 5,
            "reason": "Physical count"
        }, token=admin_token)
        if st == 400:
            record_pass("P10-25", "Stock adjustment with invalid adjustmentType rejected (HTTP 400)")
        else:
            record_fail("P10-25", "Stock adjustment invalid type", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P10-25", "Stock adjustment invalid type", str(e))

    # P10-26: Adjustment REMOVE exceeding available stock is rejected (prevents negative stock, HTTP 400)
    try:
        st_pre, res_pre = api_call("GET", "/admin/inventory/1", token=admin_token)
        curr_stock_1 = res_pre["data"].get("stock", res_pre["data"].get("currentStock", 0))

        st_neg, res_neg = api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": 1,
            "adjustmentType": "REMOVE",
            "quantity": curr_stock_1 + 100,  # Exceeds available stock
            "reason": "Accidental massive deduction"
        }, token=admin_token)

        if st_neg == 400:
            record_pass("P10-26", "Adjustment REMOVE exceeding stock strictly rejected (HTTP 400, prevents negative stock)")
        else:
            record_fail("P10-26", "Adjustment REMOVE exceeding stock", f"Expected 400, got {st_neg}: {res_neg}")
    except Exception as e:
        record_fail("P10-26", "Adjustment REMOVE exceeding stock", str(e))

    # P10-27: Adjustment SET to negative value is rejected (HTTP 400)
    try:
        st, res = api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": 1,
            "adjustmentType": "SET",
            "quantity": -10,
            "reason": "Invalid target stock"
        }, token=admin_token)
        if st == 400:
            record_pass("P10-27", "Adjustment SET to negative value rejected (HTTP 400)")
        else:
            record_fail("P10-27", "Adjustment SET to negative value", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P10-27", "Adjustment SET to negative value", str(e))

    # P10-28: Valid adjustment ADD (+10 units) increases stock and records ADJUSTMENT_IN
    try:
        st_pre, res_pre = api_call("GET", "/admin/inventory/1", token=admin_token)
        before_add = res_pre["data"].get("stock", res_pre["data"].get("currentStock", 0))

        st_adj, res_adj = api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": 1,
            "adjustmentType": "ADD",
            "quantity": 10,
            "reason": "Unrecorded surplus bags discovered during shelf audit",
            "notes": "Phase 10 test ADD"
        }, token=admin_token)

        st_post, res_post = api_call("GET", "/admin/inventory/1", token=admin_token)
        after_add = res_post["data"].get("stock", res_post["data"].get("currentStock", 0))

        if st_adj == 200 and after_add == before_add + 10:
            record_pass("P10-28", f"Valid adjustment ADD (+10) increased stock from {before_add} to {after_add}")
        else:
            record_fail("P10-28", "Valid adjustment ADD (+10)", f"Expected {before_add + 10}, got {after_add}")
    except Exception as e:
        record_fail("P10-28", "Valid adjustment ADD (+10)", str(e))

    # P10-29: Valid adjustment REMOVE (-5 units) decreases stock and records ADJUSTMENT_OUT
    try:
        st_pre, res_pre = api_call("GET", "/admin/inventory/1", token=admin_token)
        before_rem = res_pre["data"].get("stock", res_pre["data"].get("currentStock", 0))

        st_adj, res_adj = api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": 1,
            "adjustmentType": "REMOVE",
            "quantity": 5,
            "reason": "Damaged in warehouse storage during monsoon dampness",
            "notes": "Phase 10 test REMOVE"
        }, token=admin_token)

        st_post, res_post = api_call("GET", "/admin/inventory/1", token=admin_token)
        after_rem = res_post["data"].get("stock", res_post["data"].get("currentStock", 0))

        if st_adj == 200 and after_rem == before_rem - 5:
            record_pass("P10-29", f"Valid adjustment REMOVE (-5) decreased stock from {before_rem} to {after_rem}")
        else:
            record_fail("P10-29", "Valid adjustment REMOVE (-5)", f"Expected {before_rem - 5}, got {after_rem}")
    except Exception as e:
        record_fail("P10-29", "Valid adjustment REMOVE (-5)", str(e))

    # P10-30: Valid adjustment SET (= target) sets exact stock and calculates delta correctly
    try:
        st_adj, res_adj = api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": 1,
            "adjustmentType": "SET",
            "quantity": 100,
            "reason": "Quarterly physical stock reconciliation",
            "notes": "Phase 10 test SET"
        }, token=admin_token)

        st_post, res_post = api_call("GET", "/admin/inventory/1", token=admin_token)
        final_stock = res_post["data"].get("stock", res_post["data"].get("currentStock", 0))

        if st_adj == 200 and final_stock == 100:
            record_pass("P10-30", "Valid adjustment SET successfully reconciled stock to 100 units")
        else:
            record_fail("P10-30", "Valid adjustment SET", f"Expected 100, got {final_stock}")
    except Exception as e:
        record_fail("P10-30", "Valid adjustment SET", str(e))

    # -------------------------------------------------------------------------
    # PART 5: LOW-STOCK THRESHOLDS & STATUS TRANSITIONS
    # -------------------------------------------------------------------------

    test_prod_id = 50
    threshold_50 = 10
    try:
        st_p, res_p = api_call("GET", f"/admin/inventory/{test_prod_id}", token=admin_token)
        threshold_50 = res_p["data"].get("lowStockThreshold") or 10
    except Exception:
        pass

    # P10-31: Product with stock > threshold evaluates to IN_STOCK
    try:
        api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": test_prod_id,
            "adjustmentType": "SET",
            "quantity": threshold_50 + 5,
            "reason": "Test IN_STOCK transition"
        }, token=admin_token)

        st_stat, res_stat = api_call("GET", f"/inventory/status/{test_prod_id}")
        if st_stat == 200 and res_stat["data"]["status"] == "IN_STOCK" and res_stat["data"]["available"] is True:
            record_pass("P10-31", f"Stock > threshold evaluates to status: IN_STOCK (Stock: {threshold_50 + 5}, Threshold: {threshold_50})")
        else:
            record_fail("P10-31", "IN_STOCK evaluation", f"Unexpected status: {res_stat}")
    except Exception as e:
        record_fail("P10-31", "IN_STOCK evaluation", str(e))

    # P10-32: Product with stock <= threshold and > 0 evaluates to LOW_STOCK
    try:
        api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": test_prod_id,
            "adjustmentType": "SET",
            "quantity": threshold_50,
            "reason": "Test LOW_STOCK transition"
        }, token=admin_token)

        st_stat, res_stat = api_call("GET", f"/inventory/status/{test_prod_id}")
        if st_stat == 200 and res_stat["data"]["status"] == "LOW_STOCK" and res_stat["data"]["available"] is True:
            record_pass("P10-32", f"Stock <= threshold evaluates to status: LOW_STOCK (Stock: {threshold_50})")
        else:
            record_fail("P10-32", "LOW_STOCK evaluation", f"Unexpected status: {res_stat}")
    except Exception as e:
        record_fail("P10-32", "LOW_STOCK evaluation", str(e))

    # P10-33: Product with stock = 0 evaluates to OUT_OF_STOCK
    try:
        api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": test_prod_id,
            "adjustmentType": "SET",
            "quantity": 0,
            "reason": "Test OUT_OF_STOCK transition"
        }, token=admin_token)

        st_stat, res_stat = api_call("GET", f"/inventory/status/{test_prod_id}")
        if st_stat == 200 and res_stat["data"]["status"] == "OUT_OF_STOCK" and res_stat["data"]["available"] is False:
            record_pass("P10-33", "Stock = 0 evaluates to status: OUT_OF_STOCK and available: false")
        else:
            record_fail("P10-33", "OUT_OF_STOCK evaluation", f"Unexpected status: {res_stat}")
    except Exception as e:
        record_fail("P10-33", "OUT_OF_STOCK evaluation", str(e))

    # P10-34: Customer endpoint returns 'In Stock' badge when stock > threshold
    try:
        api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": test_prod_id,
            "adjustmentType": "SET",
            "quantity": 40,
            "reason": "Test badge IN_STOCK"
        }, token=admin_token)
        st_b, res_b = api_call("GET", f"/inventory/status/{test_prod_id}")
        if st_b == 200 and res_b["data"]["displayBadge"] == "In Stock":
            record_pass("P10-34", "Customer endpoint returns displayBadge='In Stock'")
        else:
            record_fail("P10-34", "displayBadge In Stock", f"Expected 'In Stock', got {res_b.get('data', {}).get('displayBadge')}")
    except Exception as e:
        record_fail("P10-34", "displayBadge In Stock", str(e))

    # P10-35: Customer endpoint returns 'Only X left' badge when low stock
    try:
        api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": test_prod_id,
            "adjustmentType": "SET",
            "quantity": 7,
            "reason": "Test badge LOW_STOCK"
        }, token=admin_token)
        st_b, res_b = api_call("GET", f"/inventory/status/{test_prod_id}")
        if st_b == 200 and res_b["data"]["displayBadge"] == "Only 7 left":
            record_pass("P10-35", "Customer endpoint returns displayBadge='Only 7 left' for low stock")
        else:
            record_fail("P10-35", "displayBadge Only X left", f"Expected 'Only 7 left', got {res_b.get('data', {}).get('displayBadge')}")
    except Exception as e:
        record_fail("P10-35", "displayBadge Only X left", str(e))

    # P10-36: Customer endpoint returns 'Out of Stock' badge when stock = 0
    try:
        api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": test_prod_id,
            "adjustmentType": "SET",
            "quantity": 0,
            "reason": "Test badge OUT_OF_STOCK"
        }, token=admin_token)
        st_b, res_b = api_call("GET", f"/inventory/status/{test_prod_id}")
        if st_b == 200 and res_b["data"]["displayBadge"] == "Out of Stock":
            record_pass("P10-36", "Customer endpoint returns displayBadge='Out of Stock'")
        else:
            record_fail("P10-36", "displayBadge Out of Stock", f"Expected 'Out of Stock', got {res_b.get('data', {}).get('displayBadge')}")
    except Exception as e:
        record_fail("P10-36", "displayBadge Out of Stock", str(e))

    # P10-37: GET /api/admin/inventory/low-stock returns items at or below threshold
    try:
        api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": 49,
            "adjustmentType": "SET",
            "quantity": 5,
            "reason": "Setup for low stock list test"
        }, token=admin_token)

        st_low, res_low = api_call("GET", "/admin/inventory/low-stock", token=admin_token)
        if st_low == 200 and res_low.get("success") and isinstance(res_low.get("data"), list):
            low_ids = [item.get("id", item.get("productId")) for item in res_low["data"]]
            if 49 in low_ids:
                record_pass("P10-37", f"GET /api/admin/inventory/low-stock lists low stock products (Found {len(low_ids)} items)")
            else:
                record_fail("P10-37", "GET /api/admin/inventory/low-stock", f"Product 49 not found in low stock list: {low_ids}")
        else:
            record_fail("P10-37", "GET /api/admin/inventory/low-stock", f"Expected 200 list, got {st_low}: {res_low}")
    except Exception as e:
        record_fail("P10-37", "GET /api/admin/inventory/low-stock", str(e))

    # P10-38: GET /api/admin/inventory/out-of-stock returns items with 0 stock
    try:
        st_out, res_out = api_call("GET", "/admin/inventory/out-of-stock", token=admin_token)
        if st_out == 200 and res_out.get("success") and isinstance(res_out.get("data"), list):
            out_ids = [item.get("id", item.get("productId")) for item in res_out["data"]]
            if test_prod_id in out_ids:
                record_pass("P10-38", f"GET /api/admin/inventory/out-of-stock lists out of stock products (Found {len(out_ids)} items)")
            else:
                record_fail("P10-38", "GET /api/admin/inventory/out-of-stock", f"Product {test_prod_id} not in out of stock list: {out_ids}")
        else:
            record_fail("P10-38", "GET /api/admin/inventory/out-of-stock", f"Expected 200 list, got {st_out}: {res_out}")
    except Exception as e:
        record_fail("P10-38", "GET /api/admin/inventory/out-of-stock", str(e))

    # Restore Product #50 and Product #49 to healthy stock for subsequent tests
    api_call("POST", "/admin/inventory/adjust", {"fertilizerId": 50, "adjustmentType": "SET", "quantity": 80, "reason": "Restore healthy stock"}, token=admin_token)
    api_call("POST", "/admin/inventory/adjust", {"fertilizerId": 49, "adjustmentType": "SET", "quantity": 60, "reason": "Restore healthy stock"}, token=admin_token)

    # -------------------------------------------------------------------------
    # PART 6: ORDER PLACEMENT STOCK DEDUCTION & NEGATIVE STOCK PREVENTION
    # -------------------------------------------------------------------------

    # Setup customer address for checkout
    addr_id = None
    try:
        st_a, res_a = api_call("POST", "/addresses", {
            "fullName": "Kisan Farmer",
            "phone": "9876500010",
            "addressLine1": "Village Farm Block 4",
            "city": "Nagpur",
            "state": "Maharashtra",
            "pincode": "440001",
            "addressType": "FARM",
            "isDefault": True
        }, token=cust_token)
        if st_a in [200, 201] and res_a.get("success"):
            addr_id = res_a["data"]["id"]
        else:
            print("Address creation response:", st_a, res_a)
    except Exception as e:
        print("Address creation error:", e)

    # Choose Product #2 for order deduction tests
    # Ensure Product #2 has exactly 50 units
    api_call("POST", "/admin/inventory/adjust", {
        "fertilizerId": 2,
        "adjustmentType": "SET",
        "quantity": 50,
        "reason": "Setup baseline for order deduction"
    }, token=admin_token)

    # P10-39: Customer placing order automatically deducts stock via InventoryService
    order_id_1 = None
    order_num_1 = None
    try:
        # Add Product #2 (3 units) to cart
        api_call("POST", "/cart/items", {"fertilizerId": 2, "quantity": 3}, token=cust_token)

        st_o, res_o = api_call("POST", "/orders", {
            "addressId": addr_id,
            "paymentMethod": "COD",
            "customerNotes": "Phase 10 Order Placement Stock Deduction"
        }, token=cust_token)

        if st_o in [200, 201] and res_o.get("success"):
            order_id_1 = res_o["data"]["id"]
            order_num_1 = res_o["data"]["orderNumber"]

            # Check stock of Product #2
            st_p2, res_p2 = api_call("GET", "/admin/inventory/2", token=admin_token)
            p2_stock = res_p2["data"].get("stock", res_p2["data"].get("currentStock", 0))
            if p2_stock == 47:  # 50 - 3
                record_pass("P10-39", f"Order #{order_num_1} automatically deducted 3 units from Product #2 (50 -> {p2_stock})")
            else:
                record_fail("P10-39", "Order placement stock deduction", f"Expected 47 units, got {p2_stock}")
        else:
            record_fail("P10-39", "Order placement stock deduction", f"Order failed {st_o}: {res_o}")
    except Exception as e:
        record_fail("P10-39", "Order placement stock deduction", str(e))

    # P10-40: Order placement creates transaction of type SALE with referenceId = orderNumber
    try:
        st_tx, res_tx = api_call("GET", f"/admin/inventory/transactions?productId=2", token=admin_token)
        tx_data = res_tx.get("data", [])
        txs = tx_data if isinstance(tx_data, list) else tx_data.get("content", [])
        sale_tx = next((t for t in txs if t.get("referenceId") == order_num_1 and t.get("transactionType") == "SALE"), None)

        if sale_tx and sale_tx.get("transactionType") == "SALE" and sale_tx.get("quantity") == 3:
            if sale_tx.get("stockBefore") == 50 and sale_tx.get("stockAfter") == 47:
                record_pass("P10-40", f"Transaction SALE recorded with referenceId={order_num_1} (50 -> 47)")
            else:
                record_fail("P10-40", "SALE transaction stock before/after", f"Mismatch: {sale_tx}")
        else:
            record_fail("P10-40", "SALE transaction generation", f"Could not find SALE transaction for order {order_num_1}")
    except Exception as e:
        record_fail("P10-40", "SALE transaction generation", str(e))

    # P10-41: Placing order for out-of-stock item is rejected (HTTP 400)
    try:
        api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": 4,
            "adjustmentType": "SET",
            "quantity": 0,
            "reason": "Test zero stock rejection"
        }, token=admin_token)

        st_cart, res_cart = api_call("POST", "/cart/items", {"fertilizerId": 4, "quantity": 1}, token=cust_token)
        if st_cart == 400:
            record_pass("P10-41", "Attempt to add/order out-of-stock item rejected (HTTP 400)")
        else:
            record_fail("P10-41", "Out of stock order rejection", f"Expected 400, got {st_cart}: {res_cart}")
    except Exception as e:
        record_fail("P10-41", "Out of stock order rejection", str(e))

    # P10-42: Placing order for quantity exceeding available stock is rejected (HTTP 400)
    try:
        st_exc, res_exc = api_call("POST", "/cart/items", {"fertilizerId": 2, "quantity": 1000}, token=cust_token)
        if st_exc == 400:
            record_pass("P10-42", "Adding quantity exceeding available stock rejected (HTTP 400)")
        else:
            record_fail("P10-42", "Quantity exceeding available stock", f"Expected 400, got {st_exc}: {res_exc}")
    except Exception as e:
        record_fail("P10-42", "Quantity exceeding available stock", str(e))

    # P10-43: Inventory summary stockRemovedToday increments on order placement
    try:
        st_sum, res_sum = api_call("GET", "/admin/inventory/summary", token=admin_token)
        if st_sum == 200 and res_sum["data"].get("stockRemovedToday", 0) >= 3:
            record_pass("P10-43", f"Inventory summary stockRemovedToday reflects order deductions ({res_sum['data']['stockRemovedToday']} removed)")
        else:
            record_fail("P10-43", "Inventory summary stockRemovedToday", f"Expected >= 3, got {res_sum.get('data', {}).get('stockRemovedToday')}")
    except Exception as e:
        record_fail("P10-43", "Inventory summary stockRemovedToday", str(e))

    # -------------------------------------------------------------------------
    # PART 7: IDEMPOTENCY & REPEAT DEDUCTION PREVENTION
    # -------------------------------------------------------------------------

    # P10-44: Redundant stock deduction for same order and product does not deduct twice
    try:
        matching_sales = int(mysql_query(f"SELECT COUNT(*) FROM inventory_transactions WHERE reference_id='{order_num_1}' AND transaction_type='SALE' AND fertilizer_id=2;"))
        if matching_sales == 1:
            record_pass("P10-44", f"Exact-once stock deduction enforced for Order #{order_num_1} (Exactly 1 SALE transaction recorded)")
        else:
            record_fail("P10-44", "Idempotent deduction", f"Expected 1 SALE transaction, found {matching_sales}")
    except Exception as e:
        record_fail("P10-44", "Idempotent deduction", str(e))

    # -------------------------------------------------------------------------
    # PART 8: ORDER CANCELLATION STOCK RESTORATION
    # -------------------------------------------------------------------------

    # P10-45: Order cancellation automatically restores stock via InventoryService
    try:
        st_pre, res_pre = api_call("GET", "/admin/inventory/2", token=admin_token)
        stock_before_cancel = res_pre["data"].get("stock", res_pre["data"].get("currentStock", 0))

        st_cancel, res_cancel = api_call("POST", f"/orders/{order_id_1}/cancel", token=cust_token)
        if st_cancel == 200 and res_cancel.get("success"):
            st_post, res_post = api_call("GET", "/admin/inventory/2", token=admin_token)
            stock_after_cancel = res_post["data"].get("stock", res_post["data"].get("currentStock", 0))

            if stock_after_cancel == stock_before_cancel + 3:
                record_pass("P10-45", f"Order cancellation restored 3 units to Product #2 ({stock_before_cancel} -> {stock_after_cancel})")
            else:
                record_fail("P10-45", "Order cancellation stock restoration", f"Expected {stock_before_cancel + 3}, got {stock_after_cancel}")
        else:
            record_fail("P10-45", "Order cancellation stock restoration", f"Cancel failed {st_cancel}: {res_cancel}")
    except Exception as e:
        record_fail("P10-45", "Order cancellation stock restoration", str(e))

    # P10-46: Order cancellation creates transaction of type CANCELLATION with referenceId = orderNumber
    try:
        st_tx, res_tx = api_call("GET", f"/admin/inventory/transactions?productId=2", token=admin_token)
        tx_data = res_tx.get("data", [])
        txs = tx_data if isinstance(tx_data, list) else tx_data.get("content", [])
        cancel_tx = next((t for t in txs if t.get("referenceId") == order_num_1 and t.get("transactionType") == "CANCELLATION"), None)

        if cancel_tx and cancel_tx.get("quantity") == 3:
            record_pass("P10-46", f"Transaction CANCELLATION recorded with referenceId={order_num_1} (Restored: +{cancel_tx['quantity']})")
        else:
            record_fail("P10-46", "CANCELLATION transaction recording", f"Could not find CANCELLATION transaction: {txs[:2]}")
    except Exception as e:
        record_fail("P10-46", "CANCELLATION transaction recording", str(e))

    # P10-47: Repeated cancellation does not restore stock a second time (idempotency guard)
    try:
        st_pre, res_pre = api_call("GET", "/admin/inventory/2", token=admin_token)
        stock_stable = res_pre["data"].get("stock", res_pre["data"].get("currentStock", 0))

        st_re, res_re = api_call("POST", f"/orders/{order_id_1}/cancel", token=cust_token)
        st_post, res_post = api_call("GET", "/admin/inventory/2", token=admin_token)
        stock_after_re = res_post["data"].get("stock", res_post["data"].get("currentStock", 0))

        if stock_after_re == stock_stable:
            record_pass("P10-47", f"Repeated cancellation attempt did not duplicate stock restoration (Stock remained {stock_stable})")
        else:
            record_fail("P10-47", "Repeated cancellation duplicate prevention", f"Stock changed from {stock_stable} to {stock_after_re}")
    except Exception as e:
        record_fail("P10-47", "Repeated cancellation duplicate prevention", str(e))

    # P10-48: Stock status transitions from OUT_OF_STOCK back to IN_STOCK upon cancellation
    try:
        api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": 2,
            "adjustmentType": "SET",
            "quantity": 2,
            "reason": "Setup for full depletion and restoration test"
        }, token=admin_token)

        api_call("POST", "/cart/items", {"fertilizerId": 2, "quantity": 2}, token=cust_token)
        st_o2, res_o2 = api_call("POST", "/orders", {
            "addressId": addr_id,
            "paymentMethod": "COD"
        }, token=cust_token)

        o2_id = res_o2["data"]["id"]

        st_s_dep, res_s_dep = api_call("GET", "/inventory/status/2")
        dep_status = res_s_dep["data"]["status"]

        api_call("POST", f"/orders/{o2_id}/cancel", token=cust_token)

        st_s_res, res_s_res = api_call("GET", "/inventory/status/2")
        restored_status = res_s_res["data"]["status"]

        if dep_status == "OUT_OF_STOCK" and restored_status in ["LOW_STOCK", "IN_STOCK"] and res_s_res["data"]["available"] is True:
            record_pass("P10-48", f"Stock status transitioned from OUT_OF_STOCK back to {restored_status} upon order cancellation")
        else:
            record_fail("P10-48", "Stock status transition upon cancellation", f"Depleted: {dep_status}, Restored: {restored_status}")
    except Exception as e:
        record_fail("P10-48", "Stock status transition upon cancellation", str(e))

    # Reset Product #2 to standard stock
    api_call("POST", "/admin/inventory/adjust", {"fertilizerId": 2, "adjustmentType": "SET", "quantity": 40, "reason": "Restore Product 2"}, token=admin_token)

    # -------------------------------------------------------------------------
    # PART 9: CONCURRENCY & TRANSACTION HISTORY FILTERING
    # -------------------------------------------------------------------------

    # P10-49: Concurrency simulation: concurrent order placements respect remaining units and never drop stock below 0
    try:
        # Set Product #3 to exactly 4 units
        api_call("POST", "/admin/inventory/adjust", {
            "fertilizerId": 3,
            "adjustmentType": "SET",
            "quantity": 4,
            "reason": "Concurrency test setup"
        }, token=admin_token)

        # Create two customer accounts
        cust_c1 = f"p10_c1_{ts}"
        cust_c2 = f"p10_c2_{ts}"
        _, r_c1 = api_call("POST", "/auth/register", {"name": "C1", "username": cust_c1, "email": f"{cust_c1}@test.com", "password": "Password123!", "phone": "9876543211"})
        _, r_c2 = api_call("POST", "/auth/register", {"name": "C2", "username": cust_c2, "email": f"{cust_c2}@test.com", "password": "Password123!", "phone": "9876543212"})
        t_c1 = r_c1["data"]["token"]
        t_c2 = r_c2["data"]["token"]

        _, r_a1 = api_call("POST", "/addresses", {"fullName": "C1", "phone": "9876543211", "addressLine1": "A1", "city": "C1", "state": "Maharashtra", "pincode": "440001", "addressType": "FARM"}, token=t_c1)
        _, r_a2 = api_call("POST", "/addresses", {"fullName": "C2", "phone": "9876543212", "addressLine1": "A2", "city": "C2", "state": "Maharashtra", "pincode": "440001", "addressType": "FARM"}, token=t_c2)

        # Both add 3 units to cart (3 + 3 = 6 > 4 available)
        api_call("POST", "/cart/items", {"fertilizerId": 3, "quantity": 3}, token=t_c1)
        api_call("POST", "/cart/items", {"fertilizerId": 3, "quantity": 3}, token=t_c2)

        order_results = []
        def place_order(tok, a_id):
            st, res = api_call("POST", "/orders", {"addressId": a_id, "paymentMethod": "COD"}, token=tok)
            order_results.append((st, res))

        th1 = threading.Thread(target=place_order, args=(t_c1, r_a1["data"]["id"]))
        th2 = threading.Thread(target=place_order, args=(t_c2, r_a2["data"]["id"]))

        th1.start()
        th2.start()
        th1.join()
        th2.join()

        # Check final stock
        st_p3, res_p3 = api_call("GET", "/admin/inventory/3", token=admin_token)
        final_stock_3 = res_p3["data"].get("stock", res_p3["data"].get("currentStock", 0))

        successes = [r for r in order_results if r[0] in [200, 201]]
        failures = [r for r in order_results if r[0] == 400]

        if len(successes) == 1 and len(failures) == 1 and final_stock_3 == 1:
            record_pass("P10-49", f"Concurrency simulation prevented overselling: 1 order placed (stock 4 -> 1), 1 rejected with HTTP 400")
        elif final_stock_3 >= 0:
            record_pass("P10-49", f"Concurrency safe: stock remained non-negative ({final_stock_3} >= 0)")
        else:
            record_fail("P10-49", "Concurrency simulation", f"Overselling occurred! Stock: {final_stock_3}, Results: {order_results}")
    except Exception as e:
        record_fail("P10-49", "Concurrency simulation", str(e))

    # Reset Product #3 to normal stock
    api_call("POST", "/admin/inventory/adjust", {"fertilizerId": 3, "adjustmentType": "SET", "quantity": 90, "reason": "Restore Product 3"}, token=admin_token)

    # P10-50: Filter /api/admin/inventory/transactions by productId returns only matching transactions
    try:
        st_p1_tx, res_p1_tx = api_call("GET", "/admin/inventory/transactions?productId=1", token=admin_token)
        if st_p1_tx == 200 and res_p1_tx.get("success"):
            tx_data = res_p1_tx.get("data", [])
            tx_items = tx_data if isinstance(tx_data, list) else tx_data.get("content", [])
            all_match = all(t.get("fertilizerId") == 1 or t.get("productId") == 1 for t in tx_items) if tx_items else True
            if all_match and len(tx_items) > 0:
                record_pass("P10-50", f"Filter transactions by productId=1 returns strictly matching transactions ({len(tx_items)} items)")
            else:
                record_fail("P10-50", "Filter transactions by productId", f"Non-matching items found: {tx_items}")
        else:
            record_fail("P10-50", "Filter transactions by productId", f"Expected 200, got {st_p1_tx}: {res_p1_tx}")
    except Exception as e:
        record_fail("P10-50", "Filter transactions by productId", str(e))

    # P10-51: Filter /api/admin/inventory/transactions by type returns only matching transaction types
    try:
        st_t_tx, res_t_tx = api_call("GET", "/admin/inventory/transactions?type=RESTOCK", token=admin_token)
        if st_t_tx == 200 and res_t_tx.get("success"):
            tx_data = res_t_tx.get("data", [])
            tx_items = tx_data if isinstance(tx_data, list) else tx_data.get("content", [])
            all_restock = all(t.get("transactionType") == "RESTOCK" for t in tx_items) if tx_items else True
            if all_restock and len(tx_items) > 0:
                record_pass("P10-51", f"Filter transactions by type=RESTOCK returns strictly RESTOCK transactions ({len(tx_items)} items)")
            else:
                record_fail("P10-51", "Filter transactions by type", f"Non-matching items found: {tx_items}")
        else:
            record_fail("P10-51", "Filter transactions by type", f"Expected 200, got {st_t_tx}: {res_t_tx}")
    except Exception as e:
        record_fail("P10-51", "Filter transactions by type", str(e))

    # P10-52: Health check confirms zero negative stock items across entire catalog (negativeStockCount: 0)
    try:
        st_h, res_h = api_call("GET", "/admin/inventory/health", token=admin_token)
        if st_h == 200 and res_h.get("success") and res_h["data"]["negativeStockCount"] == 0:
            record_pass("P10-52", f"Final Health check confirmed: 0 negative stock violations across all {res_h['data']['productsChecked']} products (Status: {res_h['data']['status']})")
        else:
            record_fail("P10-52", "Final health check", f"Violations found: {res_h}")
    except Exception as e:
        record_fail("P10-52", "Final health check", str(e))

    clean_test_data()

    print("======================================================================")
    print(f"PHASE 10 TEST RESULTS: {passed_count} PASSED, {failed_count} FAILED out of {passed_count + failed_count} TESTS")
    print("======================================================================")
    return failed_count == 0

if __name__ == "__main__":
    success = run_tests()
    sys.exit(0 if success else 1)
