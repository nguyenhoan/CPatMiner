package main;

import java.io.File;

import graphics.DotGraph;

public class GenerateImageFromDotFile {

	public static void main(String[] args) {
		File dir = new File("D:/Projects/GraphMiner/output/patterns/1460496481");
		for (File level : dir.listFiles()) {
			for (File size : level.listFiles()) {
				if (size.isDirectory()) {
					for (File p : size.listFiles()) {
						for (File file : p.listFiles()) {
							if (file.getName().endsWith(".dot")) {
								String name = file.getName().substring(0, file.getName().length() - ".dot".length());
								File image = new File(p, name + ".png");
								if (!image.exists()) {
									DotGraph.toGraphics(p.getAbsolutePath() + "/" + name, "png");
								}
							}
						}
					}
				}
			}
		}
	}

}
