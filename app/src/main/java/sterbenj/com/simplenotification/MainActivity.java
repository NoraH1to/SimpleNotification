package sterbenj.com.simplenotification;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.DatePicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import org.litepal.LitePal;
import org.litepal.tablemanager.callback.DatabaseListener;
import org.w3c.dom.Text;

import java.util.Calendar;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;

public class MainActivity extends AppCompatActivity {

    String TAG = "NoraHito";
    String OLD_TIPS = "OLDT";
    String NEW_TIPS = "NEWT";

    Calendar calendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        initSql();

        super.onCreate(savedInstanceState);

        //开机自启
        if(getIntent().getStringExtra("ACTION_BOOT") != null){
            reFlash();
            return;
        }

        //通知栏的删除操作
        if(getIntent().getStringExtra("ACTION_DELETE") != null){
            deleteTip(getIntent().getIntExtra("channel_id", -1), getIntent().getLongExtra("id", -1));
            return;
        }

        //来自提醒操作
        if(getIntent().getStringExtra("ACTION_NOTICE") != null){
            String title = getIntent().getStringExtra("Notice_title");
            String context = getIntent().getStringExtra("Notice_context");
            final AlertDialog dialog = new AlertDialog.Builder(this).create();
            dialog.setCancelable(false);
            dialog.setTitle(title);
            dialog.setMessage(context);
            dialog.setIcon(R.mipmap.ic_launcher_round);
            dialog.setButton(DialogInterface.BUTTON_POSITIVE, "知道啦", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialog.dismiss();
                    finish();
                }
            });
            dialog.show();
            return;
        }

        setContentView(R.layout.activity_main);

        initNoticeSwitch();
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
        final AppCompatButton fab = (AppCompatButton)findViewById(R.id.fab_newNotice);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //startAnimation(fab);
                newTips();
            }
        });
    }

    //初始化提醒
    //TODO:Switch
    private void initNoticeSwitch(){
        calendar = Calendar.getInstance();
        final MaterialButton confimButton = (MaterialButton)findViewById(R.id.confirm_button);

        //显示时间信息的控件
        final AppCompatTextView dayText = (AppCompatTextView)findViewById(R.id.text_day);
        final AppCompatTextView hourText = (AppCompatTextView)findViewById(R.id.text_hour);

        //开关显示界面相关
        SwitchCompat switchCompat = (SwitchCompat)findViewById(R.id.notice_switch);
        switchCompat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MaterialCardView materialCardView = (MaterialCardView)findViewById(R.id.notice_card);
                SwitchCompat switchCompat1 = (SwitchCompat)view;
                if(switchCompat1.isChecked()){
                    materialCardView.setVisibility(View.VISIBLE);
                }else{
                    materialCardView.setVisibility(View.GONE);
                }
                //开启提醒后必须设置完成才能点确定按钮
                if ((dayText.getText().toString().equals(getString(R.string.unsetting)) ||
                        hourText.getText().toString().equals(getString(R.string.unsetting))) &&
                        switchCompat1.isChecked()){
                    setNormalButtonClickable(confimButton, false);
                }else{
                    setNormalButtonClickable(confimButton, true);
                }
            }
        });

        //设置按钮设置时间相关
        MaterialButton dayButton = (MaterialButton)findViewById(R.id.button_day);
        MaterialButton hourButton = (MaterialButton)findViewById(R.id.button_hour);

        dayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar currentTime = Calendar.getInstance();
                new DatePickerDialog(MainActivity.this, 0, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        calendar.set(Calendar.YEAR, i);
                        calendar.set(Calendar.MONTH, i1);
                        calendar.set(Calendar.DAY_OF_MONTH, i2);
                        dayText.setText("" + calendar.get(Calendar.YEAR) + '年' + (calendar.get(Calendar.MONTH) + 1) + '月' + calendar.get(Calendar.DAY_OF_MONTH) + '日');
                        if (!hourText.getText().toString().equals(getString(R.string.unsetting))){
                            setNormalButtonClickable(confimButton, true);
                        }
                    }
                }, currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),  currentTime.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        hourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar currentTime = Calendar.getInstance();
                new TimePickerDialog(MainActivity.this, 0, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        calendar.set(Calendar.HOUR_OF_DAY, i);
                        calendar.set(Calendar.MINUTE, i1);
                        hourText.setText("" + calendar.get(Calendar.HOUR_OF_DAY) + '点' + calendar.get(Calendar.MINUTE) + '分');
                        if (!dayText.getText().toString().equals(getString(R.string.unsetting))){
                            setNormalButtonClickable(confimButton, true);
                        }
                    }
                }, currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE), false).show();
            }
        });
    }

    //设置一般的开关是否能点击
    private void setNormalButtonClickable(MaterialButton materialButton, boolean bool){
        if(bool){
            materialButton.setTextColor(getResources().getColor(R.color.design_default_color_primary_dark));
            materialButton.setClickable(bool);
        }else {
            materialButton.setTextColor(getResources().getColor(android.R.color.darker_gray));
            materialButton.setClickable(bool);
        }
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
        AppCompatButton fab = (AppCompatButton)findViewById(R.id.fab_newNotice);
        AppCompatButton ConfirmButton =  (AppCompatButton)findViewById(R.id.confirm_button);
        AppCompatButton CancelButton = (AppCompatButton)findViewById(R.id.cancel_button);
        AppCompatButton ReflashButton = (AppCompatButton)findViewById(R.id.reflash_button);

        //来自新建
        if(intent.getStringExtra(NEW_TIPS) != null){
            CancelButton.setVisibility(View.GONE);
            fab.setVisibility(View.GONE);
            ACTION = CREATE;
        }
        //来自已有
        else if (intent.getStringExtra(OLD_TIPS) != null) {
            ACTION = UPDATE;
            ReflashButton.setVisibility(View.GONE);
            String Title = intent.getStringExtra("Title");
            String Context = intent.getStringExtra("Context");
            initText(Title, Context);
            initNoticeLayout(intent.getLongExtra("id", -1));
        }
        //直接打开app
        else{
            CancelButton.setVisibility(View.GONE);
            fab.setVisibility(View.GONE);
            ACTION = CREATE;
        }

        ConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (calendar.getTimeInMillis() < System.currentTimeMillis() &&
                        ((SwitchCompat)findViewById(R.id.notice_switch)).isChecked()){
                    Toast.makeText(MainActivity.this, "提醒时间不能小于当前时间", Toast.LENGTH_SHORT).show();
                    return;
                }
                switch (ACTION){
                    case UPDATE:
                        updateTip(intent.getLongExtra("id", -1));
                        break;
                    case CREATE:
                        createTip(getTipTitle(), getTipContext());
                        break;
                }
            }
        });

        CancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteTip(intent.getIntExtra("channel_id", -1),
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

    //初始化提醒信息
    private void initNoticeLayout(long id){
        Tip temp = LitePal.find(Tip.class, id);
        AppCompatTextView dayText = (AppCompatTextView)findViewById(R.id.text_day);
        AppCompatTextView hourText = (AppCompatTextView)findViewById(R.id.text_hour);
        if (temp.getHasNotice() == 1){
            findViewById(R.id.notice_card).setVisibility(View.VISIBLE);
            ((SwitchCompat)findViewById(R.id.notice_switch)).setChecked(true);
            Calendar tmpCalendar = Calendar.getInstance();
            tmpCalendar.setTimeInMillis(temp.getTargetTime());
            dayText.setText("" +
                    tmpCalendar.get(Calendar.YEAR) + '年' +
                    (tmpCalendar.get(Calendar.MONTH) + 1) + '月' +
                    tmpCalendar.get(Calendar.DAY_OF_MONTH) + '日');
            hourText.setText("" +
                    tmpCalendar.get(Calendar.HOUR_OF_DAY) + '点' +
                    tmpCalendar.get(Calendar.MINUTE) + '分');
        }
    }

    private void initText(String Title, String Context){
        TextInputEditText TitleEdit = (TextInputEditText)findViewById(R.id.title);
        TextInputEditText ContextEdit = (TextInputEditText)findViewById(R.id.context);
        TitleEdit.setText(Title);
        ContextEdit.setText(Context);
    }

    //创建新通知
    private void createTip(String Title, String Context){

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
        if (((SwitchCompat)findViewById(R.id.notice_switch)).isChecked()){
            tip.setHasNotice(1);
            tip.setTargetTime(calendar.getTimeInMillis());
            createOrDeleteDNotice(tip, true);
        }else{
            tip.setHasNotice(2);
        }
        tip.save();

        long sql_id = tip.getId();
        long Time = tip.getTime();

        pushNotification(channel_id, sql_id, Title, Context, Time);

        finish();
    }

    //更新通知
    private void updateTip(long sql_id){
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
        if (((SwitchCompat)findViewById(R.id.notice_switch)).isChecked()){
            tip.setHasNotice(1);
            tip.setTargetTime(calendar.getTimeInMillis());
            createOrDeleteDNotice(tip, true);
        }else{
            tip.setHasNotice(2);
        }
        tip.updateAll("id = ?", String.valueOf(tip.getId()));

        pushNotification(channel_id, sql_id, Title, Context, Time);

        finish();
    }

    //删除通知
    private void deleteTip(int id, long sql_id){
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
        createOrDeleteDNotice(LitePal.find(Tip.class, sql_id), false);
        LitePal.delete(Tip.class, sql_id);
        Log.d("NoraHito", "deleteNotice: " + id + "     " + sql_id);
        finish();
    }

    //创建更新或删除提醒
    private void createOrDeleteDNotice(Tip tip, boolean create){
        AlarmManager alarmManager = (AlarmManager)getSystemService(Service.ALARM_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("ACTION_NOTICE", "ACTION_NOTICE");
        intent.putExtra("Notice_title", tip.getTitle());
        intent.putExtra("Notice_context", tip.getContext());
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                tip.getChannel_id() + 2333, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if(create){
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, tip.getTargetTime(), pendingIntent);
        }else{
            alarmManager.cancel(pendingIntent);
        }
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