package main;

import mining.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import utils.FileIO;
import utils.JGitUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
/**
 * @author Harry Tran on 3/3/18.
 * @project GraphMiner
 * @email trunghieu.tran@utdallas.edu
 * @organization UTDallas
 */
public class ItemBagMining {

	private static int numOfFolder = 0;
	private static DataCenter dc;

	private static void printOutResults(String dir, TransactionInfo tf) {
		String commitName = tf.getCommitName();

		StringBuilder sb = new StringBuilder();
		sb.append("<html><h3>");
		sb.append(commitName + "\n");
		sb.append("</h3>");

		StringBuilder sampleChange = new StringBuilder();
		sampleChange.append("<link rel=\"stylesheet\" href=\"../../resources/default.css\">\n" +
				"<script src=\"../../resources/highlight.pack.js\"></script> \n" +
				"<script>hljs.initHighlightingOnLoad();</script>\n");
		sampleChange.append("<html><h3>");
		sampleChange.append(commitName + "\n");
		sampleChange.append("</h3>");

		ArrayList<String> beforeAndAfter = null;
		try {
			beforeAndAfter = JGitUtil.getFileFromDir(new File(dc.getDirectoryReposFromCommitID(tf.getCommitID())), commitName);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}

		try { // FIXME
			// TODO
			sampleChange.append(writeDiffs(beforeAndAfter, tf));
		} catch (StringIndexOutOfBoundsException e) {
			return;
		}

		sb.append("<div id='frequency'>Frequency: " + tf.toStringPrintedItem() + "</div><BR>");
		sb.append("<h3>Instances</h3>");
		sb.append("<a href=" + dc.getGitCommitUrl(tf) + "> Link to repos </a>");


		File transDir = new File(dir + "/" + Integer.toString(++numOfFolder));
		if (!transDir.exists())
			transDir.mkdirs();
		FileIO.writeStringToFile(sampleChange.toString(),transDir + "/sampleChange.html");
		FileIO.writeStringToFile(sb.toString(),transDir + "/details.html");

	}

	private static String getGitURL(TransactionInfo tf) {
		return Constants.GIT_JUNIT_URL_PREFIX + tf.getCommitID() + "/" + tf.getFileName();
	}

	private static int getNewHighlight(int start, int end, int newHighlight, ArrayList<ArrayList<Integer>> highlights) {
		for (ArrayList<Integer> highlight : highlights) {
			int hStart = highlight.get(0);
			int hEnd = hStart + highlight.get(1);
			if (start >= hStart && start <= hEnd) {
				if ((start + end) > hEnd) {
					highlight.set(1, (start + end) - hStart);
				}
				newHighlight = 0;
			}
			if (end >= hStart && end <= hEnd) {
				if (start < hStart) {
					highlight.set(0, start);
				}
				newHighlight = 0;
			}
		}
		return newHighlight;
	}

	private static  String markupCode(ArrayList<ArrayList<Integer>> highlights, String str) {
		if (highlights.size() == 0) return "";
		StringBuilder markedupString = new StringBuilder();
		Collections.sort(highlights, new Comparator<ArrayList<Integer>>() {
			@Override
			public int compare(ArrayList<Integer> l1, ArrayList<Integer> l2) {
				return l1.get(0).compareTo(l2.get(0));
			}
		});
		Object[] sortedArray = highlights.toArray();
		ArrayList<Integer> first = ((ArrayList<Integer>) sortedArray[0]);
		int fPos = first.get(0);
		for (int i = 0; i < 4; i++) {
			fPos = str.lastIndexOf('\n', fPos-1);
			if (fPos == -1) {
				fPos = 0;
				break;
			}
		}
		markedupString.append(str.substring(fPos, first.get(0)).replace("<","&lt;").replace(">","&gt;"));

		int end = -1;
		for (int i = 0; i < sortedArray.length-1; i++){
			ArrayList<Integer> al = (ArrayList<Integer>) sortedArray[i];
			if (al.get(0) + al.get(1) > end) {
				String tmp = "<a id=\"change\">";
				if (al.get(2) == 0)
					tmp = "<a id=\"change\" style=\"font-style:italic\">";
				markedupString.append(tmp);
				markedupString.append(str.substring(Math.max(al.get(0), end), al.get(0) + al.get(1)).replace("<", "&lt;").replace(">", "&gt;"));
				markedupString.append("</a>");

				end = al.get(0) + al.get(1);
			}
			if (i < sortedArray.length){
				ArrayList<Integer> next = (ArrayList<Integer>) sortedArray[i+1];
				if (next.get(0) > end)
					markedupString.append(str.substring(end, next.get(0)).replace("<", "&lt;").replace(">", "&gt;"));
				else if (next.get(0) + next.get(1) > end)
					System.err.print(""); // DEBUG
			}
		}
		ArrayList<Integer> last = ((ArrayList<Integer>) sortedArray[sortedArray.length-1]);
		if (last.get(0) + last.get(1) > end) {
			String tmp = "<a id=\"change\">";
			if (last.get(2) == 0)
				tmp = "<a id=\"change\" style=\"font-style:italic\">";
			markedupString.append(tmp);
			markedupString.append(str.substring(Math.max(last.get(0), end), last.get(0) + last.get(1)).replace("<", "&lt;").replace(">", "&gt;"));
			markedupString.append("</a>");

			end = last.get(0) + last.get(1);
		}
		int lPos = end;
		for (int i = 0; i < 4; i++) {
			lPos = str.indexOf('\n', lPos+1);
			if (lPos == -1) {
				lPos = str.length();
				break;
			}
		}
		markedupString.append(str.substring(end, lPos).replace("<", "&lt;").replace(">", "&gt;"));

		return String.valueOf(markedupString);
	}

	private static String writeDiffs(ArrayList<String> beforeAndAfter, TransactionInfo tf) {
		ArrayList<ArrayList<Integer>> beforeHighlights = new ArrayList<>();
		ArrayList<ArrayList<Integer>> isMapped = new ArrayList<>();
//		HashSet<>
		ArrayList<ArrayList<Integer>> afterHighlights = new ArrayList<>();

		ArrayList<TransactionItem> transItems = tf.getTranItems();
		for (TransactionItem currItem : transItems)
			if (currItem.isBePrinted()) {
				for (int i = 0; i < currItem.getNumOfFreq(); ++i) {
					int start = currItem.getStartPosition(i);
					int end = currItem.getLengthOfIndex(i);
					int newHighlight = 1;
					int version = (currItem.getType() <= 1) ? 0 : 1;
//					System.out.println("Debug [start - length - version] : " + Integer.toString(start) + " , " + Integer.toString(end) + " , " + Integer.toString(version));
					if (version == 0) {
						newHighlight = getNewHighlight(start, end, newHighlight, beforeHighlights);
						if (newHighlight > 0) {
							ArrayList<Integer> newHighlightToAdd = new ArrayList<>();
							newHighlightToAdd.add(0, start);
							newHighlightToAdd.add(1, end);
							newHighlightToAdd.add(2, currItem.getType());
							beforeHighlights.add(newHighlightToAdd);
						}
					} else {
						newHighlight = getNewHighlight(start, end, newHighlight, afterHighlights);
						if (newHighlight > 0) {
							ArrayList<Integer> newHighlightToAdd = new ArrayList<>();
							newHighlightToAdd.add(0, start);
							newHighlightToAdd.add(1, end);
							newHighlightToAdd.add(2, currItem.getType());
							afterHighlights.add(newHighlightToAdd);
						}
					}
				}
			}

		int beforeFirstChange = 999999999;
		int beforeLastChange = 0;

		int afterFirstChange = 999999999;
		int afterLastChange = 0;

		for(ArrayList<Integer> highlight : beforeHighlights ){
			if(highlight.get(0) < beforeFirstChange){
				beforeFirstChange = highlight.get(0);
			}
			int currEndPos = highlight.get(1) + highlight.get(0);
			if(currEndPos > beforeLastChange){
				beforeLastChange = currEndPos;
			}
		}

		for(ArrayList<Integer> highlight : afterHighlights ){
			if(highlight.get(0) < afterFirstChange){
				afterFirstChange = highlight.get(0);
			}
			int currEndPos = highlight.get(1) + highlight.get(0);
			if(currEndPos > afterLastChange){
				afterLastChange = currEndPos;
			}
		}

		String afterStr = beforeAndAfter.get(0);
		String beforeStr = beforeAndAfter.get(1);

//		// DEBUGING
//		FileIO.writeStringToFile(beforeStr, Constants.TEMP_FILE + "before.txt");
//		FileIO.writeStringToFile(afterStr, Constants.TEMP_FILE + "after.txt");
//		// END
		String afterMarkup = markupCode(afterHighlights, afterStr);
		String beforeMarkup = markupCode(beforeHighlights, beforeStr);

		String markedupHTML = "<h3>Before Change</h3><pre><code class='java'>" + beforeMarkup + "</code></pre>";

		markedupHTML += "<h3>After Change</h3><pre><code class='java'>" + afterMarkup + "</code></pre>";


		return markedupHTML;
	}

	public static void process() {
		dc = new DataCenter();
		ArrayList<Bag> bags = dc.getBags();
		Collections.shuffle(bags);
		int cnt = 0;
		for (Bag bag : bags) {
			TransactionInfo titem = dc.findFirstTransactionAppearredOfBag(bag);
			if (titem != null) {
//				System.out.println(titem.getCommitName());
				TransactionInfo titemToBePrinted = new TransactionInfo(titem);
				for (BagItem bi : bag.getItems()) {
					titemToBePrinted.setPrintedItem(dc.getTransItemIDFrom(bi.getId()), bi.getNumOfFreq());
				}
				printOutResults(Constants.OUTPUT_PATH, titemToBePrinted);
				if (++cnt >= Constants.SAMPLE_LIMIT)
					break;
			}
			else {
//				System.out.println("Couldnot found trans for bag" + Integer.toString(bag.getBagID()));
			}
		}
	}

	public static void test() {
		ArrayList<String> fileList = FileIO.getAllFilesInFolder(Constants.ALL_REPOS_PATH);
		for (String str : fileList) {
			File f = new File(str);
			if (f.isDirectory())
				System.out.println(str);
		}
	}

	public static void main(String[] args) throws IOException, GitAPIException {
		System.out.println("Starting...");

		ItemBagMining.process();
//		test();

		System.out.println("Finishing!");
	}
}
