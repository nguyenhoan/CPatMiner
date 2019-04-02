package main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import utils.FileIO;
import change.ChangeGraph;

public class MainReadChangeGraphs {
	private static int numOfNodes = 0, numOfGraphs = 0;
	private static HashMap<Integer, Integer> nodeBin = new HashMap<>();
	private static StringBuilder sbNodes = new StringBuilder(), sbGraphs = new StringBuilder();

	public static void main(String[] args) {
		File dir = new File("T:/change graphs/repos-99");
		read(dir);
		FileIO.writeStringToFile(sbNodes.toString(), "T:/nodes.csv");
		ArrayList<Integer> l = new ArrayList<>(nodeBin.keySet());
		Collections.sort(l);
		for (int i : l) {
			sbGraphs.append(i + "," + nodeBin.get(i) + "\n");
		}
		FileIO.writeStringToFile(sbGraphs.toString(), "T:/graphs.csv");
	}

	private static void read(File file) {
		if (file.isDirectory()) {
			System.out.println(file.getName());
			for (File sub : file.listFiles()) {
				read(sub);
			}
		}
		else if (file.getName().endsWith(".dat")) {
			/*if (!file.getName().equals("e965d17fcbef0120d067a0d92bbb506bb1c30849.dat"))
				return;*/
//			System.out.println("Commit: " + file.getName());
			int numOfGraphs = 0;
			@SuppressWarnings("unchecked")
			HashMap<String, HashMap<String, ChangeGraph>> fileChangeGraphs = (HashMap<String, HashMap<String, ChangeGraph>>) FileIO.readObjectFromFile(file.getAbsolutePath());
			if (fileChangeGraphs == null)
				return;
			for (String fp : fileChangeGraphs.keySet()) {
//				System.out.println(fp);
				HashMap<String, ChangeGraph> cgs = fileChangeGraphs.get(fp);
				numOfGraphs += cgs.size();
				for (String method : cgs.keySet()) {
//					System.out.println(method);
					ChangeGraph cg = cgs.get(method);
					if (cg.getNodes().isEmpty())
						continue;
					// DEBUG
					/*DotGraph dg = new DotGraph(cg);
					String dirPath = "D:/temp";
					dg.toDotFile(new File(dirPath + "/" + "changegraph.dot"));
					dg.toGraphics(dirPath + "/" + "changegraph", "png");
					System.out.println();
					}*/
					int n = cg.getNodes().size();
					Integer count = nodeBin.get(n);
					if (count == null)
						count = 0;
					nodeBin.put(n, count + 1);
					numOfNodes += n;
					sbNodes.append(cg.getNodes().size() + "\n");
					/*for (ChangeNode n : cg.getNodes()) {
						if (n.getLabel().startsWith("UNKNOWN"))
							numOfUnknownNodes++;
					}*/
				}
			}
			MainReadChangeGraphs.numOfGraphs += numOfGraphs;
			/*System.out.println("Files: " + fileChangeGraphs.size());
			System.out.println("Graphs: " + MainReadChangeGraphs.numOfGraphs);
			System.out.println("Nodes: " + numOfNodes);*/
		}
	}

}
