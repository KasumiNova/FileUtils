package github.kasuminova.fileutils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static github.kasuminova.fileutils.FileUtil.batchBackupAndDelete;
import static github.kasuminova.fileutils.Main.targetFile;
import static github.kasuminova.fileutils.Main.mainDir;

public class batchDeletePanel {
    public static List<String> dirList1List = new ArrayList<>();
    JButton start1 = new JButton("开始删除（默认配置）");
    JButton start2 = new JButton("开始删除（临时配置）");
    JLabel backupDirLabel = new JLabel("备份文件夹: ");
    JTextField backupDirTextField = new JTextField();
    JLabel targetDeleteFileOrDirLabel = new JLabel("输入要删除的文件/文件夹: ");
    JTextField targetDeleteFileOrDir = new JTextField();
    JScrollPane targetDeleteFileOrDirScrollPane = new JScrollPane(targetDeleteFileOrDir,JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    public JPanel createPanel(JFrame fileUtils) {
        //主界面
        JPanel batchDeleteUtilPanel = new JPanel(new VFlowLayout());
        backupDirTextField.setText("FileUtils/Backup");

        //备份文件夹
        Box mainDirBox = Box.createHorizontalBox();
        mainDirBox.add(backupDirLabel);
        mainDirBox.add(backupDirTextField);

        //要删除的文件/文件夹
        Box targetDeleteFileOrDirBox = Box.createHorizontalBox();
        targetDeleteFileOrDirBox.add(targetDeleteFileOrDirLabel);
        targetDeleteFileOrDirBox.add(targetDeleteFileOrDirScrollPane);

        //文件夹列表
        String[] copyToDirPathArr = targetFile.toArray(new String[targetFile.size()]);
        JList<String> dirList1 = new JList<>(copyToDirPathArr);
        JList<String> dirList2 = new JList<>();

        //右键菜单
        JPopupMenu dirList1Menu = new JPopupMenu();
        JPopupMenu dirList2Menu = new JPopupMenu();
        JMenuItem moveDirList1ToDirList2 = new JMenuItem("移动左侧选中的项目至右边");
        JMenuItem addNewDir = new JMenuItem("添加文件夹");

        ActionListener addNewDirListener = e -> {
            String str = JOptionPane.showInputDialog(fileUtils,"请输入目标文件夹名称: ", "提示",JOptionPane.INFORMATION_MESSAGE);
            if (str != null && !str.isEmpty()) {
                if (str.equals(mainDir)) {
                    JOptionPane.showMessageDialog(fileUtils, "目标文件夹名称与主文件夹相同。", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                File dir = new File("./" + str);
                if (!dir.exists()) {
                    JOptionPane.showMessageDialog(fileUtils, "目标文件夹不存在。", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                if (dir.isFile()) {
                    JOptionPane.showMessageDialog(fileUtils, "目标是一个文件而非文件夹。", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                dirList1List.add(str);

                //去重
                dirList1List = dirList1List.stream().distinct().collect(Collectors.toList());
                dirList2.setListData(dirList1List.toArray(new String[dirList1List.size()]));
            }
        };
        addNewDir.addActionListener(addNewDirListener);
        dirList2Menu.add(addNewDir);
        dirList2.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()){
                    //显示 popupMenu
                    dirList2Menu.show(dirList2,e.getX(),e.getY());
                }
            }
        });

        ActionListener moveDirList1ToDirList2Listener = e -> {
            dirList1List = dirList1.getSelectedValuesList();
            //如果选中的内容不为空，则设置右侧的内容为左侧的内容
            if ((dirList1List.size() > 0)){
                dirList2.setListData(dirList1List.toArray(new String[dirList1List.size()]));
            } else {
                JOptionPane.showMessageDialog(fileUtils, "请先在左侧列表中选择条目后再使用此功能。", "错误", JOptionPane.ERROR_MESSAGE);
            }
        };
        moveDirList1ToDirList2.addActionListener(moveDirList1ToDirList2Listener);

        dirList1Menu.add(moveDirList1ToDirList2);
        dirList1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()){
                    //显示 popupMenu
                    dirList1Menu.show(dirList1,e.getX(),e.getY());
                }
            }
        });

        dirList1.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        dirList1.setVisibleRowCount(11);
        dirList2.setVisibleRowCount(11);

        dirList1.setFixedCellWidth(215);
        dirList2.setFixedCellWidth(215);

        String[] info = {"从左侧选择项目后, 右键添加"};
        dirList2.setListData(info);

        JScrollPane dirList1ScrollPane = new JScrollPane(dirList1);
        JScrollPane dirList2ScrollPane = new JScrollPane(dirList2);

        JPanel dirList1Panel = new JPanel(new VFlowLayout());
        dirList1Panel.setBorder(BorderFactory.createTitledBorder("目标文件夹（默认配置）"));
        dirList1Panel.add(dirList1ScrollPane);

        //创建一个小进度条置于窗口右下角，用于显示进度
        JPanel statusLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel statusBarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JLabel statusLabel = new JLabel("状态: 等待中");
        statusLabelPanel.add(statusLabel);

        JProgressBar statusBar = new JProgressBar();
        statusBar.setStringPainted(true);
        statusBar.setString("无任务");
        statusBarPanel.add(statusBar);

        JPanel bottomStatusPanel = new JPanel(new BorderLayout());
        bottomStatusPanel.add(statusLabelPanel, BorderLayout.WEST);
        bottomStatusPanel.add(statusBarPanel, BorderLayout.EAST);

        //开始操作文件
        start1.addActionListener(e -> {
            String mainDirTmp = backupDirTextField.getText();
            if (!mainDirTmp.isEmpty() && new File("./" + mainDirTmp).exists()) {
                if (securityCheck(targetDeleteFileOrDir.getText(), targetFile, fileUtils)) {
                    statusLabel.setText("状态: 操作进行中");
                    //创建临时路径变量
                    List<String> targetFileTmp = new ArrayList<>();
                    //为临时路径变量添加上删除的路径
                    for (String s : targetFile) {
                        targetFileTmp.add(s + "/" + targetDeleteFileOrDir.getText());
                    }

                    //详见 FileUtil 类
                    batchBackupAndDelete(targetFileTmp, mainDirTmp, fileUtils, statusBar, statusLabel);
                }
            } else {
                JOptionPane.showMessageDialog(fileUtils, "备份文件夹输入框为空，或备份文件夹不存在。", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        dirList1Panel.add(start1);

        JPanel dirList2Panel = new JPanel(new VFlowLayout());
        dirList2Panel.setBorder(BorderFactory.createTitledBorder("目标文件夹（临时配置）"));
        dirList2Panel.add(dirList2ScrollPane);

        start2.addActionListener(e -> {
            String mainDirTmp = backupDirTextField.getText();
            if (!mainDirTmp.isEmpty() && new File("./" + mainDirTmp).exists()) {
                if (dirList1List != null && dirList1List.size() > 0) {
                    if (securityCheck(targetDeleteFileOrDir.getText(), dirList1List, fileUtils)) {
                        statusLabel.setText("状态: 操作进行中");
                        //创建临时路径变量
                        List<String> targetFileTmp = new ArrayList<>();
                        //为临时路径变量添加上删除的路径
                        for (String s : dirList1List) {
                            targetFileTmp.add(s + "/" + targetDeleteFileOrDir.getText());
                        }

                        //详见 FileUtil 类
                        batchBackupAndDelete(targetFileTmp, mainDirTmp, fileUtils, statusBar, statusLabel);
                    }
                } else {
                    JOptionPane.showMessageDialog(fileUtils, "临时配置为空。", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(fileUtils, "备份文件夹输入框为空，或备份文件夹不存在。", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        dirList2Panel.add(start2);

        Box dirListBox = Box.createHorizontalBox();
        dirListBox.add(dirList1Panel);
        dirListBox.add(dirList2Panel);

        //主界面
        batchDeleteUtilPanel.add(mainDirBox);
        batchDeleteUtilPanel.add(targetDeleteFileOrDirBox);
        batchDeleteUtilPanel.add(dirListBox);
        batchDeleteUtilPanel.add(bottomStatusPanel);

        return batchDeleteUtilPanel;
    }

    /**
     * 安全检测
     * @param target 要检测的字符串
     * @param rootDirList 根文件夹
     * @param fileUtils 主窗口
     * @return 通过为 true，未通过为 false
     */
    private static boolean securityCheck(String target, List<String> rootDirList, JFrame fileUtils) {
        //空内容检查
        if (target.isEmpty()) {
            JOptionPane.showMessageDialog(fileUtils, "输入内容为空。", "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        while (target.endsWith(".") || target.endsWith("/")) {
            //去除末尾的 "."
            if (target.endsWith(".")) {
                target = target.substring(0, target.length() - 1);
            }
            //去除末尾的 "/"
            if (target.endsWith("/")) {
                target = target.substring(0, target.length() - 1);
            }
        }

        //非法字符
        String[] unavailableStr = {":", "*", "?", "<", ">", "|"};
        //非法字符检查
        for (String s : unavailableStr) {
            if (target.contains(s)) {
                JOptionPane.showMessageDialog(fileUtils, "名称包含非法字符“" + s + "”。", "错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        //根文件夹检查
        for (String s : rootDirList) {
            String targetFile = new File("./" + s + "/").getAbsolutePath();
            System.out.println(targetFile);
            String rootDir = new File("./" + s + "/" + target).getAbsolutePath();
            System.out.println(rootDir);
            if (targetFile.equals(rootDir)) {
                JOptionPane.showMessageDialog(fileUtils, "不可删除根文件夹 " + rootDir + "。", "错误", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }

        //危险关键词
        String[] dangerousStr = {
                "plugins",
                "world",
                "server.properties"
        };
        //危险关键词检查
        for (String s : dangerousStr) {
            if (target.contains(s)) {
                int choice = JOptionPane.showConfirmDialog(fileUtils, "您所输入的路径存在危险关键词: “" + s + "”,你确定要继续批量删除操作吗？", "警告", JOptionPane.YES_NO_OPTION);
                //如果选择为是则继续执行命令，如果为否则中断执行
                return choice == JOptionPane.YES_OPTION;
            }
        }
        return true;
    }
}
