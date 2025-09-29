package com.pushwoosh.inapp.view.config;

import com.pushwoosh.inapp.network.model.Resource;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.richmedia.RichMediaManager;
import java.util.Set;

public class ModalConfigResolver {
    private static final String TAG = "ModalConfigResolver";

    private static <T> T getValueOrDefault(T resourceValue, T globalValue) {
        return resourceValue != null ? resourceValue : globalValue;
    }
    
    private static <T> Set<T> getSetValueOrDefault(Set<T> resourceValue, Set<T> globalValue) {
        return (resourceValue != null && !resourceValue.isEmpty()) ? resourceValue : globalValue;
    }

    public static ModalRichmediaConfig getEffectiveConfig(Resource resource) {
        PWLog.noise(TAG, "getEffectiveConfig for resource: " + resource.getCode());
        ModalRichmediaConfig globalConfig = RichMediaManager.getDefaultRichMediaConfig();
        
        if (!resource.hasResourceModalConfig()) {
            PWLog.noise(TAG, "No resource config, using global config");
            return globalConfig;
        }
        
        ModalRichmediaConfig resourceConfig = resource.getResourceModalConfig();
        ModalRichmediaConfig effectiveConfig = new ModalRichmediaConfig();
        
        effectiveConfig.setViewPosition(getValueOrDefault(
            resourceConfig.getViewPosition(), globalConfig.getViewPosition()));
            
        effectiveConfig.setPresentAnimationType(getValueOrDefault(
            resourceConfig.getPresentAnimationType(), globalConfig.getPresentAnimationType()));
            
        effectiveConfig.setDismissAnimationType(getValueOrDefault(
            resourceConfig.getDismissAnimationType(), globalConfig.getDismissAnimationType()));
            
        effectiveConfig.setSwipeGestures(getSetValueOrDefault(
            resourceConfig.getSwipeGestures(), globalConfig.getSwipeGestures()));
            
        effectiveConfig.setWindowWidth(getValueOrDefault(
            resourceConfig.getWindowWidth(), globalConfig.getWindowWidth()));
            
        effectiveConfig.setAnimationDuration(getValueOrDefault(
            resourceConfig.getAnimationDuration(), globalConfig.getAnimationDuration()));
            
        effectiveConfig.setStatusBarCovered(getValueOrDefault(
            resourceConfig.isStatusBarCovered(), globalConfig.isStatusBarCovered()));

        effectiveConfig.setRespectEdgeToEdgeLayout(getValueOrDefault(
                resourceConfig.shouldRespectEdgeToEdgeLayout(),
                globalConfig.shouldRespectEdgeToEdgeLayout()
        ));
        
        return effectiveConfig;
    }
}