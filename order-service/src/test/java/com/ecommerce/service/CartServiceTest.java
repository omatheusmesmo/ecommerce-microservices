package com.ecommerce.service;

import com.ecommerce.dto.AddCartItemRequest;
import com.ecommerce.dto.CartResponse;
import com.ecommerce.dto.CreateCartRequest;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.CartStatus;
import com.ecommerce.repository.CartRepository;
import com.ecommerce.valueobject.Money;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class CartServiceTest {

    @Inject
    CartService cartService;

    @Inject
    CartRepository cartRepository;

    private Long persistCart(CartStatus status) {
        Cart cart = new Cart("jane@example.com");
        cart.status = status;
        cartRepository.persist(cart);
        return cart.id;
    }

    private Long persistCartWithItem(String productId, int quantity, BigDecimal unitPrice) {
        Cart cart = new Cart("jane@example.com");
        cart.addItem(new CartItem(productId, "Gaming Chair", quantity, new Money(unitPrice, "BRL")));
        cart.calculateTotal();
        cartRepository.persist(cart);
        return cart.id;
    }

    @Test
    @TestTransaction
    void createCart_persistsActiveEmptyCart() {
        CartResponse response = cartService.createCart(new CreateCartRequest("jane@example.com"));

        assertNotNull(response.id());
        assertEquals(CartStatus.ACTIVE, response.status());
        assertEquals(new Money(BigDecimal.ZERO, "BRL"), response.totalAmount());
        assertTrue(response.items().isEmpty());
    }

    @Test
    @TestTransaction
    void findById_notFound_throwsNoSuchElementException() {
        assertThrows(NoSuchElementException.class, () -> cartService.findById(Long.MAX_VALUE));
    }

    @Test
    @TestTransaction
    void addItem_newProduct_appendsItemAndRecalculatesTotal() {
        Long cartId = persistCart(CartStatus.ACTIVE);

        CartResponse response = cartService.addItem(cartId,
                new AddCartItemRequest("prod-1", "Gaming Chair", 2, new Money(new BigDecimal("100.00"), "BRL")));

        assertEquals(1, response.items().size());
        assertEquals(new Money(new BigDecimal("200.00"), "BRL"), response.totalAmount());
    }

    @Test
    @TestTransaction
    void addItem_sameProductTwice_mergesQuantityInsteadOfDuplicating() {
        Long cartId = persistCart(CartStatus.ACTIVE);

        cartService.addItem(cartId, new AddCartItemRequest("prod-1", "Gaming Chair", 2, new Money(new BigDecimal("100.00"), "BRL")));
        CartResponse response = cartService.addItem(cartId,
                new AddCartItemRequest("prod-1", "Gaming Chair", 3, new Money(new BigDecimal("100.00"), "BRL")));

        assertEquals(1, response.items().size());
        assertEquals(5, response.items().get(0).quantity());
        assertEquals(new Money(new BigDecimal("500.00"), "BRL"), response.totalAmount());
    }

    @Test
    @TestTransaction
    void addItem_toAbandonedCart_throwsIllegalStateException() {
        Long cartId = persistCart(CartStatus.ABANDONED);

        assertThrows(IllegalStateException.class, () -> cartService.addItem(cartId,
                new AddCartItemRequest("prod-1", "Gaming Chair", 1, new Money(new BigDecimal("100.00"), "BRL"))));
    }

    @Test
    @TestTransaction
    void addItem_cartNotFound_throwsNoSuchElementException() {
        assertThrows(NoSuchElementException.class, () -> cartService.addItem(Long.MAX_VALUE,
                new AddCartItemRequest("prod-1", "Gaming Chair", 1, new Money(new BigDecimal("100.00"), "BRL"))));
    }

    @Test
    @TestTransaction
    void updateItemQuantity_updatesQuantityAndRecalculatesTotal() {
        Long cartId = persistCartWithItem("prod-1", 2, new BigDecimal("100.00"));
        Long itemId = cartRepository.findByIdWithItems(cartId).orElseThrow().getItems().get(0).id;

        CartResponse response = cartService.updateItemQuantity(cartId, itemId, 5);

        assertEquals(5, response.items().get(0).quantity());
        assertEquals(new Money(new BigDecimal("500.00"), "BRL"), response.totalAmount());
    }

    @Test
    @TestTransaction
    void updateItemQuantity_itemNotFound_throwsNoSuchElementException() {
        Long cartId = persistCart(CartStatus.ACTIVE);

        assertThrows(NoSuchElementException.class,
                () -> cartService.updateItemQuantity(cartId, Long.MAX_VALUE, 5));
    }

    @Test
    @TestTransaction
    void removeItem_removesItemAndRecalculatesTotal() {
        Long cartId = persistCartWithItem("prod-1", 2, new BigDecimal("100.00"));
        Long itemId = cartRepository.findByIdWithItems(cartId).orElseThrow().getItems().get(0).id;

        CartResponse response = cartService.removeItem(cartId, itemId);

        assertTrue(response.items().isEmpty());
        assertEquals(new Money(BigDecimal.ZERO, "BRL"), response.totalAmount());
    }

    @Test
    @TestTransaction
    void abandonCart_activeCart_transitionsToAbandoned() {
        Long cartId = persistCart(CartStatus.ACTIVE);

        cartService.abandonCart(cartId);

        assertEquals(CartStatus.ABANDONED, cartRepository.findById(cartId).status);
    }

    @Test
    @TestTransaction
    void abandonCart_alreadyAbandoned_throwsIllegalStateException() {
        Long cartId = persistCart(CartStatus.ABANDONED);

        assertThrows(IllegalStateException.class, () -> cartService.abandonCart(cartId));
    }

    @Test
    @TestTransaction
    void abandonCart_notFound_throwsNoSuchElementException() {
        assertThrows(NoSuchElementException.class, () -> cartService.abandonCart(Long.MAX_VALUE));
    }

    @Test
    @TestTransaction
    void findActiveByCustomerEmail_noActiveCart_throwsNoSuchElementException() {
        persistCart(CartStatus.ABANDONED);

        assertThrows(NoSuchElementException.class,
                () -> cartService.findActiveByCustomerEmail("jane@example.com"));
    }

    @Test
    @TestTransaction
    void findActiveByCustomerEmail_activeCartExists_returnsIt() {
        Long cartId = persistCart(CartStatus.ACTIVE);

        CartResponse response = cartService.findActiveByCustomerEmail("jane@example.com");

        assertEquals(cartId, response.id());
    }
}
