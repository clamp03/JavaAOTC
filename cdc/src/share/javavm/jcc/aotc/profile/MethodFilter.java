package aotc.profile;

import java.util.*;
import java.io.*;
import aotc.share.*;
import aotc.*;

public class MethodFilter{
    private static Hashtable methods = null;
    
    public static void readProfileInfo() {
	if(methods != null) return;

	methods = new Hashtable();
	String buf;
	BufferedReader in = null;
	try {
	    in = new BufferedReader(new FileReader("methodfilter"));
	    for(buf = in.readLine(); buf != null ; buf = in.readLine()) {
		MethodFilterInfo mInfo = parseProfile(buf);
		addMethodFilterInfo(mInfo);
	    }
	}catch(IOException e) {
	    System.err.println("Can't find methodfilter file");
	    methods = null;
	}
    }
    
    private static MethodFilterInfo parseProfile(String line) {
	int count;
	int callCount = 0;
	double pTime, runTime;
	String methodName;
	
	StringTokenizer strToken = new StringTokenizer(line);
	count = strToken.countTokens();
	pTime = Double.parseDouble(strToken.nextToken());
	strToken.nextToken();
	runTime = Double.parseDouble(strToken.nextToken());

	if(count == 7) {
	    callCount = Integer.parseInt(strToken.nextToken());
	    strToken.nextToken();strToken.nextToken();
	}
	methodName = strToken.nextToken();
	//System.out.println(methodName + "\t" + pTime + "\t" + runTime + "\t" + callCount);
	return new MethodFilterInfo(methodName, pTime, runTime, callCount);
    }
    private static void addMethodFilterInfo(MethodFilterInfo mInfo) {
	MethodFilterInfo prevValue = (MethodFilterInfo)methods.remove(mInfo.getMethodName());
	if(prevValue == null) {
	    methods.put(mInfo.getMethodName(), mInfo);
	}else {
	    double pTime, runTime;
	    int callCount;
	    pTime = prevValue.getPercentage() + mInfo.getPercentage();
	    runTime = prevValue.getRuntime() + mInfo.getRuntime();
	    callCount = prevValue.getCallCount() + mInfo.getCallCount();
	    MethodFilterInfo newInfo = new MethodFilterInfo(mInfo.getMethodName(), pTime, runTime, callCount);
	    methods.put(mInfo.getMethodName(), newInfo);
	}
    }
    public static boolean determineAOTC_CallCount(String classname, String methodname, String signature) {
	CallCountTable callerCountTable = CallCountAnalyzer.getCallCountTable();
	/*
	   if(callerCountTable.callCount.containsKey(classname)) {
	   Hashtable methods = (Hashtable)callerCountTable.callCount.get(classname);
	   if(methodname == null) return true;
	   if(methods.containsKey(methodname + signature)) {
	   return true;
	   }
	   }
	 */
	if(callerCountTable.callCount.containsKey(classname)) {
	    Hashtable methods = (Hashtable)callerCountTable.callCount.get(classname);
	    if(methodname == null) {
		Enumeration elements = methods.elements();
		while(elements.hasMoreElements()) {
		    int callCount = ((Integer)elements.nextElement()).intValue();
		    if(callCount >= ClassFilter.CALL_COUNT) return true;
		}
        return true;
	    }else if(methods.containsKey(methodname + signature)) {
		int callCount = ((Integer)methods.get(methodname + signature)).intValue();
		if(callCount >= ClassFilter.CALL_COUNT) return true;
        return true;
	    }
        //new aotc.share.Assert(false, "determine AOTC call count");
	}
	return false;
    }
    public static boolean determineAOTC_Runtime(String classname, String methodname, String signature) {
	// Run Time
	readProfileInfo();
	String method;
	if(methodname == null) {
	    method = jcc.Util.convertToJNIName(classname, "", null);
	}else {
	    method = jcc.Util.convertToJNIName(classname, methodname, null);
	}
	if(methodname != null) {
	    MethodFilterInfo mInfo = (MethodFilterInfo)methods.get(methodname);
	    if(mInfo != null && mInfo.getPercentage() >= ClassFilter.RUNTIME_PERCENTAGE) return true;
	}else {
	    Enumeration keys = methods.keys();
	    while(keys.hasMoreElements()) {
		String key = keys.nextElement().toString();
		if(key.startsWith(method)) {
		    MethodFilterInfo mInfo = (MethodFilterInfo)methods.get(key);
		    if(mInfo != null && mInfo.getPercentage() >= ClassFilter.RUNTIME_PERCENTAGE) return true;
		}
	    }
	}
	return false;
    }
    public static boolean determineAOTC_LoopCount(String classname, String methodname, String signature) {
	// Loop Count
	Vector loops = null;
	if(methodname == null) {
	    loops = LoopCountAnalyzer.findProfileInfo(classname);
	}else {
	    loops = LoopCountAnalyzer.findProfileInfo(classname, methodname + signature);
	}

	if(loops == null) return false;
	int loopCount = 0;
	for(int i = 0 ; i < loops.size() ; i++) {
	    LoopInfo loopinfo = (LoopInfo)loops.elementAt(i);
	    loopCount += (loopinfo.getLoopCount() - loopinfo.getLoopIntoCount());
	}
	if(loopCount >= ClassFilter.LOOP_COUNT) return true;
	return false;
    }

    public static void printAllInfo() {
	Enumeration keys = methods.keys();
	while(keys.hasMoreElements()) {
	    String key = keys.nextElement().toString();
	    MethodFilterInfo mInfo = (MethodFilterInfo)methods.get(key);
	    System.out.println(mInfo.getMethodName() + "\t" + mInfo.getPercentage() + "\t" + mInfo.getRuntime() + "\t" + mInfo.getCallCount());
	}
    }
}
