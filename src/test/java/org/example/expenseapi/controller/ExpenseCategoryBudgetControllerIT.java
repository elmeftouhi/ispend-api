package org.example.expenseapi.controller;

import org.example.expenseapi.dto.ExpenseCategoryBudgetCreateRequest;
import org.example.expenseapi.dto.ExpenseCategoryBudgetDto;
import org.example.expenseapi.dto.BudgetStatus;
import org.example.expenseapi.model.ExpenseCategory;
import org.example.expenseapi.model.ExpenseCategoryStatus;
import org.example.expenseapi.repository.ExpenseCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ExpenseCategoryBudgetControllerIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ExpenseCategoryRepository categoryRepository;

    private ExpenseCategory category;

    private String baseUrl() { return "/v1/expense-categories/" + category.getId() + "/budgets"; }

    @BeforeEach
    void setUp() {
        categoryRepository.deleteAll();
        category = new ExpenseCategory();
        category.setName("IT-Category");
        category.setStatus(ExpenseCategoryStatus.ACTIVE);
        category.setLevel(1);
        category = categoryRepository.save(category);
    }

    @Test
    void create_list_status_delete_flow() {
        // create budget for June 2025
        ExpenseCategoryBudgetCreateRequest req = new ExpenseCategoryBudgetCreateRequest();
        req.setYear(2025);
        req.setMonth(6);
        req.setBudget(new BigDecimal("500.00"));

        ResponseEntity<ExpenseCategoryBudgetDto> createResp = restTemplate.postForEntity(baseUrl(), req, ExpenseCategoryBudgetDto.class);
        assertThat(createResp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ExpenseCategoryBudgetDto created = createResp.getBody();
        assertThat(created).isNotNull();
        assertThat(created.getYear()).isEqualTo(2025);
        assertThat(created.getMonth()).isEqualTo(6);
        assertThat(created.getBudget()).isEqualByComparingTo(new BigDecimal("500.00"));

        // list
        ResponseEntity<ExpenseCategoryBudgetDto[]> listResp = restTemplate.getForEntity(baseUrl(), ExpenseCategoryBudgetDto[].class);
        assertThat(listResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        ExpenseCategoryBudgetDto[] arr = listResp.getBody();
        assertThat(arr).isNotNull();
        assertThat(arr).hasSize(1);

        // status
        ResponseEntity<BudgetStatus> statusResp = restTemplate.getForEntity(baseUrl() + "/2025/6/status", BudgetStatus.class);
        assertThat(statusResp.getStatusCode()).isEqualTo(HttpStatus.OK);
        BudgetStatus s = statusResp.getBody();
        assertThat(s).isNotNull();
        assertThat(s.getBudget()).isEqualByComparingTo(new BigDecimal("500.00"));
        assertThat(s.getSpent()).isNotNull();

        // delete
        HttpEntity<Void> empty = new HttpEntity<>(null);
        ResponseEntity<Void> delResp = restTemplate.exchange(baseUrl() + "/2025/6", HttpMethod.DELETE, empty, Void.class);
        assertThat(delResp.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // list now empty
        ResponseEntity<ExpenseCategoryBudgetDto[]> listAfter = restTemplate.getForEntity(baseUrl(), ExpenseCategoryBudgetDto[].class);
        assertThat(listAfter.getBody()).isEmpty();
    }
}
