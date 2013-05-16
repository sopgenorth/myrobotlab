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

import static com.googlecode.javacv.cpp.opencv_core.cvAnd;
import static com.googlecode.javacv.cpp.opencv_core.cvNot;

import java.awt.image.BufferedImage;
import java.util.HashMap;

import org.myrobotlab.logging.LoggerFactory;
import org.slf4j.Logger;

import com.googlecode.javacv.cpp.opencv_core.IplImage;

public class OpenCVFilterAnd extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = LoggerFactory.getLogger(OpenCVFilterAnd.class.getCanonicalName());

	int frameCount = 0;
	
	transient IplImage buffer = null;
	transient IplImage negativeImage = null;

	public OpenCVFilterAnd(VideoProcessor vp, String name, HashMap<String, IplImage> source,  String sourceKey) {
		super(vp, name, source, sourceKey);
	}
	
	@Override
	public BufferedImage display(IplImage image, OpenCVData data) {

		return buffer.getBufferedImage(); // TODO - ran out of memory here
	}


	@Override
	public IplImage process(IplImage image, OpenCVData data) {

		++frameCount;

		if ((negativeImage == null) && (frameCount % 100 == 0)) {
			negativeImage = image.clone();
			cvNot(image, negativeImage);
		}

		if (buffer == null) {
			buffer = image.clone();
		}

		// cvAnd (src1, src2, dst, mask)

		if (negativeImage != null) {
			cvAnd(image, negativeImage, buffer, null);
		}

		return buffer;

	}

	@Override
	public void imageChanged(IplImage image) {
		// TODO Auto-generated method stub
		
	}

}