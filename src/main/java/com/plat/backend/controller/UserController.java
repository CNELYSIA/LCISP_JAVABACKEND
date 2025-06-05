package com.plat.backend.controller;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Map;


@CrossOrigin
@RestController
public class UserController {

    public String[] typeArray = new String[]{"耕地", "道路", "自训练模型"};

    @RequestMapping("info")
    public JSONObject sysInfo() throws UnknownHostException {
        return MonitorController.getInfo();
    }

    @RequestMapping("type")
    public JSONArray TypeList(){
        JSONObject type;
        JSONArray TypeList = new JSONArray();
        for (String item : typeArray){
            type= new JSONObject();
            type.put("label", item);
            type.put("value", item);
            TypeList.add(type);
        }
        return TypeList;
    }

    @RequestMapping("model")
    public JSONArray ModelList(@RequestParam("userType") String userType){
        JSONObject model;
        JSONArray ModelList = new JSONArray();
        String[] farmland_Model = new String[]{"UNet", "LinkNet","SGCNNet"};
        String[] road_Model = new String[]{"CONet", "LinkNet"};
        String[] selected_Models = switch (userType) {
            case "耕地" -> farmland_Model;
            case "道路" -> road_Model;
            default -> null;
        };
        if (selected_Models != null) {
            for (String item : selected_Models){
                model= new JSONObject();
                model.put("label", item);
                model.put("value", item);
                ModelList.add(model);
            }
        }
        return ModelList;
    }

    @RequestMapping("option")
    public JSONObject OptionList(){
        JSONObject OptionList = new JSONObject();
        String[] Sensor = new String[]{"COPERNICUS/S2_HARMONIZED", "COPERNICUS/S2_SR_HARMONIZED", "LANDSAT/LC08/C02/T1_L2", "LANDSAT/LC09/C02/T1_L2"};
        String[] Filter = new String[]{"CLOUDY_PIXEL_PERCENTAGE", "CLOUD_COVER"};
        String[] Crs = new String[]{"EPSG:4326", "EPSG:3857"};
        OptionList.put("Sensor", Sensor);
        OptionList.put("Filter", Filter);
        OptionList.put("Crs", Crs);
        return OptionList;
    }

    @GetMapping("/download/{Name}")
    public void DownloadLink(@PathVariable String Name, HttpServletResponse response) throws IOException {

        String path = "..//Resource//Tif//" + Name;
        InputStream inputStream = new FileInputStream(path);// 文件的存放路径
        response.reset();
        response.setContentType("application/octet-stream");
        String filename = new File(path).getName();
        response.addHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"));
        ServletOutputStream outputStream = response.getOutputStream();
        byte[] b = new byte[1024];
        int len;
        //从输入流中读取一定数量的字节，并将其存储在缓冲区字节数组中，读到末尾返回-1
        while ((len = inputStream.read(b)) > 0) {
            outputStream.write(b, 0, len);
        }
        inputStream.close();
    }

    @RequestMapping("/source/{Source}")
    public JSONArray SourceList(@PathVariable String Source){
        String[] SourceList  = new String[]{"UNet", "LinkNet","SGCNNet"};
        JSONObject model;
        JSONArray ModelList = new JSONArray();
        if (Integer.parseInt(Source) == 0){
            for (String item : SourceList){
                model= new JSONObject();
                model.put("label", item);
                model.put("value", item);
                ModelList.add(model);
            }
        }else{
            ModelList = new JSONArray();
        }
        return ModelList;
    }

    @RequestMapping("/log")
    @ResponseBody
    public String showLogs(@RequestBody Map<String,Object> CONFIG) throws IOException {

        String path = new String();
        int userType = (int) CONFIG.get("userType");
        String userLog = (String) CONFIG.get("userLog");
        String model = (String) CONFIG.get("userModel");
        if (userType == 0)
            path = "../User/" + model + ".log";
        else if (userType == 1)
            path = userLog + "/" + model + ".log";

            BufferedReader in = new BufferedReader(new FileReader(path));
            StringBuilder stringBuilder = new StringBuilder(); // 使用 StringBuilder 来构建字符串
            String line;
            while ((line = in.readLine()) != null) {
                stringBuilder.append(line).append("\n"); // 使用 append 方法追加内容
            }
            return stringBuilder.toString(); // 返回包含所有行内容的字符串
    }
}


