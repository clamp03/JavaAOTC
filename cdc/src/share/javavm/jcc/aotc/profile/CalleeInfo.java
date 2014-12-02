package aotc.profile;

import aotc.*;

public class CalleeInfo implements Comparable {
    //private boolean isVirtual;
    private String targetClass;
    private String targetMethod;
    private String signature;
    //private String callType;
    //private String realTargetClass;
    private int callCount;
    
    public CalleeInfo(String targetClass, String targetMethod,
            String signature, int callCount) {
        this.targetClass = new String(targetClass);
        this.targetMethod = new String(targetMethod);
        this.signature = new String(signature);
        this.callCount = callCount;
    }
    /*
    public CalleeInfo(boolean isVirtual, String targetClass, String targetMethod, 
                        String signature, String callType, String realTargetClass, int callCount) {
        this.isVirtual = isVirtual;
        this.targetClass = new String(targetClass);
        this.targetMethod = new String(targetMethod);
        this.signature = new String(signature);
        this.callType = new String(callType);
	this.realTargetClass = new String(realTargetClass);
        this.callCount = callCount;
    }
    */

    public String toString() {
        return (targetClass + "." + targetMethod + signature);
        //return (targetClass + " " + targetMethod + " " + signature 
        //            + " " + callType + " " + realTargetClass + " " + Integer.toString(callCount));
    }

    public int compareTo(Object o) {
        CalleeInfo a = this;
        CalleeInfo b = (CalleeInfo) o;
        if (a.callCount > b.callCount) {
            return 1;
        } else if (a.callCount < b.callCount) {
            return -1;
        } else {
            return 0;
        }
    }

    public String getTargetClass() {
	return targetClass;
    }
    public String getTargetMethod() {
	return targetMethod;
    }
    public String getTargetType() {
	return signature;
    }
    //public String getCallType() {
	//return callType;
    //}
    public int getCallCount() {
	return callCount;
    }
    //public String getRealTargetClass() {
	//return realTargetClass;
    //}
}
