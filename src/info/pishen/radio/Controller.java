package info.pishen.radio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeoutException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

@WebServlet("/s/*")
public class Controller extends WebSocketServlet {
	private static final long serialVersionUID = 1L;
	private static Logger log = LogManager.getLogger(Controller.class);
	private Playlist playlist = new Playlist();
	private Set<VocaMessageInbound> connections = new CopyOnWriteArraySet<VocaMessageInbound>();
	private static final String EMAIL = "email";

	@Override
	public void init() throws ServletException {
		super.init();
		Path musicDir = Paths.get(System.getenv("TOMCAT_HOME"), "vocaradio-music-dir");
		playlist.setMusicDir(musicDir.toString());
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException{
		resp.setCharacterEncoding("UTF-8");
		try(PrintWriter out = resp.getWriter()){
			if(req.getPathInfo().equals("/next") && isFromLocal(req)){
				out.print(playlist.getReturnMessage());
			}else if(req.getPathInfo().equals("/status")){
				resp.setContentType("application/json");
				out.print(getStatus());
			}else if(req.getPathInfo().equals("/ws")){
				super.doGet(req, resp);
			}else if(req.getPathInfo().equals("/user-info")){
				resp.setContentType("application/json");
				out.print(getUserInfo(req));
			}else if(req.getPathInfo().equals("/logout")){
				req.getSession().invalidate();
			}else{
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setCharacterEncoding("UTF-8");
		try(PrintWriter out = resp.getWriter(); BufferedReader postReader = req.getReader()){
			if(req.getPathInfo().equals("/login")){
				String assertion = dumpReaderToString(postReader, 0);
				
				Process verify = new ProcessBuilder("curl", "-d", 
						"assertion=" + assertion.toString() + "&audience=http://dg.pishen.info", 
						"https://verifier.login.persona.org/verify").start();
				
				try(BufferedReader verifyReader = new BufferedReader(new InputStreamReader(verify.getInputStream()))) {
					ProcessMonitor.waitProcessWithTimeout(verify, 10000);
					String verifyResult = dumpReaderToString(verifyReader, 0);
					
					JSONObject verifyJSON = new JSONObject(verifyResult);
					if(verifyJSON.getString("status").equals("okay")){
						req.getSession().setAttribute(EMAIL, verifyJSON.getString("email"));
						out.print(getUserInfo(req));
					}else{
						log.error("status not okay: " + verifyResult);
						resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
					}
				} catch (TimeoutException e) {
					verify.destroy();
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
				}
			}else if(req.getPathInfo().equals("/new-chat")){
				String email = (String)req.getSession().getAttribute(EMAIL);
				if(email != null){
					String chat = dumpReaderToString(postReader, 1000);
					JSONObject bcastJson = new JSONObject().put("type", "chat").put("name", email.split("@")[0]).put("content", chat);
					broadcast(bcastJson);
				}
			}else{
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			}
		}
	}
	
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
	
	private void broadcast(JSONObject jsonObj){
		for(VocaMessageInbound connection: connections){
			CharBuffer buffer = CharBuffer.wrap(jsonObj.toString());
			try {
				connection.getWsOutbound().writeTextMessage(buffer);
			} catch (IOException e) {
				log.error("ws send fail", e);
			}
		}
	}
	
	private boolean isFromLocal(HttpServletRequest req){
		String ip = req.getHeader("X-Forwarded-For");
		if(ip != null && ip.equals("127.0.0.1")){
			return true;
		}else{
			return false;
		}
	}
	
	private String getUserInfo(HttpServletRequest req){
		HttpSession session = req.getSession();
		JSONObject jsonObject = new JSONObject();
		if(session.getAttribute(EMAIL) == null){
			jsonObject.put(EMAIL, JSONObject.NULL);
		}else{
			jsonObject.put(EMAIL, session.getAttribute(EMAIL));
		}
		return jsonObject.toString();
	}
	
	private String getStatus(){
		try {
			Document doc = Jsoup.connect("http://localhost:8000/").get();
			Elements elements = doc.select("td.streamdata");
			if(elements.size() >= 6){
				String title = playlist.getCurrentMusicTitle();
				String numOfListeners = elements.get(5).text();
				return new JSONObject().put("onAir", true).put("title", title).put("num", numOfListeners).toString();
			}else{
				return new JSONObject().put("onAir", false).toString();
			}
		} catch (IOException e) {
			log.error("Icecast status checking error", e);
			return null;
		}
	}
	
	private String dumpReaderToString(BufferedReader in, int sizeLimit) throws IOException{
		StringBuffer strBuffer = new StringBuffer();
		String line = null;
		while((line = in.readLine()) != null){ 
			strBuffer.append(line);
			if(sizeLimit > 0 && strBuffer.length() > sizeLimit){break;}
		}
		return strBuffer.toString();
	}

}
