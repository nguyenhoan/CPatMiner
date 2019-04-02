package change;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import org.eclipse.jdt.core.dom.ASTNode;

import utils.FileIO;

public class CProject implements Serializable {
	private static final long serialVersionUID = -5815046652995572399L;

	private static HashMap<String, ArrayList<Integer>> fixingRevisions;
	private static HashSet<Byte> interestingAstNodeTypeIds = new HashSet<Byte>();
	private static HashSet<String> interestingAstNodeTypeNames = new HashSet<String>();
	public static int[] densityBins = { 2, 4, 8, 16, 32, 64, 128,
			Integer.MAX_VALUE };
	public static int[] sizeBins;

	static {
		sizeBins = new int[18];
		for (int i = 0; i < sizeBins.length - 1; i++) {
			sizeBins[i] = (int) Math.pow(2, i + 1);
		}
		sizeBins[sizeBins.length - 1] = Integer.MAX_VALUE;
		interestingAstNodeTypeIds.add((byte) 2);
		interestingAstNodeTypeIds.add((byte) 3);
		interestingAstNodeTypeIds.add((byte) 4);
		interestingAstNodeTypeIds.add((byte) 5);
		interestingAstNodeTypeIds.add((byte) 6);
		interestingAstNodeTypeIds.add((byte) 11);
		interestingAstNodeTypeIds.add((byte) 12);
		interestingAstNodeTypeIds.add((byte) 14);
		interestingAstNodeTypeIds.add((byte) 16);
		interestingAstNodeTypeIds.add((byte) 17);
		interestingAstNodeTypeIds.add((byte) 19);
		interestingAstNodeTypeIds.add((byte) 22);
		interestingAstNodeTypeIds.add((byte) 24);
		interestingAstNodeTypeIds.add((byte) 25);
		interestingAstNodeTypeIds.add((byte) 27);
		interestingAstNodeTypeIds.add((byte) 28);
		interestingAstNodeTypeIds.add((byte) 32);
		interestingAstNodeTypeIds.add((byte) 37);
		interestingAstNodeTypeIds.add((byte) 38);
		interestingAstNodeTypeIds.add((byte) 46);
		interestingAstNodeTypeIds.add((byte) 47);
		interestingAstNodeTypeIds.add((byte) 48);
		interestingAstNodeTypeIds.add((byte) 49);
		interestingAstNodeTypeIds.add((byte) 50);
		interestingAstNodeTypeIds.add((byte) 51);
		interestingAstNodeTypeIds.add((byte) 53);
		interestingAstNodeTypeIds.add((byte) 54);
		interestingAstNodeTypeIds.add((byte) 61);
		interestingAstNodeTypeIds.add((byte) 62);
		interestingAstNodeTypeIds.add((byte) 70);

		for (byte id : interestingAstNodeTypeIds) {
			interestingAstNodeTypeNames.add(ASTNode.nodeClassForType(id)
					.getSimpleName());
		}
	}

	private int id = -1;
	private String name;
	HashMap<String, Integer> tokenIndexes;
	HashMap<Integer, String> indexTokens;
	private long runningTime;
	long numOfAllRevisions = 0;
	ArrayList<CRevision> revisions;

	public CProject(int id, String name) {
		this.id = id;
		this.name = name;
	}

	@SuppressWarnings("unchecked")
	public CProject(String path, int id, String name) {
		this.id = id;
		this.name = name;
		String prefix = path + "/" + name + "-";
		this.indexTokens = (HashMap<Integer, String>) FileIO.readObjectFromFile(prefix + "indexTokens.dat");
		this.tokenIndexes = (HashMap<String, Integer>) FileIO.readObjectFromFile(prefix + "tokenIndexes.dat");
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getNumOfAllRevisions() {
		return numOfAllRevisions;
	}

	public ArrayList<CRevision> getRevisions() {
		return revisions;
	}

	public HashMap<String, Integer> getTokenIndexes() {
		return tokenIndexes;
	}

	public void setTokenIndexes(HashMap<String, Integer> tokenIndexes) {
		this.tokenIndexes = tokenIndexes;
	}

	public HashMap<Integer, String> getIndexTokens() {
		return indexTokens;
	}

	public void setIndexTokens(HashMap<Integer, String> indexTokens) {
		this.indexTokens = indexTokens;
	}

	public long getRunningTime() {
		return runningTime;
	}

	public void setRunningTime(long runningTime) {
		this.runningTime = runningTime;
	}

	public static void readFixingRevisions(String filepath) {
		fixingRevisions = new HashMap<String, ArrayList<Integer>>();

		String content = FileIO.readStringFromFile(filepath);
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			String[] parts = line.split(",");
			String name = parts[0];
			ArrayList<Integer> revisions = new ArrayList<Integer>();
			for (int i = 1; i < parts.length; i++) {
				String part = parts[i];
				int revision = Integer.parseInt(part);
				revisions.add(revision);
			}
			fixingRevisions.put(name, revisions);
		}
		sc.close();
	}
}
