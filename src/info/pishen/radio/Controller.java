package info.pishen.radio;

import info.pishen.radio.Playlist.EmptyMusicDirException;
import info.pishen.radio.Playlist.MusicDirReadingException;
import info.pishen.radio.Playlist.NoMusicDirException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/")
public class Controller extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Playlist playlist = new Playlist();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException{
		resp.setCharacterEncoding("UTF-8");
		try(PrintWriter out = resp.getWriter()){
			if(req.getServletPath().equals("/next") && req.getRemoteAddr().equals("127.0.0.1")){
				try {
					out.println(playlist.getNext());
				} catch (NoMusicDirException e) {
					out.println("music dir is not assigned.");
				} catch (MusicDirReadingException e) {
					out.println("error when reading music dir.");
				} catch (EmptyMusicDirException e) {
					out.println("music dir is empty.");
				}
			}else{
				resp.sendError(HttpServletResponse.SC_FORBIDDEN, "You've got to the wrong place.");
			}
		}
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setCharacterEncoding("UTF-8");
		try(PrintWriter out = resp.getWriter(); BufferedReader in = req.getReader()){
			if(req.getServletPath().equals("/music-dir") && req.getRemoteAddr().equals("127.0.0.1")){
				String line = in.readLine();
				if(line != null){
					playlist.setMusicDir(line);
				}
			}else{
				resp.sendError(HttpServletResponse.SC_FORBIDDEN, "You've got to the wrong place.");
			}
		}
	}

}
