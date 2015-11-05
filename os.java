import java.util.*;
import java.lang.System.*;

public class os{
	static LinkedList<Job> jobsInDrum;//is a list of jobs in the drum
	static Map<Integer, Job> jobsInMemory; //is List of all the jobs in memory
	static LinkedList<FreeSpaceEntry> freeSpaceTable;//is a list of free spaces entrees
	static Queue<Job> cpuQueue;//a queue of jobs on the cpuQueue
	static boolean cpuRunningJob;//true or false whether a job is using the cpu at the moment
	static boolean ioRunningJob; //true or false to indicate whether a job is using IO
	static Job jobOnCPU;//contains the job that is currently using the cpu
	static LinkedList<Job> jobsBeingSwapped;
	//static Job jobOnDisk;//contains the current job that is running I/O on the disk //Will be removed. Current job will always be top of queue
	static Queue<Job> diskQueue;//contains jobs that are waiting to use the disk for I/O

	//SOS's main calls the startup() function first
	//this is a good palce to add any variable/objects that need to be allocated/initialized
	static void startup(){
		sos.ontrace();
		jobsInDrum=new LinkedList<Job>();
		jobsInMemory = new HashMap<Integer, Job>();
		jobsBeingSwapped = new LinkedList<Job>();
		freeSpaceTable=new LinkedList<FreeSpaceEntry>();
		freeSpaceTable.add(new FreeSpaceEntry(0,100));//start up free space table to contain one free space of 100k, at block 0 
		cpuQueue=new LinkedList<Job>();
		diskQueue=new LinkedList<Job>();
		cpuRunningJob=false;
	}

	
	//when SOS has a new job moved to drum, Crint() is an interrupt that is called, and contains information about the job
	//the job information is taken off the p[] array, and added to a job object
	//the job object is added to a linkedList of jobs located in the drum called jobsInDrum
	//if this is the first job to enter the system, you can attempt to move job into memoery using tryMovingJobToMemory()
	static void Crint(int []a, int []p){
		Job newJobInDrum=new Job(p[1],p[2],p[3],p[4]);
		jobsInDrum.add(newJobInDrum);
		tryMovingJobToMemory();
	}


	//SOS calls this interrupt when a job was successfully moved from memory to disk(for I/O)
	//this means you can try to put the next job on the diskQueue to use the disk
	//also since Dskint was initiated, that means the program is not done with the CPU and has to be added back on the cpuQueue
	//diskQueue works in FCFS
	static void Dskint(int []a, int []p){
		//@AR Job at tob of I/O (DISK Queue) finished I/O
		if(ioRunningJob){
			Job job = diskQueue.poll();	//Remove the last executed I/O job
			if(job.blocked && !diskQueue.contains(job)){
				//If this job was previously blocked until all I/O was completed, we will unblock now
				job.blocked = false;
			}
		}
		if(diskQueue.size() > 0){
			Job nextJob = diskQueue.peek();
			ioRunningJob = true;
			sos.siodisk(nextJob.jobNumber);
		}else{
			ioRunningJob = false;
		}
	}


	//SOS invokes this interrups when a job is loaded from drum into memory successfully
	//therefore, this means the system is ready for another attempt to move something from drum into memory, and onto the cpuQueue
	//and you can attempt to move something from the cpuQueue to using the cpu
	//the scheduler() only attempts to make the move if nothing is running on the cpu at the moment by looking at the boolean jobOnCPU 
	static void Drmint(int []a, int []p){
		//@Ar successfuly brought job to Drum
		//p[5] is the current time of the job
		Job job = jobsBeingSwapped.remove();
		FreeSpaceEntry.deleteEntry(job.block, job.jobSize, freeSpaceTable);
		job.usedTime = p[5];
		cpuQueue.add(job);
		jobsInMemory.put(job.jobNumber, job);
		tryMovingJobToMemory();	//Try Moving the next job
		scheduler(a,p);			//try scheduling the job
	}

	
	//stand for time runs out
	//will be later edited for dealing with time sliced
	//for now will be called when a process finishes with the cpu and wants to terminate, since the scheduler is in FCFS
	static void Tro(int []a, int []p){
		Job job = cpuQueue.peek();
		if(job != null){
			if(p[5] >= job.maxCPUTime){
				//Job has used the maximum CPU TIME
				terminate (job.jobNumber);
			}else{
				//Job has used its current timeslice
				cpuQueue.poll();	//remove from top of the queue
				cpuQueue.add(job);	//add at tail of the queue
				//TODO:SCHEDULE NEXT JOB
			}
		}
	}

	//stands for supervisor call
	//when a=5 then job wants to terminate
	//when a=6 then job wants to move to disk and use i/o
	//when a=7 then job wants to be blocked (change boolean blocked, of current job, to true
	//or else it is called when an I/O request is done
	static void Svc(int []a, int []p){
		cpuRunningJob=false;	
		if(a[0]==5){
			terminate(cpuQueue.peek().jobNumber);
		}
		else if(a[0]==6){
			diskQueue.add(jobOnCPU);//add job on cpu to diskQueue
			if(!ioRunningJob){
				//This is the case when no more jobs were on io queue and we need to start the process
				//ourselves. If there are other jobs on the queue, this job will be processed on first come, first serve.
				Dskint(a, p);
			}
		}
		else if(a[0]==7){
			//Job requests to be blocked
			//First make sure that there is at least one IO request in queue fot this job
			//If not, then ignore
			for(Job j : diskQueue){
				if(p[1] == j.jobNumber){
					j.blocked = true;
				}
			}
		}
	}

	//Termination
	//Remove job from queues, if exist
	static void terminate(int jobID) {
		Job job = jobsInMemory.get(jobID);
		if(job !=  null){
			freeSpaceTable.add(new FreeSpaceEntry(job.block, job.jobSize));
			cpuQueue.remove(job);
			diskQueue.remove(job);	
		}
	}


	//this function attempts to move something from drum(backing store) to main memory
	//The algorithm for picking a job from backing store, and try to fit into memory, is as follows:
	//-search for the first highest priority job in the drum
	//-then invoke foundSpace() to see if space was found for that job
	//-if space is found, swapToMemory() is called to move that job into memory
	//-if space is not found, the job is added to a vector of jobs that can't fit into memory called jobsNotFitting.
	//           and try the next highest priority job in the drum.
	static boolean tryMovingJobToMemory(){
		Collections.sort(jobsInDrum);
		int base = -1;
		for(Job job : jobsInDrum){
			base = foundSpace(job);
			if(base >= 0){
				jobsInDrum.remove(job);
				swapToMemory(job, base);
				return true;
			}
		}
		return false;
	}


	//function takes a job passed to it(the job should be in the drum)
	//takes the location of the job in the linkedList(jobsInDrum)
	//takes the block/base that the job will be stored in main memory
	//and places the job passed into base location, by calling the SOS's siodrum() function
	//it places the job in a queue container calle cpuQueue
	//and since the job is no longer in the drum(backing store), it removes it from the drum
	static void swapToMemory(Job jobInDrum, int block){
		jobsBeingSwapped.add(jobInDrum);
		jobInDrum.requestedBlock=block;
		sos.siodrum(jobInDrum.jobNumber, jobInDrum.jobSize, block, 0);	
	}


	//checks the free space table if the job passed in the argument can fit a space in the free space table
	//returns the block/base found for it, and updates the free space table to subtract the size of the job passed to it 
	//if nothing found, returns a block/base value of -1(this value can never be)
	static int foundSpace(Job jobInDrum){
		int count=0;
		while(freeSpaceTable.size()>=count){//check first fit
			if(freeSpaceTable.get(count).size >= jobInDrum.jobSize){
				int tempBlock=freeSpaceTable.get(count).block;
				freeSpaceTable.get(count).size -= jobInDrum.jobSize;
				freeSpaceTable.get(count).block -=jobInDrum.jobSize;
				return tempBlock;
			}
		}
		return -1;
	}


	//first come first served
	//check if there is no job running on the cpu
	//if there isn't, pop a job of the cpu queue, and run it on the cpu by changing a[0] to 2 	
	static void scheduler(int []a, int []p){
                if(!cpuRunningJob){
                        Job jobToRun=cpuQueue.poll();//poll() if java equivelent of pop()
                        if(jobToRun!=null){
                                a[0]=2;
                                p[2]=jobToRun.block;
                                p[3]=jobToRun.jobSize;
                                p[4]=jobToRun.maxCPUTime;
                                cpuRunningJob=true;
                                jobOnCPU=jobToRun;//now there is a variable to hold information about the current job running
                        }
                }
	
	}
}
