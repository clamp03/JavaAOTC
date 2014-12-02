package aotc.optimizer;

import aotc.share.*;
import aotc.cfg.*;
import aotc.ir.*;
import opcodeconsts.*;
import java.util.*;

/**
 * Eliminates reference copies(aloads and astores) in each method.
 * Our AOTC uses ObjectCell pointer and ObjecICell pointer at the same time (pair-wise way)
 * So Eliminating reference copies helps reduing the number of volatile variable accesses (ICell->ref_DONT_ACCESS)
 * 
 * Reference copy elimination uses UD-UD chains which are made from {@link DataFlowSolver}.
 *
 * There are two-type of elimination,
 * <ul>
 * <li> aloads elimination
 * <li> astore elimination
 * </ul>
 * <p>
 * Aloads can be eliminated when one or more uses share single def(which is aload)
 * In this case, the operand of uses can be replaced by the target of aload.
 * For example, let us assume following codes
 * 
 * aload_1 (s1_ref = l1_ref)
 * dup     (s2_ref = s1_ref)
 * s2_int = foo(s2_ref)
 * s1_int = boo(s1_ref, s2_int)
 * 
 * After aloads are eliminated the codes would be 
 *
 * #aload_1 (eliminated)
 * dup (s2_ref = l1_ref)
 * s2_int = foo(s2_ref)
 * s1_int = boo(s2_ref, s2_int)
 *
 * In fact, reference copy elimination is processed after dup elimination.
 * So the result is as followings
 * 
 * #aload_1 (eliminated)
 * #aload   (eliminated)
 * s2_int = foo(l1_ref)
 * s1_int = boo(l1_ref, s2_int)
 * <p>
 * Astores can be eliminated. Let us look at the following codes which is typical use of astore
 * 
 * s0_ref = agetfield
 * astore1 (l1_ref = s0ref)
 * 
 * these can be optimized as followings
 *
 * l1_ref = agetfield
 * asotre1 (eliminated)
 *
 * @author Park Jong-Kuk
 */

public class CopyEliminator {
    private CFG cfg;

    /**
     * Constructor
     * 
     * @param cfg   control flow graph to be applied having UD-DU chain information
     */
    public CopyEliminator(CFG cfg) {
        this.cfg = cfg;
        //System.out.println(cfg.getMethodInfo().name);
    }

    /**
     * Driver for reference copy elimination
     *
     */
    public void doElimination() {
        IR ir;
	IR defCode;
	IR useCode;
        int i, j, countOperand;
        JavaVariable v1, v2;
        short shortOpcode;
        short shortOpcode2;
        boolean singleDefCondition;
        BitSet[] reachingDef = cfg.getReachingDef();

        Iterator it = cfg.iterator();
	while(it.hasNext()) {
            ir = (IR) it.next();
            //System.out.println(ir);
            countOperand = ir.getNumOfOperands();
            if (ir.hasAttribute(IRAttribute.ELIMINATED)) {
                // Removed code can't remove other code
                continue; 
            }
            // eliminate single def. single use. (aload)
            for (i = 0; i < countOperand; i++) {
                if (ir.getNumOfDefs(i) ==1 && ir.getShortOpcode() != OpcodeConst.opc_areturn) {   // single def
                    defCode = cfg.getIRAtBpc (ir.getDefOfOperand(i, 0));
                    if (defCode.hasAttribute(IRAttribute.ELIMINATED)) {
                        // Eliminated code can't remove other codes
                        continue;
                    } 
	            shortOpcode= defCode.getShortOpcode();
                    if (shortOpcode == OpcodeConst.opc_aload ||
                                (shortOpcode >= OpcodeConst.opc_aload_0 && shortOpcode <= OpcodeConst.opc_aload_3)) {
                        singleDefCondition = false;
                        for (j = 0; j < defCode.getNumOfUses(0); j++) {
                            singleDefCondition = true;
                            // if the def is the only def for every the uses it can reach
                            useCode = cfg.getIRAtBpc(defCode.getUseOfTarget(0, j));
                            if (useCode.getNumOfDefs(defCode.getTarget(0)) != 1) {
                                singleDefCondition = false;
                                break;
                            }
                            if (cfg.getIRAtBpc(useCode.getDefOfOperand(defCode.getTarget(0), 0)).compareTo(defCode) != 0) {
                                singleDefCondition = false;
                                break;
                            }
                            // For dup
                            // The Reaching Def of the operand of the copy and the Reaching Def of the operand of the useCode
                            // should be the same.
                            int reachToCopyCode, reachToUseCode;
                            reachToCopyCode = getUniqueReachOfVariable(reachingDef[defCode.getBpc()], defCode.getOperand(0));
                            reachToUseCode = getUniqueReachOfVariable(reachingDef[useCode.getBpc()], defCode.getOperand(0));
                            if (reachToCopyCode != reachToUseCode) {
                                singleDefCondition = false;
                                break;
                            }
                        }
                        if (singleDefCondition == true) {
                            for (j = 0; j < defCode.getNumOfUses(0); j++) {
                                useCode = cfg.getIRAtBpc(defCode.getUseOfTarget(0, j));
                                if (useCode.hasAttribute(IRAttribute.ELIMINATED) 
                                    /*|| defCode.hasAttribute(IRAttribute.ELIMINATED)*/)  {
                                    continue;
                                }
                                //System.out.println("useCode:" + useCode + " defCode:" + defCode);
                                //System.out.println("Try to change usecode's operand from " + defCode.getTarget(0) +
                                //                    " to " + defCode.getOperand(0));
                                useCode.changeOperand(defCode.getTarget(0), defCode.getOperand(0));
                                defCode.setAttribute(IRAttribute.ELIMINATED);
                                defCode.addDebugInfoString("Eliminated by BPC:" + ir.getBpc());
                            }
                        }
                    }
                }
            }

            // elimination of single use. single def. (astore)
            if (ir.getNumOfTargets() == 1) {
                if (ir.getNumOfUses(0) == 1) {
                    v1 = ir.getTarget(0);
                    useCode = cfg.getIRAtBpc(ir.getUseOfTarget(0, 0));
                    if (useCode.hasAttribute(IRAttribute.ELIMINATED)) {
                        // Removed code can't remove other code
                        continue; 
                    }
                    countOperand = useCode.getNumOfOperands();
                    for (i = 0; i < countOperand; i++) {
                        v2 = useCode.getOperand(i);
                        if ((v1.compareTo(v2) == 0) && (useCode.getNumOfDefs(i) == 1)) {
	                    //shortOpcode=CFG.makeShortVal((byte)0,useCode.getBytecode().getOpcode());
                            shortOpcode = useCode.getShortOpcode();
                            if (shortOpcode == OpcodeConst.opc_astore
                                || (shortOpcode >= OpcodeConst.opc_astore_0 && shortOpcode <= OpcodeConst.opc_astore_3)) {
                                //System.out.println(useCode + " can be absorbed by " + ir);
                                // 1. substitute the target of bytecode for the target of astore.
                                ir.changeTarget(0, useCode.getTarget(0));
                                // 2. and remove the astore operation
                                useCode.setAttribute(IRAttribute.ELIMINATED);
                                useCode.addDebugInfoString("Eliminated by BPC:" + ir.getBpc());
                            }
                        }
                    }
                }
            } 
        }

    }
    private int getUniqueReachOfVariable(BitSet reachingDef, JavaVariable operand) {
        int size = reachingDef.size();
        int i;
        IR ir;
        for (i = 0; i < size; i++) {
            if (reachingDef.get(i) == false) {
                continue;
            }
            ir = cfg.getIRAtBpc(i);
            if (ir.getNumOfTargets() == 0) continue;
            if (ir.getTarget(0).compareTo(operand) == 0) {
                return i;
            }
            
        }
        return -1;
    }
}
