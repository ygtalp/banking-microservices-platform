package com.banking.auth.service;

import com.banking.auth.config.JwtConfig;
import com.banking.auth.dto.*;
import com.banking.auth.exception.AccountLockedException;
import com.banking.auth.exception.EmailAlreadyExistsException;
import com.banking.auth.exception.InvalidTokenException;
import com.banking.auth.exception.TokenBlacklistedException;
import com.banking.auth.model.Role;
import com.banking.auth.model.User;
import com.banking.auth.model.UserStatus;
import com.banking.auth.repository.RoleRepository;
import com.banking.auth.repository.UserRepository;
import com.banking.auth.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        // Setup test data
        registerRequest = RegisterRequest.builder()
                .email("test@example.com")
                .password("Test@1234")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+905551234567")
                .build();

        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("Test@1234")
                .build();

        customerRole = Role.builder()
                .roleName("ROLE_CUSTOMER")
                .description("Customer role")
                .permissions(new HashSet<>())
                .build();

        testUser = User.builder()
                .id(1L)
                .userId("USR-TEST123456")
                .email("test@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+905551234567")
                .status(UserStatus.ACTIVE)
                .accountLocked(false)
                .failedLoginAttempts(0)
                .roles(new HashSet<>(Collections.singletonList(customerRole)))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Setup JWT config defaults
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(900000L);
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L);
    }

    @Test
    @DisplayName("Should register new user successfully")
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByRoleName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessTokenFromUsername(anyString(), anyList())).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshTokenFromUsername(anyString(), anyList())).thenReturn("refresh-token");
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        // Act
        ApiResponse<LoginResponse> response = authService.register(registerRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("test@example.com", response.getData().getEmail());
        assertEquals("John", response.getData().getFirstName());
        assertEquals("access-token", response.getData().getAccessToken());
        assertEquals("refresh-token", response.getData().getRefreshToken());

        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository).save(any(User.class));
        verify(kafkaTemplate).send(eq("user-events"), any());
    }

    @Test
    @DisplayName("Should throw EmailAlreadyExistsException when email exists")
    void testRegister_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(EmailAlreadyExistsException.class, () -> {
            authService.register(registerRequest);
        });

        verify(userRepository).existsByEmail("test@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void testLogin_Success() {
        // Arrange
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "test@example.com", "Test@1234");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(jwtTokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(Authentication.class))).thenReturn("refresh-token");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        // Act
        ApiResponse<LoginResponse> response = authService.login(loginRequest);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("test@example.com", response.getData().getEmail());
        assertEquals("access-token", response.getData().getAccessToken());

        verify(authenticationManager).authenticate(any(Authentication.class));
        verify(userRepository).save(any(User.class));
        verify(kafkaTemplate).send(eq("user-events"), any());
    }

    @Test
    @DisplayName("Should throw AccountLockedException when account is locked")
    void testLogin_AccountLocked() {
        // Arrange
        testUser.setAccountLocked(true);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThrows(AccountLockedException.class, () -> {
            authService.login(loginRequest);
        });

        verify(authenticationManager, never()).authenticate(any(Authentication.class));
    }

    @Test
    @DisplayName("Should increment failed attempts on bad credentials")
    void testLogin_IncrementFailedAttempts() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });

        verify(userRepository).save(argThat(user -> user.getFailedLoginAttempts() == 1));
    }

    @Test
    @DisplayName("Should logout successfully and blacklist token")
    void testLogout_Success() {
        // Arrange
        String token = "valid-token";
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        doNothing().when(tokenBlacklistService).blacklistToken(anyString());
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        // Act
        ApiResponse<Void> response = authService.logout(token, email);

        // Assert
        assertTrue(response.isSuccess());
        verify(tokenBlacklistService).blacklistToken(token);
        verify(kafkaTemplate).send(eq("user-events"), any());
    }

    @Test
    @DisplayName("Should refresh token successfully")
    void testRefreshToken_Success() {
        // Arrange
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("valid-refresh-token")
                .build();

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(tokenBlacklistService.isTokenBlacklisted(anyString())).thenReturn(false);
        when(jwtTokenProvider.getTokenType(anyString())).thenReturn("refresh");
        when(jwtTokenProvider.getUsernameFromToken(anyString())).thenReturn("test@example.com");
        when(jwtTokenProvider.getRolesFromToken(anyString())).thenReturn(Arrays.asList("CUSTOMER"));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessTokenFromUsername(anyString(), anyList())).thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshTokenFromUsername(anyString(), anyList())).thenReturn("new-refresh-token");
        doNothing().when(tokenBlacklistService).blacklistToken(anyString());

        // Act
        ApiResponse<LoginResponse> response = authService.refreshToken(request);

        // Assert
        assertTrue(response.isSuccess());
        assertNotNull(response.getData());
        assertEquals("new-access-token", response.getData().getAccessToken());
        assertEquals("new-refresh-token", response.getData().getRefreshToken());

        verify(tokenBlacklistService).blacklistToken("valid-refresh-token");
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when refresh token is invalid")
    void testRefreshToken_InvalidToken() {
        // Arrange
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("invalid-token")
                .build();

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> {
            authService.refreshToken(request);
        });

        verify(tokenBlacklistService, never()).blacklistToken(anyString());
    }

    @Test
    @DisplayName("Should throw TokenBlacklistedException when refresh token is blacklisted")
    void testRefreshToken_BlacklistedToken() {
        // Arrange
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("blacklisted-token")
                .build();

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(tokenBlacklistService.isTokenBlacklisted(anyString())).thenReturn(true);

        // Act & Assert
        assertThrows(TokenBlacklistedException.class, () -> {
            authService.refreshToken(request);
        });
    }

    @Test
    @DisplayName("Should throw InvalidTokenException when token type is not refresh")
    void testRefreshToken_WrongTokenType() {
        // Arrange
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("access-token-not-refresh")
                .build();

        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(tokenBlacklistService.isTokenBlacklisted(anyString())).thenReturn(false);
        when(jwtTokenProvider.getTokenType(anyString())).thenReturn("access");

        // Act & Assert
        assertThrows(InvalidTokenException.class, () -> {
            authService.refreshToken(request);
        });
    }

    @Test
    @DisplayName("Should reset failed attempts on successful login")
    void testLogin_ResetFailedAttempts() {
        // Arrange
        testUser.setFailedLoginAttempts(3);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "test@example.com", "Test@1234");

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authentication);
        when(jwtTokenProvider.generateAccessToken(any(Authentication.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(Authentication.class))).thenReturn("refresh-token");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(null);

        // Act
        authService.login(loginRequest);

        // Assert
        verify(userRepository).save(argThat(user -> user.getFailedLoginAttempts() == 0));
    }
}
