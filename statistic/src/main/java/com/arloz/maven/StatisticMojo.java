package com.arloz.maven;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Goal which count source code file
 *
 * @goal sourceCount
 * 
 * @phase process-sources
 */
public class StatisticMojo extends AbstractMojo {
    /** count result output file name */
    private String                resultFileName   = "source_count.txt";

    /**
     * Location of the file.
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File                  outputDirectory;

    private static final String[] INCLUDES_DEFAULT = { "java", "xml", "sql", "properties" };
    private static final String[] RATIOS_DEFAULT   = { "1.0", "1.0", "0.25", "0.25" };
    private static final String   DOT              = ".";
    /**
     * @parameter expression="${project.basedir}"
     * @required
     * @readonly
     */
    private File                  basedir;
    /**
     * @parameter expression="${project.build.sourceDirectory}"
     * @required
     * @readonly
     */
    private File                  sourcedir;
    /**
     * @parameter expression="${project.build.testSourceDirectory}"
     * @required
     * @readonly
     */
    private File                  testSourcedir;
    /**
     * @parameter expression="${project.resources}"
     * @required
     * @readonly
     */
    private List<Resource>        resources;
    /**
     * @parameter expression="${project.testResources}"
     * @required
     * @readonly
     */
    private List<Resource>        testResources;
    /**
     * @parameter
     */
    private String[]              includes;

    private Map<String, Double>   ratioMap         = new HashMap<String, Double>();

    public void execute() throws MojoExecutionException {
        init();
        try {
            countDir(getOutputResultFile("src"), sourcedir);
            countDir(getOutputResultFile("test"), testSourcedir);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to count lines of code", e);
        }

    }

    private File getOutputResultFile(String prefix) {
        File f = outputDirectory;
        if (!f.exists()) {
            f.mkdirs();
        }
        File result = new File(f, prefix + "_" + resultFileName);
        if (result.exists()) {
            result.delete();
        }

        return result;
    }

    private void init() throws MojoExecutionException {
        if (includes == null || includes.length == 0) {
            includes = INCLUDES_DEFAULT;
        }

        ratioMap.clear();
        for (int i = 0; i < includes.length; i++) {
            ratioMap.put(includes[i].toLowerCase(), 1.0);
        }
    }

    private void countDir(File outputResultFile, File dir) throws IOException {
        if (!dir.exists()) {
            return;
        }

        List<File> collected = new ArrayList<File>();
        collectFiles(collected, dir);

        String path = dir.getAbsolutePath().substring(basedir.getAbsolutePath().length());

        int total = 0;
        int source = 0;
        int blank = 0;
        for (File file : collected) {
            int[] line = countLine(file);
            total += line[0];
            source += line[1];
            blank += line[2];
            writeResult(outputResultFile, path + "/" + file.getName(), line[0], line[1], line[2]);
        }
        writeResult(outputResultFile, path, total, source, blank);
    }

    private void collectFiles(List<File> collected, File file) throws IOException {
        if (file.isFile()) {
            if (isFileTypeInclude(file)) {
                collected.add(file);
            }
        } else {
            for (File files : file.listFiles()) {
                collectFiles(collected, files);
            }
        }
    }

    private int[] countLine(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));

        int totalLine = 0;
        int sourceLine = 0;
        int blankLine = 0;
        try {
            while (reader.ready()) {
                String line = reader.readLine();
                totalLine++;
                // 空行
                if (isBlankLine(line)) {
                    blankLine++;
                    continue;
                }

                if (isCommentLine(line)) {
                    continue;
                }
                sourceLine++;
            }
        } finally {
            reader.close();
        }

        return new int[] { totalLine, sourceLine, blankLine };
    }

    private boolean isCommentLine(String line) {
        if (line == null || line.isEmpty()) {
            return false;
        }
        String trimLine = line.trim();
        return trimLine.startsWith("/*") || trimLine.startsWith("*") || trimLine.startsWith("*/")
               || trimLine.startsWith("//");
    }

    private boolean isBlankLine(String line) {
        if (line == null || line.isEmpty() || line.trim().isEmpty()) {
            return true;
        }

        return false;
    }

    private boolean isFileTypeInclude(File file) {
        boolean result = false;
        String fileType = getFileType(file);
        if (fileType != null && ratioMap.keySet().contains(fileType.toLowerCase())) {
            result = true;
        }
        return result;
    }

    private String getFileType(File file) {
        String result = null;
        String fname = file.getName();
        int index = fname.lastIndexOf(DOT);
        if (index > 0) {
            String type = fname.substring(index + 1);
            result = type.toLowerCase();
        }
        return result;
    }

    private void writeResult(File f, String path, int total, int source, int blank) {
        FileWriter w = null;
        StringBuffer sb = new StringBuffer();
        sb.append(path).append(" : ").append(total).append(",").append(source).append(",")
            .append(blank).append(",").append((total != 0) ? source * 100 / total : 0)
            .append("%\n");
        try {
            w = new FileWriter(f, true);
            w.write(sb.toString());

            getLog().debug(sb.toString());
        } catch (IOException e) {
            getLog().error("MOJO output Exception", e);
        } finally {
            if (w != null) {
                try {
                    w.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }
}
