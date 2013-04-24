package p2p;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class Browser implements Runnable {

	public void run() {
		String input;
		String command;
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));	// console input

		try {
			while (!(input = br.readLine()).equals("quit")) {
				command = input.substring(0, input.indexOf(' '));
				
				
			}
		} catch (IOException e) {
			System.out.println("Error: Input error");
		}
	}
}
