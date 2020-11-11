/*
 * Copyright ISIR, CNRS
 *
 * Contributor(s) :
 *    Guillaume Dubuisson Duplessis <gdubuisson@isir.upmc.fr> (2017)
 *
 * This software is a computer program whose purpose is to implement
 * automatic and generic measures of verbal alignment in dyadic dialogue
 * based on sequential pattern mining at the level of surface of text
 * utterances.
 *
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 *
 */
package dialign

import java.io.{File, FileInputStream, FilenameFilter, PrintWriter}

import scala.collection.mutable
import scala.io.{BufferedSource, Source}

/**
  * Created by gdd on 24/05/17.
  */
package object IO {
  type BytePosition = Long

  def toFile(content: String, filename: String): Unit = {
    val writer = new PrintWriter(new File(filename))
    try {
      writer.write(content)
      writer.flush()
    } catch {
      case e: Exception =>
        System.err.println(e)
    } finally {
      writer.close()
    }
  }

  def withFile(file: File)(op: PrintWriter => Unit) = {
    val writer = new PrintWriter(file)
    try {
      op(writer)
      writer.flush()
    } catch {
      case e: Exception =>
        System.err.println(e)
    } finally {
      writer.close()
    }
  }

  def withFile(filename: String)(op: PrintWriter => Unit) = {
    val writer = new PrintWriter(new File(filename))
    try {
      op(writer)
      writer.flush()
    } catch {
      case e: Exception =>
        System.err.println(e)
    } finally {
      writer.close()
    }
  }

  def withSource[ReturnType](file: File)(op: BufferedSource => ReturnType) = {
    val currentSource = Source.fromFile(file)
    try {
      op(currentSource)
    } catch {
      case e: Exception =>
        throw e
    } finally {
      currentSource.close()
    }
  }

  def loadIntSetFrom(filename: Option[String]): Set[Int] = {
    filename match {
      case Some(name) => loadIntSetFrom(name)
      case None =>
        Set.empty[Int]
    }
  }

  /**
    * Loads a set integer from a file containing one integer per lines
    */
  def loadIntSetFrom(filename: String): Set[Int] = {
    val excluded =
      withSource(filename) {
        source =>
          (for (line <- source.getLines())
            yield (line.toInt)).toSet
      }
    excluded
  }

  def withSource[ReturnType](filename: String)(op: BufferedSource => ReturnType) = {
    val currentSource = Source.fromFile(filename)
    try {
      op(currentSource)
    } catch {
      case e: Exception =>
        throw e
    } finally {
      currentSource.close()
    }
  }

  /**
    * Read a line from a file from a given byte (does not include
    * the final '\n')
    *
    */
  def readLineFrom(
                    filename: String,
                    startingByte: BytePosition): String = {
    // Open the file
    val file = new File(filename)
    // Jump to the byte position
    val inputStream = new FileInputStream(file)
    val skippedByte = inputStream.skip(startingByte)
    assert(skippedByte == startingByte,
      s"Unable to skip $startingByte bytes from the file")

    // Retrieve and return the line
    val builder = new mutable.StringBuilder()
    var content: Int = inputStream.read()
    var endOfLineReached = false
    while ((content != -1) && !endOfLineReached) {
      // convert to char and display it
      val c = content.toChar
      if (c == '\n') {
        endOfLineReached = true
      } else {
        builder.append(c)
      }
      content = inputStream.read()
    }

    inputStream.close()

    builder.result()
  }

  /**
    * Retrieves the list of file from a given directory with constraints on the prefix, suffix and extension
    *
    * @param prefix
    * @param suffix
    * @param extension
    * @return
    */
  def getFilenames(directory: File,
                   prefix: String, suffix: String, extension: String): Seq[File] = {
    // Listing files that respects prefix, suffix and extension
    val filenames = directory.list(new FilenameFilter {
      override def accept(file: File, s: String) =
        s.startsWith(prefix) && s.endsWith(s"$suffix.$extension")
    }).toIndexedSeq

    // Returning files
    val inputDir = directory.getAbsolutePath
    for (filename <- filenames)
      yield new File(s"$inputDir/$filename")
  }
}
