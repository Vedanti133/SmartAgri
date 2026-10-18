package com.smartagri.controller;

import com.smartagri.dto.AddToCartRequest;
import com.smartagri.dto.ApiResponse;
import com.smartagri.dto.CartDTO;
import com.smartagri.dto.CartItemDTO;
import com.smartagri.dto.UpdateCartItemRequest;
import com.smartagri.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = {"http://localhost:5500", "http://127.0.0.1:5500", "http://localhost:5501", "http://127.0.0.1:5501", "http://localhost:3000"})
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<CartDTO>> getCart(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(userId)));
    }

    @GetMapping("/items")
    public ResponseEntity<ApiResponse<java.util.List<CartItemDTO>>> getCartItems(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(userId).getItems()));
    }


    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartDTO>> addItem(HttpServletRequest request,
                                                         @Valid @RequestBody AddToCartRequest body) {
        Long userId = (Long) request.getAttribute("currentUserId");
        CartDTO cart = cartService.addItem(userId, body.getFertilizerId(), body.getQuantity());
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart", cart));
    }

    @PutMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDTO>> updateItem(HttpServletRequest request,
                                                            @PathVariable Long itemId,
                                                            @Valid @RequestBody UpdateCartItemRequest body) {
        Long userId = (Long) request.getAttribute("currentUserId");
        CartDTO cart = cartService.updateItem(userId, itemId, body.getQuantity());
        return ResponseEntity.ok(ApiResponse.ok("Cart item updated", cart));
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<CartDTO>> removeItem(HttpServletRequest request,
                                                            @PathVariable Long itemId) {
        Long userId = (Long) request.getAttribute("currentUserId");
        CartDTO cart = cartService.removeItem(userId, itemId);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart", cart));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<CartDTO>> clearCart(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("currentUserId");
        CartDTO cart = cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", cart));
    }
}
