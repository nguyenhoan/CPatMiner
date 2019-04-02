package pdg.graph;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import exas.ExasFeature;
import exas.ExasSingleFeature;
import pdg.graph.PDGDataEdge.Type;
import treed.TreedConstants;
import utils.FeatureAscendingOrder;
import utils.JavaASTUtil;

public class PDGGraph implements Serializable {
	private static final long serialVersionUID = -5128703931982211886L;
	
	private PDGBuildingContext context;
	private HashMap<String, HashSet<PDGDataNode>> defStore = new HashMap<>();
	protected PDGNode entryNode, endNode;
	protected PDGDataNode[] parameters;
	protected HashSet<PDGNode> nodes = new HashSet<PDGNode>();
	protected HashSet<PDGNode> statementNodes = new HashSet<>();
	protected HashSet<PDGDataNode> dataSources = new HashSet<>();
	protected HashSet<PDGNode> statementSources = new HashSet<>();
	protected HashSet<PDGNode> sinks = new HashSet<PDGNode>();
	protected HashSet<PDGNode> statementSinks = new HashSet<>();
	protected HashSet<PDGNode> breaks = new HashSet<>();
	protected HashSet<PDGNode> returns = new HashSet<>();
	protected HashSet<PDGNode> changedNodes = new HashSet<>();
	
	private HashMap<ExasFeature, Integer> exasVector = new HashMap<ExasFeature, Integer>();

	public PDGGraph(MethodDeclaration md, PDGBuildingContext context) {
		this(context);
		context.addScope();
		context.setMethod(md, false);
		int numOfParameters = 0;
		if (Modifier.isStatic(md.getModifiers()))
			parameters = new PDGDataNode[md.parameters().size()];
		else {
			parameters = new PDGDataNode[md.parameters().size() + 1];
			parameters[numOfParameters++] = new PDGDataNode(
					null, ASTNode.THIS_EXPRESSION, "this", "this", "this");
		}
		entryNode = new PDGEntryNode(md, ASTNode.METHOD_DECLARATION, "START");
		nodes.add(entryNode);
		statementNodes.add(entryNode);
		for (int i = 0; i < md.parameters().size(); i++) {
			SingleVariableDeclaration d = (SingleVariableDeclaration) md
					.parameters().get(i);
			String id = d.getName().getIdentifier();
			//context.addLocalVariable(id, "" + d.getStartPosition());
			mergeSequential(buildPDG(entryNode, "", d));
			String[] info = context.getLocalVariableInfo(id);
			this.parameters[numOfParameters++] = new PDGDataNode(
					d.getName(), ASTNode.SIMPLE_NAME, info[0], info[1],
					"PARAM_" + d.getName().getIdentifier(), false, true);
		}
		context.pushTry();
		if (md.getBody() != null) {
			Block block = md.getBody();
			if (!block.statements().isEmpty())
				mergeSequential(buildPDG(entryNode, "", block));
		}
		if (!context.interprocedural)
			statementSinks.addAll(context.popTry());
		adjustReturnNodes();
		adjustControlEdges();
		context.removeScope();
	}

	public PDGGraph(PDGBuildingContext context) {
		this.context = context;
	}

	public PDGGraph(PDGBuildingContext context, PDGNode node) {
		this(context);
		init(node);
	}

	private void init(PDGNode node) {
		if (node instanceof PDGDataNode && !node.isLiteral())
			dataSources.add((PDGDataNode) node);
		sinks.add(node);
		nodes.add(node);
		if (node.isStatement()) {
			statementNodes.add(node);
			statementSources.add(node);
			statementSinks.add(node);
		}
	}

	public HashSet<PDGNode> getNodes() {
		return nodes;
	}

	public HashSet<PDGNode> getChangedNodes() {
		return changedNodes;
	}

	public HashMap<ExasFeature, Integer> getExasVector() {
		return exasVector;
	}

	private PDGGraph buildPDG(PDGNode control, String branch, ASTNode node) {
		if (node instanceof ArrayAccess)
			return buildPDG(control, branch, (ArrayAccess) node);
		if (node instanceof ArrayCreation)
			return buildPDG(control, branch, (ArrayCreation) node);
		if (node instanceof ArrayInitializer)
			return buildPDG(control, branch, (ArrayInitializer) node);
		if (node instanceof AssertStatement)
			return buildPDG(control, branch, (AssertStatement) node);
		if (node instanceof Assignment)
			return buildPDG(control, branch, (Assignment) node);
		if (node instanceof Block)
			return buildPDG(control, branch, (Block) node);
		if (node instanceof BooleanLiteral)
			return buildPDG(control, branch, (BooleanLiteral) node);
		if (node instanceof BreakStatement)
			return buildPDG(control, branch, (BreakStatement) node);
		if (node instanceof CastExpression)
			return buildPDG(control, branch, (CastExpression) node);
		if (node instanceof CatchClause)
			return buildPDG(control, branch, (CatchClause) node);
		if (node instanceof CharacterLiteral)
			return buildPDG(control, branch, (CharacterLiteral) node);
		if (node instanceof ClassInstanceCreation)
			return buildPDG(control, branch, (ClassInstanceCreation) node);
		if (node instanceof ConditionalExpression)
			return buildPDG(control, branch, (ConditionalExpression) node);
		if (node instanceof ConstructorInvocation)
			return buildPDG(control, branch, (ConstructorInvocation) node);
		if (node instanceof ContinueStatement)
			return buildPDG(control, branch, (ContinueStatement) node);
		if (node instanceof DoStatement)
			return buildPDG(control, branch, (DoStatement) node);
		if (node instanceof EnhancedForStatement)
			return buildPDG(control, branch, (EnhancedForStatement) node);
		if (node instanceof ExpressionStatement)
			return buildPDG(control, branch, (ExpressionStatement) node);
		if (node instanceof FieldAccess)
			return buildPDG(control, branch, (FieldAccess) node);
		if (node instanceof ForStatement)
			return buildPDG(control, branch, (ForStatement) node);
		if (node instanceof IfStatement)
			return buildPDG(control, branch, (IfStatement) node);
		if (node instanceof InfixExpression)
			return buildPDG(control, branch, (InfixExpression) node);
		if (node instanceof Initializer)
			return buildPDG(control, branch, (Initializer) node);
		if (node instanceof InstanceofExpression)
			return buildPDG(control, branch, (InstanceofExpression) node);
		if (node instanceof LabeledStatement)
			return buildPDG(control, branch, (LabeledStatement) node);
		if (node instanceof MethodDeclaration)
			return buildPDG(control, branch, (MethodDeclaration) node);
		if (node instanceof MethodInvocation)
			return buildPDG(control, branch, (MethodInvocation) node);
		if (node instanceof NullLiteral)
			return buildPDG(control, branch, (NullLiteral) node);
		if (node instanceof NumberLiteral)
			return buildPDG(control, branch, (NumberLiteral) node);
		if (node instanceof ParenthesizedExpression)
			return buildPDG(control, branch, (ParenthesizedExpression) node);
		if (node instanceof PostfixExpression)
			return buildPDG(control, branch, (PostfixExpression) node);
		if (node instanceof PrefixExpression)
			return buildPDG(control, branch, (PrefixExpression) node);
		if (node instanceof QualifiedName)
			return buildPDG(control, branch, (QualifiedName) node);
		if (node instanceof ReturnStatement)
			return buildPDG(control, branch, (ReturnStatement) node);
		if (node instanceof SimpleName)
			return buildPDG(control, branch, (SimpleName) node);
		if (node instanceof SingleVariableDeclaration)
			return buildPDG(control, branch, (SingleVariableDeclaration) node);
		if (node instanceof StringLiteral)
			return buildPDG(control, branch, (StringLiteral) node);
		if (node instanceof SuperConstructorInvocation)
			return buildPDG(control, branch, (SuperConstructorInvocation) node);
		if (node instanceof SuperFieldAccess)
			return buildPDG(control, branch, (SuperFieldAccess) node);
		if (node instanceof SuperMethodInvocation)
			return buildPDG(control, branch, (SuperMethodInvocation) node);
		if (node instanceof SwitchCase)
			return buildPDG(control, branch, (SwitchCase) node);
		if (node instanceof SwitchStatement)
			return buildPDG(control, branch, (SwitchStatement) node);
		if (node instanceof SynchronizedStatement)
			return buildPDG(control, branch, (SynchronizedStatement) node);
		if (node instanceof ThisExpression)
			return buildPDG(control, branch, (ThisExpression) node);
		if (node instanceof ThrowStatement)
			return buildPDG(control, branch, (ThrowStatement) node);
		if (node instanceof TryStatement)
			return buildPDG(control, branch, (TryStatement) node);
		if (node instanceof TypeLiteral)
			return buildPDG(control, branch, (TypeLiteral) node);
		if (node instanceof VariableDeclarationExpression)
			return buildPDG(control, branch,
					(VariableDeclarationExpression) node);
		if (node instanceof VariableDeclarationFragment)
			return buildPDG(control, branch, (VariableDeclarationFragment) node);
		if (node instanceof VariableDeclarationStatement)
			return buildPDG(control, branch,
					(VariableDeclarationStatement) node);
		if (node instanceof WhileStatement)
			return buildPDG(control, branch, (WhileStatement) node);
		if (node instanceof WhileStatement)
			return buildPDG(control, branch, (WhileStatement) node);
		return new PDGGraph(context);
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			WhileStatement astNode) {
		context.addScope();
		PDGGraph pdg = buildArgumentPDG(control, branch,
				astNode.getExpression());
		PDGControlNode node = new PDGControlNode(control, branch,
				astNode, astNode.getNodeType());
		pdg.mergeSequentialData(node, Type.CONDITION);
		PDGGraph ebg = new PDGGraph(context, new PDGActionNode(node, "T",
				null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
		PDGGraph bg = buildPDG(node, "T", astNode.getBody());
		if (!bg.isEmpty())
			ebg.mergeSequential(bg);
		PDGGraph eg = new PDGGraph(context, new PDGActionNode(node, "F",
				null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
		pdg.mergeBranches(ebg, eg);
		/*
		 * pdg.sinks.remove(node); pdg.statementSinks.remove(node);
		 */
		pdg.adjustBreakNodes("");
		context.removeScope();
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			VariableDeclarationStatement astNode) {
		PDGGraph pdg = buildPDG(control, branch, (ASTNode) astNode.fragments()
				.get(0));
		for (int i = 1; i < astNode.fragments().size(); i++)
			pdg.mergeSequential(buildPDG(control, branch, (ASTNode) astNode
					.fragments().get(i)));
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			VariableDeclarationFragment astNode) {
		SimpleName name = astNode.getName();
		String type = JavaASTUtil.getSimpleType(astNode);
		context.addLocalVariable(name.getIdentifier(), "" + name.getStartPosition(), type);
		PDGDataNode node = new PDGDataNode(name, name.getNodeType(),
				"" + name.getStartPosition(), type,
				name.getIdentifier(), false, true);
		if (astNode.getInitializer() == null) {
			PDGGraph pdg = new PDGGraph(context, new PDGDataNode(null, ASTNode.NULL_LITERAL, "null", "", "null"));
			pdg.mergeSequentialData(new PDGActionNode(control, branch,
					astNode, ASTNode.ASSIGNMENT, null, null, "="), Type.PARAMETER);
			pdg.mergeSequentialData(node, Type.DEFINITION);
			return pdg;
		}
		PDGGraph pdg = buildPDG(control, branch, astNode.getInitializer());
		ArrayList<PDGActionNode> rets = pdg.getReturns();
		if (rets.size() > 0) {
			for (PDGActionNode ret : rets) {
				ret.astNodeType = ASTNode.ASSIGNMENT;
				ret.name = "=";
				pdg.extend(ret, new PDGDataNode(node), Type.DEFINITION);
			}
			return pdg;
		}
		ArrayList<PDGDataNode> defs = pdg.getDefinitions();
		if (defs.isEmpty()) {
			pdg.mergeSequentialData(new PDGActionNode(control, branch,
					astNode, ASTNode.ASSIGNMENT, null, null, "="), Type.PARAMETER);
			pdg.mergeSequentialData(node, Type.DEFINITION);
		} else {
			if (defs.get(0).isDummy()) {
				for (PDGDataNode def : defs) {
					defStore.remove(def.key);
					def.copyData(node);
					HashSet<PDGDataNode> ns = defStore.get(def.key);
					if (ns == null) {
						ns = new HashSet<>();
						defStore.put(def.key, ns);
					}
					ns.add(def);
				}
			} else {
				PDGDataNode def = defs.get(0);
				pdg.mergeSequentialData(
						new PDGDataNode(def.astNode, def.astNodeType, def.key, def.dataType,
								def.dataName, def.isField, false),
						Type.REFERENCE);
				pdg.mergeSequentialData(new PDGActionNode(control, branch,
						astNode, ASTNode.ASSIGNMENT, null, null, "="), Type.PARAMETER);
				pdg.mergeSequentialData(node, Type.DEFINITION);
			}
		}
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			VariableDeclarationExpression astNode) {
		PDGGraph pdg = buildPDG(control, branch, (ASTNode) astNode.fragments()
				.get(0));
		for (int i = 1; i < astNode.fragments().size(); i++)
			pdg.mergeSequential(buildPDG(control, branch, (ASTNode) astNode
					.fragments().get(i)));
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			TypeLiteral astNode) {
		PDGGraph pdg = new PDGGraph(context, new PDGDataNode(
				astNode, astNode.getNodeType(), "class", JavaASTUtil.getSimpleType(astNode.getType()), "class"));
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			TryStatement astNode) {
		if (astNode.getBody().statements().isEmpty())
			return new PDGGraph(context);
		context.pushTry();
		PDGGraph pdg = new PDGGraph(context);
		PDGControlNode node = new PDGControlNode(control, branch,
				astNode, astNode.getNodeType());
		pdg.mergeSequentialControl(node, "");
		PDGGraph[] gs = new PDGGraph[astNode.catchClauses().size() + 1];
		gs[0] = buildPDG(node, "T", astNode.getBody());
		for (int i = 0; i < astNode.catchClauses().size(); i++) {
			CatchClause cc = (CatchClause) astNode.catchClauses().get(i);
			gs[i + 1] = buildPDG(node, "F", cc);
		}
		pdg.mergeBranches(gs);
		// TODO
		// astNode.getFinally();
		context.popTry();
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ThrowStatement astNode) {
		PDGGraph pdg = buildArgumentPDG(control, branch, astNode.getExpression());
		PDGActionNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, null, "throw");
		pdg.mergeSequentialData(node, Type.PARAMETER);
		pdg.returns.add(node);
		pdg.sinks.remove(node);
		pdg.statementSinks.remove(node);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			SynchronizedStatement astNode) {
		PDGGraph pdg = buildPDG(control, branch, astNode.getExpression());
		PDGControlNode node = new PDGControlNode(control, branch,
				astNode, astNode.getNodeType());
		pdg.mergeSequentialData(node, Type.PARAMETER);
		if (!astNode.getBody().statements().isEmpty()) {
			pdg.mergeSequentialControl(new PDGActionNode(node, "",
					null, ASTNode.EMPTY_STATEMENT, null, null, "empty"), "");
			pdg.mergeSequential(buildPDG(node, "", astNode.getBody()));
			return pdg;
		}
		return new PDGGraph(context);
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			SwitchStatement astNode) {
		// TODO
		if (true) return new PDGGraph(context);
		if (astNode.statements().size() > 100)
			return new PDGGraph(context);
		PDGControlNode snode = new PDGControlNode(control, branch,
				astNode, astNode.getNodeType());
		PDGGraph pdg = new PDGGraph(context, snode);
		PDGGraph ebg = new PDGGraph(context, new PDGActionNode(snode, "T",
				null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
		List<?> statements = astNode.statements();
		int s = 0;
		while (s < statements.size()) {
			if (statements.get(s) instanceof SwitchCase)
				break;
		}
		for (int e = s + 1; e < statements.size(); e++) {
			if (statements.get(e) instanceof SwitchCase || e == statements.size() - 1) {
				if (!(statements.get(e) instanceof SwitchCase))
						e = statements.size();
				if (e > s + 1) {
					SwitchCase sc = (SwitchCase) statements.get(s);
					PDGGraph cg = null;
					if (sc.isDefault()) {
						cg = buildPDG(snode, "T", statements.subList(s+1, e));
						ebg.mergeSequential(cg);
					} else {
						PDGActionNode ccnode = new PDGActionNode(snode, "T", null, ASTNode.INFIX_EXPRESSION, null, null, "==");
						PDGGraph exg = buildArgumentPDG(snode, "T", astNode.getExpression());
						exg.mergeSequentialData(ccnode, Type.PARAMETER);
						PDGGraph cexg = buildArgumentPDG(snode, "T", ((SwitchCase) statements.get(s)).getExpression());
						cexg.mergeSequentialData(ccnode, Type.PARAMETER);
						cg = new PDGGraph(context);
						cg.mergeParallel(exg, cexg);
						cg.mergeSequentialData(new PDGActionNode(snode, "T", null, ASTNode.ASSIGNMENT, null, null, "="), Type.PARAMETER);
						PDGDataNode dummy = new PDGDataNode(null, ASTNode.SIMPLE_NAME,
								PDGNode.PREFIX_DUMMY + astNode.getStartPosition() + "_"
										+ astNode.getLength(), "boolean", PDGNode.PREFIX_DUMMY, false, true);
						cg.mergeSequentialData(dummy, Type.DEFINITION);
						cg.mergeSequentialData(new PDGDataNode(dummy.astNode, dummy.astNodeType, dummy.key, dummy.dataType, dummy.dataName), Type.REFERENCE);
					
						PDGControlNode cnode = new PDGControlNode(snode, "T", sc, ASTNode.IF_STATEMENT);
						cg.mergeSequentialData(cnode, Type.CONDITION);
		
						PDGGraph etg = new PDGGraph(context, new PDGActionNode(cnode, "T",
								null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
						PDGGraph tg = buildPDG(cnode, "T", statements.subList(s+1, e));
						if (!tg.isEmpty()) {
							etg.mergeSequential(tg);
							PDGGraph efg = new PDGGraph(context, new PDGActionNode(cnode, "F",
									null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
							cg.mergeBranches(etg, efg);
							ebg.mergeSequential(cg);
						}
					}
				}
				s = e;
			}
		}
		PDGGraph eg = new PDGGraph(context, new PDGActionNode(snode, "F",
				null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
		pdg.mergeBranches(ebg, eg);
		/*
		 * pdg.sinks.remove(node); pdg.statementSinks.remove(node);
		 */
		pdg.adjustBreakNodes("");
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			SuperMethodInvocation astNode) {
		PDGGraph[] pgs = new PDGGraph[astNode.arguments().size() + 1];
		pgs[0] = new PDGGraph(context, new PDGDataNode(
				null, ASTNode.THIS_EXPRESSION, "this",
				"super", "super"));
		for (int i = 0; i < astNode.arguments().size(); i++)
			pgs[i+1] = buildArgumentPDG(control, branch,
					(Expression) astNode.arguments().get(i));
		PDGActionNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, "super." + astNode.getName().getIdentifier() + "()", 
				astNode.getName().getIdentifier());
		PDGGraph pdg = null;
		pgs[0].mergeSequentialData(node, Type.RECEIVER);
		if (pgs.length > 0) {
			for (int i = 1; i < pgs.length; i++)
				pgs[i].mergeSequentialData(node, Type.PARAMETER);
			pdg = new PDGGraph(context);
			pdg.mergeParallel(pgs);
		} else
			pdg = new PDGGraph(context, node);
		// skip astNode.getQualifier()
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			SuperConstructorInvocation astNode) {
		PDGGraph[] pgs = new PDGGraph[astNode.arguments().size()];
		for (int i = 0; i < astNode.arguments().size(); i++) {
			pgs[i] = buildArgumentPDG(control, branch,
					(Expression) astNode.arguments().get(i));
		}
		PDGActionNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, "super()", "<new>");
		PDGGraph pdg = null;
		if (pgs.length > 0) {
			for (PDGGraph pg : pgs)
				pg.mergeSequentialData(node, Type.PARAMETER);
			pdg = new PDGGraph(context);
			pdg.mergeParallel(pgs);
		} else
			pdg = new PDGGraph(context, node);
		// skip astNode.getExpression()
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			StringLiteral astNode) {
		String lit = "";
		try {
			lit = astNode.getLiteralValue();
		} catch (IllegalArgumentException e) {}
		PDGGraph pdg = new PDGGraph(context, new PDGDataNode(
				astNode, astNode.getNodeType(), astNode.getEscapedValue(), "String",
				lit));
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			SingleVariableDeclaration astNode) {
		SimpleName name = astNode.getName();
		String type = JavaASTUtil.getSimpleType(astNode.getType());
		context.addLocalVariable(name.getIdentifier(), "" + name.getStartPosition(), type);
		PDGDataNode node = new PDGDataNode(name, name.getNodeType(),
				"" + name.getStartPosition(), type,
				name.getIdentifier(), false, true);
		PDGGraph pdg = new PDGGraph(context, new PDGDataNode(null, ASTNode.NULL_LITERAL, "null", "", "null"));
		pdg.mergeSequentialData(new PDGActionNode(control, branch,
				astNode, ASTNode.ASSIGNMENT, null, null, "="), Type.PARAMETER);
		pdg.mergeSequentialData(node, Type.DEFINITION);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch, SimpleName astNode) {
		String name = astNode.getIdentifier();
		String[] info = context.getLocalVariableInfo(name);
		if (info != null) {
			PDGGraph pdg = new PDGGraph(context, new PDGDataNode(
					astNode, astNode.getNodeType(), info[0], info[1],
					astNode.getIdentifier(), false, false));
			return pdg;
		}
		String type = context.getFieldType(name);
		if (type != null) {
			PDGGraph pdg = new PDGGraph(context, new PDGDataNode(
					null, ASTNode.THIS_EXPRESSION, "this",
					"this", "this"));
			pdg.mergeSequentialData(new PDGDataNode(astNode, ASTNode.FIELD_ACCESS,
					"this." + name, type, name, true,
					false), Type.QUALIFIER);
			return pdg;
		}
		if (Character.isUpperCase(name.charAt(0))) {
			PDGGraph pdg = new PDGGraph(context, new PDGDataNode(
					astNode, astNode.getNodeType(), name, name,
					name, false, false));
			return pdg;
		}
		PDGGraph pdg = new PDGGraph(context, new PDGDataNode(
				null, ASTNode.THIS_EXPRESSION, "this",
				"this", "this"));
		pdg.mergeSequentialData(new PDGDataNode(astNode, ASTNode.FIELD_ACCESS,
				"this." + name, "UNKNOWN", name, true,
				false), Type.QUALIFIER);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ReturnStatement astNode) {
		PDGGraph pdg = null;
		PDGActionNode node = null;
		if (astNode.getExpression() != null) {
			pdg = buildArgumentPDG(control, branch, astNode.getExpression());
			node = new PDGActionNode(control, branch, astNode, astNode.getNodeType(),
					null, null, "return");
			pdg.mergeSequentialData(node, Type.PARAMETER);
		} else {
			node = new PDGActionNode(control, branch, astNode, astNode.getNodeType(),
					null, null, "return");
			pdg = new PDGGraph(context, node);
		}
		pdg.returns.add(node);
		pdg.sinks.clear();
		pdg.statementSinks.clear();
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			QualifiedName astNode) {
		PDGGraph pdg = buildArgumentPDG(control, branch, astNode.getQualifier());
		PDGDataNode node = pdg.getOnlyDataOut();
		if (node.dataType.startsWith("UNKNOWN")) {
			String name = astNode.getName().getIdentifier();
			if (Character.isUpperCase(name.charAt(0))) {
				return new PDGGraph(context, new PDGDataNode(astNode, ASTNode.FIELD_ACCESS, astNode.getFullyQualifiedName(),
						astNode.getFullyQualifiedName(), astNode.getName().getIdentifier(), true, false));
			}
		} else
			pdg.mergeSequentialData(
					new PDGDataNode(astNode, ASTNode.FIELD_ACCESS, astNode.getFullyQualifiedName(),
							node.dataType + "." + astNode.getName().getIdentifier(),
							astNode.getName().getIdentifier(), true, false), Type.QUALIFIER);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			PrefixExpression astNode) {
		PDGGraph pdg = buildArgumentPDG(control, branch, astNode.getOperand());
		PDGDataNode node = pdg.getOnlyDataOut();
		if (astNode.getOperator() == PrefixExpression.Operator.PLUS)
			return pdg;
		if (astNode.getOperator() == PrefixExpression.Operator.INCREMENT
				|| astNode.getOperator() == PrefixExpression.Operator.DECREMENT) {
			PDGGraph rg = new PDGGraph(context, new PDGDataNode(
					null, ASTNode.NUMBER_LITERAL, "1", node.dataType, "1"));
			PDGActionNode op = new PDGActionNode(control, branch,
					astNode, astNode.getNodeType(), null, null, 
					astNode.getOperator().toString().substring(0, 1));
			pdg.mergeSequentialData(op, Type.PARAMETER);
			rg.mergeSequentialData(op, Type.PARAMETER);
			pdg.mergeParallel(rg);
		} else
			pdg.mergeSequentialData(
					new PDGActionNode(control, branch, astNode, astNode.getNodeType(),
							null, null, astNode.getOperator().toString()),
					Type.PARAMETER);
		if (astNode.getOperator() == PrefixExpression.Operator.INCREMENT
				|| astNode.getOperator() == PrefixExpression.Operator.DECREMENT) {
			pdg.mergeSequentialData(new PDGActionNode(control, branch,
					astNode, ASTNode.ASSIGNMENT, null, null, "="), Type.PARAMETER);
			pdg.mergeSequentialData(new PDGDataNode(node.astNode, node.astNodeType, node.key,
					node.dataType, node.dataName), Type.DEFINITION);
		}
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			PostfixExpression astNode) {
		PDGGraph lg = buildArgumentPDG(control, branch, astNode.getOperand());
		PDGDataNode node = lg.getOnlyDataOut();
		// FIXME handling postfix expression more precisely
		PDGGraph rg = new PDGGraph(context, new PDGDataNode(
				null, ASTNode.NUMBER_LITERAL, "1", node.dataType, "1"));
		PDGActionNode op = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, null, astNode.getOperator().toString()
						.substring(0, 1));
		lg.mergeSequentialData(op, Type.PARAMETER);
		rg.mergeSequentialData(op, Type.PARAMETER);
		PDGGraph pdg = new PDGGraph(context);
		pdg.mergeParallel(lg, rg);
		pdg.mergeSequentialData(new PDGActionNode(control, branch,
				astNode, ASTNode.ASSIGNMENT, null, null, "="), Type.PARAMETER);
		pdg.mergeSequentialData(new PDGDataNode(node.astNode, node.astNodeType, node.key,
				node.dataType, node.dataName), Type.DEFINITION);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ParenthesizedExpression astNode) {
		return buildPDG(control, branch, astNode.getExpression());
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			NumberLiteral astNode) {
		PDGGraph pdg = new PDGGraph(context, new PDGDataNode(
				astNode, astNode.getNodeType(), astNode.getToken(), "number",
				astNode.getToken()));
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			NullLiteral astNode) {
		PDGGraph pdg = new PDGGraph(context, new PDGDataNode(
				astNode, astNode.getNodeType(), astNode.toString(), "null",
				astNode.toString()));
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			MethodInvocation astNode) {
		if (astNode.getName().getIdentifier().toLowerCase().contains("assert"))
			return new PDGGraph(context, new PDGActionNode(control, branch,
					astNode, astNode.getNodeType(), null, null, "assert"));
		if (astNode.getName().getIdentifier().equals("exit")
				&& astNode.getExpression() != null && astNode.getExpression().toString().equals("System")) {
			PDGActionNode node = new PDGActionNode(control, branch,
					astNode, astNode.getNodeType(), null, "Sytem.exit()", astNode.getName().getIdentifier());
			PDGGraph pdg = new PDGGraph(context, node);
			pdg.returns.add(node);
			pdg.sinks.remove(node);
			pdg.statementSinks.remove(node);
			return pdg;
		}
		PDGGraph[] pgs = new PDGGraph[astNode.arguments().size() + 1];
		if (astNode.arguments().size() > 100)
			pgs = new PDGGraph[1];
		if (astNode.getExpression() != null)
			pgs[0] = buildArgumentPDG(control, branch,
					astNode.getExpression());
		else
			pgs[0] = new PDGGraph(context, new PDGDataNode(
					null, ASTNode.THIS_EXPRESSION, "this",
					"this", "this"));
		if (astNode.arguments().size() <= 100)
			for (int i = 0; i < astNode.arguments().size(); i++)
				pgs[i + 1] = buildArgumentPDG(control, branch, (Expression) astNode.arguments().get(i));
		PDGActionNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, 
				pgs[0].getOnlyOut().dataType + "." + astNode.getName().getIdentifier() + "()", 
				astNode.getName().getIdentifier());
		PDGGraph pdg = null;
		pgs[0].mergeSequentialData(node, Type.RECEIVER);
		if (pgs.length > 0) {
			for (int i = 1; i < pgs.length; i++)
				pgs[i].mergeSequentialData(node, Type.PARAMETER);
			pdg = new PDGGraph(context);
			pdg.mergeParallel(pgs);
		} else
			pdg = new PDGGraph(context, node);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			MethodDeclaration astNode) {
		PDGGraph pdg = new PDGGraph(context);
		// skip
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			LabeledStatement astNode) {
		adjustBreakNodes(astNode.getLabel().getIdentifier());
		return buildPDG(control, branch, astNode.getBody());
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			InstanceofExpression astNode) {
		PDGGraph pdg = buildArgumentPDG(control, branch,
				astNode.getLeftOperand());
		PDGNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, null, 
				JavaASTUtil.getSimpleType(astNode.getRightOperand()) + ".<instanceof>");
		pdg.mergeSequentialData(node, Type.PARAMETER);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			Initializer astNode) {
		return buildPDG(control, branch, astNode.getBody());
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			InfixExpression astNode) {
		PDGGraph pdg = new PDGGraph(context);
		List l = astNode.extendedOperands();
		if (l != null && l.size() > 10 - 2)
			return new PDGGraph(context, new PDGActionNode(control, branch, astNode, astNode.getNodeType(), null, null, astNode.getOperator().toString()));
		PDGGraph lg = buildArgumentPDG(control, branch,
				astNode.getLeftOperand());
		PDGGraph rg = buildArgumentPDG(control, branch,
				astNode.getRightOperand());
		PDGActionNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, null, astNode.getOperator().toString());
		lg.mergeSequentialData(node, Type.PARAMETER);
		rg.mergeSequentialData(node, Type.PARAMETER);
		pdg.mergeParallel(lg, rg);
		if (astNode.hasExtendedOperands())
			for (int i = 0; i < astNode.extendedOperands().size(); i++) {
				PDGGraph tmp = buildArgumentPDG(control, branch,
						(Expression) astNode.extendedOperands().get(i));
				tmp.mergeSequentialData(node, Type.PARAMETER);
				pdg.mergeParallel(tmp);
			}
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			IfStatement astNode) {
		context.addScope();
		PDGGraph pdg = buildArgumentPDG(control, branch,
				astNode.getExpression());
		PDGControlNode node = new PDGControlNode(control, branch,
				astNode, astNode.getNodeType());
		pdg.mergeSequentialData(node, Type.CONDITION);
		PDGGraph etg = new PDGGraph(context, new PDGActionNode(node, "T",
				null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
		PDGGraph tg = buildPDG(node, "T", astNode.getThenStatement());
		if (!tg.isEmpty())
			etg.mergeSequential(tg);
		PDGGraph efg = new PDGGraph(context, new PDGActionNode(node, "F",
				null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
		if (astNode.getElseStatement() != null) {
			PDGGraph fg = buildPDG(node, "F", astNode.getElseStatement());
			if (!fg.isEmpty())
				efg.mergeSequential(fg);
		}
		pdg.mergeBranches(etg, efg);
		/*
		 * pdg.sinks.remove(node); pdg.statementSinks.remove(node);
		 */
		context.removeScope();
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ForStatement astNode) {
		context.addScope();
		PDGGraph pdg = null;
		if (astNode.initializers() != null && astNode.initializers().size() > 0) {
			pdg = buildPDG(control, branch, (ASTNode) astNode.initializers()
					.get(0));
			for (int i = 1; i < astNode.initializers().size(); i++)
				pdg.mergeSequential(buildPDG(control, branch, (ASTNode) astNode
						.initializers().get(i)));
		}
		PDGGraph middleG = null;
		if (astNode.getExpression() != null) {
			middleG = buildArgumentPDG(control, branch, astNode.getExpression());
		}
		PDGControlNode node = new PDGControlNode(control, branch,
				astNode, astNode.getNodeType());
		if (middleG != null)
			middleG.mergeSequentialData(node, Type.CONDITION);
		else
			middleG = new PDGGraph(context, node);
		PDGGraph ebg = new PDGGraph(context, new PDGActionNode(node, "T", null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
		PDGGraph bg = buildPDG(node, "T", astNode.getBody());
		if (!bg.isEmpty()) {
			ebg.mergeSequential(bg);
		}
		if (astNode.updaters() != null && astNode.updaters().size() > 0) {
			PDGGraph ug = buildPDG(node, "T", (ASTNode) astNode.updaters()
					.get(0));
			for (int i = 1; i < astNode.updaters().size(); i++) {
				ug.mergeSequential(buildPDG(node, "T", (ASTNode) astNode
						.updaters().get(i)));
			}
			ebg.mergeSequential(ug);
		}
		
		PDGGraph eg = new PDGGraph(context, new PDGActionNode(node, "F", null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
		middleG.mergeBranches(ebg, eg);
		if (pdg == null)
			pdg = middleG;
		else
			pdg.mergeSequential(middleG);
		/*
		 * pdg.sinks.remove(node); pdg.statementSinks.remove(node);
		 */
		pdg.adjustBreakNodes("");
		context.removeScope();
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ExpressionStatement astNode) {
		PDGGraph pdg = buildPDG(control, branch, astNode.getExpression());
		ArrayList<PDGActionNode> rets = pdg.getReturns();
		if (rets.size() > 0) {
			for (PDGNode ret : new HashSet<PDGNode>(rets)) {
				for (PDGEdge e : new HashSet<PDGEdge>(ret.inEdges))
					if (e.source instanceof PDGDataNode)
						pdg.delete(e.source);
				pdg.delete(ret);
			}
		}
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			EnhancedForStatement astNode) {
		context.addScope();
		SimpleName name = astNode.getParameter().getName();
		String type = JavaASTUtil.getSimpleType(astNode.getParameter().getType());
		context.addLocalVariable(name.getIdentifier(), "" + name.getStartPosition(), type);
		PDGDataNode var = new PDGDataNode(name, name.getNodeType(),
				"" + name.getStartPosition(), type,
				name.getIdentifier(), false, true);
		PDGGraph pdg = buildArgumentPDG(control, branch, astNode.getExpression());
		pdg.mergeSequentialData(new PDGActionNode(control, branch,
				astNode, ASTNode.ASSIGNMENT, null, null, "="), Type.PARAMETER);
		pdg.mergeSequentialData(var, Type.DEFINITION);
		pdg.mergeSequentialData(new PDGDataNode(null, var.astNodeType, var.key, var.dataType, var.dataName), Type.REFERENCE);
		PDGControlNode node = new PDGControlNode(control, branch, astNode, astNode.getNodeType());
		pdg.mergeSequentialData(node, Type.CONDITION);
		pdg.mergeSequentialControl(new PDGActionNode(node, "",
				null, ASTNode.EMPTY_STATEMENT, null, null, "empty"), "");
		PDGGraph bg = buildPDG(node, "", astNode.getBody());
		if (!bg.isEmpty())
			pdg.mergeSequential(bg);
		pdg.adjustBreakNodes("");
		context.removeScope();
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			DoStatement astNode) {
		context.addScope();
		PDGControlNode node = new PDGControlNode(control, branch,
				astNode, astNode.getNodeType());
		PDGGraph pdg = buildArgumentPDG(control, branch,
				astNode.getExpression());
		pdg.mergeSequentialData(node, Type.CONDITION);
		PDGGraph ebg = new PDGGraph(context, new PDGActionNode(node, "T",
				null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
		PDGGraph bg = buildPDG(node, "T", astNode.getBody());
		if (!bg.isEmpty())
			ebg.mergeSequential(bg);
		PDGGraph eg = new PDGGraph(context, new PDGActionNode(node, "F",
				null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
		pdg.mergeBranches(ebg, eg);
		/*
		 * pdg.sinks.remove(node); pdg.statementSinks.remove(node);
		 */
		pdg.adjustBreakNodes("");
		context.removeScope();
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ContinueStatement astNode) {
		PDGActionNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), astNode.getLabel() == null ? "" : astNode.getLabel().getIdentifier(), null,
				"continue");
		PDGGraph pdg = new PDGGraph(context, node);
		pdg.breaks.add(node);
		pdg.sinks.remove(node);
		pdg.statementSinks.remove(node);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ConstructorInvocation astNode) {
		PDGGraph[] pgs = new PDGGraph[astNode.arguments().size()];
		int numOfParameters = 0;
		for (int i = 0; i < astNode.arguments().size(); i++) {
			pgs[numOfParameters] = buildArgumentPDG(control, branch,
					(Expression) astNode.arguments().get(i));
			numOfParameters++;
		}
		PDGActionNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, "this()", "<new>");
		PDGGraph pdg = null;
		if (pgs.length > 0) {
			for (PDGGraph pg : pgs)
				pg.mergeSequentialData(node, Type.PARAMETER);
			pdg = new PDGGraph(context);
			pdg.mergeParallel(pgs);
		} else
			pdg = new PDGGraph(context, node);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ConditionalExpression astNode) {
		PDGDataNode dummy = new PDGDataNode(null, ASTNode.SIMPLE_NAME,
				PDGNode.PREFIX_DUMMY + astNode.getStartPosition() + "_"
						+ astNode.getLength(), "boolean", PDGNode.PREFIX_DUMMY, false, true);
		PDGGraph pdg = buildArgumentPDG(control, branch,
				astNode.getExpression());
		PDGControlNode node = new PDGControlNode(control, branch,
				astNode, ASTNode.IF_STATEMENT);
		pdg.mergeSequentialData(node, Type.CONDITION);
		PDGGraph tg = buildArgumentPDG(node, "T", astNode.getThenExpression());
		tg.mergeSequentialData(new PDGActionNode(node, "T", null, ASTNode.ASSIGNMENT,
				null, null, "="), Type.PARAMETER);
		tg.mergeSequentialData(new PDGDataNode(dummy), Type.DEFINITION);
		PDGGraph fg = buildArgumentPDG(node, "F", astNode.getElseExpression());
		fg.mergeSequentialData(new PDGActionNode(node, "F", null, ASTNode.ASSIGNMENT,
				null, null, "="), Type.PARAMETER);
		fg.mergeSequentialData(new PDGDataNode(dummy), Type.DEFINITION);
		pdg.mergeBranches(tg, fg);
		/*
		 * pdg.sinks.remove(node); pdg.statementSinks.remove(node);
		 */
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ClassInstanceCreation astNode) {
		PDGGraph[] pgs = new PDGGraph[astNode.arguments().size()];
		int numOfParameters = 0;
		for (int i = 0; i < astNode.arguments().size(); i++) {
			pgs[numOfParameters] = buildArgumentPDG(control, branch,
					(Expression) astNode.arguments().get(i));
			numOfParameters++;
		}
		PDGActionNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, JavaASTUtil.getSimpleType(astNode.getType()), "<new>");
		PDGGraph pdg = null;
		if (pgs.length > 0) {
			for (PDGGraph pg : pgs)
				pg.mergeSequentialData(node, Type.PARAMETER);
			pdg = new PDGGraph(context);
			pdg.mergeParallel(pgs);
		} else
			pdg = new PDGGraph(context, node);
		// skip astNode.getExpression()
		// skip astNode.getAnonymousClassDeclaration()
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			CharacterLiteral astNode) {
		PDGGraph pdg = new PDGGraph(context, new PDGDataNode(
				astNode, astNode.getNodeType(), astNode.getEscapedValue(), "char",
				astNode.getEscapedValue()));
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			CatchClause astNode) {
		context.addScope();
		SimpleName name = astNode.getException().getName();
		String type = JavaASTUtil.getSimpleType(astNode.getException().getType());
		context.addLocalVariable(name.getIdentifier(), "" + name.getStartPosition(), type);
		PDGControlNode node = new PDGControlNode(control, branch,
				astNode, astNode.getNodeType());
		PDGGraph pdg = new PDGGraph(context, node);
		PDGGraph cg = new PDGGraph(context, new PDGActionNode(node, "",
				null, ASTNode.EMPTY_STATEMENT, null, null, "empty"));
		if (!astNode.getBody().statements().isEmpty())
			cg.mergeSequential(buildPDG(node, "", astNode.getBody()));
		pdg.mergeSequentialControl(cg);
		/*HashSet<PDGActionNode> nodes = context.getTrys(astNode.getException()
				.getType().resolveBinding());
		for (PDGActionNode n : nodes)
			new PDGDataEdge(n, node, Type.DEPENDENCE);*/
		context.removeScope();
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			CastExpression astNode) {
		PDGGraph pdg = buildArgumentPDG(control, branch,
				astNode.getExpression());
		PDGNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, JavaASTUtil.getSimpleType(astNode.getType()), JavaASTUtil.getSimpleType(astNode.getType()) + ".<cast>");
		pdg.mergeSequentialData(node, Type.PARAMETER);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			BreakStatement astNode) {
		PDGActionNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), astNode.getLabel() == null ? "" : astNode.getLabel().getIdentifier(), null,
				"break");
		PDGGraph pdg = new PDGGraph(context, node);
		pdg.breaks.add(node);
		pdg.sinks.remove(node);
		pdg.statementSinks.remove(node);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			BooleanLiteral astNode) {
		PDGGraph pdg = new PDGGraph(context, new PDGDataNode(
				astNode, astNode.getNodeType(), astNode.toString(), "boolean",
				astNode.toString()));
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch, Block astNode) {
		if (astNode.statements().size() > 0 && astNode.statements().size() <= 100) {
			context.addScope();
			PDGGraph pdg = buildPDG(control, branch, astNode.statements());
			context.removeScope();
			return pdg;
		}
		return new PDGGraph(context);
	}

	public PDGGraph buildPDG(PDGNode control, String branch, List<?> l) {
		ArrayList<PDGGraph> pdgs = new ArrayList<>();
		for (int i = 0; i < l.size(); i++) {
			if (l.get(i) instanceof EmptyStatement) continue;
			PDGGraph pdg = buildPDG(control, branch, (ASTNode) l.get(i));
			if (!pdg.isEmpty())
				pdgs.add(pdg);
		}
		int s = 0;
		for (int i = 1; i < pdgs.size(); i++)
			if (pdgs.get(s).statementNodes.isEmpty())
				s = i;
			else
				pdgs.get(s).mergeSequential(pdgs.get(i));
		if (s == pdgs.size())
			return new PDGGraph(context);
		return pdgs.get(s);
	}

	private PDGGraph buildPDG(PDGNode control, String branch, Assignment astNode) {
		if (!(astNode.getLeftHandSide() instanceof Name))
			return buildPDG(control, branch, astNode.getRightHandSide());
		PDGGraph lg = buildPDG(control, branch, astNode.getLeftHandSide());
		PDGDataNode lnode = lg.getOnlyDataOut();
		PDGGraph pdg = null;
		if (astNode.getOperator() != Assignment.Operator.ASSIGN) {
			String op = JavaASTUtil.getInfixOperator(astNode.getOperator());
			PDGGraph g1 = buildPDG(control, branch, astNode.getLeftHandSide());
			PDGGraph g2 = buildArgumentPDG(control, branch,
					astNode.getRightHandSide());
			PDGActionNode opNode = new PDGActionNode(control, branch,
					null, ASTNode.INFIX_EXPRESSION, null, null, op);
			g1.mergeSequentialData(opNode, Type.PARAMETER);
			g2.mergeSequentialData(opNode, Type.PARAMETER);
			pdg = new PDGGraph(context);
			pdg.mergeParallel(g1, g2);
			pdg.mergeSequentialData(new PDGActionNode(control, branch,
					astNode, ASTNode.ASSIGNMENT, null, null, "="), Type.PARAMETER);
			pdg.mergeSequentialData(lnode, Type.DEFINITION);
		} else {
			pdg = buildPDG(control, branch, astNode.getRightHandSide());
			ArrayList<PDGActionNode> rets = pdg.getReturns();
			if (rets.size() > 0) {
				for (PDGActionNode ret : rets) {
					ret.astNodeType = ASTNode.ASSIGNMENT;
					ret.name = "=";
					pdg.extend(ret, new PDGDataNode(lnode), Type.DEFINITION);
				}
				pdg.nodes.addAll(lg.nodes);
				pdg.statementNodes.addAll(lg.statementNodes);
				lg.dataSources.remove(lnode);
				pdg.dataSources.addAll(lg.dataSources);
				pdg.statementSources.addAll(lg.statementSources);
				lg.clear();
				return pdg;
			}
			ArrayList<PDGDataNode> defs = pdg.getDefinitions();
			if (defs.isEmpty()) {
				pdg.mergeSequentialData(new PDGActionNode(control, branch,
						astNode, ASTNode.ASSIGNMENT, null, null, "="), Type.PARAMETER);
				pdg.mergeSequentialData(lnode, Type.DEFINITION);
			} else {
				if (defs.get(0).isDummy()) {
					for (PDGDataNode def : defs) {
						pdg.defStore.remove(def.key);
						def.copyData(lnode);
						HashSet<PDGDataNode> ns = pdg.defStore.get(def.key);
						if (ns == null) {
							ns = new HashSet<>();
							pdg.defStore.put(def.key, ns);
						}
						ns.add(def);
					}
				} else {
					PDGDataNode def = defs.get(0);
					pdg.mergeSequentialData(new PDGDataNode(null, def.astNodeType,
							def.key, def.dataType, def.dataName),
							Type.REFERENCE);
					pdg.mergeSequentialData(new PDGActionNode(control, branch,
							astNode, ASTNode.ASSIGNMENT, null, null, "="), Type.PARAMETER);
					pdg.mergeSequentialData(lnode, Type.DEFINITION);
				}
			}
		}
		pdg.nodes.addAll(lg.nodes);
		pdg.statementNodes.addAll(lg.statementNodes);
		lg.dataSources.remove(lnode);
		pdg.dataSources.addAll(lg.dataSources);
		pdg.statementSources.addAll(lg.statementSources);
		lg.clear();
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			AssertStatement astNode) {
		if (true) 
			return new PDGGraph(context, new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, null, "assert"));
		PDGGraph pdg = buildArgumentPDG(control, branch,
				astNode.getExpression());
		PDGNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, null, "assert");
		pdg.mergeSequentialData(node, Type.PARAMETER);
		// skip astNode.getMessage()
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ArrayInitializer astNode) {
		PDGGraph[] pgs = new PDGGraph[astNode.expressions().size()];
		if (astNode.expressions().size() <= 10) {
			for (int i = 0; i < astNode.expressions().size(); i++)
				pgs[i] = buildArgumentPDG(control, branch, (Expression) astNode.expressions().get(i));
		} else
			pgs = new PDGGraph[0];
		PDGNode node = new PDGActionNode(control, branch, astNode, astNode.getNodeType(), null, "{}", "{}");
		if (pgs.length > 0) {
			for (PDGGraph pg : pgs)
				pg.mergeSequentialData(node, Type.PARAMETER);
			PDGGraph pdg = new PDGGraph(context);
			pdg.mergeParallel(pgs);
			return pdg;
		} else
			return new PDGGraph(context, node);
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ArrayCreation astNode) {
		if (astNode.getInitializer() != null) {
			return buildPDG(control, branch, astNode.getInitializer());
		}
		PDGGraph[] pgs = new PDGGraph[astNode.dimensions().size()];
		if (astNode.dimensions().size() <= 10) {
			for (int i = 0; i < astNode.dimensions().size(); i++)
				pgs[i] = buildArgumentPDG(control, branch, (Expression) astNode.dimensions().get(i));
		} else
			pgs = new PDGGraph[0];
		PDGNode node = new PDGActionNode(control, branch,
				astNode, astNode.getNodeType(), null, "{}", "<new>");
		if (pgs.length > 0) {
			for (PDGGraph pg : pgs)
				pg.mergeSequentialData(node, Type.PARAMETER);
			PDGGraph pdg = new PDGGraph(context);
			pdg.mergeParallel(pgs);
			return pdg;
		} else
			return new PDGGraph(context, node);
	}

	private void mergeBranches(PDGGraph... pdgs) {
		HashMap<String, HashSet<PDGDataNode>> defStore = new HashMap<>();
		HashMap<String, Integer> defCounts = new HashMap<>();
		sinks.clear();
		statementSinks.clear();
		for (PDGGraph pdg : pdgs) {
			nodes.addAll(pdg.nodes);
			statementNodes.addAll(pdg.statementNodes);
			sinks.addAll(pdg.sinks);
			statementSinks.addAll(pdg.statementSinks);
			for (PDGDataNode source : new HashSet<PDGDataNode>(pdg.dataSources)) {
				HashSet<PDGDataNode> defs = this.defStore.get(source.key);
				if (defs != null) {
					for (PDGDataNode def : defs)
						if (def != null)
							new PDGDataEdge(def, source, Type.REFERENCE);
					if (!defs.contains(null))
						pdg.dataSources.remove(source);
				}
			}
			dataSources.addAll(pdg.dataSources);
			// statementSources.addAll(pdg.statementSources);
			breaks.addAll(pdg.breaks);
			returns.addAll(pdg.returns);
		}
		for (PDGGraph pdg : pdgs) {
			HashMap<String, HashSet<PDGDataNode>> localStore = copyDefStore();
			updateDefStore(localStore, defCounts, pdg.defStore);
			add(defStore, localStore);
			pdg.clear();
		}
		for (String key : defCounts.keySet())
			if (defCounts.get(key) < pdgs.length)
				defStore.get(key).add(null);
		clearDefStore();
		this.defStore = defStore;
	}

	private void mergeParallel(PDGGraph... pdgs) {
		HashMap<String, HashSet<PDGDataNode>> defStore = new HashMap<>();
		HashMap<String, Integer> defCounts = new HashMap<>();
		for (PDGGraph pdg : pdgs) {
			HashMap<String, HashSet<PDGDataNode>> localStore = copyDefStore();
			nodes.addAll(pdg.nodes);
			statementNodes.addAll(pdg.statementNodes);
			sinks.addAll(pdg.sinks);
			statementSinks.addAll(pdg.statementSinks);
			dataSources.addAll(pdg.dataSources);
			statementSources.addAll(pdg.statementSources);
			breaks.addAll(pdg.breaks);
			returns.addAll(pdg.returns);
			updateDefStore(localStore, defCounts, pdg.defStore);
			add(defStore, localStore);
			pdg.clear();
		}
		clearDefStore();
		this.defStore = defStore;
	}

	private void clear() {
		nodes.clear();
		statementNodes.clear();
		dataSources.clear();
		statementSources.clear();
		sinks.clear();
		statementSinks.clear();
		breaks.clear();
		returns.clear();
		clearDefStore();
	}

	private void delete(PDGNode node) {
		if (statementSinks.contains(node))
			for (PDGEdge e : node.inEdges)
				if (e instanceof PDGDataEdge) {
					if (((PDGDataEdge) e).type == Type.DEPENDENCE)
						statementSinks.add(e.source);
					else if (((PDGDataEdge) e).type == Type.PARAMETER)
						sinks.add(e.source);
				}
		if (sinks.contains(node) && node instanceof PDGDataNode) {
			for (PDGEdge e : node.inEdges)
				if (e.source instanceof PDGDataNode)
					sinks.add(e.source);
		}
		if (statementSources.contains(node))
			for (PDGEdge e : node.outEdges)
				if (e instanceof PDGDataEdge
						&& ((PDGDataEdge) e).type == Type.DEPENDENCE)
					statementSources.add(e.target);
		nodes.remove(node);
		changedNodes.remove(node);
		statementNodes.remove(node);
		dataSources.remove(node);
		statementSources.remove(node);
		sinks.remove(node);
		statementSinks.remove(node);
		node.delete();
	}

	private PDGGraph buildArgumentPDG(PDGNode control, String branch,
			ASTNode exp) {
		PDGGraph pdg = buildPDG(control, branch, exp);
		if (pdg.isEmpty())
			return pdg;
		if (pdg.nodes.size() == 1)
			for (PDGNode node : pdg.nodes)
				if (node instanceof PDGDataNode)
					return pdg;
		ArrayList<PDGDataNode> defs = pdg.getDefinitions();
		if (!defs.isEmpty()) {
			PDGDataNode def = defs.get(0);
			pdg.mergeSequentialData(new PDGDataNode(null, def.astNodeType, def.key,
					((PDGDataNode) def).dataType, ((PDGDataNode) def).dataName,
					def.isField, false), Type.REFERENCE);
			return pdg;
		}
		ArrayList<PDGActionNode> rets = pdg.getReturns();
		if (rets.size() > 0) {
			PDGDataNode dummy = new PDGDataNode(null, ASTNode.SIMPLE_NAME,
					PDGNode.PREFIX_DUMMY + exp.getStartPosition() + "_"
							+ exp.getLength(), rets.get(0).dataType, PDGNode.PREFIX_DUMMY, false, true);
			for (PDGActionNode ret : rets) {
				ret.astNodeType = ASTNode.ASSIGNMENT;
				ret.name = "=";
				pdg.extend(ret, new PDGDataNode(dummy), Type.DEFINITION);
			}
			pdg.mergeSequentialData(new PDGDataNode(null, dummy.astNodeType,
					dummy.key, dummy.dataType, dummy.dataName), Type.REFERENCE);
			return pdg;
		}
		PDGNode node = pdg.getOnlyOut();
		if (node instanceof PDGDataNode)
			return pdg;
		PDGDataNode dummy = new PDGDataNode(null, ASTNode.SIMPLE_NAME,
				PDGNode.PREFIX_DUMMY + exp.getStartPosition() + "_"
						+ exp.getLength(), node.dataType, PDGNode.PREFIX_DUMMY, false, true);
		pdg.mergeSequentialData(new PDGActionNode(control, branch,
				null, ASTNode.ASSIGNMENT, null, null, "="), Type.PARAMETER);
		pdg.mergeSequentialData(dummy, Type.DEFINITION);
		pdg.mergeSequentialData(new PDGDataNode(null, dummy.astNodeType, dummy.key,
				dummy.dataType, dummy.dataName), Type.REFERENCE);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ArrayAccess astNode) {
		PDGGraph pdg = buildArgumentPDG(control, branch, astNode.getArray());
		String type = pdg.getOnlyOut().dataType;
		// FIXME type could be null
		if (type != null && type.endsWith("[]"))
			type = type.substring(0, type.length() - 2);
		else
			type = type + "[.]";
		PDGNode node = new PDGDataNode(astNode, astNode.getNodeType(),
				context.getKey(astNode), type,
				astNode.toString());
		pdg.mergeSequentialData(node, Type.QUALIFIER);
		PDGGraph ig = buildArgumentPDG(control, branch, astNode.getIndex());
		ig.mergeSequentialData(node, Type.PARAMETER);
		pdg.mergeBranches(ig);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			FieldAccess astNode) {
		PDGGraph pdg = buildArgumentPDG(control, branch,
				astNode.getExpression());
		PDGDataNode node = pdg.getOnlyDataOut();
		pdg.mergeSequentialData(
				new PDGDataNode(astNode, astNode.getNodeType(), astNode.toString(),
						node.dataType + "." + astNode.getName().getIdentifier(),
						astNode.getName().getIdentifier(), true, false), Type.QUALIFIER);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			SuperFieldAccess astNode) {
		PDGGraph pdg = new PDGGraph(context, new PDGDataNode(
				null, ASTNode.THIS_EXPRESSION, "this", "super", "super"));
		pdg.mergeSequentialData(
				new PDGDataNode(astNode, ASTNode.FIELD_ACCESS, astNode.toString(),
						"super." + astNode.getName().getIdentifier(),
						astNode.getName().getIdentifier(), true, false), Type.QUALIFIER);
		return pdg;
	}

	private PDGGraph buildPDG(PDGNode control, String branch,
			ThisExpression astNode) {
		PDGGraph pdg = new PDGGraph(context, new PDGDataNode(
				astNode, astNode.getNodeType(), "this", "this",
				"this"));
		return pdg;
	}

	private void mergeSequential(PDGGraph pdg) {
		if (pdg.statementNodes.isEmpty())
			return;
		for (PDGDataNode source : new HashSet<PDGDataNode>(pdg.dataSources)) {
			HashSet<PDGDataNode> defs = defStore.get(source.key);
			if (defs != null) {
				for (PDGDataNode def : defs)
					if (def != null)
						new PDGDataEdge(def, source, Type.REFERENCE);
				if (!defs.contains(null))
					pdg.dataSources.remove(source);
			}
		}
		updateDefStore(pdg.defStore);
		for (PDGNode sink : statementSinks) {
			for (PDGNode source : pdg.statementSources) {
				new PDGDataEdge(sink, source, Type.DEPENDENCE);
			}
		}
		/*if (this.statementNodes.isEmpty() || pdg.statementNodes.isEmpty()) {
			System.err.println("Merge an empty graph!!!");
			System.exit(-1);
		}*/
		this.dataSources.addAll(pdg.dataSources);
		this.sinks.clear();
		this.statementSinks.clear();
		this.statementSinks.addAll(pdg.statementSinks);
		this.nodes.addAll(pdg.nodes);
		this.statementNodes.addAll(pdg.statementNodes);
		this.breaks.addAll(pdg.breaks);
		this.returns.addAll(pdg.returns);
		pdg.clear();
	}

	private static <E> void add(HashMap<String, HashSet<E>> target,
			HashMap<String, HashSet<E>> source) {
		for (String key : source.keySet())
			if (target.containsKey(key))
				target.get(key).addAll(new HashSet<E>(source.get(key)));
			else
				target.put(key, new HashSet<E>(source.get(key)));
	}

	private static <E> void clear(HashMap<String, HashSet<E>> map) {
		for (String key : map.keySet())
			map.get(key).clear();
		map.clear();
	}

	private static <E> void update(HashMap<String, HashSet<E>> target,
			HashMap<String, HashSet<E>> source) {
		for (String key : source.keySet()) {
			HashSet<E> s = source.get(key);
			if (s.contains(null)) {
				if (target.containsKey(key)) {
					s.remove(null);
					target.get(key).addAll(new HashSet<E>(s));
				} else
					target.put(key, new HashSet<E>(s));
			} else
				target.put(key, new HashSet<E>(s));
		}
	}

	private void clearDefStore() {
		clear(defStore);
	}

	private HashMap<String, HashSet<PDGDataNode>> copyDefStore() {
		HashMap<String, HashSet<PDGDataNode>> store = new HashMap<>();
		for (String key : defStore.keySet())
			store.put(key, new HashSet<>(defStore.get(key)));
		return store;
	}

	private void updateDefStore(HashMap<String, HashSet<PDGDataNode>> store) {
		update(defStore, store);
	}

	private void updateDefStore(HashMap<String, HashSet<PDGDataNode>> target,
			HashMap<String, Integer> defCounts,
			HashMap<String, HashSet<PDGDataNode>> source) {
		for (String key : source.keySet())
			target.put(key, new HashSet<>(source.get(key)));
		for (String key : target.keySet()) {
			int c = 1;
			if (defCounts.containsKey(key))
				c += defCounts.get(key);
			defCounts.put(key, c);
		}
	}

	private void mergeSequentialControl(PDGNode next, String label) {
		sinks.clear();
		sinks.add(next);
		statementSinks.clear();
		statementSinks.add(next);
		if (statementNodes.isEmpty())
			statementSources.add(next);
		nodes.add(next);
		statementNodes.add(next);
	}

	private void mergeSequentialData(PDGNode next, Type type) {
		if (next.isStatement())
			for (PDGNode sink : statementSinks) {
				new PDGDataEdge(sink, next, Type.DEPENDENCE);
			}
		if (type == Type.DEFINITION) {
			HashSet<PDGDataNode> ns = new HashSet<>();
			ns.add((PDGDataNode) next);
			defStore.put(next.key, ns);
		} else if (type == Type.QUALIFIER) {
			dataSources.add((PDGDataNode) next);
		} else if (type != Type.REFERENCE && next instanceof PDGDataNode) {
			HashSet<PDGDataNode> ns = defStore.get(next.key);
			if (ns != null)
				for (PDGDataNode def : ns)
					new PDGDataEdge(def, next, Type.REFERENCE);
		}
		for (PDGNode node : sinks)
			new PDGDataEdge(node, next, type);
		sinks.clear();
		sinks.add(next);
		if (nodes.isEmpty() && next instanceof PDGDataNode)
			dataSources.add((PDGDataNode) next);
		nodes.add(next);
		if (next.isStatement()) {
			statementNodes.add(next);
			if (statementSources.isEmpty())
				statementSources.add(next);
			statementSinks.clear();
			statementSinks.add(next);
		}
	}

	private void extend(PDGNode ret, PDGDataNode node, Type type) {
		HashSet<PDGDataNode> ns = new HashSet<>();
		ns.add((PDGDataNode) node);
		defStore.put(node.key, ns);
		nodes.add(node);
		sinks.remove(ret);
		sinks.add(node);
		new PDGDataEdge(ret, node, type);
	}

	private void mergeSequentialControl(PDGGraph pdg) {
		if (pdg.statementNodes.isEmpty())
			return;
		if (this.statementNodes.isEmpty() || pdg.statementNodes.isEmpty()) {
			System.err.println("Merge an empty graph!!!");
			System.exit(-1);
		}
		this.sinks.clear();
		this.statementSinks.clear();
		this.statementSinks.addAll(pdg.statementSinks);
		this.nodes.addAll(pdg.nodes);
		this.statementNodes.addAll(pdg.statementNodes);
		this.breaks.addAll(pdg.breaks);
		this.returns.addAll(pdg.returns);
		pdg.clear();
	}

	private void adjustBreakNodes(String id) {
		for (PDGNode node : new HashSet<PDGNode>(breaks)) {
			if ((node.key == null && id == null) || node.key.equals(id)) {
				sinks.add(node);
				statementSinks.add(node);
				breaks.remove(node);
			}
		}
	}

	private void adjustReturnNodes() {
		sinks.addAll(returns);
		statementSinks.addAll(returns);
		returns.clear();
		endNode = new PDGEntryNode(null, ASTNode.METHOD_DECLARATION, "END");
		for (PDGNode sink : statementSinks)
			new PDGDataEdge(sink, endNode, Type.DEPENDENCE);
		sinks.clear();
		statementSinks.clear();
		nodes.add(endNode);
		statementNodes.remove(entryNode);
	}

	private void adjustControlEdges() {
		for (PDGNode node : statementNodes) {
			ArrayList<PDGNode> ens = node.getIncomingEmptyNodes();
			if (ens.size() == 1 && node.getInDependences().size() == 1) {
				PDGNode en = ens.get(0);
				if (node.control != en.control) {
					node.control.adjustControl(node, en);
				}
			}
		}
	}

	private ArrayList<PDGDataNode> getDefinitions() {
		ArrayList<PDGDataNode> defs = new ArrayList<>();
		for (PDGNode node : sinks)
			if (node.isDefinition())
				defs.add((PDGDataNode) node);
		return defs;
	}

	private ArrayList<PDGActionNode> getReturns() {
		ArrayList<PDGActionNode> nodes = new ArrayList<>();
		for (PDGNode node : statementSinks)
			if (node.astNodeType == ASTNode.RETURN_STATEMENT)
				nodes.add((PDGActionNode) node);
		return nodes;
	}

	private PDGNode getOnlyOut() {
		if (sinks.size() == 1)
			for (PDGNode n : sinks)
				return n;
		throw new RuntimeException("ERROR in getting the only output node!!!");
//		System.err.println("ERROR in getting the only output node!!!");
//		System.exit(-1);
//		return null;
	}

	private PDGDataNode getOnlyDataOut() {
		if (sinks.size() == 1)
			for (PDGNode n : sinks)
				if (n instanceof PDGDataNode)
					return (PDGDataNode) n;
		System.err.println("ERROR in getting the only data output node!!!");
		System.exit(-1);
		return null;
	}

	private boolean isEmpty() {
		return nodes.isEmpty();
	}

	private void buildClosure() {
		HashSet<PDGNode> doneNodes = new HashSet<PDGNode>();
		for (PDGNode node : nodes)
			if (!doneNodes.contains(node))
				buildDataClosure(node, doneNodes);
		buildSequentialClosure();
		doneNodes.clear();
		doneNodes = new HashSet<PDGNode>();
		for (PDGNode node : nodes)
			if (!doneNodes.contains(node))
				buildControlClosure(node, doneNodes);
	}

	public void buildSequentialClosure() {
		HashMap<PDGNode, ArrayList<PDGNode>> preNodesOfNode = new HashMap<>();
		preNodesOfNode.put(entryNode, new ArrayList<PDGNode>());
		for (PDGNode node : nodes) {
			if (node == entryNode || node instanceof PDGControlNode) {
				HashMap<String, ArrayList<PDGNode>> branchNodes = new HashMap<>();
				branchNodes.put("T", new ArrayList<PDGNode>());
				branchNodes.put("F", new ArrayList<PDGNode>());
				branchNodes.put("", new ArrayList<PDGNode>());
				for (PDGEdge e : node.outEdges) {
					if (e.target != endNode && e.target.control != null && e.target.astNodeType != ASTNode.ASSIGNMENT)
						branchNodes.get(e.getLabel()).add(e.target);
				}
				for (String branch : branchNodes.keySet()) {
					ArrayList<PDGNode> nodes = branchNodes.get(branch);
					for (int i = 0; i < nodes.size(); i++) {
						PDGNode n = nodes.get(i);
						ArrayList<PDGNode> preNodes = new ArrayList<>();
						for (int j = 0; j < i; j++)
							preNodes.add(nodes.get(j));
						preNodesOfNode.put(n, preNodes);
					}
				}
			}
		}
		HashSet<PDGNode> doneNodes = new HashSet<>();
		doneNodes.add(entryNode);
		for (PDGNode node : preNodesOfNode.keySet()) {
			if (!doneNodes.contains(node))
				buildSequentialClosure(node, doneNodes, preNodesOfNode);
		}
		for (PDGNode node : preNodesOfNode.keySet()) {
			if (node instanceof PDGActionNode) {
				for (PDGNode preNode : preNodesOfNode.get(node)) {
					if (preNode instanceof PDGActionNode && !node.hasInNode(preNode) && ((PDGActionNode) node).hasBackwardDataDependence((PDGActionNode) preNode))
						new PDGDataEdge(preNode, node, Type.DEPENDENCE);
				}
			}
		}
	}

	private void buildSequentialClosure(PDGNode node, HashSet<PDGNode> doneNodes, HashMap<PDGNode, ArrayList<PDGNode>> preNodesOfNode) {
		if (!doneNodes.contains(node.control))
			buildSequentialClosure(node.control, doneNodes, preNodesOfNode);
		preNodesOfNode.get(node).addAll(preNodesOfNode.get(node.control));
		doneNodes.add(node);
	}

	private void buildDataClosure(PDGNode node, HashSet<PDGNode> doneNodes) {
		if (node.getDefinitions().isEmpty()) {
			for (PDGEdge e : new HashSet<PDGEdge>(node.getInEdges())) {
				if (e instanceof PDGDataEdge) {
					String label = e.getLabel();
					ArrayList<PDGNode> inNodes = e.getSource().getDefinitions();
					if (inNodes.isEmpty())
						inNodes.add(e.getSource());
					else
						for (PDGNode inNode : inNodes)
							if (!node.hasInEdge(inNode, label))
								new PDGDataEdge(inNode, node, ((PDGDataEdge) e).type);
					for (PDGNode inNode : inNodes) {
						if (!doneNodes.contains(inNode))
							buildDataClosure(inNode, doneNodes);
						for (PDGEdge e1 : inNode.getInEdges()) {
							if (e1 instanceof PDGDataEdge && !(e1.getSource() instanceof PDGDataNode)) {
								if (!node.hasInEdge(e1.getSource(), label))
									new PDGDataEdge(e1.getSource(), node, ((PDGDataEdge) e).type);
							}
						}
					}
				}
			}
		}
		doneNodes.add(node);
	}

	private void buildControlClosure(PDGNode node, HashSet<PDGNode> doneNodes) {
		for (PDGEdge e : new HashSet<PDGEdge>(node.getInEdges())) {
			if (e instanceof PDGControlEdge) {
				PDGNode inNode = e.getSource();
				if (!doneNodes.contains(inNode))
					buildControlClosure(inNode, doneNodes);
				for (PDGEdge e1 : inNode.getInEdges()) {
					if ((node instanceof PDGActionNode || node instanceof PDGControlNode) && !node.hasInEdge(e1))
						new PDGControlEdge(e1.getSource(), node, e1.getLabel());
				}
			}
		}
		doneNodes.add(node);
	}

	public void prune() {
		//pruneReturnNodes();
		delete(endNode);
		pruneDataNodes();
		pruneEmptyStatementNodes();
		pruneTemporaryDataDependence();
		//pruneIsolatedNodes();
	}

	private void pruneTemporaryDataDependence() {
		for (PDGNode node : nodes) {
			if (node == entryNode || node == endNode)
				continue;
			int i = 0;
			while (i < node.inEdges.size()) {
				PDGEdge e = node.inEdges.get(i);
				if (e.source != entryNode && e instanceof PDGDataEdge && ((PDGDataEdge) e).type == Type.DEPENDENCE) {
					node.inEdges.remove(i);
					e.source.outEdges.remove(e);
					e.source = null;
					e.target = null;
				}
				else
					i++;
			}
			i = 0;
			while (i < node.outEdges.size()) {
				PDGEdge e = node.outEdges.get(i);
				if (e.target != endNode && e instanceof PDGDataEdge && ((PDGDataEdge) e).type == Type.DEPENDENCE) {
					node.outEdges.remove(i);
					e.target.inEdges.remove(e);
					e.source = null;
					e.target = null;
				}
				else
					i++;
			}
		}
	}

	@SuppressWarnings("unused")
	private void pruneIsolatedNodes() {
		HashSet<PDGNode> isoNodes = new HashSet<PDGNode>(nodes);
		isoNodes.removeAll(getReachableNodes());
		for (PDGNode node : isoNodes)
			delete(node);
	}

	private HashSet<PDGNode> getReachableNodes() {
		HashSet<PDGNode> reachableNodes = new HashSet<>();
		Stack<PDGNode> stk = new Stack<>();
		stk.push(entryNode);
		while (!stk.isEmpty()) {
			PDGNode node = stk.pop();
			reachableNodes.add(node);
			for (PDGEdge e : node.getOutEdges())
				if (!reachableNodes.contains(e.getTarget()))
					stk.push(e.getTarget());
			for (PDGEdge e : node.getInEdges())
				if (!reachableNodes.contains(e.getSource()))
					stk.push(e.getSource());
		}
		return reachableNodes;
	}

	private void pruneDataNodes() {
		//pruneDefNodes();
		pruneDummyNodes();
	}

	private void pruneDummyNodes() {
		for (PDGNode node : new HashSet<PDGNode>(nodes)) {
			if (node instanceof PDGDataNode) {
				PDGDataNode dn = (PDGDataNode) node;
				if (dn.isDummy() && dn.isDefinition() && dn.outEdges.size() <= 1) {
					PDGNode a = dn.inEdges.get(0).source;
					PDGNode s = a.inEdges.get(0).source;
					if (s instanceof PDGDataNode) {
						PDGNode dr = dn.outEdges.get(0).target;
						if (dr.inEdges.size() == 1) {
							PDGDataEdge e = (PDGDataEdge) dr.outEdges.get(0);
							new PDGDataEdge(s, e.target, e.type);
							delete(dr);
							delete(dn);
							delete(a);
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("unused")
	private void pruneDefNodes() {
		for (PDGNode node : new HashSet<PDGNode>(nodes)) {
			if (node.isDefinition() && ((PDGDataNode) node).isDeclaration
					&& node.outEdges.isEmpty()) {
				for (PDGEdge e1 : new HashSet<PDGEdge>(node.inEdges)) {
					for (PDGEdge e2 : new HashSet<PDGEdge>(e1.source.inEdges)) {
						if (e2 instanceof PDGDataEdge
								&& ((PDGDataEdge) e2).type == Type.PARAMETER)
							delete(e2.source);
					}
					delete(e1.source);
				}
				delete(node);
			}
		}
	}

	private void pruneEmptyStatementNodes() {
		for (PDGNode node : new HashSet<PDGNode>(statementNodes)) {
			if (node.isEmptyNode()) {
				int index = node.control.getOutEdgeIndex(node);
				for (PDGEdge out : new HashSet<PDGEdge>(node.outEdges)) {
					if (out.target.control != node.control) {
						out.source.outEdges.remove(out);
						out.source = node.control;
						index++;
						out.source.outEdges.add(index, out);
					}
				}
				delete(node);
				node = null;
			}
		}
	}
	
	public void buildExasVector() {
		HashSet<PDGNode> exasGraph = new HashSet<>(), visitedNodes = new HashSet<>();
		Stack<PDGNode> stk = new Stack<>();
		stk.push(entryNode);
		visitedNodes.add(entryNode);
		while (!stk.isEmpty()) {
			PDGNode node = stk.pop();
			exasGraph.add(node);
			buildExasVector(exasGraph, node);
			for (PDGEdge e : node.getOutEdges())
				if (!visitedNodes.contains(e.getTarget())) {
					stk.push(e.getTarget());
					visitedNodes.add(e.getTarget());
				}
			for (PDGEdge e : node.getInEdges())
				if (!visitedNodes.contains(e.getSource())) {
					stk.push(e.getSource());
					visitedNodes.add(e.getSource());
				}
		}
	}

	private void buildExasVector(HashSet<PDGNode> exasGraph, PDGNode node) {
		ExasSingleFeature feature = ExasFeature.getFeature(node.getExasLabel());
		ArrayList<ExasSingleFeature> sequence = new ArrayList<ExasSingleFeature>();
		sequence.add(feature);
		backwardDFS(exasGraph, node, node, sequence);
	}
	
	private void backwardDFS(HashSet<PDGNode> exasGraph, PDGNode firstNode, PDGNode lastNode, ArrayList<ExasSingleFeature> sequence) {
		forwardDFS(exasGraph, firstNode, lastNode, sequence);
		
		if(sequence.size() < ExasFeature.MAX_LENGTH) {
			for(PDGEdge e : firstNode.getInEdgesForExasVectorization()) {
				PDGNode n = e.getSource();
				if (exasGraph.contains(n)) {
					sequence.add(0, ExasFeature.getFeature(e.getExasLabel()));
					sequence.add(0, ExasFeature.getFeature(n.getExasLabel()));
					backwardDFS(exasGraph, n, lastNode, sequence);
					sequence.remove(0);
					sequence.remove(0);
				}
			}
		}
	}

	private void forwardDFS(HashSet<PDGNode> exasGraph, PDGNode firstNode, PDGNode lastNode, ArrayList<ExasSingleFeature> sequence) {
		ExasFeature feature = ExasFeature.getFeature(sequence);
		addFeature(feature, exasVector);
		feature.setFrequency(feature.getFrequency() + 1);
		
		if(sequence.size() < ExasFeature.MAX_LENGTH) {
			for(PDGEdge e : lastNode.getOutEdgesForExasVectorization()) {
				PDGNode n = e.getTarget();
				if (exasGraph.contains(n)) {
					sequence.add(ExasFeature.getFeature(e.getExasLabel()));
					sequence.add(ExasFeature.getFeature(n.getExasLabel()));
					forwardDFS(exasGraph, firstNode, n, sequence);
					sequence.remove(sequence.size()-1);
					sequence.remove(sequence.size()-1);
				}
			}
		}
	}

	private void addFeature(ExasFeature feature, HashMap<ExasFeature, Integer> vector) {
		int c = 0;
		if (vector.containsKey(feature))
			c = vector.get(feature);
		vector.put(feature, c+1);
	}
	
	public String printVector() {
		ArrayList<ExasFeature> keys = new ArrayList<ExasFeature>(this.exasVector.keySet());
		Collections.sort(keys, new FeatureAscendingOrder());
		StringBuilder sb = new StringBuilder();
		for (ExasFeature f : keys) {
			sb.append(f + "=" + exasVector.get(f) + ' ');
		}
		return sb.toString();
	}

	public void buildChangeGraph(PDGGraph other) {
		HashMap<ASTNode, PDGNode> map = new HashMap<>();
		for (PDGNode node : other.nodes) {
			if (node.astNode != null)
				map.put(node.astNode, node);
		}
		for (PDGNode node : nodes) {
			ASTNode astNode = node.astNode;
			if (astNode != null) {
				ASTNode otherAstNode = (ASTNode) astNode.getProperty(TreedConstants.PROPERTY_MAP);
				if (otherAstNode != null) {
					PDGNode otherNode = map.get(otherAstNode);
					if (otherNode != null)
						new PDGDataEdge(otherNode, node, Type.MAP);
				}
			}
		}
		nodes.addAll(other.nodes);
		changedNodes.addAll(other.changedNodes);
	}

	public void buildChangeGraph(int version) {
		addDefinitions();
//		markChanges(entryNode.astNode);
		for (PDGNode node : nodes) {
			if (node instanceof PDGEntryNode) 
				continue;
			ASTNode astNode = node.astNode;
			if (astNode != null && JavaASTUtil.isChanged(astNode)) {
				ArrayList<PDGNode> defs = node.getDefinitions();
				if (!defs.isEmpty()) {
					changedNodes.addAll(defs);
					for (PDGNode def : defs)
						def.version = version;
				}
				/*else */{
					changedNodes.add(node);
					node.version = version;
				}
			}
		}
		prune();
		buildClosure();
		cleanUp();
		/*DotGraph dg = new DotGraph(this, false);
		String dirPath = "D:/temp";
		dg.toDotFile(new File(dirPath + "/" + "pdg.dot"));
		dg.toGraphics(dirPath + "/" + "pdg", "png");*/
	}

	private void addDefinitions() {
		HashMap<String, PDGNode> defs = new HashMap<>();
		for (PDGNode node : new HashSet<PDGNode>(nodes))
			if (node instanceof PDGDataNode && !node.isLiteral() && !node.isDefinition())
				addDefinitions((PDGDataNode) node, defs);
	}

	private void addDefinitions(PDGDataNode node, HashMap<String, PDGNode> defs) {
		if (node.getDefinitions().isEmpty() && node.getQualifier() == null) {
			PDGNode def = defs.get(node.key);
			if (def == null) {
				def = new PDGDataNode(null, node.astNodeType, node.key, node.dataType, node.dataName, true, true);
				defs.put(node.key, def);
				nodes.add(def);
			}
			new PDGDataEdge(def, node, Type.REFERENCE);
			for (PDGEdge e : node.outEdges)
				if (!def.hasOutNode(e.getTarget()))
					new PDGDataEdge(def, e.getTarget(), ((PDGDataEdge) e).type);
		}
	}

	public void cleanUp() {
		clearDefStore();
		for (PDGNode node : nodes) {
			//node.astNode = null;
			int i = 0;
			while (i < node.inEdges.size()) {
				PDGEdge e = node.inEdges.get(i);
				if (e instanceof PDGDataEdge && ((PDGDataEdge) e).type == Type.DEPENDENCE)
					node.inEdges.remove(i);
				else i++;
			}
			i = 0;
			while (i < node.outEdges.size()) {
				PDGEdge e = node.outEdges.get(i);
				if (e instanceof PDGDataEdge && ((PDGDataEdge) e).type == Type.DEPENDENCE)
					node.outEdges.remove(i);
				else i++;
			}
		}
	}

	public boolean isChangedNode(PDGNode node) {
		return this.changedNodes.contains(node);
	}

	public boolean isValid() {
		for (PDGNode node : nodes) {
			if (!node.isValid())
				return false;
		}
		return true;
	}
}
