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

package io.github.scottweaver.zio.testcontainers.localstack

import com.dimafeng.testcontainers.LocalStackContainer
import org.testcontainers.containers.localstack.{LocalStackContainer => JavaLocalStackContainer}
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import zio._
import zio.aws.core.config.{AwsConfig, CommonAwsConfig}
import zio.aws.netty.NettyHttpClient
import zio.aws.s3.S3
import zio.aws.s3.model.CreateBucketRequest
import zio.aws.s3.model.primitives.BucketName
import zio.test.Assertion._
import zio.test._

object ZLocalStackContainerSpec extends ZIOSpecDefault {

  override def spec =
    suite("ZLocalStackContainerSpec")(
      test("Should start a LocalStack container and create S3 bucket") {
        val testCase =
          for {
            s3              <- ZIO.service[S3]
            _               <- s3.createBucket(CreateBucketRequest(bucket = BucketName("foo")))
            listBuckets     <- s3.listBuckets().flatMap(_.getBuckets)
            listBucketNames <- ZIO.foreach(listBuckets)(_.getName)
          } yield assert(listBucketNames)(equalTo(List("foo")))

        testCase
      }
    ).provideShared(
      ZLocalStackContainer.Settings.withServices(JavaLocalStackContainer.Service.S3),
      ZLocalStackContainer.live,
      s3Layer,
      AwsConfig.configured(),
      commonAwsConfigLayer,
      NettyHttpClient.default
    )

  private def s3Layer: RLayer[LocalStackContainer with AwsConfig, S3] =
    ZLayer.scoped {
      for {
        localstack <- ZIO.service[LocalStackContainer]
        endpoint   <- ZIO.attempt(localstack.container.getEndpointOverride(JavaLocalStackContainer.Service.S3))
        s3         <- S3.scoped(_.endpointOverride(endpoint))
      } yield s3
    }

  private def commonAwsConfigLayer: ULayer[CommonAwsConfig] =
    ZLayer.succeed(
      CommonAwsConfig(
        region = Some(software.amazon.awssdk.regions.Region.US_EAST_1),
        credentialsProvider = DefaultCredentialsProvider.builder().build(),
        endpointOverride = None,
        commonClientConfig = None
      )
    )

}
