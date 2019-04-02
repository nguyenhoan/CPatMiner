package pdg.graph;

import org.eclipse.jdt.core.dom.ASTNode;
import pdg.graph.PDGDataEdge.Type;

public class PDGDataNode extends PDGNode {
	protected boolean isField = false, isDeclaration = false;
	
	protected String dataName;
	
	public PDGDataNode(ASTNode astNode, int nodeType, String key, String dataType, String dataName) {
		super(astNode, nodeType, key);
		this.dataType = dataType;
		this.dataName = dataName;
	}
	
	public PDGDataNode(ASTNode astNode, int nodeType, String key, String dataType, String dataName, boolean isField, boolean isDeclaration) {
		this(astNode, nodeType, key, dataType, dataName);
		this.isField = isField;
		this.isDeclaration = isDeclaration;
	}

	public PDGDataNode(PDGDataNode node) {
		this(node.astNode, node.astNodeType, node.key, node.dataType, node.dataName, node.isField, node.isDeclaration);
	}

	@Override
	public String getDataName() {
		return dataName;
	}

	@Override
	public String getLabel() {
		if (astNodeType == ASTNode.CHARACTER_LITERAL || astNodeType == ASTNode.STRING_LITERAL)
			return dataType + "(lit(" + dataName.substring(1, dataName.length()-1) + "))";
		return dataType + "(" + dataName + ")";
	}

	@Override
	public String getExasLabel() {
		if (astNodeType == ASTNode.NULL_LITERAL)
			return "null";
		if (astNodeType == ASTNode.BOOLEAN_LITERAL)
			return "boolean";
		if (astNodeType == ASTNode.CHARACTER_LITERAL)
			return "char";
		if (astNodeType == ASTNode.NUMBER_LITERAL)
			return "number";
		if (astNodeType == ASTNode.STRING_LITERAL)
			return "String";
		/*if (astNodeType == ASTNode.CHARACTER_LITERAL || astNodeType == ASTNode.STRING_LITERAL)
			return dataType + "(lit(" + dataName.substring(1, dataName.length()-1) + "))";
		return dataType;*/
		if (dataType == null)
			return "UNKNOWN";
		return dataType;
	}
	
	@Override
	public boolean isDefinition() {
		for (PDGEdge e : inEdges)
			if (((PDGDataEdge) e).type == Type.DEFINITION)
				return true;
		return false;
	}
	
	@Override
	public boolean isSame(PDGNode node) {
		if (node instanceof PDGDataNode)
			return dataName.equals(((PDGDataNode) node).dataName) && dataType.equals(((PDGDataNode) node).dataType);
		return false;
	}

	public boolean isDummy() {
		return key.startsWith(PREFIX_DUMMY);
	}

	public PDGNode getQualifier() {
		for (PDGEdge e : inEdges)
			if (e instanceof PDGDataEdge && ((PDGDataEdge) e).type == Type.QUALIFIER)
				return e.source;
		return null;
	}

	public void copyData(PDGDataNode node) {
		this.astNode = node.astNode;
		this.astNodeType = node.astNodeType;
		this.dataName = node.dataName;
		this.dataType = node.dataType;
		this.key = node.key;
		this.isField = node.isField;
		this.isDeclaration = node.isDeclaration;
	}
	
	@Override
	public String toString() {
		return getLabel();
	}
}
