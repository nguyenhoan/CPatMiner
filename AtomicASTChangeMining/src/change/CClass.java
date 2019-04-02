package change;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import utils.FileIO;
import utils.Pair;
import utils.PairDescendingOrder;

public class CClass extends ChangeEntity {
	private static final long serialVersionUID = -6548668930027896029L;
	public static final double thresholdSimilarity = 0.75;
	private CFile cFile;
	private int modifiers;
	private String annotation = "";
	private String simpleName;
	private AbstractTypeDeclaration declaration;
	// private HashSet<String> extendedClassNames = new HashSet<String>();
	private HashSet<String> superClassNames = new HashSet<String>();
	private CClass mappedClass = null;
	private CClass outterClass = null;
	private ArrayList<CClass> innerClasses = new ArrayList<CClass>();
	private ArrayList<CField> fields = new ArrayList<CField>();
	private ArrayList<CMethod> methods = new ArrayList<CMethod>();
	private ArrayList<CInitializer> initializers = new ArrayList<CInitializer>();

	public CClass(CFile cFile, TypeDeclaration type, CClass outterClass) {
		this.startLine = ((CompilationUnit) type.getRoot()).getLineNumber(type.getStartPosition());
		this.cFile = cFile;
		this.outterClass = outterClass;
		this.modifiers = type.getModifiers();
		this.simpleName = type.getName().getIdentifier();
		this.declaration = type;
		if (type.getSuperclassType() != null) {
			String name = FileIO.getSimpleClassName(type.getSuperclassType()
					.toString());
			// extendedClassNames.add(name);
			superClassNames.add(name);
		}
		if (type.superInterfaceTypes() != null) {
			for (int i = 0; i < type.superInterfaceTypes().size(); i++) {
				String name = FileIO.getSimpleClassName(type
						.superInterfaceTypes().get(i).toString());
				superClassNames.add(name);
				/*
				 * if (type.isInterface()) extendedClassNames.add(name);
				 */
			}
		}
		if (type.getFields() != null) {
			for (FieldDeclaration field : type.getFields()) {
				for (int i = 0; i < field.fragments().size(); i++) {
					VariableDeclarationFragment fragment = (VariableDeclarationFragment) field
							.fragments().get(i);
					String fieldType = FileIO.getSimpleClassName(field
							.getType().toString());
					fields.add(new CField(this, field, fieldType, fragment));
				}
			}
		}
		if (type.getMethods() != null) {
			for (MethodDeclaration method : type.getMethods()) {
				MethodDeclaration md = (MethodDeclaration) method;
				if (md.getBody() != null
						&& md.getBody().statements().size() <= CMethod.MAX_NUM_STATEMENTS) // TRICK
																							// limit
																							// size
																							// of
																							// method
					methods.add(new CMethod(this, md));
			}
		}
		if (type.getTypes() != null && type.getTypes().length > 0) {
			innerClasses = new ArrayList<CClass>();
			for (TypeDeclaration innerType : type.getTypes()) {
				innerClasses.add(new CClass(cFile, innerType, this));
			}
		}
		if (type.bodyDeclarations() != null) {
			int staticId = 0;
			for (int i = 0; i < type.bodyDeclarations().size(); i++) {
				ASTNode dec = (ASTNode) type.bodyDeclarations().get(i);
				int nodeType = dec.getNodeType();
				if (type.bodyDeclarations().get(i) instanceof Initializer) {
					initializers.add(new CInitializer(this, staticId,
							(Initializer) (type.bodyDeclarations().get(i))));
					staticId++;
				} else if (type.bodyDeclarations().get(i) instanceof EnumDeclaration) {
					
				} else if (nodeType != ASTNode.TYPE_DECLARATION
						&& nodeType != ASTNode.FIELD_DECLARATION
						&& nodeType != ASTNode.METHOD_DECLARATION) {
					// System.out.println("Info: not supported " +
					// type.bodyDeclarations().get(i).getClass().getSimpleName()
					// + " in type declaration!!!");
				}
			}
		}
	}

	public CClass(CFile cFile, AnnotationTypeDeclaration type,
			CClass outterClass) {
		this.startLine = ((CompilationUnit) type.getRoot()).getLineNumber(type.getStartPosition());
		this.cFile = cFile;
		this.outterClass = outterClass;
		this.modifiers = type.getModifiers();
		this.simpleName = type.getName().getIdentifier();
		this.declaration = type;
		for (int j = 0; j < type.bodyDeclarations().size(); j++) {
			ASTNode dec = (ASTNode) type.bodyDeclarations().get(j);
			int nodeType = dec.getNodeType();
			if (nodeType == ASTNode.FIELD_DECLARATION) {
				FieldDeclaration field = (FieldDeclaration) dec;
				for (int i = 0; i < field.fragments().size(); i++) {
					VariableDeclarationFragment fragment = (VariableDeclarationFragment) field
							.fragments().get(i);
					String fieldType = FileIO.getSimpleClassName(field
							.getType().toString());
					fields.add(new CField(this, field, fieldType, fragment));
				}
			} else if (nodeType == ASTNode.METHOD_DECLARATION) {
				MethodDeclaration md = (MethodDeclaration) dec;
				if (md.getBody() != null
						&& md.getBody().statements().size() <= CMethod.MAX_NUM_STATEMENTS) // TRICK
																							// limit
																							// size
																							// of
																							// method
					methods.add(new CMethod(this, md));
			} else if (nodeType == ASTNode.TYPE_DECLARATION) {
				innerClasses
						.add(new CClass(cFile, (TypeDeclaration) dec, this));
			} else if (nodeType == ASTNode.ENUM_DECLARATION) {
				
			} else {
				// System.out.println("Info: not supported " +
				// dec.getClass().getSimpleName() +
				// " in annotation type declaration!!!");
			}
		}
	}

	@Override
	public CFile getCFile() {
		return cFile;
	}

	@Override
	public CClass getCClass() {
		return this;
	}

	public int getModifiers() {
		return modifiers;
	}

	public String getAnnotation() {
		return annotation;
	}

	public String getSimpleName() {
		return simpleName;
	}

	@Override
	public String getName() {
		return this.simpleName;
	}

	@Override
	public String getQualName() {
		return this.cFile.getPath() + "." + this.simpleName;
	}

	public String getFullQualName() {
		return this.cFile.getPath() + "." + this.simpleName;
	}

	public AbstractTypeDeclaration getDeclaration() {
		return declaration;
	}

	/*
	 * public HashSet<String> getExtendedClassNames() { return
	 * extendedClassNames; }
	 */

	public HashSet<String> getSuperClassNames() {
		return superClassNames;
	}

	public CClass getOutterClass() {
		return outterClass;
	}

	public CClass getMappedClass() {
		return mappedClass;
	}

	private void setMappedClass(CClass mappedClass) {
		this.mappedClass = mappedClass;
	}

	public ArrayList<CClass> getInnerClasses(boolean recursive) {
		ArrayList<CClass> classes = new ArrayList<CClass>(this.innerClasses);
		if (recursive) {
			for (CClass icc : innerClasses)
				classes.addAll(icc.getInnerClasses(true));
		}
		return classes;
	}

	public ArrayList<CField> getFields() {
		return fields;
	}

	public ArrayList<CMethod> getMethods() {
		return methods;
	}

	public ArrayList<CInitializer> getInitializers() {
		return initializers;
	}

	public HashMap<String, String> getFieldTypes() {
		HashMap<String, String> fieldTypes = getFieldTypes(this.fields);
		if (this.outterClass != null)
			fieldTypes.putAll(this.outterClass.getFieldTypes());
		return fieldTypes;
	}

	private HashMap<String, String> getFieldTypes(ArrayList<CField> fields) {
		HashMap<String, String> fieldTypes = new HashMap<String, String>();
		for (CField cf : fields)
			fieldTypes.put(cf.getName(), cf.getType());
		return fieldTypes;
	}

	public double computeSimilarity(CClass otherClass, boolean inMapped) {
		double commonSize = 0, totalSize = 0;
		HashSet<CMethod> methodsM = new HashSet<CMethod>(this.methods), methodsN = new HashSet<CMethod>(
				otherClass.getMethods());
		HashSet<CMethod> mappedMethodsM = new HashSet<CMethod>(), mappedMethodsN = new HashSet<CMethod>();
		HashSet<CField> fieldsM = new HashSet<CField>(this.fields), fieldsN = new HashSet<CField>(
				otherClass.getFields());
		HashSet<CField> mappedFieldsM = new HashSet<CField>(), mappedFieldsN = new HashSet<CField>();
		HashSet<CInitializer> initsM = new HashSet<CInitializer>(
				this.initializers), initsN = new HashSet<CInitializer>(
				otherClass.getInitializers());
		HashSet<CInitializer> mappedInitsM = new HashSet<CInitializer>(), mappedInitsN = new HashSet<CInitializer>();

		double[] size = CMethod.mapAll(methodsM, methodsN, mappedMethodsM,
				mappedMethodsN, inMapped);
		commonSize += size[0];
		totalSize += size[1];

		size = CField.mapAll(fieldsM, fieldsN, mappedFieldsM, mappedFieldsN);
		commonSize += size[0];
		totalSize += size[1];

		size = CInitializer.mapAll(initsM, initsN, mappedInitsM, mappedInitsN);
		commonSize += size[0];
		totalSize += size[1];

		// map inner classes
		HashSet<CClass> mappedInnersM = new HashSet<CClass>(), mappedInnersN = new HashSet<CClass>();
		mapAll(new HashSet<CClass>(this.innerClasses), new HashSet<CClass>(
				otherClass.getInnerClasses(false)), mappedInnersM,
				mappedInnersN);
		commonSize += mappedInnersM.size();
		totalSize += mappedInnersM.size();

		return commonSize / totalSize;
	}

	public static void setMap(CClass classM, CClass classN) {
		/*
		 * if (classM.getMappedClass() != null)
		 * classM.getMappedClass().setMappedClass(null); if
		 * (classN.getMappedClass() != null)
		 * classN.getMappedClass().setMappedClass(null);
		 */
		classM.setMappedClass(classN);
		classN.setMappedClass(classM);
	}

	public static void map(HashSet<CClass> classesM, HashSet<CClass> classesN,
			HashSet<CClass> mappedClassesM, HashSet<CClass> mappedClassesN) {
		HashMap<CClass, HashSet<Pair>> pairsOfMethods1 = new HashMap<CClass, HashSet<Pair>>();
		HashMap<CClass, HashSet<Pair>> pairsOfMethods2 = new HashMap<CClass, HashSet<Pair>>();
		ArrayList<Pair> pairs = new ArrayList<Pair>();
		PairDescendingOrder comparator = new PairDescendingOrder();
		for (CClass ccM : classesM) {
			HashSet<Pair> pairs1 = new HashSet<Pair>();
			for (CClass ccN : classesN) {
				double sim = ccM.computeSimilarity(ccN, false);
				if (sim >= thresholdSimilarity) {
					Pair pair = new Pair(ccM, ccN, sim);
					pairs1.add(pair);
					HashSet<Pair> pairs2 = pairsOfMethods2.get(ccN);
					if (pairs2 == null)
						pairs2 = new HashSet<Pair>();
					pairs2.add(pair);
					pairsOfMethods2.put(ccN, pairs2);
					int index = Collections.binarySearch(pairs, pair,
							comparator);
					if (index < 0)
						pairs.add(-1 - index, pair);
					else
						pairs.add(index, pair);
				}
			}
			pairsOfMethods1.put(ccM, pairs1);
		}
		while (!pairs.isEmpty()) {
			Pair pair = pairs.get(0);
			CClass ccM = (CClass) pair.getObj1(), ccN = (CClass) pair.getObj2();
			setMap(ccM, ccN);
			/*
			 * l1.remove(em1); l2.remove(em2);
			 */
			mappedClassesM.add(ccM);
			mappedClassesN.add(ccN);
			for (Pair p : pairsOfMethods1.get(pair.getObj1()))
				pairs.remove(p);
			for (Pair p : pairsOfMethods2.get(pair.getObj2()))
				pairs.remove(p);
		}
	}

	public static void mapAll(HashSet<CClass> classesM,
			HashSet<CClass> classesN, HashSet<CClass> mappedClassesM,
			HashSet<CClass> mappedClassesN) {

		// map classes with names
		HashMap<String, CClass> classWithNameM = new HashMap<String, CClass>(), classWithNameN = new HashMap<String, CClass>();
		for (CClass cc : classesM) {
			classWithNameM.put(cc.getSimpleName(), cc);
		}
		for (CClass cc : classesN) {
			classWithNameN.put(cc.getSimpleName(), cc);
		}
		HashSet<String> interNames = new HashSet<String>(
				classWithNameM.keySet());
		interNames.retainAll(classWithNameN.keySet());
		for (String name : interNames) {
			CClass ccM = classWithNameM.get(name), ccN = classWithNameN
					.get(name);
			setMap(ccM, ccN);
			mappedClassesM.add(ccM);
			mappedClassesN.add(ccN);
			classesM.remove(ccM);
			classesN.remove(ccN);
		}

		// map other classes
		if (!classesM.isEmpty() && !classesN.isEmpty()) {
			HashSet<CClass> tmpMappedClassesM = new HashSet<CClass>(), tmpMappedClassesN = new HashSet<CClass>();
			map(classesM, classesN, tmpMappedClassesM, tmpMappedClassesN);
			mappedClassesM.addAll(tmpMappedClassesM);
			mappedClassesN.addAll(tmpMappedClassesN);
			classesM.removeAll(tmpMappedClassesM);
			classesN.removeAll(tmpMappedClassesN);
		}
	}

	public void deriveChanges() {
		if (mappedClass == null)
			return;
		CClass ccN = mappedClass;
		boolean same = this.simpleName.equals(ccN.getSimpleName())
				&& this.modifiers == ccN.getModifiers()
				&& this.annotation.equals(ccN.getAnnotation())
				&& this.superClassNames.equals(ccN.getSuperClassNames()) /*
																		 * &&
																		 * this.
																		 * outterClass
																		 * .
																		 * getMappedClass
																		 * () ==
																		 * ccN.
																		 * getOutterClass
																		 * ()
																		 */;
		if (same) {
			for (CField cf : fields) {
				if (cf.getCType() != Type.Unchanged) {
					same = false;
					break;
				}
			}
		}
		if (same) {
			for (CField cf : ccN.getFields()) {
				if (cf.getCType() != Type.Unchanged) {
					same = false;
					break;
				}
			}
		}
		if (same) {
			for (CMethod cm : methods) {
				if (cm.getCType() != Type.Unchanged) {
					same = false;
					break;
				}
			}
		}
		if (same) {
			for (CMethod cm : ccN.getMethods()) {
				if (cm.getCType() != Type.Unchanged) {
					same = false;
					break;
				}
			}
		}
		if (same) {
			for (CInitializer ci : initializers) {
				if (ci.getCType() != Type.Unchanged) {
					same = false;
					break;
				}
			}
		}
		if (same) {
			for (CInitializer ci : ccN.getInitializers()) {
				if (ci.getCType() != Type.Unchanged) {
					same = false;
					break;
				}
			}
		}
		if (same) {
			for (CClass cc : innerClasses) {
				cc.deriveChanges();
				if (cc.getCType() != Type.Unchanged) {
					same = false;
					break;
				}
			}
		}
		if (same) {
			for (CClass cc : ccN.getInnerClasses(false)) {
				cc.deriveChanges();
				if (cc.getCType() != Type.Unchanged) {
					same = false;
					break;
				}
			}
		}
		if (!same) {
			this.setCType(Type.Modified);
			ccN.setCType(Type.Modified);
		}
	}

	public void printChanges(PrintStream ps) {
		if (getCType() != Type.Unchanged) {
			ps.println("\t\tClass: "
					+ getSimpleName()
					+ " --> "
					+ (this.mappedClass == null ? "null" : this.mappedClass
							.getSimpleName()));
			printChanges(ps, this.fields);
			printChanges(ps, this.methods);
			printChanges(ps, this.innerClasses);
		}
	}

	private void printChanges(PrintStream ps, List<?> list) {
		for (int i = 0; i < list.size(); i++) {
			Object obj = list.get(i);
			if (obj instanceof CField)
				((CField) obj).printChanges(ps);
			else if (obj instanceof CMethod)
				((CMethod) obj).printChanges(ps);
			else if (obj instanceof CClass)
				((CClass) obj).printChanges(ps);
		}
	}

	public boolean hasChangedName() {
		if (getCType() == Type.Unchanged)
			return false;
		if (getCType() == Type.Added || getCType() == Type.Deleted)
			return true;
		if (getCType() == Type.Modified) {
			if (this.mappedClass == null)
				return true;
			return !this.simpleName.equals(this.mappedClass.getSimpleName());
		}
		System.err.println("INFO: not-yet-used changed types");
		return false;
	}

	@Override
	public String toString() {
		return this.getName();
	}

	public void clearBodyMapping() {
		for (CMethod ce : this.methods) {
			ce.setMappedMethod(null);
		}
		for (CField ce : this.fields) {
			ce.setMappedField(null);
		}
		for (CInitializer ce : this.initializers) {
			ce.setMappedInitializer(null);
		}
	}
}
