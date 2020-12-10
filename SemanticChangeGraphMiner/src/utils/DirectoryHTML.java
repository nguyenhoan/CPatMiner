package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by michaelhilton on 2/11/16.
 */
public class DirectoryHTML {

    public void write(HashMap<Integer, ArrayList<ArrayList<String>>> foundPatterns, File dir) {
        //ADD helper Files
        try {
            Files.copy(Paths.get("SemanticChangeGraphMiner/src/resources/sortable.js"),Paths.get("output/patterns/sortable.js"), StandardCopyOption.REPLACE_EXISTING );
            Files.copy(Paths.get("SemanticChangeGraphMiner/src/resources/jquery.dataTables.min.js"),Paths.get("output/patterns/jquery.dataTables.min.js"), StandardCopyOption.REPLACE_EXISTING );
            Files.copy(Paths.get("SemanticChangeGraphMiner/src/resources/jquery.dataTables.min.css"),Paths.get("output/patterns/jquery.dataTables.min.css"), StandardCopyOption.REPLACE_EXISTING );
            Files.copy(Paths.get("SemanticChangeGraphMiner/src/resources/jquery-2.2.3.min.js"),Paths.get("output/patterns/jquery-2.2.3.min.js"), StandardCopyOption.REPLACE_EXISTING );
            Files.copy(Paths.get("SemanticChangeGraphMiner/src/resources/default.css"),Paths.get("output/patterns/default.css"), StandardCopyOption.REPLACE_EXISTING );
            Files.copy(Paths.get("SemanticChangeGraphMiner/src/resources/highlight.pack.js"),Paths.get("output/patterns/highlight.pack.js"), StandardCopyOption.REPLACE_EXISTING );
        } catch (IOException e) {
            e.printStackTrace();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<HTML><H1>Patterns That Have Been Found:</H1><body>");
//        sb.append("<link rel=\"stylesheet\" href=\"../../sortable-theme-minimal.css\" />\n" +
//                "<script src=\"../../sortable.js\"></script>");


        sb.append("<link rel=\"stylesheet\" href=\"../../jquery.dataTables.min.css\" />\n" +
                " <script src=\"../../jquery-2.2.3.min.js\"></script>\n"+
                "<script src=\"../../jquery.dataTables.min.js\"></script>");

        sb.append("<table id=\"sorttable\" class=\"sortable-theme-minimal\" data-sortable>\n");
        sb.append("<thead><tr>");
        sb.append("<th class=\"sort\" data-sort=\"id\">ID</th>");
        sb.append("<th class=\"sort\" data-sort=\"size\">Pattern Size</th>");
        sb.append("<th class=\"sort\" data-sort=\"numFound\">NumberFound</th>");
        sb.append("<th class=\"sort\" data-sort=\"details\">details</th>");
        sb.append("<th class=\"sort\" data-sort=\"nodeTypes\">NodeTypes</th>");


        sb.append("</tr></thead>");
        for(Map.Entry<Integer, ArrayList<ArrayList<String>>> patternList : foundPatterns.entrySet()) {
            if(patternList.getValue().size()>0) {
            }
            for(ArrayList<String> pattern : patternList.getValue()){
                sb.append("<tr><td class=\"id\">");

                sb.append(pattern.get(0));
                sb.append("</td><td class=\"size\">");
                sb.append(pattern.get(1));
                sb.append("</td>");

                sb.append("<td class=\"numFound\">"+pattern.get(4) + "</td>");
                sb.append("</td><td class=\"details\"><a href='");
                sb.append(pattern.get(2));
                sb.append("/details.html");
                sb.append("'>locations</a>");
                sb.append("</td><td class=\"nodeTypes\">");
                sb.append(pattern.get(5));
                sb.append("</td>");
                sb.append("</td></tr>");
            }

        }

        sb.append("</table>");

        sb.append("\n" +
                "<script>\n" +
                "$(document).ready(function(){\n" +
                "    $('#myTable').DataTable(\n" +
                "    \t{\n" +
                "        \"lengthMenu\": [[-1, 25, 50, 100], [\"All\", 25, 50, 100]]\n" +
                "    } \n" +
                "    \t);\n" +
                "});\n" +
                "</script>");

        FileIO.writeStringToFile(sb.toString(), dir + "/directory.html");
    }
}
