package psvo.job;

/**
 * Data read job
 *
 */
public class DataReadJob extends Job {
    public DataReadJob(JobGroup jobGroup) {
        super(jobGroup, 1);
    }
}