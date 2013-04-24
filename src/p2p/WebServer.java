package p2p;

import java.io.*;
import java.net.*;
import java.util.*;

public class WebServer implements Runnable {
	Socket conn;
	Dictionary<String, File> file_table;
	
	WebServer(Socket sock) {
		this.conn = sock;
		
		// for now add temp file index.html until PUT is implemented
		File tmp_file = new File("./data/index.html");
		file_table.put("index.html", tmp_file);
	}
	
	public static void main(String args[]) throws Exception {	
		int port;
		
		if (args.length != 2) {
			System.err.println("usage: p2pws -p <port>");
			System.exit(1);
		}
		
		new Thread(new Browser()).start();	// start command line input to request web pages
		
		port = Integer.parseInt(args[1]);
		ServerSocket svc = new ServerSocket(port, 5);	// listen on port specified
		
		while (true) {
			Socket conn = svc.accept();	// get a connection from a client
			new Thread(new WebServer(conn)).start();
		}
	}
		
	public void run() {
		try {
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			DataOutputStream toClient = new DataOutputStream(conn.getOutputStream());
			String line, command;
			
			while ((line = fromClient.readLine()) != null) {
				command = input.substring(0, input.indexOf(' '));
			}
			
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}
