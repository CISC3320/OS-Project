import java.lang.System.*;

class Job{
	public int jobNumber, priority, jobSize, maxCPUTime, block, usedTime;
	public boolean blocked;//true of job requests to be blocked
	Job(int jobNumber, int priority, int jobSize, int maxCPUTime){
		this.jobNumber=jobNumber;
		this.priority=priority;
		this.jobSize=jobSize;
		this.maxCPUTime=maxCPUTime;
		blocked=false;
		block=-1;//when a new job is created it isnt assigned a block yet
	}

	void printJob(){//just for testing
		System.out.println("jobNumber: "+jobNumber);
		System.out.println("priority: "+priority);
		System.out.println("jobSize: "+jobSize);
		System.out.println("maxCPUTime: "+maxCPUTime);
		System.out.println("timeUsed: "+maxCPUTime);
	}
}
