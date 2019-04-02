package codemining;

import java.util.HashSet;
import java.util.HashMap;
import java.util.Set;

public class Bucket {
	int hashcode;
	Set<Fragment> fragments = new HashSet<>();

	public Bucket(int hashcode) {
		this.hashcode = hashcode;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (obj instanceof Bucket) {
			Bucket other = (Bucket) obj;
			return this.hashcode == other.hashcode;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.hashcode;
	}

	@Override
	public String toString() {
		return "Bucket: " + hashcode + ", fragments = " + fragments;
	}

	/**
	 * 
	 * @param fragment
	 */
	void addFragment(Fragment fragment) {
		this.fragments.add(fragment);
		if (fragment.getBuckets() == null)
			fragment.setBuckets(new HashSet<Bucket>());
		fragment.getBuckets().add(this);
	}

	/**
	 * 
	 * @param fragment
	 */
	void removeFragment(Fragment fragment) {
		fragment.getBuckets().remove(this);
		this.fragments.remove(fragment);
		if (fragment.getBuckets().isEmpty())
			fragment.setBuckets(null);
	}

	private boolean isDuplicate(Bucket b) {
		/*
		 * boolean dup = false; try { dup = b != null && this != b &&
		 * fragments.size() == b.fragments.size() &&
		 * b.fragments.containsAll(fragments); } catch (Exception e) {
		 * System.err.print((fragments == null) + "\t"); System.err.print((b ==
		 * null) + "\t"); System.err.println(b != null && b.fragments == null);
		 * 
		 * e.printStackTrace(); }
		 */
		return b != null && this != b && fragments.size() == b.fragments.size()
				&& b.fragments.containsAll(fragments);
	}

	private boolean isCovered(Bucket other) {
		return other != null && this != other
				&& other.fragments.containsAll(fragments);
	}

	boolean isCovered(Set<Bucket> buckets) {
		for (Bucket b : buckets)
			if (isCovered(b))
				return true;
		return false;
	}

	boolean isDuplicate(HashMap<Integer, Bucket> buckets) {
		for (Bucket b : buckets.values())
			if (isDuplicate(b))
				return true;
		return false;
	}

	/**
	 * 
	 */
	boolean isDuplicate(HashSet<Bucket> buckets) {
		for (Bucket b : buckets)
			if (isDuplicate(b))
				return true;
		return false;
	}

	/**
	 * 
	 */
	public void clear() {
		for (Fragment f : fragments)
			f.getBuckets().remove(this);
		fragments.clear();
		fragments = null;
	}
}
