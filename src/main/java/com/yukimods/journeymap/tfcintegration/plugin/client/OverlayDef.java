package com.yukimods.journeymap.tfcintegration.plugin.client;

import journeymap.api.v2.client.fullscreen.IThemeButton;

/**
 * 单个叠加层的完整描述，仅客户端使用。
 * 服务端不引用此类。
 */
public class OverlayDef {

    public final String id;
    public final String name;
    public final String desc;
    public final String iconPath;
    public IThemeButton button;

    public OverlayDef(String id, String name, String desc, String iconPath) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.iconPath = iconPath;
    }
}