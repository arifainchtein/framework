package com.teleonome.framework.denome;

public class Dene {
	 
	private String name="";
	private String type="";
	private String deneChainName="";
	private String nucleusName="";
	private DeneWord[] deneWords = new DeneWord[100];
	
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getDeneChainName() {
		return deneChainName;
	}
	public void setDeneChainName(String deneChainName) {
		this.deneChainName = deneChainName;
	}
	public String getNucleusName() {
		return nucleusName;
	}
	public void setNucleusName(String nucleusName) {
		this.nucleusName = nucleusName;
	}
	public DeneWord[] getDeneWords() {
		return deneWords;
	}
	public void setDeneWords(DeneWord[] deneWords) {
		this.deneWords = deneWords;
	}
	
	
}
