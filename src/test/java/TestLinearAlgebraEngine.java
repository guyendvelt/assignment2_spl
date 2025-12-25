import parser.*;
import spl.lae.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class TestLinearAlgebraEngine {

    private static final double DELTA = 0.0001;
    private LinearAlgebraEngine engine;

    @BeforeEach
    void setUp() {
        engine = new LinearAlgebraEngine(4);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        if (engine != null) {
            engine.shutdown();
        }
    }

    // ==========================================
    //        BASIC OPERATION TESTS
    // ==========================================

    @Nested
    @DisplayName("Basic Operation Tests")
    class BasicOperationTests {

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Matrix addition 2x2")
        void testMatrixAddition2x2() {
            double[][] m1 = {{1, 2}, {3, 4}};
            double[][] m2 = {{5, 6}, {7, 8}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m1));
            children.add(new ComputationNode(m2));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.ADD, children);
            ComputationNode result = engine.run(root);
            
            assertEquals(ComputationNodeType.MATRIX, result.getNodeType());
            double[][] resultMatrix = result.getMatrix();
            
            assertEquals(6.0, resultMatrix[0][0], DELTA);
            assertEquals(8.0, resultMatrix[0][1], DELTA);
            assertEquals(10.0, resultMatrix[1][0], DELTA);
            assertEquals(12.0, resultMatrix[1][1], DELTA);
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Matrix multiplication 2x2")
        void testMatrixMultiplication2x2() {
            double[][] m1 = {{1, 2}, {3, 4}};
            double[][] m2 = {{5, 6}, {7, 8}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m1));
            children.add(new ComputationNode(m2));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, children);
            ComputationNode result = engine.run(root);
            
            double[][] resultMatrix = result.getMatrix();
            // [[1,2],[3,4]] * [[5,6],[7,8]] = [[19,22],[43,50]]
            assertEquals(19.0, resultMatrix[0][0], DELTA);
            assertEquals(22.0, resultMatrix[0][1], DELTA);
            assertEquals(43.0, resultMatrix[1][0], DELTA);
            assertEquals(50.0, resultMatrix[1][1], DELTA);
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Matrix negation")
        void testMatrixNegation() {
            double[][] m = {{1, 2}, {3, 4}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE, children);
            ComputationNode result = engine.run(root);
            
            double[][] resultMatrix = result.getMatrix();
            assertEquals(-1.0, resultMatrix[0][0], DELTA);
            assertEquals(-2.0, resultMatrix[0][1], DELTA);
            assertEquals(-3.0, resultMatrix[1][0], DELTA);
            assertEquals(-4.0, resultMatrix[1][1], DELTA);
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Matrix transpose")
        void testMatrixTranspose() {
            double[][] m = {{1, 2, 3}, {4, 5, 6}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE, children);
            ComputationNode result = engine.run(root);
            
            double[][] resultMatrix = result.getMatrix();
            assertEquals(3, resultMatrix.length);
            assertEquals(2, resultMatrix[0].length);
            assertEquals(1.0, resultMatrix[0][0], DELTA);
            assertEquals(4.0, resultMatrix[0][1], DELTA);
            assertEquals(2.0, resultMatrix[1][0], DELTA);
            assertEquals(5.0, resultMatrix[1][1], DELTA);
        }
    }

    // ==========================================
    //        NESTED OPERATION TESTS
    // ==========================================

    @Nested
    @DisplayName("Nested Operation Tests")
    class NestedOperationTests {

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Nested add then multiply")
        void testNestedAddThenMultiply() {
            // (A + B) * C
            double[][] a = {{1, 0}, {0, 1}};
            double[][] b = {{1, 0}, {0, 1}};
            double[][] c = {{2, 3}, {4, 5}};
            
            List<ComputationNode> addChildren = new ArrayList<>();
            addChildren.add(new ComputationNode(a));
            addChildren.add(new ComputationNode(b));
            ComputationNode addNode = new ComputationNode(ComputationNodeType.ADD, addChildren);
            
            List<ComputationNode> mulChildren = new ArrayList<>();
            mulChildren.add(addNode);
            mulChildren.add(new ComputationNode(c));
            ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, mulChildren);
            
            ComputationNode result = engine.run(root);
            double[][] resultMatrix = result.getMatrix();
            
            // (I + I) * C = 2I * C = 2C
            assertEquals(4.0, resultMatrix[0][0], DELTA);
            assertEquals(6.0, resultMatrix[0][1], DELTA);
            assertEquals(8.0, resultMatrix[1][0], DELTA);
            assertEquals(10.0, resultMatrix[1][1], DELTA);
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Double negation equals original")
        void testDoubleNegation() {
            double[][] m = {{1, 2}, {3, 4}};
            
            List<ComputationNode> inner = new ArrayList<>();
            inner.add(new ComputationNode(m));
            ComputationNode negateOnce = new ComputationNode(ComputationNodeType.NEGATE, inner);
            
            List<ComputationNode> outer = new ArrayList<>();
            outer.add(negateOnce);
            ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE, outer);
            
            ComputationNode result = engine.run(root);
            double[][] resultMatrix = result.getMatrix();
            
            assertEquals(1.0, resultMatrix[0][0], DELTA);
            assertEquals(2.0, resultMatrix[0][1], DELTA);
            assertEquals(3.0, resultMatrix[1][0], DELTA);
            assertEquals(4.0, resultMatrix[1][1], DELTA);
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Double transpose equals original")
        void testDoubleTranspose() {
            double[][] m = {{1, 2, 3}, {4, 5, 6}};
            
            List<ComputationNode> inner = new ArrayList<>();
            inner.add(new ComputationNode(m));
            ComputationNode transposeOnce = new ComputationNode(ComputationNodeType.TRANSPOSE, inner);
            
            List<ComputationNode> outer = new ArrayList<>();
            outer.add(transposeOnce);
            ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE, outer);
            
            ComputationNode result = engine.run(root);
            double[][] resultMatrix = result.getMatrix();
            
            assertEquals(2, resultMatrix.length);
            assertEquals(3, resultMatrix[0].length);
            assertEquals(1.0, resultMatrix[0][0], DELTA);
            assertEquals(2.0, resultMatrix[0][1], DELTA);
            assertEquals(3.0, resultMatrix[0][2], DELTA);
        }
    }

    // ==========================================
    //        ASSOCIATIVE NESTING TESTS
    // ==========================================

    @Nested
    @DisplayName("Associative Nesting Tests")
    class AssociativeNestingTests {

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Multiple operands in addition")
        void testMultipleAddition() {
            // A + B + C + D
            double[][] a = {{1, 1}, {1, 1}};
            double[][] b = {{2, 2}, {2, 2}};
            double[][] c = {{3, 3}, {3, 3}};
            double[][] d = {{4, 4}, {4, 4}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(a));
            children.add(new ComputationNode(b));
            children.add(new ComputationNode(c));
            children.add(new ComputationNode(d));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.ADD, children);
            ComputationNode result = engine.run(root);
            
            double[][] resultMatrix = result.getMatrix();
            // 1+2+3+4 = 10
            assertEquals(10.0, resultMatrix[0][0], DELTA);
            assertEquals(10.0, resultMatrix[0][1], DELTA);
            assertEquals(10.0, resultMatrix[1][0], DELTA);
            assertEquals(10.0, resultMatrix[1][1], DELTA);
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Multiple operands in multiplication")
        void testMultipleMultiplication() {
            // A * B * C (with identity matrices)
            double[][] identity = {{1, 0}, {0, 1}};
            double[][] m = {{2, 3}, {4, 5}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(identity));
            children.add(new ComputationNode(m));
            children.add(new ComputationNode(identity));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, children);
            ComputationNode result = engine.run(root);
            
            double[][] resultMatrix = result.getMatrix();
            // I * M * I = M
            assertEquals(2.0, resultMatrix[0][0], DELTA);
            assertEquals(3.0, resultMatrix[0][1], DELTA);
            assertEquals(4.0, resultMatrix[1][0], DELTA);
            assertEquals(5.0, resultMatrix[1][1], DELTA);
        }
    }

    // ==========================================
    //        LARGE MATRIX TESTS
    // ==========================================

    @Nested
    @DisplayName("Large Matrix Tests")
    class LargeMatrixTests {

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("10x10 matrix addition")
        void testLargeAddition() {
            double[][] m1 = new double[10][10];
            double[][] m2 = new double[10][10];
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    m1[i][j] = 1;
                    m2[i][j] = 2;
                }
            }
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m1));
            children.add(new ComputationNode(m2));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.ADD, children);
            ComputationNode result = engine.run(root);
            
            double[][] resultMatrix = result.getMatrix();
            for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                    assertEquals(3.0, resultMatrix[i][j], DELTA);
                }
            }
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("20x20 matrix multiplication")
        void testLargeMultiplication() {
            double[][] identity = new double[20][20];
            double[][] m = new double[20][20];
            for (int i = 0; i < 20; i++) {
                identity[i][i] = 1;
                for (int j = 0; j < 20; j++) {
                    m[i][j] = i * 20 + j;
                }
            }
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(identity));
            children.add(new ComputationNode(m));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, children);
            ComputationNode result = engine.run(root);
            
            double[][] resultMatrix = result.getMatrix();
            // I * M = M
            for (int i = 0; i < 20; i++) {
                for (int j = 0; j < 20; j++) {
                    assertEquals(i * 20 + j, resultMatrix[i][j], DELTA);
                }
            }
        }

        @Test
        @Timeout(value = 10, unit = TimeUnit.SECONDS)
        @DisplayName("Large matrix negation")
        void testLargeNegation() {
            double[][] m = new double[50][50];
            for (int i = 0; i < 50; i++) {
                for (int j = 0; j < 50; j++) {
                    m[i][j] = i + j;
                }
            }
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE, children);
            ComputationNode result = engine.run(root);
            
            double[][] resultMatrix = result.getMatrix();
            for (int i = 0; i < 50; i++) {
                for (int j = 0; j < 50; j++) {
                    assertEquals(-(i + j), resultMatrix[i][j], DELTA);
                }
            }
        }
    }

    // ==========================================
    //        ERROR HANDLING TESTS
    // ==========================================

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Null computation root throws exception")
        void testNullRoot() {
            assertThrows(IllegalArgumentException.class, () -> engine.run(null));
        }

        @Test
        @DisplayName("Dimension mismatch in addition throws exception")
        void testDimensionMismatchAdd() {
            double[][] m1 = {{1, 2}, {3, 4}};
            double[][] m2 = {{1, 2, 3}, {4, 5, 6}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m1));
            children.add(new ComputationNode(m2));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.ADD, children);
            assertThrows(Exception.class, () -> engine.run(root));
        }

        @Test
        @DisplayName("Dimension mismatch in multiplication throws exception")
        void testDimensionMismatchMultiply() {
            double[][] m1 = {{1, 2, 3}, {4, 5, 6}};
            double[][] m2 = {{1, 2}, {3, 4}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m1));
            children.add(new ComputationNode(m2));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, children);
            assertThrows(Exception.class, () -> engine.run(root));
        }
    }

    // ==========================================
    //        WORKER REPORT TESTS
    // ==========================================

    @Nested
    @DisplayName("Worker Report Tests")
    class WorkerReportTests {

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Worker report after computation")
        void testWorkerReportAfterComputation() {
            double[][] m1 = {{1, 2}, {3, 4}};
            double[][] m2 = {{5, 6}, {7, 8}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m1));
            children.add(new ComputationNode(m2));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.ADD, children);
            engine.run(root);
            
            String report = engine.getWorkerReport();
            assertNotNull(report);
            assertTrue(report.contains("Worker"));
        }
    }

    // ==========================================
    //        PARALLELISM VERIFICATION TESTS
    // ==========================================

    @Nested
    @DisplayName("Parallelism Verification Tests")
    class ParallelismTests {

        @Test
        @Timeout(value = 15, unit = TimeUnit.SECONDS)
        @DisplayName("Large matrix operations use parallelism")
        void testParallelExecution() throws InterruptedException {
            // Create larger engine for better parallelism
            engine.shutdown();
            engine = new LinearAlgebraEngine(8);
            
            double[][] m = new double[100][100];
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    m[i][j] = 1.0;
                }
            }
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE, children);
            engine.run(root);
            
            // Verify result
            double[][] resultMatrix = root.getMatrix();
            for (int i = 0; i < 100; i++) {
                for (int j = 0; j < 100; j++) {
                    assertEquals(-1.0, resultMatrix[i][j], DELTA);
                }
            }
            
            // Check worker report shows multiple workers did work
            String report = engine.getWorkerReport();
            assertTrue(report.contains("Worker #0"));
        }

        @Test
        @Timeout(value = 20, unit = TimeUnit.SECONDS)
        @DisplayName("Row-parallel matrix multiplication")
        void testRowParallelMultiplication() throws InterruptedException {
            engine.shutdown();
            engine = new LinearAlgebraEngine(10);
            
            int size = 50;
            double[][] m1 = new double[size][size];
            double[][] m2 = new double[size][size];
            
            // m1 = all 1s, m2 = all 1s
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    m1[i][j] = 1.0;
                    m2[i][j] = 1.0;
                }
            }
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m1));
            children.add(new ComputationNode(m2));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, children);
            engine.run(root);
            
            double[][] result = root.getMatrix();
            // Each element should be 50 (sum of 50 1s)
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    assertEquals(50.0, result[i][j], DELTA);
                }
            }
        }
    }

    // ==========================================
    //        SPECIAL MATRIX TESTS
    // ==========================================

    @Nested
    @DisplayName("Special Matrix Tests")
    class SpecialMatrixTests {

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("1x1 matrix operations")
        void testSingleElementMatrix() {
            double[][] m1 = {{5}};
            double[][] m2 = {{3}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m1));
            children.add(new ComputationNode(m2));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, children);
            ComputationNode result = engine.run(root);
            
            assertEquals(15.0, result.getMatrix()[0][0], DELTA);
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Zero matrix addition")
        void testZeroMatrixAddition() {
            double[][] zeros = {{0, 0}, {0, 0}};
            double[][] m = {{1, 2}, {3, 4}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(zeros));
            children.add(new ComputationNode(m));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.ADD, children);
            ComputationNode result = engine.run(root);
            
            double[][] resultMatrix = result.getMatrix();
            assertEquals(1.0, resultMatrix[0][0], DELTA);
            assertEquals(2.0, resultMatrix[0][1], DELTA);
            assertEquals(3.0, resultMatrix[1][0], DELTA);
            assertEquals(4.0, resultMatrix[1][1], DELTA);
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Identity matrix multiplication")
        void testIdentityMultiplication() {
            double[][] identity = {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
            double[][] m = {{1, 2, 3}, {4, 5, 6}, {7, 8, 9}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(identity));
            children.add(new ComputationNode(m));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, children);
            ComputationNode result = engine.run(root);
            
            double[][] resultMatrix = result.getMatrix();
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    assertEquals(m[i][j], resultMatrix[i][j], DELTA);
                }
            }
        }

        @Test
        @Timeout(value = 5, unit = TimeUnit.SECONDS)
        @DisplayName("Rectangular matrix transpose")
        void testRectangularTranspose() {
            double[][] m = {{1, 2, 3, 4}, {5, 6, 7, 8}};
            
            List<ComputationNode> children = new ArrayList<>();
            children.add(new ComputationNode(m));
            
            ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE, children);
            ComputationNode result = engine.run(root);
            
            double[][] resultMatrix = result.getMatrix();
            assertEquals(4, resultMatrix.length);
            assertEquals(2, resultMatrix[0].length);
            assertEquals(1.0, resultMatrix[0][0], DELTA);
            assertEquals(5.0, resultMatrix[0][1], DELTA);
            assertEquals(4.0, resultMatrix[3][0], DELTA);
            assertEquals(8.0, resultMatrix[3][1], DELTA);
        }
    }
}

