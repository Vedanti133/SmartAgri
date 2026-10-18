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
    print("SMARTAGRI PHASE 7 AUTOMATED TEST SUITE")
    print("Cart, Wishlist, Stock Validation & Customer Shopping Experience")
    print("======================================================================")

    ts = int(time.time())
    user_a_name = f"p7_user_a_{ts}"
    user_b_name = f"p7_user_b_{ts}"

    # 1. Unauthenticated Cart Access Rejected (HTTP 401)
    try:
        st, res = api_call("GET", "/cart")
        if st == 401:
            record_pass("P7-01", "Unauthenticated access to GET /api/cart rejected (HTTP 401)")
        else:
            record_fail("P7-01", "Unauthenticated cart access", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P7-01", "Unauthenticated cart access", str(e))

    # 2. Unauthenticated Wishlist Access Rejected (HTTP 401)
    try:
        st, res = api_call("GET", "/wishlist")
        if st == 401:
            record_pass("P7-02", "Unauthenticated access to GET /api/wishlist rejected (HTTP 401)")
        else:
            record_fail("P7-02", "Unauthenticated wishlist access", f"Expected 401, got {st}: {res}")
    except Exception as e:
        record_fail("P7-02", "Unauthenticated wishlist access", str(e))

    # 3. Customer User A & User B Registration and Login
    token_a = None
    user_a_id = None
    token_b = None
    user_b_id = None
    try:
        st_a, res_a = api_call("POST", "/auth/register", {
            "name": "Customer User A",
            "username": user_a_name,
            "email": f"{user_a_name}@example.com",
            "password": "Password123!",
            "role": "CUSTOMER"
        })
        token_a = (res_a.get("data") or {}).get("token")
        user_a_id = (res_a.get("data") or {}).get("id")

        st_b, res_b = api_call("POST", "/auth/register", {
            "name": "Customer User B",
            "username": user_b_name,
            "email": f"{user_b_name}@example.com",
            "password": "Password123!",
            "role": "CUSTOMER"
        })
        token_b = (res_b.get("data") or {}).get("token")
        user_b_id = (res_b.get("data") or {}).get("id")

        if token_a and user_a_id and token_b and user_b_id:
            record_pass("P7-03", f"Test customers registered successfully (User A ID: {user_a_id}, User B ID: {user_b_id})")
        else:
            record_fail("P7-03", "Customer registration", f"A: {res_a}, B: {res_b}")
    except Exception as e:
        record_fail("P7-03", "Customer registration", str(e))

    # 4. Empty Cart Retrieval
    try:
        st, res = api_call("GET", "/cart", token=token_a)
        data = res.get("data") or {}
        if st == 200 and res.get("success") and len(data.get("items", [])) == 0 and data.get("totalItemsCount") == 0:
            record_pass("P7-04", "Empty cart retrieval returns clean structure with 0 items and zero totals")
        else:
            record_fail("P7-04", "Empty cart retrieval", f"Status: {st}, Data: {data}")
    except Exception as e:
        record_fail("P7-04", "Empty cart retrieval", str(e))

    # 5. Empty Wishlist Retrieval
    try:
        st, res = api_call("GET", "/wishlist", token=token_a)
        data = res.get("data") or {}
        if st == 200 and res.get("success") and len(data.get("items", [])) == 0 and data.get("totalItemsCount") == 0:
            record_pass("P7-05", "Empty wishlist retrieval returns clean structure with 0 items")
        else:
            record_fail("P7-05", "Empty wishlist retrieval", f"Status: {st}, Data: {data}")
    except Exception as e:
        record_fail("P7-05", "Empty wishlist retrieval", str(e))

    # 6. Add Valid Item to Cart (Product ID 1, Quantity 2)
    cart_item_1_id = None
    try:
        st, res = api_call("POST", "/cart/items", {"fertilizerId": 1, "quantity": 2}, token=token_a)
        data = res.get("data") or {}
        items = data.get("items", [])
        if st == 200 and res.get("success") and len(items) == 1 and items[0].get("quantity") == 2:
            cart_item_1_id = items[0].get("id")
            record_pass("P7-06", f"Add product #1 (Qty 2) to cart succeeded (CartItemId: {cart_item_1_id})")
        else:
            record_fail("P7-06", "Add item to cart", f"Status: {st}, Res: {res}")
    except Exception as e:
        record_fail("P7-06", "Add item to cart", str(e))

    # 7. Cart Items Structure & DTO Verification (GET /api/cart/items)
    try:
        st, res = api_call("GET", "/cart/items", token=token_a)
        items = res.get("data") or []
        if st == 200 and res.get("success") and len(items) == 1:
            item = items[0]
            if item.get("name") and item.get("sellingPrice") and "npkRatio" in item and "available" in item:
                record_pass("P7-07", f"GET /api/cart/items returns correct DTO (Name: {item.get('name')}, Price: ₹{item.get('sellingPrice')}, NPK: {item.get('npkRatio')}, Available: {item.get('available')})")
            else:
                record_fail("P7-07", "Cart items DTO", f"Missing fields in item: {item}")
        else:
            record_fail("P7-07", "Cart items endpoint", f"Status: {st}, Res: {res}")
    except Exception as e:
        record_fail("P7-07", "Cart items endpoint", str(e))

    # 8. Increment Cart Item Quantity (Update to Qty 4)
    try:
        st, res = api_call("PUT", f"/cart/items/{cart_item_1_id}", {"quantity": 4}, token=token_a)
        data = res.get("data") or {}
        items = data.get("items", [])
        if st == 200 and res.get("success") and len(items) == 1 and items[0].get("quantity") == 4:
            line_tot = Decimal(str(items[0].get("lineTotal", "0")))
            unit_p = Decimal(str(items[0].get("effectivePrice", "0")))
            if line_tot == unit_p * 4:
                record_pass("P7-08", f"Increment cart item quantity to 4 updated lineTotal (₹{line_tot}) and cart count")
            else:
                record_fail("P7-08", "Quantity increment math", f"lineTotal={line_tot} != {unit_p * 4}")
        else:
            record_fail("P7-08", "Increment quantity", f"Status: {st}, Res: {res}")
    except Exception as e:
        record_fail("P7-08", "Increment quantity", str(e))

    # 9. Decrement Cart Item Quantity (Update to Qty 2)
    try:
        st, res = api_call("PUT", f"/cart/items/{cart_item_1_id}", {"quantity": 2}, token=token_a)
        data = res.get("data") or {}
        items = data.get("items", [])
        if st == 200 and res.get("success") and len(items) == 1 and items[0].get("quantity") == 2:
            record_pass("P7-09", "Decrement cart item quantity to 2 updated cart state successfully")
        else:
            record_fail("P7-09", "Decrement quantity", f"Status: {st}, Res: {res}")
    except Exception as e:
        record_fail("P7-09", "Decrement quantity", str(e))

    # 10. Add Second Product (Product ID 2, Quantity 3)
    cart_item_2_id = None
    try:
        st, res = api_call("POST", "/cart/items", {"fertilizerId": 2, "quantity": 3}, token=token_a)
        data = res.get("data") or {}
        items = data.get("items", [])
        tot_count = data.get("totalItemsCount")
        if st == 200 and res.get("success") and len(items) == 2 and tot_count == 5:
            cart_item_2_id = [i.get("id") for i in items if i.get("fertilizerId") == 2][0]
            record_pass("P7-10", f"Add second product succeeded: 2 distinct items, totalItemsCount = 5 (2 + 3)")
        else:
            record_fail("P7-10", "Add second product", f"Status: {st}, items: {len(items)}, tot_count: {tot_count}")
    except Exception as e:
        record_fail("P7-10", "Add second product", str(e))

    # 11. BigDecimal Currency & Tax Precision Verification
    try:
        st, res = api_call("GET", "/cart", token=token_a)
        data = res.get("data") or {}
        subtotal = Decimal(str(data.get("subtotal", "0")))
        disc = Decimal(str(data.get("discountAmount", "0")))
        net = Decimal(str(data.get("netAmount", "0")))
        tax = Decimal(str(data.get("taxAmount", "0")))
        grand = Decimal(str(data.get("grandTotal", "0")))

        # Check net = subtotal - disc
        # tax = net * 0.05
        # grand = net + tax
        expected_tax = (net * Decimal("0.05")).quantize(Decimal("0.01"))
        expected_grand = net + expected_tax

        if abs(tax - expected_tax) <= Decimal("0.01") and abs(grand - expected_grand) <= Decimal("0.01"):
            record_pass("P7-11", f"Cart BigDecimal accuracy: Subtotal ₹{subtotal}, Net ₹{net}, GST (5%) ₹{tax}, Grand Total ₹{grand}")
        else:
            record_fail("P7-11", "Cart calculation accuracy", f"Calculated tax={tax} vs {expected_tax}, grand={grand} vs {expected_grand}")
    except Exception as e:
        record_fail("P7-11", "Cart calculation accuracy", str(e))

    # 12. Stock Limit Validation on Add (Cannot exceed available stock)
    try:
        # Product 1 current stock in DB
        prod1_stock = int(mysql_query("SELECT stock FROM fertilizer WHERE id=1;"))
        excess_qty = prod1_stock + 100
        st, res = api_call("POST", "/cart/items", {"fertilizerId": 1, "quantity": excess_qty}, token=token_a)
        if st in (400, 422) and not res.get("success"):
            record_pass("P7-12", f"Adding quantity exceeding available stock ({excess_qty} > {prod1_stock}) rejected (HTTP {st})")
        else:
            record_fail("P7-12", "Stock limit on add", f"Expected 400/422, got {st}: {res}")
    except Exception as e:
        record_fail("P7-12", "Stock limit on add", str(e))

    # 13. Stock Limit Validation on Quantity Update (PUT /api/cart/items/{itemId})
    try:
        prod1_stock = int(mysql_query("SELECT stock FROM fertilizer WHERE id=1;"))
        excess_qty = prod1_stock + 50
        st, res = api_call("PUT", f"/cart/items/{cart_item_1_id}", {"quantity": excess_qty}, token=token_a)
        if st in (400, 422) and not res.get("success"):
            record_pass("P7-13", f"Updating quantity exceeding stock ({excess_qty} > {prod1_stock}) rejected (HTTP {st})")
        else:
            record_fail("P7-13", "Stock limit on update", f"Expected 400/422, got {st}: {res}")
    except Exception as e:
        record_fail("P7-13", "Stock limit on update", str(e))

    # 14. Out-of-Stock Product Add Rejection
    try:
        # Find product with stock == 0 or temporarily set one
        oos_id = mysql_query("SELECT id FROM fertilizer WHERE stock <= 0 LIMIT 1;")
        if not oos_id:
            # Pick product 50 and set stock to 0 temporarily
            oos_id = "50"
            mysql_query("UPDATE fertilizer SET stock=0 WHERE id=50;")
        oos_id = int(oos_id)

        st, res = api_call("POST", "/cart/items", {"fertilizerId": oos_id, "quantity": 1}, token=token_a)
        if st in (400, 422) and not res.get("success"):
            record_pass("P7-14", f"Adding out-of-stock product #{oos_id} rejected with clear error: {res.get('message')}")
        else:
            record_fail("P7-14", "Out-of-stock add", f"Expected error, got {st}: {res}")
    except Exception as e:
        record_fail("P7-14", "Out-of-stock add", str(e))

    # 15. Add Product to Wishlist (Product ID 3)
    wishlist_item_1_id = None
    try:
        st, res = api_call("POST", "/wishlist/items", {"fertilizerId": 3}, token=token_a)
        data = res.get("data") or {}
        items = data.get("items", [])
        if st == 200 and res.get("success") and len(items) == 1:
            wishlist_item_1_id = items[0].get("id")
            record_pass("P7-15", f"Add product #3 to wishlist succeeded (WishlistItemId: {wishlist_item_1_id})")
        else:
            record_fail("P7-15", "Add to wishlist", f"Status: {st}, Res: {res}")
    except Exception as e:
        record_fail("P7-15", "Add to wishlist", str(e))

    # 16. Wishlist Duplicate Prevention
    try:
        st, res = api_call("POST", "/wishlist/items", {"fertilizerId": 3}, token=token_a)
        # Should either return existing item without duplicating or 400 duplicate notice
        data = res.get("data") or {}
        items = data.get("items", []) if isinstance(data, dict) else []
        # Query DB directly to verify only 1 record exists
        count = int(mysql_query(f"SELECT COUNT(*) FROM wishlist_items wi JOIN wishlists w ON wi.wishlist_id=w.id WHERE w.user_id={user_a_id} AND wi.fertilizer_id=3;"))
        if count == 1:
            record_pass("P7-16", "Wishlist duplicate prevention: unique constraint preserved, exactly 1 entry exists in database")
        else:
            record_fail("P7-16", "Wishlist duplicate prevention", f"Count in DB is {count} (expected 1)")
    except Exception as e:
        record_fail("P7-16", "Wishlist duplicate prevention", str(e))

    # 17. Wishlist DTO & Details Verification (GET /api/wishlist)
    try:
        st, res = api_call("GET", "/wishlist", token=token_a)
        data = res.get("data") or {}
        items = data.get("items", [])
        if st == 200 and res.get("success") and len(items) == 1:
            item = items[0]
            has_fields = all(k in item for k in ["id", "fertilizerId", "fertilizerName", "brand", "sellingPrice", "stock", "npkRatio", "available"])
            if has_fields:
                record_pass("P7-17", f"Wishlist DTO contains all product details (Name: {item.get('fertilizerName')}, Brand: {item.get('brand')}, Price: ₹{item.get('sellingPrice')}, NPK: {item.get('npkRatio')})")
            else:
                record_fail("P7-17", "Wishlist DTO fields", f"Missing fields in: {item}")
        else:
            record_fail("P7-17", "Wishlist retrieval", f"Status: {st}, Res: {res}")
    except Exception as e:
        record_fail("P7-17", "Wishlist retrieval", str(e))

    # 18. Wishlist Product IDs Listing (GET /api/wishlist/product-ids)
    try:
        st, res = api_call("GET", "/wishlist/product-ids", token=token_a)
        ids = res.get("data") or []
        if st == 200 and res.get("success") and 3 in ids:
            record_pass("P7-18", f"GET /api/wishlist/product-ids returned product list: {ids}")
        else:
            record_fail("P7-18", "Wishlist product IDs", f"Status: {st}, Res: {res}")
    except Exception as e:
        record_fail("P7-18", "Wishlist product IDs", str(e))

    # 19. Add Product #4 & #5 to Wishlist
    wishlist_item_2_id = None
    try:
        api_call("POST", "/wishlist/items", {"fertilizerId": 4}, token=token_a)
        st, res = api_call("POST", "/wishlist/items", {"fertilizerId": 5}, token=token_a)
        data = res.get("data") or {}
        items = data.get("items", [])
        if st == 200 and len(items) == 3:
            wishlist_item_2_id = [i.get("id") for i in items if i.get("fertilizerId") == 4][0]
            record_pass("P7-19", f"Added multiple products to wishlist (Total in wishlist: {len(items)})")
        else:
            record_fail("P7-19", "Add multiple wishlist items", f"Status: {st}, Count: {len(items)}")
    except Exception as e:
        record_fail("P7-19", "Add multiple wishlist items", str(e))

    # 20. Remove Item from Wishlist by Item ID (DELETE /api/wishlist/items/{itemId})
    try:
        st, res = api_call("DELETE", f"/wishlist/items/{wishlist_item_2_id}", token=token_a)
        data = res.get("data") or {}
        items = data.get("items", [])
        if st == 200 and res.get("success") and not any(i.get("fertilizerId") == 4 for i in items):
            record_pass("P7-20", f"Removed item #{wishlist_item_2_id} (Product #4) from wishlist by itemId")
        else:
            record_fail("P7-20", "Remove wishlist item by ID", f"Status: {st}, Res: {res}")
    except Exception as e:
        record_fail("P7-20", "Remove wishlist item by ID", str(e))

    # 21. Remove Item from Wishlist by Product ID (DELETE /api/wishlist/items/product/{productId})
    try:
        st, res = api_call("DELETE", "/wishlist/items/product/5", token=token_a)
        data = res.get("data") or {}
        items = data.get("items", [])
        if st == 200 and res.get("success") and not any(i.get("fertilizerId") == 5 for i in items):
            record_pass("P7-21", "Removed product #5 from wishlist by productId endpoint")
        else:
            record_fail("P7-21", "Remove wishlist by productId", f"Status: {st}, Res: {res}")
    except Exception as e:
        record_fail("P7-21", "Remove wishlist by productId", str(e))

    # 22. Move Item to Cart from Wishlist by Item ID (POST /api/wishlist/items/{itemId}/move-to-cart)
    try:
        # Wishlist currently has Product #3 (wishlist_item_1_id)
        st, res = api_call("POST", f"/wishlist/items/{wishlist_item_1_id}/move-to-cart", token=token_a)

        # Check cart has product 3
        _, c_res = api_call("GET", "/cart", token=token_a)
        cart_items = (c_res.get("data") or {}).get("items", [])
        in_cart = any(i.get("fertilizerId") == 3 for i in cart_items)

        # Check wishlist no longer has product 3
        _, w_res = api_call("GET", "/wishlist", token=token_a)
        w_items = (w_res.get("data") or {}).get("items", [])
        in_wishlist = any(i.get("fertilizerId") == 3 for i in w_items)

        if st == 200 and res.get("success") and in_cart and not in_wishlist:
            record_pass("P7-22", f"Move item #{wishlist_item_1_id} (Product #3) to cart: Added to cart AND removed from wishlist")
        else:
            record_fail("P7-22", "Move to cart by itemId", f"Status: {st}, in_cart={in_cart}, in_wishlist={in_wishlist}")
    except Exception as e:
        record_fail("P7-22", "Move to cart by itemId", str(e))

    # 23. Move Item to Cart from Wishlist by Product ID (POST /api/wishlist/items/product/{productId}/move-to-cart)
    try:
        # Add Product #6 to wishlist first
        api_call("POST", "/wishlist/items", {"fertilizerId": 6}, token=token_a)
        st, res = api_call("POST", "/wishlist/items/product/6/move-to-cart", token=token_a)

        # Check cart has product 6
        _, c_res = api_call("GET", "/cart", token=token_a)
        cart_items = (c_res.get("data") or {}).get("items", [])
        in_cart = any(i.get("fertilizerId") == 6 for i in cart_items)

        _, w_res = api_call("GET", "/wishlist", token=token_a)
        w_items = (w_res.get("data") or {}).get("items", [])
        in_wishlist = any(i.get("fertilizerId") == 6 for i in w_items)

        if st == 200 and res.get("success") and in_cart and not in_wishlist:
            record_pass("P7-23", "Move product #6 to cart by productId endpoint succeeded")
        else:
            record_fail("P7-23", "Move to cart by productId", f"Status: {st}, in_cart={in_cart}, in_wishlist={in_wishlist}")
    except Exception as e:
        record_fail("P7-23", "Move to cart by productId", str(e))

    # 24. Remove Single Item from Cart (DELETE /api/cart/items/{itemId})
    try:
        st, res = api_call("DELETE", f"/cart/items/{cart_item_2_id}", token=token_a)
        cart_data = res.get("data") or {}
        cart_items = cart_data.get("items", [])
        if st == 200 and res.get("success") and not any(i.get("id") == cart_item_2_id for i in cart_items):
            record_pass("P7-24", f"Removed item #{cart_item_2_id} from cart; remaining items count: {len(cart_items)}")
        else:
            record_fail("P7-24", "Remove cart item", f"Status: {st}, Res: {res}")
    except Exception as e:
        record_fail("P7-24", "Remove cart item", str(e))

    # 25. Clear Entire Cart (DELETE /api/cart)
    try:
        st, res = api_call("DELETE", "/cart", token=token_a)
        cart_data = res.get("data") or {}
        cart_items = cart_data.get("items", [])
        if st == 200 and res.get("success") and len(cart_items) == 0 and cart_data.get("totalItemsCount") == 0:
            record_pass("P7-25", "Clear cart (DELETE /api/cart) emptied cart and reset totals to ₹0.00")
        else:
            record_fail("P7-25", "Clear cart", f"Status: {st}, Res: {res}")
    except Exception as e:
        record_fail("P7-25", "Clear cart", str(e))

    # 26. User Cart Isolation (User A's Cart items are invisible to User B)
    try:
        # Add item to User A's cart
        api_call("POST", "/cart/items", {"fertilizerId": 1, "quantity": 1}, token=token_a)

        # Retrieve User B's cart
        st, res = api_call("GET", "/cart", token=token_b)
        b_items = (res.get("data") or {}).get("items", [])
        if st == 200 and len(b_items) == 0:
            record_pass("P7-26", "User cart isolation: User B cannot see items present in User A's cart")
        else:
            record_fail("P7-26", "Cart isolation", f"User B has items: {b_items}")
    except Exception as e:
        record_fail("P7-26", "Cart isolation", str(e))

    # 27. User Wishlist Isolation (User A's Wishlist is invisible to User B)
    try:
        # Add item to User A's wishlist
        api_call("POST", "/wishlist/items", {"fertilizerId": 10}, token=token_a)

        # Retrieve User B's wishlist
        st, res = api_call("GET", "/wishlist", token=token_b)
        b_items = (res.get("data") or {}).get("items", [])
        if st == 200 and len(b_items) == 0:
            record_pass("P7-27", "User wishlist isolation: User B cannot see items in User A's wishlist")
        else:
            record_fail("P7-27", "Wishlist isolation", f"User B has wishlist items: {b_items}")
    except Exception as e:
        record_fail("P7-27", "Wishlist isolation", str(e))

    # 28. Cross-User Modification Forbidden (User B cannot modify or delete User A's cart item)
    try:
        # Get User A's cart item ID
        _, a_cart = api_call("GET", "/cart", token=token_a)
        a_item_id = a_cart["data"]["items"][0]["id"]

        # Attempt to delete User A's cart item using User B's token
        st, res = api_call("DELETE", f"/cart/items/{a_item_id}", token=token_b)
        if st in (400, 403, 404):
            record_pass("P7-28", f"Security: Cross-user cart modification rejected with HTTP {st}")
        else:
            record_fail("P7-28", "Cross-user cart modification", f"Expected error, got {st}: {res}")
    except Exception as e:
        record_fail("P7-28", "Cross-user cart modification", str(e))

    # 29. Cart Persistence across Sessions
    try:
        # Re-fetch User A's cart in a fresh session/request
        st, res = api_call("GET", "/cart", token=token_a)
        data = res.get("data") or {}
        items = data.get("items", [])
        if st == 200 and len(items) == 1 and items[0].get("fertilizerId") == 1:
            record_pass("P7-29", "Cart persistence: Cart contents preserved accurately across client sessions")
        else:
            record_fail("P7-29", "Cart persistence", f"Status: {st}, items: {items}")
    except Exception as e:
        record_fail("P7-29", "Cart persistence", str(e))

    # 30. Out-of-Stock Warning Notice & Availability Evaluation in Cart
    try:
        # Product 50 has stock 0. Add product 1 to cart, then temporarily set its stock to 0 in DB
        prod1_old_stock = int(mysql_query("SELECT stock FROM fertilizer WHERE id=1;"))
        mysql_query("UPDATE fertilizer SET stock=0 WHERE id=1;")

        st, res = api_call("GET", "/cart", token=token_a)
        data = res.get("data") or {}
        items = data.get("items", [])
        if len(items) > 0 and items[0].get("available") is False and "stock" in items[0].get("statusMessage", "").lower():
            record_pass("P7-30", f"Stock warning alert detected in cart item: '{items[0].get('statusMessage')}' (available=False)")
        else:
            record_fail("P7-30", "Cart stock warning alert", f"Item status: {items}")

        # Restore product 1 stock
        mysql_query(f"UPDATE fertilizer SET stock={prod1_old_stock} WHERE id=1;")
    except Exception as e:
        record_fail("P7-30", "Cart stock warning alert", str(e))
        mysql_query("UPDATE fertilizer SET stock=150 WHERE id=1;")

    # 31. Checkout Integration: Cart Order Placement & Automatic Cart Clearing
    try:
        # Create address for User A
        addr_data = {
            "fullName": "Phase 7 Tester",
            "phone": "9876543210",
            "addressLine1": "Test Farm Lane 7",
            "city": "Nashik",
            "state": "Maharashtra",
            "pincode": "422001",
            "addressType": "FARM",
            "isDefault": True
        }
        addr_st, addr_res = api_call("POST", "/addresses", addr_data, token=token_a)
        addr_id = (addr_res.get("data") or {}).get("id")

        # Place order
        order_st, order_res = api_call("POST", "/orders", {
            "addressId": addr_id,
            "paymentMethod": "COD",
            "notes": "Phase 7 cart checkout order verification"
        }, token=token_a)

        if order_st == 201 and order_res.get("success"):
            order_id = order_res.get("data", {}).get("id")

            # Verify cart was cleared after order placement
            _, cart_after = api_call("GET", "/cart", token=token_a)
            cleared_items = (cart_after.get("data") or {}).get("items", [])
            if len(cleared_items) == 0:
                record_pass("P7-31", f"Checkout integration: Order #{order_id} placed successfully and cart was automatically cleared")
            else:
                record_fail("P7-31", "Checkout cart clearing", f"Cart items remaining: {cleared_items}")
        else:
            record_fail("P7-31", "Checkout integration", f"Order creation failed: {order_res}")
    except Exception as e:
        record_fail("P7-31", "Checkout integration", str(e))

    # 32. Security Verification: Zero Passwords, Hashes, or Secrets Leaked in Cart/Wishlist
    try:
        _, raw_cart = api_call("GET", "/cart", token=token_a)
        _, raw_wish = api_call("GET", "/wishlist", token=token_a)
        combined_text = json.dumps(raw_cart) + json.dumps(raw_wish)
        sensitive_terms = ["password", "$2a$", "$2b$", "secret", "private_key", "cvv"]
        exposed = [term for term in sensitive_terms if term in combined_text.lower()]
        if not exposed:
            record_pass("P7-32", "Security verification: zero passwords, password hashes, or secrets exposed in cart/wishlist APIs")
        else:
            record_fail("P7-32", "Security verification", f"Sensitive tokens found: {exposed}")
    except Exception as e:
        record_fail("P7-32", "Security verification", str(e))

    # Cleanup test users
    try:
        for uid in [user_a_id, user_b_id]:
            if uid:
                mysql_query(f"DELETE FROM invoices WHERE order_id IN (SELECT id FROM orders WHERE user_id={uid});")
                mysql_query(f"DELETE FROM deliveries WHERE order_id IN (SELECT id FROM orders WHERE user_id={uid});")
                mysql_query(f"DELETE FROM payments WHERE order_id IN (SELECT id FROM orders WHERE user_id={uid});")
                mysql_query(f"DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE user_id={uid});")
                mysql_query(f"DELETE FROM orders WHERE user_id={uid};")
                mysql_query(f"DELETE FROM addresses WHERE user_id={uid};")
                mysql_query(f"DELETE FROM wishlist_items WHERE wishlist_id IN (SELECT id FROM wishlists WHERE user_id={uid});")
                mysql_query(f"DELETE FROM wishlists WHERE user_id={uid};")
                mysql_query(f"DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM cart WHERE user_id={uid});")
                mysql_query(f"DELETE FROM cart WHERE user_id={uid};")
                mysql_query(f"DELETE FROM users WHERE id={uid};")
    except Exception as e:
        print(f"Cleanup warning: {e}")

    print("======================================================================")
    print(f"PHASE 7 TEST RESULTS: {passed_count} PASSED, {failed_count} FAILED (TOTAL {passed_count + failed_count})")
    print("======================================================================")

    if failed_count > 0:
        sys.exit(1)

if __name__ == '__main__':
    run_tests()
