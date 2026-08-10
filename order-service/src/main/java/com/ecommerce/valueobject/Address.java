package com.ecommerce.valueobject;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Embeddable
public record Address(
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
        String country) {}
