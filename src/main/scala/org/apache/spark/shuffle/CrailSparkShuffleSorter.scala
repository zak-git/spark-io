/*
 * Spark-IO: Fast storage and network I/O for Spark
 *
 * Author: Patrick Stuedi <stu@zurich.ibm.com>
 *
 * Copyright (C) 2016, IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.spark.shuffle

import org.apache.spark.TaskContext
import org.apache.spark.common.Logging
import org.apache.spark.serializer.{CrailDeserializationStream, Serializer}
import org.apache.spark.util.CompletionIterator
import org.apache.spark.util.collection.ExternalSorter

/**
 * Created by stu on 22.08.16.
 */
class CrailSparkShuffleSorter extends CrailShuffleSorter with Logging {

  logInfo("crail shuffle spark sorter")

  override def sort[K, C](context: TaskContext, keyOrd: Ordering[K], ser : Serializer, inputSerializer: CrailDeserializationStream): Iterator[Product2[K, C]] = {
    val sorter =
      new ExternalSorter[K, C, C](context, ordering = Some(keyOrd), serializer = ser)
    val input = inputSerializer.asKeyValueIterator.asInstanceOf[Iterator[Product2[K, C]]]
    sorter.insertAll(input)
    context.taskMetrics().incMemoryBytesSpilled(sorter.memoryBytesSpilled)
    context.taskMetrics().incDiskBytesSpilled(sorter.diskBytesSpilled)
    context.taskMetrics().incPeakExecutionMemory(sorter.peakMemoryUsedBytes)
    CompletionIterator[Product2[K, C], Iterator[Product2[K, C]]](sorter.iterator, sorter.stop())
  }
}
