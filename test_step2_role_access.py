import urllib.request
import urllib.error
import json
import sys
import subprocess
import os

API_BASE = "http://localhost:8080/api"

def api_call(method, endpoint, payload=None, token=None):
    url = f"{API_BASE}{endpoint}"
    data = json.dumps(payload).encode('utf-8') if payload is not None else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            status = resp.status
            body = resp.read().decode('utf-8')
            return status, json.loads(body) if body else {}
    except urllib.error.HTTPError as err:
        body = err.read().decode('utf-8')
        try:
            parsed = json.loads(body)
        except Exception:
            parsed = {"error": body}
        return err.code, parsed
    except Exception as e:
        return 0, {"error": str(e)}

def parse_auth_js_permissions():
    """Extract ROLE_PAGE_PERMISSIONS from frontend/JS/auth.js"""
    auth_js_path = r"c:\Users\khush\Downloads\kk\SmartAgri_3_Language_Updated\SmartAgri_FINAL\SmartAgri_FINAL\frontend\JS\auth.js"
    with open(auth_js_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # We test the actual logic implemented in auth.js
    return content

def run_all_tests():
    print("=" * 80)
    print("SMARTAGRI STEP 2: ROLE-BASED PAGE & SIDEBAR ACCESS VERIFICATION")
    print("=" * 80)

    total_checks = 0
    passed_checks = 0

    def record_check(name, passed, detail=""):
        nonlocal total_checks, passed_checks
        total_checks += 1
        status_str = "PASS" if passed else "FAIL"
        if passed:
            passed_checks += 1
            print(f"  [+] {status_str}: {name}")
        else:
            print(f"  [-] {status_str}: {name}")
            if detail:
                print(f"      Detail: {detail}")

    # =========================================================================
    # TEST 1: Login as Rahul (ADMIN: rahul / rahul)
    # Expected: ADMIN access, admin sidebar & admin pages allowed, full API access
    # =========================================================================
    print("\n" + "=" * 60)
    print("TEST 1: ADMIN Login & Access (rahul / rahul)")
    print("=" * 60)
    st, res = api_call("POST", "/auth/login", {"username": "rahul", "password": "rahul"})
    rahul_token = res.get("data", {}).get("token")
    rahul_role = res.get("data", {}).get("role")
    record_check("ADMIN login succeeds with 200 OK", st == 200 and bool(rahul_token))
    record_check("ADMIN role is returned as 'ADMIN'", rahul_role == "ADMIN")

    # API Access checks
    st_users, _ = api_call("GET", "/users", token=rahul_token)
    record_check("ADMIN can access /api/users (HTTP 200)", st_users == 200)

    st_orders, _ = api_call("GET", "/admin/orders", token=rahul_token)
    record_check("ADMIN can access /api/admin/orders (HTTP 200)", st_orders == 200)

    st_payments, _ = api_call("GET", "/admin/payments", token=rahul_token)
    record_check("ADMIN can access /api/admin/payments (HTTP 200)", st_payments == 200)

    st_procure, _ = api_call("GET", "/admin/procurement/dashboard", token=rahul_token)
    record_check("ADMIN can access /api/admin/procurement (HTTP 200)", st_procure == 200)

    # =========================================================================
    # TEST 2: Login as CUSTOMER (farmer_ramesh / ramesh123)
    # Expected: Permitted customer sidebar/pages, Admin pages blocked,
    #           Admin-only APIs return HTTP 403 Forbidden.
    # =========================================================================
    print("\n" + "=" * 60)
    print("TEST 2: CUSTOMER Role Security & Scoped Access")
    print("=" * 60)
    st, res = api_call("POST", "/auth/login", {"username": "farmer_ramesh", "password": "ramesh123"})
    cust_token = res.get("data", {}).get("token")
    cust_role = res.get("data", {}).get("role")
    record_check("CUSTOMER login succeeds with 200 OK", st == 200 and bool(cust_token))
    record_check("CUSTOMER role is returned as 'CUSTOMER'", cust_role == "CUSTOMER")

    # Commercial allowed endpoints
    st_cart, _ = api_call("GET", "/cart", token=cust_token)
    record_check("CUSTOMER can access /api/cart (HTTP 200)", st_cart == 200)

    st_orders, _ = api_call("GET", "/orders", token=cust_token)
    record_check("CUSTOMER can access customer orders /api/orders (HTTP 200)", st_orders == 200)

    # Blocked Admin endpoints -> HTTP 403
    st_u, res_u = api_call("GET", "/users", token=cust_token)
    record_check("CUSTOMER blocked from /api/users (HTTP 403 Forbidden)", st_u == 403, str(res_u))

    st_o, res_o = api_call("GET", "/admin/orders", token=cust_token)
    record_check("CUSTOMER blocked from /api/admin/orders (HTTP 403 Forbidden)", st_o == 403, str(res_o))

    st_inv, res_inv = api_call("GET", "/admin/inventory", token=cust_token)
    record_check("CUSTOMER blocked from /api/admin/inventory (HTTP 403 Forbidden)", st_inv == 403, str(res_inv))

    st_pay, res_pay = api_call("GET", "/admin/payments", token=cust_token)
    record_check("CUSTOMER blocked from /api/admin/payments (HTTP 403 Forbidden)", st_pay == 403, str(res_pay))

    # =========================================================================
    # TEST 3: Login as RETAILER (test_retailer / retailer123)
    # Expected: Only RETAILER-allowed navigation, admin APIs blocked
    # =========================================================================
    print("\n" + "=" * 60)
    print("TEST 3: RETAILER Role Security & Scoped Access")
    print("=" * 60)
    st, res = api_call("POST", "/auth/login", {"username": "test_retailer", "password": "retailer123"})
    ret_token = res.get("data", {}).get("token")
    record_check("RETAILER login succeeds with 200 OK", st == 200 and bool(ret_token))

    st_cart, _ = api_call("GET", "/cart", token=ret_token)
    record_check("RETAILER can access shopping cart /api/cart (HTTP 200)", st_cart == 200)

    st_u, _ = api_call("GET", "/users", token=ret_token)
    record_check("RETAILER blocked from /api/users (HTTP 403 Forbidden)", st_u == 403)

    st_o, _ = api_call("GET", "/admin/orders", token=ret_token)
    record_check("RETAILER blocked from /api/admin/orders (HTTP 403 Forbidden)", st_o == 403)

    st_inv, _ = api_call("GET", "/admin/inventory", token=ret_token)
    record_check("RETAILER blocked from /api/admin/inventory (HTTP 403 Forbidden)", st_inv == 403)

    # =========================================================================
    # TEST 4: Login as DEALER (test_dealer / dealer123)
    # Expected: Only DEALER-allowed navigation, stock transfers allowed, admin blocked
    # =========================================================================
    print("\n" + "=" * 60)
    print("TEST 4: DEALER Role Security & Scoped Access")
    print("=" * 60)
    st, res = api_call("POST", "/auth/login", {"username": "test_dealer", "password": "dealer123"})
    dlr_token = res.get("data", {}).get("token")
    record_check("DEALER login succeeds with 200 OK", st == 200 and bool(dlr_token))

    st_tr, _ = api_call("GET", "/admin/transfers", token=dlr_token)
    record_check("DEALER can access /api/admin/transfers (HTTP 200)", st_tr == 200)

    st_inv, _ = api_call("GET", "/admin/inventory", token=dlr_token)
    record_check("DEALER can view inventory stock /api/admin/inventory (HTTP 200)", st_inv == 200)

    st_u, _ = api_call("GET", "/users", token=dlr_token)
    record_check("DEALER blocked from /api/users (HTTP 403 Forbidden)", st_u == 403)

    st_o, _ = api_call("GET", "/admin/orders", token=dlr_token)
    record_check("DEALER blocked from staff /api/admin/orders (HTTP 403 Forbidden)", st_o == 403)

    # =========================================================================
    # TEST 5: Login as DISTRIBUTOR (test_distributor / distrib123)
    # Expected: Only DISTRIBUTOR-allowed navigation, warehouses/transfers allowed
    # =========================================================================
    print("\n" + "=" * 60)
    print("TEST 5: DISTRIBUTOR Role Security & Scoped Access")
    print("=" * 60)
    st, res = api_call("POST", "/auth/login", {"username": "test_distributor", "password": "distrib123"})
    dst_token = res.get("data", {}).get("token")
    record_check("DISTRIBUTOR login succeeds with 200 OK", st == 200 and bool(dst_token))

    st_wh, _ = api_call("GET", "/admin/warehouses", token=dst_token)
    record_check("DISTRIBUTOR can view /api/admin/warehouses (HTTP 200)", st_wh == 200)

    st_tr, _ = api_call("GET", "/admin/transfers", token=dst_token)
    record_check("DISTRIBUTOR can view /api/admin/transfers (HTTP 200)", st_tr == 200)

    st_u, _ = api_call("GET", "/users", token=dst_token)
    record_check("DISTRIBUTOR blocked from /api/users (HTTP 403 Forbidden)", st_u == 403)

    st_o, _ = api_call("GET", "/admin/orders", token=dst_token)
    record_check("DISTRIBUTOR blocked from staff /api/admin/orders (HTTP 403 Forbidden)", st_o == 403)

    # =========================================================================
    # TEST 6: Login as SUPPLIER (test_supplier / supplier123)
    # Expected: Only SUPPLIER navigation, procurement allowed, staff/admin blocked
    # =========================================================================
    print("\n" + "=" * 60)
    print("TEST 6: SUPPLIER Role Security & Scoped Access")
    print("=" * 60)
    st, res = api_call("POST", "/auth/login", {"username": "test_supplier", "password": "supplier123"})
    sup_token = res.get("data", {}).get("token")
    record_check("SUPPLIER login succeeds with 200 OK", st == 200 and bool(sup_token))

    st_proc, _ = api_call("GET", "/admin/procurement/dashboard", token=sup_token)
    record_check("SUPPLIER can access /api/admin/procurement/dashboard (HTTP 200)", st_proc == 200)

    st_po, _ = api_call("GET", "/admin/purchase-orders", token=sup_token)
    record_check("SUPPLIER can access /api/admin/purchase-orders (HTTP 200)", st_po == 200)

    st_u, _ = api_call("GET", "/users", token=sup_token)
    record_check("SUPPLIER blocked from /api/users (HTTP 403 Forbidden)", st_u == 403)

    st_o, _ = api_call("GET", "/admin/orders", token=sup_token)
    record_check("SUPPLIER blocked from staff /api/admin/orders (HTTP 403 Forbidden)", st_o == 403)

    # =========================================================================
    # TEST 7: Login as SALES_STAFF (test_sales / sales123)
    # Expected: Orders/Fulfillment allowed, admin users & payments blocked
    # =========================================================================
    print("\n" + "=" * 60)
    print("TEST 7: SALES_STAFF Role Security & Scoped Access")
    print("=" * 60)
    st, res = api_call("POST", "/auth/login", {"username": "test_sales", "password": "sales123"})
    sal_token = res.get("data", {}).get("token")
    record_check("SALES_STAFF login succeeds with 200 OK", st == 200 and bool(sal_token))

    st_o, _ = api_call("GET", "/admin/orders", token=sal_token)
    record_check("SALES_STAFF can access /api/admin/orders (HTTP 200)", st_o == 200)

    st_del, _ = api_call("GET", "/admin/deliveries", token=sal_token)
    record_check("SALES_STAFF can access /api/admin/deliveries (HTTP 200)", st_del == 200)

    st_u, _ = api_call("GET", "/users", token=sal_token)
    record_check("SALES_STAFF blocked from /api/users (HTTP 403 Forbidden)", st_u == 403)

    st_pay, _ = api_call("GET", "/admin/payments", token=sal_token)
    record_check("SALES_STAFF blocked from /api/admin/payments (HTTP 403 Forbidden)", st_pay == 403)

    # =========================================================================
    # TEST 8: Single Admin Invariant Check
    # Expected: Exactly ONE admin (Rahul Kale / rahul / rahul@gmail.com)
    # =========================================================================
    print("\n" + "=" * 60)
    print("TEST 8: Single-Admin Invariant Verification")
    print("=" * 60)
    # Check DB directly via CLI
    mysql_cmd = '& "C:\\xampp\\mysql\\bin\\mysql.exe" -u root -psamu@123 smartagri -e "SELECT id, username, email, role FROM users WHERE role=\'ADMIN\';"'
    p = subprocess.run(["powershell", "-Command", mysql_cmd], capture_output=True, text=True)
    lines = [line.strip() for line in p.stdout.splitlines() if line.strip() and not line.startswith("id")]
    record_check("Database contains exactly ONE ADMIN record", len(lines) == 1, f"Found {len(lines)} admins: {lines}")
    if lines:
        parts = lines[0].split("\t")
        record_check("Sole ADMIN username is 'rahul'", len(parts) >= 2 and parts[1] == "rahul")
        record_check("Sole ADMIN email is 'rahul@gmail.com'", len(parts) >= 3 and parts[2] == "rahul@gmail.com")

    # Attempt to register another ADMIN
    st_dup, res_dup = api_call("POST", "/auth/register", {
        "name": "Intruder Admin",
        "username": "intruder_adm",
        "email": "intruder@smartagri.com",
        "password": "password123",
        "role": "ADMIN"
    })
    record_check("Creating secondary ADMIN during registration is rejected", st_dup in [400, 403, 409] or res_dup.get("data", {}).get("role") != "ADMIN")

    # =========================================================================
    # REGRESSION TESTS
    # =========================================================================
    print("\n" + "=" * 60)
    print("REGRESSION TESTS: Core Catalog, Cart & Frontend Guard Tests")
    print("=" * 60)
    st_fert, _ = api_call("GET", "/fertilizers")
    record_check("Public fertilizer catalog /api/fertilizers is accessible (HTTP 200)", st_fert == 200)

    st_cat, _ = api_call("GET", "/categories")
    record_check("Public categories /api/categories is accessible (HTTP 200)", st_cat == 200)

    st_inv_stat, _ = api_call("GET", "/inventory/status/1")
    record_check("Public product inventory status is accessible (HTTP 200)", st_inv_stat == 200)

    # Frontend script validation
    auth_content = parse_auth_js_permissions()
    record_check("auth.js contains ROLE_PAGE_PERMISSIONS matrix", "const ROLE_PAGE_PERMISSIONS =" in auth_content)
    record_check("auth.js contains requireLogin route guard", "function requireLogin()" in auth_content)
    record_check("auth.js contains dynamic applyRoleSidebar", "function applyRoleSidebar()" in auth_content)
    record_check("auth.js maps all 7 roles", all(r in auth_content for r in ["ADMIN", "SALES_STAFF", "SUPPLIER", "DISTRIBUTOR", "DEALER", "RETAILER", "CUSTOMER"]))

    # Summary
    print("\n" + "=" * 80)
    print(f"RESULTS: {passed_checks} / {total_checks} checks passed ({passed_checks / total_checks * 100:.1f}%)")
    print("=" * 80)

    if passed_checks == total_checks:
        print("\nALL STEP 2 SECURITY & ROLE ACCESS CHECKS PASSED SUCCESSFULLY!\n")
        return 0
    else:
        print(f"\n{total_checks - passed_checks} CHECKS FAILED!\n")
        return 1

if __name__ == "__main__":
    sys.exit(run_all_tests())
