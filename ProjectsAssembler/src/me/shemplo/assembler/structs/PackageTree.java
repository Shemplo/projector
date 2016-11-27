package me.shemplo.assembler.structs;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageTree {
	
	private Node root;
	private String manifest;
	
	public PackageTree () {
		root = new Node ();
	}
	
	public void setManifest (String manifest) {
		assert manifest != null;
		this.manifest = manifest;
	}
	
	public String manifest (){
		return manifest;
	}
	
	public boolean addPackagePath (String path) {
		boolean added = false;
		
		if (checkPath (path)) {
			root.addPackagePath (path);
			added = true;
			
			if (path != null && path.length () != 0) {
				System.out.println ("[DEBUG] Path `" + path + "` was built successfully");
			}
		} else {
			System.out.println ("[ERROR] Invalid path `" + path 
									+ "` given ... ADDING FAILED");
		}
		
		return added;
	}
	
	public boolean addFile (String path, String name, String source) {
		boolean added = false;
		
		if (checkPath (path)) {
			if (checkPath (source)) {
				root.addFile (path, name, source);
				added = true;
				
				System.out.println ("[DEBUG] File `" + name + "` was added successfully"
										+ " to package `" + path + "`");
			} else {
				System.out.println ("[ERROR] Invalid source path `" + path 
										+ "` given ... ADDING FAILED");
			}
		} else {
			System.out.println ("[ERROR] Invalid path `" + path 
									+ "` given ... ADDING FAILED");
		}
		
		return added;
	}
	
	public boolean addTree (PackageTree tree) {
		boolean added = false;
		
		Node node = mergeNodes (root, tree.root);
		if (node != null) { root = node; }
		else {
			System.out.println ("[ERROR] Something goes wgong ... ADDING FAILED");
		}
		
		return added;
	}
	
	public boolean addTree (PackageTree tree, String path) {
		boolean added = false;
		
		this.addPackagePath (path);
		Node node = root._getNode (path);
		mergeNodes (node, tree.root);
		
		return added;
	}
	
	/**
	 * second >> first
	 * 
	 * */
	private static Node mergeNodes (Node first, Node second) {
		assert first != null;
		assert second != null;
		
		second.packages.forEach ((key, value) -> {
			if (first.packages.containsKey (key)) {
				mergeNodes (first.packages.get (key), value);
			} else {
				first.packages.put (key, value);
			}
		});
		
		second.files.forEach ((key, value) -> {
			if (first.files.containsKey (key)) {
				System.out.println ("[WARNING] File `" + key + "` is already"
										+ " added to merging node ... override it");	
			}
			
			first.files.put (key, value);
		});
		
		return first;
	}
	
	public static boolean checkPath (String path) {
		if (path == null || path.length () == 0) {
			return true;
		}
		
		char lastChar = path.charAt (path.length () - 1);
		if (lastChar == '/' || lastChar == '\\') {
			path = path.substring (0, path.length () - 1);
		}
		
		String  mask = "^([^\\.][à-ÿÀ-ßa-zA-Z0-9\\\\\\/\\_\\.\\%\\:]+)$";
		Pattern pattern = Pattern.compile (mask);
		
		Matcher matcher = pattern.matcher (path);
		return matcher.find ();
	}
	
	public boolean buildTreeFromPath (String path, int line) {
		boolean built = true;
		
		File rootDir = new File (path);
		if (rootDir.exists ()) {
			if (rootDir.isDirectory ()) {
				built = root.getAllFromPath (rootDir);
			} else {
				System.out.println ("[ERROR] Directory on path `" + path
										+ " is not a directory "
										+ " in line " + line 
										+ " ... ADDING FAILED");
				built = false;
			}
		} else {
			System.out.println ("[ERROR] Directory on path `" + path 
									+ "` is not found in line " + line
									+ " ... ADDING FAILED");
			built = false;
		}
		
		return built;
	}
	
	public boolean buildDirectoryFromTree (File root) {
		if (!root.exists ()) {
			root.mkdir ();
		}
		
		try {
			this.root.buildDirectory (root); 
			return true;
		} catch (Exception e) {
			System.out.println (e.getMessage ());
			return false;
		}
	}
	
	public static boolean clearDirectory (File rootDir, int line) {
		boolean cleared = false;
		
		if (rootDir.exists ()) {
			if (rootDir.isDirectory ()) {
				for (File tmp: rootDir.listFiles ()) {
					cleared = PackageTree.clearDirectory (tmp, line) && cleared;
					tmp.delete ();
				}
				
				System.out.println ("[LOG] Directory `" + rootDir.getName () + "` deleted");
				rootDir.delete ();
			} else {
				rootDir.delete ();
				cleared = true;
				
				System.out.println ("[LOG] File `" + rootDir.getName () + "` deleted");
			}
		} else {
			System.out.println ("[ERROR] Directory on path `" + rootDir.getAbsolutePath () 
									+ "` is not found in line " + line
									+ " ... ADDING FAILED");
		}
		
		return cleared;
	}
	
	private class Node {
		
		@SuppressWarnings ("unused")
		public Node parent;
		
		private HashMap <String, Node>   packages;
		private HashMap <String, String> files;
		
		public Node () {
			_init ();
		}
		
		public Node (Node parent) {
			this.parent = parent;
			_init ();
		}
		
		private void _init () {
			packages = new HashMap <> ();
			files    = new HashMap <> ();
		}
		
		private Node _getNode (String path) {
			if (path == null || path.length () == 0) {
				return this;
			} else {
				int pointer = path.length ();
				
				for (int i = 0; i < path.length (); i ++) {
					char symbol = path.charAt (i);
					
					if (symbol == '/' || symbol == '\\') {
						pointer = i;
						break;
					}
				}
				
				String to   = path.substring (0, pointer);
				Node   node = packages.get (to);
				
				if (node != null) {
					if (pointer != path.length ()) {
						String rest = path.substring (pointer + 1);
						return node._getNode (rest);
					} else {
						return node;
					}
				} else {
					System.out.println ("[ERROR] Package `" + to + "` not found"
											+ " in directory `" + "`");
				}
			}
			
			return null;
		}
		
		public void addPackagePath (String path) {
			if (path == null || path.length () == 0) {
				return;
			} else {
				int pointer = -1;
				path = path.toLowerCase ();
				
				for (int i = 0; i < path.length (); i ++) {
					char symbol = path.charAt (i);
					
					if (symbol == '/' || symbol == '\\') {
						pointer = i;
						break;
					}
				}
				
				if (pointer == -1) {
					if (!packages.containsKey (path)) {
						packages.put (path, new Node (this));
					}
				} else {
					String to   = path.substring (0, pointer);
					String rest = path.substring (pointer + 1);
					
					if (!packages.containsKey (to)) {
						packages.put (to, new Node (this));
					}
					
					packages.get (to).addPackagePath (rest);
				}
			}
		}
		
		public void addFile (String path, String name, String source) {
			if (path == null || path.length () == 0) {
				if (files.containsKey (name)) {
					System.out.println ("[ERROR] File `" + name 
											+ "` is already added ... override");
				}
				
				files.put (name, source);
			} else {
				path = path.toLowerCase ();
				this.addPackagePath (path);
				
				Node node = this._getNode (path);
				node.addFile ("", name, source);
			}
		}
		
		public boolean getAllFromPath (File file) {
			boolean got = true;
			
			if (file.isDirectory ()) {
				for (File tmp: file.listFiles ()) {
					String name = tmp.getName ();
					
					if (tmp.isDirectory ()) {
						if (!this.packages.containsKey (name)) {
							this.packages.put (name, new Node (this));
						}
						
						this.packages.get (name).getAllFromPath (tmp);
					} else if (tmp.isFile ()) {
						if (this.files.containsKey (name)) {
							System.out.println ("[WARNING] File from `" + tmp.getAbsolutePath () 
													+ "` is already added to " + file.getName ()
													+ " ... override it");
						}
						
						this.files.put (name, tmp.getAbsolutePath ());
					} else {
						System.out.println ("[ERROR] Unexpected error :(");
						got = false;
					}
				}
			}
			
			return got;
		}
		
		public void buildDirectory (File root) throws Exception {
			packages.forEach ((key, value) -> {
				File child = new File (root.getAbsolutePath () + File.separatorChar + key);
				child.mkdir ();
				
				try                 { value.buildDirectory (child); } 
				catch (Exception e) { e.printStackTrace (); }
			});
			
			files.forEach ((key, value) -> {
				File file = new File (value);
				System.out.println ("[LOG] File " + file);
				
				if (file.exists ()) {
					File dest = new File (root.getAbsolutePath () + File.separatorChar + key);
					try                 { Files.copy (file.toPath (), dest.toPath ()); } 
					catch (Exception e) { return; }
				} else {
					System.out.println ("[ERROR] File `" + key + "` does not exist on path `" 
											+ value + "` ... BUILD FAILED");
					return;
				}
			});
		}
	}
	
}
