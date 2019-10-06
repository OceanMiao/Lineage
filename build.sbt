name := "Lineage"

version := "0.1"

scalaVersion := "2.12.4"

//libraryDependencies += "org.apache.calcite" % "calcite-core" % "1.21.0"
//libraryDependencies += "org.apache.calcite" % "calcite-server" % "1.18.0"
//libraryDependencies += "com.facebook.presto" % "presto-parser" % "0.226"
//libraryDependencies += "com.facebook.presto" % "presto-main" % "0.226"
//libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "2.1.1"
libraryDependencies += "org.antlr" % "antlr4" % "4.7.2"
libraryDependencies += "org.graphstream" % "gs-core" % "1.3"
libraryDependencies += "org.graphstream" % "gs-ui" % "1.3"




javaSource in Compile := baseDirectory.value / "src" / "main" / "generated"
