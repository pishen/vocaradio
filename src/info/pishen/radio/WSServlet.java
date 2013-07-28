package info.pishen.radio;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@WebServlet(urlPatterns={"/s/ws"})
public class WSServlet extends WebSocketServlet {
	private static final long serialVersionUID = 1L;
	private static Logger log = LogManager.getLogger(WebSocketServlet.class);
	private Set<VocaMessageInbound> connections = new CopyOnWriteArraySet<VocaMessageInbound>();

	@Override
	protected StreamInbound createWebSocketInbound(String subProtocol,
			HttpServletRequest req) {
		return new VocaMessageInbound();
	}
	
	
	
	private class VocaMessageInbound extends MessageInbound{

		@Override
		protected void onOpen(WsOutbound outbound) {
			connections.add(this);
		}
		
		@Override
		protected void onClose(int status) {
			connections.remove(this);
		}

		@Override
		protected void onBinaryMessage(ByteBuffer message){}
		
		@Override
		protected void onTextMessage(CharBuffer message){}
	}
}
