package groum;

import mining.Fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;

import change.ChangeEdge;
import change.ChangeGraph;
import change.ChangeNode;


public class GROUMGraph {
	public static int nextId = 1;
	private int id, patternId = -1;
	private String project, name;
	private HashSet<GROUMNode> nodes = new HashSet<GROUMNode>();
	
	public GROUMGraph() {
		this.id = nextId++;
	}
	
	public GROUMGraph(ChangeGraph pdg, String name) {
		this.name = name;
		HashSet<ChangeNode> changedNodes = pdg.getNodes();
		if (changedNodes.isEmpty()) return;
		HashMap<ChangeNode, GROUMNode> map = new HashMap<>();
		for (ChangeNode node : changedNodes) {
			GROUMNode cn = new GROUMNode(node);
			cn.setGraph(this);
			map.put(node, cn);
			nodes.add(cn);
		}
		for (ChangeNode node : changedNodes) {
			GROUMNode cn = map.get(node);
			for (ChangeEdge e : node.getInEdges()) {
				new GROUMEdge(map.get(e.getSource()), cn, e.getLabel());
			}
		}
	}

	public GROUMGraph(Fragment f) {
		this.name = f.getGraph().getName();
		this.project = f.getGraph().getProject();
		HashMap<GROUMNode, GROUMNode> map = new HashMap<>();
		HashSet<GROUMNode> oldNodes = new HashSet<>(), newNodes = new HashSet<>();
		for (GROUMNode node : f.getNodes()) {
			GROUMNode cn = new GROUMNode(node);
			if (cn.isCoreAction())
				cn.setLabel(String.valueOf(cn.getAstType()));
			cn.setGraph(this);
			map.put(node, cn);
			this.nodes.add(cn);
			if (cn.getVersion() == 0) // in old version
				oldNodes.add(cn);
			else if (cn.getVersion() == 1) // in new version
				newNodes.add(cn);
			else
				throw new RuntimeException("Invalid version value:" + cn.getVersion());
		}
		for (GROUMNode node : f.getNodes()) {
			GROUMNode cn = map.get(node);
			for (GROUMEdge e : node.getInEdges()) {
				GROUMNode s = e.getSrc();
				if (map.containsKey(s))
					new GROUMEdge(map.get(s), cn, e.getLabel());
			}
		}
	}

	public void delete(GROUMNode node) {
		nodes.remove(node);
	}

	public void deleteAssignmentNodes() {
		for (GROUMNode node : new HashSet<GROUMNode>(nodes))
			if (node.isAssignment()) {
				for (GROUMEdge ie : node.getInEdges()) {
					if (ie.isParameter()) {
						GROUMNode n = ie.getSrc();
						for (GROUMEdge oe : node.getOutEdges())
							if (oe.isDef() && !n.getOutNodes().contains(oe.getDest()))
								new GROUMEdge(n, oe.getDest(), oe.getLabel()); // shortcut definition edges before deleting this assignment
					}
				}
				node.delete();
			}
	}

	public void deleteUnaryOperationNodes() {
		for (GROUMNode node : new HashSet<GROUMNode>(nodes))
			if (node.getAstType() == ASTNode.PREFIX_EXPRESSION || node.getAstType() == ASTNode.POSTFIX_EXPRESSION)
				node.delete();
	}

	@SuppressWarnings("unused")
	private void deleteAssignmentEdges() {
		for (GROUMNode node : new HashSet<GROUMNode>(nodes))
			if (node.isAssignment()) {
				for (GROUMEdge e : new HashSet<GROUMEdge>(node.getOutEdges()))
					if (!e.isDef())
						e.delete();
				if (node.getOutEdges().isEmpty())
					node.delete();
			}
	}
	
	public void collapseLiterals() {
		for (GROUMNode node : new HashSet<GROUMNode>(nodes)) {
			HashMap<String, ArrayList<GROUMNode>> labelLiterals = new HashMap<>();
			for (GROUMNode n : node.getInNodes()) {
				if (n.isLiteral()) {
					String label = n.getLabel();
					ArrayList<GROUMNode> lits = labelLiterals.get(label);
					if (lits == null) {
						lits = new ArrayList<>();
						labelLiterals.put(label, lits);
					}
					lits.add(n);
				}
			}
			for (String label : labelLiterals.keySet()) {
				ArrayList<GROUMNode> lits = labelLiterals.get(label);
				if (lits.size() > 1) {
					char cl = (char) (label.charAt(0) + 128);
					lits.get(1).setLabel(String.valueOf(cl));
					for (int i = 2; i < lits.size(); i++)
						lits.get(i).delete();
				}
			}
		}
	}

	public boolean hasDuplicateEdge() {
		for (GROUMNode node : nodes)
			if (node.hasDuplicateEdge())
				return true;
		return false;
	}
	/**
	 * @return the index
	 */
	public int getId() {
		return id;
	}

	/**
	 * @param index the index to set
	 */
	public void setId(int index) {
		this.id = index;
	}

	public int getPatternId() {
		return patternId;
	}

	public void setPatternId(int patternId) {
		this.patternId = patternId;
	}

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public HashSet<GROUMNode> getNodes() {
		return nodes;
	}

	public void pruneDoubleEdges() {
		/*for (GROUMNode node : this.nodes) {
			HashMap<String, HashMap<GROUMNode, ArrayList<GROUMEdge>>> labelNodeEdges = new HashMap<>();
			for (GROUMEdge e : node.getOutEdges()) {
				HashMap<GROUMNode, ArrayList<GROUMEdge>> nodeEdges = labelNodeEdges.get(e.getLabel());
				if (nodeEdges == null) {
					nodeEdges = new HashMap<>();
					labelNodeEdges.put(node.getLabel(), nodeEdges);
				}
				ArrayList<GROUMEdge> edges = nodeEdges.get(e.getDest());
				if (edges == null) {
					edges = new ArrayList<>();
					nodeEdges.put(e.getDest(), edges);
				}
				edges.add(e);
			}
			for (String label : labelNodeEdges.keySet()) {
				HashMap<GROUMNode, ArrayList<GROUMEdge>> nodeEdges = labelNodeEdges.get(label);
				for (GROUMNode n : nodeEdges.keySet()) {
					ArrayList<GROUMEdge> edges = nodeEdges.get(n);
					for (int i = 1; i < edges.size(); i++)
						edges.get(i).delete();
				}
			}
		}*/
		for (GROUMNode node : this.nodes) {
			HashSet<GROUMEdge> es = node.getMappedEdges();
			if (es.size() > 1) {
				GROUMEdge me = null;
				for (GROUMEdge e : es) {
					if (e.getSrc().getLabel().equals(e.getDest().getLabel())) {
						me = e;
						break;
					}
				}
				for (GROUMEdge e : es) {
					if (e != me)
						e.delete();
				}
			}
		}
	}
}
