package csw.tools.mirroring.model

import java.util.Date

final case class Mirror(
    name: String,
    remoteUrl: String,
    deployBranch: String,
    enabled: Boolean,
    minutes: String = "15",
    hours: String = "0",
    status: Option[MirrorStatus]
) {
  def withStatus(other: MirrorStatus): Mirror = copy(status = Some(other))
}

final case class MirrorStatus(
    date: Date,
    successful: Boolean,
    error: Option[String]
)
