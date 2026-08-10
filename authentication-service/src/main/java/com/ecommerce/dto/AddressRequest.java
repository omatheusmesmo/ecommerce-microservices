package com.ecommerce.dto;

import com.ecommerce.entity.Address;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RegisterForReflection
public record AddressRequest(
        @Size(max = 50, message = "Label must be at most 50 characters")
        String label,

        @NotBlank(message = "Street is required") @Size(max = 255, message = "Street must be at most 255 characters")
        String street,

        @NotBlank(message = "Number is required") @Size(max = 20, message = "Number must be at most 20 characters")
        String number,

        @Size(max = 255, message = "Complement must be at most 255 characters")
        String complement,

        @NotBlank(message = "City is required") @Size(max = 100, message = "City must be at most 100 characters")
        String city,

        @NotBlank(message = "State is required") @Size(max = 100, message = "State must be at most 100 characters")
        String state,

        @NotBlank(message = "Zip code is required") @Size(max = 20, message = "Zip code must be at most 20 characters")
        String zipCode,

        @NotBlank(message = "Country is required") @Size(max = 100, message = "Country must be at most 100 characters")
        String country,

        boolean isDefault) {

    public Address toAddress(Long userId) {
        Address address = new Address();
        address.userId = userId;
        applyTo(address);
        return address;
    }

    public void applyTo(Address address) {
        address.label = label;
        address.street = street;
        address.number = number;
        address.complement = complement;
        address.city = city;
        address.state = state;
        address.zipCode = zipCode;
        address.country = country;
    }
}
