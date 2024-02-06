package com.ryan.stock.service;

import com.ryan.stock.domain.Stock;
import com.ryan.stock.repository.StockRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StockServiceTest {

    @Autowired
    private StockService stockService;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    public void beforeEach() {
        stockRepository.saveAndFlush(new Stock(1L, 100L));
    }

    @AfterEach
    public void afterEach() {
        stockRepository.deleteAll();
    }

    @Test
    public void 재고감소() {
        // given(준비): 어떠한 데이터가 준비되었을 때
        stockService.decrease(1L, 1L);

        // when(실행): 어떠한 함수를 실행하면
        Stock stock = stockRepository.findById(1L).orElseThrow();

        // then(검증): 어떠한 결과가 나와야 한다.
        assertEquals(99, stock.getQuantity());
    }
}