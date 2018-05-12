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

  def findMirror(repo: Repo): Option[Mirror]   = readMirror(mirrors.get(makeKey(repo)))
  def deleteMirror(repo: Repo): Option[Mirror] = readMirror(mirrors.remove(makeKey(repo)))

  def upsert(repo: Repo, mirror: Mirror): Option[Mirror] = findMirror(repo) match {
    case None =>
      readMirror(mirrors.put(makeKey(repo), Serialization.write(mirror)))
    case Some(oldMirror) =>
      val finalMirror = if (mirror.status.isEmpty) mirror.copy(status = oldMirror.status) else mirror
      readMirror(mirrors.put(makeKey(repo), Serialization.write(finalMirror)))
  }

  def makeKey(repo: Repo): String             = Serialization.write(repo)
  def readRepo(repoKey: String): Option[Repo] = Option(repoKey).map(Serialization.read[Repo])

  private def readMirror(string: String): Option[Mirror] = Option(string).map(Serialization.read[Mirror])

  def getAllMirrors: mutable.Map[Repo, Mirror] = mirrors.asScala.map {
    case (k, v) => readRepo(k).get -> readMirror(v).get
  }

}
