/*
 * @(#)Database.java	1.18 06/17/98
 *
 * Database.java   Version 1.0 10/16/97 rrhi, kaivalya, salina
 * Randy Heisch       IBM Corp. - Austin, TX
 *
 * Tested Kaivalya  heap highwatermark = 13795320 Mar 24 15:26:52 CST 1998
 * Reduced the need for heap size to accomodate i64bit Arch.  rrh 3/24/98
 * Data files size reduced and workload increased. rrh 3/24/98
 *                                03/11/98 rrh - null (free) objects
 *         Check for CR also - rrh 2/18/98 make Unix & Windows happy - Randy
 *         Workload also is bigger to increase the run-time 02/18/98 Randy
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1996,1997,1998 IBM Corporation, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 *
 * Permission to use, copy, modify, and distribute this software
 * and its documentation for NON-COMMERCIAL purposes and without
 * fee is hereby granted provided that this copyright notice
 * appears in all copies.
 *
 * IBM MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF
 * THE SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
 * TO THE IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE, OR NON-INFRINGEMENT. IBM SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES.
 */

package spec.benchmarks._209_db;
import spec.harness.*;
import spec.io.*;
import java.util.*;
import java.io.*;


public class Database
   {
   private Vector entries;
   private Vector fmt;
   private Entry index[];
   private int current_record;
   private String dbname = null;
   private int fnum = -1;

   public static boolean printRecords = true;

   private void read_fmt(String filename)
      {
      int len, act;
      spec.io.FileInputStream fis;
      String f;

      try {
         fis = new spec.io.FileInputStream(filename);
         Reader reader = new BufferedReader(new InputStreamReader(fis));
	 StreamTokenizer tok = new StreamTokenizer(reader);

         tok.commentChar(0);
         tok.ordinaryChar('/');
         tok.ordinaryChar(';');
         tok.ordinaryChar('*');
         tok.ordinaryChar('+');
         tok.ordinaryChar('-');
         tok.ordinaryChar('&');
         tok.ordinaryChar('|');
         tok.slashSlashComments(true);
         tok.slashStarComments(true);
         //tok.parseNumbers();


         while ( true )
            {
            tok.nextToken();

            if  ( tok.ttype == StreamTokenizer.TT_EOF )
               break;

            switch(tok.ttype)
               {
               case StreamTokenizer.TT_NUMBER:
                  spec.harness.Context.out.println("token=TT_NUMBER: "+tok.nval);
               break;

               case StreamTokenizer.TT_WORD:
                  spec.harness.Context.out.println("token=TT_WORD:   "+tok.sval);
               break;

               case '"':

                  if ( tok.sval.charAt(0) == '%' )
                     f = null;
                  else
                     f = new String(tok.sval);

                  fmt.addElement(f);

               break;
               }
            }


         fis.close();
	 fis = null;      // 03/11/98 rrh
	 tok = null;      // 03/11/98 rrh
         }
      catch (IOException e)
         {
         spec.harness.Context.out.println("ERROR opening/parsing format file "+filename);
         System.exit(1);
         };

      }

   Database(String s)
      {
      entries = new Vector();
      fmt = new Vector();
      dbname = s;
      read_fmt(s+".fmt");
      read_db(s+".dat");
      index = null;
      }

   public int numRecords() { return entries.size(); }

   public void read_db(String filename)
      {
      Entry entry;

      int i;
      int n = 0, act = 0, e, s;
      boolean OK;
      byte buffer[] = null;

      spec.harness.Context.out.print("Reading database "+dbname+" ... ");
      spec.harness.Context.out.flush();

      try {
          spec.io.FileInputStream sif = new spec.io.FileInputStream(filename);
          n = sif.getContentLength();
          buffer = new byte[n];

          int bytes_read;
          while ( (bytes_read = sif.read(buffer, act , (n - act))) > 0){
            act = act +  bytes_read;
          }
          sif.close();
	  sif = null;           // 03/11/98 rrh
          if ( act != n ){
            spec.harness.Context.out.println("ERROR reading input file");
            //System.exit(1);
            return;
          }

      }
      catch (IOException ioe)
      {
      if ( n == 0 )
         {
         spec.harness.Context.out.println("Empty database");
         return;
         }

      spec.harness.Context.out.println("ERROR opening/reading input file \""+filename+"\"");
//    System.exit(1);
      };

      entry = new Entry();

      spec.harness.Context.out.print("OK\nBuilding database ...");
      spec.harness.Context.out.flush();

      n = buffer.length;
      s = e = 0;
      while ( (e < n) && (s < n) )
         {
         // Check for CR also - rrh 2/18/98
         while ( (e < n) && (buffer[e] != '\n') && (buffer[e] != '\r') ) e++;

         if ( e < n )
            {
            if ( buffer[s] == '#' )
               {
               add(entry);
               entry = new Entry();
               }
            else
               entry.items.addElement(new String(buffer, s, e-s));

            // Discard CR & LF - rrh 2/18/98
            while ( (e < n) && ((buffer[e] == '\n') || (buffer[e] == '\r')) )
               e++;

            s = e;
            }
         }

      buffer = null;     // 03/11/98 rrh

      spec.harness.Context.out.println("Done.");
      }


   public void write_db()
      {
      Entry entry;
      String s;
      Enumeration e = entries.elements();
      spec.io.FileOutputStream fos = null;
      byte buffer[] = new byte[64*1024];
      int c, len;


      spec.harness.Context.out.print("Saving database "+dbname+" ... ");
      spec.harness.Context.out.flush();

      try
         {
         fos = new spec.io.FileOutputStream(dbname+".dat");
         }
      catch (IOException ex)
         {
         spec.harness.Context.out.println("\nERROR creating output file "+dbname+".dat");
//       System.exit(1);
         }

      Enumeration i;

      c = 0;
      while ( e.hasMoreElements() )
         {
         entry = (Entry)e.nextElement();

         i = entry.items.elements();

         while ( i.hasMoreElements() )
            {
            s = (String)i.nextElement() + "\n";

            len = s.length();

            if ( (len+c) > buffer.length )
               {
               try {fos.write(buffer, 0, c);} catch(IOException ex)
                  {
                  spec.harness.Context.out.println("ERROR writing to output file "+dbname+".dat");
//                System.exit(1);
                  }

               c = 0;
               }

            byte[] tempbuf = s.getBytes();
	    System.arraycopy(tempbuf, 0, buffer, c, tempbuf.length);
	    
// kmd      s.getChars(0, len, buffer, c);
            c += len;
            }


         s = "#\n";

         len = s.length();

         if ( (len+c) > buffer.length )
            {
            try {fos.write(buffer, 0, c);} catch(IOException ex)
               {
               spec.harness.Context.out.println("ERROR writing to output file "+dbname+".dat");
//             System.exit(1);
               }

            c = 0;
            }

	 byte[] tempbuf = s.getBytes();
	 System.arraycopy(tempbuf, 0, buffer, c, tempbuf.length);

// kmd   s.getChars(0, len, buffer, c);

         c += len;
         }


      if ( c > 0 )
         try {
             fos.write(buffer, 0, c);
             fos.close();
             }
         catch(IOException ex)
            {
            spec.harness.Context.out.println("ERROR writing to output file "+dbname+".dat");
//          System.exit(1);
            }

      buffer = null;     // 03/11/98 rrh
      fos = null;        // 03/11/98 rrh

      spec.harness.Context.out.println("Done.");
      }


   private void set_index()
      {
      int i, n;
      Enumeration e = entries.elements();

      n = entries.size();

      index = null;
      index = new Entry[n];

      i = 0;
      while ( e.hasMoreElements() )
         index[i++] = (Entry)e.nextElement();

      e = null;        // 03/11/98 rrh
      }

   public void end()
      {
      if ( index == null ) set_index();
      current_record = index.length - 1;
      printRec();
      }

   public void list()
      {
      current_record = 0;

      if ( index == null ) set_index();

      printRec();
      }


   public void gotoRec(int rec)
      {
      rec--;

      if ( index == null ) set_index();

      if ( (rec < index.length) && (rec >= 0) )
         {
         current_record = rec;

         printRec();
         }
      else
         spec.harness.Context.out.println("Invalid record number ("+(rec+1)+")");
      }


   public void next()
      {
      if ( index == null ) set_index();

      if ( current_record < (index.length-1) )
         {
         current_record++;

         printRec();
         }
      }


   public void previous()
      {
      if ( index == null ) set_index();

      if ( current_record > 0 )
         {
         current_record--;

         printRec();
         }
      }

   public int currentRec() { return current_record; }

   public void printRec()
      {
      String s;
      Entry entry;

      if ( index == null ) set_index();

      if ( (current_record >= index.length) || (current_record < 0) )
         return;

      spec.harness.Context.out.println("---- Record number "+(current_record+1)+" ----");

      entry = index[current_record];


      Enumeration i = entry.items.elements();
      Enumeration f = fmt.elements();

      while ( f.hasMoreElements() )
         {
         s = ((String)f.nextElement());

         if ( s != null )
            spec.harness.Context.out.print(s);
         else
            {
            s = (String)i.nextElement();
            spec.harness.Context.out.println(s);
            }
         }

      i = null;     // 03/11/98 rrh
      f = null;     // 03/11/98 rrh

      spec.harness.Context.out.println();
      }



   public void add(Entry entry)
      {
      //String s;         // 03/11/98 rrh
      //Hashtable ht;     // 03/11/98 rrh

      entries.addElement(entry);

      index = null;
      fnum = -1;
      }



   public Entry getEntry(DataInputStream dis)
      {
      String s = null;
      String field;

      Entry entry = new Entry();

      Enumeration f = fmt.elements();

      while ( f.hasMoreElements() )
         {
         field = (String)f.nextElement();

         if ( field != null )
            {
            // These create too much output for benchmark - rrh
            //spec.harness.Context.out.print(field);
            //spec.harness.Context.out.flush();
            }
         else
            {
            try { s = dis.readLine(); }
            catch (IOException e)
               {
               spec.harness.Context.out.println("input error");
//             System.exit(1);
               }

            entry.items.addElement(s);
            }
         }

      f = null;            // 03/11/98 rrh

      return entry;
      }


   public void modify(DataInputStream dis)
      {
      String s = null;
      String field, os;
      int fn = 0;

      if ( index == null )
         return;

      Enumeration f = fmt.elements();

      while ( f.hasMoreElements() )
         {
         field = (String)f.nextElement();

         if ( field != null )
            {
            // Reduce output
            //spec.harness.Context.out.print(field);
            //spec.harness.Context.out.flush();
            }
         else
            {
            os = (String)(((Entry)index[current_record]).items.elementAt(fn));
            // Reduce output
            //spec.harness.Context.out.print(" ("+os+") ");
            //spec.harness.Context.out.flush();

            try { s = dis.readLine(); }
            catch (IOException e)
               {
               spec.harness.Context.out.println("input error");
//             System.exit(1);
               }

            if ( s.length() > 0 )
               os = s;

            ((Entry)index[current_record]).items.setElementAt(os, fn);

            fn++;
            }
         }

      f = null;     // 03/11/98 rrh

      fnum = -1;
      }


   public void status()
      {
      if ( index == null ) set_index();
      spec.harness.Context.out.println("Record "+(current_record+1)+" of "+index.length);
      }

   private String fieldValue;

   private int getfield(DataInputStream dis)
      {
      String fs;
      int fn;


      if ( index == null ) set_index();


      Entry entry = new Entry();

      Enumeration f = fmt.elements();

      fn = 0;
      while ( f.hasMoreElements() )
         {
         fs = (String)f.nextElement();

         if ( fs != null )
            {
            // Reduce output
            //spec.harness.Context.out.print(fs);
            //spec.harness.Context.out.flush();
            }
         else
            {
            try { fieldValue = dis.readLine(); }
            catch (IOException ex)
               {
               spec.harness.Context.out.println("input error");
//             System.exit(1);
               }

            if ( fieldValue.length() > 0 )
               break;
            else
               fn++;
            }
         }

      f = null;       // 03/11/98 rrh

      if ( fn >= index[0].items.size() ) return -1;
      else return fn;
      }



   public void sort(DataInputStream dis)
      {
      int fn;

      fn = getfield(dis);

      if ( fn < 0 ) return;

      if ( fn != fnum )
         shell_sort(fn);
      }


   public void find(DataInputStream dis)
      {
      int fn, rec;

      fn = getfield(dis);

      if ( fn != fnum )
         {
         // Reduce output
         //spec.harness.Context.out.print("Sorting on requested fieldname ... ");
         //spec.harness.Context.out.flush();
         shell_sort(fn);
         }
      else
         {
         //spec.harness.Context.out.print("Already sorted");
         }


      // Reduce output
      //spec.harness.Context.out.print("\nSearching ... ");
      //spec.harness.Context.out.flush();

      if ( (rec = lookup(fieldValue, fnum)) < 0 )
         spec.harness.Context.out.println("NOT found");
      else
         {
         spec.harness.Context.out.println();

         while ( rec >= 0 )
            {
            rec--;

            if ( fieldValue.compareTo(
                 (String)index[rec].items.elementAt(fnum)) != 0 )
               break;
            }

         current_record = rec + 1;
         printRec();
         }
      }


   // Binary search the alpha sorted index list
   public int lookup(String s, int fn)
      {
      int rc, i = 0, first, last;
      boolean found;

      first = 0;
      last = index.length - 1;
      found = false;

      while ( (first <= last) && !found )
         {
         i = (first+last) >> 1;

         rc = s.compareTo((String)index[i].items.elementAt(fn));

         if ( rc == 0 ) found = true;
         else
         if ( rc < 0 ) last = i - 1;
         else
            first = i + 1;
         }

      if ( found ) return i;
      else return -1;
      }



   void shell_sort(int fn)
      {
      int i, j, gap;
      int n;
      String s1, s2;
      Entry e;

      if ( index == null ) set_index();


      n = index.length;

      for (gap = n/2; gap > 0; gap/=2)
         for (i = gap; i < n; i++)
            for (j = i-gap; j >=0; j-=gap)
               {
               s1 = (String)index[j].items.elementAt(fn);
               s2 = (String)index[j+gap].items.elementAt(fn);

               if ( s1.compareTo(s2) <= 0 ) break;

               e = index[j];
               index[j] = index[j+gap];
               index[j+gap] = e;
               }

      fnum = fn;
      }



   public void remove()
      {

      if ( index == null ) set_index();

      entries.removeElement(index[current_record]);

      if ( current_record == (index.length-1) )
         current_record--;

      index = null;
      fnum = -1;
      }



   }

