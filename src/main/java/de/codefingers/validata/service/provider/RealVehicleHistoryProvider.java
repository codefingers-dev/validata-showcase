package de.codefingers.validata.service.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Real DEKRA Provider für Production.
 *
 * Wird später implementiert mit echten DEKRA/Autodoc APIs
 * Für jetzt: Stub dass "API ist nicht verfügbar" logged
 */
@Slf4j
@Service
@Profile("aws")
public class RealVehicleHistoryProvider implements VehicleHistoryProvider {

    @Override
    public Optional<VehicleHistory> getVehicleHistory(String licensePlate, int manufacturingYear) {
        // TODO: Implementiere echte DEKRA API Integration
        // Für jetzt: return empty (fallback zu hardcoded defaults)
        log.info("RealDEKRA: API integration not yet implemented for {}", licensePlate);
        return Optional.empty();
    }

    @Override
    public boolean isRepairRealistic(String licensePlate, String repairType, int mileage) {
        // TODO: Nutze echte DEKRA Daten
        return true;
    }

    @Override
    public String getProviderName() {
        return "RealDEKRA (Production - Not Yet Implemented)";
    }
}