/**
 * Read file in remote FtpServer with SSH
 */

package com.alwyn.javautils;

import com.trilead.ssh2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

public class FtpUtil {
    private static final Logger LOG = LoggerFactory.getLogger(FtpUtil.class);

    public static Connection getConn(String ip, int port, String user, String pwd) throws Exception {
        Connection conn = new Connection(ip, port);
        if (conn.isAuthenticationComplete()) {
            return conn;
        }

        conn.connect();
        boolean isAuthenticated = conn.authenticateWithPassword(user, pwd);
        if (isAuthenticated) {
            return conn;
        } else {
            LOG.warn("FtpServer authentication failed! ");
            return null;
        }
    }

    public static void closeConn(Connection conn){
        conn.close();
    }

    public static Session getSession(Connection conn) throws IOException {
        Session session = conn.openSession();
        return session;
    }

    public static SFTPv3Client getClient(Connection conn) throws IOException {
        SFTPv3Client client = new SFTPv3Client(conn);
        return client;
    }

    public static InputStream readFile(Connection conn, String filePath, String ip) throws Exception {
        Session session = getSession(conn);

        // get file size
        session.execCommand("du -b ".concat(filePath));
        InputStream sizeIn = new StreamGobbler(session.getStdout());
        // converse from bytes to character
        InputStreamReader isr = new InputStreamReader(sizeIn);
        // create character stream buffer
        BufferedReader bufr = new BufferedReader(isr);

        String line;
        int fileSize = 0;
        while((line = bufr.readLine()) != null) {
            String[] fileAttr = line.split("\t");
            fileSize = Integer.parseInt(fileAttr[0]);
        }
        isr.close();
        session.close();
        LOG.info("    [" + ip + "]-[" + filePath + "] [fileSize] " + fileSize);

        // read content
        session = getSession(conn);
        session.execCommand("cat ".concat(filePath));
        // wait for 1000ms to avoid the network's error
        InputStream is = new StreamGobbler(session.getStdout());
        session.waitForCondition(ChannelCondition.EXIT_STATUS, 1000);
        LOG.info("    [" + ip + "]-[" + filePath + "] [FirstFileSize] " + is.available());
        int i = 0;
        while (fileSize != is.available()) {
            i++;
            Thread.sleep(1000);
            LOG.info("    [" + ip + "]-[" + filePath + "] [times: "+ i +", FileSize] " + is.available());
        }
        session.close();
        return is;
    }

    /**
     * Encapsulation
     * @param filePath
     * @param ip
     * @param account
     * @param pwd
     * @return
     */
    public MultipartFile requestFtpFile(String filePath, String ip, String account, String pwd) {
        MultipartFile multipartFile = null;

        try {
            Connection conn = getConn(ip, 22, account, pwd);
            InputStream is = readFile(conn, filePath, ip);
            multipartFile = new MockMultipartFile(filePath, is);
            closeConn(conn);
        } catch (Exception e) {
            LOG.error(Arrays.toString(e.getStackTrace()));
        }

        return multipartFile;
    }

    /**
     * Sample
     * @param args
     */
    void main(String[] args) {
        String fileUrl = "";
        String ip = "";
        String account = "";
        String pwd = "";
        requestFtpFile(fileUrl, ip, account, pwd);
    }
}
