package csw.tools.mirroring.model

import java.util.Date

final case class Mirror(
    remoteUrl: String,
    enabled: Boolean,
    syncInterval: Int = 2,
    status: Option[MirrorStatus]
) {
  def withStatus(other: MirrorStatus): Mirror = copy(status = Some(other))
}

final case class MirrorStatus(
    date: Date,
    successful: Boolean,
    error: Option[String]
)
