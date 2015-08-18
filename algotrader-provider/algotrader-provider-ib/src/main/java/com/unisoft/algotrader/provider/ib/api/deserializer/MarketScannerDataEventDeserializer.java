package com.unisoft.algotrader.provider.ib.api.deserializer;

import ch.aonyx.broker.ib.api.contract.Contract;
import ch.aonyx.broker.ib.api.contract.ContractSpecification;
import ch.aonyx.broker.ib.api.contract.OptionRight;
import ch.aonyx.broker.ib.api.contract.SecurityType;
import ch.aonyx.broker.ib.api.data.scanner.MarketScannerDataEvent;
import com.google.common.collect.Lists;
import com.unisoft.algotrader.model.refdata.Instrument;
import com.unisoft.algotrader.persistence.RefDataStore;
import com.unisoft.algotrader.provider.ib.IBProvider;
import com.unisoft.algotrader.provider.ib.api.IBConstants;
import com.unisoft.algotrader.provider.ib.api.IBSession;
import com.unisoft.algotrader.provider.ib.api.IncomingMessageId;
import com.unisoft.algotrader.provider.ib.api.InputStreamUtils;
import com.unisoft.algotrader.provider.ib.api.model.InstrumentSpecification;
import com.unisoft.algotrader.provider.ib.api.model.MarketScannerData;

import java.io.InputStream;
import java.util.List;

import static ch.aonyx.broker.ib.api.util.InputStreamUtils.readDouble;
import static ch.aonyx.broker.ib.api.util.InputStreamUtils.readInt;
import static ch.aonyx.broker.ib.api.util.InputStreamUtils.readString;
import static com.unisoft.algotrader.provider.ib.api.InputStreamUtils.*;

/**
 * Created by alex on 8/13/15.
 */
public class MarketScannerDataEventDeserializer extends Deserializer {


    private final RefDataStore refDataStore;

    public MarketScannerDataEventDeserializer(int serverCurrentVersion, RefDataStore refDataStore){
        super(IncomingMessageId.MARKET_SCANNER_DATA, serverCurrentVersion);
        this.refDataStore = refDataStore;
    }

    @Override
    public void consumeVersionLess(InputStream inputStream, IBSession ibSession) {
        final int requestId = readInt(inputStream);
        final List<MarketScannerData> marketScannerDataEvents = Lists.newArrayList();
        final int marketScannerDatas = readInt(inputStream);
        for (int i = 0; i < marketScannerDatas; i++) {
            marketScannerDataEvents.add(consumeMarketScannerDataEvent(requestId, inputStream));
        }

        ibSession.onMarketScannerData(requestId, marketScannerDataEvents);
    }

    private MarketScannerData consumeMarketScannerDataEvent(final int requestId, final InputStream inputStream) {

        final InstrumentSpecification contractSpecification = new InstrumentSpecification();
        final int ranking = readInt(inputStream);
        int instid = 0;
        if (getVersion() >= 3) {
            instid = readInt(inputStream);
        }


        final String symbol = InputStreamUtils.readString(inputStream);
        final Instrument.InstType instType = IBConstants.SecType.convert(InputStreamUtils.readString(inputStream));
        final String expString = InputStreamUtils.readString(inputStream);
        final double strike = InputStreamUtils.readDouble(inputStream);
        final Instrument.PutCall putCall = IBConstants.OptionRight.convert(InputStreamUtils.readString(inputStream));
        final String exchange = InputStreamUtils.readString(inputStream);
        final String ccyCode = InputStreamUtils.readString(inputStream);
        final String localSymbol = InputStreamUtils.readString(inputStream);


        contractSpecification.setMarketName(readString(inputStream));
        contractSpecification.setTradingClass(readString(inputStream));
        final String distance = readString(inputStream);
        final String benchmark = readString(inputStream);
        final String projection = readString(inputStream);
        String comboLegDescription = null;
        if (getVersion() >= 2) {
            comboLegDescription = readString(inputStream);
        }

        Instrument instrument = refDataStore.getInstrumentBySymbolAndExchange(IBProvider.PROVIDER_ID, symbol, exchange);
        if (instrument == null){
            throw new RuntimeException("Cannot find instrumnet symbol=" + symbol +", primaryExchange="+exchange);
        }
        contractSpecification.setInstrument(instrument);

        return new MarketScannerData(ranking, contractSpecification, distance, benchmark, projection,
                comboLegDescription);
    }
}