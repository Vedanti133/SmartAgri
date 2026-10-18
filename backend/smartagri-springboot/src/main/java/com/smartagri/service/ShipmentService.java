package com.smartagri.service;

import com.smartagri.dto.*;
import com.smartagri.entity.*;
import com.smartagri.enums.*;
import com.smartagri.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class ShipmentService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ShipmentRepository shipmentRepository;
    private final ShipmentTrackingEventRepository trackingEventRepository;
    private final OrderRepository orderRepository;
    private final WarehouseRepository warehouseRepository;
    private final DeliveryPartnerRepository deliveryPartnerRepository;
    private final FulfillmentPackageRepository packageRepository;
    private final DeliveryRepository deliveryRepository;
    private final FulfillmentAllocationRepository allocationRepository;
    private final FertilizerRepository fertilizerRepository;
    private final FertilizerBatchRepository batchRepository;
    private final BatchLocationStockRepository batchLocationStockRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final FulfillmentService fulfillmentService;
    private final PaymentService paymentService;

    public ShipmentService(ShipmentRepository shipmentRepository,
                           ShipmentTrackingEventRepository trackingEventRepository,
                           OrderRepository orderRepository,
                           WarehouseRepository warehouseRepository,
                           DeliveryPartnerRepository deliveryPartnerRepository,
                           FulfillmentPackageRepository packageRepository,
                           DeliveryRepository deliveryRepository,
                           FulfillmentAllocationRepository allocationRepository,
                           FertilizerRepository fertilizerRepository,
                           FertilizerBatchRepository batchRepository,
                           BatchLocationStockRepository batchLocationStockRepository,
                           InventoryTransactionRepository transactionRepository,
                           FulfillmentService fulfillmentService,
                           @Lazy PaymentService paymentService) {
        this.shipmentRepository = shipmentRepository;
        this.trackingEventRepository = trackingEventRepository;
        this.orderRepository = orderRepository;
        this.warehouseRepository = warehouseRepository;
        this.deliveryPartnerRepository = deliveryPartnerRepository;
        this.packageRepository = packageRepository;
        this.deliveryRepository = deliveryRepository;
        this.allocationRepository = allocationRepository;
        this.fertilizerRepository = fertilizerRepository;
        this.batchRepository = batchRepository;
        this.batchLocationStockRepository = batchLocationStockRepository;
        this.transactionRepository = transactionRepository;
        this.fulfillmentService = fulfillmentService;
        this.paymentService = paymentService;
    }

    @Transactional
    public ShipmentDTO createShipment(CreateShipmentRequest request, String username) {
        Long orderId = request.getOrderId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot create shipment for cancelled order #" + order.getOrderNumber());
        }

        if (order.getFulfillmentStatus() != FulfillmentStatus.PACKED && order.getFulfillmentStatus() != FulfillmentStatus.READY_TO_SHIP) {
            throw new IllegalArgumentException("Cannot create shipment for order in " + order.getFulfillmentStatus() + " state. Order must be PACKED before creating shipment.");
        }

        if (request.getDeliveryPartnerId() == null && (request.getCourierCode() == null || request.getCourierCode().isBlank()) && (request.getCourierName() == null || request.getCourierName().isBlank())) {
            throw new IllegalArgumentException("Delivery partner or courier is required to create a shipment");
        }

        // Section 17: Delivery Address Validation
        validateShippingAddress(order);

        // Idempotency: check if shipment already exists for this order
        List<Shipment> existingShipments = shipmentRepository.findByOrderId(orderId);
        if (!existingShipments.isEmpty() && request.getPackageId() == null) {
            log.info("Shipment already exists for order #{}, returning existing.", order.getOrderNumber());
            return fulfillmentService.toShipmentDTO(existingShipments.get(0));
        }

        Warehouse wh;
        if (request.getWarehouseId() != null) {
            wh = warehouseRepository.findById(request.getWarehouseId())
                    .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + request.getWarehouseId()));
        } else {
            List<FulfillmentAllocation> allocs = allocationRepository.findByOrderId(orderId);
            wh = !allocs.isEmpty() && allocs.get(0).getWarehouse() != null ? allocs.get(0).getWarehouse()
                    : warehouseRepository.findDefaultReceivingWarehouse().orElseGet(() ->
                    warehouseRepository.findActiveWarehouses().stream().findFirst().orElse(null));
        }

        DeliveryPartner partner = null;
        if (request.getDeliveryPartnerId() != null) {
            partner = deliveryPartnerRepository.findById(request.getDeliveryPartnerId())
                    .orElseThrow(() -> new IllegalArgumentException("Delivery partner not found with id: " + request.getDeliveryPartnerId()));
        } else if (request.getCourierCode() != null && !request.getCourierCode().isBlank()) {
            partner = deliveryPartnerRepository.findByPartnerCode(request.getCourierCode().trim().toUpperCase(Locale.ROOT)).orElse(null);
        }

        String courierName = request.getCourierName();
        if (courierName == null || courierName.isBlank()) {
            courierName = partner != null ? partner.getPartnerName() : "Kisan Speed Logistics";
        }

        String trackingNum = request.getTrackingNumber();
        if (trackingNum == null || trackingNum.isBlank()) {
            trackingNum = generateTrackingNumber();
        } else {
            trackingNum = trackingNum.trim().toUpperCase(Locale.ROOT);
            if (shipmentRepository.findByTrackingNumber(trackingNum).isPresent()) {
                throw new IllegalStateException("Tracking number '" + trackingNum + "' already exists");
            }
        }

        LocalDate expectedDelivery = request.getExpectedDeliveryDate() != null
                ? request.getExpectedDeliveryDate()
                : LocalDate.now().plusDays(3);

        Shipment shipment = new Shipment();
        shipment.setShipmentNumber(generateShipmentNumber());
        shipment.setOrder(order);
        shipment.setPackageId(request.getPackageId());
        shipment.setWarehouse(wh);
        shipment.setDeliveryPartner(partner);
        shipment.setCourierName(courierName);
        shipment.setCourierCode(partner != null ? partner.getPartnerCode() : request.getCourierCode());
        shipment.setTrackingNumber(trackingNum);
        shipment.setShippingMethod(request.getShippingMethod() != null ? request.getShippingMethod() : "STANDARD");
        shipment.setShippingCost(request.getShippingCost() != null ? request.getShippingCost() : java.math.BigDecimal.ZERO);
        shipment.setExpectedDeliveryDate(expectedDelivery);
        shipment.setStatus(ShipmentStatus.READY_FOR_DISPATCH);
        shipment.setNotes(request.getNotes());

        Shipment saved = shipmentRepository.save(shipment);

        // Initial immutable tracking event
        String locStr = wh != null ? wh.getCity() : "Fulfillment Center";
        ShipmentTrackingEvent initEvent = new ShipmentTrackingEvent(
                saved,
                "SHIPMENT_CREATED",
                "SHIPMENT_CREATED",
                "Shipment created and scheduled for dispatch via " + courierName,
                locStr,
                username != null ? username : "system"
        );
        trackingEventRepository.save(initEvent);
        saved.getTrackingEvents().add(initEvent);

        // Sync with Delivery entity (Phase 5 compatibility)
        Optional<Delivery> delivOpt = deliveryRepository.findByOrderId(orderId);
        if (delivOpt.isPresent()) {
            Delivery deliv = delivOpt.get();
            deliv.setDeliveryPartner(courierName);
            deliv.setTrackingNumber(trackingNum);
            deliv.setEstimatedDeliveryDate(expectedDelivery);
            if (deliv.getDeliveryStatus() == DeliveryStatus.NOT_ASSIGNED) {
                deliv.setDeliveryStatus(DeliveryStatus.ASSIGNED);
            }
            deliveryRepository.save(deliv);
        }

        // Update package status if linked
        if (request.getPackageId() != null) {
            packageRepository.findById(request.getPackageId()).ifPresent(pkg -> {
                pkg.setTrackingNumber(saved.getTrackingNumber());
                pkg.setCourier(saved.getCourierName());
                packageRepository.save(pkg);
            });
        }

        order.setFulfillmentStatus(FulfillmentStatus.READY_TO_SHIP);
        orderRepository.save(order);

        log.info("Created shipment #{} with tracking #{} for order #{}",
                saved.getShipmentNumber(), saved.getTrackingNumber(), order.getOrderNumber());

        return fulfillmentService.toShipmentDTO(saved);
    }

    @Transactional
    public ShipmentDTO dispatchShipment(Long shipmentId, String username) {
        Shipment shipment = shipmentRepository.findByIdWithLock(shipmentId)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found with id: " + shipmentId));

        if (shipment.getStatus() == ShipmentStatus.DISPATCHED || shipment.getStatus() == ShipmentStatus.IN_TRANSIT) {
            throw new IllegalArgumentException("Shipment #" + shipment.getShipmentNumber() + " is already dispatched");
        }

        if (!shipment.getStatus().canTransitionTo(ShipmentStatus.DISPATCHED)) {
            throw new IllegalStateException("Invalid shipment dispatch transition from " + shipment.getStatus() + " to DISPATCHED");
        }

        LocalDateTime now = LocalDateTime.now();
        shipment.setStatus(ShipmentStatus.DISPATCHED);
        shipment.setDispatchedAt(now);
        shipment.setDispatchedBy(username != null ? username : "admin");

        String locStr = shipment.getWarehouse() != null ? shipment.getWarehouse().getCity() : "Fulfillment Center";
        ShipmentTrackingEvent event = new ShipmentTrackingEvent(
                shipment,
                ShipmentStatus.DISPATCHED.name(),
                "DISPATCHED",
                "Consignment dispatched from " + (shipment.getWarehouse() != null ? shipment.getWarehouse().getWarehouseName() : "facility")
                        + " via " + shipment.getCourierName() + " (Tracking: " + shipment.getTrackingNumber() + ")",
                locStr,
                username != null ? username : "dispatcher"
        );
        trackingEventRepository.save(event);
        shipment.getTrackingEvents().add(event);

        Shipment saved = shipmentRepository.save(shipment);

        // Update Order
        Order order = saved.getOrder();
        if (order != null) {
            order.setFulfillmentStatus(FulfillmentStatus.SHIPPED);
            if (order.getOrderStatus() == OrderStatus.PROCESSING || order.getOrderStatus() == OrderStatus.PACKED || order.getOrderStatus() == OrderStatus.CONFIRMED) {
                order.setOrderStatus(OrderStatus.SHIPPED);
            }
            orderRepository.save(order);

            // Sync Delivery entity
            deliveryRepository.findByOrderId(order.getId()).ifPresent(deliv -> {
                deliv.setDeliveryStatus(DeliveryStatus.SHIPPED);
                if (deliv.getShippedAt() == null) {
                    deliv.setShippedAt(now);
                }
                deliv.setTrackingNumber(saved.getTrackingNumber());
                deliv.setDeliveryPartner(saved.getCourierName());
                deliveryRepository.save(deliv);
            });
        }

        // Update linked package status
        if (saved.getPackageId() != null) {
            packageRepository.findById(saved.getPackageId()).ifPresent(pkg -> {
                pkg.setPackageStatus(PackageStatus.DISPATCHED);
                packageRepository.save(pkg);
            });
        }

        log.info("Dispatched shipment #{} (tracking: {}) by user {}",
                saved.getShipmentNumber(), saved.getTrackingNumber(), username);

        return fulfillmentService.toShipmentDTO(saved);
    }

    @Transactional
    public ShipmentDTO addTrackingEvent(Long shipmentId, AddTrackingEventRequest request, String username) {
        Shipment shipment = shipmentRepository.findByIdWithLock(shipmentId)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found with id: " + shipmentId));

        ShipmentStatus newStatus = null;
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                newStatus = ShipmentStatus.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {}
        }

        if (newStatus != null && newStatus != shipment.getStatus()) {
            if (!shipment.getStatus().canTransitionTo(newStatus)) {
                throw new IllegalStateException("Invalid tracking status transition from " + shipment.getStatus() + " to " + newStatus);
            }
            shipment.setStatus(newStatus);
            if (newStatus == ShipmentStatus.DISPATCHED && shipment.getDispatchedAt() == null) {
                shipment.setDispatchedAt(LocalDateTime.now());
            } else if (newStatus == ShipmentStatus.DELIVERED && shipment.getDeliveredAt() == null) {
                shipment.setDeliveredAt(LocalDateTime.now());
            }
        }

        String eventCode = request.getEventCode() != null && !request.getEventCode().isBlank()
                ? request.getEventCode().trim().toUpperCase(Locale.ROOT)
                : (newStatus != null ? newStatus.name() : "IN_TRANSIT");

        ShipmentTrackingEvent event = new ShipmentTrackingEvent(
                shipment,
                shipment.getStatus().name(),
                eventCode,
                request.getDescription().trim(),
                request.getLocation() != null ? request.getLocation().trim() : null,
                username != null ? username : "operator"
        );
        trackingEventRepository.save(event);
        shipment.getTrackingEvents().add(event);

        Shipment saved = shipmentRepository.save(shipment);

        // Sync order status
        if (saved.getOrder() != null && newStatus != null) {
            try {
                FulfillmentStatus fs = FulfillmentStatus.valueOf(newStatus.name());
                saved.getOrder().setFulfillmentStatus(fs);
                orderRepository.save(saved.getOrder());
            } catch (IllegalArgumentException ignored) {}
        }

        return fulfillmentService.toShipmentDTO(saved);
    }

    @Transactional
    public ShipmentDTO markOutForDelivery(Long shipmentId, String notes, String username) {
        Shipment shipment = shipmentRepository.findByIdWithLock(shipmentId)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found with id: " + shipmentId));

        if (!shipment.getStatus().canTransitionTo(ShipmentStatus.OUT_FOR_DELIVERY)) {
            throw new IllegalStateException("Cannot transition shipment from " + shipment.getStatus() + " to OUT_FOR_DELIVERY");
        }

        LocalDateTime now = LocalDateTime.now();
        shipment.setStatus(ShipmentStatus.OUT_FOR_DELIVERY);

        String desc = "Out for delivery with delivery executive" + (notes != null && !notes.isBlank() ? " (" + notes.trim() + ")" : "");
        ShipmentTrackingEvent event = new ShipmentTrackingEvent(
                shipment,
                ShipmentStatus.OUT_FOR_DELIVERY.name(),
                "OUT_FOR_DELIVERY",
                desc,
                shipment.getOrder() != null ? shipment.getOrder().getCity() : null,
                username != null ? username : "courier"
        );
        trackingEventRepository.save(event);
        shipment.getTrackingEvents().add(event);

        Shipment saved = shipmentRepository.save(shipment);

        Order order = saved.getOrder();
        if (order != null) {
            order.setFulfillmentStatus(FulfillmentStatus.OUT_FOR_DELIVERY);
            orderRepository.save(order);

            deliveryRepository.findByOrderId(order.getId()).ifPresent(deliv -> {
                deliv.setDeliveryStatus(DeliveryStatus.OUT_FOR_DELIVERY);
                if (deliv.getOutForDeliveryAt() == null) {
                    deliv.setOutForDeliveryAt(now);
                }
                deliveryRepository.save(deliv);
            });
        }

        return fulfillmentService.toShipmentDTO(saved);
    }

    @Transactional
    public ShipmentDTO markDelivered(Long shipmentId, DeliveryProofRequest proof, String username) {
        Shipment shipment = shipmentRepository.findByIdWithLock(shipmentId)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found with id: " + shipmentId));

        // Idempotency: if already delivered, return cleanly
        if (shipment.getStatus() == ShipmentStatus.DELIVERED) {
            log.info("Shipment #{} is already marked DELIVERED (idempotent).", shipment.getShipmentNumber());
            return fulfillmentService.toShipmentDTO(shipment);
        }

        if (!shipment.getStatus().canTransitionTo(ShipmentStatus.DELIVERED)) {
            throw new IllegalStateException("Cannot transition shipment from " + shipment.getStatus() + " to DELIVERED");
        }

        LocalDateTime now = LocalDateTime.now();
        shipment.setStatus(ShipmentStatus.DELIVERED);
        shipment.setDeliveredAt(now);

        String receiver = proof != null && proof.getReceiverName() != null && !proof.getReceiverName().isBlank()
                ? proof.getReceiverName().trim()
                : (shipment.getOrder() != null ? shipment.getOrder().getRecipientName() : "Customer");
        shipment.setDeliveryProofReceiver(receiver);

        if (proof != null && proof.getNotes() != null && !proof.getNotes().isBlank()) {
            shipment.setNotes((shipment.getNotes() != null ? shipment.getNotes() + " | " : "") + proof.getNotes().trim());
        }

        ShipmentTrackingEvent event = new ShipmentTrackingEvent(
                shipment,
                ShipmentStatus.DELIVERED.name(),
                "DELIVERED",
                "Consignment delivered successfully. Handed over to: " + receiver,
                shipment.getOrder() != null ? shipment.getOrder().getCity() : null,
                username != null ? username : "courier"
        );
        trackingEventRepository.save(event);
        shipment.getTrackingEvents().add(event);

        Shipment saved = shipmentRepository.save(shipment);

        Order order = saved.getOrder();
        if (order != null) {
            order.setFulfillmentStatus(FulfillmentStatus.DELIVERED);
            order.setOrderStatus(OrderStatus.DELIVERED);

            // If COD, mark payment as PAID
            if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                order.setPaymentStatus(PaymentStatus.PAID);
                try {
                    paymentService.updatePaymentStatusAdmin(order.getId(),
                            new com.smartagri.dto.UpdatePaymentStatusRequest("PAID", null, "Paid on delivery"));
                } catch (Exception e) {
                    log.warn("Payment status auto-sync error for COD order #{}: {}", order.getId(), e.getMessage());
                }
            }
            orderRepository.save(order);

            // Sync Delivery entity
            deliveryRepository.findByOrderId(order.getId()).ifPresent(deliv -> {
                deliv.setDeliveryStatus(DeliveryStatus.DELIVERED);
                if (deliv.getDeliveredAt() == null) {
                    deliv.setDeliveredAt(now);
                }
                deliveryRepository.save(deliv);
            });
        }

        // Update package status if linked
        if (saved.getPackageId() != null) {
            packageRepository.findById(saved.getPackageId()).ifPresent(pkg -> {
                pkg.setPackageStatus(PackageStatus.DELIVERED);
                packageRepository.save(pkg);
            });
        }

        log.info("Marked shipment #{} as DELIVERED to {}", saved.getShipmentNumber(), receiver);
        return fulfillmentService.toShipmentDTO(saved);
    }

    @Transactional
    public ShipmentDTO recordDeliveryFailure(Long shipmentId, DeliveryFailureRequest request, String username) {
        Shipment shipment = shipmentRepository.findByIdWithLock(shipmentId)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found with id: " + shipmentId));

        if (!shipment.getStatus().canTransitionTo(ShipmentStatus.DELIVERY_FAILED)) {
            throw new IllegalStateException("Cannot transition shipment from " + shipment.getStatus() + " to DELIVERY_FAILED");
        }

        shipment.setStatus(ShipmentStatus.DELIVERY_FAILED);
        shipment.setDeliveryFailureReason(request.getReason().trim());
        shipment.setDeliveryFailureNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        String failureDesc = "Delivery attempt failed: " + request.getReason().trim()
                + (request.getNotes() != null && !request.getNotes().isBlank() ? " (" + request.getNotes().trim() + ")" : "");

        ShipmentTrackingEvent event = new ShipmentTrackingEvent(
                shipment,
                ShipmentStatus.DELIVERY_FAILED.name(),
                "DELIVERY_FAILED",
                failureDesc,
                shipment.getOrder() != null ? shipment.getOrder().getCity() : null,
                username != null ? username : "courier"
        );
        trackingEventRepository.save(event);
        shipment.getTrackingEvents().add(event);

        Shipment saved = shipmentRepository.save(shipment);

        Order order = saved.getOrder();
        if (order != null) {
            order.setFulfillmentStatus(FulfillmentStatus.DELIVERY_FAILED);
            orderRepository.save(order);

            deliveryRepository.findByOrderId(order.getId()).ifPresent(deliv -> {
                deliv.setDeliveryStatus(DeliveryStatus.FAILED);
                deliv.setDeliveryNotes("Delivery failed: " + request.getReason());
                deliveryRepository.save(deliv);
            });
        }

        log.info("Recorded delivery failure for shipment #{}: {}", saved.getShipmentNumber(), request.getReason());
        return fulfillmentService.toShipmentDTO(saved);
    }

    @Transactional
    public ShipmentDTO initiateRTO(Long shipmentId, String reason, String username) {
        Shipment shipment = shipmentRepository.findByIdWithLock(shipmentId)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found with id: " + shipmentId));

        if (!shipment.getStatus().canTransitionTo(ShipmentStatus.RETURN_TO_ORIGIN)) {
            throw new IllegalStateException("Cannot transition shipment from " + shipment.getStatus() + " to RETURN_TO_ORIGIN");
        }

        shipment.setStatus(ShipmentStatus.RETURN_TO_ORIGIN);
        String desc = "Return to origin initiated: " + (reason != null && !reason.isBlank() ? reason.trim() : "Returning to warehouse");

        ShipmentTrackingEvent event = new ShipmentTrackingEvent(
                shipment,
                ShipmentStatus.RETURN_TO_ORIGIN.name(),
                "RETURN_TO_ORIGIN",
                desc,
                shipment.getWarehouse() != null ? shipment.getWarehouse().getCity() : null,
                username != null ? username : "admin"
        );
        trackingEventRepository.save(event);
        shipment.getTrackingEvents().add(event);

        Shipment saved = shipmentRepository.save(shipment);

        Order order = saved.getOrder();
        if (order != null) {
            order.setFulfillmentStatus(FulfillmentStatus.RETURN_TO_ORIGIN);
            orderRepository.save(order);
        }

        log.info("Initiated RTO for shipment #{}: {}", saved.getShipmentNumber(), reason);
        return fulfillmentService.toShipmentDTO(saved);
    }

    @Transactional
    public ShipmentDTO receiveReturnedStock(Long shipmentId, String username) {
        Shipment shipment = shipmentRepository.findByIdWithLock(shipmentId)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found with id: " + shipmentId));

        // Idempotency: cannot receive if already RETURNED
        if (shipment.getStatus() == ShipmentStatus.RETURNED) {
            throw new IllegalStateException("Shipment '" + shipment.getShipmentNumber() + "' has already been received and returned to inventory.");
        }

        if (shipment.getStatus() != ShipmentStatus.RETURN_TO_ORIGIN) {
            throw new IllegalStateException("Shipment must be in RETURN_TO_ORIGIN status to receive returned stock. Current status: " + shipment.getStatus());
        }

        Order order = shipment.getOrder();
        if (order == null) {
            throw new IllegalStateException("Shipment is missing associated order");
        }

        // Restore inventory for each allocation of this order
        List<FulfillmentAllocation> allocations = allocationRepository.findByOrderId(order.getId());
        LocalDateTime now = LocalDateTime.now();

        for (FulfillmentAllocation alloc : allocations) {
            int restoreQty = alloc.getAllocatedQuantity();
            if (restoreQty <= 0) continue;

            Fertilizer fert = alloc.getOrderItem() != null ? alloc.getOrderItem().getFertilizer() : null;
            FertilizerBatch batch = alloc.getBatch();
            Warehouse wh = alloc.getWarehouse();
            WarehouseLocation loc = alloc.getLocation();

            if (fert == null && batch != null) {
                fert = batch.getFertilizer();
            }

            int fertBefore = 0;
            if (fert != null) {
                fertBefore = fert.getStock() != null ? fert.getStock() : 0;
                fert.setStock(fertBefore + restoreQty);
                fertilizerRepository.save(fert);
            }

            if (batch != null) {
                int bBefore = batch.getCurrentQuantity() != null ? batch.getCurrentQuantity() : 0;
                batch.setCurrentQuantity(bBefore + restoreQty);
                batch.setStatus(batch.computeDynamicStatus());
                batchRepository.save(batch);

                // Restore to location stock
                if (wh != null && loc != null) {
                    BatchLocationStock locStock = batchLocationStockRepository
                            .findByBatchIdAndWarehouseIdAndLocationId(batch.getId(), wh.getId(), loc.getId())
                            .orElseGet(() -> {
                                BatchLocationStock n = new BatchLocationStock();
                                n.setBatch(batch);
                                n.setWarehouse(wh);
                                n.setLocation(loc);
                                n.setQuantity(0);
                                n.setReservedQuantity(0);
                                return n;
                            });
                    locStock.setQuantity(locStock.getQuantity() + restoreQty);
                    batchLocationStockRepository.save(locStock);
                }
            }

            // Log audited RETURN_IN transaction
            InventoryTransaction tx = new InventoryTransaction();
            tx.setFertilizer(fert);
            tx.setBatch(batch);
            tx.setWarehouse(wh);
            tx.setLocation(loc);
            tx.setTransactionType(InventoryTransactionType.RETURN_IN);
            tx.setQuantity(restoreQty);
            tx.setStockBefore(fertBefore);
            tx.setStockAfter(fertBefore + restoreQty);
            tx.setReferenceType("SHIPMENT");
            tx.setReferenceId(shipment.getShipmentNumber() + " (ID:" + shipment.getId() + ")");
            tx.setReason("RTO shipment received back into warehouse: " + shipment.getShipmentNumber());
            tx.setPerformedBy(username != null ? username : "warehouse_operator");
            transactionRepository.save(tx);
        }

        shipment.setStatus(ShipmentStatus.RETURNED);

        ShipmentTrackingEvent event = new ShipmentTrackingEvent(
                shipment,
                ShipmentStatus.RETURNED.name(),
                "RETURNED",
                "Consignment received into warehouse inventory. Stock restored.",
                shipment.getWarehouse() != null ? shipment.getWarehouse().getCity() : null,
                username != null ? username : "warehouse_operator"
        );
        trackingEventRepository.save(event);
        shipment.getTrackingEvents().add(event);

        Shipment saved = shipmentRepository.save(shipment);

        order.setFulfillmentStatus(FulfillmentStatus.RETURNED);
        orderRepository.save(order);

        deliveryRepository.findByOrderId(order.getId()).ifPresent(deliv -> {
            deliv.setDeliveryStatus(DeliveryStatus.RETURNED);
            deliv.setDeliveryNotes("Returned to warehouse and restocked");
            deliveryRepository.save(deliv);
        });

        if (saved.getPackageId() != null) {
            packageRepository.findById(saved.getPackageId()).ifPresent(pkg -> {
                pkg.setPackageStatus(PackageStatus.RETURNED);
                packageRepository.save(pkg);
            });
        }

        log.info("Returned stock received for shipment #{}: restored inventory to warehouse and batches",
                saved.getShipmentNumber());

        return fulfillmentService.toShipmentDTO(saved);
    }

    @Transactional(readOnly = true)
    public CustomerTrackingDTO getCustomerTracking(Long userId, Long orderId) {
        return getCustomerTracking(userId, null, orderId);
    }

    @Transactional(readOnly = true)
    public CustomerTrackingDTO getCustomerTracking(Long userId, String role, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        if (!isAdmin && (userId == null || !order.getUser().getId().equals(userId))) {
            throw new SecurityException("Access denied: You do not own this order.");
        }

        List<Shipment> shipments = shipmentRepository.findLatestByOrderId(orderId);
        Shipment latestShipment = !shipments.isEmpty() ? shipments.get(0) : null;

        CustomerTrackingDTO dto = new CustomerTrackingDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setOrderDate(order.getCreatedAt());
        dto.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : "PENDING");
        dto.setFulfillmentStatus(order.getFulfillmentStatus() != null ? order.getFulfillmentStatus().name() : "UNALLOCATED");
        dto.setRecipientName(order.getRecipientName());
        dto.setRecipientPhone(order.getRecipientPhone());
        dto.setDeliveryAddress(String.format("%s, %s, %s - %s",
                order.getAddressLine1(), order.getCity(), order.getState(), order.getPincode()));

        // Products summary
        String prodSummary = order.getItems().stream()
                .map(i -> i.getProductName() + " (x" + i.getQuantity() + ")")
                .collect(Collectors.joining(", "));
        dto.setProductsSummary(prodSummary);

        if (latestShipment != null) {
            dto.setShipmentNumber(latestShipment.getShipmentNumber());
            dto.setTrackingNumber(latestShipment.getTrackingNumber());
            dto.setCourier(latestShipment.getCourierName());
            if (latestShipment.getDeliveryPartner() != null && latestShipment.getDeliveryPartner().getTrackingUrlTemplate() != null) {
                dto.setTrackingUrl(String.format(latestShipment.getDeliveryPartner().getTrackingUrlTemplate(), latestShipment.getTrackingNumber()));
            }
            dto.setExpectedDeliveryDate(latestShipment.getExpectedDeliveryDate());
            dto.setActualDeliveryDate(latestShipment.getActualDeliveryDate());
            dto.setCurrentStatus(latestShipment.getStatus() != null ? latestShipment.getStatus().name() : "CREATED");

            if (latestShipment.getTrackingEvents() != null) {
                dto.setTrackingTimeline(latestShipment.getTrackingEvents().stream().map(e -> {
                    ShipmentTrackingEventDTO edto = new ShipmentTrackingEventDTO();
                    edto.setId(e.getId());
                    edto.setShipmentId(latestShipment.getId());
                    edto.setStatus(e.getStatus());
                    edto.setEventCode(e.getEventCode());
                    edto.setDescription(e.getDescription());
                    edto.setLocation(e.getLocation());
                    edto.setEventTime(e.getEventTime());
                    edto.setCreatedBy(e.getCreatedBy());
                    return edto;
                }).collect(Collectors.toList()));
            }
        } else {
            dto.setCurrentStatus(order.getFulfillmentStatus() != null ? order.getFulfillmentStatus().name() : "PROCESSING");
        }

        // Packages
        dto.setPackages(packageRepository.findByOrderId(orderId).stream()
                .map(fulfillmentService::toPackageDTO).collect(Collectors.toList()));

        return dto;
    }

    @Transactional(readOnly = true)
    public ShipmentDTO getShipmentById(Long id) {
        Shipment s = shipmentRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Shipment not found with id: " + id));
        return fulfillmentService.toShipmentDTO(s);
    }

    @Transactional(readOnly = true)
    public List<ShipmentDTO> getShipments(String status, String courier, String search, Long warehouseId) {
        List<Shipment> list;
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            try {
                ShipmentStatus st = ShipmentStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
                list = shipmentRepository.findByStatus(st);
            } catch (IllegalArgumentException e) {
                list = shipmentRepository.findAll();
            }
        } else if (warehouseId != null) {
            list = shipmentRepository.findByWarehouseId(warehouseId);
        } else {
            list = shipmentRepository.findAll();
        }

        if (courier != null && !courier.isBlank()) {
            String c = courier.trim().toLowerCase(Locale.ROOT);
            list = list.stream().filter(s -> s.getCourierName() != null && s.getCourierName().toLowerCase(Locale.ROOT).contains(c)).collect(Collectors.toList());
        }

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase(Locale.ROOT);
            list = list.stream().filter(s ->
                    s.getShipmentNumber().toLowerCase(Locale.ROOT).contains(q)
                            || s.getTrackingNumber().toLowerCase(Locale.ROOT).contains(q)
                            || (s.getOrder() != null && s.getOrder().getOrderNumber().toLowerCase(Locale.ROOT).contains(q))
            ).collect(Collectors.toList());
        }

        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return list.stream().map(fulfillmentService::toShipmentDTO).collect(Collectors.toList());
    }

    private void validateShippingAddress(Order order) {
        if (order.getRecipientName() == null || order.getRecipientName().isBlank()) {
            throw new IllegalArgumentException("Shipping recipient name is required");
        }
        if (order.getRecipientPhone() == null || order.getRecipientPhone().isBlank()) {
            throw new IllegalArgumentException("Shipping recipient phone is required");
        }
        if (order.getAddressLine1() == null || order.getAddressLine1().isBlank()) {
            throw new IllegalArgumentException("Shipping address line 1 is required");
        }
        if (order.getCity() == null || order.getCity().isBlank()) {
            throw new IllegalArgumentException("Shipping city is required");
        }
        if (order.getState() == null || order.getState().isBlank()) {
            throw new IllegalArgumentException("Shipping state is required");
        }
        if (order.getPincode() == null || order.getPincode().isBlank()) {
            throw new IllegalArgumentException("Shipping pincode is required");
        }
    }

    public String generateTrackingNumber() {
        String datePart = LocalDate.now().format(DATE_FMT);
        String candidate;
        int attempts = 0;
        do {
            int randomSeq = ThreadLocalRandom.current().nextInt(100000, 999999);
            candidate = String.format(Locale.ROOT, "SA-TRK-%s-%06d", datePart, randomSeq);
            attempts++;
            if (attempts > 50) {
                candidate = String.format(Locale.ROOT, "SA-TRK-%s-%06d", datePart, System.currentTimeMillis() % 1000000);
                break;
            }
        } while (shipmentRepository.findByTrackingNumber(candidate).isPresent());
        return candidate;
    }

    private String generateShipmentNumber() {
        String datePart = LocalDate.now().format(DATE_FMT);
        int seq = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format(Locale.ROOT, "SHP-%s-%04d", datePart, seq);
    }
}
