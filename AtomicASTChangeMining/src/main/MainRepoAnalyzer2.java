package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

import utils.FileIO;
import repository.GitConnector;

public class MainRepoAnalyzer2 {
	public static String inputPath = "G:/github/repos-bare";
	private static HashMap<String, HashSet<String>> repoCommits = new HashMap<>();
	
	public static void main(String[] args) throws FileNotFoundException {
		System.setErr(new PrintStream(new FileOutputStream("G:/github/err.txt"), true));

		String content = FileIO.readStringFromFile("G:/github/selected-repos.csv");
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			int index = line.indexOf(',');
			String name = line.substring(0, index);
			File dir = new File(inputPath + "/" + name);
				analyze(dir, name);
		}
		sc.close();
		
		ArrayList<String> l = new ArrayList<>(repoCommits.keySet());
		Collections.sort(l, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return repoCommits.get(s2).size() - repoCommits.get(s1).size();
			}
		});
		for (int i = 0; i < l.size()-1; i++) {
			for (int j = i+1; j < l.size(); j++) {
				HashSet<String> inter = new HashSet<>(repoCommits.get(l.get(i)));
				inter.retainAll(repoCommits.get(l.get(j)));
				if (inter.size() * 10 >= repoCommits.get(l.get(i)).size() || inter.size() * 10 >= repoCommits.get(l.get(j)).size())
					System.out.println(l.get(i) + "\t" + l.get(j) + "\t" + repoCommits.get(l.get(i)).size() + "\t" + repoCommits.get(l.get(j)).size() + "\t" + inter.size());
			}
		}
	}

	private static void analyze(final File dir, final String name) {
		if (!dir.isDirectory())
			return;
		final File git = new File(dir, ".git");
		if (git.exists()) {
			System.out.println(name);
			GitConnector gitConn = new GitConnector(git.getAbsolutePath());
			gitConn.connect();
			Iterable<RevCommit> commits = null;
			try {
				commits = gitConn.getGit().log().call();
			} catch (GitAPIException e) {
				System.err.println(e.getMessage());
			} catch (RuntimeException e1) {
				System.err.println(e1.getMessage());
			}
			if (commits == null) {
				
			} else {
				HashSet<String> commitNames = new HashSet<>();
				for (RevCommit rc : commits) {
					commitNames.add(rc.getName());
				}
				repoCommits.put(name, commitNames);
			}
			gitConn.close();
		}
		else
			for (File sub : dir.listFiles())
				analyze(sub, name);
	}

}
