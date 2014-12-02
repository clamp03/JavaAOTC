package aotc.optimizer;

import aotc.cfg.*;
import aotc.ir.*;
import aotc.share.*;
import java.util.*;
import opcodeconsts.*; 

public class ICellEliminator {
    private CFG cfg;
    private int size;

    private BitSet reachIN[];
    //private TreeSet liveOUT[];
    
    public ICellEliminator(CFG cfg) {
        this.cfg = cfg;
        this.size = cfg.getCodeSize();
    } 

    public void doElimination() {
        cfg.checkControlFlow();
        initialize();
        //solveReachingDef();
        reachIN = cfg.getReachingDef();
        //solveLiveVariable();
        //liveOUT = cfg.getLiveVariables();

        markNecessaryCodes();
        markCallArguments();
        //TODO
        //markLiveAtHandler();
    }

    private void markCallArguments() {
        DataFlowSolver dSolver = new DataFlowSolver(cfg);
        dSolver.makeUDAndDUChains();
        Iterator itCFG;

        int numOfUses;
        int i;
        IR useCode, ir;
       
        itCFG = cfg.iterator();
	while (itCFG.hasNext()) {
	    ir = (IR) itCFG.next();
            if (ir.getNumOfTargets() == 0) continue; // -> ??? JK
            if (ir.getShortOpcode() == OpcodeConst.opc_multianewarray_quick) {
                ir.setAttribute(IRAttribute.NEED_ICELL);
                ir.addDebugInfoString("set NEED_ICELL at " + ir.getBpc());
            }
            numOfUses = ir.getNumOfUses(0);
            for (i = 0; i < numOfUses; i++) {
                useCode = cfg.getIRAtBpc(ir.getUseOfTarget(0, i));
                if (useCode.needICell()) {
                    ir.setAttribute(IRAttribute.NEED_ICELL);
                    ir.addDebugInfoString("set NEED_ICELL at " + useCode.getBpc());
                }
            }       
        }
    }

    private void markNecessaryCodes() {
        Iterator itCFG;
        Iterator itLiveVariable;
        int bpc;
        JavaVariable liveVariable;
        int i;
        IR ir, def;
        
        itCFG = cfg.iterator();
	while (itCFG.hasNext()) {
	    ir = (IR) itCFG.next();
            if (ir.hasAttribute(IRAttribute.POTENTIAL_GC_POINT) == false) continue;
            bpc = ir.getBpc();
            //itLiveVariable = liveOUT[ir.getBpc()].iterator(); //use liveIN or liveOUT?????
            itLiveVariable = ir.getLiveVariables().iterator(); //use liveIN or liveOUT?????
            //itLiveVariable = liveIN[ir.getBpc()].iterator();
            while (itLiveVariable.hasNext()) {
                liveVariable = (JavaVariable) itLiveVariable.next();
                if (liveVariable.getType() != TypeInfo.TYPE_REFERENCE) continue;
                for (i = 0; i < size; i++) { // for each reaching def.
                    if (reachIN[bpc].get(i) && i!=bpc) {
                       def = cfg.getIRAtBpc(i);
                       if (def.getNumOfTargets() == 0) continue;
                       if ((def.getTarget(0).compareTo(liveVariable) == 0) && (ir.getNumOfTargets()==0 || ir.getTarget(0).compareTo(liveVariable)!=0)) {
                            def.setAttribute(IRAttribute.NEED_ICELL);
                            def.addDebugInfoString("set NEED_ICEEL at " + bpc);
                       }
                    } /*else if(i == bpc) {
                        // self reaching def?
                       def = cfg.getIRAtBpc(i);
                       if (def.getNumOfTargets() == 0) continue;
                       if (def.getTarget(0).compareTo(liveVariable) == 0) {
                            def.setAttribute(IRAttribute.NEED_ICELL);
                            def.addDebugInfoString("set NEED_ICEEL at " + bpc);
                       }
                    }*/ // We don't need to set NEED_ICELL to this. any more
                        // Since we added GCRestorePreserve()
                }
            }
	}
    }
    
    private void initialize() {
        int i;
        IR ir;

        Iterator it;
        int bpc1, bpc2;
        it = cfg.iterator();
        while (it.hasNext()) {
            ir = (IR) it.next();
            ir.removeAttribute(IRAttribute.NEED_ICELL); // turn-oned by default
        }
    }


}

