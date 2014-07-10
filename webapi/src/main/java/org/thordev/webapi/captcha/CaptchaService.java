/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi.captcha;

import com.octo.captcha.component.image.backgroundgenerator.BackgroundGenerator;
import com.octo.captcha.component.image.backgroundgenerator.UniColorBackgroundGenerator;
import com.octo.captcha.component.image.color.RandomListColorGenerator;
import com.octo.captcha.component.image.fontgenerator.FontGenerator;
import com.octo.captcha.component.image.fontgenerator.RandomFontGenerator;
import com.octo.captcha.component.image.textpaster.DecoratedRandomTextPaster;
import com.octo.captcha.component.image.textpaster.TextPaster;
import com.octo.captcha.component.image.textpaster.textdecorator.LineTextDecorator;
import com.octo.captcha.component.image.textpaster.textdecorator.TextDecorator;
import com.octo.captcha.component.image.wordtoimage.ComposedWordToImage;
import com.octo.captcha.component.image.wordtoimage.WordToImage;
import com.octo.captcha.component.word.wordgenerator.RandomWordGenerator;
import com.octo.captcha.component.word.wordgenerator.WordGenerator;
import com.octo.captcha.engine.image.ListImageCaptchaEngine;
import com.octo.captcha.image.gimpy.GimpyFactory;
import com.octo.captcha.service.captchastore.FastHashMapCaptchaStore;
import com.octo.captcha.service.image.DefaultManageableImageCaptchaService;
import com.octo.captcha.service.image.ImageCaptchaService;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author nuo.qin
 */
public class CaptchaService {
	private static final ImageCaptchaService instance = 
			new DefaultManageableImageCaptchaService(
					new FastHashMapCaptchaStore(), 
					new JCaptchaEngine(), 180,  100000 , 75000);
 
    public static ImageCaptchaService getInstance(){
        return instance;
    }
	
	public static void responseCaptchaImage(HttpServletRequest req, 
			HttpServletResponse rsp, String sessionId) throws IOException {
		byte[] jpegByteArray;
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			BufferedImage challenge = getInstance().getImageChallengeForID(
						sessionId);
			ImageIO.write(challenge, "jpeg", os);
			jpegByteArray = os.toByteArray();
		}
		// Respont image to browser.
		rsp.setHeader("Cache-Control", "no-store");
		rsp.setHeader("Pragma", "no-cache");
		rsp.setDateHeader("Expires", 0);
		rsp.setContentType("image/jpeg");
		try (ServletOutputStream responseOutputStream = rsp.getOutputStream()) {
			responseOutputStream.write(jpegByteArray);
			responseOutputStream.flush();
		}
	}
	
	public static boolean verifyCaptcha(String sessionId, String captcha) {
		try {
			return getInstance().validateResponseForID(sessionId, captcha.toUpperCase());
		} catch (Exception err) {
			return false;
		}
	}
}

class JCaptchaEngine extends ListImageCaptchaEngine {

	private static final Integer MIN_WORD_LENGTH = 4;
	private static final Integer MAX_WORD_LENGTH = 4;
	private static final Integer IMAGE_HEIGHT = 30;
	private static final Integer IMAGE_WIDTH = 90;
	private static final Integer MIN_FONT_SIZE = 18;
	private static final Integer MAX_FONT_SIZE = 18;
	private static final String RANDOM_WORD = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	private static final Color BACKCOLOR = new Color(0xEE, 0xEE, 0xEE, 0xFF);
	// Random font
	private static final Font[] RANDOM_FONT = new Font[] { 
			new Font("nyala", Font.BOLD, MIN_FONT_SIZE), 
			new Font("Arial", Font.BOLD, MIN_FONT_SIZE),
			new Font("Bell MT", Font.BOLD, MIN_FONT_SIZE), 
			new Font("Credit valley", Font.BOLD, MIN_FONT_SIZE),
			new Font("Impact", Font.BOLD, MIN_FONT_SIZE) 
	};

	// Random color
	private static final Color[] RANDOM_COLOR = new Color[] { 
			new Color(80, 120, 200)
	};
	
	// Random color
	private static final Color[] LINE_COLOR = new Color[] { 
			new Color(255, 0, 0, 100),
			new Color(0, 255, 0, 100),
			new Color(0, 0, 255, 100),
			new Color(255, 255, 0, 100),
			new Color(0, 255, 255, 100),
			new Color(255, 0, 255, 100)
	};

	// Generate captcha
	@Override
	protected void buildInitialFactories() {

		RandomListColorGenerator randomListColorGenerator = 
				new RandomListColorGenerator(RANDOM_COLOR);
		RandomListColorGenerator lineColorGenerator = 
				new RandomListColorGenerator(LINE_COLOR);
		
		BackgroundGenerator backgroundGenerator = 
				new UniColorBackgroundGenerator(IMAGE_WIDTH, IMAGE_HEIGHT, BACKCOLOR);
		WordGenerator wordGenerator = new RandomWordGenerator(RANDOM_WORD);
		FontGenerator fontGenerator = new RandomFontGenerator(
				MIN_FONT_SIZE, MAX_FONT_SIZE, RANDOM_FONT);
		TextDecorator[] textDecorator = new TextDecorator[1];
		textDecorator[0] = new LineTextDecorator(1,lineColorGenerator);
		
		TextPaster textPaster = new DecoratedRandomTextPaster(
				MIN_WORD_LENGTH, MAX_WORD_LENGTH, 
				randomListColorGenerator, textDecorator);
		WordToImage wordToImage = new ComposedWordToImage(
				fontGenerator, backgroundGenerator, textPaster);

		addFactory(new GimpyFactory(wordGenerator, wordToImage));
	}

}
