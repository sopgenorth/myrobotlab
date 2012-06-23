package org.myrobotlab.service.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Appender;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.myrobotlab.framework.Message;
import org.myrobotlab.service.Jython;
import org.myrobotlab.test.TestHelpers;
import org.python.core.PyException;
import org.python.util.PythonInterpreter;

public class JythonTest {
	private Logger log = Logger.getLogger("JythonTest");
	String name = "sometestname";
	Jython guineaPig = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		guineaPig = new Jython(name);
	}

	@After
	public void tearDown() throws Exception {
		guineaPig = null;
	}

	@Test
	public void testGetToolTip() {
		String result = guineaPig.getToolTip();
		Assert.assertNotNull(result);
		Assert.assertFalse(result.trim().isEmpty());
	}

	@Test
	public void testPreProcessHook() {
		boolean result = false;
		Message method = null;
		
		try {
			guineaPig.preProcessHook(method);
			Assert.fail("Should have thrown NullPointerException because input to method is null.");
		} catch (NullPointerException e) {}
		
		method = new Message();

		guineaPig.preProcessHook(method);

		method.sender = "JythonTest";
		guineaPig.preProcessHook(method);
		
		method.sendingMethod = "testPreProcessHook1";
		guineaPig.preProcessHook(method);

		method.method = "badmethod";
		guineaPig.preProcessHook(method);

		method.method = "preProcessHook";
		result = guineaPig.preProcessHook(method);
		Assert.assertTrue(result);

		// TODO need to have a case where the jython is successfully executed
	}

	@Test
	public void testJython() {
		HashMap<String, Object> publicMethods = new HashMap<String, Object>();
		Method[] methods = guineaPig.getClass().getMethods();
		for (Method m : methods) {
			if (Modifier.isPublic(m.getModifiers())) {
				publicMethods.put(m.getName(), null);
				log.info(m.getName());
			}
		}
		HashMap<String, Object> commandMap = TestHelpers.<HashMap<String, Object>>getField(guineaPig, "commandMap");
		Assert.assertEquals(publicMethods.size(), commandMap.size());
	}

	@Test
	public void testCreatePythonInterpreter() {
		guineaPig.createPythonInterpreter();
		PythonInterpreter interpreter = TestHelpers.<PythonInterpreter>getField(guineaPig, "interp");
		Assert.assertNotNull(interpreter);
	}

	@Test
	public void testAttachJythonConsole() {
		guineaPig.attachJythonConsole();
	}

	@Test
	public void testExecString() {
		// TODO set up a string logger to verify that the error message is written out for the failed exec() call
		String code = null;
		guineaPig.exec(code);
		String script = TestHelpers.<String>getField(guineaPig, "script");
		Assert.assertNull(script);
		
		code = "some code";
		guineaPig.exec(code);
		script = TestHelpers.<String>getField(guineaPig, "script");
		Assert.assertNotNull(script);
		Assert.assertEquals(code, script);
	}

	@Test
	public void testExecStringBoolean() {
		// TODO set up a string logger to verify that the error message is written out for the failed exec() call
		String code = null;
		boolean replace = false;
		guineaPig.exec(code, replace);
		String script = TestHelpers.<String>getField(guineaPig, "script");
		Assert.assertNull(script);
		
		code = "some code";
		replace = false;
		guineaPig.exec(code, replace);
		script = TestHelpers.<String>getField(guineaPig, "script");
		Assert.assertNull(script);
		
		code = "some code";
		replace = true;
		guineaPig.exec(code, replace);
		script = TestHelpers.<String>getField(guineaPig, "script");
		Assert.assertNotNull(script);
		Assert.assertEquals(code, script);
	}

	@Test
	public void testFinishedExecutingScript() {
		guineaPig.finishedExecutingScript();
	}

	@Test
	public void testGetScript() {
		String code = null;
		boolean replace = false;
		guineaPig.exec(code, replace);
		Assert.assertNull(guineaPig.getScript());
		
		code = "some code";
		replace = false;
		guineaPig.exec(code, replace);
		Assert.assertNull(guineaPig.getScript());
		
		code = "some code";
		replace = true;
		guineaPig.exec(code, replace);
		Assert.assertNotNull(guineaPig.getScript());
		Assert.assertEquals(code, guineaPig.getScript());
	}

	@Test
	public void testRestart() {
		guineaPig.createPythonInterpreter();
		guineaPig.restart();
		Object interpreter = TestHelpers.<Object>getField(guineaPig, "interp");
		Assert.assertNull(interpreter);
	}

	@Test
	public void testPublishStdOut() {
		String data = "some data";
		String result = guineaPig.publishStdOut(data);
		Assert.assertEquals(data, result);
	}

}
