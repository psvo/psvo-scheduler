package psvo.scheduler;

import psvo.job.Job;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * Scheduler Queue
 *
 * Must be synchronized, operated from multiple threads
 */
class SchedQueue extends LinkedBlockingQueue<Job> {

    static final int MAX_CAPACITY = 100;

    public SchedQueue() {
        super(MAX_CAPACITY);
    }

}