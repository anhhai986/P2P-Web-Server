package p2p;

import java.io.*;
import java.net.*;
import java.util.*;

public class WebServer implements Runnable {
	Socket conn;
	static Map<String, String> file_table;
	
	WebServer(Socket sock) {
		this.conn = sock;

	}
	
	public static void main(String args[]) throws Exception {	
		int port;
		
		if (args.length != 2) {
			System.err.println("usage: p2pws -p <port>");
			System.exit(1);
		}
		
		InetAddress ip = InetAddress.getLocalHost();
		port = Integer.parseInt(args[1]);
		file_table = new HashMap<String, String>();
		file_table.put("local.html", "<html><head><title>Local Page</title></head><body><p>This is the local page on the peer server " + ip.getHostAddress() + " port " + port + "</p></body></html>");

		
		new Thread(new Browser()).start();	// start command line input to request web pages
		
		ServerSocket svc = new ServerSocket(port, 5);	// listen on port specified
		
		while (true) {
			Socket connect = svc.accept();	// get a connection from a client
			//System.out.println("got connection");
			new Thread(new WebServer(connect)).start();
		}
	}
		
	public void run() {
		try {
			BufferedReader fromClient = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			DataOutputStream toClient = new DataOutputStream(conn.getOutputStream());

			String[] first_header = new String[10];

			String line, command, path = null, data = null, clength, content;
			int contentLength = 0;
			while ((line = fromClient.readLine()) != null) {
				if (line.contains(" ")) {
					command = line.substring(0, line.indexOf(' '));
					
					if (command.equals("GET")) {
						first_header = line.split(" ");
						try {
						data = get(first_header[1]);
						toClient.writeBytes(data);
						} catch (Exception e)
						{
							//System.out.println("path:" + path);
						}
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
								System.out.println(clength);
							}
						}
						byte[] mainContent = new byte[contentLength];
						//conn.getInputStream().read(mainContent, 0, contentLength);
						/*while(true)
						{
							if(conn.getInputStream().available() > 0)
							{
								conn.getInputStream().read(mainContent);
							}
							else
							{
								break;
							}
						}*/
						InputStream blah = conn.getInputStream();
						
						while (blah.read(mainContent) != -1)
						{
							//reading
						}
						
						String cont = new String(mainContent);
						
						/*for (int i = 0; i < mainContent.length; i++)
						{
							System.out.println(mainContent[i]);
						}*/
						
						System.out.println(cont);
						file_table.put("lol.html", cont);
						
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
		//System.out.println(path);
		String contents = file_table.get(path.substring(1));	// start string path after the '/'
		String http_data = "HTTP/1.1 200 OK\nContent-Length: " + contents.length() + "\n\n" + contents;

		return http_data;
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
