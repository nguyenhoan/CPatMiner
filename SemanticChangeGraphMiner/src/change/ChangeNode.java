package change;

import java.io.Serializable;
import java.util.ArrayList;

public class ChangeNode implements Serializable {
	private static final long serialVersionUID = 1416981239324616711L;
	public static final int STATUS_UNCHANGED = 0;
	public static final int STATUS_PARTLY_CHANGED = 1;
	public static final int STATUS_FULLY_CHANGED = 2;
	public static final int STATUS_RELABELED = 3;
	public static final int STATUS_DELETED = 4;
	public static final int STATUS_ADDED = 5;
	public static final int STATUS_MOVED = 6;
	
	private int astNodeType;
	private int changeType = -1;
	private int version = -1;
	private int[] starts, lengths;
	private String type, label;
	private String dataType, dataName;
	private ArrayList<ChangeEdge> inEdges = new ArrayList<>(), outEdges = new ArrayList<>();
	
	public int getAstNodeType() {
		return astNodeType;
	}

	public int getChangeType() {
		return changeType;
	}

	public int getVersion() {
		return version;
	}

	public int[] getStarts() {
		return starts;
	}

	public int[] getLengths() {
		return lengths;
	}

	public String getDataType() {
		return dataType;
	}

	public String getDataName() {
		return dataName;
	}

	public ArrayList<ChangeEdge> getInEdges() {
		return inEdges;
	}

	public ArrayList<ChangeEdge> getOutEdges() {
		return outEdges;
	}

	public String getType() {
		return type;
	}

	public String getLabel() {
		return label;
	}

	@Override
	public String toString() {
		return getLabel();
	}
	
	public static boolean isControl(String label) {
		return label.equals("CatchClause:CatchClause") 
				|| label.equals("ConditionalExpression:ConditionalExpression")
				|| label.equals("DoStatement:DoStatement")
				|| label.equals("EnhancedForStatement:EnhancedForStatement")
				|| label.equals("ForStatement:ForStatement")
				|| label.equals("IfStatement:IfStatement")
				|| label.equals("SwitchStatement:SwitchStatement")
				|| label.equals("SynchronizedStatement:SynchronizedStatement")
				|| label.equals("TryStatement:TryStatement")
				|| label.equals("WhileStatement:WhileStatement");
	}
}
