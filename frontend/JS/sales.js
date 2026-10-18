document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('saleForm');
    const totalEl = document.getElementById('grandTotal') || document.getElementById('total');

    if (form) {
        form.addEventListener('submit', (e) => {
            e.preventDefault();
            if (typeof toast === 'function') toast('Sale saved successfully.', 'success');
        });
    }

    document.querySelectorAll('input[data-calc], .qty, .price').forEach(el => {
        el.addEventListener('input', () => {
            const row = el.closest('tr');
            if (!row) return;
            const qty = parseFloat(row.querySelector('.qty')?.value || 0);
            const price = parseFloat(row.querySelector('.price')?.value || 0);
            const total = row.querySelector('.total');
            if (total) total.textContent = '₹' + (qty * price).toFixed(2);
        });
    });
});
