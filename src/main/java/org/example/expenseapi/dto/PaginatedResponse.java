package org.example.expenseapi.dto;

import java.util.List;

public class PaginatedResponse<T> {
    private List<T> data;
    private PaginationMetadata pagination;

    public PaginatedResponse() {}

    public PaginatedResponse(List<T> data, PaginationMetadata pagination) {
        this.data = data;
        this.pagination = pagination;
    }

    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }

    public PaginationMetadata getPagination() { return pagination; }
    public void setPagination(PaginationMetadata pagination) { this.pagination = pagination; }
}

