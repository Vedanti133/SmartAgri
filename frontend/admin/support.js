/**
 * SmartAgri - Admin Customer Service & Support Console JS
 */

let allTickets = [];
let activeAdminTicketId = null;

document.addEventListener('DOMContentLoaded', async () => {
    checkAdminAuth();
    await loadAllTickets();
});

function checkAdminAuth() {
    const token = localStorage.getItem('token') || localStorage.getItem('smartagriAuthToken') || localStorage.getItem('smartagri_auth_token');
    const userStr = localStorage.getItem('user') || localStorage.getItem('smartagriUser') || localStorage.getItem('smartagri_user');
    const role = localStorage.getItem('role');
    const loginPath = window.location.pathname.includes('/admin/') ? '../HTML/login.html' : 'login.html';

    if (!token) {
        window.location.href = loginPath;
        return;
    }
    if (userStr || role) {
        try {
            const user = userStr ? JSON.parse(userStr) : { role: role };
            const effectiveRole = user.role || role;
            if (effectiveRole !== 'ADMIN' && effectiveRole !== 'ROLE_ADMIN') {
                alert('Access restricted to administrators.');
                window.location.href = loginPath;
                return;
            }
            const greeting = document.getElementById('adminUserGreeting');
            if (greeting) greeting.textContent = `${user.name || user.username || 'Admin'} (${effectiveRole})`;
        } catch (e) {}
    }
}

function logoutAdmin() {
    ['smartagriUser', 'smartagri_user', 'user', 'smartagriAuthToken', 'smartagri_auth_token', 'token', 'role', 'username'].forEach(k => localStorage.removeItem(k));
    window.location.href = window.location.pathname.includes('/admin/') ? '../HTML/login.html' : 'login.html';
}

async function loadAllTickets() {
    const status = document.getElementById('ticketStatusFilter').value;
    const url = status ? `/admin/support/tickets?status=${status}` : '/admin/support/tickets';
    const listContainer = document.getElementById('adminTicketList');

    try {
        const res = await ApiClient.get(url);
        if (res && res.success && Array.isArray(res.data)) {
            allTickets = res.data;
            renderTicketsList(allTickets);
        }
    } catch (e) {
        listContainer.innerHTML = `<div class="p-3 text-danger text-center">${e.message}</div>`;
    }
}

function renderTicketsList(tickets) {
    const listContainer = document.getElementById('adminTicketList');
    if (!tickets || tickets.length === 0) {
        listContainer.innerHTML = '<div class="p-4 text-center text-muted">No tickets found.</div>';
        return;
    }

    const statusBadges = {
        'OPEN': 'bg-primary',
        'IN_PROGRESS': 'bg-warning text-dark',
        'WAITING_FOR_CUSTOMER': 'bg-info text-dark',
        'RESOLVED': 'bg-success',
        'CLOSED': 'bg-dark'
    };

    listContainer.innerHTML = tickets.map(t => {
        const isActive = t.id === activeAdminTicketId ? 'active' : '';
        const badge = statusBadges[t.status] || 'bg-secondary';
        const dateStr = t.createdAt ? new Date(t.createdAt).toLocaleDateString() : '';

        return `
            <div class="list-group-item list-group-item-action ticket-list-item p-3 ${isActive}" onclick="selectAdminTicket(${t.id})">
                <div class="d-flex justify-content-between align-items-center mb-1">
                    <span class="fw-bold">${t.ticketNumber}</span>
                    <span class="badge ${badge}">${t.status}</span>
                </div>
                <div class="fw-semibold text-truncate mb-1">${t.subject}</div>
                <div class="d-flex justify-content-between align-items-center small text-muted">
                    <span><i class="bi bi-person me-1"></i>${t.customerName}</span>
                    <span>${dateStr}</span>
                </div>
            </div>
        `;
    }).join('');
}

function filterTicketList() {
    const q = document.getElementById('ticketSearchInput').value.toLowerCase();
    const filtered = allTickets.filter(t =>
        (t.ticketNumber && t.ticketNumber.toLowerCase().includes(q)) ||
        (t.subject && t.subject.toLowerCase().includes(q)) ||
        (t.customerName && t.customerName.toLowerCase().includes(q))
    );
    renderTicketsList(filtered);
}

async function selectAdminTicket(ticketId) {
    activeAdminTicketId = ticketId;
    renderTicketsList(allTickets);

    const header = document.getElementById('adminConvoHeader');
    const body = document.getElementById('adminConvoBody');
    const footer = document.getElementById('adminConvoFooter');

    body.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-success"></div></div>';

    try {
        const res = await ApiClient.get(`/admin/support/tickets/${ticketId}`);
        if (res && res.success && res.data) {
            const ticket = res.data;
            header.innerHTML = `
                <div>
                    <h6 class="fw-bold mb-0">${ticket.ticketNumber}: ${ticket.subject}</h6>
                    <small class="text-muted">
                        Customer: <strong>${ticket.customerName}</strong> (${ticket.customerEmail}) •
                        Order: <strong>${ticket.orderTrackingNumber || '#' + (ticket.orderId || 'None')}</strong> •
                        Status: <span class="badge bg-secondary">${ticket.status}</span>
                    </small>
                </div>
                <div>
                    <span class="badge bg-light text-dark border">Priority: ${ticket.priority}</span>
                </div>
            `;

            document.getElementById('assignAgentInput').value = ticket.assignedTo || '';

            const messages = ticket.messages || [];
            if (messages.length === 0) {
                body.innerHTML = '<div class="text-center py-5 text-muted">No messages found.</div>';
            } else {
                body.innerHTML = messages.map(m => {
                    const isStaff = m.senderType === 'ADMIN';
                    const bubbleClass = isStaff ? 'chat-bubble-admin' : 'chat-bubble-customer';
                    const senderLabel = isStaff ? `Staff (${m.senderName || 'Support'})` : (m.senderName || 'Customer');
                    const timeStr = m.createdAt ? new Date(m.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : '';

                    return `
                        <div class="chat-bubble ${bubbleClass}">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <strong class="small">${senderLabel}</strong>
                                <span class="small opacity-75 ms-3">${timeStr}</span>
                            </div>
                            <div>${m.message}</div>
                        </div>
                    `;
                }).join('');

                body.scrollTop = body.scrollHeight;
            }

            footer.classList.remove('d-none');
            document.getElementById('adminReplyInput').focus();
        }
    } catch (e) {
        body.innerHTML = `<div class="alert alert-danger">${e.message}</div>`;
    }
}

async function submitAdminReply(event) {
    event.preventDefault();
    if (!activeAdminTicketId) return;

    const input = document.getElementById('adminReplyInput');
    const msg = input.value.trim();
    if (!msg) return;

    const btn = document.getElementById('btnAdminSend');
    btn.disabled = true;

    try {
        const res = await ApiClient.post(`/admin/support/tickets/${activeAdminTicketId}/messages`, { message: msg });
        if (res && res.success) {
            input.value = '';
            await loadAllTickets();
            await selectAdminTicket(activeAdminTicketId);
        } else {
            alert('Reply failed: ' + (res.message || 'Unknown error'));
        }
    } catch (e) {
        alert('Reply error: ' + e.message);
    } finally {
        btn.disabled = false;
    }
}

async function assignActiveTicket() {
    if (!activeAdminTicketId) return;
    const agent = document.getElementById('assignAgentInput').value.trim();
    try {
        const res = await ApiClient.put(`/admin/support/tickets/${activeAdminTicketId}/assign`, { assignedTo: agent });
        if (res && res.success) {
            alert('Ticket assigned to ' + agent);
            await loadAllTickets();
            await selectAdminTicket(activeAdminTicketId);
        }
    } catch (e) {
        alert('Assign error: ' + e.message);
    }
}

async function resolveActiveTicket() {
    if (!activeAdminTicketId) return;
    try {
        const res = await ApiClient.put(`/admin/support/tickets/${activeAdminTicketId}/resolve`, {});
        if (res && res.success) {
            alert('Ticket marked as RESOLVED');
            await loadAllTickets();
            await selectAdminTicket(activeAdminTicketId);
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

async function closeActiveTicket() {
    if (!activeAdminTicketId) return;
    try {
        const res = await ApiClient.put(`/admin/support/tickets/${activeAdminTicketId}/close`, {});
        if (res && res.success) {
            alert('Ticket CLOSED');
            await loadAllTickets();
            await selectAdminTicket(activeAdminTicketId);
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}

async function updateActiveTicketStatus(status) {
    if (!activeAdminTicketId) return;
    try {
        const res = await ApiClient.put(`/admin/support/tickets/${activeAdminTicketId}/status`, { status: status });
        if (res && res.success) {
            await loadAllTickets();
            await selectAdminTicket(activeAdminTicketId);
        }
    } catch (e) {
        alert('Error: ' + e.message);
    }
}
