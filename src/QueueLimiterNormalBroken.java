import java.time.LocalDateTime;

public class QueueLimiterNormalBroken {
    private static final int MAX_QUEUE = 10;
    private static int currentQueue = 0;

    public static boolean tryEnter(int id) {
        if (currentQueue >= MAX_QUEUE) {
            System.out.printf("❌ %d 排隊已滿（目前人數：%d）%n", id, currentQueue);
            return false;
        }
        try { Thread.sleep(1); } catch (InterruptedException ignored) {}
        currentQueue++;
        System.out.printf("✅ %d 進入排隊（目前人數：%d）%n", id, currentQueue);
        return true;
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

        System.out.println("【普通變數】最終人數：" + currentQueue);

        // 計時結束
        LocalDateTime endTime = LocalDateTime.now();
        System.out.println("結束時間：" + endTime);
        long duration = java.time.Duration.between(startTime, endTime).toMillis();
        System.out.println("總耗時：" + duration + " 毫秒");
    }
}
