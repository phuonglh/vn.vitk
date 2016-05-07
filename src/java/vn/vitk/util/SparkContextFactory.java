package vn.vitk.util;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;

/**
 * @author phuonglh, phuonglh@gmail.com
 * <p>
 * Feb 11, 2016, 6:50:03 AM
 * <p>
 * Creates a singleton Spark context.
 * 
 */
public class SparkContextFactory {
	private static volatile JavaSparkContext jsc = null; 
	
	private SparkContextFactory() {}
	
	public static JavaSparkContext create(String appName, String master) {
		if (jsc == null) {
			synchronized (SparkContextFactory.class) {
				if (jsc == null) {
					SparkConf sc = new SparkConf().setAppName(appName).setMaster(master);
					jsc = new JavaSparkContext(sc);
				}
			}
		}
		return jsc;
	}

	public static JavaSparkContext create(String master) {
		if (jsc == null) {
			synchronized (SparkContextFactory.class) {
				if (jsc == null) {
					SparkConf sc = new SparkConf()
						.setAppName("vn.vitk")
						.setMaster(master);
					jsc = new JavaSparkContext(sc);
				}
			}
		}
		return jsc;
	}
	
	public static JavaSparkContext create() {
		if (jsc == null) {
			synchronized (SparkContextFactory.class) {
				if (jsc == null) {
					SparkConf sc = new SparkConf()
						.setAppName("vn.vitk")
						.setMaster("local[*]");
					jsc = new JavaSparkContext(sc);
				}
			}
		}
		return jsc;
	}
}
