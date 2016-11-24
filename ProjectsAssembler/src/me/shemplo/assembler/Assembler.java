package me.shemplo.assembler;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.shemplo.assembler.file.ConstantsReader;
import me.shemplo.assembler.file.PropertiesReader;
import me.shemplo.assembler.structs.Pair;

public class Assembler {

	private String                propertiesFileName;
	private PropertiesReader      propertiesReader;
	private ConstantsReader       constantsReader;
	private ArrayList <Pair <String [], Integer>> constantsInstructions;
	private ArrayList <Pair <String [], Integer>> assemblerInstructions;
	private ArrayList <Pair <String [], Integer>> applicationInstructions;
	private HashMap   <String, Types>             variables;
	private HashMap   <String, String>            properties;
	
	private boolean showDebug = false;
	
	private boolean getPropertiesFileStatus = false;
	private boolean readInstructionsStatus  = false;
	private boolean runInstructionsStatus   = false;
	
	private int constsReaderLine = 0;
	
	private enum Sections {
		CONSTANTS,
		ASSEMBLE,
		APPLICATION,
		UNKNOWN
	}
	
	private enum Types {
		MANIFEST,
		PACKAGE,
		FILE,
		UNKNOWN
	}
	
	private Sections section;
	
	public Assembler () {
		this.propertiesReader        = new PropertiesReader ();
		this.constantsInstructions   = new ArrayList <> ();
		this.assemblerInstructions   = new ArrayList <> ();
		this.applicationInstructions = new ArrayList <> ();
		this.variables               = new HashMap <> ();
		this.properties              = new HashMap <> ();
		
		this.section = Sections.UNKNOWN;
	}
	
	public void setPropertiesFileName (String fileName) {
		getPropertiesFileStatus = true;
		
		try {
			this.propertiesFileName = fileName;
			propertiesReader.loadFile (propertiesFileName);
		} catch (FileNotFoundException fne) {
			System.out.println ("[Assembler] Exceprion: `" 
					+ fne.getMessage () 
					+ "` in method `setPropertiesFileName`");
			getPropertiesFileStatus = false;
			this.propertiesFileName = "";
		}
	}
	
	public void getInstructions () {
		String  line       = "";
		int     lineNumber = 0;
		
		reader:
		while ((line = propertiesReader.readLine ()) != null) {
			String [] commands = compressString (line);
			if (commands.length < 1)            { lineNumber ++; continue; }
			if (commands [0].charAt (0) == '#') { lineNumber ++; continue; }
			
			Sections tmp;
			if ((tmp = _fetchSection (commands [0])) != null) {
				String sect = commands [0].toLowerCase ();
				sect = sect.substring (1, sect.length () - 1);
				
				if (tmp == Sections.UNKNOWN) {
					System.out.println ("[ERROR] Unknown section `" + sect + "` ... PARSE FAILED");
					break reader;
				} else {
					System.out.println ("[PROCESS] Section `" + sect + "` found");
					//this.constantsInstructions.add (new String [] {commands [0]});
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
										constsReaderLine = lineNumber + 1;
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
						this.constantsInstructions.add (new Pair <String [], Integer> (commands, lineNumber + 1));
					}
				} else if (section == Sections.ASSEMBLE) {
					String command = commands [0];
					
					if (command.toLowerCase ().equals ("debug")) {
						if (commands.length == 1) {
							System.out.println ("[ERROR] Not enough argument for command `" + command
													+ "` in line " + (lineNumber + 1)
													+ " ... ignore command");
							System.out.println ("[FORMAT] This command format: debug [show|hide]");
						} else if (commands.length == 2) {
							String action = commands [1];
							
							if (action.toLowerCase ().equals ("show")) {
								this.showDebug = true;
							} else if (action.toLowerCase ().equals ("hide")) {
								this.showDebug = false;
							} else {
								System.out.println ("[ERROR] Unknown action `" + action 
														+ "` in line " + (lineNumber + 1) 
														+ " ... ignore it");
							}
						} else {
							// SUCH COMMANDS ARE NOT AVAILABLE NOW //
							
							System.out.println ("[WARNING] Unexpected symbols `" + commands [2] + "` found "
													+ "near constants command `" + command 
													+ "` in line " + (lineNumber + 1) 
													+ " ... ignore them");
						}
						
						lineNumber ++;
						continue;
					} else if (command.toLowerCase ().equals ("create")) {
						if (!_registerVariable (commands, lineNumber + 1)) {
							break reader;
						}
						
						lineNumber ++;
						continue;
					}
					
					this.assemblerInstructions.add (new Pair <String [], Integer> (commands, lineNumber + 1));
				} else if (section == Sections.APPLICATION) {
					String command = commands [0];
					
					if (command.charAt (0) == '.') {
						_registerProperty (commands, lineNumber + 1);
					} else {
						this.applicationInstructions.add (new Pair <String [], Integer> (commands, lineNumber + 1));
					}
				}
			}
			
			lineNumber ++;
		}
		
		if (constantsReader == null) {
			constantsReader = new ConstantsReader ();
		}
		
		for (int i = 0; i < constantsInstructions.size (); i ++) {
			String [] commands   = constantsInstructions.get (i).f;
			          lineNumber = constantsInstructions.get (i).s;
			
			if (commands [0].charAt (0) != '.') {
				constantsReader.registerConstant (commands, lineNumber);
			} else {
				// SUCH COMMANDS ARE NOT AVAILABLE NOW //
			}
		}
		
		if (showDebug) {
			System.out.println ("[DEBUG] Constants gegistered...");
		}
		
		readInstructionsStatus = true;
	}
	
	public void assemble () {
		
		
		runInstructionsStatus = true;
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
	
	private Types _fetchType (String command) {
		if (command.toLowerCase ().equals ("<manifest>")) {
			return Types.MANIFEST;
		} else if (command.toLowerCase ().equals ("<package>")) {
			return Types.PACKAGE;
		} else if (command.toLowerCase ().equals ("<file>")) {
			return Types.FILE;
		} else if (command.charAt (0) == '<' && command.charAt (command.length () - 1) == '>') {
			return Types.UNKNOWN;
		}
		
		return null;
	}
	
	private boolean _registerVariable (String [] commands, int line) {
		boolean registered = false;
		
		if (commands.length >= 3) {
			if (commands.length > 3) {
				System.out.println ("[WARNING] Unexpected symbols `" + commands [3] + "` found "
										+ "near command `" + commands [0] + "` in line " + line 
										+ " ... ignore them");
			}
			
			Types type = _fetchType (commands [1]);
			if (type == null) {
				System.out.println ("[ERROR] Type expected, given `" + commands [1]
										+ "` in line " + line + " ... PARSE FAILED");
				System.out.println ("[FROMAT] This command format: cteate [type] [id name]");
			} else if (type == Types.UNKNOWN) {
				System.out.println ("[ERROR] Unknown type given `" + commands [1]
										+ "` in line " + line + " ... PARSE FAILED");
				System.out.println ("[FROMAT] This command format: cteate [type] [id name]");
			} else {
				String variable = commands [2];
				
				if (variable.length () > 0) {
					String mask = "^([a-zA-Z0-9\\_]+)$";
					Pattern pat = Pattern.compile (mask);
					
					Matcher matcher = pat.matcher (variable.trim ());
					if (matcher.matches ()) {
						variable = variable.toLowerCase ();
						
						if (variables.containsKey (variable)) {
							System.out.println ("[WARNING] Variable with name `" + variable
													+ "` is already created. Line: " + line);
						}
						
						variables.put (variable, type);
						registered = true;
					} else {
						System.out.println ("[ERROR] Invalid character in name `" + variable
												+ "` in line " + line + " ... ignore it");
					}
				} else {
					System.out.println ("[ERROR] Too short name of variable `" + variable 
											+ "` in line " + line + " ... PARSE FAILED");
				}
			}
		} else {
			System.out.println ("[ERROR] Not enough argument for creating variable "
					+ "in line " + line + " ... ignore command");
			System.out.println ("[FORMAT] This command format: cteate [type] [id name]");
		}
		
		return registered;
	}
	
	private boolean _registerProperty (String [] commands, int line) {
		boolean registered = false;
		
		if (commands.length >= 3) {
			if (commands.length > 3) {
				System.out.println ("[WARNING] Unexpected symbols `" + commands [3] + "` found "
										+ "near command `" + commands [0] + "` in line " + line 
										+ " ... ignore them");
			}
			
			String  property = commands [0];
			boolean propertyFlag = false;
			
			if (property.length () > 1) {
				if (property.charAt (0) == '.') {
					String name = property.substring (1).toLowerCase ();
					
					String mask = "^([a-zA-Z0-9\\_]+)$";
					Pattern pat = Pattern.compile (mask);
					
					Matcher matcher = pat.matcher (name.trim ());
					if (matcher.matches ()) {
						if (properties.containsKey (name)) {
							System.out.println ("[WARNING] Property with name `" + name
													+ "` is already registered. Line: " + line);
						}
						
						propertyFlag = true;
						property     = name;
					} else {
						System.out.println ("[ERROR] Invalid character in name `" + property
												+ "` in line " + line + " ... ignore it");
					}
				} else {
					System.out.println ("[WARNING] Declaration of property "
											+ "should start with `.` in line " + line);
				}
			} else {
				System.out.println ("[ERROR] Too short declaration of property `" 
										+ property + "` in line " + line + " ... ignore it");
			}
			
			if (!propertyFlag) {
				return false;
			}
			
			String operator = commands [1];
			boolean operatorFlag = false;
			
			if (operator.equals ("=")) {
				operatorFlag = true;
			} else {
				System.out.println ("[ERROR] Unexpected operator `" + operator 
						+ "` for registering property in line " + line
						+ " ... ignore it");
				System.out.println ("[FORMAT] This command format: .[property name] = [value]");
			}
			
			if (!operatorFlag) {
				return false;
			}
			
			String value = commands [2];
			boolean valueFlag = false;
			
			if (value.length () > 0) {
				String mask = "^([a-zA-Z0-9\\-\\$\\.\\,\\:\\!\\_\\/]+)$";
				Pattern pat = Pattern.compile (mask);
				
				Matcher matcher = pat.matcher (value.trim ());
				if (matcher.matches ()) {
					valueFlag = true;
				} else {
					System.out.println ("[ERROR] Invalid character in value `" + value
											+ "` in line " + line + " ... ignore it");
				}
			} else {
				System.out.println ("[ERROR] Property can't have empty value "
										+ "in line " + line + " ... ignore it");
			}
			
			if (!valueFlag) {
				return false;
			}
			
			String finalValue = constantsReader.putConstants (value, line);
			if (finalValue != null) { properties.put (property, finalValue); }
		} else {
			System.out.println ("[ERROR] Not enough argument for registering property "
					+ "in line " + line + " ... ignore command");
			System.out.println ("[FORMAT] This command format: .[property name] = [value]");
		}
		
		return registered;
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
	
	/**
	 * getPropertiesFileStatus <br />
	 * readInstructionsStatus
	 * */
	public boolean getStatus (String status) {
		if (status.equals ("getPropertiesFileStatus")) {
			return getPropertiesFileStatus;
		} else if (status.equals ("readInstructionsStatus")) {
			return readInstructionsStatus;
		} else if (status.equals ("runInstructionsStatus")) {
			return runInstructionsStatus;
		}
		
		System.out.println ("[DEBUG] Status `" + status + "` not found");
		return false;
	}
	
}
