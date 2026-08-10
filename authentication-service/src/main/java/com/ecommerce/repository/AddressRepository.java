package com.ecommerce.repository;

import com.ecommerce.entity.Address;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class AddressRepository implements PanacheRepository<Address> {

    public List<Address> findByUserId(Long userId) {
        return list("userId", userId);
    }

    public Optional<Address> findByIdAndUserId(Long id, Long userId) {
        return find("id = ?1 and userId = ?2", id, userId).firstResultOptional();
    }

    public void clearDefaultForUser(Long userId) {
        update("isDefault = false where userId = ?1 and isDefault = true", userId);
    }
}
