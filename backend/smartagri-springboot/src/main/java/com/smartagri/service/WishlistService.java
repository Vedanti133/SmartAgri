package com.smartagri.service;

import com.smartagri.dto.WishlistDTO;
import com.smartagri.dto.WishlistItemDTO;
import com.smartagri.entity.Fertilizer;
import com.smartagri.entity.User;
import com.smartagri.entity.Wishlist;
import com.smartagri.entity.WishlistItem;
import com.smartagri.repository.FertilizerRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.repository.WishlistItemRepository;
import com.smartagri.repository.WishlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WishlistService {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final FertilizerRepository fertilizerRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    public WishlistService(WishlistRepository wishlistRepository,
                           WishlistItemRepository wishlistItemRepository,
                           FertilizerRepository fertilizerRepository,
                           UserRepository userRepository,
                           CartService cartService) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.fertilizerRepository = fertilizerRepository;
        this.userRepository = userRepository;
        this.cartService = cartService;
    }

    @Transactional
    public Wishlist getOrCreateWishlistEntity(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
                    Wishlist wishlist = new Wishlist(user);
                    return wishlistRepository.save(wishlist);
                });
    }

    @Transactional
    public WishlistDTO getWishlist(Long userId) {
        Wishlist wishlist = getOrCreateWishlistEntity(userId);
        return buildWishlistDTO(wishlist);
    }

    @Transactional(readOnly = true)
    public List<Long> getWishlistProductIds(Long userId) {
        List<WishlistItem> items = wishlistItemRepository.findByWishlistUserId(userId);
        return items.stream()
                .map(item -> item.getFertilizer().getId())
                .collect(Collectors.toList());
    }

    @Transactional
    public WishlistDTO addItem(Long userId, Long fertilizerId) {
        if (fertilizerId == null) {
            throw new IllegalArgumentException("Fertilizer ID cannot be null");
        }

        Fertilizer fertilizer = fertilizerRepository.findById(fertilizerId)
                .orElseThrow(() -> new IllegalArgumentException("Fertilizer not found with id: " + fertilizerId));

        Wishlist wishlist = getOrCreateWishlistEntity(userId);

        WishlistItem existing = wishlistItemRepository.findByWishlistIdAndFertilizerId(wishlist.getId(), fertilizerId)
                .orElse(null);

        if (existing == null) {
            WishlistItem newItem = new WishlistItem(wishlist, fertilizer);
            wishlistItemRepository.save(newItem);
            wishlist.getItems().add(newItem);
        }

        return buildWishlistDTO(wishlist);
    }

    @Transactional
    public WishlistDTO removeItem(Long userId, Long wishlistItemId) {
        WishlistItem item = wishlistItemRepository.findByIdAndWishlistUserId(wishlistItemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Wishlist item not found with id: " + wishlistItemId));

        Wishlist wishlist = item.getWishlist();
        wishlist.getItems().remove(item);
        wishlistItemRepository.delete(item);

        return getWishlist(userId);
    }

    @Transactional
    public WishlistDTO removeItemByProduct(Long userId, Long fertilizerId) {
        WishlistItem item = wishlistItemRepository.findByFertilizerIdAndWishlistUserId(fertilizerId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Product is not in your wishlist: " + fertilizerId));

        Wishlist wishlist = item.getWishlist();
        wishlist.getItems().remove(item);
        wishlistItemRepository.delete(item);

        return getWishlist(userId);
    }

    @Transactional
    public WishlistDTO moveToCart(Long userId, Long wishlistItemId) {
        WishlistItem item = wishlistItemRepository.findByIdAndWishlistUserId(wishlistItemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Wishlist item not found with id: " + wishlistItemId));

        Fertilizer fert = item.getFertilizer();

        // 1. Add to Cart (validates active status and available stock)
        cartService.addItem(userId, fert.getId(), 1);

        // 2. Remove from Wishlist
        Wishlist wishlist = item.getWishlist();
        wishlist.getItems().remove(item);
        wishlistItemRepository.delete(item);

        return getWishlist(userId);
    }

    @Transactional
    public WishlistDTO moveToCartByProduct(Long userId, Long fertilizerId) {
        WishlistItem item = wishlistItemRepository.findByFertilizerIdAndWishlistUserId(fertilizerId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Product is not in your wishlist: " + fertilizerId));

        // 1. Add to Cart
        cartService.addItem(userId, fertilizerId, 1);

        // 2. Remove from Wishlist
        Wishlist wishlist = item.getWishlist();
        wishlist.getItems().remove(item);
        wishlistItemRepository.delete(item);

        return getWishlist(userId);
    }

    private WishlistDTO buildWishlistDTO(Wishlist wishlist) {
        WishlistDTO dto = new WishlistDTO();
        dto.setId(wishlist.getId());
        dto.setUserId(wishlist.getUser() != null ? wishlist.getUser().getId() : null);

        List<WishlistItemDTO> itemDTOs = new ArrayList<>();

        if (wishlist.getItems() != null) {
            for (WishlistItem item : wishlist.getItems()) {
                Fertilizer fert = item.getFertilizer();
                if (fert == null) continue;

                BigDecimal unitPrice = fert.getSellingPrice() != null ? fert.getSellingPrice() : BigDecimal.ZERO;
                unitPrice = unitPrice.setScale(2, RoundingMode.HALF_UP);

                BigDecimal discountPercent = fert.getDiscountPercent() != null ? fert.getDiscountPercent() : BigDecimal.ZERO;
                BigDecimal discountFactor = HUNDRED.subtract(discountPercent).divide(HUNDRED, 4, RoundingMode.HALF_UP);
                BigDecimal effectivePrice = unitPrice.multiply(discountFactor).setScale(2, RoundingMode.HALF_UP);

                int stock = fert.getStock() != null ? fert.getStock() : 0;

                WishlistItemDTO itemDTO = new WishlistItemDTO();
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
                itemDTO.setAvailableStock(stock);
                itemDTO.setInStock(stock > 0 && fert.isActive());
                itemDTO.setActive(fert.isActive());
                itemDTO.setCreatedAt(item.getCreatedAt());

                itemDTOs.add(itemDTO);
            }
        }

        dto.setItems(itemDTOs);
        dto.setTotalItemsCount(itemDTOs.size());
        return dto;
    }
}
