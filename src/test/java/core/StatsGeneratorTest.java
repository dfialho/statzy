package core;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static core.DataRecord.dataRecord;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class StatsGeneratorTest {

    private StatisticsGenerator statisticsGenerator = new StatisticsGenerator();

    @Mock
    private DataFileReader dataFileReaderStub;

    @Mock
    private DataFileWriter dataFileWriterMock;

    private void setupDataFileReader(List<DataRecord> records) throws ParseException {

        Iterator<DataRecord> iterator = records.iterator();

        when(dataFileReaderStub.read()).thenAnswer(invocationOnMock -> {
            if (iterator.hasNext()) {
                return iterator.next();
            } else {
                return null;
            }
        });

        int expectedValueCount = 0;
        if (!records.isEmpty()) {
            expectedValueCount = records.get(0).getDataValues().size();
        }

        when(dataFileReaderStub.getExpectedValueCount()).thenReturn(expectedValueCount);
    }

    private static List<Double> means(Double... means) {
        return Arrays.asList(means);
    }

    private static List<Double> stdevs(Double... stdevs) {
        return Arrays.asList(stdevs);
    }

    @Test
    public void process_3RecordsWithinTheFirstPeriodOf5Seconds_OutputsASinglePeriodWithCorrectStats() throws Exception {
        List<DataRecord> records = Arrays.asList(
                dataRecord(Timestamp.of(2016, 10, 10, 10, 10, 0), 5.5),
                dataRecord(Timestamp.of(2016, 10, 10, 10, 10, 2), 4.5),
                dataRecord(Timestamp.of(2016, 10, 10, 10, 10, 4), 6.5)
        );
        setupDataFileReader(records);

        statisticsGenerator.process(dataFileReaderStub, dataFileWriterMock, Period.of(5, Unit.SECONDS));

        verify(dataFileWriterMock, times(1))
                .write(Timestamp.of(2016, 10, 10, 10, 10, 0), 3, means(5.5), stdevs(1.0));
    }

    @Test
    public void
    process_1RecordsInTheFirstPeriodOf5SecondsAnd2InTheSecondPeriod_OutputsTwoPeriods() throws Exception {
        List<DataRecord> records = Arrays.asList(
                dataRecord(Timestamp.of(2016, 10, 10, 10, 10, 0), 5.5),
                dataRecord(Timestamp.of(2016, 10, 10, 10, 10, 5), 4.5),
                dataRecord(Timestamp.of(2016, 10, 10, 10, 10, 7), 7.5)
        );
        setupDataFileReader(records);

        statisticsGenerator.process(dataFileReaderStub, dataFileWriterMock, Period.of(5, Unit.SECONDS));

        verify(dataFileWriterMock)
                .write(Timestamp.of(2016, 10, 10, 10, 10, 0), 1, means(5.5), stdevs(0.0));
        verify(dataFileWriterMock)
                .write(Timestamp.of(2016, 10, 10, 10, 10, 5), 2, means(6.0), stdevs(2.1213203435596424));
    }

    @Test
    public void
    process_1RecordsInTheFirstPeriodOf5SecondsAnd2InTheThirdPeriod_OutputsThreePeriods() throws Exception {
        List<DataRecord> records = Arrays.asList(
                dataRecord(Timestamp.of(2016, 10, 10, 10, 10, 0), 5.5),
                dataRecord(Timestamp.of(2016, 10, 10, 10, 10, 10), 4.5),
                dataRecord(Timestamp.of(2016, 10, 10, 10, 10, 14), 7.5)
        );
        setupDataFileReader(records);

        statisticsGenerator.process(dataFileReaderStub, dataFileWriterMock, Period.of(5, Unit.SECONDS));

        verify(dataFileWriterMock)
                .write(Timestamp.of(2016, 10, 10, 10, 10, 0), 1, means(5.5), stdevs(0.0));
        verify(dataFileWriterMock)
                .write(Timestamp.of(2016, 10, 10, 10, 10, 5), 0, means(Double.NaN), stdevs(Double.NaN));
        verify(dataFileWriterMock)
                .write(Timestamp.of(2016, 10, 10, 10, 10, 10), 2, means(6.0), stdevs(2.1213203435596424));
    }

    @Test
    public void
    process_0Records_DoesNotOutputAnything() throws Exception {
        List<DataRecord> records = Collections.emptyList();
        setupDataFileReader(records);

        statisticsGenerator.process(dataFileReaderStub, dataFileWriterMock, Period.of(5, Unit.SECONDS));

        verify(dataFileWriterMock, never()).write(any(), anyInt(), anyList(), anyList());
    }

    @Test
    public void
    process_1Record_OutputsASinglePeriod() throws Exception {
        List<DataRecord> records = Collections.singletonList(
                dataRecord(Timestamp.of(2016, 10, 10, 10, 1, 10), 5.5)
        );
        setupDataFileReader(records);

        statisticsGenerator.process(dataFileReaderStub, dataFileWriterMock, Period.of(5, Unit.MINUTES));

        verify(dataFileWriterMock)
                .write(Timestamp.of(2016, 10, 10, 10, 1, 0), 1, means(5.5), stdevs(0.0));
    }

}