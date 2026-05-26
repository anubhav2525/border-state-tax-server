package com.bst.server.modules.tax.services.impl;

import com.bst.server.common.exceptions.sub.ResourceNotExistsException;
import com.bst.server.modules.tax.data.entities.TaxRateConfig;
import com.bst.server.modules.tax.data.enums.TaxModeEnum;
import com.bst.server.modules.tax.repository.TaxRateConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ApplicationTaxRateConfigService {
    private final TaxRateConfigRepository taxRateConfigRepository;

    public TaxRateConfig findByStateNameIgnoreCaseAndVehicleSeatingIgnoreCaseAndTaxType(
            String stateName,
            String vehicleSeating,
            TaxModeEnum taxType
    ) {
        return taxRateConfigRepository.
                findByStateNameIgnoreCaseAndVehicleSeatingIgnoreCaseAndTaxTypeAndDeletedFalse(
                        stateName, vehicleSeating, taxType).orElseThrow(() -> new ResourceNotExistsException(
                        "No active tax rate configured for: "
                                + stateName + " / "
                                + vehicleSeating + " / "
                                + taxType));
    }


}
