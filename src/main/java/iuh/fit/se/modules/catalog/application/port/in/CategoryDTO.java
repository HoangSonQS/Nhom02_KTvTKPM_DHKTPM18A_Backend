package iuh.fit.se.modules.catalog.application.port.in;

/**
 * CategoryDTO - Data Transfer Object for inter-module Catalog category access.
 */
public record CategoryDTO(
        Long id,
        String name,
        boolean active
) {
}
