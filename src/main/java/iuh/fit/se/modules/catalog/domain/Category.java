package iuh.fit.se.modules.catalog.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Category — Aggregate Root riêng biệt cho module Catalog.
 * Có lifecycle độc lập với Book.
 */
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Category {
    @Setter
    private Long id;
    private String name;
    private boolean isActive;

    public void rename(String newName) {
        this.name = newName;
    }

    public void disable() {
        this.isActive = false;
    }
}
