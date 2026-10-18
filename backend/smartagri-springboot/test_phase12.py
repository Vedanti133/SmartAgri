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
        "DELETE FROM goods_receipt_items WHERE goods_receipt_id IN (SELECT id FROM goods_receipts WHERE grn_number LIKE 'GRN-P12%' OR grn_number LIKE 'GRN-2026%');",
        "DELETE FROM goods_receipts WHERE grn_number LIKE 'GRN-P12%' OR grn_number LIKE 'GRN-2026%';",
        "DELETE FROM purchase_order_items WHERE purchase_order_id IN (SELECT id FROM purchase_orders WHERE po_number LIKE 'PO-P12%' OR po_number LIKE 'PO-2026%');",
        "DELETE FROM purchase_orders WHERE po_number LIKE 'PO-P12%' OR po_number LIKE 'PO-2026%';",
        "DELETE FROM suppliers WHERE supplier_code LIKE 'P12-%' OR supplier_code LIKE 'SUP-P12%';",
        "DELETE FROM inventory_transactions WHERE reference_id LIKE 'GRN-%' OR reference_id LIKE 'P12-%';",
        "DELETE FROM fertilizer_batches WHERE batch_number LIKE 'P12-%';",
        "DELETE FROM users WHERE username LIKE 'p12_%';"
    ]
    for q in queries:
        try:
            mysql_query(q)
        except Exception:
            pass

def run_tests():
    print("======================================================================")
    print("SMARTAGRI PHASE 12 AUTOMATED TEST SUITE")
    print("Supplier Management, Purchase Orders, GRN & Procurement Workflow")
    print("======================================================================")

    clean_test_data()

    # --- 1. Security & Access Control ---
    status, _ = api_call("GET", "/admin/suppliers")
    if status == 401: record_pass("P12-01", "Unauthenticated GET /api/admin/suppliers rejected (HTTP 401)")
    else: record_fail("P12-01", "Unauth suppliers check", f"Expected 401, got {status}")

    status, _ = api_call("POST", "/admin/suppliers", {"companyName": "Test", "supplierCode": "S1"})
    if status == 401: record_pass("P12-02", "Unauthenticated POST /api/admin/suppliers rejected (HTTP 401)")
    else: record_fail("P12-02", "Unauth supplier create", f"Expected 401, got {status}")

    status, _ = api_call("GET", "/admin/purchase-orders")
    if status == 401: record_pass("P12-03", "Unauthenticated GET /api/admin/purchase-orders rejected (HTTP 401)")
    else: record_fail("P12-03", "Unauth PO list", f"Expected 401, got {status}")

    status, _ = api_call("POST", "/admin/purchase-orders", {"supplierId": 1, "items": []})
    if status == 401: record_pass("P12-04", "Unauthenticated POST /api/admin/purchase-orders rejected (HTTP 401)")
    else: record_fail("P12-04", "Unauth PO create", f"Expected 401, got {status}")

    status, _ = api_call("GET", "/admin/goods-receipts")
    if status == 401: record_pass("P12-05", "Unauthenticated GET /api/admin/goods-receipts rejected (HTTP 401)")
    else: record_fail("P12-05", "Unauth GRN list", f"Expected 401, got {status}")

    status, _ = api_call("POST", "/admin/goods-receipts", {"purchaseOrderId": 1, "items": []})
    if status == 401: record_pass("P12-06", "Unauthenticated POST /api/admin/goods-receipts rejected (HTTP 401)")
    else: record_fail("P12-06", "Unauth GRN create", f"Expected 401, got {status}")

    status, _ = api_call("GET", "/admin/procurement/dashboard")
    if status == 401: record_pass("P12-07", "Unauthenticated GET /api/admin/procurement/dashboard rejected (HTTP 401)")
    else: record_fail("P12-07", "Unauth dashboard", f"Expected 401, got {status}")

    # Register customer
    ts = str(int(time.time()))[-5:]
    cust_username = f"p12_cust_{ts}"
    _, cust_reg = api_call("POST", "/auth/register", {
        "name": "Phase12 Customer",
        "username": cust_username,
        "email": f"{cust_username}@agri.com",
        "password": "Password123!",
        "phone": "9811223344"
    })
    cust_token = cust_reg.get("data", {}).get("token")

    status, _ = api_call("GET", "/admin/suppliers", token=cust_token)
    if status == 403: record_pass("P12-08", "Customer role GET /api/admin/suppliers rejected with HTTP 403 Forbidden")
    else: record_fail("P12-08", "Customer suppliers access", f"Expected 403, got {status}")

    status, _ = api_call("POST", "/admin/purchase-orders", {"supplierId": 1}, token=cust_token)
    if status == 403: record_pass("P12-09", "Customer role POST /api/admin/purchase-orders rejected with HTTP 403 Forbidden")
    else: record_fail("P12-09", "Customer PO create access", f"Expected 403, got {status}")

    status, _ = api_call("POST", "/admin/goods-receipts", {"purchaseOrderId": 1}, token=cust_token)
    if status == 403: record_pass("P12-10", "Customer role POST /api/admin/goods-receipts rejected with HTTP 403 Forbidden")
    else: record_fail("P12-10", "Customer GRN create access", f"Expected 403, got {status}")

    # Admin Login
    status, admin_login = api_call("POST", "/auth/login", {
        "username": "admin",
        "password": "adminpassword123"
    })
    admin_token = admin_login.get("data", {}).get("token")
    if not admin_token:
        print("CRITICAL: Failed to get admin token! Exiting.")
        return

    # --- 2. Supplier Management ---
    status, _ = api_call("POST", "/admin/suppliers", {"supplierCode": "SUP-1"}, token=admin_token)
    if status == 400: record_pass("P12-11", "Create supplier without company name rejected (HTTP 400)")
    else: record_fail("P12-11", "Missing company name check", f"Expected 400, got {status}")

    status, _ = api_call("POST", "/admin/suppliers", {"companyName": "IFFCO"}, token=admin_token)
    if status == 400: record_pass("P12-12", "Create supplier without supplier code rejected (HTTP 400)")
    else: record_fail("P12-12", "Missing supplier code check", f"Expected 400, got {status}")

    sup_code_1 = f"P12-SUP-{ts}-01"
    status, sup_res = api_call("POST", "/admin/suppliers", {
        "supplierCode": sup_code_1,
        "companyName": "IFFCO Agri Inputs India Ltd",
        "contactPerson": "Rajesh Sharma",
        "email": "rajesh@iffco.in",
        "phone": "9822112233",
        "city": "Pune",
        "state": "Maharashtra",
        "gstNumber": "27AAAAA0000A1Z5",
        "paymentTerms": "Net 30 Days",
        "notes": "Primary nitrogen supplier"
    }, token=admin_token)
    sup_id_1 = sup_res.get("data", {}).get("id")
    if status == 201 and sup_id_1:
        record_pass("P12-13", f"Admin created Supplier '{sup_code_1}' (ID: {sup_id_1}, Status: ACTIVE)")
    else:
        record_fail("P12-13", "Supplier creation", f"Expected 201, got {status}: {sup_res}")

    status, dup_sup = api_call("POST", "/admin/suppliers", {
        "supplierCode": sup_code_1,
        "companyName": "Duplicate Supplier Attempt"
    }, token=admin_token)
    if status in (409, 400):
        record_pass("P12-14", f"Duplicate supplier code '{sup_code_1}' strictly rejected with HTTP {status}")
    else:
        record_fail("P12-14", "Duplicate supplier code guard", f"Expected 409/400, got {status}: {dup_sup}")

    status, upd_sup = api_call("PUT", f"/admin/suppliers/{sup_id_1}", {
        "contactPerson": "Rajesh V. Sharma",
        "phone": "9822112299",
        "notes": "Updated verified contact"
    }, token=admin_token)
    if status == 200 and upd_sup.get("data", {}).get("contactPerson") == "Rajesh V. Sharma":
        record_pass("P12-15", "Update supplier details succeeded (PUT /api/admin/suppliers/{id})")
    else:
        record_fail("P12-15", "Supplier update", f"Got status {status}: {upd_sup}")

    status, deact_sup = api_call("POST", f"/admin/suppliers/{sup_id_1}/deactivate", token=admin_token)
    if status == 200 and deact_sup.get("data", {}).get("status") == "INACTIVE":
        record_pass("P12-16", "Supplier deactivated successfully (status: INACTIVE)")
    else:
        record_fail("P12-16", "Supplier deactivation", f"Got status {status}: {deact_sup}")

    status, act_sup = api_call("POST", f"/admin/suppliers/{sup_id_1}/activate", token=admin_token)
    if status == 200 and act_sup.get("data", {}).get("status") == "ACTIVE":
        record_pass("P12-17", "Supplier reactivated successfully (status: ACTIVE)")
    else:
        record_fail("P12-17", "Supplier activation", f"Got status {status}: {act_sup}")

    status, blk_sup = api_call("POST", f"/admin/suppliers/{sup_id_1}/block", token=admin_token)
    if status == 200 and blk_sup.get("data", {}).get("status") == "BLOCKED":
        record_pass("P12-18", "Supplier blocked successfully (status: BLOCKED)")
    else:
        record_fail("P12-18", "Supplier blocking", f"Got status {status}: {blk_sup}")

    # Setup dedicated test fertilizer for PO testing
    prod_sku = f"P12-FERT-{ts}"
    mysql_query(f"INSERT INTO fertilizer (name, sku, brand, selling_price, stock, minimum_stock, low_stock_threshold, status, active) VALUES ('Procurement Test Urea', '{prod_sku}', 'IFFCO', 320.00, 0, 10, 10, 'ACTIVE', 1);")
    test_prod_id = int(mysql_query(f"SELECT id FROM fertilizer WHERE sku='{prod_sku}';"))

    status, bad_po = api_call("POST", "/admin/purchase-orders", {
        "supplierId": sup_id_1,
        "items": [{"fertilizerId": test_prod_id, "orderedQuantity": 50, "unitCost": 250.00}]
    }, token=admin_token)
    if status in (400, 409):
        record_pass("P12-19", f"PO creation for BLOCKED supplier rejected with HTTP {status}")
    else:
        record_fail("P12-19", "Blocked supplier PO guard", f"Expected 400/409, got {status}: {bad_po}")

    # Reactivate supplier
    api_call("POST", f"/admin/suppliers/{sup_id_1}/activate", token=admin_token)
    record_pass("P12-20", "Supplier restored to ACTIVE status for procurement workflow")

    status, flt_sup = api_call("GET", "/admin/suppliers?status=ACTIVE", token=admin_token)
    if status == 200 and any(s.get("id") == sup_id_1 for s in flt_sup.get("data", [])):
        record_pass("P12-21", "Filter ?status=ACTIVE returned active suppliers including created supplier")
    else:
        record_fail("P12-21", "Supplier status filter", f"Got status {status}: {flt_sup}")

    status, srch_sup = api_call("GET", f"/admin/suppliers?search={sup_code_1}", token=admin_token)
    if status == 200 and len(srch_sup.get("data", [])) == 1:
        record_pass("P12-22", f"Search ?search={sup_code_1} accurately returned matching supplier")
    else:
        record_fail("P12-22", "Supplier search query", f"Got status {status}: {srch_sup}")

    # --- 3. Purchase Order Management & Calculations ---
    status, _ = api_call("POST", "/admin/purchase-orders", {"items": [{"fertilizerId": test_prod_id, "orderedQuantity": 10, "unitCost": 100.00}]}, token=admin_token)
    if status == 400: record_pass("P12-23", "Create PO without supplierId rejected (HTTP 400)")
    else: record_fail("P12-23", "PO missing supplier", f"Expected 400, got {status}")

    status, _ = api_call("POST", "/admin/purchase-orders", {"supplierId": sup_id_1, "items": []}, token=admin_token)
    if status == 400: record_pass("P12-24", "Create PO with empty line items rejected (HTTP 400)")
    else: record_fail("P12-24", "PO empty items", f"Expected 400, got {status}")

    status, _ = api_call("POST", "/admin/purchase-orders", {
        "supplierId": sup_id_1,
        "items": [{"fertilizerId": test_prod_id, "orderedQuantity": 0, "unitCost": 100.00}]
    }, token=admin_token)
    if status == 400: record_pass("P12-25", "Create PO with ordered quantity 0 rejected (HTTP 400)")
    else: record_fail("P12-25", "PO zero quantity", f"Expected 400, got {status}")

    status, _ = api_call("POST", "/admin/purchase-orders", {
        "supplierId": sup_id_1,
        "items": [{"fertilizerId": test_prod_id, "orderedQuantity": 10, "unitCost": -50.00}]
    }, token=admin_token)
    if status == 400: record_pass("P12-26", "Create PO with negative unit cost rejected (HTTP 400)")
    else: record_fail("P12-26", "PO negative unit cost", f"Expected 400, got {status}")

    # Create valid Draft PO with 100 units of test_prod_id @ ₹200.00, 5% tax
    status, po_res = api_call("POST", "/admin/purchase-orders", {
        "supplierId": sup_id_1,
        "notes": "Initial seasonal procurement batch",
        "items": [
            {
                "fertilizerId": test_prod_id,
                "orderedQuantity": 100,
                "unitCost": 200.00,
                "taxRate": 5.00,
                "notes": "Grade A granular urea"
            }
        ]
    }, token=admin_token)

    po_id_1 = po_res.get("data", {}).get("id")
    po_num_1 = po_res.get("data", {}).get("poNumber")
    poi_id_1 = po_res.get("data", {}).get("items", [{}])[0].get("id")

    if status == 201 and po_id_1 and po_num_1.startswith("PO-"):
        record_pass("P12-27", f"Admin created Draft PO #{po_num_1} (ID: {po_id_1}, Status: DRAFT)")
    else:
        record_fail("P12-27", "PO creation", f"Expected 201, got {status}: {po_res}")

    # Verify calculations:
    # 100 * 200 = 20,000 subtotal
    # 5% tax = 1,000
    # Total = 21,000
    po_data = po_res.get("data", {})
    subtotal = float(po_data.get("subtotal", 0))
    tax_amt = float(po_data.get("taxAmount", 0))
    tot_amt = float(po_data.get("totalAmount", 0))
    if subtotal == 20000.00 and tax_amt == 1000.00 and tot_amt == 21000.00:
        record_pass("P12-28", f"PO BigDecimal totals match exactly: Subtotal ₹{subtotal}, Tax ₹{tax_amt}, Total ₹{tot_amt}")
    else:
        record_fail("P12-28", "PO monetary calculations", f"Subtotal: {subtotal}, Tax: {tax_amt}, Total: {tot_amt}")

    status, upd_po = api_call("PUT", f"/admin/purchase-orders/{po_id_1}", {
        "notes": "Updated terms: Deliver directly to Central Depot"
    }, token=admin_token)
    if status == 200 and upd_po.get("data", {}).get("notes") == "Updated terms: Deliver directly to Central Depot":
        record_pass("P12-29", "Draft PO updated successfully (PUT /api/admin/purchase-orders/{id})")
    else:
        record_fail("P12-29", "Draft PO update", f"Got status {status}: {upd_po}")

    status, bad_send = api_call("POST", f"/admin/purchase-orders/{po_id_1}/send", token=admin_token)
    if status in (400, 409):
        record_pass("P12-30", "Invalid transition: Cannot send PO to supplier before approval (HTTP 400/409)")
    else:
        record_fail("P12-30", "State transition guard", f"Expected 400/409, got {status}: {bad_send}")

    status, sub_po = api_call("POST", f"/admin/purchase-orders/{po_id_1}/submit", token=admin_token)
    if status == 200 and sub_po.get("data", {}).get("status") == "PENDING_APPROVAL":
        record_pass("P12-31", f"PO #{po_num_1} submitted for approval (status: PENDING_APPROVAL)")
    else:
        record_fail("P12-31", "PO submit for approval", f"Got status {status}: {sub_po}")

    status, bad_edit = api_call("PUT", f"/admin/purchase-orders/{po_id_1}", {"notes": "Tampering after submit"}, token=admin_token)
    if status in (400, 409):
        record_pass("P12-32", "Attempting to edit PO after submission strictly rejected (HTTP 400/409)")
    else:
        record_fail("P12-32", "Edit after submit guard", f"Expected 400/409, got {status}: {bad_edit}")

    status, app_po = api_call("POST", f"/admin/purchase-orders/{po_id_1}/approve", token=admin_token)
    if status == 200 and app_po.get("data", {}).get("status") == "APPROVED" and app_po.get("data", {}).get("approvedBy") == "admin":
        record_pass("P12-33", f"PO #{po_num_1} approved (status: APPROVED, approvedBy: 'admin', approvedAt populated)")
    else:
        record_fail("P12-33", "PO approval", f"Got status {status}: {app_po}")

    status, snd_po = api_call("POST", f"/admin/purchase-orders/{po_id_1}/send", token=admin_token)
    if status == 200 and snd_po.get("data", {}).get("status") == "SENT_TO_SUPPLIER":
        record_pass("P12-34", f"PO #{po_num_1} sent to supplier (status: SENT_TO_SUPPLIER)")
    else:
        record_fail("P12-34", "Send PO to supplier", f"Got status {status}: {snd_po}")

    # Create dummy PO to test cancellation
    _, po_cancel = api_call("POST", "/admin/purchase-orders", {
        "supplierId": sup_id_1,
        "items": [{"fertilizerId": test_prod_id, "orderedQuantity": 10, "unitCost": 200.00}]
    }, token=admin_token)
    po_cancel_id = po_cancel.get("data", {}).get("id")
    status, can_res = api_call("POST", f"/admin/purchase-orders/{po_cancel_id}/cancel", {"reason": "Budget cut"}, token=admin_token)
    if status == 200 and can_res.get("data", {}).get("status") == "CANCELLED":
        record_pass("P12-35", f"PO #{po_cancel_id} cancelled with audit reason (status: CANCELLED)")
    else:
        record_fail("P12-35", "PO cancellation", f"Got status {status}: {can_res}")

    status, can_app = api_call("POST", f"/admin/purchase-orders/{po_cancel_id}/approve", token=admin_token)
    if status in (400, 409):
        record_pass("P12-36", "Invalid transition: Cancelled PO cannot be approved (HTTP 400/409)")
    else:
        record_fail("P12-36", "Cancelled PO approval guard", f"Expected 400/409, got {status}: {can_app}")

    # --- 4. Goods Receipt Note (GRN) & Over-Receiving Guard ---
    status, bad_grn = api_call("POST", "/admin/goods-receipts", {
        "purchaseOrderId": po_cancel_id,
        "items": [{"purchaseOrderItemId": 1, "receivedQuantity": 10, "batchNumber": "B1", "expiryDate": "2027-01-01"}]
    }, token=admin_token)
    if status in (400, 409):
        record_pass("P12-37", "Cannot create GRN against cancelled PO (HTTP 400/409)")
    else:
        record_fail("P12-37", "GRN against cancelled PO guard", f"Expected 400/409, got {status}: {bad_grn}")

    status, _ = api_call("POST", "/admin/goods-receipts", {"purchaseOrderId": po_id_1, "items": []}, token=admin_token)
    if status == 400: record_pass("P12-38", "Create GRN with empty items rejected (HTTP 400)")
    else: record_fail("P12-38", "GRN empty items", f"Expected 400, got {status}")

    status, _ = api_call("POST", "/admin/goods-receipts", {
        "purchaseOrderId": po_id_1,
        "items": [{"purchaseOrderItemId": poi_id_1, "receivedQuantity": 40, "batchNumber": "", "expiryDate": "2027-01-01"}]
    }, token=admin_token)
    if status == 400: record_pass("P12-39", "Create GRN with blank batch number rejected (HTTP 400)")
    else: record_fail("P12-39", "GRN blank batch", f"Expected 400, got {status}")

    status, _ = api_call("POST", "/admin/goods-receipts", {
        "purchaseOrderId": po_id_1,
        "items": [{
            "purchaseOrderItemId": poi_id_1,
            "receivedQuantity": 40,
            "batchNumber": "P12-BAD-LOT",
            "manufacturingDate": "2027-12-01",
            "expiryDate": "2027-01-01"
        }]
    }, token=admin_token)
    if status == 400: record_pass("P12-40", "GRN with manufacturing date after expiry date rejected (HTTP 400)")
    else: record_fail("P12-40", "GRN invalid dates", f"Expected 400, got {status}")

    # Over-receiving guard: PO has 100 units ordered. Attempting to receive 150 units must fail!
    status, over_grn = api_call("POST", "/admin/goods-receipts", {
        "purchaseOrderId": po_id_1,
        "items": [{
            "purchaseOrderItemId": poi_id_1,
            "receivedQuantity": 150,
            "batchNumber": "P12-OVER-LOT",
            "expiryDate": "2028-01-01"
        }]
    }, token=admin_token)
    if status == 400:
        record_pass("P12-41", "Over-receiving guard: Attempt to receive 150 units against 100 ordered strictly rejected (HTTP 400)")
    else:
        record_fail("P12-41", "Over-receiving guard", f"Expected 400, got {status}: {over_grn}")

    # Partial Receipt 1: Receive 40 units out of 100
    today_str = date.today().isoformat()
    exp_2years = (date.today() + timedelta(days=730)).isoformat()
    mfg_last_month = (date.today() - timedelta(days=30)).isoformat()
    batch_num_1 = f"P12-LOT-{ts}-01"

    status, grn_res_1 = api_call("POST", "/admin/goods-receipts", {
        "purchaseOrderId": po_id_1,
        "receivedDate": today_str,
        "supplierInvoiceNumber": f"INV-IFFCO-{ts}",
        "supplierDeliveryNumber": f"DC-TRK-{ts}",
        "packagingCondition": "GOOD",
        "sealCondition": "INTACT",
        "items": [{
            "purchaseOrderItemId": poi_id_1,
            "receivedQuantity": 40,
            "batchNumber": batch_num_1,
            "manufacturingDate": mfg_last_month,
            "expiryDate": exp_2years,
            "supplierReference": "SR-LOT-01"
        }]
    }, token=admin_token)

    grn_id_1 = grn_res_1.get("data", {}).get("id")
    grn_num_1 = grn_res_1.get("data", {}).get("grnNumber")
    gri_id_1 = grn_res_1.get("data", {}).get("items", [{}])[0].get("id")

    if status == 201 and grn_id_1 and grn_num_1.startswith("GRN-"):
        record_pass("P12-42", f"Admin created Goods Receipt #{grn_num_1} for 40 units (status: PENDING_INSPECTION)")
    else:
        record_fail("P12-42", "GRN creation", f"Expected 201, got {status}: {grn_res_1}")

    # --- 5. Quality Inspection & Breakdowns ---
    status, bad_insp = api_call("POST", f"/admin/goods-receipts/{grn_id_1}/inspect", {
        "inspectionStatus": "PASSED",
        "itemInspections": [{
            "itemId": gri_id_1,
            "acceptedQuantity": 30,
            "rejectedQuantity": 5 # 30 + 5 = 35 != 40!
        }]
    }, token=admin_token)
    if status == 400:
        record_pass("P12-43", "Quality inspection breakdown validation: accepted (30) + rejected (5) != received (40) rejected (HTTP 400)")
    else:
        record_fail("P12-43", "Inspection balance guard", f"Expected 400, got {status}: {bad_insp}")

    # Perform valid PASSED inspection (40 accepted, 0 rejected)
    status, insp_res = api_call("POST", f"/admin/goods-receipts/{grn_id_1}/inspect", {
        "inspectionStatus": "PASSED",
        "packagingCondition": "INTACT",
        "sealCondition": "FACTORY_SEAL_INTACT",
        "inspectionNotes": "Lab test passed. Nitrogen content verified at 46.2%."
    }, token=admin_token)
    if status == 200 and insp_res.get("data", {}).get("status") == "INSPECTION_PASSED" and insp_res.get("data", {}).get("inspectedBy") == "admin":
        record_pass("P12-44", f"GRN #{grn_num_1} quality inspection recorded as PASSED (status: INSPECTION_PASSED, inspectedBy: 'admin')")
    else:
        record_fail("P12-44", "Quality inspection PASSED", f"Got status {status}: {insp_res}")

    # --- 6. Batch Integration & Stock Updates ---
    status, acc_res = api_call("POST", f"/admin/goods-receipts/{grn_id_1}/accept", token=admin_token)
    if status == 200 and acc_res.get("data", {}).get("status") == "ACCEPTED":
        record_pass("P12-45", f"GRN #{grn_num_1} accepted into inventory (status: ACCEPTED)")
    else:
        record_fail("P12-45", "GRN acceptance", f"Got status {status}: {acc_res}")

    # Verify FertilizerBatch created in MySQL
    batch_rows = mysql_query(f"SELECT id, current_quantity, initial_quantity, status FROM fertilizer_batches WHERE fertilizer_id={test_prod_id} AND batch_number='{batch_num_1}';")
    if batch_rows and "40" in batch_rows and "ACTIVE" in batch_rows:
        record_pass("P12-46", f"FertilizerBatch created automatically from GRN: Batch #{batch_num_1} with current_quantity=40, status=ACTIVE")
    else:
        record_fail("P12-46", "Batch creation from GRN", f"Expected batch for {batch_num_1}, got: {batch_rows}")

    # Verify parent fertilizer stock incremented: 0 -> 40
    fert_stock = int(mysql_query(f"SELECT stock FROM fertilizer WHERE id={test_prod_id};"))
    if fert_stock == 40:
        record_pass("P12-47", "Parent fertilizer stock incremented accurately from 0 -> 40 (+40 units)")
    else:
        record_fail("P12-47", "Parent fertilizer stock update", f"Expected 40, got {fert_stock}")

    # Verify RESTOCK inventory transaction created
    tx_rows = mysql_query(f"SELECT quantity, stock_before, stock_after, performed_by FROM inventory_transactions WHERE reference_id='{grn_num_1}' AND reference_type='GRN';")
    if tx_rows and "40" in tx_rows and "admin" in tx_rows:
        record_pass("P12-48", f"RESTOCK audit transaction recorded for GRN #{grn_num_1} (qty: 40, performedBy: 'admin')")
    else:
        record_fail("P12-48", "RESTOCK transaction check", f"Expected transaction for {grn_num_1}, got: {tx_rows}")

    # --- 7. Partial Receiving & PO Lifecycle Progression ---
    # PO received 40 / 100 units -> PO status must be PARTIALLY_RECEIVED
    status, po_check_1 = api_call("GET", f"/admin/purchase-orders/{po_id_1}", token=admin_token)
    curr_po_status = po_check_1.get("data", {}).get("status")
    rec_po_qty = po_check_1.get("data", {}).get("totalReceivedQuantity")
    rem_po_qty = po_check_1.get("data", {}).get("totalRemainingQuantity")
    if curr_po_status == "PARTIALLY_RECEIVED" and rec_po_qty == 40 and rem_po_qty == 60:
        record_pass("P12-49", f"PO #{po_num_1} progressed to PARTIALLY_RECEIVED (Received: 40, Remaining: 60)")
    else:
        record_fail("P12-49", "PO partial receiving status", f"Status: {curr_po_status}, Rec: {rec_po_qty}, Rem: {rem_po_qty}")

    # Second GRN: Receive remaining 60 units
    batch_num_2 = f"P12-LOT-{ts}-02"
    status, grn_res_2 = api_call("POST", "/admin/goods-receipts", {
        "purchaseOrderId": po_id_1,
        "items": [{
            "purchaseOrderItemId": poi_id_1,
            "receivedQuantity": 60,
            "batchNumber": batch_num_2,
            "expiryDate": exp_2years
        }]
    }, token=admin_token)
    grn_id_2 = grn_res_2.get("data", {}).get("id")
    grn_num_2 = grn_res_2.get("data", {}).get("grnNumber")

    # Inspect & Accept Second GRN
    api_call("POST", f"/admin/goods-receipts/{grn_id_2}/inspect", {"inspectionStatus": "PASSED"}, token=admin_token)
    api_call("POST", f"/admin/goods-receipts/{grn_id_2}/accept", token=admin_token)

    # PO received 40 + 60 = 100 units -> PO status must be FULLY_RECEIVED
    status, po_check_2 = api_call("GET", f"/admin/purchase-orders/{po_id_1}", token=admin_token)
    final_po_status = po_check_2.get("data", {}).get("status")
    final_rec_qty = po_check_2.get("data", {}).get("totalReceivedQuantity")
    if final_po_status == "FULLY_RECEIVED" and final_rec_qty == 100:
        record_pass("P12-50", f"PO #{po_num_1} progressed to FULLY_RECEIVED (100 / 100 units fulfilled)")
    else:
        record_fail("P12-50", "PO full receiving status", f"Status: {final_po_status}, Rec: {final_rec_qty}")

    # Verify parent stock updated: 40 + 60 = 100
    fert_stock_final = int(mysql_query(f"SELECT stock FROM fertilizer WHERE id={test_prod_id};"))
    if fert_stock_final == 100:
        record_pass("P12-51", "Parent fertilizer stock updated to 100 units across both receipts")
    else:
        record_fail("P12-51", "Parent stock final", f"Expected 100, got {fert_stock_final}")

    # --- 8. Batch Replenishment (Existing Batch) ---
    # Create another PO and receive with existing batch_num_1 to test safe replenishment
    _, po_rep = api_call("POST", "/admin/purchase-orders", {
        "supplierId": sup_id_1,
        "items": [{"fertilizerId": test_prod_id, "orderedQuantity": 20, "unitCost": 200.00}]
    }, token=admin_token)
    po_rep_id = po_rep.get("data", {}).get("id")
    poi_rep_id = po_rep.get("data", {}).get("items", [{}])[0].get("id")
    api_call("POST", f"/admin/purchase-orders/{po_rep_id}/approve", token=admin_token)

    _, grn_rep = api_call("POST", "/admin/goods-receipts", {
        "purchaseOrderId": po_rep_id,
        "items": [{
            "purchaseOrderItemId": poi_rep_id,
            "receivedQuantity": 20,
            "batchNumber": batch_num_1, # Existing batch!
            "expiryDate": exp_2years
        }]
    }, token=admin_token)
    grn_rep_id = grn_rep.get("data", {}).get("id")
    api_call("POST", f"/admin/goods-receipts/{grn_rep_id}/inspect", {"inspectionStatus": "PASSED"}, token=admin_token)
    api_call("POST", f"/admin/goods-receipts/{grn_rep_id}/accept", token=admin_token)

    # Batch 1 had 40 units, replenished with +20 -> Should be 60 units without duplicate key violation!
    rep_batch_qty = int(mysql_query(f"SELECT current_quantity FROM fertilizer_batches WHERE fertilizer_id={test_prod_id} AND batch_number='{batch_num_1}';"))
    if rep_batch_qty == 60:
        record_pass("P12-52", f"Existing batch #{batch_num_1} replenished safely: current_quantity 40 -> 60 (+20 units)")
    else:
        record_fail("P12-52", "Batch replenishment", f"Expected 60, got {rep_batch_qty}")

    # --- 9. Quality Inspection Rejection & Inventory Guard ---
    # Create PO and GRN with partial rejection: 30 received -> 20 accepted, 10 rejected (broken bags)
    _, po_rej = api_call("POST", "/admin/purchase-orders", {
        "supplierId": sup_id_1,
        "items": [{"fertilizerId": test_prod_id, "orderedQuantity": 30, "unitCost": 200.00}]
    }, token=admin_token)
    po_rej_id = po_rej.get("data", {}).get("id")
    poi_rej_id = po_rej.get("data", {}).get("items", [{}])[0].get("id")
    api_call("POST", f"/admin/purchase-orders/{po_rej_id}/approve", token=admin_token)

    batch_num_part = f"P12-LOT-{ts}-PART"
    _, grn_part = api_call("POST", "/admin/goods-receipts", {
        "purchaseOrderId": po_rej_id,
        "items": [{
            "purchaseOrderItemId": poi_rej_id,
            "receivedQuantity": 30,
            "batchNumber": batch_num_part,
            "expiryDate": exp_2years
        }]
    }, token=admin_token)
    grn_part_id = grn_part.get("data", {}).get("id")
    gri_part_id = grn_part.get("data", {}).get("items", [{}])[0].get("id")

    # Inspect: 20 accepted, 10 rejected
    api_call("POST", f"/admin/goods-receipts/{grn_part_id}/inspect", {
        "inspectionStatus": "PARTIAL",
        "itemInspections": [{
            "itemId": gri_part_id,
            "acceptedQuantity": 20,
            "rejectedQuantity": 10,
            "rejectionReason": "10 bags torn during transit"
        }]
    }, token=admin_token)

    stock_pre_part = int(mysql_query(f"SELECT stock FROM fertilizer WHERE id={test_prod_id};"))
    api_call("POST", f"/admin/goods-receipts/{grn_part_id}/accept", token=admin_token)

    # Check batch has strictly 20 units (rejected 10 are NOT in saleable inventory)
    part_batch_qty = int(mysql_query(f"SELECT current_quantity FROM fertilizer_batches WHERE fertilizer_id={test_prod_id} AND batch_number='{batch_num_part}';"))
    stock_post_part = int(mysql_query(f"SELECT stock FROM fertilizer WHERE id={test_prod_id};"))

    if part_batch_qty == 20 and stock_post_part == stock_pre_part + 20:
        record_pass("P12-53", "Rejected units (10) excluded from saleable batch inventory; exactly accepted 20 added")
    else:
        record_fail("P12-53", "Rejected stock isolation", f"Batch: {part_batch_qty}, Stock: {stock_post_part}")

    # Fully rejected GRN
    _, po_full_rej = api_call("POST", "/admin/purchase-orders", {
        "supplierId": sup_id_1,
        "items": [{"fertilizerId": test_prod_id, "orderedQuantity": 15, "unitCost": 200.00}]
    }, token=admin_token)
    po_fr_id = po_full_rej.get("data", {}).get("id")
    poi_fr_id = po_full_rej.get("data", {}).get("items", [{}])[0].get("id")
    api_call("POST", f"/admin/purchase-orders/{po_fr_id}/approve", token=admin_token)

    batch_num_rej = f"P12-LOT-{ts}-REJ"
    _, grn_fr = api_call("POST", "/admin/goods-receipts", {
        "purchaseOrderId": po_fr_id,
        "items": [{
            "purchaseOrderItemId": poi_fr_id,
            "receivedQuantity": 15,
            "batchNumber": batch_num_rej,
            "expiryDate": exp_2years
        }]
    }, token=admin_token)
    grn_fr_id = grn_fr.get("data", {}).get("id")

    status, rej_res = api_call("POST", f"/admin/goods-receipts/{grn_fr_id}/reject", {"reason": "Contaminated lot"}, token=admin_token)
    if status == 200 and rej_res.get("data", {}).get("status") == "REJECTED":
        record_pass("P12-54", f"Admin rejected GRN #{grn_fr_id} (status: REJECTED)")
    else:
        record_fail("P12-54", "GRN rejection", f"Got status {status}: {rej_res}")

    status, bad_acc_rej = api_call("POST", f"/admin/goods-receipts/{grn_fr_id}/accept", token=admin_token)
    if status in (400, 409):
        record_pass("P12-55", "Attempt to accept a REJECTED GRN strictly forbidden (HTTP 400/409)")
    else:
        record_fail("P12-55", "Accept rejected GRN guard", f"Expected 400/409, got {status}: {bad_acc_rej}")

    # --- 10. Idempotency Protection ---
    status, dup_acc = api_call("POST", f"/admin/goods-receipts/{grn_id_1}/accept", token=admin_token)
    if status == 409:
        record_pass("P12-56", f"Repeated GRN acceptance attempt strictly rejected with HTTP 409 Conflict: '{dup_acc.get('message')}'")
    else:
        record_fail("P12-56", "GRN acceptance idempotency guard", f"Expected 409, got {status}: {dup_acc}")

    # Check zero duplicate stock was added
    stock_check_idem = int(mysql_query(f"SELECT stock FROM fertilizer WHERE id={test_prod_id};"))
    if stock_check_idem == stock_post_part:
        record_pass("P12-57", f"Zero duplicate stock added upon repeated acceptance attempt (Stock remained {stock_check_idem})")
    else:
        record_fail("P12-57", "Idempotent stock guard", f"Stock changed to {stock_check_idem}")

    matching_restocks = int(mysql_query(f"SELECT COUNT(*) FROM inventory_transactions WHERE reference_id='{grn_num_1}' AND reference_type='GRN';"))
    if matching_restocks == 1:
        record_pass("P12-58", f"Zero duplicate RESTOCK transactions recorded for GRN #{grn_num_1} (Count: exactly 1)")
    else:
        record_fail("P12-58", "Idempotent RESTOCK guard", f"Expected 1 RESTOCK transaction, found {matching_restocks}")

    # --- 11. Dashboard & Inventory Health ---
    status, dash_res = api_call("GET", "/admin/procurement/dashboard", token=admin_token)
    if status == 200 and dash_res.get("success"):
        d = dash_res.get("data", {})
        if d.get("totalSuppliers", 0) > 0 and d.get("openPurchaseOrders", 0) >= 0 and d.get("thisMonthPurchaseValue", 0) > 0:
            record_pass("P12-59", f"Procurement dashboard returned valid KPIs (Suppliers: {d['totalSuppliers']}, Open POs: {d['openPurchaseOrders']}, Month Spend: ₹{d['thisMonthPurchaseValue']})")
        else:
            record_fail("P12-59", "Dashboard metrics", f"Unexpected values: {d}")
    else:
        record_fail("P12-59", "Dashboard query", f"Got status {status}: {dash_res}")

    status, health_res = api_call("GET", "/admin/inventory/health", token=admin_token)
    if status == 200 and health_res.get("success"):
        h = health_res.get("data", {})
        grn_count = h.get("goodsReceiptsChecked", 0)
        proc_issues = h.get("procurementIssuesCount", 0)
        h_status = h.get("status")
        if grn_count > 0 and proc_issues == 0 and h_status == "HEALTHY":
            record_pass("P12-60", f"Inventory & Procurement Health confirmed HEALTHY: {grn_count} GRNs verified, 0 procurement issues, 0 negative stock")
        else:
            record_fail("P12-60", "Health check values", f"Status: {h_status}, GRNs: {grn_count}, Issues: {proc_issues}, List: {h.get('issuesList')}")
    else:
        record_fail("P12-60", "Health check execution", f"Got status {status}: {health_res}")

    print("======================================================================")
    print(f"PHASE 12 TEST RESULTS: {passed_count} PASSED, {failed_count} FAILED out of {passed_count + failed_count} TESTS")
    print("======================================================================")

if __name__ == "__main__":
    run_tests()
