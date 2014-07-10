/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.thordev.webapi;

import org.thordev.webapi.utility.Serializer;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 *
 * @author nuo.qin
 */

public class FileManager {
	private final static Logger logger = Logger.getLogger(FileManager.class.getName());
	private final static int maxSize = 1024 * 1024 * 5;
	private final static String uploadDir = "/WEB-INF/upload";
	private final Map<String, String> mime = new HashMap<String, String>() {
		private static final long serialVersionUID = 0L;
		{
			put("txt", "text/plain");
			put("jpeg", "image/jpeg");
			put("jpg", "image/jpeg");
			put("png", "image/png");
			put("gif", "image/gif");
			put("pdf", "application/pdf");
			put("xml", "text/xml");
			put("doc", "application/vnd.ms-word");
			put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
			put("docm", "application/vnd.ms-word.document.macroEnabled.12");
			put("xls", "application/vnd.ms-excel");
			put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
			put("xlsm", "application/vnd.ms-excel.sheet.macroEnabled.12");
			put("ppt", "application/vnd.ms-powerpoint");
			put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
			put("ppat", "application/vnd.ms-powerpoint.presentation.macroEnabled.12");
		}
	};
	
	public void addMime(String suffix, String mimeType) {
		mime.put(suffix, mimeType);
	}
	
	public void removeMime(String suffix) {
		mime.remove(suffix);
	}
	public void clearMime() {
		mime.clear();
	}
	
	public static class FileInfo {
		public String fileId;
		public String fileName;
		public String fileExtName;
		public String mimeType; 
	}
	
	public List<FileInfo> saveUploadFiles(HttpServletRequest request) throws ServletException, IOException, FileUploadException {
		return this.saveUploadFiles(request, maxSize, uploadDir);
	}
	
	public List<FileInfo> saveUploadFiles(HttpServletRequest request, String uploadDir) throws ServletException, IOException, FileUploadException {
		return this.saveUploadFiles(request, FileManager.maxSize, uploadDir);
	}
	
	public List<FileInfo> saveUploadFiles(HttpServletRequest request, int maxSize) throws ServletException, IOException, FileUploadException {
		return this.saveUploadFiles(request, maxSize, FileManager.uploadDir);
	}
	
	public List<FileInfo> saveUploadFiles(HttpServletRequest request, int maxSize, String uploadDir)
			throws ServletException, IOException, FileUploadException {
		List<FileInfo> uploadList = new LinkedList<>();
		request.setCharacterEncoding("utf-8");
		ServletFileUpload upload = new ServletFileUpload();
		upload.setHeaderEncoding("UTF-8");

		if (!ServletFileUpload.isMultipartContent(request)) {
			return uploadList;
		}
		upload.setSizeMax(maxSize);
		FileItemIterator iter;
		iter = upload.getItemIterator(request);
		while (iter.hasNext()) {
			FileItemStream item = iter.next();
			try (InputStream stream = item.openStream()) {
				if (!item.isFormField()) {
					FileInfo info = new FileInfo();
					info.fileId = UUID.randomUUID().toString().replaceAll("-", "");
					info.fileName = item.getName();
					if (info.fileName.lastIndexOf("\\") != -1)
						info.fileName = info.fileName.substring(info.fileName.lastIndexOf("\\") + 1);
					if (info.fileName.lastIndexOf("/") != -1)
						info.fileName = info.fileName.substring(info.fileName.lastIndexOf("/") + 1);
					if (info.fileName.contains(".")) {
						info.fileExtName = info.fileName.substring(info.fileName.lastIndexOf(".") + 1);
					} else {
						logger.log(Level.WARNING, "Upload file doesn't have suffix.");
						continue;
					}
					info.mimeType = getFileMIME(info.fileExtName);
					if (info.mimeType == null) {
						logger.log(Level.WARNING, "Upload file's MIME type isn't permitted.");
						continue;
					}

					File dir = new File(
							request.getServletContext().getRealPath(uploadDir));
					dir.mkdir();

					String jsonFile = request.getServletContext().getRealPath(uploadDir)
									+ "/" + info.fileId + ".json";
					Serializer.saveJsonFile(info, jsonFile);
					String dataFile = request.getServletContext().getRealPath(uploadDir)
									+ "/" + info.fileId + ".data";

					try (BufferedOutputStream bos = new BufferedOutputStream(
							new FileOutputStream(dataFile))) {
						int length;
						byte[] buffer = new byte[4096];
						while ((length = stream.read(buffer)) != -1) {
							bos.write(buffer, 0, length);
						}
					}
					uploadList.add(info);
				} 
			}
		}
		return uploadList;
	}
	
	public void downloadFile(HttpServletRequest request, HttpServletResponse response, String fileId) 
			throws ServletException, IOException {
		downloadFile(request, response, fileId, FileManager.uploadDir);
	}

	public void downloadFile(HttpServletRequest request, HttpServletResponse response, String fileId, String uploadDir) 
			throws ServletException, IOException {
		request.setCharacterEncoding("utf-8");
		String jsonFile = request.getServletContext().getRealPath(uploadDir)
									+ "/" + fileId + ".json";
		File dataFile = new File(request.getServletContext().getRealPath(uploadDir)
						+ "/" + fileId + ".data");
		if (!dataFile.exists()) {
			Dispatcher.send(response, HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		FileInfo info;
		try {
			info = Serializer.loadJsonFile(jsonFile, FileInfo.class);
		} catch (Exception ex) {
			Dispatcher.send(response, HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String filename;
		try {
			if (info.fileName != null) {
				filename  = URLEncoder.encode(info.fileName, "utf-8");
			} else
				filename = "attachment" + (info.fileExtName == null ? "" : "." + info.fileExtName);
		} catch (UnsupportedEncodingException e1) {
			filename = "attachment";
			e1.printStackTrace();
		}
		response.setHeader("Cache-Control", "no-store");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		response.setContentType(info.mimeType);
		response.addHeader("Content-Disposition", "attachment; filename=\"" + 
					filename + "\"");
		try (OutputStream os = response.getOutputStream()) {
			try (InputStream is = new FileInputStream(dataFile)) {
				int length;
				byte[] buffer = new byte[4096];
				while ((length = is.read(buffer)) != -1) {
					os.write(buffer, 0, length);
				}
			}
		}
	}
	
	private String getFileMIME(String ext) {
		if (mime.containsKey(ext))
			return mime.get(ext.toLowerCase());
		else
			return null;
	}
	
}
