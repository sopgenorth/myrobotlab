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

package org.myrobotlab.opencv;

import static com.googlecode.javacv.cpp.opencv_imgproc.CV_BGR2HSV;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_GAUSSIAN;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvSmooth;

import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.swing.JFrame;

import org.myrobotlab.logging.LoggerFactory;
import org.myrobotlab.service.OpenCV;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterSmooth extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterSmooth.class.getCanonicalName());

	IplImage buffer = null;
	BufferedImage frameBuffer = null;
	int convert = CV_BGR2HSV; // TODO - convert to all schemes
	JFrame myFrame = null;

	public OpenCVFilterSmooth(VideoProcessor vp, String name, HashMap<String, IplImage> source,  String sourceKey)  {
		super(vp, name, source, sourceKey);
	}

	@Override
	public BufferedImage display(IplImage image, OpenCVData data) {
		return image.getBufferedImage();
	}

	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		// cvDrawRect(image, startPoint, startPoint,
		// fillColor, 2, 1, 0);
		cvSmooth(image, image, CV_GAUSSIAN, 9, 7, 7, 1);

		return image;

	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub
		
	}

}