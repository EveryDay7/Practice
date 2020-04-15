package com.yyd.demo1.util;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class FTPUtil {

    /** 日志对象 **/
    private static final Logger LOGGER = LoggerFactory.getLogger(com.yyd.demo1.util.FTPUtil.class);

    /** 本地字符编码  **/
    //private static String localCharset = "GBK";

    /** FTP协议里面，规定文件名编码为iso-8859-1 **/
    //private static String serverCharset = "ISO-8859-1";

    /** UTF-8字符编码 **/
    //private static final String CHARSET_UTF8 = "UTF-8";

    /** OPTS UTF8字符串常量 **/
    //private static final String OPTS_UTF8 = "OPTS UTF8";

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
    /*private static String changeEncoding(String ftpPath) {
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
    }*/

    /**
     * 上传文件到FTP服务器,自动连接关闭ftp
     *
     * @param address  FTP服务器IP地址
     * @param port  服务器端口号,默认一般为 21
     * @param username  账号
     * @param password  密码
     * @param remoteDirectoryPath FTP服务器文件相对路径，例如：test/123
     * @param localPath 上传文件本地路径
     * @return boolean 成功返回true，否则返回false
     */
    public boolean uploadFiles(String address,int port,String username, String password,
                               String remoteDirectoryPath,String localPath){
        login(address, port, username, password);
        boolean uploadFiles = uploadFilesManual(remoteDirectoryPath, localPath);
        closeConnect();
        return uploadFiles;
    }

    /**
     * 上传文件到FTP服务器,自动连接关闭ftp
     *
     * @param address  FTP服务器IP地址
     * @param port  服务器端口号,默认一般为 21
     * @param username  账号
     * @param password  密码
     * @param remoteDirectoryPath FTP服务器文件相对路径，例如：test/123
     * @param src 上传的文件
     * @return boolean 成功返回true，否则返回false
     */
    public boolean uploadFiles(String address,int port,String username, String password,
                               String remoteDirectoryPath,File src){
        login(address, port, username, password);
        boolean uploadFiles = uploadFilesManual(remoteDirectoryPath, src);
        closeConnect();
        return uploadFiles;
    }

    /**
     * 上传文件到FTP服务器,需要手动连接关闭ftp
     *
     * @param remoteDirectoryPath FTP服务器文件相对路径，例如：test/123
     * @param localPath 上传文件本地路径
     * @return boolean 成功返回true，否则返回false
     */
    public boolean uploadFilesManual(String remoteDirectoryPath,String localPath){
        return uploadFilesManual(remoteDirectoryPath,new File(localPath));
    }

    /**
     * 上传文件到FTP服务器,需要手动连接关闭ftp
     *
     * @param remoteDirectoryPath FTP服务器文件相对路径，例如：test/123
     * @param src 文件
     * @return boolean 成功返回true，否则返回false
     */
    public boolean uploadFilesManual(String remoteDirectoryPath,File src) {
        boolean fls = false;
        if (ftpClient == null){
            LOGGER.error("FTP连接异常!");
            return fls;
        }
        if (!remoteDirectoryPath.endsWith("/")){
            remoteDirectoryPath = remoteDirectoryPath.concat("/");
        }
        //  判断文件是否为目录
        if (!src.isDirectory()){
            //  非目录,开始上传
            return uploadFile(remoteDirectoryPath,src);
        }
        String directoryPath = remoteDirectoryPath + src.getName() + "/";
        try {
            boolean makeDirFlag = createDirectorys(directoryPath);
            /*LOGGER.info("文件名称 : " + src.getName());
            LOGGER.info("FTP服务器目标相对路径 : " + remoteDirectoryPath);
            LOGGER.info("创建并切换到目录 '" + directoryPath + "' : " + makeDirFlag);*/
            if (!makeDirFlag){
                return fls;
            }
        }catch (Exception e) {
            e.printStackTrace();
            LOGGER.info(directoryPath + "目录创建失败");
        }

        //  获取该路径名表示的目录中的文件
        File[] allFile = src.listFiles();
        for (int currentFile = 0;currentFile < allFile.length;currentFile++) {
            //  此抽象路径名表示的文件是否为目录
            if (!allFile[currentFile].isDirectory()) {
                //  非目录,开始上传文件
                boolean uploadFile = uploadFile(directoryPath, allFile[currentFile]);
                if (!uploadFile){
                    return fls;
                }
            }
        }
        for (int currentFile = 0;currentFile < allFile.length;currentFile++) {
            //  此抽象路径名表示的文件是否为目录
            if (allFile[currentFile].isDirectory()) {
                // 为目录,递归
                boolean directory = uploadFilesManual(directoryPath, allFile[currentFile]);
                if (!directory){
                    return fls;
                }
            }
        }
        return true;
    }

    /**
     *  上传单个文件(非目录)
     *
     * @param ftpPath FTP服务器路径,路径不存在则创建
     * @param file 上传的文件
     * @return boolean
     * */
    private boolean uploadFile(String ftpPath, File file){
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
                //  设置编码：开启服务器对UTF-8的支持，如果服务器支持就用UTF-8编码，否则就使用本地编码（GBK）
                /*if (FTPReply.isPositiveCompletion(ftpClient.sendCommand(OPTS_UTF8, "ON"))) {
                    localCharset = CHARSET_UTF8;
                }*/
                //ftpClient.setControlEncoding(localCharset);
                //  设置传输模式
                ftpClient.setFileType(FTPClient.BINARY_FILE_TYPE);
                // 目录是否存在
                if (!ftpClient.changeWorkingDirectory(ftpPath)) {
                    LOGGER.error("上传目标目录不存在!");
                    return false;
                }
                // 设置被动模式，开通一个端口来传输数据
                ftpClient.enterLocalPassiveMode();
                // 上传文件
                flag = ftpClient.storeFile(new String(file.getName()), fis);
                String info = flag ? "上传成功,文件名 :" : "上传失败,文件名 :";
                LOGGER.info(info + file.getName());
            } catch (Exception e) {
                LOGGER.error("本地文件上传FTP失败", e);
            }
        }
        return flag;
    }

    /**
     * 逐级创建并切换到指定目录
     *
     * @param path 需要创建并切换的目录
     * @return boolean
    * */
    private boolean createDirectorys(String path){
        boolean fls = false;
        try {
            if (!path.endsWith("/")){
                path += "/";
            }
            if (ftpClient == null){
                LOGGER.error("FtpClient 异常!");
                return false;
            }
            //  当输入的目录为根目录时的情况
            if (path.equals("/")){
                return ftpClient.changeWorkingDirectory(path);
            }
            String[] split = path.split("/");
            String directory = "";
            for (int i = 0; i < split.length ; i++){
                directory += split[i] + "/";
                //  判断目录是否存在,不存在则创建
                if (!ftpClient.changeWorkingDirectory(directory)){
                    if (!ftpClient.makeDirectory(directory)){
                        LOGGER.error("创建目录 '" + directory + "' 失败!");
                        return false;
                    }
                }
            }
            //  切换目录
            fls = ftpClient.changeWorkingDirectory(path);
        }catch (Exception e){
            LOGGER.error("创建目录失败!",e);
        }
        return fls;
    }

}
