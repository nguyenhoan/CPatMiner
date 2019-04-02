package main;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Random;

import utils.FileIO;

public class RankPatterns {

	public static void main(String[] args) throws IOException {
		ArrayList<File> patternDirs = new ArrayList<>();
		final HashMap<File, Integer> frequencies = new HashMap<>(), sizes = new HashMap<>();
		final HashMap<File, Double> scores = new HashMap<>();
		File dir = new File("D:/projects/ChangeStatICSE2016/code/GraphMining/GraphMiner/output/patterns/repos-selected-hybrid/1");
		for (File dirSize : dir.listFiles()) {
			if (!dirSize.isDirectory())
				continue;
			int size = Integer.parseInt(dirSize.getName());
			for (File dirPattern : dirSize.listFiles()) {
				if (isGood(dirPattern)) {
					patternDirs.add(dirPattern);
					sizes.put(dirPattern, size);
					int f = readFrequency(dirPattern);
					frequencies.put(dirPattern, f);
					scores.put(dirPattern, Math.sqrt(size) * f);
				}
			}
		}
		Collections.sort(patternDirs, new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				double s1 = scores.get(f1), s2 = scores.get(f2);
				if (s2 > s1) return 1;
				if (s1 > s2) return -1;
				return 0;
			}
		});
		
		dir = new File("T:/pattern-samples");
		if (!dir.exists())
			dir.mkdir();
		for (int i = 0; i < patternDirs.size(); i++) {
			File p = patternDirs.get(i);
			File pd = new File(dir, p.getParentFile().getParentFile().getParentFile().getName() + "/" + (i+1) + "/" + p.getParentFile().getName() + "/" + p.getName());
			pd.mkdirs();
			copyDirectory(p, pd);
		}
	}

	private static int readFrequency(File dirPattern) {
		String content = FileIO.readStringFromFile(dirPattern.getAbsolutePath() + "/details.html");
		if (content == null)
			return 0;
		String p = "<div id='frequency'>Frequency: ";
		int s = content.indexOf(p) + p.length(), e = content.indexOf('<', s);
		int c = Integer.parseInt(content.substring(s, e));
		return c;
	}

	private static void copyDirectory(File p, File pd) {
		for (File file : p.listFiles()) {
			try {
				Files.copy(Paths.get(file.getAbsolutePath()), Paths.get(pd.getAbsolutePath() + "/" + file.getName()), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private static boolean isGood(File dirPattern) {
		return true;
	}

}
