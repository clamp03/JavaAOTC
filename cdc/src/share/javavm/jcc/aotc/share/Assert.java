package aotc.share;

/**
 * Assert checks that program is working as intended
 * This is used for debugging
 * <p>
 * @author Bae SungHwan
 */
 
public class Assert {

    /**
     * Constructor specifying yes or no of error
     *
     * @param val the boolean to be tested
     */
    public Assert(boolean val) {
	if(!val) {
	    System.err.println("[assert] No message!!");
	    try {
		throw new Exception();
	    } catch(Exception e) {
		e.printStackTrace();
	    }
	    System.exit(-2);
	}
    }

    /**
     * Constructor specifying yes of no of error and error message
     * 
     * @param val the boolean to be tested
     * @param s error message
     */
    public Assert(boolean val, String s) {
	if(!val) {		
	    System.err.println("[assert] " + s);
	    try {
		throw new Exception();
	    } catch(Exception e) {
		e.printStackTrace();
	    }
	    System.exit(-1);
	}
    }
}
