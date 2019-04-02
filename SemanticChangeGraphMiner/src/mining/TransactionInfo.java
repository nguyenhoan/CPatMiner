package mining;

import java.util.ArrayList;

/**
 * @author Harry Tran on 3/3/18.
 * @project GraphMiner
 * @email trunghieu.tran@utdallas.edu
 * @organization UTDallas
 */
public class TransactionInfo {
	private String commitName;
	private ArrayList<TransactionItem> tranItems;

	public TransactionInfo(String commitName, ArrayList<TransactionItem> tranItems) {
		this.commitName = commitName;
		this.tranItems = tranItems;
	}

	public TransactionInfo(TransactionInfo c) {
		this.commitName = c.commitName;
		this.tranItems = c.tranItems;
	}

	public TransactionItem TransactionItem(String itemId) {
		for (TransactionItem item : tranItems)
			if (item.getId().equals(itemId))
				return item;
		return null;
	}

	public void setPrintedItem(String itemId, int numOfFreq) {
		int cnt = 0;
		for (TransactionItem item : tranItems)
			if (item.getId().equals(itemId)) {
				item.setBePrinted(true);
				++cnt;
				if (cnt == numOfFreq)
					return;
			}
	}

	public String toStringPrintedItem() {
		String res = "";
		for (TransactionItem item : tranItems)
			if (item.isBePrinted()) {
				if (res.equals(""))
					res = item.getId();
				else
					res += "," + item.getId();
			}
		return res;
	}

	public String getCommitNameShorten() {
		String[] ss = commitName.split(",");
		return ss[0];
	}

	public String getCommitName() {
		return commitName;
	}

	public String getCommitNameConverted() {
		String tmp = commitName.replaceAll("/", ".");
		tmp.replaceAll(",", "_");
		return tmp;
	}

	public String getCommitID() {
		String[] ss = commitName.split(",");
		return ss[0];
	}

	public String getFileName() {
		String[] ss = commitName.split(",");
		return ss[1];
	}

	public ArrayList<TransactionItem> getTranItems() {
		return tranItems;
	}
}
