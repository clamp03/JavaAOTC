/*
 *  Used 'Inner Classes' to minimize temptations of JVM exploiting low hanging
 *  fruits. 'Inner classes' are defined in appendix D of the 'Java Programming
 *  Language' by Ken Arnold.
 *  - We moved the class declaration, unchanged, of Hash_Table to within the
 *    class declaration of Compressor.
 *  - We moved the class declarations, unchanged, of De_Stack and
      Suffix_Table to within the class declaration of Decompressor.
 *  - pre-computed trivial htab(i) to minimize millions of trivial calls
 *  - Don McCauley (IBM), Kaivalya 4/16/98
 *
 *   @(#)Compress.java	1.7 06/17/98
 *  // Don McCauley/kmd  - IBM 02/26/98
 *  // getbyte and getcode fixed -- kaivalya & Don
 * compress.c - File compression ala IEEE Computer, June 1984.
 *
 * Authors:	Spencer W. Thomas	(decvax!harpo!utah-cs!utah-gr!thomas)
 *		Jim McKie		(decvax!mcvax!jim)
 *		Steve Davies		(decvax!vax135!petsd!peora!srd)
 *		Ken Turkowski		(decvax!decwrl!turtlevax!ken)
 *		James A. Woods		(decvax!ihnp4!ames!jaw)
 *		Joe Orost		(decvax!vax135!petsd!joe)
 *
 * Algorithm from "A Technique for High Performance Data Compression",
 * Terry A. Welch, IEEE Computer Vol 17, No 6 (June 1984), pp 8-19.
 *
 * Algorithm:
 * 	Modified Lempel-Ziv method (LZW).  Basically finds common
 * substrings and replaces them with a variable size code.  This is
 * deterministic, and can be done on the fly.  Thus, the decompression
 * procedure needs no input table, but tracks the way the table was built.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.benchmarks._201_compress;

final class Compress {
    final static int BITS = 16;		/* always set to 16 for SPEC95 */
    final static int INIT_BITS = 9;	/* initial number of bits/code */
    final static int HSIZE = 69001;	/* 95% occupancy */
    final static int SUFFIX_TAB_SZ = 65536;	/* 2**BITS */
    final static int STACK_SZ = 8000;	/* decompression stack size */

    final static byte magic_header[] = { (byte)037, (byte)0235 }; /* 1F 9D */
    /* Defines for third byte of header */
    final static int BIT_MASK = 0x1f;
    final static int BLOCK_MASK = 0x80;
    /* Masks 0x40 and 0x20 are free.  I think 0x20 should mean that there is
       a fourth header byte (for expansion). */

    /*
     * the next two codes should not be changed lightly, as they must not
     * lie within the contiguous general code space.
     */ 
    final static int FIRST = 257;	/* first free entry */
    final static int CLEAR = 256;	/* table clear output code */

    final static byte lmask[] = {
	(byte)0xff, (byte)0xfe, (byte)0xfc, (byte)0xf8, (byte)0xf0,
	(byte)0xe0, (byte)0xc0, (byte)0x80, (byte)0x00
    };
    final static byte rmask[] = {
	(byte)0x00, (byte)0x01, (byte)0x03, (byte)0x07, (byte)0x0f,
	(byte)0x1f, (byte)0x3f, (byte)0x7f, (byte)0xff
    };

    public static int spec_select_action(byte[] from_buf,
					 int from_count,
					 int action,
					 byte[] to_buf) {

	Input_Buffer in = new Input_Buffer(from_count, from_buf);
	Output_Buffer out = new Output_Buffer(to_buf);

	if (action == 0) {
	    Compressor comp = new Compressor(in, out);
	    comp.compress();
	} else {
	    Decompressor decomp = new Decompressor(in, out);
	    decomp.decompress();
	}

	return( out.count() );
    }
};


/*****************************************************************/

final class Input_Buffer {
    private int InCnt;
    private int Current;
    private byte[] InBuff;

    public Input_Buffer(int c, byte[] b) {
	InCnt = c;
	Current = 0;
	InBuff = b;
    }

//     kmd 02/26/98 public byte getbyte() {

    public int getbyte() {
	if( InCnt > 0 ) {
	    InCnt--;
//	    return( InBuff[Current++] ); //     kmd 02/26/98
	    return( InBuff[Current++] & 0x00FF ); //     kmd 02/26/98
	} else {
//	    return( (byte)-1 );  //     kmd 02/26/98
	    return( -1 );        //     kmd 02/26/98
	}
    }


    public int readbytes(byte[] buf, int n) {
	int i;

	if( InCnt <= 0 )
	    return( -1 );

	if( n > InCnt )
	    n = InCnt;

	for( i=0; i<n; i++ ) {
	    buf[i] = InBuff[Current++];
	    InCnt--;
	}

	return( i );
    }
};


/*****************************************************************/

final class Output_Buffer {
    private int OutCnt;
    private byte[] OutBuff;

    public Output_Buffer(byte[] b) {
	OutCnt = 0;
	OutBuff = b;
    }

    public int count() {
	return OutCnt;
    }

    public void putbyte(byte c) {
	OutBuff[OutCnt++] = c;
    }

    public void writebytes(byte[] buf, int n) {
	int i;

	for( i=0; i<n; i++ )
	    OutBuff[OutCnt++] = buf[i];
    }
};


/*****************************************************************/

final class Code_Table {
    private short tab[];

    public Code_Table() {
	tab = new short[Compress.HSIZE];
    }

    public int of(int i) {
	return ((int)tab[i] << 16 >>> 16);
    }

    public void set(int i, int v) {
	tab[i] = (short)v;
    }

    public void clear(int size) {
	int code;
	for ( code = 0; code < size; code++ ) {
	    tab[code] = 0;
	}
    }
};


/*****************************************************************/

class Comp_Base {
    protected int n_bits;		/* number of bits/code */
    protected int maxbits;		/* user settable max # bits/code */
    protected int maxcode;		/* maximum code, given n_bits */
    protected int maxmaxcode;		/* should NEVER generate this code */

    protected int offset;
    protected int block_compress;
    protected int free_ent;		/* first unused entry */
    protected int clear_flg;

    protected Input_Buffer Input;
    protected Output_Buffer Output;
    protected byte buf[];

    public Comp_Base(Input_Buffer in, Output_Buffer out) {
	Input = in;
	Output = out;
	maxbits = Compress.BITS;
	block_compress = Compress.BLOCK_MASK;
	buf = new byte[Compress.BITS];
    }

    public int MAXCODE() {
	return ((1 << (n_bits)) - 1);
    }
};


/*****************************************************************/

/*
 * compress
 * (Originally: stdin to stdout -- Changed by SPEC to: memory to memory)
 *
 * Algorithm:  use open addressing double hashing (no chaining) on the 
 * prefix code / next character combination.  We do a variant of Knuth's
 * algorithm D (vol. 3, sec. 6.4) along with G. Knott's relatively-prime
 * secondary probe.  Here, the modular division first probe is gives way
 * to a faster exclusive-or manipulation.  Also do block compression with
 * an adaptive reset, whereby the code table is cleared when the compression
 * ratio decreases, but after the table fills.  The variable-length output
 * codes are re-sized at this point, and a special CLEAR code is generated
 * for the decompressor.  Late addition:  construct the table according to
 * file size for noticeable speed improvement on small files.  Please direct
 * questions about this implementation to ames!jaw.
 */

final class Compressor extends Comp_Base {
    private final static int CHECK_GAP = 10000;	/* ratio check interval */
    private int ratio;
    private int checkpoint;
    private int in_count;		/* length of input */
    private int out_count;		/* # of codes output */
    private int bytes_out;		/* length of compressed output */

    private Hash_Table htab;
    private Code_Table codetab;

    public Compressor(Input_Buffer in, Output_Buffer out) {
	super(in, out);
	if (maxbits < Compress.INIT_BITS) maxbits = Compress.INIT_BITS;
	if (maxbits > Compress.BITS) maxbits = Compress.BITS;
	maxmaxcode = 1 << maxbits;
	n_bits = Compress.INIT_BITS;
	maxcode = MAXCODE();

	offset = 0;
	bytes_out = 3;		/* includes 3-byte header mojo */
	out_count = 0;
	clear_flg = 0;
	ratio = 0;
	in_count = 1;
	checkpoint = CHECK_GAP;
	free_ent = ((block_compress != 0) ? Compress.FIRST : 256 );

	htab = new Hash_Table();  // dm/kmd 4/10/98
	codetab = new Code_Table();

	Output.putbyte(Compress.magic_header[0]);
	Output.putbyte(Compress.magic_header[1]);
	Output.putbyte((byte)(maxbits | block_compress));
    }

    public void compress() {
  	int fcode;
	int i = 0;
	int c;
	int ent;
	int disp;
	int hsize_reg;
	int hshift;

  	ent = Input.getbyte () ;

	hshift = 0;
	for ( fcode = htab.hsize();  fcode < 65536; fcode *= 2 )
	    hshift++;
	hshift = 8 - hshift;		/* set hash code range bound */

	hsize_reg = htab.hsize();
	htab.clear();			/* clear hash table */

	next_byte:
	while ( (c = Input.getbyte()) != -1) {
	    in_count++;
	    fcode = (((int) c << maxbits) + ent);
  	    i = ((c << hshift) ^ ent);	/* xor hashing */
            int temphtab = htab.of (i);  // dm/kmd 4/15
//dm kmd	    if ( htab.of (i) == fcode ) {  // dm/kmd 4/15
	    if ( temphtab == fcode ) {
		ent = codetab.of (i);
		continue next_byte;
	    }

//dm kmd 4/15	    if ( htab.of (i) >= 0 ) {	/* non-empty slot */
	    if ( temphtab >= 0 ) {	/* non-empty slot  dm kmd 4/15*/
		disp = hsize_reg - i;	/* secondary hash (after G. Knott) */
		if ( i == 0 )
		    disp = 1;

		do {
		    if ( (i -= disp) < 0 )
			i += hsize_reg;

                     temphtab = htab.of (i);  // dm/kmd 4/15
		    
// dm/kmd 4/15	    if ( htab.of (i) == fcode ) {
		    if ( temphtab == fcode ) {
			ent = codetab.of (i);
			continue next_byte;
		    }
// dm/kmd 4/15		} while ( htab.of (i) > 0 );
		} while ( temphtab > 0 );              // dm kmd 4/15
	    }

	    output ( ent );
	    out_count++;
	    ent = c;
	    if ( free_ent < maxmaxcode ) {
		codetab.set(i, free_ent++);	/* code -> hashtable */
  		htab.set(i, fcode);
	    }
	    else if ( (in_count >= checkpoint) && (block_compress != 0) )
		cl_block ();
	}
	/*
	 * Put out the final code.
	 */
	output( ent );
	out_count++;
	output( -1 );

	return;
    }

    /*
     * Output the given code.
     * Inputs:
     * 	code:	A n_bits-bit integer.  If == -1, then EOF.  This assumes
     *		that n_bits =< (long)wordsize - 1.
     * Outputs:
     * 	Outputs code to the file.
     * Assumptions:
     *	Chars are 8 bits long.
     * Algorithm:
     * 	Maintain a BITS character long buffer (so that 8 codes will
     * fit in it exactly).
     */

    private void output(int code) {
	int r_off = offset, bits= n_bits;
	int bp = 0;

	if ( code >= 0 ) {
	    /*
	     * Get to the first byte.
	     */
	    bp += (r_off >> 3);
	    r_off &= 7;
	    /*
	     * Since code is always >= 8 bits, only need to mask the first
	     * hunk on the left.
	     */
	    buf[bp] = (byte)((buf[bp] & Compress.rmask[r_off]) |
			     (code << r_off) & Compress.lmask[r_off]);
	    bp++;
	    bits -= (8 - r_off);
	    code >>= 8 - r_off;
	    /* Get any 8 bit parts in the middle (<=1 for up to 16 bits). */
	    if ( bits >= 8 ) {
		buf[bp++] = (byte)code;
		code >>= 8;
		bits -= 8;
	    }
	    /* Last bits. */
	    if(bits != 0)
		buf[bp] = (byte)code;
	    offset += n_bits;
	    if ( offset == (n_bits << 3) ) {
		bp = 0;
		bits = n_bits;
		bytes_out += bits;
		do
		    Output.putbyte(buf[bp++]);
		while(--bits != 0);
		offset = 0;
	    }

	    /*
	     * If the next entry is going to be too big for the code size,
	     * then increase it, if possible.
	     */
	    if ( free_ent > maxcode || (clear_flg > 0)) {
		/*
		 * Write the whole buffer, because the input side won't
		 * discover the size increase until after it has read it.
		 */
		if ( offset > 0 ) {
		    Output.writebytes( buf, n_bits );
		    bytes_out += n_bits;
		}
		offset = 0;

		if ( clear_flg != 0 ) {
		    n_bits = Compress.INIT_BITS;
		    maxcode = MAXCODE ();
		    clear_flg = 0;
		}
		else {
		    n_bits++;
		    if ( n_bits == maxbits )
			maxcode = maxmaxcode;
		    else
			maxcode = MAXCODE();
		}
	    }
	} else {
	    /*
	     * At EOF, write the rest of the buffer.
	     */
	    if ( offset > 0 )
		Output.writebytes( buf, ((offset + 7) / 8) );
	    bytes_out += (offset + 7) / 8;
	    offset = 0;
	}
    }


    /* table clear for block compress */
    private void cl_block() {
	int rat;

	checkpoint = in_count + CHECK_GAP;

	if(in_count > 0x007fffff) {	/* shift will overflow */
	    rat = bytes_out >> 8;
	    if(rat == 0) {		/* Don't divide by zero */
		rat = 0x7fffffff;
	    } else {
		rat = in_count / rat;
	    }
	} else {
	    rat = (in_count << 8) / bytes_out;	/* 8 fractional bits */
	}
	if ( rat > ratio ) {
	    ratio = rat;
	} else {
	    ratio = 0;
	    htab.clear();
	    free_ent = Compress.FIRST;
	    clear_flg = 1;
	    output ( (int) Compress.CLEAR );
	}
    }

final class Hash_Table {                 // moved 4/15/98 dm/kmd
/* Use protected instead of private
 * to allow access by parent class
 * of inner class. wnb 4/17/98
 */
    protected int tab[];		// for dynamic table sizing */
    protected int size;	

    public Hash_Table() {
	size = Compress.HSIZE;
	tab = new int[size];
    }

    public int of(int i) {
	return tab[i];
    }

    public void set(int i, int v) {
	tab[i] = v;
    }

    public int hsize() {
	return size;
    }

    public void clear() {
	int i;

	for (i = 0; i < size; i++) {
	    tab[i] = -1;
	}
    }
};

};


/*****************************************************************/

/*
 * Decompress stdin to stdout.  This routine adapts to the codes in the
 * file building the "string" table on-the-fly; requiring no table to
 * be stored in the compressed file.  The tables used herein are shared
 * with those of the compress() routine.  See the definitions above.
 */

final class Decompressor extends Comp_Base {
    private int size;

    private Code_Table tab_prefix;
    private Suffix_Table tab_suffix;
    private De_Stack de_stack;

    public Decompressor(Input_Buffer in, Output_Buffer out) {
	super(in, out);

	/* Check the magic number */
	if (((Input.getbyte() & 0xFF) != (Compress.magic_header[0] & 0xFF)) ||
	    ((Input.getbyte() & 0xFF)  != (Compress.magic_header[1] & 0xFF))) {
	    System.err.println("stdin: not in compressed format");
	    //System.exit(1);
	}

	maxbits = Input.getbyte();	/* set -b from file */
	block_compress = maxbits & Compress.BLOCK_MASK;
	maxbits &= Compress.BIT_MASK;
	maxmaxcode = 1 << maxbits;
	if (maxbits > Compress.BITS) {
	    System.err.println("stdin: compressed with " + maxbits +
			       " bits, can only handle " + Compress.BITS +
			       " bits");
	    //System.exit(1);
	}
	n_bits = Compress.INIT_BITS;
	maxcode = MAXCODE();

	offset = 0;
	size = 0;
	clear_flg = 0;
	free_ent = ((block_compress != 0) ? Compress.FIRST : 256 );

	tab_prefix = new Code_Table();
	tab_suffix = new Suffix_Table();
	de_stack = new De_Stack();

	/*
	 * As above, initialize the first 256 entries in the table.
	 */
	tab_prefix.clear(256);
	tab_suffix.init(256);
    }


    public void decompress() {
	int finchar;
	int code, oldcode, incode;

	finchar = oldcode = getcode();
	if(oldcode == -1)		/* EOF already? */
	    return;			/* Get out of here */
	Output.putbyte( (byte)finchar ); /* first code must be 8 bits = byte */

	while ( (code = getcode()) > -1 ) {

	    if ( (code == Compress.CLEAR) && (block_compress != 0) ) {
		tab_prefix.clear(256);
		clear_flg = 1;
		free_ent = Compress.FIRST - 1;
		if ( (code = getcode ()) == -1 ) /* O, untimely death! */
		    break;
	    }
	    incode = code;
	    /*
	     * Special case for KwKwK string.
	     */
	    if ( code >= free_ent ) {
		de_stack.push((byte)finchar);
		code = oldcode;
	    }

	    /*
	     * Generate output characters in reverse order
	     */
	    while ( code >= 256 ) {
		de_stack.push(tab_suffix.of(code));
		code = tab_prefix.of(code);
	    }
	    de_stack.push((byte)(finchar = tab_suffix.of(code)));

	    /*
	     * And put them out in forward order
	     */
	    do
		Output.putbyte ( de_stack.pop());
	    while ( !de_stack.is_empty());

	    /*
	     * Generate the new entry.
	     */
	    if ( (code=free_ent) < maxmaxcode ) {
		tab_prefix.set(code, oldcode);
		tab_suffix.set(code, (byte)finchar);
		free_ent = code+1;
	    } 
	    /*
	     * Remember previous code.
	     */
	    oldcode = incode;
	}
    }

    /*
     * Read one code from the standard input.  If EOF, return -1.
     * Inputs:
     * 	stdin
     * Outputs:
     * 	code or -1 is returned.
     */

    private int getcode() {
	int code;
	int r_off, bits;
	int bp = 0;

	if ( clear_flg > 0 || offset >= size || free_ent > maxcode ) {
	    /*
	     * If the next entry will be too big for the current code
	     * size, then we must increase the size.  This implies reading
	     * a new buffer full, too.
	     */
	    if ( free_ent > maxcode ) {
		n_bits++;
		if ( n_bits == maxbits )
		    maxcode = maxmaxcode; /* won't get any bigger now */
		else
		    maxcode = MAXCODE();
	    }
	    if ( clear_flg > 0) {
		n_bits = Compress.INIT_BITS;
		maxcode = MAXCODE ();
		clear_flg = 0;
	    }
	    size = Input.readbytes( buf, n_bits );
	    if ( size <= 0 )
		return -1;		/* end of file */
	    offset = 0;
	    /* Round size down to integral number of codes */
	    size = (size << 3) - (n_bits - 1);
	}
	r_off = offset;
	bits = n_bits;
	/*
	 * Get to the first byte.
	 */
	bp += (r_off >> 3);
	r_off &= 7;
	/* Get first part (low order bits) */
	code = ((buf[bp++] >> r_off) & Compress.rmask[8 - r_off]) & 0xff;
	bits -= (8 - r_off);
	r_off = 8 - r_off;		/* now, offset into code word */
	/* Get any 8 bit parts in the middle (<=1 for up to 16 bits). */
	if ( bits >= 8 ) {
	    code |= (buf[bp++] & 0xff) << r_off;
	    r_off += 8;
	    bits -= 8;
	}
	/* high order bits. */
//	code |= (buf[bp] & Compress.rmask[bits]) << r_off;  // kmd
	// Don McCauley/kmd  - IBM 02/26/98
	if ( bits > 0 ) code |= (buf[bp] & Compress.rmask[bits]) << r_off;
	offset += n_bits;

	return code;
    }

/*****************************************************************/

final class De_Stack {                         // moved 4/15/98 dm/kmd
/* Use protected instead of private
 * to allow access by parent class
 * of inner class. wnb 4/17/98
 */
    protected byte tab[];
    protected int index;

    public De_Stack() {
	tab = new byte[Compress.STACK_SZ];
	index = 0;
    }

    public void push(byte c) {
	tab[index++] = c;
    }

    public byte pop() {
	index--;
	return tab[index];
    }

    public boolean is_empty() {
	return (index == 0);
    }
};

/*****************************************************************/

final class Suffix_Table {                     // moved 4/15/98 dm/kmd
/* Use protected instead of private
 * to allow access by parent class
 * of inner class. wnb 4/17/98
 */
    protected byte tab[];

    public Suffix_Table () {
	tab = new byte[Compress.SUFFIX_TAB_SZ];
    }

    public byte of(int i) {
	return tab[i];
    }

    public void set(int i, byte v) {
	tab[i] = v;
    }

    public void init(int size) {
	int code;
	for ( code = 0; code < size; code++ ) {
	    tab[code] = (byte)code;
	}
    }
};

};


