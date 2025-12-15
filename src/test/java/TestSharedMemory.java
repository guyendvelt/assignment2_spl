import memory.*;
import java.util.Arrays;

public class TestSharedMemory {

    public static void main(String[] args) {
        System.out.println("Starting Tests...");

        try {
            testBasicLogic();
            System.out.println("[V] Basic Logic Test Passed");
        } catch (Exception e) {
            System.out.println("[X] Basic Logic Test Failed: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            testConcurrency();
            System.out.println("[V] Concurrency Test Passed");
        } catch (Exception e) {
            System.out.println("[X] Concurrency Test Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * בדיקה פשוטה שבודקת שהטעינה והקריאה עובדות נכון לוגית
     */
    private static void testBasicLogic() {
        double[][] data = {
                {1.0, 2.0},
                {3.0, 4.0}
        };
        SharedMatrix matrix = new SharedMatrix(data);

        // בדיקת קריאה
        double[][] result = matrix.readRowMajor();
        if (!Arrays.deepEquals(data, result)) {
            throw new RuntimeException("Data mismatch! expected " + Arrays.deepToString(data) + " but got " + Arrays.deepToString(result));
        }

        // בדיקת אוריינטציה
        if (matrix.getOrientation() != VectorOrientation.ROW_MAJOR) {
            throw new RuntimeException("Wrong orientation!");
        }
    }

    /**
     * בדיקת עומס: תהליכון אחד כותב כל הזמן 0 ו-1, תהליכון שני קורא.
     * אם הקורא רואה מטריצה מעורבבת (חצי 0 וחצי 1) - הנעילה נכשלה.
     */
    private static void testConcurrency() throws InterruptedException {
        int size = 50;
        double[][] allZeros = new double[size][size]; // מטריצה של אפסים
        double[][] allOnes = new double[size][size];  // מטריצה של אחדים

        // מילוי המטריצה של האחדים
        for (int i = 0; i < size; i++) {
            Arrays.fill(allOnes[i], 1.0);
        }

        SharedMatrix matrix = new SharedMatrix(allZeros);

        // דגל לעצירת התהליכונים
        boolean[] keepRunning = {true};
        // דגל לכישלון
        boolean[] failed = {false};

        // --- Thread 1: Writer (מחליף כל הזמן בין אפסים לאחדים) ---
        Thread writer = new Thread(() -> {
            while (keepRunning[0]) {
                matrix.loadRowMajor(allOnes);
                // אופציונלי: השהייה קטנטנה כדי לתת לקורא הזדמנות להיכנס באמצע
                // try { Thread.sleep(1); } catch (InterruptedException e) {}
                matrix.loadRowMajor(allZeros);
            }
        });

        // --- Thread 2: Reader (בודק שהמטריצה עקבית) ---
        Thread reader = new Thread(() -> {
            while (keepRunning[0]) {
                double[][] res = matrix.readRowMajor();

                // בדיקה: כל התאים חייבים להיות זהים לתא הראשון (0.0 או 1.0)
                // אם יש ערבוב - סימן שקראנו באמצע כתיבה!
                double val = res[0][0];
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        if (res[i][j] != val) {
                            failed[0] = true;
                            keepRunning[0] = false;
                            System.err.println("CRITICAL FAILURE: Inconsistent read! Found " + res[i][j] + " but expected " + val);
                            return;
                        }
                    }
                }
            }
        });

        writer.start();
        reader.start();

        // נותנים לזה לרוץ 2 שניות
        Thread.sleep(2000);

        // עוצרים
        keepRunning[0] = false;
        writer.join();
        reader.join();

        if (failed[0]) {
            throw new RuntimeException("Concurrency check failed! See console for details.");
        }
    }
}