package de.codefingers.validata.service.provider;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Provider für Fahrzeug-Historien-Daten.
 *
 * Abstrahiert externe DEKRA/Autodoc APIs
 * Erlaubt Mock für lokale Entwicklung, Real APIs für Production
 */
public interface VehicleHistoryProvider {

    /**
     * Ruft Fahrzeug-Historie ab
     *
     * @param vehicleRegistration Kennzeichen (z.B. "M-AB 1234")
     * @param manufacturingYear Baujahr
     * @return Fahrzeug-Historie oder empty wenn nicht gefunden
     */
    Optional<VehicleHistory> getVehicleHistory(String vehicleRegistration, int manufacturingYear);

    /**
     * Wird eine Reparatur für dieses Fahrzeug bei dieser Werkstatt erwartet?
     * (Für Anomaly Detection)
     */
    boolean isRepairRealistic(String licensePlate, String repairType, int mileage);

    /**
     * Gibt den Namen des Providers zurück (für Logging)
     */
    String getProviderName();

    // ===== DATEN KLASSEN =====

    class VehicleHistory {
        public String vehicleRegistration;
        public String brand;           // VW, BMW, Mercedes, etc
        public String model;            // Golf, 3er, C-Klasse, etc
        public int manufacturingYear;
        public int currentMileage;
        public LocalDate lastInspection;
        public List<String> repairHistory;  // Letzte 10 Reparaturen
        public int accidentCount;
        public boolean isHighRisk;      // Für Versicherung bekannt

        public VehicleHistory(String vehicleRegistration, String brand, String model,
                              int manufacturingYear, int currentMileage) {
            this.vehicleRegistration = vehicleRegistration;
            this.brand = brand;
            this.model = model;
            this.manufacturingYear = manufacturingYear;
            this.currentMileage = currentMileage;
            this.repairHistory = List.of();
            this.accidentCount = 0;
            this.isHighRisk = false;
        }
    }
}