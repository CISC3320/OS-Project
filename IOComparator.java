import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

//for sorting jobs in the diskQueue
//enables to push all the swapped out I/O requests to the end
//and give priority to jobs that are not blocked, and push those to the top of the disQueue
class IOComparator implements Comparator<Job> {
	@Override
	public int compare(Job job1, Job job2) {
        int priority1=0, priority2=0;
            if(job1.blocked)
                priority1=-1;
            if(job2.blocked)
                priority2=-1;
            if(job1.jobSwappedOut)
                priority1=-2;
            if(job2.jobSwappedOut)
                priority2=-2;
        return (priority1<priority2)? 1: (priority1>priority2)? -1: 0;
	}
}
