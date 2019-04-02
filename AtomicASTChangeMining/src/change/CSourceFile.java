package change;

import java.io.Serializable;

public class CSourceFile implements Serializable {
	private static final long serialVersionUID = 619711333136572459L;

	private int numOfLOCs = 0;
	private String path;

	public CSourceFile(String path, int numOfLOCS) {
		this.path = path;
		this.numOfLOCs = numOfLOCS;
	}

	public String getPath() {
		return path;
	}

	public int getNumOfLOCs() {
		return numOfLOCs;
	}

}
