package aotc.share;

import java.util.*;

/**
 * The Java Stack class represents a stack emulation of java stack variable.
 * The usual push and pop operation are provided, 
 * as well as a method to peek at the top item on the stack, a method to clone the top item on the stack.
 * <p>
 * @author Park Jong-Kuk
 */
public class JavaStack extends Stack {
	/**
	 * The number of item. Stack is increased or decreased by this index.
	 */
	private int index = 0;

	/**
	 * The max number of item. This stack can't exceed this size.
	 */
	private int maxStack;
	//private TreeSet used;
	
	/**
	 * Constructor 
	 * Creates an empty JavaStack.
	 * 
	 * @param maxStack the max number of item
	 */
	public JavaStack(int maxStack/*, TreeSet used*/) {
		this.maxStack = maxStack;
		//this.used = used;
	}
	/**
	 * Clones the top item of JavaStack.
	 *
	 * @return the clon of top item.
	 */
	public Object clone() {
		JavaStack cp = (JavaStack)super.clone();
		cp.index = index;
		cp.maxStack = maxStack;
		//cp.used = used;
		return cp;
	}
	
	/**
	 * Pushes an iterm onto the top of this stack.
	 * 
	 * @param type the type of item to be pushed onto this stack
	 * @return the stack variable that is at the top of this stack
	 * @see StackVariable
	 */
	public JavaVariable Push(int type) {
		new aotc.share.Assert(index<maxStack, "Stack overflow!!" + maxStack + " " + index);
		StackVariable t = new StackVariable(type, index);
		super.push(t);
		//used.add(t);
		index++;
		return t;
	}
	
	/**
	 * Removes the java variable at the top of this stack 
	 * and returns that stack variable as the value of this function
	 *
	 * @param type the type of item that is at the top of this stack
	 * @return the stack variable at the top of this stack
	 * @see StackVaraible
	 */
        public JavaVariable Pop(int type) {
		//new aotc.share.Assert(index>0, "Stack underflow!!" + this.toString());
		index--;
		return (JavaVariable)super.pop();
	}
	
	/**
	 * Removes the java variable at the top of this stack 
	 * and returns that stack variable as the value of this function
	 *
	 * @return the stack variable at the top of this stack
	 * @see StackVaraible
	 */
	public JavaVariable Pop() {
		//new aotc.share.Assert(index>0, "Stack underflow!!"+ this.toString());
		index--;
		return (JavaVariable)super.pop();
	}

	/**
	 * Looks at the stack variable at the top of this stack without removing it from the stack.
	 *
	 * @return the stack variable at the top of this stack
	 */
	public JavaVariable Peek() {
		return (JavaVariable)super.peek();
	}
	
	/**
	 * Represents the type list of stack items from bottom to top.
	 *
	 * @return the type list of stack items from bottom to top
	 * @see TypeInfo
	 */
	public String toString() {
		String ret = new String();
		for(int i=0; i<size(); i++) {
			ret += TypeInfo.type[((JavaVariable)elementAt(i)).getType()];
		}
		return ret;
	}
}
