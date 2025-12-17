import memory.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;


class SharedMemoryTest {

    private static final double DELTA = 0.0001;

    // ==========================================
    //            LOGIC TESTS
    // ==========================================

    @Test
    void testVectorInit() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        assertEquals(1.0, v.get(0), DELTA, "Index 0 incorrect");
        assertEquals(3.0, v.get(2), DELTA, "Index 2 incorrect");
        assertEquals(3, v.length(), "Length incorrect");
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation(), "Orientation wrong");
    }

    @Test
    void testVectorTranspose() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation(), "Should be COLUMN after transpose");

        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation(), "Should be ROW after 2nd transpose");
    }

    @Test
    void testVectorAdd() {
        SharedVector v1 = new SharedVector(new double[]{10, 20}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        v1.add(v2);

        assertEquals(11.0, v1.get(0), DELTA, "Add index 0 failed");
        assertEquals(22.0, v1.get(1), DELTA, "Add index 1 failed");

        // Verify v2 is unmodified
        assertEquals(1.0, v2.get(0), DELTA, "Source vector modified erroneously");
    }

    @Test
    void testVectorNegate() {
        SharedVector v = new SharedVector(new double[]{1, -2}, VectorOrientation.ROW_MAJOR);
        v.negate();

        assertEquals(-1.0, v.get(0), DELTA, "Negate failed for pos");
        assertEquals(2.0, v.get(1), DELTA, "Negate failed for neg");
    }

    @Test
    void testVectorDotProduct() {
        // Dot product: Row * Column
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.COLUMN_MAJOR);

        double res = v1.dot(v2);
        // 1*3 + 2*4 = 11
        assertEquals(11.0, res, DELTA, "Dot product calculation wrong");
    }

    @Test
    void testVectorErrors() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector v3 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR); // Same orientation

        // Test Length Mismatch
        Exception lenEx = assertThrows(Exception.class, () -> v1.add(v2),
                "Should throw exception on length mismatch");
        // Optional: Check message content if needed
        // assertTrue(lenEx.getMessage().contains("length"));

        // Test Orientation Mismatch (dot product requires orthogonal vectors)
        Exception orientEx = assertThrows(Exception.class, () -> v1.dot(v3),
                "Should throw exception on same orientation for dot product");
    }

    @Test
    void testMatrixInit() {
        double[][] data = {{1, 2}, {3, 4}};
        SharedMatrix m = new SharedMatrix(data);

        assertEquals(2, m.length(), "Matrix rows count wrong");

        double[][] read = m.readRowMajor();
        assertEquals(1.0, read[0][0], DELTA, "Matrix read [0][0] incorrect");
        assertEquals(4.0, read[1][1], DELTA, "Matrix read [1][1] incorrect");
    }

    @Test
    void testMatrixLoadColumnMajor() {
        // Data:
        // [1, 2]
        // [3, 4]
        double[][] data = {{1, 2}, {3, 4}};
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(data);

        // Expected Transpose effect when reading back as RowMajor:
        // [1, 3]
        // [2, 4]
        double[][] read = m.readRowMajor();

        assertEquals(1.0, read[0][0], DELTA, "Transposed [0][0] wrong");
        assertEquals(2.0, read[0][1], DELTA, "Transposed [0][1] wrong");
        assertEquals(3.0, read[1][0], DELTA, "Transposed [1][0] wrong");
        assertEquals(4.0, read[1][1], DELTA, "Transposed [1][1] wrong");
    }

    @Test
    void testVecMatMul() {
        // Vector (Row): [1, 2]
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        // Matrix (loaded as columns):
        // Col 1: [3, 4]
        // Col 2: [5, 6]
        // Logical Matrix:
        // | 3 5 |
        // | 4 6 |
        double[][] matData = {{3, 4}, {5, 6}};
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(matData);

        // v * M = [ (1,2)*(3,5), (1,2)*(4,6) ] = [ 13, 16]
        v.vecMatMul(m);

        assertEquals(13.0, v.get(0), DELTA, "VecMatMul index 0 failed");
        assertEquals(16.0, v.get(1), DELTA, "VecMatMul index 1 failed");
    }

    // ==========================================
    //          CONCURRENCY TESTS
    // ==========================================

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS) // Fail if deadlocked
    void testConcurrentVectorAdd() throws InterruptedException {
        int threadCount = 1000;
        SharedVector target = new SharedVector(new double[]{0}, VectorOrientation.ROW_MAJOR);
        SharedVector adder = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);

        ExecutorService es = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            es.submit(() -> {
                try {
                    target.add(adder);
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean done = latch.await(5, TimeUnit.SECONDS);
        es.shutdown();

        assertTrue(done, "Timeout waiting for threads in ConcurrentAdd");
        assertEquals(0, errors.get(), "Exceptions occurred during concurrent add");
        assertEquals(1000.0, target.get(0), DELTA, "Concurrency Add Failed! Possible Race Condition.");
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testConcurrentVecMatMul() throws InterruptedException {
        int threadCount = 20;
        // Matrix: Columns of [1,1]
        double[][] matData = {{1, 1}, {1, 1}};
        SharedMatrix sharedM = new SharedMatrix();
        sharedM.loadColumnMajor(matData);

        ExecutorService es = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            es.submit(() -> {
                try {
                    // Each thread creates its own vector [2,2]
                    SharedVector myVec = new SharedVector(new double[]{2, 2}, VectorOrientation.ROW_MAJOR);
                    // [2,2] * [1,1] = 4
                    myVec.vecMatMul(sharedM);

                    if (Math.abs(myVec.get(0) - 4.0) > DELTA || Math.abs(myVec.get(1) - 4.0) > DELTA) {
                        failures.incrementAndGet();
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean done = latch.await(5, TimeUnit.SECONDS);
        es.shutdown();

        assertTrue(done, "Timeout in ConcurrentVecMatMul");
        assertEquals(0, failures.get(), "Some threads failed in VecMatMul concurrency test");
    }
}