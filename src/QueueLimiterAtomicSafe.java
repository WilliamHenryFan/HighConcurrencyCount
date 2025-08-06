import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class QueueLimiterAtomicSafe {
    private static final int MAX_QUEUE = 10;
    private static final AtomicInteger currentQueue = new AtomicInteger(0);

    public static boolean tryEnter(int id) {
        while (true) {
            int current = currentQueue.get();
            if (current >= MAX_QUEUE) {
                System.out.printf("❌ %d 排隊已滿（目前人數：%d）%n", id, current);
                return false;
            }
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
            // CAS 原子操作，保證同一時間只有一個 Thread 可以更新成功
            if (currentQueue.compareAndSet(current, current + 1)) {
                System.out.printf("✅ %d 進入排隊（目前人數：%d）%n", id, current + 1);
                return true;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // 計時開始
        LocalDateTime startTime = LocalDateTime.now();
        System.out.println("開始時間：" + startTime);


        int totalPeople = 100;
        Thread[] threads = new Thread[totalPeople];

        for (int i = 0; i < totalPeople; i++) {
            int id = i;
            threads[i] = new Thread(() -> tryEnter(id));
        }

        for (Thread t : threads) t.start();
        for (Thread t : threads) t.join();

        System.out.println("【Atomic】最終人數：" + currentQueue.get());

        // 計時結束
        LocalDateTime endTime = LocalDateTime.now();
        System.out.println("結束時間：" + endTime);
        long duration = java.time.Duration.between(startTime, endTime).toMillis();
        System.out.println("總耗時：" + duration + " 毫秒");
    }
}
