package com.vfa.vault.dto;

public class CategoryDTO {

    public record Response(
            Integer id,
            String name,
            String icon
    ) {}
}
