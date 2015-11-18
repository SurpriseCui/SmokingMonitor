package edu.nju.ics.btsona;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
 
/**
 * 从线程发送消息到UI线程（主线程）
 * org.fneg.ThreadWithLooper.java
 * Create at: 2012-6-4 下午4:58:11
 * @author:feng<br/>
 * Email:fengcunhan@gmail.com
 *
 */
public class ThreadWithLooper extends Thread {
    private Handler handler;
     
    private Handler uiHandler;
     
    public ThreadWithLooper(Handler mHandler){
        this.uiHandler=mHandler;
        //初始化Handler，接收到主线程发送过来的Message就回复一个Message给主线程，消息内容是 一个字符串和当前时间
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