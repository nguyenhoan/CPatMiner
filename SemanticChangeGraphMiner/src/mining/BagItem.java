package mining;

/**
 * @author Harry Tran on 3/3/18.
 * @project GraphMiner
 * @email trunghieu.tran@utdallas.edu
 * @organization UTDallas
 */
public class BagItem {
	private int id;
	private int numOfFreq;

	public BagItem(int id, int numOfFreq) {
		this.id = id;
		this.numOfFreq = numOfFreq;
	}

	public static BagItem parseBagItemFromString(String str) {
		String[] ss = str.split("=");
		int id = Integer.parseInt(ss[0].replaceAll("\\s", ""));
		int numOfFreq = Integer.parseInt(ss[1].replaceAll("\\s", ""));
		return new BagItem(id, numOfFreq);
	}

	public int getId() {
		return id;
	}

	public int getNumOfFreq() {
		return numOfFreq;
	}

	public boolean containsBagItem(BagItem bagItem) {
		return (this.id == bagItem.id && this.getNumOfFreq() >= bagItem.getNumOfFreq());
	}
}
