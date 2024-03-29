package com.ryan.stock.service;

import com.ryan.stock.domain.Stock;
import com.ryan.stock.repository.StockRepository;
import org.springframework.stereotype.Service;

@Service
public class StockService {
    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public synchronized void decrease(Long id, Long quantity){
        /**
         * 재고 조회
         * 재고를 감소시킨 뒤
         * 갱신된 값을 저장하도록 한다.
         */
        Stock stock = stockRepository.findById(id).orElseThrow();
        stock.decrease(quantity);

        stockRepository.saveAndFlush(stock);
    }

}
