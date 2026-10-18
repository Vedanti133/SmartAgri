/**
 * SmartAgri - Customer Support & Helpdesk JS
 */

let myTickets = [];
let activeTicketId = null;

document.addEventListener('DOMContentLoaded', async () => {
    checkCustomerAuth();
    await loadMyTickets();
});

function checkCustomerAuth() {
    const token = localStorage.getItem('token') || localStorage.getItem('smartagriAuthToken') || localStorage.getItem('smartagri_auth_token');
    const userStr = localStorage.getItem('user') || localStorage.getItem('smartagriUser') || localStorage.getItem('smartagri_user');
    if (!token) {
        window.location.href = 'login.html';
        return;
    }
    if (userStr) {
        try {
            const user = JSON.parse(userStr);
            const greeting = document.getElementById('userGreeting');
            if (greeting) greeting.textContent = `Hello, ${user.name || user.username || 'Customer'}`;
        } catch (e) {}
    }
}

function logout() {
    ['smartagriUser', 'smartagri_user', 'user', 'smartagriAuthToken', 'smartagri_auth_token', 'token', 'role', 'username'].forEach(k => localStorage.removeItem(k));
    window.location.href = 'login.html';
}

async function loadMyTickets() {
    const container = document.getElementById('ticketsListContainer');
    try {
        const res = await ApiClient.get('/customer/support/tickets');
        if (res && res.success && Array.isArray(res.data)) {
            myTickets = res.data;
            document.getElementById('ticketCount').textContent = myTickets.length;
            if (myTickets.length === 0) {
                container.innerHTML = `
                    <div class="text-center py-5 bg-white border rounded-3 p-4">
                        <i class="bi bi-chat-heart fs-1 text-muted"></i>
                        <h6 class="mt-3">No support tickets</h6>
                        <p class="text-muted small">Have an issue? Click "Open New Ticket" to contact us.</p>
                    </div>
                `;
                return;
            }

            container.innerHTML = myTickets.map(t => {
                const isActive = t.id === activeTicketId ? 'active' : '';
                const statusBadges = {
                    'OPEN': 'bg-primary',
                    'IN_PROGRESS': 'bg-warning text-dark',
                    'WAITING_FOR_CUSTOMER': 'bg-info text-dark',
                    'RESOLVED': 'bg-success',
                    'CLOSED': 'bg-dark'
                };
                const priorityBadges = {
                    'LOW': 'text-secondary',
                    'MEDIUM': 'text-primary',
                    'HIGH': 'text-warning',
                    'URGENT': 'text-danger fw-bold'
                };
                const statusBadge = statusBadges[t.status] || 'bg-secondary';
                const priorityClass = priorityBadges[t.priority] || '';
                const dateStr = t.createdAt ? new Date(t.createdAt).toLocaleDateString() : '';

                return `
                    <div class="ticket-card ${isActive}" onclick="selectTicket(${t.id})">
                        <div class="d-flex justify-content-between align-items-center mb-1">
                            <span class="fw-bold fs-6">${t.ticketNumber}</span>
                            <span class="badge ${statusBadge}">${t.status.replace(/_/g, ' ')}</span>
                        </div>
                        <div class="fw-semibold text-truncate mb-1">${t.subject}</div>
                        <div class="d-flex justify-content-between align-items-center small text-muted">
                            <span class="${priorityClass}"><i class="bi bi-flag-fill me-1"></i>${t.priority}</span>
                            <span>${dateStr}</span>
                        </div>
                    </div>
                `;
            }).join('');
        }
    } catch (e) {
        container.innerHTML = `<div class="alert alert-danger">${e.message}</div>`;
    }
}

async function selectTicket(ticketId) {
    activeTicketId = ticketId;
    loadMyTickets(); // re-render left to show active highlight

    const header = document.getElementById('conversationHeader');
    const body = document.getElementById('conversationBody');
    const footer = document.getElementById('replyFooter');

    body.innerHTML = '<div class="text-center py-5"><div class="spinner-border text-success"></div></div>';

    try {
        const res = await ApiClient.get(`/customer/support/tickets/${ticketId}`);
        if (res && res.success && res.data) {
            const ticket = res.data;
            header.innerHTML = `
                <div>
                    <h6 class="fw-bold mb-0">${ticket.ticketNumber}: ${ticket.subject}</h6>
                    <small class="text-muted">Status: <span class="badge bg-secondary">${ticket.status}</span> • Priority: <strong>${ticket.priority}</strong></small>
                </div>
                <div>
                    ${ticket.assignedTo ? `<span class="badge bg-light text-dark border">Agent: ${ticket.assignedTo}</span>` : ''}
                </div>
            `;

            const messages = ticket.messages || [];
            if (messages.length === 0) {
                body.innerHTML = '<div class="text-center py-5 text-muted">No messages in this ticket.</div>';
            } else {
                body.innerHTML = messages.map(m => {
                    const isCustomer = m.senderType === 'CUSTOMER';
                    const bubbleClass = isCustomer ? 'chat-bubble-customer' : 'chat-bubble-admin';
                    const senderLabel = isCustomer ? 'You' : (m.senderName || 'Support Agent');
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
            document.getElementById('replyInput').focus();
        }
    } catch (e) {
        body.innerHTML = `<div class="alert alert-danger">${e.message}</div>`;
    }
}

async function submitReply(event) {
    event.preventDefault();
    if (!activeTicketId) return;

    const input = document.getElementById('replyInput');
    const msg = input.value.trim();
    if (!msg) return;

    const btn = document.getElementById('btnSendReply');
    btn.disabled = true;

    try {
        const res = await ApiClient.post(`/customer/support/tickets/${activeTicketId}/messages`, { message: msg });
        if (res && res.success) {
            input.value = '';
            await selectTicket(activeTicketId);
        } else {
            alert('Failed to send reply: ' + (res.message || 'Unknown error'));
        }
    } catch (e) {
        alert('Reply error: ' + e.message);
    } finally {
        btn.disabled = false;
    }
}

async function submitNewTicket(event) {
    event.preventDefault();
    const subject = document.getElementById('ticketSubject').value.trim();
    const description = document.getElementById('ticketDescription').value.trim();
    const priority = document.getElementById('ticketPriority').value;
    const orderIdVal = document.getElementById('ticketOrderId').value;

    const payload = {
        subject: subject,
        description: description,
        priority: priority
    };
    if (orderIdVal) payload.orderId = Number(orderIdVal);

    const btn = document.getElementById('btnSubmitTicket');
    btn.disabled = true;
    btn.textContent = 'Submitting...';

    try {
        const res = await ApiClient.post('/customer/support/tickets', payload);
        if (res && res.success && res.data) {
            bootstrap.Modal.getInstance(document.getElementById('newTicketModal')).hide();
            document.getElementById('newTicketForm').reset();
            alert('Ticket created successfully! Ticket #' + res.data.ticketNumber);
            await loadMyTickets();
            await selectTicket(res.data.id);
        } else {
            alert('Creation failed: ' + (res.message || 'Unknown error'));
        }
    } catch (e) {
        alert('Creation error: ' + e.message);
    } finally {
        btn.disabled = false;
        btn.textContent = 'Submit Ticket';
    }
}
