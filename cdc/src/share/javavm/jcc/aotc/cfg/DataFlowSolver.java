package aotc.cfg;

import java.util.*;
import opcodeconsts.*;
import aotc.ir.*;
import aotc.share.*;
import aotc.translation.*;

/** 
 * DataFlowSolver solves reaching definition and makes UD-DU chains.
 * The final purpose of this class is to get UD-DU chanins to be used other optimizations.
 * This class uses iterative data flow analysis to solve reaching definition.
 * 
 * First, this class solves reaching definition.
 * And next, makes UD-DU chains based on reaching definition.
 * Note that only UD-DU chains are(need to be) stored at IR.
 */

public class DataFlowSolver {
    private CFG cfg;
    private int codeSize;
    private HashMap usedTargets = new HashMap();
    
    /**
     * Constructs DataFlowSolver with given cfg
     * 
     * @param cfg CFG to be solved
     */
    public DataFlowSolver(CFG cfg) {
	this.cfg = cfg;
	//codeSize = cfg.getMethodInfo().code.length;
        codeSize = cfg.getCodeSize();
    }
    /**
     * Makes caches of targets as bitset form.
     */
    private void cacheTargets(TreeSet usedVariables) {
	int size = usedVariables.size();
	JavaVariable target[] = new JavaVariable[size];
	BitSet targetinfo[] = new BitSet[size];
	for(int i=0; i<size; i++) {
	    targetinfo[i] = new BitSet(codeSize);
	}

	Iterator it = usedVariables.iterator();
	for(int i=0; it.hasNext(); i++) {
	    target[i] = (JavaVariable)it.next();
	}
	
	//it = new CFG_iterator(cfg, CFG_iterator.ITER_BPC);
	//Bytecode code;
        IR ir;
        it = cfg.iterator();
	while (it.hasNext()) {
	    //code = (Bytecode)it.next();
            ir = (IR) it.next();
            if (ir.hasAttribute(IRAttribute.ELIMINATED)) continue; // uhehe99
	    for(int i = 0; i < ir.getNumOfTargets(); i++) {
		int j;
		for(j = 0; j < size; j++) {
		    if(target[j].compareTo(ir.getTarget(i))==0) {
			targetinfo[j].set(ir.getBpc());
		    }
		}
	    }
	}

	for(int i=0; i<size; i++) {
	    usedTargets.put(target[i].toString(), targetinfo[i]);
	}
    }
    
    /**
     * Solves reaching definition and makes UD-DU chains.
     * This method use iterative data-flow analysis.
     *
     * IN[b]  : a bit vector(one bit for each definition) represent definitions reaches to b
     * OUT[b] : a bit vector represent definitions go through b
     * KILL[b] : definitions killed at b 
     * GEN[b] : set of locally generated definitions in b
     *
     * OUT[b] = GEN[b]U(IN[b]-KILL[b])
     *
     */
    public void makeUDAndDUChains() {
	cacheTargets(cfg.getUsedVariables(false));

	BitSet IN[] = new BitSet[codeSize];
	BitSet OUT[] = new BitSet[codeSize];
	BitSet GEN[] = new BitSet[codeSize];
	BitSet KILL[] = new BitSet[codeSize];
	BitSet oldOut;

	TreeSet ChangeNodes = new TreeSet();
	Iterator it;
	int bpc;

	for (int i = 0 ; i < codeSize; i++) {
	    GEN[i] = new BitSet(codeSize);
	    KILL[i] = new BitSet(codeSize);
	    IN[i] = new BitSet(codeSize);
	    OUT[i] = new BitSet(codeSize);
	}

	// set GEN & KILL
	//Bytecode v1;
	//Bytecode v2;
        IR v1, v2;
	//it1 = new CFG_iterator(cfg, CFG_iterator.ITER_BPC);
        it = cfg.iterator();
	while (it.hasNext()) {
	    v1 = (IR) it.next();
	    ChangeNodes.add(v1);
	    //if(v1.getIR().getNumOfTargets()==0) continue; // uhehe99
            if (v1.hasAttribute(IRAttribute.ELIMINATED)) continue; // uhehe99
	    bpc = v1.getBpc();
	    GEN[bpc].set(bpc);
	    for(int i=0; i<v1.getNumOfTargets(); i++) {
		KILL[bpc].or((BitSet)usedTargets.get(v1.getTarget(i).toString()));
	    }
	}

	IR code, succ, pred;
	//Iterator it; 
	while (ChangeNodes.size() > 0) {
	    code = (IR) ChangeNodes.first();
	    bpc = code.getBpc();
	    ChangeNodes.remove(code);

	    for (int i = 0; i < code.getPredNum(); i++) {
		pred = code.getPred(i);
		IN[bpc].or(OUT[pred.getBpc()]);
	    }

	    oldOut = OUT[bpc];
	    OUT[bpc] = (BitSet)IN[bpc].clone();
	    OUT[bpc].andNot(KILL[bpc]);
	    OUT[bpc].or(GEN[bpc]);
	    
	    if (!oldOut.equals(OUT[bpc])) {
		for (int i = 0; i < code.getSuccNum(); i++) {
		    succ = code.getSucc(i);
		    ChangeNodes.add(succ);
		}
	    }

	}

        // Save reachingDef to CFG
        cfg.setReachingDef(IN);

	// Makes UD-DU chains
        it = cfg.iterator();
	while (it.hasNext()) {
	    v1 = (IR)it.next();
	    v1.allocateUDAndDUChains();
	}
        
        it = cfg.iterator();
	while (it.hasNext()) {
	    v1 = (IR)it.next();
	    BitSet in = IN[v1.getBpc()];
	    for(int i=0; i<codeSize; i++) {
		if(in.get(i)) {
		    makeUDAndDUChains(v1, cfg.getIRAtBpc(i));
		}
	    }
	}
    }

    /**
     * makes UD-DU relations for two IR.
     * @param v1 first IR
     * @param v2 second IR
     */
    private static void makeUDAndDUChains(IR v1, IR v2) {
	if (v1.hasAttribute(IRAttribute.ELIMINATED)
            || v2.hasAttribute(IRAttribute.ELIMINATED)) {
            return;
        } // uhehe99

	// v1 is a Bytecode and v2 is a reaching Def. to v1
	int i, j, i1, i2;
	i1 = v1.getNumOfOperands();
	i2 = v2.getNumOfTargets();
	//JavaVariableComparator comparator = new JavaVariableComparator();
	for  (i = 0; i < i1; i++) {
	    for (j = 0; j < i2; j++) { 
		if (v1.getOperand(i).compareTo(v2.getTarget(j)) == 0) {
		    v1.addDefOfOperand(i, v2.getBpc());
		    //System.out.println(v2 + " is a Def. which reaches to " + v1 + ".");
		    //System.out.println(v1 + " is a Use. which used a Def. from " + v2 + ".");
		    v2.addUseOfTarget(j, v1.getBpc());
		}
	    }
	}
    }

    public void solveLiveVariables() {
	TreeSet ChangeNodes = new TreeSet();
        Iterator it;
        TreeSet oldOut;
        TreeSet USE, DEF;
        int i;
        int bpc;
        IR ir;
        TreeSet liveIN[];
        TreeSet liveOUT[];
        liveIN = new TreeSet[codeSize];
        liveOUT = new TreeSet[codeSize];

        for (i = 0; i < codeSize; i++) {
            liveIN[i] = new TreeSet();
            liveOUT[i] = new TreeSet();
        }

        it = cfg.iterator();
	while (it.hasNext()) {
	    ir = (IR) it.next();
	    ChangeNodes.add(ir);
	}
         
	IR code, succ, pred;
	while (ChangeNodes.size() > 0) {
	    code = (IR) ChangeNodes.last();
	    bpc = code.getBpc();
	    ChangeNodes.remove(code);

	    for (i = 0; i < code.getSuccNum(); i++) {
		succ = code.getSucc(i);
		liveOUT[bpc].addAll(liveIN[succ.getBpc()]);
	    }
	    for (i = 0; i < code.getImplicitSuccNum(); i++) {
		succ = code.getImplicitSucc(i);
		liveOUT[bpc].addAll(liveIN[succ.getBpc()]);
	    }

            USE = new TreeSet();
            DEF = new TreeSet();

	    // We are interested in reference variables only.
            if (code.hasAttribute(IRAttribute.ELIMINATED) == false) {
                for (i = 0; i < code.getNumOfOperands(); i++) {
                    if (code.getOperand(i).getType() == TypeInfo.TYPE_REFERENCE) {
                    //if (code.getOperand(i).getType() != TypeInfo.TYPE_INT
                    //        && code.getOperand(i).getType() != TypeInfo.TYPE_FLOAT) {
                        USE.add(code.getOperand(i));
                    }
                }
                for (i = 0; i < code.getNumOfTargets(); i++) {
                    if (code.getTarget(i).getType() == TypeInfo.TYPE_REFERENCE) {
                    //if (code.getTarget(i).getType() != TypeInfo.TYPE_INT
                    //        && code.getTarget(i).getType() != TypeInfo.TYPE_FLOAT) {
                        DEF.add(code.getTarget(i));
                    }
                }
            }
	    oldOut = liveIN[bpc];
            
            /* IN[i] = USE[i] U (OUT[i] - DEF[i]) */
	    liveIN[bpc] = (TreeSet) liveOUT[bpc].clone();
            liveIN[bpc].removeAll(DEF);
	    liveIN[bpc].addAll(USE);
	    
	    if (!oldOut.equals(liveIN[bpc])) {
		for (i = 0; i < code.getPredNum(); i++) {
		    pred = code.getPred(i);
		    ChangeNodes.add(pred);
		}
		for (i = 0; i < code.getImplicitPredNum(); i++) {
		    pred = code.getImplicitPred(i);
		    ChangeNodes.add(pred);
		}
	    }
        }
        
        it = cfg.iterator();
    	while (it.hasNext()) {
	    ir = (IR) it.next();
            ir.setLiveVariables(liveOUT[ir.getBpc()]);
            String debugInfo = "live OUT:";
            debugInfo += liveOUT[ir.getBpc()];
            ir.addDebugInfoString(debugInfo);
	}
    }
}
