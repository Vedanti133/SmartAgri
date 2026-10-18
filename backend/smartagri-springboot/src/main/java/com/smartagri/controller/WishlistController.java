package com.smartagri.controller;

import com.smartagri.dto.AddToWishlistRequest;
import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.WishlistDTO;
import com.smartagri.service.WishlistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<WishlistDTO>> getWishlist(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.getWishlist(userId)));
    }

    @GetMapping("/product-ids")
    public ResponseEntity<ApiResponse<List<Long>>> getWishlistProductIds(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.ok(wishlistService.getWishlistProductIds(userId)));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<WishlistDTO>> addItem(HttpServletRequest request,
                                                             @Valid @RequestBody AddToWishlistRequest body) {
        Long userId = (Long) request.getAttribute("currentUserId");
        WishlistDTO wishlist = wishlistService.addItem(userId, body.getFertilizerId());
        return ResponseEntity.ok(ApiResponse.ok("Item added to wishlist", wishlist));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<WishlistDTO>> removeItem(HttpServletRequest request,
                                                                @PathVariable Long itemId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        WishlistDTO wishlist = wishlistService.removeItem(userId, itemId);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from wishlist", wishlist));
    }

    @DeleteMapping("/items/product/{productId}")
    public ResponseEntity<ApiResponse<WishlistDTO>> removeItemByProduct(HttpServletRequest request,
                                                                         @PathVariable Long productId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        WishlistDTO wishlist = wishlistService.removeItemByProduct(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok("Product removed from wishlist", wishlist));
    }

    @PostMapping("/items/{itemId}/move-to-cart")
    public ResponseEntity<ApiResponse<WishlistDTO>> moveToCart(HttpServletRequest request,
                                                                @PathVariable Long itemId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        WishlistDTO wishlist = wishlistService.moveToCart(userId, itemId);
        return ResponseEntity.ok(ApiResponse.ok("Item moved to shopping cart", wishlist));
    }

    @PostMapping("/items/product/{productId}/move-to-cart")
    public ResponseEntity<ApiResponse<WishlistDTO>> moveToCartByProduct(HttpServletRequest request,
                                                                         @PathVariable Long productId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        WishlistDTO wishlist = wishlistService.moveToCartByProduct(userId, productId);
        return ResponseEntity.ok(ApiResponse.ok("Product moved to shopping cart", wishlist));
    }
}
