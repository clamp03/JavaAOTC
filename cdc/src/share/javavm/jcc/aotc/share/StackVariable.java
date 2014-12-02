package aotc.share;

/**
 * Java Variable that has stack variable type
 * This is used to represent javav stack variable to C variable in C code.
 * <p>
 * @author Park Jong-Kuk
 */
 
public class StackVariable extends JavaVariable {
	
	/**
	 * Constructor specifying the type and the index
	 *
	 * @param type the type of stack variable
	 * @param index the index of local variable
	 * @see TypeInfo
	 */
	public StackVariable(int type, int index) {
		super((type<TypeInfo.TYPE_INT) ? TypeInfo.TYPE_INT : type, index);		
	}
	
	/**
	 * Gets the name of stack variable that can be used in C code
	 *
	 * @return the name to be used in C code
	 */
	public String toString() {
		return ("s" + index + "_" + TypeInfo.suffix[type]);
	}
}
