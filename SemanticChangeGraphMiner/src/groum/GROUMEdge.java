package groum;

import java.util.HashSet;

public class GROUMEdge {
	public static int numOfEdges = 0;
	
	private int id;
	private GROUMNode src;
	private GROUMNode dest;
	private String label = ".";
	
	public GROUMEdge(GROUMNode src, GROUMNode dest) {
		this.id = ++numOfEdges;
		this.src = src;
		this.dest = dest;
		src.addOutEdge(this);
		dest.addInEdge(this);
	}
	public GROUMEdge(GROUMNode src, GROUMNode dest, String label) {
		this(src, dest);
		this.label = label;
	}
	
	public GROUMNode getSrc() {
		return src;
	}
	public void setSrc(GROUMNode node) {
		if (dest.getInNodes().contains(node))
			delete();
		else
		{
			this.src = node;
			node.addOutEdge(this);
		}
	}
	public GROUMNode getDest() {
		return dest;
	}
	public void setDest(GROUMNode node) {
		if (src.getOutNodes().contains(node))
			delete();
		else
		{
			this.dest = node;
			node.addInEdge(this);
		}
	}
	/**
	 * @return the index
	 */
	public int getId() {
		return id;
	}
	/**
	 * @param id the index to set
	 */
	public void setId(int id) {
		this.id = id;
	}
	/**
	 * @return the label
	 */
	public String getLabel() {
		return label;
	}
	/**
	 * @param label the label to set
	 */
	public void setLabel(String label) {
		this.label = label;
	}
	
	public void delete() {
		this.src.getOutEdges().remove(this);
		this.dest.getInEdges().remove(this);
		// FIXME
		/*this.src = null;
		this.dest = null;*/
	}
	
	public boolean isControl() {
		return label.equals("_control_");
	}
	
	public boolean isData() {
		return !isControl() && !label.isEmpty();
	}
	
	public boolean isMap() {
		return label.equals("_map_");
	}

	public boolean isDef() {
		return label.equals("_def_");
	}

	public boolean isParameter() {
		return label.equals("_para_");
	}
	
	public void pruneControlClosure() {
		for (GROUMEdge e : src.getInEdges()) {
			for (GROUMEdge oe : new HashSet<>(e.src.getOutEdges())) {
				if (oe.isControl() && oe.dest == dest) {
					oe.pruneControlClosure();
					oe.delete();
					break;
				}
			}
		}
	}
	
	public void pruneDataClosure() {
		for (GROUMEdge e : src.getInEdges()) {
			if (e.isData()) {
				for (GROUMEdge oe : new HashSet<>(e.src.getOutEdges())) {
					if (oe.label.equals(label) && oe.dest == dest) {
						oe.pruneDataClosure();
						oe.delete();
						break;
					}
				}
			}
		}
	}
}
