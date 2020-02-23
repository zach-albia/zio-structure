import zio.Has

package object domain {
  type FooRepository = Has[FooRepository.Service]
  type FooService = Has[FooService.Service]
}
