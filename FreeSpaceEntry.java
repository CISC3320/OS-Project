import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

public class FreeSpaceEntry implements Comparable<FreeSpaceEntry>{
	
	public int block, size;
	/*100k size and block is numbered 0-99*/
	FreeSpaceEntry(int block, int size){
		this.block=block;
		this.size=size;
	}
	
	/* @toString: Always good for debugging
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString(){
		return "Block: "+block+"\tSize: "+size;
	}	
	
	//Following variables/methods are used for Sorting The FreeSpace table
	private static boolean sortBySize;
	
	public static void sortAndCompactBySize(List<FreeSpaceEntry> tableToSort){
		if(tableToSort.size()<2){
			return;
		}
		//@Rev
		sortAndCompactByBlock(tableToSort);	//We sort by Block to Merge and Then Sort by Size
		
		//Start of Sort by Size
		FreeSpaceEntry.sortBySize = true;
		Collections.sort(tableToSort);
		//End of Sort by Size
	}
	
	public static void sortAndCompactByBlock(List<FreeSpaceEntry> tableToSort){
		if(tableToSort.size()<2){
			return;
		}
		
		//Start of Sort by Block
		FreeSpaceEntry.sortBySize = false;
		Collections.sort(tableToSort);  //Actual Sort
		//End of Sort by Block
		
		ListIterator<FreeSpaceEntry> tableIterator = tableToSort.listIterator();
		while(tableIterator.hasNext()){
			FreeSpaceEntry tempTableAtHand = tableIterator.next();
			if(tableIterator.hasNext()){
				int currentEntryLastIndex = tempTableAtHand.block+tempTableAtHand.size;
				FreeSpaceEntry tempTableAtHandNext = tableIterator.next();
				if(tempTableAtHandNext.block == currentEntryLastIndex){
					tempTableAtHand.size += tempTableAtHandNext.size;
					tableIterator.remove();
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
	
	public static void main(String[] args){
		//@ToRemove
		//For Testing Only
		//Should be removed from final Production
		ArrayList<FreeSpaceEntry> test = new ArrayList<FreeSpaceEntry>();
		test.add(new FreeSpaceEntry(5, 2));
		test.add(new FreeSpaceEntry(0, 2));
		test.add(new FreeSpaceEntry(13, 1));
		test.add(new FreeSpaceEntry(15, 9));
		test.add(new FreeSpaceEntry(7, 5));
		test.add(new FreeSpaceEntry(79, 80));
		test.add(new FreeSpaceEntry(24, 2));
		
		System.out.println("Pre Sort");
		for(FreeSpaceEntry e : test){
			System.out.println(e);
		}
		FreeSpaceEntry.sortAndCompactByBlock(test);
		System.out.println("\nAfter Compact");
		for(FreeSpaceEntry e : test){
			System.out.println(e);
		}
	}
	
}
