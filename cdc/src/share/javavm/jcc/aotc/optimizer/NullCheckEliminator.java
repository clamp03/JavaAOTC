package aotc.optimizer;

import java.util.*;
import aotc.share.*;
import aotc.cfg.*;
import aotc.ir.*;
import opcodeconsts.*;
import aotc.translation.*;

/**
 * Elimiates null check in each method.
 * Determines variables which value is non-null or already checked null, 
 * remove this redundant null check
 * <p>
 * First gets cfg that has method bytecode.
 * 
 * Then determines Gen and Kill accoring to each bytecode
 *   Gen is the treeset of variables which value is non-null or already checked null
 *   Kill is the treeset of variables which value may be null
 * 
 * With Gen and kill in each bytecode, 
 * determines the set of variables which value is non-null or already checked null for all bytecodes
 *
 * Set the null check attributee to bytecode
 *
 * <p>
 * @author Jung Dong-Heon
 */

public class NullCheckEliminator {
    
    /**
     * control flow graph
     */ 
    private CFG cfg;
    
    /**
     * code size of method that will be apllied null check elimination
     */
    private int codeSize;
    //private TreeSet exceptionTable;

    /**
     * Constructor
     *
     * @param cfg constorl flow graph
     */
    public NullCheckEliminator(CFG cfg/*,ExceptionEntry exceptionTable[]*/) {
	this.cfg = cfg;
	//codeSize = cfg.getMethodInfo().code.length;
        codeSize = cfg.getCodeSize();
	//Iterator it = usedVar.iterator();
	/*
	while(it.hasNext()) {
	    JavaVariable usedVariable = (JavaVariable)it.next();
	    if( usedVariable.getType() != TypeInfo.TYPE_REFERENCE ) {
		if( !usedVar.remove(usedVariable) ) {
		    new aotc.share.Assert(false,"Can't Remove UsedVar : " + usedVariable);
		}
	    }
	}
	*/
    }

    /**
     * Driver for null check elimiation
     * 
     * 1) initialize 
     *    initialize GEN, KILL, IN and OUT
     *    add all bytecodes to ChangeNodes
     *    set GEN and KILL for each bytecode
     *        GEN_Check : the variable that will be checked in bytecode 
     *                    so the variable is non-null after executing bytecode
     *        GEN_Code  : the variable that will be assigned non-null value in bytecode
     *
     * 2) Determines the variable that can't need null check
     *    Worklist algorithm is used
     *    Remove a bytecode from ChangeNodes. 
     *    Determines the variables that is non-null in all predecessors OUT of this bytecode
     *    Add this variables in IN. Then determines OUT using GEN, KILL and bytecode(copy)
     *    If OUT is changed, add this bytecode to ChangedNodes
     *
     * 3) Set Null Check Eliminated attribute to bytecode, so elimnate null check in c code generation stage
     */
    public void doElimination() {
	/*
	TreeSet earliest[] = earliest = doNullCheckInsertion();
	doNullCheckElimination(earliest);
	*/
	// initialize 
	TreeSet ChangeNodes = new TreeSet();
        Iterator it;
	TreeSet GEN_Check[] = new TreeSet[codeSize];
	TreeSet GEN_Code[] = new TreeSet[codeSize];
	TreeSet KILL[] = new TreeSet[codeSize];
	TreeSet IN[] = new TreeSet[codeSize];
	TreeSet OUT[] = new TreeSet[codeSize];
	
	//Bytecode bytecode;
	// set ChangeNodes
        IR ir;
	it = cfg.iterator();
	while(it.hasNext())
	    ChangeNodes.add((IR) it.next());

	// set GEN & KILL
	// Gen has non-null variables.
	// Kill has variables which value may be null
	int bpc;
	it = cfg.iterator();
	while(it.hasNext()) {
	    ir = (IR) it.next();
	    bpc = ir.getBpc();
	    GEN_Check[bpc] = new TreeSet();
	    GEN_Code[bpc] = new TreeSet();
	    KILL[bpc] = new TreeSet();
	    IN[bpc] = new TreeSet();
	    OUT[bpc] = new TreeSet();
	    switch(ir.getShortOpcode()) {
		// array load
		case OpcodeConst.opc_iaload:	case OpcodeConst.opc_laload:
		case OpcodeConst.opc_faload:	case OpcodeConst.opc_daload:
		case OpcodeConst.opc_aaload:	case OpcodeConst.opc_baload:
		case OpcodeConst.opc_caload:	case OpcodeConst.opc_saload:
		// array store
		case OpcodeConst.opc_iastore:	case OpcodeConst.opc_lastore:
		case OpcodeConst.opc_fastore:	case OpcodeConst.opc_dastore:
		case OpcodeConst.opc_aastore:	case OpcodeConst.opc_bastore:
		case OpcodeConst.opc_castore:	case OpcodeConst.opc_sastore:
		// array length
		case OpcodeConst.opc_arraylength:
		// athrow
		case OpcodeConst.opc_athrow:
		// monitor
		case OpcodeConst.opc_monitorenter:  case OpcodeConst.opc_monitorexit:
		// putfield
		case OpcodeConst.opc_putfield_quick:	case OpcodeConst.opc_putfield2_quick:
		case OpcodeConst.opc_aputfield_quick:	case OpcodeConst.opc_putfield_quick_w:
		// getfield
		case OpcodeConst.opc_getfield_quick:    case OpcodeConst.opc_getfield2_quick:
		    GEN_Check[bpc].add(ir.getOperand(0));
		    break;
		// invoke virtual
		case OpcodeConst.opc_invokevirtual_quick_w: case OpcodeConst.opc_invokenonvirtual_quick:
		case OpcodeConst.opc_invokevirtual_quick:   case OpcodeConst.opc_ainvokevirtual_quick:
		case OpcodeConst.opc_dinvokevirtual_quick:  case OpcodeConst.opc_vinvokevirtual_quick:
		case OpcodeConst.opc_invokevirtualobject_quick:
		// invoke interface
		case OpcodeConst.opc_invokeinterface_quick:
		// getfield
		case OpcodeConst.opc_agetfield_quick:	case OpcodeConst.opc_getfield_quick_w:
		    GEN_Check[bpc].add(ir.getOperand(0));
		// invoke static
		case OpcodeConst.opc_invokestatic_checkinit_quick:  case OpcodeConst.opc_invokestatic_quick:
		// get static
		case OpcodeConst.opc_agetstatic_quick:		case OpcodeConst.opc_getstatic_quick:
		case OpcodeConst.opc_agetstatic_checkinit_quick:case OpcodeConst.opc_getstatic_checkinit_quick:
		// aconst null
		case OpcodeConst.opc_aconst_null:
		// aldc
		case OpcodeConst.opc_aldc_ind_quick:	case OpcodeConst.opc_aldc_ind_w_quick:
		case OpcodeConst.opc_aldc_quick:        case OpcodeConst.opc_aldc_w_quick:
		    if( ir.getNumOfTargets() > 0 && ir.getTarget(0).getType() == TypeInfo.TYPE_REFERENCE ) {
			KILL[bpc].add(ir.getTarget(0));
		    }
		    break;

		// newarray
		case OpcodeConst.opc_newarray:
		// multianewarray
		case OpcodeConst.opc_multianewarray_quick:
		// anewarray
		case OpcodeConst.opc_anewarray_quick:
		// new
		case OpcodeConst.opc_new_quick:
		case OpcodeConst.opc_new_checkinit_quick:
		    GEN_Code[bpc].add(ir.getTarget(0));
		    break;
	    }
	}
	
	//it = new CFG_iterator(cfg, CFG_iterator.ITER_BPC);
	//ChangeNodes.add(it.next());
	//IN[0] = new TreeSet();
	//IN[0].add(new Constant(TypeInfo.TYPE_REFERENCE, "cls" ));
	// Determines bytecode that can eliminate null check
	IR succ;
	TreeSet oldOut;
	JavaVariable cls = new Constant(TypeInfo.TYPE_REFERENCE, "cls" );
	while( ChangeNodes.size() > 0 ) {
	    ir = (IR) ChangeNodes.first();
	    bpc = ir.getBpc();
	    int before = ChangeNodes.size();
	    ChangeNodes.remove(ir);
	    int after = ChangeNodes.size();
	    new aotc.share.Assert( after == before - 1 , "Error at Removing(NullCheckEliminator)"); 
	    IN[bpc] = new TreeSet(solveInSet(ir,OUT));
	    IN[bpc].add(cls);
	    if(ir.hasAttribute(IRAttribute.BB_HANDLER)) {
		IN[bpc].add(new StackVariable(TypeInfo.TYPE_REFERENCE, 0));
	    }
	    oldOut = new TreeSet(OUT[bpc]);
	    OUT[bpc] = new TreeSet(IN[bpc]);
	    
	    if(ir.hasAttribute(IRAttribute.ELIMINATED)) {
		if( oldOut.size() != OUT[bpc].size() ) { 
		    for( int i = 0 ; i < ir.getSuccNum() ; i++ ) {
			succ = ir.getSucc(i);
			ChangeNodes.add(succ);
		    }
		}
		continue;   
	    }

	    switch(ir.getShortOpcode()) {
		case OpcodeConst.opc_aaload:
		    OUT[bpc].remove(ir.getTarget(0));
		    break;
		// array load
		case OpcodeConst.opc_aload:	case OpcodeConst.opc_aload_0:
		case OpcodeConst.opc_aload_1:	case OpcodeConst.opc_aload_2:
		case OpcodeConst.opc_aload_3:	
		// array store
		case OpcodeConst.opc_astore:
		case OpcodeConst.opc_astore_0:	case OpcodeConst.opc_astore_1:
		case OpcodeConst.opc_astore_2:	case OpcodeConst.opc_astore_3:
		    OUT[bpc].remove(ir.getTarget(0));
		    if(OUT[bpc].contains(ir.getOperand(0))) {
			OUT[bpc].add(ir.getTarget(0));
		    }
		    break;
		// dup
		case OpcodeConst.opc_dup:
		    if(ir.getTarget(0).getType() == TypeInfo.TYPE_REFERENCE) {
			OUT[bpc].remove(ir.getTarget(0));
			if(OUT[bpc].contains(ir.getOperand(0))) {
			    OUT[bpc].add(ir.getTarget(0));
			}
		    }
		    break;
		case OpcodeConst.opc_dup_x1:
		case OpcodeConst.opc_dup_x2:
		    if( ir.getNumOfOperands() == 2 ) {
			boolean value1 = false, value2 = false;
			if(ir.getOperand(0).getType() == TypeInfo.TYPE_REFERENCE 
			    && OUT[bpc].contains(ir.getOperand(0)))
			    value1 = true;
			if(ir.getOperand(1).getType() == TypeInfo.TYPE_REFERENCE
			    && OUT[bpc].contains(ir.getOperand(1)))
			    value2 = true;

			OUT[bpc].remove(ir.getTarget(0));
			OUT[bpc].remove(ir.getTarget(1));
			OUT[bpc].remove(ir.getTarget(2));

			if( value1 ) {
			    OUT[bpc].add(ir.getTarget(0));
			    OUT[bpc].add(ir.getTarget(2));
			}
			if( value2 ) {
			    OUT[bpc].add(ir.getTarget(1));
			}
		    }else if( ir.getNumOfOperands() == 3 ) {
			boolean value1 = false, value2 = false, value3 = false;
			if(ir.getOperand(0).getType() == TypeInfo.TYPE_REFERENCE
			    &&OUT[bpc].contains(ir.getOperand(0)))
			    value1 = true;
			if(ir.getOperand(1).getType() == TypeInfo.TYPE_REFERENCE
			    &&OUT[bpc].contains(ir.getOperand(1)))
			    value2 = true;
			if(ir.getOperand(2).getType() == TypeInfo.TYPE_REFERENCE
			    &&OUT[bpc].contains(ir.getOperand(2)))
			    value3 = true;

			for(int i = 0 ; i < 4 ; i++ )
			    OUT[bpc].remove(ir.getTarget(i));

			if( value1 ) {
			    OUT[bpc].add(ir.getTarget(0));
			    OUT[bpc].add(ir.getTarget(3));
			}
			if( value2 ) {
			    OUT[bpc].add(ir.getTarget(2));
			}
			if( value3 ) {
			    OUT[bpc].add(ir.getTarget(1));
			}
		    }else {
			new aotc.share.Assert(false, "dup_x1 or dup_x2 has wrong num of operands");
		    }
		    break;
		case OpcodeConst.opc_dup2:
		    if( ir.getNumOfOperands() == 2 ) {
			OUT[bpc].remove(ir.getTarget(2));
			OUT[bpc].remove(ir.getTarget(3));
			if( ir.getOperand(0).getType() == TypeInfo.TYPE_REFERENCE
			    &&OUT[bpc].contains(ir.getOperand(0))) {
			    OUT[bpc].add(ir.getTarget(3));
			}
			if( ir.getOperand(1).getType() == TypeInfo.TYPE_REFERENCE
			    &&OUT[bpc].contains(ir.getOperand(1))) {
			    OUT[bpc].add(ir.getTarget(2));
			}
		    }else if( ir.getNumOfOperands() == 1 ) {
			OUT[bpc].remove(ir.getTarget(1));
			if( ir.getOperand(0).getType() == TypeInfo.TYPE_REFERENCE
			    &&OUT[bpc].contains(ir.getOperand(0))) {
			    OUT[bpc].add(ir.getTarget(1));
			}
		    }else {
			new aotc.share.Assert(false, "dup2 has wrong num of operands");
		    }
		    break;
		case OpcodeConst.opc_dup2_x1:	case OpcodeConst.opc_dup2_x2:
		    if( ir.getNumOfOperands() == 2 ) {
			boolean value1 = false, value2 = false;
			if(ir.getOperand(0).getType() == TypeInfo.TYPE_REFERENCE 
			    && OUT[bpc].contains(ir.getOperand(0)))
			    value1 = true;
			if(ir.getOperand(1).getType() == TypeInfo.TYPE_REFERENCE
			    && OUT[bpc].contains(ir.getOperand(1)))
			    value2 = true;

			OUT[bpc].remove(ir.getTarget(0));
			OUT[bpc].remove(ir.getTarget(1));
			OUT[bpc].remove(ir.getTarget(2));

			if( value1 ) {
			    OUT[bpc].add(ir.getTarget(0));
			    OUT[bpc].add(ir.getTarget(2));
			}
			if( value2 ) {
			    OUT[bpc].add(ir.getTarget(1));
			}
		    }else if( ir.getNumOfOperands() == 3 ) {
			if( ir.getNumOfTargets() == 4 ) {
			    boolean value1 = false, value2 = false, value3 = false;
			    if(ir.getOperand(0).getType() == TypeInfo.TYPE_REFERENCE
				&&OUT[bpc].contains(ir.getOperand(0)))
				value1 = true;
			    if(ir.getOperand(1).getType() == TypeInfo.TYPE_REFERENCE
				&&OUT[bpc].contains(ir.getOperand(1)))
				value2 = true;
			    if(ir.getOperand(2).getType() == TypeInfo.TYPE_REFERENCE
				&&OUT[bpc].contains(ir.getOperand(2)))
				value3 = true;
			    for(int i = 0 ; i < 4 ; i++ )
				OUT[bpc].remove(ir.getTarget(i));

			    if( value1 ) {
				OUT[bpc].add(ir.getTarget(0));
				OUT[bpc].add(ir.getTarget(3));
			    }
			    if( value2 ) {
				OUT[bpc].add(ir.getTarget(2));
			    }
			    if( value3 ) {
				OUT[bpc].add(ir.getTarget(1));
			    }
			}else if( ir.getNumOfTargets() == 5 ) {
			    boolean value1 = false, value2 = false, value3 = false;
			    if(ir.getOperand(0).getType() == TypeInfo.TYPE_REFERENCE
				&&OUT[bpc].contains(ir.getOperand(0)))
				value1 = true;
			    if(ir.getOperand(1).getType() == TypeInfo.TYPE_REFERENCE
				&&OUT[bpc].contains(ir.getOperand(1)))
				value2 = true;
			    if(ir.getOperand(2).getType() == TypeInfo.TYPE_REFERENCE
				&&OUT[bpc].contains(ir.getOperand(2)))
				value3 = true;
			    for(int i = 0 ; i < 5 ; i++ )
				OUT[bpc].remove(ir.getTarget(i));

			    if( value1 ) {
				OUT[bpc].add(ir.getTarget(1));
				OUT[bpc].add(ir.getTarget(4));
			    }
			    if( value2 ) {
				OUT[bpc].add(ir.getTarget(0));
				OUT[bpc].add(ir.getTarget(3));
			    }
			    if( value3 ) {
				OUT[bpc].add(ir.getTarget(2));
			    }
			}else {
			    new aotc.share.Assert(false, "dup2_x1 or dup2_x2 has wrong num of targets");
			}
		    }else if( ir.getNumOfOperands() == 4 ) {
			boolean value1 = false, value2 = false, value3 = false, value4 = false;

			if(ir.getOperand(0).getType() == TypeInfo.TYPE_REFERENCE
			    &&OUT[bpc].contains(ir.getOperand(0)))
			    value1 = true;
			if(ir.getOperand(1).getType() == TypeInfo.TYPE_REFERENCE
			    &&OUT[bpc].contains(ir.getOperand(1)))
			    value2 = true;
			if(ir.getOperand(2).getType() == TypeInfo.TYPE_REFERENCE
			    &&OUT[bpc].contains(ir.getOperand(2)))
			    value3 = true;
			if(ir.getOperand(3).getType() == TypeInfo.TYPE_REFERENCE
			    &&OUT[bpc].contains(ir.getOperand(3)))
			    value4 = true;

			for(int i = 0 ; i < 6 ; i++ )
			    OUT[bpc].remove(ir.getTarget(i));

			if( value1 ) {
			    OUT[bpc].add(ir.getTarget(1));
			    OUT[bpc].add(ir.getTarget(5));
			}
			if( value2 ) {
			    OUT[bpc].add(ir.getTarget(0));
			    OUT[bpc].add(ir.getTarget(4));
			}
			if( value3 ) {
			    OUT[bpc].add(ir.getTarget(3));
			}
			if( value4 ) {
			    OUT[bpc].add(ir.getTarget(2));
			}
		    }else {
			new aotc.share.Assert(false, "dup2_x1 or dup2_x2 has wrong num of operands");
		    }
		    break;
		// swap
		case OpcodeConst.opc_swap:
			boolean value1 = false, value2 = false;
			if(ir.getOperand(0).getType() == TypeInfo.TYPE_REFERENCE
			    &&OUT[bpc].contains(ir.getOperand(0)))
			    value1 = true;
			if(ir.getOperand(1).getType() == TypeInfo.TYPE_REFERENCE
			    &&OUT[bpc].contains(ir.getOperand(1)))
			    value2 = true;
			    
			OUT[bpc].remove(ir.getTarget(0)); 
			OUT[bpc].remove(ir.getTarget(1));
			OUT[bpc].remove(ir.getOperand(0));
			OUT[bpc].remove(ir.getOperand(1));

			if( value1 ) {
			    OUT[bpc].add(ir.getTarget(0));
			}
			if( value2 ) {
			    OUT[bpc].add(ir.getTarget(1));
			}

		    break;
		default:
		    OUT[bpc].addAll(GEN_Check[bpc]);
		    OUT[bpc].addAll(GEN_Code[bpc]);
		    OUT[bpc].removeAll(KILL[bpc]);
	    }
	    if( oldOut.size() != OUT[bpc].size() ) {
		for( int i = 0 ; i < ir.getSuccNum() ; i++ ) {
		    succ = ir.getSucc(i);
		    ChangeNodes.add(succ);
		}
	    }
	}
	// Null Check Elimination
	it = cfg.iterator();
	while(it.hasNext()) {
	    ir = (IR) it.next();
	    bpc = ir.getBpc();
	    //if( GEN_Check[bpc].size() != 0 ) {
	    //	GEN_Check[bpc].removeAll(IN[bpc]);
	    String debugInfo ="NonNull : ";
	    Iterator debug = IN[bpc].iterator();
	    while(debug.hasNext()) {
		JavaVariable va = (JavaVariable)debug.next();
		debugInfo += va + "  ";

	    }
	    cfg.getIRAtBpc(bpc).addDebugInfoString(debugInfo);
	    if( GEN_Check[bpc].removeAll(IN[bpc]) ) {
		cfg.getIRAtBpc(bpc).setAttribute(IRAttribute.NULLCHECKELIMINATED);
		cfg.getIRAtBpc(bpc).addDebugInfoString("\nNull Check Eliminated");
	    }
	}
    }
    /**
     * Determine the variables that are non-null in all predecessor OUT of current bytecode
     *
     * @param ir the ir of current bytecode
     * @param OUT the OUT of all bytecodes.
     *
     * @return the variables that are non-null in all predecessor OUT of current bytecode
     */
    public TreeSet solveInSet(IR ir,TreeSet OUT[]) {
	TreeSet codeIn = new TreeSet();
	if(ir.getPredNum() > 0 ) {
	    codeIn.addAll(OUT[ir.getPred(0).getBpc()]);
	    for( int i = 1 ; i < ir.getPredNum() ; i++ ) {
		TreeSet pred = new TreeSet( OUT[ir.getPred(i).getBpc()] );
		TreeSet predUnion = new TreeSet( OUT[ir.getPred(i).getBpc()] );
		predUnion.addAll(codeIn);
		
		pred.removeAll(codeIn);
		codeIn.removeAll(OUT[ir.getPred(i).getBpc()]);
		
		predUnion.removeAll(codeIn);
		predUnion.removeAll(pred);

		codeIn = new TreeSet( predUnion );
	    }
	}
	return codeIn;
    }	
}
