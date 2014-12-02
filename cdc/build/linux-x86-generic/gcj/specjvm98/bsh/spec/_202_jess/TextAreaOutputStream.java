package spec.benchmarks._202_jess;

import java.io.*;
import java.awt.*;

/*
   TextAreaOutputStream: a simple output stream, suitable for constructing
   a PrintStream, which uses a TextArea as its output.

   Simple, but nice!

   Ernest J. Friedman-Hill
   Scientific Computing Department
   Sandia National Labs, California
   ejfried@ca.sandia.gov
*/

public class TextAreaOutputStream extends OutputStream {

  String str;
  TextArea ta;

  // the constructor. Call this with an already constructed TextArea
  // object, which you can put wherever you'd like.

  public TextAreaOutputStream(TextArea area) {
    str = new String();
    ta = area;
  }

  public void close() throws IOException {
    // nothing to close, really.
  }

  public void flush() throws IOException {
    ta.appendText(str);
    str = "";    
  }

  public void write(int b) throws IOException {
    str += String.valueOf((char) b);
  }
  
  public void write(byte b[]) throws IOException {
    str += new String(b, 0, b.length);
  }
  
  public void write(byte b[], int off, int len) throws IOException {
    str += new String(b, off, len);

  }


}
