package org.myrobotlab.speech;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.myrobotlab.framework.Service;
import org.myrobotlab.net.ClientHttpRequest;
import org.myrobotlab.service.GoogleSTT;

import com.google.gson.Gson;

/**
 * Each transcription is handled in a separate thread to ensure that the program remains responsive while the thread is waiting for 
 * a response from the server.
 * Lifted from a great page http://stt.getflourish.com/
 * by Mr. Florian Schulz.
 * More info in Chromium source :)
 * http://src.chromium.org/svn/trunk/src/content/browser/speech/speech_recognition_request.cc
 * @author Florian Schulz
 */

public class TranscriptionThread extends Thread {

	public final static Logger LOG = Logger.getLogger(GoogleSTT.class.getCanonicalName());
	
	boolean running;

	private float confidence;
	public boolean debug = true;
	private int status;
	boolean available = false;
	private String utterance;

	private String record;
	private String lang;
	
	
	public TranscriptionThread(String lang) {
		this.lang = lang;
		running = false;
	}

	public void startTranscription(String record) {
		this.record = record;
		running = true;
	}

	public void run() {
		while (true) {
			if (running) {
				transcribe(this.record);
				running = false;
			} else {
				try {
					sleep(500); // <- TODO - wait/notify/block - lets not do this 1/2 sec pause
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public boolean isRunning() {
		return running;
	}

	/**
	 * 
	 * @param path location of the audio file encoded as FLAC
	 * @return transcription of the audio file as String
	 */
	public String transcribe(String path) {
		this.available = false;

		File file = new File(path);

		lang = "en-US";		
		String response = "";
		ClientHttpRequest rs;
		try {
			Service.logTime("t1", "pre new client");
			// TODO - add Android headers 
			rs = new ClientHttpRequest("https://www.google.com/speech-api/v1/recognize?xjerr=1&client=chromium&pfilter=2&lang=" + lang + "&maxresults=6");
			Service.logTime("t1", "post new client");
			rs.setRequestProperty("Content-Type", "audio/x-flac; rate=8000"); // TODO - from targetLineData ?
			rs.setParameter("file", file);// <-- woosh 6 seconds?
			Service.logTime("t1", "post file in param");

			InputStream stream = rs.post();
			Service.logTime("t1", "post client.post");
			response = convertStreamToString(stream);
			Service.logTime("t1", "convert response");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String s = "";

		Gson gson = new Gson();
		Response transcription = gson.fromJson(response, Response.class);
		if (transcription != null) {
			if (transcription.status == 0) {
				// returns the transcription
				this.confidence = transcription.hypotheses[0].confidence;
				this.status = transcription.status;
				this.utterance = transcription.hypotheses[0].utterance;
				this.available = true;
			} else {
				// no result, could not be transcribed
				this.confidence = 0;
				this.status = transcription.status;
				this.utterance = "";
				this.available = true;
			}
			if (debug) {
				switch (this.status) {
				case 0:
					s = "Recognized: " + this.utterance + " (confidence: " + this.confidence + ")";
					this.available = true;
					status = GoogleSTT.SUCCESS;
					break;
				case 3:
					s = "We lost some words on the way.";
					status = GoogleSTT.ERROR;
					break;
				case 5:
					s = "Speech could not be interpreted.";
					status = GoogleSTT.ERROR;
					break;
				default:
					s = "Did you say something?";
					status = GoogleSTT.ERROR;
					break;
				}
			} else {
				if (this.status == 0) status = GoogleSTT.SUCCESS;
				else status = GoogleSTT.ERROR;
			}
		} else {
			s = "Speech could not be interpreted! Try to shorten the recording.";
			status = GoogleSTT.ERROR;
		}
		if(debug) {
			System.out.println(getTime() + " " + s);	
		}
		return this.utterance;
	}

	private String convertStreamToString(InputStream is) throws IOException {
		if (is != null) {
			Writer writer = new StringWriter();

			char[] buffer = new char[1024];
			try {
				Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
				int n;
				while ((n = reader.read(buffer)) != -1) {
					writer.write(buffer, 0, n);
				}
			} finally {
				is.close();
			}
			return writer.toString();
		} else {
			return "";
		}
	}
	
	public float getConfidence () 
	{
		return this.confidence;
	}
	public String getUtterance ()
	{
		return this.utterance;
	}
	public int getStatus () {
		return this.status;
	}
	/**
	 * @return true if audio processing was successfull
	 */
	public boolean isAvailable()
	{
		return this.available;
	}
	private String getTime() {
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
	}


	
	
	public static void main(String[] args) {
		org.apache.log4j.BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG);
		
		TranscriptionThread t = new TranscriptionThread("en-US");
		t.transcribe("test2.flac");		
		
	}
	

}
