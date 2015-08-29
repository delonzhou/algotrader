package com.unisoft.algotrader.provider.ib.api.deserializer;

import com.unisoft.algotrader.model.event.execution.ExecutionReport;
import com.unisoft.algotrader.model.refdata.Instrument;
import com.unisoft.algotrader.provider.ib.IBProvider;
import com.unisoft.algotrader.provider.ib.api.IBConstants;
import com.unisoft.algotrader.provider.ib.api.IBSocket;
import com.unisoft.algotrader.provider.ib.api.IncomingMessageId;
import com.unisoft.algotrader.provider.ib.api.InputStreamUtils;

import java.io.InputStream;

import static com.unisoft.algotrader.provider.ib.api.InputStreamUtils.*;


/**
 * Created by alex on 8/13/15.
 */
public class ExecutionReportDeserializer extends Deserializer {


    public ExecutionReportDeserializer(){
        super(IncomingMessageId.EXECUTION_REPORT);
    }

    @Override
    public void consumeVersionLess(final int version, final InputStream inputStream, final IBProvider ibProvider) {
        int requestId = -1;
        if (version >= 7) {
            requestId = readInt(inputStream);
        }
        final int orderId = readInt(inputStream);
        final Instrument instrument = parseInstrument(version, inputStream, ibProvider);
        final ExecutionReport executionReport = consumeExecutionReport(version, inputStream, orderId);
        ibProvider.onExecutionReportEvent(requestId, instrument, executionReport);
    }

    protected Instrument parseInstrument(final int version, final InputStream inputStream, final IBProvider ibProvider) {
        final int instId = (version >= 5)? InputStreamUtils.readInt(inputStream) : 0;
        final String symbol = readString(inputStream);
        final Instrument.InstType instType = IBConstants.SecType.convert(readString(inputStream));
        final String expString = readString(inputStream);
        final double strike = readDouble(inputStream);
        final Instrument.PutCall putCall = IBConstants.OptionRight.convert(readString(inputStream));
        final String multiplier = (version >= 9)? readString(inputStream) : null;
        final String exchange = readString(inputStream);
        final String ccyCode = readString(inputStream);
        final String localSymbol = readString(inputStream);
        final String tradingClass = (version >= 10)? readString(inputStream) : null;

        Instrument instrument = ibProvider.getRefDataStore().getInstrumentBySymbolAndExchange(IBProvider.PROVIDER_ID, symbol, exchange);
        if (instrument == null){
            throw new RuntimeException("Cannot find instrumnet symbol=" + symbol +", primaryExchange="+exchange);
        }

        return instrument;
    }

    protected ExecutionReport consumeExecutionReport(final int version, final InputStream inputStream, final int orderId){
        ExecutionReport executionReport = new ExecutionReport();
        executionReport.setOrderId(orderId);
        //TODO string to execID mapping?
        executionReport.setExecId(Long.parseLong(readString(inputStream)));
        String time = readString(inputStream);
        String account = readString(inputStream);
        String exchange = readString(inputStream);
        executionReport.setSide(IBConstants.IBSide.convert(readString(inputStream)));
        executionReport.setLastQty(readInt(inputStream));
        executionReport.setLastPrice(readDouble(inputStream));
        if (version >= 2) {
            int permanentId = readInt(inputStream);
        }
        if (version >= 3) {
            int clientId = readInt(inputStream);
        }
        if (version >= 4) {
            int liquidation = readInt(inputStream);
        }
        if (version >= 6) {
            executionReport.setFilledQty(readInt(inputStream));
            executionReport.setAvgPrice(readDouble(inputStream));
        }
        if (version >= 8) {
            String orderRef = readString(inputStream);
        }
        if (version >= 9) {
            String economicValueRule = readString(inputStream);
            double economicValueMultiplier = readDouble(inputStream);
        }
        return executionReport;
    }
}