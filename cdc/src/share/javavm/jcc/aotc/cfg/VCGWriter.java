package aotc.cfg;

import java.io.*;
import aotc.ir.*;
import aotc.translation.*;
import java.util.*;

public class VCGWriter {
	private String filename;
	private File vcgfile;
	private FileWriter out;
        private CFG cfg;
	//private int bb_id;
        private int nodeRank;
	
	public VCGWriter(CFG cfg) {
		nodeRank = 1;
                this.cfg = cfg;
	}
	
	public void generateVCG() {
		//Bytecode node[] = cfg.getBlock();
                filename = "vcg/" /* + cfg.getClassInfo().getGenericNativeName()
                                        + */ + cfg.getMethodInfo().getNativeName(true) + ".vcg";

                try {
                    vcgfile = new File(filename);
        	    out = new FileWriter(vcgfile);
		
		    // print heading
		    out.write("graph: { title: \"AOTC_CFG_GRAPH\"\n");
		    out.write("layoutalgorithm: dfs\n");
		    //out.write("x: 150\n");
		    //out.write("y: 30\n");
		    out.write("width: 1000\n");
		    out.write("height: 900\n");
                    out.write("display_edge_labels:yes\n");
                    out.write("manhatten_edges:yes\n");
                    out.write("layout_nearfactor: 0\n");
                    out.write("layout_downfactor: 100\n");
                    out.write("layout_upfactor: 0\n"); 
                    //out.write("xspace: 25\n\n");
                    out.write("classname 1 : \"CFG Edges (blue)\"\n");
                    out.write("classname 2 : \"Operands (red)\"\n");
                    out.write("classname 3 : \"Targets (green)\"\n");
                    out.write("classname 4 : \"UseDef (grey)\"\n");
	
		    // print body
                    Iterator it = cfg.iterator();
		    while (it.hasNext()) 
                    {
		        drawNode((IR) it.next());
		    }

		// print tail
		out.write("}\n");
		out.close();
                } catch (IOException e) {
                    System.err.println("Cannot generate VCG for " + filename);
                    return;
                }
	}
	
	private void drawNode(IR code) throws IOException {
		out.write("node: { title:\"bpc" + code.getBpc() + "\"\n");
                out.write("        vertical_order: " + (nodeRank++) + "\n");
		out.write("        label:\"");
		out.write(code.toString() +/* code.getDebugInfoString() +*/ /*"LOOP DEPTH=" + code.getIR().getLoopDepth() +*/ "\"\n");
                if (code.hasAttribute(IRAttribute.ELIMINATED)) {
                    out.write("        color:white }\n");
		} else if (code.hasAttribute(IRAttribute.NULLCHECKELIMINATED)) {
		    out.write("        color:yellow }\n");
                } else if (code.hasAttribute(IRAttribute.LOOP_HEADER) || code.hasAttribute(IRAttribute.LOOP_TAIL)) {
                    out.write("        color:darkmagenta }\n");
                } else if (code.hasAttribute(IRAttribute.LOOP_BODY)) {
		    out.write("        color:lightmagenta }\n");
                } else {
                    out.write("        color:lightyellow }\n");
                }

		for(int i=0; i<code.getSuccNum(); i++) {
			drawEdge(code, code.getSucc(i));
		}
                drawIRInfo(code);
	}
        private void drawIRInfo(IR ir) throws IOException {
                String operandsColor = "red";
                String targetsColor = "green";
                String useDefColor = "lightgrey";
                int i, j;
                if (ir.getOperandListString().length() > 0) {
                    out.write("node: { title:\"operands" + ir.getBpc() + "\"\n");
                    out.write("        label:\"" + ir.getOperandListString() + "\"\n");
                    out.write("        color:" + operandsColor + "}\n");
    		    out.write("nearedge: { ");
    		    out.write("targetname:\"bpc" + ir.getBpc() + "\" ");
		    out.write("sourcename:\"operands" + ir.getBpc() + "\" class: 2 color:" + operandsColor + "}\n");
                    for (i = 0; i < ir.getNumOfOperands(); i++) {
                        for (j = 0; j < ir.getNumOfDefs(i); j++) {
                            out.write("edge: { targetname: \"operands" + ir.getBpc() + "\"");
                            out.write(" sourcename: \"targets" + ir.getDefOfOperand(i, j) + "\"");
                            out.write(" class: 4 color:" + useDefColor + "}\n");
                        }
                    }
                }
                
                if (ir.getTargetListString().length() > 0) {
                    out.write("node: { title:\"targets" + ir.getBpc() + "\"\n");
                    out.write("        label:\"" + ir.getTargetListString() + "\"\n");
                    out.write("        color:" + targetsColor + "}\n");
    	    	    out.write("nearedge: { ");
		    out.write("sourcename:\"bpc" + ir.getBpc() + "\" ");
		    out.write("targetname:\"targets" + ir.getBpc() + "\" class: 3 color:" + targetsColor + "}\n");
                } 

        }
        
	private void drawEdge(IR from, IR to) throws IOException {
                //if (to.getBpc() < from.getBpc()) {
                if (from.hasAttribute(IRAttribute.LOOP_TAIL) && to.hasAttribute(IRAttribute.LOOP_HEADER)) {
		    out.write("backedge: { thickness:5 color:blue ");
                } else {
    		    out.write("edge: { thickness:3 ");
                }
		out.write("sourcename:\"bpc" + from.getBpc() + "\" ");
		out.write("targetname:\"bpc" + to.getBpc() + "\" class: 1}\n");
	}
}
