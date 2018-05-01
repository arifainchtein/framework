package com.teleonome.framework.denome;

public class Identity {
	public String organismName="";
	public String teleonomeName="";
	public String nucleusName="";
	public String deneChainName="";
	public String deneName="";
	public String deneWordName="";
	public boolean command=false;
	public String commandValue="";
	public String identityStringValue="";
	private boolean deneWord=false;
	private boolean dene=false;
	private boolean deneChain=false;
	private boolean nucleus=false;
	
	private int timeSeriesElementPosition=0;
	
	public Identity(String t, String n){
		teleonomeName = t;
		nucleusName = n;
		identityStringValue="@" + teleonomeName +":"+nucleusName;
	}
	
	public Identity(String t, String n, String mc, String m){
		teleonomeName = t;
		nucleusName = n;
		deneChainName = mc;
		deneName = m;
		dene=true;
		identityStringValue="@" + teleonomeName +":"+nucleusName +":"+deneChainName +":"+deneName;
	}
	
	public Identity(String t, String n, String mc){
		teleonomeName = t;
		nucleusName = n;
		deneChainName = mc;
		deneChain=true;
		identityStringValue="@" + teleonomeName +":"+nucleusName +":"+deneChainName;
	}
	
	public Identity(String t, String n, String mc, String m, String dw){
		teleonomeName = t;
		nucleusName = n;
		deneChainName = mc;
		deneName = m;
		deneWordName=dw;
		deneWord=true;
		identityStringValue="@" + teleonomeName +":"+nucleusName +":"+deneChainName +":"+deneName + ":" + deneWordName;
	}
	
	public Identity(String t, String n, String mc, String m, String dw, int tsep){
		teleonomeName = t;
		nucleusName = n;
		deneChainName = mc;
		deneName = m;
		deneWordName=dw;
		deneWord=true;
		timeSeriesElementPosition=tsep;
		identityStringValue="@" + teleonomeName +":"+nucleusName +":"+deneChainName +":"+deneName + ":" + deneWordName + ":"+ timeSeriesElementPosition;
	}
	
	public Identity(String denePointer){
		if(denePointer.startsWith("$")){
			command=true;
			commandValue=denePointer;
			
		}else if(denePointer.startsWith("@")){
			denePointer = denePointer.substring(1);
		}
		if(!command){
			String[] tokens = denePointer.split(":");
			switch(tokens.length){
			case 2:
				//
				// Organism and Teleonome
				//organismName = tokens[0];
				teleonomeName = tokens[0];
				nucleusName = tokens[1];
				identityStringValue="@" + teleonomeName +":"+nucleusName ;
				nucleus=true;
				break;
			case 3:
				teleonomeName = tokens[0];
				nucleusName = tokens[1];
				deneChainName = tokens[2];
				identityStringValue="@" + teleonomeName +":"+nucleusName +":"+deneChainName;
				deneChain=true;
				break;
			case 4:
				teleonomeName = tokens[0];
				nucleusName = tokens[1];
				deneChainName = tokens[2];
				deneName = tokens[3];
				identityStringValue="@" + teleonomeName +":"+nucleusName +":"+deneChainName +":"+deneName;
				dene=true;
				break;
			case 5:
				teleonomeName = tokens[0];
				nucleusName = tokens[1];
				deneChainName = tokens[2];
				deneName = tokens[3];
				deneWordName = tokens[4];
				identityStringValue="@" + teleonomeName +":"+nucleusName +":"+deneChainName +":"+deneName + ":" + deneWordName;
				deneWord=true;
				break;
			case 6:
				organismName = tokens[0];
				teleonomeName = tokens[1];
				nucleusName = tokens[2];
				deneChainName = tokens[3];
				deneName = tokens[4];
				deneWordName = tokens[5];
				identityStringValue="@"+organismName + ":" + teleonomeName +":"+nucleusName +":"+deneChainName +":"+deneName + ":" + deneWordName;
				deneWord=true;
				break;
			}
		}
	}

	public boolean isNucleus(){
		return nucleus;
	}
	
	public boolean isDeneChain(){
		return deneChain;
	}
	
	public boolean isDene(){
		return dene;
	}
	
	public boolean isDeneWord(){
		return deneWord;
	}
	
	
	public String getCommandValue(){
		return commandValue;
	}
	public boolean isCommand(){
		return command;
	}
	public String getOrganismName() {
		return organismName;
	}

	public void setOrganismName(String organismName) {
		this.organismName = organismName;
	}

	public String getTeleonomeName() {
		return teleonomeName;
	}

	public void setTeleonomeName(String teleonomeName) {
		this.teleonomeName = teleonomeName;
	}

	public String getNucleusName() {
		return nucleusName;
	}

	public void setNucleusName(String nucleusName) {
		this.nucleusName = nucleusName;
	}

	public String getDenechainName() {
		return deneChainName;
	}

	public void setDenechainName(String denechainName) {
		this.deneChainName = denechainName;
	}

	public String getDeneName() {
		return deneName;
	}

	public void setDeneName(String deneName) {
		this.deneName = deneName;
	}

	public String getDeneWordName() {
		return deneWordName;
	}

	public void setDeneWordName(String deneWordName) {
		this.deneWordName = deneWordName;
	}
	public void setTimeSeriesElementPosition(int t) {
		timeSeriesElementPosition = t;
	}
	public int getTimeSeriesElementPosition() {
		return timeSeriesElementPosition;
	}
	public String toString(){
		return identityStringValue;
	}
}
