package github.kasuminova.fileutils;


import javax.swing.*;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CopyFile {
    /**
     * 实验性：以多线程方式复制文件的方法
     * @param mainDir 主文件夹
     * @param copyToDirPath 被复制文件夹的地址列表
     * @param fileUtils 主窗口位置
     * @param statusBar 主窗口的右下角进度条进度
     * @param statusLabel 主窗口的左下角状态
     */
    public static void multiThreadedCopyFile(String mainDir, List<String> copyToDirPath, JFrame fileUtils, JProgressBar statusBar, JLabel statusLabel) {
        File mainDirFile = new File(mainDir);
        if (mainDirFile.exists()) {
            //计时器
            long startTime = System.currentTimeMillis();

            //计算大小
            long[] counts;
            counts = new FileCounter.fileCounter().getDirFileCountAndTotalSize(mainDir);
            System.out.println("counts:" + Arrays.toString(counts));

            int totalFileCount = (int) counts[0] * copyToDirPath.size();

            AtomicInteger totalProgress = new AtomicInteger();

            JFrame copyThreadFrame = new JFrame("请稍等...");
            copyThreadFrame.setSize(500, 115);

            JLabel installLabel = new JLabel("进程正在复制文件夹内容，进度: ");
            JProgressBar mainProgressBar = new JProgressBar(0, 100);
            mainProgressBar.setStringPainted(true);

            JPanel copyThreadPanel = new JPanel();
            copyThreadPanel.setLayout(new VFlowLayout());
            copyThreadPanel.add(installLabel);
            copyThreadPanel.add(mainProgressBar);

            copyThreadFrame.add(copyThreadPanel);
            copyThreadFrame.setLocationRelativeTo(fileUtils);

            //TODO:将复制方式改为多线程式的 FileChannel 并监听进度
            MultiThreadCopyDir multiCopyThread = new MultiThreadCopyDir();
            multiCopyThread.setSrcPath(mainDir);
            multiCopyThread.setDesPath(copyToDirPath);
            new Thread(multiCopyThread).start();

            Timer timer = new Timer(25, t -> {
                totalProgress.set((multiCopyThread.getCount() * 100) / totalFileCount);
                //如果操作时间超过 1 秒再弹出操作对话框
                if (startTime + 1000 <= System.currentTimeMillis() && !copyThreadFrame.isVisible()) {
                    copyThreadFrame.setVisible(true);
                }
                mainProgressBar.setString(multiCopyThread.getCount() + " / " + totalFileCount);
                mainProgressBar.setValue(totalProgress.get());
                statusBar.setString(String.format("%.2f", (double) (multiCopyThread.getCount() * 100) / totalFileCount) + "%");
                statusBar.setValue(totalProgress.get());
            });

            mainProgressBar.addChangeListener(e1 -> {
                if (mainProgressBar.getValue() >= mainProgressBar.getMaximum()) {
                    timer.stop();
                    long end = System.currentTimeMillis() - startTime;
                    System.out.println("多线程复制操作完成！");
                    JOptionPane.showMessageDialog(copyThreadFrame, "复制完成！\n耗时: " + String.format("%.2f", ((double) end / 1000)) + " 秒", "完成", JOptionPane.INFORMATION_MESSAGE);
                    statusBar.setValue(0);
                    statusBar.setString("无任务");
                    statusLabel.setText("状态：等待中");
                    copyThreadFrame.dispose();
                }
            });
            timer.start();
        }
    }

    /**
     * 批量复制文件的方法
     * @param mainDir 主文件夹
     * @param copyToDirPath 被复制文件夹的地址列表
     * @param fileUtils 主窗口位置
     * @param statusBar 主窗口的右下角进度条进度
     * @param statusLabel 主窗口的左下角状态
     */
    public static void copyFile(String mainDir, List<String> copyToDirPath, JFrame fileUtils, JProgressBar statusBar, JLabel statusLabel) {
        File mainDirFile = new File(mainDir);
        if (mainDirFile.exists()) {
            //计算大小
            long[] counts;
            //新文件操作线程
            CopyFileThread.copyFileThread copyFileThread = new CopyFileThread.copyFileThread();
            copyFileThread.setInputPath(mainDir);
            copyFileThread.setOutputPath(copyToDirPath);

            //TODO:将文件复制方式改为 FileChannel 并监听进度
            new Thread(copyFileThread).start();

            counts = new FileCounter.fileCounter().getDirFileCountAndTotalSize(mainDir);
            System.out.println("counts:" + Arrays.toString(counts));

            int totalFileCount = (int) counts[0] * copyToDirPath.size();
            long totalFileSize = counts[1] * copyToDirPath.size();
            //计时器
            long startTime = System.currentTimeMillis();
            //创建进度对话框
            JFrame copyThreadFrame = new JFrame("请稍等...");
            JLabel installLabel = new JLabel("进程正在复制文件夹内容，进度: ");
            JProgressBar dirsCountProgressBar = new JProgressBar(0, copyToDirPath.size());
            JLabel dirsNameLabel = new JLabel("当前正在粘贴的文件夹: ");
            JProgressBar mainProgressBar = new JProgressBar(0, 100);
            JProgressBar totalFileProgressBar = new JProgressBar(0, totalFileCount);
            totalFileProgressBar.setStringPainted(true);
            JLabel singleFileLabel = new JLabel("正在复制文件: ");
            JProgressBar singleFileProgressBar = new JProgressBar(0, 100);
            singleFileProgressBar.setStringPainted(true);
            JLabel speedLabel = new JLabel("速度: 0 MB/s");
            JPanel copyThreadPanel = new JPanel();

            copyThreadPanel.setLayout(new VFlowLayout());
            copyThreadPanel.add(installLabel);

            mainProgressBar.setStringPainted(true);
            dirsCountProgressBar.setStringPainted(true);

            copyThreadFrame.setSize(500, 300);

            copyThreadPanel.add(dirsCountProgressBar);
            copyThreadPanel.add(dirsNameLabel);
            copyThreadPanel.add(mainProgressBar);
            copyThreadPanel.add(totalFileProgressBar);
            copyThreadPanel.add(singleFileLabel);
            copyThreadPanel.add(singleFileProgressBar);
            copyThreadPanel.add(speedLabel);
            copyThreadFrame.add(copyThreadPanel);

            copyThreadFrame.setLocationRelativeTo(fileUtils);

            Timer timer = new Timer(25, t -> {
                //如果操作时间超过 1 秒再弹出操作对话框
                if (startTime + 1000 <= System.currentTimeMillis() && !copyThreadFrame.isVisible()) {
                    copyThreadFrame.setVisible(true);
                }
                //已复制的 Byte
                long byteProgress = copyFileThread.getProgress();
                //文件进度
                int progress = (int) ((byteProgress * 100) / totalFileSize);
                //单个文件大小
                long singleFileSize = copyFileThread.getSingleFileSize();
                //单个文件已复制的 Byte
                long singleFileByteProgress = copyFileThread.getSingleFileProgress();
                //单文件进度
                int singleFileProgress = (int) ((singleFileByteProgress * 100) / singleFileSize);
                //文件夹数量
                int dirsProgress = copyFileThread.getDirsCount();
                //单个文件夹名称
                String singleDirName = copyFileThread.getDirsName();
                //总文件数量
                int files = copyFileThread.getFiles();

                //读取任务对象当前完成的量，并设置给进度条
                if (copyFileThread.getSingleFileSize() > 0) {
                    singleFileProgressBar.setValue(singleFileProgress);

                    if (singleFileSize <= 1024 * 1024) {
                        singleFileProgressBar.setString(singleFileByteProgress / 1024 + " KB / " + singleFileSize / 1024 + " KB");
                    } else if (singleFileSize <= 1024 * 1024 * 1024) {
                        singleFileProgressBar.setString(String.format("%.2f", ((double) singleFileByteProgress / (1024 * 1024))) + " MB / " + String.format("%.2f", ((double) singleFileSize / (1024 * 1024))) + " MB");
                    } else {
                        singleFileProgressBar.setString(String.format("%.2f", ((double) singleFileByteProgress / (1024 * 1024 * 1024))) + " GB / " + String.format("%.2f", ((double) singleFileSize / (1024 * 1024 * 1024))) + " GB");
                    }
                }
                dirsNameLabel.setText("当前正在复制的文件夹: " + singleDirName);
                dirsCountProgressBar.setValue(dirsProgress);
                dirsCountProgressBar.setString("第 " + dirsProgress + " 个文件夹 / 共 " + dirsCountProgressBar.getMaximum() + " 个文件夹");
                if (totalFileSize > 0) {
                    mainProgressBar.setValue(progress);
                    //设置主窗口的进度条进度
                    statusBar.setValue(progress);
                    //设置主窗口的进度条内容
                    statusBar.setString(String.format("%.2f", (double) (byteProgress * 100) / totalFileSize) + "%");

                    if (totalFileSize <= 1024 * 1024) {
                        mainProgressBar.setString(byteProgress / 1024 + " KB / " + totalFileSize / 1024 + " KB");
                    } else if (totalFileSize <= 1024 * 1024 * 1024) {
                        mainProgressBar.setString(String.format("%.2f", ((double) byteProgress / (1024 * 1024))) + " MB / " + String.format("%.2f", ((double) totalFileSize / (1024 * 1024))) + " MB");
                    } else {
                        mainProgressBar.setString(String.format("%.2f", ((double) byteProgress / (1024 * 1024 * 1024))) + " GB / " + String.format("%.2f", ((double) totalFileSize / (1024 * 1024 * 1024))) + " GB");
                    }
                }
                totalFileProgressBar.setValue(files);
                totalFileProgressBar.setString(files + " 文件 / " + totalFileProgressBar.getMaximum() + " 文件");
                singleFileLabel.setText("正在复制文件: " + copyFileThread.getSingleFileName());
            });

            //每秒速度计数器
            AtomicReference<Double> speedPerMinute = new AtomicReference<>((double) 0);

            Timer speedTimer = new Timer(500, t -> {
                speedLabel.setText("速度: " + String.format("%.2f", (copyFileThread.getProgress() - speedPerMinute.get()) / 524288) + " MB/s");
                speedPerMinute.set((double) copyFileThread.getProgress());
            });

            totalFileProgressBar.addChangeListener(e1 -> {
                if (copyFileThread.getFiles() >= totalFileCount) {
                    long end = System.currentTimeMillis() - startTime;
                    timer.stop();
                    speedTimer.stop();
                    totalFileProgressBar.setString(totalFileProgressBar.getMaximum() + " 文件 / " + totalFileProgressBar.getMaximum() + " 文件");
                    JOptionPane.showMessageDialog(copyThreadFrame, "复制完成！\n耗时: " + String.format("%.2f", ((double) end / 1000)) + " 秒", "完成", JOptionPane.INFORMATION_MESSAGE);
                    statusBar.setValue(0);
                    statusBar.setString("无任务");
                    statusLabel.setText("状态：等待中");
                    copyThreadFrame.dispose();
                }
            });

            timer.start();
            speedTimer.start();
        } else {
            JOptionPane.showMessageDialog(fileUtils,"无法检测主文件夹，请检查是否存在。","错误",JOptionPane.ERROR_MESSAGE);
        }
    }
}
