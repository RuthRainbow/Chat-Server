
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.net.Socket;
import java.io.IOException;

class Protocol {
	private String nick;
	private String destNick;
	private Socket proSock;
	private String delims = "[ ]+";
	private String[] text;
	private String password;

	//Storing current connections - nick and associated socket
	private static Hashtable<String, Socket> onlineNicks = new Hashtable<String, Socket>();
	//Storing all known nicks - nick, password and queue of messages waiting for user to come online (ie have an entry in the other hash table)
	private static Hashtable<String, List<String>> allNicks = new Hashtable<String, List<String>>();
	
	//constructor
	Protocol(Socket currSock) {
		nick = null;
		proSock = currSock;
		text = new String[300];
	}
	
	public String process(String message) {
	//false => error occurred, RETURN AN ERROR MESSAGE AND EXCEPTION
			System.out.println("processing " + message);
			if (message == null) {
				return "";
			}
			String[] tokens = null;
			tokens = message.split(delims);

			if(message.startsWith("USER") == true) { 
				//move nick and socket to onlineNicks, if they exist in allNicks
				if(nick != null) {
					return "Already signed in, please sign out first";
				}
				nick = tokens[1];
				if(allNicks.containsKey(nick)==false){
					return "You must first create a user account";
				}
				else {
					onlineNicks.put(nick, proSock);
					//send their list of offline messages
					List<String> msgs = allNicks.get(nick);
					System.out.println(msgs.toString());
					msgs.clear();
					allNicks.put(nick, msgs);
					return ("Hello "+nick);
				}
					
			}
			else if(message.startsWith("QUIT") == true) {
				//remove the nick from the hash table if actually signed in
				if(nick != null) {
					onlineNicks.remove(nick);
					System.out.println("signing out " + nick);
				}
				//close the conn
				try {
					proSock.close();
					return "Connection closed";
				} catch (IOException close) {
					System.out.println("Failed to close the socket connection");
					System.out.println(close);
					System.exit(1);
				}
			}
			else if(message.startsWith("MSG")== true) { //takes arg dest nick, msg {
				destNick = tokens[1];
				//fill the text array with the message to be sent
				if(tokens.length > 302) {
					return "message too long";
				}
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
						return "Nick not found";
					}
					else {
						List<String> msgs = allNicks.get(nick);
						msgs.add(text.toString());
						allNicks.put(nick, msgs);
						return "User offline, message sent";
					}
				}
				else { //send message from prosock -> destnick's sock
						Socket destSock = onlineNicks.get(destNick);
						System.out.println(text.toString());
						return ("text = " + text.toString());
					}
			}
			else if(message.startsWith("NOOP")==true){
				//respond to client
				return "Acknowledged";
			}
			else if(message.startsWith("WHO")==true){
				return "not yet implemented";
			}
			else if(message.startsWith("NEW")==true){
				//tokens[1] = nick i want
				if(tokens[1]!=null) {
					if(allNicks.containsKey(tokens[1])) {
						return "nick already exists";
					}
					List<String> msgs = new ArrayList<String>();
					allNicks.put(tokens[1], msgs);
					return "created new nick";
				}
				else return "please provide a nick";
			}
			return "error";
	}
	
	
}