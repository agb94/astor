cd examples/math_70
mvn clean compile test  # compiling and running bug example
cd ../../
java -cp $(cat /tmp/astor-classpath.txt):target/classes fr.inria.main.evolution.AstorMain -mode jgenprog -package org.apache.commons -failing org.apache.commons.math.analysis.solvers.BisectionSolverTest -flthreshold 0.5 -srcjavafolder /src/java/ -srctestfolder /src/test/  -binjavafolder /target/classes/ -bintestfolder  /target/test-classes/ -location /Users/gabin/Workspace/astor/examples/math_70/ -stopfirst true -dependencies /Users/gabin/Workspace/astor/examples/libs/junit-4.4.jar -ingredientstrategy fr.inria.astor.approaches.promising.SimilarIngredientSearchStrategy
