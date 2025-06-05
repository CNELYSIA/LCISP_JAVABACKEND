package com.plat.backend.controller;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * 系统消息工具类
 *
 **/
public class MonitorController {
    private static final int OSHI_WAIT_SECOND = 1000;
    private static SystemInfo systemInfo = new SystemInfo();
    private static HardwareAbstractionLayer hardware = systemInfo.getHardware();
    private static OperatingSystem operatingSystem = systemInfo.getOperatingSystem();

    public static JSONObject getCpuInfo() {
        JSONObject cpuInfo = new JSONObject();
        CentralProcessor processor = hardware.getProcessor();
        // CPU信息
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        Util.sleep(OSHI_WAIT_SECOND);
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()] - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()] - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softirq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()] - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()] - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()] - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long user = ticks[CentralProcessor.TickType.USER.getIndex()] - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long iowait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()] - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()] - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;
        //cpu核数
        cpuInfo.put("cpuNum", processor.getLogicalProcessorCount());
        //cpu系统使用率
        cpuInfo.put("cSys", new DecimalFormat("#.##%").format(cSys * 1.0 / totalCpu));
        //cpu用户使用率
        cpuInfo.put("user", new DecimalFormat("#.##%").format(user * 1.0 / totalCpu));
        //cpu当前等待率
        cpuInfo.put("iowait", new DecimalFormat("#.##%").format(iowait * 1.0 / totalCpu));
        //cpu当前使用率
        cpuInfo.put("idle", new DecimalFormat("#.##%").format(1.0 - (idle * 1.0 / totalCpu)));
        return cpuInfo;
    }

    /**
     * 系统jvm信息
     */
    public static JSONObject getJvmInfo() {
        JSONObject jvmInfo = new JSONObject();
        Properties props = System.getProperties();
        Runtime runtime = Runtime.getRuntime();
        long jvmTotalMemoryByte = runtime.totalMemory();
        long freeMemoryByte = runtime.freeMemory();
        //jvm总内存
        jvmInfo.put("total", formatByte(runtime.totalMemory()));
        //空闲空间
        jvmInfo.put("free", formatByte(runtime.freeMemory()));
        //jvm最大可申请
        jvmInfo.put("max", formatByte(runtime.maxMemory()));
        //vm已使用内存
        jvmInfo.put("user", formatByte(jvmTotalMemoryByte - freeMemoryByte));
        //jvm内存使用率
        jvmInfo.put("usageRate", new DecimalFormat("#.##%").format((jvmTotalMemoryByte - freeMemoryByte) * 1.0 / jvmTotalMemoryByte));
        //jdk版本
        jvmInfo.put("jdkVersion", props.getProperty("java.version"));
        //jdk路径
        jvmInfo.put("jdkHome", props.getProperty("java.home"));

        long time = ManagementFactory.getRuntimeMXBean().getStartTime();
        Date date = new Date(time);

        //运行多少分钟
        long runMS = DateUtil.between(date, new Date(), DateUnit.MS);

        long nd = 1000 * 24 * 60 * 60;
        long nh = 1000 * 60 * 60;
        long nm = 1000 * 60;

        long day = runMS / nd;
        long hour = runMS % nd / nh;
        long min = runMS % nd % nh / nm;
        jvmInfo.put("runTime", (day + "天" + hour + "小时" + min + "分钟"));

        return jvmInfo;
    }

    /**
     * 系统内存信息
     */
    public static JSONObject getMemInfo() {
        JSONObject memInfo = new JSONObject();
        GlobalMemory memory = systemInfo.getHardware().getMemory();
        //总内存
        long totalByte = memory.getTotal();
        //剩余
        long acaliableByte = memory.getAvailable();
        //总内存
        memInfo.put("total", formatByte(totalByte));
        //使用
        memInfo.put("used", formatByte(totalByte - acaliableByte));
        //剩余内存
        memInfo.put("free", formatByte(acaliableByte));
        //使用率
        memInfo.put("usageRate", new DecimalFormat("#.##%").format((totalByte - acaliableByte) * 1.0 / totalByte));
        return memInfo;
    }

    /**
     * 系统盘符信息
     */
    public static JSONArray getSysFileInfo() {
        JSONObject sysFileInfo;
        JSONArray sysFiles = new JSONArray();
        FileSystem fileSystem = operatingSystem.getFileSystem();
        List<OSFileStore> fsList = fileSystem.getFileStores();
        OSFileStore[] fsArray = fsList.toArray(new OSFileStore[fsList.size()]);
        for (OSFileStore fs : fsArray) {
            sysFileInfo = new JSONObject();
            //盘符路径
            sysFileInfo.put("dirName", fs.getMount());
            //盘符类型
            sysFileInfo.put("sysTypeName", fs.getType());
            //文件类型
            sysFileInfo.put("typeName", fs.getName());
            //总大小
            sysFileInfo.put("total", formatByte(fs.getTotalSpace()));
            //剩余大小
            sysFileInfo.put("free", formatByte(fs.getUsableSpace()));
            //已经使用量
            sysFileInfo.put("used", formatByte(fs.getTotalSpace() - fs.getUsableSpace()));
            if (fs.getTotalSpace() == 0) {
                //资源的使用率
                sysFileInfo.put("usage", 0);
            } else {
                sysFileInfo.put("usage",new DecimalFormat("#.##%").format((fs.getTotalSpace() - fs.getUsableSpace()) * 1.0 / fs.getTotalSpace()));
            }
            sysFiles.add(sysFileInfo);
        }
        return sysFiles;
    }

    /**
     * 系统信息
     */
    public static JSONObject getSysInfo() throws UnknownHostException {
        JSONObject sysInfo = new JSONObject();
        Properties props = System.getProperties();
        //操作系统名
        sysInfo.put("osName", props.getProperty("os.name"));
        //系统架构
        sysInfo.put("osArch", props.getProperty("os.arch"));
        //服务器名称
        sysInfo.put("computerName", InetAddress.getLocalHost().getHostName());
        //服务器Ip
        sysInfo.put("computerIp", InetAddress.getLocalHost().getHostAddress());
        //项目路径
        sysInfo.put("userDir", props.getProperty("user.dir"));
        return sysInfo;
    }

    /**
     * 所有系统信息
     */
    public static JSONObject getInfo() throws UnknownHostException {
        JSONObject info = new JSONObject();
        info.put("cpuInfo", getCpuInfo());
        info.put("jvmInfo", getJvmInfo());
        info.put("memInfo", getMemInfo());
        info.put("sysInfo", getSysInfo());
        info.put("sysFileInfo", getSysFileInfo());
        info.put("graphicsInfo", getGraphicsInfo());
        return info;
    }

    public static JSONArray getGraphicsInfo() {
        JSONObject graphicsinfo;
        JSONArray GraphicsArray = new JSONArray();
        List<GraphicsCard> GraphicsCardLists = hardware.getGraphicsCards();
        GraphicsCard[] GraphicsCards = GraphicsCardLists.toArray(new GraphicsCard[GraphicsCardLists.size()]);
        for (GraphicsCard gc : GraphicsCards) {
            graphicsinfo = new JSONObject();
            //显卡id
            graphicsinfo.put("deviceId", gc.getDeviceId());
            //显卡名称
            graphicsinfo.put("deviceName", gc.getName());
            //显卡版本信息
            graphicsinfo.put("deviceVersion", gc.getVersionInfo());
            //显卡供应商
            graphicsinfo.put("deviceVendor", gc.getVendor());
            //显存信息
            graphicsinfo.put("deviceVRam", formatByte(gc.getVRam()));
            GraphicsArray.add(graphicsinfo);
        }
        return GraphicsArray;
    }
    /**
     * 单位转换
     * 单位转换
     */
    private static String formatByte(long byteNumber) {
        //换算单位
        double FORMAT = 1024.0;
        double kbNumber = byteNumber / FORMAT;
        if (kbNumber < FORMAT) {
            return new DecimalFormat("#.##KB").format(kbNumber);
        }
        double mbNumber = kbNumber / FORMAT;
        if (mbNumber < FORMAT) {
            return new DecimalFormat("#.##MB").format(mbNumber);
        }
        double gbNumber = mbNumber / FORMAT;
        if (gbNumber < FORMAT) {
            return new DecimalFormat("#.##GB").format(gbNumber);
        }
        double tbNumber = gbNumber / FORMAT;
        return new DecimalFormat("#.##TB").format(tbNumber);
    }
}