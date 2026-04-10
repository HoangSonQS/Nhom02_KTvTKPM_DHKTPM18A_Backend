package iuh.fit.se.modules.catalog.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * BookContent — Thành phần nội dung chi tiết của Sách.
 * Được quản lý bởi Aggregate Root Book.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BookContent {
    private String tableOfContents;
    private String excerpt;
}
