package aotc.optimizer;

import java.util.*;
import aotc.share.*;
public class IndexBound {
    Vector lowBound = new Vector();
    Vector upperBound = new Vector();

    Vector lowOffset = new Vector();
    Vector upperOffset = new Vector();

    int attribute;

    public IndexBound() {
	attribute = 0;
    }
    public IndexBound(IndexBound bound) {
	attribute = 0;
	if(bound == null) {
	    return;
	}
	for(int i = 0 ; i < bound.sizeOfLB() ; i++) {
	    lowBound.add(bound.getLB(i));
	    lowOffset.add(new Integer(bound.getLOffset(i)));    
	}
	for(int i = 0 ; i < bound.sizeOfUB() ; i++) {
	    upperBound.add(bound.getUB(i));
	    upperOffset.add(new Integer(bound.getUOffset(i)));
	}
    }
    IndexBound(JavaVariable ub, JavaVariable lb, int uoffset, int loffset) {
	upperBound.add(ub);
	lowBound.add(lb);
	upperOffset.add(new Integer(uoffset));
	lowOffset.add(new Integer(loffset));
	attribute = 0;
    }
    public boolean isZero(Hashtable set, int i) {
	IndexBound bound = this;
	TreeSet list = new TreeSet();
	int offset = 0;
	while(true) {
	    if(i >= bound.sizeOfLB()|| bound.getLB(i) == null) return false;
	    if(i >= bound.sizeOfUB()|| bound.getUB(i) == null) return false;
	    if( bound.getLB(i).toString().equals(bound.getUB(i).toString()) && bound.getLOffset(i) == bound.getUOffset(i)) {
		Integer value;
		offset += bound.getLOffset(i);
		try {
		    value = new Integer(bound.getLB(i).toString());
		    if((value.intValue() + offset) == 0) {
			return true;
		    }else {
			return false;
		    }
		}catch (NumberFormatException e) {
		    list.add(bound.getLB(i));
		    bound = (IndexBound)set.get(lowBound);
		    if(bound == null || bound.getLB(i) == null ||list.contains(bound.getLB(i))) return false;
		}

	    }else {
		return false;
	    }
	}
    }   
	
    public boolean isPositive(Hashtable set, int i) {
	IndexBound bound = this;
	TreeSet list = new TreeSet();
	int offset = 0;
	while(true) {
	    if(i >= bound.sizeOfLB()|| bound.getLB(i) == null) return false;
	    JavaVariable lowBound = bound.getLB(i);
	    if(lowBound.toString().equals(ArrayBoundCheckEliminator.mInf.toString())) return false;
	    Integer value;
	    offset += bound.getLOffset(i);
	    try {
		value = new Integer(lowBound.toString());
		if((value.intValue()+offset) > 0) {
		    return true;
		}else {
		    return false;
		}
	    }catch(NumberFormatException e) {
		list.add(lowBound);
		bound = (IndexBound)set.get(lowBound);
		if(bound == null ||bound.getLB(i) == null || list.contains(bound.getLB(i))) return false;
	    }
	}
    }
    public boolean isNegative(Hashtable set, int i) {
	IndexBound bound = this;
	TreeSet list = new TreeSet();
	int offset = 0;
	while(true) {
	    if(i >= bound.sizeOfUB()|| bound.getUB(i) == null) return false;
	    JavaVariable upperBound = bound.getUB(i);
	    if(upperBound.toString().equals(ArrayBoundCheckEliminator.inf.toString())) return false;
	    Integer value;
	    offset += bound.getUOffset(i);
	    try {
		value = new Integer(upperBound.toString());
		if((value.intValue()+offset) < 0) {
		    return true;
		}else {
		    return false;
		}
	    }catch(NumberFormatException e) {
		list.add(upperBound);
		bound = (IndexBound)set.get(upperBound);
		if(bound == null ||bound.getUB(i) == null || list.contains(bound.getUB(i))) return false;
	    }
	}
    }
    public boolean isGreaterThanOne(Hashtable set, int i) {
	IndexBound bound = this;
	TreeSet list = new TreeSet();
	int offset = 0;
	while(true) {
	    if(i >= bound.sizeOfLB()|| bound.getLB(i) == null) return false;
	    JavaVariable lowBound = bound.getLB(i);
	    if(lowBound.toString().equals(ArrayBoundCheckEliminator.mInf.toString())) return false;
	    Integer value;
	    offset += bound.getLOffset(i);
	    try {
		value = new Integer(lowBound.toString());
		if((value.intValue() + offset)> 1) {
		    return true;
		}else {
		    return false;
		}
	    }catch(NumberFormatException e) {
		list.add(lowBound);
		bound = (IndexBound)set.get(lowBound);
		if(bound == null ||bound.getLB(i) == null || list.contains(bound.getLB(i))) return false;
	    }
	}
    }
    public boolean isLessThanOne(Hashtable set, int i) {
	IndexBound bound = this;
	TreeSet list = new TreeSet();
	int offset = 0;
	while(true) {
	    if(i >= bound.sizeOfUB()|| bound.getUB(i) == null) return false;
	    JavaVariable upperBound = bound.getUB(i);
	    if(upperBound.toString().equals(ArrayBoundCheckEliminator.inf.toString())) return false;
	    Integer value;
	    offset += bound.getUOffset(i);
	    try {
		value = new Integer(upperBound.toString());
		if((value.intValue() + offset)< 0) {
		    return true;
		}else {
		    return false;
		}
	    }catch(NumberFormatException e) {
		list.add(upperBound);
		bound = (IndexBound)set.get(upperBound);
		if(bound == null || bound.getUB(i) == null ||list.contains(bound.getUB(i))) return false;
	    }
	}
    }
    public boolean isNonNegative(Hashtable set, int i) {
	IndexBound bound = this;
	TreeSet list = new TreeSet();
	int offset = 0;
	while(true) {
	    if(i >= bound.sizeOfLB()|| bound.getLB(i) == null) return false;
	    JavaVariable lowBound = bound.getLB(i);
	    if(lowBound.toString().equals(ArrayBoundCheckEliminator.mInf.toString())) return false;
	    Integer value;
	    try {
		value = new Integer(lowBound.toString());
		offset += bound.getLOffset(i);
		if((value.intValue() + offset)> 1) {
		    return true;
		}else {
		    return false;
		}
	    }catch(NumberFormatException e) {
		if(lowBound.getType() == TypeInfo.TYPE_REFERENCE && offset>=0 ) return true;
		list.add(lowBound);
		bound = (IndexBound)set.get(lowBound);
		if(bound == null  || bound.getLB(i) == null || list.contains(bound.getLB(i))) return false;
	    }
	}
    }
    public boolean isNonPositive(Hashtable set, int i) {
	IndexBound bound = this;
	TreeSet list = new TreeSet();
	int offset = 0;
	while(true) {
	    if(i >= bound.sizeOfUB()|| bound.getUB(i) == null) return false;
	    JavaVariable upperBound = bound.getUB(i);
	    if(upperBound.toString().equals(ArrayBoundCheckEliminator.inf.toString())) return false;
	    Integer value;
	    try {
		value = new Integer(upperBound.toString());
		offset += bound.getUOffset(i);
		if((value.intValue() + offset) <= 0) {
		    return true;
		}else {
		    return false;
		}
	    }catch(NumberFormatException e) {
		list.add(upperBound);
		bound = (IndexBound)set.get(upperBound);
		if(bound == null || bound.getUB(i) == null ||list.contains(bound.getUB(i))) return false;
	    }
	}
    }
    public JavaVariable getUB(int index) {
	if(index >= this.upperBound.size() || upperBound.elementAt(index) == null) return null;
	return (JavaVariable)upperBound.elementAt(index);
    }
    public JavaVariable getLB(int index) {
	if(index >= this.lowBound.size() || lowBound.elementAt(index) == null) return null;
	return (JavaVariable)lowBound.elementAt(index);
    }
    public void setUB(int index, JavaVariable bound, int offset) {
	for(int i = upperBound.size() ; i <= index ; i++)
	    upperBound.add(ArrayBoundCheckEliminator.inf);
	upperBound.setElementAt(bound, index);
	setUOffset(index, offset);
    }
    public void setLB(int index, JavaVariable bound, int offset) {
	for(int i = lowBound.size() ; i <= index ; i++)
	    lowBound.add(ArrayBoundCheckEliminator.mInf);
	lowBound.setElementAt(bound, index);
	setLOffset(index, offset);
    }
    
    public int getUOffset(int index) {
	if(index >= this.upperOffset.size()) return 0;
	return ((Integer)upperOffset.elementAt(index)).intValue();
    }
    public int getLOffset(int index) {
	if(index >= this.lowOffset.size()) return 0;
	return ((Integer)lowOffset.elementAt(index)).intValue();
    }
    public void setUOffset(int index, int offset) {
	for(int i = upperOffset.size() ; i <= index ; i++)
	    upperOffset.add(new Integer(0));
	upperOffset.setElementAt(new Integer(offset), index);
    }
    public void setLOffset(int index, int offset) {
	for(int i = lowOffset.size() ; i <= index ; i++)
	    lowOffset.add(new Integer(0));
	lowOffset.setElementAt(new Integer(offset), index);
    }

    public int sizeOfUB() { return upperBound.size(); }
    public int sizeOfLB() { return lowBound.size(); }
    
    public void setBound(int index, JavaVariable ub, JavaVariable lb, int uoffset, int loffset) {
	setUB(index, ub, uoffset);
	setLB(index, lb, loffset);
    }
    /*
    public void setNextBoundList(IndexBound bound) {
	for(int i = 0 ; i < bound.sizeOfUB() ; i++) {
	    setUB(i+1, bound.getUB(i), bound.getUOffset(i));
	}
	for(int i = 0 ; i < bound.sizeOfLB() ; i++) {
	    setLB(i+1, bound.getLB(i), bound.getLOffset(i));
	}
    }
    */
    public void switchUB(String from, JavaVariable to) {
	for(int i = 0 ; i < upperBound.size() ; i++) {
	    if(getUB(i) != null && getUB(i).toString().equals(from)) {
		int offset = getUOffset(i);
		setUB(i, to, offset);
	    }
	}
    }
    public void switchLB(String from, JavaVariable to) {
	for(int i = 0 ; i < lowBound.size() ; i++) {
	    if(getLB(i) != null && getLB(i).toString().equals(from)) {
		int offset = getLOffset(i);
		setLB(i, to, offset);
	    }
	}
    }
    public void setAttribute(int attr) {
	this.attribute = attr;
    }
    public int getAttribute() {
	return attribute;
    }
    public JavaVariable getValue(int i) {
	if(getLB(i) != null && getLB(i).toString().equals(getUB(i).toString()) && getLOffset(i) == getUOffset(i)) return getLB(i);
	return null;
    }
    /*
    public IndexBound nextBound() {
	IndexBound indexBound = new IndexBound();
	for(int i = 1 ; i < sizeOfLB() ; i++) {
	    indexBound.setLB(i-1, getLB(i), getLOffset(i));
	}
	for(int i = 1 ; i < sizeOfUB() ; i++) {
	    indexBound.setUB(i-1, getUB(i), getUOffset(i));
	}
	return indexBound;
	
    }
    */
    public String toString() {
	String result = "UB : ";
	for(int i = 0 ; i < upperBound.size() ; i++) {
	    result = result + upperBound.elementAt(i);
	}
	result = result + " LB : ";
	for(int i = 0 ; i < lowBound.size() ; i++) {
	    result = result + lowBound.elementAt(i);
	}
	result = result + " UOffset : ";
	for(int i = 0 ; i < upperOffset.size() ; i++) {
	    result = result + upperOffset.elementAt(i);
	}
	result = result + " LOffset : ";
	for(int i = 0 ; i < lowOffset.size() ; i++) {
	    result = result + lowOffset.elementAt(i);
	}
	return result;
    }
}
