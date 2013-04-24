package p2p;

import java.io.*;
import java.net.*;
import java.util.*;

public class WebServer implements Runnable {
	Socket conn;
	static Map<String, String> file_table;	// maintains all files for this peer. Key is md5 hash filename, value is the file data
	static ArrayList<String> peer_filenames;	// list of all filenames this peer is associated with
	
	// The peers map is accessed by the int value of the md5 hash of its address in ip:port form
	// The peer_info list holds the ip:port form of all peers in the group
	static Map<Integer, ArrayList<String>> peers;	/* hashtable where key is a peer-name hash and value is an arraylist of 
													   the files that it is responsible for */
	static ArrayList<String> peer_info;		// list containg the ip and port in a string to send 'ADD' commands to peers
	
	static int hash_value;
	
	WebServer(Socket sock) {
		this.conn = sock;

	}
	
	public static void main(String args[]) throws Exception {	
		int port;
		
		if (args.length != 2) {
			System.err.println("usage: p2pws -p <port>");
			System.exit(1);
		}
		
		// Setup for the static tables on this particular web server
		InetAddress ip = InetAddress.getLocalHost();
		port = Integer.parseInt(args[1]);
		// Create tables here
		file_table = new HashMap<String, String>();
		peer_filenames = new ArrayList<String>();
		peers = new HashMap<Integer, ArrayList<String>>();
		peer_info = new ArrayList<String>();
		
		// add initial page 'local.html'
		String md5 = MD5("local.html");
		md5 = md5.substring(md5.length() - 4);
		file_table.put(md5, "<html><head><title>Local Page</title></head><body><p>This is the local page on the peer server " + ip.getHostAddress() + " port " + port + "</p></body></html>");
		peer_filenames.add("local.html");
		peer_info.add(ip.getHostAddress() + ":" + port);

		md5 = MD5(ip.getHostAddress() + ":" + port);
		md5 = md5.substring(md5.length() - 4);
		hash_value = hashToInt(md5);
		// create an entry in peers for this host
		peers.put(hash_value, null);
		

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
					
					if (command.equals("ADD")) {
						if (first_header[3] != null && first_header[3].equals("norecurse")) {
							add(first_header[1], first_header[2], false);	// second argument says no recurse
						} else {
							add(first_header[1], first_header[2], true);	// second argument says to recurse
						}
						
						// now begin sending ADD to every other host in the group
						
					}
					else if (command.equals("GET")) {
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
						// also need to update peers mapping
						int hash_value = hashToInt(md5);
						peers.put(hash_value, new ArrayList<String>());
						peers.get(hash_value).add(path);

						
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
						// to remove in peer_filenames we need to find the index of the filename to remove
						int tmp_count = 0;
						for (String s : peer_filenames) {
							if (s.equals(path)) {
								break;
							}
							tmp_count++;
						}
						peer_filenames.remove(tmp_count);
						
						// now remove the index in the list on peers
						String md5 = MD5(path);
						md5 = md5.substring(md5.length() - 4);
						int hash_value = hashToInt(md5);
						
						tmp_count = 0;
						for (String s : peers.get(hash_value)) {
							if (s.equals(path)) {
								break;
							}
							tmp_count++;
						}
						peers.get(hash_value).remove(tmp_count);
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
	
	public void add(String ip, String port, boolean recurse) {
		// ip:port combination gets md5 hash and uses that to reference a particular peer from now on
		// the md5 hash gets hashed again in a HashMap
		String host_md5 = ip + ":" + port;
		host_md5 = MD5(host_md5);
		host_md5 = host_md5.substring(host_md5.length() - 4);
		int host_value = hashToInt(host_md5);
		
		// add the new host to the list on this current web server
		// it currently has no list of files that it is responsible for
		peers.put(host_value, new ArrayList<String>());
		
		if (recurse == true) {
			String peer_ip, peer_port;
			for (String s : peer_info) {	// for each peer in the group, do an ADD with norecurse
				// parse the peer_info string into ip and port
				peer_ip = s.substring(0, s.indexOf(":"));
				peer_port = s.substring(s.indexOf(":"), s.length());
				try {
					Socket conn = new Socket(ip, Integer.parseInt(port));
					DataOutputStream toServer = new DataOutputStream(conn.getOutputStream());
					
					toServer.writeBytes("ADD " + ip + " " + port + "norecurse");
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		}
		
		/* now we need to check if new peer is immediate predecessor
		 	We need to find the current predecessor and see if it has a greater hash value.
		 	Do this by finding the greatest hash value smaller than this server's hash value.
		 	Then do a check to see if the added peer has a greater hash value than this. */
		int max_value=0;
		for (String s : peer_info) {
			// we will use peer_value to compare the current predecessor at the end
			String peer_md5 = MD5(s);
			peer_md5 = peer_md5.substring(peer_md5.length() - 4);
			int peer_value = hashToInt(peer_md5);
			
			if (peer_value > max_value && peer_value < hash_value) {
				max_value = peer_value;
			}
		}
		// now that we have the current predecessor's hash_value we can compare it with the host to add
		if (max_value < host_value) {	// if true, we need to do some put'ing and delete'ing
			try {
				Socket conn = new Socket(ip, Integer.parseInt(port));
				DataOutputStream toServer = new DataOutputStream(conn.getOutputStream());
				
				// search every string from this server's list of files
				for (String s : peer_filenames) {
					String tmphash = MD5(s);
					tmphash = tmphash.substring(tmphash.length() - 4);
					int tmpvalue = hashToInt(tmphash);
					// need content for the filename
					String content = file_table.get(tmphash); 	// file_table uses md5 hash as key
					
					if (tmpvalue < host_value) {
						toServer.writeBytes("PUT /" + s + "HTTP/1.1\nContent-Length: " + content.length() + "\n\n" + content);
					}
					
				}
				
				
				
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		
		peer_info.add(host_md5);	// add ip:port to peer_info list
	}

	/* takes file path as argument */
	public String get(String path) {
		String http_data;
		
		path = MD5(path.substring(1));
		path = path.substring(path.length() - 4);
		
		System.out.println("GET: " + path);
		
		//Get int rep of hash
		int value = hashToInt(path);
	
		
		String contents = file_table.get(path);	// start string path after the '/'
		if (contents == null) {
			http_data = "HTTP/1.1 404 Not Found\nContent-Length: 0\n\n";
		}
		else {
			http_data = "HTTP/1.1 200 OK\nContent-Length: " + contents.length() + "\n\n" + contents;
		}
		
		return http_data;
	}
	
	public static int hashToInt(String md5_path) {
		//Get int rep of hash
		int value = 0;
		int multiplier = 1000;
		for(int j = 0; j < md5_path.length(); j++)
		{
			value = value + Character.getNumericValue(md5_path.charAt(j)) * multiplier;
			multiplier = multiplier / 10;
		}
		System.out.println("unique value: " + value);
		
		return value;
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
