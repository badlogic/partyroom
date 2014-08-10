package com.badlogicgames.partyroom.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;


/**
 * Generates XSPF Playlists recursively within a directory or based on directory
 * listings of an URL.
 * @author badlogic
 *
 */
public class PlaylistGenerator {
	public static class FileNameCleaner {
		final static int[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};
		static {
		    Arrays.sort(illegalChars);
		}
		public static String cleanFileName(String badFileName) {
		    StringBuilder cleanName = new StringBuilder();
		    for (int i = 0; i < badFileName.length(); i++) {
		        int c = (int)badFileName.charAt(i);
		        if (Arrays.binarySearch(illegalChars, c) < 0) {
		            cleanName.append((char)c);
		        }
		    }
		    return cleanName.toString();
		}
		}
	
	class FileDescriptor {
		String location;
		String album;
		String artist;
		String title;
		int duration;
		@Override
		public String toString () {
			return "FileDescriptor [location=" + location + ", album=" + album + ", artist=" + artist + ", title=" + title
				+ ", duration=" + duration + "]";
		}
	}
	
	class Playlist {
		String title;
		List<FileDescriptor> files = new ArrayList<FileDescriptor>();
	}
	
	private List<Playlist> listFilesInDirectory(String directory) {
		List<Playlist> result = new ArrayList<Playlist>();
		listFilesInDirectory(new File(directory), result);
		return result;
	}
	
	private void listFilesInDirectory(File directory, List<Playlist> playlists) {
		Playlist playlist = new Playlist();
		File[] list = directory.listFiles();		
		
		// gather all the files first, so we don't have to sort later on
		for(File file: list) {
			if(file.isFile()) {
				String name = file.getName().toLowerCase();
				if(name.endsWith("mp3") || name.endsWith("ogg")) {
					FileDescriptor desc = getDescriptor(file);
					if(desc != null) playlist.files.add(desc);
				}
			}
		}
		
		// add this as a playlist
		if(playlist.files.size() > 0) {
			FileDescriptor file = playlist.files.get(0);
			if(file.album != null && file.artist != null) {
				playlist.title = file.artist + " - " + file.album;
			} else {
				playlist.title = directory.getName();
			}
			playlists.add(playlist);
		}
		
		// run through the directories
		for(File file: list) {
			if(file.isDirectory()) {
				listFilesInDirectory(file, playlists);
			}
		}
	}	
	
	private FileDescriptor getDescriptor(File file) {
		try {
			AudioFile audioFile = AudioFileIO.read(file);
			AudioHeader header = audioFile.getAudioHeader();
			FileDescriptor desc = new FileDescriptor();
			desc.duration = header.getTrackLength();
			desc.location = file.getAbsolutePath();
			desc.album = audioFile.getTag().getFirst(FieldKey.ALBUM);
			desc.artist = audioFile.getTag().getFirst(FieldKey.ARTIST);
			desc.title = audioFile.getTag().getFirst(FieldKey.TITLE);
			return desc;
		} catch(Exception e) {
			return null;
		}
	}
	
	public void generate(String dirOrUrl, String output) throws IOException {
		File dir = new File(dirOrUrl);
		File out = new File(output);
		if(!dir.exists()) {
			System.out.println("Directory " + dir.getAbsolutePath() + " does not exist");
			return;
		}
		if(!dir.isDirectory()) {
			System.out.println(dir.getAbsolutePath() + " is not a directory");
			return;
		}		
		if(!out.exists()) {
			out.mkdirs();
		} else if(out.isFile()) {
			System.out.println(out.getAbsolutePath() + " is not a directory");
		}
		
		List<Playlist> playlists = listFilesInDirectory(dirOrUrl);
		for(Playlist playlist: playlists) {
			writePlaylist(playlist, new File(output, FileNameCleaner.cleanFileName(playlist.title + ".xspf")));
		}
		
		Playlist allPlaylist = new Playlist();
		allPlaylist.title = "All playlists in one!";
		for(Playlist playlist: playlists) {
			allPlaylist.files.addAll(playlist.files);
		}
		writePlaylist(allPlaylist, new File(output, FileNameCleaner.cleanFileName(allPlaylist.title + ".xspf")));
	}
	
	private void writePlaylist (Playlist playlist, File outputFile) throws IOException {		
		Writer writer = new FileWriter(outputFile);
		writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		writer.write("<playlist version=\"1\" xmlns=\"http://xspf.org/ns/0/\">\n");
		writer.write("   <title>" + playlist.title + "</title>\n");
		writer.write("   <trackList>\n");
		
		File outputDir = outputFile.getParentFile();
		for(FileDescriptor file: playlist.files) {
			String location = outputDir.toURI().relativize(new File(file.location).toURI()).getPath();
			writer.write("      <track>\n");
			writer.write("         <creator>" + StringEscapeUtils.escapeXml11(file.artist) + "</creator>\n");
			writer.write("         <album>" + StringEscapeUtils.escapeXml11(file.album) + "</album>\n");
			writer.write("         <title>" + StringEscapeUtils.escapeXml11(file.title) + "</title>\n");
			writer.write("         <duration>" + StringEscapeUtils.escapeXml11("" + file.duration * 1000) + "</duration>\n");
			writer.write("         <location>" + StringEscapeUtils.escapeXml11(location) + "</location>\n");
			writer.write("      </track>\n");
		}
		
		writer.write("   </trackList>\n");
		writer.write("</playlist>\n");
		writer.close();
	}

	public static void main (String[] args) throws IOException {
		if(args.length != 2) {
			printHelp();
			System.exit(-1);
		}			
		new PlaylistGenerator().generate(args[0], args[1]);
	}

	private static void printHelp () {
		System.out.println("Usage: java -cp partyroom.jar com.badlogicgames.partyroom.utils.PlaylistGenerator <directory-or-url> <output-dir>");
	}
}
