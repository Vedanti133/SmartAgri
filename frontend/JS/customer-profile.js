/**
 * SmartAgri - Customer Profile & Delivery Address Management
 */

let currentProfile = null;
let savedAddresses = [];

document.addEventListener('DOMContentLoaded', async () => {
    // 1. Verify authentication
    if (typeof requireLogin === 'function') {
        requireLogin();
    }

    // 2. Setup forms
    setupProfileForms();

    // 3. Load profile and addresses
    await loadProfile();
    await loadAddresses();
    if (typeof updateCartBadge === 'function') {
        await updateCartBadge();
    }
});

function setupProfileForms() {
    const editForm = document.getElementById('editProfileForm');
    if (editForm) {
        editForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await onSaveProfile();
        });
    }

    const addAddrForm = document.getElementById('addAddressForm');
    if (addAddrForm) {
        addAddrForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await onSaveAddress();
        });
    }
}

async function loadProfile() {
    try {
        const res = await ApiClient.get('/profile');
        if (res && res.success && res.data) {
            currentProfile = res.data;
            renderProfile(currentProfile);
        }
    } catch (e) {
        console.error('Failed to load profile', e);
    }
}

function renderProfile(profile) {
    const name = profile.fullName || profile.username || 'Valued Customer';
    document.getElementById('profileFullName').textContent = name;
    document.getElementById('profileUsername').textContent = `@${profile.username || 'user'}`;
    document.getElementById('profileRoleBadge').textContent = profile.role || 'CUSTOMER';
    document.getElementById('profileEmail').textContent = profile.email || 'Not provided';
    document.getElementById('profilePhone').textContent = profile.phone || 'Not provided';

    const avatar = document.getElementById('avatarInitial');
    if (avatar) {
        avatar.textContent = name.charAt(0).toUpperCase();
    }

    // Pre-fill edit modal inputs
    document.getElementById('editFullName').value = profile.fullName || '';
    document.getElementById('editPhone').value = profile.phone || '';
    document.getElementById('editEmail').value = profile.email || '';
}

async function onSaveProfile() {
    const btn = document.getElementById('saveProfileBtn');
    const msg = document.getElementById('profileMsg');
    const fullName = document.getElementById('editFullName').value.trim();
    const phone = document.getElementById('editPhone').value.trim();
    const email = document.getElementById('editEmail').value.trim();

    btn.disabled = true;
    btn.textContent = 'Saving...';
    msg.innerHTML = '';

    try {
        const res = await ApiClient.put('/profile', { fullName, phone, email });
        if (res && res.success && res.data) {
            currentProfile = res.data;
            renderProfile(currentProfile);

            // Update user in localStorage
            const user = getUser();
            if (user) {
                user.name = currentProfile.fullName;
                user.email = currentProfile.email;
                localStorage.setItem('smartagriUser', JSON.stringify(user));
                if (typeof applyUser === 'function') {
                    applyUser();
                }
            }

            const modalEl = document.getElementById('editProfileModal');
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) {
                modalInstance.hide();
            }

            showToast('Profile updated successfully!', 'success');
        } else {
            msg.innerHTML = `<div class="alert alert-danger py-2 small">${res.message || 'Update failed'}</div>`;
        }
    } catch (e) {
        msg.innerHTML = `<div class="alert alert-danger py-2 small">${e.message}</div>`;
    } finally {
        btn.disabled = false;
        btn.textContent = 'Save Changes';
    }
}

async function loadAddresses() {
    const grid = document.getElementById('addressesGrid');
    try {
        const res = await ApiClient.get('/addresses');
        if (res && res.success && Array.isArray(res.data)) {
            savedAddresses = res.data;
            renderAddresses(savedAddresses);
        } else {
            grid.innerHTML = '<div class="col-12 text-center text-muted py-4">No saved addresses found.</div>';
        }
    } catch (e) {
        grid.innerHTML = `<div class="col-12 text-center text-danger py-4">Failed to load addresses: ${e.message}</div>`;
    }
}

function renderAddresses(addresses) {
    const grid = document.getElementById('addressesGrid');
    if (!addresses || addresses.length === 0) {
        grid.innerHTML = `
            <div class="col-12">
                <div class="p-4 text-center border rounded-3 bg-white">
                    <i class="bi bi-geo-alt text-muted fs-1"></i>
                    <h6 class="fw-bold mt-2 text-secondary">No delivery addresses saved yet</h6>
                    <p class="text-muted small mb-0">Add your farm, warehouse, or home address for smooth fertilizer logistics.</p>
                </div>
            </div>
        `;
        return;
    }

    grid.innerHTML = addresses.map(addr => {
        const typeBadgeClass = `badge-${(addr.addressType || 'home').toLowerCase()}`;
        return `
            <div class="col-md-6">
                <div class="address-card ${addr.default ? 'is-default' : ''}">
                    <div class="d-flex justify-content-between align-items-center mb-2">
                        <span class="address-type-badge ${typeBadgeClass}">${addr.addressType || 'HOME'}</span>
                        ${addr.default ? '<span class="badge bg-success-subtle text-success fw-bold small"><i class="bi bi-check-circle-fill me-1"></i>Default Delivery Address</span>' : ''}
                    </div>

                    <h6 class="fw-bold mb-1">${addr.fullName}</h6>
                    <div class="small text-muted mb-2"><i class="bi bi-telephone me-1"></i>+91 ${addr.phone}</div>

                    <div class="small text-secondary mb-3">
                        ${addr.addressLine1}
                        ${addr.addressLine2 ? '<br>' + addr.addressLine2 : ''}
                        <br>${addr.city}${addr.district ? ', ' + addr.district : ''}, ${addr.state} - <strong>${addr.pincode}</strong>
                        ${addr.landmark ? '<br><span class="text-muted"><i class="bi bi-pin-map me-1"></i>Near ' + addr.landmark + '</span>' : ''}
                    </div>

                    <div class="d-flex justify-content-between align-items-center pt-2 border-top">
                        ${!addr.default ? `
                            <button class="btn btn-outline-success btn-sm" onclick="onSetDefaultAddress(${addr.id})">
                                <i class="bi bi-star me-1"></i> Set as Default
                            </button>
                        ` : '<span class="small text-success fw-semibold"><i class="bi bi-check-lg me-1"></i>Primary Address</span>'}

                        <button class="btn btn-outline-danger btn-sm" onclick="onDeleteAddress(${addr.id})" title="Delete Address">
                            <i class="bi bi-trash3"></i>
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

async function onSaveAddress() {
    const btn = document.getElementById('saveAddressBtn');
    const msg = document.getElementById('addressMsg');

    const fullName = document.getElementById('addrFullName').value.trim();
    const phone = document.getElementById('addrPhone').value.trim();
    const addressLine1 = document.getElementById('addrLine1').value.trim();
    const addressLine2 = document.getElementById('addrLine2').value.trim();
    const city = document.getElementById('addrCity').value.trim();
    const district = document.getElementById('addrDistrict') ? document.getElementById('addrDistrict').value.trim() : null;
    const state = document.getElementById('addrState').value.trim();
    const pincode = document.getElementById('addrPincode').value.trim();
    const landmark = document.getElementById('addrLandmark').value.trim();
    const addressType = document.getElementById('addrType').value;
    const isDefault = document.getElementById('addrIsDefault').checked;

    // PIN code validation
    if (!/^[1-9][0-9]{5}$/.test(pincode)) {
        msg.innerHTML = '<div class="alert alert-danger py-2 small">Please enter a valid 6-digit Indian PIN code.</div>';
        return;
    }

    // Phone validation
    if (!/^[0-9]{10}$/.test(phone)) {
        msg.innerHTML = '<div class="alert alert-danger py-2 small">Please enter a valid 10-digit mobile number.</div>';
        return;
    }

    btn.disabled = true;
    btn.textContent = 'Saving Address...';
    msg.innerHTML = '';

    try {
        const payload = {
            fullName,
            phone,
            addressLine1,
            addressLine2: addressLine2 || null,
            city,
            district: district || null,
            state,
            pincode,
            landmark: landmark || null,
            addressType,
            default: isDefault
        };

        const res = await ApiClient.post('/addresses', payload);
        if (res && res.success) {
            showToast('Address added successfully!', 'success');
            document.getElementById('addAddressForm').reset();

            const modalEl = document.getElementById('addAddressModal');
            const modalInstance = bootstrap.Modal.getInstance(modalEl);
            if (modalInstance) {
                modalInstance.hide();
            }

            await loadAddresses();
        } else {
            msg.innerHTML = `<div class="alert alert-danger py-2 small">${res.message || 'Failed to save address'}</div>`;
        }
    } catch (e) {
        msg.innerHTML = `<div class="alert alert-danger py-2 small">${e.message}</div>`;
    } finally {
        btn.disabled = false;
        btn.textContent = 'Save Address';
    }
}

async function onSetDefaultAddress(id) {
    try {
        const res = await ApiClient.put(`/addresses/${id}/default`);
        if (res && res.success) {
            showToast('Default address updated!', 'success');
            await loadAddresses();
        } else {
            showToast(res.message || 'Failed to set default address', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Error updating default address', 'danger');
    }
}

async function onDeleteAddress(id) {
    if (!confirm('Are you sure you want to delete this delivery address?')) return;

    try {
        const res = await ApiClient.delete(`/addresses/${id}`);
        if (res && res.success) {
            showToast('Address deleted successfully', 'success');
            await loadAddresses();
        } else {
            showToast(res.message || 'Failed to delete address', 'danger');
        }
    } catch (e) {
        showToast(e.message || 'Error deleting address', 'danger');
    }
}

function showToast(message, type = 'success') {
    const toastEl = document.getElementById('liveToast');
    const toastMsg = document.getElementById('toastMessage');
    if (toastEl && toastMsg) {
        toastEl.className = `toast align-items-center text-white bg-${type === 'danger' ? 'danger' : 'success'} border-0 shadow`;
        toastMsg.textContent = message;
        const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
        toast.show();
    }
}
