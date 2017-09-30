name := "gpx-enricher"

version := "0.1"

scalaVersion := "2.12.3"

scalacOptions := Seq("-unchecked", "-deprecation")

libraryDependencies += "org.scala-lang.modules" % "scala-xml_2.12" % "1.0.6"

libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.5"

libraryDependencies += "io.spray" %% "spray-json" % "1.3.3"

