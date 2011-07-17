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


package org.myrobotlab.image;

import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2GRAY;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvCvtColor;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvGetSize;

import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.IplImage;
import org.myrobotlab.service.OpenCV;

public class OpenCVFilterGray extends OpenCVFilter {

	private static final long serialVersionUID = 1L;
	
	public final static Logger LOG = Logger.getLogger(OpenCVFilterGray.class
			.getCanonicalName());

	IplImage buffer = null;

	public OpenCVFilterGray(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {
		return image.getBufferedImage();
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void loadDefaultConfiguration() {
		// TODO Auto-generated method stub

	}

	@Override
	public IplImage process(IplImage image) {

		if (buffer == null) {
			buffer = cvCreateImage(cvGetSize(image), 8, 1);

		}

		// what can you expect? nothing? - if data != null then error?
		if (image.nChannels()== 3) {
			cvCvtColor(image, buffer, CV_BGR2GRAY);
		}

		return buffer;
	}

}
