package sterbenj.com.simplenotification;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.DatePicker;
import android.widget.NumberPicker;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import org.litepal.LitePal;
import org.litepal.tablemanager.callback.DatabaseListener;

import java.util.Calendar;
import java.util.List;

import androidx.annotation.NonNull;
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

    long day_picker_value = 0;
    long hour_picker_value = 0;
    long min_picker_value = 0;
    int times_picker_value = 1;
    long def_spaceTime = 5*60*1000;

    Calendar calendar;
    CircleTip circleTip;

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
            //息屏显示
            final Window win = getWindow();
            win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

            NoticeDialog(getIntent().getIntExtra("Tip", -1));
            return;
        }

        setContentView(R.layout.activity_main);

        ((TextInputEditText)(findViewById(R.id.title))).requestFocus();

        initNoticeSwitch();
        initButton();
        initFab();


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                }else{
                    Toast.makeText(this, "没有该权限息屏提醒会失效哦！", Toast.LENGTH_SHORT).show();
                }
        }
    }

    //提醒dialog
    private void NoticeDialog(int channel_id){

        //闹钟铃声play
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        final Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
        ringtone.play();

        Log.d(TAG, "NoticeDialog: " + channel_id);
        final Tip tip_temp = LitePal.where("channel_id = ?", String.valueOf(channel_id)).find(Tip.class).get(0);
        String title = tip_temp.getTitle();
        String context = tip_temp.getContext();
        AlertDialog dialog = new AlertDialog.Builder(this).create();
        dialog.setCancelable(false);
        dialog.setTitle(title);
        dialog.setMessage(context);
        dialog.setIcon(R.mipmap.ic_launcher_round);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "知道啦", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                ringtone.stop();
                finish();
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "稍后提醒", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ringtone.stop();
                AfterTimeDialog(tip_temp, dialogInterface);
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "删除提醒", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ringtone.stop();
                deleteTip(tip_temp.getChannel_id(), tip_temp.getId());
                dialogInterface.dismiss();
                finish();
            }
        });
        dialog.show();
    }

    //稍后提醒时间选择
    private void AfterTimeDialog(final Tip tip, final DialogInterface dialogInterfaceOut){
        String[] Times = new String[]{
                "5分钟", "15分钟", "30分钟", "1小时", "2小时", "4小时"
        };
        final long[] TimesMills = new long[]{
                5*60*1000, 15*60*1000, 30*60*1000, 60*60*1000, 120*60*1000, 240*60*1000
        };
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setCancelable(true).setTitle("多长时间后提醒").setSingleChoiceItems(Times, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                createAfterNotice(tip, TimesMills[i]);
                pushNotification(tip);
                dialogInterface.dismiss();
                dialogInterfaceOut.dismiss();
                finish();
            }
        }).create().show();
    }

    private void CircleTimeDialog(final AppCompatTextView circleTimeText, final AppCompatTextView timesText,
                                      final AppCompatTextView hourText, final AppCompatTextView dayText,
                                      final MaterialButton confirmButton){
        AlertDialog dialog= new AlertDialog.Builder(MainActivity.this).create();
        dialog.setTitle("超时后循环提醒时间");
        View view1 = LayoutInflater.from(MainActivity.this)
                .inflate(R.layout.number_picker_circle_time, null);
        //初始化picker
        initDatePicker(view1);
        dialog.setView(view1);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                Calendar tempCal = Calendar.getInstance();

                //三个数据不能都为0
                if (day_picker_value == 0 && hour_picker_value == 0 && min_picker_value == 0){

                    Toast.makeText(MainActivity.this, "间隔为0，设为默认值", Toast.LENGTH_SHORT).show();
                    circleTip.setSpaceTime(def_spaceTime);
                    circleTimeText.setText("5 分");
                    if(!circleTimeText.getText().toString().equals(getString(R.string.unsetting)) &&
                            !timesText.getText().toString().equals(getString(R.string.unsetting)) &&
                            !hourText.getText().toString().equals(getString(R.string.unsetting)) &&
                            !dayText.getText().toString().equals(getString(R.string.unsetting))){
                        setNormalButtonClickable(confirmButton, true);
                    }else{
                        setNormalButtonClickable(confirmButton, false);
                    }
                }
                else{
                    //设置换算spacetime
                    circleTip.setSpaceTime(
                            day_picker_value*1000*60*60*24 +
                            hour_picker_value*1000*60*60 +
                            min_picker_value*1000*60);
                    //处理显示字符串
                    StringBuilder stringBuilder = new StringBuilder();
                    if(day_picker_value != 0){
                        stringBuilder.append(day_picker_value+" 天 ");
                    }
                    if(hour_picker_value != 0){
                        stringBuilder.append(hour_picker_value+" 时 ");
                    }
                    if(min_picker_value != 0){
                        stringBuilder.append(min_picker_value+" 分");
                    }
                    ((AppCompatTextView)findViewById(R.id.text_circleTime)).setText(stringBuilder);
                    if(!circleTimeText.getText().toString().equals(getString(R.string.unsetting)) &&
                            !timesText.getText().toString().equals(getString(R.string.unsetting)) &&
                            !hourText.getText().toString().equals(getString(R.string.unsetting)) &&
                            !dayText.getText().toString().equals(getString(R.string.unsetting))){
                        setNormalButtonClickable(confirmButton, true);
                    }else{
                        setNormalButtonClickable(confirmButton, false);
                    }
                }

                //还原value
                day_picker_value = 0;
                hour_picker_value = 0;
                min_picker_value = 0;
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //还原value
                day_picker_value = 0;
                hour_picker_value = 0;
                min_picker_value = 0;
            }
        });
        dialog.show();
    }

    //循环次数选择
    private void TimesDialog(final AppCompatTextView circleTimeText, final AppCompatTextView timesText,
                                 final AppCompatTextView hourText, final AppCompatTextView dayText,
                                 final MaterialButton confirmButton){
        AlertDialog dialog= new AlertDialog.Builder(MainActivity.this).create();
        dialog.setTitle("超时后循环提醒次数");
        View view1 = LayoutInflater.from(MainActivity.this)
                .inflate(R.layout.number_picker_times, null);
        initTimesPicker(view1);
        dialog.setView(view1);
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                circleTip.setTimes(times_picker_value);
                ((AppCompatTextView)findViewById(R.id.text_times)).setText(times_picker_value+" 次 ");
                if(!circleTimeText.getText().toString().equals(getString(R.string.unsetting)) &&
                        !timesText.getText().toString().equals(getString(R.string.unsetting)) &&
                        !hourText.getText().toString().equals(getString(R.string.unsetting)) &&
                        !dayText.getText().toString().equals(getString(R.string.unsetting))){
                    setNormalButtonClickable(confirmButton, true);
                }else{
                    setNormalButtonClickable(confirmButton, false);
                }

                //还原value
                times_picker_value = 1;
            }
        });
        dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //还原value
                times_picker_value = 0;
            }
        });
        dialog.show();
    }

    //稍后提醒建立
    private void createAfterNotice(Tip tip, long addTime){
        tip.setTargetTime(tip.getTargetTime() + addTime);
        tip.save();
        createOrDeleteDNotice(tip, true);
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
    private void pushNotification(Tip dataTip){
        int channel_id = dataTip.getChannel_id();
        long sql_id = dataTip.getId();
        String Title = dataTip.getTitle();
        String Context = dataTip.getContext();
        long Time = dataTip.getTime();
        long TargetTime;
        try{
            TargetTime = dataTip.getTargetTime();
        }catch (Exception e){
            TargetTime = 0;
        }
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
        builder.setVisibility(Notification.VISIBILITY_PUBLIC);
        if(TargetTime == 0 || TargetTime <= System.currentTimeMillis()){
            builder.setWhen(Time);
            builder.setUsesChronometer(false);
        }else{
            builder.setWhen(TargetTime);
            builder.setUsesChronometer(true);
        }

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
            pushNotification(tip);
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
    private void initNoticeSwitch(){
        calendar = Calendar.getInstance();
        final MaterialButton confimButton = (MaterialButton)findViewById(R.id.confirm_button);

        //显示时间信息的控件
        final AppCompatTextView dayText = (AppCompatTextView)findViewById(R.id.text_day);
        final AppCompatTextView hourText = (AppCompatTextView)findViewById(R.id.text_hour);
        final AppCompatTextView circleTimeText = (AppCompatTextView)findViewById(R.id.text_circleTime);
        final AppCompatTextView timesText = (AppCompatTextView)findViewById(R.id.text_times);

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
                if (((dayText.getText().toString().equals(getString(R.string.unsetting)) ||
                        hourText.getText().toString().equals(getString(R.string.unsetting))) ||
                        circleTimeText.getText().toString().equals(getString(R.string.unsetting)) ^
                        timesText.getText().toString().equals(getString(R.string.unsetting))) &&
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
        MaterialButton circleTimeButton = (MaterialButton)findViewById(R.id.button_circleTime);
        MaterialButton timesButton = (MaterialButton)findViewById(R.id.button_times);

        dayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar currentTime = Calendar.getInstance();
                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this, 0, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int i, int i1, int i2) {
                        calendar.set(Calendar.YEAR, i);
                        calendar.set(Calendar.MONTH, i1);
                        calendar.set(Calendar.DAY_OF_MONTH, i2);
                        dayText.setText("" + calendar.get(Calendar.YEAR) + '年' + (calendar.get(Calendar.MONTH) + 1) + '月' + calendar.get(Calendar.DAY_OF_MONTH) + '日');
                        if (!hourText.getText().toString().equals(getString(R.string.unsetting))){
                            if(!circleTimeText.getText().equals(getString(R.string.unsetting)) &&
                                    !timesText.getText().equals(getString(R.string.unsetting))){
                                setNormalButtonClickable(confimButton, true);
                            }
                            else if(circleTimeText.getText().equals(getString(R.string.unsetting)) &&
                                    timesText.getText().equals(getString(R.string.unsetting))){
                                setNormalButtonClickable(confimButton, true);
                            }
                            else{
                                setNormalButtonClickable(confimButton, false);
                            }
                        }
                    }
                }, currentTime.get(Calendar.YEAR), currentTime.get(Calendar.MONTH),  currentTime.get(Calendar.DAY_OF_MONTH));
                //设置最小日期
                datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
                //不设置会显示双行标题
                datePickerDialog.setTitle(null);
                datePickerDialog.show();
            }
        });

        hourButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar currentTime = Calendar.getInstance();
                TimePickerDialog timePickerDialog = new TimePickerDialog(MainActivity.this, 0, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker timePicker, int i, int i1) {
                        calendar.set(Calendar.HOUR_OF_DAY, i);
                        calendar.set(Calendar.MINUTE, i1);
                        hourText.setText("" + calendar.get(Calendar.HOUR_OF_DAY) + '点' + calendar.get(Calendar.MINUTE) + '分');
                        if (!dayText.getText().toString().equals(getString(R.string.unsetting))){
                            if(!circleTimeText.getText().equals(getString(R.string.unsetting)) &&
                                    !timesText.getText().equals(getString(R.string.unsetting))){
                                setNormalButtonClickable(confimButton, true);
                            }
                            else if(circleTimeText.getText().equals(getString(R.string.unsetting)) &&
                                    timesText.getText().equals(getString(R.string.unsetting))){
                                setNormalButtonClickable(confimButton, true);
                            }
                            else{
                                setNormalButtonClickable(confimButton, false);
                            }
                        }
                    }
                }, currentTime.get(Calendar.HOUR_OF_DAY), currentTime.get(Calendar.MINUTE), true);
                timePickerDialog.show();
            }
        });

        circleTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CircleTimeDialog(circleTimeText, timesText, dayText, hourText, confimButton);
            }
        });

        timesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TimesDialog(circleTimeText, timesText, dayText, hourText, confimButton);
            }
        });
    }

    //初始化次数picker
    private void initTimesPicker(View view){
        NumberPicker timesPicker = view.findViewById(R.id.times_picker);
        timesPicker.setMinValue(1);
        timesPicker.setMaxValue(30);
        timesPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                times_picker_value = i1;
            }
        });
    }

    //初始化日期picker
    private void initDatePicker(View view){
        NumberPicker dayPicker = view.findViewById(R.id.circleTime_picker_day);
        NumberPicker hourPicker = view.findViewById(R.id.circleTime_picker_hour);
        NumberPicker minPicker = view.findViewById(R.id.circleTime_picker_minute);

        dayPicker.setMinValue(0);
        hourPicker.setMinValue(0);
        minPicker.setMinValue(0);

        dayPicker.setMaxValue(30);
        hourPicker.setMaxValue(23);
        minPicker.setMaxValue(59);

        dayPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                day_picker_value = i1;
            }
        });
        hourPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                hour_picker_value = i1;
            }
        });
        minPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                min_picker_value = i1;
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
    //TODO: Animation
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
            circleTip = new CircleTip();
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
        //来自分享的内容
        else if(Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null){
            ACTION = CREATE;
            if("text/plain".equals(getIntent().getType())){
                initText(intent.getStringExtra(Intent.EXTRA_TITLE), intent.getStringExtra(Intent.EXTRA_TEXT));
                circleTip = new CircleTip();
            }
            else{
                Toast.makeText(this, "不支持的分享类型", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        //直接打开app
        else{
            CancelButton.setVisibility(View.GONE);
            fab.setVisibility(View.GONE);
            circleTip = new CircleTip();
            ACTION = CREATE;
        }

        ConfirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick: " + calendar.getTimeInMillis());
                Log.d(TAG, "onClick: " + System.currentTimeMillis());
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
        AppCompatTextView circleTimeText = (AppCompatTextView)findViewById(R.id.text_circleTime);
        AppCompatTextView timesText = (AppCompatTextView)findViewById(R.id.text_times);
        try{
            circleTip = LitePal.where("TipId = ?", String.valueOf(id)).find(CircleTip.class).get(0);
        }catch (Exception e){
            circleTip = new CircleTip();
        }
        if (temp.getHasNotice() == 1){
            findViewById(R.id.notice_card).setVisibility(View.VISIBLE);
            ((SwitchCompat)findViewById(R.id.notice_switch)).setChecked(true);
            //换算年月日显示
            calendar.setTimeInMillis(temp.getTargetTime());
            dayText.setText("" +
                    calendar.get(Calendar.YEAR) + " 年 " +
                    (calendar.get(Calendar.MONTH) + 1) + " 月 " +
                    calendar.get(Calendar.DAY_OF_MONTH) + " 日 ");
            hourText.setText("" +
                    calendar.get(Calendar.HOUR_OF_DAY) + " 点 " +
                    calendar.get(Calendar.MINUTE) + " 分");
            //换算间隔时间并显示
            StringBuilder stringBuilder = new StringBuilder();
            long temp_spaceTime = circleTip.getSpaceTime();
            if (temp_spaceTime/1000/60/60/24 >= 1){
                stringBuilder.append(temp_spaceTime/1000/60/60/24 + " 天 ");
                temp_spaceTime -= (temp_spaceTime/1000/60/60/24)*(1000*60*60*24);
                Log.d(TAG, "initNoticeLayout: " + temp_spaceTime);
            }
            if (temp_spaceTime/1000/60/60 >= 1){
                stringBuilder.append(temp_spaceTime/1000/60/60 + " 时 ");
                temp_spaceTime -= (temp_spaceTime/1000/60/60)*(1000*60*60);
                Log.d(TAG, "initNoticeLayout: " + temp_spaceTime);
            }
            if (temp_spaceTime/1000/60 >= 1){
                stringBuilder.append(temp_spaceTime/1000/60 + " 分 ");
                temp_spaceTime -= (temp_spaceTime/1000/60)*(1000*60);
                Log.d(TAG, "initNoticeLayout: " + temp_spaceTime);
            }
            circleTimeText.setText(stringBuilder);
            //次数
            timesText.setText(circleTip.getTimes() + " 次 ");
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
            tip.save();
            createOrDeleteDNotice(tip, true);
            createOrDeleteCircleNotice(tip, true);
        }else{
            tip.setHasNotice(2);
            createOrDeleteDNotice(tip, false);
            tip.save();
            createOrDeleteCircleNotice(tip, false);
        }
        pushNotification(tip);

        finish();
    }

    //更新通知
    private void updateTip(long sql_id){
        List<Tip> tips = LitePal.where("id = ?", String.valueOf(sql_id)).find(Tip.class);
        Tip tip = tips.get(0);

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
            createOrDeleteCircleNotice(tip, true);
        }else{
            tip.setHasNotice(2);
            createOrDeleteDNotice(tip, false);
            createOrDeleteCircleNotice(tip, false);
        }
        tip.updateAll("id = ?", String.valueOf(tip.getId()));

        pushNotification(tip);

        finish();
    }

    //删除通知
    private void deleteTip(int id, long sql_id){
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
        createOrDeleteDNotice(LitePal.find(Tip.class, sql_id), false);
        createOrDeleteCircleNotice(LitePal.find(Tip.class, sql_id), false);
        LitePal.delete(Tip.class, sql_id);
        Log.d("NoraHito", "deleteNotice: " + id + "     " + sql_id);
        finish();
    }

    //创建更新或删除提醒
    private void createOrDeleteDNotice(Tip tip, boolean create){
        AlarmManager alarmManager = (AlarmManager)getSystemService(Service.ALARM_SERVICE);
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("ACTION_NOTICE", "ACTION_NOTICE");
        intent.putExtra("Tip", tip.getChannel_id());
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                tip.getChannel_id() + 2333, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if(create){
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, tip.getTargetTime(), pendingIntent);
        }else{
            alarmManager.cancel(pendingIntent);
        }
    }

    //循环提醒数据存储与设置
    private void createOrDeleteCircleNotice(Tip tip, boolean create){
        if (create){
            Tip temp = LitePal.where("channel_id = ?", String.valueOf(tip.getChannel_id())).find(Tip.class).get(0);
            circleTip.setTipId(temp.getId());
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 1; i <= circleTip.getTimes(); i++){
                stringBuilder.append(tip.getChannel_id()*100+i + ":");
                AlarmManager alarmManager = (AlarmManager)getSystemService(Service.ALARM_SERVICE);
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("ACTION_NOTICE", "ACTION_NOTICE");
                intent.putExtra("Tip", tip.getChannel_id());
                PendingIntent pendingIntent = PendingIntent.getActivity(this,
                        tip.getChannel_id()*100+i, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, tip.getTargetTime()+i*circleTip.getSpaceTime(), pendingIntent);
            }
            circleTip.setRequests(stringBuilder.toString());
            circleTip.save();
        }else{
            try{
                circleTip = LitePal.where("TipId = ?", String.valueOf(tip.getId())).find(CircleTip.class).get(0);
                for (int i = 1; i <= circleTip.getTimes(); i++){
                    AlarmManager alarmManager = (AlarmManager)getSystemService(Service.ALARM_SERVICE);
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.putExtra("ACTION_NOTICE", "ACTION_NOTICE");
                    intent.putExtra("Tip", tip.getChannel_id());
                    PendingIntent pendingIntent = PendingIntent.getActivity(this,
                            tip.getChannel_id()*100+i, intent, PendingIntent.FLAG_UPDATE_CURRENT);
                    alarmManager.cancel(pendingIntent);
                }
                circleTip.delete();
            }catch (Exception e){
                Log.d(TAG, "createOrDeleteCircleNotice: " + e);
            }
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