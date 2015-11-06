import java.lang.System.*;
import java.util.Collections;
import java.util.LinkedList;

class Job implements Comparable<Job>{
	
	public int jobNumber, priority, jobSize, maxCPUTime, block, usedTime, requestedBlock;
	public boolean blocked;//true of job requests to be blocked
	public final long jobEntredTime;
	public boolean killThisJob;
	Job(int jobNumber, int priority, int jobSize, int maxCPUTime){
		this.jobNumber=jobNumber;
		this.priority=priority;
		this.jobSize=jobSize;
		this.maxCPUTime=maxCPUTime;
		blocked=false;
		block=-1;//when a new job is created it isnt assigned a block yet
		jobEntredTime = System.currentTimeMillis();
	}

	void printJob(){//just for testing
		System.out.println("jobNumber: "+jobNumber);
		System.out.println("priority: "+priority);
		System.out.println("jobSize: "+jobSize);
		System.out.println("maxCPUTime: "+maxCPUTime);
		System.out.println("timeUsed: "+maxCPUTime);
		System.out.println("timeEntered: "+jobEntredTime+"\n");
	}

	@Override
	public int compareTo(Job o) {
		//This will result list from Collections.sort(List) sorted by Priority First and then time they entered
		int toReturn = this.priority > o.priority ? -1 : this.priority < o.priority ? 1 : 0;
		if(toReturn == 0){
			toReturn = this.jobEntredTime > o.jobEntredTime ? -1 : this.jobEntredTime < o.jobEntredTime ? 1 : 0;
		}
		return toReturn;
	}
	
	public static void main(String[] args){
		LinkedList<Job> jobsInDrum = new LinkedList<Job>();
		jobsInDrum.add(new Job(1, 4, 50, 50));
		jobsInDrum.add(new Job(2, 1, 50, 50));
		jobsInDrum.add(new Job(3, 5, 50, 50));
		jobsInDrum.add(new Job(4, 3, 50, 50));
		jobsInDrum.add(new Job(5, 5, 20, 50));
		Collections.sort(jobsInDrum);
		for(Job job : jobsInDrum){
			job.printJob();
		}
		System.out.println(jobsInDrum.getLast().jobNumber);
		System.out.println(jobsInDrum.remove().jobNumber);
		System.out.println(jobsInDrum.remove().jobNumber);
	}
	
	
}
