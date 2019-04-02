package mining;

import java.util.ArrayList;

/**
 * @author Harry Tran on 3/3/18.
 * @project GraphMiner
 * @email trunghieu.tran@utdallas.edu
 * @organization UTDallas
 */
public class Bag {
	private int numOfFreq = 0;
	private int bagID;
	private ArrayList<BagItem> items;
	private ItemSet itemSet;

	public Bag(int numOfFreq, ArrayList<BagItem> items) {
		this.numOfFreq = numOfFreq;
		this.items = items;
	}

	public static Bag parseBagFromString(String str) {
		String[] ss = str.split("-");
		int numOfFreq = Integer.parseInt(ss[0].replaceAll("\\s", ""));
		String[] ss2 = ss[1].substring(1, ss[1].length() - 1).split(",");
		ArrayList<BagItem> items = new ArrayList<>();
		for (String item : ss2) {
			BagItem currItem = BagItem.parseBagItemFromString(item);
			items.add(currItem);
		}
		return new Bag(numOfFreq, items);
	}

	public int getCardinality() {
		int res = 0;
		for (BagItem item : items)
			res += item.getNumOfFreq();
		return res;
	}

	public ItemSet getItemSet() {
		return itemSet;
	}

	public void setItemSet(ItemSet itemSet) {
		this.itemSet = itemSet;
	}

	public int getNumOfFreq() {
		return numOfFreq;
	}


	public ArrayList<BagItem> getItems() {
		return items;
	}

	public int getBagID() {
		return bagID;
	}

	public void setBagID(int bagID) {
		this.bagID = bagID;
	}

	public boolean containsBagItem(BagItem bagItem) {
		for (BagItem item : items) {
			if (item.containsBagItem(bagItem))
				return true;
		}
		return false;
	}

	public boolean containsBag(Bag bag) {
		for (BagItem item : bag.getItems()){
			if (! this.containsBagItem(item))
				return false;
		}
		return true;
	}
}
