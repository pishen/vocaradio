package info.pishen.radio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.websocket.MessageInbound;
import org.apache.catalina.websocket.StreamInbound;
import org.apache.catalina.websocket.WebSocketServlet;
import org.apache.catalina.websocket.WsOutbound;
import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
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
				out.print(getStatus());
			}else if(req.getPathInfo().equals("/ws")){
				super.doGet(req, resp);
			}else if(req.getPathInfo().equals("/user-info")){
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
		try(PrintWriter out = resp.getWriter(); BufferedReader in = req.getReader()){
			if(req.getPathInfo().equals("/login")){
				StringBuffer assertion = new StringBuffer();
				String line = null;
				while((line = in.readLine()) != null){
					assertion.append(line);
				}
				String verifyResult = Request.Post("https://verifier.login.persona.org/verify")
					.bodyForm(Form.form()
							.add("assertion", assertion.toString())
							.add("audience", "http://dg.pishen.info").build())
							.execute().returnContent().asString();
				JSONObject verifyResultJSON = new JSONObject(verifyResult);
				if(verifyResultJSON.getString("status").equals("okay")){
					req.getSession(true).setAttribute(EMAIL, verifyResultJSON.getString("email"));
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

}
