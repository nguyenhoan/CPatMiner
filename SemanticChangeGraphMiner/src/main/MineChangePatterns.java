package main;

import change.ChangeGraph;
import groum.GROUMGraph;
import mining.Miner;
import mining.Pattern;

import org.apache.commons.lang.SystemUtils;

import utils.FileIO;
import utils.NotifyingBlockingThreadPoolExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MineChangePatterns {
	private static int THREAD_POOL_SIZE = 1;

	private static final Callable<Boolean> blockingTimeoutCallback = new Callable<Boolean>() {
		@Override
		public Boolean call() throws Exception {
			return true; // keep waiting
		}
	};
	private static NotifyingBlockingThreadPoolExecutor pool = new NotifyingBlockingThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 15, TimeUnit.SECONDS, 200, TimeUnit.MILLISECONDS, blockingTimeoutCallback);
	
	private static AtomicInteger numOfCommits = new AtomicInteger(0), numOfGraphs = new AtomicInteger(0);
	private static String changesPath ="";
	private static String reposPath = "";

	public static void main(String[] args) {
		long start = System.currentTimeMillis();
		changesPath = "contains a set of GitHub repos each of which is in the structure of username/reponame/*.dat."
				+ "E.g. inPath = repos-junit should contains junit-team/junit/*.dat"
				+ "or inPath = repos could contains junit-team/junit/*.dat and JetBrains/intellij-community/*.dat";
		if(SystemUtils.IS_OS_MAC){
//			changesPath = "/Users/hoanamzn/output/change graphs/Guru";
//			reposPath = "/Users/hoanamzn/eclipse-workspace/src";
            changesPath = "/Users/hoanamzn/output/change graphs/amazon-code";
            reposPath = "/Users/hoanamzn/Downloads/amazon code";
		} else if (SystemUtils.IS_OS_LINUX) {
			changesPath = "/home/hoan/github/change graphs/repos-99"; reposPath = "/home/hoan/github/repositories/java-stars-100";
		} else if (SystemUtils.IS_OS_WINDOWS){
			changesPath = "T:/change graphs/repos-5stars-50commits-fresh"; 
			reposPath = "E:/github/repos-5stars-50commits";
			
		}
		
		if (args.length > 0) {
			Pattern.mode = Integer.parseInt(args[0]);
			if (Pattern.mode == 0) {
				THREAD_POOL_SIZE = Integer.parseInt(args[1]);
				pool = new NotifyingBlockingThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 15, TimeUnit.SECONDS, 200, TimeUnit.MILLISECONDS, blockingTimeoutCallback);
			}
		}
		ArrayList<GROUMGraph> allGraphs = new ArrayList<>();
		HashSet<String> projectNames = new HashSet<String>();
		String content = null;
		if(SystemUtils.IS_OS_MAC){
			content = FileIO.readStringFromFile(reposPath + "/list.csv");
		} else if (SystemUtils.IS_OS_LINUX) {
			content = FileIO.readStringFromFile("/home/hoan/github/selected-repos.csv");
		} else if (SystemUtils.IS_OS_WINDOWS){
			content = FileIO.readStringFromFile(new File(reposPath).getParentFile().getAbsolutePath() + "/" + new File(reposPath).getName() + ".csv");
		}
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
//			if (projectNames.size() >= 8000)
//				break;
			String line = sc.nextLine();
			int index = line.indexOf(',');
			if (index == -1)
				index = line.length();
			final String name = line.substring(0, index);
			if (Pattern.mode == 0) {
				if (new File("output/patterns/" + name.replace("/", "---")).exists())
					continue;
				new File("output/patterns/" + name.replace("/", "---")).mkdirs();
				pool.execute(new Runnable() {
					@Override
					public void run() {
						ArrayList<GROUMGraph> graphs = readGraphs(changesPath, name);
						if (!graphs.isEmpty()) {
							projectNames.add(name);
							System.out.println("Project " + projectNames.size() + " " + name);
						}
						mine(graphs, 1, name.replace("/", "---"));
						System.out.println("Done " + name);
					}
				});
			}
			else {
				ArrayList<GROUMGraph> graphs = readGraphs(changesPath, name);
				if (!graphs.isEmpty()) {
					projectNames.add(name);
					System.out.println("Project " + projectNames.size() + " " + name);
				}
				allGraphs.addAll(graphs);
			}
		}
		sc.close();
		System.out.println("Projects: " + projectNames.size());
		System.out.println("Commits: " + numOfCommits);
		System.out.println("Graphs: " + numOfGraphs);
		if (Pattern.mode != 0) {
			File currDir = new File("output/patterns/" + new File(changesPath).getName() + (Pattern.mode == -1 ? "-hybrid" : "-cross"));
			currDir.mkdirs();
			mine(allGraphs, 1, currDir.getName());
		}
		System.out.println("Projects: " + projectNames.size());
		System.out.println("Commits: " + numOfCommits);
		System.out.println("Graphs: " + numOfGraphs);
		long end = System.currentTimeMillis();
		System.out.println((end - start) / 1000 + " s.");
		if (Pattern.mode == 0) {
			try {
				pool.await(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			} catch (final InterruptedException e) { }
		}
	}
	
	private static ArrayList<GROUMGraph> mine(ArrayList<GROUMGraph> graphs, int level, String currDir) {
		Miner miner = new Miner(level);
		miner.setCurrDir(currDir);
		if (level == 1) {
			return miner.mine(graphs, reposPath);
		} else {
			return miner.superMine(graphs);
		}
	}

	private static ArrayList<GROUMGraph> readGraphs(String changesPath, String projectName) {
		ArrayList<GROUMGraph> graphs = new ArrayList<>();
		File dir = new File(changesPath + "/" + projectName);
		if (!dir.exists())
			return graphs;
		File[] files = dir.listFiles();
		if (files == null)
			return graphs;
//		HashMap<String, Integer> commitTime = (HashMap<String, Integer>) FileIO.readObjectFromFile("T:/github/repos-metadata/" + projectName.replace("/", "---") + ".time");
//		Arrays.sort(files, new Comparator<File>() {
//			@Override
//			public int compare(File f1, File f2) {
//				String name1 = f1.getName();
//				if (name1.endsWith(".dat"))
//					name1 = name1.substring(0, name1.length() - 4);
//				else
//					return 1;
//				String name2 = f2.getName();
//				if (name2.endsWith(".dat"))
//					name2 = name2.substring(0, name2.length() - 4);
//				else
//					return -1;
//				return commitTime.get(name2) - commitTime.get(name1);
//			}
//		});
//        for (int i = 0; i < Math.min(files.length, 50); i++) {
        for (int i = 0; i < files.length; i++) {
			File sub = files[i];
			if (!sub.getName().endsWith(".dat")) continue;
			numOfCommits.incrementAndGet();
			//System.out.println("Commit " + i + ": " + file.getName());
			@SuppressWarnings("unchecked")
			HashMap<String, HashMap<String, ChangeGraph>> fileChangeGraphs = (HashMap<String, HashMap<String, ChangeGraph>>) FileIO.readObjectFromFile(sub.getAbsolutePath());
			for (String fp : fileChangeGraphs.keySet()) {
				//System.out.println(fp);
				HashMap<String, ChangeGraph> cgs = fileChangeGraphs.get(fp);
				for (String method : cgs.keySet()) {
					//System.out.println(method);
				    int index = sub.getName().indexOf('.');
				    if (index < 0) {
				        index = sub.getName().length();
				    }
					String name = FileIO.getSimpleFileName(sub.getName().substring(0, index)) + "," + fp + "," + method;
					ChangeGraph cg = cgs.get(method);
					if (cg.getNodes().size() <= 2) continue;
					numOfGraphs.incrementAndGet();
					GROUMGraph g = new GROUMGraph(cg, name);
					// FIXME
					g.pruneDoubleEdges();
					g.setProject(projectName);
					graphs.add(g);
					// DEBUG
					/*if (file.getName().equals("ddea0974608e74c284c65c82e18faa2ca02eb5d3.dat") && 
							fp.equals("platform/lang-impl/src/com/intellij/openapi/fileEditor/impl/text/TextEditorPsiDataProvider.java") &&
							method.equals("TextEditorPsiDataProvider,getData,#String#Editor#VirtualFile#")) {
						System.out.println(method);
						if (g.hasDuplicateEdge())
							System.out.print("");
						g.deleteAssignmentNodes();
						Fragment f = new Fragment(0, new ArrayList<>(g.getNodes()));
						//f.pruneClosure();
						f.toGraphics("D:/temp", "changegraph");
						System.out.print("");
					}*/
				}
			}
		}
		return graphs;
	}

}
