package io.github.scottweaver
package zillen

import zio._
import DockerContainerFailure._

private[zillen] trait FailureOps {

  def failInvalidConfig(msg: String, cause: Option[Throwable] = None) = ZIO.fail(invalidConfig(msg, cause))

  def failInvalidRuntimeState(msg: String, cause: Option[Throwable] = None) = ZIO.fail(invalidRuntimeState(msg, cause))

  def failReadyCheckFailed(msg: String, cause: Option[Throwable] = None) = ZIO.fail(readyCheckFailed(msg, cause))

  def invalidConfig(msg: String, cause: Option[Throwable] = None): InvalidDockerConfiguration = {
    val msg0 = cause.map(t => s"${msg} Cause: ${t.getMessage}").getOrElse(msg)
    InvalidDockerConfiguration(msg0, cause)
  }

  def invalidConfig(msg: String)(t: Throwable): InvalidDockerConfiguration = invalidConfig(msg, Some(t))

  def invalidRuntimeState(msg: String, cause: Option[Throwable] = None): InvalidDockerRuntimeState = {
    val msg0 = cause.map(t => s"${msg} Cause: ${t.getMessage}").getOrElse(msg)
    InvalidDockerRuntimeState(msg0, cause)
  }
  def invalidRuntimeState(msg: String)(t: Throwable): InvalidDockerRuntimeState = invalidRuntimeState(msg, Some(t))


  def readyCheckFailed(msg: String, cause: Option[Throwable] = None) =
    ContainerReadyCheckFailure(msg, cause)

}
