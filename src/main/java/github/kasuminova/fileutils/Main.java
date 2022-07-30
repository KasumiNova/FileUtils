package github.kasuminova.fileutils;

import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    //版本号
    public static final String version = "1.5.2";
    JFrame fileUtils = new JFrame("文件批量管理工具 " + version);
    JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.LEFT,JTabbedPane.SCROLL_TAB_LAYOUT);
    static Map<String, Object> config;
    //主文件夹
    static String mainDir;
    //目标文件夹
    static List<String> targetFile;

    public static void main(String[] args) {
        SetupSwing.init();
        loadConfig();
        new Main().init();
    }
    //加载配置文件
    public static void loadConfig(){
        //旧配置文件
        File oldCfgDir = new File("./config");
        File oldCfgYml = new File("./config/config.yml");
        Path outPath = Paths.get("./config/config.yml");

        File cfgDir = new File("./FileUtils");
        File backupDir = new File("./FileUtils/Backup");

        if (!backupDir.exists()) backupDir.mkdir();
        if (!cfgDir.exists()) cfgDir.mkdir();

        if (oldCfgDir.exists()) {
            if (oldCfgYml.exists()) {
                try {
                    config = new Yaml().load(Files.newInputStream(outPath));
                    JOptionPane.showMessageDialog(null,"程序的配置文件位置已更新，但是程序找到了旧路径的配置文件，请及时迁移配置。\n路径：“FileUtils/config.yml”","注意",JOptionPane.INFORMATION_MESSAGE);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        File cfgYml = new File("./FileUtils/config.yml");
        Path path = Paths.get("./FileUtils/config.yml");
        if (!cfgYml.exists()){
            try {
                InputStream input = Main.class.getResourceAsStream("/FileUtils/config.yml");
                OutputStream output = Files.newOutputStream(path);
                byte[] buf = new byte[1024];
                int bytesRead;
                while ((bytesRead = input.read(buf)) > 0) {
                    output.write(buf, 0, bytesRead);
                }
                input.close();
                output.close();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,"无法提取程序内内必要的资源文件，请检查是否有程序占用当前程序路径的 config 文件夹，或稍后再试。","错误",JOptionPane.ERROR_MESSAGE);
                throw new RuntimeException(e);
            }
            JOptionPane.showMessageDialog(null,"未检测到配置文件，已在程序路径下创建配置文件，请配置文件内容后再次启动程序。\n路径: “FileUtils/config.yml”","注意",JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        }
        try {
            config = new Yaml().load(Files.newInputStream(path));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void init() {
        //读取配置文件
        targetFile = GetCFG("CopyToDirPath");
        mainDir = GetCFG("DirPath");

        //去重
        targetFile = targetFile.stream().distinct().collect(Collectors.toList());

        //移除与主文件夹相等的配置
        for (int i = 0; i < targetFile.size(); i++) {
            if (targetFile.get(i).equals(mainDir)) {
                targetFile.remove(i);
                i--;
            }
        }

        //主文件夹
        System.out.println("主文件夹: " + mainDir);

        //将被复制的文件夹
        for (String s : targetFile) {
            System.out.println("目标文件夹: " + s);
        }

        if (!new File("./" + mainDir).exists()){
            JOptionPane.showMessageDialog(fileUtils,"配置文件中的主文件夹配置无效（未找到文件夹）,请重新配置。","未找到文件夹",JOptionPane.ERROR_MESSAGE);
            System.exit(-1);
        }

        /*
         组装内容
         */
        //主窗口配置
        tabbedPane.add(new batchCopyPanel().createPanel(fileUtils),"批量复制器");
        tabbedPane.add(new batchDeletePanel().createPanel(fileUtils), "批量删除器");

        fileUtils.add(tabbedPane);
        fileUtils.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        fileUtils.pack();
        fileUtils.setResizable(false);
        fileUtils.setLocationRelativeTo(null);
        fileUtils.setVisible(true);
    }

    //配置读取器
    public static <T> T GetCFG(String key) {
        return (T) config.get(key);
    }
}
