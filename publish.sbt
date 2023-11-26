ThisBuild / organization := "io.github.eb2501"
ThisBuild / organizationName := "eb2501"
ThisBuild / organizationHomepage := Some(url("https://eb2501.github.io"))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/eb2501/fluor-core"),
    "scm:git@github.com:eb2501/fluor-core.git"
  )
)
ThisBuild / developers := List(
  Developer(
    id = "eb2501",
    name = "Eric Blond",
    email = "eb2501@gmail.com",
    url = url("https://eb2501.github.io")
  )
)

ThisBuild / description := "Implicit Reactive Framework written in Scala3."
ThisBuild / licenses := List(
  "Apache 2" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")
)
ThisBuild / homepage := Some(url("https://github.com/eb2501/fluor-core"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := {
  val nexus = "https://s01.oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}
ThisBuild / publishMavenStyle := true

ThisBuild / dynverSonatypeSnapshots := true

ThisBuild / versionScheme := Some("early-semver")
