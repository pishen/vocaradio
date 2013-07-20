package info.pishen.radio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.ArrayList;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@WebServlet(urlPatterns={"/chat"})
public class ChatServlet extends WebSocketServlet {
	private static final long serialVersionUID = 1L;
	private static Logger log = LogManager.getLogger(WebSocketServlet.class);
	private ArrayList<ClientMessageInbound> clientList = new ArrayList<ClientMessageInbound>();

	@Override
	protected StreamInbound createWebSocketInbound(String subProtocol,
			HttpServletRequest req) {
		return new ClientMessageInbound("client" + req.getSession().getId());
	}
	
	private class ClientMessageInbound extends MessageInbound{
		private WsOutbound outbound;
		private String clientName;
		
		public ClientMessageInbound(String clientName){
			this.clientName = clientName;
		}

		@Override
		protected void onOpen(WsOutbound outbound) {
			this.outbound = outbound;
			synchronized(clientList){
				clientList.add(this);
			}
			/*try {
				outbound.writeTextMessage(CharBuffer.wrap("Hello!"));
			} catch (IOException e) {
				// TODO fail sending message, log the error
			}*/
		}
		
		@Override
		protected void onClose(int status) {
			synchronized(clientList){
				clientList.remove(this);
			}
		}

		@Override
		protected void onBinaryMessage(ByteBuffer message){}
		
		@Override
		protected void onTextMessage(CharBuffer message){
			synchronized(clientList){
				for(ClientMessageInbound client: clientList){
					try {
						client.outbound.writeTextMessage(CharBuffer.wrap(clientName + ": " + message.toString()));
						client.outbound.flush();
					} catch (IOException e) {
						log.error("writing text", e);
					}
				}
			}
		}
	}
}
