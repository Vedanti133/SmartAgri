const loginForm = document.getElementById("loginForm");
const loginMessage = document.getElementById("msg");
const passwordInput = document.getElementById("password");
const usernameInput = document.getElementById("username");
const toggleButton = document.getElementById("toggle");

if (toggleButton && passwordInput) {
    toggleButton.addEventListener("click", function () {
        passwordInput.type = passwordInput.type === "password" ? "text" : "password";
        const icon = this.querySelector("i");
        if (icon) {
            icon.classList.toggle("bi-eye");
            icon.classList.toggle("bi-eye-slash");
        }
    });
}

if (loginForm) {
    loginForm.addEventListener("submit", async function (event) {
        event.preventDefault();

        loginMessage.innerHTML = '<div class="alert alert-info">Signing in...</div>';

        try {
            const apiBase = (typeof CONFIG !== 'undefined' && CONFIG.API_BASE_URL) 
                ? CONFIG.API_BASE_URL 
                : (typeof API_URL !== 'undefined' ? API_URL : 'http://localhost:8080/api');

            const response = await fetch(apiBase + "/auth/login", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    username: usernameInput.value.trim(),
                    password: passwordInput.value
                })
            });

            const data = await response.json().catch(() => ({}));

            if (!response.ok) {
                throw new Error(response.status === 401 ? "Invalid username or password." : (data.message || "Unable to sign in. Please try again."));
            }

            const userData = data.data.user || data.data;
            const token = data.data.token || "";
            const userJson = JSON.stringify(userData);

            // Store across all phase alias keys for complete cross-module interoperability
            localStorage.setItem("smartagriUser", userJson);
            localStorage.setItem("smartagri_user", userJson);
            localStorage.setItem("user", userJson);

            if (token) {
                localStorage.setItem("smartagriAuthToken", token);
                localStorage.setItem("smartagri_auth_token", token);
                localStorage.setItem("token", token);
            }

            if (userData.role) {
                localStorage.setItem("role", userData.role);
            }
            if (userData.username) {
                localStorage.setItem("username", userData.username);
            }

            loginMessage.innerHTML = '<div class="alert alert-success">Login successful! Redirecting...</div>';

            setTimeout(() => {
                const role = (userData.role || 'CUSTOMER').toUpperCase();
                let landing = "customer-catalog.html";
                if (role === "ADMIN") {
                    landing = "admin-dashboard.html";
                } else if (role === "SALES_STAFF") {
                    landing = "admin-orders.html";
                } else if (role === "SUPPLIER" || role === "SUPPLIERS") {
                    landing = "procurement.html";
                } else {
                    landing = "customer-catalog.html";
                }
                window.location.href = landing;
            }, 600);
        } catch (error) {
            loginMessage.innerHTML = '<div class="alert alert-danger">' + error.message + "</div>";
        }
    });
}
