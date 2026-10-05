package com.company.timertracker.service;

import com.company.timertracker.dto.UserCreateRequest;
import com.company.timertracker.dto.UserResponse;
import com.company.timertracker.enums.RoleName;
import com.company.timertracker.exception.DuplicateResourceException;
import com.company.timertracker.exception.ResourceNotFoundException;
import com.company.timertracker.model.Role;
import com.company.timertracker.model.User;
import com.company.timertracker.repository.RoleRepository;
import com.company.timertracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserCreateRequest request;
    private Role employeeRole;

    @BeforeEach
    void setUp() {
        request = new UserCreateRequest("anibal", "anibal@mail.com", "password123");
        employeeRole = new Role(RoleName.EMPLOYEE);
    }

    @Test
    void createSavesUserWithHashedPasswordAndEmployeeRole() {
        when(userRepository.existsByUsername("anibal")).thenReturn(false);
        when(userRepository.existsByEmail("anibal@mail.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
        when(passwordEncoder.encode("password123")).thenReturn("hash");

        UserResponse response = userService.create(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getPasswordHash()).isEqualTo("hash");
        assertThat(saved.getRoles()).containsExactly(employeeRole);
        assertThat(saved.isEnabled()).isTrue();
        assertThat(response.username()).isEqualTo("anibal");
        assertThat(response.roles()).containsExactly(RoleName.EMPLOYEE);
    }

    @Test
    void createStoresPasswordEncodedWithBcrypt() {
        PasswordEncoder bcrypt = new BCryptPasswordEncoder();
        UserService serviceWithBcrypt = new UserService(userRepository, roleRepository, bcrypt);
        when(userRepository.existsByUsername("anibal")).thenReturn(false);
        when(userRepository.existsByEmail("anibal@mail.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));

        serviceWithBcrypt.create(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        String stored = captor.getValue().getPasswordHash();

        assertThat(stored).isNotEqualTo("password123");
        assertThat(stored).startsWith("$2");
        assertThat(bcrypt.matches("password123", stored)).isTrue();
    }

    @Test
    void createFailsWhenUsernameIsTaken() {
        when(userRepository.existsByUsername("anibal")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username already in use");

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createFailsWhenEmailIsTaken() {
        when(userRepository.existsByUsername("anibal")).thenReturn(false);
        when(userRepository.existsByEmail("anibal@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Email already in use");

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void createReturnsDuplicateWhenDatabaseRejectsConcurrentInsert() {
        when(userRepository.existsByUsername("anibal")).thenReturn(false);
        when(userRepository.existsByEmail("anibal@mail.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.EMPLOYEE)).thenReturn(Optional.of(employeeRole));
        when(userRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("uk_users_username"));

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Username or email already in use");
    }

    @Test
    void createFailsWhenEmployeeRoleIsMissing() {
        when(userRepository.existsByUsername("anibal")).thenReturn(false);
        when(userRepository.existsByEmail("anibal@mail.com")).thenReturn(false);
        when(roleRepository.findByName(RoleName.EMPLOYEE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Role EMPLOYEE not initialized");

        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void findByIdReturnsUser() {
        User user = new User();
        user.setUsername("anibal");
        user.setEmail("anibal@mail.com");
        user.addRole(employeeRole);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.findById(1L);

        assertThat(response.username()).isEqualTo("anibal");
        assertThat(response.email()).isEqualTo("anibal@mail.com");
    }

    @Test
    void findByIdFailsWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User not found: 99");
    }

    @Test
    void findAllMapsEveryUser() {
        User first = new User();
        first.setUsername("anibal");
        User second = new User();
        second.setUsername("maria");
        when(userRepository.findAll()).thenReturn(List.of(first, second));

        List<UserResponse> responses = userService.findAll();

        assertThat(responses).extracting(UserResponse::username)
                .containsExactly("anibal", "maria");
    }
}
