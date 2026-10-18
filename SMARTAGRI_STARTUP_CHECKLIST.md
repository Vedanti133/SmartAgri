# SmartAgri — Production Startup Checklist & Final Evaluator Demo Flow

Use this pre-flight checklist and structured step-by-step evaluator script before presenting the project to examiners, professors, or evaluators.

---

## ⏱️ Pre-Demo Readiness Checklist (5 Minutes)

- [ ] **1. Start MySQL Server**
  - Open **XAMPP Control Panel** or run MySQL service.
  - Click **Start** next to **MySQL**.
  - Confirm the port shows `3306` and the indicator is green.

- [ ] **2. Confirm Database Imported**
  - Open PowerShell:
    ```powershell
    C:\xampp\mysql\bin\mysql.exe -u root -e "USE smartagri; SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='smartagri';"
    ```
  - Verify that **44 tables** exist.
  - If database is not present, import:
    ```powershell
    C:\xampp\mysql\bin\mysql.exe -u root -e "CREATE DATABASE IF NOT EXISTS smartagri;"
    C:\xampp\mysql\bin\mysql.exe -u root smartagri < database.sql
    ```

- [ ] **3. Start Spring Boot Enterprise Backend**
  - Open PowerShell in `backend\smartagri-springboot`:
    ```powershell
    .\mvnw.cmd spring-boot:run
    ```
  - Wait for the message:
    ```
    INFO: Started SmartAgriApplication in X.XXX seconds
    ```
  - Verify API health in your browser: `http://localhost:8080/api/health` or `http://localhost:8080/api/fertilizers`

- [ ] **4. Launch Frontend Web Server**
  - Open a second PowerShell window in `frontend`:
    ```powershell
    python -m http.server 5500
    ```
  - Access endpoints:
    - Customer Catalog: `http://localhost:5500/HTML/customer-catalog.html`
    - Admin Console: `http://localhost:5500/HTML/admin-dashboard.html`
    - Login: `http://localhost:5500/HTML/login.html`

- [ ] **5. Test Demo Logins**
  - Open `http://localhost:5500/HTML/login.html` in an Incognito / Private browsing window.
  - **Admin Login:** `admin` / `adminpassword123` (Redirects to Admin Dashboard)
  - **Farmer Login:** `farmer_ramesh` / `ramesh123` (Redirects to Customer Catalog)

- [ ] **6. Run Regression Test Suite Certification**
  - In PowerShell (`backend\smartagri-springboot`):
    ```powershell
    python run_all_tests.py
    ```
  - Confirm **635 / 635 PASSED (100% Pass Rate across 13 Suites)**.

---

## 🔑 Demo Accounts Reference

| Role | Username | Password | Purpose & Capabilities |
| :--- | :--- | :--- | :--- |
| **System Administrator** | `admin` | `adminpassword123` | Full administrative oversight: Multi-Warehouse, Suppliers, Procurement, FEFO Allocation, Pick & Pack, Logistics, QC Restocking, Refunds, Support Helpdesk |
| **Customer (Farmer)** | `farmer_ramesh` | `ramesh123` | Farmer shopping journey: Voice & Spec Search, Wishlist, Cart, Address Book, Checkout, Real-Time Delivery Tracking, GST Invoice, RMA Returns, Support Tickets |

---

# FINAL DEMO FLOW

Follow this exact 17-step sequence during evaluation to demonstrate the complete full-stack enterprise platform:

### 1. Login
- Open `http://localhost:5500/HTML/login.html`.
- Demonstrate dual-role authentication: log in with Farmer account (`farmer_ramesh` / `ramesh123`).
- Point out secure JWT generation, automatic role-based redirect, and active session display.

### 2. Customer Catalog
- Lands on `http://localhost:5500/HTML/customer-catalog.html`.
- Display 90+ certified fertilizers (IFFCO, Coromandel, NFL) with categories (Nitrogenous, Phosphatic, Potassic, Organic, Micronutrient).

### 3. Product Search/Filter
- Type `Urea` in the search box or demonstrate **Voice Search** via the microphone icon.
- Adjust the **Nutrient Slider** (e.g. Nitrogen > 40%) and select form filter (Granular / Liquid / Powder).

### 4. Product Details
- Click on **Neem Coated Urea (50kg)**.
- Review technical chemical specifications, N-P-K nutrient composition (46-0-0), soil recommendations, and government subsidy transparency rate.

### 5. Cart
- Click **Add to Wishlist** (heart icon) to demonstrate wishlist persistence.
- Click **Add to Cart** with quantity = 2.
- Navigate to **Cart** (`customer-cart.html`), adjust quantity to 3, and show live recalculation of base price, GST (5%), and total.

### 6. Checkout
- Click **Proceed to Checkout** (`customer-checkout.html`).
- Select the pre-saved **Farm** address (`Plot 15, Agro Green Valley, Nashik`) from the multi-type address book.

### 7. Payment
- Select payment method: choose **Cash on Delivery (COD)** (or simulate UPI / Net Banking).
- Click **Place Order**. Show instant generation of unique Order Number (`SA-20260928-XXXX`).

### 8. Order Tracking
- Navigate to **My Orders** (`customer-orders.html`) and click **Track Delivery** (`customer-order-tracking.html`).
- Click **Download / Print Tax Invoice** to show the GST Tax Invoice with HSN code, CGST (2.5%), and SGST (2.5%) breakdown.

### 9. Inventory
- Log into Admin Console (`admin-dashboard.html`) as `admin` / `adminpassword123`.
- Navigate to **Batches & Inventory** (`batches.html`).
- Inspect stock across batches, manufacturing and expiry dates, quarantine flags, and low-stock threshold alerts.

### 10. Warehouse
- Navigate to **Warehouses** (`warehouses.html`).
- Explore 24 regional warehouses (General, Cold Storage, Hazardous Chemical) with internal zone and bin hierarchies.
- Showcase **Transfers** (`transfers.html`) for moving stock between warehouses.

### 11. Procurement
- Navigate to **Procurement** (`procurement.html`).
- View enterprise suppliers (IFFCO, Coromandel International, National Fertilizers Ltd).
- Review Purchase Orders (PO), Goods Receipt Notes (GRN), and putaway batch recording.

### 12. Fulfillment
- Navigate to **Fulfillment Console** (`fulfillment.html`).
- Locate the farmer's newly placed order.
- Click **Allocate Stock** — explain the automated **First-Expiry-First-Out (FEFO)** batch allocation algorithm.

### 13. Delivery
- Generate **Pick List** and assign staff picker.
- Click **Create Package** — highlight automatic **Volumetric Weight vs Actual Weight** calculation:
  $$\text{Volumetric Weight} = \frac{L \times W \times H}{5000}, \quad \text{Chargeable Weight} = \max(\text{Actual}, \text{Volumetric})$$
- Dispatch via **Kisan Speed Logistics** and observe real-time tracking milestones (`IN_TRANSIT` $\rightarrow$ `DELIVERED`).

### 14. Return/Replacement
- In customer view (`returns.html` or `customer-orders.html`), submit an RMA Return Request for 1 unit.
- In Admin Console (`admin-returns.html`), receive package at dock and perform **Quality Inspection**: select `ACCEPTED`.
- Show evaluators that inventory is automatically restored to the original batch and location bin, an immutable `RETURN_IN` transaction is written, and refund is scheduled!

### 15. Customer Support
- In Customer view (`support.html`), view or submit an inquiry ticket (e.g. "Fertilizer dosage query").
- In Admin Console (`admin-support.html`), open the ticket, reply to the farmer, and mark **Resolved**.

### 16. Admin Dashboard
- Navigate to `admin-dashboard.html`.
- Review high-level operational navigation, user management, and centralized administration hubs.

### 17. Reports/Analytics (Dashboard KPIs)
- Review enterprise KPI cards: Total Revenue, Low Stock Alerts, Active Batches, Warehouse Utilization, and Pending Orders.
- Demonstrate fulfillment health check (`HEALTHY`, invariants validated).

---

## 🆘 Quick Emergency Troubleshooting

- **Browser showing old cached JS/CSS?**
  Press `Ctrl + Shift + R` (Hard Reload) or use an Incognito / Private browsing window.
- **Port 8080 or 5500 already in use?**
  In PowerShell:
  ```powershell
  # Kill process on port 8080:
  Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess -Force
  # Kill process on port 5500:
  Stop-Process -Id (Get-NetTCPConnection -LocalPort 5500).OwningProcess -Force
  ```
- **Reset Database to clean seed state?**
  ```powershell
  C:\xampp\mysql\bin\mysql.exe -u root smartagri < database.sql
  ```
- **Run automated test validation?**
  ```powershell
  cd backend\smartagri-springboot
  python run_all_tests.py
  ```
