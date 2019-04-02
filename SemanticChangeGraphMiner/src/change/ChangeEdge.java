package change;

import java.io.Serializable;

public class ChangeEdge implements Serializable {
	private static final long serialVersionUID = -6490730732956992708L;
	
	ChangeNode source, target;
	String label;

	public ChangeNode getSource() {
		return source;
	}

	public ChangeNode getTarget() {
		return target;
	}

	public String getLabel() {
		return label;
	}
	
	@Override
	public String toString() {
		return label;
	}
}
