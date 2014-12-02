package aotc.ir;

import java.util.Vector;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.BitSet;
import opcodeconsts.*;
import aotc.translation.*;
import aotc.cfg.*;
import aotc.share.*;
import aotc.optimizer.*;
import aotc.*;
import util.*;
import components.*;

/**
 * IR (Intermediate Representation) is one of core classes in our AOTC system.
 * IR is similar to bytecode but has more information such as operand, target,
 * predecessor, successor, use, def and {@link IRAttributes}.
 *
 * @see IRAttribute
 */

public class IR implements Comparable {
        private Bytecode bytecode;
        private int bpc;
	private int originalBpc;

	private Vector looppeeling = new Vector();

	private int loopSize = 0;
	
        private int returnType;
        //private int loopHeader; // bpc of loop header
	private Vector loopHeader = new Vector();
        private IR loopPreheader;
	private IRAttribute attr = new IRAttribute();
	private JavaStack stack = null;

	private IndexBound bound = null;
	private JavaVariable upperBound = null;
	private JavaVariable lowBound = null;
	private int upperOffset = 0;
	private int lowOffset = 0;

        // branch target
        //private Vector branchTargets = new Vector();

        // targets, operands and operator
        private Vector targets = new Vector();
        private Vector operands = new Vector();
        private String operator = null;
        private MethodInfo calleeMethodInfo = null;
        private ClassInfo calleeClassInfo = null;
        private String classBlockToInit = null;
        private String debugInfoString = new String();

        // preload expression for get field/method/class block
        private Vector preloadExpression = new Vector();

        // Predecssors and Successors
	private Vector preds = new Vector();
	private Vector succs = new Vector();
        
	private Vector implicitPreds = new Vector();
	private Vector implicitSuccs = new Vector();
        
        private Vector Defs[]; // Vector per each varialbes Used here
        private Vector Uses[]; // Vector per each variable Defined here 

        private TreeSet liveVariable; 
	private BitSet invariantCheckSet = null;

        /**
         * Constructor of IR class
         * @param   bytecode    specifying the base bytecode
         */
	public IR(Bytecode bytecode) {
           this.bytecode = bytecode;
           //this.loopHeader = -1;
           this.bpc = bytecode.getBpc();
           this.setAttribute(IRAttribute.NEED_ICELL);
	   this.originalBpc = bpc;
        }
	public IR(IR ir) {
	    bytecode = new Bytecode(ir.bytecode);
	    bpc = ir.bpc;
	    originalBpc = ir.originalBpc;
	    returnType = ir.returnType;
	    loopHeader = new Vector(ir.loopHeader);
	    if(ir.loopPreheader != null) {
		loopPreheader = new IR(ir.loopPreheader);
	    }
	    if(ir.stack != null) {
		stack = (JavaStack)ir.stack.clone();
	    }
	    if(ir.bound != null) {
		bound = new IndexBound(ir.bound);
	    }
	    targets = new Vector(ir.targets);
	    operands = new Vector(ir.operands);
	    operator = ir.operator;
	    calleeMethodInfo = ir.calleeMethodInfo;
	    calleeClassInfo = ir.calleeClassInfo;
	    classBlockToInit = ir.classBlockToInit;
	    debugInfoString = new String(ir.debugInfoString);
	    preloadExpression = new Vector(ir.preloadExpression);
	    preds = new Vector(ir.preds);
	    succs = new Vector(ir.succs);
	    implicitPreds = new Vector(ir.implicitPreds);
	    implicitSuccs = new Vector(ir.implicitSuccs);
	    if(ir.Defs != null) {
		Defs = new Vector[ir.Defs.length];
		for(int i = 0 ; i < ir.Defs.length ; i++) {
		    Defs[i] = new Vector(ir.Defs[i]);
		}
	    }
	    if(ir.Uses != null) {
		Uses = new Vector[ir.Uses.length];
		for(int i = 0 ; i < ir.Uses.length ; i++) {
		    Uses[i] = new Vector(ir.Uses[i]);
		}
	    }
	    if(liveVariable != null) {
		liveVariable = new TreeSet(ir.liveVariable);
	    }
	    if(invariantCheckSet != null) {
		invariantCheckSet = (BitSet)ir.invariantCheckSet.clone();
	    }
	    attr.attribute = ir.attr.attribute;
	    attr.setAttribute(0, ir.attr.getAttribute(0));
	    attr.setAttribute(1, ir.attr.getAttribute(1));
	    looppeeling = new Vector(ir.looppeeling);
	}
	public void setOriginalBpc(int bpc) {
	    this.originalBpc = bpc;
	}
	public int getOriginalBpc() {
	    return originalBpc;
	}
        /**
         * Gets the bpc of this IR(bytecode).
         * @return  bpc of this IR
         */
        public int getBpc() {
            return this.bpc;
        }
	public void setBpc(int bpc) {
	    this.bpc = bpc;
	    this.bytecode.setBpc(bpc);
	}
	
        /**
         * Gets the opcode of this IR(bytecode).
         * @return  opcode of this IR
         */
        public short getShortOpcode() {
            return CFG.makeShortVal((byte) 0, getBytecode().getOpcode()); 
        }

        public int compareTo(Object o) {
            Bytecode b1 = this.getBytecode();
            Bytecode b2 = ((IR) o).getBytecode();
            return b1.compareTo(b2);

        }
        
        /**
         * Adds preload expression to this IR.
         * Preload expression is added at Preloader and used at CCodeGenerator to
         * load loop invariant method/field/class block at the loop header.
         *
         * @param pre load expression to be preloaded
         * @see Preloader
         * @see LoadExpression
         * @see CCodeGenerator
         */
        public void addPreloadExpression(LoadExpression newExpression) {
            boolean needRestore = newExpression.canInvokeGC();
            if (needRestore) {
                this.setAttribute(IRAttribute.POTENTIAL_GC_POINT);    
            }
            Iterator it = preloadExpression.iterator();
            LoadExpression oldExpression;
            String newCB, oldCB;
            while (it.hasNext()) {
                oldExpression = (LoadExpression) it.next();
                newCB = newExpression.getCB();
                oldCB = oldExpression.getCB();
                if (newCB != null && oldCB != null && oldCB.equals(newCB)) { // preloadExpressions already contains newExpression
                    return;
                }
            } 
            preloadExpression.add(newExpression);
        }

        /**
         * Returns the Vector storing all preload expressions.
         * When there is no preloaded expression, the size of 
         * returned Vector would be 0. 
         * @return Vector storing all preload expression of this IR.
         *
         * @param pre load expression to be preloaded
         * @see Preloader
         * @see LoadExpression
         * @see CCodeGenerator
         */ 
        public Vector getPreloadExpressions() {
            return preloadExpression;
        }
        
        /**
         * Returns the bytecode which this IR is based on.
         * 
         * @return the bytecode which this IR is based on.
         */
        public Bytecode getBytecode() {
            return bytecode;
        }

	public void setBytecode(Bytecode bytecode) {
	    this.bytecode = bytecode;
	}
        // Methods related to "OPERAND STACK"

        /**
         * Returns the current stack of this IR.
         * This is used only for stack emulation at IRGenerator.
         * And never be used after stack emulation is done.
         *
         * @return current stack
         */
	public JavaStack getStack() {
		return stack;
	}

        /**
         * Sets the current stack of this IR.
         * This is used only for stack emulation at IRGenerator.
         * And never be used after stack emulation is done.
         *
         * @param stack current stack
         */
	public void setStack(JavaStack stack) {
		this.stack = stack;
	}

        // Methods related to "LOOP"

        /**
         * Gets the loop header of this IR.
         * 
         * @return  the bpc of loop header, if this IR isn't a member of loop, return value is -1.
         */
        public int getLoopHeader() {
	    /*
            return loopHeader;
	    */
	    if(loopHeader.size() == 0) return -1;
	    return ((Integer)loopHeader.firstElement()).intValue();
        }
	public int getLoopHeader(int i) {
	    if(i >= loopHeader.size()) {
		return -1;
	    }
	    return ((Integer)loopHeader.elementAt(i)).intValue();
	}
        /**
         * Sets the loop header of this IR.
         * It can be called only once. 
         * (Once called, the other call would be ignored.)
         *
         * @param   loopHeader  the bpc of loopheader
         */
        public void setLoopHeader(int loopHeader) {
	    if(!isInLoop(loopHeader)) {
		this.loopHeader.add(new Integer(loopHeader));
	    }

        }
	public int getLoopHeaderNum() {
	    return loopHeader.size();
	}
	public boolean isInLoop(int loopHeader) {
	    return this.loopHeader.contains(new Integer(loopHeader));
	}
	public boolean removeLoopHeader(int loopHeader) {
	    return this.loopHeader.remove(new Integer(loopHeader));
	}

	public void setLoopSize(int size) {
            new aotc.share.Assert (this.hasAttribute(IRAttribute.LOOP_HEADER), "You can set loopsize only to loop-header");
	    loopSize = size;
	}
	public int getLoopSize() {
            new aotc.share.Assert (this.hasAttribute(IRAttribute.LOOP_HEADER), "You can get loopsize only to loop-header");
	    return loopSize;
	}


        /**
         * Sets the loop preheader of this IR.
         * It should be called only once. 
         * (Once called, the other calls would be ignored.)
         *
         * @param   loopHeader  the bpc of loopheader
         */
        public void setLoopPreheader(IR preheader) {
            new aotc.share.Assert (this.hasAttribute(IRAttribute.LOOP_HEADER), "You can add preheader only to loop-header");
            this.loopPreheader = preheader;
        }
        /**
         * Gets the loop preheader of this IR.
         * It should be called only once. 
         * (Once called, the other calls would be ignored.)
         *
         * @return the preheader of loop.
         *         if this IR is not a member of loop, returns null.
         */
        public IR getLoopPreheader() {
            return this.loopPreheader;
        }
       
        // Methods related to "USE-DEF & DEF-USE CHAIN"

        /**
         * Allocates memory for storing UD-DU chain.
         * This is called at {@link DataFlowSolver} before storing UD-DU chain.
         *
         * @see DataFlowSolver
         */
        public void allocateUDAndDUChains() {
            Defs = new Vector[getNumOfOperands()];
            Uses = new Vector[getNumOfTargets()];
            int i;
            for (i = 0; i < getNumOfOperands(); i++) {
                Defs[i] = new Vector();
            }
            for (i = 0; i < getNumOfTargets(); i++) {
                Uses[i] = new Vector();
            }
        }
        
        /**
         * Adds a use of target variable.
         * Adds the bpc of use code of this IR's target.
         * Note that there can be several targets and operands for each IR.
         * This is called at {@link DataFlowSolver}.
         *
         * @param   whichTarget represent the specific target of this IR by index
         * @param   useBpc      bpc of code using the target
         */
        public void addUseOfTarget(int whichTarget, int useBpc) {
            Uses[whichTarget].add(new Integer(useBpc));
        }

        /**
         * Adds a def of operand variable.
         * Adds the bpc of def code of this IR's operand.
         * Note that there can be more than one operands for each IR.
         * This is called at {@link DataFlowSolver}.
         * 
         * @param   whichOperand    represent the specific target of this IR by index
         * @param   defBpc          bpc of code defining the operand 
         */
        public void addDefOfOperand(int whichOperand, int defBpc) {
            Defs[whichOperand].add(new Integer(defBpc));
        }
	public void changeDefOfOperand(int whichOperand, int from, int to) {
	    Defs[whichOperand].remove(new Integer(from));
	    addDefOfOperand(whichOperand, to);
	}
	public void changeUseOfTarget(int whichTarget, int from, int to) {
	    Uses[whichTarget].remove(new Integer(from));
	    addUseOfTarget(whichTarget, to);
	}
	    

        /**
         * Gets the number of uses codes of specific target.
         *
         * @param   whichTarget specifies the target by index.
         * @return  the number use codes of specific target
         */
        public int getNumOfUses(int whichTarget) {
            return Uses[whichTarget].size();
        }

        /**
         * Gets the number of def codes of specific operand.
         *
         * @param   whichOperand    specifies the operand by index.
         * @return  the number of def codes of specific target
         */
        public int getNumOfDefs(int whichOperand) {
            return Defs[whichOperand].size();
        }

        /**
         * Gets the number of def codes of specific operand variable.
         * 
         * @param   operand specified the operand variable
         * @return  the number of def codes of specific operand
         */
        public int getNumOfDefs(JavaVariable operand) {
            Iterator it = operands.iterator();
            JavaVariable match;
            int i = 0;
            while(it.hasNext()) {
                match = (JavaVariable) it.next();
                if (operand.compareTo(match) == 0) {
                    return getNumOfDefs(i);
                } else {
                    i++;
                }
            }
            new aotc.share.Assert(false, "THERE ISN'T SUCH A OPERAND");
            return -1;
        }

        /**
         * Gets the bpc of use code of specific target variable.
         * Because there can be more than one use code, 
         * count is used as index.
         * 
         * @param   whichTarget specifies the target variable by index
         * @param   count       the index of use
         * @return  the bpc of use codes of the target variable
         */
        public int getUseOfTarget(int whichTarget, int count) {
            new aotc.share.Assert (whichTarget < getNumOfTargets() &&
                            count < getNumOfUses(whichTarget), "ERROR at getUseOfTarget -_-");
            Integer use = (Integer) Uses[whichTarget].elementAt(count);
            return (use.intValue());

        }
        /**
         * Gets the bpc of def code of specific operand variable.
         * Because there can be more than one def code, 
         * count is used as index.
         * 
         * @param   whichOperand specifies the target variable by index
         * @param   count       the index of specific use
         * @return  the bpc of def codes of specific target variable
         */
        public int getDefOfOperand(int whichOperand, int count) {
            new aotc.share.Assert (whichOperand < getNumOfOperands() &&
                            count < getNumOfDefs(whichOperand), "ERROR at getUseOfTarget -_-");
            Integer def = (Integer) Defs[whichOperand].elementAt(count);
            return (def.intValue());

        }

        /**
         * Gets the bpc of def code of specific operand variable.
         * Because there can be more than one def code, 
         * count is used as index.
         * 
         * @param   operand operand variable
         * @param   count   the index of def
         * @return  the bpc of def codes of specific target variable
         */
        public int getDefOfOperand(JavaVariable operand, int count) {
            Iterator it = operands.iterator();
            JavaVariable match;
            int i = 0;
            while(it.hasNext()) {
                match = (JavaVariable) it.next();
                if (operand.compareTo(match) == 0) {
                    return getDefOfOperand(i, count);
                } else {
                    i++;
                }
            }
            new aotc.share.Assert(false, "THERE ISN'T SUCH A OPERAND");
            return -1;
        }

        /**
         * Gets the number of targets of this IR.
         * @return the number of targets of this IR
         */
        public int getNumOfTargets() {
            return targets.size();
        }

        /**
         * Gets the number of operands of this IR.
         * @return the number of operands of this IR
         */
        public int getNumOfOperands() {
            return operands.size();
        }

        /**
         * Gets a target variable of this IR.
         *
         * @param i the index of target variable
         * @return the target variable at the index
         */
        public JavaVariable getTarget(int i) {
            new aotc.share.Assert (i < targets.size(), "There is only " + targets.size() + " targets.");
            return ((JavaVariable) targets.elementAt(i));
        }
        
        /**
         * Gets a operand variable of this IR.
         *
         * @param i the index of operand variable
         * @return the operand variable at the index
         */
        public JavaVariable getOperand(int i) {
            new aotc.share.Assert (i < operands.size(), "There is only " + operands.size() + " operands.");
            return ((JavaVariable) operands.elementAt(i));
        }
        
        /**
         * Gets the list of whole targets.
         * Return the Vector storing all the target variables.
         * 
         * @return the list of whole target as Vector
         */
        public Vector getTargetList() {
            return this.targets;
        }
        
        /**
         * Gets the list of whole operands.
         * Return the Vector storing all the operand variables.
         * 
         * @return the list of whole operands as Vector
         */
        public Vector getOperandList() {
            return this.operands;
        }

        public void setLiveVariables(TreeSet liveVariable) {
            this.liveVariable = liveVariable;
        }

        public TreeSet getLiveVariables() {
            return this.liveVariable;
        }

        /**
         * Gets the list of target as string.
         *
         * @return the list of target
         */
        public String getTargetListString() {
            Iterator it = targets.iterator();
            JavaVariable variable;
            String result = new String();
            if (targets.size() > 0) {
                while(it.hasNext()) {
                    variable = (JavaVariable)it.next();
                    if(variable!=null) result += variable.toString();
                    if (it.hasNext()) result += ",";
                }
            } 
            return result;
        }
        
        /**
         * Gets the list of operand as string.
         *
         * @return the list of operand 
         */
        public String getOperandListString() {
            Iterator it = operands.iterator();
            JavaVariable variable;
            String result = new String();
            if (operands.size() > 0) {
                while(it.hasNext()) {
                   variable = (JavaVariable)it.next();
                    result += variable.toString();
                    if (it.hasNext()) result += ",";
                }
            }
            return result;
        }

        /**
         * Gets the string form of this IR.
         * This is very useful for debugging.
         *
         * @return the string form of this IR (including debugInfoString).
         */ 
        public String toString() {
            String result = new String();
            result += bytecode.getString();
            result += "\ttargets:" + getTargetListString();
            result += " operands:" + getOperandListString();    
            result += debugInfoString;
            if (this.hasAttribute(IRAttribute.POTENTIAL_GC_POINT)) result += "\nPOTENTIAL_GC_POINT";
            if (this.hasAttribute(IRAttribute.NEED_ICELL)) result += "\nNEED_ICELL";
            return result;
        }

        /**
         * Sets calle MethodInfo for method invokes.
         */
        public void setCalleeMethodInfo(MethodInfo mi) {
            this.calleeMethodInfo = mi;
        }
        
        /**
         * Sets calle ClassInfo for method invokes.
         */
        public void setCalleeClassInfo(ClassInfo ci) {
            this.calleeClassInfo = ci;
        }

        /**
         * Returns the MethodInfo of callee method
         */ 
        public MethodInfo getCalleeMethodInfo() {
            new aotc.share.Assert(this.calleeMethodInfo != null, "There is no MethodInfo of callee.");
            return this.calleeMethodInfo;
        }
        
        /**
         * Returns the ClassInfo of callee method
         */ 
        public ClassInfo getCalleeClassInfo() {
            new aotc.share.Assert(this.calleeClassInfo != null, "There is no ClassInfo of callee.");
            return this.calleeClassInfo;
        }
        
        /**
         * Adds a branch target of this IR.
         * This is called at {@link IRGenerator} 
         *
         * @param target branch target
         */
        //public void addBranchTarget(IR target) {
        //    branchTargets.add(target);
        //}
        
        /**
         * Gets the number of branch target.
         *
         * The number of branch target is as followings
         *
         * 0 for non-branch opcode
         * 1 for goto, if series
         * n for lookupswitch, tableswitch
         *
         * @return the number of branch target
         */
        //public int getNumOfBranchTarget() {
        //    return branchTargets.size();
        //}

        /**
         * Gets i'th branch target.
         *
         * @param i the index of branch target
         * @return the i'th branch target
         */
        //public int getBranchTarget(int i) {
        //    int targetBpc = ((Integer)branchTargets.elementAt(i)).intValue();
        //    return targetBpc;
        //}

        /**
         * Adds a target variable of this IR.
         *
         * @param target target variable to add
         * @return the i'th branch target
         */
        public void addTarget(JavaVariable target) {
           targets.add(target); 
        }

        /**
         * Changes a target variable of this IR into another one.
         *
         * @param which index of old target variable.
         * @param target new target variable
         */
        public void changeTarget(int which, JavaVariable target) {
            //targets.remove(which);
            //targets.insertElementAt(target, which);
	    targets.setElementAt(target, which);
        }

        /**
         * Changes a operand variable of this IR into another one.
         *
         * @param which index of old operand variable.
         * @param target new operand variable
         */
        public void changeOperand(int which, JavaVariable operand) {
            //operands.remove(which);
            //operands.insertElementAt(operand, which);
            JavaVariable was = (JavaVariable) operands.elementAt(which);
	    operands.setElementAt(operand, which);
	    if(upperBound != null && upperBound.toString().equals(was.toString())) {
		upperBound = operand;
	    }
	    if(lowBound != null && lowBound.toString().equals(was.toString())) {
		lowBound = operand;
	    }
            this.addDebugInfoString("Operand changed from: " + was + " to " + operand);
        }
        
        /**
         * Changes a operand variable of this IR into another one.
         *
         * @param from old operand variable.
         * @param to new operand variable
         */
        public void changeOperand(JavaVariable from, JavaVariable to) {
            int index;
            new aotc.share.Assert(isOperand(from), from + " isn't a operand of " + this);
            // index = operands.indexOf(from); This doesn't work. Because in Vector's world all comparison are 
            //                                 based on pointer not Comparable interface.
            index = getOperandIndex(from);
            operands.removeElementAt(index);
            operands.insertElementAt(to, index);
	    if(upperBound != null && upperBound.toString().equals(from.toString())) {
		upperBound = to;
	    }
	    if(lowBound != null && lowBound.toString().equals(from.toString())) {
		lowBound = to;
	    }
            this.addDebugInfoString("Operand changed from: " + from + " to " + to);
            new aotc.share.Assert(isOperand(from) == false, "There is more than one operand named " + from + " at " + this);
        }

        
        /**
         * Adds a operand variable to this IR.
         *
         * @param to new operand variable
         */
        public void addOperand(JavaVariable operand) {
            operands.add(operand);
        }

        /**
         * Gets the index of a operand.
         *
         * @param op1 operand variable
         * @return the index of the give operand variable
         */
        public int getOperandIndex(JavaVariable op1) {
            int i;
            JavaVariable op2;
            for (i = 0; i < operands.size(); i++) {
                op2 = (JavaVariable) operands.elementAt(i);
                if (op1.compareTo(op2) == 0) {
                    return i;
                }
            }
            new aotc.share.Assert(false, op1 + " is not a operand of " + this);
            return -1;
        }
        
        /**
         * Checks whether the given variable is operand of this IR.
         *
         * @param op java variable
         * @return true if the give java variable is a operand of this IR.
         */
        private boolean isOperand(JavaVariable op) {
            int i;
            JavaVariable operand;
            for (i = 0; i < operands.size(); i++) {
                operand = (JavaVariable) operands.elementAt(i);
                if (operand.compareTo(op) == 0) {
                    return true;
                }
            }
            return false;
        }
        
        /**
         * Sets arthmetic operator of this IR.
         * Arithmetc operator means +, -, *, /, ...
         * 
         * @param op operator
         */
        public void setOperator(String op) {
            this.operator = op;
        }
        
        /**
         * Gets arthmetic operator of this IR.
         *
         * @return arithmetic operator of this IR
         */
        public String getOperator() {
            return operator;
        }

        /**
         * Sets the return type of this IR.
         *
         * The possible return types are 
         * MethodDeclarationInfo.BYTECODE_RETURN and MethodDeclarationInfo.BYTECODE_EXCEPTION_RETURN
         *
         * @param int return type
         */
        //public void setReturnType(int retType) {
        //    this.returnType = retType;
        //}

        /**
         * Gets the return type of this IR.
         *
         * @return the return type of this IR.
         */
	//public int getReturnType() {
	//    return returnType;
	//}

        /**
         * Adds debug information to this IR.
         * The information will be printed at the generated C code as comments
         * and VCG file.
         * 
         * @param s debug string to be added
         */
        public void addDebugInfoString(String s) {
            this.debugInfoString += "\n" + s;
        }

        /**
         * Gets debug information of this IR.
         * Returns the debug information string.
         * 
         * @return the debug information string.
         */
        public String getDebugInfoString() {
            if (getLoopHeader() != -1) debugInfoString += "\nLoopHeader:" + getLoopHeader();
            if (hasAttribute(IRAttribute.LOOP_HEADER)) debugInfoString += "\nLOOP_HEADER";
            if (hasAttribute(IRAttribute.LOOP_TAIL)) debugInfoString += "\nLOOP_TAIL";
            if (hasAttribute(IRAttribute.LOOP_BODY)) debugInfoString += "\nLOOP_BODY";
            return debugInfoString;
        }
	

        // Methods related to "ATTRIBUTE" 

        /**
         * Checks whether this IR has a specific IR attribute.
         * 
         * @param attr attribute to be tested
         * @return true if this IR has the attribute
         * @see IRAttribute
         */
        public boolean hasAttribute(int attr) {
	    if(attr==IRAttribute.BB_START && preds.size()==0 && getBpc()!=0)
    		return true;
	    return ((this.attr.attribute & attr)!=0);
	}

        /**
         * Sets IR attribute to this IR.
         * @param attr attribute to be set
         * @see IRAttribute
         */
	public void setAttribute(int attr) {
    	    this.attr.attribute |= attr;
	}
        
        /**
         * Removes IR attribute from this IR.
         * @param attr attribute to be removed
         * @see IRAttribute
         */
	public void removeAttribute(int attr) {
	    this.attr.attribute &= ~attr;
	}

        /**
         * Gets the value of specific IR attribute for this IR.
         * @param flag IR attribute which the value belongs to
         * @return the value of specific IR attribute
         * @see IRAttribute
         */
	public int getAttribute(int flag) {
	    return this.attr.getAttribute(flag);
	}

        /**
         * Sets the value of specific IR attribute for this IR.
         * @param flag IR attribute which the value belongs to
         * @param value attribute value to be stored as flag
         * @see IRAttribute
         */
	public void setAttribute(int flag, int value) {
	    this.attr.setAttribute(flag, value);
	}

        // Methods related to "CFG(Pred. and Succ.)"

        /**
         * Adds new predecessor to this IR.
         *
         * @param node predecessor IR
         */
	public void addPred(IR node) {
	    if(preds.contains(node)) {
		new aotc.share.Assert(false, node + " is alreay in preds of " + this);
	    }
	    preds.add(node);
	}
	public void addImplicitPred(IR node) {
            implicitPreds.add(node);
	}
	public void removePred(IR node) {
	    preds.remove(node);
	}

        /**
         * Gets predecessor of this IR.
         *
         * @param i predecessor index
         * @return i'th predecessor
         */
	public IR getPred(int i) {
            return (IR)preds.elementAt(i);
	}
	public IR getImplicitPred(int i) {
            return (IR)implicitPreds.elementAt(i);
	}
        
        /**
         * Gets the number of predecessors.
         *
         * @return the number of predecessors.
         */
	public int getPredNum() {
            return preds.size();
	}
	public int getImplicitPredNum() {
            return implicitPreds.size();
	}

        /**
         * Checks whether a IR is a predecessor of this IR.
         * 
         * @param node IR to be tested
         * @return true is node is a predecessor of this IR
         */
	public boolean isPred(IR node) {
            return preds.contains(node);
	}
		
        /**
         * Adds new successor to this IR.
         *
         * @param node new successor IR
         */
	public void addSucc(IR node) {
	    if(succs.contains(node)) {
		new aotc.share.Assert(false, node + " is alreay in succs of " + this);
	    }
            succs.add(node);
	}
	public void addImplicitSucc(IR node) {
            implicitSuccs.add(node);
	}
	public void removeSucc(IR node) {
	    succs.remove(node);
	}
       
        /**
         * Replaces a successor.
         * 
         * @param from successor IR to be replaced
         * @param to successor IR to be newly adopted
         */
        public void changeSucc(IR from, IR to) {
            int index;
            new aotc.share.Assert(succs.contains(from), from + " is not a successor of " + to);
            index = succs.indexOf(from);
            succs.remove(from);
	    if(succs.contains(to)) {
		new aotc.share.Assert(false, to + " is already in succs of " + this + ", can't change");
	    }
	    succs.insertElementAt(to, index);
       }

        /**
         * Replaces a predecessor.
         * 
         * @param from predecessor IR to be replaced
         * @param to predecessor IR to be newly adopted
         */
        public void changePred(IR from, IR to) {
            int index;
            new aotc.share.Assert(preds.contains(from), from + " is not a predecessor of " + to);
            index = preds.indexOf(from);
            preds.remove(from);
	    if(preds.contains(to)) {
		new aotc.share.Assert(false, to + " is already in preds of " + this + ", can't change");
	    }
	    preds.insertElementAt(to, index);
        }
        
        /**
         * Gets a successor of this IR.
         *
         * @param i successor index
         * @return i'th successor
         */
	public IR getSucc(int i) {
            return (IR)succs.elementAt(i);		
	}
	public IR getImplicitSucc(int i) {
            return (IR)implicitSuccs.elementAt(i);		
	}

        /**
         * Gets the number of successors.
         *
         * @return the number of successors
         */
	public int getSuccNum() {
	    return succs.size();
	}
	public int getImplicitSuccNum() {
	    return implicitSuccs.size();
	}
        
        /**
         * Checks whether given IR is a successor of this IR.
         * 
         * @param node IR to be tested
         * @return true is node is a successor of this IR
         */
	public boolean isSucc(IR node) {
            return succs.contains(node);
	}
        
        /**
         * Gets the fall through successor of a branch code.
         * @return the fall through of branch instruction.
         *         null, if this IR isn't a kind of branch instruction
         */
	public IR getFallthruSucc() {
            byte opcode = bytecode.getOpcode();
	    if(opcode==(byte)OpcodeConst.opc_goto ||
		   opcode==(byte)OpcodeConst.opc_goto_w ||
		   opcode==(byte)OpcodeConst.opc_lookupswitch ||
		   opcode==(byte)OpcodeConst.opc_tableswitch ||
		   opcode==(byte)OpcodeConst.opc_jsr ||
		   opcode==(byte)OpcodeConst.opc_jsr_w) {
			return null;
            }
	    if(succs.size()==1 || succs.size()==2) {
		return (IR)succs.elementAt(0);
	    }
	    return null;
	}

        /**
         * Gets the branch target of a branch instruction.
         * @return the branch target of a branch instruction.
         *         null, if this IR isn't a branch instruction
         */
	public IR getBranchTarget() {
            byte opcode = bytecode.getOpcode();
	    if(opcode==(byte)OpcodeConst.opc_goto ||
	            opcode==(byte)OpcodeConst.opc_goto_w ||
	            opcode==(byte)OpcodeConst.opc_jsr ||
	            opcode==(byte)OpcodeConst.opc_jsr_w) {
		return (IR)succs.elementAt(0);
	    }
	    //if(succs.size()==2) {
            if (this.isConditionalBranch()) {
		return (IR)succs.elementAt(1);
	    }
	    return null;
	}
       
        /**
         * Checks whether this IR is a kind of conditional branch
         *
         * @return true is this IR is a kind of conditional branch
         *         false, otherwise
         */
        public boolean isConditionalBranch() {
            byte opcode = bytecode.getOpcode();
            if (opcode == (byte)OpcodeConst.opc_ifeq 
                || opcode == (byte)OpcodeConst.opc_ifne
                || opcode == (byte)OpcodeConst.opc_iflt
                || opcode == (byte)OpcodeConst.opc_ifge
                || opcode == (byte)OpcodeConst.opc_ifgt
                || opcode == (byte)OpcodeConst.opc_ifle
                || opcode == (byte)OpcodeConst.opc_if_icmpeq
                || opcode == (byte)OpcodeConst.opc_if_icmpne
                || opcode == (byte)OpcodeConst.opc_if_icmplt
                || opcode == (byte)OpcodeConst.opc_if_icmpge
                || opcode == (byte)OpcodeConst.opc_if_icmpgt
                || opcode == (byte)OpcodeConst.opc_if_icmple
                || opcode == (byte)OpcodeConst.opc_if_acmpeq
                || opcode == (byte)OpcodeConst.opc_if_acmpne
                || opcode == (byte)OpcodeConst.opc_ifnull
                || opcode == (byte)OpcodeConst.opc_ifnonnull
                || opcode == (byte)OpcodeConst.opc_nonnull_quick) {
                return true;
            } else {
                return false;
            }
        }
        /**
         * Checks whether this IR is a kind of unconditional branch
         *
         * @return true is this IR is a kind of unconditional branch
         *         false, otherwise
         */
        public boolean isUnconditionalBranch() {
            byte opcode = bytecode.getOpcode();
            if (opcode == (byte)OpcodeConst.opc_goto 
                || opcode == (byte)OpcodeConst.opc_goto_w) {
                return true;
            } else {
                return false;
            }
        }

        public static boolean isInvoke(IR ir) {
            short shortOpcode = ir.getShortOpcode();
            if (shortOpcode == OpcodeConst.opc_invokestatic_checkinit_quick
	            || shortOpcode == OpcodeConst.opc_invokestatic_quick
        	    || shortOpcode == OpcodeConst.opc_invokevirtual_quick_w
        	    || shortOpcode == OpcodeConst.opc_invokenonvirtual_quick
	            || shortOpcode == OpcodeConst.opc_invokevirtual_quick
	            || shortOpcode == OpcodeConst.opc_ainvokevirtual_quick
        	    || shortOpcode == OpcodeConst.opc_dinvokevirtual_quick
	            || shortOpcode == OpcodeConst.opc_vinvokevirtual_quick
	            || shortOpcode == OpcodeConst.opc_invokevirtualobject_quick
	            || shortOpcode == OpcodeConst.opc_invokeinterface_quick) {
                return true;
            } else {
                return false;
            }
        }

        public boolean needICell() {
            if(isInvoke(this)) return true;
            short shortOpcode = this.getShortOpcode();
            if(shortOpcode==OpcodeConst.opc_monitorenter) return true;
            if(shortOpcode==OpcodeConst.opc_monitorexit) return true;
            //if(shortOpcode==OpcodeConst.opc_getstatic_checkinit_quick) return true;
            //if(shortOpcode==OpcodeConst.opc_putstatic_checkinit_quick) return true;
            //if(shortOpcode==OpcodeConst.opc_agetstatic_checkinit_quick) return true;
            //if(shortOpcode==OpcodeConst.opc_aputstatic_checkinit_quick) return true;
            //if(shortOpcode==OpcodeConst.opc_getstatic2_checkinit_quick) return true;
            //if(shortOpcode==OpcodeConst.opc_putstatic2_checkinit_quick) return true;
            return false;
        }

        public boolean hasPotentialGCPoint() {
            switch(getShortOpcode()) {
                case OpcodeConst.opc_newarray: 
                case OpcodeConst.opc_monitorenter: 
                case OpcodeConst.opc_monitorexit:
                case OpcodeConst.opc_agetstatic_checkinit_quick:
                case OpcodeConst.opc_getstatic_checkinit_quick:
                case OpcodeConst.opc_getstatic2_checkinit_quick:
                case OpcodeConst.opc_aputstatic_checkinit_quick:
                case OpcodeConst.opc_putstatic_checkinit_quick:
                case OpcodeConst.opc_putstatic2_checkinit_quick:
                case OpcodeConst.opc_new_quick:
                case OpcodeConst.opc_new_checkinit_quick:
                case OpcodeConst.opc_anewarray_quick:
                case OpcodeConst.opc_multianewarray_quick:
                    return true;
                default:
                    if (isInvoke(this)) {
                        return true;
                    } else {
                        return false;
                    }
                }
        }
        /**
         * Checks if a GC-Check point is inserted to method entry or not.
         */
        public boolean provideGCCheckPoint() {
            short shortOpcode = this.getShortOpcode();
            
            // Since we can't determine callee of virtual call, we can give answer to only static calls
            if (shortOpcode == OpcodeConst.opc_invokestatic_checkinit_quick
	            || shortOpcode == OpcodeConst.opc_invokestatic_quick
        	    || shortOpcode == OpcodeConst.opc_invokenonvirtual_quick) {
		CFGInfo info = CFGInfo.getCFGInfo(getCalleeMethodInfo());
                if(info.getPrologueGCCheckPoint()) {
                    return true;
                } else {
                    return false;
                }
            } else if(shortOpcode == OpcodeConst.opc_anewarray_quick // these instructions provides for GC-Check point
                        || shortOpcode == OpcodeConst.opc_multianewarray_quick) {
                return true;
            } else {
                return false; 
            }
        }

        public void addClassBlockToInit(String cb) {
            this.classBlockToInit = cb;
        }

        public String getClassBlockToInit() {
            return this.classBlockToInit;
        }
	
	public String getArrayLength() {
	    if(bound == null || bound.getLB(0) == null || bound.getLB(0).getType() == TypeInfo.TYPE_REFERENCE) return "ref";
	    int offset = bound.getLOffset(0);
	    String length = bound.getLB(0).toString();
	    if(offset == 0) {
		return length;
	    }else {
		return length + offset;
	    }
	}
	public int getArrayLengthOffset() {
	    if(bound == null) return 0;
	    return bound.getUOffset(0);
	}
	public JavaVariable getUpperBound() {
	    return upperBound;
	}
	public int getUpperOffset() {
	    return upperOffset;
	}
	public JavaVariable getLowBound() {
	    return lowBound;
	}
	public int getLowOffset() {
	    return lowOffset;
	}
	public void setArrayLength(IndexBound bound) {
	    this.bound = bound;
	}
	public void setUpperBound(JavaVariable bound, int offset) {
	    upperBound = bound;
	    upperOffset = offset;
	}
	public void setLowBound(JavaVariable bound, int offset) {
	    lowBound = bound;
	    lowOffset = offset;
	}
	

	public void setInvariantCheckSet(BitSet result) {
	    this.invariantCheckSet = result;
	}
	public BitSet getInvariantCheckSet() {
	    return this.invariantCheckSet;
	}
	public int getNumLoopPeeling() {
	    return looppeeling.size();
	}
	public void addLoopPeeling(int headerBpc) {
	    if(!isLoopPeeling(headerBpc)) {
		looppeeling.add(new Integer(headerBpc));
	    }
	}
	public boolean isLoopPeeling(int headerBpc) {
	    //if(AOTCWriter.PROFILE_LOOPPEELING) return true;
	    return looppeeling.contains(new Integer(headerBpc));
	}
	public void changeLoopPeeling(int from, int to) {
	    looppeeling.remove(new Integer(from));
	    addLoopPeeling(to);
	}
	public int getLoopPeeling(int i) {
	    return ((Integer)looppeeling.elementAt(i)).intValue();
	}

}
