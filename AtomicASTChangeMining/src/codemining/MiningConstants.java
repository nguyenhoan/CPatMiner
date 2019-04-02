package codemining;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;

public class MiningConstants {
	public static final int NUM_CODE_REVISIONS = 1000;
	public static int MAX_NUM_CHANGES = 1000000;
	public static int MAX_NUM_THREADS = 64;
	public static File listDir = new File("/remote/rs/tien");
	public static HashMap<String, HashSet<Integer>> projectFixingRevisions = new HashMap<>();
	public static String listNumber = "";
	public static int numOfPatterns = 0;
	public static long numOfLocations = 0;
	public static int PART_SIZE = 100;
}
