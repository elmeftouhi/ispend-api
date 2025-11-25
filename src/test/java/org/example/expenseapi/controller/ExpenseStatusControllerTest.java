package org.example.expenseapi.controller;

import org.example.expenseapi.model.ExpenseStatus;
import org.example.expenseapi.service.ExpenseStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ExpenseStatusControllerTest {

    private MockMvc mockMvc;
    private final ExpenseStatusService service = mock(ExpenseStatusService.class);

    @BeforeEach
    public void setup() {
        ExpenseStatusController controller = new ExpenseStatusController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new TestExceptionHandler())
                .build();
    }

    @Test
    public void create_shouldReturnCreated() throws Exception {
        when(service.findByName("Reimbursable")).thenReturn(Optional.empty());

        ExpenseStatus created = new ExpenseStatus();
        created.setId(1L);
        created.setName("Reimbursable");
        created.setCreatedAt(Instant.now());
        created.setCreatedBy("tester");

        when(service.createExpenseStatus(any())).thenReturn(created);

        mockMvc.perform(post("/v1/expense-statuses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Reimbursable\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Reimbursable"));

        verify(service, times(1)).findByName("Reimbursable");
        verify(service, times(1)).createExpenseStatus(any());
    }

    @Test
    public void create_conflict_shouldReturn409() throws Exception {
        ExpenseStatus existing = new ExpenseStatus();
        existing.setId(2L);
        existing.setName("Reimbursable");
        when(service.findByName("Reimbursable")).thenReturn(Optional.of(existing));

        mockMvc.perform(post("/v1/expense-statuses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Reimbursable\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Status name already in use"));

        verify(service, times(1)).findByName("Reimbursable");
        verify(service, never()).createExpenseStatus(any());
    }

    @Test
    public void get_found_shouldReturnStatus() throws Exception {
        ExpenseStatus s = new ExpenseStatus();
        s.setId(3L);
        s.setName("NonReimbursable");
        s.setCreatedAt(Instant.now());
        when(service.findById(3L)).thenReturn(Optional.of(s));

        mockMvc.perform(get("/v1/expense-statuses/3").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.name").value("NonReimbursable"));

        verify(service, times(1)).findById(3L);
    }

    @Test
    public void get_notFound_shouldReturn404() throws Exception {
        when(service.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/expense-statuses/99").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Status not found"));

        verify(service, times(1)).findById(99L);
    }

    @Test
    public void list_shouldReturnAll() throws Exception {
        ExpenseStatus a = new ExpenseStatus();
        a.setId(10L);
        a.setName("A");
        ExpenseStatus b = new ExpenseStatus();
        b.setId(11L);
        b.setName("B");

        when(service.findAll()).thenReturn(List.of(a, b));

        mockMvc.perform(get("/v1/expense-statuses").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[1].id").value(11));

        verify(service, times(1)).findAll();
    }

    @Test
    public void update_found_shouldReturnUpdated() throws Exception {
        ExpenseStatus existing = new ExpenseStatus();
        existing.setId(20L);
        existing.setName("Old");
        when(service.findById(20L)).thenReturn(Optional.of(existing));

        ExpenseStatus updated = new ExpenseStatus();
        updated.setId(20L);
        updated.setName("New");
        when(service.updateExpenseStatus(eq(20L), any())).thenReturn(updated);

        mockMvc.perform(put("/v1/expense-statuses/20")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.name").value("New"));

        verify(service, times(1)).findById(20L);
        verify(service, times(1)).updateExpenseStatus(eq(20L), any());
    }

    @Test
    public void update_notFound_shouldReturn404() throws Exception {
        when(service.findById(77L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/v1/expense-statuses/77")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"whatever\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Status not found"));

        verify(service, times(1)).findById(77L);
        verify(service, never()).updateExpenseStatus(anyLong(), any());
    }

    @Test
    public void delete_found_shouldReturnNoContent() throws Exception {
        ExpenseStatus existing = new ExpenseStatus();
        existing.setId(30L);
        existing.setName("ToDelete");
        when(service.findById(30L)).thenReturn(Optional.of(existing));

        mockMvc.perform(delete("/v1/expense-statuses/30"))
                .andExpect(status().isNoContent());

        verify(service, times(1)).findById(30L);
        verify(service, times(1)).deleteExpenseStatus(30L);
    }

    @Test
    public void delete_notFound_shouldReturn404() throws Exception {
        when(service.findById(123L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/v1/expense-statuses/123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Status not found"));

        verify(service, times(1)).findById(123L);
        verify(service, never()).deleteExpenseStatus(anyLong());
    }
}

