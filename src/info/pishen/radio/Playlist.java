package info.pishen.radio;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Let ezstream get the next song on playlist
 */
@WebServlet("/next")
public class Playlist extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private ArrayList<Path> songQueue = new ArrayList<Path>();

	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		if(request.getRemoteAddr().equals("127.0.0.1") == false){
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "You got to the wrong place.");
			return;
		}
		
		ArrayList<Path> allSongs = new ArrayList<Path>();
		try(DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("/home/pishen/radio/VOCALOID-mp3"))){
			for(Path path: stream){
				allSongs.add(path);
			}
		}
		
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.println(allSongs.get((int)(Math.random() * allSongs.size())));
		out.close();
	}
}
