package org.omg.PortableServer;


/**
* org/omg/PortableServer/LifespanPolicyValue.java .
* Generated by the IDL-to-Java compiler (portable), version "3.1"
* from ../../../../src/share/classes/org/omg/PortableServer/poa.idl
* Wednesday, October 18, 2006 9:56:44 AM GMT-08:00
*/


/**
	 * The LifespanPolicyValue can have the following values.
	 * TRANSIENT - The objects implemented in the POA 
	 * cannot outlive the POA instance in which they are 
	 * first created. 
	 * PERSISTENT - The objects implemented in the POA can 
	 * outlive the process in which they are first created. 
	 */
public class LifespanPolicyValue implements org.omg.CORBA.portable.IDLEntity
{
  private        int __value;
  private static int __size = 2;
  private static org.omg.PortableServer.LifespanPolicyValue[] __array = new org.omg.PortableServer.LifespanPolicyValue [__size];

  public static final int _TRANSIENT = 0;
  public static final org.omg.PortableServer.LifespanPolicyValue TRANSIENT = new org.omg.PortableServer.LifespanPolicyValue(_TRANSIENT);
  public static final int _PERSISTENT = 1;
  public static final org.omg.PortableServer.LifespanPolicyValue PERSISTENT = new org.omg.PortableServer.LifespanPolicyValue(_PERSISTENT);

  public int value ()
  {
    return __value;
  }

  public static org.omg.PortableServer.LifespanPolicyValue from_int (int value)
  {
    if (value >= 0 && value < __size)
      return __array[value];
    else
      throw new org.omg.CORBA.BAD_PARAM ();
  }

  protected LifespanPolicyValue (int value)
  {
    __value = value;
    __array[__value] = this;
  }
} // class LifespanPolicyValue
