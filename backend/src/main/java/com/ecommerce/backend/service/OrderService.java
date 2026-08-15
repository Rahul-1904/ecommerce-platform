package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.OrderItemResponse;
import com.ecommerce.backend.dto.OrderResponse;
import com.ecommerce.backend.dto.PlaceOrderRequest;
import com.ecommerce.backend.entity.*;
import com.ecommerce.backend.exception.BadRequestException;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.OrderRepository;
import com.ecommerce.backend.repository.ProductRepository;
import com.ecommerce.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartService cartService;

    @Transactional
    public OrderResponse placeOrder(String email, PlaceOrderRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        Cart cart = cartService.getOrCreateCart(email);
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot place an order with an empty cart");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            if (cartItem.getQuantity() > product.getStockQuantity()) {
                throw new BadRequestException("Only " + product.getStockQuantity() + " unit(s) of '" + product.getName() + "' available");
            }

            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);

            BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            total = total.add(subtotal);

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(product.getPrice())
                    .build());
        }

        Order order = Order.builder()
                .user(user)
                .totalAmount(total)
                .status(OrderStatus.PENDING)
                .shippingAddress(request.shippingAddress())
                .build();
        orderItems.forEach(item -> item.setOrder(order));
        order.setItems(orderItems);

        Order saved = orderRepository.save(order);
        cartService.clearCart(email);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersForUser(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return orderRepository.findByUserId(user.getId(), pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String email, Long orderId, boolean isAdmin) {
        Order order = findEntity(orderId);
        if (!isAdmin && !order.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException("You do not have access to this order");
        }
        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatus status) {
        Order order = findEntity(orderId);
        order.setStatus(status);
        return toResponse(orderRepository.save(order));
    }

    private Order findEntity(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(item -> new OrderItemResponse(
                        item.getProduct().getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPriceAtPurchase(),
                        item.getPriceAtPurchase().multiply(BigDecimal.valueOf(item.getQuantity()))
                ))
                .toList();

        return new OrderResponse(
                order.getId(),
                items,
                order.getTotalAmount(),
                order.getStatus(),
                order.getShippingAddress(),
                order.getCreatedAt()
        );
    }
}
