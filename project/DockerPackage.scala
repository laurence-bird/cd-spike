import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging
import com.typesafe.sbt.packager.docker.DockerPlugin.autoImport.Docker
import com.typesafe.sbt.packager.docker._
import com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._
import sbt.Keys._
import sbt._

object DockerPackage {

  private lazy val setupAlpine = Seq(
    Cmd("RUN", "apk --update add openjdk8-jre"),
    Cmd("RUN", "apk --update add bash")
  )

  val dockerLoginTask = TaskKey[Unit]("dockerLogin", "Log in to Amazon ECR")
  val dockerConfigTask = TaskKey[Unit]("dockerConfig", "Download UAT and PRD config from S3")

  val settings = Seq(
    packageName in Docker := "delivery-service-prototype-circleci",
    dockerRepository := Some("852955754882.dkr.ecr.eu-west-1.amazonaws.com"),
    dockerUpdateLatest := false,
    dockerExposedPorts := Seq(8080),
    dockerBaseImage := "alpine",
    dockerCommands := dockerCommands.value.head +: setupAlpine ++: dockerCommands.value.tail,
    mappings in Universal += file("src/main/resources/application.conf")      ->  "conf/local/application.conf",
    mappings in Universal += file("src/main/resources/uat/application.conf")  ->  "conf/uat/application.conf",
    bashScriptExtraDefines += """addJava "-Dconfig.file=${app_home}/../conf/${ENV}/application.conf"""",
    bashScriptExtraDefines += """addJava "-Xms256M"""",
    bashScriptExtraDefines += """addJava "-Xmx256M"""",
    dockerLoginTask := {
      import sys.process._
      "aws --region eu-west-1 ecr get-login" #| "bash" !
    },
    dockerConfigTask := {
      import sys.process._
      "aws --profile comms s3 sync s3://ovo-comms-platform-config/service-config/uat/delivery-service ./src/main/resources/uat" !
    },
    publish in Docker <<= (publish in Docker)
      .dependsOn(dockerLoginTask)
      .dependsOn(dockerConfigTask)
  )

}
