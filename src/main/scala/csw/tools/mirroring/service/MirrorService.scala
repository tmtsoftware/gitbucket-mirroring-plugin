package csw.tools.mirroring.service

import csw.tools.mirroring.model.{Mirror, Repo}
import gitbucket.core.util.Directory
import org.h2.mvstore.{MVMap, MVStore}
import org.json4s.jackson.Serialization
import org.json4s.{Formats, NoTypeHints}

class MirrorService {
  private implicit val formats: Formats = Serialization.formats(NoTypeHints)
  private val fileName                  = s"${Directory.GitBucketHome}/kv.mv.db"

  private lazy val store: MVStore                 = MVStore.open(fileName)
  private lazy val mirrors: MVMap[String, String] = store.openMap("mirrors")

  def close(): Unit = store.close()

  def findMirror(repo: Repo): Option[Mirror]             = readMirror(mirrors.get(makeKey(repo)))
  def deleteMirror(repo: Repo): Option[Mirror]           = readMirror(mirrors.remove(makeKey(repo)))
  def upsert(repo: Repo, mirror: Mirror): Option[Mirror] = readMirror(mirrors.put(makeKey(repo), Serialization.write(mirror)))

  private def makeKey(repo: Repo): String = Serialization.write(repo)

  private def readMirror(string: String): Option[Mirror] = Option(string).map(Serialization.read[Mirror])

}
