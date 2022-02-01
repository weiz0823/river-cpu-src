package util

import scala.collection.mutable.Queue
import scala.io.StdIn._
import scala.collection.mutable.ArrayBuffer

import java.io.{BufferedReader, InputStreamReader}
import java.net.ServerSocket
import java.net.Socket
import java.io.InputStream
import java.io.OutputStream

case class IOTransition(
    action: Char, // 'r' 'w'
    mode: Char, // 'i' 'm'
    addr: Long,
    data: Long
)

class IOControl(
    val extRam: Array[Long],
    val baseRam: Array[Long],
    val port: Int
) {
  private def byteMask(byteEnable: Int): Long = {
    var mask: Long = 0
    if ((byteEnable & 1) != 0) mask |= 0xffL
    if ((byteEnable & 2) != 0) mask |= 0xffL << 8
    if ((byteEnable & 4) != 0) mask |= 0xffL << 16
    if ((byteEnable & 8) != 0) mask |= 0xffL << 24
    mask
  }

  // 8 * 1024 * 1024 = 8 Mb, each element is 4 byte, so total 8 * 1024 * 1024 / 4 element
  val uniformData: Array[Long] = Array.fill(8 * 1024 * 1024 / 4)(0)

  val offset: Long = 0x80000000L

  val clintModel = new CoreLocalIntModel(baseAddr = 0x2000000)

  // copy data to uniformData
  extRam.copyToArray(uniformData)
  baseRam.copyToArray(uniformData, 4 * 1024 * 1024 / 4)

  private def getSram(addr: Long, byteEnable: Int): Long = {
    val idx = (addr - offset).toInt / 4
    val mask = byteMask(byteEnable)
    if (0 <= idx && idx < uniformData.length) {
      uniformData(idx) & mask
    } else {
      0
    }
  }

  private def getUart(addr: Long, byteEnable: Int): Long = {
    addr match {
      case 0x10000004 => {
        if(inputStream.available() > 0) {
          (0x01 | 0x20) << 8
        } else {
          0x20 << 8
        }
      }
      case 0x10000000 => {
        inputStream.read().toLong
      }
      case _ => 0
    }
  }

  private def setSram(addr: Long, byteEnable: Int, value: Long): Unit = {
    val idx = (addr - offset).toInt / 4
    val mask = byteMask(byteEnable)
    if (0 <= idx && idx < uniformData.length) {
      var word = uniformData(idx) // take the word out
      word &= ~mask
      word |= value & mask
      uniformData(idx) = word
    }
  }

  val sb: StringBuilder = new StringBuilder()

  private def setUart(addr: Long, byteEnable: Int, value: Long): Unit = {
    if (addr == 0x10000000) {
      if (value.toChar == '\n') {
        outputStream.write(sb.append('\n').toString().getBytes())
        sb.clear()
      } else {
        sb.append(value.toChar)
      }
    }
  }

  private def get(addr: Long, byteEnable: Int): Long = {
    addr match {
      case i if offset <= i && i < offset + 8 * 1024 * 1024 =>
        getSram(addr, byteEnable)
      case i if i >= 0x10000000 && i <= 0x10000007 => getUart(addr, byteEnable)
      case i if clintModel.isValid(i) => clintModel.get(addr, byteEnable)
      case _                          => 0
    }
  }

  def getInst(addr: Long, byteEnable: Int): Long = {
    val data = get(addr, byteEnable)
    transition += IOTransition('r', 'i', addr, data)
    data
  }

  def getData(addr: Long, byteEnable: Int): Long = {
    val data = get(addr, byteEnable)
    transition += IOTransition('r', 'm', addr, data)
    data
  }

  def set(addr: Long, byteEnable: Int, value: Long): Unit = {
    addr match {
      case i if offset <= i && i < offset + 8 * 1024 * 1024 =>
        setSram(addr, byteEnable, value)
      case i if i >= 0x10000000 && i <= 0x10000007 =>
        setUart(addr, byteEnable, value)
      case i if clintModel.isValid(i) => clintModel.set(addr, byteEnable, value)
    }
    transition += IOTransition('w', 'm', addr, value)
  }

  def isValid(addr: Long): Boolean = {
    offset <= addr && addr < offset + 8 * 1024 * 1024
  }

  def clockTick(): Unit = {
    clintModel.clockTick()
  }

  val transition: ArrayBuffer[IOTransition] = ArrayBuffer()

  val serverSocket = new ServerSocket(port)

  val socket : Socket = serverSocket.accept()

  val inputStream : InputStream = socket.getInputStream()

  val outputStream : OutputStream = socket.getOutputStream()

  def close(): Unit = {
    socket.close()
  }
}

object IOControl {
  def apply(extRam: Array[Long], baseRam: Array[Long], port: Int): IOControl =
    new IOControl(extRam, baseRam, port)

  def apply(extRam: String, baseRam: String, port: Int) =
    new IOControl(
      BinaryLoader(extRam).binary,
      BinaryLoader(baseRam).binary,
      port
    )
}
