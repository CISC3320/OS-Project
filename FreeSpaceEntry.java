import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class FreeSpaceEntry implements Comparable<FreeSpaceEntry>{
	
	public int block, size;
    
	/*100k size and block is numbered 0-99*/
	FreeSpaceEntry(int block, int size){
		this.block=block;
		this.size=size;
	}
	
    //for testing
	public String toString(){
		return "Block: "+block+"\tSize: "+size;
	}	
	
	//Following variables/methods are used for Sorting The FreeSpace table
	private static boolean sortBySize;
	
	public static void sortBySize(List<FreeSpaceEntry> tableToSort){
		if(tableToSort.size()<2){
			return;
		}
		//Start of Sort by Size
		FreeSpaceEntry.sortBySize = true;
		Collections.sort(tableToSort);
		//End of Sort by Size
	}
	
	public static void sortByBlock(List<FreeSpaceEntry> tableToSort){
		if(tableToSort.size()<2){
			return;
		}
		//Start of Sort by Block
		FreeSpaceEntry.sortBySize = false;
		Collections.sort(tableToSort);  //Actual Sort
		//End of Sort by Block
	}
	
	public static void compactBlocks(List<FreeSpaceEntry> tableToSort){
		sortByBlock(tableToSort);
		ListIterator<FreeSpaceEntry> tableIterator = tableToSort.listIterator();
		while(tableIterator.hasNext()){
			FreeSpaceEntry tempTableAtHand = tableIterator.next();
			if(tableIterator.hasNext()){
				int currentEntryLastIndex = tempTableAtHand.block+tempTableAtHand.size;
				FreeSpaceEntry tempTableAtHandNext = tableIterator.next();
				if(tempTableAtHandNext.block == currentEntryLastIndex){
					tempTableAtHand.size += tempTableAtHandNext.size;
					tableIterator.remove();
					tableIterator.previous();
				}else{
					tableIterator.previous();
				}		
			}
		}
	}
    
	@Override
	public int compareTo(FreeSpaceEntry o) {
		int comparableVar1, comparableVar2;
		if(sortBySize){
			comparableVar1 = this.size;
			comparableVar2 = o.size;
		}else{
			comparableVar1 = this.block;
			comparableVar2 = o.block;
		}
		return comparableVar1 < comparableVar2 ? -1 : comparableVar1 > comparableVar2 ? 1 : 0;
	}
    
	static void deleteEntry(int block, int jobSize, LinkedList<FreeSpaceEntry> freeSpaceTable) {
		for(FreeSpaceEntry e : freeSpaceTable){
			if(e.block == block){
				freeSpaceTable.remove(e);
				if(e.size > jobSize){
					freeSpaceTable.add(new FreeSpaceEntry(block+jobSize, e.size-jobSize));
				}
				break;
			}
		}
		compactBlocks(freeSpaceTable);
	}
	
}
