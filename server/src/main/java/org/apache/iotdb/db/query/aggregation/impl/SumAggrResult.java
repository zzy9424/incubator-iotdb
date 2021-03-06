/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.db.query.aggregation.impl;

import java.io.IOException;
import org.apache.iotdb.db.query.aggregation.AggregateResult;
import org.apache.iotdb.db.query.reader.series.IReaderByTimestamp;
import org.apache.iotdb.tsfile.file.metadata.enums.TSDataType;
import org.apache.iotdb.tsfile.file.metadata.statistics.Statistics;
import org.apache.iotdb.tsfile.read.common.BatchData;

public class SumAggrResult extends AggregateResult {

  private TSDataType seriesDataType;

  public SumAggrResult(TSDataType seriesDataType) {
    super(TSDataType.DOUBLE);
    this.seriesDataType = seriesDataType;
    reset();
    setDoubleValue(0.0);
  }

  @Override
  public Double getResult() {
    return getDoubleValue();
  }

  @Override
  public void updateResultFromStatistics(Statistics statistics) {
    double preValue = getDoubleValue();
    preValue += statistics.getSumValue();
    setDoubleValue(preValue);
  }

  @Override
  public void updateResultFromPageData(BatchData dataInThisPage) throws IOException {
    updateResultFromPageData(dataInThisPage, Long.MAX_VALUE);
  }

  @Override
  public void updateResultFromPageData(BatchData dataInThisPage, long bound) throws IOException {
    while (dataInThisPage.hasCurrent()) {
      if (dataInThisPage.currentTime() >= bound) {
        break;
      }
      updateSum(seriesDataType, dataInThisPage.currentValue());
      dataInThisPage.next();
    }
  }

  @Override
  public void updateResultUsingTimestamps(long[] timestamps, int length,
      IReaderByTimestamp dataReader) throws IOException {
    for (int i = 0; i < length; i++) {
      Object value = dataReader.getValueInTimestamp(timestamps[i]);
      if (value != null) {
        updateSum(seriesDataType, value);
      }
    }
  }

  private void updateSum(TSDataType type, Object sumVal) throws IOException {
    double preValue = getDoubleValue();
    switch (type) {
      case INT32:
        preValue += (int) sumVal;
        break;
      case INT64:
        preValue += (long) sumVal;
        break;
      case FLOAT:
        preValue += (float) sumVal;
        break;
      case DOUBLE:
        preValue += (double) sumVal;
        break;
      case TEXT:
      case BOOLEAN:
      default:
        throw new IOException(
            String.format("Unsupported data type in aggregation SUM : %s", type));
    }
    setDoubleValue(preValue);
  }

  @Override
  public boolean isCalculatedAggregationResult() {
    return false;
  }
}
