package aotc.cfg;

import java.util.*;
import aotc.ir.*;
import aotc.share.*;
import opcodeconsts.*;

/**
 * LoopFinder finds loops of given cfg.
 *
 * LoopFinder uses general algorithm to find loops. 
 * The result are marks as attribute using {@link IRAttribute}.
 * <ul>
 * <li>loop headers are marked as IRAttribute.LOOP_HEADER
 * <li>loop bodys are marked as IRAttribute.LOOP_BODY
 * <li>loop tails are marked as IRAttribute.LOOP_TAIL
 * <li>loop preheaders are marked as IRAttribute.LOOP_PREHEADER
 * </ul>
 *
 * If a IR has LOOP_HEADER attribute and LOOP_BODY attribute at the
 * same time this means that the IR is the LOOP_HEADER of nested loop.
 *
 * After finding loops, loop preheaders(nop) are generated at every loop header.
 *
 */
class LoopFinder {
    private CFG cfg;
    private int codeSize;
    
    /** 
     * Constructs LoopFinder with given CFG
     * @param cfg CFG to be analyzed by LoopFinder
     */
    public LoopFinder(CFG cfg) {
        this.cfg = cfg;
        codeSize = cfg.getCodeSize(); //getMethodInfo().code.length;
        //System.out.println(cfg.getClassInfo().className);
        //System.out.println(cfg.getMethodInfo().name);
    }
    
    /**
     * Finds loops and generate preheader.
     *
     * To find loops, first, constructs DFS-spanning tree.
     * And find retreating edges on that tree. Since there is no irreducible
     * loops in Java classfile, all retreating edges are back-edges.
     * (In fact, we don't need to find dominators for that reason)
     *
     * At next, collects all loop headers together. This is required because
     * one loop has more than one back edges. (Finally, one loop header is corresponded
     * to one loop)
     *
     * At next, find natural for each loop headers.
     *
     * Finally, loop preheaders are generated.
     * This is somewhat complex because Java classfile has some weird loop structure.
     * (For example, fall through back-edge.)
     *
     */ 
    public void findLoops() {
        // We don't need to find dominators since all retreating edges are back-edges

        // DFS traversal of the CFG
        BitSet visited = new BitSet(codeSize);
        Stack stack = new Stack();
        Stack stackPrevVisited = new Stack();
        //Vector visitResult = new Vector();
        Vector outgoingEdge[] = new Vector[codeSize];
        Vector incomingEdge[] = new Vector[codeSize];
        TreeSet backEdgeList = new TreeSet();
        Integer bpc;
        //int oldBpc = -1;
        int succ;
        IR ir;
        int i;
      
        for (i = 0; i < codeSize; i++) {
            outgoingEdge[i] = new Vector();
            incomingEdge[i] = new Vector();
        }
      
        // Add start bpc of each handlers to stack
        // If not, they will not be traversed
        Iterator it = cfg.iterator();
        while (it.hasNext()) { 
            ir = (IR) it.next();
            if (ir.hasAttribute(IRAttribute.BB_HANDLER)) {
                stack.push(new Integer(ir.getBpc()));
                stackPrevVisited.push(new Integer(-1));
            }
    
        }
        stack.push(new Integer(0));
        stackPrevVisited.push(new Integer(-1));

        int previouslyVisited = -1;
        // DFS traverse
        while (stack.size() > 0) {
            bpc = (Integer) stack.pop();
            previouslyVisited = ((Integer)stackPrevVisited.pop()).intValue();
            if (visited.get(bpc.intValue()) == false) { // unmarked
                visited.set(bpc.intValue()); // mark as visited
                if (previouslyVisited != -1) {
                    outgoingEdge[previouslyVisited].add(bpc);
                    incomingEdge[bpc.intValue()].add(new Integer(previouslyVisited));
                    //System.out.println(previouslyVisited + "->" + bpc);
                }
                //visitResult.add(bpc);
                ir = cfg.getIRAtBpc(bpc.intValue());
                for (i = 0; i < ir.getSuccNum(); i++) {
                    succ = ir.getSucc(i).getBpc();
                    if (visited.get(succ) == false) {
                        stack.push(new Integer(succ));
                        stackPrevVisited.push(bpc);
                        //DFSpanningTree.add(new Edge(bpc.intValue(), succ));
                    } 
                }
//                previouslyVisited = bpc.intValue();
            }
        }

        it = cfg.iterator();
        int irBpc;
        int predBpc;
        Integer temp;
        while (it.hasNext()) { 
            ir = (IR) it.next();
            irBpc = ir.getBpc();
            for (i = 0; i < ir.getSuccNum(); i++) {
                succ = ir.getSucc(i).getBpc(); 
                
                // Track DFS edges and check whether a edges is from desc. to pred. of DFS.
                // If then, that is a backedges.
                predBpc = irBpc;
                while (incomingEdge[predBpc].size() > 0) {
                    new aotc.share.Assert(incomingEdge[predBpc].size() <= 1, "incomingEdge has more than 1 edge-_-");
                    temp = (Integer) incomingEdge[predBpc].elementAt(0);
                    if (temp.intValue() == succ) {
                        //if (!cfg.getIRAtBpc(succ).hasAttribute(IRAttribute.FINALLY_BLOCK)) { // entry of finally block can't be a loop header.
                            //System.out.println("find back edges:" + ir.getBpc() + "->" + succ);
                            ir.setAttribute(IRAttribute.LOOP_TAIL);
                            ir.setLoopHeader(succ);
                            backEdgeList.add(new Edge(ir.getBpc(), succ));
                            //System.out.println("succ : " + succ);
                            cfg.getIRAtBpc(succ).setAttribute(IRAttribute.LOOP_HEADER);
                        //}
                        break;
                    } 
                    predBpc = temp.intValue(); // track into successor
                }
            }
        }

        // Find Natural Loops
        it = backEdgeList.iterator();
        Edge backEdge;
        int pred;
        int headerBpc, tailBpc;
        // find nodes that can reach tail without bypassing header
        while (it.hasNext()) {
            backEdge = (Edge) it.next();
            headerBpc = backEdge.to;
            tailBpc = backEdge.from;
            stack = new Stack();
            visited = new BitSet(codeSize);
            stack.add(new Integer(tailBpc));
            while (stack.size() > 0) {
                bpc = (Integer) stack.pop();
                visited.set(bpc.intValue());
                ir = cfg.getIRAtBpc(bpc.intValue());
                ir.setAttribute(IRAttribute.LOOP_BODY);
                ir.setLoopHeader(headerBpc);
                for (i = 0; i < ir.getPredNum(); i++) {
                    pred = ir.getPred(i).getBpc();
                    if (pred != headerBpc && visited.get(pred) == false)  {
                        stack.push(new Integer(pred));
                    }
                }
    
            }
            
        }
        // Generate Loop Preheader
        IR predIR, headerIR, preheaderIR;
        IR gotoIR, tailIR;
        Bytecode preheaderBytecode, gotoBytecode;
        int loopIndex;

        TreeSet headerList = new TreeSet();
        it = backEdgeList.iterator();
        //System.out.println("backedges: " + backEdgeList.size());
        while (it.hasNext()) {
            backEdge = (Edge) it.next();
            headerBpc = backEdge.to; // LOOP_HEADER
            tailBpc = backEdge.from; // LOOP_TAIL
            if (headerList.contains(new Integer(headerBpc))) {
                continue; // one preheader per one loop : some loops have mare than one backedges
            }
            headerList.add(new Integer(headerBpc));
            //System.out.println("BackEdge from " + backEdge.from + " to " + backEdge.to);
            headerIR = cfg.getIRAtBpc(headerBpc);

            // Allocate preheader IR
            preheaderBytecode = new Bytecode(cfg.allocateNewBpc(), (byte)OpcodeConst.opc_nop, new int[0]);
            preheaderIR = new IR(preheaderBytecode);
	    preheaderIR.setOriginalBpc(headerIR.getOriginalBpc());
            preheaderIR.setAttribute(IRAttribute.LOOP_PREHEADER);
	    //set loop header
	    for(i = 0 ; i < headerIR.getLoopHeaderNum() ; i++) {
		preheaderIR.setLoopHeader(headerIR.getLoopHeader(i));
	    }
            headerIR.setLoopPreheader(preheaderIR);
            
            // Add preheader to IR List
            cfg.addNewIR(preheaderIR, headerIR, CFG.ADD_IN_FRONT_OF);
            //System.out.println("Add " + preheaderIR + " as a preheader of " + headerIR);
            preheaderIR.addSucc(headerIR);
	    
            // Add preheader to CFG
            for (i = 0; i < headerIR.getPredNum(); i++) {
                predIR = headerIR.getPred(i);
                if (backEdgeList.contains(new Edge(predIR.getBpc(), headerIR.getBpc())) == false) {
                    predIR.changeSucc(headerIR, preheaderIR);
                    headerIR.removePred(predIR);
                    preheaderIR.addPred(predIR);
                }
            }
            headerIR.addPred(preheaderIR);

            // make goto to skip loop-preheader when there isn't explicit backedge
            // (there are some backedge(s) which is just fallthrough)
            tailIR = cfg.getIRAtBpc(tailBpc);
            if ((tailIR.isUnconditionalBranch() == false && tailIR.isConditionalBranch() == false)
                    || (tailIR.isConditionalBranch() == true && tailIR.getFallthruSucc() == headerIR)) {
                // allocate new goto IR
                int operands[] = new int[1];
                int newBpc = cfg.allocateNewBpc();
                operands[0] = headerBpc - newBpc;
                gotoBytecode = new Bytecode(newBpc, (byte)OpcodeConst.opc_goto, operands);
                gotoIR = new IR(gotoBytecode);
		gotoIR.setOriginalBpc(tailIR.getOriginalBpc());
		//set loop heaer
		for(i = 0 ; i < tailIR.getLoopHeaderNum() ; i++) {
		    gotoIR.setLoopHeader(tailIR.getLoopHeader(i));
		}
		
                        
                // add new IR to CFG
                cfg.addNewIR(gotoIR, tailIR, CFG.ADD_AFTER);
                //System.out.println("Add " + gotoIR + " after " + tailIR);
                if (tailIR.isConditionalBranch() == true) {
                    IR tempIR = tailIR.getFallthruSucc();
                    tailIR.changeSucc(tailIR.getFallthruSucc(), gotoIR);
                    tempIR.changePred(tailIR, gotoIR);
                } else {
                    IR tempIR = tailIR.getSucc(0);
                    tailIR.changeSucc(tailIR.getSucc(0), gotoIR);
                    tempIR.changePred(tailIR, gotoIR);
                }
                gotoIR.addPred(tailIR);
                gotoIR.addSucc(headerIR);
                gotoIR.setAttribute(IRAttribute.LOOP_TAIL);
                tailIR.removeAttribute(IRAttribute.LOOP_TAIL);
            }

            // Make goto label for preheader
            IR tempIR, tempTarget;
//            if (preheaderIR.needGotoLabel()) {
                preheaderIR.setAttribute(IRAttribute.BB_START);
//            }
//            if (!headerIR.needGotoLabel()) {
//                headerIR.removeAttribute(IRAttribute.BB_START);
//            }
            if (headerIR.hasAttribute(IRAttribute.LOOP_BODY)) {
                preheaderIR.setAttribute(IRAttribute.LOOP_BODY);
            }

        }
    }

}

/**
 * Edge is used to find back-edges of CFG(Control flow graph).
 * This class is currently used only for finding loops
 *
 */
class Edge implements Comparable {
    int from;
    int to;
    
    /**
     * Constructs new edge.
     * @param from the bpc where the edge starts from
     * @param to the bpc where the edge end
     */
    public Edge(int from, int to) {
        this.from = from;
        this.to = to;
    }

    public int compareTo(Object o) {
        Edge e1 = this;
        Edge e2 = (Edge) o;
        if (e1.from == e2.from) {
            return (e1.to - e2.to);
        } else {
            return (e1.from - e2.from);
        }
    }
}
