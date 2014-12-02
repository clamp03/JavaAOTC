package aotc.ir;

import java.util.*;
import components.*;
import opcodeconsts.*;
import consts.*;
import util.*;
import aotc.cfg.*;
import aotc.share.*;
import aotc.translation.*;
import vm.*;

/**
 * This class make IR that has additional information that can get from bytecode.
 * Emulates java stack, then sets target and operands to ir
 * And translate dup to copy instruction such as iload, aload and etc
 * This sets the value of item in constant pool to ir instead of index
 * <p>
 * @author Park Jong-Kuk
 */
 
public class IRGenerator {
    
    /**
     * This is class information that is used to get values in constant pool
     */
    private ClassInfo classInfo;
    /**
     * Method information that is used to get method type information
     */
    private MethodInfo methodInfo;
    //private TreeSet usedVariables;
    /**
     * java stack for stack emulation
     */
    private JavaStack stack;
    //private Bytecode block[];
    /**
     * control flow graph
     */
    private CFG cfg;
    /**
     * This is used to have compound ir that consist of some irs.
     */
    private Vector replaceList = new Vector(); 
    
    private ConstantObject constants[];
    /**
     * Constructor
     *
     * @param classInfo class information for constant pool
     * @param methodInfo method information for method type information
     * @param nativeTYpes may be deleted soon.
     * @param cfg control flow graph
     */
    public IRGenerator(ClassInfo classInfo, MethodInfo methodInfo, CFG cfg) {
        this.classInfo = classInfo;
        this.methodInfo = methodInfo;
        //this.usedVariables = new TreeSet(/*new JavaVariableComparator()*/);
        this.stack = new JavaStack(methodInfo.stack/*, usedVariables*/);
	//this.block = block;
        this.cfg = cfg;
        this.constants = classInfo.getConstantPool().getConstants();
    }
    /**
     * Make IR that has additional information from bytecode.
     * This emulates java stack and sets to ir.
     *     Find index and type of target and operands.
     *     Make java variable according to type of target and operands
     *     Set to ir as target or operands
     *
     * And dup, dup2, dup_x1 and etc is changed to copy ir
     * This sets the value of item in constant pool to ir instead of index
     *
     * @param ir Ir is null. This method makes ir information and set this info to ir
     */
    public void makeIR(IR ir) {
	Bytecode bytecode = ir.getBytecode();
	int bpc = bytecode.getBpc();
	// stack adjust!!

	if (ir.getStack()==null) {
	    ir.setStack(stack);
	}
	stack = ir.getStack();
	if(ir.hasAttribute(IRAttribute.BB_HANDLER)) {
	    JavaStack handler_stack = new JavaStack(methodInfo.stack/*, usedVariables*/);
	    handler_stack.Push(TypeInfo.TYPE_REFERENCE);
	    ir.setStack(handler_stack);
            stack = handler_stack;
	}

        new aotc.share.Assert(stack != null, "stack is null");
	

        /* TODO or not?  Exception catching is skipped. */

	/* To avoid negative numbers of opcodes bigger than 0x80, */
	/* opcodes are type casted to short. */ 
	short shortOpcode=ir.getShortOpcode();

	/* Make IR from Bytecode */
	switch(shortOpcode)
	{
	    case OpcodeConst.opc_nop: //0x00
		break;
    
            // 0x01 ~ 0x11 : Pushing getConstantPool().getConstants() onto the stack
	    case OpcodeConst.opc_aconst_null: // Null Constant
	    case OpcodeConst.opc_iconst_m1: // Integer Constant
	    case OpcodeConst.opc_iconst_0:
	    case OpcodeConst.opc_iconst_1:
	    case OpcodeConst.opc_iconst_2:
	    case OpcodeConst.opc_iconst_3:
	    case OpcodeConst.opc_iconst_4:
	    case OpcodeConst.opc_iconst_5:
	    case OpcodeConst.opc_lconst_0: // Long constant
	    case OpcodeConst.opc_lconst_1:
	    case OpcodeConst.opc_fconst_0: // Float constant
	    case OpcodeConst.opc_fconst_1: 
	    case OpcodeConst.opc_fconst_2:
	    case OpcodeConst.opc_dconst_0: // Double constant
	    case OpcodeConst.opc_dconst_1:
	    case OpcodeConst.opc_bipush: // Byte, short constant
	    case OpcodeConst.opc_sipush:
                {
                    int type = -1;
                    Constant operand = null;
                    StackVariable target = null; 
                    if (shortOpcode == OpcodeConst.opc_aconst_null) {
                        type = TypeInfo.TYPE_REFERENCE;
                        operand = new Constant(type, "NULL");
                    } else if (shortOpcode >= OpcodeConst.opc_iconst_m1 && shortOpcode <= OpcodeConst.opc_iconst_5) {
                        type = TypeInfo.TYPE_INT;
                        operand = new Constant(type, Integer.toString(shortOpcode-3));
                    } else if (shortOpcode >= OpcodeConst.opc_lconst_0 && shortOpcode <= OpcodeConst.opc_lconst_1) {
                        type = TypeInfo.TYPE_LONG;
        		    operand = new Constant(type, Long.toString(shortOpcode - 9));
                    } else if (shortOpcode >= OpcodeConst.opc_fconst_0 && shortOpcode <= OpcodeConst.opc_fconst_2) {
                        type = TypeInfo.TYPE_FLOAT;
        		    operand = new Constant(type, (shortOpcode-0x0B) + ".0");
                    } else if (shortOpcode >= OpcodeConst.opc_dconst_0 && shortOpcode <= OpcodeConst.opc_dconst_1) {
                        type = TypeInfo.TYPE_DOUBLE;
        		    operand = new Constant(type, (shortOpcode-0x0E) + ".0");
                    } else if (shortOpcode >= OpcodeConst.opc_bipush && shortOpcode <= OpcodeConst.opc_sipush) {
                        type = TypeInfo.TYPE_INT;
                        operand = new Constant(type, Integer.toString(bytecode.getOperand(0)));
                    } else {
                        new aotc.share.Assert(false, "Unknown constant load operation? or miss by programmer -_-"); 
                    }
                    ir.addOperand(operand);
                    ir.addTarget(stack.Push(type));
                }
		break;

            // 0x15 ~ 0x2d : Load local variables to stack
	    case OpcodeConst.opc_iload: // 0x15, 0x1a ~ 0x1d
	    case OpcodeConst.opc_iload_0:
	    case OpcodeConst.opc_iload_1:
	    case OpcodeConst.opc_iload_2:
	    case OpcodeConst.opc_iload_3:
	    case OpcodeConst.opc_lload: // 0x16 , 0x1e ~ 0x21
	    case OpcodeConst.opc_lload_0:
	    case OpcodeConst.opc_lload_1:
	    case OpcodeConst.opc_lload_2:
	    case OpcodeConst.opc_lload_3:
	    case OpcodeConst.opc_fload: // 0x17 , 0x22 ~ 0x25
	    case OpcodeConst.opc_fload_0:
	    case OpcodeConst.opc_fload_1:
	    case OpcodeConst.opc_fload_2:
	    case OpcodeConst.opc_fload_3:
	    case OpcodeConst.opc_dload: // 0x18 , 0x26 ~ 0x29
	    case OpcodeConst.opc_dload_0:
	    case OpcodeConst.opc_dload_1:
	    case OpcodeConst.opc_dload_2:
	    case OpcodeConst.opc_dload_3:
	    case OpcodeConst.opc_aload: // 0x19 , 0x2a ~ 0x2d
	    case OpcodeConst.opc_aload_0:
	    case OpcodeConst.opc_aload_1:
	    case OpcodeConst.opc_aload_2:
	    case OpcodeConst.opc_aload_3:
		{			
		    int	localVariableIndex=-1;
		    int type=-1;
		    if (shortOpcode == OpcodeConst.opc_iload // iload
			    || ( shortOpcode >= OpcodeConst.opc_iload_0 && shortOpcode <= OpcodeConst.opc_iload_3) ) {
			type = TypeInfo.TYPE_INT;
			if (shortOpcode == OpcodeConst.opc_iload) {
			    localVariableIndex = bytecode.getOperand(0);
			} else {
			    localVariableIndex = (shortOpcode - OpcodeConst.opc_iload_0);
			}
		    } else if (shortOpcode == OpcodeConst.opc_lload // lload
			    || ( shortOpcode >= OpcodeConst.opc_lload_0 && shortOpcode <= OpcodeConst.opc_lload_3) ) {
			type = TypeInfo.TYPE_LONG;
			if (shortOpcode == OpcodeConst.opc_lload) {
			    localVariableIndex = bytecode.getOperand(0);
			} else {
			    localVariableIndex = (shortOpcode - OpcodeConst.opc_lload_0);
			}
		    } else if (shortOpcode == OpcodeConst.opc_fload // fload
			    ||( shortOpcode >= OpcodeConst.opc_fload_0 && shortOpcode <= OpcodeConst.opc_fload_3) ) {
			type = TypeInfo.TYPE_FLOAT;
			if (shortOpcode == OpcodeConst.opc_fload) {
			    localVariableIndex = bytecode.getOperand(0);
			} else {
			    localVariableIndex = (shortOpcode - OpcodeConst.opc_fload_0);
			}
		    } else if (shortOpcode == OpcodeConst.opc_dload // dload
			    || ( shortOpcode >= OpcodeConst.opc_dload_0 && shortOpcode <= OpcodeConst.opc_dload_3) ) {
			type = TypeInfo.TYPE_DOUBLE;
			if (shortOpcode == OpcodeConst.opc_dload) {
			    localVariableIndex = bytecode.getOperand(0);
			} else {
			    localVariableIndex = (shortOpcode - OpcodeConst.opc_dload_0);
			}
		    } else if (shortOpcode == OpcodeConst.opc_aload // aload
			    || ( shortOpcode >= OpcodeConst.opc_aload_0 && shortOpcode <= OpcodeConst.opc_aload_3) ) {
			type = TypeInfo.TYPE_REFERENCE;
			if (shortOpcode == OpcodeConst.opc_aload) {
			    localVariableIndex = bytecode.getOperand(0);
			} else {
			    localVariableIndex = (shortOpcode - OpcodeConst.opc_aload_0);
			}
		    } else { 
			new aotc.share.Assert(false, "AOTC compiler may have missed implementation at " + bytecode.getString());
		    }

		    boolean isVirtualThis = (methodInfo.access & 0x0008 /* ACC_STATIC*/) == 0
					    && localVariableIndex == 0;

                    JavaVariable target = stack.Push(type);
		    JavaVariable operand = new LocalVariable(type, localVariableIndex);
		    if(isVirtualThis) {
			operand = new Constant(TypeInfo.TYPE_REFERENCE, "cls");
		    } else {
			operand = new LocalVariable(type, localVariableIndex);
		    }
                    ir.addTarget(target);
                    ir.addOperand(operand);
		    //usedVariables.add(operand);
		}
		break;

	    case OpcodeConst.opc_iaload:
	    case OpcodeConst.opc_laload:
	    case OpcodeConst.opc_faload:
	    case OpcodeConst.opc_daload:
	    case OpcodeConst.opc_aaload:
	    case OpcodeConst.opc_baload:
	    case OpcodeConst.opc_caload:
	    case OpcodeConst.opc_saload:
		{
		    int arrayType = -1;

		    switch(shortOpcode) {
			case OpcodeConst.opc_iaload:
			    arrayType = TypeInfo.TYPE_INT;
			    break;
			case OpcodeConst.opc_laload:
			    arrayType = TypeInfo.TYPE_LONG;
			    break;
			case OpcodeConst.opc_faload:
			    arrayType = TypeInfo.TYPE_FLOAT;
			    break;
			case OpcodeConst.opc_daload:
			    arrayType = TypeInfo.TYPE_DOUBLE;
			    break;
			case OpcodeConst.opc_aaload:
			    arrayType = TypeInfo.TYPE_REFERENCE;
			    break;
			case OpcodeConst.opc_baload:
			    arrayType = TypeInfo.TYPE_BYTE;
			    break;
			case OpcodeConst.opc_caload:
			    arrayType = TypeInfo.TYPE_CHAR;
			    break;
			case OpcodeConst.opc_saload:
			    arrayType = TypeInfo.TYPE_SHORT;
			    break;
		    }
		    JavaVariable index = stack.Pop(TypeInfo.TYPE_INT);
		    JavaVariable arrayref = stack.Pop(TypeInfo.TYPE_REFERENCE);
		    JavaVariable target = stack.Push(arrayType);
                    ir.addTarget(target);
                    ir.addOperand(arrayref);
                    ir.addOperand(index);
		}
		break;

	    case OpcodeConst.opc_istore: // 0x36 , 0x3b ~ 0x3e
	    case OpcodeConst.opc_istore_0:
	    case OpcodeConst.opc_istore_1:
	    case OpcodeConst.opc_istore_2:
	    case OpcodeConst.opc_istore_3:
	    case OpcodeConst.opc_lstore: // 0x37 , 0x3f ~ 0x42
	    case OpcodeConst.opc_lstore_0:
	    case OpcodeConst.opc_lstore_1:
	    case OpcodeConst.opc_lstore_2:
	    case OpcodeConst.opc_lstore_3:
	    case OpcodeConst.opc_fstore: // 0x38 , 0x43 ~ 0x46
	    case OpcodeConst.opc_fstore_0:
	    case OpcodeConst.opc_fstore_1:
	    case OpcodeConst.opc_fstore_2:
	    case OpcodeConst.opc_fstore_3:
	    case OpcodeConst.opc_dstore: // 0x39 , 0x47 ~ 0x4a
	    case OpcodeConst.opc_dstore_0:
	    case OpcodeConst.opc_dstore_1:
	    case OpcodeConst.opc_dstore_2:
	    case OpcodeConst.opc_dstore_3:
	    case OpcodeConst.opc_astore: // 0x39 , 0x47 ~ 0x4a
	    case OpcodeConst.opc_astore_0:
	    case OpcodeConst.opc_astore_1:
	    case OpcodeConst.opc_astore_2:
	    case OpcodeConst.opc_astore_3:
		{			
		    int	localVariableIndex=-1;
		    int type = -1;
		    if (shortOpcode == OpcodeConst.opc_istore  
			    || ( shortOpcode >= OpcodeConst.opc_istore_0 && shortOpcode <= OpcodeConst.opc_istore_3) ) {
			type = TypeInfo.TYPE_INT;
			if (shortOpcode == OpcodeConst.opc_istore) {
			    localVariableIndex = bytecode.getOperand(0);
			} else {
			    localVariableIndex = (shortOpcode - OpcodeConst.opc_istore_0);
			}
		    } else if (shortOpcode == OpcodeConst.opc_lstore 
			    || ( shortOpcode >= OpcodeConst.opc_lstore_0 && shortOpcode <= OpcodeConst.opc_lstore_3) ) {
			type = TypeInfo.TYPE_LONG;
			if (shortOpcode == OpcodeConst.opc_lstore) {
			    localVariableIndex = bytecode.getOperand(0);
			} else {
			    localVariableIndex = (shortOpcode - OpcodeConst.opc_lstore_0);
			}
		    } else if (shortOpcode == OpcodeConst.opc_fstore 
			    || ( shortOpcode >= OpcodeConst.opc_fstore_0 && shortOpcode <= OpcodeConst.opc_fstore_3) ) {

			type = TypeInfo.TYPE_FLOAT;
			if (shortOpcode == OpcodeConst.opc_fstore) {
			    localVariableIndex = bytecode.getOperand(0);
			} else {
			    localVariableIndex = (shortOpcode - OpcodeConst.opc_fstore_0);
			}
		    } else if (shortOpcode == OpcodeConst.opc_dstore 
			    || ( shortOpcode >= OpcodeConst.opc_dstore_0 && shortOpcode <= OpcodeConst.opc_dstore_3) ) {
			type = TypeInfo.TYPE_DOUBLE;
			if (shortOpcode == OpcodeConst.opc_dstore) {
			    localVariableIndex = bytecode.getOperand(0);
			} else {
			    localVariableIndex = (shortOpcode - OpcodeConst.opc_dstore_0);
			}
		    } else if (shortOpcode == OpcodeConst.opc_astore 
			    || ( shortOpcode >= OpcodeConst.opc_astore_0 && shortOpcode <= OpcodeConst.opc_astore_3) ) {
			type = TypeInfo.TYPE_REFERENCE;
			if (shortOpcode == OpcodeConst.opc_astore) {
			    localVariableIndex = bytecode.getOperand(0);
			} else {
			    localVariableIndex = (shortOpcode - OpcodeConst.opc_astore_0);
			}
		    }
		    LocalVariable target = new LocalVariable(type, localVariableIndex);
                    JavaVariable operand = stack.Pop(type);
                    ir.addOperand(operand);
                    ir.addTarget(target);
		    //usedVariables.add(target);
		}
		break;

	    case OpcodeConst.opc_iastore:
	    case OpcodeConst.opc_lastore:
	    case OpcodeConst.opc_fastore:
	    case OpcodeConst.opc_dastore:
	    case OpcodeConst.opc_aastore:
	    case OpcodeConst.opc_bastore:
	    case OpcodeConst.opc_castore:
	    case OpcodeConst.opc_sastore:
		{
		    int arrayType = -1;
		    switch(shortOpcode) {
			case OpcodeConst.opc_iastore:
			    arrayType = TypeInfo.TYPE_INT;
			    break;
			case OpcodeConst.opc_lastore:
			    arrayType = TypeInfo.TYPE_LONG;
			    break;
			case OpcodeConst.opc_fastore:
			    arrayType = TypeInfo.TYPE_FLOAT;
			    break;
			case OpcodeConst.opc_dastore:
			    arrayType = TypeInfo.TYPE_DOUBLE;
			    break;
			case OpcodeConst.opc_aastore:
			    arrayType = TypeInfo.TYPE_REFERENCE;
			    break;
			case OpcodeConst.opc_bastore:
			    arrayType = TypeInfo.TYPE_BYTE;
			    break;
			case OpcodeConst.opc_castore:
			    arrayType = TypeInfo.TYPE_CHAR;
			    break;
			case OpcodeConst.opc_sastore:
			    arrayType = TypeInfo.TYPE_SHORT;
			    break;
		    }

		    JavaVariable value = stack.Pop(arrayType);
		    JavaVariable index = stack.Pop(TypeInfo.TYPE_INT);
		    JavaVariable arrayref = stack.Pop(TypeInfo.TYPE_REFERENCE);
                    ir.addOperand(arrayref);
                    ir.addOperand(index);
                    ir.addOperand(value);
		}
		break;

	    case OpcodeConst.opc_pop2:	// 0x58
	    {
		JavaVariable op1 = stack.Pop();
                if (op1.size() == 1) {
                    stack.Pop();
                }
		break;
	    }
	    case OpcodeConst.opc_pop:	// 0x57
	    {
		stack.Pop();
		break;
	    }
	    case OpcodeConst.opc_dup: // 0x59
                {
                    JavaVariable op1 = stack.Pop();
                    JavaVariable target1 = stack.Push(op1.getType());
                    JavaVariable target2 = stack.Push(op1.getType());
                    ir.addOperand(op1);
                    ir.addTarget(target2);
                    
                    Vector toList = new Vector();
                    IR newIR;
                    newIR = makeCopyIR(ir, op1, target2);
                    newIR.addDebugInfoString("Generated by " + ir.getBpc());
                    toList.addElement(newIR);
                    addToReplaceList(ir, toList);
                }
		break;

	    case OpcodeConst.opc_dup_x1: // 
		{
		    JavaVariable op1 = stack.Pop();
		    JavaVariable op2 = stack.Pop();
		    JavaVariable target1 = stack.Push(op1.getType());
		    JavaVariable target2 = stack.Push(op2.getType());
		    JavaVariable target3 = stack.Push(op1.getType());
                    // target1 = op1
                    // target2 = op2
                    // target3 = op1
                    ir.addOperand(op1);
                    ir.addOperand(op2);
                    ir.addTarget(target1);
                    ir.addTarget(target2);
                    ir.addTarget(target3);
                    
                    Vector toList = new Vector();
                    IR newIR1, newIR2, newIR3;
                    newIR1 = makeCopyIR(ir, op1, target3);
                    newIR2 = makeCopyIR(ir, op2, target2);
                    newIR3 = makeCopyIR(ir, target3, target1);
                    newIR1.addDebugInfoString("Generated by " + ir.getBpc());
                    newIR2.addDebugInfoString("Generated by " + ir.getBpc());
                    newIR3.addDebugInfoString("Generated by " + ir.getBpc());
                    toList.addElement(newIR1);
                    toList.addElement(newIR2);
                    toList.addElement(newIR3);
                    addToReplaceList(ir, toList); 
		}
		break;

	    case OpcodeConst.opc_dup_x2:
		{
		    JavaVariable op1 = stack.Pop();
		    JavaVariable op2 = stack.Pop();
                    ir.addOperand(op1);
                    ir.addOperand(op2);
		    if(op2.size()==1) {
			JavaVariable op3 = stack.Pop();
                        ir.addOperand(op3);
			JavaVariable target1 = stack.Push(op1.getType());
			JavaVariable target2 = stack.Push(op3.getType());
			JavaVariable target3 = stack.Push(op2.getType());
			JavaVariable target4 = stack.Push(op1.getType());
                        ir.addTarget(target1);
                        ir.addTarget(target2);
                        ir.addTarget(target3);
                        ir.addTarget(target4);
                        
                        // change dups to loads
			// TODO: Temporary removal
                        Vector toList = new Vector();
                        IR newIR1, newIR2, newIR3, newIR4;
                        newIR1 = makeCopyIR(ir, op1, target4);
                        newIR2 = makeCopyIR(ir, op2, target3);
                        newIR3 = makeCopyIR(ir, op3, target2);
                        newIR4 = makeCopyIR(ir, target4, target1);
                        newIR1.addDebugInfoString("Generated by " + ir.getBpc());
                        newIR2.addDebugInfoString("Generated by " + ir.getBpc());
                        newIR3.addDebugInfoString("Generated by " + ir.getBpc());
                        newIR4.addDebugInfoString("Generated by " + ir.getBpc());
                        toList.addElement(newIR1);
                        toList.addElement(newIR2);
                        toList.addElement(newIR3);
                        toList.addElement(newIR4);
                        addToReplaceList(ir, toList); 

		    } else {
			JavaVariable target1 = stack.Push(op1.getType());
			JavaVariable target2 = stack.Push(op2.getType());
			JavaVariable target3 = stack.Push(op1.getType());
                        ir.addTarget(target1);
                        ir.addTarget(target2);
                        ir.addTarget(target3);
                        // change dups to loads
                        Vector toList = new Vector();
                        IR newIR1, newIR2, newIR3;
                        newIR1 = makeCopyIR(ir, op1, target3);
                        newIR2 = makeCopyIR(ir, op2, target2);
                        newIR3 = makeCopyIR(ir, target3, target1);
                        newIR1.addDebugInfoString("Generated by " + ir.getBpc());
                        newIR2.addDebugInfoString("Generated by " + ir.getBpc());
                        newIR3.addDebugInfoString("Generated by " + ir.getBpc());
                        toList.addElement(newIR1);
                        toList.addElement(newIR2);
                        toList.addElement(newIR3);
                        addToReplaceList(ir, toList); 
		    }
		}
		break;

	    case OpcodeConst.opc_dup2:
		{
		    JavaVariable op1 = stack.Pop();
                    ir.addOperand(op1);
		    if(op1.size()==1) {
			JavaVariable op2 = stack.Pop();
                        ir.addOperand(op2);
			JavaVariable target1 = stack.Push(op2.getType());
			JavaVariable target2 = stack.Push(op1.getType());
			JavaVariable target3 = stack.Push(op2.getType());
			JavaVariable target4 = stack.Push(op1.getType());
                        ir.addTarget(target1);
                        ir.addTarget(target2);
                        ir.addTarget(target3);
                        ir.addTarget(target4);
                        
                        // change dups to loads
                        Vector toList = new Vector();
                        IR newIR1, newIR2, newIR3, newIR4;
                        newIR1 = makeCopyIR(ir, op1, target4);
                        newIR2 = makeCopyIR(ir, op2, target3);
                        //newIR3 = makeCopyIR(ir, target4, target2);
                        //newIR4 = makeCopyIR(ir, target3, target1);
                        newIR1.addDebugInfoString("Generated by " + ir.getBpc());
                        newIR2.addDebugInfoString("Generated by " + ir.getBpc());
                        //newIR3.addDebugInfoString("Generated by " + ir.getBpc());
                        //newIR4.addDebugInfoString("Generated by " + ir.getBpc());
                        toList.addElement(newIR1);
                        toList.addElement(newIR2);
                        //toList.addElement(newIR3);
                        //toList.addElement(newIR4);
                        addToReplaceList(ir, toList); 
		    } else {
			JavaVariable target1 = stack.Push(op1.getType());
			JavaVariable target2 = stack.Push(op1.getType());
                        ir.addTarget(target1);
                        ir.addTarget(target2);
                        
                        // change dups to loads
                        Vector toList = new Vector();
                        IR newIR1;
                        newIR1 = makeCopyIR(ir, op1, target2);
                        newIR1.addDebugInfoString("Generated by " + ir.getBpc());
                        toList.addElement(newIR1);
                        addToReplaceList(ir, toList); 
		    }
		}
		break; 

	    case OpcodeConst.opc_dup2_x1:
		{
		    JavaVariable op1 = stack.Pop();
		    JavaVariable op2 = stack.Pop();

                    ir.addOperand(op1);
                    ir.addOperand(op2);

		    if(op1.size()==1) {
			JavaVariable op3 = stack.Pop();
                        ir.addOperand(op3);
			JavaVariable target1 = stack.Push(op2.getType());
			JavaVariable target2 = stack.Push(op1.getType());
			JavaVariable target3 = stack.Push(op3.getType());
			JavaVariable target4 = stack.Push(op2.getType());
			JavaVariable target5 = stack.Push(op1.getType());
                        ir.addTarget(target1);
                        ir.addTarget(target2);
                        ir.addTarget(target3);
                        ir.addTarget(target4);
                        ir.addTarget(target5);
                        
                        // change dups to loads
                        Vector toList = new Vector();
                        IR newIR1, newIR2, newIR3, newIR4, newIR5;
                        newIR1 = makeCopyIR(ir, op1, target5);
                        newIR2 = makeCopyIR(ir, op2, target4);
                        newIR3 = makeCopyIR(ir, op3, target3);
                        newIR4 = makeCopyIR(ir, target5, target2);
                        newIR5 = makeCopyIR(ir, target4, target1);
                        newIR1.addDebugInfoString("Generated by " + ir.getBpc());
                        newIR2.addDebugInfoString("Generated by " + ir.getBpc());
                        newIR3.addDebugInfoString("Generated by " + ir.getBpc());
                        newIR4.addDebugInfoString("Generated by " + ir.getBpc());
                        newIR5.addDebugInfoString("Generated by " + ir.getBpc());
                        toList.addElement(newIR1);
                        toList.addElement(newIR2);
                        toList.addElement(newIR3);
                        toList.addElement(newIR4);
                        toList.addElement(newIR5);
                        addToReplaceList(ir, toList); 
		    } else {
			JavaVariable target1 = stack.Push(op1.getType());
			JavaVariable target2 = stack.Push(op2.getType());
			JavaVariable target3 = stack.Push(op1.getType());
                        ir.addTarget(target1);
                        ir.addTarget(target2);
                        ir.addTarget(target3);
                        
                        // change dups to loads
                        Vector toList = new Vector();
                        IR newIR1, newIR2, newIR3;
                        newIR1 = makeCopyIR(ir, op1, target3);
                        newIR2 = makeCopyIR(ir, op2, target2);
                        newIR3 = makeCopyIR(ir, target3, target1);
                        newIR1.addDebugInfoString("Generated by " + ir.getBpc());
                        newIR2.addDebugInfoString("Generated by " + ir.getBpc());
                        newIR3.addDebugInfoString("Generated by " + ir.getBpc());
                        toList.addElement(newIR1);
                        toList.addElement(newIR2);
                        toList.addElement(newIR3);
                        addToReplaceList(ir, toList); 
		    }
		}
		break;

	    case OpcodeConst.opc_dup2_x2:
		{
		    JavaVariable op1 = stack.Pop();
		    JavaVariable op2 = stack.Pop();
                    ir.addOperand(op1);
                    ir.addOperand(op2);

		    if(op1.size() == 1) {
			JavaVariable op3 = stack.Pop();
                        ir.addOperand(op3);
			if(op3.size() == 1) {
			    JavaVariable op4 = stack.Pop();
                            ir.addOperand(op4);
			    JavaVariable target1 = stack.Push(op2.getType());
			    JavaVariable target2 = stack.Push(op1.getType());
			    JavaVariable target3 = stack.Push(op4.getType());
			    JavaVariable target4 = stack.Push(op3.getType());
			    JavaVariable target5 = stack.Push(op2.getType());
			    JavaVariable target6 = stack.Push(op1.getType());
                            ir.addTarget(target1);
                            ir.addTarget(target2);
                            ir.addTarget(target3);
                            ir.addTarget(target4);
                            ir.addTarget(target5);
                            ir.addTarget(target6);
                        
                            // change dups to loads
                            Vector toList = new Vector();
                            IR newIR1, newIR2, newIR3, newIR4, newIR5, newIR6;
                            newIR1 = makeCopyIR(ir, op1, target6);
                            newIR2 = makeCopyIR(ir, op2, target5);
                            newIR3 = makeCopyIR(ir, op3, target4);
                            newIR4 = makeCopyIR(ir, op4, target3);
                            newIR5 = makeCopyIR(ir, target6, target2);
                            newIR6 = makeCopyIR(ir, target5, target1);
                            newIR1.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR2.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR3.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR4.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR5.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR6.addDebugInfoString("Generated by " + ir.getBpc());
                            toList.addElement(newIR1);
                            toList.addElement(newIR2);
                            toList.addElement(newIR3);
                            toList.addElement(newIR4);
                            toList.addElement(newIR5);
                            toList.addElement(newIR6);
                            addToReplaceList(ir, toList); 
			} else {
			    JavaVariable target1 = stack.Push(op2.getType());
			    JavaVariable target2 = stack.Push(op1.getType());
			    JavaVariable target3 = stack.Push(op3.getType());
			    JavaVariable target4 = stack.Push(op2.getType());
			    JavaVariable target5 = stack.Push(op1.getType());
                            ir.addTarget(target1);
                            ir.addTarget(target2);
                            ir.addTarget(target3);
                            ir.addTarget(target4);
                            ir.addTarget(target5);
                            
                            // change dups to loads
                            Vector toList = new Vector();
                            IR newIR1, newIR2, newIR3, newIR4, newIR5;
                            newIR1 = makeCopyIR(ir, op1, target5);
                            newIR2 = makeCopyIR(ir, op2, target4);
                            newIR3 = makeCopyIR(ir, op3, target3);
                            newIR4 = makeCopyIR(ir, target5, target2);
                            newIR5 = makeCopyIR(ir, target4, target1);
                            newIR1.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR2.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR3.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR4.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR5.addDebugInfoString("Generated by " + ir.getBpc());
                            toList.addElement(newIR1);
                            toList.addElement(newIR2);
                            toList.addElement(newIR3);
                            toList.addElement(newIR4);
                            toList.addElement(newIR5);
                            addToReplaceList(ir, toList); 
			}
		    } else {
			if(op2.size() == 1) {
			    JavaVariable op3 = stack.Pop();
			    JavaVariable target1 = stack.Push(op1.getType());
			    JavaVariable target2 = stack.Push(op3.getType());
			    JavaVariable target3 = stack.Push(op2.getType());
			    JavaVariable target4 = stack.Push(op1.getType());
                            ir.addTarget(target1);
                            ir.addTarget(target2);
                            ir.addTarget(target3);
                            ir.addTarget(target4);

                            // change dups to loads
                            Vector toList = new Vector();
                            IR newIR1, newIR2, newIR3, newIR4;
                            newIR1 = makeCopyIR(ir, op1, target4);
                            newIR2 = makeCopyIR(ir, op2, target3);
                            newIR3 = makeCopyIR(ir, op3, target2);
                            newIR4 = makeCopyIR(ir, target4, target1);
                            newIR1.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR2.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR3.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR4.addDebugInfoString("Generated by " + ir.getBpc());
                            toList.addElement(newIR1);
                            toList.addElement(newIR2);
                            toList.addElement(newIR3);
                            toList.addElement(newIR4);
                            addToReplaceList(ir, toList); 
			} else {
			    JavaVariable target1 = stack.Push(op1.getType());
			    JavaVariable target2 = stack.Push(op2.getType());
			    JavaVariable target3 = stack.Push(op1.getType());
                            ir.addTarget(target1);
                            ir.addTarget(target2);
                            ir.addTarget(target3);

                            // change dups to loads
                            Vector toList = new Vector();
                            IR newIR1, newIR2, newIR3;
                            newIR1 = makeCopyIR(ir, op1, target3);
                            newIR2 = makeCopyIR(ir, op2, target2);
                            newIR3 = makeCopyIR(ir, target3, target1);
                            newIR1.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR2.addDebugInfoString("Generated by " + ir.getBpc());
                            newIR3.addDebugInfoString("Generated by " + ir.getBpc());
                            toList.addElement(newIR1);
                            toList.addElement(newIR2);
                            toList.addElement(newIR3);
                            addToReplaceList(ir, toList); 
			}
		    }
		}
		break;

	    case OpcodeConst.opc_swap:
		{
		    JavaVariable op1 = stack.Pop();
		    JavaVariable op2 = stack.Pop();
                    JavaVariable target1 = stack.Push(op1.getType());
                    JavaVariable target2 = stack.Push(op2.getType());
                    ir.addOperand(op1);
                    ir.addOperand(op2);
                    ir.addTarget(target1);
                    ir.addTarget(target2);
		}
		break;

		// 0x60 ~ 0x84 All arithmetic instructions
	    case OpcodeConst.opc_drem:
		{
		    JavaVariable op1 = stack.Pop(TypeInfo.TYPE_DOUBLE);  
		    JavaVariable op2 = stack.Pop(TypeInfo.TYPE_DOUBLE);
                    JavaVariable target1 = stack.Push(op1.getType());
                    ir.addOperand(op2);
                    ir.addOperand(op1);
                    ir.addTarget(target1);
		}
		break;
	    case OpcodeConst.opc_iadd: case OpcodeConst.opc_ladd: case OpcodeConst.opc_fadd: case OpcodeConst.opc_dadd:
	    case OpcodeConst.opc_isub: case OpcodeConst.opc_lsub: case OpcodeConst.opc_fsub: case OpcodeConst.opc_dsub:
	    case OpcodeConst.opc_imul: case OpcodeConst.opc_lmul: case OpcodeConst.opc_fmul: case OpcodeConst.opc_dmul:
	    case OpcodeConst.opc_idiv: case OpcodeConst.opc_ldiv: case OpcodeConst.opc_fdiv: case OpcodeConst.opc_ddiv:
	    case OpcodeConst.opc_irem: case OpcodeConst.opc_lrem: case OpcodeConst.opc_frem:
	    case OpcodeConst.opc_ineg: case OpcodeConst.opc_lneg: case OpcodeConst.opc_fneg: case OpcodeConst.opc_dneg:
	    case OpcodeConst.opc_ishl: case OpcodeConst.opc_lshl: case OpcodeConst.opc_ishr: case OpcodeConst.opc_lshr:
	    case OpcodeConst.opc_iushr: case OpcodeConst.opc_lushr: case OpcodeConst.opc_iand: case OpcodeConst.opc_land:			
	    case OpcodeConst.opc_ior: case OpcodeConst.opc_lor: case OpcodeConst.opc_ixor: case OpcodeConst.opc_lxor:
	    case OpcodeConst.opc_iinc:			
		{
		    int type = -1;;
		    if (shortOpcode == OpcodeConst.opc_iadd || shortOpcode == OpcodeConst.opc_isub ||
			    shortOpcode == OpcodeConst.opc_imul || shortOpcode == OpcodeConst.opc_idiv ||
			    shortOpcode == OpcodeConst.opc_irem || shortOpcode == OpcodeConst.opc_ineg ||
			    shortOpcode == OpcodeConst.opc_ishl || shortOpcode == OpcodeConst.opc_ishr ||
			    shortOpcode == OpcodeConst.opc_iushr || shortOpcode == OpcodeConst.opc_iand ||
			    shortOpcode == OpcodeConst.opc_ior || shortOpcode == OpcodeConst.opc_ixor ||
			    shortOpcode == OpcodeConst.opc_iinc) {
			type = TypeInfo.TYPE_INT;
		    } else if (shortOpcode == OpcodeConst.opc_ladd || shortOpcode == OpcodeConst.opc_lsub ||
			    shortOpcode == OpcodeConst.opc_lmul || shortOpcode == OpcodeConst.opc_ldiv ||
			    shortOpcode == OpcodeConst.opc_lrem || shortOpcode == OpcodeConst.opc_lneg ||
			    shortOpcode == OpcodeConst.opc_lshl || shortOpcode == OpcodeConst.opc_lshr ||
			    shortOpcode == OpcodeConst.opc_lushr || shortOpcode == OpcodeConst.opc_land ||
			    shortOpcode == OpcodeConst.opc_lor || shortOpcode == OpcodeConst.opc_lxor) {
			type = TypeInfo.TYPE_LONG;
		    } else if (shortOpcode == OpcodeConst.opc_fadd || shortOpcode == OpcodeConst.opc_fsub ||
			    shortOpcode == OpcodeConst.opc_fmul || shortOpcode == OpcodeConst.opc_fdiv ||
			    shortOpcode == OpcodeConst.opc_frem || shortOpcode == OpcodeConst.opc_fneg) {
			type = TypeInfo.TYPE_FLOAT;
		    } else if (shortOpcode == OpcodeConst.opc_dadd || shortOpcode == OpcodeConst.opc_dsub ||
			    shortOpcode == OpcodeConst.opc_dmul || shortOpcode == OpcodeConst.opc_ddiv ||
			    shortOpcode == OpcodeConst.opc_drem || shortOpcode == OpcodeConst.opc_dneg) {
			type = TypeInfo.TYPE_DOUBLE;
		    }

                    String operator = null;

		    /** Binary operators : OpcodeConst.opc_iadd ~ OpcodeConst.opc_drem(Algebra) 
		     *                     OpcodeConst.opc_ishl ~ OpcodeConst.opc_lxor(Logical)
		     */
		    if ((shortOpcode >= OpcodeConst.opc_iadd && shortOpcode <= OpcodeConst.opc_drem)
			    || (shortOpcode >= OpcodeConst.opc_ishl && shortOpcode <= OpcodeConst.opc_lxor)) { 

			// Get operand1 , operand2 , target
			JavaVariable op1 = stack.Pop(type);  
			JavaVariable op2 = stack.Pop(type);

			// Get operator(binary)
			if (shortOpcode >= OpcodeConst.opc_iadd && shortOpcode <= OpcodeConst.opc_dadd) { // Adds
			    operator = " + "; 
			} else if (shortOpcode >= OpcodeConst.opc_isub && shortOpcode <= OpcodeConst.opc_dsub) { // Subs
			    operator = " - ";					
			} else if (shortOpcode >= OpcodeConst.opc_imul && shortOpcode <= OpcodeConst.opc_dmul) { // Subs
			    operator = " * ";
			} else if (shortOpcode >= OpcodeConst.opc_idiv && shortOpcode <= OpcodeConst.opc_ddiv) { // Subs
			    operator = " / ";
			} else if (shortOpcode >= OpcodeConst.opc_irem && shortOpcode <= OpcodeConst.opc_drem) { // Subs
			    operator = " % ";					
			} else if (shortOpcode == OpcodeConst.opc_ishl || shortOpcode == OpcodeConst.opc_lshl) { // SHLs
			    operator = " << ";
			} else if (shortOpcode == OpcodeConst.opc_ishr || shortOpcode == OpcodeConst.opc_lshr) { // SHRs
			    operator = " >> ";
			} else if (shortOpcode == OpcodeConst.opc_iushr || shortOpcode == OpcodeConst.opc_lushr) { // Shls
			    operator = " >> ";
			} else if (shortOpcode == OpcodeConst.opc_iand || shortOpcode == OpcodeConst.opc_land) { // Ands
			    operator = " & ";
			} else if (shortOpcode == OpcodeConst.opc_ior || shortOpcode == OpcodeConst.opc_lor) { // Ands
			    operator = " | ";
			} else if (shortOpcode == OpcodeConst.opc_ixor || shortOpcode == OpcodeConst.opc_lxor) { // Ands
			    operator = " ^ ";
			}
                        ir.addTarget(stack.Push(type));
                        ir.addOperand(op2);
                        ir.addOperand(op1);
                        ir.setOperator(operator);
		    }

		    /** Unary operators : OpcodeConst.opc_ineg ~ OpcodeConst.opc_dneg
		     * 
		     */
		    if (shortOpcode >= OpcodeConst.opc_ineg && shortOpcode <= OpcodeConst.opc_dneg) { 
			// Get operand1 , target
			JavaVariable operand1 = stack.Pop(type);
			JavaVariable target1 = stack.Push(type);
                        ir.addTarget(target1);
                        ir.addOperand(operand1);
                        ir.setOperator("-");
		    } 

		    /** Incremental operators : OpcodeConst.opc_iinc 
		     * 
		     */                   
		    if (shortOpcode == OpcodeConst.opc_iinc) {
			LocalVariable operand = new LocalVariable(type, bytecode.getOperand(0));
			Constant constant = new Constant(type, Integer.toString(bytecode.getOperand(1)));
                        ir.addTarget(operand);
                        ir.addOperand(operand);
			ir.addOperand(constant);
                        ir.setOperator("+");
		    }
		}
		break;

		// Cast operators I2L(0x85) ~ I2S(0x93)
	    case OpcodeConst.opc_i2l: case OpcodeConst.opc_i2f: case OpcodeConst.opc_i2d:
	    case OpcodeConst.opc_l2i: case OpcodeConst.opc_l2f: case OpcodeConst.opc_l2d:
	    case OpcodeConst.opc_f2i: case OpcodeConst.opc_f2l: case OpcodeConst.opc_f2d:
	    case OpcodeConst.opc_d2i: case OpcodeConst.opc_d2l: case OpcodeConst.opc_d2f:
	    case OpcodeConst.opc_i2b: case OpcodeConst.opc_i2c: case OpcodeConst.opc_i2s:
		{
		    int fromType = -1;
		    int toType = -1; 

		    // First, Determine (cast) from type.
		    if (shortOpcode == OpcodeConst.opc_i2l || shortOpcode == OpcodeConst.opc_i2f
			    || shortOpcode == OpcodeConst.opc_i2d || shortOpcode == OpcodeConst.opc_i2b
			    || shortOpcode == OpcodeConst.opc_i2c || shortOpcode == OpcodeConst.opc_i2s) {
			fromType = TypeInfo.TYPE_INT;
		    } else if (shortOpcode == OpcodeConst.opc_l2i || shortOpcode == OpcodeConst.opc_l2f
			    || shortOpcode == OpcodeConst.opc_l2d) {
			fromType = TypeInfo.TYPE_LONG;
		    } else if (shortOpcode == OpcodeConst.opc_f2i || shortOpcode == OpcodeConst.opc_f2l
			    || shortOpcode == OpcodeConst.opc_f2d) {
			fromType = TypeInfo.TYPE_FLOAT;
		    } else if (shortOpcode == OpcodeConst.opc_d2i || shortOpcode == OpcodeConst.opc_d2l
			    || shortOpcode == OpcodeConst.opc_d2f) {
			fromType = TypeInfo.TYPE_DOUBLE;
		    } else {
			new aotc.share.Assert(false, "Your AOTC Compiler may have problem in executing casting instruction.");
		    }

		    // Second, Determine (cast) to type.
		    if (shortOpcode == OpcodeConst.opc_l2i || shortOpcode == OpcodeConst.opc_f2i 
			    || shortOpcode == OpcodeConst.opc_d2i) {
			toType = TypeInfo.TYPE_INT;
		    } else if (shortOpcode == OpcodeConst.opc_i2l || shortOpcode == OpcodeConst.opc_f2l 
			    || shortOpcode == OpcodeConst.opc_d2l) {
			toType = TypeInfo.TYPE_LONG;
		    } else if (shortOpcode == OpcodeConst.opc_i2f || shortOpcode == OpcodeConst.opc_l2f
			    || shortOpcode == OpcodeConst.opc_d2f) {
			toType = TypeInfo.TYPE_FLOAT;
		    } else if (shortOpcode == OpcodeConst.opc_i2d || shortOpcode == OpcodeConst.opc_l2d
			    || shortOpcode == OpcodeConst.opc_f2d) {
			toType = TypeInfo.TYPE_DOUBLE;
		    } else if (shortOpcode == OpcodeConst.opc_i2b) {
			toType = TypeInfo.TYPE_BYTE;
		    } else if (shortOpcode == OpcodeConst.opc_i2c) {
			toType = TypeInfo.TYPE_CHAR;
		    } else if (shortOpcode == OpcodeConst.opc_i2s) {
			toType = TypeInfo.TYPE_SHORT;
		    } else {
			new aotc.share.Assert(false, "Your AOTC Compiler may have problem in executing casting instruction.");
		    }

		    JavaVariable source = stack.Pop(fromType);
		    JavaVariable target = stack.Push(toType);
                    ir.addOperand(source);
                    ir.addTarget(target);
		}
		break;			

	    case OpcodeConst.opc_lcmp: 
		{
		    String condition1;
		    String condition2;

		    JavaVariable op1 = stack.Pop(TypeInfo.TYPE_LONG);
		    JavaVariable op2 = stack.Pop(TypeInfo.TYPE_LONG);
		    JavaVariable target = stack.Push(TypeInfo.TYPE_INT);

                    ir.addTarget(target);
                    ir.addOperand(op2);
                    ir.addOperand(op1);
		}
		break;
	    case OpcodeConst.opc_fcmpl: case OpcodeConst.opc_fcmpg: 
	    case OpcodeConst.opc_dcmpl: case OpcodeConst.opc_dcmpg:
		{
                    int type;
		    if (shortOpcode == OpcodeConst.opc_fcmpl) {
			type = TypeInfo.TYPE_FLOAT;
		    } else if ( shortOpcode == OpcodeConst.opc_fcmpg) {
			type = TypeInfo.TYPE_FLOAT;
		    } else if ( shortOpcode == OpcodeConst.opc_dcmpl) {
			type = TypeInfo.TYPE_DOUBLE;
		    } else {
			type = TypeInfo.TYPE_DOUBLE;
		    }

		    JavaVariable op1 = stack.Pop(type);
		    JavaVariable op2 = stack.Pop(type);
		    JavaVariable target = stack.Push(TypeInfo.TYPE_INT);

                    ir.addTarget(target);
                    ir.addOperand(op2);
                    ir.addOperand(op1);
		}
		break;

		// All Conditional Branches : 0x99 ~ 0xA6
		// 198~199 ifnull ifnonnull
	    case OpcodeConst.opc_ifeq:	    case OpcodeConst.opc_ifne:	    case OpcodeConst.opc_iflt:
	    case OpcodeConst.opc_ifge:	    case OpcodeConst.opc_ifgt:	    case OpcodeConst.opc_ifle:
	    case OpcodeConst.opc_if_icmpeq: case OpcodeConst.opc_if_icmpne: case OpcodeConst.opc_if_icmplt: 
	    case OpcodeConst.opc_if_icmpge: case OpcodeConst.opc_if_icmpgt: case OpcodeConst.opc_if_icmple:
	    case OpcodeConst.opc_if_acmpeq: case OpcodeConst.opc_if_acmpne:
	    case OpcodeConst.opc_ifnull:    case OpcodeConst.opc_ifnonnull: case OpcodeConst.opc_nonnull_quick:
		{
                    int type;
		    JavaVariable operand1;
		    JavaVariable operand2;

		    // Get the type of operand.
		    if (shortOpcode >= OpcodeConst.opc_ifeq && shortOpcode <= OpcodeConst.opc_if_icmple) {
			type = TypeInfo.TYPE_INT;
		    } else { //IF_ACMP~
			type = TypeInfo.TYPE_REFERENCE;
		    }

		    // Get real operand of the type.
		    // Do not chane the order of getting the operand2 and operand1.
		    if ( (shortOpcode >= OpcodeConst.opc_ifeq && shortOpcode <= OpcodeConst.opc_ifle)
			    || shortOpcode == OpcodeConst.opc_ifnull || shortOpcode == OpcodeConst.opc_ifnonnull
			    || shortOpcode == OpcodeConst.opc_nonnull_quick)
		    {
			if(type!=TypeInfo.TYPE_REFERENCE) {
			    operand2 = new Constant(TypeInfo.TYPE_INT, "0");
			} else {
			    operand2 = new Constant(TypeInfo.TYPE_REFERENCE, "NULL");
			}
		    } else {
			operand2 = stack.Pop(type);
		    }
		    operand1 = stack.Pop(type);
                    
                    String compareOperator = null;
		    switch(shortOpcode)
		    {
			case OpcodeConst.opc_ifeq: case OpcodeConst.opc_if_icmpeq: case OpcodeConst.opc_if_acmpeq:
			case OpcodeConst.opc_ifnull:
			    compareOperator = new String(" == ");
			    break;
			case OpcodeConst.opc_ifne: case OpcodeConst.opc_if_icmpne: case OpcodeConst.opc_if_acmpne:
			case OpcodeConst.opc_ifnonnull: case OpcodeConst.opc_nonnull_quick:
			    compareOperator = new String(" != ");
			    break;
			case OpcodeConst.opc_iflt: case OpcodeConst.opc_if_icmplt: 
			    compareOperator = new String(" < ");
			    break;
			case OpcodeConst.opc_ifge: case OpcodeConst.opc_if_icmpge:
			    compareOperator = new String(" >= ");
			    break;					
			case OpcodeConst.opc_ifgt: case OpcodeConst.opc_if_icmpgt:
			    compareOperator = new String(" > ");
			    break;
			case OpcodeConst.opc_ifle: case OpcodeConst.opc_if_icmple:
			    compareOperator = new String(" <= ");
			    break;
			default:
			    new aotc.share.Assert (false, "not available condition at "+bytecode.getString());
			    break;
		    }
		   
                    ir.addOperand(operand1);
                    ir.addOperand(operand2);
                    //ir.addBranchTarget(getBranchTarget());
                    ir.setOperator(compareOperator);
		}
		break;

	    case OpcodeConst.opc_goto: case OpcodeConst.opc_goto_w:
                {
                    //ir.addBranchTarget(getBranchTarget());
                }
		break;

	    case OpcodeConst.opc_jsr: case OpcodeConst.opc_jsr_w:
		{
                    //ir.addBranchTarget(bytecode.getBranchTarget().getBpc());
		    ir.addTarget(stack.Push(TypeInfo.TYPE_REFERENCE));
		}
		break;
	    case OpcodeConst.opc_ret:
		{
		    int localVariableIndex = bytecode.getOperand(0);
		    int type = TypeInfo.TYPE_REFERENCE;
		    LocalVariable operand = new LocalVariable(type, localVariableIndex);
		    ir.addOperand(operand);
		}
		break;

	    case OpcodeConst.opc_tableswitch: // 0xaa
		{
                    JavaVariable operand = stack.Pop(TypeInfo.TYPE_INT);
                    ir.addOperand(operand);

                    // default bpc
                    int defaultBpc = bytecode.getOperand(0) + bytecode.getBpc();
                    // low
                    int low = bytecode.getOperand(1);
                    ir.addOperand(new Constant(TypeInfo.TYPE_INT, Integer.toString(low)));
                    // high
                    int high = bytecode.getOperand(2);
                    ir.addOperand(new Constant(TypeInfo.TYPE_INT, Integer.toString(high)));
                    
                    int branchTarget;
                    // set Branch targets
	            for (int i = 0; i < high - low + 1; i++) {
                        branchTarget =  bytecode.getOperand(i + 3) + bytecode.getBpc();
	                //ir.addBranchTarget(branchTarget);
                        ir.addOperand(new Constant(TypeInfo.TYPE_INT, Integer.toString(branchTarget)));
                    }
                    // default brancg target is located at last cell
                    //ir.addBranchTarget(defaultBpc);
                    ir.addOperand(new Constant(TypeInfo.TYPE_INT, Integer.toString(defaultBpc)));
            	}
		break;

	    case OpcodeConst.opc_lookupswitch: // 0xaa
		{
                    JavaVariable operand = stack.Pop(TypeInfo.TYPE_INT);
                    ir.addOperand(operand);
                    
                    // Operand(0) is offset from current Bpc.
	            int defaultBpc = bytecode.getOperand(0) + bytecode.getBpc();
                    
                    int branchTarget;
                    // set Branch targets
	            for (int i = 0; i < bytecode.getOperand(1) * 2; i += 2) {
                        branchTarget =  bytecode.getOperand(i + 3) + bytecode.getBpc();
	                ir.addOperand(new Constant(TypeInfo.TYPE_INT, Integer.toString(bytecode.getOperand(i + 2))));
	                //ir.addBranchTarget(branchTarget);
                        ir.addOperand(new Constant(TypeInfo.TYPE_INT, Integer.toString(branchTarget)));
	            }
                    // default brancg target is located at last cell
                    //ir.addBranchTarget(defaultBpc);
                    ir.addOperand(new Constant(TypeInfo.TYPE_INT, Integer.toString(defaultBpc)));
		}
		break;

		// 0xAC ~ 0xB1
	    case OpcodeConst.opc_ireturn:
	    case OpcodeConst.opc_lreturn:
	    case OpcodeConst.opc_freturn:
	    case OpcodeConst.opc_dreturn:
	    case OpcodeConst.opc_areturn:
	    case OpcodeConst.opc_return:						 	
		{
		    JavaVariable returnValue = null;

		    int type = -1;
		    switch(shortOpcode) {
			case OpcodeConst.opc_ireturn:
			    returnValue = stack.Pop(TypeInfo.TYPE_INT);
			    break;
			case OpcodeConst.opc_lreturn:
			    returnValue = stack.Pop(TypeInfo.TYPE_LONG);
			    break;
			case OpcodeConst.opc_freturn:
			    returnValue = stack.Pop(TypeInfo.TYPE_FLOAT);
			    break;
			case OpcodeConst.opc_dreturn:
			    returnValue = stack.Pop(TypeInfo.TYPE_DOUBLE);
			    break;
			case OpcodeConst.opc_areturn:
			    returnValue = stack.Pop(TypeInfo.TYPE_REFERENCE);
			    break;
			case OpcodeConst.opc_return:
			    returnValue = null;
			    break;
			default:
			    new aotc.share.Assert(false, "Cannot determine return type at " + bytecode.getString());
			    break;
		    }
		    new aotc.share.Assert(returnValue==null || returnValue.getIndex()==0, "return index error!! : " + returnValue);
		    //codeGenerator.doReturn(returnValue);
                    if (returnValue != null) {
                        ir.addOperand(returnValue);
                        //cfg.setReturnType(returnValue.getType());
                        cfg.setNeedExitCode(true);
                    } else {
                        cfg.setNeedExitCode(true);
                    }
                    new aotc.share.Assert(stack.size() == 0 , "stack shuld be empty after return");
		}
		break;

	    case OpcodeConst.opc_newarray: 
		{
		    int arrayType = bytecode.getOperand(0);
		    JavaVariable length = stack.Pop(TypeInfo.TYPE_INT);
		    JavaVariable arrayRef = stack.Push(TypeInfo.TYPE_REFERENCE);
                    ir.addTarget(arrayRef);
                    ir.addOperand(length);
                    ir.addOperand(new Constant(TypeInfo.TYPE_INT, Integer.toString(arrayType)));
		}
		break;

	    case OpcodeConst.opc_arraylength:
		{
		    JavaVariable array = stack.Pop(TypeInfo.TYPE_REFERENCE);
                    JavaVariable target = stack.Push(TypeInfo.TYPE_INT);
                    ir.addTarget(target);
                    ir.addOperand(array);
		}
		break;

	    case OpcodeConst.opc_athrow:
		{
                    JavaVariable operand = stack.Pop(TypeInfo.TYPE_REFERENCE);
                    ir.addOperand(operand);
		}
		break;

	    case OpcodeConst.opc_monitorenter: //194
		{
                    JavaVariable operand = stack.Pop(TypeInfo.TYPE_REFERENCE);
                    ir.addOperand(operand);
		}
		break;

	    case OpcodeConst.opc_monitorexit: // 195
		{
                    JavaVariable operand = stack.Pop(TypeInfo.TYPE_REFERENCE);
                    ir.addOperand(operand);
		}
		break;

	    case OpcodeConst.opc_aldc_ind_quick:
	    case OpcodeConst.opc_aldc_ind_w_quick:
		{
                    JavaVariable target = stack.Push(TypeInfo.TYPE_REFERENCE);
                    JavaVariable operand = new Constant(TypeInfo.TYPE_INT, Integer.toString(getCPIndex(bytecode)));
                    ir.addTarget(target);
                    ir.addOperand(operand);
		}
		break;

	    case OpcodeConst.opc_aldc_quick:
	    case OpcodeConst.opc_aldc_w_quick:
		{
                    JavaVariable target = stack.Push(TypeInfo.TYPE_REFERENCE);
                    JavaVariable operand = new Constant(TypeInfo.TYPE_INT, Integer.toString(getCPIndex(bytecode)));
                    ir.addTarget(target);
                    ir.addOperand(operand);
		}
		break;

	    case OpcodeConst.opc_ldc_quick:
	    case OpcodeConst.opc_ldc_w_quick:
		{
		    int index = getCPIndex(bytecode);
		    int cpType = getCPType(classInfo, index);
		    int variableType = -1;
		    int value = ((SingleValueConstant)constants[index]).value;
		    switch(cpType) {
			case Const.CONSTANT_INTEGER:
			    variableType = TypeInfo.TYPE_INT;
			    break;			
			case Const.CONSTANT_FLOAT:
			    variableType = TypeInfo.TYPE_FLOAT;
			    break;			
			default:
			    new aotc.share.Assert(false, "Cannot resolve constant pool at" + bytecode.getString());
		    }
		    Constant constant = new Constant(variableType, Integer.toString(value));
		    ir.addTarget(stack.Push(variableType));
                    ir.addOperand(constant);
		}
		break;

	    case OpcodeConst.opc_ldc2_w_quick:
		{
		    int index = getCPIndex(bytecode);
		    int cpType = getCPType(classInfo, index);
		    int variableType = -1;
		    int highValue = ((DoubleValueConstant)constants[index]).highVal;
		    int lowValue = ((DoubleValueConstant)constants[index]).lowVal;

		    switch(cpType) {
			case Const.CONSTANT_LONG:
			    variableType = TypeInfo.TYPE_LONG;
			    break;			
			case Const.CONSTANT_DOUBLE:
			    variableType = TypeInfo.TYPE_DOUBLE;
			    break;			
			default:
			    new aotc.share.Assert(false, "Cannot resolve constant pool at" + bytecode.getString());
		    }
		    // TODO IA32's double word is little-endian format.
		    //      It should be changed when ported to MIPS that has big-endian.
		    Constant constant1 = new Constant(variableType, "0x" + Integer.toHexString(highValue));
		    Constant constant2 = new Constant(variableType, "0x" + Integer.toHexString(lowValue));
                    ir.addTarget(stack.Push(variableType));
                    ir.addOperand(constant2);
                    ir.addOperand(constant1);
		}
		break;

	    case OpcodeConst.opc_invokestatic_checkinit_quick:
	    case OpcodeConst.opc_invokestatic_quick:
	    case OpcodeConst.opc_invokenonvirtual_quick:
        {
            int index = getCPIndex(bytecode);
            MethodConstant mc = (MethodConstant)constants[index];
            boolean isStatic = false;
            if (shortOpcode==OpcodeConst.opc_invokestatic_checkinit_quick
                    || shortOpcode==OpcodeConst.opc_invokestatic_quick) {
                isStatic = true;
            }
            String type = MethodDeclarationInfo.methodTypeList(mc.sig.type.string);
            int rettype = TypeInfo.toAOTCType(type.charAt(0));
            int args = type.length() - 1;

            JavaVariable ret = null;
            JavaVariable obj = null;
            JavaVariable arg[] = new JavaVariable[args];

            for (int i = args - 1; i >= 0 ; i--) {
                arg[i] = stack.Pop();
            }
            if (!isStatic) {  
                obj = stack.Pop(); 
            }
            if (rettype != TypeInfo.TYPE_VOID) { 
                ret = stack.Push(rettype);
            }

            if(ret != null) { ir.addTarget(ret); }
            if(obj != null) { ir.addOperand(obj); }
            for (int i = 0; i < args ; i++) {
                ir.addOperand(arg[i]);
            }

            // Adds cb for class init 
            if (shortOpcode == OpcodeConst.opc_invokestatic_checkinit_quick) {
                String cb = ((CVMClass)(mc.clas.find().vmClass)).getNativeName() + "_Classblock";
                ir.addClassBlockToInit(cb);
            }
        }
		break;
	    case OpcodeConst.opc_invokevirtual_quick:
	    case OpcodeConst.opc_ainvokevirtual_quick:
	    case OpcodeConst.opc_dinvokevirtual_quick:
	    case OpcodeConst.opc_vinvokevirtual_quick:
	    case OpcodeConst.opc_invokevirtualobject_quick:
	    case OpcodeConst.opc_invokevirtual_quick_w: // Clamp added (09-03-11)
        {
            MethodConstant mc = (MethodConstant)methodInfo.typeInfo.get(Integer.toString(bytecode.getBpc()));
            new aotc.share.Assert(mc!=null, "What's it? Let's debugging!!~~2" );
            String type = MethodDeclarationInfo.methodTypeList(mc.sig.type.string);
            int args = type.length() - 1;
            JavaVariable arg[] = new JavaVariable[args];
            for (int i = args - 1; i >= 0 ; i--) arg[i] = stack.Pop();
            JavaVariable ret;
            JavaVariable obj = stack.Pop();
            int rettype = TypeInfo.toAOTCType(type.charAt(0));

            if (rettype == TypeInfo.TYPE_VOID) {
                ret = null;
            } else {
                ret = stack.Push(rettype);
            }

            if(ret != null) ir.addTarget(ret);
            ir.addOperand(obj);

            for (int i = 0; i < args ; i++) {
                ir.addOperand(arg[i]);
            }
            // Clamp - 09-03-11
            bytecode.getOperands()[0] = mc.find().methodTableIndex;
            // END
        }
		break;

	    case OpcodeConst.opc_invokeinterface_quick:
		{
		    int index = getCPIndex(bytecode);
		    MethodConstant mc = (MethodConstant)constants[index];

		    String type = MethodDeclarationInfo.methodTypeList(mc.sig.type.string);
		    int args = type.length() - 1;
                    
                    JavaVariable arg[] = new JavaVariable[args];
                    for (int i = args - 1; i >= 0 ; i--) arg[i] = stack.Pop();
                    JavaVariable ret;
                    JavaVariable obj = stack.Pop();
                    int rettype = TypeInfo.toAOTCType(type.charAt(0));

                    if (rettype == TypeInfo.TYPE_VOID) {
                        ret = null;
                    } else {
                        ret = stack.Push(rettype);
                    }

                    if(ret != null) ir.addTarget(ret);
                    ir.addOperand(obj);

                    for (int i = 0; i < args ; i++) {
                        ir.addOperand(arg[i]);
                    }
		}
		break;

	    case OpcodeConst.opc_checkcast_quick:
		{
		    JavaVariable operand1 = stack.Peek();
                    JavaVariable operand2 = new Constant(TypeInfo.TYPE_INT, Integer.toString(getCPIndex(bytecode)));
                    ir.addOperand(operand1);
                    ir.addOperand(operand2);
		}
		break;

	    case OpcodeConst.opc_instanceof_quick:
		{
		    JavaVariable operand1 = stack.Pop(TypeInfo.TYPE_REFERENCE);
                    JavaVariable operand2 = new Constant(TypeInfo.TYPE_INT, Integer.toString(getCPIndex(bytecode)));
		    JavaVariable target =  stack.Push(TypeInfo.TYPE_INT);
                    ir.addOperand(operand1);
                    ir.addOperand(operand2);
                    ir.addTarget(target);
		}
		break;

		// TODO  exittransition

		// TODO checkinit code!!!!!!!!!!!! 

	    case OpcodeConst.opc_agetstatic_quick:
	    case OpcodeConst.opc_getstatic_quick:
	    case OpcodeConst.opc_agetstatic_checkinit_quick:
	    case OpcodeConst.opc_getstatic_checkinit_quick:
		{
		    int index = getCPIndex(bytecode);
		    String type = ((FieldConstant)constants[index]).sig.type.string;
		    int targetType = TypeInfo.toAOTCType(type.charAt(0));
                    JavaVariable target = stack.Push(targetType);
                    JavaVariable operand = new Constant(TypeInfo.TYPE_INT, Integer.toString(index));
                    ir.addTarget(target);
                    ir.addOperand(operand);
                    // Adds cb for class init 
                    if (shortOpcode == OpcodeConst.opc_agetstatic_checkinit_quick 
                            || shortOpcode == OpcodeConst.opc_getstatic_checkinit_quick) {
                        ClassInfo ci = ((FieldConstant)constants[index]).find().parent;
                        String ciNativeName = ((CVMClass)(ci.vmClass)).getNativeName()+"_Classblock";
                        ir.addClassBlockToInit(ciNativeName);
                    }
		}
		break;
	    case OpcodeConst.opc_getstatic2_quick:
	    case OpcodeConst.opc_getstatic2_checkinit_quick:
		{
		    int index = getCPIndex(bytecode);
		    String type = ((FieldConstant)constants[index]).sig.type.string;
		    int targetType = TypeInfo.toAOTCType(type.charAt(0));
                    JavaVariable target = stack.Push(targetType);
                    JavaVariable operand = new Constant(TypeInfo.TYPE_INT, Integer.toString(index));
                    ir.addTarget(target);
                    ir.addOperand(operand);
                    // Adds cb for class init 
                    if (shortOpcode == OpcodeConst.opc_getstatic2_checkinit_quick) {
                        ClassInfo ci = ((FieldConstant)constants[index]).find().parent;
                        String ciNativeName = ((CVMClass)(ci.vmClass)).getNativeName()+"_Classblock";
                        ir.addClassBlockToInit(ciNativeName);
                    }
		}
		break;
	    case OpcodeConst.opc_aputstatic_quick:
	    case OpcodeConst.opc_putstatic_quick:
	    case OpcodeConst.opc_aputstatic_checkinit_quick:
	    case OpcodeConst.opc_putstatic_checkinit_quick:
		{
		    int index = getCPIndex(bytecode);
		    String type = ((FieldConstant)constants[index]).sig.type.string;
		    int targetType = TypeInfo.toAOTCType(type.charAt(0));
                    JavaVariable operand1 = stack.Pop(targetType);
                    JavaVariable operand2 = new Constant(TypeInfo.TYPE_INT, Integer.toString(index));
                    ir.addOperand(operand1);
                    ir.addOperand(operand2);
                    // Adds cb for class init 
                    if (shortOpcode == OpcodeConst.opc_aputstatic_checkinit_quick 
                            || shortOpcode == OpcodeConst.opc_putstatic_checkinit_quick) {
                        ClassInfo ci = ((FieldConstant)constants[index]).find().parent;
                        String ciNativeName = ((CVMClass)(ci.vmClass)).getNativeName()+"_Classblock";
                        ir.addClassBlockToInit(ciNativeName);
                    }
		}
		break;
	    case OpcodeConst.opc_putstatic2_quick:
	    case OpcodeConst.opc_putstatic2_checkinit_quick:
		{
		    int index = getCPIndex(bytecode);
		    String type = ((FieldConstant)constants[index]).sig.type.string;
		    int targetType = TypeInfo.toAOTCType(type.charAt(0));
		    
                    JavaVariable operand1 = stack.Pop(targetType);
                    JavaVariable operand2 = new Constant(TypeInfo.TYPE_INT, Integer.toString(index));
                    ir.addOperand(operand1);
                    ir.addOperand(operand2);
                    // Adds cb for class init 
                    if (shortOpcode == OpcodeConst.opc_putstatic2_checkinit_quick) {
                        ClassInfo ci = ((FieldConstant)constants[index]).find().parent;
                        String ciNativeName = ((CVMClass)(ci.vmClass)).getNativeName()+"_Classblock";
                        ir.addClassBlockToInit(ciNativeName);
                    }
		}
		break;

	    case OpcodeConst.opc_getfield_quick:
	    case OpcodeConst.opc_getfield2_quick:
		{
		    int index = getCPIndex(bytecode);
		    String type = (String)methodInfo.typeInfo.get(Integer.toString(bytecode.getBpc()));
		    new aotc.share.Assert(type!=null, "What's it? Let's debugging!!~~1");
		    int targetType = TypeInfo.toAOTCType(type.charAt(0));
		    
                    JavaVariable operand1 = stack.Pop(TypeInfo.TYPE_REFERENCE);
                    JavaVariable operand2 = new Constant(TypeInfo.TYPE_INT, Integer.toString(index));
                    JavaVariable target = stack.Push(targetType);
                    ir.addTarget(target);
                    ir.addOperand(operand1);
                    ir.addOperand(operand2);
		}
		break;
	    case OpcodeConst.opc_agetfield_quick:
		{
		    int index = getCPIndex(bytecode);
                    JavaVariable operand1 = stack.Pop(TypeInfo.TYPE_REFERENCE);
                    JavaVariable operand2 = new Constant(TypeInfo.TYPE_INT, Integer.toString(index));
                    JavaVariable target = stack.Push(TypeInfo.TYPE_REFERENCE);
                    
                    ir.addTarget(target);
                    ir.addOperand(operand1);
                    ir.addOperand(operand2);
		}
		break;
	    case OpcodeConst.opc_getfield_quick_w:
		{
		    int index = getCPIndex(bytecode);
		    String type = ((FieldConstant)constants[index]).sig.type.string;
		    int targetType = TypeInfo.toAOTCType(type.charAt(0));

                    JavaVariable addr = stack.Pop(TypeInfo.TYPE_REFERENCE);
                    JavaVariable operand2 = new Constant(TypeInfo.TYPE_INT, Integer.toString(index));
                    JavaVariable target = stack.Push(targetType);
                    
                    ir.addTarget(target);
                    ir.addOperand(addr);
                    ir.addOperand(operand2);
		}
		break;

	    case OpcodeConst.opc_putfield_quick:
	    case OpcodeConst.opc_putfield2_quick:
		{
		    int cpindex = getCPIndex(bytecode);
                    JavaVariable index = new Constant(TypeInfo.TYPE_INT, Integer.toString(cpindex));
		    JavaVariable value = stack.Pop(/*no info*/);
		    JavaVariable addr = stack.Pop(TypeInfo.TYPE_REFERENCE);

                    ir.addOperand(addr);
                    ir.addOperand(value);
                    ir.addOperand(index);
		}
		break;
	    case OpcodeConst.opc_aputfield_quick:
		{
		    int cpindex = getCPIndex(bytecode);
                    JavaVariable index = new Constant(TypeInfo.TYPE_INT, Integer.toString(cpindex));
		    JavaVariable value = stack.Pop(TypeInfo.TYPE_REFERENCE);
		    JavaVariable addr = stack.Pop(TypeInfo.TYPE_REFERENCE);

                    ir.addOperand(addr);
                    ir.addOperand(value);
                    ir.addOperand(index);
		}
		break;
	    case OpcodeConst.opc_putfield_quick_w:
		{
		    int cpindex = getCPIndex(bytecode);
                    JavaVariable index = new Constant(TypeInfo.TYPE_INT, Integer.toString(cpindex));
		    JavaVariable value = stack.Pop(/*no info*/);
		    JavaVariable addr = stack.Pop(TypeInfo.TYPE_REFERENCE);
                    ir.addOperand(addr);
                    ir.addOperand(value);
                    ir.addOperand(index);
		}
		break;

	    case OpcodeConst.opc_new_quick:
	    case OpcodeConst.opc_new_checkinit_quick:
		{
		    int cpindex = getCPIndex(bytecode);
                    JavaVariable index = new Constant(TypeInfo.TYPE_INT, Integer.toString(cpindex));
                    JavaVariable target = stack.Push(TypeInfo.TYPE_REFERENCE);
                    ir.addTarget(target);
                    ir.addOperand(index);
		    ClassConstant cc = (ClassConstant)constants[cpindex];
		    ir.addDebugInfoString("class : " + cc.name + " ");
                    // Adds cb for class init 
                    if (shortOpcode == OpcodeConst.opc_new_checkinit_quick) {
                        String ciNativeName = (((CVMClass)((ClassConstant)constants[cpindex]).find().vmClass).getNativeName())+"_Classblock";
                        ir.addClassBlockToInit(ciNativeName);
                    }
		}
		break;

	    case OpcodeConst.opc_anewarray_quick:
		{
		    int cpindex = getCPIndex(bytecode);
                    JavaVariable index = new Constant(TypeInfo.TYPE_INT, Integer.toString(cpindex));
		    JavaVariable count = stack.Pop(TypeInfo.TYPE_INT);
                    JavaVariable target = stack.Push(TypeInfo.TYPE_REFERENCE);
                    ir.addTarget(target);
                    ir.addOperand(index);
                    ir.addOperand(count);
		}
		break;
	    case OpcodeConst.opc_multianewarray_quick:
		{
		    int cpIndex = getCPIndex(bytecode);
		    int dimension = bytecode.getOperand(1);
                    JavaVariable operand1 = new Constant(TypeInfo.TYPE_INT, Integer.toString(cpIndex)); // index on CP
                    JavaVariable operand2 = new Constant(TypeInfo.TYPE_INT, Integer.toString(dimension)); // dimenstion
                    // Get the length of each dimension
	            JavaVariable count[] = new JavaVariable [dimension];
                    for(int i=dimension-1; i>=0; i--) {
	                count[i] = stack.Pop(TypeInfo.TYPE_INT);
	            }
	            JavaVariable ret = stack.Push(TypeInfo.TYPE_REFERENCE); // arrayreference
                    ir.addTarget(ret);
                    ir.addOperand(operand1);
                    ir.addOperand(operand2);
                    for(int i = 0 ; i < dimension; i++) {
                        ir.addOperand(count[i]);
                    }
		}
		break;

	    default:
		new aotc.share.Assert(false, "What is it?? : " + bytecode.getString());
	}
        
	// for return type
	// this is used to determine whether make exit label and what type of return variable
        /*int ret_kind = -1;
	switch(shortOpcode)
	{
	    case OpcodeConst.opc_ireturn:
	    case OpcodeConst.opc_lreturn:
	    case OpcodeConst.opc_freturn:
	    case OpcodeConst.opc_dreturn:
	    case OpcodeConst.opc_areturn:
	    case OpcodeConst.opc_return:
		ret_kind = MethodDeclarationInfo.BYTECODE_RETURN;
		break;
	    case OpcodeConst.opc_iaload:
	    case OpcodeConst.opc_laload:
	    case OpcodeConst.opc_faload:
	    case OpcodeConst.opc_daload:
	    case OpcodeConst.opc_aaload:
	    case OpcodeConst.opc_baload:
	    case OpcodeConst.opc_caload:
	    case OpcodeConst.opc_saload:

	    case OpcodeConst.opc_iastore:
	    case OpcodeConst.opc_lastore:
	    case OpcodeConst.opc_fastore:
	    case OpcodeConst.opc_dastore:
	    case OpcodeConst.opc_aastore:
	    case OpcodeConst.opc_bastore:
	    case OpcodeConst.opc_castore:
	    case OpcodeConst.opc_sastore:

	    case OpcodeConst.opc_idiv: case OpcodeConst.opc_ldiv:
	    case OpcodeConst.opc_irem: case OpcodeConst.opc_lrem:

	    case OpcodeConst.opc_newarray: 
	    case OpcodeConst.opc_arraylength:
	    case OpcodeConst.opc_athrow:
	    case OpcodeConst.opc_monitorenter: //194
	    case OpcodeConst.opc_monitorexit: // 195

	    case OpcodeConst.opc_invokestatic_checkinit_quick:
	    case OpcodeConst.opc_invokestatic_quick:
	    case OpcodeConst.opc_invokevirtual_quick_w:
	    case OpcodeConst.opc_invokenonvirtual_quick:
	    case OpcodeConst.opc_invokevirtual_quick:
	    case OpcodeConst.opc_ainvokevirtual_quick:
	    case OpcodeConst.opc_dinvokevirtual_quick:
	    case OpcodeConst.opc_vinvokevirtual_quick:
	    case OpcodeConst.opc_invokevirtualobject_quick:
	    case OpcodeConst.opc_invokeinterface_quick:

	    case OpcodeConst.opc_checkcast_quick:

	    case OpcodeConst.opc_getfield_quick:
	    case OpcodeConst.opc_getfield2_quick:
	    case OpcodeConst.opc_agetfield_quick:
	    case OpcodeConst.opc_getfield_quick_w:
	    case OpcodeConst.opc_putfield_quick:
	    case OpcodeConst.opc_putfield2_quick:
	    case OpcodeConst.opc_aputfield_quick:
	    case OpcodeConst.opc_putfield_quick_w:

	    case OpcodeConst.opc_new_quick:
	    case OpcodeConst.opc_new_checkinit_quick:
	    case OpcodeConst.opc_anewarray_quick:
	    case OpcodeConst.opc_multianewarray_quick:
		ret_kind = MethodDeclarationInfo.BYTECODE_EXCEPTION_RETURN;
		break;
	}
	ir.setReturnType(ret_kind);
        */
        // Checks whether this ir is a potential gc point.
        if (ir.hasPotentialGCPoint() 
                || ir.hasAttribute(IRAttribute.LOOP_TAIL)) {
            ir.setAttribute(IRAttribute.POTENTIAL_GC_POINT);
        }

        if (IR.isInvoke(ir)) {
            MethodConstant mc;
            ClassInfo calleeClassinfo;
            MethodInfo calleeMethodinfo;
            int index;

            if (shortOpcode == OpcodeConst.opc_invokevirtual_quick
	            || shortOpcode == OpcodeConst.opc_ainvokevirtual_quick
	            || shortOpcode == OpcodeConst.opc_dinvokevirtual_quick
	            || shortOpcode == OpcodeConst.opc_vinvokevirtual_quick
	            || shortOpcode == OpcodeConst.opc_invokevirtualobject_quick
                || shortOpcode == OpcodeConst.opc_invokevirtual_quick_w) { // i added (09-03-11)
	        mc = (MethodConstant) this.cfg.getMethodInfo().typeInfo.get(Integer.toString(ir.getBpc()));
            } else {
                index = getCPIndex(bytecode);
                mc = (MethodConstant)constants[index];
            }
            calleeMethodinfo = mc.find();
            calleeClassinfo = calleeMethodinfo.parent;
            ir.setCalleeMethodInfo(calleeMethodinfo);
            ir.setCalleeClassInfo(calleeClassinfo);
        }

	// stack copy for consistency
	IR succ;
	if(bytecode.getOpcode()==(byte)OpcodeConst.opc_tableswitch ||
	    bytecode.getOpcode()==(byte)OpcodeConst.opc_lookupswitch) {
	    for(int i=0; i<ir.getSuccNum(); i++) {
		JavaStack newStack = (JavaStack)stack.clone();
		ir.getSucc(i).setStack(newStack);
	    }
        } else {
	    succ = ir.getBranchTarget();
	    if(succ!=null && succ.hasAttribute(IRAttribute.BB_START)) {
	        JavaStack newStack = (JavaStack)stack.clone();
		succ.setStack(newStack);
	    }
	    succ = ir.getFallthruSucc();
	    if(succ!=null && succ.hasAttribute(IRAttribute.BB_START)) {
	        succ.setStack(stack);
            }
	    if(bytecode.getOpcode()==(byte)OpcodeConst.opc_jsr) {
		stack.Pop();
		succ = cfg.getIRAtBpc(bytecode.getBpc()+3);
		succ.setStack(stack);
	    } else if(bytecode.getOpcode()==(byte)OpcodeConst.opc_jsr_w) {
		stack.Pop();
		succ = cfg.getIRAtBpc(bytecode.getBpc()+5);
		succ.setStack(stack);
	    }
	}
        //this.stack = stack;
        //return ir;
    }
    /**
     * Make copy ir that copy 'from' variable to 'to' variable
     *
     * @param from operand of copy ir
     * @param to target of copy ir
     * @return copy ir
     */
    private IR makeCopyIR(IR ir, JavaVariable from, JavaVariable to) {
        Bytecode newBytecode;
        IR newIR;
        byte opcode;
        
        switch (from.getType()) {
            case TypeInfo.TYPE_INT:
                opcode = (byte) OpcodeConst.opc_iload;
                break;
            case TypeInfo.TYPE_FLOAT:
                opcode = (byte) OpcodeConst.opc_fload;
                break;
            case TypeInfo.TYPE_LONG:
                opcode = (byte) OpcodeConst.opc_lload;
                break;
            case TypeInfo.TYPE_DOUBLE:
                opcode = (byte) OpcodeConst.opc_dload;
                break;
            case TypeInfo.TYPE_REFERENCE:
                opcode = (byte) OpcodeConst.opc_aload;
                break;
            default:
                opcode = 0;
                new aotc.share.Assert (false, "Can't determine the type of operand");
                break;
        }
        newBytecode = new Bytecode(cfg.allocateNewBpc(), opcode, new int[0]); 
        newIR = new IR(newBytecode);
	//newIR.setOriginalBpc(newIR.getOriginalBpc());
	newIR.setOriginalBpc(ir.getOriginalBpc());
        newIR.addOperand(from);
        newIR.addTarget(to);
	for(int i = 0 ; i < ir.getLoopHeaderNum() ; i++) {
	    newIR.setLoopHeader(ir.getLoopHeader(i));
	}
        return newIR;
    }
    /**
     * Adds IRList that have same function of ir to replace list
     *
     * @param ir ir that has single bytecode information
     * @param IRList this has same function of ir but this is consist of some irs.
     */
    private void addToReplaceList(IR ir, Vector IRList) {
        Pair pair = new Pair(ir, IRList);
        replaceList.addElement(pair);
        
    }
    /**
     * Gets replace list that has pairs of ir and IRList
     *
     * @return replace list
     */ 
    public Vector getReplaceList() {
        return replaceList;
    }
    /**
     * Gets ths index of operand 0 in constant pool
     *
     * @param bytecode bytecode that has the operand 0 which value is in bytecode
     * @return constant pool index of operand 0 in bytecode
     */
    public static int getCPIndex(Bytecode bytecode)
    {
	return bytecode.getOperand(0);
    }
    /**
     * Gets the type of value in constant pool
     *
     * @param classinfo class information
     * @param index the index of item in constant pool
     * @return type of the constant pool item
     */
    public static int getCPType(ClassInfo classInfo, int index)
    {
    ConstantObject cp[] = classInfo.getConstantPool().getConstants();
	return cp[index].tag;	
    }
}
