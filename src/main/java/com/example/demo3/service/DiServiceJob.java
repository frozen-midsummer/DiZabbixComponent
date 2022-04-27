package com.example.demo3.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.example.demo3.entity.*;
import com.hirisun.lanji.common.enums.di.DiComponentServiceTypeEnum;
import com.hirisun.lanji.common.model.DiApiConfigCacheModel;
import com.hirisun.lanji.component.sdk.service.DiServiceAdapter;
import com.hirisun.lanji.component.sdk.utils.DiApiConfigUtils;
import com.hirisun.lanji.component.sdk.utils.HttpClientUtils;
import com.hirisun.lanji.component.sdk.utils.StringUtils;
import com.hirisun.lanji.logger.api.HirisunLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.util.*;

@Service
public class DiServiceJob {
    @Autowired
    private HirisunLogger logger;
    @Autowired
    private ScriptEngine scriptEngine;
    @Autowired
    private DiServiceAdapter diServiceAdapter;

    private ZabbixInfo zabbixInfo;// zabbix授权信息(前端设置)
    private String auth;// zabbix授权UUID
    private int scheduleTime;// 调度时间戳
    private DiApiConfigCacheModel diApiConfigCacheModel;
    private HashMap<String, String> itemTable;// key_别名对照表
    private HashMap<String, String> itemKey_Name;// key_别名对照表
    private HashMap<String, String> itemKey_Remark;// key_别名对照表
    private HashMap<String, String> params;// 初始化doPost的参数
    private HashMap<String, ItemHostGroup> itemValue;// 最终输入脚本的数据
    private List<String> key_;// 建立key_查询列表
    private List<String> hostIdList;

    public HirisunLogger getLogger() {
        return logger;
    }

    public ScriptEngine getScriptEngine() {
        return scriptEngine;
    }

    public DiServiceAdapter getDiServiceAdapter() {
        return diServiceAdapter;
    }

    @PostConstruct
    public void getDiApiConfig() {// 获取蓝基前端配置信息
        DiApiConfigCacheModel diApiConfigCacheModel = diServiceAdapter.getDiApiConfig();// 获取配置信息
        if (diApiConfigCacheModel.getComponentConfigType() == null) {
            logger.error("获取配置信息失败：配置信息不存在！");
        } else {
            if (diApiConfigCacheModel.getComponentServiceType().equals(DiComponentServiceTypeEnum.SCHEDULE)) {
                this.diApiConfigCacheModel = diApiConfigCacheModel;
                JSONObject zabbix_auth = diApiConfigCacheModel.getContent().getJSONObject("ZABBIX_AUTH");// zabbix授权UUID
                try {
                    if (zabbix_auth.toJSONString() == null) {
                        throw new RuntimeException("授权节点配置信息不存在，请确保存在配置节点！");
                    }
                    this.zabbixInfo = JSON.parseObject(zabbix_auth.toJSONString(), ZabbixInfo.class);// this.zabbixInfo为授权信息，格式如下：
                    // {"entranceAddress":"q","usrname":"w","psd":"e"}
                    HashMap<String, String> params = new HashMap<>();// 初始化doPost的参数
                    params.put("key", "value");
                    this.params = params;
                    String authReq = "{\"jsonrpc\":\"2.0\",\"method\":\"user.login\",\"id\":1,\"auth\":null,\"params\":{\"user\":\"" + zabbixInfo.getUsrname() + "\",\"password\":\"" + zabbixInfo.getPsd() + "\"}}";
                    String authRes = HttpClientUtils.doPost(zabbixInfo.getEntranceAddress(), null, params, authReq, null);// 得到授权结果
                    AuthRes authRes_Object = JSON.parseObject(authRes, AuthRes.class);
                    this.auth = authRes_Object.getResult();// 获取zabbix授权密钥
                } catch (Exception e) {
                    logger.error("获取授权配置信息失败：{}", e.getMessage());
                    throw new RuntimeException("获取授权配置信息失败！");
                }
                if (Objects.equals(diApiConfigCacheModel.getComponentConfigType(), "TRIGGER_SCHEDULE")) {
                    // 以下代码将zabbix中截至目前尚未解决的problem推送至js脚本
                    logger.info("告警信息初始化开始...........");
                    try {
                        Date date = new Date();
                        scheduleTime = (int) Math.floor(date.getTime() / 1000.000);
                        String triggerReq = "{\"jsonrpc\":\"2.0\",\"method\":\"trigger.get\",\"params\":{\"lastChangeSince\":0,\"sortfiled\":\"lastchange\",\"sortorder\":\"DESC\",\"selectHosts\":[\"host\",\"name\"],\"selectGroups\":[\"name\"],\"expandDescription\":1,\"only_true\":1},\"auth\":\"" + this.auth + "\",\"id\":2}";
                        String triggerRes = HttpClientUtils.doPost(zabbixInfo.getEntranceAddress(), null, this.params, triggerReq, null);// the result of trigger.get
                        String javaScript = DiApiConfigUtils.getScriptConent(diApiConfigCacheModel);// 获取并执行javascript脚本
                        if (!StringUtils.isEmpty(javaScript)) {
                            scriptEngine.eval(javaScript);
                            Invocable invocable = (Invocable) scriptEngine;
                            Object result = invocable.invokeFunction("run", triggerRes);
                            // 返回值可在下面这行代码查看(js脚本的返回值，调试使用)
                            // logger.info(JSON.toJSONString(result));
                        }
                    } catch (Exception e) {
                        logger.error("告警信息初始化异常：{}" + e.getMessage());
                    }
                    logger.info("告警信息初始化结束...........");

                } else if (Objects.equals(diApiConfigCacheModel.getComponentConfigType(), "ITEM_SCHEDULE")) {// 判断配置类型configtype为监控信息主动获取
                    JSONObject item_config = diApiConfigCacheModel.getContent().getJSONObject("ITEM_CONFIG");// 获取监控项配置
                    ItemConfigList itemConfig = JSON.parseObject(item_config.toJSONString(), ItemConfigList.class);// 将监控项配置转为列表
                    HashMap<String, String> itemTable = new HashMap<>();// 建立监控项key-别名哈希对照表
                    HashMap<String, String> itemKey_Name = new HashMap<>();// 建立监控项key-名称哈希对照表
                    HashMap<String, String> itemKey_Remark = new HashMap<>();// 建立监控项key-备注哈希对照表
                    List<String> key_ = new ArrayList<>();// 建立key_查询列表
                    for (int i = 0; i < itemConfig.getItemConfig().size(); i++) {
                        itemTable.put(itemConfig.getItemConfig().get(i).getItemKey(), itemConfig.getItemConfig().get(i).getAnthorName());
                        itemKey_Name.put(itemConfig.getItemConfig().get(i).getAnthorName(), itemConfig.getItemConfig().get(i).getItemName());
                        itemKey_Remark.put(itemConfig.getItemConfig().get(i).getAnthorName(), itemConfig.getItemConfig().get(i).getRemark());
                        key_.add(itemConfig.getItemConfig().get(i).getItemKey());
                    }
                    this.itemTable = itemTable;
                    this.itemKey_Name = itemKey_Name;
                    this.itemKey_Remark = itemKey_Remark;
                    this.key_ = key_;
                    // 建立监控项查询key_值列表
                    String hostReq = "{\"jsonrpc\":\"2.0\",\"method\":\"host.get\",\"params\":{\"output\":[\"hostid\",\"host\",\"name\"]},\"auth\":\"" + this.auth + "\",\"id\":2}";
                    String hostRes = HttpClientUtils.doPost(zabbixInfo.getEntranceAddress(), null, this.params, hostReq, null);// the result of host.get
                    HostGet hostGet = JSON.parseObject(hostRes, HostGet.class);// 实例化host.get的结果
                    List<String> hostIdList = new ArrayList<>();// 主机ID列表
                    HashMap<String, ItemHostGroup> itemValue = new HashMap<>();// {"主机id" = itemHostGroup,...}
                    for (int i = 0; i < hostGet.getResult().size(); i++) {
                        HashMap<String, HashMap<String, String>> items = new HashMap<>();
                        for (int j = 0; j < itemConfig.getItemConfig().size(); j++) {
                            items.put(itemConfig.getItemConfig().get(j).getAnthorName(), new HashMap<>());
                        }
                        hostIdList.add(hostGet.getResult().get(i).getHostid());
                        itemValue.put(hostGet.getResult().get(i).getHostid(), new ItemHostGroup(hostGet.getResult().get(i).getHostid(), hostGet.getResult().get(i).getName(), hostGet.getResult().get(i).getHost(), items));
                    }
                    this.itemValue = itemValue;// {"10084":{"hostid":"10084","hostip":"vm server","hostname":"vm server","items":{"sdkPused":{},"systemSwArch":{},"cpuAvg5":{},"agentPing":{}},"time":1650523639484},...}
                    this.hostIdList = hostIdList;
                }
            }
        }
    }


    @Scheduled(cron = "0/3 * * * * ?")   //每10秒执行一次，可自行配置，执行周期会根据配置自动注入
    public void diExecute() {
        logger.info("调度服务开始...........");
        try {
            if (Objects.equals(diApiConfigCacheModel.getComponentConfigType(), "TRIGGER_SCHEDULE")) {
                Date date = new Date();
                int newScheduleTime = (int) Math.floor(date.getTime() / 1000.000);// 调度开始前打下新的时间戳，以下报文选择since旧的时间戳
                String triggerReq = "{\"jsonrpc\":\"2.0\",\"method\":\"trigger.get\",\"params\":{\"lastChangeSince\":" + scheduleTime + ",\"sortfiled\":\"lastchange\",\"sortorder\":\"DESC\",\"selectHosts\":[\"host\",\"name\"],\"selectGroups\":[\"name\"],\"expandDescription\":1,\"only_true\":1},\"auth\":\"" + this.auth + "\",\"id\":2}";
                String triggerRes = HttpClientUtils.doPost(zabbixInfo.getEntranceAddress(), null, this.params, triggerReq, null);// the result of trigger.get
                String javaScript = DiApiConfigUtils.getScriptConent(diApiConfigCacheModel);// 获取并执行javascript脚本
                if (!StringUtils.isEmpty(javaScript)) {
                    scriptEngine.eval(javaScript);
                    Invocable invocable = (Invocable) scriptEngine;
                    Object result = invocable.invokeFunction("run", triggerRes);
                    // 返回值可在下面这行代码查看(js脚本的返回值，调试使用)
                    // logger.info(JSON.toJSONString(result));
                }
                scheduleTime = newScheduleTime;// 调度服务完成后更新时间戳
            } else if (Objects.equals(diApiConfigCacheModel.getComponentConfigType(), "ITEM_SCHEDULE")) {
                Date date = new Date();
                String itemReq = "{\"jsonrpc\":\"2.0\",\"method\":\"item.get\",\"params\":{\"output\":[\"itemid\",\"name\",\"delay\",\"key_\",\"lastvalue\",\"hostid\",\"lastclock\"],\"hostids\":" + this.hostIdList.toString() + ",\"search\":{\"key_\":" + JSON.toJSONString(this.key_) + "},\"sortfield\":\"name\",\"searchByAny\":\"true\"},\"auth\":\"" + this.auth + "\",\"id\":3}";
                String itemRes = HttpClientUtils.doPost(zabbixInfo.getEntranceAddress(), null, this.params, itemReq, null);// the result of item.get
                ItemRes itemResponse = JSON.parseObject(itemRes, ItemRes.class);

                for (int i = 0; i < itemResponse.getResult().size(); i++) {
                    String itemValueKey = itemResponse.getResult().get(i).getHostid();// 获得哈希表key值
                    String itemLastValue = itemResponse.getResult().get(i).getLastvalue();
                    String itemLastClock = itemResponse.getResult().get(i).getLastclock();
                    String itemDelay = itemResponse.getResult().get(i).getDelay();
                    String itemsKey = this.itemTable.get(itemResponse.getResult().get(i).getKey_());// 获得itemRes对应条目的监控项别名
                    this.itemValue.get(itemValueKey).getItems().get(itemsKey).put("itemLastValue", itemLastValue);
                    this.itemValue.get(itemValueKey).getItems().get(itemsKey).put("itemLastClock", itemLastClock);
                    this.itemValue.get(itemValueKey).getItems().get(itemsKey).put("itemDelay", itemDelay);
                    this.itemValue.get(itemValueKey).getItems().get(itemsKey).put("itemName", itemKey_Name.get(itemsKey));
                    this.itemValue.get(itemValueKey).getItems().get(itemsKey).put("remark", itemKey_Remark.get(itemsKey));
                }
                List<ItemHostGroup> itemData = new ArrayList<>();// 最终输入JS脚本的对象数组
                for (String s : hostIdList) {
                    this.itemValue.get(s).setTime(date.getTime());
                    itemData.add(this.itemValue.get(s));// 输入JS脚本中的数据
                }
                String javaScript = DiApiConfigUtils.getScriptConent(diApiConfigCacheModel);// 获取并执行javascript脚本
                if (!StringUtils.isEmpty(javaScript)) {
                    scriptEngine.eval(javaScript);
                    Invocable invocable = (Invocable) scriptEngine;
                    Object result = invocable.invokeFunction("run", JSON.toJSONString(itemData));
                    // 返回值可在下面这行代码查看(js脚本的返回值，调试使用)
                    logger.info(JSON.toJSONString(result));
                }
            }
        } catch (Exception e) {
            logger.error("调度服务异常：{}" + e.getMessage());
        }
        logger.info("调度服务结束...........");
    }

}
