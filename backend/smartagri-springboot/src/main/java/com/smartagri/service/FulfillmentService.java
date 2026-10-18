package com.smartagri.service;

import com.smartagri.dto.*;
import com.smartagri.entity.*;
import com.smartagri.enums.*;
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
public class FulfillmentService {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderItemBatchRepository orderItemBatchRepository;
    private final FulfillmentAllocationRepository allocationRepository;
    private final PickListRepository pickListRepository;
    private final FulfillmentPackageRepository packageRepository;
    private final ShipmentRepository shipmentRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final FertilizerBatchRepository batchRepository;
    private final BatchLocationStockRepository batchLocationStockRepository;

    public FulfillmentService(OrderRepository orderRepository,
                              OrderItemRepository orderItemRepository,
                              OrderItemBatchRepository orderItemBatchRepository,
                              FulfillmentAllocationRepository allocationRepository,
                              PickListRepository pickListRepository,
                              FulfillmentPackageRepository packageRepository,
                              ShipmentRepository shipmentRepository,
                              WarehouseRepository warehouseRepository,
                              WarehouseLocationRepository locationRepository,
                              FertilizerBatchRepository batchRepository,
                              BatchLocationStockRepository batchLocationStockRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderItemBatchRepository = orderItemBatchRepository;
        this.allocationRepository = allocationRepository;
        this.pickListRepository = pickListRepository;
        this.packageRepository = packageRepository;
        this.shipmentRepository = shipmentRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.batchRepository = batchRepository;
        this.batchLocationStockRepository = batchLocationStockRepository;
    }

    @Transactional
    public List<FulfillmentAllocationDTO> allocateOrder(Long orderId, String username) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot allocate cancelled order #" + order.getOrderNumber());
        }

        // Idempotency check: if allocations already exist, return them
        List<FulfillmentAllocation> existing = allocationRepository.findByOrderId(orderId);
        if (!existing.isEmpty()) {
            log.info("Order #{} already has {} allocations, returning existing.", order.getOrderNumber(), existing.size());
            return existing.stream().map(this::toAllocationDTO).collect(Collectors.toList());
        }

        List<FulfillmentAllocation> createdAllocations = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. Check if OrderItemBatch records already exist (created during checkout FEFO)
        List<OrderItem> orderItems = order.getItems();
        boolean hasExistingOib = false;

        for (OrderItem item : orderItems) {
            List<OrderItemBatch> oibList = orderItemBatchRepository.findByOrderItemId(item.getId());
            if (!oibList.isEmpty()) {
                hasExistingOib = true;
                for (OrderItemBatch oib : oibList) {
                    Warehouse wh = oib.getWarehouse();
                    WarehouseLocation loc = oib.getLocation();
                    FertilizerBatch b = oib.getBatch();

                    // If warehouse/location was not set on legacy record, fallback to default receiving or active warehouse
                    if (wh == null) {
                        wh = warehouseRepository.findDefaultReceivingWarehouse().orElseGet(() ->
                                warehouseRepository.findActiveWarehouses().stream().findFirst().orElse(null));
                    }
                    if (loc == null && wh != null) {
                        Warehouse finalWh = wh;
                        loc = locationRepository.findActiveLocationsByWarehouseId(wh.getId()).stream().findFirst().orElseGet(() ->
                                locationRepository.findAll().stream().filter(l -> l.getWarehouse().getId().equals(finalWh.getId())).findFirst().orElse(null));
                    }

                    FulfillmentAllocation fa = new FulfillmentAllocation();
                    fa.setAllocationNumber(generateAllocationNumber());
                    fa.setOrder(order);
                    fa.setOrderItem(item);
                    fa.setWarehouse(wh);
                    fa.setLocation(loc);
                    fa.setBatch(b);
                    fa.setAllocatedQuantity(oib.getQuantity());
                    fa.setPickedQuantity(0);
                    fa.setPackedQuantity(0);
                    fa.setStatus("ALLOCATED");
                    fa.setAllocatedAt(now);

                    FulfillmentAllocation saved = allocationRepository.save(fa);
                    createdAllocations.add(saved);
                }
            }
        }

        // 2. Fallback if no OrderItemBatch exists: Allocate from active warehouse/location/batch
        if (!hasExistingOib) {
            LocalDate today = LocalDate.now();
            Warehouse defWh = warehouseRepository.findDefaultReceivingWarehouse().orElseGet(() ->
                    warehouseRepository.findActiveWarehouses().stream().findFirst().orElse(null));

            for (OrderItem item : orderItems) {
                if (item.getFertilizer() == null) continue;
                Long fertId = item.getFertilizer().getId();
                int remaining = item.getQuantity();

                List<FertilizerBatch> batches = batchRepository.findSaleableBatchesFEFOWithLock(fertId, today);
                for (FertilizerBatch b : batches) {
                    if (remaining <= 0) break;
                    List<BatchLocationStock> locStocks = batchLocationStockRepository.findByBatchId(b.getId()).stream()
                            .filter(ls -> ls.getWarehouse() != null && ls.getWarehouse().getStatus() == WarehouseStatus.ACTIVE
                                    && ls.getLocation() != null && ls.getLocation().getStatus() == LocationStatus.ACTIVE
                                    && ls.getLocation().getLocationType() != LocationType.QUARANTINE
                                    && ls.getLocation().getLocationType() != LocationType.DAMAGED
                                    && ls.getAvailableQuantity() > 0)
                            .collect(Collectors.toList());

                    for (BatchLocationStock ls : locStocks) {
                        if (remaining <= 0) break;
                        int allocQty = Math.min(remaining, ls.getAvailableQuantity());
                        FulfillmentAllocation fa = new FulfillmentAllocation();
                        fa.setAllocationNumber(generateAllocationNumber());
                        fa.setOrder(order);
                        fa.setOrderItem(item);
                        fa.setWarehouse(ls.getWarehouse());
                        fa.setLocation(ls.getLocation());
                        fa.setBatch(b);
                        fa.setAllocatedQuantity(allocQty);
                        fa.setPickedQuantity(0);
                        fa.setPackedQuantity(0);
                        fa.setStatus("ALLOCATED");
                        fa.setAllocatedAt(now);

                        FulfillmentAllocation saved = allocationRepository.save(fa);
                        createdAllocations.add(saved);
                        remaining -= allocQty;
                    }
                }

                // If still remaining and defWh exists, allocate against available batch
                if (remaining > 0 && !batches.isEmpty() && defWh != null) {
                    FertilizerBatch b = batches.get(0);
                    WarehouseLocation defLoc = locationRepository.findActiveLocationsByWarehouseId(defWh.getId()).stream().findFirst().orElse(null);
                    FulfillmentAllocation fa = new FulfillmentAllocation();
                    fa.setAllocationNumber(generateAllocationNumber());
                    fa.setOrder(order);
                    fa.setOrderItem(item);
                    fa.setWarehouse(defWh);
                    fa.setLocation(defLoc);
                    fa.setBatch(b);
                    fa.setAllocatedQuantity(remaining);
                    fa.setPickedQuantity(0);
                    fa.setPackedQuantity(0);
                    fa.setStatus("ALLOCATED");
                    fa.setAllocatedAt(now);
                    createdAllocations.add(allocationRepository.save(fa));
                }
            }
        }

        order.setFulfillmentStatus(FulfillmentStatus.ALLOCATED);
        if (order.getOrderStatus() == OrderStatus.CONFIRMED || order.getOrderStatus() == OrderStatus.PENDING) {
            order.setOrderStatus(OrderStatus.PROCESSING);
        }
        orderRepository.save(order);

        log.info("Successfully allocated order #{} with {} allocation records by user {}",
                order.getOrderNumber(), createdAllocations.size(), username);

        return createdAllocations.stream().map(this::toAllocationDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FulfillmentAllocationDTO> getAllocationsForOrder(Long orderId) {
        return allocationRepository.findByOrderId(orderId).stream()
                .map(this::toAllocationDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderFulfillmentDetailDTO getFulfillmentDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        OrderFulfillmentDetailDTO dto = new OrderFulfillmentDetailDTO();
        dto.setOrderId(order.getId());
        dto.setOrderNumber(order.getOrderNumber());
        dto.setCustomerName(order.getRecipientName());
        dto.setCustomerPhone(order.getRecipientPhone());
        dto.setShippingAddress(String.format("%s, %s", order.getAddressLine1(), order.getCity()));
        dto.setCity(order.getCity());
        dto.setState(order.getState());
        dto.setPincode(order.getPincode());
        dto.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : "PENDING");
        dto.setFulfillmentStatus(order.getFulfillmentStatus() != null ? order.getFulfillmentStatus().name() : "UNALLOCATED");
        dto.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : "PENDING");
        dto.setPaymentMethod(order.getPaymentMethod());
        dto.setGrandTotal(order.getGrandTotal());
        dto.setOrderDate(order.getCreatedAt());

        // Allocations
        dto.setAllocations(allocationRepository.findByOrderId(orderId).stream()
                .map(this::toAllocationDTO).collect(Collectors.toList()));

        // Pick Lists
        dto.setPickLists(pickListRepository.findByOrderId(orderId).stream()
                .map(this::toPickListDTO).collect(Collectors.toList()));

        // Packages
        dto.setPackages(packageRepository.findByOrderId(orderId).stream()
                .map(this::toPackageDTO).collect(Collectors.toList()));

        // Shipments
        dto.setShipments(shipmentRepository.findByOrderId(orderId).stream()
                .map(this::toShipmentDTO).collect(Collectors.toList()));

        return dto;
    }

    @Transactional(readOnly = true)
    public List<OrderFulfillmentDetailDTO> getAllFulfillments(String status, Long warehouseId, String search) {
        List<Order> orders = orderRepository.findAll();
        // Sort newest first
        orders.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            String targetStatus = status.trim().toUpperCase(Locale.ROOT);
            orders = orders.stream().filter(o ->
                    (o.getFulfillmentStatus() != null && o.getFulfillmentStatus().name().equalsIgnoreCase(targetStatus))
                            || (o.getOrderStatus() != null && o.getOrderStatus().name().equalsIgnoreCase(targetStatus))
            ).collect(Collectors.toList());
        }

        if (search != null && !search.isBlank()) {
            String q = search.trim().toLowerCase(Locale.ROOT);
            orders = orders.stream().filter(o ->
                    o.getOrderNumber().toLowerCase(Locale.ROOT).contains(q)
                            || (o.getRecipientName() != null && o.getRecipientName().toLowerCase(Locale.ROOT).contains(q))
                            || (o.getCity() != null && o.getCity().toLowerCase(Locale.ROOT).contains(q))
            ).collect(Collectors.toList());
        }

        return orders.stream().map(o -> getFulfillmentDetail(o.getId())).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FulfillmentDashboardDTO getDashboard() {
        FulfillmentDashboardDTO dto = new FulfillmentDashboardDTO();
        List<Order> orders = orderRepository.findAll();
        List<Shipment> shipments = shipmentRepository.findAll();
        List<FulfillmentPackage> packages = packageRepository.findAll();

        long awaiting = orders.stream().filter(o ->
                o.getFulfillmentStatus() == FulfillmentStatus.UNALLOCATED
                        || o.getFulfillmentStatus() == FulfillmentStatus.READY_FOR_FULFILLMENT
                        || o.getFulfillmentStatus() == FulfillmentStatus.ALLOCATED
        ).count();

        long picking = orders.stream().filter(o -> o.getFulfillmentStatus() == FulfillmentStatus.PICKING).count();
        long packing = orders.stream().filter(o -> o.getFulfillmentStatus() == FulfillmentStatus.PACKING).count();
        long readyDispatch = shipments.stream().filter(s -> s.getStatus() == ShipmentStatus.READY_FOR_DISPATCH || s.getStatus() == ShipmentStatus.CREATED).count();
        long inTransit = shipments.stream().filter(s -> s.getStatus() == ShipmentStatus.DISPATCHED || s.getStatus() == ShipmentStatus.IN_TRANSIT).count();
        long outForDelivery = shipments.stream().filter(s -> s.getStatus() == ShipmentStatus.OUT_FOR_DELIVERY).count();
        long delivered = shipments.stream().filter(s -> s.getStatus() == ShipmentStatus.DELIVERED).count();
        long failed = shipments.stream().filter(s -> s.getStatus() == ShipmentStatus.DELIVERY_FAILED).count();
        long rto = shipments.stream().filter(s -> s.getStatus() == ShipmentStatus.RETURN_TO_ORIGIN || s.getStatus() == ShipmentStatus.RETURNED).count();

        dto.setOrdersAwaitingFulfillment(awaiting);
        dto.setOrdersPicking(picking);
        dto.setOrdersPacking(packing);
        dto.setOrdersReadyToDispatch(readyDispatch);
        dto.setShipmentsInTransit(inTransit);
        dto.setShipmentsOutForDelivery(outForDelivery);
        dto.setShipmentsDelivered(delivered);
        dto.setFailedDeliveries(failed);
        dto.setRtoShipments(rto);
        dto.setTotalShipments(shipments.size());
        dto.setTotalPackages(packages.size());

        return dto;
    }

    @Transactional(readOnly = true)
    public FulfillmentHealthDTO checkFulfillmentHealth() {
        FulfillmentHealthDTO health = new FulfillmentHealthDTO();
        List<Order> orders = orderRepository.findAll();
        List<Shipment> shipments = shipmentRepository.findAll();
        List<FulfillmentPackage> packages = packageRepository.findAll();
        List<FulfillmentAllocation> allocations = allocationRepository.findAll();

        health.setOrdersChecked(orders.size());
        health.setShipmentsChecked(shipments.size());
        health.setPackagesChecked(packages.size());
        health.setAllocationsChecked(allocations.size());

        List<String> issues = new ArrayList<>();

        // Check shipments without tracking numbers
        for (Shipment s : shipments) {
            if (s.getTrackingNumber() == null || s.getTrackingNumber().isBlank()) {
                issues.add("Shipment #" + s.getShipmentNumber() + " is missing tracking number.");
            }
            if (s.getStatus() == ShipmentStatus.DELIVERED && s.getDeliveredAt() == null) {
                issues.add("Shipment #" + s.getShipmentNumber() + " is marked DELIVERED but delivered_at is null.");
            }
        }

        // Check allocation invariants
        for (FulfillmentAllocation fa : allocations) {
            if (fa.getPickedQuantity() > fa.getAllocatedQuantity()) {
                issues.add("Allocation #" + fa.getAllocationNumber() + " has pickedQty (" + fa.getPickedQuantity() + ") > allocatedQty (" + fa.getAllocatedQuantity() + ")");
            }
            if (fa.getPackedQuantity() > fa.getPickedQuantity()) {
                issues.add("Allocation #" + fa.getAllocationNumber() + " has packedQty (" + fa.getPackedQuantity() + ") > pickedQty (" + fa.getPickedQuantity() + ")");
            }
        }

        // Check packages with missing items
        for (FulfillmentPackage pkg : packages) {
            if (pkg.getItems().isEmpty()) {
                issues.add("Package #" + pkg.getPackageNumber() + " has zero line items.");
            }
        }

        health.setIssuesCount(issues.size());
        health.setIssuesList(issues);
        health.setStatus(issues.isEmpty() ? "HEALTHY" : "ISSUES_DETECTED");

        return health;
    }

    private String generateAllocationNumber() {
        String datePart = LocalDate.now().format(DATE_FMT);
        int seq = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format(Locale.ROOT, "FA-%s-%04d", datePart, seq);
    }

    public FulfillmentAllocationDTO toAllocationDTO(FulfillmentAllocation fa) {
        FulfillmentAllocationDTO dto = new FulfillmentAllocationDTO();
        dto.setId(fa.getId());
        dto.setAllocationNumber(fa.getAllocationNumber());
        if (fa.getOrder() != null) {
            dto.setOrderId(fa.getOrder().getId());
            dto.setOrderNumber(fa.getOrder().getOrderNumber());
        }
        if (fa.getOrderItem() != null) {
            dto.setOrderItemId(fa.getOrderItem().getId());
            dto.setProductName(fa.getOrderItem().getProductName());
            if (fa.getOrderItem().getFertilizer() != null) {
                dto.setFertilizerId(fa.getOrderItem().getFertilizer().getId());
            }
        }
        if (fa.getWarehouse() != null) {
            dto.setWarehouseId(fa.getWarehouse().getId());
            dto.setWarehouseCode(fa.getWarehouse().getWarehouseCode());
            dto.setWarehouseName(fa.getWarehouse().getWarehouseName());
        }
        if (fa.getLocation() != null) {
            dto.setLocationId(fa.getLocation().getId());
            dto.setLocationCode(fa.getLocation().getLocationCode());
            dto.setLocationName(fa.getLocation().getLocationName());
        }
        if (fa.getBatch() != null) {
            dto.setBatchId(fa.getBatch().getId());
            dto.setBatchNumber(fa.getBatch().getBatchNumber());
        }
        dto.setAllocatedQuantity(fa.getAllocatedQuantity());
        dto.setPickedQuantity(fa.getPickedQuantity());
        dto.setPackedQuantity(fa.getPackedQuantity());
        dto.setStatus(fa.getStatus());
        dto.setAllocatedAt(fa.getAllocatedAt());
        dto.setPickedAt(fa.getPickedAt());
        dto.setPackedAt(fa.getPackedAt());
        return dto;
    }

    public PickListDTO toPickListDTO(PickList pl) {
        PickListDTO dto = new PickListDTO();
        dto.setId(pl.getId());
        dto.setPickListNumber(pl.getPickListNumber());
        if (pl.getWarehouse() != null) {
            dto.setWarehouseId(pl.getWarehouse().getId());
            dto.setWarehouseCode(pl.getWarehouse().getWarehouseCode());
            dto.setWarehouseName(pl.getWarehouse().getWarehouseName());
        }
        if (pl.getOrder() != null) {
            dto.setOrderId(pl.getOrder().getId());
            dto.setOrderNumber(pl.getOrder().getOrderNumber());
        }
        dto.setAssignedUser(pl.getAssignedUser());
        dto.setStatus(pl.getStatus() != null ? pl.getStatus().name() : "CREATED");
        dto.setNotes(pl.getNotes());
        dto.setStartedAt(pl.getStartedAt());
        dto.setCompletedAt(pl.getCompletedAt());
        dto.setCreatedAt(pl.getCreatedAt());

        if (pl.getItems() != null) {
            dto.setItems(pl.getItems().stream().map(this::toPickListItemDTO).collect(Collectors.toList()));
        }
        return dto;
    }

    public PickListItemDTO toPickListItemDTO(PickListItem item) {
        PickListItemDTO dto = new PickListItemDTO();
        dto.setId(item.getId());
        if (item.getOrderItem() != null) {
            dto.setOrderItemId(item.getOrderItem().getId());
            dto.setProductName(item.getOrderItem().getProductName());
            dto.setSku(item.getOrderItem().getSku());
        }
        if (item.getFertilizer() != null) {
            dto.setFertilizerId(item.getFertilizer().getId());
        }
        if (item.getBatch() != null) {
            dto.setBatchId(item.getBatch().getId());
            dto.setBatchNumber(item.getBatch().getBatchNumber());
        }
        if (item.getWarehouse() != null) {
            dto.setWarehouseId(item.getWarehouse().getId());
            dto.setWarehouseCode(item.getWarehouse().getWarehouseCode());
        }
        if (item.getLocation() != null) {
            dto.setLocationId(item.getLocation().getId());
            dto.setLocationCode(item.getLocation().getLocationCode());
            dto.setLocationName(item.getLocation().getLocationName());
        }
        dto.setAllocationId(item.getAllocationId());
        dto.setRequiredQuantity(item.getRequiredQuantity());
        dto.setPickedQuantity(item.getPickedQuantity());
        dto.setPicker(item.getPicker());
        dto.setPickedAt(item.getPickedAt());
        dto.setStatus(item.getStatus());
        return dto;
    }

    public FulfillmentPackageDTO toPackageDTO(FulfillmentPackage pkg) {
        FulfillmentPackageDTO dto = new FulfillmentPackageDTO();
        dto.setId(pkg.getId());
        dto.setPackageNumber(pkg.getPackageNumber());
        if (pkg.getOrder() != null) {
            dto.setOrderId(pkg.getOrder().getId());
            dto.setOrderNumber(pkg.getOrder().getOrderNumber());
        }
        if (pkg.getWarehouse() != null) {
            dto.setWarehouseId(pkg.getWarehouse().getId());
            dto.setWarehouseCode(pkg.getWarehouse().getWarehouseCode());
            dto.setWarehouseName(pkg.getWarehouse().getWarehouseName());
        }
        dto.setPackageStatus(pkg.getPackageStatus() != null ? pkg.getPackageStatus().name() : "CREATED");
        dto.setActualWeight(pkg.getActualWeight());
        dto.setLengthCm(pkg.getLengthCm());
        dto.setWidthCm(pkg.getWidthCm());
        dto.setHeightCm(pkg.getHeightCm());
        dto.setVolumetricWeight(pkg.getVolumetricWeight());
        dto.setChargeableWeight(pkg.getChargeableWeight());
        dto.setPackagingType(pkg.getPackagingType());
        dto.setPackedBy(pkg.getPackedBy());
        dto.setPackedAt(pkg.getPackedAt());
        dto.setTrackingNumber(pkg.getTrackingNumber());
        dto.setCourier(pkg.getCourier());
        dto.setNotes(pkg.getNotes());
        dto.setCreatedAt(pkg.getCreatedAt());

        if (pkg.getItems() != null) {
            dto.setItems(pkg.getItems().stream().map(this::toPackageItemDTO).collect(Collectors.toList()));
        }
        return dto;
    }

    public FulfillmentPackageItemDTO toPackageItemDTO(FulfillmentPackageItem item) {
        FulfillmentPackageItemDTO dto = new FulfillmentPackageItemDTO();
        dto.setId(item.getId());
        if (item.getFulfillmentPackage() != null) {
            dto.setPackageId(item.getFulfillmentPackage().getId());
        }
        if (item.getOrderItem() != null) {
            dto.setOrderItemId(item.getOrderItem().getId());
            dto.setProductName(item.getOrderItem().getProductName());
            dto.setSku(item.getOrderItem().getSku());
            if (item.getOrderItem().getFertilizer() != null) {
                dto.setFertilizerId(item.getOrderItem().getFertilizer().getId());
            }
        }
        if (item.getBatch() != null) {
            dto.setBatchId(item.getBatch().getId());
            dto.setBatchNumber(item.getBatch().getBatchNumber());
        }
        if (item.getWarehouse() != null) {
            dto.setWarehouseId(item.getWarehouse().getId());
        }
        if (item.getLocation() != null) {
            dto.setLocationId(item.getLocation().getId());
            dto.setLocationCode(item.getLocation().getLocationCode());
        }
        dto.setAllocationId(item.getAllocationId());
        dto.setQuantity(item.getQuantity());
        return dto;
    }

    public ShipmentDTO toShipmentDTO(Shipment s) {
        ShipmentDTO dto = new ShipmentDTO();
        dto.setId(s.getId());
        dto.setShipmentNumber(s.getShipmentNumber());
        if (s.getOrder() != null) {
            dto.setOrderId(s.getOrder().getId());
            dto.setOrderNumber(s.getOrder().getOrderNumber());
        }
        dto.setPackageId(s.getPackageId());
        if (s.getWarehouse() != null) {
            dto.setWarehouseId(s.getWarehouse().getId());
            dto.setWarehouseCode(s.getWarehouse().getWarehouseCode());
            dto.setWarehouseName(s.getWarehouse().getWarehouseName());
        }
        if (s.getDeliveryPartner() != null) {
            dto.setDeliveryPartnerId(s.getDeliveryPartner().getId());
        }
        dto.setCourierName(s.getCourierName());
        dto.setCourierCode(s.getCourierCode());
        dto.setTrackingNumber(s.getTrackingNumber());
        dto.setShippingMethod(s.getShippingMethod());
        dto.setShippingCost(s.getShippingCost());
        dto.setExpectedDeliveryDate(s.getExpectedDeliveryDate());
        dto.setActualDeliveryDate(s.getActualDeliveryDate());
        dto.setStatus(s.getStatus() != null ? s.getStatus().name() : "CREATED");
        dto.setDispatchedAt(s.getDispatchedAt());
        dto.setDeliveredAt(s.getDeliveredAt());
        dto.setDispatchedBy(s.getDispatchedBy());
        dto.setDeliveryFailureReason(s.getDeliveryFailureReason());
        dto.setDeliveryFailureNotes(s.getDeliveryFailureNotes());
        dto.setDeliveryProofReceiver(s.getDeliveryProofReceiver());
        dto.setNotes(s.getNotes());
        dto.setCreatedAt(s.getCreatedAt());

        if (s.getTrackingEvents() != null) {
            dto.setTrackingEvents(s.getTrackingEvents().stream().map(e -> {
                ShipmentTrackingEventDTO edto = new ShipmentTrackingEventDTO();
                edto.setId(e.getId());
                edto.setShipmentId(s.getId());
                edto.setStatus(e.getStatus());
                edto.setEventCode(e.getEventCode());
                edto.setDescription(e.getDescription());
                edto.setLocation(e.getLocation());
                edto.setEventTime(e.getEventTime());
                edto.setCreatedBy(e.getCreatedBy());
                return edto;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
