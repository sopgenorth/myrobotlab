package org.myrobotlab.serial;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.myrobotlab.framework.Platform;

/**
 * @author GroG
 * 
 *         This factory was created to reconcile the different SerialDevices and
 *         serial device frameworks which are incompatible depending on platform
 *         specifics (eg. Android's bluetooth & gnu serial)
 */
public class SerialDeviceFactory {

	public final static Logger log = Logger.getLogger(SerialDeviceFactory.class.getCanonicalName());

	final public static String TYPE_GNU = "org.myrobotlab.serial.gnu.SerialDeviceFactoryGNU";
	final public static String TYPE_JSSC = "org.myrobotlab.serial.jssc.SerialDeviceFactoryJSSC";
	final public static String TYPE_ANDROID_BLUETOOTH = "android.somethin";

	static public ArrayList<String> getSerialDeviceNames() {
		if (Platform.isDavlik()) {
			return getSerialDeviceNames(TYPE_ANDROID_BLUETOOTH);
		} else {
			return getSerialDeviceNames(TYPE_GNU);
		}
	}

	@SuppressWarnings("unchecked")
	static public ArrayList<String> getSerialDeviceNames(String factoryType) {
		ArrayList<String> ret = new ArrayList<String>();

		try {
			Class<?> c = Class.forName(factoryType);
			log.info("Loaded class: " + c);
			Object serialDeviceFramework = c.newInstance();
			Method m = c.getDeclaredMethod("getSerialDeviceNames");
			log.info("Got method: " + m);
			return (ArrayList<String>) m.invoke(serialDeviceFramework);
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());
		}

		return ret;
	}

	/**
	 * The main "meaty" method - get me a serial device ! A serial factory will
	 * be supplied for you.
	 * 
	 * @param name
	 * @param rate
	 * @param databits
	 * @param stopbits
	 * @param parity
	 * @return
	 * @throws SerialDeviceException
	 */
	static public SerialDevice getSerialDevice(String name, int rate, int databits, int stopbits, int parity) throws SerialDeviceException {
		if (Platform.isDavlik()) {
			// FIXME Bluetooth rate databits stopbits & parity are all
			// meaningless
			return getSerialDevice(TYPE_ANDROID_BLUETOOTH, name, rate, databits, stopbits, parity);
		} else {
			return getSerialDevice(TYPE_GNU, name, rate, databits, stopbits, parity);
		}
	}

	/**
	 * The 'under the hood" method to get a serial device - "choose your own
	 * factory"
	 * 
	 * @param factoryType
	 * @param name
	 * @param rate
	 * @param databits
	 * @param stopbits
	 * @param parity
	 * @return
	 * @throws SerialDeviceException
	 */
	static public SerialDevice getSerialDevice(String factoryType, String name, int rate, int databits, int stopbits, int parity) throws SerialDeviceException {
		log.info(String.format("getSerialDevice %s|%d|%d|%d|%d", name, rate, databits, stopbits, parity));

		SerialDevice port = null;

		try {
			Class<?> c = Class.forName(factoryType);
			log.info("Loaded class: " + c);
			Object serialDeviceFramework = c.newInstance();
			Method m = c.getDeclaredMethod("getSerialDevice", new Class<?>[] { String.class, int.class, int.class, int.class, int.class });
			log.info("Got method: " + m);
			return (SerialDevice) m.invoke(serialDeviceFramework, new Object[] { name, rate, databits, stopbits, parity });
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e.getMessage());// FIXME - logexception
		}

		return port;
	}

	public static void main(String[] args) throws IOException {
		org.apache.log4j.BasicConfigurator.configure();
		// Logger.getRootLogger().setLevel(Level.DEBUG);

		ArrayList<String> names = SerialDeviceFactory.getSerialDeviceNames(TYPE_GNU);
		for (int i = 0; i < names.size(); ++i) {
			log.info(names.get(i));
		}

		Class<?>[] d = new Class<?>[] { int.class };

		String portName = "COM9";
		try {
			SerialDevice sd = SerialDeviceFactory.getSerialDevice(TYPE_GNU, portName, 57600, 8, 1, 0); // TODO/FIXME
																										// -
																										// serialdevice
																										// identifier
																										// -
																										// opened
																										// by
																										// someone
																										// else
			sd.open();
			log.info(sd.isOpen());
			log.info(sd.isOpen());
			sd.write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 });
			sd.close();
			log.info(sd.isOpen());
			sd.open();
			sd.write(new byte[] { 0, 1, 2, 3, 4, 5, 6, 7 });
			log.info(sd.isOpen());
		} catch (SerialDeviceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
