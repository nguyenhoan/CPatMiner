package main;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.SystemUtils;

import utils.FileIO;
import utils.NotifyingBlockingThreadPoolExecutor;
import change.ChangeAnalyzer;

public class MainChangeAnalyzer {
	private static int THREAD_POOL_SIZE = 1;

	private static final Callable<Boolean> blockingTimeoutCallback = new Callable<Boolean>() {
		@Override
		public Boolean call() throws Exception {
			return true; // keep waiting
		}
	};
	private static NotifyingBlockingThreadPoolExecutor pool = new NotifyingBlockingThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 15, TimeUnit.SECONDS, 200, TimeUnit.MILLISECONDS, blockingTimeoutCallback);
	
//	public static String inputPath = "F:/github/NayaraBorges/Pousada";
	public static String inputPath = "C:/repos/repos-IntelliJ", outputPath = "C:/change graphs/repos-IntelliJ";
//	public static String inputPath = "C:/repos/repos-junit", outputPath = "C:/change graphs/repos-junit";
//	public static String inputPath = "F:/testrepos/repos-test", outputPath = "F:/change graphs/repos-test";
//	public static String inputPath = "F:/github/testchange", outputPath = "D:/temp/testchanges";
//	public static String inputPath = "/Users/michaelhilton/Development/Research/GraphMinerData/junit";
//	public static String outputPath = "/Users/michaelhilton/Development/Research/GraphMinerData/output";
//	public static String inputPath = "G:/github/repos-bare", outputPath = "T:/change graphs/repos-99";
	
	public static void main(String[] args) {
		String content = null;
		if (SystemUtils.IS_OS_WINDOWS) {
			THREAD_POOL_SIZE = 8;
			pool = new NotifyingBlockingThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 15, TimeUnit.SECONDS, 200, TimeUnit.MILLISECONDS, blockingTimeoutCallback);
//			inputPath = "F:/github/repos-junit"; outputPath = "T:/change graphs/repos-junit";
//			content = FileIO.readStringFromFile("G:/github/repos-junit.csv");
			inputPath = "F:/github/repos-IntelliJ"; outputPath = "T:/change graphs/repos-IntelliJ";
			content = FileIO.readStringFromFile("G:/github/repos-IntelliJ.csv");
//			inputPath = "T:/github/repos-selected"; outputPath = "T:/change graphs/repos-selected";
//			inputPath = "E:/github/repos-5stars-50commits"; outputPath = "T:/change graphs/repos-5stars-50commits-fresh";
//			content = FileIO.readStringFromFile("E:/github/repos-5stars-50commits-fresh-remain.csv");
		} else if (SystemUtils.IS_OS_MAC) {
			THREAD_POOL_SIZE = 8;
			pool = new NotifyingBlockingThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 15, TimeUnit.SECONDS, 200, TimeUnit.MILLISECONDS, blockingTimeoutCallback);
//			inputPath = "/Users/hoanamzn/eclipse-workspace/src";
//			outputPath = "/Users/hoanamzn/output/change graphs/Guru";
//			content = FileIO.readStringFromFile(inputPath + "/guru.csv");
            inputPath = "/Users/hoanamzn/Downloads/amazon code";
            outputPath = "/Users/hoanamzn/output/change graphs/amazon-code";
            content = FileIO.readStringFromFile(inputPath + "/list.csv");
		} else if (SystemUtils.IS_OS_LINUX) {
			THREAD_POOL_SIZE = 24;
			pool = new NotifyingBlockingThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 15, TimeUnit.SECONDS, 200, TimeUnit.MILLISECONDS, blockingTimeoutCallback);
			inputPath = "/home/hoan/github/repositories/java-stars-100";
			outputPath = "/home/hoan/github/change graphs/repos-99";
			content = FileIO.readStringFromFile("/home/hoan/github/selected-repos.csv");
		}
		if (args.length > 0) {
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-i")) {
					inputPath = args[i+1];
				}
				if (args[i].equals("-o")) {
					outputPath = args[i+1];
				}
			}
		}
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			int index = line.indexOf(',');
			if (index < 0)
				index = line.length();
			String name = line.substring(0, index);
			File dir = new File(inputPath + "/" + name);
				analyze(dir, name);
		}
		sc.close();
		try {
			pool.await(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (final InterruptedException e) { }
	}

	private static void analyze(final File dir, final String name) {
		if (!dir.isDirectory())
			return;
		File git = new File(dir, ".git");
		if (git.exists()) {
			pool.execute(new Runnable() {
				@Override
				public void run() {
					long startProjectTime = System.currentTimeMillis();
					System.out.println(name);
					String url = dir.getAbsolutePath();
					ChangeAnalyzer ca = new ChangeAnalyzer(name, -1, url);
					ca.buildGitConnector();
					ca.analyzeGit();
			    	long endProjectTime = System.currentTimeMillis();
			    	ca.getCproject().setRunningTime(endProjectTime - startProjectTime);
			    	ca.closeGitConnector();
					System.out.println("Done " + name + " in " + (endProjectTime - startProjectTime) / 1000 + "s");
				}
			});
		}
	}

}
