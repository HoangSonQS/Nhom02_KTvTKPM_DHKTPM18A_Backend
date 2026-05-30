package iuh.fit.se.modules.ai.application.port.out;

import iuh.fit.se.modules.ai.application.port.out.CatalogBookPort.BookContext;

import java.util.List;

public interface SalesRankingPort {

    List<BookContext> getTopSellingBooks(int limit);
}
