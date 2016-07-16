package vn.vitk.util;

import java.util.regex.Pattern;

/**
 * @author phuonglh, phuonglh@gmail.com
 * <p>
 * Token normalization.
 */
public class TokenNormalizer {

	static Pattern number = Pattern.compile("^([\\+\\-\\.,]?([0-9]*)?[0-9]+([\\.,]\\d+)*[%°]?)$"); 
	static Pattern punctuation = Pattern.compile("^([\\?!\\.:;,\\-\\+\\*\"'\\(\\)\\[\\]\\{\\}]+)$");
	static Pattern email = Pattern.compile("^(\\w[\\-\\._\\w]*\\w@\\w[\\-\\._\\w]*\\w\\.\\w{2,3})$");
	static Pattern date = Pattern.compile("^(\\d+[\\-\\./]\\d+([\\-\\./]\\d+)?)$|^(năm_)\\d+$");
	static Pattern code = Pattern.compile("^(\\d+[\\-\\._/]?[\\w\\W\\-\\._/]+)$|^([\\w\\W\\-\\._/]+\\d+[\\-\\._/\\d]*[\\w\\W]*)$");
	static Pattern website = Pattern.compile("^(\\w+\\.\\w+)(/\\w*)*$");
	static Pattern xmlTag = Pattern.compile("^</?\\w+>$");
	
	
	private static boolean isNumber(String token) {
		return number.matcher(token).matches();
	}

	private static boolean isPunctuation(String token) {
		return punctuation.matcher(token).matches();
	}
	
	private static boolean isEmail(String token) {
		return email.matcher(token).matches();
	}

	private static boolean isDate(String token) {
		return date.matcher(token).matches();
	}
	
	private static boolean isCode(String token) {
		return code.matcher(token).matches();
	}

	private static boolean isWebsite(String token) {
		return website.matcher(token).matches();
	}

	private static boolean isTag(String token) {
		return xmlTag.matcher(token).matches();
	}
	
	/**
	 * Normalizes the token.
	 * @param token
	 * @return a normalized token.
	 */
	public static String normalize(String token) {
		if (isNumber(token))
			return "1000";
		if (isPunctuation(token))
			return "PUNCT";
		if (isEmail(token))
			return "EMAIL";
		if (isDate(token))
			return "DATE";
		if (isCode(token))
			return "CODE";
		if (isWebsite(token))
			return "WEBSITE";
		if (isTag(token))
			return "XMLTAG";
		return token;
	}
	
}
