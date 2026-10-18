package com.smartagri.service;

import com.smartagri.dto.*;
import com.smartagri.entity.*;
import com.smartagri.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WarehouseService {

    private static final Logger log = LoggerFactory.getLogger(WarehouseService.class);

    private final WarehouseRepository warehouseRepository;
    private final WarehouseLocationRepository locationRepository;
    private final BatchLocationStockRepository batchLocationStockRepository;
    private final StockTransferRepository stockTransferRepository;
    private final FertilizerBatchRepository batchRepository;

    public WarehouseService(WarehouseRepository warehouseRepository,
                            WarehouseLocationRepository locationRepository,
                            BatchLocationStockRepository batchLocationStockRepository,
                            StockTransferRepository stockTransferRepository,
                            FertilizerBatchRepository batchRepository) {
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.batchLocationStockRepository = batchLocationStockRepository;
        this.stockTransferRepository = stockTransferRepository;
        this.batchRepository = batchRepository;
    }

    // ------------------------------------------------------------------------
    // Warehouse Operations
    // ------------------------------------------------------------------------

    @Transactional
    public WarehouseDTO createWarehouse(CreateWarehouseRequest req, String performedBy) {
        if (req.getWarehouseCode() == null || req.getWarehouseCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse code is strictly required");
        }
        if (req.getWarehouseName() == null || req.getWarehouseName().trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse name is strictly required");
        }
        if (req.getCapacity() != null && req.getCapacity() < 0) {
            throw new IllegalArgumentException("Warehouse capacity cannot be negative");
        }

        String cleanCode = req.getWarehouseCode().trim().toUpperCase(Locale.ROOT);
        if (warehouseRepository.existsByWarehouseCode(cleanCode)) {
            throw new IllegalStateException("Warehouse code '" + cleanCode + "' already exists.");
        }

        // If marked as default receiving, unset any existing default receiving warehouse
        if (Boolean.TRUE.equals(req.getIsDefaultReceiving())) {
            unsetDefaultReceivingWarehouse();
        }

        Warehouse w = new Warehouse();
        w.setWarehouseCode(cleanCode);
        w.setWarehouseName(req.getWarehouseName().trim());
        w.setWarehouseType(req.getWarehouseType() != null ? req.getWarehouseType() : WarehouseType.CENTRAL);
        w.setAddress(req.getAddress() != null ? req.getAddress().trim() : null);
        w.setCity(req.getCity() != null ? req.getCity().trim() : null);
        w.setState(req.getState() != null ? req.getState().trim() : null);
        w.setPostalCode(req.getPostalCode() != null ? req.getPostalCode().trim() : null);
        w.setContactPerson(req.getContactPerson() != null ? req.getContactPerson().trim() : null);
        w.setContactPhone(req.getContactPhone() != null ? req.getContactPhone().trim() : null);
        w.setEmail(req.getEmail() != null ? req.getEmail().trim() : null);
        w.setCapacity(req.getCapacity() != null ? req.getCapacity() : 0);
        w.setIsDefaultReceiving(Boolean.TRUE.equals(req.getIsDefaultReceiving()));
        w.setStatus(WarehouseStatus.ACTIVE);
        w.setNotes(req.getNotes() != null ? req.getNotes().trim() : null);

        Warehouse saved = warehouseRepository.save(w);
        log.info("Warehouse created: #{} [{}] by {}", saved.getId(), saved.getWarehouseCode(), performedBy);
        return WarehouseDTO.fromEntity(saved, 0);
    }

    @Transactional
    public WarehouseDTO updateWarehouse(Long id, UpdateWarehouseRequest req, String performedBy) {
        Warehouse w = warehouseRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + id));

        if (req.getWarehouseName() != null && !req.getWarehouseName().trim().isEmpty()) {
            w.setWarehouseName(req.getWarehouseName().trim());
        }
        if (req.getWarehouseType() != null) {
            w.setWarehouseType(req.getWarehouseType());
        }
        if (req.getCapacity() != null) {
            if (req.getCapacity() < 0) {
                throw new IllegalArgumentException("Warehouse capacity cannot be negative");
            }
            w.setCapacity(req.getCapacity());
        }
        if (req.getAddress() != null) w.setAddress(req.getAddress().trim());
        if (req.getCity() != null) w.setCity(req.getCity().trim());
        if (req.getState() != null) w.setState(req.getState().trim());
        if (req.getPostalCode() != null) w.setPostalCode(req.getPostalCode().trim());
        if (req.getContactPerson() != null) w.setContactPerson(req.getContactPerson().trim());
        if (req.getContactPhone() != null) w.setContactPhone(req.getContactPhone().trim());
        if (req.getEmail() != null) w.setEmail(req.getEmail().trim());
        if (req.getNotes() != null) w.setNotes(req.getNotes().trim());

        if (Boolean.TRUE.equals(req.getIsDefaultReceiving())) {
            unsetDefaultReceivingWarehouse();
            w.setIsDefaultReceiving(true);
        } else if (Boolean.FALSE.equals(req.getIsDefaultReceiving())) {
            w.setIsDefaultReceiving(false);
        }

        Warehouse saved = warehouseRepository.save(w);
        int util = calculateWarehouseUtilization(saved.getId());
        log.info("Warehouse #{} [{}] updated by {}", saved.getId(), saved.getWarehouseCode(), performedBy);
        return WarehouseDTO.fromEntity(saved, util);
    }

    @Transactional
    public WarehouseDTO activateWarehouse(Long id, String performedBy) {
        Warehouse w = warehouseRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + id));
        w.setStatus(WarehouseStatus.ACTIVE);
        Warehouse saved = warehouseRepository.save(w);
        log.info("Warehouse #{} [{}] activated by {}", saved.getId(), saved.getWarehouseCode(), performedBy);
        return WarehouseDTO.fromEntity(saved, calculateWarehouseUtilization(saved.getId()));
    }

    @Transactional
    public WarehouseDTO deactivateWarehouse(Long id, String performedBy) {
        Warehouse w = warehouseRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + id));
        w.setStatus(WarehouseStatus.INACTIVE);
        Warehouse saved = warehouseRepository.save(w);
        log.info("Warehouse #{} [{}] deactivated by {}", saved.getId(), saved.getWarehouseCode(), performedBy);
        return WarehouseDTO.fromEntity(saved, calculateWarehouseUtilization(saved.getId()));
    }

    @Transactional
    public WarehouseDTO blockWarehouse(Long id, String performedBy) {
        Warehouse w = warehouseRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + id));
        w.setStatus(WarehouseStatus.BLOCKED);
        Warehouse saved = warehouseRepository.save(w);
        log.info("Warehouse #{} [{}] blocked by {}", saved.getId(), saved.getWarehouseCode(), performedBy);
        return WarehouseDTO.fromEntity(saved, calculateWarehouseUtilization(saved.getId()));
    }

    @Transactional(readOnly = true)
    public WarehouseDTO getWarehouse(Long id) {
        Warehouse w = warehouseRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + id));
        return WarehouseDTO.fromEntity(w, calculateWarehouseUtilization(w.getId()));
    }

    @Transactional(readOnly = true)
    public List<WarehouseDTO> getAllWarehouses(String status, String search, String type) {
        List<Warehouse> list = warehouseRepository.findAll();

        return list.stream()
                .filter(w -> {
                    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
                        if (!w.getStatus().name().equalsIgnoreCase(status.trim())) {
                            return false;
                        }
                    }
                    if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type.trim())) {
                        if (!w.getWarehouseType().name().equalsIgnoreCase(type.trim())) {
                            return false;
                        }
                    }
                    if (search != null && !search.isBlank()) {
                        String q = search.trim().toLowerCase(Locale.ROOT);
                        boolean matchName = w.getWarehouseName() != null && w.getWarehouseName().toLowerCase(Locale.ROOT).contains(q);
                        boolean matchCode = w.getWarehouseCode() != null && w.getWarehouseCode().toLowerCase(Locale.ROOT).contains(q);
                        boolean matchCity = w.getCity() != null && w.getCity().toLowerCase(Locale.ROOT).contains(q);
                        if (!matchName && !matchCode && !matchCity) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(w -> WarehouseDTO.fromEntity(w, calculateWarehouseUtilization(w.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BatchLocationStockDTO> getWarehouseInventory(Long warehouseId) {
        Warehouse w = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + warehouseId));
        return batchLocationStockRepository.findByWarehouseId(w.getId()).stream()
                .map(BatchLocationStockDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StockTransferDTO> getWarehouseTransfers(Long warehouseId) {
        Warehouse w = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + warehouseId));
        return stockTransferRepository.findBySourceWarehouseIdOrDestinationWarehouseId(w.getId(), w.getId()).stream()
                .map(StockTransferDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public Warehouse getDefaultReceivingWarehouse() {
        return warehouseRepository.findFirstByIsDefaultReceivingTrueAndStatus(WarehouseStatus.ACTIVE)
                .orElseGet(() -> warehouseRepository.findByStatus(WarehouseStatus.ACTIVE).stream().findFirst().orElse(null));
    }

    @Transactional
    public WarehouseDTO setDefaultReceivingWarehouse(Long id, String performedBy) {
        Warehouse w = warehouseRepository.findByIdWithLock(id)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + id));
        unsetDefaultReceivingWarehouse();
        w.setIsDefaultReceiving(true);
        Warehouse saved = warehouseRepository.save(w);
        log.info("Warehouse #{} [{}] set as default receiving by {}", saved.getId(), saved.getWarehouseCode(), performedBy);
        return WarehouseDTO.fromEntity(saved, calculateWarehouseUtilization(saved.getId()));
    }

    private void unsetDefaultReceivingWarehouse() {
        List<Warehouse> all = warehouseRepository.findAll();
        for (Warehouse existing : all) {
            if (Boolean.TRUE.equals(existing.getIsDefaultReceiving())) {
                existing.setIsDefaultReceiving(false);
                warehouseRepository.save(existing);
            }
        }
    }

    public int calculateWarehouseUtilization(Long warehouseId) {
        List<BatchLocationStock> list = batchLocationStockRepository.findByWarehouseId(warehouseId);
        int sum = 0;
        for (BatchLocationStock bls : list) {
            sum += (bls.getQuantity() != null ? bls.getQuantity() : 0);
        }
        return sum;
    }

    // ------------------------------------------------------------------------
    // Location Operations
    // ------------------------------------------------------------------------

    @Transactional
    public WarehouseLocationDTO createLocation(Long warehouseId, CreateWarehouseLocationRequest req, String performedBy) {
        if (req.getLocationCode() == null || req.getLocationCode().trim().isEmpty()) {
            throw new IllegalArgumentException("Location code is strictly required");
        }
        if (req.getCapacity() != null && req.getCapacity() < 0) {
            throw new IllegalArgumentException("Location capacity cannot be negative");
        }

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + warehouseId));

        String cleanCode = req.getLocationCode().trim().toUpperCase(Locale.ROOT);
        if (locationRepository.existsByWarehouseIdAndLocationCode(warehouse.getId(), cleanCode)) {
            throw new IllegalStateException("Location code '" + cleanCode + "' already exists in warehouse '" + warehouse.getWarehouseCode() + "'.");
        }

        WarehouseLocation parent = null;
        if (req.getParentLocationId() != null) {
            parent = locationRepository.findById(req.getParentLocationId())
                    .orElseThrow(() -> new NoSuchElementException("Parent location not found with id: " + req.getParentLocationId()));
            if (!parent.getWarehouse().getId().equals(warehouse.getId())) {
                throw new IllegalArgumentException("Parent location must belong to the same warehouse");
            }
        }

        String locName = req.getName();
        if (locName == null || locName.trim().isEmpty()) {
            locName = cleanCode;
        }

        WarehouseLocation loc = new WarehouseLocation();
        loc.setWarehouse(warehouse);
        loc.setLocationCode(cleanCode);
        loc.setLocationType(req.getLocationType() != null ? req.getLocationType() : LocationType.BIN);
        loc.setParentLocation(parent);
        loc.setName(locName.trim());
        loc.setCapacity(req.getCapacity() != null ? req.getCapacity() : 0);
        loc.setMaxWeightKg(req.getMaxWeightKg());
        loc.setIsTemperatureControlled(req.getIsTemperatureControlled() != null ? req.getIsTemperatureControlled() : false);
        loc.setIsHazardousAllowed(req.getIsHazardousAllowed() != null ? req.getIsHazardousAllowed() : false);
        loc.setStatus(LocationStatus.ACTIVE);
        loc.setNotes(req.getNotes() != null ? req.getNotes().trim() : null);

        WarehouseLocation saved = locationRepository.save(loc);
        log.info("Location created: #{} [{}] in warehouse [{}] by {}",
                saved.getId(), saved.getLocationCode(), warehouse.getWarehouseCode(), performedBy);
        return WarehouseLocationDTO.fromEntity(saved, 0);
    }

    @Transactional
    public WarehouseLocationDTO updateLocation(Long locationId, UpdateWarehouseLocationRequest req, String performedBy) {
        WarehouseLocation loc = locationRepository.findByIdWithLock(locationId)
                .orElseThrow(() -> new NoSuchElementException("Location not found with id: " + locationId));

        if (req.getName() != null && !req.getName().trim().isEmpty()) {
            loc.setName(req.getName().trim());
        }
        if (req.getLocationType() != null) {
            loc.setLocationType(req.getLocationType());
        }
        if (req.getCapacity() != null) {
            if (req.getCapacity() < 0) {
                throw new IllegalArgumentException("Location capacity cannot be negative");
            }
            loc.setCapacity(req.getCapacity());
        }
        if (req.getMaxWeightKg() != null) {
            loc.setMaxWeightKg(req.getMaxWeightKg());
        }
        if (req.getIsTemperatureControlled() != null) {
            loc.setIsTemperatureControlled(req.getIsTemperatureControlled());
        }
        if (req.getIsHazardousAllowed() != null) {
            loc.setIsHazardousAllowed(req.getIsHazardousAllowed());
        }
        if (req.getParentLocationId() != null) {
            WarehouseLocation parent = locationRepository.findById(req.getParentLocationId())
                    .orElseThrow(() -> new NoSuchElementException("Parent location not found: " + req.getParentLocationId()));
            if (!parent.getWarehouse().getId().equals(loc.getWarehouse().getId())) {
                throw new IllegalArgumentException("Parent location must belong to the same warehouse");
            }
            if (parent.getId().equals(loc.getId())) {
                throw new IllegalArgumentException("Location cannot be its own parent");
            }
            loc.setParentLocation(parent);
        }
        if (req.getNotes() != null) loc.setNotes(req.getNotes().trim());

        WarehouseLocation saved = locationRepository.save(loc);
        int util = batchLocationStockRepository.sumQuantityByLocationId(saved.getId());
        log.info("Location #{} [{}] updated by {}", saved.getId(), saved.getLocationCode(), performedBy);
        return WarehouseLocationDTO.fromEntity(saved, util);
    }

    @Transactional
    public WarehouseLocationDTO activateLocation(Long locationId, String performedBy) {
        WarehouseLocation loc = locationRepository.findByIdWithLock(locationId)
                .orElseThrow(() -> new NoSuchElementException("Location not found with id: " + locationId));
        loc.setStatus(LocationStatus.ACTIVE);
        WarehouseLocation saved = locationRepository.save(loc);
        int util = batchLocationStockRepository.sumQuantityByLocationId(saved.getId());
        log.info("Location #{} [{}] activated by {}", saved.getId(), saved.getLocationCode(), performedBy);
        return WarehouseLocationDTO.fromEntity(saved, util);
    }

    @Transactional
    public WarehouseLocationDTO deactivateLocation(Long locationId, String performedBy) {
        WarehouseLocation loc = locationRepository.findByIdWithLock(locationId)
                .orElseThrow(() -> new NoSuchElementException("Location not found with id: " + locationId));
        loc.setStatus(LocationStatus.INACTIVE);
        WarehouseLocation saved = locationRepository.save(loc);
        int util = batchLocationStockRepository.sumQuantityByLocationId(saved.getId());
        log.info("Location #{} [{}] deactivated by {}", saved.getId(), saved.getLocationCode(), performedBy);
        return WarehouseLocationDTO.fromEntity(saved, util);
    }

    @Transactional
    public WarehouseLocationDTO blockLocation(Long locationId, String performedBy) {
        WarehouseLocation loc = locationRepository.findByIdWithLock(locationId)
                .orElseThrow(() -> new NoSuchElementException("Location not found with id: " + locationId));
        loc.setStatus(LocationStatus.BLOCKED);
        WarehouseLocation saved = locationRepository.save(loc);
        int util = batchLocationStockRepository.sumQuantityByLocationId(saved.getId());
        log.info("Location #{} [{}] blocked by {}", saved.getId(), saved.getLocationCode(), performedBy);
        return WarehouseLocationDTO.fromEntity(saved, util);
    }

    @Transactional(readOnly = true)
    public WarehouseLocationDTO getLocation(Long locationId) {
        WarehouseLocation loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new NoSuchElementException("Location not found with id: " + locationId));
        int util = batchLocationStockRepository.sumQuantityByLocationId(loc.getId());
        return WarehouseLocationDTO.fromEntity(loc, util);
    }

    @Transactional(readOnly = true)
    public List<WarehouseLocationDTO> getLocationsByWarehouse(Long warehouseId, String status, String type) {
        Warehouse w = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NoSuchElementException("Warehouse not found with id: " + warehouseId));
        List<WarehouseLocation> list = locationRepository.findByWarehouseId(w.getId());

        return list.stream()
                .filter(l -> {
                    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status.trim())) {
                        if (!l.getStatus().name().equalsIgnoreCase(status.trim())) {
                            return false;
                        }
                    }
                    if (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type.trim())) {
                        if (!l.getLocationType().name().equalsIgnoreCase(type.trim())) {
                            return false;
                        }
                    }
                    return true;
                })
                .map(l -> {
                    int util = batchLocationStockRepository.sumQuantityByLocationId(l.getId());
                    return WarehouseLocationDTO.fromEntity(l, util);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<BatchLocationStockDTO> getLocationInventory(Long locationId) {
        WarehouseLocation loc = locationRepository.findById(locationId)
                .orElseThrow(() -> new NoSuchElementException("Location not found with id: " + locationId));
        return batchLocationStockRepository.findByLocationId(loc.getId()).stream()
                .map(BatchLocationStockDTO::fromEntity)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------------
    // Dashboard Summary
    // ------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public WarehouseDashboardDTO getDashboard() {
        List<Warehouse> warehouses = warehouseRepository.findAll();
        long totalWarehouses = warehouses.size();
        long activeWarehouses = warehouses.stream().filter(w -> w.getStatus() == WarehouseStatus.ACTIVE).count();

        List<WarehouseLocation> locations = locationRepository.findAll();
        long totalLocations = locations.size();

        List<BatchLocationStock> allStock = batchLocationStockRepository.findAll();
        long totalInventoryUnits = 0;
        Set<Long> occupiedLocationIds = new HashSet<>();

        for (BatchLocationStock s : allStock) {
            int q = s.getQuantity() != null ? s.getQuantity() : 0;
            totalInventoryUnits += q;
            if (q > 0 && s.getLocation() != null) {
                occupiedLocationIds.add(s.getLocation().getId());
            }
        }
        long occupiedLocations = occupiedLocationIds.size();

        // Batches status count
        List<FertilizerBatch> batches = batchRepository.findAll();
        long nearExpiryBatches = 0;
        long expiredBatches = 0;
        for (FertilizerBatch b : batches) {
            BatchStatus status = b.computeDynamicStatus();
            if (status == BatchStatus.NEAR_EXPIRY) nearExpiryBatches++;
            if (status == BatchStatus.EXPIRED) expiredBatches++;
        }

        // Transfers in progress
        List<StockTransfer> transfers = stockTransferRepository.findAll();
        long pendingTransfers = transfers.stream()
                .filter(t -> t.getStatus() == StockTransferStatus.PENDING_APPROVAL || t.getStatus() == StockTransferStatus.APPROVED)
                .count();
        long transfersInTransit = transfers.stream()
                .filter(t -> t.getStatus() == StockTransferStatus.DISPATCHED || t.getStatus() == StockTransferStatus.PARTIALLY_RECEIVED)
                .count();

        return new WarehouseDashboardDTO(
                totalWarehouses,
                activeWarehouses,
                totalLocations,
                occupiedLocations,
                totalInventoryUnits,
                0, // low stock handled by inventory
                nearExpiryBatches,
                expiredBatches,
                pendingTransfers,
                transfersInTransit
        );
    }
}
