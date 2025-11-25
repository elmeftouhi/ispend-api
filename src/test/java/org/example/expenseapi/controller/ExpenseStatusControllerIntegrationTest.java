package org.example.expenseapi.controller;

import org.example.expenseapi.model.ExpenseStatus;
import org.example.expenseapi.repository.ExpenseStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class ExpenseStatusControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExpenseStatusRepository repository;

    @BeforeEach
    public void cleanup() {
        repository.deleteAll();
    }

    @Test
    @WithMockUser(username = "integrationUser")
    public void create_shouldPersistAndReturnDto() throws Exception {
        String json = "{\"name\":\"Reimbursable\"}";

        MvcResult result = mockMvc.perform(post("/v1/expense-statuses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Reimbursable"))
                .andReturn();

        // extract id from response and assert repository contains it
        String content = result.getResponse().getContentAsString();
        // naive extraction of id value (Jackson not used directly here for brevity)
        assertThat(content).contains("Reimbursable");

        List<ExpenseStatus> all = repository.findAll();
        assertThat(all).hasSize(1);
        ExpenseStatus saved = all.get(0);
        assertThat(saved.getName()).isEqualTo("Reimbursable");
        // AuditorAware should have written the current mocked username
        assertThat(saved.getCreatedBy()).isEqualTo("integrationUser");
    }

    @Test
    @WithMockUser(username = "integrationUser")
    public void list_shouldReturnSavedStatuses() throws Exception {
        ExpenseStatus a = new ExpenseStatus();
        a.setName("A");
        ExpenseStatus b = new ExpenseStatus();
        b.setName("B");
        repository.saveAll(List.of(a, b));

        mockMvc.perform(get("/v1/expense-statuses").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @WithMockUser(username = "integrationUser")
    public void update_and_delete_shouldModifyRepository() throws Exception {
        ExpenseStatus s = new ExpenseStatus();
        s.setName("Old");
        s = repository.save(s);

        // update
        mockMvc.perform(put("/v1/expense-statuses/" + s.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"New\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(s.getId()))
                .andExpect(jsonPath("$.name").value("New"));

        ExpenseStatus reloaded = repository.findById(s.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("New");

        // delete
        mockMvc.perform(delete("/v1/expense-statuses/" + s.getId()))
                .andExpect(status().isNoContent());

        assertThat(repository.findById(s.getId())).isEmpty();
    }

    // New tests for duplicate-name and error scenarios

    @Test
    @WithMockUser(username = "integrationUser")
    public void create_duplicate_shouldReturn409() throws Exception {
        ExpenseStatus existing = new ExpenseStatus();
        existing.setName("Duplicate");
        repository.save(existing);

        mockMvc.perform(post("/v1/expense-statuses")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Duplicate\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Status name already in use"));

        // ensure repository still contains only the original
        List<ExpenseStatus> all = repository.findAll();
        assertThat(all).hasSize(1);
    }

    @Test
    @WithMockUser(username = "integrationUser")
    public void update_duplicate_shouldReturn409() throws Exception {
        ExpenseStatus a = new ExpenseStatus();
        a.setName("A");
        a = repository.save(a);
        ExpenseStatus b = new ExpenseStatus();
        b.setName("B");
        b = repository.save(b);

        // attempt to rename A to B -> should conflict
        mockMvc.perform(put("/v1/expense-statuses/" + a.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"B\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Status name already in use"));

        // ensure names unchanged
        ExpenseStatus reloadA = repository.findById(a.getId()).orElseThrow();
        ExpenseStatus reloadB = repository.findById(b.getId()).orElseThrow();
        assertThat(reloadA.getName()).isEqualTo("A");
        assertThat(reloadB.getName()).isEqualTo("B");
    }

    @Test
    @WithMockUser(username = "integrationUser")
    public void delete_nonexistent_shouldReturn404() throws Exception {
        mockMvc.perform(delete("/v1/expense-statuses/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Status not found"));
    }
}
