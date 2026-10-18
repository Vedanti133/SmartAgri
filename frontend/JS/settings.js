document.addEventListener("DOMContentLoaded", () => {

    // ==========================================
    // DEFAULT SETTINGS
    // ==========================================

    const defaults = {

        companyName: "SmartAgri",
        companyAddress: "",
        companyPhone: "",
        companyGst: "",

        invoicePrefix: "INV-",
        invoiceTax: "18",
        invoiceCurrency: "INR",

        lowStockAlerts: true
    };


    // ==========================================
    // SETTINGS ELEMENTS
    // ==========================================

    const settingIds = Object.keys(defaults);

    const saveButton =
        document.getElementById("saveSettings");

    const unlockButton =
        document.getElementById("unlockSettings");

    const resetButton =
        document.getElementById("resetSettings");

    const lockStatus =
        document.getElementById("settingsLockStatus");

    const message =
        document.getElementById("settingsMessage");


    // ==========================================
    // SHOW MESSAGE
    // ==========================================

    function showMessage(text, type = "success") {

        if (!message) {
            return;
        }

        message.className =
            `alert alert-${type}`;

        message.textContent = text;

        message.classList.remove("d-none");


        setTimeout(() => {

            message.classList.add("d-none");

        }, 2500);
    }


    // ==========================================
    // GET SETTINGS ELEMENTS
    // ==========================================

    function getSettingElements() {

        return settingIds
            .map(id => document.getElementById(id))
            .filter(element => element !== null);

    }


    // ==========================================
    // LOCK SETTINGS
    // ==========================================

    function lockSettings() {

        getSettingElements().forEach(element => {

            element.disabled = true;

        });


        // Hide Save button
        if (saveButton) {

            saveButton.classList.add("d-none");

        }


        // Show Unlock button
        if (unlockButton) {

            unlockButton.classList.remove("d-none");

        }


        // Show lock message
        if (lockStatus) {

            lockStatus.classList.remove("d-none");

        }


        localStorage.setItem(
            "smartagri_settings_locked",
            "true"
        );
    }


    // ==========================================
    // UNLOCK SETTINGS
    // ==========================================

    function unlockSettings() {

        getSettingElements().forEach(element => {

            element.disabled = false;

        });


        // Show Save button
        if (saveButton) {

            saveButton.classList.remove("d-none");

        }


        // Hide Unlock button
        if (unlockButton) {

            unlockButton.classList.add("d-none");

        }


        // Hide lock message
        if (lockStatus) {

            lockStatus.classList.add("d-none");

        }


        localStorage.setItem(
            "smartagri_settings_locked",
            "false"
        );
    }


    // ==========================================
    // LOAD SETTINGS
    // ==========================================

    function loadSettings() {

        let savedSettings = {};

        try {

            savedSettings = JSON.parse(
                localStorage.getItem(
                    "smartagri_settings"
                ) || "{}"
            );

        } catch (error) {

            console.error(
                "Error loading settings:",
                error
            );

            savedSettings = {};
        }


        settingIds.forEach(id => {

            const element =
                document.getElementById(id);

            if (!element) {
                return;
            }


            const value =
                savedSettings[id] !== undefined
                    ? savedSettings[id]
                    : defaults[id];


            if (element.type === "checkbox") {

                element.checked =
                    Boolean(value);

            } else {

                element.value = value;

            }

        });


        // ======================================
        // CHECK LOCK STATUS
        // ======================================

        const locked =
            localStorage.getItem(
                "smartagri_settings_locked"
            ) === "true";


        if (locked) {

            lockSettings();

        } else {

            unlockSettings();

        }

    }


    // ==========================================
    // SAVE SETTINGS
    // ==========================================

    function saveSettings() {

        const settings = {};


        settingIds.forEach(id => {

            const element =
                document.getElementById(id);

            if (!element) {
                return;
            }


            if (element.type === "checkbox") {

                settings[id] =
                    element.checked;

            } else {

                settings[id] =
                    element.value.trim();

            }

        });


        // Save settings
        localStorage.setItem(
            "smartagri_settings",
            JSON.stringify(settings)
        );


        // Lock after save
        lockSettings();


        showMessage(
            "Settings saved and locked successfully.",
            "success"
        );

    }


    // ==========================================
    // RESET SETTINGS
    // ==========================================

    function resetSettings() {

        const confirmReset =
            confirm(
                "Are you sure you want to reset all settings?"
            );


        if (!confirmReset) {
            return;
        }


        // Remove saved settings
        localStorage.removeItem(
            "smartagri_settings"
        );


        // Unlock settings
        localStorage.setItem(
            "smartagri_settings_locked",
            "false"
        );


        // Restore defaults
        settingIds.forEach(id => {

            const element =
                document.getElementById(id);

            if (!element) {
                return;
            }


            if (element.type === "checkbox") {

                element.checked =
                    Boolean(defaults[id]);

            } else {

                element.value =
                    defaults[id];

            }

        });


        // Make editable
        unlockSettings();


        showMessage(
            "Settings have been reset successfully.",
            "info"
        );

    }


    // ==========================================
    // BUTTON EVENTS
    // ==========================================

    if (saveButton) {

        saveButton.addEventListener(
            "click",
            saveSettings
        );

    }


    if (unlockButton) {

        unlockButton.addEventListener(
            "click",
            unlockSettings
        );

    }


    if (resetButton) {

        resetButton.addEventListener(
            "click",
            resetSettings
        );

    }


    // ==========================================
    // INITIALIZE
    // ==========================================

    loadSettings();

});