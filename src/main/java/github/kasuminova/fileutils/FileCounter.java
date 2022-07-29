package github.kasuminova.fileutils;

import java.io.File;

public class FileCounter {
    /**
     * 计算文件大小
     */
    public static class fileCounter {
        int count = 0;
        long size = 0;
        public long[] getDirFileCountAndTotalSize(String path) {
            long[] count = new long[2];
            getFile("./" + path);
            count[0] = this.count;
            count[1] = size;
            System.out.println("共有 " + count[0] + " 个文件");
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
                    getFile(value.toString());
                }
            }
        }
    }
}
