package com.teleonome.framework.tools;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CDevice;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;

public class PiI2CConsole {

	public static final int I2C_ADDR = 11;
	static I2CBus i2c = null;
    static I2CDevice device = null;

	 
	public static void main(String[] args) {
		try {
			i2c = I2CFactory.getInstance(I2CBus.BUS_1);
			String inpcom = args[0];  
	        device = i2c.getDevice(I2C_ADDR);
	        write(inpcom);
	        Thread.sleep(100);
	        String response=readLine();
	        	//	response=readLine();
	        System.out.println(inpcom + " produces response=" + response);
		} catch (UnsupportedBusNumberException | IOException e) {
			// TODO Auto-generated catch block
			System.out.println(getStringException(e));
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			System.out.println(getStringException(e));
		}
        
       
	}
	
	public static String getStringException(Exception e){
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String exceptionAsString = sw.toString();
		return exceptionAsString;
	}
	
	private static void write(String command) throws UnsupportedEncodingException,IOException{
        byte[] b2=command.getBytes("ISO-8859-1");
        device.write(I2C_ADDR,b2);      
}


private static String  readLine()throws UnsupportedEncodingException,IOException{

        byte[] b2 =  new byte[50];
        byte ab;
        int counter =0;
        device.read(b2,0,b2.length);
          for(int i=0;i<b2.length;i++){
                System.out.println("i="+i + " b2[i]=" + b2[i]);
           if(b2[i]<=0)b2[i]=32;
}
        String s3=new String(b2, "UTF-8").trim();
      //  System.out.println("readLine is returning=" + s3);
        return s3; 
  
}



}
