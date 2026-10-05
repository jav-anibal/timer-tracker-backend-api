package com.company.timertracker.controller;

import com.company.timertracker.config.SecurityConfig;
import com.company.timertracker.dto.UserResponse;
import com.company.timertracker.enums.RoleName;
import com.company.timertracker.exception.DuplicateResourceException;
import com.company.timertracker.exception.ResourceNotFoundException;
import com.company.timertracker.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    private static final String VALID_BODY = """
            {"username": "anibal", "email": "anibal@mail.com", "password": "password123"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void createReturns201WithLocationAndNoPassword() throws Exception {
        when(userService.create(any())).thenReturn(response(2L, "anibal"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/users/2"))
                .andExpect(jsonPath("$.username").value("anibal"))
                .andExpect(jsonPath("$.roles[0]").value("EMPLOYEE"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void createReturns400WhenBodyIsInvalid() throws Exception {
        String invalidBody = """
                {"username": "an", "email": "no-es-email", "password": "corta"}
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fields.username").exists())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.password").exists());

        verify(userService, never()).create(any());
    }

    @Test
    void createReturns409WhenUsernameIsTaken() throws Exception {
        when(userService.create(any()))
                .thenThrow(new DuplicateResourceException("Username already in use"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Username already in use"));
    }

    @Test
    @WithMockUser
    void findByIdReturnsUser() throws Exception {
        when(userService.findById(2L)).thenReturn(response(2L, "anibal"));

        mockMvc.perform(get("/api/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.username").value("anibal"));
    }

    @Test
    @WithMockUser
    void findByIdReturns404WhenUserDoesNotExist() throws Exception {
        when(userService.findById(99L))
                .thenThrow(new ResourceNotFoundException("User not found: 99"));

        mockMvc.perform(get("/api/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found: 99"));
    }

    @Test
    @WithMockUser
    void findAllReturnsEveryUser() throws Exception {
        when(userService.findAll()).thenReturn(List.of(
                response(1L, "anibal"),
                response(2L, "maria")));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].username").value("maria"));
    }

    @Test
    void findAllRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    private UserResponse response(Long id, String username) {
        return new UserResponse(
                id,
                username,
                username + "@mail.com",
                true,
                Instant.parse("2026-10-05T10:00:00Z"),
                Set.of(RoleName.EMPLOYEE));
    }
}
