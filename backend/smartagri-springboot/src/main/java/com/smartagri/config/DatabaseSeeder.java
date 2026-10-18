package com.smartagri.config;

import com.smartagri.entity.Category;
import com.smartagri.entity.Fertilizer;
import com.smartagri.entity.User;
import com.smartagri.entity.Supplier;
import com.smartagri.entity.SupplierStatus;
import com.smartagri.entity.Warehouse;
import com.smartagri.entity.WarehouseLocation;
import com.smartagri.entity.WarehouseType;
import com.smartagri.entity.WarehouseStatus;
import com.smartagri.entity.LocationType;
import com.smartagri.entity.LocationStatus;
import com.smartagri.repository.CategoryRepository;
import com.smartagri.repository.FertilizerRepository;
import com.smartagri.repository.UserRepository;
import com.smartagri.repository.SupplierRepository;
import com.smartagri.repository.WarehouseRepository;
import com.smartagri.repository.WarehouseLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DatabaseSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseSeeder.class);

    private final CategoryRepository categoryRepository;
    private final FertilizerRepository fertilizerRepository;
    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository warehouseLocationRepository;
    private final PasswordEncoder passwordEncoder;

    public DatabaseSeeder(CategoryRepository categoryRepository,
                          FertilizerRepository fertilizerRepository,
                          UserRepository userRepository,
                          SupplierRepository supplierRepository,
                          WarehouseRepository warehouseRepository,
                          WarehouseLocationRepository warehouseLocationRepository,
                          PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.fertilizerRepository = fertilizerRepository;
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.warehouseLocationRepository = warehouseLocationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking SmartAgri database initialization...");
        seedCategories();
        seedFertilizers();
        seedDefaultUsers();
        seedDefaultWarehouse();
        seedDefaultSuppliers();
        log.info("SmartAgri database verification complete. Categories: {}, Fertilizers: {}, Users: {}",
                categoryRepository.count(), fertilizerRepository.count(), userRepository.count());
    }

    private void seedCategories() {
        if (!categoryRepository.existsByNameIgnoreCase("Nitrogen")) {
            Category cat = new Category("Nitrogen", "High nitrogen products for crop growth and leaf development.", "ACTIVE");
            categoryRepository.save(cat);
            log.info("Seeded category: {}", "Nitrogen");
        }
        if (!categoryRepository.existsByNameIgnoreCase("Phosphate")) {
            Category cat = new Category("Phosphate", "Phosphate based fertilizers supporting root development.", "ACTIVE");
            categoryRepository.save(cat);
            log.info("Seeded category: {}", "Phosphate");
        }
        if (!categoryRepository.existsByNameIgnoreCase("NPK")) {
            Category cat = new Category("NPK", "Balanced fertilizers containing nitrogen, phosphorus and potassium.", "ACTIVE");
            categoryRepository.save(cat);
            log.info("Seeded category: {}", "NPK");
        }
        if (!categoryRepository.existsByNameIgnoreCase("Potassic")) {
            Category cat = new Category("Potassic", "Potassium rich products for crop strength and quality.", "ACTIVE");
            categoryRepository.save(cat);
            log.info("Seeded category: {}", "Potassic");
        }
        if (!categoryRepository.existsByNameIgnoreCase("Water Soluble")) {
            Category cat = new Category("Water Soluble", "Fully soluble fertilizers for efficient crop nutrition.", "ACTIVE");
            categoryRepository.save(cat);
            log.info("Seeded category: {}", "Water Soluble");
        }
        if (!categoryRepository.existsByNameIgnoreCase("Micronutrient")) {
            Category cat = new Category("Micronutrient", "Essential nutrients such as zinc, boron, iron and manganese.", "ACTIVE");
            categoryRepository.save(cat);
            log.info("Seeded category: {}", "Micronutrient");
        }
        if (!categoryRepository.existsByNameIgnoreCase("Sulphur")) {
            Category cat = new Category("Sulphur", "Sulphur based products for soil improvement and crop nutrition.", "ACTIVE");
            categoryRepository.save(cat);
            log.info("Seeded category: {}", "Sulphur");
        }
        if (!categoryRepository.existsByNameIgnoreCase("Organic")) {
            Category cat = new Category("Organic", "Natural soil nutrients that improve soil health and fertility.", "ACTIVE");
            categoryRepository.save(cat);
            log.info("Seeded category: {}", "Organic");
        }
        if (!categoryRepository.existsByNameIgnoreCase("Bio-Fertilizer")) {
            Category cat = new Category("Bio-Fertilizer", "Biological fertilizers supporting sustainable crop production.", "ACTIVE");
            categoryRepository.save(cat);
            log.info("Seeded category: {}", "Bio-Fertilizer");
        }
    }

    private void seedFertilizers() {
        if (fertilizerRepository.count() >= 50) {
            log.info("Fertilizer table already contains {} records, skipping seeding.", fertilizerRepository.count());
            return;
        }

        // Cache categories by name
        Map<String, Category> catMap = new HashMap<>();
        for (Category c : categoryRepository.findAll()) {
            catMap.put(c.getName().toLowerCase(), c);
        }

        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-001")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-001");
            f.setName("Urea");
            f.setBrand("IFFCO");
            f.setCategory(catMap.get("nitrogen"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("260.00"));
            f.setSellingPrice(new BigDecimal("290.00"));
            f.setStock(150);
            f.setMinimumStock(50);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Wheat, Rice, Maize, Sugarcane, Cotton");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Nitrogen (N) 46.0% min");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Nitrogen");
            f.setDescription("Urea (IFFCO) - High performance Nitrogen fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-002")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-002");
            f.setName("DAP");
            f.setBrand("Coromandel");
            f.setCategory(catMap.get("phosphate"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("1150.00"));
            f.setSellingPrice(new BigDecimal("1280.00"));
            f.setStock(20);
            f.setMinimumStock(50);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Pulses, Oilseeds, Cereals, Root Crops");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Total Nitrogen 18.0%, Available P2O5 46.0%");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Phosphate");
            f.setDescription("DAP (Coromandel) - High performance Phosphate fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-003")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-003");
            f.setName("NPK 10:26:26");
            f.setBrand("IFFCO");
            f.setCategory(catMap.get("npk"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("1350.00"));
            f.setSellingPrice(new BigDecimal("1490.00"));
            f.setStock(90);
            f.setMinimumStock(35);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("All field crops, cereals, pulses, vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("NPK 10:26:26 balanced formulation");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("NPK");
            f.setDescription("NPK 10:26:26 (IFFCO) - High performance NPK fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-004")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-004");
            f.setName("NPK 20:20:20");
            f.setBrand("GSFC");
            f.setCategory(catMap.get("npk"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("1420.00"));
            f.setSellingPrice(new BigDecimal("1580.00"));
            f.setStock(0);
            f.setMinimumStock(30);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("All field crops, cereals, pulses, vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("NPK 20:20:20 balanced formulation");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("OUT_OF_STOCK");
            f.setType("NPK");
            f.setDescription("NPK 20:20:20 (GSFC) - High performance NPK fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-005")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-005");
            f.setName("MOP");
            f.setBrand("IPL");
            f.setCategory(catMap.get("potassic"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("980.00"));
            f.setSellingPrice(new BigDecimal("1090.00"));
            f.setStock(75);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Potato, Banana, Sugarcane, Fruits, Vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Water Soluble Potassium (K2O) 60.0%");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Potassic");
            f.setDescription("MOP (IPL) - High performance Potassic fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-006")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-006");
            f.setName("SOP");
            f.setBrand("Deepak Fertilizers");
            f.setCategory(catMap.get("potassic"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("1650.00"));
            f.setSellingPrice(new BigDecimal("1800.00"));
            f.setStock(18);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Potato, Banana, Sugarcane, Fruits, Vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Potassium (K2O) 50.0%, Sulphur 17.5%");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Potassic");
            f.setDescription("SOP (Deepak Fertilizers) - High performance Potassic fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-007")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-007");
            f.setName("SSP");
            f.setBrand("Paradeep");
            f.setCategory(catMap.get("phosphate"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("410.00"));
            f.setSellingPrice(new BigDecimal("475.00"));
            f.setStock(105);
            f.setMinimumStock(40);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Pulses, Oilseeds, Cereals, Root Crops");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Water Soluble P2O5 16.0%, Sulphur 11.0%");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Phosphate");
            f.setDescription("SSP (Paradeep) - High performance Phosphate fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-008")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-008");
            f.setName("Ammonium Sulphate");
            f.setBrand("FACT");
            f.setCategory(catMap.get("nitrogen"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("580.00"));
            f.setSellingPrice(new BigDecimal("650.00"));
            f.setStock(62);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Wheat, Rice, Maize, Sugarcane, Cotton");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Nitrogen standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Nitrogen");
            f.setDescription("Ammonium Sulphate (FACT) - High performance Nitrogen fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-009")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-009");
            f.setName("Ammonium Nitrate");
            f.setBrand("RCF");
            f.setCategory(catMap.get("nitrogen"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("720.00"));
            f.setSellingPrice(new BigDecimal("805.00"));
            f.setStock(42);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Wheat, Rice, Maize, Sugarcane, Cotton");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Nitrogen standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Nitrogen");
            f.setDescription("Ammonium Nitrate (RCF) - High performance Nitrogen fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-010")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-010");
            f.setName("CAN");
            f.setBrand("GSFC");
            f.setCategory(catMap.get("nitrogen"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("630.00"));
            f.setSellingPrice(new BigDecimal("710.00"));
            f.setStock(34);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Wheat, Rice, Maize, Sugarcane, Cotton");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Nitrogen standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Nitrogen");
            f.setDescription("CAN (GSFC) - High performance Nitrogen fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-011")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-011");
            f.setName("NPK 12:32:16");
            f.setBrand("IFFCO");
            f.setCategory(catMap.get("npk"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("1280.00"));
            f.setSellingPrice(new BigDecimal("1430.00"));
            f.setStock(88);
            f.setMinimumStock(30);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("All field crops, cereals, pulses, vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("NPK 12:32:16 balanced formulation");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("NPK");
            f.setDescription("NPK 12:32:16 (IFFCO) - High performance NPK fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-012")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-012");
            f.setName("NPK 15:15:15");
            f.setBrand("Zuari");
            f.setCategory(catMap.get("npk"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("1120.00"));
            f.setSellingPrice(new BigDecimal("1250.00"));
            f.setStock(58);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("All field crops, cereals, pulses, vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("NPK 15:15:15 balanced formulation");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("NPK");
            f.setDescription("NPK 15:15:15 (Zuari) - High performance NPK fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-013")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-013");
            f.setName("NPK 19:19:19");
            f.setBrand("Aries");
            f.setCategory(catMap.get("npk"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("92.00"));
            f.setSellingPrice(new BigDecimal("110.00"));
            f.setStock(140);
            f.setMinimumStock(50);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("All field crops, cereals, pulses, vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("NPK 19:19:19 balanced formulation");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("NPK");
            f.setDescription("NPK 19:19:19 (Aries) - High performance NPK fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-014")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-014");
            f.setName("NPK 13:40:13");
            f.setBrand("Coromandel");
            f.setCategory(catMap.get("water soluble"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("115.00"));
            f.setSellingPrice(new BigDecimal("135.00"));
            f.setStock(72);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Greenhouse crops, drip irrigation, fertigation, horticulture");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("NPK 13:40:13 balanced formulation");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Water Soluble");
            f.setDescription("NPK 13:40:13 (Coromandel) - High performance Water Soluble fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-015")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-015");
            f.setName("NPK 00:52:34");
            f.setBrand("Haifa");
            f.setCategory(catMap.get("water soluble"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("180.00"));
            f.setSellingPrice(new BigDecimal("215.00"));
            f.setStock(36);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Greenhouse crops, drip irrigation, fertigation, horticulture");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("NPK 00:52:34 balanced formulation");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Water Soluble");
            f.setDescription("NPK 00:52:34 (Haifa) - High performance Water Soluble fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-016")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-016");
            f.setName("Calcium Nitrate");
            f.setBrand("Yara");
            f.setCategory(catMap.get("water soluble"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("145.00"));
            f.setSellingPrice(new BigDecimal("175.00"));
            f.setStock(64);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Greenhouse crops, drip irrigation, fertigation, horticulture");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Water Soluble standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Water Soluble");
            f.setDescription("Calcium Nitrate (Yara) - High performance Water Soluble fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-017")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-017");
            f.setName("Magnesium Sulphate");
            f.setBrand("GreenGrow");
            f.setCategory(catMap.get("micronutrient"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("52.00"));
            f.setSellingPrice(new BigDecimal("68.00"));
            f.setStock(95);
            f.setMinimumStock(30);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Citrus, vegetables, paddy, cotton, horticulture");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Micronutrient standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Micronutrient");
            f.setDescription("Magnesium Sulphate (GreenGrow) - High performance Micronutrient fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-018")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-018");
            f.setName("Zinc Sulphate");
            f.setBrand("Coromandel");
            f.setCategory(catMap.get("micronutrient"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("68.00"));
            f.setSellingPrice(new BigDecimal("84.00"));
            f.setStock(120);
            f.setMinimumStock(40);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Citrus, vegetables, paddy, cotton, horticulture");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Micronutrient standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Micronutrient");
            f.setDescription("Zinc Sulphate (Coromandel) - High performance Micronutrient fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-019")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-019");
            f.setName("Ferrous Sulphate");
            f.setBrand("AgroStar");
            f.setCategory(catMap.get("micronutrient"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("49.00"));
            f.setSellingPrice(new BigDecimal("65.00"));
            f.setStock(44);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Citrus, vegetables, paddy, cotton, horticulture");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Micronutrient standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Micronutrient");
            f.setDescription("Ferrous Sulphate (AgroStar) - High performance Micronutrient fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-020")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-020");
            f.setName("Boron 20%");
            f.setBrand("Multiplex");
            f.setCategory(catMap.get("micronutrient"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("125.00"));
            f.setSellingPrice(new BigDecimal("148.00"));
            f.setStock(27);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Citrus, vegetables, paddy, cotton, horticulture");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Micronutrient standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Micronutrient");
            f.setDescription("Boron 20% (Multiplex) - High performance Micronutrient fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-021")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-021");
            f.setName("Boron 10.5%");
            f.setBrand("Aries");
            f.setCategory(catMap.get("micronutrient"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("95.00"));
            f.setSellingPrice(new BigDecimal("115.00"));
            f.setStock(51);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Citrus, vegetables, paddy, cotton, horticulture");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Micronutrient standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Micronutrient");
            f.setDescription("Boron 10.5% (Aries) - High performance Micronutrient fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-022")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-022");
            f.setName("Manganese Sulphate");
            f.setBrand("IPL");
            f.setCategory(catMap.get("micronutrient"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("72.00"));
            f.setSellingPrice(new BigDecimal("89.00"));
            f.setStock(38);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Citrus, vegetables, paddy, cotton, horticulture");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Micronutrient standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Micronutrient");
            f.setDescription("Manganese Sulphate (IPL) - High performance Micronutrient fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-023")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-023");
            f.setName("Sulphur Bentonite");
            f.setBrand("Deepak");
            f.setCategory(catMap.get("sulphur"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("780.00"));
            f.setSellingPrice(new BigDecimal("890.00"));
            f.setStock(56);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Mustard, groundnut, pulses, oilseeds, onion, garlic");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Sulphur standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Sulphur");
            f.setDescription("Sulphur Bentonite (Deepak) - High performance Sulphur fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-024")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-024");
            f.setName("Elemental Sulphur");
            f.setBrand("Paradeep");
            f.setCategory(catMap.get("sulphur"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("620.00"));
            f.setSellingPrice(new BigDecimal("720.00"));
            f.setStock(41);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Mustard, groundnut, pulses, oilseeds, onion, garlic");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Sulphur standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Sulphur");
            f.setDescription("Elemental Sulphur (Paradeep) - High performance Sulphur fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-025")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-025");
            f.setName("Neem Cake");
            f.setBrand("Organic India");
            f.setCategory(catMap.get("organic"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("540.00"));
            f.setSellingPrice(new BigDecimal("650.00"));
            f.setStock(48);
            f.setMinimumStock(15);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Organic vegetables, fruits, tea, coffee, spices");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Organic standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Organic");
            f.setDescription("Neem Cake (Organic India) - High performance Organic fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-026")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-026");
            f.setName("Vermicompost");
            f.setBrand("Green Earth");
            f.setCategory(catMap.get("organic"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("280.00"));
            f.setSellingPrice(new BigDecimal("350.00"));
            f.setStock(85);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Organic vegetables, fruits, tea, coffee, spices");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Organic standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Organic");
            f.setDescription("Vermicompost (Green Earth) - High performance Organic fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-027")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-027");
            f.setName("Farmyard Manure");
            f.setBrand("Kisan Organics");
            f.setCategory(catMap.get("organic"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("180.00"));
            f.setSellingPrice(new BigDecimal("230.00"));
            f.setStock(110);
            f.setMinimumStock(30);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Organic vegetables, fruits, tea, coffee, spices");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Organic standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Organic");
            f.setDescription("Farmyard Manure (Kisan Organics) - High performance Organic fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-028")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-028");
            f.setName("Seaweed Extract");
            f.setBrand("Multiplex");
            f.setCategory(catMap.get("bio-fertilizer"));
            f.setUnit("Litre");
            f.setPurchasePrice(new BigDecimal("210.00"));
            f.setSellingPrice(new BigDecimal("260.00"));
            f.setStock(33);
            f.setMinimumStock(15);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Legumes, pulses, oilseeds, paddy, wheat");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Bio-Fertilizer standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 Litre");
            f.setStatus("ACTIVE");
            f.setType("Bio-Fertilizer");
            f.setDescription("Seaweed Extract (Multiplex) - High performance Bio-Fertilizer fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-029")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-029");
            f.setName("Humic Acid");
            f.setBrand("Aries");
            f.setCategory(catMap.get("bio-fertilizer"));
            f.setUnit("Litre");
            f.setPurchasePrice(new BigDecimal("195.00"));
            f.setSellingPrice(new BigDecimal("240.00"));
            f.setStock(60);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Legumes, pulses, oilseeds, paddy, wheat");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Bio-Fertilizer standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 Litre");
            f.setStatus("ACTIVE");
            f.setType("Bio-Fertilizer");
            f.setDescription("Humic Acid (Aries) - High performance Bio-Fertilizer fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-030")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-030");
            f.setName("Fulvic Acid");
            f.setBrand("AgriLife");
            f.setCategory(catMap.get("bio-fertilizer"));
            f.setUnit("Litre");
            f.setPurchasePrice(new BigDecimal("220.00"));
            f.setSellingPrice(new BigDecimal("275.00"));
            f.setStock(16);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Legumes, pulses, oilseeds, paddy, wheat");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Bio-Fertilizer standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 Litre");
            f.setStatus("ACTIVE");
            f.setType("Bio-Fertilizer");
            f.setDescription("Fulvic Acid (AgriLife) - High performance Bio-Fertilizer fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-031")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-031");
            f.setName("Azospirillum");
            f.setBrand("BioGrow");
            f.setCategory(catMap.get("bio-fertilizer"));
            f.setUnit("Bottle");
            f.setPurchasePrice(new BigDecimal("75.00"));
            f.setSellingPrice(new BigDecimal("95.00"));
            f.setStock(45);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Legumes, pulses, oilseeds, paddy, wheat");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Bio-Fertilizer standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("500 ml");
            f.setStatus("ACTIVE");
            f.setType("Bio-Fertilizer");
            f.setDescription("Azospirillum (BioGrow) - High performance Bio-Fertilizer fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-032")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-032");
            f.setName("Azotobacter");
            f.setBrand("BioGrow");
            f.setCategory(catMap.get("bio-fertilizer"));
            f.setUnit("Bottle");
            f.setPurchasePrice(new BigDecimal("72.00"));
            f.setSellingPrice(new BigDecimal("92.00"));
            f.setStock(31);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Legumes, pulses, oilseeds, paddy, wheat");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Bio-Fertilizer standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("500 ml");
            f.setStatus("ACTIVE");
            f.setType("Bio-Fertilizer");
            f.setDescription("Azotobacter (BioGrow) - High performance Bio-Fertilizer fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-033")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-033");
            f.setName("Phosphate Solubilizing Bacteria");
            f.setBrand("IFFCO Bio");
            f.setCategory(catMap.get("bio-fertilizer"));
            f.setUnit("Bottle");
            f.setPurchasePrice(new BigDecimal("80.00"));
            f.setSellingPrice(new BigDecimal("105.00"));
            f.setStock(29);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Legumes, pulses, oilseeds, paddy, wheat");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Bio-Fertilizer standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("500 ml");
            f.setStatus("ACTIVE");
            f.setType("Bio-Fertilizer");
            f.setDescription("Phosphate Solubilizing Bacteria (IFFCO Bio) - High performance Bio-Fertilizer fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-034")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-034");
            f.setName("Potassium Mobilizing Bacteria");
            f.setBrand("IFFCO Bio");
            f.setCategory(catMap.get("bio-fertilizer"));
            f.setUnit("Bottle");
            f.setPurchasePrice(new BigDecimal("85.00"));
            f.setSellingPrice(new BigDecimal("110.00"));
            f.setStock(18);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Legumes, pulses, oilseeds, paddy, wheat");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Bio-Fertilizer standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("500 ml");
            f.setStatus("ACTIVE");
            f.setType("Bio-Fertilizer");
            f.setDescription("Potassium Mobilizing Bacteria (IFFCO Bio) - High performance Bio-Fertilizer fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-035")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-035");
            f.setName("Mono Ammonium Phosphate");
            f.setBrand("Coromandel");
            f.setCategory(catMap.get("phosphate"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("1320.00"));
            f.setSellingPrice(new BigDecimal("1470.00"));
            f.setStock(39);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Pulses, Oilseeds, Cereals, Root Crops");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Phosphate standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Phosphate");
            f.setDescription("Mono Ammonium Phosphate (Coromandel) - High performance Phosphate fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-036")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-036");
            f.setName("Mono Potassium Phosphate");
            f.setBrand("Haifa");
            f.setCategory(catMap.get("potassic"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("185.00"));
            f.setSellingPrice(new BigDecimal("220.00"));
            f.setStock(24);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Potato, Banana, Sugarcane, Fruits, Vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Potassic standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Potassic");
            f.setDescription("Mono Potassium Phosphate (Haifa) - High performance Potassic fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-037")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-037");
            f.setName("Potassium Nitrate");
            f.setBrand("Yara");
            f.setCategory(catMap.get("potassic"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("145.00"));
            f.setSellingPrice(new BigDecimal("178.00"));
            f.setStock(68);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Potato, Banana, Sugarcane, Fruits, Vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Potassic standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Potassic");
            f.setDescription("Potassium Nitrate (Yara) - High performance Potassic fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-038")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-038");
            f.setName("Calcium Ammonium Nitrate");
            f.setBrand("Yara");
            f.setCategory(catMap.get("nitrogen"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("710.00"));
            f.setSellingPrice(new BigDecimal("815.00"));
            f.setStock(47);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Wheat, Rice, Maize, Sugarcane, Cotton");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Nitrogen standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Nitrogen");
            f.setDescription("Calcium Ammonium Nitrate (Yara) - High performance Nitrogen fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-039")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-039");
            f.setName("UAN 28");
            f.setBrand("AgriChem");
            f.setCategory(catMap.get("nitrogen"));
            f.setUnit("Litre");
            f.setPurchasePrice(new BigDecimal("48.00"));
            f.setSellingPrice(new BigDecimal("60.00"));
            f.setStock(75);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Wheat, Rice, Maize, Sugarcane, Cotton");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Nitrogen standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 Litre");
            f.setStatus("ACTIVE");
            f.setType("Nitrogen");
            f.setDescription("UAN 28 (AgriChem) - High performance Nitrogen fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-040")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-040");
            f.setName("UAN 32");
            f.setBrand("AgriChem");
            f.setCategory(catMap.get("nitrogen"));
            f.setUnit("Litre");
            f.setPurchasePrice(new BigDecimal("52.00"));
            f.setSellingPrice(new BigDecimal("65.00"));
            f.setStock(12);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Wheat, Rice, Maize, Sugarcane, Cotton");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Nitrogen standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 Litre");
            f.setStatus("ACTIVE");
            f.setType("Nitrogen");
            f.setDescription("UAN 32 (AgriChem) - High performance Nitrogen fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-041")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-041");
            f.setName("NPK 08:21:21");
            f.setBrand("Zuari");
            f.setCategory(catMap.get("npk"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("980.00"));
            f.setSellingPrice(new BigDecimal("1100.00"));
            f.setStock(61);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("All field crops, cereals, pulses, vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("NPK 08:21:21 balanced formulation");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("NPK");
            f.setDescription("NPK 08:21:21 (Zuari) - High performance NPK fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-042")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-042");
            f.setName("NPK 14:28:14");
            f.setBrand("FACT");
            f.setCategory(catMap.get("npk"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("1210.00"));
            f.setSellingPrice(new BigDecimal("1350.00"));
            f.setStock(26);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("All field crops, cereals, pulses, vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("NPK 14:28:14 balanced formulation");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("NPK");
            f.setDescription("NPK 14:28:14 (FACT) - High performance NPK fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-043")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-043");
            f.setName("NPK 17:17:17");
            f.setBrand("Mangalore Chemicals");
            f.setCategory(catMap.get("npk"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("1180.00"));
            f.setSellingPrice(new BigDecimal("1320.00"));
            f.setStock(73);
            f.setMinimumStock(30);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("All field crops, cereals, pulses, vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("NPK 17:17:17 balanced formulation");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("NPK");
            f.setDescription("NPK 17:17:17 (Mangalore Chemicals) - High performance NPK fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-044")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-044");
            f.setName("NPK 19:19:19 Premium");
            f.setBrand("Kristalon");
            f.setCategory(catMap.get("water soluble"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("108.00"));
            f.setSellingPrice(new BigDecimal("128.00"));
            f.setStock(91);
            f.setMinimumStock(30);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Greenhouse crops, drip irrigation, fertigation, horticulture");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("NPK 19:19:19 Premium balanced formulation");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Water Soluble");
            f.setDescription("NPK 19:19:19 Premium (Kristalon) - High performance Water Soluble fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-045")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-045");
            f.setName("MAP 12:61:00");
            f.setBrand("Haifa");
            f.setCategory(catMap.get("water soluble"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("175.00"));
            f.setSellingPrice(new BigDecimal("210.00"));
            f.setStock(43);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Greenhouse crops, drip irrigation, fertigation, horticulture");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Water Soluble standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Water Soluble");
            f.setDescription("MAP 12:61:00 (Haifa) - High performance Water Soluble fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-046")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-046");
            f.setName("Potassium Schoenite");
            f.setBrand("IPL");
            f.setCategory(catMap.get("potassic"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("760.00"));
            f.setSellingPrice(new BigDecimal("850.00"));
            f.setStock(52);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Potato, Banana, Sugarcane, Fruits, Vegetables");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Potassic standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Potassic");
            f.setDescription("Potassium Schoenite (IPL) - High performance Potassic fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-047")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-047");
            f.setName("Gypsum");
            f.setBrand("Agrimax");
            f.setCategory(catMap.get("sulphur"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("330.00"));
            f.setSellingPrice(new BigDecimal("400.00"));
            f.setStock(67);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Mustard, groundnut, pulses, oilseeds, onion, garlic");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Sulphur standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Sulphur");
            f.setDescription("Gypsum (Agrimax) - High performance Sulphur fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-048")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-048");
            f.setName("Dolomite");
            f.setBrand("Kisan Minerals");
            f.setCategory(catMap.get("organic"));
            f.setUnit("Bag");
            f.setPurchasePrice(new BigDecimal("290.00"));
            f.setSellingPrice(new BigDecimal("365.00"));
            f.setStock(14);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Organic vegetables, fruits, tea, coffee, spices");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Organic standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("50 kg");
            f.setStatus("ACTIVE");
            f.setType("Organic");
            f.setDescription("Dolomite (Kisan Minerals) - High performance Organic fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-049")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-049");
            f.setName("Bio NPK");
            f.setBrand("AgroStar");
            f.setCategory(catMap.get("bio-fertilizer"));
            f.setUnit("Bottle");
            f.setPurchasePrice(new BigDecimal("105.00"));
            f.setSellingPrice(new BigDecimal("135.00"));
            f.setStock(57);
            f.setMinimumStock(20);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Legumes, pulses, oilseeds, paddy, wheat");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Bio NPK balanced formulation");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("500 ml");
            f.setStatus("ACTIVE");
            f.setType("Bio-Fertilizer");
            f.setDescription("Bio NPK (AgroStar) - High performance Bio-Fertilizer fertilizer.");
            fertilizerRepository.save(f);
        }
        if (!fertilizerRepository.existsBySkuIgnoreCase("FRT-050")) {
            Fertilizer f = new Fertilizer();
            f.setSku("FRT-050");
            f.setName("Micro Nutrient Mix");
            f.setBrand("Multiplex");
            f.setCategory(catMap.get("micronutrient"));
            f.setUnit("Kg");
            f.setPurchasePrice(new BigDecimal("135.00"));
            f.setSellingPrice(new BigDecimal("165.00"));
            f.setStock(32);
            f.setMinimumStock(25);
            f.setImageUrl("../img/img1.jpeg");
            f.setSuitableCrops("Citrus, vegetables, paddy, cotton, horticulture");
            f.setSuitableSoil("Suitable for all soil types (Alluvial, Black, Red, Sandy Loam)");
            f.setComposition("Micronutrient standard agricultural grade");
            f.setUsageInstructions("Apply according to recommended soil test report and local crop schedule.");
            f.setDiscountPercent(BigDecimal.ZERO);
            f.setTaxPercent(new BigDecimal("5.00"));
            f.setWeightValue("1 kg");
            f.setStatus("ACTIVE");
            f.setType("Micronutrient");
            f.setDescription("Micro Nutrient Mix (Multiplex) - High performance Micronutrient fertilizer.");
            fertilizerRepository.save(f);
        }
    }

    private void seedDefaultUsers() {
        // Demote any non-rahul ADMIN to SALES_STAFF to guarantee exactly one ADMIN
        List<User> existingAdmins = userRepository.findAll().stream()
                .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole()) && !"rahul".equalsIgnoreCase(u.getUsername()))
                .collect(Collectors.toList());
        for (User oldAdmin : existingAdmins) {
            oldAdmin.setRole("SALES_STAFF");
            userRepository.save(oldAdmin);
            log.info("Demoted unauthorized admin user '{}' to SALES_STAFF", oldAdmin.getUsername());
        }

        // Check if rahul exists by username or email
        Optional<User> rahulOpt = userRepository.findByUsername("rahul");
        if (rahulOpt.isEmpty()) {
            rahulOpt = userRepository.findByEmail("rahul@gmail.com");
        }

        if (rahulOpt.isEmpty()) {
            // Check if legacy 'admin' exists to convert it safely
            Optional<User> legacyAdmin = userRepository.findByUsername("admin");
            if (legacyAdmin.isPresent()) {
                User rahul = legacyAdmin.get();
                rahul.setUsername("rahul");
                rahul.setName("Rahul Kale");
                rahul.setEmail("rahul@gmail.com");
                rahul.setPassword(passwordEncoder.encode("rahul"));
                rahul.setRole("ADMIN");
                rahul.setActive(true);
                userRepository.save(rahul);
                log.info("Safely converted legacy admin user to Rahul Kale (rahul / rahul@gmail.com)");
            } else {
                User rahul = new User();
                rahul.setUsername("rahul");
                rahul.setName("Rahul Kale");
                rahul.setEmail("rahul@gmail.com");
                rahul.setPassword(passwordEncoder.encode("rahul"));
                rahul.setRole("ADMIN");
                rahul.setActive(true);
                userRepository.save(rahul);
                log.info("Seeded sole administrator user: rahul / rahul (Rahul Kale)");
            }
        } else {
            User rahul = rahulOpt.get();
            rahul.setUsername("rahul");
            rahul.setName("Rahul Kale");
            rahul.setEmail("rahul@gmail.com");
            rahul.setPassword(passwordEncoder.encode("rahul"));
            rahul.setRole("ADMIN");
            rahul.setActive(true);
            userRepository.save(rahul);
            log.info("Synchronized sole administrator user: rahul (Rahul Kale)");
        }

        Optional<User> rameshOpt = userRepository.findByUsername("farmer_ramesh");
        if (rameshOpt.isEmpty()) {
            User ramesh = new User();
            ramesh.setUsername("farmer_ramesh");
            ramesh.setPassword(passwordEncoder.encode("ramesh123"));
            ramesh.setName("Ramesh Patil");
            ramesh.setEmail("ramesh@smartagri.com");
            ramesh.setRole("CUSTOMER");
            ramesh.setActive(true);
            userRepository.save(ramesh);
            log.info("Seeded default customer user: farmer_ramesh / ramesh123");
        } else {
            User ramesh = rameshOpt.get();
            ramesh.setPassword(passwordEncoder.encode("ramesh123"));
            ramesh.setRole("CUSTOMER");
            ramesh.setActive(true);
            userRepository.save(ramesh);
            log.info("Synchronized password for farmer_ramesh / ramesh123");
        }
    }

    private void seedDefaultWarehouse() {
        if (!warehouseRepository.existsByWarehouseCode("WH-PUNE-01")) {
            Warehouse wh = new Warehouse();
            wh.setWarehouseCode("WH-PUNE-01");
            wh.setWarehouseName("Pune Central Agri-Warehouse");
            wh.setWarehouseType(WarehouseType.CENTRAL);
            wh.setAddress("Plot 104, Agro Tech Hub, Hinjawadi Phase 1");
            wh.setCity("Pune");
            wh.setState("Maharashtra");
            wh.setPostalCode("411057");
            wh.setContactPerson("Sanjay Shinde");
            wh.setContactPhone("9822110022");
            wh.setEmail("warehouse.pune@smartagri.com");
            wh.setStatus(WarehouseStatus.ACTIVE);
            wh.setCapacity(50000);
            wh.setIsDefaultReceiving(true);
            Warehouse savedWh = warehouseRepository.save(wh);

            WarehouseLocation zone = new WarehouseLocation();
            zone.setWarehouse(savedWh);
            zone.setLocationCode("ZONE-A");
            zone.setName("Main Receiving & Storage Zone");
            zone.setLocationType(LocationType.ZONE);
            zone.setStatus(LocationStatus.ACTIVE);
            zone.setCapacity(25000);
            WarehouseLocation savedZone = warehouseLocationRepository.save(zone);

            WarehouseLocation bin = new WarehouseLocation();
            bin.setWarehouse(savedWh);
            bin.setParentLocation(savedZone);
            bin.setLocationCode("BIN-A1");
            bin.setName("Standard Pallet Rack A-01");
            bin.setLocationType(LocationType.BIN);
            bin.setStatus(LocationStatus.ACTIVE);
            bin.setCapacity(5000);
            warehouseLocationRepository.save(bin);

            log.info("Seeded default warehouse: WH-PUNE-01 with ZONE-A and BIN-A1");
        }
    }

    private void seedDefaultSuppliers() {
        if (!supplierRepository.existsBySupplierCode("SUP-IFFCO-01")) {
            Supplier s1 = new Supplier();
            s1.setSupplierCode("SUP-IFFCO-01");
            s1.setCompanyName("IFFCO National Fertilizer Supply");
            s1.setContactPerson("Shri R. K. Sharma");
            s1.setEmail("supply@iffco.in");
            s1.setPhone("011-26542000");
            s1.setCity("New Delhi");
            s1.setState("Delhi");
            s1.setPostalCode("110016");
            s1.setGstNumber("07AAACI1234F1Z5");
            s1.setPaymentTerms("NET 30");
            s1.setStatus(SupplierStatus.ACTIVE);
            supplierRepository.save(s1);
            log.info("Seeded default supplier: SUP-IFFCO-01");
        }

        if (!supplierRepository.existsBySupplierCode("SUP-COROMANDEL-01")) {
            Supplier s2 = new Supplier();
            s2.setSupplierCode("SUP-COROMANDEL-01");
            s2.setCompanyName("Coromandel International Ltd");
            s2.setContactPerson("Anand Kulkarni");
            s2.setEmail("procure@coromandel.biz");
            s2.setPhone("040-27842034");
            s2.setCity("Secunderabad");
            s2.setState("Telangana");
            s2.setPostalCode("500003");
            s2.setGstNumber("36AAACC1234A1Z1");
            s2.setPaymentTerms("NET 45");
            s2.setStatus(SupplierStatus.ACTIVE);
            supplierRepository.save(s2);
            log.info("Seeded default supplier: SUP-COROMANDEL-01");
        }
    }
}
