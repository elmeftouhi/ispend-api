package org.example.expenseapi.dto;

public class ExpenseStatusUpdateRequest {
    private String name;
    private Boolean isDefault;

    public ExpenseStatusUpdateRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getIsDefault() { return isDefault; }
    public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
}
