package utils;

public class StringUtils {
	public static String[] tokenize(String s) {
		return s.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])");
	}
}
