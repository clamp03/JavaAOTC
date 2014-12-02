// -*- java -*-
//////////////////////////////////////////////////////////////////////
// LostDisplay.java
// A JES GUI That looks like the Lost in Space title
//
// (C) 1997 E.J.Friedman-Hill and Sandia National Labs
// $Id: LostDisplay.java,v 1.2 1997/04/08 23:31:36 ejfried Exp $
//////////////////////////////////////////////////////////////////////

package spec.benchmarks._202_jess;

/**
 A Fancy JES GUI that looks like Lost in Space
@author E.J. Friedman-Hill (C)1996
*/

import spec.benchmarks._202_jess.jess.*;
import java.awt.*;
import java.io.*;
import java.applet.*;

public class LostDisplay extends Panel implements ReteDisplay {

  Color[][] colors;
  Rectangle[][] locations;
  PrintStream os;
  InputStream is;
  Applet app;

  public final static int COLS = 25;

  /**
    Constructor
   */

  public LostDisplay(PrintStream os, InputStream is, Applet app) {
    this.app = app;
    priv_init(os,is);
  }

  public LostDisplay(PrintStream os, InputStream is) {
    this.app = null;
    priv_init(os,is);
  }
  
  private void priv_init (PrintStream os, InputStream is) {
      this.os = os;

    // initialize the colors of the panel lights
    colors = new Color[3][COLS];
    for (int i=0; i<3; i++)
      for (int j=0; j<COLS; j++)
        colors[i][j] = Color.black;
    
    // initialize the locations of the panel lights
    locations = new Rectangle[3][COLS];
    for (int i=0; i<3; i++)
      for (int j=0; j<COLS; j++)
        locations[i][j] = 
          new Rectangle(j*15 + 1, i*15 + 1, 13, 13);
  }  
  public void update(Graphics g) {
    paint(g);
  }

  private void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (java.lang.InterruptedException ex) {
      // so?
    }
  }

  public void paint(Graphics g) {
    Color oldColor = g.getColor();
    for (int i=0; i<3; i++)
      for (int j=0; j<COLS; j++) {
        Rectangle r = locations[i][j];
        g.setColor(colors[i][j]);
        g.fillRect(r.x, r.y, r.width, r.height);
      }
    g.setColor(oldColor);
  }

  public void AssertFact(ValueVector fact) {
    int id;
    try {
      id = fact.get(RU.ID).FactIDValue();
    } catch (ReteException re) {
      id = 0;
    }
    id = id % COLS;
    if (colors[0][id] == Color.red)
      colors[0][id] = Color.green;
    else
      colors[0][id] = Color.red;
    repaint();
    sleep(5);
  }

  public void RetractFact(ValueVector fact) {
    int id;
    try {
      id = fact.get(RU.ID).FactIDValue();
     } catch (ReteException re) {
       id = 0;
     }
    id = id % COLS;
    if (colors[0][id] == Color.green)
      colors[0][id] = Color.red;
    else
      colors[0][id] = Color.green;
    repaint();
    sleep(5);
  }

  public void AddDeffacts(Deffacts df) {
    int id = df.getName() > 0 ? df.getName() : -df.getName();
    id = id % COLS;
    if (colors[1][id] == Color.yellow)
      colors[1][id] = Color.red;
    else
      colors[1][id] = Color.yellow;
    repaint();
    sleep(5);
  }
  public void AddDeftemplate(Deftemplate dt) {
    int id = dt.getName() > 0 ? dt.getName() : -dt.getName();
    id = id % COLS;
    if (colors[1][id] == Color.green)
      colors[1][id] = Color.yellow;
    else
      colors[1][id] = Color.green;
    repaint();
    sleep(5);
  }

  public void AddDefrule(Defrule rule) {
    int id = rule.getName() > 0 ? rule.getName() : -rule.getName();
    id = id % COLS;
    if (colors[1][id] == Color.blue)
      colors[1][id] = Color.red;
    else
      colors[1][id] = Color.blue;
    repaint();
    sleep(5);
  }

  public void ActivateRule(Defrule rule) {
    int id = rule.getName() > 0 ? rule.getName() : -rule.getName();
    id = id % COLS;
    if (colors[2][id] == Color.red)
      colors[2][id] = Color.blue;
    else
      colors[2][id] = Color.red;
    repaint();
    sleep(5);
  }
  public void DeactivateRule(Defrule rule) {
    int id = rule.getName() > 0 ? rule.getName() : -rule.getName();
    id = id % COLS;
    if (colors[2][id] == Color.green)
      colors[2][id] = Color.red;
    else
      colors[2][id] = Color.green;
    repaint();
    sleep(5);
  }
  public void FireRule(Defrule rule) {
    int id = rule.getName() > 0 ? rule.getName() : -rule.getName();
    id = id % COLS;
    if (colors[2][id] == Color.yellow)
      colors[2][id] = Color.blue;
    else
      colors[2][id] = Color.yellow;
    repaint();
    sleep(5);
    System.gc();
  }

  public PrintStream stdout() {
    sleep(5);
    return os;
  }
  public java.io.InputStream stdin() {
    sleep(5);
    return is;
  }

  public PrintStream stderr() {
    sleep(5);
    return os;
  }

  public Applet applet() {
    return app;
  }
}

