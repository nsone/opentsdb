// This file is part of OpenTSDB.
// Copyright (C) 2021  The OpenTSDB Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package net.opentsdb.data;

import com.google.common.collect.Lists;
import com.google.common.primitives.Bytes;
import net.opentsdb.data.types.numeric.NumericType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class MockLowLevelMetricData implements LowLevelMetricData {
  private List<TimeSeriesDatum> data = Lists.newArrayList();
  private int readIndex = -1;
  private TimeSeriesDatumStringId currentId;
  private byte[] flatTags;
  private int[] tagBits = new int[8];
  private int tagBitsIndex;
  private int tagBitsReadIndex;

  public void add(TimeSeriesDatum datum) {
    data.add(datum);
  }

  @Override
  public StringFormat metricFormat() {
    return StringFormat.UTF8_STRING;
  }

  @Override
  public int metricStart() {
    return 0;
  }

  @Override
  public int metricLength() {
    return currentId.metric().getBytes(StandardCharsets.UTF_8).length;
  }

  @Override
  public byte[] metricBuffer() {
    return currentId.metric().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public ValueFormat valueFormat() {
    final TimeSeriesValue<NumericType> val =
            (TimeSeriesValue<NumericType>) data.get(readIndex).value();
    return val.value().isInteger() ? ValueFormat.INTEGER : ValueFormat.DOUBLE;
  }

  @Override
  public long longValue() {
    return ((TimeSeriesValue<NumericType>) data.get(readIndex).value()).value().longValue();
  }

  @Override
  public float floatValue() {
    return (float) ((TimeSeriesValue<NumericType>) data.get(readIndex).value()).value().doubleValue();
  }

  @Override
  public double doubleValue() {
    return ((TimeSeriesValue<NumericType>) data.get(readIndex).value()).value().doubleValue();
  }

  @Override
  public boolean advance() {
    if (++readIndex >= data.size()) {
      return false;
    }
    currentId = (TimeSeriesDatumStringId) data.get(readIndex).id();
    flatTags = null;
    tagBitsIndex = 0;
    tagBitsReadIndex = 0;
    for (Map.Entry<String, String> entry : currentId.tags().entrySet()) {
      if (flatTags == null) {
        flatTags = entry.getKey().getBytes(StandardCharsets.UTF_8);
        tagBits[tagBitsIndex++] = 0;
        tagBits[tagBitsIndex++] = flatTags.length;
      } else {
        if (tagBitsIndex + 2 >= tagBits.length) {
          int[] temp = new int[tagBitsIndex * 2];
          System.arraycopy(tagBits, 0, temp, 0, tagBitsIndex);
          tagBits = temp;
        }
        byte[] tagk = entry.getKey().getBytes(StandardCharsets.UTF_8);
        tagBits[tagBitsIndex++] = flatTags.length;
        tagBits[tagBitsIndex++] = tagk.length;
        flatTags = Bytes.concat(flatTags, tagk);
      }

      byte[] tagv = entry.getValue().getBytes(StandardCharsets.UTF_8);
      tagBits[tagBitsIndex++] = flatTags.length;
      tagBits[tagBitsIndex++] = tagv.length;
      flatTags = Bytes.concat(flatTags, tagv);
    }
    return true;
  }

  @Override
  public boolean hasParsingError() {
    return false;
  }

  @Override
  public String parsingError() {
    return null;
  }

  @Override
  public TimeStamp timestamp() {
    return data.get(readIndex).value().timestamp();
  }

  @Override
  public byte[] tagsBuffer() {
    return flatTags;
  }

  @Override
  public int tagBufferStart() {
    return 0;
  }

  @Override
  public int tagBufferLength() {
    return flatTags.length;
  }

  @Override
  public StringFormat tagsFormat() {
    return StringFormat.UTF8_STRING;
  }

  @Override
  public byte tagDelimiter() {
    return 0;
  }

  @Override
  public int tagSetCount() {
    return tagBitsIndex / 4;
  }

  @Override
  public boolean advanceTagPair() {
    if (tagBitsReadIndex >= tagBitsIndex) {
      return false;
    }
    tagBitsReadIndex += 4;
    return true;
  }

  @Override
  public int tagKeyStart() {
    return tagBits[tagBitsReadIndex - 4];
  }

  @Override
  public int tagKeyLength() {
    return tagBits[tagBitsReadIndex - 3];
  }

  @Override
  public int tagValueStart() {
    return tagBits[tagBitsReadIndex - 2];
  }

  @Override
  public int tagValueLength() {
    return tagBits[tagBitsReadIndex - 1];
  }

  @Override
  public void close() throws IOException {

  }
}