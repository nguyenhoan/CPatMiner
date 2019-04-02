package pdg.graph;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import utils.JavaASTUtil;

public class PDGBuildingContext {
	public Repository repository;
	public RevCommit revCommit;
	
	private MethodDeclaration method;
	public String sourceFilePath;
	protected boolean interprocedural;
	private Stack<HashSet<PDGActionNode>> stkTrys = new Stack<>();
	private Stack<HashMap<String, String>> localVariables = new Stack<>(), localVariableTypes = new Stack<>();
	private HashMap<String, String> fieldTypes = new HashMap<>();
	
	public PDGBuildingContext(Repository repository, RevCommit commit, String sourceFilePath, boolean interprocedural) {
		this.repository = repository;
		this.revCommit = commit;
		this.sourceFilePath = sourceFilePath;
		this.interprocedural = interprocedural;
	}
	
	public PDGBuildingContext(PDGBuildingContext context) {
		this.interprocedural = context.interprocedural;
	}

	public void setMethod(MethodDeclaration method, boolean buildFieldType) {
		this.method = method;
		if (buildFieldType) {
			ASTNode p = this.method.getParent();
			if (p != null) {
				if (p instanceof TypeDeclaration)
					buildFieldTypes((TypeDeclaration) p);
				else if (p instanceof EnumDeclaration)
					buildFieldTypes((EnumDeclaration) p);
			}
		}
	}
	
	private void buildFieldTypes(EnumDeclaration ed) {
		for (int i = 0; i < ed.bodyDeclarations().size(); i++) {
			if (ed.bodyDeclarations().get(i) instanceof FieldDeclaration) {
				FieldDeclaration f = (FieldDeclaration) ed.bodyDeclarations().get(i);
				String type = JavaASTUtil.getSimpleType(f.getType());
				for (int j = 0; j < f.fragments().size(); j++) {
					VariableDeclarationFragment vdf = (VariableDeclarationFragment) f.fragments().get(j);
					this.fieldTypes.put(vdf.getName().getIdentifier(), type);
				}
			}
		}
		ASTNode p = ed.getParent();
		if (p != null) {
			if (p instanceof TypeDeclaration)
				buildFieldTypes((TypeDeclaration) p);
			else if (p instanceof EnumDeclaration)
				buildFieldTypes((EnumDeclaration) p);
		}
	}

	private void buildFieldTypes(TypeDeclaration td) {
		for (FieldDeclaration f : td.getFields()) {
			String type = JavaASTUtil.getSimpleType(f.getType());
			for (int i = 0; i < f.fragments().size(); i++) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) f.fragments().get(i);
				this.fieldTypes.put(vdf.getName().getIdentifier(), type);
			}
		}
		ASTNode p = td.getParent();
		if (p != null) {
			if (p instanceof TypeDeclaration)
				buildFieldTypes((TypeDeclaration) p);
			else if (p instanceof EnumDeclaration)
				buildFieldTypes((EnumDeclaration) p);
		}
	}

	public void addMethodTry(PDGActionNode node) {
		for (int i = 0; i < stkTrys.size(); i++)
			stkTrys.get(i).add(node);
	}

	public void pushTry() {
		stkTrys.push(new HashSet<PDGActionNode>());
	}
	
	public HashSet<PDGActionNode> popTry() {
		return stkTrys.pop();
	}

	public HashSet<PDGActionNode> getTrys(ITypeBinding catchExceptionType) {
		HashSet<PDGActionNode> trys = new HashSet<>();
		for (PDGActionNode node : stkTrys.peek())
			if (node.exceptionTypes != null) {
				for (ITypeBinding type : node.exceptionTypes)
					if (isSubType(type, catchExceptionType)) {
						trys.add(node);
						break;
					}
			}
		return trys;
	}

	private boolean isSubType(ITypeBinding type, ITypeBinding catchExceptionType) {
		if (type == null) 
			return false;
		if (type.equals(catchExceptionType))
			return true;
		return isSubType(type.getSuperclass(), catchExceptionType);
	}

	public void addMethodTrys(HashSet<PDGActionNode> nodes) {
		for (int i = 0; i < stkTrys.size(); i++)
			stkTrys.get(i).addAll(nodes);
	}

	public String getKey(ArrayAccess astNode) {
		String name = null;
		Expression a = astNode.getArray();
		if (a instanceof ArrayAccess)
			name = getKey((ArrayAccess) astNode.getArray());
		else if (a instanceof FieldAccess) {
			name = a.toString();
		} 
		else if (a instanceof QualifiedName)
			name = ((QualifiedName) a).getFullyQualifiedName();
		else if (a instanceof SimpleName) {
			name = ((SimpleName) a).getIdentifier();
			String[] info = getLocalVariableInfo(name);
			if (info != null)
				name = info[0];
		} else if (a instanceof SuperFieldAccess) {
			name = a.toString();
		}
		if (astNode.getIndex() instanceof NumberLiteral)
			name += "[" + ((NumberLiteral) (astNode.getIndex())).getToken()
					+ "]";
		else
			name += "[int]";
		return name;
	}

	public String[] getLocalVariableInfo(String identifier) {
		for (int i = localVariables.size() - 1; i >= 0; i--) {
			HashMap<String, String> variables = this.localVariables.get(i);
			if (variables.containsKey(identifier))
				return new String[]{variables.get(identifier), this.localVariableTypes.get(i).get(identifier)};
		}
		return null;
	}

	public void addScope() {
		this.localVariables.push(new HashMap<String, String>());
		this.localVariableTypes.push(new HashMap<String, String>());
	}

	public void removeScope() {
		this.localVariables.pop();
		this.localVariableTypes.pop();
	}

	public void addLocalVariable(String identifier, String key, String type) {
		this.localVariables.peek().put(identifier, key);
		this.localVariableTypes.peek().put(identifier, type);
	}

	public String getFieldType(String name) {
		String type = this.fieldTypes.get(name);
		if (type == null) {
//			buildSuperFieldTypes();
			type = this.fieldTypes.get(name);
		}
		return type;
	}

	private void buildSuperFieldTypes() {
		ASTNode p = this.method.getParent();
		if (p != null && p instanceof TypeDeclaration)
			buildSuperFieldTypes((TypeDeclaration) p);
	}

	private void buildSuperFieldTypes(TypeDeclaration td) {
		if (td.getSuperclassType() != null) {
			String stype = JavaASTUtil.getSimpleType(td.getSuperclassType());
			buildSuperFieldTypes(stype);
		}
		ASTNode p = td.getParent();
		if (p != null && p instanceof TypeDeclaration)
			buildSuperFieldTypes((TypeDeclaration) p);
	}

	@SuppressWarnings("unchecked")
	private void buildSuperFieldTypes(String stype) {
		String path = getSuperTypePath(stype);
		try (RevWalk revWalk = new RevWalk(repository)) {
            RevTree tree = revCommit.getTree();
            // now try to find a specific file
            try (TreeWalk treeWalk = new TreeWalk(repository)) {
                treeWalk.addTree(tree);
                treeWalk.setRecursive(true);
                treeWalk.setFilter(PathFilter.create(path));
                if (!treeWalk.next())
                    return;
                ObjectId objectId = treeWalk.getObjectId(0);
        		String content = null;
    			ObjectLoader ldr = repository.open(objectId, Constants.OBJ_BLOB);
    			content = new String(ldr.getCachedBytes());
    			@SuppressWarnings("rawtypes")
    			Map options = JavaCore.getOptions();
    			options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
    			options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_7);
    			options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
    			ASTParser parser = ASTParser.newParser(AST.JLS4);
    			parser.setSource(content.toCharArray());
    			parser.setCompilerOptions(options);
    			ASTNode ast = parser.createAST(null);
    			if (ast instanceof CompilationUnit) {
    				CompilationUnit cu = (CompilationUnit)ast;
    				for (int i = 0; i < cu.types().size(); i++) {
    					if (cu.types().get(i) instanceof TypeDeclaration) {
    						TypeDeclaration td = (TypeDeclaration) cu.types().get(i);
    						if (td.getName().getIdentifier().equals(stype)) {
    							for (FieldDeclaration f : td.getFields()) {
    								String type = JavaASTUtil.getSimpleType(f.getType());
    								for (int j = 0; j < f.fragments().size(); j++) {
    									VariableDeclarationFragment vdf = (VariableDeclarationFragment) f.fragments().get(j);
    									String name = vdf.getName().getIdentifier();
    									if (!this.fieldTypes.containsKey(name))
    										this.fieldTypes.put(name, type);
    								}
    							}
    						}
    					}
    				}
        		}
            } catch (IOException e) {
    			System.err.println(e.getMessage());
    			return;
			}

            revWalk.dispose();
        }
	}

	private String getSuperTypePath(String stype) {
		CompilationUnit cu = (CompilationUnit) this.method.getRoot();
		for (int i = 0; i < cu.imports().size(); i++) {
			ImportDeclaration id = (ImportDeclaration) cu.imports().get(i);
			if (!id.isOnDemand() && !id.isStatic()) {
				String qn = id.getName().getFullyQualifiedName();
				if (qn.endsWith("." + stype)) {
					String pkg = "";
					if (cu.getPackage() != null)
						pkg = cu.getPackage().getName().getFullyQualifiedName();
					String path = sourceFilePath.substring(0, sourceFilePath.lastIndexOf('/'));
					if (path.endsWith(pkg.replace('.', '/'))) {
						path = path.substring(0, path.length() - pkg.length());
						path += qn.replace('.', '/') + ".java";
						return path;
					}
				}
			}
		}
		String path = sourceFilePath.substring(0, sourceFilePath.lastIndexOf('/'));
		path += "/" + stype + ".java";
		return path;
	}
}
