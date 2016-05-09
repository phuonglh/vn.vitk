package vn.vitk.util;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author LE Hong Phuong
 * <p>
 *         A file that facilitates input/output stuff of an UTF8 text file.
 *         Typically, you should use this utility to read all lines of a text
 *         file in UTF8 encoding to an array of strings by method
 *         {@link #readLines(String)}.  
 * 
 */
public final class UTF8FileIO {
	/**
	 * A buffered writer
	 */
	public static BufferedWriter writer = null;
	/**
	 * A buffered reader
	 */
	public static BufferedReader reader = null;

	/**
	 * Use static methods only.
	 */
	private UTF8FileIO() {
	}

	/**
	 * Create a buffered reader to read from a UTF-8 text file.
	 * 
	 * @param fileName
	 */
	public static void createReader(String fileName) {
		try {
			createReader(new FileInputStream(fileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a buffered reader to read from an input stream.
	 * 
	 * @param inputStream
	 */
	private static void createReader(InputStream inputStream) {
		try {
			// first, try to close the reader if it has already existed and has not been closed
			closeReader();
			// create the reader
			Reader iReader = new InputStreamReader(inputStream, "UTF-8");
			reader = new BufferedReader(iReader);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Close the reader
	 */
	public static void closeReader() {
		try {
			if (reader != null) {
				reader.close();
				reader = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a buffered writer to write to a UTF-8 text file.
	 * 
	 * @param fileName
	 */
	public static void createWriter(String fileName) {
		try {
			createWriter(new FileOutputStream(fileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create a buffered writer given an output stream
	 * 
	 * @param outputStream
	 */
	private static void createWriter(OutputStream outputStream) {
		try {
			// first, try to close the writer if it has already existed and has not been closed
			closeWriter();
			Writer oWriter = new OutputStreamWriter(outputStream, "UTF-8");
			writer = new BufferedWriter(oWriter);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Close the writer
	 */
	public static void closeWriter() {
		try {
			// flush and close the writer
			if (writer != null) {
				writer.flush();
				writer.close();
				writer = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Reads all lines of the file (including POS information).
	 * @param fileName
	 * @return
	 */
	public static String[] readLines(String fileName) {
		List<String> lines = new ArrayList<String>();
		if (reader == null)
			createReader(fileName);
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				if (line.trim().length() > 0) {
					lines.add(line.trim());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		closeReader();
		return lines.toArray(new String[lines.size()]);
	}
	
	/**
	 * Writes a line to the writer, a new line is added at the end. 
	 * @param line
	 */
	private static void writeln(String line) {
		try {
			writer.write(line);
			writer.write("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Writes an array of objects to a text file, each on a line. 
	 * @param fileName
	 * @param objects
	 */
	public static void writeln(String fileName, Object[] objects) {
		if (writer == null) {
			createWriter(fileName);
			for (Object obj : objects) {
				writeln(obj.toString());
			}
			closeWriter();
		}
	}
	
	/**
	 * Writes a list of strings to a file, each string on a line.
	 * @param fileName
	 * @param lines
	 */
	public static void writeln(String fileName, List<String> lines) {
		String[] a = lines.toArray(new String[lines.size()]);
		writeln(fileName, a);
	}
	
}
