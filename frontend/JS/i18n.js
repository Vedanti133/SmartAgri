/**
 * SmartAgri - Site-wide multilingual UI
 * Languages: English (en), Hindi (hi), Marathi (mr)
 *
 * This file intentionally translates UI text in the browser so existing
 * frontend pages and dynamically-rendered tables/cards also participate.
 * User-entered values and API data are not overwritten as "translations".
 */
(function () {
    'use strict';

    const LANG_KEY = (window.CONFIG && CONFIG.STORAGE_KEYS && CONFIG.STORAGE_KEYS.LANGUAGE)
        ? CONFIG.STORAGE_KEYS.LANGUAGE : 'smartagri_lang';

    const DICT = {
        hi: {
            // Common navigation / application
            "SmartAgri":"स्मार्टएग्री","Smart":"स्मार्ट","Agri":"एग्री","Store":"स्टोर",
            "Dashboard":"डैशबोर्ड","Admin":"प्रशासक","Settings":"सेटिंग्स","Profile":"प्रोफ़ाइल",
            "Account":"खाता","Help":"सहायता","Support":"सहायता","Customer Support":"ग्राहक सहायता",
            "Logout":"लॉग आउट","Sign Out":"साइन आउट","Close":"बंद करें","Cancel":"रद्द करें",
            "Save":"सहेजें","Submit":"जमा करें","Add":"जोड़ें","Edit":"संपादित करें","Delete":"हटाएं",
            "Update":"अपडेट करें","Create":"बनाएं","View":"देखें","View Details":"विवरण देखें",
            "Details":"विवरण","Search":"खोजें","Filter":"फ़िल्टर","Reset":"रीसेट","Refresh":"रीफ़्रेश",
            "Back":"वापस","Next":"आगे","Previous":"पिछला","Actions":"क्रियाएं","Action":"क्रिया",
            "Status":"स्थिति","Type":"प्रकार","Date":"तारीख","Time":"समय","Notes":"टिप्पणियां",
            "Description":"विवरण","Name":"नाम","Email":"ईमेल","Phone":"फ़ोन","Contact Number":"संपर्क नंबर",
            "Contact Person":"संपर्क व्यक्ति","Address":"पता","City":"शहर","State":"राज्य",
            "Country":"देश","Quantity":"मात्रा","Amount":"राशि","Total":"कुल","Price":"कीमत",
            "Available":"उपलब्ध","Active":"सक्रिय","Inactive":"निष्क्रिय","Blocked":"ब्लॉक किया गया",
            "All":"सभी","All Status":"सभी स्थितियां","All Statuses":"सभी स्थितियां","All Types":"सभी प्रकार",
            "All Products":"सभी उत्पाद","Select":"चुनें","Select All":"सभी चुनें","Loading...":"लोड हो रहा है...",
            "No records found":"कोई रिकॉर्ड नहीं मिला","No data available":"कोई डेटा उपलब्ध नहीं है",
            "No results found":"कोई परिणाम नहीं मिला","Action completed!":"क्रिया पूरी हुई!",
            "Error:":"त्रुटि:","Failed:":"विफल:","Success":"सफलता","Warning":"चेतावनी",
            // Modules
            "Categories":"श्रेणियां","Inventory":"इन्वेंटरी","Fertilizers":"उर्वरक","Suppliers":"आपूर्तिकर्ता",
            "Customers":"ग्राहक","Users":"उपयोगकर्ता","Sales":"बिक्री","Purchases":"खरीदारी",
            "Orders & Sales":"ऑर्डर और बिक्री","My Orders":"मेरे ऑर्डर","New Sale":"नई बिक्री",
            "New Purchase":"नई खरीदारी","Stock In":"स्टॉक इन","Stock Out":"स्टॉक आउट",
            "Stock Alerts":"स्टॉक अलर्ट","Reports":"रिपोर्ट","Payments":"भुगतान","Deliveries & Logistics":"डिलीवरी और लॉजिस्टिक्स",
            "Catalog":"कैटलॉग","Cart":"कार्ट","Wishlist":"विशलिस्ट","Track Order":"ऑर्डर ट्रैक करें",
            "Returns":"रिटर्न","Returns & Refunds":"रिटर्न और रिफंड","Batches & FEFO":"बैच और FEFO",
            "Procurement & GRN":"खरीद और GRN","Warehouses & Bins":"वेयरहाउस और बिन",
            "Stock Transfers":"स्टॉक ट्रांसफर","Fulfillment & Dispatch":"फुलफिलमेंट और डिस्पैच",
            "Customer":"ग्राहक","Supplier":"आपूर्तिकर्ता","Product":"उत्पाद","Products":"उत्पाद",
            "Fertilizer Product":"उर्वरक उत्पाद","Warehouse":"वेयरहाउस","Warehouses":"वेयरहाउस",
            "Orders":"ऑर्डर","Order #":"ऑर्डर #","Return #":"रिटर्न #","Tracking #":"ट्रैकिंग #",
            "Tracking URL Template":"ट्रैकिंग URL टेम्पलेट","SKU":"SKU","Code":"कोड","ID":"आईडी",
            "Items":"आइटम","units":"इकाइयां","Brand":"ब्रांड","Category":"श्रेणी","Current Stock:":"वर्तमान स्टॉक:",
            "Low Stock":"कम स्टॉक","Out of Stock":"स्टॉक समाप्त","Storefront (Shop)":"स्टोरफ्रंट (दुकान)",
            // Auth
            "Welcome Back":"वापसी पर स्वागत है","Login to SmartAgri":"SmartAgri में लॉग इन करें",
            "Username / Email":"उपयोगकर्ता नाम / ईमेल","Password":"पासवर्ड","Remember Me":"मुझे याद रखें",
            "Forgot Password?":"पासवर्ड भूल गए?","Login":"लॉग इन","Create an account":"खाता बनाएं",
            "New to SmartAgri?":"SmartAgri पर नए हैं?","Manage Fertilizers. Track Inventory. Grow Smarter.":"उर्वरक प्रबंधित करें। इन्वेंटरी ट्रैक करें। बेहतर तरीके से बढ़ें।",
            "Fertilizer Sales & Inventory Management System":"उर्वरक बिक्री और इन्वेंटरी प्रबंधन प्रणाली",
            // Statuses
            "Pending":"लंबित","Confirmed":"पुष्ट","Packed":"पैक किया गया","Shipped":"भेज दिया गया",
            "Out for Delivery":"डिलीवरी के लिए रवाना","Delivered":"डिलीवर किया गया","Cancelled":"रद्द",
            "Dispatched":"भेजा गया","Received":"प्राप्त","Draft":"ड्राफ्ट","Approved":"स्वीकृत",
            "Rejected":"अस्वीकृत","Pending Approval":"स्वीकृति लंबित","Partially Received":"आंशिक रूप से प्राप्त",
            "Fully Received":"पूरी तरह प्राप्त","Paid":"भुगतान किया गया","Processing":"प्रक्रिया में",
            "Returned":"वापस किया गया","Assigned":"असाइन किया गया","Not Assigned":"असाइन नहीं किया गया",
            "Ready To Ship":"भेजने के लिए तैयार","Unallocated":"असाइन नहीं किया गया","Allocated":"आवंटित",
            "Picking":"पिकिंग","Picked":"पिक किया गया","Packing":"पैकिंग","OPEN":"खुला",
            "IN_PROGRESS":"प्रगति में","WAITING_FOR_CUSTOMER":"ग्राहक की प्रतीक्षा","RESOLVED":"हल किया गया",
            "CLOSED":"बंद","REQUESTED":"अनुरोधित","UNDER_REVIEW":"समीक्षा में","PICKUP_SCHEDULED":"पिकअप निर्धारित",
            "PICKED_UP":"पिकअप हो गया","IN_TRANSIT":"रास्ते में","REFUND_PENDING":"रिफंड लंबित","REFUNDED":"रिफंड किया गया",
            "REPLACED":"बदला गया",
            // Warehouses / procurement
            "Source Facility":"स्रोत सुविधा","Destination Facility":"गंतव्य सुविधा","Receive Qty":"प्राप्त मात्रा",
            "Batch / Lot #":"बैच / लॉट #","New Batch Intake":"नया बैच प्राप्ति","Near Expiry (≤30d)":"समाप्ति के करीब (≤30 दिन)",
            "Central Hub":"केंद्रीय केंद्र","Regional Depot":"क्षेत्रीय डिपो","Distribution Center":"वितरण केंद्र",
            "Retail Storefront":"रिटेल स्टोरफ्रंट","Quarantine Facility":"क्वारंटीन सुविधा",
            "Create Shipment":"शिपमेंट बनाएं","Pickup Address":"पिकअप पता","Expected Delivery Date":"अपेक्षित डिलीवरी तारीख",
            // Units / fertilizer categories
            "Nitrogen":"नाइट्रोजन","Potassic":"पोटाशिक","Micronutrient":"सूक्ष्म पोषक तत्व","Organic":"जैविक",
            "Bio-Fertilizer":"जैव-उर्वरक","Water Soluble":"जल में घुलनशील","Litre":"लीटर","Bottle":"बोतल","Units":"इकाइयां",
            // Common phrases
            "Recent":"हाल के","Amount":"राशि","Created Date":"बनाने की तारीख","Action":"क्रिया",
            "Current Stock":"वर्तमान स्टॉक","Payment":"भुगतान","Invoice":"चालान","Checkout":"चेकआउट",
            "Order Success":"ऑर्डर सफल","Customer Profile":"ग्राहक प्रोफ़ाइल","Customer Orders":"ग्राहक ऑर्डर",
            "Order Tracking":"ऑर्डर ट्रैकिंग","Customer Cart":"ग्राहक कार्ट","Customer Wishlist":"ग्राहक विशलिस्ट"
        },
        mr: {
            // Common navigation / application
            "SmartAgri":"स्मार्टअॅग्री","Smart":"स्मार्ट","Agri":"अॅग्री","Store":"स्टोअर",
            "Dashboard":"डॅशबोर्ड","Admin":"प्रशासक","Settings":"सेटिंग्ज","Profile":"प्रोफाइल",
            "Account":"खाते","Help":"मदत","Support":"मदत","Customer Support":"ग्राहक मदत",
            "Logout":"लॉग आउट","Sign Out":"साइन आउट","Close":"बंद करा","Cancel":"रद्द करा",
            "Save":"जतन करा","Submit":"सबमिट करा","Add":"जोडा","Edit":"संपादित करा","Delete":"हटवा",
            "Update":"अपडेट करा","Create":"तयार करा","View":"पहा","View Details":"तपशील पहा",
            "Details":"तपशील","Search":"शोधा","Filter":"फिल्टर","Reset":"रीसेट","Refresh":"रिफ्रेश",
            "Back":"मागे","Next":"पुढे","Previous":"मागील","Actions":"कृती","Action":"कृती",
            "Status":"स्थिती","Type":"प्रकार","Date":"तारीख","Time":"वेळ","Notes":"टिप्पण्या",
            "Description":"वर्णन","Name":"नाव","Email":"ईमेल","Phone":"फोन","Contact Number":"संपर्क क्रमांक",
            "Contact Person":"संपर्क व्यक्ती","Address":"पत्ता","City":"शहर","State":"राज्य",
            "Country":"देश","Quantity":"प्रमाण","Amount":"रक्कम","Total":"एकूण","Price":"किंमत",
            "Available":"उपलब्ध","Active":"सक्रिय","Inactive":"निष्क्रिय","Blocked":"ब्लॉक केलेले",
            "All":"सर्व","All Status":"सर्व स्थिती","All Statuses":"सर्व स्थिती","All Types":"सर्व प्रकार",
            "All Products":"सर्व उत्पादने","Select":"निवडा","Select All":"सर्व निवडा","Loading...":"लोड होत आहे...",
            "No records found":"कोणतेही रेकॉर्ड सापडले नाही","No data available":"डेटा उपलब्ध नाही",
            "No results found":"कोणतेही परिणाम सापडले नाहीत","Action completed!":"कृती पूर्ण झाली!",
            "Error:":"त्रुटी:","Failed:":"अयशस्वी:","Success":"यश","Warning":"चेतावणी",
            // Modules
            "Categories":"श्रेणी","Inventory":"इन्व्हेंटरी","Fertilizers":"खते","Suppliers":"पुरवठादार",
            "Customers":"ग्राहक","Users":"वापरकर्ते","Sales":"विक्री","Purchases":"खरेदी",
            "Orders & Sales":"ऑर्डर आणि विक्री","My Orders":"माझ्या ऑर्डर्स","New Sale":"नवीन विक्री",
            "New Purchase":"नवीन खरेदी","Stock In":"स्टॉक इन","Stock Out":"स्टॉक आउट",
            "Stock Alerts":"स्टॉक अलर्ट","Reports":"अहवाल","Payments":"पेमेंट","Deliveries & Logistics":"डिलिव्हरी आणि लॉजिस्टिक्स",
            "Catalog":"कॅटलॉग","Cart":"कार्ट","Wishlist":"विशलिस्ट","Track Order":"ऑर्डर ट्रॅक करा",
            "Returns":"रिटर्न","Returns & Refunds":"रिटर्न आणि परतावा","Batches & FEFO":"बॅच आणि FEFO",
            "Procurement & GRN":"खरेदी आणि GRN","Warehouses & Bins":"गोदामे आणि बिन",
            "Stock Transfers":"स्टॉक हस्तांतरण","Fulfillment & Dispatch":"फुलफिलमेंट आणि डिस्पॅच",
            "Customer":"ग्राहक","Supplier":"पुरवठादार","Product":"उत्पादन","Products":"उत्पादने",
            "Fertilizer Product":"खत उत्पादन","Warehouse":"गोदाम","Warehouses":"गोदामे",
            "Orders":"ऑर्डर्स","Order #":"ऑर्डर #","Return #":"रिटर्न #","Tracking #":"ट्रॅकिंग #",
            "Tracking URL Template":"ट्रॅकिंग URL टेम्पलेट","SKU":"SKU","Code":"कोड","ID":"आयडी",
            "Items":"आयटम","units":"युनिट्स","Brand":"ब्रँड","Category":"श्रेणी","Current Stock:":"सध्याचा स्टॉक:",
            "Low Stock":"कमी स्टॉक","Out of Stock":"स्टॉक उपलब्ध नाही","Storefront (Shop)":"स्टोअरफ्रंट (दुकान)",
            // Auth
            "Welcome Back":"पुन्हा स्वागत आहे","Login to SmartAgri":"SmartAgri मध्ये लॉग इन करा",
            "Username / Email":"वापरकर्तानाव / ईमेल","Password":"पासवर्ड","Remember Me":"मला लक्षात ठेवा",
            "Forgot Password?":"पासवर्ड विसरलात?","Login":"लॉग इन","Create an account":"खाते तयार करा",
            "New to SmartAgri?":"SmartAgri वर नवीन आहात?","Manage Fertilizers. Track Inventory. Grow Smarter.":"खते व्यवस्थापित करा. इन्व्हेंटरी ट्रॅक करा. अधिक स्मार्टपणे वाढा.",
            "Fertilizer Sales & Inventory Management System":"खत विक्री आणि इन्व्हेंटरी व्यवस्थापन प्रणाली",
            // Statuses
            "Pending":"प्रलंबित","Confirmed":"निश्चित","Packed":"पॅक केलेले","Shipped":"पाठवलेले",
            "Out for Delivery":"डिलिव्हरीसाठी निघाले","Delivered":"वितरित","Cancelled":"रद्द",
            "Dispatched":"पाठवलेले","Received":"प्राप्त","Draft":"मसुदा","Approved":"मंजूर",
            "Rejected":"नाकारले","Pending Approval":"मंजुरी प्रलंबित","Partially Received":"अंशतः प्राप्त",
            "Fully Received":"पूर्णपणे प्राप्त","Paid":"पेड","Processing":"प्रक्रियेत",
            "Returned":"परत केलेले","Assigned":"नेमून दिलेले","Not Assigned":"नेमून दिलेले नाही",
            "Ready To Ship":"पाठवण्यासाठी तयार","Unallocated":"वाटप न केलेले","Allocated":"वाटप केलेले",
            "Picking":"पिकिंग","Picked":"पिक केलेले","Packing":"पॅकिंग","OPEN":"उघडे",
            "IN_PROGRESS":"प्रगतीत","WAITING_FOR_CUSTOMER":"ग्राहकाची प्रतीक्षा","RESOLVED":"निराकरण केले",
            "CLOSED":"बंद","REQUESTED":"विनंती केलेले","UNDER_REVIEW":"पुनरावलोकनात","PICKUP_SCHEDULED":"पिकअप नियोजित",
            "PICKED_UP":"पिकअप झाले","IN_TRANSIT":"मार्गावर","REFUND_PENDING":"परतावा प्रलंबित","REFUNDED":"परतावा दिला",
            "REPLACED":"बदललेले",
            // Warehouses / procurement
            "Source Facility":"स्रोत सुविधा","Destination Facility":"गंतव्य सुविधा","Receive Qty":"प्राप्त प्रमाण",
            "Batch / Lot #":"बॅच / लॉट #","New Batch Intake":"नवीन बॅच नोंद","Near Expiry (≤30d)":"मुदत संपण्याच्या जवळ (≤30 दिवस)",
            "Central Hub":"केंद्रीय केंद्र","Regional Depot":"प्रादेशिक डेपो","Distribution Center":"वितरण केंद्र",
            "Retail Storefront":"रिटेल स्टोअरफ्रंट","Quarantine Facility":"क्वारंटाईन सुविधा",
            "Create Shipment":"शिपमेंट तयार करा","Pickup Address":"पिकअप पत्ता","Expected Delivery Date":"अपेक्षित डिलिव्हरी तारीख",
            // Units / fertilizer categories
            "Nitrogen":"नायट्रोजन","Potassic":"पोटॅशिक","Micronutrient":"सूक्ष्म अन्नद्रव्ये","Organic":"सेंद्रिय",
            "Bio-Fertilizer":"जैविक खत","Water Soluble":"पाण्यात विरघळणारे","Litre":"लिटर","Bottle":"बाटली","Units":"युनिट्स",
            "Recent":"अलीकडील","Created Date":"तयार केल्याची तारीख","Current Stock":"सध्याचा स्टॉक",
            "Payment":"पेमेंट","Invoice":"बीजक","Checkout":"चेकआउट","Order Success":"ऑर्डर यशस्वी"
        }
    };

    // Fallback vocabulary handles dynamically-created phrases that are not in the
    // exact-phrase dictionary. Technical/API identifiers are deliberately omitted.
    const WORDS = {
      hi: {"Dashboard":"डैशबोर्ड","Inventory":"इन्वेंटरी","Fertilizer":"उर्वरक","Fertilizers":"उर्वरक","Customer":"ग्राहक","Supplier":"आपूर्तिकर्ता","Product":"उत्पाद","Products":"उत्पाद","Order":"ऑर्डर","Orders":"ऑर्डर","Sale":"बिक्री","Sales":"बिक्री","Purchase":"खरीद","Purchases":"खरीदारी","Stock":"स्टॉक","Warehouse":"वेयरहाउस","Warehouses":"वेयरहाउस","Category":"श्रेणी","Categories":"श्रेणियां","Status":"स्थिति","Date":"तारीख","Amount":"राशि","Total":"कुल","Price":"कीमत","Quantity":"मात्रा","Available":"उपलब्ध","Pending":"लंबित","Approved":"स्वीकृत","Cancelled":"रद्द","Delivered":"डिलीवर","Shipped":"भेजा गया","Received":"प्राप्त","Create":"बनाएं","Add":"जोड़ें","Edit":"संपादित करें","Delete":"हटाएं","Save":"सहेजें","Update":"अपडेट करें","Search":"खोजें","Filter":"फ़िल्टर","Refresh":"रीफ़्रेश","Reset":"रीसेट","Close":"बंद करें","Cancel":"रद्द करें","Submit":"जमा करें","Name":"नाम","Email":"ईमेल","Phone":"फ़ोन","Address":"पता","Type":"प्रकार","Code":"कोड","Brand":"ब्रांड","Items":"आइटम","Units":"इकाइयां","Unit":"इकाई","Total":"कुल","View":"देखें","Details":"विवरण","Action":"क्रिया","Actions":"क्रियाएं","Report":"रिपोर्ट","Reports":"रिपोर्ट","Payment":"भुगतान","Payments":"भुगतान","Return":"रिटर्न","Returns":"रिटर्न","Refund":"रिफंड","Support":"सहायता","User":"उपयोगकर्ता","Users":"उपयोगकर्ता","Active":"सक्रिय","Inactive":"निष्क्रिय","Blocked":"ब्लॉक किया गया","All":"सभी","Loading":"लोड हो रहा है","Error":"त्रुटि","Failed":"विफल","Success":"सफलता","Warning":"चेतावनी","New":"नया","Current":"वर्तमान","Expected":"अपेक्षित","Source":"स्रोत","Destination":"गंतव्य","Contact":"संपर्क","Person":"व्यक्ति","Batch":"बैच","Tracking":"ट्रैकिंग","Dispatch":"डिस्पैच","Delivery":"डिलीवरी","Invoice":"चालान","Checkout":"चेकआउट","Cart":"कार्ट","Wishlist":"विशलिस्ट","Login":"लॉग इन","Password":"पासवर्ड","Username":"उपयोगकर्ता नाम","Profile":"प्रोफ़ाइल","Settings":"सेटिंग्स","Help":"सहायता"},
      mr: {"Dashboard":"डॅशबोर्ड","Inventory":"इन्व्हेंटरी","Fertilizer":"खत","Fertilizers":"खते","Customer":"ग्राहक","Supplier":"पुरवठादार","Product":"उत्पादन","Products":"उत्पादने","Order":"ऑर्डर","Orders":"ऑर्डर्स","Sale":"विक्री","Sales":"विक्री","Purchase":"खरेदी","Purchases":"खरेदी","Stock":"स्टॉक","Warehouse":"गोदाम","Warehouses":"गोदामे","Category":"श्रेणी","Categories":"श्रेणी","Status":"स्थिती","Date":"तारीख","Amount":"रक्कम","Total":"एकूण","Price":"किंमत","Quantity":"प्रमाण","Available":"उपलब्ध","Pending":"प्रलंबित","Approved":"मंजूर","Cancelled":"रद्द","Delivered":"वितरित","Shipped":"पाठवलेले","Received":"प्राप्त","Create":"तयार करा","Add":"जोडा","Edit":"संपादित करा","Delete":"हटवा","Save":"जतन करा","Update":"अपडेट करा","Search":"शोधा","Filter":"फिल्टर","Refresh":"रिफ्रेश","Reset":"रीसेट","Close":"बंद करा","Cancel":"रद्द करा","Submit":"सबमिट करा","Name":"नाव","Email":"ईमेल","Phone":"फोन","Address":"पत्ता","Type":"प्रकार","Code":"कोड","Brand":"ब्रँड","Items":"आयटम","Units":"युनिट्स","Unit":"युनिट","View":"पहा","Details":"तपशील","Action":"कृती","Actions":"कृती","Report":"अहवाल","Reports":"अहवाल","Payment":"पेमेंट","Payments":"पेमेंट","Return":"रिटर्न","Returns":"रिटर्न","Refund":"परतावा","Support":"मदत","User":"वापरकर्ता","Users":"वापरकर्ते","Active":"सक्रिय","Inactive":"निष्क्रिय","Blocked":"ब्लॉक केलेले","All":"सर्व","Loading":"लोड होत आहे","Error":"त्रुटी","Failed":"अयशस्वी","Success":"यश","Warning":"चेतावणी","New":"नवीन","Current":"सध्याचा","Expected":"अपेक्षित","Source":"स्रोत","Destination":"गंतव्य","Contact":"संपर्क","Person":"व्यक्ती","Batch":"बॅच","Tracking":"ट्रॅकिंग","Dispatch":"डिस्पॅच","Delivery":"डिलिव्हरी","Invoice":"बीजक","Checkout":"चेकआउट","Cart":"कार्ट","Wishlist":"विशलिस्ट","Login":"लॉग इन","Password":"पासवर्ड","Username":"वापरकर्तानाव","Profile":"प्रोफाइल","Settings":"सेटिंग्ज","Help":"मदत"}
    };

    function getLang() {
        const saved = localStorage.getItem(LANG_KEY);
        return ['en','hi','mr'].includes(saved) ? saved : 'en';
    }

    function escapeRegExp(s) { return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'); }

    function translateText(text, lang) {
        if (!text || lang === 'en') return text;
        const exact = DICT[lang] && DICT[lang][text.trim()];
        if (exact) {
            const leading = text.match(/^\s*/)?.[0] || '';
            const trailing = text.match(/\s*$/)?.[0] || '';
            return leading + exact + trailing;
        }

        // Translate only plain human-readable text. Avoid IDs, URLs, code and
        // strings containing long technical tokens.
        if (/[{}<>]|https?:\/\//i.test(text) || /[A-Z0-9_]{5,}/.test(text)) return text;

        const vocab = WORDS[lang] || {};
        let out = text;
        const keys = Object.keys(vocab).sort((a,b)=>b.length-a.length);
        for (const key of keys) {
            out = out.replace(new RegExp('(^|\\b)' + escapeRegExp(key) + '(?=\\b|\\s|[:.,!?/#()\\-]|$)', 'g'), (m,prefix)=>prefix+vocab[key]);
        }
        return out;
    }

    function translateElement(el, lang) {
        if (!el || el.nodeType !== 1) return;
        if (el.matches('script,style,code,pre,svg')) return;

        // Translate text nodes rather than element.textContent so icons,
        // buttons, inputs and nested markup are preserved.
        const walker = document.createTreeWalker(el, NodeFilter.SHOW_TEXT);
        const nodes = [];
        let node;
        while ((node = walker.nextNode())) nodes.push(node);
        for (const textNode of nodes) {
            if (textNode.parentElement && textNode.parentElement.closest('script,style,code,pre,svg')) continue;
            if (textNode.__smartAgriOriginal === undefined) textNode.__smartAgriOriginal = textNode.nodeValue;
            textNode.nodeValue = translateText(textNode.__smartAgriOriginal, lang);
        }

        ['placeholder','title','aria-label'].forEach(attr => {
            if (!el.hasAttribute(attr)) return;
            const marker = 'i18nOriginal' + attr.replace(/-([a-z])/g,(_,c)=>c.toUpperCase());
            if (el.dataset[marker] === undefined) el.dataset[marker] = el.getAttribute(attr);
            el.setAttribute(attr, translateText(el.dataset[marker], lang));
        });
    }

    function restoreOriginalText() {
        const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT);
        let node;
        while ((node = walker.nextNode())) {
            if (node.__smartAgriOriginal !== undefined) node.nodeValue = node.__smartAgriOriginal;
        }
        document.querySelectorAll('[data-i18n-original-placeholder]').forEach(el => el.setAttribute('placeholder', el.dataset.i18nOriginalPlaceholder));
        document.querySelectorAll('[data-i18n-original-title]').forEach(el => el.setAttribute('title', el.dataset.i18nOriginalTitle));
        document.querySelectorAll('[data-i18n-original-aria-label]').forEach(el => el.setAttribute('aria-label', el.dataset.i18nOriginalAriaLabel));
    }

    function translateDOM() {
        const lang = getLang();
        document.documentElement.lang = lang;
        translateElement(document.body, lang);
        const titleOriginal = document.title || 'SmartAgri';
        if (!document.documentElement.dataset.i18nTitle) {
            document.documentElement.dataset.i18nTitle = titleOriginal;
        }
        document.title = translateText(document.documentElement.dataset.i18nTitle, lang);
        updateSelector(lang);
    }

    function addStyles() {
        if (document.getElementById('smartagri-i18n-style')) return;
        const style = document.createElement('style');
        style.id = 'smartagri-i18n-style';
        style.textContent = `
          .smartagri-language-switcher{position:fixed;top:14px;right:18px;z-index:10000;display:flex;align-items:center;gap:7px;padding:6px 9px;background:#fff;border:1px solid #dee2e6;border-radius:10px;box-shadow:0 3px 14px rgba(0,0,0,.12);font:500 13px/1.2 system-ui,sans-serif}
          .smartagri-language-switcher label{margin:0;color:#495057}
          .smartagri-language-switcher select{border:0;outline:0;background:transparent;font-weight:600;cursor:pointer}
          @media(max-width:600px){.smartagri-language-switcher{top:8px;right:8px}.smartagri-language-switcher label{display:none}}
        `;
        document.head.appendChild(style);
    }

    function updateSelector(lang) {
        const select = document.getElementById('smartagriLanguage');
        if (select && select.value !== lang) select.value = lang;
    }

    function createSelector() {
        if (!document.body || document.getElementById('smartagriLanguageSwitcher')) return;
        const wrap = document.createElement('div');
        wrap.id = 'smartagriLanguageSwitcher';
        wrap.className = 'smartagri-language-switcher';
        wrap.setAttribute('role','group');
        wrap.setAttribute('aria-label','Language selector');
        wrap.innerHTML = `
          <label for="smartagriLanguage">Language</label>
          <select id="smartagriLanguage" aria-label="Choose language">
            <option value="en">English</option>
            <option value="hi">हिंदी</option>
            <option value="mr">मराठी</option>
          </select>`;
        document.body.appendChild(wrap);
        const select = wrap.querySelector('select');
        select.value = getLang();
        select.addEventListener('change', function () {
            localStorage.setItem(LANG_KEY, this.value);
            // Always translate from the original English source text.
            restoreOriginalText();
            translateDOM();
        });
    }

    function init() {
        addStyles();
        createSelector();
        translateDOM();

        // Catch tables/cards/toasts populated later by existing JS files.
        const observer = new MutationObserver((mutations) => {
            if (getLang() === 'en') return;
            for (const mutation of mutations) {
                for (const node of mutation.addedNodes) {
                    if (node.nodeType === 1) {
                        translateElement(node, getLang());
                    }
                }
            }
        });
        observer.observe(document.body, {childList:true, subtree:true});
    }

    window.SmartAgriI18n = {
        getLanguage: getLang,
        setLanguage: function(lang) {
            if (!['en','hi','mr'].includes(lang)) return;
            localStorage.setItem(LANG_KEY, lang);
            location.reload();
        },
        translate: translateText
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();