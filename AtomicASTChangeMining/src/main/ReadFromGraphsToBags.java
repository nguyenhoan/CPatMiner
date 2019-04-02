package main;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import change.ChangeGraph;
import change.ChangeNode;
import utils.FileIO;

public class ReadFromGraphsToBags {

	public static void main(String[] args) {
//		String inPath = "F:/change graphs/repos-IntelliJ";
//		String outPath = "T:/change graphs/repos-IntelliJ-bags";
//		String inPath = "F:/change graphs/repos-junit";
//		String outPath = "T:/change graphs/repos-junit-bags";
		String inPath = "T:/change graphs/repos-5stars-50commits-fresh-survey";
		String outPath = "T:/change graphs/repos-5stars-50commits-fresh-survey-responses-bags";
		
		Set<String> names = new HashSet<>();
		String content = FileIO.readStringFromFile("T:/change graphs/survey_responses.txt");
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			String[] parts = line.split("/");
			names.add(parts[0] + "/" + parts[1]);
		}
		sc.close();
		StringBuilder sb = new StringBuilder();
		Map<String, String> commitProject = new HashMap<>();
		File inDir = new File(inPath);
		for (File user : inDir.listFiles()) {
			for (File repo : user.listFiles()) {
				if (!names.contains(user.getName() + "/" + repo.getName()))
					continue;
				File[] files = repo.listFiles();
				HashMap<String, Integer> commitTime = (HashMap<String, Integer>) FileIO.readObjectFromFile("T:/github/repos-metadata/" + user.getName() + "---" + repo.getName() + ".time");
				Arrays.sort(files, new Comparator<File>() {
					@Override
					public int compare(File f1, File f2) {
						String name1 = f1.getName();
						if (name1.endsWith(".dat"))
							name1 = name1.substring(0, name1.length() - 4);
						else
							return 1;
						String name2 = f2.getName();
						if (name2.endsWith(".dat"))
							name2 = name2.substring(0, name2.length() - 4);
						else
							return -1;
						return commitTime.get(name2) - commitTime.get(name1);
					}
				});
				for (int i = 0; i < Math.min(files.length, 50); i++) {
					File commit = files[i];
					if (commit.getName().endsWith(".dat")) {
						commitProject.put(FileIO.getSimpleFileName(commit.getName()), user.getName() + "/" + repo.getName());
						HashMap<String, HashMap<String, ChangeGraph>> fileChangeGraphs = (HashMap<String, HashMap<String, ChangeGraph>>) FileIO.readObjectFromFile(commit.getAbsolutePath());
						for (String fp : fileChangeGraphs.keySet()) {
							//System.out.println(fp);
							HashMap<String, ChangeGraph> cgs = fileChangeGraphs.get(fp);
							for (String method : cgs.keySet()) {
								//System.out.println(method);
								String name = FileIO.getSimpleFileName(commit.getName()) + "," + fp + "," + method;
								ChangeGraph cg = cgs.get(method);
								if (cg.getNodes().size() <= 2) continue;
								sb.append(name);
								for (ChangeNode node : cg.getNodes()) {
									if (node.isMapped()) {
										if (node.getVersion() == 0) {
											sb.append("|||");
											sb.append(node.getAstNodeType() + ":0:" + node.getStartsAsString() + ":" + node.getLengthsAsString());
										}
									} else {
										if (node.getVersion() == 0) {
											sb.append("|||");
											sb.append(node.getAstNodeType() + ":1:" + node.getStartsAsString() + ":" + node.getLengthsAsString());
										} else if (node.getVersion() == 1) {
											sb.append("|||");
											sb.append(node.getAstNodeType() + ":2:" + node.getStartsAsString() + ":" + node.getLengthsAsString());
										}
									}
								}
								sb.append("\n");
							}
						}
					}
				}
			}
		}
		File outDir = new File(outPath);
		if (!outDir.exists())
			outDir.mkdirs();
		FileIO.writeStringToFile(sb.toString(), outDir.getAbsolutePath() + "/transaction-bags.txt");
		sb = new StringBuilder();
		for (String c : commitProject.keySet())
			sb.append(c + " " + commitProject.get(c) + "\n");
		FileIO.writeStringToFile(sb.toString(), outDir.getAbsolutePath() + "/commit-project.txt");
	}

}
