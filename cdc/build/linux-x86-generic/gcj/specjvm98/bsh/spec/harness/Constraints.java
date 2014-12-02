/*
 * @(#)Constraints.java	1.6 06/17/98
 *
 * Copyright (c) 1998 Standard Performance Evaluation Corporation (SPEC)
 *               All rights reserved.
 * Copyright (c) 1997,1998 Sun Microsystems, Inc. All rights reserved.
 *
 * This source code is provided as is, without any express or implied warranty.
 */

package spec.harness;

import java.awt.*;
import java.applet.*;
import java.net.*;
import java.io.*;
import java.util.*;

 /**
  * This class is used to define the constraints for the Gridbaglayout
  * in an easy way. 
  * @see java.awt.GridBagLayout
  * @see java.awt.GridBagConstraints
  */

public class Constraints {
                   
    public void constrain(
                           Container container,
                           Component component, 
                           int grid_x,
                           int grid_y,
                           int grid_width,
                           int grid_height,
                           int fill,
                           int anchor,
                           double weight_x,
                           double weight_y,
                           int top,
                           int left,
                           int bottom,
                           int right
                         ) {
        
        GridBagConstraints c = new GridBagConstraints();
        
        c.gridx = grid_x; 
        c.gridy = grid_y;
        
        c.gridwidth = grid_width; 
        c.gridheight = grid_height;
        
        c.fill = fill; 
        c.anchor = anchor;
        
        c.weightx = weight_x; 
        c.weighty = weight_y;
        
        if (top+bottom+left+right > 0)
            c.insets = new Insets(top, left, bottom, right);
                    
        ((GridBagLayout)container.getLayout()).setConstraints(component, c);
        
        container.add(component);
        
    }
    
        
    public void constrain(
                           Container container,
                           Component component, 
                           int grid_x,
                           int grid_y,
                           int grid_width,
                           int grid_height
                         ) {
        
        constrain(
                   container,
                   component,
                   grid_x,
                   grid_y, 
                   grid_width,
                   grid_height,
                   GridBagConstraints.NONE, 
                   GridBagConstraints.NORTHWEST,
                   0.0,
                   0.0,
                   0,
                   0,
                   0,
                   0
                 );        
    }
    
        
    public void constrain(
                           Container container,
                           Component component, 
                           int grid_x,
                           int grid_y,
                           int grid_width,
                           int grid_height,
                           int top,
                           int left,
                           int bottom,
                           int right
                         ) {
        
        constrain(
                   container,
                   component,
                   grid_x,
                   grid_y, 
                   grid_width,
                   grid_height,
                   GridBagConstraints.NONE, 
                   GridBagConstraints.NORTHWEST, 
                   0.0,
                   0.0,
                   top,
                   left,
                   bottom,
                   right
                 );
        
    }
    
}

