package com.smartagri.service;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import com.smartagri.dto.InvoiceDTO;
import com.smartagri.dto.OrderItemDTO;
import com.smartagri.entity.Invoice;
import com.smartagri.entity.Order;
import com.smartagri.entity.OrderItem;
import com.smartagri.repository.InvoiceRepository;
import com.smartagri.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DISPLAY_DT_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    private final InvoiceRepository invoiceRepository;
    private final OrderRepository orderRepository;

    @Value("${smartagri.invoice.seller-name:SmartAgri Technologies Pvt. Ltd.}")
    private String sellerName;

    @Value("${smartagri.invoice.seller-gstin:27AABCS1429B1Z8}")
    private String sellerGstin;

    @Value("${smartagri.invoice.seller-address:Plot 104, Agro Tech Hub, Hinjawadi Phase 1, Pune, Maharashtra 411057}")
    private String sellerAddress;

    @Value("${smartagri.invoice.seller-state:Maharashtra}")
    private String sellerState;

    public InvoiceService(InvoiceRepository invoiceRepository, OrderRepository orderRepository) {
        this.invoiceRepository = invoiceRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Invoice getOrCreateInvoiceEntity(Order order) {
        Optional<Invoice> existing = invoiceRepository.findByOrderId(order.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        String invoiceNumber = generateUniqueInvoiceNumber();

        BigDecimal subtotal = order.getSubtotal() != null ? order.getSubtotal() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal taxableAmount = subtotal.subtract(discount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalTax = order.getTaxAmount() != null ? order.getTaxAmount() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShippingAmount() != null ? order.getShippingAmount() : BigDecimal.ZERO;
        BigDecimal grandTotal = order.getGrandTotal() != null ? order.getGrandTotal() : BigDecimal.ZERO;

        String buyerState = order.getState() != null ? order.getState().trim() : "";
        boolean isIntraState = buyerState.equalsIgnoreCase(sellerState.trim());

        BigDecimal cgstAmount;
        BigDecimal sgstAmount;
        BigDecimal igstAmount;

        if (isIntraState) {
            cgstAmount = totalTax.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            sgstAmount = totalTax.subtract(cgstAmount); // Ensure exact sum
            igstAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            cgstAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            sgstAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            igstAmount = totalTax.setScale(2, RoundingMode.HALF_UP);
        }

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setInvoiceDate(LocalDateTime.now());

        invoice.setSellerName(sellerName);
        invoice.setSellerGstin(sellerGstin);
        invoice.setSellerAddress(sellerAddress);
        invoice.setSellerState(sellerState);

        invoice.setBuyerName(order.getRecipientName());
        invoice.setBuyerPhone(order.getRecipientPhone());
        invoice.setBuyerAddress(order.getAddressLine1() + (order.getAddressLine2() != null ? ", " + order.getAddressLine2() : ""));
        invoice.setBuyerCity(order.getCity());
        invoice.setBuyerState(order.getState());
        invoice.setBuyerPincode(order.getPincode());

        invoice.setSubtotal(subtotal);
        invoice.setDiscountAmount(discount);
        invoice.setTaxableAmount(taxableAmount);
        invoice.setCgstAmount(cgstAmount);
        invoice.setSgstAmount(sgstAmount);
        invoice.setIgstAmount(igstAmount);
        invoice.setTotalGstAmount(totalTax);
        invoice.setShippingAmount(shipping);
        invoice.setGrandTotal(grandTotal);

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Created invoice #{} for order #{}", saved.getInvoiceNumber(), order.getOrderNumber());
        return saved;
    }

    @Transactional
    public InvoiceDTO getCustomerInvoice(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this order.");
        }

        Invoice invoice = getOrCreateInvoiceEntity(order);
        return toDTO(invoice, order);
    }

    @Transactional
    public InvoiceDTO getAdminInvoice(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        Invoice invoice = getOrCreateInvoiceEntity(order);
        return toDTO(invoice, order);
    }

    @Transactional
    public byte[] generateInvoicePdf(Long userId, Long orderId, boolean isAdmin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        if (!isAdmin && !order.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this order.");
        }

        Invoice invoice = getOrCreateInvoiceEntity(order);
        return createPdfBytes(invoice, order);
    }

    private byte[] createPdfBytes(Invoice invoice, Order order) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(document, baos);
            document.open();

            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(46, 125, 50));
            Font subHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.DARK_GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.BLACK);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.BLACK);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

            // Brand Header
            Paragraph title = new Paragraph("SmartAgri — Tax Invoice", headerFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            Paragraph subtitle = new Paragraph("Fertilizer Sales, Inventory & Fulfillment Management", smallFont);
            subtitle.setAlignment(Element.ALIGN_CENTER);
            subtitle.setSpacingAfter(15f);
            document.add(subtitle);

            // Metadata Bar Table
            PdfPTable metaTable = new PdfPTable(2);
            metaTable.setWidthPercentage(100);
            metaTable.setWidths(new float[]{1, 1});

            PdfPCell metaLeft = new PdfPCell();
            metaLeft.setBorder(Rectangle.NO_BORDER);
            metaLeft.addElement(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber(), boldFont));
            metaLeft.addElement(new Paragraph("Order Number: " + order.getOrderNumber(), boldFont));
            metaLeft.addElement(new Paragraph("Invoice Date: " + invoice.getInvoiceDate().format(DISPLAY_DT_FMT), regularFont));
            metaTable.addCell(metaLeft);

            PdfPCell metaRight = new PdfPCell();
            metaRight.setBorder(Rectangle.NO_BORDER);
            metaRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            metaRight.addElement(new Paragraph("Payment Method: " + order.getPaymentMethod(), regularFont));
            metaRight.addElement(new Paragraph("Payment Status: " + order.getPaymentStatus(), boldFont));
            metaRight.addElement(new Paragraph("Order Status: " + order.getOrderStatus(), regularFont));
            metaTable.addCell(metaRight);

            metaTable.setSpacingAfter(12f);
            document.add(metaTable);

            // Seller and Buyer Details Box
            PdfPTable partiesTable = new PdfPTable(2);
            partiesTable.setWidthPercentage(100);
            partiesTable.setWidths(new float[]{1, 1});

            PdfPCell sellerCell = new PdfPCell();
            sellerCell.setBackgroundColor(new Color(245, 245, 245));
            sellerCell.setPadding(8f);
            sellerCell.addElement(new Paragraph("SELLER DETAILS:", subHeaderFont));
            sellerCell.addElement(new Paragraph(invoice.getSellerName(), boldFont));
            sellerCell.addElement(new Paragraph("GSTIN: " + invoice.getSellerGstin(), boldFont));
            sellerCell.addElement(new Paragraph(invoice.getSellerAddress(), regularFont));
            sellerCell.addElement(new Paragraph("State: " + invoice.getSellerState(), regularFont));
            partiesTable.addCell(sellerCell);

            PdfPCell buyerCell = new PdfPCell();
            buyerCell.setBackgroundColor(new Color(245, 245, 245));
            buyerCell.setPadding(8f);
            buyerCell.addElement(new Paragraph("BUYER DETAILS / BILL TO:", subHeaderFont));
            buyerCell.addElement(new Paragraph(invoice.getBuyerName(), boldFont));
            buyerCell.addElement(new Paragraph("Phone: +91 " + invoice.getBuyerPhone(), regularFont));
            buyerCell.addElement(new Paragraph(invoice.getBuyerAddress(), regularFont));
            buyerCell.addElement(new Paragraph(invoice.getBuyerCity() + ", " + invoice.getBuyerState() + " - " + invoice.getBuyerPincode(), regularFont));
            partiesTable.addCell(buyerCell);

            partiesTable.setSpacingAfter(15f);
            document.add(partiesTable);

            // Items Table
            PdfPTable itemsTable = new PdfPTable(7);
            itemsTable.setWidthPercentage(100);
            itemsTable.setWidths(new float[]{0.6f, 2.2f, 0.7f, 1.0f, 1.0f, 1.0f, 1.1f});

            String[] headers = {"#", "Product / SKU", "Qty", "Unit Price", "Discount", "Tax (5%)", "Total"};
            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, boldFont));
                cell.setBackgroundColor(new Color(232, 245, 233));
                cell.setPadding(6f);
                itemsTable.addCell(cell);
            }

            int idx = 1;
            for (OrderItem it : order.getItems()) {
                itemsTable.addCell(new Phrase(String.valueOf(idx++), regularFont));
                itemsTable.addCell(new Phrase(it.getProductName() + "\n(SKU: " + it.getSku() + ")", regularFont));
                itemsTable.addCell(new Phrase(String.valueOf(it.getQuantity()), regularFont));
                itemsTable.addCell(new Phrase("₹" + it.getUnitPrice().toPlainString(), regularFont));
                itemsTable.addCell(new Phrase("₹" + it.getDiscountAmount().toPlainString(), regularFont));
                itemsTable.addCell(new Phrase("₹" + it.getTaxAmount().toPlainString(), regularFont));
                itemsTable.addCell(new Phrase("₹" + it.getLineTotal().toPlainString(), boldFont));
            }

            itemsTable.setSpacingAfter(15f);
            document.add(itemsTable);

            // Totals Table
            PdfPTable totalsTable = new PdfPTable(2);
            totalsTable.setWidthPercentage(50);
            totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalsTable.setWidths(new float[]{1.2f, 1.0f});

            addTotalRow(totalsTable, "Gross Subtotal:", "₹" + invoice.getSubtotal().toPlainString(), regularFont);
            addTotalRow(totalsTable, "Discount:", "-₹" + invoice.getDiscountAmount().toPlainString(), regularFont);
            addTotalRow(totalsTable, "Taxable Value:", "₹" + invoice.getTaxableAmount().toPlainString(), regularFont);

            if (invoice.getCgstAmount().compareTo(BigDecimal.ZERO) > 0) {
                addTotalRow(totalsTable, "CGST (2.5%):", "₹" + invoice.getCgstAmount().toPlainString(), regularFont);
                addTotalRow(totalsTable, "SGST (2.5%):", "₹" + invoice.getSgstAmount().toPlainString(), regularFont);
            } else {
                addTotalRow(totalsTable, "IGST (5.0%):", "₹" + invoice.getIgstAmount().toPlainString(), regularFont);
            }

            addTotalRow(totalsTable, "Shipping Charges:", "₹" + invoice.getShippingAmount().toPlainString(), regularFont);
            addTotalRow(totalsTable, "Grand Total:", "₹" + invoice.getGrandTotal().toPlainString(), boldFont);

            totalsTable.setSpacingAfter(25f);
            document.add(totalsTable);

            // Footer note
            Paragraph footer = new Paragraph("This is a computer-generated GST invoice for SmartAgri. No signature required.", smallFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate invoice PDF for order #{}", order.getOrderNumber(), e);
            throw new RuntimeException("Error generating invoice PDF: " + e.getMessage(), e);
        }
    }

    private void addTotalRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell c1 = new PdfPCell(new Phrase(label, font));
        c1.setBorder(Rectangle.NO_BORDER);
        c1.setPadding(3f);
        table.addCell(c1);

        PdfPCell c2 = new PdfPCell(new Phrase(value, font));
        c2.setBorder(Rectangle.NO_BORDER);
        c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c2.setPadding(3f);
        table.addCell(c2);
    }

    public String generateUniqueInvoiceNumber() {
        String datePart = LocalDate.now().format(DATE_FMT);
        String candidate;
        int attempts = 0;
        do {
            int randomSeq = ThreadLocalRandom.current().nextInt(1000, 9999);
            candidate = String.format(Locale.ROOT, "INV-SA-%s-%04d", datePart, randomSeq);
            attempts++;
            if (attempts > 50) {
                candidate = String.format(Locale.ROOT, "INV-SA-%s-%04d", datePart, System.currentTimeMillis() % 10000);
                break;
            }
        } while (invoiceRepository.findByInvoiceNumber(candidate).isPresent());
        return candidate;
    }

    public InvoiceDTO toDTO(Invoice inv, Order o) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setId(inv.getId());
        dto.setOrderId(o.getId());
        dto.setOrderNumber(o.getOrderNumber());
        dto.setInvoiceNumber(inv.getInvoiceNumber());
        dto.setInvoiceDate(inv.getInvoiceDate());

        dto.setSellerName(inv.getSellerName());
        dto.setSellerGstin(inv.getSellerGstin());
        dto.setSellerAddress(inv.getSellerAddress());
        dto.setSellerState(inv.getSellerState());

        dto.setBuyerName(inv.getBuyerName());
        dto.setBuyerPhone(inv.getBuyerPhone());
        dto.setBuyerAddress(inv.getBuyerAddress());
        dto.setBuyerCity(inv.getBuyerCity());
        dto.setBuyerState(inv.getBuyerState());
        dto.setBuyerPincode(inv.getBuyerPincode());

        dto.setSubtotal(inv.getSubtotal());
        dto.setDiscountAmount(inv.getDiscountAmount());
        dto.setTaxableAmount(inv.getTaxableAmount());
        dto.setCgstAmount(inv.getCgstAmount());
        dto.setSgstAmount(inv.getSgstAmount());
        dto.setIgstAmount(inv.getIgstAmount());
        dto.setTotalGstAmount(inv.getTotalGstAmount());
        dto.setShippingAmount(inv.getShippingAmount());
        dto.setGrandTotal(inv.getGrandTotal());

        dto.setPaymentMethod(o.getPaymentMethod());
        dto.setPaymentStatus(o.getPaymentStatus().name());

        List<OrderItemDTO> items = new ArrayList<>();
        if (o.getItems() != null) {
            for (OrderItem it : o.getItems()) {
                OrderItemDTO itemDTO = new OrderItemDTO();
                itemDTO.setId(it.getId());
                itemDTO.setFertilizerId(it.getFertilizer() != null ? it.getFertilizer().getId() : null);
                itemDTO.setProductName(it.getProductName());
                itemDTO.setSku(it.getSku());
                itemDTO.setBrand(it.getBrand());
                itemDTO.setImageUrl(it.getImageUrl());
                itemDTO.setCategoryName(it.getCategoryName());
                itemDTO.setUnit(it.getUnit());
                itemDTO.setWeightValue(it.getWeightValue());
                itemDTO.setQuantity(it.getQuantity());
                itemDTO.setUnitPrice(it.getUnitPrice());
                itemDTO.setDiscountPercent(it.getDiscountPercent());
                itemDTO.setDiscountAmount(it.getDiscountAmount());
                itemDTO.setTaxPercent(it.getTaxPercent());
                itemDTO.setTaxAmount(it.getTaxAmount());
                itemDTO.setLineTotal(it.getLineTotal());
                items.add(itemDTO);
            }
        }
        dto.setItems(items);

        return dto;
    }
}
