package change;

import java.io.Serializable;
import java.util.List;

public class CRevision implements Serializable {
	private static final long serialVersionUID = 8519380253593912513L;

	long id;
	int numOfFiles = 0;
	List<CSourceFile> files;
	List<CMethod> methods;
	List<CInitializer> inits;

	public long getId() {
		return id;
	}

	public int getNumOfFiles() {
		return numOfFiles;
	}

	public List<CSourceFile> getFiles() {
		return files;
	}

	public List<CMethod> getMethods() {
		return methods;
	}

	public List<CInitializer> getInits() {
		return inits;
	}

}
