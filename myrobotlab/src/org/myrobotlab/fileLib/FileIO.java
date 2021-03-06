/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */
package org.myrobotlab.fileLib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipException;

import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

public class FileIO {

	public final static Logger log = LoggerFactory.getLogger(FileIO.class.getCanonicalName());

	// --- string interface begin ---
	public final static String fileToString(File file) {
		byte[] bytes = fileToByteArray(file);
		if (bytes == null) {
			return null;
		}
		return new String(bytes);
	}

	public final static String fileToString(String filename) {
		return fileToString(new File(filename));
	}

	public final static String resourceToString(String filename) {
		byte[] bytes = resourceToByteArray(filename);
		if (bytes == null) {
			return null;
		}
		return new String(bytes);
	}

	public static void stringToFile(String filename, String data) {
		byteArrayToFile(filename, data.getBytes());
	}

	// --- string interface end --------------------

	// --- byte[] interface begin ------------------
	// rename getBytes getResourceBytes / String File InputStream
	
	

	static public boolean byteArrayToFile(String filename, byte[] data) {
		try {
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(data);
			fos.close();
			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}

	public final static byte[] fileToByteArray(File file) {
		try {
			FileInputStream fis = new FileInputStream(file);
			return toByteArray(fis);
		} catch (Exception e) {
			Logging.logException(e);
		}
		return null;
	}

	public static final byte[] resourceToByteArray(String resourceName) {
		String filename = String.format("/resource/%s", resourceName);
		InputStream isr = FileIO.class.getResourceAsStream(filename);
		if (isr == null) {
			log.error(String.format("can not find resource [%s]", filename));
			return null;
		}
		return toByteArray(isr);
	}

	public static byte[] toByteArray(InputStream is) {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		// DataInputStream input = new DataInputStream(isr);
		try {

			int nRead;
			byte[] data = new byte[16384];

			while ((nRead = is.read(data, 0, data.length)) != -1) {
				baos.write(data, 0, nRead);
			}

			baos.flush();
			baos.close();
			return baos.toByteArray();
		} catch (Exception e) {
			Logging.logException(e);
		}

		return null;
	}

	// getBytes end ------------------

	// --- object interface begin ------
	public final static boolean writeBinary(String filename, Object toSave) {
		try {
			// use buffering
			OutputStream file = new FileOutputStream(filename);
			OutputStream buffer = new BufferedOutputStream(file);
			ObjectOutput output = new ObjectOutputStream(buffer);
			try {
				output.writeObject(toSave);
				output.flush();
			} finally {
				output.close();
			}
		} catch (IOException e) {
			Logging.logException(e);
			return false;
		}
		return true;
	}

	public final static Object readBinary(String filename) {
		try {
			InputStream file = new FileInputStream(filename);
			InputStream buffer = new BufferedInputStream(file);
			ObjectInput input = new ObjectInputStream(buffer);
			try {
				return (Object) input.readObject();
			} finally {
				input.close();
			}
		} catch (Exception e) {
			Logging.logException(e);
			return null;
		}
	}

	// --- object interface end --------

	// jar pathing begin ---------------

	static public String getResouceLocation() {
		URL url = File.class.getResource("/resource");

		// FIXME - DALVIK issue !
		if (url == null) {
			return null; // FIXME DALVIK issue
		} else {
			return url.toString();
		}
	}

	static public String getRootLocation() {
		URL url = File.class.getResource("/");
		return url.toString();
	}

	static public boolean inJar() {
		String location = getResouceLocation();
		if (location != null) {
			return getResouceLocation().startsWith("jar:");
		} else {
			return false;
		}
	}

	static public String getResourceJarPath() {

		if (!inJar()) {
			log.info("resource is not in jar");
			return null;
		}

		String full = getResouceLocation();
		String jarPath = full.substring(full.indexOf("jar:file:/") + 10, full.lastIndexOf("!"));
		return jarPath;
	}
	
	static public String getBinaryPath(){
		return ClassLoader.getSystemClassLoader().getResource(".").getPath();
	}

	static public ArrayList<String> listInternalContents(String path) {
		if (!inJar()) {
			// get listing if in debug mode or classes are unzipped
			String rp = getRootLocation();
			String targetDir = rp.substring(rp.indexOf("file:/") + 6);
			String fullPath = targetDir + path;
			File dir = new File(fullPath);
			if (!dir.exists()) {
				log.error(String.format("%s does not exist", fullPath));
			}
			ArrayList<String> ret = new ArrayList<String>();
			String[] tmp = dir.list();
			for (int i = 0; i < tmp.length; ++i) {
				File dirCheck = new File(targetDir + path + "/" + tmp[i]);
				if (dirCheck.isDirectory()) {
					ret.add(tmp[i] + "/");
				} else {
					ret.add(tmp[i]);
				}
			}
			dir.list();
			return ret;
		} else {
			// unzip
			return null;
		}
	}

	static public ArrayList<String> listResourceContents(String path) {
		return listInternalContents("/resource" + path);
	}

	public static File[] getPackageContent(String packageName) throws IOException {
		ArrayList<File> list = new ArrayList<File>();
		Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(packageName);
		while (urls.hasMoreElements()) {
			URL url = urls.nextElement();
			File dir = new File(url.getFile());
			for (File f : dir.listFiles()) {
				list.add(f);
			}
		}
		return list.toArray(new File[] {});
	}

	private static FileSystem createZipFileSystem(String zipFilename, boolean create) throws IOException {
		// convert the filename to a URI
		final Path path = Paths.get(zipFilename);
		final URI uri = URI.create("jar:file:" + path.toUri().getPath());

		final Map<String, String> env = new HashMap<String,String>();
		if (create) {
			env.put("create", "true");
		}
		return FileSystems.newFileSystem(uri, env);
	}

	public static void list(String zipFilename) throws IOException {

		System.out.printf("Listing Archive:  %s\n", zipFilename);

		// create the file system
		try (FileSystem zipFileSystem = createZipFileSystem(zipFilename, false)) {

			final Path root = zipFileSystem.getPath("/");

			// walk the file tree and print out the directory and filenames
			Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					print(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					print(dir);
					return FileVisitResult.CONTINUE;
				}

				/**
				 * prints out details about the specified path such as size and
				 * modification time
				 * 
				 * @param file
				 * @throws IOException
				 */
				private void print(Path file) throws IOException {
					final DateFormat df = new SimpleDateFormat("MM/dd/yyyy-HH:mm:ss");
					final String modTime = df.format(new Date(Files.getLastModifiedTime(file).toMillis()));
					System.out.printf("%d  %s  %s\n", Files.size(file), modTime, file);
				}
			});
		}
	}

	// jar pathing end ---------------
	// -- os primitives begin -------

	//
	static public boolean copyResource(String fromFilename, String toFilename) {
		try {
			byte[] b = resourceToByteArray(fromFilename);
			File f = new File(toFilename);
			String path = f.getParent();
			File dir = new File(path);
			if (!dir.exists()){
				dir.mkdirs();
			}
			
			byteArrayToFile(toFilename, b);
			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}

	static public boolean copy(String from, String to) {
		try {
			byte[] b = fileToByteArray(new File(from));
			byteArrayToFile(to, b);
			return true;
		} catch (Exception e) {
			Logging.logException(e);
		}
		return false;
	}

	public static void main(String[] args) throws ZipException, IOException {

		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.INFO);

		String hello = resourceToString("blah.txt");

		copyResource("mrl_logo.jpg", "mrl_logo.jpg");

		byte[] b = resourceToByteArray("mrl_logo.jpg");
		
		File[] files = getPackageContent("");
		
		log.info(getBinaryPath());
		
		log.info("{}", b);

		log.info("done");

	}

}
