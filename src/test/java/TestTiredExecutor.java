import scheduling.*;
import memory.*;
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

    // Executor Initialization Tests

    @Test
    @DisplayName("Create executor with multiple threads")
    void testMultipleThreadsExecutor() {
        executor = new TiredExecutor(10);
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

    // Basic Tasks Submission Tests

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

    // Basic Functionality Tests

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

    // Shutdown Tests
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

    //Fairness Tests

    private double extractFairnessScore(String report) {
        String marker = "Fairness Score (Variance): ";
        int start = report.indexOf(marker);
        if (start == -1) return -1;
        start += marker.length();
        int end = report.indexOf("\n", start);
        return Double.parseDouble(report.substring(start, end).trim());
    }


    @Test
    @DisplayName("Fairness with many small tasks")
    void testFairnessWithManySmallTasks() {
        executor = new TiredExecutor(4);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            tasks.add(() -> Math.sqrt(100));
        }
        
        executor.submitAll(tasks);
        
        String report = executor.getWorkerReport();
        double fairnessScore = extractFairnessScore(report);
        
        assertTrue(fairnessScore >= 0, "Fairness score should be non-negative");
        System.out.println("Fairness with 1000 small tasks: " + fairnessScore);
    }

    @Test
    @DisplayName("Fairness with few large tasks")
    void testFairnessWithFewLargeTasks() {
        executor = new TiredExecutor(4);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            tasks.add(() -> {
                for (int j = 0; j < 100000; j++) {
                    Math.sqrt(j);
                }
            });
        }
        
        executor.submitAll(tasks);
        
        String report = executor.getWorkerReport();
        double fairnessScore = extractFairnessScore(report);
        
        assertTrue(fairnessScore >= 0, "Fairness score should be non-negative");
        System.out.println("Fairness with 20 large tasks: " + fairnessScore);
    }

    @Test
    @DisplayName("Compare fairness with different worker counts")
    void testFairnessWithDifferentWorkerCounts() throws InterruptedException {
        // Test with 2 workers
        executor = new TiredExecutor(2);
        List<Runnable> tasks1 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks1.add(() -> {
                for (int j = 0; j < 10000; j++) {
                    Math.sqrt(j);
                }
            });
        }
        executor.submitAll(tasks1);
        String report2Workers = executor.getWorkerReport();
        double fairness2Workers = extractFairnessScore(report2Workers);
        executor.shutdown();
        executor = null;
        
        // Test with 4 workers
        executor = new TiredExecutor(4);
        List<Runnable> tasks2 = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks2.add(() -> {
                for (int j = 0; j < 10000; j++) {
                    Math.sqrt(j);
                }
            });
        }
        executor.submitAll(tasks2);
        String report4Workers = executor.getWorkerReport();
        double fairness4Workers = extractFairnessScore(report4Workers);
        
        System.out.println("Fairness with 2 workers: " + fairness2Workers);
        System.out.println("Fairness with 4 workers: " + fairness4Workers);
        
        assertTrue(fairness2Workers >= 0);
        assertTrue(fairness4Workers >= 0);
    }

    @Test
    @DisplayName("Fairness with matrix operation workload")
    void testFairnessWithMatrixWorkload() {
        executor = new TiredExecutor(4);
        double[][] data = new double[50][50];
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                data[i][j] = i * 50 + j;
            }
        }
        SharedMatrix matrix = new SharedMatrix(data);
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            final int row = i;
            tasks.add(() -> matrix.get(row).negate());
        }
        executor.submitAll(tasks);
        String report = executor.getWorkerReport();
        double fairnessScore = extractFairnessScore(report);
        
        assertTrue(fairnessScore >= 0);
        System.out.println("Fairness with 50 matrix row negate tasks: " + fairnessScore);
    }

    // ==========================================
    //        ERROR HANDLING TESTS
    // ==========================================


    // ==========================================
    //        MATRIX OPERATIONS TESTS
    // ==========================================

    private static final double DELTA = 0.0001;

    @Test
    @DisplayName("Execute matrix row negate tasks")
    void testMatrixRowNegateTasks() {
        executor = new TiredExecutor(4);
        
        double[][] data = new double[10][10];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                data[i][j] = i * 10 + j + 1;
            }
        }
        SharedMatrix matrix = new SharedMatrix(data);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < matrix.length(); i++) {
            final int row = i;
            tasks.add(() -> matrix.get(row).negate());
        }
        
        executor.submitAll(tasks);
        
        double[][] result = matrix.readRowMajor();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                assertEquals(-(i * 10 + j + 1), result[i][j], DELTA);
            }
        }
    }

    @Test
    @DisplayName("Execute matrix row addition tasks")
    void testMatrixRowAdditionTasks() {
        executor = new TiredExecutor(4);
        
        double[][] data1 = new double[20][5];
        double[][] data2 = new double[20][5];
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 5; j++) {
                data1[i][j] = i + j;
                data2[i][j] = 10;
            }
        }
        SharedMatrix m1 = new SharedMatrix(data1);
        SharedMatrix m2 = new SharedMatrix(data2);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < m1.length(); i++) {
            final int row = i;
            tasks.add(() -> m1.get(row).add(m2.get(row)));
        }
        
        executor.submitAll(tasks);
        
        double[][] result = m1.readRowMajor();
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 5; j++) {
                assertEquals(i + j + 10, result[i][j], DELTA);
            }
        }
    }

    @Test
    @DisplayName("Execute matrix multiplication row tasks (vecMatMul)")
    void testMatrixMultiplicationRowTasks() {
        executor = new TiredExecutor(4);
        
        // A = 5x5 matrix, B = 5x5 matrix (column major for multiplication)
        double[][] dataA = new double[5][5];
        double[][] dataB = new double[5][5];
        
        // A = all 1s, B = identity -> result should be all 1s
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                dataA[i][j] = 1.0;
            }
            dataB[i][i] = 1.0;
        }
        
        SharedMatrix A = new SharedMatrix(dataA);
        SharedMatrix B = new SharedMatrix();
        B.loadColumnMajor(dataB);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < A.length(); i++) {
            final int row = i;
            tasks.add(() -> A.get(row).vecMatMul(B));
        }
        
        executor.submitAll(tasks);
        
        double[][] result = A.readRowMajor();
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                assertEquals(1.0, result[i][j], DELTA);
            }
        }
    }

    @Test
    @DisplayName("Execute large matrix negate (50x50)")
    void testLargeMatrixNegateTasks() {
        executor = new TiredExecutor(8);
        
        int size = 50;
        double[][] data = new double[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                data[i][j] = i * size + j;
            }
        }
        SharedMatrix matrix = new SharedMatrix(data);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < matrix.length(); i++) {
            final int row = i;
            tasks.add(() -> matrix.get(row).negate());
        }
        
        executor.submitAll(tasks);
        
        double[][] result = matrix.readRowMajor();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                assertEquals(-(i * size + j), result[i][j], DELTA);
            }
        }
    }

    @Test
    @DisplayName("Execute matrix multiplication (10x10 * 10x10)")
    void testMatrixMultiplication10x10() {
        executor = new TiredExecutor(4);
        
        int size = 10;
        double[][] dataA = new double[size][size];
        double[][] dataB = new double[size][size];
        
        // A = matrix with values, B = identity
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                dataA[i][j] = i * size + j;
            }
            dataB[i][i] = 1.0;
        }
        
        SharedMatrix A = new SharedMatrix(dataA);
        SharedMatrix B = new SharedMatrix();
        B.loadColumnMajor(dataB);
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < A.length(); i++) {
            final int row = i;
            tasks.add(() -> A.get(row).vecMatMul(B));
        }
        
        executor.submitAll(tasks);
        
        // A * I = A
        double[][] result = A.readRowMajor();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                assertEquals(i * size + j, result[i][j], DELTA);
            }
        }
    }

    @Test
    @DisplayName("Execute vector operations in parallel")
    void testVectorOperationsInParallel() {
        executor = new TiredExecutor(4);
        
        SharedVector[] vectors = new SharedVector[20];
        for (int i = 0; i < 20; i++) {
            vectors[i] = new SharedVector(new double[]{i, i * 2, i * 3}, VectorOrientation.ROW_MAJOR);
        }
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            final int idx = i;
            tasks.add(() -> vectors[idx].negate());
        }
        
        executor.submitAll(tasks);
        
        for (int i = 0; i < 20; i++) {
            assertEquals(-i, vectors[i].get(0), DELTA);
            assertEquals(-i * 2, vectors[i].get(1), DELTA);
            assertEquals(-i * 3, vectors[i].get(2), DELTA);
        }
    }

    @Test
    @DisplayName("Execute vector dot product tasks")
    void testVectorDotProductTasks() {
        executor = new TiredExecutor(4);
        
        double[] results = new double[10];
        SharedVector[] rowVectors = new SharedVector[10];
        SharedVector[] colVectors = new SharedVector[10];
        
        for (int i = 0; i < 10; i++) {
            rowVectors[i] = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
            colVectors[i] = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.COLUMN_MAJOR);
        }
        
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            final int idx = i;
            tasks.add(() -> {
                results[idx] = rowVectors[idx].dot(colVectors[idx]);
            });
        }
        
        executor.submitAll(tasks);
        
        // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        for (int i = 0; i < 10; i++) {
            assertEquals(32.0, results[i], DELTA);
        }
    }

    @Test
    @DisplayName("Execute mixed matrix operations")
    void testMixedMatrixOperations() {
        executor = new TiredExecutor(4);
        
        // Create matrices for different operations
        SharedMatrix negateMatrix = new SharedMatrix(new double[][]{{1, 2}, {3, 4}});
        SharedMatrix addMatrix1 = new SharedMatrix(new double[][]{{10, 20}, {30, 40}});
        SharedMatrix addMatrix2 = new SharedMatrix(new double[][]{{1, 1}, {1, 1}});
        
        List<Runnable> tasks = new ArrayList<>();
        
        // Negate tasks
        for (int i = 0; i < negateMatrix.length(); i++) {
            final int row = i;
            tasks.add(() -> negateMatrix.get(row).negate());
        }
        
        // Add tasks
        for (int i = 0; i < addMatrix1.length(); i++) {
            final int row = i;
            tasks.add(() -> addMatrix1.get(row).add(addMatrix2.get(row)));
        }
        
        executor.submitAll(tasks);
        
        // Verify negate results
        double[][] negateResult = negateMatrix.readRowMajor();
        assertEquals(-1.0, negateResult[0][0], DELTA);
        assertEquals(-4.0, negateResult[1][1], DELTA);
        
        // Verify add results
        double[][] addResult = addMatrix1.readRowMajor();
        assertEquals(11.0, addResult[0][0], DELTA);
        assertEquals(41.0, addResult[1][1], DELTA);
    }

    @Test
    @DisplayName("Execute matrix operations with multiple batches")
    void testMatrixOperationsMultipleBatches() {
        executor = new TiredExecutor(4);
        
        SharedMatrix matrix = new SharedMatrix(new double[][]{
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        });
        
        // First batch: negate all rows
        List<Runnable> batch1 = new ArrayList<>();
        for (int i = 0; i < matrix.length(); i++) {
            final int row = i;
            batch1.add(() -> matrix.get(row).negate());
        }
        executor.submitAll(batch1);
        
        // Second batch: negate again (should return to original)
        List<Runnable> batch2 = new ArrayList<>();
        for (int i = 0; i < matrix.length(); i++) {
            final int row = i;
            batch2.add(() -> matrix.get(row).negate());
        }
        executor.submitAll(batch2);
        
        // Should be back to original
        double[][] result = matrix.readRowMajor();
        assertEquals(1.0, result[0][0], DELTA);
        assertEquals(5.0, result[1][1], DELTA);
        assertEquals(9.0, result[2][2], DELTA);
    }
}
