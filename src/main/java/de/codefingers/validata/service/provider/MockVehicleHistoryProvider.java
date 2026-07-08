package de.codefingers.validata.service.provider;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

/**
 * Mock DEKRA Provider für lokale Entwicklung.
 *
 * Simulates:
 * - Realistische Fahrzeug-Daten (50+ Fahrzeuge)
 * - Netzwerk-Verzögerung (100-500ms)
 * - Fehlerszenarien (5% Error Rate)
 * - Verdächtige Muster (für Fraud Detection)
 */
@Slf4j
@Service
@Profile("local")
public class MockVehicleHistoryProvider implements VehicleHistoryProvider {

    // ===== MOCK DATEN =====

    private static final Map<String, VehicleHistory> VEHICLE_DATABASE = new HashMap<>();

    static {
        // Normale Fahrzeuge
        addVehicle("M-AB 1234", "VW", "Golf", 2024, 45000, List.of("Ölwechsel", "Inspektion"), 0);
        addVehicle("M-CD 5678", "BMW", "3er", 2023, 62000, List.of("Bremsbeläge"), 0);
        addVehicle("M-EF 9012", "Mercedes", "C-Klasse", 2022, 85000, List.of("Zahnriemen"), 1);
        addVehicle("M-GH 3456", "Audi", "A4", 2020, 120000, List.of("Motor-Überholung", "Getriebe"), 2);

        // Hochrisiko Fahrzeuge (für Fraud Detection)
        addHighRiskVehicle("M-XX 9999", "VW", "Golf", 2015, 300000, 5);  // Sehr hohes Mileage
        addHighRiskVehicle("M-YY 8888", "Opel", "Corsa", 2010, 450000, 3);  // Sehr alter
        addHighRiskVehicle("M-ZZ 7777", "Renault", "Megane", 2020, 15000, 4);  // 4 Unfälle in 4 Jahren!

        // ... weitere 40+ Fahrzeuge
        generateRandomVehicles(45);
    }

    private static void addVehicle(String plate, String brand, String model, int year,
                                   int mileage, List<String> repairs, int accidents) {
        VehicleHistory vh = new VehicleHistory(plate, brand, model, year, mileage);
        vh.repairHistory = new ArrayList<>(repairs);
        vh.accidentCount = accidents;
        vh.lastInspection = LocalDate.now().minusDays(new Random().nextInt(365));
        VEHICLE_DATABASE.put(plate, vh);
    }

    private static void addHighRiskVehicle(String plate, String brand, String model,
                                           int year, int mileage, int accidents) {
        VehicleHistory vh = new VehicleHistory(plate, brand, model, year, mileage);
        vh.accidentCount = accidents;
        vh.isHighRisk = true;
        vh.repairHistory = List.of("Motor", "Getriebe", "Karosserie", "Unfallschaden");
        VEHICLE_DATABASE.put(plate, vh);
    }

    private static void generateRandomVehicles(int count) {
        String[] brands = {"VW", "BMW", "Mercedes", "Audi", "Opel", "Ford", "Renault", "Peugeot"};
        String[] models = {"Golf", "Passat", "Polo", "3er", "5er", "C-Klasse", "E-Klasse", "A4", "A6"};
        Random rand = new Random();

        for (int i = 0; i < count; i++) {
            String plate = String.format("M-%02d %04d", i / 100, 1000 + (i % 1000));
            String brand = brands[rand.nextInt(brands.length)];
            String model = models[rand.nextInt(models.length)];
            int year = 2010 + rand.nextInt(15);
            int mileage = 10000 + rand.nextInt(300000);
            int accidents = rand.nextInt(4);

            addVehicle(plate, brand, model, year, mileage, List.of("Standard"), accidents);
        }
    }

    // ===== IMPLEMENTATION =====

    @Override
    public Optional<VehicleHistory> getVehicleHistory(String licensePlate, int manufacturingYear) {
        try {
            // Simuliere Netzwerk-Verzögerung (100-500ms)
            long delay = 100 + new Random().nextInt(400);
            Thread.sleep(delay);

            // Simuliere 5% Error Rate
            if (new Random().nextDouble() < 0.05) {
                log.warn("MockDEKRA: Simulated error for {}", licensePlate);
                return Optional.empty();  // Fahrzeug nicht gefunden (realistisch!)
            }

            log.info("MockDEKRA: Fetching history for {} ({}ms delay)", licensePlate, delay);

            VehicleHistory vh = VEHICLE_DATABASE.get(licensePlate);
            if (vh != null) {
                log.debug("MockDEKRA: Found vehicle {} - {} (mileage: {}, accidents: {})",
                        licensePlate, vh.brand + " " + vh.model, vh.currentMileage, vh.accidentCount);
                return Optional.of(vh);
            }

            // Fahrzeug nicht in Mock-DB → generiere zufälliges
            return Optional.of(generateRandomVehicle(licensePlate, manufacturingYear));

        } catch (InterruptedException e) {
            log.error("MockDEKRA: Interrupted while fetching {}", licensePlate, e);
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    @Override
    public boolean isRepairRealistic(String licensePlate, String repairType, int mileage) {
        // ✅ RICHTIG: Nutze aktuelles Jahr statt year=0!
        Optional<VehicleHistory> vh = getVehicleHistory(licensePlate, LocalDate.now().getYear());

        if (vh.isEmpty()) {
            return true;  // Wenn nicht gefunden, assume ok
        }

        VehicleHistory vehicle = vh.get();
        String lower = repairType.toLowerCase();

        // ===== LOGIK FÜR VERDÄCHTIGE PATTERNS =====

        // 1. Motor bei sehr jungem Fahrzeug (< 5 Jahre, < 100k km)
        if ((lower.contains("motor") || lower.contains("engine")) &&
                mileage < 100000 &&
                (LocalDate.now().getYear() - vehicle.manufacturingYear) < 5) {
            log.warn("SUSPICIOUS: Motor repair on young vehicle {} ({}km)",
                    licensePlate, mileage);
            return false;
        }

        // 2. Zu viele Unfallschäden
        if (lower.contains("unfall") && vehicle.accidentCount > 3) {
            log.warn("SUSPICIOUS: Multiple accidents on {} (count: {})",
                    licensePlate, vehicle.accidentCount);
            return false;
        }

        // 3. Mileage viel höher als erwartet
        int vehicleAge = LocalDate.now().getYear() - vehicle.manufacturingYear;
        int expectedMileage = vehicleAge * 15000;
        if (expectedMileage <= 0) expectedMileage = 15000;

        if (mileage > expectedMileage * 2) {
            log.warn("SUSPICIOUS: High mileage on {} ({} vs expected {})",
                    licensePlate, mileage, expectedMileage);
            return false;
        }

        return true;
    }

    @Override
    public String getProviderName() {
        return "MockDEKRA (Local Development)";
    }

    // ===== HELPER METHODS =====

    private VehicleHistory generateRandomVehicle(String licensePlate, int year) {
        String[] brands = {"VW", "BMW", "Mercedes", "Audi"};
        String[] models = {"Golf", "3er", "C-Klasse", "A4"};
        Random rand = new Random();

        VehicleHistory vh = new VehicleHistory(
                licensePlate,
                brands[rand.nextInt(brands.length)],
                models[rand.nextInt(models.length)],
                year,
                50000 + rand.nextInt(200000)
        );

        vh.accidentCount = rand.nextInt(3);
        vh.lastInspection = LocalDate.now().minusDays(rand.nextInt(365));
        return vh;
    }
}