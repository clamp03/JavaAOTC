package aotc.share;

import java.lang.Comparable;
import aotc.optimizer.*;

/**
 * This have all information that class of all variable type must have.
 * And this is super class of all variable type class
 * <p>
 * @author Park Jong-Kuk
 */
public abstract class JavaVariable implements Comparable {
    
    /**
     * This is used for index of inlined method variable.
     */
    public static final int INLINE_START = 1000;
    
    /**
     * type of java variable
     * @see TypeInfo
     */
    protected int type = -1;
    
    /**
     * index of java variable that represents number of local variable and stack variable.
     */
    protected int index = -1;
    //protected int castType = -1;
    
    /**
     * Constructor specifying type and index of variable.
     *
     * @param type the type of variable
     * @param index the index of variable
     */
    public JavaVariable(int type, int index) {
	this.type = type;
	this.index = index;
	if(index>INLINE_START)
	    new aotc.share.Assert(false, "exceed max variable index!!");
    }
    
    /**
     * Gets the index of variable
     *
     * @return the value of index
     */ 
    public int getIndex() {
	return index;
    }
    
    /**
     * Gets the type of variable
     * 
     * @return the value of type
     */
    public int getType() {
	return type;
    }
    
    /**
     * Gets the size of variable according to type
     *
     * @return the size of variable
     * @see TypeInfo
     */
    public int size() {
	return TypeInfo.size[type]; 
    }
    
    /**
     * Changes the index of variable to the index of inlined method
     * Index is increased by 1000 each inline depth
     */
    public void setInline() {
	if(index < MethodInliner.inline_depth*INLINE_START)
	    index = (index%INLINE_START) + (MethodInliner.inline_depth*INLINE_START);
    }

    /**
     * Gets the index of inlined method
     *
     * @return the index of inllined method
     */ 
    public int getInlineIndex() {
	return (index%INLINE_START);
    }

    /**
     * Check that this is variable of inlined method
     *
     * @return the value of the variable in the inlined method
     */
    public boolean isInline() {
	return (index>=MethodInliner.inline_depth*INLINE_START);
    }
    public boolean isCallerVariable() {
	return (index>=0 && index<MethodInliner.inline_depth*INLINE_START);
    }
    
    /**
     * Compares this object with the specified object for order. 
     * Returns a negative integer, zero, or a positive integer 
     * as this object is less than, equal to, or greater than the specified object.
     *
     * @param o1 the Object to be compared
     * @return a negative integer, zero, or a positive integer 
     * as this object is less than, eual to, or greater than the specified object
     */
    public int compareTo(Object o1) {
	JavaVariable v1 = (JavaVariable)this;
	JavaVariable v2 = (JavaVariable)o1;
	int r1;
	int r2;

	if(v1 instanceof StackVariable) 
            r1 = 0;
	else if(v1 instanceof LocalVariable) 
            r1 = 1;
	else 
            r1 = 2;
            
	if(v2 instanceof StackVariable) 
            r2 = 0;
	else if(v2 instanceof LocalVariable) 
            r2 = 1;
	else 
            r2 = 2;

	if(r1 != r2) return (r1 - r2);
        
        // v1 and v2 are the same kind of variable.
        if (r1 == 2) { // Constants
            return (v1.toString()).compareTo(v2.toString());
        } else {
	    if(v1.type == v2.type) { // Local- & StackVariables
                return (v1.index - v2.index);
	    } else {
	        return (v1.type - v2.type);
            }
        }
    }
}
