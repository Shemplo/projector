package me.shemplo.assembler.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class PropertiesReader {

	private FileReader     file;
	private BufferedReader reader;
	private String []      cache;
	private int            cacheLength;
	private boolean        finished;
	
	public PropertiesReader () {
		_init ();
	}
	
	private void _init () {
		cacheLength = 0;
		cache = new String [cacheLength];
		
		finished = false;
	}
	
	public void loadFile (String fileName) throws FileNotFoundException {
		this.file   = new FileReader (new File (fileName));
		
		if (file != null) {
			this.reader = new BufferedReader (file);
			_init ();
		}
	}
	
	public String readLine () {
		String string = null;
		
		try {
			if (!finished) { string = reader.readLine (); }
		} catch (IOException ioe) {
			System.out.println ("[PropertiesReader] Exceprion: `" 
									+ ioe.getMessage () 
									+ "` in method `setPropertiesFileName`");
			finished = true;
		}
		
		if (string != null) {
			_saveInCache (string);
		} else {
			finished = true;
		}
		
		return string;
	}
	
	public String readLine (int index) {
		if (_checkIndex (index)) {
			return cache [index];
		} else {
			throw new IndexOutOfBoundsException ("[PropertiesReader] Exceprion: "
													+ "`invalid index` "
													+ "in method `readLine`");
		}
	}
	
	private void _saveInCache (String string) {
		if (cacheLength + 1 >= cache.length) {
			String [] tmp = new String [cache.length > 0 ? 2 * cache.length : 1];
			System.arraycopy (cache, 0, tmp, 0, cache.length);
			
			cache = tmp;
		}
		
		cache [cacheLength ++] = string; 
	}
	
	private boolean _checkIndex (int index) {
		return index >= 0 && index < Math.min (cacheLength, cache.length);
	}
	
}
