package org.myrobotlab.service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.myrobotlab.framework.Service;
import org.myrobotlab.logging.Level;
import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.logging.LoggingFactory;
import org.slf4j.Logger;

import com.wolfram.alpha.WAEngine;
import com.wolfram.alpha.WAException;
import com.wolfram.alpha.WAPlainText;
import com.wolfram.alpha.WAPod;
import com.wolfram.alpha.WAQuery;
import com.wolfram.alpha.WAQueryResult;
import com.wolfram.alpha.WASubpod;

public class WolframAlpha extends Service {

	private static final long serialVersionUID = 1L;
	private static String AppID = "W6VGAJ-P4RA2HKTTH";
	public final static Logger log = LoggerFactory.getLogger(WolframAlpha.class
			.getCanonicalName());

	public WolframAlpha(String n) {
		super(n, WolframAlpha.class.getCanonicalName());
	}

	@Override
	public String getToolTip() {
		return "Wolfram Alpha Service";
	}

	@Override
	public void stopService() {
		super.stopService();
	}

	@Override
	public void releaseService() {
		super.releaseService();
	}

	public static void main(String[] args) {
		LoggingFactory.getInstance().configure();
		LoggingFactory.getInstance().setLevel(Level.WARN);

		WolframAlpha template = new WolframAlpha("wolfram");
		template.startService();

		Runtime.createAndStart("gui", "GUIService");
		/*
		 * GUIService gui = new GUIService("gui"); gui.startService();
		 * gui.display();
		 */
	}

	public void setAppID(String id) {
		AppID = id;
	}

	/**
	 * Query Wolfram Alpha for an answer
	 * 
	 * @param query
	 * @return
	 */
	public String wolframAlpha(String query) {
		String url;
		try {
			url = "http://www.wolframalpha.com/input/?i="
					+ URLEncoder.encode(query, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
		}
		// openUrl(url);
		// The WAEngine is a factory for creating WAQuery objects,
		// and it also used to perform those queries. You can set properties of
		// the WAEngine (such as the desired API output format types) that will
		// be inherited by all WAQuery objects created from it. Most
		// applications
		// will only need to crete one WAEngine object, which is used throughout
		// the life of the application.
		WAEngine engine = new WAEngine();

		// These properties will be set in all the WAQuery objects created from
		// this WAEngine.
		engine.setAppID(AppID);
		engine.addFormat("plaintext");

		// Create the query.
		WAQuery waquery = engine.createQuery();

		// Set properties of the query.
		waquery.setInput(query);

		try {
			// For educational purposes, print out the URL we are about to send:
			// System.out.println("Query URL:");
			// System.out.println(engine.toURL(waquery));
			// System.out.println("");

			// This sends the URL to the Wolfram|Alpha server, gets the XML
			// result
			// and parses it into an object hierarchy held by the WAQueryResult
			// object.
			WAQueryResult queryResult = engine.performQuery(waquery);

			if (queryResult.isError()) {
				return "Query error" + " \nError code: "
						+ queryResult.getErrorCode() + "\nError message: "
						+ queryResult.getErrorMessage();

			} else if (!queryResult.isSuccess()) {
				return ("Query was not understood; no results available.");
			} else {
				// Got a result.
				String full = "";
				for (WAPod pod : queryResult.getPods()) {
					if (!pod.isError()) {
						full += pod.getTitle() + "\n";
						for (WASubpod subpod : pod.getSubpods()) {
							for (Object element : subpod.getContents()) {
								if (element instanceof WAPlainText) {
									full += ((WAPlainText) element).getText()
											+ "\n";
									;
								}
							}
						}
					}
				}
				return full;

				// We ignored many other types of Wolfram|Alpha output, such as
				// warnings, assumptions, etc.
				// These can be obtained by methods of WAQueryResult or objects
				// deeper in the hierarchy.
			}
		} catch (WAException e) {
			e.printStackTrace();
		}
		return null;
	}

}
