package com.ecommerce.backend.service;

import com.ecommerce.backend.dto.AuthResponse;
import com.ecommerce.backend.dto.RegisterRequest;
import com.ecommerce.backend.entity.Role;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.exception.DuplicateResourceException;
import com.ecommerce.backend.repository.UserRepository;
import com.ecommerce.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Registration is the one place a caller could try to hand the server a role
 * — these tests exist to pin down that the server always ignores that and
 * assigns CUSTOMER itself, and that the raw password never reaches the DB.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_throwsDuplicateResourceException_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest("Rahul", "rahul@example.com", "password123");
        when(userRepository.existsByEmail("rahul@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_neverPersistsTheRawPasswordAndAlwaysAssignsCustomerRole() {
        RegisterRequest request = new RegisterRequest("Rahul", "rahul@example.com", "password123");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("rahul@example.com")).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("signed-jwt");

        AuthResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();

        assertThat(persisted.getPassword()).isEqualTo("bcrypt-hash");
        assertThat(persisted.getPassword()).isNotEqualTo("password123");
        assertThat(persisted.getRole()).isEqualTo(Role.CUSTOMER);

        assertThat(response.token()).isEqualTo("signed-jwt");
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
    }
}
