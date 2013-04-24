package p2p;

import java.io.*;
import java.net.*;
import java.util.*;

public class WebServer implements Runnable {
	Socket conn;
	Map<String, String> file_table;
	
	WebServer(Socket sock, int port) {
		// get ip address of this host
		try {
			InetAddress ip = InetAddress.getLocalHost();
			this.conn = sock;
			
			file_table = new HashMap<String, String>();
			file_table.put("local.html", "<html><head><title>Local Page</title></head><body><p>This is the local page on the peer server " + ip.getHostAddress() + " port " + port + "</p></body></html>");
			
		} catch (UnknownHostException e) {
			System.out.println(e);
		}
				
		// for now add temp file index.html until PUT is implemented
			file_table.put("test.html", "<html><head><title>test title</title></head></html>");
	}
	
	public static void main(String args[]) throws Exception {	
		int port;
		
		if (args.length != 2) {
			System.err.println("usage: p2pws -p <port>");
			System.exit(1);
		}
		
		//new Thread(new Browser()).start();	// start command line input to request web pages
		
		port = Integer.parseInt(args[1]);
		ServerSocket svc = new ServerSocket(port, 5);	// listen on port specified
		
		while (true) {
			Socket conn = svc.accept();	// get a connection from a client
			System.out.println("got connection");
			new Thread(new WebServer(conn, port)).start();
		}
	}
		
	public void run() {
		try {
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			DataOutputStream toClient = new DataOutputStream(conn.getOutputStream());
			String line, data;
			String[] first_header = new String[10];
			
			while (!(line = fromClient.readLine()).equals("")) {				
				//System.out.println(line);
				if (line.substring(0, line.indexOf(' ')).equals("GET")) {
					first_header = line.split(" ");
				}

			}

			data = get(first_header[1]);
			toClient.writeBytes(data);
			/*
			while((line = fromClient.readLine()) != null) {
				
			}*/
			
			conn.close();
			return;
			
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	/* takes file path as argument */
	public String get(String path) {
		String contents = file_table.get(path.substring(1));	// start string path after the '/'
		String http_data = "HTTP/1.1 200 OK\nContent-Length: " + contents.length() + "\n\n" + contents;

		return http_data;
	}
}
