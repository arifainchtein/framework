package com.teleonome.framework.denome;

public class MutationIdentity {
	public String organismName="";
	public String teleonomeName="";
	public String mutationName="";
	public String deneChainName="";
	public String deneName="";
	public String deneWordName="";
	public boolean command=false;
	public String commandValue="";
	public String identityStringValue="";
	
	public MutationIdentity(String t, String n, String mc, String m){
		teleonomeName = t;
		mutationName = n;
		deneChainName = mc;
		deneName = m;
		
		identityStringValue="@" + teleonomeName +":"+mutationName +":"+deneChainName +":"+deneName;
	}
	
	public MutationIdentity(String t, String n, String mc){
		teleonomeName = t;
		mutationName = n;
		deneChainName = mc;
		identityStringValue="@" + teleonomeName +":"+mutationName +":"+deneChainName;
	}
	
	public MutationIdentity(String t, String n, String mc, String m, String dw){
		teleonomeName = t;
		mutationName = n;
		deneChainName = mc;
		deneName = m;
		deneWordName=dw;
		identityStringValue="@" + teleonomeName +":"+mutationName +":"+deneChainName +":"+deneName + ":" + deneWordName;
	}
	
	public MutationIdentity(String denePointer){
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
				mutationName = tokens[1];
				identityStringValue="@" + teleonomeName +":"+mutationName ;

				break;
			case 3:
				teleonomeName = tokens[0];
				mutationName = tokens[1];
				deneChainName = tokens[2];
				identityStringValue="@" + teleonomeName +":"+mutationName +":"+deneChainName;

				break;
			case 4:
				teleonomeName = tokens[0];
				mutationName = tokens[1];
				deneChainName = tokens[2];
				deneName = tokens[3];
				identityStringValue="@" + teleonomeName +":"+mutationName +":"+deneChainName +":"+deneName;

				break;
			case 5:
				teleonomeName = tokens[0];
				mutationName = tokens[1];
				deneChainName = tokens[2];
				deneName = tokens[3];
				deneWordName = tokens[4];
				identityStringValue="@" + teleonomeName +":"+mutationName +":"+deneChainName +":"+deneName + ":" + deneWordName;

				break;
			case 6:
				organismName = tokens[0];
				teleonomeName = tokens[1];
				mutationName = tokens[2];
				deneChainName = tokens[3];
				deneName = tokens[4];
				deneWordName = tokens[5];
				identityStringValue="@"+organismName + ":" + teleonomeName +":"+mutationName +":"+deneChainName +":"+deneName + ":" + deneWordName;

				break;
			}
		}
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

	public String getMutationName() {
		return mutationName;
	}

	public void setMutationName(String m) {
		this.mutationName = m;
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

	public String toString(){
		return identityStringValue;
		
	}
}
