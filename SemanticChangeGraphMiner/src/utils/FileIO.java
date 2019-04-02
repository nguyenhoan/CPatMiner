/**
 * 
 */
package utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jgit.api.errors.GitAPIException;

import groum.GROUMGraph;
import groum.GROUMNode;
import mining.Fragment;
import mining.Pattern;

/**
 * @author hoan
 *
 */
public class FileIO {
	public static final String outputDirPath = "D:/Subject systems/webpatterns/output";
	public static PrintStream logStream;
	static {
		try {
			logStream = new PrintStream(new FileOutputStream("log.txt"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public static String getSimpleFileName(String fileName)
	{
		char separator = '/';
		if(fileName.lastIndexOf('\\') > -1)
			separator = '\\';
		int start = fileName.lastIndexOf(separator) + 1;
		int end = fileName.lastIndexOf('.');
		if(end <= start)
			end = fileName.length();
		
		return fileName.substring(start, end);
	}
	
	/*public static String getSimpleClassName(String className)
	{
		String name = className.substring(className.lastIndexOf('.') + 1);
		int start = 0;
		while (start < name.length() && !Character.isJavaIdentifierPart(name.charAt(start)))
			start++;
		int end = name.length()-1;
		while (end >= 0 && !Character.isJavaIdentifierPart(name.charAt(end)))
			end--;
		if (end < start)
			return null;
		return name.substring(start, end+1);
	}*/
	public static String getSimpleClassName(String className)
	{
		String name = className.substring(className.lastIndexOf('.') + 1);
		return name;
	}
	
	public static String getSVNRepoRootName(String url)
	{
		String name = "";
		int end = url.length() - 1;
		while (url.charAt(end) == '/' && end >= 0)
			end--;
		if (end >= 0)
		{
			int start = url.lastIndexOf('/', end);
			if (start <= end)
				name = url.substring(start+1, end+1);
		}
		
		return name;
	}
	
	public static String[] splitFileName(String fileName)
	{
		char separator = '/';
		if(fileName.lastIndexOf('\\') > -1)
			separator = '\\';
		int start = fileName.lastIndexOf(separator) + 1;
		int end = fileName.lastIndexOf('.');
		if(end <= start)
			end = fileName.length() + 1;
		String[] names = new String[2];
		names[0] = fileName.substring(0, start-1);
		names[1] = fileName.substring(start, end);
		return names;
	}
	
	/*public static String getFileContent(File file)
	{
		StringBuffer strBuf = new StringBuffer();
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
	    	String line = "";
	    	while ((line = in.readLine()) != null) { 
	    		strBuf.append(line + "\r\n");
	    	}
	    	in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return strBuf.toString();
	}*/
	
	public static String readStringFromFile(String inputFile) {
		try {
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(inputFile));
			byte[] bytes = new byte[(int) new File(inputFile).length()];
			in.read(bytes);
			in.close();
			return new String(bytes);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void writeStringToFile(String string, String outputFile) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			writer.write(string);
			writer.flush();
			writer.close();
		}
		catch (Exception e) {
			/*e.printStackTrace();
			System.exit(0);*/
			System.err.println(e.getMessage());
		}
	}
	
	/*
	 * Read/write an object
	 */
	
	public static void writeObjectToFile(Object object, String objectFile, boolean append) {
		try {
			ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(objectFile, append)));
			out.writeObject(object);
			out.flush();
			out.close();
		}
		catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}
	
	public static Object readObjectFromFile(String objectFile) {
		try {
			ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(objectFile)));
			Object object = in.readObject();
			in.close();
			return object;
		}
		catch (Exception e) {
			//e.printStackTrace();
			return null;
		}
	}
	
	public static int countLOC(File file, String extension)
	{
		int numOfLines = 0;
		if(file.isDirectory())
		{
			for(File sub : file.listFiles())
				numOfLines += countLOC(sub, extension);
		}
		else if(file.getName().endsWith("." + extension))
		{
			try {
				BufferedReader in = new BufferedReader(new FileReader(file));
		    	while (in.readLine() != null) {
		    		numOfLines++;
		    	}
		    	in.close();
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return numOfLines;
	}
	
	public static String getHtmlPageContent(String url, String query, String charset) throws MalformedURLException, IOException
	{
		URLConnection connection = new URL(url + "?" + query).openConnection();
		connection.setRequestProperty("Accept-Charset", charset);
		//System.out.println(connection.getReadTimeout());
		connection.setReadTimeout(10000);
		InputStream response = connection.getInputStream();
		StringBuilder sb = new StringBuilder();
		BufferedInputStream in = new BufferedInputStream(response);
		byte[] bytes = new byte[10000];
		int len = in.read(bytes);
		while (len != -1)
		{
			//System.out.println(len);
			//System.out.println(new String(bytes, 0, len));
			sb.append(new String(bytes, 0, len));
			//Thread.sleep(100);
			len = in.read(bytes);
		}
		in.close();
		//System.out.println(len);
		//System.out.println(sb.toString());
		/*Scanner sc = new Scanner(response);
		while (sc.hasNextLine())
		{
			System.out.println(sc.nextLine());
		}*/
		/*BufferedReader in = new BufferedReader(new InputStreamReader(response));
		String inputLine;
        while ((inputLine = in.readLine()) != null)
        {
            System.out.println(inputLine);
            System.out.println("Here!!!");
        }
        in.close();*/
		return sb.toString();
	}
	
	public static ArrayList<String> getAllFilesInFolder(String folder) {
		ArrayList<String> allFiles = new ArrayList<String>();
		for (File file : new File(folder).listFiles()) {
			if (file.isFile())
			{
				System.out.println(file.getName() + ":" + file.length());
				allFiles.add(file.getPath());
			}
			else
				allFiles.addAll(getAllFilesInFolder(file.getPath()));
		}
		return allFiles;
	}

	public static int countLOC(String source) {
		int num = 0;
		for (int i = 0; i < source.length(); i++) {
			if (source.charAt(i) == '\n')
				num++;
		}	
		return num;
	}

	public static void printOutResults(File dir, int step, Pattern p, ArrayList<ArrayList<String>> patterns, boolean isAbstract, String reposPath) {
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

	private static String writeDiffs(ArrayList<String> beforeAndAfter, Fragment rf) {
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

	private static String markupCode(ArrayList<ArrayList<Integer>> highlights, String str) {
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

	private static int getNewHighlight(int start, int end, int newHighlight, ArrayList<ArrayList<Integer>> highlights) {
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

	private static void addFragmentsToHTML(StringBuilder sb, GROUMGraph representativeGraph, String name, String projectName,Pattern p, boolean isAbstract) {
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
		String githubLocation = "https://github.com/" + projectName  + "/commit/"
                + parts[0] + "#diff-" + md5s + "L" + parts[5];
		
		sb.append("<BR>");
		sb.append("<div id='link'><a href='" + githubLocation + "' target='_blank'>Link</a></div>");
//		sb.append("<div id='time'>" + this.commitTime.get(parts[0]) + "</div>");
//		sb.append("<div id='author'>" + this.commitEmail.get(parts[0]) + "</div>");
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
	
	private static String listOfNodeTypes(ArrayList<GROUMNode> nodesOfLabel) {
		Set<String> names  = new TreeSet<>();
		for (GROUMNode groumNode : nodesOfLabel) {
			names.add(ASTNode.nodeClassForType(groumNode.getAstType()).getSimpleName());
		}
		return names.toString();
	}
	
}
