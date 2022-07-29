package github.kasuminova.fileutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import java.nio.file.Files;

public class MultiThreadCopyDir implements Runnable {
    private int count = 0;
    private int dirCount = 0;
    private int errCount = 0;
    List<Thread> list = new ArrayList<>();//存储线程对象.
    /*本递归复制函数具有检查并创建新目录的能力,*/

    /**
     * @param srcPath 可能是个目录,也可能是个文件
     * @param desPath 当 srcPath 是一个目录的时候,desPath 也必须是一个目录(1.约定该参数和srcPath是相同尾缀时;2.不用关心尾缀
     *                可能的参数组合包括:dir -> dir;file -> file;file -> dir;(不可能是dir->file)
     */
    private void createCopyPathThreads(String srcPath, String desPath) {
        File srcPathFile = new File(srcPath);
        File desPathFile = new File(desPath);
        /*分析源路径*/
        if (srcPathFile.isDirectory()) {
            //确保对应的目标目录存在
            if (!(new File(desPath)).exists()) {
                desPathFile.mkdir();//创建目录
                dirCount++;
            }
            /*遍历当前目录下的子目录和文件(即各个条目)*/
            String[] filesPathString = srcPathFile.list();
            for (String subItem : filesPathString) {

                String absoluteSrcSubItemStr = srcPath + File.separator + subItem;
                String absoluteDesSubItemStr = desPath + File.separator + subItem;

                /*直接递归:*/
                createCopyPathThreads(absoluteSrcSubItemStr, absoluteDesSubItemStr);

            }//endFor
        }//end if
        else {
            Thread thread = new Thread(() -> {
                try {
                    FileChannel inputChannel = new FileInputStream(srcPath).getChannel();
                    FileChannel outputChannel = new FileOutputStream(desPath).getChannel();
                    outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                    inputChannel.close();
                    outputChannel.close();
                    count++;
                } catch (Exception e) {
                    e.printStackTrace();
                    errCount++;
                }
            });
            list.add(thread);
            thread.setName("threadNo" + count);
        }
    }

    public int getCount() {
        return count;
    }
    public int getDirCount() {
        return dirCount;
    }
    public int getErrorCount() {
        return errCount;
    }

    public String srcPath;
    public void setSrcPath(String srcPath) {
        this.srcPath = srcPath;
    }
    public void setDesPath(List<String> desPath) {
        this.desPath = desPath;
    }
    public List<String> desPath;

    @Override
    public void run() {
        for (String s : desPath) {
            createCopyPathThreads(srcPath, "./" + s);
        }
        for (Thread thread : list) {
            thread.start();
        }
        for (Thread thread : list) {
            try {
                thread.join();
            } catch (Exception e){
                errCount++;
            }
        }
    }
}
