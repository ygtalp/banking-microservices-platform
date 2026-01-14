package com.banking.auth.service;

import com.banking.auth.model.Role;
import com.banking.auth.model.User;
import com.banking.auth.model.UserStatus;
import com.banking.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Custom User Details Service Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User activeUser;
    private User lockedUser;
    private Role customerRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Create sample roles
        customerRole = Role.builder()
                .id(1L)
                .roleName("ROLE_CUSTOMER")
                .description("Customer role")
                .build();

        adminRole = Role.builder()
                .id(2L)
                .roleName("ROLE_ADMIN")
                .description("Admin role")
                .build();

        // Create active user with customer role
        activeUser = User.builder()
                .id(1L)
                .userId("USR-123456789012")
                .email("john.doe@example.com")
                .passwordHash("$2a$12$hashed.password.here")
                .firstName("John")
                .lastName("Doe")
                .status(UserStatus.ACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .mfaEnabled(false)
                .roles(new HashSet<>(Set.of(customerRole)))
                .build();

        // Create locked user
        lockedUser = User.builder()
                .id(2L)
                .userId("USR-223456789012")
                .email("jane.locked@example.com")
                .passwordHash("$2a$12$hashed.password.here")
                .firstName("Jane")
                .lastName("Locked")
                .status(UserStatus.LOCKED)
                .accountLocked(true)
                .failedLoginAttempts(5)
                .mfaEnabled(false)
                .roles(new HashSet<>(Set.of(customerRole)))
                .build();
    }

    // ==================== LOAD USER BY USERNAME (EMAIL) TESTS ====================

    @Test
    @DisplayName("Should load user by email successfully")
    void shouldLoadUserByEmailSuccessfully() {
        // Given
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john.doe@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("john.doe@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("$2a$12$hashed.password.here");
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();

        verify(userRepository, times(1)).findByEmail("john.doe@example.com");
    }

    @Test
    @DisplayName("Should throw UsernameNotFoundException when user not found")
    void shouldThrowUsernameNotFoundExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findByEmail(anyString()))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("notfound@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found with email: notfound@example.com");

        verify(userRepository, times(1)).findByEmail("notfound@example.com");
    }

    // ==================== AUTHORITIES TESTS ====================

    @Test
    @DisplayName("Should load user with correct authorities")
    void shouldLoadUserWithCorrectAuthorities() {
        // Given
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john.doe@example.com");

        // Then
        assertThat(userDetails.getAuthorities()).isNotNull();
        assertThat(userDetails.getAuthorities()).hasSize(1);
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("Should load user with multiple roles")
    void shouldLoadUserWithMultipleRoles() {
        // Given
        activeUser.addRole(adminRole); // Now user has 2 roles
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john.doe@example.com");

        // Then
        assertThat(userDetails.getAuthorities()).hasSize(2);
        assertThat(userDetails.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Should load user with no roles")
    void shouldLoadUserWithNoRoles() {
        // Given
        activeUser.getRoles().clear(); // Remove all roles
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john.doe@example.com");

        // Then
        assertThat(userDetails.getAuthorities()).isEmpty();
    }

    // ==================== ACCOUNT LOCKED TESTS ====================

    @Test
    @DisplayName("Should load locked user with account locked flag")
    void shouldLoadLockedUserWithAccountLockedFlag() {
        // Given
        when(userRepository.findByEmail("jane.locked@example.com"))
                .thenReturn(Optional.of(lockedUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("jane.locked@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("jane.locked@example.com");
        assertThat(userDetails.isAccountNonLocked()).isFalse(); // Account is locked
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonExpired()).isTrue();
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should load unlocked user with account non-locked flag")
    void shouldLoadUnlockedUserWithAccountNonLockedFlag() {
        // Given
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john.doe@example.com");

        // Then
        assertThat(userDetails.isAccountNonLocked()).isTrue();
    }

    // ==================== USER DETAILS FIELDS TESTS ====================

    @Test
    @DisplayName("Should use email as username in UserDetails")
    void shouldUseEmailAsUsernameInUserDetails() {
        // Given
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john.doe@example.com");

        // Then
        assertThat(userDetails.getUsername()).isEqualTo("john.doe@example.com");
        assertThat(userDetails.getUsername()).isEqualTo(activeUser.getEmail());
    }

    @Test
    @DisplayName("Should use password hash as password in UserDetails")
    void shouldUsePasswordHashAsPasswordInUserDetails() {
        // Given
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john.doe@example.com");

        // Then
        assertThat(userDetails.getPassword()).isEqualTo("$2a$12$hashed.password.here");
        assertThat(userDetails.getPassword()).isEqualTo(activeUser.getPasswordHash());
    }

    @Test
    @DisplayName("Should always set account non-expired to true")
    void shouldAlwaysSetAccountNonExpiredToTrue() {
        // Given
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john.doe@example.com");

        // Then
        assertThat(userDetails.isAccountNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should always set credentials non-expired to true")
    void shouldAlwaysSetCredentialsNonExpiredToTrue() {
        // Given
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john.doe@example.com");

        // Then
        assertThat(userDetails.isCredentialsNonExpired()).isTrue();
    }

    @Test
    @DisplayName("Should always set enabled to true")
    void shouldAlwaysSetEnabledToTrue() {
        // Given
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john.doe@example.com");

        // Then
        assertThat(userDetails.isEnabled()).isTrue();
    }

    // ==================== EDGE CASES ====================

    @Test
    @DisplayName("Should handle user with special characters in email")
    void shouldHandleUserWithSpecialCharactersInEmail() {
        // Given
        activeUser.setEmail("john.doe+test@example.com");
        when(userRepository.findByEmail("john.doe+test@example.com"))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john.doe+test@example.com");

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("john.doe+test@example.com");
    }

    @Test
    @DisplayName("Should handle user with long email")
    void shouldHandleUserWithLongEmail() {
        // Given
        String longEmail = "very.long.email.address.with.many.dots@subdomain.example.com";
        activeUser.setEmail(longEmail);
        when(userRepository.findByEmail(longEmail))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername(longEmail);

        // Then
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(longEmail);
    }

    // ==================== INTEGRATION WITH SPRING SECURITY ====================

    @Test
    @DisplayName("Should create UserDetails compatible with Spring Security")
    void shouldCreateUserDetailsCompatibleWithSpringSecurity() {
        // Given
        when(userRepository.findByEmail("john.doe@example.com"))
                .thenReturn(Optional.of(activeUser));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john.doe@example.com");

        // Then
        assertThat(userDetails).isInstanceOf(org.springframework.security.core.userdetails.UserDetails.class);
        assertThat(userDetails.getUsername()).isNotNull();
        assertThat(userDetails.getPassword()).isNotNull();
        assertThat(userDetails.getAuthorities()).isNotNull();
    }
}
