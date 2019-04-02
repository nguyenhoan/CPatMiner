package repository;

public class ChangedFile {
	public String oldPath, newPath, oldContent, newContent;

	public ChangedFile(String newPath, String newContent, String oldPath, String oldContent) {
		this.newPath = newPath;
		this.newContent = newContent;
		this.oldPath = oldPath;
		this.oldContent = oldContent;
	}

}
