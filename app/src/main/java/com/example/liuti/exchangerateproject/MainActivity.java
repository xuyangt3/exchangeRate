package com.example.liuti.exchangerateproject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.GridLabelRenderer;

import android.view.View;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.TimeZone;

/**
 * The only activity of exchangeRate.
 */
public class MainActivity extends AppCompatActivity implements Response.Listener<JSONObject> {
    /**
     * Default logging tag for messages from the main activity.
     */
    static final String TAG = "ExchangeRate";

    /**
     * Name tag to store and retrieve preferences.
     */
    @SuppressWarnings("CheckStyle")
    static final String Selected_Currencies = "Selected_Currencies", Base_Currency = "Base_Currency";

    /**
     * The array of currencies' abbreviations and their descriptions.
     */
    private String[] currencyArr;

    /**
     * Stores the choice of currencies that we want to check rates.
     */
    private final ArrayList<Integer> selCurrencyLi = new ArrayList<>();

    /**
     * {@code selectedBase} Stores the choice of base currency.
     */
    private int selectedBase;

    private Date start, end;

    /**
     * Request queue for network requests.
     */
    private static RequestQueue requestQueue;

    /**
     * Put default selections into preference files.
     */
    @SuppressWarnings({"magicnumber", "CheckStyle"})
    private void initializeDefaultSelectionPreference() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
//        if (pref.contains(BaseCurrency)) {
//            return;
//        }
        StringBuilder str = new StringBuilder();
        str.append(8).append(",");
        pref.edit()
                .clear()
                .putString(Selected_Currencies, str.toString())
                .putInt(Base_Currency, 31)
                .apply();
    }

    /**
     * Read default selection from preference files.
     */
    private void readDefaultSelectionPreference() {
        SharedPreferences pref = getPreferences(Context.MODE_PRIVATE);
        String savedString = pref.getString(Selected_Currencies, "");
        StringTokenizer st = new StringTokenizer(savedString, ",");

        selectedBase = pref.getInt(Base_Currency, 0);
        while (st.hasMoreTokens()) {
            int t = Integer.parseInt(st.nextToken());
            selCurrencyLi.add(t);
        }
    }

    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
        currencyArr = getResources().getStringArray(R.array.currencies);
        initializeDefaultSelectionPreference();
        readDefaultSelectionPreference();

        final boolean[] seleCurrencies = new boolean[currencyArr.length];
        for (int i : selCurrencyLi) {
            seleCurrencies[i] = true;
        }

        //method variable has to be final in order to be accessed by anonymous inner classes
        final int[] tselectedBase = new int[]{selectedBase};

        {
            Calendar tempCld = Calendar.getInstance();
            tempCld.setTimeInMillis(System.currentTimeMillis());
            end = tempCld.getTime();
            tempCld.add(Calendar.MONTH, -3);
            start = tempCld.getTime();
        }

        requestQueue = Volley.newRequestQueue(this);

        Button selectBaseCurrency = findViewById(R.id.selectBaseCurrency);
        selectBaseCurrency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.select_currency)
                        .setPositiveButton(R.string.select, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                selectedBase = tselectedBase[0];
                                Toast.makeText(MainActivity.this, currencyArr[selectedBase], Toast.LENGTH_LONG).show();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                            }
                        })
                        .setSingleChoiceItems(R.array.currencies,
                                selectedBase,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(final DialogInterface dialog, final int id) {
                                        tselectedBase[0] = id;
                                    }
                                })
                        .create()
                        .show();
            }
        });

        Button selectCurrencyToCheck = findViewById(R.id.selectCurrencyToCheck);
        selectCurrencyToCheck.setOnClickListener(new View.OnClickListener() {
            public void onClick(final View v) {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.select_currency)
                        .setPositiveButton(R.string.select, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                int count = 0;
                                for (boolean i : seleCurrencies) {
                                    if (i) {
                                        count++;
                                    }
                                }
                                if (count > 8 || count == 0) {
                                    //restore original state
                                    Arrays.fill(seleCurrencies, false);
                                    for (int i : selCurrencyLi) {
                                        seleCurrencies[i] = true;
                                    }
                                    String message;
                                    if (count == 0) {
                                        message = "Please select at least one";
                                    } else {
                                        message = "Too many selections (more than 8)";
                                    }
                                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                                    return;
                                }
                                selCurrencyLi.clear();
                                for (int i = 0; i < seleCurrencies.length; i++) {
                                    if (seleCurrencies[i]) {
                                        selCurrencyLi.add(i);
                                    }
                                }
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(final DialogInterface dialog, final int id) {
                                Arrays.fill(seleCurrencies, false);
                                for (int i : selCurrencyLi) {
                                    seleCurrencies[i] = true;
                                }
                            }
                        })
                        .setMultiChoiceItems(R.array.currencies,
                                seleCurrencies,
                                new DialogInterface.OnMultiChoiceClickListener() {
                                    public void onClick(final DialogInterface dialog, final int i,
                                                        final boolean checked) {
                                        seleCurrencies[i] = checked;
                                    }
                                })
                        .create()
                        .show();
            }
        });

        Button selectTime = findViewById(R.id.selectTime);
        class selTimeListener implements View.OnClickListener, DialogInterface.OnClickListener {
            private Calendar cld = Calendar.getInstance();
            private NumberPicker startYear, startMonth;
            private NumberPicker endYear, endMonth;
            private int currY, currM, currD;

            public void onClick(View v) {
                cld.setTimeInMillis(System.currentTimeMillis());
                currY = cld.get(Calendar.YEAR);
                currM = cld.get(Calendar.MONTH);
                currD = cld.get(Calendar.DATE);

                AlertDialog dlg = new AlertDialog
                        .Builder(MainActivity.this)
                        .setView(getLayoutInflater().inflate(R.layout.select_date, null))
                        .setPositiveButton(R.string.select, selTimeListener.this)
                        .setNegativeButton(R.string.cancel, null)
                        .create();
                dlg.show();

                {
                    startYear = dlg.findViewById(R.id.startYear);
                    startYear.setWrapSelectorWheel(false);
                    startYear.setMinValue(1999);
                    startYear.setMaxValue(currY);

                    endYear = dlg.findViewById(R.id.endYear);
                    endYear.setWrapSelectorWheel(false);
                    endYear.setMinValue(1999);
                    endYear.setMaxValue(currY);
                }

                {
                    startMonth = dlg.findViewById(R.id.startMonth);
                    startMonth.setDisplayedValues(getResources().getStringArray(R.array.months));
                    startMonth.setMinValue(0);

                    endMonth = dlg.findViewById(R.id.endMonth);
                    endMonth.setDisplayedValues(getResources().getStringArray(R.array.months));
                    endMonth.setMinValue(0);
                }

                {
                    cld.setTime(start);
                    startYear.setValue(cld.get(Calendar.YEAR));
                    if (startYear.getValue() == currY) {
                        startMonth.setMaxValue(currM);
                    } else {
                        startMonth.setMaxValue(11);
                    }
                    startMonth.setValue(cld.get(Calendar.MONTH));

                    startYear.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                        @Override
                        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                            if (newVal != currY) {
                                startMonth.setMaxValue(11);
                            } else {
                                startMonth.setMaxValue(currM);
                            }
                        }
                    });


                    cld.setTime(end);
                    endYear.setValue(cld.get(Calendar.YEAR));
                    if (endYear.getValue() == currY) {
                        endMonth.setMaxValue(currM);
                    } else {
                        endMonth.setMaxValue(11);
                    }
                    endMonth.setValue(cld.get(Calendar.MONTH));

                    endYear.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
                        @Override
                        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                            if (newVal != currY) {
                                endMonth.setMaxValue(11);
                            } else {
                                endMonth.setMaxValue(currM);
                            }
                        }
                    });
                }
            }

            public void onClick(DialogInterface dialog, int which) {
                if ((startYear.getValue() << 4 | startMonth.getValue()) >=
                        (endYear.getValue() << 4 | endMonth.getValue())) {
                    String message;
                    if (startYear.getValue() == endYear.getValue() &&
                            startMonth.getValue() == endMonth.getValue()) {
                        message = "Selected period is too short";
                    } else {
                        message = "Start date cannot be later than end date";
                    }
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                    return;
                }
                cld.set(startYear.getValue(), startMonth.getValue(), 1);
                start = cld.getTime();

                cld.set(endYear.getValue(), endMonth.getValue(), 1);
                cld.set(Calendar.DATE, cld.getActualMaximum(Calendar.DATE));
                end = cld.getTime();
            }
        }

        selectTime.setOnClickListener(new selTimeListener());

        Button startAPI = findViewById(R.id.startAPI);
        startAPI.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startAPICall();
            }
        });
    }

    /**
     * To make MainActivity implement onClickListener. Used for drawing the graph.
     */
    @SuppressWarnings("CheckStyle")
    public void onResponse(final JSONObject response) {
        GraphView graph = findViewById(R.id.graph);
        graph.setVisibility(View.INVISIBLE);
        graph.removeAllSeries();

        JSONObject rates = null;
        try {
            rates = response.getJSONObject("rates");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            Long[] datesArr;
            {
                long oneDay = 24 * 60 *60 * 1000;
                Iterator<String> itr = rates.keys();
                ArrayList<Long> datesAL = new ArrayList<>((int)((
                        end.getTime() - start.getTime()) / oneDay / 1.4));
                while (itr.hasNext()) {
                    datesAL.add(sdf.parse(itr.next()).getTime());
                }
                datesArr = datesAL.toArray(new Long[0]);
            }
            Arrays.sort(datesArr);
            int index = 0;
            {
                long interval = (end.getTime() - start.getTime()) >>> 7;
                Long[] tempDates = new Long[129];
                long last = 0L;
                for (int i = 0; i < datesArr.length; i++) {
                    if (datesArr[i] - last > interval) {
                        last = datesArr[i];
                        tempDates[index] = datesArr[i];
                        index++;
                    }
                }
                datesArr = tempDates;
            }

            int[] colors = getResources().getIntArray(R.array.rainbow);
            String[] dateKeys = new String[index];
            for (int i = 0; i < index; i++) {
                dateKeys[i] = sdf.format(new Date(datesArr[i]));
            }
            Long l =1L;
            DataPoint[] data = new DataPoint[index];
            for (int i = 0; i < selCurrencyLi.size(); i++) {
                String symbol = currencyArr[selCurrencyLi.get(i)];
                for (int j = 0; j < index; j++) {
                    data[j] = new DataPoint(
                            datesArr[j],
                            rates.getJSONObject(dateKeys[j]).getDouble(symbol));
                }
                LineGraphSeries<DataPoint> ser = new LineGraphSeries<>(data);
                ser.setTitle(symbol);
                ser.setColor(colors[i]);
                graph.addSeries(ser);
            }

            // enable scaling and scrolling
            Viewport vpt = graph.getViewport();
            vpt.setMinX((double) datesArr[0]);
            vpt.setMaxX((double) datesArr[index - 1]);
            vpt.setScalable(true);
            vpt.setScalableY(true);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
        }

        DefaultLabelFormatter dlf = new DefaultLabelFormatter() {
            private SimpleDateFormat large = new SimpleDateFormat("M/yy");
            private SimpleDateFormat small = new SimpleDateFormat("M/d");
            private Calendar mCalendar = Calendar.getInstance();
            private long threeMonths = 3L * 30 * 24 * 60 * 60 * 1000;

            public String formatLabel(double value, boolean isValueX) {
                if (isValueX) {
                    this.mCalendar.setTimeInMillis((long) value);
                    if (mViewport.getMaxX(false)
                            - mViewport.getMinX(false) < threeMonths) {
                        return small.format(this.mCalendar.getTimeInMillis());
                    } else {
                        return large.format(this.mCalendar.getTimeInMillis());
                    }
                } else {
                    return super.formatLabel(value, false);
                }
            }
        };
        dlf.setViewport(graph.getViewport());

        GridLabelRenderer lblRenderer = graph.getGridLabelRenderer();
        lblRenderer.setLabelFormatter(dlf);
        lblRenderer.setVerticalAxisTitle("ExchangeRate");
        lblRenderer.setHorizontalAxisTitle("Time");
        lblRenderer.setNumHorizontalLabels(5);
        lblRenderer.setNumVerticalLabels(7);

        graph.getLegendRenderer().setVisible(true);

        graph.setVisibility(View.VISIBLE);
        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    /**
     * Make an API call.
     */
    void startAPICall() {
        findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        StringBuffer sb = new StringBuffer();
        sb.append("?start_at=");
        sb.append(sdf.format(start));
        sb.append("&end_at=");
        sb.append(sdf.format(end));
        sb.append("&base=");
        sb.append(currencyArr[selectedBase]);
        sb.append("&symbols=");
        for (int i : selCurrencyLi) {
            sb.append(currencyArr[i]);
            sb.append(',');
        }
        sb.deleteCharAt(sb.length() - 1);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET,
                "https://api.exchangeratesapi.io/history"
                        + sb.toString(),
                null,
                MainActivity.this,
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(final VolleyError error) {
                        Toast.makeText(MainActivity.this,
                                getString(R.string.error_message) + error.getLocalizedMessage(),
                                Toast.LENGTH_LONG);
                    }
                });
        requestQueue.add(jsonObjectRequest);
    }
}
