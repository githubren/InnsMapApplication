package com.example.yfsl.innsmapapplication;

import com.innsmap.InnsMap.INNSMapSDKResource;
import com.innsmap.InnsMap.net.http.domain.net.NetBuildingDetailBean;
import com.innsmap.InnsMap.net.http.domain.net.NetBuildingDetailFloorBean;
import com.innsmap.InnsMap.net.http.listener.forout.NetBuildingDetailListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 此类调用INNSMapResource.getBuildingDetail（）方法获取楼层信息
 * 将楼层信息放入到Map<String ,NetBuildingDetailFloorBean> floorMap集合中
 */


public class INNSMapSDKResourceUtil {
    private static INNSMapSDKResourceUtil instance;
    private String buildingId;
    private Map<String ,NetBuildingDetailFloorBean> floorMap;

    public INNSMapSDKResourceUtil(String buildingId) {
        this.buildingId = buildingId;
        loadFloorInfo(null);
    }

    public static INNSMapSDKResourceUtil getInstance(String buildingId){
        if (instance == null){
            synchronized (INNSMapSDKResourceUtil.class){
                if (instance == null){
                    instance = new INNSMapSDKResourceUtil(buildingId);
                }
            }
        }
        return instance;
    }

    public synchronized void loadFloorInfo(final ICallBack<Map<String,NetBuildingDetailFloorBean>> iCallBack) {
        if (floorMap == null){
            INNSMapSDKResource.getBuildingDetail(buildingId, new NetBuildingDetailListener() {
                @Override
                public void onSuccess(NetBuildingDetailBean netBuildingDetailBean) {
                    floorMap = new HashMap<>();
                    List<NetBuildingDetailFloorBean> overgroundList = netBuildingDetailBean.getOvergroundList();
                    for (NetBuildingDetailFloorBean overData : overgroundList){
                        floorMap.put(overData.getFloorId(),overData);
                    }
                    List<NetBuildingDetailFloorBean> undergroundList = netBuildingDetailBean.getUndergroundList();
                    for (NetBuildingDetailFloorBean underData : undergroundList){
                        floorMap.put(underData.getFloorId(),underData);
                    }
                    if (iCallBack != null){
                        iCallBack.onSuccess(floorMap);
                    }
                }

                @Override
                public void onFail(String s) {

                }
            });
        }
    }
}
