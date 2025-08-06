# Java 多執行緒計數安全問題與 AtomicInteger / LongAdder / synchronized int / 普通變數 原理與範例

## 普通變數的問題 (`count++`)
`count++` 在 JVM 中不是原子操作，會被拆成三個步驟：

1. **讀取變數值**
   ```java
   temp = count;
   ```
2. **計算加 1**
   ```java
   temp = temp + 1;
   ```
3. **寫回變數**
   ```java
   count = temp;
   ```

### 多執行緒下的競爭條件 (Race Condition)
假設 `count = 5`，兩個執行緒同時執行：
1. **執行緒 A** 讀到 `count = 5`
2. **執行緒 B** 也讀到 `count = 5`
3. A、B 都各自計算 `temp = 6`
4. A 寫回 `count = 6`
5. B 寫回 `count = 6`（覆蓋 A 的結果）

> **正確結果應該是 7，但實際結果是 6**  
> 這就是 **Race Condition（競爭條件）**。

---

## AtomicInteger 的解決方式

### 核心原理：CAS（Compare-And-Set）
`AtomicInteger` 內部使用 **CAS 原子操作** 來保證更新的正確性：

1. **讀取當前值**（假設是 5）
2. **計算新值**（5 + 1 = 6）
3. **比較當前值是否仍然是 5**
    - **如果是** → 寫入新值 6（更新成功）
    - **如果不是**（代表其他執行緒已更新過） → **重試**，重新讀取再計算

> 因為比較與更新是 **一次 CPU 原子指令** 完成，所以不會發生 Race Condition。

---

## 為什麼 Atomic 比 synchronized 快
- `synchronized`：進入/退出鎖有開銷，且會阻塞其他執行緒
- `AtomicInteger`：使用 CAS 無鎖（Lock-Free）方式，失敗才重試，不會阻塞

---

## 實務應用場景
- **計數器**（統計 API 請求數、線上人數）
- **序號生成**（流水號、訂單號）
- **併發限制**（排隊限制、限流器）
- **任務進度追蹤**（已完成任務數）

---

## 範例

### 普通變數（不安全）
```java
private static int count = 0;

public static void increment() {
    count++; // 非原子操作，可能導致錯誤結果
}
```

### AtomicInteger（安全）
```java
import java.util.concurrent.atomic.AtomicInteger;

private static AtomicInteger count = new AtomicInteger(0);

public static void increment() {
    count.incrementAndGet(); // CAS 原子操作，保證正確性
}
```

---

## LongAdder

### 介紹
- **用途**：在超高併發「累加統計」的情境下，比 `AtomicInteger` 更快
- **原因**：
    - `AtomicInteger` 在高衝突時會大量 CAS 重試
    - `LongAdder` 會把計數拆成多個變數（cell），不同執行緒更新不同 cell，最後再求和 → 降低衝突
- **缺點**：不適合用來做限流（不能準確即時取得當下精確值）

### 範例
```java
import java.util.concurrent.atomic.LongAdder;

public class LongAdderExample {
    private static final LongAdder counter = new LongAdder();

    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                counter.increment(); // 無鎖累加
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("LongAdder 累加結果：" + counter.sum()); // 準確值
    }
}
```

---

## synchronized int

### 介紹
- **用途**：保證多執行緒更新普通 `int` 的正確性
- **原理**：使用 Java 內建鎖機制，確保同一時間只有一個執行緒可以進入 `synchronized` 區塊
- **缺點**：有鎖開銷，效能比 `AtomicInteger` 慢很多

### 範例
```java
public class SynchronizedIntExample {
    private static int counter = 0;

    public static synchronized void increment() {
        counter++;
    }

    public static synchronized int getCounter() {
        return counter;
    }

    public static void main(String[] args) throws InterruptedException {
        Runnable task = () -> {
            for (int i = 0; i < 1000; i++) {
                increment();
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("synchronized int 結果：" + getCounter());
    }
}
```

---

## 四者比較表

| 類型 | 執行緒安全 | 原理 | 適合場景 | 優點 | 缺點 |
|------|------------|------|----------|------|------|
| **普通變數 (int)** | ❌ | 普通讀寫 | 單執行緒 | 無鎖，速度最快 | 多執行緒會發生 Race Condition，結果錯誤 |
| **AtomicInteger** | ✅ | CAS 原子操作 | 高併發計數、限流 | 無鎖、高效 | 高衝突下 CAS 重試開銷 |
| **LongAdder** | ✅ | 分段計數（cell）+ 最後求和 | 超高併發統計（QPS、PV） | 幾乎無鎖衝突 | 即時精確值不保證，不適合限流 |
| **synchronized int** | ✅ | JVM 內建鎖 | 任意複雜邏輯保護 | 寫法簡單、適合複雜邏輯 | 有鎖阻塞，效能最差 |

---

## 額外特性對比

| 特性 | 普通變數 (int) | `AtomicInteger` | `synchronized` int | `LongAdder` |
|------|----------------|----------------|--------------------|-------------|
| **適合場景** | 單執行緒 | 計數、限流 | 任意複雜邏輯 | 高併發統計 |
| **嚴格即時精確性** | ❌ | ✅ | ✅ | ❌（近似值） |
| **效能** | 最高 | 高 | 中 | 高（高併發累加最快） |
| **鎖機制** | 無 | 無鎖（CAS） | JVM 內建鎖 | 分段計數，幾乎無鎖衝突 |

---

## 結論
- `count++` 在多執行緒下會有 Race Condition
- `AtomicInteger` 用 CAS 保證原子性，避免資料競爭
- `LongAdder` 適合極高併發的統計場景，但不適合做限流
- `synchronized int` 適合需要保護複雜邏輯的情境，但效能最差
- 普通變數僅適合單執行緒，否則結果不可靠
