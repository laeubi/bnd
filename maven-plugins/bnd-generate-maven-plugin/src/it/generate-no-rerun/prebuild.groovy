// Check the ecore file exist!
File ecore = new File(basedir,"src/main/resources/model/test.ecore")
assert ecore.isFile()

// Check the genmodel file exist!
File genmodel = new File(basedir,"src/main/resources/model/test.genmodel")
assert genmodel.isFile()

// Check the pomfile exist!
File pom = new File(basedir,"pom.xml")
assert pom.isFile()

// Check the if some code generated file exist!
File testClass = new File(basedir,"src/main/java/aQute/bnd/maven/model/test/TestShouldNotBeDeleted.java")
assert testClass.isFile()

genmodel.setLastModified(testClass.lastModified() - 10000L);
pom.setLastModified(testClass.lastModified() - 10000L);