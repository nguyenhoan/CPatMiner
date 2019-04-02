package codemining;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;

public class Hash {
	// public static final int prime = 1048573; //20 bit
	public static final int prime = 16777213; // 24 bit
	public static final int max_bits = 100;
	public static final int max_uHash = 20;
	public static final int max_gHash = 512;
	public static final int max_vSize = 2500;
	public static Random randGaussian = new Random();
	// public static MRG32k3a randManhattan = new MRG32k3a();
	public static int[] ran;
	public static int vectorSize, uHashSize, gHashSize = max_gHash; // 9 bit
	public static double wSize;
	public static double b[][];
	public static double[][][] aGaussian;

	private Collection<? extends Fragment> fragments;
	HashMap<Integer, Bucket> buckets = new HashMap<>();

	/**
	 * This method will be used to initialize the hashing parameters
	 * 
	 * @param L
	 *            <= 512
	 * @param k
	 *            <= 20
	 * @param w
	 */
	public static void init(int gHash, int uHash, double windowSize, int vSize) {
		gHashSize = gHash;
		uHashSize = uHash;
		wSize = windowSize;
		vectorSize = vSize;
		randGaussian = new Random(prime);
		ran = new int[uHash];

		for (int i = 0; i < ran.length; i++)
			ran[i] = randomInt(0, prime);

		b = new double[gHash][uHash];
		aGaussian = new double[gHash][uHash][vSize];

		for (int i = 0; i < gHash; i++) {
			for (int j = 0; j < uHash; j++) {
				b[i][j] = randomDouble(0, windowSize);
				for (int l = 0; l < vSize; l++)
					aGaussian[i][j][l] = randGaussian.nextGaussian();
			}
		}
	}

	public static void reset(int L, int k, int vSize) {
		/*
		 * gHashSize = L; uHashSize = k;
		 */
		int vectorSize1 = vectorSize;
		vectorSize = vSize;
		if (vectorSize1 < vSize) {
			System.err.println("Vector size changed!");
			double[][][] a1 = aGaussian.clone();
			// aGaussian = new double[gHashSize][uHashSize][vectorSize];
			aGaussian = new double[max_gHash][max_uHash][vectorSize];

			for (int i = 0; i < max_gHash; i++) {
				for (int j = 0; j < max_uHash; j++) {
					for (int l = 0; l < vectorSize1; l++)
						aGaussian[i][j][l] = a1[i][j][l];
					for (int l = vectorSize1; l < vectorSize; l++)
						aGaussian[i][j][l] = randGaussian.nextGaussian();
				}
			}
		}
	}

	public static double getWSize() {
		return wSize;
	}

	public static void setWSize(double size) {
		wSize = size;
	}

	public int[] hashEuclidean(Fragment fragment) {
		int result[] = new int[gHashSize];
		for (int i = 0; i < result.length; i++) {
			result[i] = 0;
			for (int j = 0; j < Hash.uHashSize; j++)
				result[i] += Hash.ran[j] * hash(fragment.getVector(), i, j);
			result[i] %= Hash.prime;
			if (result[i] < 0)
				result[i] += Hash.prime;
			result[i] += i << 24;
		}
		return result;
	}

	public static double randomDouble(double minValue, double maxValue) {
		Random rand = new Random();
		return rand.nextDouble() * (maxValue - minValue) + minValue;
	}

	public static int randomInt(int minValue, int maxValue) {
		Random rand = new Random();
		return (int) Math.round(rand.nextDouble() * (maxValue - minValue)
				+ minValue);
	}

	public double dotProduct(double[] a, Map<? extends Feature, Integer> map) {
		double result = 0;
		for (Feature key : map.keySet())
			result += a[key.getId()] * map.get(key);
		return result;
	}

	public int hash(Map<? extends Feature, Integer> map, int hid, int lineID) {
		double dot = dotProduct(aGaussian[hid][lineID], map) + b[hid][lineID];
		return (int) Math.round(dot / Hash.wSize);
	}

	public void hash(Collection<? extends Fragment> fragments) {
		this.fragments = fragments;
		for (Fragment method : fragments) {
			int[] h = hashEuclidean(method);
			for (int i = 0; i < h.length; i++) {
				Bucket temp;
				if (buckets.containsKey(h[i]))
					temp = buckets.get(h[i]);
				else
					temp = new Bucket(h[i]);
				temp.addFragment(method);
				buckets.put(h[i], temp);
			}
		}
	}

	/**
	 * 
	 */
	public void filter() {
		HashSet<Bucket> cache = new HashSet<>();
		for (Fragment f : fragments) {
			HashSet<Bucket> bs = new HashSet<Bucket>(f.getBuckets());
			for (Bucket b : bs) {
				if (cache.contains(b))
					continue;
				if (b.isCovered(f.getBuckets())) {
					buckets.remove(b.hashcode);
					b.clear();
					b = null;
				} else
					cache.add(b);
			}
		}
	}

	public void clear() {
		for (Bucket b : buckets.values()) {
			b.clear();
			b = null;
		}
		buckets.clear();
		buckets = null;
	}

	@Override
	public String toString() {
		return "Hash scheme for " + gHashSize + " hash functions ";
	}
}
