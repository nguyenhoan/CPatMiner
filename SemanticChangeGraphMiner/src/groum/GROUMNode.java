package groum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.InfixExpression;

import change.ChangeNode;

public class GROUMNode {
	public static final int TYPE_FIELD = 0;
	public static final int TYPE_ACTION = 1;
	public static final int TYPE_CONTROL = 2;
	public static final int TYPE_OTHER = 11111;
	public static final int TYPE_SINGLE = 1;
	public static final int TYPE_MULTIPLE = 2;
	public static final int TYPE_LOOP = 3;
	public static int numOfNodes = 0;
	private static HashSet<Character> invocationTypes = new HashSet<>(), controlTypes = new HashSet<>(), literalTypes = new HashSet<>();
	private static HashMap<String, Character> infixExpressionLables = new HashMap<>();
	static {
		invocationTypes.add((char) ASTNode.ARRAY_ACCESS);
		invocationTypes.add((char) ASTNode.ARRAY_CREATION);
		invocationTypes.add((char) ASTNode.ARRAY_INITIALIZER);
		invocationTypes.add((char) ASTNode.ASSERT_STATEMENT);
		invocationTypes.add((char) ASTNode.BREAK_STATEMENT);
		invocationTypes.add((char) ASTNode.CAST_EXPRESSION);
		invocationTypes.add((char) ASTNode.CLASS_INSTANCE_CREATION);
		invocationTypes.add((char) ASTNode.CONSTRUCTOR_INVOCATION);
		invocationTypes.add((char) ASTNode.CONTINUE_STATEMENT);
		invocationTypes.add((char) ASTNode.INSTANCEOF_EXPRESSION);
		invocationTypes.add((char) ASTNode.METHOD_INVOCATION);
		invocationTypes.add((char) ASTNode.RETURN_STATEMENT);
		invocationTypes.add((char) ASTNode.SUPER_CONSTRUCTOR_INVOCATION);
		invocationTypes.add((char) ASTNode.SUPER_METHOD_INVOCATION);
		invocationTypes.add((char) ASTNode.THROW_STATEMENT);
		
		controlTypes.add((char) ASTNode.CATCH_CLAUSE);
		controlTypes.add((char) ASTNode.DO_STATEMENT);
		controlTypes.add((char) ASTNode.ENHANCED_FOR_STATEMENT);
		controlTypes.add((char) ASTNode.FOR_STATEMENT);
		controlTypes.add((char) ASTNode.IF_STATEMENT);
		controlTypes.add((char) ASTNode.SWITCH_STATEMENT);
		controlTypes.add((char) ASTNode.SYNCHRONIZED_STATEMENT);
		controlTypes.add((char) ASTNode.TRY_STATEMENT);
		controlTypes.add((char) ASTNode.WHILE_STATEMENT);
		
		literalTypes.add((char) ASTNode.BOOLEAN_LITERAL);
		literalTypes.add((char) ASTNode.CHARACTER_LITERAL);
		literalTypes.add((char) ASTNode.NULL_LITERAL);
		literalTypes.add((char) ASTNode.NUMBER_LITERAL);
		literalTypes.add((char) ASTNode.STRING_LITERAL);
		literalTypes.add((char) ASTNode.TYPE_LITERAL);
		
		// Arithmetic Operators
		infixExpressionLables.put(InfixExpression.Operator.DIVIDE.toString(), 'a');
		infixExpressionLables.put(InfixExpression.Operator.MINUS.toString(), 'a');
		infixExpressionLables.put(InfixExpression.Operator.PLUS.toString(), 'a');
		infixExpressionLables.put(InfixExpression.Operator.REMAINDER.toString(), 'a');
		infixExpressionLables.put(InfixExpression.Operator.TIMES.toString(), 'a');
		// Equality and Relational Operators
		infixExpressionLables.put(InfixExpression.Operator.EQUALS.toString(), 'r');
		infixExpressionLables.put(InfixExpression.Operator.GREATER.toString(), 'r');
		infixExpressionLables.put(InfixExpression.Operator.GREATER_EQUALS.toString(), 'r');
		infixExpressionLables.put(InfixExpression.Operator.LESS.toString(), 'r');
		infixExpressionLables.put(InfixExpression.Operator.LESS_EQUALS.toString(), 'r');
		infixExpressionLables.put(InfixExpression.Operator.NOT_EQUALS.toString(), 'r');
		// Conditional Operators
		infixExpressionLables.put(InfixExpression.Operator.CONDITIONAL_AND.toString(), 'c');
		infixExpressionLables.put(InfixExpression.Operator.CONDITIONAL_OR.toString(), 'c');
		// Bitwise and Bit Shift Operators
		infixExpressionLables.put(InfixExpression.Operator.AND.toString(), 'b');
		infixExpressionLables.put(InfixExpression.Operator.OR.toString(), 'b');
		infixExpressionLables.put(InfixExpression.Operator.XOR.toString(), 'b');
		infixExpressionLables.put(InfixExpression.Operator.LEFT_SHIFT.toString(), 's');
		infixExpressionLables.put(InfixExpression.Operator.RIGHT_SHIFT_SIGNED.toString(), 's');
		infixExpressionLables.put(InfixExpression.Operator.RIGHT_SHIFT_UNSIGNED.toString(), 's');
	}
	
	private int id;
	private String label;
	private HashSet<Integer> parameters;
	private int type = TYPE_OTHER, changeType = -1, version = -1;
	private char astType = 0;
	private int[] starts, lengths;
	private String dataType, dataName;
	private GROUMGraph graph;
	private HashSet<GROUMEdge> inEdges = new HashSet<GROUMEdge>();
	private HashSet<GROUMEdge> outEdges = new HashSet<GROUMEdge>();

	public GROUMNode(ChangeNode node) {
		this.changeType = node.getChangeType();
		this.version = node.getVersion();
		this.astType = (char) node.getAstNodeType();
		if (node.getStarts() != null)
			this.starts = Arrays.copyOf(node.getStarts(), node.getStarts().length);
		if (node.getLengths() != null)
			this.lengths = Arrays.copyOf(node.getLengths(), node.getLengths().length);
		this.dataType = node.getDataType();
		this.label = String.valueOf(this.astType);;
		/*if (node.getAstNodeType() == ASTNode.ARRAY_ACCESS) {
			this.type = TYPE_ACTION;
			this.label = "[]";
		} else if(node.getAstNodeType() == ASTNode.FIELD_ACCESS || node.getAstNodeType() == ASTNode.QUALIFIED_NAME) {
			this.type = TYPE_ACTION;
		} else */if (node.getType().equals("a")) {
			this.type = TYPE_ACTION;
			if (isInvocation(this.astType))
				this.label = node.getLabel();
			else if (this.astType == ASTNode.INFIX_EXPRESSION) {
				char cl = (char) (infixExpressionLables.get(node.getLabel()) + 128);
				this.label = String.valueOf(cl);
			} else {
				this.label = node.getLabel();
				if (this.label.length() == 1) {
					char cl = (char) (label.charAt(0) + 128);
					this.label = String.valueOf(cl);
				} else
					System.err.print("");
			}
		} else if (node.getType().equals("c")) {
			this.type = TYPE_CONTROL;
		} else if (node.getType().equals("d")) {
			this.type = TYPE_FIELD;
			if (!isLiteral()) {
				this.astType = ASTNode.SIMPLE_NAME;
				this.label = String.valueOf(this.astType);
			}
		}
	}
	
	public GROUMNode(GROUMNode node) {
		this.astType = node.astType;
		this.changeType = node.changeType;
		this.label = node.label;
		this.type = node.type;
		this.version = node.version;
		this.dataName = node.dataName;
		this.dataType = node.dataType;
		if (node.starts != null)
			this.starts = Arrays.copyOf(node.starts, node.starts.length);
		if (node.lengths != null)
			this.lengths = Arrays.copyOf(node.lengths, node.lengths.length);
	}

	public GROUMNode(String id, int version, HashMap<String, String> attributes) {
		this.version = version;
		if (version == 0)
			this.changeType = -1;
		else 
			this.changeType = 1;
		this.label = attributes.get("label");
		switch (attributes.get("shape")) {
		case "box": this.type = TYPE_ACTION; break;
		case "diamond": this.type = TYPE_CONTROL; break;
		default: this.type = TYPE_FIELD;
		}
		if (attributes.containsKey("t"))
			this.astType = attributes.get("t").charAt(0);
		if (attributes.containsKey("s"))
			this.starts = parseInts(attributes.get("s"));
		if (attributes.containsKey("l"))
			this.lengths = parseInts(attributes.get("l"));
	}

	private int[] parseInts(String s) {
		if (s.isEmpty()) 
			return new int[0];
		String[] parts = s.split(",");
		int[] a = new int[parts.length];
		for (int i = 0; i < parts.length; i++)
			a[i] = Integer.parseInt(parts[i]);
		return a;
	}

	public int getId() {
		return id;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getType() {
		return type;
	}

	public char getAstType() {
		return astType;
	}

	public int getVersion() {
		return version;
	}

	public int[] getStarts() {
		return starts;
	}

	public int[] getLengths() {
		return lengths;
	}

	public String getDataType() {
		return dataType;
	}

	public String getDataName() {
		return dataName;
	}

	public int getChangeType() {
		return changeType;
	}
	
	public void setChangeType(int changeType) {
		this.changeType = changeType;
	}

	public void setParameters(HashSet<Integer> parameters)
	{
		this.parameters = parameters; 
	}
	
	public HashSet<Integer> getParameters() {
		return parameters;
	}

	public GROUMGraph getGraph() {
		return graph;
	}

	public void setGraph(GROUMGraph graph) {
		this.graph = graph;
	}
	
	public HashSet<GROUMEdge> getInEdges() {
		return inEdges;
	}

	public HashSet<GROUMEdge> getOutEdges() {
		return outEdges;
	}
	
	public HashSet<GROUMNode> getInNodes() {
		HashSet<GROUMNode> nodes = new HashSet<GROUMNode>();
		for (GROUMEdge e : inEdges)
			nodes.add(e.getSrc());
		return nodes;
	}
	
	public HashSet<GROUMNode> getOutNodes() {
		HashSet<GROUMNode> nodes = new HashSet<GROUMNode>();
		for (GROUMEdge e : outEdges)
			nodes.add(e.getDest());
		return nodes;
	}

	public void getOutNodes(HashSet<GROUMNode> refs, HashSet<GROUMNode> nonrefs) {
		for (GROUMEdge e : outEdges) {
			if (e.getLabel().equals("_ref_"))
				refs.add(e.getDest());
			else
				nonrefs.add(e.getDest());
		}
	}
	
	public void addInEdge(GROUMEdge edge) {
		inEdges.add(edge);
	}
	
	public void addOutEdge(GROUMEdge edge) {
		outEdges.add(edge);
	}

	public GROUMNode getMappedNode() {
		for (GROUMEdge e : outEdges)
			if (e.isMap())
				return e.getDest();
		for (GROUMEdge e : inEdges)
			if (e.isMap())
				return e.getSrc();
		return null;
	}

	public HashSet<GROUMNode> getMappedNodes() {
		HashSet<GROUMNode> ns = new HashSet<>();
		for (GROUMEdge e : outEdges)
			if (e.isMap())
				ns.add(e.getDest());
		for (GROUMEdge e : inEdges)
			if (e.isMap())
				ns.add(e.getSrc());
		return ns;
	}

	public HashSet<GROUMEdge> getMappedEdges() {
		HashSet<GROUMEdge> es = new HashSet<>();
		for (GROUMEdge e : outEdges)
			if (e.isMap())
				es.add(e);
		for (GROUMEdge e : inEdges)
			if (e.isMap())
				es.add(e);
		return es;
	}

	public HashSet<GROUMNode> getPairedNodes() {
		HashSet<GROUMNode> pairedNodes = new HashSet<>();
		GROUMNode mappedNode = getMappedNode();
		if (mappedNode != null) {
			pairedNodes.add(mappedNode);
			return pairedNodes;
		}
		for (GROUMNode node : this.graph.getNodes())
			if (node.getVersion() != this.version)
				pairedNodes.add(node);
		return pairedNodes;
	}

	public boolean isAssignment() {
		return astType == ASTNode.ASSIGNMENT;
	}
	
	public boolean isControl() {
		return type == TYPE_CONTROL;
	}
	
	public boolean isCoreAction() {
		return type == TYPE_ACTION && isCoreAction(label);
	}

	public boolean isInvocation() {
		return isInvocation(astType);
	}

	public boolean isLiteral() {
		return literalTypes.contains(astType);
	}
	
	public void delete() {
		for (GROUMEdge e  : new HashSet<GROUMEdge>(inEdges))
			e.delete();
		for (GROUMEdge e : new HashSet<GROUMEdge>(outEdges))
			e.delete();
		graph.delete(this);
	}

	public boolean hasDuplicateEdge() {
		HashMap<String, HashSet<GROUMNode>> edgeNodes = new HashMap<>();
		for (GROUMEdge e : this.inEdges) {
			String label = e.getLabel();
			HashSet<GROUMNode> ns = edgeNodes.get(label);
			if (ns == null) {
				ns = new HashSet<>();
				edgeNodes.put(label, ns);
			} else if (ns.contains(e.getSrc()))
				return true;
			ns.add(e.getSrc());
		}
		edgeNodes = new HashMap<>();
		for (GROUMEdge e : this.outEdges) {
			String label = e.getLabel();
			HashSet<GROUMNode> ns = edgeNodes.get(label);
			if (ns == null) {
				ns = new HashSet<>();
				edgeNodes.put(label, ns);
			} else if (ns.contains(e.getDest()))
				return true;
			ns.add(e.getDest());
		}
		return false;
	}
	
	@Override
	public String toString() {
		return label;
	}

	public static boolean isControl(String label) {
		if (label.length() != 1)
			return false;
		char type = label.charAt(0);
		return controlTypes.contains(type);
	}
	
	public static boolean isCoreAction(String label) {
		return label.length() > 1;
	}

	public static boolean isInvocation(char astNodeType) {
		return invocationTypes.contains(astNodeType);
	}

	public ArrayList<GROUMNode> getDefinitions() {
		ArrayList<GROUMNode> defs = new ArrayList<>();
		for (GROUMEdge e : inEdges)
			if (e.getLabel().equals("_ref_"))
				defs.add(e.getSrc());
		return defs;
	}

	public ArrayList<GROUMNode> getReferences() {
		ArrayList<GROUMNode> refs = new ArrayList<>();
		for (GROUMEdge e : outEdges)
			if (e.getLabel().equals("_ref_"))
				refs.add(e.getDest());
		return refs;
	}

	public boolean isGoto() {
		return this.astType == ASTNode.RETURN_STATEMENT || this.astType == ASTNode.BREAK_STATEMENT || this.astType == ASTNode.CONTINUE_STATEMENT;
	}
}
