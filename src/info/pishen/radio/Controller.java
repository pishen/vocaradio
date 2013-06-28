package info.pishen.radio;

import info.pishen.radio.Playlist.EmptyMusicDirException;
import info.pishen.radio.Playlist.MusicDirReadingException;
import info.pishen.radio.Playlist.NoMusicDirException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

@WebServlet(urlPatterns={"/servlets/*"})
public class Controller extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Playlist playlist = new Playlist();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException{
		resp.setCharacterEncoding("UTF-8");
		try(PrintWriter out = resp.getWriter()){
			if(req.getPathInfo().equals("/next") && req.getRemoteAddr().equals("127.0.0.1")){
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
			}else if(req.getPathInfo().equals("/")){
				req.getRequestDispatcher("/index.jsp").forward(req, resp);
			}else{
				resp.sendError(HttpServletResponse.SC_FORBIDDEN, "PathInfo: " + req.getPathInfo());
			}
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setCharacterEncoding("UTF-8");
		try(PrintWriter out = resp.getWriter(); BufferedReader in = req.getReader()){
			if(req.getPathInfo().equals("/music-dir") && req.getRemoteAddr().equals("127.0.0.1")){
				String line = in.readLine();
				if(line != null){
					playlist.setMusicDir(line);
				}
				out.println("music dir '" + line + "' has been set.");
			}else{
				resp.sendError(HttpServletResponse.SC_FORBIDDEN, "You've got to the wrong place.");
			}
		}
	}
	
	private String getStatus(){
		try {
			Document doc = Jsoup.connect("http://localhost:8000/").get();
			Elements elements = doc.select("td.streamdata");
			if(elements.size() >= 6){
				String title = playlist.getCurrentMusicTitle();
				String numOfListeners = elements.get(5).text();
				return "{\"onAir\":\"true\",\"title\":\"" + title + "\",\"num\":\"" + numOfListeners + "\"}";
			}else{
				return "{\"onAir\":\"false\"}";
			}
		} catch (IOException e) {
			//TODO log error
			return null;
		}
	}

}
