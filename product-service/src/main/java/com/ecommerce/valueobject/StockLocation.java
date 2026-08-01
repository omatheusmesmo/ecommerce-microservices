package com.ecommerce.valueobject;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockLocation(
        @NotBlank(message = "Location ID is required") String locationId,

        @NotNull(message = "Quantity on hand is required")
        @Min(value = 0, message = "Quantity on hand must be greater than or equal to zero")
        Integer quantityOnHand,

        @NotNull(message = "Reserved quantity is required")
        @Min(value = 0, message = "Reserved quantity must be greater than or equal to zero")
        Integer quantityReserved) {

    public static final String DEFAULT_LOCATION_ID = "DEFAULT";

    public int available() {
        return quantityOnHand - quantityReserved;
    }

    @AssertTrue(message = "Reserved quantity cannot exceed quantity on hand")
    public boolean isReservedWithinOnHand() {
        return quantityReserved <= quantityOnHand;
    }
}
