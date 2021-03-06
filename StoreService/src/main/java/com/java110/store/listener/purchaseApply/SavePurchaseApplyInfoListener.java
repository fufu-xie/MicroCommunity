package com.java110.store.listener.purchaseApply;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.java110.core.annotation.Java110Listener;
import com.java110.core.context.DataFlowContext;
import com.java110.core.factory.GenerateCodeFactory;
import com.java110.entity.center.Business;
import com.java110.store.dao.IPurchaseApplyServiceDao;
import com.java110.utils.constant.BusinessTypeConstant;
import com.java110.utils.constant.StatusConstant;
import com.java110.utils.util.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 保存 采购申请信息 侦听
 * Created by wuxw on 2018/5/18.
 */
@Java110Listener("savePurchaseApplyInfoListener")
@Transactional
public class SavePurchaseApplyInfoListener extends AbstractPurchaseApplyBusinessServiceDataFlowListener{

    private static Logger logger = LoggerFactory.getLogger(SavePurchaseApplyInfoListener.class);

    @Autowired
    private IPurchaseApplyServiceDao purchaseApplyServiceDaoImpl;

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getBusinessTypeCd() {
        return BusinessTypeConstant.BUSINESS_TYPE_SAVE_PURCHASE_APPLY;
    }

    /**
     * 保存采购申请信息 business 表中
     * @param dataFlowContext 数据对象
     * @param business 当前业务对象
     */
    @Override
    protected void doSaveBusiness(DataFlowContext dataFlowContext, Business business) {
        JSONObject data = business.getDatas();
        Assert.notEmpty(data,"没有datas 节点，或没有子节点需要处理");

        //处理 businessPurchaseApply 节点
        if(data.containsKey("businessPurchaseApply")){
            Object bObj = data.get("businessPurchaseApply");
            JSONArray businessPurchaseApplys = null;
            if(bObj instanceof JSONObject){
                businessPurchaseApplys = new JSONArray();
                businessPurchaseApplys.add(bObj);
            }else {
                businessPurchaseApplys = (JSONArray)bObj;
            }
            //JSONObject businessPurchaseApply = data.getJSONObject("businessPurchaseApply");
            for (int bPurchaseApplyIndex = 0; bPurchaseApplyIndex < businessPurchaseApplys.size();bPurchaseApplyIndex++) {
                JSONObject businessPurchaseApply = businessPurchaseApplys.getJSONObject(bPurchaseApplyIndex);
                doBusinessPurchaseApply(business, businessPurchaseApply);
                if(bObj instanceof JSONObject) {
                    dataFlowContext.addParamOut("applyOrderId", businessPurchaseApply.getString("applyOrderId"));
                }
            }
        }
    }

    /**
     * business 数据转移到 instance
     * @param dataFlowContext 数据对象
     * @param business 当前业务对象
     */
    @Override
    protected void doBusinessToInstance(DataFlowContext dataFlowContext, Business business) {
        JSONObject data = business.getDatas();

        Map info = new HashMap();
        info.put("bId",business.getbId());
        info.put("operate",StatusConstant.OPERATE_ADD);

        //采购申请信息
        List<Map> businessPurchaseApplyInfo = purchaseApplyServiceDaoImpl.getBusinessPurchaseApplyInfo(info);
        if( businessPurchaseApplyInfo != null && businessPurchaseApplyInfo.size() >0) {
            reFreshShareColumn(info, businessPurchaseApplyInfo.get(0));
            purchaseApplyServiceDaoImpl.savePurchaseApplyInfoInstance(info);
            if(businessPurchaseApplyInfo.size() == 1) {
                dataFlowContext.addParamOut("applyOrderId", businessPurchaseApplyInfo.get(0).get("apply_order_id"));
            }
        }
    }


    /**
     * 刷 分片字段
     *
     * @param info         查询对象
     * @param businessInfo 小区ID
     */
    private void reFreshShareColumn(Map info, Map businessInfo) {

        if (info.containsKey("storeId")) {
            return;
        }

        if (!businessInfo.containsKey("store_id")) {
            return;
        }

        info.put("storeId", businessInfo.get("store_id"));
    }
    /**
     * 撤单
     * @param dataFlowContext 数据对象
     * @param business 当前业务对象
     */
    @Override
    protected void doRecover(DataFlowContext dataFlowContext, Business business) {
        String bId = business.getbId();
        //Assert.hasLength(bId,"请求报文中没有包含 bId");
        Map info = new HashMap();
        info.put("bId",bId);
        info.put("statusCd",StatusConstant.STATUS_CD_VALID);
        Map paramIn = new HashMap();
        paramIn.put("bId",bId);
        paramIn.put("statusCd",StatusConstant.STATUS_CD_INVALID);
        //采购申请信息
        List<Map> purchaseApplyInfo = purchaseApplyServiceDaoImpl.getPurchaseApplyInfo(info);
        if(purchaseApplyInfo != null && purchaseApplyInfo.size() > 0){
            reFreshShareColumn(paramIn, purchaseApplyInfo.get(0));
            purchaseApplyServiceDaoImpl.updatePurchaseApplyInfoInstance(paramIn);
        }
    }



    /**
     * 处理 businessPurchaseApply 节点
     * @param business 总的数据节点
     * @param businessPurchaseApply 采购申请节点
     */
    private void doBusinessPurchaseApply(Business business,JSONObject businessPurchaseApply){

        Assert.jsonObjectHaveKey(businessPurchaseApply,"applyOrderId","businessPurchaseApply 节点下没有包含 applyOrderId 节点");

        if(businessPurchaseApply.getString("applyOrderId").startsWith("-")){
            //刷新缓存
            //flushPurchaseApplyId(business.getDatas());

            businessPurchaseApply.put("applyOrderId",GenerateCodeFactory.getGeneratorId(GenerateCodeFactory.CODE_PREFIX_applyOrderId));

        }

        businessPurchaseApply.put("bId",business.getbId());
        businessPurchaseApply.put("operate", StatusConstant.OPERATE_ADD);
        //保存采购申请信息
        purchaseApplyServiceDaoImpl.saveBusinessPurchaseApplyInfo(businessPurchaseApply);

    }

    public IPurchaseApplyServiceDao getPurchaseApplyServiceDaoImpl() {
        return purchaseApplyServiceDaoImpl;
    }

    public void setPurchaseApplyServiceDaoImpl(IPurchaseApplyServiceDao purchaseApplyServiceDaoImpl) {
        this.purchaseApplyServiceDaoImpl = purchaseApplyServiceDaoImpl;
    }
}
