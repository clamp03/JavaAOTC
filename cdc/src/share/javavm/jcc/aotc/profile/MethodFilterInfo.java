package aotc.profile;
public class MethodFilterInfo {
    private double pTime;
    private double runTime;
    private int callCount;
    private String methodName;
    
    public MethodFilterInfo(String methodName, double pTime, double runTime, int callCount) {
	this.methodName = methodName;
	this.pTime = pTime;
	this.runTime = runTime;
	this.callCount = callCount;
    }
    public double getPercentage() {
	return pTime;
    }
    public double getRuntime() {
	return runTime;
    }
    public int getCallCount() {
	return callCount;
    }
    public String getMethodName() {
	return methodName;
    }
}
