package github.kasuminova.fileutils;

import java.io.File;

public class FileCounter {
    /**
     * 计算文件大小
     */
    public static class fileCounter {
        int count = 0;
        int dirCount = 0;
        long size = 0;
        public long[] getDirFileCountAndTotalSize(String path) {
            long[] count = new long[3];
            getFile("./" + path);
            count[0] = this.count;
            count[1] = size;
            count[2] = dirCount;
            System.out.println("共有 " + count[0] + " 个文件，" + count[2] + " 个文件夹");
            return count;
        }

        public void getFile(String filepath) {
            File file = new File(filepath);
            File[] listFile = file.listFiles();
            assert listFile != null;
            for (File value : listFile) {
                if (!value.isDirectory()) {
                    size += value.length();
                    String temp = value.toString().substring(7);
                    count++;
                } else {
                    dirCount++;
                    getFile(value.toString());
                }
            }
        }
    }
}
