package com.company.timertracker.service;

import com.company.timertracker.enums.RoleName;
import com.company.timertracker.model.Role;
import com.company.timertracker.model.User;
import com.company.timertracker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void loadsUserWithRolePrefixedAuthorities() {
        User user = new User();
        user.setUsername("anibal");
        user.setPasswordHash("hash");
        user.addRole(new Role(RoleName.EMPLOYEE));
        when(userRepository.findByUsername("anibal")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("anibal");

        assertThat(details.getUsername()).isEqualTo("anibal");
        assertThat(details.getPassword()).isEqualTo("hash");
        assertThat(details.isEnabled()).isTrue();
        assertThat(details.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_EMPLOYEE");
    }

    @Test
    void loadsDisabledUserAsDisabled() {
        User user = new User();
        user.setUsername("anibal");
        user.setPasswordHash("hash");
        user.setEnabled(false);
        when(userRepository.findByUsername("anibal")).thenReturn(Optional.of(user));

        UserDetails details = userDetailsService.loadUserByUsername("anibal");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    void failsWhenUsernameDoesNotExist() {
        when(userRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("nadie"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: nadie");
    }
}
