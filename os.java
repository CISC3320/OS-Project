import java.util.*;

public class os{
	static LinkedList<Job> jobsInDrum;//is a list of jobs in the drum
	static LinkedList<FreeSpaceEntry> freeSpaceTable;//is a list of free spaces entrees
	static Job jobBeingSwapped;
    
	static Queue<Job> cpuQueue;//a queue of jobs on the cpuQueue
	static Queue<Job> diskQueue;//contains jobs that are waiting to use the disk for I/O
    
	static Map<Integer, Job> jobsInMemory; //is List of all the jobs in memory
    
	
	static boolean cpuRunningJob=false;//true or false whether a job is using the cpu at the moment
	static boolean ioRunningJob=false; //true or false to indicate whether a job is using IO
    static boolean jobMovingToMemory=false;//true or false if a job is being moved into memory
    
    static final int timeslice=3;
    
	/*
	 * Initialize the Variables. Think of it as power on for OS
	 */
	static void startup(){
		sos.ontrace();
		jobsInDrum=new LinkedList<Job>();
		jobsInMemory = new HashMap<Integer, Job>();
		//jobsBeingSwapped = new LinkedList<Job>();
		freeSpaceTable=new LinkedList<FreeSpaceEntry>();
		freeSpaceTable.add(new FreeSpaceEntry(0,100));//start up free space table to contain one free space of 100k, at block 0 
		cpuQueue=new LinkedList<Job>();
		diskQueue=new LinkedList<Job>();
	}
    
	/*
     /when SOS has a new job moved to drum, Crint() is an interrupt that is called, and contains information about the job
     /the job information is taken off the p[] array, and added to a job object
     /the job object is added to a linkedList of jobs located in the drum called jobsInDrum
     if this is the first job to enter the system, you can attempt to move job into memoery using tryMovingJobToMemory()
     */
	static void Crint(int[] a, int[] p){
        System.out.println("INSIDE CRINT");
		//@AR: Completed
		Job newJobInDrum = new Job(p[1], p[2], p[3], p[4]); //Initialize the new job
		jobsInDrum.add(newJobInDrum);	//Add job to list indicating jobs in the Drum
		tryMovingJobToMemory();			//Try Moving the job into memory
        
        if(cpuQueue.size()>0 && cpuQueue.peek().lastSceduledTime!=-1){
            cpuQueue.peek().usedTime+=p[5]-cpuQueue.peek().lastSceduledTime;
            System.out.println("CPU TIME Used by job "+cpuQueue.peek().jobNumber+": "+cpuQueue.peek().usedTime);
        }
        scheduler(a,p);				//We will try to schedule the next Job
	}
    
    
	//SOS calls this interrupt when a job was successfully moved from memory to disk(for I/O)
	//this means you can try to put the next job on the diskQueue to use the disk
	//also since Dskint was initiated, that means the program is not done with the CPU and has to be added back on the cpuQueue
	//diskQueue works in FCFS
	//@AR Job at tob of I/O (DISK Queue) finished I/O
	static void Dskint(int[] a, int[] p){
		//@AR: Completed
        System.out.println("INSIDE DISKINT");
		if(ioRunningJob){
			Job job = diskQueue.poll();	//Remove the last executed I/O job
			if(job.blocked && !diskQueue.contains(job)){
				//If this job was previously blocked until all I/O was completed, we will unblock now
				job.blocked = false;
			}else if (!diskQueue.contains(job) && job.killThisJob){
				//Indicated if the job is ready to be killed pending all IO operations
				terminate(job.jobNumber);
			}
            else{

                job.usedTime+=p[5]-job.lastSceduledTime;
                /*
                 print cpu time for testing, delete later
                 */
                System.out.println("CPU TIME Used by job "+cpuQueue.peek().jobNumber+": "+cpuQueue.peek().usedTime);
            }
		}
		if(diskQueue.size() > 0){
			Job nextJob = diskQueue.peek();
			ioRunningJob = true;
			sos.siodisk(nextJob.jobNumber);
		}else{
			ioRunningJob = false;
		}
		scheduler(a, p);
	}
    
    
	//SOS invokes this interrups when a job is loaded from drum into memory successfully
	//therefore, this means the system is ready for another attempt to move something from drum into memory, and onto the cpuQueue
	//and you can attempt to move something from the cpuQueue to using the cpu
	//the scheduler() only attempts to make the move if nothing is running on the cpu at the moment by looking at the boolean jobOnCPU 
	static void Drmint(int []a, int []p){
        jobMovingToMemory=false;//because a job was just finished with laoding into memory*******************newly added
  		Job job = jobBeingSwapped;
		job.block = job.requestedBlock;
        FreeSpaceEntry.deleteEntry(job.block, job.jobSize, freeSpaceTable);
		cpuQueue.add(job);
		jobsInMemory.put(job.jobNumber, job);
        if(jobsInDrum.size()>0)
            tryMovingJobToMemory();	// Try Moving the next job
        
        System.out.println("INSIDE DMRINT");
        if(cpuQueue!=null && cpuQueue.peek().lastSceduledTime!=-1/*cpuQueue.size()>0*/){
            cpuQueue.peek().usedTime+=p[5]-cpuQueue.peek().lastSceduledTime;
            System.out.println("CPU TIME Used by job "+cpuQueue.peek().jobNumber+": "+cpuQueue.peek().usedTime);
        }

		scheduler(a,p);			// try scheduling the job
	}
	
	//stand for time runs out
	//will be later edited for dealing with time sliced
	//for now will be called when a process finishes with the cpu and wants to terminate, since the scheduler is in FCFS
	static void Tro(int []a, int []p){
        System.out.println("INSIDE TRO");
		Job job = cpuQueue.peek();
        job.usedTime += p[5]-job.lastSceduledTime;
        System.out.println("CPU TIME Used by job "+cpuQueue.peek().jobNumber+": "+cpuQueue.peek().usedTime);
		if(job != null){
			if(job.usedTime >= job.maxCPUTime){
				//Job has used the maximum CPU TIME
                terminate (job.jobNumber);
			}
            else{
				//Job has used its current timeslice
				cpuQueue.poll();	//remove from top of the queue
				cpuQueue.add(job);	//add at tail of the queue
            }
		}
        /*
         print cpu time for testing, delete later
         */

		scheduler(a, p); //Try to schedule the next Job
	}
	
	//stands for supervisor call
	//when a=5 then job wants to terminate
	//when a=6 then job wants to move to disk and use i/o
	//when a=7 then job wants to be blocked (change boolean blocked, of current job, to true
	//or else it is called when an I/O request is done
	static void Svc(int[] a, int[] p){
        cpuQueue.peek().usedTime += p[5]-cpuQueue.peek().lastSceduledTime;
        
        /*
         print cpu time for testing, delete later
         */
        System.out.println("INSIDE SVC");
        System.out.println("CPU TIME Used by job "+cpuQueue.peek().jobNumber+": "+cpuQueue.peek().usedTime);
        
		if(a[0]==5){
			terminate(cpuQueue.peek().jobNumber);
		}else if(a[0]==6){
			Job job = cpuQueue.poll();
			cpuQueue.add(job);
			diskQueue.add(job);//add job on cpu to diskQueue
			if(!ioRunningJob){
				//This is the case when no more jobs were on io queue and we need to start the process
				//ourselves. If there are other jobs on the queue, this job will be processed on first come, first serve.
				Dskint(a, p);
			}
		}else if(a[0]==7){
			//Job requests to be blocked
			//First make sure that there is at least one IO request in queue fot this job
			//If not, then ignore
			Job j = cpuQueue.peek();
			if(diskQueue.contains(j)){
				j.blocked = true;
			}
		}
		scheduler(a, p);
	}
    
	//Termination
	//Remove job from queues, if exist
	static void terminate(int jobID) {
		Job job = jobsInMemory.get(jobID);
		if(diskQueue.contains(job)){
			job.killThisJob = true;
		}else if(job !=  null){
			freeSpaceTable.add(new FreeSpaceEntry(job.block, job.jobSize));
            FreeSpaceEntry.compactBlocks(freeSpaceTable);
			cpuQueue.remove(job);
		}
        tryMovingJobToMemory();
	}
    
    
	//this function attempts to move something from drum(backing store) to main memory
	//The algorithm for picking a job from backing store, and try to fit into memory, is as follows:
	//search for the first highest priority job in the drum
	//then invoke foundSpace() to see if space was found for that job
	//if space is found, swapToMemory() is called to move that job into memory
	//if space is not found, the job is added to a vector of jobs that can't fit into memory called jobsNotFitting.
	//           and try the next highest priority job in the drum.
	static boolean tryMovingJobToMemory(){
        if(!jobMovingToMemory){
            Collections.sort(jobsInDrum);
            int block = -1;
            for(Job job : jobsInDrum){
                block = findSpace(job);
                if(block >= 0){
                    jobsInDrum.remove(job);
                    swapToMemory(job, block);
                    return true;
                }
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
		jobBeingSwapped = jobInDrum;
		jobInDrum.requestedBlock=block;
        jobMovingToMemory=true;
		sos.siodrum(jobInDrum.jobNumber, jobInDrum.jobSize, block, 0);
	}
    
	//checks the free space table if the job passed in the argument can fit a space in the free space table
	//returns the block/base found for it, and updates the free space table to subtract the size of the job passed to it 
	//if nothing found, returns a block/base value of -1(this value can never be)
	static int findSpace(Job jobInDrum){
		for(FreeSpaceEntry e : freeSpaceTable){
			if(e.size >= jobInDrum.jobSize){
				return e.block;
			}
		}
		return -1;
	}
    
    
	//first come first served
	//check if there is no job running on the cpu
	//if there isn't, pop a job of the cpu queue, and run it on the cpu by changing a[0] to 2 	
	static void scheduler(int []a, int []p){
		//printAll();
        Job firstJob = cpuQueue.peek();
        Job jobToRun=firstJob;
        while(jobToRun !=null && (jobToRun.blocked || jobToRun.killThisJob)){
            cpuQueue.add(cpuQueue.poll());
            jobToRun = cpuQueue.peek();
            if(jobToRun == firstJob){
                jobToRun = null;
            }
        }
        if(jobToRun!=null){
            a[0]=2;
            p[2]=jobToRun.block;
            p[3]=jobToRun.jobSize;
            if((jobToRun.maxCPUTime-jobToRun.usedTime)<timeslice)
                p[4]=jobToRun.maxCPUTime-jobToRun.usedTime;
            else
                p[4]=timeslice;
            cpuRunningJob=true;
            //System.out.println("AR-Slice Job time used by job "+jobToRun.jobNumber+":"+jobToRun.usedTime+"\nScheduled timeslice:"+p[4]);
            
            jobToRun.lastSceduledTime = p[5];
            
        }else{
            a[0]=1;
        }
	}
    
	static void printAll(){
		System.out.println("OS Statistics");
		System.out.print("\n\nJobs in Cpu queue\njobNumber\tpriority\tjobSize\t"
                         + "maxCPUTime\tusedTime\tlastSceduledTime"+
                         "\tblocked\tkillThisJob\n");
		for(Job job : cpuQueue){
			System.out.print(job.jobInfo()+"\n");
		}
		System.out.print("\n\nJobs in IO queue\njobNumber\tpriority\tjobSize\t"
                         + "maxCPUTime\tusedTime\tlastSceduledTime"+
                         "\tblocked\tkillThisJob\n");
		for(Job job : diskQueue){
			System.out.print(job.jobInfo()+"\n");
		}
		System.out.print("\n\n");
	}
}
