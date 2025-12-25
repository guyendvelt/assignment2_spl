import scheduling.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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

    @Nested
    @DisplayName("TiredExecutor Initialization Tests")
    class InitializationTests {

        @Test
        @DisplayName("Create executor with 1 thread")
        void testSingleThreadExecutor() throws InterruptedException {
            executor = new TiredExecutor(1);
            assertNotNull(executor);
        }

        @Test
        @DisplayName("Create executor with multiple threads")
        void testMultipleThreadsExecutor() throws InterruptedException {
            executor = new TiredExecutor(10);
            assertNotNull(executor);
        }

        @Test
        @DisplayName("Create executor with many threads")
        void testManyThreadsExecutor() throws InterruptedException {
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
    }

    // ==========================================
    //        TASK SUBMISSION TESTS
    // ==========================================

    @Nested
    @DisplayName("Task Submission Tests")
    class TaskSubmissionTests {

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Submit single task")
        void testSubmitSingleTask() throws InterruptedException {
            executor = new TiredExecutor(2);
            AtomicInteger counter = new AtomicInteger(0);
            
            List<Runnable> tasks = new ArrayList<>();
            tasks.add(() -> counter.incrementAndGet());
            
            executor.submitAll(tasks);
            
            assertEquals(1, counter.get());
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Submit multiple tasks")
        void testSubmitMultipleTasks() throws InterruptedException {
            executor = new TiredExecutor(4);
            AtomicInteger counter = new AtomicInteger(0);
            
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                tasks.add(() -> counter.incrementAndGet());
            }
            
            executor.submitAll(tasks);
            
            assertEquals(100, counter.get());
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Submit null task throws exception")
        void testSubmitNullTask() throws InterruptedException {
            executor = new TiredExecutor(2);
            assertThrows(IllegalArgumentException.class, () -> executor.submit(null));
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Submit tasks more than thread count")
        void testSubmitMoreTasksThanThreads() throws InterruptedException {
            executor = new TiredExecutor(3);
            AtomicInteger counter = new AtomicInteger(0);
            
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                tasks.add(() -> counter.incrementAndGet());
            }
            
            executor.submitAll(tasks);
            
            assertEquals(50, counter.get());
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Submit empty task list")
        void testSubmitEmptyTaskList() throws InterruptedException {
            executor = new TiredExecutor(2);
            List<Runnable> tasks = new ArrayList<>();
            executor.submitAll(tasks);
            // Should not throw and should complete immediately
        }
    }

    // ==========================================
    //        CONCURRENCY TESTS
    // ==========================================

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Tasks run in parallel")
        void testTasksRunInParallel() throws InterruptedException {
            executor = new TiredExecutor(4);
            AtomicInteger maxConcurrent = new AtomicInteger(0);
            AtomicInteger currentRunning = new AtomicInteger(0);
            
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tasks.add(() -> {
                    int running = currentRunning.incrementAndGet();
                    maxConcurrent.updateAndGet(max -> Math.max(max, running));
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    currentRunning.decrementAndGet();
                });
            }
            
            executor.submitAll(tasks);
            
            // At some point, multiple tasks should have run concurrently
            assertTrue(maxConcurrent.get() > 1, "Tasks did not run in parallel");
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("All tasks complete before submitAll returns")
        void testSubmitAllWaitsForCompletion() throws InterruptedException {
            executor = new TiredExecutor(4);
            AtomicInteger counter = new AtomicInteger(0);
            
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                tasks.add(() -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    counter.incrementAndGet();
                });
            }
            
            executor.submitAll(tasks);
            
            // All tasks should be complete after submitAll returns
            assertEquals(50, counter.get());
        }

        @Test
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        @DisplayName("Multiple submitAll calls work correctly")
        void testMultipleSubmitAllCalls() throws InterruptedException {
            executor = new TiredExecutor(4);
            AtomicInteger counter = new AtomicInteger(0);
            
            for (int batch = 0; batch < 5; batch++) {
                List<Runnable> tasks = new ArrayList<>();
                for (int i = 0; i < 20; i++) {
                    tasks.add(() -> counter.incrementAndGet());
                }
                executor.submitAll(tasks);
            }
            
            assertEquals(100, counter.get());
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Heavy workload stress test")
        void testHeavyWorkload() throws InterruptedException {
            executor = new TiredExecutor(8);
            AtomicInteger counter = new AtomicInteger(0);
            
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 1000; i++) {
                tasks.add(() -> {
                    // Some computation
                    double result = 0;
                    for (int j = 0; j < 100; j++) {
                        result += Math.sqrt(j);
                    }
                    // Use result to prevent optimization
                    if (result > 0) {
                        counter.incrementAndGet();
                    }
                });
            }
            
            executor.submitAll(tasks);
            
            assertEquals(1000, counter.get());
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Work distribution among threads")
        void testWorkDistribution() throws InterruptedException {
            executor = new TiredExecutor(4);
            
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                tasks.add(() -> {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            executor.submitAll(tasks);
            
            // Check worker report shows distribution
            String report = executor.getWorkerReport();
            assertNotNull(report);
            assertTrue(report.contains("Worker #"));
        }
    }

    // ==========================================
    //        SHUTDOWN TESTS
    // ==========================================

    @Nested
    @DisplayName("Shutdown Tests")
    class ShutdownTests {

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Shutdown with no tasks")
        void testShutdownNoTasks() throws InterruptedException {
            executor = new TiredExecutor(4);
            executor.shutdown();
            executor = null; // Prevent double shutdown in tearDown
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Shutdown after tasks complete")
        void testShutdownAfterTasks() throws InterruptedException {
            executor = new TiredExecutor(4);
            AtomicInteger counter = new AtomicInteger(0);
            
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tasks.add(() -> counter.incrementAndGet());
            }
            
            executor.submitAll(tasks);
            executor.shutdown();
            
            assertEquals(20, counter.get());
            executor = null; // Prevent double shutdown
        }
    }

    // ==========================================
    //        WORKER REPORT TESTS
    // ==========================================

    @Nested
    @DisplayName("Worker Report Tests")
    class WorkerReportTests {

        @Test
        @DisplayName("Worker report format")
        void testWorkerReportFormat() throws InterruptedException {
            executor = new TiredExecutor(3);
            
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                tasks.add(() -> {
                    try {
                        Thread.sleep(5);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
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
        void testWorkerReportStatistics() throws InterruptedException {
            executor = new TiredExecutor(2);
            
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                tasks.add(() -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            executor.submitAll(tasks);
            
            String report = executor.getWorkerReport();
            assertTrue(report.contains("Average Fatigue"));
            assertTrue(report.contains("Total Workers: 2"));
        }
    }

    // ==========================================
    //        FAIRNESS TESTS
    // ==========================================

    @Nested
    @DisplayName("Fairness Tests")
    class FairnessTests {

        @Test
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        @DisplayName("Work is distributed among workers")
        void testFairWorkDistribution() throws InterruptedException {
            executor = new TiredExecutor(4);
            
            List<Runnable> tasks = new ArrayList<>();
            for (int i = 0; i < 100; i++) {
                tasks.add(() -> {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
            
            executor.submitAll(tasks);
            
            String report = executor.getWorkerReport();
            // All workers should have done some work
            assertTrue(report.contains("Worker #0") && report.contains("Worker #1") 
                    && report.contains("Worker #2") && report.contains("Worker #3"));
        }
    }

    // ==========================================
    //        ERROR HANDLING TESTS
    // ==========================================

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Task throwing exception does not break executor")
        void testTaskExceptionHandling() throws InterruptedException {
            executor = new TiredExecutor(4);
            AtomicInteger successCounter = new AtomicInteger(0);
            
            List<Runnable> tasks = new ArrayList<>();
            
            // Add some tasks that throw exceptions
            for (int i = 0; i < 10; i++) {
                final int idx = i;
                tasks.add(() -> {
                    if (idx % 3 == 0) {
                        throw new RuntimeException("Test exception");
                    }
                    successCounter.incrementAndGet();
                });
            }
            
            // This should complete without hanging despite exceptions
            try {
                executor.submitAll(tasks);
            } catch (Exception e) {
                // Some exceptions might propagate
                System.out.println(e.getMessage());
            }
            
            // At least some tasks should have completed successfully
            assertTrue(successCounter.get() > 0);
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Continue after task exception")
        void testContinueAfterException() throws InterruptedException {
            executor = new TiredExecutor(2);
            AtomicInteger counter = new AtomicInteger(0);
            
            // First batch with exception
            List<Runnable> batch1 = new ArrayList<>();
            batch1.add(() -> { throw new RuntimeException("fail"); });
            
            try {
                executor.submitAll(batch1);
            } catch (Exception e) {
                // Expected
            }
            
            // Second batch should still work
            List<Runnable> batch2 = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                batch2.add(() -> counter.incrementAndGet());
            }
            
            executor.submitAll(batch2);
            
            assertEquals(10, counter.get());
        }
    }
}

