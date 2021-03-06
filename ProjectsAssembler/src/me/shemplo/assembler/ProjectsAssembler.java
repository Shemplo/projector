package me.shemplo.assembler;

public class ProjectsAssembler {

	private static final String PROPERTIES_FILE = "manual.assembler";
	
	private static Assembler assembler;
	
	public static void main (String [] args) throws Exception {
		assembler = new Assembler ();
		
		assembler.setPropertiesFileName (PROPERTIES_FILE);
		if (!assembler.getStatus ("getPropertiesFileStatus")) { return; }
		
		assembler.getInstructions ();
		if (!assembler.getStatus ("readInstructionsStatus")) { return; }
		
		assembler.assemble ();
		if (!assembler.getStatus ("runInstructionsStatus")) { return; }
		
		assembler.buildSandbox ();
	}
	
}
