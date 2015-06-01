/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package simplewebserver;	

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import simplewebserver.NanoHTTPD.Response.IStatus;

/**
 *
 * @author Romsahel
 */
public class MyWebServer extends NanoHTTPD
{

	private final String rootDir;

	public MyWebServer(int port, String rootDir)
	{
		super("192.168.1.4", port);
		this.rootDir = rootDir;
	}

	@Override
	public Response serve(IHTTPSession session)
	{
		Map<String, String> header = session.getHeaders();
		Map<String, String> parms = session.getParms();
		Method method = session.getMethod();
		String uri = session.getUri();

		System.out.println(method + " '" + uri + "' ");

		if (Method.POST.equals(method) || Method.PUT.equals(method))
			handlePost(session, parms);

		File file = new File(rootDir + uri);
		if (!file.exists())
			return getNotFoundResponse();

		if (file.isDirectory())
			return listDirectory(file, header, uri);
		else
			return downloadFile(file);
	}

	private Response handlePost(IHTTPSession session, Map<String, String> parms)
	{
		Map<String, String> files = new HashMap<>();
		try
		{
			session.parseBody(files);

			final File src = new File(files.get("filename"));
//			final File dst = new File(rootDir, parms.get("filename"));
			final File dst = new File(parms.get("filename"));
			Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
			System.out.println(src.getAbsolutePath() + ": uploaded to: " + dst.getAbsolutePath());

			return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_PLAINTEXT, "ok i am ");
		} catch (IOException ex)
		{
			Logger.getLogger(MyWebServer.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ResponseException ex)
		{
			Logger.getLogger(MyWebServer.class.getName()).log(Level.SEVERE, null, ex);
		}
		return getNotFoundResponse();
	}

	private Response downloadFile(File file)
	{
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(file);
		} catch (FileNotFoundException ex)
		{
			Logger.getLogger(MyWebServer.class.getName()).log(Level.SEVERE, null, ex);
		}
		return newFixedLengthResponse(Response.Status.OK, "application/octet-stream", fis, file.getTotalSpace());
	}

	private Response listDirectory(File file, Map<String, String> header, String uri)
	{
		String htmlCode = "<li><a href=\"http://%s\">%s</a></li>";
		StringBuilder message = new StringBuilder("<ul>");
		for (File f : file.listFiles())
			message.append(String.format(htmlCode, header.get("host") + uri + f.getName(), f.getName()));
		message.append("</ul>");

		message.append("<form method=\"post\" enctype=\"multipart/form-data\" action=\"uploadfile\">\n"
					   + "    <input type=\"file\" name=\"filename\" />\n"
					   + "    <input type=\"submit\" value=\"Send\" />\n"
					   + "</form>");

		return newFixedLengthResponse(Response.Status.OK, NanoHTTPD.MIME_HTML, message.toString());
	}

	@Override
	public Response newFixedLengthResponse(IStatus status, String mimeType, String message)
	{
		Response response = super.newFixedLengthResponse(status, mimeType, message);
		response.addHeader("Accept-Ranges", "bytes");
		return response;
	}

	protected Response getNotFoundResponse()
	{
		return newFixedLengthResponse(Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Error 404, file not found.");
	}

	public static void main(String[] args)
	{
		MyWebServer server = new MyWebServer(4242, "C:\\");
		try
		{
			server.start();
			while (true)
			{

			}
		} catch (IOException ex)
		{
			Logger.getLogger(MyWebServer.class.getName()).log(Level.SEVERE, null, ex);
		}
	}
}
