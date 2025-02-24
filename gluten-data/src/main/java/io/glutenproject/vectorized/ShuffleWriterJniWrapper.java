/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.glutenproject.vectorized;

import java.io.IOException;

import org.apache.spark.shuffle.utils.CelebornPartitionPusher;

public class ShuffleWriterJniWrapper {

  public ShuffleWriterJniWrapper() throws IOException {
  }

  /**
   * Construct native shuffle writer for shuffled RecordBatch over
   *
   * @param part contains the partitioning parameter needed by native splitter
   * @param bufferSize size of native buffers held by each partition writer
   * @param codec compression codec
   * @param dataFile acquired from spark IndexShuffleBlockResolver
   * @param subDirsPerLocalDir SparkConf spark.diskStore.subDirectories
   * @param localDirs configured local directories where Spark can write files
   * @param preferSpill
   * @param memoryPoolId
   * @return native shuffle writer instance id if created successfully.
   */
  public long make(NativePartitioning part, long offheapPerTask, int bufferSize, String codec,
                   int batchCompressThreshold, String dataFile, int subDirsPerLocalDir,
                   String localDirs, boolean preferSpill, long memoryPoolId, boolean writeSchema,
                   long handle, long taskAttemptId) {
      return nativeMake(part.getShortName(), part.getNumPartitions(),
          offheapPerTask, bufferSize, codec, batchCompressThreshold, dataFile,
          subDirsPerLocalDir, localDirs, preferSpill, memoryPoolId,
          writeSchema, handle, taskAttemptId, 0, null, "gluten");
  }

  /**
   * Construct celeborn native shuffle writer for shuffled RecordBatch over
   *
   * @param part contains the partitioning parameter needed by native shuffle writer
   * @param bufferSize size of native buffers hold by each partition writer
   * @param codec compression codec
   * @param memoryPoolId
   * @return native shuffle writer instance id if created successfully.
   */
  public long makeForCeleborn(NativePartitioning part, long offheapPerTask,
                              int bufferSize, String codec, int batchCompressThreshold,
                              int pushBufferMaxSize, CelebornPartitionPusher pusher,
                              long memoryPoolId, long handle, long taskAttemptId) {
      return nativeMake(part.getShortName(), part.getNumPartitions(),
          offheapPerTask, bufferSize, codec, batchCompressThreshold, null,
          0, null, false, memoryPoolId,
          false, handle, taskAttemptId, pushBufferMaxSize, pusher, "celeborn");
  }

  public native long nativeMake(String shortName, int numPartitions,
                                long offheapPerTask, int bufferSize,
                                String codec, int batchCompressThreshold, String dataFile,
                                int subDirsPerLocalDir, String localDirs, boolean preferSpill,
                                long memoryPoolId, boolean writeSchema,
                                long handle, long taskAttemptId, int pushBufferMaxSize,
                                CelebornPartitionPusher pusher, String shuffleWriterType);

  /**
   * Spill partition data to disk.
   *
   * @param shuffleWriterId shuffle writer instance id
   * @param size expected size to spill (in bytes)
   * @param callBySelf whether the caller is the shuffle shuffle writer itself, true
   * when running out of off-heap memory due to allocations from
   * the evaluator itself
   * @return actual spilled size
   */
  public native long nativeSpill(
      long shuffleWriterId, long size, boolean callBySelf) throws RuntimeException;

  /**
   * Push partition data to celeborn server.
   *
   * @param shuffleWriterId shuffle writer instance id
   * @param size expected size to push (in bytes)
   * @param callBySelf whether the caller is the shuffle shuffle writer itself, true
   * when running out of off-heap memory due to allocations from
   * the evaluator itself
   * @return actual pushed size
   */
  public native long nativePush(
          long shuffleWriterId, long size, boolean callBySelf) throws RuntimeException;

  /**
   * Split one record batch represented by bufAddrs and bufSizes into several batches. The batch is
   * split according to the first column as partition id. During splitting, the data in native
   * buffers will be write to disk when the buffers are full.
   *
   * @param shuffleWriterId shuffle writer instance id
   * @param numRows Rows per batch
   * @param handler handler of Velox Vector
   * @return batch bytes.
   */
  public native long split(
      long shuffleWriterId, int numRows, long handler) throws IOException;

  /**
   * Write the data remained in the buffers hold by native shuffle writer to each partition's
   * temporary file. And stop processing splitting
   *
   * @param shuffleWriterId shuffle writer instance id
   * @return SplitResult
   */
  public native SplitResult stop(long shuffleWriterId) throws IOException;

  /**
   * Release resources associated with designated shuffle writer instance.
   *
   * @param shuffleWriterId shuffle writer instance id
   */
  public native void close(long shuffleWriterId);
}
