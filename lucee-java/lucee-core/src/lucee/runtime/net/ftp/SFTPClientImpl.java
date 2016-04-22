package lucee.runtime.net.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Vector;

import lucee.print;
import lucee.commons.io.IOUtil;
import lucee.commons.lang.ExceptionUtil;
import lucee.commons.lang.StringUtil;
import lucee.runtime.exp.DatabaseException;
import lucee.runtime.exp.PageException;
import lucee.runtime.op.Caster;
import lucee.runtime.tag.FileTag;
import lucee.runtime.tag.Ftp;
import lucee.runtime.type.Collection;
import lucee.runtime.type.Collection.Key;
import lucee.runtime.type.KeyImpl;
import lucee.runtime.type.QueryImpl;
import lucee.runtime.type.dt.DateTimeImpl;
import lucee.runtime.type.util.KeyConstants;

import org.apache.commons.net.ftp.FTPFile;

import com.jcraft.jsch.*;

public class SFTPClientImpl extends AFTPClient {
	

	private static final Key IS_DIRECTORY = new KeyImpl("isDirectory");
	private JSch jsch;
	private int timeout=60000;
	private Session session;
	private ChannelSftp channelSftp;
	private InetAddress host;
	private int port;
	private String username;
	private String password;
	private boolean stopOnError;
	private String fingerprint;
	private String replyString;
	private int replyCode;
	private boolean positiveCompletion;

	SFTPClientImpl(){
		jsch = new JSch();
	}
	
	@Override
	public void init(InetAddress host, int port, String username, String password, String fingerprint, boolean stopOnError) throws SocketException, IOException {
		if(port<1)port=22;
		this.host=host;
		this.port=port;
		this.username=username;
		this.password=password;
		this.fingerprint=fingerprint==null?null:fingerprint.trim();
		this.stopOnError=stopOnError;
		

		print.e(host);
		print.e(port);
		print.e(username);
		print.e(password);
		print.e(fingerprint);
	}
	

	@Override
	public void connect() throws SocketException, IOException {
		print.e(host);
		print.e(port);
		print.e(username);
		print.e(password);
		print.e(fingerprint);
		try {
			//jsch.setKnownHosts("");
			session = jsch.getSession(username,host.getHostAddress() , port);
			/**/
			java.util.Properties config = new java.util.Properties(); 
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			
			
			
			//UserInfo ui=new UserInfoImpl();
			if(!StringUtil.isEmpty(password)) 
				session.setPassword(password);
			
		    //session.setUserInfo(ui);
			if(timeout>0) session.setTimeout(timeout);
		    session.connect();
		    
		    Channel channel=session.openChannel("sftp");
		    channel.connect();
		    channelSftp = (ChannelSftp)channel;
		    
		    
		    // check fingerprint
		    
			if(!StringUtil.isEmpty(fingerprint)) {
				if(!fingerprint.equalsIgnoreCase(fingerprint())) {
					disconnect();
					throw new IOException("given fingerprint is not a match.");
				}
	        }
		    handleSucess();
		}
		catch(JSchException e){
			handleFail(e, stopOnError);
		}
	}
	

	private String fingerprint() {
		return session.getHostKey().getFingerPrint(jsch);
	}

	
	public static void main(String[] args) throws SocketException, IOException, PageException {
		SFTPClientImpl client=new SFTPClientImpl();
		InetAddress host = InetAddress.getByName("virola.viviotech.net");
		String user = "root";
		String pass="Eunn:.TidkiWaj3";
		String fingerprint="aa:e8:43:3e:6b:44:91:a3:d3:da:07:34:90:2a:c8:5b";
		int port=10022;
		
		client.init(host, port, user, pass,null,true);
		client.connect();
		
		// fingerprint
		print.e("fingerprint: "+client.fingerprint());
		
		// list files
		FTPFile[] files = client.listFiles("/");
		print.e(Ftp.toQuery(files, "sftp","/", host.getHostAddress()));
		
		
		client.disconnect();
		print.e("done!");
		
		// UnknownHostKey: 205.210.189.210. RSA key fingerprint is aa:e8:43:3e:6b:44:91:a3:d3:da:07:34:90:2a:c8:5b
		
	}

	@Override
	public boolean rename(String from, String to) throws IOException {
		try {
			channelSftp.rename(from, to);
			handleSucess();
			return true;
		}
		catch (SftpException e) {
			handleFail(e, stopOnError);
		}
		return false;
	}

	@Override
	public boolean removeDirectory(String pathname) throws IOException {
		try {
			channelSftp.rmdir(pathname);
			handleSucess();
			return true;
		}
		catch(SftpException ioe) {
			handleFail(ioe, stopOnError);
		}
		return false;
	}
	

	@Override
	public boolean makeDirectory(String pathname) throws IOException {
		try {
			channelSftp.mkdir(pathname);
			handleSucess();
			return true;
		}
		catch(SftpException ioe) {
			handleFail(ioe, stopOnError);
		}
		return false;
	}

	@Override
	public boolean directoryExists(String pathname) throws IOException {
		try {
			String pwd=channelSftp.pwd();
			channelSftp.cd(pathname);
			channelSftp.cd(pwd); // we change it back to what it was
			handleSucess();
			return true;
		}
		catch(SftpException e) {/*do nothing*/}
		return false;
	}
	

	@Override
	public boolean changeWorkingDirectory(String pathname) throws IOException {
		try {
			channelSftp.cd(pathname);
			handleSucess();
			return true;
		}
		catch(SftpException ioe) {
			handleFail(ioe, stopOnError);
		}
		return false;
	}
	

	@Override
	public String printWorkingDirectory() throws IOException {
		try {
			String pwd = channelSftp.pwd();
			handleSucess();
			return pwd;
		}
		catch(SftpException ioe) {
			handleFail(ioe, stopOnError);
		}
		return null;
	}

	
	@Override
	public boolean deleteFile(String pathname) throws IOException {
		try {
			channelSftp.rm(pathname);
			handleSucess();
			return true;
		}
		catch(SftpException ioe) {
			handleFail(ioe, stopOnError);
		}
		return false;
	}
	

	@Override
	public boolean retrieveFile(String remote, OutputStream local) throws IOException {
		boolean success=false;
		try {
			channelSftp.get(remote, local);
			handleSucess();
			success = true;
		}
		catch(SftpException ioe) {
			handleFail(ioe, stopOnError);
		}
		return success;
	}
	

	
	@Override
	public boolean storeFile(String remote, InputStream local) throws IOException {
		try {
			this.channelSftp.put(local, remote); // TODO add progress monitor?
			handleSucess();
			return true;
		}
		catch(SftpException ioe) {
			handleFail(ioe, stopOnError);
		}
		return false;
	}
	
	@Override
	public int getReplyCode() {
		return replyCode;
	}

	@Override
	public String getReplyString() {
		return replyString;
	}

	@Override
	public FTPFile[] listFiles(String pathname) throws IOException {
		pathname=cleanPath(pathname);
		List<FTPFile> files=new ArrayList<FTPFile>();
		try {
			Vector list = channelSftp.ls(pathname);
			Iterator<ChannelSftp.LsEntry> it = list.iterator();
			ChannelSftp.LsEntry entry;
			SftpATTRS attrs;
			FTPFile file;
			String fileName;
			
			while(it.hasNext()){
				entry=it.next();
				attrs = entry.getAttrs();
				fileName=entry.getFilename();
				if(fileName.equals(".") || fileName.equals("..")) continue;
				
				file=new FTPFile();
				files.add(file);
				// is dir
				file.setType(attrs.isDir()?FTPFile.DIRECTORY_TYPE:FTPFile.FILE_TYPE);
				file.setTimestamp(Caster.toCalendar(attrs.getMTime()*1000L, null, Locale.ENGLISH));
				file.setSize(attrs.isDir()?0:attrs.getSize());
				FTPConstant.setPermission(file, attrs.getPermissions());
				file.setName(fileName);
			}
			handleSucess();
		}
		catch (SftpException e) {
			handleFail(e, stopOnError);
		}
		
		return files.toArray(new FTPFile[files.size()]);
	}
	
	
	

	
	private String cleanPath(String pathname) {
		if(!pathname.endsWith("/"))
			pathname = pathname + "/";
		
		return pathname;
	}
	
	
	
	

	@Override
	public boolean setFileType(int fileType) throws IOException {
		// not used
		return true;
	}


	@Override
	public String getPrefix() {
		return "sftp";
	}

	@Override
	public InetAddress getRemoteAddress() {
		return host;
	}

	@Override
	public boolean isConnected() {
		return channelSftp.isConnected();
	}

	@Override
	public int quit() throws IOException {
		// do nothing
		return 0;
	}

	@Override
	public void disconnect() throws IOException {
		if(session!=null && session.isConnected()) {
			session.disconnect();
			session=null;
		}
	}

	@Override
	public void setTimeout(int timeout) {
		this.timeout=timeout;
		if(session!=null) {
			try {
				session.setTimeout(timeout);
			}
			catch (JSchException e) {}
		}
	}

	@Override
	public int getDataConnectionMode() {
		// not used
		return -1;
	}

	@Override
	public void enterLocalPassiveMode() {
		// not used
	}

	@Override
	public void enterLocalActiveMode() {
		// not used

	}

	@Override
	public boolean isPositiveCompletion() {
		return positiveCompletion;
	}
	
	private void handleSucess() {
		replyCode=0;
		replyString="SSH_FX_OK successful completion of the operation";
		positiveCompletion=true;
	}
	

	
	private void handleFail(Exception e, boolean stopOnError) throws IOException {
		String msg = e.getMessage()==null?"":e.getMessage();
		if (StringUtil.indexOfIgnoreCase(msg, "AUTHENTICATION") != -1 || StringUtil.indexOfIgnoreCase(msg, "PRIVATEKEY") != -1) {
			replyCode = 51;
		}
		else replyCode = 82;
		replyString=msg;
		positiveCompletion=false;
		
		if (stopOnError) {
			disconnect();
			if(e instanceof IOException) throw (IOException)e;
			throw new IOException(e);
		}
	}

}
