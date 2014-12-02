package aotc.profile;

import java.util.*;
import aotc.translation.*;

public class CallCountTable {
    public Hashtable callerTable;
    public Hashtable callCount;
    //public TreeSet forNameSet;

    public CallCountTable() {
        callerTable = new Hashtable();
	callCount = new Hashtable();
	//forNameSet = new TreeSet();
    }
    /*
    public boolean containsForName(String className, String methodName, String signature) {
	String method = className + "|" + methodName + signature;
	if(forNameSet.contains(method)) {
	    return true;
	}
	return false;
    }
    */
    public void addCallCountInfo(String className, String methodName, String bpc, CalleeInfo callee) {
        int i;
        boolean inserted = false;
        if(callee != null) {
            Vector calleeList = findCalleeList(className, methodName, bpc);
            for (i = 0; i < calleeList.size(); i++) {
                CalleeInfo temp = (CalleeInfo) calleeList.elementAt(i);
                if (callee.compareTo(temp) > 0) {
                    break;
                } 
            }
            calleeList.insertElementAt(callee, i);
        }

        /* call count */
        //String calleeClassName = callee.getRealTargetClass();
        String calleeClassName = callee.getTargetClass();
        String calleeMethodName = callee.getTargetMethod();
        String calleeMethodSig = callee.getTargetType();
        int callCountNum = callee.getCallCount();

        if(callCount.containsKey(calleeClassName)) {
            Hashtable methods = (Hashtable)callCount.get(calleeClassName);
            if(methods.containsKey(calleeMethodName + calleeMethodSig)) {
                Integer value = (Integer)methods.get(calleeMethodName + calleeMethodSig);
                methods.put(calleeMethodName + calleeMethodSig, new Integer((value.intValue() + callCountNum)));
            }else{
                methods.put(calleeMethodName + calleeMethodSig, new Integer(callCountNum));
            }
        }else {
            Hashtable methods = new Hashtable();
            methods.put(calleeMethodName + calleeMethodSig, new Integer(callCountNum));
            callCount.put(calleeClassName, methods);
        }
        /* Class : java/lang/Class , Method : forName */
        /*
           if(callee.getTargetMethod().equals("forName") && callee.getRealTargetClass().equals("java/lang/Class")) {
           forNameSet.add(className + "|" + methodName);
           }
         */
    }

    public int getNumOfCallee(String className, String methodName, String bpc) {
        Vector calleeList = findCalleeList(className, methodName, bpc);
        return calleeList.size();
    }

    public void getCalleeInfo(String className, String methodName, String bpc, int index) {
        Vector calleeList = findCalleeList(className, methodName, bpc);
        //return calleeList.elementAt(index);
        System.out.println(calleeList.elementAt(index));

    }

    public Vector findCalleeList(String className, String methodName, String bpc) {
        Vector calleeList;
        String callerKey = makeCallerKey(className, methodName, bpc);
        if (callerTable.containsKey(callerKey)) {
            calleeList = (Vector) callerTable.get(callerKey);
        } else {
            calleeList = new Vector();
            callerTable.put(callerKey, calleeList);
        }
        return calleeList;
    }
    
    private String makeCallerKey(String className, String methodName, String bpc) {
        return new String(className + "|" + methodName + "|" + bpc);
    }
}
