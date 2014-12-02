package aotc.share;

/**
 * Pair is used to represent pair-wise data set.
 * This can store a pair of object(object1 and object2)
 * and support getters for each object item.
 */
public class Pair {
    private Object a;
    private Object b;

    /**
     * Class constructor.
     * two objects are set to be null
     */
    public Pair() {
        a = null;
        b = null;
    }

    /** 
     * Class constructor specifying object pair
     * @param a object1
     * @param b object2
     */

    public Pair(Object a, Object b) {
        this.a = a;
        this.b = b;
    }

    /**
     * Getter for object1
     * @return object1
     */
    public Object getA() {
        return a;
    }
    /**
     * Getter for object2
     * @return object2
     */
    public Object getB() {
        return b;
    }
    

}
