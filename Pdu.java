package sms;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import android.telephony.PhoneNumberUtils;

public class Pdu {
    
    private static byte reverseByte(byte b) {
        return (byte) ((b & 0xF0) >> 4 | (b & 0x0F) << 4);
    }
    
    private static byte[] encodeUCS2(String message, byte[] header)
            throws UnsupportedEncodingException {
        byte[] userData, textPart;
        textPart = message.getBytes("utf-16be");

        if (header != null) {
            // Need 1 byte for UDHL
            /*
            userData = new byte[header.length + textPart.length + 1];

            userData[0] = (byte) header.length;
            System.arraycopy(header, 0, userData, 1, header.length);
            System.arraycopy(textPart, 0, userData, header.length + 1,
                    textPart.length);
            */
            userData = new byte[header.length + textPart.length];
            System.arraycopy(header, 0, userData, 0, header.length);
            System.arraycopy(textPart, 0, userData, header.length, textPart.length);
        } else {
            userData = textPart;
        }
        byte[] ret = new byte[userData.length + 1];
        ret[0] = (byte) (userData.length & 0xff);
        System.arraycopy(userData, 0, ret, 1, userData.length);
        return ret;
    }
    
    /**
     * SMS pdu encode,support long sms
     * @param sender
     * @param body
     * @param seq
     * @param key
     * @param count
     * @return
     * @throws IOException
     */
    public static byte[] encode(String sender, String body, byte seq, byte key, byte count) throws IOException{
        byte[] pdu = null;
        byte[] scBytes = PhoneNumberUtils.networkPortionToCalledPartyBCD("0000000000");
        byte[] senderBytes = PhoneNumberUtils.networkPortionToCalledPartyBCD(sender);
        int lsmcs = scBytes.length;
        
        // 时间处理，包括年月日时分秒以及时区和夏令时
        byte[] dateBytes = new byte[7];
        Calendar calendar = new GregorianCalendar();
        dateBytes[0] = reverseByte((byte) (calendar.get(Calendar.YEAR)));
        dateBytes[1] = reverseByte((byte) (calendar.get(Calendar.MONTH) + 1));
        dateBytes[2] = reverseByte((byte) (calendar.get(Calendar.DAY_OF_MONTH)));
        dateBytes[3] = reverseByte((byte) (calendar.get(Calendar.HOUR_OF_DAY)));
        dateBytes[4] = reverseByte((byte) (calendar.get(Calendar.MINUTE)));
        dateBytes[5] = reverseByte((byte) (calendar.get(Calendar.SECOND)));
        dateBytes[6] = reverseByte((byte) ((calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET)) / (60 * 1000 * 15)));
        
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        bo.write(lsmcs);                                    // A.短信服务中心长度
        bo.write(scBytes);                                  // BC.短信服务中心号码
        
        byte[] head = null;
        if(count > 1){
            bo.write(0x64);                                 // DE.文件头类型，信息类型，2位十六进制数
            
            head = new byte[6];
            head[0] = 0x05;    //协议长度（后面占5位） 
            head[1] = 0x00; //表示拆分短信 
            head[2] = 0x03; //拆分数据的长度（后面的3位） 
            head[3] = key;    //唯一标识（用于把多条短信合并） 
            head[4] = count;//共被拆分count条短信 
            head[5] = seq;    //序号，这是其中的第seq条短信
        }
        else{
            bo.write(0x04);
        }
        
        bo.write((byte) sender.length());                   // F.发送方号码长度
        bo.write(senderBytes);                              // GH.发送方号码
        bo.write(0x00);                                     // I.协议标示，00为普通GSM，点对点方式
        bo.write(0x08);                                     // J.数据编码方案 encoding: 8 for UCS-2
        bo.write(dateBytes);                                // K.有效期

        bo.write(encodeUCS2(body, head));                   // LM.数据
        
        pdu = bo.toByteArray();
        
        return pdu;
    }
}
