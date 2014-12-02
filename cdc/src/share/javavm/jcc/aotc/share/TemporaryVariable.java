package aotc.share;

/**
 * Java Variable that has temporary variable type
 * This variable type is used for temporary C variable in C code.
 * <p>
 * @author Park Jong-Kuk
 */

public class TemporaryVariable extends JavaVariable {
	
	/**
	 * Constructor specifying the type and the index
	 *
	 * @param type type of temporary variable
	 * @param index index of temporary variable
	 * @see TypeInfo
	 */
	public TemporaryVariable(int type, int index) {
		super(type, index);		
	}
	
	/**
	 * Gets the name of temporay variable that can be used in C code.
	 *
	 * @return the name to be used in C code
	 */
	public String toString() {
		return ("t" + index + "_" + TypeInfo.suffix[type]);
	}
}
