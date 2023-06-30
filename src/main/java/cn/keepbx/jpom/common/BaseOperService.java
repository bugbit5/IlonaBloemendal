package cn.keepbx.jpom.common;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * 标准操作Service
 *
 * @author jiangzeyin
 * @date 2019/3/14
 */
public abstract class BaseOperService<T> extends BaseDataService {
    /**
     * 获取所有数据
     *
     * @return list
     * @throws IOException IO
     */
    public abstract List<T> list() throws IOException;

    /**
     * 工具id 获取 实体
     *
     * @param id 数据id
     * @return T
     * @throws IOException IO
     */
    public abstract T getItem(String id) throws IOException;

    protected JSONArray formatToArray(JSONObject jsonObject) {
        Set<String> setKey = jsonObject.keySet();
        JSONArray jsonArray = new JSONArray();
        for (String key : setKey) {
            jsonArray.add(jsonObject.getJSONObject(key));
        }
        return jsonArray;
    }
}
