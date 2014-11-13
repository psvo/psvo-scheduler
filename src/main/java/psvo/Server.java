package psvo;

import psvo.job.ComputeData;
import psvo.job.Job;
import psvo.job.JobGroup;
import psvo.scheduler.Scheduler;

/**
 * Created by petr on 11/12/14.
 */
public class Server implements Runnable {

    Scheduler scheduler;

    public Server(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Job job = scheduler.getJob();
                job.execute();
                scheduler.confirmJob(job);
            }
        } catch(InterruptedException e) {
        }
    }
}