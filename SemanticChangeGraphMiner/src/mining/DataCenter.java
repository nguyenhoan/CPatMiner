package mining;

import utils.FileIO;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Harry Tran on 3/3/18.
 * @project GraphMiner
 * @email trunghieu.tran@utdallas.edu
 * @organization UTDallas
 */
public class DataCenter {
	private  HashMap<Integer, String> bagToTransItemIDs = new HashMap<>();
	private  HashMap<String, String> commitProjectMap = new HashMap<>();
	private  ArrayList<TransactionInfo> transInfos = new ArrayList<>();
	private  ArrayList<Bag> bags = new ArrayList<>();
	public DataCenter() {
		init();
	}

	public void initBagToTransItemIDs() {
		String tmp = FileIO.readStringFromFile(Constants.MAPPING_FILE);
		String[] lines = tmp.split("\n");
		for (String line : lines) {
			String[] ss = line.split("-->");
			int bagId = Integer.parseInt(ss[0]);
			bagToTransItemIDs.put(bagId, ss[1]);
		}
	}

	public void initTransactionData() {
		String tmp = FileIO.readStringFromFile(Constants.TRANSACTION_BAGS);
		String[] lines = tmp.split("\n");
		for (String line : lines) {
			String[] ss = line.split("\\|\\|\\|");
			ArrayList<TransactionItem> items = new ArrayList<>();
			for (int i = 1; i < ss.length; ++i) {
				TransactionItem currItem = TransactionItem.parseTransactionItemFromString(ss[i]);
				items.add(currItem);
			}
			transInfos.add(new TransactionInfo(ss[0], items));
		}
	}

	public void initBags() {
		ArrayList<Bag> tempBags = new ArrayList<>();
		String tmp = FileIO.readStringFromFile(Constants.BAGS_FILE);
		String[] lines = tmp.split("\n");
		int i = 0;
		while (i + 2 < lines.length) {
			ItemSet currItemSet = ItemSet.parseItemSetFromString(lines[i]);
			String[] bagList = lines[i + 1].substring(1, lines[i + 1].length() - 1).split("},");
			for (String curr : bagList) {
				String currBagStr = curr;
				if (currBagStr.charAt(currBagStr.length() - 1) != '}') {
					currBagStr += "}";
				}
				Bag currBag = Bag.parseBagFromString(currBagStr);
				if (currBag.getCardinality() >= 3) {
					currBag.setItemSet(currItemSet);
					currBag.setBagID(tempBags.size());
					tempBags.add(currBag);
				}
			}
			i += 3;
		}
		bags.addAll(tempBags);
//		bagReduction(tempBags);
	}

	private void bagReduction(ArrayList<Bag> tempBags) {
		for (int i = 0; i < tempBags.size(); ++i) {
			boolean isFound = false;
			for (int j = i + 1; j < tempBags.size(); ++j)
				if (tempBags.get(j).containsBag(tempBags.get(i))) {
					isFound = true;
					break;
				}
			if (!isFound) bags.add(tempBags.get(i));
		}
		System.out.println("------------");
		System.out.println(tempBags.size());
		System.out.println(bags.size());
	}

	private void initCommitProjectMap() {
		String str = FileIO.readStringFromFile(Constants.COMMIT_PROJECT_FILE);
		String[] partitions = str.split("\n");
		for (String part : partitions) {
			String[] ss = part.split(" ");
			commitProjectMap.put(ss[0], ss[1]);
		}
	}

	public void init() {
		initCommitProjectMap();
		initBagToTransItemIDs();
		initTransactionData();
		initBags();
	}

	public String getTransItemIDFrom(int bagItemID) {
		return bagToTransItemIDs.containsKey(bagItemID) ? bagToTransItemIDs.get(bagItemID) : "";
	}

	private boolean isTransIncludesBag(TransactionInfo t, Bag b) {
		ArrayList<BagItem> bagItems = b.getItems();
		ArrayList<TransactionItem> transactionItems = t.getTranItems();
		for (BagItem bitem : bagItems) {
			boolean found = false;
			int cnt = 0;
			for (TransactionItem titem : transactionItems) {
				if (getTransItemIDFrom(bitem.getId()).equals(titem.getId())) {
					++cnt;
					if (cnt >= bitem.getNumOfFreq()) {
						found = true;
						break;
					}
				}
			}
			if (!found) return false;
		}
		return true;
	}

	public TransactionInfo findFirstTransactionAppearredOfBag(Bag bag) {
		ArrayList<Integer> transId = bag.getItemSet().getTransIDList();
		for (int currId : transId) {
			if (currId < transInfos.size() && isTransIncludesBag(transInfos.get(currId), bag)) {
				return transInfos.get(currId);
			}
		}
		return null;
	}

	public String getDirectoryReposFromCommitID(String commitId) {
		String tmp = commitProjectMap.get(commitId);
		return Constants.ALL_REPOS_PATH + tmp.split("/")[1] + "/";
	}

	public String getGitCommitUrl(TransactionInfo tf) {
		String tmp = commitProjectMap.get(tf.getCommitID());
		return Constants.GIT_URL_PREFIX + tmp + "/commit/" +  tf.getCommitID() + "/" + tf.getFileName();
	}

	public String getProjectNamFromCommitID(String commitId) {
		return commitProjectMap.get(commitId).split("/")[1];
	}

	public  HashMap<Integer, String> getBagToTransItemIDs() {
		return bagToTransItemIDs;
	}

	public  ArrayList<TransactionInfo> getTransInfos() {
		return transInfos;
	}

	public  ArrayList<Bag> getBags() {
		return bags;
	}
}
