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

package org.myrobotlab.net;

import java.io.Serializable;
import java.net.URL;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Outbox;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.ServiceWrapper;
import org.myrobotlab.service.interfaces.CommunicationInterface;
import org.myrobotlab.service.interfaces.Communicator;
import org.myrobotlab.service.Runtime;

public class CommunicationManager  implements Serializable, CommunicationInterface {

	private static final long serialVersionUID = 1L;
	public final static Logger log = Logger.getLogger(CommunicationManager.class.toString());
	Service myService = null;
	Outbox outbox = null;

	private Communicator comm = null;

	public CommunicationManager(Service myService) {
		// set local private references
		this.myService = myService;
		this.outbox = myService.getOutbox();

		String communicatorClass ="org.myrobotlab.net.CommObjectStreamOverTCP";
		log.info("instanciating a " + communicatorClass);
		Communicator c = (Communicator) Service.getNewInstance(communicatorClass, myService);

		outbox.setCommunicationManager(this);

		setComm(c);

	}

	public void send(final URL remoteURL, final Message msg) {
		getComm().send(remoteURL, msg);	
	}
	
	public void send(final Message msg) {
		
		ServiceWrapper sw = Runtime.getService(msg.getName());
		if (sw == null)
		{
			log.error(msg.getName() + " service does not exist - should clean up " + msg.sender );
			return;
		}
		if (sw.host.accessURL == null || sw.host.accessURL.equals(myService.url))
		{
			log.debug("sending local");
			Message m = new Message(msg); // TODO UNECESSARY - BUT TOO SCARED TO REMOVE !!
			sw.get().in(m);			
		} else {// TODO - test for loglevel
			log.debug( msg.sender + "." + msg.sendingMethod + "->" + 
					sw.host.accessURL + "/" + msg.name + "."+ msg.method + "(" + ((msg.data == null)?"":msg.data.length) + ")");
			getComm().send(sw.host.accessURL, msg);			
		}
	}

	public void setComm(Communicator comm) {
		this.comm = comm;
	}

	public Communicator getComm() {
		return comm;
	}
	


}
