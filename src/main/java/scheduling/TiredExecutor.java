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
        if(numThreads <= 0){
            throw new IllegalArgumentException("numThreads should be bigger than 0");
        }
        workers = new TiredThread[numThreads];
        double factor = 0.5 + Math.random();
        for (int i = 0; i < numThreads; i++) {
            workers[i] =  new TiredThread(i, factor);
            workers[i].start();
            idleMinHeap.add(workers[i]);
        }
    }

    public void submit(Runnable task) throws InterruptedException {
        // TODO

        inFlight.incrementAndGet();
        TiredThread currThread = idleMinHeap.take();
        Runnable wrappedTask = () -> {
            try{
                task.run();
            }finally {
                idleMinHeap.offer(currThread);
                if(inFlight.decrementAndGet() == 0){
                    synchronized (this){
                        this.notifyAll();
                    }
                }
            }
        };
        currThread.newTask(wrappedTask);
    }

    public void submitAll(Iterable<Runnable> tasks) throws InterruptedException {
        // TODO: submit tasks one by one and wait until all finish
        for(Runnable task : tasks){
            submit(task);
        }
        synchronized (this){
            while(inFlight.get() > 0){
                this.wait();
            }
        }
    }

    public void shutdown() throws InterruptedException {
        // TODO
        for(TiredThread worker : workers){
            worker.shutdown();
        }
        idleMinHeap.clear();

    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        return null;
    }
}
