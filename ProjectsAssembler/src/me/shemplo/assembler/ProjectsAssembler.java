package me.shemplo.assembler;

public class ProjectsAssembler {

	private static final String PROPERTIES_FILE = "manual.assembler";
	
	private static Assembler assembler;
	
	public static void main (String [] args) {
		assembler = new Assembler ();
		assembler.setPropertiesFileName (PROPERTIES_FILE);
		assembler.getInstructions       ();
	}
	
}