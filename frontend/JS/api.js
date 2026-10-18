/**
 * SmartAgri - Centralized REST API Communication Client
 * Handles HTTP requests, JSON parsing, standardized error formatting,
 * and future-proof authentication header injection.
 */

const ApiClient = (function () {
    /**
     * Get the active API Base URL from CONFIG or fallback.
     */
    function getBaseUrl() {
        if (typeof CONFIG !== 'undefined' && CONFIG.API_BASE_URL) {
            return CONFIG.API_BASE_URL;
        }
        if (typeof API_URL !== 'undefined') {
            return API_URL;
        }
        return 'http://localhost:8080/api';
    }

    /**
     * Build common request headers, including optional Bearer token when available.
     */
    function buildHeaders(customHeaders = {}) {
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            ...customHeaders
        };

        // Retrieve auth token if stored
        try {
            const token = localStorage.getItem('smartagriAuthToken') ||
                          localStorage.getItem('smartagri_auth_token') ||
                          localStorage.getItem('token');
            if (token) {
                headers['Authorization'] = `Bearer ${token}`;
            }
        } catch (e) {
            // Silently ignore localStorage read failures
        }

        return headers;
    }

    /**
     * Centralized request executor with robust error handling.
     */
    async function request(endpoint, options = {}) {
        const baseUrl = getBaseUrl();
        // Ensure clean URL joining
        const normalizedEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
        const url = `${baseUrl}${normalizedEndpoint}`;

        const config = {
            method: options.method || 'GET',
            headers: buildHeaders(options.headers),
            ...options
        };

        if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
            config.body = JSON.stringify(options.body);
        }

        try {
            const response = await fetch(url, config);

            // Parse response body (safely handle JSON, text, or empty responses)
            let responseData = null;
            const contentType = response.headers.get('content-type') || '';
            if (contentType.includes('application/json')) {
                responseData = await response.json().catch(() => null);
            } else {
                responseData = await response.text().catch(() => null);
            }

            if (!response.ok) {
                // Extract meaningful error message from backend ApiResponse if available
                let errorMessage = 'Request failed';
                if (responseData && typeof responseData === 'object') {
                    errorMessage = responseData.message || responseData.error || `HTTP Error ${response.status}`;
                } else if (typeof responseData === 'string' && responseData.trim()) {
                    errorMessage = responseData;
                } else {
                    errorMessage = `HTTP ${response.status}: ${response.statusText || 'Server Error'}`;
                }

                const error = new Error(errorMessage);
                error.status = response.status;
                error.data = responseData;
                throw error;
            }

            return responseData;
        } catch (networkError) {
            // Distinguish between HTTP error (with status) and offline/connection failure
            if (networkError.status) {
                throw networkError;
            }
            console.error('[ApiClient Network Error]', networkError);
            const connectionError = new Error('Unable to connect to SmartAgri server. Please ensure the backend is running.');
            connectionError.status = 0;
            connectionError.cause = networkError;
            throw connectionError;
        }
    }

    return {
        get: (endpoint, options = {}) => request(endpoint, { ...options, method: 'GET' }),
        post: (endpoint, body, options = {}) => request(endpoint, { ...options, method: 'POST', body }),
        put: (endpoint, body, options = {}) => request(endpoint, { ...options, method: 'PUT', body }),
        delete: (endpoint, options = {}) => request(endpoint, { ...options, method: 'DELETE' }),
        request: request
    };
})();

// Global alias for compatibility
window.ApiClient = ApiClient;
window.api = ApiClient.request;
