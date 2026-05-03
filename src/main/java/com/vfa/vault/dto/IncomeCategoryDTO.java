package com.vfa.vault.dto;

public class IncomeCategoryDTO {

    public record Response(
            Integer id,
            String name,
            String icon
    ) {}
}
