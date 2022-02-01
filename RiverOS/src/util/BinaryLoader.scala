package util

import java.io.DataInputStream
import java.io.FileInputStream
import scala.collection.mutable.ArrayBuffer

class BinaryLoader(filename: String) {
  private val f = new DataInputStream(new FileInputStream(filename))
  private val a = new ArrayBuffer[Long]
  while (f.available() >= 4) {
    a.append(
      Integer.toUnsignedLong(
        f.readUnsignedByte() |
          f.readUnsignedByte() << 8 |
          f.readUnsignedByte() << 16 |
          f.readUnsignedByte() << 24
      )
    )
  }
  f.close()

  val binary = a.toArray
}

object BinaryLoader {
  def apply(
      filename: String
  ): BinaryLoader =
    new BinaryLoader(filename)
}
