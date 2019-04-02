package change;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTMatcher;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import pdg.graph.PDGBuildingContext;
import pdg.graph.PDGGraph;
import treed.TreedMapper;
import utils.FileIO;
import utils.JavaASTUtil;
import utils.Pair;
import utils.PairDescendingOrder;
import utils.StringProcessor;

public class CMethod extends ChangeEntity {
	private static final long serialVersionUID = 2920972217396599104L;
	private static final String separatorParameter = "#";
	private static final double thresholdSignatureSimilarity = 0.625;
	private static final double thresholdBodySimilarity = 0.75;
	@SuppressWarnings("unused")
	private static final double thresholdDiffability = 0.5;
	public static final int MAX_NUM_STATEMENTS = 1000;

	private CClass cClass;
	private int modifiers;
	private String annotation = "";
	private String name, simpleName;
	private int numOfParameters;
	private String returnType;
	private MethodDeclaration declaration;
	private CMethod mappedMethod = null;
	private String parameterTypes;
	private HashSet<String> types, fields;
	private HashSet<String> literals = new HashSet<String>();
	private HashMap<SimpleName, HashSet<SimpleName>> localVarLocs;

	@SuppressWarnings("unchecked")
	public CMethod(CClass cClass, MethodDeclaration method) {
		this.startLine = ((CompilationUnit) method.getRoot()).getLineNumber(method.getBody().getStartPosition());
		this.cClass = cClass;
		this.modifiers = method.getModifiers();
		for (int i = 0; i < method.modifiers().size(); i++) {
			ASTNode modifier = (ASTNode) method.modifiers().get(i);
			if (modifier.getNodeType() == ASTNode.MARKER_ANNOTATION) {
				this.annotation = ((MarkerAnnotation) modifier).getTypeName()
						.toString();
				if (!this.annotation.toLowerCase().contains("null"))
					this.annotation = "";
				break;
			}
		}
		this.simpleName = method.getName().getIdentifier();
		this.numOfParameters = method.parameters().size();
		this.name = simpleName + "(" + numOfParameters + ")";
		this.declaration = method;
		this.parameterTypes = separatorParameter;
		for (int i = 0; i < method.parameters().size(); i++) {
			SingleVariableDeclaration dec = (SingleVariableDeclaration) method
					.parameters().get(i);
			/*
			 * String paraType =
			 * FileIO.getSimpleClassName(dec.getType().toString()); String temp
			 * = dec.toString(); int l = temp.length();
			 * while(temp.endsWith("[]")) { paraType += "[]"; temp =
			 * temp.substring(0, l-2); l -= 2; }
			 */
			String paraType = JavaASTUtil.getSimpleType(dec.getType());
			this.parameterTypes += paraType + separatorParameter;
		}
		String returnType;
		if (method.getReturnType2() != null) {
			returnType = method.getReturnType2().toString();
			returnType = FileIO.getSimpleClassName(returnType);
		} else {
			returnType = "void";
		}
		this.returnType = returnType;
		this.vector = new HashMap<Integer, Integer>(
				(HashMap<Integer, Integer>) method
						.getProperty(VectorVisitor.propertyVector));
		method.setProperty(VectorVisitor.propertyVector, null);
		computeVectorLength();
	}

	public int getModifiers() {
		return modifiers;
	}

	public String getAnnotation() {
		return annotation;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getSimpleName() {
		return simpleName;
	}

	@Override
	public String getQualName() {
		return this.cClass.getSimpleName() + "." + this.name;
	}

	public int getNumOfParameters() {
		return numOfParameters;
	}

	public String getParameterTypes() {
		return this.parameterTypes;
	}

	public String getReturnType() {
		return this.returnType;
	}

	public String getFullName() {
		return simpleName + parameterTypes;
	}

	public String getFullQualName() {
		return this.cClass.getFullQualName() + "." + getFullName();
	}

	public MethodDeclaration getDeclaration() {
		return declaration;
	}

	public CMethod getMappedMethod() {
		return mappedMethod;
	}

	public void setMappedMethod(CMethod mappedMethod) {
		this.mappedMethod = mappedMethod;
	}

	public HashSet<String> getTypes() {
		return types;
	}

	public HashSet<String> getFields() {
		return fields;
	}

	public HashSet<String> getLiterals() {
		return literals;
	}

	@Override
	public CFile getCFile() {
		return this.cClass.getCFile();
	}

	@Override
	public CClass getCClass() {
		return this.cClass;
	}

	public HashMap<SimpleName, HashSet<SimpleName>> getLocalVarLocs() {
		return localVarLocs;
	}

	public static void setMap(CMethod methodM, CMethod methodN) {
		/*
		 * if (methodM.getMappedMethod() != null)
		 * methodM.getMappedMethod().setMappedMethod(null); if
		 * (methodN.getMappedMethod() != null)
		 * methodN.getMappedMethod().setMappedMethod(null);
		 */
		methodM.setMappedMethod(methodN);
		methodN.setMappedMethod(methodM);
	}

	public static void map(HashSet<CMethod> methodsM,
			HashSet<CMethod> methodsN, HashSet<CMethod> mappedMethodsM,
			HashSet<CMethod> mappedMethodsN, boolean inMappedClasses) {
		HashMap<CMethod, HashSet<Pair>> pairsOfMethods1 = new HashMap<CMethod, HashSet<Pair>>();
		HashMap<CMethod, HashSet<Pair>> pairsOfMethods2 = new HashMap<CMethod, HashSet<Pair>>();
		ArrayList<Pair> pairs = new ArrayList<Pair>();
		PairDescendingOrder comparator = new PairDescendingOrder();
		for (CMethod cmM : methodsM) {
			HashSet<Pair> pairs1 = new HashSet<Pair>();
			for (CMethod cmN : methodsN) {
				double[] sim = cmM.computeSimilarity(cmN, inMappedClasses);
				// TODO loose similarity
				boolean isMapped = (inMappedClasses && sim[0] >= thresholdSignatureSimilarity)
						|| (sim[0] > 0 && sim[1] == 1.0)
						|| (sim[0] >= thresholdSignatureSimilarity && sim[1] >= thresholdBodySimilarity);
				if (isMapped) {
					Pair pair = new Pair(cmM, cmN, sim[3]);
					pairs1.add(pair);
					HashSet<Pair> pairs2 = pairsOfMethods2.get(cmN);
					if (pairs2 == null)
						pairs2 = new HashSet<Pair>();
					pairs2.add(pair);
					pairsOfMethods2.put(cmN, pairs2);
					int index = Collections.binarySearch(pairs, pair,
							comparator);
					if (index < 0)
						pairs.add(-1 - index, pair);
					else
						pairs.add(index, pair);
				}
			}
			pairsOfMethods1.put(cmM, pairs1);
		}
		while (!pairs.isEmpty()) {
			Pair pair = pairs.get(0);
			CMethod cmM = (CMethod) pair.getObj1(), cmN = (CMethod) pair
					.getObj2();
			setMap(cmM, cmN);
			/*
			 * l1.remove(em1); l2.remove(em2);
			 */
			mappedMethodsM.add(cmM);
			mappedMethodsN.add(cmN);
			for (Pair p : pairsOfMethods1.get(pair.getObj1()))
				pairs.remove(p);
			for (Pair p : pairsOfMethods2.get(pair.getObj2()))
				pairs.remove(p);
		}
	}

	@Override
	public void computeVectorLength() {
		this.vectorLength = 0;
		for (int key : vector.keySet())
			this.vectorLength += vector.get(key);
	}

	private double[] computeSimilarity(CMethod other, boolean inMappedClasses) {
		double[] sim = new double[4];
		double signature = computeNameSimilarity(other, inMappedClasses);
		double body = 0;
		if (this.vector.size() > 0 || other.getVector().size() > 0)
			body = computeVectorSimilarity(other);
		else
			body = 1.0;
		sim[0] = signature;
		sim[1] = body;
		sim[2] = signature + body;
		sim[3] = Math.round(signature * 10) + signature + body;

		return sim;
	}

	private double computeNameSimilarity(CMethod other, boolean inMappedClasses) {
		double signature = 0;
		String fullName1 = getFullName(), fullName2 = other.getFullName();
		if (inMappedClasses
				&& !this.cClass.getSimpleName().equals(
						other.cClass.getSimpleName())
				&& this.simpleName.equals(this.cClass.getSimpleName())
				&& other.simpleName.equals(other.cClass.getSimpleName())) {
			fullName1 = "class" + this.parameterTypes;
			fullName2 = "class" + other.parameterTypes;
		}
		if ((this.simpleName.startsWith("set") && other.getSimpleName()
				.startsWith("set"))
				|| (this.simpleName.startsWith("get") && other.getSimpleName()
						.startsWith("get"))) {
			fullName1 = fullName1.substring(3);
			fullName2 = fullName2.substring(3);
		}
		if (this.simpleName.equals(other.getSimpleName())) {
			if (fullName1.equals(fullName2))
				signature = 1.0;
			else {
				ArrayList<String> seqParameterTypes1 = new ArrayList<String>(), seqParameterTypes2 = new ArrayList<String>();
				if (this.parameterTypes.length() > 1) {
					String[] paraTypes1 = this.parameterTypes.substring(1)
							.split("\\" + separatorParameter);
					for (String para : paraTypes1)
						seqParameterTypes1.add(para);
				}
				if (other.getParameterTypes().length() > 1) {
					String[] paraTypes2 = other.getParameterTypes()
							.substring(1).split("\\" + separatorParameter);
					for (String para : paraTypes2)
						seqParameterTypes2.add(para);
				}
				ArrayList<Integer> lcsM = new ArrayList<Integer>(), lcsN = new ArrayList<Integer>();
				StringProcessor.doLCS(seqParameterTypes1, seqParameterTypes2,
						0, 0, lcsM, lcsN);
				signature = ((seqParameterTypes1.size() + seqParameterTypes2
						.size())
						/ (Math.max(seqParameterTypes1.size(),
								seqParameterTypes2.size()) * 2.0)
						+ lcsM.size()
						* 2.0
						/ (seqParameterTypes1.size() + seqParameterTypes2
								.size()) + 2.0) / 4.0; // *
														// Util.thresholdSwitchMethodSimilarity;
			}
		} else {
			ArrayList<String> seq1 = StringProcessor.serialize(this.returnType), seq2 = StringProcessor
					.serialize(other.getReturnType());
			ArrayList<Integer> lcsM = new ArrayList<Integer>(), lcsN = new ArrayList<Integer>();
			StringProcessor.doLCS(seq1, seq2, 0, 0, lcsM, lcsN);
			double simReturnType = lcsM.size() * 2.0
					/ (seq1.size() + seq2.size());
			seq1 = StringProcessor.serialize(fullName1);
			seq2 = StringProcessor.serialize(fullName2);
			lcsM = new ArrayList<Integer>();
			lcsN = new ArrayList<Integer>();
			StringProcessor.doLCS(seq1, seq2, 0, 0, lcsM, lcsN);
			double simFullName = lcsM.size() * 2.0
					/ (seq1.size() + seq2.size());
			signature = (simReturnType + 2.0 * simFullName) / 3.0;
		}
		return signature;
	}

	public static double[] mapAll(HashSet<CMethod> methodsM,
			HashSet<CMethod> methodsN, HashSet<CMethod> mappedMethodsM,
			HashSet<CMethod> mappedMethodsN, boolean inMappedClasses) {
		double[] size = {
				0,
				(methodsM.size() + methodsN.size() + mappedMethodsM.size() + mappedMethodsN
						.size()) / 2.0 };
		// map methods with same simple names and numbers of parameters
		HashMap<String, HashSet<CMethod>> methodsWithNameM = new HashMap<String, HashSet<CMethod>>();
		HashMap<String, HashSet<CMethod>> methodsWithNameN = new HashMap<String, HashSet<CMethod>>();
		for (CMethod cm : methodsM) {
			String name = cm.getName();
			HashSet<CMethod> cms = methodsWithNameM.get(name);
			if (cms == null)
				cms = new HashSet<CMethod>();
			cms.add(cm);
			methodsWithNameM.put(name, cms);
		}
		for (CMethod cm : methodsN) {
			String name = cm.getName();
			HashSet<CMethod> cms = methodsWithNameN.get(name);
			if (cms == null)
				cms = new HashSet<CMethod>();
			cms.add(cm);
			methodsWithNameN.put(name, cms);
		}
		HashSet<String> interNames = new HashSet<String>(
				methodsWithNameM.keySet());
		interNames.retainAll(methodsWithNameN.keySet());
		for (String name : interNames) {
			HashSet<CMethod> tmpMappedMethodsM = new HashSet<CMethod>();
			HashSet<CMethod> tmpMappedMethodsN = new HashSet<CMethod>();
			map(methodsWithNameM.get(name), methodsWithNameN.get(name),
					tmpMappedMethodsM, tmpMappedMethodsN, inMappedClasses);
			mappedMethodsM.addAll(tmpMappedMethodsM);
			mappedMethodsN.addAll(tmpMappedMethodsN);
			methodsM.removeAll(tmpMappedMethodsM);
			methodsN.removeAll(tmpMappedMethodsN);
			size[0] += tmpMappedMethodsM.size();
		}

		// map methods with same simple names
		methodsWithNameM = new HashMap<String, HashSet<CMethod>>();
		methodsWithNameN = new HashMap<String, HashSet<CMethod>>();
		for (CMethod cm : methodsM) {
			String name = cm.getSimpleName();
			if (name.equals(cm.getCClass().getSimpleName()))
				name = "class";
			HashSet<CMethod> cms = methodsWithNameM.get(name);
			if (cms == null)
				cms = new HashSet<CMethod>();
			cms.add(cm);
			methodsWithNameM.put(name, cms);
		}
		for (CMethod cm : methodsN) {
			String name = cm.getSimpleName();
			if (name.equals(cm.getCClass().getSimpleName()))
				name = "class";
			HashSet<CMethod> cms = methodsWithNameN.get(name);
			if (cms == null)
				cms = new HashSet<CMethod>();
			cms.add(cm);
			methodsWithNameN.put(name, cms);
		}
		interNames = new HashSet<String>(methodsWithNameM.keySet());
		interNames.retainAll(methodsWithNameN.keySet());
		for (String name : interNames) {
			HashSet<CMethod> tmpMappedMethodsM = new HashSet<CMethod>();
			HashSet<CMethod> tmpMappedMethodsN = new HashSet<CMethod>();
			map(methodsWithNameM.get(name), methodsWithNameN.get(name),
					tmpMappedMethodsM, tmpMappedMethodsN, inMappedClasses);
			mappedMethodsM.addAll(tmpMappedMethodsM);
			mappedMethodsN.addAll(tmpMappedMethodsN);
			methodsM.removeAll(tmpMappedMethodsM);
			methodsN.removeAll(tmpMappedMethodsN);
			size[0] += tmpMappedMethodsM.size();
		}

		// map other methods
		HashSet<CMethod> tmpMappedMethodsM = new HashSet<CMethod>();
		HashSet<CMethod> tmpMappedMethodsN = new HashSet<CMethod>();
		map(methodsM, methodsN, tmpMappedMethodsM, tmpMappedMethodsN,
				inMappedClasses);
		mappedMethodsM.addAll(tmpMappedMethodsM);
		mappedMethodsN.addAll(tmpMappedMethodsN);
		methodsM.removeAll(tmpMappedMethodsM);
		methodsN.removeAll(tmpMappedMethodsN);
		size[0] += tmpMappedMethodsM.size();

		return size;
	}

	public void deriveChanges() {
		CMethod cmN = this.mappedMethod;
		boolean matched = this.declaration.subtreeMatch(new ASTMatcher(false) {
			@Override
			public boolean match(SimpleName node, Object other) {
				if (!(other instanceof SimpleName))
					return false;
				return true;
			}
			
			@Override
			public boolean match(AnonymousClassDeclaration node, Object other) {
				if (!(other instanceof AnonymousClassDeclaration))
					return false;
				return true;
			}
		}, cmN.declaration);
		if (matched)
			return;
		TreedMapper tm = new TreedMapper(declaration, cmN.declaration);
		tm.map(false);
		if (tm.hasNonNameUnmap()) {
			this.setCType(Type.Modified);
			cmN.setCType(Type.Modified);
		}
	}

	@Override
	public void cleanForStats() {
		super.cleanForStats();
		this.annotation = null;
		this.cClass = null;
		this.declaration = null;
		if (this.fields != null) {
			this.fields.clear();
			this.fields = null;
		}
		this.literals.clear();
		this.literals = null;
		if (this.localVarLocs != null) {
			this.localVarLocs.clear();
			this.localVarLocs = null;
		}
		this.mappedMethod = null;
		if (this.types != null) {
			this.types.clear();
			this.types = null;
		}
	}

	public void printChanges(PrintStream ps) {
		if (getCType() != Type.Unchanged) {
			ps.println("\t\t\tMethod: "
					+ getFullName()
					+ " --> "
					+ (this.mappedMethod == null ? "null" : this.mappedMethod.getFullName()));
		}
	}

	@Override
	public String toString() {
		return getQualName();
	}

	public ChangeGraph getChangeGraph(Repository repository, RevCommit commit) {
		PDGGraph pdg1 = new PDGGraph(this.declaration, new PDGBuildingContext(repository, commit, this.getCFile().getPath(), false));
		pdg1.buildChangeGraph(0);
		PDGGraph pdg2 = new PDGGraph(this.mappedMethod.declaration, new PDGBuildingContext(repository, commit, this.mappedMethod.getCFile().getPath(), false));
		pdg2.buildChangeGraph(1);
		pdg2.buildChangeGraph(pdg1);
		return new ChangeGraph(pdg2);
	}
}
