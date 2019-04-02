package misc;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import utils.FileIO;
import utils.JavaASTUtil;

public class ListAPINames {
	private static HashSet<String> javalangnames = new HashSet<>();
	private static final HashMap<String, ArrayList<String>> allNames = new HashMap<>(), superNames = new HashMap<>();
	private static int numOfMethods = 0, numOfDocMethods = 0;
	
	static {
		String content = FileIO.readStringFromFile("java.lang.txt");
		Scanner sc = new Scanner(content);
		while (sc.hasNextLine()) {
			javalangnames.add(sc.nextLine());
			
		}
		sc.close();
	}
	
	public static void main(String[] args) {
		//String path = "D:/data/jdk6/j2se/src/share/classes";
		//String path = "D:/data/jdk7";
		//String path = "D:/data/jdk8";
		String path = "D:/data/apache";
		System.out.println(path);
		parse(new File(path));
		parseSupers(new File(path));
		ArrayList<String> l = new ArrayList<>(allNames.keySet());
		Collections.sort(l);
		StringBuilder sb = new StringBuilder();
		for (String c : l) {
			sb.append(c);
			ArrayList<String> names = superNames.get(c);
			sb.append(";" + names.get(0));
			sb.append(";" + names.get(1));
			for (int i = 2; i < names.size(); i++)
				sb.append("," + names.get(i));
			for (String n : allNames.get(c))
				sb.append(";" + n);
			sb.append("\n");
		}
		System.out.println(numOfDocMethods + " / " + numOfMethods);
		//FileIO.writeStringToFile(sb.toString(), "D:/Projects/ChangeStatICSE2016/data/api names/jdk7.csv");
		//FileIO.writeStringToFile(sb.toString(), "D:/Projects/ChangeStatICSE2016/data/api names/apache.csv");
	}

	private static void parse(File file) {
		if (file.isDirectory())
			for (File sub : file.listFiles())
				parse(sub);
		else if (file.getName().endsWith(".java")){
			ASTNode ast = JavaASTUtil.parseSource(FileIO.readStringFromFile(file.getAbsolutePath()));
			CompilationUnit cu = (CompilationUnit) ast;
			if (cu.getPackage() != null) {
				String pkg = cu.getPackage().getName().getFullyQualifiedName();
				int index = pkg.indexOf('.');
				if (index < 0) return;
				String prefix = pkg.substring(0, index);
				if (prefix.length() < 3) return;
				if (prefix.charAt(0) < 'a' || prefix.charAt(0) > 'z')
				if (cu.types() == null) return;
				for (int i = 0; i < cu.types().size(); i++) {
					if (cu.types().get(i) instanceof TypeDeclaration)
						parse(pkg + ".", (TypeDeclaration) cu.types().get(i));
					else if (cu.types().get(i) instanceof EnumDeclaration)
						parse(pkg + ".", (EnumDeclaration) cu.types().get(i));
					else if (cu.types().get(i) instanceof AnnotationTypeDeclaration)
						parse(pkg + ".", (AnnotationTypeDeclaration) cu.types().get(i));
				}
			}
		}
	}

	private static void parseSupers(File file) {
		if (file.isDirectory())
			for (File sub : file.listFiles())
				parseSupers(sub);
		else if (file.getName().endsWith(".java")){
			ASTNode ast = JavaASTUtil.parseSource(FileIO.readStringFromFile(file.getAbsolutePath()));
			CompilationUnit cu = (CompilationUnit) ast;
			if (cu.getPackage() != null) {
				String pkg = cu.getPackage().getName().getFullyQualifiedName();
				int index = pkg.indexOf('.');
				if (index < 0) return;
				String prefix = pkg.substring(0, index);
				if (prefix.length() < 3) return;
				if (prefix.charAt(0) < 'a' || prefix.charAt(0) > 'z')
				if (cu.types() == null) return;
				for (int i = 0; i < cu.types().size(); i++) {
					if (cu.types().get(i) instanceof TypeDeclaration)
						parseSupers(pkg + ".", (TypeDeclaration) cu.types().get(i));
					else if (cu.types().get(i) instanceof EnumDeclaration)
						parseSupers(pkg + ".", (EnumDeclaration) cu.types().get(i));
					else if (cu.types().get(i) instanceof AnnotationTypeDeclaration)
						parseSupers(pkg + ".", (AnnotationTypeDeclaration) cu.types().get(i));
				}
			}
		}
	}

	private static void parseSupers(String qualifier, AnnotationTypeDeclaration ad) {
		// TODO
	}

	private static void parseSupers(String qualifier, EnumDeclaration ed) {
		// TODO
	}

	private static void parseSupers(String qualifier, TypeDeclaration td) {
		String cname = qualifier + td.getName().getIdentifier();
		ArrayList<String> names = new ArrayList<>();
		if (td.isInterface())
			names.add("interface");
		else if (Modifier.isAbstract(td.getModifiers()))
			names.add("abstract class");
		else
			names.add("class");
		String superClassName = null;
		if (td.getSuperclassType() != null) {
			superClassName = JavaASTUtil.getQualifiedType(td.getSuperclassType());
			superClassName = getFullyQualifiedType(cname, superClassName, (CompilationUnit) td.getRoot());
		}
		names.add(superClassName);
		for (int i = 0; i < td.superInterfaceTypes().size(); i++) {
			Type type = (Type) td.superInterfaceTypes().get(0);
			String interfaceName = JavaASTUtil.getQualifiedType(type);
			interfaceName = getFullyQualifiedType(cname, interfaceName, (CompilationUnit) td.getRoot());
			names.add(interfaceName);
		}
		superNames.put(cname, names);
		for (TypeDeclaration inner : td.getTypes())
			parseSupers(cname + ".", inner);
	}

	private static void parse(String qualifier, AnnotationTypeDeclaration ad) {
		// TODO
	}

	private static void parse(String qualifier, EnumDeclaration ed) {
		// TODO
	}

	private static void parse(String qualifier, TypeDeclaration td) {
		String cname = qualifier + td.getName().getIdentifier();
		ArrayList<String> mnames = new ArrayList<>();
		mnames.add(td.getName().getIdentifier());
		for (MethodDeclaration md : td.getMethods()) {
			numOfMethods++;
			if (md.getJavadoc() != null && md.getJavadoc().getLength() > 10)
				numOfDocMethods++;
			String mname = md.getName().getIdentifier() + "(";
			//if (!md.isConstructor())
			for(int i = 0; i < md.parameters().size(); i++) {
				SingleVariableDeclaration dec = (SingleVariableDeclaration) md.parameters().get(i);
				String paraType = JavaASTUtil.getSimpleType(dec.getType());
				String temp = dec.toString();
				int l = temp.length();
				while(temp.endsWith("[]")) {
					paraType += "[]";
					temp = temp.substring(0, l-2);
					l -= 2;
				}
				mname += (i == 0 ? "" : ",") + paraType;
			}
			mname += ")";
			mnames.add(mname);
		}
		allNames.put(cname, mnames);
		for (TypeDeclaration inner : td.getTypes())
			parse(cname + ".", inner);
	}

	public static String getFullyQualifiedType(String name, String superName, CompilationUnit cu) {
		if (allNames.containsKey(superName))
			return superName;
		if (javalangnames.contains("java.lang." + superName))
			return "java.lang." + superName;
		String pkg = cu.getPackage().getName().getFullyQualifiedName();
		int index = name.length();
		do {
			index = name.lastIndexOf('.', index - 1);
			String prefix = name.substring(0, index);
			if (allNames.containsKey(prefix + "." + superName))
				return prefix + "." + superName;
		} while (index > pkg.length());
		for (int i = 0; i < cu.imports().size(); i++) {
			ImportDeclaration id = (ImportDeclaration) cu.imports().get(i);
			String idn = id.getName().getFullyQualifiedName();
			if (id.isOnDemand()) {
				if (allNames.containsKey(idn + "." + superName))
					return idn + "." + superName;
			}
			else {
				if (idn.endsWith("." + superName))
					return idn;
			}
		}
		System.out.println(superName);
		return superName;
	}

}
