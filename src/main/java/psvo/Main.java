package psvo;

import psvo.job.ComputeData;
import psvo.job.Job;
import psvo.job.JobGroup;
import psvo.scheduler.Scheduler;

import java.util.LinkedList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        Scheduler scheduler = Scheduler.getScheduler();
        try {
            List<Thread> server_threads = new LinkedList<Thread>();

            for (int i = 0; i < 10; i++) {
                Thread thread = new Thread(new Server(scheduler));
                thread.start();
                server_threads.add(thread);
            }

            List<Thread> client_threads = new LinkedList<Thread>();

            for (int i = 0; i < 1000; i++) {
                Thread thread = new Thread(new Client(scheduler));
                thread.start();
                client_threads.add(thread);
            }

            for (Thread t : client_threads) {
                t.join();
            }

//            Thread.sleep(1000);
            for (Thread t : server_threads) {
                t.interrupt();
                t.join();
            }

            scheduler.interrupt();
            scheduler.join();

        } catch (InterruptedException e) {
        }
    }
}
