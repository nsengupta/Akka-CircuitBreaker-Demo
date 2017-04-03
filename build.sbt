name := """Akka-Circuit-Breaker-Demonstration"""

version := "1.0"

scalaVersion := "2.11.8"

// compileOrder := CompileOrder.JavaThenScala

//mainClass in (Compile,run) := Some("org.training.nirmalya.Driver")

//javacOptions ++= Seq("-source", "1.7", "-target", "1.7")


libraryDependencies ++= Seq(
  "com.typesafe.akka" % "akka-actor_2.11" % "2.4.17",
  "junit" % "junit" % "4.12",
  "com.novocode" % "junit-interface" % "0.11",
  "com.typesafe.akka" % "akka-testkit_2.11" % "2.4.16",
  "com.typesafe.akka" %% "akka-slf4j" % "2.4.16",
  "ch.qos.logback" % "logback-classic" % "1.0.9",
  "org.javalite" % "javalite-common" % "1.4.12"
)

