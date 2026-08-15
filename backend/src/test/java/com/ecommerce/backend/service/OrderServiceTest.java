package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.OrderResponse;
import com.ecommerce.backend.dto.PlaceOrderRequest;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Order placement is where money and stock actually change — these tests
 * cover the paths that matter: an empty cart can't become an order, stock
 * is enforced per line, and a successful order decrements stock and clears
 * the cart atomically (from the caller's point of view).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String EMAIL = "rahul@example.com";

    @Mock private OrderRepository orderRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;
    @Mock private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Rahul").email(EMAIL).role(Role.CUSTOMER).build();
    }

    @Test
    void placeOrder_throwsBadRequest_whenCartIsEmpty() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Cart emptyCart = Cart.builder().id(1L).user(user).items(new ArrayList<>()).build();
        when(cartService.getOrCreateCart(EMAIL)).thenReturn(emptyCart);

        assertThatThrownBy(() -> orderService.placeOrder(EMAIL, new PlaceOrderRequest("221B Baker Street")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty cart");

        verify(orderRepository, never()).save(any());
    }

    @Test
    void placeOrder_throwsBadRequest_whenRequestedQuantityExceedsStock() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Product product = Product.builder().id(5L).name("Wireless Mouse").price(new BigDecimal("799.00")).stockQuantity(2).build();
        CartItem cartItem = CartItem.builder().id(1L).product(product).quantity(5).build();
        Cart cart = Cart.builder().id(1L).user(user).items(new ArrayList<>(List.of(cartItem))).build();
        when(cartService.getOrCreateCart(EMAIL)).thenReturn(cart);

        assertThatThrownBy(() -> orderService.placeOrder(EMAIL, new PlaceOrderRequest("221B Baker Street")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only 2 unit(s)")
                .hasMessageContaining("Wireless Mouse");

        verify(orderRepository, never()).save(any());
        verify(cartService, never()).clearCart(any());
    }

    @Test
    void placeOrder_decrementsStockAndClearsCart_onSuccess() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        Product product = Product.builder().id(5L).name("Wireless Mouse").price(new BigDecimal("799.00")).stockQuantity(10).build();
        CartItem cartItem = CartItem.builder().id(1L).product(product).quantity(3).build();
        Cart cart = Cart.builder().id(1L).user(user).items(new ArrayList<>(List.of(cartItem))).build();
        when(cartService.getOrCreateCart(EMAIL)).thenReturn(cart);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(100L);
            return order;
        });

        OrderResponse response = orderService.placeOrder(EMAIL, new PlaceOrderRequest("221B Baker Street"));

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        assertThat(productCaptor.getValue().getStockQuantity()).isEqualTo(7); // 10 - 3

        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("2397.00")); // 799.00 * 3
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.items()).hasSize(1);

        verify(cartService).clearCart(EMAIL);
    }

    @Test
    void placeOrder_throwsResourceNotFound_whenUserDoesNotExist() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.placeOrder("ghost@example.com", new PlaceOrderRequest("nowhere")))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_throwsResourceNotFound_whenOrderMissing() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateStatus(999L, OrderStatus.SHIPPED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOrderById_throwsAccessDenied_whenCallerIsNeitherOwnerNorAdmin() {
        Order order = Order.builder()
                .id(1L).user(user).items(new ArrayList<>())
                .totalAmount(BigDecimal.TEN).status(OrderStatus.PENDING).shippingAddress("addr")
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById("someone-else@example.com", 1L, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getOrderById_allowsAccess_whenCallerIsAdminEvenIfNotOwner() {
        Order order = Order.builder()
                .id(1L).user(user).items(new ArrayList<>())
                .totalAmount(BigDecimal.TEN).status(OrderStatus.PENDING).shippingAddress("addr")
                .build();
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById("admin@example.com", 1L, true);

        assertThat(response.id()).isEqualTo(1L);
    }
}
