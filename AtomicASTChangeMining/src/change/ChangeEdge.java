package change;

import java.io.Serializable;

import pdg.graph.PDGEdge;

public class ChangeEdge implements Serializable {
	private static final long serialVersionUID = -6490730732956992708L;

	ChangeNode source, target;
	String label;

	public ChangeEdge(ChangeNode source, ChangeNode target, PDGEdge e) {
		this.source = source;
		this.target = target;
		this.label = e.getExasLabel();
		source.outEdges.add(this);
		target.inEdges.add(this);
	}

	public ChangeNode getSource() {
		return source;
	}

	public ChangeNode getTarget() {
		return target;
	}

	public String getLabel() {
		return label;
	}

	public boolean isMapped() {
		return label.equals("_map_");
	}

	@Override
	public String toString() {
		return label;
	}
}
