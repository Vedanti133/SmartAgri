package com.smartagri.service;

import com.smartagri.dto.*;
import com.smartagri.entity.*;
import com.smartagri.repository.FertilizerRepository;
import com.smartagri.repository.InventoryTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final FertilizerRepository fertilizerRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final com.smartagri.repository.FertilizerBatchRepository batchRepository;
    private final com.smartagri.repository.OrderItemBatchRepository orderItemBatchRepository;
    private final com.smartagri.repository.OrderItemRepository orderItemRepository;
    private final com.smartagri.repository.PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final com.smartagri.repository.GoodsReceiptRepository goodsReceiptRepository;
    private final com.smartagri.repository.GoodsReceiptItemRepository goodsReceiptItemRepository;
    private final com.smartagri.repository.WarehouseRepository warehouseRepository;
    private final com.smartagri.repository.WarehouseLocationRepository locationRepository;
    private final com.smartagri.repository.BatchLocationStockRepository batchLocationStockRepository;
    private final com.smartagri.repository.StockTransferRepository stockTransferRepository;
    private final com.smartagri.repository.ShipmentRepository shipmentRepository;

    public InventoryService(FertilizerRepository fertilizerRepository,
                            InventoryTransactionRepository transactionRepository,
                            com.smartagri.repository.FertilizerBatchRepository batchRepository,
                            com.smartagri.repository.OrderItemBatchRepository orderItemBatchRepository,
                            com.smartagri.repository.OrderItemRepository orderItemRepository,
                            com.smartagri.repository.PurchaseOrderItemRepository purchaseOrderItemRepository,
                            com.smartagri.repository.GoodsReceiptRepository goodsReceiptRepository,
                            com.smartagri.repository.GoodsReceiptItemRepository goodsReceiptItemRepository,
                            com.smartagri.repository.WarehouseRepository warehouseRepository,
                            com.smartagri.repository.WarehouseLocationRepository locationRepository,
                            com.smartagri.repository.BatchLocationStockRepository batchLocationStockRepository,
                            com.smartagri.repository.StockTransferRepository stockTransferRepository,
                            com.smartagri.repository.ShipmentRepository shipmentRepository) {
        this.fertilizerRepository = fertilizerRepository;
        this.transactionRepository = transactionRepository;
        this.batchRepository = batchRepository;
        this.orderItemBatchRepository = orderItemBatchRepository;
        this.orderItemRepository = orderItemRepository;
        this.purchaseOrderItemRepository = purchaseOrderItemRepository;
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.goodsReceiptItemRepository = goodsReceiptItemRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.batchLocationStockRepository = batchLocationStockRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.shipmentRepository = shipmentRepository;
    }

    // ------------------------------------------------------------------------
    // Stock Movements & Mutations (Centralized & Concurrency-Safe)
    // ------------------------------------------------------------------------

    @Transactional
    public InventoryDTO addStock(Long fertilizerId, StockInRequest request, String performedBy) {
        int quantity = request.getQuantity() != null ? request.getQuantity() : 0;
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to add must be greater than zero. Received: " + quantity);
        }

        Fertilizer fert = fertilizerRepository.findByIdWithLock(fertilizerId)
                .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with id: " + fertilizerId));

        int stockBefore = fert.getStock() != null ? fert.getStock() : 0;
        int stockAfter = stockBefore + quantity;

        fert.setStock(stockAfter);
        if (request.getUnitCost() != null) {
            fert.setPurchasePrice(request.getUnitCost());
        }
        if (request.getSupplier() != null && !request.getSupplier().isBlank()) {
            fert.setSupplier(request.getSupplier().trim());
        }
        if (request.getLowStockThreshold() != null && request.getLowStockThreshold() > 0) {
            fert.setLowStockThreshold(request.getLowStockThreshold());
        }
        fert.syncStatusAndStock();
        Fertilizer saved = fertilizerRepository.save(fert);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setFertilizer(saved);
        tx.setTransactionType(InventoryTransactionType.RESTOCK);
        tx.setQuantity(quantity);
        tx.setStockBefore(stockBefore);
        tx.setStockAfter(stockAfter);
        tx.setReferenceType("RESTOCK");
        tx.setReferenceId(request.getReferenceId() != null && !request.getReferenceId().isBlank()
                ? request.getReferenceId().trim()
                : "RESTOCK-" + System.currentTimeMillis());
        String combinedReason = request.getReason() != null && !request.getReason().isBlank() 
                ? request.getReason().trim() 
                : "Supplier delivery restock";
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            combinedReason += " - Notes: " + request.getNotes().trim();
        }
        tx.setReason(combinedReason);
        tx.setPerformedBy(performedBy != null && !performedBy.isBlank() ? performedBy.trim() : "admin");
        transactionRepository.save(tx);

        log.info("Stock added for product #{}: +{} ({} -> {}), performed by {}",
                fertilizerId, quantity, stockBefore, stockAfter, performedBy);

        return InventoryDTO.fromFertilizer(saved);
    }

    @Transactional
    public InventoryDTO addStock(Long fertilizerId, int quantity, String referenceId, String reason, String performedBy) {
        StockInRequest req = new StockInRequest();
        req.setQuantity(quantity);
        req.setReferenceId(referenceId);
        req.setReason(reason);
        return addStock(fertilizerId, req, performedBy);
    }

    @Transactional
    public void deductStockForOrder(Long fertilizerId, int quantity, String orderNumber, String customerUsername) {
        deductStockForOrder(fertilizerId, quantity, orderNumber, customerUsername, null);
    }

    @Transactional
    public void deductStockForOrder(Long fertilizerId, int quantity, String orderNumber, String customerUsername, Long orderItemId) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Deduction quantity must be positive: " + quantity);
        }

        // Idempotency: verify stock was not already deducted for this exact order and item
        if (transactionRepository.existsByTransactionTypeAndReferenceIdAndFertilizerId(
                InventoryTransactionType.SALE, orderNumber, fertilizerId)) {
            log.warn("Stock deduction already recorded for Order #{} and Product #{}. Skipping duplicate deduction.",
                    orderNumber, fertilizerId);
            return;
        }

        // Pessimistic Write Lock to prevent concurrency overselling
        Fertilizer fert = fertilizerRepository.findByIdWithLock(fertilizerId)
                .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with id: " + fertilizerId));

        long batchCount = batchRepository.countByFertilizerId(fertilizerId);

        if (batchCount > 0) {
            // FEFO Allocation: Find non-expired, active, non-blocked batches ordered by expiryDate ASC
            LocalDate today = LocalDate.now();
            List<FertilizerBatch> saleableBatches = batchRepository.findSaleableBatchesFEFOWithLock(fertilizerId, today);

            int totalAvailableBatchStock = saleableBatches.stream()
                    .mapToInt(FertilizerBatch::getAvailableQuantity)
                    .sum();

            if (totalAvailableBatchStock < quantity) {
                throw new IllegalStateException(String.format(
                        Locale.ROOT, "Product '%s' does not have sufficient non-expired stock. Available: %d, Requested: %d.",
                        fert.getName(), totalAvailableBatchStock, quantity));
            }

            OrderItem orderItem = null;
            if (orderItemId != null) {
                orderItem = orderItemRepository.findById(orderItemId).orElse(null);
            }

            int remaining = quantity;
            int fertStockBefore = fert.getStock() != null ? fert.getStock() : 0;

            for (FertilizerBatch b : saleableBatches) {
                if (remaining <= 0) {
                    break;
                }
                int avail = b.getAvailableQuantity();
                if (avail <= 0) {
                    continue;
                }

                int alloc = Math.min(remaining, avail);
                int bBefore = b.getCurrentQuantity() != null ? b.getCurrentQuantity() : 0;
                int bAfter = bBefore - alloc;
                b.setCurrentQuantity(bAfter);
                b.setStatus(b.computeDynamicStatus());
                batchRepository.save(b);

                // Deduct from location stocks of this batch
                List<BatchLocationStock> locStocks = batchLocationStockRepository.findByBatchId(b.getId()).stream()
                        .filter(ls -> ls.getWarehouse() != null && ls.getWarehouse().getStatus() == WarehouseStatus.ACTIVE
                                && ls.getLocation() != null && ls.getLocation().getStatus() == LocationStatus.ACTIVE
                                && ls.getLocation().getLocationType() != LocationType.QUARANTINE
                                && ls.getLocation().getLocationType() != LocationType.DAMAGED
                                && ls.getAvailableQuantity() > 0)
                        .collect(Collectors.toList());

                int locRemaining = alloc;
                Warehouse lastAllocWh = null;
                WarehouseLocation lastAllocLoc = null;

                for (BatchLocationStock ls : locStocks) {
                    if (locRemaining <= 0) break;
                    int locAvail = ls.getAvailableQuantity();
                    int locDeduct = Math.min(locRemaining, locAvail);
                    ls.setQuantity(ls.getQuantity() - locDeduct);
                    batchLocationStockRepository.save(ls);
                    locRemaining -= locDeduct;
                    lastAllocWh = ls.getWarehouse();
                    lastAllocLoc = ls.getLocation();

                    if (orderItem != null) {
                        OrderItemBatch oib = new OrderItemBatch(orderItem, b, locDeduct, ls.getWarehouse(), ls.getLocation());
                        orderItemBatchRepository.save(oib);
                    }
                }

                // If no active location stock was found or remaining unmapped
                if (orderItem != null && (locStocks.isEmpty() || locRemaining > 0)) {
                    int unmapped = locStocks.isEmpty() ? alloc : locRemaining;
                    if (unmapped > 0) {
                        OrderItemBatch oib = new OrderItemBatch(orderItem, b, unmapped);
                        orderItemBatchRepository.save(oib);
                    }
                }

                // Batch-specific SALE audit transaction
                InventoryTransaction tx = new InventoryTransaction();
                tx.setFertilizer(fert);
                tx.setBatch(b);
                tx.setWarehouse(lastAllocWh);
                tx.setLocation(lastAllocLoc);
                tx.setTransactionType(InventoryTransactionType.SALE);
                tx.setQuantity(alloc);
                tx.setStockBefore(fertStockBefore);
                tx.setStockAfter(fertStockBefore - alloc);
                tx.setReferenceType("ORDER");
                tx.setReferenceId(orderNumber);
                tx.setReason("FEFO Order purchase - Batch " + b.getBatchNumber());
                tx.setPerformedBy(customerUsername != null ? customerUsername : "SYSTEM");
                transactionRepository.save(tx);

                fertStockBefore -= alloc;
                remaining -= alloc;
            }

            // Synchronize parent fertilizer stock
            fert.setStock(fert.getStock() - quantity);
            fert.syncStatusAndStock();
            fertilizerRepository.save(fert);

            log.info("FEFO deducted {} units for Order #{} from batches of product #{}", quantity, orderNumber, fertilizerId);

        } else {
            // Legacy product stock deduction without batches (Phase 10 backward compatibility)
            int available = fert.getAvailableStock();
            if (available < quantity) {
                throw new IllegalArgumentException(String.format(
                        Locale.ROOT, "Insufficient stock for product '%s'. Available: %d, Requested: %d.",
                        fert.getName(), available, quantity));
            }

            int stockBefore = fert.getStock() != null ? fert.getStock() : 0;
            int stockAfter = stockBefore - quantity;

            fert.setStock(stockAfter);
            fert.syncStatusAndStock();
            Fertilizer saved = fertilizerRepository.save(fert);

            InventoryTransaction tx = new InventoryTransaction();
            tx.setFertilizer(saved);
            tx.setTransactionType(InventoryTransactionType.SALE);
            tx.setQuantity(quantity);
            tx.setStockBefore(stockBefore);
            tx.setStockAfter(stockAfter);
            tx.setReferenceType("ORDER");
            tx.setReferenceId(orderNumber);
            tx.setReason("Order purchase");
            tx.setPerformedBy(customerUsername != null ? customerUsername : "SYSTEM");
            transactionRepository.save(tx);

            log.info("Deducted legacy stock for Order #{}: product #{}, qty -{} ({} -> {})",
                    orderNumber, fertilizerId, quantity, stockBefore, stockAfter);
        }
    }

    @Transactional
    public void restoreStockForOrder(Long fertilizerId, int quantity, String orderNumber, String customerUsername) {
        restoreStockForOrder(fertilizerId, quantity, orderNumber, customerUsername, null);
    }

    @Transactional
    public void restoreStockForOrder(Long fertilizerId, int quantity, String orderNumber, String customerUsername, Long orderItemId) {
        if (quantity <= 0) {
            return;
        }

        // Idempotency: verify stock was not already restored for this order
        if (transactionRepository.existsByTransactionTypeAndReferenceIdAndFertilizerId(
                InventoryTransactionType.CANCELLATION, orderNumber, fertilizerId)) {
            log.warn("Stock restoration already recorded for cancelled Order #{} and Product #{}. Skipping duplicate restore.",
                    orderNumber, fertilizerId);
            return;
        }

        // Check if there was an actual SALE recorded for this order and product
        boolean hadSale = transactionRepository.existsByTransactionTypeAndReferenceIdAndFertilizerId(
                InventoryTransactionType.SALE, orderNumber, fertilizerId);
        if (!hadSale) {
            log.warn("No sale transaction found for Order #{} and Product #{}. Skipping restore.",
                    orderNumber, fertilizerId);
            return;
        }

        Fertilizer fert = fertilizerRepository.findByIdWithLock(fertilizerId)
                .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with id: " + fertilizerId));

        // Check if order_item_batches records exist for this order and product
        List<OrderItemBatch> batchAllocations = null;
        if (orderItemId != null) {
            batchAllocations = orderItemBatchRepository.findByOrderItemId(orderItemId);
        }
        if (batchAllocations == null || batchAllocations.isEmpty()) {
            batchAllocations = orderItemBatchRepository.findByOrderNumberAndFertilizerId(orderNumber, fertilizerId);
        }

        if (batchAllocations != null && !batchAllocations.isEmpty()) {
            int fertStockBefore = fert.getStock() != null ? fert.getStock() : 0;

            for (OrderItemBatch oib : batchAllocations) {
                FertilizerBatch b = batchRepository.findByIdWithLock(oib.getBatch().getId())
                        .orElse(oib.getBatch());

                int bBefore = b.getCurrentQuantity() != null ? b.getCurrentQuantity() : 0;
                int bAfter = bBefore + oib.getQuantity();
                b.setCurrentQuantity(bAfter);
                b.setStatus(b.computeDynamicStatus());
                batchRepository.save(b);

                // Restore to exact BatchLocationStock if recorded
                if (oib.getWarehouse() != null && oib.getLocation() != null) {
                    BatchLocationStock locStock = batchLocationStockRepository.findByBatchIdAndLocationIdWithLock(
                            b.getId(), oib.getLocation().getId())
                            .orElseGet(() -> {
                                BatchLocationStock bls = new BatchLocationStock();
                                bls.setBatch(b);
                                bls.setWarehouse(oib.getWarehouse());
                                bls.setLocation(oib.getLocation());
                                bls.setQuantity(0);
                                bls.setReservedQuantity(0);
                                return batchLocationStockRepository.save(bls);
                            });
                    locStock.setQuantity(locStock.getQuantity() + oib.getQuantity());
                    batchLocationStockRepository.save(locStock);
                }

                InventoryTransaction tx = new InventoryTransaction();
                tx.setFertilizer(fert);
                tx.setBatch(b);
                tx.setWarehouse(oib.getWarehouse());
                tx.setLocation(oib.getLocation());
                tx.setTransactionType(InventoryTransactionType.CANCELLATION);
                tx.setQuantity(oib.getQuantity());
                tx.setStockBefore(fertStockBefore);
                tx.setStockAfter(fertStockBefore + oib.getQuantity());
                tx.setReferenceType("ORDER");
                tx.setReferenceId(orderNumber);
                tx.setReason("Order cancellation stock restoration - Batch " + b.getBatchNumber());
                tx.setPerformedBy(customerUsername != null ? customerUsername : "SYSTEM");
                transactionRepository.save(tx);

                fertStockBefore += oib.getQuantity();
            }

            fert.setStock(fert.getStock() + quantity);
            fert.syncStatusAndStock();
            fertilizerRepository.save(fert);

            log.info("Restored batch stock for cancelled Order #{}: product #{}, qty +{}",
                    orderNumber, fertilizerId, quantity);

        } else {
            // Legacy restoration
            int stockBefore = fert.getStock() != null ? fert.getStock() : 0;
            int stockAfter = stockBefore + quantity;

            fert.setStock(stockAfter);
            fert.syncStatusAndStock();
            Fertilizer saved = fertilizerRepository.save(fert);

            InventoryTransaction tx = new InventoryTransaction();
            tx.setFertilizer(saved);
            tx.setTransactionType(InventoryTransactionType.CANCELLATION);
            tx.setQuantity(quantity);
            tx.setStockBefore(stockBefore);
            tx.setStockAfter(stockAfter);
            tx.setReferenceType("ORDER");
            tx.setReferenceId(orderNumber);
            tx.setReason("Order cancellation stock restoration");
            tx.setPerformedBy(customerUsername != null ? customerUsername : "SYSTEM");
            transactionRepository.save(tx);

            log.info("Restored legacy stock for cancelled Order #{}: product #{}, qty +{} ({} -> {})",
                    orderNumber, fertilizerId, quantity, stockBefore, stockAfter);
        }
    }

    @Transactional
    public InventoryDTO adjustStock(Long fertilizerId, StockAdjustmentRequest request, String performedBy) {
        if (request.getReason() == null || request.getReason().trim().isEmpty()) {
            throw new IllegalArgumentException("Audit reason is strictly required for inventory adjustments");
        }

        String typeStr = request.getAdjustmentType() != null ? request.getAdjustmentType().trim().toUpperCase(Locale.ROOT) : "";
        int qty = request.getQuantity() != null ? request.getQuantity() : 0;

        Fertilizer fert = fertilizerRepository.findByIdWithLock(fertilizerId)
                .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with id: " + fertilizerId));

        int stockBefore = fert.getStock() != null ? fert.getStock() : 0;
        int stockAfter;
        int txQty;
        InventoryTransactionType txType;

        if ("ADD".equals(typeStr) || "ADJUSTMENT_IN".equals(typeStr)) {
            if (qty <= 0) {
                throw new IllegalArgumentException("Adjustment quantity must be at least 1");
            }
            txType = InventoryTransactionType.ADJUSTMENT_IN;
            stockAfter = stockBefore + qty;
            txQty = qty;
        } else if ("REMOVE".equals(typeStr) || "ADJUSTMENT_OUT".equals(typeStr)) {
            if (qty <= 0) {
                throw new IllegalArgumentException("Adjustment quantity must be at least 1");
            }
            if (fert.getAvailableStock() < qty) {
                throw new IllegalArgumentException(String.format(
                        Locale.ROOT, "Insufficient stock for reduction. Available: %d, Requested reduction: %d.",
                        fert.getAvailableStock(), qty));
            }
            txType = InventoryTransactionType.ADJUSTMENT_OUT;
            stockAfter = stockBefore - qty;
            txQty = qty;
        } else if ("SET".equals(typeStr)) {
            if (qty < 0) {
                throw new IllegalArgumentException("Target stock level cannot be negative: " + qty);
            }
            stockAfter = qty;
            int diff = stockAfter - stockBefore;
            if (diff >= 0) {
                txType = InventoryTransactionType.ADJUSTMENT_IN;
                txQty = diff;
            } else {
                txType = InventoryTransactionType.ADJUSTMENT_OUT;
                txQty = Math.abs(diff);
            }
        } else {
            throw new IllegalArgumentException("Invalid adjustment type: " + typeStr + ". Expected ADD, REMOVE, SET, ADJUSTMENT_IN or ADJUSTMENT_OUT.");
        }

        fert.setStock(stockAfter);
        fert.syncStatusAndStock();
        Fertilizer saved = fertilizerRepository.save(fert);

        InventoryTransaction tx = new InventoryTransaction();
        tx.setFertilizer(saved);
        tx.setTransactionType(txType);
        tx.setQuantity(txQty);
        tx.setStockBefore(stockBefore);
        tx.setStockAfter(stockAfter);
        tx.setReferenceType("ADJUSTMENT");
        tx.setReferenceId(request.getReferenceId() != null && !request.getReferenceId().isBlank()
                ? request.getReferenceId().trim()
                : "ADJ-" + System.currentTimeMillis());
        String combinedReason = request.getReason().trim();
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            combinedReason += " - Notes: " + request.getNotes().trim();
        }
        tx.setReason(combinedReason);
        tx.setPerformedBy(performedBy != null && !performedBy.isBlank() ? performedBy.trim() : "admin");
        transactionRepository.save(tx);

        log.info("Stock adjusted for product #{}: {} by {} ({} -> {}), reason: '{}'",
                fertilizerId, txType, txQty, stockBefore, stockAfter, request.getReason());

        return InventoryDTO.fromFertilizer(saved);
    }

    // ------------------------------------------------------------------------
    // Read & Query Operations
    // ------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<InventoryDTO> getAllInventory(String status, String category, String search) {
        List<Fertilizer> fertilizers = fertilizerRepository.findAll();

        return fertilizers.stream()
                .filter(f -> {
                    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
                        StockStatus st = f.getStockStatus();
                        if (!st.name().equalsIgnoreCase(status.trim())) {
                            return false;
                        }
                    }
                    if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category.trim())) {
                        String catName = f.getCategory() != null ? f.getCategory().getName() : "";
                        if (!catName.equalsIgnoreCase(category.trim())) {
                            return false;
                        }
                    }
                    if (search != null && !search.isBlank()) {
                        String q = search.trim().toLowerCase(Locale.ROOT);
                        String name = f.getName() != null ? f.getName().toLowerCase(Locale.ROOT) : "";
                        String sku = f.getSku() != null ? f.getSku().toLowerCase(Locale.ROOT) : "";
                        String brand = f.getBrand() != null ? f.getBrand().toLowerCase(Locale.ROOT) : "";
                        if (!name.contains(q) && !sku.contains(q) && !brand.contains(q)) {
                            return false;
                        }
                    }
                    return true;
                })
                .sorted(Comparator.comparing(Fertilizer::getId))
                .map(InventoryDTO::fromFertilizer)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<InventoryDTO> getInventoryPaged(String status, String category, String search, Pageable pageable) {
        List<InventoryDTO> list = getAllInventory(status, category, search);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<InventoryDTO> subList = start < list.size() ? list.subList(start, end) : Collections.emptyList();
        return new PageImpl<>(subList, pageable, list.size());
    }

    @Transactional(readOnly = true)
    public InventoryDTO getProductInventory(Long fertilizerId) {
        Fertilizer fert = fertilizerRepository.findById(fertilizerId)
                .orElseThrow(() -> new NoSuchElementException("Fertilizer not found with id: " + fertilizerId));
        return InventoryDTO.fromFertilizer(fert);
    }

    @Transactional(readOnly = true)
    public StockStatusDTO getCustomerStockStatus(Long fertilizerId) {
        Fertilizer fert = fertilizerRepository.findById(fertilizerId)
                .orElseThrow(() -> new NoSuchElementException("Product not found with id: " + fertilizerId));

        long batchCount = batchRepository.countByFertilizerId(fertilizerId);
        if (batchCount > 0) {
            List<FertilizerBatch> saleable = batchRepository.findSaleableBatchesFEFO(fertilizerId, LocalDate.now());
            int saleableStock = saleable.stream()
                    .mapToInt(FertilizerBatch::getAvailableQuantity)
                    .sum();

            StockStatusDTO dto = new StockStatusDTO();
            dto.setProductId(fert.getId());
            dto.setProductName(fert.getName());
            dto.setAvailableStock(saleableStock);
            dto.setLowStockThreshold(fert.getLowStockThreshold());

            if (saleableStock <= 0) {
                dto.setStatus(StockStatus.OUT_OF_STOCK);
                dto.setDisplayBadge("Out of Stock");
                dto.setAvailable(false);
            } else if (saleableStock <= fert.getLowStockThreshold()) {
                dto.setStatus(StockStatus.LOW_STOCK);
                dto.setDisplayBadge("Only " + saleableStock + " left");
                dto.setAvailable(true);
            } else {
                dto.setStatus(StockStatus.IN_STOCK);
                dto.setDisplayBadge("In Stock");
                dto.setAvailable(true);
            }
            return dto;
        }

        return StockStatusDTO.fromFertilizer(fert);
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> getLowStockItems() {
        return fertilizerRepository.findLowStockFertilizers().stream()
                .map(InventoryDTO::fromFertilizer)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryDTO> getOutOfStockItems() {
        return fertilizerRepository.findOutOfStockFertilizers().stream()
                .map(InventoryDTO::fromFertilizer)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InventorySummaryDTO getSummary() {
        long totalProducts = fertilizerRepository.count();
        Long totalStockUnits = fertilizerRepository.countTotalStockUnits();
        Long lowStockCount = fertilizerRepository.countLowStockProducts();
        Long outOfStockCount = fertilizerRepository.countOutOfStockProducts();

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        List<InventoryTransactionType> addTypes = List.of(
                InventoryTransactionType.PURCHASE,
                InventoryTransactionType.RESTOCK,
                InventoryTransactionType.ADJUSTMENT_IN,
                InventoryTransactionType.RETURN,
                InventoryTransactionType.CANCELLATION
        );
        Long addedToday = transactionRepository.sumQuantityByTypesSince(addTypes, startOfToday);

        List<InventoryTransactionType> removeTypes = List.of(
                InventoryTransactionType.SALE,
                InventoryTransactionType.ADJUSTMENT_OUT,
                InventoryTransactionType.DAMAGE
        );
        Long removedToday = transactionRepository.sumQuantityByTypesSince(removeTypes, startOfToday);

        return new InventorySummaryDTO(
                totalProducts,
                totalStockUnits != null ? totalStockUnits : 0L,
                lowStockCount != null ? lowStockCount : 0L,
                outOfStockCount != null ? outOfStockCount : 0L,
                addedToday != null ? addedToday : 0L,
                removedToday != null ? removedToday : 0L
        );
    }

    @Transactional(readOnly = true)
    public InventoryHealthDTO getHealth() {
        InventoryHealthDTO health = new InventoryHealthDTO();
        List<Fertilizer> all = fertilizerRepository.findAll();
        health.setProductsChecked(all.size());

        long negCount = 0;
        long lowCount = 0;
        long outCount = 0;
        long totalUnits = 0;
        List<String> issues = new ArrayList<>();

        for (Fertilizer f : all) {
            int current = f.getStock() != null ? f.getStock() : 0;
            totalUnits += current;

            if (current < 0) {
                negCount++;
                issues.add(String.format(Locale.ROOT, "Product #%d (%s) has negative stock: %d", f.getId(), f.getName(), current));
            } else if (current == 0) {
                outCount++;
            } else if (current <= f.getLowStockThreshold()) {
                lowCount++;
            }

            // Verify status consistency
            if (current == 0 && !"OUT_OF_STOCK".equalsIgnoreCase(f.getStatus()) && !"INACTIVE".equalsIgnoreCase(f.getStatus())) {
                issues.add(String.format(Locale.ROOT, "Product #%d has 0 stock but status is '%s' instead of OUT_OF_STOCK", f.getId(), f.getStatus()));
            }
        }

        health.setNegativeStockCount(negCount);
        health.setLowStockCount(lowCount);
        health.setOutOfStockCount(outCount);
        health.setTotalStockUnits(totalUnits);
        health.setTotalTransactions(transactionRepository.count());

        // Phase 11: Batch Health Checks
        List<FertilizerBatch> allBatches = batchRepository.findAll();
        health.setBatchesChecked(allBatches.size());
        long batchIssues = 0;

        Set<String> seenProductBatches = new HashSet<>();

        for (FertilizerBatch b : allBatches) {
            int currentQty = b.getCurrentQuantity() != null ? b.getCurrentQuantity() : 0;
            int reservedQty = b.getReservedQuantity() != null ? b.getReservedQuantity() : 0;

            // 1. Negative batch current quantity
            if (currentQty < 0) {
                batchIssues++;
                issues.add(String.format(Locale.ROOT, "Batch #%d (%s) has negative current quantity: %d", b.getId(), b.getBatchNumber(), currentQty));
            }

            // 2. Negative reserved quantity
            if (reservedQty < 0) {
                batchIssues++;
                issues.add(String.format(Locale.ROOT, "Batch #%d (%s) has negative reserved quantity: %d", b.getId(), b.getBatchNumber(), reservedQty));
            }

            // 3. Reserved quantity > current quantity
            if (reservedQty > currentQty) {
                batchIssues++;
                issues.add(String.format(Locale.ROOT, "Batch #%d (%s) reserved quantity (%d) exceeds current quantity (%d)", b.getId(), b.getBatchNumber(), reservedQty, currentQty));
            }

            // 4. Expired batch incorrectly marked saleable (status ACTIVE and not yet expired in status)
            if (b.isExpired() && b.getStatus() == BatchStatus.ACTIVE && b.getAvailableQuantity() > 0) {
                batchIssues++;
                issues.add(String.format(Locale.ROOT, "Batch #%d (%s) is expired (%s) but marked ACTIVE with available quantity %d", b.getId(), b.getBatchNumber(), b.getExpiryDate(), b.getAvailableQuantity()));
            }

            // 5. Duplicate batch numbers for the same product
            if (b.getFertilizer() != null) {
                String key = b.getFertilizer().getId() + ":" + b.getBatchNumber().trim().toLowerCase(Locale.ROOT);
                if (!seenProductBatches.add(key)) {
                    batchIssues++;
                    issues.add(String.format(Locale.ROOT, "Duplicate batch number '%s' detected for product #%d", b.getBatchNumber(), b.getFertilizer().getId()));
                }
            } else {
                batchIssues++;
                issues.add(String.format(Locale.ROOT, "Batch #%d (%s) is orphaned with no fertilizer associated", b.getId(), b.getBatchNumber()));
            }

            // 6. Invalid manufacturing/expiry dates
            if (b.getManufacturingDate() != null && b.getExpiryDate() != null && b.getManufacturingDate().isAfter(b.getExpiryDate())) {
                batchIssues++;
                issues.add(String.format(Locale.ROOT, "Batch #%d (%s) has manufacturing date (%s) after expiry date (%s)", b.getId(), b.getBatchNumber(), b.getManufacturingDate(), b.getExpiryDate()));
            }
        }

        // 7. Check for OrderItemBatch allocations referencing non-existent batches
        List<OrderItemBatch> allOib = orderItemBatchRepository.findAll();
        for (OrderItemBatch oib : allOib) {
            if (oib.getBatch() == null || !batchRepository.existsById(oib.getBatch().getId())) {
                batchIssues++;
                issues.add(String.format(Locale.ROOT, "OrderItemBatch #%d references a non-existent batch", oib.getId()));
            }
        }

        health.setBatchIssuesCount(batchIssues);

        // Phase 12: Procurement Consistency Checks
        List<com.smartagri.entity.GoodsReceipt> allGrns = goodsReceiptRepository.findAll();
        health.setGoodsReceiptsChecked(allGrns.size());
        long procurementIssues = 0;

        // 1. PO received quantity > ordered quantity
        List<com.smartagri.entity.PurchaseOrderItem> poiList = purchaseOrderItemRepository.findAll();
        for (com.smartagri.entity.PurchaseOrderItem poi : poiList) {
            int ord = poi.getOrderedQuantity() != null ? poi.getOrderedQuantity() : 0;
            int rec = poi.getReceivedQuantity() != null ? poi.getReceivedQuantity() : 0;
            if (rec > ord) {
                procurementIssues++;
                issues.add(String.format(Locale.ROOT, "PO Item #%d has received quantity (%d) exceeding ordered quantity (%d)", poi.getId(), rec, ord));
            }
        }

        // 2. GRN accepted > received OR rejected > received
        List<com.smartagri.entity.GoodsReceiptItem> griList = goodsReceiptItemRepository.findAll();
        for (com.smartagri.entity.GoodsReceiptItem gri : griList) {
            int rec = gri.getReceivedQuantity() != null ? gri.getReceivedQuantity() : 0;
            int acc = gri.getAcceptedQuantity() != null ? gri.getAcceptedQuantity() : 0;
            int rej = gri.getRejectedQuantity() != null ? gri.getRejectedQuantity() : 0;
            if (acc > rec) {
                procurementIssues++;
                issues.add(String.format(Locale.ROOT, "GRN Item #%d accepted quantity (%d) exceeds received quantity (%d)", gri.getId(), acc, rec));
            }
            if (rej > rec) {
                procurementIssues++;
                issues.add(String.format(Locale.ROOT, "GRN Item #%d rejected quantity (%d) exceeds received quantity (%d)", gri.getId(), rej, rec));
            }
            // 3. Missing batch for accepted lot
            if (gri.getGoodsReceipt() != null && gri.getGoodsReceipt().getStatus() == com.smartagri.entity.GoodsReceiptStatus.ACCEPTED && acc > 0) {
                if (gri.getFertilizer() != null && !batchRepository.existsByFertilizerIdAndBatchNumber(gri.getFertilizer().getId(), gri.getBatchNumber())) {
                    procurementIssues++;
                    issues.add(String.format(Locale.ROOT, "Accepted GRN Item #%d is missing associated batch '%s'", gri.getId(), gri.getBatchNumber()));
                }
            }
        }

        // 4. Missing RESTOCK transaction for accepted GRN
        List<com.smartagri.entity.GoodsReceipt> acceptedGrns = goodsReceiptRepository.findByStatusOrderByCreatedAtDesc(com.smartagri.entity.GoodsReceiptStatus.ACCEPTED);
        for (com.smartagri.entity.GoodsReceipt grn : acceptedGrns) {
            boolean hasAcceptedItems = grn.getItems().stream().anyMatch(i -> i.getAcceptedQuantity() != null && i.getAcceptedQuantity() > 0);
            if (hasAcceptedItems) {
                boolean hasRestock = transactionRepository.existsByTransactionTypeAndReferenceId(
                        InventoryTransactionType.RESTOCK, grn.getGrnNumber());
                if (!hasRestock) {
                    procurementIssues++;
                    issues.add(String.format(Locale.ROOT, "Accepted GRN #%s is missing RESTOCK inventory audit transaction", grn.getGrnNumber()));
                }
            }
        }

        health.setProcurementIssuesCount(procurementIssues);

        // Phase 13: Warehouse & Location Consistency Checks
        List<Warehouse> allWarehouses = warehouseRepository.findAll();
        health.setWarehousesChecked(allWarehouses.size());
        List<WarehouseLocation> allLocations = locationRepository.findAll();
        health.setLocationsChecked(allLocations.size());
        List<BatchLocationStock> allBls = batchLocationStockRepository.findAll();
        health.setBatchLocationRecordsChecked(allBls.size());

        long warehouseIssues = 0;
        long transferIssues = 0;

        // 1. Batch quantity vs Location quantity consistency
        for (FertilizerBatch b : allBatches) {
            int batchCurr = b.getCurrentQuantity() != null ? b.getCurrentQuantity() : 0;
            Integer locSum = batchLocationStockRepository.sumQuantityByBatchId(b.getId());
            if (locSum != null && locSum > 0 && locSum != batchCurr) {
                warehouseIssues++;
                issues.add(String.format(Locale.ROOT, "Batch #%s currentQuantity (%d) does not match sum of location stock (%d)",
                        b.getBatchNumber(), batchCurr, locSum));
            }
        }

        // 2. Negative location stock & reserved validation
        for (BatchLocationStock bls : allBls) {
            int q = bls.getQuantity() != null ? bls.getQuantity() : 0;
            int r = bls.getReservedQuantity() != null ? bls.getReservedQuantity() : 0;
            if (q < 0) {
                warehouseIssues++;
                issues.add(String.format(Locale.ROOT, "Location Stock #%d has negative quantity: %d", bls.getId(), q));
            }
            if (r < 0) {
                warehouseIssues++;
                issues.add(String.format(Locale.ROOT, "Location Stock #%d has negative reserved quantity: %d", bls.getId(), r));
            }
            if (r > q) {
                warehouseIssues++;
                issues.add(String.format(Locale.ROOT, "Location Stock #%d has reserved quantity (%d) exceeding total quantity (%d)",
                        bls.getId(), r, q));
            }
            if (bls.getLocation() != null && bls.getLocation().getStatus() == LocationStatus.BLOCKED && q > 0) {
                warehouseIssues++;
                issues.add(String.format(Locale.ROOT, "Location Stock #%d has %d units in BLOCKED location '%s'",
                        bls.getId(), q, bls.getLocation().getLocationCode()));
            }
            if (bls.getWarehouse() != null && bls.getWarehouse().getStatus() == WarehouseStatus.INACTIVE && q > 0) {
                warehouseIssues++;
                issues.add(String.format(Locale.ROOT, "Location Stock #%d has %d units in INACTIVE warehouse '%s'",
                        bls.getId(), q, bls.getWarehouse().getWarehouseCode()));
            }
        }

        // 3. Warehouse capacity overflow
        for (Warehouse w : allWarehouses) {
            if (w.getCapacity() != null && w.getCapacity() > 0) {
                int util = 0;
                for (BatchLocationStock bls : allBls) {
                    if (bls.getWarehouse() != null && bls.getWarehouse().getId().equals(w.getId())) {
                        util += (bls.getQuantity() != null ? bls.getQuantity() : 0);
                    }
                }
                if (util > w.getCapacity()) {
                    warehouseIssues++;
                    issues.add(String.format(Locale.ROOT, "Warehouse '%s' capacity exceeded: %d / %d units",
                            w.getWarehouseCode(), util, w.getCapacity()));
                }
            }
        }

        // 4. Stock Transfer Inconsistencies
        List<StockTransfer> transfers = stockTransferRepository.findAll();
        for (StockTransfer st : transfers) {
            if (st.getItems() != null) {
                for (StockTransferItem item : st.getItems()) {
                    int req = item.getRequestedQuantity() != null ? item.getRequestedQuantity() : 0;
                    int disp = item.getDispatchedQuantity() != null ? item.getDispatchedQuantity() : 0;
                    int rec = item.getReceivedQuantity() != null ? item.getReceivedQuantity() : 0;
                    if (disp > req) {
                        transferIssues++;
                        issues.add(String.format(Locale.ROOT, "Transfer #%s item #%d dispatched (%d) > requested (%d)",
                                st.getTransferNumber(), item.getId(), disp, req));
                    }
                    if (rec > disp) {
                        transferIssues++;
                        issues.add(String.format(Locale.ROOT, "Transfer #%s item #%d received (%d) > dispatched (%d)",
                                st.getTransferNumber(), item.getId(), rec, disp));
                    }
                }
            }
        }

        // 5. Fulfillment & Shipment Audits
        long fulfillmentIssues = 0;
        List<com.smartagri.entity.Shipment> allShipments = shipmentRepository.findAll();
        for (com.smartagri.entity.Shipment s : allShipments) {
            if (s.getTrackingNumber() == null || s.getTrackingNumber().isBlank()) {
                fulfillmentIssues++;
                issues.add("Shipment #" + s.getShipmentNumber() + " is missing tracking number.");
            }
            if (s.getStatus() == com.smartagri.enums.ShipmentStatus.DELIVERED && s.getDeliveredAt() == null) {
                fulfillmentIssues++;
                issues.add("Shipment #" + s.getShipmentNumber() + " is marked DELIVERED but delivered_at is null.");
            }
        }

        health.setWarehouseIssuesCount(warehouseIssues);
        health.setTransferIssuesCount(transferIssues);
        health.setShipmentsChecked(allShipments.size());
        health.setFulfillmentIssuesCount(fulfillmentIssues);
        health.setIssuesFound(issues.size());
        health.setIssuesList(issues);
        health.setStatus(issues.isEmpty() ? "HEALTHY" : "ISSUES_DETECTED");

        return health;
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDTO> getTransactions(Long fertilizerId, String type, String search) {
        InventoryTransactionType txType = null;
        if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type.trim())) {
            try {
                txType = InventoryTransactionType.valueOf(type.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }

        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        return transactionRepository.findFiltered(fertilizerId, txType, cleanSearch).stream()
                .map(InventoryTransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDTO> getProductTransactions(Long fertilizerId) {
        return transactionRepository.findByFertilizerIdOrderByCreatedAtDesc(fertilizerId).stream()
                .map(InventoryTransactionDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
