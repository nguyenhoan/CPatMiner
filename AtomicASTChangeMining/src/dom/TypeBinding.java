package dom;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

public class TypeBinding {
	private String type;
	
	public TypeBinding(String type) {
		this.type = type;
	}

	public static TypeBinding resolve(ASTNode exp) {
		int type = exp.getNodeType();
		switch (type) {
		case ASTNode.CLASS_INSTANCE_CREATION:
			return new TypeBinding(((ClassInstanceCreation) exp).getType().toString());
		}
		return new TypeBinding("UNKNOWN");
	}

	public String getQualifiedName() {
		return type;
	}
	
	public String getSimpleName() {
		return type.substring(type.lastIndexOf('.') + 1);
	}

	public static TypeBinding resolve(SingleVariableDeclaration d) {
		return new TypeBinding(d.getType().toString());
	}
}
