import parser.*;
import spl.lae.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class TestLinearAlgebraEngine {

    private static final double DELTA = 0.0001;
    private LinearAlgebraEngine lae;

    @BeforeEach
    void setUp() {
        lae = new LinearAlgebraEngine(4);
    }

    // Basic Operations Tests

    @Test
    @DisplayName("Matrix addition 2x2")
    void testMatrixAddition2x2() {
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{5, 6}, {7, 8}};
        
        List<ComputationNode> children = new ArrayList<>();
        children.add(new ComputationNode(m1));
        children.add(new ComputationNode(m2));
        ComputationNode root = new ComputationNode(ComputationNodeType.ADD, children);
        ComputationNode result = lae.run(root);
        
        assertEquals(ComputationNodeType.MATRIX, result.getNodeType());
        double[][] resultMatrix = result.getMatrix();
        
        assertEquals(6.0, resultMatrix[0][0], DELTA);
        assertEquals(8.0, resultMatrix[0][1], DELTA);
        assertEquals(10.0, resultMatrix[1][0], DELTA);
        assertEquals(12.0, resultMatrix[1][1], DELTA);
    }

    @Test
    @DisplayName("Matrix multiplication 2x2")
    void testMatrixMultiplication2x2() {
        double[][] m1 = {{1, 2}, {3, 4}};
        double[][] m2 = {{5, 6}, {7, 8}};
        
        List<ComputationNode> children = new ArrayList<>();
        children.add(new ComputationNode(m1));
        children.add(new ComputationNode(m2));
        
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, children);
        ComputationNode result = lae.run(root);
        
        double[][] resultMatrix = result.getMatrix();
        assertEquals(19.0, resultMatrix[0][0], DELTA);
        assertEquals(22.0, resultMatrix[0][1], DELTA);
        assertEquals(43.0, resultMatrix[1][0], DELTA);
        assertEquals(50.0, resultMatrix[1][1], DELTA);
    }

    @Test
    @DisplayName("Matrix negation")
    void testMatrixNegation() {
        double[][] m = {{1, 2}, {3, 4}};
        
        List<ComputationNode> children = new ArrayList<>();
        children.add(new ComputationNode(m));
        ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE, children);
        ComputationNode result = lae.run(root);
        
        double[][] resultMatrix = result.getMatrix();
        assertEquals(-1.0, resultMatrix[0][0], DELTA);
        assertEquals(-2.0, resultMatrix[0][1], DELTA);
        assertEquals(-3.0, resultMatrix[1][0], DELTA);
        assertEquals(-4.0, resultMatrix[1][1], DELTA);
    }

    @Test
    @DisplayName("Matrix transpose")
    void testMatrixTranspose() {
        double[][] m = {{1, 2, 3}, {4, 5, 6}};
        
        List<ComputationNode> children = new ArrayList<>();
        children.add(new ComputationNode(m));
        
        ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE, children);
        ComputationNode result = lae.run(root);
        
        double[][] resultMatrix = result.getMatrix();
        assertEquals(3, resultMatrix.length);
        assertEquals(2, resultMatrix[0].length);
        assertEquals(1.0, resultMatrix[0][0], DELTA);
        assertEquals(4.0, resultMatrix[0][1], DELTA);
        assertEquals(2.0, resultMatrix[1][0], DELTA);
        assertEquals(5.0, resultMatrix[1][1], DELTA);
    }

    // Nested Operations test

    @Test
    @DisplayName("Nested add then multiply")
    void testNestedAddThenMultiply() {
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
        
        ComputationNode result = lae.run(root);
        double[][] resultMatrix = result.getMatrix();
        
        assertEquals(4.0, resultMatrix[0][0], DELTA);
        assertEquals(6.0, resultMatrix[0][1], DELTA);
        assertEquals(8.0, resultMatrix[1][0], DELTA);
        assertEquals(10.0, resultMatrix[1][1], DELTA);
    }

    @Test
    @DisplayName("Double negation equals original")
    void testDoubleNegation() {
        double[][] m = {{1, 2}, {3, 4}};
        
        List<ComputationNode> inner = new ArrayList<>();
        inner.add(new ComputationNode(m));
        ComputationNode negateOnce = new ComputationNode(ComputationNodeType.NEGATE, inner);
        
        List<ComputationNode> outer = new ArrayList<>();
        outer.add(negateOnce);
        ComputationNode root = new ComputationNode(ComputationNodeType.NEGATE, outer);
        
        ComputationNode result = lae.run(root);
        double[][] resultMatrix = result.getMatrix();
        
        assertEquals(1.0, resultMatrix[0][0], DELTA);
        assertEquals(2.0, resultMatrix[0][1], DELTA);
        assertEquals(3.0, resultMatrix[1][0], DELTA);
        assertEquals(4.0, resultMatrix[1][1], DELTA);
    }

    @Test
    @DisplayName("Double transpose equals original")
    void testDoubleTranspose() {
        double[][] m = {{1, 2, 3}, {4, 5, 6}};
        
        List<ComputationNode> inner = new ArrayList<>();
        inner.add(new ComputationNode(m));
        ComputationNode transposeOnce = new ComputationNode(ComputationNodeType.TRANSPOSE, inner);
        
        List<ComputationNode> outer = new ArrayList<>();
        outer.add(transposeOnce);
        ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE, outer);
        
        ComputationNode result = lae.run(root);
        double[][] resultMatrix = result.getMatrix();
        
        assertEquals(2, resultMatrix.length);
        assertEquals(3, resultMatrix[0].length);
        assertEquals(1.0, resultMatrix[0][0], DELTA);
        assertEquals(2.0, resultMatrix[0][1], DELTA);
        assertEquals(3.0, resultMatrix[0][2], DELTA);
    }

    // Non Binary Tree Nesting Tests

    @Test
    @DisplayName("Multiple operands in addition")
    void testMultipleAddition() {
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
        ComputationNode result = lae.run(root);
        
        double[][] resultMatrix = result.getMatrix();
        assertEquals(10.0, resultMatrix[0][0], DELTA);
        assertEquals(10.0, resultMatrix[0][1], DELTA);
        assertEquals(10.0, resultMatrix[1][0], DELTA);
        assertEquals(10.0, resultMatrix[1][1], DELTA);
    }

    @Test
    @DisplayName("Multiple operands in multiplication")
    void testMultipleMultiplication() {
        double[][] identity = {{1, 0}, {0, 1}};
        double[][] m = {{2, 3}, {4, 5}};
        
        List<ComputationNode> children = new ArrayList<>();
        children.add(new ComputationNode(identity));
        children.add(new ComputationNode(m));
        children.add(new ComputationNode(identity));
        
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, children);
        ComputationNode result = lae.run(root);
        
        double[][] resultMatrix = result.getMatrix();
        assertEquals(2.0, resultMatrix[0][0], DELTA);
        assertEquals(3.0, resultMatrix[0][1], DELTA);
        assertEquals(4.0, resultMatrix[1][0], DELTA);
        assertEquals(5.0, resultMatrix[1][1], DELTA);
    }

    // Large Matrix Tests

    @Test
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
        ComputationNode result = lae.run(root);
        
        double[][] resultMatrix = result.getMatrix();
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 10; j++) {
                assertEquals(3.0, resultMatrix[i][j], DELTA);
            }
        }
    }

    @Test
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
        ComputationNode result = lae.run(root);
        
        double[][] resultMatrix = result.getMatrix();
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                assertEquals(i * 20 + j, resultMatrix[i][j], DELTA);
            }
        }
    }

    @Test
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
        ComputationNode result = lae.run(root);
        
        double[][] resultMatrix = result.getMatrix();
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 50; j++) {
                assertEquals(-(i + j), resultMatrix[i][j], DELTA);
            }
        }
    }

    //        Error Handling Tests

    @Test
    @DisplayName("Null computation root throws exception")
    void testNullRoot() {
        assertThrows(IllegalArgumentException.class, () -> lae.run(null));
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
        assertThrows(Exception.class, () -> lae.run(root));
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
        assertThrows(Exception.class, () -> lae.run(root));
    }

    // Special Matrix Tests

    @Test
    @DisplayName("1x1 matrix operations")
    void testSingleElementMatrix() {
        double[][] m1 = {{5}};
        double[][] m2 = {{3}};
        
        List<ComputationNode> children = new ArrayList<>();
        children.add(new ComputationNode(m1));
        children.add(new ComputationNode(m2));
        
        ComputationNode root = new ComputationNode(ComputationNodeType.MULTIPLY, children);
        ComputationNode result = lae.run(root);
        
        assertEquals(15.0, result.getMatrix()[0][0], DELTA);
    }


    @Test
    @DisplayName("Rectangular matrix transpose")
    void testRectangularTranspose() {
        double[][] m = {{1, 2, 3, 4}, {5, 6, 7, 8}};
        
        List<ComputationNode> children = new ArrayList<>();
        children.add(new ComputationNode(m));
        
        ComputationNode root = new ComputationNode(ComputationNodeType.TRANSPOSE, children);
        ComputationNode result = lae.run(root);
        
        double[][] resultMatrix = result.getMatrix();
        assertEquals(4, resultMatrix.length);
        assertEquals(2, resultMatrix[0].length);
        assertEquals(1.0, resultMatrix[0][0], DELTA);
        assertEquals(5.0, resultMatrix[0][1], DELTA);
        assertEquals(4.0, resultMatrix[3][0], DELTA);
        assertEquals(8.0, resultMatrix[3][1], DELTA);
    }
}
