package com.teleonome.framework.fertilizer;

/**
 * A pointer into a sperm, e.g. @Sperm:Hypothalamus:General Status LED:Identity LED (the last token is missing for
 * an action). Moved here from the fertilizer project (2026-11-13) with FertilizerEngine.
 */
public class FertilizationIdentity {

	public String identifier, moduleName="", homeBoxName="", homeBoxDeneName="";

	public FertilizationIdentity(String denePointer){
		denePointer = denePointer.substring(1);
		String[] tokens = denePointer.split(":");
		identifier= tokens[0];
		moduleName= tokens[1];
		homeBoxName= tokens[2];
		if(tokens.length>3) {
			homeBoxDeneName= tokens[3];
		}
	}

	public String getHomeoBoxName(){
		return homeBoxName;
	}

	public String getHomeBoxDeneName(){
		return homeBoxDeneName;
	}
}
