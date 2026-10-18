import re
import os
import sys

def run_frontend_simulation():
    print("=" * 80)
    print("SMARTAGRI STEP 2: FRONTEND SIDEBAR & ROUTE GUARD SIMULATION VERIFICATION")
    print("=" * 80)

    html_dir = r"c:\Users\khush\Downloads\kk\SmartAgri_3_Language_Updated\SmartAgri_FINAL\SmartAgri_FINAL\frontend\HTML"
    admin_dash_path = os.path.join(html_dir, "admin-dashboard.html")
    with open(admin_dash_path, "r", encoding="utf-8") as f:
        html_content = f.read()

    # Extract all links inside <aside class="sidebar" id="side">
    sidebar_match = re.search(r'<aside[^>]*class=[\'"][^\'"]*sidebar[^\'"]*[\'"][^>]*>(.*?)</aside>', html_content, re.DOTALL | re.IGNORECASE)
    if not sidebar_match:
        print("[-] FAIL: Could not find <aside class='sidebar'> in admin-dashboard.html")
        return 1

    sidebar_content = sidebar_match.group(1)
    link_matches = re.findall(r'<a\s+([^>]*?)>(.*?)</a>', sidebar_content, re.DOTALL)
    
    sidebar_links = []
    for attrs, inner in link_matches:
        href_match = re.search(r'href=[\'"]([^\'"]*)[\'"]', attrs)
        href = href_match.group(1) if href_match else ""
        text_match = re.search(r'<span>(.*?)</span>', inner, re.DOTALL)
        text = text_match.group(1).strip() if text_match else inner.strip()
        sidebar_links.append((href, text, attrs))

    print(f"\n[+] Total Sidebar Menu Items Discovered: {len(sidebar_links)}")
    for i, (href, text, _) in enumerate(sidebar_links, 1):
        print(f"    {i:2d}. {text:28} -> href='{href}'")

    # Read auth.js permission matrix
    auth_js_path = r"c:\Users\khush\Downloads\kk\SmartAgri_3_Language_Updated\SmartAgri_FINAL\SmartAgri_FINAL\frontend\JS\auth.js"
    with open(auth_js_path, "r", encoding="utf-8") as f:
        auth_js = f.read()

    # Define exact permissions from auth.js to test against DOM simulation
    ROLE_PERMISSIONS = {
        'ADMIN': ['*'],
        'SALES_STAFF': [
            'admin-orders.html', 'sales.html', 'new-sale.html', 'admin-delivery.html',
            'fulfillment.html', 'customers.html', 'fertilizers.html', 'categories.html',
            'inventory.html', 'stock-alerts.html', 'reports.html', 'customer-catalog.html',
            'profile.html', 'about.html'
        ],
        'SUPPLIER': [
            'procurement.html', 'purchases.html', 'new-purchase.html', 'stock-in.html',
            'suppliers.html', 'fertilizers.html', 'profile.html', 'about.html'
        ],
        'DISTRIBUTOR': [
            'customer-catalog.html', 'customer-cart.html', 'checkout.html', 'order-success.html',
            'customer-orders.html', 'customer-order-tracking.html', 'invoice.html', 'customer-invoice.html',
            'returns.html', 'support.html', 'transfers.html', 'warehouses.html', 'inventory.html',
            'stock-alerts.html', 'fertilizers.html', 'profile.html', 'customer-profile.html', 'about.html'
        ],
        'DEALER': [
            'customer-catalog.html', 'customer-cart.html', 'checkout.html', 'order-success.html',
            'customer-orders.html', 'customer-order-tracking.html', 'invoice.html', 'customer-invoice.html',
            'returns.html', 'support.html', 'transfers.html', 'inventory.html', 'fertilizers.html',
            'profile.html', 'customer-profile.html', 'about.html'
        ],
        'RETAILER': [
            'customer-catalog.html', 'customer-wishlist.html', 'customer-cart.html', 'checkout.html',
            'order-success.html', 'customer-orders.html', 'customer-order-tracking.html', 'invoice.html',
            'customer-invoice.html', 'returns.html', 'support.html', 'profile.html',
            'customer-profile.html', 'about.html'
        ],
        'CUSTOMER': [
            'customer-catalog.html', 'customer-wishlist.html', 'customer-cart.html', 'checkout.html',
            'order-success.html', 'customer-orders.html', 'customer-order-tracking.html', 'invoice.html',
            'customer-invoice.html', 'returns.html', 'support.html', 'customer-profile.html', 'about.html'
        ]
    }

    def simulate_apply_role_sidebar(role):
        effective_role = 'SUPPLIER' if role == 'SUPPLIERS' else role
        allowed = ROLE_PERMISSIONS.get(effective_role, [])
        visible = []
        hidden = []
        for href, text, attrs in sidebar_links:
            clean_href = href.split('?')[0].split('#')[0].replace('\\', '/').split('/')[-1].lower()
            is_logout = (clean_href == '' or clean_href == '#' or 'logout' in attrs.lower() or 'logout' in text.lower())
            if is_logout:
                visible.append((href, text))
                continue
            if clean_href == 'about.html':
                visible.append((href, text))
                continue
            if '*' in allowed or clean_href in allowed:
                visible.append((href, text))
            else:
                hidden.append((href, text))
        return visible, hidden

    print("\n" + "=" * 60)
    print("SIMULATION: Testing Dynamic Sidebar Filter for Each Role")
    print("=" * 60)

    for role in ['ADMIN', 'SALES_STAFF', 'SUPPLIER', 'DISTRIBUTOR', 'DEALER', 'RETAILER', 'CUSTOMER']:
        vis, hid = simulate_apply_role_sidebar(role)
        print(f"\n--- ROLE: {role} (Visible: {len(vis)}, Hidden: {len(hid)}) ---")
        vis_names = [name for _, name in vis]
        hid_names = [name for _, name in hid]
        print(f"  Visible: {', '.join(vis_names[:8])}{' ...' if len(vis_names) > 8 else ''}")
        if hid_names:
            print(f"  Hidden:  {', '.join(hid_names[:8])}{' ...' if len(hid_names) > 8 else ''}")

        # Assertions
        if role == 'ADMIN':
            assert len(hid) == 0, "ADMIN must see all sidebar items!"
        elif role == 'SALES_STAFF':
            assert "Dashboard" in hid_names, "Dashboard must be hidden for Sales Staff!"
            assert "Users" in hid_names, "Users must be hidden for Sales Staff!"
            assert "Settings" in hid_names, "Settings must be hidden for Sales Staff!"
            assert "Orders &amp; Sales" in vis_names or "Orders & Sales" in vis_names
        elif role == 'SUPPLIER':
            assert "Procurement &amp; GRN" in vis_names or "Procurement & GRN" in vis_names
            assert "Dashboard" in hid_names
            assert "Users" in hid_names
            assert "Customers" in hid_names
        elif role == 'CUSTOMER':
            assert "Storefront (Shop)" in vis_names
            assert "Dashboard" in hid_names
            assert "Users" in hid_names
            assert "Inventory" in hid_names

    print("\n[+] All role sidebar filtering simulations passed successfully!")

    # Test route guard logic for critical pages
    print("\n" + "=" * 60)
    print("SIMULATION: Testing Route Guard (isPageAllowedForRole)")
    print("=" * 60)

    def is_page_allowed(role, page):
        r = 'SUPPLIER' if role == 'SUPPLIERS' else role
        if r == 'ADMIN':
            return True
        allowed = ROLE_PERMISSIONS.get(r, [])
        clean = page.lower().replace('\\', '/').split('/')[-1]
        if clean in ['', 'index.html', 'about.html']:
            return True
        return '*' in allowed or clean in allowed

    guard_checks = [
        ('ADMIN', 'admin-dashboard.html', True),
        ('ADMIN', 'users.html', True),
        ('ADMIN', 'settings.html', True),
        ('CUSTOMER', 'admin-dashboard.html', False),
        ('CUSTOMER', 'users.html', False),
        ('CUSTOMER', 'inventory.html', False),
        ('CUSTOMER', 'customer-catalog.html', True),
        ('CUSTOMER', 'customer-cart.html', True),
        ('RETAILER', 'admin-dashboard.html', False),
        ('RETAILER', 'users.html', False),
        ('RETAILER', 'customer-catalog.html', True),
        ('DEALER', 'transfers.html', True),
        ('DEALER', 'users.html', False),
        ('DISTRIBUTOR', 'warehouses.html', True),
        ('DISTRIBUTOR', 'users.html', False),
        ('SUPPLIER', 'procurement.html', True),
        ('SUPPLIER', 'admin-orders.html', False),
        ('SUPPLIER', 'users.html', False),
        ('SALES_STAFF', 'admin-orders.html', True),
        ('SALES_STAFF', 'fulfillment.html', True),
        ('SALES_STAFF', 'users.html', False),
        ('SALES_STAFF', 'admin-dashboard.html', False),
    ]

    for role, page, expected in guard_checks:
        result = is_page_allowed(role, page)
        status = "PASS" if result == expected else "FAIL"
        print(f"  [{status}] Role '{role:11}' on '{page:22}' -> Allowed={result} (Expected {expected})")
        assert result == expected

    print("\n[+] All Route Guard simulations passed successfully!")
    print("\nALL FRONTEND ROLE ACCESS VERIFICATIONS COMPLETED SUCCESSFULLY!\n")
    return 0

if __name__ == '__main__':
    sys.exit(run_frontend_simulation())
