package aotc.cfg;

import components.*;
import opcodeconsts.*;
import aotc.share.*;
import aotc.ir.*;
import java.util.*;
import util.*;
import aotc.translation.*;
import aotc.optimizer.*;
import aotc.AOTCWriter;

/**
 * CFG (Control Flow Graph) represents a whold CFG of a method.
 * 
 * This class is one of the core classes in our AOTC classes.
 * This class not only represent CFG of a method but also works as a driver
 * for initializing and collecting basic information.
 *
 * This class do following things
 * <ul>
 * <li>It starts from bytecode array(extracted from classfile).
 * <li>Then allocates IR to each bytecodes.
 * <li>Marks predecessor, successor relationship to each IR
 * <li>Does stack emulation
 * <li>Eliminates dups
 * <li>Finds loops 
 * <li>Make UD-DU chains
 * <li>TODO move driver for optimizer out of this class
 *</ul>
 */

public class CFG {
    // Storages for store IR
    private Vector IRBlock;
    private Hashtable IRCache;
    private int maxBpc;

    private TreeSet needLabelList = null;
    private TreeSet labelList = null;
    //private TreeSet changedArgs = null;
    //private TreeSet usedICells = null;
    //private TreeSet usedVariables = null;
    
    private Bytecode block[];
    private byte code[];
    private MethodInfo methodInfo;
    private ClassInfo classInfo;
    private IRGenerator irGenerator; // added by uhehe99
    private int returnType = -1;
    //private int numOfLocalICells = -1;
    private boolean useCls;
    private boolean needExitCode;
    private CFGInfo cfgInfo;

    private BitSet reachingDef[];
    private DataFlowSolver dSolver = null;

    public final static int ADD_IN_FRONT_OF = 0x1;
    public final static int ADD_AFTER = 0x2;

    /**
     * Constructs CFG class
     * @param info method info
     * @param classInfo class info
     */
    public CFG(MethodInfo info, ClassInfo classInfo) {
        this.needLabelList = null;
        this.useCls = false;
	this.methodInfo = info;
	this.classInfo = classInfo;
        this.needExitCode = false;
	code = methodInfo.code;
        //System.out.println("Class:" + classInfo.getGenericNativeName());
	//System.out.println("Method:" + info.name);
        //System.out.println("********************************Class : " + getClassInfo().className + " , Method : " + getMethodInfo().name);
		
	block = new Bytecode[code.length];
        IRBlock = new Vector();
        IRCache = new Hashtable();

        // Build basic block
	generateCFG();
	buildBB();

        // Find loops
        LoopFinder loopFinder = new LoopFinder(this);
        loopFinder.findLoops();

        // Generate IR Information
        irGenerator = new IRGenerator(classInfo, info, this);
	generateIR();

        // Replace dups by copies : THIS IS A MUST, NOT AN OPTION!!!
        eliminateDups(irGenerator.getReplaceList());

	// LoopPeeling
	boolean isLoopPeeled = false;
	//if(!AOTCWriter.PROFILE_LOOPPEELING) {
	    LoopPeeling loopPeeling = new LoopPeeling(this);
	    isLoopPeeled = loopPeeling.doLoopPeeling();
	//}
        
        // Make UD-DU chains
        dSolver = new DataFlowSolver(this);
        dSolver.makeUDAndDUChains();
        if(AOTCWriter.REMOVE_REDUNDANT_REFERENCE_COPY) {
            CopyEliminator copyEliminator = new CopyEliminator(this);
            copyEliminator.doElimination();
        }
        
        // Eliminated redundant class init check code
        // This should be done before ICell Eliminators, 
        // Since it eliminates some POTENTIAL_GC_POINTs
        if (AOTCWriter.REMOVE_REDUNDANT_CI_CHECK) {
            ClassInitEliminator classInitEliminator = new ClassInitEliminator(this);
            classInitEliminator.eliminates();
        }

        // This also should be done before ICell Eliminators, 
        // Since it eliminates some POTENTIAL_GC_POINTs
        // move class initialization checks to loop preheader 
        dSolver.makeUDAndDUChains();
        if (AOTCWriter.MOVE_CI_CHECK_TO_LOOP_PREHEADER) {
            Preloader preloader = new Preloader(this);
        }

         // Eliminated unnecessary ICell uses
        //dSolver.makeUDAndDUChains();
        //dSolver.solveLiveVariables();
        recomputeInfo();
        if(AOTCWriter.REMOVE_REDUNDANT_FRAME) {
            ICellEliminator icellEliminator = new ICellEliminator(this);
            icellEliminator.doElimination();
        }

	// Eliminate Loop Invariant Exception Check
    /*
	if(AOTCWriter.PROFILE_LOOPPEELING) {
	    LoopInvariantFinder loopInvariantFinder = new LoopInvariantFinder(this);
	    loopInvariantFinder.find();
	}
    */
	if(isLoopPeeled) {
	    LoopInvariantFinder loopInvariantFinder = new LoopInvariantFinder(this);
	    loopInvariantFinder.find();
	    loopInvariantFinder.doElimination();
	}
	
	// Eliminate unnecessary exception check
        if (AOTCWriter.REMOVE_REDUNDANT_NULL_CHECK) {
            NullCheckEliminator nullEliminator = new NullCheckEliminator(this);
	    nullEliminator.doElimination();
        }

        // Eliminate redundant array bound checks
        if (AOTCWriter.REMOVE_REDUNDANT_BOUND_CHECK) {
    	    ArrayBoundCheckEliminator arrayBoundEliminator = new ArrayBoundCheckEliminator(this);
	    arrayBoundEliminator.doElimination();
        }
        cfgInfo = new CFGInfo(this);


        // Eliminate duplicated GC checkpoint at Loop-back edge
        // GCCheckEliminator gcCheckEliminator = new GCCheckEliminator(this);
        // gcCheckEliminator.eliminate();

	//VCGWriter vcgWriter = new VCGWriter(this);
	//vcgWriter.generateVCG();
    }

    public void recomputeInfo() {
	dSolver.makeUDAndDUChains();
	dSolver.solveLiveVariables();
    }

    public CFGInfo getCFGInfo() {
	return cfgInfo;
    }

    public void setNeedExitCode(boolean value) {
        this.needExitCode = value;
    }

    public boolean getNeedExitCode() {
        return needExitCode;
    }

    public void setUseCls(boolean newValue) {
        useCls = newValue;
    }

    public boolean getUseCls() {
        return useCls;
    }

    public int getNumOfLocalICells() {
	//if(numOfLocalICells<0) {
	    int numOfLocalICells = getChangedArgs().size();
	    Iterator it = getUsedICells().iterator();
	    while(it.hasNext()) {
		JavaVariable usedICell = (JavaVariable) it.next();
		if((usedICell instanceof StackVariable) || usedICell.getIndex() >=methodInfo.argsSize) {
		    if(!(usedICell instanceof Constant)) {
			numOfLocalICells++;
		    }
		}
	    }
	//}
        return numOfLocalICells;
    }

    public void setReachingDef(BitSet[] reachingDef) {
        this.reachingDef = reachingDef;
    }
    public BitSet[] getReachingDef() {
        return this.reachingDef;
    }

    /**
     * Gets list of IR which need to generate goto label.
     * @return list of IR which need to generate goto label as a TreeSet
     */
    public TreeSet getNeedLabelList() {
        if (needLabelList == null) {
            computeNeedLabelList();
        }
       return this.needLabelList;
    }
    
    /**
     * Returns iterator of CFG.
     * The order of IR's are directly used as code generation order.
     *
     * @return iterator
     */
    public Iterator iterator() {
        return IRBlock.iterator();
    }

    /**
     * Gets the IR of specific bpc.
     * Use Hashtable instead Vector to reduce respond time.
     * @param bpc bpc
     * @return IR IR at given bpc
     */
    public IR getIRAtBpc(int bpc) {
        Integer intBpc = new Integer(bpc);
        IR result = (IR) IRCache.get(intBpc);
        new aotc.share.Assert (result != null, "CAN'T FIND INSTRUCTION AT BPC " + bpc);
        return result;
    }
    
    /**
     * Gets i'th IR.
     * Gets i'th IR at IRBlock
     * @param i index
     * @return IR IR at given index
     */
    public IR getIRAtIndex(int i) {
        new aotc.share.Assert (i >= 0 && i < IRBlock.size(), "CAN'T FIND INSTRUCTION AT INDEX " + i);
        IR result = (IR) IRBlock.elementAt(i);
        return result;
    }

    /**
     * Returns the code size of CFG.
     * This returns the maximum bpc of CFG+1. 
     * And it is different from methodInfo.code.length when new IR's are added.
     * Also, it is different from the number of instructions.
     *
     * @return codesize
     */ 
    public int getCodeSize() {
        return (maxBpc + 1);
    }
    
    /**
     * Returns the number of IR at CFG.
     * This returns the number of IR at CFG. 
     * This is different from getCodeSize
     *
     * @return total number of IR at CFG 
     */ 
    public int getNumOfIR() {
        return IRBlock.size();
    }
    
    /**
     * Returns {@link ClassInfo}.
     * @return class info
     */
    public ClassInfo getClassInfo() {
	return this.classInfo;
    }
    /**
     * Returns {@link MethodInfo}.
     * @return method info
     */
    public MethodInfo getMethodInfo() {
	return this.methodInfo;
    }

    /**
     * Allocates new bpc.
     * This is used when generate new IR.
     *
     * @return allocated bpc
     */
    public int allocateNewBpc() {
        this.maxBpc++;
        return (this.maxBpc);
    }
    /**
     * Adds new IR to CFG.
     * Notes that this doesn't adds any predecessor, successor relationship.
     * This only add new IR to Vector and Hashtable storing IRs.
     *
     * @param newIR     new IR to be added
     * @param indexIR   the indexIR which designates the position for newIR to be inserted 
     * @param type      the way to determine relative position to indexIR
     * @return 
     */
    public void addNewIR(IR newIR, IR indexIR, int type) {
        new aotc.share.Assert(IRBlock.contains(indexIR), "There is no " + indexIR);
        int index;
        if (type == ADD_IN_FRONT_OF) {
            index = IRBlock.indexOf(indexIR);
            IRCache.put(new Integer(newIR.getBpc()), newIR); // add to hastable
            IRBlock.insertElementAt(newIR, index);
            return;
        } else if (type == ADD_AFTER) {
            index = IRBlock.indexOf(indexIR);
            IRCache.put(new Integer(newIR.getBpc()), newIR); // add to hastable
            IRBlock.insertElementAt(newIR, index + 1);
            return;

        }
        new aotc.share.Assert(false, "NOT SUPPORTED ADD TYPE");
    }
    /**
     * Replace oldIR new newIR(s).
     * Notes that this not only adds to Vector and Hashtable
     * but also predecessor, successor relationship.
     * 
     * @param from IR to be replaced
     * @param toList IR(s) to be inserted instead of old one
     */
    public void replaceIR(IR from, Vector toList) {
        int size = toList.size();
        IR first, last; 
        IR a, b;
        IR succOfFrom;
        int i;
        String debugString = "Replaced by ";

        new aotc.share.Assert (size > 0, "Size <= 0 : At replace IR");
        first = (IR) toList.elementAt(0);
        last = (IR) toList.elementAt(size - 1);
        succOfFrom = from.getSucc(0);

        // 3. Make new IR to inherit old code's attribute
        for (i = 0; i < size; i++) {
            a = (IR) toList.elementAt(i);
            if (from.hasAttribute(IRAttribute.LOOP_BODY)) {
                a.setAttribute(IRAttribute.LOOP_BODY);
            }
            if (from.hasAttribute(IRAttribute.LOOP_HEADER)) {
                a.setAttribute(IRAttribute.LOOP_HEADER);
            }
            if (from.hasAttribute(IRAttribute.LOOP_TAIL)) {
                a.setAttribute(IRAttribute.LOOP_TAIL);
            }
        }
       
        // 1. Add to IR List 
        b = from;
        for (i = 0; i < size; i++) {
            a = (IR) toList.elementAt(i);
            debugString += a.getBpc() + " ";
            addNewIR (a, b, ADD_AFTER);
            b = a;
        }
        // 2. Add to CFG (Connect with predecessor and successor edge)
        for (i = 0; i < size - 1; i++) {
            a = (IR) toList.elementAt(i);
            b = (IR) toList.elementAt(i + 1);
            a.addSucc(b);
            b.addPred(a);
        }
        first.addPred(from);
        from.changeSucc(succOfFrom, first);
        
        last.addSucc(succOfFrom);
        succOfFrom.changePred(from, last);

        from.setAttribute(IRAttribute.ELIMINATED);
        from.addDebugInfoString(debugString);
    }

    /**
     * Eliminates dups.
     * Eliminates dups and inserts load instructions instead.
     * Note that to do this, IRGenerator should give information. 
     *
     * @param replaceList replace dups with a sequence of load instructions
     */
    public void eliminateDups(Vector replaceList) {
        // replace dups by copies  
        Iterator it = replaceList.iterator();
        while (it.hasNext()) {
            Pair pair = (Pair) it.next();
            IR from = (IR) pair.getA();
            Vector toList = (Vector) pair.getB();
            replaceIR(from, toList);
        }
    }
    
    /**
     * Gets return type of this method.
     * @return return type 
     */
    public int getReturnType() {
	if(returnType<0) {
	    String signature = methodInfo.type.string;
	    return TypeInfo.toAOTCType(signature.charAt(signature.indexOf(')')+1));
	} else {
	    return returnType;
	}
    }
    
    /**
     * Gets list of used local/stack/temporary variables.
     *
     * @return list of used {@link JavaVariable} as TreeSet
     */
    public TreeSet getUsedVariables(boolean isPostPass) {
	//if(usedVariables==null) {
	    TreeSet usedVariables = new TreeSet();
	    Iterator it = this.iterator();
	    IR ir;
	    while(it.hasNext()) {
		ir = (IR)it.next();
		if(ir.hasAttribute(IRAttribute.ELIMINATED)) continue;
		for(int i=0; i<ir.getNumOfTargets(); i++) {
		    JavaVariable v = ir.getTarget(i);
		    if(isPostPass && IR.isInvoke(ir) && ir.getNumOfUses(0)==0)
			break;
		    if(v instanceof StackVariable || v instanceof LocalVariable) {
			usedVariables.add(v);
		    }
		}
		for(int i=0; i<ir.getNumOfOperands(); i++) {
		    JavaVariable v = ir.getOperand(i);
		    if(v instanceof StackVariable || v instanceof LocalVariable) {
			usedVariables.add(v);
		    }
		}
	    }
	    if(MethodInliner.inline_depth==0 && (methodInfo.access & ClassFileConst.ACC_STATIC)==0) {
		usedVariables.add(new Constant(TypeInfo.TYPE_REFERENCE, "cls"));
	    }
	//}
	return usedVariables;
    }
    
    /**
     * Gets list of used ICell variables.
     *
     * @return list of used ICell variables in TreeSet from
     */
    public TreeSet getUsedICells() {
	//if(usedICells==null) {
	    TreeSet usedICells = new TreeSet();
	    Iterator it = this.iterator();
	    IR ir;
	    while(it.hasNext()) {
		ir = (IR)it.next();
		if(ir.hasAttribute(IRAttribute.ELIMINATED)) continue;

		// multianewarray_quick always require ICEll as target variabled
		if(ir.getShortOpcode() == OpcodeConst.opc_multianewarray_quick) {
            //System.out.println("ADD USED ICELL 1: " + ir);
		    usedICells.add(ir.getTarget(0));
		}

		if(ir.hasAttribute(IRAttribute.NEED_ICELL)) {
		    for(int i=0; i<ir.getNumOfTargets(); i++) {
			JavaVariable v = ir.getTarget(i);
			if (v.getType() != TypeInfo.TYPE_REFERENCE) continue;
			if(v instanceof StackVariable || v instanceof LocalVariable) {
                //System.out.println("ADD USED ICELL 2: " + ir);
			    usedICells.add(v);
			}
		    }
		}
		for(int i=0; i<ir.getNumOfOperands(); i++) {
		    JavaVariable v = ir.getOperand(i);
		    if (v.getType() != TypeInfo.TYPE_REFERENCE) continue;
		    if((v instanceof StackVariable || v instanceof LocalVariable)
			    && ir.needICell()) {
            //System.out.println("ADD USED ICELL 3: " + ir);
			usedICells.add(v);
		    }
		    if (v instanceof LocalVariable && v.getIndex() < methodInfo.argsSize) {
            //System.out.println("ADD USED ICELL 4: " + ir);
			usedICells.add(v);
		    }
		}
		if (ir.hasAttribute(IRAttribute.POTENTIAL_GC_POINT)) {
		    Iterator it2 = ir.getLiveVariables().iterator();
		    while (it2.hasNext()) {
			JavaVariable live = (JavaVariable) it2.next();
			if (live instanceof StackVariable || live instanceof LocalVariable) {
			    if (live.getType () == TypeInfo.TYPE_REFERENCE) {
                    boolean add = true;
                    for(int i = 0 ; i < ir.getNumOfTargets() ; i++) {
                        JavaVariable v = ir.getTarget(i);
                        if(v.equals(live)) add = false;
                    }
                    if(add) {
                        //System.out.println("ADD USED ICELL 5: " + ir);
                        usedICells.add(live);
                    }
			    }
			}
		    }
		}
	    }
	    if((methodInfo.access & ClassFileConst.ACC_STATIC)==0 && MethodInliner.inline_depth==0) {
                    //System.out.println("ADD USED ICELL 6: ");
		usedICells.add(new Constant(TypeInfo.TYPE_REFERENCE, "cls"));
	    }
	//}
	return usedICells;
    }

    /**
     * Record changed reference-arguments
     */
    public TreeSet getChangedArgs()
    {
	//if(changedArgs==null) {
	    TreeSet changedArgs = new TreeSet();
	    int argumentSize = this.getMethodInfo().argsSize;
	    Iterator it = this.iterator();
	    while(it.hasNext()) {
		IR ir = (IR)it.next();
		int ntarget = ir.getNumOfTargets();
		for(int i=0; i<ntarget; i++) {
		    JavaVariable target = ir.getTarget(i);
		    if(target.getType() == TypeInfo.TYPE_REFERENCE) {
			if(target instanceof LocalVariable && target.getIndex() < argumentSize) {
			    // this target variable is argument.
			    // and is reference.
			    changedArgs.add(target);
			}
		    }
		}
	    }
	//}
        return changedArgs;
    }

    /**
     * 
     */
    public void checkControlFlow() {
        Iterator it = this.iterator();
        IR ir, succ, pred;
        int i,j;
        while (it.hasNext()) {
            ir = (IR) it.next();
            for (i = 0; i < ir.getSuccNum(); i++) {
                succ = ir.getSucc(i);
                if (succ.isPred(ir) != true) {
                    System.out.println("Error : " + ir + " should be pred. of " + succ);
                }
            }
            for (i = 0; i < ir.getPredNum(); i++) {
                pred = ir.getPred(i);
                if (pred.isSucc(ir) != true) {
                    System.out.println("Error : " + ir + " should be succ. of " + pred);
                }

            }
        }
        
    }

    /**
     * Generates IR.
     * This uses IRGenerator.
     */ 
    private void generateIR() {
	//CFG_iterator it = new CFG_iterator(this, CFG_iterator.ITER_BPC);
        Iterator it = this.iterator();
        IR ir;
        int i;
	while(it.hasNext()) {
	    // Generate IR corresponds to the bytecode
	    ir  = (IR)it.next();
	    irGenerator.makeIR(ir);
	} // end of while

    }
    /**
     * Collects IRs which need to generate goto label.
     * 
     * This is required for branch targets (including lookupswitch and
     * tableswitch) and ret pointers from jsr.
     */
    private void computeNeedLabelList() {
        // get list of bpc which need goto label
        this.needLabelList = new TreeSet();
        IR ir, branchTarget;
        JavaVariable temp;
        int i;
	Iterator it = this.iterator();
	while(it.hasNext()) {
	    ir = (IR) it.next();
            branchTarget = ir.getBranchTarget();
            if (branchTarget != null) {
                needLabelList.add(new Integer(branchTarget.getBpc()));
            }
            if (ir.getShortOpcode() == OpcodeConst.opc_lookupswitch) {
                for (i = 2; i < ir.getNumOfOperands(); i += 2) {
                    temp = ir.getOperand(i);
                    needLabelList.add(new Integer(Integer.parseInt(temp.toString())));
                }
                temp = ir.getOperand(i - 1);
                needLabelList.add(new Integer(Integer.parseInt(temp.toString())));
            } 
            if (ir.getShortOpcode() == OpcodeConst.opc_tableswitch) {
                for (i = 2; i < ir.getNumOfOperands(); i++) {
                    temp = ir.getOperand(i);
                    needLabelList.add(new Integer(Integer.parseInt(temp.toString())));
                }
            }
            if (ir.hasAttribute(IRAttribute.BB_HANDLER)) {
                needLabelList.add(new Integer(ir.getBpc()));
            }
            if (ir.getShortOpcode() == OpcodeConst.opc_jsr  
                || ir.getShortOpcode() == OpcodeConst.opc_jsr_w) { // ret from jst
                needLabelList.add(new Integer(ir.getBpc() + 3));
            }

	}

    }
   
    /**
     * Builds basic blocks.
     * 
     * This marks following attributes.
     * <ul>
     * <li>IRAttribute.BB_START for basic block entry
     * <li>IRAttribute.BB_HANDLER for exception handler entry
     * </ul>
     */
    private void buildBB() {
	int bb_id = 1;
	for(int i=0; i<block.length; i++) {
	    if(block[i]!=null) {
		if(block[i].getIR().hasAttribute(IRAttribute.BB_START)) {
	            bb_id++;
		}
		block[i].getIR().setAttribute(IRAttribute.BB_ID, bb_id-1);
	  }
	}
	ExceptionEntry einfo[] = methodInfo.exceptionTable;
	for(int i=0; i<einfo.length; i++) {
            IR ir = block[einfo[i].handlerPC].getIR();
	    ir.setAttribute(IRAttribute.BB_HANDLER);
	    ir.setAttribute(IRAttribute.HANDLER_ID, i+1);
	}
    }

    /**
     * Generates CFG.
     * This allocates IR for each bytecode. Then make predecessor and successor relationship among them.
     */
    private void generateCFG() {
	int pc, offset, addr, j;
        IR newIR;

	for(pc=0; pc<code.length; pc+=getCodeLen(pc)) {
	    Bytecode bcode = getBytecode(pc);
	    newIR = new IR(bcode);
	    bcode.setIR(newIR);
	    IRBlock.add(newIR); // add to vector
	    IRCache.put(new Integer(pc), newIR); // add to hastable
	}

	pc = 0;
	while(pc<code.length) {
	    Bytecode bcode = getBytecode(pc);

	    switch(bcode.getOpcode()) {
		case (byte)OpcodeConst.opc_ifeq:	case (byte)OpcodeConst.opc_ifne:
		case (byte)OpcodeConst.opc_iflt:	case (byte)OpcodeConst.opc_ifge:
		case (byte)OpcodeConst.opc_ifgt:	case (byte)OpcodeConst.opc_ifle:
		case (byte)OpcodeConst.opc_ifnull:	case (byte)OpcodeConst.opc_ifnonnull:
		    if(getCodeLen(pc)==bcode.getOperand(0)) {
			int operand[] = new int [0];
			block[pc] = new Bytecode(pc, (byte)OpcodeConst.opc_pop, operand);
			newIR = getIRAtBpc(pc);
			newIR.setBytecode(block[pc]);
			block[pc].setIR(newIR);
			link(getBytecode(pc), getBytecode(pc+bcode.getOperand(0)), true);
		    }else {
			link(bcode, getBytecode(pc+getCodeLen(pc)), /*true*/ false);
			link(bcode, getBytecode(pc+bcode.getOperand(0)), true);
		    }
		    break;
		case (byte)OpcodeConst.opc_if_icmpeq:	case (byte)OpcodeConst.opc_if_icmpne:
		case (byte)OpcodeConst.opc_if_icmplt:	case (byte)OpcodeConst.opc_if_icmpge:
		case (byte)OpcodeConst.opc_if_icmpgt:	case (byte)OpcodeConst.opc_if_icmple:
		case (byte)OpcodeConst.opc_if_acmpeq:	case (byte)OpcodeConst.opc_if_acmpne:
		    if(getCodeLen(pc)==bcode.getOperand(0)) {
			int operand[] = new int [0];
			block[pc] = new Bytecode(pc, (byte)OpcodeConst.opc_pop2, operand);
			newIR = getIRAtBpc(pc);
			newIR.setBytecode(block[pc]);
			block[pc].setIR(newIR);
			link(getBytecode(pc), getBytecode(pc+bcode.getOperand(0)), true);
		    } else {
			link(bcode, getBytecode(pc+getCodeLen(pc)), /*true*/ false);
			link(bcode, getBytecode(pc+bcode.getOperand(0)), true);
		    }
		    break;

		case (byte)OpcodeConst.opc_goto:	case (byte)OpcodeConst.opc_goto_w:
		    link(bcode, getBytecode(pc+bcode.getOperand(0)), true);
		    break;

		case (byte)OpcodeConst.opc_jsr:		case (byte)OpcodeConst.opc_jsr_w:
		    //link(bcode, getBytecode(pc+getCodeLen(pc)), false);
		    link(bcode, getBytecode(pc+bcode.getOperand(0)), true);
		    break;

		case (byte)OpcodeConst.opc_ireturn:	case (byte)OpcodeConst.opc_lreturn:
		case (byte)OpcodeConst.opc_areturn:	case (byte)OpcodeConst.opc_freturn:
		case (byte)OpcodeConst.opc_dreturn:	case (byte)OpcodeConst.opc_return:
		case (byte)OpcodeConst.opc_ret:		case (byte)OpcodeConst.opc_athrow:
		    break;

		case (byte)OpcodeConst.opc_lookupswitch:
		    link(bcode, getBytecode(pc+bcode.getOperand(0)), true);
		    for(j=3 ; j<bcode.getOperandNum(); j+=2) {
			if(!bcode.getIR().isSucc(getBytecode(pc+bcode.getOperand(j)).getIR())) {
			    link(bcode, getBytecode(pc+bcode.getOperand(j)), true);
			}
		    }
		    break;

		case (byte)OpcodeConst.opc_tableswitch:
		    link(bcode, getBytecode(pc+bcode.getOperand(0)), true);
		    for(j=3 ; j<bcode.getOperandNum(); j++) {
			if(!bcode.getIR().isSucc(getBytecode(pc+bcode.getOperand(j)).getIR())) {
			    link(bcode, getBytecode(pc+bcode.getOperand(j)), true);
			}
		    }
		    break;

		default:
		    link(bcode, getBytecode(pc+getCodeLen(pc)), false);
	    }

	    pc += getCodeLen(pc);
	}
        this.maxBpc = pc;

	for(pc = 0; pc<code.length; pc += getCodeLen(pc)) {
	    Bytecode bcode = getBytecode(pc);
	    if(bcode.getOpcode() == (byte)OpcodeConst.opc_jsr ||
		    bcode.getOpcode() == (byte)OpcodeConst.opc_jsr_w) {
		visited = new int [code.length];
		link_jsr_ret(getBytecode(pc+bcode.getOperand(0)), getBytecode(pc+getCodeLen(pc)));
	    }
	}

        // Connect exception edges
        int handlerPC;
	ExceptionEntry exceptionTable[] = methodInfo.exceptionTable;
	for(pc = 0; pc<code.length; pc += getCodeLen(pc)) {
	    Bytecode bcode = getBytecode(pc);
            for( int i = 0 ; i < exceptionTable.length ; i++ ) {
                if( exceptionTable[i].startPC <= pc
                        && exceptionTable[i].endPC > pc ) {
                    handlerPC = exceptionTable[i].handlerPC;
                    linkImplicitly(bcode, getBytecode(handlerPC));
                }
            }
	}
    }
    private int visited [];

    /**
     * Link predecessor and successor relationship between jsr and ret.
     */
    private void link_jsr_ret(Bytecode ret, Bytecode jsr_1) {
	visited[ret.getBpc()] = 1;
	if(ret.getOpcode()==(byte)OpcodeConst.opc_ret) {
	    link(ret, jsr_1, true);
	} else {
	    for(int i=0; i<ret.getIR().getSuccNum(); i++) {
		Bytecode succ = ret.getIR().getSucc(i).getBytecode();
		if(visited[succ.getBpc()]!=1) {
		    link_jsr_ret(ret.getIR().getSucc(i).getBytecode(), jsr_1);
		}
	    }
	}
    }

    /**
     * Returns bytecode at specific bpc.
     */ 
    private Bytecode getBytecode(int pc) {
	new aotc.share.Assert(pc>=0 && pc<code.length, "pc out of bounds!!");

	if(block[pc]!=null) {
	    return block[pc];
	}

	int opcode = code[pc];
	if(opcode<0) opcode += 256;

	int operand[];
	if(opcode==OpcodeConst.opc_wide) {
	    if(code[pc+1]==(byte)OpcodeConst.opc_iinc) {
		operand = new int [2];
		operand[0] = makeIntVal((byte)0, (byte)0, code[pc+2], code[pc+3]);
		operand[1] = makeShortVal(code[pc+4], code[pc+5]);
	    } else {
		operand = new int [1];
		operand[0] = makeIntVal((byte)0, (byte)0, code[pc+2], code[pc+3]);
	    }
	} else if(opcode==OpcodeConst.opc_lookupswitch) {
	    int addr = (pc+4) & 0xFFFFFFFC;
	    int pads = addr - pc - 1;
	    int npairs = makeIntVal(code[addr+4], code[addr+5], code[addr+6], code[addr+7]);
	    int len = 2 + npairs*2;
	    operand = new int [len];
	    for(int i=0; i<len; i++) {
		operand[i] = makeIntVal(code[addr+i*4], code[addr+i*4+1],
			code[addr+i*4+2], code[addr+i*4+3]);
	    }
	} else if(opcode==OpcodeConst.opc_tableswitch) {
	    int addr = (pc+4) & 0xFFFFFFFC;
	    int pads = addr - pc - 1;
	    int l = makeIntVal(code[addr+4], code[addr+5], code[addr+6], code[addr+7]);
	    int h = makeIntVal(code[addr+8], code[addr+9], code[addr+10], code[addr+11]);
	    int len = 3 + (h-l+1);
	    operand = new int [len];
	    for(int i=0; i<len; i++) {
		operand[i] = makeIntVal(code[addr+i*4], code[addr+i*4+1],
			code[addr+i*4+2], code[addr+i*4+3]);
	    }
	} else {
	    int type = BytecodeInfo.opcType[opcode];
	    new aotc.share.Assert(type>=0, "unsupported opcode!! : " + OpcodeConst.opcNames[opcode]);

	    int noperand = 0;
	    if(0<type && type<10) noperand = 1;
	    else if(10<=type && type<100) noperand = 2;
	    else if(100<=type) noperand = 3;

	    int currentpc = pc + 1;		
	    operand = new int [noperand];
	    for(int index=0; type!=0; index++, type/=10) {
		switch(type%10) {
		    case 1: // signed byte
			operand[index] = code[currentpc];
			currentpc++;
			break;
		    case 2: // unsigned byte
			operand[index] = makeShortVal((byte)0, code[currentpc]);
			currentpc++;
			break;
		    case 3: // signed short
			operand[index] = makeShortVal(code[currentpc], code[currentpc+1]);
			currentpc+=2;
			break;
		    case 4: // unsigned short
			operand[index] = makeIntVal((byte)0, (byte)0, code[currentpc], code[currentpc+1]);
			currentpc+=2;
			break;
		    case 5: // signed int
			operand[index] = makeIntVal(code[currentpc], code[currentpc+1], code[currentpc+2], code[currentpc+3]);
			currentpc+=4;
			break;
		    default:
			new aotc.share.Assert(false, "unsupported type number : " + (type%10)); 
		}
	    }
	}
	if(opcode==OpcodeConst.opc_wide) {
	    block[pc] = new Bytecode(pc, code[pc+1], operand);
	} else {
	    block[pc] = new Bytecode(pc, code[pc], operand);
	}
	return block[pc];
    }
    
    /**
     * Links two IR as predecessor, successor.
     * Note that even though the parameters are bytecodes but the predecessor 
     * and successor relationships are built on IRs.
     * @param pred predecessor
     * @param succ sucessor
     * @param bb_start whether successor is a basic block entry or not
     */
    private void link(Bytecode pred, Bytecode succ, boolean bb_start) {
	//pred.addSucc(succ);
	//succ.addPred(pred);
	//if(bb_start) succ.setAttribute(BytecodeAttribute.BB_START);
        pred.getIR().addSucc(succ.getIR());
        succ.getIR().addPred(pred.getIR());
	if(bb_start) succ.getIR().setAttribute(IRAttribute.BB_START);
    }
    private void linkImplicitly(Bytecode pred, Bytecode succ) {
	//pred.addSucc(succ);
	//succ.addPred(pred);
	//if(bb_start) succ.setAttribute(BytecodeAttribute.BB_START);
        pred.getIR().addImplicitSucc(succ.getIR());
        succ.getIR().addImplicitPred(pred.getIR());
    }

    /**
     * Makes short value from byte1, byte2
     * @param n1 upper byte
     * @param n2 lower byte
     * @return short value combining n1 and n2
     */
    public static short makeShortVal(byte n1, byte n2) {
	return (short)( (n1 << 8) | (n2 & 0x00FF) );		
    }
    /**
     * Makes int value from byte1, byte2, byte3 and byte4
     * @param n1 byte with MSB
     * @param n2 second byte
     * @param n3 third byte
     * @param n4 byte with LSB
     * @return int value combining n1, n2, n3 and n4
     */
    public static int makeIntVal(byte n1, byte n2, byte n3, byte n4) {
	short t1 = makeShortVal(n1, n2);
	short t2 = makeShortVal(n3, n4);
	return (t1 << 16) | (t2 & 0x0000FFFF);
    }

    /**
     * Computes code length of given bytecode at bpc
     * @param pc bpc
     * @return code length
     */
    private int getCodeLen(int pc) {
	int index = code[pc];
	if(index<0) index += 256;
	int codelen = OpcodeConst.opcLengths[index];

	if(index==OpcodeConst.opc_wide) {
	    if(code[pc+1]==(byte)OpcodeConst.opc_iinc) {
		return 6;
	    } else {
		return 4;
	    }
	}
	if(codelen!=0) return codelen;

	int addr = (pc+4) & 0xFFFFFFFC;
	int pads = addr - pc - 1;

	if(index==OpcodeConst.opc_lookupswitch) {
	    int npairs = makeIntVal(code[addr+4], code[addr+5], code[addr+6], code[addr+7]);
	    codelen = 1 + pads + 8 + npairs*8;
	} else if(index==OpcodeConst.opc_tableswitch) {
	    int low = makeIntVal(code[addr+4], code[addr+5], code[addr+6], code[addr+7]);
	    int high = makeIntVal(code[addr+8], code[addr+9], code[addr+10], code[addr+11]);
	    codelen = 1 + pads + 12 + (high-low+1)*4;
	}
	return codelen;
    }

}
