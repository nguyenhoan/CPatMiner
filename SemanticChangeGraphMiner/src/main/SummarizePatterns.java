package main;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import utils.FileIO;

public class SummarizePatterns {
	private static HashMap<String, Integer> projectAuthors = new HashMap<>(), projectCommits = new HashMap<>();
	private static HashMap<String, String> commitEmail = new HashMap<>(), commitAuthor = readAuthor(new File("T:/github/repos-metadata/"));
	private static HashSet<String> allAuthors = new HashSet<>(), allCommits = new HashSet<>();
	private static int all = 0, failures = 0;

	private static HashMap<String, String> readAuthor(File dir) {
		HashMap<String, String> commitName = new HashMap<>();
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".author")) {
				HashMap<String, String> commitInfo = (HashMap<String, String>) FileIO.readObjectFromFile(file.getAbsolutePath());
				commitName.putAll(commitInfo);
			}
		}
		return commitName;
	}

	private static HashMap<String, String> read(File dir) {
		HashMap<String, String> map = new HashMap<>();
		File[] files = dir.listFiles();
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.getName().endsWith(".email")) {
				HashMap<String, String> commitInfo = (HashMap<String, String>) FileIO.readObjectFromFile(file.getAbsolutePath());
				HashSet<String> values = new HashSet<>();
				for (Map.Entry<String, String> entry : commitInfo.entrySet())
					values.add(entry.getValue());
				projectAuthors.put(FileIO.getSimpleFileName(file.getName()), values.size());
				allAuthors.addAll(values);
				map.putAll(commitInfo);
			} else if (file.getName().endsWith(".time")) {
				HashMap<String, Integer> commitInfo = (HashMap<String, Integer>) FileIO.readObjectFromFile(file.getAbsolutePath());
				projectCommits.put(FileIO.getSimpleFileName(file.getName()), commitInfo.size());
				allCommits.addAll(commitInfo.keySet());
			}
		}
		return map;
	}
	
	public static void main(String[] args) {
//		sumStats();
		sumDays();
	}

	private static void sumDays() {
		HashMap<String, HashSet<String>> dateInstances = new HashMap<>();
		File dir = new File("D:/projects/ChangeStatICSE2016/code/GraphMining/GraphMiner/output/patterns/repos-5stars-50commits-fresh-hybrid-50-2017-03-03/1");
		for (File dirSize : dir.listFiles()) {
			if (!dirSize.isDirectory())
				continue;
			for (File dirPattern : dirSize.listFiles()) {
				if (isGood(dirPattern)) {
					String content = FileIO.readStringFromFile(dirPattern.getAbsolutePath() + "/details.html");
					if (content == null)
						continue;
					String pfreq = "<div id='frequency'>Frequency: ";
					int i = content.indexOf(pfreq) + pfreq.length();
					String pSize = "<div id='size'>Non-data size: ";
					i = content.indexOf(pSize, i) + pSize.length();
					int nonDataSize = Integer.parseInt(content.substring(i, content.indexOf("</div>", i)));
					if (nonDataSize < 3)
						continue;
					String plink = "<div id='link'><a href=", ptime = "<div id='time'>", pauthor = "<div id='author'>";
//					HashMap<String, ArrayList<String>> authorMethods = new HashMap<>(), projectMethods = new HashMap<>();
					while (true) {
						i = content.indexOf(plink, i);
						if (i == -1)
							break;
						int s = i + plink.length() + "'https://github.com/".length(), e = content.indexOf('\'', s);
						String link = content.substring(s, e);
						e = link.indexOf("/commit/");
						String project = link.substring(0, e);
						s = e + "/commit/".length();
						e = link.indexOf('#', s);
						String commit = link.substring(s, e);
						s = e + "#diff-".length();
						e = link.indexOf('L', s);
						String file = project + "/" + commit + "/" + link.substring(s, e);
						String method = file + "/" + link.substring(e + 1);
//						ArrayList<String> cs = projectMethods.get(project);
//						if (cs == null) {
//							cs = new ArrayList<>();
//							projectMethods.put(project, cs);
//						}
//						cs.add(method);
						s = content.indexOf(ptime, i + e) + ptime.length();
						e = content.indexOf("</div>", s);
						String time = content.substring(s, e);
						long timestamp = Long.parseLong(time);
						Date date = new Date(timestamp * 1000);
					    String sdate = new SimpleDateFormat("yyyy-MM-dd").format(date);
					    if (sdate.compareTo("2016-12-01") < 0) {
					    	i = e;
					    	continue;
					    }
						s = content.indexOf(pauthor, e) + pauthor.length();
						e = content.indexOf("</div>", s);
						String email = content.substring(s, e);
					    String author = commitAuthor.get(commit);
						if (email != null && !email.isEmpty() && !email.equals("null") && author != null && !author.isEmpty() && !author.equals("null")) {
						    HashSet<String> instances = dateInstances.get(sdate);
						    if (instances == null) {
						    	instances = new HashSet<>();
						    	dateInstances.put(sdate, instances);
						    }
						    instances.add("https://github.com/" + link + "\n"
						    		+ author + "\n"
								    + email + "\n"
						    		+ "file:///" 
						    		+ dirPattern.getAbsolutePath().replace('\\', '/')
						    		+ "/sampleChange.html" + "\n");
						}
//						if (author != null && !author.equals("null")) {
//							cs = authorMethods.get(author);
//							if (cs == null) {
//								cs = new ArrayList<>();
//								authorMethods.put(author, cs);
//							}
//							cs.add(method);
//						}
						i = e;
					}
				}
			}
		}
		ArrayList<String> dates = new ArrayList<>(dateInstances.keySet());
		Collections.sort(dates);
		File outDir = new File("T:/github/chang-patterns-sum-" + dir.getParentFile().getName() + "/dates");
		if (!outDir.exists())
			outDir.mkdirs();
		StringBuilder sbSum = new StringBuilder();
		for (int i = dates.size()-1; i >= 0; i--) {
			String date = dates.get(i);
			HashSet<String> instances = dateInstances.get(date);
			System.out.println(date + "\t" + instances.size());
			sbSum.append(date + "," + instances.size() + "\n");
			StringBuilder sb = new StringBuilder();
			for (String instance : instances)
				sb.append(instance + "\n");
			FileIO.writeStringToFile(sb.toString(), outDir.getAbsolutePath() + "/" + date + ".txt");
		}
		FileIO.writeStringToFile(sbSum.toString(), outDir.getAbsolutePath() + "/sum.csv");
	}

	public static void sumStats() {
		commitEmail = read(new File("T:/github/repos-metadata/"));
		HashMap<Integer, HashSet<String>> sizes = new HashMap<>(), frequencies = new HashMap<>(), projects = new HashMap<>(), commits = new HashMap<>(), authors = new HashMap<>(), files = new HashMap<>(), methods = new HashMap<>();
		HashMap<String, Integer> projectPatterns = new HashMap<>(), authorPatterns = new HashMap<>(), commitPatterns = new HashMap<>(), filePatterns = new HashMap<>(), methodPatterns = new HashMap<>(),
				uniqueAuthorPatterns = new HashMap<>(), uniqueProjectPatterns = new HashMap<>();
		HashMap<Integer, HashSet<String>> intervals = new HashMap<>(), 
				authorIntervals = new HashMap<>(), authorSameProjectIntervals = new HashMap<>(), authorCrossProjectIntervals = new HashMap<>(),  
				crossAuthorIntervals = new HashMap<>(), crossAuthorSameProjectIntervals = new HashMap<>(), crossAuthorCrossProjectIntervals = new HashMap<>(),
				projectIntervals = new HashMap<>(), 
				crossProjectIntervals = new HashMap<>(), crossProjectSameAuthorIntervals = new HashMap<>(), crossProjectCrossAuthorIntervals = new HashMap<>();
		for (Map.Entry<String, String> entry : commitEmail.entrySet()) {
			authorPatterns.put(entry.getValue(), 0);
			uniqueAuthorPatterns.put(entry.getValue(), 0);
		}
		
		File dir = new File("D:/projects/ChangeStatICSE2016/code/GraphMining/GraphMiner/output/patterns/repos-5stars-50commits-hybrid-4000/1");
		for (File dirSize : dir.listFiles()) {
			if (!dirSize.isDirectory())
				continue;
			int size = Integer.parseInt(dirSize.getName());
			sizes.put(size, new HashSet<String>());
			for (File dirPattern : dirSize.listFiles()) {
				if (isGood(dirPattern)) {
					sizes.get(size).add(dirPattern.getAbsolutePath());
					Pattern p = new Pattern(size, dirPattern);
					int freq = p.frequency;
					if (freq == 0)
						continue;
					update(frequencies, freq, dirPattern.getAbsolutePath());
					update(projects, count(p.projects), dirPattern.getAbsolutePath());
					HashSet<String> pProjects = new HashSet<>(p.projects);
					if (pProjects.size() == 1)
						update(uniqueProjectPatterns, pProjects);
					update(commits, count(p.commits), dirPattern.getAbsolutePath());
					update(authors, count(p.authors), dirPattern.getAbsolutePath());
					update(files, count(p.files), dirPattern.getAbsolutePath());
					update(methods, count(p.methods), dirPattern.getAbsolutePath());
					update(projectPatterns, p.projects);
					update(authorPatterns, p.authors);
					HashSet<String> pAuthors = new HashSet<>(p.authors);
					if (pAuthors.size() == 1)
						update(uniqueAuthorPatterns, pAuthors);
					update(commitPatterns, p.commits);
					update(filePatterns, p.files);
					update(methodPatterns, p.methodTime.keySet());
					update(intervals, p.intervals);
					update(authorIntervals, p.authorIntervals);
					update(authorSameProjectIntervals, p.authorSameProjectIntervals);
					update(authorCrossProjectIntervals, p.authorCrossProjectIntervals);
					update(crossAuthorIntervals, p.crossAuthorIntervals);
					update(crossAuthorSameProjectIntervals, p.crossAuthorSameProjectIntervals);
					update(crossAuthorCrossProjectIntervals, p.crossAuthorCrossProjectIntervals);
					update(projectIntervals, p.projectIntervals);
				}
			}
		}
		String outPath = "T:/github/chang-patterns-sum-repos-5stars-50commits-4000";
		write(sizes, outPath + "/sizes.csv");
		write(frequencies, outPath + "/frequencies.csv");
		write(projects, outPath + "/projects.csv");
		write(commits, outPath + "/commits.csv");
		write(authors, outPath + "/authors.csv");
		write(files, outPath + "/files.csv");
		write(methods, outPath + "/methods.csv");
		write(intervals, outPath + "/intervals.csv");
		write(authorIntervals, outPath + "/authorIntervals.csv");
		write(authorSameProjectIntervals, outPath + "/authorSameProjectIntervals.csv");
		write(authorCrossProjectIntervals, outPath + "/authorCrossProjectIntervals.csv");
		write(crossAuthorIntervals, outPath + "/crossAuthorIntervals.csv");
		write(crossAuthorSameProjectIntervals, outPath + "/crossAuthorSameProjectIntervals.csv");
		write(crossAuthorCrossProjectIntervals, outPath + "/crossAuthorCrossProjectIntervals.csv");
		write(projectIntervals, outPath + "/projectIntervals.csv");
		write(projectPatterns, outPath + "/projectPatterns.csv");
		write(uniqueProjectPatterns, outPath + "/uniqueProjectPatterns.csv");
		write(authorPatterns, outPath + "/authorPatterns.csv");
		write(uniqueAuthorPatterns, outPath + "/uniqueAuthorPatterns.csv");
		write(commitPatterns, outPath + "/commitPatterns.csv");
		write(filePatterns, outPath + "/filePatterns.csv");
		write(methodPatterns, outPath + "/methodPatterns.csv");
		write(projectAuthors, outPath + "/projectAuthors.csv");
		write(projectCommits, outPath + "/projectCommits.csv");
		int authorsWithPattern = 0;
		for (String author : authorPatterns.keySet())
			if (authorPatterns.get(author) > 0)
				authorsWithPattern++;
		System.out.println("Commits with patterns: " + commitPatterns.keySet().size() + " / " + allCommits.size() + " = " + (commitPatterns.keySet().size() * 100.0 / allCommits.size()) + "%");
		System.out.println("Authors with patterns: " + authorsWithPattern + " / " + allAuthors.size() + " = " + (authorsWithPattern*100.0 / allAuthors.size()) + "%");
		System.out.println(failures + " failures over " + all);
	}

	private static <K, V> void write(HashMap<K, V> map, String filename) {
		StringBuilder sb = new StringBuilder(), sb1 = new StringBuilder();
		ArrayList<K> list = new ArrayList<>(map.keySet());
		if (list.get(0) instanceof Integer)
			Collections.sort((List<Integer>) list);
		else
			Collections.sort(list, new Comparator<K>() {
				@Override
				public int compare(K e1, K e2) {
					V v1 = map.get(e1), v2 = map.get(e2);
					if (v1 instanceof Integer)
						//return (int) v1 - (int) v2; To @Hoang: I temporarily put comment slash here because of compiling error
						return 1;
					else
						return ((HashSet<String>) v1).size() - ((HashSet<String>) v2).size();
				}
			});
		for (K k: list) {
			V v = map.get(k);
			if (v instanceof Integer) {
				sb.append(k + "," + v + "\n");
			}
			else {
				HashSet<String> s = (HashSet<String>) v;
				sb.append(k + "," + s.size());
				for (String str : s)
					sb.append(",file:///" + str);
				sb.append("\n");
				sb1.append(k + "," + s.size() + "\n");
			}
		}
		FileIO.writeStringToFile(sb.toString(), filename);
		if (sb1.length() > 10)
			FileIO.writeStringToFile(sb1.toString(), filename + ".simple.csv");
	}

	private static <E> void update(HashMap<E, HashSet<String>> map, E key, String value) {
		HashSet<String> values = map.get(key);
		if (values == null) {
			values = new HashSet<>();
			map.put(key, values);
		}
		values.add(value);
	}

	private static void update(HashMap<String, Integer> map, Collection<String> keys) {
		for (String key : new HashSet<String>(keys))
			update(map, key);
	}

	private static <E> void update(HashMap<E, Integer> map, E key) {
		int c = 1;
		if (map.containsKey(key))
			c += map.get(key);
		map.put(key, c);		
	}

	private static int count(ArrayList<String> items) {
		HashSet<String> s = new HashSet<>(items);
		return s.size();
	}

	private static boolean isGood(File dirPattern) {
		return dirPattern.isDirectory();
	}

	private static <K, V> void update(HashMap<K, HashSet<V>> target, HashMap<K, HashSet<V>> source) {
		for (K key : source.keySet()) {
			HashSet<V> v = target.get(key);
			if (v == null)
				target.put(key, source.get(key));
			else
				v.addAll(source.get(key));
		}
	}
	
	static class Pattern {
		File dir;
		int size = 0, frequency = 0;
		ArrayList<String> projects = new ArrayList<>(), commits = new ArrayList<>(), files = new ArrayList<>(), authors = new ArrayList<>(), methods = new ArrayList<>();
		HashMap<String, Integer> methodTime = new HashMap<>();
		HashMap<String, String> methodAuthor = new HashMap<>(), methodProject = new HashMap<>();
		HashMap<Integer, HashSet<String>> intervals = new HashMap<>(), 
				authorIntervals = new HashMap<>(), authorSameProjectIntervals = new HashMap<>(), authorCrossProjectIntervals = new HashMap<>(), 
				crossAuthorIntervals = new HashMap<>(), crossAuthorSameProjectIntervals = new HashMap<>(), crossAuthorCrossProjectIntervals = new HashMap<>(),
				projectIntervals = new HashMap<>(), crossProjectIntervals = new HashMap<>(), crossProjectSameAuthorIntervals = new HashMap<>(), crossProjectCrossAuthorIntervals = new HashMap<>();
		
		public Pattern(int size, File dirPattern) {
			dir = dirPattern;
			String content = FileIO.readStringFromFile(dirPattern.getAbsolutePath() + "/details.html");
			if (content == null)
				return;
			this.size = size;
			String pfreq = "<div id='frequency'>Frequency: ";
			int i = content.indexOf(pfreq) + pfreq.length();
			this.frequency = Integer.parseInt(content.substring(i, content.indexOf('<', i)));
			String plink = "<div id='link'><a href=", ptime = "<div id='time'>", pauthor = "<div id='author'>";
			HashMap<String, ArrayList<String>> authorMethods = new HashMap<>(), projectMethods = new HashMap<>();
			while (true) {
				i = content.indexOf(plink, i);
				if (i == -1)
					break;
				int s = i + plink.length() + "'https://github.com/".length(), e = content.indexOf('\'', s);
				String link = content.substring(s, e);
				e = link.indexOf("/commit/");
				String project = link.substring(0, e);
				s = e + "/commit/".length();
				e = link.indexOf('#', s);
				String commit = link.substring(s, e);
				s = e + "#diff-".length();
				e = link.indexOf('L', s);
				String file = project + "/" + commit + "/" + link.substring(s, e);
				String method = file + "/" + link.substring(e + 1);
				ArrayList<String> cs = projectMethods.get(project);
				if (cs == null) {
					cs = new ArrayList<>();
					projectMethods.put(project, cs);
				}
				cs.add(method);
				s = content.indexOf(ptime, i + e) + ptime.length();
				e = content.indexOf("</div>", s);
				String time = content.substring(s, e);
				try {
					methodTime.put(method, Integer.parseInt(time));
				} catch (Exception ex) {
					System.err.println(ex.getMessage());
					failures++;
					continue;
				}
				all++;
				methods.add(method);
				projects.add(project);
				commits.add(commit);
				methodProject.put(method, project);
				s = content.indexOf(pauthor, e) + pauthor.length();
				e = content.indexOf("</div>", s);
				String author = content.substring(s, e);
				if (author != null && !author.equals("null")) {
					authors.add(author);
					methodAuthor.put(method, author);
					cs = authorMethods.get(author);
					if (cs == null) {
						cs = new ArrayList<>();
						authorMethods.put(author, cs);
					}
					cs.add(method);
				}
				files.add(file);
				i = e;
			}
			i = 0;
			intervals = intervals(new HashSet<String>(methods), false);
			crossAuthorIntervals = intervals(new HashSet<String>(methods), true);
			for (String author : authorMethods.keySet())
				update(authorIntervals, intervals(new HashSet<String>(authorMethods.get(author)), false));
			for (String project : projectMethods.keySet())
				update(projectIntervals, intervals(new HashSet<String>(projectMethods.get(project)), false));
		}

		private HashMap<Integer, HashSet<String>> intervals(HashSet<String> methods, boolean cross) {
			HashMap<Integer, HashSet<String>> intervals = new HashMap<>();
			if (methods.size() <= 1) {
				intervals.put(-2, new HashSet<String>(methods));
				return intervals;
			}
			ArrayList<String> l = new ArrayList<>(methods);
			Collections.sort(l, new Comparator<String>() {
				@Override
				public int compare(String m1, String m2) {
					return methodTime.get(m1) - methodTime.get(m2);
				}
			});
			for (int i = 0; i < l.size() - 1; i++) {
				String m2 = l.get(i+1), m1 = l.get(i);
				int interval = -1, t1 = methodTime.get(m1), t2 = methodTime.get(m2);
				if (cross) {
					if (t2 == t1)
						continue;
					String a1 = methodAuthor.get(m1);
					if (a1 != null && a1.equals(methodAuthor.get(m2)))
						continue;
				}
				if (t2 != t1) {
					interval = (t2 - t1) / 3600 / 24;
					if (interval > 0) {
						if (interval <= 7)
							interval = 7;
						else if (interval <= 30)
							interval = 30;
						else if (interval <= 365)
							interval = 365;
						else
							interval = 999;
					}
				}
				HashSet<String> s = intervals.get(interval);
				if (s == null) {
					s = new HashSet<>();
					intervals.put(interval, s);
				}
				s.add(dir.getAbsolutePath() + "," + m1 + "," + m2);
				if (cross) {
					if (methodProject.get(m1).equals(methodProject.get(m2))) {
						s = crossAuthorSameProjectIntervals.get(interval);
						if (s == null) {
							s = new HashSet<>();
							crossAuthorSameProjectIntervals.put(interval, s);
						}
					} else {
						s = crossAuthorCrossProjectIntervals.get(interval);
						if (s == null) {
							s = new HashSet<>();
							crossAuthorCrossProjectIntervals.put(interval, s);
						}
					}
					s.add(dir.getAbsolutePath() + "," + m1 + "," + m2);
				} else {
					if (methodProject.get(m1).equals(methodProject.get(m2))) {
						s = authorSameProjectIntervals.get(interval);
						if (s == null) {
							s = new HashSet<>();
							authorSameProjectIntervals.put(interval, s);
						}
					} else {
						s = authorCrossProjectIntervals.get(interval);
						if (s == null) {
							s = new HashSet<>();
							authorCrossProjectIntervals.put(interval, s);
						}
					}
					s.add(dir.getAbsolutePath() + "," + m1 + "," + m2);
				}
			}
			return intervals;
		}
	}
}
