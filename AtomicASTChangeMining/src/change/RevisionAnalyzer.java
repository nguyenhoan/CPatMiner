package change;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.eclipse.jgit.util.io.NullOutputStream;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import utils.Config;
import change.ChangeEntity.Type;

public class RevisionAnalyzer {
	private ChangeAnalyzer changeAnalyzer;
	private long revision;
	private RevCommit gitCommit;

	private HashSet<CFile> mappedFilesM = new HashSet<CFile>(),
			mappedFilesN = new HashSet<CFile>();
	private HashSet<CClass> classesM = new HashSet<CClass>(),
			classesN = new HashSet<CClass>();
	private HashSet<CClass> mappedClassesM = new HashSet<CClass>(),
			mappedClassesN = new HashSet<CClass>();
	private HashSet<CMethod> methodsM = new HashSet<CMethod>(),
			methodsN = new HashSet<CMethod>();
	private HashSet<CMethod> mappedMethodsM = new HashSet<CMethod>(),
			mappedMethodsN = new HashSet<CMethod>();
	private HashSet<CField> fieldsM = new HashSet<CField>(),
			fieldsN = new HashSet<CField>();
	private HashSet<CField> mappedFieldsM = new HashSet<CField>(),
			mappedFieldsN = new HashSet<CField>();
	private HashSet<CInitializer> initsM = new HashSet<CInitializer>(),
			initsN = new HashSet<CInitializer>();
	private HashSet<CInitializer> mappedInitsM = new HashSet<CInitializer>(),
			mappedInitsN = new HashSet<CInitializer>();
	CRevision crevision;

	public RevisionAnalyzer(ChangeAnalyzer changeAnalyzer, long revision) {
		this.changeAnalyzer = changeAnalyzer;
		this.revision = revision;
	}

	public RevisionAnalyzer(ChangeAnalyzer changeAnalyzer, RevCommit commit) {
		this.changeAnalyzer = changeAnalyzer;
		this.gitCommit = commit;
		this.revision = commit.getCommitTime();
	}

	public ChangeAnalyzer getChangeAnalyzer() {
		return changeAnalyzer;
	}

	public long getRevision() {
		return revision;
	}

	public HashSet<CFile> getMappedFilesM() {
		return mappedFilesM;
	}

	public HashSet<CFile> getMappedFilesN() {
		return mappedFilesN;
	}

	public HashSet<CClass> getMappedClassesM() {
		return mappedClassesM;
	}

	public HashSet<CClass> getMappedClassesN() {
		return mappedClassesN;
	}

	public HashSet<CMethod> getMappedMethodsM() {
		return mappedMethodsM;
	}

	public HashSet<CMethod> getMappedMethodsN() {
		return mappedMethodsN;
	}

	public HashSet<CField> getMappedFieldsM() {
		return mappedFieldsM;
	}

	public HashSet<CField> getMappedFieldsN() {
		return mappedFieldsN;
	}

	public HashSet<CInitializer> getMappedInitsM() {
		return mappedInitsM;
	}

	public HashSet<CInitializer> getMappedInitsN() {
		return mappedInitsN;
	}

	public boolean analyze() {
		if (!buildModifiedFiles())
			return false;
		if (Config.countChangeFileOnly)
			return true;
		if (!map())
			return false;
		deriveChanges();
		/*
		 * try { printChanges(new PrintStream(new
		 * FileOutputStream("output/changes.txt"))); } catch
		 * (FileNotFoundException e) { e.printStackTrace(); }
		 */
		return true;
	}

	public boolean analyzeGit() {
		if (!buildGitModifiedFiles())
			return false;
		if (Config.countChangeFileOnly)
			return true;
		if (!map())
			return false;
		deriveChanges();
		if (this.mappedMethodsM.size() > 100)
			return false;
		return true;
	}

	private boolean buildModifiedFiles() {
		SVNLogEntry logEntry = this.changeAnalyzer.getLogEntry(revision);
		HashSet<String> changedPaths = new HashSet<String>(logEntry
				.getChangedPaths().keySet());
		HashMap<String, String> copiedPaths = new HashMap<>();
		HashMap<String, Long> copiedRevisions = new HashMap<>();
		for (String changedPath : changedPaths) {
			SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry
					.getChangedPaths().get(changedPath);
			String path = entryPath.getPath();
			if (entryPath.getCopyPath() != null) {
				copiedPaths.put(path, entryPath.getCopyPath());
				copiedRevisions.put(path, entryPath.getCopyRevision());
			}
		}
		HashSet<String> javaChangedPaths = new HashSet<>();
		for (String path : changedPaths) {
			if (path.endsWith(".java")) {
				SVNLogEntryPath entryPath = (SVNLogEntryPath) logEntry
						.getChangedPaths().get(path);
				if (entryPath.getType() == SVNLogEntryPath.TYPE_MODIFIED/*
																		 * &&
																		 * entryPath
																		 * .
																		 * getKind
																		 * () ==
																		 * SVNNodeKind
																		 * .FILE
																		 */) {
					String oldPath = path;
					long oldRevision = logEntry.getRevision() - 1;
					if (copiedPaths.containsKey(path)) {
						oldPath = copiedPaths.get(path);
					} else {
						String prefix = "";
						for (String copiedPath : copiedPaths.keySet()) {
							if (path.startsWith(copiedPath)) {
								if (copiedPath.length() > prefix.length()) {
									prefix = copiedPath;
								}
							}
						}
						if (!prefix.isEmpty()) {
							oldPath = copiedPaths.get(prefix)
									+ path.substring(prefix.length());
							oldRevision = copiedRevisions.get(prefix);
						}
					}
					javaChangedPaths.add(path);
					copiedPaths.put(path, oldPath);
					copiedRevisions.put(path, oldRevision);
				}
			}
		}
		if (javaChangedPaths.size() > 50)
			return false;
		if (!javaChangedPaths.isEmpty()) {
			this.crevision = new CRevision();
			this.crevision.id = this.revision;
			this.crevision.numOfFiles = javaChangedPaths.size();
			this.crevision.files = new ArrayList<CSourceFile>();
		}
		if (Config.countChangeFileOnly)
			return true;
		for (String changedPath : javaChangedPaths) {
			String contentM = getSourceCode(copiedPaths.get(changedPath),
					copiedRevisions.get(changedPath));
			if (contentM == null)
				continue;
			String contentN = getSourceCode(changedPath, revision);
			if (contentN == null)
				continue;
			CFile fileM = new CFile(this, copiedPaths.get(changedPath),
					contentM);
			CFile fileN = new CFile(this, changedPath, contentN);
			this.mappedFilesM.add(fileM);
//			this.crevision.files.add(new CSourceFile(changedPath, fileM
//					.getSourceFile().getLines().size()));
			this.mappedFilesN.add(fileN);
			fileM.setCType(Type.Modified);
			fileN.setCType(Type.Modified);
			fileM.setMappedFile(fileN);
			fileN.setMappedFile(fileM);
		}
		return true;
	}

	private boolean buildGitModifiedFiles() {
		if (gitCommit.getParentCount() == 1) {
			RevWalk rw = new RevWalk(this.changeAnalyzer.getGitConn()
					.getRepository());
			RevCommit parent = null;
			try {
				parent = rw.parseCommit(gitCommit.getParent(0).getId());
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
			if (parent == null) {
				rw.close();
				return false;
			}
			Repository repository = this.changeAnalyzer.getGitConn()
					.getRepository();
			DiffFormatter df = new DiffFormatter(NullOutputStream.INSTANCE);
			df.setRepository(repository);
			df.setDiffComparator(RawTextComparator.DEFAULT);
			df.setDetectRenames(true);
			df.setPathFilter(PathSuffixFilter.create(".java"));
			List<DiffEntry> diffs = null;
			try {
				diffs = df.scan(parent.getTree(), gitCommit.getTree());
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
			if (diffs == null) {
				rw.close();
				df.close();
				return false;
			}
			if (diffs.size() > 50) {
				rw.close();
				df.close();
				return false;
			}
			if (!diffs.isEmpty()) {
				this.crevision = new CRevision();
				this.crevision.id = this.revision;
				this.crevision.numOfFiles = diffs.size();
				this.crevision.files = new ArrayList<CSourceFile>();
			}
			if (Config.countChangeFileOnly) {
				rw.close();
				df.close();
				return true;
			}
			if (!diffs.isEmpty()) {
				this.changeAnalyzer.incrementNumOfCodeRevisions();
				for (DiffEntry diff : diffs) {
					if (diff.getChangeType() == ChangeType.MODIFY
							&& diff.getOldMode().getObjectType() == Constants.OBJ_BLOB
							&& diff.getNewMode().getObjectType() == Constants.OBJ_BLOB) {
						// System.out.println(diff.getChangeType() + ": " +
						// diff.getOldPath() + " --> " + diff.getNewPath());
						ObjectLoader ldr = null;
						String oldContent = null, newContent = null;
						try {
							ldr = repository.open(diff.getOldId().toObjectId(),
									Constants.OBJ_BLOB);
							oldContent = new String(ldr.getCachedBytes());
						} catch (IOException e) {
							System.err.println(e.getMessage());
							continue;
						}
						try {
							ldr = repository.open(diff.getNewId().toObjectId(),
									Constants.OBJ_BLOB);
							newContent = new String(ldr.getCachedBytes());
						} catch (IOException e) {
							System.err.println(e.getMessage());
							continue;
						}
						CFile fileM = new CFile(this, diff.getOldPath(),
								oldContent);
						CFile fileN = new CFile(this, diff.getNewPath(),
								newContent);
						this.mappedFilesM.add(fileM);
//						this.crevision.files.add(new CSourceFile(diff
//								.getNewPath(), fileM.getSourceFile().getLines()
//								.size()));
						this.mappedFilesN.add(fileN);
						fileM.setCType(Type.Modified);
						fileN.setCType(Type.Modified);
						fileM.setMappedFile(fileN);
						fileN.setMappedFile(fileM);
					}
				}
			}
			rw.close();
			df.close();
		}
		return true;
	}

	private boolean map() {
		mapClasses();
		mapMethods();
		// mapFields();
		// mapEnumConstants();
		return true;
	}

	private void mapClasses() {
		// diff classes in modified files
		for (CFile fileM : mappedFilesM) {
			CFile fileN = fileM.getMappedFile();
			fileM.computeSimilarity(fileN);
			for (CClass cc : fileM.getClasses()) {
				if (cc.getMappedClass() != null) {
					mappedClassesM.add(cc);
					mappedClassesN.add(cc.getMappedClass());
					Stack<CClass> stkClasses = new Stack<CClass>();
					stkClasses.push(cc);
					while (!stkClasses.isEmpty()) {
						CClass stkClass = stkClasses.pop();
						stkClass.computeSimilarity(stkClass.getMappedClass(),
								false);
						for (CClass icc : stkClass.getInnerClasses(false)) {
							if (icc.getMappedClass() != null) {
								mappedClassesM.add(icc);
								mappedClassesN.add(icc.getMappedClass());
								stkClasses.push(icc);
							}
						}
					}
				} else
					classesM.add(cc);
			}
			for (CClass cc : fileN.getClasses()) {
				if (cc.getMappedClass() != null)
					mappedClassesN.add(cc);
				else
					classesN.add(cc);
				for (CClass icc : cc.getInnerClasses(true)) {
					if (icc.getMappedClass() != null)
						mappedClassesN.add(icc);
					else
						classesN.add(icc);
				}
			}
		}

		// map any classes
		CClass.mapAll(classesM, classesN, mappedClassesM, mappedClassesN);

		// done diffing classes
		clearClassBodyMapping();
		for (CClass cc : new HashSet<CClass>(mappedClassesM)) {
			cc.computeSimilarity(cc.getMappedClass(), true);
			for (CMethod cm : cc.getMethods()) {
				if (cm.getMappedMethod() != null)
					mappedMethodsM.add(cm);
				else
					methodsM.add(cm);
			}
			for (CField cf : cc.getFields()) {
				if (cf.getMappedField() != null)
					mappedFieldsM.add(cf);
				else
					fieldsM.add(cf);
			}
			for (CInitializer ci : cc.getInitializers()) {
				if (ci.getMappedInitializer() != null)
					mappedInitsM.add(ci);
				else
					initsM.add(ci);
			}
		}
		for (CClass cc : new HashSet<CClass>(mappedClassesN)) {
			for (CMethod cm : cc.getMethods()) {
				if (cm.getMappedMethod() != null)
					mappedMethodsN.add(cm);
				else
					methodsN.add(cm);
			}
			for (CField cf : cc.getFields()) {
				if (cf.getMappedField() != null)
					mappedFieldsN.add(cf);
				else
					fieldsN.add(cf);
			}
			for (CInitializer ci : cc.getInitializers()) {
				if (ci.getMappedInitializer() != null)
					mappedInitsN.add(ci);
				else
					initsN.add(ci);
			}
		}
		for (CClass cc : classesM) {
			for (CMethod cm : cc.getMethods()) {
				methodsM.add(cm);
			}
			for (CField cf : cc.getFields()) {
				fieldsM.add(cf);
			}
			for (CInitializer ci : cc.getInitializers()) {
				initsM.add(ci);
			}
		}
		for (CClass cc : classesN) {
			for (CMethod cm : cc.getMethods()) {
				methodsN.add(cm);
			}
			for (CField cf : cc.getFields()) {
				fieldsN.add(cf);
			}
			for (CInitializer ci : cc.getInitializers()) {
				initsN.add(ci);
			}
		}
	}

	private void clearClassBodyMapping() {
		for (CClass cc : this.classesM) {
			cc.clearBodyMapping();
		}
		for (CClass cc : this.classesN) {
			cc.clearBodyMapping();
		}
		for (CClass cc : this.mappedClassesM) {
			cc.clearBodyMapping();
		}
		for (CClass cc : this.mappedClassesN) {
			cc.clearBodyMapping();
		}
	}

	private void mapMethods() {
		CMethod.mapAll(methodsM, methodsN, mappedMethodsM, mappedMethodsN,
				false);
	}

	private void deriveChanges() {
		// deriveFieldChanges();
		deriveMethodChanges();
		//deriveInitChanges();
		// deriveEnumConstantChanges();
		// deriveClassChanges();
	}

	private void deriveMethodChanges() {
		for (CMethod cmM : new HashSet<CMethod>(mappedMethodsM)) {
			cmM.deriveChanges();
			if (cmM.getCType() == Type.Unchanged) {
				mappedMethodsM.remove(cmM);
				mappedMethodsN.remove(cmM.getMappedMethod());
			}
		}
	}

	@SuppressWarnings("unused")
	private void deriveInitChanges() {
		for (CInitializer ciM : new HashSet<CInitializer>(mappedInitsM)) {
			ciM.deriveChanges();
			if (ciM.getCType() == Type.Unchanged) {
				mappedInitsM.remove(ciM);
				mappedInitsN.remove(ciM.getMappedInitializer());
			}
		}
	}

	@SuppressWarnings("unused")
	private void deriveClassChanges() {
		for (CClass cc : classesM) {
			cc.setCType(Type.Deleted);
		}
		for (CClass cc : classesN) {
			cc.setCType(Type.Added);
		}
		for (CClass ccM : new HashSet<CClass>(mappedClassesM)) {
			ccM.deriveChanges();
			if (ccM.getCType() == Type.Unchanged) {
				mappedClassesM.remove(ccM);
				mappedClassesN.remove(ccM.getMappedClass());
			}
		}
	}

	public void printChanges(PrintStream ps) {
		ps.println("Revision: " + this.revision);
		ps.println("Old system");
		printChanges(ps, mappedFilesM);
		ps.println("New system");
		printChanges(ps, mappedFilesN);
	}

	private void printChanges(PrintStream ps, HashSet<CFile> files) {
		for (CFile cf : files) {
			cf.printChanges(ps);
		}
	}

	private String getSourceCode(String changedPath, long revision) {
		return this.changeAnalyzer.getSourceCode(changedPath, revision);
	}
}
