package com.smartagri.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartagri.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(TokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Pre-flight CORS OPTIONS requests must pass through
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        String uri = request.getRequestURI();
        String method = request.getMethod();

        // 1. Extract Bearer token if provided
        String authHeader = request.getHeader("Authorization");
        AuthenticatedUser currentUser = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (tokenService.validateToken(token)) {
                Long userId = tokenService.getUserId(token);
                String username = tokenService.getUsername(token);
                String role = tokenService.getRole(token);
                currentUser = new AuthenticatedUser(userId, username, role);
                request.setAttribute("currentUser", currentUser);
                request.setAttribute("currentUserId", userId);
                request.setAttribute("currentUsername", username);
                request.setAttribute("currentUserRole", role);
            }
        }

        // 2. Public endpoints (Always permit)
        if (uri.startsWith("/api/auth/") || uri.equals("/error") || uri.startsWith("/api/health")) {
            return true;
        }

        // Public read-only catalog and product inventory endpoints
        if (HttpMethod.GET.matches(method)) {
            if (uri.startsWith("/api/fertilizers") || uri.startsWith("/api/categories") || uri.startsWith("/api/inventory/status")) {
                return true;
            }
        }

        // 3. User endpoints that need authentication (cart, wishlist, addresses, profile, orders, payments, customer tracking)
        if (currentUser == null) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Unauthorized: Authentication required. Please log in.");
            return false;
        }

        // ADMIN has complete access across all endpoints
        if (currentUser.isAdmin()) {
            return true;
        }

        // 4. Role-based backend authorization for non-admin roles:

        // A. Users management (/api/users/**) - Strictly ADMIN only
        if (uri.startsWith("/api/users")) {
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden: Only administrator can access user management.");
            return false;
        }

        // B. Payments & Refunds back-office ledger (/api/admin/payments/**, /api/admin/refunds/**) - Strictly ADMIN only
        if (uri.startsWith("/api/admin/payments") || uri.startsWith("/api/admin/refunds")) {
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden: Admin privileges required for financial management.");
            return false;
        }

        // C. Settings (/api/admin/settings/**) - Strictly ADMIN only
        if (uri.startsWith("/api/admin/settings")) {
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden: Admin privileges required for system settings.");
            return false;
        }

        // D. Catalog mutations (POST/PUT/DELETE on /api/fertilizers, /api/categories)
        boolean isCatalogMutation = (uri.startsWith("/api/fertilizers") || uri.startsWith("/api/categories"))
                && (HttpMethod.POST.matches(method) || HttpMethod.PUT.matches(method) || HttpMethod.DELETE.matches(method));
        if (isCatalogMutation) {
            if (!currentUser.hasRole("SALES_STAFF")) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden: Product catalog modifications are restricted.");
                return false;
            }
            return true;
        }

        // E. Back-office Orders, Deliveries, Fulfillment, Support, Returns
        if (uri.startsWith("/api/admin/orders") || uri.startsWith("/api/admin/deliveries")
                || uri.startsWith("/api/admin/delivery-partners") || uri.startsWith("/api/admin/fulfillment")
                || uri.startsWith("/api/admin/packages") || uri.startsWith("/api/admin/picklists")
                || uri.startsWith("/api/admin/shipments") || uri.startsWith("/api/admin/returns")
                || uri.startsWith("/api/admin/support")) {
            if (!currentUser.hasRole("SALES_STAFF")) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden: Operational staff privileges required.");
                return false;
            }
            return true;
        }

        // F. Procurement & Supplier management
        if (uri.startsWith("/api/admin/procurement") || uri.startsWith("/api/admin/purchase-orders")
                || uri.startsWith("/api/admin/goods-receipts") || uri.startsWith("/api/admin/suppliers")) {
            if (!currentUser.hasAnyRole("SUPPLIER", "SALES_STAFF")) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden: Supplier or Procurement privileges required.");
                return false;
            }
            return true;
        }

        // G. Warehouses & Locations
        if (uri.startsWith("/api/admin/warehouses") || uri.startsWith("/api/admin/locations")) {
            if (HttpMethod.GET.matches(method) && currentUser.hasRole("DISTRIBUTOR")) {
                return true;
            }
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden: Warehouse infrastructure management is restricted.");
            return false;
        }

        // H. Stock Transfers
        if (uri.startsWith("/api/admin/stock-transfers") || uri.startsWith("/api/admin/transfers")) {
            if (!currentUser.hasAnyRole("DISTRIBUTOR", "DEALER")) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden: Stock transfer operations are restricted.");
                return false;
            }
            return true;
        }

        // I. Admin Inventory & Batches
        if (uri.startsWith("/api/admin/inventory") || uri.startsWith("/api/admin/batches")) {
            if (currentUser.hasAnyRole("CUSTOMER", "RETAILER")) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden: Access to inventory management is restricted.");
                return false;
            }
            if (HttpMethod.GET.matches(method)) {
                if (currentUser.hasAnyRole("SALES_STAFF", "DISTRIBUTOR", "DEALER", "SUPPLIER")) {
                    return true;
                }
            } else {
                // Stock In / modifications
                if (currentUser.hasAnyRole("SALES_STAFF", "SUPPLIER")) {
                    return true;
                }
            }
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden: Inventory modification is restricted.");
            return false;
        }

        // J. Any other /api/admin/** endpoint is strictly ADMIN only
        if (uri.startsWith("/api/admin/")) {
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden: Administrator privileges required.");
            return false;
        }

        // K. Commercial customer endpoints (cart, wishlist)
        if (uri.startsWith("/api/cart") || uri.startsWith("/api/wishlist")) {
            if (currentUser.hasRole("SUPPLIER")) {
                sendErrorResponse(response, HttpStatus.FORBIDDEN, "Forbidden: Supplier accounts cannot access the customer shopping cart.");
                return false;
            }
        }

        return true;
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws Exception {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> apiResponse = ApiResponse.error(message);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
