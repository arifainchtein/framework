package com.teleonome.framework.mnemosyne.operations;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.teleonome.framework.TeleonomeConstants;
import com.teleonome.framework.denome.DenomeManager;
import com.teleonome.framework.denome.Identity;
import com.teleonome.framework.utils.Utils;

public class ListFileInfoOp extends MnemosyneOperation {
	Logger logger;
	public ListFileInfoOp(JSONObject m) {
		super(m);
		logger = Logger.getLogger(getClass());
		// TODO Auto-generated constructor stub
	}

	@Override
	public void process(DenomeManager denomeManager, long currentTimeMillis, String formatedCurrentTimestamp,
			String formatedCurrentDate, String formatedCurrentTime, ZoneId zoneId) {
		// TODO Auto-generated method stub
		//
		// we are going to create a new Dene andd it to the mnemosyne
		// there can be multiple targets, ie you can add a dene to 
		// the current hour, current day etc
		// so first get the targets, which are identity pointers that point to the chain where this new dene
		//needs to be created
		//
		JSONArray targetsJSONArray = denomeManager.getAllDeneWordAttributeByDeneWordTypeFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENE_WORD_TYPE_TARGET, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		//
		// ok, now we have the targets, and we know we need to create a Dene, so first create a Dene
		// add all the DeneWords and then add the Dene to the Target
		// each target looks like:
		//   "@MonoNanny:Mnemosyne:Mnemosyne Today",
		//
		JSONObject fileLastModifiedOnDeneWord, fileSizeDeneWord,fileNameDeneWord;

		String pathToFiles = (String) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_FILE_LIST_PATH, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		//
		// the name of this new dene is a deneword where the name is "New Dene Name'
		String newDeneName = (String) denomeManager.getDeneWordAttributeByDeneWordNameFromDene(mnemosyneDene, TeleonomeConstants.MNEMOSYNE_DENEWORD_NEW_DENE_NAME, TeleonomeConstants.DENEWORD_VALUE_ATTRIBUTE);
		File pathToFilesDir = new File(pathToFiles);
		File[] files=pathToFilesDir.listFiles();
		File aFile;
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TeleonomeConstants.MNEMOSYNE_TIMESTAMP_FORMAT);
		Instant fileInstant;
		String formatedFileLastModifiedOnTime;
		String destinationIdentityPointer;
		JSONObject destinationDeneChain, newDene;
		JSONArray destinationDenes, newDeneDeneWords;
		int newDenePosition;
		for(int j=0;j<targetsJSONArray.length();j++){
			destinationIdentityPointer = targetsJSONArray.getString(j);
			logger.debug("destinationIdentityPointer=" + destinationIdentityPointer);
			destinationDeneChain = denomeManager.getDenomicElementByIdentity(new Identity(destinationIdentityPointer));
			destinationDenes = destinationDeneChain.getJSONArray("Denes");
			newDenePosition=0;
			if(destinationDenes==null){
				destinationDenes=new JSONArray();
				destinationDeneChain.put("Denes", destinationDenes);
				newDenePosition=1;
			}

			//
			// for this target list the files and create a dene for each one
			//
			for(int k=0;k<files.length;k++){
				aFile = files[k];
				newDene = new JSONObject();
				newDeneDeneWords = new JSONArray();
				newDene.put("DeneWords", newDeneDeneWords);
				newDene.put("Name", newDeneName);
				//
				// now that we have the name, look for the next position, this means, go to the chain
				// and see what is the position of the last copy of that dene that we have in the chain
				// destinationDenes could have several copies of the same dene
				// for example, if this is a daily, and you are running the process every hour
				// and is 8am, by the time you are adding a new dene, there are already 7 denes with the same
				//name, but what distinguish them is that each dene has a an attribute called "Position"
				// that way when we want to make reports or operationswe know which is the last value

				newDenePosition = denomeManager.getNextPostionForDeneInMnemosyneChain(destinationDeneChain, newDeneName);
				newDene.put("Position", newDenePosition);
				//
				//
				// also add when this dene was created


				newDene.put("Timestamp", formatedCurrentTime);
				newDene.put("Timestamp Milliseconds", currentTimeMillis);

				//
				// now add the following 3 denewords to this dene
				//
				// 1)The deneword that represents the file name
				//
				fileNameDeneWord = Utils.createDeneWordJSONObject("File Name", aFile.getName() ,null,"String",true);
				newDeneDeneWords.put(fileNameDeneWord);
				//
				// 2)The deneword that represents the file size
				//
				fileSizeDeneWord = Utils.createDeneWordJSONObject("File Size", aFile.length()/1024 ,"kb","String",true);
				newDeneDeneWords.put(fileSizeDeneWord);
				//
				// 3)The deneword that represents the file last modified date
				//
				
				fileInstant = Instant.ofEpochMilli(aFile.lastModified());
				LocalDateTime fileLastMofiedOnLocalDateTime = LocalDateTime.ofInstant(fileInstant, zoneId);
				formatedFileLastModifiedOnTime = fileLastMofiedOnLocalDateTime.format(formatter);
				fileLastModifiedOnDeneWord = Utils.createDeneWordJSONObject("Last Modified On", formatedFileLastModifiedOnTime ,null,"String",true);
				newDeneDeneWords.put(fileLastModifiedOnDeneWord);
				//
				// finally add the new dene to the chain
				destinationDenes.put(newDene);

			}
		} // for targets
	}

}
