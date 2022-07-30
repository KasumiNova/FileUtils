package github.kasuminova.fileutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class FileThread {
    public static class backupAndDeleteFileThread implements Runnable {
        //源路径，最终路径
        List<String> inputPath;
        String backupPath;
        public void setOldPath(List<String> inputPath) {
            this.inputPath = inputPath;
        }
        public void setBackupPath(String backupPath) {
            this.backupPath = backupPath;
        }
        //记录任务总量
        long progress;
        long singleFileProgress;
        String singleFileName;
        long singleFileSize;
        int files;
        int dirsCount;
        int nullFileCount;
        String inputFile;
        public String getInputFile() {
            return inputFile;
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
        public int getNullFileCount() {
            return nullFileCount;
        }

        @Override
        public void run() {
            for (String s : inputPath) {
                //设定当前复制的文件夹名
                inputFile = s;
                System.out.println("要删除的文件/文件夹：" + inputFile);
                //开始复制
                try {
                    File file = new File("./" + inputFile);
                    if (file.exists()) {
                        if (file.isFile()) {
                            copyFile("./" + inputFile,"./" + backupPath + "/" + inputFile);
                        } else {
                            copyDir("./" + inputFile, "./" + backupPath + "/" + inputFile);
                        }
                    } else {
                        System.out.println("./" + inputFile + " 文件不存在，跳过复制");
                        nullFileCount += 1;
                    }
                } catch (Exception e){
                    e.printStackTrace();
                } finally {
                    delDir(new File("./" + inputFile));
                }
                //完成数 +1
                dirsCount += 1;
            }
        }

        /**
         * 递归删除文件夹
         * @param file 文件夹
         */
        public static void delDir(File file) {
            if (file.exists()) {
                // 判断是否是一个目录, 不是的话跳过, 直接删除; 如果是一个目录, 先将其内容清空.
                if (file.isDirectory()) {
                    // 获取子文件/目录
                    File[] subFiles = file.listFiles();
                    // 遍历该目录
                    for (File subFile : subFiles) {
                        // 递归调用删除该文件: 如果这是一个空目录或文件, 一次递归就可删除. 如果这是一个非空目录, 多次
                        // 递归清空其内容后再删除
                        delDir(subFile);
                    }
                }
                // 删除空目录或文件
                file.delete();
            }
        }

        /**
         * 创建文件，传入路径
         * @param path
         * @throws IOException
         */
        private void createFile(String path) throws IOException {
            File file = new File(path);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            file.createNewFile();
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
            if (!file.exists()) createFile(file.getPath());
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
