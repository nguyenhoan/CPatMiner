/**
 * 
 */
package repository;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import utils.FileIO;

/**
 * @author Nguyen Anh Hoan
 *
 */
public class SVNConnector {
	static String[] fixingPatterns = {
			// "issue"
			"issue[\\s]+[0-9]+", "issues[\\s]+[0-9]+", "issue[\\s]+#[0-9]+",
			"issues[\\s]+#[0-9]+", "issue[\\s]+# [0-9]+", "bug"
			/*
			 * ,"bug[\\s]+[0-9]+" ,"bug[\\s]+[0-9]+" ,"bug[\\s]+#[0-9]+"
			 * ,"bug[\\s]+#[0-9]+" ,"bug[\\s]+# [0-9]+" ,"bug id=[0-9]+"
			 */
			, "fix"
			// ,"fix[ ]+#[0-9]+"
			// ,"fixes[ ]+#[0-9]+"
			// ,"pr[ ]+[0-9]+"
			// ,"pr[\\:][ ]+[0-9]+"
			, "error", "exception"
	/*
	 * "\\bfix(s|es|ing|ed)?\\b", "\\berror(s)?\\b", "\\bbug(s)?",
	 * "\\bissue(s)?"
	 */
	};
	private SVNRepository repository = null;
	private ISVNAuthenticationManager authManager;
	private SVNClientManager clientManager = null;
	private SVNURL url;
	private String username;
	private String password;
	private long latestRevision = -1;

	public SVNConnector(String url, String username, String password) {
		initialize(url, username, password);
	}

	private void initialize(String url, String username, String password) {
		try {
			this.url = SVNURL.parseURIEncoded(url);
		} catch (SVNException e) {
			e.printStackTrace();
		}
		this.username = username;
		this.password = password;
		// For using over http:// and https://
		DAVRepositoryFactory.setup();
		// For using over svn:// and svn+xxx://
		SVNRepositoryFactoryImpl.setup();
		// For using over file:///
		FSRepositoryFactory.setup();
	}

	public SVNRepository getRepository() {
		return repository;
	}

	public long getLatestRevision() {
		return latestRevision;
	}

	public void setLatestRevision(long latestRevision) {
		this.latestRevision = latestRevision;
	}

	public void setLatestRevision() {
		try {
			this.latestRevision = this.repository.getLatestRevision();
		} catch (SVNException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 */
	public boolean connect() {
		try {
			authManager = SVNWCUtil.createDefaultAuthenticationManager(username, password.toCharArray());
			repository = SVNRepositoryFactory.create(this.url);
			getRepository().setAuthenticationManager(authManager);
			// repository.testConnection();
			// System.out.println("Connected to SVN");
		} catch (SVNException e) {
			System.err.println("Error connecting to " + url + ". "
					+ e.getMessage());
			// e.printStackTrace();
			return false;
		}
		return true;
	}

	public long update(String wcPath, long revision) {
		clientManager = SVNClientManager.newInstance(
				SVNWCUtil.createDefaultOptions(true), authManager);
		SVNUpdateClient updateClient = clientManager.getUpdateClient();
		/*
		 * sets externals not to be ignored during the update
		 */
		updateClient.setIgnoreExternals(false);
		/*
		 * returns the number of the revision wcPath was updated to
		 */
		File wcFile = new File(wcPath);
		if (!wcFile.exists())
			wcFile.mkdirs();
		try {
			return updateClient.doUpdate(wcFile, SVNRevision.create(revision),
					SVNDepth.INFINITY, true, true);
		} catch (SVNException e) {
			try {
				return updateClient.doCheckout(url, wcFile,
						SVNRevision.create(revision),
						SVNRevision.create(revision), SVNDepth.INFINITY, true);
			} catch (SVNException e1) {
				e1.printStackTrace();
			}
		}

		return -1;
	}

	public boolean checkFileExistence(String path, long revision) {
		try {
			return getRepository().checkPath(path, revision) == SVNNodeKind.FILE;
		} catch (SVNException e) {
			e.printStackTrace();
		}

		return false;
	}

	public String getFile(String path, long revision) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			getRepository().getFile(path, revision, null, out);
		} catch (SVNException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		return out.toString();
	}

	public int[] countLOC(String path, long revision) {
		int[] count = { 0, 0 };
		try {
			Collection<?> entries = getRepository().getDir(path, revision,
					null, (Collection<?>) null);
			Iterator<?> iterator = entries.iterator();
			while (iterator.hasNext()) {
				SVNDirEntry entry = (SVNDirEntry) iterator.next();
				if (entry.getKind() == SVNNodeKind.DIR) {
					int[] sub = countLOC((path.equals("")) ? entry.getName()
							: path + "/" + entry.getName(), revision);
					count[0] += sub[0];
					count[1] += sub[1];
				} else if (entry.getName().endsWith(".java")) {
					count[0]++;
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					getRepository().getFile(path + "/" + entry.getName(),
							revision, null, out);
					System.out.println(path + "/" + entry.getName());
					String content = out.toString();
					int index = -1;
					while ((index = content.indexOf('\n', index + 1)) > -1)
						count[1]++;
				}
			}
		} catch (SVNException e) {
			e.printStackTrace();
		}

		return count;
	}

	public int[] countFiles(String path, long revision) {
		int[] count = { 0, 0 };
		try {
			Collection<?> entries = getRepository().getDir(path, revision,
					null, (Collection<?>) null);
			Iterator<?> iterator = entries.iterator();
			while (iterator.hasNext()) {
				SVNDirEntry entry = (SVNDirEntry) iterator.next();
				if (entry.getKind() == SVNNodeKind.DIR) {
					int[] sub = countFiles((path.equals("")) ? entry.getName()
							: path + "/" + entry.getName(), revision);
					count[0] += sub[0];
					count[1] += sub[1];
				} else if (entry.getName().endsWith(".java")) {
					count[0]++;
				} else if (entry.getName().endsWith(".jar")
						&& entry.getName().trim().contains("junit")) {
					count[1]++;
				}
			}
		} catch (SVNException e) {
			e.printStackTrace();
		}

		return count;
	}

	public String getAbsolutePath(SVNDirEntry entry) {
		String repRootPath = entry.getRepositoryRoot().toDecodedString();
		return entry.getURL().toDecodedString().substring(repRootPath.length());
	}

	private boolean hasExtension(HashSet<String> extensions, String path) {
		for (String extension : extensions) {
			if (path.endsWith(extension)) {
				return true;
			}
		}
		return false;
	}

	public void checkout(String localRootPath, String path, long revision,
			HashSet<String> extensions) {
		try {
			Collection<?> entries = repository.getDir(path, revision, null,
					(Collection<?>) null);
			Iterator<?> iterator = entries.iterator();
			while (iterator.hasNext()) {
				SVNDirEntry entry = (SVNDirEntry) iterator.next();
				SVNNodeKind nodeKind = entry.getKind();
				if (nodeKind == SVNNodeKind.DIR) {
					File file = new File(localRootPath + "/" + revision + "/"
							+ path + "/" + entry.getName());
					file = new File(file.getAbsolutePath());
					if (!file.exists()) {
						file.mkdirs();
					}
					checkout(localRootPath, (path.equals("")) ? entry.getName()
							: path + "/" + entry.getName(), revision,
							extensions);
				} else if (nodeKind == SVNNodeKind.FILE) {
					File file = new File(localRootPath + "/" + revision + "/"
							+ path + "/" + entry.getName());
					file = new File(file.getAbsolutePath());
					if (!file.exists()) {
						boolean hasExtension = hasExtension(extensions,
								entry.getName());
						if (hasExtension) {
							ByteArrayOutputStream out = new ByteArrayOutputStream();
							getRepository().getFile(
									path + "/" + entry.getName(), revision,
									null, out);
							// System.out.println(path + "/" + entry.getName());
							String content = out.toString();
							// FileIO.writeStringToFile(content, localRootPath +
							// "/" + revision + "/" + path + "/" +
							// entry.getName());
							FileIO.writeStringToFile(content,
									file.getAbsolutePath());
						}
					}
				}
			}
		} catch (SVNException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	public ArrayList<Integer> getJavaFixRevisions(long start, long end) {
		ArrayList<Integer> revs = new ArrayList<Integer>();
		Collection logEntries = null;
		try {
			logEntries = repository.log(new String[] { "" }, null, start, end,
					true, true);
		} catch (SVNException svne) {
			System.out.println("error while collecting log information for '"
					+ url + "': " + svne.getMessage());
			return new ArrayList<>();
		}
		for (Iterator entries = logEntries.iterator(); entries.hasNext();) {
			/*
			 * gets a next SVNLogEntry
			 */
			SVNLogEntry logEntry = (SVNLogEntry) entries.next();
			if (logEntry.getMessage() == null)
				continue;
			String message = logEntry.getMessage().toLowerCase();
			if (isFixingCommit(message)) {
				if (logEntry.getChangedPaths().size() > 0) {
					Set changedPathsSet = logEntry.getChangedPaths().keySet();

					for (Iterator changedPaths = changedPathsSet.iterator(); changedPaths
							.hasNext();) {
						SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry
								.getChangedPaths().get(changedPaths.next());
						if (entryPath.getPath().endsWith(".java")) {
							revs.add((int) logEntry.getRevision());
							System.out.println(logEntry.getRevision());
							break;
						}
					}
				}
			}
		}

		return revs;
	}

	public static boolean isFixingCommit(String commitLog) {
		Pattern p;
		if (commitLog != null) {
			String tmpLog = commitLog.toLowerCase();
			for (int i = 0; i < fixingPatterns.length; i++) {
				String patternStr = fixingPatterns[i];
				p = Pattern.compile(patternStr);
				Matcher m = p.matcher(tmpLog);
				boolean isFixing = m.find();
				if (isFixing) {
					return true;
				}
			}
		}
		return false;
	}

	public static String getFile(SVNRepository repository, String path,
			long revision) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			repository.getFile(path, revision, null, out);
		} catch (SVNException e) {
			System.err.println("Path " + path + " not found at revision "
					+ revision);
			// e.printStackTrace();
		}

		return out.toString();
	}

	public static int[] countLOC(SVNRepository repository, String path,
			long revision) {
		int[] count = { 0, 0 };
		try {
			Collection<?> entries = repository.getDir(path, revision, null,
					(Collection<?>) null);
			Iterator<?> iterator = entries.iterator();
			while (iterator.hasNext()) {
				SVNDirEntry entry = (SVNDirEntry) iterator.next();
				if (entry.getKind() == SVNNodeKind.DIR) {
					int[] sub = countLOC(repository,
							(path.equals("")) ? entry.getName() : path + "/"
									+ entry.getName(), revision);
					count[0] += sub[0];
					count[1] += sub[1];
				} else if (entry.getName().endsWith(".java")) {
					count[0]++;
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					repository.getFile(path + "/" + entry.getName(), revision,
							null, out);
					// System.out.println(path + "/" + entry.getName());
					String content = out.toString();
					int index = -1;
					while ((index = content.indexOf('\n', index + 1)) > -1)
						count[1]++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return count;
	}

	public static int[] countFiles(SVNRepository repository, String path,
			long revision) {
		int[] count = { 0, 0 };
		try {
			Collection<?> entries = repository.getDir(path, revision, null,
					(Collection<?>) null);
			Iterator<?> iterator = entries.iterator();
			while (iterator.hasNext()) {
				SVNDirEntry entry = (SVNDirEntry) iterator.next();
				if (entry.getKind() == SVNNodeKind.DIR) {
					int[] sub = countFiles(repository,
							(path.equals("")) ? entry.getName() : path + "/"
									+ entry.getName(), revision);
					count[0] += sub[0];
					count[1] += sub[1];
				} else if (entry.getName().endsWith(".java")) {
					count[0]++;
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					repository.getFile(path + "/" + entry.getName(), revision,
							null, out);
					// System.out.println(path + "/" + entry.getName());
					String content = out.toString();
					int index = -1;
					while ((index = content.indexOf('\n', index + 1)) > -1)
						count[1]++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return count;
	}

	public static boolean isSourceCodeChange(Set<String> paths,
			String[] languages) {
		for (String path : paths) {
			for (String lang : languages) {
				if (path.endsWith(lang))
					return true;
			}
		}
		return false;
	}
}
