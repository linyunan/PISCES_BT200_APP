package com.yundiankj.ble_lock.Resource;

import java.util.Formatter;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AesEntryDetry {
	// 加密秘钥 ，16个字节也就是128 bit
	private static final byte[] AES_KEY = {0x01, 0x03, 0x05, 0x07, (byte) 0xFF, (byte) 0xFE, 0x77, (byte) 0x88, 0x12,
			(byte) 0x99, 0x32, 0x41, 0x14, 0x24, 0x25, 0x36};

	// 需要加密的数据(保证16个字节，不够的自己填充)
	private static final byte[] SOURCE_BUF = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
			11, 12, 13, 14, 15, 16 };

	// Java测试工程入口方法，在这个方法中调用加解密方法并打印结果
	public static void main(String[] args) throws Exception {
		// 需要加密的原始数据转化成字符串并打印到控制台
		String strSource = BytetohexString(SOURCE_BUF);
		System.out.println("source:\n" + strSource);

		// 调用加密方法，对数据进行加密，加密后的数据存放到encryBuf字节数组中
		byte[] encryBuf = encrypt( SOURCE_BUF);
		// 将加密后的字节数组数据转成字符串并打印到控制台
		String strEncry = BytetohexString(encryBuf).toLowerCase();
		System.out.println("encrypte:\n" + strEncry);

		// 调用解密方法，对数据进行解密，解密后的数据存放到decryBuf字节数组中
		byte[] decryBuf = decrypt( encryBuf);
		// 将解密后的字节数组数据转成字符串并打印到控制台
		String strDecry = BytetohexString(decryBuf);
		System.out.println("decrypte:\n" + strDecry);

	}

	// 加密方法
	public static byte[] encrypt( byte[] clear) throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(AES_KEY, "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
		byte[] encrypted = cipher.doFinal(clear);
		return encrypted;
	}

	// 解密方法
	public static byte[] decrypt( byte[] encrypted)
			throws Exception {
		SecretKeySpec skeySpec = new SecretKeySpec(AES_KEY, "AES");
		Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, skeySpec);
		byte[] decrypted = cipher.doFinal(encrypted);
		return decrypted;
	}

	// 字节数组按照一定格式转换拼装成字符串用于打印显示
	public static String BytetohexString(byte[] b) {
		int len = b.length;
		StringBuilder sb = new StringBuilder(b.length * (2 + 1));
		Formatter formatter = new Formatter(sb);

		for (int i = 0; i < len; i++) {
			if (i < len - 1)
				formatter.format("0x%02X:", b[i]);
			else
				formatter.format("0x%02X", b[i]);

		}
		formatter.close();

		return sb.toString();
	}
}
