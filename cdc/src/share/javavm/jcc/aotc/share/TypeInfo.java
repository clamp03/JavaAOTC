package aotc.share;

/**
 * The class that defines the constants that are used to identify java variable type
 * <p>
 * @author Park Jong-Kuk
 */

public class TypeInfo {
	
	/**
	 * The constants in the aotc code, that identifies the unknown call type
	 */
	public static final int CALLTYPE_UNKNOWN = -1;
	/**
	 * The constants in the aotc code, that identifies the java interpreter call type
	 */
	public static final int CALLTYPE_JAVA = 0;
	/**
	 * The constants in the aotc code, that identifies the jni call type
	 */
	public static final int CALLTYPE_JNI = 1;
	/**
	 * The constants in the aotc code, that identifies the cni call type
	 */
	public static final int CALLTYPE_CNI = 2;
	/**
	 * The constants in the aotc code, that identifies the aotc call type
	 */
	public static final int CALLTYPE_AOTC = 3;
	
	/**
	 * The constants in the aotc code, that identifies the none type
	 */
	public static final int TYPE_NONE = -1;
	/**
	 * The constants in the aotc code, that identifies the boolean type
	 */
	public static final int TYPE_BOOLEAN = 0;
	/**
	 * The constants in the aotc code, that identifies the byte type
	 */
	public static final int TYPE_BYTE = 1;
	/**
	 * The constants in the aotc code, that identifies the char type
	 */
	public static final int TYPE_CHAR = 2;
	/**
	 * The constants in the aotc code, that identifies the short type
	 */
	public static final int TYPE_SHORT = 3;
	/**
	 * The constants in the aotc code, that identifies the int type
	 */
	public static final int TYPE_INT = 4;
	/**
	 * The constants in the aotc code, that identifies the long type
	 */
	public static final int TYPE_LONG = 5;
	/**
	 * The constants in the aotc code, that identifies the float type
	 */
	public static final int TYPE_FLOAT = 6;
	/**
	 * The constants in the aotc code, that identifies the double type
	 */
	public static final int TYPE_DOUBLE = 7;
	/**
	 * The constants in the aotc code, that identifies the reference type
	 */
	public static final int TYPE_REFERENCE = 8;
	/**
	 * The constants in the aotc code, that identifies the void type
	 */
	public static final int TYPE_VOID = 9;
	/**
	 * The constants in the aotc code, that identifies the float type
	 */
	public static final int TYPE_FLOAT_X = 10;
	/**
	 * The constants in the aotc code, that identifies the double type
	 */
	public static final int TYPE_DOUBLE_X = 11;
	
	/**
	 * The constants in the aotc code. that identifies the suffix for each type
	 */
	public static final String suffix[] = {
		"boolean", "byte", "char", "short", "int", "long",
		"float", "double", "ref", "void", "floatx", "doublex" };
	/**
	 * The constants in the aotc code. that identifies the argument type that used in C code for each type
	 */
	public static final String argTypeName[] = {
		"CVMJavaBoolean", "CVMJavaByte", "CVMJavaChar", "CVMJavaShort", "CVMJavaInt", "CVMJavaLong",
		"CVMJavaFloat", "CVMJavaDouble", "jobject", "void",
		"CVMJavaVal32", "CVMJavaVal64" };
	/**
	 * The constants in the aotc code. that identifies the cvm return type for each type
	 */
	public static final String CVMretTypeName[] = {
		"CVM_TYPEID_BOOLEAN", "CVM_TYPEID_BYTE", "CVM_TYPEID_CHAR", "CVM_TYPEID_SHORT", "CVM_TYPEID_INT",
		"CVM_TYPEID_LONG", "CVM_TYPEID_FLOAT", "CVM_TYPEID_DOUBLE", "CVM_TYPEID_OBJ", "CVM_TYPEID_VOID",
		"--", "--" };
	/**
	 * The constants in the aotc code. that identifies the return type for each type
	 */
	public static final String retTypeName[] = {
               "CVMJavaBoolean", "CVMJavaByte", "CVMJavaChar", "CVMJavaShort", "CVMJavaInt", "CVMJavaLong",
               "CVMJavaFloat", "CVMJavaDouble", "jobject", "void",
               "CVMJavaVal32", "CVMJavaVal64" };
	 /**
	 * The constants in the aotc code. that identifies the type string for each type number
	 */
	public static final String typeName[] = {
		"CVMJavaBoolean", "CVMJavaByte", "CVMJavaChar", "CVMJavaShort", "CVMJavaInt", "CVMJavaLong",
		"CVMJavaFloat", "CVMJavaDouble", "jobject", "void",
		"CVMJavaVal32", "CVMJavaVal64" };
	/**
	 * The constants in the aotc code. that identifies the type charactor for each type number
	 */
	public static final String type[] = {
		"Z", "B", "C", "S", "I", "J",
		"F", "D", "L", "V", "f", "d" };
	/**
	 * The constants in the aotc code. that identifies the size for each type number
	 */
	public static final int size[] = {
		1, 1, 1, 1, 1, 2,
		1, 2, 1, 1, 1, 2 };
	/**
	 * Gets ths return type of method
	 *
	 * @param type the return type of method to be used index of retTypeName
	 * @return the name of return type to be used in C code
	 */
	public static String getRetTypeName(char type) {
		return retTypeName[toAOTCType(type)];
	}
	
	/**
	 * Gets ths return type of method
	 *
	 * @param type the return type of method to be used index of CVMretTypeName
	 * @return the name of return type to be used in C code
	 */
	public static String getCVMRetTypeName(char type) {
		return CVMretTypeName[toAOTCType(type)];
	}
	
	/**
	 * Gets ths type of variable
	 *
	 * @param type the type of variable to be used index of typeName
	 * @return the name of type to be used in C code
	 */
	public static String getTypeName(char type) {
		return typeName[toAOTCType(type)];
	}
	
	/**
	 * Gets ths type of argument variable
	 *
	 * @param type the type of arguemnt variable to be used index of argTypeName
	 * @return the name of argument type to be used in C code
	 */
	public static String getArgTypeName(char type) {
		return argTypeName[toAOTCType(type)];
	}
	
	/**
	 * Gets ths suffix of variable
	 *
	 * @param type the type of variable
	 * @return the suffix name of type to be used in C code
	 */
	public static String getCVMTypeSuffix(char type) {
		switch(type){	
		case 'L': case '[':
			return "Object";
		case 'I':
			return "Int";
		case 'B':
			return "Byte";
		case 'C':
			return "Char";
		case 'D':
			return "Double";
		case 'F':
			return "Float";
		case 'J':
			return "Long";
		case 'S':
			return "Short";
		case 'Z':
			return "Boolean";
		case 'V':
			return "Void";
		default:
			new aotc.share.Assert( false , "123 unknown type : " + type );
			return "  ";
		}
	}
	
	/**
	 * Translates the char of type to the index of AOTC type
	 *
	 * @param type the type of variable
	 * @return the index of AOTC type
	 */
	public static int toAOTCType(char type) {
		switch(type){	
		case 'L': case '[':
			return TYPE_REFERENCE; 
		case 'I':
			return TYPE_INT;
		case 'B':
			return TYPE_BYTE;
		case 'C':
			return TYPE_CHAR;
		case 'D':
			return TYPE_DOUBLE;
		case 'F':
			return TYPE_FLOAT;
		case 'J':
			return TYPE_LONG;
		case 'S':
			return TYPE_SHORT;
		case 'Z':
			return TYPE_BOOLEAN;
		case 'V':
			return TYPE_VOID;
		default:
			new aotc.share.Assert( false , "unknown type : " + type );
			return ' ';
		}
	}
	/**
	 * Translates the int of type to the index of AOTC type suffix
	 *
	 * @param type the type of variable
	 * @return the AOTC type suffix
	 */
	public static String toAOTCTypeSuffix(int type) {
		switch(type){	
                case TYPE_FLOAT: return "Float";
                case TYPE_REFERENCE: return "Ref";
                case TYPE_LONG: return "Long";
                case TYPE_DOUBLE: return "Double";
                default: return "Int";
                }
	}
}
