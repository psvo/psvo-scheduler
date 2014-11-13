package psvo.job;

import javafx.util.Callback;

/**
 * Job group
 */
public class JobGroup {
    private int group;

    public JobGroup(int group) {
        this.group = group;
    }

    public int getGroupId() {
        return group;
    }
}
