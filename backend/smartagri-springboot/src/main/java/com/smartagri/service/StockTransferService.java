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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class StockTransferService {

    private static final Logger log = LoggerFactory.getLogger(StockTransferService.class);

    private final StockTransferRepository stockTransferRepository;
    private final StockTransferItemRepository stockTransferItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final BatchLocationStockRepository batchLocationStockRepository;
    private final FertilizerBatchRepository batchRepository;
    private final FertilizerRepository fertilizerRepository;
    private final InventoryTransactionRepository transactionRepository;

    public StockTransferService(StockTransferRepository stockTransferRepository,
                                StockTransferItemRepository stockTransferItemRepository,
                                WarehouseRepository warehouseRepository,
                                WarehouseLocationRepository locationRepository,
                                BatchLocationStockRepository batchLocationStockRepository,
                                FertilizerBatchRepository batchRepository,
                                FertilizerRepository fertilizerRepository,
                                InventoryTransactionRepository transactionRepository) {
        this.stockTransferRepository = stockTransferRepository;
        this.stockTransferItemRepository = stockTransferItemRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.batchLocationStockRepository = batchLocationStockRepository;
        this.batchRepository = batchRepository;
        this.fertilizerRepository = fertilizerRepository;
        this.transactionRepository = transactionRepository;
    }

    private String generateTransferNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String transferNum;
        do {
            int randomSuffix = ThreadLocalRandom.current().nextInt(1000, 10000);
            transferNum = String.format(Locale.ROOT, "ST-%s-%04d", datePart, randomSuffix);
        } while (stockTransferRepository.existsByTransferNumber(transferNum));
        return transferNum;
    }

    // ------------------------------------------------------------------------
    // Create Transfer
    // ------------------------------------------------------------------------

    @Transactional
    public StockTransferDTO createTransfer(CreateStockTransferRequest req, String requestedBy) {
        if (req.getSourceWarehouseId() == null || req.getDestinationWarehouseId() == null) {
            throw new IllegalArgumentException("Source and destination warehouse IDs are strictly required");
        }
        if (req.getSourceWarehouseId().equals(req.getDestinationWarehouseId())) {
            throw new IllegalArgumentException("Source and destination warehouses cannot be the same");
        }
        if (req.getItems() == null || req.getItems().isEmpty()) {
            throw new IllegalArgumentException("Stock transfer must contain at least one item");
        }

        Warehouse sourceWh = warehouseRepository.findById(req.getSourceWarehouseId())
                .orElseThrow(() -> new NoSuchElementException("Source warehouse not found: " + req.getSourceWarehouseId()));
        Warehouse destWh = warehouseRepository.findById(req.getDestinationWarehouseId())
                .orElseThrow(() -> new NoSuchElementException("Destination warehouse not found: " + req.getDestinationWarehouseId()));

        if (sourceWh.getStatus() == WarehouseStatus.BLOCKED || sourceWh.getStatus() == WarehouseStatus.INACTIVE) {
            throw new IllegalStateException("Source warehouse is " + sourceWh.getStatus() + " and cannot dispatch stock.");
        }
        if (destWh.getStatus() == WarehouseStatus.BLOCKED || destWh.getStatus() == WarehouseStatus.INACTIVE) {
            throw new IllegalStateException("Destination warehouse is " + destWh.getStatus() + " and cannot receive stock.");
        }

        StockTransfer transfer = new StockTransfer();
        transfer.setTransferNumber(generateTransferNumber());
        transfer.setSourceWarehouse(sourceWh);
        transfer.setDestinationWarehouse(destWh);
        transfer.setRequestedBy(requestedBy != null && !requestedBy.isBlank() ? requestedBy.trim() : "admin");
        transfer.setRequestedAt(LocalDateTime.now());
        StockTransferStatus initialStatus = (req.getStatus() != null) ? req.getStatus() : StockTransferStatus.DRAFT;
        transfer.setStatus(initialStatus);
        transfer.setNotes(req.getNotes() != null ? req.getNotes().trim() : null);

        for (CreateStockTransferItemRequest itemReq : req.getItems()) {
            if (itemReq.getBatchId() == null) {
                throw new IllegalArgumentException("Batch ID is required for each transfer item");
            }
            if (itemReq.getSourceLocationId() == null) {
                throw new IllegalArgumentException("Source location ID is required for each transfer item");
            }
            if (itemReq.getRequestedQuantity() == null || itemReq.getRequestedQuantity() <= 0) {
                throw new IllegalArgumentException("Requested transfer quantity must be greater than zero");
            }

            FertilizerBatch batch = batchRepository.findById(itemReq.getBatchId())
                    .orElseThrow(() -> new NoSuchElementException("Batch not found: " + itemReq.getBatchId()));

            Fertilizer fert = (itemReq.getFertilizerId() != null)
                    ? fertilizerRepository.findById(itemReq.getFertilizerId()).orElse(batch.getFertilizer())
                    : batch.getFertilizer();

            WarehouseLocation srcLoc = locationRepository.findById(itemReq.getSourceLocationId())
                    .orElseThrow(() -> new NoSuchElementException("Source location not found: " + itemReq.getSourceLocationId()));

            WarehouseLocation destLoc = null;
            if (itemReq.getDestinationLocationId() != null) {
                destLoc = locationRepository.findById(itemReq.getDestinationLocationId())
                        .orElseThrow(() -> new NoSuchElementException("Destination location not found: " + itemReq.getDestinationLocationId()));
                if (!destLoc.getWarehouse().getId().equals(destWh.getId())) {
                    throw new IllegalArgumentException("Destination location '" + destLoc.getLocationCode() + "' does not belong to destination warehouse '" + destWh.getWarehouseCode() + "'");
                }
                if (destLoc.getStatus() == LocationStatus.BLOCKED) {
                    throw new IllegalStateException("Destination location '" + destLoc.getLocationCode() + "' is BLOCKED");
                }
            } else {
                destLoc = locationRepository.findByWarehouseIdAndStatus(destWh.getId(), LocationStatus.ACTIVE).stream().findFirst().orElse(null);
            }

            if (!srcLoc.getWarehouse().getId().equals(sourceWh.getId())) {
                throw new IllegalArgumentException("Source location '" + srcLoc.getLocationCode() + "' does not belong to source warehouse '" + sourceWh.getWarehouseCode() + "'");
            }
            if (srcLoc.getStatus() == LocationStatus.BLOCKED) {
                throw new IllegalStateException("Source location '" + srcLoc.getLocationCode() + "' is BLOCKED");
            }

            // Verify available quantity in source location
            BatchLocationStock srcStock = batchLocationStockRepository.findByBatchIdAndLocationId(batch.getId(), srcLoc.getId())
                    .orElseThrow(() -> new IllegalStateException("No stock record exists for batch '" + batch.getBatchNumber() + "' at source location '" + srcLoc.getLocationCode() + "'"));

            if (srcStock.getAvailableQuantity() < itemReq.getRequestedQuantity()) {
                throw new IllegalArgumentException(String.format(
                        "Insufficient available stock at source location '%s' for batch '%s'. Available: %d, Requested: %d.",
                        srcLoc.getLocationCode(), batch.getBatchNumber(), srcStock.getAvailableQuantity(), itemReq.getRequestedQuantity()));
            }

            StockTransferItem item = new StockTransferItem();
            item.setFertilizer(fert);
            item.setBatch(batch);
            item.setSourceLocation(srcLoc);
            item.setDestinationLocation(destLoc);
            item.setRequestedQuantity(itemReq.getRequestedQuantity());
            item.setDispatchedQuantity(0);
            item.setReceivedQuantity(0);
            item.setNotes(itemReq.getNotes());
            transfer.addItem(item);
        }

        StockTransfer saved = stockTransferRepository.save(transfer);
        log.info("Stock transfer created: #{} [{}] from {} to {} by {}",
                saved.getId(), saved.getTransferNumber(), sourceWh.getWarehouseCode(), destWh.getWarehouseCode(), requestedBy);
        return StockTransferDTO.fromEntity(saved);
    }

    // ------------------------------------------------------------------------
    // Transfer Lifecycle Transitions
    // ------------------------------------------------------------------------

    @Transactional
    public StockTransferDTO submitTransfer(Long id, String performedBy) {
        StockTransfer transfer = stockTransferRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Stock transfer not found with id: " + id));

        if (transfer.getStatus() != StockTransferStatus.DRAFT) {
            throw new IllegalStateException("Cannot submit transfer in status: " + transfer.getStatus() + ". Must be DRAFT.");
        }

        transfer.setStatus(StockTransferStatus.PENDING_APPROVAL);
        StockTransfer saved = stockTransferRepository.save(transfer);
        log.info("Transfer #{} [{}] submitted for approval by {}", saved.getId(), saved.getTransferNumber(), performedBy);
        return StockTransferDTO.fromEntity(saved);
    }

    @Transactional
    public StockTransferDTO approveTransfer(Long id, String approvedBy) {
        StockTransfer transfer = stockTransferRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Stock transfer not found with id: " + id));

        if (transfer.getStatus() != StockTransferStatus.PENDING_APPROVAL && transfer.getStatus() != StockTransferStatus.DRAFT) {
            throw new IllegalStateException("Cannot approve transfer in status: " + transfer.getStatus());
        }

        transfer.setStatus(StockTransferStatus.APPROVED);
        transfer.setApprovedBy(approvedBy != null && !approvedBy.isBlank() ? approvedBy.trim() : "admin");
        transfer.setApprovedAt(LocalDateTime.now());

        StockTransfer saved = stockTransferRepository.save(transfer);
        log.info("Transfer #{} [{}] approved by {}", saved.getId(), saved.getTransferNumber(), saved.getApprovedBy());
        return StockTransferDTO.fromEntity(saved);
    }

    @Transactional
    public StockTransferDTO dispatchTransfer(Long id, String dispatchedBy) {
        StockTransfer transfer = stockTransferRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Stock transfer not found with id: " + id));

        // Idempotency: If already DISPATCHED or beyond, reject with 409
        if (transfer.getStatus() == StockTransferStatus.DISPATCHED
                || transfer.getStatus() == StockTransferStatus.PARTIALLY_RECEIVED
                || transfer.getStatus() == StockTransferStatus.FULLY_RECEIVED) {
            throw new IllegalStateException("Transfer '" + transfer.getTransferNumber() + "' has already been dispatched.");
        }

        if (transfer.getStatus() != StockTransferStatus.APPROVED) {
            throw new IllegalStateException("Cannot dispatch transfer in status: " + transfer.getStatus() + ". Transfer must be APPROVED first.");
        }

        String operator = dispatchedBy != null && !dispatchedBy.isBlank() ? dispatchedBy.trim() : "admin";

        // Process line items: Lock and deduct stock from source location
        for (StockTransferItem item : transfer.getItems()) {
            WarehouseLocation srcLoc = locationRepository.findByIdWithLock(item.getSourceLocation().getId())
                    .orElseThrow(() -> new NoSuchElementException("Source location not found: " + item.getSourceLocation().getId()));

            BatchLocationStock srcStock = batchLocationStockRepository.findByBatchIdAndLocationIdWithLock(
                    item.getBatch().getId(), srcLoc.getId())
                    .orElseThrow(() -> new IllegalStateException("Source stock record not found for batch " + item.getBatch().getBatchNumber()));

            int reqQty = item.getRequestedQuantity();
            if (srcStock.getAvailableQuantity() < reqQty) {
                throw new IllegalStateException(String.format(
                        "Source location '%s' has insufficient available stock (%d) for requested dispatch quantity (%d)",
                        srcLoc.getLocationCode(), srcStock.getAvailableQuantity(), reqQty));
            }

            // Deduct stock from source location
            int stockBefore = srcStock.getQuantity();
            int stockAfter = stockBefore - reqQty;
            srcStock.setQuantity(stockAfter);
            batchLocationStockRepository.save(srcStock);

            item.setDispatchedQuantity(reqQty);

            // Create TRANSFER_OUT audit transaction
            InventoryTransaction tx = new InventoryTransaction();
            tx.setFertilizer(item.getFertilizer());
            tx.setBatch(item.getBatch());
            tx.setWarehouse(transfer.getSourceWarehouse());
            tx.setLocation(srcLoc);
            tx.setTransactionType(InventoryTransactionType.TRANSFER_OUT);
            tx.setQuantity(reqQty);
            tx.setStockBefore(stockBefore);
            tx.setStockAfter(stockAfter);
            tx.setReferenceType("STOCK_TRANSFER");
            tx.setReferenceId(transfer.getTransferNumber());
            tx.setReason(String.format("Transfer OUT to %s - Batch %s",
                    transfer.getDestinationWarehouse().getWarehouseCode(), item.getBatch().getBatchNumber()));
            tx.setPerformedBy(operator);
            transactionRepository.save(tx);
        }

        transfer.setStatus(StockTransferStatus.DISPATCHED);
        transfer.setDispatchedBy(operator);
        transfer.setDispatchedAt(LocalDateTime.now());

        StockTransfer saved = stockTransferRepository.save(transfer);
        log.info("Transfer #{} [{}] dispatched by {}", saved.getId(), saved.getTransferNumber(), operator);
        return StockTransferDTO.fromEntity(saved);
    }

    @Transactional
    public StockTransferDTO receiveTransfer(Long id, ReceiveStockTransferRequest req, String receivedBy) {
        StockTransfer transfer = stockTransferRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Stock transfer not found with id: " + id));

        // Idempotency: If already FULLY_RECEIVED, reject with 409
        if (transfer.getStatus() == StockTransferStatus.FULLY_RECEIVED) {
            throw new IllegalStateException("Transfer '" + transfer.getTransferNumber() + "' has already been fully received.");
        }

        if (transfer.getStatus() != StockTransferStatus.DISPATCHED && transfer.getStatus() != StockTransferStatus.PARTIALLY_RECEIVED) {
            throw new IllegalStateException("Cannot receive transfer in status: " + transfer.getStatus() + ". Transfer must be DISPATCHED or PARTIALLY_RECEIVED.");
        }

        String operator = receivedBy != null && !receivedBy.isBlank() ? receivedBy.trim() : "admin";

        // Map received quantities & locations if provided
        Map<Long, Integer> receiptMap = new HashMap<>();
        Map<Long, Long> destLocMap = new HashMap<>();
        if (req != null && req.getItemReceipts() != null && !req.getItemReceipts().isEmpty()) {
            for (ReceiveStockTransferRequest.ItemReceipt r : req.getItemReceipts()) {
                receiptMap.put(r.getItemId(), r.getReceivedQuantity());
                if (r.getDestinationLocationId() != null) {
                    destLocMap.put(r.getItemId(), r.getDestinationLocationId());
                }
            }
        }

        for (StockTransferItem item : transfer.getItems()) {
            int toReceive;
            if (receiptMap.containsKey(item.getId())) {
                toReceive = receiptMap.get(item.getId());
            } else {
                // Default: receive all remaining unreceived dispatched quantity
                toReceive = item.getRemainingToReceive();
            }

            if (toReceive < 0) {
                throw new IllegalArgumentException("Received quantity cannot be negative");
            }
            if (toReceive == 0) {
                continue;
            }
            if (toReceive > item.getRemainingToReceive()) {
                throw new IllegalArgumentException(String.format(
                        "Received quantity (%d) exceeds remaining unreceived dispatched quantity (%d) for item #%d",
                        toReceive, item.getRemainingToReceive(), item.getId()));
            }

            WarehouseLocation destLoc = null;
            if (destLocMap.containsKey(item.getId()) && destLocMap.get(item.getId()) != null) {
                destLoc = locationRepository.findByIdWithLock(destLocMap.get(item.getId()))
                        .orElse(null);
            }
            if (destLoc == null && item.getDestinationLocation() != null) {
                destLoc = locationRepository.findByIdWithLock(item.getDestinationLocation().getId())
                        .orElse(null);
            }
            if (destLoc == null) {
                destLoc = locationRepository.findByWarehouseIdAndStatus(transfer.getDestinationWarehouse().getId(), LocationStatus.ACTIVE)
                        .stream().findFirst().orElseThrow(() -> new IllegalStateException("No active destination location available"));
            }
            if (destLoc.getStatus() == LocationStatus.BLOCKED) {
                throw new IllegalStateException("Destination location '" + destLoc.getLocationCode() + "' is BLOCKED");
            }
            item.setDestinationLocation(destLoc);

            // Find or create destination BatchLocationStock
            Optional<BatchLocationStock> opt = batchLocationStockRepository.findByBatchIdAndLocationIdWithLock(
                    item.getBatch().getId(), destLoc.getId());
            BatchLocationStock destStock;
            if (opt.isPresent()) {
                destStock = opt.get();
            } else {
                destStock = new BatchLocationStock();
                destStock.setBatch(item.getBatch());
                destStock.setWarehouse(transfer.getDestinationWarehouse());
                destStock.setLocation(destLoc);
                destStock.setQuantity(0);
                destStock.setReservedQuantity(0);
                destStock = batchLocationStockRepository.save(destStock);
            }

            int stockBefore = destStock.getQuantity();
            int stockAfter = stockBefore + toReceive;
            destStock.setQuantity(stockAfter);
            batchLocationStockRepository.save(destStock);

            item.setReceivedQuantity(item.getReceivedQuantity() + toReceive);

            // Record TRANSFER_IN audit transaction
            InventoryTransaction tx = new InventoryTransaction();
            tx.setFertilizer(item.getFertilizer());
            tx.setBatch(item.getBatch());
            tx.setWarehouse(transfer.getDestinationWarehouse());
            tx.setLocation(destLoc);
            tx.setTransactionType(InventoryTransactionType.TRANSFER_IN);
            tx.setQuantity(toReceive);
            tx.setStockBefore(stockBefore);
            tx.setStockAfter(stockAfter);
            tx.setReferenceType("STOCK_TRANSFER");
            tx.setReferenceId(transfer.getTransferNumber());
            tx.setReason(String.format("Transfer IN from %s - Batch %s",
                    transfer.getSourceWarehouse().getWarehouseCode(), item.getBatch().getBatchNumber()));
            tx.setPerformedBy(operator);
            transactionRepository.save(tx);
        }

        // Determine if all items are fully received
        boolean allComplete = true;
        boolean anyReceived = false;
        for (StockTransferItem item : transfer.getItems()) {
            if (item.getReceivedQuantity() > 0) {
                anyReceived = true;
            }
            if (item.getReceivedQuantity() < item.getDispatchedQuantity()) {
                allComplete = false;
            }
        }

        if (allComplete) {
            transfer.setStatus(StockTransferStatus.FULLY_RECEIVED);
        } else if (anyReceived) {
            transfer.setStatus(StockTransferStatus.PARTIALLY_RECEIVED);
        }

        transfer.setReceivedBy(operator);
        transfer.setReceivedAt(LocalDateTime.now());
        if (req != null && req.getNotes() != null) {
            transfer.setNotes((transfer.getNotes() != null ? transfer.getNotes() + " | " : "") + req.getNotes().trim());
        }

        StockTransfer saved = stockTransferRepository.save(transfer);
        log.info("Transfer #{} [{}] received by {}: Status={}", saved.getId(), saved.getTransferNumber(), operator, saved.getStatus());
        return StockTransferDTO.fromEntity(saved);
    }

    @Transactional
    public StockTransferDTO rejectTransfer(Long id, String reason, String performedBy) {
        StockTransfer transfer = stockTransferRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Stock transfer not found with id: " + id));

        if (transfer.getStatus() != StockTransferStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Cannot reject transfer in status: " + transfer.getStatus() + ". Must be PENDING_APPROVAL.");
        }

        transfer.setStatus(StockTransferStatus.REJECTED);
        if (reason != null && !reason.isBlank()) {
            transfer.setNotes((transfer.getNotes() != null ? transfer.getNotes() + " | " : "") + "Rejected: " + reason.trim());
        }

        StockTransfer saved = stockTransferRepository.save(transfer);
        log.info("Transfer #{} [{}] rejected by {}", saved.getId(), saved.getTransferNumber(), performedBy);
        return StockTransferDTO.fromEntity(saved);
    }

    @Transactional
    public StockTransferDTO cancelTransfer(Long id, String reason, String performedBy) {
        StockTransfer transfer = stockTransferRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Stock transfer not found with id: " + id));

        if (transfer.getStatus() == StockTransferStatus.DISPATCHED
                || transfer.getStatus() == StockTransferStatus.PARTIALLY_RECEIVED
                || transfer.getStatus() == StockTransferStatus.FULLY_RECEIVED) {
            throw new IllegalStateException("Cannot cancel transfer in status: " + transfer.getStatus() + ". Stock has already been dispatched.");
        }
        if (transfer.getStatus() == StockTransferStatus.CANCELLED) {
            throw new IllegalStateException("Transfer is already cancelled");
        }

        transfer.setStatus(StockTransferStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            transfer.setNotes((transfer.getNotes() != null ? transfer.getNotes() + " | " : "") + "Cancelled: " + reason.trim());
        }

        StockTransfer saved = stockTransferRepository.save(transfer);
        log.info("Transfer #{} [{}] cancelled by {}", saved.getId(), saved.getTransferNumber(), performedBy);
        return StockTransferDTO.fromEntity(saved);
    }

    // ------------------------------------------------------------------------
    // Read Operations
    // ------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public StockTransferDTO getTransfer(Long id) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Stock transfer not found with id: " + id));
        return StockTransferDTO.fromEntity(transfer);
    }

    @Transactional(readOnly = true)
    public List<StockTransferDTO> getAllTransfers(String status, Long sourceWarehouseId, Long destinationWarehouseId) {
        List<StockTransfer> list = stockTransferRepository.findAll();

        return list.stream()
                .filter(st -> {
                    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
                        if (!st.getStatus().name().equalsIgnoreCase(status.trim())) {
                            return false;
                        }
                    }
                    if (sourceWarehouseId != null && !st.getSourceWarehouse().getId().equals(sourceWarehouseId)) {
                        return false;
                    }
                    if (destinationWarehouseId != null && !st.getDestinationWarehouse().getId().equals(destinationWarehouseId)) {
                        return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(StockTransfer::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(StockTransferDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
