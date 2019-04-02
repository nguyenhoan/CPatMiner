package change;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import org.eclipse.jdt.core.dom.*;

/**
 * @author Nguyen Anh Hoan
 *
 */
public class VectorVisitor extends ASTVisitor {
	public static final int maxSizeOfGram = 2; // now < 8
	public static final int minFragmentSize = 30;

	public static final byte ClassFragment = 1;
	public static final byte MethodFragment = 2;
	public static final byte LoopStatementFragment = 3;
	public static final byte IfStatementFragment = 4;
	public static final byte SwitchStatementFragment = 5;
	// public static final byte BlockFragment = 6;
	public static final byte MethodState = 7;
	public static final byte DeclarationState = 8;
	public static final byte AssertState = 9;
	public static final byte AssignState = 10;

	public static final byte ArrayState = 13;
	public static final byte Expression = 14;
	public static final byte DeclarationExp = 17;
	public static final byte SimpleName = 18;
	public static final byte Literal = 19;
	public static final byte StatementsGroupFragment = 27;
	public static final byte MethodsGroupFragment = 28;
	public static final byte OtherStatementFragment = 29;
	public static final byte OtherFragments = 30;
	public static final byte NotConsideredFrags = 31;

	/**
	 * Global Maps between an n-gram and its index and vice versa
	 */
	public static HashMap<Integer, Integer> gram2Index = new HashMap<Integer, Integer>();
	public static HashMap<Integer, Integer> index2Gram = new HashMap<Integer, Integer>();

	public static String propertyVector = "vector";
	public static byte[] indexer = new byte[127]; // category of this node type
													// (Class, Method, Block,
													// Statement, ..)
	private HashMap<String, HashMap<Integer, Integer>> vectors = new HashMap<String, HashMap<Integer, Integer>>();
	/*
	 * Stack of children's n-gram vectors
	 */
	Stack<ArrayList<HashMap<Integer, Integer>>> stackChildrenVectors = new Stack<ArrayList<HashMap<Integer, Integer>>>();
	/*
	 * Stack of VERTICAL n-grams starting from the ROOT of the subtrees
	 */
	Stack<ArrayList<HashMap<Integer, Integer>>> stackChildrenRootVGrams = new Stack<ArrayList<HashMap<Integer, Integer>>>();

	int lastNode = 0; // for end token of vector
	Stack<Integer> stackCurrentNode = new Stack<Integer>(); // for start token
															// of vector

	public VectorVisitor() {

	}

	public HashMap<String, HashMap<Integer, Integer>> getVectors() {
		return vectors;
	}

	static {
		int index = 0;
		for (byte i = 0; i < indexer.length; i++) {
			if (i == 0 || i == 6 || i == 11 || i == 12 || i == 17 || i == 19
					|| i == 20 || i == 22 || i == 26 || i == 28 || i == 30
					|| i == 35 || i == 46 || i == 47 || i == 54 || i == 54
					|| i == 63 || i == 64
			// || (i>=71 && i<=83)
			// || i==52 || i==53
			// || i == 42
			// || i == ASTNode.BLOCK
			) {
				indexer[i] = NotConsideredFrags;
			} else {
				int gram = i << 24;
				gram2Index.put(gram, index);
				index2Gram.put(index++, gram);

				switch (i) {
				// case ASTNode.COMPILATION_UNIT: indexer[i] =
				// Fragment.ClassFragment; break;
				// case ASTNode.TYPE_DECLARATION_STATEMENT: indexer[i] =
				// Fragment.ClassFragment; break;
				case ASTNode.TYPE_DECLARATION:
					indexer[i] = ClassFragment;
					break;
				case ASTNode.METHOD_DECLARATION:
					indexer[i] = MethodFragment;
					break;
				// case ASTNode.BLOCK: indexer[i] = Fragment.BlockFragment;
				// break;
				case ASTNode.DO_STATEMENT:
					indexer[i] = LoopStatementFragment;
					break;
				case ASTNode.FOR_STATEMENT:
					indexer[i] = LoopStatementFragment;
					break;
				case ASTNode.ENHANCED_FOR_STATEMENT:
					indexer[i] = LoopStatementFragment;
					break;
				case ASTNode.WHILE_STATEMENT:
					indexer[i] = LoopStatementFragment;
					break;
				case ASTNode.IF_STATEMENT:
					indexer[i] = IfStatementFragment;
					break;
				// case ASTNode.SWITCH_CASE: indexer[i] =
				// Fragment.IfStatementFragment; break;
				case ASTNode.SWITCH_STATEMENT:
					indexer[i] = SwitchStatementFragment;
					break;

				case ASTNode.CONDITIONAL_EXPRESSION:
					indexer[i] = Expression;
					break;
				case ASTNode.INFIX_EXPRESSION:
					indexer[i] = Expression;
					break;
				case ASTNode.POSTFIX_EXPRESSION:
					indexer[i] = Expression;
					break;
				case ASTNode.PREFIX_EXPRESSION:
					indexer[i] = Expression;
					break;
				case ASTNode.PARENTHESIZED_EXPRESSION:
					indexer[i] = Expression;
					break;
				case ASTNode.INSTANCEOF_EXPRESSION:
					indexer[i] = Expression;
					break;

				case ASTNode.METHOD_INVOCATION:
					indexer[i] = MethodState;
					break;
				case ASTNode.SUPER_METHOD_INVOCATION:
					indexer[i] = MethodState;
					break;

				// case ASTNode.VARIABLE_DECLARATION_EXPRESSION: indexer[i] =
				// Fragment.DeclarationExp; break;
				// case ASTNode.VARIABLE_DECLARATION_FRAGMENT: indexer[i] =
				// Fragment.VarState; break;
				case ASTNode.VARIABLE_DECLARATION_STATEMENT:
					indexer[i] = DeclarationState;
					break;
				case ASTNode.FIELD_DECLARATION:
					indexer[i] = DeclarationState;
					break;

				case ASTNode.SIMPLE_NAME:
					indexer[i] = SimpleName;
					break;
				case ASTNode.BOOLEAN_LITERAL:
					indexer[i] = Literal;
					break;
				case ASTNode.CHARACTER_LITERAL:
					indexer[i] = Literal;
					break;
				case ASTNode.STRING_LITERAL:
					indexer[i] = Literal;
					break;
				case ASTNode.NUMBER_LITERAL:
					indexer[i] = Literal;
					break;

				case ASTNode.ARRAY_ACCESS:
					indexer[i] = ArrayState;
					break;
				case ASTNode.ARRAY_CREATION:
					indexer[i] = ArrayState;
					break;
				case ASTNode.ARRAY_INITIALIZER:
					indexer[i] = ArrayState;
					break;
				case ASTNode.ARRAY_TYPE:
					indexer[i] = ArrayState;
					break;

				case ASTNode.ASSERT_STATEMENT:
					indexer[i] = AssertState;
					break;
				case ASTNode.ASSIGNMENT:
					indexer[i] = AssignState;
					break;

				case ASTNode.MEMBER_REF:
					indexer[i] = OtherFragments;
					break;
				case ASTNode.METHOD_REF:
					indexer[i] = OtherFragments;
					break;
				case ASTNode.METHOD_REF_PARAMETER:
					indexer[i] = OtherFragments;
					break;
				case ASTNode.PRIMITIVE_TYPE:
					indexer[i] = OtherFragments;
					break;
				case ASTNode.QUALIFIED_NAME:
					indexer[i] = OtherFragments;
					break;
				case ASTNode.TAG_ELEMENT:
					indexer[i] = OtherFragments;
					break;
				case ASTNode.TEXT_ELEMENT:
					indexer[i] = OtherFragments;
					break;
				case ASTNode.TYPE_PARAMETER:
					indexer[i] = OtherFragments;
					break;
				default:
					indexer[i] = OtherStatementFragment;
					break;
				}
			}
		}
	}

	@Override
	public void preVisit(ASTNode node) {
		int aNode = node.getNodeType();
		if (aNode == ASTNode.JAVADOC || aNode == ASTNode.BLOCK_COMMENT
				|| aNode == ASTNode.LINE_COMMENT)
			return;

		stackCurrentNode.push(++lastNode);

		stackChildrenVectors.push(new ArrayList<HashMap<Integer, Integer>>());
		stackChildrenRootVGrams
				.push(new ArrayList<HashMap<Integer, Integer>>());
	}

	@Override
	public void postVisit(ASTNode node) {
		int aNode = node.getNodeType();
		if (aNode == ASTNode.JAVADOC || aNode == ASTNode.BLOCK_COMMENT
				|| aNode == ASTNode.LINE_COMMENT)
			return;

		buildFragment(node);
	}

	private void buildFragment(ASTNode node) {
		int nodeType = node.getNodeType();

		ArrayList<HashMap<Integer, Integer>> childrenVectors = stackChildrenVectors
				.pop();
		ArrayList<HashMap<Integer, Integer>> childrenRootVGrams = stackChildrenRootVGrams
				.pop();
		/*
		 * VERTICAL n-grams starting from this node
		 */
		HashMap<Integer, Integer> myRootVGrams = new HashMap<Integer, Integer>();

		HashMap<Integer, Integer> vector = new HashMap<Integer, Integer>();
		/*
		 * Adding vectors of all children
		 */
		if (!childrenVectors.isEmpty()) {
			vector.putAll(new HashMap<Integer, Integer>(childrenVectors.get(0)));
			for (int i = 1; i < childrenVectors.size(); i++) {
				HashMap<Integer, Integer> childVector = childrenVectors.get(i);
				for (int index : childVector.keySet()) {
					if (vector.containsKey(index))
						vector.put(index,
								(vector.get(index) + childVector.get(index)));
					else
						vector.put(index, childVector.get(index));
				}
			}
		}
		/*
		 * This node is also a single node type in the vector
		 */
		if ((indexer[nodeType] != NotConsideredFrags)) {
			// if(indexer[nodeType] <= 11) {
			int gram = nodeType << 24;
			int index = gram2Index.get(gram);
			if (vector.containsKey(index))
				vector.put(index, vector.get(index) + 1);
			else
				vector.put(index, 1);
		}
		/*
		 * This node is also a 1-gram in the vector
		 */
		if (indexer[nodeType] <= 11) {
			int gram = -indexer[nodeType];
			int tmpIndex;
			if (gram2Index.containsKey(gram))
				tmpIndex = gram2Index.get(gram);
			else {
				tmpIndex = gram2Index.size();
				gram2Index.put(gram, tmpIndex);
				index2Gram.put(tmpIndex, gram);
			}
			if (myRootVGrams.containsKey(tmpIndex))
				myRootVGrams.put(tmpIndex, myRootVGrams.get(tmpIndex) + 1);
			else
				myRootVGrams.put(tmpIndex, 1);
		}
		/*
		 * Building all n-grams starting from this node (will be used by its
		 * parent)
		 */
		if (!childrenRootVGrams.isEmpty()) {
			if (indexer[nodeType] <= 11) {
				for (HashMap<Integer, Integer> childGram : childrenRootVGrams) {
					for (int index : childGram.keySet()) {
						int gram = index2Gram.get(index);
						gram = -((indexer[nodeType] << (4 * getSizeOfGram(gram))) - gram);
						int tmpIndex;
						if (gram2Index.containsKey(gram))
							tmpIndex = gram2Index.get(gram);
						else {
							tmpIndex = gram2Index.size();
							gram2Index.put(gram, tmpIndex);
							index2Gram.put(tmpIndex, gram);
						}
						if (vector.containsKey(tmpIndex))
							vector.put(tmpIndex, vector.get(tmpIndex)
									+ childGram.get(index));
						else
							vector.put(tmpIndex, childGram.get(index));
						if (getSizeOfGram(gram) < maxSizeOfGram) {
							if (myRootVGrams.containsKey(tmpIndex))
								myRootVGrams.put(
										tmpIndex,
										myRootVGrams.get(tmpIndex)
												+ childGram.get(index));
							else
								myRootVGrams
										.put(tmpIndex, childGram.get(index));
						}
					}
				}
			} else {
				for (HashMap<Integer, Integer> childGram : childrenRootVGrams) {
					for (int index : childGram.keySet()) {
						if (myRootVGrams.containsKey(index))
							myRootVGrams.put(index, myRootVGrams.get(index)
									+ childGram.get(index));
						else
							myRootVGrams.put(index, childGram.get(index));
					}
				}
			}
		}
		/*
		 * Build the corresponding fragment
		 */
		if ((// nodeType == ASTNode.TYPE_DECLARATION ||
		nodeType == ASTNode.METHOD_DECLARATION
				|| (node.getParent() != null
						&& node.getParent().getParent() != null && node
						.getParent().getParent().getNodeType() == ASTNode.FIELD_DECLARATION)
				|| (nodeType == ASTNode.ENUM_CONSTANT_DECLARATION) || (node
				.getNodeType() == ASTNode.INITIALIZER && (node.getParent()
				.getNodeType() == ASTNode.TYPE_DECLARATION || node.getParent()
				.getParent().getNodeType() == ASTNode.TYPE_DECLARATION)))
		// && lastNode-currentNode >= Fragment.minFragmentSize
		) {
			node.setProperty(propertyVector, vector);
			/*
			 * String name = ""; if(nodeType == ASTNode.TYPE_DECLARATION) {
			 * if(node.getParent().getNodeType() == ASTNode.COMPILATION_UNIT)
			 * name = ((TypeDeclaration)node).getName().toString(); else
			 * if(node.getParent().getParent().getNodeType() ==
			 * ASTNode.COMPILATION_UNIT) name =
			 * ((TypeDeclaration)node.getParent()).getName().toString() +
			 * Util.separatorInnerClass +
			 * ((TypeDeclaration)node).getName().toString(); } else if(nodeType
			 * == ASTNode.METHOD_DECLARATION) {
			 * if(node.getParent().getNodeType() == ASTNode.TYPE_DECLARATION) {
			 * MethodDeclaration mNode = (MethodDeclaration)node; name =
			 * ((TypeDeclaration)node.getParent()).getName().toString() + "." +
			 * mNode.getName().toString() + Util.separatorParameter;
			 * 
			 * } } vectors.put(name, vector);
			 */
		}
		/*
		 * Pushing to the stacks respectively
		 */
		if (!stackChildrenVectors.isEmpty()) { // if not root
			if (!vector.isEmpty()) {
				ArrayList<HashMap<Integer, Integer>> parentVectors = stackChildrenVectors
						.pop(); // get siblings
				parentVectors.add(vector); // join them (append this node type)
				stackChildrenVectors.push(parentVectors); // back home
			}
			if (!myRootVGrams.isEmpty()) {
				ArrayList<HashMap<Integer, Integer>> parentGrams = stackChildrenRootVGrams
						.pop(); // get siblings
				parentGrams.add(myRootVGrams); // join them (append this node
												// type)
				stackChildrenRootVGrams.push(parentGrams); // back home
			}
		}
	}

	private byte getSizeOfGram(int gram) {
		byte i = 0;
		gram = Math.abs(gram);
		while (gram != 0) {
			i++;
			gram = gram / 16;
		}
		return i;
	}
}
