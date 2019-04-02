/**
 * 
 */
package mining;

import exas.ExasFeature;
import graphics.DotGraph;
import groum.GROUMEdge;
import groum.GROUMGraph;
import groum.GROUMNode;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.jdt.core.dom.ASTNode;

/**
 * @author Nguyen Anh Hoan
 *
 */
public class Fragment {
	public static final int minSize = 2;
	public static final int maxSize = 20;
	
	public static int nextFragmentId = 1, numofFragments = 0;
	
	private int id = -1;
	private Fragment genFragmen;
	private ArrayList<GROUMNode> nodes = new ArrayList<>();
	private HashSet<GROUMNode> oldNodes = new HashSet<>(), newNodes = new HashSet<>();
	private GROUMGraph graph;
	private HashMap<Integer, Integer> vector = new HashMap<>();
	private int idSum = 0;
	
	private Fragment() {
		this.id = nextFragmentId++;
		numofFragments++;
	}
	
	public Fragment(GROUMNode node) {
		this();
		this.graph = node.getGraph();
		nodes.add(node);
		if (node.getVersion() == 0)
			oldNodes.add(node);
		else if (node.getVersion() == 1)
			newNodes.add(node);
		this.idSum = node.getId();
		vector.put(1, 1);
	}

	public Fragment(GROUMNode[] pair) {
		this();
		this.graph = pair[0].getGraph();
		nodes.add(pair[0]); nodes.add(pair[1]);
		for (GROUMNode node : pair) {
			if (node.getVersion() == 0)
				oldNodes.add(node);
			else if (node.getVersion() == 1)
				newNodes.add(node);
		}
		this.idSum = pair[0].getId() + pair[1].getId();
		ExasFeature exasFeature = new ExasFeature(nodes);
		addFeature(exasFeature.getFeature(pair[0].getLabel()), vector);
		addFeature(exasFeature.getFeature(pair[1].getLabel()), vector);
		ArrayList<String> labels = new ArrayList<>();
		labels.add(pair[0].getLabel());
		if (pair[0].getMappedNode() == pair[1] && pair[1].getMappedNode() == pair[0])
			labels.add("_map_");
		labels.add(pair[1].getLabel());
		addFeature(exasFeature.getFeature(labels), vector);
	}

	public Fragment extract() {
		Fragment f = new Fragment(id, nodes);
		return f;
	}
	
	public Fragment(Fragment fragment, ArrayList<GROUMNode> ens) {
		this();
		this.genFragmen = fragment;
		this.graph = fragment.graph;
		this.nodes = new ArrayList<GROUMNode>(fragment.nodes);
		this.oldNodes = new HashSet<>(fragment.oldNodes);
		this.newNodes = new HashSet<>(fragment.newNodes);
		this.idSum = fragment.getIdSum();
		this.vector = new HashMap<Integer, Integer>(fragment.getVector());
		for (GROUMNode en : ens) {
			this.nodes.add(en);
			if (en.getVersion() == 0)
				oldNodes.add(en);
			else if (en.getVersion() == 1)
				newNodes.add(en);
			this.idSum += en.getId();
			ExasFeature exasFeature = new ExasFeature(nodes);
			buildVector(en, exasFeature);
		}
	}
	
	public Fragment(ArrayList<GROUMNode> ens) {
		this();
		for (GROUMNode en : ens) {
			this.nodes.add(en);
			if (en.getVersion() == 0)
				oldNodes.add(en);
			else if (en.getVersion() == 1)
				newNodes.add(en);
			this.idSum += en.getId();
			ExasFeature exasFeature = new ExasFeature(nodes);
			buildVector(en, exasFeature);
		}
	}

	public Fragment(int id, ArrayList<GROUMNode> nodes) {
		this.id = id;
		HashMap<GROUMNode, GROUMNode> map = new HashMap<>();
		for (GROUMNode node : nodes) {
			GROUMNode cn = new GROUMNode(node);
			map.put(node, cn);
			this.nodes.add(cn);
			if (cn.getVersion() == 0)
				oldNodes.add(cn);
			else if (cn.getVersion() == 1)
				newNodes.add(cn);
		}
		for (GROUMNode node : nodes) {
			GROUMNode cn = map.get(node);
			for (GROUMEdge e : node.getInEdges()) {
				GROUMNode s = e.getSrc();
				if (map.containsKey(s))
					new GROUMEdge(map.get(s), cn, e.getLabel());
			}
		}
	}

	public void pruneClosure() {
		pruneControlClosure();
		pruneDataClosure();
	}

	private void pruneDataClosure() {
		for (GROUMNode node : nodes) {
			for (GROUMEdge e : new HashSet<>(node.getInEdges())) {
				if (e.isData()) {
					e.pruneDataClosure();
				}
			}
		}
	}

	private void pruneControlClosure() {
		for (GROUMNode node : nodes) {
			for (GROUMEdge e : new HashSet<>(node.getInEdges())) {
				if (e.isControl()) {
					e.pruneControlClosure();
				}
			}
		}
	}
	
	public void buildVector(GROUMNode node, ExasFeature exasFeature) {
		ArrayList<String> sequence = new ArrayList<>();
		sequence.add(node.getLabel());
		backwardDFS(node, node, sequence, exasFeature);
	}
	
	private void backwardDFS(GROUMNode firstNode, GROUMNode lastNode, ArrayList<String> sequence, ExasFeature exasFeature) {
		forwardDFS(firstNode, lastNode, sequence, exasFeature);
		
		if(sequence.size() < ExasFeature.MAX_LENGTH) {
			for(GROUMEdge e : firstNode.getInEdges()) {
				if (nodes.contains(e.getSrc())) {
					GROUMNode n = e.getSrc();
					sequence.add(0, e.getLabel());
					sequence.add(0, n.getLabel());
					backwardDFS(n, lastNode, sequence, exasFeature);
					sequence.remove(0);
					sequence.remove(0);
				}
			}
		}
	}
	
	private void forwardDFS(GROUMNode firstNode, GROUMNode lastNode, ArrayList<String> sequence, ExasFeature exasFeature) {
		int feature = exasFeature.getFeature(sequence);
		addFeature(feature, vector);
		
		if(sequence.size() < ExasFeature.MAX_LENGTH) {
			for(GROUMEdge e : lastNode.getOutEdges()) {
				if (nodes.contains(e.getDest())) {
					GROUMNode n = e.getDest();
					sequence.add(e.getLabel());
					sequence.add(n.getLabel());
					forwardDFS(firstNode, n, sequence, exasFeature);
					sequence.remove(sequence.size()-1);
					sequence.remove(sequence.size()-1);
				}
			}
		}
	}
	
	private void addFeature(int feature, HashMap<Integer, Integer> vector) {
		int c = 0;
		if (vector.containsKey(feature))
			c = vector.get(feature);
		vector.put(feature, c+1);
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public Fragment getGenFragmen() {
		return genFragmen;
	}

	public void setGenFragmen(Fragment genFragmen) {
		this.genFragmen = genFragmen;
	}

	public ArrayList<GROUMNode> getNodes() {
		return nodes;
	}
	public void setNodes(ArrayList<GROUMNode> nodes) {
		this.nodes = nodes;
	}
	
	public int getIdSum() {
		return idSum;
	}
	public HashMap<Integer, Integer> getVector() {
		return vector;
	}
	public void setVector(HashMap<Integer, Integer> vector) {
		this.vector = vector;
	}
	public void setId() {
		this.id = nextFragmentId++;
		//return this.id;
	}
	/**
	 * @return the graph
	 */
	public GROUMGraph getGraph() {
		return graph;
	}
	/**
	 * @param graph the graph to set
	 */
	public void setGraph(GROUMGraph graph) {
		this.graph = graph;
	}
	
	public int getVectorHashCode() {
		ArrayList<Integer> keys = new ArrayList<>(vector.keySet());
		Collections.sort(keys);
		int h = 0;
		for (int key : keys) {
			h = h * 31 + vector.get(key);
		}
		return h;
	}
	
	/**
	 * Not exactly matched but the same vector
	 * @param frag
	 * @return
	 */
	public boolean exactCloneTo(Fragment frag) {
		if (this == frag) {
			System.err.println("Same fragment in exactCloneTo!!!");
			return false;
		}
		if (frag == null) {
			System.err.println("NULL fragment in exactCloneTo!!!");
			return false;
		}
		/*HashSet<CFGNode> tempNodes = new HashSet<CFGNode>();
		tempNodes.addAll(frag.nodes);
		tempNodes.retainAll(this.nodes);
		if(tempNodes.size() > 0)
			return false;*/
		if (vector == null || frag.vector == null) {
			System.err.println("NULL vector!!!");
			return false;
		}
		if(this.nodes.size() != frag.nodes.size() || !this.vector.equals(frag.vector))
			return false;
		return true;
	}
	/**
	 * The same subgraph - same set of nodes
	 * @param other
	 * @return
	 */
	public boolean isSameAs(Fragment other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		if (this.idSum != other.getIdSum())
			return false;
		return nodes.equals(other.nodes);
	}
	/**
	 * Set of nodes contains all the nodes of the other fragment
	 * @param fragment
	 * @return
	 */
	public boolean contains(Fragment fragment) {
		if(fragment == null) return false;
		if (nodes == null) 
			return false;
		if (graph != fragment.getGraph())
			return false;
		if (nodes.size() >= fragment.nodes.size() && nodes.containsAll((fragment.nodes)))
			return true;
		else
			return false;
	}
	/**
	 * 
	 */
	public boolean contains(GROUMNode node) {
		return this.nodes.contains(node);
	}
	public boolean overlap(Fragment fragment) {
		if (this == fragment) {
			System.err.println("Same fragment in checking overlap");
			return false;
		}
		if (fragment == null) {
			System.err.println("NULL fragment in checking overlap");
			return false;
		}
		HashSet<GROUMNode> tempNodes = new HashSet<GROUMNode>();
		tempNodes.addAll(fragment.nodes);
		tempNodes.retainAll(this.nodes);
		for (GROUMNode node : tempNodes)
			if (node.isCoreAction())
				return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		return id;
	}
	
	/*@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		result.append("Fragment " + this.id + ": " + 
				this.nodes.size() + " nodes\r\n");
		try {
			result.append("File: " + GROUMNode.fileNames.get(this.graph.getFileID()) + "\r\n");
		}
		catch(Exception e) {
			e.printStackTrace();
			System.err.println(this.graph);
			System.err.println(this.graph.getFileID());
			System.err.println(GROUMNode.fileNames.get(this.graph.getFileID()));
		}
		//result.append("Vector: " + this.gramVector + "\r\n");
		result.append(this.nodes.size() + " Nodes: ");
		for(GROUMNode node : this.nodes)
			result.append(node.getLabel() + " ");
		result.append("\r\n");
		for (GROUMNode node : this.nodes) {
			result.append("Node: " + node.getId() + 
					" - Label: " + node.getLabel() + 
					"\tLines: " + node.getStartLine() + "-->" + node.getEndLine() + "\r\n");
		}
		result.append("Edges:\r\n");
		HashSet<GROUMNode> nodes = new HashSet<GROUMNode>(this.nodes);
		LinkedList<GROUMNode> queue = new LinkedList<GROUMNode>();
		for(GROUMNode node : nodes)
		{
			HashSet<GROUMNode> tmp = new HashSet<GROUMNode>(node.getOutNodes());
			tmp.retainAll(nodes);
			if(tmp.isEmpty())
				queue.add(node);
		}
		nodes.removeAll(queue);
		while(!queue.isEmpty())
		{
			GROUMNode node = queue.poll();
			HashSet<GROUMNode> tmp = new HashSet<GROUMNode>(node.getInNodes());
			tmp.retainAll(this.nodes);
			for(GROUMNode n : tmp)
			{
				//result.append(node.getLabel() + "-->" + n.getLabel() + " ");
				result.append(node.getId() + "<--" + n.getId() + " ");
			}
			tmp.retainAll(nodes);
			queue.addAll(tmp);
			nodes.removeAll(tmp);
		}
		result.append("\r\n--------------------------------------------------\r\n");
		
		return result.toString();
	}*/
	
	public void toDot(String path, String name) {
		DotGraph dg = toDotGraph();
		File dir = new File(path);
		if (!dir.exists())
			dir.mkdirs();
		//name += "_ " + FileIO.getSimpleFileName(GROUMNode.fileNames.get(this.graph.getFileID()));
		dg.toDotFile(new File(path + "/" + name + ".dot"));
	}
	
	public void toGraphics(String path, String name) {
		DotGraph dg = toDotGraph();
		File dir = new File(path);
		if (!dir.exists())
			dir.mkdirs();
		//name += "_ " + FileIO.getSimpleFileName(GROUMNode.fileNames.get(this.graph.getFileID()));
		dg.toDotFile(new File(path + "/" + name + ".dot"));
		DotGraph.toGraphics(path + "/" + name, "png");
	}

	public DotGraph toDotGraph() {
		StringBuilder graph = new StringBuilder();
		DotGraph dg = new DotGraph(graph);
		graph.append(dg.addStart());
		
		StringBuilder[] subgraphs = new StringBuilder[2];
		subgraphs[0] = new StringBuilder();
		subgraphs[0].append(dg.addSubgraphStart("cluster0"));
		subgraphs[1] = new StringBuilder();
		subgraphs[1].append(dg.addSubgraphStart("cluster1"));
		HashMap<GROUMNode, Integer> ids = new HashMap<GROUMNode, Integer>();
		// add nodes
		int id = 0;
		for(GROUMNode node : nodes) {
			id++;
			ids.put(node, id);
			String label = node.getLabel();
			if (node.getType() == GROUMNode.TYPE_ACTION) {
				if (!node.isInvocation()) {
					if (label.length() == 1 && label.charAt(0) >= 128)
						label = ASTNode.nodeClassForType(node.getAstType()).getSimpleName() + ":" + ((char) (label.charAt(0) - 128));
					else
						label = ASTNode.nodeClassForType(node.getAstType()).getSimpleName();
				} else if (label.length() == 1 && label.charAt(0) < 'a') {
					try {
						label = ASTNode.nodeClassForType(node.getAstType()).getSimpleName();
					} catch (IllegalArgumentException e) {}
				}
			} else if (label.length() == 1) {
				label = ASTNode.nodeClassForType(node.getAstType()).getSimpleName();
				char ch = label.charAt(0);
				if (ch >= 128)
					label += "*";
			}
			add(subgraphs[node.getVersion()], dg, id, node.getType(), new String[]{"label", "a", "s", "l"}, new String[]{label, ""+((int)node.getAstType()), ""+buildValue(node.getStarts()), ""+buildValue(node.getLengths())});
		}
		subgraphs[0].append(dg.addSubgraphLabel("Old"));
		subgraphs[0].append(dg.addStyle("dotted"));
		subgraphs[0].append(dg.addSubgraphEnd());
		subgraphs[1].append(dg.addSubgraphLabel("New"));
		subgraphs[1].append(dg.addStyle("dotted"));
		subgraphs[1].append(dg.addSubgraphEnd());
		graph.append(subgraphs[0] + "\n");
		graph.append(subgraphs[1] + "\n");
		// add edges
		for(GROUMNode node : nodes) {
			int sId = ids.get(node);
			for(GROUMEdge out : node.getOutEdges()) {
				if (out.getLabel().equals(".")) continue;
				if (nodes.contains(out.getDest())) {
					int eId = ids.get(out.getDest());
					graph.append(dg.addEdge(sId, eId, null, null, out.getLabel()));
				}
			}
		}

		graph.append(dg.addEnd());
		return dg;
	}

	private String buildValue(int[] a) {
		if (a == null || a.length == 0)
			return "";
		String s = "" + a[0];
		for (int i = 1; i < a.length; i++)
			s += "," + a[i];
		return s;
	}

	public void add(StringBuilder graph, DotGraph dg, int id, int nodeType, String[] names, String[] values) {
		names = Arrays.copyOf(names, names.length+1);
		values = Arrays.copyOf(values, values.length+1);
		names[names.length-1] = "shape";
		if(nodeType == GROUMNode.TYPE_CONTROL)
			values[values.length-1] = DotGraph.SHAPE_DIAMOND;
		else if (nodeType == GROUMNode.TYPE_ACTION)
			values[values.length-1] = DotGraph.SHAPE_BOX;
		else
			values[values.length-1] = DotGraph.SHAPE_ELLIPSE;
		graph.append(dg.addNode(id, names, values));
	}
	
	public void delete() {
		this.genFragmen = null;
		this.graph = null;
		this.oldNodes.clear();
		this.newNodes.clear();
		this.vector.clear();
		try {
			this.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		numofFragments--;
	}
	
	HashSet<Fragment> exactCloneList(HashSet<Fragment> group, Fragment frag) {
		HashSet<Fragment> res = new HashSet<Fragment>();
		group.remove(frag);
		for (Fragment u : group) {
			if (frag.exactCloneTo(u)) 
				res.add(u);
		}
		if(res.contains(frag))
			res.remove(frag);
		return res;
	}
	
	HashSet<Fragment> nonOverlapCloneList(HashSet<Fragment> group, Fragment frag) {
		HashSet<Fragment> res = new HashSet<Fragment>();
		group.remove(frag);
		for (Fragment u : group) {
			if (!frag.overlap(u))
				res.add(u);
		}
		if(res.contains(frag))
			res.remove(frag);
		return res;
	}

	public HashMap<String, HashSet<ArrayList<GROUMNode>>> extend() {
		int[] sizes = getMethodSizes();
		HashSet<GROUMNode> ens = new HashSet<>();
		GROUMNode[] lasts = {null, null};
		for (GROUMNode node : nodes) {
			for (GROUMNode n : node.getInNodes()) {
				if (sizes[n.getVersion()] < Pattern.maxSize && !nodes.contains(n))
					ens.add(n);
			}
			for (GROUMNode n : node.getOutNodes()) {
				if (sizes[n.getVersion()] < Pattern.maxSize && !nodes.contains(n))
					ens.add(n);
			}
			lasts[node.getVersion()] = node;
		}
		HashMap<String, HashSet<ArrayList<GROUMNode>>> lens = new HashMap<>();
		for (GROUMNode node : ens) {
			if ((node.isCoreAction() || node.isControl())
					&& lasts[node.getVersion()] != null 
					&& node.getLabel().equals(lasts[node.getVersion()].getLabel())) 
				continue;
			GROUMNode mappedNode = node.getMappedNode();
			if (node.getType() == GROUMNode.TYPE_FIELD) {
				if (node.isLiteral()) {
					add(lens, node, mappedNode);
				} else {
					ArrayList<GROUMNode> defs = node.getDefinitions();
					if (defs.isEmpty()) {
						HashSet<GROUMNode> refs = new HashSet<GROUMNode>(), nonrefs = new HashSet<GROUMNode>();
						node.getOutNodes(refs, nonrefs);
						if (overlap(nonrefs, nodes))
							add(lens, node, mappedNode);
						else {
							for (GROUMNode next : nonrefs) {
								add(lens, node, mappedNode, next);
							}
						}
					}
				}
			} else if (node.getType() == GROUMNode.TYPE_ACTION) {
				if (node.isCoreAction()) {
					add(lens, node, mappedNode);
				}
				else {
					HashSet<GROUMNode> ins = node.getInNodes(), outs = node.getOutNodes();
					ins.remove(mappedNode); outs.remove(mappedNode);
					if (!ins.isEmpty() && !outs.isEmpty()) {
						boolean found = false;
						for (GROUMNode n : ins) {
							if (nodes.contains(n)) {
								found = true;
								break;
							}
						}
						if (found) {
							found = false;
							for (GROUMNode n : outs) {
								if (n.isCoreAction() && nodes.contains(n)) {
									found = true;
									break;
								}
							}
							if (found) {
								add(lens, node, mappedNode);
							} else {
								for (GROUMNode next : outs) {
									if (next.isCoreAction()) {
										add(lens, node, mappedNode, next);
									}
								}
							}
						} else {
							for (GROUMNode next : ins) {
								add(lens, node, mappedNode, next);
							}
						}
					}
				}
			} else if (node.getType() == GROUMNode.TYPE_CONTROL) {
				HashSet<GROUMNode> ins = node.getInNodes(), outs = node.getOutNodes();
				ins.remove(mappedNode); outs.remove(mappedNode);
				if (!ins.isEmpty() && !outs.isEmpty()) {
					boolean found = false;
					for (GROUMNode n : ins) {
						if (nodes.contains(n)) {
							found = true;
							break;
						}
					}
					if (found) {
						found = false;
						for (GROUMNode n : outs) {
							if (n.isCoreAction() && nodes.contains(n)) {
								found = true;
								break;
							}
						}
						if (found) {
							add(lens, node, mappedNode);
						} else {
							for (GROUMNode next : outs) {
								if (next.isCoreAction()) {
									add(lens, node, mappedNode, next);
								}
							}
						}
					} else {
						for (GROUMNode next : ins) {
							add(lens, node, mappedNode, next);
						}
					}
				}
			}
		}
		return lens;
	}

	private void add(HashMap<String, HashSet<ArrayList<GROUMNode>>> lens, GROUMNode node, GROUMNode mappedNode, GROUMNode next) {
		String label = node.getLabel();
		GROUMNode mappedNext = next.getMappedNode();
		if (mappedNode != null)
			label += "-" + mappedNode.getLabel();
		label += "-" + next.getLabel();
		if (mappedNext != null)
			label += "-" + mappedNext.getLabel();
		HashSet<ArrayList<GROUMNode>> s = lens.get(label);
		if (s == null) {
			s = new HashSet<>();
			lens.put(label, s);
		}
		ArrayList<GROUMNode> l = new ArrayList<>();
		l.add(node);
		if (mappedNode != null)
			l.add(mappedNode);
		l.add(next);
		if (mappedNext != null)
			l.add(mappedNext);
		s.add(l);
	}

	private void add(HashMap<String, HashSet<ArrayList<GROUMNode>>> lens, GROUMNode node, GROUMNode mappedNode) {
		String label = node.getLabel();
		if (mappedNode != null)
			label += "-" + mappedNode.getLabel();
		HashSet<ArrayList<GROUMNode>> s = lens.get(label);
		if (s == null) {
			s = new HashSet<>();
			lens.put(label, s);
		}
		ArrayList<GROUMNode> l = new ArrayList<>();
		l.add(node);
		if (mappedNode != null)
			l.add(mappedNode);
		s.add(l);
	}

	private <E> boolean overlap(HashSet<E> s1, ArrayList<E> s2) {
		for (E e : s1)
			if (s2.contains(e))
				return true;
		return false;
	}

	public HashMap<String, HashSet<ArrayList<GROUMNode>>> superExtend() {
		int[] sizes = getMethodSizes();
		HashSet<GROUMNode> ens = new HashSet<>();
		GROUMNode[] lasts = {null, null};
		for (GROUMNode node : nodes) {
			for (GROUMNode n : node.getInNodes()) {
				if (sizes[n.getVersion()] < Pattern.maxSize && !nodes.contains(n))
					ens.add(n);
			}
			for (GROUMNode n : node.getOutNodes()) {
				if (sizes[n.getVersion()] < Pattern.maxSize && !nodes.contains(n))
					ens.add(n);
			}
			lasts[node.getVersion()] = node;
		}
		HashMap<String, HashSet<ArrayList<GROUMNode>>> lens = new HashMap<>();
		for (GROUMNode node : ens) {
			if ((node.isCoreAction() || node.isControl())
					&& lasts[node.getVersion()] != null 
					&& node.getLabel().equals(lasts[node.getVersion()].getLabel())) 
				continue;
			GROUMNode mappedNode = node.getMappedNode();
			if (node.getType() == GROUMNode.TYPE_FIELD) {
				if (node.isLiteral()) {
					add(lens, node, mappedNode);
				}
				else {
					ArrayList<GROUMNode> nexts = new ArrayList<>();
					int c = 0;
					for (GROUMEdge e : node.getOutEdges()) {
						GROUMNode n = e.getDest();
						if (n.getType() != GROUMNode.TYPE_FIELD) {
							if (n != mappedNode && nodes.contains(n)) {
								c++;
								if (c == 2)
									break;
							} else
								nexts.add(n);
						}
					}
					if (c == 1) {
						for (GROUMNode next : nexts) {
							add(lens, node, mappedNode, next);
						}
					} else if (c == 2) {
						add(lens, node, mappedNode);
					}
				}
			} else if (node.getType() == GROUMNode.TYPE_ACTION) {
				add(lens, node, mappedNode);
			} else if (node.getType() == GROUMNode.TYPE_CONTROL) {
				add(lens, node, mappedNode);
			}
		}
		checkConnectivity(lens);
		return lens;
	}

	private void checkConnectivity(HashMap<String, HashSet<ArrayList<GROUMNode>>> lens) {
		for (String label : lens.keySet()) {
			HashSet<ArrayList<GROUMNode>> ens = lens.get(label);
			for (ArrayList<GROUMNode> ns : new HashSet<ArrayList<GROUMNode>>(ens)) {
				HashSet<GROUMNode> oldNeighbors = new HashSet<>(), newNeighbors = new HashSet<>();
				for (GROUMNode n : ns) {
					if (n.getVersion() == 0) {
						oldNeighbors.addAll(n.getInNodes());
						oldNeighbors.addAll(n.getOutNodes());
					} else if (n.getVersion() == 1) {
						newNeighbors.addAll(n.getInNodes());
						newNeighbors.addAll(n.getOutNodes());
					}
				}
				boolean isConnected = true;
				if (!oldNeighbors.isEmpty()) {
					oldNeighbors.retainAll(oldNodes);
					isConnected = !oldNeighbors.isEmpty();
				}
				if (isConnected && !newNeighbors.isEmpty()) {
					newNeighbors.retainAll(newNodes);
					isConnected = !newNeighbors.isEmpty();
				}
				if (!isConnected)
					ens.remove(ns);
				else
					System.out.print("");
			}
		}
	}

	public boolean isAChange() {
		// TODO refine change pattern
		// is not a change if old and new nodes have same labels
		
		// add reference nodes
		// remove non-initialized definition nodes
		boolean hasGotoOnly = true;
		for (GROUMNode node : new HashSet<GROUMNode>(nodes)) {
			ArrayList<GROUMNode> refs = node.getReferences();
			if (!refs.isEmpty()) {
				for (GROUMNode ref : refs) {
					if (overlap(ref.getOutNodes(), nodes))
						nodes.add(ref);
				}
				if (!overlap(node.getInNodes(), nodes))
					nodes.remove(node);
			}
			if (node.isCoreAction() && !node.isGoto())
				hasGotoOnly = false;
		}
		if (hasGotoOnly)
			return false;
		boolean hasOld = false, hasNew = false;
		for (GROUMNode node : nodes) {
			if (!node.isCoreAction() && !node.isControl()) continue;
			if (node.getVersion() == 0) hasOld = true;
			else if (node.getVersion() == 1) hasNew = true;
			if (hasOld && hasNew) return true;
		}
		return false;
	}

	public int[] getMethodSizes() {
		int[] sizes = {0, 0};
		for(GROUMNode node : nodes)
			if(node.isCoreAction()) {
				sizes[node.getVersion()]++;
			}
		return sizes;
	}

	public int getNonDataSize() {
		int count = 0;
		for (GROUMNode node : nodes)
			if (node.getType() != GROUMNode.TYPE_FIELD)
				count++;
		return count;
	}
}
