import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import parser.ComputationNode;
import parser.ComputationNodeType;
import spl.lae.LinearAlgebraEngine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestLAE {

    private LinearAlgebraEngine lae;
    private static final double DELTA = 0.0001; // For floating point comparison

    @BeforeEach
    public void setUp() {
        // אתחול המנוע עם מספר ת'רדים (למשל 4) לפני כל טסט
        lae = new LinearAlgebraEngine(4);
    }

    @AfterEach
//    public void tearDown() throws InterruptedException {
//        // חובה לסגור את ה-Executor כדי שהטסטים לא ייתקעו
//        if (lae != null) {
//            lae.shutdown();
//        }
//    }

    /**
     * בדיקת חיבור פשוט של שתי מטריצות
     * A + B
     */
    @Test
    public void testSimpleAdd() {
        double[][] m1 = {{1.0, 2.0}, {3.0, 4.0}};
        double[][] m2 = {{5.0, 6.0}, {7.0, 8.0}};
        double[][] expected = {{6.0, 8.0}, {10.0, 12.0}};

        ComputationNode nodeA = new ComputationNode(m1);
        ComputationNode nodeB = new ComputationNode(m2);
        ComputationNode addNode = new ComputationNode(ComputationNodeType.ADD, Arrays.asList(nodeA, nodeB));

        ComputationNode resultNode = lae.run(addNode);

        assertMatrixEquals(expected, resultNode.getMatrix());
    }

    /**
     * בדיקת כפל מטריצות (לא ריבועיות) כדי לוודא שהלוגיקה של Row/Col תקינה
     * A (2x3) * B (3x2) = Result (2x2)
     */
    @Test
    public void testMultiplyDimensions() {
        double[][] a = {
                {1, 2, 3},
                {4, 5, 6}
        }; // 2x3

        double[][] b = {
                {7, 8},
                {9, 1},
                {2, 3}
        }; // 3x2

        // Calculation:
        // [0][0] = 1*7 + 2*9 + 3*2 = 7+18+6 = 31
        // [0][1] = 1*8 + 2*1 + 3*3 = 8+2+9 = 19
        // [1][0] = 4*7 + 5*9 + 6*2 = 28+45+12 = 85
        // [1][1] = 4*8 + 5*1 + 6*3 = 32+5+18 = 55
        double[][] expected = {
                {31, 19},
                {85, 55}
        };

        ComputationNode nodeA = new ComputationNode(a);
        ComputationNode nodeB = new ComputationNode(b);
        ComputationNode mulNode = new ComputationNode(ComputationNodeType.MULTIPLY, Arrays.asList(nodeA, nodeB));

        ComputationNode resultNode = lae.run(mulNode);

        assertMatrixEquals(expected, resultNode.getMatrix());
    }

    /**
     * בדיקת פעולת שלילה (Negate)
     * -A
     */
    @Test
    public void testNegate() {
        double[][] m = {{1, -2}, {0, 5}};
        double[][] expected = {{-1, 2}, {0, -5}};

        ComputationNode nodeM = new ComputationNode(m);
        ComputationNode negNode = new ComputationNode(ComputationNodeType.NEGATE, Arrays.asList(nodeM));

        ComputationNode resultNode = lae.run(negNode);

        assertMatrixEquals(expected, resultNode.getMatrix());
    }

    /**
     * בדיקת שחלוף (Transpose)
     * A^T
     */
    @Test
    public void testTranspose() {
        double[][] m = {
                {1, 2, 3},
                {4, 5, 6}
        }; // 2x3
        double[][] expected = {
                {1, 4},
                {2, 5},
                {3, 6}
        }; // 3x2

        ComputationNode nodeM = new ComputationNode(m);
        ComputationNode transNode = new ComputationNode(ComputationNodeType.TRANSPOSE, Arrays.asList(nodeM));

        ComputationNode resultNode = lae.run(transNode);

        assertMatrixEquals(expected, resultNode.getMatrix());
    }

    /**
     * בדיקת עץ חישוב מורכב
     * (A + B)^T
     */
    @Test
    public void testComplexTree() {
        double[][] a = {{1, 2}, {3, 4}};
        double[][] b = {{2, 3}, {4, 5}};
        // Sum = {{3, 5}, {7, 9}}
        // Transpose = {{3, 7}, {5, 9}}
        double[][] expected = {{3, 7}, {5, 9}};

        ComputationNode nodeA = new ComputationNode(a);
        ComputationNode nodeB = new ComputationNode(b);
        ComputationNode addNode = new ComputationNode(ComputationNodeType.ADD, Arrays.asList(nodeA, nodeB));
        ComputationNode rootNode = new ComputationNode(ComputationNodeType.TRANSPOSE, Arrays.asList(addNode));

        ComputationNode resultNode = lae.run(rootNode);

        assertMatrixEquals(expected, resultNode.getMatrix());
    }

    /**
     * בדיקה קריטית: Associative Nesting
     * בדיקה שהמנוע יודע להתמודד עם חיבור של 3 מטריצות
     * A + B + C
     */
    @Test
    public void testAssociativeNesting() {
        double[][] m1 = {{1, 1}, {1, 1}};
        double[][] m2 = {{2, 2}, {2, 2}};
        double[][] m3 = {{3, 3}, {3, 3}};
        double[][] expected = {{6, 6}, {6, 6}};

        ComputationNode node1 = new ComputationNode(m1);
        ComputationNode node2 = new ComputationNode(m2);
        ComputationNode node3 = new ComputationNode(m3);

        // יצירת רשימה עם 3 ילדים (מצב לא בינארי)
        List<ComputationNode> children = new ArrayList<>();
        children.add(node1);
        children.add(node2);
        children.add(node3);

        ComputationNode addNode = new ComputationNode(ComputationNodeType.ADD, children);

        // ה-run אמור להפעיל associativeNesting פנימית ולהסתדר עם זה
        ComputationNode resultNode = lae.run(addNode);

        assertMatrixEquals(expected, resultNode.getMatrix());
    }

    /**
     * בדיקת שגיאה: כפל עם מימדים לא תואמים
     */
    @Test
    public void testInvalidMultiplicationDimensions() {
        double[][] a = {{1, 2, 3}, {4, 5, 6}}; // 2x3
        double[][] b = {{1, 2}, {3, 4}};       // 2x2 (Cannot multiply 2x3 by 2x2)

        ComputationNode nodeA = new ComputationNode(a);
        ComputationNode nodeB = new ComputationNode(b);
        ComputationNode mulNode = new ComputationNode(ComputationNodeType.MULTIPLY, Arrays.asList(nodeA, nodeB));

        // מצפים ל-RuntimeException או IllegalArgumentException
        assertThrows(RuntimeException.class, () -> {
            lae.run(mulNode);
        });
    }

    // --- Helper Methods ---

    private void assertMatrixEquals(double[][] expected, double[][] actual) {
        assertNotNull(actual, "Result matrix is null");
        assertEquals(expected.length, actual.length, "Row count mismatch");
        if (expected.length > 0) {
            assertEquals(expected[0].length, actual[0].length, "Column count mismatch");
        }

        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < expected[0].length; j++) {
                assertEquals(expected[i][j], actual[i][j], DELTA,
                        "Mismatch at [" + i + "][" + j + "]");
            }
        }
    }
}