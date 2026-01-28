package com.ecommerce.repository;

import com.ecommerce.entity.User;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class UserRepository implements PanacheRepository<User> {

    public User findByEmail(String email){
        return find("email", email).firstResult();
    }
}
