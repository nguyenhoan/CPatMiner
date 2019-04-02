package main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Scanner;

import utils.FileIO;

public class ComputeUniqueness {

	public static void main(String[] args) {
		HashMap<String, Integer> allProjectPatterns = read("T:/github/chang-patterns-sum-repos-5stars-50commits-4000/projectPatterns.csv"), 
				uniqueProjectPatterns = read("T:/github/chang-patterns-sum-repos-5stars-50commits-4000/uniqueProjectPatterns.csv");
		ArrayList<String> l = new ArrayList<>(allProjectPatterns.keySet());
		Collections.sort(l);
		StringBuilder sb = new StringBuilder();
		for (String name : l) {
			int all = allProjectPatterns.get(name), unique = uniqueProjectPatterns.containsKey(name) ? uniqueProjectPatterns.get(name) : 0;
			double p = unique * 1.0 / all;
			sb.append(name + "," + all + "," + unique + "," + p + "\n");
		}
		FileIO.writeStringToFile(sb.toString(), "T:/github/chang-patterns-sum-repos-5stars-50commits-4000/uniqueuniqueProject.csv");
	}

	private static HashMap<String, Integer> read(String path) {
		HashMap<String, Integer> map = new HashMap<>();
		String content = FileIO.readStringFromFile(path);
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			String[] parts = line.split(",");
			String name = parts[0];
			int count = Integer.parseInt(parts[1]);
			map.put(name, count);
		}
		sc.close();
		return map;
	}

}
