package change;

import java.io.Serializable;
import java.util.HashSet;

public class ChangeGraph implements Serializable {
	private static final long serialVersionUID = -874502848659906533L;
	
	private HashSet<ChangeNode> nodes = new HashSet<>();

	public HashSet<ChangeNode> getNodes() {
		return nodes;
	}

}
