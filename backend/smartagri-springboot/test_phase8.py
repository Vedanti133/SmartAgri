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

def run_tests():
    print("======================================================================")
    print("SMARTAGRI PHASE 8 AUTOMATED TEST SUITE")
    print("Customer Checkout, Address Management & Order Review Verification")
    print("======================================================================")

    ts = int(time.time())
    user_a_name = f"p8_user_a_{ts}"
    user_b_name = f"p8_user_b_{ts}"

    # -------------------------------------------------------------------------
    # PART 1: AUTHENTICATION & SECURITY PRE-CHECKS
    # -------------------------------------------------------------------------

    # 1. Unauthenticated Address Access Rejected (GET /api/addresses -> 401)
    try:
        st, res = api_call("GET", "/addresses")
        if st == 401:
            record_pass("P8-01", "Unauthenticated GET /api/addresses rejected (HTTP 401)")
        else:
            record_fail("P8-01", "Unauthenticated GET /api/addresses", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P8-01", "Unauthenticated GET /api/addresses", str(e))

    # 2. Unauthenticated Address Creation Rejected (POST /api/addresses -> 401)
    try:
        st, res = api_call("POST", "/addresses", {"fullName": "Hacker", "phone": "9999999999"})
        if st == 401:
            record_pass("P8-02", "Unauthenticated POST /api/addresses rejected (HTTP 401)")
        else:
            record_fail("P8-02", "Unauthenticated POST /api/addresses", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P8-02", "Unauthenticated POST /api/addresses", str(e))

    # 3. Unauthenticated Order Placement Rejected (POST /api/orders -> 401)
    try:
        st, res = api_call("POST", "/orders", {"addressId": 1, "paymentMethod": "COD"})
        if st == 401:
            record_pass("P8-03", "Unauthenticated POST /api/orders rejected (HTTP 401)")
        else:
            record_fail("P8-03", "Unauthenticated POST /api/orders", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P8-03", "Unauthenticated POST /api/orders", str(e))

    # Register & Login User A and User B
    token_a = None
    user_a_id = None
    token_b = None
    user_b_id = None
    try:
        st_a, res_a = api_call("POST", "/auth/register", {
            "name": "Ramesh Patel Farmer",
            "username": user_a_name,
            "email": f"{user_a_name}@example.com",
            "password": "Password123!",
            "role": "CUSTOMER"
        })
        if st_a in (200, 201) and res_a.get("success"):
            user_a_id = res_a["data"]["id"]
            # login A
            _, log_a = api_call("POST", "/auth/login", {"username": user_a_name, "password": "Password123!"})
            token_a = log_a["data"]["token"]

        st_b, res_b = api_call("POST", "/auth/register", {
            "name": "Suresh Deshmukh Farmer",
            "username": user_b_name,
            "email": f"{user_b_name}@example.com",
            "password": "Password123!",
            "role": "CUSTOMER"
        })
        if st_b in (200, 201) and res_b.get("success"):
            user_b_id = res_b["data"]["id"]
            # login B
            _, log_b = api_call("POST", "/auth/login", {"username": user_b_name, "password": "Password123!"})
            token_b = log_b["data"]["token"]

        if token_a and token_b:
            record_pass("P8-04", f"Customer User A (id={user_a_id}) and User B (id={user_b_id}) registered and authenticated")
        else:
            record_fail("P8-04", "Register & Authenticate Users", f"Token A: {bool(token_a)}, Token B: {bool(token_b)}")
    except Exception as e:
        record_fail("P8-04", "Register & Authenticate Users", str(e))

    # -------------------------------------------------------------------------
    # PART 2: ADDRESS MANAGEMENT (CRUD & VALIDATION)
    # -------------------------------------------------------------------------

    # 5. Address Validation: Missing Full Name rejected (HTTP 400)
    try:
        st, res = api_call("POST", "/addresses", {
            "fullName": "",
            "phone": "9876543210",
            "addressLine1": "Plot 12, Farm Road",
            "city": "Pune",
            "state": "Maharashtra",
            "pincode": "411001"
        }, token=token_a)
        if st == 400:
            record_pass("P8-05", "Address creation with missing full_name rejected (HTTP 400)")
        else:
            record_fail("P8-05", "Missing full_name validation", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P8-05", "Missing full_name validation", str(e))

    # 6. Address Validation: Missing Address Line 1 rejected (HTTP 400)
    try:
        st, res = api_call("POST", "/addresses", {
            "fullName": "Ramesh Patel",
            "phone": "9876543210",
            "addressLine1": "",
            "city": "Pune",
            "state": "Maharashtra",
            "pincode": "411001"
        }, token=token_a)
        if st == 400:
            record_pass("P8-06", "Address creation with missing address_line1 rejected (HTTP 400)")
        else:
            record_fail("P8-06", "Missing address_line1 validation", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P8-06", "Missing address_line1 validation", str(e))

    # 7. Address Validation: Missing City rejected (HTTP 400)
    try:
        st, res = api_call("POST", "/addresses", {
            "fullName": "Ramesh Patel",
            "phone": "9876543210",
            "addressLine1": "Farm 14",
            "city": "",
            "state": "Maharashtra",
            "pincode": "411001"
        }, token=token_a)
        if st == 400:
            record_pass("P8-07", "Address creation with missing city rejected (HTTP 400)")
        else:
            record_fail("P8-07", "Missing city validation", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P8-07", "Missing city validation", str(e))

    # 8. Address Validation: Missing State rejected (HTTP 400)
    try:
        st, res = api_call("POST", "/addresses", {
            "fullName": "Ramesh Patel",
            "phone": "9876543210",
            "addressLine1": "Farm 14",
            "city": "Pune",
            "state": "",
            "pincode": "411001"
        }, token=token_a)
        if st == 400:
            record_pass("P8-08", "Address creation with missing state rejected (HTTP 400)")
        else:
            record_fail("P8-08", "Missing state validation", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P8-08", "Missing state validation", str(e))

    # 9. Address Validation: Invalid Phone Format rejected (HTTP 400)
    try:
        st, res = api_call("POST", "/addresses", {
            "fullName": "Ramesh Patel",
            "phone": "12345", # invalid length
            "addressLine1": "Farm 14",
            "city": "Pune",
            "state": "Maharashtra",
            "pincode": "411001"
        }, token=token_a)
        if st == 400:
            record_pass("P8-09", "Address creation with invalid phone (short) rejected (HTTP 400)")
        else:
            record_fail("P8-09", "Invalid phone validation", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P8-09", "Invalid phone validation", str(e))

    # 10. Address Validation: Invalid PIN Code (5 digits) rejected (HTTP 400)
    try:
        st, res = api_call("POST", "/addresses", {
            "fullName": "Ramesh Patel",
            "phone": "9876543210",
            "addressLine1": "Farm 14",
            "city": "Pune",
            "state": "Maharashtra",
            "pincode": "41100" # 5 digits
        }, token=token_a)
        if st == 400:
            record_pass("P8-10", "Address creation with 5-digit PIN code rejected (HTTP 400)")
        else:
            record_fail("P8-10", "Invalid pincode validation", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P8-10", "Invalid pincode validation", str(e))

    # 11. Address Validation: PIN Code starting with 0 rejected (HTTP 400)
    try:
        st, res = api_call("POST", "/addresses", {
            "fullName": "Ramesh Patel",
            "phone": "9876543210",
            "addressLine1": "Farm 14",
            "city": "Pune",
            "state": "Maharashtra",
            "pincode": "012345" # starts with 0
        }, token=token_a)
        if st == 400:
            record_pass("P8-11", "Address creation with PIN code starting with '0' rejected (HTTP 400)")
        else:
            record_fail("P8-11", "PIN code starting with 0", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P8-11", "PIN code starting with 0", str(e))

    # 12. Create Valid Primary Address for User A with District
    addr_a1_id = None
    try:
        st, res = api_call("POST", "/addresses", {
            "fullName": "Ramesh Kisan Patel",
            "phone": "9876543210",
            "addressLine1": "Survey No 42, Kisan Farmhouse",
            "addressLine2": "Taluka Shirur, Post Pabal",
            "city": "Shirur",
            "district": "Pune",
            "state": "Maharashtra",
            "pincode": "412403",
            "landmark": "Near Gram Panchayat Mandir",
            "addressType": "FARM",
            "default": True
        }, token=token_a)
        if st in (200, 201) and res.get("success"):
            addr_a1_id = res["data"]["id"]
            if res["data"]["district"] == "Pune" and res["data"]["city"] == "Shirur":
                record_pass("P8-12", f"Created Primary Farm Address with District (id={addr_a1_id})")
            else:
                record_fail("P8-12", "Address District field check", f"Data: {res['data']}")
        else:
            record_fail("P8-12", "Create Address with District", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-12", "Create Address with District", str(e))

    # 13. Create Second Address for User A (Home)
    addr_a2_id = None
    try:
        st, res = api_call("POST", "/addresses", {
            "fullName": "Ramesh Patel Home",
            "phone": "9876543210",
            "addressLine1": "Flat 302, Green Meadows",
            "addressLine2": "Kharadi Bypass Road",
            "city": "Pune",
            "district": "Pune",
            "state": "Maharashtra",
            "pincode": "411014",
            "landmark": "Near EON IT Park",
            "addressType": "HOME",
            "default": False
        }, token=token_a)
        if st in (200, 201) and res.get("success"):
            addr_a2_id = res["data"]["id"]
            record_pass("P8-13", f"Created Second Address for User A (id={addr_a2_id})")
        else:
            record_fail("P8-13", "Create Second Address", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-13", "Create Second Address", str(e))

    # 14. List Addresses for User A (GET /api/addresses)
    try:
        st, res = api_call("GET", "/addresses", token=token_a)
        if st == 200 and res.get("success") and len(res["data"]) >= 2:
            record_pass("P8-14", f"GET /api/addresses returns {len(res['data'])} addresses for User A")
        else:
            record_fail("P8-14", "List Addresses for User A", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-14", "List Addresses for User A", str(e))

    # 15. Get Specific Address by ID (GET /api/addresses/{id})
    try:
        st, res = api_call("GET", f"/addresses/{addr_a1_id}", token=token_a)
        if st == 200 and res.get("success") and res["data"]["id"] == addr_a1_id:
            record_pass("P8-15", f"GET /api/addresses/{addr_a1_id} returns correct address details")
        else:
            record_fail("P8-15", "Get Address by ID", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-15", "Get Address by ID", str(e))

    # 16. Update Address (PUT /api/addresses/{id})
    try:
        st, res = api_call("PUT", f"/addresses/{addr_a2_id}", {
            "fullName": "Ramesh Patel Residence",
            "phone": "9876543211",
            "addressLine1": "Flat 302, Green Meadows Tower B",
            "city": "Pune",
            "district": "Pune Urban",
            "state": "Maharashtra",
            "pincode": "411014",
            "addressType": "HOME",
            "default": False
        }, token=token_a)
        if st == 200 and res.get("success") and res["data"]["district"] == "Pune Urban" and res["data"]["phone"] == "9876543211":
            record_pass("P8-16", f"PUT /api/addresses/{addr_a2_id} successfully updated fields and district")
        else:
            record_fail("P8-16", "Update Address", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-16", "Update Address", str(e))

    # 17. Set Default Address (PUT /api/addresses/{id}/default)
    try:
        st, res = api_call("PUT", f"/addresses/{addr_a2_id}/default", token=token_a)
        if st == 200 and res.get("success") and res["data"]["default"] is True:
            # Verify addr_a1 is now not default
            _, res_a1 = api_call("GET", f"/addresses/{addr_a1_id}", token=token_a)
            if res_a1["data"]["default"] is False:
                record_pass("P8-17", f"PUT /api/addresses/{addr_a2_id}/default sets address as default and unsets others")
            else:
                record_fail("P8-17", "Set Default Address", f"Previous default not unset: {res_a1}")
        else:
            record_fail("P8-17", "Set Default Address", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-17", "Set Default Address", str(e))

    # Reset addr_a1 as default for checkout testing
    api_call("PUT", f"/addresses/{addr_a1_id}/default", token=token_a)

    # 18. Cross-User Security: User B cannot view User A's address (GET /api/addresses/{addr_a1_id})
    try:
        st, res = api_call("GET", f"/addresses/{addr_a1_id}", token=token_b)
        if st in (403, 400):
            record_pass("P8-18", f"Cross-user isolation: User B cannot read User A's address (HTTP {st})")
        else:
            record_fail("P8-18", "Cross-user view isolation", f"Expected 403 or 400, got {st}: {res}")
    except Exception as e:
        record_fail("P8-18", "Cross-user view isolation", str(e))

    # 19. Cross-User Security: User B cannot edit User A's address (PUT /api/addresses/{addr_a1_id})
    try:
        st, res = api_call("PUT", f"/addresses/{addr_a1_id}", {
            "fullName": "Tampered By User B",
            "phone": "9999999999",
            "addressLine1": "Hacked",
            "city": "Pune",
            "state": "Maharashtra",
            "pincode": "411001"
        }, token=token_b)
        if st in (403, 400):
            record_pass("P8-19", f"Cross-user isolation: User B cannot edit User A's address (HTTP {st})")
        else:
            record_fail("P8-19", "Cross-user edit isolation", f"Expected 403 or 400, got {st}: {res}")
    except Exception as e:
        record_fail("P8-19", "Cross-user edit isolation", str(e))

    # 20. Cross-User Security: User B cannot set User A's address as default (PUT /api/addresses/{addr_a1_id}/default)
    try:
        st, res = api_call("PUT", f"/addresses/{addr_a1_id}/default", token=token_b)
        if st in (403, 400):
            record_pass("P8-20", f"Cross-user isolation: User B cannot set User A's address as default (HTTP {st})")
        else:
            record_fail("P8-20", "Cross-user default isolation", f"Expected 403 or 400, got {st}: {res}")
    except Exception as e:
        record_fail("P8-20", "Cross-user default isolation", str(e))

    # 21. Cross-User Security: User B cannot delete User A's address (DELETE /api/addresses/{addr_a1_id})
    try:
        st, res = api_call("DELETE", f"/addresses/{addr_a1_id}", token=token_b)
        if st in (403, 400):
            record_pass("P8-21", f"Cross-user isolation: User B cannot delete User A's address (HTTP {st})")
        else:
            record_fail("P8-21", "Cross-user delete isolation", f"Expected 403 or 400, got {st}: {res}")
    except Exception as e:
        record_fail("P8-21", "Cross-user delete isolation", str(e))

    # 22. Address Deletion: User A can delete own address (DELETE /api/addresses/{addr_a2_id})
    try:
        st, res = api_call("DELETE", f"/addresses/{addr_a2_id}", token=token_a)
        if st == 200 and res.get("success"):
            # Verify it cannot be retrieved now
            st_check, _ = api_call("GET", f"/addresses/{addr_a2_id}", token=token_a)
            if st_check in (400, 404):
                record_pass("P8-22", f"DELETE /api/addresses/{addr_a2_id} successfully deleted address")
            else:
                record_fail("P8-22", "Delete address check", f"Address still found: {st_check}")
        else:
            record_fail("P8-22", "Delete address", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-22", "Delete address", str(e))

    # -------------------------------------------------------------------------
    # PART 3: CHECKOUT VALIDATION & SERVER-SIDE ENFORCEMENT
    # -------------------------------------------------------------------------

    # 23. Checkout with Empty Cart Rejected (POST /api/orders -> 400)
    try:
        # User A's cart is empty currently
        st, res = api_call("POST", "/orders", {
            "addressId": addr_a1_id,
            "paymentMethod": "COD"
        }, token=token_a)
        if st == 400 and ("empty" in str(res).lower() or "cart" in str(res).lower()):
            record_pass("P8-23", "Checkout with empty shopping cart rejected (HTTP 400)")
        else:
            record_fail("P8-23", "Empty cart checkout rejection", f"Expected 400, got {st}: {res}")
    except Exception as e:
        record_fail("P8-23", "Empty cart checkout rejection", str(e))

    # Add item to User A's cart for further tests: Product 1 (Urea, sellingPrice=266.50)
    api_call("POST", "/cart/items", {"fertilizerId": 1, "quantity": 2}, token=token_a)

    # 24. Checkout with Non-Existent Address Rejected (HTTP 400/404)
    try:
        st, res = api_call("POST", "/orders", {
            "addressId": 999999,
            "paymentMethod": "COD"
        }, token=token_a)
        if st in (400, 404):
            record_pass("P8-24", "Checkout with non-existent address ID rejected (HTTP 400/404)")
        else:
            record_fail("P8-24", "Non-existent address checkout", f"Expected 400/404, got {st}: {res}")
    except Exception as e:
        record_fail("P8-24", "Non-existent address checkout", str(e))

    # 25. Create Address for User B and attempt User A checkout with User B's Address (Rejected)
    addr_b_id = None
    try:
        _, res_b_addr = api_call("POST", "/addresses", {
            "fullName": "Suresh Deshmukh Farm",
            "phone": "9822001122",
            "addressLine1": "Gat No 88, Green Fields",
            "city": "Baramati",
            "district": "Pune",
            "state": "Maharashtra",
            "pincode": "413102",
            "addressType": "FARM",
            "default": True
        }, token=token_b)
        addr_b_id = res_b_addr["data"]["id"]

        st, res = api_call("POST", "/orders", {
            "addressId": addr_b_id, # User B's address
            "paymentMethod": "COD"
        }, token=token_a)
        if st in (400, 403):
            record_pass("P8-25", "Checkout with another user's address rejected (HTTP 400/403)")
        else:
            record_fail("P8-25", "Cross-user address in checkout", f"Expected 400 or 403, got {st}: {res}")
    except Exception as e:
        record_fail("P8-25", "Cross-user address in checkout", str(e))

    # 26. Price Manipulation Prevention: Client passes tampered expectedGrandTotal (Rejected)
    try:
        st, res = api_call("POST", "/orders", {
            "addressId": addr_a1_id,
            "paymentMethod": "COD",
            "expectedGrandTotal": 1.00 # Tampered total!
        }, token=token_a)
        if st == 400 and ("price" in str(res).lower() or "changed" in str(res).lower()):
            record_pass("P8-26", "Tampered expectedGrandTotal rejected with price change alert (HTTP 400)")
        else:
            record_fail("P8-26", "Tampered expectedGrandTotal check", f"Expected 400 with price message, got {st}: {res}")
    except Exception as e:
        record_fail("P8-26", "Tampered expectedGrandTotal check", str(e))

    # 27. Price Manipulation Prevention: Client passes tampered expectedItemPrices (Rejected)
    try:
        st, res = api_call("POST", "/orders", {
            "addressId": addr_a1_id,
            "paymentMethod": "COD",
            "expectedItemPrices": {1: 5.00} # Tampered unit price for product 1!
        }, token=token_a)
        if st == 400 and ("price" in str(res).lower() or "changed" in str(res).lower()):
            record_pass("P8-27", "Tampered expectedItemPrices rejected with price change alert (HTTP 400)")
        else:
            record_fail("P8-27", "Tampered expectedItemPrices check", f"Expected 400 with price message, got {st}: {res}")
    except Exception as e:
        record_fail("P8-27", "Tampered expectedItemPrices check", str(e))

    # 28. Stock Revalidation: Cart item quantity exceeding available stock rejected
    try:
        # Check stock of Product 2
        p2_stock_str = mysql_query("SELECT stock FROM fertilizer WHERE id = 2;")
        p2_stock = int(p2_stock_str) if p2_stock_str else 50
        # Add Product 2 with normal quantity to User B's cart
        api_call("POST", "/cart/items", {"fertilizerId": 2, "quantity": 1}, token=token_b)
        # Artificially set Product 2 stock to 0 in MySQL to simulate out-of-stock between cart and checkout
        mysql_query("UPDATE fertilizer SET stock = 0 WHERE id = 2;")
        st, res = api_call("POST", "/orders", {
            "addressId": addr_b_id,
            "paymentMethod": "COD"
        }, token=token_b)
        if st == 400 and ("stock" in str(res).lower() or "insufficient" in str(res).lower()):
            record_pass("P8-28", "Checkout revalidation catches out-of-stock product and rejects (HTTP 400)")
        else:
            record_fail("P8-28", "Stock deficit checkout rejection", f"Expected 400, got {st}: {res}")
        # Restore Product 2 stock and clear User B's cart
        mysql_query(f"UPDATE fertilizer SET stock = {p2_stock} WHERE id = 2;")
        api_call("DELETE", "/cart/clear", token=token_b)
    except Exception as e:
        record_fail("P8-28", "Stock deficit checkout rejection", str(e))

    # 29. Inactive Product Revalidation: Inactive product in cart rejected
    try:
        api_call("POST", "/cart/items", {"fertilizerId": 3, "quantity": 1}, token=token_b)
        # Deactivate Product 3 in MySQL
        mysql_query("UPDATE fertilizer SET active = 0 WHERE id = 3;")
        st, res = api_call("POST", "/orders", {
            "addressId": addr_b_id,
            "paymentMethod": "COD"
        }, token=token_b)
        if st == 400 and ("inactive" in str(res).lower() or "cannot be ordered" in str(res).lower()):
            record_pass("P8-29", "Checkout revalidation catches inactive product and rejects (HTTP 400)")
        else:
            record_fail("P8-29", "Inactive product checkout rejection", f"Expected 400, got {st}: {res}")
        # Reactivate Product 3 and clear cart
        mysql_query("UPDATE fertilizer SET active = 1 WHERE id = 3;")
        api_call("DELETE", "/cart/clear", token=token_b)
    except Exception as e:
        record_fail("P8-29", "Inactive product checkout rejection", str(e))

    # -------------------------------------------------------------------------
    # PART 4: SUCCESSFUL ORDER PLACEMENT, CALCULATIONS & SNAPSHOTS
    # -------------------------------------------------------------------------

    # 30. Successful Order Placement: Under Threshold (< ₹1000) standard shipping fee applied (₹50)
    # User A currently has Product 1 (qty 2, price 266.50 -> net subtotal 533.00 < 1000)
    order_a1_dto = None
    initial_p1_stock = int(mysql_query("SELECT stock FROM fertilizer WHERE id = 1;"))
    try:
        st, res = api_call("POST", "/orders", {
            "addressId": addr_a1_id,
            "paymentMethod": "COD",
            "notes": "Please call before dispatching"
        }, token=token_a)
        if st in (200, 201) and res.get("success"):
            order_a1_dto = res["data"]
            # Verify shipping fee is 50.00
            shipping = float(order_a1_dto["shippingAmount"])
            if shipping == 50.0:
                record_pass("P8-30", "Order placed with Net < ₹1000 has standard shipping fee (₹50.00)")
            else:
                record_fail("P8-30", "Under-threshold shipping calculation", f"Expected 50.00, got {shipping}")
        else:
            record_fail("P8-30", "Order placement under threshold", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-30", "Order placement under threshold", str(e))

    # 31. Verify Stock Deduction in MySQL
    try:
        new_p1_stock = int(mysql_query("SELECT stock FROM fertilizer WHERE id = 1;"))
        if new_p1_stock == initial_p1_stock - 2:
            record_pass("P8-31", f"Authoritative stock deducted in MySQL: {initial_p1_stock} -> {new_p1_stock}")
        else:
            record_fail("P8-31", "Stock deduction verification", f"Expected {initial_p1_stock - 2}, got {new_p1_stock}")
    except Exception as e:
        record_fail("P8-31", "Stock deduction verification", str(e))

    # 32. Verify Cart Cleared in MySQL
    try:
        cart_items_count = int(mysql_query(f"SELECT COUNT(*) FROM cart_items ci JOIN cart c ON ci.cart_id = c.id WHERE c.user_id = {user_a_id};"))
        if cart_items_count == 0:
            record_pass("P8-32", "Shopping cart cleared in MySQL upon successful order placement")
        else:
            record_fail("P8-32", "Cart clearing verification", f"Cart items remaining: {cart_items_count}")
    except Exception as e:
        record_fail("P8-32", "Cart clearing verification", str(e))

    # 33. Verify Complete Address Snapshot in Order (including District)
    try:
        if order_a1_dto:
            snap_match = (
                order_a1_dto["recipientName"] == "Ramesh Kisan Patel" and
                order_a1_dto["recipientPhone"] == "9876543210" and
                order_a1_dto["city"] == "Shirur" and
                order_a1_dto["district"] == "Pune" and
                order_a1_dto["state"] == "Maharashtra" and
                order_a1_dto["pincode"] == "412403" and
                order_a1_dto["addressType"] == "FARM"
            )
            if snap_match:
                record_pass("P8-33", "Order correctly captured immutable address snapshot with district")
            else:
                record_fail("P8-33", "Address snapshot verification", f"Snapshot data: {order_a1_dto}")
        else:
            record_fail("P8-33", "Address snapshot verification", "Order DTO was None")
    except Exception as e:
        record_fail("P8-33", "Address snapshot verification", str(e))

    # 34. Address Immutability: Mutating saved address does NOT affect historical order
    try:
        # Update addr_a1 to new city and district
        api_call("PUT", f"/addresses/{addr_a1_id}", {
            "fullName": "Ramesh Altered Name",
            "phone": "9999988888",
            "addressLine1": "New Road 99",
            "city": "Nagpur",
            "district": "Nagpur",
            "state": "Maharashtra",
            "pincode": "440001",
            "addressType": "WORK"
        }, token=token_a)

        # Retrieve order again and check snapshot
        st, res_order = api_call("GET", f"/orders/{order_a1_dto['id']}", token=token_a)
        refetched_order = res_order["data"]
        if refetched_order["recipientName"] == "Ramesh Kisan Patel" and refetched_order["district"] == "Pune":
            record_pass("P8-34", "Historical order address snapshot remains immutable after address record edit")
        else:
            record_fail("P8-34", "Address immutability verification", f"Altered in order: {refetched_order}")
    except Exception as e:
        record_fail("P8-34", "Address immutability verification", str(e))

    # 35. Free Shipping Threshold Qualification (Net >= ₹1000 -> Shipping = ₹0.00)
    # Add 5 units of Product 1 (5 * 266.50 = 1332.50 >= 1000) to User A's cart
    order_a2_dto = None
    try:
        api_call("POST", "/cart/items", {"fertilizerId": 1, "quantity": 5}, token=token_a)
        st, res = api_call("POST", "/orders", {
            "addressId": addr_a1_id,
            "paymentMethod": "COD"
        }, token=token_a)
        if st in (200, 201) and res.get("success"):
            order_a2_dto = res["data"]
            shipping = float(order_a2_dto["shippingAmount"])
            if shipping == 0.0:
                record_pass("P8-35", "Order placed with Net >= ₹1000 qualifies for FREE shipping (₹0.00)")
            else:
                record_fail("P8-35", "Free shipping calculation", f"Expected 0.00, got {shipping}")
        else:
            record_fail("P8-35", "Free shipping order placement", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-35", "Free shipping order placement", str(e))

    # 36. Authoritative GST Tax Calculation (5% on Net Subtotal)
    try:
        if order_a2_dto:
            subtotal = Decimal(str(order_a2_dto["subtotal"]))
            discount = Decimal(str(order_a2_dto["discountAmount"]))
            net = subtotal - discount
            expected_tax = (net * Decimal("0.05")).quantize(Decimal("0.01"))
            actual_tax = Decimal(str(order_a2_dto["taxAmount"])).quantize(Decimal("0.01"))
            expected_grand_total = (net + expected_tax + Decimal(str(order_a2_dto["shippingAmount"]))).quantize(Decimal("0.01"))
            actual_grand_total = Decimal(str(order_a2_dto["grandTotal"])).quantize(Decimal("0.01"))

            if actual_tax == expected_tax and actual_grand_total == expected_grand_total:
                record_pass("P8-36", f"GST 5% tax (₹{actual_tax}) and grand total (₹{actual_grand_total}) match authoritative server formulas")
            else:
                record_fail("P8-36", "GST and total formula check", f"Tax: exp={expected_tax}, act={actual_tax}. Total: exp={expected_grand_total}, act={actual_grand_total}")
        else:
            record_fail("P8-36", "GST formula check", "Order DTO was None")
    except Exception as e:
        record_fail("P8-36", "GST formula check", str(e))

    # 37. Online Payment Method Checkout (Payment Method ONLINE)
    order_b_online_dto = None
    try:
        api_call("DELETE", "/cart/clear", token=token_b)
        api_call("POST", "/cart/items", {"fertilizerId": 5, "quantity": 1}, token=token_b)
        st, res = api_call("POST", "/orders", {
            "addressId": addr_b_id,
            "paymentMethod": "ONLINE",
            "paymentReference": "TXN_MOCK_TEST_9988"
        }, token=token_b)
        if st in (200, 201) and res.get("success"):
            order_b_online_dto = res["data"]
            if order_b_online_dto["paymentMethod"] == "ONLINE":
                record_pass("P8-37", f"Order placed with ONLINE payment method (Order #{order_b_online_dto['orderNumber']})")
            else:
                record_fail("P8-37", "ONLINE payment method check", f"Method: {order_b_online_dto['paymentMethod']}")
        else:
            record_fail("P8-37", "ONLINE payment order placement", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-37", "ONLINE payment order placement", str(e))

    # 38. Phase 5 Delivery Tracking Auto-Initialized for Phase 8 Order
    try:
        if order_b_online_dto:
            delivery_rec = mysql_query(f"SELECT tracking_number, delivery_status FROM deliveries WHERE order_id = {order_b_online_dto['id']};")
            if delivery_rec and ("ORDER_PLACED" in delivery_rec or "NOT_ASSIGNED" in delivery_rec):
                record_pass("P8-38", f"Delivery tracking auto-initialized: {delivery_rec}")
            else:
                record_fail("P8-38", "Delivery tracking check", f"Result: {delivery_rec}")
        else:
            record_fail("P8-38", "Delivery tracking check", "Order DTO was None")
    except Exception as e:
        record_fail("P8-38", "Delivery tracking check", str(e))

    # 39. Phase 5 Payment Record Auto-Initialized for Phase 8 Order
    try:
        if order_b_online_dto:
            pay_rec = mysql_query(f"SELECT payment_method, payment_status FROM payments WHERE order_id = {order_b_online_dto['id']};")
            if pay_rec and "ONLINE" in pay_rec:
                record_pass("P8-39", f"Payment record auto-initialized: {pay_rec}")
            else:
                record_fail("P8-39", "Payment record check", f"Result: {pay_rec}")
        else:
            record_fail("P8-39", "Payment record check", "Order DTO was None")
    except Exception as e:
        record_fail("P8-39", "Payment record check", str(e))

    # 40. Order Cancellation Restores Stock in MySQL
    try:
        # Get Product 5 stock before cancellation
        p5_stock_before = int(mysql_query("SELECT stock FROM fertilizer WHERE id = 5;"))
        # Cancel order
        st, res = api_call("PUT", f"/orders/{order_b_online_dto['id']}/cancel", token=token_b)
        if st == 200 and res.get("success"):
            p5_stock_after = int(mysql_query("SELECT stock FROM fertilizer WHERE id = 5;"))
            if p5_stock_after == p5_stock_before + 1:
                record_pass("P8-40", f"Order cancellation restored product stock in MySQL ({p5_stock_before} -> {p5_stock_after})")
            else:
                record_fail("P8-40", "Stock restoration check", f"Stock before: {p5_stock_before}, after: {p5_stock_after}")
        else:
            record_fail("P8-40", "Cancel order API", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-40", "Cancel order API", str(e))

    # 41. Customer Orders List (GET /api/orders) contains District
    try:
        st, res = api_call("GET", "/orders", token=token_a)
        if st == 200 and res.get("success") and len(res["data"]) >= 1:
            first_ord = res["data"][0]
            if "district" in first_ord and first_ord["district"] is not None:
                record_pass("P8-41", f"Customer orders list contains district field: '{first_ord['district']}'")
            else:
                record_fail("P8-41", "Orders list district check", f"Order data: {first_ord}")
        else:
            record_fail("P8-41", "Customer orders list", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-41", "Customer orders list", str(e))

    # 42. Order Details (GET /api/orders/{id}) contains Items Snapshot and Totals
    try:
        st, res = api_call("GET", f"/orders/{order_a1_dto['id']}", token=token_a)
        if st == 200 and res.get("success"):
            dto = res["data"]
            if dto.get("items") and len(dto["items"]) > 0 and dto["items"][0].get("productName"):
                record_pass("P8-42", f"Order details contains line items with product snapshot: {dto['items'][0]['productName']}")
            else:
                record_fail("P8-42", "Order details snapshot items check", f"Data: {dto}")
        else:
            record_fail("P8-42", "Order details API", f"Got {st}: {res}")
    except Exception as e:
        record_fail("P8-42", "Order details API", str(e))

    print("======================================================================")
    print(f"PHASE 8 TESTS COMPLETED: {passed_count} PASSED, {failed_count} FAILED (TOTAL: {passed_count + failed_count})")
    print("======================================================================")
    return failed_count == 0

if __name__ == '__main__':
    success = run_tests()
    sys.exit(0 if success else 1)
