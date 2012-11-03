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

package org.myrobotlab.service;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.service.interfaces.ServoControl;
import org.myrobotlab.service.interfaces.ServoController;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root
public class Servo extends Service implements ServoControl {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger.getLogger(Servo.class.getCanonicalName());

	ServoController controller = null;
	
	@Element
	private Integer position = null; // position -1 invalid not set yet
	@Element
	private int positionMin = 0;
	@Element
	private int positionMax = 180;
	
	// FIXME - should be implemented inside the Arduino / ServoController - but has to be tied to position
	int sweepStart = 0;
	int sweepEnd = 180;
	int sweepDelayMS = 1000;
	int sweepIncrement = 1;
	boolean sweeperRunning = false;
	transient Thread sweeper = null;
	
	public Servo(String n) {
		super(n, Servo.class.getCanonicalName());
		load();
	}

	@Override
	public void loadDefaultConfiguration() {
	}


	@Override
	public boolean setController(ServoController controller) {
		if (controller == null)
		{
			log.error("setting null as controller");
			return false;
		}
		this.controller = controller;
		return true;
	}

	/**
	 * simple move servo to the location desired
	 */
	@Override
	public void moveTo(Integer newPos) {
		if (newPos == null)
		{
			return;
		}
		if (controller == null)
		{
			log.error(String.format("%s's controller is not set", getName()));
			return;
		}
		if (newPos >= positionMin && newPos <= positionMax )
		{
			controller.servoWrite(getName(), newPos);
			position = newPos;
		} else {
			log.error(String.format("Servo.moveTo(%d) out of range", newPos));
		}
	}

	/**
	 * moves the servo in the range -1.0 to 1.0
	 * where in a typical servo
	 * 		-1.0 = 0
	 * 		 0.0 = 90
	 * 		 1.0 = 180
	 * setting the min and max will affect the range
	 * where -1.0 will always be the minPos and 1.0 will be maxPos
	 */
	@Override
	public void move(Float amount) 
	{
		if (amount > 1 || amount < -1)
		{
			log.error("Servo.move %d out of range");
			return;
		}
		int range = positionMax - positionMin;
		int newPos = Math.abs((int)(range/2 * amount - range/2));
		
		controller.servoWrite(getName(), newPos);
		position = newPos;

	}
	
	public boolean isAttached() {
		return controller != null;
	}
	
	public void setPositionMin(Integer min)
	{
		this.positionMin = min; 
	}

	@Override
	public void setPositionMax(Integer max) {
		this.positionMax = max; 
	}


	public Integer getPosition() {
		return position;
	}

	@Override
	public String getToolTip() {
		return "<html>service for a servo</html>";
	}
	
	
	/**
	 * Sweeper - TODO - should be implemented in the arduino code for smoother function
	 *
	 */
	private class Sweeper implements Runnable { 
		@Override
		public void run() {

			while (sweeperRunning) {
				// controller.servoMoveTo(name, position);
				position += sweepIncrement;

				// switch directions
				if ((position <= sweepStart && sweepIncrement < 0)
						|| (position >= sweepEnd && sweepIncrement > 0)) {
					sweepIncrement = sweepIncrement * -1;
				}

				moveTo(position);
				
				try {
					Thread.sleep(sweepDelayMS);
				} catch (InterruptedException e) {
					sweeperRunning = false;
					logException(e);
				}
			}
		}

	}

	public void sweep(int sweepStart, int sweepEnd, int sweepDelayMS,
			int sweepIncrement) {
		if (sweeperRunning) {
			stopSweep();
		}
		this.sweepStart = sweepStart;
		this.sweepEnd = sweepEnd;
		this.sweepDelayMS = sweepDelayMS;
		this.sweepIncrement = sweepIncrement;

		sweeperRunning = true;

		sweeper = new Thread(new Sweeper());
		sweeper.start();
	}

	public void stopSweep() {
		sweeperRunning = false;
		sweeper = null;
	}

	public void sweep() {
		sweep(sweepStart, sweepEnd, sweepDelayMS, sweepIncrement);
	}

	public static void main(String[] args) throws InterruptedException {

		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);

		// FIXME - routing of servo.attach("arduino", 3);
		
		Runtime.createAndStart("arduino", "Arduino");

		Servo right = new Servo("servo01");
		right.startService();
/*
		Servo left = new Servo("left");
		left.startService();

		//Servo neck = new Servo("neck");
		//neck.startService();
		
		for (int i = 0; i < 30; ++i)
		{
		
			right.attach("arduino", 2);
			left.attach("arduino", 3);
			
			right.moveTo(120); // 70 back
			left.moveTo(70); // 118 back
	
			Thread.sleep(10000);
			
			right.moveTo(90);		
			left.moveTo(90);
	
			//right.detach();
			//left.detach();
		}
*/
		Runtime.createAndStart("gui", "GUIService");
		
	}

	@Override
	public String getControllerName() {
		if (controller == null)
		{
			return null;
		} 
		
		return controller.getName();
	}

	@Override
	public Integer getPin() {
		if (controller == null)
		{
			return null;
		}
		
		return controller.getServoPin(getName());
	}


	
}
