package utils;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;


import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;


public class GitConnector extends VCSConnector {

	private String url;

	private Git git;
	private Repository repository;
	private String ASTDir;
	private String path;
	private ArrayList<String> ASTs;
	private ExecutorService executor;
	private CountDownLatch latch;

	public GitConnector(String url) {
		this.url = url;
	}

	public boolean connect() {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		try {
			repository = builder.setGitDir(new File(url))
					.readEnvironment() // scan environment GIT_* variables
					.findGitDir() // scan up the file system tree
					.build();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return false;
		}
		try {
			if (repository.getBranch() == null)
				return false;
		} catch (IOException e) {
			return false;
		}
		git = new Git(repository);
		return true;
	}

	public void close() {
		this.git.close();
		this.repository.close();
	}

	public Iterable<RevCommit> log() {
		try {
			return git.log().call();
		} catch (GitAPIException e) {
			System.err.println(e.getMessage());
			return null;
		}
	}


	public ArrayList<String> getFileFromCommit(String commithash, String fileName) throws IOException, GitAPIException {
		ArrayList<String> bothFiles = new ArrayList<>();
		Repository repo = git.getRepository();
		ObjectId lastCommitId = repository.resolve(commithash);
		try (RevWalk walk = new RevWalk(repo)) {
			RevCommit head = walk.parseCommit(lastCommitId);
			// and using commit's tree find the path
			RevTree tree = head.getTree();
			//            System.out.println("Having tree: " + tree);
			bothFiles.add(getSpecificFile(fileName, tree));

			RevCommit[] parents = head.getParents();
			RevCommit parentHead = walk.parseCommit(parents[0]);
			RevTree parentTree = parentHead.getTree();
			bothFiles.add(getSpecificFile(fileName, parentTree));
			head.disposeBody();
			parentHead.disposeBody();
			walk.dispose();
		}
		return bothFiles;
	}

	private String getSpecificFile(String fileName, RevTree tree) throws IOException {
		// now try to find a specific file
		try (TreeWalk treeWalk = new TreeWalk(repository)) {
			treeWalk.addTree(tree);
			treeWalk.setRecursive(true);
			treeWalk.setFilter(PathFilter.create(fileName));
			if (!treeWalk.next()) {
				throw new IllegalStateException("Did not find expected file " + fileName);
			}

			ObjectId objectId = treeWalk.getObjectId(0);
			//                ObjectLoader loader = repository.open(objectId);

			// and then one can the loader to read the file
			//                loader.copyTo(System.out);
			ObjectLoader ldr = repository.open(objectId, Constants.OBJ_BLOB);
			String fileContents = new String(ldr.getCachedBytes());
			return fileContents;
		}
	}

	public String getPath() {
		return this.path;
	}

}
