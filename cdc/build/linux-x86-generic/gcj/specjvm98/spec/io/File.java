/*
 * @(#)File.java	2.2 09/08/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */
package spec.io;

/** 
 This class extends the java.io.File and provides some extra functionality. 
 File object maintains a static table of files called the Table of existing
 files. The files listed in the TableOfExisting files are loaded from the
 local disk. All the other files are loaded over the network.
*/
public class File extends java.io.File{

/** 
 Variable representing the file name
 */
  
  String filename;

/** 
 Table to hold the ListOfExisting files
 */
  private static TableOfExistingFiles tef = new TableOfExistingFiles();
  
/**
 Constructor.
 @param Path of the file (includes the file name also)
 */
  public File(String path){

    super(path);

    //System.out.println("DEBUG: Inside constructor File(" + path + ")");

    //filename = path;
    filename = getPath();

  }

/**
 Overloaded constructor which takes the path and the files name to load the
 file
 @param path Directory of the file
 @param name file name
 */
  public File(String path , String name){

    super(path , name);

    //    System.err.println("DEBUG: Inside constructor File(" + path + " , " + name + ")");

    //filename = path + name;
    filename = getPath();

  }

/**
 Overloaded constructor which takes the directory in the File object format
 and a name of the file.
 @param dir Directory of the file in the File object format
 @param name file name
 */
  public File(File dir , String name){

    super(dir , name);

    //System.out.println("Inside constructor File(dir, name) , dir.getName = " + dir.getName());
    //    System.err.println("DEBUG: File constructor, dir.getPath() = " + dir.getPath() + " name = " + name);
    //filename = dir.getPath() + name;
    filename = getPath();

  }

/**
 returns the name of the file
 @return name of file
 */
  public String getName(){

    String returnvalue = super.getName();

    //System.out.println("Inside File.getName() , \nfilename = " + filename + "\nreturnvalue = " +returnvalue);

    
    return returnvalue;

  }

/**
 returns the path of the file
 */
  public String getPath(){

    String returnvalue = super.getPath();
    
    //System.out.println("Inside File.getPath() ,  filename = " + filename + " returnvalue = " +returnvalue);

    return returnvalue;

  }

/**
 returns tha absolutePath of the file Dir + name
 */
  public String getAbsolutePath(){

    String returnvalue = super.getAbsolutePath();
   
    //System.out.println("Inside File.getAbsolutePath() ,  filename = " + filename + " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
 returns the Parent directory name
 */

  public String getParent(){

    String returnvalue = super.getParent();

    //String returnvalue = null;

    //System.out.println("Inside File.getParent() ,  filename = " + filename + " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
 Checks whether the given file name exists or not
 */
  public boolean exists(){

    //boolean returnvalue = super.exists();
    String tolook = filename.replace( '\\', '/' );
    //System.err.println("DEBUG: filename used for exists = " + filename);
    boolean returnvalue = tef.exists( tolook);

    //System.out.println("Inside File.exists() ,  filename = " + filename + " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
 Checks whether the given file is writablle or not
 */  
  public boolean canWrite(){
    
    boolean returnvalue = super.canWrite();

    //System.out.println("Inside File.canWrite() ,  filename = " + filename + " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
 Checks whether the given file is readable
 */
  public boolean canRead(){
    
    boolean returnvalue = super.canRead();

    //System.out.println("Inside File.canRead() ,  filename = " + filename + " returnvalue = " + returnvalue);

    return returnvalue;

  }  

/**
 Checks whether the given file name is a file or directory
 The functionality is modified to return whether the file name is classes.zip
 or not
 */
  public boolean isFile(){
   
    //boolean returnvalue = super.isFile();

    boolean returnvalue = filename.endsWith("classes.zip");

    //System.out.println("Inside File.isFile() ,  filename = " + filename + " returnvalue = " + returnvalue);

    return returnvalue;

  }  

/**
 Checks whether the given file name is directory or not
 */
  public boolean isDirectory(){
    
    boolean returnvalue = super.isDirectory();

    //System.out.println("Inside File.isDirectory() ,  filename = " + filename + " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
 Returns the date of last modification
 */
  public long lastModified(){

    long returnvalue = super.lastModified();
    
    //System.out.println("Inside File.lastModified() ,  filename = " + filename + " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
 Returns the length of the file
 */
  public long length(){
    
    long returnvalue = super.length();

    //System.out.println("Inside File.length() ,  filename = " + filename + " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
 Creates a new directory
 */
  public boolean mkdir(){

    boolean returnvalue = super.mkdir();

    //System.out.println("Inside File.mkdir() ,  filename = " + filename + " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
 Renames the file
 */
  public boolean renameTo(File dest){

    boolean returnvalue = super.renameTo(dest);

    //System.out.println("Inside File.renameTo() ,  filename = " + filename +  " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
  Creates a directory whose pathname is specified by this File object, 
  including any necessary parent directories. 
 */  
  public boolean mkdirs() {

    boolean returnvalue = super.mkdirs();

    //System.out.println("Inside File.mkdirs() ,  filename = " + filename +  " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
 Returns a list of the files in the directory specified by this File object
*/
  public String[] list(){
 
    String[] returnvalue = super.list();
    
    //System.out.println("Inside File.list() ,  filename = " + filename +  " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
 Returns a list of the files in the directory specified by this File that 
 satisfy the specified filter. 
*/ 
  public String[] list(java.io.FilenameFilter filter){
 
    String[] returnvalue = super.list(filter);

    //System.out.println("Inside File.list(filter) ,  filename = " + filename +  " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
 Deletes the file specified by this object. 
 */ 
  public boolean delete(){

    boolean returnvalue = super.delete();

    //System.out.println("Inside File.delete() ,  filename = " + filename +  " returnvalue = " + returnvalue);
    
    return returnvalue;

  }

/**
 Computes a hashcode for the file. 
 */
  public int hashCode(){

    int returnvalue = super.hashCode();

    //System.out.println("Inside File.hashcode() ,  filename = " + filename +    " returnvalue = " + returnvalue);

    return returnvalue;

  }

/**
 Compares this object against the specified object.  
 */
  public boolean equals(Object obj){

    boolean returnvalue = super.equals(obj);

    //System.out.println("Inside File.equals() ,  filename = " + filename +  " returnvalue = " + returnvalue);

    return returnvalue;
  }

/**
 Returns a string representation of this object. 
*/  
  public String toString(){

    String returnvalue = super.toString();

    //System.out.println("Inside File.toString() ,  filename = " + filename +  " returnvalue = " + returnvalue);

    return returnvalue;

  }

}
    
    
    
    
  
    
