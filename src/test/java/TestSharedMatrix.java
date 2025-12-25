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

import static org.junit.jupiter.api.Assertions.*;

class TestSharedMatrix {

    private static final double DELTA = 0.0001;

    // ==========================================
    //        BASIC OPERATIONS TESTS
    // ==========================================

    @Nested
    @DisplayName("SharedMatrix Basic Operations")
    class BasicOperationsTests {

        @Test
        @DisplayName("Matrix initialization with 2D array")
        void testMatrixInit() {
            double[][] data = {{1, 2}, {3, 4}};
            SharedMatrix m = new SharedMatrix(data);
            assertEquals(2, m.length());
            double[][] read = m.readRowMajor();
            assertEquals(1.0, read[0][0], DELTA);
            assertEquals(2.0, read[0][1], DELTA);
            assertEquals(3.0, read[1][0], DELTA);
            assertEquals(4.0, read[1][1], DELTA);
        }

        @Test
        @DisplayName("Matrix initialization with null throws exception")
        void testMatrixInitNull() {
            assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(null));
        }

        @Test
        @DisplayName("Empty matrix constructor")
        void testEmptyMatrixConstructor() {
            SharedMatrix m = new SharedMatrix();
            assertEquals(0, m.length());
        }

        @Test
        @DisplayName("1x1 matrix")
        void testSingleElementMatrix() {
            double[][] data = {{42}};
            SharedMatrix m = new SharedMatrix(data);
            assertEquals(1, m.length());
            double[][] read = m.readRowMajor();
            assertEquals(42.0, read[0][0], DELTA);
        }

        @Test
        @DisplayName("Large matrix initialization")
        void testLargeMatrixInit() {
            int size = 100;
            double[][] data = new double[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    data[i][j] = i * size + j;
                }
            }
            SharedMatrix m = new SharedMatrix(data);
            assertEquals(size, m.length());
            double[][] read = m.readRowMajor();
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    assertEquals(i * size + j, read[i][j], DELTA);
                }
            }
        }

        @Test
        @DisplayName("Non-square matrix (3x5)")
        void testNonSquareMatrix() {
            double[][] data = {
                {1, 2, 3, 4, 5},
                {6, 7, 8, 9, 10},
                {11, 12, 13, 14, 15}
            };
            SharedMatrix m = new SharedMatrix(data);
            assertEquals(3, m.length());
            double[][] read = m.readRowMajor();
            assertEquals(1.0, read[0][0], DELTA);
            assertEquals(15.0, read[2][4], DELTA);
        }
    }

    // ==========================================
    //        LOAD OPERATIONS TESTS
    // ==========================================

    @Nested
    @DisplayName("SharedMatrix Load Operations")
    class LoadOperationsTests {

        @Test
        @DisplayName("Load row major")
        void testLoadRowMajor() {
            SharedMatrix m = new SharedMatrix();
            double[][] data = {{1, 2, 3}, {4, 5, 6}};
            m.loadRowMajor(data);
            assertEquals(2, m.length());
            double[][] read = m.readRowMajor();
            assertEquals(1.0, read[0][0], DELTA);
            assertEquals(6.0, read[1][2], DELTA);
        }

        @Test
        @DisplayName("Load column major")
        void testLoadColumnMajor() {
            double[][] data = {{1, 2}, {3, 4}};
            SharedMatrix m = new SharedMatrix();
            m.loadColumnMajor(data);
            double[][] read = m.readRowMajor();
            // Column major loading transposes the data
            assertEquals(1.0, read[0][0], DELTA);
            assertEquals(2.0, read[0][1], DELTA);
            assertEquals(3.0, read[1][0], DELTA);
            assertEquals(4.0, read[1][1], DELTA);
        }

        @Test
        @DisplayName("Load row major with null throws exception")
        void testLoadRowMajorNull() {
            SharedMatrix m = new SharedMatrix();
            assertThrows(IllegalArgumentException.class, () -> m.loadRowMajor(null));
        }

        @Test
        @DisplayName("Load column major with null throws exception")
        void testLoadColumnMajorNull() {
            SharedMatrix m = new SharedMatrix();
            assertThrows(IllegalArgumentException.class, () -> m.loadColumnMajor(null));
        }

        @Test
        @DisplayName("Load replaces existing data")
        void testLoadReplacesData() {
            SharedMatrix m = new SharedMatrix(new double[][]{{1, 2}, {3, 4}});
            m.loadRowMajor(new double[][]{{5, 6, 7}, {8, 9, 10}});
            assertEquals(2, m.length());
            double[][] read = m.readRowMajor();
            assertEquals(5.0, read[0][0], DELTA);
            assertEquals(10.0, read[1][2], DELTA);
        }
    }

    // ==========================================
    //        GET AND ORIENTATION TESTS
    // ==========================================

    @Nested
    @DisplayName("SharedMatrix Get and Orientation")
    class GetAndOrientationTests {

        @Test
        @DisplayName("Get vector at valid index")
        void testGetValidIndex() {
            SharedMatrix m = new SharedMatrix(new double[][]{{1, 2}, {3, 4}, {5, 6}});
            SharedVector v = m.get(1);
            assertEquals(3.0, v.get(0), DELTA);
            assertEquals(4.0, v.get(1), DELTA);
        }

        @Test
        @DisplayName("Get throws on negative index")
        void testGetNegativeIndex() {
            SharedMatrix m = new SharedMatrix(new double[][]{{1, 2}, {3, 4}});
            assertThrows(IllegalArgumentException.class, () -> m.get(-1));
        }

        @Test
        @DisplayName("Get throws on out of bounds index")
        void testGetOutOfBounds() {
            SharedMatrix m = new SharedMatrix(new double[][]{{1, 2}, {3, 4}});
            assertThrows(IllegalArgumentException.class, () -> m.get(5));
        }

        @Test
        @DisplayName("Get orientation for row major matrix")
        void testGetOrientationRowMajor() {
            SharedMatrix m = new SharedMatrix(new double[][]{{1, 2}, {3, 4}});
            assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
        }

        @Test
        @DisplayName("Get orientation for column major matrix")
        void testGetOrientationColumnMajor() {
            SharedMatrix m = new SharedMatrix();
            m.loadColumnMajor(new double[][]{{1, 2}, {3, 4}});
            assertEquals(VectorOrientation.COLUMN_MAJOR, m.getOrientation());
        }

        @Test
        @DisplayName("Get orientation throws for empty matrix")
        void testGetOrientationEmpty() {
            SharedMatrix m = new SharedMatrix();
            assertThrows(IllegalArgumentException.class, () -> m.getOrientation());
        }
    }

    // ==========================================
    //        EDGE CASE TESTS
    // ==========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Square matrix 10x10")
        void testSquareMatrix10x10() {
            double[][] data = new double[10][10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    data[i][j] = i * 10 + j + 1;
                }
            }
            SharedMatrix m = new SharedMatrix(data);
            assertEquals(10, m.length());
            double[][] read = m.readRowMajor();
            assertEquals(1.0, read[0][0], DELTA);
            assertEquals(100.0, read[9][9], DELTA);
            assertEquals(55.0, read[5][4], DELTA);
        }

        @Test
        @DisplayName("Rectangular matrix 2x10")
        void testRectangularMatrix2x10() {
            double[][] data = new double[2][10];
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 10; j++) {
                    data[i][j] = i * 10 + j;
                }
            }
            SharedMatrix m = new SharedMatrix(data);
            assertEquals(2, m.length());
            double[][] read = m.readRowMajor();
            assertEquals(0.0, read[0][0], DELTA);
            assertEquals(19.0, read[1][9], DELTA);
        }

        @Test
        @DisplayName("Rectangular matrix 10x2")
        void testRectangularMatrix10x2() {
            double[][] data = new double[10][2];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 2; j++) {
                    data[i][j] = i * 2 + j;
                }
            }
            SharedMatrix m = new SharedMatrix(data);
            assertEquals(10, m.length());
            double[][] read = m.readRowMajor();
            assertEquals(0.0, read[0][0], DELTA);
            assertEquals(19.0, read[9][1], DELTA);
        }

        @Test
        @DisplayName("Matrix transpose result verification via column major load")
        void testMatrixTransposeViaColumnMajor() {
            // Original: [[1, 2, 3], [4, 5, 6]]
            // Transposed: [[1, 4], [2, 5], [3, 6]]
            double[][] original = {{1, 2, 3}, {4, 5, 6}};
            SharedMatrix m = new SharedMatrix();
            m.loadColumnMajor(original);

            double[][] result = m.readRowMajor();
            assertEquals(2, result.length); // 3 rows now
            assertEquals(3, result[0].length); // 2 cols now
            assertEquals(1.0, result[0][0], DELTA);
            assertEquals(2.0, result[0][1], DELTA);
            assertEquals(3.0, result[0][2], DELTA);
            assertEquals(4.0, result[1][0], DELTA);
            assertEquals(5.0, result[1][1], DELTA);
            assertEquals(6.0, result[1][2], DELTA);
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
        @DisplayName("Concurrent read operations on matrix")
        void testConcurrentMatrixRead() throws InterruptedException {
            double[][] data = new double[10][10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    data[i][j] = i * 10 + j;
                }
            }
            SharedMatrix m = new SharedMatrix(data);

            int threadCount = 100;
            ExecutorService es = Executors.newFixedThreadPool(20);
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger failures = new AtomicInteger(0);

            for (int i = 0; i < threadCount; i++) {
                es.submit(() -> {
                    try {
                        double[][] read = m.readRowMajor();
                        for (int r = 0; r < 10; r++) {
                            for (int c = 0; c < 10; c++) {
                                if (Math.abs(read[r][c] - (r * 10 + c)) > DELTA) {
                                    failures.incrementAndGet();
                                }
                            }
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

            assertTrue(done, "Timeout in concurrent read");
            assertEquals(0, failures.get(), "Concurrent read returned wrong values");
        }

        @Test
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        @DisplayName("Parallel matrix row operations")
        void testParallelMatrixRowOperations() throws InterruptedException {
            int rows = 100;
            double[][] data = new double[rows][10];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < 10; j++) {
                    data[i][j] = 1.0;
                }
            }
            SharedMatrix m = new SharedMatrix(data);

            ExecutorService es = Executors.newFixedThreadPool(rows);
            CountDownLatch latch = new CountDownLatch(rows);
            AtomicInteger errors = new AtomicInteger(0);

            // Each thread negates one row
            for (int i = 0; i < rows; i++) {
                final int row = i;
                es.submit(() -> {
                    try {
                        m.get(row).negate();
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean done = latch.await(15, TimeUnit.SECONDS);
            es.shutdown();

            assertTrue(done, "Timeout in parallel row operations");
            assertEquals(0, errors.get(), "Errors in parallel row operations");

            // Verify all rows are negated
            double[][] result = m.readRowMajor();
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < 10; j++) {
                    assertEquals(-1.0, result[i][j], DELTA, 
                        "Row " + i + " col " + j + " not negated properly");
                }
            }
        }
    }

    // ==========================================
    //        COMPUTATION CORRECTNESS TESTS
    // ==========================================

    @Nested
    @DisplayName("Computation Correctness Tests")
    class ComputationCorrectnessTests {

        @Test
        @DisplayName("Matrix addition result verification")
        void testMatrixAdditionCorrectness() {
            // Create two matrices and add them row by row
            SharedMatrix m1 = new SharedMatrix(new double[][]{
                {1, 2, 3},
                {4, 5, 6}
            });
            SharedMatrix m2 = new SharedMatrix(new double[][]{
                {10, 20, 30},
                {40, 50, 60}
            });

            // Add row by row
            for (int i = 0; i < m1.length(); i++) {
                m1.get(i).add(m2.get(i));
            }

            double[][] result = m1.readRowMajor();
            assertEquals(11.0, result[0][0], DELTA);
            assertEquals(22.0, result[0][1], DELTA);
            assertEquals(33.0, result[0][2], DELTA);
            assertEquals(44.0, result[1][0], DELTA);
            assertEquals(55.0, result[1][1], DELTA);
            assertEquals(66.0, result[1][2], DELTA);
        }

        @Test
        @DisplayName("Matrix multiplication result verification 2x2")
        void testMatrixMultiplicationCorrectness2x2() {
            // A = [[1, 2], [3, 4]]
            // B = [[5, 6], [7, 8]]
            // A * B = [[19, 22], [43, 50]]
            SharedMatrix A = new SharedMatrix(new double[][]{{1, 2}, {3, 4}});
            SharedMatrix B = new SharedMatrix();
            B.loadColumnMajor(new double[][]{{5, 6}, {7, 8}});

            // Multiply each row of A by B
            for (int i = 0; i < A.length(); i++) {
                A.get(i).vecMatMul(B);
            }

            double[][] result = A.readRowMajor();
            assertEquals(19.0, result[0][0], DELTA);
            assertEquals(22.0, result[0][1], DELTA);
            assertEquals(43.0, result[1][0], DELTA);
            assertEquals(50.0, result[1][1], DELTA);
        }

        @Test
        @DisplayName("Matrix multiplication result verification 3x3")
        void testMatrixMultiplicationCorrectness3x3() {
            // A = [[1, 2, 3], [4, 5, 6], [7, 8, 9]]
            // B = [[9, 8, 7], [6, 5, 4], [3, 2, 1]]
            // A * B = [[30, 24, 18], [84, 69, 54], [138, 114, 90]]
            SharedMatrix A = new SharedMatrix(new double[][]{
                {1, 2, 3},
                {4, 5, 6},
                {7, 8, 9}
            });
            SharedMatrix B = new SharedMatrix();
            B.loadColumnMajor(new double[][]{
                {9, 8, 7},
                {6, 5, 4},
                {3, 2, 1}
            });

            for (int i = 0; i < A.length(); i++) {
                A.get(i).vecMatMul(B);
            }

            double[][] result = A.readRowMajor();
            assertEquals(30.0, result[0][0], DELTA);
            assertEquals(24.0, result[0][1], DELTA);
            assertEquals(18.0, result[0][2], DELTA);
            assertEquals(84.0, result[1][0], DELTA);
            assertEquals(69.0, result[1][1], DELTA);
            assertEquals(54.0, result[1][2], DELTA);
            assertEquals(138.0, result[2][0], DELTA);
            assertEquals(114.0, result[2][1], DELTA);
            assertEquals(90.0, result[2][2], DELTA);
        }

        @Test
        @DisplayName("Matrix negate result verification")
        void testMatrixNegateCorrectness() {
            SharedMatrix m = new SharedMatrix(new double[][]{
                {1, -2, 3},
                {-4, 5, -6}
            });

            for (int i = 0; i < m.length(); i++) {
                m.get(i).negate();
            }

            double[][] result = m.readRowMajor();
            assertEquals(-1.0, result[0][0], DELTA);
            assertEquals(2.0, result[0][1], DELTA);
            assertEquals(-3.0, result[0][2], DELTA);
            assertEquals(4.0, result[1][0], DELTA);
            assertEquals(-5.0, result[1][1], DELTA);
            assertEquals(6.0, result[1][2], DELTA);
        }
    }

    // ==========================================
    //        LARGE MATRIX TESTS
    // ==========================================

    @Nested
    @DisplayName("Large Matrix Tests")
    class LargeMatrixTests {

        @Test
        @DisplayName("50x50 matrix operations")
        void test50x50Matrix() {
            int size = 50;
            double[][] data = new double[size][size];
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    data[i][j] = i + j;
                }
            }

            SharedMatrix m = new SharedMatrix(data);
            assertEquals(size, m.length());

            // Negate all rows
            for (int i = 0; i < m.length(); i++) {
                m.get(i).negate();
            }

            double[][] result = m.readRowMajor();
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    assertEquals(-(i + j), result[i][j], DELTA);
                }
            }
        }

        @Test
        @DisplayName("100x100 matrix multiplication")
        void test100x100MatrixMultiplication() {
            int size = 100;
            double[][] dataA = new double[size][size];
            double[][] dataB = new double[size][size];

            // Identity-like matrices for easy verification
            for (int i = 0; i < size; i++) {
                dataA[i][i] = 1.0;
                dataB[i][i] = 2.0;
            }

            SharedMatrix A = new SharedMatrix(dataA);
            SharedMatrix B = new SharedMatrix();
            B.loadColumnMajor(dataB);

            for (int i = 0; i < A.length(); i++) {
                A.get(i).vecMatMul(B);
            }

            double[][] result = A.readRowMajor();
            // Diagonal should be 2.0, rest 0.0
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (i == j) {
                        assertEquals(2.0, result[i][j], DELTA);
                    } else {
                        assertEquals(0.0, result[i][j], DELTA);
                    }
                }
            }
        }

        @Test
        @DisplayName("Large rectangular matrix 200x50")
        void testLargeRectangularMatrix() {
            int rows = 200;
            int cols = 50;
            double[][] data = new double[rows][cols];
            for (int i = 0; i < rows; i++) {
                for (int j = 0; j < cols; j++) {
                    data[i][j] = 1.0;
                }
            }

            SharedMatrix m = new SharedMatrix(data);
            assertEquals(rows, m.length());

            double[][] result = m.readRowMajor();
            assertEquals(rows, result.length);
            assertEquals(cols, result[0].length);
        }
    }

    // ==========================================
    //        SPECIAL MATRIX TESTS
    // ==========================================

    @Nested
    @DisplayName("Special Matrix Tests")
    class SpecialMatrixTests {

        @Test
        @DisplayName("Zero matrix operations")
        void testZeroMatrix() {
            SharedMatrix m = new SharedMatrix(new double[][]{
                {0, 0, 0},
                {0, 0, 0}
            });

            // Negate zeros
            for (int i = 0; i < m.length(); i++) {
                m.get(i).negate();
            }

            double[][] result = m.readRowMajor();
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 3; j++) {
                    assertEquals(0.0, result[i][j], DELTA);
                }
            }
        }

        @Test
        @DisplayName("Diagonal matrix")
        void testDiagonalMatrix() {
            double[][] data = {
                {5, 0, 0, 0},
                {0, 10, 0, 0},
                {0, 0, 15, 0},
                {0, 0, 0, 20}
            };
            SharedMatrix m = new SharedMatrix(data);

            // Multiply by itself (diagonal * diagonal = diagonal with squared values)
            SharedMatrix m2 = new SharedMatrix();
            m2.loadColumnMajor(data);

            for (int i = 0; i < m.length(); i++) {
                m.get(i).vecMatMul(m2);
            }

            double[][] result = m.readRowMajor();
            assertEquals(25.0, result[0][0], DELTA);
            assertEquals(100.0, result[1][1], DELTA);
            assertEquals(225.0, result[2][2], DELTA);
            assertEquals(400.0, result[3][3], DELTA);
        }

        @Test
        @DisplayName("Upper triangular matrix")
        void testUpperTriangularMatrix() {
            double[][] data = {
                {1, 2, 3},
                {0, 4, 5},
                {0, 0, 6}
            };
            SharedMatrix m = new SharedMatrix(data);

            double[][] result = m.readRowMajor();
            assertEquals(1.0, result[0][0], DELTA);
            assertEquals(2.0, result[0][1], DELTA);
            assertEquals(3.0, result[0][2], DELTA);
            assertEquals(0.0, result[1][0], DELTA);
            assertEquals(4.0, result[1][1], DELTA);
            assertEquals(5.0, result[1][2], DELTA);
            assertEquals(0.0, result[2][0], DELTA);
            assertEquals(0.0, result[2][1], DELTA);
            assertEquals(6.0, result[2][2], DELTA);
        }

        @Test
        @DisplayName("Matrix with all same values")
        void testUniformMatrix() {
            double[][] data = new double[5][5];
            for (int i = 0; i < 5; i++) {
                for (int j = 0; j < 5; j++) {
                    data[i][j] = 7.0;
                }
            }
            SharedMatrix m = new SharedMatrix(data);
            assertEquals(5, m.length());

            SharedVector v = new SharedVector(new double[]{1, 1, 1, 1, 1}, VectorOrientation.ROW_MAJOR);
            SharedMatrix mCol = new SharedMatrix();
            mCol.loadColumnMajor(data);

            v.vecMatMul(mCol);

            // Each dot product: 1*7 + 1*7 + 1*7 + 1*7 + 1*7 = 35
            for (int i = 0; i < 5; i++) {
                assertEquals(35.0, v.get(i), DELTA);
            }
        }

        @Test
        @DisplayName("Identity matrix")
        void testIdentityMatrix() {
            double[][] data = {
                {1, 0, 0},
                {0, 1, 0},
                {0, 0, 1}
            };
            SharedMatrix m = new SharedMatrix(data);

            assertEquals(3, m.length());
            double[][] result = m.readRowMajor();
            
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (i == j) {
                        assertEquals(1.0, result[i][j], DELTA);
                    } else {
                        assertEquals(0.0, result[i][j], DELTA);
                    }
                }
            }
        }

        @Test
        @DisplayName("Matrix with negative values")
        void testMatrixWithNegatives() {
            double[][] data = {
                {-1, -2, -3},
                {-4, -5, -6}
            };
            SharedMatrix m = new SharedMatrix(data);

            double[][] result = m.readRowMajor();
            assertEquals(-1.0, result[0][0], DELTA);
            assertEquals(-2.0, result[0][1], DELTA);
            assertEquals(-3.0, result[0][2], DELTA);
            assertEquals(-4.0, result[1][0], DELTA);
            assertEquals(-5.0, result[1][1], DELTA);
            assertEquals(-6.0, result[1][2], DELTA);
        }

        @Test
        @DisplayName("Matrix with decimal values")
        void testMatrixWithDecimals() {
            double[][] data = {
                {1.5, 2.5},
                {3.5, 4.5}
            };
            SharedMatrix m = new SharedMatrix(data);
            assertEquals(2, m.length());
            double[][] result = m.readRowMajor();
            assertEquals(1.5, result[0][0], DELTA);
            assertEquals(2.5, result[0][1], DELTA);
            assertEquals(3.5, result[1][0], DELTA);
            assertEquals(4.5, result[1][1], DELTA);
        }
    }
}

