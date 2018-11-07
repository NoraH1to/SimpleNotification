package sterbenj.com.simplenotification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import org.litepal.LitePal;
import org.litepal.tablemanager.callback.DatabaseListener;

import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.app.NotificationCompat;

public class MainActivity extends AppCompatActivity {

    String TAG = "NoraHito";
    String OLD_TIPS = "OLDT";
    String NEW_TIPS = "NEWT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initSql();

        super.onCreate(savedInstanceState);

        //开机自启
        if(getIntent().getStringExtra("ACTION_BOOT") != null){
            reFlash();
            return;
        }

        //删除操作
        if(getIntent().getStringExtra("ACTION_DELETE") != null){
            deleteNotice(getIntent().getIntExtra("channel_id", -1), getIntent().getLongExtra("id", -1));
            return;
        }
        setContentView(R.layout.activity_main);

        initButton();
        initFab();


    }

    //初始化数据库监听
    private void initSql(){
        LitePal.registerDatabaseListener(new DatabaseListener() {
            @Override
            public void onCreate() {
            }

            @Override
            public void onUpgrade(int oldVersion, int newVersion) {
//                if (oldVersion < 4 && newVersion >= 6){
//                    List<Tip> tips = LitePal.order("channel_id desc").find(Tip.class);
//                    for (Tip tip : tips){
//                        tip.setTime(-1);
//                        tip.updateAll("id = ?", String.valueOf(tip.getId()));
//                    }
//                }
            }
        });
    }

    //推送Notification
    private void pushNotification(int channel_id, long sql_id ,String Title, String Context, long Time){
        //初始化通知对象
        Notification notification;
        NotificationCompat.Builder builder;
        builder = new NotificationCompat.Builder(this, "NoraHito" + channel_id);

        //安卓8以上申请通知通道
        NotificationManager mNotificationManager = (NotificationManager)getSystemService(this.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("NoraHito" + channel_id,
                    "提醒", NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(notificationChannel);
            builder.setChannelId("NoraHito" + channel_id);
        }

        //设置通知内容
        if (Title.isEmpty())
            Title = "无标题";
        builder.setContentTitle(Title);
        if (Context.isEmpty())
            Context = "无详情";
        builder.setContentText(Context);
        builder.setSmallIcon(R.drawable.ic_add_withe_24dp);
        builder.setWhen(Time);

        //设置完成按钮动作
        Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
        intent1.putExtra("id", sql_id);
        intent1.putExtra("channel_id", channel_id);
        intent1.putExtra("ACTION_DELETE", "ACTION_DELETE");
        PendingIntent pendingIntent1 = PendingIntent.getActivity(this, channel_id + 233, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.addAction(R.drawable.ic_done_black_24dp, "完成" ,pendingIntent1);

        //设置通知点击跳转
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("id", sql_id);
        intent.putExtra("channel_id", channel_id);
        intent.putExtra("Title", Title);
        intent.putExtra("Context", Context);
        intent.putExtra(OLD_TIPS, OLD_TIPS);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, channel_id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);

        //完成通知对象，设置通知常驻
        notification = builder.build();
        notification.flags = Notification.FLAG_ONGOING_EVENT;

        //显示通知
        mNotificationManager.notify(channel_id, notification);
    }

    //检查是否有为关掉的通知
    public void reFlash(){
        List<Tip> tips = LitePal.order("channel_id asc").find(Tip.class);
        if(tips.size() == 0){
            Toast.makeText(getApplicationContext(), "没有可以恢复的通知", Toast.LENGTH_SHORT).show();
            return;
        }
        for (int i = 0; i < tips.size(); i++){
            Tip tip = tips.get(i);

            long sql_id = tip.getId();
            int channel_id = tip.getChannel_id();
            String Title = tip.getTitle();
            String Context = tip.getContext();
            long Time = tip.getTime();

            pushNotification(channel_id, sql_id, Title, Context, Time);

            finish();
        }
    }

    //初始化fab
    private void initFab(){
        final FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab_newNotice);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab.setImageDrawable(null);
                startAnimation(fab);
            }
        });
    }

    //开始fab动画
    private void startAnimation(View view) {
        Animation animation = new ScaleAnimation(1, 10, 1, 10, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        animation.setDuration(200);
        animation.setFillAfter(true);
        view.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                newTips();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }

    //打开新的编辑界面
    private void newTips(){
        Intent intent = new Intent(MainActivity.this, MainActivity.class);
        intent.putExtra(NEW_TIPS, NEW_TIPS);
        startActivity(intent);
        MainActivity.this.overridePendingTransition(0, 0);
        MainActivity.this.finish();
    }

    //初始化按钮
    private void initButton(){
        final int UPDATE = 0;
        final int CREATE = 1;
        final int ACTION;

        final Intent intent = getIntent();
        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab_newNotice);
        AppCompatButton ConfirmButton =  (AppCompatButton)findViewById(R.id.confirm_button);
        AppCompatButton CancelButton = (AppCompatButton)findViewById(R.id.cancel_button);
        AppCompatButton ReflashButton = (AppCompatButton)findViewById(R.id.reflash_button);

        //来自新建
        if(intent.getStringExtra(NEW_TIPS) != null){
            CancelButton.setVisibility(View.GONE);
            fab.hide();
            ACTION = CREATE;
        }
        //来自已有
        else if (intent.getStringExtra(OLD_TIPS) != null) {
            ACTION = UPDATE;
            ReflashButton.setVisibility(View.GONE);
            String Title = intent.getStringExtra("Title");
            String Context = intent.getStringExtra("Context");
            long id = intent.getLongExtra("id", -1);
            int channel_id = intent.getIntExtra("channel_id", -1);
            initText(Title, Context);
        }
        //直接打开app
        else{
            CancelButton.setVisibility(View.GONE);
            fab.hide();
            ACTION = CREATE;
        }

        ConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (ACTION){
                    case UPDATE:
                        updateNotice(intent.getLongExtra("id", -1));
                        break;
                    case CREATE:
                        createNotice(getTipTitle(), getTipContext());
                        break;
                }
            }
        });

        CancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteNotice(intent.getIntExtra("channel_id", -1),
                        (intent.getLongExtra("id", -1)));
            }
        });

        ReflashButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                reFlash();
            }
        });
    }

    private void initText(String Title, String Context){
        TextInputEditText TitleEdit = (TextInputEditText)findViewById(R.id.title);
        TextInputEditText ContextEdit = (TextInputEditText)findViewById(R.id.context);
        TitleEdit.setText(Title);
        ContextEdit.setText(Context);
    }

    //创建新通知
    private void createNotice(String Title, String Context){

        //与数据库进行交互，判断如何新建channel_id
        List<Tip> tips = LitePal.order("channel_id desc").find(Tip.class);

        int channel_id;
        if (tips.size() == 0){
            channel_id = 1;
        }
        else{
            channel_id = tips.get(0).getChannel_id() + 1;
        }

        if (Title.isEmpty())
            Title = "无标题";
        if (Context.isEmpty())
            Context = "无详情";
        //同步到数据库
        Tip tip = new Tip();
        tip.setChannel_id(channel_id);
        tip.setTitle(Title);
        tip.setContext(Context);
        tip.setTime(System.currentTimeMillis());
        tip.save();

        long sql_id = tip.getId();
        long Time = tip.getTime();

        pushNotification(channel_id, sql_id, Title, Context, Time);

        finish();
    }

    //更新通知
    private void updateNotice(long sql_id){
        List<Tip> tips = LitePal.where("id = ?", String.valueOf(sql_id)).find(Tip.class);
        Tip tip = tips.get(0);
        int channel_id = tip.getChannel_id();
        long Time = tip.getTime();

        TextInputEditText Title_edit = (TextInputEditText)findViewById(R.id.title);
        TextInputEditText Context_edit = (TextInputEditText)findViewById(R.id.context);

        String Title = Title_edit.getText().toString();
        String Context = Context_edit.getText().toString();

        if (Title.isEmpty())
            Title = "无标题";
        if (Context.isEmpty())
            Context = "无详情";
        //同步到数据库
        tip.setTitle(Title);
        tip.setContext(Context);
        tip.updateAll("id = ?", String.valueOf(tip.getId()));

        pushNotification(channel_id, sql_id, Title, Context, Time);

        finish();
    }

    //删除通知
    private void deleteNotice(int id, long sql_id){
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
        LitePal.delete(Tip.class, sql_id);
        Log.d("NoraHito", "deleteNotice: " + id + "     " + sql_id);
        finish();
    }

    //获取title
    private String getTipTitle(){
        String title;
        try{
            title = ((TextInputEditText)findViewById(R.id.title)).getText().toString();
        }catch (NullPointerException e){
            return null;
        }
        return title;
    }

    //获取context
    private String getTipContext(){
        String context;
        try{
            context = ((TextInputEditText)findViewById(R.id.context)).getText().toString();
        }catch (NullPointerException e){
            return null;
        }
        return context;
    }

    //home直接清除
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (KeyEvent.KEYCODE_HOME == keyCode){
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}