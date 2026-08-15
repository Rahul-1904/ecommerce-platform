package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.ProductRequest;
import com.ecommerce.backend.dto.ProductResponse;
import com.ecommerce.backend.entity.Category;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.exception.ResourceNotFoundException;
import com.ecommerce.backend.repository.CategoryRepository;
import com.ecommerce.backend.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void create_throwsResourceNotFoundException_whenCategoryDoesNotExist() {
        ProductRequest request = new ProductRequest("Wireless Mouse", "desc", new BigDecimal("799.00"), 40, 99L);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");

        verify(productRepository, never()).save(any());
    }

    @Test
    void create_linksTheProductToTheResolvedCategory() {
        Category category = Category.builder().id(1L).name("Electronics").build();
        ProductRequest request = new ProductRequest("Wireless Mouse", "desc", new BigDecimal("799.00"), 40, 1L);

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponse response = productService.create(request);

        assertThat(response.name()).isEqualTo("Wireless Mouse");
        assertThat(response.categoryId()).isEqualTo(1L);
        assertThat(response.categoryName()).isEqualTo("Electronics");
    }

    @Test
    void getById_throwsResourceNotFoundException_whenProductMissing() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void delete_throwsResourceNotFoundException_ratherThanSilentlyNoOp_whenProductMissing() {
        when(productRepository.existsById(42L)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(42L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(productRepository, never()).deleteById(any());
    }
}
