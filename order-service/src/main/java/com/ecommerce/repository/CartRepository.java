package com.ecommerce.repository;

import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class CartRepository implements PanacheRepository<Cart> {

    public Optional<Cart> findByIdWithItems(Long id) {
        return find(
                        "SELECT c FROM Cart c " + "LEFT JOIN FETCH c.items " + "WHERE c.id = :id",
                        Parameters.with("id", id))
                .firstResultOptional();
    }

    public Optional<Cart> findActiveByCustomerEmail(String email) {
        return find(
                        "SELECT c FROM Cart c " + "LEFT JOIN FETCH c.items "
                                + "WHERE c.customerEmail = :email AND c.status = :status",
                        Parameters.with("email", email).and("status", CartStatus.ACTIVE))
                .firstResultOptional();
    }

    public List<Cart> findByStatus(CartStatus status) {
        return list("status", status);
    }
}
