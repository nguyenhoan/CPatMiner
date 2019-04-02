package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.InternetAddress;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import utils.FileIO;
import utils.NotifyingBlockingThreadPoolExecutor;
import repository.GitConnector;

public class MainRepoAnalyzer {
	private static final int THREAD_POOL_SIZE = 4;

	private static final Callable<Boolean> blockingTimeoutCallback = new Callable<Boolean>() {
		@Override
		public Boolean call() throws Exception {
			return true; // keep waiting
		}
	};
	private static final NotifyingBlockingThreadPoolExecutor pool = new NotifyingBlockingThreadPoolExecutor(THREAD_POOL_SIZE, 2 * THREAD_POOL_SIZE, 15, TimeUnit.SECONDS, 200, TimeUnit.MILLISECONDS, blockingTimeoutCallback);
	
	public static String inputPath = "G:/github/repos-bare";
	private static PrintStream ps;
	
	public static void main(String[] args) throws FileNotFoundException {
		System.setErr(new PrintStream(new FileOutputStream("G:/github/err.txt"), true));
		ps = new PrintStream(new FileOutputStream("T:/github/repos.csv"), true);
		File dir = new File(inputPath);
		for (File sub : dir.listFiles())
			analyze(sub, "");
		try {
			pool.await(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (final InterruptedException e) { }
		String content = FileIO.readStringFromFile("G:/github/repos.csv");
		Scanner sc = new Scanner(content);
		HashMap<String, HashSet<String>> authorRepos = new HashMap<>();
		HashMap<String, Integer> authorCommits = new HashMap<>();
		int numOfRepos = 0, numOfAllCommits = 0, numOfValidCommits = 0;
		StringBuilder sbRepos = new StringBuilder(), sbAuthors = new StringBuilder();
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			String[] parts = line.split(",");
			String name = parts[0];
			int numOfCommits = Integer.parseInt(parts[1]), numOfAuthors = Integer.parseInt(parts[2]);
			if (numOfCommits >= 10000 && numOfAuthors >= 10) {
				numOfRepos++;
				for (int i = 3; i < parts.length; i++) {
					HashSet<String> repos = authorRepos.get(parts[i]);
					if (repos == null) {
						repos = new HashSet<>();
						authorRepos.put(parts[i], repos);
					}
					repos.add(name);
					Integer count = authorCommits.get(parts[i]);
					if (count == null)
						count = 1;
					else
						count++;
					authorCommits.put(parts[i], count);
				}
				System.out.println(name + "\t" + numOfCommits + "\t" + numOfAuthors);
				sbRepos.append(name + "," + numOfCommits + "," + numOfAuthors + "\n");
				numOfAllCommits += numOfCommits;
				numOfValidCommits += parts.length - 3;
			}
		}
		sc.close();
		System.out.println("Repos: " + numOfRepos);
		System.out.println("Commits: " + numOfAllCommits);
		System.out.println("Valid commits: " + numOfValidCommits);
		System.out.println("Authors: " + authorRepos.size());
		for (String author : authorRepos.keySet()) {
			sbAuthors.append(author + "," + authorRepos.get(author).size() + "," + authorCommits.get(author));
			for (String repo : authorRepos.get(author))
				sbAuthors.append("," + repo);
			sbAuthors.append("\n");
		}
		FileIO.writeStringToFile(sbRepos.toString(), "G:/github/selected-repos.csv");
		FileIO.writeStringToFile(sbAuthors.toString(), "G:/github/selected-authors.csv");
	}

	private static void analyze(final File dir, final String parent) {
		if (!dir.isDirectory())
			return;
		final String name = (parent.isEmpty() ? "" : parent + "/") + dir.getName();
		//if (new File(outputPath + "/" + name).exists()) return;
		final File git = new File(dir, ".git");
		if (git.exists()) {
			pool.execute(new Runnable() {
				@Override
				public void run() {
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
						ps.println(name + ",0,0");
					} else {
						HashSet<String> commitNames = new HashSet<>(), authors = new HashSet<>();
						StringBuilder sb = new StringBuilder();
						for (RevCommit rc : commits) {
							commitNames.add(rc.getName());
							String author = null;
							try {
								PersonIdent person = rc.getAuthorIdent();
								author = person.getEmailAddress();
								InternetAddress email = new InternetAddress(author);
								email.validate();
							} catch (Exception e) {
								author = null;
							}
							// DEBUG
							if (author == null || author.isEmpty()) {
								System.err.println(name + " " + rc.getName());
							} else {
								authors.add(author);
								sb.append("," + author);
							}
						}
						ps.println(name + "," + commitNames.size() + "," + authors.size() + sb.toString());
					}
					gitConn.close();
				}
			});
		}
		else
			for (File sub : dir.listFiles())
				analyze(sub, name);
	}

}
