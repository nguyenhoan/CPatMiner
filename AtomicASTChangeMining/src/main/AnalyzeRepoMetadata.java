package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.revwalk.RevCommit;

import repository.GitConnector;
import utils.FileIO;
import utils.NotifyingBlockingThreadPoolExecutor;

public class AnalyzeRepoMetadata {
	private static final int THREAD_POOL_SIZE = 8;

	private static final Callable<Boolean> blockingTimeoutCallback = new Callable<Boolean>() {
		@Override
		public Boolean call() throws Exception {
			return true; // keep waiting
		}
	};
	private static final NotifyingBlockingThreadPoolExecutor pool = new NotifyingBlockingThreadPoolExecutor(THREAD_POOL_SIZE, 2 * THREAD_POOL_SIZE, 15, TimeUnit.SECONDS, 200, TimeUnit.MILLISECONDS, blockingTimeoutCallback);


	public static void main(String[] args) throws NoHeadException, GitAPIException, FileNotFoundException {
//		extractRepoMetadata();
		readRepoMetadata();
	}


	private static void readRepoMetadata() throws FileNotFoundException {
		File dir = new File("T:/github/repos-metadata/");
		HashSet<String> commits = new HashSet<>(), emails = new HashSet<>();
		PrintStream ps = new PrintStream(new FileOutputStream("T:/github/repos-metadata/sum.csv"));
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".email")) {
				String name = file.getName().substring(0, file.getName().length() - ".email".length());
				System.out.println(name);
				if (!isActive("T:/github/repos-metadata/" + name + ".time"))
					continue;
				HashMap<String, String> commitEmail = (HashMap<String, String>) FileIO.readObjectFromFile(file.getAbsolutePath());
				HashSet<String> keys = new HashSet<>(commitEmail.keySet()), values = new HashSet<>(commitEmail.values());
				if (keys.size() < 50)
					continue;
				ps.println(name + "," + keys.size() + "," + values.size());
				commits.addAll(keys);
				emails.addAll(values);
			}
		}
		ps.println("All," + commits.size() + "," + emails.size());
		ps.flush();
		ps.close();
	}


	private static boolean isActive(String path) {
		HashMap<String, Integer> commitTime = (HashMap<String, Integer>) FileIO.readObjectFromFile(path);
		for (int time : new HashSet<Integer>(commitTime.values())) {
			Date date = new Date(((long) time) * 1000);
		    String sdate = new SimpleDateFormat("yyyy-MM-dd").format(date);
		    if (sdate.compareTo("2017-02-01") >= 0)
		    	return true;
		}
		return false;
	}


	public static void extractRepoMetadata() {
//		final String inputPath = "T:/github/repos-selected";
//		final String inputPath = "G:/github/repos-5stars-50commits";
		final String inputPath = "E:/github/repos-5stars-50commits";

		String content = FileIO.readStringFromFile(new File(inputPath).getParentFile().getAbsolutePath() + "/" + new File(inputPath).getName() + ".csv");
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			int index = line.indexOf(',');
			if (index == -1)
				index = line.length();
			final String name = line.substring(0, index);
			pool.execute(new Runnable() {
				@Override
				public void run() {
					System.out.println(name);
					File git = new File(inputPath + "/" + name + "/.git");
					GitConnector gitConn = new GitConnector(git.getAbsolutePath());
					gitConn.connect();
					Iterable<RevCommit> commits = null;
					try {
						ObjectId head = gitConn.getRepository().resolve(Constants.HEAD);
						commits = gitConn.getGit().log().add(head).call();
					} catch (IOException e) {
						e.printStackTrace();
					} catch (NoHeadException e) {
						e.printStackTrace();
					} catch (GitAPIException e) {
						e.printStackTrace();
					}
					if (commits == null)
						return;
					HashMap<String, String> commitEmail = new HashMap<>(), commitAuthor = new HashMap<>();
					HashMap<String, Integer> commitTime = new HashMap<>();
					StringBuilder sb = new StringBuilder();
					for (RevCommit rc : commits) {
						String email = null, author = null;
						try {
							PersonIdent person = rc.getAuthorIdent();
							email = person.getEmailAddress();
							author = person.getName();
						} catch (Exception e) {
							email = null;
						}
						commitTime.put(rc.getName(), rc.getCommitTime());
						if (email != null && !email.isEmpty() && author != null && !author.isEmpty()) {
							commitEmail.put(rc.getName(), email);
							commitAuthor.put(rc.getName(), author);
						}
						sb.append(rc.getName() + "," + rc.getCommitTime() + "," + email + "\n");
					}
					FileIO.writeObjectToFile(commitEmail, "T:/github/repos-metadata/" + name.replace("/", "---") + ".email", false);
					FileIO.writeObjectToFile(commitAuthor, "T:/github/repos-metadata/" + name.replace("/", "---") + ".author", false);
					FileIO.writeObjectToFile(commitTime, "T:/github/repos-metadata/" + name.replace("/", "---") + ".time", false);
					FileIO.writeStringToFile(sb.toString(), "T:/github/repos-metadata/" + name.replace("/", "---") + ".csv");
					gitConn.close();
				}
			});
		}
		try {
			pool.await(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (final InterruptedException e) { }
		sc.close();
	}

}
