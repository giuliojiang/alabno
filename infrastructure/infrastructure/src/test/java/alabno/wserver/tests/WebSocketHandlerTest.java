package alabno.wserver.tests;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;
import java.util.concurrent.ExecutorService;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.java_websocket.WebSocket;
import org.junit.Test;

import alabno.msfeedback.FeedbackUpdaters;
import alabno.wserver.AssignmentCreator;
import alabno.wserver.WebSocketHandler;

public class WebSocketHandlerTest {

    // Mockeries
    WebSocket mockWebSocketConnection = mock(WebSocket.class);
    ExecutorService mockExecutorService = mock(ExecutorService.class);

    WebSocketHandler handler = new WebSocketHandler(mockExecutorService, new FeedbackUpdaters());

    @Test
    public void handleMessageEmpty() {

        replay(mockWebSocketConnection);
        replay(mockExecutorService);

        handler.handleMessage(mockWebSocketConnection, "blah");

        verify(mockWebSocketConnection);
        verify(mockExecutorService);
    }

    @Test
    public void handleLoginTest() {

        Capture<String> captured_string = EasyMock.<String>newCapture();

        mockWebSocketConnection.send(EasyMock.capture(captured_string));
        mockWebSocketConnection.send((String) anyObject());

        replay(mockWebSocketConnection);
        replay(mockExecutorService);

        handler.handleMessage(mockWebSocketConnection,
                "    {\r\n        \"type\": \"login\",\r\n        \"username\": \"gj414\",\r\n        \"password\": \"9c4b8a984db84c98b49fa849a8\"\r\n    }");

        verify(mockWebSocketConnection);
        verify(mockExecutorService);

        assertTrue(captured_string.getValue().contains("login_success"));

    }

    @Test
    public void handleNewAssignmentTest() {

        // login
        mockWebSocketConnection.send((String) anyObject()); // receive login
                                                            // success and token
        mockWebSocketConnection.send((String) anyObject()); // receive jobs list
        // submit job
        expect(mockExecutorService.submit(isA(AssignmentCreator.class))).andReturn(null); // job
                                                                                          // is
                                                                                          // submitted
        Capture<String> captured_string = EasyMock.<String>newCapture();
        mockWebSocketConnection.send(EasyMock.capture(captured_string)); // success

        replay(mockExecutorService);
        replay(mockWebSocketConnection);

        handler.handleMessage(mockWebSocketConnection,
                "    {\r\n        \"type\": \"login\",\r\n        \"username\": \"gj414\",\r\n        \"password\": \"9c4b8a984db84c98b49fa849a8\"\r\n    }");

        handler.handleMessage(mockWebSocketConnection,
                "    {\r\n        \"type\": \"new_assignment\",\r\n        \"id\": \"gj414-98332052\",\r\n        \"title\": \"PINTOS\",\r\n        \"ex_type\": \"C\",\r\n        \"model_git\": \"https://gitlab.doc.ic.ac.uk/ajf/autumn/haskell_quadratic.git\",\r\n        \"students_git\": [\r\n            \"https://gitlab.doc.ic.ac.uk/autumn_16_17/haskell_quadratic_gj414.git\",\r\n            \"https://gitlab.doc.ic.ac.uk/autumn_16_17/haskell_quadratic_ap2314.git\"\r\n        ]\r\n    }");

        verify(mockWebSocketConnection);
        verify(mockExecutorService);

        assertTrue(captured_string.getValue().contains("job_sent"));
    }
}
