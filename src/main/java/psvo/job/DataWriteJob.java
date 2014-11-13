package psvo.job;

/**
 * Data write job
 *
 */
public abstract class DataWriteJob extends Job {
    public DataWriteJob(JobGroup jobGroup) {
        super(jobGroup, -1);
    }
}