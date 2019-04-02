package mining;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Harry Tran on 3/3/18.
 * @project GraphMiner
 * @email trunghieu.tran@utdallas.edu
 * @organization UTDallas
 */
public class ItemSet {
	private int numOfFreq;
	private String setName;
	private ArrayList<Integer> transIDList;

	public ItemSet(int numOfFreq, String setName, ArrayList<Integer> transIDList) {
		this.numOfFreq = numOfFreq;
		this.setName = setName;
		this.transIDList = transIDList;
	}

	public static ItemSet parseItemSetFromString(String str) {
		String[] ss = str.split(":");
		String[] ss2 = ss[0].split("-");
		int numOfFreq = Integer.parseInt(ss2[0].replaceAll("\\s", ""));
		String setName = ss2[1];
		String[] ss3 = ss[1].substring(1, ss[1].length() - 1).split(",");
		ArrayList<Integer> transIDList = new ArrayList<>();
		for (String curr : ss3) {
			int currId = Integer.parseInt(curr.replaceAll("\\s", ""));
			transIDList.add(currId);
		}
		Collections.sort(transIDList);
		return new ItemSet(numOfFreq, setName, transIDList);
	}

	public int getNumOfFreq() {
		return numOfFreq;
	}

	public String getSetName() {
		return setName;
	}

	public ArrayList<Integer> getTransIDList() {
		return transIDList;
	}
}
