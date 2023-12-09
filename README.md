# fluor-core

`fluor-core` is the base library that enables writing implicit reactive applications in Scala 3.

## Setup

Add this to your `sbt` build definition:

```scala
libraryDependencies += "io.github.eb2501" %% "fluor-core" % "x.y.z"
```

The latest release is [![Maven Central](https://img.shields.io/maven-central/v/io%2Egithub%2Eeb2501/fluor-core.svg)](https://search.maven.org/artifact/io.github.eb2501/fluor-core)

## Usage

For an example, we'll create a simple `Page` instance where we have a read-write property `x` and another read-only property `y` that depends on `x`:
    
```scala
import eb2501.fluor.core.Page

@main def main: Unit = {
  object MyPage extends Page {
    val x = write { 0 }
    val y = read {
      Thread.sleep(1000)
      x + 2
    }
  }

  println("Starting!")
  println(s"y is set to ${MyPage.y.^}")
  println(s"y is still set to ${MyPage.y.^}")
  MyPage.x.^ = 4
  println(s"y is now set to ${MyPage.y.^}")
}
```

The output should look like this:
    
```
Starting!
y is set to 2        # appears after 1 second
y is still set to 2  # appears immediately
y is now set to 6    # appears after 1 second
```

This example illustrates that `y` is _perfectly cached_ as it is holding its results until a relevant change happens.

## Documentation

For more information, please check out the [manual](docs/manual.md).
