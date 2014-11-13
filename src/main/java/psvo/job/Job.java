package psvo.job;

import java.util.logging.Logger;

/**
 * Job base class
 */
public abstract class Job {
    private JobGroup jobGroup;
    private int jobFlag;
    public int sequenceNumber;

    public Job(JobGroup jobGroup, int jobFlag) {
        this.jobGroup = jobGroup;
        this.jobFlag = jobFlag;
    }

    public JobGroup getJobGroup() {
        return jobGroup;
    }

    public int getJobFlag() {
        return jobFlag;
    }

    public void execute() throws  InterruptedException {
        Logger logger = Logger.getLogger(getClass().getName());
        logger.info(String.format("executing job " + toString()));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "jobGroup=" + jobGroup.getGroupId() +
                ", jobFlag=" + jobFlag +
                '}';
    }
}
