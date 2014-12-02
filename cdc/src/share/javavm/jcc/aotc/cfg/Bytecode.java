package aotc.cfg;

import java.util.Vector;
import java.lang.Comparable;
import opcodeconsts.*;
import aotc.ir.*;
import aotc.share.*;

/**
 * This is class that has bytecode informations.
 * Infomations are bpc, opcode, operand and ir.
 * For order, this includes compareTo method.
 * bpc is bytecode program count simillar to program count
 * opcode specify the function of bytecode.
 * operand is operand of bytecode.
 * ir includes all other informations
 * <p>
 * @author Bae SungHwan
 */
public class Bytecode implements Comparable {
	/**
	 * Bytecode program count
	 */
	private int bpc;
	/**
	 * Opcode specify the function of bytecode
	 */
	private byte opcode;
	/**
	 * Operands of bytecode
	 */
	private int operand[];
	/**
	 * extra information of bytecode
	 *
	 * @see IR
	 */
        private IR ir; // added by uhehe99
	
	/**
	 * Constructor
	 *
	 * @param bpc bytecode program count
	 * @param opcode Specify the function of bytecode
	 * @param operand operands of bytecode
	 */
	public Bytecode(int bpc, byte opcode, int operand[]) {
		this.bpc = bpc;
		this.opcode = opcode;
		this.operand = operand;
	}
	public Bytecode(Bytecode bytecode) {
	    this.bpc = bytecode.bpc;
	    this.opcode = bytecode.opcode;
	    this.operand = bytecode.operand;
	}
        /**
	 * Compares this object with the specified object for order
	 * Returns a negative integer, zero, or a positive integer 
	 * as this object is less than, equal to, or greater than the specified object.
	 *
	 * @param o1 the Object to be compared
	 * @return a negative integer, zero, or a positive integer 
	 * as this object is less than, eual to, or greater than the specified object

         * It is assumed that compared Bytecodes are belongs to the same Method 
         */
        public int compareTo(Object o) {
            if (o == null || this == null) {
                new aotc.share.Assert (false, "Cannot compare null references");
            }
            if (this.getBpc() == ((Bytecode) o).getBpc()) {
                //System.out.println(this + " : " + o + "= 0");
                return 0;
            } else if (this.getBpc() > ((Bytecode) o).getBpc()) {
                //System.out.println(this + " : " + o + "= -1");
                return 1;
            } else {
                //System.out.println(this + " : " + o + "= 1");
                return -1;
            }
        }
	/**
	 * Gets bpc to be used checking handler, making label and etc
	 * 
	 * @return bytecode program count
	 */
	public int getBpc() {
		return bpc;
	}
	public void setBpc(int bpc) {
	    this.bpc = bpc;
	}
	
	/**
	 * Gets opcode to be used knowing function of bytecode.
	 *
	 * @return opcode
	 */
	public byte getOpcode() {
		return opcode;
	}
	
	/**
	 * Gets operands that in in the java stack
	 * 
	 * @param i the index of operands array
	 * @return operands that is stack variable
	 */
	public int getOperand(int i) {
		return operand[i];
	}
	
	public int[] getOperands() {
	    return operand;
	}
	/**
	 * Gets operand size
	 *
	 * @return In invoke opcode, the size of operand is not fixed. 
	 *         So to avoid array bound exception, this is needed.
	 */
	public int getOperandNum() {
		return operand.length;
	}
	
	/**
	 * Gets bytecode information of string type
	 * This is used for debugging
	 * 
	 * @return the string that adds bpc, opcNames and operands informations.
	 */
	public String getString() {
		String str;
		int val;
		int _opcode = opcode;
		int len = operand.length;
		if(_opcode<0) _opcode += 256;

		str = bpc + ": " + OpcodeConst.opcNames[_opcode];

		switch(_opcode) {
			case OpcodeConst.opc_newarray:
				str += (" " + BytecodeInfo.arrayTypeName[operand[0]]);
				break;
			case OpcodeConst.opc_lookupswitch:
				str += (" default : &" + (bpc+operand[0]) + ", pairs : " + operand[1] + "\n   ");
				for(int i=2; i<operand.length; i+=2) {
					str += (" (" + operand[i] + ", &" + (bpc+operand[i+1]) + ")"); 
				}
				break;
			case OpcodeConst.opc_tableswitch:
				str += (" default : &" + (bpc+operand[0])
					+ ", low : " + operand[1] + ", high : " + operand[2] + "\n   ");
				for(int i=3; i<operand.length; i++) {
					str += (" &" + (bpc+operand[i])); 
				}
				break;
			default:
				for(int i=0; i<operand.length; i++) {
					str += (" " + operand[i]);
				}
		}
		return str/*+attr.getString()*/;
	}
	/**
	 * Gets bytecode information of string type
	 *
	 * @see getString
	 */
        public String toString() { 
            return getString();
        }
	
	/**
	 * Sets the ir information that includes extra informations
	 *
	 * @param ir ir information
	 * @see IR
	 */
        public void setIR(IR ir) {
                this.ir = ir;
        }
        
	/**
	 * Gets the ir information that includes extra informations
	 *
	 * @return ir information
	 * @see IR
	 */
        public IR getIR() {
                return this.ir;
        }
}
