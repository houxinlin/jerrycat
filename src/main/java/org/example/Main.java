package org.example;

import org.example.utils.WarUtils;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        String webProjectPath ="/home/LinuxWork/test/servlet-container";
        try {
            WarUtils.unzipWar("/home/LinuxWork/test/servlet-container/servlet-0.0.1-SNAPSHOT.war",webProjectPath);
            JerryCat jerryCat = new JerryCat(webProjectPath);
            jerryCat.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}