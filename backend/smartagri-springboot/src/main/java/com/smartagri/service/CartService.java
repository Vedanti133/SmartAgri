package com.smartagri.service;

import com.smartagri.dto.CartDTO;
import com.smartagri.dto.CartItemDTO;
import com.smartagri.entity.Cart;
import com.smartagri.entity.CartItem;
import com.smartagri.entity.Fertilizer;
import com.smartagri.entity.User;
import com.smartagri.repository.CartItemRepository;
import com.smartagri.repository.CartRepository;
import com.smartagri.repository.FertilizerRepository;
import com.smartagri.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.05"); // 5% GST
    private static final BigDecimal TAX_PERCENT = new BigDecimal("5.00");

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final FertilizerRepository fertilizerRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository,
                       CartItemRepository cartItemRepository,
                       FertilizerRepository fertilizerRepository,
                       UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.fertilizerRepository = fertilizerRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Cart getOrCreateCartEntity(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
                    Cart cart = new Cart(user);
                    return cartRepository.save(cart);
                });
    }

    @Transactional
    public CartDTO getCart(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);
        return buildCartDTO(cart);
    }

    @Transactional
    public CartDTO addItem(Long userId, Long fertilizerId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        Fertilizer fertilizer = fertilizerRepository.findById(fertilizerId)
                .orElseThrow(() -> new IllegalArgumentException("Fertilizer not found with id: " + fertilizerId));

        if (!fertilizer.isActive()) {
            throw new IllegalArgumentException("Product is currently inactive and cannot be purchased");
        }

        int availableStock = fertilizer.getStock() != null ? fertilizer.getStock() : 0;
        if (availableStock <= 0) {
            throw new IllegalArgumentException("Product '" + fertilizer.getName() + "' is out of stock");
        }

        Cart cart = getOrCreateCartEntity(userId);

        CartItem existingItem = cartItemRepository.findByCartIdAndFertilizerId(cart.getId(), fertilizerId)
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + quantity;
            if (newQuantity > availableStock) {
                throw new IllegalArgumentException("Cannot add " + quantity + " more. Total requested (" + newQuantity
                        + ") exceeds available stock (" + availableStock + ")");
            }
            existingItem.setQuantity(newQuantity);
            cartItemRepository.save(existingItem);
        } else {
            if (quantity > availableStock) {
                throw new IllegalArgumentException("Requested quantity (" + quantity
                        + ") exceeds available stock (" + availableStock + ")");
            }
            CartItem newItem = new CartItem(cart, fertilizer, quantity);
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
        }

        return buildCartDTO(cart);
    }

    @Transactional
    public CartDTO updateItem(Long userId, Long cartItemId, Integer quantity) {
        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        CartItem item = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found with id: " + cartItemId));

        int availableStock = item.getFertilizer().getStock() != null ? item.getFertilizer().getStock() : 0;
        if (quantity > availableStock) {
            throw new IllegalArgumentException("Requested quantity (" + quantity
                    + ") exceeds available stock (" + availableStock + ")");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);

        return getCart(userId);
    }

    @Transactional
    public CartDTO removeItem(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findByIdAndCartUserId(cartItemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Cart item not found with id: " + cartItemId));

        Cart cart = item.getCart();
        cart.getItems().remove(item);
        cartItemRepository.delete(item);

        return getCart(userId);
    }

    @Transactional
    public CartDTO clearCart(Long userId) {
        Cart cart = getOrCreateCartEntity(userId);
        cart.getItems().clear();
        cartRepository.save(cart);
        return buildCartDTO(cart);
    }

    private CartDTO buildCartDTO(Cart cart) {
        CartDTO dto = new CartDTO();
        dto.setId(cart.getId());
        dto.setUserId(cart.getUser() != null ? cart.getUser().getId() : null);

        List<CartItemDTO> itemDTOs = new ArrayList<>();
        int totalItemsCount = 0;
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal netAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                Fertilizer fert = item.getFertilizer();
                int qty = item.getQuantity() != null ? item.getQuantity() : 1;
                totalItemsCount += qty;

                BigDecimal unitPrice = fert.getSellingPrice() != null ? fert.getSellingPrice() : BigDecimal.ZERO;
                unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);

                BigDecimal discountPercent = fert.getDiscountPercent() != null ? fert.getDiscountPercent() : BigDecimal.ZERO;

                // effectivePrice = unitPrice * (100 - discountPercent) / 100
                BigDecimal discountFactor = HUNDRED.subtract(discountPercent).divide(HUNDRED, 4, RoundingMode.HALF_UP);
                BigDecimal effectivePrice = unitPrice.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);

                BigDecimal lineGross = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal lineNet = effectivePrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);

                subtotal = subtotal.add(lineGross);
                netAmount = netAmount.add(lineNet);

                CartItemDTO itemDTO = new CartItemDTO();
                itemDTO.setId(item.getId());
                itemDTO.setFertilizerId(fert.getId());
                itemDTO.setName(fert.getName());
                itemDTO.setSku(fert.getSku());
                itemDTO.setBrand(fert.getBrand());
                itemDTO.setImageUrl(fert.getImageUrl());
                itemDTO.setCategoryName(fert.getCategory() != null ? fert.getCategory().getName() : "");
                itemDTO.setNpkRatio(fert.getNpkRatio());
                itemDTO.setUnitPrice(unitPrice);
                itemDTO.setDiscountPercent(discountPercent);
                itemDTO.setEffectivePrice(effectivePrice);
                itemDTO.setQuantity(qty);
                int stock = fert.getStock() != null ? fert.getStock() : 0;
                itemDTO.setAvailableStock(stock);
                itemDTO.setLineTotal(lineNet);

                if (!fert.isActive()) {
                    itemDTO.setAvailable(false);
                    itemDTO.setStatusMessage("This product is currently unavailable.");
                } else if (stock <= 0) {
                    itemDTO.setAvailable(false);
                    itemDTO.setStatusMessage("This product is currently out of stock.");
                } else if (qty > stock) {
                    itemDTO.setStatusMessage("Only " + stock + " units currently available in stock.");
                }

                itemDTOs.add(itemDTO);

            }
        }

        BigDecimal discountAmount = subtotal.subtract(netAmount);
        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            discountAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal taxAmount = netAmount.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = netAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        dto.setItems(itemDTOs);
        dto.setTotalItemsCount(totalItemsCount);
        dto.setUniqueItemsCount(itemDTOs.size());
        dto.setSubtotal(subtotal);
        dto.setDiscountAmount(discountAmount);
        dto.setNetAmount(netAmount);
        dto.setTaxPercent(TAX_PERCENT);
        dto.setTaxAmount(taxAmount);
        dto.setGrandTotal(grandTotal);

        return dto;
    }
}
