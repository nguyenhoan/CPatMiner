/**
 * 
 */
package mining;

import groum.GROUMGraph;
import groum.GROUMNode;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jgit.api.errors.GitAPIException;
import utils.DirectoryHTML;
import utils.FileIO;
import utils.JGitUtil;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * @author hoan
 * 
 */
public class Miner {
	private static final String PAIR_SEPARATOR = "~";

	private int level = 0;
	private String reposPath;
	private String currDir;
	private HashMap<String, String> commitEmail = readEmail(new File("T:/github/repos-metadata/"));
	private HashMap<String, Integer> commitTime = readTime(new File("T:/github/repos-metadata/"));

	public ArrayList<Lattice> lattices = new ArrayList<Lattice>();
	
	public Miner(int level) {
		System.out.println("Mining level " + level);
		this.level = level;
	}

	public String getCurrDir()
		return currDir;
	}

	public void setCurrDir(String currDir) {
		this.currDir = currDir;
	}

	private static HashMap<String, String> readEmail(File dir) {
		HashMap<String, String> commitName = new HashMap<>();
		if (dir.exists()) {
    		for (File file : dir.listFiles()) {
    			if (file.getName().endsWith(".email")) {
    				HashMap<String, String> commitInfo = (HashMap<String, String>) FileIO.readObjectFromFile(file.getAbsolutePath());
    				commitName.putAll(commitInfo);
    			}
    		}
		}
		return commitName;
	}

	private static HashMap<String, Integer> readTime(File dir) {
		HashMap<String, Integer> commitName = new HashMap<>();
		if (dir.exists()) {
    		for (File file : dir.listFiles()) {
    			if (file.getName().endsWith(".time")) {
    				HashMap<String, Integer> commitInfo = (HashMap<String, Integer>) FileIO.readObjectFromFile(file.getAbsolutePath());
    				commitName.putAll(commitInfo);
    			}
    		}
		}
		return commitName;
	}

	public ArrayList<GROUMGraph> mine(ArrayList<GROUMGraph> groums, String reposPath) {
		this.reposPath = reposPath;
		for (GROUMGraph groum : groums) {
			groum.deleteAssignmentNodes();
			//groum.deleteUnaryOperationNodes();
			groum.collapseLiterals();
		}
		HashMap<String, HashSet<GROUMNode[]>> nodesOfLabel = new HashMap<>();
		for (GROUMGraph groum : groums) {
			for (GROUMNode node : groum.getNodes()) {
				if (node.getVersion() != 0) continue;
				GROUMNode mappedNode = node.getMappedNode();
				if (mappedNode == null) continue;
				String label = node.getLabel() + PAIR_SEPARATOR + mappedNode.getLabel();
				HashSet<GROUMNode[]> nodes = nodesOfLabel.get(label);
				if (nodes == null)
					nodes = new HashSet<>();
				nodes.add(new GROUMNode[]{node, mappedNode});
				nodesOfLabel.put(label, nodes);
			}
		}
		lattices.add(new Lattice());
		Lattice l = new Lattice();
		l.setStep(2);
		lattices.add(l);
		for (String label : new HashSet<String>(nodesOfLabel.keySet())) {
			HashSet<GROUMNode[]> nodes = nodesOfLabel.get(label);
			if (nodes.size() < Pattern.minFreq || !GROUMNode.isCoreAction(label.split(PAIR_SEPARATOR)[0]))
				nodesOfLabel.remove(label);
		}
		System.out.println("Got all first pairs");
		for (String label : nodesOfLabel.keySet()) {
			HashSet<GROUMNode[]> pairs = nodesOfLabel.get(label);
			HashSet<Fragment> fragments = new HashSet<>();
			for (GROUMNode[] pair : pairs) {
				Fragment f = new Fragment(pair);
				fragments.add(f);
			}
			Pattern p = new Pattern(fragments, fragments.size());
			extend(p);
		}
		System.out.println("Done mining level " + this.level);
		Lattice.filter(lattices);
		System.out.println("Done filtering level " + this.level);

		printOutResults();


		// Collect patterns mined from this level preparing for the next (super) pattern mining level
		ArrayList<GROUMGraph> patterns = new ArrayList<>();
		collectPatternsForNextStep(patterns);
		
		return patterns;
	}

	private void printOutResults() {
		//Data structure so we can build the navigation Table
		HashMap<Integer, ArrayList<ArrayList<String>>> foundPatterns = new HashMap<>();

		File dir = new File("output/patterns" + "/" + this.getCurrDir() + "/" + this.level);
		for (int step = Pattern.minSize; step <= lattices.size(); step++) {
			Lattice lat = lattices.get(step - 1);
			ArrayList<ArrayList<String>> patterns = new ArrayList<>();
			for (Pattern p : lat.getPatterns()) {
				printOutResults(dir, step, p, patterns, false);
			}
			//ListOfPatterns
			foundPatterns.put(step,patterns);
		}
//		System.out.println("Done reporting.");
//		System.out.println("Patterns:");
//		System.out.println(foundPatterns);
		if (dir.exists()) {
			DirectoryHTML d = new DirectoryHTML();
			d.write(foundPatterns, dir);
		}
	}

	private void printOutResults(File dir, int step, Pattern p, ArrayList<ArrayList<String>> patterns, boolean isAbstract) {
		File patternDir = new File(dir.getAbsolutePath() + "/" + step + "/"  + p.getId());
		if (!patternDir.exists())
			patternDir.mkdirs();
		Fragment rf = p.getRepresentative().extract();
		rf.pruneClosure();
		rf.toDot(patternDir.getAbsolutePath(), rf.getId() + "");
		StringBuilder sb = new StringBuilder();
		GROUMGraph representativeGraph = p.getRepresentative().getGraph();
		String name = representativeGraph.getName();

		sb.append("<html><h3>");
		sb.append(name + "\n");
		sb.append("</h3>");


		StringBuilder sampleChange = new StringBuilder();
		sampleChange.append("<link rel=\"stylesheet\" href=\"../../../../default.css\">\n" +
				"<script src=\"../../../../highlight.pack.js\"></script> \n" +
				"<script>hljs.initHighlightingOnLoad();</script>\n");
		sampleChange.append("<html><h3>");
		sampleChange.append(name + "\n");
		sampleChange.append("</h3>");

		/*sb.append("<img src='");
		sb.append(rf.getId()+".png");
		sb.append("'><BR><BR><BR>");*/


		ArrayList<String> beforeAndAfter = null;
		try {
			beforeAndAfter = JGitUtil.getFileFromDir(new File(reposPath + "/" + representativeGraph.getProject()), name);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}
		try { // FIXME
			sampleChange.append(writeDiffs(beforeAndAfter, rf));
		} catch (StringIndexOutOfBoundsException e) {
			return;
		}

		sb.append("<div id='inPattern'>In pattern: SUPERPATTERN</div><BR>");
		
		sb.append("<div id='frequency'>Frequency: " + p.getFreq() + "</div><BR>");
		sb.append("<div id='size'>Non-data size: " + rf.getNonDataSize() + "</div><BR>");
		
		sb.append("<h3>Instances</h3>");
		
		String projectName = representativeGraph.getProject();
		addFragmentsToHTML(sb, representativeGraph, name, projectName, p, isAbstract);

		for (Fragment f : p.getFragments()) {
			if (f == p.getRepresentative()) continue;
			GROUMGraph currGraph = f.getGraph();
			projectName = currGraph.getProject();
			name = currGraph.getName();
			addFragmentsToHTML(sb, currGraph, name, projectName, p, isAbstract);
		}


		FileIO.writeStringToFile(sampleChange.toString(),
				patternDir.getAbsolutePath() + "/sampleChange.html");

		FileIO.writeStringToFile(sb.toString(),
				patternDir.getAbsolutePath() + "/details.html");

		ArrayList<String> currPattern = new ArrayList<>();


		currPattern.add(Integer.toString(p.getId()));
		currPattern.add(Integer.toString(p.getSize()));
		currPattern.add(patternDir.getAbsolutePath().substring(patternDir.getParentFile().getParentFile().getAbsolutePath().length() + 1));
		currPattern.add(rf.getId() + "");
//		currPattern.add(String.valueOf(lat.getPatterns().get(0).getFragments().size()));
		currPattern.add(String.valueOf(p.getFragments().size()));
//		currPattern.add(listOfNodeTypes(lat.getPatterns().get(0).getRepresentative().getNodes()));
		currPattern.add(listOfNodeTypes(p.getRepresentative().getNodes()));
		patterns.add(currPattern);
	}

	private String writeDiffs(ArrayList<String> beforeAndAfter, Fragment rf) {
		ArrayList<ArrayList<Integer>> beforeHighlights = new ArrayList<>();
		ArrayList<ArrayList<Integer>> afterHighlights = new ArrayList<>();

		ArrayList<GROUMNode> nodes = rf.getNodes();
		for(GROUMNode node : nodes) {
			if(node.getStarts() == null){
//				System.out.println("NuLL");
			} else {
				if (node.getStarts().length > 0) {
					int i = 0;
					for (int start : node.getStarts()) {
						int end = node.getLengths()[i];
						int newHighlight = 1;
						if (node.getVersion() == 0) {
							newHighlight = getNewHighlight(start, end, newHighlight, beforeHighlights);
							if (newHighlight > 0) {
								ArrayList<Integer> newHighlightToAdd = new ArrayList<>();
								newHighlightToAdd.add(0, start);
								newHighlightToAdd.add(1, end);
								beforeHighlights.add(newHighlightToAdd);
							}
						} else {
							newHighlight = getNewHighlight(start, end, newHighlight, afterHighlights);
							if (newHighlight > 0) {
								ArrayList<Integer> newHighlightToAdd = new ArrayList<>();
								newHighlightToAdd.add(0, start);
								newHighlightToAdd.add(1, end);
								afterHighlights.add(newHighlightToAdd);
							}
						}
						i++;
					}
//					System.out.println("Before: " + beforeHighlights);
//					System.out.println("After: " + afterHighlights);
				}
			}
		}

		//TODO find first and last code change
		int beforeFirstChange = 999999999;
		int beforeLastChange = 0;

		int afterFirstChange = 999999999;
		int afterLastChange = 0;

		for(ArrayList<Integer> highlight : beforeHighlights ){
			if(highlight.get(0) < beforeFirstChange){
				beforeFirstChange = highlight.get(0);
			}
			int currEndPos = highlight.get(1) + highlight.get(0);
			if(currEndPos > beforeLastChange){
				beforeLastChange = currEndPos;
			}
		}

		for(ArrayList<Integer> highlight : afterHighlights ){
			if(highlight.get(0) < afterFirstChange){
				afterFirstChange = highlight.get(0);
			}
			int currEndPos = highlight.get(1) + highlight.get(0);
			if(currEndPos > afterLastChange){
				afterLastChange = currEndPos;
			}
		}
//		System.out.println("&&&&&&&&&&&&&&&&&&&&&&");
//		System.out.println(beforeAndAfter.get(0).substring(beforeFirstChange-200,beforeLastChange+200));
//		System.out.println("&&&&&&&&&&&&&&&&&&&&&&");

//		for(ArrayList<Integer> highlight : afterHighlights ){
//			System.out.println("*********************");
//			System.out.println(beforeAndAfter.get(0).substring(highlight.get(0),highlight.get(1)+highlight.get(0)));
//		}
//
//		for(ArrayList<Integer> highlight : beforeHighlights ){
//			System.out.println("*********************");
//			System.out.println(beforeAndAfter.get(1).substring(highlight.get(0),highlight.get(1)+highlight.get(0)));
//		}

		String afterStr = beforeAndAfter.get(0);
		String beforeStr = beforeAndAfter.get(1);

		String afterMarkup = markupCode(afterHighlights, afterStr);
		String beforeMarkup = markupCode(beforeHighlights, beforeStr);

		String markedupHTML = "<h3>Before Change</h3><pre><code class='java'>" + beforeMarkup + "</code></pre>";

		markedupHTML += "<h3>After Change</h3><pre><code class='java'>" + afterMarkup + "</code></pre>";


		return markedupHTML;
	}

	private String markupCode(ArrayList<ArrayList<Integer>> highlights, String str) {
		StringBuilder markedupString = new StringBuilder();
		Collections.sort(highlights, new Comparator<ArrayList<Integer>>() {
			@Override
			public int compare(ArrayList<Integer> l1, ArrayList<Integer> l2) {
				return l1.get(0).compareTo(l2.get(0));
			}
		});
		Object[] sortedArray = highlights.toArray();
		ArrayList<Integer> first = ((ArrayList<Integer>) sortedArray[0]);
		int fPos = first.get(0);
		for (int i = 0; i < 4; i++) {
			fPos = str.lastIndexOf('\n', fPos-1);
			if (fPos == -1) {
				fPos = 0;
				break;
			}
		}
		markedupString.append(str.substring(fPos, first.get(0)).replace("<","&lt;").replace(">","&gt;"));
		
		int end = -1;
		for (int i = 0; i < sortedArray.length-1; i++){
			ArrayList<Integer> al = (ArrayList<Integer>) sortedArray[i];
			if (al.get(0) + al.get(1) > end) {
				markedupString.append("<a id=\"change\">");
				markedupString.append(str.substring(Math.max(al.get(0), end), al.get(0) + al.get(1)).replace("<", "&lt;").replace(">", "&gt;"));
				markedupString.append("</a>");
			
				end = al.get(0) + al.get(1);
			}
			if (i < sortedArray.length){
				ArrayList<Integer> next = (ArrayList<Integer>) sortedArray[i+1];
				if (next.get(0) > end)
					markedupString.append(str.substring(end, next.get(0)).replace("<", "&lt;").replace(">", "&gt;"));
				else if (next.get(0) + next.get(1) > end)
					System.err.print(""); // DEBUG
			}
		}
		ArrayList<Integer> last = ((ArrayList<Integer>) sortedArray[sortedArray.length-1]);
		if (last.get(0) + last.get(1) > end) {
			markedupString.append("<a id=\"change\">");
			markedupString.append(str.substring(Math.max(last.get(0), end), last.get(0) + last.get(1)).replace("<", "&lt;").replace(">", "&gt;"));
			markedupString.append("</a>");
		
			end = last.get(0) + last.get(1);
		}
		int lPos = end;
		for (int i = 0; i < 4; i++) {
			lPos = str.indexOf('\n', lPos+1);
			if (lPos == -1) {
				lPos = str.length();
				break;
			}
		}
		markedupString.append(str.substring(end, lPos).replace("<", "&lt;").replace(">", "&gt;"));

		return String.valueOf(markedupString);
	}

	private int getNewHighlight(int start, int end, int newHighlight, ArrayList<ArrayList<Integer>> highlights) {
		for (ArrayList<Integer> highlight : highlights) {
            int hStart = highlight.get(0);
            int hEnd = hStart + highlight.get(1);
            if (start >= hStart && start <= hEnd) {
                if ((start + end) > hEnd) {
                    highlight.set(1, (start + end) - hStart);
                }
                newHighlight = 0;
            }
            if (end >= hStart && end <= hEnd) {
                if (start < hStart) {
                    highlight.set(0, start);
                }
                newHighlight = 0;
            }
        }
		return newHighlight;
	}

	private void addFragmentsToHTML(StringBuilder sb, GROUMGraph representativeGraph, String name, String projectName, Pattern p, boolean isAbstract) {
		String[] parts = name.split(",");
		byte[] thedigest = null;
		try {
			byte[] bytesOfMessage = parts[1].getBytes("UTF-8");
			MessageDigest md = MessageDigest.getInstance("MD5");
			thedigest = md.digest(bytesOfMessage);
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
			thedigest = null;
		}
		String md5s = parts[1];
		if (thedigest != null) {
	        StringBuffer sbDigest = new StringBuffer();
	        for (int i = 0; i < thedigest.length; ++i)
	          	sbDigest.append(Integer.toHexString((thedigest[i] & 0xFF) | 0x100).substring(1,3));
	        	md5s = sbDigest.toString();
		}
		String githubLocation = "https://github.com/" + projectName  + "/commit/" + parts[0] + "#diff-" + md5s + "L" + parts[5];
		
		sb.append("<BR>");
		sb.append("<div id='link'><a href='" + githubLocation + "' target='_blank'>Link</a></div>");
		sb.append("<div id='time'>" + this.commitTime.get(parts[0]) + "</div>");
		sb.append("<div id='author'>" + this.commitEmail.get(parts[0]) + "</div>");
		sb.append("<div id='method'>" + projectName + "," + name + "</div>");
		sb.append("<BR>");
		/*if(isAbstract || level > 1) {
            sb.append("<div id='fromPattern' > From pattern: <a href='../../../" + (isAbstract ? this.level : (this.level - 1)) + "/" + representativeGraph.getNodes().size() + "/" + representativeGraph.getPatternId() + "/details.html'>" + representativeGraph.getPatternId() + "<a></div><BR>");
			
			String pathToFile = "output/patterns" + "/" + this.getCurrDir() + "/" + (isAbstract ? this.level : (this.level - 1)) + "/" + representativeGraph.getNodes().size() + "/" + representativeGraph.getPatternId() + "/details.html";
			Path path = Paths.get(pathToFile);
			Charset charset = StandardCharsets.UTF_8;
			String link = "<a href='../../../" + level  + (isAbstract ? "-abstract"  : "") + "/" + p.getSize() + "/" + p.getId() + "/details.html'>" + p.getId() + "<a></div><BR>";
			String content = null;
			try {
				content = new String(Files.readAllBytes(path), charset);
				String Regex = "SUPERPATTERN";
				content = content.replaceAll(Regex, link);
				Files.write(path, content.getBytes(charset));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}*/
	}

	private void collectPatternsForNextStep(ArrayList<GROUMGraph> patterns) {
		//Data structure so we can build the navigation Table
		HashMap<Integer, ArrayList<ArrayList<String>>> foundPatterns = new HashMap<>();

		File dir = new File("output/patterns" + "/" + this.getCurrDir() + "/" + this.level + "-abstract");
		
		for (int step = Pattern.minSize; step <= lattices.size(); step++) {
			Lattice lat = lattices.get(step - 1);
			HashMap<String, HashSet<Fragment>> labelFragments = new HashMap<String, HashSet<Fragment>>(); 
			for (Pattern p : lat.getPatterns()) {
				Fragment f = p.getRepresentative();
				GROUMGraph g = new GROUMGraph(f);
				g.setPatternId(p.getId());
				patterns.add(g);
				if (level == 1) {
					ArrayList<GROUMNode> nodes = new ArrayList<GROUMNode>(g.getNodes());
					Collections.sort(nodes, new Comparator<GROUMNode>() {
						@Override
						public int compare(GROUMNode n1, GROUMNode n2) {
							return n1.getLabel().compareTo(n2.getLabel());
						}
					});
					Fragment af = new Fragment(nodes);
					af.setGraph(g);
					String label = nodes.toString();
					HashSet<Fragment> fragments = labelFragments.get(label);
					if (fragments == null) {
						fragments = new HashSet<Fragment>();
						labelFragments.put(label, fragments);
					}
					fragments.add(af);
				}
			}
			if (level == 1) {
				HashSet<HashSet<Fragment>> groups = new HashSet<>();
				for (String label : labelFragments.keySet()) {
					HashSet<Fragment> fragments = labelFragments.get(label);
					HashMap<Integer, HashSet<Fragment>> buckets = hash(fragments );
					for (int h : buckets.keySet()) {
						HashSet<Fragment> bucket = buckets.get(h);
						while (!bucket.isEmpty()) {
							Fragment f = null;
							for (Fragment fragment : bucket) {
								f = fragment;
								break;
							}
							HashSet<Fragment> group = new HashSet<>();
							group.add(f);
							bucket.remove(f);
							for (Fragment g : new HashSet<Fragment>(bucket)) {
								if (f.getVector().equals(g.getVector())) {
									group.add(g);
									bucket.remove(g);
								}
							}
							groups.add(group);
						}
						bucket.clear();
					}
					fragments.clear();
				}
				ArrayList<ArrayList<String>> patternInfo = new ArrayList<>();
				for (HashSet<Fragment> g : groups) {
					Pattern p = new Pattern(g, g.size());
					p.setId();
					printOutResults(dir, step, p, patternInfo, true);
				}
				//ListOfPatterns
				foundPatterns.put(step,patternInfo);
				labelFragments.clear();
			}
			lat.clear();
		}
		if (level == 1 && dir.exists()) {
			DirectoryHTML d = new DirectoryHTML();
			d.write(foundPatterns, dir);
		}
	}

	public ArrayList<GROUMGraph> superMine(ArrayList<GROUMGraph> groums) {
		Pattern.minFreq = 2;
		Pattern.mode = 0;
		HashMap<String, HashSet<GROUMNode[]>> nodesOfLabel = new HashMap<>();
		for (GROUMGraph groum : groums) {
			for (GROUMNode node : groum.getNodes()) {
				if (node.getVersion() != 0) continue;
				HashSet<GROUMNode> pairedNodes = node.getPairedNodes();
				for (GROUMNode pairedNode : pairedNodes) {
					GROUMNode mappedNode = pairedNode.getMappedNode();
					if (mappedNode != null && mappedNode != node)
						continue;
					String label = node.getLabel() + PAIR_SEPARATOR + pairedNode.getLabel();
					HashSet<GROUMNode[]> nodes = nodesOfLabel.get(label);
					if (nodes == null)
						nodes = new HashSet<>();
					nodes.add(new GROUMNode[]{node, pairedNode});
					nodesOfLabel.put(label, nodes);
				}
			}
		}
		lattices.add(new Lattice());
		Lattice l = new Lattice();
		l.setStep(2);
		lattices.add(l);
		for (String label : new HashSet<String>(nodesOfLabel.keySet())) {
			HashSet<GROUMNode[]> nodes = nodesOfLabel.get(label);
			String[] labels = label.split(PAIR_SEPARATOR);
			if (nodes.size() < Pattern.minFreq
					|| (!GROUMNode.isCoreAction(labels[0]) && !GROUMNode.isControl(labels[0]))
					|| (!GROUMNode.isCoreAction(labels[1]) && !GROUMNode.isControl(labels[1])))
				nodesOfLabel.remove(label);
		}
		System.out.println("Got all first pairs");
		for (String label : nodesOfLabel.keySet()) {
			HashSet<GROUMNode[]> pairs = nodesOfLabel.get(label);
			HashSet<Fragment> fragments = new HashSet<>();
			for (GROUMNode[] pair : pairs) {
				Fragment f = new Fragment(pair);
				fragments.add(f);
			}
			Pattern p = new Pattern(fragments, fragments.size());
			superExtend(p);
		}
		System.out.println("Done mining level " + this.level);
		Lattice.filter(lattices);
		System.out.println("Done filtering level " + this.level);


		printOutResults();

		// Collect patterns mined from this level preparing for the next (super) pattern mining lelvel
		ArrayList<GROUMGraph> patterns = new ArrayList<>();
		collectPatternsForNextStep(patterns);
		
		return patterns;
	}
	
	private String listOfNodeTypes(ArrayList<GROUMNode> nodesOfLabel) {
		Set<String> names  = new TreeSet<>();
		for (GROUMNode groumNode : nodesOfLabel) {
			names.add(ASTNode.nodeClassForType(groumNode.getAstType()).getSimpleName());
		}
		return names.toString();
	}
	
	private void superExtend(Pattern pattern) {
		HashMap<String, HashMap<Fragment, HashSet<ArrayList<GROUMNode>>>> labelFragmentExtendableNodes = new HashMap<>();
		for (Fragment f : pattern.getFragments()) {
			HashMap<String, HashSet<ArrayList<GROUMNode>>> xns = f.superExtend();
			for (String label : xns.keySet()) {
				HashMap<Fragment, HashSet<ArrayList<GROUMNode>>> fens = labelFragmentExtendableNodes.get(label);
				if (fens == null) {
					fens = new HashMap<>();
					labelFragmentExtendableNodes.put(label, fens);
				}
				fens.put(f, xns.get(label));
			}
		}
		for (String label : new HashSet<String>(labelFragmentExtendableNodes.keySet())) {
			HashMap<Fragment, HashSet<ArrayList<GROUMNode>>> fens = labelFragmentExtendableNodes.get(label);
			if (fens.size() < Pattern.minFreq)
				labelFragmentExtendableNodes.remove(label);
		}
		HashSet<Fragment> group = new HashSet<>();
		int xfreq = Pattern.minFreq - 1;
		for (String label : labelFragmentExtendableNodes.keySet()) {
			HashMap<Fragment, HashSet<ArrayList<GROUMNode>>> fens = labelFragmentExtendableNodes.get(label);
			HashSet<Fragment> xfs = new HashSet<>();
			for (Fragment f : fens.keySet()) {
				for (ArrayList<GROUMNode> ens : fens.get(f)) {
					Fragment xf = new Fragment(f, ens);
					xfs.add(xf);
				}
			}
			boolean isGiant = isGiant(xfs, pattern);
			//System.out.println("\tTrying with label " + label + ": " + xfs.size());
			HashSet<Fragment> g = new HashSet<>();
			int freq = mine(g, xfs, pattern, isGiant);
			if (freq > xfreq && !Lattice.containsAll(lattices, g)) {
				group = g;
				xfreq = freq;
			}
		}
		//System.out.println("Done trying all labels");
		if (xfreq >= Pattern.minFreq) {
			Pattern xp = new Pattern(group, xfreq);
			Fragment rep = null, xrep = null;
			for (Fragment f : group) {
				xrep = f;
				break;
			}
			for (Fragment f : pattern.getFragments()) {
				rep = f;
				break;
			}
			if (rep == null || xrep == null)
				throw new NullPointerException();
			/*ArrayList<String> labels = new ArrayList<>();
			for (int j = rep.getNodes().size(); j < xrep.getNodes().size(); j++) {
				GROUMNode node = xrep.getNodes().get(j);
				String label = node.getLabel();
				int type = node.getType();
				if (type == GROUMNode.TYPE_ACTION) {
					if (!node.isInvocation()) {
						if (label.length() == 1 && label.charAt(0) >= 128)
							label = ASTNode.nodeClassForType(node.getAstType()).getSimpleName() + ":" + ((char) (label.charAt(0) - 128));
						else
							label = ASTNode.nodeClassForType(node.getAstType()).getSimpleName();
					}
				} else if (label.length() == 1) {
					label = ASTNode.nodeClassForType(node.getAstType()).getSimpleName();
					char ch = label.charAt(0);
					if (ch >= 128)
						label += "*";
				}
				labels.add(label);
			}
			System.out.println("{Extending pattern of size " + rep.getNodes().size()
					+ " " + rep.getNodes()
					+ " occurences: " + pattern.getFragments().size()
					+ " frequency: " + pattern.getFreq()
					+ " with label " + labels
					+ " occurences: " + group.size()
					+ " frequency: " + xfreq
					+ " patterns: " + Pattern.nextID 
					+ " fragments: " + Fragment.numofFragments 
					+ " next fragment: " + Fragment.nextFragmentId);*/
			pattern.clear();
			superExtend(xp);
			//System.out.println("}");
		} else if (pattern.isAChange())
			pattern.add2Lattice(lattices);
	}
	
	private void extend(Pattern pattern) {
		HashMap<String, HashMap<Fragment, HashSet<ArrayList<GROUMNode>>>> labelFragmentExtendableNodes = new HashMap<>();
		for (Fragment f : pattern.getFragments()) {
			HashMap<String, HashSet<ArrayList<GROUMNode>>> xns = f.extend();
			for (String label : xns.keySet()) {
				HashMap<Fragment, HashSet<ArrayList<GROUMNode>>> fens = labelFragmentExtendableNodes.get(label);
				if (fens == null) {
					fens = new HashMap<>();
					labelFragmentExtendableNodes.put(label, fens);
				}
				fens.put(f, xns.get(label));
			}
		}
		for (String label : new HashSet<String>(labelFragmentExtendableNodes.keySet())) {
			HashMap<Fragment, HashSet<ArrayList<GROUMNode>>> fens = labelFragmentExtendableNodes.get(label);
			if (fens.size() < Pattern.minFreq)
				labelFragmentExtendableNodes.remove(label);
		}
		HashSet<Fragment> group = new HashSet<>();
		int xfreq = Pattern.minFreq - 1;
		for (String label : labelFragmentExtendableNodes.keySet()) {
			HashMap<Fragment, HashSet<ArrayList<GROUMNode>>> fens = labelFragmentExtendableNodes.get(label);
			HashSet<Fragment> xfs = new HashSet<>();
			for (Fragment f : fens.keySet()) {
				for (ArrayList<GROUMNode> ens : fens.get(f)) {
					Fragment xf = new Fragment(f, ens);
					xfs.add(xf);
				}
			}
			boolean isGiant = isGiant(xfs, pattern);
			//System.out.println("\tTrying with label " + label + ": " + xfs.size());
			HashSet<Fragment> g = new HashSet<>();
			int freq = mine(g, xfs, pattern, isGiant);
			if (freq > xfreq && !Lattice.containsAll(lattices, g)) {
				group = g;
				xfreq = freq;
			}
		}
		//System.out.println("Done trying all labels");
		if (xfreq >= Pattern.minFreq) {
			Pattern xp = new Pattern(group, xfreq);
			ArrayList<String> labels = new ArrayList<>();
			Fragment rep = null, xrep = null;
			for (Fragment f : group) {
				xrep = f;
				break;
			}
			for (Fragment f : pattern.getFragments()) {
				rep = f;
				break;
			}
			if (rep == null || xrep == null)
				throw new NullPointerException();
			for (int j = rep.getNodes().size(); j < xrep.getNodes().size(); j++)
				labels.add(xrep.getNodes().get(j).getLabel());
			/*System.out.println("{Extending pattern of size " + rep.getNodes().size()
					+ " " + rep.getNodes()
					+ " occurences: " + pattern.getFragments().size()
					+ " frequency: " + pattern.getFreq()
					+ " with label " + labels
					+ " occurences: " + group.size()
					+ " frequency: " + xfreq
					+ " patterns: " + Pattern.nextID 
					+ " fragments: " + Fragment.numofFragments 
					+ " next fragment: " + Fragment.nextFragmentId);*/
			pattern.clear();
			extend(xp);
			//System.out.println("}");
		} else if (pattern.isAChange())
			pattern.add2Lattice(lattices);
	}

	private boolean isGiant(HashSet<Fragment> xfs, Pattern pattern) {
		return pattern.getSize() > 1 
				&& (xfs.size() > Pattern.maxFreq || xfs.size() > pattern.getFragments().size() * pattern.getSize() * pattern.getSize());
	}

	private int mine(HashSet<Fragment> result, HashSet<Fragment> fragments, Pattern pattern, boolean isGiant) {
		HashSet<HashSet<Fragment>> groups = group(fragments);
		HashSet<Fragment> group = new HashSet<>();
		int xfreq = Pattern.minFreq - 1;
		for (HashSet<Fragment> g : groups) {
			int freq = computeFrequency(g, isGiant && isGiant(g, pattern));
			if (freq > xfreq) {
				group = g;
				xfreq = freq;
			}
		}
		result.addAll(group);
		return xfreq;
	}

	private HashSet<HashSet<Fragment>> group(HashSet<Fragment> fragments) {
		HashSet<HashSet<Fragment>> groups = new HashSet<>();
		HashMap<Integer, HashSet<Fragment>> buckets = hash(fragments);
		for (int h : buckets.keySet()) {
			HashSet<Fragment> bucket = buckets.get(h);
			group(groups, bucket);
		}
		return groups;
	}

	private HashMap<Integer, HashSet<Fragment>> hash(HashSet<Fragment> fragments) {
		HashMap<Integer, HashSet<Fragment>> buckets = new HashMap<Integer, HashSet<Fragment>>();
		for (Fragment f : fragments) {
			int h = f.getVectorHashCode();
			HashSet<Fragment> bucket = buckets.get(h);
			if (bucket == null) {
				bucket = new HashSet<>();
				buckets.put(h, bucket);
			}
			bucket.add(f);
		}
		return buckets;
	}

	private int computeFrequency(HashSet<Fragment> fragments, boolean isGiant) {
		HashSet<String> entities = new HashSet<>();
		HashMap<GROUMGraph, ArrayList<Fragment>> fragmentsOfGraph = new HashMap<GROUMGraph, ArrayList<Fragment>>();
		for (Fragment f : fragments) {
			GROUMGraph g = f.getGraph();
			if (Pattern.mode > 0) { // cross entities
				String entity = g.getProject(); // cross projects
				if (Pattern.mode == 1) // cross methods
					entity += "," + g.getName();
				else if (Pattern.mode == 2) { // cross commits
					String name = g.getName();
					entity += "," + name.substring(0, name.indexOf(','));
				}
				entities.add(entity);
			}
			ArrayList<Fragment> fs = fragmentsOfGraph.get(g);
			if (fs == null)
				fs = new ArrayList<Fragment>();
			fs.add(f);
			fragmentsOfGraph.put(g, fs);
		}
		int freq = 0;
		for (GROUMGraph g : fragmentsOfGraph.keySet()) {
			ArrayList<Fragment> fs = fragmentsOfGraph.get(g);
			int i = 0;
			while (i < fs.size()) {
				Fragment f = fs.get(i);
				int j = i + 1;
				while (j < fs.size()) {
					if (f.overlap(fs.get(j))) {
						if (isGiant)
							fragments.remove(fs.get(j));
						fs.remove(j);
					}
					else
						j++;
				}
				i++;
			}
			freq += i;
		}
		if (Pattern.mode > 0)
			return entities.size();
		return freq;
	}

	private void group(HashSet<HashSet<Fragment>> groups, HashSet<Fragment> bucket) {
		while (!bucket.isEmpty()) {
			Fragment f = null;
			for (Fragment fragment : bucket) {
				f = fragment;
				break;
			}
			group(f, groups, bucket);
		}
	}

	private void group(Fragment f, HashSet<HashSet<Fragment>> groups, HashSet<Fragment> bucket) {
		HashSet<Fragment> group = new HashSet<>();
		HashSet<Fragment> fs = new HashSet<>();
		group.add(f);
		fs.add(f.getGenFragmen());
		bucket.remove(f);
		f.setGenFragmen(null);
		for (Fragment g : new HashSet<Fragment>(bucket)) {
			if (f.getVector().equals(g.getVector())) {
				group.add(g);
				fs.add(g.getGenFragmen());
				bucket.remove(g);
				g.setGenFragmen(null);
			}
		}
		if (fs.size() >= Pattern.minFreq && group.size() >= Pattern.minFreq) {
			removeDuplicates(group);
			if (group.size() >= Pattern.minFreq)
				groups.add(group);
		}
	}

	private void removeDuplicates(HashSet<Fragment> group) {
		ArrayList<Fragment> l = new ArrayList<>(group);
		int i = 0;
		while (i < l.size() - 1) {
			Fragment f = l.get(i);
			int j = i + 1;
			while (j < l.size()) {
				if (f.isSameAs(l.get(j)))
					l.remove(j);
				else
					j++;
			}
			i++;
		}
		group.retainAll(l);
	}
}
