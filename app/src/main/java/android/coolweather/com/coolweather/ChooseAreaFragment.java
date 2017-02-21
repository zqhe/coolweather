package android.coolweather.com.coolweather;

import android.app.ProgressDialog;
import android.coolweather.com.coolweather.db.City;
import android.coolweather.com.coolweather.db.County;
import android.coolweather.com.coolweather.db.Province;
import android.coolweather.com.coolweather.util.HttpUtil;
import android.coolweather.com.coolweather.util.Utility;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2017/2/21 0021.
 */

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog mProgressDialog;
    private TextView title_text;
    private Button back_button;
    private ListView list_view;
    private ArrayAdapter<String> mAdapter;
    private List<String> dataList = new ArrayList<>();
    private List<Province> mProvinceList;
    private List<City> mCityList;
    private List<County> mCountyList;
    private Province selectedProvince;
    private City selecedCity;
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        title_text = (TextView) view.findViewById(R.id.title_text);
        back_button = (Button) view.findViewById(R.id.back_button);
        list_view = (ListView) view.findViewById(R.id.list_view);
        mAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, dataList);
        list_view.setAdapter(mAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (LEVEL_PROVINCE == currentLevel) {
                    selectedProvince = mProvinceList.get(position);
                    queryCities();
                } else if (LEVEL_CITY == currentLevel) {
                    selecedCity = mCityList.get(position);
                    queryCounties();
                }
            }
        });
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LEVEL_COUNTY == currentLevel) {
                    queryCities();
                } else if (LEVEL_CITY == currentLevel) {
                    queryProvinces();
                }
            }
        });
        queryProvinces();
    }

    private void queryProvinces() {
        title_text.setText("中国");
        back_button.setVisibility(View.GONE);
        mProvinceList = DataSupport.findAll(Province.class);
        if (mProvinceList.isEmpty()) {
            String address = BASE_ADDRESS;
            queryFromServer(address, TYPE_PROVINCE);
        } else {
            dataList.clear();
            for (Province province : mProvinceList) {
                dataList.add(province.getProvinceName());
            }
            mAdapter.notifyDataSetChanged();
            list_view.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }
    }

    private static final String BASE_ADDRESS = "http://guolin.tech/api/china";
    private static final String TYPE_PROVINCE = "province";
    private static final String TYPE_CITY = "city";
    private static final String TYPE_COUNTY = "county";

    private void queryCities() {
        title_text.setText(selectedProvince.getProvinceName());
        back_button.setVisibility(View.VISIBLE);
        mCityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (mCityList.isEmpty()) {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = BASE_ADDRESS + "/" + provinceCode;
            queryFromServer(address, TYPE_CITY);
        } else {
            dataList.clear();
            for (City city : mCityList) {
                dataList.add(city.getCityName());
            }
            mAdapter.notifyDataSetChanged();
            list_view.setSelection(0);
            currentLevel = LEVEL_CITY;
        }
    }

    private void queryCounties() {
        title_text.setText(selecedCity.getCityName());
        back_button.setVisibility(View.VISIBLE);
        mCountyList = DataSupport.where("cityid = ?", String.valueOf(selecedCity.getId())).find(County.class);
        if (mCountyList.isEmpty()) {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selecedCity.getCityCode();
            String address = BASE_ADDRESS + "/" + provinceCode + "/" + cityCode;
            queryFromServer(address, TYPE_COUNTY);
        } else {
            dataList.clear();
            for (County county : mCountyList) {
                dataList.add(county.getCountyName());
            }
            mAdapter.notifyDataSetChanged();
            list_view.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        }
    }

    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                String responseBody = response.body().string();
                boolean result = false;
                if (TYPE_PROVINCE.equals(type)) {
                    result = Utility.handleProvinceResponse(responseBody);
                } else if (TYPE_CITY.equals(type)) {
                    result = Utility.handleCityResponse(responseBody, selectedProvince.getId());
                } else if (TYPE_COUNTY.equals(type)) {
                    result = Utility.handleCountyResponse(responseBody, selecedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if (TYPE_PROVINCE.equals(type)) {
                                queryProvinces();
                            } else if (TYPE_CITY.equals(type)) {
                                queryCities();
                            } else if (TYPE_COUNTY.equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    private void showProgressDialog() {

        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage("正在加载...");
            mProgressDialog.setCanceledOnTouchOutside(false);
        }
        mProgressDialog.show();
    }

    private void closeProgressDialog() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }
    }

}
