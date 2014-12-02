package aotc.optimizer;

import aotc.cfg.*;
import aotc.ir.*;
import java.util.*;

/**
 * GCCheckEliminator eliminates unnecessary (duplicated) GC-CheckPoint at loop-backedge.
 * If all paths coming into loop-backedge has gone through GC-point, we don't need to provide for
 * GC-CheckPoint.
 */

public class GCCheckEliminator {
    private CFG cfg;
    private int codeSize;
    private boolean[] isChecked;

    public GCCheckEliminator(CFG cfg) {
        this.cfg = cfg;
        codeSize = cfg.getCodeSize();
    }

    public void eliminate() {
        Iterator it;
        IR ir;
        int bpc;
        
        solveIfChecked();
        
        it = cfg.iterator();
        while (it.hasNext()) {
           ir = (IR) it.next();
           bpc = ir.getBpc();
           if (ir.hasAttribute(IRAttribute.LOOP_TAIL) 
                    && isChecked[bpc] == true) {
               ir.setAttribute(IRAttribute.REDUNDANT_GC_CHECK);
               ir.addDebugInfoString("HAS REDUNDANT_GC_CHECK");
           }
        }
    }
    private void solveIfChecked() {
        Iterator it;
        IR ir;
        int bpc;
        TreeSet ChangeNodes = new TreeSet();
        boolean IN_Checked[] = new boolean[codeSize];
        boolean OUT_Checked[] = new boolean[codeSize];
        boolean oldOUT;

	// initialize
        IN_Checked = new boolean[codeSize];
        OUT_Checked = new boolean[codeSize];
        for (int i = 0; i < codeSize; i++) {
            IN_Checked[i] = false;
            OUT_Checked[i] = false;
        }
        
        // adds loop headers to ChangeNode
        it = cfg.iterator();
        while (it.hasNext()) {
           ir = (IR) it.next();
           //if (ir.hasAttribute(IRAttribute.LOOP_HEADER)) {
           ChangeNodes.add(ir);
           //}
        }

        while (ChangeNodes.size() > 0) {
            ir = (IR) ChangeNodes.first();
            ChangeNodes.remove(ir);
            bpc = ir.getBpc();
            oldOUT = OUT_Checked[bpc];  
           
            if (ir.provideGCCheckPoint()) { // if ir is a potential_gc_point
                // sets OUT_Checked as true
                OUT_Checked[bpc] = true;
            } else {
                // computes IN_Checked and propagates it as OUT_Checked
                if (ir.hasAttribute(IRAttribute.LOOP_HEADER)) {
                    IN_Checked[bpc] = false;
                } else {
                    IN_Checked[bpc] = true; 
                    for (int i = 0; i < ir.getPredNum(); i++) {
                        IN_Checked[bpc] &= OUT_Checked[ir.getPred(i).getBpc()];
                    }
                }
                OUT_Checked[bpc] = IN_Checked[bpc];
            }
            ir.addDebugInfoString("GC_CHECKED = " + (OUT_Checked[bpc] ? "true" : "false"));
            if (oldOUT != OUT_Checked[bpc]) {
                for(int i = 0; i < ir.getSuccNum(); i++ ) {
                    ChangeNodes.add(ir.getSucc(i));
                }
                if (oldOUT == true && OUT_Checked[bpc] == false) {
                    System.out.println("Error At REDUNDANT GC_Check Eliminator. This can result in infinite loop.");
                }
            }
        } // end of while

        // Save the result into isChecked, a instance variable.
        isChecked = OUT_Checked;
    }
}
