package psvo.scheduler;

import psvo.job.Job;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Job Scheduler
 */
public class Scheduler {

    Logger logger = Logger.getLogger(Scheduler.class.getName());

    private final static int LOCK_TIMEOUT = 1000;

    private final SchedQueue schedRxQueue = new SchedQueue();
    private final SchedQueue execRxQueue = new SchedQueue();
    private final SchedQueue schedTxQueue = new SchedQueue();
    private final SchedQueue execTxQueue = new SchedQueue();

    private final Set<Integer> groupSet = new HashSet<Integer>();
    private int typeFlag = 0;

    private Thread requestSchedulerThread;
    private Thread confirmSchedulerThread;

    // debugging
    private final static AtomicInteger sequence = new AtomicInteger(0);

    private static abstract class SubScheduler implements Runnable {
        protected final Scheduler scheduler;

        public SubScheduler(Scheduler scheduler) {
            this.scheduler = scheduler;
        }

        public boolean addToGroupSet(Job job) {
            int groupId = job.getJobGroup().getGroupId();
            if (!scheduler.groupSet.contains(groupId)) {
                scheduler.groupSet.add(groupId);
                return true;
            } else {
                return false;
            }
        }

        public void removeFromGroupSet(Job job) {
            scheduler.groupSet.remove(job.getJobGroup().getGroupId());
        }

        public boolean addTypeFlag(Job job) {
            int flag = job.getJobFlag();
            assert flag == 1 || flag == -1;
            if (scheduler.typeFlag == 0 || Integer.signum(scheduler.typeFlag) == flag) {
                scheduler.typeFlag += flag;
                return true;
            } else {
                return false;
            }
        }

        public void removeTypeFlag(Job job) {
            int flag = job.getJobFlag();
            assert flag == 1 || flag == -1;
            assert Integer.signum(scheduler.typeFlag) == flag;
            scheduler.typeFlag -= flag;
        }
    }

    private static class RequestScheduler extends SubScheduler {
        public RequestScheduler(Scheduler scheduler) {
            super(scheduler);
        }
        @Override
        public void run() {
            try {
                Job job;
                boolean groupOk;
                boolean typeFlagOk;

                while (true) {
                    scheduler.logger.info("getting job from schedRxQueue");
                    job = scheduler.schedRxQueue.take();
                    scheduler.logger.info("got job from schedRxQueue " + job.toString());

                    job.sequenceNumber = sequence.addAndGet(1);

                    synchronized (scheduler) {
                        do {
                            groupOk = addToGroupSet(job);
                            if (!groupOk) {
                                scheduler.wait();
                            }
                        } while (!groupOk);

                        do {
                            typeFlagOk = addTypeFlag(job);
                            if (!typeFlagOk) {
                                scheduler.wait();
                            }
                        } while (!typeFlagOk);
                    }

                    scheduler.logger.info("putting job to execRxQueue " + job.toString());
                    scheduler.execRxQueue.put(job);
                    scheduler.logger.info("put job to execRxQueue " + job.toString());
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private static class ConfirmScheduler extends SubScheduler {
        public ConfirmScheduler(Scheduler scheduler) {
            super(scheduler);
        }
        @Override
        public void run() {
            Job job;
            try {
                while (true) {
                    scheduler.logger.info("getting job from execTxQueue");
                    job = scheduler.execTxQueue.take();
                    scheduler.logger.info("got job from execTxQueue " + job.toString());

                    synchronized (scheduler) {
                        removeTypeFlag(job);
                        removeFromGroupSet(job);
                        scheduler.notify();
                    }

                    synchronized (scheduler.schedTxQueue) {
                        if (scheduler.schedTxQueue.remainingCapacity() == 0) {
                            scheduler.schedTxQueue.notifyAll();
                            scheduler.schedTxQueue.wait(LOCK_TIMEOUT);
                        }
                        if (scheduler.schedTxQueue.isEmpty()) {
                            scheduler.schedTxQueue.notifyAll();
                        }
                        if (!scheduler.schedTxQueue.offer(job, LOCK_TIMEOUT, TimeUnit.MILLISECONDS)) {
                            scheduler.logger.info("job confirmation to client aborted " + job.toString());
                        }
                    }
                }
            } catch (InterruptedException e) {
            }
        }
    }

    private Scheduler() {
    }

    public static Scheduler getScheduler() {
        Scheduler scheduler = new Scheduler();

        scheduler.requestSchedulerThread = new Thread(new RequestScheduler(scheduler));
        scheduler.confirmSchedulerThread = new Thread(new ConfirmScheduler(scheduler));

        scheduler.requestSchedulerThread.start();
        scheduler.confirmSchedulerThread.start();

        return scheduler;
    }

    public void interrupt() {
        requestSchedulerThread.interrupt();
        confirmSchedulerThread.interrupt();
    }

    public void join() throws InterruptedException {
        requestSchedulerThread.join();
        confirmSchedulerThread.join();

        assert typeFlag == 0;
        assert groupSet.isEmpty();
        assert schedRxQueue.isEmpty();
        assert execRxQueue.isEmpty();
        assert execTxQueue.isEmpty();

        logger.info("scheduler is correctly terminated");
    }

    public void requestJob(Job job) throws InterruptedException {
        logger.info("putting job to scheduling queue " + job.toString());
        schedRxQueue.put(job);
        logger.info("job put to scheduling queue " + job.toString());
    }

    public Job getJob() throws InterruptedException {
        logger.info("retrieving job from execution queue");
        Job job = execRxQueue.take();
        logger.info("job retrieved from execution queue " + job.toString());
        return job;
    }

    public void confirmJob(Job job) throws InterruptedException {
        logger.info("sending job confirmation " + job.toString());
        execTxQueue.put(job);
        logger.info("job confirmation sent " + job.toString());
    }

    public Job getConfirmation(Job job) throws InterruptedException {
        synchronized (schedTxQueue) {
            while (true) {
                if (job.equals(schedTxQueue.peek())) {
                    schedTxQueue.notifyAll();
                    return schedTxQueue.take();
                } else {
                    schedTxQueue.wait();
                }
            }
        }
    }
}