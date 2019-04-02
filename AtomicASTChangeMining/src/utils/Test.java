package utils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class Test {

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		String[] sa = new String[2];
		Arrays.fill(sa, "");
		double tmp = 0;
		boolean b = tmp < 0x1-52;
		HashMap<String, Integer> map = new HashMap<>();
		HashSet<String> set = new HashSet<>();
		set.add("a");
		set.add("b");
		JavaUtils.addAll(map, set);
		System.out.println();
		
		int i = 1 + (2 << 8) + (3 << 16) + (4 << 24);
		System.out.println(i);
		System.out.println((byte) (i >> 16));
		
		int change = -65504;
		System.out.println((byte) (change & 255));
		System.out.println((byte) ((change >> 8) & 255));
		System.out.println((byte) ((change >> 16) & 255));
		System.out.println((change >> 24) == 1);
		
		change = 16711712;
		System.out.println((byte) (change & 255));
		System.out.println((byte) ((change >> 8) & 255));
		System.out.println((byte) ((change >> 16) & 255));
		System.out.println((change >> 24) == 1);
		
		char ch = 65;
		String s = "" + ch;
		System.out.println(s);
	}

}
