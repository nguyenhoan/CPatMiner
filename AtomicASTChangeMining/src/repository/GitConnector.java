package repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.NullOutputStream;

public class GitConnector extends AbstractConnector {
	private String url;
	private int numberOfCommits = -1, numberOfCodeCommits = -1;

	public Git getGit() {
		return git;
	}

	public Repository getRepository() {
		return repository;
	}

	private Git git;
	private Repository repository;

	public GitConnector(String url) {
		this.url = url;
	}

	public int getNumberOfCommits() {
		return numberOfCommits;
	}

	public int getNumberOfCodeCommits() {
		return numberOfCodeCommits;
	}

	public boolean connect() {
		FileRepositoryBuilder builder = new FileRepositoryBuilder();
		try {
			repository = builder.setGitDir(new File(url)).readEnvironment() // scan
																			// environment
																			// GIT_*
																			// variables
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

	public void getFileChanges(String extension) {
		Iterable<RevCommit> commits = null;
		try {
			commits = git.log().call();
		} catch (GitAPIException e) {
			System.err.println(e.getMessage());
		}
		if (commits == null)
			return;
		for (RevCommit commit : commits) {
			if (commit.getParentCount() > 0) {
				RevWalk rw = new RevWalk(repository);
				RevCommit parent = null;
				try {
					parent = rw.parseCommit(commit.getParent(0).getId());
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
				if (parent == null) {
					rw.close();
					continue;
				}
				DiffFormatter df = new DiffFormatter(NullOutputStream.INSTANCE);
				df.setRepository(repository);
				df.setDiffComparator(RawTextComparator.DEFAULT);
				df.setDetectRenames(true);
				if (extension != null)
					df.setPathFilter(PathSuffixFilter.create(extension));
				List<DiffEntry> diffs = null;
				try {
					diffs = df.scan(parent.getTree(), commit.getTree());
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
				if (diffs == null) {
					rw.close();
					df.close();
					continue;
				}
				if (!diffs.isEmpty()) {
					// System.out.println(commit.getName());
					System.out.println(commit.getCommitTime());
					// System.out.println(commit.getFullMessage());
					for (DiffEntry diff : diffs) {
						if (diff.getOldMode().getObjectType() == Constants.OBJ_BLOB
								&& diff.getNewMode().getObjectType() == Constants.OBJ_BLOB) {
							// System.out.println(diff.getChangeType() + ": " +
							// diff.getOldPath() + " --> " + diff.getNewPath());
							ObjectLoader ldr = null;
							@SuppressWarnings("unused")
							String oldContent = null, newContent = null;
							try {
								ldr = repository.open(diff.getOldId()
										.toObjectId(), Constants.OBJ_BLOB);
								oldContent = new String(ldr.getCachedBytes());
							} catch (IOException e) {
								System.err.println(e.getMessage());
							}
							try {
								ldr = repository.open(diff.getNewId()
										.toObjectId(), Constants.OBJ_BLOB);
								newContent = new String(ldr.getCachedBytes());
							} catch (IOException e) {
								System.err.println(e.getMessage());
							}
							/*
							 * System.out.println(oldContent);
							 * System.out.println(newContent);
							 */
						}
					}
				}
				rw.close();
				df.close();
			}
		}
	}

	public String getFileContent(ObjectId objectId, int objectType) {
		String content = null;
		try {
			ObjectLoader ldr = repository.open(objectId, objectType);
			content = new String(ldr.getCachedBytes());
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
		return content;
	}

	public String getFileContent(ObjectId objectId) {
		return getFileContent(objectId, Constants.OBJ_BLOB);
	}

	public ArrayList<Integer> getJavaFixRevisions() {
		ArrayList<Integer> revisions = new ArrayList<>();
		Iterable<RevCommit> commits = null;
		try {
			commits = git.log().call();
		} catch (GitAPIException e) {
			System.err.println(e.getMessage());
		}
		if (commits == null)
			return revisions;
		for (RevCommit commit : commits) {
			if (commit.getParentCount() > 0) {
				if (!isFixingCommit(commit.getFullMessage()))
					continue;
				RevWalk rw = new RevWalk(repository);
				RevCommit parent = null;
				try {
					parent = rw.parseCommit(commit.getParent(0).getId());
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
				if (parent == null) {
					rw.close();
					continue;
				}
				DiffFormatter df = new DiffFormatter(NullOutputStream.INSTANCE);
				df.setRepository(repository);
				df.setDiffComparator(RawTextComparator.DEFAULT);
				df.setDetectRenames(true);
				df.setPathFilter(PathSuffixFilter.create(".java"));
				List<DiffEntry> diffs = null;
				try {
					diffs = df.scan(parent.getTree(), commit.getTree());
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
				if (diffs != null && !diffs.isEmpty()) {
					int index = Collections.binarySearch(revisions,
							commit.getCommitTime());
					if (index < 0)
						index = -index - 1;
					revisions.add(index, commit.getCommitTime());
				}
				rw.close();
				df.close();
			}
		}
		return revisions;
	}

	public int getNumberOfCommits(String extension) {
		Iterable<RevCommit> commits = null;
		try {
			commits = git.log().call();
		} catch (GitAPIException e) {
			System.err.println(e.getMessage());
		}
		if (commits == null)
			return -1;
		this.numberOfCommits = 0;
		this.numberOfCodeCommits = 0;
		for (RevCommit commit : commits) {
			this.numberOfCommits++;
			if (extension == null) {
				continue;
			}
			if (commit.getParentCount() > 0) {
				RevWalk rw = new RevWalk(repository);
				RevCommit parent = null;
				try {
					parent = rw.parseCommit(commit.getParent(0).getId());
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
				if (parent == null) {
					rw.close();
					continue;
				}
				DiffFormatter df = new DiffFormatter(NullOutputStream.INSTANCE);
				df.setRepository(repository);
				df.setDiffComparator(RawTextComparator.DEFAULT);
				df.setDetectRenames(true);
				if (extension != null)
					df.setPathFilter(PathSuffixFilter.create(extension));
				List<DiffEntry> diffs = null;
				try {
					diffs = df.scan(parent.getTree(), commit.getTree());
				} catch (IOException e) {
					System.err.println(e.getMessage());
				}
				if (diffs == null) {
					rw.close();
					df.close();
					continue;
				}
				if (!diffs.isEmpty()) {
					this.numberOfCodeCommits++;
				}
				rw.close();
				df.close();
			}
		}
		if (extension == null)
			return this.numberOfCommits;
		return this.numberOfCodeCommits;
	}

	public RevCommit getCommit(String commitId) {
		try (RevWalk walk = new RevWalk(repository)) {
            ObjectId id = repository.resolve(commitId);
            RevCommit commit= walk.parseCommit(id);
            walk.dispose();
            return commit;
		} catch (IOException e) {
		}
		return null;
	}

	public ArrayList<ChangedFile> getChangedFiles(RevCommit commit, String string) {
		ArrayList<ChangedFile> files = new ArrayList<>();
		RevWalk rw = new RevWalk(repository );
		RevCommit parent = null;
		try {
			parent = rw.parseCommit(commit.getParent(0).getId());
		} catch (IOException e1) {
			System.err.println(e1.getMessage());
		}
		if (parent != null) {
			DiffFormatter df = new DiffFormatter(NullOutputStream.INSTANCE);
			df.setRepository(repository);
			df.setDiffComparator(RawTextComparator.DEFAULT);
			df.setDetectRenames(true);
			df.setPathFilter(PathSuffixFilter.create(".java"));
			List<DiffEntry> diffs = null;
			try {
				diffs = df.scan(parent.getTree(), commit.getTree());
			} catch (IOException e1) {
				System.err.println(e1.getMessage());
			}
			if (diffs != null && !diffs.isEmpty()) {
				for (DiffEntry diff : diffs) {
					if (diff.getChangeType() == ChangeType.MODIFY && diff.getOldMode().getObjectType() == Constants.OBJ_BLOB && diff.getNewMode().getObjectType() == Constants.OBJ_BLOB) {
						ObjectLoader ldr = null;
						String oldContent = null, newContent = null;
						try {
							ldr = repository.open(diff.getOldId().toObjectId(), Constants.OBJ_BLOB);
							oldContent = new String(ldr.getCachedBytes());
							ldr = repository.open(diff.getNewId().toObjectId(), Constants.OBJ_BLOB);
							newContent = new String(ldr.getCachedBytes());
						} catch (IOException e1) {
							System.err.println(e1.getMessage());
						}
						if (oldContent != null && newContent != null) {
							files.add(new ChangedFile(diff.getNewPath(), newContent, diff.getOldPath(), oldContent));
						}
					}
				}
			}
			df.close();
		}
		rw.close();
		return files;
	}
}
