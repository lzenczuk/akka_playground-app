import Dependencies._

lazy val web = (project in file("web"))
  .settings(Commons.settings: _*)
  .settings(libraryDependencies ++= akkaDependencies ++ akkaHttpDependencies)
  .settings(mainClass in assembly := Some("com.github.lzenczuk.apa.web.Main"))
  .settings(assemblyJarName in assembly := "akka-playground-web.jar")
