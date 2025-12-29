import scheduling.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestTiredExecutor {

    private TiredExecutor executor;

    @AfterEach
    void tearDown() throws InterruptedException {
        if (executor != null) {
            executor.shutdown();
        }
    }

    // ==========================================
    //        EXECUTOR INITIALIZATION TESTS
    // ==========================================

    @Test
    @DisplayName("Create executor with 1 thread")
    void testSingleThreadExecutor() {
        executor = new TiredExecutor(1);
        assertNotNull(executor);
    }

    @Test
    @DisplayName("Create executor with multiple threads")
    void testMultipleThreadsExecutor() {
        executor = new TiredExecutor(10);
        assertNotNull(executor);
    }

    @Test
    @DisplayName("Create executor with many threads")
    void testManyThreadsExecutor() {
        executor = new TiredExecutor(50);
        assertNotNull(executor);
    }

    @Test
    @DisplayName("Create executor with zero threads throws exception")
    void testZeroThreadsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TiredExecutor(0));
    }

    @Test
    @DisplayName("Create executor with negative threads throws exception")
    void testNegativeThreadsThrows() {
        assertThrows(IllegalArgumentException.class, () -> new TiredExecutor(-5));
    }

    // ==========================================
    //        TASK SUBMISSION TESTS
    // ==========================================

    @Test
    @DisplayName("Submit single task")
    void testSubmitSingleTask() {
        executor = new TiredExecutor(2);
        int[] counter = {0};
        
        List<Runnable> tasks = new ArrayList<>();
        tasks.add(() -> counter[0]++);
        
        executor.submitAll(tasks);
        
        assertEquals(1, counter[0]);
    }

    @Test
    @DisplayName("Submit multiple tasks")
    void testSubmitMultipleTasks() {
        executor = new TiredExecutor(4);
        int[] counter = {0};
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(() -> {
                synchronized (counter) {
                    counter[0]++;
                }
            });
        }
        
        executor.submitAll(tasks);
        
        assertEquals(100, counter[0]);
    }

    @Test
    @DisplayName("Submit null task throws exception")
    void testSubmitNullTask() {
        executor = new TiredExecutor(2);
        assertThrows(IllegalArgumentException.class, () -> executor.submit(null));
    }

    @Test
    @DisplayName("Submit tasks more than thread count")
    void testSubmitMoreTasksThanThreads() {
        executor = new TiredExecutor(3);
        int[] counter = {0};
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            tasks.add(() -> {
                synchronized (counter) {
                    counter[0]++;
                }
            });
        }
        
        executor.submitAll(tasks);
        
        assertEquals(50, counter[0]);
    }

    @Test
    @DisplayName("Submit empty task list")
    void testSubmitEmptyTaskList() {
        executor = new TiredExecutor(2);
        List<Runnable> tasks = new ArrayList<>();
        executor.submitAll(tasks);
    }

    // ==========================================
    //        BASIC FUNCTIONALITY TESTS
    // ==========================================

    @Test
    @DisplayName("All tasks complete before submitAll returns")
    void testSubmitAllWaitsForCompletion() {
        executor = new TiredExecutor(4);
        int[] counter = {0};
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            tasks.add(() -> {
                synchronized (counter) {
                    counter[0]++;
                }
            });
        }
        
        executor.submitAll(tasks);
        
        assertEquals(50, counter[0]);
    }

    @Test
    @DisplayName("Multiple submitAll calls work correctly")
    void testMultipleSubmitAllCalls() {
        executor = new TiredExecutor(4);
        int[] counter = {0};
        
        for (int batch = 0; batch < 5; batch++) {
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tasks.add(() -> {
                    synchronized (counter) {
                        counter[0]++;
                    }
                });
            }
            executor.submitAll(tasks);
        }
        
        assertEquals(100, counter[0]);
    }

    @Test
    @DisplayName("Heavy workload test")
    void testHeavyWorkload() {
        executor = new TiredExecutor(8);
        int[] counter = {0};
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            tasks.add(() -> {
                double result = 0;
                for (int j = 0; j < 100; j++) {
                    result += Math.sqrt(j);
                }
                if (result > 0) {
                    synchronized (counter) {
                        counter[0]++;
                    }
                }
            });
        }
        
        executor.submitAll(tasks);
        
        assertEquals(1000, counter[0]);
    }

    // ==========================================
    //        SHUTDOWN TESTS
    // ==========================================

    @Test
    @DisplayName("Shutdown with no tasks")
    void testShutdownNoTasks() throws InterruptedException {
        executor = new TiredExecutor(4);
        executor.shutdown();
        executor = null;
    }

    @Test
    @DisplayName("Shutdown after tasks complete")
    void testShutdownAfterTasks() throws InterruptedException {
        executor = new TiredExecutor(4);
        int[] counter = {0};
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> {
                synchronized (counter) {
                    counter[0]++;
                }
            });
        }
        
        executor.submitAll(tasks);
        executor.shutdown();
        
        assertEquals(20, counter[0]);
        executor = null;
    }

    // ==========================================
    //        WORKER REPORT TESTS
    // ==========================================

    @Test
    @DisplayName("Worker report format")
    void testWorkerReportFormat() {
        executor = new TiredExecutor(3);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tasks.add(() -> Math.sqrt(100));
        }
        
        executor.submitAll(tasks);
        
        String report = executor.getWorkerReport();
        assertTrue(report.contains("WORKER REPORT"));
        assertTrue(report.contains("Worker #0"));
        assertTrue(report.contains("Worker #1"));
        assertTrue(report.contains("Worker #2"));
        assertTrue(report.contains("Fatigue"));
        assertTrue(report.contains("Work Time"));
    }

    @Test
    @DisplayName("Worker report shows statistics")
    void testWorkerReportStatistics() {
        executor = new TiredExecutor(2);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> Math.sqrt(100));
        }
        
        executor.submitAll(tasks);
        
        String report = executor.getWorkerReport();
        assertTrue(report.contains("Average Fatigue"));
        assertTrue(report.contains("Total Workers: 2"));
    }

    // ==========================================
    //        FAIRNESS TESTS
    // ==========================================

    @Test
    @DisplayName("Work is distributed among workers")
    void testFairWorkDistribution() {
        executor = new TiredExecutor(4);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(() -> Math.sqrt(100));
        }
        
        executor.submitAll(tasks);
        
        String report = executor.getWorkerReport();
        assertTrue(report.contains("Worker #0") && report.contains("Worker #1") 
                && report.contains("Worker #2") && report.contains("Worker #3"));
    }

    // ==========================================
    //        ERROR HANDLING TESTS
    // ==========================================

    @Test
    @DisplayName("Task throwing exception does not break executor")
    void testTaskExceptionHandling() {
        executor = new TiredExecutor(4);
        int[] successCounter = {0};
        
        List<Runnable> tasks = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            tasks.add(() -> {
                if (idx % 3 == 0) {
                    throw new RuntimeException("Test exception");
                }
                synchronized (successCounter) {
                    successCounter[0]++;
                }
            });
        }
        
        try {
            executor.submitAll(tasks);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        
        assertTrue(successCounter[0] > 0);
    }

    @Test
    @DisplayName("Continue after task exception")
    void testContinueAfterException() {
        executor = new TiredExecutor(2);
        int[] counter = {0};
        
        List<Runnable> batch1 = new ArrayList<>();
        batch1.add(() -> { throw new RuntimeException("fail"); });
        
        try {
            executor.submitAll(batch1);
        } catch (Exception e) {
            // Expected
        }
        
        List<Runnable> batch2 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            batch2.add(() -> {
                synchronized (counter) {
                    counter[0]++;
                }
            });
        }
        
        executor.submitAll(batch2);
        
        assertEquals(10, counter[0]);
    }
}
