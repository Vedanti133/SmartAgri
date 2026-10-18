import urllib.request
import urllib.parse
import urllib.error
import json
import subprocess
import sys
import time
from datetime import date, timedelta

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
        "DELETE FROM order_item_batches WHERE order_item_id IN (SELECT oi.id FROM order_items oi JOIN orders o ON oi.order_id=o.id JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p11_%');",
        "DELETE FROM invoices WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p11_%');",
        "DELETE FROM deliveries WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p11_%');",
        "DELETE FROM payments WHERE order_id IN (SELECT o.id FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p11_%');",
        "DELETE oi FROM order_items oi JOIN orders o ON oi.order_id=o.id JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p11_%';",
        "DELETE o FROM orders o JOIN users u ON o.user_id=u.id WHERE u.username LIKE 'p11_%';",
        "DELETE a FROM addresses a JOIN users u ON a.user_id=u.id WHERE u.username LIKE 'p11_%';",
        "DELETE cp FROM customer_profiles cp JOIN users u ON cp.user_id=u.id WHERE u.username LIKE 'p11_%';",
        "DELETE ci FROM cart_items ci JOIN cart c ON ci.cart_id=c.id JOIN users u ON c.user_id=u.id WHERE u.username LIKE 'p11_%';",
        "DELETE c FROM cart c JOIN users u ON c.user_id=u.id WHERE u.username LIKE 'p11_%';",
        "DELETE FROM inventory_transactions WHERE reference_id LIKE 'P11_%' OR reference_id LIKE 'BATCH-P11%' OR performed_by LIKE 'p11_%';",
        "DELETE FROM fertilizer_batches WHERE batch_number LIKE 'P11-%';",
        "DELETE FROM users WHERE username LIKE 'p11_%';"
    ]
    for q in queries:
        try:
            mysql_query(q)
        except Exception:
            pass

def run_tests():
    print("======================================================================")
    print("SMARTAGRI PHASE 11 AUTOMATED TEST SUITE")
    print("Batch/Lot Tracking, Expiry Management & FEFO Inventory")
    print("======================================================================")

    clean_test_data()

    # --- 1. Authentication & Security ---
    status, res = api_call("GET", "/admin/inventory/batches")
    if status == 401:
        record_pass("P11-01", "Unauthenticated GET /api/admin/inventory/batches rejected (HTTP 401)")
    else:
        record_fail("P11-01", "Unauthenticated GET /api/admin/inventory/batches rejected", f"Expected 401, got {status}")

    status, res = api_call("GET", "/admin/inventory/batches/summary")
    if status == 401:
        record_pass("P11-02", "Unauthenticated GET /api/admin/inventory/batches/summary rejected (HTTP 401)")
    else:
        record_fail("P11-02", "Unauthenticated GET /api/admin/inventory/batches/summary rejected", f"Expected 401, got {status}")

    status, res = api_call("POST", "/admin/inventory/batches", {"batchNumber": "TEST"})
    if status == 401:
        record_pass("P11-03", "Unauthenticated POST /api/admin/inventory/batches rejected (HTTP 401)")
    else:
        record_fail("P11-03", "Unauthenticated POST /api/admin/inventory/batches rejected", f"Expected 401, got {status}")

    status, res = api_call("POST", "/admin/inventory/batches/1/block", {"reason": "Test"})
    if status == 401:
        record_pass("P11-04", "Unauthenticated POST /api/admin/inventory/batches/1/block rejected (HTTP 401)")
    else:
        record_fail("P11-04", "Unauthenticated POST /api/admin/inventory/batches/1/block rejected", f"Expected 401, got {status}")

    status, res = api_call("POST", "/admin/inventory/batches/1/unblock", {})
    if status == 401:
        record_pass("P11-05", "Unauthenticated POST /api/admin/inventory/batches/1/unblock rejected (HTTP 401)")
    else:
        record_fail("P11-05", "Unauthenticated POST /api/admin/inventory/batches/1/unblock rejected", f"Expected 401, got {status}")

    status, res = api_call("POST", "/admin/inventory/batches/1/adjust", {"adjustmentType": "ADD", "quantity": 5, "reason": "Test"})
    if status == 401:
        record_pass("P11-06", "Unauthenticated POST /api/admin/inventory/batches/1/adjust rejected (HTTP 401)")
    else:
        record_fail("P11-06", "Unauthenticated POST /api/admin/inventory/batches/1/adjust rejected", f"Expected 401, got {status}")

    status, res = api_call("POST", "/admin/inventory/batches/1/damaged", {"quantity": 2, "reason": "Test"})
    if status == 401:
        record_pass("P11-07", "Unauthenticated POST /api/admin/inventory/batches/1/damaged rejected (HTTP 401)")
    else:
        record_fail("P11-07", "Unauthenticated POST /api/admin/inventory/batches/1/damaged rejected", f"Expected 401, got {status}")

    # Register customer and login admin
    cust_ts = int(time.time() * 1000) % 100000
    cust_username = f"p11_cust_{cust_ts}"
    status, reg_res = api_call("POST", "/auth/register", {
        "username": cust_username,
        "password": "Password123!",
        "email": f"{cust_username}@example.com",
        "fullName": "Batch Test Customer",
        "phone": "9876543210"
    })
    cust_token = reg_res.get("data", {}).get("token")

    status, admin_res = api_call("POST", "/auth/login", {
        "username": "admin",
        "password": "adminpassword123"
    })
    admin_token = admin_res.get("data", {}).get("token")

    # Customer token role check
    status, res = api_call("GET", "/admin/inventory/batches", token=cust_token)
    if status == 403:
        record_pass("P11-08", "Customer token GET /api/admin/inventory/batches rejected with HTTP 403")
    else:
        record_fail("P11-08", "Customer token GET /api/admin/inventory/batches rejected", f"Expected 403, got {status}")

    status, res = api_call("POST", "/admin/inventory/batches", {"batchNumber": "TEST"}, token=cust_token)
    if status == 403:
        record_pass("P11-09", "Customer token POST /api/admin/inventory/batches rejected with HTTP 403")
    else:
        record_fail("P11-09", "Customer token POST /api/admin/inventory/batches rejected", f"Expected 403, got {status}")

    # --- 2. Batch Creation & Validations ---
    today = date.today()
    exp_future = (today + timedelta(days=180)).isoformat()
    exp_near = (today + timedelta(days=20)).isoformat()
    exp_past = (today - timedelta(days=10)).isoformat()
    mfg_date = (today - timedelta(days=30)).isoformat()
    mfg_invalid = (today + timedelta(days=200)).isoformat()

    # P11-10: Non-existent product
    status, res = api_call("POST", "/admin/inventory/batches", {
        "fertilizerId": 999999,
        "batchNumber": "P11-BAD-01",
        "quantity": 10,
        "expiryDate": exp_future
    }, token=admin_token)
    if status == 404:
        record_pass("P11-10", "Batch creation with non-existent product rejected (HTTP 404)")
    else:
        record_fail("P11-10", "Batch creation with non-existent product rejected", f"Expected 404, got {status}")

    # P11-11: Empty batch number
    status, res = api_call("POST", "/admin/inventory/batches", {
        "fertilizerId": 1,
        "batchNumber": "   ",
        "quantity": 10,
        "expiryDate": exp_future
    }, token=admin_token)
    if status == 400:
        record_pass("P11-11", "Batch creation with blank batch number rejected (HTTP 400)")
    else:
        record_fail("P11-11", "Batch creation with blank batch number rejected", f"Expected 400, got {status}")

    # P11-12: Non-positive quantity
    status, res = api_call("POST", "/admin/inventory/batches", {
        "fertilizerId": 1,
        "batchNumber": "P11-QTY-0",
        "quantity": 0,
        "expiryDate": exp_future
    }, token=admin_token)
    if status == 400:
        record_pass("P11-12", "Batch creation with zero quantity rejected (HTTP 400)")
    else:
        record_fail("P11-12", "Batch creation with zero quantity rejected", f"Expected 400, got {status}")

    # P11-13: Missing expiry date
    status, res = api_call("POST", "/admin/inventory/batches", {
        "fertilizerId": 1,
        "batchNumber": "P11-NO-EXP",
        "quantity": 10
    }, token=admin_token)
    if status == 400:
        record_pass("P11-13", "Batch creation with missing expiry date rejected (HTTP 400)")
    else:
        record_fail("P11-13", "Batch creation with missing expiry date rejected", f"Expected 400, got {status}")

    # P11-14: Manufacturing date after expiry date
    status, res = api_call("POST", "/admin/inventory/batches", {
        "fertilizerId": 1,
        "batchNumber": "P11-BAD-DATES",
        "quantity": 10,
        "manufacturingDate": mfg_invalid,
        "expiryDate": exp_future
    }, token=admin_token)
    if status == 400:
        record_pass("P11-14", "Batch creation with mfg date after expiry date rejected (HTTP 400)")
    else:
        record_fail("P11-14", "Batch creation with mfg date after expiry date rejected", f"Expected 400, got {status}")

    # Create dedicated test fertilizers for Phase 11 batches to isolate from seeded catalog
    prod_sku_main = f"P11-MAIN-{cust_ts}"
    mysql_query(f"INSERT INTO fertilizer (name, sku, brand, selling_price, stock, minimum_stock, low_stock_threshold, status, active) VALUES ('P11 Main Fertilizer', '{prod_sku_main}', 'Kisan Agri', 350.00, 0, 10, 10, 'ACTIVE', 1);")
    p11_prod_main = int(mysql_query(f"SELECT id FROM fertilizer WHERE sku='{prod_sku_main}';"))

    prod_sku_sec = f"P11-SEC-{cust_ts}"
    mysql_query(f"INSERT INTO fertilizer (name, sku, brand, selling_price, stock, minimum_stock, low_stock_threshold, status, active) VALUES ('P11 Sec Fertilizer', '{prod_sku_sec}', 'Kisan Agri', 400.00, 0, 10, 10, 'ACTIVE', 1);")
    p11_prod_sec = int(mysql_query(f"SELECT id FROM fertilizer WHERE sku='{prod_sku_sec}';"))

    # P11-15: Valid batch creation
    test_batch_1 = f"P11-BN-{cust_ts}-01"
    status, b1_res = api_call("POST", "/admin/inventory/batches", {
        "fertilizerId": p11_prod_main,
        "batchNumber": test_batch_1,
        "quantity": 50,
        "manufacturingDate": mfg_date,
        "expiryDate": exp_future,
        "supplierName": "IFFCO Manufacturing Unit",
        "purchaseOrderNumber": f"P11_PO_{cust_ts}",
        "notes": "Premium grade urea batch"
    }, token=admin_token)
    if status == 201 and b1_res.get("success"):
        b1_data = b1_res.get("data", {})
        batch_id_1 = b1_data.get("id")
        record_pass("P11-15", f"Admin created Batch #{test_batch_1} (ID: {batch_id_1}, Qty: 50, Status: ACTIVE)")
    else:
        record_fail("P11-15", "Valid batch creation", f"Expected 201, got {status}: {b1_res}")
        batch_id_1 = None

    # P11-16: Duplicate batch number for same product rejected with HTTP 409
    status, dup_res = api_call("POST", "/admin/inventory/batches", {
        "fertilizerId": p11_prod_main,
        "batchNumber": test_batch_1,
        "quantity": 25,
        "expiryDate": exp_future
    }, token=admin_token)
    if status == 409:
        record_pass("P11-16", f"Duplicate batch number '{test_batch_1}' for same product rejected with HTTP 409 Conflict")
    else:
        record_fail("P11-16", "Duplicate batch number rejected with 409", f"Expected 409, got {status}")

    # P11-17: Duplicate batch number for different product is permitted
    status, diff_prod_res = api_call("POST", "/admin/inventory/batches", {
        "fertilizerId": p11_prod_sec,
        "batchNumber": test_batch_1,
        "quantity": 30,
        "expiryDate": exp_future
    }, token=admin_token)
    if status == 201:
        record_pass("P11-17", f"Same batch number '{test_batch_1}' permitted for distinct product (HTTP 201)")
    else:
        record_fail("P11-17", "Duplicate batch number for different product", f"Expected 201, got {status}")

    # P11-18: Verify audit transaction created for batch intake
    status, tx_res = api_call("GET", f"/admin/inventory/batches/{batch_id_1}/history", token=admin_token)
    if status == 200 and tx_res.get("success") and len(tx_res.get("data", [])) > 0:
        first_tx = tx_res["data"][0]
        if first_tx.get("transactionType") == "RESTOCK" and first_tx.get("quantity") == 50:
            record_pass("P11-18", "RESTOCK transaction recorded with batch_id and quantity=50")
        else:
            record_fail("P11-18", "Audit transaction fields", f"Unexpected tx: {first_tx}")
    else:
        record_fail("P11-18", "Audit transaction history query", f"Got status {status}: {tx_res}")

    # --- 3. Dynamic Status Rules ---
    # P11-19: Create near-expiry batch
    test_batch_near = f"P11-NEAR-{cust_ts}"
    status, bn_res = api_call("POST", "/admin/inventory/batches", {
        "fertilizerId": p11_prod_main,
        "batchNumber": test_batch_near,
        "quantity": 15,
        "expiryDate": exp_near
    }, token=admin_token)
    if status == 201 and bn_res.get("data", {}).get("status") == "NEAR_EXPIRY":
        record_pass("P11-19", f"Batch with expiry in 20 days evaluates to status: NEAR_EXPIRY")
        batch_id_near = bn_res["data"]["id"]
    else:
        record_fail("P11-19", "NEAR_EXPIRY status evaluation", f"Expected NEAR_EXPIRY, got {bn_res}")
        batch_id_near = None

    # P11-20: Create expired batch
    test_batch_exp = f"P11-EXP-{cust_ts}"
    status, be_res = api_call("POST", "/admin/inventory/batches", {
        "fertilizerId": p11_prod_main,
        "batchNumber": test_batch_exp,
        "quantity": 20,
        "expiryDate": exp_past
    }, token=admin_token)
    if status == 201 and be_res.get("data", {}).get("status") == "EXPIRED":
        record_pass("P11-20", f"Batch with past expiry date evaluates to status: EXPIRED")
        batch_id_exp = be_res["data"]["id"]
    else:
        record_fail("P11-20", "EXPIRED status evaluation", f"Expected EXPIRED, got {be_res}")
        batch_id_exp = None

    # P11-21: Depleted status (quantity = 0)
    test_batch_dep = f"P11-DEP-{cust_ts}"
    status, bd_res = api_call("POST", "/admin/inventory/batches", {
        "fertilizerId": p11_prod_main,
        "batchNumber": test_batch_dep,
        "quantity": 5,
        "expiryDate": exp_future
    }, token=admin_token)
    batch_id_dep = bd_res.get("data", {}).get("id")
    # Adjust stock to 0
    status, adj_res = api_call("POST", f"/admin/inventory/batches/{batch_id_dep}/adjust", {
        "adjustmentType": "SET",
        "quantity": 0,
        "reason": "Depleting batch for status test"
    }, token=admin_token)
    if status == 200 and adj_res.get("data", {}).get("status") == "DEPLETED":
        record_pass("P11-21", "Batch with current quantity 0 evaluates to status: DEPLETED")
    else:
        record_fail("P11-21", "DEPLETED status evaluation", f"Expected DEPLETED, got {adj_res}")

    # P11-22: Summary API returns accurate batch counts
    status, sum_res = api_call("GET", "/admin/inventory/batches/summary", token=admin_token)
    if status == 200 and sum_res.get("data", {}).get("totalBatches") >= 4:
        s_data = sum_res["data"]
        record_pass("P11-22", f"Summary reports: Total: {s_data.get('totalBatches')}, Active: {s_data.get('activeBatches')}, Near: {s_data.get('nearExpiryBatches')}, Expired: {s_data.get('expiredBatches')}")
    else:
        record_fail("P11-22", "Batch summary counts", f"Got status {status}: {sum_res}")

    # P11-23: GET /api/admin/inventory/batches/near-expiry
    status, ne_res = api_call("GET", "/admin/inventory/batches/near-expiry", token=admin_token)
    if status == 200 and len(ne_res.get("data", [])) > 0:
        record_pass("P11-23", f"GET /api/admin/inventory/batches/near-expiry returned {len(ne_res['data'])} near-expiry batches")
    else:
        record_fail("P11-23", "Near-expiry query", f"Got status {status}: {ne_res}")

    # P11-24: GET /api/admin/inventory/batches/expired
    status, ex_res = api_call("GET", "/admin/inventory/batches/expired", token=admin_token)
    if status == 200 and len(ex_res.get("data", [])) > 0:
        record_pass("P11-24", f"GET /api/admin/inventory/batches/expired returned {len(ex_res['data'])} expired batches")
    else:
        record_fail("P11-24", "Expired batches query", f"Got status {status}: {ex_res}")

    # P11-25: Single batch lookup GET /api/admin/inventory/batches/{id}
    status, get_res = api_call("GET", f"/admin/inventory/batches/{batch_id_1}", token=admin_token)
    if status == 200 and get_res.get("data", {}).get("batchNumber") == test_batch_1:
        record_pass("P11-25", f"GET /api/admin/inventory/batches/{batch_id_1} returned full metadata")
    else:
        record_fail("P11-25", "Batch lookup by ID", f"Got status {status}: {get_res}")

    # --- 4. FEFO Order Allocation ---
    # Setup dedicated test fertilizer product for clean FEFO allocation verification
    prod_sku = f"FEFO-{cust_ts}"
    mysql_query(f"INSERT INTO fertilizer (name, sku, brand, selling_price, stock, minimum_stock, low_stock_threshold, status, active) VALUES ('FEFO Test Urea', '{prod_sku}', 'Kisan Agri', 300.00, 0, 10, 10, 'ACTIVE', 1);")
    fefo_prod_id = int(mysql_query(f"SELECT id FROM fertilizer WHERE sku='{prod_sku}';"))

    # Create 3 batches with different expiry dates:
    # Batch A: expires in 10 days, qty 5
    # Batch B: expires in 30 days, qty 10
    # Batch C: expires in 60 days, qty 20
    bA_exp = (today + timedelta(days=10)).isoformat()
    bB_exp = (today + timedelta(days=30)).isoformat()
    bC_exp = (today + timedelta(days=60)).isoformat()

    status, bA = api_call("POST", "/admin/inventory/batches", {"fertilizerId": fefo_prod_id, "batchNumber": f"P11-A-{cust_ts}", "quantity": 5, "expiryDate": bA_exp}, token=admin_token)
    status, bB = api_call("POST", "/admin/inventory/batches", {"fertilizerId": fefo_prod_id, "batchNumber": f"P11-B-{cust_ts}", "quantity": 10, "expiryDate": bB_exp}, token=admin_token)
    status, bC = api_call("POST", "/admin/inventory/batches", {"fertilizerId": fefo_prod_id, "batchNumber": f"P11-C-{cust_ts}", "quantity": 20, "expiryDate": bC_exp}, token=admin_token)

    batch_A_id = bA["data"]["id"]
    batch_B_id = bB["data"]["id"]
    batch_C_id = bC["data"]["id"]

    # Verify initial stock = 35 (5 + 10 + 20)
    init_stock = int(mysql_query(f"SELECT stock FROM fertilizer WHERE id={fefo_prod_id};"))
    if init_stock == 35:
        record_pass("P11-26", f"Parent fertilizer stock synchronized to 35 units across 3 batches")
    else:
        record_fail("P11-26", "Initial stock sync", f"Expected 35, got {init_stock}")

    # Customer orders 12 units
    # Add address
    status, addr_res = api_call("POST", "/addresses", {
        "fullName": "Batch Farmer",
        "phone": "9811223344",
        "addressLine1": "Plot 102, Agri Gate",
        "city": "Nashik",
        "state": "Maharashtra",
        "district": "Nashik",
        "pincode": "422003",
        "addressType": "FARM",
        "isDefault": True
    }, token=cust_token)
    addr_id = addr_res.get("data", {}).get("id")

    # Add to cart: 12 units
    status, cart_res = api_call("POST", "/cart/items", {
        "fertilizerId": fefo_prod_id,
        "quantity": 12
    }, token=cust_token)

    # Checkout
    status, order_res = api_call("POST", "/orders", {
        "addressId": addr_id,
        "paymentMethod": "COD",
        "notes": "FEFO test order"
    }, token=cust_token)

    order_id = order_res.get("data", {}).get("id")
    order_number = order_res.get("data", {}).get("orderNumber")

    if status == 201 and order_id:
        record_pass("P11-27", f"Customer placed order #{order_number} for 12 units")
    else:
        record_fail("P11-27", "Order placement for 12 units", f"Expected 201, got {status}: {order_res}")

    # P11-28: Verify FEFO allocation consumed Batch A completely (5 units -> 0)
    status, bA_check = api_call("GET", f"/admin/inventory/batches/{batch_A_id}", token=admin_token)
    curr_A = bA_check.get("data", {}).get("currentQuantity")
    if curr_A == 0:
        record_pass("P11-28", f"FEFO allocated Batch A (earliest expiry) first: currentQuantity reduced 5 -> 0 (DEPLETED)")
    else:
        record_fail("P11-28", "Batch A FEFO deduction", f"Expected 0, got {curr_A}")

    # P11-29: Verify FEFO allocation consumed Batch B partially (7 units deducted: 10 -> 3)
    status, bB_check = api_call("GET", f"/admin/inventory/batches/{batch_B_id}", token=admin_token)
    curr_B = bB_check.get("data", {}).get("currentQuantity")
    if curr_B == 3:
        record_pass("P11-29", f"FEFO allocated remaining 7 units from Batch B: currentQuantity reduced 10 -> 3")
    else:
        record_fail("P11-29", "Batch B FEFO deduction", f"Expected 3, got {curr_B}")

    # P11-30: Verify Batch C was untouched (remains 20)
    status, bC_check = api_call("GET", f"/admin/inventory/batches/{batch_C_id}", token=admin_token)
    curr_C = bC_check.get("data", {}).get("currentQuantity")
    if curr_C == 20:
        record_pass("P11-30", f"Batch C untouched by FEFO allocation: currentQuantity remains 20")
    else:
        record_fail("P11-30", "Batch C untouched", f"Expected 20, got {curr_C}")

    # P11-31: Verify order_item_batches records exact allocations in MySQL
    oib_rows = mysql_query(f"SELECT batch_id, quantity FROM order_item_batches WHERE order_item_id IN (SELECT id FROM order_items WHERE order_id={order_id}) ORDER BY batch_id ASC;")
    # Expect 2 lines: batch_A_id 5, batch_B_id 7
    if str(batch_A_id) in oib_rows and str(batch_B_id) in oib_rows and "5" in oib_rows and "7" in oib_rows:
        record_pass("P11-31", f"order_item_batches records exact mapping: Batch A (5 units) and Batch B (7 units)")
    else:
        record_fail("P11-31", "order_item_batches rows", f"Expected allocations for A and B, got: {oib_rows}")

    # P11-32: Verify SALE audit transactions recorded with exact batch references
    status, a_tx = api_call("GET", f"/admin/inventory/batches/{batch_A_id}/history", token=admin_token)
    sale_txs = [t for t in a_tx.get("data", []) if t.get("transactionType") == "SALE"]
    if len(sale_txs) == 1 and sale_txs[0].get("quantity") == 5 and sale_txs[0].get("referenceId") == order_number:
        record_pass("P11-32", f"Batch A audit log recorded SALE transaction for {order_number} (-5 units)")
    else:
        record_fail("P11-32", "Batch A SALE audit", f"Expected 1 SALE for 5 units, got {sale_txs}")

    # P11-33: Verify parent fertilizer stock deducted accurately: 35 -> 23
    new_fert_stock = int(mysql_query(f"SELECT stock FROM fertilizer WHERE id={fefo_prod_id};"))
    if new_fert_stock == 23:
        record_pass("P11-33", "Parent fertilizer stock updated from 35 -> 23 (-12 units)")
    else:
        record_fail("P11-33", "Parent stock deduction", f"Expected 23, got {new_fert_stock}")

    # --- 5. Order Cancellation & Exact Batch Restoration ---
    status, cancel_res = api_call("PUT", f"/orders/{order_id}/cancel", token=cust_token)
    if status == 200 and cancel_res.get("success"):
        record_pass("P11-34", f"Customer cancelled order #{order_number}")
    else:
        record_fail("P11-34", "Order cancellation", f"Expected 200, got {status}: {cancel_res}")

    # P11-35: Batch A restored by exactly 5 (0 -> 5)
    status, bA_rest = api_call("GET", f"/admin/inventory/batches/{batch_A_id}", token=admin_token)
    rest_A = bA_rest.get("data", {}).get("currentQuantity")
    if rest_A == 5:
        record_pass("P11-35", f"Cancellation restored exactly 5 units to Batch A (0 -> 5)")
    else:
        record_fail("P11-35", "Batch A restoration", f"Expected 5, got {rest_A}")

    # P11-36: Batch B restored by exactly 7 (3 -> 10)
    status, bB_rest = api_call("GET", f"/admin/inventory/batches/{batch_B_id}", token=admin_token)
    rest_B = bB_rest.get("data", {}).get("currentQuantity")
    if rest_B == 10:
        record_pass("P11-36", f"Cancellation restored exactly 7 units to Batch B (3 -> 10)")
    else:
        record_fail("P11-36", "Batch B restoration", f"Expected 10, got {rest_B}")

    # P11-37: Parent fertilizer stock restored to 35
    rest_fert_stock = int(mysql_query(f"SELECT stock FROM fertilizer WHERE id={fefo_prod_id};"))
    if rest_fert_stock == 35:
        record_pass("P11-37", "Parent fertilizer stock accurately restored to 35 units")
    else:
        record_fail("P11-37", "Parent stock restore", f"Expected 35, got {rest_fert_stock}")

    # P11-38: Idempotency of cancellation restoration
    status, cancel_dup = api_call("PUT", f"/orders/{order_id}/cancel", token=cust_token)
    # Repeated cancel is rejected or ignored without changing stock
    post_dup_stock = int(mysql_query(f"SELECT stock FROM fertilizer WHERE id={fefo_prod_id};"))
    if post_dup_stock == 35:
        record_pass("P11-38", "Repeated cancellation attempt did not duplicate batch stock restoration (Stock remains 35)")
    else:
        record_fail("P11-38", "Cancellation idempotency", f"Expected 35, got {post_dup_stock}")

    # --- 6. Expiry Protection ---
    # Setup product with ONLY an expired batch
    exp_prod_sku = f"EXP-{cust_ts}"
    mysql_query(f"INSERT INTO fertilizer (name, sku, brand, selling_price, stock, minimum_stock, low_stock_threshold, status, active) VALUES ('Expired Stock Fertilizer', '{exp_prod_sku}', 'Old Corp', 150.00, 0, 5, 5, 'ACTIVE', 1);")
    exp_prod_id = int(mysql_query(f"SELECT id FROM fertilizer WHERE sku='{exp_prod_sku}';"))

    # Add expired batch with 10 units
    status, b_exp = api_call("POST", "/admin/inventory/batches", {
        "fertilizerId": exp_prod_id,
        "batchNumber": f"P11-EXPBAT-{cust_ts}",
        "quantity": 10,
        "expiryDate": exp_past
    }, token=admin_token)

    # P11-39: Public stock status reports OUT_OF_STOCK because expired stock cannot be sold
    status, stat_res = api_call("GET", f"/inventory/status/{exp_prod_id}")
    if status == 200 and stat_res.get("data", {}).get("available") is False and stat_res.get("data", {}).get("displayBadge") == "Out of Stock":
        record_pass("P11-39", "Public availability reports 'Out of Stock' for product with only expired batches")
    else:
        record_fail("P11-39", "Customer availability for expired batches", f"Expected available=False, got {stat_res}")

    # P11-40: Attempt to checkout product with expired batch is rejected
    api_call("POST", "/cart/items", {"fertilizerId": exp_prod_id, "quantity": 1}, token=cust_token)
    status, bad_order = api_call("POST", "/orders", {"addressId": addr_id, "paymentMethod": "COD"}, token=cust_token)
    if status in (400, 409):
        record_pass("P11-40", f"Order attempt for expired batch rejected with HTTP {status} (insufficient non-expired stock)")
    else:
        record_fail("P11-40", "Expiry purchase protection", f"Expected 400/409, got {status}: {bad_order}")

    # Clean cart
    api_call("DELETE", "/cart", token=cust_token)

    # --- 7. Batch Adjustments, Damaged & Blocking ---
    # P11-41: Batch Adjust ADD (+10)
    status, adj_add = api_call("POST", f"/admin/inventory/batches/{batch_C_id}/adjust", {
        "adjustmentType": "ADD",
        "quantity": 10,
        "reason": "Supplier bonus pack allocation"
    }, token=admin_token)
    if status == 200 and adj_add.get("data", {}).get("currentQuantity") == 30:
        record_pass("P11-41", "Admin adjustment ADD (+10) increased Batch C stock from 20 -> 30")
    else:
        record_fail("P11-41", "Batch adjust ADD", f"Expected 30, got {adj_add}")

    # P11-42: Batch Adjust REMOVE (-5)
    status, adj_rem = api_call("POST", f"/admin/inventory/batches/{batch_C_id}/adjust", {
        "adjustmentType": "REMOVE",
        "quantity": 5,
        "reason": "Laboratory sample extraction"
    }, token=admin_token)
    if status == 200 and adj_rem.get("data", {}).get("currentQuantity") == 25:
        record_pass("P11-42", "Admin adjustment REMOVE (-5) reduced Batch C stock from 30 -> 25")
    else:
        record_fail("P11-42", "Batch adjust REMOVE", f"Expected 25, got {adj_rem}")

    # P11-43: Batch Adjust without mandatory reason rejected
    status, adj_noreason = api_call("POST", f"/admin/inventory/batches/{batch_C_id}/adjust", {
        "adjustmentType": "ADD",
        "quantity": 5,
        "reason": "   "
    }, token=admin_token)
    if status == 400:
        record_pass("P11-43", "Batch adjustment with empty reason rejected (HTTP 400)")
    else:
        record_fail("P11-43", "Mandatory adjustment reason", f"Expected 400, got {status}")

    # P11-44: Batch Adjust REMOVE exceeding available quantity rejected
    status, adj_excess = api_call("POST", f"/admin/inventory/batches/{batch_C_id}/adjust", {
        "adjustmentType": "REMOVE",
        "quantity": 999,
        "reason": "Over-reduction attempt"
    }, token=admin_token)
    if status == 400:
        record_pass("P11-44", "Adjustment REMOVE exceeding available quantity strictly rejected (HTTP 400)")
    else:
        record_fail("P11-44", "Negative quantity prevention", f"Expected 400, got {status}")

    # P11-45: Mark damaged units
    status, dmg_res = api_call("POST", f"/admin/inventory/batches/{batch_C_id}/damaged", {
        "quantity": 3,
        "reason": "Warehouse bag tear during transit"
    }, token=admin_token)
    if status == 200 and dmg_res.get("data", {}).get("currentQuantity") == 22:
        record_pass("P11-45", "Mark damaged (-3 units) reduced Batch C stock from 25 -> 22")
    else:
        record_fail("P11-45", "Mark damaged", f"Expected 22, got {dmg_res}")

    # P11-46: Mark damaged exceeding available stock rejected
    status, dmg_bad = api_call("POST", f"/admin/inventory/batches/{batch_C_id}/damaged", {
        "quantity": 999,
        "reason": "Over damage attempt"
    }, token=admin_token)
    if status == 400:
        record_pass("P11-46", "Mark damaged exceeding available stock rejected (HTTP 400)")
    else:
        record_fail("P11-46", "Damaged excess guard", f"Expected 400, got {status}")

    # P11-47: Block Batch from sale
    status, blk_res = api_call("POST", f"/admin/inventory/batches/{batch_B_id}/block", {
        "reason": "Quality check hold"
    }, token=admin_token)
    if status == 200 and blk_res.get("data", {}).get("status") == "BLOCKED":
        record_pass("P11-47", f"Batch B blocked from sale (status: BLOCKED)")
    else:
        record_fail("P11-47", "Block batch", f"Expected BLOCKED, got {blk_res}")

    # P11-48: Blocked batch excluded from FEFO allocation
    # Available on fefo_prod_id is now: Batch A (5), Batch B (BLOCKED: 10), Batch C (22).
    # Customer orders 20 units -> Should take 5 from Batch A, SKIP Batch B, and take 15 from Batch C!
    api_call("POST", "/cart/items", {"fertilizerId": fefo_prod_id, "quantity": 20}, token=cust_token)
    status, blk_order = api_call("POST", "/orders", {"addressId": addr_id, "paymentMethod": "COD"}, token=cust_token)
    if status == 201:
        # Check Batch B is still 10 (untouched because blocked)
        curr_B_post = int(mysql_query(f"SELECT current_quantity FROM fertilizer_batches WHERE id={batch_B_id};"))
        if curr_B_post == 10:
            record_pass("P11-48", "FEFO skipped blocked Batch B completely (remains 10 units)")
        else:
            record_fail("P11-48", "FEFO skip blocked batch", f"Expected 10, got {curr_B_post}")
    else:
        record_fail("P11-48", "Order with blocked batch", f"Expected 201, got {status}: {blk_order}")

    # P11-49: Unblock Batch
    status, unblk_res = api_call("POST", f"/admin/inventory/batches/{batch_B_id}/unblock", {}, token=admin_token)
    if status == 200 and unblk_res.get("data", {}).get("status") in ("ACTIVE", "NEAR_EXPIRY"):
        record_pass("P11-49", f"Batch B successfully unblocked (status: {unblk_res['data']['status']})")
    else:
        record_fail("P11-49", "Unblock batch", f"Expected ACTIVE/NEAR_EXPIRY, got {unblk_res}")

    # P11-50: Unblocking an expired batch is strictly rejected
    status, unblk_exp = api_call("POST", f"/admin/inventory/batches/{batch_id_exp}/unblock", {}, token=admin_token)
    if status == 400:
        record_pass("P11-50", "Attempting to unblock an expired batch rejected (HTTP 400)")
    else:
        record_fail("P11-50", "Unblock expired batch guard", f"Expected 400, got {status}")

    # --- 8. Filter, History & Health ---
    # P11-51: Filter batches by status
    status, flt_res = api_call("GET", "/admin/inventory/batches?status=EXPIRED", token=admin_token)
    if status == 200 and len(flt_res.get("data", [])) > 0:
        all_exp = all(b.get("status") == "EXPIRED" for b in flt_res["data"])
        if all_exp:
            record_pass("P11-51", f"Filter ?status=EXPIRED returned strictly expired batches ({len(flt_res['data'])} items)")
        else:
            record_fail("P11-51", "Status filter check", "Found non-expired batches in filter result")
    else:
        record_fail("P11-51", "Status filter query", f"Got status {status}: {flt_res}")

    # P11-52: Update batch metadata PUT /api/admin/inventory/batches/{id}
    status, upd_res = api_call("PUT", f"/admin/inventory/batches/{batch_id_1}", {
        "notes": "Updated storage location: Rack B-14",
        "supplierReference": "SR-UPDATED-99"
    }, token=admin_token)
    if status == 200 and upd_res.get("data", {}).get("notes") == "Updated storage location: Rack B-14":
        record_pass("P11-52", "PUT /api/admin/inventory/batches/{id} successfully updated metadata")
    else:
        record_fail("P11-52", "Update batch metadata", f"Got status {status}: {upd_res}")

    # P11-53: Comprehensive Inventory Health Check (Phase 10 + Phase 11)
    status, health_res = api_call("GET", "/admin/inventory/health", token=admin_token)
    if status == 200 and health_res.get("success"):
        h = health_res.get("data", {})
        batches_checked = h.get("batchesChecked", 0)
        neg_count = h.get("negativeStockCount", 0)
        h_status = h.get("status")
        if batches_checked > 0 and neg_count == 0 and h_status == "HEALTHY":
            record_pass("P11-53", f"Health Check confirmed HEALTHY: {batches_checked} batches checked, 0 negative stock violations")
        else:
            record_fail("P11-53", "Health check values", f"Status: {h_status}, Batches: {batches_checked}, NegCount: {neg_count}, Issues: {h.get('issuesList')}")
    else:
        record_fail("P11-53", "Health check execution", f"Got status {status}: {health_res}")

    print("======================================================================")
    print(f"PHASE 11 TEST RESULTS: {passed_count} PASSED, {failed_count} FAILED out of {passed_count + failed_count} TESTS")
    print("======================================================================")

if __name__ == "__main__":
    run_tests()
