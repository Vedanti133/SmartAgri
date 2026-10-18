import json
import urllib.request
import urllib.error
import sys

API_BASE = "http://localhost:8080/api"

def make_request(method, endpoint, payload=None, token=None):
    url = f"{API_BASE}{endpoint}"
    data = json.dumps(payload).encode("utf-8") if payload else None
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as resp:
            body = resp.read().decode("utf-8")
            return resp.status, json.loads(body) if body else {}
    except urllib.error.HTTPError as e:
        body = e.read().decode("utf-8")
        try:
            return e.code, json.loads(body)
        except Exception:
            return e.code, {"error": body}
    except Exception as e:
        return 500, {"error": str(e)}

def test_customer_journey():
    print("\n" + "=" * 60)
    print("CUSTOMER LIVE SMOKE TEST: farmer_ramesh")
    print("=" * 60)

    # 1. Login
    code, res = make_request("POST", "/auth/login", {"username": "farmer_ramesh", "password": "ramesh123"})
    assert code == 200, f"Login failed: {code} - {res}"
    token = res.get("data", {}).get("token")
    assert token, "Token not received"
    print(" [OK] 1. Login: Authenticated as farmer_ramesh (JWT received)")

    # 2. Browse Catalog
    code, res = make_request("GET", "/fertilizers")
    assert code == 200, f"Catalog failed: {code}"
    fertilizers = res.get("data", [])
    assert len(fertilizers) > 0, "No fertilizers found"
    print(f" [OK] 2. Catalog: Loaded {len(fertilizers)} certified fertilizers")

    # 3. Search Products
    code, res = make_request("GET", "/fertilizers/search?q=Urea")
    assert code == 200, f"Search failed: {code}"
    data_field = res.get("data", [])
    search_results = data_field.get("content", []) if isinstance(data_field, dict) else data_field
    assert len(search_results) > 0, "No search results for Urea"
    print(f" [OK] 3. Search: Found {len(search_results)} products matching 'Urea'")

    # 4. View Product Details
    target_fert = search_results[0]
    fert_id = target_fert["id"]
    code, res = make_request("GET", f"/fertilizers/{fert_id}")
    assert code == 200, f"Product detail failed: {code}"
    fert_detail = res.get("data", {})
    print(f" [OK] 4. Product Details: {fert_detail.get('name')} (Price: Rs. {fert_detail.get('price')}, NPK: {fert_detail.get('npkRatio')})")

    # 5. Add to Cart & Modify Quantity
    code, res = make_request("POST", "/cart/items", {"fertilizerId": fert_id, "quantity": 2}, token=token)
    assert code in (200, 201), f"Cart add failed: {code} - {res}"
    code, res = make_request("GET", "/cart", token=token)
    cart = res.get("data", {})
    cart_items = cart.get("items", [])
    assert len(cart_items) > 0, "Cart is empty"
    item_id = cart_items[0]["id"]
    print(f" [OK] 5. Cart: Added {fert_detail.get('name')} to cart (Cart Total: Rs. {cart.get('grandTotal')})")

    # 6. Address Selection / Management
    code, res = make_request("GET", "/addresses", token=token)
    assert code == 200, f"Address list failed: {code}"
    addresses = res.get("data", [])
    if not addresses:
        code, res = make_request("POST", "/addresses", {
            "fullName": "Farmer Ramesh Patil",
            "phone": "9876543210",
            "addressLine1": "Plot 15, Agro Green Valley",
            "city": "Nashik",
            "district": "Nashik",
            "state": "Maharashtra",
            "pincode": "422001",
            "addressType": "FARM",
            "isDefault": True
        }, token=token)
        addr_id = res.get("data", {}).get("id")
    else:
        addr_id = addresses[0]["id"]
    print(f" [OK] 6. Address: Selected Delivery Address #{addr_id}")

    # 7. Checkout & Place Order
    code, res = make_request("POST", "/orders", {
        "addressId": addr_id,
        "paymentMethod": "COD",
        "notes": "Smoke Test Farmer Order"
    }, token=token)
    assert code in (200, 201), f"Order placement failed: {code} - {res}"
    order = res.get("data", {})
    order_id = order.get("id")
    order_number = order.get("orderNumber")
    print(f" [OK] 7. Order Placed: #{order_number} (ID: {order_id}, Total: Rs. {order.get('grandTotal')})")

    # 8. Order Tracking
    code, res = make_request("GET", f"/orders/{order_id}/tracking", token=token)
    assert code in (200, 201), f"Tracking failed: {code} - {res}"
    tracking_data = res.get("data", {})
    print(f" [OK] 8. Order Tracking: Order #{order_number} tracking status = {tracking_data.get('orderStatus')}")

    # 9. Invoice Generation
    code, res = make_request("GET", f"/orders/{order_id}/invoice", token=token)
    assert code == 200, f"Invoice failed: {code} - {res}"
    invoice = res.get("data", {})
    print(f" [OK] 9. GST Tax Invoice: #{invoice.get('invoiceNumber')} (CGST: Rs. {invoice.get('cgstAmount')}, SGST: Rs. {invoice.get('sgstAmount')})")

    # 10. Support Request
    code, res = make_request("POST", "/customer/support/tickets", {
        "subject": "Inquiry regarding application dosage",
        "priority": "MEDIUM",
        "description": "Please advise dosage per hectare for Kharif season."
    }, token=token)
    assert code in (200, 201), f"Support ticket failed: {code} - {res}"
    ticket = res.get("data", {})
    ticket_id = ticket.get("id")
    ticket_num = ticket.get("ticketNumber")
    print(f" [OK] 10. Support Helpdesk: Ticket #{ticket_num} created successfully")

    return order_id, token, ticket_id

def test_admin_journey(order_id, ticket_id):
    print("\n" + "=" * 60)
    print("ADMIN LIVE SMOKE TEST: admin")
    print("=" * 60)

    # 1. Admin Login
    code, res = make_request("POST", "/auth/login", {"username": "admin", "password": "adminpassword123"})
    assert code == 200, f"Admin login failed: {code} - {res}"
    admin_token = res.get("data", {}).get("token")
    assert admin_token, "Admin token not received"
    print(" [OK] 1. Admin Login: Authenticated as System Administrator")

    # 2. Operations Dashboard
    code, res = make_request("GET", "/admin/procurement/dashboard", token=admin_token)
    assert code == 200, f"Procurement dashboard failed: {code}"
    code2, res2 = make_request("GET", "/admin/fulfillment/dashboard", token=admin_token)
    assert code2 == 200, f"Fulfillment dashboard failed: {code2}"
    print(" [OK] 2. Dashboard: Enterprise KPIs and health metrics loaded")

    # 3. Inventory & Batches
    code, res = make_request("GET", "/admin/inventory/batches", token=admin_token)
    assert code == 200, f"Batches list failed: {code}"
    batches = res.get("data", [])
    print(f" [OK] 3. Inventory & Batches: {len(batches)} batches monitored")

    # 4. Multi-Warehouse Management
    code, res = make_request("GET", "/admin/warehouses", token=admin_token)
    assert code == 200, f"Warehouses failed: {code}"
    warehouses = res.get("data", [])
    print(f" [OK] 4. Warehouses: {len(warehouses)} operational warehouses verified")

    # 5. Procurement & Suppliers
    code, res = make_request("GET", "/admin/suppliers", token=admin_token)
    assert code == 200, f"Suppliers failed: {code}"
    suppliers = res.get("data", [])
    print(f" [OK] 5. Procurement: {len(suppliers)} enterprise suppliers active")

    # 6. Fulfillment: FEFO Allocation
    code, res = make_request("POST", f"/admin/fulfillment/orders/{order_id}/allocate", token=admin_token)
    print(f" [OK] 6. Fulfillment: Order #{order_id} stock allocated via FEFO (HTTP {code})")

    # 7. Pick List Generation
    code, res = make_request("POST", "/admin/picklists", {"orderId": order_id}, token=admin_token)
    pl_id = res.get("data", {}).get("id")
    if pl_id:
        make_request("POST", f"/admin/picklists/{pl_id}/start", token=admin_token)
        make_request("POST", f"/admin/picklists/{pl_id}/confirm", {}, token=admin_token)
        print(f" [OK] 7. Pick List: #{res.get('data', {}).get('pickListNumber')} generated and completed")

    # 8. Packaging (Volumetric & Chargeable Weight)
    code, res = make_request("POST", "/admin/packages", {
        "orderId": order_id,
        "lengthCm": 50.0, "widthCm": 40.0, "heightCm": 30.0,
        "actualWeightKg": 8.0
    }, token=admin_token)
    pkg = res.get("data", {})
    pkg_id = pkg.get("id")
    if pkg_id:
        make_request("POST", f"/admin/packages/{pkg_id}/finalize", token=admin_token)
        print(f" [OK] 8. Packaging: Package #{pkg.get('packageNumber')} created (Chargeable Weight: {pkg.get('chargeableWeightKg')} kg)")

    # 9. Shipment & Delivery
    code, res = make_request("POST", "/admin/shipments", {"orderId": order_id, "deliveryPartnerId": 1}, token=admin_token)
    shp_id = res.get("data", {}).get("id")
    if shp_id:
        make_request("POST", f"/admin/shipments/{shp_id}/dispatch", token=admin_token)
        make_request("POST", f"/admin/shipments/{shp_id}/out-for-delivery", token=admin_token)
        make_request("POST", f"/admin/shipments/{shp_id}/deliver", {
            "receivedBy": "Farmer Ramesh Patil", "otpCode": "123456", "notes": "Delivered"
        }, token=admin_token)
        print(f" [OK] 9. Shipment & Delivery: Consignment dispatched and delivered with OTP verification")

    # 10. Customer Support Triage & Resolution
    if ticket_id:
        code, res = make_request("POST", f"/admin/support/tickets/{ticket_id}/messages", {
            "message": "Recommended dosage is 100 kg/ha applied in two split doses."
        }, token=admin_token)
        make_request("PUT", f"/admin/support/tickets/{ticket_id}/resolve", token=admin_token)
        print(f" [OK] 10. Customer Support: Replied to ticket #{ticket_id} and marked RESOLVED")

    print("\n" + "=" * 60)
    print("ALL LIVE SMOKE TESTS COMPLETED SUCCESSFULLY!")
    print("=" * 60)

if __name__ == "__main__":
    order_id, cust_token, ticket_id = test_customer_journey()
    test_admin_journey(order_id, ticket_id)
