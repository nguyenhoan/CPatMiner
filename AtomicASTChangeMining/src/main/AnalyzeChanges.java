package main;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import utils.FileIO;

public class AnalyzeChanges {

	public static void main(String[] args) {
		String inputPath = "/remote/rs/tien/hoan/change-graphs";
		final HashMap<String, Integer> sizes = new HashMap<String, Integer>();
		File dir = new File(inputPath);
		for (File ud : dir.listFiles()) {
			if (!ud.isDirectory())
				continue;
			System.out.println(ud.getName());
			for (File rd : ud.listFiles()) {
				if (!rd.isDirectory())
					continue;
				System.out.println(rd.getName());
				/*int c = 0;
				for (File f : rd.listFiles())
					if (f.isFile() && f.getName().endsWith(".dat"))
						c++;*/
				int c = rd.list().length - 1;
				sizes.put(ud.getName() + "/" + rd.getName(), c);
			}
		}
		ArrayList<String> l = new ArrayList<String>(sizes.keySet());
		Collections.sort(l, new Comparator<String>() {
			@Override
			public int compare(String s1, String s2) {
				return sizes.get(s2) - sizes.get(s1);
			}
		});
		System.out.println("Sorting");
		StringBuilder sb = new StringBuilder();
		for (String n : l)
			sb.append(n + "," + sizes.get(n) + "\n");
		FileIO.writeStringToFile(sb.toString(), dir.getParentFile().getAbsolutePath() + "/sizes.csv");
	}

}
