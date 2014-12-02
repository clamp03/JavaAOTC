package aotc.ir;

/**
 * The class that defines the constants that are used to identify IR attribute
 * <p>
 * @author Park Jong-Kuk
 */
 
public class IRAttribute {
    /**
     * basic block starting point attribute
     */
    public static final int BB_START                = 0x0001;
    /**
     * basic block starting point attribute
     */
    public static final int BB_HANDLER              = 0x0002;
    /**
     * This represents copy is eliminated
     * @see CopyEliminator
     */
    public static final int ELIMINATED              = 0x0004;
    /**
     * This represents null check is elimiated
     * @see NullCheckEliminator
     */
    public static final int NULLCHECKELIMINATED     = 0x0008;
    /**
     * Header of loop
     * @see LoopFinder
     */
    public static final int LOOP_HEADER             = 0x0010;
    /**
     * Tail of loop
     * @see LoopFinder
     */
    public static final int LOOP_TAIL               = 0x0020;
    /**
     * Body of loop
     * @see LoopFinder
     */
    public static final int LOOP_BODY               = 0x0040;
    /**
     * Preheaher of loop
     * @see LoopFinder
     */
    public static final int LOOP_PREHEADER          = 0x0080;
    /**
     * It represents this bytecode is preloaded
     * @see Preloader
     */
    public static final int PRELOADED               = 0x0100;
    
    /**
     * It represents if garbage collecting can occur at this bytecode
     */
    public static final int POTENTIAL_GC_POINT      = 0x0200;
   
    /**
     * It represents if this byte code need to write ObjectICell as well as ObjectCell
     */
    public static final int NEED_ICELL              = 0x0400;
    /**
     * It represents if this byte code need to write ObjectICell as well as ObjectCell
     */
    public static final int REDUNDANT_GC_CHECK      = 0x0800;
    /**
     * It represents if this byte code need to write ObjectICell as well as ObjectCell
     */
    public static final int REDUNDANT_CLASSINIT      = 0x1000;
    
    public static final int ARRAYNEGATIVESIZECHECKELIMINATED	= 0x4000;
    
    public static final int ARITHMETICCHECKELIMINATED		= 0x8000;

    public static final int ARRAYSTORECHECKELIMINATED		= 0x10000;

    public static final int CLASSCASTCHECKELIMINATED		= 0x20000;
    
    public static final int ARRAYBOUNDCHECKELIMINATED		= 0x40000;
   //    public static final int BB_TRY
    
    /**
     * ID of basic block that identify basic block
     */
    public static final int BB_ID                   = 0x0000;
    /**
     * ID of handler that identify handler
     */
    public static final int HANDLER_ID              = 0x0001;
    
    /**
     * attribute contains ir attribute
     */
    int attribute;
    // following array number is the number of above attributes.
    
    /**
     * this contains basic block id and handler id
     */
    private int attr_value[] = new int [2];
    
    /**
     * Gets attribute that is basic block id or handler id
     *
     * @return basic block id or handler id
     */
    public int getAttribute(int i) {
	return attr_value[i];
    }
    
    /**
     * Sets attribute that is basic block id or handler id
     *
     * @param i determines whether block id or handler id
     * @param value basic block id or handler id
     */
    public void setAttribute(int i, int value) {
	attr_value[i] = value;
    }
    
    /**
     * Gets string that is used for printing debugging info
     *
     * @return the value of handler id
     */
    public String getString() {
	String ret = "";
	if((attribute & BB_HANDLER)!=0) {
	    ret += (" (HANDLER #" + attr_value[HANDLER_ID] + ")");
	}
	return ret;
    }
}
