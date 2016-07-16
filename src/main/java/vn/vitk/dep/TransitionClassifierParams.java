package vn.vitk.dep;

import java.util.UUID;

import org.apache.spark.ml.param.DoubleParam;
import org.apache.spark.ml.param.IntParam;
import org.apache.spark.ml.param.JavaParams;
import org.apache.spark.ml.param.ParamMap;
import org.apache.spark.ml.param.ParamValidators;
import org.apache.spark.ml.param.Params;

/**
 * @author Phuong LE-HONG
 * <p>
 * Jul 11, 2016, 3:05:35 PM
 * <p>
 * Training parameters for a MLP.
 */
public class TransitionClassifierParams extends JavaParams {
	private static final long serialVersionUID = -3862425558891374277L;
	private String uid = null;
	private DoubleParam minFF = null;
	private IntParam numFeatures = null;
	private DoubleParam regParam = null;
	private IntParam maxIter = null;
	private DoubleParam tolerance = null;

	/**
	 * Creates default parameters.
	 */
	public TransitionClassifierParams() {
		minFF = new DoubleParam(this, "minFF", "min feature frequency", ParamValidators.gt(0));
		setDefault(minFF, 2.0);
		
		numFeatures = new IntParam(this, "numFeatures", "number of features used in feature hashing", ParamValidators.gt(0));
		setDefault(numFeatures, 1000);
		
		regParam = new DoubleParam(this, "regParam", "regularization parameter", ParamValidators.gtEq(0d));
		setDefault(regParam, 0.0);
		
		maxIter = new IntParam(this, "maxIter", "max number of iterations", ParamValidators.gt(0));
		setDefault(maxIter, 100);
		
		tolerance = new DoubleParam(this, "tolerance", "convergence tolerance of iterations", ParamValidators.gt(0));
		setDefault(tolerance, 1E-5);
	}

	/* (non-Javadoc)
	 * @see org.apache.spark.ml.param.Params#copy(org.apache.spark.ml.param.ParamMap)
	 */
	@Override
	public Params copy(ParamMap extra) {
		return defaultCopy(extra);
	}

	/* (non-Javadoc)
	 * @see org.apache.spark.ml.util.Identifiable#uid()
	 */
	@Override
	public String uid() {
		if (uid == null) {
			String ruid = UUID.randomUUID().toString();
			int n = ruid.length();
			uid = "mlpParams" + "_" + ruid.substring(n-12, n);
		}
		return uid;
	}
	/**
	 * Set the minimal feature frequency, either an absolute integer value or 
	 * a real percentage value.
	 * @param value
	 * @return this object
	 */
	public TransitionClassifierParams setMinFF(double value) {
		set(minFF, value);
		return this;
	}
	
	/**
	 * Gets the minimal feature frequency parameter.
	 * @return the feature frequency parameter. 
	 */
	public DoubleParam getMinFF() {
		return minFF;
	}

	/**
	 * Set the number of features used in the feature hashing trick.
	 * @param value
	 * @return this object.
	 */
	public TransitionClassifierParams setNumFeatures(int value) {
		set(numFeatures, value);
		return this;
	}
	
	/**
	 * Gets the number of features.
	 * @return the number of feature parameter.
	 */
	public IntParam getNumFeatures() {
		return numFeatures;
	}
	
	/**
	 * Set the regularization value.
	 * @param value
	 * @return this object
	 */
	public TransitionClassifierParams setRegParam(double value) {
		set(regParam, value);
		return this;
	}
	
	/**
	 * Gets the regularization parameter.
	 * @return the regularization parameter.
	 */
	public DoubleParam getRegParam() {
		return regParam;
	}
	
	/**
	 * Set the max iterations in training.
	 * @param value
	 * @return this object.
	 */
	public TransitionClassifierParams setMaxIter(int value) {
		set(maxIter, value);
		return this;
	}
	
	/**
	 * Gets the max iteration parameter.
	 * @return the max iteration parameter.
	 */
	public IntParam getMaxIter() {
		return maxIter;
	}
	
	/**
	 * Set the convergence tolerance.
	 * @param value
	 * @return this object
	 */
	public TransitionClassifierParams setTolerance(double value) {
		set(tolerance, value);
		return this;
	}
	
	/**
	 * Gets the tolerance parameter.
	 * @return the tolerance parameter.
	 */
	public DoubleParam getTolerance() {
		return tolerance;
	}
}
