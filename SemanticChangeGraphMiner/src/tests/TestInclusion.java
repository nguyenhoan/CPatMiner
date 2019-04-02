package tests;

import java.util.HashMap;
import java.util.Scanner;

import utils.FileIO;

public class TestInclusion {

	public static void main(String[] args) {
		HashMap<String, String> sentItems = read("D:/Projects/ChangeStatICSE2016/papers/change-pattern-mining/results/survey/survey-sent.txt");
		HashMap<String, String> subSentItems = read("D:/Projects/ChangeStatICSE2016/papers/change-pattern-mining/results/survey/survey-02-21-sent.txt");
		System.out.println(sentItems.size());
		System.out.println(subSentItems.size());
		System.out.println(sentItems.keySet().containsAll(subSentItems.keySet()));
		System.out.println(subSentItems.keySet().containsAll(sentItems.keySet()));
	}

	private static HashMap<String, String> read(String path) {
		HashMap<String, String> items = new HashMap<>();
		String content = FileIO.readStringFromFile(path);
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			if (line.startsWith("https://github.com/")) {
				String key = line + "\n" + sc.nextLine();
				if (items.containsKey(key))
					System.err.println();
				String value = key;
				while (!line.isEmpty()) {
					line = sc.nextLine();
					value += "\n" + line;
				}
				items.put(key, value);
			}
		}
		sc.close();
		return items;
	}

}
