package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.Javadoc;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.Modifier.ModifierKeyword;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.TagElement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.UnionType;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.dom.Assignment.Operator;
import org.eclipse.jdt.core.dom.WildcardType;
import org.eclipse.jdt.internal.core.dom.NaiveASTFlattener;

import treed.TreedConstants;

public class JavaASTUtil {
	private static final HashMap<ModifierKeyword, Integer> modifierType = new HashMap<>();
	
	static {
		modifierType.put(ModifierKeyword.ABSTRACT_KEYWORD, 1);
		modifierType.put(ModifierKeyword.DEFAULT_KEYWORD, 2);
		modifierType.put(ModifierKeyword.FINAL_KEYWORD, 3);
		modifierType.put(ModifierKeyword.NATIVE_KEYWORD, 4);
		modifierType.put(ModifierKeyword.PRIVATE_KEYWORD, 2);
		modifierType.put(ModifierKeyword.PROTECTED_KEYWORD, 2);
		modifierType.put(ModifierKeyword.PUBLIC_KEYWORD, 2);
		modifierType.put(ModifierKeyword.STATIC_KEYWORD, 5);
		modifierType.put(ModifierKeyword.STRICTFP_KEYWORD, 6);
		modifierType.put(ModifierKeyword.SYNCHRONIZED_KEYWORD, 7);
		modifierType.put(ModifierKeyword.TRANSIENT_KEYWORD, 8);
		modifierType.put(ModifierKeyword.VOLATILE_KEYWORD, 7);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ASTNode parseSource(String source) {
		Map options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_7);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
		ASTParser parser = ASTParser.newParser(AST.JLS4);
    	parser.setSource(source.toCharArray());
    	parser.setCompilerOptions(options);
    	ASTNode ast = parser.createAST(null);
		return ast;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ASTNode parseSource(String source, String name) {
		Map options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_7);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
		ASTParser parser = ASTParser.newParser(AST.JLS4);
    	parser.setSource(source.toCharArray());
    	parser.setCompilerOptions(options);
		parser.setEnvironment(
				new String[]{}, 
				new String[]{}, 
				new String[]{}, 
				true);
		parser.setResolveBindings(true);
		parser.setBindingsRecovery(true);
    	parser.setUnitName(name);
    	ASTNode ast = parser.createAST(null);
		return ast;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static ASTNode parseSource(String source, int kind) {
		Map options = JavaCore.getOptions();
		options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_1_7);
		options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_1_7);
		options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_1_7);
		ASTParser parser = ASTParser.newParser(AST.JLS4);
    	parser.setSource(source.toCharArray());
    	parser.setCompilerOptions(options);
    	parser.setKind(kind);
    	ASTNode ast = parser.createAST(null);
		return ast;
	}
	
	public static String getSource(ASTNode node) {
		NaiveASTFlattener flatterner = new NaiveASTFlattener();
		node.accept(flatterner);
		return flatterner.getResult();
	}

	public static boolean isLiteral(int astNodeType) {
		return ASTNode.nodeClassForType(astNodeType).getSimpleName().endsWith("Literal");
	}

	public static boolean isLiteral(ASTNode node) {
		int type = node.getNodeType();
		if (type == ASTNode.BOOLEAN_LITERAL || 
				type == ASTNode.CHARACTER_LITERAL || 
				type == ASTNode.NULL_LITERAL || 
				type == ASTNode.NUMBER_LITERAL || 
				type == ASTNode.STRING_LITERAL)
			return true;
		if (type == ASTNode.PREFIX_EXPRESSION) {
			PrefixExpression pe = (PrefixExpression) node;
			return isLiteral(pe.getOperand());
		}
		if (type == ASTNode.POSTFIX_EXPRESSION) {
			PostfixExpression pe = (PostfixExpression) node;
			return isLiteral(pe.getOperand());
		}
		if (type == ASTNode.PARENTHESIZED_EXPRESSION) {
			ParenthesizedExpression pe = (ParenthesizedExpression) node;
			return isLiteral(pe.getExpression());
		}
		
		return false;
	}

	public static boolean isPublic(MethodDeclaration declaration) {
		for (int i = 0; i < declaration.modifiers().size(); i++) {
			Modifier m = (Modifier) declaration.modifiers().get(i);
			if (m.isPublic())
				return true;
		}
		return false;
	}

	public static String buildSignature(MethodDeclaration method) {
		NaiveASTFlattener flatterner = new NaiveASTFlattener() {
			@Override
			public boolean visit(MethodDeclaration node) {
				return super.visit(node);
			}
			
			@Override
			public boolean visit(Javadoc node) {
				return false;
			}
			
			@Override
			public boolean visit(Block node) {
				return false;
			}
			@Override
			public boolean visit(ExpressionStatement node) {
				node.getExpression().accept(this);
				return false;
			}
		};
		method.accept(flatterner);
		return flatterner.getResult();
	}

	public static String getSimpleType(VariableDeclarationFragment f) {
		ASTNode p = f.getParent();
		if (p instanceof FieldDeclaration)
			return getSimpleType(((FieldDeclaration) p).getType());
		if (p instanceof VariableDeclarationStatement)
			return getSimpleType(((VariableDeclarationStatement) p).getType());
		if (p instanceof VariableDeclarationExpression)
			return getSimpleType(((VariableDeclarationExpression) p).getType());
		throw new UnsupportedOperationException("Get type of a declaration!!!");
	}

	public static String getSimpleType(Type type) {
		if (type.isArrayType()) {
			ArrayType t = (ArrayType) type;
			return getSimpleType(t.getElementType()) + getDimensions(t.getDimensions());
			//return type.toString();
		} else if (type.isParameterizedType()) {
			ParameterizedType t = (ParameterizedType) type;
			return getSimpleType(t.getType());
		} else if (type.isPrimitiveType()) {
			String pt = type.toString();
			if (pt.equals("byte") || pt.equals("short") || pt.equals("int") || pt.equals("long") 
					|| pt.equals("float") || pt.equals("double"))
				return "number";
			return pt;
		} else if (type.isQualifiedType()) {
			QualifiedType t = (QualifiedType) type;
			return t.getName().getIdentifier();
		} else if (type.isSimpleType()) {
			String pt = type.toString();
			if (pt.equals("Byte") || pt.equals("Short") || pt.equals("Integer") || pt.equals("Long") 
					|| pt.equals("Float") || pt.equals("Double"))
				return "number";
			return pt;
		} /*else if (type.isIntersectionType()) {
			IntersectionType it = (IntersectionType) type;
			@SuppressWarnings("unchecked")
			ArrayList<Type> types = new ArrayList<>(it.types());
			String s = getSimpleType(types.get(0));
			for (int i = 1; i < types.size(); i++)
				s += "&" + getSimpleType(types.get(i));
			return s;
		} */else if (type.isUnionType()) {
			UnionType it = (UnionType) type;
			@SuppressWarnings("unchecked")
			ArrayList<Type> types = new ArrayList<>(it.types());
			String s = getSimpleType(types.get(0));
			for (int i = 1; i < types.size(); i++)
				s += "|" + getSimpleType(types.get(i));
			return s;
		} else if (type.isWildcardType()) {
			WildcardType wt = (WildcardType) type;
			if (wt.getBound() == null)
				return "Object";
			return getSimpleType(wt.getBound());
		} /*else if (type.isNameQualifiedType()) {
			NameQualifiedType nqt = (NameQualifiedType) type;
			return nqt.getName().getIdentifier();
		} else if (type.isAnnotatable()) {
			return type.toString();
		}*/
		throw new IllegalArgumentException("Declare a variable with unknown type!!!");
	}

	public static String getQualifiedType(Type type) {
		if (type.isArrayType()) {
			ArrayType t = (ArrayType) type;
			return getQualifiedType(t.getElementType()) + getDimensions(t.getDimensions());
			//return type.toString();
		} else if (type.isParameterizedType()) {
			ParameterizedType t = (ParameterizedType) type;
			return getQualifiedType(t.getType());
		} else if (type.isPrimitiveType()) {
			return type.toString();
		} else if (type.isQualifiedType()) {
			QualifiedType qt = (QualifiedType) type;
			return getQualifiedType(qt.getQualifier()) + "." + qt.getName().getIdentifier();
		} else if (type.isSimpleType()) {
			return type.toString();
		} /*else if (type.isIntersectionType()) {
			IntersectionType it = (IntersectionType) type;
			@SuppressWarnings("unchecked")
			ArrayList<Type> types = new ArrayList<>(it.types());
			String s = getQualifiedType(types.get(0));
			for (int i = 1; i < types.size(); i++)
				s += "&" + getQualifiedType(types.get(i));
			return s;
		} */else if (type.isUnionType()) {
			UnionType it = (UnionType) type;
			@SuppressWarnings("unchecked")
			ArrayList<Type> types = new ArrayList<>(it.types());
			String s = getQualifiedType(types.get(0));
			for (int i = 1; i < types.size(); i++)
				s += "|" + getQualifiedType(types.get(i));
			return s;
		} else if (type.isWildcardType()) {
			WildcardType wt = (WildcardType) type;
			if (wt.getBound() == null)
				return "Object";
			return getQualifiedType(wt.getBound());
		} /*else if (type.isNameQualifiedType()) {
			NameQualifiedType nqt = (NameQualifiedType) type;
			return nqt.getQualifier().getFullyQualifiedName() + "." + nqt.getName().getIdentifier();
		} else if (type.isAnnotatable()) {
			return type.toString();
		}*/
		throw new IllegalArgumentException("Declare a variable with unknown type!!!");
	}

	public static String getSimpleType(Type type, HashSet<String> typeParameters) {
		if (type.isArrayType()) {
			ArrayType t = (ArrayType) type;
			return getSimpleType(t.getElementType(), typeParameters) + getDimensions(t.getDimensions());
			//return type.toString();
		} else if (type.isParameterizedType()) {
			ParameterizedType t = (ParameterizedType) type;
			return getSimpleType(t.getType(), typeParameters);
		} else if (type.isPrimitiveType()) {
			return type.toString();
		} else if (type.isQualifiedType()) {
			QualifiedType t = (QualifiedType) type;
			return t.getName().getIdentifier();
		} else if (type.isSimpleType()) {
			if (typeParameters.contains(type.toString()))
				return "Object";
			return type.toString();
		} /*else if (type.isIntersectionType()) {
			IntersectionType it = (IntersectionType) type;
			@SuppressWarnings("unchecked")
			ArrayList<Type> types = new ArrayList<>(it.types());
			String s = getSimpleType(types.get(0), typeParameters);
			for (int i = 1; i < types.size(); i++)
				s += "&" + getSimpleType(types.get(i), typeParameters);
			return s;
		} */else if (type.isUnionType()) {
			UnionType it = (UnionType) type;
			@SuppressWarnings("unchecked")
			ArrayList<Type> types = new ArrayList<>(it.types());
			String s = getSimpleType(types.get(0), typeParameters);
			for (int i = 1; i < types.size(); i++)
				s += "|" + getSimpleType(types.get(i), typeParameters);
			return s;
		} else if (type.isWildcardType()) {
			WildcardType wt = (WildcardType) type;
			if (wt.getBound() == null)
				return "Object";
			return getSimpleType(wt.getBound(), typeParameters);
		} /*else if (type.isNameQualifiedType()) {
			NameQualifiedType nqt = (NameQualifiedType) type;
			return nqt.getName().getIdentifier();
		} else if (type.isAnnotatable()) {
			return type.toString();
		}*/
		throw new IllegalArgumentException("Declare a variable with unknown type!!!");
	}

	private static String getDimensions(int dimensions) {
		String s = "";
		for (int i = 0; i < dimensions; i++)
			s += "[]";
		return s;
	}

	public static String getInfixOperator(Operator operator) {
		if (operator == Operator.ASSIGN)
			return null;
		String op = operator.toString();
		return op.substring(0, op.length() - 1);
	}
	
	public static TypeDeclaration getType(TypeDeclaration td, String name) {
		for (TypeDeclaration inner : td.getTypes())
			if (inner.getName().getIdentifier().equals(name))
				return inner;
		return null;
	}

	public static boolean hasSameSignature(MethodDeclaration md, IMethod imethod) {
		if (md.getName().getIdentifier().equals(imethod.getElementName())) {
			if (md.parameters().size() == imethod.getNumberOfParameters()) {
				for (int i = 0; i < md.parameters().size(); i++) {
					SingleVariableDeclaration d = (SingleVariableDeclaration) md.parameters().get(i);
					String type = Signature.createTypeSignature(d.getType().toString(), false);
					if (!type.equals(imethod.getParameterTypes()[i]))
						return false;
				}
				return true;
			}
		}
		return false;
	}

	public static boolean isDeprecated(MethodDeclaration method) {
		Javadoc doc = method.getJavadoc();
		if (doc != null) {
			for (int i = 0; i < doc.tags().size(); i++) {
				TagElement tag = (TagElement) doc.tags().get(i);
				if (tag.getTagName() != null && tag.getTagName().toLowerCase().equals("@deprecated"))
					return true;
			}
		}
		return false;
	}

	public static int countLeaves(ASTNode node) {
		class LeaveCountASTVisitor extends ASTVisitor {
			private Stack<Integer> numOfChildren = new Stack<Integer>();
			private int numOfLeaves = 0;
			
			public LeaveCountASTVisitor() {
				numOfChildren.push(0);
			}
			
			@Override
			public void preVisit(ASTNode node) {
				int n = numOfChildren.pop();
				numOfChildren.push(n + 1);
				numOfChildren.push(0);
			}
			
			@Override
			public void postVisit(ASTNode node) {
				int n = numOfChildren.pop();
				if (n == 0)
					numOfLeaves++;
			}
		};
		LeaveCountASTVisitor v = new LeaveCountASTVisitor();
		node.accept(v);
		return v.numOfLeaves;
	}

	public static ArrayList<String> tokenizeNames(ASTNode node) {
		return new ASTVisitor() {
			private ArrayList<String> names = new ArrayList<>();
			
			@Override
			public boolean visit(org.eclipse.jdt.core.dom.SimpleName node) {
				names.add(node.getIdentifier());
				return false;
			};
		}.names;
	}

	public static HashSet<String> getComputationDatas(ASTNode e) {
		class DataCollectingASTVisitor extends ASTVisitor {
			private HashSet<String> datas = new HashSet<>();
			
			@Override
			public boolean visit(FieldAccess node) {
				datas.add(node.toString());
				return false;
			}
			
			@Override
			public boolean visit(ArrayAccess node) {
				datas.add(node.toString());
				return false;
			}
			
			@Override
			public boolean visit(MethodInvocation node) {
				datas.add(node.toString());
				return false;
			}
			
			@Override
			public boolean visit(QualifiedName node) {
				datas.add(node.toString());
				return false;
			}
			
			@Override
			public boolean visit(SimpleName node) {
				datas.add(node.toString());
				return false;
			}
			
			@Override
			public boolean visit(SuperFieldAccess node) {
				datas.add(node.toString());
				return false;
			}
			
			@Override
			public boolean visit(SuperMethodInvocation node) {
				datas.add(node.toString());
				return false;
			}
			
			@Override
			public boolean visit(AnonymousClassDeclaration node) {
				return false;
			}
			
			@Override
			public boolean visit(MethodDeclaration node) {
				return false;
			}
			
			@Override
			public boolean visit(QualifiedType node) {
				return false;
			}
			
			@Override
			public boolean visit(TypeDeclaration node) {
				return false;
			}
		};
		DataCollectingASTVisitor visitor = new DataCollectingASTVisitor();
		e.accept(visitor);
		return visitor.datas;
	}

	public static HashSet<String> getConditionDatas(ASTNode e) {
		class DataCollectingASTVisitor extends ASTVisitor {
			private HashSet<String> datas = new HashSet<>();
			
			@Override
			public boolean visit(FieldAccess node) {
				datas.add(node.toString());
				return false;
			}
			
			@Override
			public boolean visit(ArrayAccess node) {
				datas.add(node.toString());
				return false;
			}
			
			@Override
			public boolean visit(MethodInvocation node) {
				datas.add(node.toString());
				if (node.getExpression() != null) node.getExpression().accept(this);
				for (Iterator<?> it = node.arguments().iterator(); it.hasNext(); ) {
					Expression e = (Expression) it.next();
					e.accept(this);
				}
				return false;
			}
			
			@Override
			public boolean visit(QualifiedName node) {
				datas.add(node.toString());
				return false;
			}
			
			@Override
			public boolean visit(SimpleName node) {
				datas.add(node.toString());
				return false;
			}
			
			@Override
			public boolean visit(SuperFieldAccess node) {
				datas.add(node.toString());
				return false;
			}
			
			@Override
			public boolean visit(SuperMethodInvocation node) {
				datas.add(node.toString());
				for (Iterator<?> it = node.arguments().iterator(); it.hasNext(); ) {
					Expression e = (Expression) it.next();
					e.accept(this);
				}
				return false;
			}
			
			@Override
			public boolean visit(AnonymousClassDeclaration node) {
				return false;
			}
			
			@Override
			public boolean visit(MethodDeclaration node) {
				return false;
			}
			
			@Override
			public boolean visit(QualifiedType node) {
				return false;
			}
			
			@Override
			public boolean visit(TypeDeclaration node) {
				return false;
			}
		};
		DataCollectingASTVisitor visitor = new DataCollectingASTVisitor();
		e.accept(visitor);
		return visitor.datas;
	}

	public static Expression getConditionExpression(ASTNode sp) {
		int type = sp.getNodeType();
		if (type == ASTNode.CONDITIONAL_EXPRESSION) {
			return ((ConditionalExpression) sp).getExpression();
		}
		if (type == ASTNode.DO_STATEMENT) {
			return ((DoStatement) sp).getExpression();
		}
		if (type == ASTNode.FOR_STATEMENT) {
			return ((ForStatement) sp).getExpression();
		}
		if (type == ASTNode.IF_STATEMENT) {
			return ((IfStatement) sp).getExpression();
		}
		if (type == ASTNode.WHILE_STATEMENT) {
			return ((WhileStatement) sp).getExpression();
		}
		if (type == ASTNode.ENHANCED_FOR_STATEMENT) {
			return ((EnhancedForStatement) sp).getExpression();
		}
		if (type == ASTNode.SWITCH_STATEMENT) {
			return ((SwitchStatement) sp).getExpression();
		}
		return null;
	}
	
	public static boolean isChanged(ASTNode astNode) {
		if (astNode.getProperty(TreedConstants.PROPERTY_STATUS) == null)
			return false;
		int status = (Integer) astNode.getProperty(TreedConstants.PROPERTY_STATUS);
		if (status > TreedConstants.STATUS_UNCHANGED)
			return true;
		ASTNode p = astNode.getParent();
		if (p instanceof ExpressionStatement) {
			status = (int) p.getProperty(TreedConstants.PROPERTY_STATUS);
			if (status > TreedConstants.STATUS_UNCHANGED)
				return true;
		}
		return false;
	}

	public static String getParamterTypeNames(MethodDeclaration md) {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		if (md.parameters().size() > 0) {
			sb.append(getParameterTypeName((SingleVariableDeclaration) md.parameters().get(0)));
		}
		sb.append(")");
		return sb.toString();
	}

	private static String getParameterTypeName(SingleVariableDeclaration d) {
		return d.getType().resolveBinding().getErasure().getQualifiedName();
	}

	public static ASTNode getNode(ASTNode ast, int start, int end) {
		ASTNode[] results = new ASTNode[1];
		ASTVisitor v = new ASTVisitor(true) {
			@Override
			public boolean preVisit2(ASTNode node) {
				if (results[0] != null)
					return false;
				if (node.getStartPosition() > start || node.getStartPosition() + node.getLength() < end)
					return false;
				return true;
			}
			
			@Override
			public void postVisit(ASTNode node) {
				if (results[0] == null && node.getStartPosition() <= start && node.getStartPosition() + node.getLength() >= end)
					results[0] = node;
			}
		};
		ast.accept(v);
		return results[0];
	}

	public static int getType(Modifier mn) {
		return modifierType.get(mn.getKeyword());
	}
}
