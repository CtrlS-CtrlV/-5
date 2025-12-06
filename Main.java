import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class Resource {
    private final String name;
    public final Lock lock = new ReentrantLock();

    public Resource(String name) { this.name = name; }
    public String getName() { return name; }
}

public class Main {
    private static final Map<Integer, Long> resultsMap = new ConcurrentHashMap<>();
    private static final List<String> eventLog = new CopyOnWriteArrayList<>();
    
    private static final AtomicLong totalProcessedTime = new AtomicLong(0);

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        System.out.println("=== PART 1: Multithreaded Processing (Callable/Future) ===");
        runProcessingBenchmark();

        System.out.println("\n=== PART 2: Deadlock Simulation & Fix ===");
        runDeadlockScenario();
    }

    public static void runProcessingBenchmark() throws InterruptedException, ExecutionException {
        int tasksCount = 50; // Кількість задач
        int threadPoolSize = 4; // Кількість потоків

        // ExecutorService (Пул потоків)
        ExecutorService executor = Executors.newFixedThreadPool(threadPoolSize);
        List<Future<Long>> futures = new ArrayList<>();

        long startTime = System.currentTimeMillis();

        // Створення задач
        for (int i = 0; i < tasksCount; i++) {
            final int num = i + 10; // Обчислюємо факторіал для чисел від 10 до 60
            Callable<Long> task = () -> {
                long taskStart = System.nanoTime();
                long fact = calculateFactorial(num);
                
                // Симуляція роботи
                Thread.sleep(10); 
                
                long taskTime = System.nanoTime() - taskStart;
                totalProcessedTime.addAndGet(taskTime); // Atomic update
                eventLog.add("Task " + num + " done."); // CopyOnWriteArrayList
                
                return fact;
            };
            futures.add(executor.submit(task));
        }

        // Збір результатів
        for (int i = 0; i < tasksCount; i++) {
            try {
                Long result = futures.get(i).get();
                resultsMap.put(i, result); // ConcurrentHashMap put
            } catch (Exception e) {
                System.out.println("Error in task: " + e.getMessage());
            }
        }

        executor.shutdown();
        long endTime = System.currentTimeMillis();

        System.out.println("Total time: " + (endTime - startTime) + " ms");
        System.out.println("Processed Items: " + resultsMap.size());
        System.out.println("Atomic Total CPU Time (ns): " + totalProcessedTime.get());
        System.out.println("Log size (CopyOnWrite): " + eventLog.size());
    }
    private static long calculateFactorial(int n) {
        long result = 1;
        for (int i = 1; i <= n; i++) result *= i;
        return result;
    }

    public static void runDeadlockScenario() throws InterruptedException {
        Resource r1 = new Resource("Res-1");
        Resource r2 = new Resource("Res-2");

        Thread t1 = new Thread(() -> {
            // Uncomment next line to see DEADLOCK (program will hang)
            // unsafeOperation(r1, r2); 
            safeOperation(r1, r2); // Використовуємо безпечний метод
        });

        Thread t2 = new Thread(() -> {
            // Uncomment next line to see DEADLOCK
            // unsafeOperation(r2, r1); 
            safeOperation(r2, r1); // Використовуємо безпечний метод
        });

        t1.start(); t2.start();
        t1.join(); t2.join();
        System.out.println("Deadlock avoided successfully!");
    }
    private static void unsafeOperation(Resource from, Resource to) {
        from.lock.lock();
        try {
            Thread.sleep(50); // Чекаємо, щоб інший потік встиг заблокувати свій ресурс
            to.lock.lock();
            try {
                System.out.println("Unsafe Action: " + from.getName() + " -> " + to.getName());
            } finally {
                to.lock.unlock();
            }
        } catch (InterruptedException e) { e.printStackTrace(); } 
        finally { from.lock.unlock(); }
    }

    private static void safeOperation(Resource from, Resource to) {
        Resource first = from.getName().compareTo(to.getName()) < 0 ? from : to;
        Resource second = from.getName().compareTo(to.getName()) < 0 ? to : from;

        first.lock.lock();
        try {
            second.lock.lock();
            try {
                System.out.println("Safe Action: " + from.getName() + " -> " + to.getName());
            } finally {
                second.lock.unlock();
            }
        } finally {
            first.lock.unlock();
        }
    }
}
