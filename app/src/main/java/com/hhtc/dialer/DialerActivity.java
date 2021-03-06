package com.hhtc.dialer;

import android.Manifest;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.hhtc.dialer.add.ContactAddOrEditActivity;
import com.hhtc.dialer.animation.DialerActionButtonAnimation;
import com.hhtc.dialer.call.service.TelephoneService;
import com.hhtc.dialer.data.bean.UserInfo;
import com.hhtc.dialer.data.tradition.TraditionSynchronise;
import com.hhtc.dialer.main.DialerFragment;
import com.hhtc.dialer.main.DialerTabPagerAdapter;
import com.hhtc.dialer.main.FloatingViewModel;
import com.hhtc.dialer.main.TabSelectedListenerImpl;
import com.hhtc.dialer.main.collects.CollectsFragment;
import com.hhtc.dialer.main.contacts.ContactsFragment;
import com.hhtc.dialer.main.recent.RecentFragment;
import com.hhtc.dialer.permission.PermissionsUtil;
import com.hhtc.dialer.utils.IntentProvider;
import com.hhtc.dialer.utils.intentUnits;
import com.hhtc.dialer.view.TableViewPager;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 新版本dialer
 */
public class DialerActivity extends AppCompatActivity {

    public static final int REQUEST_READ_CONTACTS = 1;

    public static Uri URI_USER = new Uri.Builder()
            .scheme("content")
            .authority("com.hhtc.dialer.user.provider")
            .query("user")
            .build();

    private TextView title_tips;
    private ImageButton add_contacts;
    private TabLayout page_navigation;
    private TableViewPager content_pager;
    private FloatingActionButton action_button;
    private DialerTabPagerAdapter adapter;
    private String[] stringTips;
    private FloatingViewModel mSharedViewModel;
    private TextView localName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialer);
        initView();
        setData();
    }


    private void initView() {
        title_tips = findViewById(R.id.title_tips);
        add_contacts = findViewById(R.id.add_contacts);
        page_navigation = findViewById(R.id.page_navigation);
        content_pager = findViewById(R.id.content_pager);
        action_button = findViewById(R.id.action_button);
        localName = findViewById(R.id.localName);
        ActivityCompat.requestPermissions(this, PermissionsUtil.allPhoneGroupPermissionsUsedInDialer.toArray(new String[PermissionsUtil.allPhoneGroupPermissionsUsedInDialer.size()]),
                REQUEST_READ_CONTACTS);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //开始同步电话数据
        if (ActivityCompat.checkSelfPermission(DialerActivity.this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            TraditionSynchronise.getInstance().startSynchronization(getApplicationContext());
            mSharedViewModel.getNotify().observe(this, aVoid -> mSharedViewModel.getUserInfo().observe(DialerActivity.this, DialerActivity.this::onChanged));
            mSharedViewModel.loadUserInfo();
        }
    }

    private void setData() {
        adapter = new DialerTabPagerAdapter(getSupportFragmentManager(), createPager());
        content_pager.setAdapter(adapter);
        page_navigation.setupWithViewPager(content_pager);
        content_pager.setCurrentItem(1);
        Objects.requireNonNull(page_navigation.getTabAt(0)).setIcon(R.drawable.dialer_main_collect_icon_selector);
        Objects.requireNonNull(page_navigation.getTabAt(1)).setIcon(R.drawable.dialer_main_recent_call_icon_selector);
        Objects.requireNonNull(page_navigation.getTabAt(2)).setIcon(R.drawable.dialer_main_contacts_icon_selector);

        page_navigation.addOnTabSelectedListener(tabSelectedListener);
        action_button.setOnClickListener(this::actionButton);
        stringTips = getResources().getStringArray(R.array.dialer_title_tips);

        mSharedViewModel = ViewModelProviders.of(this).get(FloatingViewModel.class);
        mSharedViewModel.getFloating().observe(this, aBoolean -> {
            if (aBoolean) {
                DialerActionButtonAnimation.scaleOut(action_button);
            } else {
                DialerActionButtonAnimation.scaleIn(action_button);
            }
        });

        add_contacts.setOnClickListener(this::addContact);

    }

    public void onChanged(@Nullable UserInfo info) {
        if (Objects.nonNull(info)) {
            localName.setText(Objects.requireNonNull(info).getName());
            //启动服务
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent intent = new Intent(this, TelephoneService.class);
                intent.setAction(intentUnits.ACTION_TELEPHONE);
                startForegroundService(intent);
            } else {
                intentUnits.startService(getApplicationContext(), IntentProvider.getCallServiceProvider().getIntent(getApplicationContext()));
            }
        } else {
            showInputDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (action_button.getVisibility() == View.GONE) {
            DialerActionButtonAnimation.scaleIn(action_button);
        }
    }

    private List<DialerFragment> createPager() {
        List<DialerFragment> pagers = new ArrayList<>();
        pagers.add(CollectsFragment.newInstance());
        pagers.add(RecentFragment.newInstance());
        pagers.add(ContactsFragment.newInstance());
        content_pager.setPagerSelectMove(pagers.get((pagers.size() - 1)));
        return pagers;
    }

    private TabSelectedListenerImpl tabSelectedListener = new TabSelectedListenerImpl() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            title_tips.setText(stringTips[tab.getPosition()]);
            if (tab.getPosition() == 2) {
                add_contacts.setVisibility(View.VISIBLE);
            } else {
                add_contacts.setVisibility(View.GONE);
            }
        }
    };


    private void actionButton(View view) {
        DialerActionButtonAnimation.scaleOut(view);
        intentUnits.startPlate(DialerActivity.this, IntentProvider.getPlateProvider().getIntent(getApplicationContext()));
    }

    private void addContact(View view) {
        intentUnits.startAddContact(DialerActivity.this, IntentProvider.getContactAddOrEditProvider(ContactAddOrEditActivity.ADD, -1).getIntent(getApplicationContext()));

    }


    private void showInputDialog() {
        final EditText editText = new EditText(this);
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(this);
        inputDialog.setTitle("请输入ID").setView(editText);
        inputDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    if (!TextUtils.isEmpty(editText.getText().toString())) {
                        ContentResolver resolver = getContentResolver();
                        ContentValues values = new ContentValues(1);
                        values.put("user_name", editText.getText().toString());
                        resolver.insert(URI_USER, values);
                    }
                }).show();
    }

}
