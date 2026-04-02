package iuh.fit.se.modules.order.adapter.outbound.internal;

import iuh.fit.se.modules.inventory.application.port.in.InventoryInternalUseCase;
import iuh.fit.se.modules.order.application.port.out.InventoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InternalInventoryAdapter implements InventoryPort {

    private final InventoryInternalUseCase inventoryUseCase;

    @Override
    public void decreaseStockBulk(List<StockItem> items, String referenceId) {
        List<InventoryInternalUseCase.StockItemRequest> requests = items.stream()
                .map(item -> InventoryInternalUseCase.StockItemRequest.builder()
                        .bookId(item.getBookId())
                        .amount(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        inventoryUseCase.decreaseStockBulk(requests, referenceId);
    }

    @Override
    public void increaseStockBulk(List<StockItem> items, String referenceId) {
        List<InventoryInternalUseCase.StockItemRequest> requests = items.stream()
                .map(item -> InventoryInternalUseCase.StockItemRequest.builder()
                        .bookId(item.getBookId())
                        .amount(item.getQuantity())
                        .build())
                .collect(Collectors.toList());

        inventoryUseCase.increaseStockBulk(requests, referenceId);
    }
}
