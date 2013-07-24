/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modelinglab.utils.filesystem;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.LinkedList;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 *
 * @author Gonzalo Ortiz Jaureguizar (gortiz at software.imdea.org)
 */
public class Zipper {

    private Zipper() {
    }

    public static Zipper getInstance() {
        return ZipperHolder.INSTANCE;
    }
    
    public void zip(File input, File output) throws IOException {
        Collection<File> files = new LinkedList<File>();
        files.add(input);
        zip(files, output);
    }
    
    public void zip(File input, OutputStream output) throws IOException {
        Collection<File> files = new LinkedList<File>();
        files.add(input);
        zip(files, output);
    }

    public void zip(Iterable<File> inputFiles, File output) throws IOException {
        OutputStream os = new FileOutputStream(output);
        zip(inputFiles, os);
    }
    
    public void zip(Iterable<File> inputFiles, OutputStream output) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(output);
        zos.setLevel(Deflater.DEFAULT_COMPRESSION);
        zos.setMethod(Deflater.DEFLATED);
        for (File input : inputFiles) {
            zip(input, "", zos);
        }
        zos.close();
    }
    
    private void zip(File input, String path, ZipOutputStream zos) throws IOException {
        assert path.endsWith("/") || path.isEmpty();
        
        if (input.isDirectory()) {
            path = path + input.getName() + '/';
            for (File f : input.listFiles()) {
                zip(f, path, zos);
            }
        }
        else {
            ZipEntry zipEntry = new ZipEntry(path + input.getName());
            zos.putNextEntry(zipEntry);
            
            InputStream is = new BufferedInputStream(new FileInputStream(input));
            FileSystem.copy(is, zos, false);
            zos.closeEntry();
        }
    }

    public void unzip(InputStream input, File outputFolder) throws IOException {
        BufferedInputStream bufferedInput;
        BufferedOutputStream dest;

        if (input instanceof BufferedInputStream) {
            bufferedInput = (BufferedInputStream) input;
        } else {
            bufferedInput = new BufferedInputStream(input);
        }
        ZipInputStream zis = new ZipInputStream(bufferedInput);
        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {
            if (entry.isDirectory()) {
                File folder = new File(outputFolder, entry.getName());
                if (!folder.exists()) {
                    folder.mkdirs();
                }
            } else {
                File outputFile = new File(outputFolder, entry.getName());
                if (!outputFile.getParentFile().exists()) {
                    outputFile.getParentFile().mkdirs();
                }
                if (outputFile.exists()) {
                    outputFile.delete();
                }
                // write the files
                dest = new BufferedOutputStream(new FileOutputStream(outputFile));
                FileSystem.copy(zis, dest, false);
                dest.close();
            }
        }
        zis.close();
    }

    public void unzip(File input, File output) throws IOException {
        unzip(new FileInputStream(input), output);
    }

    private static class ZipperHolder {

        private static final Zipper INSTANCE = new Zipper();
    }
}
