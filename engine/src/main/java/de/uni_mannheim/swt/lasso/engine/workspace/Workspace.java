/*
 * LASSO - an Observatorium for the Dynamic Selection, Analysis and Comparison of Software
 * Copyright (C) 2024 Marcus Kessel (University of Mannheim) and LASSO contributers
 *
 * This file is part of LASSO.
 *
 * LASSO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LASSO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LASSO.  If not, see <https://www.gnu.org/licenses/>.
 */
package de.uni_mannheim.swt.lasso.engine.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.uni_mannheim.swt.lasso.core.dto.file.FileViewItem;
import de.uni_mannheim.swt.lasso.core.model.Abstraction;
import de.uni_mannheim.swt.lasso.core.model.MavenProject;
import de.uni_mannheim.swt.lasso.engine.LSLScript;

import de.uni_mannheim.swt.lasso.engine.dag.ActionNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.*;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.DirectoryScanner;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Models LASSO workspaces for LSL scripts.
 *
 * @author Marcus Kessel
 *
 */
public class Workspace {

    public static final String ENCODING_DEFAULT = "UTF-8";

    public static final String SCRIPT_JSON = "script.json";
    public static final String SCRIPT_EXECUTION_LOG = "execution_log.txt";

    private ObjectMapper objectMapper = new ObjectMapper();

    private File lassoRoot;
    private File root;
    private LSLScript script;

    public File getMavenRepository() {
        return new File(lassoRoot, "repository/repository/");
    }

    public File createGlobalLassoFile(String filename) {
        return new File(lassoRoot, filename);
    }

    public File createGlobalLassoDirectory(String name) {
        File dir = createGlobalLassoFile(name);
        if(!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }

    public File createFile(String filename) {
        return new File(root, filename);
    }

    public File createDirectory(String name) {
        File dir = createFile(name);
        if(!dir.exists()) {
            dir.mkdirs();
        }

        return dir;
    }

    public File createDirectory(Abstraction abstraction) {
        File abstractionRoot = createDirectory(abstraction.getName());
        if(!abstractionRoot.exists()) {
            abstractionRoot.mkdirs();
        }

        return abstractionRoot;
    }

    public File getRoot(Abstraction abstraction) {
        return createDirectory(abstraction);
    }

    public File getRootForActionByName(ActionNode actionNode, Abstraction abstraction) {
        File[] files = getRoot(abstraction).listFiles((FileFilter) new PrefixFileFilter(actionNode.getName(), IOCase.SENSITIVE));

        if(ArrayUtils.isEmpty(files)) {
            return null;
        }

        return Arrays.stream(files).filter(File::isDirectory).findFirst().orElse(null);
    }

    public File getRoot(String actionInstanceId, Abstraction abstraction) {
        return new File(getRoot(abstraction), actionInstanceId);
    }

    public void writeObject(String filename, Object object) throws IOException {
        objectMapper.writeValue(new File(root, filename),object);
    }

    public <T> T readObject(String filename, Class<T> clazz) throws IOException {
        return objectMapper.readValue(new File(root, filename), clazz);
    }

    public void write(String filename, String content) throws IOException {
        FileUtils.write(new File(root, filename), content, ENCODING_DEFAULT);
    }

    public String read(String filename) throws IOException {
        return FileUtils.readFileToString(new File(root, filename), ENCODING_DEFAULT);
    }

    public String readScriptExecutionLog() throws IOException {
        return read(SCRIPT_EXECUTION_LOG);
    }

    public boolean fileExists(String filename) {
        return createFile(filename).exists();
    }

    public String readLines(String filename, int max) throws IOException {
        File file = createFile(filename);

        StringBuilder stringBuilder = new StringBuilder();
        try(LineIterator lineIterator = FileUtils.lineIterator(file)) {
            int lines = 0;

            while(lineIterator.hasNext()) {
                String line = lineIterator.nextLine();
                lines++;

                stringBuilder.append(line);
                stringBuilder.append(System.lineSeparator());

                if(max > -1 && lines >= max) {
                    break;
                }
            }
        }

        return stringBuilder.toString();
    }

    public Collection<File> listFilesRecursively(MavenProject project, String filename, String extension) {
        return listFilesRecursively(project.getBaseDir(), filename, extension);
    }

    public Collection<File> listFilesRecursively(File baseDir, String filename, String extension) {
        return listFilesRecursively(new File(baseDir, filename), extension);
    }

    public Collection<File> listFilesRecursively(File directory, String extension) {
        return listFiles(directory, extension, true);
    }

    public Collection<File> listFiles(File directory, String extension, boolean recursive) {
        if(!directory.isDirectory()) {
            return Collections.emptyList();
        }

        return FileUtils.listFiles(
                directory,
                new String[] { extension }, recursive);
    }

    public String[] scanForFiles(List<String> filePatterns) {
        DirectoryScanner ds = new DirectoryScanner();
        String[] includes = filePatterns.toArray(new String[0]);
        //String[] excludes = {"modules\\*\\**"};
        ds.setIncludes(includes);
        //ds.setExcludes(excludes);
        ds.setBasedir(getRoot());
        //ds.setCaseSensitive(true);
        ds.scan();

        String[] srcFiles = ds.getIncludedFiles();

        return srcFiles;
    }

    public static void debugTree(FileViewItem item, int depth) {
        System.out.println(StringUtils.repeat(' ', depth) + item.getText() + " => " + item.getValue());

        if (CollectionUtils.isNotEmpty(item.getChildren())) {
            item.getChildren().forEach(c -> debugTree(c, depth + 1));
        }
    }

    public Map<String, FileViewItem> scanForFileItems(List<String> filePatterns) {
        String[] files = scanForFiles(filePatterns);

        Map<String, FileViewItem> items = new HashMap<>();

        List<String> flatTreePaths = Arrays.stream(files).flatMap(path -> {
            List<String> paths = new LinkedList<>();
            paths.add(path);

            String[] parts = StringUtils.split(path, File.separator);

            // exclude current path, only look at parent
            if (parts.length > 2) {
                for (int i = parts.length - 1; i >= 0; i--) {
                    String parentPath = String.join(File.separator, ArrayUtils.subarray(parts, 0, i));

                    if (StringUtils.isEmpty(parentPath)) {
                        parentPath = File.separator;
                    }

                    paths.add(parentPath);
                }
            }

            return paths.stream();
        }).distinct().collect(Collectors.toList());

        for (String path : flatTreePaths) {
            FileViewItem item = null;

            if (items.containsKey(path)) {
                item = items.get(path);
            } else {
                item = new FileViewItem();
                item.setValue(path);

                if (StringUtils.equals(path, File.separator)) {
                    item.setText(File.separator);
                } else {
                    item.setText(FilenameUtils.getName(path));
                }

                item.setText(FilenameUtils.getName(path));

                items.put(path, item);
            }

            // root
            if (StringUtils.equals(path, File.separator)) {
                continue;
            }

            // immediate parent
            String parentPath = FilenameUtils.getFullPathNoEndSeparator(path);

            if (StringUtils.isEmpty(parentPath)) {
                parentPath = File.separator;
            }

            FileViewItem parent = null;
            if (items.containsKey(parentPath)) {
                parent = items.get(parentPath);
            } else {
                parent = new FileViewItem();
                parent.setValue(parentPath);

                if (StringUtils.equals(parentPath, File.separator)) {
                    parent.setText(File.separator);
                } else {
                    parent.setText(FilenameUtils.getName(parentPath));
                }

                items.put(parentPath, parent);
            }

            parent.addChild(item);
        }

        return items;
    }

    public void createZipFile(OutputStream out, List<String> filePatterns) throws IOException {
        String[] srcFiles = scanForFiles(filePatterns);

        createZipFile(out, getRoot(), srcFiles);
    }

    protected void createZipFile(OutputStream out, File baseDir, String[] srcFiles) throws IOException {
        ZipOutputStream zipOut = null;
        try {
            zipOut = new ZipOutputStream(out);
            for (String srcFile : srcFiles) {
                File fileToZip = new File(baseDir, srcFile);
                FileInputStream fis = new FileInputStream(fileToZip);

                ZipEntry zipEntry = new ZipEntry(srcFile);
                zipOut.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while((length = fis.read(bytes)) >= 0) {
                    zipOut.write(bytes, 0, length);
                }
                fis.close();
            }
        } finally {
            IOUtils.closeQuietly(zipOut);
            IOUtils.closeQuietly(out);
        }
    }

    public String getExecutionId() {
        return script.getExecutionId();
    }

    public LSLScript getScript() {
        return script;
    }

    public void setScript(LSLScript script) {
        this.script = script;
    }

    public File getRoot() {
        return root;
    }

    public void setRoot(File root) {
        this.root = root;
    }

    public File getLassoRoot() {
        return lassoRoot;
    }

    public void setLassoRoot(File lassoRoot) {
        this.lassoRoot = lassoRoot;
    }
}
