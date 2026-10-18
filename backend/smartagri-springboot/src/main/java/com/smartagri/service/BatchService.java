package com.smartagri.service;

import com.smartagri.dto.*;
import com.smartagri.entity.*;
import com.smartagri.repository.FertilizerBatchRepository;
import com.smartagri.repository.FertilizerRepository;
import com.smartagri.repository.InventoryTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BatchService {

    private static final Logger log = LoggerFactory.getLogger(BatchService.class);

    private final FertilizerBatchRepository batchRepository;
    private final FertilizerRepository fertilizerRepository;
    private final InventoryTransactionRepository transactionRepository;

    public BatchService(FertilizerBatchRepository batchRepository,
                        FertilizerRepository fertilizerRepository,
                        InventoryTransactionRepository transactionRepository) {
        this.batchRepository = batchRepository;
        this.fertilizerRepository = fertilizerRepository;
        this.transactionRepository = transactionRepository;
    }

    // ------------------------------------------------------------------------
    // Batch Creation & Intake
    // ------------------------------------------------------------------------

    @Transactional
    public BatchDTO createBatch(CreateBatchRequest req, String performedBy) {
        if (req.getFertilizerId() == null) {
            throw new IllegalArgumentException("Fertilizer ID is required");
        }
        if (req.getBatchNumber() == null || req.getBatchNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Batch number is required");
        }
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new IllegalArgumentException("Batch quantity must be greater than zero");
        }
        if (req.getExpiryDate() == null) {
            throw new IllegalArgumentException("Expiry date is required");
        }
        if (req.getManufacturingDate() != null && req.getManufacturingDate().isAfter(req.getExpiryDate())) {
            throw new IllegalArgumentException("Manufacturing date cannot be after expiry date");
        }

        Fertilizer fert = fertilizerRepository.findByIdWithLock(req.getFertilizerId())
                .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with id: " + req.getFertilizerId()));

        String cleanBatchNumber = req.getBatchNumber().trim();
        if (batchRepository.existsByFertilizerIdAndBatchNumber(fert.getId(), cleanBatchNumber)) {
            throw new IllegalStateException("Batch number '" + cleanBatchNumber + "' already exists for product '" + fert.getName() + "'.");
        }

        FertilizerBatch batch = new FertilizerBatch();
        batch.setFertilizer(fert);
        batch.setBatchNumber(cleanBatchNumber);
        batch.setManufacturingDate(req.getManufacturingDate());
        batch.setExpiryDate(req.getExpiryDate());
        batch.setInitialQuantity(req.getQuantity());
        batch.setCurrentQuantity(req.getQuantity());
        batch.setReservedQuantity(0);
        batch.setSupplierName(req.getSupplierName() != null ? req.getSupplierName().trim() : null);
        batch.setSupplierReference(req.getSupplierReference() != null ? req.getSupplierReference().trim() : null);
        batch.setPurchaseOrderNumber(req.getPurchaseOrderNumber() != null ? req.getPurchaseOrderNumber().trim() : null);
        batch.setNotes(req.getNotes() != null ? req.getNotes().trim() : null);

        // Initial status computation
        batch.setStatus(batch.computeDynamicStatus());
        FertilizerBatch savedBatch = batchRepository.save(batch);

        // Synchronize fertilizer stock
        int fertStockBefore = fert.getStock() != null ? fert.getStock() : 0;
        int fertStockAfter = fertStockBefore + req.getQuantity();
        fert.setStock(fertStockAfter);
        if (req.getUnitCost() != null) {
            fert.setPurchasePrice(req.getUnitCost());
        }
        if (req.getSupplierName() != null && !req.getSupplierName().isBlank()) {
            fert.setSupplier(req.getSupplierName().trim());
        }
        fert.syncStatusAndStock();
        fertilizerRepository.save(fert);

        // Record audit transaction
        InventoryTransaction tx = new InventoryTransaction();
        tx.setFertilizer(fert);
        tx.setBatch(savedBatch);
        tx.setTransactionType(InventoryTransactionType.RESTOCK);
        tx.setQuantity(req.getQuantity());
        tx.setStockBefore(fertStockBefore);
        tx.setStockAfter(fertStockAfter);
        tx.setReferenceType("BATCH");
        tx.setReferenceId(req.getPurchaseOrderNumber() != null && !req.getPurchaseOrderNumber().isBlank()
                ? req.getPurchaseOrderNumber().trim()
                : "BATCH-" + savedBatch.getBatchNumber());
        tx.setReason("New batch intake: " + savedBatch.getBatchNumber() + (req.getNotes() != null ? " - " + req.getNotes().trim() : ""));
        tx.setPerformedBy(performedBy != null && !performedBy.isBlank() ? performedBy.trim() : "admin");
        transactionRepository.save(tx);

        log.info("Batch #{} created for product #{}: qty {}, expires {}, performed by {}",
                savedBatch.getBatchNumber(), fert.getId(), req.getQuantity(), savedBatch.getExpiryDate(), performedBy);

        return BatchDTO.fromEntity(savedBatch);
    }

    // ------------------------------------------------------------------------
    // Batch Updates & Lifecycle Actions
    // ------------------------------------------------------------------------

    @Transactional
    public BatchDTO updateBatch(Long id, UpdateBatchRequest req, String performedBy) {
        FertilizerBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Batch not found with id: " + id));

        LocalDate mfg = req.getManufacturingDate() != null ? req.getManufacturingDate() : batch.getManufacturingDate();
        LocalDate exp = req.getExpiryDate() != null ? req.getExpiryDate() : batch.getExpiryDate();

        if (mfg != null && exp != null && mfg.isAfter(exp)) {
            throw new IllegalArgumentException("Manufacturing date cannot be after expiry date");
        }

        if (req.getManufacturingDate() != null) batch.setManufacturingDate(req.getManufacturingDate());
        if (req.getExpiryDate() != null) {
            batch.setExpiryDate(req.getExpiryDate());
            if (batch.getStatus() != BatchStatus.BLOCKED) {
                batch.setStatus(batch.computeDynamicStatus());
            }
        }
        if (req.getSupplierName() != null) batch.setSupplierName(req.getSupplierName().trim());
        if (req.getSupplierReference() != null) batch.setSupplierReference(req.getSupplierReference().trim());
        if (req.getPurchaseOrderNumber() != null) batch.setPurchaseOrderNumber(req.getPurchaseOrderNumber().trim());
        if (req.getNotes() != null) batch.setNotes(req.getNotes().trim());

        FertilizerBatch saved = batchRepository.save(batch);
        log.info("Batch #{} updated by {}", saved.getId(), performedBy);
        return BatchDTO.fromEntity(saved);
    }

    @Transactional
    public BatchDTO blockBatch(Long id, String reason, String performedBy) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason is strictly required for blocking a batch");
        }

        FertilizerBatch batch = batchRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Batch not found with id: " + id));

        batch.setStatus(BatchStatus.BLOCKED);
        FertilizerBatch saved = batchRepository.save(batch);

        // Record audit transaction
        InventoryTransaction tx = new InventoryTransaction();
        tx.setFertilizer(saved.getFertilizer());
        tx.setBatch(saved);
        tx.setTransactionType(InventoryTransactionType.ADJUSTMENT_OUT);
        tx.setQuantity(0);
        tx.setStockBefore(saved.getFertilizer().getStock());
        tx.setStockAfter(saved.getFertilizer().getStock());
        tx.setReferenceType("BATCH_BLOCK");
        tx.setReferenceId(saved.getBatchNumber());
        tx.setReason("Batch blocked from sale: " + reason.trim());
        tx.setPerformedBy(performedBy != null && !performedBy.isBlank() ? performedBy.trim() : "admin");
        transactionRepository.save(tx);

        log.info("Batch #{} blocked by {}. Reason: {}", saved.getBatchNumber(), performedBy, reason);
        return BatchDTO.fromEntity(saved);
    }

    @Transactional
    public BatchDTO unblockBatch(Long id, String performedBy) {
        FertilizerBatch batch = batchRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Batch not found with id: " + id));

        if (batch.isExpired()) {
            throw new IllegalArgumentException("Cannot unblock an expired batch");
        }

        batch.setStatus(batch.computeDynamicStatus());
        if (batch.getStatus() == BatchStatus.BLOCKED) {
            batch.setStatus(BatchStatus.ACTIVE);
        }
        FertilizerBatch saved = batchRepository.save(batch);

        // Record audit transaction
        InventoryTransaction tx = new InventoryTransaction();
        tx.setFertilizer(saved.getFertilizer());
        tx.setBatch(saved);
        tx.setTransactionType(InventoryTransactionType.ADJUSTMENT_IN);
        tx.setQuantity(0);
        tx.setStockBefore(saved.getFertilizer().getStock());
        tx.setStockAfter(saved.getFertilizer().getStock());
        tx.setReferenceType("BATCH_UNBLOCK");
        tx.setReferenceId(saved.getBatchNumber());
        tx.setReason("Batch unblocked and restored to sale");
        tx.setPerformedBy(performedBy != null && !performedBy.isBlank() ? performedBy.trim() : "admin");
        transactionRepository.save(tx);

        log.info("Batch #{} unblocked by {}", saved.getBatchNumber(), performedBy);
        return BatchDTO.fromEntity(saved);
    }

    @Transactional
    public BatchDTO adjustBatch(Long id, BatchAdjustmentRequest req, String performedBy) {
        if (req.getReason() == null || req.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Audit reason is strictly required for batch adjustments");
        }
        if (req.getAdjustmentType() == null || req.getAdjustmentType().trim().isEmpty()) {
            throw new IllegalArgumentException("Adjustment type is required (ADD, REMOVE, SET)");
        }

        FertilizerBatch batch = batchRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Batch not found with id: " + id));
        Fertilizer fert = fertilizerRepository.findByIdWithLock(batch.getFertilizer().getId())
                .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with id: " + batch.getFertilizer().getId()));

        String typeStr = req.getAdjustmentType().trim().toUpperCase(Locale.ROOT);
        int qty = req.getQuantity() != null ? req.getQuantity() : 0;
        int batchBefore = batch.getCurrentQuantity() != null ? batch.getCurrentQuantity() : 0;
        int batchAfter;
        int delta;
        InventoryTransactionType txType;

        if ("ADD".equals(typeStr) || "ADJUSTMENT_IN".equals(typeStr)) {
            if (qty <= 0) {
                throw new IllegalArgumentException("Adjustment quantity must be at least 1");
            }
            batchAfter = batchBefore + qty;
            delta = qty;
            txType = InventoryTransactionType.ADJUSTMENT_IN;
        } else if ("REMOVE".equals(typeStr) || "ADJUSTMENT_OUT".equals(typeStr)) {
            if (qty <= 0) {
                throw new IllegalArgumentException("Adjustment quantity must be at least 1");
            }
            if (batch.getAvailableQuantity() < qty) {
                throw new IllegalArgumentException(String.format(
                        Locale.ROOT, "Insufficient available batch stock for reduction. Available: %d, Requested reduction: %d.",
                        batch.getAvailableQuantity(), qty));
            }
            batchAfter = batchBefore - qty;
            delta = -qty;
            txType = InventoryTransactionType.ADJUSTMENT_OUT;
        } else if ("SET".equals(typeStr)) {
            if (qty < 0) {
                throw new IllegalArgumentException("Target batch stock level cannot be negative: " + qty);
            }
            batchAfter = qty;
            delta = batchAfter - batchBefore;
            if (delta < 0 && batch.getAvailableQuantity() < Math.abs(delta)) {
                throw new IllegalArgumentException("Cannot reduce batch below reserved quantity");
            }
            txType = delta >= 0 ? InventoryTransactionType.ADJUSTMENT_IN : InventoryTransactionType.ADJUSTMENT_OUT;
        } else {
            throw new IllegalArgumentException("Invalid adjustment type: " + typeStr + ". Expected ADD, REMOVE, or SET.");
        }

        batch.setCurrentQuantity(batchAfter);
        batch.setStatus(batch.computeDynamicStatus());
        FertilizerBatch savedBatch = batchRepository.save(batch);

        // Synchronize parent fertilizer stock
        int fertBefore = fert.getStock() != null ? fert.getStock() : 0;
        int fertAfter = Math.max(0, fertBefore + delta);
        fert.setStock(fertAfter);
        fert.syncStatusAndStock();
        fertilizerRepository.save(fert);

        // Record audit transaction
        InventoryTransaction tx = new InventoryTransaction();
        tx.setFertilizer(fert);
        tx.setBatch(savedBatch);
        tx.setTransactionType(txType);
        tx.setQuantity(Math.abs(delta));
        tx.setStockBefore(fertBefore);
        tx.setStockAfter(fertAfter);
        tx.setReferenceType("BATCH_ADJUSTMENT");
        tx.setReferenceId(savedBatch.getBatchNumber());
        tx.setReason("Batch #" + savedBatch.getBatchNumber() + " adjustment: " + req.getReason().trim());
        tx.setPerformedBy(performedBy != null && !performedBy.isBlank() ? performedBy.trim() : "admin");
        transactionRepository.save(tx);

        log.info("Batch #{} adjusted by {} ({} -> {}), reason: '{}'",
                savedBatch.getBatchNumber(), txType, batchBefore, batchAfter, req.getReason());

        return BatchDTO.fromEntity(savedBatch);
    }

    @Transactional
    public BatchDTO markDamaged(Long id, BatchDamageRequest req, String performedBy) {
        if (req.getReason() == null || req.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Audit reason is strictly required for reporting damaged stock");
        }
        if (req.getQuantity() == null || req.getQuantity() <= 0) {
            throw new IllegalArgumentException("Damaged quantity must be greater than zero");
        }

        FertilizerBatch batch = batchRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Batch not found with id: " + id));
        Fertilizer fert = fertilizerRepository.findByIdWithLock(batch.getFertilizer().getId())
                .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with id: " + batch.getFertilizer().getId()));

        if (batch.getAvailableQuantity() < req.getQuantity()) {
            throw new IllegalArgumentException(String.format(
                    Locale.ROOT, "Requested damaged quantity (%d) exceeds available batch stock (%d)",
                    req.getQuantity(), batch.getAvailableQuantity()));
        }

        int batchBefore = batch.getCurrentQuantity() != null ? batch.getCurrentQuantity() : 0;
        int batchAfter = batchBefore - req.getQuantity();
        batch.setCurrentQuantity(batchAfter);
        batch.setStatus(batch.computeDynamicStatus());
        FertilizerBatch savedBatch = batchRepository.save(batch);

        // Synchronize parent fertilizer stock
        int fertBefore = fert.getStock() != null ? fert.getStock() : 0;
        int fertAfter = Math.max(0, fertBefore - req.getQuantity());
        fert.setStock(fertAfter);
        fert.syncStatusAndStock();
        fertilizerRepository.save(fert);

        // Record audit transaction
        InventoryTransaction tx = new InventoryTransaction();
        tx.setFertilizer(fert);
        tx.setBatch(savedBatch);
        tx.setTransactionType(InventoryTransactionType.DAMAGED);
        tx.setQuantity(req.getQuantity());
        tx.setStockBefore(fertBefore);
        tx.setStockAfter(fertAfter);
        tx.setReferenceType("DAMAGED");
        tx.setReferenceId(savedBatch.getBatchNumber());
        tx.setReason("Damaged batch write-off: " + req.getReason().trim() + (req.getNotes() != null ? " - " + req.getNotes().trim() : ""));
        tx.setPerformedBy(performedBy != null && !performedBy.isBlank() ? performedBy.trim() : "admin");
        transactionRepository.save(tx);

        log.info("Batch #{} damaged report: -{} units ({} -> {}), performed by {}",
                savedBatch.getBatchNumber(), req.getQuantity(), batchBefore, batchAfter, performedBy);

        return BatchDTO.fromEntity(savedBatch);
    }

    // ------------------------------------------------------------------------
    // Read & Query Operations
    // ------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public BatchDTO getBatch(Long id) {
        FertilizerBatch batch = batchRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Batch not found with id: " + id));
        return BatchDTO.fromEntity(batch);
    }

    @Transactional(readOnly = true)
    public List<BatchDTO> getAllBatches(Long fertilizerId, String status, String search, String expiryFilter) {
        List<FertilizerBatch> list;
        if (fertilizerId != null) {
            list = batchRepository.findByFertilizerIdOrderByExpiryDateAsc(fertilizerId);
        } else {
            list = batchRepository.findAll();
        }

        LocalDate today = LocalDate.now();

        return list.stream()
                .filter(b -> {
                    // Status filter
                    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
                        BatchStatus dynamic = b.computeDynamicStatus();
                        if (!dynamic.name().equalsIgnoreCase(status.trim())) {
                            return false;
                        }
                    }

                    // Expiry filter
                    if (expiryFilter != null && !expiryFilter.isBlank() && !"ALL".equalsIgnoreCase(expiryFilter.trim())) {
                        String ef = expiryFilter.trim().toLowerCase(Locale.ROOT);
                        if ("expired".equals(ef) && !b.isExpired()) {
                            return false;
                        }
                        if ("near_expiry".equals(ef) && !b.isNearExpiry()) {
                            return false;
                        }
                        if ("active".equals(ef) && (b.isExpired() || b.getStatus() == BatchStatus.BLOCKED || b.getAvailableQuantity() <= 0)) {
                            return false;
                        }
                    }

                    // Search filter
                    if (search != null && !search.isBlank()) {
                        String q = search.trim().toLowerCase(Locale.ROOT);
                        String bn = b.getBatchNumber() != null ? b.getBatchNumber().toLowerCase(Locale.ROOT) : "";
                        String fn = b.getFertilizer() != null && b.getFertilizer().getName() != null ? b.getFertilizer().getName().toLowerCase(Locale.ROOT) : "";
                        String sku = b.getFertilizer() != null && b.getFertilizer().getSku() != null ? b.getFertilizer().getSku().toLowerCase(Locale.ROOT) : "";
                        String sup = b.getSupplierName() != null ? b.getSupplierName().toLowerCase(Locale.ROOT) : "";
                        if (!bn.contains(q) && !fn.contains(q) && !sku.contains(q) && !sup.contains(q)) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted(Comparator.comparing(FertilizerBatch::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(FertilizerBatch::getId))
                .map(BatchDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BatchDTO> getNearExpiryBatches() {
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(30);
        return batchRepository.findNearExpiryBatches(today, threshold).stream()
                .map(BatchDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BatchDTO> getExpiredBatches() {
        LocalDate today = LocalDate.now();
        return batchRepository.findExpiredBatches(today).stream()
                .map(BatchDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDTO> getBatchHistory(Long batchId) {
        FertilizerBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NoSuchElementException("Batch not found with id: " + batchId));

        return transactionRepository.findAll().stream()
                .filter(t -> t.getBatch() != null && t.getBatch().getId().equals(batchId))
                .sorted(Comparator.comparing(InventoryTransaction::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(InventoryTransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BatchSummaryDTO getSummary() {
        List<FertilizerBatch> all = batchRepository.findAll();

        long totalBatches = all.size();
        long activeBatches = 0;
        long nearExpiryBatches = 0;
        long expiredBatches = 0;
        long depletedBatches = 0;
        long blockedBatches = 0;
        long totalBatchUnits = 0;
        long nearExpiryUnits = 0;
        long expiredUnits = 0;

        for (FertilizerBatch b : all) {
            BatchStatus status = b.computeDynamicStatus();
            int curr = b.getCurrentQuantity() != null ? b.getCurrentQuantity() : 0;
            totalBatchUnits += curr;

            if (status == BatchStatus.BLOCKED) {
                blockedBatches++;
            } else if (status == BatchStatus.EXPIRED) {
                expiredBatches++;
                expiredUnits += curr;
            } else if (status == BatchStatus.DEPLETED) {
                depletedBatches++;
            } else if (status == BatchStatus.NEAR_EXPIRY) {
                nearExpiryBatches++;
                nearExpiryUnits += curr;
            } else {
                activeBatches++;
            }
        }

        return new BatchSummaryDTO(
                totalBatches,
                activeBatches,
                nearExpiryBatches,
                expiredBatches,
                depletedBatches,
                blockedBatches,
                totalBatchUnits,
                nearExpiryUnits,
                expiredUnits
        );
    }
}
