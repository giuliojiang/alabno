package alabno.wserver;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class AutoMarkerWSServer extends WebSocketServer implements Runnable {
	
	private final int threadsAvailable = Runtime.getRuntime().availableProcessors();
	ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(threadsAvailable);

	public AutoMarkerWSServer(int listenPort) {
		super(new InetSocketAddress(listenPort));
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		System.out.println("New connection opened");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		System.out.println("Connection closed");
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		System.out.println("Received message: " + message);

		// Parse the JSON
		JsonParser parser = new JsonParser(message);

		if (!parser.isOk()) {
            System.out.println("Json message received from client is malformed"
                + ". Ignoring...");
			return;
		}

		String type = parser.getString("type");
		if (type == null) {
			System.out.println("type is not set in the json message");
			return;
		}
		
		switch(type)
		{
		case "login":
			handleLogin(parser, conn);
			break;
		case "new_assignment":
			handleNewAssignment(parser, conn);
			break;
		default:
			System.out.println("Unrecognized client message type " + type);
		}

	}

	private void handleNewAssignment(JsonParser parser, WebSocket conn) {
		String exerciseType = parser.getString("ex_type");
		String modelAnswerGitLink = parser.getString("model_git");
		JSONArray studentGitLinks = parser.getArray("students_git");
		
		AssignmentCreator newAssignmentProcessor = new AssignmentCreator(
				exerciseType, modelAnswerGitLink, studentGitLinks);
		
		executor.submit(newAssignmentProcessor);
	}

	private void handleLogin(JsonParser parser, WebSocket conn) {
		boolean success = true;
		
		// get username
		String username = parser.getString("username");
		if (username == null || username.isEmpty()) {
			success = false;
		}
				
		// get password (hash)
		String password = parser.getString("password");
		if (password == null || password.isEmpty()) {
			success = false;
		}
		
		// check login
		// TODO
		
		// if login successful
		if (success) {
			JSONObject success_msg = new JSONObject();
			success_msg.put("type", "login_success");
			success_msg.put("id", "" + username.hashCode());
			conn.send(success_msg.toJSONString());
			return;
		}
		
		// if login fails
		else {
			JSONObject failure_msg = new JSONObject();
			failure_msg.put("type", "login_fail");
			conn.send(failure_msg.toJSONString());
			return;
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		System.out.println("WebSocket server error message: ");
		ex.printStackTrace();
	}

}