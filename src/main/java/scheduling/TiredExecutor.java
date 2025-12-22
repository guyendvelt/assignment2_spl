package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        // TODO
        if (numThreads <= 0) {
            throw new IllegalArgumentException("numThreads should be bigger than 0");
        }
        workers = new TiredThread[numThreads];
        double factor = 0.5 + Math.random();
        for (int i = 0; i < numThreads; i++) {
            workers[i] = new TiredThread(i, factor);
            workers[i].start();
            idleMinHeap.add(workers[i]);
        }
    }

    public void submit(Runnable task) {
        // TODO
        inFlight.incrementAndGet();
        try {
            TiredThread currThread = idleMinHeap.take();
            Runnable wrappedTask = () -> {
                try {
                    task.run();
                } finally {
                    idleMinHeap.offer(currThread);
                    if (inFlight.decrementAndGet() == 0) {
                        synchronized (this) {
                            this.notifyAll();
                        }
                    }
                }
            };
            currThread.newTask(wrappedTask);
        } catch (InterruptedException e) {
            inFlight.decrementAndGet();
//            throw new RuntimeException(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public void submitAll(Iterable<Runnable> tasks)  {
        // TODO: submit tasks one by one and wait until all finish
        for (Runnable task : tasks) {
            submit(task);
        }
        synchronized (this) {
            while (inFlight.get() > 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {

                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        // TODO
        for (TiredThread worker : workers) {
            worker.shutdown();
        }
        for (TiredThread worker : workers) {
            worker.join();
        }
        idleMinHeap.clear();

    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        StringBuilder sb = new StringBuilder();
        sb.append("================ WORKER REPORT ================\n");
        double totalFatigue = 0;
        for (TiredThread worker : workers) {
            double fatigue = worker.getFatigue();
            totalFatigue += fatigue;

            long timeUsedMs = worker.getTimeUsed() / 1_000_000;
            long idleMs = worker.getTimeIdle() / 1_000_000;
            sb.append("Worker #").append(worker.getWorkerId())
                    .append(" | Fatigue: ").append(fatigue)
                    .append(" | Work Time: ").append(timeUsedMs).append(" ms")
                    .append(" | Idle Time: ").append(idleMs).append(" ms\n");
        }
        double averageFatigue = totalFatigue / workers.length;
        double sumSquaredDeviations = 0;
        for (TiredThread worker : workers) {
            double diff = worker.getFatigue() - averageFatigue;
            sumSquaredDeviations += (diff * diff);
        }
        sb.append("-----------------------------------------------\n");
        sb.append("Total Workers: ").append(workers.length).append("\n");
        sb.append("Average Fatigue: ").append(averageFatigue).append("\n");
        sb.append("Fairness Score (Variance): ").append(sumSquaredDeviations).append("\n");
        sb.append("===============================================\n");
        return sb.toString();
    }
}
