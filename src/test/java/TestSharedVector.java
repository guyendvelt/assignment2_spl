import memory.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

class TestSharedVector {

    private static final double DELTA = 0.0001;
    //Initialization Tests
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

    // Transpose Tests

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
    @DisplayName("Multiple transposes")
    void testMultipleTransposes() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        for (int i = 0; i < 10; i++) {
            v.transpose();
        }
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        assertEquals(1.0, v.get(0), DELTA);
        assertEquals(2.0, v.get(1), DELTA);
        assertEquals(3.0, v.get(2), DELTA);
        assertEquals(3, v.length());

    }

    // Add Tests

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
    @DisplayName("Add large vectors")
    void testAddLargeVectors() {
        int size = 5000;
        double[] data1 = new double[size];
        double[] data2 = new double[size];
        for (int i = 0; i < size; i++) {
            data1[i] = i;
            data2[i] = 2*i;
        }
        SharedVector v1 = new SharedVector(data1, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(data2, VectorOrientation.ROW_MAJOR);
        v1.add(v2);
        for (int i = 0; i < size; i++) {
            assertEquals(3 * i, v1.get(i), DELTA);
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

    // Negate Tests
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

    //Dot Product Tests

    @Test
    @DisplayName("Basic dot product")
    void testBasicDotProduct() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.COLUMN_MAJOR);
        double res = v1.dot(v2);
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
        assertEquals(-14.0, v1.dot(v2), DELTA);
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

    // Vectors * Matrix Tests  (VecMatMull Function)

    @Test
    @DisplayName("Basic vector-matrix multiplication")
    void testBasicVecMatMul() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        double[][] matrixData = {{3, 4}, {5, 6}};
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(matrixData);
        v.vecMatMul(m);
        assertEquals(13.0, v.get(0), DELTA);
        assertEquals(16.0, v.get(1), DELTA);
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

    @Test
    @DisplayName(" multiple operations correctness")
    void testMultipleOperations() {
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
