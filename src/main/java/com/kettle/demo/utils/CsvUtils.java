package com.kettle.demo.utils;

import com.opencsv.CSVWriter;
import org.pentaho.di.core.logging.LogChannel;
import org.pentaho.di.core.logging.LogChannelFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CsvUtils {
    public static void writeCSV(final String fileName, final List<String[]> data) {

        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("统计数据");
        CSVWriter writer = null;
        try {
            // 创建文件所在目录
            FileOutputStream fileOutputStream = new FileOutputStream(fileName);
            fileOutputStream.write(0xef);
            fileOutputStream.write(0xbb);
            fileOutputStream.write(0xbf);
            writer = new CSVWriter(new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8.name()), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
            writer.writeAll(data);

        } catch (Exception e) {
            kettleLog.logError("将数据写入CSV出错：" + e);
        } finally {
            if (null != writer) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    kettleLog.logError("关闭文件输出流出错：" + e);
                }
            }
        }
    }


    public static void appendCSV(final String fileName, final List<String[]> data) {

        LogChannelFactory logChannelFactory = new org.pentaho.di.core.logging.LogChannelFactory();
        LogChannel kettleLog = logChannelFactory.create("上报数据");
        CSVWriter writer = null;
        try {
            // 创建文件所在目录
            FileOutputStream fileOutputStream = new FileOutputStream(fileName,true);
            fileOutputStream.write(0xef);
            fileOutputStream.write(0xbb);
            fileOutputStream.write(0xbf);
            writer = new CSVWriter(new OutputStreamWriter(fileOutputStream, StandardCharsets.UTF_8.name()), CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER, CSVWriter.DEFAULT_ESCAPE_CHARACTER, CSVWriter.DEFAULT_LINE_END);
            writer.writeAll(data);

        } catch (Exception e) {
            kettleLog.logError("将数据写入CSV出错：" + e);
        } finally {
            if (null != writer) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    kettleLog.logError("关闭文件输出流出错：" + e);
                }
            }
        }
    }



}
