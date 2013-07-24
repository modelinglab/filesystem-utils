/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modelinglab.utils.filesystem;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class contains static functions that simplify interaction with file system. All these functions <b>should be independent</b> of SmartGUI
 * 
 * @deprecated you should use com.google.common.io.Files (JDK 1.6 and below) or java.nio.Files (JDK 1.7)
 * @author Gonzalo Ortiz Jaureguizar (gortiz at software.imdea.org)
 */
public class FileSystem {

    private FileSystem() {
    }

    /**
     * Delete the file. If file is a directory, then delete their children.
     * @param file the file to delete
     * @return true if and only if the file or directory is successfully deleted the file or file doesn't exists
     */
    static public boolean delete(File file) {
        if (file == null) {
            return true;
        }
        if (!file.exists()) {
            return true;
        }

        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    delete(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return file.delete();
    }

    static public void copy(InputStream in, OutputStream out) throws IOException {
        copy(in, out, true);
    }

    static public void copy(InputStream in, OutputStream out, boolean closeAtTheEnd) throws IOException {
        byte[] buf = new byte[1024];
        int length = in.read(buf);
        while (length != -1) {
            out.write(buf, 0, length);
            length = in.read(buf);
        }
        if (closeAtTheEnd) {
            in.close();
            out.close();
        }
    }

    /**
     * copy one file to another. If the source file is a directory, then recursively copies its contents.
     * @param sourceLocation the source file to copy
     * @param targetLocation the destination file
     * @param overwrite If the file already exists will be overwritten if and only if this parameter is true
     * @return true if and only if the two files are different and the file or directory is copied correctly
     */
    static public boolean copy(File sourceLocation, File targetLocation, boolean overwrite) {
        boolean error = false;
        if (sourceLocation.equals(targetLocation)) {
            return false;
        }
        try {
            if (sourceLocation.isDirectory()) {

                if (targetLocation.exists() && !targetLocation.isDirectory() && overwrite) {
                    delete(targetLocation);
                }
                if (!targetLocation.exists()) {
                    targetLocation.mkdirs();
                }

                String[] children = sourceLocation.list();
                for (int i = 0; i < children.length; i++) {
                    error |= copy(new File(sourceLocation, children[i]),
                            new File(targetLocation, children[i]), overwrite);
                }
            } else {
                if (targetLocation.exists()) {
                    if (!targetLocation.isFile()) {
                        return false;
                    } else if (overwrite) {
                        delete(targetLocation);
                    } else {
                        return false;
                    }
                } else {
                    targetLocation.getParentFile().mkdirs();
                }

                InputStream in = new FileInputStream(sourceLocation);
                OutputStream out = new FileOutputStream(targetLocation);

                copy(in, out);
            }
        } catch (IOException e) {
            Logger.getLogger(FileSystem.class.getName()).log(Level.SEVERE, null, e);
            return false;
        }
        return !error;
    }

    /**
     * copy one file to another. If the source file is a directory, then recursively copies its contents.
     * Never overwrite existing files
     * @param sourceLocation
     * @param targetLocation
     * @see FileSystemTools#copy(File, File, boolean)
     */
    static public boolean copy(File sourceLocation, File targetLocation) {
        return copy(sourceLocation, targetLocation, false);
    }

    /**
     * Given a file name filter, this function returns a vector with the route (on the workspace) of all 
     * files in the workspace that meet the filter.
     *
     * @param filter A FilenameFilter that filters the files.
     * @param root The root file in which the files will be searched
     * @param filterHidden true if the hidden files (which isHidden() return true OR start with '.') should be ignored. 
     * @param filterDirectories true if the directories files should be ignored.
     * @return
     */
    static public List<File> findFiles(FilenameFilter filter, File root, boolean filterHidden, boolean filterDirectories) {
        LinkedList<File> list = new LinkedList<File>();
        findFiles(root, filter, list, filterHidden, filterDirectories);
        return list;
    }

    static private void findFiles(File folder, FilenameFilter filter, List<File> list, boolean filterHidden, boolean filterDirectories) {
        File[] files = folder.listFiles();
        for (File f : files) {
            String name = f.getName();
            if (filterHidden && (f.isHidden() || name.startsWith("."))) {
                continue;
            } else if (f.isDirectory()) {
                findFiles(f, filter, list, filterHidden, filterDirectories);
                if (!filterDirectories && filter.accept(folder, name)) {
                    list.add(f);
                }
            } else if (f.isFile() && filter.accept(folder, name)) {
                list.add(f);
            }
        }
    }

    /**
     * @param parent the father file  
     * @param child the child file
     * @param startWithSlash if and only if startWithSlash is true, the relative path will begin with / (or maybe \ in windows systems) 
     * @return the relative path from a father of an child file or null if child file is not a child of the parent file
     */
    static public String getRelativePathFrom(File parent, File child, boolean startWithSlash) {
        String p = parent.getAbsolutePath();
        String c = child.getAbsolutePath();
        if (!c.startsWith(p)) {
            return null;
        } else if (!startWithSlash) {
            return c.substring(p.length() + 1);
        } else {
            return c.substring(p.length());
        }
    }

    /**
     * @param parent the father of the files
     * @param children a list of children files
     * @return a list with the relative path of the children files from the father.
     * @throws IllegalArgumentException if any of the files on the list is not a child of the parent
     * @param startWithSlash if and only if startWithSlash is true, the relative paths will begin with / (or maybe \ in windows systems) 
     */
    static public List<String> getRelativePathsFrom(File parent, Collection<File> children, boolean startWithSlash) throws IllegalArgumentException {
        List<String> list = new ArrayList<String>(children.size());
        for (File child : children) {
            String relativePath = getRelativePathFrom(parent, child, startWithSlash);
            if (relativePath == null) {
                throw new IllegalArgumentException("The file " + child.getAbsolutePath() + " is not a child of " + parent.getAbsolutePath());
            }
            list.add(relativePath);
        }
        return list;
    }

    static public File createTempFolder(String prefix) throws IOException {
        File f = File.createTempFile(prefix, "");

        if (f.exists()) {
            f.delete();
        }

        f.mkdirs();

        return f;
    }
}
