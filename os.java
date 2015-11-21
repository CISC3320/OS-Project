import java.util.*;

public class os{
	static LinkedList<FreeSpaceEntry> freeSpaceTable;//is a list of free spaces entrees
	
	static LinkedList<Job> drumQueue;//is a list of jobs in the drum
	static LinkedList<Job> diskQueue;//contains jobs that are waiting to use the disk for I/O
	static LinkedList<Job> cpuQueue;//a queue of jobs on the cpuQueue

	static Job jobBeingSwapped = null;	//job that is either being swapped in or out of memory
	static Job jobDoingIO = null;
	static Job lastJobIn = null;
	
	static Map<Integer, Job> jobsInMemory; //is List of all the jobs in memory
    
	
	static boolean cpuRunningJob=false;//true or false whether a job is using the cpu at the moment
	static boolean ioRunningJob=false; //true or false to indicate whether a job is using IO
    static boolean jobMovingToMemory=false;//true or false if a job is being moved into memory
    static boolean jobMovingOutOfMemory=false;//true or false if a job is being moved into memory
    
    static final int timeslice=14;	//14 111 61
    static final int maxWithSamePriority=2;
    static final int[] priorityInQueue = new int[5];
    
    static int lastJobKicked = 0;
    
	/*
	 * Initialize the Variables. Think of it as power on for OS
	 */
	static void startup(){
		sos.offtrace();
		//sos.ontrace();
		drumQueue=new LinkedList<Job>();
		jobsInMemory = new HashMap<Integer, Job>();
		cpuQueue=new LinkedList<Job>();
		diskQueue=new LinkedList<Job>();
		freeSpaceTable=new LinkedList<FreeSpaceEntry>();
		freeSpaceTable.add(new FreeSpaceEntry(0,100));//start up free space table to contain one free space of 100k, at block 0 
	}
    
	/*
     /when SOS has a new job moved to drum, Crint() is an interrupt that is called, and contains information about the job
     /the job information is taken off the p[] array, and added to a job object
     /the job object is added to a linkedList of jobs located in the drum called jobsInDrum
     if this is the first job to enter the system, you can attempt to move job into memoery using tryMovingJobToMemory()
     */
	static void Crint(int[] a, int[] p){
        updateCPUused(p[5]);	//update cpu time
		Job newJobInDrum = new Job(p[1], p[2], p[3], p[4]); //Initialize the new job
		drumQueue.add(newJobInDrum);	//Add job to list indicating jobs in the Drum
		jobMigration();			//Try Moving the job into memory
        scheduler(a,p);				//We will try to schedule the next Job
	}
    
    static void jobMigration(){
        if(drumQueue.size()>0){
        	if(jobBeingSwapped == null){
        		if(!tryMovingJobToMemory()){
        			if(lastJobKicked > 1000){
        				if(tryMovingJobOutOfMemory()){
        					lastJobKicked = 0;
        				}
        			}
        		}
        	}
        }
    }
	
	//SOS calls this interrupt when a job was successfully moved from memory to disk(for I/O)
	//this means you can try to put the next job on the diskQueue to use the disk
	//also since Dskint was initiated, that means the program is not done with the CPU and has to be added back on the cpuQueue
	//diskQueue works in FCFS
	//@AR Job at tob of I/O (DISK Queue) finished I/O
	static void Dskint(int[] a, int[] p){
		//DirtyCode, will need to be refactord
		String callingMethod = Thread.currentThread().getStackTrace()[2].getMethodName(); 
		if(!callingMethod.equals("scheduler"))
        	updateCPUused(p[5]);	//Update cpu time
		if(ioRunningJob){
			Job job = jobDoingIO;
			diskQueue.remove(job);
			jobDoingIO = null;
			//System.out.println("Ar: Job "+job.jobNumber+" finished IO");
			if(job.blocked && !diskQueue.contains(job)){
				//If this job was previously blocked until all I/O was completed, we will unblock now
				job.blocked = false;
			}
			if (!diskQueue.contains(job) && job.killThisJob){
				//Indicated if the job is ready to be killed pending all IO operations
				terminate(job.jobNumber);
			}
		}
		if(diskQueue.size() > 0 && !ioRunningJob){
			for(int i = diskQueue.size()-1; i>=0; i--){
				Job nextJob = diskQueue.get(i);	
				if(jobsInMemory.containsKey(nextJob.jobNumber) && nextJob != jobBeingSwapped){
					ioRunningJob = true;
					jobDoingIO = nextJob;
					sos.siodisk(nextJob.jobNumber);
					break;
				}	
			}
			
		}else{
			ioRunningJob = false;
		}
		jobMigration();			//Try Moving the job into memory
		if(!callingMethod.equals("scheduler"))
		scheduler(a, p);
	}
    
    
	//SOS invokes this interrups when a job is loaded from drum into memory successfully
	//therefore, this means the system is ready for another attempt to move something from drum into memory, and onto the cpuQueue
	//and you can attempt to move something from the cpuQueue to using the cpu
	//the scheduler() only attempts to make the move if nothing is running on the cpu at the moment by looking at the boolean jobOnCPU 
	static void Drmint(int []a, int []p){
        updateCPUused(p[5]);
        if(jobMovingToMemory){
        	jobMovingToMemory = false; //because a job was just finished with laoding into memory*******************newly added
        	Job job = jobBeingSwapped;
        	System.out.println("AR: Job "+job.jobNumber+" moved into Memory");
        	lastJobIn = job;
        	job.printJob();
        	job.superPriority = false;
        	jobBeingSwapped = null;
    		cpuQueue.add(job);
    		jobsInMemory.put(job.jobNumber, job);
        }else if(jobMovingOutOfMemory){
        	jobMovingOutOfMemory = false; //because a job was just swaped out of memory
        	Job job = jobBeingSwapped;
        	System.out.println("AR: Job "+job.jobNumber+" moved out of Memory");
        	job.printJob();
        	job.superPriority = false;
        	job.block = -1;
        	jobBeingSwapped = null;
        	priorityInQueue[job.priority-1]--;
            while(cpuQueue.contains(job)){
            	cpuQueue.remove(job);
            }
			jobsInMemory.remove(job.jobNumber);
			drumQueue.add(job);
			job.jobEntredTime = System.currentTimeMillis();
        }
        //printAll();
        jobMigration();
		scheduler(a,p);			// try scheduling the job
	}
	
	//stand for time runs out
	//will be later edited for dealing with time sliced
	//for now will be called when a process finishes with the cpu and wants to terminate, since the scheduler is in FCFS
	static void Tro(int []a, int []p){
        Job job = cpuQueue.peek();
        
        updateCPUused(p[5]);
        
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
		jobMigration();			//Try Moving the job into memory
		scheduler(a, p); //Try to schedule the next Job
	}
	
	//stands for supervisor call
	//when a=5 then job wants to terminate
	//when a=6 then job wants to move to disk and use i/o
	//when a=7 then job wants to be blocked (change boolean blocked, of current job, to true
	//or else it is called when an I/O request is done
	static void Svc(int[] a, int[] p){
        updateCPUused(p[5]);
		if(a[0]==5){
			terminate(cpuQueue.peek().jobNumber);
		}else if(a[0]==6){
			Job job = cpuQueue.peek();
			//System.out.println("Ar: Job "+job.jobNumber+" is asking for IO");
			diskQueue.add(job);//add job on cpu to diskQueue
		}else if(a[0]==7){
			//Job requests to be blocked
			//First make sure that there is at least one IO request in queue fot this job
			//If not, then ignore
			Job j = cpuQueue.peek();
			//System.out.println("Ar: Job "+j.jobNumber+" is asking to be blocked");
			//printAll();
			if(diskQueue.contains(j)){
				j.blocked = true;
			}
		}
		if(!ioRunningJob){
			//This is the case when no more jobs were on io queue and we need to start the process
			//ourselves. If there are other jobs on the queue, this job will be processed on first come, first serve.
			Dskint(a, p);
		}
		jobMigration();			//Try Moving the job into memory
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
			jobsInMemory.remove(jobID);
			priorityInQueue[job.priority-1]--;
		}
		jobMigration();			//Try Moving the job into memory
	}
    
    
	//this function attempts to move something from drum(backing store) to main memory
	//The algorithm for picking a job from backing store, and try to fit into memory, is as follows:
	//search for the first highest priority job in the drum
	//then invoke foundSpace() to see if space was found for that job
	//if space is found, swapToMemory() is called to move that job into memory
	//if space is not found, the job is added to a vector of jobs that can't fit into memory called jobsNotFitting.
	//           and try the next highest priority job in the drum.
	static boolean tryMovingJobToMemory(){
        if(!jobMovingToMemory && !jobMovingOutOfMemory){
            Collections.sort(drumQueue);
            //Added to bring back the io pending job back in memory
            Job tjob = diskQueue.peek();
            if(tjob != null && drumQueue.contains(tjob)){
            	drumQueue.remove(tjob);
            	drumQueue.addFirst(tjob);
            	tjob.superPriority = true;
            }
            /*
            System.out.println("IN");
			for(Job job: drumQueue){
				job.printJob();
			}
			*/
            int block = -1;
            boolean passedOnce = false;
            for(int i=0; i<2; i++){
	            for(Job job : drumQueue){
	            	if(job.superPriority || priorityInQueue[job.priority-1] < maxWithSamePriority || passedOnce){
		                block = findSpace(job);
		                if(block >= 0){
		                    drumQueue.remove(job);
		                    swapToMemory(job, block);
		                    priorityInQueue[job.priority-1]++;
		                    return true;
		                }
	            	}
	            }
	            passedOnce = true;
            }
        }
        return false;
	}
	
	static boolean tryMovingJobOutOfMemory(){
		if(!jobMovingToMemory && !jobMovingOutOfMemory && jobsInMemory.size() > 3){
			ArrayList<Job> jobs = new ArrayList<Job>(jobsInMemory.values());
			//Job.sortListBySizeAndBlocked(jobs);
			/*
			for(Job job: jobs){
				job.printJob();
			}
			*/
			//Experiment
			//Kick out the BiggestSize Job //Bring in Smallest
			Collections.sort(jobs);
			/*
			System.out.println("OUT");
			for(Job job: jobs){
				job.printJob();
			}
			*/
			for(int i= jobs.size()-1; i>=0; i--){
				Job job = jobs.get(i);
				if(!ioRunningJob || jobDoingIO != job){
					swapOutOfMemory(job);
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
		FreeSpaceEntry.deleteEntry(block, jobInDrum.jobSize, freeSpaceTable);
		jobBeingSwapped = jobInDrum;
		jobInDrum.block=block;
        jobMovingToMemory=true;
		sos.siodrum(jobInDrum.jobNumber, jobInDrum.jobSize, block, 0);
	}
    
	/*
	 * It will signals the siodrum to swap a job out of memory
	 * Once finish, another job can take the freed up memory space
	 */
	static void swapOutOfMemory(Job jobToSwap){
		if(jobToSwap.block>-1){
    		freeSpaceTable.add(new FreeSpaceEntry(jobToSwap.block, jobToSwap.jobSize));
    		FreeSpaceEntry.compactBlocks(freeSpaceTable);
    	}
		jobBeingSwapped = jobToSwap;
        jobMovingOutOfMemory=true;
		sos.siodrum(jobToSwap.jobNumber, jobToSwap.jobSize, jobToSwap.block, 1);
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
		if(!ioRunningJob){
			Dskint(a, p);
		}
        Job firstJob = cpuQueue.peek();
        Job jobToRun=firstJob;
        while(jobToRun !=null && (jobToRun.blocked || jobToRun == jobBeingSwapped || jobToRun.killThisJob)){
            cpuQueue.add(cpuQueue.poll());
            jobToRun = cpuQueue.peek();
            if(jobToRun == firstJob){
                jobToRun = null;
            }
        }
        if(jobToRun!=null){
        	//System.out.println("Ars: Scheduling "+jobToRun.jobNumber + " blocked: "+jobToRun.blocked);
        	//jobToRun.printJob();
            a[0]=2;
            p[2]=jobToRun.block;
            p[3]=jobToRun.jobSize;
            if((jobToRun.maxCPUTime-jobToRun.usedTime)<timeslice)
                p[4]=jobToRun.maxCPUTime-jobToRun.usedTime;
            else
                p[4]=timeslice;
            cpuRunningJob=true;
            jobToRun.lastScheduledTime = p[5];
            jobToRun.cpuTimeAdded=false;
            
        }else{
            a[0]=1;
        }
	}
    
    static void updateCPUused(int time){
        if(cpuQueue.size()>0 && cpuQueue.peek().lastScheduledTime!=-1 && !cpuQueue.peek().blocked){
            if(!cpuQueue.peek().cpuTimeAdded){
                cpuQueue.peek().usedTime+=time-cpuQueue.peek().lastScheduledTime;
                lastJobKicked += time-cpuQueue.peek().lastScheduledTime;
                cpuQueue.peek().cpuTimeAdded=true;
            }
        }
    }
    
	static void printAll(){
		System.out.println("OS Statistics");
		System.out.print("\n\nJobs in Cpu queue\n");
		for(Job job : cpuQueue){
			job.printJob();
		}
		System.out.print("\n\nJobs in IO queue\n");
		for(Job job : diskQueue){
			job.printJob();
		}
		System.out.print("\n\nJobs in Drum\n");
		for(Job job : drumQueue){
			job.printJob();
		}
		System.out.println("\n\nFree Space Enteries");
		for(FreeSpaceEntry fs : freeSpaceTable){
			System.out.println(fs);
		}
		System.out.println("\n\nIORunninJob: "+ioRunningJob);
		System.out.print("\n\n");
	}
	
}
