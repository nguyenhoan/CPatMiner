package mining;

import java.util.ArrayList;

/**
 * @author Harry Tran on 3/3/18.
 * @project GraphMiner
 * @email trunghieu.tran@utdallas.edu
 * @organization UTDallas
 */
public class TransactionItem {
	private String id;
	private int type;
	private int numOfFreq;
	private ArrayList<Integer> startPositions;
	private ArrayList<Integer> lengths;
	private boolean isBePrinted = false;

	public TransactionItem(String id, int type, int numOfFreq, ArrayList<Integer> startPositions, ArrayList<Integer> lengths) {
		this.id = id;
		this.type = type;
		this.numOfFreq = numOfFreq;
		this.startPositions = startPositions;
		this.lengths = lengths;
	}

	public static TransactionItem parseTransactionItemFromString(String str) {
		String[] elements = str.split(":");
		String id = elements[0] + ":" + elements[1];
		int type = Integer.parseInt(elements[1]);
		String[] startP = elements[2].substring(1, elements[2].length() - 1).split(",");
		String[] le = elements[3].substring(1, elements[3].length() - 1).split(",");

		ArrayList<Integer> startPositions = new ArrayList<>();
		ArrayList<Integer> lengths = new ArrayList<>();

		if (!elements[2].equals("null")) {
			for (String ss : startP) {
				startPositions.add(Integer.parseInt(ss));
			}

			for (String ss : le) {
				lengths.add(Integer.parseInt(ss));
			}
		}
		int numOfFreq = startPositions.size();

		return new TransactionItem(id, type, numOfFreq, startPositions, lengths);
	}

	public int getStartPosition(int index) {
		if (index < numOfFreq) {
			return startPositions.get(index);
		} else {
			return -1;
		}
	}

	public int getLengthOfIndex(int index) {
		if (index < numOfFreq) {
			return lengths.get(index);
		} else {
			return -1;
		}
	}

	public int getEndPosition(int index) {
		if (index < numOfFreq) {
			return startPositions.get(index) + lengths.get(index) - 1;
		} else {
			return -1;
		}
	}

	public int getType() {
		return type;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public int getNumOfFreq() {
		return numOfFreq;
	}

	public boolean isBePrinted() {
		return isBePrinted;
	}

	public void setBePrinted(boolean bePrinted) {
		isBePrinted = bePrinted;
	}
}
