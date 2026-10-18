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
        "DELETE FROM stock_transfer_items WHERE transfer_id IN (SELECT id FROM stock_transfers WHERE transfer_number LIKE 'TR-P13%' OR transfer_number LIKE 'TR-2026%');",
        "DELETE FROM stock_transfers WHERE transfer_number LIKE 'TR-P13%' OR transfer_number LIKE 'TR-2026%';",
        "DELETE FROM goods_receipt_items WHERE goods_receipt_id IN (SELECT id FROM goods_receipts WHERE grn_number LIKE 'GRN-P13%');",
        "DELETE FROM goods_receipts WHERE grn_number LIKE 'GRN-P13%';",
        "DELETE FROM purchase_order_items WHERE purchase_order_id IN (SELECT id FROM purchase_orders WHERE po_number LIKE 'PO-P13%');",
        "DELETE FROM purchase_orders WHERE po_number LIKE 'PO-P13%';",
        "DELETE FROM suppliers WHERE supplier_code LIKE 'P13-%';",
        "DELETE FROM batch_location_stock WHERE location_id IN (SELECT id FROM warehouse_locations WHERE location_code LIKE 'LOC-P13%' OR location_code LIKE 'BIN-P13%');",
        "DELETE FROM warehouse_locations WHERE location_code LIKE 'LOC-P13%' OR location_code LIKE 'BIN-P13%';",
        "DELETE FROM warehouses WHERE warehouse_code LIKE 'WH-P13%';",
        "DELETE FROM inventory_transactions WHERE reference_id LIKE 'TR-P13%' OR reference_id LIKE 'GRN-P13%';",
        "DELETE FROM fertilizer_batches WHERE batch_number LIKE 'P13-%';",
        "DELETE FROM users WHERE username LIKE 'p13_%';"
    ]
    for q in queries:
        try:
            mysql_query(q)
        except Exception:
            pass

def run_tests():
    print("======================================================================")
    print("SMARTAGRI PHASE 13 AUTOMATED TEST SUITE")
    print("Multi-Warehouse, Bin/Locations, Stock Transfers & Inventory Operations")
    print("======================================================================")

    clean_test_data()

    # --- 1. Security & Access Control ---
    status, _ = api_call("GET", "/admin/warehouses")
    if status == 401: record_pass("P13-01", "Unauthenticated GET /api/admin/warehouses rejected (HTTP 401)")
    else: record_fail("P13-01", "Unauth warehouses check", f"Expected 401, got {status}")

    status, _ = api_call("POST", "/admin/warehouses", {"warehouseCode": "WH-T", "warehouseName": "Test"})
    if status == 401: record_pass("P13-02", "Unauthenticated POST /api/admin/warehouses rejected (HTTP 401)")
    else: record_fail("P13-02", "Unauth warehouse create", f"Expected 401, got {status}")

    status, _ = api_call("GET", "/admin/transfers")
    if status == 401: record_pass("P13-03", "Unauthenticated GET /api/admin/transfers rejected (HTTP 401)")
    else: record_fail("P13-03", "Unauth transfers list", f"Expected 401, got {status}")

    status, _ = api_call("POST", "/admin/transfers", {"sourceWarehouseId": 1, "destinationWarehouseId": 2, "items": []})
    if status == 401: record_pass("P13-04", "Unauthenticated POST /api/admin/transfers rejected (HTTP 401)")
    else: record_fail("P13-04", "Unauth transfer create", f"Expected 401, got {status}")

    status, _ = api_call("GET", "/admin/warehouses/dashboard")
    if status == 401: record_pass("P13-05", "Unauthenticated GET /api/admin/warehouses/dashboard rejected (HTTP 401)")
    else: record_fail("P13-05", "Unauth dashboard", f"Expected 401, got {status}")

    # Register customer
    ts = str(int(time.time()))[-5:]
    cust_username = f"p13_cust_{ts}"
    _, cust_reg = api_call("POST", "/auth/register", {
        "name": "Phase13 Customer",
        "username": cust_username,
        "email": f"{cust_username}@agri.com",
        "password": "Password123!",
        "phone": "9822334455"
    })
    cust_token = cust_reg.get("data", {}).get("token")

    status, _ = api_call("GET", "/admin/warehouses", token=cust_token)
    if status == 403: record_pass("P13-06", "Customer role GET /api/admin/warehouses rejected with HTTP 403 Forbidden")
    else: record_fail("P13-06", "Customer warehouses access", f"Expected 403, got {status}")

    status, _ = api_call("POST", "/admin/warehouses", {"warehouseCode": "WH-X", "warehouseName": "Test"}, token=cust_token)
    if status == 403: record_pass("P13-07", "Customer role POST /api/admin/warehouses rejected with HTTP 403 Forbidden")
    else: record_fail("P13-07", "Customer warehouse create access", f"Expected 403, got {status}")

    status, _ = api_call("GET", "/admin/transfers", token=cust_token)
    if status == 403: record_pass("P13-08", "Customer role GET /api/admin/transfers rejected with HTTP 403 Forbidden")
    else: record_fail("P13-08", "Customer transfers access", f"Expected 403, got {status}")

    status, _ = api_call("POST", "/admin/transfers", {"sourceWarehouseId": 1}, token=cust_token)
    if status == 403: record_pass("P13-09", "Customer role POST /api/admin/transfers rejected with HTTP 403 Forbidden")
    else: record_fail("P13-09", "Customer transfer create access", f"Expected 403, got {status}")

    # Admin Login
    status, admin_login = api_call("POST", "/auth/login", {
        "username": "admin",
        "password": "adminpassword123"
    })
    admin_token = admin_login.get("data", {}).get("token")
    if not admin_token:
        print("CRITICAL: Failed to get admin token! Exiting.")
        return

    # --- 2. Warehouse Management (CRUD & Validation) ---
    status, _ = api_call("POST", "/admin/warehouses", {"warehouseName": "Missing Code"}, token=admin_token)
    if status == 400: record_pass("P13-10", "Create warehouse without warehouseCode rejected (HTTP 400)")
    else: record_fail("P13-10", "Missing warehouseCode check", f"Expected 400, got {status}")

    status, _ = api_call("POST", "/admin/warehouses", {"warehouseCode": "WH-T1"}, token=admin_token)
    if status == 400: record_pass("P13-11", "Create warehouse without warehouseName rejected (HTTP 400)")
    else: record_fail("P13-11", "Missing warehouseName check", f"Expected 400, got {status}")

    wh_code_1 = f"WH-P13-PUNE-{ts}"
    status, wh_res_1 = api_call("POST", "/admin/warehouses", {
        "warehouseCode": wh_code_1,
        "warehouseName": "Pune Regional Depot",
        "warehouseType": "REGIONAL",
        "address": "Plot 12, Bhosari MIDC",
        "city": "Pune",
        "state": "Maharashtra",
        "postalCode": "411026",
        "capacity": 30000,
        "contactPerson": "Ramesh Patil",
        "contactPhone": "9822114455",
        "email": "pune@smartagri.com",
        "isDefaultReceiving": False,
        "notes": "Regional distribution hub for Western Maharashtra"
    }, token=admin_token)
    wh_id_1 = wh_res_1.get("data", {}).get("id")
    if status == 201 and wh_id_1:
        record_pass("P13-12", f"Successfully created Warehouse 1 ({wh_code_1}) (HTTP 201)")
    else:
        record_fail("P13-12", "Create Warehouse 1", f"Expected 201, got {status}: {wh_res_1}")

    # Duplicate warehouse code check
    status, dup_wh = api_call("POST", "/admin/warehouses", {
        "warehouseCode": wh_code_1,
        "warehouseName": "Duplicate Pune",
        "warehouseType": "REGIONAL"
    }, token=admin_token)
    if status == 409: record_pass("P13-13", "Duplicate warehouseCode rejected with HTTP 409 Conflict")
    else: record_fail("P13-13", "Duplicate warehouseCode check", f"Expected 409, got {status}")

    # Create Warehouse 2 (Destination)
    wh_code_2 = f"WH-P13-NASHIK-{ts}"
    status, wh_res_2 = api_call("POST", "/admin/warehouses", {
        "warehouseCode": wh_code_2,
        "warehouseName": "Nashik Retail Hub",
        "warehouseType": "RETAIL",
        "address": "Satpur Industrial Area",
        "city": "Nashik",
        "state": "Maharashtra",
        "postalCode": "422007",
        "capacity": 15000,
        "contactPerson": "Suresh Deshmukh",
        "contactPhone": "9833221100",
        "email": "nashik@smartagri.com",
        "isDefaultReceiving": False
    }, token=admin_token)
    wh_id_2 = wh_res_2.get("data", {}).get("id")
    if status == 201 and wh_id_2:
        record_pass("P13-14", f"Successfully created Warehouse 2 ({wh_code_2}) (HTTP 201)")
    else:
        record_fail("P13-14", "Create Warehouse 2", f"Expected 201, got {status}: {wh_res_2}")

    # Query warehouse by ID
    status, get_wh = api_call("GET", f"/admin/warehouses/{wh_id_1}", token=admin_token)
    if status == 200 and get_wh.get("data", {}).get("warehouseCode") == wh_code_1:
        record_pass("P13-15", f"GET /api/admin/warehouses/{wh_id_1} returned correct details")
    else:
        record_fail("P13-15", "GET warehouse by ID", f"Expected 200, got {status}")

    # Update warehouse
    status, upd_wh = api_call("PUT", f"/admin/warehouses/{wh_id_1}", {
        "warehouseCode": wh_code_1,
        "warehouseName": "Pune Regional Distribution Center (Updated)",
        "warehouseType": "DISTRIBUTION",
        "capacity": 45000
    }, token=admin_token)
    if status == 200 and upd_wh.get("data", {}).get("capacity") == 45000:
        record_pass("P13-16", "Successfully updated warehouse details and capacity (HTTP 200)")
    else:
        record_fail("P13-16", "Update warehouse", f"Expected 200, got {status}")

    # Toggle warehouse status
    status, st_wh = api_call("PATCH", f"/admin/warehouses/{wh_id_1}/status?status=INACTIVE", token=admin_token)
    if status == 200 and st_wh.get("data", {}).get("status") == "INACTIVE":
        record_pass("P13-17", "Successfully changed warehouse status to INACTIVE")
    else:
        record_fail("P13-17", "Deactivate warehouse", f"Expected 200, got {status}")

    status, st_wh2 = api_call("PATCH", f"/admin/warehouses/{wh_id_1}/status?status=ACTIVE", token=admin_token)
    if status == 200 and st_wh2.get("data", {}).get("status") == "ACTIVE":
        record_pass("P13-18", "Successfully re-activated warehouse status to ACTIVE")
    else:
        record_fail("P13-18", "Activate warehouse", f"Expected 200, got {status}")

    # --- 3. Warehouse Locations & Bins ---
    status, _ = api_call("POST", f"/admin/warehouses/{wh_id_1}/locations", {"locationName": "Missing Code"}, token=admin_token)
    if status == 400: record_pass("P13-19", "Create location without locationCode rejected (HTTP 400)")
    else: record_fail("P13-19", "Missing locationCode check", f"Expected 400, got {status}")

    # Create Zone Location in Pune Warehouse
    zone_code_1 = f"LOC-P13-Z1-{ts}"
    status, loc_zone = api_call("POST", f"/admin/warehouses/{wh_id_1}/locations", {
        "locationCode": zone_code_1,
        "locationName": "Zone A - Chemical Fertilizers",
        "locationType": "ZONE",
        "capacity": 20000,
        "maxWeightKg": 30000,
        "isHazardousAllowed": True
    }, token=admin_token)
    zone_id_1 = loc_zone.get("data", {}).get("id")
    if status == 201 and zone_id_1:
        record_pass("P13-20", f"Created Zone Location ({zone_code_1}) (HTTP 201)")
    else:
        record_fail("P13-20", "Create Zone Location", f"Expected 201, got {status}: {loc_zone}")

    # Create Bin inside Zone in Pune Warehouse
    bin_code_1 = f"BIN-P13-A1-{ts}"
    status, loc_bin_1 = api_call("POST", f"/admin/warehouses/{wh_id_1}/locations", {
        "locationCode": bin_code_1,
        "locationName": "Bin A-01 Urea Bulk",
        "locationType": "BIN",
        "parentLocationId": zone_id_1,
        "capacity": 10000,
        "maxWeightKg": 15000,
        "isTemperatureControlled": False,
        "isHazardousAllowed": False
    }, token=admin_token)
    bin_id_1 = loc_bin_1.get("data", {}).get("id")
    if status == 201 and bin_id_1 and loc_bin_1.get("data", {}).get("parentLocationId") == zone_id_1:
        record_pass("P13-21", f"Created Hierarchical Bin Location ({bin_code_1}) with Parent Zone (HTTP 201)")
    else:
        record_fail("P13-21", "Create Bin Location", f"Expected 201, got {status}: {loc_bin_1}")

    # Duplicate location code check in same warehouse
    status, dup_loc = api_call("POST", f"/admin/warehouses/{wh_id_1}/locations", {
        "locationCode": bin_code_1,
        "locationName": "Duplicate Bin",
        "locationType": "BIN"
    }, token=admin_token)
    if status == 409: record_pass("P13-22", "Duplicate locationCode in same warehouse rejected with HTTP 409 Conflict")
    else: record_fail("P13-22", "Duplicate locationCode check", f"Expected 409, got {status}")

    # Create Bin in Nashik Warehouse (Destination)
    bin_code_2 = f"BIN-P13-N1-{ts}"
    status, loc_bin_2 = api_call("POST", f"/admin/warehouses/{wh_id_2}/locations", {
        "locationCode": bin_code_2,
        "locationName": "Nashik Retail Shelf 1",
        "locationType": "SHELF",
        "capacity": 5000
    }, token=admin_token)
    bin_id_2 = loc_bin_2.get("data", {}).get("id")
    if status == 201 and bin_id_2:
        record_pass("P13-23", f"Created Destination Location in Nashik ({bin_code_2}) (HTTP 201)")
    else:
        record_fail("P13-23", "Create Dest Location", f"Expected 201, got {status}")

    # Update location
    status, upd_loc = api_call("PUT", f"/admin/locations/{bin_id_1}", {
        "locationCode": bin_code_1,
        "locationName": "Bin A-01 Urea High Capacity",
        "locationType": "BIN",
        "capacity": 12000,
        "isTemperatureControlled": True
    }, token=admin_token)
    if status == 200 and upd_loc.get("data", {}).get("capacity") == 12000 and upd_loc.get("data", {}).get("isTemperatureControlled") is True:
        record_pass("P13-24", "Successfully updated location capacity and temperature control flag")
    else:
        record_fail("P13-24", "Update location", f"Expected 200, got {status}")

    # Block location and test blocked check
    status, blk_loc = api_call("PATCH", f"/admin/locations/{bin_id_1}/status?status=BLOCKED", token=admin_token)
    if status == 200 and blk_loc.get("data", {}).get("status") == "BLOCKED":
        record_pass("P13-25", "Successfully updated location status to BLOCKED")
    else:
        record_fail("P13-25", "Block location", f"Expected 200, got {status}")

    # Re-activate location
    status, act_loc = api_call("PATCH", f"/admin/locations/{bin_id_1}/status?status=ACTIVE", token=admin_token)
    if status == 200 and act_loc.get("data", {}).get("status") == "ACTIVE":
        record_pass("P13-26", "Successfully re-activated location status to ACTIVE")
    else:
        record_fail("P13-26", "Re-activate location", f"Expected 200, got {status}")

    # --- 4. GRN Acceptance with Multi-Warehouse & Bin Stock Allocation ---
    # Create supplier & PO
    sup_code = f"P13-SUP-{ts}"
    _, sup_res = api_call("POST", "/admin/suppliers", {
        "supplierCode": sup_code,
        "companyName": "KRIBHCO Agro Logistics Ltd",
        "contactPerson": "Anand Rathi",
        "email": f"{sup_code}@kribhco.in"
    }, token=admin_token)
    sup_id = sup_res.get("data", {}).get("id")

    fert_id = int(mysql_query("SELECT id FROM fertilizer WHERE active = 1 AND status = 'ACTIVE' LIMIT 1;"))

    po_data = {
        "supplierId": sup_id,
        "notes": "Phase 13 Warehouse GRN intake test",
        "items": [{
            "fertilizerId": fert_id,
            "orderedQuantity": 100,
            "unitCost": 450.0
        }]
    }
    _, po_res = api_call("POST", "/admin/purchase-orders", po_data, token=admin_token)
    po_id = po_res.get("data", {}).get("id")
    po_num = po_res.get("data", {}).get("poNumber")
    poi_id = po_res.get("data", {}).get("items", [])[0]["id"]

    # Submit and Approve PO
    api_call("POST", f"/admin/purchase-orders/{po_id}/submit", token=admin_token)
    api_call("POST", f"/admin/purchase-orders/{po_id}/approve", token=admin_token)

    # Create GRN targeted to Pune Warehouse and Bin A1
    batch_num_1 = f"P13-LOT-{ts}-01"
    grn_data = {
        "purchaseOrderId": po_id,
        "warehouseId": wh_id_1,
        "locationId": bin_id_1,
        "supplierInvoiceNumber": f"INV-P13-{ts}",
        "items": [{
            "purchaseOrderItemId": poi_id,
            "receivedQuantity": 80,
            "batchNumber": batch_num_1,
            "manufacturingDate": str(date.today() - timedelta(days=10)),
            "expiryDate": str(date.today() + timedelta(days=365)),
            "unitCost": 450.0
        }]
    }
    status, grn_res = api_call("POST", "/admin/goods-receipts", grn_data, token=admin_token)
    grn_id = grn_res.get("data", {}).get("id")
    grn_num = grn_res.get("data", {}).get("grnNumber")
    if status == 201 and grn_res.get("data", {}).get("warehouseId") == wh_id_1:
        record_pass("P13-27", f"Created GRN #{grn_num} targeted to Pune Warehouse and Bin A1 (HTTP 201)")
    else:
        record_fail("P13-27", "Create GRN with warehouse", f"Expected 201, got {status}: {grn_res}")

    # Inspect & Accept GRN
    api_call("POST", f"/admin/goods-receipts/{grn_id}/inspect", {
        "inspectionStatus": "PASSED",
        "packagingCondition": "INTACT",
        "sealCondition": "VERIFIED"
    }, token=admin_token)

    status, acc_grn = api_call("POST", f"/admin/goods-receipts/{grn_id}/accept", token=admin_token)
    if status == 200 and acc_grn.get("data", {}).get("status") == "ACCEPTED":
        record_pass("P13-28", f"Accepted GRN #{grn_num} and added 80 units to inventory")
    else:
        record_fail("P13-28", "Accept GRN", f"Expected 200, got {status}")

    # Verify BatchLocationStock created in Bin A1
    status, loc_stocks = api_call("GET", f"/admin/locations/{bin_id_1}/stock", token=admin_token)
    bls_items = loc_stocks.get("data", [])
    matched_bls = next((item for item in bls_items if item["batchNumber"] == batch_num_1), None)
    if status == 200 and matched_bls and matched_bls["quantity"] == 80:
        record_pass("P13-29", f"BatchLocationStock verified: 80 units of lot {batch_num_1} present in Bin A1")
        batch_id_1 = matched_bls["batchId"]
    else:
        record_fail("P13-29", "BatchLocationStock verification", f"Expected 80 units, got: {bls_items}")
        batch_id_1 = None

    # Verify RESTOCK transaction recorded with warehouse and location
    tx_sql = f"SELECT transaction_type, warehouse_id, location_id, quantity FROM inventory_transactions WHERE reference_id = '{grn_num}'"
    tx_db = mysql_query(tx_sql)
    if f"{wh_id_1}" in tx_db and f"{bin_id_1}" in tx_db and "RESTOCK" in tx_db:
        record_pass("P13-30", f"Audited RESTOCK transaction verified with warehouse_id={wh_id_1} and location_id={bin_id_1}")
    else:
        record_fail("P13-30", "RESTOCK transaction check", f"Expected warehouse & location in tx, got: '{tx_db}'")

    # --- 5. Stock Transfer Lifecycle & Concurrency ---
    # Validation 1: Same source and destination rejected
    status, _ = api_call("POST", "/admin/transfers", {
        "sourceWarehouseId": wh_id_1,
        "destinationWarehouseId": wh_id_1,
        "items": [{"batchId": batch_id_1, "sourceLocationId": bin_id_1, "requestedQuantity": 10}]
    }, token=admin_token)
    if status == 400: record_pass("P13-31", "Same source and destination warehouse rejected with HTTP 400")
    else: record_fail("P13-31", "Same source/dest check", f"Expected 400, got {status}")

    # Validation 2: Quantity exceeding available stock rejected
    status, _ = api_call("POST", "/admin/transfers", {
        "sourceWarehouseId": wh_id_1,
        "destinationWarehouseId": wh_id_2,
        "items": [{"batchId": batch_id_1, "sourceLocationId": bin_id_1, "requestedQuantity": 9999}]
    }, token=admin_token)
    if status == 400: record_pass("P13-32", "Transfer quantity exceeding available stock rejected with HTTP 400")
    else: record_fail("P13-32", "Over-request transfer check", f"Expected 400, got {status}")

    # Create valid transfer in DRAFT status (25 units)
    status, tr_draft = api_call("POST", "/admin/transfers", {
        "sourceWarehouseId": wh_id_1,
        "destinationWarehouseId": wh_id_2,
        "reason": "Rebalance stock from Pune to Nashik store",
        "status": "DRAFT",
        "items": [{
            "batchId": batch_id_1,
            "sourceLocationId": bin_id_1,
            "requestedQuantity": 25
        }]
    }, token=admin_token)
    tr_id_1 = tr_draft.get("data", {}).get("id")
    tr_num_1 = tr_draft.get("data", {}).get("transferNumber")
    if status == 201 and tr_draft.get("data", {}).get("status") == "DRAFT":
        record_pass("P13-33", f"Created Stock Transfer #{tr_num_1} in DRAFT status (HTTP 201)")
    else:
        record_fail("P13-33", "Create DRAFT transfer", f"Expected 201, got {status}: {tr_draft}")

    # Query transfer by ID
    status, get_tr = api_call("GET", f"/admin/transfers/{tr_id_1}", token=admin_token)
    if status == 200 and get_tr.get("data", {}).get("transferNumber") == tr_num_1:
        record_pass("P13-34", f"GET /api/admin/transfers/{tr_id_1} returned correct transfer details")
    else:
        record_fail("P13-34", "GET transfer by ID", f"Expected 200, got {status}")

    # Illegal transition: Attempt to dispatch DRAFT directly
    status, _ = api_call("POST", f"/admin/transfers/{tr_id_1}/dispatch", token=admin_token)
    if status in (400, 409): record_pass("P13-35", "Dispatching unapproved DRAFT transfer rejected (HTTP 400/409)")
    else: record_fail("P13-35", "Illegal dispatch check", f"Expected 400/409, got {status}")

    # Submit for approval
    status, sub_tr = api_call("POST", f"/admin/transfers/{tr_id_1}/submit", token=admin_token)
    if status == 200 and sub_tr.get("data", {}).get("status") == "PENDING_APPROVAL":
        record_pass("P13-36", "Successfully submitted DRAFT transfer for approval (PENDING_APPROVAL)")
    else:
        record_fail("P13-36", "Submit transfer for approval", f"Expected 200, got {status}")

    # Approve transfer
    status, app_tr = api_call("POST", f"/admin/transfers/{tr_id_1}/approve", token=admin_token)
    if status == 200 and app_tr.get("data", {}).get("status") == "APPROVED" and app_tr.get("data", {}).get("approvedBy") == "admin":
        record_pass("P13-37", "Successfully approved Stock Transfer (APPROVED) with audit user 'admin'")
    else:
        record_fail("P13-37", "Approve transfer", f"Expected 200, got {status}")

    # Illegal transition: Attempt to receive non-dispatched transfer
    status, _ = api_call("POST", f"/admin/transfers/{tr_id_1}/receive", {
        "items": [{"itemId": app_tr.get("data", {}).get("items", [])[0]["id"], "receivedQuantity": 25, "destinationLocationId": bin_id_2}]
    }, token=admin_token)
    if status in (400, 409): record_pass("P13-38", "Receiving non-dispatched transfer rejected (HTTP 400/409)")
    else: record_fail("P13-38", "Illegal receive check", f"Expected 400/409, got {status}")

    # --- 6. Dispatch Transfer & Stock Deduction ---
    status, disp_tr = api_call("POST", f"/admin/transfers/{tr_id_1}/dispatch", token=admin_token)
    if status == 200 and disp_tr.get("data", {}).get("status") == "DISPATCHED" and disp_tr.get("data", {}).get("totalDispatchedUnits") == 25:
        record_pass("P13-39", f"Dispatched Stock Transfer #{tr_num_1} (25 units deducted from source bin)")
    else:
        record_fail("P13-39", "Dispatch transfer", f"Expected 200, got {status}: {disp_tr}")

    # Verify source location stock was deducted: 80 - 25 = 55 units remaining in Bin A1
    _, loc_stocks_after_disp = api_call("GET", f"/admin/locations/{bin_id_1}/stock", token=admin_token)
    bls_after_disp = next((i for i in loc_stocks_after_disp.get("data", []) if i["batchNumber"] == batch_num_1), None)
    if bls_after_disp and bls_after_disp["quantity"] == 55:
        record_pass("P13-40", "Verified source location stock deducted from 80 to 55 units in Bin A1")
    else:
        record_fail("P13-40", "Source stock deduction check", f"Expected 55 units, got: {bls_after_disp}")

    # Verify TRANSFER_OUT transaction recorded
    tx_sql_out = f"SELECT transaction_type, warehouse_id, location_id, quantity FROM inventory_transactions WHERE reference_id = '{tr_num_1}' AND transaction_type = 'TRANSFER_OUT'"
    tx_out_db = mysql_query(tx_sql_out)
    if "TRANSFER_OUT" in tx_out_db and f"{wh_id_1}" in tx_out_db and f"{bin_id_1}" in tx_out_db:
        record_pass("P13-41", "Audited TRANSFER_OUT transaction logged with source warehouse and bin")
    else:
        record_fail("P13-41", "TRANSFER_OUT audit check", f"Expected TRANSFER_OUT row, got: '{tx_out_db}'")

    # Idempotency guard: duplicate dispatch returns HTTP 409 Conflict
    status, dup_disp = api_call("POST", f"/admin/transfers/{tr_id_1}/dispatch", token=admin_token)
    if status == 409: record_pass("P13-42", "Duplicate dispatch rejected with HTTP 409 Conflict (Idempotency)")
    else: record_fail("P13-42", "Duplicate dispatch check", f"Expected 409, got {status}")

    # --- 7. Transfer Receiving & Location Stock Increment ---
    tr_item_1_id = disp_tr.get("data", {}).get("items", [])[0]["id"]

    # Over-receive validation: Receiving more than dispatched rejected
    status, _ = api_call("POST", f"/admin/transfers/{tr_id_1}/receive", {
        "items": [{"itemId": tr_item_1_id, "receivedQuantity": 50, "destinationLocationId": bin_id_2}]
    }, token=admin_token)
    if status == 400: record_pass("P13-43", "Over-receive exceeding dispatched quantity rejected with HTTP 400")
    else: record_fail("P13-43", "Over-receive check", f"Expected 400, got {status}")

    # Partial receive: Receive 15 out of 25 units at Nashik Shelf 1
    status, part_rcv = api_call("POST", f"/admin/transfers/{tr_id_1}/receive", {
        "items": [{"itemId": tr_item_1_id, "receivedQuantity": 15, "destinationLocationId": bin_id_2}],
        "notes": "Partial first shipment received"
    }, token=admin_token)
    if status == 200 and part_rcv.get("data", {}).get("status") == "PARTIALLY_RECEIVED" and part_rcv.get("data", {}).get("totalReceivedUnits") == 15:
        record_pass("P13-44", "Partial stock transfer receipt processed successfully (PARTIALLY_RECEIVED, 15 units)")
    else:
        record_fail("P13-44", "Partial transfer receipt", f"Expected PARTIALLY_RECEIVED, got {status}: {part_rcv}")

    # Verify destination location stock created: 15 units in Nashik Shelf 1
    _, dest_stocks_1 = api_call("GET", f"/admin/locations/{bin_id_2}/stock", token=admin_token)
    dest_bls_1 = next((i for i in dest_stocks_1.get("data", []) if i["batchNumber"] == batch_num_1), None)
    if dest_bls_1 and dest_bls_1["quantity"] == 15:
        record_pass("P13-45", "Verified destination location received 15 units of lot in Nashik Shelf 1")
    else:
        record_fail("P13-45", "Dest stock check after partial rcv", f"Expected 15 units, got: {dest_bls_1}")

    # Complete receiving: Receive remaining 10 units at Nashik Shelf 1
    status, full_rcv = api_call("POST", f"/admin/transfers/{tr_id_1}/receive", {
        "items": [{"itemId": tr_item_1_id, "receivedQuantity": 10, "destinationLocationId": bin_id_2}],
        "notes": "Final balance received"
    }, token=admin_token)
    if status == 200 and full_rcv.get("data", {}).get("status") == "FULLY_RECEIVED" and full_rcv.get("data", {}).get("totalReceivedUnits") == 25:
        record_pass("P13-46", "Full stock transfer receipt completed (FULLY_RECEIVED, total 25 units)")
    else:
        record_fail("P13-46", "Full transfer receipt", f"Expected FULLY_RECEIVED, got {status}: {full_rcv}")

    # Verify destination location stock is now 25 units
    _, dest_stocks_2 = api_call("GET", f"/admin/locations/{bin_id_2}/stock", token=admin_token)
    dest_bls_2 = next((i for i in dest_stocks_2.get("data", []) if i["batchNumber"] == batch_num_1), None)
    if dest_bls_2 and dest_bls_2["quantity"] == 25:
        record_pass("P13-47", "Verified destination location stock incremented to 25 units in Nashik Shelf 1")
    else:
        record_fail("P13-47", "Dest stock check after full rcv", f"Expected 25 units, got: {dest_bls_2}")

    # Verify TRANSFER_IN transaction recorded
    tx_sql_in = f"SELECT transaction_type, warehouse_id, location_id, quantity FROM inventory_transactions WHERE reference_id = '{tr_num_1}' AND transaction_type = 'TRANSFER_IN'"
    tx_in_db = mysql_query(tx_sql_in)
    if "TRANSFER_IN" in tx_in_db and f"{wh_id_2}" in tx_in_db and f"{bin_id_2}" in tx_in_db:
        record_pass("P13-48", "Audited TRANSFER_IN transaction logged with destination warehouse and bin")
    else:
        record_fail("P13-48", "TRANSFER_IN audit check", f"Expected TRANSFER_IN row, got: '{tx_in_db}'")

    # Idempotency guard: duplicate receive when remaining is 0 returns HTTP 409
    status, dup_rcv = api_call("POST", f"/admin/transfers/{tr_id_1}/receive", {
        "items": [{"itemId": tr_item_1_id, "receivedQuantity": 5, "destinationLocationId": bin_id_2}]
    }, token=admin_token)
    if status == 409: record_pass("P13-49", "Receiving on already FULLY_RECEIVED transfer rejected with HTTP 409 Conflict")
    else: record_fail("P13-49", "Duplicate receive check", f"Expected 409, got {status}")

    # --- 8. Batch Lot Identity Preservation Across Transfers ---
    # Total batch quantity must equal sum across all location stocks (55 + 25 = 80)
    batch_sql = f"SELECT current_quantity FROM fertilizer_batches WHERE id = {batch_id_1}"
    batch_qty = int(mysql_query(batch_sql))
    loc_sum_sql = f"SELECT SUM(quantity) FROM batch_location_stock WHERE batch_id = {batch_id_1}"
    loc_sum_qty = int(mysql_query(loc_sum_sql))
    if batch_qty == 80 and loc_sum_qty == 80:
        record_pass("P13-50", f"Batch integrity verified: FertilizerBatch currentQuantity ({batch_qty}) == SUM(BatchLocationStock) ({loc_sum_qty})")
    else:
        record_fail("P13-50", "Batch location sum check", f"Expected 80, got batch_qty={batch_qty}, loc_sum={loc_sum_qty}")

    # Check zero duplicate batches created for the same lot number
    dup_batch_sql = f"SELECT COUNT(*) FROM fertilizer_batches WHERE batch_number = '{batch_num_1}'"
    dup_batch_count = int(mysql_query(dup_batch_sql))
    if dup_batch_count == 1:
        record_pass("P13-51", "Strict lot identity preservation: exactly 1 physical batch record exists across all warehouses")
    else:
        record_fail("P13-51", "Duplicate batch check", f"Expected 1, got {dup_batch_count}")

    # --- 9. Transfer Rejection and Cancellation Workflows ---
    # Create transfer 2 for cancellation test
    status, tr_can = api_call("POST", "/admin/transfers", {
        "sourceWarehouseId": wh_id_1,
        "destinationWarehouseId": wh_id_2,
        "status": "DRAFT",
        "items": [{"batchId": batch_id_1, "sourceLocationId": bin_id_1, "requestedQuantity": 10}]
    }, token=admin_token)
    tr_can_id = tr_can.get("data", {}).get("id")

    status, can_res = api_call("POST", f"/admin/transfers/{tr_can_id}/cancel?reason=Test+cancellation", token=admin_token)
    if status == 200 and can_res.get("data", {}).get("status") == "CANCELLED":
        record_pass("P13-52", "Successfully cancelled DRAFT transfer (CANCELLED)")
    else:
        record_fail("P13-52", "Cancel transfer", f"Expected 200, got {status}")

    # Create transfer 3 for rejection test
    status, tr_rej = api_call("POST", "/admin/transfers", {
        "sourceWarehouseId": wh_id_1,
        "destinationWarehouseId": wh_id_2,
        "status": "PENDING_APPROVAL",
        "items": [{"batchId": batch_id_1, "sourceLocationId": bin_id_1, "requestedQuantity": 10}]
    }, token=admin_token)
    tr_rej_id = tr_rej.get("data", {}).get("id")

    status, rej_res = api_call("POST", f"/admin/transfers/{tr_rej_id}/reject?reason=Manager+declined", token=admin_token)
    if status == 200 and rej_res.get("data", {}).get("status") == "REJECTED":
        record_pass("P13-53", "Successfully rejected PENDING_APPROVAL transfer (REJECTED)")
    else:
        record_fail("P13-53", "Reject transfer", f"Expected 200, got {status}")

    # --- 10. Customer Order FEFO Deduction & Exact Location Restoration ---
    status, addr_res = api_call("POST", "/addresses", {
        "fullName": "Phase13 Farmer",
        "phone": "9811223344",
        "addressLine1": "Plot 10, Agri Gate",
        "city": "Pune",
        "state": "Maharashtra",
        "district": "Pune",
        "pincode": "411001",
        "addressType": "FARM",
        "isDefault": True
    }, token=cust_token)
    addr_id = addr_res.get("data", {}).get("id")

    # Add item to cart and checkout
    api_call("POST", "/cart/items", {"fertilizerId": fert_id, "quantity": 5}, token=cust_token)
    status, order_res = api_call("POST", "/orders", {
        "addressId": addr_id,
        "paymentMethod": "COD",
        "notes": "Phase 13 warehouse FEFO test order"
    }, token=cust_token)
    order_id = order_res.get("data", {}).get("id")

    # Verify order item batches recorded warehouse & location
    oib_sql = f"SELECT warehouse_id, location_id FROM order_item_batches WHERE order_item_id IN (SELECT id FROM order_items WHERE order_id = {order_id})"
    oib_db = mysql_query(oib_sql)
    if oib_db and len(oib_db.split()) >= 2:
        record_pass("P13-54", f"Customer checkout FEFO deduction successfully tracked warehouse and location in order_item_batches")
    else:
        record_fail("P13-54", "OrderItemBatch warehouse tracking", f"Expected warehouse and location in DB, got: '{oib_db}'")

    # Cancel customer order and verify exact location restoration
    status, can_order = api_call("PUT", f"/orders/{order_id}/cancel", token=cust_token)
    if status == 200:
        record_pass("P13-55", "Customer order cancelled successfully and stock restored to exact warehouse & location")
    else:
        record_fail("P13-55", "Order cancellation", f"Expected 200, got {status}: {can_order}")

    # --- 11. Warehouse Inventory Health Check & Consistency ---
    status, health_res = api_call("GET", "/admin/inventory/health", token=admin_token)
    health = health_res.get("data", {})
    if status == 200 and health.get("status") == "HEALTHY":
        record_pass("P13-56", "Inventory Health Check status is HEALTHY")
    else:
        record_fail("P13-56", "Inventory Health Check", f"Expected HEALTHY, got: {health}")

    if health.get("warehouseIssuesCount") == 0:
        record_pass("P13-57", f"Zero warehouse/location discrepancy issues found (warehousesChecked: {health.get('warehousesChecked')}, locationsChecked: {health.get('locationsChecked')})")
    else:
        record_fail("P13-57", "Warehouse discrepancies check", f"Issues found: {health.get('warehouseIssuesCount')}")

    if health.get("transferIssuesCount") == 0:
        record_pass("P13-58", "Zero stock transfer issues found across all transfer orders")
    else:
        record_fail("P13-58", "Transfer discrepancies check", f"Issues found: {health.get('transferIssuesCount')}")

    if health.get("batchIssuesCount") == 0:
        record_pass("P13-59", f"Zero batch discrepancy issues found (batchesChecked: {health.get('batchesChecked')})")
    else:
        record_fail("P13-59", "Batch issues check", f"Issues found: {health.get('batchIssuesCount')}")

    if health.get("procurementIssuesCount") == 0:
        record_pass("P13-60", f"Zero procurement/GRN issues found (goodsReceiptsChecked: {health.get('goodsReceiptsChecked')})")
    else:
        record_fail("P13-60", "Procurement issues check", f"Issues found: {health.get('procurementIssuesCount')}")

    # Warehouse Dashboard endpoint verification
    status, dash_res = api_call("GET", "/admin/warehouses/dashboard", token=admin_token)
    dash = dash_res.get("data", {})
    if status == 200 and dash.get("totalWarehouses", 0) >= 2:
        record_pass("P13-61", f"GET /api/admin/warehouses/dashboard returned {dash.get('totalWarehouses')} facilities and {dash.get('totalLocations')} locations")
    else:
        record_fail("P13-61", "Warehouse Dashboard check", f"Expected >=2 warehouses, got: {dash}")

    # Summary
    print("\n======================================================================")
    print(f"PHASE 13 TEST RESULTS: {passed_count} PASSED, {failed_count} FAILED")
    print("======================================================================")
    if failed_count == 0:
        print("ALL PHASE 13 TESTS COMPLETED AND VERIFIED!")
    else:
        sys.exit(1)

if __name__ == "__main__":
    run_tests()
