package change;

import java.io.PrintStream;
import java.util.HashSet;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import utils.FileIO;
import utils.JavaASTUtil;

public class CFile extends ChangeEntity {
	private static final long serialVersionUID = 4073095618526557436L;
	public static final int MAX_SIZE = 500000;
	private RevisionAnalyzer cRevisionAnalyzer;
	private String path;
	private String simpleName;
	private CFile mappedFile;
	private CompilationUnit compileUnit;
	private HashSet<CClass> classes = new HashSet<CClass>();

	public CFile(RevisionAnalyzer revisionAnalyzer, String filePath,
			String content) {
		this.startLine = 0;
		this.cRevisionAnalyzer = revisionAnalyzer;
		this.path = filePath;
		this.simpleName = FileIO.getSimpleFileName(path);
		try {
			compileUnit = (CompilationUnit) JavaASTUtil.parseSource(content);
		} catch (Exception e) {} // FIXME somehow throw org/eclipse/text/edits/MalformedTreeException
		if (compileUnit == null || compileUnit.types() == null || compileUnit.types().isEmpty()) {
			// System.out.println("\t\tDiscarded " + filePath);
		} else {
			VectorVisitor vectorVisitor = new VectorVisitor();
			compileUnit.accept(vectorVisitor);
			for (int index = 0; index < compileUnit.types().size(); index++) {
				AbstractTypeDeclaration declaration = (AbstractTypeDeclaration) compileUnit
						.types().get(index);
				switch (declaration.getNodeType()) {
				case ASTNode.TYPE_DECLARATION: {
					TypeDeclaration type = (TypeDeclaration) declaration;
					this.classes.add(new CClass(this, type, null));
					break;
				}
				case ASTNode.ENUM_DECLARATION: {
					break;
				}
				case ASTNode.ANNOTATION_TYPE_DECLARATION: {
					AnnotationTypeDeclaration type = (AnnotationTypeDeclaration) declaration;
					this.classes.add(new CClass(this, type, null));
					break;
				}
				default: {
					// System.out.println("Info: Some other type declaration not implemented. "
					// + declaration.getClass().getSimpleName());
					break;
				}
				}
			}
		}
	}

	public RevisionAnalyzer getcRevisionAnalyzer() {
		return cRevisionAnalyzer;
	}

	public String getPath() {
		return path;
	}

	public String getSimpleName() {
		return simpleName;
	}

	@Override
	public String getName() {
		return this.simpleName;
	}

	public HashSet<CClass> getClasses() {
		return classes;
	}

	public CFile getMappedFile() {
		return mappedFile;
	}

	public void setMappedFile(CFile mappedFile) {
		this.mappedFile = mappedFile;
	}

	@Override
	public CFile getCFile() {
		return this;
	}

	@Override
	public CClass getCClass() {
		return null;
	}

	public void computeSimilarity(CFile otherFile) {
		HashSet<CClass> classesM = new HashSet<CClass>(this.classes), classesN = new HashSet<CClass>(
				otherFile.getClasses());
		HashSet<CClass> mappedClassesM = new HashSet<CClass>(), mappedClassesN = new HashSet<CClass>();

		// map class having same name with file
		CClass cM = null, cN = null;
		for (CClass cc : classesM) {
			if (cc.getSimpleName().equals(simpleName))
				cM = cc;
		}
		for (CClass cc : classesN) {
			if (cc.getSimpleName().equals(otherFile.getSimpleName()))
				cN = cc;
		}
		if (cM != null && cN != null) {
			double sim = cM.computeSimilarity(cN, false);
			if (sim >= CClass.thresholdSimilarity) {
				CClass.setMap(cM, cN);
				mappedClassesM.add(cM);
				mappedClassesN.add(cN);
				classesM.remove(cM);
				classesN.remove(cN);
			}
		}

		CClass.mapAll(classesM, classesN, mappedClassesM, mappedClassesN);
	}

	public void printChanges(PrintStream ps) {
		if (getCType() != Type.Unchanged) {
			ps.println("\tFile: "
					+ getPath()
					+ " --> "
					+ (this.mappedFile == null ? "null" : this.mappedFile
							.getPath()));
			printChanges(ps, this.classes);
		}
	}

	private void printChanges(PrintStream ps, HashSet<CClass> classes) {
		for (CClass cc : classes)
			cc.printChanges(ps);
	}

	@Override
	public String toString() {
		return this.simpleName;
	}

	@Override
	public String getQualName() {
		return this.path;
	}
}
