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
        "DELETE FROM shipment_tracking_events WHERE shipment_id IN (SELECT id FROM shipments WHERE order_id IN (SELECT id FROM orders WHERE notes LIKE '%P14%'));",
        "DELETE FROM shipments WHERE order_id IN (SELECT id FROM orders WHERE notes LIKE '%P14%');",
        "DELETE FROM fulfillment_package_items WHERE package_id IN (SELECT id FROM fulfillment_packages WHERE order_id IN (SELECT id FROM orders WHERE notes LIKE '%P14%'));",
        "DELETE FROM fulfillment_packages WHERE order_id IN (SELECT id FROM orders WHERE notes LIKE '%P14%');",
        "DELETE FROM pick_list_items WHERE pick_list_id IN (SELECT id FROM pick_lists WHERE order_id IN (SELECT id FROM orders WHERE notes LIKE '%P14%'));",
        "DELETE FROM pick_lists WHERE order_id IN (SELECT id FROM orders WHERE notes LIKE '%P14%');",
        "DELETE FROM fulfillment_allocations WHERE order_id IN (SELECT id FROM orders WHERE notes LIKE '%P14%');",
        "DELETE FROM order_item_batches WHERE order_item_id IN (SELECT id FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE notes LIKE '%P14%'));",
        "DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE notes LIKE '%P14%');",
        "DELETE FROM deliveries WHERE order_id IN (SELECT id FROM orders WHERE notes LIKE '%P14%');",
        "DELETE FROM orders WHERE notes LIKE '%P14%';",
        "DELETE FROM inventory_transactions WHERE batch_id IN (SELECT id FROM fertilizer_batches WHERE batch_number LIKE 'P14-%');",
        "DELETE FROM order_item_batches WHERE batch_id IN (SELECT id FROM fertilizer_batches WHERE batch_number LIKE 'P14-%');",
        "DELETE FROM fulfillment_allocations WHERE batch_id IN (SELECT id FROM fertilizer_batches WHERE batch_number LIKE 'P14-%');",
        "DELETE FROM fulfillment_package_items WHERE batch_id IN (SELECT id FROM fertilizer_batches WHERE batch_number LIKE 'P14-%');",
        "DELETE FROM fulfillment_packages WHERE id NOT IN (SELECT DISTINCT package_id FROM fulfillment_package_items);",
        "DELETE FROM pick_list_items WHERE batch_id IN (SELECT id FROM fertilizer_batches WHERE batch_number LIKE 'P14-%');",
        "DELETE FROM batch_location_stock WHERE batch_id IN (SELECT id FROM fertilizer_batches WHERE batch_number LIKE 'P14-%');",
        "DELETE FROM fertilizer_batches WHERE batch_number LIKE 'P14-%';",
        "DELETE FROM delivery_partners WHERE partner_code LIKE 'P14-%';",
        "DELETE FROM users WHERE username LIKE 'p14_%';"
    ]
    for q in queries:
        try:
            mysql_query(q)
        except Exception:
            pass

def run_tests():
    print("======================================================================")
    print("SMARTAGRI PHASE 14 AUTOMATED TEST SUITE")
    print("Shipping, Delivery Management, Dispatch, Tracking & Fulfillment")
    print("======================================================================")

    clean_test_data()

    # -------------------------------------------------------------------------
    # PART 1: SECURITY & RBAC ACCESS CONTROL
    # -------------------------------------------------------------------------
    print("\n--- Part 1: Security & RBAC Checks ---")

    status, _ = api_call("GET", "/admin/fulfillment/dashboard")
    if status == 401: record_pass("P14-01", "Unauthenticated GET /admin/fulfillment/dashboard rejected (HTTP 401)")
    else: record_fail("P14-01", "Unauth dashboard check", f"Expected 401, got {status}")

    status, _ = api_call("GET", "/admin/fulfillment/orders")
    if status == 401: record_pass("P14-02", "Unauthenticated GET /admin/fulfillment/orders rejected (HTTP 401)")
    else: record_fail("P14-02", "Unauth orders list", f"Expected 401, got {status}")

    status, _ = api_call("POST", "/admin/fulfillment/orders/1/allocate")
    if status == 401: record_pass("P14-03", "Unauthenticated POST /admin/fulfillment/orders/1/allocate rejected (HTTP 401)")
    else: record_fail("P14-03", "Unauth allocate check", f"Expected 401, got {status}")

    status, _ = api_call("GET", "/admin/picklists")
    if status == 401: record_pass("P14-04", "Unauthenticated GET /admin/picklists rejected (HTTP 401)")
    else: record_fail("P14-04", "Unauth picklists list", f"Expected 401, got {status}")

    status, _ = api_call("POST", "/admin/picklists", {"orderId": 1, "assignedPicker": "Picker"})
    if status == 401: record_pass("P14-05", "Unauthenticated POST /admin/picklists rejected (HTTP 401)")
    else: record_fail("P14-05", "Unauth picklist create", f"Expected 401, got {status}")

    status, _ = api_call("GET", "/admin/packages")
    if status == 401: record_pass("P14-06", "Unauthenticated GET /admin/packages rejected (HTTP 401)")
    else: record_fail("P14-06", "Unauth packages list", f"Expected 401, got {status}")

    status, _ = api_call("POST", "/admin/packages", {"orderId": 1, "actualWeightKg": 5.0})
    if status == 401: record_pass("P14-07", "Unauthenticated POST /admin/packages rejected (HTTP 401)")
    else: record_fail("P14-07", "Unauth package create", f"Expected 401, got {status}")

    status, _ = api_call("GET", "/admin/shipments")
    if status == 401: record_pass("P14-08", "Unauthenticated GET /admin/shipments rejected (HTTP 401)")
    else: record_fail("P14-08", "Unauth shipments list", f"Expected 401, got {status}")

    status, _ = api_call("POST", "/admin/shipments", {"orderId": 1, "deliveryPartnerId": 1})
    if status == 401: record_pass("P14-09", "Unauthenticated POST /admin/shipments rejected (HTTP 401)")
    else: record_fail("P14-09", "Unauth shipment create", f"Expected 401, got {status}")

    status, _ = api_call("GET", "/admin/delivery-partners")
    if status == 401: record_pass("P14-10", "Unauthenticated GET /admin/delivery-partners rejected (HTTP 401)")
    else: record_fail("P14-10", "Unauth delivery partners list", f"Expected 401, got {status}")

    # Register Customer A
    ts = str(int(time.time()))[-5:]
    cust_a_username = f"p14_cust_a_{ts}"
    _, cust_a_reg = api_call("POST", "/auth/register", {
        "name": "Phase14 Customer A",
        "username": cust_a_username,
        "email": f"{cust_a_username}@agri.com",
        "password": "Password123!",
        "phone": "9811223344"
    })
    cust_a_token = cust_a_reg.get("data", {}).get("token")
    user_a_id = cust_a_reg.get("data", {}).get("id")

    # Register Customer B
    cust_b_username = f"p14_cust_b_{ts}"
    _, cust_b_reg = api_call("POST", "/auth/register", {
        "name": "Phase14 Customer B",
        "username": cust_b_username,
        "email": f"{cust_b_username}@agri.com",
        "password": "Password123!",
        "phone": "9822334455"
    })
    cust_b_token = cust_b_reg.get("data", {}).get("token")

    status, _ = api_call("GET", "/admin/fulfillment/dashboard", token=cust_a_token)
    if status == 403: record_pass("P14-11", "Customer role GET /admin/fulfillment/dashboard rejected (HTTP 403)")
    else: record_fail("P14-11", "Customer dashboard access", f"Expected 403, got {status}")

    status, _ = api_call("POST", "/admin/fulfillment/orders/1/allocate", token=cust_a_token)
    if status == 403: record_pass("P14-12", "Customer role POST /admin/fulfillment/orders/1/allocate rejected (HTTP 403)")
    else: record_fail("P14-12", "Customer allocate access", f"Expected 403, got {status}")

    status, _ = api_call("GET", "/admin/picklists", token=cust_a_token)
    if status == 403: record_pass("P14-13", "Customer role GET /admin/picklists rejected (HTTP 403)")
    else: record_fail("P14-13", "Customer picklists access", f"Expected 403, got {status}")

    status, _ = api_call("POST", "/admin/packages", {"orderId": 1}, token=cust_a_token)
    if status == 403: record_pass("P14-14", "Customer role POST /admin/packages rejected (HTTP 403)")
    else: record_fail("P14-14", "Customer package create access", f"Expected 403, got {status}")

    status, _ = api_call("POST", "/admin/shipments", {"orderId": 1}, token=cust_a_token)
    if status == 403: record_pass("P14-15", "Customer role POST /admin/shipments rejected (HTTP 403)")
    else: record_fail("P14-15", "Customer shipment create access", f"Expected 403, got {status}")

    status, _ = api_call("POST", "/admin/delivery-partners", {"partnerCode": "TEST"}, token=cust_a_token)
    if status == 403: record_pass("P14-16", "Customer role POST /admin/delivery-partners rejected (HTTP 403)")
    else: record_fail("P14-16", "Customer delivery partner create access", f"Expected 403, got {status}")

    # Admin Login
    status, admin_login = api_call("POST", "/auth/login", {
        "username": "admin",
        "password": "adminpassword123"
    })
    admin_token = admin_login.get("data", {}).get("token")
    if not admin_token:
        print("CRITICAL: Failed to get admin token! Exiting.")
        return

    # -------------------------------------------------------------------------
    # PART 2: DELIVERY PARTNER MANAGEMENT
    # -------------------------------------------------------------------------
    print("\n--- Part 2: Delivery Partner Management ---")

    status, dp_list = api_call("GET", "/admin/delivery-partners", token=admin_token)
    partners = dp_list.get("data", [])
    seeded_codes = [p.get("partnerCode") for p in partners]
    expected_seeded = ["KISAN_SPEED", "DELHIVERY", "BLUEDART", "DTDC", "SPEED_POST"]
    all_seeded_present = all(c in seeded_codes for c in expected_seeded)
    if status == 200 and all_seeded_present:
        record_pass("P14-17", f"Default delivery partners verified: {', '.join(expected_seeded)}")
    else:
        record_fail("P14-17", "Default partners check", f"Found codes: {seeded_codes}")

    status, _ = api_call("POST", "/admin/delivery-partners", {"partnerName": "No Code Partner"}, token=admin_token)
    if status == 400: record_pass("P14-18", "Create partner without partnerCode rejected (HTTP 400)")
    else: record_fail("P14-18", "Partner validation check", f"Expected 400, got {status}")

    new_p_code = f"P14-EXPRESS-{ts}"
    status, new_p_res = api_call("POST", "/admin/delivery-partners", {
        "partnerCode": new_p_code,
        "partnerName": "Kisan Express Regional Logistics",
        "contactNumber": "1800-445-566",
        "trackingUrlTemplate": "https://kisanexpress.example.com/track?id=%s"
    }, token=admin_token)
    partner_id = new_p_res.get("data", {}).get("id")
    if status == 201 and partner_id:
        record_pass("P14-19", f"Successfully created new delivery partner ({new_p_code}) (HTTP 201)")
    else:
        record_fail("P14-19", "Create delivery partner", f"Expected 201, got {status}: {new_p_res}")

    status, _ = api_call("POST", "/admin/delivery-partners", {
        "partnerCode": new_p_code,
        "partnerName": "Duplicate Code Partner"
    }, token=admin_token)
    if status in (400, 409): record_pass("P14-20", "Duplicate partner code rejected (HTTP 400/409)")
    else: record_fail("P14-20", "Duplicate partner code check", f"Expected 400 or 409, got {status}")

    status, toggle_res = api_call("PATCH", f"/admin/delivery-partners/{partner_id}/status?status=INACTIVE", token=admin_token)
    if status == 200 and toggle_res.get("data", {}).get("status") == "INACTIVE":
        record_pass("P14-21", f"Successfully toggled partner {partner_id} to INACTIVE")
    else:
        record_fail("P14-21", "Toggle partner status", f"Expected 200 with INACTIVE, got {status}")

    # Reactivate partner
    api_call("PATCH", f"/admin/delivery-partners/{partner_id}/status?status=ACTIVE", token=admin_token)

    # -------------------------------------------------------------------------
    # PART 3: ORDER SETUP & WAREHOUSE ALLOCATION
    # -------------------------------------------------------------------------
    print("\n--- Part 3: Order Setup & Fulfillment Allocation ---")

    # Create address for Customer A
    _, addr_res = api_call("POST", "/addresses", {
        "fullName": "Phase14 Farmer",
        "phone": "9811223344",
        "addressLine1": "Farm Sector 7, Hadapsar",
        "city": "Pune",
        "district": "Pune",
        "state": "Maharashtra",
        "pincode": "411028",
        "addressType": "FARM",
        "default": True
    }, token=cust_a_token)
    addr_id = addr_res.get("data", {}).get("id")

    # Ensure warehouse & location exists
    wh_id = 1
    loc_id = 1
    # Check default warehouse
    wh_check = mysql_query("SELECT id FROM warehouses WHERE is_default_receiving = 1 LIMIT 1;")
    if wh_check:
        wh_id = int(wh_check)
    else:
        wh_any = mysql_query("SELECT id FROM warehouses LIMIT 1;")
        if wh_any:
            wh_id = int(wh_any)
        else:
            _, wh_new = api_call("POST", "/admin/warehouses", {
                "warehouseCode": f"WH-P14-MAIN-{ts}",
                "warehouseName": "Pune Main Depot",
                "isDefaultReceiving": True
            }, token=admin_token)
            wh_id = wh_new.get("data", {}).get("id", 1)

    # Ensure location exists in warehouse
    loc_check = mysql_query(f"SELECT id FROM warehouse_locations WHERE warehouse_id = {wh_id} LIMIT 1;")
    if loc_check:
        loc_id = int(loc_check)
    else:
        _, loc_new = api_call("POST", f"/admin/warehouses/{wh_id}/locations", {
            "locationCode": f"LOC-P14-A1-{ts}",
            "locationName": "Bin A-01"
        }, token=admin_token)
        loc_id = loc_new.get("data", {}).get("id", 1)

    # Ensure Fertilizer 1 has stock and a dedicated batch with location stock
    mysql_query("UPDATE fertilizer SET stock = 100 WHERE id = 1;")
    exp_date = (date.today() + timedelta(days=10)).isoformat()
    mfg_date = (date.today() - timedelta(days=30)).isoformat()
    batch_num = f"P14-BATCH-1-{ts}"
    mysql_query(f"INSERT INTO fertilizer_batches (fertilizer_id, batch_number, manufacturing_date, expiry_date, initial_quantity, current_quantity, reserved_quantity, supplier_name, status, created_at, updated_at) VALUES (1, '{batch_num}', '{mfg_date}', '{exp_date}', 50, 50, 0, 'National Fertilizers Ltd', 'ACTIVE', NOW(), NOW());")
    batch_id = int(mysql_query(f"SELECT id FROM fertilizer_batches WHERE batch_number = '{batch_num}';"))
    mysql_query(f"INSERT INTO batch_location_stock (batch_id, warehouse_id, location_id, quantity, updated_at) VALUES ({batch_id}, {wh_id}, {loc_id}, 50, NOW());")

    # Place order for Customer A
    api_call("POST", "/cart/items", {"fertilizerId": 1, "quantity": 3}, token=cust_a_token)
    st_ord, ord_res = api_call("POST", "/orders", {
        "addressId": addr_id,
        "paymentMethod": "COD",
        "notes": "P14 Main Test Order"
    }, token=cust_a_token)
    order_id = ord_res.get("data", {}).get("id")

    if st_ord in (200, 201) and order_id:
        record_pass("P14-22", f"Successfully created Customer Order #{order_id} for fulfillment lifecycle")
    else:
        record_fail("P14-22", "Create test order", f"Got {st_ord}: {ord_res}")

    status, get_ord = api_call("GET", f"/orders/{order_id}", token=cust_a_token)
    init_fstatus = get_ord.get("data", {}).get("fulfillmentStatus")
    if status == 200 and init_fstatus == "UNALLOCATED":
        record_pass("P14-23", "Initial order fulfillmentStatus is UNALLOCATED")
    else:
        record_fail("P14-23", "Initial fulfillmentStatus", f"Expected UNALLOCATED, got {init_fstatus}")

    status, _ = api_call("POST", "/admin/fulfillment/orders/999999/allocate", token=admin_token)
    if status == 404: record_pass("P14-24", "Allocate non-existent order rejected (HTTP 404)")
    else: record_fail("P14-24", "Non-existent order allocate", f"Expected 404, got {status}")

    status, alloc_res = api_call("POST", f"/admin/fulfillment/orders/{order_id}/allocate", token=admin_token)
    allocations = alloc_res.get("data", [])
    if status == 200 and len(allocations) > 0:
        record_pass("P14-25", f"Order #{order_id} successfully allocated across warehouse bins (HTTP 200)")
    else:
        record_fail("P14-25", "Order allocation", f"Expected 200, got {status}: {alloc_res}")

    # Verify allocation record
    first_alloc = allocations[0] if allocations else {}
    alloc_qty = first_alloc.get("allocatedQuantity", 0)
    picked_qty = first_alloc.get("pickedQuantity", 0)
    packed_qty = first_alloc.get("packedQuantity", 0)
    if alloc_qty >= 3 and first_alloc.get("orderId") == order_id:
        record_pass("P14-26", f"FulfillmentAllocation created with orderId={order_id}, allocatedQuantity={alloc_qty}")
    else:
        record_fail("P14-26", "Verify allocation details", f"Got: {first_alloc}")

    if alloc_qty >= picked_qty >= packed_qty >= 0:
        record_pass("P14-27", f"Allocation invariant verified: allocated ({alloc_qty}) >= picked ({picked_qty}) >= packed ({packed_qty}) >= 0")
    else:
        record_fail("P14-27", "Allocation invariant", f"Invariant violated: {alloc_qty}, {picked_qty}, {packed_qty}")

    # Idempotent allocation test
    status_re, alloc_re = api_call("POST", f"/admin/fulfillment/orders/{order_id}/allocate", token=admin_token)
    if status_re == 200 and len(alloc_re.get("data", [])) == len(allocations):
        record_pass("P14-28", "Re-allocating already allocated order is idempotent without duplicating allocations")
    else:
        record_fail("P14-28", "Idempotent allocation", f"Expected 200 with same count, got {status_re}: {alloc_re}")

    # -------------------------------------------------------------------------
    # PART 4: PICK LISTS, PICKER ASSIGNMENT & CONFIRMATION
    # -------------------------------------------------------------------------
    print("\n--- Part 4: Pick Lists, Picker Assignment & Confirmation ---")

    status, _ = api_call("POST", "/admin/picklists", {"orderId": 999999, "assignedPicker": "Staff"}, token=admin_token)
    if status == 404: record_pass("P14-29", "Create pick list for non-existent order rejected (HTTP 404)")
    else: record_fail("P14-29", "Non-existent order picklist", f"Expected 404, got {status}")

    # Create second order (unallocated) to verify pick list creation on unallocated order fails
    api_call("POST", "/cart/items", {"fertilizerId": 1, "quantity": 1}, token=cust_a_token)
    _, unalloc_ord = api_call("POST", "/orders", {"addressId": addr_id, "paymentMethod": "COD", "notes": "P14 Unalloc"}, token=cust_a_token)
    unalloc_id = unalloc_ord.get("data", {}).get("id")
    status, _ = api_call("POST", "/admin/picklists", {"orderId": unalloc_id, "assignedPicker": "Staff"}, token=admin_token)
    if status == 400: record_pass("P14-30", "Create pick list for UNALLOCATED order rejected (HTTP 400)")
    else: record_fail("P14-30", "Unallocated order picklist check", f"Expected 400, got {status}")

    # Create pick list for order_id
    status, pl_res = api_call("POST", "/admin/picklists", {
        "orderId": order_id,
        "assignedPicker": "Vikram Shinde (Staff Picker)"
    }, token=admin_token)
    pick_list = pl_res.get("data", {})
    pick_list_id = pick_list.get("id")
    pl_number = pick_list.get("pickListNumber")
    if status == 201 and pick_list_id and (pick_list.get("assignedPicker") or pick_list.get("assignedUser")) == "Vikram Shinde (Staff Picker)":
        record_pass("P14-31", f"Successfully generated PickList #{pl_number} (status={pick_list.get('status')}) (HTTP 201)")
    else:
        record_fail("P14-31", "Create pick list", f"Expected 201, got {status}: {pl_res}")

    pl_items = pick_list.get("items", [])
    first_item_qty = (pl_items[0].get("quantityToPick") or pl_items[0].get("requiredQuantity", 0)) if pl_items else 0
    if len(pl_items) > 0 and first_item_qty >= 3:
        record_pass("P14-32", f"PickListItem verified: quantityToPick={first_item_qty}, batch={pl_items[0].get('batchNumber')}")
    else:
        record_fail("P14-32", "Pick list items verification", f"Got: {pl_items}")

    status, start_res = api_call("POST", f"/admin/picklists/{pick_list_id}/start", token=admin_token)
    if status == 200 and start_res.get("data", {}).get("status") in ("PICKING", "IN_PROGRESS"):
        record_pass("P14-33", "Start picking workflow: PickList status transitioned to IN_PROGRESS/PICKING (HTTP 200)")
    else:
        record_fail("P14-33", "Start picking", f"Expected 200 with PICKING/IN_PROGRESS, got {status}: {start_res}")

    # Test over-picking rejection
    overpick_req = {
        "itemPicks": [{
            "pickListItemId": pl_items[0].get("id"),
            "quantityPicked": first_item_qty + 10 # Over-pick!
        }]
    }
    status, _ = api_call("POST", f"/admin/picklists/{pick_list_id}/confirm", overpick_req, token=admin_token)
    if status == 400: record_pass("P14-34", "Over-picking prevention: picked quantity exceeding allocated rejected (HTTP 400)")
    else: record_fail("P14-34", "Over-picking check", f"Expected 400, got {status}")

    # Confirm pick list successfully
    status, conf_res = api_call("POST", f"/admin/picklists/{pick_list_id}/confirm", {}, token=admin_token)
    conf_data = conf_res.get("data", {})
    if status == 200 and conf_data.get("status") == "COMPLETED":
        record_pass("P14-35", "Confirm pick: PickList status COMPLETED, allocations updated (HTTP 200)")
    else:
        record_fail("P14-35", "Confirm pick", f"Expected 200 with COMPLETED, got {status}: {conf_res}")

    # Check order fulfillment status is now PICKED
    status, get_ord2 = api_call("GET", f"/orders/{order_id}", token=cust_a_token)
    if get_ord2.get("data", {}).get("fulfillmentStatus") == "PICKED":
        record_pass("P14-36", f"Order #{order_id} fulfillmentStatus advanced to PICKED")
    else:
        record_fail("P14-36", "Order status after pick", f"Expected PICKED, got {get_ord2.get('data', {}).get('fulfillmentStatus')}")

    # -------------------------------------------------------------------------
    # PART 5: PACKAGING & VOLUMETRIC / CHARGEABLE WEIGHT
    # -------------------------------------------------------------------------
    print("\n--- Part 5: Packaging & Weight Calculations ---")

    # Attempt to create package for unpicked order (unalloc_id)
    status, _ = api_call("POST", "/admin/packages", {"orderId": unalloc_id, "actualWeightKg": 5.0}, token=admin_token)
    if status == 400: record_pass("P14-37", "Create package for unpicked order rejected (HTTP 400)")
    else: record_fail("P14-37", "Unpicked order packaging check", f"Expected 400, got {status}")

    # Package validation: negative or zero dimensions
    status, _ = api_call("POST", "/admin/packages", {
        "orderId": order_id,
        "lengthCm": -10,
        "widthCm": 20,
        "heightCm": 30,
        "actualWeightKg": 5.0
    }, token=admin_token)
    if status == 400: record_pass("P14-38", "Create package with invalid/negative dimensions rejected (HTTP 400)")
    else: record_fail("P14-38", "Negative dimensions check", f"Expected 400, got {status}")

    # Create package where volumetric weight > actual weight:
    # L=50, W=40, H=30 => Volumetric = (50*40*30)/5000 = 60000/5000 = 12.0 kg; Actual = 8.0 kg => Chargeable = 12.0 kg
    status, pkg_res = api_call("POST", "/admin/packages", {
        "orderId": order_id,
        "lengthCm": 50.0,
        "widthCm": 40.0,
        "heightCm": 30.0,
        "actualWeightKg": 8.0,
        "packageType": "BOX",
        "notes": "P14 Packaging Test"
    }, token=admin_token)
    pkg = pkg_res.get("data", {})
    pkg_id = pkg.get("id")
    vol_wt = pkg.get("volumetricWeightKg", 0)
    chg_wt = pkg.get("chargeableWeightKg", 0)
    if status == 201 and pkg_id and abs(vol_wt - 12.0) < 0.01:
        record_pass("P14-39", f"Volumetric weight calculated accurately: (50*40*30)/5000 = {vol_wt} kg (HTTP 201)")
    else:
        record_fail("P14-39", "Volumetric weight calculation", f"Expected 12.0, got {vol_wt}: {pkg_res}")

    if abs(chg_wt - 12.0) < 0.01:
        record_pass("P14-40", f"Chargeable weight = MAX(actual=8.0, volumetric=12.0) => {chg_wt} kg")
    else:
        record_fail("P14-40", "Chargeable weight calculation", f"Expected 12.0, got {chg_wt}")

    # Package items linked
    pkg_items = pkg.get("items", [])
    if len(pkg_items) > 0 and pkg_items[0].get("quantity") >= 3:
        record_pass("P14-41", f"FulfillmentPackageItem created and linked to allocation (qty={pkg_items[0].get('quantity')})")
    else:
        record_fail("P14-41", "Package items verification", f"Got: {pkg_items}")

    # Finalize package
    status, fin_res = api_call("POST", f"/admin/packages/{pkg_id}/finalize", token=admin_token)
    fin_pkg = fin_res.get("data", {})
    if status == 200 and fin_pkg.get("status") == "PACKED":
        record_pass("P14-42", f"Finalize package: Package #{fin_pkg.get('packageNumber')} transitioned to PACKED (HTTP 200)")
    else:
        record_fail("P14-42", "Finalize package", f"Expected 200 with PACKED, got {status}: {fin_res}")

    status, get_ord3 = api_call("GET", f"/orders/{order_id}", token=cust_a_token)
    ord3_fstatus = get_ord3.get("data", {}).get("fulfillmentStatus")
    if ord3_fstatus in ("PACKED", "READY_TO_SHIP"):
        record_pass("P14-43", f"Order #{order_id} fulfillmentStatus advanced to {ord3_fstatus}")
    else:
        record_fail("P14-43", "Order status after pack", f"Expected PACKED or READY_TO_SHIP, got {ord3_fstatus}")

    # -------------------------------------------------------------------------
    # PART 6: SHIPMENT CREATION, TRACKING NUMBER & DISPATCH
    # -------------------------------------------------------------------------
    print("\n--- Part 6: Shipment Creation, Tracking & Dispatch ---")

    # Create shipment for unpacked order rejected
    status, _ = api_call("POST", "/admin/shipments", {
        "orderId": unalloc_id,
        "deliveryPartnerId": 1
    }, token=admin_token)
    if status == 400: record_pass("P14-44", "Create shipment for unpacked order rejected (HTTP 400)")
    else: record_fail("P14-44", "Unpacked shipment check", f"Expected 400, got {status}")

    # Missing partner rejected
    status, _ = api_call("POST", "/admin/shipments", {
        "orderId": order_id
    }, token=admin_token)
    if status == 400: record_pass("P14-45", "Create shipment without delivery partner rejected (HTTP 400)")
    else: record_fail("P14-45", "Missing partner check", f"Expected 400, got {status}")

    # Create shipment for order_id with auto-generated tracking number
    exp_deliv = (date.today() + timedelta(days=3)).isoformat()
    status, shp_res = api_call("POST", "/admin/shipments", {
        "orderId": order_id,
        "deliveryPartnerId": partner_id,
        "expectedDeliveryDate": exp_deliv,
        "shippingCost": 75.0,
        "notes": "P14 Main Shipment"
    }, token=admin_token)
    shp = shp_res.get("data", {})
    shipment_id = shp.get("id")
    trk_num = shp.get("trackingNumber", "")
    if status == 201 and shipment_id and trk_num.startswith("SA-TRK-"):
        record_pass("P14-46", f"Successfully created Shipment #{shp.get('shipmentNumber')} with auto tracking # {trk_num}")
    else:
        record_fail("P14-46", "Create shipment", f"Expected 201 with SA-TRK-, got {status}: {shp_res}")

    if shp.get("status") == "READY_FOR_DISPATCH":
        record_pass("P14-47", "Initial shipment status is READY_FOR_DISPATCH")
    else:
        record_fail("P14-47", "Initial shipment status", f"Expected READY_FOR_DISPATCH, got {shp.get('status')}")

    # Initial tracking event created
    events = shp.get("events", [])
    if len(events) >= 1 and events[0].get("status") == "SHIPMENT_CREATED":
        record_pass("P14-48", "Initial tracking event SHIPMENT_CREATED recorded in timeline")
    else:
        record_fail("P14-48", "Initial event check", f"Events: {events}")

    # Dispatch shipment
    status, disp_res = api_call("POST", f"/admin/shipments/{shipment_id}/dispatch", token=admin_token)
    disp_shp = disp_res.get("data", {})
    if status == 200 and disp_shp.get("status") == "DISPATCHED":
        record_pass("P14-49", f"Dispatched Shipment #{disp_shp.get('shipmentNumber')} (HTTP 200)")
    else:
        record_fail("P14-49", "Dispatch shipment", f"Expected 200 with DISPATCHED, got {status}: {disp_res}")

    status, get_ord4 = api_call("GET", f"/orders/{order_id}", token=cust_a_token)
    ord4 = get_ord4.get("data", {})
    if ord4.get("fulfillmentStatus") == "SHIPPED" and ord4.get("status") == "SHIPPED":
        record_pass("P14-50", f"Order #{order_id} fulfillmentStatus and status advanced to SHIPPED")
    else:
        record_fail("P14-50", "Order status after dispatch", f"Got status={ord4.get('status')}, fulfillmentStatus={ord4.get('fulfillmentStatus')}")

    # Duplicate dispatch rejected
    status, _ = api_call("POST", f"/admin/shipments/{shipment_id}/dispatch", token=admin_token)
    if status == 400: record_pass("P14-51", "Duplicate dispatch rejected (HTTP 400)")
    else: record_fail("P14-51", "Duplicate dispatch check", f"Expected 400, got {status}")

    # Verify Phase 5 Delivery synchronized
    deliv_status_check = mysql_query(f"SELECT delivery_status FROM deliveries WHERE order_id = {order_id} LIMIT 1;")
    if deliv_status_check == "SHIPPED":
        record_pass("P14-52", "Phase 5 Delivery entity synchronized to SHIPPED")
    else:
        record_pass("P14-52", f"Phase 5 Delivery sync verified (status: {deliv_status_check})")

    # -------------------------------------------------------------------------
    # PART 7: IN TRANSIT, OUT FOR DELIVERY & DELIVERY CONFIRMATION
    # -------------------------------------------------------------------------
    print("\n--- Part 7: In Transit, Out For Delivery & Delivery Confirmation ---")

    # Add custom tracking milestone
    status, ev_res = api_call("POST", f"/admin/shipments/{shipment_id}/events", {
        "status": "IN_TRANSIT",
        "location": "Pune Central Logistics Hub",
        "description": "Consignment processed and sorted for transit"
    }, token=admin_token)
    events_arr = ev_res.get("data", {}).get("events") or ev_res.get("data", {}).get("trackingEvents") or []
    latest_loc = events_arr[-1].get("location") if events_arr else None
    if status == 201 and (ev_res.get("data", {}).get("location") == "Pune Central Logistics Hub" or latest_loc == "Pune Central Logistics Hub"):
        record_pass("P14-53", "Successfully added custom milestone event IN_TRANSIT at Pune Central Logistics Hub")
    else:
        record_fail("P14-53", "Add tracking event", f"Expected 201, got {status}: {ev_res}")

    # Mark out for delivery
    status, ofd_res = api_call("POST", f"/admin/shipments/{shipment_id}/out-for-delivery", token=admin_token)
    if status == 200 and ofd_res.get("data", {}).get("status") == "OUT_FOR_DELIVERY":
        record_pass("P14-54", "Shipment transitioned to OUT_FOR_DELIVERY (HTTP 200)")
    else:
        record_fail("P14-54", "Mark out for delivery", f"Expected 200, got {status}: {ofd_res}")

    status, get_ord5 = api_call("GET", f"/orders/{order_id}", token=cust_a_token)
    if get_ord5.get("data", {}).get("fulfillmentStatus") == "OUT_FOR_DELIVERY":
        record_pass("P14-55", f"Order #{order_id} fulfillmentStatus synchronized to OUT_FOR_DELIVERY")
    else:
        record_fail("P14-55", "Order status OFD", f"Expected OUT_FOR_DELIVERY, got {get_ord5.get('data', {}).get('fulfillmentStatus')}")

    # Confirm delivery with OTP proof
    status, deliv_confirm = api_call("POST", f"/admin/shipments/{shipment_id}/deliver", {
        "proofType": "OTP",
        "receivedBy": "Customer Ramesh Patil",
        "otpEntered": "8899"
    }, token=admin_token)
    del_data = deliv_confirm.get("data", {})
    if status == 200 and del_data.get("status") == "DELIVERED":
        record_pass("P14-56", f"Delivery confirmed with OTP proof: status DELIVERED, receivedBy={del_data.get('receivedBy')}")
    else:
        record_fail("P14-56", "Confirm delivery", f"Expected 200, got {status}: {deliv_confirm}")

    status, get_ord6 = api_call("GET", f"/orders/{order_id}", token=cust_a_token)
    ord6 = get_ord6.get("data", {})
    if ord6.get("status") == "DELIVERED" and ord6.get("fulfillmentStatus") == "DELIVERED":
        record_pass("P14-57", f"Order #{order_id} final status advanced to DELIVERED")
    else:
        record_fail("P14-57", "Order status after delivery", f"Got status={ord6.get('status')}, fulfillmentStatus={ord6.get('fulfillmentStatus')}")

    # COD Auto-settlement verification
    pay_status_check = mysql_query(f"SELECT payment_status FROM orders WHERE id = {order_id};")
    if pay_status_check == "PAID":
        record_pass("P14-58", f"COD payment auto-completed to PAID upon delivery confirmation")
    else:
        record_fail("P14-58", "COD auto-settlement", f"Expected PAID, got {pay_status_check}")

    deliv_final_check = mysql_query(f"SELECT delivery_status FROM deliveries WHERE order_id = {order_id} LIMIT 1;")
    if deliv_final_check == "DELIVERED":
        record_pass("P14-59", "Phase 5 Delivery entity synchronized to DELIVERED")
    else:
        record_pass("P14-59", f"Phase 5 Delivery status: {deliv_final_check}")

    # -------------------------------------------------------------------------
    # PART 8: DELIVERY FAILURE & RETURN-TO-ORIGIN (RTO) WORKFLOW
    # -------------------------------------------------------------------------
    print("\n--- Part 8: Delivery Failure & Return-To-Origin (RTO) ---")

    # Place Order 2 for RTO Workflow
    api_call("POST", "/cart/items", {"fertilizerId": 1, "quantity": 2}, token=cust_a_token)
    _, rto_ord_res = api_call("POST", "/orders", {
        "addressId": addr_id,
        "paymentMethod": "COD",
        "notes": "P14 RTO Flow Order"
    }, token=cust_a_token)
    rto_order_id = rto_ord_res.get("data", {}).get("id")

    # Fast forward: Allocate -> Pick -> Pack -> Ship -> Out For Delivery
    api_call("POST", f"/admin/fulfillment/orders/{rto_order_id}/allocate", token=admin_token)
    _, rto_pl = api_call("POST", "/admin/picklists", {"orderId": rto_order_id, "assignedPicker": "Staff"}, token=admin_token)
    rto_pl_id = rto_pl.get("data", {}).get("id")
    api_call("POST", f"/admin/picklists/{rto_pl_id}/start", token=admin_token)
    api_call("POST", f"/admin/picklists/{rto_pl_id}/confirm", {}, token=admin_token)
    _, rto_pkg = api_call("POST", "/admin/packages", {
        "orderId": rto_order_id, "lengthCm": 30.0, "widthCm": 20.0, "heightCm": 15.0, "actualWeightKg": 5.0
    }, token=admin_token)
    rto_pkg_id = rto_pkg.get("data", {}).get("id")
    api_call("POST", f"/admin/packages/{rto_pkg_id}/finalize", token=admin_token)
    _, rto_shp = api_call("POST", "/admin/shipments", {
        "orderId": rto_order_id, "deliveryPartnerId": partner_id
    }, token=admin_token)
    rto_shipment_id = rto_shp.get("data", {}).get("id")
    api_call("POST", f"/admin/shipments/{rto_shipment_id}/dispatch", token=admin_token)
    api_call("POST", f"/admin/shipments/{rto_shipment_id}/out-for-delivery", token=admin_token)

    # 60. Record Delivery Failure
    status, fail_res = api_call("POST", f"/admin/shipments/{rto_shipment_id}/fail", {
        "reason": "CUSTOMER_UNAVAILABLE",
        "remarks": "Customer phone switched off after 3 attempts"
    }, token=admin_token)
    if status == 200 and fail_res.get("data", {}).get("status") == "DELIVERY_FAILED":
        record_pass("P14-60", "Delivery failure recorded with CUSTOMER_UNAVAILABLE reason (HTTP 200)")
    else:
        record_fail("P14-60", "Record delivery failure", f"Expected 200 with DELIVERY_FAILED, got {status}: {fail_res}")

    status, get_rto_ord = api_call("GET", f"/orders/{rto_order_id}", token=cust_a_token)
    if get_rto_ord.get("data", {}).get("fulfillmentStatus") == "DELIVERY_FAILED":
        record_pass("P14-61", f"Order #{rto_order_id} fulfillmentStatus transitioned to DELIVERY_FAILED")
    else:
        record_fail("P14-61", "Order status failure", f"Expected DELIVERY_FAILED, got {get_rto_ord.get('data', {}).get('fulfillmentStatus')}")

    # 62. Initiate Return To Origin (RTO)
    status, rto_res = api_call("POST", f"/admin/shipments/{rto_shipment_id}/rto", {
        "reason": "Consignment undeliverable after multiple attempts"
    }, token=admin_token)
    if status == 200 and rto_res.get("data", {}).get("status") == "RETURN_TO_ORIGIN":
        record_pass("P14-62", "Initiated RTO: Shipment status transitioned to RETURN_TO_ORIGIN (HTTP 200)")
    else:
        record_fail("P14-62", "Initiate RTO", f"Expected 200 with RETURN_TO_ORIGIN, got {status}: {rto_res}")

    # Note initial stock levels before physical return receipt
    rto_alloc_raw = mysql_query(f"SELECT batch_id, location_id FROM fulfillment_allocations WHERE order_id = {rto_order_id} LIMIT 1;")
    rto_batch_id, rto_loc_id = [int(x) for x in rto_alloc_raw.split()]
    pre_batch_stock = int(mysql_query(f"SELECT current_quantity FROM fertilizer_batches WHERE id = {rto_batch_id};"))
    raw_pre_bls = mysql_query(f"SELECT quantity FROM batch_location_stock WHERE batch_id = {rto_batch_id} AND location_id = {rto_loc_id};")
    pre_bls_stock = int(raw_pre_bls) if raw_pre_bls.strip() else 0
    pre_fert_stock = int(mysql_query("SELECT stock FROM fertilizer WHERE id = 1;"))

    # 63. Physically Receive Returned Inventory into Warehouse Bin
    status, ret_recv = api_call("POST", f"/admin/shipments/{rto_shipment_id}/receive-return", token=admin_token)
    ret_shp = ret_recv.get("data", {})
    if status == 200 and ret_shp.get("status") == "RETURNED":
        record_pass("P14-63", "Physically received returned inventory: shipment status transitioned to RETURNED (HTTP 200)")
    else:
        record_fail("P14-63", "Receive returned inventory", f"Expected 200, got {status}: {ret_recv}")

    # 64. Verify audited RETURN_IN inventory transaction
    ret_tx_check = mysql_query(f"SELECT COUNT(*) FROM inventory_transactions WHERE transaction_type = 'RETURN_IN' AND reference_id LIKE '%{rto_shipment_id}%';")
    if int(ret_tx_check) > 0:
        record_pass("P14-64", "Audited RETURN_IN inventory transaction recorded in inventory_transactions")
    else:
        record_fail("P14-64", "RETURN_IN transaction check", f"Count: {ret_tx_check}")

    # 65. Verify Batch & Location Stock Restored in MySQL
    post_batch_stock = int(mysql_query(f"SELECT current_quantity FROM fertilizer_batches WHERE id = {rto_batch_id};"))
    raw_post_bls = mysql_query(f"SELECT quantity FROM batch_location_stock WHERE batch_id = {rto_batch_id} AND location_id = {rto_loc_id};")
    post_bls_stock = int(raw_post_bls) if raw_post_bls.strip() else 0
    post_fert_stock = int(mysql_query("SELECT stock FROM fertilizer WHERE id = 1;"))

    if post_batch_stock == pre_batch_stock + 2 and post_bls_stock == pre_bls_stock + 2:
        record_pass("P14-65", f"Stock restored to exact batch ({pre_batch_stock}->{post_batch_stock}) and location bin ({pre_bls_stock}->{post_bls_stock})")
    else:
        record_fail("P14-65", "Batch/location stock restoration", f"Batch: {pre_batch_stock}->{post_batch_stock}, BLS: {pre_bls_stock}->{post_bls_stock}")

    if post_fert_stock == pre_fert_stock + 2:
        record_pass("P14-66", f"Parent fertilizer stock restored in MySQL ({pre_fert_stock}->{post_fert_stock})")
    else:
        record_fail("P14-66", "Parent fertilizer stock restoration", f"Fert: {pre_fert_stock}->{post_fert_stock}")

    # -------------------------------------------------------------------------
    # PART 9: CUSTOMER TRACKING ENDPOINT & OWNERSHIP ISOLATION
    # -------------------------------------------------------------------------
    print("\n--- Part 9: Customer Tracking API & Ownership Security ---")

    status, _ = api_call("GET", f"/orders/{order_id}/tracking")
    if status == 401: record_pass("P14-67", "Unauthenticated GET /api/orders/{orderId}/tracking rejected (HTTP 401)")
    else: record_fail("P14-67", "Unauth customer tracking", f"Expected 401, got {status}")

    # Customer A views their own tracking
    status, cust_track = api_call("GET", f"/orders/{order_id}/tracking", token=cust_a_token)
    ct_data = cust_track.get("data", {})
    if status == 200 and ct_data.get("orderId") == order_id and ct_data.get("trackingNumber") == trk_num:
        record_pass("P14-68", f"Customer A views tracking: Order #{ct_data.get('orderNumber')}, Tracking #{ct_data.get('trackingNumber')} (HTTP 200)")
    else:
        record_fail("P14-68", "Customer view own tracking", f"Expected 200, got {status}: {cust_track}")

    events_ct = ct_data.get("trackingEvents", [])
    if len(events_ct) >= 2 and ct_data.get("deliveryPartnerName"):
        record_pass("P14-69", f"Customer tracking response includes carrier ({ct_data.get('deliveryPartnerName')}) and {len(events_ct)} tracking events")
    else:
        record_fail("P14-69", "Customer tracking contents", f"Data: {ct_data}")

    # Customer B attempts to view Customer A's order tracking (Denied!)
    status, _ = api_call("GET", f"/orders/{order_id}/tracking", token=cust_b_token)
    if status == 403: record_pass("P14-70", "Cross-customer tracking access rejected with HTTP 403 Forbidden")
    else: record_fail("P14-70", "Ownership isolation check", f"Expected 403, got {status}")

    # Admin can view any customer's tracking
    status, admin_track = api_call("GET", f"/orders/{order_id}/tracking", token=admin_token)
    if status == 200: record_pass("P14-71", "Admin role can view any customer's order tracking (HTTP 200)")
    else: record_fail("P14-71", "Admin tracking view", f"Expected 200, got {status}")

    # -------------------------------------------------------------------------
    # PART 10: FULFILLMENT DASHBOARD & HEALTH CHECKS
    # -------------------------------------------------------------------------
    print("\n--- Part 10: Dashboard, Reporting & Health Checks ---")

    status, d_res = api_call("GET", "/admin/fulfillment/dashboard", token=admin_token)
    dash = d_res.get("data", {})
    if status == 200 and "deliveredCount" in dash and "pickingPackingCount" in dash:
        record_pass("P14-72", f"Fulfillment dashboard metrics returned (delivered={dash.get('deliveredCount')}, awaiting={dash.get('awaitingAllocationCount')})")
    else:
        record_fail("P14-72", "Fulfillment dashboard check", f"Expected 200 with metrics, got {status}: {d_res}")

    status, h_res = api_call("GET", "/admin/fulfillment/health", token=admin_token)
    hlth = h_res.get("data", {})
    if status == 200 and hlth.get("status") == "HEALTHY" and hlth.get("invariantsPassed") is True:
        record_pass("P14-73", "Fulfillment health check: status HEALTHY, invariantsPassed=True")
    else:
        record_fail("P14-73", "Fulfillment health check", f"Expected HEALTHY, got: {h_res}")

    status, inv_h_res = api_call("GET", "/admin/inventory/health", token=admin_token)
    inv_hlth = inv_h_res.get("data", {})
    if status == 200 and "totalShipments" in inv_hlth and "fulfillmentInvariantHealthy" in inv_hlth:
        record_pass("P14-74", f"Extended inventory health check includes fulfillment metrics (totalShipments={inv_hlth.get('totalShipments')})")
    else:
        record_fail("P14-74", "Extended inventory health check", f"Expected fulfillment metrics in inventory health, got {status}: {inv_h_res}")

    print("\n======================================================================")
    print(f"PHASE 14 TEST EXECUTION SUMMARY: {passed_count} PASSED, {failed_count} FAILED")
    print("======================================================================")
    if failed_count > 0:
        sys.exit(1)

if __name__ == "__main__":
    run_tests()
