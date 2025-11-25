package org.example.expenseapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.expenseapi.dto.UserCreateRequest;
import org.example.expenseapi.dto.UserUpdateRequest;
import org.example.expenseapi.model.User;
import org.example.expenseapi.model.UserStatus;
import org.example.expenseapi.application.service.UserApplicationService;
import org.example.expenseapi.testutil.UserBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UserApplicationService userService = mock(UserApplicationService.class);
    private UserController userController;

    @BeforeEach
    public void setup() {
        userController = new UserController(userService);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new TestExceptionHandler())
                .build();
    }

    @Test
    public void createUser_shouldReturnCreated() throws Exception {
        UserCreateRequest req = new UserCreateRequest();
        req.setFirstname("John");
        req.setLastname("Doe");
        req.setEmail("john@example.com");
        req.setPassword("secret");
        req.setStatus(UserStatus.ACTIVE);

        User created = UserBuilder.aUser()
                .withId(1L)
                .withFirstName(req.getFirstname())
                .withLastName(req.getLastname())
                .withEmail(req.getEmail())
                .withStatus(req.getStatus())
                .build();

        when(userService.createUser(any(User.class))).thenReturn(created);

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.firstname").value("John"))
                .andExpect(jsonPath("$.email").value("john@example.com"));

        verify(userService, times(1)).createUser(any(User.class));
    }

    @Test
    public void getUser_found_shouldReturnOk() throws Exception {
        User u = UserBuilder.aUser()
                .withId(2L)
                .withFirstName("Jane")
                .withLastName("Smith")
                .withEmail("jane@example.com")
                .build();

        when(userService.findById(2L)).thenReturn(Optional.of(u));

        mockMvc.perform(get("/v1/users/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.firstname").value("Jane"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));

        verify(userService, times(1)).findById(2L);
    }

    @Test
    public void getUser_notFound_shouldReturnServerError() throws Exception {
        when(userService.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/users/99"))
                .andExpect(status().isNotFound());

        verify(userService, times(1)).findById(99L);
    }

    @Test
    public void listUsers_shouldReturnList() throws Exception {
        User a = UserBuilder.aUser()
                .withId(1L)
                .withFirstName("A")
                .withLastName("A")
                .withEmail("a@example.com")
                .build();

        User b = UserBuilder.aUser()
                .withId(2L)
                .withFirstName("B")
                .withLastName("B")
                .withEmail("b@example.com")
                .build();

        List<User> users = Arrays.asList(a, b);
        when(userService.findAll()).thenReturn(users);

        mockMvc.perform(get("/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));

        verify(userService, times(1)).findAll();
    }

    @Test
    public void updateUser_shouldReturnOk() throws Exception {
        UserUpdateRequest req = new UserUpdateRequest();
        req.setFirstname("New");
        req.setLastname("Name");
        req.setEmail("new@example.com");
        req.setPassword("pwd");
        req.setStatus(UserStatus.INACTIVE);

        User updated = UserBuilder.aUser()
                .withId(5L)
                .withFirstName(req.getFirstname())
                .withLastName(req.getLastname())
                .withEmail(req.getEmail())
                .withStatus(req.getStatus())
                .build();

        // ensure controller's existence check passes
        when(userService.findById(5L)).thenReturn(Optional.of(UserBuilder.aUser().withId(5L).build()));

        when(userService.updateUser(eq(5L), any(User.class))).thenReturn(updated);

        mockMvc.perform(put("/v1/users/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(userService, times(1)).updateUser(eq(5L), any(User.class));
    }

    @Test
    public void deleteUser_shouldReturnNoContent() throws Exception {
        // ensure controller's existence check passes
        when(userService.findById(7L)).thenReturn(Optional.of(UserBuilder.aUser().withId(7L).build()));
        doNothing().when(userService).deleteUser(7L);

        mockMvc.perform(delete("/v1/users/7"))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(7L);
    }

    @Test
    public void createUser_emailConflict_shouldReturnConflict() throws Exception {
        String email = "exists@example.com";
        User existing = UserBuilder.aUser().withId(10L).withEmail(email).build();

        when(userService.findByEmail(email)).thenReturn(Optional.of(existing));

        UserCreateRequest req = new UserCreateRequest();
        req.setFirstname("New");
        req.setLastname("User");
        req.setEmail(email);
        req.setPassword("pwd");
        req.setStatus(UserStatus.ACTIVE);

        mockMvc.perform(post("/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Email already in use"));

        verify(userService, times(1)).findByEmail(email);
        verify(userService, never()).createUser(any(User.class));
    }

    @Test
    public void toggleStatus_activeToInactive_shouldReturnUpdated() throws Exception {
        long id = 3L;
        User existing = UserBuilder.aUser().withId(id).withStatus(UserStatus.ACTIVE).build();
        User updated = UserBuilder.aUser().withId(id).withStatus(UserStatus.INACTIVE).build();

        when(userService.findById(id)).thenReturn(Optional.of(existing));
        when(userService.updateUser(eq(id), any(User.class))).thenReturn(updated);

        mockMvc.perform(patch("/v1/users/3/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        verify(userService, times(1)).findById(id);
        verify(userService, times(1)).updateUser(eq(id), any(User.class));
    }

    @Test
    public void toggleStatus_inactiveToActive_shouldReturnUpdated() throws Exception {
        long id = 4L;
        User existing = UserBuilder.aUser().withId(id).withStatus(org.example.expenseapi.model.UserStatus.INACTIVE).build();
        User updated = UserBuilder.aUser().withId(id).withStatus(org.example.expenseapi.model.UserStatus.ACTIVE).build();

        when(userService.findById(id)).thenReturn(Optional.of(existing));
        when(userService.updateUser(eq(id), any(User.class))).thenReturn(updated);

        mockMvc.perform(patch("/v1/users/4/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(4))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(userService, times(1)).findById(id);
        verify(userService, times(1)).updateUser(eq(id), any(User.class));
    }

    @Test
    public void toggleStatus_userNotFound_shouldReturnNotFound() throws Exception {
        long id = 42L;
        when(userService.findById(id)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/v1/users/42/status"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("User not found"));

        verify(userService, times(1)).findById(id);
        verify(userService, never()).updateUser(anyLong(), any(User.class));
    }

}
