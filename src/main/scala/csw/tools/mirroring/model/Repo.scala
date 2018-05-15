package csw.tools.mirroring.model

import gitbucket.core.service.RepositoryService.RepositoryInfo
import org.json4s.{Formats, NoTypeHints}
import org.json4s.jackson.Serialization

import scala.language.implicitConversions

case class Repo(owner: String, name: String)

object Repo {
  implicit def fromRepositoryInfo(repositoryInfo: RepositoryInfo): Repo = Repo(repositoryInfo.owner, repositoryInfo.name)
  private implicit val formats: Formats                                 = Serialization.formats(NoTypeHints)

  def toJsonString(repo: Repo): String           = Serialization.write(repo)
  def fromJsonString(json: String): Option[Repo] = Option(json).map(Serialization.read[Repo])
}
