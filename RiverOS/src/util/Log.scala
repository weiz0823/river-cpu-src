package util

object Log {
  def fail(msg: String): Unit = {
    println(s"[ \033[31mFail\033[0m ] $msg")
  }

  def info(msg: String): Unit = {
    println(s"[ \033[34mInfo\033[0m ] $msg")
  }

  def warn(msg: String): Unit = {
    println(s"[ \033[33mWarn\033[0m ] $msg")
  }

  def done(msg: String): Unit = {
    println(s"[ \033[32mDone\033[0m ] $msg")
  }
}
