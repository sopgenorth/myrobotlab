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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.LogManager;
import org.apache.log4j.net.SocketAppender;
import org.myrobotlab.control.GUIServiceGUI;
import org.myrobotlab.control.RuntimeGUI;
import org.myrobotlab.control.ServiceGUI;
import org.myrobotlab.control.TabControl2;
import org.myrobotlab.control.Welcome;
import org.myrobotlab.control.widget.AboutDialog;
import org.myrobotlab.control.widget.ConnectDialog;
import org.myrobotlab.control.widget.Console;
import org.myrobotlab.framework.Message;
import org.myrobotlab.framework.Service;
import org.myrobotlab.framework.Status;
import org.myrobotlab.logging.Appender;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.Logging;
import org.myrobotlab.logging.LoggingFactory;
import org.myrobotlab.service.interfaces.ServiceInterface;
import org.myrobotlab.string.Util;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;
import org.slf4j.Logger;

import com.mxgraph.model.mxCell;
import com.mxgraph.view.mxGraph;

/*
 * GUIService -> Look at service registry
 * GUIService -> attempt to create a panel for each registered service
 * 		GUIService -> create panel
 *      GUIService -> panel.init(this, serviceName);
 *      	   panel.send(Notify, someoutputfn, GUIName, panel.inputfn, data);
 *  
 *       
 *       
 *       serviceName (source) --> GUIService-> msg
 * Arduino arduino01 -> post message -> outbox -> outbound -> notifyList -> reference of sender? (NO) will not transport
 * across process boundry 
 * 
 * 		serviceGUI needs a Runtime
 * 		Arduino arduin-> post back (data) --> GUIService - look up serviceGUI by senders name ServiceGUI->invoke(data)
 * 
 * References :
 * http://www.scribd.com/doc/13122112/Java6-Rules-Adding-Components-To-The-Tabs-On-JTabbedPaneI-Now-A-breeze
 */

@Root
public class GUIService extends Service implements WindowListener, ActionListener, Serializable {

	private static final long serialVersionUID = 1L;

	transient public final static Logger log = LoggerFactory.getLogger(GUIService.class);

	public String graphXML = "";

	public transient JFrame frame = null;

	@Element(required = false)
	public String lastTabVisited;

	public transient JTabbedPane tabs = new JTabbedPane();

	// system menu items - FIXME make all menus here
	transient JMenuItem recording = new JMenuItem("start recording");
	transient JMenuItem loadRecording = new JMenuItem("load recording");

	final public String welcomeTabText = "Welcome";
	// TODO - make MTOD !! from internet

	public transient GUIServiceGUI guiServiceGUI = null;

	// FIXME - supply Welcome type - create WelcomeGUI type - with boundServiceName this GUIService
	transient Welcome welcome = null;
	transient HashMap<String, ServiceGUI> serviceGUIMap = new HashMap<String, ServiceGUI>();

	boolean isDisplaying = false;
	transient JLabel status = new JLabel("status");

	public GUIService(String n) {
		super(n);
		Runtime.getInstance().addListener("registered", n, "registered");
		Runtime.getInstance().addListener("released", n, "released");
		// TODO - add the release route too
		load();// <-- HA was looking all over for it
	}

	public Service registered(final Service s) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				addTab(s.getName());
				// kind of kludgy but got to keep them in sync
				RuntimeGUI rg = (RuntimeGUI) serviceGUIMap.get(Runtime.getInstance().getName());
				if (rg != null) {
					rg.registered(s);
				}
			}
		});
		return s;
	}

	public Service released(final Service s) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				removeTab(s.getName());
				// kind of kludgy but got to keep them in sync
				RuntimeGUI rg = (RuntimeGUI) serviceGUIMap.get(Runtime.getInstance().getName());
				if (rg != null) {
					rg.released(s);
				}
			}
		});
		return s;
	}

	public boolean hasDisplay() {
		return true;
	}

	public HashMap<String, ServiceGUI> getServiceGUIMap() {
		return serviceGUIMap;
	}

	public boolean preProcessHook(Message m) {
		// FIXME - problem with collisions of this service's methods
		// and dialog methods ?!?!?

		// if the method name is == to a method in the GUIService
		if (methodSet.contains(m.method)) {
			// process the message like a regular service
			return true;
		}

		// otherwise send the message to the dialog with the senders name
		ServiceGUI sg = serviceGUIMap.get(m.sender);
		if (sg == null) {
			log.error("attempting to update sub-gui - sender " + m.sender + " not available in map " + getName());
		} else {
			// FIXME - NORMALIZE - Instantiator or Service - not both !!!
			// Instantiator.invokeMethod(serviceGUIMap.get(m.sender), m.method,
			// m.data);
			invokeOn(serviceGUIMap.get(m.sender), m.method, m.data);
		}

		return false;
	}

	/**
	 * builds all the service tabs for the first time
	 * called when GUIService starts
	 * @return
	 */
	synchronized public JTabbedPane buildTabPanels() {
		// add the welcome screen
		if (!serviceGUIMap.containsKey(welcomeTabText)) {
			welcome = new Welcome(welcomeTabText, this, tabs);
			welcome.init();
			serviceGUIMap.put(welcomeTabText, welcome);
		}

		HashMap<String, ServiceInterface> services = Runtime.getRegistry();
		log.info("buildTabPanels service count " + Runtime.getRegistry().size());

		TreeMap<String, ServiceInterface> sortedMap = new TreeMap<String, ServiceInterface>(services);
		Iterator<String> it = sortedMap.keySet().iterator();
		synchronized (sortedMap) { // FIXED YAY !!!!
			while (it.hasNext()) {
				String serviceName = it.next();
				addTab(serviceName);
			}
		}

		frame.pack();
		return tabs;
	}

	
	/**
	 * add a service tab to the GUIService
	 * @param serviceName - name of service to add
	 * 
	 * FIXME - full parameter of addTab(final String serviceName, final String serviceType, final String lable)
	 * then overload
	 */
	synchronized public void addTab(final String serviceName) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				ServiceInterface sw = Runtime.getService(serviceName);

				if (sw == null) {
					log.error(String.format("addTab %1$s can not proceed - %1$s does not exist in registry (yet?)", serviceName));
					return;
				}

				// get service type class name TODO
				String guiClass = String.format("org.myrobotlab.control.%sGUI", sw.getClass().getSimpleName());

				if (serviceGUIMap.containsKey(sw.getName())) {
					log.debug(String.format("not creating %1$s gui - it already exists", sw.getName()));
					return;
				}

				ServiceGUI newGUI = createTabbedPanel(serviceName, guiClass, sw);
				// woot - got index !
				int index = tabs.indexOfTab(serviceName) - 1;

				if (newGUI != null) {
					++index;
				}

				guiServiceGUI = (GUIServiceGUI) serviceGUIMap.get(getName());
				if (guiServiceGUI != null) {
					guiServiceGUI.rebuildGraph();
				}

				Component c = tabs.getTabComponentAt(index);
				if (c instanceof TabControl2) {
					TabControl2 tc = (TabControl2) c;

					if (!sw.isLocal()) {
						Color hsv = GUIService.getColorFromURI(sw.getHost());
						tabs.setBackgroundAt(index, hsv);
					}

					// String serviceName = tc.getText();
					/*
					 * REMOVED RECENTLY - implement in ServiceGUI if necessary
					 * if (undockedPanels.containsKey(serviceName)) {
					 * UndockedPanel up = undockedPanels.get(serviceName); if
					 * (!up.isDocked()) { tc.undockPanel(); } }
					 */
				}

				frame.pack();

			}
		});
	}

	public void removeTab(final String name) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				log.info("removeTab");

				// detaching & removing the ServiceGUI
				ServiceGUI sg = serviceGUIMap.get(name);
				if (sg != null) {
					sg.remove();
					serviceGUIMap.remove(name);
				} else {
					log.warn(String.format("{} was not in the serviceGUIMap - unable to remove", name));
				}

				guiServiceGUI = (GUIServiceGUI) serviceGUIMap.get(getName());
				if (guiServiceGUI != null) {
					guiServiceGUI.rebuildGraph();
				}
				frame.pack();
			}
		});
	}
	
	/**
	 * attempts to create a new ServiceGUI and add it to the map
	 * 
	 * @param serviceName
	 * @param guiClass
	 * @param sw
	 * @return
	 */

	public ServiceGUI createTabbedPanel(String serviceName, String guiClass, ServiceInterface sw) {
		ServiceGUI gui = null;
		ServiceInterface se = sw;

		gui = (ServiceGUI) getNewInstance(guiClass, se.getName(), this, tabs);

		if (gui == null) {
			log.info(String.format("could not construct a %s object - creating generic template", guiClass));
			gui = (ServiceGUI) getNewInstance("org.myrobotlab.control._TemplateServiceGUI", se.getName(), this, tabs);
		}

		gui.init();
		serviceGUIMap.put(serviceName, gui);
		gui.attachGUI();

		// TODO - all auto-subscribtions could be done here
		subscribe("publishStatus", se.getName(), "getStatus", String.class);
		return gui;
	}

	public static Color getColorFromURI(Object uri) {
		StringBuffer sb = new StringBuffer(String.format("%d", Math.abs(uri.hashCode())));
		Color c = new Color(Color.HSBtoRGB(Float.parseFloat("0." + sb.reverse().toString()), 0.8f, 0.7f));
		return c;
	}

	public static List<Component> getAllComponents(final Container c) {
		Component[] comps = c.getComponents();
		List<Component> compList = new ArrayList<Component>();
		for (Component comp : comps) {
			compList.add(comp);
			if (comp instanceof Container)
				compList.addAll(getAllComponents((Container) comp));
		}
		return compList;
	}

	public void display() {
		if (!isDisplaying) {
			// reentrant
			if (frame != null) {
				frame.dispose();
				frame = null;
			}

			if (frame == null) {
				frame = new JFrame();
			}

			frame.addWindowListener(this);
			frame.setTitle("myrobotlab - " + getName() + " " + Runtime.getVersion().trim());

			buildTabPanels();

			JPanel main = new JPanel(new BorderLayout());
			main.add(tabs, BorderLayout.CENTER);
			main.add(status, BorderLayout.SOUTH);
			status.setOpaque(true);

			frame.add(main);

			URL url = getClass().getResource("/resource/mrl_logo_36_36.png");
			Toolkit kit = Toolkit.getDefaultToolkit();
			Image img = kit.createImage(url);
			frame.setIconImage(img);

			// menu
			frame.setJMenuBar(buildMenu());
			frame.setVisible(true);
			frame.pack();
			
			isDisplaying = true;
		}

	}

	// @Override - only in Java 1.6
	public void windowActivated(WindowEvent e) {
		// log.info("windowActivated");
	}

	// @Override - only in Java 1.6
	public void windowClosed(WindowEvent e) {
		// log.info("windowClosed");
	}

	// @Override - only in Java 1.6
	public void windowClosing(WindowEvent e) {
		// check for all service guis and see if its
		// ok to shutdown now
		Iterator<Map.Entry<String, ServiceGUI>> it = serviceGUIMap.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String, ServiceGUI> pairs = (Map.Entry<String, ServiceGUI>) it.next();
			// String serviceName = pairs.getKey();
			/*
			 * if (undockedPanels.containsKey(serviceName)) { UndockedPanel up =
			 * undockedPanels.get(serviceName); if (!up.isDocked()) {
			 * up.savePosition(); } }
			 */
			ServiceGUI sg = pairs.getValue();
			sg.savePosition();
			sg.isReadyForRelease();
			sg.makeReadyForRelease();
		}

		save();

		Runtime.releaseAll();
		System.exit(1); // the Big Hamm'r
	}

	// @Override - only in Java 1.6
	public void windowDeactivated(WindowEvent e) {
		// log.info("windowDeactivated");
	}

	// @Override - only in Java 1.6
	public void windowDeiconified(WindowEvent e) {
		// log.info("windowDeiconified");
	}

	// @Override - only in Java 1.6
	public void windowIconified(WindowEvent e) {
		// log.info("windowActivated");
	}

	// @Override - only in Java 1.6
	public void windowOpened(WindowEvent e) {
		// log.info("windowOpened");

	}

	public void about() {
		new AboutDialog(frame);
	}

	public void stopService() {
		dispose();
		super.stopService();
	}

	public void dispose() {
		if (frame != null) {
			frame.dispose();
		}
	}

	@Override
	public String getDescription() {
		return "<html>Service used to graphically display and control other services</html>";
	}

	public void pack() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				frame.pack();
			}
		});
	}

	public JFrame getFrame() {
		return frame;
	}

	public void setDstMethodName(String d) {
		guiServiceGUI.dstMethodName.setText(d);
	}

	public void setDstServiceName(String d) {
		guiServiceGUI.dstServiceName.setText(d);
	}

	public void setPeriod0(String s) {
		guiServiceGUI.period0.setText(s);
	}

	public String getDstMethodName() {
		return guiServiceGUI.dstMethodName.getText();
	}

	public String getDstServiceName() {
		return guiServiceGUI.dstServiceName.getText();
	}

	public String getSrcMethodName() {
		return guiServiceGUI.srcMethodName.getText();
	}

	public String getSrcServiceName() {
		return guiServiceGUI.srcServiceName.getText();
	}

	public HashMap<String, mxCell> getCells() {
		return guiServiceGUI.serviceCells;
	}

	public mxGraph getGraph() {
		return guiServiceGUI.graph;
	}

	public void setSrcMethodName(String d) {
		guiServiceGUI.srcMethodName.setText(d);
	}

	public void setSrcServiceName(String d) {
		guiServiceGUI.srcServiceName.setText(d);
	}

	public void setArrow(String s) {
		guiServiceGUI.arrow0.setText(s);
	}

	public void setPeriod1(String s) {
		guiServiceGUI.period1.setText(s);
	}

	public String getGraphXML() {
		return graphXML;
	}

	public void setGraphXML(String xml) {
		graphXML = xml;
	}

	// FIXME - now I think its only "register" - Deprecate if possible
	public void registerServicesEvent(String host, int port, Message msg) {
		buildTabPanels();
	}

	// FIXME - now I think its only "register" - Deprecate if possible
	/*
	public void registerServicesEvent() {
		buildTabPanels();
	}
	*/

	static public void console() {
		attachJavaConsole();
	}

	static public void attachJavaConsole() {
		JFrame j = new JFrame("Java Console");
		j.setSize(500, 550);
		Console c = new Console();
		j.add(c.getScrollPane());
		j.setVisible(true);
		c.startLogging();
	}

	/**
	 * Build the menu for display.
	 * 
	 * @return
	 */
	public JMenuBar buildMenu() {
		JMenuBar menuBar = new JMenuBar();

		// --- system ----
		JMenu systemMenu = new JMenu("system");

		JMenuItem save = new JMenuItem("save");
		save.setActionCommand("save");
		systemMenu.add(save);
		save.addActionListener(this);

		JMenuItem load = new JMenuItem("load");
		load.setActionCommand("load");
		systemMenu.add(load);
		load.addActionListener(this);

		JMenuItem unhideAll = new JMenuItem("unhide all");
		unhideAll.setActionCommand("unhide all");
		systemMenu.add(unhideAll);
		unhideAll.addActionListener(this);

		JMenuItem hideAll = new JMenuItem("hide all");
		hideAll.setActionCommand("hide all");
		systemMenu.add(hideAll);
		hideAll.addActionListener(this);

		JMenu m = new JMenu("logging");
		systemMenu.add(m);

		JMenu m2 = new JMenu("level");
		m.add(m2);
		buildLogLevelMenu(m2);

		m2 = new JMenu("type");
		m.add(m2);
		buildLogAppenderMenu(m2);

		m = new JMenu("update");
		buildUpdatesMenu(m);

		systemMenu.add(m);

		systemMenu.add(buildRecordingMenu(new JMenu("recording")));

		menuBar.add(systemMenu);

		JMenu help = new JMenu("help");
		JMenuItem about = new JMenuItem("about");
		about.addActionListener(this);
		help.add(about);
		menuBar.add(help);

		return menuBar;
	}

	public JMenu buildRecordingMenu(JMenu parentMenu) {

		recording.addActionListener(this);
		parentMenu.add(recording);

		loadRecording.addActionListener(this);
		parentMenu.add(loadRecording);

		return parentMenu;
	}

	@Override
	public void actionPerformed(ActionEvent ae) {
		String cmd = ae.getActionCommand();
		Object source = ae.getSource();
		// get local runtime instance
		Runtime runtime = Runtime.getInstance();
		if ("save".equals(cmd)) {
			Runtime.saveAll();
		} else if ("load".equals(cmd)) {
			Runtime.loadAll();
		} else if ("unhide all".equals(cmd)) {
			unhideAll();
		} else if ("hide all".equals(cmd)) {
			hideAll();
		} else if ("install latest".equals(cmd)) {
			runtime.updateAll();
		} else if (cmd.equals(Level.DEBUG) || cmd.equals(Level.INFO) || cmd.equals(Level.WARN) || cmd.equals(Level.ERROR) || cmd.equals(Level.FATAL)) {
			// TODO this needs to be changed into something like tryValueOf(cmd)
			Logging logging = LoggingFactory.getInstance();
			logging.setLevel(cmd);
		} else /*
				 * if ("connect".equals(cmd)) { ConnectDialog dlg = new
				 * ConnectDialog(new JFrame(), "connect", "message", this,
				 * lastHost, lastPort); lastHost = dlg.host.getText(); lastPort
				 * = dlg.port.getText(); } else
				 */if (cmd.equals(Appender.NONE)) {
			Logging logging = LoggingFactory.getInstance();
			logging.removeAllAppenders();
		} else if (cmd.equals(Appender.REMOTE)) {
			JCheckBoxMenuItem m = (JCheckBoxMenuItem) ae.getSource();
			if (m.isSelected()) {
				ConnectDialog dlg = new ConnectDialog(new JFrame(), "connect to remote logging", "message", this, "127.0.0.1", "6767");
				Logging logging = LoggingFactory.getInstance();
				logging.addAppender(Appender.REMOTE, dlg.host.getText(), dlg.port.getText());
			} else {
				Logging logging = LoggingFactory.getInstance();
				logging.removeAppender(Appender.REMOTE);
			}
		} else if (cmd.equals(Appender.FILE)) { // FIXME - refactor it all out
												// (it recovered from enums !
			Logging logging = LoggingFactory.getInstance();
			logging.addAppender(Appender.FILE);
		} else if (cmd.equals(Appender.NONE)) {
			Logging logging = LoggingFactory.getInstance();
			logging.addAppender(Appender.NONE);
		} else if ("explode".equals(cmd)) {
			// display();
		} else if (source == recording) {
			if ("start recording".equals(recording.getText())) {
				startRecording();
				recording.setText("stop recording");
			} else {
				stopRecording();
				recording.setText("start recording");
			}
		} else if (source == loadRecording) {
			JFileChooser c = new JFileChooser(cfgDir);
			FileNameExtensionFilter filter = new FileNameExtensionFilter("Message files", "msg");
			c.setFileFilter(filter);
			// Demonstrate "Open" dialog:
			String filename;
			String dir;
			int rVal = c.showOpenDialog(frame);
			if (rVal == JFileChooser.APPROVE_OPTION) {
				filename = c.getSelectedFile().getName();
				dir = c.getCurrentDirectory().toString();
				loadRecording(dir + "/" + filename);
			}
			if (rVal == JFileChooser.CANCEL_OPTION) {

			}
		} else {
			invoke(Util.StringToMethodName(cmd));
		}
	}

	/**
	 * Add all options to the Software Update menu.
	 * 
	 * @param parentMenu
	 */
	private void buildUpdatesMenu(JMenu parentMenu) {
		JMenuItem mi = new JMenuItem("install latest");
		mi = new JMenuItem("install latest");
		mi.addActionListener(this);

		parentMenu.add(mi);

	}

	/**
	 * Add all options to the Log Appender menu.
	 * 
	 * @param parentMenu
	 */
	private void buildLogAppenderMenu(JMenu parentMenu) {
		Enumeration appenders = LogManager.getRootLogger().getAllAppenders();
		boolean console = false;
		boolean file = false;
		boolean remote = false;

		while (appenders.hasMoreElements()) {
			Object o = appenders.nextElement();
			if (o.getClass() == ConsoleAppender.class) {
				console = true;
			} else if (o.getClass() == FileAppender.class) {
				file = true;
			} else if (o.getClass() == SocketAppender.class) {
				remote = true;
			}

			log.info(o.getClass().toString());
		}

		JCheckBoxMenuItem mi = new JCheckBoxMenuItem(Appender.NONE);
		mi.setSelected(!console && !file && !remote);
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JCheckBoxMenuItem(Appender.CONSOLE);
		mi.setSelected(console);
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JCheckBoxMenuItem(Appender.FILE);
		mi.setSelected(file);
		mi.addActionListener(this);
		parentMenu.add(mi);

		mi = new JCheckBoxMenuItem(Appender.REMOTE);
		mi.setSelected(remote);
		mi.addActionListener(this);
		parentMenu.add(mi);
	}

	/**
	 * Add all options to the Log Level menu.
	 * 
	 * @param parentMenu
	 */
	private void buildLogLevelMenu(JMenu parentMenu) {
		ButtonGroup logLevelGroup = new ButtonGroup();

		String level = LoggingFactory.getInstance().getLevel();

		JRadioButtonMenuItem mi = new JRadioButtonMenuItem(Level.DEBUG);
		mi.setSelected(("DEBUG".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(Level.INFO);
		mi.setSelected(("INFO".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(Level.WARN);
		mi.setSelected(("WARN".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(Level.ERROR);
		mi.setSelected(("ERROR".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);

		mi = new JRadioButtonMenuItem(Level.FATAL); // TODO - deprecate to WTF
													// :)
		mi.setSelected(("FATAL".equals(level)));
		mi.addActionListener(this);
		logLevelGroup.add(mi);
		parentMenu.add(mi);
	}

	static public void restart(String restartScript) {
		JFrame frame = new JFrame();
		int ret = JOptionPane.showConfirmDialog(frame, "<html>New components have been added,<br>" + " it is necessary to restart in order to use them.</html>", "restart",
				JOptionPane.YES_NO_OPTION);
		if (ret == JOptionPane.OK_OPTION) {
			log.info("restarting");
			Runtime.restart(restartScript);
		} else {
			log.info("chose not to restart");
			return;
		}
	}

	public void startService() {
		super.startService();
		display();
	}

	public void getStatus(Status inStatus) {

		if (inStatus.isError()) {
			status.setOpaque(true);
			status.setForeground(Color.white);
			status.setBackground(Color.red);
		} else if (inStatus.isWarn()) {
			status.setOpaque(true);
			status.setForeground(Color.black);
			status.setBackground(Color.yellow);
		} else {
			status.setForeground(Color.black);
			status.setOpaque(false);
		}

		status.setText(inStatus.detail);
	}

	/**
	 * closes window and puts the panel back into the tabbed pane
	 */
	public void dockPanel(final String label) {

		if (serviceGUIMap.containsKey(label)) {
			ServiceGUI sg = serviceGUIMap.get(label);
			sg.dockPanel();
		} else {
			log.error("dockPanel - {} not in serviceGUIMap", label);
		}
	}

	// must handle docked or undocked
	public void hidePanel(final String label) {
		if (serviceGUIMap.containsKey(label)) {
			ServiceGUI sg = serviceGUIMap.get(label);
			sg.hidePanel();
		} else {
			log.error("hidePanel - {} not in serviceGUIMap", label);
		}
	}

	public void hideAll() {
		log.info("hideAll");
		// spin through all undocked
		for (Map.Entry<String, ServiceGUI> o : serviceGUIMap.entrySet()) {
			hidePanel(o.getKey());
		}
	}

	public void unhideAll() {
		log.info("unhideAll");
		// spin through all undocked
		for (Map.Entry<String, ServiceGUI> o : serviceGUIMap.entrySet()) {
			unhidePanel(o.getKey());
		}
	}

	// must handle docked or undocked & re-entrant for unhidden
	public void unhidePanel(final String label) {
		if (serviceGUIMap.containsKey(label)) {
			ServiceGUI sg = serviceGUIMap.get(label);
			sg.unhidePanel();
		} else {
			log.error("unhidePanel - {} not in serviceGUIMap", label);
		}
	}

	public void undockPanel(final String label) {
		if (serviceGUIMap.containsKey(label)) {
			ServiceGUI sg = serviceGUIMap.get(label);
			sg.undockPanel();
		} else {
			log.error("undockPanel - {} not in serviceGUIMap", label);
		}
	}

	public static void main(String[] args) throws ClassNotFoundException, URISyntaxException {
		LoggingFactory.getInstance().configure();
		Logging logging = LoggingFactory.getInstance();
		logging.setLevel(Level.INFO);
		/*
		 * //float x = 539248398 >> 10; float x = 10f % 539248398; log.info(x);
		 * float y = 5383823987f % 10f; log.info(y);
		 * 
		 * URI a = new URI("tcp://127.0.0.1:6767"); URI b = new
		 * URI("tcp://192.168.0.2:6767");
		 * 
		 * String c = a.toString(); String d = b.toString();
		 * 
		 * log.info(c); log.info(String.format("0.%d",Math.abs(c.hashCode())));
		 * 
		 * log.info(d); log.info(String.format("0.%d",Math.abs(d.hashCode())));
		 */
		// Runtime.createAndStart("clock", "Clock");

		Runtime.createAndStart("i01", "InMoov");

		GUIService gui2 = (GUIService) Runtime.createAndStart("gui1", "GUIService");
		gui2.startService();

		/*
		 * Clock clock = new Clock("clock"); clock.startService();
		 */

		// Runtime.createAndStart("opencv", "OpenCV");
		// gui2.display();

		// gui2.startRecording();
		// gui2.stopRecording();

		// gui2.loadRecording(".myrobotlab/gui1_20120918052147517.msg");

	}

}
