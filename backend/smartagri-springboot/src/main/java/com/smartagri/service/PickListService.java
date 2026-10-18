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
public class PickListService {

    private static final Logger log = LoggerFactory.getLogger(PickListService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PickListRepository pickListRepository;
    private final PickListItemRepository pickListItemRepository;
    private final OrderRepository orderRepository;
    private final WarehouseRepository warehouseRepository;
    private final FulfillmentAllocationRepository allocationRepository;
    private final FulfillmentService fulfillmentService;

    public PickListService(PickListRepository pickListRepository,
                           PickListItemRepository pickListItemRepository,
                           OrderRepository orderRepository,
                           WarehouseRepository warehouseRepository,
                           FulfillmentAllocationRepository allocationRepository,
                           FulfillmentService fulfillmentService) {
        this.pickListRepository = pickListRepository;
        this.pickListItemRepository = pickListItemRepository;
        this.orderRepository = orderRepository;
        this.warehouseRepository = warehouseRepository;
        this.allocationRepository = allocationRepository;
        this.fulfillmentService = fulfillmentService;
    }

    @Transactional
    public PickListDTO createPickList(CreatePickListRequest request, String username) {
        Long orderId = request.getOrderId();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Cannot create pick list for cancelled order #" + order.getOrderNumber());
        }

        // Ensure allocations exist and order is not UNALLOCATED
        List<FulfillmentAllocation> allocations = allocationRepository.findByOrderId(orderId);
        if (order.getFulfillmentStatus() == FulfillmentStatus.UNALLOCATED || allocations.isEmpty()) {
            throw new IllegalArgumentException("Cannot create pick list: Order #" + order.getOrderNumber() + " is UNALLOCATED. Please allocate inventory first.");
        }

        Warehouse wh;
        if (request.getWarehouseId() != null) {
            wh = warehouseRepository.findById(request.getWarehouseId())
                    .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + request.getWarehouseId()));
        } else {
            wh = allocations.get(0).getWarehouse();
        }

        PickList pl = new PickList();
        pl.setPickListNumber(generatePickListNumber());
        pl.setOrder(order);
        pl.setWarehouse(wh);
        pl.setNotes(request.getNotes());

        String picker = request.getAssignedPicker();
        if (picker != null && !picker.isBlank()) {
            pl.setAssignedUser(picker.trim());
            pl.setStatus(PickListStatus.ASSIGNED);
        } else {
            pl.setStatus(PickListStatus.CREATED);
        }

        PickList saved = pickListRepository.save(pl);

        List<PickListItem> items = new ArrayList<>();
        for (FulfillmentAllocation alloc : allocations) {
            PickListItem pli = new PickListItem();
            pli.setPickList(saved);
            pli.setOrderItem(alloc.getOrderItem());
            if (alloc.getOrderItem() != null && alloc.getOrderItem().getFertilizer() != null) {
                pli.setFertilizer(alloc.getOrderItem().getFertilizer());
            }
            pli.setBatch(alloc.getBatch());
            pli.setWarehouse(alloc.getWarehouse());
            pli.setLocation(alloc.getLocation());
            pli.setAllocationId(alloc.getId());
            pli.setRequiredQuantity(alloc.getAllocatedQuantity());
            pli.setPickedQuantity(0);
            pli.setStatus("PENDING");

            PickListItem savedItem = pickListItemRepository.save(pli);
            items.add(savedItem);
        }

        saved.setItems(items);

        order.setFulfillmentStatus(FulfillmentStatus.PICKING);
        orderRepository.save(order);

        log.info("Created PickList #{} for order #{} with {} items",
                saved.getPickListNumber(), order.getOrderNumber(), items.size());

        return fulfillmentService.toPickListDTO(saved);
    }

    @Transactional
    public PickListDTO assignPicker(Long pickListId, String picker) {
        PickList pl = pickListRepository.findByIdWithLock(pickListId)
                .orElseThrow(() -> new NoSuchElementException("PickList not found with id: " + pickListId));

        if (picker == null || picker.isBlank()) {
            throw new IllegalArgumentException("Picker username is required");
        }

        pl.setAssignedUser(picker.trim());
        if (pl.getStatus() == PickListStatus.CREATED) {
            pl.setStatus(PickListStatus.ASSIGNED);
        }

        PickList saved = pickListRepository.save(pl);
        log.info("Assigned picker {} to PickList #{}", picker, saved.getPickListNumber());
        return fulfillmentService.toPickListDTO(saved);
    }

    @Transactional
    public PickListDTO startPicking(Long pickListId, String picker) {
        PickList pl = pickListRepository.findByIdWithLock(pickListId)
                .orElseThrow(() -> new NoSuchElementException("PickList not found with id: " + pickListId));

        if (picker != null && !picker.isBlank()) {
            pl.setAssignedUser(picker.trim());
        }
        pl.setStatus(PickListStatus.IN_PROGRESS);
        if (pl.getStartedAt() == null) {
            pl.setStartedAt(LocalDateTime.now());
        }

        if (pl.getOrder() != null && pl.getOrder().getFulfillmentStatus() == FulfillmentStatus.ALLOCATED) {
            pl.getOrder().setFulfillmentStatus(FulfillmentStatus.PICKING);
            orderRepository.save(pl.getOrder());
        }

        PickList saved = pickListRepository.save(pl);
        log.info("Started picking for PickList #{}", saved.getPickListNumber());
        return fulfillmentService.toPickListDTO(saved);
    }

    @Transactional
    public PickListDTO confirmPick(Long pickListId, ConfirmPickRequest request, String username) {
        PickList pl = pickListRepository.findByIdWithLock(pickListId)
                .orElseThrow(() -> new NoSuchElementException("PickList not found with id: " + pickListId));

        LocalDateTime now = LocalDateTime.now();

        List<ConfirmPickRequest.PickItemConfirmation> confirmations = new ArrayList<>();
        if (request != null && request.getItems() != null && !request.getItems().isEmpty()) {
            confirmations.addAll(request.getItems());
        } else if (request != null && request.getItemId() != null && request.getPickedQuantity() != null) {
            confirmations.add(new ConfirmPickRequest.PickItemConfirmation(request.getItemId(), request.getPickedQuantity()));
        } else {
            // Auto-confirm all items in pick list
            List<PickListItem> allItems = pickListItemRepository.findByPickListId(pickListId);
            for (PickListItem itm : allItems) {
                confirmations.add(new ConfirmPickRequest.PickItemConfirmation(itm.getId(), itm.getRequiredQuantity()));
            }
        }

        for (ConfirmPickRequest.PickItemConfirmation conf : confirmations) {
            PickListItem item = pickListItemRepository.findByIdWithLock(conf.getItemId())
                    .orElseThrow(() -> new NoSuchElementException("PickListItem not found with id: " + conf.getItemId()));

            if (!item.getPickList().getId().equals(pickListId)) {
                throw new IllegalArgumentException("Item #" + conf.getItemId() + " does not belong to PickList #" + pickListId);
            }

            int newPicked = conf.getPickedQuantity() != null ? conf.getPickedQuantity() : 0;
            if (newPicked < 0) {
                throw new IllegalArgumentException("Picked quantity cannot be negative");
            }

            if (newPicked > item.getRequiredQuantity()) {
                throw new IllegalArgumentException(String.format(
                        Locale.ROOT, "Over-picking rejected: picked quantity (%d) exceeds required quantity (%d) for item #%d",
                        newPicked, item.getRequiredQuantity(), item.getId()));
            }

            // Check batch status
            if (item.getBatch() != null) {
                if (item.getBatch().getStatus() == BatchStatus.EXPIRED || item.getBatch().isExpired()) {
                    throw new IllegalStateException("Cannot pick from expired batch #" + item.getBatch().getBatchNumber());
                }
                if (item.getBatch().getStatus() == BatchStatus.BLOCKED) {
                    throw new IllegalStateException("Cannot pick from blocked batch #" + item.getBatch().getBatchNumber());
                }
            }

            // Check location status
            if (item.getLocation() != null && item.getLocation().getStatus() != LocationStatus.ACTIVE) {
                throw new IllegalStateException("Cannot pick from inactive/blocked location: " + item.getLocation().getLocationCode());
            }

            item.setPickedQuantity(newPicked);
            item.setPicker(username != null ? username : "picker");
            item.setPickedAt(now);
            if (newPicked >= item.getRequiredQuantity()) {
                item.setStatus("PICKED");
            }

            pickListItemRepository.save(item);

            // Update matching FulfillmentAllocation
            if (item.getAllocationId() != null) {
                allocationRepository.findByIdWithLock(item.getAllocationId()).ifPresent(alloc -> {
                    alloc.setPickedQuantity(newPicked);
                    alloc.setPickedAt(now);
                    if (newPicked >= alloc.getAllocatedQuantity()) {
                        alloc.setStatus("PICKED");
                    }
                    allocationRepository.save(alloc);
                });
            }
        }

        // Check if all items in pick list are completed
        boolean allPicked = pl.getItems().stream()
                .allMatch(i -> i.getPickedQuantity() != null && i.getPickedQuantity() >= i.getRequiredQuantity());

        if (allPicked) {
            pl.setStatus(PickListStatus.COMPLETED);
            if (pl.getCompletedAt() == null) {
                pl.setCompletedAt(now);
            }

            // Update order status if all allocations are picked
            Order order = pl.getOrder();
            if (order != null) {
                List<FulfillmentAllocation> allAlloc = allocationRepository.findByOrderId(order.getId());
                boolean allOrderAllocPicked = allAlloc.stream()
                        .allMatch(a -> a.getPickedQuantity() != null && a.getPickedQuantity() >= a.getAllocatedQuantity());
                if (allOrderAllocPicked) {
                    order.setFulfillmentStatus(FulfillmentStatus.PICKED);
                    orderRepository.save(order);
                }
            }
        } else {
            pl.setStatus(PickListStatus.IN_PROGRESS);
        }

        PickList saved = pickListRepository.save(pl);
        log.info("Confirmed pick for PickList #{} (status: {})", saved.getPickListNumber(), saved.getStatus());
        return fulfillmentService.toPickListDTO(saved);
    }

    @Transactional
    public PickListDTO completePickList(Long pickListId, String username) {
        PickList pl = pickListRepository.findByIdWithLock(pickListId)
                .orElseThrow(() -> new NoSuchElementException("PickList not found with id: " + pickListId));

        LocalDateTime now = LocalDateTime.now();
        for (PickListItem item : pl.getItems()) {
            if (item.getPickedQuantity() < item.getRequiredQuantity()) {
                item.setPickedQuantity(item.getRequiredQuantity());
                item.setPicker(username);
                item.setPickedAt(now);
                item.setStatus("PICKED");
                pickListItemRepository.save(item);

                if (item.getAllocationId() != null) {
                    allocationRepository.findById(item.getAllocationId()).ifPresent(a -> {
                        a.setPickedQuantity(item.getRequiredQuantity());
                        a.setPickedAt(now);
                        a.setStatus("PICKED");
                        allocationRepository.save(a);
                    });
                }
            }
        }

        pl.setStatus(PickListStatus.COMPLETED);
        pl.setCompletedAt(now);
        PickList saved = pickListRepository.save(pl);

        Order order = pl.getOrder();
        if (order != null) {
            order.setFulfillmentStatus(FulfillmentStatus.PICKED);
            orderRepository.save(order);
        }

        return fulfillmentService.toPickListDTO(saved);
    }

    @Transactional
    public PickListDTO cancelPickList(Long pickListId, String reason, String username) {
        PickList pl = pickListRepository.findByIdWithLock(pickListId)
                .orElseThrow(() -> new NoSuchElementException("PickList not found with id: " + pickListId));

        pl.setStatus(PickListStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            pl.setNotes((pl.getNotes() != null ? pl.getNotes() + " | " : "") + "Cancelled: " + reason);
        }

        for (PickListItem item : pl.getItems()) {
            item.setStatus("CANCELLED");
            pickListItemRepository.save(item);
        }

        PickList saved = pickListRepository.save(pl);
        return fulfillmentService.toPickListDTO(saved);
    }

    @Transactional(readOnly = true)
    public PickListDTO getPickListById(Long id) {
        PickList pl = pickListRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("PickList not found with id: " + id));
        return fulfillmentService.toPickListDTO(pl);
    }

    @Transactional(readOnly = true)
    public List<PickListDTO> getPickLists(String status, Long warehouseId, Long orderId) {
        List<PickList> list;
        if (orderId != null) {
            list = pickListRepository.findByOrderId(orderId);
        } else if (warehouseId != null) {
            list = pickListRepository.findByWarehouseId(warehouseId);
        } else if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
            try {
                PickListStatus st = PickListStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
                list = pickListRepository.findByStatus(st);
            } catch (IllegalArgumentException e) {
                list = pickListRepository.findAll();
            }
        } else {
            list = pickListRepository.findAll();
        }

        list.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return list.stream().map(fulfillmentService::toPickListDTO).collect(Collectors.toList());
    }

    private String generatePickListNumber() {
        String datePart = LocalDate.now().format(DATE_FMT);
        int seq = ThreadLocalRandom.current().nextInt(1000, 9999);
        return String.format(Locale.ROOT, "PL-%s-%04d", datePart, seq);
    }
}
