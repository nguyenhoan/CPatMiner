package main;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;

import utils.FileIO;

public class AnalyzeCommitTime {

	public static void main(String[] args) {
		HashSet<String> names = new HashSet<>();
		File dir = new File("T:/github/repos-metadata/");
		for (File file : dir.listFiles()) {
			if (file.getName().endsWith(".time")) {
				HashMap<String, Integer> commitInfo = (HashMap<String, Integer>) FileIO.readObjectFromFile(file.getAbsolutePath());
				for (int time : commitInfo.values()) {
					long timestamp = time;
					Date date = new Date(timestamp * 1000);
				    String sdate = new SimpleDateFormat("yyyy-MM-dd").format(date);
				    if (sdate.compareTo("2017-02-01") >= 0) {
				    	names.add(file.getName());
				    	break;
				    }
				}
			}
		}
		System.out.println(names.size());
	}

}
