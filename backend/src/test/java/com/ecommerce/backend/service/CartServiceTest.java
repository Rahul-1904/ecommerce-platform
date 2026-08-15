package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.CartItemRequest;
import com.ecommerce.backend.dto.CartResponse;
import com.ecommerce.backend.entity.Cart;
import com.ecommerce.backend.entity.CartItem;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.entity.Role;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.CartItemRepository;
import com.ecommerce.backend.repository.CartRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    private static final String EMAIL = "rahul@example.com";

    @Mock private CartRepository cartRepository;
    @Mock private CartItemRepository cartItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private CartService cartService;

    private Cart cart;

    @BeforeEach
    void setUp() {
        User user = User.builder().id(1L).email(EMAIL).role(Role.CUSTOMER).build();
        cart = Cart.builder().id(1L).user(user).items(new ArrayList<>()).build();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
    }

    @Test
    void addItem_throwsBadRequest_whenRequestedQuantityExceedsStock() {
        Product product = Product.builder().id(5L).name("Wireless Mouse").price(new BigDecimal("799.00")).stockQuantity(3).build();
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(1L, 5L)).thenReturn(Optional.empty());

        CartItemRequest request = new CartItemRequest(5L, 5); // wants 5, only 3 in stock

        assertThatThrownBy(() -> cartService.addItem(EMAIL, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only 3 unit(s)");

        verify(cartItemRepository, never()).save(any());
    }

    @Test
    void addItem_addsANewLine_whenProductNotAlreadyInCart() {
        Product product = Product.builder().id(5L).name("Wireless Mouse").price(new BigDecimal("799.00")).stockQuantity(20).build();
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(1L, 5L)).thenReturn(Optional.empty());

        CartResponse response = cartService.addItem(EMAIL, new CartItemRequest(5L, 2));

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).quantity()).isEqualTo(2);
        assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("1598.00"));
        verify(cartItemRepository).save(any(CartItem.class));
    }

    @Test
    void addItem_incrementsTheExistingLine_ratherThanDuplicatingIt_whenProductAlreadyInCart() {
        Product product = Product.builder().id(5L).name("Wireless Mouse").price(new BigDecimal("799.00")).stockQuantity(20).build();
        CartItem existing = CartItem.builder().id(10L).cart(cart).product(product).quantity(2).build();
        cart.getItems().add(existing);
        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByCartIdAndProductId(1L, 5L)).thenReturn(Optional.of(existing));

        cartService.addItem(EMAIL, new CartItemRequest(5L, 3)); // 2 existing + 3 more, within stock

        assertThat(existing.getQuantity()).isEqualTo(5);
        assertThat(cart.getItems()).hasSize(1); // still one line, not a duplicate
        verify(cartItemRepository).save(existing);
    }

    @Test
    void addItem_throwsResourceNotFound_whenProductDoesNotExist() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addItem(EMAIL, new CartItemRequest(999L, 1)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
