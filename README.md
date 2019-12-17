# zio-structure
Just another ZIO demo, with an emphasis on service-level transactions using
[zio-saga](https://github.com/VladKopanev/zio-saga). I have explored three
ways of doing this, namely:
  - tagless-final encoding: which I found to be annoying, and realized later on
    to have unprincipled, leaky abstractions because I was depending on the
    repository implementation to do the transactions;
  - plain old programming to an interface: didn't quite work out for me because
    I've had to duplicate FooService code (not to mention leaky abstractions); and,
  - zio-saga: which I just recently realized is the implementation with _actual_
    service-level transactions and is purely encoded in the domain layer. This
    is the domain-driven way to go!

## How to run
`sbt run`

There are three mains to choose from:
  - `InMemoryMain` - Runs `Program` with an in-memory standard lib `Map` repository
  - `SlickMain` - Runs `Program` with an in-memory Slick h2 repository
  - `ParMain` - `ZStream`'s high-level concurrency demo, adding streams of `Long`s in single vs multi-threaded mode

## Service-level transactions
See [`mergeFoos`](https://github.com/zach-albia/zio-structure/blob/master/src/main/scala/domain/FooService.scala#L55) for an example.
