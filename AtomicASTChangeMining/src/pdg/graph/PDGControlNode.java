package pdg.graph;

import org.eclipse.jdt.core.dom.ASTNode;

public class PDGControlNode extends PDGNode {

	public PDGControlNode(PDGNode control, String branch, ASTNode astNode, int nodeType) {
		super(astNode, nodeType);
		this.control = control;
		new PDGControlEdge(control, this, branch);
	}

	@Override
	public String getLabel() {
		return ASTNode.nodeClassForType(astNodeType).getSimpleName();
	}

	@Override
	public String getExasLabel() {
		return ASTNode.nodeClassForType(astNodeType).getSimpleName();
	}
	
	@Override
	public boolean isDefinition() {
		return false;
	}
	
	@Override
	public String toString() {
		return getLabel();
	}

	public PDGGraph getBody() {
		PDGGraph g = new PDGGraph(null);
		g.nodes.add(this);
		for (PDGEdge e : outEdges) {
			PDGNode node = e.target;
			if (!node.isEmptyNode())
				g.nodes.add(node);
		}
		for (PDGEdge e : outEdges) {
			PDGNode node = e.target;
			node.addNeighbors(g.nodes);
		}
		g.nodes.remove(this);
		return g;
	}
	
	@Override
	public boolean isSame(PDGNode node) {
		if (node instanceof PDGControlNode)
			return astNodeType == node.astNodeType;
		return false;
	}
}
