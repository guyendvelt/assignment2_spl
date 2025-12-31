import scheduling.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class TestTiredThread {

    private TiredThread thread;

    @AfterEach
    void tearDown() {
        if (thread != null && thread.isAlive()) {
            thread.shutdown();
            try {
                thread.join(1000);
            } catch (InterruptedException e) {
               //do nothing
            }
        }
    }

    //Task Execution tests

    @Test
    @DisplayName("Execute single task")
    void testExecuteSingleTask() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        int[] counter = {0};
        thread.newTask(() -> counter[0]++);
        Thread.sleep(100);
        assertEquals(1, counter[0]);
    }


    @Test
    @DisplayName("Task execution increases timeUsed")
    void testTaskExecutionIncreasesTimeUsed() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        long initialTimeUsed = thread.getTimeUsed();
        double initialFatigue = thread.getFatigue();
        thread.newTask(() -> {
            for (int i = 0; i < 10000; i++) {
                Math.sqrt(i);
            }
        });
        Thread.sleep(100);
        assertTrue(thread.getTimeUsed() > initialTimeUsed);
        assertTrue(thread.getFatigue() > initialFatigue);

    }


    @Test
    @DisplayName("Higher fatigue factor results in higher fatigue")
    void testFatigueFactorAffectsFatigue() throws InterruptedException {
        TiredThread lowFatigueThread = new TiredThread(0, 0.5);
        TiredThread highFatigueThread = new TiredThread(1, 1.5);
        lowFatigueThread.start();
        highFatigueThread.start();
        Runnable tasks = () -> {
            for (int i = 0; i < 50000; i++) {
                Math.sqrt(i);
            }
        };
        lowFatigueThread.newTask(tasks);
        highFatigueThread.newTask(tasks);
        Thread.sleep(200);
        double lowFatigue = lowFatigueThread.getFatigue();
        double highFatigue = highFatigueThread.getFatigue();
        assertTrue(lowFatigue > 0);
        assertTrue(highFatigue > 0);
        assertTrue(highFatigue > lowFatigue);
        lowFatigueThread.shutdown();
        highFatigueThread.shutdown();
        lowFatigueThread.join(1000);
        highFatigueThread.join(1000);
    }

    void testCompareToOrdersByFatigue() throws InterruptedException {
        TiredThread t1 = new TiredThread(0, 1.0);
        TiredThread t2 = new TiredThread(1, 1.0);
        t1.start();
        t2.start();
        t1.newTask(() -> {
            for (int i = 0; i < 100000; i++) {
                Math.sqrt(i);
            }
        });
        Thread.sleep(200);
        assertTrue(t1.compareTo(t2) > 0);
        assertTrue(t2.compareTo(t1) < 0);
        t1.shutdown();
        t2.shutdown();
        t1.join(1000);
        t2.join(1000);
    }
    // Shutdown tests

    @Test
    @DisplayName("Shutdown stops the thread")
    void testShutdownStopsThread() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        assertTrue(thread.isAlive());
        thread.shutdown();
        thread.join(1000);
        assertFalse(thread.isAlive());
        thread = null;
    }

    // Error Handling tests
    @Test
    @DisplayName("newTask throws when thread is shutdown")
    void testNewTaskThrowsWhenShutdown() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        thread.shutdown();
        thread.join(1000);
        assertThrows(IllegalStateException.class, () -> thread.newTask(() -> {}));
        thread = null;
    }

    @Test
    @DisplayName("Thread handles RuntimeException in task")
    void testThreadHandlesRuntimeException() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        thread.newTask(() -> {
            throw new RuntimeException("Test exception");
        });
        Thread.sleep(200);
        assertFalse(thread.isAlive());
        thread = null;
    }

    //  Time's update tests
    @Test
    @DisplayName("timeIdle increases while waiting")
    void testTimeIdleIncreases() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        Thread.sleep(100);
        thread.newTask(() -> {});
        Thread.sleep(50);
        assertTrue(thread.getTimeIdle() > 0);
    }

     //  Matrix operations tasks testing
    @Test
    @DisplayName("Execute matrix negate task")
    void testExecuteMatrixNegateTask() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        memory.SharedMatrix matrix = new memory.SharedMatrix(new double[][]{{1, 2}, {3, 4}});
        thread.newTask(() -> matrix.get(0).negate());
        Thread.sleep(100);
        double[][] result = matrix.readRowMajor();
        assertEquals(-1.0, result[0][0], 0.0001);
        assertEquals(-2.0, result[0][1], 0.0001);
    }

    @Test
    @DisplayName("Execute vector dot product task")
    void testExecuteVectorDotProductTask() throws InterruptedException {
        thread = new TiredThread(0, 1.0);
        thread.start();
        memory.SharedVector row = new memory.SharedVector(new double[]{1, 2, 3}, memory.VectorOrientation.ROW_MAJOR);
        memory.SharedVector col = new memory.SharedVector(new double[]{4, 5, 6}, memory.VectorOrientation.COLUMN_MAJOR);
        double[] result = {0};
        thread.newTask(() -> result[0] = row.dot(col));
        Thread.sleep(100);
        // 1*4 + 2*5 + 3*6 = 4 + 10 + 18 = 32
        assertEquals(32.0, result[0], 0.0001);
    }


}

