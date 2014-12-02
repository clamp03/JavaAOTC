package aotc.translation;

import aotc.share.*;
import components.*;
import util.*;

import aotc.*;

/**
 * MethodDeclarationInfo generates method difinition.
 */
public class MethodDeclarationInfo {

    public final static int BYTECODE_NORMAL = 0;
    public final static int BYTECODE_RETURN = 1;
    public final static int BYTECODE_EXCEPTION_RETURN = 2;

    /**
     * Generates arguments list.
     * Ex) CVMExecEnv *ee, jclass cls_ICell, CVMJavaByte l1_byte, ...
     *
     * @param method	the method info
     */
    public static String argumentList(MethodInfo method) {
	String signature = method.type.string;
    // Call Arg - BY Clamp *****
	String result = "register CVMExecEnv *ee, ";
    if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.JNI_TYPE
            || AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.WRAPPER_TYPE) {
        result += "register jclass cls_ICell";
    } else if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.MIX_TYPE) {
        result += "CVMStackVal32* arguments, int* ret, register jclass cls_ICell";
    } else {
        new aotc.share.Assert(false,"Unknown Type : " + AOTCWriter.METHOD_ARGUMENT_PASSING);
    }
    // *************************
	int i = 0;
	if( (method.access & ClassFileConst.ACC_STATIC) == 0 ) {
	    i++;
	}
	int index = signature.indexOf('(')+1;
	char c;

	while( ( c = signature.charAt(index) )!= ')') {
	    if( result.length() != 0) result += ", ";
	    int type = TypeInfo.toAOTCType(c);
	    LocalVariable local = new LocalVariable(type, i++);
	    if(type==TypeInfo.TYPE_REFERENCE) {
		result += "register " + TypeInfo.argTypeName[type] + " " + local + "_ICell";
	    } else {
		result += "register " + TypeInfo.argTypeName[type] + " " + local;
	    }
	    switch(c) {
		case 'L':
		    index=signature.indexOf(';',index);		
		    break;
		case 'J':	case 'D':
		    i++;
		    break;
		case 'I':	case 'Z':
		case 'B':	case 'C':
		case 'F':	case 'S':
		    break;
		case '[':
		    index++;
		    while(signature.charAt(index)=='[') {
			index++;
		    }
		    if(signature.charAt(index)=='L')
			index=signature.indexOf(';',index);
		    break;
		default:
		    new aotc.share.Assert(false,"Unknown Type : "+c);
		    break;
	    }
	    index++;
	}
	return result;
    }

    /**
     * Generates method difinition.
     * See output "C" files.
     *
     * @param name	the method name
     * @param method	the method info
     */
    public static String methodDefinition(String name, MethodInfo method) {
    // Call Arg - BY Clamp *****
    String result;
    if(AOTCWriter.METHOD_ARGUMENT_PASSING == AOTCWriter.CNI_TYPE) {
        result = "JNIEXPORT int ";
        result += "Java_" + method.getNativeName(true).substring(5);
        result +="(CVMExecEnv* ee, CVMStackVal32 *arguments)";
        return result;
    }else {
        result = "JNIEXPORT ";
        String signature = method.type.string;
        result += (TypeInfo.getRetTypeName(signature.charAt(signature.indexOf(')')+1)));
        result += (" JNICALL "+name+"(");
        result += (MethodDeclarationInfo.argumentList(method)+")");
        return result;
    }
    // *************************
    }

    /**
     * Generates method type for method call.
     *
     * @param type	the method argement type signature
     */
    public static String methodTypeList(String type) {
	int index = type.indexOf('(')+1;
	char c;
	String result = "";

	while( ( c = type.charAt(index) )!= ')') {
	    result += TypeInfo.type[TypeInfo.toAOTCType(c)];
	    if(c == 'L') {
		index=type.indexOf(';',index);
	    }
	    if(c == '[') {
		index++;
		while(type.charAt(index)=='[') {
		    index++;
		}
		if(type.charAt(index)=='L')
		    index=type.indexOf(';',index);
	    }
	    index++;
	}
	return type.charAt(++index) + result;
    }
}
