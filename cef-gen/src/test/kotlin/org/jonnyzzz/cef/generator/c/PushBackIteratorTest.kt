package org.jonnyzzz.cef.generator.c

import org.junit.Assert
import org.junit.Test


class PushBackIteratorTest {

  @Test
  fun test_simple() {

    val pb = listOf("a", "b", "c").iterator().asPushBack()

    Assert.assertEquals(pb.next(), "a")
    pb.pushBack("Q")
    Assert.assertEquals(pb.next(), "Q")
    Assert.assertEquals(pb.next(), "b")

    pb.pushBack("x")
    pb.pushBack("y")
    pb.pushBack("z")
    Assert.assertEquals(pb.next(), "z")
    Assert.assertEquals(pb.next(), "y")
    Assert.assertEquals(pb.next(), "x")

    Assert.assertEquals(pb.next(), "c")

    Assert.assertFalse(pb.hasNext())
  }


}