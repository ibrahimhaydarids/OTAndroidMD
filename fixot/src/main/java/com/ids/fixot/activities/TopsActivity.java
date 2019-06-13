package com.ids.fixot.activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ids.fixot.Actions;
import com.ids.fixot.AppService;
import com.ids.fixot.ConnectionRequests;
import com.ids.fixot.GlobalFunctions;
import com.ids.fixot.LocalUtils;
import com.ids.fixot.MarketStatusReceiver.MarketStatusListener;
import com.ids.fixot.MarketStatusReceiver.marketStatusReceiver;
import com.ids.fixot.MyApplication;
import com.ids.fixot.R;
import com.ids.fixot.adapters.InstrumentsRecyclerAdapter;
import com.ids.fixot.adapters.MarketsSpinnerAdapter;
import com.ids.fixot.adapters.StockQuotationRecyclerAdapter;
import com.ids.fixot.adapters.TopsRecyclerAdapter;
import com.ids.fixot.fragments.TopGainersFragment;
import com.ids.fixot.fragments.TopLosersFragment;
import com.ids.fixot.model.Instrument;
import com.ids.fixot.model.Stock;
import com.ids.fixot.enums.enums.TradingSession;
import com.ids.fixot.model.StockQuotation;
import com.ids.fixot.model.TimeSale;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class TopsActivity extends AppCompatActivity implements InstrumentsRecyclerAdapter.RecyclerViewOnItemClickListener,MarketStatusListener {

    private BroadcastReceiver receiver;

    NestedScrollView nsScroll;
    RelativeLayout rootLayout;
    TabLayout tlTabs;
    SwipeRefreshLayout swipeContainer;
    GetTops getTops;
    String toolbarTitle = "";

    //<editor-fold desc ="Lists">
    LinearLayout llGainersHeader, llLosersHeader, llTradedHeader, llTradesHeader, llVolumeHeader;
    RecyclerView rvTopGainers, rvTopLosers, rvTopTraded, rvTopTrades, rvTopVolume;
    LinearLayoutManager llmGainers, llmLosers, llTraded, llTrades, llVolume;
    TopsRecyclerAdapter gainersAdapter, losersAdapter, tradedAdapter, tradesAdapter, volumeAdapter;
    ArrayList<Stock> allStocks = new ArrayList<>();
    ArrayList<Stock> tmpStocks = new ArrayList<>();

    TextView tvGainersTrading, tvGainersSymbol, tvGainersChange, tvGainersChangePercent, tvGainersPrice;
    TextView tvLosersTrading, tvLosersSymbol, tvLosersChange, tvLosersChangePercent, tvLosersPrice;
    TextView tvTradedTrading, tvTradedSymbol, tvTradedChange, tvTradedChangePercent, tvTradedPrice;
    TextView tvTradesTrading, tvTradesSymbol, tvTradesChange, tvTradesChangePercent, tvTradesPrice;
    TextView tvVolumeTrading, tvVolumeSymbol, tvVolumeChange, tvVolumeChangePercent, tvVolumePrice;

    //filter header
    RecyclerView rvInstruments;
    Spinner spMarkets;
    MarketsSpinnerAdapter marketsSpinnerAdapter;
    ArrayList<TradingSession> AllMarkets = new ArrayList<>();
    TradingSession selectMarket = TradingSession.All ;
    ArrayList<Instrument> marketInstruments = new ArrayList<>();
    ArrayList<Instrument> allInstruments = new ArrayList<>();
    Boolean isSelectInstrument = false;
    InstrumentsRecyclerAdapter instrumentsRecyclerAdapter;
    Instrument selectedInstrument = new Instrument();
    String instrumentId = "";
    GetInstruments getInstruments;






    //</editor-fold>

    private boolean started = false;

    public TopsActivity() {
        LocalUtils.updateConfig(this);
    }

    @Override
    public void refreshMarketTime(String status,String time,Integer color){

        final TextView marketstatustxt = findViewById(R.id.market_state_value_textview);
        final LinearLayout llmarketstatus = findViewById(R.id.ll_market_state);
        final TextView markettime =  findViewById(R.id.market_time_value_textview);

        marketstatustxt.setText(status);
        markettime.setText(time);
        llmarketstatus.setBackground(ContextCompat.getDrawable(this,color));

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
Log.wtf("bob","test");
        receiver = new marketStatusReceiver(this);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(AppService.ACTION_MARKET_SERVICE));

        Actions.setActivityTheme(this);
        Actions.setLocal(MyApplication.lang, this);
        setContentView(R.layout.activity_all_tops);
        Actions.showHideFooter(this);
        Actions.initializeBugsTracking(this);

        Actions.initializeToolBar(getResources().getString(R.string.tops), this);

        for(int i=0;i<MyApplication.instruments.size();i++){
            Log.wtf("instruments",MyApplication.instruments.get(i).getInstrumentName());
        }


        started = true;


      /*  allInstruments.addAll(MyApplication.instruments);
        marketInstruments = allInstruments;
*/

        findViews();



        MyApplication.instrumentId = "";
        getInstruments = new GetInstruments();

        Log.wtf("onCreate","MyApplication.instruments count: " + MyApplication.instruments.size());
        if (MyApplication.instruments.size() < 2) {

            Actions.initializeInstruments(this);
            getInstruments.executeOnExecutor(MyApplication.threadPoolExecutor);
        } else {

            allInstruments.clear();

            if(!MyApplication.isOTC) {
                for (int i = 1; i < AllMarkets.size(); i++) {
                    allInstruments.addAll(Actions.filterInstrumentsByMarketSegmentID(MyApplication.instruments, AllMarkets.get(i).getValue()));
                }
            }else{
                allInstruments.addAll(MyApplication.instruments);
            }


        }






        if (Actions.isNetworkAvailable(this)){

            getTops = new GetTops();
            getTops.executeOnExecutor(MyApplication.threadPoolExecutor);
        }else{

            Actions.CreateDialog(this, getString(R.string.no_net), false, false);
        }

        Actions.overrideFonts(this, rootLayout, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            Runtime.getRuntime().gc();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findViews() {

        nsScroll = findViewById(R.id.nsScroll);
        rootLayout = findViewById(R.id.rootLayout);
        tlTabs = findViewById(R.id.tlTabs);

        swipeContainer = findViewById(R.id.swipeContainer);

        swipeContainer.setOnRefreshListener(() -> {

            allStocks.clear();

           notifyadapters();

            getTops = new GetTops();
            getTops.execute();
            swipeContainer.setRefreshing(false);
        });


        initializeHeaders();

        initializeRecyclers();
        initializeFilters();
    }

    private void initializeRecyclers() {

        rvTopGainers = findViewById(R.id.rvTopGainers);
        llmGainers = new LinearLayoutManager(this);
        rvTopGainers.setLayoutManager(llmGainers);
        rvTopGainers.setNestedScrollingEnabled(false);

        rvTopLosers = findViewById(R.id.rvTopLosers);
        llmLosers = new LinearLayoutManager(this);
        rvTopLosers.setLayoutManager(llmLosers);
        rvTopLosers.setNestedScrollingEnabled(false);

        rvTopTraded = findViewById(R.id.rvTopTraded);
        llTraded = new LinearLayoutManager(this);
        rvTopTraded.setLayoutManager(llTraded);
        rvTopTraded.setNestedScrollingEnabled(false);

        rvTopTrades = findViewById(R.id.rvTopTrades);
        llTrades = new LinearLayoutManager(this);
        rvTopTrades.setLayoutManager(llTrades);
        rvTopTrades.setNestedScrollingEnabled(false);

        rvTopVolume = findViewById(R.id.rvTopVolume);
        llVolume = new LinearLayoutManager(this);
        rvTopVolume.setLayoutManager(llVolume);
        rvTopVolume.setNestedScrollingEnabled(false);
    }

    private void initializeHeaders() {

        llGainersHeader = findViewById(R.id.llGainersHeader);
        tvGainersSymbol = llGainersHeader.findViewById(R.id.tvSymbol);
        tvGainersPrice = llGainersHeader.findViewById(R.id.tvLast);
        tvGainersTrading = llGainersHeader.findViewById(R.id.tvTrading);
        tvGainersChange = llGainersHeader.findViewById(R.id.tvChange);
        tvGainersChangePercent = llGainersHeader.findViewById(R.id.tvChangePercent);

        llLosersHeader = findViewById(R.id.llLosersHeader);
        tvLosersSymbol = llLosersHeader.findViewById(R.id.tvSymbol);
        tvLosersPrice = llLosersHeader.findViewById(R.id.tvLast);
        tvLosersTrading = llLosersHeader.findViewById(R.id.tvTrading);
        tvLosersChange = llLosersHeader.findViewById(R.id.tvChange);
        tvLosersChangePercent = llLosersHeader.findViewById(R.id.tvChangePercent);

        llTradedHeader = findViewById(R.id.llTradedHeader);
        tvTradedSymbol = llTradedHeader.findViewById(R.id.tvSymbol);
        tvTradedPrice = llTradedHeader.findViewById(R.id.tvLast);
        tvTradedTrading = llTradedHeader.findViewById(R.id.tvTrading);
        tvTradedChange = llTradedHeader.findViewById(R.id.tvChange);
        tvTradedChangePercent = llTradedHeader.findViewById(R.id.tvChangePercent);

        llTradesHeader = findViewById(R.id.llTradesHeader);
        tvTradesSymbol = llTradesHeader.findViewById(R.id.tvSymbol);
        tvTradesPrice = llTradesHeader.findViewById(R.id.tvLast);
        tvTradesTrading = llTradesHeader.findViewById(R.id.tvTrading);
        tvTradesChange = llTradesHeader.findViewById(R.id.tvChange);
        tvTradesChangePercent = llTradesHeader.findViewById(R.id.tvChangePercent);

        llVolumeHeader = findViewById(R.id.llVolumeHeader);
        tvVolumeSymbol = llVolumeHeader.findViewById(R.id.tvSymbol);
        tvVolumePrice = llVolumeHeader.findViewById(R.id.tvLast);
        tvVolumeTrading = llVolumeHeader.findViewById(R.id.tvTrading);
        tvVolumeChange = llVolumeHeader.findViewById(R.id.tvChange);
        tvVolumeChangePercent = llVolumeHeader.findViewById(R.id.tvChangePercent);

        tvGainersChange.setText(getResources().getString(R.string.change_title));
        tvGainersChangePercent.setText(getResources().getString(R.string.change_weight_percent_title));

        tvLosersChange.setText(getResources().getString(R.string.change_title));
        tvLosersChangePercent.setText(getResources().getString(R.string.change_weight_percent_title));

        tvTradedChange.setText(getResources().getString(R.string.trading_value));
        tvTradedChange.setTextSize(MyApplication.lang == MyApplication.ENGLISH ? 12 : 11);

        tvTradesTrading.setVisibility(View.VISIBLE);
        tvTradesChange.setText(getResources().getString(R.string.trades_title));
        tvTradesChangePercent.setText(getResources().getString(R.string.change_title));
        tvTradesTrading.setText(getResources().getString(R.string.amount_title));

        tvVolumeTrading.setVisibility(View.VISIBLE);
        tvVolumeChange.setText(getResources().getString(R.string.trades_title));
        tvVolumeChangePercent.setText(getResources().getString(R.string.change_title));
        tvVolumeTrading.setText(getResources().getString(R.string.amount_title));
    }

    private void initializeFilters(){

        if(MyApplication.lang == MyApplication.ARABIC){
            AllMarkets.add(TradingSession.All_ar);
            AllMarkets.add(TradingSession.REG_ar);
            AllMarkets.add(TradingSession.FUNDS_ar);
        }
        else{
            AllMarkets.add(TradingSession.All);
            AllMarkets.add(TradingSession.REG);
            AllMarkets.add(TradingSession.FUNDS);
        }


        ImageView iv_arrow = findViewById(R.id.iv_arrow);
        iv_arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                spMarkets.performClick();
            }
        });
        rvInstruments =  findViewById(R.id.RV_instrument);
        spMarkets =  findViewById(R.id.spMarket);
        marketsSpinnerAdapter = new MarketsSpinnerAdapter(this, AllMarkets , true) ;
        spMarkets.setAdapter(marketsSpinnerAdapter);
        spMarkets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectMarket = AllMarkets.get(position);
                MyApplication.instrumentId = "";
                isSelectInstrument = false;

                if(selectMarket.getValue() == TradingSession.All.getValue()){
                    marketInstruments = allInstruments;
                }else{
                    marketInstruments = Actions.filterInstrumentsByMarketSegmentID(MyApplication.instruments , selectMarket.getValue());
                }

                for (Instrument inst : marketInstruments) { inst.setIsSelected(false); }

                instrumentsRecyclerAdapter = new InstrumentsRecyclerAdapter(TopsActivity.this, marketInstruments,TopsActivity.this);
                rvInstruments.setAdapter(instrumentsRecyclerAdapter);
                Log.wtf("select Market : " + selectMarket.toString() , "instrument count = " + marketInstruments.size());
                allStocks.clear();
                retrieveFiltered();

                Log.wtf("on instr click","allStocks count = " + allStocks.size());

                notifyadapters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        rvInstruments.setLayoutManager(new LinearLayoutManager(TopsActivity.this , LinearLayoutManager.HORIZONTAL, false));
        instrumentsRecyclerAdapter = new InstrumentsRecyclerAdapter(this, marketInstruments,this);
        rvInstruments.setAdapter(instrumentsRecyclerAdapter);

    }



    public void back(View v) {

        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();

        Actions.checkSession(this);

        //Actions.InitializeSessionService(this);
        //Actions.InitializeMarketService(this);
        Actions.InitializeSessionServiceV2(this);
       // Actions.InitializeMarketServiceV2(this);

        Actions.checkLanguage(this, started);
    }

    @Override
    protected void onStop() {
        super.onStop();

        Actions.unregisterMarketReceiver(this);
        Actions.unregisterSessionReceiver(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyApplication.sessionOut = Calendar.getInstance();
    }

    public void loadFooter(View v) {

        Actions.loadFooter(this, v);
    }

    @Override
    public void onItemClicked(View v, int position) {
        for(int i=0; i<marketInstruments.size(); i++){
            if(i==position){
                marketInstruments.get(i).setIsSelected(marketInstruments.get(i).getIsSelected() ? false : true);
                selectedInstrument = marketInstruments.get(i).getIsSelected() ? marketInstruments.get(i) : new Instrument();
                instrumentId = marketInstruments.get(i).getIsSelected() ? selectedInstrument.getInstrumentCode() : "" ;
                isSelectInstrument = marketInstruments.get(i).getIsSelected() ? true : false ;
                MyApplication.instrumentId = instrumentId;
            }
            else{
                marketInstruments.get(i).setIsSelected(false);
            }
        }

        /*if(allInstruments.get(position).getIsSelected()){ allInstruments.get(position).setIsSelected(false); }
        else{ allInstruments.get(position).setIsSelected(true); }*/

        instrumentsRecyclerAdapter.notifyDataSetChanged();

        allStocks.clear();
        retrieveFiltered();
    }

    private void retrieveFiltered() {
        tmpStocks = new ArrayList<>();
        allStocks = new ArrayList<>();
        tmpStocks = (Actions.filterTopsByInstruments(MyApplication.stockTops , MyApplication.instruments));


        if(isSelectInstrument) {

            allStocks.addAll(tmpStocks);
        }
        else{

            if(selectMarket.getValue() == TradingSession.All.getValue()){
                allStocks.addAll(Actions.filterTopsByInstruments(MyApplication.stockTops, allInstruments));
            }
            else{
                allStocks.addAll(Actions.filterTopsByInstruments(MyApplication.stockTops, marketInstruments));

            }
        }


        gainersAdapter = new TopsRecyclerAdapter(TopsActivity.this, Actions.getStocksTopsByType(allStocks, MyApplication.TOP_GAINERS), MyApplication.TOP_GAINERS);
        rvTopGainers.setAdapter(gainersAdapter);

        losersAdapter = new TopsRecyclerAdapter(TopsActivity.this, Actions.getStocksTopsByType(allStocks, MyApplication.TOP_LOSERS), MyApplication.TOP_LOSERS);
        rvTopLosers.setAdapter(losersAdapter);

        tradedAdapter = new TopsRecyclerAdapter(TopsActivity.this, Actions.getStocksTopsByType(allStocks, MyApplication.TOP_TRADED), MyApplication.TOP_TRADED);
        rvTopTraded.setAdapter(tradedAdapter);

        tradesAdapter = new TopsRecyclerAdapter(TopsActivity.this, Actions.getStocksTopsByType(allStocks, MyApplication.TOP_TRADES), MyApplication.TOP_TRADES);
        rvTopTrades.setAdapter(tradesAdapter);

        volumeAdapter = new TopsRecyclerAdapter(TopsActivity.this, Actions.getStocksTopsByType(allStocks, MyApplication.TOP_VALUES), MyApplication.TOP_VALUES);
        rvTopVolume.setAdapter(volumeAdapter);



        Log.wtf("on instr click","allStocks count = " + allStocks.size());

        //rvStocks.setAdapter(adapter);
        //adapter.notifyDataSetChanged();
    }




    private class GetInstruments extends AsyncTask<Void, String, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
           // MyApplication.showDialog(TopsActivity.this);
        }

        @Override
        protected String doInBackground(Void... a) {

            String result = "";
            String url = MyApplication.link + MyApplication.GetInstruments.getValue(); // this method uses key after login

            try {
                HashMap<String, String> parameters = new HashMap<String, String>();
                parameters.put("id", instrumentId.length() == 0 ? "0" : instrumentId);
                parameters.put("key", getResources().getString(R.string.beforekey));
                result = ConnectionRequests.GET(url, TopsActivity.this, parameters);

                MyApplication.instruments.addAll(GlobalFunctions.GetInstrumentsList(result));

            } catch (Exception e) {
                e.printStackTrace();
                if(MyApplication.isDebug) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.error_code) + MyApplication.GetInstruments.getKey(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            MyApplication.dismiss();

            allInstruments.clear();
            for(int i=1; i<AllMarkets.size(); i++){
                Log.wtf("GetInstruments onPostExecute","MyApplication.instruments count: " + MyApplication.instruments.size());
                Log.wtf("GetInstruments onPostExecute","AllMarkets.get(i) : " + AllMarkets.get(i).getValue());
                allInstruments.addAll(Actions.filterInstrumentsByMarketSegmentID(MyApplication.instruments , AllMarkets.get(i).getValue()));
            }
            marketInstruments = allInstruments;
            instrumentsRecyclerAdapter.notifyDataSetChanged();

            tmpStocks = (MyApplication.stockTops);
            allStocks.addAll(Actions.filterTopsByInstruments(tmpStocks,  marketInstruments));
          try {
              notifyadapters();
          }catch (Exception e){
              Log.wtf("adapter_exception",e.toString());
          }

        }
    }




    private void notifyadapters(){
        gainersAdapter.notifyDataSetChanged();
        losersAdapter.notifyDataSetChanged();
        tradedAdapter.notifyDataSetChanged();
        tradesAdapter.notifyDataSetChanged();
        volumeAdapter.notifyDataSetChanged();
    }









    private class GetTops extends AsyncTask<Void, Void, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            try {
                MyApplication.showDialog(TopsActivity.this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        protected String doInBackground(Void... params) {

            String result = "";
            String url = MyApplication.link + MyApplication.GetStockTops.getValue(); // this method uses key after login

            HashMap<String, String> parameters = new HashMap<String, String>();

            try {
                parameters.clear();
                parameters.put("MarketID", MyApplication.marketID);
                parameters.put("count", "5");
                parameters.put("key",  getResources().getString(R.string.beforekey));
                result = ConnectionRequests.GET(url, TopsActivity.this, parameters);

                try {
                    allStocks.addAll(GlobalFunctions.GetStockTops(result));
                    MyApplication.stockTops.addAll(GlobalFunctions.GetStockTops(result));
                } catch (Exception e) {

                    e.printStackTrace();
                    if(MyApplication.isDebug) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.error_code) + MyApplication.GetStockTops.getKey(), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if(MyApplication.isDebug) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.error_code) + MyApplication.GetStockTops.getKey(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
            return result;
        }







            @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.wtf("alStocks", allStocks.size()+"");

            try {

                gainersAdapter = new TopsRecyclerAdapter(TopsActivity.this, Actions.getStocksTopsByType(allStocks, MyApplication.TOP_GAINERS), MyApplication.TOP_GAINERS);
                rvTopGainers.setAdapter(gainersAdapter);

                losersAdapter = new TopsRecyclerAdapter(TopsActivity.this, Actions.getStocksTopsByType(allStocks, MyApplication.TOP_LOSERS), MyApplication.TOP_LOSERS);
                rvTopLosers.setAdapter(losersAdapter);

                tradedAdapter = new TopsRecyclerAdapter(TopsActivity.this, Actions.getStocksTopsByType(allStocks, MyApplication.TOP_TRADED), MyApplication.TOP_TRADED);
                rvTopTraded.setAdapter(tradedAdapter);

                tradesAdapter = new TopsRecyclerAdapter(TopsActivity.this, Actions.getStocksTopsByType(allStocks, MyApplication.TOP_TRADES), MyApplication.TOP_TRADES);
                rvTopTrades.setAdapter(tradesAdapter);

                volumeAdapter = new TopsRecyclerAdapter(TopsActivity.this, Actions.getStocksTopsByType(allStocks, MyApplication.TOP_VALUES), MyApplication.TOP_VALUES);
                rvTopVolume.setAdapter(volumeAdapter);


                marketInstruments = allInstruments;
                instrumentsRecyclerAdapter.notifyDataSetChanged();


                MyApplication.dismiss();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




    private ArrayList<Stock> filterTops(String tradingSession,String instrumentId){
        ArrayList<Stock> filteredArray=new ArrayList<>();

         if(!instrumentId.isEmpty()){
            for (int i = 0; i < allStocks.size(); i++) {
                if (allStocks.get(i).getInstrumentId().matches(instrumentId));
                   filteredArray.add(allStocks.get(i));

            }
        }

      return filteredArray;
    }





}
