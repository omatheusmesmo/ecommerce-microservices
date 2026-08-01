package com.ecommerce.service;

import com.ecommerce.dto.AddCartItemRequest;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.CreateCartRequest;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.CartStatus;
import com.ecommerce.repository.CartRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NoSuchElementException;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CartService {

    private static final Logger LOG = Logger.getLogger(CartService.class);

    @Inject
    CartRepository cartRepository;

    @Transactional
    public CartResponse createCart(CreateCartRequest request) {
        LOG.infof("Creating cart for customer: %s", request.customerEmail());

        Cart cart = new Cart(request.customerEmail());
        cartRepository.persist(cart);

        return CartResponse.from(cart);
    }

    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public CartResponse findById(Long id) {
        LOG.debugf("Finding cart by ID: %d", id);

        return cartRepository
                .findByIdWithItems(id)
                .map(CartResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Cart not found with id: " + id));
    }

    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public List<CartResponse> findAll() {
        LOG.debug("Listing all carts");

        return cartRepository.listAll().stream().map(CartResponse::from).toList();
    }

    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    public CartResponse findActiveByCustomerEmail(String email) {
        LOG.debugf("Finding active cart for customer: %s", email);

        return cartRepository
                .findActiveByCustomerEmail(email)
                .map(CartResponse::from)
                .orElseThrow(() -> new NoSuchElementException("No active cart found for customer: " + email));
    }

    @Transactional
    public CartResponse addItem(Long cartId, AddCartItemRequest request) {
        Cart cart = cartRepository
                .findByIdWithItems(cartId)
                .orElseThrow(() -> new NoSuchElementException("Cart not found with id: " + cartId));

        requireActive(cart);

        CartItem existing = cart.getItems().stream()
                .filter(item -> item.productId.equals(request.productId()))
                .findFirst()
                .orElse(null);

        if (existing != null) {
            existing.quantity += request.quantity();
        } else {
            cart.addItem(
                    new CartItem(request.productId(), request.productName(), request.quantity(), request.unitPrice()));
        }

        cart.calculateTotal();
        cartRepository.persist(cart);

        LOG.infof("Cart %d - item %s added, total: %s", cart.id, request.productId(), cart.totalAmount);

        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long cartId, Long itemId, Integer quantity) {
        Cart cart = cartRepository
                .findByIdWithItems(cartId)
                .orElseThrow(() -> new NoSuchElementException("Cart not found with id: " + cartId));

        requireActive(cart);

        CartItem item = findItem(cart, itemId);
        item.quantity = quantity;

        cart.calculateTotal();
        cartRepository.persist(cart);

        return CartResponse.from(cart);
    }

    @Transactional
    public CartResponse removeItem(Long cartId, Long itemId) {
        Cart cart = cartRepository
                .findByIdWithItems(cartId)
                .orElseThrow(() -> new NoSuchElementException("Cart not found with id: " + cartId));

        requireActive(cart);

        CartItem item = findItem(cart, itemId);
        cart.removeItem(item);

        cart.calculateTotal();
        cartRepository.persist(cart);

        return CartResponse.from(cart);
    }

    @Transactional
    public void abandonCart(Long cartId) {
        LOG.infof("Abandoning cart: %d", cartId);

        Cart cart = cartRepository
                .findByIdOptional(cartId)
                .orElseThrow(() -> new NoSuchElementException("Cart not found with id: " + cartId));

        if (!cart.status.canTransitionTo(CartStatus.ABANDONED)) {
            throw new IllegalStateException(
                    "Cannot transition cart from " + cart.status + " to " + CartStatus.ABANDONED);
        }

        cart.status = CartStatus.ABANDONED;
        cartRepository.persist(cart);
    }

    private CartItem findItem(Cart cart, Long itemId) {
        return cart.getItems().stream()
                .filter(item -> item.id.equals(itemId))
                .findFirst()
                .orElseThrow(
                        () -> new NoSuchElementException("Item not found with id: " + itemId + " in cart: " + cart.id));
    }

    private void requireActive(Cart cart) {
        if (cart.status != CartStatus.ACTIVE) {
            throw new IllegalStateException("Cart " + cart.id + " is not active (status: " + cart.status + ")");
        }
    }
}
