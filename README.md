# SmartAgri — Enterprise Fertilizer Sales, Inventory & E-Commerce Management Platform

[![System Status](https://img.shields.io/badge/System-Production%20Ready-brightgreen)](https://github.com/)
[![Java](https://img.shields.io/badge/Java-21%20LTS-orange)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-brightgreen)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.io/badge/Database-MySQL%208.0%20%2F%20XAMPP-blue)](https://www.mysql.com/)
[![Tests](https://img.shields.io/badge/Automated%20Tests-635%20%2F%20635%20Passed%20(100%25)-success)](backend/smartagri-springboot)

---

## 🌾 Project Overview

**SmartAgri** is a comprehensive, production-grade enterprise full-stack web platform built for fertilizer e-commerce, multi-warehouse inventory management, purchase procurement, batch & lot FEFO/FIFO tracking, dispatch fulfillment, reverse logistics, and customer support helpdesk ticketing.

Developed incrementally across **17 comprehensive engineering phases**, SmartAgri bridges rural farmers with certified fertilizer manufacturers, providing transparent pricing, subsidy compliance, government regulation tracking, real-time logistics tracking, automated returns inspection, and instant GST invoices.

---

## 🏗️ Architecture & Technology Stack

```
   ┌─────────────────────────────────────────────────────────────┐
   │                   SmartAgri Web Frontend                   │
   │  (HTML5, CSS3, Bootstrap 5, Modern Vanilla JS, Responsive)  │
   └───────────────┬─────────────────────────────▲───────────────┘
                   │ HTTP / REST API Calls       │ JSON Responses
                   │ (JWT Bearer Auth)           │
   ┌───────────────▼─────────────────────────────┴───────────────┐
   │             Spring Boot 3.5.4 Enterprise Backend            │
   │   - Spring Web (REST Controllers & Global Exception Handler) │
   │   - Spring Security & JWT Token Provider (Role-Based RBAC)   │
   │   - Spring Data JPA & Hibernate ORM                          │
   │   - Dynamic DatabaseSeeder (Idempotent seed & hash sync)    │
   └───────────────┬─────────────────────────────▲───────────────┘
                   │ JPA / Hibernate Queries     │ Result Sets
                   │ (Pessimistic Write Locks)   │
   ┌───────────────▼─────────────────────────────┴───────────────┐
   │                       MySQL 8.0 / XAMPP                     │
   │            (44 Relational Tables, Schema & Dumps)           │
   └─────────────────────────────────────────────────────────────┘
```

### Technology Highlights
- **Backend:** Java 21 LTS, Spring Boot 3.5.4, Spring Security, JWT (HMAC-SHA256), Spring Data JPA, Hibernate, Jakarta Validation, Lombok.
- **Frontend:** Responsive HTML5, Custom CSS3 Design System, Bootstrap 5.3, Vanilla ES6+ JavaScript, FontAwesome Icons.
- **Database:** MySQL 8.0 (InnoDB engine, foreign keys, transaction isolation, pessimistic locking).
- **Tooling:** Apache Maven (via Maven Wrapper `mvnw`), Python 3 automated regression test suites.

---

## 🚀 Key Modules Across Phases 1–17

1. **Authentication & RBAC (Phases 1–4):** Dual-role security (`ADMIN` vs `CUSTOMER`), BCrypt password encryption, stateless JWT sessions, customer profile and address book management.
2. **Order & GST Invoice Engine (Phase 5):** Multi-item order placement, SAC/HSN code mapping, CGST/SGST/IGST tax calculation, downloadable printable GST invoices.
3. **Product Discovery & Voice Search (Phase 6):** Advanced filtering by N-P-K nutrient composition, crop suitability, fertilizer form (Granular, Liquid, Powder), and Web Speech API voice search.
4. **Customer Experience & Wishlist (Phase 7):** Persistent shopping cart, item quantity controls, stock availability checks, and customer wishlist toggling.
5. **Checkout & Address Management (Phase 8):** Multi-address management (Home, Farm, Warehouse), default address selection, order review summary, and checkout validation.
6. **Payment Gateway Lifecycle (Phase 9):** Simulation of UPI, Net Banking, Credit/Debit Cards, and Cash on Delivery (COD); payment verification, retry mechanics, and transaction ledger.
7. **Inventory Management & Transactions (Phase 10):** Low-stock threshold alerts, restock auditing, safety stock warnings, and immutable `inventory_transactions` ledger.
8. **Batch & Expiry Management (Phase 11):** Manufacturing and expiry dates, quarantine flags, automated FEFO (First-Expiry-First-Out) batch allocation during order processing.
9. **Multi-Warehouse & Transfers (Phase 12):** Multi-warehouse directory (General, Cold Storage, Hazardous Chemical), zone and bin hierarchy, inter-warehouse transfer requests with transit tracking.
10. **Procurement & Goods Receipt (Phase 13):** Supplier management, purchase order (PO) generation, Goods Receipt Note (GRN) inspection, putaway workflows, and automatic batch registration.
11. **Dispatch & Shipment Fulfillment (Phase 14):** Pick lists, package creation with volumetric & chargeable weight calculations, delivery partner integrations, real-time shipment milestone tracking, and Return-to-Origin (RTO).
12. **Returns, Refunds & Customer Support (Phase 15):** 14-day RMA returns, batch lineage preservation, return pickup scheduling, dockside quality inspection (ACCEPTED / REJECTED / QUARANTINED), automated inventory restoration, COD bank detail capture & refunds, ₹0 replacement orders, and multi-turn customer support ticketing.
13. **Production Readiness & Laptop Portability (Phase 16):** Fully portable database dumps (`database.sql`, `smartagri_schema.sql`), configurable environment properties (`application.properties`), session key normalization, synchronized UI navigation, and comprehensive 595-test verification suite.
14. **Final System Audit, Bug Fixing & Submission Readiness (Phase 17):** End-to-end full system audit, frontend broken link remediation, static asset validation, cross-tenant isolation enforcement, zero-orphan relational integrity verification, 15-step evaluator demonstration flow, and 635/635 automated regression test suite.

---

## 🔑 Demo & Test Credentials

The database seeder automatically creates and synchronizes default credentials on application startup:

| Role | Username | Password | Access / Capabilities |
| :--- | :--- | :--- | :--- |
| **System Administrator** | `admin` | `adminpassword123` | Full access to Admin Console, Multi-Warehouse Operations, Procurement, Fulfillment, Returns QC, Refunds, Helpdesk |
| **Customer (Farmer)** | `farmer_ramesh` | `ramesh123` | Full access to Public Catalog, Shopping Cart, Wishlist, Checkout, Order Tracking, Invoices, RMA Returns, Support Tickets |

---

## ⚡ Quick Start Guide (Windows)

### 1. Prerequisites
- **Java Development Kit (JDK):** Version 21 (or 17+)
- **MySQL Database:** Running locally on port 3306 (via XAMPP or standalone MySQL Server)
- **Web Browser:** Google Chrome, Microsoft Edge, or Firefox

### 2. Database Initialization
1. Open XAMPP Control Panel and start the **MySQL** module.
2. Open PowerShell or Command Prompt and create the database:
   ```powershell
   mysql -u root -e "CREATE DATABASE IF NOT EXISTS smartagri;"
   ```
3. Import the pre-populated seed data:
   ```powershell
   mysql -u root smartagri < database.sql
   ```
   *(Alternatively, import `database.sql` through phpMyAdmin at `http://localhost/phpmyadmin`)*

### 3. Configure & Start Backend
1. Open `backend/smartagri-springboot/src/main/resources/application.properties` and verify your MySQL credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/smartagri?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=
   ```
2. Navigate to the backend directory and launch Spring Boot:
   ```powershell
   cd backend\smartagri-springboot
   .\mvnw.cmd spring-boot:run
   ```
3. The backend will boot on `http://localhost:8080`.

### 4. Launch Frontend
1. Open `frontend/HTML/customer-catalog.html` or `frontend/HTML/login.html` directly in your browser, or serve using VS Code Live Server / Python:
   ```powershell
   cd frontend
   python -m http.server 5500
   ```
2. Access the application in your browser:
   - **Customer Portal:** `http://localhost:5500/HTML/customer-catalog.html`
   - **Admin Console:** `http://localhost:5500/HTML/admin-dashboard.html`
   - **Login Page:** `http://localhost:5500/HTML/login.html`

---

## 🧪 Comprehensive Automated Test Results

SmartAgri features a 100% automated regression test suite covering all functional modules:

| Test Suite | Module Description | Test Count | Pass Rate |
| :--- | :--- | :---: | :---: |
| `test_phase5.py` | Order Placement, Delivery Status & GST Invoice Generation | 34 / 34 | 100% |
| `test_phase6.py` | Advanced Product Search, Nutrient Filters & Sorting | 33 / 33 | 100% |
| `test_phase7.py` | Shopping Cart, Quantity Controls & Customer Wishlist | 32 / 32 | 100% |
| `test_phase8.py` | Customer Checkout, Address Validation & Order Review | 42 / 42 | 100% |
| `test_phase9.py` | Payment Gateways (UPI/Card/NetBanking/COD) & Lifecycle | 44 / 44 | 100% |
| `test_phase10.py` | Inventory Transactions, Safety Stock & Threshold Alerts | 52 / 52 | 100% |
| `test_phase11.py` | Batch & Lot Expiry Tracking, FEFO Allocation Algorithm | 53 / 53 | 100% |
| `test_phase12.py` | Multi-Warehouse Management & Stock Transfer Dispatch | 60 / 60 | 100% |
| `test_phase13.py` | Supplier Procurement, Purchase Orders & GRN Inspection | 61 / 61 | 100% |
| `test_phase14.py` | Pick Lists, Packing, Shipments & Fulfillment Tracking | 74 / 74 | 100% |
| `test_phase15.py` | Reverse Logistics, QC Restocking, Refunds & Helpdesk | 73 / 73 | 100% |
| `test_phase16.py` | Portable DB Dumps, Seeding, Cross-Phase Health Checks | 37 / 37 | 100% |
| `test_phase17.py` | Comprehensive End-to-End Audit, Invariants & Submission Certification | 40 / 40 | 100% |
| **TOTAL** | **Enterprise Full-Stack Verification (13 Suites)** | **635 / 635** | **100%** |

To execute the entire automated regression harness:
```powershell
cd backend\smartagri-springboot
python run_all_tests.py
```

---

## 📂 Project Directory Structure

```
SmartAgri/
├── database.sql                        # Complete portable database dump (schema + data)
├── smartagri_schema.sql                # Clean DDL database schema definition
├── README.md                           # Master project documentation
├── SMARTAGRI_LAPTOP_SETUP.md           # Step-by-step laptop transfer & setup guide
├── SMARTAGRI_STARTUP_CHECKLIST.md      # 5-minute pre-demo startup checklist
├── .gitignore                          # Build artifacts & temp file exclusions
│
├── backend/
│   └── smartagri-springboot/           # Spring Boot 3.5.4 Backend
│       ├── pom.xml                     # Maven dependencies
│       ├── mvnw / mvnw.cmd             # Maven Wrapper scripts
│       ├── application-example.properties # Portable configuration template
│       ├── src/main/java/com/smartagri/
│       │   ├── controller/             # REST API Controllers (20+ controllers)
│       │   ├── entity/                 # JPA Entities (44 relational entities)
│       │   ├── repository/             # Spring Data JPA Repositories
│       │   ├── service/                # Transactional Business Logic
│       │   ├── dto/                    # Data Transfer Objects
│       │   └── config/                 # Security, JWT, CORS, DatabaseSeeder
│       ├── src/main/resources/
│       │   └── application.properties  # Database and environment configurations
│       └── test_phase*.py              # Automated regression test suites (Phases 5-16)
│
└── frontend/                           # Responsive Web Frontend
    ├── HTML/                           # User & Admin HTML Web Pages
    │   ├── login.html                  # Unified login page
    │   ├── customer-catalog.html       # Public fertilizer product catalog
    │   ├── customer-cart.html          # Shopping cart page
    │   ├── customer-orders.html        # Order history with Return/Replace actions
    │   ├── customer-order-tracking.html # Real-time delivery timeline tracking
    │   ├── returns.html                # Customer RMA return submission & tracking
    │   ├── support.html                # Customer helpdesk ticketing system
    │   ├── admin-dashboard.html        # Main admin operations hub
    │   ├── fulfillment.html            # Pick, pack & dispatch console
    │   ├── procurement.html            # Supplier PO & GRN receiving
    │   ├── warehouses.html             # Multi-warehouse & bin management
    │   ├── transfers.html              # Stock transfers between warehouses
    │   ├── batches.html                # Batch & expiry management
    │   ├── admin-returns.html          # Return inspection & QC restock console
    │   └── admin-support.html          # Support ticket triage & reply console
    ├── CSS/                            # Stylesheets & theme definitions
    ├── JS/                             # Client-side business logic & API bindings
    └── assets/                         # Static icons and logos
```

---

## 📄 License & Academic Attribution
Developed as part of the **SmartAgri Full-Stack Enterprise Fertilizer Management Project**. Suitable for academic demonstration, industrial evaluation, and production deployment.

## Multilingual UI

The frontend supports three selectable languages across all 51 HTML pages:
- English
- Hindi (हिंदी)
- Marathi (मराठी)

A language selector is added automatically by `frontend/JS/i18n.js`. The selected language is stored in browser local storage (`smartagri_lang`), so it remains selected when the user opens another SmartAgri page. The translation layer also observes dynamically-rendered UI such as tables, cards, alerts and status labels.

To add or correct a translation, update the `DICT` / `WORDS` objects in `frontend/JS/i18n.js`.

