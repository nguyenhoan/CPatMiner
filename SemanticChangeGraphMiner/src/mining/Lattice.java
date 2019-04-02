package mining;

import java.util.ArrayList;
import java.util.HashSet;
/**
 * @author Nguyen Anh Hoan
 *
 */
public class Lattice {
	private int step;
	private ArrayList<Pattern> patterns = new ArrayList<Pattern>();

	public int getStep() {
		return step;
	}

	public void setStep(int step) {
		this.step = step;
	}

	public ArrayList<Pattern> getPatterns() {
		return patterns;
	}
	
	public void add(Pattern p) {
		patterns.add(p);
	}
	
	public void remove(Pattern p) {
		patterns.remove(p);
	}
	
	private boolean containsAll(HashSet<Fragment> g) {
		for (Pattern p : patterns)
			if (p.containsAll(g))
				return true;
		return false;
	}
	
	public static void filter(ArrayList<Lattice> lattices) {
		for (int size = Pattern.minSize-1; size < lattices.size(); size++) {
			Lattice l1 = lattices.get(size);
			for (Pattern p1 : new ArrayList<Pattern>(l1.getPatterns())) {
				boolean found = false;
				for (int i = size; i < lattices.size() - 1; i++) {
					Lattice l2 = lattices.get(i);
					for (Pattern p2 : l2.getPatterns()) {
						if (p2 != p1 && p2.containsOne(p1)) {
							l1.remove(p1);
							found = true;
							break;
						}
					}
					if (found) break;
				}
			}
		}
	}

	public static boolean containsAll(ArrayList<Lattice> lattices, HashSet<Fragment> g) {
		int size = 0;
		for (Fragment f : g) {
			size = f.getNodes().size();
			break;
		}
		for (int i =  lattices.size() - 1; i >= size-1; i--) {
			Lattice l = lattices.get(i);
			if (l.containsAll(g))
				return true;
		}
		return false;
	}

	public void clear() {
		for (Pattern p : this.patterns)
			p.clear();
		this.patterns.clear();
	}
}
