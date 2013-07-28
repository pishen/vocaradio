package info.pishen.radio;

import info.pishen.radio.Playlist.EmptyMusicDirException;
import info.pishen.radio.Playlist.MusicDirReadingException;
import info.pishen.radio.Playlist.NoMusicDirException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

@WebServlet("/s/*")
public class Controller extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Logger log = LogManager.getLogger(Controller.class);
	private Playlist playlist = new Playlist();

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
				try {
					out.println(playlist.getNext());
				} catch (NoMusicDirException e) {
					out.println("music dir is not assigned.");
				} catch (MusicDirReadingException e) {
					out.println("error when reading music dir.");
				} catch (EmptyMusicDirException e) {
					out.println("music dir is empty.");
				}
			}else if(req.getPathInfo().equals("/status")){
				resp.setContentType("application/json");
				out.print(getStatus());
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
				JSONObject jsonObject = new JSONObject(verifyResult);
				//TODO
			}else{
				resp.sendError(HttpServletResponse.SC_NOT_FOUND);
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
