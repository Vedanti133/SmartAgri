package com.smartagri.service;

import com.smartagri.dto.*;
import com.smartagri.entity.*;
import com.smartagri.enums.*;
import com.smartagri.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final ReturnRequestItemRepository returnRequestItemRepository;
    private final ReturnShipmentRepository returnShipmentRepository;
    private final ReturnTrackingEventRepository returnTrackingEventRepository;
    private final ReturnInspectionRepository returnInspectionRepository;
    private final RefundRepository refundRepository;
    private final ReplacementOrderRepository replacementOrderRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;
    private final FertilizerRepository fertilizerRepository;
    private final FertilizerBatchRepository fertilizerBatchRepository;
    private final BatchLocationStockRepository batchLocationStockRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final FulfillmentAllocationRepository fulfillmentAllocationRepository;
    private final OrderItemBatchRepository orderItemBatchRepository;
    private final SupportTicketRepository supportTicketRepository;

    public ReturnService(ReturnRequestRepository returnRequestRepository,
                         ReturnRequestItemRepository returnRequestItemRepository,
                         ReturnShipmentRepository returnShipmentRepository,
                         ReturnTrackingEventRepository returnTrackingEventRepository,
                         ReturnInspectionRepository returnInspectionRepository,
                         RefundRepository refundRepository,
                         ReplacementOrderRepository replacementOrderRepository,
                         OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         UserRepository userRepository,
                         DeliveryRepository deliveryRepository,
                         FertilizerRepository fertilizerRepository,
                         FertilizerBatchRepository fertilizerBatchRepository,
                         BatchLocationStockRepository batchLocationStockRepository,
                         WarehouseRepository warehouseRepository,
                         WarehouseLocationRepository warehouseLocationRepository,
                         InventoryTransactionRepository inventoryTransactionRepository,
                         FulfillmentAllocationRepository fulfillmentAllocationRepository,
                         OrderItemBatchRepository orderItemBatchRepository,
                         SupportTicketRepository supportTicketRepository) {
        this.returnRequestRepository = returnRequestRepository;
        this.returnRequestItemRepository = returnRequestItemRepository;
        this.returnShipmentRepository = returnShipmentRepository;
        this.returnTrackingEventRepository = returnTrackingEventRepository;
        this.returnInspectionRepository = returnInspectionRepository;
        this.refundRepository = refundRepository;
        this.replacementOrderRepository = replacementOrderRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.userRepository = userRepository;
        this.deliveryRepository = deliveryRepository;
        this.fertilizerRepository = fertilizerRepository;
        this.fertilizerBatchRepository = fertilizerBatchRepository;
        this.batchLocationStockRepository = batchLocationStockRepository;
        this.warehouseRepository = warehouseRepository;
        this.warehouseLocationRepository = warehouseLocationRepository;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.fulfillmentAllocationRepository = fulfillmentAllocationRepository;
        this.orderItemBatchRepository = orderItemBatchRepository;
        this.supportTicketRepository = supportTicketRepository;
    }

    public ReturnRequestDTO createReturn(Long customerId, CreateReturnRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + request.getOrderId()));

        // Verification 1: Order ownership
        if (!order.getUser().getId().equals(customerId) && !"ADMIN".equalsIgnoreCase(customer.getRole())) {
            throw new SecurityException("Access denied: You do not own this order");
        }

        // Verification 2: Order delivered check
        boolean isDelivered = (order.getOrderStatus() == OrderStatus.DELIVERED);
        Optional<Delivery> deliveryOpt = deliveryRepository.findByOrderId(order.getId());
        if (deliveryOpt.isPresent() && deliveryOpt.get().getDeliveryStatus() == DeliveryStatus.DELIVERED) {
            isDelivered = true;
        }
        if (!isDelivered) {
            throw new IllegalStateException("Return requests can only be initiated for DELIVERED orders");
        }

        // Verification 3: 14-day return window check
        LocalDateTime deliveredAt = deliveryOpt.map(Delivery::getDeliveredAt)
                .orElse(order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt());
        if (deliveredAt != null && deliveredAt.isBefore(LocalDateTime.now().minusDays(14))) {
            throw new IllegalStateException("Return window of 14 days has expired for this order");
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("At least one item must be selected for return");
        }

        // Create ReturnRequest
        ReturnRequest returnRequest = new ReturnRequest();
        returnRequest.setReturnNumber("RET-" + System.currentTimeMillis() + "-" + (int) (1000 + Math.random() * 9000));
        returnRequest.setOrder(order);
        returnRequest.setCustomer(customer);
        returnRequest.setReason(request.getReason() != null ? request.getReason() : ReturnReason.OTHER);
        returnRequest.setCustomerComments(request.getCustomerComments());
        returnRequest.setStatus(ReturnStatus.REQUESTED);
        returnRequest.setRequestedAt(LocalDateTime.now());
        returnRequest = returnRequestRepository.save(returnRequest);

        // Fetch allocations to trace original batch and location lineage
        List<FulfillmentAllocation> allocations = fulfillmentAllocationRepository.findByOrderId(order.getId());

        // Process items
        for (CreateReturnItemRequest itemReq : request.getItems()) {
            OrderItem orderItem = orderItemRepository.findById(itemReq.getOrderItemId())
                    .orElseThrow(() -> new IllegalArgumentException("Order item not found: " + itemReq.getOrderItemId()));

            if (!orderItem.getOrder().getId().equals(order.getId())) {
                throw new IllegalArgumentException("Order item " + itemReq.getOrderItemId() + " does not belong to order " + order.getId());
            }

            Fertilizer fertilizer = orderItem.getFertilizer();
            if (fertilizer != null && fertilizer.getReturnPolicy() == ProductReturnPolicy.NON_RETURNABLE) {
                throw new IllegalStateException("Product '" + fertilizer.getName() + "' is non-returnable");
            }

            // Unreturned quantity check
            Integer alreadyRequested = returnRequestItemRepository.sumRequestedQuantityByOrderItemIdAndStatusNotIn(
                    orderItem.getId(), List.of(ReturnStatus.REJECTED, ReturnStatus.CANCELLED));
            int availableToReturn = orderItem.getQuantity() - (alreadyRequested != null ? alreadyRequested : 0);

            if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0 || itemReq.getQuantity() > availableToReturn) {
                throw new IllegalArgumentException("Invalid return quantity for item " + orderItem.getProductName() +
                        ": requested " + itemReq.getQuantity() + ", max available to return: " + availableToReturn);
            }

            ReturnRequestItem retItem = new ReturnRequestItem();
            retItem.setReturnRequest(returnRequest);
            retItem.setOrderItem(orderItem);
            retItem.setFertilizer(fertilizer);
            retItem.setRequestedQuantity(itemReq.getQuantity());
            retItem.setItemCondition(itemReq.getCondition() != null ? itemReq.getCondition() : ItemCondition.OPENED);
            retItem.setResolution(itemReq.getResolution() != null ? itemReq.getResolution() : ReturnResolution.REFUND);
            retItem.setReason(itemReq.getReason() != null ? itemReq.getReason() : request.getCustomerComments());

            // Trace batch and location lineage
            FulfillmentAllocation matchedAlloc = allocations.stream()
                    .filter(a -> a.getOrderItem() != null && a.getOrderItem().getId().equals(orderItem.getId()))
                    .findFirst().orElse(null);

            if (matchedAlloc != null) {
                if (matchedAlloc.getBatch() != null) retItem.setBatchId(matchedAlloc.getBatch().getId());
                if (matchedAlloc.getWarehouse() != null) retItem.setWarehouseId(matchedAlloc.getWarehouse().getId());
                if (matchedAlloc.getLocation() != null) retItem.setLocationId(matchedAlloc.getLocation().getId());
            } else {
                List<OrderItemBatch> batches = orderItemBatchRepository.findByOrderItemId(orderItem.getId());
                if (!batches.isEmpty() && batches.get(0).getBatch() != null) {
                    retItem.setBatchId(batches.get(0).getBatch().getId());
                }
            }

            returnRequest.addItem(retItem);
            returnRequestItemRepository.save(retItem);
        }

        // Create reverse logistics shipment
        ReturnShipment shipment = new ReturnShipment();
        shipment.setReturnRequest(returnRequest);
        shipment.setShipmentNumber("RET-SHP-" + System.currentTimeMillis());
        shipment.setTrackingNumber("RET-TRK-" + System.currentTimeMillis());
        shipment.setStatus(ReturnShipmentStatus.CREATED);

        String pickupAddr = request.getPickupAddress();
        if (pickupAddr == null || pickupAddr.trim().isEmpty()) {
            pickupAddr = order.getAddressLine1() + ", " + order.getCity() + ", " + order.getState() + " - " + order.getPincode();
        }
        shipment.setPickupAddress(pickupAddr);
        shipment = returnShipmentRepository.save(shipment);
        returnRequest.setShipment(shipment);

        // Add initial tracking event
        ReturnTrackingEvent event = new ReturnTrackingEvent();
        event.setReturnShipment(shipment);
        event.setStatus("CREATED");
        event.setEventCode("RETURN_REQUESTED");
        event.setDescription("Return request submitted by customer");
        event.setLocation("Customer Location");
        event.setEventTime(LocalDateTime.now());
        event.setCreatedBy(returnRequest.getCustomer() != null ? returnRequest.getCustomer().getUsername() : "Customer");
        returnTrackingEventRepository.save(event);
        shipment.getEvents().add(event);

        return mapToDTO(returnRequestRepository.save(returnRequest));
    }

    public ReturnRequestDTO approveReturn(Long returnId, ApproveReturnRequest request) {
        ReturnRequest ret = returnRequestRepository.findByIdForUpdate(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnId));

        if (ret.getStatus() != ReturnStatus.REQUESTED && ret.getStatus() != ReturnStatus.UNDER_REVIEW) {
            throw new IllegalStateException("Return cannot be approved from status: " + ret.getStatus());
        }

        ret.setStatus(ReturnStatus.APPROVED);
        ret.setApprovedAt(LocalDateTime.now());
        if (request != null && request.getAdminNotes() != null) {
            ret.setAdminNotes(request.getAdminNotes());
        }

        // Set approved quantities
        for (ReturnRequestItem item : ret.getItems()) {
            if (request != null && request.getApprovedQuantities() != null && request.getApprovedQuantities().containsKey(item.getId())) {
                item.setApprovedQuantity(request.getApprovedQuantities().get(item.getId()));
            } else {
                item.setApprovedQuantity(item.getRequestedQuantity());
            }
            returnRequestItemRepository.save(item);
        }

        if (request != null && request.getDestinationWarehouseId() != null && ret.getShipment() != null) {
            warehouseRepository.findById(request.getDestinationWarehouseId()).ifPresent(wh -> {
                ret.getShipment().setDestinationWarehouse(wh);
                returnShipmentRepository.save(ret.getShipment());
            });
        }

        return mapToDTO(returnRequestRepository.save(ret));
    }

    public ReturnRequestDTO rejectReturn(Long returnId, RejectReturnRequest request) {
        ReturnRequest ret = returnRequestRepository.findByIdForUpdate(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnId));

        if (ret.getStatus().isTerminal()) {
            throw new IllegalStateException("Cannot reject return already in terminal status: " + ret.getStatus());
        }

        ret.setStatus(ReturnStatus.REJECTED);
        ret.setRejectedAt(LocalDateTime.now());
        if (request != null) {
            ret.setRejectionReason(request.getRejectionReason());
            ret.setAdminNotes(request.getAdminNotes());
        }

        if (ret.getShipment() != null) {
            ret.getShipment().setStatus(ReturnShipmentStatus.CANCELLED);
            returnShipmentRepository.save(ret.getShipment());
        }

        return mapToDTO(returnRequestRepository.save(ret));
    }

    public ReturnRequestDTO cancelReturn(Long customerId, Long returnId) {
        ReturnRequest ret = returnRequestRepository.findByIdForUpdate(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnId));

        if (!ret.getCustomer().getId().equals(customerId)) {
            throw new SecurityException("Access denied: You do not own this return request");
        }

        if (!ret.getStatus().canCancel()) {
            throw new IllegalStateException("Return request cannot be cancelled at status: " + ret.getStatus());
        }

        ret.setStatus(ReturnStatus.CANCELLED);
        if (ret.getShipment() != null) {
            ret.getShipment().setStatus(ReturnShipmentStatus.CANCELLED);
            returnShipmentRepository.save(ret.getShipment());
        }

        return mapToDTO(returnRequestRepository.save(ret));
    }

    public ReturnRequestDTO schedulePickup(Long returnId, ScheduleReturnPickupRequest request) {
        ReturnRequest ret = returnRequestRepository.findByIdForUpdate(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnId));

        if (ret.getStatus() != ReturnStatus.APPROVED && ret.getStatus() != ReturnStatus.FAILED_PICKUP && ret.getStatus() != ReturnStatus.PICKUP_SCHEDULED) {
            throw new IllegalStateException("Cannot schedule pickup for return at status: " + ret.getStatus());
        }

        ReturnShipment shipment = ret.getShipment();
        if (shipment == null) {
            shipment = new ReturnShipment();
            shipment.setReturnRequest(ret);
            shipment.setShipmentNumber("RET-SHP-" + System.currentTimeMillis());
            shipment.setTrackingNumber("RET-TRK-" + System.currentTimeMillis());
        }

        if (request.getCarrier() != null) shipment.setCarrier(request.getCarrier());
        if (request.getTrackingNumber() != null && !request.getTrackingNumber().trim().isEmpty()) {
            shipment.setTrackingNumber(request.getTrackingNumber());
        }
        if (request.getPickupAddress() != null && !request.getPickupAddress().trim().isEmpty()) {
            shipment.setPickupAddress(request.getPickupAddress());
        }
        shipment.setPickupScheduledAt(request.getPickupScheduledAt() != null ? request.getPickupScheduledAt() : LocalDateTime.now().plusDays(1));
        shipment.setStatus(ReturnShipmentStatus.PICKUP_SCHEDULED);

        if (request.getDestinationWarehouseId() != null) {
            warehouseRepository.findById(request.getDestinationWarehouseId()).ifPresent(shipment::setDestinationWarehouse);
        }

        shipment = returnShipmentRepository.save(shipment);
        ret.setShipment(shipment);
        ret.setStatus(ReturnStatus.PICKUP_SCHEDULED);

        ReturnTrackingEvent event = new ReturnTrackingEvent();
        event.setReturnShipment(shipment);
        event.setStatus("PICKUP_SCHEDULED");
        event.setEventCode("PICKUP_SCHEDULED");
        event.setDescription("Reverse logistics pickup scheduled with carrier: " + shipment.getCarrier());
        event.setLocation(shipment.getPickupAddress());
        event.setEventTime(LocalDateTime.now());
        event.setCreatedBy("Admin");
        returnTrackingEventRepository.save(event);
        shipment.getEvents().add(event);

        return mapToDTO(returnRequestRepository.save(ret));
    }

    public ReturnRequestDTO updateShipmentStatus(Long returnId, ReturnShipmentStatus shipmentStatus, String location, String notes) {
        ReturnRequest ret = returnRequestRepository.findByIdForUpdate(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnId));

        ReturnShipment shipment = ret.getShipment();
        if (shipment == null) {
            throw new IllegalStateException("Shipment not initialized for return: " + returnId);
        }

        shipment.setStatus(shipmentStatus);
        if (shipmentStatus == ReturnShipmentStatus.PICKED_UP) {
            shipment.setPickedUpAt(LocalDateTime.now());
            ret.setStatus(ReturnStatus.PICKED_UP);
        } else if (shipmentStatus == ReturnShipmentStatus.IN_TRANSIT) {
            ret.setStatus(ReturnStatus.IN_TRANSIT);
        } else if (shipmentStatus == ReturnShipmentStatus.RECEIVED) {
            shipment.setReceivedAt(LocalDateTime.now());
            ret.setStatus(ReturnStatus.RECEIVED);
            ret.setReceivedAt(LocalDateTime.now());
        } else if (shipmentStatus == ReturnShipmentStatus.CANCELLED) {
            ret.setStatus(ReturnStatus.CANCELLED);
        }

        returnShipmentRepository.save(shipment);

        ReturnTrackingEvent event = new ReturnTrackingEvent();
        event.setReturnShipment(shipment);
        event.setStatus(shipmentStatus.name());
        event.setEventCode(shipmentStatus.name());
        event.setDescription(notes != null ? notes : "Shipment status updated to " + shipmentStatus);
        event.setLocation(location != null ? location : "In Transit Hub");
        event.setEventTime(LocalDateTime.now());
        event.setCreatedBy("Carrier/Admin");
        returnTrackingEventRepository.save(event);
        shipment.getEvents().add(event);

        return mapToDTO(returnRequestRepository.save(ret));
    }

    public ReturnRequestDTO receiveReturn(Long returnId, ReceiveReturnRequest request) {
        ReturnRequest ret = returnRequestRepository.findByIdForUpdate(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnId));

        ret.setStatus(ReturnStatus.RECEIVED);
        ret.setReceivedAt(LocalDateTime.now());

        ReturnShipment shipment = ret.getShipment();
        if (shipment != null) {
            shipment.setStatus(ReturnShipmentStatus.RECEIVED);
            shipment.setReceivedAt(LocalDateTime.now());
            if (request != null && request.getWarehouseId() != null) {
                warehouseRepository.findById(request.getWarehouseId()).ifPresent(shipment::setDestinationWarehouse);
            }
            returnShipmentRepository.save(shipment);

            ReturnTrackingEvent event = new ReturnTrackingEvent();
            event.setReturnShipment(shipment);
            event.setStatus("RECEIVED");
            event.setEventCode("PACKAGE_RECEIVED");
            event.setDescription("Return package received at warehouse" + (request != null && request.getNotes() != null ? ": " + request.getNotes() : ""));
            event.setLocation(shipment.getDestinationWarehouse() != null ? shipment.getDestinationWarehouse().getWarehouseName() : "Warehouse");
            event.setEventTime(LocalDateTime.now());
            event.setCreatedBy("Warehouse Staff");
            returnTrackingEventRepository.save(event);
            shipment.getEvents().add(event);
        }

        // Set received quantities on items
        for (ReturnRequestItem item : ret.getItems()) {
            if (request != null && request.getReceivedQuantities() != null && request.getReceivedQuantities().containsKey(item.getId())) {
                item.setReceivedQuantity(request.getReceivedQuantities().get(item.getId()));
            } else {
                item.setReceivedQuantity(item.getApprovedQuantity() > 0 ? item.getApprovedQuantity() : item.getRequestedQuantity());
            }
            returnRequestItemRepository.save(item);
        }

        return mapToDTO(returnRequestRepository.save(ret));
    }

    public ReturnRequestDTO inspectReturn(Long returnId, InspectReturnRequest request, String inspectedBy) {
        ReturnRequest ret = returnRequestRepository.findByIdForUpdate(returnId)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnId));

        if (ret.getStatus() != ReturnStatus.RECEIVED && ret.getStatus() != ReturnStatus.INSPECTING && ret.getStatus() != ReturnStatus.INSPECTION_COMPLETED) {
            throw new IllegalStateException("Return cannot be inspected from status: " + ret.getStatus());
        }

        ReturnInspection inspection = ret.getInspection();
        if (inspection == null) {
            inspection = new ReturnInspection();
            inspection.setReturnRequest(ret);
        }

        inspection.setInspectedBy(inspectedBy != null ? inspectedBy : (request.getInspectedBy() != null ? request.getInspectedBy() : "QC_INSPECTOR"));
        inspection.setInspectionDate(LocalDateTime.now());
        inspection.setItemCondition(request.getItemCondition() != null ? request.getItemCondition() : ItemCondition.UNOPENED);
        inspection.setResult(request.getResult() != null ? request.getResult() : InspectionResult.ACCEPTED);
        inspection.setInspectionNotes(request.getInspectionNotes());

        if (request.getQuarantineLocationId() != null) {
            warehouseLocationRepository.findById(request.getQuarantineLocationId()).ifPresent(inspection::setQuarantineLocation);
        }

        int totalAccepted = 0;
        int totalRejected = 0;
        int totalQuarantined = 0;

        for (ReturnRequestItem item : ret.getItems()) {
            int acc = 0;
            int rej = 0;

            if (request.getAcceptedQuantities() != null && request.getAcceptedQuantities().containsKey(item.getId())) {
                acc = request.getAcceptedQuantities().get(item.getId());
                rej = request.getRejectedQuantities() != null ? request.getRejectedQuantities().getOrDefault(item.getId(), 0) : 0;
            } else {
                int baseQty = item.getReceivedQuantity() > 0 ? item.getReceivedQuantity() : item.getApprovedQuantity();
                if (request.getResult() == InspectionResult.ACCEPTED) {
                    acc = baseQty;
                    rej = 0;
                } else if (request.getResult() == InspectionResult.REJECTED || request.getResult() == InspectionResult.QUARANTINED) {
                    acc = 0;
                    rej = baseQty;
                } else {
                    acc = request.getAcceptedQuantity() != null ? request.getAcceptedQuantity() : baseQty;
                    rej = request.getRejectedQuantity() != null ? request.getRejectedQuantity() : 0;
                }
            }

            item.setAcceptedQuantity(acc);
            item.setRejectedQuantity(rej);
            item.setItemCondition(request.getItemCondition() != null ? request.getItemCondition() : ItemCondition.UNOPENED);
            returnRequestItemRepository.save(item);

            totalAccepted += acc;
            totalRejected += rej;
            if (request.getResult() == InspectionResult.QUARANTINED) {
                totalQuarantined += rej;
            }

            // RESTORE STOCK ONLY FOR ACCEPTED ITEMS (NEVER for rejected or quarantined)
            if (acc > 0 && request.getResult() != InspectionResult.REJECTED && request.getResult() != InspectionResult.QUARANTINED) {
                restoreItemStock(ret, item, acc, inspection.getInspectedBy());
            }
        }

        inspection.setAcceptedQuantity(totalAccepted);
        inspection.setRejectedQuantity(totalRejected);
        inspection.setQuarantinedQuantity(totalQuarantined);
        inspection = returnInspectionRepository.save(inspection);
        ret.setInspection(inspection);

        // Check if any replacement requested
        boolean hasReplacement = ret.getItems().stream().anyMatch(i -> i.getResolution() == ReturnResolution.REPLACEMENT);

        if (hasReplacement && totalAccepted > 0) {
            // Process Replacement Order
            createReplacementWorkflow(ret);
        } else if (request.getResult() == InspectionResult.REJECTED && totalAccepted == 0) {
            ret.setStatus(ReturnStatus.REJECTED);
            ret.setCompletedAt(LocalDateTime.now());
        } else {
            // Process Refund
            ret.setStatus(ReturnStatus.REFUND_PENDING);
            createOrUpdateRefund(ret, totalAccepted);
        }

        return mapToDTO(returnRequestRepository.save(ret));
    }

    private void restoreItemStock(ReturnRequest returnRequest, ReturnRequestItem item, int quantityToRestore, String performedBy) {
        Fertilizer fertilizer = fertilizerRepository.findByIdWithLock(item.getFertilizer().getId())
                .orElse(item.getFertilizer());

        int stockBefore = fertilizer.getStock();
        int stockAfter = stockBefore + quantityToRestore;
        fertilizer.setStock(stockAfter);
        fertilizerRepository.save(fertilizer);

        FertilizerBatch batch = null;
        if (item.getBatchId() != null) {
            Optional<FertilizerBatch> bOpt = fertilizerBatchRepository.findByIdWithLock(item.getBatchId());
            if (bOpt.isPresent()) {
                batch = bOpt.get();
                batch.setCurrentQuantity(batch.getCurrentQuantity() + quantityToRestore);
                fertilizerBatchRepository.save(batch);

                // Update location stock to keep FertilizerBatch.currentQuantity == SUM(BatchLocationStock.quantity)
                if (item.getLocationId() != null) {
                    Optional<BatchLocationStock> blsOpt = batchLocationStockRepository.findByBatchIdAndLocationIdWithLock(
                            batch.getId(), item.getLocationId());
                    if (blsOpt.isPresent()) {
                        BatchLocationStock bls = blsOpt.get();
                        bls.setQuantity(bls.getQuantity() + quantityToRestore);
                        batchLocationStockRepository.save(bls);
                    } else {
                        // Create location stock record
                        WarehouseLocation loc = warehouseLocationRepository.findById(item.getLocationId()).orElse(null);
                        Warehouse wh = item.getWarehouseId() != null ? warehouseRepository.findById(item.getWarehouseId()).orElse(null) : (loc != null ? loc.getWarehouse() : null);
                        if (loc != null && wh != null) {
                            BatchLocationStock newBls = new BatchLocationStock();
                            newBls.setBatch(batch);
                            newBls.setWarehouse(wh);
                            newBls.setLocation(loc);
                            newBls.setQuantity(quantityToRestore);
                            batchLocationStockRepository.save(newBls);
                        }
                    }
                }
            }
        }

        // Audit InventoryTransaction
        Warehouse wh = item.getWarehouseId() != null ? warehouseRepository.findById(item.getWarehouseId()).orElse(null) : null;
        WarehouseLocation loc = item.getLocationId() != null ? warehouseLocationRepository.findById(item.getLocationId()).orElse(null) : null;

        InventoryTransaction tx = new InventoryTransaction();
        tx.setFertilizer(fertilizer);
        tx.setBatch(batch);
        tx.setWarehouse(wh);
        tx.setLocation(loc);
        tx.setTransactionType(InventoryTransactionType.RETURN_IN);
        tx.setStockBefore(stockBefore);
        tx.setStockAfter(stockAfter);
        tx.setQuantity(quantityToRestore);
        tx.setReferenceType("RETURN");
        tx.setReferenceId(returnRequest.getReturnNumber());
        tx.setReason("Customer return inspection accepted: " + returnRequest.getReturnNumber());
        tx.setPerformedBy(performedBy != null ? performedBy : "QC_INSPECTOR");
        tx.setCreatedAt(LocalDateTime.now());
        inventoryTransactionRepository.save(tx);
    }

    private void createReplacementWorkflow(ReturnRequest ret) {
        Order originalOrder = ret.getOrder();

        // Create 0-surcharge replacement order
        Order replacementOrder = new Order();
        replacementOrder.setOrderNumber("ORD-REPL-" + System.currentTimeMillis() + "-" + (int) (1000 + Math.random() * 9000));
        replacementOrder.setUser(ret.getCustomer());
        replacementOrder.setDeliveryAddress(originalOrder.getDeliveryAddress());
        replacementOrder.setRecipientName(originalOrder.getRecipientName());
        replacementOrder.setRecipientPhone(originalOrder.getRecipientPhone());
        replacementOrder.setAddressLine1(originalOrder.getAddressLine1());
        replacementOrder.setAddressLine2(originalOrder.getAddressLine2());
        replacementOrder.setCity(originalOrder.getCity());
        replacementOrder.setDistrict(originalOrder.getDistrict());
        replacementOrder.setState(originalOrder.getState());
        replacementOrder.setPincode(originalOrder.getPincode());
        replacementOrder.setOrderStatus(OrderStatus.CONFIRMED);
        replacementOrder.setPaymentStatus(PaymentStatus.PAID);
        replacementOrder.setPaymentMethod("REPLACEMENT");
        replacementOrder.setSubtotal(BigDecimal.ZERO);
        replacementOrder.setGrandTotal(BigDecimal.ZERO);
        replacementOrder.setNotes("Replacement order for return: " + ret.getReturnNumber());
        replacementOrder = orderRepository.save(replacementOrder);

        for (ReturnRequestItem item : ret.getItems()) {
            if (item.getResolution() == ReturnResolution.REPLACEMENT && item.getAcceptedQuantity() > 0) {
                OrderItem replItem = new OrderItem();
                replItem.setOrder(replacementOrder);
                replItem.setFertilizer(item.getFertilizer());
                replItem.setProductName(item.getFertilizer() != null ? item.getFertilizer().getName() : item.getOrderItem().getProductName());
                replItem.setSku(item.getFertilizer() != null ? item.getFertilizer().getSku() : item.getOrderItem().getSku());
                replItem.setQuantity(item.getAcceptedQuantity());
                replItem.setUnitPrice(BigDecimal.ZERO);
                replItem.setLineTotal(BigDecimal.ZERO);
                orderItemRepository.save(replItem);
                replacementOrder.getItems().add(replItem);
            }
        }

        ReplacementOrder ro = new ReplacementOrder();
        ro.setReturnRequest(ret);
        ro.setOriginalOrder(originalOrder);
        ro.setReplacementOrder(replacementOrder);
        ro.setStatus("CONFIRMED");
        ro.setNotes("Replacement order generated: " + replacementOrder.getOrderNumber());
        replacementOrderRepository.save(ro);

        ret.setStatus(ReturnStatus.REPLACED);
        ret.setCompletedAt(LocalDateTime.now());
    }

    private void createOrUpdateRefund(ReturnRequest ret, int totalAccepted) {
        Order order = ret.getOrder();

        // Calculate refundable amount accurately using lineTotal / orderedQty * acceptedQty
        BigDecimal refundableSum = BigDecimal.ZERO;
        for (ReturnRequestItem item : ret.getItems()) {
            if (item.getAcceptedQuantity() > 0 && item.getResolution() == ReturnResolution.REFUND) {
                OrderItem oi = item.getOrderItem();
                if (oi.getQuantity() > 0) {
                    BigDecimal itemShare = oi.getLineTotal()
                            .multiply(BigDecimal.valueOf(item.getAcceptedQuantity()))
                            .divide(BigDecimal.valueOf(oi.getQuantity()), 2, RoundingMode.HALF_UP);
                    refundableSum = refundableSum.add(itemShare);
                }
            }
        }

        // Cap at order grand total
        if (refundableSum.compareTo(order.getGrandTotal()) > 0) {
            refundableSum = order.getGrandTotal();
        }

        Refund refund = ret.getRefund();
        if (refund == null) {
            refund = new Refund();
            refund.setRefundNumber("REF-" + System.currentTimeMillis() + "-" + (int) (1000 + Math.random() * 9000));
            refund.setReturnRequest(ret);
            refund.setOrder(order);
        }

        refund.setAmount(refundableSum);
        refund.setReason("Return approved: " + ret.getReturnNumber());
        refund.setPaymentMethod(order.getPaymentMethod());

        // COD refund check: if COD and no bank details, mark as PENDING_CUSTOMER_REFUND_DETAILS
        if ("COD".equalsIgnoreCase(order.getPaymentMethod()) && (refund.getBankAccountDetails() == null || refund.getBankAccountDetails().trim().isEmpty())) {
            refund.setStatus(RefundStatus.PENDING_CUSTOMER_REFUND_DETAILS);
        } else {
            refund.setStatus(RefundStatus.PENDING);
        }

        refund = refundRepository.save(refund);
        ret.setRefund(refund);
    }

    @Transactional(readOnly = true)
    public ReturnRequestDTO getReturnById(Long id) {
        ReturnRequest ret = returnRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + id));
        return mapToDTO(ret);
    }

    @Transactional(readOnly = true)
    public ReturnRequestDTO getReturnByNumber(String returnNumber) {
        ReturnRequest ret = returnRequestRepository.findByReturnNumber(returnNumber)
                .orElseThrow(() -> new IllegalArgumentException("Return request not found: " + returnNumber));
        return mapToDTO(ret);
    }

    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getCustomerReturns(Long customerId) {
        return returnRequestRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReturnRequestDTO> getAllReturns(ReturnStatus status) {
        List<ReturnRequest> list = (status != null)
                ? returnRequestRepository.findByStatusOrderByCreatedAtDesc(status)
                : returnRequestRepository.findAllByOrderByCreatedAtDesc();
        return list.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReturnDashboardDTO getDashboard() {
        ReturnDashboardDTO dto = new ReturnDashboardDTO();
        dto.setTotalReturns(returnRequestRepository.count());
        dto.setPendingReturns(returnRequestRepository.countByStatus(ReturnStatus.REQUESTED)
                + returnRequestRepository.countByStatus(ReturnStatus.UNDER_REVIEW));
        dto.setApprovedReturns(returnRequestRepository.countByStatus(ReturnStatus.APPROVED)
                + returnRequestRepository.countByStatus(ReturnStatus.PICKUP_SCHEDULED)
                + returnRequestRepository.countByStatus(ReturnStatus.PICKED_UP)
                + returnRequestRepository.countByStatus(ReturnStatus.IN_TRANSIT));
        dto.setInInspection(returnRequestRepository.countByStatus(ReturnStatus.RECEIVED)
                + returnRequestRepository.countByStatus(ReturnStatus.INSPECTING));
        dto.setCompletedReturns(returnRequestRepository.countByStatus(ReturnStatus.REFUNDED)
                + returnRequestRepository.countByStatus(ReturnStatus.REPLACED));
        dto.setRejectedReturns(returnRequestRepository.countByStatus(ReturnStatus.REJECTED));

        BigDecimal processedRefunds = refundRepository.sumAmountByOrderIdAndStatusIn(null, List.of(RefundStatus.PROCESSED));
        dto.setTotalRefundedAmount(processedRefunds != null ? processedRefunds : BigDecimal.ZERO);

        dto.setOpenTicketsCount(supportTicketRepository.countByStatus(TicketStatus.OPEN)
                + supportTicketRepository.countByStatus(TicketStatus.IN_PROGRESS));

        List<ReturnRequestDTO> recentReturns = returnRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream().limit(10).map(this::mapToDTO).collect(Collectors.toList());
        dto.setRecentReturns(recentReturns);

        return dto;
    }

    public ReturnRequestDTO mapToDTO(ReturnRequest ret) {
        ReturnRequestDTO dto = new ReturnRequestDTO();
        dto.setId(ret.getId());
        dto.setReturnNumber(ret.getReturnNumber());
        if (ret.getOrder() != null) {
            dto.setOrderId(ret.getOrder().getId());
            dto.setOrderTrackingNumber(ret.getOrder().getOrderNumber());
        }
        if (ret.getCustomer() != null) {
            dto.setCustomerId(ret.getCustomer().getId());
            dto.setCustomerName(ret.getCustomer().getName());
            dto.setCustomerEmail(ret.getCustomer().getEmail());
        }
        dto.setStatus(ret.getStatus());
        dto.setReason(ret.getReason());
        dto.setCustomerComments(ret.getCustomerComments());
        dto.setAdminNotes(ret.getAdminNotes());
        dto.setRejectionReason(ret.getRejectionReason());
        dto.setRequestedAt(ret.getRequestedAt());
        dto.setApprovedAt(ret.getApprovedAt());
        dto.setRejectedAt(ret.getRejectedAt());
        dto.setReceivedAt(ret.getReceivedAt());
        dto.setCompletedAt(ret.getCompletedAt());
        dto.setCreatedAt(ret.getCreatedAt());
        dto.setUpdatedAt(ret.getUpdatedAt());

        // Items
        List<ReturnRequestItem> items = ret.getItems() != null ? ret.getItems() : returnRequestItemRepository.findByReturnRequestId(ret.getId());
        dto.setItems(items.stream().map(i -> {
            ReturnRequestItemDTO iDto = new ReturnRequestItemDTO();
            iDto.setId(i.getId());
            if (i.getOrderItem() != null) iDto.setOrderItemId(i.getOrderItem().getId());
            if (i.getFertilizer() != null) {
                iDto.setFertilizerId(i.getFertilizer().getId());
                iDto.setFertilizerName(i.getFertilizer().getName());
                iDto.setFertilizerImageUrl(i.getFertilizer().getImageUrl());
            }
            iDto.setBatchId(i.getBatchId());
            iDto.setWarehouseId(i.getWarehouseId());
            iDto.setLocationId(i.getLocationId());
            iDto.setRequestedQuantity(i.getRequestedQuantity());
            iDto.setApprovedQuantity(i.getApprovedQuantity());
            iDto.setReceivedQuantity(i.getReceivedQuantity());
            iDto.setAcceptedQuantity(i.getAcceptedQuantity());
            iDto.setRejectedQuantity(i.getRejectedQuantity());
            iDto.setItemCondition(i.getItemCondition());
            iDto.setResolution(i.getResolution());
            iDto.setReason(i.getReason());
            return iDto;
        }).collect(Collectors.toList()));

        // Shipment
        ReturnShipment shp = ret.getShipment();
        if (shp == null && ret.getId() != null) {
            shp = returnShipmentRepository.findByReturnRequestId(ret.getId()).orElse(null);
        }
        if (shp != null) {
            ReturnShipmentDTO sDto = new ReturnShipmentDTO();
            sDto.setId(shp.getId());
            sDto.setReturnRequestId(ret.getId());
            sDto.setShipmentNumber(shp.getShipmentNumber());
            sDto.setTrackingNumber(shp.getTrackingNumber());
            sDto.setCarrier(shp.getCarrier());
            sDto.setPickupAddress(shp.getPickupAddress());
            if (shp.getDestinationWarehouse() != null) {
                sDto.setDestinationWarehouseId(shp.getDestinationWarehouse().getId());
                sDto.setDestinationWarehouseName(shp.getDestinationWarehouse().getWarehouseName());
            }
            sDto.setStatus(shp.getStatus());
            sDto.setPickupScheduledAt(shp.getPickupScheduledAt());
            sDto.setPickedUpAt(shp.getPickedUpAt());
            sDto.setReceivedAt(shp.getReceivedAt());
            sDto.setNotes(shp.getNotes());

            List<ReturnTrackingEvent> events = (shp.getEvents() != null && !shp.getEvents().isEmpty())
                    ? shp.getEvents()
                    : returnTrackingEventRepository.findByReturnShipmentIdOrderByEventTimeAsc(shp.getId());

            sDto.setTrackingEvents(events.stream().map(e -> {
                ReturnTrackingEventDTO eDto = new ReturnTrackingEventDTO();
                eDto.setId(e.getId());
                eDto.setStatus(e.getStatus());
                eDto.setEventCode(e.getEventCode());
                eDto.setLocation(e.getLocation());
                eDto.setDescription(e.getDescription());
                eDto.setEventTime(e.getEventTime());
                return eDto;
            }).collect(Collectors.toList()));

            dto.setShipment(sDto);
        }

        // Inspection
        ReturnInspection insp = ret.getInspection();
        if (insp == null && ret.getId() != null) {
            insp = returnInspectionRepository.findByReturnRequestId(ret.getId()).orElse(null);
        }
        if (insp != null) {
            ReturnInspectionDTO iDto = new ReturnInspectionDTO();
            iDto.setId(insp.getId());
            iDto.setReturnRequestId(ret.getId());
            iDto.setInspectedBy(insp.getInspectedBy());
            iDto.setInspectionDate(insp.getInspectionDate());
            iDto.setItemCondition(insp.getItemCondition());
            iDto.setAcceptedQuantity(insp.getAcceptedQuantity());
            iDto.setRejectedQuantity(insp.getRejectedQuantity());
            iDto.setQuarantinedQuantity(insp.getQuarantinedQuantity());
            iDto.setInspectionNotes(insp.getInspectionNotes());
            iDto.setResult(insp.getResult());
            if (insp.getQuarantineLocation() != null) {
                iDto.setQuarantineLocationId(insp.getQuarantineLocation().getId());
                iDto.setQuarantineLocationCode(insp.getQuarantineLocation().getLocationCode());
            }
            dto.setInspection(iDto);
        }

        // Refund
        Refund ref = ret.getRefund();
        if (ref == null && ret.getId() != null) {
            ref = refundRepository.findByReturnRequestId(ret.getId()).orElse(null);
        }
        if (ref != null) {
            RefundDTO rDto = new RefundDTO();
            rDto.setId(ref.getId());
            rDto.setRefundNumber(ref.getRefundNumber());
            rDto.setReturnRequestId(ret.getId());
            if (ref.getOrder() != null) rDto.setOrderId(ref.getOrder().getId());
            rDto.setAmount(ref.getAmount());
            rDto.setCurrency(ref.getCurrency());
            rDto.setStatus(ref.getStatus());
            rDto.setReason(ref.getReason());
            rDto.setPaymentMethod(ref.getPaymentMethod());
            rDto.setPaymentReference(ref.getPaymentReference());
            rDto.setBankAccountDetails(ref.getBankAccountDetails());
            rDto.setRequestedAt(ref.getRequestedAt());
            rDto.setProcessedAt(ref.getProcessedAt());
            rDto.setProcessedBy(ref.getProcessedBy());
            rDto.setNotes(ref.getNotes());
            dto.setRefund(rDto);
        }

        // Replacement
        replacementOrderRepository.findByReturnRequestId(ret.getId()).ifPresent(ro -> {
            ReplacementOrderDTO roDto = new ReplacementOrderDTO();
            roDto.setId(ro.getId());
            roDto.setReturnRequestId(ret.getId());
            roDto.setReturnNumber(ret.getReturnNumber());
            if (ro.getOriginalOrder() != null) {
                roDto.setOriginalOrderId(ro.getOriginalOrder().getId());
                roDto.setOriginalOrderTrackingNumber(ro.getOriginalOrder().getOrderNumber());
            }
            if (ro.getReplacementOrder() != null) {
                roDto.setReplacementOrderId(ro.getReplacementOrder().getId());
                roDto.setReplacementOrderTrackingNumber(ro.getReplacementOrder().getOrderNumber());
            }
            roDto.setStatus(ro.getStatus());
            roDto.setNotes(ro.getNotes());
            roDto.setCreatedAt(ro.getCreatedAt());
            dto.setReplacement(roDto);
        });

        return dto;
    }
}
