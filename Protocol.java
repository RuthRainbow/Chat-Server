
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
	//PROBLEM: need to check if nick exists to send a message, don't want to have to know password
	//Storing all known nicks - nick, password and queue of messages waiting for user to come online (ie have an entry in the other hash table)
	private static Hashtable<NickPwd, List<String>> allNicks = new Hashtable<NickPwd, List<String>>();
	
	//constructor
	Protocol(Socket currSock) {
		nick = null;
		proSock = currSock;
		password = null;
	}
	
	public void process(String message) throws IOException {
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
				else {
					if(tokens.length == 3) {
						nick = tokens[1];
						password = tokens[2];
						NickPwd currnick = new NickPwd(nick, password);
						if((allNicks.containsKey(currnick))==false){
							//FAILED HERE
							write("Please enter a valid username and password");
						}
						else {
							//CHECK IF PASSWORD IS CORRECT
							onlineNicks.put(nick, proSock);
							//send their list of offline messages
							List<String> msgs = allNicks.get(nick);
							System.out.println(msgs.toString());
							msgs.clear();
							//update their msgs list to empty
							allNicks.put(currnick, msgs);
							write("Hello " + nick);
						}
					}
					else write("Usage: USER <nick> <password>");
				}
			}
			else if(message.startsWith("NOOP")==true){
				//respond to client
				write("Acknowledged");
			}
			else if(message.startsWith("WHO")==true){
				write("Online: " + onlineNicks.toString());
			}
			else if(message.startsWith("MSG")== true) { //takes arg dest nick, msg {
				destNick = tokens[1];
				//fill the text array with the message to be sent
				if(tokens.length > 302) {
					write("message too long");
				}
				else if((nick == null) || onlineNicks.containsKey(nick)==false) {
					write("Please sign in to send a message");
				}
				else if(tokens.length > 2){
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
						//PROBLEM: need to know their password to check if their nick exists!
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
							destSock.getOutputStream().write((nick + ": " + Arrays.toString(text) + "\r\n").getBytes());
							System.out.println("sending " + Arrays.toString(text).getBytes());
						}
				}
				else write("usage: MSG <nick> <message>");
			}

			else if(message.startsWith("NEW")==true){
				//tokens[1] = nick i want, tokens[2] = password
				if(tokens.length >= 3) {
					if(tokens[1]!=null && tokens[2]!= null) {
						if(allNicks.containsKey(tokens[1])) {
							write("nick already exists");
						}
						else {
							List<String> msgs = new ArrayList<String>();
							allNicks.put(new NickPwd(tokens[1], tokens[2]), msgs);
							write("created new nick");
						}
					}
				}
				else write("usage: NEW <nick> <password>");
			}
			else if(message.startsWith("QUIT") == true) {
				//remove the nick from the hash table if actually signed in
				if(nick != null) {
					onlineNicks.remove(nick);
					write("Signing out " + nick);
					System.out.println("signing out " + nick);
				}
				else {
					write("Not signed in anyway");
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
		public String userNick;
		public String userPwd;
		
		NickPwd(String n, String p) {
			this.userNick = n;
			this.userPwd = p;
		}
		
		public String getPassword() {
			return this.userPwd;
		}
		
		public String getNick() {
			return this.userNick;
		}
		
	}
	
	
}