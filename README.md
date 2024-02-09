# 인프런 재고시스템 동시성 제어 강의 학습 프로젝트

## 학습 개요

---
MySQL 공식 문서와 Real MySQL 서적 그리고 여러 자료를 통해 잠금의 대한 개념을 학습을 했지만 실제 현업에서 제가 사용했던 방식은 비관적 동시성 제어를 통한 개발을 주로 작업을 했습니다. 
경합 충돌이 발생하는 상황 또는 성능적인 이점을 얻을 수 있는 부분의 있어서 더 올바른 기술을 사용해 문제 해결하는 능력을 키우고 싶었습니다. 
그래서 다음 학습을 진행했으며 실제 예시와 다르게 몇 가지 코드를 수정하고 테스트 코드를 추가적으로 작성했습니다.

## 목차

---
1. 재고시스템 비즈니스 로직
   1. 구현 및 문제점
   
2. 문제 해결방법
   1. Synchronized 키워드를 활용한 동시성 제어
   2. Database 잠금을 활용한 동시성 제어
   3. Redis 서버를 활용한 동시성 제어


## 구현 및 문제점

---
### 재고 시스템 비즈니스 로직
- 구현
  * [StockService](src/main/java/com/ryan/stock/service/StockService.java) 클래스 decrease 메서드를 통해 현재 재고 수량을 확인한 뒤 요청된 수량만큼 재고를 감소시킵니다. 
  * 실제 재고 감소 역할은 [Stock.java](src/main/java/com/ryan/stock/domain/Stock.java) 클래스에서 이루워집니다.
- 문제점
  * 테스트 코드 [StockServiceTest](src/test/java/com/ryan/stock/service/StockServiceTest.java) 파일의 작성했습니다.
  * 하나의 요청을 통해 재고를 감소하는 테스트와 동시 요청을 통해 재고를 감소하는 로직 테스트를 진행 했습니다.
  * 하나의 요청 테스트는 문제없이 진행을 완료했습니다. 하지만, 동시 요청 테스트는 실패 했습니다.
  * 원인은 race condition(경쟁 상태)문제가 발생했습니다. 간단하게 <U>**race condition은 공유 자원에 대해 여러 스레드가 동시에 접근을 시도할 때, 실행 순서에 따라 결과값이 달라지는 현상**</U>을 말합니다.

## 문제 해결방법

---
문제점을 해결하기 위해 java에서 제공되는 synchronized 키워드를 시작해 database 그리고 redis 서버를 활용해서 위 문제점을 해결했습니다.

### Synchronized 키워드를 사용해서 동시성 제어
기본적으로 멀티 스레드가 동시 자원을 접근하는 상황이 발생 했을 때 문제점을 해결하는 방법입니다.

[StockService](src/main/java/com/ryan/stock/service/StockService.java)
```java
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
``` 
재고를 감소하는 메서드 자체의 synchronized 키워드를 사용해서 스레드가 재고를 감소하기 전 잠금을 획득하고 해당 메서드를 수행할 수 있습니다.
잠금을 획득을 못한 스레드는 결국 대기 상태의 빠집니다.

<span style="color:red"> 주의 사항 </span><br/>

synchronized 키워드를 사용했을 때 발생하는 문제가 있습니다. 여러 서버에서 하나의 자원의 접근하게 되면 동일한 동시성 이슈가 발생합니다.
무슨말이냐면 하나의 서버에서 동시성을 처리 한다면 문제는 발생하지 않습니다. synchronized 키워드는 하나의 프로세스에서 멀티 스레드가 작업을 할 때 동시성 제어를 하는 기술입니다.
그렇기 때문에 여러 서버에서 하나의 DB에 접근하게 되면 동시성 이슈가 발생합니다.

### Database 잠금을 활용한 동시성 제어
데이터베이스에서는 여러 잠금을 제공합니다. 글로벌 락, 테이블 락, 네임드 락, 메타데이터 락, 레코드 락 등 여러가지가 존재 하지만
비즈니스 로직을 해결할 때 사용되는 방법은 <U>**비관적 잠금**</U>, <U>**낙관적 잠금**</U>, <U>**네임드 잠금**</U>을 활용해서 race condition 문제를 해결합니다.

1. Pessimistic Lock(비관적 잠금) [PessimisticLockStockService](src/main/java/com/ryan/stock/service/PessimisticLockStockService.java)
   * 실제로 테이블 레코드에 Lock(잠금)을 걸어서 정합성을 맞추는 방법입니다.
   * exclusive lock을 설정하게 되면 다른 트랜잭션에서는 lock이 해지되기전에 데이터를 가져갈 수 없게 됩니다.
   * 스레드가 데드락(교착상태)이 걸릴 수 있기 때문에 조심해야합니다.
   * 실제 SQL 구문으로는 FOR UPDATE를 사용해서 잠금을 획득 합니다.
     * ```sql
       SELECT * FROM stock WHERE id = 1 FOR UPDATE
       ```

2. Optimistic Lock(낙관적 잠금) [OptimisticLockStockFacade](src/main/java/com/ryan/stock/facade/OptimisticLockStockFacade.java)
   * 실제로 Lock을 이용하지 않고 버전을 이용함으로써 정합성을 맞추는 방법입니다.
   * 먼저 데이터를 조회한 후 update를 수행할 때 현재 내가 읽은 버전이 맞는지 확인하며 업데이트를 합니다.
   * 업데이트 충돌이 발생하거나 읽은 버전에서 이미 다른 트랜잭션에서 수정사항이 생겼다면 재시도 로직을 구현해야합니다.
   

3. Named Lock(네임드 잠금) [NamedLockStockFacade](src/main/java/com/ryan/stock/facade/NamedLockStockFacade.java)
   * 네임드 락은 실제 테이블 또는 레코드에 잠금을 설정하는게 아닌 메타데이터 락 방식을 사용합니다.
   * 데이터베이스 별도의 공간에 잠금을 설정해서 사용합니다.
   * 주의사항으로는 transaction 이 종료될 때 lock을 자동으로 해제되지 않습니다. (rollback X)
   * 잠금을 획득하는 방식은 get_lock(key, time) 함수를 사용해서 잠금을 획득하며, 잠금 해제는 release_lock(key)을 사용합니다.
     * ```sql
        SELECT get_lock('ryan', 1000) # 잠금 획득
        SELECT release_lock('ryan') # 잠금 해제
       ```

<span style="color:red"> 주의 사항 </span><br/>

동시성 요구사항이 크다면 **비관적 잠금**을 사용하는 걸 추천드립니다. 동시성을 중요하게 생각하는 사례는 0 또는 1이라는 값의 정합성을 중요시 하는 서비스입니다.
예를들어 <U>영화, 버스, 기차, 팬션</U> 등과 같은 좌석 예매 서비스는 동시성을 중요하게 생각합니다.

그럼 반대로 낙관적 동시성 제어를 활용하면 좋은 서비스는 재고 관련 된 수량을 차감하는 서비스에 적합합니다.
왜냐하면 결과적으로 데이터는 수량이 0미만, -(마이너스)가 되지 않으면 문제가 발생하지 않습니다. 

하지만 아쉽게도 낙관적 잠금을 어플리케이션 레벨에서 재시도 로직을 구현 해야합니다.
비즈니스 로직 사항이 많이 복잡하고 DB Update 충돌이 많이 발생해서 성능저하로 이어진다면 오히려 **비관적 잠금**을 추천드리겠습니다.

### Redis 잠금을 활용한 동시성 제어
Redis 서버를 활용해서 분산 잠금을 구현할 수 있습니다. 
대표적으로 스핀락(Spin Lock)또는 pub/sub 구조의 잠금을 구현할 수 있습니다. 클라이언트 라이브러리로 Lettuce와 Redisson을 사용됩니다.

Lettuce 클라이언트는 **implementation 'org.springframework.boot:spring-boot-starter-data-redis'** 라이브러리를 등록해서 사용할 수 있습니다.
Redisson 클라이언트는 **implementation 'org.redisson:redisson-spring-boot-starter:3.26.0'** 라이브러리를 등록해서 사용할 수 있습니다.

Lettuce 클라이언트를 활용한 스핀락을 구현 로직은 다음과 같습니다.

비즈니스 로직에서 락을 획득할 때까지 재시도를 진행합니다. (**획득하는 시간만큼 스레드는 대기를 합니다.**) 락을 획득하면 비즈니스 로직을 수행하고 로직이 끝나면 락을 반납합니다.

[LettuceLockStockFacade](src/main/java/com/ryan/stock/facade/LettuceLockStockFacade.java)
```java
public void decrease(Long id, Long quantity) throws InterruptedException {
    while (!redisLockRepository.lock(id)) {
        Thread.sleep(100);
    }

    try {
        stockService.decrease(id, quantity);
    } finally {
        redisLockRepository.unlock(id);
    }

}
```
[RedisLockRepository](src/main/java/com/ryan/stock/repository/RedisLockRepository.java)
```java
public Boolean lock(Long key) {
    return redisTemplate
    .opsForValue()
    .setIfAbsent(generateKey(key), "lock", Duration.ofMillis(3000));
}

public Boolean unlock(Long key) {
    return redisTemplate.delete(generateKey(key));
}

private String generateKey(Long key) {
    return key.toString();
}
```
Redis 명령어는 다음과 같습니다. setnx key value 형태로 잠금을 획득합니다. del ryan 명령어를 사용해서 잠금을 해제 합니다.
```shell
    setnx ryan lock # 락 획득
    del ryan # 락 해재
```

Redisson 클라이언트를 활용한 분산 락 구현 로직은 다음과 같습니다.

[RedissonLockStockFacade](src/main/java/com/ryan/stock/facade/RedissonLockStockFacade.java)
```java
public void decrease(Long id, Long quantity) {
    RLock lock = redissonClient.getLock(id.toString());

    try {
        //waitTime: 몇 초동안 락 획득을 시도할 시간(10초), leaseTime: 몇 초동안 락을 점유할 시간(1초)
        boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);

        if (!available) {
            System.out.println("Lock 획득 실패");
            return;
        }

        stockService.decrease(id, quantity);
    } catch (InterruptedException e) {
        throw new RuntimeException();
    } finally {
        lock.unlock();
    }
}
```

Redisson 클라이언트는 기본적으로 내장 함수를 바로 사용할 수 있습니다.

Redis 명령어는 다음과 같습니다. setnx key value 형태로 잠금을 획득합니다. del ryan 명령어를 사용해서 잠금을 해제 합니다.
```shell
    subscribe ryan # 채널 생성
    publish ryan hello # 채널 메시지 전송
```

<span style="color:red"> 주의 사항 </span><br/>

Lettuce 클라이언트를 사용해서 분산 락을 구현했을 때 주의사항이 있습니다.
1. Lock의 타임아웃을 지정할 수 없습니다. 그렇다는 건 락을 획득하지 못하면 무한 루프를 돌게 됩니다.
2. 무한 루프를 실행 한다면 결국 Redis에 부하가 발생하게 됩니다. 만약 Redis 서버가 죽게 되면... 서비스 큰 장애가 발생합니다.


락을 획득하는 재시도 기능이 필요하지 않다면 Lettuce 클라이언트를 활용해서 분산 락 시스템을 개발하는 걸 추천드립니다.
그렇지 않고 재시도가 필요한 경우 Redisson 클라이언트를 활용해서 분산 락 시스템을 개발하는 걸 추천드립니다.
