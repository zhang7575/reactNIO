package com.paypal.jzhang13.reactorNIO;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class EchoClient {

    public static void main(String[] args) {

        Socket client = null;
        PrintWriter writer = null;
        BufferedReader reader = null;

        try {
            client = new Socket();
            client.connect(new InetSocketAddress("localhost", 12345));
            writer = new PrintWriter(client.getOutputStream(), true);
            writer.println("Hello");
            Thread.sleep(1000);
            writer.println("My ");
//           Thread.sleep(1000);
//            writer.println("Beautiful ");
//            Thread.sleep(1000);
//            writer.println("World!");
            writer.flush();

            reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            //reader.lines().forEach(l->System.out.println(l));;
            System.out.println(reader.readLine());
            System.out.println(reader.readLine());
//            System.out.println(reader.readLine());



        } catch (Exception e) {
            e.printStackTrace();
        } 
        finally {
            try {
                if (writer != null) {
                    writer.close();
                }
                if (reader != null) {
                    reader.close();
                }
                if (client != null) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}