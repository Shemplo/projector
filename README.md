# ![](projector.png) Projector #

Very **simple** but **powerful** tool to pack your java project in JAR-file, that contains only one runnable file.
Now it's absolutely simple to merge all libraries with reflections to one place with your configuration. 


*Also you can use this tool as zip-archiver* (simple bonus)

### Get started ###

Everything that you need is download sources and pack them into JAR (or already built JAR-file).

Then you are able to start packing your project: only you need create a file `manual.assembler` 
in the same directory where loaded JAR ( *pras.jar* ) is. 
There you will write instructions for builder.

To start *pras.jar* write command in console

	`java -jar pras.jar`

and follow the assembly.

### Structure ###

Here will be described how `manual.assembler` file is arranged.

The main part of this file - is **Sections**. 
In each of sections can be used only determined commands that is provided below.
The structure of declaring section is `:[section name]:` (f.e. `:application:`)

	#This is comment

* Section **:constants:**

In this section you can manage constants and their values.

If you want to write constants in separate file, write command:
	
	#Signature
	.constants = [path to file]
	
	#Example
	.constants = constants.assembler
	
Then to add new constant write command:

	#Signature
	$+[constant name] = [constant value]
	
	#Example
	$+MY_LIBRARY = C:/mylib
	
To get value of constant write command:

	#Signature
	$.[constant name]
	
	#Example
	$+MY_JAVA = $.MY_LIBRARY/java
	
Also you should know about existing default constants:

* **ASSEMBLER_DIR** - by default it points on the child `assembler` of directory where `pras.jar` was run. 
In this directory will happen all process of assembling.

* **SRC_DIR**       - by default it points on the child `bin` of directory where `pras.jar` was run.
In this directory (suggested) will be placed class files of current project
	
* **LOGS_DIR**      - by default it's like **$.ASSEMBLER_DIR/logs**.
Here will be places all logs of assembling.
	
* **SANDBOX_DIR**   - by default it's like **$.ASSEMBLER_DIR/sandbox**.
Here will be unpacked all JAR libraries that you wanted t add.
Also here can be other temporary files.
	
* **TARGET_DIR**    - by default it's like **$.ASSEMBLER_DIR/target**.
Here will be assembled project but not packed into JAR.
If you wanted to do it manually so it's up for you.

You can change the value of default constants one time. To do this use command:

	#Signature
	$![constant name] = [constant value]
	
	#Example
	$!SRC_DIR = c:/my/project
	
* Section **:assemble:**

In this section you can put commands for assembler. 
Also here you can declare a manifests for JARs.

* Section **:application:**

In this section you can set properties of application (such as *name*, *author*, *version*).