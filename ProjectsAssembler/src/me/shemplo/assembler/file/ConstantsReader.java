package me.shemplo.assembler.file;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.shemplo.assembler.Assembler;

public class ConstantsReader {

	private FileReader     file;
	private BufferedReader reader;
	
	private HashSet <String>         system;
	private HashMap <String, String> consts;
	
	public ConstantsReader () {
		consts = new HashMap <> ();
		system = new HashSet <> ();
		_addDefaultConstants ();
	}
	
	public ConstantsReader (String fileName) throws FileNotFoundException {
		file = new FileReader (fileName);
		
		if (file != null) {
			reader = new BufferedReader (file);
			consts = new HashMap <>     ();
			system = new HashSet <>     ();
			
			try {
				_addDefaultConstants ();
				_parseConstants ();
			} catch (IOException ioe) {
				System.out.println ("[ConstantsReader] Exceprion: `" 
										+ ioe.getMessage () 
										+ "` in method `_parseConstants`");
			}
		}
	}
	
	private void _addDefaultConstants () {
		String path = this.getClass ()
							.getProtectionDomain ()
							.getCodeSource ()
							.getLocation ()
							.getPath ()
							.trim ();
		
		try {
			path = URLDecoder.decode (path, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			System.out.println ("[WARNING] Assembler path contains non-ASCII "
									+ "chars that is unsupported by UTF-8");
		}
		
		String realPath = path;
		if (path.charAt (path.length () - 1) != '/' || path.charAt (path.length () - 1) != '\\') {
			int position = Math.max (path.lastIndexOf ('/'), path.lastIndexOf ('\\'));
			if (position != -1) { realPath = path.substring (0, position); }
		}
		
		if (path.charAt (0) == '/' || path.charAt (0) == '\\') {
			realPath = realPath.substring (1);
		}
		
		final String SRC_DIR = "SRC_DIR";
		consts.put (SRC_DIR, realPath + "/src");
		system.add (SRC_DIR);
		
		final String ASSEMBLER_DIR = "ASSEMBLER_DIR";
		consts.put (ASSEMBLER_DIR, realPath + "/assembler");
		system.add (ASSEMBLER_DIR);
		//consts.put ("ASSEMBLER_DIR", "Hello");
		
		final String SANDBOX_DIR = "SANDBOX_DIR";
		consts.put (SANDBOX_DIR, realPath + "/assembler/sandbox");
		system.add (SANDBOX_DIR);
		
		final String TARGET_DIR = "TARGET_DIR";
		consts.put (TARGET_DIR, realPath + "/assembler/target");
		system.add (TARGET_DIR);
		
		final String LOGS_DIR = "LOGS_DIR";
		consts.put (LOGS_DIR, realPath + "/assembler/logs");
		system.add (LOGS_DIR);
	}
	
	private void _parseConstants () throws IOException {
		String string     = null;
		int    lineNumber = 0;
		
		while ((string = reader.readLine ()) != null) {
			String [] commands = Assembler.compressString (string);
			registerConstant (commands, lineNumber + 1);
			
			lineNumber ++;
		}
	}
	
	public void registerConstant (String [] commands, int line) {
		if (commands.length >= 3) {
			if (commands.length > 3) {
				System.out.println ("[WARNING] Unexpected symbols `" + commands [3] + "` found "
										+ "near constant command in line " + line
										+ " ... ignore them");
			}
			
			String nconst   = commands [0];
			String operator = commands [1];
			String value    = commands [2];
			
			boolean systemConst = false;
			boolean nconstsFlag = false;
			if (nconst.length () > 3) {
				if (nconst.charAt (0) == '$') {
					if (nconst.charAt (1) == '+') {
						String name = nconst.substring (2).toUpperCase ();
						
						String mask = "^([A-Z0-9\\_]+)$";
						Pattern pat = Pattern.compile (mask);
						
						Matcher matcher = pat.matcher (name.trim ());
						if (matcher.matches ()) {
							if (!consts.containsKey (name)) {
								nconstsFlag = true;
								nconst      = name;
							} else {
								System.out.println ("[ERROR] Constant `" + name + "` is already"
														+ " registerd. Line: " + line + " ... ignore it");
							}
						} else {
							System.out.println ("[ERROR] Invalid character in constant name `" + nconst
													+ "` in line " + line + " ... ignore it");
						}
					} else if (nconst.charAt (1) == '!') {
						String name = nconst.substring (2).toUpperCase ();
						
						String mask = "^([A-Z0-9\\_]+)$";
						Pattern pat = Pattern.compile (mask);
						
						Matcher matcher = pat.matcher (name.trim ());
						if (matcher.matches ()) {
							if (system.contains (name)) {
								nconstsFlag = true;
								systemConst = true;
								nconst      = name;
							} else {
								System.out.println ("[ERROR] Constant `" + name + "` is not found"
														+ " in reserved. Line: " + line + " ... ignore it");
							}
						} else {
							System.out.println ("[ERROR] Invalid character in constant name `" + nconst
													+ "` in line " + line + " ... ignore it");
						}
					} else {
						System.out.println ("[ERROR] Declaration of new constant"
												+ "should have operator `+` or `!` in line " + line
												+ " ... ignore it");
					}
				} else {
					System.out.println ("[WARNING] Declaration of new constant "
											+ "should start with `$` in line " + line);
				}
			} else {
				System.out.println ("[ERROR] Too short declaration of constant command `" 
										+ nconst + "` in line " + line + " ... ignore it");
			}
			
			if (!nconstsFlag) {
				return;
			}
			
			boolean operatorFlag = false;
			if (operator.equals ("=")) {
				operatorFlag = true;
			} else {
				System.out.println ("[ERROR] Unexpected operator `" + operator 
						+ "` for declaration constant in line " + line
						+ " ... ignore it");
				System.out.println ("[FORMAT] This command format: $+[constant name] = [constant value]");
			}
			
			if (!operatorFlag) {
				return;
			}
			
			boolean valueFlag = false;
			if (value.length () > 0) {
				String mask = "^([�-��-�a-zA-Z0-9\\-\\$\\.\\,\\:\\!\\_\\/]+)$";
				Pattern pat = Pattern.compile (mask);
				
				Matcher matcher = pat.matcher (value.trim ());
				if (matcher.matches ()) {
					valueFlag = true;
				} else {
					System.out.println ("[ERROR] Invalid character in value `" + value
											+ "` in line " + line + " ... ignore it");
				}
			} else {
				System.out.println ("[ERROR] Constant can't have empty value "
										+ "in line " + line + " ... ignore it");
			}
			
			if (!valueFlag) {
				return;
			}
			
			String finalValue = putConstants (value, line);
			if (finalValue != null) {
				consts.put (nconst, finalValue);
				if (systemConst) { system.remove (nconst); }
			}
		} else {
			System.out.println ("[ERROR] Not enough argument for creating constant"
					+ " in line " + line + " ... ignore command");
			System.out.println ("[FORMAT] This command format: $+[constant name] = [constant value]");
		}
	}
	
	public String putConstants (String string, int line) {
		StringBuilder sb   = new StringBuilder ();
		StringBuilder name = new StringBuilder ();
		
		boolean valid = true;

		iterator:
		for (int i = 0; i < string.length (); i ++) {
			char current = string.charAt (i);
			
			if (current != '$') {
				sb.append (current);
			} else if (i < string.length () - 2) {
				char next = string.charAt (i + 1);
				
				if (next != '.') {
					sb.append (current);
				} else {
					name.setLength (0);
					int start = i + 2;
					i = start;
					char seq = 0;
					
					while (i < string.length () && (Character.isUpperCase (seq = string.charAt (i))
							|| Character.isDigit (seq) || (seq) == '_')) {;
						name.append (seq);
						i ++;
					}
					
					if (i != start) {
						i --;
						String nconst = name.toString ();
						
						if (consts.containsKey (nconst)) {
							sb.append (consts.get (nconst));
						} else {
							System.out.println ("[ERROR] Constant `" + name.toString ()
													+ "` was not declared before"
													+ " in line " + line + " ... ignore it");
							valid = false;
							break iterator;
						}
					} else {
						System.out.println ("[ERROR] Empty constant name"
												+ " in line " + line
												+ ", position " + i
												+ " ... ignore it");
						
						valid = false;
						break iterator;
					}
				}
			} else {
				sb.append (current);
			}
		}
		
		if (valid) { return sb.toString (); } 
		else       { return null; }
	}
	
}
