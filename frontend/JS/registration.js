const registrationForm = document.getElementById("registrationForm");
const msg = document.getElementById("msg");
const togglePassword = document.getElementById("togglePassword");

togglePassword.addEventListener("click", function () {
    const password = document.getElementById("password");
    const icon = this.querySelector("i");

    password.type = password.type === "password" ? "text" : "password";
    icon.classList.toggle("bi-eye");
    icon.classList.toggle("bi-eye-slash");
});

registrationForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    const name = document.getElementById("name").value.trim();
    const username = document.getElementById("username").value.trim();
    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    const confirmPassword = document.getElementById("confirmPassword").value;
    const role = document.getElementById("role").value;
    const button = document.getElementById("registerButton");

    if (password !== confirmPassword) {
        msg.innerHTML = '<div class="alert alert-danger">Passwords do not match.</div>';
        return;
    }

    if (role === "ADMIN") {
        msg.innerHTML = '<div class="alert alert-danger">Registration with role ADMIN is not permitted.</div>';
        return;
    }

    button.disabled = true;
    button.innerHTML = 'Creating account...';
    msg.innerHTML = "";

    try {
        const response = await fetch(API_URL + "/auth/register", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name, username, email, password, role })
        });

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(response.status === 400 ? (data.message || "Registration details are invalid.") : "Unable to create account. Please try again.");
        }

        msg.innerHTML = '<div class="alert alert-success">Account created successfully. Redirecting to login...</div>';
        registrationForm.reset();

        setTimeout(() => {
            window.location.href = "login.html";
        }, 1200);
    } catch (error) {
        msg.innerHTML = '<div class="alert alert-danger">' + error.message + "</div>";
    } finally {
        button.disabled = false;
        button.innerHTML = 'Create Account <i class="bi bi-person-plus"></i>';
    }
});
