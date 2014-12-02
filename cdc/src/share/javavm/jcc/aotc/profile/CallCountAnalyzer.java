package aotc.profile;

import java.io.*;
import java.util.*;

public class CallCountAnalyzer
{   
    public static CallCountTable getCallCountTable() {
        if(callCounterTable == null) {
            initialize();
        }
        return callCounterTable;
    }

    private static CallCountTable callCounterTable=null;
    
    private static void initialize() {
        readProfileInfo();
    }
    private static void readProfileInfo() {
        String buf;
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader("aotcprof"));
            ProfileParser parser = new ProfileParser();
            boolean success;
            for (buf = in.readLine(); buf != null; buf = in.readLine()) {
                success = parser.parseNext(buf);
                if (!success) {
                    System.err.println("Invalid token : " + buf);
                    callCounterTable = null;
                    break;
                }
                if (parser.getState() == ProfileParser.STATE_END) {
                    callCounterTable = parser.getCallCountTable();
                    break;
                }
            }
            in.close();
        } catch (IOException e) {
            System.err.println("Can't find aotcprof file");
            callCounterTable = null;
        }         
    }
}


// Implemented a a finite state machine
class ProfileParser { 
    public static final int STATE_INIT = 1;
    public static final int STATE_READ_CLASS = 2;
    public static final int STATE_READ_METHOD = 3;
    public static final int STATE_READ_BPC = 4;
    public static final int STATE_READ_CALLEEINFO = 5;
    public static final int STATE_END = 6;

    private static final int TOKEN_TYPE_START = 1;
    private static final int TOKEN_TYPE_CLASS = 2;
    private static final int TOKEN_TYPE_METHOD = 3;
    private static final int TOKEN_TYPE_BPC = 4;
    private static final int TOKEN_TYPE_CALLEEINFO = 5;
    private static final int TOKEN_TYPE_END = 6;
    
    private CallCountTable table;

    private String currentClass;
    private String currentMethod;
    private String currentBpc;
            
    private int state;

    ProfileParser() {
        state = STATE_INIT;
        table = new CallCountTable();
    }

    int getState() {
        return state;
    }

    CallCountTable getCallCountTable() {
        return table;
    }

    // 1. process current token
    // 2. determine next state
    public boolean parseNext(String line) {

        int tokenType = getTokenType(line);

        switch (tokenType) {
            case TOKEN_TYPE_CLASS:
                currentClass = new String(line);
                break;
            case TOKEN_TYPE_METHOD:
                line = line.substring(1);
                {                                                             
                    StringTokenizer st = new StringTokenizer(line);           
                    String name = st.nextToken();                             
                    String type = st.nextToken();                             
                    line = name + type;                                       
                }                       
                currentMethod = new String(line);
                break;
            case TOKEN_TYPE_BPC:
                line = line.substring(2);
                currentBpc = new String(line);
                break;
            case TOKEN_TYPE_CALLEEINFO:
                line = line.substring(3);
                table.addCallCountInfo(currentClass, currentMethod, currentBpc, makeCalleeInfo(line));
                break;
            default:
                break;
        } 
    
        // determine next state
        switch (state) {
            case STATE_INIT: // Initial state
                if (tokenType == TOKEN_TYPE_START) {
                    state = STATE_READ_CLASS;
                    return true;
                } else {
                    return false;
                }
            case STATE_READ_CLASS:
                if (tokenType == TOKEN_TYPE_CLASS) {
                    state = STATE_READ_METHOD;
                    // process class
                    return true;
                } else {
                    return false;
                }
            case STATE_READ_METHOD:
                if (tokenType == TOKEN_TYPE_METHOD) {
                    state = STATE_READ_BPC;
                    // process method
                    return true;
                } else if (tokenType == TOKEN_TYPE_CLASS) {
                    state = STATE_READ_METHOD;
                    // process class
                    return true;
                } else {
                    return false;
                }
            case STATE_READ_BPC:
                if (tokenType == TOKEN_TYPE_BPC) {
                    state = STATE_READ_CALLEEINFO;
                    // process method
                    return true;
                } else if (tokenType == TOKEN_TYPE_METHOD) {
                    state = STATE_READ_BPC;
                    // process bpc
                    return true;
                } else {
                    return false;
                }
            case STATE_READ_CALLEEINFO:
                if (tokenType == TOKEN_TYPE_CALLEEINFO) {
                    state = STATE_READ_CALLEEINFO;
                    // process calleeinfo
                    return true;
                } else if (tokenType == TOKEN_TYPE_BPC) {
                    state = STATE_READ_CALLEEINFO;
                    // process bpc 
                    return true;
                } else if (tokenType == TOKEN_TYPE_METHOD) {
                    state = STATE_READ_BPC;
                    // process method
                    return true;
                } else if (tokenType == TOKEN_TYPE_CLASS) {
                    state = STATE_READ_METHOD;
                    // process method
                    return true;
                } else if (tokenType == TOKEN_TYPE_END) {
                    state = STATE_END;
                    // process method
                    return true;
                } else {
                    return false;
                }
            default:
                return false;
        }
    }

    private CalleeInfo makeCalleeInfo(String line) {
        boolean isVirtual = true;
        String calleeClass;
        String calleeMethod;
        String signature;
	String realTargetClass;
        //String callType;
        String mIndex;
        int callCount;
        StringTokenizer st = new StringTokenizer(line);
        calleeClass = st.nextToken();
        calleeMethod = st.nextToken();
        signature = st.nextToken();
	
        //callType = st.nextToken();
        mIndex = st.nextToken();
	//realTargetClass = st.nextToken();
	realTargetClass = calleeClass;
	callCount = Integer.parseInt(st.nextToken());
	//return new CalleeInfo(isVirtual, calleeClass, calleeMethod, signature, callType, realTargetClass, callCount);
    return new CalleeInfo(calleeClass, calleeMethod, signature, callCount);
    }

    private int getTokenType(String line) {
        if (line.charAt(0) == ' ') {
            if (line.charAt(1) == ' ') {
                if (line.charAt(2) == ' ') {
                    return TOKEN_TYPE_CALLEEINFO;
                } else {
                    return TOKEN_TYPE_BPC;
                } 

            } else {
                return TOKEN_TYPE_METHOD;
            }
        } else {
            if (line.equals("#start")) {
                return TOKEN_TYPE_START;
            } else if (line.equals("#end")) {
                return TOKEN_TYPE_END;
            } else {
                return TOKEN_TYPE_CLASS;
            }
        }
    }
}
