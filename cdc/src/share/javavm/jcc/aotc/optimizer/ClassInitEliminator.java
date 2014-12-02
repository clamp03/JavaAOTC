package aotc.optimizer;

import java.util.*;
import aotc.cfg.*;
import aotc.ir.*;
import opcodeconsts.*;
import aotc.share.*;

public class ClassInitEliminator {
    private CFG cfg;
    private int codeSize;

    private TreeSet IN[];

    public ClassInitEliminator(CFG cfg) {
	this.cfg = cfg;
        codeSize = cfg.getCodeSize();
        //System.out.println(cfg.getClassInfo().className);
        //System.out.println(cfg.getMethodInfo().name);
    }
    public void eliminates() {

        IR ir;
        Iterator it;
	TreeSet ChangeNodes = new TreeSet();
	TreeSet IN_CHECKED[] = new TreeSet[codeSize];
	TreeSet OUT_CHECKED[] = new TreeSet[codeSize];
        String GEN_CB[] = new String[codeSize];

	it = cfg.iterator();
	while(it.hasNext()) {
            ir = (IR) it.next();
            /************************************************************
             * null means not initialized, in other words, universal set.
             *************************************************************/
            OUT_CHECKED[ir.getBpc()] = null; 
	    ChangeNodes.add(ir);
        }
        OUT_CHECKED[0] = new TreeSet();
        
	int bpc;
        short shortOpcode;
	it = cfg.iterator();

        // Initialize GEN_CB, which store class block to be initialized at each bpc
	while(it.hasNext()) {
            ir = (IR) it.next();
            bpc = ir.getBpc();
            shortOpcode = ir.getShortOpcode();
            switch (shortOpcode) {
                case OpcodeConst.opc_invokestatic_checkinit_quick:
                case OpcodeConst.opc_aputstatic_checkinit_quick:
                case OpcodeConst.opc_putstatic_checkinit_quick:
                case OpcodeConst.opc_putstatic2_checkinit_quick:
                case OpcodeConst.opc_agetstatic_checkinit_quick:
                case OpcodeConst.opc_getstatic_checkinit_quick:
                case OpcodeConst.opc_getstatic2_checkinit_quick:
                case OpcodeConst.opc_new_quick:
                    GEN_CB[bpc] = ir.getClassBlockToInit();
                    break;
                default:
                    GEN_CB[bpc] = null;
                    break;
            }
	    ChangeNodes.add(ir); // Adds to worklist
        }

        IR predIR; 
        TreeSet predOut;
       
        // Data-flow analysis

        // You can check whether it works well by inspecting db_Main_inst_1main.vcg의 bpc 47과 48에서 할 수 있음.
	while (ChangeNodes.size() > 0) {
	    ir = (IR) ChangeNodes.first(); // Get first item of worklist
	    ChangeNodes.remove(ir); // remove the item from worklist
            //System.out.println(ir);
	    bpc = ir.getBpc();
            // compute IN_CHECKED (intersection of all predecessor's OUT_CHECKED)
            if (ir.getPredNum() == 0) {
                IN_CHECKED[bpc] = new TreeSet();
            } else {
                TreeSet tempSet = null;
                for (int i = 0; i < ir.getPredNum(); i++) {
                    tempSet = solveIntersect(tempSet, OUT_CHECKED[ir.getPred(i).getBpc()]);
                }
                IN_CHECKED[bpc] = tempSet;
            }

            // compute OUT_CHECKED
            TreeSet oldOUT = OUT_CHECKED[bpc];
            if (IN_CHECKED[bpc] != null) {
                OUT_CHECKED[bpc] = (TreeSet) IN_CHECKED[bpc].clone();
                if (GEN_CB[bpc] != null) {
                    OUT_CHECKED[bpc].add(GEN_CB[bpc]);
                }
                //System.out.println("OUT_CHECKED[" + bpc + "]=" + OUT_CHECKED[bpc]);
                // Checks whether the OUT_CHECKED is changed or not
                // If changed, add all successors to the worklist.
                if (oldOUT == null || oldOUT.size() != OUT_CHECKED[bpc].size()) {
                    for( int i = 0 ; i < ir.getSuccNum() ; i++ ) {
                        ChangeNodes.add(ir.getSucc(i));
                    }
                }
            }
        }
        
        // Find redundant ClassInitChecks
        it = cfg.iterator();
	while(it.hasNext()) {
            ir = (IR) it.next();
            bpc = ir.getBpc();
            shortOpcode = ir.getShortOpcode();
            ir.addDebugInfoString("IN CHECK: " + IN_CHECKED[bpc]);
            switch (shortOpcode) {
                case OpcodeConst.opc_invokestatic_checkinit_quick:
                case OpcodeConst.opc_aputstatic_checkinit_quick:
                case OpcodeConst.opc_putstatic_checkinit_quick:
                case OpcodeConst.opc_putstatic2_checkinit_quick:
                case OpcodeConst.opc_agetstatic_checkinit_quick:
                case OpcodeConst.opc_getstatic_checkinit_quick:
                case OpcodeConst.opc_getstatic2_checkinit_quick:
                case OpcodeConst.opc_new_quick:
                    if (GEN_CB[bpc] != null) { 
                        if (IN_CHECKED[bpc].contains(GEN_CB[bpc])) {
                            ir.addDebugInfoString("HAS REDUNDANT CLASSINIT CHECK");
                            ir.setAttribute(IRAttribute.REDUNDANT_CLASSINIT);
                            if (shortOpcode == OpcodeConst.opc_aputstatic_checkinit_quick
                                        || shortOpcode == OpcodeConst.opc_putstatic_checkinit_quick
                                        || shortOpcode == OpcodeConst.opc_putstatic2_checkinit_quick
                                        || shortOpcode == OpcodeConst.opc_agetstatic_checkinit_quick
                                        || shortOpcode == OpcodeConst.opc_getstatic_checkinit_quick
                                        || shortOpcode == OpcodeConst.opc_getstatic2_checkinit_quick) {
                                    // This should be done before ICellEliminator
                                    ir.removeAttribute(IRAttribute.POTENTIAL_GC_POINT);
                                }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
    /************************************************************
     * null means not initialized, in other words, universal set.
     *************************************************************/
    private TreeSet solveIntersect(TreeSet s1, TreeSet s2) {
        if (s1 == null) {
            if (s2 == null) {
                return null;
            } else {
                return (TreeSet) s2.clone();
            }
        } else {
            if (s2 == null) {
                return (TreeSet) s1.clone();
            }
        }
        //Then both s1, s2 are not null
        Iterator it = s1.iterator();
        String cb;
        TreeSet result = new TreeSet();
        while (it.hasNext()) {
            cb = (String) it.next();
            if (s2.contains(cb)) {
                result.add(cb);
            }   
        } 
        return result;
    }

}
