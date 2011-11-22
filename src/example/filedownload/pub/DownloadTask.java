package example.filedownload.pub;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import example.filedownload.Utils;

public class DownloadTask extends AsyncTask<String, String, String> {
    private final static String TAG = "DownloadFileAsync";
    private final int threadNum = 5;
    private URL	 url;
    private String urlStr;		
    private String fileName;		// �����ļ���
    private String fileDir;		// ����Ŀ¼

    private long totalSize = -1; 
    private boolean interrupt = false;
    
    private long networkSpeed;	// ����
    private long downloadSize;
    private long downloadPercent;
    
    private Context context = null;
    private DownloadTaskListener listener = null;
    private List<DownloadThread> threadList = new ArrayList<DownloadThread>();
    
    
    public DownloadTask(Context context, 
	    String url, 
	    String fileDir, 
	    String fileName, 
	    DownloadTaskListener listener) throws MalformedURLException {
	this.context = context;
	this.listener = listener;
	
	this.urlStr = url;
	this.url = new URL(url);
	this.fileDir = fileDir;
	this.fileName = fileName;
    }
	
    public long getDownloadPercent() {
	return this.downloadPercent;
    }
    
    public long getDownloadSpeed() {
	return this.networkSpeed;
    }
    
    public long getTotalSize() {
	return this.totalSize;
    }
    
    public long getDownloadSize() {
	return this.downloadSize;
    }
    
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    public void onCancelled() {
        super.onCancelled();
        interrupt = true;          
    }
    
    @Override
    protected String doInBackground(String... urls) {
        try {
    		Log.i(TAG,"doInBackground");
    		downloadFile();
	    } catch (ClientProtocolException e) {
		Log.i(TAG, e.getMessage());
		e.printStackTrace();
	    } catch (IOException e) {
		Log.i(TAG, e.getMessage());
		e.printStackTrace();
	    } catch (InterruptedException e) {
		Log.i(TAG, e.getMessage());
		e.printStackTrace();
	    }
	    return null;
        
    }
    
    private void setHttpHeader(HttpURLConnection con) {
		con.setAllowUserInteraction(true);
		con.setConnectTimeout(5000);
		con.setDoOutput(true);
		con.setReadTimeout(10000); 
		con.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
		con.setRequestProperty("Accept-Language", "zh-CN");
		con.setRequestProperty("Referer", url.toString()); 
		con.setRequestProperty("Charset", "UTF-8");
		try {
		    con.setRequestMethod("GET");
		} catch (ProtocolException e) {
		    
		    e.printStackTrace();
		}
    }
    
    private void downloadFile() throws ClientProtocolException, IOException, InterruptedException {
    	Log.i(TAG,"downloadFile");
    	
    	long start = System.currentTimeMillis();
    	long previousTime = start;
    	long previousBytes = 0;
    	HttpURLConnection con = null;
    	// ��URL����
    	con = (HttpURLConnection) this.url.openConnection();
    	
    	// ����Http Header
    	setHttpHeader(con);
		
	totalSize = con.getContentLength();
		
	// ����Ƿ�SD������
	long storage = Utils.getAvailableStorage();
	Log.i(TAG, "storage:" + storage);
	if (totalSize > storage) {
	    Toast.makeText(context, "SD ���ڴ治��", Toast.LENGTH_LONG);
	    interrupt = true;
	    return;
	}
	
	con.disconnect();
	if (HttpStatus.SC_OK != con.getResponseCode()) {
	    Log.i(TAG,"ResponseCode: " + con.getResponseCode());
	    interrupt = true;
	    return;
	}
		
	if (totalSize < 0 && !interrupt) {
	    downloadFile();
	    return;
	}
	
	long blockSize = totalSize / threadNum;
	 
	// �����ֶ������߳�
	for(int i = 0; i < threadNum; i++) {
	    long startPos = blockSize * i;
	    long endPos = (((i + 1) / threadNum) == 1) ? totalSize - 1 : blockSize * (i + 1) - 1;
	    File file = new File(this.fileDir + this.fileName + ".part" + i);
	    if (file.exists()) {
		startPos += file.length();
		downloadSize += file.length();
	    }
		
	    DownloadThread thread = new DownloadThread(file, startPos, endPos,i);
	    threadList.add(thread);
	    if (startPos < (endPos + 1)) thread.start();
	}

	while(downloadSize < totalSize) {
	    Thread.sleep(1000);
	    downloadSize = 0;
	    if (interrupt) {
		for(int i = 0; i < threadNum; i++) {
		    downloadSize += threadList.get(i).getDownloadSize();
		    threadList.get(i).onCancel();
		}
		break;
	    } else {
		for(int i = 0; i < threadNum; i++) {
		    downloadSize += threadList.get(i).getDownloadSize();
		}
	    }
	    
	    // ÿ 100 ms ˢ����ʾһ��
	    if (System.currentTimeMillis() - previousTime > 100) {
		networkSpeed = (downloadSize - previousBytes) / (System.currentTimeMillis() - previousTime);
		previousTime = System.currentTimeMillis();
		previousBytes = downloadSize;
		
		downloadPercent = (int)((downloadSize*100)/totalSize);
		publishProgress(""+ downloadPercent);
		Log.i(TAG, "networkSpeed:" + networkSpeed + " kbps");
	    }
	  
	}
	
	long end = System.currentTimeMillis();
	networkSpeed = downloadSize / (end - start);
	Log.i(TAG, "networkSpeed:" + networkSpeed + " kbps");
	
	if (!interrupt) { // �ֶ��ļ�����
	    File file = new File(this.fileDir + this.fileName);
	    if (file.exists()) file.delete();
	    FileOutputStream randomFile = new FileOutputStream(file, true);	
	    Log.i(TAG,"downloadFile writFile");
	    
	    int currentTotalSize = 0;
	    for(int i = 0; i < threadNum; i++) {     			           			
		    byte b[] = new byte [4096];
    		    int j = 0;           			
    		    InputStream input = new FileInputStream(threadList.get(i).getFile());
    		    while((j = input.read(b)) > 0) {
    			randomFile.write(b, 0, j);
    			currentTotalSize += j;
    		    }           			
    		    input.close();
	    }
	    
	    // �������������������»��������������error,�򲿷��߳���Ҫ��������
	    if (currentTotalSize == totalSize) { 
		    for(int i = 0; i < threadNum; i++) {     			           			
			threadList.get(i).getFile().delete();
		    }
	    } else {
		    for(int i = 0; i < threadNum; i++) {   
			    long startPos = blockSize * i;
			    long endPos = (((i + 1) / threadNum) == 1) ? totalSize - 1 : blockSize * (i + 1) - 1;
			    if ((endPos - startPos + 1) != threadList.get(i).getDownloadSize()) {
				threadList.get(i).getFile().delete();
			    }
			
		    }
	    }
	    randomFile.close(); 
	}

	Log.i(TAG,"downloadFile end");
    }
    
    protected void onProgressUpdate(String... progress) {
         Log.d("ANDRO_ASYNC",progress[0]);
         
         listener.updateProcess(this.urlStr, progress[0]);
    }

    @Override
    protected void onPostExecute(String unused) {
        File file = new File(Utils.APK_ROOT + Utils.getFileNameFromUrl(urlStr));
        
        Log.i(TAG,"onPostExecute" + "totalSize: " + totalSize + "file.length():" + file.length());
        if (!interrupt && (totalSize > 0 && totalSize == file.length())) {
    	    listener.finishDownload(this.urlStr);
        }
        else if (!interrupt && (totalSize > 0 && totalSize < file.length())) { // �����ļ�У��
	    listener.startDownload(this.urlStr);
        }
        else if (totalSize < 0){
            listener.startDownload(this.urlStr);
        }
        else if (interrupt) {
    	    Log.i(TAG,"onPostExecute interrrupt true");	
        }
        Log.i(TAG,"onPostExecute end");
    }
    
    // �����߳�
    public class DownloadThread extends Thread {
        public final static int TIMEOUT = 30;
        private File file;
        private long startPos;
        private long endPos;
        private long downloadSize = -1;
        private InputStream in = null;
        private int id;
        public DownloadThread(File file, long startPos, long endPos, int id) {
        	this.file = file;
        	this.startPos = startPos;
        	this.endPos = endPos;
        	this.id = id;
        	downloadSize = this.file.length();
        }
        
       
        public void onCancel() {
    	interrupt = true;
    	if (in != null) {
		    try {
			in.close();
		    } catch (IOException e) {
			Log.i(TAG, "Can not close inputstream");
			e.printStackTrace();
		    }
    	}

        }
        
        public long getDownloadSize() {
    	return downloadSize;
        }
        
        public void setDownloadSize(long size) {
    	downloadSize = size;
        }
        
        public File getFile() {
    	return file;
        }
        
        /*
         * �ֶ�����
         */
        private void downloadFile(File file,
        	long startPos, long endPos
        	) throws ClientProtocolException, IOException, InterruptedException {

        	HttpURLConnection con = (HttpURLConnection) url.openConnection();  
        	
        	setHttpHeader(con);
		con.setRequestProperty("Range", "bytes="+ startPos + "-" + endPos);
		con.connect();
		Log.i("Thread","ResponseCode: " + con.getResponseCode() + "thread ID: " + id);
		
		if (HttpStatus.SC_OK == con.getResponseCode() || 
		    HttpStatus.SC_PARTIAL_CONTENT == con.getResponseCode()) {
		    	in = con.getInputStream();
		    	RandomAccessFile randomFile = new RandomAccessFile(file, "rw");  
			randomFile.seek(randomFile.length());
			
			long len = con.getContentLength();
			Log.i(TAG,"Thread id:" + id + " len:" + len + " endPos - startPos + 1 :" + (endPos - startPos + 1));
			byte b[] = new byte [4096];
			int j = 0;
			long currentDownloadSize = 0;
			
			while((currentDownloadSize < (endPos - startPos + 1)) && 
			      !interrupt) {
			    Log.i(TAG,"Thread Read begin " + id + "endPos - startPos + 1 :" + (endPos - startPos + 1) + " currentDownloadSize : " + currentDownloadSize);
			    Log.i(TAG,"Thread id: " + id + " inputsteam:" + in.available());
			    if (in.available() > 0) {
				j = in.read(b);
				    
				currentDownloadSize += j;
				downloadSize += j;
				randomFile.write(b, 0, j);
				
			    } else {
				in.close();
				randomFile.close();
				con.disconnect();
				Thread.sleep(5000);
				downloadFile(file, startPos, endPos);
				break;
			    }
			    Thread.sleep(1000);
			    Log.i(TAG,"Thread id" + id + "read:" + j);
			}
			Log.i(TAG,"Thread Read end " + id);
			randomFile.close();
			in.close();
			con.disconnect();
			
			Log.i("Thread","Read end ");
		}
		else {
		    interrupt = true;
		}
        }
        
        @Override
        public void run() {
    	try {
		    downloadFile(file, startPos, endPos);
		} catch (ClientProtocolException e) {
		    e.printStackTrace();
		    Log.i("Thread","" + id + "err: " + e.getMessage());
		} catch (IOException e) {
		    Log.i("Thread","" + id + "err: " + e.getMessage());
		    e.printStackTrace();
		} catch (InterruptedException e) {
		    Log.i("Thread","" + id + "err: " + e.getMessage());
		    e.printStackTrace();
		}
        }
    }
}