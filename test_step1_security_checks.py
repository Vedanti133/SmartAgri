import urllib.request
import urllib.error
import json
import sys
import subprocess

BASE_URL = "http://localhost:8080/api"

def api_call(method, endpoint, payload=None, token=None):
    url = f"{BASE_URL}{endpoint}"
    data = json.dumps(payload).encode('utf-8') if payload else None
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

def run_tests():
    print("=" * 70)
    print("SMARTAGRI STEP 1: SINGLE ADMIN ACCOUNT SECURITY VERIFICATION")
    print("=" * 70)

    passes = 0
    failures = 0

    # ---------------------------------------------------------
    # TEST 1: Login with Rahul (Username: rahul, Password: rahul)
    # Expected: SUCCESS -> ADMIN access
    # ---------------------------------------------------------
    print("\n[TEST 1] Login with Rahul Kale (rahul / rahul)...")
    status, res = api_call("POST", "/auth/login", {"username": "rahul", "password": "rahul"})
    rahul_token = None
    if status == 200 and res.get("success") is True:
        data = res.get("data", {})
        user_role = data.get("role")
        rahul_token = data.get("token")
        if user_role == "ADMIN" and rahul_token:
            print(f"  --> PASS: Login successful! Role={user_role}, Token received.")
            passes += 1
        else:
            print(f"  --> FAIL: Expected role ADMIN, got {user_role}")
            failures += 1
    else:
        print(f"  --> FAIL: Status {status}, Response: {res}")
        failures += 1

    # Verify Rahul can access protected Admin endpoints
    if rahul_token:
        print("  Checking Admin authorization on /api/users...")
        u_status, u_res = api_call("GET", "/users", token=rahul_token)
        if u_status == 200:
            print(f"  --> PASS: Admin access authorized on /api/users (HTTP {u_status})")
            passes += 1
        else:
            print(f"  --> FAIL: Admin access denied on /api/users: HTTP {u_status}")
            failures += 1

    # ---------------------------------------------------------
    # TEST 2: Attempt to create another user with Role: ADMIN
    # Expected: REJECTED
    # ---------------------------------------------------------
    print("\n[TEST 2] Attempt to create another user with Role: ADMIN...")
    # Attempt 2A: Via public registration /auth/register
    status_2a, res_2a = api_call("POST", "/auth/register", {
        "username": "fake_admin_user",
        "name": "Fake Admin",
        "email": "fake_admin@example.com",
        "password": "password123",
        "role": "ADMIN"
    })
    if status_2a in [400, 403]:
        print(f"  --> PASS: Public registration with Role: ADMIN rejected (HTTP {status_2a}): {res_2a.get('message')}")
        passes += 1
    else:
        print(f"  --> FAIL: Public registration allowed another ADMIN! HTTP {status_2a}: {res_2a}")
        failures += 1

    # Attempt 2B: Via admin user creation API /api/users with Rahul's token
    if rahul_token:
        status_2b, res_2b = api_call("POST", "/users", {
            "username": "second_admin",
            "name": "Second Admin",
            "email": "second_admin@example.com",
            "password": "password123",
            "role": "ADMIN"
        }, token=rahul_token)
        if status_2b in [400, 403]:
            print(f"  --> PASS: Admin API creation of another ADMIN rejected (HTTP {status_2b}): {res_2b.get('message')}")
            passes += 1
        else:
            print(f"  --> FAIL: Admin API allowed creation of another ADMIN! HTTP {status_2b}: {res_2b}")
            failures += 1

    # ---------------------------------------------------------
    # TEST 3: Attempt to change an existing non-admin user to ADMIN
    # Expected: REJECTED
    # ---------------------------------------------------------
    print("\n[TEST 3] Attempt to change an existing non-admin user to ADMIN...")
    # Find a non-admin user ID
    target_user_id = None
    if rahul_token:
        _, users_list = api_call("GET", "/users", token=rahul_token)
        user_items = users_list.get("data", [])
        for u in user_items:
            if u.get("role") != "ADMIN":
                target_user_id = u.get("id")
                break

    if target_user_id:
        # Attempt 3A: Via PUT /users/{id}/role
        status_3a, res_3a = api_call("PUT", f"/users/{target_user_id}/role", {"role": "ADMIN"}, token=rahul_token)
        if status_3a in [400, 403]:
            print(f"  --> PASS: Role update to ADMIN rejected (HTTP {status_3a}): {res_3a.get('message')}")
            passes += 1
        else:
            print(f"  --> FAIL: Changing user to ADMIN allowed! HTTP {status_3a}: {res_3a}")
            failures += 1

        # Attempt 3B: Via PUT /users/{id}
        status_3b, res_3b = api_call("PUT", f"/users/{target_user_id}", {"role": "ADMIN"}, token=rahul_token)
        if status_3b in [400, 403]:
            print(f"  --> PASS: Full user update to ADMIN rejected (HTTP {status_3b}): {res_3b.get('message')}")
            passes += 1
        else:
            print(f"  --> FAIL: Full update to ADMIN allowed! HTTP {status_3b}: {res_3b}")
            failures += 1
    else:
        print("  --> SKIP: No non-admin user found to test role change")

    # ---------------------------------------------------------
    # TEST 4: Attempt to create another ADMIN using different username but Rahul's email
    # Expected: REJECTED
    # ---------------------------------------------------------
    print("\n[TEST 4] Attempt to create another ADMIN using different username ('imposter') but Rahul's email ('rahul@gmail.com')...")
    status_4, res_4 = api_call("POST", "/auth/register", {
        "username": "imposter_admin",
        "name": "Imposter Admin",
        "email": "rahul@gmail.com",
        "password": "password123",
        "role": "ADMIN"
    })
    if status_4 in [400, 403]:
        print(f"  --> PASS: Rejected (HTTP {status_4}): {res_4.get('message')}")
        passes += 1
    else:
        print(f"  --> FAIL: Allowed! HTTP {status_4}: {res_4}")
        failures += 1

    # ---------------------------------------------------------
    # TEST 5: Attempt to create another ADMIN using another email but username rahul
    # Expected: REJECTED
    # ---------------------------------------------------------
    print("\n[TEST 5] Attempt to create another ADMIN using another email ('imposter@gmail.com') but username 'rahul'...")
    status_5, res_5 = api_call("POST", "/auth/register", {
        "username": "rahul",
        "name": "Imposter Rahul",
        "email": "imposter@gmail.com",
        "password": "password123",
        "role": "ADMIN"
    })
    if status_5 in [400, 403]:
        print(f"  --> PASS: Rejected (HTTP {status_5}): {res_5.get('message')}")
        passes += 1
    else:
        print(f"  --> FAIL: Allowed! HTTP {status_5}: {res_5}")
        failures += 1

    # ---------------------------------------------------------
    # TEST 6: Verify database contains exactly ONE ADMIN
    # Expected: Rahul Kale / rahul / rahul@gmail.com -> ADMIN
    # ---------------------------------------------------------
    print("\n[TEST 6] Verify database contains exactly ONE ADMIN...")
    mysql_cmd = [
        r"C:\xampp\mysql\bin\mysql.exe",
        "-u", "root",
        "-psamu@123",
        "smartagri",
        "-e",
        "SELECT id, username, name, email, role, active FROM users WHERE role = 'ADMIN';"
    ]
    proc = subprocess.run(mysql_cmd, capture_output=True, text=True)
    out = proc.stdout.strip()
    print("Database Query Output:\n" + out)
    lines = [l for l in out.splitlines() if l.strip()]
    # First line is header, second line is record
    if len(lines) == 2:
        data_line = lines[1]
        if "rahul" in data_line and "Rahul Kale" in data_line and "rahul@gmail.com" in data_line and "ADMIN" in data_line:
            print("  --> PASS: Database contains EXACTLY ONE ADMIN: Rahul Kale / rahul / rahul@gmail.com")
            passes += 1
        else:
            print("  --> FAIL: Admin row content does not match expected Rahul Kale credentials")
            failures += 1
    else:
        print(f"  --> FAIL: Expected 1 admin record, found {len(lines) - 1} records")
        failures += 1

    # Test DB engine trigger protection against raw SQL injection/tampering
    print("\n[TEST 6B] Database trigger protection: Attempt direct SQL insert of non-Rahul ADMIN...")
    sql_tamper = "INSERT INTO users (username, name, email, password, role, active) VALUES ('sql_admin', 'SQL Admin', 'sql_admin@test.com', 'hash', 'ADMIN', 1);"
    proc_trigger = subprocess.run([
        r"C:\xampp\mysql\bin\mysql.exe",
        "-u", "root",
        "-psamu@123",
        "smartagri",
        "-e",
        sql_tamper
    ], capture_output=True, text=True)
    if proc_trigger.returncode != 0 and "Violation of Single Admin Policy" in proc_trigger.stderr:
        print(f"  --> PASS: Database trigger BLOCKED raw SQL insert of unauthorized admin: {proc_trigger.stderr.strip()}")
        passes += 1
    else:
        print(f"  --> FAIL: Database trigger did not block unauthorized admin insert! returncode={proc_trigger.returncode}")
        failures += 1

    # ---------------------------------------------------------
    # TEST 7: Verify existing non-admin login and functionality still work
    # Expected: NO REGRESSION
    # ---------------------------------------------------------
    print("\n[TEST 7] Verify existing non-admin login (farmer_ramesh / ramesh123)...")
    c_status, c_res = api_call("POST", "/auth/login", {"username": "farmer_ramesh", "password": "ramesh123"})
    if c_status == 200 and c_res.get("success") is True:
        c_data = c_res.get("data", {})
        c_role = c_data.get("role")
        c_token = c_data.get("token")
        if c_role == "CUSTOMER" and c_token:
            print(f"  --> PASS: Customer login successful! Role={c_role}")
            passes += 1

            # Test catalog endpoint access with customer token
            cat_status, cat_res = api_call("GET", "/fertilizers", token=c_token)
            if cat_status == 200:
                print(f"  --> PASS: Customer access to /api/fertilizers catalog verified (HTTP 200)")
                passes += 1
            else:
                print(f"  --> FAIL: Customer access to /api/fertilizers failed: HTTP {cat_status}")
                failures += 1

            # Test that customer CANNOT access admin endpoints
            adm_status, adm_res = api_call("GET", "/users", token=c_token)
            if adm_status == 403:
                print(f"  --> PASS: Customer correctly blocked from Admin endpoint /api/users (HTTP 403 Forbidden)")
                passes += 1
            else:
                print(f"  --> FAIL: Customer was NOT blocked from Admin endpoint! HTTP {adm_status}")
                failures += 1
        else:
            print(f"  --> FAIL: Expected role CUSTOMER, got {c_role}")
            failures += 1
    else:
        print(f"  --> FAIL: Customer login failed: HTTP {c_status}, Response: {c_res}")
        failures += 1

    # Test non-admin registration still works (e.g. new customer)
    print("\n[TEST 7B] Verify non-admin registration still works for valid roles...")
    import time
    new_user_username = f"new_farmer_{int(time.time())}"
    new_user_email = f"{new_user_username}@smartagri.com"
    r_status, r_res = api_call("POST", "/auth/register", {
        "username": new_user_username,
        "name": "New Farmer",
        "email": new_user_email,
        "password": "farmerpassword",
        "role": "CUSTOMER"
    })
    if r_status in [200, 201] and r_res.get("success") is True:
        print(f"  --> PASS: Non-admin registration (CUSTOMER) succeeded (HTTP {r_status})")
        passes += 1
    else:
        print(f"  --> FAIL: Non-admin registration failed! HTTP {r_status}: {r_res}")
        failures += 1

    print("\n" + "=" * 70)
    print(f"FINAL RESULT: {passes} PASSED, {failures} FAILED")
    print("=" * 70)
    return failures == 0

if __name__ == "__main__":
    success = run_tests()
    sys.exit(0 if success else 1)
