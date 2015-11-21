import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;


class Job implements Comparable<Job>{
	
	public int jobNumber, priority, jobSize, maxCPUTime, block, usedTime;
	public boolean blocked;//true of job requests to be blocked
	public long jobEntredTime;
	public int lastScheduledTime;
	public boolean killThisJob;
    public boolean cpuTimeAdded;//makes sure cpuTime is added only once per call of scheduler
    public boolean superPriority = false;
    
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
		jobEntredTime = System.currentTimeMillis();
	}
    
	void printJob(){//just for testing
		System.out.println("jobNumber: "+jobNumber +"\tpriority: "+priority +"\tBlock: "+block
				+"\tjobSize: "+jobSize+"\tmaxCPUTime: "+maxCPUTime
				+"\ttimeUsed: "+usedTime +"\tKillMe: "+killThisJob
				+"\tBlocked: "+blocked+"\tSuper: "+superPriority);

	}
    
	String jobInfo(){
		return jobNumber+"\t"+priority+"\t"+jobSize+"\t"
        +maxCPUTime+"\t"+usedTime+"\t"+lastScheduledTime
        +"\t"+blocked+"\t"+killThisJob;
	}
	
	@Override
	public int compareTo(Job o) {
		//This will result list from Collections.sort(List) sorted by Priority First and then time they entered
		/*
		int toReturn = this.priority > o.priority ? -1 : this.priority < o.priority ? 1 : 0;
		if(toReturn == 0){
			toReturn = this.jobEntredTime > o.jobEntredTime ? -1 : this.jobEntredTime < o.jobEntredTime ? 1 : 0;
			//toReturn = this.jobSize < o.jobSize ? -1 : this.jobSize > o.jobSize ? 1 : 0;
		}
		*/
		
		
		return (this.maxCPUTime-this.usedTime) < (o.maxCPUTime-o.usedTime) ? -1 : (this.maxCPUTime-this.usedTime > this.maxCPUTime-this.usedTime) ? 1 : 0;
	}
	
	public static void main(String[] args){
		LinkedList<Job> jobsInDrum = new LinkedList<Job>();
		jobsInDrum.add(new Job(1, 4, 50, 1));
		jobsInDrum.add(new Job(2, 1, 50, 32));
		jobsInDrum.add(new Job(3, 5, 50, 510));
		jobsInDrum.add(new Job(4, 3, 50, 2));
		jobsInDrum.add(new Job(5, 5, 20, 30));
		Collections.sort(jobsInDrum);
		for(Job job : jobsInDrum){
			job.printJob();
		}
		System.out.println(jobsInDrum.getLast().jobNumber);
		System.out.println(jobsInDrum.remove().jobNumber);
		System.out.println(jobsInDrum.remove().jobNumber);
	}
	
	public static void sortListBySizeAndBlocked(List<Job> uList){
		Collections.sort(uList, new SizeBlockComparator());
	}
}

class SizeBlockComparator implements Comparator<Job>{
	@Override
	public int compare(Job a, Job o) {
		//Will result in largest jobs Blocked First, then Size
		int toReturn  = a.blocked == o.blocked ? 0 : a.blocked ? 1 : -1;
		if(toReturn == 0){
			toReturn = a.jobSize > o.jobSize ? 1 : a.jobSize < o.jobSize ? -1 : 0;
		}
		return toReturn;
	}
}
