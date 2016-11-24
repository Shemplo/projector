package me.shemplo.assembler;

import java.io.FileNotFoundException;
import java.util.ArrayList;

import me.shemplo.assembler.file.ConstantsReader;
import me.shemplo.assembler.file.PropertiesReader;

public class Assembler {

	private String                propertiesFileName;
	private PropertiesReader      propertiesReader;
	private ConstantsReader       constantsReader;
	private ArrayList <String []> assemblerInstructions;
	
	private int constsReaderLine = 0;
	
	private enum Sections {
		CONSTANTS,
		ASSEMBLE,
		APPLICATION,
		UNKNOWN
	}
	
	private Sections section;
	
	public Assembler () {
		this.propertiesReader      = new PropertiesReader ();
		this.assemblerInstructions = new ArrayList <> ();
		
		this.section = Sections.UNKNOWN;
	}
	
	public void setPropertiesFileName (String fileName) {
		try {
			this.propertiesFileName = fileName;
			propertiesReader.loadFile (propertiesFileName);
		} catch (FileNotFoundException fne) {
			System.out.println ("[Assembler] Exceprion: `" 
					+ fne.getMessage () 
					+ "` in method `setPropertiesFileName`");
			this.propertiesFileName = "";
		}
	}
	
	public void getInstructions () {
		String  line       = "";
		int     lineNumber = 0;
		
		reader:
		while ((line = propertiesReader.readLine ()) != null) {
			String [] commands = compressString (line);
			if (commands.length < 1)            { continue; }
			if (commands [0].charAt (0) == '#') { continue; }
			
			Sections tmp;
			if ((tmp = _fetchSection (commands [0])) != null) {
				String sect = commands [0].toLowerCase ();
				sect = sect.substring (1, sect.length () - 1);
				
				if (tmp == Sections.UNKNOWN) {
					System.out.println ("[ERROR] Unknown section `" + sect + "` ... PARSE FAILED");
					break reader;
				} else {
					System.out.println ("[PROCESS] Property `" + sect + "` section found");
					this.assemblerInstructions.add (new String [] {commands [0]});
				}
				
				if (commands.length != 1) {
					System.out.println ("[WARNING] Unexpected sybols `" + commands [1] +"` found "
											+ "near section command `" + commands [0] 
											+ "` in line: " + (lineNumber + 1)
											+ " ... ignore them");
				}
				
				this.section = tmp;
			} else {
				if (section == Sections.CONSTANTS) {
					String command = commands [0];
					
					if (command.charAt (0) == '.') {
						command = command.toLowerCase ();
						if (command.equals (".constants")) {
							if (constantsReader != null) {
								System.out.println ("[WARNING] Constants file was already set in"
														+ " line " + constsReaderLine + "."
														+ " Line: " + (lineNumber + 1));
							}
							
							if (commands.length >= 3) {
								if (commands.length > 3) {
									System.out.println ("[WARNING] Unexpected symbols `" + commands [3] + "` found "
											+ "near constants command `" + command 
											+ "` in line " + (lineNumber + 1) 
											+ " ... ignore them");
								}
								
								String operator = commands [1];
								String fileName = commands [2];
								
								if (operator.equals ("=")) {
									try {
										constantsReader = new ConstantsReader (fileName);
									} catch (FileNotFoundException fnfe) {
										System.out.println ("[ERROR] Given file path `" + fileName + "`"
												+ " is invalid in line " + (lineNumber + 1) + " ... ignore command");
									}
								} else {
									System.out.println ("[ERROR] Unexpected operator `" + operator 
											+ "` for command `" + command + "` in line " + (lineNumber + 1)
											+ " ... ignore command");
									System.out.println ("[FORMAT] This command format: .constants = [file name]");
								}
							} else {
								System.out.println ("[ERROR] Not enough argument for command `" + command
														+ "` in line " + (lineNumber + 1)
														+ " ... ignore command");
								System.out.println ("[FORMAT] This command format: .constants = [file name]");
								
								//Think about the break;
								//break reader;
							}
						}
					} else {
						this.assemblerInstructions.add (commands);
					}
				}
			}
			
			lineNumber ++;
		}
	}
	
	private Sections _fetchSection (String command) {
		if (command.toLowerCase ().equals (":constants:")) {
			return Sections.CONSTANTS;
		} else if (command.toLowerCase ().equals (":assemble:")) {
			return Sections.ASSEMBLE;
		} else if (command.toLowerCase ().equals (":application:")) {
			return Sections.APPLICATION;
		} else if (command.charAt (0) == command.charAt (command.length () - 1)
					&& command.charAt (0) == ':') {
			return Sections.UNKNOWN;
		}
		
		return null;
	}
	
	public static String [] compressString (String string) {
		ArrayList <String> coms = new ArrayList <> ();
		StringBuilder        sb = new StringBuilder ();
		boolean       spacePrev = false;
		
		for (int i = 0; i < string.length (); i ++) {
			char current = string.charAt (i);
			
			if (current == ' ') {
				if (!spacePrev) {
					coms.add (sb.toString ());
					sb.setLength (0);
					spacePrev = true;
				}
			} else if (!System.lineSeparator ().equals (current)) {
				sb.append (current);
				spacePrev = false;
			}
		}
		
		if (sb.length () > 0) {
			coms.add (sb.toString ());
		}
		
		return coms.toArray (new String [coms.size ()]);
	}
	
}
