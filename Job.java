import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;


class Job implements Comparable<Job>{
	
	public int jobNumber, priority, jobSize, maxCPUTime, block, usedTime/*total amount of cpu time used so far*/;
	public boolean blocked;//true of job requests to be blocked
    public int timeSliceUsed=0;//how much of the time slice a job used per cpu scheduler call
	public int lastScheduledTime;//last time job ran on the cpu
	public boolean killThisJob;//true if the job needs to be terminated
    public boolean cpuTimeAdded;//makes sure cpuTime is added only once per call of scheduler
    public boolean jobSwappedOut=false;//true if the job was in memory and then was kicked back to drum
    
	Job(int jobNumber, int priority, int jobSize, int maxCPUTime){
		this.jobNumber=jobNumber;
		this.priority=priority;
		this.jobSize=jobSize;
		this.maxCPUTime=maxCPUTime;
		usedTime = 0;
		blocked=false;
        cpuTimeAdded=false;
		block=-1;//when a new job is created it isnt assigned a block yet
        lastScheduledTime=-1;
	}
	
	@Override
	public int compareTo(Job o) {
        return (this.maxCPUTime-this.usedTime) < (o.maxCPUTime-o.usedTime) ? -1 : (this.maxCPUTime-this.usedTime > this.maxCPUTime-this.usedTime) ? 1 : 0;
	}
}
