package aotc.share;

import aotc.share.*;

/**
 * Java Variable that has local variable type
 * This is used to represent java local variable to C variable in C code.
 * <p>
 * @author Park Jong-Kuk
 */
 
public class LocalVariable extends JavaVariable {

	/**
	 * Constructor specifying the type and the index
	 *
	 * @param type the type of local variable
	 * @param index the index of local variable
	 * @see TypeInfo
	 */

	public LocalVariable(int type, int index) {
		super((type<TypeInfo.TYPE_INT) ? TypeInfo.TYPE_INT : type, index);		
	}
	
	/**
	 * Gets the name of local variable that can be used in C code.
	 *
	 * @return the name to be used in C code
	 */
	public String toString() {
		return ("l" + index + "_" + TypeInfo.suffix[type]);
	}
}
