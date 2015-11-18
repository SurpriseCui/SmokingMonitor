package edu.nju.ics.btsona;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
/*
 * 
 * ��ã�
 * templete0
 * sampleFrameLength = 400;
 * Arrays.copyOfRange(original, 400, 1600);
 * dtw.getDistance()) < 15.5
 */
public class AudioRecorder {

	/**
	 * Settings
	 */
	private static final int RECORDER_BPP = 16;
	public static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
	private String AUDIO_RECORDER_FOLDER = "AudioRecorder";
	private String AUDIO_RECORDER_TEMP_FILE = null;
	private static int RECORDER_SAMPLERATE = 44100; //44100
	private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	// private static final int RECORDER_CHANNELS =
	// AudioFormat.CHANNEL_IN_STEREO;
	private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

	private AudioRecord recorder = null;
	private int bufferSize = 0;
	private Thread recordingThread = null;
	private Thread recordingLogThread = null;
	private Thread changeUIThread = null;
	
	private Date beginTime = new Date();
	private Wave waveTemplete = null;
	double original[] = null;
	double templete[] = null;
	static int sampleFrameLength = 1200;//8K sample rate //0.1s
	static int start = 1600;	//ȥ����ǰ�˵�����
	static int lighterTime = 0;
	static TextView tv_lighter = null;
	static Button bb = null;
	static int deepBreathTime = 0;
	static TextView tv_deepBreath = null;
	
	private static double window = 0.2;
	private static double OverlapTime = 0;
	private static int Overlap = 0;
	private static int bufferTimes = 250;
	
	private static double bigWindow = 2;
	private static int mingap = (int) (bigWindow / window * 0.3);
	private static int bigWindowLen = (int) (bigWindow / window);
	
	private Handler UIHandler;
	
	private static BluetoothAdapter mAdapter;
	/**
	 * Recording state
	 */

	private boolean isRecording = false;

	private String soundFileName = null;
	
    public Handler getHandler() {
        return UIHandler;
    }
 
    public void setHandler(Handler handler) {
        this.UIHandler = handler;
    }

	public AudioRecorder(TextView tv_l, TextView tv_d) {
		bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
				RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
		this.tv_lighter = tv_l;
		this.tv_deepBreath = tv_d;
	}
	
	public AudioRecorder() {
		bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
				RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
	}

    Handler mHandler = new Handler() {  
        
        @Override  
        public void handleMessage(Message msg) {  
            super.handleMessage(msg);  
           
            switch (msg.what) {  
            case 0:  
                //������������,�õ�����  
            	tv_lighter.setText("����������" + ++lighterTime);
                Message message = new Message();
                message.what = Messages.MSG_LIGHTER;
                message.obj = Integer.toString(lighterTime);
                UIHandler.sendMessage(message);
                
                break;  
            case 1:
            	tv_deepBreath.setText("�����������" + ++deepBreathTime);
                break;
            default:  
                break;  
            }  
        }  
  
    };  
	
	/**
	 * Start recording audio
	 */
	public void startRecording(String folderName, String fileName,
			int audioSource) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		this.AUDIO_RECORDER_TEMP_FILE = "record_temp_" + fileName + ".raw";
		this.AUDIO_RECORDER_FOLDER = folderName;
		this.soundFileName = this.createSoundFile(folderName, fileName);
		
		this.waveTemplete = new Wave(Environment
				.getExternalStorageDirectory().getAbsolutePath()
				+ "/"
				+ folderName + "/source/templete1.wav");
		this.original = waveTemplete.getSampleAmplitudes();
		this.templete = Arrays.copyOfRange(original, 400, 1600);

		recorder = new AudioRecord(audioSource, RECORDER_SAMPLERATE,
				RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING, bufferSize);

		int i = recorder.getState();
		if (i == 1)
			recorder.startRecording();

		isRecording = true;

		recordingThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					beginTime= new Date();
//					System.out.println("beginTime" + beginTime.getTime());
					writeAudioDataToFile();
				} catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}, "AudioRecorder Thread");

		recordingThread.start();
	}

	private void writeAudioDataToFile() throws UnsupportedEncodingException {
		byte data[] = new byte[bufferSize];
		byte dataBuffer[] = new byte[bufferSize * bufferTimes];
		
		String filename = getTempFilename();
		FileOutputStream os = null;
		Log.e("write to", filename);
		try {
			os = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		int read = 0;
		int times = 0;

		if (null != os) {
			
			while (isRecording) {
				BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
				
				read = recorder.read(data, 0, bufferSize);
				for(int j = 0; j < data.length; j ++){
					dataBuffer[times * bufferSize + j] = data[j];
				}
				times += 1;
				
//				Log.e("times:",Integer.toString(times) + ":" + Integer.toString(bufferSize));
//				//���㵱ǰ�Ѿ�¼��ʱ��
//				Date nowTime = new Date();
//				long time_temp = nowTime.getTime() - beginTime.getTime();    //��������
//		        long hours = time_temp / 1000 / 3600;                //���Сʱ��
//		        long temp2 = time_temp % (1000 * 3600);
//		        long mins = temp2 / 1000 / 60;                    //��������
//		        
////		        System.out.println("nowTime" + nowTime.getTime());
////		        System.out.println(time_temp);
////		        System.out.println("hour:" + hours + "mins:" + mins);
//		        
//		        //�½�һ���߳�����ÿ��һ��ʱ����log
//				if(mins >= 1){
//					beginTime = new Date();
//					String folderName = "microphone-sona";
//					final String nowLogFile = this.createSoundFile(folderName, nowTime
//							+ AudioRecorder.AUDIO_RECORDER_FILE_EXT_WAV);
//					
//					recordingLogThread = new Thread(new Runnable() {
//						@Override
//						public void run() {
//							copyWaveFile(getTempFilename(), nowLogFile);
//						}
//					}, "AudioRecorderLog Thread");
//
//					recordingLogThread.start();
//					
////					deleteTempFile();
////					filename = getTempFilename();
////					os = null;
////					Log.e("write to", filename);
////					try {
////						os = new FileOutputStream(filename);
////					} catch (FileNotFoundException e) {
////						e.printStackTrace();
////					}
//				}
				
				//ʵʱ��������
				if (AudioRecord.ERROR_INVALID_OPERATION != read) {
					try {
						//�жϴ���
						if(times == bufferTimes){
							times = 0;
//							Log.e("read","----------------------------");
							double Data[] = getSampleAmplitudes(dataBuffer);
							Spectrogram sg = new Spectrogram(Data);
							double frameInSec = (double)sg.getFftSampleSize() / (double)RECORDER_SAMPLERATE;
							
							//������Ƶ�ʵ�Ԫ���Լ�ÿ��Ƶ�ʵ�Ԫ����(in HZ)
							int numFrequencyUnit = sg.getNumFrequencyUnit();
							double frequncyInHZ = (double)RECORDER_SAMPLERATE / (double)numFrequencyUnit;
							
							int numFrameInWindow = (int) (window / frameInSec);
							double windowLen = numFrameInWindow * frameInSec;
							
							//������ĳ��Ƶ�����������
							double [][] test = sg.getAbsoluteSpectrogramData();
							//����ÿһ֡ÿ���Ӵ�����
							double [][]subPower = new double[test.length][8];
							
							if(OverlapTime == 0)
								Overlap = numFrameInWindow;
							else
								Overlap = (int) (numFrameInWindow / (window - OverlapTime));
							double[] resultAvg = new double[test.length / Overlap + 1];
							double[] resultMen = new double[test.length / Overlap + 1];
							
							int index = 0;
							int flag = 0;
							
							mAdapter = BluetoothAdapter.getDefaultAdapter();
							if(!mAdapter.isEnabled()){
								//��i֡��ת����ʱ��i*frameInSec
								for(int i = 0; i < test.length; i ++){
									//��j��Ƶ�ʣ�ת����Ƶ��j*frequncyInHZ 	//����[w/16,w/8]�а����ܶ�Ƶ�ʶ�
									//ÿһ֡���Ӧ8��Ƶ�ʶ�[0,w/256],[w/256,w/128],[w/128,w/64],[w/64,w/32],[w/32,w/16],[w/16.w/8],[w/8,w/4],[w/4,w/2]
									for(int num = 0; num < 8; num ++){
										double result = 0;
										for(int j = (int) ((RECORDER_SAMPLERATE / Math.pow(2, 8 - (num - 1))) / (frequncyInHZ)); j < (RECORDER_SAMPLERATE / Math.pow(2, 8 - num)) / (frequncyInHZ) ; j ++){
											result += Math.log(test[i][j]);
										}
										subPower[i][num] = result;
									}
								}
//								System.out.println(test.length + ":" + numFrameInWindow + ":" + test.length / numFrameInWindow);
								
								for(int i = 0; i < test.length; i += Overlap){
									//һ������	����4���Ӵ�������ƽ��ֵ
									double []windowAvg = new double[numFrameInWindow];
									double []windowMen = new double[numFrameInWindow];
									int num = 0;
									for(int j = 0; j < numFrameInWindow; j ++){
										num ++;
										if(i + j >= test.length){
											flag = 1;
											break;
										}
										windowAvg[j] = subPower[i + j][5];
										windowMen[j] = subPower[i + j][7];
	//									System.out.println(windowAvg[j] + ":" + windowMen[j]);
									}
									if(flag == 1){
										windowAvg = Arrays.copyOfRange(windowAvg, 0, num - 1);
										windowMen = Arrays.copyOfRange(windowMen, 0, num - 1);
										flag = 0;
									}
									resultAvg[index] = getAverage(windowAvg);
									resultMen[index] = getMedian(windowMen);
									index ++;
								}
								
								int bigWindowLen = (int)(bigWindow / window);
								int []timeSquence = new int[resultAvg.length];
								for(int i = 0; i < resultAvg.length; i ++){
									if((resultAvg[i] > 70) && (resultMen[i] > 450) ){
										timeSquence[i] = 1;
									}
									else
										timeSquence[i] = 0;
								}
								
								for(int i = 0; i < timeSquence.length; i ++){
									int sum = 0;
									for(int j = 0; j < bigWindowLen; j ++){
										if(i + j >= timeSquence.length)
											break;
										sum += timeSquence[i + j];
									}
									if(sum > 2){
										for(int j = 0; j < bigWindowLen; j ++){
											if(i + j >= timeSquence.length)
												break;
											timeSquence[i + j] = 0;
										}
									}
	//								Log.e("test",Integer.toString(i));
									if(sum == 2){
										for(int j = 0; j < bigWindowLen; j ++){
	//										System.out.print(timeSquence[i + j]);
											if(i + j>= timeSquence.length)
												break;
											if(timeSquence[i + j] == 1){
												for(int p = 0; p < mingap; p ++){
													if(i + j + p >= timeSquence.length)
														break;
													if(timeSquence[i + j + p] == 1)
														timeSquence[i + j + p] = 0;
												}
											}
										}
	//									System.out.println();
									}
								}
								
								for(int i = 0; i  < timeSquence.length; i++ ){
									if(timeSquence[i] > 0){
										Log.e("read", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
										changeUIThread = new Thread(new Runnable() {
											@Override
											public void run() {
												//��ʱ���������֮������Ϣ��Handler�����UI���£�  
								                mHandler.sendEmptyMessage(0);  
								                  
											}
										}, "changeUI Thread");
										changeUIThread.start();
									}
								}
							}
						}
						//д����ʱ�ļ�
						os.write(data);
						}
						catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			try {
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
//			while (isRecording) {
//				read = recorder.read(data, 0, bufferSize);
//
//				if (AudioRecord.ERROR_INVALID_OPERATION != read) {
//					try {
//						os.write(data);
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				}
//			}
//
//			try {
//				os.close();
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
		}
	}

	private String createSoundFile(String folderName, String fileName) {
		File f = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/" + folderName + "/");
		if (!f.exists()) {
			f.mkdirs();
		}
		f = new File(Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/" + folderName + "/" + fileName);
		return f.getAbsolutePath();
	}

	/**
	 * Stop recording audio
	 */
	public void stopRecording() {
		if (null != recorder) {
			isRecording = false;

			int i = recorder.getState();
			if (i == 1)
				recorder.stop();
			recorder.release();

			recorder = null;
			recordingThread = null;
			recordingLogThread = null;
			changeUIThread = null;
		}

		Log.e("read",  "copyWaveFile");
		copyWaveFile(getTempFilename(), this.soundFileName);
		deleteTempFile();
		Log.e("read",  "deleteTempFile");
	}

	private void deleteTempFile() {
		File file = new File(getTempFilename());
		file.delete();
	}

	private void copyWaveFile(String inFilename, String outFilename) {
		System.err.println("cp " + inFilename + " " + outFilename);
		FileInputStream in = null;
		FileOutputStream out = null;
		long totalAudioLen = 0;
		long totalDataLen = totalAudioLen + 36;
		long longSampleRate = RECORDER_SAMPLERATE;
		int channels = 1;
		long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

		byte[] data = new byte[bufferSize];

		try {
			in = new FileInputStream(inFilename);
			out = new FileOutputStream(outFilename);
			totalAudioLen = in.getChannel().size();
			totalDataLen = totalAudioLen + 36;

			WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
					longSampleRate, channels, byteRate);

			while (in.read(data) != -1) {
				out.write(data);
			}

			in.close();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean getIsRecording() {
		return this.isRecording;
	}

	private String getTempFilename() {
		String filepath = Environment.getExternalStorageDirectory().getPath();
		File file = new File(filepath, AUDIO_RECORDER_FOLDER);

		if (!file.exists()) {
			file.mkdirs();
		}

		return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
	}

	private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
			long totalDataLen, long longSampleRate, int channels, long byteRate)
			throws IOException {

		byte[] header = new byte[44];

		header[0] = 'R'; // RIFF/WAVE header
		header[1] = 'I';
		header[2] = 'F';
		header[3] = 'F';
		header[4] = (byte) (totalDataLen & 0xff);
		header[5] = (byte) ((totalDataLen >> 8) & 0xff);
		header[6] = (byte) ((totalDataLen >> 16) & 0xff);
		header[7] = (byte) ((totalDataLen >> 24) & 0xff);
		header[8] = 'W';
		header[9] = 'A';
		header[10] = 'V';
		header[11] = 'E';
		header[12] = 'f'; // 'fmt ' chunk
		header[13] = 'm';
		header[14] = 't';
		header[15] = ' ';
		header[16] = 16; // 4 bytes: size of 'fmt ' chunk
		header[17] = 0;
		header[18] = 0;
		header[19] = 0;
		header[20] = 1; // format = 1
		header[21] = 0;
		header[22] = (byte) channels;
		header[23] = 0;
		header[24] = (byte) (longSampleRate & 0xff);
		header[25] = (byte) ((longSampleRate >> 8) & 0xff);
		header[26] = (byte) ((longSampleRate >> 16) & 0xff);
		header[27] = (byte) ((longSampleRate >> 24) & 0xff);
		header[28] = (byte) (byteRate & 0xff);
		header[29] = (byte) ((byteRate >> 8) & 0xff);
		header[30] = (byte) ((byteRate >> 16) & 0xff);
		header[31] = (byte) ((byteRate >> 24) & 0xff);
		header[32] = (byte) (2 * 16 / 8); // block align
		header[33] = 0;
		header[34] = RECORDER_BPP; // bits per sample
		header[35] = 0;
		header[36] = 'd';
		header[37] = 'a';
		header[38] = 't';
		header[39] = 'a';
		header[40] = (byte) (totalAudioLen & 0xff);
		header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
		header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
		header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

		out.write(header, 0, 44);
	}
	
    public double[] getSampleAmplitudes(byte[] data){
        int bytePerSample = 2;
        int numSamples = data.length / bytePerSample;
        double[] amplitudes = new double[numSamples];
        
        int pointer = 0;
        for (int i = 0; i < numSamples; i++) {
            short amplitude = 0;
            for (int byteNumber = 0; byteNumber < bytePerSample; byteNumber++) {
                    // little endian
                    amplitude |= (short) ((data[pointer++] & 0xFF) << (byteNumber * 8));
            }  
            amplitudes[i] = amplitude;
        }
        
        return amplitudes;
    }
    
	 /**
	  * �����˫����������ֵ�ĺ�
	  *
	  * @param inputData
	  *            ������������
	  * @return ������
	  */
	 public static double getSum(double[] inputData) {
		 if (inputData == null || inputData.length == 0)
			 return -1;
		 int len = inputData.length;
		 double sum = 0;
		 for (int i = 0; i < len; i++) {
//			 System.out.println(inputData[i]);
			 sum = sum + inputData[i];
			 
		 }
		 return sum;
		 
	 }
	 
	 /**
	  * �����˫����������ֵ��ƽ��ֵ
	  *
	  * @param inputData
	  *            ������������
	  * @return ������
	  */
	 public static double getAverage(double[] inputData) {
		 if (inputData == null || inputData.length == 0)
			 return -1;
		 int len = inputData.length;
		 double result;
		 result = getSum(inputData) / len;
		 
		 return result;
	 }
	 
	 public static double getMedian(double[] inputData){
		 ArrayList<Double> arr=new ArrayList<Double>();
		 for(int i = 0; i < inputData.length; i ++){
				 arr.add(inputData[i]);
             
		 }
		 
		 Collections.sort(arr);
		 Double j = arr.get(inputData.length/2);
       	 if(inputData.length%2==0){
       		 j=(arr.get(inputData.length/2)+arr.get(inputData.length/2+1))/2;
       		 return j;
       	 }else{
       		 j=arr.get(inputData.length/2) ;
       		 return j;
       	 }
	 }
}
