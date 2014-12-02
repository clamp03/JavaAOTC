package aotc.profile;

import java.util.*;
import java.io.*;
import aotc.share.*;
public class LoopCountAnalyzer {
    private static Hashtable profInfo = null;
    
    public static boolean loadProfileInfo() {
	if(profInfo != null) return true;
	profInfo = new Hashtable();
	try {
	    BufferedReader reader = new BufferedReader(new FileReader("looppeelingprof"));
	    String buf;
	    boolean start = false;
	    String classname = null;
	    String methodname = null;
	    for(buf = reader.readLine(); buf != null; buf = reader.readLine()) {
		if(buf.equals("#start")) {
		    start = true;
		}else if(buf.equals("#end")) {
		    start = false;
		}else if (start == false) {
		    continue;
		}else if(buf.charAt(0) == ' ') {
		    if(buf.charAt(1) == ' ') {
			new aotc.share.Assert(methodname != null && classname != null, "Error classname or method name is null");
			StringTokenizer st = new StringTokenizer(buf.trim());
			new aotc.share.Assert(st.countTokens() == 5, "Error do not match number of loop peeling prof token");
			int bpc = Integer.parseInt(st.nextToken());
			int size = Integer.parseInt(st.nextToken());
			long loopCount = Integer.parseInt(st.nextToken());
			long eliminatedException = Integer.parseInt(st.nextToken());
			long loopIntoCount = Integer.parseInt(st.nextToken());
			LoopInfo prof = new LoopInfo(size, loopCount, eliminatedException, loopIntoCount);
			String key = classname + " " +  methodname + " " + bpc;
			profInfo.put(key, prof);
			/*
			if(prof.determineLoopPeeling()) {
			    System.err.println("... loop peeling : " + classname + " " + methodname + " " + buf.trim());
			}
			*/
		    }else {
			new aotc.share.Assert(classname != null, "Error classname is null");
			methodname = buf.trim();
		    }
		}else {
		    classname = buf.trim();
		}
	    }
	}catch(IOException e) {
	    System.err.println("Can't find looppeelingprof file");
	    profInfo = null;
	    return false;
	}
	return true;
    }
    public static LoopInfo findProfileInfo(String classname, String methodname, int headerBpc) {
	if(classname == null || methodname == null) return null;
	if(!loadProfileInfo()) return null;
	String key = classname + " " + methodname + " " + headerBpc;
	return (LoopInfo)profInfo.get(key);
    }
    public static Vector findProfileInfo(String classname, String methodname) {
	if(classname == null || methodname == null || !loadProfileInfo() ) return null;
	String id = classname + " " + methodname + " ";
	Vector loopInfo = new Vector();
	Enumeration it = profInfo.keys();
	while(it.hasMoreElements()) {
	    String key = (String)it.nextElement();
	    if(key.startsWith(id)) {
		loopInfo.add(profInfo.get(key));
	    }
	}
	if(loopInfo.size() == 0) return null;
	else return loopInfo;
    }
    public static Vector findProfileInfo(String classname) {
	if(classname == null || !loadProfileInfo() ) return null;
	String id = classname + " ";
	Vector loopInfo = new Vector();
	Enumeration it = profInfo.keys();
	while(it.hasMoreElements()) {
	    String key = (String)it.nextElement();
	    if(key.startsWith(id)) {
		loopInfo.add(profInfo.get(key));
	    }
	}
	if(loopInfo.size() == 0) return null;
	else return loopInfo;
    }
}  

