package kro.kr.rhya_network.aninotice.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import kro.kr.rhya_network.aninotice.R;

public class StartActivity extends AppCompatActivity {
    // 인터넷 연결 확인 결과 변수
    public static final String WIFE_STATE = "WIFE";
    public static final String MOBILE_STATE = "MOBILE";
    public static final String NONE_STATE = "NONE";
    // 인터넷 연결 확인 함수
    public static String getWhatKindOfNetwork(Context context){
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return WIFE_STATE;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return MOBILE_STATE;
            }
        }
        return NONE_STATE;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        final ImageView img = (ImageView) findViewById(R.id.appLogo);

        Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.alpha_anim_0_1_v1);

        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // 애니메이션 종료 이벤트
                String getNetwork =  getWhatKindOfNetwork(getApplication());
                if(getNetwork.equals("NONE")) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
                    builder.setTitle("인터넷 연결 안 됨");
                    builder.setIcon(R.mipmap.ic_launcher);
                    builder.setMessage("Ani 알리미 작업을 위해서는 인터넷 연결이 필요합니다. 인터넷 연결을 확인 후 다시 실행해 주세요.");
                    builder.setCancelable(false);
                    builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
                    builder.create().show();
                }else {
                    // 파일 권한
                    checkPermission();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        img.startAnimation(anim);
    }

    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }else {
            Intent intent = new Intent(getApplicationContext(), AniActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.next_anim_a_1, R.anim.next_anim_a_2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(getApplicationContext(), AniActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.next_anim_a_1, R.anim.next_anim_a_2);
                } else {
                    // 권한 거절된 경우 처리
                    AlertDialog.Builder builder = new AlertDialog.Builder(StartActivity.this);
                    builder.setTitle("권한 거부됨");
                    builder.setIcon(R.mipmap.ic_launcher);
                    builder.setMessage("저장소 권한이 거부되었습니다. Ani 알리미를 사용하기 위해서는 해당 권한이 필요합니다.");
                    builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            finish();
                        };
                    });
                    builder.setCancelable(false);
                    builder.create().show();
                }
                break;
        }
    }
}
