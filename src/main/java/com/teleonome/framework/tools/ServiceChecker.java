package com.teleonome.framework.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ServiceChecker {

    public static void main(String[] args) {
        checkService("avahi-daemon");
        checkService("postgresql");
        checkNetwork();
    }

    private static boolean checkService(String serviceName) {
        String command = "systemctl is-active --quiet " + serviceName;
        boolean toReturn=false;
        try {
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println(serviceName + ": true");
                toReturn=true;
            } else {
                System.out.println(serviceName + ": false");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    private static boolean checkNetwork() {
        String command = "ping -c 1 8.8.8.8";
        boolean toReturn=false;
        try {
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Network: connected");
                toReturn=true;
            } else {
                System.out.println("Network: disconnected");
                restartNetworkInterface();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return toReturn;
    }

    private static void restartNetworkInterface() {
        try {
            System.out.println("Restarting wlan1...");
            Process ifdown = Runtime.getRuntime().exec("sudo ifdown wlan1");
            ifdown.waitFor();
            Process ifup = Runtime.getRuntime().exec("sudo ifup wlan1");
            ifup.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}