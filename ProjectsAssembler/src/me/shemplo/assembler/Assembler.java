package me.shemplo.assembler;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.shemplo.assembler.file.ConstantsReader;
import me.shemplo.assembler.file.PropertiesReader;
import me.shemplo.assembler.structs.PackageTree;
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
	private boolean buildingSandboxStatus   = false;
	
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
		boolean wasStopped = false;
		
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
					wasStopped = true;
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
							System.out.println ("[ERROR] Not enough arguments for command `" + command
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
							wasStopped = true;
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
		
		if (showDebug) { System.out.println ("[DEBUG] Constants gegistered..."); }
		if (showDebug) { System.out.println ("[DEBUG] Properies read..."); }
		
		if (!wasStopped) {
			readInstructionsStatus = true;
		}
	}
	
	private HashMap <String, PackageTree>              pathes;
	private HashMap <String, HashMap <String, String>> manifests;
	
	public void assemble () {
		if (showDebug) { System.out.println ("[DEBUG] Starting assembly..."); }
		
		boolean wasStopped = false;
		pathes = new HashMap <> ();
		
		assembler:
		for (int i = 0; i < assemblerInstructions.size (); i ++) {
			String [] commands = assemblerInstructions.get (i).f;
			int       line     = assemblerInstructions.get (i).s;
			
			String variable = commands [0].toLowerCase ();
			if (variables.containsKey (variable)) {
				Types type = variables.get (variable);
				
				if (type == Types.PACKAGE) {
					if (!pathes.containsKey (variable)) {
						pathes.put (variable, new PackageTree ());
					}
					
					if (commands.length >= 2) {
						String action = commands [1];
						
						if (action.equals ("+") || action.equals ("import")) {
							if (commands.length >= 3) {
								Types itype  = Types.UNKNOWN;
								String value = "";
								String to    = "";
								
								if (commands.length >= 3) {
									String string  = commands [2];
									
									boolean typeGiven = false;
									Types tempType = _fetchType (string);
									
									if (tempType != null) {
										itype = tempType;
										typeGiven = true;
									} else {
										value = constantsReader.putConstants (string, line);
										value = value.toLowerCase ();
										
										if (!PackageTree.checkPath (value)) {
											System.out.println ("[ERROR] Invalid source path `" + value 
																	+ "` given in line " 
																	+ line + " ... ADDING FAILED");
											continue assembler;
										}
										
										itype = _fetchTypeFromPath (value);
									}
									
									if (typeGiven && commands.length >= 4) {
										value = constantsReader.putConstants (commands [3], line);
										value = value.toLowerCase ();
										
										if (!PackageTree.checkPath (value)) {
											System.out.println ("[ERROR] Invalid source path `" + value 
																	+ "` given in line " 
																	+ line + " ... ADDING FAILED");
											continue assembler;
										}
										
										//itype = _fetchTypeFromPath (value);
									} else if (typeGiven && commands.length < 4) {
										System.out.println ("[ERROR] Not enough arguments for command `" + action
												+ "` in line " + line + " ... ignore command");
										System.out.println ("[FORMAT] This command format: "
																+ "[id name] [action] <[type]> [value]");
									}
									
									if ((typeGiven && commands.length >= 6)
											|| (!typeGiven && commands.length >= 5)) {
										string = commands [3 + (typeGiven ? 1 : 0)];
										if (string.equals ("to")) {
											to = commands [4 + (typeGiven ? 1 : 0)];
											
											int pointer = -1;
											for (int j = 0; j < to.length (); j ++) {
												char symbol = to.charAt (j);
												
												if (symbol == '/' || symbol == '\\') {
													pointer = j;
													break;
												}
											}
											
											if (pointer == -1) {
												if (variable.equals (to) || to.equals (".")) {
													to = "";
												} else {
													System.out.println ("[ERROR] Invalid id name given `" + to 
																			+ "` in local path in line " + line
																			+ " ... ADDING FAILEDa");
													continue assembler;
												}
											} else {
												String root = to.substring (0, pointer);
												if (variable.equals (root) || root.equals (".")) {
													to = to.substring (pointer + 1);
												} else {
													System.out.println ("[ERROR] Invalid id name given `" + root 
																			+ "` in local path in line " + line
																			+ " (`" + variable + "` expected)"
																			+ " ... ADDING FAILED");
													continue assembler;
												}
											}
											
											if (!PackageTree.checkPath (to)) {
												System.out.println ("[ERROR] Invalid source path `" + to 
																		+ "` given in line " 
																		+ line + " ... ADDING FAILED");
												continue assembler;
											}
											
											to = constantsReader.putConstants (to, line);
											to = to.toLowerCase ();
										} else {
											System.out.println ("[ERROR] Action `" + string + "` is not supported"
																	+ " in line " + line + " ... ADDING FAILED");
											continue assembler;
										}
									} /*else if (typeGiven && commands.length < 6) {
										System.out.println ("[ERROR] Not enough arguments for command `" + action
												+ "` in line " + line + " ... ignore command");
										System.out.println ("[FORMAT] This command format: "
																+ "[id name] [action] <[type]> [value] to [package]");
									}*/ else if (commands.length > 3 + (typeGiven ? 1 : 0)) {
										System.out.println ("[WARNING] Unexpected symbols `" 
																+ commands [3 + (typeGiven ? 1 : 0)] + "` found "
																+ "near command `" + action + "` in line " + line
																+ " ... ignore them");
									}
								} else {
									System.out.println ("[ERROR] Not enough arguments for command `" + action
															+ "` in line " + line + " ... ignore command");
									System.out.println ("[FORMAT] This command format: "
															+ "[id name] [action] <[type]> [value]");
								}
								
								if (commands.length > 6) {
									System.out.println ("[WARNING] Unexpected symbols `" + commands [5] + "` found "
															+ "near command `" + action + "` in line " + line
															+ " ... ignore them");
								}
								
								if (itype == null) {
									System.out.println ("[ERROR] Failed to fetch `" + value
															+ "` type in line " + line + " ... PARSE FAILED");
									wasStopped = true;
									break assembler;
								} 
								
								if (showDebug) {
									System.out.println ("[DEBUG] Value: " + value + " (type " + itype + ")");
								}
								
								if (itype == Types.UNKNOWN) {
									System.out.println ("[ERROR] Unknown type given `" + commands [2]
															+ "` in line " + line + " ... PARSE FAILED");
									
									continue assembler;
								} else if (itype == Types.MANIFEST) {
									System.out.println ("[ERROR] To add manifest file in jar use `set` "
															+ " instead of `" + action + "` in line " + line
															+ " ... ignore it");
									continue assembler;
								} else if (itype == Types.FILE) {
									PackageTree root = pathes.get (variable);
									String toLocal = _fetchLocalPath (variable, to, line);
									
									if (toLocal == null) {
										wasStopped = true;
										break assembler;
									}
									
									String fileName = _fetchFileName (value, line);
									
									if (fileName == null) {
										wasStopped = true;
										break assembler;
									}
									
									root.addFile (toLocal, fileName, value);
								} else if (itype == Types.PACKAGE) {
									if (pathes.containsKey (value)) {
										PackageTree tree = pathes.get (variable);
										tree.addTree (pathes.get (value), to);
										
										// IMPORTANT //
										variables.remove (value);
										pathes.remove    (value);
									} else {
										if (PackageTree.checkPath (value)) {
											PackageTree tree = new PackageTree ();
											
											if (tree.buildTreeFromPath (value, line)) {
												pathes.get (variable).addTree (tree, to);
											} else {
												continue assembler;
											}
										} else {
											System.out.println ("[ERROR] Invalid source path `" + value 
																	+ "` given in line " 
																	+ line + " ... ADDING FAILED");
											continue assembler;
										}
									}
								}
							} else {
								System.out.println ("[ERROR] Not enough arguments for command"
										+ " in line " + line + " ... ignore command");
								System.out.println ("[FORMAT] This command format: [id name] [action] [argument]");
								
								wasStopped = true;
								break assembler;
							}
						} else if (action.equals ("set")) {
							if (commands.length >= 4) {
								if (commands.length > 4) {
									System.out.println ("[WARNING] Unexpected symbols `" + commands [4] + "` found "
															+ "near command `" + action + "` in line " + line
															+ " ... ignore them");
								}
								
								Types itype = _fetchType (commands [2]);
								String name = commands [3];
								
								if (itype == null) {
									/* IT'S IMPOSSIBLE */
									/*   DO NOT SEE   */
								} else if (itype == Types.UNKNOWN) {
									System.out.println ("[ERROR] Unknown type given `" + commands [2]
															+ "` in line " + line + " ... PARSE FAILED");
									continue assembler;
								} else if (itype == Types.MANIFEST) {
									_registerVariable (new String [] {"create", "<manifest>", name}, line);
									
									manifests.put (name, new HashMap <> ());
									manifests.get (name).put ("__PARENT__", variable);
								}
							} else {
								System.out.println ("[ERROR] Not enough arguments for command"
														+ " in line " + line + " ... ignore command");
								System.out.println ("[FORMAT] This command format: [id name] set <[type]> [id name2]");
							}
						} else {
							System.out.println ("[ERROR] Requested method `" + action
													+ "` is not supported in line " + line
													+ " ... ighore it");
						}
					} else {
						System.out.println ("[ERROR] Not enough argument for command"
												+ " in line " + line + " ... ignore command");
						System.out.println ("[FORMAT] This command format: [id name] [action] [argument]");
						
						wasStopped = true;
						break assembler;
					}
				} else if (type == Types.MANIFEST) {
					
					//STOP
				}
			} else {
				System.out.println ("[ERROR] Variable `" + variable
										+ "` was not created before"
										+ " in line " + line + " ... ASSEMBLY FAILED");
				wasStopped = true;
				break assembler;
			}
		}
		
		if (!wasStopped) {
			runInstructionsStatus = true;
		}
	}
	
	public void buildSandbox () {
		if (showDebug) { System.out.println ("[DEBUG] Starting building sandbox..."); }
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
	
	private Types _fetchTypeFromPath (String path) {
		int pointer = path.lastIndexOf ('.');
		
		if (pointer == -1) {
			boolean directoryExists = false;
			
			File directory = new File (path);
			directoryExists = directory.exists () && directory.isDirectory ();
			directoryExists = directoryExists 
								|| (variables.containsKey (path) 
										&& variables.get (path) == Types.PACKAGE);
			
			if (!directoryExists) {
				System.out.println ("[ERROR] Requested directory on path `" + path 
										+ "` does not exist");
			} else {
				return Types.PACKAGE;
			}
		} else {
			boolean fileExists = false;
			
			File file = new File (path);
			fileExists = file.exists () && file.isFile ();
			
			if (!fileExists) {
				System.out.println ("[ERROR] Requested file on path `" + path 
										+ "` does not exist");
			} else {
				return Types.FILE;
			}
		}
		
		return null;
	}
	
	private String _fetchLocalPath (String variable, String path, int line) {
		if (path == null || path.length () == 0) {
			return "";
		}
		
		if (path.charAt (0) == '.') {
			if (path.length () >= 3) {
				return path.substring (2);
			}
			
			System.out.println ("[ERROR] Given local path `./` is not correct."
									+ " Line: " + line);
		} else {
			StringBuilder sb = new StringBuilder ();
			int pointer = -1;
			
			for (int i = 0; i < path.length (); i ++) {
				char symbol = path.charAt (i);
				
				if (symbol == '/' || symbol == '\\') {
					pointer = i;
					break;
				} else {
					sb.append (symbol);
				}
			}
			
			if (pointer != -1) {
				if (variable.equals (sb.toString ())) {
					int offset = Math.min (pointer + 1, 3);
					if (path.length () >= offset) {
						return path.substring (offset);
					} else {
						System.out.println ("[ERROR] Given local path `" + variable + "/` is not correct."
												+ " Line: " + line + " ... ADDING FAILED");
					}
				} else {
					System.out.println ("[ERROR] Wrong variable given `" + variable + "` in local path"
											+ " in line " + line + " ... ADDING FAILED");
				}
			} else {
				System.out.println ("[WARNING] Root path was not found in local path `" + path 
										+ "` in line " + line);
				return path;
			}
		}
		
		return null;
	}
	
	private String _fetchFileName (String path, int line) {
		if (path == null || path.length () == 0) {
			System.out.println ("[ERROR] Empty file name given in path `" + path 
									+ "` in line " + line + " ... ADDING FAILED");
			return null;
		}
		
		int pointer = -1;
		
		for (int i = path.length () - 1; i >= 0; i --) {
			char symbol = path.charAt (i);
			
			if (symbol == '/' || symbol == '\\') {
				pointer = i;
				break;
			}
		}
		
		if (pointer != -1 && pointer != path.length () - 1) {
			return path.substring (pointer + 1);
		} else if (pointer == path.length () - 1) {
			System.out.println ("[ERROR] Empty file name given in path `" + path 
									+ "` in line " + line + " ... ADDING FAILED");
			return null;
		}
		
		return path;
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
				String mask = "^([à-ÿÀ-ßa-zA-Z0-9\\-\\$\\.\\,\\:\\!\\_\\/]+)$";
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
	 * readInstructionsStatus  <br />
	 * runInstructionsStatus   <br />
	 * buildingSandboxStatus   <br />
	 * */
	public boolean getStatus (String status) {
		if (status.equals ("getPropertiesFileStatus")) {
			return getPropertiesFileStatus;
		} else if (status.equals ("readInstructionsStatus")) {
			return readInstructionsStatus;
		} else if (status.equals ("runInstructionsStatus")) {
			return runInstructionsStatus;
		} else if (status.equals ("buildingSandboxStatus")) {
			return buildingSandboxStatus;
		}
		
		System.out.println ("[DEBUG] Status `" + status + "` not found");
		return false;
	}
	
}
