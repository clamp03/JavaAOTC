package aotc.cfg;

/**
 * The class that defines the constants that are used to identify bytecode info type
 * <p>
 * @author Bae SungHwan
 */
public interface BytecodeInfo {
	
	/**
	 * The constants in the aotc code, that identifies the array type name
	 */
	public static final String arrayTypeName[] = {
		"", "", "", "", "T_BOOLEAN", "T_CHAR", "T_FLOAT",
		"T_DOUBLE", "T_BYTE", "T_SHORT", "T_INT", "T_LONG"
	};
	
	/**
	 * The constants in the aotc code, that identifies the opc type
	 * signed byte	: 1,	unsigned byte	: 2
	 * signed short	: 3,	unsigned short	: 4
	 * signed int	: 5,
	 * unsupported	: -1	special			: -2
     */
    // Clamp - I Changed OPCTYPE *****
    public static final int opcType[] = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           /* 0 - 9 */
        0, 0, 0, 0, 0, 0, 1, 3, -1, -1,         /* 10 - 19 */
        -1, 2, 2, 2, 2, 2, 0, 0, 0, 0,          /* 20 - 29 */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           /* 30 - 39 */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           /* 40 - 49 */
        0, 0, 0, 0, 2, 2, 2, 2, 2, 0,           /* 50 - 59 */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           /* 60 - 69 */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           /* 70 - 79 */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           /* 80 - 89 */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           /* 90 - 99 */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           /* 100 - 109 */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           /* 110 - 119 */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           /* 120 - 129 */
        0, 0, 12, 0, 0, 0, 0, 0, 0, 0,          /* 130 - 139 */
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0,           /* 140 - 149 */
        0, 0, 0, 3, 3, 3, 3, 3, 3, 3,           /* 150 - 159 */
        3, 3, 3, 3, 3, 3, 3, 3, 3, 2,           /* 160 - 169 */
        -2, -2, 0, 0, 0, 0, 0, 0, -1, -1,       /* 170 - 179 */
        -1, -1, -1, -1, -1, -1, -1, -1, 2, -1,  /* 180 - 189 */
        0, 0, -1, -1, 0, 0, -2, -1, 3, 3,       /* 190 - 199 */
        5, 5, -1, 2, 4, 4, 4, 22, 12, 12,       /* 200 - 209 */
        22, 4, 12, 4, 4, -1, 12, 4, 4, 0,       /* 210 - 219 */
        12, 22, 22, 124, 2, 2, 0, 22, 4, 4,     /* 220 - 229 */
        12, 4, 4, 4, 4, 4, 4, 4, 4, 4,          /* 230 - 239 */
        4, 4, 4, 4, 4, 4, 4, 4, 4, 14,          /* 240 - 249 */
        -1, -1, -1, -1, -1, -1
    };
    // **********************************

}
