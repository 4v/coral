import sbt._
import Keys._

object Resolvers {
  val allResolvers = Seq (
    "Spray Repository"        at "http://repo.spray.io",
    "Typesafe Repository"     at "http://repo.typesafe.com/typesafe/releases/",
    "krasserm at bintray"     at "http://dl.bintray.com/krasserm/maven"
  )
}

