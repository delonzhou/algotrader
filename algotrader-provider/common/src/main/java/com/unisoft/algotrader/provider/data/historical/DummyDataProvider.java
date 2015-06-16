package com.unisoft.algotrader.provider.data.historical;

import com.lmax.disruptor.RingBuffer;
import com.unisoft.algotrader.event.EventBusManager;
import com.unisoft.algotrader.event.data.Bar;
import com.unisoft.algotrader.event.data.MarketDataContainer;
import com.unisoft.algotrader.provider.SubscriptionKey;
import com.univocity.parsers.csv.CsvParserSettings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by alex on 5/19/15.
 */
public class DummyDataProvider implements HistoricalDataProvider {

    private final RingBuffer<MarketDataContainer> marketDataRB;

    private final static long DAY_TO_MS = TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS);
    private final static int DAILY_SIZE = 60 * 60 * 24;

    public DummyDataProvider() {
        this(EventBusManager.INSTANCE.rawMarketDataRB);
    }

    public DummyDataProvider(RingBuffer<MarketDataContainer> marketDataRB) {
        this.marketDataRB = marketDataRB;
    }

    public static SimpleDateFormat FORMAT2 = new SimpleDateFormat("yyyyMMdd");


    @Override
    public void subscribe(SubscriptionKey subscriptionKey, Date fromDate, Date toDate) {
        CsvParserSettings settings = new CsvParserSettings();

        settings.getFormat().setLineSeparator("\n");
        settings.getFormat().setDelimiter(',');
        settings.setHeaderExtractionEnabled(true);

        long dateTime = fromDate.getTime();
        long toDateTime = toDate.getTime();

        int count = 0;
        while (dateTime < toDateTime) {
            long sequence = marketDataRB.next();

            MarketDataContainer event = marketDataRB.get(sequence);
            event.reset();
            event.dateTime = dateTime;
            event.instId = subscriptionKey.instId;
            event.bitset.set(MarketDataContainer.BAR_BIT);

            Bar bar = event.bar;
            bar.instId = subscriptionKey.instId;
            bar.dateTime = dateTime;
            bar.size = subscriptionKey.frequency;
            bar.high = 1000 + count;
            bar.low = 800 + count;
            bar.open = 900 + count;
            bar.close = 950 + count;
            bar.volume = 0;
            bar.openInt = 0;

            marketDataRB.publish(sequence);

            dateTime += DAY_TO_MS;
            count++;
        }
    }

    @Override
    public String providerId() {
        return "CSV";
    }

    @Override
    public boolean connected(){
        return true;
    }

    public static void main(String [] args){
        DummyDataProvider provider = new DummyDataProvider();
        provider.subscribe(SubscriptionKey.createDailySubscriptionKey("HSI"), 20110101, 20141231);
    }
}
