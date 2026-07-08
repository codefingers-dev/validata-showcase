package de.codefingers.validata.service.provider;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit Tests für MockVehicleHistoryProvider
 *
 * KEIN @SpringBootTest nötig!
 * Das ist reine Business-Logik, kein Spring nötig.
 */
@Slf4j
class MockVehicleHistoryProviderTest {

    private MockVehicleHistoryProvider provider;

    @BeforeEach
    void setup() {
        // ✅ EINFACH instantiieren, kein Spring Context nötig!
        provider = new MockVehicleHistoryProvider();
    }

    @Test
    void testGetVehicleHistory_Success() {
        // Arrange
        String licensePlate = "M-ZZ 7777";
        int expectedYear = 2024;  // ← CONSISTENT!

        // Act
        Optional<VehicleHistoryProvider.VehicleHistory> result =
                provider.getVehicleHistory(licensePlate, expectedYear);  // ← FIX: 2020 → 2024

        // Assert
        assertTrue(result.isPresent(), "Vehicle should be found");
        VehicleHistoryProvider.VehicleHistory vehicle = result.get();
        assertNotNull(vehicle.brand, "Brand should not be null");
        assertNotNull(vehicle.model, "Model should not be null");
        log.info("✅ Found vehicle: {} {} (year {}) with {} km",
                vehicle.brand, vehicle.model, vehicle.manufacturingYear, vehicle.currentMileage);
    }

    @Test
    void testGetVehicleHistory_NotFound() {
        // Arrange: Non-existent license plate
        String licensePlate = "XX-FAKE 9999";

        // Act
        Optional<VehicleHistoryProvider.VehicleHistory> result =
                provider.getVehicleHistory(licensePlate, 2020);

        // Assert: Should generate random vehicle if not found
        // (MockVehicleHistoryProvider generates random data)
        assertTrue(result.isPresent(), "Should generate random vehicle");
        assertNotNull(result.get().brand, "Generated vehicle should have brand");
        log.info("✅ Generated random vehicle for unknown plate: {}",
                result.get().brand + " " + result.get().model);
    }

    @Test
    void testIsRepairRealistic_Motor_YoungVehicle() {
        // Arrange: Motor repair auf 2-year-old Fahrzeug mit nur 50k km
        String licensePlate = "M-AB 1234";
        int year = 2024;  // ← 2 Jahre alt (2026 - 2024)
        int mileage = 50000;

        // Act
        boolean result = provider.isRepairRealistic(licensePlate, "Motor", mileage);

        // Assert
        assertFalse(result, "Motor repair should be UNREALISTIC on young vehicle");

        log.info("✅ Correctly detected unrealistic motor repair on {} year old vehicle with {} km",
                (2026 - year), mileage);
    }

    @Test
    void testIsRepairRealistic_OilChange_Normal() {
        // Arrange: Ölwechsel = immer realistic
        String licensePlate = "M-AB 1234";
        int mileage = 45000;

        // Act
        boolean result = provider.isRepairRealistic(licensePlate, "Ölwechsel", mileage);

        // Assert
        assertTrue(result, "Oil change should be realistic for any vehicle");
        log.info("✅ Correctly approved oil change at {} km", mileage);
    }

    @Test
    void testIsRepairRealistic_HighMileage() {
        // Arrange: Repair auf auto mit unrealistisch hohem Mileage (5x normal)
        String licensePlate = "M-AB 1234";
        int unrealisticMileage = 500000;  // 2 year old car should have ~30k km max
        String repairType = "Getriebe";

        // Act
        boolean result = provider.isRepairRealistic(licensePlate, repairType, unrealisticMileage);

        // Assert
        assertFalse(result, "Unrealistic high mileage should be caught");
        log.info("✅ Correctly detected high mileage anomaly: {} km on young vehicle", unrealisticMileage);
    }

    @Test
    void testHighRiskVehicleDetection() {
        // Arrange: High-risk Fahrzeug mit 4 Unfällen
        String licensePlate = "M-ZZ 7777";  // Known high-risk in Mock DB
        int year = 2020;

        // Act
        Optional<VehicleHistoryProvider.VehicleHistory> result =
                provider.getVehicleHistory(licensePlate, year);

        // Assert
        assertTrue(result.isPresent(), "High-risk vehicle should be found");
        VehicleHistoryProvider.VehicleHistory vehicle = result.get();
        assertEquals(4, vehicle.accidentCount, "Should have 4 accidents");
        assertTrue(vehicle.isHighRisk, "Should be marked as high-risk");
        log.info("✅ Correctly identified high-risk vehicle: {} {} with {} accidents",
                vehicle.brand, vehicle.model, vehicle.accidentCount);
    }

    @Test
    void testGetProviderName() {
        // Act
        String name = provider.getProviderName();

        // Assert
        assertEquals("MockDEKRA (Local Development)", name);
        assertTrue(name.contains("Mock"), "Should indicate this is a mock provider");
        log.info("✅ Provider name correct: {}", name);
    }

    @Test
    void testPerformance_ShouldBeFast() {
        // Arrange
        String licensePlate = "M-AB 1234";
        int year = 2024;
        int maxAllowedDuration = 1000;  // 1 second max (includes 100-500ms mock delay)

        // Act
        long start = System.currentTimeMillis();
        Optional<VehicleHistoryProvider.VehicleHistory> result =
                provider.getVehicleHistory(licensePlate, year);  // ← FIX: 2020 → 2024
        long duration = System.currentTimeMillis() - start;

        // Assert
        assertTrue(result.isPresent(), "Vehicle should be found");
        assertTrue(duration < maxAllowedDuration,
                String.format("Should complete within %dms (was: %dms, includes mock delay)",
                        maxAllowedDuration, duration));
        log.info("✅ Performance test passed: {}ms (acceptable with mock delay)", duration);
    }

    // ===== NEW: Additional Test für bessere Coverage =====

    @Test
    void testMultipleVehicleQueries() {
        // Arrange: Multiple vehicles from Mock DB
        String[] licensePlates = {
                "M-ZZ 7777",      // ✅ Known to exist (high-risk)
                "XX-UNKNOWN-1",   // Will be randomly generated
                "XX-UNKNOWN-2"    // Will be randomly generated
        };

        // Act & Assert
        for (String plate : licensePlates) {
            Optional<VehicleHistoryProvider.VehicleHistory> result =
                    provider.getVehicleHistory(plate, 2024);
            assertTrue(result.isPresent(),
                    "Vehicle " + plate + " should be found in Mock DB");
        }

        log.info("✅ All vehicles found or generated successfully");
    }


}