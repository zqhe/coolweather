package android.coolweather.com.coolweather.db;

import org.litepal.crud.DataSupport;

/**
 * Created by Administrator on 2017/2/20 0020.
 */

public class Province extends DataSupport {
    private int id;
    private int provinceCode;
    private String provinceName;


    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(int provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

}
