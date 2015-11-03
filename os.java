import java.util.*;
import java.lang.System.*;

public class os{
	static LinkedList<Job> jobsInDrum;//is a list of jobs in the drum
	static LinkedList<FreeSpaceEntry> freeSpaceTable;//is a list of free spaces entrees
	static Queue<Job> cpuQueue;//a queue of jobs on the cpuQueue
	static boolean cpuRunningJob;//true or false whether a job is using the cpu at the moment
	static Job jobOnCPU;//contains the job that is currently using the cpu
	static Job jobOnDisk;//contains the current job that is running on the disk
	static Queue<Job> diskQueue;//contains jobs that are waiting to use the disk for I/O

	//SOS's main calls the startup() function first
	//this is a good palce to add any variable/objects that need to be allocated/initialized
	static void startup(){
		sos.ontrace();
		jobsInDrum=new LinkedList<Job>();
		freeSpaceTable=new LinkedList<FreeSpaceEntry>();
		freeSpaceTable.add(new FreeSpaceEntree(0,100));//start up free space table to contain one free space of 100k, at block 0 
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
		/*cpuQueue.add(jobOnDisk);
		if(diskQueue.size()!=0){
			jobOnDisk=diskQueue.peek();
			sos.siodisk(diskQueue.poll().jobNumber);
		}*/
		System.out.println("TEST");
	}


	//SOS invokes this interrups when a job is loaded from drum into memory successfully
	//therefore, this means the system is ready for another attempt to move something from drum into memory, and onto the cpuQueue
	//and you can attempt to move something from the cpuQueue to using the cpu
	//the scheduler() only attempts to make the move if nothing is running on the cpu at the moment by looking at the boolean jobOnCPU 
	static void Drmint(int []a, int []p){
		tryMovingJobToMemory();
		scheduler(a,p);
	}

	
	//stand for time runs out
	//will be later edited for dealing with time sliced
	//for now will be called when a process finishes with the cpu and wants to terminate, since the scheduler is in FCFS
	static void Tro(int []a, int []p){

	}

	//stands for supervisor call
	//when a=5 then job wants to terminate
	//when a=6 then job wants to move to disk and use i/o
	//when a=7 then job wants to be blocked (change boolean blocked, of current job, to true
	//or else it is called when an I/O request is done
	static void Svc(int []a, int []p){
		cpuRunningJob=false;	
		if(a[0]==5){
					
		}
		else if(a[0]==6){
			diskQueue.add(jobOnCPU);//add job on cpu to diskQueue
                        jobOnDisk=diskQueue.poll();//save top of diskQeueu to jobOnDisk(the job that is going to run on the disk right now)
			//?????
			sos.siodisk(jobOnDisk.jobNumber);//run the top of the diskQueue on the disk for I/O	
			//?????
		}
		else if(a[0]==7){
		
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
		Vector<Job> jobsNotFitting=new Vector<Job>();
		while(jobsNotFitting.size()!=0 || jobsNotFitting.size()!=jobsInDrum.size()){
			int count=0;
			int highestPriority=5;
			int jobLocationInDrum=0;
			int base=-1; //will be -1 untill it is changed

			//first look for job to move in with highest priorty
			while(count<jobsInDrum.size()){
				if(!jobsNotFitting.contains(jobsInDrum.get(count)) && jobsInDrum.get(count).priority<=highestPriority){
					highestPriority=jobsInDrum.get(count).priority;
					jobLocationInDrum=count;
				}
				count++;
			}

			//then we check if the highest priority job fits into memory
			base=foundSpace(jobsInDrum.get(jobLocationInDrum));
			if(base!=1000){
                		swapToMemory(jobsInDrum.get(jobLocationInDrum),jobLocationInDrum, base);
				return true;
              		}
			//if the highest priority job does not fit we add it to our list of jobs that don't fit
			else{
				jobsNotFitting.add(jobsInDrum.get(jobLocationInDrum));

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
	static void swapToMemory(Job jobInDrum, int locationInDrum, int base){
		sos.siodrum(jobInDrum.jobNumber, jobInDrum.jobSize, base, 0);
		jobInDrum.base=base;
		cpuQueue.add(jobInDrum);

		jobsInDrum.remove(locationInDrum);
	}


	//checks the free space table if the job passed in the argument can fit a space in the free space table
	//returns the block/base found for it, and updates the free space table to subtract the size of the job passed to it 
	//if nothing found, returns a block/base value of 1000(this value can never be)
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
		return 1000;	
	}


	//first come first served
	//check if there is no job running on the cpu
	//if there isn't, pop a job of the cpu queue, and run it on the cpu by changing a[0] to 2 	
	static void scheduler(int []a, int []p){
                if(!cpuRunningJob){
                        Job jobToRun=cpuQueue.poll();//poll() if java equivelent of pop()
                        if(jobToRun!=null){
                                a[0]=2;
                                p[2]=jobToRun.base;
                                p[3]=jobToRun.jobSize;
                                p[4]=jobToRun.maxCPUTime;
				cpuRunningJob=true;
				jobOnCPU=jobToRun;//now there is a variable to hold information about the current job running
                        }
                }
	
	}
}
