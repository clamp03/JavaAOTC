package aotc.profile;

public class LoopInfo {
    private int size;
    private long loopCount;
    private long exceptionCount;
    private long loopIntoCount;

    public LoopInfo(int s, long l, long e, long li) {
	size = s;
	loopCount = l;
	exceptionCount = e;
	loopIntoCount = li;
    }
    public boolean determineLoopPeeling() {
	double check = (1.0 - 2.0 * (double)loopIntoCount / (double)loopCount) * exceptionCount;
	if(check > 100 && size <= 100) {
	    return true;
	}else return false;
    }
    public int getSize() {
	return size;
    }
    public long getLoopCount() {
	return loopCount;
    }
    public long getExceptionCount() {
	return exceptionCount;
    }
    public long getLoopIntoCount() {
	return loopIntoCount;
    }
}

