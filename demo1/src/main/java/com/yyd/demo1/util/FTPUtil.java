package com.yyd.demo1.util;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;


public class FTPUtil {
    /** 日志对象 **/
    private static final Logger LOGGER = LoggerFactory.getLogger(com.yyd.demo1.util.FTPUtil.class);

    /** 本地字符编码  **/
    private static String localCharset = "GBK";

    /** FTP协议里面，规定文件名编码为iso-8859-1 **/
    private static String serverCharset = "ISO-8859-1";

    /** UTF-8字符编码 **/
    private static final String CHARSET_UTF8 = "UTF-8";

    /** OPTS UTF8字符串常量 **/
    private static final String OPTS_UTF8 = "OPTS UTF8";

    /** 设置缓冲区大小4M **/
    private static final int BUFFER_SIZE = 1024 * 1024 * 4;

    /** FTPClient对象 **/
    private static FTPClient ftpClient = null;

    /**
     * 连接FTP服务器
     *
     * @param address  地址，如：127.0.0.1
     * @param port     端口，如：21
     * @param username 用户名，如：root
     * @param password 密码，如：root
     */
    public void login(String address, int port, String username, String password) {
        ftpClient = new FTPClient();
        try {
            //  设置连接超时时长 1min
            ftpClient.setConnectTimeout(60*1000);
            ftpClient.connect(address, port);
            boolean login = ftpClient.login(username, password);
            if (!login){
                LOGGER.error("FTP登录失败");
                return;
            }
            ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                closeConnect();
                LOGGER.error("FTP服务器连接失败");
            }
            LOGGER.info("FTP登录成功!");
        } catch (Exception e) {
            LOGGER.error("FTP登录失败", e);
        }
    }

    /**
     * 关闭FTP连接
     *
     */
    public void closeConnect() {
        if (ftpClient != null && ftpClient.isConnected()) {
            try {
                ftpClient.logout();
                ftpClient.disconnect();
                LOGGER.info("关闭FTP连接成功");
            } catch (IOException e) {
                LOGGER.error("关闭FTP连接失败", e);
            }
        }
    }

    /**
     * FTP服务器路径编码转换
     *
     * @param ftpPath FTP服务器路径
     * @return String
     */
    private static String changeEncoding(String ftpPath) {
        String directory = null;
        try {
            if (FTPReply.isPositiveCompletion(ftpClient.sendCommand(OPTS_UTF8, "ON"))) {
                localCharset = CHARSET_UTF8;
            }
            directory = new String(ftpPath.getBytes(localCharset), serverCharset);
        } catch (Exception e) {
            LOGGER.error("路径编码转换失败", e);
        }
        return directory;
    }

    /**
     *  上传单个文件
     *
     * @param address FTP服务器IP地址
     * @param port FTP端口号,默认为 21
     * @param username 账号
     * @param password 密码
     * @param ftpPath FTP服务器路径
     * @param savePath 上传文件的本地路径
     * @return boolean
     * */
    public boolean uploadFile(String address,int port,String username, String password,
                              String ftpPath, String savePath){
        login(address,port,username,password);
        boolean b = uploadFile(ftpPath, savePath);
        //boolean b = uploadSubdirectories(new File(savePath), ftpPath);
        closeConnect();
        return b;
    }

    public boolean uploadFile(String address,int port,String username, String password,
                              String ftpPath, File file){
        login(address,port,username,password);
        boolean b = uploadFile(ftpPath, file);
        //boolean b = uploadSubdirectories(file, ftpPath);
        closeConnect();
        return b;
    }

    /**
     *  上传单个文件
     *
     * @param ftpPath FTP服务器路径
     * @param savePath 上传文件的本地路径
     * @return boolean
     * */
    public boolean uploadFile(String ftpPath, String savePath){
        return uploadSubdirectories(new File(savePath),ftpPath);
    }

    /**
     *  上传单个文件
     *
     * @param ftpPath FTP服务器路径
     * @param file 上传的文件
     * @return boolean
    * */
    public boolean uploadFile(String ftpPath, File file){
        //return uploadSubdirectories(file, ftpPath);
        boolean flag = false;
        if (!ftpPath.endsWith("/")){
            ftpPath = ftpPath.concat("/");
        }
        if (ftpClient != null) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(file);
                ftpClient.setBufferSize(BUFFER_SIZE);
                // 设置编码：开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码（GBK）
                if (FTPReply.isPositiveCompletion(ftpClient.sendCommand(OPTS_UTF8, "ON"))) {
                    localCharset = CHARSET_UTF8;
                }
                ftpClient.setControlEncoding(localCharset);
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                String path = changeEncoding(ftpPath);
                // 目录是否存在
                if (!ftpClient.changeWorkingDirectory(path)) {
                    //this.createDirectorys(path);
                    LOGGER.error("目录 " + path + " 不存在!");
                    return flag;
                }
                // 设置被动模式，开通一个端口来传输数据
                ftpClient.enterLocalPassiveMode();
                // 上传文件
                flag = ftpClient.storeFile(new String(file.getName().getBytes(localCharset), serverCharset), fis);
            } catch (Exception e) {
                LOGGER.error("本地文件上传FTP失败", e);
            }
        }
        return flag;
    }


    /**
     * 文件夹上传到FTP服务器
     *
     * @param address FTP服务器IP地址
     * @param port FTP端口号,默认为 21
     * @param username 账号
     * @param password 密码
     * @param remoteDirectoryPath FTP服务器文件相对路径，例如：test/123
     * @param localDirectory 文件夹路径
     * @return boolean 成功返回true，否则返回false
     */
    public boolean uploadDirectory(String address,int port,String username, String password,
                                   String remoteDirectoryPath,String localDirectory){
        login(address,port,username,password);
        boolean b = uploadDirectory(remoteDirectoryPath, localDirectory);
        closeConnect();
        return b;
    }

    public boolean uploadDirectory(String address,int port,String username, String password,
                                   String remoteDirectoryPath,File src){
        login(address,port,username,password);
        boolean b = uploadDirectory(remoteDirectoryPath, src);
        closeConnect();
        return b;
    }

    /**
     * 文件夹上传到FTP服务器
     *
     * @param remoteDirectoryPath FTP服务器文件相对路径，例如：test/123
     * @param localDirectory 文件夹路径
     * @return boolean 成功返回true，否则返回false
     */
    public boolean uploadDirectory(String remoteDirectoryPath,String localDirectory){
        File src = new File(localDirectory);
        return uploadDirectory(remoteDirectoryPath,src);
    }

    public boolean uploadDirectory(String remoteDirectoryPath,File src) {
        if (!remoteDirectoryPath.endsWith("/")){
            remoteDirectoryPath = remoteDirectoryPath.concat("/");
        }
        try {
            remoteDirectoryPath = remoteDirectoryPath + src.getName() + "/";
            boolean makeDirFlag = this.ftpClient.makeDirectory(remoteDirectoryPath);
            LOGGER.info("文件名称 : " + src.getName());
            LOGGER.info("FTP服务器目标相对路径 : " + remoteDirectoryPath);
            LOGGER.info("创建目录 " + src.getName() + " : " + makeDirFlag);
            if (!makeDirFlag){
                LOGGER.error("文件上传失败!");
                return false;
            }
        }catch (IOException e) {
            e.printStackTrace();
            LOGGER.info(remoteDirectoryPath + "目录创建失败");
        }
        File[] allFile = src.listFiles();
        for (int currentFile = 0;currentFile < allFile.length;currentFile++) {
            if (!allFile[currentFile].isDirectory()) {
                String srcName = allFile[currentFile].getPath().toString();
                uploadSubdirectories(new File(srcName), remoteDirectoryPath);
            }
        }
        for (int currentFile = 0;currentFile < allFile.length;currentFile++) {
            if (allFile[currentFile].isDirectory()) {
                // 递归
                uploadDirectory(allFile[currentFile].getPath().toString(),
                        remoteDirectoryPath);
            }
        }
        return true;
    }

    /**
     * 文件上传到FTP服务器
     *
     * @param localFile 上传的文件
     * @param romotUpLoadePath FTP服务器文件相对路径，例如：test/123
     * @return boolean 成功返回true，否则返回false
     */
    private boolean uploadSubdirectories(File localFile, String romotUpLoadePath){
        BufferedInputStream inStream = null;
        boolean success = false;
        try {
            this.ftpClient.changeWorkingDirectory(romotUpLoadePath);// 改变工作路径
            inStream = new BufferedInputStream(new FileInputStream(localFile));
            LOGGER.info(localFile.getName() + "开始上传.....");
            success = this.ftpClient.storeFile(localFile.getName(), inStream);
            if (success == true) {
                LOGGER.info(localFile.getName() + "上传成功");
                return success;
            }
            LOGGER.error(localFile.getName() + "上传失败");
        }catch (FileNotFoundException e) {
            e.printStackTrace();
            LOGGER.error(localFile + "未找到");
        }catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (inStream != null) {
                try {
                    inStream.close();
                }catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return success;
    }

    /**
     * 在服务器上递归创建目录
     *
     * @param dirPath 上传目录路径
     * @return
     */
    /*public void createDirectorys(String dirPath) {
        try {
            if (!dirPath.endsWith("/")) {
                dirPath += "/";
            }
            String directory = dirPath.substring(0, dirPath.lastIndexOf("/") + 1);
            ftpClient.makeDirectory("/");
            int start = 0;
            int end = 0;
            if (directory.startsWith("/")) {
                start = 1;
            }else{
                start = 0;
            }
            end = directory.indexOf("/", start);
            while(true) {
                String subDirectory = new String(dirPath.substring(start, end));
                if (!ftpClient.changeWorkingDirectory(subDirectory)) {
                    if (ftpClient.makeDirectory(subDirectory)) {
                        ftpClient.changeWorkingDirectory(subDirectory);
                    } else {
                        LOGGER.info("创建目录失败");
                        return;
                    }
                }
                start = end + 1;
                end = directory.indexOf("/", start);
                //检查所有目录是否创建完毕
                if (end <= start) {
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("上传目录创建失败", e);
        }
    }*/
}
