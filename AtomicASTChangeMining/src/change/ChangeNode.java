package change;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.jdt.core.dom.*;
import pdg.graph.PDGActionNode;
import pdg.graph.PDGControlNode;
import pdg.graph.PDGDataNode;
import pdg.graph.PDGNode;
import treed.TreedConstants;

public class ChangeNode implements Serializable {
	private static final long serialVersionUID = 1416981239324616711L;

	int astNodeType;
	int changeType = -1;
	int version = -1;
	int[] starts, lengths;
	String type, label;
	String dataType, dataName;
	ArrayList<ChangeEdge> inEdges = new ArrayList<>(), outEdges = new ArrayList<>();

	public ChangeNode(PDGNode node) {
		this.astNodeType = node.getAstNodeType();
		this.version = node.version;
		if (node.getAstNode() != null) {
			setPositionInfo(node.getAstNode());
			if(node.getAstNode().getProperty(TreedConstants.PROPERTY_STATUS) != null)
				this.changeType = (int) node.getAstNode().getProperty(TreedConstants.PROPERTY_STATUS);
		}
		if (node instanceof PDGActionNode)
			this.type = "a";
		else if (node instanceof PDGControlNode)
			this.type = "c";
		else if (node instanceof PDGDataNode)
			this.type = "d";
		this.label = node.getExasLabel();
		this.dataName = node.getDataName();
		this.dataType = node.getDataType();
	}

	private void setPositionInfo(ArrayAccess astNode) {
		starts = new int[2];
		lengths = new int[2];
		starts[0] = astNode.getStartPosition();
		lengths[0] = astNode.getIndex().getStartPosition() - starts[0];
		starts[1] = astNode.getIndex().getStartPosition() + astNode.getIndex().getLength();
		lengths[1] = astNode.getStartPosition() + astNode.getLength() - starts[1];
	}

	private void setPositionInfo(ArrayCreation astNode) {
		ArrayType at = astNode.getType();
		starts = new int[1];
		lengths = new int[1];
		starts[0] = astNode.getStartPosition();
		lengths[0] = at.getStartPosition() + at.getLength() - starts[0];
	}

	private void setPositionInfo(ArrayInitializer astNode) {
		if (astNode.expressions().isEmpty()) {
			starts = new int[1];
			lengths = new int[1];
			starts[0] = astNode.getStartPosition();
			lengths[0] = astNode.getLength();
		} else {
			starts = new int[2];
			lengths = new int[2];
			starts[0] = astNode.getStartPosition();
			ASTNode e = (ASTNode) astNode.expressions().get(0);
			lengths[0] = e.getStartPosition() - starts[0];
			e = (ASTNode) astNode.expressions().get(astNode.expressions().size() - 1);
			starts[1] = e.getStartPosition() + e.getLength();
			lengths[1] = astNode.getStartPosition() + astNode.getLength() - starts[1];
		}
	}

	private void setPositionInfo(AssertStatement astNode) {
		starts = new int[] {astNode.getStartPosition()};
		lengths = new int[] {astNode.getExpression().getStartPosition() - starts[0]};
	}

	private void setPositionInfo(Assignment astNode) {
		Expression e = astNode.getLeftHandSide();
		starts = new int[]{e.getStartPosition() + e.getLength()};
		e = astNode.getRightHandSide();
		lengths = new int[]{e.getStartPosition() - starts[0]};
	}

	private void setPositionInfo(BooleanLiteral astNode) {
		starts = new int[] {astNode.getStartPosition()};
		lengths = new int[] {astNode.getLength()};
	}

	private void setPositionInfo(BreakStatement astNode) {
		starts = new int[] {astNode.getStartPosition()};
		lengths = new int[] {astNode.getLabel() == null ? astNode.getLength() : astNode.getLabel().getStartPosition() - starts[0]};
	}

	private void setPositionInfo(CastExpression astNode) {
		starts = new int[] {astNode.getStartPosition()};
		lengths = new int[] {astNode.getExpression().getStartPosition() - starts[0]};
	}

	private void setPositionInfo(CatchClause astNode) {
		starts = new int[2];
		lengths = new int[2];
		starts[0] = astNode.getStartPosition();
		lengths[0] = astNode.getException().getStartPosition() - starts[0];
		starts[1] = astNode.getException().getStartPosition() + astNode.getException().getLength();
		lengths[1] = astNode.getBody().getStartPosition() - starts[1];
	}

	private void setPositionInfo(CharacterLiteral astNode) {
		starts = new int[] {astNode.getStartPosition()};
		lengths = new int[] {astNode.getLength()};
	}

	private void setPositionInfo(ClassInstanceCreation astNode) {
		starts = new int[astNode.arguments().isEmpty() ? 1 : 2];
		lengths = new int[starts.length];
		Expression e = astNode.getExpression();
		if (e == null)
			starts[0] = astNode.getStartPosition();
		else 
			starts[0] = e.getStartPosition() + e.getLength();
		AnonymousClassDeclaration acd = astNode.getAnonymousClassDeclaration();
		if (astNode.arguments().isEmpty()) {
			lengths[0] = (acd == null ? astNode.getStartPosition() + astNode.getLength() : acd.getStartPosition()) - starts[0];
		} else {
			e = (Expression) astNode.arguments().get(0);
			lengths[0] = e.getStartPosition() - starts[0];
			e = (Expression) astNode.arguments().get(astNode.arguments().size() - 1);
			starts[1] = e.getStartPosition() + e.getLength();
			lengths[1] = (acd == null ? astNode.getStartPosition() + astNode.getLength() : acd.getStartPosition()) - starts[1];
		}
	}

	private void setPositionInfo(ConditionalExpression astNode) {
		starts = new int[] {astNode.getExpression().getStartPosition() + astNode.getExpression().getLength()};
		lengths = new int[] {astNode.getThenExpression().getStartPosition() - starts[0]};
	}

	private void setPositionInfo(ConstructorInvocation astNode) {
		starts = new int[astNode.arguments().isEmpty() ? 1 : 2];
		lengths = new int[starts.length];
		starts[0] = astNode.getStartPosition();
		if (astNode.arguments().isEmpty()) {
			lengths[0] = astNode.getStartPosition() + astNode.getLength() - starts[0];
		} else {
			Expression e = (Expression) astNode.arguments().get(0);
			lengths[0] = e.getStartPosition() - starts[0];
			e = (Expression) astNode.arguments().get(astNode.arguments().size() - 1);
			starts[1] = e.getStartPosition() + e.getLength();
			lengths[1] = astNode.getStartPosition() + astNode.getLength() - starts[1];
		}
	}

	private void setPositionInfo(ContinueStatement astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getLabel() == null ? astNode.getLength() : astNode.getLabel().getStartPosition() - starts[0]};
	}

	private void setPositionInfo(DoStatement astNode) {
		starts = new int[] {astNode.getStartPosition()};
		lengths = new int[] {astNode.getBody().getStartPosition() - starts[0]};
	}

	private void setPositionInfo(EnhancedForStatement astNode) {
		starts = new int[2];
		lengths = new int[2];
		starts[0] = astNode.getStartPosition();
		lengths[0] = astNode.getParameter().getStartPosition() - starts[0];
		starts[1] = astNode.getExpression().getStartPosition() + astNode.getExpression().getLength();
		lengths[1] = astNode.getBody().getStartPosition() - starts[1];
	}

	private void setPositionInfo(FieldAccess astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getLength()};
	}

	private void setPositionInfo(ForStatement astNode) {
		starts = new int[2];
		lengths = new int[2];
		starts[0] = astNode.getStartPosition();
		if (!astNode.initializers().isEmpty()) {
			lengths[0] = ((ASTNode) astNode.initializers().get(0)).getStartPosition() - starts[0];
		} else if (astNode.getExpression() != null) {
			lengths[0] = astNode.getExpression().getStartPosition() - starts[0];
		} else if (!astNode.updaters().isEmpty()) {
			lengths[0] = ((ASTNode) astNode.updaters().get(0)).getStartPosition() - starts[0];
		} else {
			starts = new int[] {astNode.getStartPosition()};
			lengths = new int[] {astNode.getBody().getStartPosition() - starts[0]};
			return;
		}
		if (!astNode.updaters().isEmpty()) {
			Expression e = (Expression) astNode.updaters().get(astNode.updaters().size() - 1);
			starts[1] = e.getStartPosition() + e.getLength();
		} else if (astNode.getExpression() != null) {
			starts[1] = astNode.getExpression().getStartPosition() + astNode.getExpression().getLength();
		} else if (!astNode.initializers().isEmpty()) {
			Expression e = (Expression) astNode.initializers().get(astNode.initializers().size() - 1);
			starts[1] = e.getStartPosition() + e.getLength();
		}
		lengths[1] = astNode.getBody().getStartPosition() - starts[1];
	}

	private void setPositionInfo(IfStatement astNode) {
		starts = new int[2];
		lengths = new int[2];
		starts[0] = astNode.getStartPosition();
		lengths[0] = astNode.getExpression().getStartPosition() - starts[0];
		starts[1] = astNode.getExpression().getStartPosition() + astNode.getExpression().getLength();
		lengths[1] = astNode.getThenStatement().getStartPosition() - starts[1];
	}

	private void setPositionInfo(InfixExpression astNode) {
		Expression e = astNode.getLeftOperand();
		starts = new int[]{e.getStartPosition() + e.getLength()};
		e = astNode.getRightOperand();
		lengths = new int[]{e.getStartPosition() - starts[0]};
	}

	private void setPositionInfo(InstanceofExpression astNode) {
		Expression e = astNode.getLeftOperand();
		starts = new int[]{e.getStartPosition() + e.getLength()};
		Type t = astNode.getRightOperand();
		lengths = new int[]{t.getStartPosition() - starts[0]};
	}

	private void setPositionInfo(MethodInvocation astNode) {
		starts = new int[astNode.arguments().isEmpty() ? 1 : 2];
		lengths = new int[starts.length];
		starts[0] = astNode.getName().getStartPosition();
		if (astNode.arguments().isEmpty()) {
			lengths[0] = astNode.getStartPosition() + astNode.getLength() - starts[0];
		} else {
			Expression e = (Expression) astNode.arguments().get(0);
			lengths[0] = e.getStartPosition() - starts[0];
			e = (Expression) astNode.arguments().get(astNode.arguments().size() - 1);
			starts[1] = e.getStartPosition() + e.getLength();
			lengths[1] = astNode.getStartPosition() + astNode.getLength() - starts[1];
		}
	}

	private void setPositionInfo(NullLiteral astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getLength()};
	}

	private void setPositionInfo(NumberLiteral astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getLength()};
	}

	private void setPositionInfo(PostfixExpression astNode) {
		starts = new int[] {astNode.getOperand().getStartPosition() + astNode.getOperand().getLength()};
		lengths = new int[] {astNode.getStartPosition() + astNode.getLength() - starts[0]};
	}

	private void setPositionInfo(PrefixExpression astNode) {
		starts = new int[] {astNode.getStartPosition()};
		lengths = new int[] {astNode.getOperand().getStartPosition() - starts[0]};
	}

	private void setPositionInfo(QualifiedName astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getLength()};
	}

	private void setPositionInfo(ReturnStatement astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getExpression() == null ? astNode.getLength() : astNode.getExpression().getStartPosition() - starts[0]};
	}

	private void setPositionInfo(SimpleName astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getLength()};
	}

	private void setPositionInfo(SingleVariableDeclaration astNode) {
		if (astNode.getInitializer() == null) {
			starts = new int[] {astNode.getName().getStartPosition()};
			lengths = new int[] {astNode.getName().getLength()};
		} else {
			starts = new int[] {astNode.getName().getStartPosition() + astNode.getName().getLength()};
			lengths = new int[] {astNode.getInitializer().getStartPosition() - starts[0]};
		}
	}

	private void setPositionInfo(StringLiteral astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getLength()};
	}

	private void setPositionInfo(SuperConstructorInvocation astNode) {
		starts = new int[astNode.arguments().isEmpty() ? 1 : 2];
		lengths = new int[starts.length];
		Expression e = astNode.getExpression();
		if (e == null)
			starts[0] = astNode.getStartPosition();
		else 
			starts[0] = e.getStartPosition() + e.getLength();
		if (astNode.arguments().isEmpty()) {
			lengths[0] = astNode.getStartPosition() + astNode.getLength() - starts[0];
		} else {
			e = (Expression) astNode.arguments().get(0);
			lengths[0] = e.getStartPosition() - starts[0];
			e = (Expression) astNode.arguments().get(astNode.arguments().size() - 1);
			starts[1] = e.getStartPosition() + e.getLength();
			lengths[1] = astNode.getStartPosition() + astNode.getLength() - starts[1];
		}
	}

	private void setPositionInfo(SuperFieldAccess astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getLength()};
	}

	private void setPositionInfo(SuperMethodInvocation astNode) {
		starts = new int[astNode.arguments().isEmpty() ? 1 : 2];
		lengths = new int[starts.length];
		Expression e = astNode.getQualifier();
		if (e == null)
			starts[0] = astNode.getStartPosition();
		else 
			starts[0] = e.getStartPosition() + e.getLength();
		if (astNode.arguments().isEmpty()) {
			lengths[0] = astNode.getStartPosition() + astNode.getLength() - starts[0];
		} else {
			e = (Expression) astNode.arguments().get(0);
			lengths[0] = e.getStartPosition() - starts[0];
			e = (Expression) astNode.arguments().get(astNode.arguments().size() - 1);
			starts[1] = e.getStartPosition() + e.getLength();
			lengths[1] = astNode.getStartPosition() + astNode.getLength() - starts[1];
		}
	}

	private void setPositionInfo(SwitchStatement astNode) {
		// TODO
	}

	private void setPositionInfo(SynchronizedStatement astNode) {
		starts = new int[2];
		lengths = new int[2];
		starts[0] = astNode.getStartPosition();
		lengths[0] = astNode.getExpression().getStartPosition() - starts[0];
		starts[1] = astNode.getExpression().getStartPosition() + astNode.getExpression().getLength();
		lengths[1] = astNode.getBody().getStartPosition() - starts[1];
	}

	private void setPositionInfo(ThisExpression astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getLength()};
	}

	private void setPositionInfo(ThrowStatement astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getExpression().getStartPosition() - starts[0]};
	}

	private void setPositionInfo(TryStatement astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getBody().getStartPosition() - starts[0]};
	}

	private void setPositionInfo(TypeLiteral astNode) {
		starts = new int[]{astNode.getStartPosition()};
		lengths = new int[]{astNode.getLength()};
	}

	private void setPositionInfo(VariableDeclarationFragment astNode) {
		if (astNode.getInitializer() == null) {
			starts = new int[] {astNode.getName().getStartPosition()};
			lengths = new int[] {astNode.getName().getLength()};
		} else {
			starts = new int[] {astNode.getName().getStartPosition() + astNode.getName().getLength()};
			lengths = new int[] {astNode.getInitializer().getStartPosition() - starts[0]};
		}
	}

	private void setPositionInfo(WhileStatement astNode) {
		starts = new int[2];
		lengths = new int[2];
		starts[0] = astNode.getStartPosition();
		lengths[0] = astNode.getExpression().getStartPosition() - starts[0];
		starts[1] = astNode.getExpression().getStartPosition() + astNode.getExpression().getLength();
		lengths[1] = astNode.getBody().getStartPosition() - starts[1];
	}

	private void setPositionInfo(ASTNode astNode) {
		if (astNode instanceof ArrayAccess) setPositionInfo((ArrayAccess) astNode);
		else if (astNode instanceof ArrayCreation) setPositionInfo((ArrayCreation) astNode);
		else if (astNode instanceof ArrayInitializer) setPositionInfo((ArrayInitializer) astNode);
		else if (astNode instanceof AssertStatement) setPositionInfo((AssertStatement) astNode);
		else if (astNode instanceof Assignment) setPositionInfo((Assignment) astNode);
		else if (astNode instanceof BooleanLiteral) setPositionInfo((BooleanLiteral) astNode);
		else if (astNode instanceof BreakStatement) setPositionInfo((BreakStatement) astNode);
		else if (astNode instanceof CastExpression) setPositionInfo((CastExpression) astNode);
		else if (astNode instanceof CatchClause) setPositionInfo((CatchClause) astNode);
		else if (astNode instanceof CharacterLiteral) setPositionInfo((CharacterLiteral) astNode);
		else if (astNode instanceof ClassInstanceCreation) setPositionInfo((ClassInstanceCreation) astNode);
		else if (astNode instanceof ConditionalExpression) setPositionInfo((ConditionalExpression) astNode);
		else if (astNode instanceof ContinueStatement) setPositionInfo((ContinueStatement) astNode);
		else if (astNode instanceof ConstructorInvocation) setPositionInfo((ConstructorInvocation) astNode);
		else if (astNode instanceof DoStatement) setPositionInfo((DoStatement) astNode);
		else if (astNode instanceof EnhancedForStatement) setPositionInfo((EnhancedForStatement) astNode);
		else if (astNode instanceof FieldAccess) setPositionInfo((FieldAccess) astNode);
		else if (astNode instanceof ForStatement) setPositionInfo((ForStatement) astNode);
		else if (astNode instanceof IfStatement) setPositionInfo((IfStatement) astNode);
		else if (astNode instanceof InfixExpression) setPositionInfo((InfixExpression) astNode);
		else if (astNode instanceof InstanceofExpression) setPositionInfo((InstanceofExpression) astNode);
		else if (astNode instanceof MethodInvocation) setPositionInfo((MethodInvocation) astNode);
		else if (astNode instanceof NullLiteral) setPositionInfo((NullLiteral) astNode);
		else if (astNode instanceof NumberLiteral) setPositionInfo((NumberLiteral) astNode);
		else if (astNode instanceof PostfixExpression) setPositionInfo((PostfixExpression) astNode);
		else if (astNode instanceof PrefixExpression) setPositionInfo((PrefixExpression) astNode);
		else if (astNode instanceof QualifiedName) setPositionInfo((QualifiedName) astNode);
		else if (astNode instanceof ReturnStatement) setPositionInfo((ReturnStatement) astNode);
		else if (astNode instanceof SimpleName) setPositionInfo((SimpleName) astNode);
		else if (astNode instanceof SingleVariableDeclaration) setPositionInfo((SingleVariableDeclaration) astNode);
		else if (astNode instanceof StringLiteral) setPositionInfo((StringLiteral) astNode);
		else if (astNode instanceof SuperConstructorInvocation) setPositionInfo((SuperConstructorInvocation) astNode);
		else if (astNode instanceof SuperFieldAccess) setPositionInfo((SuperFieldAccess) astNode);
		else if (astNode instanceof SuperMethodInvocation) setPositionInfo((SuperMethodInvocation) astNode);
		else if (astNode instanceof SwitchStatement) setPositionInfo((SwitchStatement) astNode);
		else if (astNode instanceof SynchronizedStatement) setPositionInfo((SynchronizedStatement) astNode);
		else if (astNode instanceof ThisExpression) setPositionInfo((ThisExpression) astNode);
		else if (astNode instanceof ThrowStatement) setPositionInfo((ThrowStatement) astNode);
		else if (astNode instanceof TryStatement) setPositionInfo((TryStatement) astNode);
		else if (astNode instanceof TypeLiteral) setPositionInfo((TypeLiteral) astNode);
		else if (astNode instanceof VariableDeclarationFragment) setPositionInfo((VariableDeclarationFragment) astNode);
		else if (astNode instanceof WhileStatement) setPositionInfo((WhileStatement) astNode);
		else throw new IllegalArgumentException(astNode.getClass().toString());
	}

	public int getAstNodeType() {
		return astNodeType;
	}

	public int getChangeType() {
		return changeType;
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
		if (dataType != null) {
			if (dataName != null)
				return dataType + "(" + dataName + ")";
			return dataType;
		}
		return label;
	}

	public int getVersion() {
		return version;
	}

	public String getStartsAsString() {
		return Arrays.toString(starts).replace(" ", "");
	}

	public String getLengthsAsString() {
		return Arrays.toString(lengths).replace(" ", "");
	}

	@Override
	public String toString() {
		return label;
	}

	public boolean isMapped() {
		if (this.version == 0)
			return isMapped(this.outEdges);
		if (this.version == 1)
			return isMapped(this.inEdges);
		return false;
	}

	private boolean isMapped(ArrayList<ChangeEdge> edges) {
		for (ChangeEdge e : edges) {
			if (e.isMapped())
				return true;
		}
		return false;
	}
}
