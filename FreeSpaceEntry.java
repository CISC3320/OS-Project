import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
	
	public static void sortBySize(List<FreeSpaceEntry> tableToSort){
		FreeSpaceEntry.sortBySize = true;
		Collections.sort(tableToSort);
	}
	
	public static void sortByBlock(List<FreeSpaceEntry> tableToSort){
		FreeSpaceEntry.sortBySize = false;
		Collections.sort(tableToSort);
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
	
}
