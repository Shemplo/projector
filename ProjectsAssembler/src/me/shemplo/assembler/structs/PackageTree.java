package me.shemplo.assembler.structs;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageTree {
	
	private Node root;
	
	public PackageTree (String name) {
		root = new Node (name);
	}
	
	public boolean addPackage (String path, String name) {
		boolean added = false;
		
		if (_checkPath (path)) {
			added = root.addPackage (path, name);
		} else {
			System.out.println ("[ERROR] Invalid path `" + path 
									+ "` given ... ADDING FAILED");
		}
		
		return added;
	}
	
	public boolean addFile (String path, String name) {
		boolean added = false;
		
		if (_checkPath (path)) {
			added = root.addFile (path, name);
		} else {
			System.out.println ("[ERROR] Invalid path `" + path 
									+ "` given ... ADDING FAILED");
		}
		
		return added;
	}
	
	private boolean _checkPath (String path) {
		if (path == null || path.length () == 0) {
			return true;
		}
		
		char lastChar = path.charAt (path.length () - 1);
		if (lastChar == '.' || lastChar == '/' || lastChar == '\\') {
			path = path.substring (0, path.length () - 1);
		}
		
		String  mask = "^([^\\.][a-zA-Z0-9\\\\\\/\\_\\.]+)$";
		Pattern pattern = Pattern.compile (mask);
		
		Matcher matcher = pattern.matcher (path);
		return matcher.find ();
	}
	
	private class Node {
		
		public Node parent;
		public String name, path;
		
		private HashMap <String, Node>   packages;
		private HashMap <String, String> files;
		
		public Node (String name) {
			_init (name);
		}
		
		public Node (String name, Node parent) {
			this.parent = parent;
			_init (name);
		}
		
		private void _init (String name) {
			this.name = name;
			
			if (parent == null) {
				this.path = name;
			} else {
				this.path = parent.path + "/" + this.name;
			}
			
			packages = new HashMap <> ();
			files    = new HashMap <> ();
		}
		
		private Node _getNode (String path) {
			if (path == null || path.length () == 0) {
				return this;
			} else {
				int pointer = path.length ();
				
				for (int i = 0; i < path.length (); i ++) {
					if (path.charAt (i) == '/' || path.charAt (i) == '.') {
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
						return this;
					}
				} else {
					System.out.println ("[ERROR] Package `" + to + "` not found"
											+ " in directory `" + this.path + "`");
				}
			}
			
			return null;
		}
		
		public boolean addPackage (String path, String name) {
			boolean added = false;
			Node node = _getNode (path);
			
			if (node != null) {
				if (node.packages.containsKey (name)) {
					System.out.println ("[WARNING] Package `" + name + "` already"
											+ " exists in directory `" + node.path + "` ... overwited");
				}
				
				node.packages.put (name, new Node (name, node));
				added = true;
			}
			
			return added;
		}
		
		public boolean addFile (String path, String name) {
			boolean added = false;
			Node node = _getNode (path);
			
			if (node != null) {
				if (node.packages.containsKey (name)) {
					System.out.println ("[WARNING] File `" + name + "` already"
											+ " exists in directory `" + node.path + "` ... overwited");
				}
				
				node.files.put (name, node.path + "/" + name);
				added = true;
			}
			
			return added;
		}
	}
	
}
