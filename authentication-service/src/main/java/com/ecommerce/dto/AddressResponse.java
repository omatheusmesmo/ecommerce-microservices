package com.ecommerce.dto;

import com.ecommerce.entity.Address;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

@RegisterForReflection
public record AddressResponse(
        Long id,
        String label,
        String street,
        String number,
        String complement,
        String city,
        String state,
        String zipCode,
        String country,
        boolean isDefault,
        LocalDateTime createdAt) {

    public static AddressResponse from(Address address) {
        return new AddressResponse(
                address.id,
                address.label,
                address.street,
                address.number,
                address.complement,
                address.city,
                address.state,
                address.zipCode,
                address.country,
                address.isDefault,
                address.createdAt);
    }
}
