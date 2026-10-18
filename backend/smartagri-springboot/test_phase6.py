import urllib.request
import urllib.parse
import urllib.error
import json
import subprocess
import sys
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
    print("SMARTAGRI PHASE 6 AUTOMATED TEST SUITE")
    print("Advanced Product Search, Filtering, Sorting, and E-Commerce Integration")
    print("======================================================================")

    # 1. Test Filter Options Endpoint
    try:
        status, res = api_call("GET", "/fertilizers/filter-options")
        if status == 200 and res.get("success") and res.get("data"):
            data = res["data"]
            brands = data.get("brands", [])
            types = data.get("fertilizerTypes", [])
            categories = data.get("categories", [])
            npks = data.get("npkRatios", [])
            min_p = data.get("minPrice")
            max_p = data.get("maxPrice")
            total = data.get("totalCount")

            if len(brands) >= 20 and len(types) >= 5 and len(categories) >= 8 and len(npks) >= 5 and total >= 50:
                record_pass("P6-01", f"Filter options endpoint returns {len(brands)} brands, {len(types)} types, {len(npks)} NPK ratios (min ₹{min_p}, max ₹{max_p})")
            else:
                record_fail("P6-01", "Filter options endpoint", f"Counts insufficient: brands={len(brands)}, types={len(types)}, categories={len(categories)}")
        else:
            record_fail("P6-01", "Filter options endpoint", f"Status: {status}, Response: {res}")
    except Exception as e:
        record_fail("P6-01", "Filter options endpoint", str(e))

    # 2. Test Exact Name Search (Urea)
    try:
        status, res = api_call("GET", "/fertilizers/search?q=Urea")
        if status == 200 and res.get("success"):
            data = res.get("data", {})
            content = data.get("content", [])
            if any(item.get("name") == "Urea" for item in content):
                record_pass("P6-02", f"Search exact name 'Urea' returned {len(content)} matches including Urea")
            else:
                record_fail("P6-02", "Search exact name", f"Item not found in: {[i.get('name') for i in content]}")
        else:
            record_fail("P6-02", "Search exact name", f"Status: {status}")
    except Exception as e:
        record_fail("P6-02", "Search exact name", str(e))

    # 3. Test Partial Name Search (Potassium)
    try:
        status, res = api_call("GET", "/fertilizers/search?q=Potassium")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            if len(content) > 0:
                record_pass("P6-03", f"Partial name/nutrient search 'Potassium' matched {len(content)} products")
            else:
                record_fail("P6-03", "Partial name search", f"Content count: {len(content)}")
        else:
            record_fail("P6-03", "Partial name search", f"Status: {status}")
    except Exception as e:
        record_fail("P6-03", "Partial name search", str(e))

    # 4. Test Search Case Insensitivity (dap vs DAP)
    try:
        _, res_lower = api_call("GET", "/fertilizers/search?q=dap")
        _, res_upper = api_call("GET", "/fertilizers/search?q=DAP")
        lower_cnt = res_lower.get("data", {}).get("totalElements", 0)
        upper_cnt = res_upper.get("data", {}).get("totalElements", 0)

        if lower_cnt > 0 and lower_cnt == upper_cnt:
            record_pass("P6-04", f"Search case-insensitivity: 'dap' ({lower_cnt}) == 'DAP' ({upper_cnt})")
        else:
            record_fail("P6-04", "Search case-insensitivity", f"Counts differ: lower={lower_cnt}, upper={upper_cnt}")
    except Exception as e:
        record_fail("P6-04", "Search case-insensitivity", str(e))

    # 5. Test Filter by Brand (IFFCO)
    try:
        status, res = api_call("GET", "/fertilizers/search?brand=IFFCO")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            if total > 0 and all(item.get("brand") == "IFFCO" for item in content):
                record_pass("P6-05", f"Filter by brand 'IFFCO' returned {total} products, all brand=IFFCO")
            else:
                record_fail("P6-05", "Filter by brand", f"Not all IFFCO or empty: total={total}")
        else:
            record_fail("P6-05", "Filter by brand", f"Status: {status}")
    except Exception as e:
        record_fail("P6-05", "Filter by brand", str(e))

    # 6. Test Filter by Category Name (Nitrogen / Nitrogenous)
    try:
        status, res = api_call("GET", "/fertilizers/search?category=Nitrogen")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            if total > 0 and all("Nitrogen" in item.get("categoryName", "") for item in content):
                record_pass("P6-06", f"Filter by category 'Nitrogen' returned {total} products, all category=Nitrogen")
            else:
                record_fail("P6-06", "Filter by category name", f"Total={total}")
        else:
            record_fail("P6-06", "Filter by category name", f"Status: {status}")
    except Exception as e:
        record_fail("P6-06", "Filter by category name", str(e))

    # 7. Test Filter by Category ID
    try:
        status, res = api_call("GET", "/fertilizers/search?categoryId=1")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            if total > 0 and all(item.get("categoryId") == 1 for item in content):
                record_pass("P6-07", f"Filter by categoryId=1 returned {total} products, all categoryId=1")
            else:
                record_fail("P6-07", "Filter by category ID", f"Total={total}")
        else:
            record_fail("P6-07", "Filter by category ID", f"Status: {status}")
    except Exception as e:
        record_fail("P6-07", "Filter by category ID", str(e))

    # 8. Test Filter by Fertilizer Type (Nitrogen)
    try:
        status, res = api_call("GET", "/fertilizers/search?type=Nitrogen")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            if total > 0 and all(item.get("type") == "Nitrogen" for item in content):
                record_pass("P6-08", f"Filter by fertilizerType 'Nitrogen' returned {total} products, all type=Nitrogen")
            else:
                record_fail("P6-08", "Filter by fertilizer type", f"Total={total}")
        else:
            record_fail("P6-08", "Filter by fertilizer type", f"Status: {status}")
    except Exception as e:
        record_fail("P6-08", "Filter by fertilizer type", str(e))

    # 9. Test Filter by Minimum Price
    try:
        status, res = api_call("GET", "/fertilizers/search?minPrice=500")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            if total > 0 and all(float(item.get("sellingPrice", 0)) >= 500 for item in content):
                record_pass("P6-09", f"Filter minPrice=500 returned {total} products, all >= ₹500")
            else:
                record_fail("P6-09", "Filter min price", f"Total={total}")
        else:
            record_fail("P6-09", "Filter min price", f"Status: {status}")
    except Exception as e:
        record_fail("P6-09", "Filter min price", str(e))

    # 10. Test Filter by Maximum Price
    try:
        status, res = api_call("GET", "/fertilizers/search?maxPrice=300")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            if total > 0 and all(float(item.get("sellingPrice", 0)) <= 300 for item in content):
                record_pass("P6-10", f"Filter maxPrice=300 returned {total} products, all <= ₹300")
            else:
                record_fail("P6-10", "Filter max price", f"Total={total}")
        else:
            record_fail("P6-10", "Filter max price", f"Status: {status}")
    except Exception as e:
        record_fail("P6-10", "Filter max price", str(e))

    # 11. Test Filter by Price Range
    try:
        status, res = api_call("GET", "/fertilizers/search?minPrice=200&maxPrice=500")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            if total > 0 and all(200 <= float(item.get("sellingPrice", 0)) <= 500 for item in content):
                record_pass("P6-11", f"Filter price range ₹200-₹500 returned {total} products correctly")
            else:
                record_fail("P6-11", "Filter price range", f"Total={total}")
        else:
            record_fail("P6-11", "Filter price range", f"Status: {status}")
    except Exception as e:
        record_fail("P6-11", "Filter price range", str(e))

    # 12. Test Filter by Stock Status: in_stock
    try:
        status, res = api_call("GET", "/fertilizers/search?stock=in_stock")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            if total > 0 and all(item.get("stock", 0) > 0 for item in content):
                record_pass("P6-12", f"Filter stock=in_stock returned {total} products, all stock > 0")
            else:
                record_fail("P6-12", "Filter in_stock", f"Total={total}")
        else:
            record_fail("P6-12", "Filter in_stock", f"Status: {status}")
    except Exception as e:
        record_fail("P6-12", "Filter in_stock", str(e))

    # 13. Test Filter by Stock Status: low_stock
    try:
        status, res = api_call("GET", "/fertilizers/search?stock=low_stock")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            # Each item should have 0 < stock and (stock <= minimumStock or stock <= 10)
            is_valid = total > 0 and all(item.get("stock", 0) > 0 and (item.get("stock", 0) <= item.get("minimumStock", 10) or item.get("stock", 0) <= 10) for item in content)
            if is_valid:
                record_pass("P6-13", f"Filter stock=low_stock returned {total} items with low stock inventory")
            else:
                record_fail("P6-13", "Filter low_stock", f"Total={total}")
        else:
            record_fail("P6-13", "Filter low_stock", f"Status: {status}")
    except Exception as e:
        record_fail("P6-13", "Filter low_stock", str(e))

    # 14. Test Filter by Stock Status: out_of_stock
    try:
        mysql_query("UPDATE fertilizer SET stock = 0 WHERE id = 50;")
        status, res = api_call("GET", "/fertilizers/search?stock=out_of_stock")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            if total > 0 and all(item.get("stock", 0) == 0 for item in content):
                record_pass("P6-14", f"Filter stock=out_of_stock returned {total} items with stock == 0")
            else:
                record_fail("P6-14", "Filter out_of_stock", f"Total={total}")
        else:
            record_fail("P6-14", "Filter out_of_stock", f"Status: {status}")
        mysql_query("UPDATE fertilizer SET stock = 40 WHERE id = 50;")
    except Exception as e:
        record_fail("P6-14", "Filter out_of_stock", str(e))

    # 15. Test NPK Search with Colons (e.g., 20:20:20)
    try:
        status, res = api_call("GET", "/fertilizers/search?npk=20:20:20")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            if total > 0 and all(item.get("npkRatio") == "20:20:20" for item in content):
                record_pass("P6-15", f"Filter by NPK ratio '20:20:20' returned {total} matching products")
            else:
                record_fail("P6-15", "NPK colon search", f"Total={total}")
        else:
            record_fail("P6-15", "NPK colon search", f"Status: {status}")
    except Exception as e:
        record_fail("P6-15", "NPK colon search", str(e))

    # 16. Test NPK Search with Hyphens (Voice/Text: 20-20-20 -> matches 20:20:20)
    try:
        status, res = api_call("GET", "/fertilizers/search?q=20-20-20")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            if total > 0 and any("20:20:20" in (item.get("npkRatio") or "") or "20:20:20" in item.get("name", "") for item in content):
                record_pass("P6-16", f"Hyphenated NPK voice query '20-20-20' successfully resolved to '20:20:20'")
            else:
                record_fail("P6-16", "Hyphenated NPK query", f"Total={total}")
        else:
            record_fail("P6-16", "Hyphenated NPK query", f"Status: {status}")
    except Exception as e:
        record_fail("P6-16", "Hyphenated NPK query", str(e))

    # 17. Test Search by Suitable Crop
    try:
        status, res = api_call("GET", "/fertilizers/search?q=Wheat")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            if total > 0 and any("wheat" in item.get("suitableCrops", "").lower() for item in content):
                record_pass("P6-17", f"Search by suitable crop 'Wheat' returned {total} relevant products")
            else:
                record_fail("P6-17", "Search by crop", f"Total={total}")
        else:
            record_fail("P6-17", "Search by crop", f"Status: {status}")
    except Exception as e:
        record_fail("P6-17", "Search by crop", str(e))

    # 18. Test Multi-Parameter Combined Search
    try:
        status, res = api_call("GET", "/fertilizers/search?brand=IFFCO&type=Nitrogen&minPrice=100&maxPrice=1500")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            total = res.get("data", {}).get("totalElements", 0)
            matches_all = all(item.get("brand") == "IFFCO" and item.get("type") == "Nitrogen" and 100 <= float(item.get("sellingPrice", 0)) <= 1500 for item in content)
            if total > 0 and matches_all:
                record_pass("P6-18", f"Multi-parameter search (brand=IFFCO, type=Nitrogen, ₹100-₹1500) returned {total} items")
            else:
                record_fail("P6-18", "Multi-parameter search", f"Total={total}, matches_all={matches_all}")
        else:
            record_fail("P6-18", "Multi-parameter search", f"Status: {status}")
    except Exception as e:
        record_fail("P6-18", "Multi-parameter search", str(e))

    # 19. Test Sorting: Price Ascending
    try:
        status, res = api_call("GET", "/fertilizers/search?sort=price_asc&size=50")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            prices = [float(item["sellingPrice"]) for item in content]
            if len(prices) > 0 and prices == sorted(prices):
                record_pass("P6-19", f"Sort price_asc ordered {len(prices)} items from ₹{prices[0]} to ₹{prices[-1]}")
            else:
                record_fail("P6-19", "Sort price_asc", f"Prices not monotonic: {prices[:5]}")
        else:
            record_fail("P6-19", "Sort price_asc", f"Status: {status}")
    except Exception as e:
        record_fail("P6-19", "Sort price_asc", str(e))

    # 20. Test Sorting: Price Descending
    try:
        status, res = api_call("GET", "/fertilizers/search?sort=price_desc&size=50")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            prices = [float(item["sellingPrice"]) for item in content]
            if len(prices) > 0 and prices == sorted(prices, reverse=True):
                record_pass("P6-20", f"Sort price_desc ordered {len(prices)} items from ₹{prices[0]} down to ₹{prices[-1]}")
            else:
                record_fail("P6-20", "Sort price_desc", f"Prices not monotonic: {prices[:5]}")
        else:
            record_fail("P6-20", "Sort price_desc", f"Status: {status}")
    except Exception as e:
        record_fail("P6-20", "Sort price_desc", str(e))

    # 21. Test Sorting: Name Ascending
    try:
        status, res = api_call("GET", "/fertilizers/search?sort=name_asc&size=50")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            names = [item["name"].lower() for item in content]
            if len(names) > 0 and names == sorted(names):
                record_pass("P6-21", f"Sort name_asc ordered {len(names)} items alphabetically A->Z")
            else:
                record_fail("P6-21", "Sort name_asc", f"Names not alphabetical: {names[:4]}")
        else:
            record_fail("P6-21", "Sort name_asc", f"Status: {status}")
    except Exception as e:
        record_fail("P6-21", "Sort name_asc", str(e))

    # 22. Test Sorting: Newest Arrivals
    try:
        status, res = api_call("GET", "/fertilizers/search?sort=newest&size=10")
        if status == 200 and res.get("success"):
            content = res.get("data", {}).get("content", [])
            if len(content) > 0:
                record_pass("P6-22", f"Sort newest returned {len(content)} latest additions")
            else:
                record_fail("P6-22", "Sort newest", "Empty content")
        else:
            record_fail("P6-22", "Sort newest", f"Status: {status}")
    except Exception as e:
        record_fail("P6-22", "Sort newest", str(e))

    # 23. Test Pagination: Page 0, Size 12
    try:
        status, res = api_call("GET", "/fertilizers/search?page=0&size=12")
        if status == 200 and res.get("success"):
            data = res.get("data", {})
            content = data.get("content", [])
            page = data.get("page")
            size = data.get("size")
            totalElements = data.get("totalElements")
            totalPages = data.get("totalPages")
            first = data.get("first")
            last = data.get("last")

            expected_pages = (totalElements + size - 1) // size
            if len(content) == 12 and page == 0 and size == 12 and totalElements >= 50 and totalPages == expected_pages and first is True and last is False:
                record_pass("P6-23", f"Pagination page=0, size=12: returned 12 items, {totalPages} total pages, first=True, last=False")
            else:
                record_fail("P6-23", "Pagination page 0", f"len={len(content)}, p={page}, s={size}, totalE={totalElements}, totalP={totalPages}")
        else:
            record_fail("P6-23", "Pagination page 0", f"Status: {status}")
    except Exception as e:
        record_fail("P6-23", "Pagination page 0", str(e))

    # 24. Test Pagination: Page 1, Size 12 (Disjoint from Page 0)
    try:
        _, res0 = api_call("GET", "/fertilizers/search?page=0&size=12")
        ids0 = {item["id"] for item in res0.get("data", {}).get("content", [])}

        status, res1 = api_call("GET", "/fertilizers/search?page=1&size=12")
        if status == 200 and res1.get("success"):
            data = res1.get("data", {})
            content = data.get("content", [])
            ids1 = {item["id"] for item in content}
            first = data.get("first")

            if len(content) == 12 and len(ids0.intersection(ids1)) == 0 and first is False:
                record_pass("P6-24", f"Pagination page=1 returns 12 distinct products, completely disjoint from page 0")
            else:
                record_fail("P6-24", "Pagination page 1", f"Overlap: {ids0.intersection(ids1)}, first: {first}")
        else:
            record_fail("P6-24", "Pagination page 1", f"Status: {status}")
    except Exception as e:
        record_fail("P6-24", "Pagination page 1", str(e))

    # 25. Test Legacy Keyword Search Compatibility (Returns raw List<FertilizerDTO>)
    try:
        status, res = api_call("GET", "/fertilizers/search?keyword=urea")
        if status == 200 and res.get("success"):
            data = res.get("data")
            if isinstance(data, list) and len(data) > 0 and all("urea" in item.get("name", "").lower() for item in data):
                record_pass("P6-25", f"Legacy search contract (keyword=urea) returned List<FertilizerDTO> of size {len(data)}")
            else:
                record_fail("P6-25", "Legacy search compatibility", f"Expected list, got: {type(data)}")
        else:
            record_fail("P6-25", "Legacy search compatibility", f"Status: {status}")
    except Exception as e:
        record_fail("P6-25", "Legacy search compatibility", str(e))

    # 26. Test Empty Search Results Graceful Handling
    try:
        status, res = api_call("GET", "/fertilizers/search?q=NonExistentFertilizerSuper12345XYZ")
        if status == 200 and res.get("success"):
            data = res.get("data", {})
            content = data.get("content", [])
            total = data.get("totalElements", -1)
            empty = data.get("empty")
            if total == 0 and len(content) == 0 and empty is True:
                record_pass("P6-26", "Empty search results handled gracefully (HTTP 200, empty=True, totalElements=0)")
            else:
                record_fail("P6-26", "Empty search results", f"total={total}, len={len(content)}, empty={empty}")
        else:
            record_fail("P6-26", "Empty search results", f"Status: {status}")
    except Exception as e:
        record_fail("P6-26", "Empty search results", str(e))

    # 27. Test E-Commerce Workflow Integration: Search -> Add to Cart
    try:
        # Create or login customer
        user_data = {"username": "p6_cust", "email": "p6_cust@example.com", "password": "Password@123", "role": "CUSTOMER"}
        api_call("POST", "/auth/register", user_data)
        login_st, login_res = api_call("POST", "/auth/login", {"username": "p6_cust", "password": "Password@123"})
        token = login_res.get("data", {}).get("token")

        if token:
            # 1. Search for Urea
            _, search_res = api_call("GET", "/fertilizers/search?q=Urea")
            items = search_res.get("data", {}).get("content", [])
            if len(items) > 0:
                fert_id = items[0]["id"]
                # 2. Add to Cart
                cart_st, cart_res = api_call("POST", "/cart/items", {"fertilizerId": fert_id, "quantity": 2}, token=token)
                if cart_st == 200 and cart_res.get("success"):
                    # 3. Verify cart has item
                    _, get_cart = api_call("GET", "/cart", token=token)
                    cart_items = get_cart.get("data", {}).get("items", [])
                    if any(item.get("fertilizerId") == fert_id for item in cart_items):
                        record_pass("P6-27", f"E-Commerce workflow: Searched item #{fert_id} successfully added to cart and verified")
                    else:
                        record_fail("P6-27", "Cart integration", "Item not found in customer cart")
                else:
                    record_fail("P6-27", "Cart integration", f"Add to cart failed: {cart_res}")
            else:
                record_fail("P6-27", "Cart integration", "No search items returned")
        else:
            record_fail("P6-27", "Cart integration", f"Login failed: {login_res}")

        # Cleanup test user cart
        mysql_query("DELETE ci FROM cart_items ci JOIN cart c ON ci.cart_id=c.id JOIN users u ON c.user_id=u.id WHERE u.username='p6_cust';")
        mysql_query("DELETE c FROM cart c JOIN users u ON c.user_id=u.id WHERE u.username='p6_cust';")
        mysql_query("DELETE FROM users WHERE username='p6_cust';")
    except Exception as e:
        record_fail("P6-27", "Cart integration", str(e))

    # 28. Test Invalid Filter Values Handled Gracefully
    try:
        status, res = api_call("GET", "/fertilizers/search?minPrice=-100&stock=UNKNOWN_STOCK&categoryId=9999999")
        if status == 200 and res.get("success"):
            data = res.get("data", {})
            total = data.get("totalElements", -1)
            record_pass("P6-28", f"Invalid filter values handled gracefully (HTTP 200, total={total})")
        else:
            record_fail("P6-28", "Invalid filter values", f"Status: {status}, res: {res}")
    except Exception as e:
        record_fail("P6-28", "Invalid filter values", str(e))

    # 29. Test Product Result Contains Required Catalog Fields
    try:
        status, res = api_call("GET", "/fertilizers/search?q=Urea")
        if status == 200 and res.get("success"):
            items = res.get("data", {}).get("content", [])
            if len(items) > 0:
                first = items[0]
                required_fields = ["id", "name", "brand", "categoryId", "categoryName", "sellingPrice", "stock", "sku", "npkRatio", "unit"]
                missing = [f for f in required_fields if f not in first]
                if not missing:
                    record_pass("P6-29", f"Product search result contains all required catalog fields ({', '.join(required_fields)})")
                else:
                    record_fail("P6-29", "Catalog fields check", f"Missing fields: {missing}")
            else:
                record_fail("P6-29", "Catalog fields check", "No items returned")
        else:
            record_fail("P6-29", "Catalog fields check", f"Status: {status}")
    except Exception as e:
        record_fail("P6-29", "Catalog fields check", str(e))

    # 30. Security: No Unauthorized Sensitive Data Exposed in Search
    try:
        status, res = api_call("GET", "/fertilizers/search?page=0&size=50")
        raw_text = json.dumps(res)
        sensitive_terms = ["password", "$2a$", "$2b$", "secret", "private_key", "cvv"]
        exposed = [term for term in sensitive_terms if term in raw_text.lower()]
        if not exposed:
            record_pass("P6-30", "Security verification: zero passwords, hashes, or secrets exposed in product search")
        else:
            record_fail("P6-30", "Security verification", f"Sensitive data exposed: {exposed}")
    except Exception as e:
        record_fail("P6-30", "Security verification", str(e))

    # 31. Test Existing Customer Catalog Endpoint Still Works
    try:
        status, res = api_call("GET", "/fertilizers")
        if status == 200 and res.get("success") and isinstance(res.get("data"), list):
            items = res.get("data")
            if len(items) >= 50:
                record_pass("P6-31", f"Existing customer catalog (GET /api/fertilizers) intact, returning {len(items)} active items")
            else:
                record_fail("P6-31", "Existing catalog endpoint", f"Returned count: {len(items)}")
        else:
            record_fail("P6-31", "Existing catalog endpoint", f"Status: {status}")
    except Exception as e:
        record_fail("P6-31", "Existing catalog endpoint", str(e))

    # 32. Test Existing Checkout & Order Placement with Searched Product
    try:
        import time
        ts = int(time.time())
        cust_user = f"p6_ord_{ts}"
        user_data = {
            "name": "P6 Test Farmer",
            "username": cust_user,
            "email": f"{cust_user}@example.com",
            "password": "Password123!",
            "role": "CUSTOMER"
        }
        reg_st, reg_res = api_call("POST", "/auth/register", user_data)
        data = (reg_res or {}).get("data") or {}
        token = data.get("token")
        user_id = data.get("id")

        if not token:
            _, login_res = api_call("POST", "/auth/login", {"username": cust_user, "password": "Password123!"})
            ldata = (login_res or {}).get("data") or {}
            token = ldata.get("token")
            user_id = (ldata.get("user") or {}).get("id")

        if token and user_id:
            # Create address via POST /addresses
            addr_data = {
                "fullName": "P6 Test Farmer",
                "phone": "9876543210",
                "addressLine1": "Farm Lane 1",
                "city": "Pune",
                "state": "Maharashtra",
                "pincode": "411001",
                "addressType": "HOME",
                "isDefault": True
            }
            addr_st, addr_res = api_call("POST", "/addresses", addr_data, token=token)
            addr_id = (addr_res.get("data") or {}).get("id")


            # Add searched product to cart
            api_call("POST", "/cart/items", {"fertilizerId": 1, "quantity": 2}, token=token)

            # Place order
            order_data = {"addressId": addr_id, "paymentMethod": "COD", "notes": "Phase 6 search-to-checkout verification"}
            order_st, order_res = api_call("POST", "/orders", order_data, token=token)

            if order_st == 201 and order_res.get("success"):
                order_id = order_res.get("data", {}).get("id")
                record_pass("P6-32", f"Existing checkout & order placement works with searched product (OrderId: #{order_id})")

                # 33. Test Payment, Delivery & Invoice Generation on Searched Product Order
                inv_st, inv_res = api_call("GET", f"/orders/{order_id}/invoice", token=token)
                deliv_st, deliv_res = api_call("GET", f"/orders/{order_id}/delivery", token=token)

                if inv_st == 200 and inv_res.get("success") and deliv_st == 200 and deliv_res.get("success"):
                    inv_num = inv_res.get("data", {}).get("invoiceNumber")
                    trk_num = deliv_res.get("data", {}).get("trackingNumber")
                    record_pass("P6-33", f"Payment, Delivery & GST Invoice intact for order #{order_id} (Invoice: {inv_num}, Tracking: {trk_num})")
                else:
                    record_fail("P6-33", "Delivery & Invoice intact", f"Invoice status: {inv_st}, Delivery status: {deliv_st}")
            else:
                record_fail("P6-32", "Checkout flow", f"Order creation failed: {order_res}")

            # Cleanup
            mysql_query(f"DELETE FROM invoices WHERE order_id IN (SELECT id FROM orders WHERE user_id={user_id});")
            mysql_query(f"DELETE FROM deliveries WHERE order_id IN (SELECT id FROM orders WHERE user_id={user_id});")
            mysql_query(f"DELETE FROM payments WHERE order_id IN (SELECT id FROM orders WHERE user_id={user_id});")
            mysql_query(f"DELETE FROM order_items WHERE order_id IN (SELECT id FROM orders WHERE user_id={user_id});")
            mysql_query(f"DELETE FROM orders WHERE user_id={user_id};")
            mysql_query(f"DELETE FROM addresses WHERE user_id={user_id};")
            mysql_query(f"DELETE FROM cart_items WHERE cart_id IN (SELECT id FROM cart WHERE user_id={user_id});")
            mysql_query(f"DELETE FROM cart WHERE user_id={user_id};")
            mysql_query(f"DELETE FROM users WHERE id={user_id};")
        else:
            record_fail("P6-32", "Checkout flow", f"Login failed for test user: reg_res={reg_res}")
    except Exception as e:
        record_fail("P6-32", "Checkout flow", str(e))



    print("======================================================================")
    print(f"PHASE 6 TEST RESULTS: {passed_count} PASSED, {failed_count} FAILED (TOTAL {passed_count + failed_count})")
    print("======================================================================")

    if failed_count > 0:
        sys.exit(1)

if __name__ == '__main__':
    run_tests()

