package cn.kmbeast.utils;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 文件哈希映射服务
public class FileHashService {
    private static final String HASH_MAP_FILE = "file_hash_map.dat";
    private final Map<String, String> fileHashMap; // 哈希值 -> 文件名

    public FileHashService() {
        this.fileHashMap = loadHashMap();
    }

    // 加载持久化的哈希映射
    @SuppressWarnings("unchecked")
    private Map<String, String> loadHashMap() {
        File file = new File(HASH_MAP_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                return (Map<String, String>) ois.readObject();
            } catch (Exception e) {
                System.err.println("加载文件哈希映射失败: " + e.getMessage());
            }
        }
        return new ConcurrentHashMap<>();
    }

    // 保存哈希映射到文件
    private synchronized void saveHashMap() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(HASH_MAP_FILE))) {
            oos.writeObject(fileHashMap);
        } catch (Exception e) {
            System.err.println("保存文件哈希映射失败: " + e.getMessage());
        }
    }

    // 检查文件是否已存在
    public String getExistingFileName(String fileHash) {
        return fileHashMap.get(fileHash);
    }

    // 保存新的文件哈希映射
    public void saveFileHash(String fileHash, String fileName) {
        fileHashMap.put(fileHash, fileName);
        saveHashMap(); // 持久化保存
    }

    // 计算文件哈希值
    public String calculateFileHash(MultipartFile file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(file.getBytes());

        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = String.format("%02x", b);
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
