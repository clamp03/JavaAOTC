package aotc.share;

/**
 * Java Variable that has constant variable type
 * This is used in ldc kinds. 
 * Double, flaot is stored as integer and must be casted
 * This variable type is used for constant C variable in C code.
 * <p>
 * @author Park Jong-Kuk
 */

public class Constant extends JavaVariable {
	
	/**
	 * value of constant variable that specify name of variable in C code.
	 */
	private String value;
	
	/**
	 * Gets the value of constant variable.
	 *
	 * @return value of constant variable
	 */
	public String toString() {
		return value; 
	} 
	
	/**
	 * Constructor specifying the type and the constant value
	 *
	 * @param type	the type of constant variable
	 * @param constValue the value of constant variable
	 * @see TypeInfo
	 */
	public Constant(int type, String constValue) {
		super(type, -1);
		this.value = constValue;
	}
}
