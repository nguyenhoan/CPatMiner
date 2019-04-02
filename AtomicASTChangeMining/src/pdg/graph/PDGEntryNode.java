package pdg.graph;

import org.eclipse.jdt.core.dom.ASTNode;

public class PDGEntryNode extends PDGNode {
	private String label;
	
	public PDGEntryNode(ASTNode astNode, int nodeType, String label) {
		super(astNode, nodeType);
		this.label = label;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public String getExasLabel() {
		return label;
	}
	
	@Override
	public boolean isDefinition() {
		return false;
	}
	
	@Override
	public String toString() {
		return getLabel();
	}
}
