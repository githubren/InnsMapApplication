package com.example.yfsl.innsmapapplication;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;

import com.innsmap.InnsMap.INNSMapSDKResource;
import com.innsmap.InnsMap.INNSMapView;
import com.innsmap.InnsMap.location.bean.INNSMapLocation;
import com.innsmap.InnsMap.location.bean.Position;
import com.innsmap.InnsMap.map.sdk.domain.out.BitmapInformation;
import com.innsmap.InnsMap.map.sdk.domain.out.BitmapInformationFactory;
import com.innsmap.InnsMap.map.sdk.domain.overlay.BitmapOverlayer;
import com.innsmap.InnsMap.map.sdk.domain.overlay.Overlayer;
import com.innsmap.InnsMap.map.sdk.domain.overlay.PointOverlayer;
import com.innsmap.InnsMap.net.http.domain.net.NetBuildingDetailFloorBean;
import com.innsmap.InnsMap.net.http.listener.forout.NetMapLoadListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class MapView extends RelativeLayout {
    public MapView(Context context) {
        super(context);
    }

    public MapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context,attrs);
    }

    private INNSMapView innsMapView;//地图控件
    private RelativeLayout mapHintContainer;//“地图只能在航站楼内使用”提示容器
    private WheelView wheelView;//显示楼层的滑轮控件
    private String drawFloorId;//当前绘制楼层的id
    private INNSMapLocation innsMapLocation;
    private OnMapClickListener onMapClickListener;//地图点击监听器
    private OnLoadMapListener onLoadMapListener;//地图加载完成监听器
    private CardView wheelViewContainer;//包裹楼层的容器
    private boolean wheelViewDataInitSuccess;//楼层数据初始化完成记录器，用于地图绘制完成后显示
    private boolean wheelViewLoaded = false;//确保楼层只初始化一次
    private Map<String,List<List<PointF>>> recordSearchPathMap;//记录导航搜索的数据
    private Position startLocation;//记录开始导航的数据
    private Position endLocation;//记录结束导航的数据
    private OnFloorSwitchListenwe onFloorSwitchListenwe;//切换楼层监听器
    private List<NetBuildingDetailFloorBean> floorBeans;//滑轮数据
    private PointOverlayer pointOverlayer;//定位点信息
    private BitmapOverlayer bitmapOverlayer;//定位点坐标
    private int wheelVisible;

    private void init(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs,R.styleable.MapView);
        int wheelGravity = typedArray.getInt(R.styleable.MapView_wheel_view_gravity,1);
        int visible = typedArray.getInt(R.styleable.MapView_hint_visible,2);
        //滑轮默认显示
        wheelVisible = typedArray.getInt(R.styleable.MapView_wheel_visible,2);
        typedArray.recycle();
        View view = LayoutInflater.from(context).inflate(R.layout.map_view_layout,null,false);
        innsMapView = view.findViewById(R.id.inns_map);
        mapHintContainer = view.findViewById(R.id.map_hint_container);
        wheelViewContainer = view.findViewById(R.id.wheel_view_container);
        wheelView = view.findViewById(R.id.wheel_view);
        if (wheelVisible == 1){
            wheelViewContainer.setVisibility(GONE);
        }else {
            wheelViewContainer.setVisibility(VISIBLE);
        }
        if (visible == 1){
            mapHintContainer.setVisibility(GONE);
        }else {
            mapHintContainer.setVisibility(VISIBLE);
        }
        RelativeLayout.LayoutParams params = (LayoutParams) wheelViewContainer.getLayoutParams();
        switch (wheelGravity){
            case 1://右下
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                break;
            case 2://左下
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                break;
            case 3://左上
                params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
                break;
            case 4://右上
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                break;
            case 5://左中
                params.addRule(RelativeLayout.CENTER_VERTICAL);
                break;
            case 6://右中
                params.addRule(RelativeLayout.CENTER_VERTICAL);
                params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                break;
            default:
                break;
        }
        wheelViewContainer.requestLayout();
        addView(view);
    }

    public void removeAllOverlayer(){
        if (innsMapView != null){
            innsMapView.removeAllOverlayer();
        }
    }

    public void addOverlayer(Overlayer overlayer){
        if (innsMapView != null){
            innsMapView.addOverlayer(overlayer);
        }
    }

    public void removeOverlayer(Overlayer overlayer){
        if (innsMapView != null){
            innsMapView.removeOverlayer(overlayer);
        }
    }

    public void setOnMapClickListener(OnMapClickListener onMapClickListener){
        this.onMapClickListener = onMapClickListener;
    }

    public void setOnLoadMapListener(OnLoadMapListener onLoadMapListener){
        this.onLoadMapListener = onLoadMapListener;
    }

    public void setOnFloorSwitchListenwe(OnFloorSwitchListenwe onFloorSwitchListenwe){
        this.onFloorSwitchListenwe = onFloorSwitchListenwe;
    }

    /**
     * 画起点与终点
     */
    public void drawOverlayer(PointF pointF ,boolean endOrStart){
        BitmapOverlayer bitmapOverlayer = new BitmapOverlayer();
        bitmapOverlayer.setPointF(pointF);//单位是m
        BitmapInformation bmpOverlayer = BitmapInformationFactory.fromResource(R.drawable.icon_end_location);
        if (!endOrStart){
            bmpOverlayer = BitmapInformationFactory.fromResource(R.drawable.icon_start_location);
        }
        bitmapOverlayer.icon(bmpOverlayer);//放图形
        if (innsMapView != null){
            innsMapView.addOverlayer(bitmapOverlayer);
        }
    }

    /**
     * 画地图
     * @param mapClickListener    地图是否设置点击监听事件 true 设置监听
     * @param isDrawLocationInfo  是否绘制定位信息 true  绘制
     */

    private boolean recordMapClick;
    private boolean recordDrawLocation;

    public void drawMapAndMoveLocation(INNSMapLocation innsMapLocation,boolean mapClickListener,boolean isDrawLocationInfo){
        if (innsMapView == null) return;
        recordMapClick = mapClickListener;
        recordDrawLocation = isDrawLocationInfo;
        if (isDrawLocationInfo){//需要绘制定位信息
            drawLocationPoint(innsMapLocation);
        }
        //楼层ID相等，且楼层控件已画出
        if (TextUtils.equals(innsMapLocation.getFloorId(),drawFloorId) && wheelViewLoaded) return;
        //ID不相等时将传进来的innsMapLocation赋值给全局变量
        this.innsMapLocation = innsMapLocation;
        drawFloorId = innsMapLocation.getFloorId();
        if (! wheelViewLoaded){//确保楼层初始化一次
            wheelViewLoaded = true;
            initWheelView(innsMapLocation.getBuildingId());
        }
        innsMapView.responseRotate(false);//不响应地图旋转
        if (innsMapView == null || TextUtils.isEmpty(innsMapLocation.getBuildingId()) || TextUtils.isEmpty(innsMapLocation.getFloorId())){
            if (onLoadMapListener != null){
                onLoadMapListener.onFail("加载地图失败");
            }
            return;
        }
        innsMapView.loadMap(innsMapLocation.getBuildingId(), innsMapLocation.getFloorId(), new NetMapLoadListener() {//加载一张地图
            @Override
            public void onSuccess() {
                if (mapHintContainer == null) return;//地图加载成功的时候 隐藏上方的提示“地图需在航站楼内使用”
                mapHintContainer.setVisibility(GONE);
                if (innsMapView == null) return;
                innsMapView.setSiftOverlay(false);//不隐藏重叠的覆盖物
                if (wheelVisible != 1){//！=1那么就是 =2 显示
                    wheelViewContainer.setVisibility(VISIBLE);
                }
                if (mapClickListener){//地图点击监听  点击地图  绘制故障点
                    drawMalfMapPoint(new PointF(innsMapLocation.getX(),innsMapLocation.getY()));//调用的画故障点方法 跟画终点的方法一样
                    //直接调用画起点终点那个方法 第二个参数传入true 使其不画起点  达到与上面方法一样的效果
//                    drawOverlayer(new PointF(innsMapLocation.getX(),innsMapLocation.getY()),true);
                }
            }

            @Override
            public void onFail(String s) {

            }
        });
    }

    /**
     * 画故障点
     * @param pointF
     */
    private void drawMalfMapPoint(PointF pointF) {
        if (innsMapView == null) return;
        innsMapView.removeAllOverlayer();
        if (pointF.x == 0 && pointF.y == 0) return;
        BitmapOverlayer bitmapOverlayer = new BitmapOverlayer();
        bitmapOverlayer.setPointF(pointF);
        BitmapInformation bmpOverlayer = BitmapInformationFactory.fromResource(R.drawable.icon_end_location);
        bitmapOverlayer.icon(bmpOverlayer);
        innsMapView.addOverlayer(bitmapOverlayer);
    }


    /**
     * 初始化楼层
     * @param buildingId
     */
    private void initWheelView(String buildingId) {
        INNSMapSDKResourceUtil.getInstance(buildingId).loadFloorInfo(new ICallBack<Map<String, NetBuildingDetailFloorBean>>() {
            @Override
            public void onSuccess(Map<String, NetBuildingDetailFloorBean> object) {
                List<String> floor = new ArrayList<>();//存放楼层名字
                floorBeans = new ArrayList<>();//存放NetBuildingDetailFloorBean对象
                floorBeans.addAll(object.values());
                //将集合floorBeans排序
                Collections.sort(floorBeans, (o1, o2) -> {
                    String ol1 = o1.getFloorName();
                    String ol2 = o2.getFloorName();
                    if (ol1.startsWith("B") && ol2.startsWith("B")){
                        return ol1.compareTo(ol2);//两个值作比较 前者大于后者 返回1 相等返回0 小于返回-1
                    }
                    return ol2.compareTo(ol1);
                });
                int index = 0;
                for (int i =0;i<floorBeans.size();i++){
                    NetBuildingDetailFloorBean data = floorBeans.get(i);
                    floor.add(data.getFloorName());
                    if (TextUtils.equals(drawFloorId,data.getFloorId())){
                        index = i;
                    }
                }
                wheelViewDataInitSuccess = true;//滑轮数据初始化成功
                wheelView.setItems(floor);
                wheelView.setDefault(index);
                wheelView.setOnWheelViewListener(new WheelView.OnWheelViewListener(){//wheelView的点击监听
                    @Override
                    public void onSelected(int selectedIndex, String item) {
                        super.onSelected(selectedIndex, item);
                        if (TextUtils.equals(drawFloorId,floorBeans.get(selectedIndex).getFloorId())) return;
                        if (onFloorSwitchListenwe != null){
                            onFloorSwitchListenwe.selectedFloor(selectedIndex,item);
                        }
                        innsMapLocation.setFloorId(floorBeans.get(selectedIndex).getFloorId());
                        innsMapView.removeAllOverlayer();
                        drawMapAndMoveLocation(innsMapLocation,recordMapClick,recordDrawLocation);
                    }
                });
            }

            @Override
            public void onFail() {

            }
        });
    }

    /**
     * 绘制定位点坐标
     * @param innsMapLocation
     */
    private void drawLocationPoint(INNSMapLocation innsMapLocation) {
        if (innsMapLocation.getX() != 0 && innsMapLocation.getY() != 0){
            innsMapView.setPositionPoint(new PointF(innsMapLocation.getX(),innsMapLocation.getY()));
        }
    }
}
