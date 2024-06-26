package com.github.dedis.popstellar.testutils

import org.mockito.ArgumentCaptor
import org.mockito.Mockito

object MockitoKotlinHelpers {
  /**
   * Returns Mockito.eq() as nullable type to avoid java.lang.IllegalStateException when null is
   * returned.
   *
   * Generic T is nullable because implicitly bounded by Any?.
   */
  fun <T> eq(obj: T): T = Mockito.eq<T>(obj)

  /**
   * Returns Mockito.any() as nullable type to avoid java.lang.IllegalStateException when null is
   * returned.
   */
  fun <T> any(): T = Mockito.any<T>()

  fun <T> any(entity: Class<T>): T = Mockito.any<T>(entity)

  inline fun <reified T> argumentCaptor(): ArgumentCaptor<T> =
    ArgumentCaptor.forClass(T::class.java)

  /**
   * Returns ArgumentCaptor.capture() as nullable type to avoid java.lang.IllegalStateException when
   * null is returned.
   */
  fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
}
