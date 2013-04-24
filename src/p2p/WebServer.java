package p2p;

import java.io.*;
import java.net.*;
import java.util.*;

public class WebServer implements Runnable {
	Socket conn;
	static Map<String, String> file_table;	// maintains all files for a peer. Key is md5 hash filename, value is the file data
	static ArrayList<String> peer_filenames;	// list of all filenames this peer is associated with
	
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
		peer_filenames = new ArrayList<String>();
		
		String md5 = MD5("local.html");
		md5 = md5.substring(md5.length() - 4);
		file_table.put(md5, "<html><head><title>Local Page</title></head><body><p>This is the local page on the peer server " + ip.getHostAddress() + " port " + port + "</p></body></html>");
		peer_filenames.add("local.html");

		ServerSocket svc = new ServerSocket(port, 5);	// listen on port specified
		new Thread(new Browser()).start();	// start command line input to request web pages

		
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
					line.trim();
					first_header = line.split(" ");
					command = first_header[0];
					
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
						path = first_header[1].substring(1);

						while(!(line = fromClient.readLine()).trim().isEmpty())
						{
							if(line.substring(0, line.indexOf(' ')).equals("Content-Length:"))
							{
								line = line.substring(line.indexOf(' '));
								clength = line.trim();
								contentLength = Integer.parseInt(clength);
								//System.out.println(clength);
							}
						}
						byte[] mainContent = new byte[contentLength];

						/* the read method does not always read in all the bytes, so we need to loop until the buffer is full */
						for (int x = 0; x < mainContent.length;) {
								x += conn.getInputStream().read(mainContent, x, mainContent.length-x);
						}
						String cont = new String(mainContent);
						
						String md5 = MD5(path);
						md5 = md5.substring(md5.length() - 4);
												
						file_table.put(md5, cont);
						peer_filenames.add(path);
						
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

					} else if (command.equals("DELETE")) {
						path = first_header[1].substring(1);
						
						file_table.remove(path);
					}

				} else if (line.equals("LIST")) {
					for (String s : peer_filenames) {
						toClient.writeBytes(s + "\n");
					}
				}

			}

			
			conn.close();
			return;
			
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	

	/* takes file path as argument */
	public String get(String path) {
		String http_data;
		
		path = MD5(path.substring(1));
		path = path.substring(path.length() - 4);
		
		System.out.println("GET: " + path);
		
		//Get int rep of hash
		int value = 0;
		int multiplier = 1000;
		for(int j = 0; j < path.length(); j++)
		{
			value = value + Character.getNumericValue(path.charAt(j)) * multiplier;
			multiplier = multiplier / 10;
		}
		System.out.println("unique value: " + value);
		
		
		String contents = file_table.get(path);	// start string path after the '/'
		if (contents == null) {
			http_data = "HTTP/1.1 404 Not Found\nContent-Length: 0\n\n";
		}
		else {
			http_data = "HTTP/1.1 200 OK\nContent-Length: " + contents.length() + "\n\n" + contents;
		}
		
		return http_data;
	}

	public static String MD5(String md5) {
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

}
