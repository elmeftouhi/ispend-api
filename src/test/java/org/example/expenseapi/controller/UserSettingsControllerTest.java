package org.example.expenseapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.expenseapi.application.service.UserSettingsApplicationService;
import org.example.expenseapi.dto.UserSettingsUpdateRequest;
import org.example.expenseapi.model.User;
import org.example.expenseapi.model.UserSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class UserSettingsControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UserSettingsApplicationService service = mock(UserSettingsApplicationService.class);
    private UserSettingsController controller;

    @BeforeEach
    public void setup() {
        controller = new UserSettingsController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TestExceptionHandler())
                .build();
    }

    @Test
    public void getSettings_found_shouldReturnOk() throws Exception {
        long userId = 7L;
        User u = new User(); u.setId(userId);
        UserSettings s = new UserSettings(); s.setId(11L); s.setUser(u); s.setCurrency("EUR"); s.setDecimalDigits(2); s.setWeekStart("MONDAY");

        when(service.findByUserId(userId)).thenReturn(Optional.of(s));

        mockMvc.perform(get("/v1/users/7/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.currency").value("EUR"));

        verify(service, times(1)).findByUserId(userId);
    }

    @Test
    public void getSettings_notFound_shouldReturn404() throws Exception {
        when(service.findByUserId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/users/99/settings"))
                .andExpect(status().isNotFound());

        verify(service, times(1)).findByUserId(99L);
    }

    @Test
    public void updateSettings_valid_shouldReturnOk() throws Exception {
        long userId = 5L;
        User u = new User(); u.setId(userId);
        UserSettings existing = new UserSettings(); existing.setId(21L); existing.setUser(u);
        UserSettings saved = new UserSettings(); saved.setId(21L); saved.setUser(u); saved.setCurrency("USD"); saved.setDecimalDigits(2); saved.setWeekStart("SUNDAY");

        when(service.createOrUpdate(eq(userId), any(UserSettings.class))).thenReturn(saved);

        UserSettingsUpdateRequest req = new UserSettingsUpdateRequest();
        req.setCurrency("USD"); req.setDecimalDigits(2); req.setWeekStart("sunday");

        mockMvc.perform(put("/v1/users/5/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(21))
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.weekStart").value("SUNDAY"));

        verify(service, times(1)).createOrUpdate(eq(userId), any(UserSettings.class));
    }

    @Test
    public void updateSettings_invalidWeekStart_shouldReturn400() throws Exception {
        UserSettingsUpdateRequest req = new UserSettingsUpdateRequest();
        req.setWeekStart("notaday");

        mockMvc.perform(put("/v1/users/2/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verify(service, never()).createOrUpdate(anyLong(), any(UserSettings.class));
    }
}

