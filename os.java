import java.util.*;

public class os{
    
	static LinkedList<FreeSpaceEntry> freeSpaceTable;//is a list of free spaces entrees 
    
    static LinkedList<Job> drumQueue;//is a list of jobs in the drum
	static LinkedList<Job> cpuQueue;//a list of jobs waiting to use the CPU
	static LinkedList<Job> diskQueue;//contains a list of jobs waiting to use the disk for I/O
    
	static Map<Integer, Job> jobsInMemory; //is List of all the jobs in memory
    
    static Job jobBeingSwappedIn=null;//current job that is being moved into memory
    static Job jobBeingSwappedOut=null;//current job that is being moved out of memory
    
    static Job jobDoingIO=null;//the current job that is doing IO 
	
	static boolean cpuRunningJob=false;//true or false whether a job is using the cpu at the moment
	static boolean ioRunningJob=false; //true or false to indicate whether a job is doing IO at the moment
    static boolean jobMovingToMemory=false;//true or false if a job is being moved into memory
    static boolean jobMovingOutOfMemory=false;//true or false if a job is being moved out of memory
    
    static final int timeslice=21;// cpu scheduler runs in Round Robin, and this is the time slice that will be used

    //for gathering statistics of jobs in disk queue, used for determining if a blocked job should be swapped out or not
    static int totalBlockedRequests=0;//total number of I/O requests that are blocked in the diskqueue
    static int totalIORequests=0;//total number of I/O requests that are in the diskqueue
    static int totalSwappedOutRequests=0;//total number of I/O requests that were blocked and swapped out of memory in the diskqueue
    
    
	
    //Initialize variables and objects
	static void startup(){
		//sos.ontrace();
		drumQueue=new LinkedList<Job>();
		cpuQueue=new LinkedList<Job>();
		diskQueue=new LinkedList<Job>();
		jobsInMemory = new HashMap<Integer, Job>();
		freeSpaceTable=new LinkedList<FreeSpaceEntry>();
		freeSpaceTable.add(new FreeSpaceEntry(0,100));//start up free space table to contain one free space of 100k, at block 0 
        
	}
    
	
     //when SOS has a new job moved to drum, Crint() is an interrupt that is called, and contains information about the job
     //the job information is taken off the p[] array, and added to a job object
     //the job object is added to a linkedList of jobs located in the drum called DrumQueue
     //you can attempt to move the incoming job into memory using the tryMovingJobToMemory() function.
	static void Crint(int[] a, int[] p){
        updateCPUused(p[5]);
        
		//@AR: Completed
		Job newJobInDrum = new Job(p[1], p[2], p[3], p[4]); //Initialize the new job
		drumQueue.add(newJobInDrum);	//Add job to list indicating jobs in the Drum
		tryMovingJobToMemory();			//Try Moving the job into memory
        scheduler(a,p);				//We will try to schedule the next Job
	}
    
    
	//SOS calls this interrupt when a job was successfully moved from memory to disk(for I/O)
    //I/O request on top of diskQueue is the one finished with doing I/O, so it can be popped of the diskQueue
    //If there are no more I/O requests for that job, then it can be unblocked.
	//You can try to put the next request in the diskQueue to use the disk, if it wasn't swapped out of memory
	//Jobs in diskqueue, are sorted in a way that all of the swapped jobs are at the end of the queue, but jobs not swapped out are processed in FCFS
	static void Dskint(int[] a, int[] p){
        updateCPUused(p[5]);
        
		if(ioRunningJob){
			Job job = diskQueue.poll();	//Remove the last executed I/O job
			if(job.blocked && !diskQueue.contains(job)){
				//If this job was previously blocked until all I/O was completed, we will unblock now
				job.blocked = false;
			}else if (!diskQueue.contains(job) && job.killThisJob){
				//Indicated if the job is ready to be killed pending all IO operations
				terminate(job);
			}
		}
		if(diskQueue.size() > 0){
            Collections.sort(diskQueue, new IOComparator());//sort: puts all the jobs that are swapped out of memory to the end of the diskqueue.
            Job nextJob = diskQueue.peek();
            if(!nextJob.jobSwappedOut){//if the first job on the disQueue is swapped out, then there are no jobs in memory waiting for I/O, because of sort
                jobDoingIO=nextJob;
                ioRunningJob = true;
                sos.siodisk(nextJob.jobNumber);//next job with I/O request, that is in memory, is now being moved to disk for I/O
            }
            else
                ioRunningJob=false;
        }
		else{
			ioRunningJob = false;
		}
		scheduler(a, p);//since Dskint is an interrupt, whatever job was using the cpu was kicked off, so bring in the next job to use the cpu
	}
    
    
	//SOS invokes this interrups when a job is loaded from drum into memory successfully, or from memory to Drum
    //If nothing is using the drum, then you can try to move something else into memory from drum using tryMovingJobToMemory()
	//Moves the next item on the cpuQueue to use the cpu, since cpu was interrupted.
	static void Drmint(int []a, int []p){
        updateCPUused(p[5]);//used to keep track of how much cpu time in total was used by the job that was interrupted while using the cpu
        
        if(jobMovingToMemory){//this is for jobs that finished moving into memory
            jobMovingToMemory=false;
            Job job=jobBeingSwappedIn;
            jobBeingSwappedIn=null;//must be set to null to signify that there is now no job in the act of moving into memory
            cpuQueue.add(job);
            drumQueue.remove(job);
            jobsInMemory.put(job.jobNumber, job);
            job.jobSwappedOut=false;//if job was moved into memory, then it is definitly not swapped out of memory
            
            if(jobMovingOutOfMemory && jobBeingSwappedOut!=null){//means if a job was waiting on a move into memory to be done, it can can swap out now
                moveJobOutOfMemory(jobBeingSwappedOut); 
            }
        }
        else if(jobMovingOutOfMemory){//this is for jobs that are finished with moving out of memory
            jobMovingOutOfMemory=false;
            Job job=jobBeingSwappedOut;
            jobBeingSwappedOut=null;//must be set to null to signify thata there is no job in the act of moving out of memory
            jobsInMemory.remove(job.block);
            cpuQueue.remove(job);
            drumQueue.add(job);
            jobMovingOutOfMemory=false;
            
            freeSpaceTable.add(new FreeSpaceEntry(job.block, job.jobSize));//updates free space table
            FreeSpaceEntry.compactBlocks(freeSpaceTable);//compacts free space table            
        }        
       
        if(drumQueue.size()>0 && !jobMovingOutOfMemory)//if there is currently no job moving out of memory, and there is at least one job in the drum, try moving next job in
            tryMovingJobToMemory();
        
        scheduler(a,p);//since Drmint is an interrupt, whatever job was using the cpu was kicked off, so bring in the next job to use the cpu
	}
	
	//stand for time runs out
    //Interrupt intiated when a job runs its specified time slice
	static void Tro(int []a, int []p){
        updateCPUused(p[5]);//used to keep track of how much cpu time in total was used by the job that was interrupted while using the cpu

        Job job = cpuQueue.peek();
            
		if(job != null){
			if(job.usedTime >= job.maxCPUTime){
				//Job has used the maximum CPU TIME
                terminate (job);
			}
            else{
				//Job has used its current timeslice
				cpuQueue.poll();	//remove from top of the queue
				cpuQueue.add(job);	//add at tail of the queue
            }
		}
        
		scheduler(a, p); //bring in the next job to use the cpu
	}
	
	//stands for supervisor call
	//when a=5 then job wants to terminate
	//when a=6 then job wants to move to disk and use i/o
	//when a=7 then job wants to be blocked (change boolean blocked, of current job, to true
	static void Svc(int[] a, int[] p){
        updateCPUused(p[5]);
		if(a[0]==5){//job requests termination
			terminate(cpuQueue.peek());
        }    
		else if(a[0]==6){//job makes an I/O request
			Job job = cpuQueue.poll();
			cpuQueue.add(job);
			diskQueue.add(job);//add job on cpu to diskQueue
			if(!ioRunningJob){
				//If disk is idle, then we can schedule the current I/O request to use the disk
				Dskint(a, p);
			}
		}
        else if(a[0]==7){
			//Job requests to be blocked
			//First make sure that there is at least one IO request in queue fot this job
			//If not, then ignore
			Job j = cpuQueue.peek();
			if(diskQueue.contains(j)){
				j.blocked = true;
			}
		}
        
        tryMovingJobOutOfMemory();//function called to decide if there are too many blocked jobs in the diskQueue, and if any of them should be swapped out of memory.
		scheduler(a, p);//since Svc is an interrupt, whatever job was using the cpu was kicked off, so bring in the next job to use the cpu
	}
    
	//Termination
	//Remove job from queues, if exist
	static void terminate(Job job) {
		if(diskQueue.contains(job)){
			job.killThisJob = true;
		}else if(job !=  null){
			freeSpaceTable.add(new FreeSpaceEntry(job.block, job.jobSize));//updates Free Space table
            FreeSpaceEntry.compactBlocks(freeSpaceTable);
			cpuQueue.remove(job);
            jobsInMemory.remove(job.jobNumber);
		}
        tryMovingJobToMemory();//if a job terminated, then there is an empty space, and we can try to move a job into that space
	}
    
    //attempts to move a job into memory
    //This function is like the Short term scheduler
    //function makes tries sure that the disk is not idle:
        //if the diskQueue is running short on I/O requests, it attempts to see if any swaped out jobs in the drum, can be put back into memory
        //and fit in the currently available space
    //if the diskqueue is good with I/O requests, or none of the I/O requests can fit into memory at the moment:
        //the function tries to move something from drum into memory that fits in the available space
        //none of these things can be jobs that are swapped out
 	static boolean tryMovingJobToMemory(){
        gatherIOStatistics();//used to check the statistics of jobs in diskQueue, ex. how many jobs are swapped out with I/O requests pending
        Collections.sort(drumQueue);
        int block = -1;
        if(!jobMovingToMemory && !jobMovingOutOfMemory){//function only continues if the drum is idle
            if((totalIORequests-totalSwappedOutRequests)<=2){//checks if the disk is in danger of becoming idle, by checking how much I/O requests are left in the diskQueue
                for(Job job : drumQueue){
                    if(job.jobSwappedOut){//looks for something that has been blocked, and swapped out of memory
                        block = findSpace(job);//tried to find space in memory to fit the blocked, swapped out,  job
                        if(block >= 0){//true if space was found
                            drumQueue.remove(job);
                            swapToMemory(job, block);
                            return true;
                        }
                    }
                }
            }
            if(block==-1){//try to swap in something that has never never been moved into memory
                for(Job job : drumQueue){
                    if(!job.jobSwappedOut){
                        block = findSpace(job);
                        if(block >= 0){//true if space was found for the job in memory
                            drumQueue.remove(job);
                            swapToMemory(job, block);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
	}   
    
	//function takes a job passed to it(the job should be in the drum)
	//takes the location of the job in the linkedList(drumQueue)
	//takes the block/base that the job will be stored in main memory
	//and places the job passed into base location, by calling the SOS's siodrum() function
	//it places the job in a queue container calle cpuQueue
	//and since the job is no longer in the drum(backing store), it removes it from the drum
    //this function should only be called from tryMovingJobToMemory(), or else errors will occure. 
	static void swapToMemory(Job jobInDrum, int block){
		jobBeingSwappedIn = jobInDrum;
        jobMovingToMemory=true;
        jobInDrum.block=block;
        FreeSpaceEntry.deleteEntry(jobInDrum.block, jobInDrum.jobSize, freeSpaceTable);//updates the free space table
		sos.siodrum(jobInDrum.jobNumber, jobInDrum.jobSize, block, 0);
	}
    
    //finds total amount of requests in diskQueue that are swapped out, blocked, and total IO requests
    //needed for algorithm in tryMovingJobOutOfMemoery() and tryMovingJobsIntoMemory()
    static void gatherIOStatistics(){
        totalIORequests=diskQueue.size();
        totalBlockedRequests=0;
        totalSwappedOutRequests=0;
        for(Job job: diskQueue){
            if(job.blocked)
                totalBlockedRequests++;
            if(job.jobSwappedOut){
                totalSwappedOutRequests++;
            }
        }
    }
    
    //Incharge of making sure there are not too many blocked jobs waiting to do I/O on the diskQueue
    //function gathers statistics of now many total, blocked, swapped out, I/O requets there are on the diskQueue
    static void tryMovingJobOutOfMemory(){
        if(drumQueue.size()>totalSwappedOutRequests){//first make sure that there is at least some jobs that are not blocked in the drum waiting their turn to swap in
            gatherIOStatistics();
            
            //allow for more then half of the I/O requests in the diskQueue to be swapped out
            //but makes sure there is at least 4 pending I/O requests whos job is not swapped out
            if(totalIORequests/1.7>totalSwappedOutRequests && (totalIORequests-totalSwappedOutRequests>3)){
                if(totalBlockedRequests-totalSwappedOutRequests>0){//check if there is at least one I/O request who's job wants to be blocked
                    for(Job job:diskQueue){
                        if(job.blocked && !job.jobSwappedOut && job!=jobDoingIO){//look for job in diskQueue that is blocked, not swapped out yet, and not using the disk
                            if(!jobMovingOutOfMemory){//make sure nothing is being moved out of memory first
                                jobMovingOutOfMemory=true;
                                jobBeingSwappedOut=job;
                                jobBeingSwappedOut.jobSwappedOut=true;
                                //if nothing is being moved into memory either, then you can use moveJobOutOfMemory() to start moving the job out
                                //if something is moving into memory, then as soon as it is done, moveJobOutOfMemory() will be called from within Drmint for this job
                                if(!jobMovingToMemory){//if nothing is being moved into memory either, then you can use moveJobOutOfMemory() to start moving the job out
                                    moveJobOutOfMemory(jobBeingSwappedOut);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    //initiates the movement of a job out of memory, that is specified in the argument
    static void moveJobOutOfMemory(Job job){
        cpuQueue.remove(job);
        jobsInMemory.remove(job.jobNumber);
        sos.siodrum(job.jobNumber, job.jobSize, job.block, 1);
    }
    
    
	//checks the free space table if the job passed in the argument can fit a space in the free space table
	//returns the block/base found for it
	//if nothing found, returns a block/base value of -1(this value can never be)
	static int findSpace(Job jobInDrum){
		for(FreeSpaceEntry e : freeSpaceTable){
			if(e.size >= jobInDrum.jobSize){
				return e.block;
			}
		}
		return -1;
	}
    
    
	//Round Robin
	//check if there is no job running on the cpu
	//if there isn't, take the top job on the top of the cpuQueue and run it on the cpu for a specific time slice
	static void scheduler(int []a, int []p){

        Job firstJob = cpuQueue.peek();
        Job jobToRun=firstJob;
        
        //make sure the job on top of the cpuQueue is not blocked, or in the process of being terminated
        //if the job on top of cpuQueue is not fit to run, then keep looping through the cpuQueue till you get one that is fit to run on the cpu
        while(jobToRun !=null && (jobToRun.blocked || jobToRun.killThisJob )){
            cpuQueue.add(cpuQueue.poll());
            jobToRun = cpuQueue.peek();
            if(jobToRun == firstJob){
                jobToRun = null;
            }
        }
        if(jobToRun!=null){//true if you find  job on the cpuQueue that i fit to run on the cpu
            a[0]=2;
            p[2]=jobToRun.block;
            p[3]=jobToRun.jobSize;
            if((jobToRun.maxCPUTime-jobToRun.usedTime)<timeslice)//if the cpu time a job has remaining is smaller then the time slice, then run it for the remaining time
                p[4]=jobToRun.maxCPUTime-jobToRun.usedTime;
            else
                p[4]=timeslice;//else run the job for the specified time slice
            cpuRunningJob=true;
            
            jobToRun.lastScheduledTime = p[5];
            jobToRun.cpuTimeAdded=false;
            
        }else{
            a[0]=1;
        }
	}
    
    //this function should be called every time the cpu gets interrupted. 
    //this function makes sure that the total amount of time a job ran on the cpu gets updates after every interrupt
    static void updateCPUused(int time){
        //make sure that there is at least one job on the cpu first, and make sure it at least ran once, and it is not a blocked job
        //to check if it ran at least once, the lastScheduledTime would be anything >=0, since lastScheduledTime is initialized to -1 for every job that is new
        if(cpuQueue.size()>0 && cpuQueue.peek().lastScheduledTime!=-1 && !cpuQueue.peek().blocked){
            
            //this function gets called from every interrupt,
            //it must be checked that the amount of cpu time added is not duplicated
            //cpuTimeAdded is false if:  cpu run time has not been added to total cpu time used by last job using the cpu
            //cpuTimeAdded is true if: cpu run time has already been added to the total cpu time used by the last job using the cpu
            if(!cpuQueue.peek().cpuTimeAdded){
                cpuQueue.peek().usedTime+=time-cpuQueue.peek().lastScheduledTime;
                cpuQueue.peek().cpuTimeAdded=true;
            }
        }
    }
}
