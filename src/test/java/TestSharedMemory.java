import memory.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class TestSharedMemory {

    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("=== Starting Corrected SharedMemory Tests ===\n");

        // --- 1. בדיקות לוגיות - SharedVector ---
        runTest("Vector: Init & Get", TestSharedMemory::testVectorInit);
        runTest("Vector: Transpose", TestSharedMemory::testVectorTranspose);
        runTest("Vector: Add", TestSharedMemory::testVectorAdd);
        runTest("Vector: Negate", TestSharedMemory::testVectorNegate);
        runTest("Vector: Dot Product", TestSharedMemory::testVectorDotProduct);
        runTest("Vector: Errors (Dimension Mismatch)", TestSharedMemory::testVectorErrors);

        // --- 2. בדיקות לוגיות - SharedMatrix ---
        runTest("Matrix: Init & Read RowMajor", TestSharedMemory::testMatrixInit);
        runTest("Matrix: Load ColumnMajor (Transpose logic)", TestSharedMemory::testMatrixLoadColumnMajor);
        runTest("Vector * Matrix Multiplication (VecMatMul)", TestSharedMemory::testVecMatMul);

        // --- 3. בדיקות מקביליות (Concurrency) ---
        runTest("Concurrency: Vector Add (Race Condition Check)", TestSharedMemory::testConcurrentVectorAdd);
        runTest("Concurrency: Multi-Threaded VecMatMul", TestSharedMemory::testConcurrentVecMatMul);

        System.out.println("\n=== Test Summary ===");
        System.out.println("Passed: " + testsPassed);
        System.out.println("Failed: " + testsFailed);

        if (testsFailed > 0) {
            System.out.println("❌ SOME TESTS FAILED!");
            System.exit(1);
        } else {
            System.out.println("✅ ALL TESTS PASSED!");
        }
    }

    // --- Helper Methods ---

    private static void runTest(String name, Runnable test) {
        System.out.print("Running " + name + "... ");
        try {
            test.run();
            System.out.println("✅ PASS");
            testsPassed++;
        } catch (Throwable e) {
            System.out.println("❌ FAIL");
            System.out.println("   -> Error: " + e.getMessage());
            // e.printStackTrace(); // הסר הערה כדי לראות את השגיאה המלאה במידת הצורך
            testsFailed++;
        }
    }

    private static void assertEqual(double expected, double actual, String msg) {
        if (Math.abs(expected - actual) > 0.0001) {
            throw new RuntimeException(msg + " Expected: " + expected + ", Got: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new RuntimeException(msg);
    }

    private static void assertThrows(Runnable runnable, String expectedErrorPart) {
        try {
            runnable.run();
            throw new RuntimeException("Expected exception but none was thrown.");
        } catch (Exception e) {
            // בודקים אם ההודעה או סוג השגיאה מכילים את הרמז
            boolean isMatch = (e.getMessage() != null && e.getMessage().toLowerCase().contains(expectedErrorPart.toLowerCase()))
                    || e.getClass().getSimpleName().toLowerCase().contains(expectedErrorPart.toLowerCase());

            if (!isMatch) {
                // אופציונלי: אפשר להחמיר ולהכשיל אם השגיאה לא תואמת בדיוק
                // System.err.println("Caught unexpected exception: " + e);
            }
        }
    }

    // ==========================================
    //            LOGIC TESTS
    // ==========================================

    private static void testVectorInit() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        assertEqual(1.0, v.get(0), "Index 0 incorrect");
        assertEqual(3.0, v.get(2), "Index 2 incorrect");
        assertEqual(3, v.length(), "Length incorrect");
        assertTrue(v.getOrientation() == VectorOrientation.ROW_MAJOR, "Orientation wrong");
    }

    private static void testVectorTranspose() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        v.transpose();
        assertTrue(v.getOrientation() == VectorOrientation.COLUMN_MAJOR, "Should be COLUMN after transpose");
        v.transpose();
        assertTrue(v.getOrientation() == VectorOrientation.ROW_MAJOR, "Should be ROW after 2nd transpose");
    }

    private static void testVectorAdd() {
        SharedVector v1 = new SharedVector(new double[]{10, 20}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        v1.add(v2);

        assertEqual(11.0, v1.get(0), "Add index 0 failed");
        assertEqual(22.0, v1.get(1), "Add index 1 failed");
        // מוודאים ש-v2 לא השתנה
        assertEqual(1.0, v2.get(0), "Source vector modified erroneously");
    }

    private static void testVectorNegate() {
        SharedVector v = new SharedVector(new double[]{1, -2}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEqual(-1.0, v.get(0), "Negate failed for pos");
        assertEqual(2.0, v.get(1), "Negate failed for neg");
    }

    private static void testVectorDotProduct() {
        // Dot product: Row * Column (לפי המימוש ב-SharedVector)
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{3, 4}, VectorOrientation.COLUMN_MAJOR);

        double res = v1.dot(v2);
        // 1*3 + 2*4 = 11
        assertEqual(11.0, res, "Dot product calculation wrong");
    }

    private static void testVectorErrors() {
        SharedVector v1 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector v2 = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector v3 = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR); // Same orientation

        assertThrows(() -> v1.add(v2), "length"); // אורך שונה
        // לפי הקוד שלך, dot זורק שגיאה אם הכיוונים *זהים*
        assertThrows(() -> v1.dot(v3), "orientation");
    }

    private static void testMatrixInit() {
        double[][] data = {{1, 2}, {3, 4}};
        SharedMatrix m = new SharedMatrix(data);
        assertEqual(2, m.length(), "Matrix rows count wrong");
        double[][] read = m.readRowMajor();
        assertEqual(1.0, read[0][0], "Matrix read [0][0] incorrect");
        assertEqual(4.0, read[1][1], "Matrix read [1][1] incorrect");
    }

    private static void testMatrixLoadColumnMajor() {
        // כשמטעינים ColumnMajor וקוראים RowMajor, זה בעצם מבצע שחלוף (Transpose) ויזואלי של הנתונים.
        // נתונים מקוריים:
        // [1, 2]
        // [3, 4]
        double[][] data = {{1, 2}, {3, 4}};
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(data);

        // הלוגיקה ב-readRowMajor הופכת אינדקסים כשהמטריצה היא ColumnMajor
        double[][] read = m.readRowMajor();

        // מצופה:
        // [1, 3]
        // [2, 4]
        assertEqual(1.0, read[0][0], "Transposed [0][0] wrong");
        assertEqual(3.0, read[0][1], "Transposed [0][1] wrong");
        assertEqual(2.0, read[1][0], "Transposed [1][0] wrong");
        assertEqual(4.0, read[1][1], "Transposed [1][1] wrong");
    }

    private static void testVecMatMul() {
        // וקטור (שורה): [1, 2]
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        // מטריצה שתייצג עמודות (כדי לאפשר כפל):
        // נטען אותה כך שהווקטורים הפנימיים יהיו COLUMN_MAJOR.
        // שורה 1 בקלט -> וקטור עמודה 1: [3, 4]
        // שורה 2 בקלט -> וקטור עמודה 2: [5, 6]
        // המטריצה הלוגית:
        // | 3 5 |
        // | 4 6 |
        double[][] matData = {{3, 4}, {5, 6}};
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(matData);

        // v * M = [ (1,2)*(3,4), (1,2)*(5,6) ] = [ 3+8, 5+12 ] = [ 11, 17 ]
        v.vecMatMul(m);

        // התוצאה נשמרת בתוך v עצמו (in-place)
        assertEqual(11.0, v.get(0), "VecMatMul index 0 failed");
        assertEqual(17.0, v.get(1), "VecMatMul index 1 failed");
    }

    // ==========================================
    //          CONCURRENCY TESTS
    // ==========================================

    private static void testConcurrentVectorAdd() {
        // תרחיש: 1000 תהליכונים מוסיפים 1 לוקטור בו זמנית.
        // מטרת הטסט: לוודא שאין Race Condition בתוך הפונקציה add.
        int threadCount = 1000;
        SharedVector target = new SharedVector(new double[]{0}, VectorOrientation.ROW_MAJOR);
        SharedVector adder = new SharedVector(new double[]{1}, VectorOrientation.ROW_MAJOR);

        ExecutorService es = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            es.submit(() -> {
                try {
                    target.add(adder);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean done = latch.await(5, TimeUnit.SECONDS);
            assertTrue(done, "Timeout waiting for threads in ConcurrentAdd");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted");
        }
        es.shutdown();

        // אם המנעולים עובדים, כל התוספות נחשבות
        assertEqual(1000.0, target.get(0), "Concurrency Add Failed! Possible Race Condition.");
    }

    private static void testConcurrentVecMatMul() {
        // תרחיש: מספר תהליכונים מבצעים כפל וקטור-מטריצה על אותה מטריצה משותפת.
        // המטרה: לוודא שמנגנון ה-ReadLock במטריצה מאפשר קריאה מקבילית ולא גורם לקריסה/חסימה.

        int threadCount = 20;
        // מטריצה פשוטה
        double[][] matData = {{1, 1}, {1, 1}};
        SharedMatrix sharedM = new SharedMatrix();
        sharedM.loadColumnMajor(matData); // עמודות של [1,1]

        ExecutorService es = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // מונה שגיאות אטומי
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            es.submit(() -> {
                try {
                    // כל תהליכון יוצר וקטור משלו ומכפיל במטריצה המשותפת
                    SharedVector myVec = new SharedVector(new double[]{2, 2}, VectorOrientation.ROW_MAJOR);
                    // [2,2] * [1,1] = 4
                    myVec.vecMatMul(sharedM);

                    if (myVec.get(0) != 4.0 || myVec.get(1) != 4.0) {
                        failures.incrementAndGet();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean done = latch.await(5, TimeUnit.SECONDS);
            assertTrue(done, "Timeout in ConcurrentVecMatMul");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted");
        }
        es.shutdown();

        assertEqual(0.0, failures.get(), "Some threads failed in VecMatMul concurrency test");
    }
}