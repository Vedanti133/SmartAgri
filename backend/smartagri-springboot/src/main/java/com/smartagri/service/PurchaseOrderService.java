package com.smartagri.service;

import com.smartagri.dto.*;
import com.smartagri.entity.*;
import com.smartagri.repository.FertilizerRepository;
import com.smartagri.repository.PurchaseOrderRepository;
import com.smartagri.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {

    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderService.class);

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;
    private final FertilizerRepository fertilizerRepository;

    public PurchaseOrderService(PurchaseOrderRepository purchaseOrderRepository,
                                SupplierRepository supplierRepository,
                                FertilizerRepository fertilizerRepository) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.supplierRepository = supplierRepository;
        this.fertilizerRepository = fertilizerRepository;
    }

    private synchronized String generateUniquePoNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = 1000 + new Random().nextInt(9000);
        String candidate = "PO-" + datePart + "-" + rand;
        while (purchaseOrderRepository.existsByPoNumber(candidate)) {
            rand = 1000 + new Random().nextInt(9000);
            candidate = "PO-" + datePart + "-" + rand;
        }
        return candidate;
    }

    @Transactional(readOnly = true)
    public List<PurchaseOrderDTO> getAllPurchaseOrders(Long supplierId, PurchaseOrderStatus status, String search) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        List<PurchaseOrder> list = purchaseOrderRepository.searchPurchaseOrders(supplierId, status, cleanSearch);
        return list.stream().map(PurchaseOrderDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDTO getPurchaseOrderById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with id: " + id));
        return PurchaseOrderDTO.fromEntity(po);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderDTO getPurchaseOrderByNumber(String poNumber) {
        PurchaseOrder po = purchaseOrderRepository.findByPoNumber(poNumber)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with number: " + poNumber));
        return PurchaseOrderDTO.fromEntity(po);
    }

    @Transactional
    public PurchaseOrderDTO createPurchaseOrder(CreatePurchaseOrderRequest req, String createdBy) {
        if (req.getSupplierId() == null) {
            throw new IllegalArgumentException("Supplier ID is required");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Purchase order must contain at least one item");
        }

        Supplier supplier = supplierRepository.findById(req.getSupplierId())
                .orElseThrow(() -> new NoSuchElementException("Supplier not found with id: " + req.getSupplierId()));

        if (supplier.getStatus() == SupplierStatus.BLOCKED) {
            throw new IllegalStateException("Cannot create purchase order for a BLOCKED supplier");
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(generateUniquePoNumber());
        po.setSupplier(supplier);
        po.setOrderDate(req.getOrderDate() != null ? req.getOrderDate() : LocalDate.now());
        po.setExpectedDeliveryDate(req.getExpectedDeliveryDate());
        po.setStatus(PurchaseOrderStatus.DRAFT);
        po.setNotes(req.getNotes() != null ? req.getNotes().trim() : null);
        po.setCreatedBy(createdBy != null && !createdBy.isBlank() ? createdBy.trim() : "admin");

        for (CreatePurchaseOrderItemRequest itemReq : req.getItems()) {
            if (itemReq.getFertilizerId() == null) {
                throw new IllegalArgumentException("Fertilizer ID is required for each line item");
            }
            if (itemReq.getOrderedQuantity() == null || itemReq.getOrderedQuantity() <= 0) {
                throw new IllegalArgumentException("Ordered quantity must be greater than zero");
            }
            if (itemReq.getUnitCost() == null || itemReq.getUnitCost().signum() < 0) {
                throw new IllegalArgumentException("Unit cost cannot be negative");
            }

            Fertilizer fert = fertilizerRepository.findById(itemReq.getFertilizerId())
                    .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with id: " + itemReq.getFertilizerId()));

            PurchaseOrderItem item = new PurchaseOrderItem();
            item.setFertilizer(fert);
            item.setOrderedQuantity(itemReq.getOrderedQuantity());
            item.setReceivedQuantity(0);
            item.setUnitCost(itemReq.getUnitCost());
            if (itemReq.getTaxRate() != null) {
                item.setTaxRate(itemReq.getTaxRate());
            }
            item.setNotes(itemReq.getNotes());
            item.calculateTotals();
            po.addItem(item);
        }

        po.recalculateTotals();
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        log.info("Purchase order created: #{} [{}] for supplier '{}'", saved.getId(), saved.getPoNumber(), supplier.getCompanyName());
        return PurchaseOrderDTO.fromEntity(saved);
    }

    @Transactional
    public PurchaseOrderDTO updatePurchaseOrder(Long id, UpdatePurchaseOrderRequest req) {
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with id: " + id));

        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Only DRAFT purchase orders can be edited. Current status: " + po.getStatus());
        }

        if (req.getSupplierId() != null) {
            Supplier s = supplierRepository.findById(req.getSupplierId())
                    .orElseThrow(() -> new NoSuchElementException("Supplier not found with id: " + req.getSupplierId()));
            po.setSupplier(s);
        }
        if (req.getOrderDate() != null) po.setOrderDate(req.getOrderDate());
        if (req.getExpectedDeliveryDate() != null) po.setExpectedDeliveryDate(req.getExpectedDeliveryDate());
        if (req.getNotes() != null) po.setNotes(req.getNotes().trim());

        if (req.getItems() != null && !req.getItems().isEmpty()) {
            po.getItems().clear();
            for (CreatePurchaseOrderItemRequest itemReq : req.getItems()) {
                Fertilizer fert = fertilizerRepository.findById(itemReq.getFertilizerId())
                        .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with id: " + itemReq.getFertilizerId()));

                PurchaseOrderItem item = new PurchaseOrderItem();
                item.setFertilizer(fert);
                item.setOrderedQuantity(itemReq.getOrderedQuantity());
                item.setReceivedQuantity(0);
                item.setUnitCost(itemReq.getUnitCost());
                if (itemReq.getTaxRate() != null) item.setTaxRate(itemReq.getTaxRate());
                item.setNotes(itemReq.getNotes());
                item.calculateTotals();
                po.addItem(item);
            }
        }

        po.recalculateTotals();
        PurchaseOrder saved = purchaseOrderRepository.save(po);
        return PurchaseOrderDTO.fromEntity(saved);
    }

    @Transactional
    public PurchaseOrderDTO submitForApproval(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with id: " + id));

        if (po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot submit PO for approval from status: " + po.getStatus() + ". Must be DRAFT.");
        }

        po.setStatus(PurchaseOrderStatus.PENDING_APPROVAL);
        log.info("PO #{} submitted for approval", po.getPoNumber());
        return PurchaseOrderDTO.fromEntity(purchaseOrderRepository.save(po));
    }

    @Transactional
    public PurchaseOrderDTO approvePurchaseOrder(Long id, String approvedBy) {
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with id: " + id));

        if (po.getStatus() != PurchaseOrderStatus.PENDING_APPROVAL && po.getStatus() != PurchaseOrderStatus.DRAFT) {
            throw new IllegalStateException("Cannot approve PO from status: " + po.getStatus() + ". Expected DRAFT or PENDING_APPROVAL.");
        }

        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setApprovedBy(approvedBy != null && !approvedBy.isBlank() ? approvedBy.trim() : "admin");
        po.setApprovedAt(LocalDateTime.now());
        log.info("PO #{} approved by {}", po.getPoNumber(), po.getApprovedBy());
        return PurchaseOrderDTO.fromEntity(purchaseOrderRepository.save(po));
    }

    @Transactional
    public PurchaseOrderDTO sendToSupplier(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with id: " + id));

        if (po.getStatus() != PurchaseOrderStatus.APPROVED) {
            throw new IllegalStateException("Cannot send PO to supplier from status: " + po.getStatus() + ". Must be APPROVED.");
        }

        po.setStatus(PurchaseOrderStatus.SENT_TO_SUPPLIER);
        log.info("PO #{} sent to supplier '{}'", po.getPoNumber(), po.getSupplier().getCompanyName());
        return PurchaseOrderDTO.fromEntity(purchaseOrderRepository.save(po));
    }

    @Transactional
    public PurchaseOrderDTO cancelPurchaseOrder(Long id, String reason) {
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with id: " + id));

        if (po.getStatus() == PurchaseOrderStatus.FULLY_RECEIVED || po.getStatus() == PurchaseOrderStatus.CLOSED || po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot cancel PO in status: " + po.getStatus());
        }

        po.setStatus(PurchaseOrderStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            String existingNotes = po.getNotes() != null ? po.getNotes() + " | " : "";
            po.setNotes(existingNotes + "Cancellation Reason: " + reason.trim());
        }
        log.info("PO #{} cancelled. Reason: {}", po.getPoNumber(), reason);
        return PurchaseOrderDTO.fromEntity(purchaseOrderRepository.save(po));
    }

    @Transactional
    public PurchaseOrderDTO closePurchaseOrder(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with id: " + id));

        if (po.getStatus() != PurchaseOrderStatus.FULLY_RECEIVED && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalStateException("Only received purchase orders can be closed. Current: " + po.getStatus());
        }

        po.setStatus(PurchaseOrderStatus.CLOSED);
        log.info("PO #{} closed", po.getPoNumber());
        return PurchaseOrderDTO.fromEntity(purchaseOrderRepository.save(po));
    }
}
