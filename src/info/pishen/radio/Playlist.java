package info.pishen.radio;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Playlist {
	private Path musicDir;
	private ArrayList<Path> musicQueue = new ArrayList<Path>();
	private String currentMusicTitle;
	
	public String getReturnMessage(){
		String returnMessage = null;
		try {
			returnMessage = getNext();
		} catch (NoMusicDirException e) {
			returnMessage = "music dir is not assigned.";
		} catch (MusicDirReadingException e) {
			returnMessage = "error when reading music dir.";
		} catch (EmptyMusicDirException e) {
			returnMessage = "music dir is empty.";
		}
		return returnMessage;
	}
	
	private String getNext() throws NoMusicDirException, MusicDirReadingException, EmptyMusicDirException{
		if(musicDir == null){
			throw new NoMusicDirException();
		}
		
		if(musicQueue.isEmpty()){
			ArrayList<Path> allMusic = new ArrayList<Path>();
			try(DirectoryStream<Path> stream = Files.newDirectoryStream(musicDir)){
				for(Path path: stream){
					allMusic.add(path);
				}
			} catch (IOException e) {
				throw new MusicDirReadingException();
			}
			
			if(allMusic.isEmpty()){
				throw new EmptyMusicDirException();
			}
			
			Path nextMusicPath = allMusic.get((int)(Math.random() * allMusic.size()));
			setCurrentMusicTitle(nextMusicPath.getFileName().toString().replaceAll("\\.\\w{3}", ""));
			
			return nextMusicPath.toString();
		}else{
			//TODO get music from queue
			return null;
		}
	}
	
	private synchronized void setCurrentMusicTitle(String value){
		currentMusicTitle = value;
	}
	
	public synchronized String getCurrentMusicTitle(){
		return currentMusicTitle;
	}
	
	public void setMusicDir(String pathName){
		musicDir = Paths.get(pathName);
	}
	
	public class NoMusicDirException extends Exception{
		private static final long serialVersionUID = 1L;
	}
	
	public class MusicDirReadingException extends Exception{
		private static final long serialVersionUID = 1L;
	}
	
	public class EmptyMusicDirException extends Exception{
		private static final long serialVersionUID = 1L;
	}
}
