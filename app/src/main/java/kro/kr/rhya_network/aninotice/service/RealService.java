package kro.kr.rhya_network.aninotice.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;

import java.net.URL;

import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import kro.kr.rhya_network.aninotice.R;
import kro.kr.rhya_network.aninotice.activity.AniActivity;
import kro.kr.rhya_network.aninotice.utils.MD5;
import kro.kr.rhya_network.aninotice.utils.PreferenceManager;
import kro.kr.rhya_network.aninotice.activity.StartActivity;

public class RealService extends Service {
    // Thread 변수 생성
    private Thread mainThread;
    // 날자 형식 변환 Format 변수
    private final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    // MainService context 전역 변수
    public static Context mContext;
    // 서비스 intent 변수
    public static Intent serviceIntent = null;
    // 함수 결과 반환
    public ArrayList<String> function_result;
    // 파일 저장 날자 데이터 저장 키 이름
    private final String FILE_SAVE_DATE_KEY_NAME = "FILE_SAVE_DATE";
    // 가져올 HTML 속성
    private final String HTML_DIV_CLASS = "class=\"item1\"";
    // 공백 치환 문자열
    private final String REP_STR_1 = " ";
    private final String REP_STR_2 = "";
    // 파일 생성 데이터
    private final String FILE_DIR_NAME = "/AniNotice";
    private final String FILE_DIR_FULL_NAME = Environment.getExternalStorageDirectory().getAbsolutePath() + FILE_DIR_NAME;
    private final String FILE_NAME = "/AniList.list";
    // 알림 데이터
    private final String ANI_NOTICE_CHANNELID = "fcm_default_channel";
    private final String ANI_NOTICE_CHANNELNAME = "Channel human readable title";
    private final String ANI_NOTICE_TITLE = "애니 업로드!";
    // Split 데이터
    private final String FILE_SPLIT_TEXT = "<#SPLIT#>";
    private final String FILE_SPLIT_NO_TEXT = "<#SPLIT_NO_TEXT#>";


    /**
     * 생성자
     */
    public RealService() {
        mContext = this;
    }


    /**
     * 작동 중 알림 생성 함수
     */
    private void startForegroundService() {
        // 알림 생성 변수
        final String channelID_eng = "default";
        final String channelID_kor = "기본채널";
        final String notice_title = "Ani 알리미";
        final String notice_message = "알리미 작동 중...";

        // 알림 생성
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelID_eng);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle(notice_title);
        builder.setContentText(notice_message);
        Intent notificationIntent = new Intent(this, AniActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        builder.setContentIntent(pendingIntent);


        // 오레오 버전 이상 노티피케이션 알림 설정
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(new NotificationChannel(channelID_eng, channelID_kor, NotificationManager.IMPORTANCE_DEFAULT));
        }

        // Foreground 실행
        startForeground(3, builder.build());
    }


    /**
     * 메인 Thread 작업 함수
     * @param intent
     * @param flags
     * @param startId
     * @return
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Thread 생성 및 실행
        serviceIntent = intent;
        mainThread = new Thread(new Runnable() {
            @Override
            public void run() {
                // 변수 선언
                boolean run = true; // while 변수
                String fileSaveDate; // 파일 저장 날자
                String nowDate; // 현재 날자
                Source htmlSource; // HTML 파싱 변수
                function_result = new ArrayList<>(); // 함수 결과 반환

                // 알림 실행 여부 확인
                if (PreferenceManager.getBoolean(RealService.mContext, AniActivity.NOTICE_TOF_KEY_NAME)) {
                    // 작동 알림 표시
                    startForegroundService();
                    // 무한 반복문 진입
                    while (run) {
                        // 예외 처리
                        try {
                            // 알림 실행 여부 확인
                            if (!PreferenceManager.getBoolean(RealService.mContext, AniActivity.NOTICE_TOF_KEY_NAME)) {
                                // while 탈출
                                run = false;
                                // while 종료
                                break;
                            }


                            // 인터넷 연결 확인
                            if (StartActivity.getWhatKindOfNetwork(getApplication()).equals(StartActivity.NONE_STATE)) {
                                // 5초 대기
                                Thread.sleep(5000);
                                // while 1번 종료
                                continue;
                            }


                            // 변수 초기화
                            ArrayList<Element> getHTMLData; // HTML 파싱 데이터
                            ArrayList<String> getHTMLAni = new ArrayList<String>(); // HTML 파싱 애니 데이터
                            ArrayList<String> addAnilist = new ArrayList<String>(); // 알림 출력 애니 리스트


                            // 현재 날자 가져오기
                            Date dt = new Date();
                            nowDate = format.format(dt);
                            // 파일 저장 날자 가져오기
                            fileSaveDate = PreferenceManager.getString(mContext, FILE_SAVE_DATE_KEY_NAME);

                            // HTML 파싱
                            htmlSource = new Source(new URL(AniActivity.ANI_PAGE_URL));
                            // HTML 파싱 데이터 변환
                            getHTMLData = (ArrayList<Element>) htmlSource.getAllElements(HTMLElementName.DIV);
                            // HTML 파싱 데이터 필요한 테그만 추출
                            for (int i = 0 ; i< getHTMLData.size(); i ++) {
                                // 특정 DIV 클레스만 가져오기
                                if (getHTMLData.get(i).getAttributes().toString().trim().equals(HTML_DIV_CLASS)) {
                                    // 애니 리스트 추가
                                    getHTMLAni.add(getHTMLData.get(i).getTextExtractor().toString());
                                }
                            }

                            // 앱 처음 실행 감지
                            if (fileSaveDate.equals(PreferenceManager.DEFAULT_VALUE_STRING)) {
                                // SharePreference 데이터 설정
                                PreferenceManager.setString(mContext, FILE_SAVE_DATE_KEY_NAME, nowDate);
                                // 파일 생성
                                saveFile(getHTMLAni);
                            }

                            // 애니 파일 리스트 읽기
                            loadFile();

                            // 애니 파일 리스트, HTML 파싱 애니 리스트 비교
                            for (int i = 0; i < getHTMLAni.size(); i++) {
                                // 애니 리스트 존재 유무 확인
                                if (!function_result.contains(MD5.getMD5(getHTMLAni.get(i)))) {
                                    // 공백 확인
                                    if (getHTMLAni.get(i).replace(REP_STR_1, REP_STR_2).length() > 0) {
                                        // 애니 리스트 추가
                                        addAnilist.add(getHTMLAni.get(i));
                                        // 알림 생성
                                        sendNotification(getHTMLAni.get(i));
                                        // 0.5초 대기
                                        Thread.sleep(500);
                                    }
                                }
                            }

                            // 애니 리스트 추가
                            for (int i = 0; i < getHTMLAni.size(); i++) {
                                addAnilist.add(getHTMLAni.get(i));
                            }

                            // 파일 저장 날자 확인
                            if (!nowDate.equals(fileSaveDate)) {
                                // 날자 변경
                                PreferenceManager.setString(mContext, FILE_SAVE_DATE_KEY_NAME, nowDate);
                                // 파일 저장
                                saveFile(getHTMLAni);
                            }else {
                                // 파일 저장
                                saveFile(addAnilist);
                            }

                            // 2번 반복
                            for (int i = 0; i < 2; i ++) {
                                // 중지 확인
                                if (!PreferenceManager.getBoolean(RealService.mContext, AniActivity.NOTICE_TOF_KEY_NAME)) {
                                    // while 탈출
                                    run = false;
                                    // for 종료
                                    break;
                                }

                                // 1초 대기
                                Thread.sleep(1000);
                            }

                            // 변수 Null 할당
                            getHTMLData = null;
                            htmlSource = null;
                            getHTMLAni = null;
                            addAnilist = null;
                            dt = null;
                            // GC 호출
                            System.gc();
                        } catch (InterruptedException ex) {
                            // while 중지
                            run = false;
                        } catch (IOException e) {
                            // 예외 처리 없음
                        }
                    }
                }

                // 종료
                return;
            }
        });
        mainThread.start();

        return START_NOT_STICKY;
    }



    // ------------------------------------------------------------------------------- //
    // onDestroy
    @Override
    public void onDestroy() {
        super.onDestroy();

        setAlarmTimer();
        Thread.currentThread().interrupt();

        if (mainThread != null) {
            mainThread.interrupt();
            mainThread = null;
        }
    }

    // onCreate
    @Override
    public void onCreate() {
        super.onCreate();
    }

    // onBind
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // onUnbind
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }
    // ------------------------------------------------------------------------------- //



    /**
     * 파일 쓰기 함수
     *
     * @param inputs 작성 내용
     */
    private void saveFile(ArrayList<String> inputs) {
        // 변수 초기화
        File saveFile = null;

        // 파일 경로 생성
        if( Build.VERSION.SDK_INT < 29) {
            saveFile = new File(FILE_DIR_FULL_NAME);
        } else {
            saveFile = RealService.this.getExternalFilesDir(FILE_DIR_NAME);
        }

        // 폴더 생성
        if(!saveFile.exists())
            saveFile.mkdir();

        // 파일 쓰기 변수 초기화
        BufferedWriter buf = null;
        FileWriter fw = null;

        // 예외 처리
        try {
            // 파일 읽기
            StringBuilder pathMaker = new StringBuilder();
            pathMaker.append(saveFile);
            pathMaker.append(FILE_NAME);
            fw = new FileWriter(pathMaker.toString(), false);
            pathMaker = null;
            buf = new BufferedWriter(fw);
            for(int i=0;i< inputs.size();i++) {
                buf.append(MD5.getMD5(inputs.get(i)));
                buf.append(System.lineSeparator());
            }
            buf.close();
            fw.close();
            inputs = null;
        } catch (IOException e) {
            // null 대입
            saveFile = null;
            buf = null;
            fw = null;
            inputs = null;
        }
    }


    /**
     * 파일 로딩 함수
     *
     * @return 애니 리스트
     */
    private void loadFile() {
        // 변수 초기화
        String readLine = null;
        File saveFile = null;
        function_result.clear();

        // 파일 경로 생성
        if( Build.VERSION.SDK_INT < 29) {
            saveFile = new File(FILE_DIR_FULL_NAME);
        } else {
            saveFile = RealService.this.getExternalFilesDir(FILE_DIR_NAME);
        }

        // 폴더 생성
        if(saveFile == null) {
            assert saveFile != null;
            saveFile.mkdir();
        }

        // 파일 읽기 변수 초기화
        BufferedReader buf = null;
        FileReader fr = null;

        // 예외 처리
        try {
            // 파일 읽기
            StringBuilder pathMaker = new StringBuilder();
            pathMaker.append(saveFile);
            pathMaker.append(FILE_NAME);
            fr = new FileReader(pathMaker.toString());
            pathMaker = null;
            buf = new BufferedReader(fr);

            while ((readLine = buf.readLine()) != null) {
                function_result.add(readLine);
            }

            buf.close();
            fr.close();
        } catch (FileNotFoundException e) {
            // null 대입
            buf = null;
            fr = null;
        } catch (IOException e) {
            // null 대입
            buf = null;
            fr = null;
        }
    }


    /**
     * 알림 타이머 설정 함수
     */
    protected void setAlarmTimer() {
        final Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.SECOND, 1);
        Intent intent = new Intent(this, AlarmRecever.class);
        PendingIntent sender = PendingIntent.getBroadcast(this, 0,intent,0);

        AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), sender);
    }


    /**
     * 알림 생성 함수
     * @param messageBody 메시지
     */
    private void sendNotification(String messageBody) {
        Intent intent = new Intent(this, AniActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int)(System.currentTimeMillis()/1000), intent, PendingIntent.FLAG_ONE_SHOT);

        String channelId = ANI_NOTICE_CHANNELID;
        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(ANI_NOTICE_TITLE)
                        .setContentText(messageBody)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setPriority(Notification.PRIORITY_HIGH)
                        .setContentIntent(pendingIntent);


        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId,ANI_NOTICE_CHANNELNAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify((int)(System.currentTimeMillis()/1000), notificationBuilder.build());
    }
}
