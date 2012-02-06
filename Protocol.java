
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.net.Socket;
import java.io.IOException;

class Protocol {
	private String nick;
	private String destNick;
	private Socket proSock;
	private String delims = "[ ]+";
	private String[] text = null;
	private String password;

	//Storing current connections - nick and associated socket
	private static Hashtable<String, Socket> onlineNicks = new Hashtable<String, Socket>();
	//Storing all known nicks - nick, password and queue of messages waiting for user to come online (ie have an entry in the other hash table)
	private static Hashtable<NickPwd, List<String>> allNicks = new Hashtable<NickPwd, List<String>>();
	
	//constructor
	Protocol(Socket currSock) {
		nick = null;
		proSock = currSock;
		password = null;
	}
	
	public void process(String message) throws IOException {
	//false => error occurred, RETURN AN ERROR MESSAGE AND EXCEPTION
			System.out.println("processing " + message);
			if (message == null) {
				write("no message");
			}
			String[] tokens = null;
			tokens = message.split(delims);

			if(message.startsWith("USER") == true) { 
				//move nick and socket to onlineNicks, if they exist in allNicks
				if(nick != null) {
					write("Already signed in, please sign out first");
				}
				
				nick = tokens[1];
				password = tokens[2];
				
				if(allNicks.containsKey(nick)==false){
					write("You must first create a user account");
				}
				else {
					//CHECK IF PASSWORD IS CORRECT
					onlineNicks.put(nick, proSock);
					//send their list of offline messages
					List<String> msgs = allNicks.get(nick);
					System.out.println(msgs.toString());
					msgs.clear();
					//update their msgs list to empty
					allNicks.put(new NickPwd(nick, password), msgs);
					write("Hello " + nick);
				}
			}
			else if(message.startsWith("NOOP")==true){
				//respond to client
				write("Acknowledged");
			}
			else if(message.startsWith("WHO")==true){
				write(onlineNicks.toString());
			}
			else if(message.startsWith("MSG")== true) { //takes arg dest nick, msg {
				destNick = tokens[1];
				//fill the text array with the message to be sent
				if(tokens.length > 302) {
					write("message too long");
				}
				text = new String[tokens.length-2];
				for (int i = 2; i < tokens.length; i++) {
					System.out.println("i = " + i);
					System.out.println("length of tokens is " + tokens.length);
					System.out.println("adding " + tokens[i]);
					//System.out.println(" to the string " + text[i-2]);
					text[i-2] = tokens[i];
				}
				//look for dest nick in storage table
				if(onlineNicks.containsKey(destNick)==false) {
					//add message to queue in allNicks 
					if(allNicks.containsKey(destNick)==false) {
						write("Nick not found");
					}
					else {
						List<String> msgs = allNicks.get(nick);
						msgs.add(text.toString());
						allNicks.put(new NickPwd(nick, password), msgs);
						write("User offline, message sent");
					}
				}
				else { //send message from prosock -> destnick's sock
						Socket destSock = onlineNicks.get(destNick);
						System.out.println(Arrays.toString(text));
						destSock.getOutputStream().write((destNick + ": " + Arrays.toString(text) + "\r\n").getBytes());
						System.out.println("sending " + Arrays.toString(text).getBytes());
					}
			}

			else if(message.startsWith("NEW")==true){
				//tokens[1] = nick i want
				//WAHHHH
				if(tokens[1] != null) nick = tokens[1];
				if(tokens[2] != null) password = tokens[2];
				//THIS CAN@T HANDLE EMPTY NICK OR PASSWORD FIELDS
				if(nick!=null && password!= null) {
					if(allNicks.containsKey(nick)) {
						write("nick already exists");
					}
					else {
						List<String> msgs = new ArrayList<String>();
						allNicks.put(new NickPwd(nick, password), msgs);
						write("created new nick");
					}
				}
				else write("please provide a nick and password");
			}
			else if(message.startsWith("QUIT") == true) {
				//remove the nick from the hash table if actually signed in
				if(nick != null) {
					onlineNicks.remove(nick);
					write("Signing out " + nick);
					System.out.println("signing out " + nick);
				}
				else {
					write("Please sign in first");
				}
				//close the conn
				try {
					write("Connection closing");
					proSock.close();
					//NEED TO ALSO STOP THE SERVERTHREAD
				} catch (IOException close) {
					System.out.println("Failed to close the socket connection");
					System.out.println(close);
					System.exit(1);
				}
			}
	}
	
	public void write(String msg) throws IOException {
		if((proSock.isClosed()) == false) {
			this.proSock.getOutputStream().write((msg + "\r\n").getBytes());
		}
		else System.out.println("Error - trying to write to closed socket");

	}
	
	private class NickPwd {
		private String userNick;
		private String userPwd;
		
		NickPwd(String n, String p) {
			this.userNick = n;
			this.userPwd = userPwd;
		}
		
		public String getPassword() {
			return this.userPwd;
		}
		
		public String getNick() {
			return this.userNick;
		}
		
	}
	
	
}