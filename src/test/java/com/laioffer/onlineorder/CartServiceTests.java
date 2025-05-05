package com.laioffer.onlineorder;


import com.laioffer.onlineorder.entity.CartEntity;
import com.laioffer.onlineorder.entity.MenuItemEntity;
import com.laioffer.onlineorder.entity.OrderItemEntity;
import com.laioffer.onlineorder.model.CartDto;
import com.laioffer.onlineorder.repository.CartRepository;
import com.laioffer.onlineorder.repository.MenuItemRepository;
import com.laioffer.onlineorder.repository.OrderItemRepository;
import com.laioffer.onlineorder.service.CartService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.List;
import java.util.Optional;
import java.util.Set;


@ExtendWith(MockitoExtension.class)
public class CartServiceTests {


    @Mock
    private CartRepository cartRepository;


    @Mock
    private MenuItemRepository menuItemRepository;


    @Mock
    private OrderItemRepository orderItemRepository;


    private CartService cartService;


    @BeforeEach
    void setup() {
        cartService = new CartService(cartRepository, menuItemRepository, orderItemRepository);
    }


    @Test
    void addMenuItemToCart_whenOrderNotExist_shouldCreateOneOrderItem() {
        // Mock data
        long customerId = 1L;
        long menuItemId = 2L;
        long cartId = 3L;
        CartEntity cartEntity = new CartEntity(cartId, customerId, 0.0);
        MenuItemEntity menuItem = new MenuItemEntity(menuItemId, 1L, "Name", "", 10.0, "");


        // Mock repository method calls
        Mockito.when(cartRepository.getByCustomerId(customerId)).thenReturn(cartEntity);
        Mockito.when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));
        Mockito.when(orderItemRepository.findByCartIdAndMenuItemId(cartId, menuItemId)).thenReturn(null);


        // Perform the method under test
        cartService.addMenuItemToCart(customerId, menuItemId);


        // Verify the repository method calls
        OrderItemEntity newOrderItem = new OrderItemEntity(null, menuItemId, cartId, 10.0, 1);
        Mockito.verify(orderItemRepository).save(newOrderItem);
        Mockito.verify(cartRepository).updateTotalPrice(cartId, 10.0);
    }


    @Test
    void addMenuItemToCart_whenOrderAlreadyExist_shouldUpdateOrderItem() {
        // Mock data
        long customerId = 1L;
        long menuItemId = 2L;
        long cartId = 3L;
        long orderItemId = 4L;
        CartEntity cartEntity = new CartEntity(cartId, customerId, 10.0);
        MenuItemEntity menuItem = new MenuItemEntity(menuItemId, 1L, "Name", "", 10.0, "");
        OrderItemEntity orderItemEntity = new OrderItemEntity(orderItemId, menuItemId, customerId, 10.0, 1);


        // Mock repository method calls
        Mockito.when(cartRepository.getByCustomerId(customerId)).thenReturn(cartEntity);
        Mockito.when(menuItemRepository.findById(menuItemId)).thenReturn(Optional.of(menuItem));
        Mockito.when(orderItemRepository.findByCartIdAndMenuItemId(cartId, menuItemId)).thenReturn(orderItemEntity);


        // Perform the method under test
        cartService.addMenuItemToCart(customerId, menuItemId);


        // Verify the repository method calls
        OrderItemEntity newOrderItem = new OrderItemEntity(orderItemId, menuItemId, cartId, 10.0, 2);
        Mockito.verify(orderItemRepository).save(newOrderItem);
        Mockito.verify(cartRepository).updateTotalPrice(cartId, 20.0);
    }


    @Test
    void getCart_shouldReturnCartDto() {
        // Mock data
        long customerId = 1L;
        long cartId = 3L;
        CartEntity cartEntity = new CartEntity(cartId, customerId, 21.0);
        List<OrderItemEntity> orderItems = List.of(
                new OrderItemEntity(1L, 1L, cartId, 10.0, 1),
                new OrderItemEntity(2L, 2L, cartId, 10.0, 2)
        );
        List<MenuItemEntity> menuItems = List.of(
                new MenuItemEntity(1L, 1L, "Name1", "", 1.0, ""),
                new MenuItemEntity(2L, 1L, "Name2", "", 10.0, "")
        );


        // Mock repository method calls
        Mockito.when(cartRepository.getByCustomerId(customerId)).thenReturn(cartEntity);
        Mockito.when(orderItemRepository.getAllByCartId(cartEntity.id())).thenReturn(orderItems);
        Mockito.when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItems.get(0)));
        Mockito.when(menuItemRepository.findById(2L)).thenReturn(Optional.of(menuItems.get(1)));


        // Perform the method under test
        CartDto cartDto = cartService.getCart(customerId);


        // Verify the expected CartDto properties
        Assertions.assertEquals(cartId, cartDto.id());
        Assertions.assertEquals(cartEntity.totalPrice(), cartDto.totalPrice());
        Assertions.assertEquals(orderItems.size(), cartDto.orderItems().size());
    }


    @Test
    void clearCart_shouldRemoveAllItemsAndResetTotalPrice() {
        // Mock data
        long customerId = 1L;
        long cartId = 2L;
        CartEntity cartEntity = new CartEntity(cartId, customerId, 21.0);


        // Mock repository method calls
        Mockito.when(cartRepository.getByCustomerId(customerId)).thenReturn(cartEntity);


        // Perform the method under test
        cartService.clearCart(customerId);


        // Verify the repository method calls
        Mockito.verify(orderItemRepository).deleteByCartId(cartId);
        Mockito.verify(cartRepository).updateTotalPrice(cartId, 0.0);
    }
}
