package graphics;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import org.apache.commons.lang.SystemUtils;

import groum.GROUMNode;
import utils.FileIO;


public class DotGraph {
	public static final String SHAPE_BOX = "box";
	public static final String SHAPE_DIAMOND = "diamond";
	public static final String SHAPE_ELLIPSE = "ellipse";
	public static final String COLOR_BLACK = "black";
	public static final String COLOR_RED = "red";
	public static final String STYLE_ROUNDED = "rounded";
	public static final String STYLE_DOTTED = "dotted";
	public static final String WINDOWS_EXEC_DOT = "D:/Program Files (x86)/Graphviz2.36/bin/dot.exe";	// Windows
	public static final String MAC_EXEC_DOT = "dot";	// Mac
	public static final String LINUX_EXEC_DOT = null;	// Linux
	
	public static String EXEC_DOT = null;
	
	static {
		if (SystemUtils.IS_OS_WINDOWS)
			EXEC_DOT = WINDOWS_EXEC_DOT;
		else if (SystemUtils.IS_OS_MAC)
			EXEC_DOT = MAC_EXEC_DOT;
		else if (SystemUtils.IS_OS_LINUX)
			EXEC_DOT = LINUX_EXEC_DOT;
	}

	private StringBuilder graph = new StringBuilder();

	public DotGraph(StringBuilder sb) {
		this.graph = sb;
	}

	public String addStart() {
		return "digraph G {\n";
	}
	
	public String addSubgraphStart(String name) {
		return "subgraph " + name + " {\n";
	}
	
	public String addNode(int id, String[] names, String[] values) {
		StringBuffer buf = new StringBuffer();
		buf.append(id + " [" + names[0] + "=\"" + values[0] + "\"");
		for (int i = 1; i < names.length; i++) {
			String name = names[i], value = values[i];
			buf.append(" " + name + "=\"" + value + "\"");
		}
		buf.append("];\n");

		return buf.toString();
	}
	
	public String addEdge(int sId, int eId, String style, String color, String label) {
		StringBuffer buf = new StringBuffer();
		if(label == null)
			label = "";
		buf.append(sId + " -> " + eId + " [label=\"" + label + "\"");
		if(style != null && !style.isEmpty())
			buf.append(" style=" + style);
		if(color != null && !color.isEmpty())
			buf.append(" color=" + color);
		buf.append("];\n");

		return buf.toString();
	}
	
	public String addEnd() {
		return "}";
	}
	
	public String addSubgraphEnd() {
		return "}";
	}

	public String addSubgraphLabel(String label) {
		return "label = \"" + label + "\";\n";
	}
	
	public String addStyle(String style) {
		return "style=\"" + style + "\";\n";
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
		}
		catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void toGraphics(String file, String type) {
		if (EXEC_DOT == null) return;
		Runtime rt = Runtime.getRuntime();
		
		String[] args = {EXEC_DOT, "-T"+type, file+".dot", "-o", file+"."+type};
		try {
			Process p = rt.exec(args);
			p.waitFor();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void read(File file, HashMap<String, GROUMNode> oldNodes, HashMap<String, GROUMNode> newNodes) {
		if (!file.getName().endsWith(".dot")) 
			return;
		String content = FileIO.readStringFromFile(file.getAbsolutePath());
		Scanner sc = new Scanner(content);
		HashMap<String, GROUMNode> nodes = new HashMap<>();
		while (sc.hasNext()) {
			String line = sc.nextLine();
			if (line.equals("subgraph cluster0 {"))
				break;
		}
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			if (line.equals("}"))
				break;
			if (Character.isDigit(line.charAt(0))) {
				int index = line.indexOf(' ');
				String id = line.substring(0, index);
				HashMap<String, String> attributes = getAttributes(line.substring(index+1));
				GROUMNode node = new GROUMNode(id, 0, attributes);
				nodes.put(id, node);
				oldNodes.put(id, node);
			}
		}
		while (sc.hasNext()) {
			String line = sc.nextLine();
			if (line.equals("subgraph cluster1 {"))
				break;
		}
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			if (line.equals("}"))
				break;
			if (Character.isDigit(line.charAt(0))) {
				int index = line.indexOf(' ');
				String id = line.substring(0, index);
				HashMap<String, String> attributes = getAttributes(line.substring(index+1));
				GROUMNode node = new GROUMNode(id, 1, attributes);
				nodes.put(id, node);
				newNodes.put(id, node);
			}
		}
		String e = " -> ";
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			if (line.equals("}"))
				break;
			if (Character.isDigit(line.charAt(0))) {
				int index = line.indexOf(e);
				String id1 = line.substring(0, index), id2 = line.substring(index + e.length(), line.indexOf(" ", index + e.length()));
				HashMap<String, String> attributes = getAttributes(line.substring(index + e.length()));
				if (attributes.get("label").equals("_map_")) {
					oldNodes.get(id1).setChangeType(0);
					newNodes.get(id2).setChangeType(0);
				}
			}
		}
		sc.close();
	}

	private static HashMap<String, String> getAttributes(String line) {
		HashMap<String, String> attributes = new HashMap<>();
		int s = line.indexOf('['), e = line.indexOf(']');
		String[] parts = line.substring(s+1, e).split(" ");
		for (String part : parts) {
			int index = part.indexOf('=');
			String name = part.substring(0, index), value = part.substring(index+1);
			if (value.startsWith("\""))
				value = value.substring(1, value.length()-1);
			attributes.put(name, value);
		}
		return attributes;
	}
}
