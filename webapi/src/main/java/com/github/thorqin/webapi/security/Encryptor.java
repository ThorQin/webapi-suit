package com.github.thorqin.webapi.security;

import com.github.thorqin.webapi.utility.Serializer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;

public class Encryptor {
	public final static String AES = "aes";
	public final static String DES = "des";
	public final static String DESede = "desede";
	private final Cipher cipher;
	private final SecretKey secretKey;
	
	public final static String generateKey(String cipher) throws NoSuchAlgorithmException {
		final int keySize;
		switch (cipher.toLowerCase()) {
			case AES:
				keySize = 128;
				break;
			case DES:
				keySize = 56;
				break;
			case DESede:
				keySize = 168;
				break;
			default:
				throw new NoSuchAlgorithmException("Unsupport cipher: " + cipher);
		}
		KeyGenerator keygen = KeyGenerator.getInstance(cipher);
		keygen.init(keySize);
		return Base64.encodeBase64String(keygen.generateKey().getEncoded());
	}
 
    public final static String md5(String s) {
		char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'a', 'b', 'c', 'd', 'e', 'f' };
		byte[] md = md5(s.getBytes());
		if (md == null)
			return null;
		int j = md.length;
		char str[] = new char[j * 2];
		int k = 0;
		for (int i = 0; i < j; i++) {
			byte byte0 = md[i];
			str[k++] = hexDigits[byte0 >>> 4 & 0xf];
			str[k++] = hexDigits[byte0 & 0xf];
		}
		return new String(str);
	}
	public final static byte[] md5(byte[] bytes) {
		try {
			MessageDigest mdTemp = MessageDigest.getInstance("MD5");
			mdTemp.update(bytes);
			return mdTemp.digest();
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
	}
	public Encryptor(String cipher, String key) throws Exception {
		secretKey = new SecretKeySpec(Base64.decodeBase64(key), cipher); 
		this.cipher = Cipher.getInstance(cipher);
	}
 
//    public Encryptor(byte[] key, String cipher) throws Exception {
//		final int keySize;
//		switch (cipher.toLowerCase()) {
//			case AES:
//				keySize = 128;
//				break;
//			case DES:
//				keySize = 56;
//				break;
//			case DESede:
//				keySize = 168;
//				break;
//			default:
//				throw new UnsupportedEncodingException("Unsupport cipher: " + cipher);
//		}
//		KeyGenerator kgen = KeyGenerator.getInstance(cipher);
//		kgen.init(keySize, new SecureRandom(key));
//		SecretKey sKey = kgen.generateKey();  
//		byte[] enCodeFormat = sKey.getEncoded();  
//		secretKey = new SecretKeySpec(enCodeFormat, cipher); 
//		this.cipher = Cipher.getInstance(cipher);
//    }
	
	public synchronized final byte[] encrypt(byte[] src) throws Exception {
		cipher.init(Cipher.ENCRYPT_MODE, secretKey);
		return cipher.doFinal(src);
	}
	public synchronized final byte[] decrypt(byte[] src) throws Exception {
		cipher.init(Cipher.DECRYPT_MODE, secretKey);
		return cipher.doFinal(src);
	}


	public static void main(String arg[]) throws Exception {
		String cipher;
		if (arg.length != 1) {
			System.out.println();
			System.out.println("Usage: Encryptor <Cipher: AES|DES|DESede>");
			System.out.println();
			return;
		} else {
			cipher = arg[0].toLowerCase();
			if (!cipher.equals(AES) && !cipher.equals(DES) && !cipher.equals(DESede)) {
				System.out.println();
				System.out.println("Usage: Encryptor <Cipher: AES|DES|DESede>");
				System.out.println();
				return;
			}
		}
		String key = Encryptor.generateKey(cipher);
//		Encryptor enc = new Encryptor(cipher, key);
//		
//		String message = "test message!";
//		byte[] encData = enc.encrypt(message.getBytes());
//		String decStr = new String(enc.decrypt(encData));
		System.out.println("-----------------------------------");
		System.out.println(cipher.toUpperCase() + " KEY: " + key);
		System.out.println("-----------------------------------");
//		System.out.println("src:" + message);
//		System.out.println("enc:" + Base64.encodeBase64String(encData));
//		System.out.println("dec:" + decStr);
	}
}
