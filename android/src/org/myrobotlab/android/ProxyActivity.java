/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//package org.myrobotlab.android;
package org.myrobotlab.android; 

import org.myrobotlab.service.Proxy;

import android.os.Bundle;

// references :
// http://www.dreamincode.net/forums/topic/130521-android-part-iii-dynamic-layouts/

//  extends ServiceActivity
public class ProxyActivity extends ServiceActivity {

	Proxy myService = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.proxy);
        
        myService = (Proxy)sw.service;   
        // setText(R.id.udpdata, myService.servicePort);
        //setContentView(layout);        

    }

	@Override
	public void attachGUI() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void detachGUI() {
		// TODO Auto-generated method stub
		
	}

}
