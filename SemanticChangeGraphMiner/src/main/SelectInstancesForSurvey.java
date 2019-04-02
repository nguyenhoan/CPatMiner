package main;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import utils.FileIO;

public class SelectInstancesForSurvey {

	public static void main(String[] args) {
		HashSet<String> names = new HashSet<>();
		String content = FileIO.readStringFromFile("D:/Projects/ChangeStatICSE2016/papers/change-pattern-mining/results/survey/survey-sent.txt");
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			if (line.startsWith("https://github.com/")) {
				while (sc.hasNextLine()) {
					line = sc.nextLine().trim();
					if (!line.isEmpty()) {
						names.add(line);
						break;
					}
				}
			}
		}
		sc.close();
		File dir = new File("T:/github/chang-patterns-sum-repos-5stars-50commits-fresh-hybrid-50-2017-03-03/dates");
		File[] files = dir.listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
		        return name.endsWith(".txt");
			}
		});
		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File f1, File f2) {
				return f2.compareTo(f1);
			}
		});
		HashMap<String, ArrayList<PatternInstance>> authorInstances = new HashMap<>();
		select(files, 1, authorInstances, names);
		System.out.println(authorInstances.size());
		int c = 0;
		for (String key : authorInstances.keySet())
			c += authorInstances.get(key).size();
		System.out.println(c);
		
		authorInstances = new HashMap<>();
		select(files, 7, authorInstances, names);
		System.out.println(authorInstances.size());
		c = 0;
		for (String key : authorInstances.keySet())
			c += authorInstances.get(key).size();
		System.out.println(c);
		
		authorInstances = new HashMap<>();
		select(files, 14, authorInstances, names);
		System.out.println(authorInstances.size());
		c = 0;
		for (String key : authorInstances.keySet())
			c += authorInstances.get(key).size();
		System.out.println(c);
		
		ArrayList<PatternInstance> list = new ArrayList<>();
		for (String name : authorInstances.keySet())
			list.add(authorInstances.get(name).get(0));
		Collections.sort(list, new Comparator<PatternInstance>() {
			@Override
			public int compare(PatternInstance pi1, PatternInstance pi2) {
				return pi2.date.compareTo(pi1.date);
			}
		});
		StringBuilder sb = new StringBuilder();
		for (PatternInstance pi : list) {
//			System.out.println(pi);
//			System.out.println();
			sb.append(pi + "\n");
			sb.append("\n");
		}
		FileIO.writeStringToFile(sb.toString(), dir.getParentFile().getAbsolutePath() + "/survey.txt");
	}
	
	private static void select(File[] files, int n, HashMap<String, ArrayList<PatternInstance>> authorInstances, HashSet<String> names) {
		for (int i = 0; i < Math.min(files.length, n); i++) {
			File file = files[i];
			add(authorInstances, read(file, names));
		}
		for (String key : authorInstances.keySet())
			Collections.sort(authorInstances.get(key), new Comparator<PatternInstance>() {
				@Override
				public int compare(PatternInstance pi1, PatternInstance pi2) {
					int c = pi2.date.compareTo(pi1.date);
					if (c != 0)
						return c;
					return pi2.size - pi1.size;
				}
			});
	}
	
	private static void add(HashMap<String, ArrayList<PatternInstance>> authorInstances, HashMap<String, ArrayList<PatternInstance>> other) {
		for (String key : other.keySet()) {
			ArrayList<PatternInstance> l = authorInstances.get(key);
			if (l == null) {
				l = new ArrayList<>();
				authorInstances.put(key, l);
			}
			l.addAll(other.get(key));
		}
	}

	private static HashMap<String, ArrayList<PatternInstance>> read(File file, HashSet<String> names) {
		HashMap<String, ArrayList<PatternInstance>> authorInstances = new HashMap<>();
		String content = FileIO.readStringFromFile(file.getAbsolutePath());
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
			String link = sc.nextLine().trim();
			if (link.isEmpty())
				continue;
			String name = sc.nextLine().trim();
			if (name.contains("?") || names.contains(name)) {
				sc.nextLine();
				sc.nextLine();
				sc.nextLine();
				continue;
			}
			PatternInstance pi = new PatternInstance(link, name, sc.nextLine(), sc.nextLine(), file.getName().substring(0, file.getName().lastIndexOf('.')));
			ArrayList<PatternInstance> pis = authorInstances.get(pi.name);
			if (pis == null) {
				pis = new ArrayList<>();
				authorInstances.put(pi.name, pis);
			}
			pis.add(pi);
			sc.nextLine();
		}
		sc.close();
		return authorInstances;
	}

	static class PatternInstance {
		String name, email, link, path, date;
		int size;

		public PatternInstance(String link, String name, String email, String path, String date) {
			this.name = name;
			this.email = email;
			this.link = link;
			this.path = path;
			this.date = date;
			String[] parts = path.split("\\\\");
			if (parts.length <= 1)
				parts = path.split("/");
			this.size = Integer.parseInt(parts[parts.length-3]);
		}
		
		@Override
		public String toString() {
			return this.link + "\n" + this.name + "\n" + this.email + "\n" + this.path + "\n" + this.date;
		}
	}

}
