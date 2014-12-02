// -*- java -*-
//////////////////////////////////////////////////////////////////////
// StringFunctions.java
// Example user-defined functions
//
// (C) 1996 E.J.Friedman-Hill and Sandia National Labs
// TEST ONLY - NOT FOR DISTRIBUTION
//////////////////////////////////////////////////////////////////////
package spec.benchmarks._202_jess;



/**
  A User defined function interface. I suggest that this be used to
 implement multiple functions per subclass by using the 1st argument
 as a selector.

@author E.J. Friedman-Hill (C)1996
*/

public class StringFunctions implements Userfunction {

  int _name = RU.putAtom("string-function");

  public int name() {
    return _name;
  }
  
  public Value Call(ValueVector vv, Context context) throws ReteException {
    // These functions can be used like
    // (string-function cat "A" "B" "C")
    // (string-function upcase "om")

    String selector = vv.get(1).StringValue();
    
    if (selector.equals("cat")) {
      String s = vv.get(2).StringValue();
      for (int i=3; i < vv.size(); i++)
        s += vv.get(i).StringValue();
        
      return new Value(s, RU.STRING);

    } else if (selector.equals("upcase")) {
      return new Value(vv.get(2).StringValue().toUpperCase(), RU.STRING);

    } else if (selector.equals("downcase")) {
      return new Value(vv.get(2).StringValue().toLowerCase(), RU.STRING);

    } else
      throw new ReteException("StringFunctions::Call",
                              "Unimplemented function",
                              selector);
               
               


  }

}
