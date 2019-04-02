package codemining;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class Grouper {
	private static final double threshold = 0;

	private Hash hash;
	private Set<Fragment> clones = new HashSet<Fragment>();

	public Grouper(Hash hash) {
		this.hash = hash;
	}

	public void pair() {
		for (Bucket b : hash.buckets.values()) {
			ArrayList<Fragment> fragments = new ArrayList<Fragment>(b.fragments);
			for (int i = 0; i < fragments.size() - 1; i++)
				for (int j = i + 1; j < fragments.size(); j++) {
					Fragment fi = fragments.get(i);
					Fragment fj = fragments.get(j);
					if (fi.getClones() == null || !fi.getClones().contains(fj)) {
						double distance = fi.distance(fj);
						if (distance <= threshold) {
							if (fi.getClones() == null)
								fi.setClones(new HashSet<Fragment>());
							fi.getClones().add(fj);
							if (fj.getClones() == null)
								fj.setClones(new HashSet<Fragment>());
							fj.getClones().add(fi);
							clones.add(fi);
							clones.add(fj);
						}
					}
				}
		}
	}

	public Set<Group> group() {
		Set<Group> groups = new HashSet<>();
		HashSet<Fragment> tmpClones = new HashSet<Fragment>(clones);
		while (!tmpClones.isEmpty()) {
			Fragment f = null;
			for (Fragment tf : tmpClones) {
				f = tf;
				break;
			}
			HashSet<Fragment> fs = new HashSet<>();
			Stack<Fragment> stack = new Stack<>();
			stack.push(f);
			// System.err.print("Start a group:");
			while (!stack.isEmpty()) {
				Fragment f1 = stack.pop();
				fs.add(f1);
				// System.err.print(" " + id1);
				tmpClones.remove(f1);
				for (Fragment f2 : f1.getClones())
					if (tmpClones.contains(f2))
						stack.push(f2);
			}
			// System.err.println();
			Group g = new Group(fs);
			groups.add(g);
		}

		return groups;
	}

	public void reportPairs(String path) {
		try {
			BufferedWriter fout = new BufferedWriter(new FileWriter(path));
			for (Fragment f : clones) {
				fout.write(f.getId() + ": ");
				for (Fragment cf : f.getClones()) {
					fout.write("\t" + cf.getId());
				}
				fout.write("\n");
			}
			fout.flush();
			fout.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
