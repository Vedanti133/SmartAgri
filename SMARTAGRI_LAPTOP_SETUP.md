# SmartAgri — Laptop Setup & Deployment Guide

This guide provides instructions to transfer and execute the complete **SmartAgri** platform on another Windows laptop for demonstration or evaluation.

---

## 💻 1. Minimum System Requirements

- **Operating System:** Windows 10 or Windows 11 (64-bit)
- **Processor:** Intel Core i3 / AMD Ryzen 3 or higher
- **RAM:** Minimum 8 GB (16 GB recommended)
- **Disk Space:** At least 2 GB free disk space
- **Software Dependencies:**
  - **Java Development Kit (JDK):** Version 21 LTS (or 17 LTS)
  - **Database Engine:** MySQL 8.0 or MariaDB (via XAMPP)
  - **Web Browser:** Google Chrome, Microsoft Edge, or Mozilla Firefox
  - *(Optional)* **VS Code** with "Live Server" extension, or **Python 3.x**

---

## 📦 2. Step-by-Step Software Installation

### Step 2.1 — Install Java 21 LTS
1. Download JDK 21 from Oracle or Eclipse Adoptium:
   - Oracle JDK 21: [https://www.oracle.com/java/technologies/downloads/#jdk21-windows](https://www.oracle.com/java/technologies/downloads/#jdk21-windows)
   - Eclipse Temurin 21: [https://adoptium.net/](https://adoptium.net/)
2. Run the installer and keep the default installation path (e.g. `C:\Program Files\Java\jdk-21`).
3. Set the `JAVA_HOME` environment variable:
   - Press `Win + S`, type **Environment Variables**, and select **Edit the system environment variables**.
   - Click **Environment Variables...**.
   - Under **System Variables**, click **New**:
     - Variable name: `JAVA_HOME`
     - Variable value: `C:\Program Files\Java\jdk-21` *(adjust if installed elsewhere)*
   - Find `Path` under System Variables, click **Edit**, click **New**, and add:
     - `%JAVA_HOME%\bin`
   - Click **OK** to save.
4. Verify in a new PowerShell window:
   ```powershell
   java -version
   ```
   *Expected output: `openjdk version "21..."` or `java version "21..."`.*

---

### Step 2.2 — Install & Start MySQL (via XAMPP)
1. Download and install XAMPP from: [https://www.apachefriends.org/](https://www.apachefriends.org/).
2. Launch the **XAMPP Control Panel**.
3. Click the **Start** button next to **MySQL** (and optionally **Apache** if you wish to use phpMyAdmin).
4. Verify MySQL is running on port **3306** (indicated by green highlighting).

---

## 🗄️ 3. Database Creation & Schema Import

You can import the database using either the **Command Line (Fastest)** or **phpMyAdmin (GUI)**.

### Method A: Command Line (Recommended)
1. Open PowerShell and navigate to the project directory:
   ```powershell
   cd SmartAgri
   ```
2. Create the `smartagri` database and import `database.sql`:
   ```powershell
   # If MySQL root has no password (default XAMPP setup):
   C:\xampp\mysql\bin\mysql.exe -u root -e "CREATE DATABASE IF NOT EXISTS smartagri;"
   C:\xampp\mysql\bin\mysql.exe -u root smartagri < database.sql
   ```
   *(If your MySQL root user has a password, add `-p` to prompt for it: `mysql -u root -p smartagri < database.sql`)*

### Method B: phpMyAdmin GUI
1. Open your web browser and go to `http://localhost/phpmyadmin`.
2. Click **New** on the left navigation panel.
3. Enter database name: `smartagri`, collation: `utf8mb4_unicode_ci`, and click **Create**.
4. Select the newly created `smartagri` database.
5. Click the **Import** tab at the top.
6. Click **Choose File** and select `database.sql` from the project root.
7. Scroll to the bottom and click **Import**.
8. Verify that **44 tables** are created and populated.

---

## ⚙️ 4. Backend Configuration Check

The backend is configured in `backend/smartagri-springboot/src/main/resources/application.properties`.

Open this file in any text editor and verify the database connection settings:

```properties
# MySQL Connection Settings
spring.datasource.url=jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:smartagri}?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}

# Server Port
server.port=${SERVER_PORT:8080}

# Hibernate DDL Mode
spring.jpa.hibernate.ddl-auto=update
```

> **Note:** The configuration uses default environment variable fallbacks (`root` user, empty password, `localhost:3306`). If your MySQL server uses a custom password, either set the environment variable `$env:DB_PASSWORD = "yourpassword"` or edit `spring.datasource.password=yourpassword` directly.

---

## 🚀 5. Starting the Spring Boot Backend

1. Open PowerShell and navigate to the Spring Boot backend directory:
   ```powershell
   cd SmartAgri\backend\smartagri-springboot
   ```
2. Launch the application using the Maven Wrapper (no separate Maven installation required!):
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
3. Watch the terminal logs. When you see:
   ```
   INFO: Started SmartAgriApplication in X.XXX seconds
   ```
   the backend is active and ready to accept requests at `http://localhost:8080/api`.

---

## 🌐 6. Launching the Frontend

You can run the frontend using either VS Code Live Server, Python's built-in HTTP server, or by opening the HTML files directly.

### Method A: Python HTTP Server (Zero-install method)
1. Open a new PowerShell window and navigate to the frontend folder:
   ```powershell
   cd SmartAgri\frontend
   python -m http.server 5500
   ```
2. Open your web browser and navigate to:
   ```
   http://localhost:5500/HTML/customer-catalog.html
   ```

### Method B: VS Code Live Server
1. Open the `SmartAgri` project folder in Visual Studio Code.
2. In the Extensions tab (`Ctrl + Shift + X`), ensure **Live Server** (by Ritwick Dey) is installed.
3. Right-click on `frontend/HTML/customer-catalog.html` or `frontend/HTML/login.html` and select **Open with Live Server**.
4. The page will open automatically at `http://127.0.0.1:5500`.

---

## 🔑 7. Default Credentials for Live Demonstration

The application comes pre-loaded with demonstration accounts:

### 1. System Administrator
- **Username:** `admin`
- **Password:** `adminpassword123`
- **Role:** `ADMIN`
- **Access URL:** `http://localhost:5500/HTML/admin-dashboard.html`
- **Capabilities:** Multi-warehouse inventory, purchase procurement, pick/pack/dispatch, returns quality inspection, refund processing, helpdesk ticketing.

### 2. Verified Farmer (Customer)
- **Username:** `farmer_ramesh`
- **Password:** `ramesh123`
- **Role:** `CUSTOMER`
- **Access URL:** `http://localhost:5500/HTML/customer-catalog.html`
- **Capabilities:** Product browsing, nutrient search, cart & wishlist, order placement, order tracking, downloadable GST invoices, return requests, support tickets.

---

## 🛠️ 8. Common Troubleshooting & FAQ

| Problem | Cause | Resolution |
| :--- | :--- | :--- |
| **Port 8080 already in use** | Another service or orphaned process is bound to port 8080. | Run `Get-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess \| Stop-Process -Force` in PowerShell, then re-run `./mvnw.cmd spring-boot:run`. Alternatively, change `server.port=8081` in `application.properties`. |
| **Access denied for user 'root'@'localhost'** | MySQL root has a password that does not match `application.properties`. | Edit `spring.datasource.password=your_mysql_password` in `application.properties`. |
| **Communications link failure (Port 3306)** | MySQL service is not running. | Open XAMPP Control Panel and click **Start** next to MySQL. Ensure port 3306 is not blocked. |
| **CORS policy blocked by browser** | Accessing HTML via `file:///` path directly. | Serve the frontend through an HTTP server (e.g., `python -m http.server 5500` or VS Code Live Server). |
| **Java version error (`UnsupportedClassVersionError`)** | Java runtime is older than JDK 17/21. | Download and install JDK 21 LTS and verify `java -version` returns 21. |
| **`mvnw.cmd` execution policy error** | PowerShell script execution disabled. | Run `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` in the current terminal window. |

---

## 🎯 9. Verification & Automated Test Certification

To verify that the entire backend, database, and all 17 business modules are operating with 100% integrity on this laptop:
```powershell
cd SmartAgri\backend\smartagri-springboot
python run_all_tests.py
```
*Expected Result: **635 / 635 tests passed (100.0%) across all 13 test suites (Phases 5 through 17)** in under 25 seconds.*

You can also run individual phase test suites if desired:
```powershell
python test_phase17.py    # 40 / 40 passed (Part 1-7 End-to-End Audit)
python test_phase16.py    # 37 / 37 passed (Phase 16 Readiness)
python test_phase15.py    # 73 / 73 passed (Returns & Support)
python test_phase14.py    # 74 / 74 passed (Fulfillment & Shipping)
```

---

## 📋 10. Evaluator Demonstration Walkthrough
For the recommended 15-step evaluator demonstration flow, refer to `SMARTAGRI_STARTUP_CHECKLIST.md`.

