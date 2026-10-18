package com.smartagri.service;

import com.smartagri.dto.*;
import com.smartagri.entity.*;
import com.smartagri.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.05"); // 5% GST
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${app.order.free-shipping-threshold:1000.00}")
    private BigDecimal freeShippingThreshold = new BigDecimal("1000.00");

    @Value("${app.order.standard-shipping-fee:50.00}")
    private BigDecimal standardShippingFee = new BigDecimal("50.00");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final FertilizerRepository fertilizerRepository;
    private final UserRepository userRepository;
    private final DeliveryRepository deliveryRepository;
    private final PaymentRepository paymentRepository;
    private final DeliveryService deliveryService;
    private final PaymentService paymentService;
    private final InventoryService inventoryService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        CartRepository cartRepository,
                        AddressRepository addressRepository,
                        FertilizerRepository fertilizerRepository,
                        UserRepository userRepository,
                        DeliveryRepository deliveryRepository,
                        PaymentRepository paymentRepository,
                        @org.springframework.context.annotation.Lazy DeliveryService deliveryService,
                        @org.springframework.context.annotation.Lazy PaymentService paymentService,
                        @org.springframework.context.annotation.Lazy InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.fertilizerRepository = fertilizerRepository;
        this.userRepository = userRepository;
        this.deliveryRepository = deliveryRepository;
        this.paymentRepository = paymentRepository;
        this.deliveryService = deliveryService;
        this.paymentService = paymentService;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public OrderDTO createOrder(Long userId, CreateOrderRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart not found for user: " + userId));

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new IllegalArgumentException("Your shopping cart is empty. Please add fertilizers before checkout.");
        }

        Address address = addressRepository.findByIdAndUserId(request.getAddressId(), userId)
                .orElseThrow(() -> new IllegalArgumentException("Selected delivery address does not exist or does not belong to you."));

        // Generate unique order number
        String orderNumber = generateUniqueOrderNumber();

        // Financial totals using BigDecimal (ROUND_HALF_UP, 2 decimals)
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        // Pre-validate stock and compute financial snapshot
        List<OrderItem> orderItemsToSave = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Fertilizer fert = fertilizerRepository.findById(cartItem.getFertilizer().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + cartItem.getFertilizer().getId()));

            if (!fert.isActive()) {
                throw new IllegalArgumentException("Product '" + fert.getName() + "' is currently inactive and cannot be ordered.");
            }

            int currentStock = fert.getStock() != null ? fert.getStock() : 0;
            int requestedQty = cartItem.getQuantity() != null ? cartItem.getQuantity() : 1;

            if (currentStock < requestedQty) {
                throw new IllegalArgumentException("Insufficient stock for product '" + fert.getName() +
                        "'. Requested: " + requestedQty + ", available in stock: " + currentStock);
            }

            // Calculations
            BigDecimal unitPrice = fert.getSellingPrice() != null ? fert.getSellingPrice().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            BigDecimal discountPercent = fert.getDiscountPercent() != null ? fert.getDiscountPercent().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            BigDecimal discountFactor = HUNDRED.subtract(discountPercent).divide(HUNDRED, 4, RoundingMode.HALF_UP);
            BigDecimal discountedUnitPrice = unitPrice.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);

            BigDecimal lineGross = unitPrice.multiply(BigDecimal.valueOf(requestedQty)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineNet = discountedUnitPrice.multiply(BigDecimal.valueOf(requestedQty)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal itemDiscount = lineGross.subtract(lineNet);
            BigDecimal itemTax = lineNet.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTotal = lineNet.add(itemTax).setScale(2, RoundingMode.HALF_UP);

            subtotal = subtotal.add(lineGross);
            discountAmount = discountAmount.add(itemDiscount);
            netAmount = netAmount.add(lineNet);
            taxAmount = taxAmount.add(itemTax);

            if (request.getExpectedItemPrices() != null && request.getExpectedItemPrices().containsKey(fert.getId())) {
                BigDecimal expected = request.getExpectedItemPrices().get(fert.getId());
                if (expected != null && expected.setScale(2, RoundingMode.HALF_UP).compareTo(unitPrice.setScale(2, RoundingMode.HALF_UP)) != 0
                        && expected.setScale(2, RoundingMode.HALF_UP).compareTo(discountedUnitPrice.setScale(2, RoundingMode.HALF_UP)) != 0) {
                    throw new IllegalArgumentException("One or more product prices have changed. Please review your cart.");
                }
            }

            // Build historical product snapshot
            OrderItem orderItem = new OrderItem();
            orderItem.setFertilizer(fert);
            orderItem.setProductName(fert.getName());
            orderItem.setSku(fert.getSku());
            orderItem.setBrand(fert.getBrand());
            orderItem.setImageUrl(fert.getImageUrl());
            orderItem.setCategoryName(fert.getCategory() != null ? fert.getCategory().getName() : "");
            orderItem.setUnit(fert.getUnit());
            orderItem.setWeightValue(fert.getWeightValue());
            orderItem.setQuantity(requestedQty);
            orderItem.setUnitPrice(unitPrice);
            orderItem.setDiscountPercent(discountPercent);
            orderItem.setDiscountAmount(itemDiscount);
            orderItem.setTaxPercent(new BigDecimal("5.00"));
            orderItem.setTaxAmount(itemTax);
            orderItem.setLineTotal(lineTotal);

            orderItemsToSave.add(orderItem);
        }

        // Shipping calculation rule: Subtotal >= freeShippingThreshold -> 0.00, else standardShippingFee
        BigDecimal shippingAmount = netAmount.compareTo(freeShippingThreshold) >= 0
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : standardShippingFee.setScale(2, RoundingMode.HALF_UP);

        BigDecimal grandTotal = netAmount.add(taxAmount).add(shippingAmount).setScale(2, RoundingMode.HALF_UP);

        // Price revalidation: check if customer checkout expectation matches authoritative calculation
        if (request.getExpectedSubtotal() != null && request.getExpectedSubtotal().setScale(2, RoundingMode.HALF_UP).compareTo(subtotal.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalArgumentException("One or more product prices have changed. Please review your cart.");
        }
        if (request.getExpectedGrandTotal() != null && request.getExpectedGrandTotal().setScale(2, RoundingMode.HALF_UP).compareTo(grandTotal.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalArgumentException("One or more product prices have changed. Please review your cart.");
        }

        // Build Order with address snapshot
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUser(user);
        order.setDeliveryAddress(address);

        // Address Snapshot
        order.setRecipientName(address.getFullName());
        order.setRecipientPhone(address.getPhone());
        order.setAddressLine1(address.getAddressLine1());
        order.setAddressLine2(address.getAddressLine2());
        order.setCity(address.getCity());
        order.setDistrict(address.getDistrict());
        order.setState(address.getState());
        order.setPincode(address.getPincode());
        order.setLandmark(address.getLandmark());
        order.setAddressType(address.getAddressType());

        order.setOrderStatus(OrderStatus.PENDING);
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setPaymentMethod(request.getPaymentMethod() != null && !request.getPaymentMethod().isBlank()
                ? request.getPaymentMethod().trim().toUpperCase(Locale.ROOT)
                : "COD");

        order.setSubtotal(subtotal);
        order.setDiscountAmount(discountAmount);
        order.setTaxAmount(taxAmount);
        order.setShippingAmount(shippingAmount);
        order.setGrandTotal(grandTotal);
        order.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);

        Order savedOrder = orderRepository.save(order);

        // Attach order to items and save, then allocate batch inventory via FEFO
        for (OrderItem item : orderItemsToSave) {
            item.setOrder(savedOrder);
            OrderItem savedItem = orderItemRepository.save(item);
            savedOrder.getItems().add(savedItem);

            inventoryService.deductStockForOrder(
                    savedItem.getFertilizer().getId(),
                    savedItem.getQuantity(),
                    savedOrder.getOrderNumber(),
                    user.getUsername(),
                    savedItem.getId()
            );
        }

        // Clear shopping cart
        cart.getItems().clear();
        cartRepository.save(cart);

        // Phase 5: Initialize Delivery and Payment records
        deliveryService.createInitialDelivery(savedOrder);
        paymentService.createPaymentForOrder(savedOrder, savedOrder.getPaymentMethod(), request.getPaymentReference());

        log.info("Order successfully created: {} for user: {}", savedOrder.getOrderNumber(), user.getUsername());
        return toOrderDTO(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getCustomerOrders(Long userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO getCustomerOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this order.");
        }
        return toOrderDTO(order);
    }

    @Transactional
    public OrderDTO cancelOrder(Long userId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found with id: " + orderId));
        if (!order.getUser().getId().equals(userId)) {
            throw new SecurityException("Access denied: You do not own this order.");
        }

        if (!order.getOrderStatus().canCancel()) {
            throw new IllegalArgumentException("Order #" + order.getOrderNumber() +
                    " cannot be cancelled in its current state (" + order.getOrderStatus() + ").");
        }

        // Restore product stock in MySQL via centralized InventoryService (Phase 10 & 11)
        for (OrderItem item : order.getItems()) {
            if (item.getFertilizer() != null) {
                inventoryService.restoreStockForOrder(
                        item.getFertilizer().getId(), item.getQuantity(), order.getOrderNumber(), order.getUser().getUsername(), item.getId());
            }
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
        if (order.getPaymentStatus() == PaymentStatus.PAID || order.getPaymentStatus() == PaymentStatus.SUCCESS) {
            order.setPaymentStatus(PaymentStatus.REFUNDED);
        }
        Order saved = orderRepository.save(order);

        paymentService.refundPaymentIfApplicable(saved);
        deliveryRepository.findByOrderId(saved.getId()).ifPresent(d -> {
            d.setDeliveryNotes("Order cancelled by customer.");
            deliveryRepository.save(d);
        });

        log.info("Order #{} has been cancelled by customer.", order.getOrderNumber());
        return toOrderDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrdersAdmin(String statusFilter) {
        if (statusFilter != null && !statusFilter.isBlank() && !"ALL".equalsIgnoreCase(statusFilter.trim())) {
            try {
                OrderStatus status = OrderStatus.valueOf(statusFilter.trim().toUpperCase(Locale.ROOT));
                return orderRepository.findByOrderStatusOrderByCreatedAtDesc(status).stream()
                        .map(this::toOrderDTO)
                        .collect(Collectors.toList());
            } catch (IllegalArgumentException e) {
                // Ignore invalid filter and fall back to all
            }
        }
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toOrderDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderDTO getOrderAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));
        return toOrderDTO(order);
    }

    @Transactional
    public OrderDTO updateOrderStatusAdmin(Long orderId, String newStatusStr) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found with id: " + orderId));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(newStatusStr.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid order status: " + newStatusStr);
        }

        if (!order.getOrderStatus().canTransitionTo(newStatus)) {
            throw new IllegalArgumentException("Invalid status transition from " + order.getOrderStatus() + " to " + newStatus);
        }

        // If transitioning to CANCELLED from cancellable status, restore stock
        if (newStatus == OrderStatus.CANCELLED && order.getOrderStatus().canCancel()) {
            for (OrderItem item : order.getItems()) {
                if (item.getFertilizer() != null) {
                    inventoryService.restoreStockForOrder(
                            item.getFertilizer().getId(), item.getQuantity(), order.getOrderNumber(), "admin", item.getId());
                }
            }
            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                order.setPaymentStatus(PaymentStatus.REFUNDED);
            }
        }

        // If delivered with COD, mark payment as paid
        if (newStatus == OrderStatus.DELIVERED && "COD".equalsIgnoreCase(order.getPaymentMethod())) {
            order.setPaymentStatus(PaymentStatus.PAID);
        }

        order.setOrderStatus(newStatus);
        Order saved = orderRepository.save(order);
        log.info("Admin updated Order #{} to status {}", order.getOrderNumber(), newStatus);

        // Phase 5: Sync delivery status and payment status when order status moves
        if (newStatus == OrderStatus.PACKED) {
            deliveryRepository.findByOrderId(order.getId()).ifPresent(d -> {
                if (d.getDeliveryStatus() == DeliveryStatus.NOT_ASSIGNED || d.getDeliveryStatus() == DeliveryStatus.ASSIGNED) {
                    d.setDeliveryStatus(DeliveryStatus.PACKED);
                    deliveryRepository.save(d);
                }
            });
        } else if (newStatus == OrderStatus.SHIPPED) {
            deliveryRepository.findByOrderId(order.getId()).ifPresent(d -> {
                if (d.getDeliveryStatus() != DeliveryStatus.SHIPPED) {
                    if (d.getTrackingNumber() == null) {
                        d.setTrackingNumber(deliveryService.generateTrackingNumber());
                    }
                    d.setDeliveryStatus(DeliveryStatus.SHIPPED);
                    d.setShippedAt(LocalDateTime.now());
                    deliveryRepository.save(d);
                }
            });
        } else if (newStatus == OrderStatus.DELIVERED) {
            deliveryRepository.findByOrderId(order.getId()).ifPresent(d -> {
                d.setDeliveryStatus(DeliveryStatus.DELIVERED);
                d.setDeliveredAt(LocalDateTime.now());
                deliveryRepository.save(d);
            });
            if ("COD".equalsIgnoreCase(order.getPaymentMethod())) {
                paymentService.updatePaymentStatusAdmin(order.getId(),
                        new com.smartagri.dto.UpdatePaymentStatusRequest("PAID", null, "Paid on delivery"));
            }
        } else if (newStatus == OrderStatus.CANCELLED) {
            paymentService.refundPaymentIfApplicable(order);
            deliveryRepository.findByOrderId(order.getId()).ifPresent(d -> {
                d.setDeliveryNotes("Order cancelled.");
                deliveryRepository.save(d);
            });
        }

        return toOrderDTO(saved);
    }

    @Transactional(readOnly = true)
    public OrderMetricsDTO getOrderMetricsAdmin() {
        return new OrderMetricsDTO(
                orderRepository.count(),
                orderRepository.countByOrderStatus(OrderStatus.PENDING),
                orderRepository.countByOrderStatus(OrderStatus.CONFIRMED),
                orderRepository.countByOrderStatus(OrderStatus.PROCESSING),
                orderRepository.countByOrderStatus(OrderStatus.PACKED),
                orderRepository.countByOrderStatus(OrderStatus.SHIPPED),
                orderRepository.countByOrderStatus(OrderStatus.DELIVERED),
                orderRepository.countByOrderStatus(OrderStatus.CANCELLED)
        );
    }

    @Transactional(readOnly = true)
    public FulfillmentMetricsDTO getFulfillmentMetricsAdmin() {
        FulfillmentMetricsDTO m = new FulfillmentMetricsDTO();
        m.setTotalOrders(orderRepository.count());
        m.setPendingOrders(orderRepository.countByOrderStatus(OrderStatus.PENDING));
        m.setConfirmedOrders(orderRepository.countByOrderStatus(OrderStatus.CONFIRMED));
        m.setProcessingOrders(orderRepository.countByOrderStatus(OrderStatus.PROCESSING));
        m.setPackedOrders(orderRepository.countByOrderStatus(OrderStatus.PACKED));
        m.setShippedOrders(orderRepository.countByOrderStatus(OrderStatus.SHIPPED));
        m.setDeliveredOrders(orderRepository.countByOrderStatus(OrderStatus.DELIVERED));
        m.setCancelledOrders(orderRepository.countByOrderStatus(OrderStatus.CANCELLED));

        m.setTotalDeliveries(deliveryRepository.count());
        m.setNotAssignedDeliveries(deliveryRepository.countByDeliveryStatus(DeliveryStatus.NOT_ASSIGNED));
        m.setAssignedDeliveries(deliveryRepository.countByDeliveryStatus(DeliveryStatus.ASSIGNED));
        m.setPackedDeliveries(deliveryRepository.countByDeliveryStatus(DeliveryStatus.PACKED));
        m.setShippedDeliveries(deliveryRepository.countByDeliveryStatus(DeliveryStatus.SHIPPED));
        m.setOutForDeliveryDeliveries(deliveryRepository.countByDeliveryStatus(DeliveryStatus.OUT_FOR_DELIVERY));
        m.setDeliveredDeliveries(deliveryRepository.countByDeliveryStatus(DeliveryStatus.DELIVERED));
        m.setFailedDeliveries(deliveryRepository.countByDeliveryStatus(DeliveryStatus.FAILED));
        m.setReturnedDeliveries(deliveryRepository.countByDeliveryStatus(DeliveryStatus.RETURNED));
        m.setDeliveredToday(deliveryRepository.countByDeliveredAtAfter(LocalDate.now().atStartOfDay()));

        m.setPendingPayments(paymentRepository.countByPaymentStatus(PaymentStatus.PENDING));
        m.setSuccessfulPayments(paymentRepository.countByPaymentStatus(PaymentStatus.PAID)
                + paymentRepository.countByPaymentStatus(PaymentStatus.SUCCESS));

        return m;
    }

    private String generateUniqueOrderNumber() {
        String datePart = LocalDate.now().format(DATE_FORMATTER);
        String candidate;
        int attempts = 0;
        do {
            int randomSeq = ThreadLocalRandom.current().nextInt(1000, 9999);
            candidate = String.format(Locale.ROOT, "SA-%s-%04d", datePart, randomSeq);
            attempts++;
            if (attempts > 50) {
                candidate = String.format(Locale.ROOT, "SA-%s-%04d", datePart, System.currentTimeMillis() % 100000);
                break;
            }
        } while (orderRepository.findByOrderNumber(candidate).isPresent());

        return candidate;
    }

    private OrderDTO toOrderDTO(Order o) {
        OrderDTO dto = new OrderDTO();
        dto.setId(o.getId());
        dto.setOrderNumber(o.getOrderNumber());

        User u = o.getUser();
        if (u != null) {
            dto.setUserId(u.getId());
            dto.setCustomerUsername(u.getUsername());
            dto.setCustomerName(u.getName());
            dto.setCustomerEmail(u.getEmail());
        }

        dto.setDeliveryAddressId(o.getDeliveryAddress() != null ? o.getDeliveryAddress().getId() : null);
        dto.setRecipientName(o.getRecipientName());
        dto.setRecipientPhone(o.getRecipientPhone());
        dto.setAddressLine1(o.getAddressLine1());
        dto.setAddressLine2(o.getAddressLine2());
        dto.setCity(o.getCity());
        dto.setDistrict(o.getDistrict());
        dto.setState(o.getState());
        dto.setPincode(o.getPincode());
        dto.setLandmark(o.getLandmark());
        dto.setAddressType(o.getAddressType());

        dto.setOrderStatus(o.getOrderStatus().name());
        dto.setFulfillmentStatus(o.getFulfillmentStatus() != null ? o.getFulfillmentStatus().name() : "UNALLOCATED");
        dto.setPaymentStatus(o.getPaymentStatus().name());
        dto.setPaymentMethod(o.getPaymentMethod());

        dto.setSubtotal(o.getSubtotal());
        dto.setDiscountAmount(o.getDiscountAmount());
        dto.setTaxAmount(o.getTaxAmount());
        dto.setShippingAmount(o.getShippingAmount());
        dto.setGrandTotal(o.getGrandTotal());

        dto.setNotes(o.getNotes());
        dto.setCanCancel(o.getOrderStatus().canCancel());
        dto.setCreatedAt(o.getCreatedAt());
        dto.setUpdatedAt(o.getUpdatedAt());

        int totalItemsCount = 0;
        List<OrderItemDTO> itemDTOs = new ArrayList<>();
        if (o.getItems() != null) {
            for (OrderItem item : o.getItems()) {
                totalItemsCount += (item.getQuantity() != null ? item.getQuantity() : 1);
                OrderItemDTO idto = new OrderItemDTO();
                idto.setId(item.getId());
                idto.setFertilizerId(item.getFertilizer() != null ? item.getFertilizer().getId() : null);
                idto.setProductName(item.getProductName());
                idto.setSku(item.getSku());
                idto.setBrand(item.getBrand());
                idto.setImageUrl(item.getImageUrl());
                idto.setCategoryName(item.getCategoryName());
                idto.setUnit(item.getUnit());
                idto.setWeightValue(item.getWeightValue());
                idto.setQuantity(item.getQuantity());
                idto.setUnitPrice(item.getUnitPrice());
                idto.setDiscountPercent(item.getDiscountPercent());
                idto.setDiscountAmount(item.getDiscountAmount());
                idto.setTaxPercent(item.getTaxPercent());
                idto.setTaxAmount(item.getTaxAmount());
                idto.setLineTotal(item.getLineTotal());
                itemDTOs.add(idto);
            }
        }

        dto.setTotalItemsCount(totalItemsCount);
        dto.setItems(itemDTOs);

        // Populate Phase 5 Delivery & Payment tracking details
        deliveryRepository.findByOrderId(o.getId()).ifPresent(d -> {
            dto.setDeliveryStatus(d.getDeliveryStatus().name());
            dto.setTrackingNumber(d.getTrackingNumber());
            dto.setDeliveryPartner(d.getDeliveryPartner());
            dto.setEstimatedDeliveryDate(d.getEstimatedDeliveryDate());
        });

        paymentRepository.findByOrderId(o.getId()).ifPresent(p -> {
            dto.setPaymentId(p.getId());
            dto.setTransactionId(p.getTransactionId());
            boolean canRetry = (p.getPaymentStatus() == PaymentStatus.FAILED ||
                    p.getPaymentStatus() == PaymentStatus.PENDING ||
                    p.getPaymentStatus() == PaymentStatus.INITIATED)
                    && o.getOrderStatus() != OrderStatus.CANCELLED;
            dto.setCanRetryPayment(canRetry);
        });

        return dto;
    }
}
