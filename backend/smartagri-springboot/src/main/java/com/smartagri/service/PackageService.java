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
public class PackageService {

    private static final Logger log = LoggerFactory.getLogger(PackageService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final FulfillmentPackageRepository packageRepository;
    private final FulfillmentPackageItemRepository packageItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final FertilizerBatchRepository batchRepository;
    private final FulfillmentAllocationRepository allocationRepository;
    private final FulfillmentService fulfillmentService;

    public PackageService(FulfillmentPackageRepository packageRepository,
                          FulfillmentPackageItemRepository packageItemRepository,
                          OrderRepository orderRepository,
                          OrderItemRepository orderItemRepository,
                          WarehouseRepository warehouseRepository,
                          WarehouseLocationRepository locationRepository,
                          FertilizerBatchRepository batchRepository,
                          FulfillmentAllocationRepository allocationRepository,
                          FulfillmentService fulfillmentService) {
        this.packageRepository = packageRepository;
        this.packageItemRepository = packageItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.batchRepository = batchRepository;
        this.allocationRepository = allocationRepository;
        this.fulfillmentService = fulfillmentService;
    }

    @Transactional
    public FulfillmentPackageDTO createPackage(CreatePackageRequest request, String username) {
        Long orderId = request.getOrderId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot create package for cancelled order #" + order.getOrderNumber());
        }

        if (order.getFulfillmentStatus() != FulfillmentStatus.PICKED &&
            order.getFulfillmentStatus() != FulfillmentStatus.PACKING) {
            throw new IllegalArgumentException("Cannot create package: Order #" + order.getOrderNumber() +
                    " is " + order.getFulfillmentStatus() + ". Items must be picked first.");
        }

        if (request.getLengthCm() != null && request.getLengthCm().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Package length must be greater than zero");
        }
        if (request.getWidthCm() != null && request.getWidthCm().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Package width must be greater than zero");
        }
        if (request.getHeightCm() != null && request.getHeightCm().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Package height must be greater than zero");
        }
        if (request.getActualWeight() != null && request.getActualWeight().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Package actual weight must be greater than zero");
        }

        List<FulfillmentAllocation> allocations = allocationRepository.findByOrderId(orderId);
        if (allocations.isEmpty()) {
            throw new IllegalStateException("No allocations found for order #" + order.getOrderNumber() + ". Please allocate first.");
        }

        Warehouse wh;
        if (request.getWarehouseId() != null) {
            wh = warehouseRepository.findById(request.getWarehouseId())
                    .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + request.getWarehouseId()));
        } else {
            wh = allocations.get(0).getWarehouse();
        }

        FulfillmentPackage pkg = new FulfillmentPackage();
        pkg.setPackageNumber(generatePackageNumber());
        pkg.setOrder(order);
        pkg.setWarehouse(wh);
        pkg.setActualWeight(request.getActualWeight() != null ? request.getActualWeight() : java.math.BigDecimal.ZERO);
        pkg.setLengthCm(request.getLengthCm() != null ? request.getLengthCm() : java.math.BigDecimal.ZERO);
        pkg.setWidthCm(request.getWidthCm() != null ? request.getWidthCm() : java.math.BigDecimal.ZERO);
        pkg.setHeightCm(request.getHeightCm() != null ? request.getHeightCm() : java.math.BigDecimal.ZERO);
        pkg.setPackagingType(request.getPackagingType() != null ? request.getPackagingType().trim() : "BOX");
        pkg.setNotes(request.getNotes());
        pkg.setPackageStatus(PackageStatus.PACKING);
        pkg.calculateWeights();

        FulfillmentPackage savedPkg = packageRepository.save(pkg);

        List<FulfillmentPackageItem> items = new ArrayList<>();

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CreatePackageRequest.PackageItemInput in : request.getItems()) {
                OrderItem oi = orderItemRepository.findById(in.getOrderItemId())
                        .orElseThrow(() -> new NoSuchElementException("OrderItem not found with id: " + in.getOrderItemId()));

                if (!oi.getOrder().getId().equals(orderId)) {
                    throw new IllegalArgumentException("OrderItem #" + in.getOrderItemId() + " does not belong to order #" + order.getOrderNumber());
                }

                int packQty = in.getQuantity() != null ? in.getQuantity() : 0;
                if (packQty <= 0) {
                    throw new IllegalArgumentException("Packed quantity must be positive");
                }

                // Find matching allocation
                FulfillmentAllocation matchingAlloc = null;
                if (in.getAllocationId() != null) {
                    matchingAlloc = allocationRepository.findById(in.getAllocationId()).orElse(null);
                }
                if (matchingAlloc == null) {
                    matchingAlloc = allocations.stream()
                            .filter(a -> a.getOrderItem().getId().equals(oi.getId()))
                            .findFirst()
                            .orElse(null);
                }

                if (matchingAlloc != null) {
                    int remainingToPack = matchingAlloc.getPickedQuantity() - matchingAlloc.getPackedQuantity();
                    if (packQty > remainingToPack) {
                        throw new IllegalArgumentException(String.format(
                                Locale.ROOT, "Over-packing rejected: requested quantity (%d) exceeds unpackaged picked quantity (%d)",
                                packQty, remainingToPack));
                    }
                    matchingAlloc.setPackedQuantity(matchingAlloc.getPackedQuantity() + packQty);
                    matchingAlloc.setPackedAt(LocalDateTime.now());
                    if (matchingAlloc.getPackedQuantity() >= matchingAlloc.getAllocatedQuantity()) {
                        matchingAlloc.setStatus("PACKED");
                    }
                    allocationRepository.save(matchingAlloc);
                }

                FulfillmentPackageItem pi = new FulfillmentPackageItem();
                pi.setFulfillmentPackage(savedPkg);
                pi.setOrderItem(oi);
                pi.setBatch(in.getBatchId() != null ? batchRepository.findById(in.getBatchId()).orElse(null) : (matchingAlloc != null ? matchingAlloc.getBatch() : null));
                pi.setWarehouse(in.getWarehouseId() != null ? warehouseRepository.findById(in.getWarehouseId()).orElse(null) : wh);
                pi.setLocation(in.getLocationId() != null ? locationRepository.findById(in.getLocationId()).orElse(null) : (matchingAlloc != null ? matchingAlloc.getLocation() : null));
                pi.setAllocationId(matchingAlloc != null ? matchingAlloc.getId() : in.getAllocationId());
                pi.setQuantity(packQty);

                FulfillmentPackageItem savedItem = packageItemRepository.save(pi);
                items.add(savedItem);
            }
        } else {
            // Auto-pack all remaining unpacked allocations
            for (FulfillmentAllocation alloc : allocations) {
                int unpackaged = alloc.getPickedQuantity() - alloc.getPackedQuantity();
                if (unpackaged <= 0) continue;

                alloc.setPackedQuantity(alloc.getPickedQuantity());
                alloc.setPackedAt(LocalDateTime.now());
                if (alloc.getPackedQuantity() >= alloc.getAllocatedQuantity()) {
                    alloc.setStatus("PACKED");
                }
                allocationRepository.save(alloc);

                FulfillmentPackageItem pi = new FulfillmentPackageItem();
                pi.setFulfillmentPackage(savedPkg);
                pi.setOrderItem(alloc.getOrderItem());
                pi.setBatch(alloc.getBatch());
                pi.setWarehouse(alloc.getWarehouse());
                pi.setLocation(alloc.getLocation());
                pi.setAllocationId(alloc.getId());
                pi.setQuantity(unpackaged);

                FulfillmentPackageItem savedItem = packageItemRepository.save(pi);
                items.add(savedItem);
            }
        }

        savedPkg.setItems(items);

        order.setFulfillmentStatus(FulfillmentStatus.PACKING);
        orderRepository.save(order);

        log.info("Created Package #{} for order #{} with {} items (chargeable weight: {} kg)",
                savedPkg.getPackageNumber(), order.getOrderNumber(), items.size(), savedPkg.getChargeableWeight());

        return fulfillmentService.toPackageDTO(savedPkg);
    }

    @Transactional
    public FulfillmentPackageDTO markPackagePacked(Long packageId, String username) {
        FulfillmentPackage pkg = packageRepository.findByIdWithLock(packageId)
                .orElseThrow(() -> new NoSuchElementException("Package not found with id: " + packageId));

        pkg.setPackageStatus(PackageStatus.PACKED);
        pkg.setPackedBy(username != null ? username : "packer");
        pkg.setPackedAt(LocalDateTime.now());

        FulfillmentPackage saved = packageRepository.save(pkg);

        Order order = saved.getOrder();
        if (order != null) {
            List<FulfillmentAllocation> allocations = allocationRepository.findByOrderId(order.getId());
            boolean allPacked = allocations.stream()
                    .allMatch(a -> a.getPackedQuantity() != null && a.getPackedQuantity() >= a.getAllocatedQuantity());
            if (allPacked) {
                order.setFulfillmentStatus(FulfillmentStatus.PACKED);
                if (order.getOrderStatus() == OrderStatus.PROCESSING || order.getOrderStatus() == OrderStatus.CONFIRMED) {
                    order.setOrderStatus(OrderStatus.PACKED);
                }
                orderRepository.save(order);
            }
        }

        log.info("Marked package #{} as PACKED", saved.getPackageNumber());
        return fulfillmentService.toPackageDTO(saved);
    }

    @Transactional(readOnly = true)
    public FulfillmentPackageDTO getPackageById(Long id) {
        FulfillmentPackage pkg = packageRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Package not found with id: " + id));
        return fulfillmentService.toPackageDTO(pkg);
    }

    @Transactional(readOnly = true)
    public List<FulfillmentPackageDTO> getPackages(String status, Long orderId, Long warehouseId) {
        List<FulfillmentPackage> list;
        if (orderId != null) {
            list = packageRepository.findByOrderId(orderId);
        } else if (warehouseId != null) {
            list = packageRepository.findByWarehouseId(warehouseId);
        } else if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            try {
                PackageStatus st = PackageStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
                list = packageRepository.findByPackageStatus(st);
            } catch (IllegalArgumentException e) {
                list = packageRepository.findAll();
            }
        } else {
            list = packageRepository.findAll();
        }

        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return list.stream().map(fulfillmentService::toPackageDTO).collect(Collectors.toList());
    }

    private String generatePackageNumber() {
        String datePart = LocalDate.now().format(DATE_FMT);
        int seq = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format(Locale.ROOT, "PKG-%s-%04d", datePart, seq);
    }
}
