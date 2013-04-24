package p2p;

import java.io.*;
import java.math.BigInteger;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class WebServer implements Runnable {
	Socket conn;
	Map<String, String> file_table;
	
	WebServer(Socket sock) {
		this.conn = sock;
		file_table = new HashMap<String, String>();
		// for now add temp file index.html until PUT is implemented
		file_table.put("index.html", "TESTING 123");
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
			String line, command, path, data, clength, content;
			int contentLength = 0;
			while ((line = fromClient.readLine()) != null) {
				if (line.contains(" ")) {
					command = line.substring(0, line.indexOf(' '));
					
					if (command.equals("GET")) {
						data = get();
						System.out.println(data);
					} 
					else if (command.equals("PUT")) 
					{
						
						line = line.substring(line.indexOf(' '));
						path = line.substring(0, line.indexOf(' '));
						
						while(!(line = fromClient.readLine()).trim().isEmpty())
						{
							if(line.substring(0, line.indexOf(' ')).equals("Content-Length:"))
							{
								line = line.substring(line.indexOf(' '));
								clength = line.trim();
								contentLength = Integer.parseInt(clength);
							}
						}
						
						byte[] mainContent = new byte[contentLength];
						//conn.getInputStream().read(mainContent, 0, contentLength);
						conn.getInputStream().read(mainContent);
						String cont = new String(mainContent);
						
						System.out.println(cont);
						
						try {
							URL url = new URL("http://127.0.0.1:12345");
						
							HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
							httpCon.setDoOutput(true);
							OutputStreamWriter out = new OutputStreamWriter(
							    httpCon.getOutputStream());
							
							out.write("HTTP/1.1 200 OK\r\n");                            
							out.write("Content-Length: " + 0 + "\r\n");
							out.write("\r\n");
							out.close();
							
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
				}
				
			}
			
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	public String MD5(String md5) {
		   try {
		        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
		        byte[] array = md.digest(md5.getBytes());
		        StringBuffer sb = new StringBuffer();
		        for (int i = 0; i < array.length; ++i) {
		          sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100).substring(1,3));
		       }
		        return sb.toString();
		    } catch (java.security.NoSuchAlgorithmException e) {
		    }
		    return null;
	}
	
	public String get() {
		
		return file_table.get("index.html");
	}
	
	public Boolean put(String path) {
		URL url;
		File f = new File(path);
		int length = (int) f.length();
		String contentLength = Integer.toString(length);
		try {
			url = new URL("localhost:12345");
		
			HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
			httpCon.addRequestProperty("Content-Length", contentLength);
			httpCon.setRequestMethod("PUT");
			OutputStreamWriter out = new OutputStreamWriter(
			    httpCon.getOutputStream());
			out.write("Resource content");
			out.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return false;
	}
}
