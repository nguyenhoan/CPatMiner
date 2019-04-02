package main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import utils.FileIO;

public class FindExamples {

	public static void main(String[] args) {
		findGaps();
//		findForStatments();
	}

	private static void findGaps() {
		HashMap<File, Double> pScore = new HashMap<>();
		File dir = new File("D:/Projects/ChangeStatICSE2016/code/GraphMining/GraphMiner/output/patterns/repos-5stars-50commits-fresh-hybrid-50-2017-02-18/1");
		for (File sizeDir : dir.listFiles()) {
			if (sizeDir.isDirectory()) {
				for (File patternDir : sizeDir.listFiles()) {
					String content = FileIO.readStringFromFile(patternDir.getAbsolutePath() + "/sampleChange.html");
					if (content == null)
						continue;
					ArrayList<String> oldLines = new ArrayList<>(), newLines = new ArrayList<>();
					ArrayList<Integer> oldChanges = new ArrayList<>(), newChanges = new ArrayList<>();
					int s = content.indexOf("<pre><code class='java'>") + "<pre><code class='java'>".length();
					int e = content.indexOf("</code></pre>", s);
					read(content.substring(s, e), oldLines, oldChanges);
					s = content.indexOf("<pre><code class='java'>", e) + "<pre><code class='java'>".length();
					e = content.indexOf("</code></pre>", s);
					read(content.substring(s, e), newLines, newChanges);
					if (hasFor(oldChanges, oldLines) || hasFor(newChanges, newLines)) {
						double oldGaps = countGaps(oldChanges, oldLines, new HashSet<>(newLines));
						double newGaps = countGaps(newChanges, newLines, new HashSet<>(oldLines));
						double gaps = (oldGaps + newGaps) / 2.0;
						if (oldGaps >= 2 && newGaps >= 2 && oldGaps <= 4 && newGaps <= 4)
							pScore.put(patternDir, gaps);
					}
				}
			}
		}
		ArrayList<File> ps = new ArrayList<>(pScore.keySet());
		Collections.sort(ps, new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				double s2 = pScore.get(f2), s1 = pScore.get(f1);
				if (s1 > s2)
					return -1;
				if (s1 < s2)
					return 1;
				return 0;
			}
		});
		StringBuilder sb = new StringBuilder();
		for (File p : ps) {
			System.out.println("file:///" + p.getAbsolutePath().replace('\\', '/') + "/sampleChange.html");
			System.out.println("file:///" + p.getAbsolutePath().replace('\\', '/') + "/details.html");
			System.out.println();
			sb.append("file:///" + p.getAbsolutePath().replace('\\', '/') + "/sampleChange.html\n");
			sb.append("file:///" + p.getAbsolutePath().replace('\\', '/') + "/details.html\n");
			sb.append("\n");
		}
		FileIO.writeStringToFile(sb.toString(), "T:/temp/pattern-gaps.txt");
	}

	private static boolean hasFor(ArrayList<Integer> changes, ArrayList<String> lines) {
		for (int i : changes) {
			if (hasFor(lines.get(i)))
				return true;
		}
		return false;
	}

	private static boolean hasFor(String line) {
		int s = 0;
		while (true) {
			int index = line.indexOf("<a id=\"change\">", s);
			if (index == -1)
				return false;
			s = index + "<a id=\"change\">".length();
			int e = line.indexOf("</a>", s);
			if (e == -1)
				e = line.length();
			String c = line.substring(s, e);
			if (c.startsWith("for") || c.startsWith("if") || c.startsWith("while"))
				return true;
		}
	}

	private static double countGaps(ArrayList<Integer> changes, ArrayList<String> lines, HashSet<String> otherLines) {
		double gaps = 0;
		if (changes.size() > 1) {
			for (int i = 1; i < changes.size(); i++) {
				int gap = 0;
				int s = changes.get(i-1), e = changes.get(i);
				if (e - s > 10)
					continue;
				for (int j = s + 1; j < e; j++) {
					String line = lines.get(j);
					if (!otherLines.contains(line))
						gap++;
				}
				if (gap > 0)
					gaps += Math.log10(10 + gap/10.0);
			}
		}
		return gaps;
	}

	private static void read(String content, ArrayList<String> lines, ArrayList<Integer> changes) {
		Scanner sc = new Scanner(content);
		int i = -1;
		while (sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			i++;
			lines.add(line);
			if (isChanged(line))
				changes.add(i);
		}
		sc.close();
	}

	private static boolean isChanged(String line) {
		int s = 0;
		while (true) {
			int index = line.indexOf("<a id=\"change\">", s);
			if (index == -1)
				return false;
			s = index + "<a id=\"change\">".length();
			int e = line.indexOf("</a>", s);
			if (e == -1)
				e = line.length();
			String c = line.substring(s, e);
			if (c.contains("("))
				return true;
		}
	}

	public static void findForStatments() {
		File dir = new File("D:/Projects/ChangeStatICSE2016/code/GraphMining/GraphMiner/output/patterns/repos-5stars-50commits-fresh-hybrid-50-2017-02-18/1");
		for (File sizeDir : dir.listFiles()) {
			if (sizeDir.isDirectory())
				for (File patternDir : sizeDir.listFiles()) {
					for (File file : patternDir.listFiles()) {
						if (file.getName().endsWith(".dot")) {
							String content = FileIO.readStringFromFile(file.getAbsolutePath());
							int c0 = content.indexOf("subgraph cluster0 {");
							int c1 = content.indexOf("subgraph cluster1 {");
							int s0 = content.indexOf("[label=\"ForStatement\" ", c0);
							if (s0 == -1)
								break;
							if (s0 > c1)
								break;
							s0 = content.indexOf("[label=\"ForStatement\" ", c1);
							if (s0 > -1)
								break;
							int s1 = content.indexOf("[label=\"EnhancedForStatement\" ", c0);
							if (s1 == -1 || s1 <= c1)
								break;
							System.out.println("file:///" + patternDir.getAbsolutePath().replace('\\', '/') + "/sampleChange.html");
							System.out.println("file:///" + patternDir.getAbsolutePath().replace('\\', '/') + "/details.html");
							System.out.println();
							break;
						}
					}
				}
		}
	}

}
