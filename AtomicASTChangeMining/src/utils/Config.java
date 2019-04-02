package utils;

public class Config {
	public static final boolean INTER_PROCEDURAL = false;
	public final static boolean BUILD_VECTOR = true;
	public static final boolean countChangeFileOnly = false;
	public static String repetitionsPath = "D:/changeanalysis";
	public static String svnRootPath = "F:/sourceforge";
	public static final String FIXREF_SUBJECT_SYSTEM_ROOT_PATH = "D:\\Subject systems\\fixref";
	public static final String FIXREF_SUBJECT_FEATURE_ROOT_PATH = FIXREF_SUBJECT_SYSTEM_ROOT_PATH + "\\features";
	public static final String SCMiner_SUBJECT_SYSTEM_ROOT_PATH = "D:\\Subject systems\\scminer";
	public static final String SCMiner_SUBJECT_FEATURE_ROOT_PATH = SCMiner_SUBJECT_SYSTEM_ROOT_PATH + "\\features";
	public static String outputDirPath = "D:/Subject systems/output";
	public static final int MAX_EXTRACTED_COMMITS = Integer.MAX_VALUE;
}
