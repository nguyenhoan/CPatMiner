package pdg.graph;

public abstract class PDGEdge {
	protected PDGNode source;
	protected PDGNode target;
	
	public PDGEdge(PDGNode source, PDGNode target) {
		this.source = source;
		this.target = target;
	}

	public abstract String getLabel();

	public PDGNode getSource() {
		return source;
	}

	public PDGNode getTarget() {
		return target;
	}

	public abstract String getExasLabel();
}
