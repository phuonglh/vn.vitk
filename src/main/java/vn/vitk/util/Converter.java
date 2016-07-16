package vn.vitk.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * @author Phuong LE-HONG, <phuonglh@gmail.com>
 * <p>
 * Apr 14, 2016, 11:46:00 AM
 * <p>
 * An utility to transform tokens into common templates, for example
 * numbers "100" or "1000" are transformed to NUMBER; "Ha Noi" or
 * "New York" are transformed to "NAME", etc.   
 */
public class Converter implements Serializable {
	private static final long serialVersionUID = -8916193514910249287L;
	
	private Map<String, Pattern> patterns = new HashMap<String, Pattern>();
	
	public Converter(String regexpFileName) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(regexpFileName), "UTF-8"));
			String line = null;
			while ((line = br.readLine()) != null) {
				String[] parts = line.trim().split("\\s+");
				if (parts.length == 2) {
					patterns.put(parts[0].trim(), Pattern.compile(parts[1].trim()));
				} else {
					System.err.println("Bad format regexp file: " + line);
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	public String convert(String token) {
		for (String type : patterns.keySet()) {
			Pattern pattern = patterns.get(type);
			String t = token.replace('_', ' ');
			if (pattern.matcher(t).matches()) {
				if (type.contains("entity"))
					return "entity";
				else if (type.contains("name"))
					return "name";
				else if (type.contains("allcap"))
					return "allcap";
				else if (type.contains("email"))
					return "email";
				else if (type.contains("date"))
					return "date";
				else if (type.contains("hour"))
					return "hour";
				else if (type.contains("number"))
					return "number";
				else if (type.contains("fraction"))
					return "fraction";
				else if (type.contains("punctuation"))
					return "punct";
			}
		}
		return "normal";
	}
	
	public static void main(String[] args) {
		Converter transformer = new Converter("dat/tok/regexp.txt");
		String[] tokens = {"H5N1", "H5N1;Np", "2006/11/12", "22/07/2012;D", "khanhlh@mail.com", "12:05", "299.2", "33/3", "Jean-Claude Juncker", "Lê Hồng Quang", "không có gì"};
		for (String token : tokens)
			System.out.println(token + "=>" + transformer.convert(token));
	}
}
