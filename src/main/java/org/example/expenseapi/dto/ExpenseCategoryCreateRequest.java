package org.example.expenseapi.dto;

import org.example.expenseapi.model.ExpenseCategoryStatus;

public class ExpenseCategoryCreateRequest {
    private String name;
    private ExpenseCategoryStatus status;
    private Integer level;
    private Long parentId;

    public ExpenseCategoryCreateRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ExpenseCategoryStatus getStatus() { return status; }
    public void setStatus(ExpenseCategoryStatus status) { this.status = status; }

    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }

    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}
