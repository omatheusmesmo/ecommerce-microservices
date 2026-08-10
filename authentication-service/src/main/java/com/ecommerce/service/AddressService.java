package com.ecommerce.service;

import com.ecommerce.dto.AddressRequest;
import com.ecommerce.dto.AddressResponse;
import com.ecommerce.entity.Address;
import com.ecommerce.repository.AddressRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.NoSuchElementException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class AddressService {

    private static final Logger LOG = Logger.getLogger(AddressService.class);

    @Inject
    AddressRepository addressRepository;

    public List<AddressResponse> listForUser(Long userId) {
        LOG.debugf("Listing addresses for user: %d", userId);
        return addressRepository.findByUserId(userId).stream()
                .map(AddressResponse::from)
                .toList();
    }

    public AddressResponse getById(Long userId, Long addressId) {
        return AddressResponse.from(findOwned(userId, addressId));
    }

    @Transactional
    public AddressResponse create(Long userId, AddressRequest request) {
        Address address = request.toAddress(userId);

        boolean firstAddress = addressRepository.findByUserId(userId).isEmpty();
        if (request.isDefault() || firstAddress) {
            addressRepository.clearDefaultForUser(userId);
            address.isDefault = true;
        }

        addressRepository.persist(address);
        LOG.infof("Address %d created for user %d", address.id, userId);
        return AddressResponse.from(address);
    }

    @Transactional
    public AddressResponse update(Long userId, Long addressId, AddressRequest request) {
        Address address = findOwned(userId, addressId);

        request.applyTo(address);
        if (request.isDefault() && !address.isDefault) {
            addressRepository.clearDefaultForUser(userId);
        }
        address.isDefault = request.isDefault();

        addressRepository.persist(address);
        LOG.infof("Address %d updated for user %d", address.id, userId);
        return AddressResponse.from(address);
    }

    @Transactional
    public void delete(Long userId, Long addressId) {
        Address address = findOwned(userId, addressId);
        addressRepository.delete(address);
        LOG.infof("Address %d deleted for user %d", addressId, userId);
    }

    private Address findOwned(Long userId, Long addressId) {
        return addressRepository
                .findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new NoSuchElementException("Address not found with id: " + addressId));
    }
}
