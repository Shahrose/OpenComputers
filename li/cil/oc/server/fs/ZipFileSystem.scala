package li.cil.oc.server.fs

import java.util.zip.{ZipEntry, ZipFile}
import scala.collection.mutable

// TODO we may want to read in the complete zip file (and keep a cache in the
// factory) to avoid a ton of open real file handles.
class ZipFileSystem(val zip: ZipFile, val root: String) extends InputStreamFileSystem {

  private val directories = mutable.Map.empty[ZipEntry, Array[String]]

  def exists(path: String) = entry(path).isDefined

  def size(path: String) = entry(path).fold(0L)(_.getSize)

  def isDirectory(path: String) = entry(path).exists(_.isDirectory)

  def list(path: String) = entry(path) match {
    case Some(entry) if entry.isDirectory => Some(entries(entry))
    case _ => None
  }

  override def close() {
    super.close()
    zip.close()
    directories.clear()
  }

  protected def openInputStream(path: String, handle: Long) = entry(path).map(entry => zip.getInputStream(entry))

  // ----------------------------------------------------------------------- //

  private def entry(path: String) = Option(zip.getEntry((root + path.replace("\\", "/")).replace("//", "/")))

  private def entries(path: ZipEntry) = directories.get(path).getOrElse {
    val pathName = root + path.getName
    val list = mutable.ArrayBuffer.empty[String]
    val iterator = zip.entries
    while (iterator.hasMoreElements) {
      val child = iterator.nextElement
      if (child.getName.startsWith(pathName)) {
        list += child.getName.substring(pathName.length)
      }
    }
    val children = list.toArray
    directories += path -> children
    children
  }
}
