package com.teleonome.framework.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.SensorValue;
import com.teleonome.framework.exception.MissingDenomeException;

public class TestJSON {
	DenomeManager aDenomeManager;
	
	public TestJSON(){
		
			com.teleonome.framework.denome.DenomeViewManager aDenomeViewerManager = new com.teleonome.framework.denome.DenomeViewManager();
			try {
				String pulse = FileUtils.readFileToString(new File("Test.pulse"));
				
				String treeData = aDenomeViewerManager.renderJQTreeData(pulse);
				
				System.out.println(treeData);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			
			
			
		
			
	}
	
	
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		new TestJSON();
	}

}
