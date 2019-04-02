package tests;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import utils.FileIO;
import utils.GitConnector;

public class TestParseGitCommit {

	public static void main(String[] args) throws IOException, GitAPIException {
		String reposPath = "T:/github/repos-selected", content = FileIO.readStringFromFile(new File(reposPath).getParentFile().getAbsolutePath() + "/repos-selected.csv");
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			int index = line.indexOf(',');
			if (index == -1)
				index = line.length();
			final String name = line.substring(0, index);
			String project = reposPath + "/" + name, url = project + "/.git";
			FileRepositoryBuilder builder = new FileRepositoryBuilder();
			Repository repository = null;
			try {
				repository = builder.setGitDir(new File(url))
						.readEnvironment() // scan environment GIT_* variables
						.findGitDir() // scan up the file system tree
						.build();
			} catch (IOException e) {
				System.err.println(e.getMessage());
				continue;
			}
			Git git = new Git(repository);
			Iterable<RevCommit> commits = git.log().call();
	        for (RevCommit commit : commits){
	        	String commithash = commit.getName();
	        	System.out.println(project + ": " + commithash);
	    		ObjectId lastCommitId = repository.resolve(commithash);
	    		try (RevWalk walk = new RevWalk(repository)) {
	    			RevCommit head = walk.parseCommit(lastCommitId);
	    			RevTree tree = head.getTree();
	    			walk.dispose();
	    		}
	        }
	        git.close();
	        repository.close();
		}
		sc.close();
	}

}
