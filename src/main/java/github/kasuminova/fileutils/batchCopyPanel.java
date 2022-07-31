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

import static github.kasuminova.fileutils.FileUtil.copyFile;
import static github.kasuminova.fileutils.FileUtil.multiThreadedCopyFile;
import static github.kasuminova.fileutils.Main.targetFile;
import static github.kasuminova.fileutils.Main.mainDir;

public class batchCopyPanel {
    public static List<String> dirList1List = new ArrayList<>();
    JButton start1 = new JButton("开始复制（默认配置）");
    JButton start2 = new JButton("开始复制（临时配置）");
    JTextField mainDirTextField = new JTextField();
    JLabel mainDirLabel = new JLabel("主文件夹: ");
    /**
     * 创建一个批量复制器的面板
     * @param fileUtils 程序主面板
     * @return 批量复制器面板
     */
    public JPanel createPanel(JFrame fileUtils) {
        //主界面
        JPanel batchCopyUtilPanel = new JPanel(new VFlowLayout());
        mainDirTextField.setText(mainDir);
        Box mainDirBox = Box.createHorizontalBox();
        mainDirBox.add(mainDirLabel);
        mainDirBox.add(mainDirTextField);

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

        dirList1.setVisibleRowCount(10);
        dirList2.setVisibleRowCount(10);

        dirList1.setFixedCellWidth(240);
        dirList2.setFixedCellWidth(240);

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

        JCheckBox useMultiThread = new JCheckBox("实验性: 启用多线程复制", false);
        useMultiThread.setToolTipText("使用多线程可以大大加快复制大量文件的速度(最高 300%)，但是可能存在未知问题。");

        //开始操作文件
        start1.addActionListener(e -> {
            final String mainDirTmp = mainDirTextField.getText();
            if (new File("./" + mainDirTmp).exists()) {
                long[] fileCount = new FileCounter.fileCounter().getDirFileCountAndTotalSize(mainDir);
                if (fileCount[0] == 0 && fileCount[2] == 0) {
                    JOptionPane.showMessageDialog(fileUtils,"主文件夹没有需要复制的文件或文件夹。", "错误", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                statusLabel.setText("状态: 操作进行中");
                if (useMultiThread.isSelected()) {
                    multiThreadedCopyFile(mainDirTmp, targetFile, fileUtils, statusBar, statusLabel);
                } else {
                    //详见复制文件类
                    copyFile(mainDirTmp, targetFile, fileUtils, statusBar, statusLabel);
                }
            } else {
                JOptionPane.showMessageDialog(fileUtils,"主文件夹不存在。", "错误", JOptionPane.ERROR_MESSAGE);
            }

        });
        dirList1Panel.add(start1);

        JPanel dirList2Panel = new JPanel(new VFlowLayout());
        dirList2Panel.setBorder(BorderFactory.createTitledBorder("目标文件夹（临时配置）"));
        dirList2Panel.add(dirList2ScrollPane);

        start2.addActionListener(e -> {
            final String mainDirTmp = mainDirTextField.getText();
            if (dirList1List != null && dirList1List.size() > 0){
                if (new File("./" + mainDirTmp).exists()) {
                    long[] fileCount = new FileCounter.fileCounter().getDirFileCountAndTotalSize(mainDir);
                    if (fileCount[0] == 0 && fileCount[2] == 0) {
                        JOptionPane.showMessageDialog(fileUtils,"主文件夹没有需要复制的文件或文件夹。", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    statusLabel.setText("状态: 操作进行中");
                    if (useMultiThread.isSelected()) {
                            multiThreadedCopyFile(mainDirTmp, dirList1List, fileUtils, statusBar, statusLabel);
                    } else {
                        //详见复制文件类
                        copyFile(mainDirTmp, dirList1List, fileUtils, statusBar, statusLabel);
                    }
                } else {
                    JOptionPane.showMessageDialog(fileUtils,"主文件夹不存在。", "错误", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(fileUtils,"临时配置为空。", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });
        dirList2Panel.add(start2);

        Box dirListBox = Box.createHorizontalBox();
        dirListBox.add(dirList1Panel);
        dirListBox.add(dirList2Panel);

        //主界面
        batchCopyUtilPanel.add(mainDirBox);
        batchCopyUtilPanel.add(dirListBox);
        batchCopyUtilPanel.add(useMultiThread);
        batchCopyUtilPanel.add(bottomStatusPanel);

        return batchCopyUtilPanel;
    }
}
