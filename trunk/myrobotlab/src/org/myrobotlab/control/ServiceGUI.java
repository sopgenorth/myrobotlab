/**
 *                    
 * @author greg (at) myrobotlab.org
 *  
 * This file is part of MyRobotLab (http://myrobotlab.org).
 *
 * MyRobotLab is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * MyRobotLab is distributed in the hope that it will be useful or fun,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * All libraries in thirdParty bundle are subject to their own license
 * requirements - please refer to http://myrobotlab.org/libraries for 
 * details.
 * 
 * Enjoy !
 * 
 * */

package org.myrobotlab.control;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.NotifyEntry;
import org.myrobotlab.service.interfaces.GUI;

public abstract class ServiceGUI {

	public final static Logger log = Logger.getLogger(ServiceGUI.class.getCanonicalName());

	// FIXME - Why a GUI interface - Swing will be Swing (too much abstraction)
	// FIXME - cheesy - have resizer in GUIService too because its a lame updating the interface all the time
	//final static public ComponentResizer resizer = new ComponentResizer();
	
	public final String boundServiceName;
	final GUI myService;

	GridBagConstraints gc = new GridBagConstraints();
	// index of tab in the tab panel -1 would be not displayed or displayed in custom tab
	public int myIndex = -1; 

	public JPanel display = new JPanel();

	public abstract void init();	

	public class DetachListener implements ActionListener
	{

		@Override
		public void actionPerformed(ActionEvent e) {
			log.error("undock " + boundServiceName);
			//releaseServiceButton.setVisible(false); - FIXME same functionality
			myService.undockPanel(boundServiceName);
		}
		
	}
		
	public ServiceGUI(final String boundServiceName, final GUI myService) {
		this.boundServiceName = boundServiceName;
		this.myService = myService;

		gc.anchor = GridBagConstraints.FIRST_LINE_END;

		// place menu
		gc.gridx = 0;
		gc.gridy = 0;

		//resizer.registerComponent(display);
		//resizer.registerComponent(widgetFrame);
		
		display.setLayout(new GridBagLayout());

		gc.anchor = GridBagConstraints.FIRST_LINE_START;

	}
	
	public JPanel getDisplay()
	{
		return display;
		//return widgetFrame;
	}

	/*
	 * Service functions
	 */
	public void sendNotifyRequest(String inOutMethod) 
	{
		sendNotifyRequest(inOutMethod, inOutMethod, null);
	}

	public void sendNotifyRequest(String inMethod, String outMethod) 
	{
		sendNotifyRequest(inMethod, outMethod, null);
	}
	
	public void sendNotifyRequest(String outMethod, String inMethod, Class<?> parameterType) 
	{
		NotifyEntry ne = null;
		if (parameterType != null) {
			ne = new NotifyEntry(outMethod, myService.getName(), inMethod, new Class[]{parameterType});
		} else {
			ne = new NotifyEntry(outMethod, myService.getName(), inMethod, null);
		}
		
		myService.send(boundServiceName, "notify", ne);

	}

	// TODO - more closely model java event system with addNotification or
	// addListener
	public void removeNotifyRequest(String inOutMethod) 
	{
		removeNotifyRequest(inOutMethod, inOutMethod, null);
	}

	public void removeNotifyRequest(String inMethod, String outMethod) 
	{
		removeNotifyRequest(inMethod, outMethod, null);
	}

	public void removeNotifyRequest(String outMethod, String inMethod,
			Class<?> parameterType) {

		NotifyEntry ne = null;
		if (parameterType != null) {
			ne = new NotifyEntry(outMethod, myService.getName(), inMethod, new Class[]{parameterType});
		} else {
			ne = new NotifyEntry(outMethod, myService.getName(), inMethod, null);
		}
		myService.send(boundServiceName, "removeNotify", ne);

	}
	
	public abstract void attachGUI();

	public abstract void detachGUI();
	
	public int test (int i, double d)
	{
		int x = 0;
		return x;
	}

}
