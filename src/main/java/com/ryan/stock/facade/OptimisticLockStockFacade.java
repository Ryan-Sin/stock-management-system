package com.ryan.stock.facade;

import com.ryan.stock.service.OptimisticLockStockService;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
public class OptimisticLockStockFacade {
    private final OptimisticLockStockService optimisticLockStockService;

    public OptimisticLockStockFacade(OptimisticLockStockService optimisticLockStockService) {
        this.optimisticLockStockService = optimisticLockStockService;
    }

    public void decrease(Long id, Long quantity) throws InterruptedException {
        while (true) {
            try {
                optimisticLockStockService.decrease(id, quantity);
                break;
            } catch (OptimisticLockingFailureException e) {
                Thread.sleep(100);
            }
        }
    }
}
