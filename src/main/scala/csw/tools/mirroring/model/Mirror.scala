package csw.tools.mirroring.model

import java.util.Date

final case class Mirror(
    remoteUrl: String,
    enabled: Boolean,
    syncIntervalInMinutes: Int,
    status: Option[MirrorStatus]
) {
  def withStatus(other: MirrorStatus): Mirror       = copy(status = Some(other))
  def withStatusFrom(other: Option[Mirror]): Mirror = copy(status = status.orElse(other.flatMap(_.status)))
}

final case class MirrorStatus(
    date: Date,
    successful: Boolean,
    error: Option[String]
)
