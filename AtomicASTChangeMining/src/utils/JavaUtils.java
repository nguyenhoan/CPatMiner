package utils;

import java.util.HashMap;
import java.util.HashSet;

public class JavaUtils {

	public static <K> void addAll(HashMap<K, Integer> map, HashSet<K> set) {
		for (K k : set) {
			int c = 1;
			if (map.containsKey(k))
				c += map.get(k);
			map.put(k, c);
		}
	}

	public static <K> void removeAll(HashMap<K, Integer> map1, HashMap<K, Integer> map2) {
		HashSet<K> keys = new HashSet<>(map1.keySet());
		keys.retainAll(map2.keySet());
		for (K key : keys) {
			int c1 = map1.get(key), c2 = map2.get(key);
			if (c1 > c2)
				map1.put(key, c1 - c2);
			else
				map1.remove(key);
		}
	}

	public static <K, V> void addAll(HashMap<K, HashSet<V>> map, HashMap<K, HashSet<V>> other) {
		for (K k : other.keySet()) {
			HashSet<V> values = map.get(k);
			if (values == null) {
				values = new HashSet<>();
				map.put(k, values);
			}
			values.addAll(other.get(k));
		}
	}
	
}
