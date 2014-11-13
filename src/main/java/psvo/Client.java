package psvo;

import psvo.job.*;
import psvo.scheduler.Scheduler;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by petr on 11/12/14.
 */
public class Client implements Runnable {

    Scheduler scheduler;

    public Client(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        try {
            for (int i = 0; i < 100; i++) {
                JobGroup jobGroup = new JobGroup(random.nextInt(10));
                Job job;
                switch (random.nextInt(4)) {
                    case 0:
                        job = new ComputeData(jobGroup);
                        break;
                    case 1:
                        job = new EvaluateData(jobGroup);
                        break;
                    case 2:
                        job = new InsertData(jobGroup);
                        break;
                    case 3:
                    default:
                        job = new UpdateData(jobGroup);
                        break;
                }
                scheduler.requestJob(job);
                scheduler.getConfirmation(job);
            }
        } catch(InterruptedException e) {
        }
    }
}