package kro.kr.rhya_network.aninotice.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import kro.kr.rhya_network.aninotice.R;
import kro.kr.rhya_network.aninotice.service.RealService;
import kro.kr.rhya_network.aninotice.utils.PreferenceManager;

public class AniActivity extends AppCompatActivity {
    // Loading object
    private ImageView imgView;
    private ProgressBar pBar;
    private TextView textView;
    // Web View
    private WebView webView;
    private WebSettings webSettings;
    // Foreground 서비스
    private Intent serviceIntent;
    // 나가기 버튼 2번 시간
    private long backKeyPressedTime = 0;
    // 알림 SharePreference 키 이름
    public static final String NOTICE_TOF_KEY_NAME = "NoticeTOF";
    // 애니 웹 페이지 URL
    public static final String ANI_PAGE_URL = "https://linkkf.app/";
    // 애니메이션 동작 여부
    public boolean isAnimStart = false;
    // 알림 설정 방법 메시지 출력 여부 키 이름
    private final String HELP_NOTICE_ON_OFF = "HelpNoticeTOF";


    /**
     * 키 입력 처리
     * @param keyCode 키 번호
     * @param event 이벤트
     * @return true / false
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 키 입력 감지
        switch (keyCode) {
            // 볼륨 -
            // -----------------------------------
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                // 처음 실행 확인 메시지 출력 여부
                if (PreferenceManager.getBoolean(RealService.mContext, HELP_NOTICE_ON_OFF)) {
                    // 메시지 출력 여부 변경
                    PreferenceManager.setBoolean(RealService.mContext, HELP_NOTICE_ON_OFF, false);
                    // 메시지 출력
                    ShowHelpMessage();
                }

                // SharePreference 값 확인
                if (PreferenceManager.getBoolean(RealService.mContext, NOTICE_TOF_KEY_NAME)) {
                    // SharePreference 값 변경 --> false [ 알림 중지 ]
                    PreferenceManager.setBoolean(RealService.mContext, NOTICE_TOF_KEY_NAME, false);
                    // Toast 메시지 생성
                    Toast.makeText(this, "알리미 중지 중...", Toast.LENGTH_SHORT).show();
                }

                break;

            // 볼륨 +
            // -----------------------------------
            case KeyEvent.KEYCODE_VOLUME_UP:
                // 처음 실행 확인 메시지 출력 여부
                if (PreferenceManager.getBoolean(RealService.mContext, HELP_NOTICE_ON_OFF)) {
                    // 메시지 출력 여부 변경
                    PreferenceManager.setBoolean(RealService.mContext, HELP_NOTICE_ON_OFF, false);
                    // 메시지 출력
                    ShowHelpMessage();
                }

                // SharePreference 값 확인
                if (!PreferenceManager.getBoolean(RealService.mContext, NOTICE_TOF_KEY_NAME)) {
                    // SharePreference 값 변경 --> true [ 알림 시작 ]
                    PreferenceManager.setBoolean(RealService.mContext, NOTICE_TOF_KEY_NAME, true);
                    // Toast 메시지 생성
                    Toast.makeText(this, "알리미 시작 중...", Toast.LENGTH_SHORT).show();
                }
                break;

            // 나가기 버튼
            // -----------------------------------
            case KeyEvent.KEYCODE_BACK:
                // 2번 클릭 감지 메시지
                if (System.currentTimeMillis() > backKeyPressedTime + 2000) {
                    backKeyPressedTime = System.currentTimeMillis();
                    // Toast 메시지 생성
                    Toast.makeText(this, "\'뒤로\' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show();
                }else {
                    // 2초 이내에 뒤로가기 버튼을 한번 더 클릭시 앱 종료
                    if (System.currentTimeMillis() <= backKeyPressedTime + 2000) {
                        // 알림 종료 유무
                        if (!PreferenceManager.getBoolean(RealService.mContext, NOTICE_TOF_KEY_NAME)) {
                            // 완전 종료
                            moveTaskToBack(true);
                            finishAndRemoveTask();
                            android.os.Process.killProcess(android.os.Process.myPid());
                        } else {
                            // 백그라운드로 전환
                            moveTaskToBack(true);
                        }
                    }
                }
                break;
        }

        // 키 작동 여부 반환
        return false;
    }


    /**
     * 화면 생성 후 작업
     * @param savedInstanceState
     */
    @Override
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ani);


        // 절전 모드 해제
        // -----------------------------------
        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(POWER_SERVICE);
        boolean isWhiteListing = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            isWhiteListing = pm.isIgnoringBatteryOptimizations(getApplicationContext().getPackageName());
        }
        if (!isWhiteListing) {
            Intent intent = new Intent();
            intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
            startActivity(intent);
        }


        // 로딩 화면 변수 초기화
        imgView = (ImageView) findViewById(R.id.dontShowWebViewImage);
        pBar = (ProgressBar) findViewById(R.id.progressBar);
        textView = (TextView) findViewById(R.id.textView);

        // 애니메이션 생성
        final Animation anim_off = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.alpha_anim_1_0_v1);
        // 애니메이션 이벤트 설정
        anim_off.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                imgView.setVisibility(View.GONE);
                pBar.setVisibility(View.GONE);
                textView.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });


        // WebView 설정
        // -----------------------------------
        webView = (WebView) findViewById(R.id.webView);
        webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webView.loadUrl(ANI_PAGE_URL);
        // 로딩 진행도 이벤트
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);

                // 프로그래스바 업데이트
                pBar.setProgress(newProgress);
                textView.setText(String.valueOf(newProgress).concat("%"));


                if (newProgress == 100 && !isAnimStart) {
                    // 변수 변경
                    isAnimStart = true;
                    // 지연 실행
                    Handler mHandler = new Handler(); mHandler.postDelayed(new Runnable() {
                        public void run() {
                            // 애니메이션 실행
                            imgView.startAnimation(anim_off);
                            pBar.startAnimation(anim_off);
                            textView.startAnimation(anim_off);
                        }
                    }, 1000);
                }
            }
        });


        // Foreground 서비스 실행
        // -----------------------------------
        if (RealService.serviceIntent == null) {
            serviceIntent = new Intent(this, RealService.class);
            startService(serviceIntent);
        } else {
            serviceIntent = RealService.serviceIntent;
        }

        // 지연 실행
        Handler mHandler = new Handler(); mHandler.postDelayed(new Runnable() {
            public void run() {
                webView.loadUrl(ANI_PAGE_URL);
            }
        }, 600);


    }


    /**
     * 알림 설정 도움말 출력 함수
     */
    private void ShowHelpMessage() {
        final String title = "사용 도움말";
        final String message = "Ani 알림이의 알림 설정/해제 방식은 메인 화면에서 볼륨 Up/Down 버튼을 통해 조정 할 수 있습니다. Up 버튼: 알림 시작, Down 버튼: 알림 중지 (알림 시작은 앱 재부팅이 필요하고, 알림 중지는 앱이 종료됨과 동시에 중지됩니다), 해당 도움말은 이후 표시되지 않습니다.";
        final String btnText = "확인";


        AlertDialog.Builder builder = new AlertDialog.Builder(AniActivity.this);
        builder.setTitle(title);
        builder.setIcon(R.mipmap.ic_launcher);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setPositiveButton(btnText, null);
        builder.create().show();
    }


    /**
     * onDestroy 이벤트 처리 함수
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serviceIntent!=null) {
            stopService(serviceIntent);
            serviceIntent = null;
        }
    }
}
