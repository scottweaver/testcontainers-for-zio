package io.github.scottweaver.zio.testcontainers.k3s

import com.coralogix.zio.k8s.model.apps.v1.{ Deployment, DeploymentSpec }
import com.coralogix.zio.k8s.model.core.v1.{ Container, PodSpec, PodTemplateSpec }
import com.coralogix.zio.k8s.model.pkg.apis.meta.v1.{ LabelSelector, ObjectMeta }

object Data {
  val app    = "app"
  val name   = "display-date"
  val labels = Map(app -> name)

  val replicas = 3

  val deployment = Deployment(
    metadata = ObjectMeta(
      name = "display-date"
    ),
    spec = DeploymentSpec(
      replicas = replicas,
      selector = LabelSelector(matchLabels = labels),
      template = PodTemplateSpec(
        metadata = ObjectMeta(
          labels = labels
        ),
        spec = PodSpec(
          containers = Vector(
            Container(
              name = name,
              image = "busybox",
              imagePullPolicy = "IfNotPresent",
              args = Vector(
                "/bin/sh",
                "-c",
                "while true;do date;sleep 5; done"
              )
            )
          )
        )
      )
    )
  )
}
