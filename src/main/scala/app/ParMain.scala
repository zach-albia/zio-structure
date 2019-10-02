package app

import zio.clock.nanoTime
import zio.console.putStrLn
import zio.stream._
import zio.{ZIO, App}

object ParMain extends App {

  override def run(args: List[String]): ZIO[Environment, Nothing, Int] = {
    val topNum      = 360000000L
    val concurrency = Runtime.getRuntime.availableProcessors()
    val segmentSize = topNum / concurrency
    val million     = 1000000
    val oneToTopNum = 1L to topNum
    val longs       = Stream.fromIterable(oneToTopNum) // for single-threaded
    val longsByConcurrency = // for multi-threaded
      Stream.unfold(1L)(s => {
        val max = s + segmentSize
        if (max - 1 <= topNum) Some((s until max, max))
        else None
      })
    val sumSink = Sink.foldLeft[Long, Long](0)(_ + _)
    for {
      // single-threaded
      start1   <- nanoTime
      sumCount <- longs.run(sumSink)
      end1     <- nanoTime
      time     = s"single-threaded time in ms: ${(end1 - start1) / million.toDouble}"
      _        <- putStrLn(time)
      _        <- putStrLn(s"sum is $sumCount")
      // multi-threaded
      start2 <- nanoTime
      sumCount2 <- longsByConcurrency
                    .mapMParUnordered(concurrency)(s =>
                      ZIO.succeed(s.reduce(_ + _)))
                    .run(sumSink)
      end2 <- nanoTime
      time = s"$concurrency threads multithreaded time in ms: ${(end2 -
        start2) /
        million.toDouble}"
      _ <- putStrLn(time)
      _ <- putStrLn(s"sum is $sumCount2")
    } yield 0 // exitCode
  }
}
