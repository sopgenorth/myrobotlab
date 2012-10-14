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

import static com.googlecode.javacv.cpp.opencv_core.CV_FONT_HERSHEY_PLAIN;
import static com.googlecode.javacv.cpp.opencv_core.CV_RGB;
import static com.googlecode.javacv.cpp.opencv_core.IPL_DEPTH_32F;
import static com.googlecode.javacv.cpp.opencv_core.cvCopy;
import static com.googlecode.javacv.cpp.opencv_core.cvCreateImage;
import static com.googlecode.javacv.cpp.opencv_core.cvMinMaxLoc;
import static com.googlecode.javacv.cpp.opencv_core.cvPutText;
import static com.googlecode.javacv.cpp.opencv_core.cvRectangle;
import static com.googlecode.javacv.cpp.opencv_core.cvResetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvScalar;
import static com.googlecode.javacv.cpp.opencv_core.cvSetImageROI;
import static com.googlecode.javacv.cpp.opencv_core.cvSize;
import static com.googlecode.javacv.cpp.opencv_imgproc.CV_TM_SQDIFF;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvMatchTemplate;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;
import org.myrobotlab.service.OpenCV;

import com.googlecode.javacv.cpp.opencv_core.CvFont;
import com.googlecode.javacv.cpp.opencv_core.CvPoint;
import com.googlecode.javacv.cpp.opencv_core.CvRect;
import com.googlecode.javacv.cpp.opencv_core.IplImage;

// TODO - http://opencv.willowgarage.com/wiki/FastMatchTemplate

public class OpenCVFilterMatchTemplate extends OpenCVFilter {

	private static final long serialVersionUID = 1L;

	public final static Logger log = Logger
			.getLogger(OpenCVFilterMatchTemplate.class.getCanonicalName());
	
	public IplImage template = null;
	IplImage res = null;
	double[] minVal = new double[1];
	double[] maxVal = new double[1];
	CvPoint minLoc = new CvPoint();
	CvPoint maxLoc = new CvPoint();

	CvPoint tempRect0 = new CvPoint();
	CvPoint tempRect1 = new CvPoint();
	
	CvPoint centeroid = new CvPoint(0, 0);
	
	int i = 0;

	public OpenCVFilterMatchTemplate(OpenCV service, String name) {
		super(service, name);
	}

	@Override
	public BufferedImage display(IplImage image, Object[] data) {
		
		return image.getBufferedImage();
		/* display problem
		if (res != null)
		{
			return res.getBufferedImage();
		} else {
			return image.getBufferedImage();
		}
		*/
	}

	int clickCount = 0;
	int x0,y0,x1,y1;
	public CvRect rect = new CvRect();
	public boolean makeTemplate = false;
	public void samplePoint(MouseEvent event) {
		// MouseEvent me = (MouseEvent)params[0];
		if (event.getButton() == 1) {
			if (clickCount%2 == 0)
			{
				x0 = event.getPoint().x;
				y0 = event.getPoint().y;
			} else {
				x1 = event.getPoint().x;
				y1 = event.getPoint().y;
				rect.x(x0);
				rect.y(y0);
				rect.width(Math.abs(x1-x0));
				rect.height(Math.abs(y1-y0));
				makeTemplate = true;
			}
		}
		++clickCount;
	}
	
	
	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public void loadDefaultConfiguration() {
		// Read in the template to be used for matching:
		//template = cvLoadImage("template.jpg");
	}
	
	CvPoint textpt = new CvPoint(10,20);
	private CvFont font = new CvFont(CV_FONT_HERSHEY_PLAIN, 1, 1);
	
	public int matchRatio = Integer.MAX_VALUE;
	boolean isTracking = false;
	
	@Override
	public IplImage process(IplImage image) {
		// cvMatchTemplate(iamge, arg1, arg2, arg3);

		/*
		if (res == null && template != null) // || dim size change 
		{
			res = cvCreateImage( cvSize( image.width() - template.width() + 1, 
					image.height() - template.height() + 1), IPL_DEPTH_32F, 1 );
		}
		*/

		// CV_TM_CCOEFF_NORMED
		//cv.cvMatchTemplate(arg0, arg1, arg2, arg3);
		if (template != null && res != null)
		{
			// TODO - DISPLAY RES SO THAT RESULTS FORM DIFFERENT FN's CAN BE EXAMINED
			cvMatchTemplate(image, template, res, CV_TM_SQDIFF);
		// cvNormalize( ftmp[i], ftmp[i], 1, 0, CV_MINMAX );
		
		
			cvMinMaxLoc ( res, minVal, maxVal, minLoc, maxLoc, null );

			tempRect0.x(minLoc.x());
			tempRect0.y(minLoc.y());
			tempRect1.x(minLoc.x() + template.width());
			tempRect1.y(minLoc.y() + template.height());
		}

		if (makeTemplate)
		{
			makeTemplate = false;
			template = cvCreateImage( cvSize( rect.width(), 
					rect.height()), image.depth(), image.nChannels() );
			/* copy ROI to subimg */
			cvSetImageROI(image, rect);
			cvCopy(image, template, null);
			cvResetImageROI(image);
			myService.invoke("publishTemplate", name, template.getBufferedImage());
			myService.invoke("publishIplImageTemplate", template); // FYI - IplImage is not serializable
			res = cvCreateImage( cvSize( image.width() - template.width() + 1, 
					image.height() - template.height() + 1), IPL_DEPTH_32F, 1 );
		}
		
		if (template != null)
		{
			String text = ""+minVal[0];
			
			//textpt.y(20);
			//cvPutText(image, text, textpt, font, CV_RGB(254, 254, 254));
			textpt.y(20);
			matchRatio = (int)(minVal[0]/((tempRect1.x() - tempRect0.x()) * (tempRect1.y() - tempRect0.y())));		
			cvPutText(image, "" + matchRatio , textpt, font, CV_RGB(254, 254, 254));
			
			if (matchRatio < 500)
			{
				// draw rectangle
				cvRectangle( image, 
						tempRect0, 
						tempRect1,
						cvScalar( 255, 255, 255, 0 ), 1, 0, 0 );	

				textpt.y(30);
				cvPutText(image, "locked" , textpt, font, CV_RGB(254, 254, 254));
				centeroid.x(tempRect0.x() + ((tempRect1.x() - tempRect0.x())/2));
				centeroid.y(tempRect0.y() + ((tempRect1.y() - tempRect0.y())/2));
				myService.invoke("publish", centeroid);
				if (isTracking != true) // message clutter optimization
				{
					myService.invoke("isTracking", true);
				}
				isTracking = true;

			} else {
				if (isTracking != false)
				{
					myService.invoke("isTracking", false);
				} 
				isTracking = false;
			}
		}// if template != null

		
		return image;

	}

	
	
}