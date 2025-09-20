package com.example.treeapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SearchRequest {
    @NotBlank(message = "Search query cannot be empty.")
    @Size(max = 100, message = "Search query cannot exceed 100 characters.")
    private String query;
}
