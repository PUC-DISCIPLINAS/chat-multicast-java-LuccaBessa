import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

public class UDPServer {
	public static void main(String[] args) {
		String message;
		ArrayList<Room> rooms = new ArrayList<>();
		int port = 6789;

		try (DatagramSocket socket = new DatagramSocket(port)) {
			System.out.println("Server listening to port: UDP/"+ port +".");
			byte[] buffer = new byte[100000];

			while (true) {
				DatagramPacket request = new DatagramPacket(buffer, buffer.length);
				String answer = "";
				char messageId;

				socket.receive(request);
				message = new String(request.getData()).trim();

				System.out.println("Server: received -> " + message + ".");

				messageId = message.charAt(0);

				if(messageId == '1') {
					answer = "";

					if (rooms.isEmpty()) {
						answer = "No rooms";
					}else {
						for (Room room: rooms) {
							answer = answer.concat(room.getName()+"\n");
						}
					}
				} else if(messageId == '2') {
					String roomName = message.substring(1).split("---")[0];
					String userName = message.substring(1).split("---")[1];
					String wasAdded = "";
					answer = "No room found\n";

					if (rooms.isEmpty()) {
						answer = "No rooms\n";
					}else {
						for (Room room: rooms) {
							if(room.getName().compareTo(roomName) == 0) {
								wasAdded = room.addMember(userName);

								if (wasAdded.compareTo("") == 0) {
									answer = room.getIp();
								} else {
									answer = wasAdded;
								}
							}
						}
					}
				} else if(messageId == '3') {
					String roomName = message.substring(1).split("---")[0];
					String userName = message.substring(1).split("---")[1];

					if (rooms.isEmpty()) {
						answer = "224.0.0.0";
						rooms.add(new Room(answer, roomName, userName));
					}else {
						if(rooms.size() == 10) {
							answer = "Maximum chat room limit";
						}else {
							for (Room room: rooms) {
								if (room.getName().compareTo(roomName) == 0) {
									answer = "Room already created\n";
									break;
								}
							}

							if (answer.compareTo("Room already created\n") != 0) {
								answer = "224.0.0."+(rooms.size());
								rooms.add(new Room(answer, roomName, userName));
							}
						}
					}
				} else if(messageId == '4') {
					String roomName = message.substring(1).split("---")[0];
					answer = "No room found\n";

					if (rooms.isEmpty()) {
						answer = "No rooms\n";
					}else {
						for (Room room: rooms) {
							if(room.getName().compareTo(roomName) == 0) {
								answer = room.getMembers();
							}
						}
					}
				}else if(messageId == '5') {
					String roomName = message.substring(1).split("---")[0];
					String userName = message.substring(1).split("---")[1];
					System.out.println("Room: " + roomName + " User: " + userName);
					Room roomWithNoMembers = null;

					for (Room room: rooms) {
						if(room.getName().compareTo(roomName) == 0) {
							 room.removeMember(userName);
							 if(room.getMembers().compareTo("") == 0) {
							 	roomWithNoMembers = room;
							 }
						}
					}

					if(roomWithNoMembers != null) {
						rooms.remove(roomWithNoMembers);
					}
				}

				System.out.println("Sending: " + answer);
				DatagramPacket reply = new DatagramPacket(answer.getBytes(StandardCharsets.UTF_8), answer.length(), request.getAddress(), request.getPort());
				socket.send(reply);
			}
		} catch (SocketException e) {
			System.out.println("Socket: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("IO: " + e.getMessage());
		}
	}
}
