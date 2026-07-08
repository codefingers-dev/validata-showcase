package de.codefingers.validata.service.analysis.rules;


import de.codefingers.validata.model.dto.InvoiceData;

public interface DuplicationDetectorService {
    DuplicationCheckResult detectDuplication(InvoiceData data);
}
