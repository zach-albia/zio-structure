# zio-structure
Just another way of structuring ZIO apps, with an emphasis on service-level transactions.

## How to run
`sbt run`

There are three mains to choose from:
  - `InMemoryMain` - Runs `Program` with an in-memory standard lib `Map` repository
  - `SlickMain` - Runs `Program` with an in-memory Slick h2 repository
  - `ParMain` - `ZStream`'s high-level concurrency demo, adding streams of `Long`s in single vs multi-threaded mode

## Service-level transactions
See [`mergeFoos`](https://github.com/zach-albia/zio-structure/blob/master/src/main/scala/domain/FooService.scala#L55) for an example.
