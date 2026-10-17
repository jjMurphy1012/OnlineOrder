package com.laioffer.onlineorder;

import com.laioffer.onlineorder.entity.CartEntity;
import com.laioffer.onlineorder.entity.MenuItemEntity;
import com.laioffer.onlineorder.entity.OrderItemEntity;
import com.laioffer.onlineorder.model.CartDto;
import com.laioffer.onlineorder.model.OrderItemDto;
import com.laioffer.onlineorder.repository.CartRepository;
import com.laioffer.onlineorder.repository.MenuItemRepository;
import com.laioffer.onlineorder.repository.OrderItemRepository;
import com.laioffer.onlineorder.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

@ExtendWith(MockitoExtension.class)
class CartReadRegressionTests {
    private static final long CUSTOMER_ID = 7L;
    private static final long CART_ID = 9L;

    @Mock private CartRepository carts;
    @Mock private MenuItemRepository menuItems;
    @Mock private OrderItemRepository orderItems;

    private CartService service;

    @BeforeEach
    void setUp() {
        service = new CartService(carts, menuItems, orderItems);
        when(carts.getByCustomerId(CUSTOMER_ID)).thenReturn(new CartEntity(CART_ID, CUSTOMER_ID, 18.5));
    }

    @Test
    void unorderedBatchPreservesCartOrderAndStoredPriceQuantity() {
        MenuItemEntity firstMenu = menu(11L, "First", 12.0);
        MenuItemEntity secondMenu = menu(22L, "Second", 15.0);
        OrderItemEntity first = new OrderItemEntity(101L, 11L, CART_ID, 3.5, 2);
        OrderItemEntity second = new OrderItemEntity(102L, 22L, CART_ID, 11.5, 1);
        when(orderItems.getAllByCartId(CART_ID)).thenReturn(List.of(first, second));
        when(menuItems.findAllById(List.of(11L, 22L))).thenReturn(List.of(secondMenu, firstMenu));

        CartDto actual = service.getCart(CUSTOMER_ID);

        assertEquals(new CartDto(CART_ID, 18.5,
                List.of(new OrderItemDto(first, firstMenu), new OrderItemDto(second, secondMenu))), actual);
        verify(menuItems).findAllById(List.of(11L, 22L));
        verify(menuItems, never()).findById(anyLong());
    }

    @Test
    void repeatedMenuReferenceFetchesOnceWithoutDroppingOrderRows() {
        MenuItemEntity menu = menu(11L, "Shared menu", 8.0);
        OrderItemEntity first = new OrderItemEntity(101L, 11L, CART_ID, 3.5, 2);
        OrderItemEntity second = new OrderItemEntity(102L, 11L, CART_ID, 11.5, 1);
        when(orderItems.getAllByCartId(CART_ID)).thenReturn(List.of(first, second));
        when(menuItems.findAllById(List.of(11L))).thenReturn(List.of(menu));

        assertEquals(List.of(new OrderItemDto(first, menu), new OrderItemDto(second, menu)),
                service.getCart(CUSTOMER_ID).orderItems());
        verify(menuItems).findAllById(List.of(11L));
        verify(menuItems, never()).findById(anyLong());
    }

    @Test
    void emptyCartDoesNotIssueAMenuQuery() {
        when(orderItems.getAllByCartId(CART_ID)).thenReturn(List.of());

        assertEquals(new CartDto(CART_ID, 18.5, List.of()), service.getCart(CUSTOMER_ID));
        verifyNoInteractions(menuItems);
    }

    @Test
    void missingMenuReferenceFailsInsteadOfReturningPartialCart() {
        when(orderItems.getAllByCartId(CART_ID)).thenReturn(List.of(
                new OrderItemEntity(101L, 11L, CART_ID, 3.5, 2),
                new OrderItemEntity(102L, 22L, CART_ID, 11.5, 1)));
        when(menuItems.findAllById(List.of(11L, 22L))).thenReturn(List.of(menu(11L, "First", 12.0)));

        assertThrows(NoSuchElementException.class, () -> service.getCart(CUSTOMER_ID));
        verify(menuItems, never()).findById(anyLong());
    }

    private static MenuItemEntity menu(long id, String name, double price) {
        return new MenuItemEntity(id, 3L, name, name + " description", price, "image-" + id);
    }
}
