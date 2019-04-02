package utils;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by michaelhilton on 7/19/16.
 */
public class JGitUtil {

    /**
     * Snippet which shows how to use RevWalk and TreeWalk to read the contents
     * of a specific file from a specific commit.
     *
     * @author dominik.stadler at gmx.at
     *         <p>
     *         Source: https://github.com/centic9/jgit-cookbook/blob/master/src/main/java/org/dstadler/jgit/api/ReadFileFromCommit.java
     */
    public static ArrayList<String> getFileFromDir(File project, String name) throws IOException, GitAPIException {
//        System.out.print("\rPROCESSING PROJECT: " + project.getName() + "                                                   \n");
        GitConnector gc = new GitConnector(project + "/.git");
        ArrayList<String> beforeAndAfter = null;
        if (gc.connect()) {
            String[] nameParts = name.split(",");
            String commitID = nameParts[0];
            String fileName = nameParts[1];
            beforeAndAfter = gc.getFileFromCommit(commitID, fileName);
            gc.close();
        }
        return beforeAndAfter;
    }


}


