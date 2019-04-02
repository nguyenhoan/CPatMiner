package codemining;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class Group {
	public static int nextID = 1;

	public int id;
	public HashSet<Fragment> fragments = new HashSet<Fragment>();

	public Group(HashSet<Fragment> fragments) {
		this.id = nextID++;
		this.fragments = new HashSet<Fragment>(fragments);
	}

	@Override
	public int hashCode() {
		return this.id;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Group other = (Group) obj;
		if (id != other.id)
			return false;
		return true;
	}

	public void report(String path) {
		File dir = new File(path);
		if (!dir.exists())
			dir.mkdirs();
		dir = new File(path);
		if (!dir.exists())
			dir.mkdirs();
		try {
			BufferedWriter fout = new BufferedWriter(new FileWriter(
					dir.getAbsolutePath() + "\\"
							+ String.format("%03d", getFragmentSize()) + "_"
							+ String.format("%04d", this.fragments.size())
							+ "_" + String.format("%05d", this.id) + ".txt"));
			fout.write("Group " + String.format("%05d", this.id)
					+ " containing " + fragments.size() + " clones.\n");
			for (Fragment fr : this.fragments) {
				fout.write(fr.toString() + "\n");
			}
			fout.flush();
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private int getFragmentSize() {
		HashMap<Integer, Integer> sizes = new HashMap<>();
		for (Fragment f : this.fragments) {
			int size = f.getSize(), c = 1;
			if (sizes.containsKey(size))
				c = sizes.get(size) + 1;
			sizes.put(size, c);
		}
		int size = 0, count = 0;
		for (int s : sizes.keySet()) {
			int c = sizes.get(s);
			if (c > count) {
				size = s;
				count = c;
			}
		}
		return size;
	}
}