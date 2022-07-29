package github.kasuminova.fileutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CopyFileThread {
    public static class copyFileThread implements Runnable {
        //源路径，最终路径
        String inputPath;
        List<String> outputPath;
        public void setInputPath(String inputPath) {
            this.inputPath = inputPath;
        }
        public void setOutputPath(List<String> outputPath) {
            this.outputPath = outputPath;
        }
        //记录任务总量
        long progress;
        long singleFileProgress;
        String singleFileName;
        long singleFileSize;
        int files;
        int dirsCount;
        String dirsName;

        public String getDirsName() {
            return dirsName;
        }
        public int getDirsCount() {
            return dirsCount;
        }
        public long getProgress() {
            return progress;
        }
        public int getFiles() {
            return files;
        }
        public long getSingleFileProgress() {
            return singleFileProgress;
        }
        public String getSingleFileName() {
            return singleFileName;
        }
        public long getSingleFileSize() {
            return singleFileSize;
        }

        @Override
        public void run() {
            for (String s : outputPath) {
                //设定当前复制的文件夹名
                dirsName = s;
                //开始复制
                copyDir(inputPath, dirsName);
                //完成数 +1
                dirsCount += 1;
            }
        }

        public void copyDir(String oldPath, String newPath){
            File file = new File(oldPath);        //文件名称列表
            String[] filePath = file.list();

            if (!(new File(newPath)).exists()) {
                (new File(newPath)).mkdir();
            }

            assert filePath != null;
            for (String s : filePath) {
                if ((new File(oldPath + File.separator + s)).isDirectory()) {
                    copyDir(oldPath + File.separator + s, newPath + File.separator + s);
                }

                if (new File(oldPath + File.separator + s).isFile()) {
                    try {
                        copyFile(oldPath + File.separator + s, newPath + File.separator + s);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        public void copyFile(String oldPath, String newPath) throws IOException {
            File oldFile = new File(oldPath);
            File file = new File(newPath);
            FileInputStream in = new FileInputStream(oldFile);
            FileOutputStream out = new FileOutputStream(file);

            singleFileName = file.getName();
            singleFileSize = in.getChannel().size();

            byte[] buffer;

            if (singleFileSize <= 1024) {
                buffer = new byte[1024];
            } else if (singleFileSize <= 1024 * 1024) {
                buffer = new byte[1024 * 4];
            } else if (singleFileSize <= 1024 * 1024 * 100) {
                buffer = new byte[1024 * 16];
            } else {
                buffer = new byte[1024 * 256];
            }

            //重置变量
            singleFileProgress = 0;
            int len;
            while((len = in.read(buffer)) != -1){
                out.write(buffer, 0, len);
                singleFileProgress += len;
                progress += len;
            }
            in.close();
            out.close();
            //文件统计数 +1
            files += 1;
        }
    }
}
