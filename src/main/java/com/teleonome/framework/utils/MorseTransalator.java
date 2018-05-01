package com.teleonome.framework.utils;

public class MorseTransalator {

	private static String[] alpha = { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j",
			"k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v",
			"w", "x", "y", "z", "1", "2", "3", "4", "5", "6", "7", "8",
			"9", "0", " " };


	private static String[] dottie = { ".-", "-...", "-.-.", "-..", ".", "..-.", "--.",
			"....", "..", ".---", "-.-", ".-..", "--", "-.", "---", ".--.",
			"--.-", ".-.", "...", "-", "..-", "...-", ".--", "-..-",
			"-.--", "--..", ".----", "..---", "...--", "....-", ".....",
			"-....", "--...", "---..", "----.", "-----", "|" };

	public static String[] getMorseCode(String s) {
		String[] letters = s.toLowerCase().split("");
		String letter;
		String[] toReturn=new String[letters.length];
		for (int i = 0;i < letters.length; i++){
		    letter = letters[i];
		    next:
		    for(int j=0;j<alpha.length;j++) {
		    	if(alpha[j].equals(letter)) {
		    		toReturn[i]=dottie[j];
		    	}
		    }
		}
		return toReturn;
	}
}
