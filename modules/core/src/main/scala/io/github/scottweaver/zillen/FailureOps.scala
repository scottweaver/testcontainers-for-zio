/*
 * Copyright 2021 io.github.scottweaver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
