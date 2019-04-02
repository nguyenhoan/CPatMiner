package graphics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import change.ChangeEdge;
import change.ChangeGraph;
import change.ChangeNode;
import pdg.graph.PDGActionNode;
import pdg.graph.PDGControlEdge;
import pdg.graph.PDGControlNode;
import pdg.graph.PDGDataEdge;
import pdg.graph.PDGDataEdge.Type;
import pdg.graph.PDGDataNode;
import pdg.graph.PDGEntryNode;
import pdg.graph.PDGGraph;
import pdg.graph.PDGNode;
import pdg.graph.PDGEdge;

public class DotGraph {
	public static final String SHAPE_BOX = "box";
	public static final String SHAPE_DIAMOND = "diamond";
	public static final String SHAPE_ELLIPSE = "ellipse";
	public static final String COLOR_BLACK = "black";
	public static final String COLOR_RED = "red";
	public static final String STYLE_ROUNDED = "rounded";
	public static final String STYLE_DOTTED = "dotted";
	public static String EXEC_DOT = "D:/Program Files (x86)/Graphviz2.36/bin/dot.exe"; // Windows

	private StringBuilder graph = new StringBuilder();

	public DotGraph(StringBuilder sb) {
		this.graph = sb;
	}

	public DotGraph(ChangeGraph cg) {
		graph.append(addStart());
		
		HashMap<ChangeNode, Integer> ids = new HashMap<>();
		// add nodes
		int id = 0;
		for (ChangeNode node : cg.getNodes()) {
			ids.put(node, ++id);
			String color = null;
			String shape = null;
			if (node.getType().equals("a"))
				shape = SHAPE_BOX;
			else if (node.getType().equals("c"))
				shape = SHAPE_DIAMOND;
			else if (node.getType().equals("d"))
				shape = SHAPE_ELLIPSE;
			graph.append(addNode(id, node.getLabel(), shape, null, color, color));
		}
		//add edges
		for (ChangeNode node : cg.getNodes()) {
			int tId = ids.get(node);
			for (ChangeEdge e : node.getInEdges()) {
				int sId = ids.get(e.getSource());
				String label = e.getLabel();
				if (label.equals("T") || label.equals("F"))
					graph.append(addEdge(sId, tId, null, null, label));
				else
					graph.append(addEdge(sId, tId, STYLE_DOTTED, null, label));
			}
		}

		graph.append(addEnd());
	}

	public DotGraph(PDGGraph pdg, boolean changeOnly) {
		graph.append(addStart());
		
		HashMap<PDGNode, Integer> ids = new HashMap<PDGNode, Integer>();
		// add nodes
		int id = 0;
		for (PDGNode node : pdg.getNodes()) {
			if (changeOnly && !pdg.isChangedNode(node)) continue;
			id++;
			ids.put(node, id);
			String color = null;
			if (pdg.isChangedNode(node))
				color = COLOR_RED;
			if (node instanceof PDGEntryNode)
				graph.append(addNode(id, node.getLabel(), SHAPE_ELLIPSE,
						STYLE_DOTTED, color, color));
			else if (node instanceof PDGControlNode)
				graph.append(addNode(id, node.getLabel(), SHAPE_DIAMOND, null,
						color, color));
			else if (node instanceof PDGActionNode)
				graph.append(addNode(id, node.getLabel(), SHAPE_BOX, null,
						color, color));
			else if (node instanceof PDGDataNode)
				graph.append(addNode(id, node.getLabel(), SHAPE_ELLIPSE, null,
						color, color));
		}
		// add edges
		for (PDGNode node : pdg.getNodes()) {
			if (!ids.containsKey(node)) continue;
			int tId = ids.get(node);
			HashMap<String, Integer> numOfEdges = new HashMap<>();
			for (PDGEdge e : node.getInEdges()) {
				if (!ids.containsKey(e.getSource())) continue;
				int sId = ids.get(e.getSource());
				String label = e.getLabel();
				if (e instanceof PDGDataEdge) {
					//if (e.getTarget() instanceof PDGEntryNode || ((PDGDataEdge) e).getType() != Type.DEPENDENCE)
					{
						int n = 1;
						if (numOfEdges.containsKey(label))
							n += numOfEdges.get(label);
						numOfEdges.put(label, n);
						graph.append(addEdge(sId, tId, STYLE_DOTTED, null,
								label + (((PDGDataEdge) e).getType() == Type.PARAMETER ? n : "")));
					}
				} else if (e instanceof PDGControlEdge) {
					PDGNode s = e.getSource();
					if (s instanceof PDGEntryNode || s instanceof PDGControlNode) {
						int n = 0;
						for (PDGEdge out : s.getOutEdges()) {
							if (out.getLabel().equals(label))
								n++;
							if (out == e)
								break;
						}
						graph.append(addEdge(sId, tId, null, null, label + n));
					} else
						graph.append(addEdge(sId, tId, null, null, label));
				} else
					graph.append(addEdge(sId, tId, null, null, label));
			}
		}

		graph.append(addEnd());
	}

	public String addStart() {
		return "digraph G {\n";
	}

	public String addNode(int id, String label, String shape, String style,
			String borderColor, String fontColor) {
		StringBuffer buf = new StringBuffer();
		buf.append(id + " [label=\"" + label + "\"");
		if (shape != null && !shape.isEmpty())
			buf.append(" shape=" + shape);
		if (style != null && !style.isEmpty())
			buf.append(" style=" + style);
		if (borderColor != null && !borderColor.isEmpty())
			buf.append(" color=" + borderColor);
		if (fontColor != null && !fontColor.isEmpty())
			buf.append(" fontcolor=" + fontColor);
		buf.append("]\n");

		return buf.toString();
	}

	public String addEdge(int sId, int eId, String style, String color,
			String label) {
		StringBuffer buf = new StringBuffer();
		if (label == null)
			label = "";
		buf.append(sId + " -> " + eId + " [label=\"" + label + "\"");
		if (style != null && !style.isEmpty())
			buf.append(" style=" + style);
		if (color != null && !color.isEmpty())
			buf.append(" color=" + color);
		buf.append("];\n");

		return buf.toString();
	}

	public String addEnd() {
		return "}";
	}

	public String getGraph() {
		return this.graph.toString();
	}

	public void toDotFile(File file) {
		try {
			BufferedWriter fout = new BufferedWriter(new FileWriter(file));
			fout.append(this.graph.toString());
			fout.flush();
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void toGraphics(String file, String type) {
		Runtime rt = Runtime.getRuntime();

		String[] args = { EXEC_DOT, "-T" + type, file + ".dot", "-o",
				file + "." + type };
		try {
			Process p = rt.exec(args);
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
