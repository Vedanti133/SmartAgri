package com.smartagri.service;

import com.smartagri.dto.*;
import com.smartagri.entity.*;
import com.smartagri.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GoodsReceiptService {

    private static final Logger log = LoggerFactory.getLogger(GoodsReceiptService.class);

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final FertilizerBatchRepository batchRepository;
    private final FertilizerRepository fertilizerRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final BatchLocationStockRepository batchLocationStockRepository;
    private final WarehouseService warehouseService;

    public GoodsReceiptService(GoodsReceiptRepository goodsReceiptRepository,
                               GoodsReceiptItemRepository goodsReceiptItemRepository,
                               PurchaseOrderRepository purchaseOrderRepository,
                               PurchaseOrderItemRepository purchaseOrderItemRepository,
                               FertilizerBatchRepository batchRepository,
                               FertilizerRepository fertilizerRepository,
                               InventoryTransactionRepository transactionRepository,
                               WarehouseRepository warehouseRepository,
                               WarehouseLocationRepository locationRepository,
                               BatchLocationStockRepository batchLocationStockRepository,
                               WarehouseService warehouseService) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.goodsReceiptItemRepository = goodsReceiptItemRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.batchRepository = batchRepository;
        this.fertilizerRepository = fertilizerRepository;
        this.transactionRepository = transactionRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.batchLocationStockRepository = batchLocationStockRepository;
        this.warehouseService = warehouseService;
    }

    private synchronized String generateUniqueGrnNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = 1000 + new Random().nextInt(9000);
        String candidate = "GRN-" + datePart + "-" + rand;
        while (goodsReceiptRepository.existsByGrnNumber(candidate)) {
            rand = 1000 + new Random().nextInt(9000);
            candidate = "GRN-" + datePart + "-" + rand;
        }
        return candidate;
    }

    @Transactional(readOnly = true)
    public List<GoodsReceiptDTO> getAllGoodsReceipts(Long poId, GoodsReceiptStatus status, String search) {
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        List<GoodsReceipt> list = goodsReceiptRepository.searchGoodsReceipts(poId, status, cleanSearch);
        return list.stream().map(GoodsReceiptDTO::fromEntity).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GoodsReceiptDTO getGoodsReceipt(Long id) {
        GoodsReceipt grn = goodsReceiptRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Goods receipt not found with id: " + id));
        return GoodsReceiptDTO.fromEntity(grn);
    }

    @Transactional(readOnly = true)
    public GoodsReceiptDTO getGoodsReceiptById(Long id) {
        return getGoodsReceipt(id);
    }

    @Transactional
    public GoodsReceiptDTO createGoodsReceipt(CreateGoodsReceiptRequest req, String receivedBy) {
        if (req.getPurchaseOrderId() == null) {
            throw new IllegalArgumentException("Purchase order ID is required");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Goods receipt must contain at least one item");
        }

        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(req.getPurchaseOrderId())
                .orElseThrow(() -> new NoSuchElementException("Purchase order not found with id: " + req.getPurchaseOrderId()));

        // PO must be in an acceptable status for receiving
        if (po.getStatus() != PurchaseOrderStatus.APPROVED &&
            po.getStatus() != PurchaseOrderStatus.SENT_TO_SUPPLIER &&
            po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new IllegalStateException("Cannot receive items against PO in status: " + po.getStatus() +
                    ". Must be APPROVED, SENT_TO_SUPPLIER, or PARTIALLY_RECEIVED.");
        }

        // Warehouse & Location Resolution
        Warehouse warehouse = null;
        if (req.getWarehouseId() != null) {
            warehouse = warehouseRepository.findById(req.getWarehouseId())
                    .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + req.getWarehouseId()));
            if (warehouse.getStatus() == WarehouseStatus.BLOCKED || warehouse.getStatus() == WarehouseStatus.INACTIVE) {
                throw new IllegalStateException("Warehouse '" + warehouse.getWarehouseCode() + "' is " + warehouse.getStatus() + " and cannot receive goods.");
            }
        } else {
            warehouse = warehouseService.getDefaultReceivingWarehouse();
        }

        WarehouseLocation location = null;
        if (req.getLocationId() != null) {
            location = locationRepository.findById(req.getLocationId())
                    .orElseThrow(() -> new NoSuchElementException("Location not found with id: " + req.getLocationId()));
            if (warehouse != null && !location.getWarehouse().getId().equals(warehouse.getId())) {
                throw new IllegalArgumentException("Location does not belong to the selected receiving warehouse");
            }
            if (location.getStatus() == LocationStatus.BLOCKED) {
                throw new IllegalStateException("Location '" + location.getLocationCode() + "' is BLOCKED.");
            }
        } else if (warehouse != null) {
            location = locationRepository.findByWarehouseIdAndStatus(warehouse.getId(), LocationStatus.ACTIVE).stream()
                    .findFirst().orElse(null);
        }

        GoodsReceipt grn = new GoodsReceipt();
        grn.setGrnNumber(generateUniqueGrnNumber());
        grn.setPurchaseOrder(po);
        grn.setWarehouse(warehouse);
        grn.setLocation(location);
        grn.setReceivedDate(req.getReceivedDate() != null ? req.getReceivedDate() : LocalDate.now());
        grn.setReceivedBy(receivedBy != null && !receivedBy.isBlank() ? receivedBy.trim() : "admin");
        grn.setStatus(GoodsReceiptStatus.PENDING_INSPECTION);
        grn.setSupplierInvoiceNumber(req.getSupplierInvoiceNumber() != null ? req.getSupplierInvoiceNumber().trim() : null);
        grn.setSupplierDeliveryNumber(req.getSupplierDeliveryNumber() != null ? req.getSupplierDeliveryNumber().trim() : null);
        grn.setPackagingCondition(req.getPackagingCondition());
        grn.setSealCondition(req.getSealCondition());
        grn.setLabelVerification(req.getLabelVerification() != null ? req.getLabelVerification() : true);
        grn.setQuantityVerification(req.getQuantityVerification() != null ? req.getQuantityVerification() : true);
        grn.setInspectionStatus(QualityInspectionStatus.PENDING);
        grn.setNotes(req.getNotes() != null ? req.getNotes().trim() : null);

        for (CreateGoodsReceiptItemRequest itemReq : req.getItems()) {
            if (itemReq.getPurchaseOrderItemId() == null) {
                throw new IllegalArgumentException("Purchase order item ID is required");
            }
            if (itemReq.getReceivedQuantity() == null || itemReq.getReceivedQuantity() <= 0) {
                throw new IllegalArgumentException("Received quantity must be greater than zero");
            }
            if (itemReq.getBatchNumber() == null || itemReq.getBatchNumber().isBlank()) {
                throw new IllegalArgumentException("Batch number is required for receiving lot");
            }
            if (itemReq.getExpiryDate() == null) {
                throw new IllegalArgumentException("Expiry date is required for receiving lot");
            }
            if (itemReq.getManufacturingDate() != null && itemReq.getManufacturingDate().isAfter(itemReq.getExpiryDate())) {
                throw new IllegalArgumentException("Manufacturing date cannot be after expiry date");
            }

            PurchaseOrderItem poi = purchaseOrderItemRepository.findById(itemReq.getPurchaseOrderItemId())
                    .orElseThrow(() -> new NoSuchElementException("Purchase order item not found with id: " + itemReq.getPurchaseOrderItemId()));

            if (!poi.getPurchaseOrder().getId().equals(po.getId())) {
                throw new IllegalArgumentException("PO item #" + poi.getId() + " does not belong to PO #" + po.getId());
            }

            // Over-receiving prevention
            int remainingToReceive = poi.getRemainingQuantity();
            if (itemReq.getReceivedQuantity() > remainingToReceive) {
                throw new IllegalArgumentException(String.format(
                        "Cannot receive %d units for '%s'. Remaining ordered quantity on PO is %d.",
                        itemReq.getReceivedQuantity(), poi.getFertilizer().getName(), remainingToReceive));
            }

            GoodsReceiptItem gri = new GoodsReceiptItem();
            gri.setPurchaseOrderItem(poi);
            gri.setFertilizer(poi.getFertilizer());
            gri.setReceivedQuantity(itemReq.getReceivedQuantity());
            // Default accepted quantity matches received quantity until inspection overrides
            gri.setAcceptedQuantity(itemReq.getReceivedQuantity());
            gri.setRejectedQuantity(0);
            gri.setBatchNumber(itemReq.getBatchNumber().trim());
            gri.setManufacturingDate(itemReq.getManufacturingDate());
            gri.setExpiryDate(itemReq.getExpiryDate());
            gri.setSupplierReference(itemReq.getSupplierReference());
            gri.setUnitCost(itemReq.getUnitCost() != null ? itemReq.getUnitCost() : poi.getUnitCost());
            gri.setNotes(itemReq.getNotes());
            grn.addItem(gri);
        }

        GoodsReceipt saved = goodsReceiptRepository.save(grn);
        log.info("Goods receipt created: #{} [{}] against PO #{}", saved.getId(), saved.getGrnNumber(), po.getPoNumber());
        return GoodsReceiptDTO.fromEntity(saved);
    }

    @Transactional
    public GoodsReceiptDTO inspectGoodsReceipt(Long id, InspectGoodsReceiptRequest req, String inspectedBy) {
        GoodsReceipt grn = goodsReceiptRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Goods receipt not found with id: " + id));

        if (grn.getStatus() == GoodsReceiptStatus.ACCEPTED || grn.getStatus() == GoodsReceiptStatus.CANCELLED) {
            throw new IllegalStateException("Cannot inspect GRN in status: " + grn.getStatus());
        }

        grn.setPackagingCondition(req.getPackagingCondition());
        grn.setSealCondition(req.getSealCondition());
        if (req.getLabelVerification() != null) grn.setLabelVerification(req.getLabelVerification());
        if (req.getQuantityVerification() != null) grn.setQuantityVerification(req.getQuantityVerification());
        grn.setInspectionStatus(req.getInspectionStatus());
        grn.setInspectionNotes(req.getInspectionNotes());
        grn.setInspectedBy(inspectedBy != null && !inspectedBy.isBlank() ? inspectedBy.trim() : "admin");
        grn.setInspectedAt(LocalDateTime.now());

        if (req.getItemInspections() != null && !req.getItemInspections().isEmpty()) {
            Map<Long, InspectGoodsReceiptRequest.ItemInspectionBreakdown> map = req.getItemInspections().stream()
                    .collect(Collectors.toMap(InspectGoodsReceiptRequest.ItemInspectionBreakdown::getItemId, b -> b));

            for (GoodsReceiptItem item : grn.getItems()) {
                InspectGoodsReceiptRequest.ItemInspectionBreakdown b = map.get(item.getId());
                if (b != null) {
                    if (b.getAcceptedQuantity() < 0 || b.getRejectedQuantity() < 0) {
                        throw new IllegalArgumentException("Accepted and rejected quantities cannot be negative");
                    }
                    if (b.getAcceptedQuantity() + b.getRejectedQuantity() != item.getReceivedQuantity()) {
                        throw new IllegalArgumentException(String.format(
                                "Sum of accepted (%d) and rejected (%d) must equal received quantity (%d) for item #%d",
                                b.getAcceptedQuantity(), b.getRejectedQuantity(), item.getReceivedQuantity(), item.getId()));
                    }
                    item.setAcceptedQuantity(b.getAcceptedQuantity());
                    item.setRejectedQuantity(b.getRejectedQuantity());
                    item.setRejectionReason(b.getRejectionReason());
                }
            }
        } else {
            // Default behavior based on inspectionStatus
            if (req.getInspectionStatus() == QualityInspectionStatus.PASSED) {
                for (GoodsReceiptItem item : grn.getItems()) {
                    item.setAcceptedQuantity(item.getReceivedQuantity());
                    item.setRejectedQuantity(0);
                }
            } else if (req.getInspectionStatus() == QualityInspectionStatus.FAILED) {
                for (GoodsReceiptItem item : grn.getItems()) {
                    item.setAcceptedQuantity(0);
                    item.setRejectedQuantity(item.getReceivedQuantity());
                    item.setRejectionReason(req.getInspectionNotes() != null ? req.getInspectionNotes() : "Quality inspection failed");
                }
            }
        }

        // Compute overall GRN status from inspection
        if (req.getInspectionStatus() == QualityInspectionStatus.PASSED) {
            grn.setStatus(GoodsReceiptStatus.INSPECTION_PASSED);
        } else if (req.getInspectionStatus() == QualityInspectionStatus.PARTIAL) {
            grn.setStatus(GoodsReceiptStatus.PARTIALLY_ACCEPTED);
        } else if (req.getInspectionStatus() == QualityInspectionStatus.FAILED) {
            grn.setStatus(GoodsReceiptStatus.REJECTED);
        }

        GoodsReceipt saved = goodsReceiptRepository.save(grn);
        log.info("GRN #{} inspected by {}: Status={}, Quality={}",
                saved.getGrnNumber(), saved.getInspectedBy(), saved.getStatus(), saved.getInspectionStatus());
        return GoodsReceiptDTO.fromEntity(saved);
    }

    @Transactional
    public GoodsReceiptDTO acceptGoodsReceipt(Long id, String performedBy) {
        GoodsReceipt grn = goodsReceiptRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Goods receipt not found with id: " + id));

        // IDEMPOTENCY GUARD: If already accepted, strictly reject with 409 Conflict
        if (grn.getStatus() == GoodsReceiptStatus.ACCEPTED) {
            throw new IllegalStateException("GRN '" + grn.getGrnNumber() + "' has already been accepted and processed.");
        }

        if (grn.getStatus() == GoodsReceiptStatus.CANCELLED || grn.getStatus() == GoodsReceiptStatus.REJECTED) {
            throw new IllegalStateException("Cannot accept GRN in status: " + grn.getStatus());
        }

        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(grn.getPurchaseOrder().getId())
                .orElseThrow(() -> new NoSuchElementException("Associated PO not found: " + grn.getPurchaseOrder().getId()));

        String adminUser = (performedBy != null && !performedBy.isBlank()) ? performedBy.trim() : "admin";

        // Warehouse and Location Resolution
        Warehouse warehouse = grn.getWarehouse();
        if (warehouse == null) {
            warehouse = warehouseService.getDefaultReceivingWarehouse();
            grn.setWarehouse(warehouse);
        }
        if (warehouse != null && (warehouse.getStatus() == WarehouseStatus.BLOCKED || warehouse.getStatus() == WarehouseStatus.INACTIVE)) {
            throw new IllegalStateException("Cannot accept GRN into " + warehouse.getStatus() + " warehouse '" + warehouse.getWarehouseCode() + "'.");
        }

        WarehouseLocation location = grn.getLocation();
        if (location == null && warehouse != null) {
            location = locationRepository.findByWarehouseIdAndStatus(warehouse.getId(), LocationStatus.ACTIVE).stream()
                    .findFirst().orElse(null);
            grn.setLocation(location);
        }
        if (location != null && location.getStatus() == LocationStatus.BLOCKED) {
            throw new IllegalStateException("Cannot accept GRN into BLOCKED location '" + location.getLocationCode() + "'.");
        }

        // Process each line item with accepted quantity > 0
        for (GoodsReceiptItem item : grn.getItems()) {
            int acceptedQty = item.getAcceptedQuantity() != null ? item.getAcceptedQuantity() : 0;
            if (acceptedQty <= 0) {
                continue;
            }

            Fertilizer fert = fertilizerRepository.findByIdWithLock(item.getFertilizer().getId())
                    .orElseThrow(() -> new NoSuchElementException("Fertilizer not found: " + item.getFertilizer().getId()));

            // Expiry protection: If expiry date is in the past, reject acceptance into saleable inventory
            if (item.getExpiryDate().isBefore(LocalDate.now())) {
                throw new IllegalStateException(String.format(
                        "Cannot accept expired lot '%s' (Expiry: %s) into saleable inventory for product '%s'.",
                        item.getBatchNumber(), item.getExpiryDate(), fert.getName()));
            }

            // Check if batch already exists for this product
            Optional<FertilizerBatch> existingBatchOpt = batchRepository.findByFertilizerIdAndBatchNumber(
                    fert.getId(), item.getBatchNumber());

            FertilizerBatch batch;
            if (existingBatchOpt.isPresent()) {
                // Replenish existing batch safely
                batch = existingBatchOpt.get();
                batch.setInitialQuantity(batch.getInitialQuantity() + acceptedQty);
                batch.setCurrentQuantity(batch.getCurrentQuantity() + acceptedQty);
                batch.setStatus(batch.computeDynamicStatus());
                batch = batchRepository.save(batch);
                log.info("Replenished existing batch #{} (+{} units) for product #{}", batch.getBatchNumber(), acceptedQty, fert.getId());
            } else {
                // Create new batch
                batch = new FertilizerBatch();
                batch.setFertilizer(fert);
                batch.setBatchNumber(item.getBatchNumber());
                batch.setManufacturingDate(item.getManufacturingDate());
                batch.setExpiryDate(item.getExpiryDate());
                batch.setInitialQuantity(acceptedQty);
                batch.setCurrentQuantity(acceptedQty);
                batch.setReservedQuantity(0);
                batch.setSupplierName(po.getSupplier() != null ? po.getSupplier().getCompanyName() : null);
                batch.setSupplierReference(item.getSupplierReference() != null ? item.getSupplierReference() : grn.getSupplierInvoiceNumber());
                batch.setPurchaseOrderNumber(po.getPoNumber());
                batch.setNotes("Created via GRN " + grn.getGrnNumber());
                batch.setStatus(batch.computeDynamicStatus());
                batch = batchRepository.save(batch);
                log.info("Created new batch #{} ({} units) from GRN #{}", batch.getBatchNumber(), acceptedQty, grn.getGrnNumber());
            }

            // Phase 13: Create / update BatchLocationStock
            if (warehouse != null && location != null) {
                Optional<BatchLocationStock> opt = batchLocationStockRepository.findByBatchIdAndLocationIdWithLock(
                        batch.getId(), location.getId());
                BatchLocationStock locStock;
                if (opt.isPresent()) {
                    locStock = opt.get();
                } else {
                    locStock = new BatchLocationStock();
                    locStock.setBatch(batch);
                    locStock.setWarehouse(warehouse);
                    locStock.setLocation(location);
                    locStock.setQuantity(0);
                    locStock.setReservedQuantity(0);
                    locStock = batchLocationStockRepository.save(locStock);
                }
                locStock.setQuantity(locStock.getQuantity() + acceptedQty);
                batchLocationStockRepository.save(locStock);
            }

            // Synchronize parent fertilizer stock
            int fertStockBefore = fert.getStock() != null ? fert.getStock() : 0;
            int fertStockAfter = fertStockBefore + acceptedQty;
            fert.setStock(fertStockAfter);
            if (item.getUnitCost() != null) {
                fert.setPurchasePrice(item.getUnitCost());
            }
            if (po.getSupplier() != null) {
                fert.setSupplier(po.getSupplier().getCompanyName());
            }
            fert.syncStatusAndStock();
            fertilizerRepository.save(fert);

            // Create audited RESTOCK inventory transaction
            InventoryTransaction tx = new InventoryTransaction();
            tx.setFertilizer(fert);
            tx.setBatch(batch);
            tx.setWarehouse(warehouse);
            tx.setLocation(location);
            tx.setTransactionType(InventoryTransactionType.RESTOCK);
            tx.setQuantity(acceptedQty);
            tx.setStockBefore(fertStockBefore);
            tx.setStockAfter(fertStockAfter);
            tx.setReferenceType("GRN");
            tx.setReferenceId(grn.getGrnNumber());
            tx.setReason("GRN " + grn.getGrnNumber() + " accepted: Batch " + batch.getBatchNumber() + " from PO " + po.getPoNumber());
            tx.setPerformedBy(adminUser);
            transactionRepository.save(tx);

            // Update PurchaseOrderItem received quantity
            PurchaseOrderItem poi = item.getPurchaseOrderItem();
            if (poi != null) {
                int poiRecBefore = poi.getReceivedQuantity() != null ? poi.getReceivedQuantity() : 0;
                poi.setReceivedQuantity(poiRecBefore + acceptedQty);
                purchaseOrderItemRepository.save(poi);
            }
        }

        // Update overall Purchase Order status based on total received quantities
        boolean allItemsFullyReceived = true;
        boolean anyItemReceived = false;

        for (PurchaseOrderItem poi : po.getItems()) {
            int ordered = poi.getOrderedQuantity();
            int received = poi.getReceivedQuantity() != null ? poi.getReceivedQuantity() : 0;
            if (received > 0) {
                anyItemReceived = true;
            }
            if (received < ordered) {
                allItemsFullyReceived = false;
            }
        }

        if (allItemsFullyReceived) {
            po.setStatus(PurchaseOrderStatus.FULLY_RECEIVED);
        } else if (anyItemReceived) {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }
        purchaseOrderRepository.save(po);

        // Transition GRN to ACCEPTED
        grn.setStatus(GoodsReceiptStatus.ACCEPTED);
        GoodsReceipt saved = goodsReceiptRepository.save(grn);

        log.info("GRN #{} accepted successfully by {}. PO #{} status: {}",
                saved.getGrnNumber(), adminUser, po.getPoNumber(), po.getStatus());
        return GoodsReceiptDTO.fromEntity(saved);
    }

    @Transactional
    public GoodsReceiptDTO rejectGoodsReceipt(Long id, String reason, String rejectedBy) {
        GoodsReceipt grn = goodsReceiptRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Goods receipt not found with id: " + id));

        if (grn.getStatus() == GoodsReceiptStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot reject a GRN that has already been accepted and added to inventory.");
        }

        grn.setStatus(GoodsReceiptStatus.REJECTED);
        grn.setInspectionStatus(QualityInspectionStatus.FAILED);
        grn.setInspectionNotes(reason != null ? reason.trim() : "Rejected by inspector");
        grn.setInspectedBy(rejectedBy != null && !rejectedBy.isBlank() ? rejectedBy.trim() : "admin");
        grn.setInspectedAt(LocalDateTime.now());

        // Zero out accepted quantities
        for (GoodsReceiptItem item : grn.getItems()) {
            item.setAcceptedQuantity(0);
            item.setRejectedQuantity(item.getReceivedQuantity());
            item.setRejectionReason(reason != null ? reason.trim() : "Rejected at receipt");
        }

        GoodsReceipt saved = goodsReceiptRepository.save(grn);
        log.info("GRN #{} rejected by {}: '{}'", saved.getGrnNumber(), rejectedBy, reason);
        return GoodsReceiptDTO.fromEntity(saved);
    }
}
