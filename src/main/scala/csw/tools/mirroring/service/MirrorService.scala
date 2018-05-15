package csw.tools.mirroring.service

import csw.tools.mirroring.model.{Mirror, Repo}
import gitbucket.core.util.Directory
import org.h2.mvstore.{MVMap, MVStore}
import org.json4s.jackson.Serialization
import org.json4s.{Formats, NoTypeHints}

import scala.collection.JavaConverters.mapAsScalaConcurrentMapConverter
import scala.collection.mutable

class MirrorService {
  private implicit val formats: Formats = Serialization.formats(NoTypeHints)
  private val fileName                  = s"${Directory.GitBucketHome}/kv.mv.db"

  private lazy val store: MVStore                 = MVStore.open(fileName)
  private lazy val mirrors: MVMap[String, String] = store.openMap("mirrors")

  def close(): Unit = store.close()

  def findMirror(repo: Repo): Option[Mirror]   = readMirror(mirrors.get(Repo.makeKey(repo)))
  def deleteMirror(repo: Repo): Option[Mirror] = readMirror(mirrors.remove(Repo.makeKey(repo)))

  def upsert(repo: Repo, mirror: Mirror): Option[Mirror] = {
    val updatedMirror = mirror.withStatusFrom(findMirror(repo))
    readMirror(mirrors.put(Repo.makeKey(repo), Serialization.write(updatedMirror)))
  }

  private def readMirror(string: String): Option[Mirror] = Option(string).map(Serialization.read[Mirror])

  def getAllMirrors: mutable.Map[Repo, Mirror] = mirrors.asScala.map {
    case (k, v) => Repo.readRepo(k).get -> readMirror(v).get
  }

}
