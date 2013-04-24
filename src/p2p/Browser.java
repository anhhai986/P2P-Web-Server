package p2p;

import java.io.*;
import java.net.*;

public class Browser implements Runnable {

	public void run() {
		String input;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));	// console input
		
		Socket conn;
		try {
			conn = new Socket("localhost", 12345);
		
			BufferedReader fromServer = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			DataOutputStream toServer = new DataOutputStream(conn.getOutputStream());
			
			while (!(input = br.readLine()).equals("quit")) {
				toServer.writeBytes(input + '\n');
				
			}
		} catch (Exception e) {
			System.out.println(e);
		}
	}
}
