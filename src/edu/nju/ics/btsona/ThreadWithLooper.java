package edu.nju.ics.btsona;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
 
/**
 * ���̷߳�����Ϣ��UI�̣߳����̣߳�
 * org.fneg.ThreadWithLooper.java
 * Create at: 2012-6-4 ����4:58:11
 * @author:feng<br/>
 * Email:fengcunhan@gmail.com
 *
 */
public class ThreadWithLooper extends Thread {
    private Handler handler;
     
    private Handler uiHandler;
     
    public ThreadWithLooper(Handler mHandler){
        this.uiHandler=mHandler;
        //��ʼ��Handler�����յ����̷߳��͹�����Message�ͻظ�һ��Message�����̣߳���Ϣ������ һ���ַ����͵�ǰʱ��
        handler =new Handler(){
 
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what){
                case Messages.MSG_HELLO:
                     Message message=new Message();
                     message.what=Messages.MSG_HELLO;
                     message.obj="Yes!I get a hello"+System.currentTimeMillis();
                     uiHandler.sendMessage(message);
                    break;
                     
                }
            }
             
        };
    }
     
    public Handler getHandler() {
        return handler;
    }
 
    public void setHandler(Handler handler) {
        this.handler = handler;
    }
     
    @Override
    public void run() {
        Looper.prepare();
         
        Looper.loop();
    }
     
     
}