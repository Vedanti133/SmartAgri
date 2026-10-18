/**
 * SmartAgri - Centralized Frontend Configuration
 * Contains client-side constants and configuration parameters.
 * NOTE: Never place backend secrets, passwords, or private keys in this file.
 */

const CONFIG = {
    // Backend API Base URL
    API_BASE_URL: 'http://localhost:8080/api',

    // Application Metadata
    APP_NAME: 'SmartAgri',
    APP_TAGLINE: 'Fertilizer Sales, Inventory Management and E-Commerce System',
    VERSION: '1.0.0',

    // Localization Defaults
    DEFAULT_LANGUAGE: 'en',
    SUPPORTED_LANGUAGES: [
        { code: 'en', label: 'English' },
        { code: 'hi', label: 'हिंदी' },
        { code: 'mr', label: 'मराठी' }
    ],

    // Currency Formatting
    CURRENCY_CODE: 'INR',
    CURRENCY_SYMBOL: '₹',

    // LocalStorage Keys for UI state & sessions
    STORAGE_KEYS: {
        USER_SESSION: 'smartagriUser',
        AUTH_TOKEN: 'smartagriAuthToken',
        LANGUAGE: 'smartagri_lang',
        THEME: 'smartagri_theme',
        CART: 'smartagri_cart',
        WISHLIST: 'smartagri_wishlist'
    },

    // Order Lifecycle Status Definitions
    ORDER_STATUSES: {
        PENDING: { label: 'Pending', badgeClass: 'bg-warning text-dark' },
        CONFIRMED: { label: 'Confirmed', badgeClass: 'bg-info text-dark' },
        PACKED: { label: 'Packed', badgeClass: 'bg-primary' },
        SHIPPED: { label: 'Shipped', badgeClass: 'bg-secondary' },
        OUT_FOR_DELIVERY: { label: 'Out for Delivery', badgeClass: 'bg-info' },
        DELIVERED: { label: 'Delivered', badgeClass: 'bg-success' },
        CANCELLED: { label: 'Cancelled', badgeClass: 'bg-danger' }
    }
};

// Backward-compatible alias for existing code
const API_URL = CONFIG.API_BASE_URL;

// Freeze configuration object to prevent inadvertent runtime mutations
Object.freeze(CONFIG);
Object.freeze(CONFIG.STORAGE_KEYS);
Object.freeze(CONFIG.ORDER_STATUSES);
