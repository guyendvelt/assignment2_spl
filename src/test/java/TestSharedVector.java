import memory.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CyclicBarrier;

import static org.junit.jupiter.api.Assertions.*;

class TestSharedVector {

    private static final double DELTA = 0.0001;
    //        BASIC OPERATIONS TESTS
    @Nested
    @DisplayName("SharedVector Basic Operations")
    class BasicOperationsTests {

        @Test
        @DisplayName("Vector initialization with ROW_MAJOR orientation")
        void testVectorInitRowMajor() {
            SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
            assertEquals(1.0, v.get(0), DELTA, "Index 0 incorrect");
            assertEquals(2.0, v.get(1), DELTA, "Index 1 incorrect");
            assertEquals(3.0, v.get(2), DELTA, "Index 2 incorrect");
            assertEquals(3, v.length(), "Length incorrect");
            assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation(), "Orientation wrong");
        }

        @Test
        @DisplayName("Vector initialization with COLUMN_MAJOR orientation")
        void testVectorInitColumnMajor() {
            SharedVector v = new SharedVector(new double[]{4, 5, 6, 7}, VectorOrientation.COLUMN_MAJOR);
            assertEquals(4.0, v.get(0), DELTA);
            assertEquals(7.0, v.get(3), DELTA);
            assertEquals(4, v.length());
            assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        }

        @Test
        @DisplayName("Single element vector")
        void testSingleElementVector() {
            SharedVector v = new SharedVector(new double[]{42.5}, VectorOrientation.ROW_MAJOR);
            assertEquals(42.5, v.get(0), DELTA);
            assertEquals(1, v.length());
        }

        @Test
        @DisplayName("Large vector initialization")
        void testLargeVectorInit() {
            double[] largeData = new double[10000];
            for (int i = 0; i < largeData.length; i++) {
                largeData[i] = i * 1.5;
            }
            SharedVector v = new SharedVector(largeData, VectorOrientation.ROW_MAJOR);
            assertEquals(10000, v.length());
            assertEquals(0.0, v.get(0), DELTA);
            assertEquals(9999 * 1.5, v.get(9999), DELTA);
            assertEquals(5000 * 1.5, v.get(5000), DELTA);
        }

        @Test
        @DisplayName("Vector with negative values")
        void testVectorWithNegatives() {
            SharedVector v = new SharedVector(new double[]{-1, -2.5, 0, 3.7, -100}, VectorOrientation.ROW_MAJOR);
            assertEquals(-1.0, v.get(0), DELTA);
            assertEquals(-2.5, v.get(1), DELTA);
            assertEquals(0.0, v.get(2), DELTA);
            assertEquals(3.7, v.get(3), DELTA);
            assertEquals(-100.0, v.get(4), DELTA);
        }

        @Test
        @DisplayName("Vector with decimal precision")
        void testVectorDecimalPrecision() {
            SharedVector v = new SharedVector(new double[]{0.123456789, 1.111111111, 2.999999999}, VectorOrientation.ROW_MAJOR);
            assertEquals(0.123456789, v.get(0), DELTA);
            assertEquals(1.111111111, v.get(1), DELTA);
            assertEquals(2.999999999, v.get(2), DELTA);
        }
    }

    // ==========================================
    //        TRANSPOSE OPERATIONS TESTS
    // ==========================================

    @Nested
    @DisplayName("SharedVector Transpose Operations")
    class TransposeOperationsTests {

        @Test
        @DisplayName("Single transpose ROW to COLUMN")
        void testTransposeRowToColumn() {
            SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            v.transpose();
            assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        }

        @Test
        @DisplayName("Single transpose COLUMN to ROW")
        void testTransposeColumnToRow() {
            SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
            v.transpose();
            assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
        }

        @Test
        @DisplayName("Double transpose returns to original")
        void testDoubleTranspose() {
            SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            v.transpose();
            v.transpose();
            assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
        }

        @Test
        @DisplayName("Multiple transposes")
        void testMultipleTransposes() {
            SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
            for (int i = 0; i < 10; i++) {
                v.transpose();
            }
            // Even number of transposes should return to original
            assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());

            v.transpose();
            assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        }

        @Test
        @DisplayName("Transpose does not change data")
        void testTransposePreservesData() {
            SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
            v.transpose();
            assertEquals(1.0, v.get(0), DELTA);
            assertEquals(2.0, v.get(1), DELTA);
            assertEquals(3.0, v.get(2), DELTA);
            assertEquals(3, v.length());
        }
    }

    // ==========================================
    //        ADD OPERATIONS TESTS
    // ==========================================

    @Nested
    @DisplayName("SharedVector Add Operations")
    class AddOperationsTests {

        @Test
        @DisplayName("Basic addition of two row vectors")
        void testBasicAdd() {
            SharedVector v1 = new SharedVector(new double[]{10, 20}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            v1.add(v2);
            assertEquals(11.0, v1.get(0), DELTA);
            assertEquals(22.0, v1.get(1), DELTA);
        }

        @Test
        @DisplayName("Add does not modify source vector")
        void testAddDoesNotModifySource() {
            SharedVector v1 = new SharedVector(new double[]{10, 20}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            v1.add(v2);
            assertEquals(1.0, v2.get(0), DELTA);
            assertEquals(2.0, v2.get(1), DELTA);
        }

        @Test
        @DisplayName("Add column vectors")
        void testAddColumnVectors() {
            SharedVector v1 = new SharedVector(new double[]{5, 10, 15}, VectorOrientation.COLUMN_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.COLUMN_MAJOR);
            v1.add(v2);
            assertEquals(6.0, v1.get(0), DELTA);
            assertEquals(12.0, v1.get(1), DELTA);
            assertEquals(18.0, v1.get(2), DELTA);
        }

        @Test
        @DisplayName("Add with negative values")
        void testAddWithNegatives() {
            SharedVector v1 = new SharedVector(new double[]{-5, 10, -15}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{5, -10, 15}, VectorOrientation.ROW_MAJOR);
            v1.add(v2);
            assertEquals(0.0, v1.get(0), DELTA);
            assertEquals(0.0, v1.get(1), DELTA);
            assertEquals(0.0, v1.get(2), DELTA);
        }

        @Test
        @DisplayName("Add with zeros")
        void testAddWithZeros() {
            SharedVector v1 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{0, 0, 0}, VectorOrientation.ROW_MAJOR);
            v1.add(v2);
            assertEquals(1.0, v1.get(0), DELTA);
            assertEquals(2.0, v1.get(1), DELTA);
            assertEquals(3.0, v1.get(2), DELTA);
        }

        @Test
        @DisplayName("Add large vectors")
        void testAddLargeVectors() {
            int size = 5000;
            double[] data1 = new double[size];
            double[] data2 = new double[size];
            for (int i = 0; i < size; i++) {
                data1[i] = i;
                data2[i] = size - i;
            }
            SharedVector v1 = new SharedVector(data1, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(data2, VectorOrientation.ROW_MAJOR);
            v1.add(v2);
            for (int i = 0; i < size; i++) {
                assertEquals(size, v1.get(i), DELTA);
            }
        }

        @Test
        @DisplayName("Add throws on length mismatch")
        void testAddLengthMismatch() {
            SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
            assertThrows(IllegalArgumentException.class, () -> v1.add(v2));
        }

        @Test
        @DisplayName("Add throws on orientation mismatch")
        void testAddOrientationMismatch() {
            SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
            assertThrows(IllegalArgumentException.class, () -> v1.add(v2));
        }
    }

    // ==========================================
    //        NEGATE OPERATIONS TESTS
    // ==========================================

    @Nested
    @DisplayName("SharedVector Negate Operations")
    class NegateOperationsTests {

        @Test
        @DisplayName("Negate positive values")
        void testNegatePositives() {
            SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
            v.negate();
            assertEquals(-1.0, v.get(0), DELTA);
            assertEquals(-2.0, v.get(1), DELTA);
            assertEquals(-3.0, v.get(2), DELTA);
        }

        @Test
        @DisplayName("Negate negative values")
        void testNegateNegatives() {
            SharedVector v = new SharedVector(new double[]{-1, -2}, VectorOrientation.ROW_MAJOR);
            v.negate();
            assertEquals(1.0, v.get(0), DELTA);
            assertEquals(2.0, v.get(1), DELTA);
        }

        @Test
        @DisplayName("Negate mixed values")
        void testNegateMixed() {
            SharedVector v = new SharedVector(new double[]{1, -2, 0, 3.5, -4.5}, VectorOrientation.ROW_MAJOR);
            v.negate();
            assertEquals(-1.0, v.get(0), DELTA);
            assertEquals(2.0, v.get(1), DELTA);
            assertEquals(0.0, v.get(2), DELTA);
            assertEquals(-3.5, v.get(3), DELTA);
            assertEquals(4.5, v.get(4), DELTA);
        }

        @Test
        @DisplayName("Double negate returns original")
        void testDoubleNegate() {
            SharedVector v = new SharedVector(new double[]{1, -2, 3}, VectorOrientation.ROW_MAJOR);
            v.negate();
            v.negate();
            assertEquals(1.0, v.get(0), DELTA);
            assertEquals(-2.0, v.get(1), DELTA);
            assertEquals(3.0, v.get(2), DELTA);
        }

        @Test
        @DisplayName("Negate zeros")
        void testNegateZeros() {
            SharedVector v = new SharedVector(new double[]{0, 0, 0}, VectorOrientation.ROW_MAJOR);
            v.negate();
            assertEquals(0.0, v.get(0), DELTA);
            assertEquals(0.0, v.get(1), DELTA);
            assertEquals(0.0, v.get(2), DELTA);
        }

        @Test
        @DisplayName("Negate large vector")
        void testNegateLargeVector() {
            int size = 10000;
            double[] data = new double[size];
            for (int i = 0; i < size; i++) {
                data[i] = i - size / 2;
            }
            SharedVector v = new SharedVector(data, VectorOrientation.ROW_MAJOR);
            v.negate();
            for (int i = 0; i < size; i++) {
                assertEquals(-(i - size / 2), v.get(i), DELTA);
            }
        }
    }

    // ==========================================
    //        DOT PRODUCT OPERATIONS TESTS
    // ==========================================

    @Nested
    @DisplayName("SharedVector Dot Product Operations")
    class DotProductOperationsTests {

        @Test
        @DisplayName("Basic dot product")
        void testBasicDotProduct() {
            SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.COLUMN_MAJOR);
            double res = v1.dot(v2);
            // 1*3 + 2*4 = 11
            assertEquals(11.0, res, DELTA);
        }

        @Test
        @DisplayName("Dot product with zeros")
        void testDotProductWithZeros() {
            SharedVector v1 = new SharedVector(new double[]{0, 0, 0}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.COLUMN_MAJOR);
            assertEquals(0.0, v1.dot(v2), DELTA);
        }

        @Test
        @DisplayName("Dot product with negative values")
        void testDotProductWithNegatives() {
            SharedVector v1 = new SharedVector(new double[]{1, -2, 3}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{-1, 2, -3}, VectorOrientation.COLUMN_MAJOR);
            // 1*(-1) + (-2)*2 + 3*(-3) = -1 - 4 - 9 = -14
            assertEquals(-14.0, v1.dot(v2), DELTA);
        }

        @Test
        @DisplayName("Dot product result is scalar")
        void testDotProductScalar() {
            SharedVector v1 = new SharedVector(new double[]{2, 3, 4, 5}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1, 1, 1, 1}, VectorOrientation.COLUMN_MAJOR);
            // 2 + 3 + 4 + 5 = 14
            assertEquals(14.0, v1.dot(v2), DELTA);
        }

        @Test
        @DisplayName("Dot product with single element")
        void testDotProductSingleElement() {
            SharedVector v1 = new SharedVector(new double[]{5}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{7}, VectorOrientation.COLUMN_MAJOR);
            assertEquals(35.0, v1.dot(v2), DELTA);
        }

        @Test
        @DisplayName("Dot product large vectors")
        void testDotProductLargeVectors() {
            int size = 1000;
            double[] data1 = new double[size];
            double[] data2 = new double[size];
            double expected = 0;
            for (int i = 0; i < size; i++) {
                data1[i] = i;
                data2[i] = 1;
                expected += i;
            }
            SharedVector v1 = new SharedVector(data1, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(data2, VectorOrientation.COLUMN_MAJOR);
            assertEquals(expected, v1.dot(v2), DELTA);
        }

        @Test
        @DisplayName("Dot product throws on length mismatch")
        void testDotProductLengthMismatch() {
            SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.COLUMN_MAJOR);
            assertThrows(IllegalArgumentException.class, () -> v1.dot(v2));
        }

        @Test
        @DisplayName("Dot product throws when both row major")
        void testDotProductBothRowMajor() {
            SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            assertThrows(IllegalArgumentException.class, () -> v1.dot(v2));
        }

        @Test
        @DisplayName("Dot product throws when both column major")
        void testDotProductBothColumnMajor() {
            SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
            assertThrows(IllegalArgumentException.class, () -> v1.dot(v2));
        }

        @Test
        @DisplayName("Dot product throws when first is column major")
        void testDotProductFirstColumnMajor() {
            SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            assertThrows(IllegalArgumentException.class, () -> v1.dot(v2));
        }
    }

    // ==========================================
    //        VEC-MAT-MUL OPERATIONS TESTS
    // ==========================================

    @Nested
    @DisplayName("SharedVector VecMatMul Operations")
    class VecMatMulOperationsTests {

        @Test
        @DisplayName("Basic vector-matrix multiplication")
        void testBasicVecMatMul() {
            SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            double[][] matData = {{3, 4}, {5, 6}};
            SharedMatrix m = new SharedMatrix();
            m.loadColumnMajor(matData);
            v.vecMatMul(m);
            // v * M = [(1,2)·(3,5), (1,2)·(4,6)] = [13, 16]
            assertEquals(13.0, v.get(0), DELTA);
            assertEquals(16.0, v.get(1), DELTA);
        }

        @Test
        @DisplayName("VecMatMul with identity-like matrix")
        void testVecMatMulIdentity() {
            SharedVector v = new SharedVector(new double[]{5, 10}, VectorOrientation.ROW_MAJOR);
            double[][] matData = {{1, 0}, {0, 1}};
            SharedMatrix m = new SharedMatrix();
            m.loadColumnMajor(matData);
            v.vecMatMul(m);
            assertEquals(5.0, v.get(0), DELTA);
            assertEquals(10.0, v.get(1), DELTA);
        }

        @Test
        @DisplayName("VecMatMul with zeros")
        void testVecMatMulWithZeros() {
            SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
            double[][] matData = {{0, 0, 0}, {0, 0, 0}, {0, 0, 0}};
            SharedMatrix m = new SharedMatrix();
            m.loadColumnMajor(matData);
            v.vecMatMul(m);
            assertEquals(0.0, v.get(0), DELTA);
            assertEquals(0.0, v.get(1), DELTA);
            assertEquals(0.0, v.get(2), DELTA);
        }

        @Test
        @DisplayName("VecMatMul 3x3 matrix")
        void testVecMatMul3x3() {
            SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
            double[][] matData = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
            SharedMatrix m = new SharedMatrix();
            m.loadColumnMajor(matData);
            v.vecMatMul(m);
            // Column 0: [1,4,7] -> 1*1 + 2*4 + 3*7 = 1 + 8 + 21 = 30
            // Column 1: [2,5,8] -> 1*2 + 2*5 + 3*8 = 2 + 10 + 24 = 36
            // Column 2: [3,6,9] -> 1*3 + 2*6 + 3*9 = 3 + 12 + 27 = 42
            assertEquals(30.0, v.get(0), DELTA);
            assertEquals(36.0, v.get(1), DELTA);
            assertEquals(42.0, v.get(2), DELTA);
        }

        @Test
        @DisplayName("VecMatMul throws on null matrix")
        void testVecMatMulNullMatrix() {
            SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
            assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(null));
        }

        @Test
        @DisplayName("VecMatMul throws when vector is column major")
        void testVecMatMulColumnMajorVector() {
            SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
            double[][] matData = {{1, 2}, {3, 4}};
            SharedMatrix m = new SharedMatrix();
            m.loadColumnMajor(matData);
            assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
        }

        @Test
        @DisplayName("VecMatMul throws on dimension mismatch")
        void testVecMatMulDimensionMismatch() {
            SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
            double[][] matData = {{1, 2}, {3, 4}};
            SharedMatrix m = new SharedMatrix();
            m.loadColumnMajor(matData);
            assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
        }
    }

    // ==========================================
    //        EDGE CASE TESTS
    // ==========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Very small values")
        void testVerySmallValues() {
            SharedVector v = new SharedVector(new double[]{1e-10, 2e-10}, VectorOrientation.ROW_MAJOR);
            assertEquals(1e-10, v.get(0), 1e-15);
            assertEquals(2e-10, v.get(1), 1e-15);
        }

        @Test
        @DisplayName("Very large values")
        void testVeryLargeValues() {
            SharedVector v = new SharedVector(new double[]{1e10, 2e10}, VectorOrientation.ROW_MAJOR);
            assertEquals(1e10, v.get(0), 1e5);
            assertEquals(2e10, v.get(1), 1e5);
        }

        @Test
        @DisplayName("Mixed extreme values in add")
        void testAddExtremeValues() {
            SharedVector v1 = new SharedVector(new double[]{1e10, -1e10}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1e10, 1e10}, VectorOrientation.ROW_MAJOR);
            v1.add(v2);
            assertEquals(2e10, v1.get(0), 1e5);
            assertEquals(0.0, v1.get(1), 1e5);
        }

        @Test
        @DisplayName("Chained operations correctness")
        void testChainedOperationsCorrectness() {
            // Start with vector [1, 2, 3]
            // Negate: [-1, -2, -3]
            // Add [1, 1, 1]: [0, -1, -2]
            // Negate: [0, 1, 2]
            SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
            SharedVector addVec = new SharedVector(new double[]{1, 1, 1}, VectorOrientation.ROW_MAJOR);

            v.negate();
            v.add(addVec);
            v.negate();

            assertEquals(0.0, v.get(0), DELTA);
            assertEquals(1.0, v.get(1), DELTA);
            assertEquals(2.0, v.get(2), DELTA);
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
        @DisplayName("Concurrent vector add - race condition test")
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

            boolean done = latch.await(10, TimeUnit.SECONDS);
            es.shutdown();

            assertTrue(done, "Timeout waiting for threads");
            assertEquals(0, errors.get(), "Exceptions occurred during concurrent add");
            assertEquals(1000.0, target.get(0), DELTA, "Race condition detected in add!");
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Concurrent vector negate")
        void testConcurrentVectorNegate() throws InterruptedException {
            int threadCount = 100;
            SharedVector v = new SharedVector(new double[]{100}, VectorOrientation.ROW_MAJOR);

            ExecutorService es = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                es.submit(() -> {
                    try {
                        v.negate();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean done = latch.await(10, TimeUnit.SECONDS);
            es.shutdown();

            assertTrue(done, "Timeout waiting for threads");
            // Even number of negates should return to original (or negative if thread timing varies)
            double result = v.get(0);
            assertTrue(Math.abs(result) == 100.0, "Value should be ±100, got: " + result);
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Concurrent transpose operations")
        void testConcurrentTranspose() throws InterruptedException {
            int threadCount = 50;
            SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);

            ExecutorService es = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(threadCount);

            for (int i = 0; i < threadCount; i++) {
                es.submit(() -> {
                    try {
                        v.transpose();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean done = latch.await(10, TimeUnit.SECONDS);
            es.shutdown();

            assertTrue(done, "Timeout waiting for threads");
            // Data should remain unchanged regardless of transpose count
            assertEquals(1.0, v.get(0), DELTA);
            assertEquals(2.0, v.get(1), DELTA);
            assertEquals(3.0, v.get(2), DELTA);
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Concurrent VecMatMul with shared matrix")
        void testConcurrentVecMatMul() throws InterruptedException {
            int threadCount = 50;
            double[][] matData = {{1, 1}, {1, 1}};
            SharedMatrix sharedM = new SharedMatrix();
            sharedM.loadColumnMajor(matData);

            ExecutorService es = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger failures = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                es.submit(() -> {
                    try {
                        SharedVector myVec = new SharedVector(new double[]{2, 2}, VectorOrientation.ROW_MAJOR);
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

            boolean done = latch.await(10, TimeUnit.SECONDS);
            es.shutdown();

            assertTrue(done, "Timeout in ConcurrentVecMatMul");
            assertEquals(0, failures.get(), "Some threads failed in VecMatMul concurrency test");
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Concurrent dot product operations")
        void testConcurrentDotProduct() throws InterruptedException {
            int threadCount = 100;
            ExecutorService es = Executors.newFixedThreadPool(20);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger failures = new AtomicInteger(0);

            SharedVector sharedRow = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
            SharedVector sharedCol = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.COLUMN_MAJOR);

            for (int i = 0; i < threadCount; i++) {
                es.submit(() -> {
                    try {
                        double result = sharedRow.dot(sharedCol);
                        // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
                        if (Math.abs(result - 32.0) > DELTA) {
                            failures.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean done = latch.await(10, TimeUnit.SECONDS);
            es.shutdown();

            assertTrue(done, "Timeout in concurrent dot product");
            assertEquals(0, failures.get(), "Concurrent dot product returned wrong values");
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Mixed concurrent operations - stress test")
        void testMixedConcurrentOperations() throws InterruptedException {
            int threadCount = 200;
            SharedVector v1 = new SharedVector(new double[]{100, 200, 300}, VectorOrientation.ROW_MAJOR);
            SharedVector v2 = new SharedVector(new double[]{1, 1, 1}, VectorOrientation.ROW_MAJOR);

            ExecutorService es = Executors.newFixedThreadPool(20);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger errors = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final int op = i % 4;
                es.submit(() -> {
                    try {
                        switch (op) {
                            case 0:
                                v1.get(0);
                                break;
                            case 1:
                                v1.length();
                                break;
                            case 2:
                                v1.getOrientation();
                                break;
                            case 3:
                                v2.get(0);
                                break;
                        }
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean done = latch.await(10, TimeUnit.SECONDS);
            es.shutdown();

            assertTrue(done, "Timeout in mixed operations");
            assertEquals(0, errors.get(), "Errors in mixed concurrent operations");
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Barrier synchronized concurrent operations")
        void testBarrierSynchronizedOperations() throws Exception {
            int threadCount = 10;
            CyclicBarrier barrier = new CyclicBarrier(threadCount);
            SharedVector[] vectors = new SharedVector[threadCount];
            
            for (int i = 0; i < threadCount; i++) {
                vectors[i] = new SharedVector(new double[]{i, i * 2}, VectorOrientation.ROW_MAJOR);
            }

            ExecutorService es = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger errors = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                final int idx = i;
                es.submit(() -> {
                    try {
                        barrier.await(); // All threads start together
                        vectors[idx].negate();
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean done = latch.await(10, TimeUnit.SECONDS);
            es.shutdown();

            assertTrue(done, "Timeout in barrier test");
            assertEquals(0, errors.get(), "Errors in barrier synchronized operations");

            // Verify results
            for (int i = 0; i < threadCount; i++) {
                assertEquals(-i, vectors[i].get(0), DELTA);
                assertEquals(-i * 2, vectors[i].get(1), DELTA);
            }
        }
    }
}

