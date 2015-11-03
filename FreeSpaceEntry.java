public class FreeSpaceEntry{
	public int block, size;
	/*100k size and block is numbered 0-99*/
	FreeSpaceEntry(int block, int size){
		this.block=block;
		this.size=size;
	}
}
