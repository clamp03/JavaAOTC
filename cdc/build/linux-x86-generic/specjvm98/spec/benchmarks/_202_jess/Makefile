# To use this on UNIX, change all the '/' to '/'. This makefile
# should work with most MAKEs, include NMAKE on Win32.

.SUFFIXES: .java .class


.java.class :
	/usr/local/java/jdk1.3/solaris/bin/javac -sourcepath ../../.. -bootclasspath /micro/people/fresko/ws/j2me-cdc-dotdot-opt/build/solaris/btclasses.zip:/micro/people/fresko/ws/j2me-cdc-dotdot-opt/build/solaris/lib/foundation.jar:../../..:/usr/local/java/jdk1.3/solaris/jre/lib/rt.jar:.:jess -classpath ../../..   $<

SRC = Jess.java LostDisplay.java NullDisplay.java \
      TextAreaOutputStream.java Main.java
 
JESSSRC = \
	jess/Activation.java \
	jess/Binding.java  \
	jess/Context.java \
	jess/Deffacts.java \
	jess/Deffunction.java \
	jess/Defrule.java \
	jess/Deftemplate.java \
	jess/Fact.java \
	jess/Funcall.java \
	jess/GlobalContext.java \
	jess/Jesp.java \
	jess/JessToken.java \
	jess/JessTokenStream.java \
	jess/MathFunctions.java \
	jess/MiscFunctions.java \
	jess/MultiFunctions.java \
	jess/Node.java \
	jess/Node1.java \
	jess/Node2.java \
	jess/NodeNot2.java \
	jess/NodeTerm.java \
	jess/Pattern.java \
	jess/PredFunctions.java \
	jess/RU.java \
	jess/Rete.java \
	jess/ReteCompiler.java \
	jess/ReteDisplay.java \
	jess/ReteException.java \
	jess/StringFunctions.java \
	jess/Successor.java \
	jess/Test1.java \
	jess/Test2.java \
	jess/Token.java \
	jess/TokenVector.java \
	jess/Userfunction.java \
	jess/Userpackage.java \
	jess/Value.java \
	jess/ValueVector.java 

VSRC  = jess/view/View.java 

# Don't build 'view' command by default
OBJ = $(SRC:.java=.class) $(JESSSRC:.java=.class) 

Main: $(SRC) $(JESSSRC) $(OBJ) 

clean:
	rm -rf *.class *~
	rm -rf jess/*.class jess/*~
	rm -rf jess/view/*.class jess/view/*~

